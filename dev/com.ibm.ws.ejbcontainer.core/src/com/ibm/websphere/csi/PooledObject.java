/*******************************************************************************
 * Copyright (c) 2002, 2003 IBM Corporation and others.
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
 * A PooledObject receives notifications from the Pool when the object is placed into the Pool
 * and when the object is discarded from the Pool.
 * 
 * @see Pool
 * @see PoolManager
 */

public interface PooledObject {

    /**
     * Called after the pool discards the object.
     * This gives the <code>PooledObject</code> an opportunity to perform any
     * required clean up.
     */
    public void discard();

    /**
     * Called prior to the object being placed back in the pool.
     * This gives the <code>PooledObject</code> an opportunity to re-initialize
     * its internal state.
     */
    public void reset();

}
