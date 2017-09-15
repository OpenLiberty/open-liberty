/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.msgstore;

/**
 * A window onto the behaviour of the cache.
 * 
 * @author DrPhill
 *
 */
public interface CacheStatistics {
    /**
     * @return current count of the number of items in cache
     */
    public long getCurrentCount();
    
    /**
     * @return current total of (admitted) sizes of all items in cache
     */
    public long getCurrentSize();

    /**
     * @return current total of (admitted) total size believed discardable.
     */
    public long getDiscardableSize();
    
    /**
     * @return the currently active maximum size for the cache. This
     * value is set by various methods at startup time.
     */
    public long getMaximumSize();
    
    /**
     * @return the total number of items that have been in the cache
     * since it was started, or since the totals were last cleared
     */
    public long getTotalCount();
    
    /**
     * @return the total number of items that have been discarded from
     * the cache since it was started, or since the totals were last 
     * reset
     */
    public long getTotalDiscardCount();
    
    /**
     * @return the total of the (admitted) size of all items that have been 
     * discarded from the cache since it was started, or since the totals 
     * were last reset
     */
    public long getTotalDiscardSize();
    
    /**
     * @return the total number of items that have been refused entry
     * to the cache since it was started, or since the totals were last 
     * reset.  This includes items that were refused because they were
     * too large for the available space and items that were refused 
     * because they were larger than the cache.
     */
    public long getTotalRefusalCount();
    
    /**
     * @return the total of the (admitted) size of all items that have been
     * in the cache since it was started, or since the totals were last cleared
     */
    public long getTotalSize();
    
    /**
     * Reset the running totasls.  This does not affect the cache in any other way. 
     */
    public void resetTotals();
}
