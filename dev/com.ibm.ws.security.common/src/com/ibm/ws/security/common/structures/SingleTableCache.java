/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.common.structures;

import java.util.Collections;
import java.util.Map;

import com.ibm.websphere.ras.annotation.Sensitive;

public class SingleTableCache extends CommonCache {

    private Map<String, Object> lookupTable;

    public SingleTableCache(long timeoutInMilliSeconds) {
        this(0, timeoutInMilliSeconds);
    }

    public SingleTableCache(int entryLimit, long timeoutInMilliSeconds) {
        if (entryLimit > 0) {
            this.entryLimit = entryLimit;
        }
        lookupTable = Collections.synchronizedMap(new BoundedHashMap(this.entryLimit));

        if (timeoutInMilliSeconds > 0) {
            this.timeoutInMilliSeconds = timeoutInMilliSeconds;
        }

        scheduleEvictionTask(this.timeoutInMilliSeconds);
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
        return lookupTable.get(key);
    }

    /**
     * Insert the value into the Cache using the specified key.
     */
    public synchronized void put(@Sensitive String key, Object value) {
        lookupTable.put(key, value);
    }

    /**
     * Implementation of the eviction strategy.
     */
    protected synchronized void evictStaleEntries() {
        lookupTable = Collections.synchronizedMap(new BoundedHashMap(this.entryLimit));
    }

}
