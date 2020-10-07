/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation and others.
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
import java.sql.SQLSyntaxErrorException;
import java.sql.Statement;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.api.OauthConsentStore;
import com.ibm.ws.security.oauth20.exception.OAuthDataException;
import com.ibm.ws.security.oauth20.plugins.db.DetectDatabaseType.DBType;

/**
 *
 */
public class DBConsentCache extends OAuthJDBCImpl implements OauthConsentStore {

    private static final TraceComponent tc = Tr.register(DBConsentCache.class, "OAuth20Provider", null);
    private String TABLE_NAME = TableConstants.TABLE_CONSENT;

    private final static String INSERT_STMT = "INSERT INTO %s ("
            + TableConstants.COL_CONSENT_CLIENT_ID + ","
            + TableConstants.COL_CONSENT_USER + ","
            + TableConstants.COL_CONSENT_PROVIDER_ID + ","
            // + TableConstants.COL_CONSENT_RESOURCE_ID + ","
            + TableConstants.COL_CONSENT_SCOPE + ","
            + TableConstants.COL_CONSENT_EXPIRES + ","
            + TableConstants.COL_CONSENT_EXTENDEDFIELDS
            + ") VALUES (?,?,?,?,?,?)";

    private final static String DELETE_STMT = "DELETE FROM %s WHERE "
            + TableConstants.COL_CONSENT_CLIENT_ID + " = ? AND "
            + TableConstants.COL_CONSENT_USER + " = ? AND "
            + TableConstants.COL_CONSENT_PROVIDER_ID + " = ? ";

    final static String CONFIG_CLEANUP_INTERVAL = "oauthjdbc.CleanupInterval";
    final static String CONFIG_CLEANUP_BATCH_SIZE = "oauthjdbc.CleanupBatchSize";

    int cleanupInterval = 0;
    int cleanupBatchSize = 250;

    protected String componentId;
    // protected String tableName;
    private String tokenCacheJndi;

    public static enum DBVENDOR {
        UNSPECIFIED, DB2, MICROSOFT, ORACLE, INFORMIX, SYBASE
    };

    private DBVENDOR dbVendor = DBVENDOR.UNSPECIFIED;
    private final String MICROSOFT = "MICROSOFT";
    private final String DB2 = "DB2";
    private final String ORACLE = "ORACLE";
    private final String INFORMIX = "INFORMIX";
    private final String SYBASE = "ADAPTIVE SERVER";
    private ExecutorService executorService;

    @SuppressWarnings("rawtypes")
    Future cleanupThreadFuture;

    boolean cleanupThreadExitSemaphore = false;

    // private static int cacheCapacity = 500;

    public DBConsentCache() {

    }

    public DBConsentCache(String componentId,
            ExecutorService executorsvc,
            DataSource dataSource,
            String tableName,
            @Sensitive Object[] credentials,
            String tokenCacheJndi,
            int cleanupInterval,
            int cleanupBatchSize) {
        super(dataSource, credentials);
        this.componentId = componentId;
        this.TABLE_NAME = tableName;
        if (tokenCacheJndi == null || "".equals(tokenCacheJndi)) {
            this.tokenCacheJndi = Constants.DEFAULT_DYNACACHE_JNDI_DB_TOKENS;
        } else {
            this.tokenCacheJndi = tokenCacheJndi;
        }
        this.executorService = executorsvc;
        this.cleanupInterval = cleanupInterval;
        this.cleanupBatchSize = cleanupBatchSize;

    }

    @Override
    public void initialize() {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Using cleanup interval: " + cleanupInterval
                    + " cleanupBatchSize: " + cleanupBatchSize);
        }
        createCacheTable();
        startCleanupThread();
        // getCache(tokenCacheJndi);
    }

    @FFDCIgnore(java.sql.SQLException.class)
    private void createCacheTable() {

        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        boolean error = false;
        try {
            connection = getInitializedConnection();
            connection.setAutoCommit(false);
            statement = connection.createStatement();

            String dbp = connection.getMetaData().getDatabaseProductName();
            if (dbp != null) {
                dbp = dbp.toUpperCase();
                if (dbp.contains(MICROSOFT)) {
                    dbVendor = DBVENDOR.MICROSOFT;
                } else if (dbp.contains(DB2)) {
                    dbVendor = DBVENDOR.DB2;
                } else if (dbp.contains(ORACLE)) {
                    dbVendor = DBVENDOR.ORACLE;
                } else if (dbp.contains(INFORMIX)) {
                    dbVendor = DBVENDOR.INFORMIX;
                } else if (dbp.contains(SYBASE)) {
                    dbVendor = DBVENDOR.SYBASE;
                }
            }

            // first check if the table exists
            boolean tableExists = false;

            // ResultSet resultSet = connection.getMetaData().getTables(null, null, TABLE_NAME, new String[] {"TABLE"});
            resultSet = connection.getMetaData().getTables(null, null, "%", null);

            while (resultSet.next()) {
                String table_name = resultSet.getString(3);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Found table =" + table_name);
                }

                if ("OAUTH20CONSENTCACHE".equalsIgnoreCase(table_name)) {
                    tableExists = true;
                    break;
                }
            }

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "tableExists: " + tableExists);
            }

            if (!tableExists) {
                // else create the new table
                // @av2
                /*
                 * String createTable = "CREATE TABLE " + TABLE_NAME + " (" + UUID + " VARCHAR(1024), " + TOKEN
                 * + " LONG VARCHAR FOR BIT DATA, " + CACHE_TIMEOUT + " BIGINT, " + TIMESTAMP + " TIMESTAMP)";
                 */
                String createTable = null;
                if (dbVendor.equals(DBVENDOR.ORACLE)) {
                    createTable = "CREATE TABLE " + TABLE_NAME + " (" + TableConstants.COL_CONSENT_CLIENT_ID + " VARCHAR(256), " + TableConstants.COL_CONSENT_USER
                            + " VARCHAR(256), " + TableConstants.COL_CONSENT_PROVIDER_ID + " VARCHAR(256), "
                            + TableConstants.COL_CONSENT_SCOPE + " VARCHAR(1024), " +
                            TableConstants.COL_CONSENT_EXPIRES + " NUMBER(19), " +
                            TableConstants.COL_CONSENT_EXTENDEDFIELDS + " CLOB )";

                } /*
                   * else if (dbVendor.equals(DBVENDOR.MICROSOFT) || dbVendor.equals(DBVENDOR.SYBASE)){
                   * createTable = "CREATE TABLE " + TABLE_NAME + " (" + TableConstants.COL_CONSENT_CLIENT_ID + " VARCHAR(256), " + TableConstants.COL_CONSENT_REDIRECT_URI
                   * + " VARCHAR(256), " + TableConstants.COL_CONSENT_PROVIDER_ID + " VARCHAR(256), " + TableConstants.COL_CONSENT_RESOURCE_ID + " VARCHAR(256), " +
                   * TableConstants.COL_CONSENT_SCOPE + " VARCHAR(1024), " +
                   * TableConstants.COL_CONSENT_EXPIRES + " BIGINT)";
                   * }
                   */else {
                    createTable = "CREATE TABLE " + TABLE_NAME + " (" + TableConstants.COL_CONSENT_CLIENT_ID + " VARCHAR(256), "
                            + TableConstants.COL_CONSENT_USER + " VARCHAR(256), "
                            + TableConstants.COL_CONSENT_PROVIDER_ID + " VARCHAR(256), "
                            + TableConstants.COL_CONSENT_SCOPE + " VARCHAR(1024), " +
                            TableConstants.COL_CONSENT_EXPIRES + " BIGINT, " +
                            TableConstants.COL_CONSENT_EXTENDEDFIELDS + " CLOB )";
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "createTable: " + createTable);
                    Tr.debug(tc, "database provider = " + dbp);
                }
                if (!dbp.equals("DB1234")) { // skip following for junit testing.
                    statement.executeUpdate(createTable);
                }
            }
        } catch (SQLException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Failed to create JDBC Cache table", e);
            error = true;
        } catch (OAuthDataException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Failed to create JDBC Cache table", e);
            error = true;

        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Failed to close JDBC Statement", e);
                }
            }
            closeResultSet(resultSet);
            closeConnection(connection, error);
        }
    }

    private Connection getInitializedConnection() throws OAuthDataException, SQLException {
        Connection conn = getDBConnection();
        conn.setAutoCommit(false);
        return conn;
    }

    private void startCleanupThread() {
        synchronized (DBConsentCache.class) {
            if (cleanupInterval > 0 && cleanupThreadFuture == null) {
                cleanupThreadFuture = executorService.submit(new CleanupThread(this));
            }
        }
    }

    @Override
    public void stopCleanupThread() {
        if (cleanupThreadFuture != null) {
            // tell the thread to die when it's in a safe spot.
            cleanupThreadExitSemaphore = true;
            cleanupThreadFuture.cancel(true);
        }
    }

    @Trivial
    class CleanupThread extends Thread {
        final String MYCLASS = CleanupThread.class.getName();
        Logger _log = Logger.getLogger(MYCLASS);
        protected boolean fineLoggable;
        protected boolean finestLoggable;
        protected DBConsentCache _me;
        protected DBType databaseType = null;

        public CleanupThread(DBConsentCache me) {
            _me = me;
        }

        @Override
        @FFDCIgnore({ com.ibm.ws.security.oauth20.exception.OAuthDataException.class, InterruptedException.class })
        public void run() {
            String methodName = "run";
            _log.entering(MYCLASS, methodName);
            // Nothing to do for cache, expiry is checked on cache load
            // Determine the database type, to see if it supports LIMIT
            Connection connCheck = null;
            try {
                connCheck = _me.getDBConnection();
                databaseType = DetectDatabaseType.DetectionUtils.detectDbType(connCheck);
            } catch (OAuthDataException e) {
                _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
                return;
            } finally {
                try {
                    if (connCheck != null && !connCheck.isClosed())
                        connCheck.close();
                } catch (SQLException e) {
                    if (finestLoggable) {
                        _log.logp(Level.FINEST, MYCLASS, methodName, "Unable to close connection.");
                    }
                }
            }
            while (true) {
                try {
                    sleep(_me.cleanupInterval * 1000);
                } catch (InterruptedException e) {
                    if (finestLoggable) {
                        _log.logp(Level.FINEST, MYCLASS, methodName,
                                "Cleanup thread was interrupted");
                    }
                }
                if (_me.cleanupThreadExitSemaphore == true) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "cleanup thread exiting because exit semaphore is true");
                    }
                    break;
                }
                runCleanup();

            }
        }

        protected void runCleanup() {
            String methodName = "runCleanup";
            _log.entering(MYCLASS, methodName);

            Date now = new Date();
            long nowTime = now.getTime();
            // Get the number of expired
            int expiredCount = getExpiredCount(nowTime);
            if (fineLoggable) {
                _log.logp(Level.FINEST, MYCLASS, methodName,
                        "About to delete all tokens with expiry <= " + nowTime);
                _log.logp(Level.FINEST, MYCLASS, methodName,
                        "Number of expired tokens in the DB: " + expiredCount);
            }

            if (expiredCount > 10000) {
                String msg = expiredCount
                        + " expired tokens to delete. Consider increasing OAuth provider cleanup interval";
                _log.logp(Level.WARNING, MYCLASS, methodName, msg);
            }
            boolean error = false;
            Connection conn = null;
            int numDeleted = 0;
            while (expiredCount > 0 && numDeleted < expiredCount) {
                PreparedStatement st = null;
                try {
                    conn = _me.getDBConnection();
                    conn.setAutoCommit(false);
                    /*
                     * Note the COL_CONSENT_EXPIRES > 0. A value <= 0 implies
                     * "forever" so we don't delete those.
                     */
                    String delete;
                    if (DBType.DB2 == databaseType) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Running cleanup with LIMIT in DB2");
                        }
                        delete = "DELETE FROM (SELECT " + TableConstants.COL_CONSENT_EXPIRES + " FROM " + TABLE_NAME + " WHERE "
                                + TableConstants.COL_CONSENT_EXPIRES + " > 0 AND "
                                + TableConstants.COL_CONSENT_EXPIRES + " <= ?  ORDER BY " + TableConstants.COL_CONSENT_EXPIRES + " FETCH FIRST " + cleanupBatchSize
                                + " ROWS ONLY)";
                        numDeleted += cleanupBatchSize;
                    } else if (databaseType.isSqlLimitSupported()) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Running cleanup with LIMIT");
                        }
                        delete = "DELETE FROM " + TABLE_NAME + " WHERE "
                                + TableConstants.COL_CONSENT_EXPIRES + " > 0 AND "
                                + TableConstants.COL_CONSENT_EXPIRES + " <= ? "
                                + "LIMIT " + cleanupBatchSize;
                        numDeleted += cleanupBatchSize;
                    } else {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Running cleanup without LIMIT");
                        }
                        delete = "DELETE FROM " + TABLE_NAME + " WHERE "
                                + TableConstants.COL_CONSENT_EXPIRES + " > 0 AND "
                                + TableConstants.COL_CONSENT_EXPIRES + " <= ?";
                        numDeleted += expiredCount;
                    }
                    st = conn.prepareStatement(delete);
                    st.setLong(1, nowTime);
                    st.execute();
                } catch (SQLException syne) {
                    // DB LIMIT support may have been misdetected, try disabling
                    databaseType = DBType.UNKNOWN;
                    _log.logp(Level.WARNING, CLASS, methodName, syne
                            .getMessage(), syne);
                    _log.logp(Level.FINE, MYCLASS, methodName,
                            "SQL error, switching off LIMIT");
                } catch (Exception e) {
                    // log but don't fail
                    _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(),
                            e);
                    error = true;
                } finally {
                    closeStatement(st);
                    closeConnection(conn, error);
                }
            }
        }
    } // end CleanupThread class

    protected void closeStatement(Statement statement) {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Internal error closing SQL statement", e);
            }
        }
    }

    protected int getExpiredCount(long nowTime) {
        int result = -1;
        ResultSet queryResults = null;
        PreparedStatement st = null;
        Connection conn = null;
        try {
            conn = getDBConnection();
            conn.setAutoCommit(false);
            if (tc.isEventEnabled()) {
                Tr.event(tc, "Checking for expired with time: " + nowTime);
            }
            String query = "SELECT COUNT(*) AS \"TOTAL\" FROM " + TABLE_NAME
                    + " WHERE " + TableConstants.COL_CONSENT_EXPIRES + " > 0 AND "
                    + TableConstants.COL_CONSENT_EXPIRES + " <= ? ";
            st = conn.prepareStatement(query);
            st.setLong(1, nowTime);
            queryResults = st.executeQuery();

            while (queryResults != null && queryResults.next()) {
                result = queryResults.getInt("TOTAL");
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "Updated result to: " + result);
                }
            }
        } catch (SQLException e) {
            // log but don't fail
            Tr.error(tc, "Internal SQL error wile getting expired count: " + e.getMessage(), e);
        } catch (OAuthDataException e) {
            // log but don't fail
            Tr.error(tc, "Internal OAuth error wile getting expired count: " + e.getMessage(), e);
        } finally {
            closeResultSet(queryResults);
            if (st != null) {
                try {
                    st.close();
                } catch (SQLException e) {
                    Tr.error(tc, "Internal error wile closing SQL statement: " + e.getMessage(), e);
                }
            }
            closeConnection(conn, false);
        }
        return result;
    }

    @Override
    @FFDCIgnore(java.lang.NumberFormatException.class)
    public void init(OAuthComponentConfiguration config) {
        super.init(config);
        componentId = config.getUniqueId();
        TABLE_NAME = config.getConfigPropertyValue(CONFIG_TOKEN_TABLE);
        cleanupInterval = config.getConfigPropertyIntValue(CONFIG_CLEANUP_INTERVAL);
        try {
            cleanupBatchSize = config.getConfigPropertyIntValue(CONFIG_CLEANUP_BATCH_SIZE);
        } catch (NumberFormatException e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "No setting for "
                        + CONFIG_CLEANUP_BATCH_SIZE
                        + ", using default cleanup batch size of: "
                        + cleanupBatchSize);
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Using cleanup interval: " + cleanupInterval
                    + " cleanupBatchSize: " + cleanupBatchSize);
        }

        startCleanupThread();

        String tokenCacheJndi = config.getConfigPropertyValue(Constants.DYNACACHE_CONFIG_DB_TOKENS);
        if (tokenCacheJndi == null || "".equals(tokenCacheJndi)) {
            tokenCacheJndi = Constants.DEFAULT_DYNACACHE_JNDI_DB_TOKENS;
        }
        // getCache(tokenCacheJndi);
    }

    /**
     * Cache the token in the DB
     *
     * @param conn
     * @param clientId
     * @param redirectUri
     * @param scopes
     * @param resourceId
     * @param providerId
     * @param expires
     * @throws Exception
     * @throws SQLSyntaxErrorException
     */
    private void add(Connection conn,
            String clientId,
            String user,
            String scopeString,
            String resource,
            String providerId,
            long expires) throws Exception, SQLSyntaxErrorException {

        /*
         * some customers create a key from clientId, providerId, and user.
         * To avoid duplicateKeyException if scopes change between requests,
         * delete before inserting.
         */
        delete(conn, clientId, user, providerId);

        /*
         * Just do insert. There should not be an existing token with same
         * lookup key.
         *
         * First we calculate the expiry time (0 means don't expire) and
         * generate scopes as a comma-separated list
         */
        PreparedStatement st = null;
        try {
            st = conn.prepareStatement(String.format(INSERT_STMT, TABLE_NAME));
            st.setString(1, clientId);
            st.setString(2, user);
            st.setString(3, providerId);
            st.setString(4, scopeString);
            st.setLong(5, expires);

            JsonObject extendedFields = new JsonObject();// JSONUtil.getJsonObject(extensionPropertiesMap);
            if (resource != null) {
                extendedFields.addProperty(OAuth20Constants.RESOURCE, resource);
            } else {
                extendedFields.addProperty("", "");
            }
            st.setString(6, extendedFields.toString());
            st.execute();
        } finally {
            closeStatement(st);
        }
    }

    private void delete(Connection conn,
            String clientId,
            String user,
            String providerId) {

        PreparedStatement st = null;
        try {
            st = conn.prepareStatement(String.format(DELETE_STMT, TABLE_NAME));
            st.setString(1, clientId);
            st.setString(2, user);
            st.setString(3, providerId);
            st.execute();
        } catch (Exception e) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Caught exception: " + e + " when attempting to delete consent cache entry prior to insert. Entry might not exist. Exception message: " + e.getMessage());
            }
        } finally {
            closeStatement(st);
        }

    }

    /**
      * {@inheritDoc}
      *
      * @param clientId
     * @param providerId
     * @param scopes
     * @param redirectUri
     * @param resourceId
      */
    @Override
    public boolean validateConsent(String clientId, String user, String providerId, String[] scopes, String resource) {
        // TODO Auto-generated method stub
        boolean isValid = false;

        Connection conn = null;
        boolean error = false;
        ResultSet queryResults = null;

        try {
            conn = getDBConnection();
            conn.setAutoCommit(false);

            String query = "SELECT * FROM " + TABLE_NAME + " WHERE "
                    + TableConstants.COL_CONSENT_CLIENT_ID + " = ? AND "
                    + TableConstants.COL_CONSENT_USER + " = ? AND "
                    // + TableConstants.COL_CONSENT_RESOURCE_ID + " = ? AND "
                    + TableConstants.COL_CONSENT_PROVIDER_ID + " = ? ";

            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, clientId);
            st.setString(2, user);
            // st.setString(3, resourceId);
            st.setString(3, providerId);
            queryResults = st.executeQuery();

            while (queryResults != null && !isValid
                    && queryResults.next()) {
                isValid = checkValidityAndScopeAndResource(queryResults, scopes, resource);

            }
        } catch (Exception e) {
            // log but don't fail
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Cannot get consent entry from DB , ", e);
            }
            error = true;
        } finally {
            closeResultSet(queryResults);
            closeConnection(conn, error);
            // _log.exiting(CLASS, methodName, result);
        }

        if (isValid) {
            // cache.put(cacheKey, new CacheEntry(result, result
            // .getLifetimeSeconds()));
        }
        return isValid;
    }

    /**
     * @param queryResults
     * @param scopes2
     * @throws SQLException
     */
    private boolean checkValidityAndScopeAndResource(ResultSet queryResults, String[] scopes2, String resource) throws SQLException {
        // TODO Auto-generated method stub

        boolean isValid = true;

        String scopeStr = queryResults.getString(TableConstants.COL_CONSENT_SCOPE);

        long expires = queryResults.getLong(TableConstants.COL_CONSENT_EXPIRES);

        Date now = new Date();
        if (now.getTime() < expires) {
            for (String scope : scopes2) {
                if (!scopeStr.contains(scope)) {
                    isValid = false;
                    break;
                }
            }
        } else {
            isValid = false;
        }

        if (isValid) {

            // checking resource
            // 240714 Clob clob = queryResults.getClob(TableConstants.COL_CONSENT_EXTENDEDFIELDS);
            JsonObject extendedFields = null;
            boolean gotData = false;
            if (getDBType() != null && getDBType().isClobSupported()) {
                Clob clob = queryResults.getClob(TableConstants.COL_CONSENT_EXTENDEDFIELDS);
                if (clob != null) {
                    gotData = true;
                    extendedFields = (new JsonParser()).parse(clob.getCharacterStream()).getAsJsonObject();
                }
            } else {
                String strClob = queryResults.getString(TableConstants.COL_CONSENT_EXTENDEDFIELDS);
                if (strClob != null) {
                    gotData = true;
                    extendedFields = (new JsonParser()).parse(strClob).getAsJsonObject();
                }
            }

            JsonPrimitive jPrimitive = null;
            if (gotData) {
                // JsonObject extendedFields = (new JsonParser()).parse(clob.getCharacterStream()).getAsJsonObject();

                if (extendedFields != null) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Extended property value  =  ", extendedFields.get("").getAsString());
                    }
                    jPrimitive = extendedFields.getAsJsonPrimitive(OAuth20Constants.RESOURCE);
                }

                if (jPrimitive != null && resource != null) {
                    JsonPrimitive jResource = new JsonPrimitive(resource);
                    isValid = jResource.equals(jPrimitive);
                } else if (jPrimitive == null && resource == null) {
                    isValid = true;
                } else {
                    isValid = false;
                }
            }
        } // end if isValid
        return isValid;
    }

    /**
     * @param clientId
     * @param redirectUri
     * @param scope
     * @param resourceId
     * @param providerId
     * @param keyLifetime
     */
    @Override
    public void addConsent(String clientId, String user, String scopeString, String resource, String providerId, int keyLifeTimeInSeconds) {
        // TODO Auto-generated method stub

        boolean error = true;
        Connection conn = null;
        // String cacheKey = getCacheKey(lookupKey);
        // cache.put(cacheKey, new CacheEntry(entry, lifetime));

        long expires = 0;
        if (keyLifeTimeInSeconds > 0) {
            expires = new Date().getTime() + (1000L * keyLifeTimeInSeconds);
        }

        try {
            conn = getInitializedConnection();
            conn.setAutoCommit(false);
            add(conn, clientId, user, scopeString, resource, providerId, expires);
            error = false;
        } catch (java.sql.SQLSyntaxErrorException sqle) {
            Tr.error(tc, "Internal error adding consent entry: " + sqle.getMessage(), sqle);
        } catch (Exception e) {
            Tr.error(tc, "Internal error adding consent entry: " + e.getMessage(), e);
        } finally {
            closeConnection(conn, error);
            if (tc.isDebugEnabled()) {
                if (error != true) {
                    Tr.debug(tc, "entry added, details follow:");
                    Tr.debug(tc, "  table: " + TABLE_NAME);
                    Tr.debug(tc, "  client id: " + clientId);
                    Tr.debug(tc, "  user: " + user);
                    Tr.debug(tc, "  scopes: " + scopeString);
                    Tr.debug(tc, "  resource: " + resource);
                    // Tr.debug(tc, " resource id: " + resourceId);
                    Tr.debug(tc, "  provider id: " + providerId);
                    Tr.debug(tc, "  expires: " + expires);

                }
            }
        }

    }

}
