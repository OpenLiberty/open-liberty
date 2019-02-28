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

import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.kernel.service.util.ConcurrentObjectPool;

/**
 * A Pool of WsByteBuffers. The size of the entries and the pool depth are
 * configurable.
 */
public class WsByteBufferPool {
    private final int intEntrySize;
    private final int globalPoolSize;
    private final boolean isDirectPool;
    private final ConcurrentObjectPool<PooledWsByteBufferImpl> pool;

    /**
     * When {@link #inUseTracking} is enabled, this table records poolable objects that were previously created, but
     * are currently not in the pool. An entry is placed in this table for a {@link #get()} and removed for a
     * {@link #put(Object)}. An entry's key and value are identical, and are the poolable object itself.
     */
    private final Hashtable<PooledWsByteBufferImpl, PooledWsByteBufferImpl> inUseTable;

    private final AtomicInteger intUniqueCounter = new AtomicInteger(0);

    private static final TraceComponent tc = Tr.register(WsByteBufferPool.class,
                                                         MessageConstants.WSBB_TRACE_NAME,
                                                         MessageConstants.WSBB_BUNDLE);

    /**
     * Create the pool and obtain the values for the size of the pool
     * entries and the pool depth.
     *
     * @param entrySizeIn
     * @param _globalPoolSize
     * @param tracking
     * @param isDirectPool
     */
    public WsByteBufferPool(int entrySizeIn, int _globalPoolSize, boolean tracking, boolean isDirectPool) { // @427758C

        this.intEntrySize = entrySizeIn;
        this.globalPoolSize = _globalPoolSize;
        this.isDirectPool = isDirectPool;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Creating : " +
                         this.toString() +
                         " direct=" + isDirectPool +
                         " backing pool size: " + this.globalPoolSize);
        }

        this.pool = new ConcurrentObjectPool<>(globalPoolSize);
        this.inUseTable = tracking ? new Hashtable<PooledWsByteBufferImpl, PooledWsByteBufferImpl>(globalPoolSize * 2) : null;
    }

    private WsByteBufferPoolManagerImpl wsbbPoolManager = null;

    public PooledWsByteBufferImpl create() {
        // The factory has the responsibility of supplying the unique identifier,
        // so set it into the WsByteBuffer at this time.
        int intUniqueId = intUniqueCounter.getAndIncrement();
        if (intUniqueId == -1) {
            // if counter has rolled over to -1, then increment it because
            // -1 is the ID of all WsByteBuffers which have been created
            // with the wrap method
            intUniqueId = intUniqueCounter.getAndIncrement();
        }

        return new PooledWsByteBufferImpl(Integer.valueOf(intUniqueId));
    }

    public void destroy(PooledWsByteBufferImpl obj) {

        // If we try to set this on the initialization of the class,
        // we end up in an infinite loop calling WsByteBufferPoolManager.getRef
        if (this.wsbbPoolManager == null) {
            this.wsbbPoolManager = (WsByteBufferPoolManagerImpl) WsByteBufferPoolManagerImpl.getRef();
        }
        if (this.wsbbPoolManager != null) {
            this.wsbbPoolManager.releasing(((WsByteBufferImpl) obj).oByteBuffer);
        }
    }

    /**
     * Return a buffer from the pool, or allocate a new buffer is the pool
     * is full.
     *
     * @return PooledWsByteBufferImpl
     */
    public PooledWsByteBufferImpl getEntry() {
        PooledWsByteBufferImpl returnValue = pool.get();
        if (returnValue == null) {
            returnValue = create();
        }
        if (inUseTable != null) {
            inUseTable.put(returnValue, returnValue);
        }
        return returnValue;
    }

    /**
     * Return a buffer to the pool or free the buffer to be garbage
     * collected if the pool is full.
     *
     * @param buffer to be released.
     * @param entryID
     */
    public void release(PooledWsByteBufferImpl buffer) {
        if (inUseTable != null) {
            inUseTable.remove(buffer);
        }
        boolean pooled = pool.put(buffer);
        if (isDirectPool && !pooled) {
            destroy(buffer);
        }
    }

    /**
     * Return the inUse table.
     *
     * @return Object[] an array of Objects representing the inUse table
     */
    @SuppressWarnings("unchecked")
    public Object[] getInUse() {
        return inUseTable != null ? (((Hashtable<PooledWsByteBufferImpl, PooledWsByteBufferImpl>) inUseTable.clone()).keySet().toArray()) : new Object[0];
    }

    /**
     * Remove a buffer from the InUse pool. To be used when the buffer
     * should be removed without waiting for the release logic to remove it.
     *
     * @param buffer to be released.
     */
    public void removeFromInUse(Object buffer) {
        if (inUseTable != null) {
            if (null == buffer) {
                throw new NullPointerException();
            }
            inUseTable.remove(buffer);
        }
    }

    /**
     * Return a customized toString.
     *
     * @return String
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(64);
        sb.append('[');
        sb.append(getClass().getSimpleName());
        sb.append('@');
        sb.append(Integer.toHexString(hashCode()));
        sb.append('/').append(this.intEntrySize);
        sb.append(']');
        return sb.toString();
    }
}
