/*******************************************************************************
 * Copyright (c) 1999, 2002 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.lock;

import com.ibm.ejs.container.ContainerTx;
import com.ibm.ejs.container.EJSContainer;

/**
 * A <code>LockStratgey</code> is used by the EJB container to perform
 * locking and unlocking of EJBs. <p>
 * 
 * The base <code>LockStrategy</code> implementation provides a null
 * implementation of all methods. This implementation is used when
 * all entity bean locking is delegated to the database. <p>
 */

public class LockStrategy
{
    /**
     * The two legal <code>LockStrategy</code> implementations. <p>
     */
    public static LockStrategy EXCLUSIVE_LOCK_STRATEGY =
                    new ExclusiveLockStrategy();

    public static LockStrategy NULL_LOCK_STRATEGY = new LockStrategy();

    protected LockStrategy() {}

    /**
     * Acquire a transaction duration lock identified by the given name
     * for the duration of the given transaction and in the given mode. <p>
     * 
     * @param c EJB Container instance in which to obtain the lock.
     * @param tx ContainerTx associated with the current transaction.
     * @param lockName name used to identify the lock to acquire.
     * @param mode mode of the lock to obtain.
     * 
     * @return true if the lock was acquired, and false if the lock
     *         was already held.
     */
    // PQ53065
    public boolean lock(EJSContainer c, ContainerTx tx, Object lockName, int mode)
                    throws LockException
    {

        //---------------------------------------
        // This method intentionally left blank.
        //---------------------------------------

        return true;
    }

    /**
     * Release the lock identified by the given lock name and held by
     * the given locker. <p>
     */
    public void unlock(EJSContainer c, Object lockName, Locker locker)
    {
        //---------------------------------------
        // This method intentionally left blank.
        //---------------------------------------

    } // unlock

} // LockStrategy
