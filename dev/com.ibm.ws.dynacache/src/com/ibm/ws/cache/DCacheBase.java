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

import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.ibm.websphere.cache.ChangeListener;
import com.ibm.websphere.cache.InvalidationListener;
import com.ibm.websphere.cache.PreInvalidationListener;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.cache.intf.CacheStatisticsListener;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.intf.DCacheConfig;
import com.ibm.ws.cache.stat.CachePerf;
import com.ibm.ws.cache.stat.CachePerfFactory;
import com.ibm.wsspi.cache.EventSource;

/**
 * This abstract class is extended by Cache.java and CacheProviderWrapper.java. It contains common variables and methods
 * for any cache provider.
 */
public abstract class DCacheBase implements DCache {

    private static TraceComponent tc = Tr.register(DCacheBase.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    // -------------------------------------------------
    // Constants
    // -------------------------------------------------
    public static final String DEFAULT_CACHE_NAME = "baseCache";
    public static final int DEFAULT_CACHE_SIZE = 2000;
    public static final String DEFAULT_DISTRIBUTED_MAP_NAME = "default";
    public static final String DEFAULT_BASE_JNDI_NAME = "services/cache/basecache";
    public static final String DEFAULT_DMAP_JNDI_NAME = "services/cache/distributedmap";

    public static final boolean FIRE_INVALIDATION_LISTENER = true;
    public static final boolean COORDINATE = true;
    public static final boolean INCREMENT_REFF_COUNT = true;
    public static final boolean IGNORE_COUNTING = true;
    public static final boolean ASK_PERMISSION = true;
    public static final boolean FROM_DEPID_TEMPLATE_INVALIDATION = true;
    public static final boolean CHECK_PREINVALIDATION_LISTENER = true;
    public static final boolean ENTRY_OVERWRITTEN_ON_DISK = true;

    /**
     * The internal name of this cache - JNDI name if defined from Admin Console
     */
    protected String cacheName; // Caution - If this cache was created from the factory,
                                // it will be NOT be prefixed with DistributedObjectCache.FACTORY_PREFIX.

    protected CacheConfig cacheConfig = null;

    protected final AtomicReference<CachePerf> cachePerfRef = new AtomicReference<CachePerf>();

    protected CacheStatisticsListener cacheStatisticsListener = null;

    protected boolean hasPushPullEntries = false; // PK59026

    // -------------------------------------------------
    // constraint for JVM heap
    // -------------------------------------------------
    protected boolean memoryCacheSizeInMBEnabled = false;
    protected long upperLimitMemoryCacheSizeInBytes = -1;
    protected long lowerLimitMemoryCacheSizeInBytes = -1;
    protected long maxMemoryCacheSizeInBytes = -1;
    protected volatile long currentMemoryCacheSizeInBytes = -1;

    // -------------------------------------------------
    // Listener support
    // -------------------------------------------------
    protected boolean bEnableListener = false;
    protected EventSource eventSource = null;
    protected boolean ignoreValueInInvalidationEvent = false;

    // -------------------------------------------------
    // Disk Cache support
    // -------------------------------------------------
    protected boolean swapToDisk = false; // CacheOnDisk can set this to false!

    public DCacheBase(String cacheName, CacheConfig cacheConfig) {
        this.cacheConfig = cacheConfig;
        this.ignoreValueInInvalidationEvent = cacheConfig.ignoreValueInInvalidationEvent;
        this.cacheName = cacheName;
        if (cacheConfig.memoryCacheSizeInMB != CacheConfig.DEFAULT_DISABLE_CACHE_SIZE_MB) {
            if (cacheConfig.isDefaultCacheProvider()) {
                this.memoryCacheSizeInMBEnabled = (cacheConfig.memoryCacheSizeInMB > 0);
                if (this.memoryCacheSizeInMBEnabled) {
                    this.currentMemoryCacheSizeInBytes = 0;
                    this.maxMemoryCacheSizeInBytes = cacheConfig.memoryCacheSizeInMB * DiskCacheSizeInfo.MB_SIZE;
                    this.upperLimitMemoryCacheSizeInBytes = this.maxMemoryCacheSizeInBytes * cacheConfig.memoryCacheHighThreshold / 100l;
                    this.lowerLimitMemoryCacheSizeInBytes = this.maxMemoryCacheSizeInBytes * cacheConfig.memoryCacheLowThreshold / 100l;
                }
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, "DCacheBase cacheName=" + this.cacheName + " memoryCacheSizeInMBEnabled=" + memoryCacheSizeInMBEnabled
                            + " upperLimitMemoryCacheSizeInBytes=" + upperLimitMemoryCacheSizeInBytes + " lowerLimitMemoryCacheSizeInBytes="
                            + lowerLimitMemoryCacheSizeInBytes + " maxMemoryCacheSizeInBytes=" + maxMemoryCacheSizeInBytes);
                }
            } else {
                // DYNA1068E: The cache size in MB feature is disabled because the configured cache provider \"{0}\"
                // does not support this feature for the cache instance \"{1}\".
                Tr.error(tc, "DYNA1068E", new Object[] { cacheConfig.cacheProviderName, this.cacheName });
                disableCacheSizeInMB();
            }
        }
    }

    /**
     * This is a helper method to add change listener to all entries.
     */
    public synchronized boolean addChangeListener(ChangeListener listener) {
        if (bEnableListener && listener != null) {
            eventSource.addListener(listener);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "addChangeListener() cacheName=" + this.cacheName + " listener=" + eventSource.getChangeListenerCount());
            }
            return true;
        }
        return false;
    }

    /**
     * This is a helper method to add invalidation listener to all entries.
     */
    public synchronized boolean addInvalidationListener(InvalidationListener listener) {
        if (bEnableListener && listener != null) {
            eventSource.addListener(listener);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "addInvalidationListener() cacheName=" + this.cacheName + " listener=" + eventSource.getInvalidationListenerCount());
            }
            return true;
        }
        return false;
    }

    /**
     * This is a helper method to add pre-invalidation listener to all entries.
     */
    public synchronized boolean addPreInvalidationListener(PreInvalidationListener listener) {
        if (bEnableListener && listener != null) {
            if (eventSource.getPreInvalidationListenerCount() > 0 && tc.isDebugEnabled()) {
                Tr.debug(tc, "addPreInvalidationListener() cacheName=" + this.cacheName + " one already exists. Overwriting old listener.");
            }
            eventSource.addListener(listener);
            return true;
        }
        return false;
    }

    /**
     * Clears everything from the cache, so that it is just like when it was instantiated.
     * 
     */
    public void clear() {
        boolean waitOnInvalidation = true;
        clear(waitOnInvalidation);
    }

    public abstract void clear(boolean waitOnInvalidation);

    /**
     * enableListener - enable or disable the invalidation, change and preInvalidation listener support. You must call
     * enableListener(true) before calling addInvalidationListner(), addChangeListener() or
     * addPreInvalidationListener().
     * 
     * @param enable
     *            - true to enable support for invalidation, change and preInvalidation listeners or false to disable
     *            support for invalidation, change and preInvalidation listeners
     * @return boolean "true" means listener support was successfully enabled or disabled. "false" means this cache is
     *         configurated to use the listener's J2EE context for event notification and the callback registration
     *         failed. In this case, the caller's thread context will be used for event notification.
     */
    public synchronized boolean enableListener(boolean enable) {
        boolean success = true;
        if (enable && eventSource == null) {
            success = initEventSource();
        }
        bEnableListener = enable;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "enableListener() cacheName=" + this.cacheName + " enable=" + enable + " success=" + success
                    + " ignoreValueInInvalidationEvent=" + this.ignoreValueInInvalidationEvent);
        }
        return success;
    }

    /**
     * Returns the configuration object passed into the CacheProvider.createCache(CacheConfig cc) method when the cache
     * was created.
     * 
     * @return the configuration associated with the cache instance
     */
    @Trivial
    public DCacheConfig getCacheConfig() {
        return cacheConfig;
    }

    public abstract Set getCacheIdsByDependency(Object dependency);

    /**
     * This method returns the cache ids dependent on a data id.
     * 
     * @return A set of cache IDs or null.
     */
    public Set getCacheIdsByDependency(String dependency) {
        return getCacheIdsByDependency((Object) dependency);
    }

    public String getCacheName() {
        return this.cacheName;
    }

    /**
     * Returns cachePerf.
     * 
     * @Return cachePerf The cachePerf
     */
    public CachePerf getCachePerf() {
        return this.cachePerfRef.get();
    }

    public void setCachePerf(CachePerfFactory factory) {
        CachePerf oldPerf = cachePerfRef.get();
        CachePerf newPerf = null;
        if (factory != null)
            newPerf = factory.create(this);
        if (cachePerfRef.compareAndSet(oldPerf, newPerf) && oldPerf != null) {
            // if we reset the cache perf, make sure we remove the old counters
            oldPerf.removePMICounters();
        }
    }

    /**
     * Returns CacheStatisticsListner object.
     * 
     * @return CacheStatisticsListener object
     */
    public CacheStatisticsListener getCacheStatisticsListener() {
        return cacheStatisticsListener;
    }

    /**
     * Get the default priority value as set in the Admin GUI/dynacache.xml file.
     * 
     * @param defaultPriority
     *            The default priority for this appserver.
     */
    public int getDefaultPriority() {
        return this.cacheConfig.getCachePriority();
    }

    /**
     * Is cache size in MB enabled?
     * 
     * @Return true if cache size in MB > 0
     */
    @Trivial
    public boolean isCacheSizeInMBEnabled() {
        return this.memoryCacheSizeInMBEnabled;
    }

    /**
     * Disable cache size in MB
     */
    public void disableCacheSizeInMB() {
        this.memoryCacheSizeInMBEnabled = false;
        this.cacheConfig.memoryCacheSizeInMB = -1;
        this.currentMemoryCacheSizeInBytes = -1;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "disableCacheSizeInMB() cacheName=" + this.cacheName);
        }
    }

    /**
     * Increase cache size in bytes to the total count
     * 
     * @param size
     *            The size to be increased
     */
    public void increaseCacheSizeInBytes(long size, String msg) {
        if (this.memoryCacheSizeInMBEnabled) {
            this.currentMemoryCacheSizeInBytes += size;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "increaseCacheSizeInBytes() cacheName=" + cacheName + " " + msg + " size=" + size + " currentMemoryCacheSizeInBytes="
                        + this.currentMemoryCacheSizeInBytes);
            }
        }
    }

    /**
     * Decrease cache size in bytes to the total count
     * 
     * @param size
     *            The size to be decreased
     */
    public void decreaseCacheSizeInBytes(long size, String msg) {
        if (this.memoryCacheSizeInMBEnabled) {
            this.currentMemoryCacheSizeInBytes -= size;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "decreaseCacheSizeInBytes() cacheName=" + cacheName + " " + msg + " size=" + size + " currentMemoryCacheSizeInBytes="
                        + this.currentMemoryCacheSizeInBytes);
            }
        }
    }

    public float getCurrentMemoryCacheSizeInMB() {
        long memoryCacheSizeInBytes = this.currentMemoryCacheSizeInBytes;
        if (memoryCacheSizeInBytes >= 0) {

            float currentMemoryCacheSizeInMB = (float) memoryCacheSizeInBytes / (float) DiskCacheSizeInfo.MB_SIZE;
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "getCurrentMemoryCacheSizeInMB() cacheName=" + this.cacheName + " currentMemoryCacheSizeInBytes="
                        + memoryCacheSizeInBytes + " currentMemoryCacheSizeInMB=" + currentMemoryCacheSizeInMB);
            }
            return currentMemoryCacheSizeInMB;
        } else {
            return -1;
        }
    }

    /**
     * This returns the cache entry identified by the specified entryInfo. It returns null if not in the cache.
     * 
     * @param ei
     *            The entryInfo for the entry.
     * @return The entry identified by the entryInfo.
     */
    public com.ibm.websphere.cache.CacheEntry getEntry(com.ibm.websphere.cache.EntryInfo ei) {
        return getEntry(ei, true);
    }

    public abstract com.ibm.websphere.cache.CacheEntry getEntry(com.ibm.websphere.cache.EntryInfo ei, boolean checkAskPermission);

    public abstract com.ibm.websphere.cache.CacheEntry getEntry(Object id, int source, boolean ignoreCounting, boolean incrementRefCount);

    /**
     * This returns the cache entry identified by the specified cache id. It returns null if not in the cache.
     * 
     * @param id
     *            The string representation of cache id for the entry. The id cannot be null.
     * @return The entry identified by the cache id.
     */
    public com.ibm.websphere.cache.CacheEntry getEntry(String id) {
        return getEntry((Object) id);
    }

    /**
     * This returns the cache entry identified by the specified cache id. It returns null if not in the cache.
     * 
     * @param id
     *            The object representation of cache id for the entry. The id cannot be null.
     * @return The entry identified by the cache id.
     */
    public com.ibm.websphere.cache.CacheEntry getEntry(Object id) {
        return getEntry(id, CachePerf.LOCAL, IGNORE_COUNTING, !INCREMENT_REFF_COUNT);
    }

    public EventSource getEventSource() {
        return this.eventSource;
    }

    /**
     * This gets the maximum number of cache entries for this cache instance.
     * 
     * @return The maximum number of cache entries.
     */
    public int getMaxNumberCacheEntries() {
        return cacheConfig.cacheSize;
    }

    /**
     * Is disk cache offload enable?
     * 
     * @return the boolean to indicate disk cache offload enabled or not.
     */
    public boolean getSwapToDisk() {
        return swapToDisk;
    }

    public Object getValue(com.ibm.websphere.cache.EntryInfo ei, boolean askPermission) {
        return getValue(ei.getIdObject(), ei.getTemplate(), askPermission, !IGNORE_COUNTING);
    }

    public Object getValue(Object id, boolean askPermission) {
        return getValue(id, null, askPermission, !IGNORE_COUNTING);
    }

    public abstract Object getValue(Object id, String template, boolean askPermission, boolean ignoreCounting);

    public Object getValue(String id, boolean askPermission) {
        return getValue((Object) id, askPermission);
    }

    public boolean hasPushPullEntries() { // PK59026
        return hasPushPullEntries;
    }

    /**
     * This is a helper method to initialize event source for invalidation listener
     */
    private boolean initEventSource() {
        boolean success = false;
        try {
            eventSource = ServerCache.cacheUnit.createEventSource(cacheConfig.useListenerContext, cacheName);
        } catch (Exception ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.Cache.initEventSource", "3289", this);
        }
        if (eventSource != null) {
            success = true;
        }
        return success;
    }

    public void invalidateById(Object id, boolean waitOnInvalidation) {
        invalidateById(id, CachePerf.DIRECT, waitOnInvalidation, CHECK_PREINVALIDATION_LISTENER);
    }

    public void invalidateById(Object id, boolean waitOnInvalidation, boolean checkPreInvalidationListener) {
        invalidateById(id, CachePerf.DIRECT, waitOnInvalidation, checkPreInvalidationListener);
    }

    public void invalidateById(Object id, int causeOfInvalidation, boolean waitOnInvalidation) {
        invalidateById(id, causeOfInvalidation, waitOnInvalidation, CHECK_PREINVALIDATION_LISTENER);
    }

    public abstract void invalidateById(Object id, int causeOfInvalidation, boolean waitOnInvalidation, boolean checkPreInvalidationListener);

    public void invalidateById(String id, boolean waitOnInvalidation) {
        invalidateById((Object) id, CachePerf.DIRECT, waitOnInvalidation, CHECK_PREINVALIDATION_LISTENER);
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
     */
    public void invalidateById(Object id, int causeOfInvalidation, int sourceOfInvalidation, boolean waitOnInvalidation,
            boolean invokeInternalInvalidateById) {
        invalidateById(id, causeOfInvalidation, sourceOfInvalidation, waitOnInvalidation, invokeInternalInvalidateById,
                InvalidateByIdEvent.INVOKE_DRS_RENOUNCE);
    }

    public abstract void invalidateById(Object id, int causeOfInvalidation, int sourceOfInvalidation, boolean waitOnInvalidation,
            boolean invokeInternalInvalidateById, boolean invokeDRSRenounce);

    public boolean isEnableListener() {
        return this.bEnableListener;
    }

    /**
     * This is a helper method to remove change listener for all entries.
     */
    public synchronized boolean removeChangeListener(ChangeListener listener) {
        if (bEnableListener && listener != null) {
            eventSource.removeListener(listener);
            return true;
        }
        return false;
    }

    /**
     * This is a helper method to remove invalidation listener for all entries.
     */
    public synchronized boolean removeInvalidationListener(InvalidationListener listener) {
        if (bEnableListener && listener != null) {
            eventSource.removeListener(listener);
            return true;
        }
        return false;
    }

    /**
     * This is a helper method to remove pre-invalidation listener for all entries.
     */
    public synchronized boolean removePreInvalidationListener(PreInvalidationListener listener) {
        if (bEnableListener && listener != null) {
            eventSource.removeListener(listener);
            return true;
        }
        return false;
    }

    public com.ibm.websphere.cache.CacheEntry setEntry(CacheEntry cacheEntry) {
        return setEntry(cacheEntry, CachePerf.LOCAL);
    }

    public com.ibm.websphere.cache.CacheEntry setEntry(CacheEntry cacheEntry, int source) {
        return setEntry(cacheEntry, source, !IGNORE_COUNTING, !COORDINATE, !INCREMENT_REFF_COUNT);
    }

    public abstract com.ibm.websphere.cache.CacheEntry setEntry(CacheEntry cacheEntry, int source, boolean ignoreCounting, boolean coordinate,
            boolean incRefCount);

    /**
     * This sets the actual value (JSP or command) of an entry in the cache.
     * 
     * @param entryInfo
     *            The cache entry
     * @param value
     *            The value to cache in the entry
     */
    public void setValue(EntryInfo entryInfo, Object value) {
        setValue(entryInfo, value, !shouldPull(entryInfo.getSharingPolicy(), entryInfo.id), DynaCacheConstants.VBC_CACHE_NEW_CONTENT);
    }

    /**
     * This sets the actual value (JSP or command) of an entry in the cache.
     * 
     * @param entryInfo
     *            The cache entry
     * @param value
     *            The value to cache in the entry
     * @param directive
     *            boolean to indicate CACHE_NEW_CONTENT or USE_CACHED_VALUE
     */
    public void setValue(EntryInfo entryInfo, Object value, boolean directive) {
        setValue(entryInfo, value, !shouldPull(entryInfo.getSharingPolicy(), entryInfo.id), directive);
    }

    public abstract void setValue(EntryInfo entryInfo, Object value, boolean coordinate, boolean directive);

    public abstract boolean shouldPull(int share, Object id);

}
