/*******************************************************************************
 * Copyright (c) 1997, 2007 IBM Corporation and others.
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

/**
 * This interface is used to provide the cache instance configuration to a cache provider. This 
 * configuration is provided to the {@link CacheProvider} via the 
 * {@link CacheProvider#createCache(CacheConfig)} method.
 *
 * @ibm-spi
 * @since WAS 6.1.0.27
 */
public interface CacheConfig {

	/**
	 * Returns the name of cache instance.
	 *
	 * @return Name of cache instance.
	 * @ibm-api
	 */
	public String getCacheName();

	/**
	 * Returns the maximum number of cache entries allowed in the memory.
	 *
	 * @return The maximum number of cache entries allowed.
	 * @ibm-api
	 */
	public long getMaxCacheSize();

	/**
	 * Returns the maximum space on the JVM heap that can be occupied by
	 * the cache entries.
	 *
	 * @return The maximum size of cache allowed in terms of JVM heap.
	 * @ibm-api
	 */
	public long getMaxCacheSizeInMB();

	/**
	 * Returns the high threshold in percentage of JVM heap space that can be occupied by
	 * the cache entries.
	 *
	 * @return The high threshold of JVM heap space that can be occupied by the cache entries
	 * @ibm-api
	 */
	public int getHighThresholdCacheSizeInMB();

	/**
	 * Returns the low threshold in percentage of JVM heap space that can be occupied by
	 * the cache entries.
	 *
	 * @return The low threshold of JVM heap space that can be occupied by the cache entries
	 * @ibm-api
	 */
	public int getLowThresholdCacheSizeInMB();

	/**
	 * Returns the type of evictor algorithm. 
	 *
	 * @return The type of evictor algorithm.
     * @see EvictorAlgorithmType
	 * @ibm-api
	 */
	public EvictorAlgorithmType getEvictorAlgorithmType();

	/**
	 * Eviction algorithm type
	 * <ul>
	 * <li>EvictorAlgorithmType.LRUEvictor is defined as an evictor type that
	 * uses a least recently used algorithm to decide which entries to evict
	 * when the cache entries map exceeds a maximum number of entries.
	 * <li>EvictorAlgorithmType.LFUEvictor is defined as an evictor type that
	 * uses a least frequently used algorithm to decide which entries to evict
	 * when the cache entries map exceeds a maximum number of entries.
	 * </ul>
	 */
	public enum EvictorAlgorithmType {

		/**
		 * An Evictor that uses the Least Recently Used Algorithm
		 */
		LRUEvictor,

		/**
		 * An Evictor that uses the Least Frequently Used Algorithm
		 */
		LFUEvictor
	}
	
	/**
	 * Indicates if this cache is meant to be distributed across the 
	 * server instances of the cache provider.
	 * 
	 * @return true - Replication is enabled for this cache instance configuration
	 * @ibm-api
	 */
	public boolean isDistributed();
	
	/**
	 * 
	 * Returns additional properties configured for the cacheinstance via the
	 * WebSphere admin console, wsadmin, the cacheinstances.properties file or 
	 * the DistributedObjectCacheFactory.getMap(String name, Properties properties) 
	 * API.
	 * 
	 * @return additional properties used for configuring the cache provider's cache instance 
	 */
	public Map<String, String> getProperties();

}