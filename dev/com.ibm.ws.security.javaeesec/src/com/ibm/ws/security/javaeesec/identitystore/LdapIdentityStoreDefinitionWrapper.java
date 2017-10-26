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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.el.ELException;
import javax.el.ELProcessor;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStore.ValidationType;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition.LdapSearchScope;

import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * A wrapper class that offers convenience methods for retrieving configuration
 * from an {@link LdapIdentityStoreDefinition} instance.
 *
 * <p/>
 * The methods in this class will evaluate any EL expressions provided in the
 * {@link LdapIdentityStoreDefinition} first and if no EL expressions are provided,
 * return the literal value instead.
 */
class LdapIdentityStoreDefinitionWrapper {

    /** The distinguished name to bind with for this IdentityStore. */
    private final String bindDn;

    /** The distinguished name's password to bind with for this IdentityStore. */
    private final String bindDnPassword;

    /** The base distinguished name for users/callers. */
    private final String callerBaseDn;

    /** The LDAP attribute that contains the user/caller name. */
    private final String callerNameAttribute;

    /** The search base to search for the user/caller on the LDAP server. */
    private final String callerSearchBase;

    /** The LDAP filter to search for the user/caller on the LDAP server. */
    private final String callerSearchFilter;

    /** The search scope to search for the user/caller. */
    private final LdapSearchScope callerSearchScope;

    /** The LDAP attribute to use to search for group membership on a group entity. */
    private final String groupMemberAttribute;

    /** The LDAP attribute to use to search for group membership on a user/caller entity. */
    private final String groupMemberOfAttribute;

    /** The LDAP attribute to retrieve the group name from. */
    private final String groupNameAttribute;

    /** The search base to search for groups on the LDAP server. */
    private final String groupSearchBase;

    /** The LDAP filter to search for groups on the LDAP server. */
    private final String groupSearchFilter;

    /** The search scope to search for groups. */
    private final LdapSearchScope groupSearchScope;

    /** The definitions for this IdentityStore. */
    private final LdapIdentityStoreDefinition idStoreDefinition;

    /** The maximum number of results to return on a search. */
    private final int maxResults;

    /** The priority for this IdentityStore. */
    private final int priority;

    /** The read timeout for LDAP contexts. */
    private final int readTimeout;

    /** The URL for the LDAP server to connect to. */
    private final String url;

    /** The ValidationTypes this IdentityStore can be used for. */
    private final Set<ValidationType> useFor;

    /**
     * Create a new instance of an {@link LdapIdentityStoreDefinitionWrapper} that will provide
     * convenience methods to access configuration from the {@link LdapIdentityStoreDefinition}
     * instance.
     *
     * @param idStoreDefinition The {@link LdapIdentityStoreDefinition} to wrap.
     */
    LdapIdentityStoreDefinitionWrapper(LdapIdentityStoreDefinition idStoreDefinition) {
        /*
         * Ensure we were passed a non-null definition.
         */
        if (idStoreDefinition == null) {
            throw new IllegalArgumentException("The LdapIdentityStoreDefinition cannot be null.");
        }
        this.idStoreDefinition = idStoreDefinition;

        /*
         * Set all configuration. We do this in the constructor instead of on retrieval
         * in order to fail-fast.
         */
        this.bindDn = setBindDn();
        this.bindDnPassword = setBindDnPasword();
        this.callerBaseDn = setCallerBaseDn();
        this.callerNameAttribute = setCallerNameAttribute();
        this.callerSearchBase = setCallerSearchBase();
        this.callerSearchFilter = setCallerSearchFilter();
        this.callerSearchScope = setCallerSearchScope();
        this.groupMemberAttribute = setGroupMemberAttribute();
        this.groupMemberOfAttribute = setGroupMemberOfAttribute();
        this.groupNameAttribute = setGroupNameAttribute();
        this.groupSearchBase = setGroupSearchBase();
        this.groupSearchFilter = setGroupSearchFilter();
        this.groupSearchScope = setGroupSearchScope();
        this.maxResults = setMaxResults();
        this.priority = setPriority();
        this.readTimeout = setReadTimeout();
        this.url = setUrl();
        this.useFor = setUseFor();
    }

    /**
     * Get the distinguished name to bind to the LDAP server with.
     *
     * @return The distinguished name to bind with.
     *
     * @see LdapIdentityStoreDefinition#bindDn()
     */
    String getBindDn() {
        return this.bindDn;
    }

    /**
     * Get the password to bind to the LDAP server with.
     *
     * @return The password to bind with.
     *
     * @see LdapIdentityStoreDefinition#bindDnPassword()
     */
    @Sensitive
    String getBindDnPassword() {
        return this.bindDnPassword;
    }

    /**
     * Get the caller base distinguished name.
     *
     * @return The call base distinguished name.
     *
     * @see LdapIdentityStoreDefinition#callerBaseDn()
     */
    String getCallerBaseDn() {
        return callerBaseDn;
    }

    /**
     * Get the LDAP attribute to use to find the name of a user/caller entity.
     *
     * @return The user/caller name attribute.
     *
     * @see LdapIdentityStoreDefinition#callerNameAttribute()
     */
    String getCallerNameAttribute() {
        return callerNameAttribute;
    }

    /**
     * Get the search base to search for user/caller entities.
     *
     * @return The user/caller search base.
     *
     * @see LdapIdentityStoreDefinition#callerSearchBase()
     */
    String getCallerSearchBase() {
        return callerSearchBase;
    }

    /**
     * Get the search filter to search for user/caller entities.
     *
     * @return The user/caller search filter.
     *
     * @see LdapIdentityStoreDefinition#callerSearchFilter()
     */
    String getCallerSearchFilter() {
        return callerSearchFilter;
    }

    /**
     * Get the search scope to search for user/caller entities.
     *
     * @return The user/caller search scope.
     *
     * @see LdapIdentityStoreDefinition#callerSearchScope()
     * @see LdapIdentityStoreDefinition#callerSearchScopeExpression()
     */
    LdapSearchScope getCallerSearchScope() {
        return callerSearchScope;
    }

    /**
     * Get the LDAP attribute to use to find group membership on a group entity.
     *
     * @return The group member attribute.
     *
     * @see LdapIdentityStoreDefinition#groupMemberAttribute()
     */
    String getGroupMemberAttribute() {
        return groupMemberAttribute;
    }

    /**
     * Get the LDAP attribute to use to find group membership on a user/caller entity.
     *
     * @return The group member of attribute.
     *
     * @see LdapIdentityStoreDefinition#groupMemberOfAttribute()
     */
    String getGroupMemberOfAttribute() {
        return groupMemberOfAttribute;
    }

    /**
     * Get the LDAP attribute to use to find the name of a group entity.
     *
     * @return The group name attribute.
     *
     * @see LdapIdentityStoreDefinition#groupNameAttribute()
     */
    String getGroupNameAttribute() {
        return groupNameAttribute;
    }

    /**
     * Get the search base to search for group entities.
     *
     * @return The group search base.
     *
     * @see LdapIdentityStoreDefinition#groupSearchBase()
     */
    String getGroupSearchBase() {
        return groupSearchBase;
    }

    /**
     * Get the search filter to search for group entities.
     *
     * @return The group search filter.
     *
     * @see LdapIdentityStoreDefinition#groupSearchFilter()
     */
    String getGroupSearchFilter() {
        return groupSearchFilter;
    }

    /**
     * Get the search scope to search for group entities.
     *
     * @return The group search scope.
     *
     * @see LdapIdentityStoreDefinition#groupSearchScope()
     * @see LdapIdentityStoreDefinition#groupSearchScopeExpression()
     */
    LdapSearchScope getGroupSearchScope() {
        return groupSearchScope;
    }

    /**
     * Get the maximum number of results to return from a search.
     *
     * @return The maximum number of results.
     *
     * @see LdapIdentityStoreDefinition#maxResults()
     * @see LdapIdentityStoreDefinition#maxResultsExpression()
     */
    int getMaxResults() {
        return maxResults;
    }

    /**
     * Get the priority for the {@link IdentityStore}.
     *
     * @return The priority.
     *
     * @see LdapIdentityStoreDefinition#priority()
     * @see LdapIdentityStoreDefinition#priorityExpression()
     */
    int getPriority() {
        return this.priority;
    }

    /**
     * Get the read timeout for the {@link IdentityStore}.
     *
     * @return The read timeout.
     *
     * @see LdapIdentityStoreDefinition#readTimeout()
     * @see LdapIdentityStoreDefinition#readTimeoutExpression()
     */
    int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Get the URL of the LDAP server to bind to.
     *
     * @return The URL of the LDAP server.
     *
     * @see LdapIdentityStoreDefinition#url()
     */
    String getUrl() {
        return this.url;
    }

    /**
     * Get the useFor for the {@link IdentityStore}.
     *
     * @return The useFor.
     *
     * @see LdapIdentityStoreDefinition#useFor()
     * @see LdapIdentityStoreDefinition#useForExpression()
     */
    Set<ValidationType> getUseFor() {
        return this.useFor;
    }

    /**
     * This method will process a configuration value for any configuration setting in
     * {@link LdapIdentityStoreDefinition} that is a string and whose name is NOT a
     * "*Expression". It will first check to see if it is a EL expression. It it is, it
     * will return the evaluated expression; otherwise, it will return the literal String.
     *
     * @param name The name of the property. Used for error messages.
     * @param value The value returned from from the {@link LdapIdentityStoreDefinition}, which can
     *            either be a literal String or an EL expression.
     * @return The String value.
     */
    @FFDCIgnore(ELException.class)
    private String processString(String name, String value) {
        String result;

        ELProcessor elp = new ELProcessor();
        try {
            Object obj = elp.eval(value);
            if (obj instanceof String) {
                result = (String) obj;
            } else {
                throw new IllegalArgumentException("Expected '" + name + "' to evaluate to an String value.");
            }
        } catch (ELException e) {
            result = value;
        }

        return result;
    }

    /**
     * Validate and return the bindDn.
     *
     * @return The validated bindDn.
     */
    private String setBindDn() {
        return processString("bindDn", idStoreDefinition.bindDn());
    }

    /**
     * Validate and return the bindDnPassword.
     *
     * @return The validated bindDnPassword.
     */
    private String setBindDnPasword() {
        return processString("bindDnPassword", idStoreDefinition.bindDnPassword());
    }

    /**
     * Validate and return the callerBaseDn.
     *
     * @return The validated callerBaseDn.
     */
    private String setCallerBaseDn() {
        return processString("callerBaseDn", idStoreDefinition.callerBaseDn());
    }

    /**
     * Validate and return the callerNameAttribute.
     *
     * @return The validated callerNameAttribute.
     */
    private String setCallerNameAttribute() {
        return processString("callerNameAttribute", idStoreDefinition.callerNameAttribute());
    }

    /**
     * Validate and return the callerSearchBase.
     *
     * @return The validated callerSearchBase.
     */
    private String setCallerSearchBase() {
        return processString("callerSearchBase", idStoreDefinition.callerSearchBase());
    }

    /**
     * Validate and return the callerSearchFilter.
     *
     * @return The validated callerSearchFilter.
     */
    private String setCallerSearchFilter() {
        return processString("callerSearchFilter", idStoreDefinition.callerSearchFilter());
    }

    /**
     * Validate and return the callerSearchScope.
     *
     * @return The validated callerSearchScope
     */
    private LdapSearchScope setCallerSearchScope() {
        LdapSearchScope scope;

        /*
         * The expression language value takes precedence over the direct setting.
         */
        if (idStoreDefinition.callerSearchScopeExpression().isEmpty()) {
            /*
             * Direct setting.
             */
            scope = idStoreDefinition.callerSearchScope();
        } else {
            /*
             * Evaluate the EL expression to get the value.
             */
            ELProcessor elp = new ELProcessor();
            elp.getELManager().importClass(LdapSearchScope.class.getCanonicalName());
            Object obj = elp.eval(idStoreDefinition.callerSearchScopeExpression());
            if (obj instanceof LdapSearchScope) {
                scope = (LdapSearchScope) obj;
            } else {
                throw new IllegalArgumentException("Expected 'callerSearchScopeExpression' to evaluate to an LdapSearchScope type.");
            }
        }

        return scope;
    }

    /**
     * Validate and return the groupMemberAttribute.
     *
     * @return The validated groupMemberAttribute.
     */
    private String setGroupMemberAttribute() {
        return processString("groupMemberAttribute", idStoreDefinition.groupMemberAttribute());
    }

    /**
     * Validate and return the groupMemberOfAttribute.
     *
     * @return The validated groupMemberOfAttribute.
     */
    private String setGroupMemberOfAttribute() {
        return processString("groupMemberOfAttribute", idStoreDefinition.groupMemberOfAttribute());
    }

    /**
     * Validate and return the groupNameAttribute.
     *
     * @return The validated groupNameAttribute.
     */
    private String setGroupNameAttribute() {
        return processString("groupNameAttribute", idStoreDefinition.groupNameAttribute());
    }

    /**
     * Validate and return the groupSearchBase.
     *
     * @return The validated groupSearchBase.
     */
    private String setGroupSearchBase() {
        return processString("groupSearchBase", idStoreDefinition.groupSearchBase());
    }

    /**
     * Validate and return the groupSearchFilter.
     *
     * @return The validated groupSearchFilter.
     */
    private String setGroupSearchFilter() {
        return processString("groupSearchFilter", idStoreDefinition.groupSearchFilter());
    }

    /**
     * Validate and return the groupSearchScope.
     *
     * @return The validated groupSearchScope
     */
    private LdapSearchScope setGroupSearchScope() {
        LdapSearchScope scope;

        /*
         * The expression language value takes precedence over the direct setting.
         */
        if (idStoreDefinition.groupSearchScopeExpression().isEmpty()) {
            /*
             * Direct setting.
             */
            scope = idStoreDefinition.groupSearchScope();
        } else {
            /*
             * Evaluate the EL expression to get the value.
             */
            ELProcessor elp = new ELProcessor();
            elp.getELManager().importClass(LdapSearchScope.class.getCanonicalName());
            Object obj = elp.eval(idStoreDefinition.groupSearchScopeExpression());
            if (obj instanceof LdapSearchScope) {
                scope = (LdapSearchScope) obj;
            } else {
                throw new IllegalArgumentException("Expected 'groupSearchScopeExpression' to evaluate to an LdapSearchScope type.");
            }
        }

        return scope;
    }

    /**
     * Validate and return the maxResults.
     *
     * @return The validated maxResults
     */
    private int setMaxResults() {
        int maxResults;

        /*
         * The expression language value takes precedence over the direct setting.
         */
        if (idStoreDefinition.maxResultsExpression().isEmpty()) {
            /*
             * Direct setting.
             */
            maxResults = idStoreDefinition.maxResults();
        } else {
            /*
             * Evaluate the EL expression to get the value.
             */
            ELProcessor elp = new ELProcessor();
            Object obj = elp.eval(idStoreDefinition.maxResultsExpression());
            if (obj instanceof Number) {
                maxResults = ((Number) obj).intValue();
            } else {
                throw new IllegalArgumentException("Expected 'maxResultsExpression' to evaluate to an integer value.");
            }
        }

        return maxResults;
    }

    /**
     * Validate and return the priority either from the EL expression or the direct priority setting.
     *
     * @return The validated priority.
     */
    private int setPriority() {
        int priority;

        /*
         * The expression language value takes precedence over the direct setting.
         */
        if (idStoreDefinition.priorityExpression().isEmpty()) {
            /*
             * Direct setting.
             */
            priority = idStoreDefinition.priority();
        } else {
            /*
             * Evaluate the EL expression to get the value.
             */
            ELProcessor elp = new ELProcessor();
            Object obj = elp.eval(idStoreDefinition.priorityExpression());
            if (obj instanceof Number) {
                priority = ((Number) obj).intValue();
            } else {
                throw new IllegalArgumentException("Expected 'priorityExpression' to evaluate to an integer value.");
            }
        }

        return priority;
    }

    /**
     * Validate and return the readTimeout.
     *
     * @return The validated readTimeout
     */
    private int setReadTimeout() {
        int readTimeout;

        /*
         * The expression language value takes precedence over the direct setting.
         */
        if (idStoreDefinition.readTimeoutExpression().isEmpty()) {
            /*
             * Direct setting.
             */
            readTimeout = idStoreDefinition.readTimeout();
        } else {
            /*
             * Evaluate the EL expression to get the value.
             */
            ELProcessor elp = new ELProcessor();
            Object obj = elp.eval(idStoreDefinition.readTimeoutExpression());
            if (obj instanceof Number) {
                readTimeout = ((Number) obj).intValue();
            } else {
                throw new IllegalArgumentException("Expected 'readTimeoutExpression' to evaluate to an integer value.");
            }
        }

        return readTimeout;
    }

    /**
     * Validate and return the url.
     *
     * @return The validated url.
     */
    private String setUrl() {
        return processString("url", idStoreDefinition.url());
    }

    /**
     * Validate and return the {@link ValidationType}s for the {@link IdentityStore} from either
     * the EL expression or the direct useFor setting.
     *
     * @return The validated useFor types.
     */
    private Set<ValidationType> setUseFor() {
        Set<ValidationType> types = null;

        /*
         * The expression language value takes precedence over the direct setting.
         */
        if (idStoreDefinition.useForExpression().isEmpty()) {
            types = new HashSet<ValidationType>(Arrays.asList(idStoreDefinition.useFor()));
        } else {
            /*
             * Evaluate the EL expression to get the value.
             */
            ELProcessor elp = new ELProcessor();
            elp.getELManager().importClass(ValidationType.class.getCanonicalName());
            Object obj = elp.eval(idStoreDefinition.useForExpression());
            if (obj instanceof Object[]) {
                types = new HashSet(Arrays.asList(obj));
            } else if (obj instanceof Set) {
                types = (Set<ValidationType>) obj;
            } else {
                throw new IllegalArgumentException("Expected 'useForExpression' to evaluate to a Set<ValidationType>.");
            }
        }

        if (types == null || types.isEmpty()) {
            throw new IllegalArgumentException("The identity store must be configured with at least one ValidationType.");
        }
        return Collections.unmodifiableSet(types);
    }
}
