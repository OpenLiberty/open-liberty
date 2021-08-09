/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
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
 * A <code>Pool</code> maintains a preallocated homogenous
 * collection of objects. <p>
 * 
 * A <code>Pool</code> is allocated via the
 * <code>PoolManager</code>. It has associated minimum and maximum
 * values that control the total number of objects that will be kept
 * in the pool. The pool will never be automatically drained below the
 * minimum number of objects although it may, at any given instant, contain
 * fewer than the minimum number of objects (indeed it may be empty). It
 * will never contain more than the maximum number. <p>
 * 
 * Retrieving an object from a <code>Pool</code> may fail if the
 * pool is empty. It is up to the caller to handle this failure. <p>
 * 
 * Returning an object to the <code>Pool</code> may fail if the
 * pool is full.
 * 
 * Certain implementations of this interface provide callback methods that allow the
 * user of the pool to handle initialization and cleanup that may be needed when
 * objects are retrieved from the pool or returned to the pool.
 */
public interface Pool {

    /**
     * Get an object from the pool. If the pool is empty, a null will be returned.
     */
    public Object get();

    /**
     * Get an object from the pool. If the pool is empty, a new instance of "Class"
     * will be returned. The same Class
     * must be used on all calls to this method (and the preLoad method) for
     * a given Pool instance.
     */
    public Object get(Class c)
                    throws InstantiationException, IllegalAccessException;

    /**
     * Get an object from the pool. If the pool is empty, the PooledObjectMaster's
     * newInstance() method will be called and that object returned.
     */
    public Object get(PooledObjectMaster m);

    /**
     * Add an object to the pool. If the put would result in the pool growing
     * beyond its maximum size, the object will be discarded.
     */
    public void put(Object o);

    /**
     * Create instances of Class and add to the pool, up to lower pool size value. This
     * same Class must be used on all subsequent calls to get(Class c) for this pool instance.
     */
    public void preLoad(Class c)
                    throws InstantiationException, IllegalAccessException;

    /**
     * Calls PooledObjectMaster.newInstance() to create instances of PooledObject
     * and add to the pool, up to lower pool size value.
     */
    public void preLoad(PooledObjectMaster m);

    /**
     * Destroy the Pool and discard all the objects in it.
     */
    public void destroy();

} // Pool

