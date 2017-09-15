/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.sib.processor.utils;

// Import required classes.
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *  This class handles the synchronisation between the threads that
 * are processing proxy subscription creations and deletions.  While
 * those are being processed, the reset message can't be processed.
 * 
 * When the Reset message is being processed, the Create/Delete messasges
 * need to be suspended.
 */
public final class LockManager
{
  private static final TraceComponent tc =
    SibTr.register(
      LockManager.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
      
  /**
   * NLS for component
   */
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);  
      

  /** The object that the locking will take place round */
  private Object iMutex = new Object();

  /** Indicator for if the subscription table is locked */
  private boolean iExclusivelyLocked = false;

  /** The number of active locks held */
  private int iLockCount = 0;
  
  /** The number of exclusive locks held */
  private int iExclusiveLockCount = 0;

  /** The thread holder of the exclusive lock */
  Thread iExclusiveLockHolder = null;

  /** 
   * Constructor for the locking manager class
   */
  public LockManager()
  {
  }

  /** 
   * This method allows multiple lockers to lock the same mutex, 
   * until the lock exclusive is called.  Then all lock requesters
   * have to wait until the exclusive lock is released.
   *
   */
  public synchronized void lock()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "lock", this);

    boolean interrupted = false;

    // Attempt to get a lock on the mutex.
    // if we fail, then that is because the lock
    // must be held exclusively.
    while (!tryLock())
      try
      {
        // Wait for 1 second then try again.
        if (tc.isDebugEnabled())
          SibTr.debug(tc, "Waiting for lock");
        wait(1000);
      }
      catch (InterruptedException e)
      {
        // No FFDC code needed
        interrupted = true;
      }

    if (interrupted)
      Thread.currentThread().interrupt();

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "lock");

  }

  /** 
   * This method increments a the number of locks that have 
   * been obtained.
   * 
   * If the thread requesting the lock happens to be the thread
   * that already has the exclusive lock we let that succeed as 
   * we don't want to cause any dead locks !
   *
   * @return boolean  if the lock was achieved.
   *
   */
  private boolean tryLock()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "tryLock", this);

    boolean result = false;

    synchronized (iMutex)
    {
      // Check that we aren't exclusively locked.
      if (!iExclusivelyLocked)
      {
        iLockCount++;
        result = true;
      }
      // If the table lock holder is this thread, 
      // then allow it to succeed so it won't cause dead locks.
      else
        if (iExclusiveLockHolder == Thread.currentThread())
          result = true;
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "tryLock", new Boolean(result));

    return result;
  }

  /** 
   * This method decrements the number of locks that are held
   *
   */
  public synchronized void unlock()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "unlock", this);

    // If the subscription locks are down to zero then notify the threads waiting
    // for the subscriptionTable lock.
    synchronized (iMutex)
    {
      // If we are exclusively locked, then just return, as long as
      // the thread that is calling is the thread that holds the
      // exclusive lock.
      if (iExclusivelyLocked && iExclusiveLockHolder == Thread.currentThread())
      {
        if (tc.isEntryEnabled())
          SibTr.exit(tc, "unlock");
        return;
      }

      // Decrement the lock count and notify the other threads if no 
      // one holds the lock any more.
      if (--iLockCount == 0)
        notifyAll();
        
      if (iLockCount < 0)
      {      
        if (tc.isEntryEnabled()) 
          SibTr.exit(tc, "unlock", "SIErrorException");
        
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.utils.LockManager",
            "1:196:1.19" });
          
        throw new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.utils.LockManager",
              "1:203:1.19" },
            null));
      }        
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "unlock");
  }

  /** 
   * This method locks the mutex so no other lockers can
   * get the lock.
   *
   */
  public synchronized void lockExclusive()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "lockExclusive", this);

    boolean interrupted = false;

    // If another thread is attempting to lock exclusive, 
    // wait for it to finish first.
    while (!tryLockExclusive())
      try
      {
        if (tc.isDebugEnabled())
          SibTr.debug(tc, "Waiting to get exclusive lock");
        wait(1000);
      }
      catch (InterruptedException e)
      {
        // No FFDC code needed
        interrupted = true;
      }

    // Wait for existing locks to be released
    while (iLockCount > 0)
      try
      {
        if (tc.isDebugEnabled())
          SibTr.debug(tc, "Waiting for lock count to reach 0 " + iLockCount);
        wait(1000);
      }
      catch (InterruptedException e)
      {
        // No FFDC code needed
        interrupted = true;
      }

    if (interrupted)
      Thread.currentThread().interrupt();

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "lockExclusive");
  }

  /** 
   * This method attempts to lock the exclusively and
   * won't succeed until it has the exclusive lock.
   *
   * @return boolean true if it is now the exclusive lock holder,
   *                  false if a wait is still needed to lock 
   *                  exclusively
   *
   */
  private boolean tryLockExclusive()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "tryLockExclusive", this);

    boolean result = false;

    // Synchronize on the locking Mutex
    synchronized (iMutex)
    {
      // If it isn't already locked - lock it on this thread.
      if (!iExclusivelyLocked)
      {
        // Block new many lock requests
        iExclusivelyLocked = true;

        // Set the exclusive lock holder to be this thread
        iExclusiveLockHolder = Thread.currentThread();

        if (tc.isDebugEnabled())
          SibTr.debug(tc, "Got exclusive lock for thread " + iExclusiveLockHolder);

        // Set the return result to be true
        result = true;
        
        // Increment the exclusive lock count
        iExclusiveLockCount++;
      }
      // If we are locked, then check that the thread that is requesting
      // this lock is the same thread that owns the lock already.
      else if (iExclusiveLockHolder == Thread.currentThread())
      {
        if (tc.isDebugEnabled())
          SibTr.debug(tc, "Already hold exclusive lock " + (iExclusiveLockCount + 1));
        result = true;
        // Increment the exclusive lock count
        iExclusiveLockCount++;
        
      }
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "tryLockExclusive", new Boolean(result));

    return result;
  }

  /** 
   * This method unlocks the exclusive lock that was held.
   * 
   * It also notifies any lock waiters.
   * 
   */
  public synchronized void unlockExclusive()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "unlockExclusive", this);

    // Synchronize on the locking Mutex.
    synchronized (iMutex)
    {
      // Only release the lock if the holder is the current thread.
      if (Thread.currentThread() == iExclusiveLockHolder)
      {
        if (tc.isDebugEnabled())
          SibTr.debug(tc, "Unlocking current thread " + (iExclusiveLockCount - 1));

        // Decrement the exclusive lock count,
        // if the count reaches 0 then we know that 
        // we can safely remove the exclusive lock
        if (--iExclusiveLockCount == 0)
        {
          // Set the flag to indicate that the exclusive lock
          // has been released.
          iExclusivelyLocked = false;

          // Set the exclusive lock holder thread to be null
          iExclusiveLockHolder = null;

          // Notify any threads waiting for this lock.
          notifyAll();
        }
      }
      else if (tc.isDebugEnabled())
        SibTr.debug(tc, "Thread not the current thread to unlock exclusively");
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "unlockExclusive");

  }

}
