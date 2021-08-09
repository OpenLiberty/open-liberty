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
 * This interface is used to monitor the cache. It is utilized by the
 * WebSphere Cache Monitor application shipped as a part of the WAS install image
 * to administer and monitor the contents of the {@link CoreCache}
 *
 * @since WAS7.0
 * @ibm-spi
 */
public interface CacheStatistics {

	/**
	 * Statistics name: CacheHits
	 * <p>
	 * Description: The total number of cache hits.
	 * 
	 * @return The total number of cache hits.
	 */
	public long getCacheHitsCount();

	/**
	 * Statistics name: CacheLruRemoves
	 * <p>
	 * Description: The number of memory-based least recently used (LRU) evictions.
	 * These correspond to the number of objects that are evicted from the memory cache,
	 * based on the LRU policy.
	 * 
	 * @return The number of objects that are removed by LRU evictions
	 */
	public long getCacheLruRemovesCount();

	/**
	 * Statistics name: CacheMisses
	 * <p>
	 * Description: The total number of cache misses.
	 * 
	 * @return The total number of cache misses.
	 */
	public long getCacheMissesCount();

	/**
	 * Statistics name: CacheRemoves
	 * <p>
	 * Description: The total number of cache removes.
	 * 
	 * @return The total number of cache removes.
	 */
	public long getCacheRemovesCount();

	/**
	 * Statistics name: ExplicitInvalidationsFromMemory
	 * <p>
	 * Description: Metric that captures the number of explicit invalidations that result
	 * in an entry being removed from memory.
	 * 
	 * @return The total number of explicitly triggered invalidations from memory.
	 */
	public long getExplicitInvalidationsFromMemoryCount();

	/**
	 * Extended cache statistics specific to the cache provider
	 *
	 * @return {@link Map} of {cache statistic name --> Cache statistic value}
	 **/
	public Map<String, Number> getExtendedStats();

	/**
	 * Statistics name: MemoryCacheEntries
	 * <p>
	 * Description: The number of cache entries in memory.
	 * 
	 * @return The number of cache entries in memory.
	 */
	public long getMemoryCacheEntriesCount();

	/**
	 * Statistics name: MemoryCacheSizeInMB
	 * <p>
	 * Description: The size of the cache in terms of memory occupied on the JVM heap.
	 *
	 * @return The amount of JVM heap in MB occupied by the cache
	 */
	public float getMemoryCacheSizeInMBCount();

	/**
	 * Statistics name: TimeoutInvalidationsFromMemory
	 * <p>
	 * Description: Metric that captures the number of timeout invalidations that result in an entry being removed from memory.
	 * 
	 * @return The total number of timeout invalidations from memory.
	 */
	public long getTimeoutInvalidationsFromMemoryCount();
	
	/**
	 * This is used to reset all statistics counters in the cache proivder excluding:
	 * <ol>
	 * <li>MemoryCacheEntries
	 * <li>MemoryCacheSizeInMB
	 * </ol>
	 */
	public void reset();

}
