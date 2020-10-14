/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.thread.term;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;

/**
 * An object pool which is optimized for access by individual threads. Much
 * like the famed TwoTierObjectPool, this pool maintains a thread level cache
 * which is used first, followed by a global cache which is used when the
 * thread cache is empty.
 *
 * The pool works hand-in-hand with the TerminationManager service to clean up
 * the thread level object pools as threads terminate. The component which
 * creates a pool instance must provide a reference to the TerminationManager
 * service, so the pool can identify threads which require cleanup, and must
 * register a TerminationHandler, so that the pool can be notified when threads
 * exit.
 *
 * This pool is not tuned as well as the TwoTierObjectPool, so for extremely
 * high throughput applications, that pool is probably preferred. If the pool
 * needs to be kept tidy when threads go away, such as when threads are tied to
 * native resources, and the calling thread can't notify the pool when it is
 * going to go away, this pool is preferred.
 */
public class ThreadOptimizedObjectPool<T> {
    /** Factory for creating pooled objects. */
    public static interface ObjectFactory<T> {
        public T create();
    }

    /** Object recycling facility. */
    public static interface ObjectDestroyer<T> {
        public void destroy(T obj);
    }

    /** The default max entries in the local pool. */
    private static final int MAX_ENTRIES_LOCAL_POOL = 5;

    /** The default max entries in the global pool. */
    private static final int MAX_ENTRIES_GLOBAL_POOL = 100;

    /** The thread level object pool. */
    private final Map<Thread, List<T>> localPools = Collections.synchronizedMap(new HashMap<Thread, List<T>>());

    /** The global object pool. */
    private final List<T> globalPool = new LinkedList<T>();

    /** The factory which will create new objects for use by the pool. */
    private ObjectFactory<T> factory = null;

    /** The destructor for objects ejected from the pool. */
    private ObjectDestroyer<T> destroyer = null;

    /** The max entries to put in the local pool. */
    private final int maxEntriesInLocalPool;

    /** The max entries to put in the global pool. */
    private final int maxEntriesInGlobalPool;

    /** The thread termination manager service. */
    private TerminationManager tm = null;

    /**
     * Constructor, taking the default local and global pool sizes.
     *
     * @param factory   The factory used to create new objects in the pool.
     * @param destroyer The destroyer used to destroy objects which won't fit in the pool.
     * @param tm        The TerminationManager service. A TerminationManager must be provided so that the pool can register
     *                      for notifications from the threads that have thread-specific pools. If the TerminationManager
     *                      service becomes unavailable, the pool creator must destroy the pool.
     */
    public ThreadOptimizedObjectPool(ObjectFactory<T> factory, ObjectDestroyer<T> destroyer, TerminationManager tm) {
        this(MAX_ENTRIES_LOCAL_POOL, MAX_ENTRIES_GLOBAL_POOL, factory, destroyer, tm);
    }

    /**
     * Constructor.
     *
     * @param localMax  The maximum number of objects to put in the thread-specific pool.
     * @param globalMax The maximum number of objects to put in the "overflow" pool which is used by all threads when
     *                      the thread-specific pool is empty.
     * @param factory   The factory used to create new objects in the pool.
     * @param destroyer The destroyer used to destroy objects which won't fit in the pool.
     * @param tm        The TerminationManager service. A TerminationManager must be provided so that the pool can register
     *                      for notifications from the threads that have thread-specific pools. If the TerminationManager
     *                      service becomes unavailable, the pool creator must destroy the pool.
     */
    public ThreadOptimizedObjectPool(int localMax, int globalMax, ObjectFactory<T> factory, ObjectDestroyer<T> destroyer, TerminationManager tm) {
        maxEntriesInLocalPool = localMax;
        maxEntriesInGlobalPool = globalMax;
        this.factory = factory;
        this.destroyer = destroyer;
        this.tm = tm;
    }

    /**
     * Get an object from the pool, or creates one if there are none available
     * in the pool.
     *
     * @return An object of the desired type, or null if no object was available.
     */
    public T get() {
        T obj = null;

        List<T> localPool = localPools.get(Thread.currentThread());
        if ((localPool != null) && (localPool.size() > 0)) {
            obj = localPool.remove(0);
        }

        if (obj == null) {
            synchronized (globalPool) {
                if (globalPool.size() > 0) {
                    obj = globalPool.remove(0);
                }
            }
        }

        if (obj == null) {
            obj = factory.create();
        }

        return obj;
    }

    /**
     * Return an object to the pool. If the object will not fit in the pool,
     * it is destroyed.
     *
     * @param obj The object to return to the pool.
     */
    public void put(T obj) {
        Thread thd = Thread.currentThread();

        List<T> localPool = localPools.get(thd);
        if (localPool == null) {
            localPool = new LinkedList<T>();
            localPools.put(thd, localPool);

            if (tm != null) {
                tm.registerCurrentThread();
            }
        }

        if (localPool.size() < maxEntriesInLocalPool) {
            localPool.add(obj);
        } else {
            poolToGlobalPoolOrDestroy(obj);
        }
    }

    /**
     * Returns an object to the global pool if there's room, or destroys
     * it.
     *
     * @param obj The object which we will attempt to put in the global pool.
     */
    private void poolToGlobalPoolOrDestroy(T obj) {
        boolean objectPooled = false;
        synchronized (globalPool) {
            if (globalPool.size() < maxEntriesInGlobalPool) {
                globalPool.add(obj);
                objectPooled = true;
            }
        }
        if (objectPooled == false) {
            destroyer.destroy(obj);
        }
    }

    /**
     * This method must be called when a thread terminates so that the pool can
     * clean up the local cache for this thread. The preferred way to do this
     * is to register a TerminationHandler service, but an alternative may be
     * appropriate in some situations.
     *
     * @param thread The thread which is terminating.
     */
    public void threadTerminated(Thread thread) {
        List<T> localPool = localPools.remove(thread);

        if (localPool != null) {
            for (T obj : localPool) {
                poolToGlobalPoolOrDestroy(obj);
            }
            localPool.clear();
        }
    }

    /**
     * Destroys all objects which are in the pool at the time this call is
     * made.
     *
     * The caller is responsible for making sure no new objects are added to
     * the pool after calling this method.
     */
    public void destroyAllObjects() {
        synchronized (localPools) {
            for (List<T> localPool : localPools.values()) {
                for (T obj : localPool) {
                    try {
                        destroyer.destroy(obj);
                    } catch (Throwable t) {
                        /* Just keep going... */
                    }
                }
                localPool.clear();
            }
            localPools.clear();
        }

        synchronized (globalPool) {
            for (T obj : globalPool) {
                try {
                    destroyer.destroy(obj);
                } catch (Throwable t) {
                    /* Just keep going... */
                }
            }
            globalPool.clear();
        }
    }

    /**
     * Collects all elements in the global and local pools.
     *
     * @return A string representation of all pool elements.
     */
    @Trivial
    @FFDCIgnore(Throwable.class)
    public String getPoolData() {
        StringBuilder sb = new StringBuilder();

        try {
            sb.append("Global Free Pool Content:");
            synchronized (globalPool) {
                if (globalPool.size() > 0) {
                    Iterator<T> ctxs = globalPool.iterator();
                    while (ctxs.hasNext()) {
                        sb.append("\n" + (ctxs.next()).toString());
                    }
                } else {
                    sb.append("\nNo free elements found in global pool.");
                }
            }

            sb.append("\n\nPer Thread Local Free Pool content:");
            synchronized (localPools) {
                if (!localPools.isEmpty()) {
                    for (Map.Entry<Thread, List<T>> entry : localPools.entrySet()) {
                        sb.append("\nThread: " + entry.getKey().getId());
                        List<T> ctxList = entry.getValue();
                        Iterator<T> ctxs = ctxList.iterator();
                        sb.append("\n{");
                        while (ctxs.hasNext()) {
                            sb.append("\n" + (ctxs.next()).toString());
                        }
                        sb.append("\n}");
                    }
                } else {
                    sb.append("\nNo free elements found in local thread pools.");
                }
            }
        } catch (Throwable t) {
            sb.append("ThreadOptimizedObjectPool.getPoolData. Error: " + t.toString());
        }
        return sb.toString();
    }
}
