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
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.microprofile.config.archaius.composite.CompositeConfig;
import com.ibm.ws.microprofile.config.interfaces.SourcedValue;

@Trivial
public class StampedValue {

    private static final TraceComponent tc = Tr.register(StampedValue.class);

    //this is the actual value
    private final AtomicStampedReference<SourcedValue> stampedValue = new AtomicStampedReference<>(null, -1);
    //the type of the value
    private final Type type;
    //the parent container
    private final TypeContainer parentContainer;
    //the backing composite config
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
    StampedValue(Type type, TypeContainer parentContainer, CompositeConfig config) {
        this.type = type;
        this.parentContainer = parentContainer;
        this.config = config;
    }

    /**
     * Fetch the latest version of the property. If not up to date then resolve to the latest
     * value, inline.
     *
     * @return the latest version of the value, either from the cache or the underlying source
     */
    public SourcedValue getSourced() {
        boolean fromCache = true;
        int cacheVersion = stampedValue.getStamp();
        int latestVersion = parentContainer.getPrimaryVersion();
        String key = parentContainer.getKey();
        SourcedValue sourcedValue = null;
        if (cacheVersion != latestVersion) {
            SourcedValue currentValue = stampedValue.getReference();
            SourcedValue newValue = config.getSourcedValue(type, key);

            if (stampedValue.compareAndSet(currentValue, newValue, cacheVersion, latestVersion)) {
                sourcedValue = newValue;
                fromCache = false;
            }
        }
        if (fromCache) {
            sourcedValue = stampedValue.getReference();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            if (sourcedValue != null) {
                Tr.debug(tc, "get: Key={0}, Value={1}", key, sourcedValue);
            } else {
                Tr.debug(tc, "get: Key={0} not found", key);
            }
        }

        return sourcedValue;
    }

    protected Type getType() {
        return this.type;
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
        StampedValue other = (StampedValue) obj;
        if (type != other.type)
            return false;
        return true;
    }

    @Override
    @Trivial
    public String toString() {
        return stampedValue.getReference() + "[version:" + stampedValue.getStamp() + "]";
    }
}
