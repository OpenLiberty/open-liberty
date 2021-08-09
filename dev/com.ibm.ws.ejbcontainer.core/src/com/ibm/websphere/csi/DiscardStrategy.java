/*******************************************************************************
 * Copyright (c) 2000, 2001 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

import java.lang.Exception;

/**
 * <code>DiscardStrategy</code> is the abstract mechanism for hooking
 * application-specific processing into an <code>EJBCache</code>'s eviction
 * mechanism. For each object evicted from the cache (as selected by the
 * <code>EvictionStrategy</code>) the <code>DiscardStrategy</code> is
 * notified. The notification occurs after the object has been removed
 * from the cache (and so is, presumably, inaccessible to other parts of
 * the application). <p>
 * 
 * The <code>DiscardStrategy</code> will also be informed when an object
 * is removed from the cache using the <code>remove</code> call
 * 
 * This hook may be used for clean up of other data structures, storing
 * objects to backing store, etc. <p>
 * 
 * @see Cache
 * @see EvictionStrategy
 * 
 */

public interface DiscardStrategy
{
    /**
     * Called by the cache after it evicts an object from the cache.
     * This gives the implementation an opportunity to perform any
     * required clean up, e.g. passivation, before an object is
     * garbage collected.
     * 
     * The cache will be holding the bucket lock when this method is
     * invoked.
     * 
     * @param key The key for the object which was evicted
     * @param object The object which was evicted
     *            <p>
     * 
     */

    void discardObject(EJBCache cache, Object key, Object object)
                    throws Exception;
}
