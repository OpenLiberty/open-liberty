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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.el.ELException;
import javax.el.ELProcessor;
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

    /** The query to use to lookup users/callers. */
    private final String callerQuery;

    /** The data source lookup. */
    private final String dataSourceLookup;

    /** The query to use to lookup groups. */
    private final String groupsQuery;

    /** The hashing algorithm class to use. */
    private final Class<? extends PasswordHash> hashAlgorithm;

    /** Parameters to configure the hash algorithm with. */
    private final List<String> hashAlgorithmParameters;

    /** The definitions for this IdentityStore. */
    private final DatabaseIdentityStoreDefinition idStoreDefinition;

    /** The priority for this IdentityStore. */
    private final int priority;

    /** The ValidationTypes this IdentityStore can be used for. */
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
         * Set all configuration. We do this in the constructor instead of on retrieval
         * in order to fail-fast.
         */
        this.callerQuery = setCallerQuery();
        this.dataSourceLookup = setDataSourceLookup();
        this.groupsQuery = setGroupsQuery();
        this.hashAlgorithm = setHashAlgorithm();
        this.hashAlgorithmParameters = setHashAlgorithmParameters();
        this.priority = setPriority();
        this.useFor = setUseFor();
    }

    /**
     * Get the user/caller query for the {@link IdentityStore}.
     *
     * @return The user/caller query.
     *
     * @see DatabaseIdentityStoreDefinition#callerQuery()
     */
    String getCallerQuery() {
        return this.callerQuery;
    }

    /**
     * Get the datasource lookup for the {@link IdentityStore}.
     *
     * @return The datasource lookup.
     *
     * @see DatabaseIdentityStoreDefinition#dataSourceLookup()
     */
    String getDataSourceLookup() {
        return dataSourceLookup;
    }

    /**
     * Get the groups query for the {@link IdentityStore}.
     *
     * @return The groups query.
     *
     * @see DatabaseIdentityStoreDefinition#groupsQuery()
     */
    String getGroupsQuery() {
        return groupsQuery;
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
        return this.priority;
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
        return this.useFor;
    }

    /**
     * This method will process a configuration value for any configuration setting in
     * {@link DatabaseIdentityStoreDefinition} that is a string and whose name is NOT a
     * "*Expression". It will first check to see if it is a EL expression. It it is, it
     * will return the evaluated expression; otherwise, it will return the literal String.
     *
     * @param name The name of the property. Used for error messages.
     * @param value The value returned from from the {@link DatabaseIdentityStoreDefinition}, which can
     *            either be a literal String or an EL expression.
     * @return The String value.
     */
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
     * Validate and return the callerQuery.
     *
     * @return The validated callerQuery.
     */
    private String setCallerQuery() {
        return processString("callerQuery", idStoreDefinition.callerQuery());
    }

    /**
     * Validate and return the dataSourceLookup.
     *
     * @return The validated dataSourceLookup.
     */
    private String setDataSourceLookup() {
        return processString("dataSourceLookup", idStoreDefinition.dataSourceLookup());
    }

    /**
     * Validate and return the groupsQuery.
     *
     * @return The validated groupsQuery.
     */
    private String setGroupsQuery() {
        return processString("groupsQuery", idStoreDefinition.groupsQuery());
    }

    /**
     * Validate and return the hashAlgorithm.
     *
     * @return The validated hashAlgorithm.
     */
    private Class<? extends PasswordHash> setHashAlgorithm() {
        return idStoreDefinition.hashAlgorithm(); // TODO Not sure this is correct.
    }

    /**
     * Validate and return the hashAlgorithmParameters.
     *
     * @return The validated hashAlgorithmParameters.
     */
    private List<String> setHashAlgorithmParameters() {
        List<String> parameters = new ArrayList<String>();

        String[] config = idStoreDefinition.hashAlgorithmParameters();
        if (config != null && config.length > 0) {
            for (int idx = 0; idx < config.length; idx++) {
                String value = config[idx];
                parameters.add(processString("hashAlgorithmParameters[" + idx + "]", value));
            }
        }

        return parameters;
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
