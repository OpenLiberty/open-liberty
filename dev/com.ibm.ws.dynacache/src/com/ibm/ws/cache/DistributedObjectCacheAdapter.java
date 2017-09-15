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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.websphere.cache.ChangeListener;
import com.ibm.websphere.cache.DistributedNioMapObject;
import com.ibm.websphere.cache.DistributedObjectCache;
import com.ibm.websphere.cache.InvalidationListener;
import com.ibm.websphere.cache.PreInvalidationListener;
import com.ibm.websphere.cache.exception.DiskCacheEntrySizeOverLimitException;
import com.ibm.websphere.cache.exception.DiskIOException;
import com.ibm.websphere.cache.exception.DiskOffloadNotEnabledException;
import com.ibm.websphere.cache.exception.DiskSizeInEntriesOverLimitException;
import com.ibm.websphere.cache.exception.DiskSizeOverLimitException;
import com.ibm.websphere.cache.exception.DynamicCacheException;
import com.ibm.websphere.cache.exception.MiscellaneousException;
import com.ibm.websphere.cache.exception.SerializationException;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.intf.DCache;
import com.ibm.ws.cache.intf.DCacheConfig;
import com.ibm.ws.cache.stat.CachePerf;
import com.ibm.ws.cache.util.ExceptionUtility;
import com.ibm.ws.cache.util.ValidateUtility;

public class DistributedObjectCacheAdapter extends DistributedObjectCache implements com.ibm.websphere.cache.CacheLocal {

    //-----------------------------------------------------------------
    // DistributedMapImpl           ( TYPE_DISTRIBUTED_MAP )
    // and
    // DistributedLockingMapImpl    ( TYPE_DISTRIBUTED_LOCKING_MAP )
    //-----------------------------------------------------------------
    protected static final boolean IS_UNIT_TEST = false;
    private static TraceComponent tc = Tr.register(DistributedObjectCacheAdapter.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");
    private static final boolean INCREMENT_REF_COUNT = true;
    private static final boolean SKIP_MEMORY_WRITE_TO_DISK = true;

    private static final Object[] NULL_ALIASIDs = null;
    private static final Object[] NULL_DEPIDs = null;

    protected DCache cache = null;
    protected String cacheName = null;
    protected String myTemplate = null;
    protected int sharingPolicy = EntryInfo.NOT_SHARED;
    protected int timeToLive = -1;
    protected int priority = -1;
    protected EntryInfo.EntryInfoPool entryInfoPool = null;
    protected CacheEntry.CacheEntryPool cacheEntryPool = null;
    protected int mapType = -1;
    //-----------------------------------------------------------------

    //-----------------------------------------------------------------
    // DistributedLockingMapImpl    ( TYPE_DISTRIBUTED_LOCKING_MAP )
    //-----------------------------------------------------------------
    protected String transactionIdPrefix = null;
    protected boolean useLockManager = false;
    protected static final String LOCK_MANAGER_DISABLED = "Lock Manager is disabled";
    protected static final String INVALID_ARGUMENTS = "Invalid arguments";
    //-----------------------------------------------------------------

    //-----------------------------------------------------------------
    // DistributedMapImpl           ( TYPE_DISTRIBUTED_MAP )
    //-----------------------------------------------------------------
    //-----------------------------------------------------------------

    //-----------------------------------------------------------------
    // DistributedNioMapImpl           ( TYPE_DISTRIBUTED_MAP )
    //-----------------------------------------------------------------
    //-----------------------------------------------------------------

    //-----------------------------------------------------------------
    // Adapter data - not exposed to sub-classes
    //-----------------------------------------------------------------
    private DCacheConfig cacheConfig = null;

    //-----------------------------------------------------------------
    // Sub-classes
    //-----------------------------------------------------------------
    // TYPE_DISTRIBUTED_MAP             DistributedMapImpl()
    // TYPE_DISTRIBUTED_LOCKING_MAP     DistributedLockingMapImpl()
    // TYPE_DISTRIBUTED_NIO_MAP         DistributedNioMapImpl()
    //-----------------------------------------------------------------

    //-----------------------------------------------------------------
    // Don't allow a 0-arg CTOR - Force javac error message
    //-----------------------------------------------------------------
    private DistributedObjectCacheAdapter() {}

    protected DistributedObjectCacheAdapter(DCache cache, int mapType) {

        final String methodName = "DistributedObjectCacheAdapter() - CTOR";

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, methodName + " " + cache.getCacheName() + " Cache=" + cache + " CacheConfig=" + cache.getCacheConfig() + " enableReplication="
                         + cache.getCacheConfig().isEnableCacheReplication());

        //---------------------------------------------------------
        // common init
        //---------------------------------------------------------
        this.cache = cache;
        this.cacheName = cache.getCacheName();
        this.cacheConfig = cache.getCacheConfig();
        this.transactionIdPrefix = cacheConfig.getServerNodeName() + "." + cacheConfig.getServerServerName() + ".";
        this.myTemplate = transactionIdPrefix + cacheName;

        setMapType(mapType);
        setSharingPolicy(this.cacheConfig.getDefaultShareType());

        //------------------------------------------------------------
        // sub-class must decide what type/size object and/or pools
        //------------------------------------------------------------
        createMapSpecificObjects();

        //------------------------------------------------------------
        // Feature enable or disable
        //------------------------------------------------------------
        if (entryInfoPool != null) {
            entryInfoPool.setFeatures(cacheConfig);
        }
        //------------------------------------------------------------

        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName + " Adapter Impl=" + this);

        if (tc.isEntryEnabled())
            Tr.exit(tc, methodName + " MapType:" + mapType);
    }

    @Override
    protected void finalize() {
        final String methodName = "finalize";

        destroyMapSpecificObjects();

        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName + " Object destroyed:" + this);
    }

    //--------------------------------------------------------------
    // Sub-classes - Allocate your objects
    //--------------------------------------------------------------
    protected void createMapSpecificObjects() {
        throw new RuntimeException("You must override this method!!");
    }

    protected void destroyMapSpecificObjects() {
        throw new RuntimeException("You must override this method!!");
    }

    //--------------------------------------------------------------

    //--------------------------------------------------------------
    // All cache types inherite the following common public methods
    // All cache types inherite the following common public methods
    // All cache types inherite the following common public methods
    // All cache types inherite the following common public methods
    //--------------------------------------------------------------

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * getMapType - gets the map type for this map.
     *
     * @return returns the current sharing policy of DistributedMap.
     * @see getSharingPolicy
     */
    @Override
    final public int getMapType() {
        return mapType;
    }

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    final public String getTransactionIdPrefix() {
        return transactionIdPrefix;
    }

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    @Override
    final public void setSharingPolicy(int sharingPolicy) {
        ValidateUtility.sharingPolicy(sharingPolicy);
        this.sharingPolicy = sharingPolicy;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "setSharingPolicy() set global sharingPolicy to " + this.sharingPolicy + " for cacheName=" + cache.getCacheName());
        }
    }

    //--------------------------------------------------------------  //WMT begin
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    @Override
    final public void setDRSBootstrap(boolean drsBootstrap) {
        cache.getCacheConfig().setDrsBootstrapEnabled(drsBootstrap);
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "setDRSBootstrap() set DRSBootStrap to " + drsBootstrap + " for cacheName=" + cache.getCacheName());
        }
    } //WMT end

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    @Override
    final public boolean isDRSBootstrapEnabled() { //390766
        return cache.getCacheConfig().isDrsBootstrapEnabled();
    }

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    @Override
    final public void setPriority(int priority) {
        ValidateUtility.priority(priority);
        this.priority = priority;
    }

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    @Override
    final public void setTimeToLive(int timeToLive) {
        ValidateUtility.timeToLive(timeToLive);
        this.timeToLive = timeToLive;
        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "setTimeToLive() set global timeToLive to " + timeToLive + " for cacheName=" + cache.getCacheName());
        }
    }

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * getSharingPolicy - gets the sharing policy for this map.
     *
     * @return returns the current sharing policy of DistributedMap.
     * @see getSharingPolicy
     */
    @Override
    final public int getSharingPolicy() {
        return sharingPolicy;
    }

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Returns the number of key-value mappings in this map. If the
     * map contains more than <tt>Integer.MAX_VALUE</tt> elements, returns
     * <tt>Integer.MAX_VALUE</tt>.
     *
     * @return the number of key-value mappings in this map.
     */
    @Override
    final public int size() {
        return cache.getNumberCacheEntries();
    }

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Returns number of key-value mappings in this map.
     *
     * @param includeDiskCache true to get the size of the memory and disk maps; false to get the size of memory map.
     * @return the number of key-value mappings in this map.
     */
    @Override
    public int size(boolean includeDiskCache) {
        int mappings = 0;

        mappings = cache.getNumberCacheEntries();

        if (includeDiskCache) {
            if (cache instanceof CacheProviderWrapper) {
                CacheProviderWrapper cpw = (CacheProviderWrapper) cache;
                if (cpw.featureSupport.isDiskCacheSupported())
                    mappings = mappings + cache.getIdsSizeDisk();
            } else {
                mappings = mappings + cache.getIdsSizeDisk();
            }
        }
        return mappings;
    }

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    @Override
    final public boolean isEmpty() {
        return cache.getNumberCacheEntries() == 0;
    }

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Returns <tt>true</tt> if this map contains no key-value mappings.
     *
     * @param includeDiskCache true to check the memory and disk maps; false to check
     *            the memory map.
     * @return <tt>true</tt> if this map contains no key-value mappings.
     */
    @Override
    public boolean isEmpty(boolean includeDiskCache) {

        boolean isCacheEmpty = false;

        /*
         * Check if the memory cache has entries
         */
        isCacheEmpty = cache.getNumberCacheEntries() == 0;

        /*
         * If the memory cache is not empty then we don't need to check the disk cache.
         * If the cache is an instanceof CacheProviderWrapper then we know it's not dynacache so
         * we need to check if it supports disk caching. If it doesn't then we don't need to check
         * if the disk cache is empty.
         */
        if (includeDiskCache && isCacheEmpty) {
            if (cache instanceof CacheProviderWrapper) {
                if (((CacheProviderWrapper) cache).featureSupport.isDiskCacheSupported()) {
                    isCacheEmpty = cache.getIdsSizeDisk() == 0;
                }
            } else {
                // Not an instanceof CacheProviderWrapper so we know it's dynacache and we know
                // getIdsSizeDisk is implemented.
                isCacheEmpty = cache.getIdsSizeDisk() == 0;
            }
        }
        return isCacheEmpty;
    }

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.
     *
     * @param key key whose presence in this map is to be tested.
     * @param includeDiskCache true to check the specified key contained in the memory or disk
     *            maps; false to check the specified key contained in the memory map.
     * @return <tt>true</tt> if this map contains a mapping for the specified
     *         key.
     *
     * @throws ClassCastException if the key is of an inappropriate type for
     *             this map.
     * @throws NullPointerException if the key is <tt>null</tt> and this map
     *             does not not permit <tt>null</tt> keys.
     */
    @Override
    final public boolean containsKey(Object key) {
        ValidateUtility.objectNotNull(key, "key");
        //return get(key) != null;
        return cache.containsCacheId(key);
    }

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Returns <tt>true</tt> if this map contains a mapping for the specified
     * key.
     *
     * @param key key whose presence in this map is to be tested.
     * @param includeDiskCache true to check the specified key contained in the memory or
     *            disk maps; false to check the specified key contained in the memory map.
     * @return <tt>true</tt> if this map contains a mapping for the specified
     *         key.
     *
     * @throws ClassCastException if the key is of an inappropriate type for
     *             this map.
     * @throws NullPointerException if the key is <tt>null</tt> and this map
     *             does not not permit <tt>null</tt> keys.
     */
    @Override
    public boolean containsKey(Object key, boolean includeDiskCache) {
        ValidateUtility.objectNotNull(key, "key");
        boolean found = cache.containsCacheId(key);
        if (found) {
            return found;
        }
        if (includeDiskCache) {
            if (cache instanceof CacheProviderWrapper) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "containKey instanceof CacheProviderWrapper");
                CacheProviderWrapper cpw = (CacheProviderWrapper) cache;
                if (cpw.featureSupport.isDiskCacheSupported())
                    found = cache.containsKeyDisk(key);
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "containKey not instanceof CacheProviderWrapper");
                found = cache.containsKeyDisk(key);
            }
        }
        return found;
    }

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Returns <tt>true</tt> if this map maps one or more keys to the
     * specified value. More formally, returns <tt>true</tt> if and only if
     * this map contains at least one mapping to a value <tt>v</tt> such that
     * <tt>(value==null ? v==null : value.equals(v))</tt>. This operation
     * will probably require time linear in the map size for most
     * implementations of the <tt>Map</tt> interface.
     *
     * @param value value whose presence in this map is to be tested.
     * @return <tt>true</tt> if this map maps one or more keys to the
     *         specified value.
     */
    @Override
    final public boolean containsValue(Object value) {
        ValidateUtility.objectNotNull(value, "value");
        Enumeration vEnum = cache.getAllIds();
        while (vEnum.hasMoreElements()) {
            Object key = vEnum.nextElement();

            // CPF_CODE_REVIEW - Should this be cache.getEntryFromMemory() ??
            // CPF_CODE_REVIEW - Why do we allow null value to return true??

            Object val = get(key);
            if (val == value || (val != null && val.equals(value)))
                return true;
        }
        return false;
    }

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Copies all of the mappings from the specified map to this map
     * (optional operation). These mappings will replace any mappings that
     * this map had for any of the keys currently in the specified map.
     *
     * @param map Mappings to be stored in this map.
     *
     * @throws UnsupportedOperationException if the <tt>putAll</tt> method is
     *             not supported by this map.
     *
     * @throws ClassCastException if the class of a key or value in the
     *             specified map prevents it from being stored in this map.
     *
     * @throws IllegalArgumentException some aspect of a key or value in the
     *             specified map prevents it from being stored in this map.
     *
     * @throws NullPointerException this map does not permit <tt>null</tt>
     *             keys or values, and the specified key or value is
     *             <tt>null</tt>.
     */
    @Override
    final public void putAll(Map map) {
        ValidateUtility.objectNotNull(map, "map");
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry e = (Map.Entry) it.next();
            put(e.getKey(), e.getValue());
        }
    }

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Compares the specified object with this map for equality. Returns
     * <tt>true</tt> if the given object is also a map and the two Maps
     * represent the same mappings. More formally, two maps <tt>t1</tt> and
     * <tt>t2</tt> represent the same mappings if
     * <tt>t1.entrySet().equals(t2.entrySet())</tt>. This ensures that the
     * <tt>equals</tt> method works properly across different implementations
     * of the <tt>Map</tt> interface.
     *
     * @param o object to be compared for equality with this map.
     * @return <tt>true</tt> if the specified object is equal to this map.
     */
    @Override
    final public boolean equals(Object o) {
        return o == this;
    }

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Returns the hash code value for this map. The hash code of a map
     * is defined to be the sum of the hashCodes of each entry in the map's
     * entrySet view. This ensures that <tt>t1.equals(t2)</tt> implies
     * that <tt>t1.hashCode()==t2.hashCode()</tt> for any two maps
     * <tt>t1</tt> and <tt>t2</tt>, as required by the general
     * contract of Object.hashCode.
     *
     * @return the hash code value for this map.
     * @see Map.Entry#hashCode()
     * @see Object#hashCode()
     * @see Object#equals(Object)
     * @see #equals(Object)
     */
    @Override
    final public int hashCode() {
        return super.hashCode();
    }

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Removes all mappings from this map (optional operation).
     *
     * @throws UnsupportedOperationException clear is not supported by this
     *             map.
     */
    @Override
    final public void clear() {
        boolean waitOnInvalidation = true;
        cache.clear(waitOnInvalidation);
    }

    //--------------------------------------------------------------
    // Common public method - not extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * PM21179
     * Removes all mappings from this map (optional operation) and optionally
     * the disk cache.
     *
     * @throws UnsupportedOperationException clear is not supported by this
     *             map.
     */
    @Override
    public void clearMemory(boolean clearDisk) {
        cache.clearMemory(clearDisk);
    }

    //--------------------------------------------------------------
    // Any cache types may choose to activate following public methods
    // Any cache types may choose to activate following public methods
    // Any cache types may choose to activate following public methods
    // Any cache types may choose to activate following public methods
    //--------------------------------------------------------------

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Returns the value to which this map maps the specified key. Returns
     * <tt>null</tt> if the map contains no mapping for this key. A return
     * value of <tt>null</tt> does not <i>necessarily</i> indicate that the
     * map contains no mapping for the key; it's also possible that the map
     * explicitly maps the key to <tt>null</tt>. The <tt>containsKey</tt>
     * operation may be used to distinguish these two cases.
     *
     * @param key key whose associated value is to be returned.
     * @return the value to which this map maps the specified key, or
     *         <tt>null</tt> if the map contains no mapping for this key.
     *
     * @throws ClassCastException if the key is not of an inappropriate type for
     *             this map. (Currently supports only String)
     * @throws NullPointerException key is <tt>null</tt> and this map does not
     *             not permit <tt>null</tt> keys.
     *
     * @see #containsKey(Object)
     */
    @Override
    public Object get(Object key) {
        final String methodName = "get(key)";
        return functionNotAvailable(methodName);
    }

    // Used by subclasses
    final protected Object common_get(Object key) {
        ValidateUtility.objectNotNull(key, "key");
        Object value = cache.getValue(key, cache.shouldPull(this.sharingPolicy, key));
        return value;
    }

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Returns the value to which this map maps the specified key. Returns
     * <tt>null</tt> if the map contains no mapping for this key. A return
     * value of <tt>null</tt> does not <i>necessarily</i> indicate that the
     * map contains no mapping for the key; it's also possible that the map
     * explicitly maps the key to <tt>null</tt>. The <tt>containsKey</tt>
     * operation may be used to distinguish these two cases.
     *
     * @param key key whose associated value is to be returned.
     * @return the value to which this map maps the specified key, or
     *         <tt>null</tt> if the map contains no mapping for this key.
     *
     * @throws ClassCastException if the key is not of an inappropriate type for
     *             this map. (Currently supports only String)
     * @throws NullPointerException key is <tt>null</tt> and this map does not
     *             not permit <tt>null</tt> keys.
     *
     * @see #containsKey(Object)
     */
    @Override
    public com.ibm.websphere.cache.CacheEntry getCacheEntry(Object key) {
        final String methodName = "getCacheEntry(key)";
        return (com.ibm.websphere.cache.CacheEntry) functionNotAvailable(methodName);
    }

    // Used by subclasses - WARNING - caller must release CE ( ce.finish() )
    final protected com.ibm.websphere.cache.CacheEntry common_getCacheEntry(Object key) {
        final String methodName = "getCacheEntry(key)";
        ValidateUtility.objectNotNull(key, "key");
        EntryInfo ei = entryInfoPool.allocate(key, NULL_DEPIDs, NULL_ALIASIDs);
        ei.setSharingPolicy(this.sharingPolicy);
        com.ibm.websphere.cache.CacheEntry cacheEntry = cache.getEntry(ei, true);
        ei.returnToPool();
        if (tc.isDebugEnabled()) {
            if (cacheEntry != null) {
                Tr.debug(tc, methodName + " " + cache.getCacheName() + " id=" + cacheEntry.getIdObject());
            }
        }
        return cacheEntry;
    }

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * JavaDoc is in DistributedNioMap.
     *
     * @param key
     * @param value
     * @param userMetaData
     * @param priority
     * @param timeToLive
     * @param sharingPolicy
     * @param dependencyIds
     * @param alias
     * @return CacheEntry
     */
    @Override
    public void put(
                    Object key,
                    Object value,
                    Object userMetaData,
                    int priority,
                    int timeToLive,
                    int sharingPolicy,
                    Object dependencyIds[],
                    Object alias[]) {
        final String methodName = "put(key,value,userMetaData...)";
        functionNotAvailable(methodName);
    }

    @Override
    public void put(
                    Object key,
                    Object value,
                    Object userMetaData,
                    int priority,
                    int timeToLive,
                    int inactivityTime,
                    int sharingPolicy,
                    Object dependencyIds[],
                    Object alias[]) {
        final String methodName = "put(key,value,userMetaData...)";
        functionNotAvailable(methodName);
    }

    @Override
    public void put(
                    Object key,
                    Object value,
                    Object userMetaData,
                    int priority,
                    int timeToLive,
                    int inactivityTime,
                    int sharingPolicy,
                    Object dependencyIds[],
                    Object alias[],
                    boolean skipMemoryAndWriteToDisk) throws DynamicCacheException {
        final String methodName = "put(key,value,userMetaData...)";
        functionNotAvailable(methodName);
    }

    // used by subclass DistributedNioMap
    @Override
    public com.ibm.websphere.cache.CacheEntry putAndGet(
                                                        Object key,
                                                        Object value,
                                                        Object userMetaData,
                                                        int priority,
                                                        int timeToLive,
                                                        int sharingPolicy,
                                                        Object dependencyIds[],
                                                        Object alias[]) {
        final String methodName = "putAndGet(key,value,userMetaData...)";
        return (com.ibm.websphere.cache.CacheEntry) functionNotAvailable(methodName);
    }

    @Override
    public com.ibm.websphere.cache.CacheEntry putAndGet(
                                                        Object key,
                                                        Object value,
                                                        Object userMetaData,
                                                        int priority,
                                                        int timeToLive,
                                                        int inactivityTime,
                                                        int sharingPolicy,
                                                        Object dependencyIds[],
                                                        Object alias[]) {
        final String methodName = "putAndGet(key,value,userMetaData...)";
        return (com.ibm.websphere.cache.CacheEntry) functionNotAvailable(methodName);
    }

    // used by subclass DistributedNioMap
    final protected void common_put(
                                    Object key,
                                    Object value,
                                    Object userMetaData,
                                    int priority,
                                    int timeToLive,
                                    int sharingPolicy,
                                    Object dependencyIds[],
                                    Object aliasIds[]) {
        try {
            internal_putAndGet(key, value, userMetaData, priority, timeToLive, -1, sharingPolicy, dependencyIds, aliasIds, !INCREMENT_REF_COUNT, !SKIP_MEMORY_WRITE_TO_DISK);
        } catch (DynamicCacheException ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.DistribtedObjectCacheAdapter", "669", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "common_put - exception: " + ExceptionUtility.getStackTrace(ex));
        }
    }

    // used by subclass DistributedNioMap
    final protected void common_put(
                                    Object key,
                                    Object value,
                                    Object userMetaData,
                                    int priority,
                                    int timeToLive,
                                    int inactivityTime,
                                    int sharingPolicy,
                                    Object dependencyIds[],
                                    Object aliasIds[]) {
        try {
            internal_putAndGet(key, value, userMetaData, priority, timeToLive, inactivityTime, sharingPolicy, dependencyIds, aliasIds, !INCREMENT_REF_COUNT,
                               !SKIP_MEMORY_WRITE_TO_DISK);
        } catch (DynamicCacheException ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.DistribtedObjectCacheAdapter", "690", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "common_put - exception: " + ExceptionUtility.getStackTrace(ex));
        }
    }

    // used by subclass DistributedNioMap
    final protected void common_put(
                                    Object key,
                                    Object value,
                                    Object userMetaData,
                                    int priority,
                                    int timeToLive,
                                    int inactivityTime,
                                    int sharingPolicy,
                                    Object dependencyIds[],
                                    Object aliasIds[],
                                    boolean skipMemoryAndWriteToDisk) throws DynamicCacheException {
        internal_putAndGet(key, value, userMetaData, priority, timeToLive, inactivityTime, sharingPolicy, dependencyIds, aliasIds, !INCREMENT_REF_COUNT, skipMemoryAndWriteToDisk);
    }

    // used by subclass DistributedNioMap
    final protected com.ibm.websphere.cache.CacheEntry common_putAndGet(
                                                                        Object key,
                                                                        Object value,
                                                                        Object userMetaData,
                                                                        int priority,
                                                                        int timeToLive,
                                                                        int sharingPolicy,
                                                                        Object dependencyIds[],
                                                                        Object aliasIds[]) {

        try {
            return internal_putAndGet(key, value, userMetaData, priority, timeToLive, -1, sharingPolicy, dependencyIds, aliasIds, INCREMENT_REF_COUNT, !SKIP_MEMORY_WRITE_TO_DISK);
        } catch (DynamicCacheException ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.DistribtedObjectCacheAdapter", "724", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "common_putAndGet - exception: " + ExceptionUtility.getStackTrace(ex));
        }
        return null;
    }

    final protected com.ibm.websphere.cache.CacheEntry common_putAndGet(
                                                                        Object key,
                                                                        Object value,
                                                                        Object userMetaData,
                                                                        int priority,
                                                                        int timeToLive,
                                                                        int inactivityTime,
                                                                        int sharingPolicy,
                                                                        Object dependencyIds[],
                                                                        Object aliasIds[]) {

        try {
            return internal_putAndGet(key, value, userMetaData, priority, timeToLive, inactivityTime, sharingPolicy, dependencyIds, aliasIds, INCREMENT_REF_COUNT,
                                      !SKIP_MEMORY_WRITE_TO_DISK);
        } catch (DynamicCacheException ex) {
            com.ibm.ws.ffdc.FFDCFilter.processException(ex, "com.ibm.ws.cache.DistribtedObjectCacheAdapter", "745", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "common_putAndGet - exception: " + ExceptionUtility.getStackTrace(ex));
        }
        return null;
    }

    // used by subclass DistributedNioMap
    final private com.ibm.websphere.cache.CacheEntry internal_putAndGet(
                                                                        Object key,
                                                                        Object value,
                                                                        Object userMetaData,
                                                                        int priority,
                                                                        int timeToLive,
                                                                        int inactivityTime,
                                                                        int sharingPolicy,
                                                                        Object dependencyIds[],
                                                                        Object aliasIds[],
                                                                        boolean incrementRefCount,
                                                                        boolean skipMemoryAndWriteToDisk) throws DynamicCacheException {

        final String methodName = "putAndGet(..)";
        ValidateUtility.objectNotNull(key, "key", value, "value");
        if (skipMemoryAndWriteToDisk && !cache.getSwapToDisk()) {
            throw new DiskOffloadNotEnabledException("DiskOffloadNotEnabledException occurred. The disk offload feature for cache instance \"" + cache.getCacheName()
                                                     + "\" is not enabled.");
        }
        if (skipMemoryAndWriteToDisk == true && sharingPolicy != EntryInfo.NOT_SHARED) {
            //DYNA1072W=DYNA1072W: The cache id \"{0}\" will not be replicated to other servers because \"skipMemoryWriteToDisk\" is set to true. The sharing policy will be set to \"not-shared\".
            Tr.warning(tc, "DYNA1072W", new Object[] { key });
            sharingPolicy = EntryInfo.NOT_SHARED;
        }
        ValidateUtility.sharingPolicy(sharingPolicy);
        if (sharingPolicy == EntryInfo.SHARED_PUSH_PULL ||
            sharingPolicy == EntryInfo.SHARED_PULL) {
            invalidate(key, true);
        }
        EntryInfo ei = entryInfoPool.allocate(key, dependencyIds, aliasIds);
        ei.setUserMetaData(userMetaData);
        ei.setPriority(priority);
        ei.setTimeLimit(timeToLive);
        ei.setSharingPolicy(sharingPolicy);
        ei.setInactivity(inactivityTime);
        CacheEntry ce = cacheEntryPool.allocate();
        ce.copyMetaData(ei);
        ce.setValue(value);
        ce.setSkipMemoryAndWriteToDisk(skipMemoryAndWriteToDisk);
        if (value instanceof DistributedNioMapObject) {
            ce.useByteBuffer = true;
        }
        // only put and no get for when skipMemoryAndWriteToDisk is true ==> refcount will not increment
        com.ibm.websphere.cache.CacheEntry newEntry = cache.setEntry(ce, CachePerf.LOCAL, false, Cache.COORDINATE, incrementRefCount);
        ei.returnToPool();

        try {

            if (cache.getCacheConfig().isDefaultCacheProvider()) {

                if (tc.isDebugEnabled()) {
                    if (newEntry != null) {
                        Tr.debug(tc, methodName + " " + cache.getCacheName() + " id=" + newEntry.getIdObject() + " incRefCount=" + incrementRefCount +
                                     " skipMemoryWriteToDisk=" + skipMemoryAndWriteToDisk);
                    }
                }

                if (ce.skipMemoryAndWriteToDisk) {
                    int errorCode = ce.skipMemoryAndWriteToDiskErrorCode;
                    StringBuffer message = new StringBuffer();
                    Exception ex = null;
                    switch (errorCode) {
                        case HTODDynacache.NO_EXCEPTION:
                            break;
                        case HTODDynacache.DISK_EXCEPTION:
                            message.append("The disk IO exception has occurred when writing cache ID: ");
                            message.append(ce.id);
                            message.append(" to the disk cache. ");
                            ex = cache.getDiskCacheException();
                            if (ex != null) {
                                message.append(ex.getMessage());
                            }
                            throw new DiskIOException(message.toString());
                        case HTODDynacache.DISK_SIZE_OVER_LIMIT_EXCEPTION:
                            message.append("Exception has occurred either (1) there is no disk space available, or (2) the disk cache size in GB s over the (diskCacheSizeInGB) limit when writing cache ID: ");
                            message.append(ce.id);
                            message.append(" to the disk cache. ");
                            ex = cache.getDiskCacheException();
                            if (ex != null) {
                                message.append(ex.getMessage());
                            }
                            throw new DiskSizeOverLimitException(message.toString());
                        case HTODDynacache.OTHER_EXCEPTION:
                            message.append("The runtime exception other than a Disk IOException has occurred when writing cache ID: ");
                            message.append(ce.id);
                            message.append(" to the disk cache. ");
                            ex = cache.getDiskCacheException();
                            if (ex != null) {
                                message.append(ex.getMessage());
                            }
                            throw new MiscellaneousException(message.toString());
                        case HTODDynacache.SERIALIZATION_EXCEPTION:
                            message.append("The serialization exception has occurred when writing cache ID: ");
                            message.append(ce.id);
                            message.append(" to the disk cache. ");
                            throw new SerializationException(message.toString());
                        case HTODDynacache.DISK_SIZE_IN_ENTRIES_OVER_LIMIT_EXCEPTION:
                            message.append("The disk cache size in entries is over the (diskCacheSize) limit when writing cache ID: ");
                            message.append(ce.id);
                            message.append(" to the disk cache. ");
                            throw new DiskSizeInEntriesOverLimitException(message.toString());
                        case HTODDynacache.DISK_CACHE_ENTRY_SIZE_OVER_LIMIT_EXCEPTION:
                            message.append("The cache entry size is over the configured disk cache entry size (diskCacheEntrySizeInMB) limit when writing cache ID: ");
                            message.append(ce.id);
                            message.append(" to the disk cache. ");
                            throw new DiskCacheEntrySizeOverLimitException(message.toString());
                    }
                }
            }
        } finally {
            ce.value = ce.serializedValue = null; //prevent ce.reset from Nio object release
            ce.returnToPool();
        }
        return newEntry;
    }

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Adds one or more aliases for the given key in the cache's mapping table. If the alias is already
     * associated with another key, it will be changed to associate with the new key.
     *
     * @param key the key assoicated with alias
     * @param aliasArray the aliases to use for lookups
     * @throws IllegalArgumentException if the key is not in the cache's mapping table.
     */
    @Override
    public void addAlias(Object key, Object[] aliasArray) {
        final String methodName = "addAlias(key, aliasArray)";
        functionNotAvailable(methodName);
    }

    // used by subclasses
    final protected void common_addAlias(Object key, Object[] aliasArray) {
        ValidateUtility.objectNotNull(key, "key", aliasArray, "aliasArray");
        cache.addAlias(
                       key,
                       aliasArray,
                       cache.shouldPull(this.sharingPolicy, key),
                       true);
    }

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Removes an alias from the cache's mapping table.
     *
     * @param alias the alias to move out of the cache's mapping table
     */
    @Override
    public void removeAlias(Object alias) {
        final String methodName = "removeAlias( alias )";
        functionNotAvailable(methodName);
    }

    // used by subclasses
    final protected void common_removeAlias(Object alias) {
        ValidateUtility.objectNotNull(alias, "alias");
        cache.removeAlias(
                          alias,
                          cache.shouldPull(this.sharingPolicy, alias),
                          true);
    }

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * For now, use this to release LRU cache entry (regular objects or ByteByffers/MetaData)
     *
     * @param numOfEntries the number of cache entries to be released
     */
    @Override
    public void releaseLruEntries(int numOfEntries) {
        final String methodName = "releaseLruEntries(numOfEntries)";
        functionNotAvailable(methodName);
    }

    // used by subclasses
    final protected void common_releaseLruEntries(int numOfEntries) {
        if (numOfEntries > 0) {
            int entriesRemoved = 0;
            for (int i = 0; i < numOfEntries; i++) {
                // free up one entry
                FreeLruEntryResult result = cache.freeLruEntry();
                if (result.success == false) {
                    break;
                }
                entriesRemoved++;
            }
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, "releaseLruEntries(): cacheName=" + this.cacheName +
                             " entriesToRemove=" + numOfEntries +
                             " entriesRemoved=" + entriesRemoved);
            }
        }
    }

    // --------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Associates the specified value with the specified key in this map
     * (optional operation). If the map previously contained a mapping for
     * this key, the old value is replaced. This method will use optional
     * metadata looked up from the cache configuration file. If no meta data
     * is found, the entrie is cached with an infinite timeout and the cache's
     * default priority.
     *
     * Metadata found in the cache configuration is looked up via class name
     * and includes priority, timeout, and dependency ids.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key from the local map, or <tt>null</tt>
     *         if there was no mapping for key in the local map. A <tt>null</tt> return can
     *         also indicate that the map previously associated <tt>null</tt>
     *         with the specified key, if the implementation supports
     *         <tt>null</tt> values.
     *
     * @throws UnsupportedOperationException if the <tt>put</tt> operation is
     *             not supported by this map.
     * @throws ClassCastException if the class of the specified key or value
     *             prevents it from being stored in this map.
     * @throws IllegalArgumentException if some aspect of this key or value
     *             prevents it from being stored in this map.
     * @throws NullPointerException this map does not permit <tt>null</tt>
     *             keys or values, and the specified key or value is
     *             <tt>null</tt>.
     */
    @Override
    public Object put(Object key, Object value) {
        final String methodName = "put(key, value)";
        return functionNotAvailable(methodName);
    }

    // Used by subclasses
    final protected Object common_put(Object key, Object value) {
        EntryInfo ei = entryInfoPool.allocate(key, DistributedObjectCacheAdapter.NULL_DEPIDs, DistributedObjectCacheAdapter.NULL_DEPIDs);
        ei.setTimeLimit(this.timeToLive);
        ei.setSharingPolicy(this.sharingPolicy);
        Object retValue = cache.invalidateAndSet(ei, value, cache.getCacheConfig().isEnableCacheReplication());

        ei.returnToPool();
        return retValue;
    }

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Associates the specified value with the specified key in this map
     * (optional operation). If the map previously contained a
     * mapping for this key, the old value is replaced.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @param priority the priority value for the cache entry. entries
     *            with higher priority will remain in the cache longer
     *            than those with a lower priority in the case of cache
     *            overflow.
     * @param timeToLive the time in seconds that the cache entry should remain
     *            in the cache
     * @param sharingPolicy how the cache entry should be shared in a cluster.
     *            values are EntryInfo.NOT_SHARED, EntryInfo.SHARED_PUSH,
     *            and EntryInfo.SHARED_PUSH_PULL.
     * @param dependencyIds an optional set of dependency ids to associate with
     *            the cache entry
     * @return previous value associated with specified key from the local map, or <tt>null</tt>
     *         if there was no mapping for the key in the local map. A <tt>null</tt> return can
     *         also indicate that the map previously associated <tt>null</tt>
     *         with the specified key, if the implementation supports
     *         <tt>null</tt> values.
     *
     * @throws UnsupportedOperationException if the <tt>put</tt> operation is
     *             not supported by this map.
     * @throws ClassCastException if the class of the specified key or value
     *             prevents it from being stored in this map.
     * @throws IllegalArgumentException if some aspect of this key or value
     *             prevents it from being stored in this map.
     * @throws NullPointerException this map does not permit <tt>null</tt>
     *             keys or values, and the specified key or value is
     *             <tt>null</tt>.
     */
    @Override
    public Object put(Object key, Object value, int priority, int timeToLive, int sharingPolicy, Object dependencyIds[]) {
        final String methodName = "put(key, value, priority, timeToLive, sharingPolicy, dependencyIds)";
        return functionNotAvailable(methodName);
    }

    // Used by subclasses
    protected Object common_put(Object key, Object value, int priority, int timeToLive, int sharingPolicy, Object dependencyIds[]) {
        return common_put(key, value, priority, timeToLive, -1, sharingPolicy, dependencyIds);
    }

    @Override
    public Object put(Object key, Object value, int priority, int timeToLive, int inactivityTime, int sharingPolicy, Object dependencyIds[]) {
        final String methodName = "put(key, value, priority, timeToLive, inactivityTime, sharingPolicy, dependencyIds)";
        return functionNotAvailable(methodName);
    }

    // Used by subclasses
    protected Object common_put(Object key, Object value, int priority, int timeToLive, int inactivityTime, int sharingPolicy, Object dependencyIds[]) {
        EntryInfo ei = entryInfoPool.allocate(key, dependencyIds, DistributedObjectCacheAdapter.NULL_ALIASIDs);
        ei.setPriority(priority);
        ei.setTimeLimit(timeToLive);
        if (inactivityTime != -1) {
            ei.setInactivity(inactivityTime);
        }
        ei.setSharingPolicy(sharingPolicy);

        Object retValue = cache.invalidateAndSet(ei, value, cache.getCacheConfig().isEnableCacheReplication());

        ei.returnToPool();
        return retValue;
    }

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * invalidate - invalidates the given key. If the key is
     * for a specific cache entry, then only that object is
     * invalidated. If the key is for a dependency id, then
     * all objects that share that dependency id will be
     * invalidated. This method waits on invalidation to complete.
     *
     * @param key the key which will be invalidated
     * @see remove
     */
    @Override
    public void invalidate(Object key) {
        final String methodName = "invalidate(key)";
        functionNotAvailable(methodName);
    }

    // Used by subclasses
    protected void common_invalidate(Object key) {
        ValidateUtility.objectNotNull(key, "key");
        cache.invalidateById(key, true);
    }

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * invalidate - invalidates the given key. If the key is
     * for a specific cache entry, then only that object is
     * invalidated. If the key is for a dependency id, then
     * all objects that share that dependency id will be
     * invalidated.
     *
     * @param key the key which will be invalidated
     * @param wait if true, then the method will not complete until the invalidation
     *            has occured. if false, then the invalidation will occur in batch mode
     * @see remove
     */
    @Override
    public void invalidate(Object key, boolean wait) {
        final String methodName = "invalidate(key, wait)";
        functionNotAvailable(methodName);
    }

    // Used by subclasses
    protected void common_invalidate(Object key, boolean waitOnInvalidation) {
        ValidateUtility.objectNotNull(key, "key");
        cache.invalidateById(key, waitOnInvalidation);
    }

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * invalidate - invalidates the given key. If the key is
     * for a specific cache entry, then only that object is
     * invalidated. If the key is for a dependency id, then
     * all objects that share that dependency id will be
     * invalidated.
     *
     * @param key the key which will be invalidated
     * @param wait if true, then the method will not complete until the invalidation
     *            has occured. if false, then the invalidation will occur in batch mode
     * @see remove
     */
    @Override
    public void invalidate(Object key, boolean wait, boolean checkPreInvalidationListener) {
        final String methodName = "invalidate(key, wait, checkPreInvalidationListener)";
        functionNotAvailable(methodName);
    }

    // Used by subclasses
    protected void common_invalidate(Object key, boolean waitOnInvalidation, boolean checkPreInvalidationListener) {
        ValidateUtility.objectNotNull(key, "key");
        cache.invalidateById(key, waitOnInvalidation, checkPreInvalidationListener);
    }

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Removes the mapping for this key from this map if present (optional
     * operation).
     *
     * @param key key whose mapping is to be removed from the map.
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key. A <tt>null</tt> return can
     *         also indicate that the map previously associated <tt>null</tt>
     *         with the specified key, if the implementation supports
     *         <tt>null</tt> values.
     * @throws UnsupportedOperationException if the <tt>remove</tt> method is
     *             not supported by this map.
     */
    @Override
    public Object remove(Object key) {
        final String methodName = "remove(key)";
        return functionNotAvailable(methodName);
    }

    // Used by subclasses
    protected Object common_remove(Object key) {

        // CPF_CODE_REVIEW - Should this be cache.getEntryFromMemory() ??
        // CPF_CODE_REVIEW - Since we are going to invalidate anyway,
        // why go to the remote??

        ValidateUtility.objectNotNull(key, "key");
        Object old = get(key);
        cache.invalidateById(key, true);
        return old;
    }

    /**
     * Returns a set view of the keys contained in this map. The set is
     * backed by the map, so changes to the map are reflected in the set, and
     * vice-versa. If the map is modified while an iteration over the set is
     * in progress, the results of the iteration are undefined. The set
     * supports element removal, which removes the corresponding mapping from
     * the map, via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt> <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the add or <tt>addAll</tt> operations.
     *
     * @return a set view of the keys contained in this map.
     */
    @Override
    public Set keySet() {
        final String methodName = "keySet()";
        functionNotAvailable(methodName);
        return null;
    }

    // Used by subclasses
    protected Set common_keySet() {
        return cache.getCacheIds();
    }

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Returns a set view of the keys contained in this map. The set is
     * backed by the map, so changes to the map are reflected in the set, and
     * vice-versa. If the map is modified while an iteration over the set is
     * in progress, the results of the iteration are undefined. The set
     * supports element removal, which removes the corresponding mapping from
     * the map, via the <tt>Iterator.remove</tt>, <tt>Set.remove</tt>,
     * <tt>removeAll</tt> <tt>retainAll</tt>, and <tt>clear</tt> operations.
     * It does not support the add or <tt>addAll</tt> operations.
     *
     * @param includeDiskCache true to get keys contained in the memory and disk maps; false to get keys contained in the memory map.
     * @return a set view of the keys contained in this map.
     */
    @Override
    public Set keySet(boolean includeDiskCache) {
        final String methodName = "keySet(boolean)";
        functionNotAvailable(methodName);
        return null;
    }

    // Used by subclasses
    protected Set common_keySet(boolean includeDiskCache) {
        Set cacheIds = cache.getCacheIds();

        /*
         * If the cache is an instanceof CacheProviderWrapper then we know it's not dynacache so
         * we need to check if it supports disk caching. If it doesn't then we don't need incude keys
         * from the disk cache.
         */
        if (cache instanceof CacheProviderWrapper)
            includeDiskCache = ((CacheProviderWrapper) cache).featureSupport.isDiskCacheSupported();

        if (includeDiskCache && cache.getIdsSizeDisk() > 0) {
            int index = 0;
            boolean more = true;
            Set cacheIdsDisk = null;
            do {
                cacheIdsDisk = cache.getIdsByRangeDisk(index, 100);
                if (cacheIdsDisk != null) {
                    cacheIds.addAll(cacheIdsDisk);
                    if (!cacheIdsDisk.contains(HTODDynacache.DISKCACHE_MORE)) {
                        more = false;
                    }
                } else {
                    more = false;
                }
                index = 1;
            } while (more == true);
            cacheIds.remove(HTODDynacache.DISKCACHE_MORE);
        }
        return cacheIds;
    }

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Returns a collection view of the values contained in this map. The
     * collection is backed by the map, so changes to the map are reflected in
     * the collection, and vice-versa. If the map is modified while an
     * iteration over the collection is in progress, the results of the
     * iteration are undefined. The collection supports element removal,
     * which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Collection.remove</tt>,
     * <tt>removeAll</tt>, <tt>retainAll</tt> and <tt>clear</tt> operations.
     * It does not support the add or <tt>addAll</tt> operations.
     *
     * @return a collection view of the values contained in this map.
     */
    @Override
    public Collection values() {
        final String methodName = "values()";
        functionNotAvailable(methodName);
        return null;
    }

    // Used by subclasses
    final protected Collection common_values() {
        //TODO: make efficient
        ArrayList<Object> al = new ArrayList<Object>();
        Enumeration vEnum = cache.getAllIds();
        while (vEnum.hasMoreElements())
            al.add(get(vEnum.nextElement()));
        return al;
    }

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * Returns a set view of the mappings contained in this map. Each element
     * in the returned set is a <tt>Map.Entry</tt>. The set is backed by the
     * map, so changes to the map are reflected in the set, and vice-versa.
     * If the map is modified while an iteration over the set is in progress,
     * the results of the iteration are undefined. The set supports element
     * removal, which removes the corresponding mapping from the map, via the
     * <tt>Iterator.remove</tt>, <tt>Set.remove</tt>, <tt>removeAll</tt>,
     * <tt>retainAll</tt> and <tt>clear</tt> operations. It does not support
     * the <tt>add</tt> or <tt>addAll</tt> operations.
     *
     * @return a set view of the mappings contained in this map.
     */
    @Override
    public Set entrySet() {
        final String methodName = "values()";
        functionNotAvailable(methodName);
        return null;
    }

    // Used by subclasses
    final protected Set common_entrySet() {
        HashSet hs = new HashSet();
        Enumeration vEnum = cache.getAllIds();
        while (vEnum.hasMoreElements()) {
            Object key = vEnum.nextElement();
            hs.add(new DMIEntry(key, get(key)));
        }
        return hs;
    }

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * enableListener - enable or disable the invalidation and change listener support.
     * You must call enableListener(true) before calling addInvalidationListner() or addChangeListener().
     *
     * @param enable - true to enable support for invalidation and change listeners
     *            or false to disable support for invalidation and change listeners
     * @return boolean "true" means listener support was successfully enabled or disabled.
     *         "false" means this DistributedMap is configurated to use the listener's J2EE context for
     *         event notification and the callback registration failed. In this case, the caller's thread
     *         context will be used for event notification.
     *
     */
    @Override
    public boolean enableListener(boolean enable) {
        final String methodName = "enableListener(enable)";
        functionNotAvailable(methodName);
        return false;
    }

    // Used by subclasses
    final protected boolean common_enableListener(boolean enable) {
        return cache.enableListener(enable);
    }

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * addInvalidationListener - adds an invalidation listener for this DistributeMap.
     *
     * @param listener the invalidation listener object
     * @return boolean "true" means the invalidation listener was successfully added.
     *         "false" means either the passed listener object is null or listener support is not enable.
     * @see #removeInvalidationListener(com.ibm.websphere.cache.InvalidationListener)
     */
    @Override
    public boolean addInvalidationListener(InvalidationListener listener) {
        final String methodName = "addInvalidationListener(listener)";
        functionNotAvailable(methodName);
        return false;
    }

    // Used by subclasses
    final protected boolean common_addInvalidationListener(InvalidationListener listener) {
        if (listener != null)
            return this.cache.addInvalidationListener(listener);
        return false;
    }

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * removeInvalidationListener - removes an invalidation listener for this DistributedMap.
     *
     * @param listener the invalidation listener object
     * @return boolean "true" means the invalidation listener was successfully removed.
     *         "false" means either passed listener object is null or listener support is not enable.
     * @see #addInvalidationListener(com.ibm.websphere.cache.InvalidationListener)
     */
    @Override
    public boolean removeInvalidationListener(InvalidationListener listener) {
        final String methodName = "removeInvalidationListener(listener)";
        functionNotAvailable(methodName);
        return false;
    }

    // Used by subclasses
    final protected boolean common_removeInvalidationListener(InvalidationListener listener) {
        if (listener != null)
            return this.cache.removeInvalidationListener(listener);
        return false;
    }

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * addChangeListener - adds a change listener for this DistributedMap.
     *
     * @param listener the change listener object
     * @return boolean "true" means the change listener was successfully added.
     *         "false" means either the passed listener object is null or listener support is not enable.
     * @see #removeChangeListener(com.ibm.websphere.cache.ChangeListener)
     */
    @Override
    public boolean addChangeListener(ChangeListener listener) {
        final String methodName = "addChangeListener(listener)";
        functionNotAvailable(methodName);
        return false;
    }

    // Used by subclasses
    final protected boolean common_addChangeListener(ChangeListener listener) {
        if (listener != null)
            return this.cache.addChangeListener(listener);
        return false;
    }

    // todo in V6.1
    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    ///**
    // * This method will release all resources associated
    // * with this map.  Once a map is destroyed you can
    // * no longer use it.
    // */
    //public boolean destroy(){
    //      final String methodName = "destroy()";
    //      functionNotAvailable(methodName);
    //      return false;
    //}

    //--------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    //--------------------------------------------------------------
    /**
     * removeChangeListener - removes a change listener for this DistributedMap.
     *
     * @param listener the change listener object
     * @return boolean "true" means the change listener was successfully removed.
     *         "false" means either passed listener object is null or listener support is not enable.
     * @see #addChangeListener(com.ibm.websphere.cache.ChangeListener)
     */
    @Override
    public boolean removeChangeListener(ChangeListener listener) {
        final String methodName = "removeChangeListener(listener)";
        functionNotAvailable(methodName);
        return false;
    }

    // Used by subclasses
    final protected boolean common_removeChangeListener(ChangeListener listener) {
        if (listener != null)
            return this.cache.removeChangeListener(listener);
        return false;
    }

    /**
     * addPreInvalidationListener - adds a pre-invalidation listener for
     * this map.
     *
     * @param listener
     *            the pre-invalidation listener object
     * @return boolean "true" means the pre-invalidation listener was
     *         successfully added. "false" means either the passed listener
     *         object is null or listener support is not enable.
     * @see #removePreInvalidationListener(com.ibm.websphere.cache.PreInvalidationListener)
     */
    public boolean addPreInvalidationListener(PreInvalidationListener listener) {
        final String methodName = "addPreInvalidationListener(listener)";
        functionNotAvailable(methodName);
        return false;
    }

    // Used by subclasses
    final protected boolean common_addPreInvalidationListener(
                                                              PreInvalidationListener listener) {
        if (listener != null)
            return this.cache.addPreInvalidationListener(listener);
        return false;
    }

    // --------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    // --------------------------------------------------------------
    /**
     * removePreInvalidationListener - removes a pre-invalidation listener for
     * this DistributedMap.
     *
     * @param listener
     *            the pre-invalidation listener object
     * @return boolean "true" means the pre-invalidation listener was
     *         successfully removed. "false" means either passed listener object
     *         is null or listener support is not enable.
     * @see #addPreInvalidationListener(com.ibm.websphere.cache.PreInvalidationListener)
     */
    @Override
    public boolean removePreInvalidationListener(PreInvalidationListener listener) {
        final String methodName = "removePreInvalidationListener(listener)";
        functionNotAvailable(methodName);
        return false;
    }

    // Used by subclasses
    final protected boolean common_removePreInvalidationListener(
                                                                 PreInvalidationListener listener) {
        if (listener != null)
            return this.cache.removePreInvalidationListener(listener);
        return false;
    }

    // --------------------------------------------------------------
    // Map specific method - extended/implemented by sub-classes
    // --------------------------------------------------------------
    //--------------------------------------------------------------
    // Adapter helper
    //--------------------------------------------------------------
    private void setMapType(int mapType) {
        final String methodName = "setMapType";
        if (tc.isDebugEnabled())
            Tr.debug(tc, methodName + " " + cache.getCacheName() + " MapTypeOld:" + this.mapType + " MapTypeNew:" + mapType);
        this.mapType = mapType;
    }

    //--------------------------------------------------------------
    // Adapter helper
    //--------------------------------------------------------------
    private Object functionNotAvailable(String method) {
        // CPF_TODO - also log a message saying typical cause is config mistake
        if (true)
            throw new RuntimeException(method + " not implemented in " + this);
        return null;
    }

    public DCache getCache() {
        return this.cache;
    }

    class DMIEntry implements Map.Entry {
        Object key;
        Object value;

        DMIEntry(Object key, Object value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public Object getKey() {
            return key;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public Object setValue(Object value) {
            throw new UnsupportedOperationException();
        }
    }

}
