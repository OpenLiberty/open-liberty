/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.recoverylog.spi;

import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;

//------------------------------------------------------------------------------
// Class: Lock
//------------------------------------------------------------------------------
/**
* <p>
* The Lock class provides a mechanism to control access to the data structures that make up the
* in memory log. Callers can obtain two different types of 'lock' on an instance of the Lock class.
* Multiple threads may obtain concurrent 'shared' locks but only one thread at a time may obtain
* the 'exclusive' lock. 
* </p>
*
* <p>
* Normal Running Mode (Current log file is 'filling up'):-
* </p>
*
* <p>
* Shared locks are requested and released by multiple threads without blocking each other (ie
* multiple shared locks may be held by different threads at the same time) No thread is requesting
* or holding the exclusive lock.
* </p>
*
* <p>
* Exclusive Lock Requested (Threads start to discover a keypoint is required requests exclusive lock)
* </p>
*
* <p>
* Typically, a number of threads will detect that the log has run out of space and must keypoint 
* at rougly the same time. Each thread will direct a keypoint operation to occur but the code
* needs to be smart enough to perform only a single keypoint in response. 
* </p>
*
* <p>
* The required behaviour is that one of these threads perform the actuall keypoint whilst the other
* threads wait until this keypoint is complete and then return as if they had driven the keypoint.
* The code that invoked the keypoint does not know if it occured on the same thread but is guarenteed
* that all active data in memory prior to the keypoint call is stored on disk.
* </p>
*
* <p>
* An unusual locking model is required to support this. The keypoint operation will attempt to obtain
* the exclusive lock before performing the keypoint logic. It may or may not be granted (see below)
* If it is granted then the real keypoint logic is performed, otherwise the operation is actually a
* no-op. The 'attemptExclusiveLock' method returns a boolean to indicate if the lock has been
* granted or not.
* </p>
*  
* <p>
* A group of threads make the request to obtain the exclusive lock for a given keypoint event.
* These requests occur around about the same time but ultlimatly only one of these requests
* will actually be granted. The others will not be granted but the calls will remain blocked until
* the thread that does get the lock releases it again after performing a keypoint operation.
* At this point all threads will be released, those that did not get the lock simply continue
* without performing a keypoint and the cycle is ready to repeat at the next keypoint event.
* </p>
* 
* <p>
* The exclusive lock can't be granted if other threads in the system hold shared locks and this
* imposes a couple of extra rules :
*
* <ul>
* <li>1. Before a thread that requests the exclusive lock when another thread is requesting the
*     exclusive lock already can block it must "suspend" its shared locks (these are
*     restored again when it is released)</li>
* <li>2. Once a thread is requsting the exclusive lock, other threads can't be granted new shared locks
*     unless they already hold a shared lock. This will allow existing work on such threads to
*     complete (ie get to a point where the thread holds no shared locks) but not prevent this from
*     occuring by stopping further shared lock requests.</li>
* </ul>
* </p>
*/
public class Lock
{
  /**
  * WebSphere RAS TraceComponent registration
  */
  private static final TraceComponent tc = Tr.register(Lock.class,
                                           TraceConstants.TRACE_GROUP, null);

  /**
  * Map of thread to Integer containing the number of shared locks held by that thread.
  */
  private java.util.HashMap _sharedThreads = null;

  /**
  * The identity of the thread that currently holds the exclusive lock (if any)
  */
  private Thread            _threadHoldingExclusiveLock  = null;

  /**
  * The identity of the thread that currently requests the exclusive lock (if any)
  */
  private Thread            _threadRequestingExclusiveLock  = null;

  /**
  * The 'traceId' string is output at key trace points to allow easy mapping
  * of recovery log operations to clients logs.
  */
  private String _traceId;

  //------------------------------------------------------------------------------
  // Method: Lock.Lock
  //------------------------------------------------------------------------------
  /**
  * Default constructor. Creates and initializes a new Lock object.
  *
  * @param ownerIdentifier Free-form string supplied by the "owner" of this lock
  *                        object to distinguish it from other lock objects.
  */ 
  public Lock(String ownerIdentifier)
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "Lock",ownerIdentifier);

    _sharedThreads = new java.util.HashMap();
    _threadHoldingExclusiveLock = null;
    _threadRequestingExclusiveLock  = null;

    _traceId = "Lock:" + "ownerIdentifier=" + ownerIdentifier + " @" +System.identityHashCode(this);

    if (tc.isEntryEnabled()) Tr.exit(tc, "Lock", this);
  }

  //------------------------------------------------------------------------------
  // Method: Lock.getSharedLock
  //------------------------------------------------------------------------------
  /**
  * This method is called to request a shared lock. There are conditions under which a
  * shared lock cannot be granted and this method will block until no such conditions
  * apply. When the method returns the thread has been granted an additional shared
  * lock. A single thread may hold any number of shared locks, but the number of 
  * requests must be matched by an equal number of releases.
  *
  * @param requestId The 'identifier' of the lock requester. This is used ONLY for
  *                  trace output. It should be used to 'pair up' get/set pairs in
  *                  the code.
  */
  public void getSharedLock(int requestId)
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "getSharedLock",new Object[] {this,new Integer(requestId)});

    Thread currentThread = Thread.currentThread();
    Integer count = null;

    synchronized(this)
    {
      // If this thread does not have any existing shared locks and there is a thread waiting to get the exclusive
      // lock or a thread (other than this thread) who already holds the exclusive lock then we will have to wait
      // until the exclusive lock is eventually dropped before granting the shared lock.
      if ( (!_sharedThreads.containsKey(currentThread)) &&
           ( (_threadRequestingExclusiveLock != null) ||
             ((_threadHoldingExclusiveLock != null) && (!_threadHoldingExclusiveLock.equals(currentThread)))))
      {
        while ((_threadHoldingExclusiveLock != null) || (_threadRequestingExclusiveLock != null))
        {
          if (tc.isDebugEnabled()) Tr.debug(tc, "Thread " + Integer.toHexString(currentThread.hashCode()) + " is waiting for the exclusive lock to be released");
          try
          {
            if (tc.isDebugEnabled()) Tr.debug(tc, "Thread " + Integer.toHexString(currentThread.hashCode()) + " waiting..");
            this.wait();
          }
          catch(java.lang.InterruptedException exc)
          {
            FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.Lock.getSharedLock", "180", this);
            if (tc.isDebugEnabled()) Tr.debug(tc, "Thread " + Integer.toHexString(currentThread.hashCode()) + " was interrupted unexpectedly during wait. Retesting condition");
            // This exception is recieved if another thread interrupts this thread by calling this threads 
            // Thread.interrupt method. The Lock class does not use this mechanism for breaking out of the
            // wait call - it uses notifyAll to wake up all waiting threads. This exception should never
            // be generated. If for some reason it is called then ignore it and start to wait again.
          }
        }
        if (tc.isDebugEnabled()) Tr.debug(tc, "Thread " + Integer.toHexString(currentThread.hashCode()) + " is has detected that the exclusive lock has been released");
      }

      count = (Integer)_sharedThreads.get(currentThread);

      if (count == null)
      {
        count = new Integer(1);
      }
      else
      {
        count = new Integer(count.intValue()+1);
      }

      _sharedThreads.put(currentThread,count);
    }
    
    if (tc.isDebugEnabled()) Tr.debug(tc, "Count: " + count);

    if (tc.isEntryEnabled()) Tr.exit(tc, "getSharedLock");
  }

  //------------------------------------------------------------------------------
  // Method: Lock.releaseSharedLock
  //------------------------------------------------------------------------------
  /**
  * Releases a single shared lock from thread.
  *
  * @exception NoSharedLockException The caller does not hold a shared lock to release
  *
  * @param requestId The 'identifier' of the lock requester. This is used ONLY for
  *                  trace output. It should be used to 'pair up' get/set pairs in
  *                  the code.
  */
  public void releaseSharedLock(int requestId) throws NoSharedLockException
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "releaseSharedLock",new Object[]{this,new Integer(requestId)});

    Thread currentThread = Thread.currentThread();
    int newValue = 0;

    synchronized(this)
    {
      Integer count = (Integer)_sharedThreads.get(currentThread);

      if (count == null)
      {
        if (tc.isEntryEnabled()) Tr.exit(tc, "releaseSharedLock", "NoSharedLockException");
        throw new NoSharedLockException();
      }

      newValue = count.intValue()-1;

      if (newValue > 0)
      {
        count = new Integer(newValue);
        _sharedThreads.put(currentThread,count);
      }
      else
      {
        count = null;
        _sharedThreads.remove(currentThread);
      }

      // If this thread no longer holds any shared locks then inform those waiting of this
      // fact.
      if (count == null)
      {
        this.notifyAll();
      }
    }

    if (tc.isDebugEnabled())
    {
      int countValue = 0;
      Integer count = (Integer)_sharedThreads.get(currentThread); 
      
      if (count != null) countValue = count.intValue();
      
      Tr.debug(tc, "Count: " + count);          
    }
    
    if (tc.isEntryEnabled()) Tr.exit(tc, "releaseSharedLock");
  }

  //------------------------------------------------------------------------------
  // Method: Lock.attemptExclusiveLock
  //------------------------------------------------------------------------------
  /**
  * Requests the 'exclusive' lock. Multiple threads may hold shared locks concurrently
  * but only one thread may hold the 'exclusive' lock at a time. 
  *
  * @exception HoldingExclusiveLockException This thread already holds the exclusive lock.
  * @return boolean Indicates if the caller has managed to obtain the exclusive lock.
  */
  public boolean attemptExclusiveLock() throws HoldingExclusiveLockException
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "attemptExclusiveLock",this);

    boolean obtained = false;

    Thread currentThread = Thread.currentThread();

    synchronized(this)
    {
      if ((_threadHoldingExclusiveLock != null) && (_threadHoldingExclusiveLock.equals(currentThread)))
      {
        // This thread already holds the exclusive lock. A thread is not allowed to request the exclusive
        // lock again if it already holds it.
        if (tc.isEntryEnabled()) Tr.exit(tc, "attemptExclusiveLock", "HoldingExclusiveLockException");
        throw new HoldingExclusiveLockException();
      }
      else
      {
        if ((_threadHoldingExclusiveLock != null) || (_threadRequestingExclusiveLock != null))
        {
          // Another thread is either holding or requesting the exclusive lock. This thread will 
          // not be granted the lock and will return with 'false'. Before returning, this
          // thread will wait for the exclusive lock to be released.

          // Before we start waiting for the exclusive lock release event, we must ensure we
          // "suspend" any shared locks this thread currently holds. If this is not done, then
          // these shared locks will prevent an exclusive lock from being granted and
          // hence deadlock the system. When the waiting is complete these shared locks will be
          // restored. Notify other waiting threads so that they know that some more
          // shared locks have been removed.
          Integer suspendedSharedLockCount = (Integer)_sharedThreads.get(currentThread);
          _sharedThreads.remove(currentThread);
          this.notifyAll();
     
          if (tc.isDebugEnabled()) Tr.debug(tc, "Thread " + Integer.toHexString(currentThread.hashCode()) + " is waiting for the exclusive lock to be released");

          while ((_threadHoldingExclusiveLock != null) || (_threadRequestingExclusiveLock != null))
          {
            try
            {
              if (tc.isDebugEnabled()) Tr.debug(tc, "Thread " + Integer.toHexString(currentThread.hashCode()) + " waiting..");
              this.wait();
            }
            catch(java.lang.InterruptedException exc)
            {
              FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.Lock.attemptExclusiveLock", "325", this);
              if (tc.isDebugEnabled()) Tr.debug(tc, "Thread " + Integer.toHexString(currentThread.hashCode()) + " was interrupted unexpectedly during wait. Retesting condition");
              // This exception is recieved if another thread interrupts this thread by calling this threads 
              // Thread.interrupt method. The Lock class does not use this mechanism for breaking out of the
              // wait call - it uses notifyAll to wake up all waiting threads. This exception should never
              // be generated. If for some reason it is called then ignore it and start to wait again.
            }
          }

          if (tc.isDebugEnabled()) Tr.debug(tc, "Thread " + Integer.toHexString(currentThread.hashCode()) + " has detected that the exclusive lock has been released");

          // Resume the shared locks that were suspened prior to the wait
          if (suspendedSharedLockCount != null) _sharedThreads.put(currentThread,suspendedSharedLockCount);           //  PK96860
   
          obtained = false;          
        }
        else
        {
          // No other thread is holding or waiting for the exclusive lock. This thread will obtain it when
          // there are no shared locks (other that this threads) in the system.

          // First, update _threadRequestingExclusiveLock so other threads will know we are
          // requesting the exclusive lock.
          _threadRequestingExclusiveLock = currentThread;

          if (tc.isDebugEnabled()) Tr.debug(tc, "Thread " + Integer.toHexString(currentThread.hashCode()) + " is waiting for the shared locks to quiesce");

          while ( (_sharedThreads.size() > 1) ||
                  ((_sharedThreads.size() == 1) && (!_sharedThreads.containsKey(currentThread))) )
          {
            try
            {
              if (tc.isDebugEnabled()) Tr.debug(tc, "Thread " + Integer.toHexString(currentThread.hashCode()) + " waiting..");
              this.wait();
            }
            catch(java.lang.InterruptedException exc)
            {
              FFDCFilter.processException(exc, "com.ibm.ws.recoverylog.spi.Lock.attemptExclusiveLock", "362", this);
              if (tc.isDebugEnabled()) Tr.debug(tc, "Thread " + Integer.toHexString(currentThread.hashCode()) + " was interrupted unexpectedly during wait. Retesting condition");
              // This exception is recieved if another thread interrupts this thread by calling this threads 
              // Thread.interrupt method. The Lock class does not use this mechanism for breaking out of the
              // wait call - it uses notifyAll to wake up all waiting threads. This exception should never
              // be generated. If for some reason it is called then ignore it and start to wait again.
            }
          }

          if (tc.isDebugEnabled()) Tr.debug(tc, "Thread " + Integer.toHexString(currentThread.hashCode()) + " has detected that the shared locks have quiesced");

          _threadHoldingExclusiveLock = currentThread;
          _threadRequestingExclusiveLock = null;
          obtained = true;
        }
      }
    }
        
    if (tc.isEntryEnabled()) Tr.exit(tc, "attemptExclusiveLock","" + obtained);

    return obtained;
  }

  //------------------------------------------------------------------------------
  // Method: Lock.releaseExclusiveLock
  //------------------------------------------------------------------------------
  /**
  * Releases the exclusive lock.
  *
  * @exception NoExclusiveLockException The caller does not hold the exclusive lock.
  */
  public void releaseExclusiveLock() throws NoExclusiveLockException
  {
    if (tc.isEntryEnabled()) Tr.entry(tc, "releaseExclusiveLock",this);

    Thread currentThread = Thread.currentThread();

    synchronized(this)
    {
      if ((_threadHoldingExclusiveLock == null) || (!_threadHoldingExclusiveLock.equals(currentThread)))
      {
        if (tc.isEntryEnabled()) Tr.exit(tc, "releaseExclusiveLock", "NoExclusiveLockException");
        throw new NoExclusiveLockException();
      }

      _threadHoldingExclusiveLock = null;
      _threadRequestingExclusiveLock = null;

      // There may be thread waiting for the exclusive lock to be released. Notify them of this event.
      this.notifyAll();
    }

    if (tc.isEntryEnabled()) Tr.exit(tc, "releaseExclusiveLock");
  }

  //------------------------------------------------------------------------------
  // Method: Lock.toString
  //------------------------------------------------------------------------------
  /**
  * Returns the string representation of this object instance.
  * 
  * @return String The string representation of this object instance.
  */
  public String toString()
  {
  	return _traceId;
  }

  //------------------------------------------------------------------------------
  // Method: Lock.DEBUG_showLocks
  //------------------------------------------------------------------------------
  /**
  * DEBUG code to dump the thread -> lock mappings.
  *
  * Commented out to ensure that it is not compiled into the code by mistake.
  * Re-enable for debug pruposes only.
  */
  /*
  private synchronized void DEBUG_showLocks()
  {
    Set entrySet = _sharedThreads.entrySet();
    Iterator entrySetIterator = entrySet.iterator();

    //System.out.println("Requesting exclusive lock thread :");
    if (tc.isDebugEnabled()) Tr.debug(tc, "Requesting exclusive lock thread :");

    if (_threadRequestingExclusiveLock != null)
    {
      //System.out.println("Thread " + Integer.toHexString(_threadRequestingExclusiveLock.hashCode()));
      if (tc.isDebugEnabled()) Tr.debug(tc, "Thread " + Integer.toHexString(_threadRequestingExclusiveLock.hashCode()));
    }
    else
    {
      //System.out.println("Not held");
      if (tc.isDebugEnabled()) Tr.debug(tc, "Not held");
    }

    //System.out.println("Holding exclusive lock thread :");
    if (tc.isDebugEnabled()) Tr.debug(tc, "Holding exclusive lock thread :");

    if (_threadHoldingExclusiveLock != null)
    {
      //System.out.println("Thread " + Integer.toHexString(_threadHoldingExclusiveLock.hashCode()));
      if (tc.isDebugEnabled()) Tr.debug(tc, "Thread " + Integer.toHexString(_threadHoldingExclusiveLock.hashCode()));
    }
    else
    {
      //System.out.println("Not held");
      if (tc.isDebugEnabled()) Tr.debug(tc,"Not held" );
    }

    //System.out.println("Shared locks :");
    if (tc.isDebugEnabled()) Tr.debug(tc, "Shared locks :");

    if (entrySetIterator.hasNext())
    {
      while (entrySetIterator.hasNext())
      {
        Map.Entry entry = (Map.Entry)entrySetIterator.next();

        Thread thread = (Thread)entry.getKey();
        Integer count = (Integer)entry.getValue();

        //System.out.println("Thread " + " " + Integer.toHexString(thread.hashCode()) + " " + count.intValue());
        if (tc.isDebugEnabled()) Tr.debug(tc, "Thread " + " " + Integer.toHexString(thread.hashCode()) + " " + count.intValue());
      }
    }
    else
    {
      //System.out.println("Not held");
      if (tc.isDebugEnabled()) Tr.debug(tc,"Not held" );
    }
  }
  */
}

