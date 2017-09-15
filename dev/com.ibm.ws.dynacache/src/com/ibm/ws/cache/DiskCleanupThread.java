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

import java.util.concurrent.Future;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.util.ExceptionUtility;

public class DiskCleanupThread implements java.lang.Runnable {

    private static TraceComponent tc = Tr.register(DiskCleanupThread.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    CacheOnDisk cod;
    HTODDynacache htod;
    Future<?> currentThread;
    boolean processDiskCleanup;
    boolean stopped = false;
    final Object dcMonitor = new Object() {
    };
    boolean scan;

    public DiskCleanupThread(CacheOnDisk cod) {
        this.cod = cod;
        this.htod = cod.htod;
        this.processDiskCleanup = false;
        this.scan = false;
    }

    public void run() {
        final String methodName = "DiskCleanupThread.run()";
        for (;;) {
            synchronized (dcMonitor) {
                if (this.processDiskCleanup == false) {
                    currentThread = null;
                    break;
                }

                this.processDiskCleanup = false;
            } // giving up the monitor for other threads

            if (stopped)
                break;
            
            try {
                if (cleanUpHTOD(this.scan) == HTODDynacache.DISK_EXCEPTION) {
                    cod.stopOnError(this.cod.htod.diskCacheException);
                    stopped = true;
                } else {
                    if (cod.htod.invalidationBuffer.isCleanupPending()) {
                        cod.htod.invalidationBuffer.resetCleanupPending();
                        this.scan = true;
                        if (cleanUpHTOD(HTODInvalidationBuffer.SCAN) == HTODDynacache.DISK_EXCEPTION) {
                            cod.stopOnError(this.cod.htod.diskCacheException);
                            stopped = true;
                        }
                    }
                }
            } finally {
                cod.populateEvictionTable = false;
                cod.htod.invalidationBuffer.setlastRemoveTime();
                synchronized (cod.diskCacheMonitor) {
                    if (cod.doNotify) {
                        traceDebug(methodName, "notify completion for cache name \"" + this.cod.cacheName);
                        cod.diskCacheMonitor.notifyAll();
                    }
                }
                cod.htod.invalidationBuffer.resetCleanupPending();
            }
        }
    }

    protected int cleanUpHTOD(boolean scan) {
        final String methodName = "cleanUpHTOD()";
        int returnCode = HTODDynacache.NO_EXCEPTION;
        if (htod != null) {
            try {
                returnCode = htod.removeExpiredCache(scan);
                if (returnCode == HTODDynacache.DISK_EXCEPTION) {
                    return returnCode;
                }
                if (scan) {
                    cod.updateLastScanFile();
                }
            } catch (Throwable t) {
                com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.DiskCleanupTask.cleanupHTOD", "96", this);
                traceDebug(methodName, "cacheName=" + cod.cacheName + "\nException: " + ExceptionUtility.getStackTrace(t));
            }
        }
        // Set alarm when
        // (1) Server is NOT stopped AND
        // (2) CleanupPending is false AND
        // (3) It just finished running disk scan AND
        // (4) diskCachePerformanceLevel is NOT HIGH
        if (scan && cod.diskCachePerformanceLevel != CacheConfig.HIGH && !stopped && !this.htod.invalidationBuffer.isCleanupPending()) {
            if (cod.cleanupFrequency == 0) {
                cod.sleepTime = cod.calculateSleepTime();
            }
            traceDebug(methodName, "cacheName=" + cod.cacheName + " set alarm sleepTime=" + cod.sleepTime);
            Scheduler.createNonDeferrable(cod.sleepTime, cod, this);
        } else {
            if (scan && cod.diskCachePerformanceLevel != CacheConfig.HIGH) {
                traceDebug(methodName, "cacheName=" + cod.cacheName + " alarm is not enabled now. stopped=" + stopped + " cleanupPending="
                        + this.htod.invalidationBuffer.isCleanupPending());
            }
        }
        return returnCode;
    }

    private void traceDebug(String methodName, String message) {
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " " + message);
        }
    }
}
