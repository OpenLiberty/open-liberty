/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.session.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.ibm.wsspi.session.IStoreCallback;

/**
 * This class extends java.util.HashMap though it's maximum size is input
 * to the constructor and will not grow. This class also keeps track of the
 * Most-Recently-Used (MRU) and Least-Recently-Used (LRU) entries and all in
 * between.
 * Entries are considered to be MRU when it is created via put() and also
 * whenever
 * it is accessed via get().
 * In the event of a table full condition during put(), the oldest entry is
 * removed
 * and replaced with the new entry. The replaced entry is returned to the
 * caller.
 * Since Hashmap is totally unsynchronized, this class will provide the needed
 * synchronization for a threaded environment.
 * The keysSet() method will return a non "Fail-fast" version of the keys that
 * are a snapshot at the time of the method call.
 * 
 * @see java.util.HashMap
 */
public class LRUHashMap extends HashMap {

    private IStoreCallback _iStoreCallback;
    CacheEntryWrapper mru;
    CacheEntryWrapper lru;
    int maxSize;
    int currentSize;
    private static final long serialVersionUID = -1137988339144221054L;

    private static final String methodClassName = "LRUHashMap";

    private static final int PUT = 0;
    private static final int ACCESS_OBJECT = 1;
    private static final int GET = 2;
    private static final int REMOVE = 3;
    private static final int REMOVE_GUTS = 4;
    private static final int ENTRY_SET = 5;
    private static final int KEY_SET = 6;
    private static final int UPDATE_CACHE_LIST = 7;

    private static final String methodNames[] = { "put", "accessObject", "get", "remove", "removeGuts", "entrySet", "keySet", "updateCacheList" };

    public LRUHashMap() {
        this(128);
    }

    /**
     * Constructs a new, empty LRUHashMap with the specified maximum capacity
     * 
     * @param maxCapacity
     *            the maximum capacity of the table.
     */
    public LRUHashMap(int maxCapacity) {

        // Need to play with load factor and initial capacity so that no rehash ever
        // occurs

        super(maxCapacity + 20, 1);
        currentSize = 0;
        lru = null;
        mru = null;

        maxSize = maxCapacity;

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodClassName, "capacity is : " + maxSize);
        }
    }

    public void setStoreCallback(IStoreCallback callback) {
        _iStoreCallback = callback;
    }

    public IStoreCallback getStoreCallback() {
        return _iStoreCallback;
    }

    // If an entry that uses this key is found then it will **NOT** be returned
    // If the hashmap is full then the oldest entry is removed and this
    // is returned to the caller.

    public synchronized Object put(Object key, Object value) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            //PM16861 Modified trace statement by removing toString of session object to avoid deadlock
            StringBuffer sb = new StringBuffer("key=").append(key);
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[PUT], sb.toString());
        }

        if (maxSize == 0)
            return null;

        CacheEntryWrapper currEntry;
        CacheEntryWrapper oldestEntry = null;

        // See if entry exists
        currEntry = (CacheEntryWrapper) super.get(key);

        if (currEntry == null) { // need new entry
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[PUT], "Doesn't exist in HashMap");
            }

            currentSize++;
            if (_iStoreCallback != null) {
                _iStoreCallback.sessionLiveCountInc(value);
            }

            if (currentSize > maxSize) {
                // too many entries, remove the oldest
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[PUT], "Too Many Entries.. Remove the oldest entry: " + lru.key);
                }
                oldestEntry = (CacheEntryWrapper) removeGuts(lru.key);
                if (_iStoreCallback != null) {
                    _iStoreCallback.sessionCacheDiscard(oldestEntry.value);
                }
            }

            currEntry = new CacheEntryWrapper();
            currEntry.key = key;
            currEntry.value = value;

            currEntry.next = mru;
            if (mru != null)
                mru.prev = currEntry;
            else
                lru = currEntry;
            mru = currEntry;

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[PUT], "Adding new entry to the map");
            }

        }

        else { // found entry

            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[PUT], "Key already in use .. Reuse the entry");
            }

            updateCacheList(key);
            currEntry.value = value;
        }

        Object replacedEntry = super.put(key, currEntry);

        if (oldestEntry != null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                //PM16861
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[PUT], "Returning object associated with this key: " + oldestEntry.key);
            }
            return oldestEntry.value;
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[PUT], null);
        }
        return null;
    }

    public Object accessObject(Object key) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[ACCESS_OBJECT], "key=" + key);
        }

        CacheEntryWrapper currEntry = updateCacheList(key);// (CacheEntryWrapper)
                                                           // super.get(key);

        if (currEntry == null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[ACCESS_OBJECT], "null - Object doesn't exist.");
            }
            return null;
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[ACCESS_OBJECT], "Object=" + currEntry.value);
        }
        return currEntry.value;
    }

    public Object get(Object key) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[GET], "key=" + key);
        }

        CacheEntryWrapper currEntry = (CacheEntryWrapper) super.get(key);

        if (currEntry == null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[GET], "null - Object doesn't exist.");
            }
            return null;
        }
        // PQ83345 Start dead lock
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[GET], "found object with key=" + key);
        }

        // PQ83345 end

        return currEntry.value;

    }

    public synchronized Object remove(Object key) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[REMOVE], "Removing the object associated with this key=" + key);
        }

        CacheEntryWrapper entry = (CacheEntryWrapper) removeGuts(key);

        if (entry == null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[REMOVE], "null - Object doesn't exist.");
            }
            return null;
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            //PM16861
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[REMOVE], "The object being returned was associated with this key=" + key);
        }
        return entry.value;
    }

    private Object removeGuts(Object key) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[REMOVE_GUTS], "key=" + key);
        }
        CacheEntryWrapper currEntry = (CacheEntryWrapper) super.remove(key);

        if (currEntry == null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[REMOVE_GUTS], "key not found in hashmap");
            }
            return null;
        }

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.logp(Level.FINE, methodClassName, methodNames[REMOVE_GUTS], "key found in hashmap");
        }
        currentSize--;
        if (_iStoreCallback != null) {
            _iStoreCallback.sessionLiveCountDec(currEntry.value);
        }
        CacheEntryWrapper prev = currEntry.prev;
        CacheEntryWrapper next = currEntry.next;

        if (prev == null) {
            mru = next;
        } else {
            prev.next = next;
        }

        if (next == null) {
            lru = prev;
        } else {
            next.prev = prev;
        }

        currEntry.prev = null;
        currEntry.next = null;

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[REMOVE_GUTS], "returning with value: " + currEntry);
        }
        return currEntry;
    }

    public synchronized void clear() {

        super.clear();
        if (_iStoreCallback != null) {
            for (int i = 0; i < currentSize; i++) {
                _iStoreCallback.sessionLiveCountDec(null);
            }
        }
        currentSize = 0;
        mru = null;
        lru = null;
    }

    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    public void putAll(Map t) {
        throw new UnsupportedOperationException();
    }

    public Set entrySet() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[ENTRY_SET]);
        }

        Set myEntrySet;
        Object[] entries = null;

        // Get the "fail-fast keyset from the HashMap and convert it to an array
        myEntrySet = super.entrySet();

        if (myEntrySet != null) {
            synchronized (this) { // this instantiates and uses the fail-fast iterator
                entries = myEntrySet.toArray();
            }
        }

        // convert to actual values
        for (int i = 0; i < entries.length; i++) {
            Map.Entry me = (Map.Entry) entries[i];
            CacheEntryWrapper cew = (CacheEntryWrapper) me.getValue();
            entries[i] = cew.value;
        }

        // get a non-fail-fast Set
        Set entrySet = new SessionHashSet(entries);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[ENTRY_SET], "returning entrySet " + entrySet);
        }
        return entrySet;
    }

    public Collection values() {
        throw new UnsupportedOperationException();
    }

    public Set keySet() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[KEY_SET]);
        }

        Set myKeySet;
        Object[] keys = null;

        // Get the "fail-fast keyset from the HashMap and convert it to an array
        myKeySet = super.keySet();

        if (myKeySet != null) {
            synchronized (this) { // this instantiates and uses the fail-fast iterator
                keys = myKeySet.toArray();
            }
        }

        // get a non-fail-fast Set
        Set keySet = new SessionHashSet(keys);
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[KEY_SET], "returning keySet " + keySet);
        }
        return keySet;
    }

    // remove the input entry from it's spot in the list and make it the mru
    private synchronized CacheEntryWrapper updateCacheList(Object key) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[UPDATE_CACHE_LIST], "key=" + key);
        }
        CacheEntryWrapper entry = (CacheEntryWrapper) super.get(key);

        if (entry == null) {
            return null;
        }

        CacheEntryWrapper prev = entry.prev;
        CacheEntryWrapper next = entry.next;

        if (prev != null) {
            prev.next = next;

            entry.prev = null;
            entry.next = mru;
            mru.prev = entry;
            mru = entry;

            if (next != null)
                next.prev = prev;
            else
                lru = prev;
        }
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            //PM16861
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[UPDATE_CACHE_LIST], "Returning object associated with this key=" + key);
        }
        return entry;
    }

    private static class CacheEntryWrapper {
        CacheEntryWrapper prev;
        CacheEntryWrapper next;
        Object key;
        Object value;

        public String toString() {
            StringBuffer sb = new StringBuffer();
            //sb.append(" ## CacheEntryWrapper ").append(" key: ").append(key).append(" value: ").append(value);
            sb.append(" ## CacheEntryWrapper ").append(" key: ").append(key);//PM45446 - remove printing session object
            return sb.toString();
        }
    }

}
