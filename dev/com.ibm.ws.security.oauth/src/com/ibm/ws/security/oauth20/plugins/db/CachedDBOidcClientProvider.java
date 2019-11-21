/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins.db;

import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.error.OidcServerException;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.oauth20.TraceConstants;
import com.ibm.ws.security.oauth20.api.OidcOAuth20ClientProvider;
import com.ibm.ws.security.oauth20.error.impl.BrowserAndServerLogMessage;
import com.ibm.ws.security.oauth20.exception.OAuthDataException;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClient;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClientDBModel;
import com.ibm.ws.security.oauth20.plugins.OidcBaseClientValidator;
import com.ibm.ws.security.oauth20.util.ClientUtils;
import com.ibm.ws.security.oauth20.util.HashSecretUtils;
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;
import com.ibm.ws.security.oauth20.web.RegistrationEndpointServices;

/**
 * JDBC-based client provider with a distributed cachmap used for performance
 *
 */
public class CachedDBOidcClientProvider extends OAuthJDBCImpl implements OidcOAuth20ClientProvider {

    private static final TraceComponent tc = Tr.register(CachedDBOidcClientProvider.class, TraceConstants.TRACE_GROUP, TraceConstants.MESSAGE_BUNDLE);

    private final static String CLASS = CachedDBOidcClientProvider.class.getName();
    private final Logger _log = Logger.getLogger(CLASS);

    private String _componentId;
    private String _tableName;
    // private String clientCacheJndi;
    // private Map<String, OidcBaseClient> _cache;
    private boolean hasRewrites; // URI redirect token substitution
    private final String[] _providerRewrites;
    private DynamicDBMigrator _dbMigrator;
    private boolean migrationAttempted = false;
    private static final String CLIENT_CONFIG_PARAMS = String.format(" (%s, %s, %s, %s, %s, %s, %s)",
            TableConstants.COL_CC2_COMPONENTID,
            TableConstants.COL_CC2_CLIENTID,
            TableConstants.COL_CC2_CLIENTSECRET,
            TableConstants.COL_CC2_DISPLAYNAME,
            TableConstants.COL_CC2_REDIRECTURI,
            TableConstants.COL_CC2_ENABLED,
            TableConstants.COL_CC2_CLIENTMETADATA);

    private final boolean updateXORtoHash = true;
    private String hashType = OAuth20Constants.XOR;
    private final int numIterations = HashSecretUtils.DEFAULT_ITERATIONS;
    private final int keylength = HashSecretUtils.DEFAULT_ITERATIONS;

    public CachedDBOidcClientProvider(String componentId, DataSource dataSource, String tableName, @Sensitive Object[] credentials, String clientCacheJndi, String[] providerRewrites) {
        super(dataSource, credentials);
        _componentId = componentId;
        _tableName = tableName;
        // if (clientCacheJndi == null || "".equals(clientCacheJndi)) {
        // _clientCacheJndi = Constants.DEFAULT_DYNACACHE_JNDI_DB_CLIENTS;
        // } else {
        // _clientCacheJndi = clientCacheJndi;
        // }

        _providerRewrites = providerRewrites != null ? providerRewrites.clone() : null;
        _dbMigrator = new DynamicDBMigrator(_tableName);
    }

    public CachedDBOidcClientProvider(String componentId, DataSource dataSource, String tableName, @Sensitive Object[] credentials, String clientCacheJndi, String[] providerRewrites, String hashType) {
        super(dataSource, credentials);
        _componentId = componentId;
        _tableName = tableName;
        _providerRewrites = providerRewrites != null ? providerRewrites.clone() : null;
        _dbMigrator = new DynamicDBMigrator(_tableName);

        this.hashType = hashType;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Client secret hash type is " + hashType);
        }
    }

    @Override
    public void initialize() {
        String methodName = "initialize";
        _log.entering(CLASS, methodName);
        boolean finestLoggable = _log.isLoggable(Level.FINEST);
        try {
            if (finestLoggable) {
                _log.logp(Level.FINEST, CLASS, methodName, "Using ComponentId: " + _componentId);
            }

            // cache = DynaCacheUtils.getDynamicCache(clientCacheJndi, new String[0], new BaseClient[0]);
            hasRewrites = ClientUtils.initRewrites(_componentId, _providerRewrites);
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }

    @Override
    public void init(OAuthComponentConfiguration config) {
        String methodName = "init";
        _log.entering(CLASS, methodName);

        try {
            super.init(config);

            _tableName = config.getConfigPropertyValue(CONFIG_CLIENT_TABLE);
            _componentId = config.getUniqueId();
            _dbMigrator = new DynamicDBMigrator(_tableName);

            boolean finestLoggable = _log.isLoggable(Level.FINEST);
            if (finestLoggable) {
                _log.logp(Level.FINEST, CLASS,
                        methodName,
                        "Using ComponentId: " + _componentId);
            }

            // String clientCacheJndi = config.getConfigPropertyValue(Constants.DYNACACHE_CONFIG_DB_CLIENTS);
            //
            // if (clientCacheJndi == null || clientCacheJndi.isEmpty()) {
            // clientCacheJndi = Constants.DEFAULT_DYNACACHE_JNDI_DB_CLIENTS;
            // }
            // _cache = DynaCacheUtils.getDynamicCache(clientCacheJndi, new String[0], new OidcBaseClient[0]);

            hasRewrites = ClientUtils.initRewrites(config);
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }

    @Override
    @FFDCIgnore(java.sql.SQLException.class)
    public OidcBaseClient put(OidcBaseClient newClient) throws OidcServerException {
        String methodName = "put";
        _log.entering(CLASS, methodName, new Object[] { newClient });

        Connection conn = null;
        boolean error = true;

        try {
            conn = getInitializedConnection();
            // _cache.put(newClient.getClientId(), newClient);
            addClientToDB(conn, getOidcBaseClientDBModel(_componentId, newClient, 1 /**enabled bit**/
            ));
            error = false;
        } catch (SQLException e) {
            // Try to add missing tables
            attemptDbMigration(conn);

            try {
                // Retry obtaining client
                addClientToDB(conn, getOidcBaseClientDBModel(_componentId, newClient, 1 /**enabled bit**/
                ));
                error = false;
            } catch (SQLException e1) {
                String operation = "INSERT";
                logMessageAndThrowOidcServerException(methodName, e1, "ERROR_PERFORMING_DB_OPERATION", operation, newClient.getClientId());
            }

        } finally {
            closeConnection(conn, error);
        }

        _log.exiting(CLASS, methodName, new Object[] {});

        return (error == true) ? null : newClient;
    }

    private void attemptDbMigration(Connection conn) {
        if (!migrationAttempted) {
            migrationAttempted = true;
            _dbMigrator.execute(conn);

        }
    }

    void addClientToDB(Connection conn, OidcBaseClientDBModel clientDbModel) throws SQLException, OidcServerException {
        String methodName = "addClientToDB";
        _log.entering(CLASS, methodName, new Object[] { clientDbModel });

        String clientSecret = null;
        if (isPBKDF2WithHmacSHA512Configured()) {
            HashSecretUtils.processMetatypeForHashInfo(clientDbModel.getClientMetadata(), clientDbModel.getClientId(), hashType, numIterations, keylength);
            HashSecretUtils.hashClientMetaTypeSecret(clientDbModel.getClientMetadata(), clientDbModel.getClientId(), updateXORtoHash);
            clientSecret = HashSecretUtils.hashSecret(clientDbModel.getClientSecret(), clientDbModel.getClientId(), updateXORtoHash, clientDbModel.getClientMetadata());
        } else if (isXORConfigured()) {
            encodeClientSecretInClientMetadata(clientDbModel);
            clientSecret = PasswordUtil.passwordEncode(clientDbModel.getClientSecret());
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The client secret type is unknown, attempt to hash. " + hashType);
            }
            // to do -- throw an exception here? We don't want to store something unexpected in the database.
            IllegalArgumentException ie = new IllegalArgumentException("Unknown hash type provided, " + hashType + ", the new client cannot be registered: " + clientDbModel.getClientId());
            logMessageAndThrowOidcServerException(methodName, ie, "ERROR_PERFORMING_DB_OPERATION", "INSERT", clientDbModel.getClientId());
        }

        PreparedStatement st = null;

        String QUERY_INSERT = "INSERT INTO " + _tableName
                + CLIENT_CONFIG_PARAMS
                + " VALUES ( ?, ?, ?, ?, ?, ?, ? )";

        try {
            st = conn.prepareStatement(QUERY_INSERT);
            st.setString(1, _componentId);
            st.setString(2, clientDbModel.getClientId());
            st.setString(3, clientSecret);
            st.setString(4, clientDbModel.getDisplayName());
            st.setString(5, clientDbModel.getRedirectUri());
            st.setInt(6, clientDbModel.getEnabled());

            st.setString(7, clientDbModel.getClientMetadata().toString());

            st.executeUpdate();
        } finally {
            closeStatement(st, methodName);

            _log.exiting(CLASS, methodName, null);
        }
    }

    @Override
    @FFDCIgnore(java.sql.SQLException.class)
    public OidcBaseClient get(String clientId) throws OidcServerException {
        String methodName = "get";
        _log.entering(CLASS, methodName, new Object[] { clientId });

        OidcBaseClient result = null;
        Connection conn = null;
        boolean error = true;

        // result = _cache.get(clientId);

        if (result == null) {
            try {
                conn = getInitializedConnection();
                result = getClientFromDB(conn, clientId);
                error = false;
            } catch (SQLException e) {
                // Try to add missing tables
                attemptDbMigration(conn);

                try {
                    // Retry obtaining client
                    result = getClientFromDB(conn, clientId);
                    error = false;
                } catch (SQLException e1) {
                    String operation = "SELECT";
                    logMessageAndThrowOidcServerException(methodName, e1, "ERROR_PERFORMING_DB_OPERATION", operation, clientId);
                }
            } finally {
                closeConnection(conn, error);
            }
        }

        if (hasRewrites && result != null) {
            result = ClientUtils.uriRewrite(result);
        }

        _log.exiting(CLASS, methodName, result);

        return result;
    }

    OidcBaseClient getClientFromDB(Connection conn, String clientId) throws SQLException, OidcServerException {
        return getClientFromDB(conn, clientId, true);
    }

    OidcBaseClient getClientFromDB(Connection conn, String clientId, boolean removeHashInfo) throws SQLException, OidcServerException {
        String methodName = "getClientFromDB";
        _log.entering(CLASS, methodName, new Object[] { clientId, _componentId });

        OidcBaseClient result = null;
        ResultSet queryResults = null;
        PreparedStatement st = null;

        final String QUERY_GET_CLIENTID = "SELECT * FROM " + _tableName + " WHERE "
                + TableConstants.COL_CC2_COMPONENTID + " = ? AND "
                + TableConstants.COL_CC2_CLIENTID + " = ?";

        try {
            st = conn.prepareStatement(QUERY_GET_CLIENTID);
            st.setString(1, _componentId);
            st.setString(2, clientId);

            queryResults = st.executeQuery();

            while (queryResults != null && queryResults.next()) {
                result = getClientFromDBModel(conn, queryResults, removeHashInfo);
                if (result != null) {
                    // _cache.put(result.getClientId(), result);
                    break;
                }
            }
        } finally {
            closeResultSet(queryResults);
            closeStatement(st, methodName);

            _log.exiting(CLASS, methodName, result);
        }

        return result;
    }

    OidcBaseClient getClientFromDBModel(Connection conn, ResultSet queryResults) throws SQLException {
        return getClientFromDBModel(conn, queryResults, true);
    }

    OidcBaseClient getClientFromDBModel(Connection conn, ResultSet queryResults, boolean removeHashInfo) throws SQLException {
        // Obtain DB Model
        OidcBaseClientDBModel clientDBModel = getDBModelOfClient(conn, queryResults, removeHashInfo);

        // Map DB Model to OidcBaseClient
        // Validate and set default facade on errors
        OidcBaseClient client = setDefaultFacade(clientDBModel, getOidcBaseClient(clientDBModel));

        String storedSecret = client.getClientSecret();
        if (storedSecret != null && !storedSecret.isEmpty()) {

            String secretType = PasswordUtil.getCryptoAlgorithm(storedSecret);
            /*
             * Originally, the secret was stored XOR, but now we're storing new secrets as a oneway hash.
             * Only decode if it's still an XORd String
             */
            if (secretType == null || secretType.equals(OAuth20Constants.XOR)) { // type is null on plain text, handled by passwordDecode
                client.setClientSecret(PasswordUtil.passwordDecode(storedSecret));
            } else {
                client.setClientSecret(storedSecret);
            }
        }

        return client;
    }

    OidcBaseClient setDefaultFacade(OidcBaseClientDBModel clientDBModel, OidcBaseClient client) {
        OidcBaseClient deepCopyClient = client.getDeepCopy();

        deepCopyClient.setEnabled(clientDBModel.getEnabled() == 1);
        deepCopyClient.setComponentId(clientDBModel.getComponentId());

        boolean isBlank = isUninitializedClientMetdata(clientDBModel.getClientMetadata());
        if (isBlank) {
            String oldRedirectUri = clientDBModel.getRedirectUri();
            if (!OidcOAuth20Util.isNullEmpty(oldRedirectUri)) {
                deepCopyClient.setRedirectUris(OidcOAuth20Util.initJsonArray(oldRedirectUri));
            }

            String oldSecret = clientDBModel.getClientSecret();
            if (!OidcOAuth20Util.isNullEmpty(oldSecret)) {
                deepCopyClient.setClientSecret(oldSecret);
            }

            String oldClientName = clientDBModel.getDisplayName();
            if (!OidcOAuth20Util.isNullEmpty(oldClientName)) {
                deepCopyClient.setClientName(oldClientName);
            }

            // Set Defaults for Output Params
            deepCopyClient.setClientSecretExpiresAt(0);
            deepCopyClient.setClientIdIssuedAt(0); // Set 0 for those that migrated?

            // Registration URI will need to be computed at a higher level
        }

        return OidcBaseClientValidator.getInstance(deepCopyClient).setDefaultsForOmitted();
    }

    static boolean isUninitializedClientMetdata(JsonObject clientMetadata) {
        return (clientMetadata == null || clientMetadata.isJsonNull() || clientMetadata.entrySet().size() == 0);
    }

    /**
     * Extract DB model of OAUTH20CLIENTCONFIG table from queryResults
     *
     * @param queryResults
     * @return POJO representing the OAUTH20CLIENTCONFIG table
     * @throws SQLException
     * @throws OidcServerException
     */
    OidcBaseClientDBModel getDBModelOfClient(Connection conn, ResultSet queryResults) throws SQLException {
        return getDBModelOfClient(conn, queryResults, true);
    }

    OidcBaseClientDBModel getDBModelOfClient(Connection conn, ResultSet queryResults, boolean removeHashInfoFromMetadata) throws SQLException {
        String componentId = queryResults.getString(TableConstants.COL_CC2_COMPONENTID);
        String clientId = queryResults.getString(TableConstants.COL_CC2_CLIENTID);
        String clientSecret = queryResults.getString(TableConstants.COL_CC2_CLIENTSECRET);
        String displayName = queryResults.getString(TableConstants.COL_CC2_DISPLAYNAME);
        String redirectUri = queryResults.getString(TableConstants.COL_CC2_REDIRECTURI);
        int enabled = queryResults.getInt(TableConstants.COL_CC2_ENABLED);
        JsonObject clientMetadata = null;
        if (getDBType().isClobSupported()) {
            Clob clob = queryResults.getClob("CLIENTMETADATA");
            clientMetadata = (new JsonParser()).parse(clob.getCharacterStream()).getAsJsonObject();
        } else {
            String strClob = queryResults.getString("CLIENTMETADATA");
            clientMetadata = (new JsonParser()).parse(strClob).getAsJsonObject();
        }

        if (removeHashInfoFromMetadata) {
            clientMetadata.remove(OAuth20Constants.HASH_ALGORITHM);
            clientMetadata.remove(OAuth20Constants.SALT);
            clientMetadata.remove(OAuth20Constants.HASH_ITERATIONS);
            clientMetadata.remove(OAuth20Constants.HASH_LENGTH);
        }

        return new OidcBaseClientDBModel(componentId,
                clientId,
                clientSecret,
                displayName,
                redirectUri,
                enabled,
                clientMetadata);
    }

    @Override
    public Collection<OidcBaseClient> getAll() throws OidcServerException {
        String methodName = "getAll";
        _log.entering(CLASS, methodName, new Object[] {});

        Collection<OidcBaseClient> results = getAll(null);

        _log.exiting(CLASS, methodName, results);

        return results;
    }

    @Override
    @FFDCIgnore(java.sql.SQLException.class)
    public Collection<OidcBaseClient> getAll(HttpServletRequest request) throws OidcServerException {
        String methodName = "getAll(request)";
        _log.entering(CLASS, methodName, new Object[] {});

        Collection<OidcBaseClient> results = null;
        Connection conn = null;
        boolean error = true;

        try {
            conn = getInitializedConnection();
            results = findAllClientsFromDB(conn, request);
            error = false;
        } catch (SQLException e) {
            // Try to add missing tables
            attemptDbMigration(conn);

            try {
                // Retry obtaining client
                results = findAllClientsFromDB(conn, request);
                error = false;
            } catch (SQLException e1) {
                logMessageAndThrowOidcServerException(methodName, e1, "ERROR_GETTING_CLIENTS_FROM_DB");
            }
        } finally {
            closeConnection(conn, error);
        }

        if (hasRewrites && results != null) {
            Collection<OidcBaseClient> updatedResults = new ArrayList<OidcBaseClient>();
            for (OidcBaseClient result : results) {
                updatedResults.add(ClientUtils.uriRewrite(result));
            }

            results = updatedResults;
        }

        _log.exiting(CLASS, methodName, results);

        return results;
    }

    Collection<OidcBaseClient> findAllClientsFromDB(Connection conn, HttpServletRequest request) throws SQLException, OidcServerException {
        String methodName = "findAllClientsFromDB(conn,request)";
        _log.entering(CLASS, methodName, new Object[] {});

        Collection<OidcBaseClient> results = new ArrayList<OidcBaseClient>();
        ResultSet queryResults = null;
        PreparedStatement st = null;

        try {
            String query = "SELECT * FROM " + _tableName + " WHERE "
                    + TableConstants.COL_CC2_COMPONENTID + " = ?";

            st = conn.prepareStatement(query);
            st.setString(1, _componentId);

            queryResults = st.executeQuery();

            while (queryResults != null && queryResults.next()) {
                OidcBaseClient result = getClientFromDBModel(conn, queryResults);

                if (request != null /** && (OidcOAuth20Util.isNullEmpty(result.getRegistrationClientUri())) **/
                ) {
                    RegistrationEndpointServices.processClientRegistationUri(result, request);
                }

                results.add(result);
            }
        } finally {
            closeResultSet(queryResults);
            closeStatement(st, methodName);
            _log.exiting(CLASS, methodName, results);
        }
        return results;
    }

    @Override
    @FFDCIgnore(java.sql.SQLException.class)
    public OidcBaseClient update(OidcBaseClient newClient) throws OidcServerException {
        String methodName = "update";
        _log.entering(CLASS, methodName, new Object[] { newClient });

        // _cache.put(newClient.getClientId(), newClient);

        OidcBaseClient retVal = null;
        Connection conn = null;
        boolean error = true;

        try {
            conn = getInitializedConnection();

            if (update(conn, getOidcBaseClientDBModel(_componentId, newClient, 1 /**enabled bit**/
            )) == 1) {
                retVal = newClient;
            }

            error = false;
        } catch (SQLException e) {
            // Try to add missing tables
            attemptDbMigration(conn);

            try {
                // Retry obtaining client
                if (update(conn, getOidcBaseClientDBModel(_componentId, newClient, 1 /**enabled bit**/
                )) == 1) {
                    retVal = newClient;
                }
                error = false;
            } catch (SQLException e1) {
                String operation = "UPDATE";
                logMessageAndThrowOidcServerException(methodName, e1, "ERROR_PERFORMING_DB_OPERATION", operation, newClient.getClientId());
            }

        } finally {
            closeConnection(conn, error);
        }

        _log.exiting(CLASS, methodName, retVal);

        return retVal;
    }

    int update(Connection conn, OidcBaseClientDBModel clientDbModel) throws OidcServerException, SQLException {
        String methodName = "update";
        _log.entering(CLASS, methodName, new Object[] { conn, _componentId, clientDbModel });

        PreparedStatement st = null;
        int retVal = 0;

        String clientSecret = clientDbModel.getClientSecret();

        if (isPBKDF2WithHmacSHA512Configured()) {
            HashSecretUtils.processMetatypeForHashInfo(clientDbModel.getClientMetadata(), clientDbModel.getClientId(), hashType, numIterations, keylength);
            HashSecretUtils.hashClientMetaTypeSecret(clientDbModel.getClientMetadata(), clientDbModel.getClientId(), updateXORtoHash);
            clientSecret = HashSecretUtils.hashSecret(clientDbModel.getClientSecret(), clientDbModel.getClientId(), updateXORtoHash, clientDbModel.getClientMetadata());
        } else if (isXORConfigured()) {
            encodeClientSecretInClientMetadata(clientDbModel);
            clientSecret = PasswordUtil.passwordEncode(clientDbModel.getClientSecret());
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "The client secret type is unknown, attempt to hash. " + hashType);
            }
            // to do -- throw an exception here? We don't want to store something unexpected in the database.
            IllegalArgumentException ie = new IllegalArgumentException("Unknown hash type provided, " + hashType + ", the new client cannot be registered: " + clientDbModel.getClientId());
            logMessageAndThrowOidcServerException(methodName, ie, "ERROR_PERFORMING_DB_OPERATION", "UPDATE", clientDbModel.getClientId());
        }

        String QUERY_UPDATE = "UPDATE " + _tableName + " SET "
                + TableConstants.COL_CC2_COMPONENTID + "=? ,"
                + TableConstants.COL_CC2_CLIENTSECRET + "=? ,"
                + TableConstants.COL_CC2_DISPLAYNAME + "=? ,"
                + TableConstants.COL_CC2_REDIRECTURI + "=? ,"
                + TableConstants.COL_CC2_ENABLED + "=? ,"
                + TableConstants.COL_CC2_CLIENTMETADATA + "=? " + "WHERE "
                + TableConstants.COL_CC2_CLIENTID + " = ? AND "
                + TableConstants.COL_CC2_COMPONENTID + " = ?";
        try {
            st = conn.prepareStatement(QUERY_UPDATE);
            st.setString(1, _componentId);
            st.setString(2, clientSecret);
            st.setString(3, clientDbModel.getDisplayName());
            st.setString(4, clientDbModel.getRedirectUri());
            st.setInt(5, clientDbModel.getEnabled()); // enabled

            // encodeClientSecretInClientMetadata(clientDbModel);

            st.setString(6, clientDbModel.getClientMetadata().toString());
            st.setString(7, clientDbModel.getClientId());
            st.setString(8, _componentId);

            retVal = st.executeUpdate();

            // TODO: If rowcount == 0
            // if (rowCount == 0) {
            // //This can happen if another thread updated or deleted the object concurrently. In this case, only one thread wins;
            // //other threads receive the StaleDataException which should be appropriately handled by the calling app.
            // IMessage message = _messages.message(NLS_STALE_DATA_ON_UPDATE, ClientEntity.class.getName(), newClient.getId());
            // getLogger().debug(message.getServerString());
            // throw new StaleDataException(message.getServerString());
            // }
        } finally {
            closeStatement(st, methodName);
            _log.exiting(CLASS, methodName, retVal);
        }
        return retVal;
    }

    @Override
    public boolean delete(String clientId) throws OidcServerException {
        String methodName = "delete";
        _log.entering(CLASS, methodName, new Object[] { clientId });

        Connection conn = null;
        boolean error = true;

        // weak hashmap, may or may not be in there
        // OidcBaseClient cachedClient = _cache.get(clientId);
        // if (cachedClient != null) {
        // Object removed = _cache.remove(clientId);
        // _log.fine("removed object from cache: " + removed);
        // if (removed == null) {
        // _log.severe("Cannot remove cache object with ID: " + clientId);
        // }
        // }

        try {
            conn = getInitializedConnection();
            error = !deleteClientFromDB(conn, clientId);
        } catch (SQLException e) {
            String operation = "DELETE";
            logMessageAndThrowOidcServerException(methodName, e, "ERROR_PERFORMING_DB_OPERATION", operation, clientId);
        } finally {
            closeConnection(conn, error);
        }

        _log.exiting(CLASS, methodName, error);

        // Return boolean whether delete from DB was successful
        return !error;
    }

    boolean deleteClientFromDB(Connection conn, String clientId) throws SQLException {
        String methodName = "deleteClientFromDB";
        _log.entering(CLASS, methodName, new Object[] { _tableName, _componentId, clientId });

        ResultSet queryResults = null;
        PreparedStatement st = null;
        boolean error = true;

        try {
            final String QUERY_DELETE = "DELETE FROM " + _tableName + " WHERE "
                    + TableConstants.COL_CC2_COMPONENTID + " = ? AND "
                    + TableConstants.COL_CC2_CLIENTID + " = ?";

            st = conn.prepareStatement(QUERY_DELETE);
            st.setString(1, _componentId);
            st.setString(2, clientId);

            int numDeleted = st.executeUpdate();
            _log.logp(Level.FINE, CLASS, methodName, "Num entries deleted: " + numDeleted);

            if (numDeleted > 0) {
                error = false;
            }
        } finally {
            closeResultSet(queryResults);
            closeStatement(st, methodName);

            _log.exiting(CLASS, methodName, null);
        }

        // Return boolean whether delete from DB was successful
        return !error;
    }

    @Override
    @FFDCIgnore(java.sql.SQLException.class)
    public boolean exists(String clientId) throws OidcServerException {
        String methodName = "exists";
        _log.entering(CLASS, methodName, new Object[] { clientId });

        boolean result = false;
        Connection conn = null;
        boolean error = true;
        OidcBaseClient client = null;

        // OidcBaseClient client = _cache.get(clientId);
        if (client == null) {
            try {
                conn = getInitializedConnection();
                client = getClientFromDB(conn, clientId);
                error = false;
            } catch (SQLException e) {
                // Try to add missing tables
                attemptDbMigration(conn);

                try {
                    // Retry obtaining client
                    client = getClientFromDB(conn, clientId);
                    error = false;
                } catch (SQLException e1) {
                    String operation = "SELECT";
                    logMessageAndThrowOidcServerException(methodName, e1, "ERROR_PERFORMING_DB_OPERATION", operation, clientId);
                }
            } finally {
                closeConnection(conn, error);
            }
        }

        result = (client != null);

        _log.exiting(CLASS, methodName, "" + result);

        return result;
    }

    // TODO: Need to validate this works
    @Override
    @FFDCIgnore(java.sql.SQLException.class)
    public boolean validateClient(String clientId, String clientSecret) throws OidcServerException {
        String methodName = "validateClient";
        _log.entering(CLASS, methodName, new Object[] { clientId, "secret_removed" });

        boolean result = false;
        Connection conn = null;
        boolean error = true;

        OidcBaseClient client = null;

        try {
            if (clientSecret != null && !clientSecret.isEmpty()) {
                conn = getInitializedConnection();
                client = getClientFromDB(conn, clientId, false);
                error = false;
            }
        } catch (SQLException e) {
            // Try to add missing tables
            attemptDbMigration(conn);

            try {
                // Retry obtaining client
                client = getClientFromDB(conn, clientId, false);
                error = false;
            } catch (SQLException e1) {
                String operation = "SELECT";
                logMessageAndThrowOidcServerException(methodName, e1, "ERROR_PERFORMING_DB_OPERATION", operation, clientId);
            }
        } finally {
            closeConnection(conn, error);
        }

        if (client != null && client.isEnabled() && client.isConfidential()) {
            if (hasRewrites) {
                client = ClientUtils.uriRewrite(client);
            }

            String storedSecret = client.getClientSecret();

            boolean update = updateXORtoHash;
            String secretType = PasswordUtil.getCryptoAlgorithm(storedSecret);
            if (secretType != null && secretType.equals(OAuth20Constants.HASH)) {
                update = false; // already hashed, don't need to update
                clientSecret = HashSecretUtils.hashSecret(clientSecret, clientId, false, client.getSalt(), client.getAlgorithm(), client.getIterations(), client.getLength());
            }

            if (storedSecret != null && storedSecret.equals(clientSecret)) {
                result = true;

                /*
                 * Optionally update the client secret from XOR to Hash
                 */
                if (update && !isXORConfigured()) { // convert XOR to Hash
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "Converting client secret for " + clientId + " to hash during a validateClient request");
                    }
                    update(client);
                }
            }

        }

        _log.exiting(CLASS, methodName, "" + result);

        return result;
    }

    @FFDCIgnore(com.ibm.ws.security.oauth20.exception.OAuthDataException.class)
    Connection getInitializedConnection() throws OidcServerException {
        Connection conn;
        try {
            conn = getDBConnection();
            conn.setAutoCommit(false);
        } catch (OAuthDataException e) {
            throw new OidcServerException(e.getLocalizedMessage(), OIDCConstants.ERROR_SERVER_ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        } catch (SQLException e) {
            throw new OidcServerException(e.getLocalizedMessage(), OIDCConstants.ERROR_SERVER_ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
        }

        return conn;
    }

    private static OidcBaseClientDBModel getOidcBaseClientDBModel(String componentId, OidcBaseClient client, int enabled) {
        return new OidcBaseClientDBModel(componentId, client.getClientId(), null, "", null, enabled, getClientMetadata(client));
    }

    private static OidcBaseClient getOidcBaseClient(OidcBaseClientDBModel clientDbModel) {

        OidcBaseClient client = OidcOAuth20Util.GSON_RAW.fromJson(clientDbModel.getClientMetadata(), OidcBaseClient.class);
        client.setComponentId(clientDbModel.getComponentId());
        client.setClientId(clientDbModel.getClientId());
        client.setEnabled((clientDbModel.getEnabled() != 0));

        return client;
    }

    @Sensitive
    private static JsonObject getClientMetadata(OidcBaseClient client) {
        JsonObject clientMetadataJsonObj = OidcOAuth20Util.getJsonObj(client);

        clientMetadataJsonObj.remove("client_id");

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            Set<Map.Entry<String, JsonElement>> entries = clientMetadataJsonObj.entrySet();
            for (Map.Entry<String, JsonElement> entry : entries) {
                String key = entry.getKey();

                if (key.equals(OAuth20Constants.CLIENT_SECRET)) {
                    sb.append(" " + key + "=secret_removed");
                } else if (key.equals(OAuth20Constants.SALT)) {
                    sb.append(" " + key + "=salt_removed");
                } else {
                    sb.append(" " + key + "=" + entry.getValue());
                }

            }
            sb.append("]");
            Tr.debug(tc, CLASS, "getClientMetadata: " + sb.toString());
        }

        return clientMetadataJsonObj;
    }

    void encodeClientSecretInClientMetadata(OidcBaseClientDBModel clientDbModel) {
        JsonObject clientMetadataJson = clientDbModel.getClientMetadata();
        if (clientMetadataJson != null && clientMetadataJson.has(OAuth20Constants.CLIENT_SECRET)) {
            String clientSecret = clientMetadataJson.get(OAuth20Constants.CLIENT_SECRET).getAsString();
            if (clientSecret != null && !clientSecret.isEmpty()) {
                clientMetadataJson.addProperty(OAuth20Constants.CLIENT_SECRET, PasswordUtil.passwordEncode(clientSecret));
            }
        }
    }

    /**
     * Logs the exception message, logs an error message in the server log, and throws an OidcServerException using the message
     * key and message arguments provided.
     */
    void logMessageAndThrowOidcServerException(String methodName, SQLException e, String msgKey, Object... msgArgs) throws OidcServerException {
        _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);

        // The caught SQLException message might contain sensitive information about database operations.
        // Log the message with the caught SQLException message, but don't include the SQLException message in the new exception that gets thrown.
        Object[] updatedMsgArgs = appendStringMessageToArgs(e.getLocalizedMessage(), msgArgs);
        Tr.error(tc, msgKey, updatedMsgArgs);

        // Use an empty string as the last insert to avoid an unused insert (e.g. "{2}") showing up in the exception message
        updatedMsgArgs = appendStringMessageToArgs("", msgArgs);
        throw new OidcServerException(new BrowserAndServerLogMessage(tc, msgKey, updatedMsgArgs), OIDCConstants.ERROR_SERVER_ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
    }

    /**
     * Logs the exception message, logs an error message in the server log, and throws an OidcServerException using the message
     * key and message arguments provided.
     */
    void logMessageAndThrowOidcServerException(String methodName, IllegalArgumentException e, String msgKey, Object... msgArgs) throws OidcServerException {
        _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);

        // The caught SQLException message might contain sensitive information about database operations.
        // Log the message with the caught SQLException message, but don't include the SQLException message in the new exception that gets thrown.
        Object[] updatedMsgArgs = appendStringMessageToArgs(e.getLocalizedMessage(), msgArgs);
        Tr.error(tc, msgKey, updatedMsgArgs);

        // Use an empty string as the last insert to avoid an unused insert (e.g. "{2}") showing up in the exception message
        updatedMsgArgs = appendStringMessageToArgs("", msgArgs);
        throw new OidcServerException(new BrowserAndServerLogMessage(tc, msgKey, updatedMsgArgs), OIDCConstants.ERROR_SERVER_ERROR, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e);
    }

    Object[] appendStringMessageToArgs(String additionalInsert, Object... msgArgs) {
        Object[] updatedMsgArgs = new Object[1];
        if (msgArgs != null) {
            updatedMsgArgs = new Object[msgArgs.length + 1];
            for (int i = 0; i < msgArgs.length; i++) {
                updatedMsgArgs[i] = msgArgs[i];
            }
        }
        updatedMsgArgs[updatedMsgArgs.length - 1] = additionalInsert;
        return updatedMsgArgs;
    }

    private boolean isPBKDF2WithHmacSHA512Configured() {
        return hashType.equals(HashSecretUtils.PBKDF2WithHmacSHA512);
    }

    private boolean isXORConfigured() {
        return hashType.equals(OAuth20Constants.XOR);
    }

}
