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
import java.util.IdentityHashMap;
import java.util.Iterator;

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
public final class AdvancedLockManager
{
  private static final TraceComponent tc =
    SibTr.register(
      AdvancedLockManager.class,
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
  private int readLockCount = 0;
  private int readerThreadCount = 0;
  
  /** The number of exclusive locks held */
  private int iExclusiveLockCount = 0;

  /** The thread holder of the exclusive lock */
  private Thread iExclusiveLockHolder = null;
  
  private IdentityHashMap readerThreads = new IdentityHashMap();

  private static class LockCount
  {
    public int count = 0;
  }
  
  /** 
   * Constructor for the locking manager class
   */
  public AdvancedLockManager()
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
      if (!iExclusivelyLocked || iExclusiveLockHolder == Thread.currentThread())
      {
        incrementThreadLockCount();
        result = true;
      }      
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "tryLock", new Boolean(result));

    return result;
  }

  /**
   * The mutex must be held before calling this method.
   * 
   * Increments the read lock count for a given thread. If this is the
   * first time a given thread has taken a read lock, a new entry is
   * created for it in the HashMap.
   */
  private void incrementThreadLockCount()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "incrementThreadLockCount", this);
    //get the current thread
    Thread currentThread = Thread.currentThread();
    //get it's current read lock count
    LockCount count = (LockCount) readerThreads.get(currentThread);
    //if it doesn't exist, create it
    if(count == null)
    {
      count = new LockCount();
      readerThreads.put(currentThread, count);
    }
    //increment the count
    if(count.count == 0) readerThreadCount++;
    count.count++;
    readLockCount++;
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "incrementThreadLockCount");
  }
  
  /**
   * The mutex must be held before calling this method.
   * 
   * Increments the read lock count for a given thread. If this is the
   * first time a given thread has taken a read lock, a new entry is
   * created for it in the HashMap.
   */
  private void decrementThreadLockCount()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "decrementThreadLockCount", this);
    //get the current thread
    Thread currentThread = Thread.currentThread();
    //get it's current read lock count
    LockCount count = (LockCount) readerThreads.get(currentThread);
    //if it doesn't exist, do nothing
    if(count != null && count.count > 0)
    {
      //decrement the count
      count.count--;
      if(count.count == 0) readerThreadCount--;
      readLockCount--;
    }
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "decrementThreadLockCount");
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
      if (readLockCount == 0)
      {
        if (tc.isEntryEnabled()) 
          SibTr.exit(tc, "unlock", "SIErrorException");
        
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.utils.AdvancedLockManager",
            "1:237:1.3" });
          
        throw new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.utils.AdvancedLockManager",
              "1:244:1.3" },
            null));
      }
      
      decrementThreadLockCount();
      // Decrement the lock count and notify the other threads if no 
      // one holds the lock any more.
      if (readLockCount == 0)
        notifyAll();        
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
    while (alienReadLocksHeld())
      try
      {
        if (tc.isDebugEnabled())
          SibTr.debug(tc, "Waiting for read lock thread count to reach 0 or 1(this thread) " + readerThreadCount);
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
   * The mutex must be help before calling this method.
   * 
   * Are there any read locks held by threads other than the current one?
   * 
   * @return true if there any read locks held by threads other than the current one
   */
  private boolean alienReadLocksHeld()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "alienReadLocksHeld", this);
    
    boolean locksHeld = false;
    
    //if there is more than one thread holding read locks then return true
    if(readerThreadCount > 1)
    {
      locksHeld = true;
    }
    //if there is exactly one thread holding a read lock...
    else if(readerThreadCount == 1)
    {
      //check if it is the current thread holding that lock
      Thread currentThread = Thread.currentThread();
      LockCount count = (LockCount) readerThreads.get(currentThread);
      //if the current thread does not hold any locks then return true since
      //it must be a different thread.
      locksHeld = (count == null || count.count == 0);
    }
    //if there are no threads holding read locks then return false (default)
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "alienReadLocksHeld", new Boolean(locksHeld));
    
    return locksHeld;
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

  public String toString()
  {
    StringBuffer buf = new StringBuffer("Current thread: ");
    buf.append(Thread.currentThread());
    buf.append("\n");
    synchronized(iMutex)
    {      
      buf.append("Exclusive Lock:\n");
      if(iExclusiveLockHolder != null)
      {
        buf.append(iExclusiveLockHolder);
        buf.append(" = ");
        buf.append(iExclusiveLockCount);
        buf.append("\n");
      }
      
      buf.append("Read Locks:\n");
      Iterator itr = readerThreads.keySet().iterator();
      while(itr.hasNext())
      {
        Thread thread = (Thread) itr.next();
        buf.append(thread);
        buf.append(" = ");
        buf.append(((LockCount)readerThreads.get(thread)).count);
        buf.append("\n");
      }
    }
    return buf.toString();
  }  
}
