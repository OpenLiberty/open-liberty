/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.identitystore;

import java.lang.reflect.UndeclaredThrowableException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
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
import javax.security.enterprise.credential.CallerOnlyCredential;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition.LdapSearchScope;

import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.openliberty.security.jakartasec.identitystore.permissions.IdentityStorePermissionService;

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
    @Sensitive
    public LdapIdentityStore(@Sensitive LdapIdentityStoreDefinition idStoreDefinition) {
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
        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("No URL was provided to the LdapIdentityStore.");
        }

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
        if (bindDn != null && !bindDn.isEmpty() && bindPw != null) {

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
        return getDirContext(env);
    }

    @Sensitive
    @FFDCIgnore(PrivilegedActionException.class)
    private DirContext getDirContext(@Sensitive Hashtable<Object, Object> env) throws NamingException {
        try {
            return AccessController.doPrivileged(new GetDirContextAction(env));
        } catch (PrivilegedActionException e) {
            Exception oe = e.getException();
            if (oe instanceof NamingException) {
                throw (NamingException) oe;
            } else if (oe instanceof RuntimeException) {
                throw (RuntimeException) oe;
            } else {
                throw new UndeclaredThrowableException(oe);
            }
        }
    }

    /**
     * Made this method non-anonymous to add the trivial annotation to avoid
     * printing out the password.
     */
    @Trivial
    private class GetDirContextAction implements PrivilegedExceptionAction<DirContext> {
        private final Hashtable<Object, Object> env;

        private GetDirContextAction(Hashtable<Object, Object> env) {
            this.env = env;
        }

        @Override
        public DirContext run() throws NamingException {
            return new InitialLdapContext(env, null);
        }
    }

    @Override
    public Set<String> getCallerGroups(CredentialValidationResult validationResult) {
        if (!validationTypes().contains(IdentityStore.ValidationType.PROVIDE_GROUPS)) {
            return new HashSet<String>();
        }

        IdentityStorePermissionService.checkPermission("getGroups");

        String userDn = validationResult.getCallerDn();
        if (userDn == null || userDn.isEmpty()) {
            String user = validationResult.getCallerPrincipal().getName();
            if (isValidDn(user)) {
                userDn = user;
            } else {
                String filter = getFormattedFilter(idStoreDefinition.getCallerSearchFilter(), user, idStoreDefinition.getCallerNameAttribute());
                userDn = getUserDn(user, filter, getCallerSearchControls());
            }
        }

        if (userDn == null || userDn.isEmpty()) {
            return new HashSet<String>();
        }

        try {
            return getGroups(bind(), userDn);
        } catch (NamingException e) {
            Tr.error(tc, "JAVAEESEC_ERROR_EXCEPTION_ON_BIND", new Object[] { this.idStoreDefinition.getBindDn(), e });
            throw new IllegalStateException(e);
        }
    }

    @Override
    public CredentialValidationResult validate(Credential credential) {
        /*
         * Only support UserPasswordCredential.
         */
        if (!(credential instanceof UsernamePasswordCredential || credential instanceof CallerOnlyCredential)) {
            Tr.error(tc, "JAVAEESEC_ERROR_WRONG_CRED");
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }

        if (credential.isValid()) {
            String user;
            boolean usernameOnly = false;
            if (credential instanceof UsernamePasswordCredential) {
                user = ((UsernamePasswordCredential) credential).getCaller();
            } else {
                usernameOnly = true;
                user = ((CallerOnlyCredential) credential).getCaller();
            }
            String filter = idStoreDefinition.getCallerSearchFilter();
            String callerNameAttribute = idStoreDefinition.getCallerNameAttribute();
            String callerName = null;
            String userDn = null;

            Set<String> groups = new HashSet<String>();

            if (isValidDn(user)) {
                userDn = user;
            } else {
                filter = getFormattedFilter(filter, user, callerNameAttribute);
                userDn = getUserDn(user, filter, getCallerSearchControls());
            }
            if (userDn == null) {
                return CredentialValidationResult.INVALID_RESULT;
            }

            /*
             * Authenticate the caller against the LDAP server.
             */
            DirContext context = null;
            if (!usernameOnly) {
                try {
                    context = bind(userDn, new ProtectedString(((UsernamePasswordCredential) credential).getPassword().getValue()));
                } catch (NamingException e) {
                    Tr.debug(tc, "JAVAEESEC_ERROR_EXCEPTION_ON_BIND", new Object[] { userDn, e });
                    return CredentialValidationResult.INVALID_RESULT;
                }
            } else {
                try {
                    context = bind();
                } catch (NamingException e) {
                    Tr.error(tc, "JAVAEESEC_ERROR_EXCEPTION_ON_BIND", new Object[] { idStoreDefinition.getBindDn(), e });
                    throw new IllegalStateException(e);
                }
            }
            if (context == null) {
                return CredentialValidationResult.INVALID_RESULT;
            }

            if (callerNameAttribute.equalsIgnoreCase("dn")) {
                callerName = userDn;
            } else {
                try {
                    Attributes attrs = context.getAttributes(userDn, new String[] { callerNameAttribute });
                    Attribute attribute = attrs.get(callerNameAttribute);
                    if (attribute == null) {
                        Tr.warning(tc, "JAVAEESEC_WARNING_MISSING_CALLER_ATTR", new Object[] { userDn, callerNameAttribute });
                        return CredentialValidationResult.INVALID_RESULT;
                    }
                    NamingEnumeration<?> ne = attribute.getAll();
                    while (ne.hasMoreElements()) {
                        callerName = (String) ne.nextElement();
                    }
                } catch (NamingException e) {
                    Tr.warning(tc, "JAVAEESEC_WARNING_EXCEPTION_ON_GETATTRIBUTES", new Object[] { userDn, callerNameAttribute, e });
                }
            }

            if (validationTypes().contains(IdentityStore.ValidationType.PROVIDE_GROUPS)) {
                /*
                 * Get the caller's groups.
                 */
                groups = getGroups(context, userDn);
            }

            String url = idStoreDefinition.getUrl();
            url = url.replaceFirst("(?i)ldaps?:\\/\\/", "");
            if (url.endsWith("/")) {
                url = url.substring(0, url.length() - 1);
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
     * @param filter     The filter to search for the caller.
     * @param controls   The {@link SearchControls} object.
     * @return The user's DN.
     */
    private String getUserDn(String callerName, String filter, SearchControls controls) {
        String userDn = null;
        String searchBase = idStoreDefinition.getCallerSearchBase();
        if (searchBase == null || searchBase.isEmpty()) {
            userDn = idStoreDefinition.getCallerNameAttribute() + "=" + callerName + "," + idStoreDefinition.getCallerBaseDn();
        } else {
            DirContext ctx = null;
            try {
                ctx = bind();
            } catch (NamingException e) {
                Tr.error(tc, "JAVAEESEC_ERROR_EXCEPTION_ON_BIND", new Object[] { this.idStoreDefinition.getBindDn(), e });
                throw new IllegalStateException(e);
            }
            try {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "JNDI_CALL search", new Object[] { searchBase, filter, printControls(controls) });
                }
                NamingEnumeration<SearchResult> ne = ctx.search(new LdapName(searchBase), filter, controls);
                if (ne.hasMoreElements()) {
                    userDn = ne.nextElement().getNameInNamespace();
                    if (ne.hasMoreElements()) {
                        Tr.warning(tc, "JAVAEESEC_WARNING_MULTI_CALLER_LDAP", new Object[] { callerName, filter, searchBase });
                        return null;
                    }
                }
            } catch (NamingException e) {
                Tr.error(tc, "JAVAEESEC_ERROR_EXCEPTION_ON_SEARCH", new Object[] { callerName, filter, searchBase, e });
                throw new IllegalStateException(e);
            }
        }
        return userDn;
    }

    /**
     * Get the groups for the caller
     *
     * @param context  The {@link DirContext} to use when performing the search.
     * @param callerDn The caller's distinguished name.
     * @return The set of groups the caller is a member of.
     */
    private Set<String> getGroups(DirContext context, String callerDn) {
        Set<String> groups = null;
        String groupSearchBase = idStoreDefinition.getGroupSearchBase();
        String groupSearchFilter = idStoreDefinition.getGroupSearchFilter();
        if (groupSearchBase.isEmpty() || groupSearchFilter.isEmpty()) {
            groups = getGroupsByMembership(context, callerDn);
        } else {
            groups = getGroupsByMember(context, callerDn, groupSearchBase, groupSearchFilter);
        }
        return groups;
    }

    /**
     * Get the groups for the caller by using a member-style attribute found on group LDAP entities.
     *
     * @param context           The {@link DirContext} to use when performing the search.
     * @param callerDn          The caller's distinguished name.
     * @param groupSearchFilter The filter to use when searching for groups
     * @param groupSearchBase   The base of the tree to start the group search from
     * @return The set of groups the caller is a member of.
     */
    private Set<String> getGroupsByMember(DirContext context, String callerDn, String groupSearchBase, String groupSearchFilter) {

        String groupNameAttribute = idStoreDefinition.getGroupNameAttribute();

        String[] attrIds = { groupNameAttribute };
        long limit = Long.valueOf(idStoreDefinition.getMaxResults());
        int timeOut = idStoreDefinition.getReadTimeout();
        int scope = getSearchScope(idStoreDefinition.getGroupSearchScope());

        SearchControls controls = new SearchControls(scope, limit, timeOut, attrIds, false, false);

        String filter = getFormattedFilter(groupSearchFilter, callerDn, idStoreDefinition.getGroupMemberAttribute());

        Set<String> groupNames = new HashSet<String>();

        try {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "JNDI_CALL search", new Object[] { groupSearchBase, filter, printControls(controls) });
            }

            NamingEnumeration<SearchResult> ne = context.search(new LdapName(groupSearchBase), filter, controls);

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Iterate through the search results");
            }

            while (ne.hasMoreElements()) {
                SearchResult sr = ne.nextElement();
                String groupDn = sr.getNameInNamespace();
                if (groupNameAttribute.equalsIgnoreCase("dn")) {
                    groupNames.add(groupDn);
                } else {
                    Attribute groupNameAttr = sr.getAttributes().get(groupNameAttribute);
                    if (groupNameAttr == null) {
                        Tr.warning(tc, "JAVAEESEC_WARNING_MISSING_GROUP_ATTR", new Object[] { groupDn, groupNameAttribute });
                        continue;
                    }
                    NamingEnumeration<?> ne2 = groupNameAttr.getAll();
                    if (ne2.hasMoreElements()) {
                        groupNames.add((String) ne2.nextElement());
                    }
                }
            }
        } catch (NamingException e) {
            Tr.error(tc, "JAVAEESEC_ERROR_EXCEPTION_ON_GROUP_SEARCH", new Object[] { callerDn, e });
            throw new IllegalStateException(e);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getGroupsByMember", groupNames);
        }
        return groupNames;
    }

    /**
     * Format the callerSearchFilter or groupSearchFilter. We need to check for String substitution.
     * If a substitution is needed, use the result as is. Otherwise, construct the remainder of the
     * filter using the name attribute of the group or caller.
     *
     * @param searchFilter The filter set in LdapIdentityStore
     * @param caller       The name of the caller whose groups or DN we are searching for
     * @param attribute    The attribute to use when forming the filter
     * @return The new filter after string replacements or constructing the filter
     */
    private String getFormattedFilter(String searchFilter, String caller, String attribute) {
        //Allow %v in addition to %s for string replacement
        String filter = searchFilter.replaceAll("%v", "%s");
        if (!(filter.startsWith("(") && filter.endsWith(")")) && !filter.isEmpty()) {
            filter = "(" + filter + ")";
        }
        if (filter.contains("%s")) {
            filter = String.format(filter, caller);
        } else {
            filter = "(&" + filter + "(" + attribute + "=" + caller + "))";
        }
        return filter;
    }

    /**
     * Get the groups for the caller by using the memberOf-style attribute found on user LDAP entities.
     *
     * @param context  The {@link DirContext} to use when performing the search.
     * @param callerDn The caller's distinguished name.
     * @return The set of groups the caller is a member of.
     */
    private Set<String> getGroupsByMembership(DirContext context, String callerDn) {
        String memberOfAttribute = idStoreDefinition.getGroupMemberOfAttribute();
        String groupNameAttribute = idStoreDefinition.getGroupNameAttribute();
        Attributes attrs;
        Set<String> groupDns = new HashSet<String>();

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "JNDI_CALL getAttributes", new Object[] { callerDn, memberOfAttribute });
        }
        try {
            attrs = context.getAttributes(callerDn, new String[] { memberOfAttribute });

            Attribute groupSet = attrs.get(memberOfAttribute);
            if (groupSet != null) {
                NamingEnumeration<?> ne = groupSet.getAll();
                while (ne.hasMoreElements()) {
                    groupDns.add((String) ne.nextElement());
                }
            }
        } catch (NamingException e) {
            Tr.warning(tc, "JAVAEESEC_WARNING_EXCEPTION_ON_GETATTRIBUTES", new Object[] { callerDn, memberOfAttribute, e });
        }

        if (groupNameAttribute.equalsIgnoreCase("dn")) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getGroupsByMembership", groupDns);
            }
            return groupDns;
        }

        Set<String> groupNames = new HashSet<String>();
        String groupDn = null;
        Iterator<String> it = groupDns.iterator();
        try {
            while (it.hasNext()) {
                groupDn = it.next();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "JNDI_CALL getAttributes", new Object[] { groupDn, groupNameAttribute });
                }
                Attributes groupNameAttrs = context.getAttributes(groupDn, new String[] { groupNameAttribute });
                Attribute groupNameAttr = groupNameAttrs.get(groupNameAttribute);
                if (groupNameAttr == null) {
                    Tr.warning(tc, "JAVAEESEC_WARNING_MISSING_GROUP_ATTR", new Object[] { groupDn, groupNameAttribute });
                    continue;
                }
                NamingEnumeration<?> ne = groupNameAttr.getAll();
                if (ne.hasMoreElements()) {
                    groupNames.add((String) ne.nextElement());
                }
            }
        } catch (NamingException e) {
            Tr.warning(tc, "JAVAEESEC_WARNING_EXCEPTION_ON_GETATTRIBUTES", new Object[] { groupDn, groupNameAttribute, e });
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getGroupsByMembership", groupNames);
        }
        return groupNames;
    }

    @FFDCIgnore(InvalidNameException.class)
    private boolean isValidDn(String user) {
        try {
            new LdapName(user).toString();
        } catch (InvalidNameException e) {
            return false;
        }
        return true;
    }

    /**
     * Get the {@link SearchControls} object for the caller search.
     *
     * @return The {@link SearchControls} object to use when search LDAP for the user.
     */
    private SearchControls getCallerSearchControls() {
        String[] attrIds = { idStoreDefinition.getCallerNameAttribute() };
        long limit = Long.valueOf(idStoreDefinition.getMaxResults());
        int timeOut = idStoreDefinition.getReadTimeout();
        int scope = getSearchScope(idStoreDefinition.getCallerSearchScope());
        return new SearchControls(scope, limit, timeOut, attrIds, false, false);
    }

    /**
     * Get a user-readable string representing the {@link SearchControls} object.
     *
     * @param controls The controls to get the string for.
     * @return The string representation for the SearchControls object.
     */
    private String printControls(SearchControls controls) {
        StringBuffer result = new StringBuffer();
        result.append("[searchScope: ").append(controls.getSearchScope());
        result.append(", timeLimit: ").append(controls.getTimeLimit());
        result.append(", countLimit: ").append(controls.getCountLimit());
        result.append(", returningObjFlag: ").append(controls.getReturningObjFlag());
        result.append(", returningAttributes: ").append(controls.getReturningAttributes()[0]).append("]");
        return result.toString();
    }

    /**
     * Convert the {@link LdapSearchScope} setting to the JNDI {@link SearchControls} equivalent.
     *
     * @param scope The {@link LdapIdentityStore} to convert to the JNDI equivalent.
     * @return The JNDI {@link SearchControls} search scope.
     */
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
    public Set<ValidationType> validationTypes() {
        return this.idStoreDefinition.getUseFor();
    }
}
