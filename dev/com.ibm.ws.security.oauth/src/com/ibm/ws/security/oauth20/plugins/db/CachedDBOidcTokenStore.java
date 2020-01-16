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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.sql.DataSource;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ibm.oauth.core.api.config.OAuthComponentConfiguration;
import com.ibm.oauth.core.api.oauth20.token.OAuth20Token;
import com.ibm.oauth.core.internal.oauth20.OAuth20Constants;
import com.ibm.oauth.core.util.JSONUtil;
import com.ibm.websphere.crypto.PasswordUtil;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
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
import com.ibm.ws.security.oauth20.util.OIDCConstants;
import com.ibm.ws.security.oauth20.web.EndpointUtils;

/*
 * This class handles token caching in the DB
 */
public class CachedDBOidcTokenStore extends OAuthJDBCImpl
        implements OAuth20EnhancedTokenCache {
    private static final TraceComponent tc = Tr.register(CachedDBOidcTokenStore.class, "OAuth20Provider", null);

    private final static String INSERT_STMT = "INSERT INTO %s ("
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
            + TableConstants.COL_OC2_STATEID + ","
            + TableConstants.COL_OC2_EXTENDEDFIELDS
            + ") VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";

    final static String CONFIG_CLEANUP_INTERVAL = "oauthjdbc.CleanupInterval";
    final static String CONFIG_LIMIT_REFRESH = "oauthjdbc.LimitRefreshToken";
    final static String CONFIG_CLEANUP_BATCH_SIZE = "oauthjdbc.CleanupBatchSize";

    final static String TYPE_AZN_GRANT = "authorization_grant";
    final static String SUBTYPE_REFRESH = "refresh_token";

    int cleanupInterval = 0;
    int cleanupBatchSize = 250;

    boolean limitRefreshTokens = true;

    protected String componentId;
    protected String tableName;
    private String tokenCacheJndi;
    private boolean columnAdded = false;
    private ExecutorService executorService;
    @SuppressWarnings("rawtypes")
    Future cleanupThreadFuture;

    boolean cleanupThreadExitSemaphore = false;
    String accessTokenEncoding = OAuth20Constants.PLAIN_ENCODING;
    int accessTokenLength;

    static Map<String, CacheEntry> cache;

    public CachedDBOidcTokenStore() {
    }

    public CachedDBOidcTokenStore(String componentId,
            ExecutorService executorsvc,
            DataSource dataSource,
            String tableName,
            @Sensitive Object[] credentials,
            String tokenCacheJndi,
            int cleanupInterval,
            int cleanupBatchSize,
            boolean limitRefreshTokens) {
        super(dataSource, credentials);
        this.componentId = componentId;
        this.tableName = tableName;
        if (tokenCacheJndi == null || "".equals(tokenCacheJndi)) {
            this.tokenCacheJndi = Constants.DEFAULT_DYNACACHE_JNDI_DB_TOKENS;
        } else {
            this.tokenCacheJndi = tokenCacheJndi;
        }
        this.executorService = executorsvc;
        this.cleanupInterval = cleanupInterval;
        this.cleanupBatchSize = cleanupBatchSize;
        this.limitRefreshTokens = limitRefreshTokens;
    }

    public CachedDBOidcTokenStore(String componentId,
            ExecutorService executorsvc,
            DataSource dataSource,
            String tableName,
            @Sensitive Object[] credentials,
            String tokenCacheJndi,
            int cleanupInterval,
            int cleanupBatchSize,
            boolean limitRefreshTokens, String accessTokenEncoding, int accessTokenLength) {
        super(dataSource, credentials);
        this.componentId = componentId;
        this.tableName = tableName;
        if (tokenCacheJndi == null || "".equals(tokenCacheJndi)) {
            this.tokenCacheJndi = Constants.DEFAULT_DYNACACHE_JNDI_DB_TOKENS;
        } else {
            this.tokenCacheJndi = tokenCacheJndi;
        }
        this.executorService = executorsvc;
        this.cleanupInterval = cleanupInterval;
        this.cleanupBatchSize = cleanupBatchSize;
        this.limitRefreshTokens = limitRefreshTokens;
        this.accessTokenEncoding = accessTokenEncoding;
        this.accessTokenLength = accessTokenLength;
    }

    /** {@inheritDoc} */
    @Override
    public void initialize() {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Using cleanup interval: " + cleanupInterval
                    + " limitRefreshTokens: " + limitRefreshTokens
                    + " cleanupBatchSize: " + cleanupBatchSize);
        }
        startCleanupThread();
        getCache(tokenCacheJndi);
    }

    @Override
    @FFDCIgnore(java.lang.NumberFormatException.class)
    public void init(OAuthComponentConfiguration config) {
        super.init(config);
        componentId = config.getUniqueId();
        tableName = config.getConfigPropertyValue(CONFIG_TOKEN_TABLE);
        cleanupInterval = config.getConfigPropertyIntValue(CONFIG_CLEANUP_INTERVAL);
        limitRefreshTokens = config.getConfigPropertyBooleanValue(CONFIG_LIMIT_REFRESH);
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
                    + " limitRefreshTokens: " + limitRefreshTokens
                    + " cleanupBatchSize: " + cleanupBatchSize);
        }

        startCleanupThread();

        String tokenCacheJndi = config.getConfigPropertyValue(Constants.DYNACACHE_CONFIG_DB_TOKENS);
        if (tokenCacheJndi == null || "".equals(tokenCacheJndi)) {
            tokenCacheJndi = Constants.DEFAULT_DYNACACHE_JNDI_DB_TOKENS;
        }
        getCache(tokenCacheJndi);
    }

    private static synchronized void getCache(String tokenCacheJndi) {
        if (cache == null)
            cache = DynaCacheUtils.getDynamicCache(tokenCacheJndi, new String[0], new CacheEntry[0]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OAuth20Token getByHash(String hash) {
        OAuth20Token result = null;
        Connection conn = null;
        ResultSet queryResults = null;
        boolean error = true;
        boolean isSqlException = false;
        String cacheKey = getCacheKey(hash);
        CacheEntry ce = cache.get(cacheKey);
        if (ce != null) {
            if (!ce.isExpired()) {
                result = ce._token;
                if (result != null) {
                    result.setLastAccess();
                }
            } else {
                // trace message about token being expired
                Tr.info(tc, "Token with the cacheKey: " + cacheKey + " has expired.");
                cache.remove(cacheKey);
            }
        }
        if (result == null) {
            try {
                conn = getInitializedConnection();
                result = getByHash(conn, hash, queryResults);
                error = false;
            } catch (SQLException sqle) {
                isSqlException = true;
                if (!columnAdded) {
                    try {
                        // Try adding the EXTENDEDFIELDS column to the table
                        new DynamicDBMigrator().addColumnToTable(conn,
                                tableName,
                                TableConstants.COL_OC2_EXTENDEDFIELDS,
                                "CLOB");
                        // columnAdded = true;
                        result = getByHash(conn, hash, queryResults);
                        error = false;
                    } catch (Exception e) {
                        Tr.error(tc, "Internal error getting token by hash key: " + e.getMessage(), e);
                    }
                } else {
                    Tr.error(tc, "Internal error getting token by hash key: " + sqle.getMessage(), sqle);
                }
            } catch (Exception e) {
                Tr.error(tc, "Internal error getting token by hash key: " + e.getMessage(), e);
                error = true;
            } finally {
                if (isSqlException && !columnAdded) {
                    columnAdded = true; // only attempt once
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "getByHash , columnAdded is true");
                    }
                }

                closeResultSet(queryResults);
                closeConnection(conn, error);
            }
            if (result != null) {
                cache.put(cacheKey, new CacheEntry(result, result
                        .getLifetimeSeconds()));
            } else {
                // try the cache one more time
                try {
                    Thread.yield();
                } catch (Exception e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Internal error while yielding", new Object[] { e });
                    }
                }
                ce = cache.get(cacheKey);
                if (ce != null) {
                    if (!ce.isExpired()) {
                        result = ce._token;
                    } else {
                        cache.remove(cacheKey);
                    }
                }
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
        return result;
    }

    /**
     * Get a token from the DB cache
     *
     * @param conn
     * @param hash
     * @param queryResults
     * @return
     * @throws SQLException
     */
    private OAuth20Token getByHash(Connection conn,
            String hash,
            ResultSet queryResults) throws SQLException {
        PreparedStatement st = null;
        OAuth20Token result = null;
        try {
            String query = "SELECT * FROM " + tableName + " WHERE "
                    + TableConstants.COL_OC2_LOOKUPKEY + " = ? AND "
                    + TableConstants.COL_OC2_COMPONENTID + " = ?";
            st = conn.prepareStatement(query);
            st.setString(1, hash);
            st.setString(2, componentId);
            queryResults = st.executeQuery();
            while (queryResults != null &&
                    result == null &&
                    queryResults.next()) {
                result = createToken(queryResults);
            }
        } finally {
            closeResultSet(queryResults);
            closeStatement(st);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeByHash(String hash) {
        Connection conn = null;
        boolean error = true;
        boolean isSqlException = false;
        String cacheKey = getCacheKey(hash);
        CacheEntry ce = cache.get(cacheKey);
        if (ce != null) {
            cache.remove(cacheKey);
        }
        try {
            conn = getInitializedConnection();
            removeByHash(conn, hash);
            error = false;
        } catch (java.sql.SQLException sqle) {
            isSqlException = true;
            if (!columnAdded) {
                try {
                    // Try adding the EXTENDEDFIELDS column to the table
                    new DynamicDBMigrator().addColumnToTable(conn,
                            tableName,
                            TableConstants.COL_OC2_EXTENDEDFIELDS,
                            "CLOB");
                    // columnAdded = true;
                    removeByHash(conn, hash);
                    error = false;
                } catch (Exception e) {
                    Tr.error(tc, "Internal error removing token by hash key: " + e.getMessage(), e);
                }
            } else {
                Tr.error(tc, "Internal error removing token by hash key: " + sqle.getMessage(), sqle);
            }
        } catch (Exception e) {
            // log but don't fail
            Tr.error(tc, "Internal error removing token by hash key: " + e.getMessage(), e);
        } finally {
            if (isSqlException && !columnAdded) {
                columnAdded = true; // only attempt once
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "removeByHash , columnAdded is true");
                }
            }

            closeConnection(conn, error);
        }
    }

    /**
     * Remove the token matching the hash from the DB cache
     *
     * @param conn
     * @param hash
     * @throws SQLException
     */
    private void removeByHash(Connection conn, String hash) throws SQLException {
        PreparedStatement st = null;
        try {
            String delete = "DELETE FROM " + tableName + " WHERE "
                    + TableConstants.COL_OC2_LOOKUPKEY + " = ? AND "
                    + TableConstants.COL_OC2_COMPONENTID + " = ?";
            st = conn.prepareStatement(delete);
            st.setString(1, hash);
            st.setString(2, componentId);
            st.execute();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Token removed, details follow:");
                Tr.debug(tc, "  table name: " + tableName);
                Tr.debug(tc, "  lookup key: " + hash);
                Tr.debug(tc, "  component ID: " + componentId);
            }
        } finally {
            closeStatement(st);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @FFDCIgnore(java.sql.SQLSyntaxErrorException.class)
    public void add(String lookupKeyParm, OAuth20Token entry, int lifetime) {
        String lookupKey = lookupKeyParm;
        boolean shouldHash = false;
        CacheUtil cacheUtil = new CacheUtil();
        if (cacheUtil.shouldHash(entry, this.accessTokenEncoding)) {
            shouldHash = true;
            lookupKey = cacheUtil.computeHash(lookupKeyParm, this.accessTokenEncoding);
        } else {
            lookupKey = MessageDigestUtil.getDigest(lookupKeyParm);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "lookupKey: " + lookupKey);
        }
        boolean error = true;
        boolean isSqlException = false;
        Connection conn = null;
        String cacheKey = getCacheKey(lookupKey);
        if (!shouldHash) {
            // make sure local and db caches has same data
            cache.put(cacheKey, new CacheEntry(entry, lifetime));
        }

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
        try {
            conn = getInitializedConnection();
            add(conn, entry, lookupKey, expires, scopes, false, shouldHash);
            error = false;
        } catch (java.sql.SQLSyntaxErrorException sqle) {
            isSqlException = true;
            if (!columnAdded) {
                try {
                    // Try adding the EXTENDEDFIELDS column to the table
                    new DynamicDBMigrator().addColumnToTable(conn,
                            tableName,
                            TableConstants.COL_OC2_EXTENDEDFIELDS,
                            "CLOB");
                    // columnAdded = true;
                    add(conn, entry, lookupKey, expires, scopes, false, shouldHash);
                    error = false;
                } catch (Exception e) {
                    Tr.error(tc, "Internal error adding token: " + e.getMessage(), e);
                }
            } else {
                Tr.error(tc, "Internal error adding token: " + sqle.getMessage(), sqle);
            }
        } catch (Exception e) {
            Tr.error(tc, "Internal error adding token: " + e.getMessage(), e);
        } finally {
            if (isSqlException && !columnAdded) {
                columnAdded = true; // only attempt once
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "add , columnAdded is true");
                }
            }

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
                    Tr.debug(tc, "  grantType: " + entry.getGrantType());
                    Tr.debug(tc, "  extensionProperties: " + entry.getExtensionProperties());
                }
            }
        }
    }

    /**
     * Cache the token in the DB
     *
     * @param conn
     * @param entry
     * @param lookupKey
     * @param expires
     * @param scopes
     * @throws Exception
     * @throws SQLSyntaxErrorException
     */
    private void add(Connection conn,
            OAuth20Token entry,
            String lookupKey,
            long expires,
            StringBuffer scopes, boolean alreadyHashed, boolean shouldHash) throws Exception, SQLSyntaxErrorException {
        /*
         * Just do insert. There should not be an existing token with same
         * lookup key.
         *
         * First we calculate the expiry time (0 means don't expire) and
         * generate scopes as a comma-separated list
         */
        String tokenId = entry.getId();
        String tokenString = entry.getTokenString();
        CacheUtil cacheUtil = new CacheUtil(this);
        if (!alreadyHashed) {
            if (shouldHash) {
                tokenId = cacheUtil.computeHash(tokenId, this.accessTokenEncoding);
                tokenString = cacheUtil.computeHash(tokenString, this.accessTokenEncoding);
            } else {
                tokenString = PasswordUtil.passwordEncode(tokenString);
            }
        }

        PreparedStatement st = null;
        try {
            st = conn.prepareStatement(String.format(INSERT_STMT, tableName));
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
            JsonObject extendedFields = JSONUtil.getJsonObject(entry.getExtensionProperties());
            if (extendedFields == null)
                extendedFields = new JsonObject();
            extendedFields.addProperty(OAuth20Constants.GRANT_TYPE, entry.getGrantType());

            String refreshId = null, accessId = null;
            if (OAuth20Constants.TOKENTYPE_ACCESS_TOKEN.equals(entry.getType())) {
                if ((refreshId = cacheUtil.getRefreshTokenId(entry)) != null) {
                    extendedFields.addProperty(OAuth20Constants.REFRESH_TOKEN_ID, refreshId);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Access Token is added to cache , refresh token id " + refreshId);
                    }
                }
            } else if (OIDCConstants.TOKENTYPE_ID_TOKEN.equals(entry.getType())) {
                if ((accessId = cacheUtil.getAccessTokenId(entry)) != null) {
                    extendedFields.addProperty(OAuth20Constants.ACCESS_TOKEN_ID, accessId);
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "ID Token is added to cache , access token id " + accessId);
                    }
                }
            }
            if (!alreadyHashed) {
                if (shouldHash) {
                    OAuth20Token localToken = new OAuth20TokenImpl(tokenId, componentId, entry.getType(), entry.getSubType(),
                            entry.getCreatedAt(), entry.getLifetimeSeconds(), tokenString, entry.getClientId(), entry.getUsername(),
                            entry.getScope(), entry.getRedirectUri(), entry.getStateId(), entry.getExtensionProperties(), entry.getGrantType());
                    if (localToken != null) {
                        if (refreshId != null) {
                            ((OAuth20TokenImpl) localToken).setRefreshTokenKey(refreshId);
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "Saving the token to in-memory, refresh token id in local token = " + refreshId);
                            }
                        } else if (accessId != null) {
                            ((OAuth20TokenImpl) localToken).setAccessTokenKey(accessId);
                            if (tc.isDebugEnabled()) {
                                Tr.debug(tc, "Saving the token to in-memory, , access token id in local token = " + accessId);
                            }
                        }
                    }
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Saving the token to in-memory, local token id = " + tokenId);
                    }
                    String cacheKey = getCacheKey(lookupKey);
                    // make sure local and db caches has same data
                    cache.put(cacheKey, new CacheEntry(localToken, localToken.getLifetimeSeconds()));
                }
            }
            String jsonString = extendedFields.toString();
            st.setString(15, jsonString);
            st.execute();
        } finally {
            closeStatement(st);
        }
    }

    /** {@inheritDoc} */
    @Override
    @FFDCIgnore(java.sql.SQLSyntaxErrorException.class)
    public void addByHash(String lookupKey, OAuth20Token entry, int lifetime) {

        boolean error = true;
        boolean isSqlException = false;
        Connection conn = null;
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
        try {
            conn = getInitializedConnection();
            add(conn, entry, lookupKey, expires, scopes, true, false);
            error = false;
        } catch (java.sql.SQLSyntaxErrorException sqle) {
            isSqlException = true;
            if (!columnAdded) {
                try {
                    // Try adding the EXTENDEDFIELDS column to the table
                    new DynamicDBMigrator().addColumnToTable(conn,
                            tableName,
                            TableConstants.COL_OC2_EXTENDEDFIELDS,
                            "CLOB");
                    // columnAdded = true;
                    add(conn, entry, lookupKey, expires, scopes, true, false);
                    error = false;
                } catch (Exception e) {
                    Tr.error(tc, "Internal error adding token: " + e.getMessage(), e);
                }
            } else {
                Tr.error(tc, "Internal error adding token: " + sqle.getMessage(), sqle);
            }
        } catch (Exception e) {
            Tr.error(tc, "Internal error adding token: " + e.getMessage(), e);
        } finally {
            if (isSqlException && !columnAdded) {
                columnAdded = true; // only attempt once
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "add , columnAdded is true");
                }
            }

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
                    Tr.debug(tc, "  grantType: " + entry.getGrantType());
                    Tr.debug(tc, "  extensionProperties: " + entry.getExtensionProperties());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<OAuth20Token> getAll() {
        Collection<OAuth20Token> results = new ArrayList<OAuth20Token>();
        Connection conn = null;
        boolean error = true;
        boolean isSqlException = false;
        ResultSet queryResults = null;

        // Don't use the cache. This is an admin/test command

        try {
            conn = getInitializedConnection();
            getAll(conn, results, queryResults);
            error = false;
        } catch (SQLException sqle) {
            isSqlException = true;
            if (!columnAdded) {
                try {
                    // Try adding the EXTENDEDFIELDS column to the table
                    new DynamicDBMigrator().addColumnToTable(conn,
                            tableName,
                            TableConstants.COL_OC2_EXTENDEDFIELDS,
                            "CLOB");
                    // columnAdded = true;
                    getAll(conn, results, queryResults);
                    error = false;
                } catch (Exception e) {
                    Tr.error(tc, "Internal error getting all tokens: " + e.getMessage(), e);
                }
            } else {
                Tr.error(tc, "Internal error getting all token : " + sqle.getMessage(), sqle);
            }
        } catch (Exception e) {
            Tr.error(tc, "Internal error getting all tokens: " + e.getMessage(), e);
        } finally {
            if (isSqlException && !columnAdded) {
                columnAdded = true; // only attempt once
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "getAll , columnAdded is true");
                }
            }

            closeResultSet(queryResults);
            closeConnection(conn, error);
        }
        return results;
    }

    /**
     * Get all the tokens in the DB cache
     *
     * @param conn
     * @param results
     * @param queryResults
     * @throws SQLException
     */
    private void getAll(Connection conn,
            Collection<OAuth20Token> results,
            ResultSet queryResults) throws SQLException {
        PreparedStatement st = null;
        try {
            String query = "SELECT * FROM " + tableName + " WHERE "
                    + TableConstants.COL_OC2_COMPONENTID + " =? ";
            st = conn.prepareStatement(query);
            st.setString(1, componentId);
            queryResults = st.executeQuery();

            while (queryResults != null && queryResults.next()) {
                OAuth20Token result = createToken(queryResults);
                results.add(result);
            }
        } finally {
            closeStatement(st);
            closeResultSet(queryResults);
        }
    }

    protected OAuth20Token createToken(ResultSet queryResults)
            throws SQLException {
        OAuth20Token result = null;

        String uniqueId = queryResults.getString(TableConstants.COL_OC2_UNIQUEID);
        String componentId = queryResults.getString(TableConstants.COL_OC2_COMPONENTID);
        String type = queryResults.getString(TableConstants.COL_OC2_TYPE);
        String subType = queryResults.getString(TableConstants.COL_OC2_SUBTYPE);
        long createdAt = queryResults.getLong(TableConstants.COL_OC2_CREATEDAT);
        int lifetime = queryResults.getInt(TableConstants.COL_OC2_LIFETIME);
        long expires = queryResults.getLong(TableConstants.COL_OC2_EXPIRES);
        String tokenString = queryResults.getString(TableConstants.COL_OC2_TOKENSTRING);

        String clientId = queryResults.getString(TableConstants.COL_OC2_CLIENTID);
        String username = queryResults.getString(TableConstants.COL_OC2_USERNAME);
        String scopeStr = queryResults.getString(TableConstants.COL_OC2_SCOPE);
        String redirectUri = queryResults.getString(TableConstants.COL_OC2_REDIRECTURI);
        String stateId = queryResults.getString(TableConstants.COL_OC2_STATEID);
        JsonObject extendedFields = null;
        if (getDBType().isClobSupported()) {
            Clob clob = queryResults.getClob(TableConstants.COL_OC2_EXTENDEDFIELDS);
            extendedFields = (new JsonParser()).parse(clob.getCharacterStream()).getAsJsonObject();
        } else {
            String strClob = queryResults.getString(TableConstants.COL_OC2_EXTENDEDFIELDS);
            extendedFields = (new JsonParser()).parse(strClob).getAsJsonObject();
        }

        String grantType = null;
        String refreshId = null, accessId = null;

        if (extendedFields != null) {
            grantType = extendedFields.get(OAuth20Constants.GRANT_TYPE).getAsString();
            if (OAuth20Constants.TOKENTYPE_ACCESS_TOKEN.equals(type)) {
                if (extendedFields.get(OAuth20Constants.REFRESH_TOKEN_ID) != null) {
                    refreshId = extendedFields.get(OAuth20Constants.REFRESH_TOKEN_ID).getAsString();
                }
            } else if (OIDCConstants.TOKENTYPE_ID_TOKEN.equals(type)) {
                accessId = extendedFields.get(OAuth20Constants.ACCESS_TOKEN_ID).getAsString();
            }
            extendedFields.remove(OAuth20Constants.GRANT_TYPE);
            extendedFields.remove(OAuth20Constants.REFRESH_TOKEN_ID);
            extendedFields.remove(OAuth20Constants.ACCESS_TOKEN_ID);
        }
        boolean isAppPasswordOrAppTokenGT = (OAuth20Constants.GRANT_TYPE_APP_PASSWORD.equals(grantType)) || (OAuth20Constants.GRANT_TYPE_APP_TOKEN.equals(grantType));
        boolean isAuthorizationGrantTypeAndCodeSubType = (OAuth20Constants.TOKENTYPE_AUTHORIZATION_GRANT.equals(type) && OAuth20Constants.GRANT_TYPE_AUTHORIZATION_CODE.equals(subType));
        if ((isAuthorizationGrantTypeAndCodeSubType) || (!isAppPasswordOrAppTokenGT && (OAuth20Constants.PLAIN_ENCODING.equals(this.accessTokenEncoding)))) {
            // decode token string
            tokenString = PasswordUtil.passwordDecode(tokenString);
        }

        Map<String, String[]> extensionProperties = JSONUtil.jsonObjectToStringsMap(extendedFields);

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
                    scopes, redirectUri, stateId, extensionProperties, grantType);
            if (refreshId != null) {
                ((OAuth20TokenImpl) result).setRefreshTokenKey(refreshId);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Got the Access Token from cache, refresh token id = " + refreshId);
                }
            } else if (accessId != null) {
                ((OAuth20TokenImpl) result).setAccessTokenKey(accessId);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Got the ID Token from cache, access token id = " + accessId);
                }
            }
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Internal error ceating token :" + "The OAuth20Token is expired already");
            // try {
            // throw new Exception("The OAuth20Token is expired already");
            // } catch (Exception e) {
            // if (tc.isDebugEnabled())
            // Tr.debug(tc, "Internal error ceating token :" + e.getMessage(), e);
            // }
        }
        if (result != null) {
            result.setLastAccess();
        }
        return result;
    }

    @SuppressWarnings("unused")
    private void dumpTokens(Connection conn) {
        ResultSet queryResults = null;
        PreparedStatement st = null;
        try {
            String query = "SELECT * FROM " + tableName;
            st = conn.prepareStatement(query);
            queryResults = st.executeQuery();
            while (queryResults != null && queryResults.next()) {
                String type = queryResults.getString(TableConstants.COL_OC2_TYPE);
                String subType = queryResults.getString(TableConstants.COL_OC2_SUBTYPE);
                long expires = queryResults.getLong(TableConstants.COL_OC2_EXPIRES);
                String tokenString = queryResults.getString(TableConstants.COL_OC2_TOKENSTRING);
                // double-check it's not expired
                Date now = new Date();
                boolean expired = now.getTime() >= expires;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "token: " + tokenString +
                            " type: " + type +
                            " subtype: " + subType +
                            " expires: " + expires +
                            " expired: " + expired);
                }
            }
        } catch (Exception e) {
            // log but don't fail
            Tr.error(tc, "internal error dumping tokens: " + e.getMessage(), e);
        } finally {
            closeResultSet(queryResults);
            closeStatement(st);
        }
    }

    private void startCleanupThread() {
        synchronized (CachedDBOidcTokenStore.class) {
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
        protected CachedDBOidcTokenStore _me;
        protected DBType databaseType = null;
        private boolean stopped = false;

        public CleanupThread(CachedDBOidcTokenStore me) {
            _me = me;
        }

        @Override
        @FFDCIgnore({ com.ibm.ws.security.oauth20.exception.OAuthDataException.class, InterruptedException.class })
        public void run() {
            // Nothing to do for cache, expiry is checked on cache load
            // Determine the database type, to see if it supports LIMIT
            Connection connCheck = null;
            try {
                connCheck = _me.getDBConnection();
                databaseType = DetectDatabaseType.DetectionUtils.detectDbType(connCheck);
            } catch (OAuthDataException e) {
                Tr.error(tc, "Internal error getting DB connection: " + e.getMessage(), e);
                return;
            } finally {
                try {
                    if (connCheck != null && !connCheck.isClosed())
                        connCheck.close();
                } catch (SQLException e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Unable to close connection.");
                    }
                }
            }
            while (!stopped) {
                try {
                    sleep(_me.cleanupInterval * 1000);
                } catch (InterruptedException e) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Cleanup thread was interrupted");
                    }
                }
                if (_me.cleanupThreadExitSemaphore == true) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "token store cleanup thread exiting because exit semaphore is true");
                    }
                    break;
                }
                runCleanup();
            }
        }

        // Yes this is a hack until we have more time to do it right with
        // a thread pool
        public void stopCleanup() {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "stopping cleanup thread");
            }
            stopped = true;
        }

        protected void runCleanup() {
            Date now = new Date();
            long nowTime = now.getTime();
            // Get the number of expired
            int expiredCount = getExpiredCount(nowTime);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "About to delete all tokens with expiry <= " + nowTime);
                Tr.debug(tc, "Number of expired tokens in the DB: " + expiredCount);
            }
            if (expiredCount > 10000) {
                Tr.warning(tc, "OAUTH_PROVIDER_DB_TOOMANY_EXPTOKEN", new Object[] { expiredCount, this._me.cleanupInterval, "cleanupExpiredTokenInterval" });
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
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Running cleanup with LIMIT");
                        }
                        delete = "DELETE FROM " + tableName + " WHERE "
                                + TableConstants.COL_OC2_EXPIRES + " > 0 AND "
                                + TableConstants.COL_OC2_EXPIRES + " <= ? "
                                + "LIMIT " + cleanupBatchSize;
                        numDeleted += cleanupBatchSize;
                    } else {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Running cleanup without LIMIT");
                        }
                        delete = "DELETE FROM " + tableName + " WHERE "
                                + TableConstants.COL_OC2_EXPIRES + " > 0 AND "
                                + TableConstants.COL_OC2_EXPIRES + " <= ?";
                        numDeleted += expiredCount;
                    }
                    st = conn.prepareStatement(delete);
                    st.setLong(1, nowTime);
                    st.execute();
                } catch (SQLException syne) {
                    // DB LIMIT support may have been misdetected, try disabling
                    databaseType = DBType.UNKNOWN;
                    if (tc.isWarningEnabled()) {
                        Tr.warning(tc, "Internal error running cleanup: " + syne.getMessage(), syne);
                    }
                    if (tc.isErrorEnabled()) {
                        Tr.error(tc, "SQL error, switching off LIMIT");
                    }
                } catch (Exception e) {
                    // log but don't fail
                    Tr.error(tc, "Internal error running cleanup: " + e.getMessage(), e);
                    error = true;
                } finally {
                    closeStatement(st);
                    closeConnection(conn, error);
                }
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
            String query = "SELECT COUNT(*) AS \"TOTAL\" FROM " + tableName
                    + " WHERE " + TableConstants.COL_OC2_EXPIRES + " > 0 AND "
                    + TableConstants.COL_OC2_EXPIRES + " <= ? ";
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

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumTokens(String username, String client) {
        int result = -1;
        ResultSet queryResults = null;
        PreparedStatement st = null;

        Connection conn = null;
        boolean isSqlException = false;
        try {
            conn = getInitializedConnection();
            result = getNumTokens(conn, username, client, st, queryResults);
        } catch (SQLException sqle) {
            isSqlException = true;
            if (!columnAdded) {
                try {
                    // Try adding the EXTENDEDFIELDS column to the table
                    new DynamicDBMigrator().addColumnToTable(conn,
                            tableName,
                            TableConstants.COL_OC2_EXTENDEDFIELDS,
                            "CLOB");
                    // columnAdded = true;
                    result = getNumTokens(conn, username, client, st, queryResults);
                } catch (Exception e) {
                    Tr.error(tc, "Internal SQL error while getting number of tokens: " + e.getMessage(), e);
                }
            } else {
                Tr.error(tc, "Internal SQL error while getting number of tokens: " + sqle.getMessage(), sqle);
            }
        } catch (OAuthDataException e) {
            Tr.error(tc, "Internal OAuth error while getting number of tokens: " + e.getMessage(), e);
        } finally {
            if (isSqlException && !columnAdded) {
                columnAdded = true; // only attempt once
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "getNumTokens , columnAdded is true");
                }
            }

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

    /**
     * Get the number of token in the DB cache for the given username and client
     *
     * @param conn
     * @param username
     * @param client
     * @param st
     * @param queryResults
     * @return
     * @throws SQLException
     */
    private int getNumTokens(Connection conn,
            String username,
            String client,
            PreparedStatement st,
            ResultSet queryResults) throws SQLException {
        int result = -1;
        try {
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
                if (tc.isEventEnabled()) {
                    Tr.event(tc, "Updated result to: " + result);
                }
            }
        } finally {
            closeResultSet(queryResults);
            closeStatement(st);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<OAuth20Token> getAllUserTokens(String username) {
        Collection<OAuth20Token> results = new ArrayList<OAuth20Token>();
        Connection conn = null;
        boolean error = true;
        boolean isSqlException = false;
        ResultSet queryResults = null;

        // No DB cache index on username, just load from DB

        try {
            conn = getInitializedConnection();
            getAllUserTokens(conn, username, queryResults, results);
            error = false;
        } catch (SQLException sqle) {
            isSqlException = true;
            if (!columnAdded) {
                try {
                    // Try adding the EXTENDEDFIELDS column to the table
                    new DynamicDBMigrator().addColumnToTable(conn,
                            tableName,
                            TableConstants.COL_OC2_EXTENDEDFIELDS,
                            "CLOB");
                    // columnAdded = true;
                    getAllUserTokens(conn, username, queryResults, results);
                    error = false;
                } catch (Exception e) {
                    Tr.error(tc, "Internal error while getting all user tokens: " + e.getMessage(), e);
                }
            } else {
                Tr.error(tc, "Internal SQL error while getting all user tokens: " + sqle.getMessage(), sqle);
            }
        } catch (Exception e) {
            // log but don't fail
            Tr.error(tc, "Internal error getting all user tokens: " + e.getMessage(), e);
            error = true;
        } finally {
            if (isSqlException && !columnAdded) {
                columnAdded = true; // only attempt once
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "getAllUserTokens , columnAdded is true");
                }
            }

            closeResultSet(queryResults);
            closeConnection(conn, error);
        }
        return results;
    }

    /**
     * Get all tokens in the DB cache for the given username
     *
     * @param conn
     * @param username
     * @param queryResults
     * @return
     * @throws SQLException
     */
    private void getAllUserTokens(Connection conn,
            String username,
            ResultSet queryResults,
            Collection<OAuth20Token> results) throws SQLException {
        PreparedStatement st = null;
        try {
            String query = "SELECT * FROM " + tableName + " WHERE "
                    + TableConstants.COL_OC2_COMPONENTID + " =? AND "
                    + TableConstants.COL_OC2_USERNAME + " =?";
            st = conn.prepareStatement(query);
            st.setString(1, componentId);
            st.setString(2, username);
            queryResults = st.executeQuery();
            while (queryResults != null && queryResults.next()) {
                OAuth20Token result = createToken(queryResults);
                if (result != null) {
                    results.add(result);
                }
            }
        } finally {
            closeResultSet(queryResults);
            closeStatement(st);
        }
    }

    /**
     * Get all tokens in the DB cache for the given username and client and stateId
     *
     * @param conn
     * @param username
     * @param clientId
     * @param stateId
     * @param queryResults
     * @return
     * @throws SQLException
     */
    private void getMatchingTokens(Connection conn,
            String username,
            String clientId,
            String stateId,
            ResultSet queryResults,
            Collection<OAuth20Token> results) throws SQLException {
        PreparedStatement st = null;
        try {
            String query = "SELECT * FROM " + tableName + " WHERE "
                    + TableConstants.COL_OC2_COMPONENTID + " =? AND "
                    + TableConstants.COL_OC2_CLIENTID + " =? AND "
                    + TableConstants.COL_OC2_USERNAME + " =? AND "
                    + TableConstants.COL_OC2_STATEID + " =? ";
            st = conn.prepareStatement(query);
            st.setString(1, componentId);
            st.setString(2, clientId);
            st.setString(3, username);
            st.setString(4, stateId);
            queryResults = st.executeQuery();
            while (queryResults != null && queryResults.next()) {
                OAuth20Token result = createToken(queryResults);
                if (result != null) {
                    results.add(result);
                }
            }
        } finally {
            closeResultSet(queryResults);
            closeStatement(st);
        }
    }

    /**
     * Get all tokens in the DB cache for the given username and client
     *
     * @param conn
     * @param username
     * @param clientId
     * @param queryResults
     * @return
     * @throws SQLException
     */
    private void getMatchingTokens(Connection conn,
            String username,
            String clientId,
            ResultSet queryResults,
            Collection<OAuth20Token> results) throws SQLException {
        PreparedStatement st = null;
        try {
            String query = "SELECT * FROM " + tableName + " WHERE "
                    + TableConstants.COL_OC2_COMPONENTID + " =? AND "
                    + TableConstants.COL_OC2_CLIENTID + " =? AND "
                    + TableConstants.COL_OC2_USERNAME + " =? ";

            st = conn.prepareStatement(query);
            st.setString(1, componentId);
            st.setString(2, clientId);
            st.setString(3, username);

            queryResults = st.executeQuery();
            while (queryResults != null && queryResults.next()) {
                OAuth20Token result = createToken(queryResults);
                if (result != null) {
                    results.add(result);
                }
            }
        } finally {
            closeResultSet(queryResults);
            closeStatement(st);
        }
    }

    protected String getCacheKey(String lookupKey) {
        return lookupKey + "_" + componentId;
    }

    @Override
    public OAuth20Token get(String lookupKey) {
        String hash = lookupKey;// MessageDigestUtil.getDigest(lookupKey);
        if (!PasswordUtil.isHashed(hash)) {
            if (!OAuth20Constants.PLAIN_ENCODING.equals(this.accessTokenEncoding) || (lookupKey.length() == (this.accessTokenLength + 2))) { // app-password or app-token
                if (OAuth20Constants.PLAIN_ENCODING.equals(this.accessTokenEncoding)) { // must be app-password or app-token
                    hash = EndpointUtils.computeTokenHash(lookupKey);
                } else {
                    hash = EndpointUtils.computeTokenHash(lookupKey, this.accessTokenEncoding);
                }
            } else {
                hash = MessageDigestUtil.getDigest(lookupKey);
            }
        }

        return getByHash(hash);
    }

    @Override
    public void remove(String lookupKey) {
        String hash = lookupKey;// MessageDigestUtil.getDigest(lookupKey);
        if (!PasswordUtil.isHashed(hash)) {
            if (!OAuth20Constants.PLAIN_ENCODING.equals(this.accessTokenEncoding) || (lookupKey.length() == (this.accessTokenLength + 2))) { // app-password or app-token
                if (OAuth20Constants.PLAIN_ENCODING.equals(this.accessTokenEncoding)) { // must be app-password or app-token
                    hash = EndpointUtils.computeTokenHash(lookupKey);
                } else {
                    hash = EndpointUtils.computeTokenHash(lookupKey, this.accessTokenEncoding);
                }
            } else {
                hash = MessageDigestUtil.getDigest(lookupKey);
            }
        }
        removeByHash(hash);
    }

    private Connection getInitializedConnection() throws OAuthDataException, SQLException {
        Connection conn = getDBConnection();
        conn.setAutoCommit(false);
        return conn;
    }

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

    /** {@inheritDoc} */
    @Override
    public Collection<OAuth20Token> getMatchingTokens(String username, String client, String tokenType) {
        Collection<OAuth20Token> results = new ArrayList<OAuth20Token>();
        Connection conn = null;
        boolean error = true;
        boolean isSqlException = false;
        ResultSet queryResults = null;

        try {
            conn = getInitializedConnection();
            if (tokenType != null) {
                getMatchingTokens(conn, username, client, tokenType, queryResults, results);
            } else {
                getMatchingTokens(conn, username, client, queryResults, results);
            }
            error = false;
        } catch (SQLException sqle) {
            isSqlException = true;
            if (!columnAdded) {
                try {
                    // Try adding the EXTENDEDFIELDS column to the table
                    new DynamicDBMigrator().addColumnToTable(conn,
                            tableName,
                            TableConstants.COL_OC2_EXTENDEDFIELDS,
                            "CLOB");
                    // columnAdded = true;
                    if (tokenType != null) {
                        getMatchingTokens(conn, username, client, tokenType, queryResults, results);
                    } else {
                        getMatchingTokens(conn, username, client, queryResults, results);
                    }
                    error = false;
                } catch (Exception e) {
                    Tr.error(tc, "Internal error while getting all matching tokens: " + e.getMessage(), e);
                }
            } else {
                Tr.error(tc, "Internal SQL error while getting all matching tokens: " + sqle.getMessage(), sqle);
            }
        } catch (Exception e) {
            // log but don't fail
            Tr.error(tc, "Internal error getting all matching tokens: " + e.getMessage(), e);
            error = true;
        } finally {
            if (isSqlException && !columnAdded) {
                columnAdded = true; // only attempt once
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "getMatchingTokens , columnAdded is true");
                }
            }

            closeResultSet(queryResults);
            closeConnection(conn, error);
        }

        return results;

    }

    /** {@inheritDoc} */
    @Override
    public Collection<OAuth20Token> getUserAndClientTokens(String username, String client) {

        return getMatchingTokens(username, client, null);

    }
}
