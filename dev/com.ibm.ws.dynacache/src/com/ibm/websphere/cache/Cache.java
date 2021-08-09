/*******************************************************************************
 * Copyright (c) 1997, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.cache;

import java.util.Collection;
import java.util.Enumeration;

/**
 * This is the underlying cache mechanism that is
 * used by the servlet, JSP, webservices, command
 * cache. It contains the methods used to inspect
 * and manage the current state of the cache.
 * 
 * @see DynamicCacheAccessor#getDistributedMap
 * @see DistributedMap
 * @see DistributedObjectCache
 * @deprecated Use DistributedMap to store and manage objects
 *             in cache. DynamicCacheAccessor#getDistributedMap
 *             will return a DistributedMap for accessing
 *             base cache.
 * @ibm-api 
 */
public interface Cache {

    /**
     * This returns the cache entry identified by
     * the specified entry info. It returns null
     * if not in the cache.
     * 
     * @param entryInfo The entry info object for the entry to be found.
     *                  It cannot be null.
     * @return The entry indentified by the cache id.
     * @see DistributedMap
     * @see DistributedObjectCache
     * @deprecated Use DistributedMap to store and manage objects 
     *             in cache.
     * @ibm-api 
     */
    public CacheEntry getEntry(EntryInfo entryInfo);

    /**
     * This returns the cache entry identified by the
     * specified cache id. It returns null if not in 
     * the cache.
     * 
     * @param id     The cache id object for the entry to be found.
     *               It cannot be null.
     * @return The entry indentified by the cache id.
     * @see DistributedMap
     * @deprecated Use DistributedMap to store and manage objects 
     *             in cache.
     * @ibm-api 
     */
    public CacheEntry getEntry(String id);

    /**
     * This tries to find a value in the cache.
     * If it is not there, it will try to execute it.
     * 
     * @param entryInfo The entry info object for the entry to be found.
     *                  It cannot be null.
     * @param askPermission
     *                  True implies that execution must ask the
     *                  coordinating CacheUnit for permission.
     * @return Value of the cache entry idetified by the entry 
     *         info.
     * @see DistributedMap
     * @deprecated Use DistributedMap to store and manage objects 
     *             in cache.
     * @ibm-api 
     */
    public Object getValue(EntryInfo entryInfo, boolean askPermission);

    /**
     * This method tries to find a value in the cache.
     * If it is not there, it will try to execute it.
     *
     * @param id The cache id for the entry to be found.  It cannot be null.
     * @param askPermission True implies that execution must ask the
     * coordinating CacheUnit for permission.
     * @see DistributedMap
     * @deprecated Use DistributedMap to store and manage objects 
     *             in cache.
     * @ibm-api 
     */
    public Object getValue(String id, boolean askPermission);

    /**
     * This method tries to find a value in the cache.
     * If it is not there, it will try to execute it.
     *
     * @param id The cache id for the entry to be found.  It cannot be null.
     * @param askPermission True implies that execution must ask the
     * coordinating CacheUnit for permission.
     * @see DistributedMap
     * @deprecated Use DistributedMap to store and manage objects 
     *             in cache.
     * @ibm-api 
     */
    public Object getValue(Object id, boolean askPermission);

   /**
     * This method invalidates in all caches all entries dependent on the specified
     * id.
     *
     * @param id The cache id or data id.
     * @param waitOnInvalidation True indicates that this method should
     * not return until the invalidations have taken effect on all caches.
     * False indicates that the invalidations will be queued for later
     * batch processing.
     * @see DistributedMap
     * @deprecated Use DistributedMap to store and manage objects 
     *             in cache.
     * @ibm-api 
     */
    public void invalidateById(String id, boolean waitOnInvalidation);

   /**
     * This method invalidates in all caches all entries dependent on the specified
     * template.
     *
     * @param template The template name.
     * @param waitOnInvalidation True indicates that this method should
     * not return until the invalidations have taken effect on all caches.
     * False indicates that the invalidations will be queued for later
     * batch processing.
     * @see DistributedMap
     * @deprecated Use DistributedMap to store and manage objects 
     *             in cache.
     * @ibm-api 
     */
    public void invalidateByTemplate(String template,boolean waitOnInvalidation);

   /**
     * This method clears everything from the cache,
     * so that it is just like when it was instantiated.
     * @see DistributedMap
     * @deprecated Use DistributedMap to store and manage objects 
     *             in cache.
     * @ibm-api 
     */
    public void clear();

   /**
     * This method returns the cache ids for all cache entries.
     *
     * @return The Enumeration of cache ids.
     * @see DistributedMap
     * @deprecated Use DistributedMap to store and manage objects 
     *             in cache.
     * @ibm-api 
     */
    public Enumeration getAllIds();

   /**
     * This method gets the maximum number of cache entries.
     *
     * @return The maximum number of cache entries.
     * @see DistributedMap
     * @deprecated Use DistributedMap to store and manage objects 
     *             in cache.
     * @ibm-api 
     */
    public int getMaxNumberCacheEntries();


    /**
     * This method gets the current number of cache entries.
     *
     * @return The current number of cache entries.
     * @see DistributedMap
     * @deprecated Use DistributedMap to store and manage objects 
     *             in cache.
     * @ibm-api 
     */
    public int getNumberCacheEntries();


    /**
     * This method gets the default priority value as set in the Admin GUI/dynacache.xml file.
     *
     * @return The default priority for this appserver.
     * @see DistributedMap
     * @deprecated Use DistributedMap to store and manage objects 
     *             in cache.
     * @ibm-api 
     */
    public int getDefaultPriority();

    /**
     * This method returns the dependency ids for all cache entries.
     *
     * @return A Collection of dependency ids.
     * @see DistributedMap
     * @deprecated Use DistributedMap to store and manage objects 
     *             in cache.
     * @ibm-api 
     */
    public Collection getAllDependencyIds(); 

    /**
     * This method returns the cache ids of the entries dependent on the dependency id 
     * passed as a parameter or null if no entry depends on it.
     *
     * @param dependency ID for which Cache IDs are needed.
     * @return A Collection of cache IDs or null.
     * @see DistributedMap
     * @deprecated Use DistributedMap to store and manage objects 
     *             in cache.
     * @ibm-api 
     */
    public Collection getCacheIdsByDependency(String dependency);

    /**
     * This method returns the cache ids of the entries that have the template
     * passed as a parameter or null if no entry has this template.
     *
     * @param template for which Cache IDs are needed.
     * @return A Collection of cache IDs or null.
     * @see DistributedMap
     * @deprecated Use DistributedMap to store and manage objects 
     *             in cache.
     * @ibm-api 
     */
    public Collection getCacheIdsByTemplate(String template);
}
