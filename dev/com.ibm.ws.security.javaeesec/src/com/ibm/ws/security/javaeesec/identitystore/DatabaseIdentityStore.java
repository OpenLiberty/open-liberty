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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.CDI;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.enterprise.credential.Credential;
import javax.security.enterprise.credential.UsernamePasswordCredential;
import javax.security.enterprise.identitystore.CredentialValidationResult;
import javax.security.enterprise.identitystore.DatabaseIdentityStoreDefinition;
import javax.security.enterprise.identitystore.IdentityStore;
import javax.security.enterprise.identitystore.IdentityStorePermission;
import javax.security.enterprise.identitystore.PasswordHash;
import javax.sql.DataSource;

import com.ibm.websphere.ras.ProtectedString;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.javaeesec.JavaEESecConstants;

/**
 * Liberty's database {@link IdentityStore} implementation.
 */
@Default
@ApplicationScoped
public class DatabaseIdentityStore implements IdentityStore {

    private static final TraceComponent tc = Tr.register(DatabaseIdentityStore.class);

    /** The definitions for this IdentityStore. */
    private final DatabaseIdentityStoreDefinitionWrapper idStoreDefinition;

    private final PasswordHash passwordHash;

    /**
     * Construct a new {@link DatabaseIdentityStore} instance using the specified definitions.
     *
     * @param idStoreDefinition The definitions to use to configure the {@link IdentityStore}.
     */
    public DatabaseIdentityStore(DatabaseIdentityStoreDefinition idStoreDefinition) {
        this.idStoreDefinition = new DatabaseIdentityStoreDefinitionWrapper(idStoreDefinition);

        Instance<? extends PasswordHash> p2phi = CDI.current().select(this.idStoreDefinition.getHashAlgorithm());
        if (p2phi != null) {
            passwordHash = p2phi.get();
        } else {
            throw new IllegalArgumentException("Cannot load the password HashAlgorithm, the CDI bean was not found for: " + this.idStoreDefinition.getHashAlgorithm());
        }
        List<String> params = this.idStoreDefinition.getHashAlgorithmParameters();
        if (params != null && !params.isEmpty()) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Processing HashAlgorithmParameters.");
            }
            Map<String, String> prepped = new HashMap<String, String>(params.size());
            for (String param : params) {
                String[] split = param.split("=");
                if (split.length != 2) {
                    throw new IllegalArgumentException("Hash algorithm parameter is in the incorrect format. Expected: name=value,  recevied: " + param);
                }
                prepped.put(split[0], split[1]);
            }
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Processed HashAlgorithmParameters: " + prepped);
            }

            passwordHash.initialize(prepped);
        }
    }

    @Override
    public Set<String> getCallerGroups(CredentialValidationResult validationResult) {
        Set<String> groups = new HashSet<String>();
        if (!validationTypes().contains(ValidationType.PROVIDE_GROUPS)) {
            return groups;
        }
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(new IdentityStorePermission(JavaEESecConstants.GET_GROUPS_PERMISSION));
        }

        String caller = validationResult.getCallerPrincipal().getName();
        if (caller == null) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Caller is null, cannot get groups.");
            }
            return groups;
        }
        PreparedStatement prep = null;
        try {
            Connection conn = getConnection(caller);
            try {
                prep = conn.prepareStatement(idStoreDefinition.getGroupsQuery());
                prep.setString(1, caller);
                ResultSet result = runQuery(prep, caller);

                while (result.next()) {
                    String aGroup = result.getString(1);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "For caller " + caller + " found " + aGroup);
                    }
                    if (aGroup != null) {
                        groups.add(aGroup);
                    }
                }

            } finally {
                conn.close();
            }
        } catch (NamingException | SQLException e) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Exception getting groups for caller:" + caller, e);
            }
        }

        /*
         * Currently, we're allowing partial results to be returned if we somehow fail
         * while processing the results
         */
        return groups;
    }

    @Override
    public int priority() {
        return idStoreDefinition.getPriority();
    }

    @Override
    @Sensitive
    public CredentialValidationResult validate(Credential credential) {
        if (!validationTypes().contains(ValidationType.VALIDATE)) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }

        if (!(credential instanceof UsernamePasswordCredential)) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Credential was not UsernamePasswordCredential");
            }
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }

        UsernamePasswordCredential cred = (UsernamePasswordCredential) credential;
        String caller = cred.getCaller();
        if (caller == null) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Caller is null, cannot validate credential.");
            }
            return CredentialValidationResult.INVALID_RESULT;
        }

        if (cred.getPassword().getValue() == null) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Password is null, cannot validate credential.");
            }
            return CredentialValidationResult.INVALID_RESULT;
        }

        ProtectedString dbPassword = null;
        PreparedStatement prep = null;
        try {

            Connection conn = getConnection(caller);
            try {
                prep = conn.prepareStatement(idStoreDefinition.getCallerQuery());
                prep.setString(1, caller);

                ResultSet result = runQuery(prep, caller);

                if (!result.next()) { // advance to first result
                    if (tc.isEventEnabled()) {
                        Tr.event(tc, "No users returned for caller: " + caller + ", using query: " + idStoreDefinition.getCallerQuery());
                    }
                    return CredentialValidationResult.INVALID_RESULT;
                }

                String dbreturn = result.getString(1);
                if (dbreturn == null) {
                    if (tc.isEventEnabled()) {
                        Tr.event(tc, "The password returned from database is null for caller: " + caller);
                    }
                    return CredentialValidationResult.INVALID_RESULT;
                }
                dbPassword = new ProtectedString(dbreturn.toCharArray());

                if (result.next()) { // check if there are additional results.
                    if (tc.isEventEnabled()) {
                        Tr.event(tc, "Multiple results returned for caller: " + caller);
                    }
                    return CredentialValidationResult.INVALID_RESULT;
                }

            } finally {
                conn.close();
            }
        } catch (NamingException | SQLException e) {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Exception validating caller: " + caller, e);
            }
            return CredentialValidationResult.INVALID_RESULT;
        }

        if (passwordHash.verify(cred.getPassword().getValue(), String.valueOf(dbPassword.getChars()))) {
            Set<String> groups = getCallerGroups(new CredentialValidationResult(null, caller, caller, caller, null));
            return new CredentialValidationResult(idStoreDefinition.getDataSourceLookup(), caller, caller, caller, groups);
        } else {
            if (tc.isEventEnabled()) {
                Tr.event(tc, "PasswordHash verify check returned false for caller: " + caller);
            }
            return CredentialValidationResult.INVALID_RESULT;
        }
    }

    @Override
    public Set<ValidationType> validationTypes() {
        return idStoreDefinition.getUseFor();
    }

    private ResultSet runQuery(PreparedStatement prep, String caller) throws SQLException {
        long startTime = System.currentTimeMillis();
        ResultSet result = prep.executeQuery();
        long endTime = System.currentTimeMillis();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Time to run query on caller " + caller + ". Start time: " + startTime + ". End time: " + endTime + ". Total time in ms: " + (endTime - startTime));
        }
        return result;
    }

    private Connection getConnection(String caller) throws NamingException, SQLException {
        // datasource could be an expression, look it up fresh everytime.
        DataSource dataSource = (DataSource) new InitialContext().lookup(idStoreDefinition.getDataSourceLookup());

        return dataSource.getConnection();
    }
}
