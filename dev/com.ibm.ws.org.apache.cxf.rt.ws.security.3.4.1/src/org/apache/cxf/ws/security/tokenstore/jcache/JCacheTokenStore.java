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

package org.apache.cxf.ws.security.tokenstore.jcache;

import java.io.Closeable;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

import org.apache.cxf.Bus;
import org.apache.cxf.buslifecycle.BusLifeCycleListener;
import org.apache.cxf.buslifecycle.BusLifeCycleManager;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.cxf.ws.security.tokenstore.TokenStoreException;
import org.apache.cxf.ws.security.utils.JCacheSupport;

/**
 * An in-memory JCache implementation of the TokenStore interface. The default TTL is 60 minutes
 * and the max TTL is 12 hours.
 */
public class JCacheTokenStore implements TokenStore, Closeable, BusLifeCycleListener {
    private final Bus bus;
    private final Cache<String, SecurityToken> cache;
    private final CacheManager cacheManager;
    private final String key;

    public JCacheTokenStore(String key, Bus b, URL configFileURL) throws TokenStoreException {
        bus = b;
        if (bus != null) {
            b.getExtension(BusLifeCycleManager.class).registerLifeCycleListener(this);
        }

        this.key = key;
        try {
            // Exclude the endpoint info bit added in TokenStoreUtils when getting the template name
            String template = key;
            if (template.contains("-")) {
                template = key.substring(0, key.lastIndexOf('-'));
            }

            final CachingProvider cachingProvider = Caching.getCachingProvider();
            cacheManager = cachingProvider.getCacheManager(configFileURL.toURI(),
                    SecurityToken.class.getClassLoader()); 
            cache = JCacheSupport.getOrCreate(cacheManager, key, String.class, SecurityToken.class);
        } catch (Exception e) {
            throw new TokenStoreException(e);
        }
    }

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
        if (!cacheManager.isClosed()) {
            cacheManager.destroyCache(key);
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
