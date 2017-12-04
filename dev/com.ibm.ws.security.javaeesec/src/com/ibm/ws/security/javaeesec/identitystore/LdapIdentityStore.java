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
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
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
import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
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
    private DirContext bind(String bindDn, ProtectedString bindPw) throws NamingException {
        Hashtable<Object, Object> env = new Hashtable<Object, Object>();
        String url = this.idStoreDefinition.getUrl();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url);

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
            String decodedBindPw = PasswordUtil.passwordDecode(new String(bindPw.getChars()).trim());
            if (decodedBindPw == null || decodedBindPw.isEmpty()) {
                throw new IllegalArgumentException("An empty password is invalid.");
            }

            env.put(Context.SECURITY_PRINCIPAL, bindDn);
            env.put(Context.SECURITY_CREDENTIALS, decodedBindPw);
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "JNDI_CALL bind", new Object[] { bindDn, url });
        }
        return new InitialLdapContext(env, null);
    }

    @Override
    public Set<String> getCallerGroups(CredentialValidationResult validationResult) {
        if (!validationTypes().contains(IdentityStore.ValidationType.PROVIDE_GROUPS)) {
            return new HashSet<String>();
        }

        SecurityManager secManager = System.getSecurityManager();
        if (secManager != null) {
            secManager.checkPermission(new IdentityStorePermission("getGroups"));
        }

        String userDn = validationResult.getCallerDn();
        if (userDn == null || userDn.isEmpty()) {
            String[] attrIds = { idStoreDefinition.getCallerNameAttribute() };
            String user = validationResult.getCallerPrincipal().getName();
            String filter = "(&(" + attrIds[0] + "=" + user + ")" + idStoreDefinition.getCallerSearchFilter() + ")";
            if (isValidDn(user)) {
                userDn = user;
            } else {
                userDn = getUserDn(user, filter, getCallerSearchControls());
            }
        }

        try {
            return getGroups(bind(), userDn);
        } catch (NamingException e) {
        }
        return new HashSet<String>();
    }

    /**
     * Get the {@link SearchControls} object for the caller search.
     *
     * @return The {@link SearchControls} object to use when search LDAP for the user.
     */
    @Trivial
    private SearchControls getCallerSearchControls() {
        String[] attrIds = { idStoreDefinition.getCallerNameAttribute() };
        long limit = Long.valueOf(idStoreDefinition.getMaxResults());
        int timeOut = idStoreDefinition.getReadTimeout();
        int scope = getSearchScope(idStoreDefinition.getCallerSearchScope());
        return new SearchControls(scope, limit, timeOut, attrIds, false, false);
    }

    /**
     * Convert the {@link LdapSearchScope} setting to the JNDI {@link SearchControls} equivalent.
     *
     * @param scope The {@link LdapIdentityStore} to convert to the JNDI equivalent.
     * @return The JNDI {@link SearchControls} search scope.
     */
    @Trivial
    private int getSearchScope(LdapSearchScope scope) {
        if (scope == LdapSearchScope.ONE_LEVEL) {
            return SearchControls.ONELEVEL_SCOPE;
        } else {
            return SearchControls.SUBTREE_SCOPE;
        }
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
            String callerName = null;
            String userDn = null;

            filter = "(&(" + attrIds[0] + "=" + user + ")" + filter + ")";
            Tr.debug(tc, "filter: ", filter);
            Tr.debug(tc, "attrIds: ", attrIds[0]);

            Set<String> groups = new HashSet<String>();

            if (isValidDn(user)) {
                userDn = user;
            } else {
                userDn = getUserDn(user, filter, getCallerSearchControls());
                callerName = user;
            }
            if (userDn == null) {
                return CredentialValidationResult.INVALID_RESULT;
            }

            /*
             * Authenticate the caller against the LDAP server.
             */
            DirContext context = null;
            try {

                context = bind(userDn, new ProtectedString(cred.getPassword().getValue()));
                if (context == null) {
                    return CredentialValidationResult.INVALID_RESULT;
                }
            } catch (NamingException e) {
                return CredentialValidationResult.INVALID_RESULT;
            }

            String url = idStoreDefinition.getUrl();
            url = url.replaceFirst("(?i)ldaps?:\\/\\/", "");
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
            }

            if (callerName == null) {
                try {
                    Attributes attrs = context.getAttributes(userDn, attrIds);
                    Attribute attribute = attrs.get(attrIds[0]);
                    NamingEnumeration<?> ne = attribute.getAll();
                    while (ne.hasMoreElements()) {
                        callerName = (String) ne.nextElement();
                    }
                } catch (NamingException e) {

                }
            }

            if (validationTypes().contains(IdentityStore.ValidationType.PROVIDE_GROUPS)) {

                /*
                 * Get the caller's groups.
                 */
                try {
                    String groupSearchBase = idStoreDefinition.getGroupSearchBase();
                    String groupSearchFilter = idStoreDefinition.getGroupSearchFilter();
                    if (groupSearchBase.isEmpty() || groupSearchFilter.isEmpty()) {
                        groups = getGroupsByMembership(context, userDn);
                    } else {
                        groups = getGroupsByMember(context, userDn, groupSearchBase, groupSearchFilter);
                    }
                } catch (NamingException e) {
                    Tr.debug(tc, "exception: " + e);
                }
            }
            return new CredentialValidationResult(url, callerName, userDn, userDn, groups);
        }

        return CredentialValidationResult.INVALID_RESULT;
    }

    /**
     * Get the caller's full distinguished name (DN). The DN can be returned in one of the following ways:
     *
     * <ul>
     * <li>Using the callerSearchBase, caller's name and the callerBaseDn to form the DN.</li>
     * <li>Search in LDAP for the user and returning the DN from the LDAP entry.</li>
     * </ul>
     *
     * @param callerName The caller's name.
     * @param filter The filter to search for the caller.
     * @param controls The {@link SearchControls} object.
     * @return The user's DN.
     */
    private String getUserDn(String callerName, String filter, SearchControls controls) {
        String userDn = null;
        String searchBase = idStoreDefinition.getCallerSearchBase();
        if (searchBase == null || searchBase.isEmpty()) {
            userDn = idStoreDefinition.getCallerNameAttribute() + "=" + callerName + "," + idStoreDefinition.getCallerBaseDn();
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
     * Get a user-readable string representing the {@link SearchControls} object.
     *
     * @param controls The controls to get the string for.
     * @return The string representation for the SearchControls object.
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

    private Set<String> getGroups(DirContext context, String userDn) {
        Set<String> groups = null;
        String groupSearchBase = idStoreDefinition.getGroupSearchBase();
        String groupSearchFilter = idStoreDefinition.getGroupSearchFilter();
        try {
            if (groupSearchBase.isEmpty() || groupSearchFilter.isEmpty()) {
                groups = getGroupsByMembership(context, userDn);
            } else {
                groups = getGroupsByMember(context, userDn, groupSearchBase, groupSearchFilter);
            }
        } catch (NamingException e) {
            Tr.error(tc, "An error occurred while searching for groups.", e);
        }
        return groups;
    }

    /**
     * Get the groups for the caller by using a member-style attribute found on group LDAP entities.
     *
     * @param context The {@link DirContext} to use when performing the search.
     * @param callerDn The caller's distinguished name.
     * @param groupSearchFilter
     * @param groupSearchBase2
     * @return The set of groups the caller is a member of.
     * @throws NamingException If there was an issue with the JNDI request.
     */
    private Set<String> getGroupsByMember(DirContext context, String callerDn, String groupSearchBase, String groupSearchFilter) throws NamingException {

        String[] attrIds = { idStoreDefinition.getGroupNameAttribute() };
        long limit = Long.valueOf(idStoreDefinition.getMaxResults());
        int timeOut = idStoreDefinition.getReadTimeout();
        int scope = getSearchScope(idStoreDefinition.getGroupSearchScope());

        SearchControls controls = new SearchControls(scope, limit, timeOut, attrIds, false, false);
        String filter = "(&" + groupSearchFilter + "(" + idStoreDefinition.getGroupMemberAttribute() + "=" + callerDn + "))";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "JNDI_CALL search entry", new Object[] { groupSearchBase, filter, printControls(controls) });
        }
        NamingEnumeration<SearchResult> ne = context.search(new LdapName(groupSearchBase), filter, controls);
        Set<String> groups = new HashSet<String>();
        while (ne.hasMoreElements()) {
            groups.add(ne.nextElement().getNameInNamespace());
        }
        return groups;
    }

    /**
     * Get the groups for the caller by using the memberOf-style attribute found on user LDAP entities.
     *
     * @param context The {@link DirContext} to use when performing the search.
     * @param callerDn The caller's distinguished name.
     * @param memberOfAttribute The attribute to use as the memberOf attribute..
     * @return The set of groups the caller is a member of.
     * @throws NamingException If there was an issue with the JNDI request.
     */
    private Set<String> getGroupsByMembership(DirContext context, String callerDn) throws NamingException {
        String memberOfAttribute = idStoreDefinition.getGroupMemberOfAttribute();
        Attributes attrs;
        Set<String> groups = new HashSet<String>();

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "JNDI_CALL getAttributes", new Object[] { callerDn, memberOfAttribute });
        }
        attrs = context.getAttributes(callerDn, new String[] { memberOfAttribute });

        Attribute groupSet = attrs.get(memberOfAttribute);
        if (groupSet != null) {
            NamingEnumeration<?> ne = groupSet.getAll();
            while (ne.hasMoreElements()) {
                groups.add((String) ne.nextElement());
            }
        }

        return groups;
    }

    @Trivial
    @FFDCIgnore(InvalidNameException.class)
    private boolean isValidDn(String user) {
        try {
            new LdapName(user).toString();
        } catch (InvalidNameException e) {
            return false;
        }
        return true;
    }

    @Override
    public Set<ValidationType> validationTypes() {
        return this.idStoreDefinition.getUseFor();
    }
}
