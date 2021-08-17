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

package org.apache.wss4j.common.cache;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;

import org.apache.wss4j.common.ext.WSSecurityException;
import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.CachePersistenceException;
import org.ehcache.PersistentCacheManager;
import org.ehcache.Status;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.config.units.MemoryUnit;

/**
 * An in-memory EHCache implementation of the ReplayCache interface, that overflows to disk.
 * The default TTL is 60 minutes and the max TTL is 12 hours.
 */
public class EHCacheReplayCache implements ReplayCache {

    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(EHCacheReplayCache.class);

    private  Cache<String, EHCacheValue> cache; //@AV999
    private  CacheManager cacheManager; //@AV999
    private final String key;
    private final Path diskstorePath;
    private final boolean persistent;

    public EHCacheReplayCache(String key) throws WSSecurityException {
        this(key, null);
    }

    public EHCacheReplayCache(String key, Path diskstorePath) throws WSSecurityException {
        //this(key, diskstorePath, 50, 10000, false);
        this(key, diskstorePath, 5, 10000, false); //@AV999
    }

    public EHCacheReplayCache(String key, Path diskstorePath, long diskSize, long heapEntries, boolean persistent)
            throws WSSecurityException {
        this.key = key;
        this.diskstorePath = diskstorePath;
        this.persistent = persistent;

        // Do some sanity checking on the arguments
        if (key == null || persistent && diskstorePath == null) {
            throw new NullPointerException();
        }
        if (diskstorePath != null && (diskSize < 5 || diskSize > 10000)) {
            throw new IllegalArgumentException("The diskSize parameter must be between 5 and 10000 (megabytes)");
        }
        if (heapEntries < 100) {
            throw new IllegalArgumentException("The heapEntries parameter must be greater than 100 (entries)");
        }

        try {
            ResourcePoolsBuilder resourcePoolsBuilder = ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .heap(heapEntries, EntryUnit.ENTRIES);
            if (diskstorePath != null) {
                LOG.warn("@AV999 , 0, configuring EHCacheReplayCache, disk size = ", diskSize); //@AV999
                resourcePoolsBuilder = resourcePoolsBuilder.disk(diskSize, MemoryUnit.MB, persistent);
            }

            CacheConfigurationBuilder<String, EHCacheValue> configurationBuilder =
                    CacheConfigurationBuilder.newCacheConfigurationBuilder(
                            String.class, EHCacheValue.class, resourcePoolsBuilder)
                            .withExpiry(new EHCacheExpiry());

            if (diskstorePath != null) {
                cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                        .with(CacheManagerBuilder.persistence(diskstorePath.toFile()))
                        .withCache(key, configurationBuilder)
                        .build();
            } else {
                cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                        .withCache(key, configurationBuilder)
                        .build();
            }
            cacheManager.init();
            cache = cacheManager.getCache(key, String.class, EHCacheValue.class);
        } catch (Exception ex) {
            LOG.error("@AV999 , 1, Error configuring EHCacheReplayCache", ex.getMessage());
            //@AV999
            try {
                ResourcePoolsBuilder resourcePoolsBuilder = ResourcePoolsBuilder.newResourcePoolsBuilder()
                                .heap(heapEntries, EntryUnit.ENTRIES);
                CacheConfigurationBuilder<String, EHCacheValue> configurationBuilder = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                        String.class, EHCacheValue.class, resourcePoolsBuilder)
                                        .withExpiry(new EHCacheExpiry());
                cacheManager = CacheManagerBuilder.newCacheManagerBuilder()
                                .withCache(key, configurationBuilder)
                                .build();
                cacheManager.init();
                cache = cacheManager.getCache(key, String.class, EHCacheValue.class);
                
            } catch (Exception ex2) {
                LOG.error("@AV999 , 2, Error configuring EHCacheReplayCache", ex2.getMessage());
                throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex2, "replayCacheError");  
            }
            //@AV999
            //LOG.error("Error configuring EHCacheReplayCache", ex.getMessage()); //@AV999
            //throw new WSSecurityException(WSSecurityException.ErrorCode.FAILURE, ex, "replayCacheError"); //@AV999
        }
    }

    /**
     * Add the given identifier to the cache. It will be cached for a default amount of time.
     * @param identifier The identifier to be added
     */
    public void add(String identifier) {
        add(identifier, null);
    }

    /**
     * Add the given identifier to the cache to be cached for the given time
     * @param identifier The identifier to be added
     * @param expiry A custom expiry time for the identifier. Can be null in which case, the default expiry is used.
     */
    public void add(String identifier, Instant expiry) {
        if (identifier == null || "".equals(identifier)) {
            return;
        }

        cache.put(identifier, new EHCacheValue(identifier, expiry));
    }

    /**
     * Return true if the given identifier is contained in the cache
     * @param identifier The identifier to check
     */
    public boolean contains(String identifier) {
        if (cache == null) {
            return false;
        }
        EHCacheValue element = cache.get(identifier);
        return element != null;
    }

    // Only exposed for testing
    EHCacheValue get(String identifier) {
        return cache.get(identifier);
    }

    @Override
    public synchronized void close() {
        if (cacheManager.getStatus() == Status.AVAILABLE) {
            cacheManager.removeCache(key);

            cacheManager.close();

            if (!persistent && cacheManager instanceof PersistentCacheManager) {
                try {
                    ((PersistentCacheManager) cacheManager).destroy();
                } catch (CachePersistenceException e) {
                    LOG.debug("Error in shutting down persistent cache", e);
                }

                // As we're not using a persistent disk store, just delete it - it should be empty after calling
                // destroy above
                if (diskstorePath != null) {
                    File file = diskstorePath.toFile();
                    if (file.exists() && file.canWrite()) {
                        file.delete();
                    }
                }
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
