/*******************************************************************************
 * Copyright (c) 1997, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.cache.stat;


public interface CachePerf {

    // Primary internal set of constants for cause
   public static final int UNSET    = 0;
   public static final int DIRECT   = 1;
   public static final int LRU      = 2;
   public static final int TIMEOUT  = 3;
   // public static final int DISK_TIMEOUT  = 4;  // CPF-Inactivity
   // public static final int CLEAR_ALL     = 5;  // CPF-Inactivity
   public static final int INACTIVE = 6;          // CPF-Inactivity
   public static final int DISK_GARBAGE_COLLECTOR = 7; // 321649
   public static final int DISK_OVERFLOW          = 8; // 321649

   // Primary internal set of constants for source
   public static final int MEMORY = 1;
   public static final int REMOTE = 2;
   public static final int DISK   = 3;
   public static final int NOOP   = 4;
   public static final int LOCAL  = 5;

    /* reports whether PMI reporting is set to a sufficiently high value for the cache
     * to report info.
     *
     * @return whether the cache should report info to PMI
     */
    public boolean isPMIEnabled();

    /*
     * resets the PMI counters to zero
     */
    public void resetPMICounters();

    /*
     * updates the global statistics for cache size.  updates the two supplied arguments,
     * and calculates the total # of entries in memory and on disk.
     *
     * @param max Maximum # of entries that can be stored in memory
     * @param current Current # of in memory cache entries
     */
    public void updateCacheSizes(long max, long current);
    
    /*
     * updates the global statistics for disk information 
     *
     * @param objectsOnDisk number of cache entries that are currently on disk
     * @param pendingRemoval number of objects that have been invalidated by are yet to be removed from disk
     * @param depidsOnDisk number of dependency ids that are currently on disk
     * @param depidsBuffered number of dependency ids that have been currently bufferred in memory for the disk
     * @param depidsOffloaded number of dependency ids offloaded to disk
     * @param depidBasedInvalidations number of dependency id based invalidations
     * @param templatesOnDisk number of templates that are currently on disk
     * @param templatesBuffered number of templates that have been currently bufferred in memory for the disk
     * @param templatesOffloaded number of templates offloaded to disk
     * @param templateBasedInvalidations number of template based invalidations
     */
    public void updateDiskCacheStatistics(long objectsOnDisk, long pendingRemoval, 
                                          long depidsOnDisk, long depidsBuffered, long depidsOffloaded, long depidBasedInvalidations,
                                          long templatesOnDisk, long templatesBuffered, long templatesOffloaded, long templateBasedInvalidations);
    
    /*
     * updates global and by template cache size statistics.
     *
     * @param template the template of the cache entry that was  
     *                 created.  If null, template processing will not happen
     * @param source whether the creation was generated internally or remotely
    */ 
    public void onEntryCreation(String template, int source);

    /*
   * registers that a request came into the cache for a cacheable object
   *
   * @param template the template of the cache entry that was hit
   * @param source whether the invalidation was generated internally or remotely
   */
   public void onRequest(String template, int source);

    /*
     * updates global and by template cache hit statistics.  Also updates the 
     * total # of cache entries. Counts both local and remote hits. 
     * Because remote misses only happen after local misses,
     * remote misses are not counted in the total or by template counts
     *
     * @param template the template of the cache entry that was 
     *                 hit.  If null, template processing will not happen
     */
    public void onCacheHit(String template, int locality);

    /*
     * updates global and by template cache hit statistics.  Also updates the 
     * total # of cache entries.  Because remote misses only happen after local misses,
     * remote misses are not counted in the total or by template counts
     *
     * @param template the template of the cache entry that was  
     *                 missed.  If null, template processing will not happen
     * @param locality Whether the miss was local or remote
     */
    public void onCacheMiss(String template, int locality);

    /*
    * updates global and by template invalidation statistics.  Also updates the 
    * total # of cache entries.  if the cause is LRU, then locality will indicate 
    * whether the entry was passivated to disk (with the DISK constant) or not (anything else)
    *
    * @param template the template of the cache entry that was hit
    * @param cause the cause of invalidation
    * @param locality whether the invalidation occurred on disk, in memory, or neither
    * @param source whether the invalidation was generated internally or remotely
    */
   public void onInvalidate(String template, int cause, int locality, int source);
    
   /*
    * updates global and by template invalidation statistics.  Also updates the 
    * total # of cache entries.  if the cause is LRU, then locality will indicate 
    * whether the entry was passivated to disk (with the DISK constant) or not (anything else)
    *
    * @param template the template of the cache entry that was hit
    * @param cause the cause of invalidation
    * @param locality whether the invalidation occurred on disk, in memory, or neither
    * @param source whether the invalidation was generated internally or remotely
    * @param dels  number of invalidations that occurred
    */
   public void batchOnInvalidate(String template, int cause, int locality, int source, int dels);
 
   /*
    * updates statistics when clear cache is invoked.
    *
    * @param memory boolean to indicate memory cache is cleared
    * @param memory boolean to indicate disk cache is cleared
    */
   public void onCacheClear(boolean memory, boolean disk);

   /*
    * Unregister ALL PMI modules when the cache stops
    * 
    * @throws StatsFactoryException
    */
   void removePMICounters();

}
