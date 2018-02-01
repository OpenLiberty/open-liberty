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
public class TypedProperty {

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
    private final CopyOnWriteArrayList<TypedPropertyValue> typeCache = new CopyOnWriteArrayList<TypedPropertyValue>();

    /**
     * Reference to the externally managed master version used as the dirty flag
     */
    private final AtomicInteger masterVersion;

    public TypedProperty(String key, CompositeConfig config, AtomicInteger version) {
        this.key = key;
        this.config = config;
        this.masterVersion = version;
    }

    /**
     * Add a new property to the end of the array list but first check
     * to see if it already exists.
     *
     * @param newProperty
     * @return
     */
    private TypedPropertyValue add(final TypedPropertyValue newProperty) {
        //this method was all wrong, it never really checked the cache for an existing value
        //so I rewrote it ;-)
        TypedPropertyValue cachedProperty = null;

        for (TypedPropertyValue property : typeCache) {
            if (property.equals(newProperty)) {
                cachedProperty = property;
                break;
            }
        }
        if (cachedProperty == null) {
            typeCache.add(newProperty);
            cachedProperty = newProperty;
        }

        return cachedProperty;
    }

    public TypedPropertyValue asType(final Type type) {
        TypedPropertyValue prop = add(new TypedPropertyValue(type, this, config));
        return prop;
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
     * Get the master version's value
     *
     * @return
     */
    int getMasterVersion() {
        return masterVersion.get();
    }
}
