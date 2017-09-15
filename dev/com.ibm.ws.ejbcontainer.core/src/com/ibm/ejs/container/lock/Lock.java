/*******************************************************************************
 * Copyright (c) 1998, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ejs.container.lock;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A <code>Lock</code> is stored in the lock table by a
 * <code>LockManager</code> whenever there are multiple users of
 * a lock. <p>
 * 
 */

class Lock
                implements LockProxy
{
    //d121558
    private static final TraceComponent tc = Tr.register(Lock.class, "EJBContainer", "com.ibm.ejs.container.container");

    /**
     * The mode this lock is currently held in. <p>
     */

    private int mode;

    /**
     * Id of this lock. <p>
     */

    private Object lockName;

    /**
     * The <code>Locker</code>'s that are currently holding this
     * lock. <p>
     */

    private Hashtable holders;

    /**
     * The <code>Waiter</code>s that are currently waiting to acquire
     * this lock. <p>
     * 
     * FIX ME: For efficiency these should be combined. Vectors make
     * terrible queues, hashtables are expensive to allocate.
     * Need something like a hashQueue.
     */

    private Hashtable waiters;
    private Vector waitQ;

    /**
     * The <code>LockManager</code> that owns this lock. <p>
     */

    private LockManager lockMgr;

    /**
     * The number of users of this lock. It should always be equal to
     * the number of holders + the number of waiters.
     */

    private int numUsers;

    /**
     * Create a new <code>Lock</code> instance. <p>
     * 
     * @param lockName the <code>Object</code> that defines the identity
     *            of this <code>Lock</code> instance <p>
     * 
     * @param holder the <code>Locker</code> instance that currently
     *            holds this <code>Lock</code> <p>
     * 
     * @param lockMgr the <code>LockManager</code> that owns this
     *            <code>Lock</code> <p>
     */

    Lock(Object lockName, Locker holder, LockManager lockMgr)
    {
        this.lockName = lockName;
        this.holders = new Hashtable();
        holders.put(holder, holder);
        this.numUsers++;
        this.lockMgr = lockMgr;
        this.mode = holder.getLockMode(lockName);
        this.waiters = new Hashtable();
        this.waitQ = new Vector();

    } // Lock

    /**
     * Attempt to acquire this lock in the requested mode on behalf of the
     * given locker. <p>
     * 
     * Note, this lock object wouldn't exist if there wasn't already at
     * least one holder. An attempt to acquire this lock is therefore
     * only compatible (i.e. won't block) if this lock is held in the
     * SHARED mode. <p>
     * 
     */
    void acquire(Locker locker, int requestedMode, boolean wait)
                    throws InterruptedException,
                    LockException
    {
        Waiter waiter = null;

        synchronized (this) {

            //-----------------------------------------
            // If there are no holders or waiters then
            // acquisition is immediately successful
            //-----------------------------------------

            if (numUsers == 0) {
                holders.put(locker, locker);
                mode = requestedMode;
                numUsers++;
                return;
            }

            //------------------------------------------------
            // If lock already held then acquisition succeeds
            //------------------------------------------------

            if (holders.get(locker) != null) {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Lock currently held by locker");
                }
                return;
            }

            //------------------------------------------------------
            // Check for conflict, if requested mode is compatible
            // with held mode and no one is waiting then grant lock
            //------------------------------------------------------

            if (mode == LockManager.SHARED) {
                if (requestedMode == LockManager.SHARED) {
                    if (waiters.size() == 0) {
                        holders.put(locker, locker);
                        numUsers++;
                        return;
                    }
                }
            }

            //-------------------------------------------
            // Conflict: check if we are already waiting,
            // and if we are then upgrade our lock mode
            // if necessary
            //-------------------------------------------

            Waiter currWaiter = (Waiter) waiters.get(locker);

            if (currWaiter != null) {
                if (currWaiter.mode != requestedMode) {
                    if (lockMgr.detectDeadlock(locker, this, requestedMode)) {
                        throw new DeadlockException();
                    }
                    if (requestedMode == LockManager.EXCLUSIVE) {
                        currWaiter.mode = LockManager.EXCLUSIVE;
                    }
                }
                waiter = currWaiter;
            } else {

                //------------------------------------------
                // Not currently waiting, add to wait queue
                //------------------------------------------

                if (lockMgr.detectDeadlock(locker, this, requestedMode)) {
                    throw new DeadlockException();
                }
                waiter = new Waiter(this, locker, requestedMode);
                waiters.put(locker, waiter);
                lockMgr.waitEvent(waiter);
                numUsers++;
                waitQ.addElement(waiter);
            }
        }

        //------------------------------------------------------------
        // Note, at this point we have added new waiter to lock, but
        // lock is NOT held on behalf of caller. Lock has effectively
        // been reserved for waiter.
        //------------------------------------------------------------

        if (!wait) {
            return;
        }

        //----------------------------------------------------------------
        // Sleep current thread outside of scope of the synchronization
        // lock on this instance
        //----------------------------------------------------------------

        synchronized (waiter) {

            //-----------------------------------------------------------
            // Check to make sure lock wasn't released or granted while
            // we were waiting to acquire synchronization lock
            //-----------------------------------------------------------

            if (waiter.released) {
                throw new LockReleasedException();
            }

            if (waiter.theLock.holders.get(waiter.locker) == null) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "waiting for lock",
                             new Object[] { lockName, locker,
                                           LockManager.modeStrs[mode] });
                }
                waiter.wait();

                if (waiter.released) {
                    throw new LockReleasedException();
                }
            }
        }

    } // acquire

    /*
     * Release all holders of and waiters on this lock with respect to
     * the given locker and return the number of users of this lock
     */

    synchronized int release(Locker locker) {

        //-----------------------------------------------------------------
        // A transaction can be both a holder and a waiter if
        // it is attempting to convert a shared lock to an exclusive one.
        //-----------------------------------------------------------------

        if (holders.remove(locker) != null) {
            numUsers--;
        }

        Waiter waiter = (Waiter) waiters.remove(locker);
        if (waiter != null) {
            numUsers--;
            synchronized (waiter) {
                waitQ.removeElement(waiter);
                waiter.released = true;
                lockMgr.unwaitEvent(waiter);
                waiter.notify();
            }
        }

        //----------------------------------------------------------------
        // Q: Do we care about attempts to release locks for given locker
        //    that are not held by that locker? If so fail here if locker
        //    was neither holder nor waiter.
        //----------------------------------------------------------------

        //----------------------------------------------------------
        // If there are no more holders then wake up all compatible
        // waiters from the head of the wait queue. This imposes a
        // strict FIFO scheduling.
        //----------------------------------------------------------

        if (holders.size() == 0) {

            //-------------------------------------------------
            // Grant first waiter the lock and then see if the
            // remaining waiters are compatible.
            //-------------------------------------------------

            if (waiters.size() > 0) {
                Waiter newHolder = (Waiter) waitQ.firstElement();
                synchronized (newHolder) {
                    waiters.remove(newHolder.locker);
                    waitQ.removeElementAt(0);
                    holders.put(newHolder.locker, newHolder.locker);
                    this.mode = newHolder.mode;
                    lockMgr.unwaitEvent(newHolder);
                    newHolder.notify();
                }
            }

            //--------------------------------------------------
            // If lock now held in exclusive mode we are done,
            // else grant lock to all waiters at head of queue
            // that are requesting the lock in the SHARED mode.
            //--------------------------------------------------

            if (mode == LockManager.EXCLUSIVE) {
                return numUsers;
            }

            while (waiters.size() > 0) {
                Waiter newHolder = (Waiter) waitQ.firstElement();
                if (newHolder.mode == LockManager.EXCLUSIVE) {
                    return numUsers;
                }

                synchronized (newHolder) {
                    waiters.remove(newHolder.locker);
                    waitQ.removeElementAt(0);
                    holders.put(newHolder.locker, newHolder.locker);
                    lockMgr.unwaitEvent(newHolder);
                    newHolder.notify();
                }
            }
        }
        return numUsers;

    } // release

    /**
     * This is a heavyweight lock instance.
     */

    public final boolean isLock() {
        return true;
    }

    /**
     * Return holders of this lock.
     */

    public Enumeration getHolders() {

        return holders.elements();

    } // getHolders

    /**
     * Returns true if the specified locker holds a lock.
     */
    // PQ53065
    public boolean isHolder(Locker locker)
    {
        return (holders.get(locker) != null);
    } // isHolder

} // Lock

