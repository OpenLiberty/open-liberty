/*******************************************************************************
 * Copyright (c) 2001, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.rsadapter.impl;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.rsadapter.AdapterUtil; 

import java.text.DecimalFormat;
import java.util.LinkedList; 

/**
 * <p>An Adapter-specific, minimal implementation of a bounded hash map. This implementation
 * differs from the java.util.HashMap implementation in several ways,</p>
 * 
 * <ul>
 * <li>Allows keys to map to multiple values simultaneously. When a remove is done, only one
 * of the values is removed.</li>
 * <li>Enforces a maximum size for the map. When the maximum size is reached and an add is
 * performed, an entry from the bucket least recently used is discarded.</li>
 * <li>The hash function is simply the hash code modulus the maximum number of entries.</li>
 * <li>Methods are NOT synchronized. (add, remove, removeAll)</li>
 * </ul>
 */
public class CacheMap {
    private static TraceComponent tc = Tr.register(CacheMap.class, AdapterUtil.TRACE_GROUP, AdapterUtil.NLS_FILE); 

    /** Upper limit on the number of entries in the CacheMap. */
    private final int maxEntries;

    /** Current number of entries in the CacheMap. */
    private int numEntries;

    /** Number of buckets in the CacheMap. */
    private final int numBuckets;

    /** Maximum bucket size for buckets in the CacheMap. [d178082] */
    private static final int maxBucketSize = 5;

    /** A counter of entries discarded from the cache to make room for new entries. */
    private int numDiscards;

    /** The values stored in each bucket of the CacheMap. Bucket index is zero-based. */
    private final Object[][] values;

    /** The keys stored in each bucket of the CacheMap. Bucket index is zero-based. */
    private final Object[][] keys;

    /** List containing the size of each bucket. */
    private final int[] bucketSizes;

    /** List of indices (zero-based) of previous buckets, as ordered for MRU/LRU. */
    private final int[] previous;

    /** List of indices (zero-based) of next buckets, as ordered for MRU/LRU. */
    private final int[] next;

    /** Index of the bucket with its "next" pointer pointing to the LRU bucket. */
    private final int BEFORE_LRU;

    /** Index of the bucket with its "previous" pointing to the MRU bucket. */
    private final int AFTER_MRU;

    /**
     * Create a new CacheMap with a maximum number of entries. When the CacheMap grows beyond
     * the maximum number of entries, an entry from the least recently used bucket is
     * discarded.
     * 
     * @param maxSize the maximum number of entries which may be stored in the CacheMap.
     */
    public CacheMap(int maxSize) {
        maxEntries = maxSize;

        // The "next" and "previous" lists reserve the index after the last bucket index for
        // the pointer to the LRU bucket and the following index for the pointer to the MRU
        // bucket.

        BEFORE_LRU = numBuckets = (maxEntries * 4 + 1) / 3; 
        AFTER_MRU = numBuckets + 1;

        int numPointers = numBuckets + 2;
        previous = new int[numPointers];
        next = new int[numPointers];

        // Buckets are split into two separate arrays for 'keys' and 'values'.  The first
        // array index indicates the bucket.  The second index indicates the entry within the
        // bucket.

        values = new Object[numBuckets][maxBucketSize]; 
        keys = new Object[numBuckets][maxBucketSize]; 
        bucketSizes = new int[numBuckets];

        for (int i = 0; i < numBuckets; i++) {
            // Unused bucket pointers should point to the bucket before the LRU bucket.
            next[i] = previous[i] = BEFORE_LRU;
        }

        // The newly initialized cache map has no MRU or LRU buckets.
        next[BEFORE_LRU] = AFTER_MRU;
        previous[AFTER_MRU] = BEFORE_LRU;
    }

    /**
     * Add an entry to the map consisting of the specified (key, value) pair. A key is allowed
     * to simultaneously map to multiple values. If the maximum number of entries for the
     * CacheMap is exceeded, an entry is discarded.
     * 
     * @param key the key.
     * @param value the value.
     * 
     * @return the discarded value, or null if none.
     */
    public final Object add(Object key, Object value) {
        int bucketIndex = (key.hashCode() & Integer.MAX_VALUE) % numBuckets;
        int bucketSize = bucketSizes[bucketIndex]++;

        // Discard an entry from the bucket if it's already at the maximum bucket size.

        Object discardedObject = bucketSize == maxBucketSize ?
                        discardFromBucket(bucketIndex, --bucketSize) :
                        null; 

        // Add the new entry.  We might temporarily exceed the maximum entry limit by 1.  In
        // that case we will end up removing an entry before returning.

        values[bucketIndex][bucketSize] = value;
        keys[bucketIndex][bucketSize] = key;

        // Update the MRU/LRU pointer for the current bucket.  This involves removing the
        // current pointer, if it exists, and creating a new MRU/LRU pointer at the MRU end
        // of the list.

        // Remove current pointer, if there is one.

        int n, p;

        if ((n = next[bucketIndex]) != BEFORE_LRU) {
            previous[n] = p = previous[bucketIndex];
            next[p] = n;
        }

        // Create a new MRU/LRU pointer at the MRU end of the list.

        p = previous[AFTER_MRU];
        previous[AFTER_MRU] = next[p] = bucketIndex;

        next[bucketIndex] = AFTER_MRU;
        previous[bucketIndex] = p;

        // If we exceeded the upper limit on entries in the cache map, remove a LRU entry and
        // return it.  Otherwise, if we had to discard an entry to keep from exceeding the
        // maximum bucket size, return that entry.  Otherwise, return null.

        return ++numEntries > maxEntries ? removeLRU() : discardedObject; 
    }

    /**
     * Inserts into this CacheMap all entries from the specified CacheMap,
     * returning a list of any values that do not fit.
     * 
     * @param c the source CacheMap from which to copy entries.
     * @return list of values from the source CacheMap which didn't fit.
     */
    public Object[] addAll(CacheMap c) {
        LinkedList<Object> discards = new LinkedList<Object>();

        Object discard;
        for (int bucketIndex = c.next[c.BEFORE_LRU]; bucketIndex != c.AFTER_MRU; bucketIndex = c.next[bucketIndex])
            for (int i = 0; i < c.bucketSizes[bucketIndex]; i++)
                if ((discard = add(c.keys[bucketIndex][i], c.values[bucketIndex][i])) != null)
                    discards.add(discard);

        return discards.toArray();
    }

    /**
     * Discard an entry from the specified bucket. The actual entry is not nulled out because
     * it will later be overwritten.
     * 
     * @param bucketIndex the index of the bucket from which to discard an entry.
     * @param entryIndex the index of the entry within the bucket to discard.
     * 
     * @return the discarded value.
     * 
     */
    private Object discardFromBucket(int bucketIndex, int entryIndex) {
        numDiscards++;
        bucketSizes[bucketIndex]--;
        numEntries--;
        return values[bucketIndex][entryIndex];
    }

    /**
     * Creates and returns a String representing this CacheMap in a readable format. This
     * method is provided only for tracing and error reporting purposes. It is unsynchronized
     * and not safe to call when other threads may be operating on the CacheMap.
     * 
     * @return nicely formatted text representing this CacheMap.
     */
    public String display() {
        StringBuffer sb = new StringBuffer();
        DecimalFormat f = new java.text.DecimalFormat("'  '000");

        sb.append(AdapterUtil.EOLN).append(this);
        sb.append(AdapterUtil.EOLN).append("Number of entries:   ").append(numEntries);
        sb.append(AdapterUtil.EOLN).append("Maximum entries:     ").append(maxEntries);
        sb.append(AdapterUtil.EOLN).append("Number of buckets:   ").append(numBuckets);
        sb.append(AdapterUtil.EOLN).append("Maximum bucket size: ").append(maxBucketSize); 
        sb.append(AdapterUtil.EOLN).append("Number of discards:  ").append(numDiscards); 

        sb.append(AdapterUtil.EOLN);
        sb.append(AdapterUtil.EOLN).append("BUCKET SIZE PREV NEXT");
        sb.append(AdapterUtil.EOLN); 

        for (int i = 0; i < numBuckets; i++) {
            sb.append(f.format(i));
            sb.append(f.format(bucketSizes[i]));
            sb.append(f.format(previous[i]));
            sb.append(f.format(next[i]));
            sb.append(AdapterUtil.EOLN); 

            for (int j = 0; j < bucketSizes[i]; j++)
                sb.append("                      ").append(Integer.toHexString(values[i][j].hashCode())).append(' ').append(keys[i][j]).append(AdapterUtil.EOLN); 
        }

        sb.append(f.format(BEFORE_LRU)).append("  LRU     ").append(f.format(next[BEFORE_LRU])).append(AdapterUtil.EOLN); 
        sb.append(f.format(AFTER_MRU)).append("  MRU").append(f.format(previous[AFTER_MRU])).append(AdapterUtil.EOLN); 

        return new String(sb);
    }

    /**
     * Returns the maximum number of entries that can be kept in the cache.
     * 
     * @return the maximum number of entries that can be kept in the cache.
     */
    public final int getMaxSize() {
        return maxEntries;
    }

    /**
     * Remove an entry. If the key maps to multiple values, only one is removed.
     * 
     * @param key the key.
     * 
     * @return the value that was removed, or null if none is found.
     */
    public final Object remove(Object key) {
        int bucketIndex = (key.hashCode() & Integer.MAX_VALUE) % numBuckets;
        Object[] bucketKeys = keys[bucketIndex]; 
        int bucketSize = bucketSizes[bucketIndex];

        // Look through the entries in the bucket to find one that matches for removal.

        for (int i = bucketSize - 1; i >= 0; i--) {
            if (bucketKeys[i].equals(key)) {
                // If we emptied the bucket then remove its MRU/LRU entry.

                if ((bucketSizes[bucketIndex] = --bucketSize) == 0) {
                    // Nothing will be left in the bucket, so remove its MRU/LRU entry.
                    int n = next[bucketIndex];
                    int p = previous[n] = previous[bucketIndex];
                    next[p] = n;

                    next[bucketIndex] = previous[bucketIndex] = BEFORE_LRU;
                }

                numEntries--;

                bucketKeys[i] = bucketKeys[bucketSize];
                bucketKeys[bucketSize] = null;

                Object[] bucketValues = values[bucketIndex];
                Object value = bucketValues[i];
                bucketValues[i] = bucketValues[bucketSize];
                bucketValues[bucketSize] = null;

                return value;
            }
        }

        // Entry was not found for removal.

        return null;
    }

    /**
     * Remove all entries in the map.
     * 
     * @return a list of all values that were removed.
     */
    public final Object[] removeAll() {
        Object[] list = new Object[numEntries];
        numEntries = 0;
        Object[] bucketValues;
        Object[] bucketKeys; 

        // Empty all buckets.

        try {
            for (int i = numBuckets, counter = 0; i > 0;) {
                for (int j = bucketSizes[--i]; j > 0;) {
                    bucketValues = values[i];
                    bucketKeys = keys[i];

                    bucketKeys[--j] = null; 

                    list[counter++] = bucketValues[j];
                    bucketValues[j] = null;
                }

                bucketSizes[i] = 0;
            }

            // Clear all MRU/LRU entries for buckets.
            for (int i = 0; i <= AFTER_MRU; i++)
                next[i] = previous[i] = BEFORE_LRU;
        } catch (ArrayIndexOutOfBoundsException ioobX) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) { 
                Tr.debug(this, tc, "ArrayIndexOutOfBoundsException is caught during removeAll() of the cachMap ", this);
                Tr.debug(this, tc, "Possible causes:");
                Tr.debug(this, tc, "multithreaded access of JDBC objects by the Application");
                Tr.debug(this, tc, "Application is closing JDBC objects in a finalize()");
                Tr.debug(this, tc, "Exception is: ", ioobX);
            }

            throw ioobX;
        }

        return list;
    }

    /**
     * Remove an entry from the least recently used bucket. This method requires no explicit
     * synchronization because the calling method (add) is already synchronized.
     * As of  all synchronization was removed.
     * 
     * @return the value that was removed.
     */
    private final Object removeLRU() {
        int bucketIndex = next[BEFORE_LRU];
        Object[] bucketValues = values[bucketIndex];
        Object[] bucketKeys = keys[bucketIndex]; 
        int bucketSize = --bucketSizes[bucketIndex];

        numEntries--;

        // Choose an entry in the LRU bucket to remove.  The numDiscards counter is used to
        // reduce the chance of always choosing the same index.

        int indexToRemove = (numDiscards++ & Integer.MAX_VALUE) % (bucketSize + 1);

        Object value = bucketValues[indexToRemove];

        // If we emptied the bucket then remove its MRU/LRU entry.

        if (bucketSize == 0) {
            int n = next[bucketIndex];
            int p = previous[n] = previous[bucketIndex];
            next[p] = n;

            next[bucketIndex] = BEFORE_LRU;
            //previous[bucketIndex] = BEFORE_LRU; // already true

            // As long as we've already determined the new bucket size is 0, do some optimized
            // bucket clearing.

            bucketValues[0] = null;
            bucketKeys[0] = null;
        }
        else {// We didn't empty the bucket.  Remove the first entry and replace with the last.
            bucketValues[indexToRemove] = bucketValues[bucketSize];
            bucketValues[bucketSize] = null;

            bucketKeys[indexToRemove] = bucketKeys[bucketSize];
            bucketKeys[bucketSize] = null;
        }

        return value;
    }

    /**
     * @return the number of entries in the map.
     */
    public final int size() {
        return numEntries;
    }
}
