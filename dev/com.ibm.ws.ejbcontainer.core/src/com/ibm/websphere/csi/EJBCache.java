/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

import java.util.Enumeration;

/**
 * An EJBCache is used by the container to store all stateful and entity
 * beans that are currently active.
 */

public interface EJBCache
{
    /**
     * Return the number of buckets this cache is using.
     */
    public int getNumBuckets();

    /**
     * Return the name of the cache, used to identify it.
     **/
    public String getName(); // d129562

    /**
     * Return the number of objects stored in this cache.
     */
    public int getSize();

    /**
     * Set the discard strategy for this cache.
     */
    public void setDiscardStrategy(DiscardStrategy d);

    /**
     * Set the fault strategy for this cache
     */
    public void setFaultStrategy(FaultStrategy f);

    /**
     * Set the sweep interval for this cache.
     */
    public void setSweepInterval(long sweepInterval);

    /**
     * Return the preferred maximum size of this cache. This is treated as
     * a soft limit, so this is simply the size at which the cache would come
     * back to over time if it grows over this number.
     */
    public void setCachePreferredMaxSize(int maxSize);

    /**
     * Return an enumeration of all elements in this cache.
     * The Enumeration must contain CacheElements.
     * 
     * @see CacheElement
     */
    public Enumeration<?> enumerateElements();

    /**
     * Return element for the given key.
     */
    public Object find(Object key);

    /**
     * Return element for the given key but do not pin the element.
     * In additional, if object is found, add adjustPinCount to the pinned state.
     **/
    // d173022.12
    public Object findDontPinNAdjustPinCount(Object key,
                                             int adjustPinCount);

    /**
     * Find the element for the given key, fault the object into the cache
     * if the element was not found
     */
    public Object findAndFault(Object key)
                    throws FaultException;

    /**
     * Insert element into the cache.
     */
    public CacheElement insert(Object key, Object object);

    /**
     * Inserts element into the cache without pinning.
     */
    // F61004.6
    public CacheElement insertUnpinned(Object key, Object object);

    /**
     * Pin element in the cache, do not allow it to be swapped out.
     */
    public void pin(Object key);

    /**
     * Pin element in the cache, do not allow it to be swapped out.
     */
    // F61004.6
    public void pinElement(CacheElement element);

    /**
     * Unpin element in the cache, allow it to be swapped out. <p>
     * 
     * @param key key of element to be unpinned.
     * 
     * @return the number of pins remaining.
     */
    public int unpin(Object key);

    /**
     * Unpin element in the cache, allow it to be swapped out. <p>
     * 
     * @return the number of pins remaining.
     */
    // F61004.6
    public int unpinElement(CacheElement element);

    /**
     * Remove element from cache identified by the given key. Return
     * object removed.
     */
    public Object remove(Object key, boolean dropRef);

    /**
     * Remove element from cache.
     */
    // F61004.6
    public void removeElement(CacheElement element, boolean dropRef);

    /**
     * Discard an element from the cache identified by the given key.
     * Return the object removed. Very similar to remove, the distinction
     * being that removeAndDiscard will invoke the DiscardStrategy.
     */
    public Object removeAndDiscard(Object key, boolean dropRef)
                    throws DiscardException;

    /**
     * Return true, iff the cache contains the given key.
     */
    public boolean contains(Object key);

    /**
     * Mark the element as being ineligible for eviction from the cache.
     **/
    //d465813
    public void markElementEvictionIneligible(CacheElement element);

    /**
     * Terminate this cache - cancels and unsets evictionStrategies.
     */
    //d601399
    public void terminate();

} // EJBCache
