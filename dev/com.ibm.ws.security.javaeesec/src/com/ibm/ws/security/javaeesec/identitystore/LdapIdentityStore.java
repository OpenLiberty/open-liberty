/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.identitystore;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.naming.CommunicationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.ServiceUnavailableException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapName;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStorePermission;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition.LdapSearchScope;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * Liberty's LDAP {@link IdentityStore} implementation.
 */
@Default
@ApplicationScoped
public class LdapIdentityStore implements IdentityStore {
    private static final TraceComponent tc = Tr.register(LdapIdentityStore.class);

    /** The definitions for this IdentityStore. */
    private final LdapIdentityStoreDefinitionWrapper idStoreDefinition;

    /**
     * Construct a new {@link LdapIdentityStore} instance using the specified definitions.
     *
     * @param idStoreDefinition The definitions to use to configure the {@link IdentityStore}.
     */
    public LdapIdentityStore(LdapIdentityStoreDefinition idStoreDefinition) {
        this.idStoreDefinition = new LdapIdentityStoreDefinitionWrapper(idStoreDefinition);
    }

    /**
     * Bind to the LDAP server for administrative operations such as searches.
     *
     * @return The bound {@link DirContext}.
     * @throws NamingException If there was a failure to bind to the LDAP server.
     */
    private DirContext bind() throws NamingException {
        return bind(this.idStoreDefinition.getBindDn(), this.idStoreDefinition.getBindDnPassword());
    }

    /**
     * Bind to the LDAP server.
     *
     * @param bindDn The distinguished name used to bind to the LDAP server.
     * @param bindPw The password used to bind to the LDAP server.
     * @return The bound {@link DirContext}.
     * @throws NamingException If there was a failure to bind to the LDAP server.
     */
    private DirContext bind(String bindDn, @Sensitive String bindPw) throws NamingException {
        Hashtable<Object, Object> env = new Hashtable<Object, Object>();
        String url = this.idStoreDefinition.getUrl();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url);

        //TODO: Fix ssl
        boolean sslEnabled = url != null && url.startsWith("ldaps");
        if (sslEnabled) {
            env.put("java.naming.ldap.factory.socket", "com.ibm.ws.ssl.protocol.LibertySSLSocketFactory");
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }

        /*
         * Add credentials.
         */
        if (bindDn != null && !bindDn.isEmpty()) {

            /*
             * Support encoded passwords.
             */
            String decodedBindPw = PasswordUtil.passwordDecode(bindPw.trim());
            if (decodedBindPw == null || decodedBindPw.isEmpty()) {
                throw new IllegalArgumentException("An empty password is invalid.");
            }

            env.put(Context.SECURITY_PRINCIPAL, bindDn);
            env.put(Context.SECURITY_CREDENTIALS, decodedBindPw);
        }

        /*
         * TODO How do we support SSL setting? End-point based configuration?
         */

        return new InitialLdapContext(env, null);
    }

    @Override
    public Set<String> getCallerGroups(CredentialValidationResult validationResult) {
        SecurityManager secManager = System.getSecurityManager();
        if (secManager != null) {
            secManager.checkPermission(new IdentityStorePermission("getGroups"));
        }

        String userDn = validationResult.getCallerDn();
        if (userDn == null || userDn.isEmpty()) {
            String[] attrIds = { idStoreDefinition.getCallerNameAttribute() };
            String user = validationResult.getCallerPrincipal().getName();
            String filter = "(&(" + attrIds[0] + "=" + user + ")" + idStoreDefinition.getCallerSearchFilter() + ")";
            userDn = getUserDn(user, filter, getUserSearchControls());
        }

        Set<String> groups = new HashSet<String>();
        try {
            DirContext context = bind();

            if (idStoreDefinition.getGroupSearchBase().isEmpty() || idStoreDefinition.getGroupSearchFilter().isEmpty()) {
                groups = getGroupsByMembership(context, userDn, idStoreDefinition.getGroupMemberOfAttribute());
            } else {
                groups = getGroupsByMember(context, userDn);
            }
        } catch (NamingException e) {
            Tr.debug(tc, "exception: ", e);
        }
        return groups;
    }

    @Trivial
    private SearchControls getUserSearchControls() {
        String[] attrIds = { idStoreDefinition.getCallerNameAttribute() };
        long limit = Long.valueOf(idStoreDefinition.getMaxResults());
        int timeOut = idStoreDefinition.getReadTimeout();
        int scope = getSearchScope(idStoreDefinition.getCallerSearchScope());
        return new SearchControls(scope, limit, timeOut, attrIds, false, false);
    }

    @Trivial
    private int getSearchScope(LdapSearchScope scope) {
        if (scope == LdapSearchScope.ONE_LEVEL) {
            return SearchControls.ONELEVEL_SCOPE;
        } else {
            return SearchControls.SUBTREE_SCOPE;
        }
    }

    /**
     * Most LDAPs throw CommunicationException when LDAP server is down, but
     * z/OS sometime throws ServiceUnavailableException when ldap server is down.
     */
    private boolean isConnectionException(NamingException e, String METHODNAME) {
        if (e instanceof CommunicationException) {
            return true;
        } else if (e instanceof ServiceUnavailableException) {
            return true;
        }
        return false;
    }

    @Override
    public int priority() {
        return this.idStoreDefinition.getPriority();
    }

    @Override
    public CredentialValidationResult validate(Credential credential) {
        /*
         * Only support UserPasswordCredential.
         */
        if (!(credential instanceof UsernamePasswordCredential)) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }

        UsernamePasswordCredential cred = (UsernamePasswordCredential) credential;

        if (credential.isValid()) {
            String user = cred.getCaller();
            String filter = idStoreDefinition.getCallerSearchFilter();
            String[] attrIds = { idStoreDefinition.getCallerNameAttribute() };
            String memberof = idStoreDefinition.getGroupMemberOfAttribute();
            String member = idStoreDefinition.getGroupMemberAttribute();

            filter = "(&(" + attrIds[0] + "=" + user + ")" + filter + ")";
            Tr.debug(tc, "filter: ", filter);
            Tr.debug(tc, "attrIds: ", attrIds[0]);
            Tr.debug(tc, "memberof: ", memberof);
            Tr.debug(tc, "member: ", member);

            Set<String> groups = new HashSet<String>();

            String userDn = getUserDn(user, filter, getUserSearchControls());
            if (userDn == null) {
                return CredentialValidationResult.INVALID_RESULT;
            }
            try {

                DirContext context = bind(userDn, cred.getPasswordAsString());
                if (context == null) {
                    return CredentialValidationResult.INVALID_RESULT;
                }

                if (idStoreDefinition.getGroupSearchBase().isEmpty() || idStoreDefinition.getGroupSearchFilter().isEmpty()) {
                    groups = getGroupsByMembership(context, userDn, memberof);
                } else {
                    groups = getGroupsByMember(context, userDn);
                }
                Tr.debug(tc, "groups: ", groups);
            } catch (NamingException e) {
                Tr.debug(tc, "exception: " + e);
//                if (!isConnectionException(e, "getCallerGroups")) {
                return CredentialValidationResult.INVALID_RESULT;
//                }
            }

            //TODO: storeId, host:port currently
            String url = idStoreDefinition.getUrl();
            url = url.replaceFirst("(?i)ldaps?:\\/\\/", "");
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }
            return new CredentialValidationResult(url, user, userDn, userDn, groups);
        }

        return CredentialValidationResult.INVALID_RESULT;
    }

    @FFDCIgnore(InvalidNameException.class)
    private String getUserDn(String user, String filter, SearchControls controls) {
        //check if user is a valid DN
        String userDn = null;
        try {
            userDn = new LdapName(user).toString();
        } catch (InvalidNameException e) {

        }
        if (userDn != null) {
            return userDn;
        }
        String searchBase = idStoreDefinition.getCallerSearchBase();
        if (searchBase == null || searchBase.isEmpty()) {
            userDn = idStoreDefinition.getCallerNameAttribute() + "=" + user + idStoreDefinition.getCallerBaseDn();
        } else {
            try {
                DirContext ctx = bind();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "JNDI_CALL search", new Object[] { searchBase, filter, printControls(controls) });
                }
                NamingEnumeration<SearchResult> ne = ctx.search(new LdapName(searchBase), filter, controls);
                if (ne.hasMoreElements()) {
                    userDn = ne.nextElement().getNameInNamespace();
                    Tr.debug(tc, "userDN: ", userDn);
                    if (ne.hasMoreElements()) {
                        Tr.debug(tc, "multiple principals found");
                        return null;
                    }
                } else {
                    Tr.debug(tc, "no principal found");
                    return null;
                }
            } catch (NamingException ne) {
                Tr.debug(tc, "exception: ", ne);
            }
        }
        return userDn;
    }

    /**
     * @param controls
     * @return
     */
    @Trivial
    private String printControls(SearchControls controls) {
        StringBuffer result = new StringBuffer();
        result.append("[searchScope: ").append(controls.getSearchScope());
        result.append(", timeLimit: ").append(controls.getTimeLimit());
        result.append(", countLimit: ").append(controls.getCountLimit());
        result.append(", returningObjFlag: ").append(controls.getReturningObjFlag());
        result.append(", returningAttributes: ").append(controls.getReturningAttributes()[0]).append("]");
        return result.toString();
    }

    private Set<String> getGroupsByMember(DirContext context, String userDN) throws InvalidNameException, NamingException {

        String[] attrIds = { idStoreDefinition.getGroupNameAttribute() };
        long limit = Long.valueOf(idStoreDefinition.getMaxResults());
        int timeOut = idStoreDefinition.getReadTimeout();
        int scope = getSearchScope(idStoreDefinition.getGroupSearchScope());

        SearchControls controls = new SearchControls(scope, limit, timeOut, attrIds, false, false);
        String filter = "(&" + idStoreDefinition.getGroupSearchFilter() + "(" + idStoreDefinition.getGroupMemberAttribute() + "=" + userDN + "))";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "JNDI_CALL search entry", new Object[] { idStoreDefinition.getGroupSearchBase(), filter, printControls(controls) });
        }
        NamingEnumeration<SearchResult> ne = context.search(new LdapName(idStoreDefinition.getGroupSearchBase()), filter, controls);
        Set<String> groups = new HashSet<String>();
        while (ne.hasMoreElements()) {
            groups.add(ne.nextElement().getNameInNamespace());
        }
        return groups;
    }

    private Set<String> getGroupsByMembership(DirContext context, String user, String memberof) throws NamingException {
        Attributes attrs;
        Set<String> groups = new HashSet<String>();

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "JNDI_CALL getAttributes", new Object[] { user, memberof });
        }
        attrs = context.getAttributes(user, new String[] { memberof });

        Attribute groupSet = attrs.get(memberof);
        NamingEnumeration<?> ne = groupSet.getAll();
        while (ne.hasMoreElements()) {
            groups.add((String) ne.nextElement());
        }
        return groups;
    }

    @Override
    public Set<ValidationType> validationTypes() {
        return this.idStoreDefinition.getUseFor();
    }
}
