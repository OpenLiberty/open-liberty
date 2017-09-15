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
package com.ibm.ws.sib.processor.runtime.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.exceptions.SIMPIncorrectCallException;
import com.ibm.ws.sib.processor.exceptions.SIMPInvalidRuntimeIDException;
import com.ibm.ws.sib.processor.impl.AnycastInputHandler;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.PubSubOutputHandler;
import com.ibm.ws.sib.processor.impl.indexes.SubscriptionIndex;
import com.ibm.ws.sib.processor.impl.indexes.SubscriptionTypeFilter;
import com.ibm.ws.sib.processor.impl.interfaces.ControllableSubscription;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PubSubMessageItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPLocalSubscriptionControllable;
import com.ibm.ws.sib.processor.runtime.SIMPLocalTopicSpaceControllable;
import com.ibm.ws.sib.processor.runtime.SIMPRemoteTopicSpaceControllable;
import com.ibm.ws.sib.processor.runtime.SIMPTopicSpaceControllable;
import com.ibm.ws.sib.processor.runtime.anycast.AttachedRemoteSubscriberIterator;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;

/**
 * The adapter presented by a queue to perform dynamic
 * control operations.
 * <p>
 * The operations in this interface are specific to a queueing point.
 */
public class Topicspace extends MediatedMessageHandlerControl implements SIMPTopicSpaceControllable
{
  private static TraceComponent tc =
    SibTr.register(
      Topicspace.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
      
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private SubscriptionIndex _subIndex;

  public Topicspace(MessageProcessor messageProcessor,
                    BaseDestinationHandler destination)
  {
    super(messageProcessor,destination);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "Topicspace");
      
    //we don't do this here because the BDH is not initialized yet
    //Instead we use lazy initialization
    //_subIndex = baseDest.getSubscriptionIndex();  
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "Topicspace", this);      
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPTopicSpaceControllable#getAttachedRemoteSubscriberIterator()
   */
  public SIMPIterator getAttachedRemoteSubscriberIterator() 
    throws SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getAttachedRemoteSubscriberIterator");
      
    assertMessageHandlerNotCorrupt();
    
    Collection anycastInputHandlers = baseDest.getPseudoDurableAIHMap().values();
    
    AttachedRemoteSubscriberIterator remoteSubscriberItr = 
      new AttachedRemoteSubscriberIterator(anycastInputHandlers);
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getAttachedRemoteSubscriberIterator", remoteSubscriberItr);
    return remoteSubscriberItr;
  }
  
  private SubscriptionIndex getSubscriptionIndex()
  {
    if(_subIndex==null)
    {
      _subIndex = baseDest.getSubscriptionIndex();
    }
    return _subIndex;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPTopicSpaceControllable#getLocalSubscriptionIterator()
   */
  protected SIMPIterator getDurableSubscriptionIterator() throws SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getDurableSubscriptionIterator");
      
    assertMessageHandlerNotCorrupt();
    SubscriptionTypeFilter filter = new SubscriptionTypeFilter();
    filter.DURABLE = Boolean.TRUE;
    SIMPIterator itr = new ControllableIterator(getSubscriptionIndex().iterator(filter));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getDurableSubscriptionIterator", itr);
    return itr;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPTopicSpaceControllable#getLocalSubscriptionIterator()
   */
  public SIMPIterator getLocalSubscriptionIterator() throws SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getLocalSubscriptionIterator");
      
    assertMessageHandlerNotCorrupt();  
    SubscriptionTypeFilter filter = new SubscriptionTypeFilter();
    filter.LOCAL = Boolean.TRUE;
    SIMPIterator itr = new ControllableIterator(getSubscriptionIndex().iterator(filter));
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getLocalSubscriptionIterator", itr);
    return itr;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPTopicSpaceControllable#getLocalSubscriptionIterator()
   */
  protected SIMPIterator getInternalLocalSubscriptionIterator() throws SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getInternalLocalSubscriptionIterator");
    
    assertMessageHandlerNotCorrupt();
    SubscriptionTypeFilter filter = new SubscriptionTypeFilter();
    filter.LOCAL = Boolean.TRUE;
    SIMPIterator itr = new BasicSIMPIterator(getSubscriptionIndex().iterator(filter));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getInternalLocalSubscriptionIterator", itr);
    return itr;
  }
  
  public SIMPLocalSubscriptionControllable getLocalSubscriptionControlByID(String id)
    throws SIMPInvalidRuntimeIDException,
           SIMPControllableNotFoundException,
           SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getLocalSubscriptionControlByID", id);
      
    assertMessageHandlerNotCorrupt();
    
    ControllableSubscription sub = getSubscription(id);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getLocalSubscriptionControlByID", sub.getControlAdapter());
    return (SIMPLocalSubscriptionControllable) sub.getControlAdapter();
  }
  
  /**
   * Gets and returns the subscription based on the id.
   * @param id
   * @return
   * @throws SIMPControllableNotFoundException
   */
  private ControllableSubscription getSubscription(String id) 
  throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getSubscription", id);
      
    //id is assumed to be SIBUuid12
    SIBUuid12 uuid = new SIBUuid12(id);
    SubscriptionTypeFilter filter = new SubscriptionTypeFilter();
    filter.LOCAL = Boolean.TRUE;
    ControllableSubscription sub = getSubscriptionIndex().findByUuid(uuid, filter);
    
    // If the sub is null, throw an exception
    if (sub == null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(
          tc,
          "getLocalSubscriptionControlByID",
          "SIMPControllableNotFoundException");
          
      throw new SIMPControllableNotFoundException(
        nls.getFormattedMessage(
          "SUBSCRIPTION_NOT_FOUND_ERROR_CWSIP0271",
          new Object[]{id},
          null));  
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getSubscription", sub);
    
    return sub;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPTopicSpaceControllable#deleteLocalSubscriptionControlByID(java.lang.String)
   */
  public void deleteLocalSubscriptionControlByID(String id) 
  throws SIMPInvalidRuntimeIDException, SIMPControllableNotFoundException, SIMPException, 
         SIDurableSubscriptionNotFoundException, SIDestinationLockedException, SIResourceException, SIIncorrectCallException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deleteLocalSubscriptionControlByID", id);
    
    assertMessageHandlerNotCorrupt();

    ControllableSubscription sub = getSubscription(id);

    if (!sub.isDurable())
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(
          tc,
          "deleteLocalSubscriptionControlByID",
          "SIMPIncorrectCallException");
          
      throw new SIMPIncorrectCallException(
        nls.getFormattedMessage(
          "SUBSCRIPTION_DELETE_ERROR_CWSIP0272",
          new Object[]{id},
          null));  
    }
    
    HashMap durableSubs = destinationManager.getDurableSubscriptionsTable();    

    synchronized (durableSubs)
    {
      // Look up the consumer dispatcher for this subId in the system durable subs list
      ConsumerDispatcher cd = 
        (ConsumerDispatcher) durableSubs.get(sub.getConsumerDispatcherState().getSubscriberID());
      
      // Does the subscription exist, if it doesn't, throw a 
      // SIDestinationNotFoundException
      if (cd == null)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
          SibTr.exit(tc, "deleteSubscription", "SIMPControllableNotFoundException");
          
        throw new SIMPControllableNotFoundException(          
          nls.getFormattedMessage(
            "SUBSCRIPTION_DOESNT_EXIST_ERROR_CWSIP0146",
            new Object[] { sub.getConsumerDispatcherState().getSubscriberID(),
                           messageProcessor.getMessagingEngineName() },
          null));
      }        

      // Obtain the destination from the queueing points
      DestinationHandler destination = cd.getDestination();

      // Call the deleteDurableSubscription method on the destination
      // NOTE: this assumes the durable subscription is always local
      destination.deleteDurableSubscription(sub.getConsumerDispatcherState().getSubscriberID(), 
                                            messageProcessor.getMessagingEngineName());
    }

    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteLocalSubscriptionControlByID");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPTopicSpaceControllable#getLocalTopicSpaceIterator()
   */
  public SIMPLocalTopicSpaceControllable getLocalTopicSpaceControl()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getLocalTopicSpaceControl");
      
    SIMPLocalTopicSpaceControllable control = null;
    PubSubMessageItemStream is = baseDest.getPublishPoint();
    if(is != null)
    {
      control = (SIMPLocalTopicSpaceControllable) is.getControlAdapter();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getLocalTopicSpaceControl", control);
      
    return control;
  }
  
  /*
   *  (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPTopicSpaceControllable#getLocalSubscriptionControlByName(java.lang.String)
   */
  public SIMPLocalSubscriptionControllable getLocalSubscriptionControlByName(String subscriptionName) throws SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getLocalSubscriptionControlByName", subscriptionName);
    
    assertMessageHandlerNotCorrupt();
    
    //since subscription name is guaranteed to be unique on the ME, we get the
    //ME wide map of subscriptions
    HashMap durableSubs = messageProcessor.getDestinationManager().getDurableSubscriptionsTable();
    //get a ConsumerDispatcher for the subscription
    ConsumerDispatcher cd = (ConsumerDispatcher)durableSubs.get(subscriptionName);
    SIMPLocalSubscriptionControllable controlAdapter = null;
    if(cd!=null)
    {
      controlAdapter = (SIMPLocalSubscriptionControllable)cd.getControlAdapter();
    }
    // If the sub is null, throw an exception
    if (controlAdapter == null)
    {
      SIMPControllableNotFoundException exception = 
        new SIMPControllableNotFoundException(
            nls.getFormattedMessage(
              "SUBSCRIPTION_NOT_FOUND_ERROR_CWSIP0271",
              new Object[]{subscriptionName},
              null)); 
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(
          tc,
          "getLocalSubscriptionControlByName",
          exception);
          
      throw exception;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getLocalSubscriptionControlByName", controlAdapter);
    return controlAdapter;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPTopicSpaceControllable#getRemoteTopicSpaceIterator()
   */
  public SIMPIterator getRemoteTopicSpaceIterator()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.entry(tc, "getRemoteTopicSpaceIterator");

    //we get the hashmap of all PSOHs
    //these are keyed by SIB8Uuid of the remote ME
    Map pubsubOutHandlers = baseDest.cloneAllPubSubOutputHandlers();
    
    //we get a corresponding list of AIHs, also keyed by remote ME uuid
    Map aihMap = baseDest.getPseudoDurableAIHMap();
    
    Map pairings = new HashMap();
    //first we go through the list of psoh
    Iterator psOHIterator = pubsubOutHandlers.values().iterator();
    while(psOHIterator.hasNext())
    {
      PubSubOutputHandler psoh = (PubSubOutputHandler)psOHIterator.next();
      SIBUuid8 uuid = psoh.getTargetMEUuid();
      //add a new pairing
      RemoteTopicSpaceIterator.PublishConsumePairing pairing = 
        new RemoteTopicSpaceIterator.PublishConsumePairing(psoh, null);
      pairings.put(uuid, pairing);
    }
    
    //now we go through the map adding in AIHs to existing
    //pairing where appropriate, or creating new pairings 
    //for new additions
    Iterator aihIterator = aihMap.values().iterator();
    while(aihIterator.hasNext())
    {
      AnycastInputHandler aih = (AnycastInputHandler)aihIterator.next();
      SIBUuid8 uuid = aih.getLocalisationUuid();
      Object existingPairing = pairings.get(uuid);
      if(existingPairing!=null)
      {
        ((RemoteTopicSpaceIterator.PublishConsumePairing)existingPairing).
          setAnycastInputHandler(aih);
      }
      else
      {
        //create a new pairing for just the AIH
        RemoteTopicSpaceIterator.PublishConsumePairing pairing = 
          new RemoteTopicSpaceIterator.PublishConsumePairing(null, aih);
        pairings.put(uuid, pairing);      
      }
      
    }
    //these pairings are now used by the iterator
    SIMPIterator itr =  new RemoteTopicSpaceIterator(pairings.values().iterator());   
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getRemoteTopicSpaceIterator", itr);
    return itr;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPTopicSpaceControllable#getRemoteTopicSpaceControlByID(java.lang.String)
   */
  public SIMPRemoteTopicSpaceControllable getRemoteTopicSpaceControlByID(String id) 
    throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getRemoteTopicSpaceControlByID", id);

    SIMPIterator remoteTopicSpaces = this.getRemoteTopicSpaceIterator();
    RemoteTopicSpaceControl returnControl=null;
    while(remoteTopicSpaces.hasNext())
    {
      RemoteTopicSpaceControl topicSpace = 
        (RemoteTopicSpaceControl)remoteTopicSpaces.next();
      if(topicSpace.getId().equals(id))
      {
        returnControl = topicSpace;
      }
    }
    if(returnControl==null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(
          tc,
          "getRemoteTopicSpaceControlByID",
          "SIMPControllableNotFoundException");
          
      throw new SIMPControllableNotFoundException(
        nls.getFormattedMessage(
          "SUBSCRIPTION_NOT_FOUND_ERROR_CWSIP0271",
          new Object[]{id},
          null));        
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getRemoteTopicSpaceControlByID", returnControl);
    return returnControl;
  }

}
