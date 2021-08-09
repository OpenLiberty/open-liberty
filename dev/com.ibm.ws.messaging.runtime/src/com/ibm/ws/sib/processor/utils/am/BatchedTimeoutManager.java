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

package com.ibm.ws.sib.processor.utils.am;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.Alarm;

import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.utils.LockManager;
import com.ibm.ws.sib.processor.utils.linkedlist.LinkedList;
import com.ibm.ws.sib.processor.utils.linkedlist.SimpleLinkedListEntry;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.ws.util.ObjectPool;

/**
 * A BatchedTimeoutManager manages a number of grouped alarms using the MPAlarmManager.
 * 
 * Alarms are created using a large percentage late value. This makes batching more likely.
 * The alarms are grouped so that there is a single call to the alarm method for each batch
 * of alarms which are fired.
 * 
 * Alarms are started by passing in a reusable BatchedTimeoutEntry object. When the group alarm
 * for a BTE is fired, it is automatically restarted until the BTE is removed.
 * 
 * A BatchedTimeoutManager is created in a stopped state and no alarms are created. It can be started
 * and stopped at any time.
 */
public class BatchedTimeoutManager implements GroupAlarmListener
{
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  
  private ObjectPool linkedListEntryPool;
  //the unique id of this GroupAlarmListener
  private SIBUuid12 uuid = new SIBUuid12();
  //the time the MPAM should wait before firing each alarm
  private long delta;
  // Number of buckets (used when timeout is later changed)
  private int numOfBuckets;
  //the percent by which an alarm can be late
  private int percentLate;
  //the list of currently active BTEs
  protected LinkedList activeEntries;
  //trace
  private static final TraceComponent tc =
    SibTr.register(
      BatchedTimeoutManager.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

 
  //The MPAlarmManager instance
  private MPAlarmManager am;
  //the BatchedTimeoutProcessor which will do the work when a group alarm is fired
  private BatchedTimeoutProcessor handler;
  //the list of BTEs which were fired in each batch
  private ArrayList<BatchedTimeoutEntry> timedout; 
  //indicates if this BatchedTimeoutManager is stopped
  private boolean isStopped; // this has not been started
  private LockManager btmLockManager = new LockManager();
  private LockManager timeoutLockManager = new LockManager();
  // Flags to indicate a failure in the last batch
  private boolean batchFailed = false;
  private boolean batchCleared = false;

  /**
   * an entry in the linked list of active BTEs
   */
  public static class LinkedListEntry extends SimpleLinkedListEntry
  {
    //the running alarm, if any
    Alarm alarm;
    //the owning BTE
    public BatchedTimeoutEntry bte;
    
    public synchronized String toString(String indent)
    {    
      StringBuffer buffer = new StringBuffer();
  
      buffer.append(indent);
      buffer.append("LinkedListEntry("+bte+",");
      if(parentList == null)
      {
        buffer.append(" not in list)");
      }
      else
      {
        buffer.append("\n" + indent+indent+"\\->"+alarm + ")");    
      }      
    
      return buffer.toString();
    }
  }
  /**
   * Constructor.
   *
   * @param numOfBuckets The number of buckets. Must be > 1
   * @param timeoutInterval The target time between alarms
   * @param timeoutEntries The timeout entries to begin with.
   * @param handler The handler to call when a timeout occurs.
   */
  public BatchedTimeoutManager(int numOfBuckets,
                               long timeoutInterval,
                               List timeoutEntries,
                               BatchedTimeoutProcessor handler,
                               MessageProcessor messageProcessor)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "BatchedTimeoutManager",
        new Object[] {
          Integer.valueOf(numOfBuckets),
          Long.valueOf(timeoutInterval),
          timeoutEntries,
          handler,
          messageProcessor });

          
    btmLockManager.lockExclusive();
    try
    {
      //get a reference to the alarm manager
      am = messageProcessor.getAlarmManager();
      //get a reference to the object pool
      linkedListEntryPool = messageProcessor.getBatchedTimeoutManagerEntryPool();
      //calculate the target accuracy (% late)
      this.delta = timeoutInterval;
      this.numOfBuckets = numOfBuckets;
      long bucketInterval = delta / (numOfBuckets - 1);
      percentLate = (int) (100 * bucketInterval / delta);
      //initialize the list of BTEs fired in each batch
      timedout = new ArrayList<BatchedTimeoutEntry>(10);
      //create the list of active BTEs
      activeEntries = new LinkedList();
      //start stopped
      isStopped = true;
      
      this.handler = handler;
      
      //add the initial entries to the active list
      if(timeoutEntries != null)
      {
        Iterator itr = timeoutEntries.iterator();
        while(itr.hasNext())
        {
          addTimeoutEntry((BatchedTimeoutEntry)itr.next());
        }
      }
    }
    finally
    {
      btmLockManager.unlockExclusive();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    	SibTr.exit(this, tc, "BatchedTimeoutManager");
  }

  /**
   * Update the timeout interval of this alarm, the change will come into effect the
   * next time an alarm is scheduled (so the next alarm will pop on the old timeout)
   * 
   * @param newTimeout
   */
  public void updateTimeout(long newTimeout)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "updateTimeout", Long.valueOf(newTimeout));
    
    this.delta = newTimeout;
    long bucketInterval = delta / (numOfBuckets - 1);
    percentLate = (int) (100 * bucketInterval / delta);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateTimeout", Integer.valueOf(percentLate));
  }

  /**
   * The group alarm call. The context on this call will be null.
   */
  public void alarm(Object context)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    	SibTr.entry(this, tc, "alarm", context);
    
    btmLockManager.lock();
    boolean btmLocked = true;
    timeoutLockManager.lockExclusive();
    boolean timeoutLocked = true;
    try
    {
      //we won't be called unless there are alarms which have timedout but check anyway
      if (!timedout.isEmpty() && !isStopped)
      {
        btmLockManager.unlock();
        btmLocked = false;
        //see defect 280069:
        //So that we can release the exclusive lock before calling processTimedoutEntries
        //(and so avoid deadlock on streamStatus monitor) we take a local copy 
        //(which therefore cannot be modified by anyone behind us) and create 
        //a new list; this replaces holding the exclusive lock for the duration of the method
        //and then clearing the list at the end. 
        
        List tempTimeoutList = timedout; //take a local copy of the list
        
        timedout = new ArrayList<BatchedTimeoutEntry>(); //the timeout entries will be dealt with
        //presently. Meantime we empty the timedout list so that subsequent timeouts
        //can be added to it without affecting processTimedoutEntries
        
        timeoutLockManager.unlockExclusive(); //we no longer care if timedout
        timeoutLocked = false;
        //changes underneath us so we can release the exclusive lock.

        try
        {
          //call the processor with the list of timedout BTEs, which should not be changed
          handler.processTimedoutEntries(tempTimeoutList);
          
          // We had a good batch - clear any previous failures
          batchFailed = false;
          batchCleared = false;
        }
        catch(Throwable e)
        {
          // FFDC the problem with the alarm, but don't re-throw the exception
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.utils.am.BatchedTimeoutManager.alarm",
            "1:280:1.24",
            new Object[] {this, handler, tempTimeoutList});
          
          SibTr.exception(tc, e);          
          
          // Alarms registered with the BatchedTimeoutManager are set to auto-repeat,
          // if we simply report the failure in an alarm it'll probably happen again
          // and again, forever. So we need to take steps to stop that happening, by
          // removing entries and possibly even stopping the manager (athough as a manager
          // is specific to a certain object (e.g. a specific AOStream) the damage should 
          // be contained - although still serious.
          
          // We worked the previous time so we'll give the alarms one more try before
          // bombing out
          if(!batchFailed)
            batchFailed = true;
         // If we've failed before, this was our last chance, something is wrong with
          // at least one of the entries, but we can't tell which one so we'll have to
          // remove all the entries - which will stop the error happening over and over
          // again (but not fix the reason for the error!)
          else
          {
            batchFailed = false;
            
            // Remove all the entries and see if that stops the error happening
            Iterator itr = tempTimeoutList.iterator();
            while(itr.hasNext())
            {
              BatchedTimeoutEntry bte = (BatchedTimeoutEntry)itr.next();
              removeTimeoutEntry(bte);
            }

            // If we've already been here and cleared the entries but something has
            // reregistered a new alarm that caused another problem (probably the same
            // one) we just give up and stop the alarm manager to prevent new alarms
            // being registered
            if(batchCleared)
            {
              // This is serious, throw another FFDC
              SIErrorException e2 =
                new SIErrorException(
                  nls.getFormattedMessage(
                    "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                    new Object[] {
                      "com.ibm.ws.sib.processor.utils.am.BatchedTimeoutManager.alarm",
                      "1:325:1.24" },
                    null), e);
              
              FFDCFilter.processException(
                e2,
                "com.ibm.ws.sib.processor.utils.am.BatchedTimeoutManager.alarm",
                "1:331:1.24",
                this);
              
              SibTr.exception(tc, e2);
              
              // Stop the timer
              stopTimer();
            }
            // Remember that we've just cleared up after a problem
            else
              batchCleared = true;
          }
        }
        //restart the entries
        restartEntries(tempTimeoutList);      
      }
    }
    finally
    {
      if(timeoutLocked)
        timeoutLockManager.unlockExclusive();
      if(btmLocked)
        btmLockManager.unlock();
    }
        
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    	SibTr.exit(tc, "alarm");
  }

  /**
   * Restart alarms for a a list of BTEs
   * 
   * @param timedout the list of BTEs to be restarted
   */
  private void restartEntries(List timedout)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "restartEntries", new Object[] { timedout });

    //iterate over the list of entries
    Iterator itr = timedout.iterator();
    while(itr.hasNext())
    {
      BatchedTimeoutEntry bte = (BatchedTimeoutEntry)itr.next();
      //get the BTE's entry in the active list
      LinkedListEntry entry = bte.getEntry();
      //if it is not in the list (i.e. the entry is null) then it has already been removed
      if(entry != null && activeEntries.contains(entry))
      {
        //start a new alarms
        startNewAlarm(entry);
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    	SibTr.exit(tc, "restartEntries");
  }

  /**
   * Start the BatchedTimeoutManager
   */
  public void startTimer()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    	SibTr.entry(tc, "startTimer");
    btmLockManager.lockExclusive();
    try
    {
      //only start if currently stopped
      if (isStopped)
      {
        //set stopped to false
        isStopped = false;
        //iterate over the entries currently in the active list
        LinkedListEntry entry = (LinkedListEntry) activeEntries.getFirst();
        while(entry != null && activeEntries.contains(entry))
        {
          //start an alarm for each entry
          startNewAlarm(entry);
          entry = (LinkedListEntry) entry.getNext();
        }
      }
    }
    finally
    {
      btmLockManager.unlockExclusive();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    	SibTr.exit(tc, "startTimer");
  }

  /**
   * Start a new alarm for a given entry in the active list.
   * 
   * @param entry the entry in the active list
   */
  private void startNewAlarm(LinkedListEntry entry)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "startNewAlarm", new Object[] { entry });

    //start a new alarm
    entry.alarm = am.create(delta, percentLate, this, entry);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "startNewAlarm");
  }

  /**
   * Stop the BatchedTimeoutManager
   */
  public void stopTimer()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    	SibTr.entry(tc, "stopTimer");
    btmLockManager.lockExclusive();
    try
    {
      //only stop if we are currently started
      if (!isStopped)
      {
        //set stopped to true
        isStopped = true;
        //iterate over the entries in the active list
        LinkedListEntry entry = (LinkedListEntry) activeEntries.getFirst();
        while(entry != null && activeEntries.contains(entry))
        {
          //cancel the alarm for each one
          if(entry.alarm != null)
          {
            entry.alarm.cancel();
            entry.alarm = null;
          }           
          entry = (LinkedListEntry) entry.getNext();
        }
      }
    }
    finally
    {
      btmLockManager.unlockExclusive();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    	SibTr.exit(tc, "stopTimer");
  }

  /**
   * Method to close this timer forever
   */
  public void close()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    	SibTr.entry(tc, "close");
    btmLockManager.lockExclusive();
    try
    {
      stopTimer();
      activeEntries = null;
    }
    finally
    {
      btmLockManager.unlockExclusive();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    	SibTr.exit(tc, "close");
  }

  /*
   * Go through the entries, telling them of the cancellation (if they're interested)
   */
  public void cancel()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "cancel");
    
    ArrayList<BatchedTimeoutEntry> localActiveList = null; 

    btmLockManager.lock();
    try
    {
      if (activeEntries != null)
      {
        // Take a local copy so we don't need to hold the lock when
        // we drive the processTimedoutEntries method
        LinkedListEntry entry = (LinkedListEntry) activeEntries.getFirst();
        while(entry != null)
        {
          if(entry.bte != null)
          {
            if(localActiveList == null)
              localActiveList = new ArrayList<BatchedTimeoutEntry>();
            
            localActiveList.add(entry.bte);
          }
          
          entry = (LinkedListEntry) entry.getNext();
        }
      }
    }
    finally
    {
      btmLockManager.unlock();
    }
    
    // Now call cancel on all entries
    if(localActiveList != null)
    {
      for (int i = 0; i < localActiveList.size(); i++)
        localActiveList.get(i).cancel();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "cancel");
  }
  
  /*
   * Go through the entries, telling them of the cancellation (if they're interested)
   */
  public void driveAllActiveEntries()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "driveAllActiveEntries");
    
    ArrayList<BatchedTimeoutEntry> localActiveList = null; 

    btmLockManager.lock();
    try
    {
      // Take a local copy so we don't need to hold the lock when
      // we drive the processTimedoutEntries method
      LinkedListEntry entry = (LinkedListEntry) activeEntries.getFirst();
      while(entry != null)
      {
        if(entry.bte != null)
        {
          if(localActiveList == null)
            localActiveList = new ArrayList<BatchedTimeoutEntry>();
          
          localActiveList.add(entry.bte);
        }
        
        entry = (LinkedListEntry) entry.getNext();
      }
    }
    finally
    {
      btmLockManager.unlock();
    }
    
    // Now call the 'alarm' method
    if(localActiveList != null)
      handler.processTimedoutEntries(localActiveList);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "driveAllActiveEntries");
  }
  
  /**
   * Add a timeout entry
   * @param value The BTE to start an alarm for
   */
  public void addTimeoutEntry(BatchedTimeoutEntry bte)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    	SibTr.entry(tc, "addTimeoutEntry", bte);
    
    //get a new entry for the active list
    btmLockManager.lockExclusive();
    try
    {
      LinkedListEntry entry = (LinkedListEntry) linkedListEntryPool.remove();
      if(entry == null)
      {
        entry = new LinkedListEntry();
      }
      entry.bte = bte;    
      bte.setEntry(entry);
      
      //put the new entry in the active list
      activeEntries.put(entry);
      
      //if we're not stopped
      if(!isStopped)
      {
        //start a new alarm
        startNewAlarm(entry);
      }
    }
    finally
    {
      btmLockManager.unlockExclusive();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    	SibTr.exit(tc, "addTimeoutEntry");
  }

  /**
   * Remove a timeout entry.
   * @param value The BTE to be removed from the active list
   */
  public void removeTimeoutEntry(BatchedTimeoutEntry bte)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    	SibTr.entry(tc, "removeTimeoutEntry", bte);
    
    btmLockManager.lockExclusive();
    try
    {
      //get the BTE's entry in the active list
      LinkedListEntry entry = bte.getEntry();
      //if it's not active, it won't have an entry    
      if(activeEntries.contains(entry))
      {      
        //remove it from the active list
        activeEntries.remove(entry);
        //clean up
        bte.setEntry(null);
        
        if(entry.alarm != null)
        {
          // Cancel the alarm (but not the whole group). This allows us to reuse the
          // entry (put it back in the pool) now that the alarm manager will not invoke
          // the alarms registered entry (i.e. this pooled object)
          ((MPAlarmImpl)entry.alarm).cancelSingleEntry();
          entry.alarm = null;
        }
        
        entry.bte = null;
        linkedListEntryPool.add(entry);                  
      }
    }
    finally
    {
      btmLockManager.unlockExclusive();
    }
        
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    	SibTr.exit(tc, "removeTimeoutEntry");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.utils.am.GroupAlarmListener#beginGroupAlarm(java.lang.Object)
   */
  public void beginGroupAlarm(Object firstContext)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "beginGroupAlarm", new Object[] { firstContext });

    LinkedListEntry entry = (LinkedListEntry)firstContext;

    btmLockManager.lock();
    timeoutLockManager.lockExclusive();
    try
    {
      if(!isStopped && activeEntries.contains(entry))
      {
        //start a new batch, clear the timedout list
        timedout.clear();
              
        //the alarm reference for this entry is not longer valid
        entry.alarm = null;
        //add the first BTE
        timedout.add(entry.bte);
      }
    }
    finally
    {
      timeoutLockManager.unlockExclusive();
      btmLockManager.unlock();  
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "beginGroupAlarm");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.utils.am.GroupAlarmListener#addContext(java.lang.Object)
   */
  public void addContext(Object nextContext)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addContext", new Object[] { nextContext });

    LinkedListEntry entry = (LinkedListEntry)nextContext;
    
    btmLockManager.lock();
    if(!isStopped && activeEntries.contains(entry))
    {
      //the alarm reference for this entry is not longer valid
      entry.alarm = null;
      timeoutLockManager.lockExclusive();
      //add each BTE to the timedout list
      timedout.add(entry.bte);
      timeoutLockManager.unlockExclusive();
    }
    btmLockManager.unlock();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addContext");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.utils.am.GroupAlarmListener#getGroupUuid()
   */
  public SIBUuid12 getGroupUuid()
  {
    return uuid;
  }
}
