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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyContainer;
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
public class CachedCompositePropertyContainer implements PropertyContainer {

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
    private final CopyOnWriteArrayList<CachedCompositeProperty<?>> cache = new CopyOnWriteArrayList<CachedCompositeProperty<?>>();

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
    private <T> CachedCompositeProperty<T> add(final CachedCompositeProperty<T> newProperty) {
        //this method was all wrong, it never really checked the cache for an existing value
        //so I rewrote it ;-)
        CachedCompositeProperty<T> cachedProperty = null;

        for (CachedCompositeProperty<?> property : cache) {
            if (property.equals(newProperty)) {
                cachedProperty = (CachedCompositeProperty<T>) property;
                break;
            }
        }
        if (cachedProperty == null) {
            cache.add(newProperty);
            cachedProperty = newProperty;
        }

        return cachedProperty;
    }

    /** {@inheritDoc} */
    @Override
    public Property<String> asString(final String defaultValue) {
        return asType(String.class, defaultValue);
    }

    /** {@inheritDoc} */
    @Override
    public Property<Integer> asInteger(final Integer defaultValue) {
        return asType(Integer.class, defaultValue);
    }

    /** {@inheritDoc} */
    @Override
    public Property<Long> asLong(final Long defaultValue) {
        return asType(Long.class, defaultValue);
    }

    /** {@inheritDoc} */
    @Override
    public Property<Double> asDouble(final Double defaultValue) {
        return asType(Double.class, defaultValue);
    }

    /** {@inheritDoc} */
    @Override
    public Property<Float> asFloat(final Float defaultValue) {
        return asType(Float.class, defaultValue);
    }

    /** {@inheritDoc} */
    @Override
    public Property<Short> asShort(final Short defaultValue) {
        return asType(Short.class, defaultValue);
    }

    /** {@inheritDoc} */
    @Override
    public Property<Byte> asByte(final Byte defaultValue) {
        return asType(Byte.class, defaultValue);
    }

    /** {@inheritDoc} */
    @Override
    public Property<BigDecimal> asBigDecimal(final BigDecimal defaultValue) {
        return asType(BigDecimal.class, defaultValue);
    }

    /** {@inheritDoc} */
    @Override
    public Property<Boolean> asBoolean(final Boolean defaultValue) {
        return asType(Boolean.class, defaultValue);
    }

    /** {@inheritDoc} */
    @Override
    public Property<BigInteger> asBigInteger(final BigInteger defaultValue) {
        return asType(BigInteger.class, defaultValue);
    }

    /** {@inheritDoc} */
    @Override
    public <T> Property<T> asType(final Class<T> type, final T defaultValue) {
        CachedCompositeProperty<T> prop = add(new CachedCompositeProperty<T>(type, defaultValue, this) {
            @Override
            protected T resolveCurrent() throws Exception {
                return config.get(type, key, defaultValue);
            }
        });
        return prop;
    }

    /** {@inheritDoc} */
    @Override
    public <T> Property<T> asType(Function<String, T> type, String defaultValue) {
        //can't implement this at the moment ... and don't need it anyway
        throw new UnsupportedOperationException();
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
