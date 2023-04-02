/*******************************************************************************
 * Copyright (c) 2020, 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.structures;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.ibm.websphere.ras.annotation.Sensitive;

public class SingleTableCache extends CommonCache {

    protected Map<Object, CacheValue> lookupTable;

    public SingleTableCache(long timeoutInMilliSeconds) {
        this(0, timeoutInMilliSeconds);
    }

    public SingleTableCache(int entryLimit, long timeoutInMilliSeconds) {
        lookupTable = Collections.synchronizedMap(new BoundedGenericHashMap<Object, CacheValue>(entryLimit));

        rescheduleCleanup(timeoutInMilliSeconds);
    }

    /**
     * Remove an object from the Cache.
     */
    public synchronized void remove(@Sensitive Object key) {
        lookupTable.remove(key);
    }

    /**
     * Find and return the object associated with the specified key.
     */
    public synchronized Object get(@Sensitive Object key) {
        CacheValue cacheValue = lookupTable.get(key);
        if (cacheValue == null) {
            return null;
        }
        if (cacheValue.isExpired(timeoutInMilliSeconds)) {
            remove(key);
            return null;
        }
        return cacheValue.getValue();
    }

    /**
     * Insert the value into the Cache using the specified key.
     */
    public synchronized void put(@Sensitive Object key, Object value) {
        put(key, value, 0);
    }

    /**
     * Insert the value into the Cache using the specified key.
     */
    public synchronized void put(@Sensitive Object key, Object value, long clockSkew) {
        lookupTable.put(key, createCacheValue(value, clockSkew));
    }

    protected CacheValue createCacheValue(Object value, long clockSkew) {
        return new CacheValue(value, clockSkew);
    }

    public int size() {
        return lookupTable.size();
    }

    /**
     * Implementation of the eviction strategy.
     */
    @Override
    protected synchronized void evictStaleEntries() {
        List<Object> keysToRemove = new ArrayList<>();
        for (Entry<Object, CacheValue> entry : lookupTable.entrySet()) {
            Object key = entry.getKey();
            CacheValue cacheValue = entry.getValue();
            if (cacheValue == null || cacheValue.isExpired(timeoutInMilliSeconds)) {
                keysToRemove.add(key);
            }
        }
        for (Object keyToRemove : keysToRemove) {
            remove(keyToRemove);
        }
    }

}
