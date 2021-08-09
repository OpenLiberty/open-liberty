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
package com.ibm.websphere.cache;

import com.ibm.websphere.cache.CacheEntry;
import com.ibm.websphere.cache.ChangeListener;
import com.ibm.websphere.cache.InvalidationListener;
import com.ibm.websphere.cache.PreInvalidationListener;
import com.ibm.websphere.cache.exception.*;

/**
 * The DistributedNioMap is a high performance
 * map specifically designed for storing
 * java.nio.Buffer objects.  When a cached object
 * is being removed from cache and the cached
 * object implements the DistributedNioMapObject
 * interface, the DistributedNioMapObject.release()
 * method will be called to notify the cache object
 * it is being removed from cache.
 * @ibm-api
 */
public interface DistributedNioMap {

	/**
	 * Returns the cache entry which maps the specified key. Returns
     * <tt>null</tt> if specified key does not exist
	 * in cache's mapping table or the cache entry is in invalidated state.
     * <br>
     * You must call CacheEntry.finish() when you are finished
     * using the entry.  This will release resources associated
     * with this CacheEntry.
     *
     * @param key key whose associated value is to be returned.
	 * @return the cache entry to which maps the specified key, or
	 *          <tt>null</tt> if the specified key does not exist or the
	 *          cache entry is in invalidated state.
	 *
	 * @throws ClassCastException if the key is not of an inappropriate type for
	 *        this map. (Currently supports only type String)
	 * @throws NullPointerException key is <tt>null</tt> and this map does not
	 *        not permit <tt>null</tt> keys.
	 *
     * @ibm-api
	 */
	public CacheEntry getCacheEntry(Object key);

    /**
     * Associates the specified value with the specified key in this map.
     * If specified key does not exists, put new cache entry to cache's mapping
     * table. If specified key exists, replaces the old cache entry in cache's
     * mapping table with new one and then delete the old cache entry after
     * reference count becomes zero.
     * @ibm-api
     * @param key        key with which the specified value is to be associated.
     * @param value      value to be associated with the specified key.
     * @param userMetaData
     *                   userMetaData for the cache entry.
     * @param priority   the priority value for the cache entry.  entries
     *                   with higher priority will remain in the cache longer
     *                   than those with a lower priority in the case of cache
     *                   overflow.
     * @param timeToLive the time in seconds that the cache entry should remain
     *                   in the cache
     * @param sharingPolicy
     *                   how the cache entry should be shared in a cluster.
     *                   values are EntryInfo.NOT_SHARED, EntryInfo.SHARED_PUSH,
     *                   and EntryInfo.SHARED_PUSH_PULL.
     * @param dependencyIds
     *                   an optional set of dependency ids to associate with
     *                   the cache entry
     * @param alias      an optional set of alias ids to associate with the cache entry
     * @exception UnsupportedOperationException
     *                   if the <tt>put</tt> operation is
     *                   not supported by this map.
     * @exception ClassCastException
     *                   if the class of the specified key or value
     *                   prevents it from being stored in this map.
     * @exception IllegalArgumentException
     *                   if some aspect of this key or value
     *                   prevents it from being stored in this map.
     * @exception NullPointerException
     *                   this map does not permit <tt>null</tt>
     *                   keys or values, and the specified key or value is
     *                   <tt>null</tt>.
     * @see DistributedNioMapObject
     * @ibm-api
     */
	public void put(
		Object key,
		Object value,
		Object userMetaData,
		int priority,
		int timeToLive,
		int sharingPolicy,
		Object dependencyIds[],
		Object alias[]);

     /**
     * Associates the specified value with the specified key in this map.
     * If specified key does not exists, put new cache entry to cache's mapping
     * table. If specified key exists, replaces the old cache entry in cache's
     * mapping table with new one and then delete the old cache entry after
     * reference count becomes zero.
     * @ibm-api
     * @param key        key with which the specified value is to be associated.
     * @param value      value to be associated with the specified key.
     * @param userMetaData
     *                   userMetaData for the cache entry.
     * @param priority   the priority value for the cache entry.  entries
     *                   with higher priority will remain in the cache longer
     *                   than those with a lower priority in the case of cache
     *                   overflow.
     * @param timeToLive the time in seconds that the cache entry should remain
     *                   in the cache
     * @param inactivityTime
     *			 the time in seconds that a cache entry should remain in
     *			 the cache if not accessed. inactivityTime is reset to 0
     *			 every time an entry is accessed.
     * @param sharingPolicy
     *                   how the cache entry should be shared in a cluster.
     *                   values are EntryInfo.NOT_SHARED, EntryInfo.SHARED_PUSH,
     *                   and EntryInfo.SHARED_PUSH_PULL.
     * @param dependencyIds
     *                   an optional set of dependency ids to associate with
     *                   the cache entry
     * @param alias      an optional set of alias ids to associate with the cache entry
     * @exception UnsupportedOperationException
     *                   if the <tt>put</tt> operation is
     *                   not supported by this map.
     * @exception ClassCastException
     *                   if the class of the specified key or value
     *                   prevents it from being stored in this map.
     * @exception IllegalArgumentException
     *                   if some aspect of this key or value
     *                   prevents it from being stored in this map.
     * @exception NullPointerException
     *                   this map does not permit <tt>null</tt>
     *                   keys or values, and the specified key or value is
     *                   <tt>null</tt>.
     * @see DistributedNioMapObject
     * @ibm-api
     */
	public void put(
		Object key,
		Object value,
		Object userMetaData,
		int priority,
		int timeToLive,
		int inactivityTime,
		int sharingPolicy,
		Object dependencyIds[],
		Object alias[]);


    /**
     * Associates the specified value with the specified key in this map.
     * If specified key does not exists, put new cache entry to cache's mapping
     * table. If specified key exists, replaces the old cache entry in cache's
     * mapping table with new one and then delete the old cache entry after
     * reference count becomes zero.
     * @ibm-api
     * @param key        key with which the specified value is to be associated.
     * @param value      value to be associated with the specified key.
     * @param userMetaData
     *                   userMetaData for the cache entry.
     * @param priority   the priority value for the cache entry.  entries
     *                   with higher priority will remain in the cache longer
     *                   than those with a lower priority in the case of cache
     *                   overflow.
     * @param timeToLive the time in seconds that the cache entry should remain
     *                   in the cache
     * @param inactivityTime
     *			 the time in seconds that a cache entry should remain in
     *			 the cache if not accessed. inactivityTime is reset to 0
     *			 every time an entry is accessed.
     * @param sharingPolicy
     *                   how the cache entry should be shared in a cluster.
     *                   values are EntryInfo.NOT_SHARED, EntryInfo.SHARED_PUSH,
     *                   and EntryInfo.SHARED_PUSH_PULL. If skipMemoryAndWriteToDisk is
     *                   set to true, the sharing policy will be set to "not-shared".
     * @param dependencyIds
     *                   an optional set of dependency ids to associate with
     *                   the cache entry
     * @param alias      an optional set of alias ids to associate with the cache entry
     * @param skipMemoryAndWriteToDisk  if true, the cache entry will not put in the memory cache but
     *                                  written to disk cache directly. When getting the entry
     *                                  from the disk cache, the entry will not be promoted to the memory
     *                                  cache instead it will be passed it to the caller. In addition, 
     *                                  the entry will not be replicated to other servers. The sharing 
     *                                  policy will be set to "not-shared".
     * @exception UnsupportedOperationException
     *                   if the <tt>put</tt> operation is
     *                   not supported by this map.
     * @exception ClassCastException
     *                   if the class of the specified key or value
     *                   prevents it from being stored in this map.
     * @exception IllegalArgumentException
     *                   if some aspect of this key or value
     *                   prevents it from being stored in this map.
     * @exception NullPointerException
     *                   this map does not permit <tt>null</tt>
     *                   keys or values, and the specified key or value is
     *                   <tt>null</tt>.
     * @exception DynamicCacheException
     *                   When skipMemoryAndWriteToDisk is set to true, the following
     *                   exceptions which extend DynamicCacheException can occur:
     *                   (1) <tt>DiskOffloadNotEnabledException</tt>
     *                       The code detects that the disk offload feature for cache instance
     *                       is not enabled. The put operation is aborted.
     *                   (2) <tt>DiskIOException</tt>
     *                       The code detects an unrecoverable disk IO exception when putting
     *                       cache entry to the disk. The disk offload feature will be disabled.
     *                   (3) <tt>DiskSizeOverLimitException</tt>
     *                       The code detects out of disk space or the disk cache size has reached
     *                       the limit of diskCacheSizeInGB. The put operation is aborted.
     *                   (4) <tt>SerializationException</tt>
     *                       The code detects a serialization exception when serializing the cache
     *                       entry. The put operation is aborted.
     *                   (5) <tt>DiskSizeInEntriesOverLimitException</tt>
     *                       The disk cache size in entries has reached the limit of diskCacheSize.
     *                       The put operation is aborted.
     *                   (6) <tt>DiskCacheEntrySizeOverLimitException</tt>
     *                       The disk cache entry size has reached the limit of diskCacheEntrySizeInMB.
     *                       The put operation is aborted.
     *                   (7) <tt>MiscellaneousException</tt>
     *                       The code detects a runtime exception other than a disk IO exception.
     *                       The put operation is aborted.
     * @see DistributedNioMapObject
     * @see DiskOffloadNotEnabledException
     * @see DiskIOException
     * @see DiskSizeOverLimitException
     * @see SerializationException
     * @see DiskSizeInEntriesOverLimitException
     * @see DiskCacheEntrySizeOverLimitException
     * @see MiscellaneousException
     * @ibm-api
     */
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
        boolean skipMemoryAndWriteToDisk) throws DynamicCacheException;


    /**
     * Associates the specified value with the specified key in this map.
     * If specified key does not exists, put new cache entry to cache's mapping
     * table. If specified key exists, replaces the old cache entry in cache's
     * mapping table with new one and then delete the old cache entry after
     * reference count becomes zero.
     * @ibm-api
     * @param key        key with which the specified value is to be associated.
     * @param value      value to be associated with the specified key.
     * @param userMetaData
     *                   userMetaData for the cache entry.
     * @param priority   the priority value for the cache entry.  entries
     *                   with higher priority will remain in the cache longer
     *                   than those with a lower priority in the case of cache
     *                   overflow.
     * @param timeToLive the time in seconds that the cache entry should remain
     *                   in the cache
     * @param sharingPolicy
     *                   how the cache entry should be shared in a cluster.
     *                   values are EntryInfo.NOT_SHARED, EntryInfo.SHARED_PUSH,
     *                   and EntryInfo.SHARED_PUSH_PULL.
     * @param dependencyIds
     *                   an optional set of dependency ids to associate with
     *                   the cache entry
     * @param alias      an optional set of alias ids to associate with the cache entry
     * @return The newly created CacheEntry. You must call
     *         CacheEntry.finish() when you are finished
     *         using the entry.
     * @exception UnsupportedOperationException
     *                   if the <tt>put</tt> operation is
     *                   not supported by this map.
     * @exception ClassCastException
     *                   if the class of the specified key or value
     *                   prevents it from being stored in this map.
     * @exception IllegalArgumentException
     *                   if some aspect of this key or value
     *                   prevents it from being stored in this map.
     * @exception NullPointerException
     *                   this map does not permit <tt>null</tt>
     *                   keys or values, and the specified key or value is
     *                   <tt>null</tt>.
     * @see DistributedNioMapObject
     * @ibm-api
     */
	public CacheEntry putAndGet(
		Object key,
		Object value,
		Object userMetaData,
		int priority,
		int timeToLive,
		int sharingPolicy,
		Object dependencyIds[],
		Object alias[]);

/**
     * Associates the specified value with the specified key in this map.
     * If specified key does not exists, put new cache entry to cache's mapping
     * table. If specified key exists, replaces the old cache entry in cache's
     * mapping table with new one and then delete the old cache entry after
     * reference count becomes zero.
     * @ibm-api
     * @param key        key with which the specified value is to be associated.
     * @param value      value to be associated with the specified key.
     * @param userMetaData
     *                   userMetaData for the cache entry.
     * @param priority   the priority value for the cache entry.  entries
     *                   with higher priority will remain in the cache longer
     *                   than those with a lower priority in the case of cache
     *                   overflow.
     * @param timeToLive the time in seconds that the cache entry should remain
     *                   in the cache
     * @param inactivityTime
     *			 the time in seconds that a cache entry should remain in
     *			 the cache if not accessed. inactivityTime is reset to 0
     *			 every time an entry is accessed.
     * @param sharingPolicy
     *                   how the cache entry should be shared in a cluster.
     *                   values are EntryInfo.NOT_SHARED, EntryInfo.SHARED_PUSH,
     *                   and EntryInfo.SHARED_PUSH_PULL.
     * @param dependencyIds
     *                   an optional set of dependency ids to associate with
     *                   the cache entry
     * @param alias      an optional set of alias ids to associate with the cache entry
     * @return The newly created CacheEntry. You must call
     *         CacheEntry.finish() when you are finished
     *         using the entry.
     * @exception UnsupportedOperationException
     *                   if the <tt>put</tt> operation is
     *                   not supported by this map.
     * @exception ClassCastException
     *                   if the class of the specified key or value
     *                   prevents it from being stored in this map.
     * @exception IllegalArgumentException
     *                   if some aspect of this key or value
     *                   prevents it from being stored in this map.
     * @exception NullPointerException
     *                   this map does not permit <tt>null</tt>
     *                   keys or values, and the specified key or value is
     *                   <tt>null</tt>.
     * @see DistributedNioMapObject
     * @ibm-api
     */
	public CacheEntry putAndGet(
		Object key,
		Object value,
		Object userMetaData,
		int priority,
		int timeToLive,
		int inactivityTime,
		int sharingPolicy,
		Object dependencyIds[],
		Object alias[]);

	/**
	* invalidate - invalidates the given key.  If the key is
	* for a specific cache entry, then only that object is
	* invalidated.  If the key is for a dependency id, then
	* all objects that share that dependency id will be
	* invalidated.
	* @param key the key which will be invalidated
	* @param wait if true, then the method will not complete until the invalidation
	*             has occured.  if false, then the invalidation will occur in batch mode
	* @param checkPreInvalidationListener if true, first check with PreInvalidationListener to see
	* 										if invalidation should occur. If false, bypass this check
	* 										and invalidate immediately.
	* @see com.ibm.websphere.cache.PreInvalidationListener
    * @ibm-api
	*/
	public void invalidate(Object key, boolean wait, boolean checkPreInvalidationListener);

	/**
	 * Invalidates the given key.  If the key is
	 * for a specific cache entry, then only that object is
	 * invalidated.  If the key is for a dependency id, then
	 * all objects that share that dependency id will be
	 * invalidated.
	 *
	 * @param key the key which will be invalidated
	 * @param wait if true, then the method will not complete until the invalidation
	 *             has occured.  if false, then the invalidation will occur in batch mode
     * @ibm-api
	 */
	public void invalidate(Object key, boolean wait);

	/**
	 * Invalidates the given key.  If the key is
	 * for a specific cache entry, then only that object is
	 * invalidated.  If the key is for a dependency id, then
	 * all objects that share that dependency id will be
	 * invalidated.  This method is the same as
     * using invalidate(key, true).
	 *
	 * @param key the key which will be invalidated
     * @ibm-api
	 */
	public void invalidate(Object key);

	/**
	 * Removes all mappings from this DistributedNioMap.
     * @ibm-api
	 */
	public void clear();

	/**
	 * Adds an alias for the given key in the cache's mapping table. If the alias is already
	 * associated with another key, it will be changed to associate with the new key.
		 *
	 * @param key the key assoicated with alias
	 * @param aliasArray the alias to use for lookups
	 * @throws IllegalArgumentException if the key is not in the cache's mapping table.
     * @ibm-api
	 */
	public void addAlias(Object key, Object[] aliasArray);

	/**
	 * Removes an alias from the cache's mapping table.
	 * @param alias the alias to move out of the cache's mapping table
     * @ibm-api
	 */
	public void removeAlias(Object alias);

    /**
     * Use this method to release LRU cache entries (regular objects or ByteByffers/MetaData).
     *
     * @param numOfEntries
     *               the number of cache entries to be released
     * @ibm-api
     */
	public void releaseLruEntries(int numOfEntries);

    // todo: V6.1
    ///**
    // * This method will release all resources associated
    // * with this map.  Once a map is destroyed you can
    // * no longer use it.
    // *
    // * @return success
    // *         - true  The map was destroyed.
    // *         - flase The map could not be destroyed.
    // */
    //public boolean destroy();

    /**
     * Returns the total number of key-value mappings. Returns size of memory map plus disk map if includeDiskCache is
     * true. Returns size of memory map size if includeDiskCache is false.
     * @param includeDiskCache true to get the size of the memory and disk maps; false to get the size of memory map.
     * @return the number of key-value mappings in this map.
     * @ibm-api
     */
    public int size(boolean includeDiskCache);

    /**
     * Returns true if this map contains no key-value mappings. Checks both memory and disk maps if includeDiskCache
     * is true. Check only memory cache if includeDiskCache is false.
     * @param includeDiskCache true to check the memory and disk maps; false to check the memory map.
     * @return true if this map contains no key-value mappings.
     * @ibm-api
     */
    public boolean isEmpty(boolean includeDiskCache);

    /**
     * Returns true if this map contains mapping for the specified key. Checks both memory and disk map if includeDiskCache
     * is true. Check only memory map if includeDiskCache is false.
     * @param key whose presence in this map is to be tested.
     * @param includeDiskCache true to check the specified key contained in the memory or disk maps; false to check the specified key contained in the memory map.
     * @return true if this map contains a mapping for the specified key.
     * @ibm-api
     */
    public boolean containsKey(Object key, boolean includeDiskCache);

    /**
     * enableListener - enable or disable the invalidation and change listener support.
     * You must call enableListener(true) before calling addInvalidationListner() or addChangeListener().
     *
     * @param enable - true to enable support for invalidation and change listeners
     *                 or false to disable support for invalidation and change listeners
     * @return boolean "true" means listener support was successfully enabled or disabled.
     *                 "false" means this DistributedMap is configurated to use the listener's J2EE context for
     *             event notification and the callback registration failed.  In this case, the caller's thread
     *             context will be used for event notification.
     *
     * @ibm-api
     */
    public boolean enableListener(boolean enable);

    /**
     * addInvalidationListener - adds an invalidation listener for this DistributeMap.
     *
     * @param listener the invalidation listener object
     * @return boolean "true" means the invalidation listener was successfully added.
     *                 "false" means either the passed listener object is null or listener support is not enable.
     * @see #removeInvalidationListener(com.ibm.websphere.cache.InvalidationListener)
     * @ibm-api
     */
    public boolean addInvalidationListener(InvalidationListener listener);

    /**
     * removeInvalidationListener - removes an invalidation listener for this DistributedMap.
     *
     * @param listener the invalidation listener object
     * @return boolean "true" means the invalidation listener was successfully removed.
     *                 "false" means either passed listener object is null or listener support is not enable.
     * @see #addInvalidationListener(com.ibm.websphere.cache.InvalidationListener)
     * @ibm-api
     */
    public boolean removeInvalidationListener(InvalidationListener listener);

    /**
     * addChangeListener - adds a change listener for this DistributedMap.
     *
     * @param listener the change listener object
     * @return boolean "true" means the change listener was successfully added.
     *                 "false" means either the passed listener object is null or listener support is not enable.
     * @see #removeChangeListener(com.ibm.websphere.cache.ChangeListener)
     * @ibm-api
     */
    public boolean addChangeListener(ChangeListener listener);

    /**
     * removeChangeListener - removes a change listener for this DistributedMap.
     *
     * @param listener the change listener object
     * @return boolean "true" means the change listener was successfully removed.
     *                 "false" means either passed listener object is null or listener support is not enable.
     * @see #addChangeListener(com.ibm.websphere.cache.ChangeListener)
     * @ibm-api
     */
    public boolean removeChangeListener(ChangeListener listener);

    /**
	* addPreInvalidationListener - adds a pre-invalidation listener for this DistributeMap. If
	* one already exists, this method will over-write it.
	*
	* @param listener the pre-invalidation listener object
	* @return boolean "true" means the pre-invalidation listener was successfully added.
	*                 "false" means either the passed listener object is null or listener support is not enabled.
	* @see com.ibm.websphere.cache.PreInvalidationListener
	* @see #removePreInvalidationListener(com.ibm.websphere.cache.PreInvalidationListener)
    * @ibm-api
	*/
    public boolean addPreInvalidationListener(PreInvalidationListener listener);

    /**
	* removePreInvalidationListener - removes a pre-invalidation listener for this DistributedMap.
    *
    * @param listener the invalidation listener object
	* @return boolean "true" means the pre-invalidation listener was successfully removed.
	*                 "false" means either passed listener object is null or listener support is not enabled.
	* @see com.ibm.websphere.cache.PreInvalidationListener
	* @see #addPreInvalidationListener(com.ibm.websphere.cache.PreInvalidationListener)
    * @ibm-api
    */
    public boolean removePreInvalidationListener(PreInvalidationListener listener);
}
