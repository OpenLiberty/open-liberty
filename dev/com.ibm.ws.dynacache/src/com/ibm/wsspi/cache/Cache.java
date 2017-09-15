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

import java.util.Collection;
import java.util.Enumeration;

import com.ibm.ws.cache.intf.DCache;

/**
 * This is the underlying cache mechanism that is
 * used by the CacheMonitor. It contains the methods used to inspect
 * and manage the current state of the cache.
 * @ibm-spi 
 */
public class Cache {
   
	private DCache cacheInstance = null;
	
	public Cache(com.ibm.websphere.cache.Cache ci){
        cacheInstance = (DCache)ci;
    }
	
	/**
     * This method moves the specified entry to the end of the LRU queue.
     * 
	 * @param ce The cache entry
	 */
	public void refreshEntry(com.ibm.wsspi.cache.CacheEntry ce){
		cacheInstance.refreshEntry(ce.cacheEntry);
	}

	/**
     * This method determines the disk offloaded feature is enabled or not.
     * 
	 * @return True if disk offload feature is enabled. False if disk offload is disabled.
	 */
	public boolean getSwapToDisk() {
        return cacheInstance.getSwapToDisk();
    }
	
	/**
     * This method returns the cache entry specified by cache ID from the disk cache.
     * 
	 * @param cacheId the cache ID
	 * @return The cache entry. NULL if cache ID does not exist.
	 */
	public CacheEntry getEntryDisk(Object cacheId) {
		CacheEntry cacheEntry = new CacheEntry(cacheInstance.getEntryDisk(cacheId));	 
		return cacheEntry;
	}
	
    /**
     * This method clears everything from the disk cache.
     */
	public void clearDisk(){
		cacheInstance.clearDisk();	
	}
	
    /**
     * This method returns the cache IDs found in the disk cache based on specified the index and the length.
     * If index = 1 or -1, the set might contain "DISKCACHE_MORE" to indicate there are more cache IDs in the disk cache.
     * The caller need to remove DISKCACHE_MORE" from the set before it is being used.
     * 
     * @param index If index = 0, it starts the beginning. If index = 1, it means "next". If Index = -1, it means "previous".
     * @param length The max number of cache IDs to be read. If length = -1, it reads all cache IDs until the end.
     * @return The collecton of cache IDs.
     */
	public Collection getIdsByRangeDisk(int index, int length){
		return cacheInstance.getIdsByRangeDisk(index, length);
	}
	
    /**
     * This method returns the templates found in the disk cache based on specified the index and the length.
     * If index = 1 or -1, the set might contain "DISKCACHE_MORE" to indicate there are more templates in the disk cache.
     * The caller need to remove DISKCACHE_MORE" from the set before it is being used.
     * 
     * @param index If index = 0, it starts the beginning. If index = 1, it means "next". If Index = -1, it means "previous".
     * @param length The max number of templates to be read. If length = -1, it reads all templates until the end.
     * @return The collecton of templates.
     */
	public Collection getTemplatesByRangeDisk(int index, int length){
		return cacheInstance.getTemplatesByRangeDisk(index, length);
	}
	
	/**
	 * This method returns an instance of CacheStatisticsListener.
	 * @return The instance of CacheStatisticsListener
	 */
	public com.ibm.wsspi.cache.CacheStatisticsListener getCacheStatisticsListener(){
		return new CacheStatisticsListener(cacheInstance.getCacheStatisticsListener());
	}
	
	/**
	 * This method returns an instance of CacheEntry specified cache ID.
	 * 
	 * @param cacheId the cache ID
	 * @return The instance of CacheEntry
	 */
	public com.ibm.wsspi.cache.CacheEntry getEntry(Object cacheId){
		CacheEntry cacheEntry = new CacheEntry(cacheInstance.getEntry(cacheId));	 
		return cacheEntry;
	}

    /**
     * This method invalidates in all caches all entries dependent on the specified
     * id.
     *
     * @param id The cache id or data id.
     * @param waitOnInvalidation True indicates that this method should
     * not return until the invalidations have taken effect on all caches.
     * False indicates that the invalidations will be queued for later
     * batch processing.
     */
    public void invalidateById(String id, boolean waitOnInvalidation){
		cacheInstance.invalidateById(id, waitOnInvalidation);
	}

    /**
     * This method invalidates in all caches all entries dependent on the specified
     * template.
     *
     * @param template The template name.
     * @param waitOnInvalidation True indicates that this method should
     * not return until the invalidations have taken effect on all caches.
     * False indicates that the invalidations will be queued for later
     * batch processing.
     */
    public void invalidateByTemplate(String template,boolean waitOnInvalidation){
		cacheInstance.invalidateByTemplate(template, waitOnInvalidation);
	}

    /**
     * This method clears everything from the cache,
     * so that it is just like when it was instantiated.
     */
    public void clear(){
		cacheInstance.clear();
	}

    /**
     * This method returns the cache IDs for all cache entries from memory cache.
     *
     * @return The Enumeration of cache ids.
     */
    public Enumeration getAllIds(){
		return cacheInstance.getAllIds();
	}

    /**
     * This method returns the maximum number of cache entries that are held in memory cache.
     *
     * @return The maximum of cache entries.
     */
    public int getMaxNumberCacheEntries(){
		return (int)cacheInstance.getCacheConfig().getMaxCacheSize();
	}
    
    /**
     * This method returns the current number of cache entries for this cache instance.
     *
     * @return The current number of cache entries.
     */
    public int getNumberCacheEntries(){
		return cacheInstance.getNumberCacheEntries();
	}
    
	/**
	 * This method returns the maximum space on the JVM heap that can be occupied by
	 * the cache entries.
	 *
	 * @return The maximum size of cache allowed in terms of JVM heap.
	 */
    public int getMaxCacheSizeInMB(){
		return (int)cacheInstance.getCacheConfig().getMaxCacheSizeInMB();
	}
    
	/**
	 * This method returns the current space on the JVM heap that is occupied by
	 * the cache entries.
	 *
	 * @return The current size of cache in terms of JVM heap.
	 */
    public float getMemoryCacheSizeInMB(){
    	return cacheInstance.getCurrentMemoryCacheSizeInMB();
    }
    
    /**
     * This method returns the default priority value as set in the Administrator console GUI/dynacache.xml file.
     *
     * @return The default priority 
     */
    public int getDefaultPriority(){
		return cacheInstance.getCacheConfig().getCachePriority();
	}

    /**
     * This method returns the dependency IDs for all cache entries in the memory cache.
     *
     * @return A Collection of dependency IDs.
     */
    public Collection getAllDependencyIds(){
    	return cacheInstance.getAllDependencyIds();
    }

    /**
     * This method returns the cache IDs of the entries in the memory cache specified by the dependency ID. 
     *
     * @param dependency ID for the group of cache IDs.
     * @return A Collection of cache IDs
     */
    public Collection getCacheIdsByDependency(String dependency){
    	return cacheInstance.getCacheIdsByDependency(dependency);
    }
    
    /**
     * This method returns the cache IDs of the entries in the memory cache specified by the template. 
     *
     * @param template for the group of cache IDs.
     * @return A Collection of cache IDs
     */
    public Collection getCacheIdsByTemplate(String template){
    	return cacheInstance.getCacheIdsByTemplate(template);
    }
    
    /**
     * This method returns the current number of templates in the disk cache.
     * 
     * @return The current number of templates
     */
    public int getTemplatesSizeDisk(){
    	return cacheInstance.getTemplatesSizeDisk();
    }
    
    /**
     * This method returns the cache IDs of the entries in the disk cache specified by the template. 
     *
     * @param template for the group of cache IDs.
     * @return A Collection of cache IDs
     */
    public Collection getTemplateValueDisk(String template){
    	return cacheInstance.getCacheIdsByTemplateDisk(template);
    }
    
    /**
     * This method returns the current number of cache entries in the disk cache.
     * 
     * @return The current number of cache entries
     */
    public int getIdsSizeDisk(){
    	return cacheInstance.getIdsSizeDisk();
    }
    
    /**
     * This method returns the current number of dependency IDs in the disk cache.
     * 
     * @return The current number of dependency IDs
     */
    public int getDepIdsSizeDisk(){
    	return cacheInstance.getDepIdsSizeDisk();
    }

    /**
     * This method returns the dependency IDs found in the disk cache based on specified the index and the length.
     * If index = 1 or -1, the set might contain "DISKCACHE_MORE" to indicate there are more dependency IDs in the disk cache.
     * The caller need to remove DISKCACHE_MORE" from the set before it is being used.
     * 
     * @param index If index = 0, it starts the beginning. If index = 1, it means "next". If Index = -1, it means "previous".
     * @param length The max number of dependency IDs to be read. If length = -1, it reads all dependency IDs until the end.
     * @return The collecton of dependency IDs.
     */
    public Collection getDepIdsByRangeDisk(int index, int length){
		return cacheInstance.getDepIdsByRangeDisk(index, length);
	}
    
    /**
     * This method returns the cache IDs of the entries in the disk cache specified by the dependency ID. 
     *
     * @param depId for the group of cache IDs.
     * @return A Collection of cache IDs
     */
    public Collection getDepIdValueDisk(Object depId) {
    	return cacheInstance.getCacheIdsByDependencyDisk(depId);
    }
    
    /**
     * This method returns the maximum number of cache entries that are held in disk cache.
     *
     * @return The maximum of disk cache entries.
     */
    public long getDiskCacheSizeLimit() {      
    	return cacheInstance.getCacheConfig().getDiskCacheSize();   
    }
    
    /**
     * This method returns the maximum number of disk cache size in gigabytes (GB).
     *
     * @return The maximum of disk cache size in GB.
     */
    public long getDiskCacheSizeInGBLimit() {
        return cacheInstance.getCacheConfig().getDiskCacheSizeInGB();
    }

    /**
     * This method returns the maximum size of an individual cache entry in megabytes (MB). 
     * Any cache entry that is larger than this, when evicted from memory, will not be offloaded
     * to disk.
     *
     * @return The maximum of disk cache entry size in MB.
     */
    public long getDiskCacheEntrySizeInMBLimit() {
    	return cacheInstance.getCacheConfig().getDiskCacheEntrySizeInMB();
    }

    /**
     * This method returns a boolean to indicate whether in-memory cached objects are saved to disk when the server stops.
     *
     * @return boolean flushToDiskOnStop true or false 
     */
    public boolean getFlushToDiskOnStop() {
    	return cacheInstance.getCacheConfig().isFlushToDiskOnStop();
    }

    /**
     * This method returns the performance level to tune the performance of the disk cache.
     *
     * @return Peformance level 0=low 1=balance 2=custom 3=high 
     */
    public int getDiskCachePerformanceLevel() {
    	return cacheInstance.getCacheConfig().getDiskCachePerformanceLevel();
    }

    /**
     * This method returns a value for the disk cache cleanup frequency, in minutes. 
     * If this value is set to 0, the cleanup runs only at midnight. This setting applies 
     * only when the Disk Offload Performance Level is low, balanced, or custom. 
     * The high performance level does not require disk cleanup, and this value is ignored. 
     *
     * @return Cleanup frequency in minutes 
     */
    public long getCleanupFrequency() {
    	return cacheInstance.getCacheConfig().getCleanupFrequency();
    }

    /**
     * This method returns a boolean to indicate whether the disk cache is using buffers for dependency 
     * IDs and templates.  
     *
     * @return boolean delayOffload true or false 
     */
    public boolean getDelayOffload() {
    	return cacheInstance.getCacheConfig().isDelayOffload();
    }

    /**
     * This method returns a value for the maximum number of cache identifiers that are stored for an 
     * individual dependency ID or template in the disk cache metadata in memory. If this limit is exceeded, 
     * the information is offloaded to the disk. This setting applies only when the disk offload performance 
     * level is custom. 
     *
     * @return Delay offload entries limit 
     */
    public long getDelayOffloadEntriesLimit() {
    	return cacheInstance.getCacheConfig().getDelayOffloadEntriesLimit();
    }

    /**
     * This method returns a value for the maximum number of dependency identifier buckets in the disk cache 
     * metadata in memory. If this limit is exceeded, the information is offloaded to the disk. This setting 
     * applies only when the disk cache performance level is custom. 
     *
     * @return Delay offload dependency id buckets 
     */
    public long getDelayOffloadDepIdBuckets() {      
    	return cacheInstance.getCacheConfig().getDelayOffloadDepIdBuckets();      
    }
    
    /**
     * This method returns a value for the maximum number of template buckets in the disk cache 
     * metadata in memory. If this limit is exceeded, the information is offloaded to the disk. This setting 
     * applies only when the disk cache performance level is custom. 
     *
     * @return Delay offload template buckets 
     */
    public long getDelayOffloadTemplateBuckets() {       
    	return cacheInstance.getCacheConfig().getDelayOffloadTemplateBuckets();      
    }
   
    /**
     * This method returns the eviction algorithm that the disk cache will use to evict entries 
     * once the high threshold is reached.
     *
     * @return Eviction policy 0=disable 1=random 2:size
     */
    public int getDiskCacheEvictionPolicy() {       
    	return cacheInstance.getCacheConfig().getDiskCacheEvictionPolicy();                  
    }              

    /**
     * This method returns the high threshold is expressed in terms of the percentage of the disk cache 
     * size in GB or entries. The high value is used when limit disk cache size in GB and limit disk
     * cache size in entries are specified.
     *
     * @return High threshold in percentage 
     */
    public int getDiskCacheHighThreshold() {     
    	return cacheInstance.getCacheConfig().getDiskCacheHighThreshold();                
    }         

    /**
     * This method returns the low threshold is expressed in terms of the percentage of the disk cache 
     * size in GB or entries. The lower value is used when limit disk cache size in GB and limit disk
     * cache size in entries are specified.
     *
     * @return Low threshold in percentage 
     */
    public int getDiskCacheLowThreshold() {   
    	return cacheInstance.getCacheConfig().getDiskCacheLowThreshold();         
    }     
    
    /**
     * This method returns the current disk cache size in Megabytes (MB).
     *
     * @return current disk cache size in MB
     */
    public float getDiskCacheSizeInMBs(){
    	return cacheInstance.getDiskCacheSizeInMBs();
    }
  
}