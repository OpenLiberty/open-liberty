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

import java.util.Hashtable;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.channelfw.internal.ChannelFrameworkConstants;

/**
 * A two-tiered object pool uses a global shared pool along with threadlocal
 * level pools for performance. Batchs can be pulled from or pushed into the
 * global pool from the thread level ones.
 */
public class TwoTierObjectPool implements ObjectPool {
    // size of thread pools for this instance
    private int threadPoolSize;
    // thread locals reference
    private ThreadLocal<LocalThreadObjectPool> threadLocals = null;
    // main pool
    private CircularObjectPool mainPool = null;
    // factory to creaet new objects for this pool
    private ObjectFactory factory;
    // tracking table
    private Hashtable<Object, Object> inUseTable = null;
    // tracking on/off
    private boolean inUseTracking;

    private boolean cleanUpOld = true;

    // factory to destroy objects for this pool
    // Used when we allocate direct ByteBuffers from a native DLL, like on z/OS
    private ObjectDestroyer destroyer;

    /**
     * Trace Component
     */
    private static final TraceComponent tc = Tr.register(TwoTierObjectPool.class, ChannelFrameworkConstants.BASE_TRACE_NAME, ChannelFrameworkConstants.BASE_BUNDLE);

    /**
     * Construct a two tier pool with one tier using the LocalThreadObjectPool
     * and another tier using the CircularObjectPool.
     * 
     * All threads accessing this pool will have a LocalThreadObjectPool
     * automatically created.
     * 
     * @param _threadPoolSize
     * @param _mainPoolSize
     * @param _factory
     * @param _inUseTracking
     */
    public TwoTierObjectPool(int _threadPoolSize, int _mainPoolSize, ObjectFactory _factory, boolean _inUseTracking) {
        this.threadPoolSize = _threadPoolSize;
        this.factory = _factory;
        this.inUseTracking = _inUseTracking;
        if (_threadPoolSize > 0) {
            this.threadLocals = new ThreadLocal<LocalThreadObjectPool>();
        }

        if (_mainPoolSize > 0) {
            this.mainPool = new CircularObjectPool(_mainPoolSize, factory);
        }
        if (_inUseTracking) {
            this.inUseTable = new Hashtable<Object, Object>(_threadPoolSize * 2 + _mainPoolSize * 2);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Object Pool " + this + " created, local size: " + _threadPoolSize + ", main pool size: " + _mainPoolSize);
        }
    }

    /**
     * Construct a two tier pool with one tier using the LocalThreadObjectPool
     * and another tier using the CircularObjectPool. This version takes a
     * destroyer.
     * 
     * All threads accessing this pool will have a LocalThreadObjectPool
     * automatically created.
     * 
     * @param _threadPoolSize
     * @param _mainPoolSize
     * @param _factory
     * @param _destroyer
     * @param _inUseTracking
     */
    public TwoTierObjectPool(int _threadPoolSize, int _mainPoolSize, ObjectFactory _factory, ObjectDestroyer _destroyer, boolean _inUseTracking) {
        this.threadPoolSize = _threadPoolSize;
        this.factory = _factory;
        this.destroyer = _destroyer;
        this.inUseTracking = _inUseTracking;
        if (_threadPoolSize > 0) {
            this.threadLocals = new ThreadLocal<LocalThreadObjectPool>();
        }

        if (_mainPoolSize > 0) {
            this.mainPool = new CircularObjectPool(_mainPoolSize, factory, destroyer);
        }
        if (_inUseTracking) {
            this.inUseTable = new Hashtable<Object, Object>(_threadPoolSize * 2 + _mainPoolSize * 2);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Object Pool " + this + " created, local size: " + _threadPoolSize + ", main pool size: " + _mainPoolSize);
        }
    }

    /**
     * Construct a two tier pool with one tier using the LocalThreadObjectPool
     * and another tier using the CircularObjectPool.
     * 
     * All threads accessing this pool will have a LocalThreadObjectPool
     * automatically created.
     * 
     * @param _threadPoolSize
     * @param _mainPoolSize
     */
    public TwoTierObjectPool(int _threadPoolSize, int _mainPoolSize) {
        this(_threadPoolSize, _mainPoolSize, null, false);
    }

    /**
     * Inform the pools not to clean up old unused stored items.
     */
    public void doNotCleanUpOld() {
        if (mainPool != null) {
            mainPool.setCleanUpOld(false);
        }
        this.cleanUpOld = false;
    }

    public Object get() {

        Object ret = null;
        LocalThreadObjectPool localPool = getThreadLocalObjectPool();
        if (localPool != null) {
            ret = localPool.get();
        }

        // if local pool has run out of entries, do a batch get from the global
        // pool, and pre-fill the local pool
        if (ret == null && mainPool != null) {
            if (localPool != null) {
                Object[] objectArray = mainPool.getBatch();
                localPool.putBatch(objectArray);
                ret = localPool.get();
            } else {
                ret = mainPool.get();
            }
        }

        if (ret == null && factory != null) {
            ret = factory.create();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "Object Pool " + this + " couldn't obtain object from either local or global pool, new object created");
            }
        }

        if (inUseTracking && ret != null) {
            inUseTable.put(ret, ret);
        }
        return ret;
    }

    public Object put(Object o) {
        if (null == o) {
            throw new NullPointerException();
        }
        Object putObject = o;
        if (inUseTracking) {
            inUseTable.remove(o);
        }
        // if no local pool or local pool full, try to add to main pool
        LocalThreadObjectPool localPool = getThreadLocalObjectPool();
        if (localPool != null) {
            putObject = localPool.put(putObject);
        }
        // if local pool returns object indicating it is full, do a batch get from
        // the local pool and put it in the global pool
        if (mainPool != null && putObject != null) {
            mainPool.put(putObject);
            if (localPool != null) {
                Object[] objectArray = localPool.getBatch();
                if (objectArray[0] != null) {
                    mainPool.putBatch(objectArray);
                }
            }
        }
        return null;
    }

    /**
     * get a local thread specific pool. To be used for threads that frequently
     * access this pool
     * system.
     */
    private LocalThreadObjectPool getThreadLocalObjectPool() {
        if (threadPoolSize <= 0) {
            // no local thread pool needed.
            return null;
        }
        LocalThreadObjectPool localPool = null;
        if (threadLocals != null) {
            localPool = threadLocals.get();
            if (localPool == null) {
                localPool = new LocalThreadObjectPool(threadPoolSize, null, destroyer); // @PK36998C
                threadLocals.set(localPool);

                localPool.setCleanUpOld(cleanUpOld);
            }
        }

        return localPool;
    }

    /**
     * remove the object from the inUse before normal release processing
     * would remove it. To be used in when some other code knows that
     * leaving it in the InUse table would create false-positive leak
     * detection hits.
     * 
     * @param o
     */
    public void removeFromInUse(Object o) {
        if (null == o) {
            throw new NullPointerException();
        }

        if (inUseTracking) {
            inUseTable.remove(o);
        }
    }

    /**
     * @return Object[]
     */
    public Object[] getInUseTable() {
        return (((Hashtable) inUseTable.clone()).keySet().toArray());
    }

    /**
     * This is used to purge the ThreadLocal level information back to the main
     * group when the thread is being killed off.
     * 
     */
    public void purgeThreadLocal() {
        // if thread local pools exist, check to see if we have one created and
        // therefore need to tell it to purge
        if (null != threadLocals) {
            LocalThreadObjectPool pool = threadLocals.get();
            if (null != pool) {
                Object[] data = pool.purge();
                if (0 < data.length) {
                    mainPool.putBatch(data);
                }
            }
        }
    }
}
