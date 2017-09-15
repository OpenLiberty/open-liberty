/*******************************************************************************
 * Copyright (c) 1997, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import com.ibm.websphere.cache.ChangeEvent;
import com.ibm.websphere.cache.InvalidationEvent;
import com.ibm.websphere.cache.exception.DiskCacheUsingOldFormatException;
import com.ibm.websphere.cache.exception.DiskIOException;
import com.ibm.websphere.cache.exception.DynamicCacheException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cache.stat.CachePerf;
import com.ibm.ws.cache.util.ObjectSizer;
import com.ibm.wsspi.cache.CacheStatistics;

/**
 * 
 * This is the underlying cache mechanism that is used by the JSP/Servlet cache. It contains the methods used to inspect
 * and manage the current state of the cache. <br>
 * 
 * <pre>
 * DRS Design Note: Cache sends messages to DRSRemoteServices.
 * DRSRemoteServices sends messages to DRSNotificationService.
 * Cache does NOT send messages to DRSNotificationService.
 * 
 * Cache --> RemoteServices --> NotificationService --> DRS
 * 
 * DRSMessageListener sends messages to CacheUnit
 * CacheUnit sends messages to Cache
 * 
 * DRSD --> DRSMessageListener --> CacheUnit --> Cache
 * 
 * </pre>
 */
public class Cache extends DCacheBase implements com.ibm.websphere.cache.CacheLocal {

    private static TraceComponent tc = Tr.register(Cache.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
    private static final int LOGGING_WINDOW = 1000;
    private final long lastTimeForStatistics = 0;

    // -------------------------------------------------

    /**
     * This JVM's BatchUpdateDaemon. Shared - Should this be static?
     */
    private BatchUpdateDaemon batchUpdateDaemon = null;

    /**
     * This JVM's RemoteServices.
     */
    private RemoteServices remoteServices = null;

    /**
     * This JVM's TimeLimitDaemon.
     */
    private TimeLimitDaemon timeLimitDaemon = null;
    private InvalidationAuditDaemon invalidationAuditDaemon = null;

    /**
     * This is a pool of free entries in the cache
     */
    private CacheEntry.CacheEntryPool cacheEntryPool = null;

    /**
     * This provides an index from ids (cache ids or data ids) to the ids of entries that depend on them.
     */
    private DependencyTable dataDependencyTable = null;

    /**
     * This provides an index from template names to the ids of entries that depend on them.
     */
    private DependencyTable templateDependencyTable = null;

    /**
     * This is an index to the entries for random access given a cache id. The key is the cache id that is unique within
     * the server. The value is the entry.
     */
    private NonSyncHashtable entryHashtable = null;

    /**
     * This is the default value for the priority.
     */
    private int defaultPriority = CacheConfig.DEFAULT_PRIORITY;

    // -------------------------------------------------
    // HTOD
    // -------------------------------------------------
    private DynacacheOnDisk diskCache = null;
    private boolean flushToDiskComplete = false;
    private boolean enableDiskCacheSizeInBytesChecking = false; // this boolean is set to true by CacheOnDisk if
                                                                // conditions meet
    private int entriesInMemoryRemoved = 0;
    private int entriesInDiskRemoved = 0;
    private long diskCacheEntrySizeInMBEvictedCount = 0;
    private long diskCacheSizeInGBEvictedCount = 0;
    private long diskCacheSizeEvictedCount = 0;
    private long diskCacheEntrySizeInMBEvictedLimit = 1;
    private long diskCacheSizeInGBEvictedLimit = 1;
    private long diskCacheSizeEvictedLimit = 1;
    // -------------------------------------------------

    // -------------------------------------------------
    // LRU management
    // -------------------------------------------------
    private CacheEntry.LRUHead lruBuckets[] = null;
    private int lruTop = 0;
    // -------------------------------------------------

    private int cacheSizeLimit = 0; // memory cache size + overflow buffer size if disk offload enabled

    private boolean displayedLRUMessage = false;

    /* refcount leak detection code */
    transient private volatile Map<Object, String> refCountLeakMap = new ConcurrentHashMap<Object, String>();
    private long lastTimeCheck = 0;
    private static long leakDetectionInterval = (5 * 60000); // multiples of 1 minute output the leak detection table
    private static String leakDetectionOutput = "ceRefCount.txt"; // write to the current Dir.

    /**
     * Constructor with parameter.
     * 
     * @param cacheName
     * @param cacheConfig
     */
    public Cache(String cacheName, CacheConfig cacheConfig) {
        super(cacheName, cacheConfig);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "Cache() CTOR cacheName:" + cacheName);
        }

        // ---------------------------------------

        this.swapToDisk = cacheConfig.enableDiskOffload;
        this.ignoreValueInInvalidationEvent = cacheConfig.ignoreValueInInvalidationEvent;
        String disableCookieList = cacheConfig.disableStoreCookies;

        this.defaultPriority = cacheConfig.cachePriority;
        if (this.defaultPriority < 0 || this.defaultPriority > CacheConfig.MAX_PRIORITY) {
            // DYNA0069W=DYNA0069W: An invalid value \"{0}\" for custom property \"{1}\" in cache name \"{2}\". The
            // valid range is low: \"{3}\" and high: \"{4}\". This custom property is set to \"{5}\".
            Tr.warning(tc, "DYNA0069W", new Object[] { new Integer(defaultPriority), "cachePriority", this.cacheName, new Integer(0),
                                                      new Integer(CacheConfig.MAX_PRIORITY), new Integer(CacheConfig.DEFAULT_PRIORITY) });
            this.defaultPriority = CacheConfig.DEFAULT_PRIORITY;
        }

        this.cacheStatisticsListener = new CacheStatisticsListenerImpl(this.cacheName);

        boolean cacheInstanceStoreCookies = true;
        if (disableCookieList.equalsIgnoreCase("All")) {
            cacheInstanceStoreCookies = false;
        } else {
            StringTokenizer tokens = new StringTokenizer(disableCookieList, ":");
            while (tokens.hasMoreElements()) {
                String name = (String) tokens.nextElement();
                if (name.equals(cacheName) || name.equalsIgnoreCase("All")) {
                    cacheInstanceStoreCookies = false;
                    break;
                }
            }
        }
        cacheConfig.setCacheInstanceStoreCookies(cacheInstanceStoreCookies);

        if (!cacheInstanceStoreCookies && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, " Store_Cookies property disabled for cacheName:" + cacheName);

        // --------------------------------------------------------
        // Create a CacheEntry pool
        // --------------------------------------------------------
        int poolSize = Math.min(1000, cacheConfig.cacheSize / 10);
        cacheEntryPool = CacheEntry.createCacheEntryPool(this, poolSize);
        // --------------------------------------------------------

        // --------------------------------------------------------
        // Prepare the LRU buckets
        // --------------------------------------------------------
        lruBuckets = new CacheEntry.LRUHead[CacheConfig.MAX_PRIORITY + 1];
        for (int i = 0; i < lruBuckets.length; i++) {
            lruBuckets[i] = new CacheEntry.LRUHead();
            lruBuckets[i].priority = i;
        }
        // --------------------------------------------------------

        // --------------------------------------------------------
        // Setup tables for cache entries, templates and dependencies
        // --------------------------------------------------------
        entryHashtable = new NonSyncHashtable(cacheConfig.cacheSize);
        increaseCacheSizeInBytes(ObjectSizer.FASTHASHTABLE_INITIAL_OVERHEAD + ObjectSizer.FASTHASHTABLE_INITIAL_PER_ENTRY_OVERHEAD
                                 * cacheConfig.cacheSize, "EHT");

        if (!cacheConfig.disableDependencyId) {
            dataDependencyTable = new DependencyTable(DependencyTable.CONCURRENT_HASHMAP, cacheConfig.cacheSize / 3);
        }
        if (!cacheConfig.disableTemplatesSupport) {
            templateDependencyTable = new DependencyTable(DependencyTable.CONCURRENT_HASHMAP, cacheConfig.cacheSize / 10);
        }

        // --------------------------------------------------------
        // (1) The cacheSizeLimit is initialized to cache size.
        // (2) If the disk offload feature is enabled, re-calculate cacheSizeLimit using
        // lruToDiskTriggerPercent which is the percentage of memory cache size used as a overflow
        // buffer.
        // (3) Cache entries in the overflow buffer are purged and asynchronously offload to disk
        // cache at a frequency of lruToDiskTriggerTime. If the memory ovrflow buffer is full,
        // cache entries are offload to disk cache synchronously on the caller thread.
        // (4) The cacheSizeLimit is used by cache.getFreeLruEntry() to determine whether it can
        // get the cache entry from the pool or perform LRU opeartion synchronously to free up
        // cache entry from the memory cache on the caller thread.
        // (5) If the disk offload feature is not enabled, the lruToDiskTriggerPercent is set to zero.
        // (6) The lruToDiskTriggerPercent is used by TimeLimitDaemon to control trimCache().
        // --------------------------------------------------------
        int lruToDiskTriggerPercent = cacheConfig.lruToDiskTriggerPercent;
        this.cacheSizeLimit = cacheConfig.cacheSize; // initialize to memory cache size
        // --------------------------------------------------------
        // If disk overflow is enabled, initialize it
        // --------------------------------------------------------
        if (swapToDisk) {
            // -------------------------------------
            // swapToDisk is true
            // -------------------------------------
            diskCache = new CacheOnDisk(cacheConfig, this);
            // -------------------------------------
            // swapToDisk may change to "false" due
            // to CacheOnDisk unrecoverable error.
            // -------------------------------------
            if (!swapToDisk) {
                diskCache = null;
                lruToDiskTriggerPercent = 0;
            } else {
                lruToDiskTriggerPercent = CacheConfig.DEFAULT_LRU_TO_DISK_TRIGGER_PERCENT; // initialize to default
                if (cacheConfig.lruToDiskTriggerPercent > CacheConfig.MAX_LRU_TO_DISK_TRIGGER_PERCENT
                    || cacheConfig.lruToDiskTriggerPercent < CacheConfig.MIN_LRU_TO_DISK_TRIGGER_PERCENT) {
                    Tr.warning(tc, "DYNA0069W", new Object[] { new Integer(cacheConfig.lruToDiskTriggerPercent), "lruToDiskTriggerPercent",
                                                              cacheName, new Integer(CacheConfig.MIN_LRU_TO_DISK_TRIGGER_PERCENT),
                                                              new Integer(CacheConfig.MAX_LRU_TO_DISK_TRIGGER_PERCENT),
                                                              new Integer(CacheConfig.DEFAULT_LRU_TO_DISK_TRIGGER_PERCENT) });
                } else {
                    lruToDiskTriggerPercent = cacheConfig.lruToDiskTriggerPercent;
                }
                // re-calculate the cache size limit if lruToDiskTriggerPercent > 0
                if (cacheConfig.lruToDiskTriggerPercent > 0) {
                    this.cacheSizeLimit = cacheConfig.cacheSize + cacheConfig.cacheSize * cacheConfig.lruToDiskTriggerPercent / 100;
                }
            }
        } else {
            // -------------------------------------
            // swapToDisk is false
            // -------------------------------------
            lruToDiskTriggerPercent = 0;
        }
        cacheConfig.lruToDiskTriggerPercent = lruToDiskTriggerPercent; // saved it back in CacheConfig in case it is
                                                                       // changed.

        // --------------------------------------------------------

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "Cache() CTOR cacheName=" + cacheName + " cache=" + this + " useServerClassLoader:" + cacheConfig.useServerClassLoader
                        + " filterTimeOutInvalidation:" + cacheConfig.filterTimeOutInvalidation + " filterLRUInvalidation:"
                        + cacheConfig.filterLRUInvalidation + " lruToDiskTriggerPercent=" + cacheConfig.lruToDiskTriggerPercent + " cacheSizeLimit="
                        + this.cacheSizeLimit + " cacheSizeInMB=" + cacheConfig.memoryCacheSizeInMB + " cascadeCachespecProperties="
                        + cacheConfig.cascadeCachespecProperties);
        }
    }

    /**
     * Returns various cache statistics encapsulated int the CacheStatistics interface.
     * 
     * @return CacheStatistics
     */
    @Override
    public CacheStatistics getCacheStatistics() {

        CacheStatisticsImpl statistics = new CacheStatisticsImpl();
        statistics.setCacheHitsCount(cacheStatisticsListener.getCacheHitsCount());
        statistics.setCacheLruRemovesCount(cacheStatisticsListener.getCacheLruRemovesCount());
        statistics.setCacheMissesCount(cacheStatisticsListener.getCacheMissesCount());
        statistics.setCacheRemovesCount(cacheStatisticsListener.getCacheRemovesCount());
        statistics.setExplicitInvalidationsFromMemoryCount(cacheStatisticsListener.getExplicitInvalidationsFromMemoryCount());
        statistics.setMemoryCacheEntriesCount(getNumberCacheEntries());
        statistics.setMemoryCacheSizeInMBCount(getCurrentMemoryCacheSizeInMB());
        statistics.setTimeoutInvalidationsFromMemoryCount(cacheStatisticsListener.getTimeoutInvalidationsFromMemoryCount());

        TreeMap<String, Number> extendedStats = new TreeMap<String, Number>();
        extendedStats.put("OverflowEntriesFromMemory", cacheStatisticsListener.getOverflowEntriesFromMemoryCount());
        extendedStats.put("ExplicitInvalidationsFromDisk", cacheStatisticsListener.getExplicitInvalidationsFromDiskCount());
        extendedStats.put("ExplicitInvalidationsLocal", cacheStatisticsListener.getExplicitInvalidationsLocalCount());
        extendedStats.put("ExplicitInvalidationsRemote", cacheStatisticsListener.getExplicitInvalidationsRemoteCount());
        extendedStats.put("TimeoutInvalidationsFromDisk", cacheStatisticsListener.getTimeoutInvalidationsFromDiskCount());
        extendedStats.put("GarbageCollectorInvalidationsFromDisk", cacheStatisticsListener.getGarbageCollectorInvalidationsFromDiskCount());
        extendedStats.put("DependencyIdsOffloadedToDisk", cacheStatisticsListener.getDepIdsOffloadedToDiskCount());
        extendedStats.put("DependencyIdBasedInvalidationsFromDisk", cacheStatisticsListener.getDepIdBasedInvalidationsFromDiskCount());
        extendedStats.put("TemplatesOffloadedToDisk", cacheStatisticsListener.getTemplatesOffloadedToDiskCount());
        extendedStats.put("TemplateBasedInvalidationsFromDisk", cacheStatisticsListener.getTemplateBasedInvalidationsFromDiskCount());
        extendedStats.put("OverflowInvalidationsFromDisk", cacheStatisticsListener.getOverflowInvalidationsFromDiskCount());
        extendedStats.put("ObjectsReadFromDisk", cacheStatisticsListener.getObjectsReadFromDiskCount());
        extendedStats.put("ObjectsReadFromDisk4K", cacheStatisticsListener.getObjectsReadFromDisk4KCount());
        extendedStats.put("ObjectsReadFromDisk40K", cacheStatisticsListener.getObjectsReadFromDisk40KCount());
        extendedStats.put("ObjectsReadFromDisk400K", cacheStatisticsListener.getObjectsReadFromDisk400KCount());
        extendedStats.put("ObjectsReadFromDisk4000K", cacheStatisticsListener.getObjectsReadFromDisk4000KCount());
        extendedStats.put("ObjectsReadFromDiskSize", cacheStatisticsListener.getObjectsReadFromDiskSizeCount());
        extendedStats.put("ObjectsWriteToDisk", cacheStatisticsListener.getObjectsWriteToDiskCount());
        extendedStats.put("ObjectsWriteToDisk4K", cacheStatisticsListener.getObjectsWriteToDisk4KCount());
        extendedStats.put("ObjectsWriteToDisk40K", cacheStatisticsListener.getObjectsWriteToDisk40KCount());
        extendedStats.put("ObjectsWriteToDisk400K", cacheStatisticsListener.getObjectsWriteToDisk400KCount());
        extendedStats.put("ObjectsWriteToDisk4000K", cacheStatisticsListener.getObjectsWriteToDisk4000KCount());
        extendedStats.put("ObjectsWriteToDiskSize", cacheStatisticsListener.getObjectsWriteToDiskSizeCount());
        extendedStats.put("ObjectsDeleteFromDisk", cacheStatisticsListener.getObjectsDeleteFromDiskCount());
        extendedStats.put("ObjectsDeleteFromDisk4K", cacheStatisticsListener.getObjectsDeleteFromDisk4KCount());
        extendedStats.put("ObjectsDeleteFromDisk40K", cacheStatisticsListener.getObjectsDeleteFromDisk40KCount());
        extendedStats.put("ObjectsDeleteFromDisk400K", cacheStatisticsListener.getObjectsDeleteFromDisk400KCount());
        extendedStats.put("ObjectsDeleteFromDisk4000K", cacheStatisticsListener.getObjectsDeleteFromDisk4000KCount());
        extendedStats.put("ObjectsDeleteFromDiskSize", cacheStatisticsListener.getObjectsDeleteFromDiskSizeCount());
        extendedStats.put("RemoteInvalidationNotifications", cacheStatisticsListener.getRemoteInvalidationNotificationsCount());
        extendedStats.put("RemoteUpdateNotifications", cacheStatisticsListener.getRemoteUpdateNotificationsCount());
        extendedStats.put("RemoteObjectUpdates", cacheStatisticsListener.getRemoteObjectUpdatesCount());
        extendedStats.put("RemoteObjectUpdateSize", cacheStatisticsListener.getRemoteObjectUpdateSizeCount());
        extendedStats.put("RemoteObjectHits", cacheStatisticsListener.getRemoteObjectHitsCount());
        extendedStats.put("RemoteObjectFetchSize", cacheStatisticsListener.getRemoteObjectFetchSizeCount());
        extendedStats.put("RemoteObjectMisses", cacheStatisticsListener.getRemoteObjectMissesCount());
        extendedStats.put("ObjectsAsyncLruToDisk", cacheStatisticsListener.getObjectsAsyncLruToDiskCount());
        extendedStats.put("ObjectsOnDisk", (long) getIdsSizeDisk());
        extendedStats.put("DiskCacheSizeInMB", getDiskCacheSizeInMBs());
        extendedStats.put("DependencyIdsOnDisk", (long) getDepIdsSizeDisk());
        extendedStats.put("TemplatesOnDisk", (long) getTemplatesSizeDisk());
        extendedStats.put("PendingRemovalFromDisk", (long) getPendingRemovalSizeDisk());
        extendedStats.put("DependencyIdsBufferedForDisk", (long) getDepIdsBufferedSizeDisk());
        extendedStats.put("TemplatesBufferedForDisk", (long) getTemplatesBufferedSizeDisk());
        extendedStats.put("PushPullTableSize", (long) getPushPullTableSize());
        statistics.setExtendedStats(extendedStats);
        return statistics;
    }

    /**
     * **** DCache interface **** <br>
     * Sets this JVM's BatchUpdateDaemon. It is called by the CacheUnitImpl when things get started. <br>
     * NOTE: There is only one batchUpdateDaemon shared by all cache instances.
     * 
     * @param batchUpdateDaemon
     *            The batchUpdateDaemon.
     */
    @Override
    public void setBatchUpdateDaemon(BatchUpdateDaemon batchUpdateDaemon) {
        this.batchUpdateDaemon = batchUpdateDaemon;
    }

    /**
     * **** DCache interface **** <br>
     * This sets this JVM's TimeLimitDaemon. It is called by the CacheUnitImpl when things get started. <br>
     * NOTE: There is only one TimeLimitDaemon shared by all cache instances.
     * 
     * @param timeLimitDaemon
     *            The timeLimitDaemon.
     */
    @Override
    public void setTimeLimitDaemon(TimeLimitDaemon timeLimitDaemon) {
        this.timeLimitDaemon = timeLimitDaemon;
    }

    @Override
    public void setInvalidationAuditDaemon(InvalidationAuditDaemon iad) {
        this.invalidationAuditDaemon = iad;
    }

    /**
     * **** DCache interface **** <br>
     * This sets remoteServices. It is called by the ServerCache when things get started. <br>
     * NOTE: Each cache instance has its own remoteServices.
     * 
     * @param remoteServices
     *            The remoteServices.
     */
    @Override
    public void setRemoteServices(RemoteServices remoteServices) {
        this.remoteServices = remoteServices;
    }

    /**
     * **** DCache interface **** <br>
     * Returns remoteServices.
     * 
     * @Return remoteServices The remoteServices
     */
    @Override
    public RemoteServices getRemoteServices() {
        return remoteServices;
    }

    /**
     * This method will be invoked once Dynacache is ready to use this cache instance.
     */
    @Override
    public void start() {
        if ((timeLimitDaemon == null) || (batchUpdateDaemon == null) || (cacheStatisticsListener == null) || (remoteServices == null)) {
            throw new IllegalStateException("batchUpdateDaemon, cacheStatisticsListener, " + "remoteServices, and timeLimitDaemon "
                                            + "must all be set before start()");
        }
        this.timeLimitDaemon.createExpirationMetaData(this);
        flushToDiskComplete = false;
        if (this.swapToDisk) {
            if (diskCache.getStartState() == CacheOnDisk.START_LPBT_SCAN || diskCache.shouldPopulateEvictionTable()) {
                diskCache.invokeDiskCleanup(HTODInvalidationBuffer.SCAN);
            } else if (diskCache.getStartState() == CacheOnDisk.START_LPBT_REMOVE) {
                diskCache.invokeDiskCleanup(!HTODInvalidationBuffer.SCAN);
            }
        }
    }

    /**
     * Puts an entry into the cache. If the entry already exists in the cache, this method will ALSO update the same.
     * 
     * Called by DistributedNioMap (old ProxyCache) Called by Cache.setEntry(cacheEntry, source)
     * 
     * The passed CacheEntry does come from this Cache's CacheEntry pool.
     * 
     * @param cacheEntry
     *            The CacheEntry object
     * @param source
     *            The source (local or remote)
     * @param ignoreCounting
     *            true to ignore the statistics counting
     * @param coordinate
     *            Indicates that the value should be set in other caches caching this value.
     * @param incRefcount
     *            Indicates that refcount of the returned cache entry will be incremented.
     * 
     * @return The CacheEntry
     */
    @Override
    public com.ibm.websphere.cache.CacheEntry setEntry(CacheEntry cacheEntry, int source, boolean ignoreCounting, boolean coordinate,
                                                       boolean incRefcount) {

        CacheEntry cacheEntryOut = null;

        if (!ignoreCounting) {
            CachePerf cachePerf = cachePerfRef.get();
            if (cachePerf != null && source == CachePerf.LOCAL && cachePerf.isPMIEnabled())
                cachePerf.onRequest(cacheEntry.getTemplate(), source);
        }

        Object id = cacheEntry.getIdObject();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "setEntry() cacheName=" + this.cacheName + " id=" + id + " sharing policy=" + cacheEntry.getSharingPolicy() + " timeToLive="
                         + cacheEntry.getTimeLimit());
        }

        // _syncSetEntry() will inc the refCount
        cacheEntryOut = _syncSetEntry(cacheEntry, source, ignoreCounting, incRefcount);

        // skip telling the timeLimitDaemon because it has been handled in the LRUToDisk()
        if (!cacheEntryOut.skipMemoryAndWriteToDisk && (cacheEntryOut.timeLimit > 0 || cacheEntryOut.inactivity > 0)) { // CPF-Inactivity
            timeLimitDaemon.valueHasChanged(this, cacheEntryOut.id, cacheEntryOut.expirationTime, cacheEntryOut.inactivity);
        }

        // return when exception detected during write-to-disk-directly operation
        if (cacheEntryOut.getSharingPolicy() == EntryInfo.NOT_SHARED || cacheEntryOut.skipMemoryAndWriteToDiskErrorCode != HTODDynacache.NO_EXCEPTION) {
            cacheEntryOut.finish();
            return cacheEntryOut;
        }

        if (source == CachePerf.REMOTE && cacheEntry.getSharingPolicy() == EntryInfo.SHARED_PUSH) {
            byte[] serializedValue = cacheEntry.getSerializedValue();
            int valueSize = 0;
            if (serializedValue != null) {
                valueSize = serializedValue.length;
            }
            this.cacheStatisticsListener.remoteObjectUpdates(id, valueSize);
        }

        // Set CacheEntryOut on Central Cache
        if (coordinate) {
            updatePeerCaches(cacheEntryOut);
        } else {
            cacheEntryOut.finish();
        }

        return cacheEntryOut;
    }

    // ------------------------------------------------------

    // Created wrapper for entryHashtable.get() to encapsulate processing
    // needed when swapping to disk.
    // Monitor informs the method whether or not it should
    // track cache hits in PMI
    private CacheEntry getCacheEntry(Object id, boolean askPermission, boolean ignoreCounting, String _missTemplate, boolean incRefCount) {
        CacheEntry cacheEntry;

        CachePerf cachePerf = null;
        if (!ignoreCounting) {
            cachePerf = cachePerfRef.get();
        }

        // updateLruLocation only if getCacheEntry is
        // called from CacheHook.java or CommandCache.java
        if (incRefCount) {
            cacheEntry = getCacheEntry(id, incRefCount);
        } else {
            cacheEntry = (CacheEntry) entryHashtable.get(id);
            if (null != cacheEntry && incRefCount)
                cacheEntry.incRefCount();
        }

        if (cacheEntry != null) {
            if (!ignoreCounting) {
                // Update cache statistics if the entry is valid (VET < t < RET).
                // If it is invalid VBC, it will update later using updateCacheStatistics()
                if (cacheEntry.isInvalid() == false) {
                    cacheStatisticsListener.localCacheHit(id, CachePerf.MEMORY);
                    if (cachePerf != null && (cacheEntry.value != null || cacheEntry.serializedValue != null) && cachePerf.isPMIEnabled())
                        cachePerf.onCacheHit(cacheEntry.getTemplate(), CachePerf.MEMORY);
                } else {
                    cacheEntry.setVBCSource(DynaCacheConstants.VBC_INVALID_MEMORY_HIT); // indicate that CE comes from
                                                                                        // local memory hit
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "getCacheEntry() cacheName=" + this.cacheName + " found in memory invalid VBC for id " + id + " vbcSource="
                                     + DynaCacheConstants.VBC_INVALID_MEMORY_HIT);
                    }
                }
            }
            return cacheEntry;
        } else if (cacheEntry == null && swapToDisk) {
            // synchronized (this) {
            cacheEntry = diskCache.readCacheEntry(id);
            // }
            if (cacheEntry != null) {
                if (incRefCount) {
                    cacheEntry.incRefCount();
                }
                cacheEntry.loadedFromDisk = true;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    if (cacheEntry.isInvalid() == false) {
                        Tr.debug(tc, "getCacheEntry() cacheName=" + this.cacheName + " found on disk for id " + id + ", ignoreCounting="
                                     + ignoreCounting + " skipMemoryAndWriteToDisk=" + cacheEntry.skipMemoryAndWriteToDisk);
                    } else {
                        Tr.debug(tc, "getCacheEntry() cacheName=" + this.cacheName + " found on disk invalid VBC for id " + id + " vbcSource="
                                     + DynaCacheConstants.VBC_INVALID_DISK_HIT);
                    }
                }
                if (!ignoreCounting) {
                    // Update cache statistics if the entry is valid (VET < t < RET).
                    // If it is invalid VBC, it will update later using updateCacheStatistics()
                    if (cacheEntry.isInvalid() == false) {
                        cacheStatisticsListener.localCacheHit(id, CachePerf.DISK);
                        if (cachePerf != null && cachePerf.isPMIEnabled()) {
                            cachePerf.onCacheHit(cacheEntry.getTemplate(), CachePerf.DISK);
                        }
                    } else {
                        cacheEntry.setVBCSource(DynaCacheConstants.VBC_INVALID_DISK_HIT); // indicate that CE comes from
                                                                                          // disk
                    }
                }
                // skip putting the cache entry to memory cache if skipMemoryAndWriteToDisk
                if (cacheEntry.skipMemoryAndWriteToDisk) {
                    return cacheEntry;
                }
                // Place CE into cache, get updated copy
                cacheEntry = (CacheEntry) setEntry(cacheEntry, CachePerf.DISK, ignoreCounting, !COORDINATE, !INCREMENT_REFF_COUNT);

                return cacheEntry;
            }
        }

        // See if another cache has the CE
        if (cacheEntry == null && askPermission) {
            cacheEntry = remoteServices.getEntry(id);
            if (cacheEntry != null) {
                if (incRefCount) {
                    cacheEntry.incRefCount();
                }
                if (!ignoreCounting) {
                    int valueSize = 0;
                    byte[] serializedValue = cacheEntry.getSerializedValue();
                    if (serializedValue != null) {
                        valueSize = serializedValue.length;
                    }
                    // Update cache statistics if the entry is valid (VET < t < RET).
                    // If it is invalid VBC, it will update later using updateCacheStatistics()
                    if (cacheEntry.isInvalid() == false) {
                        this.cacheStatisticsListener.remoteObjectHits(cacheEntry.id, valueSize);
                        if (cachePerf != null && cachePerf.isPMIEnabled()) {
                            cachePerf.onCacheHit(cacheEntry.getTemplate(), CachePerf.REMOTE);
                        }
                    } else {
                        cacheEntry.setVBCSource(DynaCacheConstants.VBC_INVALID_REMOTE_HIT); // indicate that CE comes
                                                                                            // from remote
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "getCacheEntry() cacheName=" + this.cacheName + " found from remote invalid VBC for id " + id
                                         + " vbcSource=" + DynaCacheConstants.VBC_INVALID_REMOTE_HIT);
                        }
                    }
                }
                cacheEntry = (CacheEntry) setEntry(cacheEntry, CachePerf.REMOTE, ignoreCounting, !COORDINATE, !INCREMENT_REFF_COUNT);
            } else {
                if (!ignoreCounting) {
                    // cacheStatisticsListener.getValueCacheMiss(id);
                    cacheStatisticsListener.remoteObjectMisses(id);

                    if (cachePerf != null && cachePerf.isPMIEnabled()) {
                        cachePerf.onCacheMiss(_missTemplate, CachePerf.LOCAL);
                    }
                }
            }

        } else {
            if (!ignoreCounting) {
                cacheStatisticsListener.cacheMiss(id);
                if (cachePerf != null && cachePerf.isPMIEnabled())
                    cachePerf.onCacheMiss(_missTemplate, CachePerf.LOCAL);
            }
        }
        return cacheEntry;
    }

    private CacheEntry getCacheEntry(Object id, boolean incRefCount) {
        CacheEntry cacheEntry = (CacheEntry) entryHashtable.get(id);
        if (null != cacheEntry && incRefCount)
            cacheEntry.incRefCount();
        if (cacheEntry != null && !cacheEntry.pendingRemoval && !cacheEntry.removeWhenUnpinned) {
            updateLruLocation(cacheEntry);
            if (cacheEntry.inactivity > 0) {
                timeLimitDaemon.valueWasAccessed(this, cacheEntry.id, cacheEntry.expirationTime, cacheEntry.inactivity);
            }
        }
        return cacheEntry;
    }

    // ------------------------------------------------------
    // Called by Cache.setEntry()
    // ------------------------------------------------------
    // Refactored into synchronized method because calling a
    // synchronized method is much better for performance than
    // synchronized(this)
    // ------------------------------------------------------
    private synchronized CacheEntry _syncSetEntry(CacheEntry cacheEntry, int source, boolean ignoreCounting, boolean incRefCount) {

        // don't use getCacheEntry because we will end up in a infinite loop;
        // besides, we don't need to read the entry from disk here because
        // we will delete it
        int cause = -1;
        // set hasPushPullEntries to true when an entry uses Push-Pull mode or Pull mode
        if (hasPushPullEntries == false
            && (cacheEntry.sharingPolicy == EntryInfo.SHARED_PUSH_PULL || cacheEntry.sharingPolicy == EntryInfo.SHARED_PULL)) { // PK59026
            hasPushPullEntries = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "_syncSetEntry() Entry uses Push-Pull or Pull mode cacheName=" + cacheName + " id=" + cacheEntry.id);
            }
        }

        CacheEntry oldEntry = (CacheEntry) entryHashtable.get(cacheEntry.id);

        if (!ignoreCounting) {
            CachePerf cachePerf = cachePerfRef.get();
            if (cachePerf != null && source != CachePerf.DISK && cachePerf.isPMIEnabled()) {
                if (!((cacheEntry.getSharingPolicy() == EntryInfo.SHARED_PULL || cacheEntry.getSharingPolicy() == EntryInfo.SHARED_PUSH_PULL) && source == CachePerf.REMOTE)) {
                    if (oldEntry == null) {
                        cachePerf.onEntryCreation(cacheEntry.getTemplate(), source);
                    }
                }
            }
        }
        CacheEntry newEntry = null;

        if (oldEntry == null) {
            //
            // Do not find the cache entry in memory cache
            //
            cause = ChangeEvent.NEW_ENTRY_ADDED;

            if (swapToDisk && cacheEntry.skipMemoryAndWriteToDisk) {
                // skipMemoryAndWriteToDisk is true
                // skip promoting the cache entry to memory and write to the disk directly
                writeToDiskDirectly(cacheEntry, cause, source);
                return cacheEntry;
            }

            newEntry = getFreeLruEntry();
            entryHashtable.put(cacheEntry.id, newEntry);

            // check if it is in the disk cache. If exists, remove it from disk cache.
            if (swapToDisk) {
                if (!cacheEntry.loadedFromDisk) {
                    if (diskCache.containsKey(cacheEntry.id)) {
                        diskCache.delCacheEntry(cacheEntry, CachePerf.DIRECT, source, !FROM_DEPID_TEMPLATE_INVALIDATION);
                        cause = ChangeEvent.EXISTING_VALUE_CHANGED;
                    }
                }
            }
        } else {
            //
            // Found the cache entry in memroy cache
            //
            cause = ChangeEvent.EXISTING_VALUE_CHANGED;

            if (swapToDisk && cacheEntry.skipMemoryAndWriteToDisk) {
                // skipMemoryAndWriteToDisk is true
                // skip promoting the cache entry to memory and write to the disk directly
                // the entry exists in the memory cache ==> remove the entry from memory cache and associated tables
                entryHashtable.remove(oldEntry.id);
                decreaseCacheSizeInBytes(oldEntry);
                if (oldEntry.timeLimit > 0 || oldEntry.inactivity > 0) {
                    timeLimitDaemon.valueWasRemoved(this, oldEntry.id);
                }
                removeInvalidationInfo(oldEntry);
                for (int i = 0; i < cacheEntry.aliasList.length; i++) {
                    entryHashtable.remove(oldEntry.aliasList[i]);
                    decreaseCacheSizeInBytes(ObjectSizer.FASTHASHTABLE_PER_ENTRY_OVERHEAD + ObjectSizer.OBJECT_REF_SIZE, "ALIAS");
                }
                if (oldEntry.getRefCount() <= 0) {
                    oldEntry.returnToPool();
                } else if (oldEntry.removeWhenUnpinned == false) {
                    oldEntry.removeWhenUnpinned = true;
                    oldEntry.lruHead.remove(oldEntry);
                }
                writeToDiskDirectly(cacheEntry, cause, source);
                return cacheEntry;
            }
            if (cacheEntry.loadedFromDisk && oldEntry.loadedFromDisk) {
                if (cacheEntry.getRefCount() > 0 && oldEntry != cacheEntry) {
                    oldEntry.incRefCount();
                    oldEntry.incRefCount();
                }
                return oldEntry;
            }

            decreaseCacheSizeInBytes(oldEntry);

            if (oldEntry.timeStamp > cacheEntry.timeStamp) {
                Tr.debug(tc, "_syncSetEntry() cacheName=" + this.cacheName + " ERROR: attempting to overwrite "
                             + "cacheEntry with older cacheEntry: " + cacheEntry.id);
                // return;
            }

            if (swapToDisk && oldEntry.loadedFromDisk) {
                diskCache.delCacheEntry(oldEntry, CachePerf.DIRECT, source, !FROM_DEPID_TEMPLATE_INVALIDATION);
                oldEntry.loadedFromDisk = false;
            }
            if (oldEntry.getRefCount() == 0)
                newEntry = oldEntry;
            else {
                newEntry = getFreeLruEntry();
                entryHashtable.put(cacheEntry.id, newEntry);
                if (oldEntry.removeWhenUnpinned == false) {
                    oldEntry.removeWhenUnpinned = true;
                    oldEntry.lruHead.remove(oldEntry);
                }
            }
            removeInvalidationInfo(oldEntry);

            // Remove the alias cache entry from cache's
            // mapping table according to oldEntry
            Enumeration e = oldEntry.getAliasList();
            while (e.hasMoreElements()) {
                Object alias = e.nextElement();
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "_syncSetEntry() cacheName=" + this.cacheName + " removing alias from EHT id=" + cacheEntry.id + " alias=" + alias);
                }
                entryHashtable.remove(alias);
                decreaseCacheSizeInBytes(ObjectSizer.FASTHASHTABLE_PER_ENTRY_OVERHEAD + ObjectSizer.OBJECT_REF_SIZE, "ALIAS");
            }
        }

        updateInvalidationHashtable(cacheEntry);

        newEntry.copy(cacheEntry);
        updateLruLocation(newEntry);
        increaseCacheSizeInBytes(newEntry);

        // Adding the alias cache entry to cache's mapping
        // table according to newEntry
        Enumeration e = newEntry.getAliasList();
        while (e.hasMoreElements()) {
            Object alias = e.nextElement();
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "_syncSetEntry() cacheName=" + this.cacheName + " adding alias to EHT id=" + cacheEntry.id + " alias=" + alias);
            }
            entryHashtable.put(alias, newEntry);
            increaseCacheSizeInBytes(ObjectSizer.FASTHASHTABLE_PER_ENTRY_OVERHEAD + ObjectSizer.OBJECT_REF_SIZE, "ALIAS");
        }

        // refCount will be decremented by CacheEntry.finish() later in Cache.java
        newEntry.incRefCount();

        // IncRefCount can be true by DistributedObjectCacheAdapter.internal_putAndGet() because the newEntry is
        // returned to the caller.
        // The caller will call the CacheEntry.finish() after it finishes to use the entry.
        if (incRefCount) {
            newEntry.incRefCount();
        }

        // ---------------------------------
        // Event notification
        // ---------------------------------
        if (bEnableListener && eventSource.getChangeListenerCount() > 0 && source != CachePerf.DISK) {
            int src = ChangeEvent.LOCAL;
            if (source == CachePerf.REMOTE) {
                src = ChangeEvent.REMOTE;
            }
            Object value = null;
            if (newEntry.serializedValue != null) {
                value = newEntry.serializedValue;
            } else {
                value = newEntry.getValue();
            }
            // TODO CPF_CODE_REVIEW - use ChangeEvent pooling
            ChangeEvent event = new ChangeEvent(newEntry.id, value, cause, src, cacheName);
            eventSource.cacheEntryChanged(event);
        }
        // ---------------------------------

        return newEntry;
    }

    // skip putting the cache entry in memory and write it to disk directly
    private void writeToDiskDirectly(CacheEntry cacheEntry, int cause, int source) {
        cacheEntry.incRefCount();
        LruToDiskResult toDiskResult = lruToDisk(cacheEntry);
        if (toDiskResult.entryOverwritten == ENTRY_OVERWRITTEN_ON_DISK) {
            cause = ChangeEvent.EXISTING_VALUE_CHANGED;
        }
        // ---------------------------------
        // Event notification
        // ---------------------------------
        if (cacheEntry.skipMemoryAndWriteToDiskErrorCode == HTODDynacache.NO_EXCEPTION && bEnableListener && eventSource.getChangeListenerCount() > 0) {
            int src = ChangeEvent.LOCAL;
            if (source == CachePerf.REMOTE) {
                src = ChangeEvent.REMOTE;
            }
            Object value = null;
            if (cacheEntry.serializedValue != null) {
                value = cacheEntry.serializedValue;
            } else {
                value = cacheEntry.getValue();
            }
            ChangeEvent event = new ChangeEvent(cacheEntry.id, value, cause, src, cacheName);
            eventSource.cacheEntryChanged(event);
        }
    }

    /**
     * This returns the cache entry identified by the specified entryInfo. It returns null if not in the cache.
     * 
     * @param ei
     *            The entryInfo for the entry.
     * @param checkAskPermission
     *            true to check to find askPermission from sharing policy
     * @return The entry identified by the entryInfo.getIdObject().
     */
    @Override
    public com.ibm.websphere.cache.CacheEntry getEntry(com.ibm.websphere.cache.EntryInfo ei, boolean checkAskPermission) {

        Object id = ei.getIdObject();
        if (id == null)
            return null;
        boolean askPermission = getPermission(ei, checkAskPermission, id);
        CacheEntry cacheEntry = getCacheEntry(id, askPermission, !IGNORE_COUNTING, ei.getTemplate(), INCREMENT_REFF_COUNT);
        cacheEntry = checkExpired(cacheEntry);

        return cacheEntry;
    }

    @Override
    public com.ibm.websphere.cache.CacheEntry getEntry(com.ibm.websphere.cache.EntryInfo ei, boolean checkPermission, boolean ignoreCounting) {

        Object id = ei.getIdObject();
        if (id == null)
            return null;
        boolean askPermission = getPermission(ei, checkPermission, id);
        CacheEntry cacheEntry = getCacheEntry(id, askPermission, ignoreCounting, ei.getTemplate(), INCREMENT_REFF_COUNT);
        cacheEntry = checkExpired(cacheEntry);

        return cacheEntry;

    }

    private CacheEntry checkExpired(CacheEntry cacheEntry) {
        // if entry is expired, return NULL
        if (cacheEntry != null && cacheEntry.getExpirationTime() > 0 && cacheEntry.getExpirationTime() < System.currentTimeMillis()) {
            cacheEntry.finish();
            cacheEntry = null;
        }
        return cacheEntry;
    }

    private boolean getPermission(com.ibm.websphere.cache.EntryInfo ei, boolean checkAskPermission, Object id) {
        boolean askPermission = false;
        CachePerf cachePerf = cachePerfRef.get();
        if (cachePerf != null && cachePerf.isPMIEnabled())
            cachePerf.onRequest(ei.getTemplate(), CachePerf.LOCAL);
        if (checkAskPermission)
            askPermission = shouldPull(ei.getSharingPolicy(), id);
        return askPermission;
    }

    /**
     * This returns the cache entry identified by the specified cache id. It returns null if not in the cache.
     * 
     * Warning: If incrementRefCount is true, the refCount of CE will be incremented to avoid LRU. The caller has to do
     * ce.finish().
     * 
     * @param id
     *            The cache id for the entry. The id cannot be null.
     * @param source
     *            The source - local or remote
     * @param ignoreCounting
     *            true to ignore the statistics counting
     * @param incrementRefCount
     *            true to increment the refCount of the entry
     * @return The entry identified by the cache id.
     */
    @Override
    public com.ibm.websphere.cache.CacheEntry getEntry(Object id, int source, boolean ignoreCounting, boolean incrementRefCount) {
        if (id == null)
            return null;
        CacheEntry cacheEntry = getCacheEntry(id, !ASK_PERMISSION, ignoreCounting, null, incrementRefCount);
        if (!ignoreCounting && cacheEntry != null && source == CachePerf.REMOTE) {
            CachePerf cachePerf = cachePerfRef.get();
            if (cachePerf != null && cachePerf.isPMIEnabled()) {
                cachePerf.onRequest(cacheEntry.getTemplate(), CachePerf.REMOTE);
            }
        }
        // if entry is expired, return NULL
        if (cacheEntry != null && cacheEntry.getExpirationTime() > 0 && cacheEntry.getExpirationTime() < System.currentTimeMillis()) {
            if (incrementRefCount) {
                cacheEntry.finish();
            }
            cacheEntry = null;
        }
        return cacheEntry;
    }

    /**
     * This method is kept for cache monitor. Used to move the entry to the end of the LRU queue
     */
    @Override
    public synchronized void refreshEntry(com.ibm.websphere.cache.CacheEntry cacheEntry) {
        CacheEntry ce = (CacheEntry) cacheEntry;
        updateLruLocation(ce);
    }

    /**
     * used to update an entry's location in the LRU
     */
    private synchronized final void updateLruLocation(CacheEntry cacheEntry) {
        if (cacheEntry.lruHead == null) {
            int lruBucket = (lruTop + cacheEntry.priority) % lruBuckets.length;
            cacheEntry.lruHead = lruBuckets[lruBucket];
            cacheEntry.lruHead.addLast(cacheEntry);
        } else if (cacheEntry.lruHead.priority != cacheEntry.priority
                   || (cacheEntry.lruHead.priority == cacheEntry.priority && !cacheEntry.lruHead.isLast(cacheEntry))) {
            cacheEntry.lruHead.remove(cacheEntry);
            int lruBucket = (lruTop + cacheEntry.priority) % lruBuckets.length;
            cacheEntry.lruHead = lruBuckets[lruBucket];
            cacheEntry.lruHead.addLast(cacheEntry);
        }
    }

    // This method is used to update the cache statistics when getting CE found to be invalid ( VET < t < RET) earlier.
    @Override
    public void updateStatisticsForVBC(com.ibm.websphere.cache.CacheEntry ce, boolean directive) { // LIDB4537-24
        CacheEntry cacheEntry = (CacheEntry) ce;
        Object id = cacheEntry.getIdObject();
        int vbcSource = cacheEntry.getVBCSource();
        String template = cacheEntry.getTemplate();
        long valueSize = cacheEntry.getCacheValueSize();
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "updateStatisticsForVBC() cacheName=" + this.cacheName + " useCachedContent=" + directive + " id=" + id + " vbcSource="
                         + vbcSource);
        }

        CachePerf cachePerf = cachePerfRef.get();
        if (directive == DynaCacheConstants.VBC_CACHE_NEW_CONTENT) {
            // CACHE_NEW_CONTENT
            if (vbcSource == DynaCacheConstants.VBC_INVALID_MEMORY_HIT || vbcSource == DynaCacheConstants.VBC_INVALID_DISK_HIT) {
                // local memory or disk
                cacheStatisticsListener.cacheMiss(id);
            } else if (vbcSource == DynaCacheConstants.VBC_INVALID_REMOTE_HIT) {
                // remote
                cacheStatisticsListener.remoteObjectMisses(id);
            }
            if (cachePerf != null && cachePerf.isPMIEnabled()) {
                cachePerf.onCacheMiss(template, CachePerf.LOCAL);
            }
        } else {
            // USE_CACHED_CONTENT
            if (vbcSource == DynaCacheConstants.VBC_INVALID_MEMORY_HIT) {
                // local memory
                cacheStatisticsListener.localCacheHit(id, CachePerf.MEMORY);
                if (cachePerf != null && cachePerf.isPMIEnabled()) {
                    cachePerf.onCacheHit(template, CachePerf.MEMORY);
                }
            } else if (vbcSource == DynaCacheConstants.VBC_INVALID_DISK_HIT) {
                // disk
                cacheStatisticsListener.localCacheHit(id, CachePerf.DISK);
                if (cachePerf != null && cachePerf.isPMIEnabled()) {
                    cachePerf.onCacheHit(template, CachePerf.DISK);
                }
            } else if (vbcSource == DynaCacheConstants.VBC_INVALID_REMOTE_HIT) {
                // remote
                this.cacheStatisticsListener.remoteObjectHits(id, (int) valueSize);
                if (cachePerf != null && cachePerf.isPMIEnabled()) {
                    cachePerf.onCacheHit(template, CachePerf.REMOTE);
                }
            }
        }
    }

    /**
     * This sets the actual value (JSP or command) of an entry in the cache. It coordinates with other caches that have
     * cached the value.
     * 
     * @param entryInfo
     *            The cache entry
     * @param value
     *            The value to cache in the entry
     * @param coordinate
     *            Indicates that the value should be set in other caches caching this value.
     * @param directive
     *            boolean to indicate CACHE_NEW_CONTENT or USE_CACHED_VALUE
     */
    @Override
    public void setValue(EntryInfo entryInfo, Object value, boolean coordinate, boolean directive) {
        if (entryInfo == null) {
            throw new NullPointerException("input parameter entryInfo is null.");
        }
        if (entryInfo.getIdObject() == null) {
            throw new NullPointerException("entryInfo.getIdObject() is null.");
        }

        if (!entryInfo.wasPrioritySet()) {
            entryInfo.setPriority(defaultPriority);
        }

        if (entryInfo.getSharingPolicy() == EntryInfo.SHARED_PUSH_PULL && entryInfo.getValidatorExpirationTime() != -1) {
            invalidateById(entryInfo.id, CachePerf.DIRECT, CachePerf.LOCAL, true, !InvalidateByIdEvent.INVOKE_INTERNAL_INVALIDATE_BY_ID,
                           !InvalidateByIdEvent.INVOKE_DRS_RENOUNCE);
        }

        // _syncSetValue() will inc the refCount
        CacheEntry cacheEntry = _syncSetValue(entryInfo, value, directive);

        if (cacheEntry.timeLimit > 0 || cacheEntry.inactivity > 0) { // CPF-Inactivity
            timeLimitDaemon.valueHasChanged(this, cacheEntry.id, cacheEntry.expirationTime, cacheEntry.inactivity);
        }

        // Local only - No coordination
        if (entryInfo.isNotShared()) {
            cacheEntry.finish();
            return;
        }

        // Set CacheEntry on Central Cache
        if (coordinate) {
            this.updatePeerCaches(cacheEntry);
        } else {
            cacheEntry.finish();
        }
    }

    // 01/08/2004 - this method will now inc the refCount
    // Refactored into synchronized method because calling
    // a synchronized method is much better for performance
    // than a synchronized(this).
    private synchronized CacheEntry _syncSetValue(EntryInfo entryInfo, Object value, boolean directive) {
        // Do not use getCacheEntry wrapper here (similar to _syncSetEntry)
        // since we don't need to read in a stale entry.
        int cause = -1;

        // set hasPushPullEntries to true when an entry uses Push-Pull mode or Pull mode
        if (hasPushPullEntries == false
            && (entryInfo.sharingPolicy == EntryInfo.SHARED_PUSH_PULL || entryInfo.sharingPolicy == EntryInfo.SHARED_PULL)) { // PK59026
            hasPushPullEntries = true;
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "_syncSetValue() Entry uses Push-Pull or Pull mode cacheName=" + cacheName + " id=" + entryInfo.id);
            }
        }

        boolean updateLru = true;

        CacheEntry cacheEntry = (CacheEntry) entryHashtable.get(entryInfo.getIdObject());

        if (cacheEntry == null) {
            cause = ChangeEvent.NEW_ENTRY_ADDED;

            cacheEntry = getFreeLruEntry();
            entryHashtable.put(entryInfo.getIdObject(), cacheEntry);

            boolean found = false;
            // if not in memory, clean up potential entry from disk
            if (swapToDisk) {
                if (diskCache.containsKey(entryInfo.getIdObject())) {
                    if (directive == DynaCacheConstants.VBC_CACHE_NEW_CONTENT) {
                        // CACHE_NEW_CONTENT - add the id to invalidationBuffer and remove the entry from disk later
                        cacheEntry.id = entryInfo.getIdObject();
                        diskCache.delCacheEntry(cacheEntry, CachePerf.DIRECT, CachePerf.LOCAL, !FROM_DEPID_TEMPLATE_INVALIDATION);
                        cause = ChangeEvent.EXISTING_VALUE_CHANGED;
                    } else {
                        // USE_CACHED_CONTENT
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "_syncSetValue(): cacheName=" + this.cacheName
                                         + " this entry is found in the disk but not in memory for USE_CACHED_CONTENT. id=" + entryInfo.id);
                        }
                        CacheEntry tempCE = diskCache.readCacheEntry(entryInfo.getIdObject());
                        if (tempCE != null) {
                            found = true;
                            // find entry from disk. Update with new expiration times for GC and disk header
                            diskCache.updateExpirationTime(entryInfo.getIdObject(), tempCE.expirationTime, (int) tempCE.getCacheValueSize(),
                                                           entryInfo.expirationTime, entryInfo.validatorExpirationTime);
                            // make a copy of CE and then update with new expiration times
                            cacheEntry.copy(tempCE);
                            cacheEntry.setValidatorExpirationTime(entryInfo.validatorExpirationTime);
                            // cacheEntry.setExpirationTime(entryInfo.expirationTime); // comment out because VBC does
                            // not change real expiration time
                            cacheEntry.loadedFromDisk = true;
                            cause = ChangeEvent.EXPIRATION_TIMES_CHANGED;
                            updateLru = false; // don't update LRU location because the CE is not in memory
                        }
                    }
                }
            }
            // if the entry cannot find from memory and disk for USE_CACHED_CONTENT, it is unexpected error.
            // treat it like CACHE_NEW_CONTENT
            if (found == false && directive == DynaCacheConstants.VBC_USE_CACHED_CONTENT) {
                directive = DynaCacheConstants.VBC_CACHE_NEW_CONTENT;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "_syncSetValue(): cacheName=" + this.cacheName
                                 + " could not find cache entry from memory/disk for USE_CACHED_CONTENT. id=" + entryInfo.id);
                }
            }
        } else {
            cause = ChangeEvent.EXISTING_VALUE_CHANGED;

            if (directive == DynaCacheConstants.VBC_CACHE_NEW_CONTENT) {
                decreaseCacheSizeInBytes(cacheEntry);
            }

            if (swapToDisk && cacheEntry.loadedFromDisk) {
                if (directive == DynaCacheConstants.VBC_CACHE_NEW_CONTENT) {
                    // CACHE_NEW_CONTENT - add the id to invalidationBuffer and remove the entry from disk later
                    diskCache.delCacheEntry(cacheEntry, CachePerf.DIRECT, CachePerf.LOCAL, !FROM_DEPID_TEMPLATE_INVALIDATION);
                    removeInvalidationInfo(cacheEntry);
                } else {
                    // USE_CACHED_CONTENT
                    // update with new expiration times for GC and disk header
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "_syncSetValue(): cacheName=" + this.cacheName
                                     + " this entry is found in memory/disk for USE_CACHED_CONTENT. id=" + entryInfo.id);
                    }
                    diskCache.updateExpirationTime(entryInfo.getIdObject(), cacheEntry.expirationTime, (int) cacheEntry.getCacheValueSize(),
                                                   entryInfo.expirationTime, entryInfo.validatorExpirationTime);
                    // update existing CE with new expiration times
                    cacheEntry.setValidatorExpirationTime(entryInfo.validatorExpirationTime);
                    // cacheEntry.setExpirationTime(entryInfo.expirationTime); // comment out because VBC does not
                    // change real expiration time
                }
            } else {
                if (directive == DynaCacheConstants.VBC_CACHE_NEW_CONTENT) {
                    // CACHE_NEW_CONTENT
                    removeInvalidationInfo(cacheEntry);
                } else {
                    // USE_CACHED_CONTENT
                    // update existing CE with new expiration times
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "_syncSetValue(): cacheName=" + this.cacheName + " this entry is found in memory for USE_CACHED_CONTENT. id="
                                     + entryInfo.id);
                    }
                    cacheEntry.setValidatorExpirationTime(entryInfo.validatorExpirationTime);
                    // cacheEntry.setExpirationTime(entryInfo.expirationTime); // comment out because VBC does not
                    // change real expiration time
                }
            }
        }

        CachePerf cachePerf = cachePerfRef.get();
        if (cachePerf != null && cause == ChangeEvent.NEW_ENTRY_ADDED && directive == DynaCacheConstants.VBC_CACHE_NEW_CONTENT
            && cachePerf.isPMIEnabled()) {
            cachePerf.onEntryCreation(entryInfo.getTemplate(), CachePerf.LOCAL);
        }

        if (directive == DynaCacheConstants.VBC_CACHE_NEW_CONTENT) {
            // CACHE_NEW_CONTENT - update the CE using entryInfo, dependency tables and value.
            cacheEntry.copyMetaData(entryInfo);
            updateInvalidationHashtable(cacheEntry);
            cacheEntry.setValue(value);
            increaseCacheSizeInBytes(cacheEntry);
        }
        if (updateLru) {
            updateLruLocation(cacheEntry);
        }

        Enumeration e = cacheEntry.getAliasList();
        if (e != null) {
            while (e.hasMoreElements()) {
                Object alias = e.nextElement();
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "_syncSetValue() adding alias=" + alias);
                }
                entryInfo.addAlias(alias);
            }
        }

        cacheEntry.timeStamp = System.currentTimeMillis();
        cacheEntry.incRefCount();

        // TODO CPF_CODE_REVIEW - Add pooling to ChangeEvent
        if (bEnableListener && eventSource.getChangeListenerCount() > 0) {
            ChangeEvent event = new ChangeEvent(cacheEntry.id, cacheEntry.getValue(), cause, ChangeEvent.LOCAL, cacheName);
            eventSource.cacheEntryChanged(event);
        }
        return cacheEntry;
    }

    /**
     * This is a helper method called by setEntry and setValue.
     */
    private void updateInvalidationHashtable(CacheEntry cacheEntry) {
        Object id = cacheEntry.id;

        if (!this.cacheConfig.disableDependencyId) {
            // Add dependencies for all dataIds to the cacheEntry
            for (int i = 0; i < cacheEntry._dataIds.length; i++)
                dataDependencyTable.add(cacheEntry._dataIds[i], id);
        }

        if (!this.cacheConfig.disableTemplatesSupport) {
            // Add dependencies for all templates to the cacheEntry
            for (int i = 0; i < cacheEntry._templates.length; i++)
                templateDependencyTable.add(cacheEntry._templates[i], id);
        }
    }

    /**
     * This is a helper method called by setEntry and setValue.
     */
    private void removeInvalidationInfo(CacheEntry cacheEntry) {
        Object id = cacheEntry.id;

        if (!this.cacheConfig.disableDependencyId) {
            // Removedependencies for all dataIds to the cacheEntry
            for (int i = 0; i < cacheEntry._dataIds.length; i++)
                dataDependencyTable.removeEntry(cacheEntry._dataIds[i], id);
        }

        if (!this.cacheConfig.disableTemplatesSupport) {
            // Remove dependencies for all templates to the cacheEntry
            for (int i = 0; i < cacheEntry._templates.length; i++)
                templateDependencyTable.removeEntry(cacheEntry._templates[i], id);
        }
    }

    /**
     * This is used by the CacheHook to determine if an entry has been either removed or invalidated while it is being
     * rendered.
     * 
     * @param id
     *            The cache id for the entry being tested.
     * @return True if id is in cache and !removeWhenUnpinned.
     */
    @Override
    public boolean isValid(String id) {
        CacheEntry cacheEntry = getCacheEntry(id, !ASK_PERMISSION, IGNORE_COUNTING, null, !INCREMENT_REFF_COUNT);
        if (cacheEntry == null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "isValid() cacheName=" + this.cacheName + " id=" + id + " cacheEntry == null");
            return false;
        }
        if (cacheEntry.pendingRemoval) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "isValid() cacheName=" + this.cacheName + " id=" + id + " cacheEntry.invalid");
            return false;
        }

        // Do I still need this rest?
        // (I do while I only treat invalid as a very short window.)
        return !cacheEntry.removeWhenUnpinned;
    }

    // This method is used by distributedMap.put()
    @Override
    public Object invalidateAndSet(EntryInfo ei, Object value, boolean coordinate) {
        final String methodName = "invalidateAndSet()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, methodName + " cacheName=" + this.cacheName + " id=" + ei.getIdObject() + " coordinate=" + coordinate);

        Object oldValue = null;
        if (ei == null) {
            return null;
        }

        Object id = ei.getIdObject();

        CachePerf cachePerf = cachePerfRef.get();
        if (cachePerf != null && cachePerf.isPMIEnabled())
            cachePerf.onRequest(ei.getTemplate(), CachePerf.LOCAL);

        CacheEntry ce = getCacheEntry(id, !ASK_PERMISSION, IGNORE_COUNTING, null, INCREMENT_REFF_COUNT);
        if (ce != null) {
            oldValue = ce.getValue();
            ce.finish();
        } else
            oldValue = null;
        
        if ( ce != null )
        	if (ce.timeLimit > 0 || ce.inactivity > 0 ) {
        		if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        			Tr.debug(tc, methodName + " cacheName=" + this.cacheName + " id=" + id + " timeLimit=" + ce.timeLimit + " inactivity=" + ce.inactivity );
        		timeLimitDaemon.valueWasRemoved(this, id);
        	}

        if (ei.isSharedPull() || (cacheConfig.propogateInvalidationsNotShared && ei.isNotShared())) {
            // fix DRS message ordering problem by skipping the removal cache id from peer server's PushPullTable
            // defect 495487 waitOnInvalidation is set to false for Push-Pull optimization in Batch mode
            invalidateById(id, CachePerf.DIRECT, CachePerf.LOCAL, false, !InvalidateByIdEvent.INVOKE_INTERNAL_INVALIDATE_BY_ID,
                           !InvalidateByIdEvent.INVOKE_DRS_RENOUNCE);
        }

        if (!ei.wasPrioritySet()) {
            ei.setPriority(defaultPriority);
        }

        // _syncSetValue() will inc the refCount
        CacheEntry cacheEntry = _syncSetValue(ei, value, DynaCacheConstants.VBC_CACHE_NEW_CONTENT);

        if (cacheEntry.timeLimit > 0 || cacheEntry.inactivity > 0) {
            timeLimitDaemon.valueHasChanged(this, cacheEntry.id, cacheEntry.expirationTime, cacheEntry.inactivity);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " sharingPolicy=" + ei.sharingPolicy + " timeToLive=" + ei.timeLimit + " inactivity=" + ei.inactivity);
        }

        // Local only means no coordination
        if (ei.isNotShared()) {
            cacheEntry.finish();
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, methodName + " id=" + id);
            return oldValue;
        }

        // Set CacheEntry on Central Cache
        if (coordinate) {
            updatePeerCaches(cacheEntry);
        } else {
            cacheEntry.finish();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, methodName + " id=" + id);

        return oldValue;

    }

    /**
     * Returns the value to which this map maps the specified cache id. Returns <tt>null</tt> if the map contains no
     * mapping for this key.
     * 
     * @param id
     *            cache id whose associated value is to be returned.
     * @param template
     *            template name associated with cache id (No effect on CoreCache)
     * @param askPermission
     *            True implies that execution must ask the coordinating CacheUnit for permission.
     * @param ignoreCounting
     *            True implies that no counting for PMI and cache statistics.
     * @return the value to which this map maps the specified cache id, or <tt>null</tt> if the map contains no mapping
     *         for this cache id.
     */
    @Override
    public Object getValue(Object id, String template, boolean askPermission, boolean ignoreCounting) {
        CachePerf cachePerf = null;
        if (!ignoreCounting) {
            cachePerf = cachePerfRef.get();
        }

        if (cachePerf != null && cachePerf.isPMIEnabled())
            cachePerf.onRequest(template, CachePerf.LOCAL);

        Object value = null;
        if (id != null) {
            CacheEntry cacheEntry = getCacheEntry(id, askPermission, ignoreCounting, template, INCREMENT_REFF_COUNT);
            if (cacheEntry != null) { /* Fix introduced for ST */
                value = cacheEntry.getValue();
                cacheEntry.finish();
            }
        }
        return value;
    }

    /**
     * This invalidates all entries in this Cache having a dependency on this id.
     * 
     * @param id
     *            cache id or dependency id.
     * @param causeOfInvalidation
     *            The cause of invalidation
     * @param waitOnInvalidation
     *            True indicates that this method should not return until the invalidations have taken effect on all
     *            caches. False indicates that the invalidations will be queued for later batch processing.
     * @param checkPreInvalidationListener
     *            true indicates that we will verify with the preInvalidationListener prior to invalidating. False means
     *            we will bypass this check.
     */
    @Override
    public void invalidateById(Object id, int causeOfInvalidation, boolean waitOnInvalidation, boolean checkPreInvalidationListener) {
        if (id != null) {
            batchUpdateDaemon.invalidateById(id, causeOfInvalidation, waitOnInvalidation, this, checkPreInvalidationListener);
        }
    }

    /**
     * This invalidates all entries in this Cache having a dependency on this id.
     * 
     * @param id
     *            cache id or dependency id.
     * @param causeOfInvalidation
     *            The cause of invalidation
     * @param sourceOfInvalidation
     *            The source of invalidation (local or remote)
     * @param waitOnInvalidation
     *            True indicates that this method should not return until the invalidations have taken effect on all
     *            caches. False indicates that the invalidations will be queued for later batch processing.
     * @param invokeInternalInvalidation
     *            true indicates that the local invalidation should happen. False means do not invoke local
     *            invalidation. It is used to prevent infinite loop.
     * @param invokeDRSRenounce
     *            true indicates to invoke DRS renounce during sending the message to remote server.
     */
    @Override
    public void invalidateById(Object id, int causeOfInvalidation, int sourceOfInvalidation, boolean waitOnInvalidation,
                               boolean invokeInternalInvalidateById, boolean invokeDRSRenounce) { // LI4337-17
        if (id != null) {
            batchUpdateDaemon.invalidateById(id, causeOfInvalidation, sourceOfInvalidation, waitOnInvalidation, invokeInternalInvalidateById,
                                             invokeDRSRenounce, this, Cache.CHECK_PREINVALIDATION_LISTENER);
        }
    }

    /**
     * This invalidates all entries in this Cache having a dependency on this template.
     * 
     * @param template
     *            The template name.
     * @param waitOnInvalidation
     *            True indicates that this method should not return until the invalidations have taken effect on all
     *            caches. False indicates that the invalidations will be queued for later batch processing.
     */
    @Override
    public void invalidateByTemplate(String template, boolean waitOnInvalidation) {
        if (template != null) {
            batchUpdateDaemon.invalidateByTemplate(template, waitOnInvalidation, this);
        }
    }

    /**
     * This applies a set of invalidations and new entries to this Cache. It is called by this Cache's CacheUnit when it
     * arrives remotely. It is called by this Cache's BatchUpdateDaemon when it arrives locally.
     * 
     * @param invalidateIdEvents
     *            A Vector of invalidate by id.
     * @param invalidateTemplateEvents
     *            A Vector of invalidate by template.
     * @param pushEntryEvents
     *            A Vector of cache entries.
     */
    @Override
    public void batchUpdate(HashMap invalidateIdEvents, HashMap invalidateTemplateEvents, ArrayList pushEntryEvents) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "batchUpdate cacheName=" + cacheName, new Object[] { invalidateIdEvents, invalidateTemplateEvents, pushEntryEvents });

        // ----------------------------------------------------
        // 1 of 3 - Handle invalidateIdEvents
        // ----------------------------------------------------
        Iterator it = invalidateIdEvents.values().iterator();
        while (it.hasNext()) {
            InvalidateByIdEvent idEvent = (InvalidateByIdEvent) it.next();
            if (isEnableListener() && getEventSource().getPreInvalidationListenerCount() > 0 && idEvent.source == InvalidationEvent.REMOTE) {
                if (getEventSource().shouldInvalidate(idEvent.getId(), idEvent.source, idEvent.causeOfInvalidation) == false) {
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "invalidateById() cacheName=" + getCacheName() + " skip invalidation of id=" + idEvent.getId()
                                     + " because PreInvalidationListener.shouldInvalidate() returned false.");
                    }
                    continue;
                }
            }

            if (idEvent.isInvokeInternalInvalidateById()) { // local LRU events are already processed
                internalInvalidateById(idEvent.getId(), idEvent.causeOfInvalidation, idEvent.source, FIRE_INVALIDATION_LISTENER);
            }
        }
        // ----------------------------------------------------

        // ----------------------------------------------------
        // 2 of 3 - Handle invalidateTemplateEvents
        // ----------------------------------------------------
        it = invalidateTemplateEvents.values().iterator();
        while (it.hasNext()) {
            InvalidateByTemplateEvent invalidateByTemplateEvent = (InvalidateByTemplateEvent) it.next();
            // Check command type on the event
            if (invalidateByTemplateEvent.isCacheCommand_Clear()) {
                clearLocal(invalidateByTemplateEvent.source);
            } else if (invalidateByTemplateEvent.isCacheCommand_InvalidateByTemplate()) {
                internalInvalidateByTemplate(invalidateByTemplateEvent);
            } else
                throw new IllegalStateException("Program check - cache command unknown.");
        }
        // ----------------------------------------------------

        // ----------------------------------------------------
        // 3 of 3 - Handle pushEntryEvents
        // ----------------------------------------------------
        it = pushEntryEvents.iterator();
        while (it.hasNext()) {
            CacheEntry cacheEntry = (CacheEntry) it.next();
            setEntry(cacheEntry, CachePerf.REMOTE);
        }
        // ----------------------------------------------------

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "batchUpdate cacheName=" + cacheName);
    }

    /**
     * This invalidates all entries in this Cache having a dependency on this template.
     * 
     * @param template
     *            A template name.
     * @note Really should not be public, but needed to access from ServletWrapper
     */
    private synchronized void internalInvalidateByTemplate(InvalidateByTemplateEvent event) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "internalInvalidateByTemplate() cacheName=" + cacheName + " template=[" + event.getTemplate() + "]");

        long start = System.nanoTime();
        String template = event.getTemplate().trim();
        int source = event.source;

        if (!this.cacheConfig.disableTemplatesSupport) {
            ValueSet valueSet = templateDependencyTable.removeDependency(template);
            if (swapToDisk) {
                ValueSet vs = diskCache.readTemplate(template, HTODDynacache.DELETE); // CCC-2
                if (valueSet == null)
                    valueSet = vs;
                else {
                    valueSet.union(vs);
                    vs.clear();
                }
            }
            if (valueSet != null && valueSet.size() > 0) {
                event.addRemovedIds(valueSet);
                this.entriesInMemoryRemoved = 0;
                this.entriesInDiskRemoved = 0;
                loopRemove(valueSet, CachePerf.DIRECT, source, FIRE_INVALIDATION_LISTENER);
                if (this.entriesInDiskRemoved > 0) {
                    this.cacheStatisticsListener.templateBasedInvalidationsFromDisk(template);
                }
                valueSet.clear();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    String msg = "internalInvalidateByTemplate() cacheName=" + cacheName + " template=" + template + " numOfMemoryEntries="
                                 + this.entriesInMemoryRemoved + " numOfDiskEntries=" + this.entriesInDiskRemoved + " cause=" + CachePerf.DIRECT
                                 + " source=" + source2Text(source) + " elapsed=" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + " listenerEnabled="
                                 + bEnableListener;
                    Tr.debug(tc, msg);
                }
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "internalInvalidateByTemplate() template=" + template);
    }

    /**
     * This invalidates all entries in this Cache having a dependency on this id.
     * 
     * @param id
     *            A cache id or data id.
     * @param causeOfInvalidation
     *            The cause of invalidation...see InvalidateByIdEvent.
     */
    private synchronized boolean internalInvalidateById(Object id, int causeOfInvalidation, int source, boolean bFireIL) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "internalInvalidateById() cacheName=" + this.cacheName + " id=" + id);

        boolean rc = remove(id, causeOfInvalidation, source, bFireIL, !FROM_DEPID_TEMPLATE_INVALIDATION);

        internalInvalidateByDepId(id, causeOfInvalidation, source, bFireIL);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "internalInvalidateById: cacheName=" + cacheName + " id=" + id + " rc=" + rc);

        return rc;
    }

    /**
     * This invalidates all entries in this Cache having a dependency on this dependency id.
     * 
     * @param id
     *            dependency id.
     * @param causeOfInvalidation
     *            The cause of invalidation
     * @param source
     *            The source of invalidation (local or remote)
     * @param bFireIL
     *            True to fire invalidation event
     */
    @Override
    public synchronized void internalInvalidateByDepId(Object id, int causeOfInvalidation, int source, boolean bFireIL) {
        if (!this.cacheConfig.disableDependencyId) {
            long start = System.nanoTime();
            this.entriesInMemoryRemoved = 0;
            this.entriesInDiskRemoved = 0;

            ValueSet valueSet = dataDependencyTable.removeDependency(id);
            if (swapToDisk) {
                if (valueSet == null)
                    valueSet = diskCache.readDependency(id, HTODDynacache.DELETE);
                else
                    valueSet.union(diskCache.readDependency(id, HTODDynacache.DELETE));
            }
            if (valueSet != null && valueSet.size() > 0) {
                loopRemove(valueSet, causeOfInvalidation, source, bFireIL);
                valueSet.clear();
                valueSet = null;
                if (this.entriesInDiskRemoved > 0) {
                    this.cacheStatisticsListener.depIdBasedInvalidationsFromDisk(id);
                }
                if (TraceComponent.isAnyTracingEnabled()) {
                    CachePerf cachePerf = cachePerfRef.get();
                    boolean pmiEnabled = (cachePerf != null) ? cachePerf.isPMIEnabled() : false;
                    String msg = "internalInvalidateByDepId() cacheName=" + cacheName + " dep-id=" + id + " numOfMemoryEntries="
                                 + this.entriesInMemoryRemoved + " numOfDiskEntries=" + this.entriesInDiskRemoved + " cause=" + cause2Text(causeOfInvalidation)
                                 + " source=" + source2Text(source) + " elapsed=" + TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) + " listenerEnabled="
                                 + bEnableListener + " PMIEnabled=" + pmiEnabled;
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, msg);
                    } else if (tc.isEventEnabled()) {
                        Tr.event(tc, msg);
                    }
                    // System.out.println("*** " + msg);
                }
            }
        }
    }

    private void loopRemove(ValueSet valueSet, int causeOfInvalidation, int source, boolean bFireIL) {
        if (valueSet != null && !valueSet.isEmpty()) {
            Iterator it = valueSet.iterator();
            while (it.hasNext()) {
                Object entryId = it.next();
                remove(entryId, causeOfInvalidation, source, bFireIL, FROM_DEPID_TEMPLATE_INVALIDATION);
            }
        }
    }

    /**
     * Remove the entry identified by the cache id from the Cache. If pinned, this will remove the entry when it is no
     * longer pinned. If an entry with this id does not exist, this method does nothing. WARNING: The id can be a cache
     * Id, alias Id, dep Id if bReadDiskValue is true. The id must be cache Id if bReadDiskValue is false
     */
    private synchronized final boolean remove(Object id, int causeOfInvalidation, int source, boolean bFireIL, boolean fromDepIdTemplateInvalidation) {
        if (id == null) {
            throw new NullPointerException("input parameter id is null.");
        }

        boolean foundOnDisk = false;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "Cache.remove() cacheName=" + cacheName + " id=" + id + " cause=" + cause2Text(causeOfInvalidation) + " source=" + source2Text(source));
        }

        // -----------------------------------------------------------
        // Look for CE in memory or disk & remember where found
        // -----------------------------------------------------------
        CacheEntry cacheEntry = (CacheEntry) entryHashtable.get(id);
        boolean allocateCE = false;
        if (cacheEntry == null && swapToDisk) {
            // Read cache entry from disk when
            // (1) fromDepIdTemplateInvalidation = false
            // (2) listener is enabled AND bFireIL is true AND invalidation listeners > 0 AND
            // ignoreValueInInvalidationEvent is false
            // Otherwise, get CE from the pool
            if (!fromDepIdTemplateInvalidation) {
                if (bEnableListener && bFireIL && eventSource.getInvalidationListenerCount() > 0 && this.ignoreValueInInvalidationEvent == false) {
                    cacheEntry = diskCache.readCacheEntry(id, HTODDynacache.CALLED_FROM_REMOVE);
                } else {
                    if (diskCache.isCacheIdInAuxDepIdTable(id) == true) { // cache id found in the aux dependency table?
                        allocateCE = true;
                    } else {
                        if (diskCache.containsKey(id) == true) { // cache id found in the disk cache?
                            allocateCE = true;
                        }
                    }
                }
            } else {
                if (bEnableListener && bFireIL && eventSource.getInvalidationListenerCount() > 0 && this.ignoreValueInInvalidationEvent == false) {
                    cacheEntry = diskCache.readCacheEntry(id, HTODDynacache.CALLED_FROM_REMOVE);
                } else {
                    allocateCE = true;
                }
            }

            if (allocateCE) {
                cacheEntry = cacheEntryPool.allocate();
                cacheEntry.id = id;
            }

            if (cacheEntry != null) {
                foundOnDisk = true;
            }
        }
        // -----------------------------------------------------------

        // The entry will NOT add to the TimeLimitDaemon's heap if the entry is never timeout (TimeLimit <= 0)
        // If the cache entry is found, check the TimeLimit > 0 before calling timeLimitDaemon.valueWasRemoved().
        if (!foundOnDisk) {
            if (cacheEntry != null && cacheEntry.timeLimit > 0) {
                timeLimitDaemon.valueWasRemoved(this, cacheEntry.id);
            }
        } else {
            // cache is found on the disk. If the disk performance is set to high, remove the id from TimeLimitDaemon.
            if (allocateCE && this.cacheConfig.diskCachePerformanceLevel == CacheConfig.HIGH) {
                timeLimitDaemon.valueWasRemoved(this, id);
            } else {
                if (cacheEntry != null && this.cacheConfig.diskCachePerformanceLevel == CacheConfig.HIGH && cacheEntry.timeLimit > 0) {
                    timeLimitDaemon.valueWasRemoved(this, cacheEntry.id);
                }
            }
        }

        // -----------------------------------------------------------
        // If CE not found or found + invalid, just return
        // -----------------------------------------------------------
        if ((cacheEntry == null) || (cacheEntry.pendingRemoval) || (cacheEntry.removeWhenUnpinned)) {
            return false;
        }
        // -----------------------------------------------------------

        if (!foundOnDisk && causeOfInvalidation != -1 && !cacheEntry.loadedFromDisk) {
            CachePerf cachePerf = cachePerfRef.get();
            if (cachePerf != null && cachePerf.isPMIEnabled())
                cachePerf.onInvalidate(cacheEntry.getTemplate(), causeOfInvalidation, CachePerf.MEMORY, source);
        }
        if (!foundOnDisk) {
            cacheStatisticsListener.remove(id, causeOfInvalidation, CachePerf.MEMORY, source);
        }
        cacheEntry.pendingRemoval = true;
        // -----------------------------------------------------------

        // Defect 180174
        // If the value has not been accessed by cacheEntry.getValue()
        // on a user thread, just return the serialized value in
        // the invalidation event.
        Object value = null;
        if (cacheEntry.serializedValue != null) {
            value = cacheEntry.serializedValue;
        } else {
            value = cacheEntry.getValue();
        }

        // Removedependencies for all dataIds to the cacheEntry
        if (!foundOnDisk && !this.cacheConfig.disableDependencyId) {
            for (int i = 0; i < cacheEntry._dataIds.length; i++) {
                boolean found = dataDependencyTable.removeEntry(cacheEntry._dataIds[i], id);
                if (!found && swapToDisk && !cacheEntry.loadedFromDisk) {
                    diskCache.delDependencyEntry(cacheEntry._dataIds[i], id);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "**** Cache.remove: did=" + cacheEntry._dataIds[i] + " id=" + id);
                }
            }
        }

        // Remove dependencies for all templates to the cacheEntry
        if (!foundOnDisk && !this.cacheConfig.disableTemplatesSupport) {
            for (int i = 0; i < cacheEntry._templates.length; i++) {
                templateDependencyTable.removeEntry(cacheEntry._templates[i], id);
            }
        }

        // Get return to see if entry exists in memory,
        // if not, check disk
        if (!foundOnDisk) {
            entryHashtable.remove(cacheEntry.id);
            decreaseCacheSizeInBytes(cacheEntry);
            this.entriesInMemoryRemoved++;
            if (fromDepIdTemplateInvalidation && source != CachePerf.REMOTE
                && (cacheEntry.sharingPolicy == EntryInfo.SHARED_PUSH_PULL || cacheEntry.sharingPolicy == EntryInfo.SHARED_PULL)) {
                invalidateById(cacheEntry.id, causeOfInvalidation, source, false, !InvalidateByIdEvent.INVOKE_INTERNAL_INVALIDATE_BY_ID);
            }

            if (cacheEntry.loadedFromDisk) {
                diskCache.delCacheEntry(cacheEntry, causeOfInvalidation, source, fromDepIdTemplateInvalidation);
            }
        } else {
            // proces disk entry (this will only occur if swapToDisk == true)
            diskCache.delCacheEntry(cacheEntry, causeOfInvalidation, source, fromDepIdTemplateInvalidation);
            this.entriesInDiskRemoved++;
        }

        if (bEnableListener && bFireIL && eventSource.getInvalidationListenerCount() > 0 && causeOfInvalidation > 0) { // CCC-CE
            int src = InvalidationEvent.LOCAL;
            if (source == CachePerf.REMOTE) {
                src = InvalidationEvent.REMOTE;
            }
            // Value is valid if ignoreValueInInvalidationEvent is false. Otherwise, leave the value to NULL.
            if (this.ignoreValueInInvalidationEvent == true) {
                value = null;
            }
            InvalidationEvent ie = new InvalidationEvent(cacheEntry.id, value, causeOfInvalidation, src, this.cacheName);
            eventSource.fireEvent(ie);
        }
        if (!foundOnDisk) {
            for (int i = 0; i < cacheEntry.aliasList.length; i++) {
                entryHashtable.remove(cacheEntry.aliasList[i]);
                decreaseCacheSizeInBytes(ObjectSizer.FASTHASHTABLE_PER_ENTRY_OVERHEAD + ObjectSizer.OBJECT_REF_SIZE, "ALIAS");
            }
            if (cacheEntry.getRefCount() <= 0) {
                cacheEntry.returnToPool();
            } else if (cacheEntry.removeWhenUnpinned == false) {
                cacheEntry.removeWhenUnpinned = true;
                cacheEntry.lruHead.remove(cacheEntry);
            }
        }
        if (allocateCE) {
            cacheEntry.returnToPool();
        }

        return true; // successfully removed from the cache (both mem & disk)
    }

    private String cause2Text(int cause) {
        String causeText = null;
        switch (cause) {
            case CachePerf.DIRECT:
                causeText = "DIRECT";
                break;
            case CachePerf.LRU:
                causeText = "LRU";
                break;
            case CachePerf.TIMEOUT:
                causeText = "TIMEOUT";
                break;
            case CachePerf.INACTIVE:
                causeText = "INACIVE";
                break;
            case CachePerf.DISK_GARBAGE_COLLECTOR:
                causeText = "Disk Garbage Collector";
                break;
            case CachePerf.DISK_OVERFLOW:
                causeText = "Disk Overflow";
                break;
            default:
                causeText = "unknown";
        }
        return causeText;

    }

    private String source2Text(int source) {
        String sourceText = null;
        switch (source) {
            case CachePerf.MEMORY:
                sourceText = "MEMORY";
                break;
            case CachePerf.REMOTE:
                sourceText = "REMOTE";
                break;
            case CachePerf.DISK:
                sourceText = "DISK";
                break;
            case CachePerf.NOOP:
                sourceText = "NOOP";
                break;
            case CachePerf.LOCAL:
                sourceText = "LOCAL";
                break;
            default:
                sourceText = "unknown";
        }
        return sourceText;

    }

    /**
     * This method clears everything from the cache, so that it is just like when it was instantiated.
     * 
     * @param waitOnInvalidation
     *            True indicates that this method should not return until the invalidations have taken effect on all
     *            caches. False indicates that the invalidations will be queued for later batch processing. (No effect
     *            on CoreCache)
     */
    @Override
    public void clear(boolean waitOnInvalidation) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "clear() cacheName=" + this.cacheName + " waitOnInvalidation=" + waitOnInvalidation);
        }
        if (bEnableListener && eventSource.getPreInvalidationListenerCount() > 0) {
            if (eventSource.shouldInvalidate("*", InvalidationEvent.LOCAL, InvalidationEvent.CLEAR_ALL) == false) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "clear() cacheName=" + this.cacheName
                                 + " skip clearing cache because PreInvalidationListener.shouldInvalidate() returns false.");
                }
                return;
            }
        }
        batchUpdateDaemon.cacheCommand_Clear(waitOnInvalidation, this);
    }

    // -----------------------------------------------------

    // -----------------------------------------------------
    // Helper to clear local cache
    // -----------------------------------------------------
    private synchronized void clearLocal(int source) {

        int cause = CachePerf.DIRECT;
        if (swapToDisk) {
            diskCache.clearDiskCache();
        }
        boolean savedSwapToDisk = swapToDisk;

        swapToDisk = false;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "clearLocal() cacheName=" + cacheName + " invalidating " + entryHashtable.size() + " entries");

        Enumeration e = entryHashtable.keys();
        while (e.hasMoreElements()) {
            Object id = e.nextElement();
            internalInvalidateById(id, cause, source, !FIRE_INVALIDATION_LISTENER);
        }

        swapToDisk = savedSwapToDisk;
        if (swapToDisk) {
            // Clear disk invalidation buffers because internalInvalidateById might add cache ids to the disk
            // invalidation buffers
            diskCache.clearInvalidationBuffers();
        }
        // Fire only one invalidation event instead
        // of firing for each cache id
        if (bEnableListener) {
            int src = InvalidationEvent.LOCAL;
            if (source == CachePerf.REMOTE) {
                src = InvalidationEvent.REMOTE;
            }
            InvalidationEvent ie = new InvalidationEvent("*", null, InvalidationEvent.CLEAR_ALL, src, this.cacheName);
            eventSource.fireEvent(ie);
        }

        this.timeLimitDaemon.cacheCleared(this);
        this.invalidationAuditDaemon.cacheCleared(cacheName);

        if (isCacheSizeInMBEnabled()) {
            this.currentMemoryCacheSizeInBytes = 0;
            increaseCacheSizeInBytes(ObjectSizer.FASTHASHTABLE_INITIAL_OVERHEAD + ObjectSizer.FASTHASHTABLE_INITIAL_PER_ENTRY_OVERHEAD
                                     * cacheConfig.cacheSize, "EHT");
        }

        CachePerf cachePerf = cachePerfRef.get();
        if (cachePerf != null && cachePerf.isPMIEnabled()) {
            cachePerf.onCacheClear(true, swapToDisk);
        }

    }

    // -----------------------------------------------------

    /**
     * Returns an enumeration view of the cache IDs contained in the memory cache.
     * 
     * @return An enumeration of cache IDs or empty if there is no cache ID.
     */
    @Override
    public synchronized Enumeration getAllIds() {
        return entryHashtable.keys();
    }

    /**
     * Returns a set view of the cache IDs contained in the memory cache.
     * 
     * @return A Set of cache IDs or empty set if there is no cache ID.
     */
    @Override
    public Set getCacheIds() {
        return new ValueSet(getAllIds());
    }

    /**
     * Returns a hashcode of all the cache entries in memory cache. If includeValue is true, hashcode will include the
     * cache value. lease note that the cache key and value objects should override the default
     * java.lang.Object.hashCode() method to get semantic comparability between object instances.
     * 
     * @param debug
     *            If debug is true, a list of the cache IDs and their hashcodes are written to the SystemOut log.
     * @param includeValue
     *            If includeValue is true, the hashcode of value will be in the calculation.
     * @return hashcode of all the cache entries
     */
    @Override
    public int getMemoryCacheHashcode(boolean debug, boolean includeValue) { // LI4337-17
        int totalHashcode = 0;
        StringBuffer sb = new StringBuffer();
        int count = 0;
        int totalCount = 0;
        synchronized (this) {
            // loop to retrieve each cache entry from memory cache
            Enumeration e = entryHashtable.elements();
            while (e.hasMoreElements()) {
                CacheEntry ce = (CacheEntry) e.nextElement();
                // find hashcode of cache id and then add to the total
                int id_hc = ce.id.hashCode();
                // if the entry exists also in disk, skip calculating the hashcode of cache id
                if (ce.loadedFromDisk == false) {
                    totalHashcode += id_hc;
                }
                // if includeValue is true, find hashcode of cache value and then add to the total
                // if the entry exists also in disk, skip calculating the hashcode of cache value
                if (includeValue && ce.loadedFromDisk == false) {
                    if (ce.valueHashcode == 0 && null != ce.value) {
                        ce.valueHashcode = ce.value.hashCode();
                    }
                    totalHashcode += ce.valueHashcode;
                }
                totalCount++;
                // if debug is true, put the cache id and its hashcode into string buffer and then write to
                // systemout.log
                // when it reaches 100 entries.
                if (debug) {
                    sb.append("\nid=");
                    sb.append(ce.id);
                    sb.append(" id_hashcode=");
                    sb.append(id_hc);
                    if (includeValue) {
                        sb.append(" value_hashcode=");
                        sb.append(ce.valueHashcode);
                    }
                    if (this.swapToDisk) {
                        sb.append(" existOnDisk=");
                        sb.append(ce.loadedFromDisk);
                    }
                    count++;
                    if (count == 100) {
                        Tr.info(tc, "DYNA1035I", new Object[] { String.valueOf(count), this.cacheName, sb.toString() });
                        sb.setLength(0);
                        count = 0;
                    }
                }
            }
        }
        if (debug && count > 0) {
            Tr.info(tc, "DYNA1035I", new Object[] { String.valueOf(count), this.cacheName, sb.toString() });
        }
        Tr.info(tc, "DYNA1038I", new Object[] { String.valueOf(totalCount), this.cacheName });
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "getMemoryCacheHashcode(): cacheName=" + this.cacheName + " totalCount=" + totalCount + " totalHashcode=" + totalHashcode);
        }
        return totalHashcode;
    }

    /**
     * Returns a hashcode of all the cache entries in disk cache. If includeValue is true, hashcode will include the
     * cache value. lease note that the cache key and value objects should override the default
     * java.lang.Object.hashCode() method to get semantic comparability between object instances. Since this command
     * computes a hash of the cached objects on disk, this is an CPU and I/O intensive operation.
     * 
     * @param debug
     *            If debug is true, a list of the cache IDs and their hashcodes are written to the SystemOut log.
     * @param includeValue
     *            If includeValue is true, the hashcode of value will be in the calculation.
     * @return hashcode of all the cache entries
     */
    @Override
    public int getDiskCacheHashcode(boolean debug, boolean includeValue) throws DynamicCacheException { // LI4337-17
        int totalHashcode = 0;
        int totalCount = 0;
        if (swapToDisk) {
            if (getIdsSizeDisk() > 0) {
                int index = 0; // indicate to starting from the beginning of ids on disk
                boolean more = false;
                StringBuffer sb = new StringBuffer();
                List<Object> aList = new ArrayList<Object>(100);
                // loop to retrieve 100 cache entries from disk cache until end of it.
                do {
                    // index: 0=first time 1=next; loop to retrieve 100 entries from disk cache until Result.bmore set
                    // to false
                    Result result = diskCache.readHashcodeByRange(index, 100, debug, includeValue);
                    // handle the disk exception and no hashcode for cache value (using old format) cases

                    if (result.returnCode == HTODDynacache.DISK_EXCEPTION) {
                        throw new DiskIOException("The disk IO exception has occurred when reading entries from disk cache. "
                                                  + result.diskException.getMessage());
                    } else if (result.returnCode == HTODDynacache.NO_HASHCODE_OLD_FORMAT) {
                        throw new DiskCacheUsingOldFormatException(
                                        "Getting hashcode for disk cache failed because the disk cache is using the old format. The new format includes hashcode for cache value in the disk header.");
                    }
                    // get the result
                    totalHashcode += result.totalHashcode;
                    int count = result.dataSize;
                    totalCount += count;
                    more = result.bMore;
                    // if debug is true, put the cache id and its hashcode into string buffer and then write to
                    // systemout.log
                    // when it reaches 100 entries.
                    if (debug && count > 0) {
                        List list = (List) result.data;
                        aList.addAll(list);
                        // write to systemout.log if no more entries reported or list is over 100.
                        if (aList.size() >= 100 || more == false) {
                            Object[] sa = aList.toArray();
                            // calculate how many lines write to systemout.log
                            if (more) {
                                count = 100;
                            } else {
                                count = aList.size();
                            }
                            // merge the list of hashcode strings into one string
                            for (int i = 0; i < count; i++) {
                                Object o = sa[i];
                                String s = (String) o;
                                sb.append(s);
                                aList.remove(o);
                            }
                            Tr.info(tc, "DYNA1036I", new Object[] { String.valueOf(count), this.cacheName, sb.toString() });
                            sb.setLength(0);
                        }
                    }
                    index = 1; // indicate to continue the next set of ids
                } while (more == true);
                Tr.info(tc, "DYNA1039I", new Object[] { String.valueOf(totalCount), this.cacheName });
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "getDiskCacheHashcode(): cacheName=" + this.cacheName + " diskOffloadEnabled=" + this.swapToDisk + " totalCount="
                             + totalCount + " totalHashcode=" + totalHashcode);
            }
        }
        return totalHashcode;
    }

    /**
     * Returns a hashcode of all the cache IDs in PushPullTable. Please note that the cache key and value objects should
     * override the default java.lang.Object.hashCode() and java.lang.Object.equals() method to get semantic
     * comparability between object instances.
     * 
     * @param debug
     *            If debug is true, a list of the cache IDs and their hashcodes are written to the SystemOut log.
     */
    @Override
    public int getCacheIdsHashcodeInPushPullTable(boolean debug) { // LI4337-17
        return this.remoteServices.getCacheIdsHashcodeInPushPullTable(debug);
    }

    /**
     * Returns true if memory cache contains a mapping for the specified cache ID.
     * 
     * @param cacheId
     *            cache ID is to be tested.
     * @return <code>true</code> if the memory cache contains the specified cacheID.
     */
    @Override
    public boolean containsCacheId(Object cacheId) {
        return entryHashtable.containsKey(cacheId);
    }

    /**
     * Returns true if disk cache contains a mapping for the specified cache ID.
     * 
     * @param key
     *            cache ID is to be tested.
     * @return <code>true</code> if the disk cache contains the specified cacheID.
     */
    @Override
    public boolean containsKeyDisk(Object key) {
        if (swapToDisk) {
            return diskCache.containsKey(key);
        }
        return false;
    }

    /**
     * Returns the dependency IDs for all the cache entries. A dependency provides a mechanism for grouping of
     * cache-ids.
     * 
     * A dependency-id can also be a cache-id. Dependency IDs label cache entries and are used by invalidation rules to
     * invalidate & timeout one or more cache entries at a time.
     * 
     * The relationship beween a dependency-id and its dependent ids is formed via invalidation rules in the
     * cachespec.xml for servlet caches. For object caches this relationship is explicitly specified via the
     * DistributedMap APIs.
     * 
     * @return A set of dependency IDs.
     */
    @Override
    public Collection getAllDependencyIds() {
        return getDependencyIds();
    }

    /**
     * Returns the dependency IDs for all cache entries.
     * 
     * @return A set of dependency IDs.
     */
    public synchronized Set getDependencyIds() {
        ValueSet ids = null;
        if (!this.cacheConfig.disableDependencyId) {
            Iterator e = dataDependencyTable.getKeys();
            if (e != null) {
                ids = new ValueSet(e);
            }
        }
        if (ids == null) {
            ids = new ValueSet(0);
        }
        return ids;
    }

    /**
     * Returns the cache IDs that are associated with a dependency ID. It returns an empty set if no cache ID is
     * associated with it.
     * <p>
     * <br>
     * Dependency elements are used to group related cache items. Cache entries having the same depenendency ID are
     * managed as a group. Each related cache item shares the same dependency id, so it only takes one member of the
     * dependency group to get invalidated, for the rest of the group to be evicted. The dependency ID can be as simple
     * as just a name such as storeId
     * 
     * @param dependency
     *            dependency ID or template ID for the groupof cache IDs.
     * @return A set of cache IDs or empty set if no cache ID is associated with it.
     */
    @Override
    public synchronized Set getCacheIdsByDependency(Object dependency) {
        Set ids = null;
        if (!this.cacheConfig.disableDependencyId) {
            ids = dataDependencyTable.getEntries(dependency);
        }
        if (ids == null) {
            ids = new ValueSet(0);
        }
        return ids;
    }

    /**
     * Returns the cache IDs that are associated with a template. It returns an empty set if no cache ID is associated
     * with it.
     * 
     * @param template
     *            template for the groupof cache IDs.
     * @return A set of cache IDs or empty set if no cache ID is associated with it.
     */
    @Override
    public synchronized Set getCacheIdsByTemplate(String template) {
        Set ids = null;
        if (!this.cacheConfig.disableTemplatesSupport) {
            ids = templateDependencyTable.getEntries(template);
        }
        if (ids == null) {
            ids = new ValueSet(0);
        }
        return ids;
    }

    private boolean canAllocate() {
        boolean allocate = false;
        int currentCacheEntries = getNumberCacheEntries();
        if (currentCacheEntries < cacheSizeLimit) {
            if (isCacheSizeInMBEnabled()) {
                if (currentMemoryCacheSizeInBytes < maxMemoryCacheSizeInBytes) {
                    allocate = true;
                } else {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "canAllocate() cacheName=" + this.cacheName + " currentMemoryCacheSizeInBytes=" + currentMemoryCacheSizeInBytes
                                     + " maxMemoryCacheSizeInBytes=" + maxMemoryCacheSizeInBytes + " diff="
                                     + (currentMemoryCacheSizeInBytes - maxMemoryCacheSizeInBytes));
                    }

                }
            } else {
                allocate = true;
            }
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "canAllocate() cacheName=" + this.cacheName + " currentCacheEntries=" + currentCacheEntries + " cacheSizelimit="
                             + cacheSizeLimit + " diff=" + (currentCacheEntries - cacheSizeLimit));
            }
        }
        return allocate;
    }

    /**
     * This is a helper method that is called by pin, get/setEntry and setValue methods. It tries to find a free entry
     * or an LRU victim. It first tries to find an entry on the freeList. If none on the freeList, it executes the LRU
     * clock algorithm. A lock on the cache manager is always obtained prior to calling this method.
     * 
     * @return The entry found free or made free.
     */
    private synchronized CacheEntry getFreeLruEntry() {
        CacheEntry cacheEntry = null;

        if (canAllocate()) {
            return cacheEntryPool.allocate();
        }
        try {
            FreeLruEntryResult result = freeLruEntry();
            if (result.success == true) {
                return getFreeLruEntry();
            }

            // Last resort. Everything was pinned in the cache.
            // PK33017 - increment overflow entries from memory statistic
            cacheStatisticsListener.overflowEntriesFromMemory();
            // Create an overflow entry.
            cacheEntry = cacheEntryPool.allocate();
            return cacheEntry;

        } catch (Throwable t) {
            com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.cache.Cache.getFreeLruEntry", "1181", this);
        }
        return null;
    }

    // Cache entries in the overflow buffer are purged and asynchronously offloaded to disk
    @Override
    @Trivial
    public void trimCache() {

        if (((CacheConfig) getCacheConfig()).isRefCountTrackingEnabled()) {
            lookForLeaks();
        }

        int size = getNumberCacheEntriesUnsynchronized() - this.cacheConfig.cacheSize;
        if (size > 0) { // Remove entries in EHT if the entries in EHT is over cache size
            int entriesRemoved = 0;
            for (int i = 0; i < size; i++) {
                // free up one entry
                FreeLruEntryResult result = freeLruEntry();
                if (result.success == false) {
                    break;
                }
                if (swapToDisk) {
                    cacheStatisticsListener.objectsAsyncLruToDisk();
                }
                entriesRemoved++;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "trimCache(): cacheName=" + this.cacheName + " entriesToRemove=" + size + " entriesRemoved=" + entriesRemoved
                             + " ObjectsAsyncLruToDisk=" + cacheStatisticsListener.getObjectsAsyncLruToDiskCount());
            }
        }

        // Constrain the JVM heap between high and low threshold
        // Remove entries in EHT if it is over cache size in MB in high threshold.
        long cacheSizeInBytes = this.currentMemoryCacheSizeInBytes;
        if (isCacheSizeInMBEnabled() && cacheSizeInBytes >= this.upperLimitMemoryCacheSizeInBytes) {
            long bytesToRemove = cacheSizeInBytes - this.lowerLimitMemoryCacheSizeInBytes;
            long savedBytesToRemove = bytesToRemove;
            long bytesRemoved = 0;
            int entriesRemoved = 0;
            while (bytesToRemove > 0) {
                if (this.currentMemoryCacheSizeInBytes <= this.lowerLimitMemoryCacheSizeInBytes) {
                    break;
                }
                // free up one entry
                FreeLruEntryResult result = freeLruEntry();
                if (result.success == false) {
                    break;
                }
                bytesToRemove -= result.bytesRemoved;
                bytesRemoved += result.bytesRemoved;
                if (swapToDisk) {
                    cacheStatisticsListener.objectsAsyncLruToDisk();
                }
                entriesRemoved++;
            }

            if (savedBytesToRemove > 0 && TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, "trimCache(): cacheName=" + this.cacheName + " bytesToRemove=" + savedBytesToRemove + " bytesRemoved=" + bytesRemoved
                             + " entriesRemoved=" + entriesRemoved + " ObjectsAsyncLruToDisk=" + cacheStatisticsListener.getObjectsAsyncLruToDiskCount());
            }

        }
    }

    /**
     * Remove the LRU entry from memory. If disk caching is enabled write the LRUed entry to disk
     */
    @Override
    public synchronized FreeLruEntryResult freeLruEntry() {
        FreeLruEntryResult result = new FreeLruEntryResult();
        CacheEntry cacheEntry = null;
        int endTop = (lruTop + lruBuckets.length - 1) % lruBuckets.length;
        while (cacheEntry == null && lruTop != endTop) {
            if (!lruBuckets[lruTop].isEmpty()) {
                Iterator it = lruBuckets[lruTop].iterator();
                while (it.hasNext()) {
                    cacheEntry = (CacheEntry) it.next();
                    if (cacheEntry.getRefCount() == 0) {
                        Object id = cacheEntry.id;
                        if (id != null) {
                            result.success = false;
                            if (isCacheSizeInMBEnabled()) {
                                result.bytesRemoved = cacheEntry.getObjectSize();
                            }

                            if (swapToDisk && cacheEntry.persistToDisk) {
                                LruToDiskResult toDiskResult = lruToDisk(cacheEntry);
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, toDiskResult.toString());
                                if (toDiskResult.result != HTODDynacache.DISK_EXCEPTION && toDiskResult.result != HTODDynacache.OTHER_EXCEPTION
                                    && toDiskResult.result != HTODDynacache.SERIALIZATION_EXCEPTION)
                                    result.success = true;
                            } else {
                                // Display this message only once if the disk offload feature is disabled
                                // DYNA1070I=DYNA1070I: Cache instance \"{0}\" is full and has reached the maximum
                                // configured size of {1} entries.
                                // Space on the JVM heap for new entries will now be made by evicting existing cache
                                // entries using the LRU algorithm.
                                // Please consider enabling the disk offload feature for the cache instance to prevent
                                // the discard of cache entries
                                // from memory.
                                if (swapToDisk == false && displayedLRUMessage == false) {
                                    Tr.audit(tc, "DYNA1070I", new Object[] { cacheName, new Integer(cacheConfig.cacheSize) });
                                    displayedLRUMessage = true;
                                }

                                if (shouldInvalidate(id)) {
                                    result.success = internalInvalidateById(id, CachePerf.LRU, CachePerf.LOCAL, FIRE_INVALIDATION_LISTENER);
                                }

                                if (false == cacheConfig.filterLRUInvalidation && true == result.success) {
                                    invalidateById(id, CachePerf.LRU, CachePerf.LOCAL, false, !InvalidateByIdEvent.INVOKE_INTERNAL_INVALIDATE_BY_ID);
                                }
                            }
                            if (true == result.success) {
                                if (tc.isDebugEnabled()) {
                                    Tr.exit(tc, "return freeLruEntry() true");
                                }
                                return result;
                            }
                        }
                    }
                }
            }

            // Empty or eveything was pinned, need to combine...
            int newTop = (lruTop + 1) % lruBuckets.length;
            CacheEntry.LRUHead lruBucket = lruBuckets[lruTop];
            while (!lruBucket.isEmpty()) {
                CacheEntry updateEntry = lruBucket.removeFirst();
                updateEntry.lruHead = lruBuckets[newTop];
                lruBuckets[newTop].addFirst(updateEntry);
            }

            lruTop = newTop;
            if (cacheConfig.isRefCountTrackingEnabled()) {
                Tr.warning(tc, getCacheName() + " Empty or eveything was pinned, need to combine... " + lruTop);
            }
            for (int i = 0; i < lruBuckets.length; i++)
                lruBuckets[(lruTop + i) % lruBuckets.length].priority = i;

        }
        return result;
    }

    private boolean shouldInvalidate(Object id) {

        boolean shouldInvalidate = true;

        if (this.isEnableListener() && this.getEventSource().getPreInvalidationListenerCount() > 0) {
            shouldInvalidate = this.getEventSource().shouldInvalidate(id, CachePerf.LRU, CachePerf.LOCAL);
            if (shouldInvalidate == false) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "freeLruEntry() cacheName=" + this.getCacheName() + " skip invalidation of id=" + id
                                 + " because PreInvalidationListener.shouldInvalidate() returns false.");
                }
            }
        }
        return shouldInvalidate;
    }

    // return boolean is used to determine whether the entry exists on disk
    // cache
    private synchronized LruToDiskResult lruToDisk(CacheEntry cacheEntry) {

        if (tc.isDebugEnabled()) {
            Tr.entry(tc, "lruToDisk", cacheEntry);
        }

        LruToDiskResult toDiskResult = new LruToDiskResult();
        toDiskResult.entryOverwritten = !ENTRY_OVERWRITTEN_ON_DISK;
        if (cacheEntry.id == null) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "lruToDisk.1");
            return toDiskResult;
        }

        boolean discard = false;
        int overLimitType = 0;

        // **** Step 1
        // check the following:
        // (1) Can cacheEntry be serialized
        // (2) over diskCacheSizeLimit
        // (3) check diskCacheSizeInGBLimit
        // (4) check diskCacheSizeInBytesLimit
        //
        // No write to disk if loadedFromDisk is true. It is because
        // the cache is already in the disk.
        if (!cacheEntry.loadedFromDisk) {
            ((CacheOnDisk) diskCache).htod.diskCacheException = null;
            if (!cacheEntry.prepareForSerialization()) {
                // a msg was logged during the above call
                discard = true;
                toDiskResult.result = HTODDynacache.SERIALIZATION_EXCEPTION;
            }
            if (!discard && cacheEntry.serializedValue == null) {
                discard = true;
                toDiskResult.result = HTODDynacache.SERIALIZATION_EXCEPTION;
            }

            int diskCacheSizeLimit = this.diskCache.getDiskCacheSizeLimit();
            if (!discard && diskCacheSizeLimit > 0 && getIdsSizeDisk() >= diskCacheSizeLimit) {
                this.diskCacheSizeEvictedCount++;
                if (this.diskCacheSizeEvictedCount == this.diskCacheSizeEvictedLimit) {
                    Tr.error(tc, "DYNA0065W",
                             new Object[] { new Integer(diskCacheSizeLimit), this.cacheName, Long.valueOf(this.diskCacheSizeEvictedCount) });
                    if (this.diskCacheSizeEvictedLimit == 1) {
                        this.diskCacheSizeEvictedLimit = LOGGING_WINDOW;
                    } else {
                        this.diskCacheSizeEvictedLimit += LOGGING_WINDOW;
                    }
                }
                overLimitType = CacheOnDisk.DISK_CACHE_SIZE_IN_ENTRIES_TYPE;
                discard = true;
                toDiskResult.result = HTODDynacache.DISK_SIZE_IN_ENTRIES_OVER_LIMIT_EXCEPTION;
            }

            long diskCacheEntrySizeInBytesLimit = this.diskCache.getDiskCacheEntrySizeInBytesLimit();
            if (!discard && diskCacheEntrySizeInBytesLimit > 0 && cacheEntry.serializedValue.length >= diskCacheEntrySizeInBytesLimit) {
                this.diskCacheEntrySizeInMBEvictedCount++;
                if (this.diskCacheEntrySizeInMBEvictedCount == this.diskCacheEntrySizeInMBEvictedLimit) {
                    Tr.error(tc, "DYNA0064W", new Object[] { new Long(diskCacheEntrySizeInBytesLimit / DiskCacheSizeInfo.MB_SIZE), this.cacheName,
                                                            new Long(this.diskCacheEntrySizeInMBEvictedCount) });
                    if (this.diskCacheEntrySizeInMBEvictedLimit == 1) {
                        this.diskCacheEntrySizeInMBEvictedLimit = LOGGING_WINDOW;
                    } else {
                        this.diskCacheEntrySizeInMBEvictedLimit += LOGGING_WINDOW;
                    }
                }
                discard = true;
                toDiskResult.result = HTODDynacache.DISK_CACHE_ENTRY_SIZE_OVER_LIMIT_EXCEPTION;
            }
            if (this.enableDiskCacheSizeInBytesChecking && !discard) {
                int dataSize = cacheEntry.serializedId.length;

                if (cacheEntry._templates != null && cacheEntry._templates.length > 0) {
                    dataSize += cacheEntry._templates[0].length();
                }
                if (cacheEntry._serializedDataIds != null) {
                    dataSize += cacheEntry._serializedDataIds.length;
                }
                dataSize += cacheEntry.serializedValue.length;
                long diskCacheSizeInBytesLimit = this.diskCache.getDiskCacheSizeInBytesLimit();
                if (diskCacheSizeInBytesLimit > 0 && (diskCache.getCacheSizeInBytes() + dataSize) >= diskCacheSizeInBytesLimit) {
                    this.diskCacheSizeInGBEvictedCount++;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "lruToDisk(): cacheName=" + this.cacheName + " diskCacheSizeInGBEvictedCount=" + diskCacheSizeInGBEvictedCount
                                     + " diskCache.getCacheSizeInBytes()=" + diskCache.getCacheSizeInBytes() + " dataSize=" + dataSize
                                     + " diskCacheSizeInBytesLimit=" + diskCacheSizeInBytesLimit);
                    }
                    if (this.diskCacheSizeInGBEvictedCount == this.diskCacheSizeInGBEvictedLimit) {
                        Tr.error(tc, "DYNA0063W", new Object[] { new Integer(this.diskCache.getDiskCacheSizeInGBLimit()), this.cacheName,
                                                                new Long(this.diskCacheSizeInGBEvictedCount) });
                        if (this.diskCacheSizeInGBEvictedLimit == 1) {
                            this.diskCacheSizeInGBEvictedLimit = LOGGING_WINDOW;
                        } else {
                            this.diskCacheSizeInGBEvictedLimit += LOGGING_WINDOW;
                        }
                    }
                    overLimitType = CacheOnDisk.DISK_CACHE_SIZE_IN_BYTES_TYPE;
                    discard = true;
                    toDiskResult.result = HTODDynacache.DISK_SIZE_OVER_LIMIT_EXCEPTION;
                }
            }
        }

        // handle the error found from step 1
        if (discard) {
            if (overLimitType > 0 && this.cacheConfig.getDiskCacheEvictionPolicy() != CacheConfig.EVICTION_NONE) {
                this.diskCache.invokeDiskCacheGarbageCollector(overLimitType);
            }
            int cause = CachePerf.LRU;
            if (overLimitType > 0) {
                cause = CachePerf.DISK_OVERFLOW;
            }
            if (cacheEntry.skipMemoryAndWriteToDisk) {
                cacheEntry.skipMemoryAndWriteToDiskErrorCode = toDiskResult.result;
                CachePerf cachePerf = cachePerfRef.get();
                if (cachePerf != null && !cacheEntry.loadedFromDisk && cachePerf.isPMIEnabled())
                    cachePerf.onInvalidate(cacheEntry.getTemplate(), cause, CachePerf.MEMORY, CachePerf.LOCAL);
                cacheStatisticsListener.remove(cacheEntry.id, cause, CachePerf.MEMORY, CachePerf.LOCAL);
            } else {
                internalInvalidateById(cacheEntry.id, cause, CachePerf.LOCAL, FIRE_INVALIDATION_LISTENER); // CAC-O
                invalidateById(cacheEntry.id, cause, CachePerf.LOCAL, false, !InvalidateByIdEvent.INVOKE_INTERNAL_INVALIDATE_BY_ID);
            }

            if (tc.isEntryEnabled())
                Tr.exit(tc, "lruToDisk.2");

            return toDiskResult;
        }

        if (!cacheEntry.skipMemoryAndWriteToDisk && this.cacheConfig.diskCachePerformanceLevel != CacheConfig.HIGH
            && (cacheEntry.timeLimit > 0 || cacheEntry.inactivity > 0)) {
            timeLimitDaemon.valueWasRemoved(this, cacheEntry.id);
        }

        // Skip step 2 & 3 when loadFromDisk is true because the cache entry
        // exists on disk cache
        if (cacheEntry.loadedFromDisk) {
            removeInvalidationInfo(cacheEntry);
        } else {

            invokeGCIfNecessary();

            //
            // **** Step 2
            // write the cache entry to the disk cache
            //
            toDiskResult.result = diskCache.writeCacheEntry(cacheEntry);
            if (toDiskResult.result == HTODDynacache.NO_EXCEPTION_ENTRY_OVERWRITTEN) {
                // set returnBoolean to true
                // error code to HTODDynacache.NO_EXCEPTION so that the caller
                // does not need to handle overwritten case
                toDiskResult.entryOverwritten = ENTRY_OVERWRITTEN_ON_DISK;
                toDiskResult.result = HTODDynacache.NO_EXCEPTION;
            }
            //
            // handle the error found from step 2
            if (toDiskResult.result != HTODDynacache.NO_EXCEPTION) { // 311376
                int cause = CachePerf.LRU;
                if (toDiskResult.result == HTODDynacache.DISK_SIZE_OVER_LIMIT_EXCEPTION) {
                    cause = CachePerf.DISK_OVERFLOW;
                }
                if (cacheEntry.skipMemoryAndWriteToDisk) {
                    // skipMemoryAndWriteToDisk = true
                    // update the PMI and statistics. No need to remove the
                    // cache id from memory cache
                    cacheEntry.skipMemoryAndWriteToDiskErrorCode = toDiskResult.result;
                    CachePerf cachePerf = cachePerfRef.get();
                    if (cachePerf != null && !cacheEntry.loadedFromDisk && cachePerf.isPMIEnabled())
                        cachePerf.onInvalidate(cacheEntry.getTemplate(), cause, CachePerf.MEMORY, CachePerf.LOCAL);
                    cacheStatisticsListener.remove(cacheEntry.id, cause, CachePerf.MEMORY, CachePerf.LOCAL);
                } else {
                    // skipMemoryAndWriteToDisk = false
                    // call invalidate to remove cache entry from memory cache
                    internalInvalidateById(cacheEntry.id, cause, CachePerf.LOCAL, FIRE_INVALIDATION_LISTENER);
                    invalidateById(cacheEntry.id, cause, CachePerf.LOCAL, false, !InvalidateByIdEvent.INVOKE_INTERNAL_INVALIDATE_BY_ID);
                }
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "lruToDisk.3");

                return toDiskResult;
            }
            //
            // **** Step 3
            // write the dependency id and template to the disk cache
            //
            if (!this.cacheConfig.disableDependencyId) {
                for (int i = 0; i < cacheEntry._dataIds.length; i++) {
                    if (toDiskResult.result == HTODDynacache.NO_EXCEPTION) {
                        toDiskResult.result = diskCache.writeDependencyEntry(cacheEntry._dataIds[i], cacheEntry.id);
                        if (toDiskResult.result == HTODDynacache.NO_EXCEPTION && !cacheEntry.skipMemoryAndWriteToDisk) {
                            dataDependencyTable.removeEntry(cacheEntry._dataIds[i], cacheEntry.id);
                        }
                    }
                }
            }
            if (!this.cacheConfig.disableTemplatesSupport) {
                for (int i = 0; i < cacheEntry._templates.length; i++) {
                    if (toDiskResult.result == HTODDynacache.NO_EXCEPTION) {
                        toDiskResult.result = diskCache.writeTemplateEntry(cacheEntry._templates[i], cacheEntry.id);
                        if (toDiskResult.result == HTODDynacache.NO_EXCEPTION && !cacheEntry.skipMemoryAndWriteToDisk) {
                            templateDependencyTable.removeEntry(cacheEntry._templates[i], cacheEntry.id);
                        }
                    }
                }
            }
            //
            // handle the error found from step 3
            if (toDiskResult.result != HTODDynacache.NO_EXCEPTION) {
                int cause = CachePerf.LRU;
                if (toDiskResult.result == HTODDynacache.DISK_SIZE_OVER_LIMIT_EXCEPTION) {
                    cause = CachePerf.DISK_OVERFLOW;
                }
                // if exception is not disk exception, add the cache id in the
                // explict invalidation buffer. This will
                // remove the cache entry as well as assoicated dependency id or
                // template. No fire event or
                // update PMI or statistics during cache entry removal
                if (toDiskResult.result != HTODDynacache.DISK_EXCEPTION) {
                    ValueSet valueSet = new ValueSet(1);
                    valueSet.add(cacheEntry.id);
                    diskCache.delCacheEntry(valueSet, cause, CachePerf.LOCAL, !FROM_DEPID_TEMPLATE_INVALIDATION, !HTODInvalidationBuffer.FIRE_EVENT);
                }
                if (cacheEntry.skipMemoryAndWriteToDisk) {
                    // skipMemoryAndWriteToDisk = true
                    // update the PMI and statistics. No need to remove the
                    // cache id from memory cache
                    cacheEntry.skipMemoryAndWriteToDiskErrorCode = toDiskResult.result;
                    CachePerf cachePerf = cachePerfRef.get();
                    if (cachePerf != null && !cacheEntry.loadedFromDisk && cachePerf.isPMIEnabled())
                        cachePerf.onInvalidate(cacheEntry.getTemplate(), cause, CachePerf.MEMORY, CachePerf.LOCAL);
                    cacheStatisticsListener.remove(cacheEntry.id, cause, CachePerf.MEMORY, CachePerf.LOCAL);
                } else {
                    // skipMemoryAndWriteToDisk = false
                    // call invalidate to remove cache entry from the memory
                    internalInvalidateById(cacheEntry.id, cause, CachePerf.LOCAL, FIRE_INVALIDATION_LISTENER);
                    invalidateById(cacheEntry.id, cause, CachePerf.LOCAL, false, !InvalidateByIdEvent.INVOKE_INTERNAL_INVALIDATE_BY_ID);
                }
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "lruToDisk.4");

                return toDiskResult;
            }
        }

        //
        // step 4
        // remove the cache entry and aliases from entryhashtable if it
        // skipMemoryAndWriteToDisk is false
        if (!cacheEntry.skipMemoryAndWriteToDisk) {
            entryHashtable.remove(cacheEntry.id);
            decreaseCacheSizeInBytes(cacheEntry);
            for (int i = 0; i < cacheEntry.aliasList.length; i++) {
                entryHashtable.remove(cacheEntry.aliasList[i]);
                decreaseCacheSizeInBytes(ObjectSizer.FASTHASHTABLE_PER_ENTRY_OVERHEAD + ObjectSizer.OBJECT_REF_SIZE, "ALIAS");
            }
        }

        //
        // step 5
        // if cache id is also the dependency id, write the dependency to the
        // disk
        //
        if (!this.cacheConfig.disableDependencyId) {
            ValueSet valueSet = dataDependencyTable.removeDependency(cacheEntry.id);
            if (valueSet != null && !cacheEntry.loadedFromDisk) {
                ValueSet valueSetClone = (ValueSet) valueSet.clone();
                toDiskResult.result = diskCache.writeDependency(cacheEntry.id, valueSetClone);
            }
            //
            // handle the error found from step 5
            //
            if (toDiskResult.result != HTODDynacache.NO_EXCEPTION) {
                dataDependencyTable.add(cacheEntry.id, valueSet);
                int cause = CachePerf.LRU;
                if (toDiskResult.result == HTODDynacache.DISK_SIZE_OVER_LIMIT_EXCEPTION) {
                    cause = CachePerf.DISK_OVERFLOW;
                }
                // if exception is not disk exception, add the cache id in the
                // explict invalidation buffer. This will
                // remove the cache entry as well as assoicated dependency id or
                // template. No fire event or update
                // pmi or statistics during the cache entry removal.
                if (toDiskResult.result != HTODDynacache.DISK_EXCEPTION) {
                    ValueSet valueSet1 = new ValueSet(1);
                    valueSet1.add(cacheEntry.id);
                    diskCache.delCacheEntry(valueSet1, cause, CachePerf.LOCAL, !FROM_DEPID_TEMPLATE_INVALIDATION, !HTODInvalidationBuffer.FIRE_EVENT);
                }
                // call invalidate to remove cache ids associated with
                // dependency id
                internalInvalidateById(cacheEntry.id, cause, CachePerf.LOCAL, FIRE_INVALIDATION_LISTENER);
                invalidateById(cacheEntry.id, cause, CachePerf.LOCAL, false, !InvalidateByIdEvent.INVOKE_INTERNAL_INVALIDATE_BY_ID);
                if (cacheEntry.skipMemoryAndWriteToDisk) {
                    // update pmi and statistics
                    cacheEntry.skipMemoryAndWriteToDiskErrorCode = toDiskResult.result;
                    CachePerf cachePerf = cachePerfRef.get();
                    if (cachePerf != null && !cacheEntry.loadedFromDisk && cachePerf.isPMIEnabled())
                        cachePerf.onInvalidate(cacheEntry.getTemplate(), cause, CachePerf.MEMORY, CachePerf.LOCAL);
                    cacheStatisticsListener.remove(cacheEntry.id, cause, CachePerf.MEMORY, CachePerf.LOCAL);
                } else {
                    // return the cache entry to the pool
                    cacheEntry.returnToPool();
                }
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "lruToDisk.5");

                return toDiskResult;
            }
        }
        //
        // LRU to disk operation is successful
        //
        if (cacheEntry.skipMemoryAndWriteToDisk) {
            if (this.cacheConfig.diskCachePerformanceLevel == CacheConfig.HIGH && (cacheEntry.timeLimit > 0 || cacheEntry.inactivity > 0)) {
                // skipMemoryAndWriteToDisk = true and high performance with TTL
                // notify the timeLimitDaemon to indicate the value has changed
                timeLimitDaemon.valueHasChanged(this, cacheEntry.id, cacheEntry.expirationTime, cacheEntry.inactivity);
            }
        } else {
            // update pmi and statistics
            CachePerf cachePerf = cachePerfRef.get();
            if (cachePerf != null && !cacheEntry.loadedFromDisk && cachePerf.isPMIEnabled())
                cachePerf.onInvalidate(cacheEntry.getTemplate(), CachePerf.LRU, CachePerf.DISK, CachePerf.LOCAL);
            cacheStatisticsListener.remove(cacheEntry.id, CachePerf.LRU, CachePerf.DISK, CachePerf.LOCAL);

            cacheEntry.returnToPool();
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "lruToDisk");

        return toDiskResult;
    }

    private void invokeGCIfNecessary() {

        if (tc.isDebugEnabled()) {
            Tr.entry(tc, "invokeGCIfNecessary SIZE LIMIT " + diskCache.getDiskCacheSizeHighLimit() + " ACTUAL " + getIdsSizeDisk() + "BYTES LIMIT "
                         + diskCache.getDiskCacheSizeInBytesHighLimit() + " ACTUAL " + diskCache.getCacheSizeInBytes());
        }

        boolean invokeGC = false;
        boolean invokedGC = false;
        if (this.cacheConfig.getDiskCacheEvictionPolicy() != CacheConfig.EVICTION_NONE) {
            invokeGC = false;
            if (this.diskCache.getDiskCacheSizeLimit() > 0) {
                if (getIdsSizeDisk() > this.diskCache.getDiskCacheSizeHighLimit()) {
                    invokeGC = true;
                    invokedGC = diskCache.invokeDiskCacheGarbageCollector(CacheOnDisk.DISK_CACHE_SIZE_IN_ENTRIES_TYPE);
                }
            }
            if (!invokeGC && this.diskCache.getDiskCacheSizeInBytesLimit() > 0) {
                if (this.diskCache.getCacheSizeInBytes() > this.diskCache.getDiskCacheSizeInBytesHighLimit()) {
                    invokeGC = true;
                    invokedGC = diskCache.invokeDiskCacheGarbageCollector(CacheOnDisk.DISK_CACHE_SIZE_IN_BYTES_TYPE);
                }
            }
        }

        if (tc.isDebugEnabled()) {
            Tr.exit(tc, "invokeGCIfNecessary " + invokeGC + " invokedGC " + invokedGC);
        }

    }

    /**
     * This method will be invoked when the CacheServiceImpl DS that represents this cache is stopped
     */
    @Override
    public synchronized void stop() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, " Stopping cache: " + cacheName);
        if (swapToDisk && !flushToDiskComplete) {
            flushToDisk();
        }
        CachePerf cachePerf = cachePerfRef.get();
        if (cachePerf != null) {
            cachePerf.removePMICounters();
        }
    }

    private void flushToDisk() {
        boolean flushToDisk = this.cacheConfig.flushToDiskOnStop;
        if (cacheName != null) {
            if (cacheName.equals(DCacheBase.DEFAULT_CACHE_NAME)) {
                String systemProperty = System.getProperty("com.ibm.ws.cache.flushToDiskOnStop");
                if (systemProperty != null && systemProperty.equalsIgnoreCase("true")) {
                    flushToDisk = true;
                }
            }
            if (flushToDisk) {
                Tr.info(tc, "DYNA0060I", new Object[] { cacheName });
                swapToDisk = false;
                diskCache.stop(!HTODDynacache.COMPLETE_CLEAR);
                Enumeration e = entryHashtable.elements();
                // int size = entryHashtable.size();
                long timeoutAdjustment = 2 * 60 * 1000; // 2 min (time for stop and restart server)
                // int i = 0;
                int numOffload = 0;
                long lastWriteObjectSize = cacheStatisticsListener.getObjectsWriteToDiskSizeCount();
                long time1 = System.nanoTime();
                while (e.hasMoreElements()) {
                    CacheEntry ce = (CacheEntry) e.nextElement();
                    if (ce.persistToDisk) {
                        if (ce.timeLimit <= 0 || (ce.timeLimit > 0 && (System.currentTimeMillis() + timeoutAdjustment) < ce.expirationTime)) {
                            numOffload++;
                            lruToDisk(ce);
                        }
                    }
                    // i++;
                    // if (size > 10000 && i % 1000 == 0) {
                    // System.out.println("Cache.flushToDisk() current offload entry=" + i);
                    // }
                }
                long time2 = System.nanoTime();
                if (diskCache.writeAuxiliaryDepTables() == HTODDynacache.DISK_EXCEPTION)
                    return;
                StringBuffer message = new StringBuffer();
                CacheOnDisk cacheOnDisk = (CacheOnDisk) diskCache;
                message.append(" numOfEntriesFlushToDisk=");
                message.append(numOffload);
                message.append(" numOfBytesFlushToDisk=");
                message.append(cacheStatisticsListener.getObjectsWriteToDiskSizeCount() - lastWriteObjectSize);
                message.append(" timeElapsedEntriesFlushToDisk=");
                message.append(TimeUnit.NANOSECONDS.toMillis(time2 - time1));
                message.append(" numDepIdsInAuxTable=");
                message.append(cacheOnDisk.htod.numDepIdsInAuxTable);
                message.append(" numCacheIdsInDepIdAuxTable=");
                message.append(cacheOnDisk.htod.numCacheIdsInDepIdAuxTable);
                message.append(" numTemplatesInAuxTable=");
                message.append(cacheOnDisk.htod.numTemplatesInAuxTable);
                message.append(" numCacheIdsInTemplateAuxTable=");
                message.append(cacheOnDisk.htod.numCacheIdsInTemplateAuxTable);
                message.append(" timeElapsedWriteAuxTables=");
                message.append(cacheOnDisk.htod.timeElapsedWriteAuxTables);
                int numExplicitBufferFlushToDisk = cacheOnDisk.htod.numExplicitBufferLimitOnStop;
                if (numExplicitBufferFlushToDisk > 0) {
                    message.append(" numExplicitBufferFlushToDisk=");
                    message.append(numExplicitBufferFlushToDisk);
                    message.append(" explicitBufferLimitOnStop=");
                    message.append(cacheOnDisk.explicitBufferLimitOnStop);
                }
                Tr.info(tc, "DYNA0073I", new Object[] { cacheName, message.toString() });
                diskCache.close(CacheOnDisk.DELETE_IN_PROGRESS_FILE);
            } else {
                Tr.info(tc, "DYNA0061I", new Object[] { cacheName });
                swapToDisk = false;
                diskCache.stop(HTODDynacache.COMPLETE_CLEAR);
                diskCache.close(CacheOnDisk.DELETE_IN_PROGRESS_FILE);
                diskCache.deleteDiskCacheFiles();
            }
            flushToDiskComplete = true;
        }
    }

    /**
     * This gets the current number of cache entries for this cache instance.
     * 
     * @return The current number of cache entries.
     */
    @Override
    public synchronized int getNumberCacheEntries() {
        return entryHashtable.size();
    }

    /**
     * This method is the unsynchronized version of getNumberCacheEntries and NEEDS to be calle in ALL situations where
     * a close approxiamation of the no. of entries in the cache is required. Goes without saying this method is more
     * performant and eliminates deadlock possibilities.
     */
    @Override
    @Trivial
    public int getNumberCacheEntriesUnsynchronized() {
        return entryHashtable.size();
    }

    /**
     * Return to indicate the entry can be pulled from other remote caches which caching this value.
     * 
     * @param share
     *            sharing policy
     * @id cache ID
     */
    @Override
    public boolean shouldPull(int share, Object id) {
        return remoteServices.shouldPull(share, id);
    }

    @Override
    public void setSwapToDisk(boolean enable) {
        this.swapToDisk = enable;
        this.cacheConfig.enableDiskOffload = enable;
    }

    @Override
    public com.ibm.websphere.cache.CacheEntry getEntryFromMemory(Object id) {
        return (com.ibm.websphere.cache.CacheEntry) entryHashtable.get(id);
    }

    /**
     * Adds an alias for the given key in the cache's mapping table. If the alias is already associated with another
     * key, it will be changed to associate with the new key.
     * 
     * @param key
     *            the key assoicated with alias
     * @param aliasArray
     *            the alias to use for lookups
     * @param askPermission
     *            True implies that execution must ask the coordinating CacheUnit for permission.
     * @param coordinate
     *            Indicates that the value should be set in other caches caching this value.
     */
    @Override
    public void addAlias(Object key, Object[] aliasArray, boolean askPermission, boolean coordinate) {
        // _syncAddAlias() will inc the reff count.
        CacheEntry cacheEntry = _syncAddAlias(key, aliasArray, askPermission);

        if (cacheEntry == null) {
            throw new IllegalArgumentException("The cache id is not found in the cache's mapping table when adding alias");
        }
        cacheEntry.finish();

        if (cacheEntry.sharingPolicy != EntryInfo.NOT_SHARED && coordinate) {
            AliasEntry aliasEntry = new AliasEntry(key, AliasEntry.ADD_ALIAS, cacheEntry.sharingPolicy, aliasArray);
            batchUpdateDaemon.pushAliasEntry(aliasEntry, this);
        }
        return;
    }

    // Caller must call ce.finish()
    private synchronized CacheEntry _syncAddAlias(Object key, Object[] aliasArray, boolean askPermission) {
        CacheEntry cacheEntry = getCacheEntry(key, askPermission, IGNORE_COUNTING, null, INCREMENT_REFF_COUNT);

        if (cacheEntry == null) {
            return null;
        }
        if (cacheEntry.loadedFromDisk) {
            diskCache.delCacheEntry(cacheEntry, CachePerf.DIRECT, CachePerf.LOCAL, !FROM_DEPID_TEMPLATE_INVALIDATION);
            cacheEntry.loadedFromDisk = false;
        }
        if (cacheEntry.getExpirationTime() > 0 && cacheEntry.getExpirationTime() < System.currentTimeMillis()) { // entry
                                                                                                                 // expired?
            cacheEntry.finish();
            return null;
        }
        for (int i = 0; i < aliasArray.length; i++) {
            if (aliasArray[i] != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    Tr.debug(tc, "_syncAddAlias() cacheName=" + this.cacheName + " adding alias to EHT key=" + key + " alias=" + aliasArray[i]);

                CacheEntry oldAlias = (CacheEntry) entryHashtable.get(aliasArray[i]);
                if (oldAlias != null) {
                    removeAlias(aliasArray[i], askPermission, false);
                }

                cacheEntry.addAlias(aliasArray[i]);
                entryHashtable.put(aliasArray[i], cacheEntry);
                increaseCacheSizeInBytes(ObjectSizer.FASTHASHTABLE_PER_ENTRY_OVERHEAD + ObjectSizer.OBJECT_REF_SIZE, "ALIAS");
            }
        }
        return cacheEntry;
    }

    /**
     * Removes an alias from the cache mapping.
     * 
     * @param alias
     *            the alias assoicated with cache id
     * @param askPermission
     *            True implies that execution must ask the coordinating CacheUnit for permission (No effect on
     *            CoreCache).
     * @param coordinate
     *            Indicates that the value should be set in other caches caching this value. (No effect on CoreCache)
     */
    @Override
    public void removeAlias(Object alias, boolean askPermission, boolean coordinate) {
        // _syncRemoveAlias() will inc the reff count.
        CacheEntry cacheEntry = _syncRemoveAlias(alias, askPermission);
        if (cacheEntry == null) {
            throw new IllegalArgumentException("The alias is not found in the cache's mapping table when removing alias");
        }
        cacheEntry.finish();
        if (cacheEntry.sharingPolicy == EntryInfo.NOT_SHARED) {
            return;
        }
        if (coordinate) {
            AliasEntry aliasEntry = new AliasEntry(cacheEntry.getIdObject(), AliasEntry.REMOVE_ALIAS, cacheEntry.sharingPolicy,
                            CacheEntry.EMPTY_OBJECT_ARRAY);
            aliasEntry.addAlias(alias);
            batchUpdateDaemon.pushAliasEntry(aliasEntry, this);
        }
        return;
    }

    // Caller must call ce.finish()
    private synchronized CacheEntry _syncRemoveAlias(Object alias, boolean askPermission) { // CCC
        CacheEntry cacheEntry = getCacheEntry(alias, askPermission, IGNORE_COUNTING, null, INCREMENT_REFF_COUNT);
        if (cacheEntry != null) {
            if (cacheEntry.loadedFromDisk) {
                diskCache.delCacheEntry(cacheEntry, CachePerf.DIRECT, CachePerf.LOCAL, !FROM_DEPID_TEMPLATE_INVALIDATION);
                cacheEntry.loadedFromDisk = false;
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "_syncRemoveAlias() cacheName=" + this.cacheName + " removing alias from EHT " + alias);
            cacheEntry.removeAlias(alias);
            entryHashtable.remove(alias);
            decreaseCacheSizeInBytes(ObjectSizer.FASTHASHTABLE_PER_ENTRY_OVERHEAD + ObjectSizer.OBJECT_REF_SIZE, "ALIAS");
        }
        return cacheEntry;
    }

    /**
     * HTOD - getIdsByRangeDisk() used by DMap, CacheMonitor and debugging WARNING: If index = 1 or -1, the set might
     * contain "DISKCACHE_MORE" to indicate there are more cache ids on the disk cache. The caller need to remove
     * DISKCACHE_MORE" from the set before it is being used. The "DISKCACHE_MORE" key is defined as
     * HTODDynacache.DISKCACHE_MORE.
     * 
     * @param index
     *            If index = 0, it starts the beginning. If index = 1, it means "next". If Index = -1, it means
     *            "previous".
     * @param length
     *            The max number of cache ids to be read. If length = -1, it reads all cache ids until the end.
     * @return The Set of cache ids.
     */
    @Override
    public synchronized Set getIdsByRangeDisk(int index, int length) {
        Set ids = null;
        if (swapToDisk) {
            ids = diskCache.readCacheIdsByRange(index, length);
        }
        if (ids == null) {
            ids = new ValueSet(0);
        }
        return ids;
    }

    /**
     * Returns a set of dependency IDs based on the range and size. WARNING: If index = 1 or -1, the set might contain
     * "DISKCACHE_MORE" to indicate there are more dep ids on the disk cache. The caller need to remove DISKCACHE_MORE"
     * from the set before it is being used. The "DISKCACHE_MORE" key is defined as HTODDynacache.DISKCACHE_MORE.
     * 
     * @param index
     *            If index = 0, it starts the beginning. If index = 1, it means "next". If Index = -1, it means
     *            "previous".
     * @param length
     *            The max number of dependency ids to be read. If length = -1, it reads all dependency ids until the
     *            end.
     * @return The Set of dependency ids.
     */
    @Override
    public synchronized Set getDepIdsByRangeDisk(int index, int length) {
        Set depids = null;
        if (swapToDisk) {
            depids = diskCache.readDependencyByRange(index, length);
        }
        if (depids == null) {
            depids = new ValueSet(0);
        }
        return depids;
    }

    /**
     * Returns a set of templates based on the range and size. WARNING: If index = 1 or -1, the set might contain
     * "DISKCACHE_MORE" to indicate there are more templates on the disk cache. The caller need to remove
     * DISKCACHE_MORE" from the set before it is being used. The "DISKCACHE_MORE" key is defined as
     * HTODDynacache.DISKCACHE_MORE.
     * 
     * @param index
     *            If index = 0, it starts the beginning. If index = 1, it means "next". If Index = -1, it means
     *            "previous".
     * @param length
     *            The max number of templates to be read. If length = -1, it reads all templates until the end.
     * @return The Set of templates.
     */
    @Override
    public synchronized Set getTemplatesByRangeDisk(int index, int length) {
        Set templates = null;
        if (swapToDisk) {
            templates = diskCache.readTemplatesByRange(index, length);
        }
        if (templates == null) {
            templates = new ValueSet(0);
        }
        return templates;
    }

    /**
     * HTOD - getEntryDisk() used by CacheMonitor or debugging
     * 
     * @param cacheId
     *            The cache id
     * @return The CacheEntry.
     */
    @Override
    public synchronized com.ibm.websphere.cache.CacheEntry getEntryDisk(Object cacheId) {
        com.ibm.websphere.cache.CacheEntry ce = null;
        if (swapToDisk) {
            ce = diskCache.readCacheEntry(cacheId);
        }
        return ce;
    }

    /**
     * Returns the cache IDs that are associated with a dependency ID from the disk cache. It returns an empty set if no
     * cache ID is associated with it.
     * <p>
     * <br>
     * Dependency elements are used to group related cache items. Cache entries having the same depenendency ID are
     * managed as a group. Each related cache item shares the same dependency id, so it only takes one member of the
     * dependency group to get invalidated, for the rest of the group to be evicted. The dependency ID can be as simple
     * as just a name such as storeId
     * 
     * @param depId
     *            dependency ID for the groupof cache IDs.
     * @return A set of cache IDs or empty set if no cache ID is associated with it.
     */
    @Override
    public synchronized Set getCacheIdsByDependencyDisk(Object depId) {
        Set ids = null;
        if (swapToDisk) {
            ids = diskCache.readDependency(depId, !HTODDynacache.DELETE);
        }
        if (ids == null) {
            ids = new ValueSet(0);
        }
        return ids;
    }

    /**
     * Returns the cache IDs that are associated with a template from the disk cache. It returns an empty set if no
     * cache ID is associated with it.
     * 
     * @param template
     *            template for the groupof cache IDs.
     * @return A set of cache IDs or empty set if no cache ID is associated with it.
     */
    @Override
    public synchronized Set getCacheIdsByTemplateDisk(String template) {
        Set ids = null;
        if (swapToDisk) {
            ids = diskCache.readTemplate(template, !HTODDynacache.DELETE);
        }
        if (ids == null) {
            ids = new ValueSet(0);
        }
        return ids;
    }

    /**
     * HTOD - getIdsSizeDisk() used by CacheMonitor diskStatistics.jsp
     * 
     * @return The cache ids size for the disk.
     */
    @Override
    public int getIdsSizeDisk() {
        if (swapToDisk) {
            int size = diskCache.getCacheIdsSize(CacheOnDisk.FILTER);
            return size > 0 ? size : 0;
        }
        return 0;
    }

    /**
     * Returns the actual number of entries from disk cache without filtering.
     * 
     * @return The actual number of entries from the disk cache(no filtering).
     */
    @Override
    public int getActualIdsSizeDisk() {
        if (swapToDisk) {
            return diskCache.getCacheIdsSize(!CacheOnDisk.FILTER);
        }
        return 0;
    }

    /**
     * Returns the current dependency IDs size for the disk cache.
     * 
     * @return The current dependency ids size for the disk cache.
     */
    @Override
    public int getDepIdsSizeDisk() {
        if (swapToDisk) {
            return diskCache.getDepIdsSize();
        }
        return 0;
    }

    /**
     * HTOD - getTemplatesSizeDisk()
     * 
     * @return The current templates size for the disk.
     */
    @Override
    public int getTemplatesSizeDisk() {
        if (swapToDisk) {
            return diskCache.getTemplatesSize();
        }
        return 0;
    }

    /**
     * HTOD - getPendingRemovalSizeDisk()
     * 
     * @return The current pending removal size in the disk invalidation buffers.
     */
    @Override
    public int getPendingRemovalSizeDisk() {
        if (swapToDisk) {
            return diskCache.getPendingRemovalSize();
        }
        return 0;
    }

    /**
     * Returns the size of dependency id auxiliary table for disk cache.
     * 
     * @return the size of dependency id auxiliary table for disk cache
     */
    @Override
    public int getDepIdsBufferedSizeDisk() {
        if (swapToDisk) {
            return diskCache.getDepIdsBufferedSize();
        }
        return 0;
    }

    /**
     * Returns the size of template auxiliary table for disk cache.
     * 
     * @return the size of template auxiliary table for disk cache
     */
    @Override
    public int getTemplatesBufferedSizeDisk() {
        if (swapToDisk) {
            return diskCache.getTemplatesBufferedSize();
        }
        return 0;
    }

    /**
     * @return the current cache size in bytes for the disk.
     */
    @Override
    public long getCacheSizeInBytesDisk() {
        if (swapToDisk) {
            return diskCache.getCacheSizeInBytes();
        }
        return 0;
    }

    /**
     * This method clears everything from the disk cache. This is called by CacheMonitor
     */
    @Override
    public synchronized void clearDisk() {
        if (swapToDisk) {
            if (bEnableListener && eventSource.getPreInvalidationListenerCount() > 0) {
                if (eventSource.shouldInvalidate("*", InvalidationEvent.LOCAL, InvalidationEvent.CLEAR_ALL) == false) {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                        Tr.debug(tc, "clearDisk() cacheName=" + this.cacheName
                                     + " skip clearing cache because PreInvalidationListener.shouldInvalidate() returns false.");
                    }
                    return;
                }
            }
            diskCache.clearDiskCache();
            CachePerf cachePerf = cachePerfRef.get();
            if (cachePerf != null && cachePerf.isPMIEnabled()) {
                cachePerf.onCacheClear(false, true);
            }
        }
    }

    /**
     * HTOD - invokeDiskCleanup
     */
    @Override
    @Trivial
    public void invokeDiskCleanup(boolean scan) {
        if (swapToDisk) {
            diskCache.invokeDiskCleanup(scan);
        }
    }

    /**
     * HTOD - isDiskCleanupRunning
     */
    @Override
    public boolean isDiskCleanupRunning() {
        if (swapToDisk) {
            return diskCache.isCleanupRunning();
        }
        return false;
    }

    /**
     * HTOD - isDiskInvalidationBufferFull
     */
    @Override
    @Trivial
    public boolean isDiskInvalidationBufferFull() {
        if (swapToDisk) {
            return diskCache.isInvalidationBuffersFull();
        }
        return false;
    }

    /**
     * Returns the exception object from the disk cache because disk cache reported the error.
     * 
     * @return The exception object
     */
    @Override
    public Exception getDiskCacheException() {
        return this.diskCache.getDiskCacheException();
    }

    /**
     * HTOD - getDiskCacheSizeInMBs() used by CacheMonitor diskStatistics.jsp
     * 
     * @return the current cache size in MBs for the disk.
     */
    @Override
    public float getDiskCacheSizeInMBs() {
        if (swapToDisk) {
            float size = ((float) diskCache.getCacheSizeInBytes()) / (float) DiskCacheSizeInfo.MB_SIZE;
            return size > 0 ? size : 0;
        }
        return 0;
    }

    /**
     * HTOD - releaseDiskCacheUnusedPools - release unused pools in the disk cache
     */
    @Override
    public void releaseDiskCacheUnusedPools() {
        if (swapToDisk && diskCache != null) {
            diskCache.releaseUnusedPools();
        }
    }

    /**
     * Returns number of the cache IDs in the PushPullTable.
     */
    @Override
    public int getPushPullTableSize() {
        return this.remoteServices.getPushPullTableSize();
    }

    /**
     * Returns all of the cache IDs in the PushPullTable
     */
    @Override
    public List getCacheIdsInPushPullTable() {
        return this.remoteServices.getCacheIdsInPushPullTable();
    }

    /**
     * This is a used by disk cleanup - scan the entries which has expiration time but have not expired yet and add to
     * the TimeLimitDaemon heap when diskCachePerformanceLevel is high
     */
    @Override
    public void addToTimeLimitDaemon(Object id, long expirationTime, int inactivity) {
        this.timeLimitDaemon.valueHasChanged(this, id, expirationTime, inactivity);
    }

    // ----------------------------------------------------
    /**
     * This method is called whenever someone needs to have the performance monitor refershed. Currently we only update
     * cache memory and disk sizes. Add cache instance type specific updates in this method.
     */
    @Override
    @Trivial
    public void refreshCachePerf() {
        CachePerf cachePerf = cachePerfRef.get();
        if (cachePerf != null && cachePerf.isPMIEnabled()) {
            cachePerf.updateCacheSizes(cacheConfig.cacheSize, entryHashtable.size());
            if (swapToDisk && (System.currentTimeMillis() - this.lastTimeForStatistics) > LOGGING_WINDOW) {
                cachePerf.updateDiskCacheStatistics(getActualIdsSizeDisk(), getPendingRemovalSizeDisk(), getDepIdsSizeDisk(),
                                                    getDepIdsBufferedSizeDisk(), cacheStatisticsListener.getDepIdsOffloadedToDiskCount(),
                                                    cacheStatisticsListener.getDepIdBasedInvalidationsFromDiskCount(), getTemplatesSizeDisk(), getTemplatesBufferedSizeDisk(),
                                                    cacheStatisticsListener.getTemplatesOffloadedToDiskCount(),
                                                    cacheStatisticsListener.getTemplateBasedInvalidationsFromDiskCount());
            }

        }
    }

    // ----------------------------------------------------
    // Helper
    // ----------------------------------------------------
    private void updatePeerCaches(CacheEntry cacheEntry) {
        final String methodName = "updatePeerCaches()";

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " Entry cacheName=" + this.cacheName + " isDRSReady=" + remoteServices.isDRSReady() + " id=" + cacheEntry.id
                         + " refCount=" + cacheEntry.getRefCount());
        }
        if (remoteServices.isDRSReady() && !this.cacheConfig.isDrsDisabled()) {
            this.batchUpdateDaemon.pushCacheEntry(cacheEntry, this);
            // refCount will be decremented in batchUpdateDaemon.wakeUp()
        } else {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " DRS is not ready!! - CE will be sent during bootstrap CE=" + cacheEntry);
            }
            cacheEntry.finish();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " Exit cacheName=" + this.cacheName + " id=" + cacheEntry.id + " refCount=" + cacheEntry.getRefCount());
        }
    }

    // ----------------------------------------------------

    @Override
    public synchronized void setEnableDiskCacheSizeInBytesChecking(boolean enableDiskCacheSizeInBytesChecking) {
        this.enableDiskCacheSizeInBytesChecking = enableDiskCacheSizeInBytesChecking;
    }

    /**
     * Increase cache entry size in bytes to the total count if (1) cache size in MB enabled (2) cache entry's getSize()
     * returns valid size.
     * 
     * @param cacheEntry
     *            cache entry
     */
    private void increaseCacheSizeInBytes(CacheEntry cacheEntry) {
        if (this.memoryCacheSizeInMBEnabled) {
            long size = cacheEntry.getObjectSize();
            if (size != -1) {
                size += ObjectSizer.FASTHASHTABLE_PER_ENTRY_OVERHEAD;
                this.currentMemoryCacheSizeInBytes += size;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "increaseCacheSizeInBytes() cacheName=" + cacheName + " id=" + cacheEntry.id + " size=" + size
                                 + " currentMemoryCacheSizeInBytes=" + this.currentMemoryCacheSizeInBytes);
                }
            } else {
                disableCacheSizeInMB();
            }
        }
    }

    /**
     * Decrease cache entry size in bytes to the total count if (1) cache size in MB enabled (2) cache entry's getSize()
     * returns valid size.
     * 
     * @param cacheEntry
     *            cache entry
     */
    private void decreaseCacheSizeInBytes(CacheEntry cacheEntry) {
        if (this.memoryCacheSizeInMBEnabled) {
            long size = cacheEntry.getObjectSize();
            if (size != -1) {
                size += ObjectSizer.FASTHASHTABLE_PER_ENTRY_OVERHEAD;
                this.currentMemoryCacheSizeInBytes -= size;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "decreaseCacheSizeInBytes() cacheName=" + cacheName + " id=" + cacheEntry.id + " size=" + size
                                 + " currentMemoryCacheSizeInBytes=" + this.currentMemoryCacheSizeInBytes);
                }
            } else {
                disableCacheSizeInMB();
            }
        }
    }

    public void pinLocal(Object key) {

        if (tc.isDebugEnabled()) {
            Tr.entry(tc, "pinLocal", key);
        }

        com.ibm.websphere.cache.CacheEntry ce = getEntry(key, CachePerf.LOCAL, true, true);

        if (tc.isDebugEnabled()) {
            Tr.exit(tc, "pinLocal", ce);
        }

    }

    public void unpinLocal(Object key) {

        if (tc.isDebugEnabled()) {
            Tr.entry(tc, "unpinLocal", key);
        }

        com.ibm.websphere.cache.CacheEntry ce = getEntry(key, CachePerf.LOCAL, true, false);
        if (null != ce) {
            ce.finish();
        }

        if (tc.isDebugEnabled()) {
            Tr.exit(tc, "unpinLocal", key);
        }
    }

    public Map<Object, String> getRefCountLeakMap() {
        return refCountLeakMap;
    }

    public static final String extractStackTrace(CacheEntry ce) {

        StringBuffer stackTrace = new StringBuffer();

        if (null == ce.id) {
            stackTrace.append(" extractStackTrace: received  null id");
        } else {
            stackTrace.append("\n" + ce.id.toString() + " -->" + ce.getRefCount() + " pendingRemoval: " + ce.pendingRemoval + " removeWhenUnpinned: "
                              + ce.removeWhenUnpinned + " age(seconds): " + (int) ((System.currentTimeMillis() - ce.getTimeStamp()) / 1000));
        }

        Throwable t = new Throwable();
        StackTraceElement[] ste = t.getStackTrace();
        for (int i = 0; i < 10 && i < ste.length; i++) {
            stackTrace.append("\n" + ste[i].toString());
        }
        return stackTrace.toString();
    }

    public void lookForLeaks() {

        long now = System.currentTimeMillis();
        if (lastTimeCheck == 0) {
            lastTimeCheck = now;
            return;
        } else if ((now - lastTimeCheck) < leakDetectionInterval) {
            return;
        }

        lastTimeCheck = now;

        try {
            FileWriter outFile = new FileWriter(leakDetectionOutput, true);
            outFile.write("\n\n\n****  " + (new Date()) + "  ***\n");
            for (Map.Entry<Object, String> entry : refCountLeakMap.entrySet()) {
                outFile.write(entry.getValue());
                outFile.write("\n");
            }
            outFile.write("\n----------\n");
            outFile.close();

        } catch (IOException e) {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.cache.Cache.lookForLeaks", "3500", this);
        }
    }

    public class LruToDiskResult {

        @Override
        public String toString() {
            return "LruToDiskResult [entryOverwritten=" + entryOverwritten + ", result=" + result + "]";
        }

        public int result; // entry successfully written to disk
        public boolean entryOverwritten; // entry was overwritten on disk (already existed)

        public LruToDiskResult() {
            result = HTODDynacache.NO_EXCEPTION;
            entryOverwritten = false;
        }
    }

    /*
     * PM21179
     */
    @Override
    public void clearMemory(boolean clearDisk) {

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, cacheName + " clearDisk" + clearDisk);
        }

        if (swapToDisk && clearDisk == false) {
            flushToDisk(); // move entries from memory to disk
        } else if (clearDisk == true) {
            diskCache.clearDiskCache();
            diskCache.clearInvalidationBuffers();
        }

        Enumeration e = entryHashtable.keys();
        while (e.hasMoreElements()) {
            Object id = e.nextElement();
            internalInvalidateById(id, CachePerf.DIRECT, CachePerf.LOCAL, !FIRE_INVALIDATION_LISTENER);
        }

        if (bEnableListener) {
            InvalidationEvent ie = new InvalidationEvent("*", null, InvalidationEvent.CLEAR_ALL, InvalidationEvent.LOCAL, this.cacheName);
            eventSource.fireEvent(ie);
        }

        timeLimitDaemon.cacheCleared(this);
        this.invalidationAuditDaemon.cacheCleared(cacheName);

        if (isCacheSizeInMBEnabled()) {
            this.currentMemoryCacheSizeInBytes = 0;
            increaseCacheSizeInBytes(ObjectSizer.FASTHASHTABLE_INITIAL_OVERHEAD + ObjectSizer.FASTHASHTABLE_INITIAL_PER_ENTRY_OVERHEAD
                                     * cacheConfig.cacheSize, "EHT");
        }

        CachePerf cachePerf = cachePerfRef.get();
        if (cachePerf != null && cachePerf.isPMIEnabled()) {
            cachePerf.onCacheClear(true, swapToDisk);
        }

    }

    @Override
    public void resetPMICounters() {
        CachePerf cachePerf = cachePerfRef.get();
        if (cachePerf != null) {
            cachePerf.resetPMICounters();
        }
    }
}
