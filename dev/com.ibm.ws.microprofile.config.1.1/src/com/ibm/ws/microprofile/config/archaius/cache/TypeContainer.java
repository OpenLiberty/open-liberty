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

package com.ibm.ws.microprofile.config.archaius.cache;

import java.lang.reflect.Type;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.ws.microprofile.config.archaius.composite.CompositeConfig;

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
public class TypeContainer {

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
    private final CopyOnWriteArrayList<StampedValue> typeCache = new CopyOnWriteArrayList<StampedValue>();

    /**
     * Reference to the externally managed primary version used as the dirty flag
     */
    private final AtomicInteger primaryVersion;

    public TypeContainer(String key, CompositeConfig config, AtomicInteger version) {
        this.key = key;
        this.config = config;
        this.primaryVersion = version;
    }

    /**
     * Check if a StampedValue already exists for the type, if it does, return it,
     * otherwise create a new one and add it
     *
     * @param type
     * @return
     */
    public StampedValue asType(final Type type) {

        StampedValue cachedValue = null;

        //looping around a list is probably quicker than having a map since there are probably only one or two different
        //types in use at any one time
        for (StampedValue value : typeCache) {
            if (value.getType().equals(type)) {
                cachedValue = value;
                break;
            }
        }
        if (cachedValue == null) {
            StampedValue newValue = new StampedValue(type, this, config);
            typeCache.add(newValue);
            cachedValue = newValue;
        }

        return cachedValue;
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
     * Get the primary version's value
     *
     * @return
     */
    int getPrimaryVersion() {
        return primaryVersion.get();
    }
}
