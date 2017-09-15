/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.env.was;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.wim.env.ICacheUtil;

/**
 * Cache containing three internal tables in order to implement a least-recently-used removal algorithm.
 */
public class Cache implements ICacheUtil {

    private static final TraceComponent tc = Tr.register(Cache.class, "Authentication");

    /**
     * Default cache timeout.
     */
    private static int defaultTimeout;

    /**
     * Primary hash table containing the most recently used entries.
     */
    private ConcurrentHashMap<String, Object> primaryTable;

    /**
     * Secondary hash table containing less recently used entries.
     */
    private ConcurrentHashMap<String, Object> secondaryTable;

    /**
     * Tertiary hash table containing least recently used entries that
     * are eligible for eviction.
     */
    private ConcurrentHashMap<String, Object> tertiaryTable;

    /**
     * Initial size of the hash tables in the cache.
     */
    private int minSize = 0;

    /**
     * Maximum number of entries allowed in the cache.
     */
    private int entryLimit = 4500;

    /**
     * Timer to schedule the eviction task.
     */
    private Timer timer;

    private ICacheUtil cache = null;

    public Cache() {

    }

    private Cache(int initialSize, int cacheMaxSize, long timeoutInMilliSeconds) {
        primaryTable = new ConcurrentHashMap<String, Object>(initialSize);
        secondaryTable = new ConcurrentHashMap<String, Object>(initialSize);
        tertiaryTable = new ConcurrentHashMap<String, Object>(initialSize);

        this.minSize = initialSize;
        this.entryLimit = cacheMaxSize;

        if (timeoutInMilliSeconds > 0) {
            scheduleEvictionTask(timeoutInMilliSeconds);
        }
    }

    /**
     * DoPriv action that swaps the thread's context classloader between
     * the system loader (first invocation) and the original thread's
     * context class loader.
     *
     */
    private class SwapTCCLAction implements PrivilegedAction<ClassLoader> {

        private ClassLoader savedTCCL;
        private boolean swapped = false;

        /*
         * (non-Javadoc)
         *
         * @see java.security.PrivilegedAction#run()
         */
        @Override
        public ClassLoader run() {
            ClassLoader cl;
            if (swapped) {
                cl = savedTCCL;
            } else {
                cl = ClassLoader.getSystemClassLoader();
                savedTCCL = Thread.currentThread().getContextClassLoader();
            }
            Thread.currentThread().setContextClassLoader(cl);
            swapped = !swapped;
            // return the new TCCL
            return cl;
        }
    }

    private void scheduleEvictionTask(long timeoutInMilliSeconds) {
        EvictionTask evictionTask = new EvictionTask();

        // Before creating new Timers, which create new Threads, we
        // must ensure that we are not using any application classloader
        // as the current thread's context classloader.  Otherwise, it
        // is possible that the new Timer thread would hold on to the
        // app classloader indefinitely, thereby leaking it and all
        // classes that it loaded, long after the app has been stopped.
        SwapTCCLAction swapTCCL = new SwapTCCLAction();
        AccessController.doPrivileged(swapTCCL);
        try {
            timer = new Timer(true);
            long period = timeoutInMilliSeconds;// / 2;
            long delay = period;
            timer.schedule(evictionTask, delay, period);
        } finally {
            AccessController.doPrivileged(swapTCCL);
        }
    }

    /**
     * Remove an object from the Cache.
     */
    public synchronized Object remove(String key) {
        primaryTable.remove(key);
        secondaryTable.remove(key);
        return tertiaryTable.remove(key);
    }

    /**
     * Find and return the object associated with the specified key.
     */
    @Override
    public synchronized Object get(Object key) {
        ConcurrentHashMap<String, Object> tableRef = primaryTable;
        Entry curEntry = (Entry) primaryTable.get(key);

        // Not found in primary
        if (curEntry == null) {
            tableRef = secondaryTable;
            curEntry = (Entry) secondaryTable.get(key);

            // Not found in primary or secondary
            if (curEntry == null) {
                tableRef = tertiaryTable;
                curEntry = (Entry) tertiaryTable.get(key);
            }

            // Not found in primary, secondary, or tertiary
            if (curEntry == null) {
                tableRef = null;
            }
        }

        // If found in secondary or tertiary, move entry to primary
        if ((tableRef != null) && (tableRef != primaryTable)) {
            primaryTable.put(String.valueOf(key), curEntry);
            tableRef.remove(key);
        }

        // If not present even in any table, add an empty entry
        // that can be found faster for update
        if (tableRef == null) {
            curEntry = (Entry) primaryTable.get(key);
            if (curEntry == null) {
                curEntry = new Entry();
                primaryTable.put(String.valueOf(key), curEntry);
            }
        }
        return curEntry.value;
    }

    /**
     * Insert the value into the Cache using the specified key.
     */
    public synchronized Object insert(String key, Object value) {
        // evict until size < maxSize
        while (isEvictionRequired() && entryLimit > 0 && entryLimit < Integer.MAX_VALUE) {
            evictStaleEntries();
        }

        Entry curEntry = new Entry(value);
        Entry oldEntry = (Entry) primaryTable.put(key, curEntry);

        return oldEntry;
    }

    /**
     * Determine if the cache is &quot;full&quot; and entries need
     * to be evicted.
     */
    protected boolean isEvictionRequired() {
        final String METHODNAME = "isEvictionRequired";
        boolean evictionRequired = false;

        int size = primaryTable.size() + secondaryTable.size() + tertiaryTable.size();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc,
                     METHODNAME + " The current cache (" + hashCode() + ") size is " + size + "( " + primaryTable.size() + ", " + secondaryTable.size() + ", "
                         + tertiaryTable.size() + ")");
        }

        if (entryLimit != 0 && entryLimit != Integer.MAX_VALUE) {
            // If the cache size is greater than its limit, time to purge...
            if (size > entryLimit) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, METHODNAME + " The cache size is " + size + "( " + primaryTable.size() + ", " + secondaryTable.size() + ", " + tertiaryTable.size()
                                 + ") which is greater than the cache limit of " + entryLimit + ".");

                evictionRequired = true;
            }
        }
        return evictionRequired;
    }

    /**
     * Implementation of the eviction strategy.
     */
    @Trivial
    protected synchronized void evictStaleEntries() {
/*
 * final String METHODNAME = "evictStaleEntries";
 * if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
 * int size = primaryTable.size() + secondaryTable.size() + tertiaryTable.size();
 * Tr.debug(tc, METHODNAME + " The current cache size is " + size + "( " + primaryTable.size() + ", " + secondaryTable.size() + ", " + tertiaryTable.size() + ")");
 * }
 */
        tertiaryTable = secondaryTable;
        secondaryTable = primaryTable;
        primaryTable = new ConcurrentHashMap<String, Object>((minSize > secondaryTable.size()) ? minSize : secondaryTable.size());
    }

    /**
     * Purge all entries from the Cache. Semantically, this should
     * behave the same way the the expiration of all entries from
     * the cache.
     */
    public synchronized void clearAllEntries() {

        tertiaryTable.putAll(primaryTable);
        tertiaryTable.putAll(secondaryTable);

        primaryTable.clear();
        secondaryTable.clear();

        evictStaleEntries();
    }

    public static long getDefaultTimeout() {
        return defaultTimeout;
    }

    public static void setDefaultTimeout(int timeout) {
        defaultTimeout = timeout;
    }

    public static class Entry {
        public Object value;
        public int timesAccessed;

        public Entry() {}

        public Entry(Object value) {
            this.value = value;
        }
    }

    int getEntryLimit() {
        return entryLimit;
    }

    @Trivial
    private class EvictionTask extends TimerTask {
        /** {@inheritDoc} */
        @Override
        public void run() {
            evictStaleEntries();
        }
    }

    @Override
    public void stopEvictionTask() {
        if (timer != null) {
            timer.cancel();
            timer.purge();
            timer = null;
        }
    }

    @Override
    public boolean isCacheInitialized() {
        if (cache != null)
            return true;

        return false;
    }

    @Override
    public boolean isCacheAvailable() {
        return true;
    }

    @Override
    public int getNotSharedInt() {
        return 0;
    }

    @Override
    public int getSharedPushInt() {
        return 0;
    }

    @Override
    public int getSharedPushPullInt() {
        return 0;
    }

    @Override
    public void setSharingPolicy(int sharingPolicy) {}

    @Override
    public int getSharingPolicyInt(String sharingPolicyStr) {
        return 0;
    }

    @Override
    public Object put(Object key, Object value, int priority, long timeToLive, int sharingPolicy, Object dependencyIds[]) {
        return put(String.valueOf(key), value);
    }

    @Override
    public void invalidate(Object key) {
        remove(key);
    }

    @Override
    public int size(boolean includeDiskCache) {
        return primaryTable.size() + secondaryTable.size() + tertiaryTable.size();
    }

    @Override
    public boolean isEmpty(boolean includeDiskCache) {
        if (primaryTable.isEmpty() && secondaryTable.isEmpty() && tertiaryTable.isEmpty())
            return true;

        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        if (primaryTable.containsKey(key) || secondaryTable.containsKey(key) || tertiaryTable.containsKey(key))
            return true;

        return false;
    }

    @Override
    public boolean containsKey(Object key, boolean includeDiskCache) {
        if (primaryTable.containsKey(key) || secondaryTable.containsKey(key) || tertiaryTable.containsKey(key))
            return true;

        return false;
    }

    @Override
    public Set<String> keySet(boolean includeDiskCache) {
        Set<String> keySet = new HashSet<String>();
        keySet.addAll(primaryTable.keySet());
        keySet.addAll(secondaryTable.keySet());
        keySet.addAll(tertiaryTable.keySet());
        return keySet;
    }

    @Override
    public Object put(String key, Object value) {
        return insert(String.valueOf(key), value);
    }

    @Override
    public void clear() {
        clearAllEntries();
    }

    @Override
    public boolean containsValue(Object value) {
        if (primaryTable.containsValue(value) || secondaryTable.containsValue(value) || tertiaryTable.containsValue(value))
            return true;

        return false;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Set<String> keySet() {
        Set<String> keySet = new HashSet<String>();
        keySet.addAll(primaryTable.keySet());
        keySet.addAll(secondaryTable.keySet());
        keySet.addAll(tertiaryTable.keySet());
        return keySet;
    }

    @Override
    public int size() {
        return primaryTable.size() + secondaryTable.size() + tertiaryTable.size();
    }

    @Override
    public void setTimeToLive(int timeToLive) {
        defaultTimeout = timeToLive;
    }

    @Override
    public ICacheUtil initialize(int initialSize, int cacheSize, long cachetimeOut) {

        final String METHODNAME = "initialize";
        cache = new Cache(initialSize, cacheSize, cachetimeOut);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " cache initialized successfully");
        }

        return cache;
    }

    @Override
    public ICacheUtil initialize(String cacheName, int cacheSize, boolean diskOffLoad) {
        return initialize(cacheSize, cacheSize, defaultTimeout);
    }

    @Override
    public ICacheUtil initialize(String cacheName, int cacheSize, boolean diskOffLoad, int sharingPolicy) {
        return initialize(cacheSize, cacheSize, defaultTimeout);
    }

    @Override
    public Set<java.util.Map.Entry<String, Object>> entrySet() {
        Set<java.util.Map.Entry<String, Object>> retValue = primaryTable.entrySet();
        retValue.addAll(secondaryTable.entrySet());
        retValue.addAll(tertiaryTable.entrySet());
        return retValue;
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> map) {
        Set<?> entrySet = map.entrySet();
        for (Object entry : entrySet) {
            Map.Entry<String, Object> actualEntry = (Map.Entry<String, Object>) entry;
            put(String.valueOf(actualEntry.getKey()), actualEntry.getValue());
        }
    }

    @Override
    public Object remove(Object key) {
        return remove(String.valueOf(key));
    }

    @Override
    public Collection<Object> values() {
        Collection<Object> retValue = primaryTable.values();
        retValue.addAll(secondaryTable.values());
        retValue.addAll(tertiaryTable.values());
        return retValue;
    }

    @Override
    public void invalidate() {
        clearAllEntries();
    }
}
