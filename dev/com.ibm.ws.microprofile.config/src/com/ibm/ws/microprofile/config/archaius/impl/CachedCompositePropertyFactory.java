/**
 * Copyright 2015 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
//Based on com.netflix.archaius.DefaultPropertyFactory
package com.ibm.ws.microprofile.config.archaius.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.netflix.archaius.api.Config;
import com.netflix.archaius.api.ConfigListener;
import com.netflix.archaius.property.ListenerManager;

public class CachedCompositePropertyFactory implements ConfigListener {

    /**
     * Config from which properties are retrieved. Config may be a composite.
     */
    private final CompositeConfig config;

    /**
     * Cache of properties so PropertyContainer may be re-used
     */
    private final ConcurrentMap<String, CachedCompositePropertyContainer> cache = new ConcurrentHashMap<String, CachedCompositePropertyContainer>();

    /**
     * Monotonically incrementing version number whenever a change in the Config
     * is identified. This version is used as a global dirty flag indicating that
     * properties should be updated when fetched next.
     */
    private final AtomicInteger version = new AtomicInteger();

    /**
     * Array of all active callbacks. ListenerWrapper#update will be called for any
     * change in config.
     */
    private final ListenerManager listeners = new ListenerManager();

    /**
     * Constructor
     *
     * @param config
     */
    public CachedCompositePropertyFactory(CompositeConfig config) {
        this.config = config;
        this.config.addListener(this);
    }

    /**
     * Gets the cached property container or make a new one, cache it and return it
     *
     * @param propName
     * @return the property's container
     */
    public CachedCompositePropertyContainer getProperty(String propName) {
        CachedCompositePropertyContainer container = cache.get(propName);
        if (container == null) {
            container = new CachedCompositePropertyContainer(propName, config, version, listeners);
            CachedCompositePropertyContainer existing = cache.putIfAbsent(propName, container);
            if (existing != null) {
                return existing;
            }
        }

        return container;
    }

    /** {@inheritDoc} */
    @Override
    public void onConfigAdded(Config config) {
        invalidate();
    }

    /** {@inheritDoc} */
    @Override
    public void onConfigRemoved(Config config) {
        invalidate();
    }

    /** {@inheritDoc} */
    @Override
    public void onConfigUpdated(Config config) {
        invalidate();
    }

    /** {@inheritDoc} */
    @Override
    public void onError(Throwable error, Config config) {
        // TODO
    }

    /**
     * Invalidate all PropertyContainer instance's caches and poke all listeners
     */
    public void invalidate() {
        // Incrementing the version will cause all PropertyContainer instances to invalidate their
        // cache on the next call to get
        version.incrementAndGet();

        // We expect a small set of callbacks and invoke all of them whenever there is any change
        // in the configuration regardless of change. The blanket update is done since we don't track
        // a dependency graph of replacements.
        listeners.updateAll();
    }
}
