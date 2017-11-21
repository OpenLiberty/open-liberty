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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStore.ValidationType;
import javax.security.enterprise.identitystore.PasswordHash;

/**
 * A wrapper class that offers convenience methods for retrieving configuration
 * from an {@link DatabaseIdentityStoreDefinition} instance.
 *
 * <p/>
 * The methods in this class will evaluate any EL expressions provided in the
 * {@link DatabaseIdentityStoreDefinition} first and if no EL expressions are provided,
 * return the literal value instead.
 */
public class DatabaseIdentityStoreDefinitionWrapper {

    /** The query to use to lookup users/callers. Will be null when set by a deferred EL expression. */
    private final String callerQuery;

    /** The data source lookup. Will be null when set by a deferred EL expression. */
    private final String dataSourceLookup;

    /** The query to use to lookup groups. Will be null when set by a deferred EL expression. */
    private final String groupsQuery;

    /** The hashing algorithm class to use. Will be null when set by a deferred EL expression. */
    private final Class<? extends PasswordHash> hashAlgorithm;

    /** Parameters to configure the hash algorithm with. Will be null when set by a deferred EL expression. */
    private final List<String> hashAlgorithmParameters;

    /** The definitions for this IdentityStore. */
    private final DatabaseIdentityStoreDefinition idStoreDefinition;

    /** The priority for this IdentityStore. Will be null when set by a deferred EL expression. */
    private final Integer priority;

    /** The ValidationTypes this IdentityStore can be used for. Will be null when set by a deferred EL expression. */
    private final Set<ValidationType> useFor;

    /**
     * Create a new instance of an {@link DatabaseIdentityStoreDefinitionWrapper} that will provide
     * convenience methods to access configuration from the {@link DatabaseIdentityStoreDefinition}
     * instance.
     *
     * @param idStoreDefinition The {@link DatabaseIdentityStoreDefinition} to wrap.
     */
    DatabaseIdentityStoreDefinitionWrapper(DatabaseIdentityStoreDefinition idStoreDefinition) {
        /*
         * Ensure we were passed a non-null definition.
         */
        if (idStoreDefinition == null) {
            throw new IllegalArgumentException("The DatabaseIdentityStoreDefinition cannot be null.");
        }
        this.idStoreDefinition = idStoreDefinition;

        /*
         * Evaluate the configuration. The values will be non-null if the setting is NOT
         * a deferred EL expression. If it is a deferred EL expression, we will dynamically
         * evaluate it at call time.
         */
        this.callerQuery = evaluateCallerQuery(true);
        this.dataSourceLookup = evaluateDataSourceLookup(true);
        this.groupsQuery = evaluateGroupsQuery(true);
        this.hashAlgorithm = evaluateHashAlgorithm();
        this.hashAlgorithmParameters = evaluateHashAlgorithmParameters();
        this.priority = evaluatePriority(true);
        this.useFor = evaluateUseFor(true);
    }

    /**
     * Evaluate and return the callerQuery.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The callerQuery or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    private String evaluateCallerQuery(boolean immediateOnly) {
        return ELHelper.processString("callerQuery", idStoreDefinition.callerQuery(), immediateOnly);
    }

    /**
     * Evaluate and return the dataSourceLookup.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The dataSourceLookup or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    private String evaluateDataSourceLookup(boolean immediateOnly) {
        return ELHelper.processString("dataSourceLookup", idStoreDefinition.dataSourceLookup(), immediateOnly);
    }

    /**
     * Evaluate and return the groupsQuery.
     *
     * @param immediateOnly If true, only return a non-null value if the setting is either an
     *            immediate EL expression or not set by an EL expression. If false, return the
     *            value regardless of where it is evaluated.
     * @return The groupsQuery or null if immediateOnly==true AND the value is not evaluated
     *         from a deferred EL expression.
     */
    private String evaluateGroupsQuery(boolean immediateOnly) {
        return ELHelper.processString("groupsQuery", idStoreDefinition.groupsQuery(), immediateOnly);
    }

    /**
     * Evaluate and return the hashAlgorithm.
     *
     * @return The hashAlgorithm.
     */
    private Class<? extends PasswordHash> evaluateHashAlgorithm() {
        return idStoreDefinition.hashAlgorithm();
    }

    /**
     * Evaluate and return the hashAlgorithmParameters.
     *
     * @return The hashAlgorithmParameters.
     */
    private List<String> evaluateHashAlgorithmParameters() {
        List<String> parameters = new ArrayList<String>();

        String[] config = idStoreDefinition.hashAlgorithmParameters();
        if (config != null && config.length > 0) {
            for (int idx = 0; idx < config.length; idx++) {
                String value = config[idx];
                parameters.add(ELHelper.processString("hashAlgorithmParameters[" + idx + "]", value, false));
            }
        }

        return parameters;
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
    private Integer evaluatePriority(boolean immediateOnly) {
        return ELHelper.processInt("priorityExpression", this.idStoreDefinition.priorityExpression(), this.idStoreDefinition.priority(), immediateOnly);
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
    private Set<ValidationType> evaluateUseFor(boolean immediateOnly) {
        return ELHelper.processUseFor(this.idStoreDefinition.useForExpression(), this.idStoreDefinition.useFor(), immediateOnly);
    }

    /**
     * Get the user/caller query for the {@link IdentityStore}.
     *
     * @return The user/caller query.
     *
     * @see DatabaseIdentityStoreDefinition#callerQuery()
     */
    String getCallerQuery() {
        return (this.callerQuery != null) ? this.callerQuery : evaluateCallerQuery(false);
    }

    /**
     * Get the datasource lookup for the {@link IdentityStore}.
     *
     * @return The datasource lookup.
     *
     * @see DatabaseIdentityStoreDefinition#dataSourceLookup()
     */
    String getDataSourceLookup() {
        return (dataSourceLookup != null) ? this.dataSourceLookup : evaluateDataSourceLookup(false);
    }

    /**
     * Get the groups query for the {@link IdentityStore}.
     *
     * @return The groups query.
     *
     * @see DatabaseIdentityStoreDefinition#groupsQuery()
     */
    String getGroupsQuery() {
        return (groupsQuery != null) ? this.groupsQuery : evaluateGroupsQuery(false);
    }

    /**
     * Get the hash algorithm for the {@link IdentityStore}.
     *
     * @return The hash algorithm.
     *
     * @see DatabaseIdentityStoreDefinition#hashAlgorithm()
     */
    Class<? extends PasswordHash> getHashAlgorithm() {
        return hashAlgorithm;
    }

    /**
     * Get the hash algorithm parameters for the {@link IdentityStore}.
     *
     * @return The hash algorithm parameters.
     *
     * @see DatabaseIdentityStoreDefinition#hashAlgorithmParameters()
     */
    List<String> getHashAlgorithmParameters() {
        return hashAlgorithmParameters;
    }

    /**
     * Get the priority for the {@link IdentityStore}.
     *
     * @return The priority.
     *
     * @see DatabaseIdentityStoreDefinition#priority()
     * @see DatabaseIdentityStoreDefinition#priorityExpression()
     */
    int getPriority() {
        return (this.priority != null) ? this.priority : evaluatePriority(false);
    }

    /**
     * Get the useFor for the {@link IdentityStore}.
     *
     * @return The useFor.
     *
     * @see DatabaseIdentityStoreDefinition#useFor()
     * @see DatabaseIdentityStoreDefinition#useForExpression()
     */
    Set<ValidationType> getUseFor() {
        return (this.useFor != null) ? this.useFor : evaluateUseFor(false);
    }
}
