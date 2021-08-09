/*******************************************************************************
 * Copyright (c) 2012, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.wim.env;

import java.util.Map;
import java.util.Set;

/**
 * Interface for cache utilities.
 * Most of the methods here are from WebSphere's com.ibm.websphere.cache.DistributeCache Interface.
 */
public interface ICacheUtil extends Map<String, Object> {
    /**
     * Constant to be used when Sharing is not applicable.
     */
    public static int SHARED_NA = -1;

    /**
     * Returns true if the cache is initialized
     */
    public boolean isCacheInitialized();

    /**
     * Returns true if the cache is available
     */
    public boolean isCacheAvailable();

    /**
     * Get integer value of sharing policy for NOT_SHARED.
     * Return SHARED_NA if not applicable.
     */
    public int getNotSharedInt();

    /**
     * Get integer value of sharing policy for SHARED_PUSH.
     * Return SHARED_NA if not applicable.
     */
    public int getSharedPushInt();

    /**
     * Get integer value of sharing policy for SHARED_PUSH_PULL.
     * Return SHARED_NA if not applicable.
     */
    public int getSharedPushPullInt();

    /**
     * setSharingPolicy - sets the sharing policy for cache. This applies to distributed cache only.
     *
     * @see #getSharingPolicy()
     */
    public void setSharingPolicy(int sharingPolicy);

    /**
     * getSharingPolicyInt - gets the integer value of the sharing policy for Cache.
     * This applies to distributed cache only.
     *
     * @param sharingPolicyStr Sharing policy string.
     *
     * @return returns the current sharing policy. Return SHARED_NA if not applicable.
     */
    public int getSharingPolicyInt(String sharingPolicyStr);

    /**
     * Set the global time-to-live (in seconds) for this cache.
     *
     * @param timeToLive
     */
    public void setTimeToLive(int timeToLive);

    /**
     * Returns the value for the specified key. Returns
     * <tt>null</tt> if the map contains no mapping for this key. A return
     * value of <tt>null</tt> does not <i>necessarily</i> indicate that the
     * map contains no mapping for the key; it's also possible that the map
     * explicitly maps the key to <tt>null</tt>. The <tt>containsKey</tt>
     * operation may be used to distinguish these two cases.
     *
     * @param key key whose associated value is to be returned.
     * @return the value to which this cache maps the specified key, or
     *         <tt>null</tt> if the cache contains no mapping for this key.
     *
     * @see #containsKey(Object)
     */
    @Override
    public Object get(Object key);

    /**
     * Associates the specified value with the specified key. If the cache previously
     * contained a mapping for this key, the old value is replaced.
     *
     * @param key key with which the specified value is to be associated.
     * @param value value to be associated with the specified key.
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key. A <tt>null</tt> return can
     *         also indicate that the map previously associated <tt>null</tt>
     *         with the specified key, if the implementation supports
     *         <tt>null</tt> values.
     */
    @Override
    public Object put(String key, Object value);

    /**
     * Associates the specified value with the specified key in this cache
     * (optional operation). If the map previously contained a mapping for
     * this key, the old value is replaced.
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
     *            values are Cache implementation specific.
     *            e.g: NOT_SHARED, SHARED_PUSH, SHARED_PUSH_PULL, etc.
     * @param dependencyIds an optional set of dependency ids to associate with
     *            the cache entry
     * @return previous value associated with specified key, or <tt>null</tt>
     *         if there was no mapping for key. A <tt>null</tt> return can
     *         also indicate that the map previously associated <tt>null</tt>
     *         with the specified key, if the implementation supports
     *         <tt>null</tt> values.
     */
    public Object put(Object key, Object value, int priority, long timeToLive, int sharingPolicy, Object dependencyIds[]);

    /**
     * invalidate - invalidates the given key. If the key is
     * for a specific cache entry, then only that object is
     * invalidated. If the key is for a dependency id, then
     * all objects that share that dependency id will be
     * invalidated.
     *
     * @param key the key which will be invalidated
     * @see #remove(Object key)
     */
    public void invalidate(Object key);

    /**
     * invalidate - invalidates the entire cache
     */
    public void invalidate();

    /**
     * Returns the total number of key-value mappings. Returns size of memory map plus disk map if includeDiskCache is
     * true. Returns size of memory map size if includeDiskCache is false.
     *
     * @param includeDiskCache true to get the size of the memory and disk maps; false to get the size of memory map.
     * @return the number of key-value mappings in this map.
     */
    public int size(boolean includeDiskCache);

    /**
     * Returns true if this map contains no key-value mappings. Checks both memory and disk maps if includeDiskCache
     * is true. Check only memory cache if includeDiskCache is false.
     *
     * @param includeDiskCache true to check the memory and disk maps; false to check the memory map.
     * @return true if this map contains no key-value mappings.
     */
    public boolean isEmpty(boolean includeDiskCache);

    /**
     * @see java.util.Map#containsKey(Object)
     */
    @Override
    public boolean containsKey(Object key);

    /**
     * Returns true if this map contains mapping for the specified key. Checks both memory and disk map if includeDiskCache
     * is true. Check only memory map if includeDiskCache is false.
     *
     * @param key whose presence in this map is to be tested.
     * @param includeDiskCache true to check the specified key contained in the memory or disk maps; false to check the specified key contained in the memory map.
     * @return true if this map contains a mapping for the specified key.
     */
    public boolean containsKey(Object key, boolean includeDiskCache);

    /**
     * Returns a set view of the keys contained in this map. Returns all the keys in both memory map and disk map if includeDiskCache is true.
     * Return only keys in memory map if includeDiskCache is false.
     * Warning: If this method is used with includeDiskCache set to true, all the keys on disk are read into memory and that might consume a lot of memory depending
     * on the size of disk map.
     *
     * @param includeDiskCache true to get keys contained in the memory and disk maps; false to get keys contained in the memory map.
     * @return a set view of the keys contained in this map.
     */
    public Set<String> keySet(boolean includeDiskCache);

    /**
     * Initialize the cache.
     *
     * @param cacheName name of the cache
     * @param cacheSize size of the cache
     * @param diskOffLoad should the cache data be stored on the disk
     */
    public ICacheUtil initialize(String cacheName, int cacheSize, boolean diskOffLoad);

    /**
     * Initialize the cache.
     *
     * @param cacheName name of the cache
     * @param cacheSize size of the cache
     * @param diskOffLoad should the cache data be stored on the disk
     * @param sharingPolicy sharing policy of the cache
     */
    public ICacheUtil initialize(String cacheName, int cacheSize, boolean diskOffLoad, int sharingPolicy);

    public ICacheUtil initialize(int initialSize, int cacheSize, long cachetimeOut);

    public ICacheUtil initialize(String cacheName, int initialSize, int cacheSize, long cachetimeOut);

    public void stopEvictionTask();
}
