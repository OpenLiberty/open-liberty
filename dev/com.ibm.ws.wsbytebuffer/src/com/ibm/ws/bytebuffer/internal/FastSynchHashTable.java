/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.bytebuffer.internal;

import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * FastSyncHashTable.
 * 
 * This is a table implementation that does very little besides basic table operations.
 * In making it fast, it does not maintain counts of items in the table, cannot grow or change in size once instantiated,
 * and therefore has no global tablewide locks. This locks at the bucket level, providing for smaller granularity of
 * of locking and therefore faster access of data. The main table is a 2-d array allowing for some variation. Both the
 * hashCode() and equals() methods are used to locate and compare keys to find entries in the table.
 */

public class FastSynchHashTable {
    protected int xVar = 1000;
    private static final int yVar = 3;
    protected FastSyncHashBucket[][] mainTable;

    private static final TraceComponent tc = Tr.register(FastSynchHashTable.class,
                                                         MessageConstants.WSBB_TRACE_NAME,
                                                         MessageConstants.WSBB_BUNDLE);

    /**
     * Constructor.
     * 
     * @param buckets
     */
    public FastSynchHashTable(int buckets) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "Created FastSyncHashTable(int): " + buckets);
        xVar = buckets / yVar + 1;
        initBuckets();
    }

    /**
     * Constructor.
     */
    public FastSynchHashTable() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            Tr.event(tc, "Created FastSyncHashTable()");
        initBuckets();
    }

    private void initBuckets() {
        mainTable = new FastSyncHashBucket[xVar][yVar];
        for (int i = 0; i < xVar; i++) {
            for (int j = 0; j < yVar; j++) {
                mainTable[i][j] = new FastSyncHashBucket();
            }
        }
    }

    /**
     * Get an object from the table.
     * 
     * @param key
     *            an Object
     * @return value in the table or null if does not exist.
     */
    public Object get(int key) {
        return syncGetValueFromBucket(getBucket(key), key, false);
    }

    /**
     * Internal get from the table which is partially synchronized at the hash
     * bucket level.
     * 
     * @param hb
     *            a hash bucket to go to.
     * @param key
     *            the key to retrieve
     * @param remove
     *            whether to remove it from the table or not
     * @return value in the table
     */
    private Object syncGetValueFromBucket(FastSyncHashBucket hb, int key, boolean remove) {
        FastSyncHashEntry e = null;
        FastSyncHashEntry last = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "syncGetValueFromBucket: key, remove " + key + " " + remove);
        }
        synchronized (hb) {
            e = hb.root;
            while (e != null) {
                if (e.key == key) {
                    if (remove) {
                        if (last == null) {
                            // this is root
                            hb.root = e.next;
                        } else {
                            last.next = e.next;
                        }
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "syncGetValueFromBucket: found value in bucket");
                    }
                    return e.value;
                }
                last = e;
                e = e.next;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "syncGetValueFromBucket-returned null");
        }
        return null;
    }

    /**
     * Put into the table if does not exist.
     * 
     * @param key
     *            Object to identify the value by
     * @param value
     *            Object to store in the table
     * @return value of the object in the cache
     */
    public Object put(int key, Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "put");
        }
        if (value == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "value == null");
            }
            throw new NullPointerException("Missing value");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "key " + key);
        }
        // check the table
        FastSyncHashBucket bucket = getBucket(key);
        Object retVal = syncGetValueFromBucket(bucket, key, false);
        if (retVal != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "put: key is already defined in bucket...returning value from bucket and new key value will be discarded.");
            return retVal;
        }
        // new entry
        FastSyncHashEntry e = new FastSyncHashEntry(key, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "put: put new key and value into bucket");
        }
        return syncPutIntoBucket(bucket, e);
    }

    /**
     * forcePut.
     * 
     * Put in the table whether it exists or not. First removes if in the table.
     * Does not hold synchronization throughout...which means a window is open for
     * a get to miss.
     * 
     * @param key
     *            Object to identify the value by
     * @param value
     *            Object to store in the table
     * @return value of the object in the cache
     */
    public Object forcePut(int key, Object value) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "forcePut(int,Object):");
        }
        if (value == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "value is null");
            }
            throw new NullPointerException("Missing value");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "key: " + key);
        }
        // check the table removing if it exists
        FastSyncHashBucket bucket = getBucket(key);
        syncGetValueFromBucket(bucket, key, true);

        // new entry
        FastSyncHashEntry e = new FastSyncHashEntry(key, value);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "forcePut:  put new hash and key in bucket");
        }
        return syncPutIntoBucket(bucket, e);
    }

    /**
     * Calculate the correct bucket for this object.
     * 
     * @param hc
     *            integer hash code
     * @return a FastSyncHashBucket that stores these items
     */
    private FastSyncHashBucket getBucket(int hc) {
        return mainTable[(hc & 0x7FFFFFFF) % xVar][(hc & 0x7FFFFFFF) % yVar];
    }

    /**
     * Put synchronized into bucket.
     * 
     * Puts an object into the bucket and is synchronized.
     * 
     * @param hb
     *            a FastSyncHashBucket
     * @param newEntry
     *            a FastSyncHashEntry to place in the Bucket
     * @return the value in the table
     */
    private Object syncPutIntoBucket(FastSyncHashBucket hb, FastSyncHashEntry newEntry) {
        FastSyncHashEntry e = null;
        FastSyncHashEntry last = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "syncPutIntoBucket");
        }
        synchronized (hb) {
            e = hb.root;

            // already in there?
            while (e != null) {
                if (e.key == newEntry.key) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "syncPutIntoBucket: key/hash pair is already in the bucket");
                    }
                    return e.value;
                }
                last = e;
                e = e.next;
            }

            if (last == null) {
                if (hb.root == null) {
                    // this is root
                    hb.root = newEntry;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                        Tr.exit(tc, "syncPutIntoBucket: Adding new entry at the beginning (root)");
                    }
                    return newEntry.value;
                }
                last = hb.root;
                while (last.next != null) {
                    last = last.next;
                }
            }
            last.next = newEntry;
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
                Tr.exit(tc, "syncPutIntoBucket: Adding new entry at the end");
            }
            return newEntry.value;
        } // end-sync
    }

    /**
     * remove and retrieve the object from the table.
     * 
     * Returns null if the object was not found.
     * 
     * @param key
     *            the object used to identify this value
     * @return the object in the table
     */
    public Object remove(int key) {
        return syncGetValueFromBucket(getBucket(key), key, true);
    }

    /**
     * This routine returns a snapshot of all the values in the hash table.
     * 
     * CAUTION: The entries returned may no longer be valid, so code that
     * processes
     * of the return values should be aware of this. The main reason for
     * this function is to provide a way of dumping the table for diagnostic
     * purposes.
     * 
     * @return an array of all the values in the hash table.
     */
    public Object[] getAllEntryValues() {
        List<Object> values = new ArrayList<Object>();
        FastSyncHashBucket hb = null;
        FastSyncHashEntry e = null;

        for (int i = 0; i < xVar; i++) {
            for (int j = 0; j < yVar; j++) {
                hb = mainTable[i][j];
                synchronized (hb) {
                    e = hb.root;
                    while (e != null) {
                        values.add(e.value);
                        e = e.next;
                    }
                }
            }
        }

        return values.toArray();
    }

}

class FastSyncHashBucket {
    FastSyncHashEntry root;
}

class FastSyncHashEntry {
    int key;
    Object value;
    FastSyncHashEntry next;

    /**
     * Constructor.
     * 
     * @param key
     * @param value
     */
    public FastSyncHashEntry(int key, Object value) {
        this.key = key;
        this.value = value;
        this.next = null;
    }
}
