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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.wsspi.channelfw.objectpool.ObjectDestroyer;
import com.ibm.wsspi.channelfw.objectpool.ObjectFactory;
import com.ibm.wsspi.channelfw.objectpool.TwoTierObjectPool;

/**
 * A Pool of WsByteBuffers. The size of the entries and the pool depth are
 * configurable.
 */
public class WsByteBufferPool {
    private int intEntrySize;
    private int globalPoolSize;
    private int localThreadPoolSize;
    private TwoTierObjectPool pool = null;
    private WsByteBufferFactory wsbbFactory = null;

    int intUniqueCounter = 0;

    private static final TraceComponent tc = Tr.register(WsByteBufferPool.class,
                                                         MessageConstants.WSBB_TRACE_NAME,
                                                         MessageConstants.WSBB_BUNDLE);

    /**
     * Create the pool and obtain the values for the size of the pool
     * entries and the pool depth.
     * 
     * @param entrySizeIn
     * @param _localPoolSize
     * @param _globalPoolSize
     * @param tracking
     * @param isDirectPool
     * @param cleanUpOld
     */
    public WsByteBufferPool(int entrySizeIn, int _localPoolSize, int _globalPoolSize, boolean tracking, boolean isDirectPool, boolean cleanUpOld) { // @427758C

        this.intEntrySize = entrySizeIn;
        this.globalPoolSize = _globalPoolSize;
        this.localThreadPoolSize = _localPoolSize;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
            Tr.event(tc, "Creating : " +
                         this.toString() +
                         " direct=" + isDirectPool +
                         " backing pool size: " + this.globalPoolSize +
                         " local thread pool size: " + this.localThreadPoolSize);
        }
        this.wsbbFactory = new WsByteBufferFactory();

        if (isDirectPool) {
            this.pool = new TwoTierObjectPool(localThreadPoolSize, globalPoolSize, wsbbFactory, wsbbFactory, tracking);
        } else {
            this.pool = new TwoTierObjectPool(localThreadPoolSize, globalPoolSize, wsbbFactory, null, tracking);
        }

        if (!cleanUpOld) {
            this.pool.doNotCleanUpOld();
        }
    }

    /**
     * inner class for creating the object which will be pooled.
     */
    public class WsByteBufferFactory implements ObjectFactory, ObjectDestroyer {
        private WsByteBufferPoolManagerImpl wsbbPoolManager = null;

        public Object create() {
            int intUniqueId;

            PooledWsByteBufferImpl pooledWSBB = new PooledWsByteBufferImpl();

            // The factory has the responsibility of supplying the unique identifier,
            // so set it into the WsByteBuffer at this time.
            synchronized (this) {
                intUniqueId = intUniqueCounter++;
                if (intUniqueCounter == -1) {
                    // if counter has rolled over to -1, then increment it because
                    // -1 is the ID of all WsByteBuffers which have been created
                    // with the wrap method
                    intUniqueCounter++;
                }

            }
            pooledWSBB.setID(Integer.valueOf(intUniqueId));
            return pooledWSBB;
        }

        public void destroy(Object obj) {

            // If we try to set this on the initialization of the class,
            // we end up in an infinite loop calling WsByteBufferPoolManager.getRef
            if (this.wsbbPoolManager == null) {
                this.wsbbPoolManager = (WsByteBufferPoolManagerImpl) WsByteBufferPoolManagerImpl.getRef();
            }
            if (this.wsbbPoolManager != null) {
                this.wsbbPoolManager.releasing(((WsByteBufferImpl) obj).oByteBuffer);
            }
        }

    }

    /**
     * Return a buffer from the pool, or allocate a new buffer is the pool
     * is full.
     * 
     * @return PooledWsByteBufferImpl
     */
    public PooledWsByteBufferImpl getEntry() {
        return (PooledWsByteBufferImpl) this.pool.get();
    }

    /**
     * Return a buffer to the pool or free the buffer to be garbage
     * collected if the pool is full.
     * 
     * @param buffer to be released.
     * @param entryID
     */
    public void release(Object buffer, Object entryID) {
        this.pool.put(buffer);
    }

    /**
     * Return the inUse table.
     * 
     * @return Object[] an array of Objects representing the inUse table
     */
    public Object[] getInUse() {
        return (this.pool.getInUseTable());
    }

    /**
     * Remove a buffer from the InUse pool. To be used when the buffer
     * should be removed without waiting for the release logic to remove it.
     * 
     * @param buffer to be released.
     */
    public void removeFromInUse(Object buffer) {
        this.pool.removeFromInUse(buffer);
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

    /**
     * Caller is requesting that this pool purge it's thread local
     * information back to the main group pool.
     * 
     */
    public void purgeThreadLocal() {
        this.pool.purgeThreadLocal();
    }
}
