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
//Based on com.netflix.archaius.property.DefaultPropertyContainer

package com.ibm.ws.microprofile.config.archaius.impl;

import java.lang.reflect.Type;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.netflix.archaius.api.PropertyListener;
import com.netflix.archaius.property.ListenerManager;
import com.netflix.archaius.property.ListenerManager.ListenerUpdater;

/**
 * Implementation of PropertyContainer which reuses the same object for each
 * type. This implementation assumes that each fast property is mostly accessed
 * as the same type but allows for additional types to be deserialized.
 * Instead of incurring the overhead for caching in a hash map, the objects are
 * stored in a CopyOnWriteArrayList and items are retrieved via a linear scan.
 *
 * Once created a PropertyContainer property cannot be removed. However, listeners may be
 * added and removed.
 *
 * @author elandau
 *
 */
public class CachedCompositePropertyContainer {

    /**
     * The property name
     */
    private final String key;

    /**
     * Config from which property values are resolved
     */
    private final CompositeConfig config;

    /**
     * Cache for each type attached to this property.
     */
    private final CopyOnWriteArrayList<CachedCompositeProperty> cache = new CopyOnWriteArrayList<CachedCompositeProperty>();

    /**
     * Listeners are tracked globally as an optimization so it is not necessary to iterate through all
     * property containers when the listeners need to be invoked since the expectation is to have far
     * less listeners than property containers.
     */
    private final ListenerManager listeners;

    /**
     * Reference to the externally managed master version used as the dirty flag
     */
    private final AtomicInteger masterVersion;

    public CachedCompositePropertyContainer(String key, CompositeConfig config, AtomicInteger version, ListenerManager listeners) {
        this.key = key;
        this.config = config;
        this.listeners = listeners;
        this.masterVersion = version;
    }

    /**
     * Add a new property to the end of the array list but first check
     * to see if it already exists.
     *
     * @param newProperty
     * @return
     */
    @SuppressWarnings("unchecked")
    private CachedCompositeProperty add(final CachedCompositeProperty newProperty) {
        //this method was all wrong, it never really checked the cache for an existing value
        //so I rewrote it ;-)
        CachedCompositeProperty cachedProperty = null;

        for (CachedCompositeProperty property : cache) {
            if (property.equals(newProperty)) {
                cachedProperty = property;
                break;
            }
        }
        if (cachedProperty == null) {
            cache.add(newProperty);
            cachedProperty = newProperty;
        }

        return cachedProperty;
    }

    public CachedCompositeProperty asType(final Type type) {
        CachedCompositeProperty prop = add(new CachedCompositeProperty(type, this) {
            @Override
            protected CachedCompositeValue resolveCurrent() throws Exception {
                return config.getCompositeValue(type, key);
            }
        });
        return prop;
    }

    /**
     * Add a listener
     *
     * @param listener
     * @param listenerUpdater
     */
    void addListener(PropertyListener<?> listener, ListenerUpdater updater) {
        listeners.add(listener, updater);
    }

    /**
     * Get the key
     *
     * @return
     */
    String getKey() {
        return key;
    }

    /**
     * Remove a listener
     *
     * @param listener
     */
    void removeListener(PropertyListener<?> listener) {
        listeners.remove(listener);
    }

    /**
     * Get the master version's value
     *
     * @return
     */
    int getMasterVersion() {
        return masterVersion.get();
    }
}
