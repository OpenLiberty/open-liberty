/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.cxf.ws.security.cache;

import java.io.Closeable;
import java.net.URL;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.ws.security.cache.ReplayCache;

/**
 * An in-memory EHCache implementation of the ReplayCache interface. The default TTL is 60 minutes and the
 * max TTL is 12 hours.
 */
public class EHCacheReplayCache implements ReplayCache, Closeable, BusLifeCycleListener {
    
    public static final long DEFAULT_TTL = 3600L;
    public static final long MAX_TTL = DEFAULT_TTL * 12L;
    private Ehcache cache;
    private CacheManager cacheManager;
    private Bus bus;
    private long ttl = DEFAULT_TTL;
    
    public EHCacheReplayCache(String key, Bus b, URL configFileURL) {
        bus = b;
        if (bus != null) {
            bus.getExtension(BusLifeCycleManager.class).registerLifeCycleListener(this);
        }
        cacheManager = EHCacheManagerHolder.getCacheManager(bus, configFileURL);
        
        CacheConfiguration cc = EHCacheManagerHolder.getCacheConfiguration(key, cacheManager);

        Ehcache newCache = new Cache(cc);
        cache = cacheManager.addCacheIfAbsent(newCache);
        // Set the TimeToLive value from the CacheConfiguration
        ttl = cc.getTimeToLiveSeconds();
    }
    
    /**
     * Set a new (default) TTL value in seconds
     * @param newTtl a new (default) TTL value in seconds
     */
    public void setTTL(long newTtl) {
        ttl = newTtl;
    }    
       
    /**
     * Get the (default) TTL value in seconds
     * @return the (default) TTL value in seconds
     */
    public long getTTL() {
        return ttl;
    }
    
    /**
     * Add the given identifier to the cache. It will be cached for a default amount of time.
     * @param identifier The identifier to be added
     */
    public void add(String identifier) {
        add(identifier, ttl);
    }
    
    /**
     * Add the given identifier to the cache to be cached for the given time
     * @param identifier The identifier to be added
     * @param timeToLive The length of time to cache the Identifier in seconds
     */
    public void add(String identifier, long timeToLive) {
        if (identifier == null || "".equals(identifier)) {
            return;
        }
        
        int parsedTTL = (int)timeToLive;
        if (timeToLive != (long)parsedTTL || parsedTTL < 0 || parsedTTL > MAX_TTL) {
            // Default to configured value
            parsedTTL = (int)ttl;
            if (ttl != (long)parsedTTL) {
                // Fall back to 60 minutes if the default TTL is set incorrectly
                parsedTTL = 3600;
            }
        }
        
        cache.put(new Element(identifier, identifier, false, parsedTTL, parsedTTL));
    }
    
    /**
     * Return true if the given identifier is contained in the cache
     * @param identifier The identifier to check
     */
    public boolean contains(String identifier) {
        Element element = cache.get(identifier);
        if (element != null) {
            if (cache.isExpired(element)) {
                cache.remove(identifier);
                return false;
            }
            return true;
        }
        return false;
    }

    public synchronized void close() {
        if (cacheManager != null) {
            EHCacheManagerHolder.releaseCacheManger(cacheManager);
            cacheManager = null;
            cache = null;
            if (bus != null) {
                bus.getExtension(BusLifeCycleManager.class).unregisterLifeCycleListener(this);
            }
        }
        
    }

    public void initComplete() {
    }
    public void preShutdown() {
        close();
    }
    public void postShutdown() {
        close();
    }
    
}
