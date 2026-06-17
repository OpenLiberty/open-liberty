/*******************************************************************************
 * Copyright (c) 2021, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.webcontainer.security.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.cache.Cache;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.common.encoder.Base64Coder;
import com.ibm.ws.webcontainer.security.LoggedOutCookieCache;
import com.ibm.ws.webcontainer.security.LoggedOutCookieCacheHelper;
import com.ibm.ws.webcontainer.security.TraceConstants;

import io.openliberty.jcache.CacheService;

/**
 * A JCache backed {@link LoggedOutCookieCache} implementation.
 */
public class JCacheLoggedOutCookieCache implements LoggedOutCookieCache {

    private static final TraceComponent tc = Tr.register(JCacheLoggedOutCookieCache.class, "LoggedOutCookieCache", TraceConstants.MESSAGE_BUNDLE);
    private final CacheService cacheService;
    private static final int SHA512_DIGEST_LENGTH = 64;
    private volatile boolean migrationCompleted = false;
    private volatile boolean migrationWarningPrinted = false;
    private volatile boolean migrationErrorPrinted = false;

    public JCacheLoggedOutCookieCache(CacheService cacheService) {
        this.cacheService = cacheService;
    }

    /**
     * Migrate old logged-out tokens (without LOGOUT: prefix) to their hashed versions.
     * This method should be called once during server startup after the JCache is ready.
     *
     * This migration ensures backward compatibility with older versions that stored
     * tokens without hashing. The migrated entries will use the same global expiration
     * policy as all other cache entries, since JCache doesn't support per-entry TTL
     * through the standard API.
     *
     * Once all servers are upgraded, this migration can be removed.
     */
    public void migrateOldTokensToHashedVersions() {
        if (migrationCompleted) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Token migration already completed, skipping.");
            }
            return;
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Starting migration of old logged-out tokens to hashed versions.");
        }

        try {
            Cache<Object, Object> cache = cacheService.getCache();
            if (cache == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "Cache is not available, skipping migration.");
                }
                return;
            }

            // Collect entries to migrate and build the batch update map
            Map<Object, Object> entriesToMigrate = new HashMap<>();

            for (Cache.Entry<Object, Object> entry : cache) {
                try {
                    Object key = entry.getKey();
                    String keyStr = (String) key;

                    // Check if the key is NOT prefixed with "LOGOUT:"
                    if (!(keyStr.startsWith(LoggedOutCookieCacheHelper.LOGOUT_KEY_PREFIX))) {
                        // Process(hash or append prefix) and add to migration map
                        String hashedKey;
                        boolean isJWTLoggedOutToken = Base64Coder.base64DecodeString(keyStr).length <= SHA512_DIGEST_LENGTH;
                        if (isJWTLoggedOutToken) {
                            // For JWT LoggedOutTokens, we are hashing them already with sha512, so we just need to add the prefix
                            hashedKey = LoggedOutCookieCacheHelper.LOGOUT_KEY_PREFIX + keyStr;
                        } else {
                            // Generate the hashed version of the key
                            hashedKey = LoggedOutCookieCacheHelper.generateTokenHashKey(keyStr);
                        }
                        
                        entriesToMigrate.put(hashedKey, "");

                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Migrated old token to hashed version: " + keyStr.substring(0, 20) + "... -> " + hashedKey);
                        }
                        if (!(migrationWarningPrinted)) {
                            // Warn user that older version tokens exist in the JCache instance.
                            Tr.warning(tc, "OLD_LOGOUT_TOKENS_MIGRATED");
                            migrationWarningPrinted = true;
                        }
                    } else {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, "Exiting migration due to hashed token in cache: " + keyStr);
                        }
                        break;
                    }
                } catch (Exception e) {
                    // Log error and continue with next entry
                    if (!(migrationErrorPrinted)) {
                        Tr.error(tc, "JCACHE_MIGRATION_FAILURE", e);
                        migrationErrorPrinted = true;
                    }
                }
            }
            
            // Batch insert all migrated entries
            int migratedCount = entriesToMigrate.size();
            if (!entriesToMigrate.isEmpty()) {
                cache.putAll(entriesToMigrate);
            }

            migrationCompleted = true;

            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "Token migration completed. Migrated: " + migratedCount);
            }

        } catch (Exception e) {
            if (!(migrationErrorPrinted)) {
                Tr.error(tc, "JCACHE_MIGRATION_FAILURE", e);
                migrationErrorPrinted = true;
            }
        }
    }

    @Override
    public boolean contains(String key) {
        /*
         * Default the response to be 'false' in the case there is an issue
         * communicating with the JCache cache. If we defaulted to 'true', it is
         * possible an network outage, or the similar, could result in all cookies being
         * determined to be "logged out".
         */
        boolean contains = false;

        try {
            contains = cacheService.getCache().containsKey(key);

            if (tc.isDebugEnabled()) {
                if (contains) {
                    Tr.debug(tc, "JCache HIT for key " + key);
                } else {
                    Tr.debug(tc, "JCache MISS for key " + key);
                }
            }
        } catch (Exception e) {
            /*
             * Don't let a JCache failure propagate up the call stack. Log it and move on.
             */
            if (tc.isErrorEnabled()) {
                Tr.error(tc, "JCACHE_CONTAINSKEY_FAILURE", e);
            }
        }
        return contains;
    }

    @Override
    public void put(String key, Object value) {
        try {
            cacheService.getCache().put(key, value);
        } catch (Exception e) {
            /*
             * Don't let a JCache failure propagate up the call stack. Log it and move on.
             */
            if (tc.isErrorEnabled()) {
                Tr.error(tc, "JCACHE_PUT_FAILURE", e);
            }
        }
    }
}
