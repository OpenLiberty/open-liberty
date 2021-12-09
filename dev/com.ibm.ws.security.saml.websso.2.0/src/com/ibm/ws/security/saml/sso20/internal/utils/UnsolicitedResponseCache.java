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
package com.ibm.ws.security.saml.sso20.internal.utils;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.security.common.structures.BoundedHashMap;

/**
 * Cache containing three internal tables in order to implement a least-recently-used removal algorithm.
 */
public class UnsolicitedResponseCache {

    private static final TraceComponent tc = Tr.register(UnsolicitedResponseCache.class);

    /**
     * Primary hash table containing the most recently used entries.
     */
    private final Map<String, Object> primaryTable;

    /**
     * Maximum number of entries allowed in the cache.
     */
    private int entryLimit = 50000;
    /**
     * Default cache timeout.
     */
    private long timeoutInMilliSeconds = 15 * 60 * 1000;

    private long clockSkewInMilliSeconds = 0;

    /**
     * Timer to schedule the eviction task.
     */
    private Timer timer;

    public UnsolicitedResponseCache(int entryLimit, long timeoutInMilliSeconds, long clockSkew) {
        if (entryLimit > 0) {
            this.entryLimit = entryLimit;
        }

        if (timeoutInMilliSeconds > 0) {
            this.timeoutInMilliSeconds = timeoutInMilliSeconds;
        }

        this.clockSkewInMilliSeconds = clockSkew;

        primaryTable = Collections.synchronizedMap(new BoundedHashMap(this.entryLimit));

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

    }

    /**
     * Find and return the object associated with the specified key.
     */
    public synchronized Object get(String key) {

        Object curEntry = primaryTable.get(key);

        return curEntry;
    }

    /**
     * Insert the value into the Cache using the specified key.
     */
    public synchronized void put(String key, Object value) {

        primaryTable.put(key, value);

    }

    public synchronized boolean isValid(String key) {

        Object curEntry = primaryTable.get(key);
        if (curEntry != null) {
            Date now = new Date();
            Long lEntry = (Long) curEntry;
            Date exp = new Date(lEntry.longValue() + this.clockSkewInMilliSeconds);
            if (exp.after(now)) {
                return true;
            } else {
                primaryTable.remove(key);
            }
        }

        return false;
    }

    protected synchronized void invalidate() {

        /*
         * Two phases to the remove are used to avoid
         * ConcurrentModificationException, and minimize impact to main path
         */
        Map<String, Object> tmpTable = new HashMap<String, Object>();
        tmpTable.putAll(this.primaryTable);
        Set<String> keys = tmpTable.keySet();
        Set<String> keysToRemove = new HashSet<String>();
        Date now = new Date();

        // find and record keys to remove
        for (Iterator<String> i = keys.iterator(); i.hasNext();) {
            String key = i.next();
            Object ce = tmpTable.get(key);
            if (ce != null) {
                Date cd = new Date((Long) ce);
                if (cd.before(now)) {
                    keysToRemove.add(key);
                }
            }
        }
        // now remove them
        for (Iterator<String> i = keysToRemove.iterator(); i.hasNext();) {
            String key = i.next();
            this.primaryTable.remove(key);
        }
        // tmpTable.clear(); not needed

    }

    /**
     * Implementation of the eviction strategy.
     */
    protected synchronized void evictStaleEntries() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            int size = primaryTable.size();
            Tr.debug(tc, "The current cache size is " + size);
        }

        invalidate();

    }

    private class EvictionTask extends TimerTask {

        /** {@inheritDoc} */
        @Override
        public void run() {
            evictStaleEntries();
        }

    }

}
