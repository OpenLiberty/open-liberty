/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.util.cache;

import com.ibm.ejs.container.util.locking.LockTable;
import com.ibm.websphere.csi.DiscardStrategy;

/**
 * <code>DiscardWithLockStrategy</code> is the abstract mechanism for hooking
 * application-specific locking and processing into an <code>EJBCache</code>'s
 * eviction mechanism. For each object evicted from the cache (as selected by
 * the <code>EvictionStrategy</code>) the <code>LockTable</code> provided by
 * the <code>DiscardWithLockStrategy</code> is asked for a lock object to
 * synchronize the eviction processing, and then the
 * <code>DiscardWithLockStrategy</code> is notified by calling discardObject.
 * The notification occurs after the object has been removed from the cache
 * (and so is, presumably, inaccessible to other parts of the application). <p>
 * 
 * This interface should be used when the application performs additional
 * locking beyond the internal Cache locking, and the discardObject processing
 * may attempt to obtain the application lock as well as calling back into
 * the Cache, thus acquiring additional Cache locking. This allows the
 * application to insure the order in which locks are obtained is always
 * application first, and then Cache.... thus preventing deadlocks. <p>
 * 
 * The <code>DiscardWithLockStrategy</code> will also be informed when an object
 * is removed from the cache using the <code>removeAndDiscard</code> call, but
 * the lock synchronization is not performed. <p>
 * 
 * This hook may be used for clean up of other data structures, storing
 * objects to backing store, etc. <p>
 * 
 * @see Cache
 * @see EvictionStrategy
 */

public interface DiscardWithLockStrategy extends DiscardStrategy
{
    /**
     * Returns the LockTable to be used to synchronize Cache eviction
     * processing. <p>
     * 
     * The returned LockTable will be used to obtain a lock object when the
     * Cache has selected an object to be evicted, but prior to obtaining any
     * internal Cache locks (bucket lock). The returned lock object will be
     * used to synchronize both the removal of the object from the Cache, as
     * well as the call to discardObject. <p>
     * 
     * @return LockTable to use for synchronization of the eviction processing.
     **/
    public LockTable getEvictionLockTable();

}
