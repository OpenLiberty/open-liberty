/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal.cache;

import java.util.HashSet;
import java.util.Set;

import javax.cache.Cache;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.security.authentication.cache.CacheObject;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

import io.openliberty.jcache.CacheService;
import io.openliberty.jcache.DeserializationException;
import io.openliberty.jcache.SerializationException;

/**
 * JCache-backed key-value authentication cache that can be used to provide distributed caching.
 */
public class JCacheAuthCache implements AuthCache {

    private static final TraceComponent tc = Tr.register(JCacheAuthCache.class);

    /**
     * The {@link CacheService} being used, if one is provided.
     */
    private CacheService cacheService = null;

    private AuthCache inMemoryCache = null;

    private static final Set<String> NOT_SERIALIZABLE_CREDS = new HashSet<String>();

    static {
        /*
         * Configure the set of common private credentials that we know are not Serializable.
         */
        NOT_SERIALIZABLE_CREDS.add("sun.security.jgss.GSSCredentialImpl"); // SPNEGO
        NOT_SERIALIZABLE_CREDS.add("com.ibm.security.jgss.GSSCredentialImpl"); // SPNEGO
        NOT_SERIALIZABLE_CREDS.add("com.sun.security.jgss.ExtendedGSSCredentialImpl"); // SPNEGO
        NOT_SERIALIZABLE_CREDS.add("com.ibm.ws.security.jwt.internal.JwtTokenConsumerImpl"); // OIDC (oidcLogin)
        NOT_SERIALIZABLE_CREDS.add("com.ibm.ws.security.openidconnect.client.jose4j.OidcTokenImpl"); // OIDC (openidConnectClient)
    }

    /**
     * Instantiate a new {@link JCacheAuthCache} instance.
     *
     * @param cacheService  The {@link CacheService} to use to connect to the backing JCache implementation.
     * @param inMemoryCache An in-memory cache to store objects that cannot be stored in the JCache.
     */
    public JCacheAuthCache(CacheService cacheService, AuthCache inMemoryCache) {
        /*
         * Eviction, map sizing, etc are all handled by the JCache implementation.
         */
        this.cacheService = cacheService;
        this.inMemoryCache = inMemoryCache;
    }

    @Override
    public void clearAllEntries() {
        /*
         * Clear the in-memory cache.
         */
        inMemoryCache.clearAllEntries();

        /*
         * Don't clear the JCache if we are stopping or we have not finished starting.
         */
        if (!FrameworkState.isStopping() && AuthCacheServiceImpl.isServerStarted()) {
            Cache<Object, Object> jCache = getJCache();
            if (jCache != null) {
                jCache.removeAll(); // Notifies listeners, clear() does not.
            }

            Tr.info(tc, "JCACHE_AUTH_CACHE_CLEARED_ALL_ENTRIES", cacheService.getCache().getName());
        }
    }

    @Override
    public Object get(Object key) {
        Object value = null;

        /*
         * First check the JCache.
         */
        Cache<Object, Object> jCache = getJCache();
        if (jCache != null) {

            /*
             * Search the JCache for the entry.
             */
            try {
                value = jCache.get(key);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    if (value == null) {
                        Tr.debug(tc, "JCache MISS for key " + key);
                    } else {
                        Tr.debug(tc, "JCache HIT for key " + key);
                    }
                }
            } catch (DeserializationException e) {
                /*
                 * This should really never happen unless there are multiple server - one
                 * with a Serializable version of a class and another with a non-Serializable
                 * version.
                 */
                Tr.error(tc, e.getMessage());
            }
        }

        /*
         * If we didn't find it in the JCache, check the in-memory cache.
         */
        if (value == null) {
            value = inMemoryCache.get(key);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                if (value == null) {
                    Tr.debug(tc, "In-memory cache MISS for key " + key);
                } else {
                    Tr.debug(tc, "In-memory cache HIT for key " + key);
                }
            }
        }

        return value;
    }

    @Override
    @FFDCIgnore({ SerializationException.class })
    public void insert(Object key, CacheObject value) {

        /*
         * Skip trying to serialize if we know we don't support it.
         */
        boolean forceInMemory = false;
        for (Object cred : value.getSubject().getPrivateCredentials()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Checking private credential: " + cred.getClass());
            }
            if (NOT_SERIALIZABLE_CREDS.contains(cred.getClass().getName())) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Private credential: " + cred.getClass() + " will be stored to the in-memory cache.");
                }
                forceInMemory = true;
                break;
            }
        }

        if (!forceInMemory) {
            try {
                Cache<Object, Object> jCache = getJCache();
                if (jCache != null) {
                    jCache.put(key, value);
                }
            } catch (SerializationException e) {
                /*
                 * If we could not serialize the object, we should store it in the local cache.
                 */
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Failed to insert value for key " + key + " into JCache due to SerializationException. Inserting into in-memory cache instead.", e);
                }
                inMemoryCache.insert(key, value);
            }
        } else {
            /*
             * We didn't attempt to serialize the object. Put it in the local cache.
             */
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Refused to insert value for key " + key + " into JCache due to known limitation. Inserting into in-memory cache instead.");
            }
            inMemoryCache.insert(key, value);
        }
    }

    @Override
    public void remove(Object key) {
        /*
         * Remove from the in-memory cache.
         */
        inMemoryCache.remove(key);

        /*
         * Remove from the JCache.
         */
        Cache<Object, Object> jCache = getJCache();
        if (jCache != null) {
            jCache.remove(key);
        }
    }

    /**
     * Get the JCache {@link Cache} instance that backs this cache. This method
     * will also register the cache eviction listeners with the JCache cache {@link Cache}.
     *
     * @return The JCache {@link Cache} instance that backs this cache, if there is one. Otherwise; null.
     */
    private Cache<Object, Object> getJCache() {
        Cache<Object, Object> jCache = null;
        if (cacheService != null) {
            jCache = cacheService.getCache();
        }

        return jCache;
    }

    @Override
    @Trivial
    public void stopEvictionTask() {
        /*
         * Eviction is handled by the JCache implementation. Stop the eviction task on the in-memory cache.
         */
        if (inMemoryCache != null) {
            inMemoryCache.stopEvictionTask();
        }
    }
}
