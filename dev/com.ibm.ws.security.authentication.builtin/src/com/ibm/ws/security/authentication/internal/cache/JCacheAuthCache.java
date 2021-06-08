/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal.cache;

import javax.cache.Cache;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.wsspi.kernel.service.utils.FrameworkState;

import io.openliberty.jcache.JCacheService;

/**
 * JCache-backed key-value authentication cache that can be used to provide distributed caching.
 */
public class JCacheAuthCache implements AuthCache {

    private static final TraceComponent tc = Tr.register(JCacheAuthCache.class);

    /**
     * The {@link JCacheService} being used, if one is provided.
     */
    private JCacheService jCacheService = null;

    private AuthCache inMemoryCache = null; // TODO Need to use this for at minimum serialization failures.

    /**
     * Instantiate a new {@link JCacheAuthCache} instance.
     *
     * @param jCacheService The {@link JCacheService} to use to connect to the backing JCache implementation.
     * @param inMemoryCache An in-memory cache to store objects that cannot be stored in the JCache.
     */
    public JCacheAuthCache(JCacheService jCacheService, AuthCache inMemoryCache) {
        /*
         * Eviction, map sizing, etc are all handled by the JCache implementation.
         */
        this.jCacheService = jCacheService;
        this.inMemoryCache = inMemoryCache;
    }

    @Override
    public void clearAllEntries() {
        inMemoryCache.clearAllEntries();

        /*
         * Don't clear the JCache if we are stopping or we have not finished starting.
         */
        if (!FrameworkState.isStopping() && AuthCacheServiceImpl.isServerStarted()) {
            Cache<Object, Object> jCache = getJCache();
            if (jCache != null) {
                jCache.removeAll(); // Notifies listeners, clear() does not.
            }

            Tr.info(tc, "JCACHE_AUTH_CACHE_CLEARED_ALL_ENTRIES", jCacheService.getCache().getName());
        }
    }

    @Override
    public Object get(Object key) {
        Cache<Object, Object> jCache = getJCache();
        if (jCache != null) {

            /*
             * Search the JCache for the entry.
             */
            Object value = jCache.get(key);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                if (value == null) {
                    Tr.debug(tc, "JCache MISS for key " + key);
                } else {
                    Tr.debug(tc, "JCache HIT for key " + key);
                }
            }

            return value;
        }
        return null;
    }

    @Override
    public void insert(Object key, Object value) {
        Cache<Object, Object> jCache = getJCache();
        if (jCache != null) {
            jCache.put(key, value);
        }
    }

    @Override
    public void remove(Object key) {
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
        if (jCacheService != null) {
            jCache = jCacheService.getCache();
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
