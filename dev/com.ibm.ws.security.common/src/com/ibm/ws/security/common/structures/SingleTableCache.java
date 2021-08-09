/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    protected Map<String, Object> lookupTable;

    public SingleTableCache(long timeoutInMilliSeconds) {
        this(0, timeoutInMilliSeconds);
    }

    public SingleTableCache(int entryLimit, long timeoutInMilliSeconds) {
        if (entryLimit > 0) {
            this.entryLimit = entryLimit;
        }
        lookupTable = Collections.synchronizedMap(new BoundedHashMap(this.entryLimit));

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
    public synchronized Object get(@Sensitive String key) {
        CacheEntry entry = (CacheEntry) lookupTable.get(key);
        if (entry == null) {
            return null;
        }
        if (entry.isExpired(timeoutInMilliSeconds)) {
            remove(key);
            return null;
        }
        return entry.getValue();
    }

    /**
     * Insert the value into the Cache using the specified key.
     */
    public synchronized void put(@Sensitive String key, Object value) {
        CacheEntry entry = new CacheEntry(value);
        lookupTable.put(key, entry);
    }

    /**
     * Implementation of the eviction strategy.
     */
    @Override
    protected synchronized void evictStaleEntries() {
        List<String> keysToRemove = new ArrayList<String>();
        for (Entry<String, Object> entry : lookupTable.entrySet()) {
            String key = entry.getKey();
            Object cacheEntry = entry.getValue();
            if (cacheEntry == null || ((CacheEntry) cacheEntry).isExpired(timeoutInMilliSeconds)) {
                keysToRemove.add(key);
            }
        }
        for (String keyToRemove : keysToRemove) {
            lookupTable.remove(keyToRemove);
        }
    }

}
