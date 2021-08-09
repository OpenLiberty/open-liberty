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

import java.util.Set;

import com.ibm.websphere.cache.CacheEntry;
import com.ibm.websphere.cache.EntryInfo;

/**
 * This class is the underlying cache interface for a cache provider, and
 * contains the methods used to get, put, inspect and manage the
 * current state of the cache. Methods of the CoreCache are called for
 * caching of servlets, JSPs, webservices, WebSphere Commands and POJOs
 * in DistributedMap and DistributedNioMap. In relation to Dynacache, each
 * instance of the CoreCache interface represents a cache instance.
 * 
 * <p><h3>Implementation Details</h3>
 * <ul>
 * 
 * <li>If the implementor of the CoreCache intends to return a copy (not just a reference) of the {@link CacheEntry} when a <code>CacheEntry get(Object cacheId) </code> is issued,
 * then the provider does <b>NOT</b> need to follow the
 * implementation guidelines for reference counting specified in the javadoc of this class's methods.
 * A CacheEntry is created by the provider using the cache ID and value passed into the
 * <code>put(EntryInfo ei, Object value);</code> method.
 * 
 * <li>The Dynacache code that calls the CoreCache does NOT synchronize on ANY of
 * the methods of the CoreCache. The cache provider's implementation of the
 * CoreCache is expected to apply the cache provider's locking policies on the
 * operations of the CoreCache.
 * </ul>
 * 
 * @ibm-spi
 * @since WAS 6.1.0.27
 */
public interface CoreCache {

    /**
     * This method clears everything from the cache, so that it returns to the
     * state it was in when it was instantiated.
     * 
     */
    public void clear();

    /**
     * Returns true if memory cache contains a mapping for the specified cache
     * ID.
     * 
     * @param cacheId cache ID is to be tested.
     * @return <code>true</code> - memory cache contains the specified
     *         cacheID.
     */
    public boolean containsCacheId(Object cacheId);

    /**
     * Returns the CacheEntry identified by the specified cache ID. A CacheEntry
     * holds the cache ID(i.e. the key), value and the metadata for a cached
     * item.
     * 
     * <p>
     * Implementation Guidelines: On a <code>get</code> the implementation of
     * the {@link CacheEntry} should pin the entry in the Cache. The internal
     * reference count of the CacheEntry should be incremented.
     * <code> CacheEntry.incrementReferenceCount();</code>
     * 
     * @param cacheId
     *            The cache ID object for the entry to be found. It cannot be
     *            <code>null</code>.
     * @return The entry identified by the cache ID. <code>null</code> if the
     *         cache ID is not found in the cache.
     */
    public CacheEntry get(Object cacheId);

    /**
     * Returns a set view of the cache IDs contained in the memory cache.
     * 
     * @return A {@link Set} of cache IDs or empty set if there is no cache ID.
     */
    public Set<Object> getCacheIds();

    /**
     * Returns the cache IDs that are associated with a dependency ID. It
     * returns an empty set if no cache ID is associated with it.
     * <p>
     * <br>
     * Dependency elements are used to group related cache items. Cache entries
     * having the same depenendency ID are managed as a group. Each related
     * cache item shares the same dependency id, so it only takes one member of
     * the dependency group to get invalidated, for the rest of the group to be
     * evicted. The dependency ID can be as simple as just a name such as
     * storeId
     * 
     * @param keyword
     *            dependency ID or template ID for the groupof cache IDs.
     * @return A {@link Set} of cache IDs or empty set if no cache ID is
     *         associated with it.
     */
    public Set<Object> getCacheIds(Object keyword);

    /**
     * Returns various cache statistics encapsulated int the CacheStatistics
     * interface.
     * 
     * @return {@link CacheStatistics}
     */
    public CacheStatistics getCacheStatistics();

    /**
     * Returns the dependency IDs for all the cache entries. A dependency
     * provides a mechanism for grouping of cache-ids.
     * 
     * A dependency-id can also be a cache-id. Dependency IDs label cache
     * entries and are used by invalidation rules to invalidate & timeout one or
     * more cache entries at a time.
     * 
     * The relationship beween a dependency-id and its dependent ids is formed
     * via invalidation rules in the cachespec.xml for servlet caches. For
     * object caches this relationship is explicitly specified via the
     * DistributedMap APIs.
     * 
     * @return A set of dependency IDs.
     * @ibm-api
     */
    public Set<Object> getDependencyIds();

    /**
     * Returns the Template IDs for all the cache entries. A template provides a
     * mechanism for grouping of servlet and JSP cache-ids.
     * 
     * A template is semantically equivalent to a dependency. However a template
     * is a mechanism of grouping for servlet, JSP and portal cache objects. A
     * template for instance can be the URI or the context root of an
     * application or URI of a top level servlet.
     * 
     * @return A set of template IDs.
     * @ibm-api
     */
    public Set<Object> getTemplateIds();

    /**
     * Invalidates all the cache entries dependent on the specified cache ID. If
     * the ID is for a specific cache entry, then only that object is
     * invalidated. If the ID is for a dependency id, then all objects that
     * share that dependency ID will be invalidated.
     * 
     * <p>
     * Implementation Guidelines: This method unpins an entry from the cache if
     * the internal reference count is zero. If the CacheEntry is being used by
     * other clients i.e. its reference count is greater than zero, this cache
     * entry is marked for removal.
     * 
     * <br>
     * <code>
     * CacheEntry oldCacheEntry = cacheMap.remove(id);
     * if oldCacheEntry.getReferenceCount() > 0 then oldCacheEntry.setRemoveWhenUnpinned();
     * if oldCacheEntry.getReferenceCount() == 0 then
     * if oldCacheEntry.isPooled()
     * oldCacheEntry.returnToPool (pooling of CacheEntry)
     * else
     * oldCacheEntry = null (NO pooling of CacheEntry, explicit release for garbage collection)
     * </code>
     * 
     * @param id
     *            The cache ID or dependency ID. It cannot be null.
     * @param waitOnInvalidation
     *            <code>true</code> indicates that this method should not
     *            return until the invalidations have taken effect on all
     *            caches. <code>false</code> indicates that the invalidations
     *            will be queued for later batch processing.
     */
    public void invalidate(Object id, boolean waitOnInyvalidation);

    /**
     * Invalidates the cache id.
     * 
     * <p>
     * Implementation Guidelines: This method unpins an entry from the cache if
     * the internal reference count is zero. If the CacheEntry is being used by
     * other clients i.e. its reference count is greater than zero, this cache
     * entry is marked for removal.
     * 
     * <br>
     * <code>
     * CacheEntry oldCacheEntry = cacheMap.remove(id);
     * if oldCacheEntry.getReferenceCount() > 0 then oldCacheEntry.setRemoveWhenUnpinned();
     * if oldCacheEntry.getReferenceCount() == 0 then
     * if oldCacheEntry.isPooled()
     * oldCacheEntry.returnToPool (pooling of CacheEntry)
     * else
     * oldCacheEntry = null (NO pooling of CacheEntry, explicit release for garbage collection)
     * </code>
     * 
     * @param cacheId
     *            The cache ID. It cannot be null.
     * @param waitOnInvalidation
     *            <code>true</code> indicates that this method should not
     *            return until the invalidations have taken effect on all
     *            caches. <code>false</code> indicates that the invalidations
     *            will be queued for later batch processing.
     */
    public void invalidateByCacheId(Object cacheId, boolean waitOnInvalidation);

    /**
     * Invalidates all the cache entries dependent on the specified dependency ID.
     * 
     * <p>
     * Implementation Guidelines: This method unpins an entry from the cache if
     * the internal reference count is zero. If the CacheEntry is being used by
     * other clients i.e. its reference count is greater than zero, this cache
     * entry is marked for removal.
     * 
     * <br>
     * <code>
     * CacheEntry oldCacheEntry = cacheMap.remove(id);
     * if oldCacheEntry.getReferenceCount() > 0 then oldCacheEntry.setRemoveWhenUnpinned();
     * if oldCacheEntry.getReferenceCount() == 0 then
     * if oldCacheEntry.isPooled()
     * oldCacheEntry.returnToPool (pooling of CacheEntry)
     * else
     * oldCacheEntry = null (NO pooling of CacheEntry, explicit release for garbage collection)
     * </code>
     * 
     * @param dependency
     *            The dependency ID. It cannot be null.
     * @param waitOnInvalidation
     *            <code>true</code> indicates that this method should not
     *            return until the invalidations have taken effect on all
     *            caches. <code>false</code> indicates that the invalidations
     *            will be queued for later batch processing.
     */
    public void invalidateByDependency(Object dependency, boolean waitOnInvalidation);

    /**
     * Invalidates all the cache entries dependent on the specified template ID.
     * 
     * <p>
     * Implementation Guidelines: This method unpins an entry from the cache if
     * the internal reference count is zero. If the CacheEntry is being used by
     * other clients i.e. its reference count is greater than zero, this cache
     * entry is marked for removal.
     * 
     * <br>
     * <code>
     * CacheEntry oldCacheEntry = cacheMap.remove(id);
     * if oldCacheEntry.getReferenceCount() > 0 then oldCacheEntry.setRemoveWhenUnpinned();
     * if oldCacheEntry.getReferenceCount() == 0 then
     * if oldCacheEntry.isPooled()
     * oldCacheEntry.returnToPool (pooling of CacheEntry)
     * else
     * oldCacheEntry = null (NO pooling of CacheEntry, explicit release for garbage collection)
     * </code>
     * 
     * @param template
     *            The template ID. It cannot be null.
     * @param waitOnInvalidation
     *            <code>true</code> indicates that this method should not
     *            return until the invalidations have taken effect on all
     *            caches. <code>false</code> indicates that the invalidations
     *            will be queued for later batch processing.
     */
    public void invalidateByTemplate(String template, boolean waitOnInvalidation);

    /**
     * Puts an entry into the CoreCache. If the entry already exists in the
     * cache, this method will ALSO update the same.
     * 
     * <p>
     * Implementation Guidelines: Cache providers should store a {cacheId -->
     * CacheEntry} association in their cache.
     * 
     * A {@link CacheEntry} is a container object containing the cache key,
     * value and metadata for a particular cached item. The CacheEntry object is
     * owned by the CacheProvider. <br>
     * This method unpins an entry from the cache if the internal reference
     * count is zero. If the CacheEntry is being used by other clients i.e. its
     * reference count is greater than zero, this cache entry is marked for
     * removal.
     * 
     * <br>
     * <code> CacheEntry oldCacheEntry = cacheMap.put(id); </code>
     * <ul>
     * <li><code>if the Old CacheEntry.getReferenceCount() > 0, oldCacheEntry.setRemoveWhenUnpinned()</code></li>
     * <li><code>if the old CacheEntry.getReferenceCount() == 0, oldCacheEntry.returnToPool() or oldCacheEntry = null</code></li>
     * </ul>
     * 
     * <br>
     * The CacheProvider has to create associations between this entry and the
     * rest of the entries in the cache using the getDataIds() and
     * getTemplates() methods on the {@link EntryInfo}.
     * 
     * The CacheProvider should use the getters provided on the {@link EntryInfo} to control and set the expiration timeout and other
     * characteristics of the cached item in its own cache.
     * 
     * @param ei
     *            The metadata of the cache entry including its key.
     * @param value
     *            The object to store in the cache.
     * 
     * @return oldCacheEntry
     *         The previous CacheEntry if any, associated with the key(ei) in the cache. If no key (ei) is found, <code>null</code> is returned.
     */
    public CacheEntry put(EntryInfo ei, Object value);

    /**
     * Refresh the entry by updating the LRU location.
     * 
     * @param cacheId
     */
    public void refreshEntry(Object CacheId);

    public void setEventSource(EventSource eventSource);

    /**
     * This method will be invoked once Dynacache is ready to use the {@link CoreCache}. It is the responsibility of the {@link CacheProvider} to initialize the internal state of
     * this cache instance in the
     * <code>CacheProvider.createCache(CacheConfig cc)</code> method
     */
    public void start();

    /**
     * This method will be invoked when the application server is stopped. At
     * this point the {@link CacheProvider} ought to cleanup any resources
     * related to the this cache instance
     * 
     */
    public void stop();

    /**
     * The touch method updates the expiration times and the state of an entry
     * in the CoreCache.
     * 
     * <p>
     * Implementation Guidelines: A {@link CacheEntry} has three possible
     * states:
     * <ul>
     * <li>Expired = cached content not needed and not available anymore
     * <li>Invalid = cached content currently invalid but still available
     * <li>Valid = cached content valid and available
     * </ul>
     * 
     * <br>
     * After the validatorExpirationTime has passed a CacheEntry transitions
     * from the Valid to the Invalid state. After the expirationTime has passed
     * a CacheEntry transitions from the Invalid to the Expired state. An
     * expired CacheEntry is eventually purged from the cache.
     * 
     * <br>
     * The validatorExpirationTime for a CacheEntry is always less than or equal
     * to the expirationTime. The validatorExpirationTime and expirationTime
     * control the state of the {@link CacheEntry}.
     * 
     * @param id
     *            The cache ID for the cache value to be found. It cannot be
     *            null.
     * @param validatorExpirationTime
     *            The value of validator expiration time
     * @param expirationTime
     *            The value of expiration time
     */
    public void touch(Object id, long validatorExpirationTime, long expirationTime);

    /**
     * Returns the name of the cache instance
     * 
     * @return name of a cache instance that is unique within this JVM.
     */
    public String getCacheName();
}
