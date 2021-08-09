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
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.websphere.cache.EntryInfo;
import com.ibm.websphere.cache.exception.DynamicCacheException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.intf.ServletCacheUnit;
import com.ibm.wsspi.cache.CacheFeatureSupport;
import com.ibm.wsspi.cache.CacheStatistics;
import com.ibm.wsspi.cache.CoreCache;

/**
 * This is the impl class thats wraps each third party cache provider. This class extends the DCacheBase
 * abstract class which contains all the methods called by Dynacache internal code. The cache provider
 * overrides all the methods in DCache that are common with CoreCache. For all methods in the CoreCache
 * this class delegates to the corresponding method to the CacheProvider's impl of CoreCache.
 * 
 * When Dynacache calls methods on this Wrapper that are NOT defined in CoreCache. This wrapper logs a
 * debug message indicating that the function is NOT supported for a cache provider. Look {@code}setTimeLimitDaemon
 * 
 * If the CacheProvider's corecache method throws a runtime exception, we do NOT handle the exception. This
 * exception is propogated upstream to the caller.
 *
 */
public class CacheProviderWrapper extends DCacheBase {

    private static TraceComponent tc = Tr.register(CacheProviderWrapper.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    CacheFeatureSupport featureSupport = null;
    CoreCache coreCache = null;
    String cacheProviderName = "";
    CacheStatistics cacheStatistics = null;
    private final static HashMap EMPTY_MAP = new HashMap(0);
    private final static String CLEAR = "clearCache";

    /**
     * CacheProviderWrapper constructor kept public for FVT purposes
     * 
     * @param cacheConfig The cache configuration
     * @param featureSupport The feature support
     * @param coreCache The CoreCache
     */
    public CacheProviderWrapper(CacheConfig cacheConfig, CacheFeatureSupport featureSupport, CoreCache coreCache) {
        super(cacheConfig.cacheName, cacheConfig);
        final String methodName = "CTOR";
        this.cacheProviderName = cacheConfig.cacheProviderName;
        this.featureSupport = featureSupport;
        this.coreCache = coreCache;
        this.swapToDisk = cacheConfig.enableDiskOffload;
        if (this.swapToDisk == true && this.featureSupport.isDiskCacheSupported() == false) {
            //DYNA1064E=DYNA1064E: The operation \"{0}\" for cacheName \"{1}\" cannot be performed because cache provider \"{2}\" does not support disk cache offload feature.
            Tr.error(tc, "DYNA1064E", new Object[] { "enableDiskOffload", this.cacheName, cacheProviderName });
            this.swapToDisk = false;
            cacheConfig.enableDiskOffload = false;
        }
        if (cacheConfig.enableCacheReplication == true && featureSupport.isReplicationSupported() == false) {
            //DYNA1065E=DYNA1065E: The operation \"{0}\" for cacheName \"{1}\" cannot be performed because cache provider \"{2}\" does not support DRS replication feature.
            Tr.error(tc, "DYNA1065E", new Object[] { "enableCacheReplication", this.cacheName, cacheProviderName });
            cacheConfig.enableCacheReplication = false;
        }
        this.cacheStatistics = this.coreCache.getCacheStatistics();
        if (this.cacheStatistics == null && tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " **ERROR cacheStatistics object is found to be NULL.");
        }
        cacheStatisticsListener = new CacheStatisticsListenerWrapper(this.cacheStatistics);
    }

    /**
     * Returns true if memory cache contains a mapping for the specified cache
     * ID.
     * 
     * @param cacheId
     *            cache ID is to be tested.
     * @return <code>true</code> if the memory cache contains the specified
     *         cacheID.
     */
    @Override
    public boolean containsCacheId(Object cacheId) {
        boolean found = false;
        if (cacheId != null) {
            found = this.coreCache.containsCacheId(cacheId);
        }
        return found;
    }

    /**
     * This method clears everything from the cache, so that it is just like
     * when it was instantiated.
     *
     * @param waitOnInvalidation True indicates that this method should
     *            not return until the invalidations have taken effect on all caches.
     *            False indicates that the invalidations will be queued for later
     *            batch processing. (No effect on CoreCache)
     */
    @Override
    public void clear(boolean waitOnInvalidation) {
        invalidateExternalCaches(null, CLEAR);
        this.coreCache.clear();
    }

    /**
     * enableListener - enable or disable the invalidation, change and preInvalidation listener support.
     * You must call enableListener(true) before calling addInvalidationListner(), addChangeListener() or
     * addPreInvalidationListener(). The EventSource is passed to the CoreCache so that it can fire events.
     *
     * @param enable - true to enable support for invalidation, change and preInvalidation listeners
     *            or false to disable support for invalidation, change and preInvalidation listeners
     * @return boolean "true" means listener support was successfully enabled or disabled.
     *         "false" means this cache is configurated to use the listener's J2EE context for
     *         event notification and the callback registration failed. In this case, the caller's thread
     *         context will be used for event notification.
     */
    @Override
    public boolean enableListener(boolean enable) {
        boolean success = super.enableListener(enable);
        this.coreCache.setEventSource(eventSource);
        return success;
    }

    /**
     * Returns various cache statistics encapsulated int the CacheStatistics
     * interface.
     * 
     * @return CacheStatistics
     */
    @Override
    public CacheStatistics getCacheStatistics() {
        return this.cacheStatistics;
    }

    /**
     * This returns the cache entry identified by the specified entryInfo.
     * It returns null if not in the cache.
     *
     * @param ei The entryInfo for the entry.
     * @param checkAskPermission true to check askPermission from sharing policy (No effect on CoreCache)
     * @return The entry identified by the entryInfo.getIdObject().
     */
    @Override
    public com.ibm.websphere.cache.CacheEntry getEntry(EntryInfo ei, boolean checkAskPermission) {
        final String methodName = "getEntry()";
        com.ibm.websphere.cache.CacheEntry ce = null;
        Object id = null;
        if (ei != null) {
            id = ei.getIdObject();
            ce = this.coreCache.get(id);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " id=" + id + " cacheEntry=" + ce);
        }
        return ce;
    }

    @Override
    public com.ibm.websphere.cache.CacheEntry getEntry(EntryInfo ei, boolean checkAskPermission, boolean ignoreCounting) {
        final String methodName = "getEntry()";
        com.ibm.websphere.cache.CacheEntry ce = null;
        Object id = null;
        if (ei != null) {
            id = ei.getIdObject();
            ce = this.coreCache.get(id);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " id=" + id + " cacheEntry=" + ce);
        }
        return ce;
    }

    /**
     * This returns the cache entry identified by the specified cache id.
     * It returns null if not in the cache.
     *
     * @param id The cache id for the entry. The id cannot be null.
     * @param source The source - local or remote (No effect on CoreCache)
     * @param ignoreCounting true to ignore the statistics counting (No effect on CoreCache)
     * @param incrementRefCount true to increment the refCount of the entry (No effect on CoreCache)
     * @return The entry identified by the cache id.
     */
    @Override
    public com.ibm.websphere.cache.CacheEntry getEntry(Object id, int source, boolean ignoreCounting, boolean incrementRefCount) {
        final String methodName = "getEntry()";
        com.ibm.websphere.cache.CacheEntry ce = this.coreCache.get(id);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " id=" + id + " cacheEntry=" + ce);
        }
        return ce;
    }

    /**
     * This returns the cache entry from memory cache identified by the specified cache id.
     * It returns null if not in the cache.
     *
     * @param id The cache id for the entry. The id cannot be null.
     * @return The entry identified by the cache id.
     */
    @Override
    public com.ibm.websphere.cache.CacheEntry getEntryFromMemory(Object id) {
        final String methodName = "getEntryFromMemory()";
        com.ibm.websphere.cache.CacheEntry ce = this.coreCache.get(id);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " id=" + id + " cacheEntry=" + ce);
        }
        return ce;
    }

    /**
     * Returns an enumeration view of the cache IDs contained in the memory cache.
     * 
     * @return An enumeration of cache IDs or empty if there is no cache ID.
     */
    @Override
    public Enumeration getAllIds() {
        Set ids = this.coreCache.getCacheIds();
        ValueSet idvs = new ValueSet(ids.iterator());
        return idvs.elements();
    }

    /**
     * Returns a set view of the cache IDs contained in the memory cache.
     * 
     * @return A Set of cache IDs or empty set if there is no cache ID.
     */
    @Override
    public Set getCacheIds() {
        return this.coreCache.getCacheIds();
    }

    /**
     * Returns the dependency IDs for all the cache entries. A dependency
     * provides a mechanism for grouping of cache-ids.
     * 
     * A dependency-id can also be a cache-id. Dependency IDs label cache
     * entries and are used by invalidation rules to invalidate & timeout one or
     * more cache entries at a time.
     * 
     * The relationship beween a dependency-id and its dependent ids is formed
     * via invalidation rules in the cachespec.xml for servlet caches. For
     * object caches this relationship is explicitly specified via the
     * DistributedMap APIs.
     * 
     * @return A set of dependency IDs.
     */
    @Override
    public Collection getAllDependencyIds() {
        return this.coreCache.getDependencyIds();
    }

    /**
     * Returns the cache IDs that are associated with a dependency ID. It
     * returns an empty set if no cache ID is associated with it.
     * <p>
     * <br>
     * Dependency elements are used to group related cache items. Cache entries
     * having the same depenendency ID are managed as a group. Each related
     * cache item shares the same dependency id, so it only takes one member of
     * the dependency group to get invalidated, for the rest of the group to be
     * evicted. The dependency ID can be as simple as just a name such as
     * storeId
     * 
     * @param dependency
     *            dependency ID for the groupof cache IDs.
     * @return A set of cache IDs or empty set if no cache ID is
     *         associated with it.
     */
    @Override
    public Set getCacheIdsByDependency(Object dependency) {
        return this.coreCache.getCacheIds(dependency);
    }

    /**
     * Returns the cache IDs that are associated with a template. It
     * returns an empty set if no cache ID is associated with it.
     * 
     * @param template
     *            template for the groupof cache IDs.
     * @return A set of cache IDs or empty set if no cache ID is
     *         associated with it.
     */
    @Override
    public Set getCacheIdsByTemplate(String template) {
        return this.coreCache.getCacheIds(template);
    }

    /**
     * Returns a hashcode of all the cache entries in memory cache. If includeValue
     * is true, hashcode will include the cache value. lease note that the cache key
     * and value objects should override the default java.lang.Object.hashCode()
     * method to get semantic comparability between object instances.
     * 
     * This method is relevant ONLY if the CacheProvider supports cache replication Dynacache style.
     * i.e. cacheFeatureSupport.isReplicationEnabled returns true.
     *
     * @param debug If debug is true, a list of the cache IDs and their hashcodes are written to the SystemOut log.
     * @param includeValue If includeValue is true, the hashcode of value will be in the calculation.
     * @return hashcode of all the cache entries
     */
    @Override
    public int getMemoryCacheHashcode(boolean debug, boolean includeValue) {
        final String methodName = "getMemoryCacheHashcode()";
        int totalHashcode = 0;
        StringBuffer sb = new StringBuffer();
        int count = 0;
        int totalCount = 0;
        Set cacheIds = this.coreCache.getCacheIds();
        if (cacheIds != null && !cacheIds.isEmpty()) {
            Iterator it = cacheIds.iterator();
            while (it.hasNext()) {
                Object cacheId = it.next();
                boolean found = false;
                int id_hc = cacheId.hashCode();
                int value_hc = 0;
                if (includeValue) {
                    com.ibm.websphere.cache.CacheEntry ce = this.coreCache.get(cacheId);
                    if (ce != null) {
                        found = true;
                        value_hc = ce.getValue().hashCode();
                    } else {
                        if (tc.isDebugEnabled()) {
                            Tr.debug(tc, methodName + " cacheName=" + cacheName + " cacheId=" + cacheId + " ERROR - the cache entry cannot be found");
                        }
                    }
                } else {
                    found = true;
                }
                // if debug is true, put the cache id and its hashcode into string buffer and then write to systemout.log 
                // when it reaches 100 entries.
                if (debug && found) {
                    sb.append("\nid=");
                    sb.append(cacheId);
                    sb.append(" id_hashcode=");
                    sb.append(id_hc);
                    if (includeValue) {
                        sb.append(" value_hashcode=");
                        sb.append(value_hc);
                    }
                    count++;
                    if (count == 100) {
                        Tr.info(tc, "DYNA1035I", new Object[] { String.valueOf(count), this.cacheName, sb.toString() });
                        sb.setLength(0);
                        count = 0;
                    }
                }
                if (found) {
                    totalCount++;
                    totalHashcode += id_hc;
                    if (includeValue) {
                        totalHashcode += value_hc;
                    }
                }
            }
        }
        if (debug && count > 0) {
            Tr.info(tc, "DYNA1035I", new Object[] { String.valueOf(count), this.cacheName, sb.toString() });
        }
        Tr.info(tc, "DYNA1038I", new Object[] { String.valueOf(totalCount), this.cacheName });
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "getMemoryCacheHashcode():  totalCount=" + totalCount + " totalHashcode=" + totalHashcode);
        }
        return totalHashcode;
    }

    /**
     * This gets the current number of cache entries for this cache instance.
     *
     * @return The current number of cache entries.
     */
    @Override
    public int getNumberCacheEntries() {
        if (this.cacheStatistics != null) {
            return (int) this.cacheStatistics.getMemoryCacheEntriesCount();
        }
        return 0;
    }

    /**
     * This gets the current number of cache entries for this cache instance.
     *
     * @return The current number of cache entries.
     */
    @Override
    public int getNumberCacheEntriesUnsynchronized() {
        return getNumberCacheEntries();
    }

    /**
     * Returns the value to which this map maps the specified cache id. Returns
     * <tt>null</tt> if the map contains no mapping for this key.
     *
     * @param id cache id whose associated value is to be returned.
     * @param template template name associated with cache id (No effect on CoreCache)
     * @param askPermission True implies that execution must ask the coordinating CacheUnit for permission (No effect on CoreCache).
     * @param ignoreCounting True implies that no counting for PMI and cache statistics (No effect on CoreCache)
     * @return the value to which this map maps the specified cache id, or
     *         <tt>null</tt> if the map contains no mapping for this cache id.
     */
    @Override
    public Object getValue(Object id, String template, boolean askPermission, boolean ignoreCounting) {
        final String methodName = "getValue()";
        Object value = null;
        com.ibm.websphere.cache.CacheEntry ce = this.coreCache.get(id);
        if (ce != null) {
            value = ce.getValue();
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " id=" + id + " value=" + value);
        }
        return value;
    }

    /**
     * Puts an entry into the cache. If the entry already exists in the
     * cache, this method will ALSO update the same.
     * 
     * Called by DistributedMap
     * 
     * @param ei The EntryInfo object
     * @param value The value of the object
     * @param coordinate Indicates that the value should be set in other caches caching this value. (No effect on CoreCache)
     */
    @Override
    public Object invalidateAndSet(com.ibm.ws.cache.EntryInfo ei, Object value, boolean coordinate) {
        final String methodName = "invalidateAndSet()";
        Object oldValue = null;
        Object id = null;
        if (ei != null && value != null) {
            id = ei.getIdObject();
            com.ibm.websphere.cache.CacheEntry oldCacheEntry = this.coreCache.put(ei, value);
            if (oldCacheEntry != null) {
                oldValue = oldCacheEntry.getValue();
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " id=" + id + " value=" + value);
        }
        return oldValue;
    }

    /**
     * This invalidates all entries in this Cache having a dependency
     * on this dependency id.
     *
     * @param id dependency id.
     * @param causeOfInvalidation The cause of invalidation
     * @param source The source of invalidation (local or remote)
     * @param bFireIL True to fire invalidation event
     */
    @Override
    public void internalInvalidateByDepId(Object id, int causeOfInvalidation, int source, boolean bFireIL) {
        final String methodName = "internalInvalidateByDepId()";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " id=" + id);
        }
        this.invalidateExternalCaches(id, null);
        this.coreCache.invalidateByDependency(id, true);
    }

    /**
     * This invalidates all entries in this Cache having a dependency
     * on this id.
     *
     * @param id cache id or dependency id.
     * @param causeOfInvalidation The cause of invalidation (No effect on CoreCache)
     * @param waitOnInvalidation True indicates that this method should
     *            not return until the invalidations have taken effect on all caches.
     *            False indicates that the invalidations will be queued for later
     *            batch processing.
     * @param checkPreInvalidationListener true indicates that we will verify with the preInvalidationListener
     *            prior to invalidating. False means we will bypass this check. (No effect on CoreCache)
     */
    @Override
    public void invalidateById(Object id, int causeOfInvalidation, boolean waitOnInvalidation, boolean checkPreInvalidationListener) {
        final String methodName = "invalidateById()-1";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " id=" + id);
        }
        this.invalidateExternalCaches(id, null);
        this.coreCache.invalidate(id, waitOnInvalidation);
    }

    /**
     * This invalidates all entries in this Cache having a dependency
     * on this id.
     *
     * @param id cache id or dependency id.
     * @param causeOfInvalidation The cause of invalidation (No effect on CoreCache)
     * @param sourceOfInvalidation The source of invalidation (local or remote) (No effect on CoreCache)
     * @param waitOnInvalidation True indicates that this method should
     *            not return until the invalidations have taken effect on all caches.
     *            False indicates that the invalidations will be queued for later
     *            batch processing.
     * @param invokeInternalInvalidation true indicates that the local invalidation should happen.
     *            False means do not invoke local invalidation. It is used to prevent infinite loop. (No effect on CoreCache)
     * @param invokeDRSRenounce true indicates to invoke DRS renounce during sending the message to remote server. (No effect on CoreCache)
     */
    @Override
    public void invalidateById(Object id, int causeOfInvalidation, int sourceOfInvalidation,
                               boolean waitOnInvalidation, boolean invokeInternalInvalidateById, boolean invokeDRSRenounce) {
        final String methodName = "invalidateById()-2";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " id=" + id);
        }
        this.invalidateExternalCaches(id, null);
        this.coreCache.invalidate(id, waitOnInvalidation);
    }

    /**
     * This invalidates all entries in this Cache having a dependency
     * on this template.
     *
     * @param template The template name.
     * @param waitOnInvalidation True indicates that this method should
     *            not return until the invalidations have taken effect on all caches.
     *            False indicates that the invalidations will be queued for later
     *            batch processing.
     */
    @Override
    public void invalidateByTemplate(String template, boolean waitOnInvalidation) {
        final String methodName = "invalidateByTemplate()";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " template=" + template);
        }
        this.invalidateExternalCaches(null, template);
        this.coreCache.invalidateByTemplate(template, waitOnInvalidation);
    }

    private void invalidateExternalCaches(Object id, String template) {

        ServletCacheUnit su = ServerCache.cacheUnit.getServletCacheUnit();

        if (su != null && cacheConfig.enableServletSupport) {

            if (null != id) { //propogate ids and dep ids
                HashMap invalidateIDMap = new HashMap(1);
                invalidateIDMap.put(id, null);
                su.invalidateExternalCaches(invalidateIDMap, EMPTY_MAP);
            }

            if (null != template && 0 != template.trim().length()) { //propogate templates
                InvalidateByTemplateEvent ite = new InvalidateByTemplateEvent(template, -1);
                HashMap invalidateTemplateMap = new HashMap(1);
                if (template.equals(CLEAR)) {
                    ite.setCacheCommand_Clear();
                } else {
                    ValueSet removedIds = new ValueSet(coreCache.getCacheIds(template).iterator());
                    ite.addRemovedIds(removedIds);
                }
                invalidateTemplateMap.put(template, ite);
                su.invalidateExternalCaches(EMPTY_MAP, invalidateTemplateMap);
            }
        }
    }

    /**
     * This is used by the CacheHook to determine if an entry has
     * been either removed or invalidated while it is being rendered.
     *
     * @param id The cache id for the entry being tested.
     * @return True if id is in cache
     */
    @Override
    public boolean isValid(String id) {
        return this.coreCache.containsCacheId(id);
    }

    /**
     * This method is kept for cache monitor. Used to move the entry
     * to the end of the LRU queue
     */
    @Override
    public void refreshEntry(com.ibm.websphere.cache.CacheEntry cacheEntry) {
        final String methodName = "refreshEntry()";
        Object id = null;
        if (cacheEntry != null) {
            id = cacheEntry.getIdObject();
            this.coreCache.refreshEntry(id);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " id=" + id);
        }
    }

    /**
     * Puts an entry into the cache. If the entry already exists in the
     * cache, this method will ALSO update the same.
     * 
     * Called by DistributedNioMap (old ProxyCache)
     * Called by Cache.setEntry(cacheEntry, source)
     * 
     * @param cacheEntry The CacheEntry object
     * @param source The source (local or remote) (No effect on CoreCache)
     * @param ignoreCounting true to ignore the statistics counting (No effect on CoreCache)
     * @param coordinate Indicates that the value should be set in other caches caching this value. (No effect on CoreCache)
     * @param incRefcount Indicates that refcount of the returned cache entry will be incremented.
     *
     * @return The CacheEntry
     */
    @Override
    public com.ibm.websphere.cache.CacheEntry setEntry(CacheEntry cacheEntry, int source,
                                                       boolean ignoreCounting, boolean coordinate, boolean incRefcount) {
        final String methodName = "setEntry()";
        Object id = null;
        com.ibm.websphere.cache.CacheEntry newEntry = null;
        if (cacheEntry != null) {
            id = cacheEntry.getIdObject();
            com.ibm.ws.cache.EntryInfo ei = new com.ibm.ws.cache.EntryInfo();
            ei.copyMetadata(cacheEntry);
            this.coreCache.put(ei, cacheEntry.getValue());
            if (incRefcount) {
                newEntry = this.coreCache.get(id);
            }
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " id=" + id);
        }
        return newEntry;
    }

    /**
     * Puts an entry into the cache. If the entry already exists in the
     * cache, this method will ALSO update the same.
     * 
     * Called by DistributedNioMap (old ProxyCache)
     * Called by Cache.setEntry(cacheEntry, source)
     * 
     * @param entryInfo The EntryInfo object
     * @param value The value of the object
     * @param coordinate Indicates that the value should be set in other caches caching this value. (No effect on CoreCache)
     * @param directive boolean to indicate CACHE_NEW_CONTENT or USE_CACHED_VALUE
     */
    @Override
    public void setValue(com.ibm.ws.cache.EntryInfo entryInfo, Object value, boolean coordinate, boolean directive) {
        final String methodName = "setValue()";
        Object id = null;
        if (entryInfo != null) {
            id = entryInfo.getIdObject();
        }
        if (directive == DynaCacheConstants.VBC_CACHE_NEW_CONTENT) {
            this.coreCache.put(entryInfo, value);
        } else {
            this.coreCache.touch(entryInfo.id, entryInfo.validatorExpirationTime, entryInfo.expirationTime);
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " id=" + id + " directive=" + directive);
        }
    }

    /**
     * This method will be invoked once Dynacache is ready to use this cache instance.
     */
    @Override
    public void start() {
        coreCache.start();
    }

    /**
     * This method will be invoked when the application server is stopped.
     */
    @Override
    public void stop() {
        coreCache.stop();
    }

    /*************************************************************************************
     * Alias Support
     *************************************************************************************/

    /**
     * Adds an alias for the given key in the cache's mapping table. If the alias is already
     * associated with another key, it will be changed to associate with the new key.
     * 
     * @param key the key assoicated with alias
     * @param aliasArray the alias to use for lookups
     * @param askPermission True implies that execution must ask the coordinating CacheUnit for permission (No effect on CoreCache).
     * @param coordinate Indicates that the value should be set in other caches caching this value. (No effect on CoreCache)
     */
    @Override
    public void addAlias(Object key, Object[] aliasArray, boolean askPermission, boolean coordinate) {
        final String methodName = "addAlias()";
        if (this.featureSupport.isAliasSupported()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            Tr.error(tc, "DYNA1063E", new Object[] { methodName, cacheName, this.cacheProviderName });
        }
        return;
    }

    /**
     * Removes an alias from the cache mapping.
     * 
     * @param alias the alias assoicated with cache id
     * @param askPermission True implies that execution must ask the coordinating CacheUnit for permission (No effect on CoreCache).
     * @param coordinate Indicates that the value should be set in other caches caching this value. (No effect on CoreCache)
     */
    @Override
    public void removeAlias(Object alias, boolean askPermission, boolean coordinate) {
        final String methodName = "removeAlias()";
        if (this.featureSupport.isAliasSupported()) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            Tr.error(tc, "DYNA1063E", new Object[] { methodName, cacheName, this.cacheProviderName });
        }
        return;
    }

    /*************************************************************************************
     * DRS Push-Pull Support
     *************************************************************************************/

    /**
     * Returns a hashcode of all the cache IDs in PushPullTable.
     * Please note that the cache key and value objects should override the default java.lang.Object.hashCode() and
     * java.lang.Object.equals() method to get semantic comparability between object instances.
     *
     * @param debug If debug is true, a list of the cache IDs and their hashcodes are written to the SystemOut log.
     */
    @Override
    public int getCacheIdsHashcodeInPushPullTable(boolean debug) {
        final String methodName = "getCacheIdsHashcodeInPushPullTable()";
        if (this.featureSupport.isReplicationSupported()) {
            // TODO write code to support getCacheIdsHashcodeInPushPullTable function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            Tr.error(tc, "DYNA1065E", new Object[] { methodName, cacheName, this.cacheProviderName });
        }
        return 0;
    }

    /**
     * Returns all of the cache IDs in the PushPullTable
     */
    @Override
    public List getCacheIdsInPushPullTable() {
        final String methodName = "getCacheIdsInPushPullTable()";
        List list = new ArrayList();
        if (this.featureSupport.isReplicationSupported()) {
            // TODO write code to support getCacheIdsInPushPullTable function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            Tr.error(tc, "DYNA1065E", new Object[] { methodName, cacheName, this.cacheProviderName });
        }
        return list;
    }

    /**
     * Returns number of the cache IDs in the PushPullTable.
     */
    @Override
    public int getPushPullTableSize() {
        final String methodName = "getPushPullTableSize()";
        if (this.featureSupport.isReplicationSupported()) {
            // TODO write code to support getPushPullTableSize function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            Tr.error(tc, "DYNA1065E", new Object[] { methodName, cacheName, this.cacheProviderName });
        }
        return 0;
    }

    /**
     * Return to indicate the entry can be pulled from other remote caches which caching this value.
     * 
     * @param share sharing policy
     * @id cache ID
     */
    @Override
    public boolean shouldPull(int share, Object id) {
        final String methodName = "shouldPull()";
        boolean shouldPull = false;
        if (this.featureSupport.isReplicationSupported()) {
            // TODO write code to support shouldPull function
            //if (tc.isDebugEnabled()) {
            //    Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            //}
        } else {
            //Tr.error(tc, "DYNA1065E", new Object[] { methodName, cacheName, this.cacheProviderName});
        }
        return shouldPull;
    }

    /*************************************************************************************
     * Disk Cache Support
     *************************************************************************************/

    /**
     * This method clears everything from the disk cache. This is called by CacheMonitor
     */
    @Override
    public void clearDisk() {
        final String methodName = "clearDisk()";
        if (this.swapToDisk) {
            // TODO write code to support clearDisk function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
    }

    /**
     * Returns true if disk cache contains a mapping for the specified cache
     * ID.
     * 
     * @param key
     *            cache ID is to be tested.
     * @return <code>true</code> if the disk cache contains the specified
     *         cacheID.
     */
    @Override
    public boolean containsKeyDisk(Object key) {
        final String methodName = "containsKeyDisk()";
        if (this.swapToDisk) {
            // TODO write code to support containsKeyDisk function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return false;
    }

    /**
     * Returns the actual number of entries from disk cache without filtering.
     *
     * @return The actual number of entries from the disk cache (no filtering).
     */
    @Override
    public int getActualIdsSizeDisk() {
        final String methodName = "getActualIdsSizeDisk()";
        if (this.swapToDisk) {
            // TODO write code to support getActualIdsSizeDisk function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return 0;
    }

    /**
     * @return the current cache size in bytes for the disk cache.
     */
    @Override
    public long getCacheSizeInBytesDisk() {
        final String methodName = "getCacheSizeInBytesDisk()";
        if (this.swapToDisk) {
            // TODO write code to support getCacheSizeInBytesDisk function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return 0;
    }

    /**
     * Returns the cache IDs that are associated with a dependency ID from the
     * disk cache. It returns an empty set if no cache ID is associated with it.
     * <p>
     * <br>
     * Dependency elements are used to group related cache items. Cache entries
     * having the same depenendency ID are managed as a group. Each related
     * cache item shares the same dependency id, so it only takes one member of
     * the dependency group to get invalidated, for the rest of the group to be
     * evicted. The dependency ID can be as simple as just a name such as
     * storeId
     * 
     * @param depId
     *            dependency ID for the groupof cache IDs.
     * @return A set of cache IDs or empty set if no cache ID is
     *         associated with it.
     */
    @Override
    public Set getCacheIdsByDependencyDisk(Object depId) {
        final String methodName = "getCacheIdsByDependencyDisk()";
        Set ids = new HashSet();
        if (this.swapToDisk) {
            // TODO write code to support getCacheIdsByDependencyDisk function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return ids;
    }

    /**
     * Returns the cache IDs that are associated with a template from the
     * disk cache. It returns an empty set if no cache ID is associated with it.
     * 
     * @param template
     *            template for the groupof cache IDs.
     * @return A set of cache IDs or empty set if no cache ID is
     *         associated with it.
     */
    @Override
    public Set getCacheIdsByTemplateDisk(String template) {
        final String methodName = "getCacheIdsByTemplateDisk()";
        Set ids = new HashSet();
        if (this.swapToDisk) {
            // TODO write code to support getCacheIdsByTemplateDisk function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return ids;
    }

    /**
     * Returns the size of dependency id auxiliary table for disk cache.
     * 
     * @return the size of dependency id auxiliary table for disk cache
     */
    @Override
    public int getDepIdsBufferedSizeDisk() {
        final String methodName = "getDepIdsBufferedSizeDisk()";
        if (this.swapToDisk) {
            // TODO write code to support getDepIdsBufferedSizeDisk function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return 0;
    }

    /**
     * Returns a set of dependency IDs based on the range and size.
     * WARNING: If index = 1 or -1, the set might contain "DISKCACHE_MORE" to indicate there are more dep ids on the disk cache.
     * The caller need to remove DISKCACHE_MORE" from the set before it is being used.
     * The "DISKCACHE_MORE" key is defined as HTODDynacache.DISKCACHE_MORE.
     * 
     * @param index If index = 0, it starts the beginning. If index = 1, it means "next". If Index = -1, it means "previous".
     * @param length The max number of dependency ids to be read. If length = -1, it reads all dependency ids until the end.
     * @return The Set of dependency ids.
     */
    @Override
    public Set getDepIdsByRangeDisk(int index, int length) {
        final String methodName = "getDepIdsByRangeDisk()";
        Set ids = new HashSet();
        if (this.swapToDisk) {
            // TODO write code to support getDepIdsByRangeDisk function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return ids;
    }

    /**
     * Returns the current dependency IDs size for the disk cache.
     *
     * @return The current dependency ids size for the disk cache.
     */
    @Override
    public int getDepIdsSizeDisk() {
        final String methodName = "getDepIdsSizeDisk()";
        if (this.swapToDisk) {
            // TODO write code to support getDepIdsSizeDisk function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return 0;
    }

    /**
     * Returns the exception object from the disk cache because disk cache reported the error.
     *
     * @return The exception object
     */
    @Override
    public Exception getDiskCacheException() {
        final String methodName = "getDiskCacheException()";
        Exception ex = null;
        if (this.swapToDisk) {
            // TODO write code to support getDiskCacheException function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return ex;
    }

    /**
     * Returns a hashcode of all the cache entries in disk cache. If includeValue
     * is true, hashcode will include the cache value. lease note that the cache key
     * and value objects should override the default java.lang.Object.hashCode()
     * method to get semantic comparability between object instances.
     * Since this command computes a hash of the cached objects on disk, this is
     * an CPU and I/O intensive operation.
     *
     * @param debug If debug is true, a list of the cache IDs and their hashcodes are written to the SystemOut log.
     * @param includeValue If includeValue is true, the hashcode of value will be in the calculation.
     * @return hashcode of all the cache entries
     */
    @Override
    public int getDiskCacheHashcode(boolean debug, boolean includeValue) throws DynamicCacheException {
        final String methodName = "getDiskCacheHashcode()";
        if (this.swapToDisk) {
            // TODO write code to support getDiskCacheHashcode function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.intf.DCache#getDiskCacheSizeInMBs()
     */
    @Override
    public float getDiskCacheSizeInMBs() {
        final String methodName = "getDiskCacheSizeInMBs()";
        if (this.swapToDisk) {
            // TODO write code to support getDiskCacheSizeInMBs function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.intf.DCache#getEntryDisk(java.lang.Object)
     */
    @Override
    public com.ibm.websphere.cache.CacheEntry getEntryDisk(Object cacheId) {
        final String methodName = "getEntryDisk()";
        CacheEntry ce = null;
        if (this.swapToDisk) {
            // TODO write code to support getEntryDisk function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return ce;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.intf.DCache#getIdsByRangeDisk(int, int)
     */
    @Override
    public Set getIdsByRangeDisk(int index, int length) {
        final String methodName = "getIdsByRangeDisk()";
        Set ids = new HashSet();
        if (this.swapToDisk) {
            // TODO write code to support getIdsByRangeDisk function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return ids;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.intf.DCache#getIdsSizeDisk()
     */
    @Override
    public int getIdsSizeDisk() {
        final String methodName = "getIdsSizeDisk()";
        if (this.swapToDisk) {
            // TODO write code to support getIdsSizeDisk function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.intf.DCache#getPendingRemovalSizeDisk()
     */
    @Override
    public int getPendingRemovalSizeDisk() {
        final String methodName = "getPendingRemovalSizeDisk()";
        if (this.swapToDisk) {
            // TODO write code to support getPendingRemovalSizeDisk function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
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
        final String methodName = "getTemplatesBufferedSizeDisk()";
        if (this.swapToDisk) {
            // TODO write code to support getTemplatesBufferedSizeDisk function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return 0;
    }

    /**
     * Returns a set of templates based on the range and size.
     * WARNING: If index = 1 or -1, the set might contain "DISKCACHE_MORE" to indicate there are more templates on the disk cache.
     * The caller need to remove DISKCACHE_MORE" from the set before it is being used.
     * The "DISKCACHE_MORE" key is defined as HTODDynacache.DISKCACHE_MORE.
     * 
     * @param index If index = 0, it starts the beginning. If index = 1, it means "next". If Index = -1, it means "previous".
     * @param length The max number of templates to be read. If length = -1, it reads all templates until the end.
     * @return The Set of templates.
     */
    @Override
    public Set getTemplatesByRangeDisk(int index, int length) {
        final String methodName = "getTemplatesByRangeDisk()";
        Set ids = new HashSet();
        if (this.swapToDisk) {
            // TODO write code to support getTemplatesByRangeDisk function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return ids;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.intf.DCache#getTemplatesSizeDisk()
     */
    @Override
    public int getTemplatesSizeDisk() {
        final String methodName = "getTemplatesSizeDisk()";
        if (this.swapToDisk) {
            // TODO write code to support getTemplatesSizeDisk function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.intf.DCache#invokeDiskCleanup(boolean)
     */
    @Override
    public void invokeDiskCleanup(boolean scan) {
        final String methodName = "invokeDiskCleanup()";
        if (this.swapToDisk) {
            // TODO write code to support invokeDiskCleanup function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.intf.DCache#isDiskCleanupRunning()
     */
    @Override
    public boolean isDiskCleanupRunning() {
        final String methodName = "isDiskCleanupRunning()";
        boolean diskCleanupRunning = false;
        if (this.swapToDisk) {
            // TODO write code to support isDiskCleanupRunning function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return diskCleanupRunning;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.intf.DCache#isDiskInvalidationBufferFull()
     */
    @Override
    public boolean isDiskInvalidationBufferFull() {
        final String methodName = "isDiskInvalidationBufferFull()";
        boolean diskInvalidationBufferFull = false;
        if (this.swapToDisk) {
            // TODO write code to support isDiskInvalidationBufferFull function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
        return diskInvalidationBufferFull;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.cache.intf.DCache#releaseDiskCacheUnusedPools()
     */
    @Override
    public void releaseDiskCacheUnusedPools() {
        final String methodName = "releaseDiskCacheUnusedPools()";
        if (this.swapToDisk) {
            // TODO write code to support releaseDiskCacheUnusedPools function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
    }

    @Override
    public void setEnableDiskCacheSizeInBytesChecking(boolean enableDiskCacheSizeInBytesChecking) {
        final String methodName = "setEnableDiskCacheSizeInBytesChecking()";
        if (this.swapToDisk) {
            // TODO write code to support setEnableDiskCacheSizeInBytesChecking function
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
            }
        } else {
            if (this.featureSupport.isDiskCacheSupported() == false) {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " no operation is done because the disk cache offload is not enabled");
                }
            }
        }
    }

    @Override
    public void setSwapToDisk(boolean enable) {
        final String methodName = "setSwapToDisk()";
        if (enable == true) {
            if (this.featureSupport.isDiskCacheSupported()) {
                // TODO write code to support setSwapToDisk function
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it is not implemented yet");
                }
                this.swapToDisk = false;
            } else {
                Tr.error(tc, "DYNA1064E", new Object[] { methodName, cacheName, this.cacheProviderName });
                this.swapToDisk = false;
            }
        } else {
            this.swapToDisk = false;
        }
    }

    /*************************************************************************************
     * PMI counters and CacheStatistics Support
     *************************************************************************************/

    /**
     * This method needs to change if cache provider supports PMI counters.
     */
    @Override
    public void refreshCachePerf() {
        /*
         * final String methodName = "refreshCachePerf()";
         * if (tc.isDebugEnabled()) {
         * Tr.debug(tc, methodName + " cacheName=" + cacheName);
         * }
         */
    }

    /**
     * This method needs to change if cache provider supports PMI counters.
     */
    @Override
    public void resetPMICounters() {
        // TODO needs to change if cache provider supports PMI counters.
        final String methodName = "resetPMICounters()";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName);
        }
    }

    /**
     * This method needs to change if cache provider supports PMI and CacheStatisticsListener.
     */
    @Override
    public void updateStatisticsForVBC(com.ibm.websphere.cache.CacheEntry cacheEntry, boolean directive) {
        // TODO needs to change if cache provider supports PMI and CacheStatisticsListener
        final String methodName = "updateStatisticsForVBC()";
        Object id = null;
        if (cacheEntry != null) {
            id = cacheEntry.getIdObject();
        }
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " id=" + id + " directive=" + directive);
        }
    }

    /*************************************************************************************
     * NO-OP methods
     *************************************************************************************/

    /**
     * This method is used only by default cache provider (cache.java). Do nothing.
     */
    @Override
    public void setBatchUpdateDaemon(BatchUpdateDaemon batchUpdateDaemon) {
        final String methodName = "setBatchUpdateDaemon()";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName);
        }
    }

    /**
     * This method is used only by default cache provider (cache.java). Do nothing.
     */
    @Override
    public void addToTimeLimitDaemon(Object id, long expirationTime, int inactivity) {
        final String methodName = "addToTimeLimitDaemon()";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it should not be called");
        }
    }

    /**
     * This method is used only by default cache provider (cache.java). Do nothing.
     */
    @Override
    public void batchUpdate(HashMap invalidateIdEvents, HashMap invalidateTemplateEvents, ArrayList pushEntryEvents) {
        final String methodName = "batchUpdate()";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it should not be called");
        }
    }

    /**
     * This method is used only by default cache provider (cache.java). Return NullRemoteServices.
     */
    @Override
    public RemoteServices getRemoteServices() {
        return new NullRemoteServices();
    }

    /**
     * This method is used only by default cache provider (cache.java). Return false.
     */
    @Override
    public FreeLruEntryResult freeLruEntry() {
        final String methodName = "freeLruEntry()";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it should not be called");
        }
        return new FreeLruEntryResult();
    }

    /**
     * This method is used only by default cache provider (cache.java). Do nothing.
     */
    @Override
    public void setRemoteServices(RemoteServices remoteServices) {
        final String methodName = "setRemoteServices(remoteServices)";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it should not be called");
        }
    }

    /**
     * This method is used only by default cache provider (cache.java). Do nothing.
     */
    @Override
    public void setTimeLimitDaemon(TimeLimitDaemon timeLimitDaemon) {
        final String methodName = "setTimeLimitDaemon(timeLimitDaemon)";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it should not be called");
        }
    }

    /**
     * This method is used only by default cache provider (cache.java). Do nothing.
     */
    @Override
    public void trimCache() {
        final String methodName = "trimCache()";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it should not be called");
        }
    }

    /*
     * PM21179
     */
    @Override
    public void clearMemory(boolean clearDisk) {
        final String methodName = "clearDisk";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it should not be called");
        }
    }

    @Override
    public void setInvalidationAuditDaemon(InvalidationAuditDaemon iad) {
        final String methodName = "setInvalidationAuditDaemon(iad)";
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, methodName + " cacheName=" + cacheName + " ERROR because it should not be called");
        }
    }
}
