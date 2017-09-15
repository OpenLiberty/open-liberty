/*******************************************************************************
* Copyright (c) 2017 IBM Corporation and others.
*
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
*******************************************************************************
* Copyright 2010-2013 Coda Hale and Yammer, Inc.
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
*******************************************************************************/
package com.ibm.ws.microprofile.metrics.impl;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;

import org.eclipse.microprofile.metrics.MetricRegistry;

/**
 * A map of shared, named metric registries.
 */
public class SharedMetricRegistries {
    private static final ConcurrentMap<String, MetricRegistry> REGISTRIES = new ConcurrentHashMap<String, MetricRegistry>();

    private static AtomicReference<String> defaultRegistryName = new AtomicReference<String>();

    /* Visible for testing */
    static void setDefaultRegistryName(AtomicReference<String> defaultRegistryName) {
        SharedMetricRegistries.defaultRegistryName = defaultRegistryName;
    }

    private SharedMetricRegistries() { /* singleton */ }

    public static void clear() {
        REGISTRIES.clear();
    }

    public static Set<String> names() {
        return REGISTRIES.keySet();
    }

    public static void remove(String key) {
        REGISTRIES.remove(key);
    }

    public static MetricRegistry add(String name, MetricRegistry registry) {
        return REGISTRIES.putIfAbsent(name, registry);
    }

    public static MetricRegistry getOrCreate(String name) {
        final MetricRegistry existing = REGISTRIES.get(name);
        if (existing == null) {
            final MetricRegistry created = new MetricRegistryImpl();
            final MetricRegistry raced = add(name, created);
            if (raced == null) {
                return created;
            }
            return raced;
        }
        return existing;
    }

    /**
     * Creates a new registry and sets it as the default one under the provided name.
     *
     * @param name the registry name
     * @return the default registry
     * @throws IllegalStateException if the name has already been set
     */
    public synchronized static MetricRegistry setDefault(String name) {
        final MetricRegistry registry = getOrCreate(name);
        return setDefault(name, registry);
    }

    /**
     * Sets the provided registry as the default one under the provided name
     *
     * @param name the default registry name
     * @param metricRegistry the default registry
     * @throws IllegalStateException if the default registry has already been set
     */
    public static MetricRegistry setDefault(String name, MetricRegistry metricRegistry) {
        if (defaultRegistryName.compareAndSet(null, name)) {
            add(name, metricRegistry);
            return metricRegistry;
        }
        throw new IllegalStateException("Default metric registry name is already set.");
    }

    /**
     * Gets the name of the default registry, if it has been set
     *
     * @return the default registry
     * @throws IllegalStateException if the default has not been set
     */
    public static MetricRegistry getDefault() {
        MetricRegistry metricRegistry = tryGetDefault();
        if (metricRegistry == null) {
            throw new IllegalStateException("Default registry name has not been set.");
        }
        return metricRegistry;
    }

    /**
     * Same as {@link #getDefault()} except returns null when the default registry has not been set.
     *
     * @return the default registry or null
     */
    public static MetricRegistry tryGetDefault() {
        final String name = defaultRegistryName.get();
        if (name != null) {
            return getOrCreate(name);
        } else {
            return null;
        }
    }
}
