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
package com.ibm.ws.sib.processor.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidFlowsException;
import com.ibm.ws.sib.processor.impl.interfaces.DispatchableConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.DispatchableKey;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.messagecontrol.ConsumerSet;
import com.ibm.wsspi.sib.messagecontrol.Flow;

/*
 * Locking hierarchy:
 * 
 *       ConsumerDispatcher.consumerPoints
 *          JSConsumerSet.consumerList
 *             JSLocalConsumerPoint.this
 *                JSConsumerSet.maxActiveMessagePrepareLock (Read/Write)
 *                   JSConsumerSet.maxActiveMessageLock
 *                JSLocalConsumerPoint._maxActiveMessageLock
 *          JSConsumerSet.this
 *          JSConsumerSet.classifications
 */
public class JSConsumerSet implements ConsumerSet
{
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  
  //trace
  private static final TraceComponent tc =
    SibTr.register(
        JSConsumerSet.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
    

  /** This COnsumerSet's label */
  private String label = null;
  
  /** The Classifications currently defined for this ConsumerSet */
  private JSConsumerClassifications classifications = null;

  /** HashMap used in the calculation of the class of message to use at dispatch time */
  HashMap<Integer, Integer> weightMap = null;

  /** The maximum number of active (locked or transactionally deleted)
    messages assigned to this ConsumerSet */
  private int maxActiveMessages = Integer.MAX_VALUE;
  /** The current number of active messages locked to members of the set */
  private int currentActiveMessages = 0; 
  /** The number of prepared, but not yet committed, active messages. These
   * are added when a consumer is about to attempt to lock a message and
   * committed once the consumer has the message locked (at which point they get
   * added to the currentActiveMessage value */
  private int preparedActiveMessages = 0; 
  /** A lock used when checking/updating the active message counters */
  private Object maxActiveMessageLock = 0;
  /** This lock is held while a consumer is preparing an add of an active message
   * and released at commit/rollback of the add.
   * Normally the lock is held as a read lock unless this add would make the
   * active message count hit the maximum, in which case a write lock is taken to
   * ensure any inflight prepares complete first and no new ones can occur until
   * the outcome of this add is know. This prevents the suspend/resume state of the
   * ConsumerSet changing while any of the members are halfway through an add. */
  private ReentrantReadWriteLock maxActiveMessagePrepareLock = null;
  private Lock maxActiveMessagePrepareReadLock = null;
  private Lock maxActiveMessagePrepareWriteLock = null;
  
  /** We set a resume threshold to avoid "dithering" where message processing could lead to 
   * the rapid suspension and resumption of consumers */
  private int resumeThreshold = Integer.MAX_VALUE;
  
  /** Registry of keys that need to be informed of flow changes */
  ArrayList<DispatchableKey> keyRegistry = null;
  
  /** Flag that keeps track of whether the entire ConsumerSet is suspended or not */
  private boolean consumerSetSuspended = false;  

  /** List of consumers that belong to this Set
   * NOTE: We lock on this list. This lock comes above any LocalConsumerPoint
   *       lock, which comes above the maxActiveMessage lock of this set.
   *       Basically, don't hold any consumer related locks at the point that you lock
   *       this list. */
  private ArrayList<DispatchableConsumerPoint> consumerList = null;  
  /** We roundrobin the startpoint in te list when we're resuming consumers to try
   * to give everyone a fair chance of restarting before they're suspended again */
  int startPoint = 0;

  /** ReadWrite locks to support access to classifications that may occasionally change */
  private ReadWriteLock classificationLock = null;
  private Lock classificationReadLock = null;
  private Lock classificationWriteLock = null;
  
  /** Flag that keeps track of whether this is a UT environment */
  private boolean unitTestOperation = false;
  
  public JSConsumerSet(String label)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, 
                  "JSConsumerSet", 
                  new Object[]{label});

    // Set the label
    this.label = label;
    // Set up the registry
    keyRegistry = new ArrayList<DispatchableKey>();
    // Set up the Consumer List
    consumerList = new ArrayList<DispatchableConsumerPoint>();      
    // Create an object to hold the Classifications defined for this Set
    classifications = new JSConsumerClassifications(label);
    
    // Initialise the classification locks
    classificationLock = new ReentrantReadWriteLock();
    classificationReadLock = classificationLock.readLock();
    classificationWriteLock = classificationLock.writeLock();

    // Initialise the various maxActiveMessage locks
    maxActiveMessageLock = new Object();
    maxActiveMessagePrepareLock = new ReentrantReadWriteLock();
    maxActiveMessagePrepareReadLock = maxActiveMessagePrepareLock.readLock();
    maxActiveMessagePrepareWriteLock = maxActiveMessagePrepareLock.writeLock();
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "maxActiveMessagePrepareLock: " + maxActiveMessagePrepareLock);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "JSConsumerSet", this);    
  }
  
  /**
   * Add a new consumer to this set.
   * 
   * @param lcp
   */
  public void addConsumer(DispatchableConsumerPoint lcp)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addConsumer", lcp);

    // WARNING: We mustn't hold the LCP lock of the consumer at this point
    synchronized(consumerList)
    {
      consumerList.add(lcp);
    }
    
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addConsumer");

  } 
  
  /**
   * Remove a consumer from the set.
   * @param lcp
   */
  public void removeConsumer(DispatchableConsumerPoint lcp)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeConsumer", lcp);

    // WARNING: We mustn't hold the LCP lock of the consumer at this point
    synchronized(consumerList)
    {
      consumerList.remove(lcp);
    }
    
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeConsumer");

  } 
  
  /**
   * Determine the index of the getCursor to use based on the classification of a
   * message.
   * 
   * @param msg
   * @return
   */
  public int getGetCursorIndex(SIMPMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getGetCursorIndex");
    
    // The zeroth index is reserved for non-classified messages
    int classPos = 0;
    synchronized(classifications)
    {
      if(classifications.getNumberOfClasses()>0)
      {
        // Need to get the classification out of the message
        String keyClassification = msg.getMessageControlClassification(true);        

        // In the special case where the weighting for the classification in the
        // message was zero, we use the Default cursor
        if(keyClassification != null && classifications.getWeight(keyClassification) > 0)
          // Get the position of the classification
          classPos = classifications.getPosition(keyClassification);
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getGetCursorIndex", classPos);
    
    return classPos;
  }
 
  /**
   * Determine the index of the getCursor to use based on the classifications defined
   * for the ConsumerSet that this consumer belongs to.
   * 
   * @param msg
   * @return
   */
  public synchronized int chooseGetCursorIndex(int previous)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "chooseGetCursorIndex", new Object[] {Integer.valueOf(previous)});
    
    // The zeroth index represents the default cursor for non-classified messages.
    int classPos = 0;
    if(classifications.getNumberOfClasses()>0)
    {
      // Need to determine the class of message to process
      if(previous == -1)
      {
        // First time through, get the initial weightings table
        weightMap = classifications.getWeightings();
      }
      else
      {
        // Need to remove previous entries from the weightMap
        weightMap.remove(Integer.valueOf(previous));
      }

      if(!weightMap.isEmpty())
      {
        classPos = classifications.findClassIndex(weightMap);
      } // eof non-empty weightmap
      else if(unitTestOperation)
      {
        // In a production environment we'd return zero in this case so that the
        // default cursor would be used. In a Unit test environment, where we are
        // classifying messages and where we have configured the test so that a
        // cursor associated with a specific classification should be used then
        // we'll alert the test to an error by throwing this exception.
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "chooseGetCursorIndex", "SIErrorException");        
        throw new SIErrorException();
      }
    }
   
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "chooseGetCursorIndex", classPos);
    
    return classPos;
  }      
 
  /**
   * Returns a reference to the Classifications object that wraps the
   * classifications specified by XD.
   * 
   * @return
   */
  public JSConsumerClassifications getClassifications()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getClassifications");
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getClassifications", classifications);
    
    // TODO No synchronization?
    return classifications;
  }  

  /**
   * Because there may be multiple members or the ConsumerSet, if we're managing 
   * the active mesage count we must 'reserve' a space for any message that a consumer
   * may lock, rather than lock it first then realise that the ConsumerSet has reached
   * its maximum active message limit and have to unlock it.
   * 
   * Preparing the add of a message will result in the maxActiveMessagePrepareLock being held
   * until the add succeeds (committed) or fails (rolled back).
   * Normally the lock is held as a read lock unless this add would make the
   * active message count hit the maximum, in which case a write lock is taken to
   * ensure any inflight prepares complete first and no new ones can occur until
   * the outcome of this add is know. This prevents the suspend/resume state of the
   * ConsumerSet changing while any of the members are halfway through an add.
   */
  public boolean prepareAddActiveMessage()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this,tc, "prepareAddActiveMessage");
    
    boolean messageAccepted = false;
    boolean limitReached = false;

    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "maxActiveMessagePrepareReadLock.lock(): " + maxActiveMessagePrepareLock);
    
    // Add us as a reader - this will block if a writer currently holds the lock.
    maxActiveMessagePrepareReadLock.lock(); 

    // Lock the actual message counters - to prevent other 'readers' concurrently making
    // changes.
    synchronized(maxActiveMessageLock)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Active Messages: current: " + currentActiveMessages +
                          ", prepared: " + preparedActiveMessages +
                          ", maximum: " + maxActiveMessages +
                          " (suspended: " + consumerSetSuspended + ")");
      
      // If we're already suspended we'll simply unlock the prepare and reject the message,
      // causing the consumer to suspend itself.
      
      // Otherwise, we need to check the current number of active messages...
      if(!consumerSetSuspended)
      {
        // If adding this message would cause us to reach the limit then we need to
        // drop out of this synchronize and re-take the prepare lock as a write lock.
        if((currentActiveMessages + preparedActiveMessages) == (maxActiveMessages - 1))
        {
          limitReached = true;
        }
        // Otherwise, we're safe to accept the message and add this message to the
        // prepare counter
        else
        {
          preparedActiveMessages++;
          messageAccepted = true;
          
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Prepare added");
        }
      }
      
      // If the message hasn't been accepted yet (because we're suspended or we
      // have to take the write lock) then release the read lock
      if(!messageAccepted)
      {
        maxActiveMessagePrepareReadLock.unlock();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "maxActiveMessagePrepareReadLock.unlock(): " + maxActiveMessagePrepareLock);
      }
    } // synchronize
    
    // If this message would cause us to reach the limit then we must take the write
    // lock to prevent others preparing messages while we're in the middle of this
    // threashold-case. We also need to block until any existing prepares complete to
    // ensure that we really are on the brink of suspending the set
    if(limitReached)
    {
      boolean releaseWriteLock = true;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "maxActiveMessagePrepareWriteLock.lock(): " + maxActiveMessagePrepareLock);
      
      // Take the write lock and hold it until the commit/rollback
      maxActiveMessagePrepareWriteLock.lock();
      
      // Re-take the active message counter lock
      synchronized(maxActiveMessageLock)
      {
        // We could have been suspended between us releasing the locks above
        // and re-taking them here, if that's the case we simply release the
        // write lock and reject the message add.
        if(!consumerSetSuspended)
        {
          // We're not suspended yet so accept this message and add the prepared
          // message
          messageAccepted = true;
          preparedActiveMessages++;

          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Prepare added");
          
          // If we're still in the position where committing this add would make
          // us hit the limit then we need to hold onto the write lock until the
          // commit/rollback.
          if((currentActiveMessages + preparedActiveMessages) == maxActiveMessages)
          {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              SibTr.debug(tc, "Write lock held until commit/rollback");
            
            releaseWriteLock = false;
          }
          // Otherwise, someone must have got in while we'd released the locks and
          // removed one or more of the active messages. In which case this add won't
          // make us hit the limit so we downgrade the prepare lock to a read and release
          // the write lock once we've done it.
          else
          {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
              SibTr.debug(tc, "maxActiveMessagePrepareReadLock.lock(): " + maxActiveMessagePrepareLock);
            maxActiveMessagePrepareReadLock.lock();
          }
        }
        
        // If we were told to release the write lock do it now.
        if(releaseWriteLock)
        {
          maxActiveMessagePrepareWriteLock.unlock();
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "maxActiveMessagePrepareWriteLock.unlock(): " + maxActiveMessagePrepareLock);
        }
      }
    }

    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "prepareAddActiveMessage", Boolean.valueOf(messageAccepted));
      
    return messageAccepted;
  }  

  /**
   * Commit the adding of the active message (remember, this is nothing to
   * do with the committing of the delete of the message).
   * 
   * We will have held the maxActiveMessagePrepareLock since the prepare
   * of the add until this method (or a rollback)
   *
   */
  public void commitAddActiveMessage()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "commitAddActiveMessage");

    // Lock the active message counter lock
    synchronized(maxActiveMessageLock)
    {
      // Move the prepared add to the real count
      currentActiveMessages++;
      preparedActiveMessages--;
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Active Messages: current: " + currentActiveMessages +
                          ", prepared: " + preparedActiveMessages +
                          ", maximum: " + maxActiveMessages +
                          " (suspended: " + consumerSetSuspended + ")");
      
      // We should never go negative, if we do something has gone wrong - FFDC
      if(preparedActiveMessages < 0)
      {
        SIErrorException e = new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0001",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.JSConsumerSet.commitAddActiveMessage",
                "1:499:1.16",
                Integer.valueOf(preparedActiveMessages),
                Integer.valueOf(currentActiveMessages),
                Integer.valueOf(maxActiveMessages),
                Boolean.valueOf(consumerSetSuspended)
                },
              null));

          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.JSConsumerSet.commitAddActiveMessage",
            "1:510:1.16",
            this);

          SibTr.exception(tc, e);
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.JSConsumerSet.commitAddActiveMessage",
              "1:517:1.16" });
      }
      
      // If we currently hold the write lock (as we will always either hold
      // the read lock of the write lock we know this check will return the right
      // answer (i.e. it can't change under us)) we must have thought this add
      // would take us up to the limit and therefore require a suspend.
      if(maxActiveMessagePrepareLock.isWriteLockedByCurrentThread())
      {
        // If the limit has been reached, suspend the set
        if(currentActiveMessages == maxActiveMessages)
        {
          // All this does is mark the set as suspended. We leave it up to the members
          // of the set to notice that the set is suspended and suspend themselves.
          // This is more efficient than going round and suspending everyone, only to
          // have to resume then all again in a second. Hopefully the set will be
          // resumed before most of them notice it was suspended.
          consumerSetSuspended = true;

          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "JSConsumerSet suspended " + this);
        }
        
        // Release the write lock
        maxActiveMessagePrepareWriteLock.unlock();
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "maxActiveMessagePrepareWriteLock.unlock(): " + maxActiveMessagePrepareLock);
      }
      else
      {
        // We only held a read lock, release it now
        maxActiveMessagePrepareReadLock.unlock();
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "maxActiveMessagePrepareReadLock.unlock(): " + maxActiveMessagePrepareLock);
      }
    }

    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "commitAddActiveMessage");
  }  
  
  /**
   * Roll back the adding of the active message (remember, this is nothing to
   * do with the rolling back of the delete of the message).
   * 
   * We will have held the maxActiveMessagePrepareLock since the prepare
   * of the add until this method (or a commit)
   *
   */
  public void rollbackAddActiveMessage()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "rollbackAddActiveMessage");

    // Lock the active message counter lock
    synchronized(maxActiveMessageLock)
    {
      // Rolling back the prepare simply decrements the prepared message count
      preparedActiveMessages--;

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "Active Messages: current: " + currentActiveMessages +
                          ", prepared: " + preparedActiveMessages +
                          ", maximum: " + maxActiveMessages +
                          " (suspended: " + consumerSetSuspended + ")");
      
      // If we currently hold the write lock (as we will always either hold
      // the read lock of the write lock we know this check will return the right
      // answer (i.e. it can't change under us)) we must have thought this add
      // would take us up to the limit and therefore require a suspend, as this
      // is a rollback the suspend is no longer required
      if(maxActiveMessagePrepareLock.isWriteLockedByCurrentThread())
      {
        // Release the write lock
        maxActiveMessagePrepareWriteLock.unlock();
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "maxActiveMessagePrepareWriteLock.unlock(): " + maxActiveMessagePrepareLock);
      }
      else
      {
        // We only held a read lock, release it now
        maxActiveMessagePrepareReadLock.unlock();
      
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "maxActiveMessagePrepareReadLock.unlock(): " + maxActiveMessagePrepareLock);
      }
      
      // We should never go negative, if we do something has gone wrong - FFDC
      if(preparedActiveMessages < 0)
      {
        SIErrorException e = new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0001",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.JSConsumerSet.rollbackAddActiveMessage",
                "1:615:1.16",
                Integer.valueOf(preparedActiveMessages),
                Integer.valueOf(currentActiveMessages),
                Integer.valueOf(maxActiveMessages),
                Boolean.valueOf(consumerSetSuspended)
                },
              null));

          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.JSConsumerSet.rollbackAddActiveMessage",
            "1:626:1.16",
            this);

          SibTr.exception(tc, e);
          SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.JSConsumerSet.rollbackAddActiveMessage",
              "1:633:1.16" });
      }
    }

    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "rollbackAddActiveMessage");
  }  
  
  
  /**
   * Decrement the active message count
   * 
   * @param messageCount number of messages to remove from the count
   */
  public void removeActiveMessages(int messageCount)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "removeActiveMessages", Integer.valueOf(messageCount));

    boolean resumeConsumers = false;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "maxActiveMessagePrepareReadLock.lock(): " + maxActiveMessagePrepareLock);
    
    // Take a read lock on the prepare to ensure the set isn't in the process
    // of suspending itself.
    maxActiveMessagePrepareReadLock.lock();
    
    try
    {
      // Lock down the active message count
      synchronized(maxActiveMessageLock)
      {
        // Decrement the count
        currentActiveMessages -= messageCount;

        // We should never go negative, if we do something has gone wrong - FFDC
        if(currentActiveMessages < 0)
        {
          SIErrorException e = new SIErrorException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                new Object[] {
                  "com.ibm.ws.sib.processor.impl.JSConsumerSet.removeActiveMessages",
                  "1:677:1.16",
                  Integer.valueOf(preparedActiveMessages),
                  Integer.valueOf(currentActiveMessages),
                  Integer.valueOf(maxActiveMessages),
                  Boolean.valueOf(consumerSetSuspended)
                  },
                null));

            FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.impl.JSConsumerSet.removeActiveMessages",
              "1:688:1.16",
              this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.JSConsumerSet.removeActiveMessages",
                "1:695:1.16" });
        }
        
        // If the set is currently suspended then we'll check to see if
        // we've fallen below the threashold
        if(consumerSetSuspended)
        {
          // The threashold that we resume a set is potentially a few messages
          // below the max, so we don't constantly switch from suspended to resumed
          if(currentActiveMessages == (resumeThreshold - messageCount))
          {
            // Reset the consumerSet flag to make any inquiring consumers aware
            // of the current state.
            consumerSetSuspended = false;
            
            // Now we need to resume any currently suspended consumers. We can't
            // do this while we hold the maxActiveMessageLock as the resume will
            // need to take the LCP lock for each consumer. Instead we release
            // this lock and perform the resume under the consumerList lock to
            // prevent the list changing. We're also reliant on each consumer being
            // resumed re-checking with us that we're still resumed (as we no
            // longer hold the maxActiveMessageLock at the time of the resume - so
            // some other consumer could have got in and caused us to suspend again)
            resumeConsumers = true;
          }
        }
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "Active Messages: current: " + currentActiveMessages +
                            ", prepared: " + preparedActiveMessages +
                            ", maximum: " + maxActiveMessages +
                            " (suspended: " + consumerSetSuspended + ")");
      } // synchronized
    }
    finally
    {
      // Release the read lock
      maxActiveMessagePrepareReadLock.unlock();

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "maxActiveMessagePrepareReadLock.unlock(): " + maxActiveMessagePrepareLock);
    }
    
    // Now we've release the lock, resume the consumers if needed
    if(resumeConsumers)
      resumeConsumers();
    
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "removeActiveMessages");
  }

  /**
   * Resume all the members of the set. This will only cause a real
   * resume on consumers that are actually in the suspend state for
   * reaching the set's max active mesasge limit. If they're not
   * suspended (because they hadn't noticed the set was suspended)
   * or they're suspended for a different reason (e.g local maxActiveMessage
   * limit reached) then they'll remain suspended.
   */
  private void resumeConsumers()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "resumeConsumers");

    synchronized(consumerList)
    {
      int listSize = consumerList.size();
      // Roundrobin the start point in the list as it's quite possible that
      // the first consumer that is resumed will cause the set to be suspended
      // again. If we always started at the top of the list it would always be
      // the same consumer that got to process any messages.
      startPoint = ++startPoint % listSize;
      for(int i = 0; i < listSize; i++)
      {
        JSLocalConsumerPoint lcp = (JSLocalConsumerPoint)consumerList.get((startPoint + i) % listSize);
        lcp.resumeConsumer(DispatchableConsumerPoint.SUSPEND_FLAG_SET_ACTIVE_MSGS);
      }
    }
    
    // The calling consumer's thread has just released a message, rather than let it go
    // straight back round and take another message (possibly suspending the set again
    // before any resumed thread gets a chance to run) we yield this thread - just to
    // be fair. We're nice like that.
    Thread.yield();
    
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "resumeConsumers");
  }
  
  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.extension.ConsumerSet#setConcurrencyLimit(int)
   */
  public void setConcurrencyLimit(int maxConcurrency)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "setConcurrencyLimit", Integer.valueOf(maxConcurrency));

    boolean resumeConsumers = false;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "maxActiveMessagePrepareWriteLock.lock(): " + maxActiveMessagePrepareLock);
    
    // Fully lock down the active message counting
    maxActiveMessagePrepareWriteLock.lock();
    
    try
    {
      synchronized(maxActiveMessageLock)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "Inital active Messages: current: " + currentActiveMessages +
                            ", prepared: " + preparedActiveMessages +
                            ", maximum: " + maxActiveMessages +
                            " (suspended: " + consumerSetSuspended + ")");
        
        // A value of zero indicates no maximum
        if(maxConcurrency == 0)
        {
          maxActiveMessages = Integer.MAX_VALUE;
          resumeThreshold = Integer.MAX_VALUE;
        }
        else
        {
          maxActiveMessages = maxConcurrency;
          
          // Set the resumeThreshold to 80% of this value.
          float res = (maxActiveMessages * 8);
          res = Math.round(res / 10);
          this.resumeThreshold = (int) res;
        }
        
        // Check that changing the limit doesn't change our current
        // suspend state
        if(currentActiveMessages > maxActiveMessages)
          consumerSetSuspended = true;
        else if(consumerSetSuspended && (currentActiveMessages < resumeThreshold))
        {
          consumerSetSuspended = false;
          resumeConsumers = true;
        }
      } // synchronized
    }
    finally
    {
      maxActiveMessagePrepareWriteLock.unlock();

      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "maxActiveMessagePrepareWriteLock.unlock(): " + maxActiveMessagePrepareLock);
    }
    
    // We may have caused the set to be resumed - wakeup any suspended
    // members of the set.
    if(resumeConsumers)
      resumeConsumers();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "setConcurrencyLimit");    
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.extension.ConsumerSet#setFlowProperties(com.ibm.wsspi.sib.extension.Flow[])
   */
  public void setFlowProperties(Flow[] flows)
    throws SIIncorrectCallException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setFlowProperties", flows);

    try
    {
      // Take the Write Lock for classifications
      classificationWriteLock.lock();

      // Disallow null flows
      if(flows == null)
      {
        SIIncorrectCallException e =
          new SIIncorrectCallException(
            nls.getFormattedMessage(
                "NULL_FLOW_CLASSIFICATIONS_CWSIP0851",
                new Object[] {
                  "com.ibm.ws.sib.processor.impl.JSConsumerSet.setFlowProperties",
                  "1:877:1.16",
                  label },
                null));
        
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.JSConsumerSet.setFlowProperties",
          "1:885:1.16",
          this);
            
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
          SibTr.exit(tc, "setFlowProperties", "SIIncorrectCallException");
        throw e;
      }
    
      // Set up the new classifications for the ConsumerSet
      boolean weightChangeOnly = classifications.setClassifications(flows);
    
      // Need to notify registered keys of the change but only if the flow change is
      // substantive - i.e. more than just a change in the relative weightings of the 
      // current flows.
      if(!weightChangeOnly)
      {
        Iterator<DispatchableKey> it = keyRegistry.iterator();
        while (it.hasNext())
        {
          DispatchableKey key = (DispatchableKey)it.next();
          // Notify the appropriate key
          if(key instanceof LocalQPConsumerKey)
          {
            ((LocalQPConsumerKey)(key)).notifyResetFlowProperties();
          }
          else if(key instanceof LocalQPConsumerKeyGroup)
          {
            ((LocalQPConsumerKeyGroup)(key)).notifyResetFlowProperties();
          }
        }
      }
    }
    catch(InvalidFlowsException ife)
    {
      // FFDC
      FFDCFilter.processException(
        ife,
        "com.ibm.ws.sib.processor.impl.JSConsumerSet.setFlowProperties",
        "1:923:1.16",
        this);
      
      // Map the InvalidFlowsException to an SIIncorrectCallException
      SIIncorrectCallException e =
        new SIIncorrectCallException(
          nls.getFormattedMessage(
              "INVALID_FLOW_CLASSIFICATIONS_CWSIP0852",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.JSConsumerSet.setFlowProperties",
                "1:933:1.16",
                label },
              null));
          
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "setFlowProperties", "SIIncorrectCallException");
      throw e;
    }
    finally
    {
      // Free the write lock
      classificationWriteLock.unlock();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setFlowProperties");        
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.extension.ConsumerSet#getFlows()
   */
  public Flow[] getFlows()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getFlows");
    }
    Flow[] flows = classifications.getFlows();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.exit(tc, "getFlows", flows);
    }    
    return flows;
  }  
  
  /**
   * Returns the name of a classification specified by XD.
   * 
   * @return
   */
  public synchronized void registerKey(DispatchableKey key)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "registerKey", key);
    keyRegistry.add(key);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "registerKey");
  }   

  /**
   * Is this consumer set suspended because it has breached its concurrency limit?
   * 
   * @return
   */
  public boolean isConsumerSetSuspended()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "isConsumerSetSuspended");
    
    boolean suspended = false;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "maxActiveMessagePrepareReadLock.lock(): " + maxActiveMessagePrepareLock);
    
    // Take the prepare read lock to prevent the state changing under us
    maxActiveMessagePrepareReadLock.lock();
    
    // Lock down the suspend state
    synchronized(maxActiveMessageLock)
    {
      suspended = consumerSetSuspended;
    }
    
    // Release the prepare lock
    maxActiveMessagePrepareReadLock.unlock();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "maxActiveMessagePrepareReadLock.unlock(): " + maxActiveMessagePrepareLock);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "isConsumerSetSuspended", Boolean.valueOf(suspended));  
    
    return suspended;
  }

  /**
   * Take a classification readlock
   * 
   */
  public void takeClassificationReadLock()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "takeClassificationReadLock");
    
    classificationReadLock.lock();
    
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "takeClassificationReadLock");
  }  
 
  /**
   * Free a classification readlock
   * 
    * TODO Put all calls to this in a finally
  */
  public void freeClassificationReadLock()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "freeClassificationReadLock");
    
    classificationReadLock.unlock();
    
    if(TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "freeClassificationReadLock");
  }

  /* (non-Javadoc)
   * @see com.ibm.wsspi.sib.messagecontrol.ConsumerSet#getLabel()
   */
  public String getLabel() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getLabel");
      SibTr.exit(tc, "getLabel", label);
    }    
    return label;
  } 
  
  /**
   * Set the flag that signifies that this is a UT environment. 
   */
  public synchronized void setUnitTestOperation()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "setUnitTestOperation");
      SibTr.exit(tc, "setUnitTestOperation");
    }

    unitTestOperation = true; 
  }
  
  public String toString()
  {
	String jsConsumerSetStr = "JSConsumerSet: " +  label;
    return jsConsumerSetStr;	  
  }
  
}
