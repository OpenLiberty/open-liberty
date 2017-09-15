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
import java.util.concurrent.locks.ReentrantLock;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.matchspace.Selector;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.LockingCursor;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DispatchableConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.DispatchableKey;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumableKey;
import com.ibm.ws.sib.processor.impl.interfaces.JSConsumerKey;
import com.ibm.ws.sib.processor.impl.interfaces.JSConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.JSKeyGroup;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.linkedlist.SimpleEntry;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 */
public class LocalQPConsumerKeyGroup extends SimpleEntry implements JSKeyGroup, Filter, DispatchableKey
{
  private static final TraceComponent tc =
    SibTr.register(
      LocalQPConsumerKeyGroup.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  // ReentrantLock to be used by us and JSLocalConsumerPoints to manage
  // exclusive access to the group across the multiple members
  private ReentrantLock _lock;
  
  private boolean ready;
  private boolean specificReady;
  private boolean groupReady;
  private long version;

  private int memberCount;
  private int startedCount;
  private int generalMemberCount;

  private LocalQPConsumerKey singleMember;

  // We use ArrayLists here as we don't expect a great rate of adding and removing and we
  // want scanning of the list to be as fast as possible
  private ArrayList<LocalQPConsumerKey> generalKeyMembers;
  private ArrayList<LocalQPConsumerKey> specificKeyMembers;
  private int generalMemberIndex;

  private JSConsumerManager consumerDispatcher;
  private SIBUuid12 connectionUuid;

  private boolean currentMatch;
  private LocalQPConsumerKey currentMatchingMember;

  private ConsumableKey msgAttachedMember;

  // This lock is held by any member of the group for the duration of a consumerMessages
  // call (potentially a long time). It is equivalent to the asynchConsumer lock in LCP.
  // The keyGroup object itself should be locked while changing ready state
  protected Object asynchGroupLock;

  private boolean consumerThreadActive = false;
  private long consumerThreadID = 0;
  
  /** Flag to indicate whether Message Classification is enabled */
  private boolean classifyingMessages = false;
  
  /** Class that wrappers a ConsumerSet. This will be null unless XD has registered for
   *  message classification */
  private JSConsumerSet consumerSet = null;
  
  /** An array of filters. Unless XD has registered for message classification
   * the array will have just one member.
   */
  private LocalQPConsumerKeyFilter[] consumerKeyFilter = null;
  
  /** Flag whether classifications have been reset. If they have, then the filters
   * need to be reset also.
   */
  private boolean pendingFlowReset = false;
  
  public LocalQPConsumerKeyGroup(ConsumerDispatcher consumerDispatcher,
                                 JSConsumerSet consumerSet)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "LocalQPConsumerKeyGroup", new Object[] {consumerDispatcher, consumerSet});

    // Create a reentrant lock to use for the cross-group-member lock
    _lock = new ReentrantLock();
    
    if(consumerDispatcher.isPubSub())
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "LocalQPConsumerKeyGroup", "SIErrorException");
      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.LocalQPConsumerKeyGroup",
            "1:156:1.6.1.14" },
          null));
    }
     
    // Set the ConsumerSet into the group
    this.consumerSet = consumerSet;
    
    // Register with the consumerSet if non-null
    if(consumerSet != null)
    {
      classifyingMessages = true;
      consumerSet.registerKey(this);
    }
    
    // Initialise the keyGroup
    ready = false;
    groupReady = false;
    version = 0;
    generalKeyMembers = null;
    specificKeyMembers = null;
    this.consumerDispatcher = consumerDispatcher;
    this.connectionUuid = null;
    memberCount = 0;
    startedCount = 0;
    generalMemberCount = 0;
    asynchGroupLock = new Object();
    singleMember = null;
    msgAttachedMember = null;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "LocalQPConsumerKeyGroup", this);
  }

  /**
   * Add a member to the group
   * @param key
   * @throws SIStoreException
   */
  public void addMember(JSConsumerKey key) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addMember", key);
    
    lock();
    
    try
    {
      
      // If this is the first member remember the connectionUuid, all members
      // must be from the same connection
      if(connectionUuid == null)
        connectionUuid = key.getConnectionUuid();
      else if(!connectionUuid.equals(key.getConnectionUuid()))
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "addMember", "SIErrorException");
        throw new SIErrorException(nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.LocalQPConsumerKeyGroup",
              "1:216:1.6.1.14" },
            null));
      }

      // SIB0113b Multiple consumermanager types can be part of a group
//      // Double check that this keyGroup is for the correct consumerDispatcher
//      if(consumerDispatcher != key.getConsumerManager())
//      {
//        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "addMember", "SIErrorException");
//        throw new SIErrorException(nls.getFormattedMessage(
//          "INTERNAL_MESSAGING_ERROR_CWSIP0001",
//          new Object[] {
//            "com.ibm.ws.sib.processor.impl.ConsumerKeyGroup",
//            "1:229:1.6.1.14" },
//          null));
//      }

      // Forward scanning and ordering groups are not supported
      if(key.getForwardScanning())
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "addMember", "SIErrorException");
          throw new SIErrorException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0001",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.LocalQPConsumerKeyGroup",
                "1:242:1.6.1.14" },
              null));
      }

      try
      {
        // By adding a new member to the key any existing cursor on the ItemStream
        // may be in the wrong place. Other than performance there is no disadvantage to
        // starting from the top

        // Discard any old cursor
        if(consumerKeyFilter != null)
        {
          if(classifyingMessages)
          {
            // Take the classifications read lock
            consumerSet.takeClassificationReadLock();
   
            int numFilters = consumerKeyFilter.length;    
            for(int i=0;i<numFilters;i++)
              consumerKeyFilter[i].discard();
   
            //Create a new one (we can use the CD itemStream because we know this cannot
            // be a subscription (not supported)
            createNewFiltersAndCursors();
            
            // Free the classifications read lock
            consumerSet.freeClassificationReadLock();
          }
          else
          {
            consumerKeyFilter[0].discard();
            //Create a new one (we can use the CD itemStream because we know this cannot
            // be a subscription (not supported)
            createNewFiltersAndCursors();
          }
        }
        else // Need to set up new filters
        {
          if(classifyingMessages)
          {
            // Take the classifications read lock
            consumerSet.takeClassificationReadLock();
   
            //Create a new one (we can use the CD itemStream because we know this cannot
            // be a subscription (not supported)
            createNewFiltersAndCursors();
            
            // Free the classifications read lock
            consumerSet.freeClassificationReadLock();
          }
          else
          {
            //Create a new one (we can use the CD itemStream because we know this cannot
            // be a subscription (not supported)
            createNewFiltersAndCursors();
          }        
        }
      }
      catch (MessageStoreException e)
      {
        // MessageStoreException shouldn't occur so FFDC.
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.LocalQPConsumerKeyGroup.addMember",
          "1:307:1.6.1.14",
          this);

        SibTr.exception(tc, e);

        SIResourceException newE = new SIResourceException(
                                      nls.getFormattedMessage(
                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                        new Object[] {
                                          "com.ibm.ws.sib.processor.impl.LocalQPConsumerKeyGroup",
                                          "1:317:1.6.1.14",
                                          e },
                                        null),
                                      e);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "addMember", newE);

        throw newE;

      }

      synchronized(consumerDispatcher.getDestination().getReadyConsumerPointLock())
      {
        // By adding another member they cannot all be ready (the new one cannot be)
        // therefore we must remove the group from the CD's ready list
        if(ready)
        {
          //set the state to not ready
          ready = false;

          consumerDispatcher.removeReadyConsumer(this, specificReady);
        }
      }

      // If real life we expect there only to be a single member of the group
      // (JMS will always specify OrderingContext). Therefore we optimise the
      // case for one member - we avoid any lists
      if(memberCount == 0)
      {
        singleMember = (LocalQPConsumerKey)key;

        if(!(key.isSpecific()))
          generalMemberCount++;
      }
      else
      {
        if(singleMember != null)
        {
          if(generalMemberCount == 1)
            addMemberToList(singleMember, false);
          else
            addMemberToList(singleMember, true);

          singleMember = null;
        }

        if(key.isSpecific())
        {
          addMemberToList(key, true);
        }
        else
        {
          generalMemberCount++;
          addMemberToList(key, false);
        }
      }

      memberCount++;
    }
    finally
    {
      unlock();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addMember", Long.valueOf(memberCount));
  }

  /**
   * Add the member to the correct list
   * @param key
   * @param specificList
   */
  private void addMemberToList(JSConsumerKey key, boolean specificList)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry( tc, "addMemberToList", new Object[] {key, Boolean.valueOf(specificList)});

    if(specificList)
    {
      if(specificKeyMembers == null)
      {
        // Our first specific member, create a list for them
        specificKeyMembers = new ArrayList<LocalQPConsumerKey>();
      }
      specificKeyMembers.add((LocalQPConsumerKey)key);
    }
    else
    {
      if(generalKeyMembers == null)
      {
        // Our first specific member, create a list for them
        generalKeyMembers = new ArrayList<LocalQPConsumerKey>();
      }
      generalKeyMembers.add((LocalQPConsumerKey)key);

      // As we've modified the list we need to reset the index
      generalMemberIndex = 0;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addMemberToList");
  }

  /**
   * Remove a member from the group
   * @param key
   */
  public void removeMember(JSConsumerKey key)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeMember", key);

    LocalQPConsumerKey anyKey = null;

    // We lock the CD so other members are not added/removed while
    // we do this
    synchronized(consumerDispatcher.getDestination().getReadyConsumerPointLock())
    {
      if(singleMember != null)
      {
        if(!(key.isSpecific()))
          generalMemberCount--;

        if(singleMember == key)
          singleMember = null;
        else
        {
          // We must be the only member
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "removeMember", "SIErrorException");
            throw new SIErrorException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                new Object[] {
                  "com.ibm.ws.sib.processor.impl.LocalQPConsumerKeyGroup",
                  "1:453:1.6.1.14" },
                null));
        }
      }
      else
      {
        if(key.isSpecific())
          specificKeyMembers.remove(key);
        else
        {
          generalKeyMembers.remove(key);
          generalMemberCount--;
          // As we've modified the list we need a new iterator
          generalMemberIndex = 0;
        }
      }

      memberCount--;

      // If that was the last member, we can remove this group
      if(memberCount == 0)
      {
        consumerDispatcher.removeKeyGroup(this);
        // Drive finished against the set of cursors
        
        if(classifyingMessages)
        {
          // Take the classifications read lock
          consumerSet.takeClassificationReadLock();
          
          int numFilters = consumerKeyFilter.length;    
          for(int i=0;i<numFilters;i++)
            consumerKeyFilter[i].detach(); 

          // Free the classifications read lock
          consumerSet.freeClassificationReadLock();          
        }
        else
        {
          consumerKeyFilter[0].detach();  
        }
      }
      // If all the remaining members are started we can start receiving
      // messages again (we couldn't have been before because the member
      // was in stopped state.
      else if(memberCount == startedCount)
      {
        if(generalMemberCount > 0)
          anyKey = generalKeyMembers.get(0);
        else
          anyKey = specificKeyMembers.get(0);
      }
    } // synchronized

    //  Start the member up
    if(anyKey!=null)
      anyKey.getConsumerPoint().checkForMessages();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeMember");
  }

  public void startMember()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "startMember");
      SibTr.exit(tc, "startMember", new Object[] {Integer.valueOf(startedCount), Integer.valueOf(memberCount)});
    }

    startedCount++;
  }

  public void stopMember()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "stopMember");
      SibTr.exit(tc, "stopMember", new Object[] {Integer.valueOf(startedCount), Integer.valueOf(memberCount)});
    }

    startedCount--;
  }

  public boolean isStarted()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isStarted", new Object[] {Integer.valueOf(startedCount), Integer.valueOf(memberCount)});

    // A group is started if all its members are started
    if(startedCount == memberCount)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "isStarted", Boolean.valueOf(true));
      return true;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isStarted", Boolean.valueOf(false));
    return false;
  }

  /**
   * Set the state of a ConsumerKeyGroup to ready and move it in to the ready list.
   * This causes the ready consumer list version, the specific ready consumer counter and the
   * specific ready consumer list version all to be incremented (if appropriate).
   *
   * This method only has any effect if the state was originally not ready.
   */
  public void ready(Reliability unrecoverableReliability) throws SINotPossibleInCurrentConfigurationException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "ready");

    //get the readyConsumer list lock
    synchronized (consumerDispatcher.getDestination().getReadyConsumerPointLock())
    {
      // See if we can go from not ready to ready..
      if(!ready)
      {
        // Everyone must be started
        if(startedCount == memberCount)
        {
          //set the state to ready
          ready = true;

          // Remember how we've registered with the CD
          if(generalMemberCount == 0)
            specificReady = true;
          else
            specificReady = false;

          version = consumerDispatcher.newReadyConsumer(this, specificReady);
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "ready");
  }

  /**
   * Set the state of a ConsumerKeyGroup to not ready and remove it from the ready list.
   * or decrement the specific ready counter (if appropriate). Note that there is no need to
   * increment the specific consumer list version number.
   *
   * This method only has any effect if the state was originally ready.
   */
  public void notReady()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "notReady");

    //get the ready consumer list lock
    synchronized (consumerDispatcher.getDestination().getReadyConsumerPointLock())
    {
      if(ready)
      {
        ready = false;

        consumerDispatcher.removeReadyConsumer(this, specificReady);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "notReady");
  }

  public void markNotReady()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "markNotReady");
      SibTr.exit(tc, "markNotReady");
    }

    ready = false;
  }

  public boolean isKeyReady()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isKeyReady");
      SibTr.exit(tc, "isKeyReady", Boolean.valueOf(ready));
    }

    return ready;
  }

  public long getVersion()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getVersion");
      SibTr.exit(tc, "getVersion", Long.valueOf(version));
    }

    return version;
  }

  /**
   * Return one of the groups non-specific members
   */
  public LocalQPConsumerKey resolvedKey()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "resolvedKey");

    LocalQPConsumerKey key = null;

    if(generalMemberCount > 0)
    {
      // If we only have one this is easy
      if(singleMember != null)
        key = singleMember;
      // Otherwise, we try to be a little fair and pick the next one in
      // the list
      else
      {
        key = generalKeyMembers.get(generalMemberIndex);
        // Wrap the index if required
        if(++generalMemberIndex == generalMemberCount)
          generalMemberIndex = 0;
      }
    }
    else
    {
      // We should have at least one of these
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "resolvedKey", "SIErrorException");
        throw new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.LocalQPConsumerKeyGroup",
              "1:688:1.6.1.14" },
            null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "resolvedKey", key);

    return key;
  }

  /**
   * Mark the group as groupReady, this is seperate to the ready state as that
   * indicates to the CD if the group is ready, this indicates to the group if
   * it is ready (i.e. willing to take on a message).
   */
  public void groupReady()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "groupReady", Boolean.valueOf(groupReady));

    if(startedCount == memberCount)
      groupReady = true;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "groupReady", Boolean.valueOf(groupReady));
  }

  public void groupNotReady()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "groupNotReady", Boolean.valueOf(groupReady));
      SibTr.exit(tc, "groupNotReady");
    }

    groupReady = false;
  }

  public boolean isGroupReady()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isGroupReady");
      SibTr.exit(tc, "isGroupReady", Boolean.valueOf(groupReady));
    }

    return groupReady;
  }

//  public LockingCursor getCursor()
//  {
//    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
//    {
//      SibTr.entry(tc, "getCursor");
//      SibTr.exit(tc, "getCursor", getCursor);
//    }
//    return getCursor;
//  }
  /**
   * Return the getCursor for this consumer. This method is only called in the
   * case where messages are not classified by XD.
   */
  public LockingCursor getDefaultGetCursor()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDefaultGetCursor");
    
    LockingCursor cursor = consumerKeyFilter[0].getGetCursor();
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getDefaultGetCursor", cursor);

    return cursor;
  }

  /**
   * Retrieves the GetCursor that is associated with the filter at the
   * specified classification index.
   * 
   * This method should be called under the classification readlock as it
   * references resources that are dependent on the current set of
   * classifications.
   *  
   * @param classification
   * @return
   */
  public LockingCursor getGetCursor(int classification)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getGetCursor", Integer.valueOf(classification));
    
    // Classifications may change dynamically, so check the pendingReset
    // flag under this lock. If classifications have changed then we'll
    // set up the filters again
    if(pendingFlowReset)
    { 
      pendingFlowReset = false;
      // Reset the filters under the lock
      resetFlowProperties();
    }
    
    LockingCursor cursor = consumerKeyFilter[classification].getGetCursor();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getGetCursor", cursor);

    return cursor;
  }  
  
  public LockingCursor getGetCursor(SIMPMessage msg) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getGetCursor");
    
    // Take the classifications read lock
    consumerSet.takeClassificationReadLock(); 

    // Classifications may change dynamically, so check the pendingReset
    // flag under this lock. If classifications have changed then we'll
    // set up the filters again
    if(pendingFlowReset)
    { 
      pendingFlowReset = false;
      // Reset the filters under the lock
      resetFlowProperties();
    }
    
    LockingCursor cursor = consumerKeyFilter[0].getGetCursor();
    
    // Take the classifications read lock
    consumerSet.freeClassificationReadLock();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getGetCursor", cursor);

    return cursor;
  }   
  
  public SIBUuid12 getConnectionUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getConnectionUuid");
      SibTr.exit(tc, "getConnectionUuid", connectionUuid);
    }

    return connectionUuid;
  }

  public boolean getForwardScanning()
  {
    return false;
  }

  /**
   * We only want to remember the result of a filter match if it is called as a result of a consumer
   * asking for a message. The consumer indicates that it is asking for a message by calling this
   * method with active set to true. After the consumer has got it's message it should call again
   * with active false.
   *
   * This is because the filterMatches method can get called at any time by threads other than the
   * consumers' thread. Previously, if this happened while the consumer was trying to get a message,
   * the match results were lost or changed.
   *
   * @param active
   */
  public void setConsumerActive(boolean active)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setConsumerActive", active);
    if(active)
    {
      consumerThreadID = Thread.currentThread().getId();
    }
    else
    {
      consumerThreadID = 0;
    }
    consumerThreadActive = active;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "setConsumerActive", consumerThreadID);
  }

  /**
   * All members of a keyGroup share the same getCursor on the itemStream, which
   * uses this method to filter the items. This allows us to see if an item matches
   * ANY of the members of the group.
   */
  public boolean filterMatches(AbstractItem item)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "filterMatches", item);

    boolean match = false;
    LocalQPConsumerKey matchingMember = null;

    // Hopefully we have a general consumer so we don't need to parse the message
    if(generalMemberCount > 0)
    {
      // We have a match but we don't care which one out of the general members
      // actually takes the message, so if one of the general members is the one
      // performing the scan they can take the message.
      matchingMember = null;
      match = true;
    }
    // Damn, all we've got are members with selectors, we'll have to parse the message
    else
    {
      // If there is just the single member see if they match the message
      if(singleMember != null)
      {
        if(singleMember.filterMatches(item))
        {
          match = true;
          matchingMember = singleMember;
        }
      }
      // Otherwise we give all the members a chance to match it
      else
      {
        LocalQPConsumerKey keyMember;
        int index;
        int size = specificKeyMembers.size();
        for(index = 0;
            (index < size) && !match;
            index++)
        {
          keyMember = specificKeyMembers.get(index);
          // Drop out if one of the members matches it
          if(keyMember.filterMatches(item))
          {
            match = true;
            matchingMember = keyMember;
          }
        }
      }
    }

//  we only want to remember this result if get got here as a result of a consumer
    //asking for a message.
    boolean onConsumerThread = consumerThreadActive &&
                               (Thread.currentThread().getId() == consumerThreadID);

    if(onConsumerThread)
    {
      currentMatch = match;
      currentMatchingMember = matchingMember;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "filterMatches", new Object[] {Boolean.valueOf(match), matchingMember});

    return match;
  }

  /**
   * Returns the member which last matched a message
   * @param preferedKey
   * @return
   */
  public ConsumableKey getMatchingMember(ConsumableKey preferedKey)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getMatchingMember");

    ConsumableKey key = null;

    // The last move of the getCursor found a match
    if(currentMatch)
    {
      // There was a general waiter so we'll try to use the preferred
      // member
      if(currentMatchingMember == null)
      {
        // We can only choose the preferred member if they are not specific
        if(!preferedKey.isSpecific())
          key = preferedKey;
        else if(generalMemberCount > 0)
          key = resolvedKey();
      }
      // The match was to a specific member, we much choose then
      else
        key = currentMatchingMember;
    }
    else
    {
      // There was no match
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getMatchingMember", "SIErrorException");
        throw new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.LocalQPConsumerKeyGroup",
              "1:982:1.6.1.14" },
            null));
    }

    // We shouldn't be here if we didn't get a match
    if(key == null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getMatchingMember", "SIErrorException");
        throw new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.LocalQPConsumerKeyGroup",
              "1:995:1.6.1.14" },
            null));
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMatchingMember", key);

    return key;
  }

  /**
   * Record the fact that one of the members has a message attached
   * @param consumerKey
   */
  public void attachMessage(ConsumableKey consumerKey)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "attachMessage", msgAttachedMember);
      SibTr.exit(tc, "attachMessage", consumerKey);
    }
    if(msgAttachedMember == null)
      msgAttachedMember = consumerKey;
    else
    {
      // We already have a message attached
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "attachMessage", "SIErrorException");
        throw new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.LocalQPConsumerKeyGroup",
              "1:1027:1.6.1.14" },
            null));
    }
  }

  public ConsumableKey getAttachedMember()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getAttachedMember");
      SibTr.exit(tc, "getAttachedMember", msgAttachedMember);
    }
    ConsumableKey key = msgAttachedMember;
    msgAttachedMember = null;
    return key;
  }

  public JSConsumerManager getConsumerManager() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getConsumerManager");
      SibTr.exit(tc, "getConsumerManager", consumerDispatcher);
    }

    return consumerDispatcher;
  }

  public boolean isSpecific() {
    // TODO Auto-generated method stub
    return false;
  }

  public DispatchableConsumerPoint getConsumerPoint() {
    // TODO Auto-generated method stub
    return null;
  }

  public JSConsumerKey getParent() {
    // TODO Auto-generated method stub
    return null;
  }

  public Selector getSelector() {
    // TODO Auto-generated method stub
    return null;
  }

  public void notifyConsumerPointAboutException(SIException e) {
    // TODO Auto-generated method stub

  }

  public void notifyReceiveAllowed(boolean newReceiveAllowed, DestinationHandler handler) {
    // TODO Auto-generated method stub

  }

  public boolean requiresRecovery(SIMPMessage message) throws SIResourceException {
    // TODO Auto-generated method stub
    return false;
  }

  public Object getAsynchGroupLock() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getAsynchGroupLock");
      SibTr.exit(tc, "getAsynchGroupLock", asynchGroupLock);
    }
    return asynchGroupLock;
  }

  /**
   * Create the filters and cursors for this group. If XD has registered a
   * MessageController we'll need a cursor-filter pair for each classification.
   * 
   * @throws SIResourceException
   * @throws MessageStoreException
   */
  private void createNewFiltersAndCursors() 
    throws SIResourceException, MessageStoreException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createNewFiltersAndCursors");
    
    LockingCursor cursor = null;

    if(classifyingMessages)
    {
      // Classifying messages for XD.
      // this work should be done under the classifications readlock acquired higher
      // up in the stack
      JSConsumerClassifications classifications = consumerSet.getClassifications();
      int numClasses = classifications.getNumberOfClasses();      

      // Instantiate a new array of Filters and associated cursors. If there is
      // no message classification, then we'll instantiate a single filter and 
      // cursor pair
      consumerKeyFilter = new LocalQPConsumerKeyFilter[numClasses+1];
    
      for(int i=0;i<numClasses+1;i++)
      {
        String classificationName = null;
        // The zeroth filter belongs to the default classification, which has a 
        // null classification name
        if(i > 0)
          classificationName = classifications.getClassification(i);
        consumerKeyFilter[i] = new LocalQPConsumerKeyFilter(this, i, classificationName);

        cursor = ((ConsumerDispatcher)consumerDispatcher).getItemStream().newLockingItemCursor(consumerKeyFilter[i]);
        consumerKeyFilter[i].setLockingCursor(cursor);
      }      
    }
    else
    {
      consumerKeyFilter = new LocalQPConsumerKeyFilter[1];
      consumerKeyFilter[0] = new LocalQPConsumerKeyFilter(this, 0, null);
      cursor = ((ConsumerDispatcher)consumerDispatcher).getItemStream().newLockingItemCursor(consumerKeyFilter[0]);
      consumerKeyFilter[0].setLockingCursor(cursor);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createNewFiltersAndCursors");
  }  

  /**
   * The Flow properties on a ConsumerSet may be reset dynamically. This
   * method supports the resetting of cursors and filters.
   * 
   */
  public void notifyResetFlowProperties() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "notifyResetFlowProperties");
 
    pendingFlowReset = true;
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "notifyResetFlowProperties"); 
  }    
  
  /**
   * The Flow properties on a ConsumerSet may be reset dynamically. This
   * method supports the resetting of cursors and filters.
   * 
   */
  private void resetFlowProperties() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "resetFlowProperties");
 
    // Tidy up what we had before
    int numFilters = consumerKeyFilter.length;   
    for(int i=0;i<numFilters;i++)
    {
      // Discard the cursor
      consumerKeyFilter[i].discard();
    }    
    
    // Recreate the filters and cursors
    try
    {
      createNewFiltersAndCursors();
    } 
    catch (Exception ex)
    {
      // TODO Auto-generated catch block
      // FFDC
      FFDCFilter.processException(
          ex,
        "com.ibm.ws.sib.processor.impl.LocalQPConsumerKeyGroup.resetFlowProperties",
        "1:1197:1.6.1.14",
        this);

      SIErrorException finalE =
        new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] { "com.ibm.ws.sib.processor.impl.LocalQPConsumerKeyGroup", "1:1204:1.6.1.14", ex },
            null),
            ex);

      SibTr.exception(tc, finalE);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] { "com.ibm.ws.sib.processor.impl.LocalQPConsumerKeyGroup", "1:1210:1.6.1.14", SIMPUtils.getStackTrace(ex) });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "resetFlowProperties", finalE);

      throw finalE;        
    }
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "resetFlowProperties");
  }
  
  /**
   * Only called when consumer is ready and when already holding the ConsumerDispatchers readyConsumerPointLock
   * @return
   */
  public boolean hasNonSpecificConsumers()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "hasNonSpecificConsumers");
    boolean value;
    if (generalMemberCount > 0)
      value = true;
    else
      value = false;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "hasNonSpecificConsumers", Boolean.valueOf(value));
    return value;
  }

  public void lock() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "lock");
    
    _lock.lock();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "lock");
  }

  public void unlock() {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(this, tc, "unlock");
    
    _lock.unlock();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(this, tc, "unlock");
  }
}
