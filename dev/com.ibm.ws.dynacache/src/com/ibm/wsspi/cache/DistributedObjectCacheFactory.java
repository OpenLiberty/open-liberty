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
package com.ibm.wsspi.cache;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.ibm.websphere.cache.DistributedMap;
import com.ibm.websphere.cache.DistributedNioMap;
import com.ibm.websphere.cache.DistributedObjectCache;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.cache.CacheConfig;
import com.ibm.ws.cache.CacheService;
import com.ibm.ws.cache.CacheServiceImpl;
import com.ibm.ws.cache.DCacheBase;
import com.ibm.ws.cache.DistributedMapImpl;
import com.ibm.ws.cache.DistributedNioMapImpl;
import com.ibm.ws.cache.ServerCache;
import com.ibm.ws.cache.intf.DCache;

/**
 * This class provides components with factory methods to
 * create/lookup instances of a DistributedObjectCache.
 * Each DistributedObjectCache instance can be configured
 * independently. Use the DistributedObjectCache.getMapType
 * method to determine the map type.
 * 
 * The following property keys should be used to specify the configuration of
 * the {@link DistributedMap} or {@link DistributedNioMap} created using
 * this class.
 * 
 * @see DistributedMap
 * @see DistributedNioMap
 * @ibm-spi
 */
public class DistributedObjectCacheFactory {
    private static TraceComponent tc = Tr.register(DistributedObjectCacheFactory.class, "WebSphere Dynamic Cache", "com.ibm.ws.cache.resources.dynacache");

    /**
     * Property key to specify cache size.
     * Value is string int
     * 
     * @since v6.0
     */
    public static final String KEY_CACHE_SIZE = CacheConfig.CACHE_SIZE;

    /**
     * Property key to disble dependency support.
     * Value is VALUE_TRUE or VALUE_FALSE
     * 
     * @since v6.0
     */
    public static final String KEY_DISABLE_DEPENDENCY_ID = CacheConfig.DISABLE_DEPENDENCY_ID;

    /**
     * Property key to disble template support.
     * Value is VALUE_TRUE or VALUE_FALSE
     * 
     * @since v6.0
     */
    public static final String KEY_DISABLE_TEMPLATES_SUPPORT = CacheConfig.DISABLE_TEMPLATES_SUPPORT;

    /**
     * Property key to enable disk offload support.
     * Value is VALUE_TRUE or VALUE_FALSE
     * 
     * @since v6.0
     */
    public static final String KEY_ENABLE_DISK_OFFLOAD = CacheConfig.ENABLE_DISK_OFFLOAD;

    /**
     * Property key to enable nio support.
     * Value is VALUE_TRUE or VALUE_FALSE
     * 
     * @see DistributedNioMap
     * @since v6.0
     */
    public static final String KEY_ENABLE_NIO_SUPPORT = CacheConfig.ENABLE_NIO_SUPPORT;

    /**
     * Property key to specify the disk offload location.
     * Value is string path
     * 
     * @since v6.0
     */
    public static final String KEY_DISK_OFFLOAD_LOCATION = CacheConfig.DISK_OFFLOAD_LOCATION;

    /**
     * Property key to enable listener context on callbacks.
     * Value is VALUE_TRUE or VALUE_FALSE
     * 
     * @since v6.0
     */
    public static final String KEY_USE_LISTENER_CONTEXT = CacheConfig.USE_LISTENER_CONTEXT;

    /**
     * Property key to enable flushing cache contents
     * to disk when the server is stopped.
     * This property is ignored if disk offload is disabled.
     * Value is VALUE_TRUE or VALUE_FALSE
     * 
     * @since v6.0
     */
    public static final String KEY_FLUSH_TO_DISK_ON_STOP = CacheConfig.FLUSH_TO_DISK_ON_STOP;

    /**
     * Property key to enable cache replication.
     * Value is VALUE_TRUE or VALUE_FALSE
     * 
     * @since v6.0
     */
    public static final String KEY_ENABLE_CACHE_REPLICATION = CacheConfig.ENABLE_CACHE_REPLICATION;

    /**
     * Property key to specify the replication domain.
     * Value is string domain name.
     * 
     * @since v6.0
     */
    public static final String KEY_REPLICATION_DOMAIN = CacheConfig.REPLICATION_DOMAIN;

    /**
     * Property key to specify the disk cache performance level.
     * Value: 0 - Low Performance and low memory usage;
     * Value: 1 - Balanced Performance and balanced memory usage (default);
     * Value: 2 - Custom Performance and custom memory usage;
     * Value: 3 - High Performance and high memory usage;
     * 
     * @since v6.1
     */
    public static final String KEY_DISKCACHE_PERFORMANCE_LEVEL = CacheConfig.DISKCACHE_PERFORMANCE_LEVEL;

    /**
     * Property key to specify the frequency at which the disk cache cleanup daemon
     * should remove expired entries from the disk cache.
     * Value: positive value in minutes; default = 0 (cleanup is scheduled to run at midnight).
     * 
     * @since v6.0
     */
    public static final String KEY_DISK_CLEANUP_FREQUENCY = CacheConfig.DISK_CLEANUP_FREQUENCY;

    /**
     * Property key to provide a way to limit the buffering of dependency
     * and template information by specify an upper bound on the number of cache entries any
     * specific dependency can contain for buffering in memory. If there are more entries per
     * dependency than this limit, the dependency/template information is written to disk.
     * Value: positive value; default = 1000;
     * 
     * @since v6.0
     */
    public static final String KEY_DISK_DELAY_OFFLOAD_ENTRIES_LIMIT = CacheConfig.DISK_DELAY_OFFLOAD_ENTRIES_LIMIT;

    /**
     * Property key to provide a way to limit the buffering of dependency ID information
     * by specifying an upper bound on the number of dependencies that will be buffered in memory.
     * If the count of dependencies exceeds this limit, the excess will be written to disk.
     * Value: positive value; default = 1000;
     * 
     * @since v6.0
     */
    public static final String KEY_DISK_DELAY_OFFLOAD_DEPID_BUCKETS = CacheConfig.DISK_DELAY_OFFLOAD_DEPID_BUCKETS;

    /**
     * Property key to provide a way to limit the buffering of template information
     * by specifying an upper bound on the number of templates that will be buffered in memory.
     * If the count of templates exceeds this limit, the excess will be written to disk.
     * Value: positive value; default = 1000;
     * 
     * @since v6.0
     */
    public static final String KEY_DISK_DELAY_OFFLOAD_TEMPLATE_BUCKETS = CacheConfig.DISK_DELAY_OFFLOAD_TEMPLATE_BUCKETS;

    /**
     * Property key to specify the disk cache eviction policy.
     * Value: 0 (none) - eviction algorithm disabled (default);
     * Value: 1 (random) - The expired objects are removed first. If the disk size has not reached
     * the low threshold limit, objects are picked from disk cache in random order until the disk
     * cache size has reached.
     * Value: 2 (size based) - The expired objects are removed first. If the disk size has not
     * reached the low threshold limit, largest-sized objects are removed until the disk cache
     * size has reached
     * 
     * @since v6.1
     */
    public static final String KEY_DISKCACHE_EVICTION_POLICY = CacheConfig.DISKCACHE_EVICTION_POLICY;

    /**
     * Property key to specify the high threshold in percentage. It is used to start evicting objects
     * out of disk cache when one or both of the high thresholds is reached
     * Value: 0 - 100
     * 
     * 
     * @since v6.1
     */
    public static final String KEY_DISKCACHE_HIGH_THRESHOLD = CacheConfig.DISKCACHE_HIGH_THRESHOLD;

    /**
     * Property key to specify the low threshold in percentage. It is used to bring down the disk cache till the low
     * threshold is reached.
     * Value: 0 - 100
     * 
     * 
     * @since v6.1
     */
    public static final String KEY_DISKCACHE_LOW_THRESHOLD = CacheConfig.DISKCACHE_LOW_THRESHOLD;

    /**
     * Property key to specify the limit of disk cache size in entries.
     * Value: positive integer; default 0 means no limit
     * 
     * 
     * @since v6.1
     */
    public static final String KEY_DISKCACHE_SIZE = CacheConfig.DISKCACHE_SIZE;

    /**
     * Property key to specify the limit of disk cache size in GB.
     * Value: positive integer; default 0 means no limit
     * 
     * 
     * @since v6.1
     */
    public static final String KEY_DISKCACHE_SIZE_GB = CacheConfig.DISKCACHE_SIZE_GB;

    /**
     * Property key to specify the limit of disk cache entry size in MB.
     * Value: positive integer; default 0 means no limit
     * 
     * 
     * @since v6.1
     */
    public static final String KEY_DISKCACHE_ENTRY_SIZE_MB = CacheConfig.DISKCACHE_ENTRY_SIZE_MB;

    /**
     * Property key to specify the limit of percentage of the memory cache size used as a overflow buffer
     * when disk offload is enabled.
     * Value: 0 - 100; default 0 means no overflow
     * 
     * 
     * @since v6.1
     */
    public static final String KEY_LRU_TO_DISK_TRIGGER_PERCENT = CacheConfig.LRU_TO_DISK_TRIGGER_PERCENT;

    /**
     * Property key to specify a value for the maximum memory cache size in megabytes (MB).
     * Value: positive integer; default: -1 means no limit on memory cache size
     * 
     * @since v7.0
     */
    public static final String KEY_MEMORY_CACHE_SIZE_IN_MB = CacheConfig.MEMORY_CACHE_SIZE_IN_MB;

    /**
     * Property key to specify the memory cache size high threshold in percentage.
     * It is used to start evicting objects out of memory cache until the high threshold is reached
     * Value: 0 - 100 (default: 95)
     * 
     * @since v7.0
     */
    public static final String KEY_MEMORY_CACHE_SIZE_HIGH_THRESHOLD = CacheConfig.MEMORY_CACHE_HIGH_THRESHOLD;

    /**
     * Property key to specify the memory cache low threshold in percentage.
     * It is used to prune the memory cache until the low threshold is reached.
     * Value: 0 - 100 (default: 80)
     * 
     * @since v7.0
     */
    public static final String KEY_MEMORY_CACHE_SIZE_LOW_THRESHOLD = CacheConfig.MEMORY_CACHE_LOW_THRESHOLD;

    /**
     * Property value for true.
     * 
     * @since v6.0
     */
    public static final String VALUE_TRUE = "true";

    /**
     * Property value for false.
     * 
     * @since v6.0
     */
    public static final String VALUE_FALSE = "false";
    //---------------------------------------------------------

    //---------------------------------------------------------
    // 
    //---------------------------------------------------------
    private static final Object distributedMapsSynchronizeObject = new Object();
    public static Map<String, DistributedObjectCache> distributedMaps = new ConcurrentHashMap<String, DistributedObjectCache>();
    private static CacheService cacheService = null;

    private DistributedObjectCacheFactory() {}

    /**
     * Returns the DistributedMap
     * instance specified by the given id. If
     * the given instance has not yet been created, then a new instance
     * is created using the default parameters.
     * 
     * @param name instance name
     * @return A DistributedMap instance
     * 
     * @see #getMap(String, Properties)
     */
    public static DistributedObjectCache getMap(String name) {
        return getMap(name, new Properties());
    }

    public static DistributedObjectCache removeMap(String name) {
        if (tc.isDebugEnabled())
            Tr.entry(tc, "removeMap", name);

        DistributedObjectCache cache = distributedMaps.remove(name);

        if (tc.isDebugEnabled())
            Tr.exit(tc, "removeMap", cache);

        return cache;

    }

    /**
     * Returns the DistributedMap or DistributedNioMap
     * instance specified by the given id, using the
     * the parameters specified in properties. If the given
     * instance has not yet been created, then a new
     * instance is created using the parameters
     * specified in the properties object. Use the
     * various KEY_xxx and VALUE_xxx
     * constants to populate the passed properties object.
     * 
     * @param name instance name
     * @param properties
     * @return A DistributedObjectCache instance
     */
    public static DistributedObjectCache getMap(String name, Properties properties) {
        final String methodName = "getMap()";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName + " name: (" + name + ") properties:" + properties);
        }

        if (name == null || name.trim().length() == 0) {
            throw new IllegalStateException("Map name can not be null or empty");
        }
        if (ServerCache.objectCacheEnabled == false) {
            // display debug statement instead of warning message because the security tries to create object instance 
            // before object cache service is started.
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " WebSphere Dynamic Cache instance named " + name +
                             " cannot be used because of Dynamic Object cache service has not be started.");
            }
            return null;
        }

        DistributedObjectCache distributedObjectCache = null;
        synchronized (distributedMapsSynchronizeObject) {
            distributedObjectCache = distributedMaps.get(name);
            if (distributedObjectCache == null) {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " Existing DistributedObjectCache not found for " + name);
                }
                distributedObjectCache = createDistributedObjectCache(name, properties);
                distributedMaps.put(name, distributedObjectCache);
            } else {
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " Existing DistributedObjectCache found for " + name + " " + distributedObjectCache);
                }
            }
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName + " map:" + distributedObjectCache);
        }
        return distributedObjectCache;
    }

    public static DistributedObjectCache createDistributedObjectCache(String name, Properties properties) {
        final String methodName = "createDistributedObjectCache()";
        if (tc.isEntryEnabled()) {
            Tr.entry(tc, methodName + " name: " + name + " properties:" + properties);
        }

        if (ServerCache.objectCacheEnabled == false) {
            // display debug statement instead of warning message because the security tries to create object instance 
            // before object cache service is started.
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " WebSphere Dynamic Cache instance named " + name +
                             " cannot be used because of Dynamic Object cache service has not be started.");
            }
            return null;
        }

        DistributedObjectCache distributedObjectCache = null;
        DCache cache = ServerCache.getCache(name);
        if (cache == null) {
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " Existing Cache not found for " + name);
            }
            CacheConfig cacheConfig = (CacheConfig) (cacheService.getCacheInstanceConfig(DCacheBase.DEFAULT_CACHE_NAME)).clone();
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheConfig:" + cacheConfig);
            }
            properties.put(CacheConfig.CACHE_NAME, name);
            cacheConfig.overrideCacheConfig(properties);
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " cacheConfig:" + cacheConfig);
            }
            try {
                cacheService.addCacheInstanceConfig(cacheConfig, true);
                distributedObjectCache = (DistributedObjectCache) ServerCache.cacheUnit.createObjectCache(name);
                if (tc.isDebugEnabled()) {
                    Tr.debug(tc, methodName + " DistributedMap " + name + " created from cache instance " + cache);
                }
            } catch (Exception e) {
                if (tc.isDebugEnabled()) {
                    Tr.warning(tc, methodName + " Exception:" + e.getMessage());
                }
            }
        } else { //ONLY test code shld proceed down this path
            if (tc.isDebugEnabled()) {
                Tr.debug(tc, methodName + " Existing Cache found for " + name + " " + cache);
            }
            if (cache.getCacheConfig().isEnableNioSupport()) {
                distributedObjectCache = new DistributedNioMapImpl(cache);
            } else {
                distributedObjectCache = new DistributedMapImpl(cache);
            }
        }

        if (tc.isEntryEnabled()) {
            Tr.exit(tc, methodName + " map:" + distributedObjectCache);
        }

        return distributedObjectCache;
    }

    public static void setCacheService(CacheService cacheService) {
        DistributedObjectCacheFactory.cacheService = cacheService;
    }

    public static void unsetCacheService(CacheServiceImpl cacheServiceImpl) {
        DistributedObjectCacheFactory.cacheService = null;
    }
}
