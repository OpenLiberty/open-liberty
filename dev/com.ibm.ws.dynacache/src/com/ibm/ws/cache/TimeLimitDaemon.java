/*******************************************************************************
 * Copyright (c) 1997, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.stat.CachePerf;

/**
 * This thread removes cache entries whose time limit has expired.
 * It tries to wake up once every time granule, which is a configurable
 * number of seconds.
 * It may get behind temporarily due to bursts of server processor activity
 * (e.g., garbage collection).  However, it will catch back up when
 * processor resources are available.
 */
public class TimeLimitDaemon extends RealTimeDaemon {
    private static TraceComponent tc = Tr.register(TimeLimitDaemon.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    public final static boolean UNIT_TEST_INACTIVITY = false; 

    private static final boolean IS_UNIT_TEST = false;

    private static final int DEFAULT_SIZE_FOR_MEM = 1024;
    private static final int DEFAULT_SIZE_FOR_MEM_DISK = DEFAULT_SIZE_FOR_MEM * 20;

    /**
     * This daemon tries to wake up each time granule.
     * A time limit for a cacheEntry is rounded up to a time granule.
     * It is set by the Configuration class in the TimeLimitDaemon constructor.
     */

    private InvalidationTaskPool taskPool = new InvalidationTaskPool(100);

    private ConcurrentHashMap <DCache, ExpirationMetaData> cacheInstancesTable = new ConcurrentHashMap <DCache, ExpirationMetaData>(10,2,2);

    private long lastTimeReleaseDiskCachePool = 0; 

    private long lastTimeoutCheckedTime = 0;

    private int timeoutTriggerTime   = 0; // TLD granularity (in msec)
    private int lruToDiskTriggerTime = 0; // frequency of removing cache entries from overflow buffer asynchronously (in msec)

    private boolean isLruToDiskRunnning = false;

    //private long tcount = 0;
    //private long ccount = 0;

    /*
     * This is called by the Configuration class to create the
     * TimeLimitDaemon.
     *
     * @param maxTimeLimitInSeconds This determines how many seconds the
     * longest time limit can be on a cache entry. DEPRECATED
     * @param timeGranularityInSeconds This is the timeGranularity.
     * @param lruToDiskTriggerTime This is the frequency of removing cache entries from overflow buffer asynchronously
     */
    public TimeLimitDaemon(int timeGranularityInSeconds, int lruToDiskTriggerTime) {
        super(lruToDiskTriggerTime);
        this.lruToDiskTriggerTime = lruToDiskTriggerTime;
        this.timeoutTriggerTime = timeGranularityInSeconds * 1000;
        this.isLruToDiskRunnning = false;
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Creating TimeLimitDaemon - set time granularity to " + timeGranularityInSeconds + " lruToDiskTriggerTime=" + this.lruToDiskTriggerTime);
        this.lastTimeReleaseDiskCachePool = System.currentTimeMillis();
        if (timeGranularityInSeconds <= 0) {
            throw new IllegalArgumentException("timeGranularityInSeconds must be positive");
        }
    }

    /**
     * This is alled by the CacheUnitImpl when things start up.
     */
    public void start() {
        super.start();
    }

    /**
     * This implements the abstract method in RealTimeDaemon.
     * It gathers all the cacheEntry ids whose timeLimit has expired
     * and calls the Cache.invalidateById method.
     * It handles the case where multiple timeIntervals have
     * elapsed since the last call and catches up to current time
     * by processing multiple timeLimitArray elements.
     *
     * @param startDaemonTime The time (in milliseconds) when the daemon
     * was started.
     * @param startWakeUpTime The time (in milliseconds) just before
     * the wakeUp method was called.
     */
    public void wakeUp(long startDaemonTime, long startWakeUpTime) {
        if ( UNIT_TEST_INACTIVITY ) {
            System.out.println("wakUp() - entry");
        }
        //final String methodName = "wakeUp()";
        boolean bTimeOutChecked = false;
        try {
            // check the cache entries timeout when one of following conditions meet
            // (1) lruToDiskTriggerTime == timeoutTriggerTime (TLD granularity; default 5 sec)
            // (2) time elapsed >= timeoutTriggerTime (5 sec) since the last timeout checked.
            if (this.lruToDiskTriggerTime == this.timeoutTriggerTime || 
                ((startWakeUpTime - this.lastTimeoutCheckedTime) >= this.timeoutTriggerTime)) {
                this.lastTimeoutCheckedTime = startWakeUpTime;
                ArrayList <InvalidationData> invalidateIds = new ArrayList <InvalidationData>();
                Iterator walker = cacheInstancesTable.entrySet().iterator();
                while (walker.hasNext()) {
                    Map.Entry entry = (Map.Entry) walker.next();
                    DCache cache = (DCache) entry.getKey();
                    ExpirationMetaData expirationMetaData = (ExpirationMetaData) entry.getValue();
                    synchronized (expirationMetaData) {
                        InvalidationTask currentTask = expirationMetaData.timeLimitHeap.minimum();
                        while (currentTask != null) {
                            if ( UNIT_TEST_INACTIVITY ) {
                                System.out.println(" startWakeUpTime="+ startWakeUpTime);
                                System.out.println(" currentTask.expirationTime="+ currentTask.expirationTime);
                                System.out.println(" currentTask.isInactivityTimeOut="+ currentTask.isInactivityTimeOut);
                            }
                            if (currentTask.expirationTime <= startWakeUpTime) {
                                currentTask = expirationMetaData.timeLimitHeap.deleteMin();
                                expirationMetaData.expirationTable.remove(currentTask.id);
                                //tcount++;
                                //traceDebug(methodName, "cacheName=" + cache.getCacheName() + " id=" + currentTask.id + " expirationTableSize=" + expirationMetaData.expirationTable.size() + " timeLimitHeapSize=" + expirationMetaData.timeLimitHeap.size() + " count=" + tcount);
                                invalidateIds.add(new InvalidationData(currentTask.id, currentTask.isInactivityTimeOut));
                                currentTask.reset();
                                taskPool.add(currentTask);
                                currentTask = expirationMetaData.timeLimitHeap.minimum();
                            } else {
                                currentTask = null;
                            }
                        }
                    }
                    // invalidate expired or inactive CacheEntry
                    if (invalidateIds.size() > 0) {
                        Iterator it = invalidateIds.iterator();
                        while (it.hasNext()) {
                            InvalidationData idata = (InvalidationData) it.next();
                            cache.invalidateById(idata.id, idata.isInactivityTimeOut?CachePerf.INACTIVE:CachePerf.TIMEOUT, false); // CPF-Inactivity
                        }
                        invalidateIds.clear();
                    }
                }
                diskCacheHouseKeeping();  //3821
                bTimeOutChecked = true;
            }
        } catch (Throwable ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.TimeLimitDaemon.wakeUp", "152", this);
        } finally {
            // skip trimCache (removing entries in the overflow buffer) operation if trimCache operation 
            // is still in process
            // loop through each cache instances to remove cache entries in the overflow buffer
            // and asynchronously offloaded to disk at a frequency of lruToDiskTriggerTime 
            // milliseconds. Process trimCache when one of the following conditions meet:
            // (1) timeout has just finished checking (sane behavior as before).
            // (2) lruToDiskTriggerPercent for a particular cache instance > 0
        	// (3) we need to contrain the cache instance in terms of JVM heap
            if (this.isLruToDiskRunnning == false) {
                this.isLruToDiskRunnning = true;
                Map caches = ServerCache.getCacheInstances();
                Iterator i = caches.values().iterator();
                while ( i.hasNext() ) {
                    DCache cache = (DCache)i.next();
                    if (cache != null && cache.getCacheConfig().isDefaultCacheProvider()) {
                        if (bTimeOutChecked ||
                        		cache.getCacheConfig().getLruToDiskTriggerPercent() > 0 || 
                        		cache.isCacheSizeInMBEnabled()) {
                            cache.trimCache();
                        }
                    }
                }
                this.isLruToDiskRunnning = false;
            }
        }
    }

    private void diskCacheHouseKeeping(){
        boolean bReleaseDiskCachePool = false;
        if ((System.currentTimeMillis() - this.lastTimeReleaseDiskCachePool) > CacheConfig.DEFAULT_DISKCACHE_POOL_ENTRY_LIFE * 12) {
            bReleaseDiskCachePool = true;
            this.lastTimeReleaseDiskCachePool = System.currentTimeMillis();
        }
        String cacheName = null;
        DCache ci = null;

        //Handle all the cache instances
        Iterator it = (ServerCache.getCacheInstances()).keySet().iterator();
        while (it.hasNext()) {
            cacheName = (String)it.next();      
            ci = ServerCache.getCache(cacheName);       
            if (ci != null && ci.getCacheConfig().isDefaultCacheProvider()) {
                if (ci.isDiskInvalidationBufferFull()) {
                    ci.invokeDiskCleanup(!HTODInvalidationBuffer.SCAN);
                }
                if (bReleaseDiskCachePool) {
                    ci.releaseDiskCacheUnusedPools();
                }
            }
        }
    }

    /**
     * This notifies this daemon that a value has changed,
     * so the expiration time should be updated.
     * It updates internal tables and indexes accordingly.
     *
     * @param cache The cache instance.
     * @param id The cache id.
     * @param expirationTime The new expiration time.
     */
    public void valueHasChanged(DCache cache, Object id, long expirationTime, int inactivity) {  // CPF-Inactivity
        //final String methodName = "valueHasChanged()";
        if (expirationTime <= 0 && inactivity <=0 ) { // CPF-Inactivity
            throw new IllegalArgumentException("expirationTime or inactivity must be positive");
        }
        if ( UNIT_TEST_INACTIVITY ) {
            System.out.println(" valueHasChanged() - entry");
            System.out.println(" expirationTime="+expirationTime);
            System.out.println(" inactivity="+inactivity);
        }
        // CPF-Inactivity
        //-----------------------------------------------------------
        // Force an expriationTime if we have an inactivity timer
        //-----------------------------------------------------------
        boolean isInactivityTimeOut = false;
        if ( inactivity > 0 ) {
            // For 7.0, use QuickApproxTime.getRef.getApproxTime()
            long adjustedExpirationTime = System.currentTimeMillis() + ( inactivity * 1000 );
            if ( adjustedExpirationTime < expirationTime ||
                 expirationTime <= 0 ) {
                expirationTime = adjustedExpirationTime;
                isInactivityTimeOut = true;
            }
        }
        ExpirationMetaData expirationMetaData = (ExpirationMetaData)cacheInstancesTable.get(cache);
        // Just make sure expirationMetaData is NOT NULL in case of an internal-error condition
        if (expirationMetaData == null) {
            return;
        }
        synchronized (expirationMetaData) {
            InvalidationTask it = (InvalidationTask) expirationMetaData.expirationTable.get(id);
            if (it == null) {
                it = (InvalidationTask) taskPool.remove();
                it.id = id;
                expirationMetaData.expirationTable.put(id, it);
            } else {
                expirationMetaData.timeLimitHeap.delete(it);
            }
            it.expirationTime = expirationTime;
            it.isInactivityTimeOut = isInactivityTimeOut;
            expirationMetaData.timeLimitHeap.insert(it);
            //ccount++;
            //traceDebug(methodName, "cacheName=" + cache.getCacheName() + " id=" + id + " expirationTableSize=" + expirationMetaData.expirationTable.size() + " timeLimitHeapSize=" + expirationMetaData.timeLimitHeap.size() + " count=" + ccount);
        }
    }

    /**
     * This notifies this daemon that an entry has removed,
     * It removes the entry from the internal tables.
     *
     * @param cache The cache instance.
     * @param id The cache id.
     */
    public void valueWasRemoved(DCache cache, Object id) {
        //final String methodName = "valueWasRemoved()";
        if ( UNIT_TEST_INACTIVITY ) {
            System.out.println("valueWasRemoved() - entry");
        }
        ExpirationMetaData expirationMetaData = (ExpirationMetaData)cacheInstancesTable.get(cache);
        if (expirationMetaData == null) {
            return;
        }
        synchronized (expirationMetaData) {
            InvalidationTask it = (InvalidationTask) expirationMetaData.expirationTable.remove(id);
            if (it != null) {
                expirationMetaData.timeLimitHeap.delete(it);
                it.reset();
                taskPool.add(it);
                //traceDebug(methodName, "cacheName=" + cache.cacheName + " id=" + id + " expirationTableSize=" + expirationMetaData.expirationTable.size() + " timeLimitHeapSize=" + expirationMetaData.timeLimitHeap.size());
            }
        }
    }

    // CPF-Inactivity - cache check inactivity > 0 before calling this method.
    public void valueWasAccessed(DCache cache, Object id, long expirationTime, int inactivity) {
        valueHasChanged(cache, id, expirationTime, inactivity);
    }

    /**
     * This method is called when the cache is created. This will initialize ExpirationMetaData for specified cache
     *
     * @param cache The cache instance.
     */
    public void createExpirationMetaData(DCache cache) {
        //final String methodName = "createExpirationMetaData()";
        ExpirationMetaData expirationMetaData = (ExpirationMetaData)cacheInstancesTable.get(cache);
        if (expirationMetaData == null) {
            int initialTableSize = DEFAULT_SIZE_FOR_MEM;
            if (cache.getSwapToDisk() && cache.getCacheConfig().getDiskCachePerformanceLevel() == CacheConfig.HIGH) {
                initialTableSize = DEFAULT_SIZE_FOR_MEM_DISK;
            }
            expirationMetaData = new ExpirationMetaData(initialTableSize);
            cacheInstancesTable.put(cache, expirationMetaData);
        }
    }

    /**
     * This notifies this daemon that a specified cache instance has cleared.
     * It clears the internal tables
     *
     * @param cache The cache instance.
     * @param id The cache id.
     */
    public void cacheCleared(DCache cache) {
        final String methodName = "cacheCleared()";
        ExpirationMetaData expirationMetaData = (ExpirationMetaData)cacheInstancesTable.get(cache);
        if (expirationMetaData == null) {
            return;
        }
        synchronized (expirationMetaData) {
            if (!expirationMetaData.expirationTable.isEmpty()) {
                Enumeration e = expirationMetaData.expirationTable.elements();
                while (e.hasMoreElements()) {
                    InvalidationTask it = (InvalidationTask)e.nextElement();
                    it.reset();
                    taskPool.add(it);
                }
                expirationMetaData.expirationTable.clear();
                expirationMetaData.timeLimitHeap.clear();
                if (tc.isDebugEnabled()){
                	Tr.debug(tc, methodName + cache.getCacheName() + " expirationTable=" + expirationMetaData.expirationTable.size());
                }
            }
        }
    }

    static public class ExpirationMetaData {
        /**
         * ExpirationTable allows the timeLimitArray element
         * to be found containing a given cacheEntry id.
         * The key is a cacheEntry id.
         * The value is the InvalidationTask
         */
        public NonSyncHashtable expirationTable;
        /**
         * TimeLimitHeap holds InvalidationTask elements for each id that needs to be
         * invalidated
         */
        public BinaryHeap timeLimitHeap;

        public ExpirationMetaData(int initialTableSize) {
            this.expirationTable = new NonSyncHashtable(initialTableSize);
            this.timeLimitHeap   = new BinaryHeap(initialTableSize);
        }
    }

    static public class InvalidationData {
        public Object id;
        public boolean isInactivityTimeOut;
        public InvalidationData(Object id, boolean isInactivityTimeOut) {
            this.id = id;
            this.isInactivityTimeOut = isInactivityTimeOut;
        }
    }

    static public class InvalidationTask {
        public Object id;              // cache id
        public long expirationTime;    // expiration time 
        public boolean isInactivityTimeOut = false;
        public int index;              // index in heap

        public final boolean lessThan(InvalidationTask other) {
            return expirationTime < other.expirationTime;
        }

        public final boolean equals(InvalidationTask other) {
            return expirationTime == other.expirationTime;
        }

        public final boolean lessThanOrEquals(InvalidationTask other) {
            return expirationTime <= other.expirationTime;
        }
        
        public final void reset(){        	
        	id = null;
        	expirationTime = -1;  
        	isInactivityTimeOut = false;
        	index = -1;      
        }        

    }

    static class InvalidationTaskPool extends com.ibm.ws.util.ObjectPool {
        public InvalidationTaskPool(int size) {
            super("InvalidationTaskPool", size);
        }

        protected Object createObject() {
            return new InvalidationTask();
        }
    }

    /**
     * BinaryHeap - binary heap implementation
     *
     */

    static public class BinaryHeap {

        private InvalidationTask[] heapArray;
        private int heapSize;

        public BinaryHeap(int size) {
            //heapArray = new InvalidationTask[DEFAULT_SIZE];
            heapArray = new InvalidationTask[size];
            InvalidationTask negInfinity = new InvalidationTask();
            negInfinity.expirationTime = Long.MIN_VALUE;
            negInfinity.index = 0;
            heapArray[0] = negInfinity;
            heapSize = 0;
        }

        /*
        public void dump() {
           for (int i=0;i<=heapSize;i++) {
              System.out.println("["+i+"]="+heapArray[i].expirationTime);
           }
        }
        */

        //
        // Heap operations
        //

        /**
         *  Insert an element E with key E.key into the heap, preserving
         *  heap order.
         *
         *  @param el  the InvalidationTask element to insert
         *
         */
        public synchronized void insert(InvalidationTask el) {
            int i = ++heapSize;
            growIfNec();
            while (el.lessThan(heapArray[parent(i)])) {
                heapArray[i] = heapArray[parent(i)];
                heapArray[i].index = i;
                i = parent(i);
            }
            // i now indicates this element's proper place
            heapArray[i] = el;
            heapArray[i].index = i;
        }

        /**
         *  Return a reference to the minimum element in the heap,
         *  without removing it.
         */

        public final InvalidationTask minimum() {
            if (isEmpty()) {
                return null;
            }
            return heapArray[1];
        } // minimum

        /**
         *  Return a reference to the minimum element in the heap,
         *  removing it from the heap.
         */
        public final synchronized InvalidationTask deleteMin() {
            InvalidationTask min;
            if (isEmpty())
                min = null;
            else {
                // Pull out the last element
                InvalidationTask last = heapArray[heapSize];
                heapArray[heapSize--] = null;
                if (isEmpty())
                    min = last; // last was the only element
                else {
                    // Grab minimum from the top, put last back there, and re-heapify
                    min = heapArray[1];
                    heapArray[1] = last;
                    heapArray[1].index = 1;
                    heapify(1);
                }
            }
            return min;
        }

        /**
         *  Deletes the item with the given key from the heap.
         *
         *  @param i  the index, or key, of the item to delete
         */
        public synchronized void delete(InvalidationTask el) {
            int i = findKey(el);
            if (i == -1)
                throw new java.lang.IllegalArgumentException();
            heapArray[i] = heapArray[0]; // heapArray[0] holds negative infinity
            heapArray[i].index = i;
            percolateUp(i);
            deleteMin();
            heapArray[0].index = 0;
        }

        private int findKey(InvalidationTask c) {
            return c.index;
        }

        /**
         *  Reestablish heap property with the precondition
         *  that the element at i is smaller than it's parent.
         *
         *  @param i  the index which violates heap property
         */
        private void percolateUp(int i) {
            while (heapArray[i].lessThan(heapArray[parent(i)])) {
                // swap element i with it's parent
                int j = parent(i);
                InvalidationTask c = heapArray[j];
                heapArray[j] = heapArray[i];
                heapArray[j].index = j;
                heapArray[i] = c;
                heapArray[i].index = i;
                int prev = i;
                i = j;
                // need to call heapify at this point on
                // the item we just swapped
                heapify(prev);
            }
        }

        /**
         *  Heapify assumes that given an index i into the heap, left(i)
         *  and right(i) are heaps, but i may violate the heap property.
         *  It effectively percolates i down the heap until the heap property
         *  is reestablished. <p>
         *
         *  @param index  the index of the heap array which violates the heap property.
         */
        private final void heapify(int i) {
            InvalidationTask tmp = heapArray[i];
            int l;
            for (; left(i) <= heapSize; i = l) {
                l = left(i); // left child of current node
                // if the left child is not the end of the heap, and
                // the right child is less than left child, advance
                // to the right child.
                if (l < heapSize && heapArray[right(i)].lessThan(heapArray[l]))
                    l++;

                // if the current, lesser child is less than the item we are
                // heapifying for, then move that child into the
                // current slot, else we're done.
                if (heapArray[l].lessThan(tmp)) {
                    heapArray[i] = heapArray[l];
                    heapArray[i].index = i;
                } else
                    break;
            }
            heapArray[i] = tmp;
            heapArray[i].index = i;
        }

        public final boolean isEmpty() {
            return heapSize == 0;
        }

        public final int size() {
            return heapSize;
        }

        public final void clear() {
            for (int i = 1; i <= heapSize; i++) {
                heapArray[i] = null;
            }
            heapSize = 0;
        }

        private static final int parent(int i) {
            return(int) i / 2;
        }

        private static final int left(int i) {
            return 2 * i;
        }

        private static final int right(int i) {
            return 2 * i + 1;
        }

        /**
         * Private method that doubles the heap array if full.
         */
        private void growIfNec() {
            if ((heapSize + 1) == heapArray.length) {
                InvalidationTask[] oldHeap = heapArray;
                heapArray = new InvalidationTask[heapSize * 2];
                System.arraycopy(oldHeap, 0, heapArray, 0, oldHeap.length);
            }
        }
    } // BinaryHeap
}
