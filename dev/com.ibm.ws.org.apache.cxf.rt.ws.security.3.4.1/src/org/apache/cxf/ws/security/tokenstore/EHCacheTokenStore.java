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

package org.apache.cxf.ws.security.tokenstore;

import java.io.Closeable;
// Liberty Change Start: Imports
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
// Liberty Change End
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier; // Liberty Change

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.util.StringUtils;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.Status;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;

// Liberty Change Start: Imports
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.EntryUnit;
import org.ehcache.expiry.ExpiryPolicy;
// Liberty Change End
import org.ehcache.xml.XmlConfiguration;

/**
 * An in-memory EHCache implementation of the TokenStore interface. The default TTL is 60 minutes
 * and the max TTL is 12 hours.
 */
public class EHCacheTokenStore implements TokenStore, Closeable, BusLifeCycleListener {
    // Liberty Change Start
    protected static Duration timetoidle = null;
    protected static Duration timetolive = null;
    private final Bus bus;
    private Cache<String, SecurityToken> cache;
    private CacheManager cacheManager;
	// Liberty Change End
    private final String key;

    public EHCacheTokenStore(String key, Bus b, URL configFileURL) throws TokenStoreException {
        bus = b;
        if (bus != null) {
            b.getExtension(BusLifeCycleManager.class).registerLifeCycleListener(this);
        }

        this.key = key;
        try {
            XmlConfiguration xmlConfig = new XmlConfiguration(configFileURL); // Liberty Change

            // Exclude the endpoint info bit added in TokenStoreUtils when getting the template name
            String template = key;
            if (template.contains("-")) {
                template = key.substring(0, key.lastIndexOf('-'));
            }

            CacheConfigurationBuilder<String, SecurityToken> configurationBuilder =
                    xmlConfig.newCacheConfigurationBuilderFromTemplate(template,
                            String.class, SecurityToken.class);

            cacheManager = CacheManagerBuilder.newCacheManagerBuilder().withCache(key, configurationBuilder).build();

            cacheManager.init();
            cache = cacheManager.getCache(key, String.class, SecurityToken.class);

        } catch (Exception e) {
            throw new TokenStoreException(e);
        }
    }
    
	
    // Liberty Change Start
    /**
     * @param cachekey
     * @param bus
     * @param oldconfig
     */
    public EHCacheTokenStore(String cachekey, Bus bus, HashMap oldconfig) {
        this.key = cachekey;
        this.bus = bus;
        this.cacheManager = null;
        this.cache = null;
        
        Path diskstorePath = null;
        String path = (String)oldconfig.get("getDiskStorePath");
        if ("java.io.tmpdir".equals(path)) {
            path = path + File.separator
                + bus.getId();
            
        }
        diskstorePath = Paths.get(path);
        
        int diskElements = (int)oldconfig.get("getMaxElementsOnDisk");
        long heapEntries = (long)oldconfig.get("getMaxEntriesLocalHeap");
        
        boolean persistent = (boolean)oldconfig.get("isDiskPersistent");
        boolean eternal = (boolean)oldconfig.get("isEternal");

        ExpiryPolicy<Object, Object> custom_expiry = null;
        if (eternal) {
            custom_expiry = ExpiryPolicy.NO_EXPIRY;
        } else {
            timetoidle = Duration.of(((long)oldconfig.get("getTimeToIdleSeconds")), ChronoUnit.SECONDS);         
            timetolive = Duration.of(((long)oldconfig.get("getTimeToLiveSeconds")), ChronoUnit.SECONDS);
            custom_expiry = new ExpiryPolicy<Object, Object>() {
                
                @Override
                public String toString() {
                  return "Custom Expiry";
                }

                @Override
                public Duration getExpiryForCreation(Object key, Object value) {
                  return timetolive;
                }

                @Override
                public Duration getExpiryForAccess(Object key, Supplier<?> value) {
                  return timetoidle;
                }

                @Override
                public Duration getExpiryForUpdate(Object key, Supplier<?> oldValue, Object newValue) {
                  return null;
                }
              };
        }
        
        ResourcePoolsBuilder resourcePoolsBuilder = null;
        CacheConfigurationBuilder<String, SecurityToken> configurationBuilder = null;
        
        resourcePoolsBuilder = ResourcePoolsBuilder.newResourcePoolsBuilder()
                            .heap(heapEntries, EntryUnit.ENTRIES);
        configurationBuilder = CacheConfigurationBuilder.newCacheConfigurationBuilder(
                                   String.class, SecurityToken.class, resourcePoolsBuilder).withExpiry(custom_expiry);
            
            
        cacheManager = CacheManagerBuilder.newCacheManagerBuilder().withCache(cachekey, configurationBuilder).build();
        cacheManager.init();
        cache = cacheManager.getCache(cachekey, String.class, SecurityToken.class);
                  
    }
	// Liberty Change Start

    public void add(SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(token.getId())) {
            cache.put(token.getId(), token);
        }
    }

    public void add(String identifier, SecurityToken token) {
        if (token != null && !StringUtils.isEmpty(identifier)) {
            cache.put(identifier, token);
        }
    }

    public void remove(String identifier) {
        if (cache != null && !StringUtils.isEmpty(identifier)) {
            cache.remove(identifier);
        }
    }

    @SuppressWarnings("unchecked") // Liberty Change
    public Collection<String> getTokenIdentifiers() {
        if (cache == null) {
            return null;
        }

        // Not very efficient, but we are only using this method for testing
        Set<String> keys = new HashSet<>();
        for (Cache.Entry<String, SecurityToken> entry : cache) {
            keys.add(entry.getKey());
        }

        return keys;
    }

    public SecurityToken getToken(String identifier) {
        if (cache == null) {
            return null;
        }
        return cache.get(identifier);
    }

    public synchronized void close() {
        if (cacheManager.getStatus() == Status.AVAILABLE) {
            cacheManager.removeCache(key);
            cacheManager.close();
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
