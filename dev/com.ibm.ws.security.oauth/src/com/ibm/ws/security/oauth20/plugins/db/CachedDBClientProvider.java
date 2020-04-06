/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.oauth20.plugins.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.oauth20.client.OAuth20Client;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedClientProvider;
import com.ibm.ws.security.oauth20.exception.OAuthDataException;
import com.ibm.ws.security.oauth20.plugins.BaseClient;
import com.ibm.ws.security.oauth20.util.ClientUtils;
import com.ibm.ws.security.oauth20.util.DynaCacheUtils;
import com.ibm.ws.security.oauth20.util.OidcOAuth20Util;

/**
 * JDBC-based client provider with a distributed cachmap used for performance
 */
public class CachedDBClientProvider extends OAuthJDBCImpl implements OAuth20EnhancedClientProvider {

    final static String CLASS = CachedDBClientProvider.class.getName();
    Logger _log = Logger.getLogger(CLASS);

    private String componentId;
    private String tableName;
    private String clientCacheJndi;
    private Map<String, BaseClient> cache;
    private boolean hasRewrites; // URI redirect token substitution
    private String[] providerRewrites;

    public CachedDBClientProvider() {
    }

    public CachedDBClientProvider(String componentId, DataSource ds, String tableName, @Sensitive Object[] credentials, String clientCacheJndi, String[] providerRewrites) {
        super(ds, credentials);
        this.componentId = componentId;
        this.tableName = tableName;
        if (clientCacheJndi == null || "".equals(clientCacheJndi)) {
            this.clientCacheJndi = Constants.DEFAULT_DYNACACHE_JNDI_DB_CLIENTS;
        } else {
            this.clientCacheJndi = clientCacheJndi;
        }

        this.providerRewrites = providerRewrites != null ? providerRewrites.clone() : null;
    }

    public void initialize() {
        String methodName = "initialize";
        _log.entering(CLASS, methodName);
        boolean finestLoggable = _log.isLoggable(Level.FINEST);
        try {
            if (finestLoggable) {
                _log.logp(Level.FINEST, CLASS, methodName, "Using ComponentId: " + componentId);
            }

            cache = DynaCacheUtils.getDynamicCache(clientCacheJndi, new String[0], new BaseClient[0]);
            hasRewrites = ClientUtils.initRewrites(componentId, providerRewrites);
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }

    public void init(OAuthComponentConfiguration config) {
        String methodName = "init";
        _log.entering(CLASS, methodName);
        boolean finestLoggable = _log.isLoggable(Level.FINEST);
        try {
            super.init(config);
            tableName = config.getConfigPropertyValue(CONFIG_CLIENT_TABLE);
            componentId = config.getUniqueId();

            if (finestLoggable) {
                _log.logp(Level.FINEST, CLASS, methodName,
                        "Using ComponentId: " + componentId);
            }

            String clientCacheJndi = config.getConfigPropertyValue(Constants.DYNACACHE_CONFIG_DB_CLIENTS);
            if (clientCacheJndi == null || "".equals(clientCacheJndi)) {
                clientCacheJndi = Constants.DEFAULT_DYNACACHE_JNDI_DB_CLIENTS;
            }
            cache = DynaCacheUtils.getDynamicCache(clientCacheJndi, new String[0], new BaseClient[0]);
            hasRewrites = ClientUtils.initRewrites(config);
        } finally {
            _log.exiting(CLASS, methodName);
        }
    }

    public boolean exists(String clientId) {
        String methodName = "exists";
        boolean result = false;
        Connection conn = null;
        boolean error = false;

        OAuth20Client client = null;
        client = cache.get(clientId);

        if (client == null) {
            try {
                conn = getDBConnection();
                conn.setAutoCommit(false);
                client = loadClient(conn, clientId);
            } catch (Exception e) {
                // log but don't fail
                _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
                error = true;
            } finally {
                closeConnection(conn, error);
            }
        }

        result = (client != null);
        _log.exiting(CLASS, methodName, "" + result);
        return result;
    }

    public OAuth20Client get(String clientId) {
        String methodName = "get";
        _log.entering(CLASS, methodName, new Object[] { clientId });

        BaseClient result = null;
        Connection conn = null;
        boolean error = false;

        result = cache.get(clientId);

        if (result == null) {
            try {
                conn = getDBConnection();
                conn.setAutoCommit(false);
                result = loadClient(conn, clientId);
            } catch (Exception e) {
                // log but don't fail
                _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
                error = true;
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

    public boolean validateClient(String clientId, String clientSecret) {
        String methodName = "validateClient";
        _log.entering(CLASS, methodName, new Object[] { clientId, "secret_removed" });

        BaseClient client = null;
        Connection conn = null;
        boolean error = false;
        boolean result = false;

        client = cache.get(clientId);

        if (client == null) {
            try {
                if (clientSecret != null && clientSecret.length() > 0) {
                    conn = getDBConnection();
                    conn.setAutoCommit(false);
                    client = loadClient(conn, clientId);
                }
            } catch (Exception e) {
                // log but don't fail
                _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
                error = true;
            } finally {
                closeConnection(conn, error);
            }
        }

        if (client != null && client.isEnabled() && client.isConfidential()) {
            if (hasRewrites) {
                client = ClientUtils.uriRewrite(client);
            }

            String storedSecret = client.getClientSecret();
            if (storedSecret != null && storedSecret.equals(clientSecret)) {
                result = true;
            }
        }

        _log.exiting(CLASS, methodName, "" + result);
        return result;
    }

    public BaseClient put(BaseClient newClient) throws OAuthDataException {
        String methodName = "put";
        _log.entering(CLASS, methodName, new Object[] { newClient });
        BaseClient retVal = newClient;
        Connection conn = null;
        boolean error = true;

        String clientId = newClient.getClientId();
        if (exists(clientId)) {
            delete(clientId);
        }

        try {
            conn = getDBConnection();
            conn.setAutoCommit(false);
            addClient(conn, newClient);
            error = false;
            cache.put(clientId, newClient);
        } catch (SQLException e) {
            throw new OAuthDataException(e);
        } finally {
            closeConnection(conn, error);
        }
        if (error) {
            retVal = null;
        }
        _log.exiting(CLASS, methodName, new Object[] {});
        return retVal;
    }

    public boolean delete(String clientIdentifier) {
        String methodName = "delete";
        _log.entering(CLASS, methodName, new Object[] { clientIdentifier });

        Connection conn = null;
        boolean success = true;

        // weak hashmap, may or may not be in there
        BaseClient cachedClient = cache.get(clientIdentifier);
        if (cachedClient != null) {
            Object removed = cache.remove(clientIdentifier);
            _log.fine("removed object from cache: " + removed);
            if (removed == null) {
                _log.severe("Cannot remove cache object with ID: " + clientIdentifier);
            }
        }

        try {
            conn = getDBConnection();
            conn.setAutoCommit(false);
            success = deleteClient(conn, clientIdentifier);
        } catch (Exception e) {
            // log but don't fail
            _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
            success = false;
        } finally {
            closeConnection(conn, !success);
        }

        _log.exiting(CLASS, methodName, success);
        return success;
    }

    public Collection<BaseClient> getAll() {
        String methodName = "getAll";
        _log.entering(CLASS, methodName, new Object[] {});

        // Admin or test operation, skip cache

        Collection<BaseClient> results = null;
        Connection conn = null;
        boolean error = false;

        try {
            conn = getDBConnection();
            conn.setAutoCommit(false);
            results = findAllClients(conn);
            if (hasRewrites && results != null) {
                Collection<BaseClient> updatedResults = new ArrayList<BaseClient>();
                for (BaseClient result : results) {
                    updatedResults.add(ClientUtils.uriRewrite(result));
                }
                results = updatedResults;
            }
        } catch (Exception e) {
            // log but don't fail
            _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
            error = true;
        } finally {
            closeConnection(conn, error);
            _log.exiting(CLASS, methodName, results);
        }

        return results;
    }

    protected BaseClient loadClient(Connection conn, String clientId) {
        String methodName = "findClient";
        _log.entering(CLASS, methodName, new Object[] { clientId });
        BaseClient result = null;
        ResultSet queryResults = null;
        PreparedStatement st = null;
        try {
            String query = "SELECT * FROM " + tableName + " WHERE "
                    + TableConstants.COL_CC2_COMPONENTID + " = ? AND "
                    + TableConstants.COL_CC2_CLIENTID + " = ?";

            st = conn.prepareStatement(query);
            st.setString(1, componentId);
            st.setString(2, clientId);
            queryResults = st.executeQuery();

            while (queryResults != null && result == null && queryResults.next()) {
                result = createClient(queryResults, clientId);
                cache.put(result.getClientId(), result);
            }
        } catch (SQLException e) {
            // log but don't fail
            _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
        } finally {
            closeResultSet(queryResults);
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                    _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
                }
            }
            _log.exiting(CLASS, methodName, result);
        }
        return result;
    }

    protected void addClient(Connection conn, BaseClient newClient) throws OAuthDataException {
        String methodName = "addClient";
        _log.entering(CLASS, methodName, new Object[] { newClient });
        ResultSet queryResults = null;
        PreparedStatement st = null;
        try {
            String insert = "INSERT INTO " + tableName
                    + " VALUES ( ?, ?, ?, ?, ?, ? )";

            st = conn.prepareStatement(insert);
            st.setString(1, componentId);
            st.setString(2, newClient.getClientId());
            // encode client secret
            st.setString(3, PasswordUtil.passwordEncode(newClient.getClientSecret()));
            st.setString(4, newClient.getClientName());
            // st.setString(5, newClient.getRedirectUris());
            st.setInt(6, 1); // enabled

            st.executeUpdate();
        } catch (SQLException e) {
            throw new OAuthDataException(e);
        } finally {
            closeResultSet(queryResults);
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                    _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
                }
            }
            _log.exiting(CLASS, methodName, null);
        }
    }

    protected boolean deleteClient(Connection conn, String clientId) {
        String methodName = "deleteClient";
        _log.entering(CLASS, methodName,
                new Object[] { tableName, componentId, clientId });
        boolean success = true;
        ResultSet queryResults = null;
        PreparedStatement st = null;
        try {
            String del = "DELETE FROM " + tableName + " WHERE "
                    + TableConstants.COL_CC2_COMPONENTID + " = ? AND "
                    + TableConstants.COL_CC2_CLIENTID + " = ?";

            st = conn.prepareStatement(del);
            st.setString(1, componentId);
            st.setString(2, clientId);

            int numDeleted = st.executeUpdate();
            _log.logp(Level.FINE, CLASS, methodName, "Num entries deleted: " + numDeleted);
            if (numDeleted < 1) {
                success = false;
            }
        } catch (SQLException e) {
            _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
            success = false;
        } finally {
            closeResultSet(queryResults);
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                    _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
                }
            }
            _log.exiting(CLASS, methodName, null);
        }
        return success;
    }

    protected Collection<BaseClient> findAllClients(Connection conn) {
        String methodName = "findAllClients";
        _log.entering(CLASS, methodName, new Object[] {});
        Collection<BaseClient> results = new ArrayList<BaseClient>();
        ResultSet queryResults = null;
        PreparedStatement st = null;
        try {
            String query = "SELECT * FROM " + tableName + " WHERE "
                    + TableConstants.COL_CC2_COMPONENTID + " = ?";

            st = conn.prepareStatement(query);
            st.setString(1, componentId);
            queryResults = st.executeQuery();

            while (queryResults != null && queryResults.next()) {
                BaseClient result = createClient(queryResults);
                results.add(result);
            }
        } catch (SQLException e) {
            // log but don't fail
            _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
        } finally {
            closeResultSet(queryResults);
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                    _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
                }
            }
            _log.exiting(CLASS, methodName, results);
        }
        return results;
    }

    protected BaseClient createClient(ResultSet queryResults) throws SQLException {
        String clientId = queryResults.getString(TableConstants.COL_CC2_CLIENTID);
        return createClient(queryResults, clientId);
    }

    protected BaseClient createClient(ResultSet queryResults, String clientId) throws SQLException {
        String clientSecret = queryResults.getString(TableConstants.COL_CC2_CLIENTSECRET);
        // decode client secret
        clientSecret = PasswordUtil.passwordDecode(clientSecret);
        String displayName = queryResults.getString(TableConstants.COL_CC2_DISPLAYNAME);
        String redirectURI = queryResults.getString(TableConstants.COL_CC2_REDIRECTURI);
        int enabled = queryResults.getInt(TableConstants.COL_CC2_ENABLED);
        BaseClient result = new BaseClient(componentId,
                clientId,
                clientSecret,
                displayName,
                OidcOAuth20Util.initJsonArray(redirectURI),
                (enabled != 0));
        return result;
    }

    protected int update(Connection conn, BaseClient newClient) {
        String methodName = "update";
        _log.entering(CLASS, methodName, new Object[] { conn, newClient });
        PreparedStatement st = null;
        int retVal = 0;
        try {
            String insert = "UPDATE " + tableName + " SET "
                    + TableConstants.COL_CC2_COMPONENTID + "=? ,"
                    + TableConstants.COL_CC2_CLIENTSECRET + "=? ,"
                    + TableConstants.COL_CC2_DISPLAYNAME + "=? ,"
                    + TableConstants.COL_CC2_REDIRECTURI + "=? ,"
                    + TableConstants.COL_CC2_ENABLED + "=? " + "WHERE "
                    + TableConstants.COL_CC2_CLIENTID + " = ?  AND "
                    + TableConstants.COL_CC2_COMPONENTID + " = ?";

            st = conn.prepareStatement(insert);
            st.setString(1, componentId);
            st.setString(2, newClient.getClientSecret());
            st.setString(3, newClient.getClientName());
            // This class isn't used anymore.. it has been superceded by CachedDBOidcClientProvider
            // st.setString(4, newClient.getRedirectUri());
            st.setInt(5, newClient.isEnabled() ? 1 : 0); // enabled
            st.setString(6, newClient.getClientId());
            st.setString(7, componentId);

            retVal = st.executeUpdate();
        } catch (SQLException e) {
            // log but don't fail
            _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
        } finally {
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                    _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
                }
            }
            _log.exiting(CLASS, methodName, retVal);
        }
        return retVal;
    }

    public BaseClient update(BaseClient newClient) {
        String methodName = "update";
        _log.entering(CLASS, methodName, new Object[] { newClient });

        cache.put(newClient.getClientId(), newClient);

        Connection conn = null;
        boolean error = false;
        BaseClient retVal = null;
        try {
            conn = getDBConnection();
            conn.setAutoCommit(false);
            if (update(conn, newClient) == 1) {
                retVal = newClient;
            }
        } catch (Exception e) {
            // log but don't fail
            _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
            error = true;
        } finally {
            closeConnection(conn, error);
            _log.exiting(CLASS, methodName, retVal);
        }

        return retVal;
    }

    public int getCount() {
        String methodName = "getCount";
        _log.entering(CLASS, methodName, new Object[] {});

        int result = -1;

        ResultSet queryResults = null;
        PreparedStatement st = null;

        try {

            Connection conn = getDBConnection();
            conn.setAutoCommit(false);

            String query = "SELECT COUNT(*) AS \"TOTAL\" FROM " + tableName + " WHERE "
                    + TableConstants.COL_CC2_COMPONENTID + " = ?";

            st = conn.prepareStatement(query);
            st.setString(1, componentId);
            queryResults = st.executeQuery();

            while (queryResults != null && queryResults.next()) {
                result = queryResults.getInt("TOTAL");
            }
        } catch (SQLException e) {
            // log but don't fail
            _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
        } catch (OAuthDataException e) {
            // log but don't fail
            _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
        } finally {
            closeResultSet(queryResults);
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                    _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
                }
            }
            _log.exiting(CLASS, methodName, result);
        }
        return result;
    }

}
