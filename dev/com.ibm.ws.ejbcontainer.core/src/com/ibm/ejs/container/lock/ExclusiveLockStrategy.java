/*******************************************************************************
 * Copyright (c) 1998, 2002 IBM Corporation and others.
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
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * This class provides a <code>LockStrategy</code> implemenation to be
 * used when the container has exclusive access to the peristent store
 * associated with a given bean instance. <p>
 */
class ExclusiveLockStrategy
                extends LockStrategy
{
    private static final TraceComponent tc =
                    Tr.register(ExclusiveLockStrategy.class, "EJBContainer",
                                "com.ibm.ejs.container.container");

    private static final String CLASS_NAME =
                    "com.ibm.ejs.container.lock.ExclusiveLockStrategy";

    protected ExclusiveLockStrategy() {}

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
    @Override
    public boolean lock(EJSContainer c, ContainerTx tx, Object lockName, int mode)
                    throws LockException
    {
        try
        {
            Locker locker = tx; // d139352-2
            return c.getLockManager().lock(lockName, locker, mode);
        } catch (InterruptedException ex)
        {
            FFDCFilter.processException(ex, CLASS_NAME + ".lock", "75", this);
            Tr.fatal(tc, "ATTEMPT_TO_ACQUIRE_LOCK_INTERRUPTED_CNTR0004E",
                     new Object[] { ex }); //p111002.5
        }

        return false;
    } // enlist

    /**
     * Release the lock identified by the given lock name and held by
     * the given locker. <p>
     */
    @Override
    public void unlock(EJSContainer c, Object lockName, Locker locker)
    {
        c.getLockManager().unlock(lockName, locker);
    } // unlock

} // ExclusiveLockStrategy

