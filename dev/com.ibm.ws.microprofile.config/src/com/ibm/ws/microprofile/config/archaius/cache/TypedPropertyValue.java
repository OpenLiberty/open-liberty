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

package com.ibm.ws.microprofile.config.archaius.cache;

import java.lang.reflect.Type;
import java.util.concurrent.atomic.AtomicStampedReference;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.microprofile.config.archaius.composite.CompositeConfig;
import com.ibm.ws.microprofile.config.interfaces.ConfigException;
import com.ibm.ws.microprofile.config.interfaces.SourcedPropertyValue;

public class TypedPropertyValue {

    private static final TraceComponent tc = Tr.register(TypedPropertyValue.class);

    private final AtomicStampedReference<SourcedPropertyValue> stampedValue = new AtomicStampedReference<>(null, -1);
    private final Type type;
    private final TypedProperty parentContainer;

    private final CompositeConfig config;

    /**
     * Constructor
     *
     * @param type
     * @param defaultValue
     * @param parentContainer
     * @param
     * @param config
     */
    TypedPropertyValue(Type type, TypedProperty parentContainer, CompositeConfig config) {
        this.type = type;
        this.parentContainer = parentContainer;
        this.config = config;
    }

    protected SourcedPropertyValue resolveCurrent() {
        return config.getSourcedValue(type, getKey());
    }

    public String getKey() {
        return parentContainer.getKey();
    }

    /**
     * Fetch the latest version of the property. If not up to date then resolve to the latest
     * value, inline.
     *
     * @return the latest version of the value, either from the cache or the underlying source
     */
    public SourcedPropertyValue getSourced() {
        boolean fromCache = true;
        int cacheVersion = stampedValue.getStamp();
        int latestVersion = parentContainer.getMasterVersion();
        SourcedPropertyValue compositeValue = null;
        if (cacheVersion != latestVersion) {
            SourcedPropertyValue currentValue = stampedValue.getReference();
            SourcedPropertyValue newValue = null;
            try {
                newValue = resolveCurrent();
            } catch (ConfigException e) {
                throw e;
            } catch (Exception e) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "get: Unable to get current version of property '{0}'. Exception: {1}", parentContainer.getKey(), e);
                }
                throw new ConfigException(e);
            }

            if (stampedValue.compareAndSet(currentValue, newValue, cacheVersion, latestVersion)) {
                compositeValue = newValue;
                fromCache = false;
            }
        }
        if (fromCache) {
            compositeValue = stampedValue.getReference();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            if (compositeValue != null) {
                Tr.debug(tc, "get: Key={0}, Value={1}, Source={2}", getKey(), compositeValue.getValue(), compositeValue.getSource());
            } else {
                Tr.debug(tc, "get: Key={0} not found", getKey());
            }
        }

        return compositeValue;
    }

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
        TypedPropertyValue other = (TypedPropertyValue) obj;
        if (type != other.type)
            return false;
        return true;
    }
}
