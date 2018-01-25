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
//Based on com.netflix.archaius.property.DefaultPropertyContainer#CachedProperty

package com.ibm.ws.microprofile.config.archaius.impl;

import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicStampedReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.config.interfaces.ConfigException;
import com.netflix.archaius.api.Property;
import com.netflix.archaius.api.PropertyListener;
import com.netflix.archaius.property.ListenerManager.ListenerUpdater;

public abstract class CachedCompositeProperty implements Property<Object> {

    private static final TraceComponent tc = Tr.register(CachedCompositeProperty.class);

    private final AtomicStampedReference<CachedCompositeValue> cache = new AtomicStampedReference<>(null, -1);
    private final Type type;
    private final CachedCompositePropertyContainer parentContainer;

    /**
     * Constructor
     *
     * @param type
     * @param defaultValue
     * @param parentContainer
     */
    CachedCompositeProperty(Type type, CachedCompositePropertyContainer parentContainer) {
        this.type = type;
        this.parentContainer = parentContainer;
    }

    /** {@inheritDoc} */
    @Override
    public void addListener(final PropertyListener<Object> listener) {
        parentContainer.addListener(listener, new ListenerUpdater() {
            private final AtomicReference<Object> last = new AtomicReference<Object>(null);

            @Override
            public void update() {
                final Object prev = last.get();
                final Object value;

                try {
                    value = get();
                } catch (Exception e) {
                    listener.onParseError(e);
                    return;
                }

                if (prev != value) {
                    if (last.compareAndSet(prev, value)) {
                        listener.onChange(value);
                    }
                }
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void removeListener(PropertyListener<Object> listener) {
        parentContainer.removeListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public String getKey() {
        return parentContainer.getKey();
    }

    /**
     * Fetch the latest version of the property. If not up to date then resolve to the latest
     * value, inline.
     *
     * @return the latest version of the value, either from the cache or the underlying source
     */
    public CachedCompositeValue getSourced() {
        boolean fromCache = true;
        int cacheVersion = cache.getStamp();
        int latestVersion = parentContainer.getMasterVersion();
        CachedCompositeValue compositeValue = null;
        if (cacheVersion != latestVersion) {
            CachedCompositeValue currentValue = cache.getReference();
            CachedCompositeValue newValue = null;
            try {
                newValue = resolveCurrent();
            } catch (ConfigException e) {
                throw e;
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "get", "Unable to get current version of property '" + parentContainer.getKey() + "'", e);
                }
                throw new ConfigException(e);
            }

            if (cache.compareAndSet(currentValue, newValue, cacheVersion, latestVersion)) {
                compositeValue = newValue;
                fromCache = false;
            }
        }
        if (fromCache) {
            compositeValue = cache.getReference();
        }

        return compositeValue;
    }

    /**
     * Fetch the latest version of the property. If not up to date then resolve to the latest
     * value, inline.
     *
     * @return the latest version of the value, either from the cache or the underlying source
     */
    @Override
    public Object get() {
        CachedCompositeValue compositeValue = getSourced();

        Object actual = null;
        if (compositeValue != null) {
            actual = compositeValue.getValue();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            if (compositeValue != null) {
                Tr.debug(tc, "get: Key={0}, Value={1}, Source={2}", getKey(), compositeValue.getValue(), compositeValue.getSource());
            } else {
                Tr.debug(tc, "get: Key={0} not found", getKey());
            }
        }
        return actual;
    }

    /**
     * Resolve to the most recent value
     *
     * @return
     * @throws Exception
     */
    protected abstract CachedCompositeValue resolveCurrent() throws Exception;

    /**
     * CachedCompositeProperty is used by CachedCompositePropertyContainer as a tuple of type and value.
     * The container wants a one-to-one mapping from type to value but since there are rarely more than
     * just a couple of types, a Map is too heavy. A List is used instead and the code checks to see if
     * a type already exists using the equals and hashCode methods of CachedCompositeProperty.
     * Therefore, only type is used in the calculation of these methods.
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        //result = prime * result + ((defaultValue == null) ? 0 : defaultValue.hashCode());
        result = prime * result + type.hashCode();
        return result;
    }

    /**
     * @see #hashCode()
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        CachedCompositeProperty other = (CachedCompositeProperty) obj;
        if (type != other.type)
            return false;
        return true;
    }
}
