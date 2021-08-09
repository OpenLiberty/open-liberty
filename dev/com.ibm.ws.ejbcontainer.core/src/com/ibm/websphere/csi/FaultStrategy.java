/*******************************************************************************
 * Copyright (c) 1999, 2001 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.websphere.csi;

/**
 * <code>FaultStrategy</code> is the abstract mechanism for customizing
 * the fault or miss behavior of <code>Cache</code>. When a key is not
 * found in the cache during <code>findAndFault()</code>, <code>Cache</code>
 * notifies the associated <code>FaultStrategy</code>, providing the key
 * for the object.
 * 
 * The <code>Cache</code> holds the bucket lock when informing the fault
 * strategy.
 * 
 * <code>FaultStrategy</code> may be used to implement transparent loading
 * of objects into the cache.
 * 
 * @see Cache
 * 
 */

public interface FaultStrategy
{

    //
    // Operations
    //

    /**
     * Called by <code>Cache</code> when a <code>findAndFault()</code>
     * operation fails to find an object with the speicifed key in the cache.
     * The implementation should construct the object associated with the key
     * and return it to the cache, which will perform an
     * <code>insert()</code>.
     * 
     * Note that the bucket lock will be held when this method is invoked
     * <p>
     * 
     * @param cache The cache where the failing <code>findAndFault()</code>
     *            was invoked
     * @param key The key associated with the object which could
     *            not be found
     * 
     * @return The object associated with the key or null if no object
     *         should be inserted
     * 
     */

    Object faultOnKey(EJBCache cache, Object key) throws Exception;

}
