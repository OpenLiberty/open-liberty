/*******************************************************************************
 * Copyright (c) 1999, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.util.cache;

/**
 * <code>EvictionStrategy</code> is the abstract mechanism for customizing
 * the eviction behavior of <code>Cache</code>. When the
 * preferred maximum size associated with a <code>Cache</code> determines
 * that the cache is full, the cache asks <code>EvictionStrategy</code> to
 * select a victim to be removed.
 * 
 * <code>EvictionStrategy</code> may use any means to select a victim from
 * the cache, or may return null if there are no objects in the cache which
 * can be discarded. Doing so will cause the insert() operation on the Cache
 * to fail.
 * 
 * @see Cache
 * 
 */

public interface EvictionStrategy
{

    //
    // Operations
    //

    /**
     * Called by the cache when it needs to discard an object from the cache.
     * 
     * The cache will be holding the lock on the bucket when this method is
     * invoked. Enables the eviction strategy to change the "decision" on
     * evicting an object with the bucket lock held (a consistent state).
     * 
     * @param element The element that is to be evicted
     * 
     * @return boolean to indicate whether to proceed with eviction or not.
     */

    boolean canBeDiscarded(Element element);

    /**
     * Cease all work being done for this strategy
     */
    //d601399
    void cancel();

    /**
     * Set the interval that the eviction strategy uses between sweeps of the
     * cache to remove cached elements.
     * 
     * @param sweepInterval time interval in milliseconds
     */
    void setSweepInterval(long sweepInterval);

    /**
     * Set the preferred size for the cache that this eviction strategy is
     * responsible for.
     * 
     * @param cacheSize preferred cache size used
     */
    void setPreferredMaxSize(int cacheSize);

    /**
     * Get the current preferred maximum cache size that the eviction strategy
     * is abiding by.
     * 
     * @return the preferred cache max size
     */
    int getPreferredMaxSize();

}
