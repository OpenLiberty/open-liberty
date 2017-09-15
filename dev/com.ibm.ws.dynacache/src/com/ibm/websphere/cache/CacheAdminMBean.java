/*******************************************************************************
 * Copyright (c) 1997, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cache;

import javax.management.AttributeNotFoundException;

/**
 * The CacheAdmin MBean defines the management interface to retrieve usage statistics
 * for a server's DynaCache (also known as the Distributed Map).
 * <p>
 * The ObjectName for this MBean is {@value #OBJECT_NAME}.
 * 
 * @ibm-api
 */
public interface CacheAdminMBean {
    /**
     * A String representing the {@link javax.management.ObjectName} that this MBean maps to.
     * See also the @Component property.
     * Note that in many (most?) cases users will want to reference this as "Websphere:type=DynaCache,*",
     * which will also work with Traditional WAS (which uses a related but different object name)
     */
    public static String OBJECT_NAME = "WebSphere:feature=CacheAdmin,type=DynaCache,name=DistributedMap";

    /**
     * Returns the maximum size in entries of the in-memory cache.
     * 
     * @Return The maximum size in entries of the in-memory cache.
     */
    public abstract int getCacheSize();

    /**
     * Return the number of used cache entries in the "baseCache" in-memory cache.
     * 
     * @return The number of used cache entries in the "baseCache" in-memory cache.
     */
    public abstract int getUsedCacheSize();

    /**
     * Indicates whether disk based overflow (disk offload) is enabled.
     * 
     * @return true if the disk based overflow (disk offload) is enabled.
     */
    public abstract boolean getDiskOverflow();

    public abstract String[] getCacheStatisticNames();

    public abstract String[] getCacheStatisticNames(String cacheInstance)
                    throws AttributeNotFoundException;

    /**
     * Retrieves the names of the available cache instances.
     * 
     * @return the names of the available cache instances.
     */
    public abstract String[] getCacheInstanceNames();

    /**
     * Retrieves all of the available cache statistics for the "baseCache" instance.
     * 
     * @return all of the available cache statistics for the "baseCache" instance.
     */
    public abstract String[] getAllCacheStatistics();

    /**
     * Retrieves cache statistics specified by the named cache instance.
     * 
     * @param cacheInstance The name of the cache instance.
     * @return The all statistics list of the named cache instance
     * @throws javax.management.AttributeNotFoundException
     */
    public abstract String[] getAllCacheStatistics(String cacheInstance)
                    throws javax.management.AttributeNotFoundException;

    public abstract String[] getCacheStatistics(String[] names)
                    throws javax.management.AttributeNotFoundException;

    /**
     * Retrieves cache statistics specified by the names array for the named cache instance.
     * 
     * @param cacheInstance The name of the cache instance.
     * @param names The array of cache statistic names
     * @return The statistics list of the names array
     * @throws javax.management.AttributeNotFoundException
     */
    public abstract String[] getCacheStatistics(String cacheInstance,
                                                String[] names) throws javax.management.AttributeNotFoundException;

    /**
     * Retrieves all of the cache IDs in memory for the named cache instance that matches
     * the specified regular expression. The java.util.regex libraries are for matching.
     * The find() method locates the matching IDs.
     * 
     * @param cacheInstance The name of the cache instance.
     * @param pattern A regular expression that is specified as a string.
     * @return The list of cache IDs mapped to the pattern
     * @throws javax.management.AttributeNotFoundException
     */
    public abstract String[] getCacheIDsInMemory(String cacheInstance,
                                                 String pattern) throws javax.management.AttributeNotFoundException;

    /**
     * Retrieves all of the cache IDs on disk for the named cache instance that matches
     * the specified regular expression. The java.util.regex libraries are for matching.
     * The find() method locates the matching IDs. This operation can take a non-deterministic
     * amount of time to complete in some extreme cases.
     * 
     * @param cacheInstance The name of the cache instance.
     * @param pattern A regular expression that is specified as a string.
     * @return The list of cache IDs mapped to the pattern
     * @throws javax.management.AttributeNotFoundException
     */
    public abstract String[] getCacheIDsOnDisk(String cacheInstance,
                                               String pattern) throws javax.management.AttributeNotFoundException;

    /**
     * Retrieves all of the cache IDs in the PushPullTable for the named cache instance that
     * matches the specified regular expression. The java.util.regex libraries are for matching.
     * The find() method locates the matching IDs.
     * 
     * @param cacheInstance The name of the cache instance.
     * @param pattern A regular expression that is specified as a string.
     * @return The list of cache IDs mapped to the pattern
     * @throws javax.management.AttributeNotFoundException
     */
    public abstract String[] getCacheIDsInPushPullTable(String cacheInstance,
                                                        String pattern) throws javax.management.AttributeNotFoundException;

    /**
     * Invalidates all cache entries that match the pattern mapped cache IDs in the
     * named cache instance and all cache entries dependent upon the matched entries
     * in the instance. Returns the list of cache IDs mapped to the pattern.
     * Dependent cache entries invalidated are not in the list. Matched entries are
     * invalidated in the memory cache and disk cache.
     * To clear a cache,* invoke invalidateCacheIDs with a pattern = *.
     * In this case, a list with only the element * is returned.
     * 
     * @param cacheInstance The name of the cache instance.
     * @param pattern A regular expression that is specified as a string.
     * @param waitOnInvalidation True indicates that this method should not return until
     *            the invalidations have taken effect. False indicates that the invalidations will be queued for
     *            later batch processing. For waitOnInvalidation = true, this method will take a long time to
     *            return and potentially could lock the cache and reduce throughput.
     *            If waitOnInvalidation = false, this method returns almost immediately, and the invalidates
     *            are handled on a separate thread.
     * @return The list of cache IDs mapped to the pattern
     * @throws javax.management.AttributeNotFoundException
     */
    public abstract String[] invalidateCacheIDs(String cacheInstance,
                                                String pattern, boolean waitOnInvalidation)
                    throws javax.management.AttributeNotFoundException;

    /**
     * Retrieves the CacheEntry which holds metadata information for the cache ID.
     * Returns null if the CacheEntry is not found. If the entry is found, the following information is returned:
     * Cache ID
     * User metadata,
     * Priority,
     * Time To Live indicator (TTL),
     * Inactivity,
     * ExpirationTime,
     * Sharing Policy,
     * value Size,
     * value hashcode,
     * A boolean that indicate whether or not the entry exists on disk.
     * A boolean that indicates whether the cache entry skipped memory and was written directly to disk cache.
     * Template IDs,
     * Dependency IDs,
     * Alias IDs
     * 
     * @param cacheInstance The name of the cache instance.
     * @param cacheId The name of cache ID
     * @return cache entry
     * @throws javax.management.AttributeNotFoundException
     */
    public abstract String getCacheEntry(String cacheInstance, String cacheId)
                    throws javax.management.AttributeNotFoundException;

    /**
     * Clear all the memory and disk entries for the named cache instance.
     * 
     * @param cacheInstance The name of the cache instance.
     * @throws javax.management.AttributeNotFoundException
     */
    public abstract void clearCache(String cacheInstance)
                    throws javax.management.AttributeNotFoundException;

    /**
     * Returns an MD5 digest of all the cache entries for the named cache instance.
     * Note that the cache key and value objects should override the default java.lang.Object.hashCode()
     * method to get semantic comparability between object instances. This is a CPU and I/O intensive
     * when the useMemoryCacheDigest parameter is set to false. In this case the command will compute
     * a hash of the cached objects on disk.
     * 
     * @param cacheInstance The name of the cache instance.
     * @param useMemoryCacheDigest If true, use only the memory cache digest. If false, use the whole cache digest.
     * @param cacheIDOnly If true, get cache ID digest. It also includes ID digest from the PushPullTable. If false, get cache ID/value digest.
     * @param debug If debug is true, a list of the cache IDs and their hashcodes are written to the SystemOut log.
     * @return MD5 digest of all the cache entries in memory/disk for the named cache instance.
     * @throws javax.management.AttributeNotFoundException
     */
    public abstract String getCacheDigest(String cacheInstance,
                                          boolean useMemoryCacheDigest, boolean cacheIDOnly, boolean debug)
                    throws javax.management.AttributeNotFoundException;

}