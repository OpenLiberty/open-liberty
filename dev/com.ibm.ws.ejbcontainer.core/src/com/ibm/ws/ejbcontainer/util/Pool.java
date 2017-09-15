/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
 * A <code>Pool</code> maintains a preallocated homogeneous
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
     * Add an object to the pool. If the put would result in the pool growing
     * beyond its maximum size, the object will be discarded.
     */
    public void put(Object o);

    /**
     * Destroy the Pool and discard all the objects in it.
     */
    public void destroy();

    /**
     * Returns the maximum number of instances that may be present in the pool.
     */
    public int getMaxSize();

    /**
     * Sets the maximum number of instances to keep in the pool.
     */
    // F96377
    public void setMaxSize(int maxSize);

} // Pool

