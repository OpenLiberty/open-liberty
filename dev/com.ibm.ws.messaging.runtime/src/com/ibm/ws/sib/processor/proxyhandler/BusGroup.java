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
package com.ibm.ws.sib.processor.proxyhandler;

import java.util.Enumeration;
import java.util.Hashtable;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.control.SubscriptionMessage;
import com.ibm.ws.sib.mfp.control.SubscriptionMessageType;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcherState;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The BusGroup contains the collection of Neighbours that belong to this 
 * bus.
 */
final class BusGroup
{
  private static final TraceComponent tc =
    SibTr.register(
      BusGroup.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  
  /** The current subscriptions that have been sent by this local ME to this bus */
  private Hashtable iLocalSubscriptions = new Hashtable();
  
  /** The current set of remote subscriptions that have been forwarded by this ME */
  private Hashtable iRemoteSubscriptions = new Hashtable();  

  /** The name of the bus in question */
  private String iBusName;

  /** The list of all Neighbours in this Bus */
  private Neighbour iNeighbours[] = new Neighbour[0];

  /** Reference to the MultiMEProxyHandler instance */
  private MultiMEProxyHandler iProxyHandler;
  
  /** Lock for subscription addition/removal */
  private Object subscriptionLock = new Object();
  
  /** If true, this bus is the Messaging Engine's local bus*/
  private boolean isLocalBus;

  /** 
   * Creates a new BusGroup instance.
   * 
   * <p>
   * This class holds reference to all Neighbours in this Bus that 
   * this ME is connected to.
   * 
   * @param name  The name of the Bus.
   * @param proxyHandler  The Single Proxy Handler instance.
   * @param localBus true if this BusGroup represents the messaging engine's bus
   */
  BusGroup(String name, MultiMEProxyHandler proxyHandler, boolean localBus)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "BusGroup", 
        new Object[]{name, proxyHandler, new Boolean(localBus)});

    // Assign the name of the Bus
    iBusName = name;

    // Assign the proxy Handler
    iProxyHandler = proxyHandler;
    isLocalBus = localBus;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "BusGroup", this);
  }

  /** Method that returns the name of this Bus
   * 
   * @return String the name of the Bus
   */
  final String getName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getName");
      SibTr.exit(tc, "getName", iBusName);
    }

    return iBusName;
  }

  /** 
   * Method that returns all the Neighbours for this Bus
   * 
   * @return Neighbour[]  An array of all the active Neighbours
   *
   */
  Neighbour[] getMembers()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getMembers");
      SibTr.exit(tc, "getMembers");
    }

    return iNeighbours;
  }

  /** 
   * Gets all the local subscriptions that have been sent to this Bus.
   * 
   * Returns a cloned list of subscriptions to the requester.
   * 
   * @return Hashtable The cloned subscription hashtable.
   */
  Hashtable getLocalSubscriptions()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getLocalSubscriptions");
      SibTr.exit(tc, "getLocalSubscriptions", iLocalSubscriptions);
    }

    return (Hashtable) iLocalSubscriptions.clone();
  }
  
  /** 
   * Gets all the remote subscriptions that have been sent to this Bus.
   * 
   * Returns a cloned list of subscriptions to the requester.
   * 
   * @return Hashtable The cloned subscription hashtable.
   */
  Hashtable getRemoteSubscriptions()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getRemoteSubscriptions");
      SibTr.exit(tc, "getRemoteSubscriptions", iRemoteSubscriptions);
    }

    return (Hashtable) iRemoteSubscriptions.clone();
  }


  /**
   * Called when a subscribe needs to be propagated to the group.
   * <p>
   * The addSubscription puts the subscription into the list of 
   * Subscriptions that have been propagated to this Bus.
   *
   * @param subState        The subscription that was created.
   * @param messageHandler  The subscriptionMessage that will be used for
   *                         sending to Neighbouring ME's 
   * @param transaction     The transaction object to create the subscription with
   * @param sendProxy       Indicates whether to send proxy messages.
   *
   * @return SubscriptionMessageHandler  The new subscription message handler
   *                                      if one was created.
   */
  SubscriptionMessageHandler addLocalSubscription(
    ConsumerDispatcherState subState,
    SubscriptionMessageHandler messageHandler,
    Transaction transaction,
    boolean sendProxy) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "addLocalSubscription",
        new Object[] {
          subState,
          messageHandler,
          transaction,
          new Boolean(sendProxy)});

    // This method will have been called by the local MP when registering
    // a local Subscription.
    messageHandler = addSubscription(subState.getTopicSpaceName(),
                                     subState.getTopicSpaceUuid(),
                                     subState.getTopic(),                                  
                                     messageHandler,
                                     iLocalSubscriptions,
                                     sendProxy);

    // If the message handler is not null then send the message to all the neighbours
    if (messageHandler != null && sendProxy)
      sendToNeighbours(messageHandler.getSubscriptionMessage(), transaction, false);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addLocalSubscription", messageHandler);

    return messageHandler;
  }
  
  /**
   * Adds a remote subscription to this ME.
   * 
   * This subscription would have originated from another Bus/bus
   * 
   * @param topicSpace      The subscriptions topicSpace.
   * @param topic           The subscriptions topic. 
   * @param messageHandler  The subscriptionMessage that will be used for
   *                         sending to Neighbouring ME's 
   * @param sendProxy       If the proxy request should be sent
   */
  SubscriptionMessageHandler addRemoteSubscription(SIBUuid12 topicSpace,
                                                   String topic,
                                                   SubscriptionMessageHandler messageHandler,
                                                   boolean sendProxy)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addRemoteSubscription",
        new Object[] {
            topicSpace,
            topic,
            messageHandler});
    
    messageHandler = 
      addSubscription(null,
                      topicSpace, 
                      topic,
                      messageHandler,
                      iRemoteSubscriptions,
                      sendProxy);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addRemoteSubscription",messageHandler);
      
    return messageHandler;                                                   
  }

  /**
  * Called when a subscribe needs to be propagated to the group.
  * <p>
  * The addSubscription puts the subscription into the list of 
  * Subscriptions that have been propagated to this Bus.
  *
  * @param topicSpaceUuid  The subscriptions topicSpace uuid.
  * @param topic           The subscriptions topic.
  * @param messageHandler  The subscriptionMessage that will be used for
  *                         sending to Neighbouring ME's 
  * @param subscriptionsTable  The table to check for a proxy on.
  * @param sendProxy       Indicates whether to send proxy messages.
  * 
  * @return SubscriptionMessageHandler  The new subscription message handler
  *                                      if one was created.
  */
  private SubscriptionMessageHandler addSubscription(
      String topicSpaceName,
      SIBUuid12 topicSpaceUuid,
      String topic,
      SubscriptionMessageHandler messageHandler,
      Hashtable subscriptionsTable,
      boolean sendProxy)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "addSubscription",
        new Object[] {
          topicSpaceName,
          topicSpaceUuid,
          topic,
          messageHandler,
          subscriptionsTable,
          new Boolean(sendProxy)});

    // Get a key to find the Subscription with
    final String key = subscriptionKey(topicSpaceUuid, topic);

    synchronized( subscriptionLock )
    {
      // Find the subscription from this Buses list of subscriptions.
      MESubscription subscription = (MESubscription) subscriptionsTable.get(key);
      
      //defect 267686
      //Lookup the local topic space name - the remote ME will need to know.
      String localTSName = null;
      try
      {
        DestinationHandler destHand = null;
        
        if (sendProxy || topicSpaceName == null)
          destHand = iProxyHandler.
                       getMessageProcessor().
                         getDestinationManager().
                           getDestination(topicSpaceUuid, false);
        else
          destHand = iProxyHandler.
                       getMessageProcessor().
                         getDestinationManager().getDestination(topicSpaceName, false);
        
        if(destHand!=null)
        {
          localTSName = destHand.getName();
        }
      }
      catch(SIException e)
      {
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.proxyhandler.BusGroup.addSubscription",
          "1:339:1.49",
          this);
        SIErrorException error = new SIErrorException(e); 
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "addSubscription", error);
        throw error;      
      }      
      
      if(localTSName!=null)
      {
        // Lookup foreign topicspace mapping 
        String foreignTSName =
          iProxyHandler.
          getMessageProcessor().
          getDestinationManager().
          getTopicSpaceMapping(iBusName, topicSpaceUuid);        
        
        // If no topicspace mapping exists, we shouldn`t fwd to the other ME.
        if (foreignTSName != null)
        {        
          if (subscription == null)
          {             
            subscription = new MESubscription(topicSpaceUuid, 
                                              localTSName, 
                                              topic, 
                                              foreignTSName);
            subscriptionsTable.put(key, subscription);
          }
           
          // Perform the proxy subscription operation.
          messageHandler = doProxySubscribeOp(subscription.addRef(),
                                              subscription,
                                              messageHandler,
                                              subscriptionsTable,
                                              sendProxy);
        }
        
      }//end if localTSName!=null
    }//end sync

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addSubscription");

    return messageHandler;
  }

  /**
   * Called when an unsubscribe needs to be propagated to the group.
   *
   * @param subState        The subscription to be removed
   * @param sendProxy       Indicates whether to send proxy messages.
   * @param messageHandler  The message that is used to forward onto Neighbours
   * @param transaction   The transaction to remove the subscription with
   * 
   * @return SubscriptionMessageHandler  The new subscription message handler
   *                                      if one was created.
   *
   * @exception  SIResourceException  Thrown when an attempt is
   *                                  made to remove a proxy subscription that
   *                                  that doesn't exist
   */
  SubscriptionMessageHandler removeLocalSubscription(
    ConsumerDispatcherState subState,
    SubscriptionMessageHandler messageHandler,
    Transaction transaction,
    boolean sendProxy) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "removeLocalSubscription",
        new Object[] {
          subState,
          messageHandler,
          new Boolean(sendProxy),
          transaction });

    // As this method is only called when it is a local subscription created, then 
    // the true flag is set to indicate this.
    messageHandler = removeSubscription(subState.getTopicSpaceUuid(),
                                        subState.getTopic(),
                                        messageHandler,
                                        iLocalSubscriptions,
                                        sendProxy);

    // If the message handler is not null then send the message to all the neighbours
    if (messageHandler != null && sendProxy)
      sendToNeighbours(messageHandler.getSubscriptionMessage(), transaction, false);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeLocalSubscription", messageHandler);

    return messageHandler;
  }
  
  /**
  * Called when an unsubscribe needs to be propagated to the group.
  * Decrements the reference count on the subscription for any 
  * subscriptions registered remotely.
  *
  * @param topicSpace      The topicSpace for the subscription
  * @param topic           The topic of the subscription
  * @param messageHandler  The message that is used to forward onto Neighbours
  * 
  * @return SubscriptionMessageHandler  The new subscription message handler
  *                                      if one was created.
  */
  SubscriptionMessageHandler removeRemoteSubscription(
    SIBUuid12 topicSpace,
    String topic,
    SubscriptionMessageHandler messageHandler,
    boolean sendProxy)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeRemoteSubscription",
        new Object[] {
              topicSpace,
              topic,
              messageHandler});
      
    messageHandler = 
      removeSubscription(topicSpace,
                         topic,
                         messageHandler,
                         iRemoteSubscriptions,
                         sendProxy);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeRemoteSubscription", messageHandler);
    
    return messageHandler;    
  }  

  /**
  * Called when an unsubscribe needs to be propagated to the group.
  * Decrements the reference count on the subscription.
  *
  * @param topicSpace      The topicSpace for the subscription
  * @param topic           The topic of the subscription
  * @param sendProxy       Indicates whether to send proxy   .
  * @param messageHandler  The message that is used to forward onto Neighbours
  * 
  * @return SubscriptionMessageHandler  The new subscription message handler
  *                                      if one was created.
  */
  private SubscriptionMessageHandler removeSubscription(
    SIBUuid12 topicSpace,
    String topic,
    SubscriptionMessageHandler messageHandler,
    Hashtable subscriptionsTable,
    boolean sendProxy)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "removeSubscription",
        new Object[] {
          topicSpace,
          topic,
          messageHandler,
          subscriptionsTable,
          new Boolean(sendProxy)});

    // Get a key to find the Subscription with
    final String key = subscriptionKey(topicSpace, topic);

    synchronized( subscriptionLock )
    {
      // Find the subscription from this Buss list of subscriptions.
      final MESubscription subscription = (MESubscription) subscriptionsTable.get(key);
      
      // If the subscription doesn't exist then simply return
      // as this is a Foreign Bus subscription.
      if (subscription == null)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(
            tc,
            "removeSubscription",
            "Non Existent Subscription " + topicSpace + ":" + topic);
        
        return messageHandler;
      }
  
      // Perform the proxy subscription operation.
      messageHandler = doProxySubscribeOp(subscription.removeRef(),
                                          subscription,
                                          messageHandler,
                                          subscriptionsTable,
                                          sendProxy);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeSubscription", messageHandler);

    return messageHandler;
  }

  /** Sends the messages to all the Neighbours in this Bus.
   * 
   * @param transaction  The transaction to send the message under
   * @param msg  The message to be sent.
   */
  protected void sendToNeighbours(SubscriptionMessage msg, Transaction transaction, boolean startup)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendToNeighbours", new Object[] { msg, transaction, new Boolean(startup)});

    for (int i = 0; i < iNeighbours.length; i++)
    {
      // Only want to set the subscription message type if we are at startup.
      if (startup)
      {      
        msg.setSubscriptionMessageType(SubscriptionMessageType.REQUEST);
        iNeighbours[i].setRequestedProxySubscriptions();
      }
        
      iNeighbours[i].sendToNeighbour(msg, transaction);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendToNeighbours");
  }

  /**
   * Adds a reference of Neighbour to this Bus group
   * 
   * @param neighbour  The neighbour to be added to the group
   *
   */
  void addNeighbour(Neighbour neighbour)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addNeighbour", neighbour);

    final Neighbour[] tmp = new Neighbour[iNeighbours.length + 1];
    System.arraycopy(iNeighbours, 0, tmp, 0, iNeighbours.length);
    tmp[iNeighbours.length] = neighbour;

    iNeighbours = tmp;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addNeighbour");
  }

  /** 
   * Removes a Neighbour reference from this Bus group
   *
   * @param neighbour  The Neighbour to be removed
   *
   */
  void removeNeighbour(Neighbour neighbour)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeNeighbour", neighbour);

    Neighbour[] tmp = iNeighbours;

    // Loop through the Neighbours in this Bus
    for (int i = 0; i < iNeighbours.length; ++i)
      if (iNeighbours[i].equals(neighbour))
      {
        // If the Neighbours match, then resize the array without this
        // Neighbour in it.
        tmp = new Neighbour[iNeighbours.length - 1];
        System.arraycopy(iNeighbours, 0, tmp, 0, i);
        System.arraycopy(
          iNeighbours,
          i + 1,
          tmp,
          i,
          iNeighbours.length - i - 1);

        iNeighbours = tmp;

        break;
      }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeNeighbour");
  }

  /**
   * Performs a proxy subscription operation for the given subscription.
   *
   * @param op              The operation to be performed.
   * @param subscription    The subscription.
   * @param sendProxy       Indicates whether to send the proxy message.
   * @param messageHandler  The handler used for creating the message
   *
   * @return SubscriptionMessageHandler  The subscription message that was used
   *                               to send to the Neighbour
   * 
   */
  private SubscriptionMessageHandler doProxySubscribeOp(
    int op,
    MESubscription subscription,
    SubscriptionMessageHandler messageHandler,
    Hashtable subscriptionsTable,
    boolean sendProxy)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "doProxySubscribeOp",
        new Object[] {
          new Integer(op),
          subscription,
          messageHandler,
          new Boolean(sendProxy)});

    // If we don't want to send proxy messages, set the operation to be
    // no operation.

    if (!sendProxy)
    {
      // If we aren't to send the proxy and we have an unsubscribe, then we need
      // to remove the subscription from the list.
      if (op == MESubscription.UNSUBSCRIBE)
        subscriptionsTable.remove(
          subscriptionKey(
            subscription.getTopicSpaceUuid(),
            subscription.getTopic()));

      op = MESubscription.NOP;            
    }

    // Perform an action depending on the required operation.

    switch (op)
    {

      // For new subscriptions or modified subscriptions, send a proxy subscription
      // message to all active neighbours.

      case MESubscription.SUBSCRIBE :

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(
            tc,
            "Publishing new Subscription "
              + subscription + "," + sendProxy);
        // Create the Proxy Message to be sent to all the Neighbours in this
        // Bus   
        if (messageHandler == null)
        {
          messageHandler = iProxyHandler.getMessageHandler();

          // Reset the message.
          messageHandler.resetCreateSubscriptionMessage(subscription, isLocalBus);
        }
        else
        {
          // Add the subscription to the message
          messageHandler.addSubscriptionToMessage(subscription, isLocalBus);
        }

        break;

      case MESubscription.UNSUBSCRIBE :

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(
            tc,
            "Publishing Delete subscription "
              + subscription + "," + sendProxy);

        // Create the Proxy Message to be sent to all the Neighbours in this
        // Bus.
        messageHandler = iProxyHandler.getMessageHandler();
        // Reset the message.
        messageHandler.resetDeleteSubscriptionMessage(subscription, isLocalBus);

        // Remove the subscription from the table.
        subscriptionsTable.remove(
          subscriptionKey(
            subscription.getTopicSpaceUuid(),
            subscription.getTopic()));

        // Send the message to the Neighbours

        break;

        // For other operations, do nothing.

      default :
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(
            tc,
            "Doing nothing for subscription "
              + subscription + "," + sendProxy);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "doProxySubscribeOp", messageHandler);

    return messageHandler;
  }

  /**
   * Generates a string key for a subscription.
   *
   * @param topicSpace The destination Topicspace
   * @param topic      The topic name.
   *
   * @return The key.
   *
   */
  public final static String subscriptionKey(SIBUuid12 topicSpace, String topic)
  {
    return topicSpace + "+" + topic;
  }

  public String toString()
  {
    return "Bus name " + iBusName;
  }
  
  /**
   * Equality test for BusGroup objects.
   * 
   * @param group  The Bus group to check
   * 
   * @return true if the objects are equal.
   */
  public boolean equals(BusGroup group)
  {
    boolean equal = false;
    if (iBusName.equals(group.getName()))
      equal = true;
      
    return equal;
  }

  /**
   * Resets the list of subscriptions by newing up two new lists,
   * the local and remote subscriptions.
   */
  protected void reset()
  {
    iLocalSubscriptions = new Hashtable();
    iRemoteSubscriptions = new Hashtable();
  }
  
  /**
   * Generates the reset subscription message that should be sent to a 
   * member, or members of this Bus.
   * 
   * @return a new ResetSubscriptionMessage
   */
  protected SubscriptionMessage generateResetSubscriptionMessage()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "generateResetSubscriptionMessage");
    
    // Get the Message Handler for doing this operation.
    final SubscriptionMessageHandler messageHandler =
      iProxyHandler.getMessageHandler();

    // Reset the message.
    messageHandler.resetResetSubscriptionMessage();
     
    // Add the local subscriptions
    addToMessage(messageHandler, iLocalSubscriptions);
      
    // Add the remote Subscriptions
    addToMessage(messageHandler, iRemoteSubscriptions);
      
    SubscriptionMessage message = messageHandler.getSubscriptionMessage();
 
    // Add the message back into the pool
    iProxyHandler.addMessageHandler(messageHandler);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "generateResetSubscriptionMessage", message);
   
    return message;
  }
  
  /**
   * Generates the reply subscription message that should be sent to a 
   * the neighbor on the Bus who sent the request.
   * 
   * @return a new ReplySubscriptionMessage
   */
  protected SubscriptionMessage generateReplySubscriptionMessage()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "generateReplySubscriptionMessage");
      
    // Get the Message Handler for doing this operation.
    final SubscriptionMessageHandler messageHandler =
      iProxyHandler.getMessageHandler();
  
    // Reset the message.
    messageHandler.resetReplySubscriptionMessage();
      
    if (iLocalSubscriptions.size() > 0) 
    // Add the local subscriptions
      addToMessage(messageHandler, iLocalSubscriptions);
      
    if (iRemoteSubscriptions.size() > 0) 
      // Add the remote Subscriptions
      addToMessage(messageHandler, iRemoteSubscriptions);
        
    SubscriptionMessage message = messageHandler.getSubscriptionMessage();
   
    // Add the message back into the pool
    iProxyHandler.addMessageHandler(messageHandler);
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "generateReplySubscriptionMessage", message);
     
    return message;
  }
  
  /**
   * Adds the subscriptions to the subscription message
   * 
   * @param messageHandler The message to add to
   * @param subscriptions  The subscriptions to be added.
   */
  private final void addToMessage(SubscriptionMessageHandler messageHandler, 
                                  Hashtable subscriptions)  
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "addToMessage", 
        new Object[]{messageHandler, subscriptions});
    // Get the enumeration of the subscriptions.
    Enumeration enu = subscriptions.elements();

    while (enu.hasMoreElements())
    {
      final MESubscription subscription = (MESubscription) enu.nextElement();

      // Add this subscription into the message.
      messageHandler.addSubscriptionToMessage(subscription, isLocalBus);

    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "addToMessage");
  }

  /**
   * If at startup the reset list failed, then we need to try and send it again.  
   * Each Neighbour needs to be told that the send failed so that it can retry.
   */
  void resetListFailed()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "resetListFailed");
    for (int i = 0; i < iNeighbours.length; i++)
    {
        
      iNeighbours[i].resetListFailed();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "resetListFailed");
    
  }


}
