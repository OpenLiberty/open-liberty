/*******************************************************************************
 * Copyright (c) 2021, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.jcache;

import javax.cache.Cache;

/**
 * A interface that defines a JCache service.
 */
public interface CacheService {
    /**
     * Get the JCache {@link Cache} instance that will be used to store objects.
     *
     * @return the JCache {@link Cache} instance.
     */
    public Cache<Object, Object> getCache();

    /**
     * Get the name of the cache to create.
     *
     * @return the name of the cache or null if a cache should not be created.
     */
    public String getCacheName();

    /**
     * Deserialize a serialized object.
     *
     * @param bytes The serialized object.
     * @return The deserialized object.
     *
     * @throws DeserializationException If there was an issue deserializing the object.
     */
    public Object deserialize(byte[] bytes);

    /**
     * Serialize an object.
     *
     * @param o The object to serialize.
     * @return The serialized object.
     *
     * @throws SerializationException If there was an issue serializing the object.
     */
    public byte[] serialize(Object o);
}
