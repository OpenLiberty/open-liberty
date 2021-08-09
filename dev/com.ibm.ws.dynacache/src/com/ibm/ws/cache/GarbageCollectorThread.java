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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

public class GarbageCollectorThread implements java.lang.Runnable {

    private static TraceComponent tc = Tr.register(GarbageCollectorThread.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    CacheOnDisk cod;
    HTODDynacache htod;
    int GCType;

    Future<?> currentThread;
    boolean processGC;
    final Object gcMonitor = new Object() {};
    long totalDeleted;
    long totalDeletedSize;

    public GarbageCollectorThread(CacheOnDisk cod) {
        this.cod = cod;
        this.htod = cod.htod;
        this.processGC = false;
        this.totalDeleted = 0;
    }

    @Override
    public void run() {

        if (tc.isDebugEnabled()) {
            Tr.entry(tc, "run");
        }

        final String methodName = "GarbageCollectorThread.run()";

        for (;;) {
            synchronized (gcMonitor) {
                if (this.processGC == false) {
                    currentThread = null;
                    break;
                }

                this.processGC = false;
            } // giving up the monitor for other threads

            if (this.cod.evictionPolicy != CacheConfig.EVICTION_NONE) {
                ArrayList evictionList = null;

                if (this.GCType == CacheOnDisk.DISK_CACHE_SIZE_IN_ENTRIES_TYPE) {
                    int deleteEntries = this.cod.getCacheIdsSize(CacheOnDisk.FILTER)
                                        - this.cod.htod.invalidationBuffer.size(HTODInvalidationBuffer.GC_BUFFER)
                                        - this.cod.diskCacheSizeInfo.diskCacheSizeLowLimit;
                    evictionList = this.htod.walkEvictionTable(this.cod.evictionPolicy, deleteEntries, 0);
                    if (tc.isEventEnabled()) {
                        Tr.event(tc, methodName + " cacheName=" + this.cod.cacheName + " size exceeding high threshold limit of "
                                     + this.cod.diskCacheSizeInfo.highThreshold + "%." + " Disk cache garbage collector evicting " + evictionList.size()
                                     + " entries;  Request entries=" + deleteEntries);
                    } else {
                        traceDebug(methodName, "cacheName=" + this.cod.cacheName + " size exceeding high threshold limit of "
                                               + this.cod.diskCacheSizeInfo.highThreshold + "%." + " Disk cache garbage collector evicting " + evictionList.size()
                                               + " entries;  Request entries=" + deleteEntries);
                    }
                } else if (this.GCType == CacheOnDisk.DISK_CACHE_SIZE_IN_BYTES_TYPE) {
                    long deleteSize = this.cod.getCacheSizeInBytes() - this.cod.diskCacheSizeInfo.getDiskCacheSizeInBytesLowLimit();
                    evictionList = this.htod.walkEvictionTable(this.cod.evictionPolicy, 0, deleteSize);

                    int size = (evictionList != null) ? evictionList.size() : 0;
                    if (tc.isEventEnabled()) {
                        Tr.event(tc, methodName + " cacheName=" + this.cod.cacheName + " size in GB exceeding high threshold limit of "
                                     + this.cod.diskCacheSizeInfo.highThreshold + "%." + " Disk cache garbage collector evicting " + size
                                     + " entries;  Request size=" + deleteSize + " bytes");
                    } else {
                        traceDebug(methodName, "cacheName=" + this.cod.cacheName + " size in GB exceeding high threshold limit of "
                                               + this.cod.diskCacheSizeInfo.highThreshold + "%." + " Disk cache garbage collector evicting " + size
                                               + " entries;  Request size=" + deleteSize + " bytes");
                    }
                }
                if (evictionList != null && evictionList.size() > 0) {
                    this.htod.invalidationBuffer.add(evictionList, HTODInvalidationBuffer.GC_BUFFER);
                }

                long t = System.nanoTime();
                Result res = htod.deleteEntriesFromInvalidationBuffer(false);
                long currentDeleteTime = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - t);
                totalDeleted += (res.numExplicitDeleted + res.numScanDeleted + res.numGCDeleted);
                totalDeletedSize += res.deletedSize;

                StringBuffer dcmsg = new StringBuffer();
                dcmsg.append("  ReturnCode=");
                dcmsg.append(res.returnCode);
                dcmsg.append("  DeleteTime=");
                dcmsg.append(currentDeleteTime);
                dcmsg.append("  ExplicitEntriesDeleted=");
                dcmsg.append(res.numExplicitDeleted);
                dcmsg.append("  ScanEntriesDeleted=");
                dcmsg.append(res.numScanDeleted);
                dcmsg.append("  GCEntriesDeleted=");
                dcmsg.append(res.numGCDeleted);
                dcmsg.append("  DeletedSize=");
                dcmsg.append(res.deletedSize);
                dcmsg.append("  totalDeleted=");
                dcmsg.append(this.totalDeleted);
                dcmsg.append("  totalDeletedSize=");
                dcmsg.append(this.totalDeletedSize);

                htod.returnToResultPool(res);

                if (tc.isEventEnabled()) {
                    Tr.event(tc, methodName + " The garbage collector finished for cache name \"" + this.cod.cacheName + "\"."
                                 + " The statistics are: " + dcmsg.toString());
                } else {
                    traceDebug(methodName, "The garbage collector finished for cache name \"" + this.cod.cacheName + "\"."
                                           + " The statistics are: " + dcmsg.toString());
                }

            }
        }

        if (tc.isDebugEnabled()) {
            Tr.exit(tc, "run");
        }
    }

    private void traceDebug(String methodName, String message) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " " + message);
        }
    }
}
