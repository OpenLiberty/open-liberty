/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
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
import java.util.ArrayList;
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
 * Cache containing three internal tables in order to implement a FIFO removal algorithm.
 */
public class Cache implements ICacheUtil {

    private static final TraceComponent tc = Tr.register(Cache.class, "Authentication");

    /**
     * Default cache timeout.
     */
    private static int defaultTimeout;

    /**
     * Primary hash table containing the most recently entered entries.
     */
    private ConcurrentHashMap<String, Object> primaryTable;

    /**
     * Secondary hash table containing less recently entered entries.
     */
    private ConcurrentHashMap<String, Object> secondaryTable;

    /**
     * Tertiary hash table containing least recently entered entries that
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

    /**
     * Boolean to determine whether the cache has been initialized.
     */
    private boolean isInitialized = false;

    /**
     * The name of the cache, used for trace
     */
    private String cacheName = "DefaultCacheName";

    /**
     * Default constructor for the cache
     */
    public Cache() {

    }

    /**
     * Constructs a cache with given values
     *
     * @param initialSize initial size of the HashTables in the cache
     * @param cacheMaxSize max size of the HashTables in the cache
     * @param timeoutInMilliSeconds default timeout for the cache
     */
    private Cache(int initialSize, int cacheMaxSize, long timeoutInMilliSeconds, String name) {
        primaryTable = new ConcurrentHashMap<String, Object>(initialSize);
        secondaryTable = new ConcurrentHashMap<String, Object>(initialSize);
        tertiaryTable = new ConcurrentHashMap<String, Object>(initialSize);

        this.minSize = initialSize;
        this.entryLimit = cacheMaxSize;
        this.cacheName = name;
        this.isInitialized = true;
        setDefaultTimeout((int) timeoutInMilliSeconds);

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

    /**
     * Creates a timer and schedules the eviction task based on the timeout in milliseconds
     *
     * @param timeoutInMilliSeconds the time to be used in milliseconds for the eviction task
     */
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
            long period = timeoutInMilliSeconds / 3;
            long delay = period;
            timer.schedule(evictionTask, delay, period);
        } finally {
            AccessController.doPrivileged(swapTCCL);
        }
    }

    /**
     * Remove an object from the Cache.
     *
     * @param key the key of the entry to be removed
     * @return the previous value associated with key, or null if there was none
     */
    public synchronized Object remove(String key) {
        Object retVal;

        retVal = primaryTable.remove(key);
        if (retVal == null) {
            retVal = secondaryTable.remove(key);
            if (retVal == null) {
                return tertiaryTable.remove(key);
            }
        }
        return retVal;
    }

    /**
     * Find and return the object associated with the specified key.
     */
    @Override
    public synchronized Object get(Object key) {
        ConcurrentHashMap<String, Object> tableRef = primaryTable;
        CacheEntry curEntry = (CacheEntry) primaryTable.get(key);

        // Not found in primary
        if (curEntry == null) {
            tableRef = secondaryTable;
            curEntry = (CacheEntry) secondaryTable.get(key);

            // Not found in primary or secondary
            if (curEntry == null) {
                tableRef = tertiaryTable;
                curEntry = (CacheEntry) tertiaryTable.get(key);
            }

            // Not found in primary, secondary, or tertiary
            if (curEntry == null) {
                tableRef = null;
            }
        }

        // If not present even in any table, add an empty entry
        // that can be found faster for update
        if (tableRef == null) {
            curEntry = (CacheEntry) primaryTable.get(key);
            if (curEntry == null) {
                curEntry = new CacheEntry();
                primaryTable.put(String.valueOf(key), curEntry);
            }
        }
        return curEntry.value;
    }

    /**
     * Insert the value into the Cache using the specified key.
     *
     * @param key the key of the entry to be inserted
     * @param value the value of the entry to be inserted
     * @return the previous value associated with key, or null if there was none
     */
    public synchronized Object insert(String key, Object value) {
        // evict until size < maxSize
        while (isEvictionRequired() && entryLimit > 0 && entryLimit < Integer.MAX_VALUE) {
            evictStaleEntries();
        }

        CacheEntry curEntry = new CacheEntry(value);
        CacheEntry oldEntry1 = (CacheEntry) primaryTable.put(key, curEntry);

        CacheEntry oldEntry2 = (CacheEntry) secondaryTable.remove(key);
        CacheEntry oldEntry3 = (CacheEntry) tertiaryTable.remove(key);

        return (oldEntry1 != null) ? oldEntry1.value : (oldEntry2 != null) ? oldEntry2.value : (oldEntry3 != null) ? oldEntry3.value : null;
    }

    /**
     * Update the value into the Cache using the specified key, but do not change
     * the table level of the cache. If the key is not in any table, add it to
     * the primaryTable
     *
     * @param key the key of the entry to be inserted
     * @param value the value of the entry to be inserted
     * @return the previous value associated with key, or null if there was none
     */
    public synchronized Object update(String key, Object value) {
        // evict until size < maxSize
        while (isEvictionRequired() && entryLimit > 0 && entryLimit < Integer.MAX_VALUE) {
            evictStaleEntries();
        }

        CacheEntry oldEntry = null;
        CacheEntry curEntry = new CacheEntry(value);
        if (primaryTable.containsKey(key)) {
            oldEntry = (CacheEntry) primaryTable.put(key, curEntry);
        } else if (secondaryTable.containsKey(key)) {
            oldEntry = (CacheEntry) secondaryTable.put(key, curEntry);
        } else if (tertiaryTable.containsKey(key)) {
            oldEntry = (CacheEntry) tertiaryTable.put(key, curEntry);
        } else {
            oldEntry = (CacheEntry) primaryTable.put(key, curEntry);
        }

        return oldEntry != null ? oldEntry.value : null;
    }

    /**
     * Determine if the cache is &quot;full&quot; and entries need
     * to be evicted.
     *
     * @return true if eviction is required
     */
    protected boolean isEvictionRequired() {
        final String METHODNAME = "isEvictionRequired";
        boolean evictionRequired = false;

        int size = primaryTable.size() + secondaryTable.size() + tertiaryTable.size();
        if (tc.isDebugEnabled()) {
            Tr.debug(tc,
                     METHODNAME + " The current " + cacheName + " cache (" + hashCode() + ") size is " + size + "( " + primaryTable.size() + ", " + secondaryTable.size() + ", "
                         + tertiaryTable.size() + ")");
        }

        if (entryLimit != 0 && entryLimit != Integer.MAX_VALUE) {
            // If the cache size would be greater than its limit, time to purge...
            if (size >= entryLimit) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, METHODNAME + " The cache size on " + cacheName + " is " + size + "( " + primaryTable.size() + ", " + secondaryTable.size() + ", "
                                 + tertiaryTable.size()
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

        // log only when we evict the last table
        if (!tertiaryTable.isEmpty()) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "evictStaleEntries Evicting tertiaryTable cache " + cacheName + ", size is " + tertiaryTable.size());
            }
        }
        tertiaryTable = secondaryTable;
        secondaryTable = primaryTable;
        primaryTable = new ConcurrentHashMap<String, Object>((minSize > secondaryTable.size()) ? minSize : secondaryTable.size());
    }

    /**
     * Purge all entries from the Cache. Semantically, this should
     * behave the same as the expiration of all entries from
     * the cache.
     */
    public synchronized void clearAllEntries() {

        tertiaryTable.putAll(primaryTable);
        tertiaryTable.putAll(secondaryTable);

        primaryTable.clear();
        secondaryTable.clear();

        evictStaleEntries();
    }

    /**
     * retreives the default timeout value
     *
     * @return the default timeout value
     */
    public static long getDefaultTimeout() {
        return defaultTimeout;
    }

    /**
     * sets the default timeout value
     */
    public static void setDefaultTimeout(int timeout) {
        defaultTimeout = timeout;
    }

    /**
     * A class to wrap values entered to the cache
     */
    public static class CacheEntry {

        public Object value;
        public int timesAccessed;

        public CacheEntry() {}

        public CacheEntry(Object value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CacheEntry other = (CacheEntry) obj;
            if (value == null) {
                if (other.value != null)
                    return false;
            } else if (!value.equals(other.value))
                return false;
            return true;
        }

    }

    /**
     * The eviction task for evicting stale entries
     */
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
        return isInitialized;
    }

    /**
     * Not implemented
     */
    @Override
    public boolean isCacheAvailable() {
        return true;
    }

    /**
     * Not implemented
     */
    @Override
    public int getNotSharedInt() {
        return 0;
    }

    /**
     * Not implemented
     */
    @Override
    public int getSharedPushInt() {
        return 0;
    }

    /**
     * Not implemented
     */
    @Override
    public int getSharedPushPullInt() {
        return 0;
    }

    /**
     * Not implemented
     */
    @Override
    public void setSharingPolicy(int sharingPolicy) {}

    /**
     * Not implemented
     */
    @Override
    public int getSharingPolicyInt(String sharingPolicyStr) {
        return 0;
    }

    /**
     * A put with the TTL defined is a new entry and should be inserted in the cache in the primaryTable.
     * To do an update action to the cache, see put(key,value).
     */
    @Override
    public Object put(Object key, Object value, int priority, long timeToLive, int sharingPolicy, Object dependencyIds[]) {
        return insert(String.valueOf(key), value);
    }

    @Override
    public void invalidate(Object key) {
        remove(key);
    }

    @Override
    public int size(boolean includeDiskCache) {
        return size();
    }

    @Override
    public boolean isEmpty(boolean includeDiskCache) {
        return isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        if (primaryTable.containsKey(key) || secondaryTable.containsKey(key) || tertiaryTable.containsKey(key))
            return true;

        return false;
    }

    @Override
    public boolean containsKey(Object key, boolean includeDiskCache) {
        return containsKey(key);
    }

    @Override
    public Set<String> keySet(boolean includeDiskCache) {
        return keySet();
    }

    /**
     * A put with only the key, value provided acts as an update to the cache and
     * the entry retains the same creation time to live in the cache.
     * To do an insert action to the cache, see put(key,value,int,int,int,Object).
     */
    @Override
    public Object put(String key, Object value) {
        return update(String.valueOf(key), value);
    }

    @Override
    public void clear() {
        clearAllEntries();
    }

    @Override
    public boolean containsValue(Object value) {
        CacheEntry entry = new CacheEntry(value);
        if (primaryTable.containsValue(entry) || secondaryTable.containsValue(entry) || tertiaryTable.containsValue(entry))
            return true;

        return false;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
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
        setDefaultTimeout(timeToLive);
    }

    @Override
    public ICacheUtil initialize(int initialSize, int cacheSize, long cachetimeOut) {
        return initialize(cacheName, initialSize, cacheSize, cachetimeOut);
    }

    @Override
    public ICacheUtil initialize(String name, int initialSize, int cacheSize, long cachetimeOut) {

        final String METHODNAME = "initialize";
        Cache cache = new Cache(initialSize, cacheSize, cachetimeOut, name);

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, METHODNAME + " cache initialized successfully: " + name);
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
        ArrayList<Object> retValue = new ArrayList<Object>();

        retValue.addAll(primaryTable.values());
        retValue.addAll(secondaryTable.values());
        retValue.addAll(tertiaryTable.values());

        return retValue;
    }

    @Override
    public void invalidate() {
        clearAllEntries();
    }
}