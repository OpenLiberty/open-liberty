/*******************************************************************************
 * Copyright (c) 1997, 2008 IBM Corporation and others.
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
import java.util.Iterator;
import java.util.HashMap;
import java.util.Set;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cache.HTODDynacache.EvictionTableEntry;
import com.ibm.ws.cache.stat.CachePerf;

/**
 * This class is used by CacheOnDisk and HTODDynacache. The function is to buffer the invalidation ids or expired ids
 * and delay the invalidation. The Low Priority Background Thread will be invoked when (1) the disk cleanup is scheduled
 * (2) invalidation buffer is in full/life condition.
 */
public class HTODInvalidationBuffer {

    private static TraceComponent tc = Tr.register(HTODInvalidationBuffer.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    private static final boolean IS_UNIT_TEST = false;

    public static final boolean SCAN = true;
    public static final boolean FIRE_EVENT = true;
    public static final boolean CHECK_FULL = true;
    public static final boolean ALIAS_ID = true;

    public static final int SCAN_BUFFER = 1; // 3821
    public static final int EXPLICIT_BUFFER = 2; // 3821
    public static final int GC_BUFFER = 3; // 3821

    public static final byte STATUS_FIRE_EVENT = (byte) 0x80;
    public static final byte STATUS_FROM_DEPID_TEMPLATE = (byte) 0x40;
    public static final byte STATUS_ALIAS = (byte) 0x20;
    public static final byte STATUS_REMOTE = (byte) 0x10;
    public static final byte STATUS_CAUSE_MASK = (byte) 0x0F;

    private CacheOnDisk cod = null;
    private HashMap explicitBuffer = null;
    private ValueSet scanBuffer = null;
    private ArrayList garbageCollectorBuffer = null; // 3821

    private boolean cleanupDiskPending = false;
    private boolean loopOnce = false;
    private boolean stopping = false;
    private boolean diskClearInProgress = false;
    private int maxInvalidationBufferSize = 0;
    private int maxInvalidationBufferLife = 0;
    private long lastRemoveTime = 0;
    private long lastWaitTime = 0;

    private HTODInvalidationBuffer() {
    }

    public HTODInvalidationBuffer(CacheOnDisk cod) {
        this.cod = cod;
        this.maxInvalidationBufferSize = cod.invalidationBufferSize;
        this.maxInvalidationBufferLife = cod.invalidationBufferLife;
        this.explicitBuffer = new HashMap(this.maxInvalidationBufferSize);
        this.scanBuffer = new ValueSet(this.maxInvalidationBufferSize);
        if (this.cod.evictionPolicy != CacheConfig.EVICTION_NONE) {
            this.garbageCollectorBuffer = new ArrayList(this.maxInvalidationBufferSize); // 3821
        } else {
            this.garbageCollectorBuffer = new ArrayList();
        }
        this.cleanupDiskPending = false;
        this.loopOnce = false;
        this.stopping = false;

    }

    /**
     * Call this method to store a cache id in the one of invalidation buffers. The entry is going to remove from the
     * disk using LPBT.
     * 
     * @param id
     *            - Object
     * @param eventAlreadyFired
     *            - boolean to select which invalidation buffer is used to store.
     */
    protected synchronized void add(Object id, int bufferType, int cause, int source, boolean fromDepIdTemplateInvalidation, boolean fireEvent,
            boolean isAlias) {
        final String methodName = "add(Object)";
        if (id == null) {
            return;
        }
        if (bufferType == EXPLICIT_BUFFER) {
            byte info = 0;
            if (cause != 0 && source != 0) {
                info = (byte) cause;
                if (source == CachePerf.REMOTE) {
                    info = (byte) (info | STATUS_REMOTE);
                }
                if (fromDepIdTemplateInvalidation) {
                    info = (byte) (info | STATUS_FROM_DEPID_TEMPLATE);
                }
                if (isAlias) {
                    info = (byte) (info | STATUS_ALIAS);
                }
                if (fireEvent) {
                    info = (byte) (info | STATUS_FIRE_EVENT);
                }
            }
            this.explicitBuffer.put(id, new Byte(info));
            this.scanBuffer.remove(id);
        } else if (bufferType == SCAN_BUFFER) {
            if (!this.explicitBuffer.containsKey(id)) {
                this.scanBuffer.add(id);
            }
        } else if (bufferType == GC_BUFFER) {
            this.garbageCollectorBuffer.add(id);
        }

        traceDebug(methodName, "cacheName=" + this.cod.cacheName + " id=" + id + " bufferType=" + bufferType + " ExplicitBuffer="
                + this.explicitBuffer.size() + " ScanBuffer=" + this.scanBuffer.size() + " GCBuffer=" + this.garbageCollectorBuffer.size()
                + " cause=" + cause + " source=" + source + " fireEvent=" + fireEvent);

        if (isFull() || bufferType == GC_BUFFER) { // 3821 NK end
            invokeBackgroundInvalidation(!SCAN);
        }
    }

    /**
     * Call this method to store a collection of cache ids in the one of invalidation buffers. The entries are going to
     * remove from the disk using LPBT.
     * 
     * @param idSet
     *            - ValueSet used to store a collection of Ids.
     * @param eventAlreadyFired
     *            - boolean to select which invalidation buffer is used to store.
     * @param checkFull
     *            - boolean to check the invalidation buffer full condition
     */
    protected synchronized void add(ValueSet idSet, int bufferType, int cause, int source, boolean fromDepIdTemplateInvalidation, boolean fireEvent,
            boolean checkFull) {
        final String methodName = "add(ValueSet)";
        if (idSet == null || idSet.isEmpty()) {
            return;
        }
        int size = idSet.size();
        if (bufferType == EXPLICIT_BUFFER) {
            byte info = 0;
            if (cause != 0 && source != 0) {
                info = (byte) cause;
                if (source == CachePerf.REMOTE) {
                    info = (byte) (info | STATUS_REMOTE);
                }
                if (fromDepIdTemplateInvalidation) {
                    info = (byte) (info | STATUS_FROM_DEPID_TEMPLATE);
                }
                if (fireEvent) {
                    info = (byte) (info | STATUS_FIRE_EVENT);
                }
            }
            Iterator it = idSet.iterator();
            while (it.hasNext()) {
                Object entryId = it.next();
                this.explicitBuffer.put(entryId, new Byte(info));
            }
            if (!this.scanBuffer.isEmpty()) {
                filter(this.scanBuffer, idSet);
            }
        } else if (bufferType == SCAN_BUFFER) {
            if (!explicitBuffer.isEmpty()) {
                filter(idSet, this.explicitBuffer);
            }
            this.scanBuffer.addAll(idSet);
        } else if (bufferType == GC_BUFFER) {
            this.garbageCollectorBuffer.addAll(idSet);
        }

        // if (size > 50) {
        traceDebug(methodName, "cacheName=" + this.cod.cacheName + " idSet=" + size + " idSetFilter=" + idSet.size() + " bufferType=" + bufferType
                + " explicitBuffer=" + this.explicitBuffer.size() + " scanBuffer=" + this.scanBuffer.size() + " GCBuffer="
                + this.garbageCollectorBuffer.size());
        // }

        if ((checkFull && isFull()) || bufferType == GC_BUFFER) { // 3821 NK end
            invokeBackgroundInvalidation(!SCAN);
        }
    }

    protected synchronized void add(ArrayList evictionSet, int bufferType) { // 3821 begin
        final String methodName = "add(evictionSet)";
        if (evictionSet == null || evictionSet.isEmpty()) {
            return;
        }
        if (bufferType == GC_BUFFER) {
            this.garbageCollectorBuffer.addAll(evictionSet);
            traceDebug(methodName, "cacheName=" + this.cod.cacheName + " evictionSet=" + evictionSet.size() + " explicitBuffer="
                    + this.explicitBuffer.size() + " scanBuffer=" + this.scanBuffer.size() + " GCBuffer=" + this.garbageCollectorBuffer.size());
            invokeBackgroundInvalidation(!SCAN);
        }
    } // 3821 end

    /**
     * Call this method when a cache id is retrieved from one of invalidation buffers. The entry is being used to remove
     * from the disk using LPBT.
     * 
     * @param bufferType
     *            - specify the invalidation buffer.
     * @return Object cache id.
     */
    protected synchronized Object get(int bufferType) {
        // final String methodName = "get(boolean)";
        Object id = null;
        if (bufferType == this.EXPLICIT_BUFFER) {
            if (!this.explicitBuffer.isEmpty()) {
                Set s = this.explicitBuffer.keySet();
                Iterator it = s.iterator();
                ExplicitIdData idData = new ExplicitIdData();
                while (it.hasNext()) {
                    idData.id = it.next();
                    idData.info = ((Byte) this.explicitBuffer.get(idData.id)).byteValue();
                    if ((idData.info & HTODInvalidationBuffer.STATUS_ALIAS) == 0) {
                        id = idData;
                        break;
                    }
                }
            }
        } else if (bufferType == this.SCAN_BUFFER) {
            if (!this.scanBuffer.isEmpty()) {
                id = this.scanBuffer.getOne();
            }
        } else if (bufferType == this.GC_BUFFER) {
            if (!this.garbageCollectorBuffer.isEmpty()) {
                // Always get the first one from the list
                id = this.garbageCollectorBuffer.get(0);
            }
        }

        // if (id != null) {
        // traceDebug(methodName, "cacheName=" + this.cod.cacheName + " id=" + id + " bufferType=" + bufferType);
        // }
        return id;
    }

    /**
     * Call this method when a cache id is retrieved from one of invalidation buffers.
     * 
     * @param bufferType
     *            - specify the invalidation buffer.
     * @return Object cache id.
     */
    protected synchronized Object getAndRemove(int bufferType) {
        final String methodName = "getAndRemove(bufferType)";
        Object id = null;
        if (bufferType == this.EXPLICIT_BUFFER) {
            if (!this.explicitBuffer.isEmpty()) {
                Set s = this.explicitBuffer.keySet();
                Iterator it = s.iterator();
                ExplicitIdData idData = new ExplicitIdData();
                while (it.hasNext()) {
                    idData.id = it.next();
                    idData.info = ((Byte) this.explicitBuffer.get(idData.id)).byteValue();
                    if ((idData.info & HTODInvalidationBuffer.STATUS_ALIAS) == 0) {
                        id = idData;
                        this.explicitBuffer.remove(idData.id);
                        break;
                    }
                }
            }
        } else if (bufferType == this.SCAN_BUFFER) {
            if (!this.scanBuffer.isEmpty()) {
                id = this.scanBuffer.getOne();
                if (id != null) {
                    this.scanBuffer.remove(id);
                }
            }
        } else if (bufferType == this.GC_BUFFER) {
            if (!this.garbageCollectorBuffer.isEmpty()) {
                // Always get the first one from the list
                id = this.garbageCollectorBuffer.get(0);
                if (id != null) {
                    this.remove(id, bufferType, false); // id not return to pool but it will return later.
                }
            }
        }

        // if (id != null) {
        // traceDebug(methodName, "cacheName=" + this.cod.cacheName + " id=" + id + " bufferType=" + bufferType +
        // " explicitBuffer=" + this.explicitBuffer.size() + " scanBuffer=" + this.scanBuffer.size() + " GCBuffer=" +
        // this.garbageCollectorBuffer.size());
        // }
        return id;
    }

    /**
     * Call this method when a cache id is removed from one of invalidation buffers. The entry is being used to remove
     * from the disk using LPBT.
     * 
     */

    protected synchronized void remove(Object id, int bufferType) {
        remove(id, bufferType, true);
    }

    // returnToPool boolean is used by GC buffer only
    protected synchronized void remove(Object id, int bufferType, boolean returnToPool) {
        // final String methodName = "remove(Object,bufferType)";
        if (id == null) {
            return;
        }
        if (bufferType == this.EXPLICIT_BUFFER) {
            this.explicitBuffer.remove(id);
        } else if (bufferType == this.SCAN_BUFFER) {
            this.scanBuffer.remove(id);
        } else if (bufferType == this.GC_BUFFER) {
            if (id instanceof EvictionTableEntry) {
                EvictionTableEntry evt1 = (EvictionTableEntry) id;
                int i;
                for (i = 0; i < garbageCollectorBuffer.size(); i++) {
                    EvictionTableEntry evt = (EvictionTableEntry) garbageCollectorBuffer.get(i);
                    if (evt == evt1)
                        break;
                }
                if (i < garbageCollectorBuffer.size())
                    this.garbageCollectorBuffer.remove(i);
                if (returnToPool) {
                    cod.htod.evictionEntryPool.add(id);
                }
            }
        }
        // traceDebug(methodName, "cacheName=" + this.cod.cacheName + " id=" + id + " bufferType=" + bufferType);
    }

    /**
     * Call this method to remove a specified cache id from invalidation buffers excluding garbage collector buffers.
     */
    protected synchronized void remove(Object id) {
        // final String methodName = "remove(Object)";
        if (id == null) {
            return;
        }
        this.explicitBuffer.remove(id);
        this.scanBuffer.remove(id);
        // this.garbageCollectorBuffer.remove(id);
        // if (id instanceof HTODDynacache.EvictionTableEntry)
        // cod.htod.evictionEntryPool.add(id);

        // traceDebug(methodName, "id=" + id);
    }

    /**
     * Call this method to get and remove all the cache ids from explicit buffer.
     * 
     * @return ValueSet - a collection of cache ids NOT including alias ids.
     */
    protected synchronized ValueSet getAndRemoveFromExplicitBuffer() {
        final String methodName = "getAndRemoveFromExplicitBuffer()";
        ValueSet valueSet = null;
        if (this.explicitBuffer.size() == 0) {
            valueSet = new ValueSet(1);
        } else {
            valueSet = new ValueSet(this.explicitBuffer.size());
            Iterator it = this.explicitBuffer.keySet().iterator();
            while (it.hasNext()) {
                Object current = it.next();
                byte info = ((Byte) this.explicitBuffer.get(current)).byteValue();
                if ((info & HTODInvalidationBuffer.STATUS_ALIAS) == 0) {
                    valueSet.add(current);
                }
            }
            this.explicitBuffer.clear();
        }

        traceDebug(methodName, "cacheName=" + this.cod.cacheName + " bufferSize=" + valueSet.size());
        return valueSet;
    }

    /**
     * Call this method to get and remove the EVT from GC buffer.
     * 
     * @return boolean indicate whether it is found from GC buffer.
     */
    protected synchronized boolean findAndRemoveFromGCBuffer(long expirationTime, int hashcode, int size) {
        EvictionTableEntry evt = null;
        int i;
        for (i = 0; i < garbageCollectorBuffer.size(); i++) {
            evt = (EvictionTableEntry) garbageCollectorBuffer.get(i);
            if (evt.expirationTime == expirationTime && evt.hashcode == hashcode && evt.size == size) {
                break;
            }
        }
        if (i < garbageCollectorBuffer.size()) {
            this.garbageCollectorBuffer.remove(i);
            if (evt != null) {
                cod.htod.evictionEntryPool.add(evt);
            }
            return true;
        }
        return false;
    }

    /**
     * Call this method to clear the invalidation buffers.
     * 
     * @param forEventAlreadyFired
     *            - true to clear invalidation buffer which is used for event already fired.
     * @param forEventNotFired
     *            - true to clear invalidation buffer which is used for event not fired.
     */
    protected synchronized void clear(int bufferType) {
        final String methodName = "clear()";
        if (bufferType == HTODInvalidationBuffer.EXPLICIT_BUFFER) {
            this.explicitBuffer.clear();
        } else if (bufferType == HTODInvalidationBuffer.SCAN_BUFFER) {
            this.scanBuffer.clear();
        } else if (bufferType == HTODInvalidationBuffer.GC_BUFFER) {
            // todo: return GC to pool?
            for (int i = 0; i < garbageCollectorBuffer.size(); i++) {
                EvictionTableEntry evt = (EvictionTableEntry) garbageCollectorBuffer.get(i);
                cod.htod.evictionEntryPool.add(evt);
            }
            this.garbageCollectorBuffer.clear();
        }
        traceDebug(methodName, "cacheName=" + this.cod.cacheName + " bufferType=" + bufferType);
    }

    /**
     * Call this method to invoke LPBT.
     * 
     * @param scan
     *            - true to disk cache for expired entries.
     */
    protected synchronized void invokeBackgroundInvalidation(boolean scan) {
        final String methodName = "invokeBackgroundInvalidation()";
        if (!stopping) {
            synchronized (cod.diskCleanupThread.dcMonitor) {
                if (cod.diskCleanupThread.currentThread != null) {
                    this.cod.invokeDiskCleanup(scan);
                } else {
                    if (scan == SCAN) {
                        traceDebug(methodName, "cacheName=" + this.cod.cacheName + " set cleanupDiskPending to true");
                        this.cleanupDiskPending = true;
                    }
                }

            }
        }
    }

    /**
     * Call this method to check whether a specified id exists in the invalidation explicit or scan buffer.
     * 
     * @param id
     *            - cache id.
     * @return boolean - true means a specified id is found.
     */
    protected synchronized boolean contains(Object id) {
        boolean found = false;
        if (this.explicitBuffer.containsKey(id) || this.scanBuffer.contains(id)) {
            found = true;
        }
        return found;
    }

    /**
     * Call this method to check whether a "full" condition is met to start LPBT.
     * 
     * @return boolean - true means the condition "full".
     */
    @Trivial
    public synchronized boolean isFull() {
        // final String methodName = "isFull()";
        boolean isFull = false;
        int size = this.explicitBuffer.size() + this.scanBuffer.size() + this.garbageCollectorBuffer.size();
        if (size > this.maxInvalidationBufferSize || (System.currentTimeMillis() - this.lastRemoveTime) >= this.maxInvalidationBufferLife) {
            isFull = true;
            setlastRemoveTime();
        }
        // if (isFull) {
        // traceDebug(methodName, "cacheName=" + this.cod.cacheName + " isFull=" + isFull + " explicitBuffer=" +
        // explicitBuffer.size() + " scanBuffer=" + this.scanBuffer.size());
        // }
        return isFull;
    }

    /**
     * Call this method to filter out the specified collection of cache ids based on invalidation buffers.
     * 
     * @param filterValueSet
     *            - a collection of cache ids,
     */
    protected synchronized void filter(ValueSet filterValueSet) {
        boolean explicitBufferEmpty = this.explicitBuffer.isEmpty();
        boolean scanBufferEmpty = this.scanBuffer.isEmpty();
        if (filterValueSet != null && !filterValueSet.isEmpty() && (!explicitBufferEmpty || !scanBufferEmpty)) {
            Iterator it = filterValueSet.iterator();
            while (it.hasNext()) {
                Object o = it.next();
                if (!explicitBufferEmpty && this.explicitBuffer.containsKey(o)) {
                    it.remove();
                } else if (!scanBufferEmpty && this.scanBuffer.contains(o)) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Call this method to filter out the specified collection of cache ids based on another collection of cache ids.
     * 
     * @param filterValueSet
     *            - a collection of cache ids to be filtered,
     * @param valueSet
     *            - a collection of cache ids,
     */
    private void filter(ValueSet filterValueSet, ValueSet valueSet) {
        if (filterValueSet != null && valueSet != null && !filterValueSet.isEmpty() && !valueSet.isEmpty()) {
            Iterator it = filterValueSet.iterator();
            while (it.hasNext()) {
                Object o = it.next();
                if (valueSet.contains(o)) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Call this method to filter out the specified collection of cache ids based on another collection of cache ids.
     * 
     * @param filterValueSet
     *            - a collection of cache ids to be filtered,
     * @param valueSet
     *            - a collection of cache ids,
     */
    private void filter(ValueSet filterValueSet, HashMap hashmap) {
        if (filterValueSet != null && hashmap != null && !filterValueSet.isEmpty() && !hashmap.isEmpty()) {
            Iterator it = filterValueSet.iterator();
            while (it.hasNext()) {
                Object o = it.next();
                if (hashmap.containsKey(o)) {
                    it.remove();
                }
            }
        }
    }

    /**
     * Call this method to get the size from one of invalidation buffers.
     * 
     * @param eventAlreadyFired
     *            - boolean to select which invalidation buffer is used to get the size.
     * @return int - the size.
     */
    @Trivial
    protected synchronized int size(int bufferType) {
        if (bufferType == EXPLICIT_BUFFER) {
            return this.explicitBuffer.size();
        } else if (bufferType == SCAN_BUFFER) {
            return this.scanBuffer.size();
        } else if (bufferType == GC_BUFFER) {
            return this.garbageCollectorBuffer.size();
        }
        return 0;
    }

    /**
     * Call this method to get the total size of invalidation explicit and scan buffers .
     * 
     * @return int - the size.
     */
    @Trivial
    protected synchronized int size() {
        return this.explicitBuffer.size() + this.scanBuffer.size();
    }

    /**
     * Call this method to check the state of "Cleanup Pending".
     * 
     * @return boolean - the state.
     */
    protected synchronized boolean isCleanupPending() {
        return this.cleanupDiskPending;
    }

    /**
     * Call this method to reset the state of "Cleanup Pending" to false.
     */
    protected synchronized void resetCleanupPending() {
        final String methodName = "resetCleanupPending()";
        this.cleanupDiskPending = false;
        traceDebug(methodName, "cacheName=" + this.cod.cacheName);
    }

    /**
     * Call this method to check the state of "LPBT in Progress".
     * 
     * @return boolean - the state.
     */
    protected synchronized boolean isBackgroundInvalidationInProgress() {
        boolean cleanupThreadRunning = false;
        if (this.cod.diskCleanupThread != null) {
            synchronized (cod.diskCleanupThread.dcMonitor) {
                cleanupThreadRunning = this.cod.diskCleanupThread.currentThread != null;
            }
        }
        boolean garbageCollectorThreadRunning = false;
        if (this.cod.garbageCollectionThread != null) {
            synchronized (cod.garbageCollectionThread.gcMonitor) {
                garbageCollectorThreadRunning = this.cod.garbageCollectionThread.currentThread != null;
            }
        }

        return (cleanupThreadRunning || garbageCollectorThreadRunning) ? true : false;
    }

    /**
     * Call this method to check the state of "Loop Once".
     * 
     * @return boolean - the state.
     */
    protected synchronized boolean isLoopOnce() {
        final String methodName = "isLoopOnce()";
        if (loopOnce) {
            traceDebug(methodName, "cacheName=" + this.cod.cacheName + " isLoopOnce=" + loopOnce + " explicitBuffer=" + explicitBuffer.size()
                    + " scanBuffer=" + this.scanBuffer.size());
        }
        return this.loopOnce;
    }

    /**
     * Call this method to set the state of "Loop Once". It is using in the LPBT.
     * 
     * @param loopOnce
     *            - boolean to set true or false.
     */
    protected synchronized void setLoopOnce(boolean loopOnce) {
        final String methodName = "setLoopOnce()";
        this.loopOnce = loopOnce;
        traceDebug(methodName, "cacheName=" + this.cod.cacheName + " loopOnce=" + loopOnce + " explicitBuffer=" + explicitBuffer.size()
                + " scanBuffer=" + this.scanBuffer.size());
    }

    /**
     * Call this method to set the last remove time.
     */
    @Trivial
    protected synchronized void setlastRemoveTime() {
        this.lastRemoveTime = System.currentTimeMillis();
    }

    /**
     * Call this method to set the state of "Stopping". No more invoking the LPBT.
     * 
     * @param stopping
     *            - boolean to set true or false.
     */
    protected synchronized void setStopping(boolean stopping) {
        final String methodName = "setStopping()";
        this.stopping = stopping;
        traceDebug(methodName, "cacheName=" + this.cod.cacheName + " stopping=" + this.stopping);
    }

    /**
     * Call this method to check the state of "Disk Clear In Progress".
     * 
     * @return boolean - the state.
     */
    protected synchronized boolean isDiskClearInProgress() {
        return this.diskClearInProgress;
    }

    /**
     * Call this method to set the state of "Disk Clear In Progress". It is using in the disk clear.
     * 
     * @param inProgress
     *            - boolean to set true or false.
     */
    protected synchronized void setDiskClearInProgress(boolean inProgress) {
        // final String methodName = "setDiskClearInProgress()";
        this.diskClearInProgress = inProgress;
        // traceDebug(methodName, "cacheName=" + this.cod.cacheName + " diskClearInProgress=" +
        // this.diskClearInProgress);
    }

    private void traceDebug(String methodName, String message) {
        if (IS_UNIT_TEST) {
            System.out.println(this.getClass().getName() + "." + methodName + " " + message);
        } else {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " " + message);
            }
        }
    }
}