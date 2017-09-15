/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.session.store.memory;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import com.ibm.ws.session.utils.LoggingUtil;
import com.ibm.ws.session.utils.SessionHashSet;
import com.ibm.wsspi.session.IStore;

/**
 * This class extends java.util.HashMap. The constuctor expects three arguments.
 * The first argument is the store to which this hashmap will belong, the second is
 * the maximum number of entries (maxCapacity) for this Map. The
 * Map will never exceed maxCapacity and hence no rehash will ever occur for the
 * SessionSimpleHashMap. The second argument denotes whether overflow is allowed.
 * Once the SessionSimpleHashMap is full, if overflow is disabled a TooManySessionsException
 * will be thrown back to the caller. However, if overflow is enabled a new Overflow Hashmap
 * will be created and all new put() requests will be placed in the overflow Hashmap.
 * 
 * Since Hashmap is totally unsynchronized, this class will provide the needed
 * synchronization for a threaded environment. There is no synchronization on get() for
 * the SessionSimpleHashMap though gets will be synchronized when the overflow Hashmap
 * is used (since the HashMap may rehash).
 * 
 * The keysSet() method will return a non "Fail-fast" version of the keys that
 * are a snapshot at the time of the method call.
 * 
 * @see java.util.HashMap
 */

public class SessionSimpleHashMap extends HashMap {

    private static class SerializableObject implements Serializable {
        private static final long serialVersionUID = -1713215368202372876L;
    }
    
    private IStore _iStore;
    private int maxSize;
    private int currentSize;
    private HashMap OverflowTabl;
    private Serializable OverflowTablLock = new SerializableObject();
    boolean overflowAllowed;
    private static final long serialVersionUID = 6091018332887652886L;
    private static final String methodClassName = "SessionSimpleHashMap";
    private String appNameForLogging = "";

    private static final int GET = 0;
    private static final int REMOVE = 1;
    private static final int KEYSET = 2;
    private static final int PUT = 3;
    private static final String methodNames[] = { "get", "remove", "keySet", "put" };

    /*
     * The Constructor
     */
    public SessionSimpleHashMap(IStore iStore, int maxCapacity, boolean overflow) {
        // Need to play with load factor and initial capacity so that no rehash ever occurs
        super(maxCapacity + 20, 1);
        currentSize = 0;
        maxSize = maxCapacity;
        overflowAllowed = overflow;
        _iStore = iStore;
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            appNameForLogging = " AppName=" + _iStore.getId();
            String s = "capacity is: " + maxSize + " overflow is: " + overflowAllowed + appNameForLogging;
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodClassName, s);
        }
    }

    /*
     * This method puts an entry into the HashMap. It does follow HashMap semantics by checking for
     * an existing entry and returning that entry when we replace it. However, the session component
     * ensures there is not an existing entry prior to calling put, so we don't expect to ever get
     * a non-null value back.
     * 
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public synchronized Object put(Object key, Object value) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            //PM16861
            StringBuffer sb = new StringBuffer("{").append(key).append("} ").append(appNameForLogging);
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[PUT], sb.toString());
        }
        Object replacedEntry = null;

        // First see if replacing an existing entry 
        Object currEntry = super.get(key);

        if (currEntry != null) {
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[PUT], "replacing existing entry");
            }
            replacedEntry = super.put(key, value);
        } else {
            if ((overflowAllowed) && (OverflowTabl != null)) {
                synchronized (OverflowTablLock) {
                    currEntry = OverflowTabl.get(key);
                    if (currEntry != null) {
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[PUT], "replacing existing entry in overflow Hashmap");
                        }
                        replacedEntry = OverflowTabl.put(key, value);
                    }
                }
            }
        }

        // Handle new entries
        if (currEntry == null) {
            currentSize++;
            // increment pmi counter
            if (_iStore.getStoreCallback() != null) {
                _iStore.getStoreCallback().sessionLiveCountInc(value);
            }

            if (currentSize <= maxSize) {
                replacedEntry = super.put(key, value);
                if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[PUT], "add new entry to Hashmap");
                }
            } else { // overflow
                currentSize--;
                if (overflowAllowed) {
                    synchronized (OverflowTablLock) {
                        if (OverflowTabl == null) {
                            OverflowTabl = new HashMap(currentSize, 1);
                            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[PUT], "Creating Overflow Table");
                            }
                        }

                        replacedEntry = OverflowTabl.put(key, value);
                        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[PUT], "add new entry to overflow Hashmap");
                        }
                    }
                } else
                    throw new TooManySessionsException(); // no overflow allowed
            }
        }

        return replacedEntry;
    }

    /*
     * This method returns the entry from the hashmap associated with the given key.
     * If the object is not found in the original hashmap we will search the overflow
     * hashmap if applicable.
     * 
     * @see java.util.Map#get(java.lang.Object)
     */
    @Override
    public Object get(Object key) {
        final boolean isTraceOn = com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled();
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            String s = key + appNameForLogging;
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[GET], s);
        }
        Object currEntry = super.get(key);

        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            if (currEntry != null) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[GET], "key found in HashMap");
            }
        }

        if ((currEntry == null) && (overflowAllowed) && (OverflowTabl != null)) {
            synchronized (OverflowTablLock) {
                currEntry = OverflowTabl.get(key);
                if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                    if (currEntry != null) {
                        LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[GET], "key found in overflow HashMap");
                    }
                }
            }
        }
        if (isTraceOn && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            //We already logged if the key was found ... only log if it wasn't found
            if (currEntry == null) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[GET], "key not found");
            }
        }
        return currEntry;
    }

    @Override
    public synchronized Object remove(Object key) {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINER)) {
            String s = key + appNameForLogging;
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[REMOVE], s);
        }

        Object removedEntry = super.remove(key);

        if ((removedEntry != null)) {
            currentSize--;
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[REMOVE], "key removed from HashMap");
            }
        } else {
            // Must have returned null
            if ((overflowAllowed) && (OverflowTabl != null)) {
                synchronized (OverflowTablLock) {
                    removedEntry = OverflowTabl.remove(key);
                }
            }
            if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
                String message = (removedEntry != null ? "key removed from overflow HashMap" : "key not found");
                LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[REMOVE], message);
            }
        }
        return removedEntry;
    }

    @Override
    public synchronized void clear() {
        super.clear();
        currentSize = 0;
        if (OverflowTabl != null) {
            synchronized (OverflowTablLock) {
                OverflowTabl.clear();
            }
            OverflowTabl = null;
        }
    }

    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set keySet() {
        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.entering(methodClassName, methodNames[KEYSET], appNameForLogging);
        }

        Set myKeySet;
        Object[] keys;
        int keysLen = 0;
        Object[] allKeys = null;

        Set overflowKeySet;
        Object[] overflowKeys = null;
        int overflowKeysLen = 0;

        // Get the "fail-fast keyset from the HashMaps and convert it to an array 
        myKeySet = super.keySet();
        synchronized (this) {
            keys = myKeySet.toArray(); // base Map
            keysLen = keys.length;
            if (OverflowTabl != null) {
                overflowKeySet = OverflowTabl.keySet();
                synchronized (OverflowTablLock) {
                    overflowKeys = overflowKeySet.toArray(); // overflow Map
                    overflowKeysLen = overflowKeys.length;
                }
            }
        }

        int allKeysLen = keysLen + overflowKeysLen;

        // This should be a null set 

        if (allKeysLen != 0) {
            allKeys = new Object[allKeysLen];

            if (keysLen != 0) {
                System.arraycopy(keys, 0, allKeys, 0, keysLen);
            }

            if (overflowKeysLen != 0) {
                System.arraycopy(overflowKeys, 0, allKeys, keysLen, overflowKeysLen);
            }
        }

        // get a non-fail-fast Set 
        Set keySet = new SessionHashSet(allKeys);

        if (com.ibm.ejs.ras.TraceComponent.isAnyTracingEnabled() && LoggingUtil.SESSION_LOGGER_CORE.isLoggable(Level.FINE)) {
            LoggingUtil.SESSION_LOGGER_CORE.exiting(methodClassName, methodNames[KEYSET], keySet);
        }
        return keySet;
    }

    public Object accessEntry(Object key) {
        return get(key);
    }

    //We want to make sure we don't override a value ... must use putNoReplace
    public Object insertEntry(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    public int getOverflowSize() {
        int overflowSize = 0;
        if (OverflowTabl != null) {
            overflowSize = OverflowTabl.size();
        }
        return overflowSize;
    }
}
