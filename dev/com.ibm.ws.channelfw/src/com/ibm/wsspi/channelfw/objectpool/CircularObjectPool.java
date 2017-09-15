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

/**
 * Supplies basic object pooling functionality. This class can be wrapped by
 * more specific pooling code.
 */
public class CircularObjectPool implements ObjectPool {

    // free array is circular queue, where last entry is where the last (most
    // recent)
    // thing was added, and firstEntry is where the first (oldest) thing was added
    private Object[] free = null;
    private ObjectFactory factory = null;
    private ObjectDestroyer destroyer = null;
    private long[] timeFreed = null;

    private int firstEntry = -1; // where the oldest entry is in the queue
    private int lastEntry = -1; // where the newest entry is in the queue

    private final int poolSize;
    private static int minPoolSize = 20;
    private static int batchSize = minPoolSize / 2;

    private boolean cleanUpOld = true;

    /**
     * Creates an object pool.
     * 
     * @param size
     *            Maximum size the free pool (pre-allocated objects) can grow to.
     */
    public CircularObjectPool(int size) {
        this(size, null, null);
    }

    /**
     * Creates an object pool.
     * 
     * @param size
     *            Maximum size of the free pool.
     * @param fact
     *            Factory to use to create new poolable objects.
     */
    public CircularObjectPool(int size, ObjectFactory fact) {
        this(size, fact, null);
    }

    /**
     * Creates an object pool.
     * 
     * @param size
     *            Maximum size of the free pool.
     * @param fact
     *            Factory to use to create new poolable objects.
     * @param dest
     *            optional destroyer to use when discarding objects
     */
    public CircularObjectPool(int size, ObjectFactory fact, ObjectDestroyer dest) {
        this.factory = fact;
        this.destroyer = dest;
        this.poolSize = size;
        this.free = new Object[poolSize];
        this.timeFreed = new long[poolSize];
        for (int i = 0; i < poolSize; i++) {
            this.free[i] = null;
            this.timeFreed[i] = 0L;
        }
    }

    /**
     * Gets an Object from the pool and returns it. If there are
     * currently no entries in the pool then a new object will be
     * created.
     * 
     * @return Object from the pool
     */
    @Override
    public Object get() {

        Object oObject = null;

        synchronized (this) {
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
        }
        if (oObject == null && factory != null) {
            oObject = factory.create();
        }

        return oObject;
    }

    /**
     * Puts an Object into the free pool. If the free pool is full
     * then this object will overlay the oldest object in the pool.
     * 
     * @param object
     *            to be put in the pool.
     */
    @Override
    public Object put(Object object) {
        Object returnVal = null;
        long currentTime = CHFWBundle.getApproxTime();

        synchronized (this) {
            // get next free position, or oldest position if none free
            lastEntry++;
            // If last entry is past end of array, go back to the beginning
            if (lastEntry == poolSize) {
                lastEntry = 0;
            }
            returnVal = free[lastEntry]; // get whatever was in that slot for return
                                         // value
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

            if (returnVal != null && destroyer != null) { // @PK36998A
                destroyer.destroy(returnVal);
            }

            // check timestamp of oldest entry, and delete if older than 60 seconds
            // we should do a batch cleanup of all pools based on a timer rather than
            // waiting for a buffer to be released to trigger it, maybe in the next
            // release
            if (cleanUpOld) {
                while (firstEntry != lastEntry) {
                    if (currentTime > (timeFreed[firstEntry] + 60000L)) {
                        if (destroyer != null && free[firstEntry] != null) { // @PK36998A
                            destroyer.destroy(free[firstEntry]);
                        }
                        free[firstEntry] = null;
                        firstEntry++;
                        if (firstEntry == poolSize) {
                            firstEntry = 0;
                        }
                    } else
                        break;
                }
            }
        }
        return returnVal;
    }

    /**
     * Set the flag on whether to clean up entries that have been idle in the pool
     * for a set period of time.
     * 
     * @param flag
     */
    public void setCleanUpOld(boolean flag) {
        this.cleanUpOld = flag;
    }

    /**
     * Gets an array of Objects from the pool. This method will return a varying
     * number of elements
     * based on the number of available (it will always leave at least 1/2 the
     * minimum pool size) and
     * the batch size configured for the pool.
     * 
     * @return Object[] The array of objects removed from the pool
     */
    protected Object[] getBatch() {

        Object[] objectArray = new Object[batchSize];
        int index = 1;
        synchronized (this) {
            objectArray[0] = get(); // always get at least 1
            while (getCurrentNumElements() > minPoolSize / 2 && index < batchSize) {
                objectArray[index] = get();
                index++;
            }
        }
        return objectArray;
    }

    /**
     * Puts a set of Objects into the free pool. If the free pool is full
     * then this object will overlay the oldest object in the pool.
     * 
     * @param objectArray
     *            list of objects to put into the pool
     */
    protected void putBatch(Object[] objectArray) {

        int index = 0;
        synchronized (this) {
            while (index < objectArray.length && objectArray[index] != null) {
                put(objectArray[index]);
                index++;
            }
        }
        return;
    }

    private int getCurrentNumElements() {
        if (lastEntry == -1)
            return 0;
        if (lastEntry >= firstEntry) {
            return (lastEntry - firstEntry) + 1;
        }
        return (lastEntry + 1 + (poolSize - firstEntry));
    }

}
