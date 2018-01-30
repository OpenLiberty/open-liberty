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
package com.ibm.ws.microprofile.config.archaius.cache;

import java.lang.reflect.Type;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.ws.microprofile.config.archaius.composite.CompositeConfig;
import com.ibm.ws.microprofile.config.archaius.composite.ConfigListener;
import com.ibm.ws.microprofile.config.interfaces.SourcedPropertyValue;

public class ConfigCache implements ConfigListener {

    /**
     * Config from which properties are retrieved.
     */
    private final CompositeConfig config;

    /**
     * Cache of Typed Properties
     */
    private final ConcurrentMap<String, TypedProperty> cache = new ConcurrentHashMap<String, TypedProperty>();

    /**
     * Monotonically incrementing version number whenever a change in the Config
     * is identified. This version is used as a global dirty flag indicating that
     * properties should be updated when fetched next.
     */
    private final AtomicInteger version = new AtomicInteger();

    /**
     * Constructor
     *
     * @param config
     */
    public ConfigCache(CompositeConfig config) {
        this.config = config;
        this.config.addListener(this);
    }

    /**
     * Gets the cached property container or make a new one, cache it and return it
     *
     * @param propName
     * @return the property's container
     */
    private TypedProperty getProperty(String propName) {
        TypedProperty container = cache.get(propName);
        if (container == null) {
            container = new TypedProperty(propName, config, version);
            TypedProperty existing = cache.putIfAbsent(propName, container);
            if (existing != null) {
                return existing;
            }
        }

        return container;
    }

    /** {@inheritDoc} */
    @Override
    public void onConfigAdded() {
        invalidate();
    }

    /** {@inheritDoc} */
    @Override
    public void onConfigUpdated() {
        invalidate();
    }

    /**
     * Invalidate all PropertyContainer instance's caches and poke all listeners
     */
    public void invalidate() {
        // Incrementing the version will cause all PropertyContainer instances to invalidate their
        // cache on the next call to get
        version.incrementAndGet();
    }

    /**
     * @param propertyName
     * @param propertyType
     * @return
     */
    public SourcedPropertyValue getSourcedValue(String propertyName, Type propertyType) {
        TypedProperty container = getProperty(propertyName);
        TypedPropertyValue property = container.asType(propertyType);
        SourcedPropertyValue value = property.getSourced();
        return value;
    }

}
