/*******************************************************************************
 * Copyright (c) 2002, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.ejbcontainer.util;

/**
 * Abstract base class for Pool implementations
 */
public abstract class PoolImplBase implements Pool {

    /**
     * True iff an attempt to retrieve an instance from this pool
     * hasn't happened recently. <p>
     */
    protected boolean inactive = true;

    /**
     * Pool manager that "owns" this pool.
     */
    protected PoolManagerImpl poolMgr;

    /**
     * Retrieve an object from this pool. If the retrieved object implements the
     * PooledObject interface, the onGetFromPool method will be called.
     * 
     * This method will return null if the pool is empty.
     */
    public abstract Object get();

    /**
     * Return an object instance to this pool. <p>
     * 
     * If there is no room left in the pool the instance will be
     * discarded, and if DiscardStrategy interface is being used,
     * the appropriate callback method will be called.
     */
    public abstract void put(Object o);

    /**
     * Remove some or all of the elements from this pool, down to its
     * minimum value. Discontinue if the pool becomes active while draining.
     * Discarded objects will have appropriate callback method called.
     */
    abstract void periodicDrain();

    /**
     * Remove all of the elements from this pool. Discarded objects will
     * have appropriate callback method called.
     */
    abstract void completeDrain();

    /**
     * Prevents the pool from accepting any further pooled instances.
     */
    // F73236
    abstract void disable();

    /**
     * Destroy this object pool and discard all the objects in it.
     */
    public final void destroy()
    {
        poolMgr.remove(this);
        disable(); // F73236
        completeDrain();
    } // destroy

} // PoolImplBase
