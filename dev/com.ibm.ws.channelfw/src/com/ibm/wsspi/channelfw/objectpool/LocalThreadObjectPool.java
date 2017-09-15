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
package com.ibm.wsspi.channelfw.objectpool;

import com.ibm.websphere.channelfw.osgi.CHFWBundle;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;

/**
 * this object is now the same as the CircularObjectPool, without sync blocks
 */
public class LocalThreadObjectPool implements ObjectPool {

    // free array is circular queue, where last entry is where the last (most
    // recent)
    // thing was added, and firstEntry is where the first (oldest) thing was added
    private Object[] free = null;
    private ObjectFactory factory = null;
    private ObjectDestroyer destroyer = null;
    private long[] timeFreed = null;

    private int firstEntry = -1; // where the oldest entry is in the queue
    private int lastEntry = -1; // where the newest entry is in the queue

    private final int maxPoolSize;
    private int poolSize;
    private int putCount = 0;
    private int getCount = 0;
    private int underflowCount = 0;
    private int overflowCount = 0;
    private int minElements = 0;
    private final int adjustThreshold = 1000;
    private int minPoolSize;
    private int batchSize;
    private int adjustSize;

    private boolean cleanUpOld = true;

    /**
     * Trace Component
     */
    private static final TraceComponent tc = Tr.register(LocalThreadObjectPool.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);

    /**
     * Creates an object pool.
     * 
     * @param size
     *            Maximum size of the free pool.
     * @param fact
     *            Factory to use to create new poolable objects.
     */
    public LocalThreadObjectPool(int size, ObjectFactory fact) {
        this(size, fact, null);
    }

    /**
     * Creates an object pool with a destroyer
     * 
     * @param size
     *            Maximum size of the free pool.
     * @param fact
     *            Factory to use to create new poolable objects.
     * @param dest
     */
    public LocalThreadObjectPool(int size, ObjectFactory fact, ObjectDestroyer dest) {
        destroyer = dest;
        factory = fact;
        maxPoolSize = size;
        poolSize = maxPoolSize;

        if (maxPoolSize > 10) {
            minPoolSize = maxPoolSize / 5;
            batchSize = minPoolSize / 2;
            if (batchSize < 2) {
                batchSize = minPoolSize;
            }
            adjustSize = minPoolSize / 2;
        } else {
            minPoolSize = maxPoolSize / 2;
            if (minPoolSize == 0) {
                minPoolSize = 1;
            }
            if (minPoolSize >= 3) {
                batchSize = 3;
            } else {
                batchSize = minPoolSize;
            }
            adjustSize = 0;
        }

        free = new Object[poolSize];
        timeFreed = new long[poolSize];

        // ensure these are initialized to null for all JDKs
        for (int i = 0; i < poolSize; i++) {
            free[i] = null;
            timeFreed[i] = 0L;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Object Pool " + this + " created, max size: " + maxPoolSize + ", minPoolSize: " + minPoolSize);
        }
    }

    /**
     * Creates an object pool.
     * 
     * @param size
     *            Maximum size the free pool (pre-allocated objects) can grow to.
     */
    public LocalThreadObjectPool(int size) {
        this(size, null);
    }

    /*
     * @see com.ibm.wsspi.channelfw.objectpool.ObjectPool#get()
     */
    @Override
    public Object get() {

        Object oObject = null;
        getCount++;

        // Check if any are free in the free hashtable
        if (lastEntry > -1) {
            // Free array has entries, get the last one.
            // remove last one for best performance
            oObject = free[lastEntry];
            free[lastEntry] = null;
            if (lastEntry == firstEntry) { // none left in pool
                lastEntry = -1;
                firstEntry = -1;
            } else if (lastEntry > 0) {
                lastEntry = lastEntry - 1;
            } else { // last entry = 0, reset to end of list
                lastEntry = poolSize - 1;
            }
        }
        if (oObject == null)
            underflowCount++;
        if (getCurrentNumElements() < minElements)
            minElements = getCurrentNumElements();
        if ((getCount + putCount) > adjustThreshold)
            adjustPoolSize();
        if (oObject == null && factory != null) {
            oObject = factory.create();
        }
        return oObject;
    }

    /*
     * @see com.ibm.wsspi.channelfw.objectpool.ObjectPool#put(java.lang.Object)
     */
    @Override
    public Object put(Object object) {
        putCount++;
        long currentTime = CHFWBundle.getApproxTime();

        // get next free position, or oldest position if none free
        lastEntry++;
        // If last entry is past end of array, go back to the beginning
        if (lastEntry == poolSize) {
            lastEntry = 0;
        }
        Object returnVal = free[lastEntry]; // get whatever was in that slot for
                                            // return value
        free[lastEntry] = object;
        timeFreed[lastEntry] = currentTime;
        // if we overlaid the first/oldest entry, reset the first entry position
        if (lastEntry == firstEntry) {
            firstEntry++;
            if (firstEntry == poolSize) {
                firstEntry = 0;
            }
        }
        if (firstEntry == -1) { // if pool was empty before, this is first entry now
            firstEntry = lastEntry; // should always be at '0', this may
            // overwrite the oldest entry, in which case
            // the oldest one will be garbage collected
        }
        // if a return value is set, then the pool is full
        // NOTE: We do not call destroy on the local pool because returnVal will
        // be added to the main pool by TwoTier
        if (returnVal != null) {
            overflowCount++;
        }

        // check timestamp of oldest entry, and delete if older than 60 seconds
        // we should do a batch cleanup of all pools based on a timer rather than
        // waiting for a buffer to be released to trigger it, maybe in the next
        // release
        if (cleanUpOld) {
            while (firstEntry != lastEntry && currentTime > (timeFreed[firstEntry] + 60000L)) {
                if (destroyer != null && free[firstEntry] != null) {
                    destroyer.destroy(free[firstEntry]);
                }
                free[firstEntry] = null;
                firstEntry++;
                if (firstEntry == poolSize) {
                    firstEntry = 0;
                }
            }
        }

        if ((getCount + putCount) > adjustThreshold) {
            adjustPoolSize();
        }
        return returnVal;
    }

    /**
     * Set the flag on whether to clean up old, presumably stale entries
     * in the pool.
     * 
     * @param flag
     */
    public void setCleanUpOld(boolean flag) {
        cleanUpOld = flag;
    }

    /**
     * Gets an array of Objects from the pool. This method will return a varying
     * number
     * of elements based on the number of available objects (it will always leave
     * at
     * least 1/2 the minimum pool size) and the batch size configured for the
     * pool.
     * 
     * @return Object[] The arrray of objects removed from the pool.
     */
    protected Object[] getBatch() {

        Object[] objectArray = new Object[batchSize];
        objectArray[0] = get(); // always attempt to get at least 1, get() could
                                // return null
                                // if no factory was defined when instantiated.

        int numElements = getCurrentNumElements();

        for (int i = 1; i <= numElements && i < batchSize; i++) {
            objectArray[i] = get();
        }

        return objectArray;
    }

    /**
     * Puts a set of Objects into the free pool. If the free pool is full
     * then this object will overlay the oldest object in the pool.
     * 
     * @param objectArray
     *            the objects to be put in the pool.
     */
    protected void putBatch(Object[] objectArray) {
        int index = 0;
        while (index < objectArray.length && objectArray[index] != null) {
            put(objectArray[index]);
            index++;
        }
    }

    private int getCurrentNumElements() {
        if (lastEntry == -1) {
            return 0;
        }
        if (lastEntry >= firstEntry) {
            return (lastEntry - firstEntry) + 1;
        }
        return (lastEntry + 1 + (poolSize - firstEntry));
    }

    /**
     * Purge the contents and hand them to the calling pool that
     * owns this threadlocal version.
     * 
     * @return Object[]
     */
    protected Object[] purge() {
        Object[] data = new Object[getCurrentNumElements()];
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "LocalPool is purging " + data.length + " items.  firstEntry: " + firstEntry + " lastEntry: " + lastEntry + " poolSize: " + poolSize);
        }

        if (0 < data.length) {
            int out = 0;
            if (lastEntry >= firstEntry) {
                for (int i = firstEntry; i <= lastEntry; i++) {
                    data[out] = free[i];
                    free[i] = null;
                    out++;
                }
            } else {
                for (int i = firstEntry; i < poolSize; i++) {
                    data[out] = free[i];
                    free[i] = null;
                    out++;
                }
                for (int i = 0; i <= lastEntry; i++) {
                    data[out] = free[i];
                    free[i] = null;
                    out++;
                }
            }

            lastEntry = -1;
            firstEntry = -1;
        }

        return data;
    }

    private void adjustPoolSize() {
        int oldPoolSize = poolSize;

        // if poolSize is small, then don't lower it anymore, plus the integer
        // math starts to create problems.

        // pool is too big if it never misses and always has more than the minimum
        // elements
        if (underflowCount == 0 && poolSize > 10 && minElements > minPoolSize / 2 && poolSize > minPoolSize) {

            if ((poolSize - minPoolSize) > adjustSize * 2) {
                poolSize = (poolSize - minPoolSize) / 2 + minPoolSize;
            } else {
                poolSize = poolSize - adjustSize;
            }
            if (poolSize < minPoolSize) {
                poolSize = minPoolSize;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Object Pool " + this + " reduced, old size: " + oldPoolSize + " new pool size: " + poolSize);
            }
            // adjust pointers to last and first entries, and release buffers in
            // now unused portion of pool
            while (oldPoolSize > poolSize) {
                // release old entries
                if (destroyer != null && free[oldPoolSize - 1] != null) {
                    destroyer.destroy(free[oldPoolSize - 1]);
                }

                free[oldPoolSize - 1] = null;
                // reset any pointers that pointed outside the new pool size boundaries
                if (lastEntry == oldPoolSize - 1)
                    lastEntry--;
                if (firstEntry == oldPoolSize - 1)
                    firstEntry--;
                oldPoolSize--;
            }
        }

        // pool is too small if it underflows and overflows
        else if (underflowCount > 0 && overflowCount > 0 && poolSize < maxPoolSize) {
            poolSize = poolSize + adjustSize;
            if (poolSize > maxPoolSize) {
                poolSize = maxPoolSize;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Object Pool " + this + " enlarged, old size: " + oldPoolSize + " new pool size: " + poolSize);
            }
            // if array has wrapped and first entry is past the last entry, then we
            // need to
            // adjust the pointers to reflect the new space in the 'middle' of the
            // queue
            if (firstEntry > lastEntry) {
                while (oldPoolSize < poolSize) {
                    // if new available element is between the first and last ones, adjust
                    // by moving last element to the new position, and back up lastEntry
                    // pointer
                    if (firstEntry > lastEntry) {
                        if (destroyer != null && free[oldPoolSize - 1] != null) {
                            destroyer.destroy(free[oldPoolSize - 1]);
                        }

                        free[oldPoolSize - 1] = free[lastEntry];
                        free[lastEntry] = null;
                        if (lastEntry > 0) {
                            lastEntry--;
                        } else {
                            lastEntry = oldPoolSize - 1;
                        }
                    }
                    oldPoolSize++;
                }
            }
        }

        // reset all fields
        minElements = getCurrentNumElements();
        getCount = 0;
        putCount = 0;
        underflowCount = 0;
        overflowCount = 0;

    }

}
