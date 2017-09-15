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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.intf.CacheStatisticsListener;
import com.ibm.ws.cache.stat.CachePerf;

public class CacheStatisticsListenerImpl implements CacheStatisticsListener {

    private static TraceComponent tc = Tr.register(CacheStatisticsListenerImpl.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    private long cacheHits = 0;
    private long cacheMisses = 0;

    private long memoryCacheHits = 0;
    private long diskCacheHits = 0;

    private long cacheRemoves = 0;
    private long cacheLruRemoves = 0;

    private long overflowEntriesFromMemory = 0; //PK33017

    private long explicitInvalidationsFromMemory = 0;
    private long explicitInvalidationsFromDisk = 0;
    private long explicitInvalidationsLocal = 0;
    private long explicitInvalidationsRemote = 0;
    private long timeoutInvalidationsFromMemory = 0;
    private long timeoutInvalidationsFromDisk = 0;
    private long garbageCollectorInvalidationsFromDisk = 0;
    private long overflowInvalidationsFromDisk = 0;

    private long depIdsOffloadedToDisk = 0;
    private long depIdBasedInvalidationsFromDisk = 0;
    private long templatesOffloadedToDisk = 0;
    private long templateBasedInvalidationsFromDisk = 0;

    private long objectsReadFromDisk = 0;
    private long objectsReadFromDisk4K = 0;
    private long objectsReadFromDisk40K = 0;
    private long objectsReadFromDisk400K = 0;
    private long objectsReadFromDisk4000K = 0;
    private long objectsReadFromDiskSize = 0;

    private long objectsWriteToDisk = 0;
    private long objectsWriteToDisk4K = 0;
    private long objectsWriteToDisk40K = 0;
    private long objectsWriteToDisk400K = 0;
    private long objectsWriteToDisk4000K = 0;
    private long objectsWriteToDiskSize = 0;

    private long objectsDeleteFromDisk = 0;
    private long objectsDeleteFromDisk4K = 0;
    private long objectsDeleteFromDisk40K = 0;
    private long objectsDeleteFromDisk400K = 0;
    private long objectsDeleteFromDisk4000K = 0;
    private long objectsDeleteFromDiskSize = 0;

    private long remoteInvalidationNotifications = 0;
    private long remoteUpdateNotifications = 0;
    private long remoteObjectUpdates = 0;
    private long remoteObjectUpdateSize = 0;
    private long remoteObjectHits = 0;
    private long remoteObjectFetchSize = 0;
    private long remoteObjectMisses = 0;

    private long objectsAsyncLruToDisk = 0;

    private final Object diskReadMonitor = new Object();
    private final String cacheName;

    public CacheStatisticsListenerImpl(String cacheName) {
        this.cacheName = cacheName;
    }

    @Override
    public final void localCacheHit(Object id, int locality) {
        this.cacheHits++;
        if (locality == CachePerf.MEMORY) {
            this.memoryCacheHits++;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, this.cacheName + ": Local cache Hit: " + id + " memoryCacheHits=" + this.memoryCacheHits + " cacheHits=" + this.cacheHits);
            }
        } else {
            this.diskCacheHits++;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, this.cacheName + ": Local cache Hit: " + id + " diskCacheHits=" + this.diskCacheHits + " cacheHits=" + this.cacheHits);
            }
        }
    }

    @Override
    public final void cacheMiss(Object id) {
        this.cacheMisses++;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.cacheName + ": Cache Miss: id=" + id + " cacheMisses=" + cacheMisses);
        }
    }

    @Override
    public void start() {}

    @Override
    public final void remove(Object id, int cause, int locality, int source) {
        switch (cause) {
            case CachePerf.DIRECT:
                this.cacheRemoves++;
                if (locality == CachePerf.DISK) {
                    this.explicitInvalidationsFromDisk++;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, this.cacheName + "Remove id=" + id + " explicitInvalidationsFromDisk=" + this.explicitInvalidationsFromDisk);
                    }
                } else {
                    this.explicitInvalidationsFromMemory++;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, this.cacheName + "Remove id=" + id + " explicitInvalidationsFromMemory=" + this.explicitInvalidationsFromMemory);
                    }
                }
                if (source == CachePerf.REMOTE) {
                    this.explicitInvalidationsRemote++;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, this.cacheName + "Remove id=" + id + " explicitInvalidationsRemote=" + this.explicitInvalidationsRemote);
                    }
                } else {
                    this.explicitInvalidationsLocal++;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, this.cacheName + "Remove id=" + id + " explicitInvalidationsLocal=" + this.explicitInvalidationsLocal);
                    }
                }
                break;
            case CachePerf.LRU:
                this.cacheLruRemoves++;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, this.cacheName + "Remove id=" + id + " cacheLruRemoves=" + this.cacheLruRemoves);
                }
                break;
            case CachePerf.TIMEOUT:
                this.cacheRemoves++;
                if (locality == CachePerf.DISK) {
                    this.timeoutInvalidationsFromDisk++;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, this.cacheName + "Remove id=" + id + " timeoutInvalidationsFromDisk=" + this.timeoutInvalidationsFromDisk);
                    }
                } else {
                    this.timeoutInvalidationsFromMemory++;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, this.cacheName + "Remove id=" + id + " timeoutInvalidationsFromMemory=" + this.timeoutInvalidationsFromMemory);
                    }
                }
                break;
            case CachePerf.DISK_GARBAGE_COLLECTOR:
                //this.numRemoves++;
                this.garbageCollectorInvalidationsFromDisk++;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, this.cacheName + "Remove id=" + id + " garbageCollectorInvalidationsFromDisk=" + this.garbageCollectorInvalidationsFromDisk);
                }
                break;
            case CachePerf.DISK_OVERFLOW:
                //this.numRemoves++;
                this.overflowInvalidationsFromDisk++;
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, this.cacheName + "Remove id=" + id + " overflowInvalidationsFromDisk=" + this.overflowInvalidationsFromDisk);
                }
                break;
            default:
                break;
        }
    }

    @Override
    public final void depIdsOffloadedToDisk(Object did) {
        this.depIdsOffloadedToDisk++;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.cacheName + ": dependency ID offload to disk  did=" + did + " depIdsOffloadedToDisk=" + this.depIdsOffloadedToDisk);
        }
    }

    @Override
    public final void depIdBasedInvalidationsFromDisk(Object did) {
        this.depIdBasedInvalidationsFromDisk++;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.cacheName + ": dependency ID based invalidation  did=" + did + " depIdBasedInvalidationsFromDisk=" + this.depIdBasedInvalidationsFromDisk);
        }
    }

    @Override
    public final void templatesOffloadedToDisk(Object template) {
        this.templatesOffloadedToDisk++;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.cacheName + ": template offload to disk template=" + template + " templatesOffloadedToDisk=" + this.templatesOffloadedToDisk);
        }
    }

    @Override
    public final void templateBasedInvalidationsFromDisk(Object template) {
        this.templateBasedInvalidationsFromDisk++;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.cacheName + ": template based invalidation  template=" + template + " templateBasedInvalidationsFromDisk=" + this.templateBasedInvalidationsFromDisk);
        }
    }

    @Override
    public final void remoteInvalidationNotifications(Object id) {
        this.remoteInvalidationNotifications++;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.cacheName + ": remote invalidation notification id=" + id + " remoteInvalidationNotifications=" + this.remoteInvalidationNotifications);
        }
    }

    @Override
    public final void remoteUpdateNotifications(Object id) {
        this.remoteUpdateNotifications++;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.cacheName + ": remote update notification id=" + id + " remoteUpdateNotifications=" + this.remoteUpdateNotifications);
        }
    }

    @Override
    public final void remoteUpdateNotifications(int numNotifications) {
        this.remoteUpdateNotifications += numNotifications;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.cacheName + ": remote update notification numNotifications=" + numNotifications + " remoteUpdateNotifications=" + this.remoteUpdateNotifications);
        }
    }

    @Override
    public final void remoteObjectUpdates(Object id, int valueSize) {
        this.remoteObjectUpdates++;
        this.remoteObjectUpdateSize += valueSize;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.cacheName + ": remote object update id=" + id + " valueSize=" + valueSize + " remoteObjectUpdates=" + this.remoteObjectUpdates
                         + " remoteObjectUpdateSize=" + this.remoteObjectUpdateSize);
        }
    }

    @Override
    public final void remoteObjectHits(Object id, int valueSize) {
        this.cacheHits++;
        this.remoteObjectHits++;
        this.remoteObjectFetchSize += valueSize;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.cacheName + ": remote object hit id=" + id + " valueSize=" + valueSize + " cacheHits=" + this.cacheHits + " remoteObjectHits="
                         + this.remoteObjectHits + " remoteObjectFetchSize=" + this.remoteObjectFetchSize);
        }
    }

    @Override
    public final void remoteObjectMisses(Object id) {
        this.cacheMisses++;
        this.remoteObjectMisses++;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.cacheName + ": remote object miss id=" + id + " cacheMisses=" + this.cacheMisses + " remoteObjectMisses=" + this.remoteObjectMisses);
        }
    }

    @Override
    public final void readEntryFromDisk(Object id, int valueSize) {
        synchronized (this.diskReadMonitor) {
            this.objectsReadFromDisk++;
            this.objectsReadFromDiskSize += valueSize;
            if (valueSize <= 4000) {
                this.objectsReadFromDisk4K++;
            } else if (valueSize <= 40000) {
                this.objectsReadFromDisk40K++;
            } else if (valueSize <= 400000) {
                this.objectsReadFromDisk400K++;
            } else if (valueSize <= 4000000) {
                this.objectsReadFromDisk4000K++;
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.cacheName + ": read entry from disk id=" + id + " valueSize=" + valueSize + " objectsReadFromDisk=" + this.objectsReadFromDisk
                         + " objectsReadFromDiskSize" + this.objectsReadFromDiskSize);
        }
    }

    @Override
    public final void writeEntryToDisk(Object id, int valueSize) {
        this.objectsWriteToDisk++;
        this.objectsWriteToDiskSize += valueSize;
        if (valueSize <= 4000) {
            this.objectsWriteToDisk4K++;
        } else if (valueSize <= 40000) {
            this.objectsWriteToDisk40K++;
        } else if (valueSize <= 400000) {
            this.objectsWriteToDisk400K++;
        } else if (valueSize <= 4000000) {
            this.objectsWriteToDisk4000K++;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.cacheName + ": write entry to disk id=" + id + " valueSize=" + valueSize + " objectsWriteToDisk=" + this.objectsWriteToDisk
                         + " objectsWriteToDiskSize=" + this.objectsWriteToDiskSize);
        }
    }

    @Override
    public final void deleteEntryFromDisk(Object id, int valueSize) {
        this.objectsDeleteFromDisk++;
        this.objectsDeleteFromDiskSize += valueSize;
        if (valueSize <= 4000) {
            this.objectsDeleteFromDisk4K++;
        } else if (valueSize <= 40000) {
            this.objectsDeleteFromDisk40K++;
        } else if (valueSize <= 400000) {
            this.objectsDeleteFromDisk400K++;
        } else if (valueSize <= 4000000) {
            this.objectsDeleteFromDisk4000K++;
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.cacheName + ": delete entry from disk id=" + id + " valueSize=" + valueSize + " objectsDeleteFromDisk=" + this.objectsDeleteFromDisk
                         + " objectsDeleteFromDiskSize=" + this.objectsDeleteFromDiskSize);
        }
    }

    @Override
    public final void objectsAsyncLruToDisk() {
        this.objectsAsyncLruToDisk++;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.cacheName + ": objectsAsyncLruToDisk=" + this.objectsAsyncLruToDisk);
        }
    }

    @Override
    public final void overflowEntriesFromMemory() { //PK33017
        this.overflowEntriesFromMemory++;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, this.cacheName + ": overflowEntriesFromMemory=" + this.overflowEntriesFromMemory);
        }
    }

    @Override
    public final void reset() {
        cacheHits = 0;
        cacheMisses = 0;
        cacheRemoves = 0;
        cacheLruRemoves = 0;
        overflowEntriesFromMemory = 0;
        memoryCacheHits = 0;
        diskCacheHits = 0;

        explicitInvalidationsFromMemory = 0;
        explicitInvalidationsFromDisk = 0;
        explicitInvalidationsLocal = 0;
        explicitInvalidationsRemote = 0;
        timeoutInvalidationsFromMemory = 0;
        timeoutInvalidationsFromDisk = 0;
        garbageCollectorInvalidationsFromDisk = 0;
        overflowInvalidationsFromDisk = 0;

        depIdsOffloadedToDisk = 0;
        depIdBasedInvalidationsFromDisk = 0;
        templatesOffloadedToDisk = 0;
        templateBasedInvalidationsFromDisk = 0;

        objectsReadFromDisk = 0;
        objectsReadFromDisk4K = 0;
        objectsReadFromDisk40K = 0;
        objectsReadFromDisk400K = 0;
        objectsReadFromDisk4000K = 0;
        objectsReadFromDiskSize = 0;

        objectsWriteToDisk = 0;
        objectsWriteToDisk4K = 0;
        objectsWriteToDisk40K = 0;
        objectsWriteToDisk400K = 0;
        objectsWriteToDisk4000K = 0;
        objectsWriteToDiskSize = 0;

        objectsDeleteFromDisk = 0;
        objectsDeleteFromDisk4K = 0;
        objectsDeleteFromDisk40K = 0;
        objectsDeleteFromDisk400K = 0;
        objectsDeleteFromDisk4000K = 0;
        objectsDeleteFromDiskSize = 0;

        remoteInvalidationNotifications = 0;
        remoteUpdateNotifications = 0;
        remoteObjectUpdates = 0;
        remoteObjectUpdateSize = 0;
        remoteObjectHits = 0;
        remoteObjectFetchSize = 0;
        remoteObjectMisses = 0;

        objectsAsyncLruToDisk = 0;
    }

    @Override
    public final void resetMemory() {
        cacheHits = 0;
        cacheMisses = 0;
        cacheRemoves = 0;
        cacheLruRemoves = 0;
        overflowEntriesFromMemory = 0;

        memoryCacheHits = 0;

        explicitInvalidationsFromMemory = 0;
        explicitInvalidationsLocal = 0;
        explicitInvalidationsRemote = 0;
        timeoutInvalidationsFromMemory = 0;

        remoteInvalidationNotifications = 0;
        remoteUpdateNotifications = 0;
        remoteObjectUpdates = 0;
        remoteObjectUpdateSize = 0;
        remoteObjectHits = 0;
        remoteObjectFetchSize = 0;
        remoteObjectMisses = 0;
    }

    @Override
    public final void resetDisk() {

        diskCacheHits = 0;
        explicitInvalidationsFromDisk = 0;
        timeoutInvalidationsFromDisk = 0;
        garbageCollectorInvalidationsFromDisk = 0;
        overflowInvalidationsFromDisk = 0;

        depIdsOffloadedToDisk = 0;
        depIdBasedInvalidationsFromDisk = 0;
        templatesOffloadedToDisk = 0;
        templateBasedInvalidationsFromDisk = 0;

        objectsReadFromDisk = 0;
        objectsReadFromDisk4K = 0;
        objectsReadFromDisk40K = 0;
        objectsReadFromDisk400K = 0;
        objectsReadFromDisk4000K = 0;
        objectsReadFromDiskSize = 0;

        objectsWriteToDisk = 0;
        objectsWriteToDisk4K = 0;
        objectsWriteToDisk40K = 0;
        objectsWriteToDisk400K = 0;
        objectsWriteToDisk4000K = 0;
        objectsWriteToDiskSize = 0;

        objectsDeleteFromDisk = 0;
        objectsDeleteFromDisk4K = 0;
        objectsDeleteFromDisk40K = 0;
        objectsDeleteFromDisk400K = 0;
        objectsDeleteFromDisk4000K = 0;
        objectsDeleteFromDiskSize = 0;

        objectsAsyncLruToDisk = 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getCacheHits()
     */
    @Override
    public long getCacheHitsCount() {
        return cacheHits;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getCacheLruRemoves()
     */
    @Override
    public long getCacheLruRemovesCount() {
        return cacheLruRemoves;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getCacheMisses()
     */
    @Override
    public long getCacheMissesCount() {
        return cacheMisses;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getCacheRemoves()
     */
    @Override
    public long getCacheRemovesCount() {
        return cacheRemoves;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getDepIdBasedInvalidationsFromDisk()
     */
    @Override
    public long getDepIdBasedInvalidationsFromDiskCount() {
        return depIdBasedInvalidationsFromDisk;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getDepIdsOffloadedToDisk()
     */
    @Override
    public long getDepIdsOffloadedToDiskCount() {
        return depIdsOffloadedToDisk;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getLocalCacheHits()
     */
    @Override
    public long getDiskCacheHitsCount() {
        return diskCacheHits;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getExplicitInvalidationsFromDisk()
     */
    @Override
    public long getExplicitInvalidationsFromDiskCount() {
        return explicitInvalidationsFromDisk;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getExplicitInvalidationsFromMemory()
     */
    @Override
    public long getExplicitInvalidationsFromMemoryCount() {
        return explicitInvalidationsFromMemory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getExplicitInvalidationsLocal()
     */
    @Override
    public long getExplicitInvalidationsLocalCount() {
        return explicitInvalidationsLocal;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getExplicitInvalidationsRemote()
     */
    public long getExplicitInvalidationsRemoteCount() {
        return explicitInvalidationsRemote;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getGarbageCollectorInvalidationsFromDisk()
     */
    public long getGarbageCollectorInvalidationsFromDiskCount() {
        return garbageCollectorInvalidationsFromDisk;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getLocalCacheHits()
     */
    public long getMemoryCacheHitsCount() {
        return memoryCacheHits;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsAsyncLruToDisk()
     */
    public long getObjectsAsyncLruToDiskCount() {
        return objectsAsyncLruToDisk;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsDeleteFromDisk()
     */
    public long getObjectsDeleteFromDiskCount() {
        return objectsDeleteFromDisk;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsDeleteFromDisk4000K()
     */
    public long getObjectsDeleteFromDisk4000KCount() {
        return objectsDeleteFromDisk4000K;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsDeleteFromDisk400K()
     */
    public long getObjectsDeleteFromDisk400KCount() {
        return objectsDeleteFromDisk400K;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsDeleteFromDisk40K()
     */
    public long getObjectsDeleteFromDisk40KCount() {
        return objectsDeleteFromDisk40K;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsDeleteFromDisk4K()
     */
    public long getObjectsDeleteFromDisk4KCount() {
        return objectsDeleteFromDisk4K;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsDeleteFromDiskSize()
     */
    public long getObjectsDeleteFromDiskSizeCount() {
        return objectsDeleteFromDiskSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsReadFromDisk()
     */
    public long getObjectsReadFromDiskCount() {
        return objectsReadFromDisk;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsReadFromDisk4000K()
     */
    public long getObjectsReadFromDisk4000KCount() {
        return objectsReadFromDisk4000K;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsReadFromDisk400K()
     */
    public long getObjectsReadFromDisk400KCount() {
        return objectsReadFromDisk400K;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsReadFromDisk40K()
     */
    public long getObjectsReadFromDisk40KCount() {
        return objectsReadFromDisk40K;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsReadFromDisk4K()
     */
    public long getObjectsReadFromDisk4KCount() {
        return objectsReadFromDisk4K;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsReadFromDiskSize()
     */
    public long getObjectsReadFromDiskSizeCount() {
        return objectsReadFromDiskSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsWriteToDisk()
     */
    public long getObjectsWriteToDiskCount() {
        return objectsWriteToDisk;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsWriteToDisk4000K()
     */
    public long getObjectsWriteToDisk4000KCount() {
        return objectsWriteToDisk4000K;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsWriteToDisk400K()
     */
    public long getObjectsWriteToDisk400KCount() {
        return objectsWriteToDisk400K;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsWriteToDisk40K()
     */
    public long getObjectsWriteToDisk40KCount() {
        return objectsWriteToDisk40K;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsWriteToDisk4K()
     */
    public long getObjectsWriteToDisk4KCount() {
        return objectsWriteToDisk4K;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getObjectsWriteToDiskSize()
     */
    public long getObjectsWriteToDiskSizeCount() {
        return objectsWriteToDiskSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getOverflowEntriesFromMemory()
     */
    public long getOverflowEntriesFromMemoryCount() {
        return overflowEntriesFromMemory;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getOverflowInvalidationsFromDisk()
     */
    public long getOverflowInvalidationsFromDiskCount() {
        return overflowInvalidationsFromDisk;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getRemoteInvalidationNotifications()
     */
    public long getRemoteInvalidationNotificationsCount() {
        return remoteInvalidationNotifications;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getRemoteObjectFetchSize()
     */
    public long getRemoteObjectFetchSizeCount() {
        return remoteObjectFetchSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getRemoteObjectHits()
     */
    public long getRemoteObjectHitsCount() {
        return remoteObjectHits;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getRemoteObjectMisses()
     */
    public long getRemoteObjectMissesCount() {
        return remoteObjectMisses;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getRemoteObjectUpdates()
     */
    public long getRemoteObjectUpdatesCount() {
        return remoteObjectUpdates;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getRemoteObjectUpdateSize()
     */
    public long getRemoteObjectUpdateSizeCount() {
        return remoteObjectUpdateSize;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getRemoteUpdateNotifications()
     */
    public long getRemoteUpdateNotificationsCount() {
        return remoteUpdateNotifications;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getTemplateBasedInvalidationsFromDisk()
     */
    public long getTemplateBasedInvalidationsFromDiskCount() {
        return templateBasedInvalidationsFromDisk;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getTemplatesOffloadedToDisk()
     */
    public long getTemplatesOffloadedToDiskCount() {
        return templatesOffloadedToDisk;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getTimeoutInvalidationsFromDisk()
     */
    public long getTimeoutInvalidationsFromDiskCount() {
        return timeoutInvalidationsFromDisk;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.DCacheStatisticsListener#getTimeoutInvalidationsFromMemory()
     */
    public long getTimeoutInvalidationsFromMemoryCount() {
        return timeoutInvalidationsFromMemory;
    }

}
