/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal.cache;

/**
 * Key-value authentication cache interface.
 */
public interface AuthCache {

    /**
     * Clear all entries in the cache.
     */
    public void clearAllEntries();

    /**
     * Get the value for the specified key from the cache.
     *
     * @param key The key to look for the value for.
     * @return The value mapped to the specified key, or null if no mapping exists.
     */
    public Object get(Object key);

    /**
     * Remove the value for the specified from the cache.
     *
     * @param key The key to remove the value for.
     */
    public void remove(Object key);

    /**
     * Insert the value for the specified key into the cache.
     *
     * @param key   The key to add the value for.
     * @param value The value to map to the specified key.
     */
    public void insert(Object key, Object value);

    /**
     * Stop the eviction task, if any.
     */
    public void stopEvictionTask();
}
