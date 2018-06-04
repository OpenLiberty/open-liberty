/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.javaeesec.identitystore;

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.HashSet;
import java.util.Set;

import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStore.ValidationType;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition;
import javax.security.enterprise.identitystore.LdapIdentityStoreDefinition.LdapSearchScope;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
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

    private static final TraceComponent tc = Tr.register(LdapIdentityStoreDefinitionWrapper.class);

    /** The distinguished name to bind with for this IdentityStore. Will be null when set by a deferred EL expression. */
    private final String bindDn;

    /** The distinguished name's password to bind with for this IdentityStore. Will be null when set by a deferred EL expression. */
    private final ProtectedString bindDnPassword;

    /** The base distinguished name for users/callers. Will be null when set by a deferred EL expression. */
    private final String callerBaseDn;

    /** The LDAP attribute that contains the user/caller name. Will be null when set by a deferred EL expression. */
    private final String callerNameAttribute;

    /** The search base to search for the user/caller on the LDAP server. Will be null when set by a deferred EL expression. */
    private final String callerSearchBase;

    /** The LDAP filter to search for the user/caller on the LDAP server. Will be null when set by a deferred EL expression. */
    private final String callerSearchFilter;

    /** The search scope to search for the user/caller. */
    private final LdapSearchScope callerSearchScope;

    /** The LDAP attribute to use to search for group membership on a group entity. Will be null when set by a deferred EL expression. */
    private final String groupMemberAttribute;

    /** The LDAP attribute to use to search for group membership on a user/caller entity. Will be null when set by a deferred EL expression. */
    private final String groupMemberOfAttribute;

    /** The LDAP attribute to retrieve the group name from. Will be null when set by a deferred EL expression. */
    private final String groupNameAttribute;

    /** The search base to search for groups on the LDAP server. Will be null when set by a deferred EL expression. */
    private final String groupSearchBase;

    /** The LDAP filter to search for groups on the LDAP server. Will be null when set by a deferred EL expression. */
    private final String groupSearchFilter;

    /** The search scope to search for groups. Will be null when set by a deferred EL expression. */
    private final LdapSearchScope groupSearchScope;

    /** The definitions for this IdentityStore. */
    private final LdapIdentityStoreDefinition idStoreDefinition;

    /** The maximum number of results to return on a search. Will be null when set by a deferred EL expression. */
    private final Integer maxResults;

    /** The priority for this IdentityStore. Will be null when set by a deferred EL expression. */
    private final Integer priority;

    /** The read timeout for LDAP contexts. Will be null when set by a deferred EL expression. */
    private final Integer readTimeout;

    /** The URL for the LDAP server to connect to. Will be null when set by a deferred EL expression. */
    private final String url;

    /** The ValidationTypes this IdentityStore can be used for. Will be null when set by a deferred EL expression. */
    private final Set<ValidationType> useFor;

    private final ELHelper elHelper;

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

        this.elHelper = new ELHelper();

        /*
         * Evaluate the configuration. The values will be non-null if the setting is NOT
         * a deferred EL expression. If it is a deferred EL expression, we will dynamically
         * evaluate it at call time.
         */
        this.bindDn = evaluateBindDn(true);
        this.bindDnPassword = evaluateBindDnPassword(true);
        this.callerBaseDn = evaluateCallerBaseDn(true);
        this.callerNameAttribute = evaluateCallerNameAttribute(true);
        this.callerSearchBase = evaluateCallerSearchBase(true);
        this.callerSearchFilter = evaluateCallerSearchFilter(true);
        this.callerSearchScope = evaluateCallerSearchScope(true);
        this.groupMemberAttribute = evaluateGroupMemberAttribute(true);
        this.groupMemberOfAttribute = evaluateGroupMemberOfAttribute(true);
        this.groupNameAttribute = evaluateGroupNameAttribute(true);
        this.groupSearchBase = evaluateGroupSearchBase(true);
        this.groupSearchFilter = evaluateGroupSearchFilter(true);
        this.groupSearchScope = evaluateGroupSearchScope(true);
        this.maxResults = evaluateMaxResults(true);
        this.priority = evaluatePriority(true);
        this.readTimeout = evaluateReadTimeout(true);
        this.url = evaluateUrl(true);
        this.useFor = evaluateUseFor(true);
    }

    /**
     * Evaluate and return the bindDn.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The bindDn or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private String evaluateBindDn(boolean immediateOnly) {
        try {
            return elHelper.processString("bindDn", this.idStoreDefinition.bindDn(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAVAEESEC_WARNING_IDSTORE_CONFIG", new Object[] { "bindDn", "" });
            }
            return ""; /* Default value from spec. */
        }
    }

    /**
     * Evaluate and return the bindDnPassword.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The bindDnPassword or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private ProtectedString evaluateBindDnPassword(boolean immediateOnly) {
        String result;
        try {
            result = elHelper.processString("bindDnPassword", this.idStoreDefinition.bindDnPassword(), immediateOnly, true);

        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAVAEESEC_WARNING_IDSTORE_CONFIG", new Object[] { "bindDnPassword", "" });
            }
            result = ""; /* Default value from spec. */
        }
        return (result == null) ? null : new ProtectedString(result.toCharArray());
    }

    /**
     * Evaluate and return the callerBaseDn.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The callerBaseDn or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private String evaluateCallerBaseDn(boolean immediateOnly) {
        try {
            return elHelper.processString("callerBaseDn", this.idStoreDefinition.callerBaseDn(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAVAEESEC_WARNING_IDSTORE_CONFIG", new Object[] { "callerBaseDn", "" });
            }
            return ""; /* Default value from spec. */
        }
    }

    /**
     * Evaluate and return the callerNameAttribute.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The callerNameAttribute or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private String evaluateCallerNameAttribute(boolean immediateOnly) {
        try {
            return elHelper.processString("callerNameAttribute", this.idStoreDefinition.callerNameAttribute(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAVAEESEC_WARNING_IDSTORE_CONFIG", new Object[] { "callerNameAttribute", "uid" });
            }
            return "uid"; /* Default value from spec. */
        }
    }

    /**
     * Evaluate and return the callerSearchBase.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The callerSearchBase or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private String evaluateCallerSearchBase(boolean immediateOnly) {
        try {
            return elHelper.processString("callerSearchBase", this.idStoreDefinition.callerSearchBase(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAVAEESEC_WARNING_IDSTORE_CONFIG", new Object[] { "callerSearchBase", "" });
            }
            return ""; /* Default value from spec. */
        }
    }

    /**
     * Evaluate and return the callerSearchFilter.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The callerSearchFilter or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private String evaluateCallerSearchFilter(boolean immediateOnly) {
        try {
            return elHelper.processString("callerSearchFilter", this.idStoreDefinition.callerSearchFilter(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAVAEESEC_WARNING_IDSTORE_CONFIG", new Object[] { "callerSearchFilter", "" });
            }
            return ""; /* Default value from spec. */
        }
    }

    /**
     * Evaluate and return the callerSearchScope.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The callerSearchScope or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private LdapSearchScope evaluateCallerSearchScope(boolean immediateOnly) {
        try {
            return elHelper.processLdapSearchScope("callerSearchScopeExpression", this.idStoreDefinition.callerSearchScopeExpression(), this.idStoreDefinition.callerSearchScope(),
                                                   immediateOnly);
        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAVAEESEC_WARNING_IDSTORE_CONFIG", new Object[] { "callerSearchScope/callerSearchScopeExpression", "LdapSearchScope.SUBTREE" });
            }
            return LdapSearchScope.SUBTREE; /* Default value from spec. */
        }
    }

    /**
     * Evaluate and return the groupMemberAttribute.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The groupMemberAttribute or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private String evaluateGroupMemberAttribute(boolean immediateOnly) {
        try {
            return elHelper.processString("groupMemberAttribute", this.idStoreDefinition.groupMemberAttribute(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAVAEESEC_WARNING_IDSTORE_CONFIG", new Object[] { "groupMemberAttribute", "member" });
            }
            return "member"; /* Default value from spec. */
        }
    }

    /**
     * Evaluate and return the groupMemberOfAttribute.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The groupMemberOfAttribute or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private String evaluateGroupMemberOfAttribute(boolean immediateOnly) {
        try {
            return elHelper.processString("groupMemberOfAttribute", this.idStoreDefinition.groupMemberOfAttribute(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAVAEESEC_WARNING_IDSTORE_CONFIG", new Object[] { "groupMemberOfAttribute", "memberOf" });
            }
            return "memberOf"; /* Default value from spec. */
        }
    }

    /**
     * Evaluate and return the groupNameAttribute.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The groupNameAttribute or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private String evaluateGroupNameAttribute(boolean immediateOnly) {
        try {
            return elHelper.processString("groupNameAttribute", this.idStoreDefinition.groupNameAttribute(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAVAEESEC_WARNING_IDSTORE_CONFIG", new Object[] { "groupNameAttribute", "cn" });
            }
            return "cn"; /* Default value from spec. */
        }
    }

    /**
     * Evaluate and return the groupSearchBase.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The groupSearchBase or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private String evaluateGroupSearchBase(boolean immediateOnly) {
        try {
            return elHelper.processString("groupSearchBase", this.idStoreDefinition.groupSearchBase(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAVAEESEC_WARNING_IDSTORE_CONFIG", new Object[] { "groupSearchBase", "" });
            }
            return ""; /* Default value from spec. */
        }
    }

    /**
     * Evaluate and return the groupSearchFilter.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The groupSearchFilter or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private String evaluateGroupSearchFilter(boolean immediateOnly) {
        try {
            return elHelper.processString("groupSearchFilter", this.idStoreDefinition.groupSearchFilter(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "There was an error resolving the '{1}' configuration object. Ensure any EL expressions are resolveable. The value will be defaulted to '{2}'",
                           new Object[] { "groupSearchFilter", "" });
            }
            return ""; /* Default value from spec. */
        }
    }

    /**
     * Evaluate and return the groupSearchScope.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The groupSearchScope or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private LdapSearchScope evaluateGroupSearchScope(boolean immediateOnly) {
        try {
            return elHelper.processLdapSearchScope("groupSearchScopeExpression", this.idStoreDefinition.groupSearchScopeExpression(), this.idStoreDefinition.groupSearchScope(),
                                                   immediateOnly);
        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAVAEESEC_WARNING_IDSTORE_CONFIG", new Object[] { "groupSearchScope/groupSearchScopeExpression", "LdapSearchScope.SUBTREE" });
            }
            return LdapSearchScope.SUBTREE; /* Default value from spec. */
        }
    }

    /**
     * Evaluate and return the maxResults.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The maxResults or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private Integer evaluateMaxResults(boolean immediateOnly) {
        try {
            return elHelper.processInt("maxResultsExpression", this.idStoreDefinition.maxResultsExpression(), this.idStoreDefinition.maxResults(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAVAEESEC_WARNING_IDSTORE_CONFIG", new Object[] { "maxResults/maxResultsExpression", "1000" });
            }
            return 1000; /* Default value from spec. */
        }
    }

    /**
     * Evaluate and return the priority.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The priority or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private Integer evaluatePriority(boolean immediateOnly) {
        try {
            return elHelper.processInt("priorityExpression", this.idStoreDefinition.priorityExpression(), this.idStoreDefinition.priority(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAVAEESEC_WARNING_IDSTORE_CONFIG", new Object[] { "priority/priorityExpression", "80" });
            }
            return 80; /* Default value from spec. */
        }
    }

    /**
     * Evaluate and return the readTimeout.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The readTimeout or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private Integer evaluateReadTimeout(boolean immediateOnly) {
        try {
            return elHelper.processInt("readTimeoutExpression", this.idStoreDefinition.readTimeoutExpression(), this.idStoreDefinition.readTimeout(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAVAEESEC_WARNING_IDSTORE_CONFIG", new Object[] { "readTimeout/readTimeoutExpression", "0" });
            }
            return 0; /* Default value from spec. */
        }
    }

    /**
     * Evaluate and return the url.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The url or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private String evaluateUrl(boolean immediateOnly) {
        try {
            return elHelper.processString("url", this.idStoreDefinition.url(), immediateOnly);
        } catch (IllegalArgumentException e) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAVAEESEC_WARNING_IDSTORE_CONFIG", new Object[] { "url", "" });
            }
            return ""; /* Default value from spec. */
        }
    }

    /**
     * Evaluate and return the useFor.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The useFor or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    @FFDCIgnore(IllegalArgumentException.class)
    private Set<ValidationType> evaluateUseFor(boolean immediateOnly) {
        try {
            return elHelper.processUseFor(this.idStoreDefinition.useForExpression(), this.idStoreDefinition.useFor(), immediateOnly);
        } catch (IllegalArgumentException e) {
            Set<ValidationType> values = new HashSet<ValidationType>();
            values.add(ValidationType.PROVIDE_GROUPS); /* Default value from the spec. */
            values.add(ValidationType.VALIDATE); /* Default value from the spec. */

            if (TraceComponent.isAnyTracingEnabled() && tc.isWarningEnabled()) {
                Tr.warning(tc, "JAVAEESEC_WARNING_IDSTORE_CONFIG", new Object[] { "useFor/useForExpression", values });
            }
            return values;
        }
    }

    /**
     * Get the distinguished name to bind to the LDAP server with.
     *
     * @return The distinguished name to bind with.
     *
     * @see LdapIdentityStoreDefinition#bindDn()
     */
    String getBindDn() {
        return (this.bindDn != null) ? this.bindDn : evaluateBindDn(false);
    }

    /**
     * Get the password to bind to the LDAP server with.
     *
     * @return The password to bind with.
     *
     * @see LdapIdentityStoreDefinition#bindDnPassword()
     */
    ProtectedString getBindDnPassword() {
        return (this.bindDnPassword != null) ? this.bindDnPassword : evaluateBindDnPassword(false);
    }

    /**
     * Get the caller base distinguished name.
     *
     * @return The call base distinguished name.
     *
     * @see LdapIdentityStoreDefinition#callerBaseDn()
     */
    String getCallerBaseDn() {
        return (this.callerBaseDn != null) ? this.callerBaseDn : evaluateCallerBaseDn(false);
    }

    /**
     * Get the LDAP attribute to use to find the name of a user/caller entity.
     *
     * @return The user/caller name attribute.
     *
     * @see LdapIdentityStoreDefinition#callerNameAttribute()
     */
    String getCallerNameAttribute() {
        return (this.callerNameAttribute != null) ? this.callerNameAttribute : evaluateCallerNameAttribute(false);
    }

    /**
     * Get the search base to search for user/caller entities.
     *
     * @return The user/caller search base.
     *
     * @see LdapIdentityStoreDefinition#callerSearchBase()
     */
    String getCallerSearchBase() {
        return (this.callerSearchBase != null) ? this.callerSearchBase : evaluateCallerSearchBase(false);
    }

    /**
     * Get the search filter to search for user/caller entities.
     *
     * @return The user/caller search filter.
     *
     * @see LdapIdentityStoreDefinition#callerSearchFilter()
     */
    String getCallerSearchFilter() {
        return (this.callerSearchFilter != null) ? this.callerSearchFilter : evaluateCallerSearchFilter(false);
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
        return this.callerSearchScope != null ? this.callerSearchScope : evaluateCallerSearchScope(false);
    }

    /**
     * Get the LDAP attribute to use to find group membership on a group entity.
     *
     * @return The group member attribute.
     *
     * @see LdapIdentityStoreDefinition#groupMemberAttribute()
     */
    String getGroupMemberAttribute() {
        return (this.groupMemberAttribute != null) ? this.groupMemberAttribute : evaluateGroupMemberAttribute(false);
    }

    /**
     * Get the LDAP attribute to use to find group membership on a user/caller entity.
     *
     * @return The group member of attribute.
     *
     * @see LdapIdentityStoreDefinition#groupMemberOfAttribute()
     */
    String getGroupMemberOfAttribute() {
        return (this.groupMemberOfAttribute != null) ? this.groupMemberOfAttribute : evaluateGroupMemberOfAttribute(false);
    }

    /**
     * Get the LDAP attribute to use to find the name of a group entity.
     *
     * @return The group name attribute.
     *
     * @see LdapIdentityStoreDefinition#groupNameAttribute()
     */
    String getGroupNameAttribute() {
        return (this.groupNameAttribute != null) ? this.groupNameAttribute : evaluateGroupNameAttribute(false);
    }

    /**
     * Get the search base to search for group entities.
     *
     * @return The group search base.
     *
     * @see LdapIdentityStoreDefinition#groupSearchBase()
     */
    String getGroupSearchBase() {
        return (this.groupSearchBase != null) ? this.groupSearchBase : evaluateGroupSearchBase(false);
    }

    /**
     * Get the search filter to search for group entities.
     *
     * @return The group search filter.
     *
     * @see LdapIdentityStoreDefinition#groupSearchFilter()
     */
    String getGroupSearchFilter() {
        return (this.groupSearchFilter != null) ? this.groupSearchFilter : evaluateGroupSearchFilter(false);
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
        return (this.groupSearchScope != null) ? this.groupSearchScope : evaluateGroupSearchScope(false);
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
        return (this.maxResults != null) ? this.maxResults : evaluateMaxResults(false);
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
        return (this.priority != null) ? this.priority : evaluatePriority(false);
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
        return (this.readTimeout != null) ? this.readTimeout : evaluateReadTimeout(false);
    }

    /**
     * Get the URL of the LDAP server to bind to.
     *
     * @return The URL of the LDAP server.
     *
     * @see LdapIdentityStoreDefinition#url()
     */
    String getUrl() {
        return (this.url != null) ? this.url : evaluateUrl(false);
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
        return (this.useFor != null) ? this.useFor : evaluateUseFor(false);
    }

    private boolean isCTS() {
        try {
            return AccessController.doPrivileged(new PrivilegedExceptionAction<Boolean>() {
                @Override
                public Boolean run() throws SecurityException, NullPointerException, IllegalArgumentException {
                    String result = System.getProperty("cts");
                    return result != null && result.equalsIgnoreCase("true");
                }
            });
        } catch (PrivilegedActionException e) {
            return false;
        }
    }

}
