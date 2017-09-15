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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * The <code>LockManager</code> is designed to run as a singleton within
 * an <code>EJSContainer</code> and provide management of transaction
 * duration locks. <p>
 * 
 * The <code>LockManager</code> is not general purpose. It is designed
 * with full knowledge of the container's lock usage patterns and takes
 * advantage of this knowledge to improve performance. <p>
 * 
 * The name space for the locks managed by the <code>LockManager</code>
 * is defined by the container the lock manager is associated with. The
 * container requests that the lock manager acquire and release locks
 * that are identified by <code>Object</code> instances that implement
 * hashCode() and equals() properly with respect to the lock name
 * space. <p>
 * 
 * The <code>LockManager</code> does not track all locks held by a
 * particular transaction. Rather, it provides an interface to unlock
 * individual locks. The owning container transaction is responsible
 * for dropping its locks at end of transaction. <p>
 * 
 * To support transaction isolation levels below serializability, it must
 * be possible to release locks before a transaction completes. The same
 * unlock call that is used at end of transaction may be used to release
 * locks early. <p>
 * 
 * The lock manager will perform deadlock detection whenever a lock
 * acquisition request would require a wait. If the wait would cause a
 * deadlock to occur (within the managed lock space) the request will
 * fail by raising a <code>DeadlockException</code>, the requestor will
 * not be blocked, and the state of the locks managed by the lock manager
 * will not be changed. It is up to the caller to handle the deadlock
 * exception and determine the appropriate response. <p>
 * 
 */

public class LockManager
{
    //d121558
    private static final TraceComponent tc = Tr.register(LockManager.class, "EJBContainer", "com.ibm.ejs.container.container");

    /**
     * Locks acquired in the <code>SHARED</code> mode only
     * conflict with <code>EXCLUSIVE</code> mode locks. <p>
     */

    public static final int SHARED = 0;

    /**
     * <code>EXCLUSIVE</code> locks conflict with all other locks. <p>
     */

    public static final int EXCLUSIVE = 1;

    /*
     * Translate a lock mode into a string.
     */

    public static final String modeStrs[] = {
                                             "SHARED", // 0
                                             "EXCLUSIVE", // 1
    };

    /*
     * Currently, the locks are just stored in a simple hashtable.
     * Probably worth replacing this with a bucket-locked, bounded-size
     * table for performance and working set management.
     */

    private Hashtable lockTable;

    /*
     * The wait table maps a locker to the lock it is waiting to acquire.
     */

    private Hashtable waitTable;

    /**
     * Create a new <code>LockManager</code> instance. <p>
     */

    public LockManager() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) //130050
            Tr.entry(tc, "<init>");

        lockTable = new Hashtable();
        waitTable = new Hashtable();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) //130050
            Tr.exit(tc, "<init>");
    } // LockManager

    /**
     * Acquire the lock defined by the given <code>Object</code> for
     * the given container transaction in the specified mode. <p>
     * 
     * If the lock can be acquired in the given mode the container
     * transaction is added to the list of holders and this method
     * returns successfully. <p>
     * 
     * If the lock request conflicts with the current holders of
     * the lock then a check is made to see if waiting for the lock
     * would cause a deadlock. If so, the <code>DeadlockException</code>
     * is thrown, the caller does not wait, and the request fails.
     * If deadlock would not occur then the calling thread is blocked
     * until it is possible to acquire the lock in the requested mode. <p>
     * 
     * @param lockName the <code>Object</code> that identifies the lock
     *            to acquire the; this object instance must correctly
     *            implement the <code>hashCode</code>() and
     *            <code>equals</code> correctly with respect to the lock
     *            name space <p>
     * 
     * @param locker the <code>Locker</code> that is requesting the
     *            lock <p>
     * 
     * @param int the mode of the requested lock <p>
     * 
     * @exception <code>DeadlockException</code> is thrown if
     *            waiting for the lock to become available in the
     *            requested mode would cause a deadlock with respect to
     *            the set of locks managed by this lock manager <p>
     * 
     * @execption <code>InterruptedException</code> is thrown if
     *            this lock request was waiting for the lock to become
     *            available in the requested mode and the wait was
     *            interrupted; this should never happen and indicates
     *            in internal error if it does <p>
     * 
     * @exception <code>LockReleasedException</code> is thrown if
     *            this lock request was waiting for the lock to become
     *            available and another thread explicitly released the
     *            lock; it is up to the caller to determine the
     *            correct response <p>
     * 
     * @return true if the lock was acquired, and false if the lock
     *         was already held.
     */
    // PQ53065
    public boolean lock(Object lockName, Locker locker, int mode)
                    throws InterruptedException,
                    LockException
    {
        Lock theLock = null;

        synchronized (lockTable) {

            Object o = lockTable.put(lockName, locker);
            if (o == null || o == locker) {

                //--------------------------------------------------------
                // Fastpath: no conflicts, just leave locker  as
                // placeholder in case another attempt is made to acquire
                // this lock before it is released.
                //--------------------------------------------------------

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "lock acquired",
                             new Object[] { lockName, locker, modeStrs[mode] });
                }

                return (o == null);

            } else {

                //--------------------------------------------------------
                // Conflict, upgrade LockProxy to full lock if necessary.
                //--------------------------------------------------------

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Conflict : Lock upgrading from proxy",
                             new Object[] { lockName, locker,
                                           modeStrs[mode] });
                }

                LockProxy l = (LockProxy) o;
                if (l.isLock())
                {
                    theLock = (Lock) l;

                    // If the locker is already a holder of the lock, then
                    // lock succeeds, return false (already held).     PQ53065
                    if (theLock.isHolder(locker))
                    {
                        // Put the lock back into the table or the waiters will
                        // be waiting forever.                          d114715
                        lockTable.put(lockName, theLock);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                            Tr.debug(tc, "lock acquired (already held)",
                                     new Object[] { lockName, locker, modeStrs[mode] });
                        }
                        return false;
                    }
                }
                else
                {
                    theLock = new Lock(lockName, (Locker) o, this);
                }
                lockTable.put(lockName, theLock);
                theLock.acquire(locker, mode, false);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                    Tr.debug(tc, "Conflict : Lock upgraded from proxy",
                             new Object[] { lockName, locker,
                                           modeStrs[mode] });
                }
            }
        }

        //----------------------------------------------------------------
        // Acquire the lock outside the synchronization on the lockTable
        // so that other lock requests may proceed since acquisition may
        // block the current thread until the lock is acquired.
        //----------------------------------------------------------------

        theLock.acquire(locker, mode, true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "lock acquired",
                     new Object[] { lockName, locker, modeStrs[mode] });
        }

        return true;
    } // lock

    /**
     * Release the lock held by the given container transaction
     * for the given <code>Object</code>. <p>
     * 
     * It is an error to attempt to unlock a lock that has not been
     * locked. <p>
     * 
     * @param lockName the <code>Object</code> identifying the lock to
     *            release; this object instance must correctly
     *            implement the <code>hashCode</code>() and
     *            <code>equals</code> correctly with respect to the lock
     *            name space <p>
     * 
     * @param locker the <code>Locker</code> holding the lock to drop <p>
     * 
     */

    public void unlock(Object lockName, Locker locker)
    {
        synchronized (lockTable) {

            Object o = lockTable.remove(lockName);
            if (o == null || o == locker) {

                //----------------------------------------------------------
                // Fastpath: no conflicts, the given locker was only holder,
                // nothing to do.
                //----------------------------------------------------------

            } else {

                //--------------------------------------------------
                // Conflict: the lock table entry must be an actual
                // lock instance or an error has occurred.
                //--------------------------------------------------

                Lock theLock = (Lock) o;
                if (theLock.release(locker) != 0) {
                    lockTable.put(lockName, theLock);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
            Tr.debug(tc, "lock released", new Object[] { lockName, locker });
        }
    } // unlock

    /**
     * Release all locks held by the given container transaction. <p>
     * 
     * Used primarily for two purposes:
     * 1 - Releases lock that remain held due to failed creates. These must
     * be held to the end of the transaction, but there is no BeanO
     * enlisted with the transaction, so they don't get released through
     * the ActivationStrategy.
     * 2 - Cleanup due to internal error resulting in locks not released.
     * 
     * @param locker the <code>Locker</code> holding the locks to drop <p>
     */
    // d110984
    public void unlock(Locker locker)
    {
        // All access to the lockTable must be synchronized, even just checking
        // the size.  Since this method is for a transaction and not a bean
        // type, it is difficult to tell when no Option A caching has been
        // involved.  The performance impact should be minimal.         d114715
        synchronized (lockTable)
        {
            // If there are not locks, then don't do anything, including trace
            // entry/exit.  Either there are no Option A beans, or there just
            // don't happen to be any locks.
            if (lockTable.size() == 0)
            {
                return;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.entry(tc, "unlock", locker);

            // Loop through all of the locks in the table, and unlock any
            // locks held by the locker (containerTx).  The containerTx
            // holds the lock if it is the locker or if it is the holder
            // on a true Lock object.
            Enumeration lockNames = lockTable.keys();

            while (lockNames.hasMoreElements())
            {
                Object lockName = lockNames.nextElement();

                Object o = lockTable.get(lockName);
                if (o == locker)
                {
                    unlock(lockName, locker);
                }
                else if (((LockProxy) o).isLock())
                {
                    if (((Lock) o).isHolder(locker))
                    {
                        unlock(lockName, locker);
                    }
                } // if isLock()
            } // while lockNames

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                Tr.exit(tc, "unlock");

        } // synchronized
    } // unlock

    /*
     * Record the fact that lock requester has entered a wait Q.
     */

    void waitEvent(Waiter w) {
        waitTable.put(w.locker, w.theLock);
    } // waitEvent

    /*
     * Record the fact that lock requester has been removed from wait Q.
     */

    void unwaitEvent(Waiter w) {
        waitTable.remove(w.locker);
    } // unwaitEvent

    /*
     * This method returns true iff waiting for the given lock instance in
     * the given mode would cause a deadlock with respect to the set of
     * locks known to this <code>LockManager</code> instance. <p>
     */

    boolean detectDeadlock(Locker acquirer, Lock l, int mode) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.entry(tc, "detectDeadlock", new Object[] { acquirer, l,
                                                         modeStrs[mode] });
        }

        boolean result = false;

        //----------------------------------------------------------------
        // Iterate through the holders of the given lock and determine if
        // any of them are waiting on a lock held by the requestor. If
        // any of them are then a deadlock exists.
        //----------------------------------------------------------------

        Enumeration holders = l.getHolders();
        while (holders.hasMoreElements()) {
            if (lockerWaitingOn((Locker) holders.nextElement(), acquirer)) {
                result = true;
                break;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            Tr.exit(tc, "detectDeadlock:" + result);
        }
        return result;

    } // detectDeadlock

    /*
     * Return true iff the given locker is waiting on the given target;
     * recurse as necessary.
     */

    public boolean lockerWaitingOn(Locker locker, Locker target)
    {
        //-------------------------------------------------------------
        // Locker can be waiting on at most one lock. If that lock
        // is held by target or if any of the holders of that lock
        // is waiting on the target then locker is waiting on target.
        //-------------------------------------------------------------

        Lock waitingOn = (Lock) waitTable.get(locker);

        if (waitingOn == null) {
            return false;
        }

        Enumeration holders = waitingOn.getHolders();
        while (holders.hasMoreElements()) {
            Locker next = (Locker) holders.nextElement();
            if (next == target) {
                return true;
            }
            if (lockerWaitingOn(next, target)) {
                return true;
            }
        }
        return false;
    } // lockerWaitingOn

    /**
     * Return number of active locks.
     */

    public int size() {
        return lockTable.size();
    } // size

    /**
     * Dump internal state of lock manager.
     */

    public void dump() {

        if (!tc.isDumpEnabled()) {
            return;
        }

        Enumeration vEnum = lockTable.keys();

        Tr.dump(tc, "-- Lock Manager Dump --");
        while (vEnum.hasMoreElements()) {
            Object key = vEnum.nextElement();
            Tr.dump(tc, "lock table entry",
                    new Object[] { key, lockTable.get(key) });
        }

    } // dump

} // LockManager

