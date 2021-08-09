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
package com.ibm.ws.cache.stat.internal;


/**
 * WebSphere Dynamic Cache Stats interface.
 *
 *  Dynamic cache stats are structured as follows in the PMI tree:
 * <br><br>
 *    &lt;server&gt;<br>
 *    |<br>
 *    |__Dynamic Caching+<br>
 *    &nbsp;&nbsp;|<br>
 *    &nbsp;&nbsp;|__&lt;Servlet: instance1&gt;<br>
 *    &nbsp;&nbsp;&nbsp;&nbsp;|__Templates+<br>
 *    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|__&lt;template_1&gt;<br>
 *    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|__&lt;template_2&gt;<br>
 *    &nbsp;&nbsp;&nbsp;&nbsp;|__Disk+<br>
 *    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|__&lt;Disk Offload Enabled&gt;<br>
 *    &nbsp;&nbsp;|<br>
 *    &nbsp;&nbsp;|__&lt;Object: instance2&gt;<br>
 *    &nbsp;&nbsp;&nbsp;&nbsp;|__Object Cache+<br>
 *    &nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;|__&lt;Counters&gt;<br>
 *
 *    <p>+ indicates logical group
 *    <br><br>
 *    {@link com.ibm.websphere.pmi.stat.StatDescriptor} is used to locate and access particular Stats in the PMI tree.
 *    <br>Example:
 *    <ol>
 *    <li>StatDescriptor to represent statistics for cache <i>Servlet: instance1</I> templates group <I>Template_1</I>: <code>new StatDescriptor (new String[] {WSDynamicCacheStats.NAME, "Servlet: instance1", WSDynamicCacheStats.TEMPLATE_GROUP, "Template_1"});</code>
 *    <li>StatDescriptor to represent statistics for cache <i>Servlet: instance1</I> disk group <I>Disk Offload Enabled</I>: <code>new StatDescriptor (new String[] {WSDynamicCacheStats.NAME, "Servlet: instance1", WSDynamicCacheStats.DISK_GROUP, WSDynamicCacheStats.DISK_OFFLOAD_ENABLED});</code>
 *    <li>StatDescriptor to represent statistics for cache <i>Object: instance2</I> object cache group <I>Counters</I>: <code>new StatDescriptor (new String[] {WSDynamicCacheStats.NAME, "Object: instance2", WSDynamicCacheStats.OBJECT_GROUP, WSDynamicCacheStats.OBJECT_COUNTERS});</code>
 *    </ol>
 *    <p>Note: cache instance names are prepended with cache type ("Servlet: " or "Object: ").
 *
 * @see com.ibm.websphere.pmi.stat.StatDescriptor
   @ibm-api
 */

public interface WSDynamicCacheStats {

    /**
     * The name of the Dynamic Cache performance module.
     */
    public static final String NAME = "cacheModule";

    /**
     * The name of the servlet cache logical group.
     * @deprecated As of 6.1, this group is no longer created.
     */
    public static final String SERVLET_CACHE_GROUP = NAME + ".servlet";

    /**
     * The name of the object cache logical group.
     * @deprecated As of 6.1, this group is no longer created. Use OBJECT_GROUP. 
     */
    public static final String OBJECT_CACHE_GROUP  = NAME + ".object";

    /**
     * The name of the template logical group.
     */
    public static final String TEMPLATE_GROUP      = NAME + ".template";

    /**
     * The name of the disk logical group.
     */
    public static final String DISK_GROUP          = NAME + ".disk";

    /**
     * The name of the disk offload eanble under disk logical group.
     */
    public static final String DISK_OFFLOAD_ENABLED = NAME + ".diskOffloadEnabled";

    /**
     * The name of the logical group for object cache.
     */
    public static final String OBJECT_GROUP          = NAME + ".objectCache";

    /**
     * The name of the object counters under object cache logical group.
     */
    public static final String OBJECT_COUNTERS       = NAME + ".counters";

    /**
     * The prefix of the servlet cache instance. The servlet instance names are prepended with this servlet cache type
     */
    public static final String SERVLET_CACHE_TYPE_PREFIX   = "Servlet: ";

    /**
     * The prefix of the object cache instance. The object instance names are prepended with this object cache type
     */
    public static final String OBJECT_CACHE_TYPE_PREFIX    = "Object: ";

    /**
     * The maximum number of in-memory cache entries. (CountStatistic)
     */
    public static final int MaxInMemoryCacheEntryCount            = 1;

    /**
     * The current count of in-memory cache entries. (CountStatistic)
     */
    public static final int InMemoryCacheEntryCount               = 2;

    /**
     * The count of requests for cacheable objects that are served from memory. (templates group or object cache group CountStatistic)
     */
    public static final int HitsInMemoryCount                     = 21;

    /**
     * The count of requests for cacheable objects that are served from disk. (templates group or object cache group CountStatistic)
     */
    public static final int HitsOnDiskCount                       = 22;

    /**
     * The count of explicit invalidations. (templates group or object cache group CountStatistic)
     */
    public static final int ExplicitInvalidationCount             = 23;

    /**
     * The count of cache entries that are removed from memory by a Least Recently Used (LRU) algorithm. (templates group or object cache group CountStatistic)
     */
    public static final int LruInvalidationCount                  = 24;

    /**
     * The count of cache entries that are removed from memory and disk because their timeout has expired. (templates group or object cache group CountStatistic)
     */
    public static final int TimeoutInvalidationCount              = 25;

    /**
     * The current count of used cache entries in memory and disk. (templates group or object cache group CountStatistic)
     */
    public static final int InMemoryAndDiskCacheEntryCount        = 26;

    /**
     * The count of requests for cacheable objects that are served from other Java virtual machines within the replication domain. (templates group or object cache group CountStatistic)
     */
    public static final int RemoteHitCount                        = 27;

    /**
     * The count of requests for cacheable objects that were not found in the cache. (templates group or object cache group CountStatistic)
     */
    public static final int MissCount                             = 28;

    /**
     * The count of requests for cacheable objects that are generated by applications running on this application server. (templates group or object cache group CountStatistic)
     */
    public static final int ClientRequestCount                    = 29;

    /**
     * The count of requests for cacheable objects that are generated by cooperating caches in this replication domain. (templates group or object cache group CountStatistic)
     */
    public static final int DistributedRequestCount               = 30;

    /**
     * The count of explicit invalidations resulting in the removal of an entry from memory. (templates group or object cache group CountStatistic)
     */
    public static final int ExplicitMemoryInvalidationCount       = 31;

    /**
     * The count of explicit invalidations resulting in the removal of an entry from disk. (templates group or object cache group CountStatistic)
     */
    public static final int ExplicitDiskInvalidationCount         = 32;

    /**
     * The count of explicit invalidations generated locally, either programmatically or by a cache policy. (templates group or object cache group CountStatistic)
     */
    public static final int LocalExplicitInvalidationCount        = 34;

    /**
     * The count of explicit invalidations received from a cooperating Java virtual machine in this replication domain. (templates group or object cache group CountStatistic)
     */
    public static final int RemoteExplicitInvalidationCount       = 35;

    /**
     * The count of cache entries that are received from cooperating dynamic caches. (templates group or object cache group CountStatistic)
     */
    public static final int RemoteCreationCount                   = 36;

    /**
     * The current count of cache entries that are currently on disk. (Disk Group CountStatistic)
     */
    public static final int ObjectsOnDisk                         = 4;

    /**
     * The count of requests for cacheable objects that are served from disk. (Disk Group CountStatistic)
     */
    public static final int HitsOnDisk                            = 5;

    /**
     * The count of explicit invalidations resulting in the removal of an entry from disk. (Disk Group CountStatistic)
     */
    public static final int ExplicitInvalidationsFromDisk         = 6;

    /**
     * The count of cache entries that are removed from disk because their timeout has expired. (Disk Group CountStatistic)
     */
    public static final int TimeoutInvalidationsFromDisk          = 7;

    /**
     * The current count of cache entries that are pending to be removed from disk. (Disk Group CountStatistic)
     */
    public static final int PendingRemovalFromDisk                = 8;

    /**
     * The current count of dependency ids that are currently on disk. (Disk Group CountStatistic)
     */
    public static final int DependencyIdsOnDisk                   = 9;

    /**
     * The current count of dependency ids that are buffered for disk. (Disk Group CountStatistic)
     */
    public static final int DependencyIdsBufferedForDisk          = 10;

    /**
     * The current count of dependency ids that are offloaded to disk. (Disk Group CountStatistic)
     */
    public static final int DependencyIdsOffloadedToDisk          = 11;

    /**
     * The current count of dependency Id based invalidations from disk. (Disk Group CountStatistic)
     */
    public static final int DependencyIdBasedInvalidationsFromDisk = 12;

    /**
     * The current count of templates that are currently on disk. (Disk Group CountStatistic)
     */
    public static final int TemplatesOnDisk                        = 13;

    /**
     * The current count of templates that are buffered for disk. (Disk Group CountStatistic)
     */
    public static final int TemplatesBufferedForDisk               = 14;

    /**
     * The current count of templates that are offloaded to disk. (Disk Group CountStatistic)
     */
    public static final int TemplatesOffloadedToDisk               = 15;

    /**
     * The current count of template based invalidations from disk. (Disk Group CountStatistic)
     */
    public static final int TemplateBasedInvalidationsFromDisk     = 16;

    /**
     * The count of garbage collector invalidations resulting in the removal of entries from disk cache due to high threshold has reached. (Disk Group CountStatistic)
     */
    public static final int GarbageCollectorInvalidationsFromDisk  = 17;

    /**
     * The count of invalidations resulting in the removal of entries from disk cache due to exceeding the disk cache size or disk cache size in GB limit. (Disk Group CountStatistic)
     */
    public static final int OverflowInvalidationsFromDisk          = 18;

}
