/*******************************************************************************
 * Copyright (c) 1997, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client;

import java.util.Collections;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.structures.BoundedHashMap;

/**
 * Cache containing three internal tables in order to implement a least-recently-used removal algorithm.
 */
public class Cache {

    private static final TraceComponent tc = Tr.register(Cache.class);

    /**
     * Primary hash table containing the most recently used entries.
     */
    private Map<String, Object> primaryTable;

    /**
     * Secondary hash table containing less recently used entries.
     */
    private Map<String, Object> secondaryTable;

    /**
     * Tertiary hash table containing least recently used entries that
     * are eligible for eviction.
     */
    private Map<String, Object> tertiaryTable;

    /**
     * Maximum number of entries allowed in the cache.
     */
    private int entryLimit = 50000;
    /**
     * Default cache timeout.
     */
    private long timeoutInMilliSeconds = 5 * 60 * 1000;

    /**
     * Timer to schedule the eviction task.
     */
    private Timer timer;

    public Cache(int entryLimit, long timeoutInMilliSeconds) {
        if (entryLimit > 0) {
            this.entryLimit = entryLimit;
        }
        primaryTable = Collections.synchronizedMap(new BoundedHashMap(this.entryLimit));
        secondaryTable = Collections.synchronizedMap(new BoundedHashMap(this.entryLimit));
        tertiaryTable = Collections.synchronizedMap(new BoundedHashMap(this.entryLimit));

        if (timeoutInMilliSeconds > 0) {
            this.timeoutInMilliSeconds = timeoutInMilliSeconds;
        }

        scheduleEvictionTask(this.timeoutInMilliSeconds);

    }

    public int size() {
        return this.entryLimit;
    }

    private void scheduleEvictionTask(long timeoutInMilliSeconds) {
        EvictionTask evictionTask = new EvictionTask();
        timer = new Timer(true);
        long period = timeoutInMilliSeconds;
        long delay = period;
        timer.schedule(evictionTask, delay, period);
    }

    /**
     * Remove an object from the Cache.
     */
    public synchronized void remove(Object key) {

        primaryTable.remove(key);
        secondaryTable.remove(key);
        tertiaryTable.remove(key);

    }

    /**
     * Find and return the object associated with the specified key.
     */
    public synchronized Object get(String key) {

        Object curEntry = primaryTable.get(key);

        // Not found in primary
        if (curEntry == null) {
            curEntry = secondaryTable.get(key);

            // Not found in primary or secondary
            if (curEntry == null) {
                curEntry = tertiaryTable.get(key);
            }
        }

        return curEntry;
    }

    /**
     * Insert the value into the Cache using the specified key.
     */
    public synchronized void put(String key, Object value) {

        primaryTable.put(key, value);

    }

    /**
     * Implementation of the eviction strategy.
     */
    protected synchronized void evictStaleEntries() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            int size = primaryTable.size() + secondaryTable.size() + tertiaryTable.size();
            Tr.debug(tc, "The current cache size is " + size + "( " + primaryTable.size() + ", " + secondaryTable.size() + ", " + tertiaryTable.size() + ")");
        }

        tertiaryTable = secondaryTable;
        secondaryTable = primaryTable;
        primaryTable = Collections.synchronizedMap(new BoundedHashMap(this.entryLimit));

    }

    private class EvictionTask extends TimerTask {

        /** {@inheritDoc} */
        @Override
        public void run() {
            evictStaleEntries();
        }

    }

}
