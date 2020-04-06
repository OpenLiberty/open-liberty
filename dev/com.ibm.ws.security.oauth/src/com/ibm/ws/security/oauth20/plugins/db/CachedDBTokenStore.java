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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.websphere.crypto.InvalidPasswordEncodingException;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.crypto.UnsupportedCryptoAlgorithmException;
import com.ibm.websphere.ras.annotation.Sensitive;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.oauth20.api.Constants;
import com.ibm.ws.security.oauth20.api.OAuth20EnhancedTokenCache;
import com.ibm.ws.security.oauth20.exception.OAuthDataException;
import com.ibm.ws.security.oauth20.plugins.CacheEntry;
import com.ibm.ws.security.oauth20.plugins.OAuth20TokenImpl;
import com.ibm.ws.security.oauth20.plugins.db.DetectDatabaseType.DBType;
import com.ibm.ws.security.oauth20.util.CacheUtil;
import com.ibm.ws.security.oauth20.util.DynaCacheUtils;
import com.ibm.ws.security.oauth20.util.MessageDigestUtil;
import com.ibm.ws.security.oauth20.web.EndpointUtils;

public class CachedDBTokenStore extends OAuthJDBCImpl implements
        OAuth20EnhancedTokenCache {
    private static final TraceComponent tc = Tr.register(
            CachedDBTokenStore.class, "OAuth20Provider",
            "com.ibm.ws.security.oauth20.resources.ProviderMsgs");

    final static String CLASS = CachedDBTokenStore.class.getName();
    Logger _log = Logger.getLogger(CLASS);

    final static String CONFIG_CLEANUP_INTERVAL = "oauthjdbc.CleanupInterval";
    final static String CONFIG_LIMIT_REFRESH = "oauthjdbc.LimitRefreshToken";
    final static String CONFIG_CLEANUP_BATCH_SIZE = "oauthjdbc.CleanupBatchSize";

    final static String TYPE_AZN_GRANT = "authorization_grant";
    final static String SUBTYPE_REFRESH = "refresh_token";

    int cleanupInterval = 0;
    int cleanupBatchSize = 250;

    boolean limitRefreshTokens = true;

    static Thread cleanupThread = null;

    protected String componentId;
    protected String tableName;
    private String tokenCacheJndi;
    boolean cleanupThreadExitSemaphore = false;

    static Map<String, CacheEntry> cache;
    
    String accessTokenEncoding = OAuth20Constants.PLAIN_ENCODING;
    int accessTokenLength;

    public CachedDBTokenStore() {
    }

    public CachedDBTokenStore(String componentId, DataSource ds, String tableName, @Sensitive Object[] credentials, String tokenCacheJndi, int cleanupInterval, int cleanupBatchSize, boolean limitRefreshTokens, String accessTokenEncoding, int accessTokenLength) {
        super(ds, credentials);
        this.componentId = componentId;
        this.tableName = tableName;
        if (tokenCacheJndi == null || "".equals(tokenCacheJndi)) {
            this.tokenCacheJndi = Constants.DEFAULT_DYNACACHE_JNDI_DB_TOKENS;
        } else {
            this.tokenCacheJndi = tokenCacheJndi;
        }

        this.cleanupInterval = cleanupInterval;
        this.cleanupBatchSize = cleanupBatchSize;
        this.limitRefreshTokens = limitRefreshTokens;
        
        this.accessTokenEncoding = accessTokenEncoding;
        this.accessTokenLength = accessTokenLength;
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        String methodName = "initialize";
        boolean finestLoggable = _log.isLoggable(Level.FINEST);

        if (finestLoggable) {
            _log.logp(Level.FINEST, CLASS, methodName,
                    "Using cleanup interval: " + cleanupInterval
                            + " limitRefreshTokens: " + limitRefreshTokens
                            + " cleanupBatchSize: " + cleanupBatchSize);

        }

        startCleanupThread();
        getCache(tokenCacheJndi);
    }

    @Override
    @FFDCIgnore(java.lang.NumberFormatException.class)
    public void init(OAuthComponentConfiguration config) {
        String methodName = "init";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName);
        }
        boolean finestLoggable = _log.isLoggable(Level.FINEST);
        super.init(config);
        componentId = config.getUniqueId();
        tableName = config.getConfigPropertyValue(CONFIG_TOKEN_TABLE);
        cleanupInterval = config.getConfigPropertyIntValue(CONFIG_CLEANUP_INTERVAL);
        limitRefreshTokens = config.getConfigPropertyBooleanValue(CONFIG_LIMIT_REFRESH);
        try {
            cleanupBatchSize = config.getConfigPropertyIntValue(CONFIG_CLEANUP_BATCH_SIZE);
        } catch (NumberFormatException e) {
            _log.logp(Level.FINEST, CLASS, methodName, "No setting for "
                    + CONFIG_CLEANUP_BATCH_SIZE
                    + ", using default cleanup batch size of: "
                    + cleanupBatchSize);
        }

        if (finestLoggable) {
            _log.logp(Level.FINEST, CLASS, methodName,
                    "Using cleanup interval: " + cleanupInterval
                            + " limitRefreshTokens: " + limitRefreshTokens
                            + " cleanupBatchSize: " + cleanupBatchSize);

        }

        startCleanupThread();

        String tokenCacheJndi = config.getConfigPropertyValue(Constants.DYNACACHE_CONFIG_DB_TOKENS);
        if (tokenCacheJndi == null || "".equals(tokenCacheJndi)) {
            tokenCacheJndi = Constants.DEFAULT_DYNACACHE_JNDI_DB_TOKENS;
        }
        getCache(tokenCacheJndi);

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName);
        }
    }

    private static synchronized void getCache(String tokenCacheJndi) {
        if (cache == null)
            cache = DynaCacheUtils.getDynamicCache(tokenCacheJndi, new String[0], new CacheEntry[0]);
    }

    @Override
    public OAuth20Token getByHash(String hash) {
        String methodName = "getByHash";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, new Object[] { hash });
        }
        OAuth20Token result = null;
        Connection conn = null;
        boolean error = false;
        ResultSet queryResults = null;

        String cacheKey = getCacheKey(hash);
        CacheEntry ce = cache.get(cacheKey);
        if (ce != null) {
            if (!ce.isExpired()) {
                result = ce._token;
            } else {
                cache.remove(cacheKey);
            }
        }

        if (result == null) {
            try {
                conn = getDBConnection();
                conn.setAutoCommit(false);

                String query = "SELECT * FROM " + tableName + " WHERE "
                        + TableConstants.COL_OC2_LOOKUPKEY + " = ? AND "
                        + TableConstants.COL_OC2_COMPONENTID + " = ?";

                PreparedStatement st = conn.prepareStatement(query);
                st.setString(1, hash);
                st.setString(2, componentId);
                queryResults = st.executeQuery();

                while (queryResults != null && result == null
                        && queryResults.next()) {
                    result = createToken(queryResults);
                }
            } catch (Exception e) {
                // log but don't fail
                _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
                error = true;
            } finally {
                closeResultSet(queryResults);
                closeConnection(conn, error);
                _log.exiting(CLASS, methodName, result);
            }

            if (result != null) {
                cache.put(cacheKey, new CacheEntry(result, result
                        .getLifetimeSeconds()));
            } else { // GK1
                // try one more time // @GK1
                try { // @GK1
                    Thread.yield(); // @GK1
                } catch (Exception e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Internal error while yielding", new Object[] { e });
                    }
                } // @GK1
                ce = cache.get(cacheKey); // @GK1
                if (ce != null) { // @GK1
                    if (!ce.isExpired()) { // @GK1
                        result = ce._token; // @GK1
                    } else { // @GK1
                        cache.remove(cacheKey); // @GK1
                    } // @GK1
                } // @GK1
            }
        }

        if (tc.isDebugEnabled()) {
            if (result == null) {
                Tr.debug(tc, "Token not found, lookup details follow:");
                Tr.debug(tc, "  table name: " + tableName);
                Tr.debug(tc, "  lookup key: " + hash);
                Tr.debug(tc, "  component ID: " + componentId);
            }
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName, result);
        }
        return result;
    }

    @Override
    public void removeByHash(String hash) {
        String methodName = "removeByHash";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName, new Object[] { hash });
        }

        Connection conn = null;
        boolean error = false;

        String cacheKey = getCacheKey(hash);
        CacheEntry ce = cache.get(cacheKey);
        if (ce != null) {
            cache.remove(cacheKey);
        }

        try {
            conn = getDBConnection();
            conn.setAutoCommit(false);

            String delete = "DELETE FROM " + tableName + " WHERE "
                    + TableConstants.COL_OC2_LOOKUPKEY + " = ? AND "
                    + TableConstants.COL_OC2_COMPONENTID + " = ?";

            PreparedStatement st = conn.prepareStatement(delete);
            st.setString(1, hash);
            st.setString(2, componentId);
            st.execute();

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Token removed, details follow:");
                Tr.debug(tc, "  table name: " + tableName);
                Tr.debug(tc, "  lookup key: " + hash);
                Tr.debug(tc, "  component ID: " + componentId);
            }
        } catch (Exception e) {
            // log but don't fail
            _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
            error = true;
        } finally {
            closeConnection(conn, error);
            if (tc.isEntryEnabled()) {
                Tr.exit(tc, methodName);
            }
        }
    }

    @Override
    public void add(String lookupKeyParm, OAuth20Token entry, int lifetime) {
        String methodName = "add";
        String lookupKey = lookupKeyParm;
        boolean shouldHash = false;
        CacheUtil cacheUtil = new CacheUtil();
        if (cacheUtil.shouldHash(entry, this.accessTokenEncoding)) {
            shouldHash = true;
            lookupKey = cacheUtil.computeHash(lookupKeyParm, this.accessTokenEncoding);
        } else {
            lookupKey = MessageDigestUtil.getDigest(lookupKeyParm);
        }
        _log.entering(CLASS, methodName, new Object[] { lookupKeyParm, lookupKey });
        Connection conn = null;
        boolean error = false;

        String cacheKey = getCacheKey(lookupKey);
        cache.put(cacheKey, new CacheEntry(entry, lifetime));

        long expires = 0;
        if (entry.getLifetimeSeconds() > 0) {
            expires = entry.getCreatedAt() + (1000L * entry.getLifetimeSeconds());
        }
        StringBuffer scopes = new StringBuffer();
        String[] ascopes = entry.getScope();
        if (ascopes != null && ascopes.length > 0) {
            for (int i = 0; i < ascopes.length; i++) {
                scopes.append(ascopes[i].trim());
                if (i < (ascopes.length - 1)) {
                    scopes.append(" ");
                }
            }
        }
        String tokenId = entry.getId();
        String tokenString = entry.getTokenString();
        if (shouldHash) {
            tokenId = cacheUtil.computeHash(tokenId, this.accessTokenEncoding);
            tokenString = cacheUtil.computeHash(tokenString, this.accessTokenEncoding);
        } else {
            tokenString = PasswordUtil.passwordEncode(tokenString);
        }
        
        try {
            conn = getDBConnection();
            conn.setAutoCommit(false);

            /*
             * Just do insert. There should not be an existing token with same
             * lookup key.
             */

            /*
             * first we calculate the expiry time (0 means don't expire) and
             * generate scopes as a comma-separated list
             */
            String insert = "INSERT INTO " + tableName + " ("
                    + TableConstants.COL_OC2_LOOKUPKEY + ","
                    + TableConstants.COL_OC2_UNIQUEID + ","
                    + TableConstants.COL_OC2_COMPONENTID + ","
                    + TableConstants.COL_OC2_TYPE + ","
                    + TableConstants.COL_OC2_SUBTYPE + ","
                    + TableConstants.COL_OC2_CREATEDAT + ","
                    + TableConstants.COL_OC2_LIFETIME + ","
                    + TableConstants.COL_OC2_EXPIRES + ","
                    + TableConstants.COL_OC2_TOKENSTRING + ","
                    + TableConstants.COL_OC2_CLIENTID + ","
                    + TableConstants.COL_OC2_USERNAME + ","
                    + TableConstants.COL_OC2_SCOPE + ","
                    + TableConstants.COL_OC2_REDIRECTURI + ","
                    + TableConstants.COL_OC2_STATEID
                    + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            PreparedStatement st = conn.prepareStatement(insert);
            st.setString(1, lookupKey);
            st.setString(2, tokenId);
            // use the componentId of the tokenCache
            st.setString(3, componentId);
            st.setString(4, entry.getType());
            st.setString(5, entry.getSubType());
            st.setLong(6, entry.getCreatedAt());
            st.setInt(7, entry.getLifetimeSeconds());
            st.setLong(8, expires);
            // encoded token string
            st.setString(9, tokenString);
            st.setString(10, entry.getClientId());
            st.setString(11, entry.getUsername());
            st.setString(12, scopes.toString());
            st.setString(13, entry.getRedirectUri());
            st.setString(14, entry.getStateId());
            st.execute();
        } catch (Exception e) {
            // log but don't fail
            _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
            error = true;
            // ffdc @GK1
            com.ibm.ws.ffdc.FFDCFilter.processException(e, // GK1
                    "com.ibm.ws.security.adminTasks.securityDomain.SecurityConfigProvider", "334", this); // GK1
        } finally {
            closeConnection(conn, error);
            if (tc.isDebugEnabled()) {
                if (error != true) {
                    Tr.debug(tc, "Token added, details follow:");
                    Tr.debug(tc, "  table: " + tableName);
                    Tr.debug(tc, "  key: " + lookupKey);
                    Tr.debug(tc, "  id: " + entry.getId());
                    Tr.debug(tc, "  component: " + componentId);
                    Tr.debug(tc, "  type: " + entry.getType());
                    Tr.debug(tc, "  subtype: " + entry.getSubType());
                    Tr.debug(tc, "  creation: " + entry.getCreatedAt());
                    Tr.debug(tc, "  lifetime: " + entry.getLifetimeSeconds());
                    Tr.debug(tc, "  expires: " + expires);
                    Tr.debug(tc, "  (password skipped)");
                    Tr.debug(tc, "  client id: " + entry.getClientId());
                    Tr.debug(tc, "  username: " + entry.getUsername());
                    Tr.debug(tc, "  scopes: " + scopes.toString());
                    Tr.debug(tc, "  redirect: " + entry.getRedirectUri());
                    Tr.debug(tc, "  state: " + entry.getStateId());
                }
            }
            _log.exiting(CLASS, methodName);
        }
    }

    @Override
    public Collection<OAuth20Token> getAll() {
        String methodName = "getAll";
        _log.entering(CLASS, methodName, new Object[] {});
        Collection<OAuth20Token> results = new ArrayList<OAuth20Token>();
        Connection conn = null;
        boolean error = false;
        ResultSet queryResults = null;

        // Don't use the cache. This is an admin/test command

        try {
            conn = getDBConnection();
            conn.setAutoCommit(false);

            String query = "SELECT * FROM " + tableName + " WHERE "
                    + TableConstants.COL_OC2_COMPONENTID + " =? ";

            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, componentId);
            queryResults = st.executeQuery();

            while (queryResults != null && queryResults.next()) {
                OAuth20Token result = createToken(queryResults);
                results.add(result);
            }
        } catch (Exception e) {
            // log but don't fail
            _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
            error = true;
        } finally {
            closeResultSet(queryResults);
            closeConnection(conn, error);
            _log.exiting(CLASS, methodName, results);
        }

        return results;
    }

    protected OAuth20Token createToken(ResultSet queryResults)
            throws SQLException {
        String methodName = "createToken";
        OAuth20Token result = null;

        String uniqueId = queryResults.getString(TableConstants.COL_OC2_UNIQUEID);
        String componentId = queryResults.getString(TableConstants.COL_OC2_COMPONENTID);
        String type = queryResults.getString(TableConstants.COL_OC2_TYPE);
        String subType = queryResults.getString(TableConstants.COL_OC2_SUBTYPE);
        long createdAt = queryResults.getLong(TableConstants.COL_OC2_CREATEDAT);
        int lifetime = queryResults.getInt(TableConstants.COL_OC2_LIFETIME);
        long expires = queryResults.getLong(TableConstants.COL_OC2_EXPIRES);
        String tokenString = queryResults.getString(TableConstants.COL_OC2_TOKENSTRING);
        // decode token string
        // TODO : see if I can check on the grant type for app-password here
        try {
            if (!PasswordUtil.isHashed(tokenString)) {
                tokenString = PasswordUtil.passwordDecode(tokenString);
            }

        } catch (Exception e) {

        }
        String clientId = queryResults.getString(TableConstants.COL_OC2_CLIENTID);
        String username = queryResults.getString(TableConstants.COL_OC2_USERNAME);
        String scopeStr = queryResults.getString(TableConstants.COL_OC2_SCOPE);
        String redirectUri = queryResults.getString(TableConstants.COL_OC2_REDIRECTURI);
        String stateId = queryResults.getString(TableConstants.COL_OC2_STATEID);

        String[] scopes = null;
        if (scopeStr != null) {
            scopes = scopeStr.split(" ");
        }

        // double-check it's not expired
        Date now = new Date();
        if (now.getTime() < expires) {
            // not yet expired
            result = new OAuth20TokenImpl(uniqueId, componentId, type, subType,
                    createdAt, lifetime, tokenString, clientId, username,
                    scopes, redirectUri, stateId, null, null);
        } else {
            // trace @GK1
            com.ibm.ws.ffdc.FFDCFilter.processException(new Exception("The OAuth20Token is expired already"), // GK1
                    "com.ibm.ws.security.adminTasks.securityDomain.SecurityConfigProvider", "441", this); // GK1
            _log.logp(Level.FINEST, CLASS, methodName, "The OAuth20Token is expired already");
        }

        return result;
    }

    @SuppressWarnings("unused")
    private void dumpTokens(Connection conn) {
        String methodName = "dumpTokens";
        _log.entering(CLASS, methodName);
        ResultSet queryResults = null;
        boolean finestLoggable = _log.isLoggable(Level.FINEST);
        try {
            String query = "SELECT * FROM " + tableName;

            PreparedStatement st = conn.prepareStatement(query);
            queryResults = st.executeQuery();

            while (queryResults != null && queryResults.next()) {
                String type = queryResults.getString(TableConstants.COL_OC2_TYPE);
                String subType = queryResults.getString(TableConstants.COL_OC2_SUBTYPE);
                long expires = queryResults.getLong(TableConstants.COL_OC2_EXPIRES);
                String tokenString = queryResults.getString(TableConstants.COL_OC2_TOKENSTRING);

                // double-check it's not expired
                Date now = new Date();
                boolean expired = now.getTime() >= expires;

                if (finestLoggable) {
                    _log.logp(Level.FINEST, CLASS, methodName, "token: "
                            + tokenString + " type: " + type + " subtype: "
                            + subType + " expires: " + expires + " expired: "
                            + expired);
                }
            }
        } catch (Exception e) {
            // log but don't fail
            _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
        } finally {
            closeResultSet(queryResults);
            _log.exiting(CLASS, methodName);
        }
    }

    void startCleanupThread() {
        synchronized (this.getClass()) {
            if (cleanupThread == null) {
                if (cleanupInterval > 0) {
                    cleanupThread = new CleanupThread(this);
                    cleanupThread.start();
                    // not using executor because this whole class is likely unused in Liberty
                }
            }
        }
    }

    @Override
    public void stopCleanupThread() {
        cleanupThreadExitSemaphore = true;
    }

    @Trivial
    class CleanupThread extends Thread {
        final String MYCLASS = CleanupThread.class.getName();
        Logger _log = Logger.getLogger(MYCLASS);

        protected boolean fineLoggable;
        protected boolean finestLoggable;

        protected CachedDBTokenStore _me;
        protected DBType databaseType = null;

        public CleanupThread(CachedDBTokenStore me) {
            _me = me;
        }

        @Override
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

            fineLoggable = _log.isLoggable(Level.FINE);
            finestLoggable = _log.isLoggable(Level.FINEST);

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
                        Tr.debug(tc, "cacheddb token store cleanup thread exiting because exit semaphore is true");
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
                try {
                    conn = _me.getDBConnection();
                    conn.setAutoCommit(false);

                    /*
                     * Note the COL_OC2_EXPIRES > 0. A value <= 0 implies
                     * "forever" so we don't delete those.
                     */
                    String delete;
                    if (DBType.DB2 == databaseType) {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Running cleanup with LIMIT in DB2");
                        }
                        delete = "DELETE FROM (SELECT " + TableConstants.COL_OC2_EXPIRES + " FROM " + tableName + " WHERE "
                                + TableConstants.COL_OC2_EXPIRES + " > 0 AND "
                                + TableConstants.COL_OC2_EXPIRES + " <= ?  ORDER BY " + TableConstants.COL_OC2_EXPIRES + " FETCH FIRST " + cleanupBatchSize + " ROWS ONLY)";
                        numDeleted += cleanupBatchSize;
                    } else if (databaseType.isSqlLimitSupported()) {
                        _log.logp(Level.FINEST, MYCLASS, methodName,
                                "Running cleanup with LIMIT");
                        delete = "DELETE FROM " + tableName + " WHERE "
                                + TableConstants.COL_OC2_EXPIRES + " > 0 AND "
                                + TableConstants.COL_OC2_EXPIRES + " <= ? "
                                + "LIMIT " + cleanupBatchSize;
                        numDeleted += cleanupBatchSize;
                    } else {
                        _log.logp(Level.FINEST, MYCLASS, methodName,
                                "Running cleanup without LIMIT");
                        delete = "DELETE FROM " + tableName + " WHERE "
                                + TableConstants.COL_OC2_EXPIRES + " > 0 AND "
                                + TableConstants.COL_OC2_EXPIRES + " <= ?";
                        numDeleted += expiredCount;
                    }

                    PreparedStatement st = conn.prepareStatement(delete);
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
                    closeConnection(conn, error);
                }
            }
            _log.exiting(MYCLASS, methodName);
        }
    }

    protected int getExpiredCount(long nowTime) {
        String methodName = "getExpiredCount";
        _log.entering(CLASS, methodName, new Object[] {});

        int result = -1;

        ResultSet queryResults = null;
        PreparedStatement st = null;

        Connection conn = null;
        try {
            conn = getDBConnection();
            conn.setAutoCommit(false);
            _log.logp(Level.FINE, CLASS, methodName,
                    "Checking for expired with time: " + nowTime);

            String query = "SELECT COUNT(*) AS \"TOTAL\" FROM " + tableName
                    + " WHERE " + TableConstants.COL_OC2_EXPIRES + " > 0 AND "
                    + TableConstants.COL_OC2_EXPIRES + " <= ? ";
            st = conn.prepareStatement(query);
            st.setLong(1, nowTime);
            queryResults = st.executeQuery();

            while (queryResults != null && queryResults.next()) {
                result = queryResults.getInt("TOTAL");
                _log.logp(Level.FINE, CLASS, methodName, "Updated result to: "
                        + result);
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
                    _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(),
                            e);
                }
            }
            closeConnection(conn, false);
            _log.exiting(CLASS, methodName, result);
        }
        return result;
    }

    @Override
    public int getNumTokens(String username, String client) {
        String methodName = "getNumTokens";
        _log.entering(CLASS, methodName, new Object[] { username, client });

        int result = -1;

        ResultSet queryResults = null;
        PreparedStatement st = null;

        Connection conn = null;
        try {
            conn = getDBConnection();
            conn.setAutoCommit(false);

            String query = "SELECT COUNT(*) AS \"TOTAL\" FROM " + tableName
                    + " WHERE " + TableConstants.COL_OC2_COMPONENTID
                    + " =? AND " + TableConstants.COL_OC2_CLIENTID + " =? AND "
                    + TableConstants.COL_OC2_USERNAME + " =?";

            st = conn.prepareStatement(query);
            st.setString(1, componentId);
            st.setString(2, client);
            st.setString(3, username);
            queryResults = st.executeQuery();

            while (queryResults != null && queryResults.next()) {
                result = queryResults.getInt("TOTAL");
                _log.logp(Level.FINE, CLASS, methodName, "Updated result to: "
                        + result);
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
            closeConnection(conn, false);
            _log.exiting(CLASS, methodName, result);
        }
        return result;
    }

    @Override
    public Collection<OAuth20Token> getAllUserTokens(String username) {
        String methodName = "getAllAccessTokens";
        _log.entering(CLASS, methodName, new Object[] { username });
        Collection<OAuth20Token> results = new ArrayList<OAuth20Token>();
        Connection conn = null;
        boolean error = false;
        ResultSet queryResults = null;

        // No DB cache index on username, just load from DB

        try {
            conn = getDBConnection();
            conn.setAutoCommit(false);

            String query = "SELECT * FROM " + tableName + " WHERE "
                    + TableConstants.COL_OC2_COMPONENTID + " =? AND "
                    + TableConstants.COL_OC2_USERNAME + " =?";

            PreparedStatement st = conn.prepareStatement(query);
            st.setString(1, componentId);
            st.setString(2, username);
            queryResults = st.executeQuery();

            while (queryResults != null && queryResults.next()) {
                OAuth20Token result = createToken(queryResults);
                if (result != null) {
                    results.add(result);
                }
            }
        } catch (Exception e) {
            // log but don't fail
            _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
            error = true;
        } finally {
            closeResultSet(queryResults);
            closeConnection(conn, error);
            _log.exiting(CLASS, methodName, results);
        }

        return results;
    }

    protected String getCacheKey(String lookupKey) {
        return lookupKey + "_" + componentId;
    }

    @Override
    public OAuth20Token get(String lookupKey) {
        Tr.entry(tc, "get", lookupKey);
        String hash = lookupKey;// MessageDigestUtil.getDigest(lookupKey);
        if (!PasswordUtil.isHashed(hash)) {
            if (!OAuth20Constants.PLAIN_ENCODING.equals(this.accessTokenEncoding) || (lookupKey.length() == (this.accessTokenLength + 2))) { // app-password or app-token
                hash = EndpointUtils.computeTokenHash(lookupKey, this.accessTokenEncoding);
            } else {
                hash = MessageDigestUtil.getDigest(lookupKey);
            }
        }
        return getByHash(hash);
    }

    @Override
    public void remove(String lookupKey) {
        Tr.entry(tc, "remove", lookupKey);
        String hash = lookupKey;// MessageDigestUtil.getDigest(lookupKey);
        if (!PasswordUtil.isHashed(hash)) {
            if (!OAuth20Constants.PLAIN_ENCODING.equals(this.accessTokenEncoding) || (lookupKey.length() == (this.accessTokenLength + 2))) { // app-password or app-token
                hash = EndpointUtils.computeTokenHash(lookupKey, this.accessTokenEncoding);
            } else {
                hash = MessageDigestUtil.getDigest(lookupKey);
            }
        }
        removeByHash(hash);
    }
    
    /** {@inheritDoc} */
    @Override
    public void addByHash(String hash, OAuth20Token entry, int lifetime) {
        

    }

/** {@inheritDoc} */
    @Override
    public Collection<OAuth20Token> getMatchingTokens(String username, String client, String tokenType) {
        String methodName = "getMatchingTokens";
        _log.entering(CLASS, methodName, new Object[] { username });
        Collection<OAuth20Token> results = new ArrayList<OAuth20Token>();
        Connection conn = null;
        boolean error = false;
        ResultSet queryResults = null;
        

        try {
            conn = getDBConnection();
            conn.setAutoCommit(false);

            if (tokenType != null) {
                String query = "SELECT * FROM " + tableName + " WHERE "
                        + TableConstants.COL_OC2_COMPONENTID + " =? AND "
                        + TableConstants.COL_OC2_CLIENTID + " =? AND "
                        + TableConstants.COL_OC2_USERNAME + " =? AND "
                        + TableConstants.COL_OC2_STATEID+ " =?";

                PreparedStatement st = conn.prepareStatement(query);
                st.setString(1, componentId);
                st.setString(2, client);
                st.setString(3, username);
                st.setString(4, tokenType);
                queryResults = st.executeQuery();
            } else {
                String query = "SELECT * FROM " + tableName + " WHERE "
                        + TableConstants.COL_OC2_COMPONENTID + " =? AND "
                        + TableConstants.COL_OC2_CLIENTID + " =? AND "
                        + TableConstants.COL_OC2_USERNAME + " =?";         

                PreparedStatement st = conn.prepareStatement(query);
                st.setString(1, componentId);
                st.setString(2, client);
                st.setString(3, username);
                queryResults = st.executeQuery();
            }    

            while (queryResults != null && queryResults.next()) {
                OAuth20Token result = createToken(queryResults);
                if (result != null) {
                    results.add(result);
                }
            }
        } catch (Exception e) {
            // log but don't fail
            _log.logp(Level.SEVERE, CLASS, methodName, e.getMessage(), e);
            error = true;
        } finally {
            closeResultSet(queryResults);
            closeConnection(conn, error);
            _log.exiting(CLASS, methodName, results);
        }

        return results;

    }

    /** {@inheritDoc} */
    @Override
    public Collection<OAuth20Token> getUserAndClientTokens(String username, String client) {
        
        return getMatchingTokens(username, client, null);
    }
}
