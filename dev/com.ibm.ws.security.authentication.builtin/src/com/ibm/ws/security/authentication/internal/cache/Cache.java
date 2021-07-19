/*******************************************************************************
 * Copyright (c) 1997, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.authentication.internal.cache;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.security.authentication.cache.CacheEvictionListener;

import io.openliberty.jcache.JCacheService;

/**
 * Cache containing three internal tables in order to implement a least-recently-used removal algorithm.
 */
public class Cache {

    private static final TraceComponent tc = Tr.register(Cache.class, "Authentication");

    /**
     * Primary hash table containing the most recently used entries.
     */
    private ConcurrentHashMap<Object, Object> primaryTable;

    /**
     * Secondary hash table containing less recently used entries.
     */
    private ConcurrentHashMap<Object, Object> secondaryTable;

    /**
     * Tertiary hash table containing least recently used entries that
     * are eligible for eviction.
     */
    private ConcurrentHashMap<Object, Object> tertiaryTable;

    /**
     * Initial size of the hash tables in the cache.
     */
    private int minSize = 0;

    /**
     * Maximum number of entries allowed in the cache.
     */
    private int entryLimit = 500;

    /**
     * Listener for cache eviction notifications.
     */
    private final Set<CacheEvictionListener> cacheEvictionListenerSet;

    /**
     * Timer to schedule the eviction task.
     */
    private Timer timer;

    /**
     * The {@link JCacheService} being used, if one is provided.
     */
    private JCacheService jCacheService = null;

    private boolean registeredJCacheListener = false;

    public Cache(JCacheService jCacheService, Set<CacheEvictionListener> callbackSet) {
        /*
         * Eviction, map sizing, etc are all handled by the JCache implementation.
         */
        this.jCacheService = jCacheService;
        this.cacheEvictionListenerSet = callbackSet;
    }

    public Cache(int initialSize, int entryLimit, long timeoutInMilliSeconds) {
        this(initialSize, entryLimit, timeoutInMilliSeconds, null);
    }

    public Cache(int initialSize, int entryLimit, long timeoutInMilliSeconds, Set<CacheEvictionListener> callbackSet) {
        primaryTable = new ConcurrentHashMap<Object, Object>(initialSize);
        secondaryTable = new ConcurrentHashMap<Object, Object>(initialSize);
        tertiaryTable = new ConcurrentHashMap<Object, Object>(initialSize);

        this.minSize = initialSize;
        this.entryLimit = entryLimit;
        this.cacheEvictionListenerSet = callbackSet;

        if (timeoutInMilliSeconds > 0) {
            scheduleEvictionTask(timeoutInMilliSeconds);
        }
    }

    private void scheduleEvictionTask(long timeoutInMilliSeconds) {
        EvictionTask evictionTask = new EvictionTask();
        timer = new Timer(true);
        long period = timeoutInMilliSeconds / 2;
        long delay = period;
        timer.schedule(evictionTask, delay, period);
    }

    /**
     * Remove an object from the Cache.
     */
    public synchronized void remove(Object key) {
        javax.cache.Cache<Object, byte[]> jCache = getJCache();
        if (jCache != null) {
            jCache.remove(key);
            printJCacheContents(jCache); // TODO REMOVE
            return;
        }

        Object victim = null;
        if (cacheEvictionListenerSet.isEmpty() == false) {
            victim = get(key);
        }

        primaryTable.remove(key);
        secondaryTable.remove(key);
        tertiaryTable.remove(key);

        if (victim != null) {
            ArrayList<Object> evictedValues = new ArrayList<Object>();
            evictedValues.add(victim);
            for (CacheEvictionListener evictionCallback : cacheEvictionListenerSet) {
                evictionCallback.evicted(evictedValues);
            }
        }
    }

    /**
     * Find and return the object associated with the specified key.
     */
    public synchronized Object get(Object key) {
        javax.cache.Cache<Object, byte[]> jCache = getJCache();
        if (jCache != null) {
            printJCacheContents(jCache); // TODO REMOVE

            /*
             * Search the JCache for the entry.
             */
            byte[] objBytes = jCache.get(key);
            if (objBytes == null) {
                Tr.debug(tc, "JCache MISS for key " + key);
            } else {
                Tr.debug(tc, "JCache HIT for key " + key);
            }

            /*
             * Use the SerializationService to deserialize the entry.
             */
            return jCacheService.deserialize(objBytes);
        }

        ConcurrentHashMap<Object, Object> tableRef = primaryTable;
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
            primaryTable.put(key, curEntry);
            tableRef.remove(key);
        }

        // If not present even in any table, add an empty entry
        // that can be found faster for update
        if (tableRef == null) {
            curEntry = (Entry) primaryTable.get(key);
            if (curEntry == null) {
                curEntry = new Entry();
                Entry prevEntry = (Entry) primaryTable.putIfAbsent(key, curEntry);
                if (prevEntry != null)
                    curEntry = prevEntry; // We lost the race, so use the entry from the other thread
            }
        }
        return curEntry.value;
    }

    /**
     * Insert the value into the Cache using the specified key.
     */
    public synchronized void insert(Object key, Object value) {
        javax.cache.Cache<Object, byte[]> jCache = getJCache();
        if (jCache != null) {
            jCache.put(key, jCacheService.serialize(value));
            printJCacheContents(jCache); // TODO REMOVE
            return;
        }

        // evict until size < maxSize
        while (isEvictionRequired() && entryLimit > 0 && entryLimit < Integer.MAX_VALUE) {
            evictStaleEntries();
        }

        Entry curEntry = new Entry(value);
        Entry oldEntry = (Entry) primaryTable.put(key, curEntry);

        if (oldEntry != null && oldEntry.value != null) {
            ArrayList<Object> evictedValues = new ArrayList<Object>();
            evictedValues.add(oldEntry.value);
            for (CacheEvictionListener evictionCallback : cacheEvictionListenerSet) {
                evictionCallback.evicted(evictedValues);
            }
        }
    }

    /**
     * Determine if the cache is &quot;full&quot; and entries need
     * to be evicted.
     */
    protected boolean isEvictionRequired() {
        /*
         * JCache implementation will handle eviction.
         */
        if (getJCache() != null) {
            return false;
        }

        boolean evictionRequired = false;
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            int size = primaryTable.size() + secondaryTable.size() + tertiaryTable.size();
            Tr.debug(tc, "The current cache size is " + size + "( " + primaryTable.size() + ", " + secondaryTable.size() + ", " + tertiaryTable.size() + ")");
        }

        if (entryLimit != 0 && entryLimit != Integer.MAX_VALUE) {
            int size = primaryTable.size() + secondaryTable.size() + tertiaryTable.size();

            // If the cache size is greater than its limit, time to purge...
            if (size > entryLimit) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "The cache size is " + size + "( " + primaryTable.size() + ", " + secondaryTable.size() + ", " + tertiaryTable.size()
                                 + ") which is greater than the cache limit of " + entryLimit + ".");
                evictionRequired = true;
            }
        }
        return evictionRequired;
    }

    /**
     * Implementation of the eviction strategy.
     */
    protected synchronized void evictStaleEntries() {
        /*
         * JCache implementation will handle eviction.
         */
        if (getJCache() != null) {
            return;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            int size = primaryTable.size() + secondaryTable.size() + tertiaryTable.size();
            Tr.debug(tc, "The current cache size is " + size + "( " + primaryTable.size() + ", " + secondaryTable.size() + ", " + tertiaryTable.size() + ")");
        }

        ConcurrentHashMap<Object, Object> victims = tertiaryTable;

        tertiaryTable = secondaryTable;
        secondaryTable = primaryTable;
        primaryTable = new ConcurrentHashMap<Object, Object>((minSize > secondaryTable.size()) ? minSize : secondaryTable.size());

        if (victims.isEmpty() == false) {
            List<Object> evictedValues = new ArrayList<Object>();
            Iterator<Object> iterator = victims.values().iterator();
            while (iterator.hasNext()) {
                Entry entry = (Entry) iterator.next();
                if (entry.value != null) {
                    evictedValues.add(entry.value);
                }
            }

            for (CacheEvictionListener evictionCallback : cacheEvictionListenerSet) {
                evictionCallback.evicted(evictedValues);
            }
        }
    }

    /**
     * Purge all entries from the Cache. Semantically, this should
     * behave the same way the the expiration of all entries from
     * the cache.
     */
    protected synchronized void clearAllEntries() {
        return; // TODO This needs to be removed. Here to stop init on startup.

//        javax.cache.Cache<Object, byte[]> jCache = getJCache();
//        if (jCache != null) {
//            printJCacheContents(jCache); // TODO REMOVE
//            jCache.removeAll(); // Notifies listeners, clear() does not.
//            return;
//        }
//
//        tertiaryTable.putAll(primaryTable);
//        tertiaryTable.putAll(secondaryTable);
//
//        primaryTable.clear();
//        secondaryTable.clear();
//
//        evictStaleEntries();
    }

    // TODO If we don't need this, remove as it only adds work to serialization.
    public static class Entry implements Serializable {
        // TODO Generate UID
        private static final long serialVersionUID = 1L;

        public Object value;

        public Entry() {
        }

        public Entry(Object value) {
            this.value = value;
        }
    }

    private class EvictionTask extends TimerTask {

        /** {@inheritDoc} */
        @Override
        public void run() {
            evictStaleEntries();
        }

    }

    protected void stopEvictionTask() {
        if (timer != null) {
            timer.cancel();
        }
    }

    /**
     * Get the JCache {@link javax.cache.Cache} instance that backs this cache. This method
     * will also register the cache eviction listeners with the JCache cache {@link javax.cache.Cache}.
     *
     * @return The JCache {@link javax.cache.Cache} instance that backs this cache, if there is one. Otherwise; null.
     */
    private javax.cache.Cache<Object, byte[]> getJCache() {
        javax.cache.Cache<Object, byte[]> jCache = null;
        if (jCacheService != null) {
            jCache = jCacheService.getCache();

            /*
             * Register as JCache listeners.
             *
             * TODO Need to unregister on teardown? Reset registeredJCacheListener
             */
            if (!registeredJCacheListener) {
// TODO Causes weird CNFE on multi-node hazelcast cluster
//                JCacheListener<Object, byte[]> jcl = new JCacheListener<Object, byte[]>(jCacheService, cacheEvictionListenerSet);
//                jCache.registerCacheEntryListener(new MutableCacheEntryListenerConfiguration<Object, byte[]>(FactoryBuilder.factoryOf(jcl), null, false, false));
                registeredJCacheListener = true;
            }
        }

        return jCache;
    }

    // TODO REMOVE
    @Trivial
    private void printJCacheContents(javax.cache.Cache<Object, byte[]> jCache) {
        Iterator<javax.cache.Cache.Entry<Object, byte[]>> iter = jCache.iterator();
        Set<String> entries = new HashSet<String>();
        while (iter.hasNext()) {
            javax.cache.Cache.Entry<Object, byte[]> entry = iter.next();
            entries.add(entry.getKey() + ": " + entry.getValue().length);
        }
        Tr.debug(tc, "Entries: " + entries);
    }
}
