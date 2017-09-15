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

import java.util.ArrayList;
import java.util.Iterator;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.IncorrectMessageTypeException;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageType;
import com.ibm.ws.sib.mfp.control.SubscriptionMessage;
import com.ibm.ws.sib.mfp.control.SubscriptionMessageType;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.DestinationManager;
import com.ibm.ws.sib.processor.impl.SecurityContext;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.io.MECommsTrc;
import com.ibm.ws.sib.security.auth.OperationType;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.AsynchConsumerCallback;
import com.ibm.wsspi.sib.core.LockedMessageEnumeration;
import com.ibm.wsspi.sib.core.SIBusMessage;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;

/** 
 * Class that listens for messages from the subscription that is made 
 * to listen for Proxy subscription updates.
 */
public class NeighbourProxyListener implements AsynchConsumerCallback
{
  private static final TraceNLS nls_cwsik =
    TraceNLS.getTraceNLS(SIMPConstants.CWSIK_RESOURCE_BUNDLE);
     
  private static final TraceComponent tc =
    SibTr.register(
      NeighbourProxyListener.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

 
  /** Cached reference to the Neighbours object */
  private Neighbours iNeighbours;

  /** Cached reference to the MultiMEProxyHandler */
  private MultiMEProxyHandler iProxyHandler;

  /** The ArrayList of Topics to be added */
  private ArrayList iAddTopics;

  /** The ArrayList of Topic spaces to be added */
  private ArrayList iAddTopicSpaces;
  
  /** The ArrayList of foreign topic space mappings to be added */
  private ArrayList iAddTopicSpaceMappings;

  /** The ArrayList of Topics to be removed */
  private ArrayList iDeleteTopics;

  /** The ArrayList of Topic spaces  to be removed */
  private ArrayList iDeleteTopicSpaces;
  
  /** A cached reference to the Destination Manager instance */
  private DestinationManager iDestinationManager;

  /**
   *  Constructor for the new NeighbourProxyListener which listens for messages
   * put to the System Queue used for receiving proxy subscription update 
   * messages
   * 
   * @param neighbours    The object containing the complete list of Neighbours.
   * @param proxyHandler  The MultiMEProxyHandler instance.
   */
  NeighbourProxyListener(
    Neighbours neighbours,
    MultiMEProxyHandler proxyHandler)
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "NeighbourProxyListener", new Object[]{neighbours, proxyHandler});
      
    iNeighbours = neighbours;
    iProxyHandler = proxyHandler;
    iDestinationManager = 
      iProxyHandler.getMessageProcessor().getDestinationManager();

    iAddTopics = new ArrayList();
    iAddTopicSpaces = new ArrayList();
    iAddTopicSpaceMappings = new ArrayList();

    iDeleteTopics = new ArrayList();
    iDeleteTopicSpaces = new ArrayList();

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "NeighbourProxyListener", this);
  }

  /** 
   * The consumeMessages method that drives the update of the Neighbour state with 
   * a new Proxy
   * 
   * @param message  The message containing the updated subscription state.
   */
  public void consumeMessages(LockedMessageEnumeration messages) throws Exception
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "consumeMessages", new Object[] { messages });

    SIBusMessage jsMessage = null;
    
    while (messages.hasNext())
    {    
      try
      {
        jsMessage = messages.nextLocked();
      }
      catch (SIException e)
      {
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.proxyhandler.NeighbourProxyListener.consumeMessages",
          "1:157:1.73",
          this);
          
        SibTr.exception(tc, e);
      }
  
      if (jsMessage!=null && 
          ((JsMessage) jsMessage).getJsMessageType() != MessageType.SUBSCRIPTION)
      {
        if (tc.isDebugEnabled())
          SibTr.debug(
            tc,
            "Message On incorrect Destination, expecting message "
              + "of Type "
              + MessageType.SUBSCRIPTION
              + " received "
              + ((JsMessage) jsMessage).getJsMessageType());
              
        if (tc.isEntryEnabled()) SibTr.exit(tc, "consumeMessages");
        return;
      }
  
      // Only continue if the message returned is not null
      if (jsMessage!=null)
      {    
        SubscriptionMessage subMessage = null;     
        
        try
        {
          subMessage = ((JsMessage) jsMessage).makeInboundSubscriptionMessage();
        }
        catch (IncorrectMessageTypeException e)
        {
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.proxyhandler.NeighbourProxyListener.consumeMessages",
            "1:193:1.73",
            this);
            
          SibTr.exception(tc, e);
        }
        
        // Invoke the limited ME comms trace again now that we have fluffed up the message
        if (TraceComponent.isAnyTracingEnabled()) {
          MECommsTrc.traceMessage(tc, 
              iProxyHandler.getMessageProcessor(), 
              subMessage.getGuaranteedSourceMessagingEngineUUID(),
              MECommsTrc.OP_SUBMSG_CONSUME, 
              null, 
              subMessage);
        }
        
        LocalTransaction transaction = null;
        
        // Switch on the Type of the Message to understand what we need to 
        // process
        try
        {
          
          // Get a transaction to operate under
          transaction = iProxyHandler.getTransactionManager().createLocalTransaction(true);
    
          switch (subMessage.getSubscriptionMessageType().toInt())
          {
            case SubscriptionMessageType.CREATE_INT :
    
              // Create a new Proxy subscription
              handleCreateProxySubscription(subMessage, (Transaction) transaction);
    
              break;
    
            case SubscriptionMessageType.DELETE_INT :
    
              // Delete a proxy subscription
              handleDeleteProxySubscription(subMessage, (Transaction) transaction);
    
              break;
    
            case SubscriptionMessageType.RESET_INT :
    
              // Reset the subscription state.
              handleResetState(subMessage, (Transaction) transaction, false);
    
              break;
    
            case SubscriptionMessageType.REQUEST_INT :
    
              // Reset the subscription state.
              handleResetState(subMessage, (Transaction) transaction, true);
    
              break;
  
            case SubscriptionMessageType.REPLY_INT :
    
              // Reset the subscription state.
              handleResetState(subMessage, (Transaction) transaction, false);
    
              break;
            default :
              // Log Message
              break;
          }
    
          // Delete the current message from the destination as we 
          // have now read it.
          messages.deleteCurrent(transaction);
        
          // Commit the transaction.
          transaction.commit();
        }      
        catch (SIException e)
        {        
          // If there are any exceptions, then this is bad, log a FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.proxyhandler.NeighbourProxyListener.consumeMessages",
            "1:273:1.73",
            this);
            
          SibTr.exception(tc, e);
          
          handleRollback(subMessage, transaction);                
        }
        catch (RuntimeException e)
        {        
          // If there are any exceptions, then this is bad, log a FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.proxyhandler.NeighbourProxyListener.consumeMessages",
            "1:286:1.73",
            this);
            
          SibTr.exception(tc, e);
          
          handleRollback(subMessage, transaction);        
          
          if (tc.isEntryEnabled()) SibTr.exit(tc, "consumeMessages", e);
          
          throw e;
        }
  
      }
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "consumeMessages");
  }
  
  /**
   * Rolls back and readds the proxy subscriptions that may have been removed.
   * 
   * @param subMessage
   */
  void handleRollback(SubscriptionMessage subMessage, 
                      LocalTransaction transaction)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "handleRollback", new Object[] { subMessage, transaction });

    try
    {
      if (transaction != null)
      {
        try
        {
          transaction.rollback();
        }
        catch (SIException e)
        {
          // If there are any exceptions, then this is bad, log a FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.proxyhandler.NeighbourProxyListener.handleRollback",
            "1:330:1.73",
            this);
          
          SibTr.exception(tc, e);
        }
      }
    
      Transaction msTran = iProxyHandler.getMessageProcessor().resolveAndEnlistMsgStoreTransaction(transaction);
      
      switch (subMessage.getSubscriptionMessageType().toInt())
      {
        case SubscriptionMessageType.CREATE_INT :
    
          // Remove any created Proxy subscriptions
         iProxyHandler.remoteUnsubscribeEvent(iAddTopicSpaces,
                                              iAddTopics, 
                                              subMessage.getBus(), 
                                              msTran,
                                              false);
    
          break;
    
        case SubscriptionMessageType.DELETE_INT :
    
        // Create any removed Proxy subscriptions
        iProxyHandler.remoteSubscribeEvent(iDeleteTopicSpaces,
                                           iDeleteTopics,                                          
                                           subMessage.getBus(),
                                           msTran, 
                                           false);              
    
          break;
    
        case SubscriptionMessageType.RESET_INT :
    
        // Remove any created Proxy subscriptions
        iProxyHandler.remoteUnsubscribeEvent(iAddTopicSpaces,
                                             iAddTopics,                                            
                                             subMessage.getBus(), 
                                             msTran,
                                             false);
    
        // Create any removed Proxy subscriptions
        iProxyHandler.remoteSubscribeEvent(iDeleteTopicSpaces,
                                           iDeleteTopics,                                            
                                           subMessage.getBus(),
                                           msTran, 
                                           false);
                                                         
          break;
    
        default :
          // Log Message
          break;
      }
    }
    catch (SIException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.proxyhandler.NeighbourProxyListener.handleRollback",
        "1:392:1.73",
        this);
    }
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "handleRollback");
  }

  /**
   * Handles a reset Message from the Neighbour.
   * This will sync all proxy subscriptions that are known to this Neighbour 
   * with the remote Neighbours real state
   * 
   * @param resetMessage  The message containing the reset details.
   * @param transaction   The transaction to do the operation
   * @param request       If this is a request message from a remote neighbour
   */
  void handleResetState(SubscriptionMessage resetMessage, 
      Transaction transaction,
                        boolean request)
    throws SIException
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "handleResetState", 
        new Object[]{resetMessage, transaction, new Boolean(request)});
    
    boolean foreignTemporaryProxy = false;
    // Flag for securing foreign proxy subs
    boolean foreignAllowed = true;
    // Userid to be stored when securing foreign proxy subs
    String MESubUserId = null;
    // Flag to indicate whether a proxy sub originated from
    // a foreign bus where the home bus is secured.
    boolean foreignSecuredProxy = false;
        
    // Get the iterators that we require for this method
    final Iterator topics = resetMessage.getTopics().iterator();
    final Iterator topicSpaces = resetMessage.getTopicSpaces().iterator();
    final Iterator topicSpaceMappings = resetMessage.getTopicSpaceMappings().iterator();

    // Get the Me name and ME uuid
    final byte[] meUUIDArr = resetMessage.getMEUUID();
    final SIBUuid8 meUUID = new SIBUuid8(meUUIDArr);

    // Get the Bus that this message arrived from
    final String busId = resetMessage.getBusName();

    // Get the Neighbour from the above details
    Neighbour neighbour = iNeighbours.getNeighbour(meUUID);
    
    // If the neighbour is null, then create the Neighbour.
    if (neighbour == null)
    {   
      iProxyHandler.getLockManager().lockExclusive();

      try
      {          
        // Create the Neighbour
        iProxyHandler.createNeighbour(meUUID, busId, transaction);

        if (tc.isDebugEnabled()) 
          SibTr.debug(tc, "Created Messaging Engine " + meUUID + " on bus " + busId );
          
        neighbour = iNeighbours.getNeighbour(meUUID);
        
        if (!request)
        {
          neighbour.sendRequestProxySubscriptions();          
        }
      }
      catch (SIException e)
      {
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.proxyhandler.NeighbourProxyListener.handleResetState",
          "1:468:1.73",
          this);
          
        if (tc.isEventEnabled())
          SibTr.exception(tc, e);
        
        if (tc.isEntryEnabled())
          SibTr.exit(tc, "handleResetState", "Unable to create Neighbour so exiting");
        return;
      }
      finally
      {
        iProxyHandler.getLockManager().unlockExclusive();       
      }
    }
    
    // Check if the neighbour had had a request sent to it
    if (neighbour.wasProxyRequestSent())
    {
      // Remove the alarm for this neighbour.
      neighbour.setRequestedProxySubscriptionsResponded();
    }

    boolean okToForward = false;

    // If the length of the buses are more than one 
    // then that indicates that this subscription is in more then one bus.
    // This subscription is then required to be forwarded onto all Neighbours in 
    // the other buses.
    if (iNeighbours.getAllBuses().length > 1)
    {
      okToForward = true;

      // Clear the Array Lists needed.
      iAddTopics.clear();
      iAddTopicSpaces.clear();
      iAddTopicSpaceMappings.clear();
      iDeleteTopics.clear();
      iDeleteTopicSpaces.clear();
    }

    // Mark all the current subscriptions from this Neighbour
    neighbour.markAllProxies();

    // Cycle through each of the new topics.
    while (topics.hasNext())
    {
      // Get the parameters for this iteration
      final String topic = (String) topics.next();
      //as we are receiving this from a remote ME, the foreign topic
      //space name is actually our own topic space name.
      String topicSpaceName = (String) topicSpaceMappings.next();
      //see defect 267686:
      //This will be the topic space uuid if the subscription is coming
      //from another ME on the same bus. Otherwise it will be the foreign
      //topic space name from this ME's perspective.
      String foreignTopicSpaceName = (String)topicSpaces.next();
      
      //the uuid for our local topic space
      SIBUuid12 localTopicSpaceUuid = null; 
      
      foreignTemporaryProxy = false;
      foreignAllowed = true;
      MESubUserId = null;
      foreignSecuredProxy = false;
      
      // If this message originated from a foreign bus then convert the
      // topicSpaceName to a uuid
      if (!busId.equals(iProxyHandler.getMessageProcessor().getMessagingEngineBus()))
      {
        DestinationHandler topicSpace = 
          iDestinationManager.getDestination(topicSpaceName, false);
                      
        if (topicSpace == null)
        {
          // We do not register temporary proxies for foreign buses.
          // Instead the situation is handled with the request message code.  
          // So we ignore the message
          foreignTemporaryProxy = true;
        }
        else 
        {
          // Check whether bus security is enabled 
          if (iProxyHandler.getMessageProcessor().isBusSecure()) 
          {
            // Check authority to produce to destination
            try 
            {
              if(resetMessage.isSecurityUseridSentBySystem())
              {
                // Message is from a foreign bus but still retains
                // SIBServerSubject. This implies that no link inbound
                // userid was set and we'll allow processing to continue
                // without the necessity of access checks.                            
              }
              else
              {                            
                // Get the userid against which to check access.
                // This userid will also be stored in the MESubscription
                MESubUserId = resetMessage.getSecurityUserid();
                foreignAllowed =
                  checkDestinationAccess(topicSpace,
                                         topic,
                                         MESubUserId);
                foreignSecuredProxy = foreignAllowed;
              }
            } 
            catch (SIException e) 
            {
              // No FFDC code needed
              if (tc.isDebugEnabled())
                SibTr.debug(tc, "Caught exception " + e);
              foreignAllowed = false;
            }
          }

          if (foreignAllowed)
          {
            //we have located our local dest handler for
            //our local topic space for this subscription, so we can 
            //obtain the uuid
            localTopicSpaceUuid = topicSpace.getUuid();
          }          
        }
      } 
      else
      {
        //see defect 267686:
        //we are on the same bus as the subscribing ME - this means that
        //the foreignTopicSpace is actually the local topic space's uuid
        localTopicSpaceUuid = new SIBUuid12(foreignTopicSpaceName);
      }
        

      if (!foreignTemporaryProxy && foreignAllowed) 
      {
        // Remove the proxy to this topic
        final MESubscription subscription = 
          neighbour.proxyRegistered(localTopicSpaceUuid,
                                    topicSpaceName,
                                    topic,
                                    foreignTopicSpaceName,
                                    transaction,
                                    foreignSecuredProxy,
                                    MESubUserId);
                                    
        // If the proxy was registered and there are other Neighbours
        // that we can forward this message onto, then add the topic
        // to the list of topics to be forwarded.
        if (subscription!=null)
        {
          // This is a created proxy, so create the PubSubHandler and 
          // assign to the match space.
          final boolean proxyCreated = 
            iNeighbours.createProxy(
              neighbour,
              iDestinationManager.getDestinationInternal(localTopicSpaceUuid, false),
              subscription,
              localTopicSpaceUuid, 
              topic,
              false);
          
          if (proxyCreated && okToForward)
          {
            // Add to the list of topics to forward
            iAddTopics.add(topic);
            iAddTopicSpaces.add(localTopicSpaceUuid);
            iAddTopicSpaceMappings.add(foreignTopicSpaceName);
          }
        }
      }
    }

    // Tell the Neighbour to remove all subscriptions that aren't needed 
    // any more
    neighbour.sweepMarkedProxies(
      iDeleteTopicSpaces,
      iDeleteTopics,
      transaction,
      okToForward);

    // Forward the deletions onto the other connected Neighbours
    if (okToForward && iDeleteTopics.size() > 0)
    {
      iProxyHandler.unsubscribeEvent(
        iDeleteTopicSpaces,
        iDeleteTopics,      
        busId,
        transaction);
    }

    // Forward the creations onto the other connected Neigbours
    if (okToForward && iAddTopics.size() > 0)
    {
      iProxyHandler.subscribeEvent(iAddTopicSpaces, iAddTopics, busId, transaction);
    }
    
    if (request)
      neighbour.sendReplyMessage();

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "handleResetState");
  }

  /**
   * Used to remove a proxy subscription on this Neighbour
   * 
   * There can be more than one Subscription to be removed in this
   * message so the list of topics needs to be iterated through.
   * 
   * The message can also be empty indicating that this Neighbour
   * can now be removed.
   * 
   * @param deleteMessage  This is the message containing the delete
   *                        information.
   * @param transaction    The transaction to do the delete under
   */
  void handleDeleteProxySubscription(SubscriptionMessage deleteMessage,
      Transaction transaction) throws SIResourceException
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "handleDeleteProxySubscription", 
        new Object[] { deleteMessage, transaction });

    // Get the iterators that we require for this method
    final Iterator topics = deleteMessage.getTopics().iterator();
    final Iterator topicSpaces = deleteMessage.getTopicSpaces().iterator();
    final Iterator topicSpaceMappings = deleteMessage.getTopicSpaceMappings().iterator();

    // Get the Me name and ME uuid
    final byte[] meUUIDArr = deleteMessage.getMEUUID();
    final SIBUuid8 meUUID = new SIBUuid8(meUUIDArr);

    // Get the bus that this message arrived from
    final String busId = deleteMessage.getBusName();
    //now we have the relevant information we 
    //can delete the proxy subscription
    deleteProxySubscription(topics,
      topicSpaces, topicSpaceMappings, meUUID, busId, transaction);
    
    if (tc.isEntryEnabled())
      SibTr.exit(tc, "handleDeleteProxySubscription");
  } 
   
  public void deleteProxySubscription(Iterator topics, Iterator topicSpaces,
    Iterator topicSpaceMappings, SIBUuid8 meUUID, String busId, 
    Transaction transaction) throws SIResourceException
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "deleteProxySubscription", 
        new Object[] {topics, topicSpaces,topicSpaceMappings,
          meUUID, busId, transaction});
          
    // Get the Neighbour
    final Neighbour neighbour = 
      iNeighbours.getNeighbour(meUUID);
      
    // If the neighbour is null, then simply return.
    if (neighbour == null)
    { 
      if (tc.isEntryEnabled()) 
        SibTr.exit(tc, "deleteProxySubscription", "Unknown neighbour");   
      return;
    }

    boolean okToForward = false;

    // If the length of the Buses are more than one 
    // then that indicates that this subscription is in more then one bus.
    // This subscription is then required to be forwarded onto all Neighbours in 
    // the other buses.
    if (iNeighbours.getAllBuses().length > 1)
    {
      okToForward = true;

      iDeleteTopics.clear();
      iDeleteTopicSpaces.clear();
    }

    // Iterate through the parameters stored in the Message
    while (topics.hasNext())
    {
      // Get the parameters for this iteration
      final String topic = (String) topics.next();
      //as we are receiving this from a remote ME, the foreign topic
      //space name is actually our own topic space name.
      String topicSpaceName = (String) topicSpaceMappings.next();
      //see defect 267686:
      //This will be the topic space uuid if the subscription is coming
      //from another ME on the same bus. Otherwise it will be the foreign
      //topic space name from this ME's perspective.
      String foreignTopicSpaceName = (String)topicSpaces.next();
      
      //the uuid for our local topic space
      SIBUuid12 localTopicSpaceUuid = null; 

      // If this message originated from a foreign bus then convert the
      // topicSpaceName to a uuid
      if (!busId.equals(iProxyHandler.getMessageProcessor().getMessagingEngineBus()))
      {
        DestinationHandler topicSpace = null;
        try
        { 
          topicSpace = 
            iDestinationManager.getDestination(topicSpaceName, false);
        } catch (SIException e)
        {
          // No FFDC code needed
          if (tc.isDebugEnabled())
            SibTr.debug(tc, "Destination not found " + topicSpaceName);
        }
                      
        if (topicSpace == null)
        {
          // We do not register temporary proxies for foreign buses.
          // Instead the situation is handled with the request message code.  
          // So we ignore the message
          if (tc.isEntryEnabled())
            SibTr.exit(tc, "deleteProxySubscription", "Destination not found " + topicSpaceName);

          return;
        }
        //we have located our local dest handler for
        //our local topic space for this subscription, so we can 
        //obtain the uuid
        localTopicSpaceUuid = topicSpace.getUuid();
      }
      else
      {
        //see defect 267686:
        //we are on the same bus as the subscribing ME - this means that
        //the foreignTopicSpace is actually the local topic space's uuid
        localTopicSpaceUuid = new SIBUuid12(foreignTopicSpaceName);
      }
        
        
      // Remove the proxy to this topic
      final MESubscription subscription =
        neighbour.proxyDeregistered(localTopicSpaceUuid, topic, transaction);

      // If the proxy wasn't deregistered, then remove the element from the list.
      if (subscription!=null)
      {
        // Tell the Neighbours class that the Proxy has been deleted
        // so it can remove the PubSubOutputHandlers if nessacary.
        final boolean proxyDeleted = 
          iNeighbours.deleteProxy(
            iDestinationManager.getDestinationInternal(localTopicSpaceUuid, false),
            subscription, 
            neighbour,
            localTopicSpaceUuid,
            topic,
            true,
            false); // Don't delete the proxy.
        
        if (proxyDeleted && okToForward)
        {        
          iDeleteTopics.add(topic);
          iDeleteTopicSpaces.add(localTopicSpaceUuid);
        }
      }
    }

    // Does this need to be forwarded onto another Neighbour ?
    if (okToForward && iDeleteTopics.size() > 0)
    {
      iProxyHandler.unsubscribeEvent(
        iDeleteTopicSpaces,
        iDeleteTopics,
        busId,
        transaction);
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "deleteProxySubscription");
  }

  /** 
   * Creates a proxy subscription on this Neighbour
   * 
   * This method parses the SubscriptionMessage and pulls out
   * the relevant subscription creation information.  
   * 
   * @param createMessage  The subscription message containing the details
   * @param transaction   The transaction to do the create under.
   */
  protected void handleCreateProxySubscription(SubscriptionMessage createMessage,
      Transaction transaction) throws SIResourceException
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "handleCreateProxySubscription", 
        new Object[] { createMessage, transaction });
        
    // Flag for securing foreign proxy subs
    boolean foreignAllowed = true;
    // Userid to be stored when securing foreign proxy subs
      String MESubUserId = null;
    // Flag to indicate whether a proxy sub originated from
    // a foreign bus where the home bus is secured.
    boolean foreignSecuredProxy = false;
    
    // Get the iterators that we require for this method
    final Iterator topics = createMessage.getTopics().iterator();
    final Iterator topicSpaces = createMessage.getTopicSpaces().iterator();
    final Iterator topicSpaceMappings = createMessage.getTopicSpaceMappings().iterator();


    // Get the Me name and ME uuid
    final byte[] meUUIDArr = createMessage.getMEUUID();
    final SIBUuid8 meUUID = new SIBUuid8(meUUIDArr);

    // Get the bus that this message arrived from
    final String busId = createMessage.getBusName();

    // Get the Neighbour from the above details
    Neighbour neighbour = iNeighbours.getNeighbour(meUUID);
    
    // If the neighbour is null, then simply return.
    if (neighbour == null)
    { 
      if (tc.isEntryEnabled()) 
        SibTr.exit(tc, "handleCreateProxySubscription", "Unknown neighbour");   
      return;
    }
    
    boolean okToForward = false;

    // If the length of the buses are more than one 
    // then that indicates that this subscription is in more then one bus.
    // This subscription is then required to be forwarded onto all Neighbours in 
    // the other buses.
    if (iNeighbours.getAllBuses().length > 1)
    {
      okToForward = true;

      // Reset the list of add subscriptions.
      iAddTopics.clear();
      iAddTopicSpaces.clear();
      iAddTopicSpaceMappings.clear();
    }

    // Iterate through the parameters stored in the Message
    while (topics.hasNext())
    {
      // Get the parameters for this iteration
      final String topic = (String) topics.next();
      //as we are receiving this from a remote ME, the foreign topic
      //space name is actually our own topic space name.
      String topicSpaceName = (String) topicSpaceMappings.next();
      //see defect 267686:
      //This will be the topic space uuid if the subscription is coming
      //from another ME on the same bus. Otherwise it will be the foreign
      //topic space name from this ME's perspective.
      String foreignTopicSpaceName = (String)topicSpaces.next();
      
      //the uuid for our local topic space
      SIBUuid12 localTopicSpaceUuid = null;

      foreignAllowed = true;
      MESubUserId = null;
      foreignSecuredProxy = false;
      
      // If this message originated from a foreign bus then convert the
      // topicSpaceName to a uuid
      if (!busId.equals(iProxyHandler.getMessageProcessor().getMessagingEngineBus()))
      {
        DestinationHandler topicSpace = null;
        try
        { 
          topicSpace =  
            iDestinationManager.getDestination(topicSpaceName, false);
        } 
        catch (SIException e)
        {
          // No FFDC code needed
          if (tc.isDebugEnabled())
            SibTr.debug(tc, "Destination not found " + topicSpaceName);
        }
                      
        if (topicSpace == null)
        {
          // We do not register temporary proxies for foreign buses.
          // Instead the situation is handled with the request message code.  
          // So we ignore the message
          if (tc.isEntryEnabled())
            SibTr.exit(tc, "handleCreateProxySubscription", "Topic Space " + topicSpaceName + " not found");

          return;
        }
        
        // Check whether bus security is enabled 
        if (iProxyHandler.getMessageProcessor().isBusSecure()) 
        {
          try 
          {
            if(createMessage.isSecurityUseridSentBySystem())
             {
              // Message is from a foreign bus but still retains
              // SIBServerSubject. This implies that no link inbound
              // userid was set and we'll allow processing to continue
              // without the necessity of access checks.              
            }
            else
            {
              // Get the userid against which to check access.
              // This userid will also be stored in the MESubscription
              MESubUserId = createMessage.getSecurityUserid(); 
                                
              // Check authority to produce to destination
              foreignAllowed =
                checkDestinationAccess(topicSpace,
                                       topic,
                                       MESubUserId);
              foreignSecuredProxy = foreignAllowed;
            }
          } 
          catch (SIException e) 
          {
            // No FFDC code needed
            if (tc.isDebugEnabled())
              SibTr.debug(tc, "Caught exception " + e);
            foreignAllowed = false;
          }
        }
        
        if (foreignAllowed)
        {
          //we have located our local dest handler for
          //our local topic space for this subscription, so we can 
          //obtain the uuid
          localTopicSpaceUuid = topicSpace.getUuid();
        } 
      } 
      else
      {
        //see defect 267686:
        //we are on the same bus as the subscribing ME - this means that
        //the foreignTopicSpace is actually the local topic space's uuid
        localTopicSpaceUuid = new SIBUuid12(foreignTopicSpaceName);
      }

      if (foreignAllowed) 
      {
        // Tell the Neighbour to create the proxy subscription if
        // required.

        final MESubscription sub = 
          neighbour.proxyRegistered(localTopicSpaceUuid,
                                    topicSpaceName, 
                                    topic,
                                    foreignTopicSpaceName,
                                    transaction,
                                    foreignSecuredProxy,
                                    MESubUserId);
                                    
        // If this was registered, then add to the list of registered subscriptions,
        // but we only need to do this if we can forward this onto other Neighbours.
        if (sub!=null)
        {
          // Create the PubSubHandler and assign it to the match space
          final boolean created =
            iNeighbours.createProxy(
              neighbour,
              iDestinationManager.getDestinationInternal(localTopicSpaceUuid, false),
              sub,
              localTopicSpaceUuid,
              topic,
              false);

          // Only if the pubsub output handler is created, run through the
          // proxy created code.
          if (created && okToForward)
          {
            iAddTopics.add(topic);
            iAddTopicSpaces.add(localTopicSpaceUuid);
            iAddTopicSpaceMappings.add(foreignTopicSpaceName);
          }
        }
      }
    }

    // Forward onto other Neighbours if this is a new proxy
    // and there are other Neighbours that require this change.
    if (okToForward && iAddTopics.size() > 0)
    {
      iProxyHandler.subscribeEvent(iAddTopicSpaces, iAddTopics, busId, transaction);
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "handleCreateProxySubscription");
  }
  
  /**
   * <p>Check authority to produce to a destination</p>
   * @param destinationHandler
   * @param discriminator
   * @param msg
   * @param exceptionReason
   * @param exceptionInserts
   * @return
   * @throws SIStoreException
   */
  private boolean checkDestinationAccess(
    DestinationHandler destination,
    String discriminator,
    String userId)
    throws SIDiscriminatorSyntaxException {
    if (tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "checkDestinationAccess",
        new Object[] { destination, discriminator, userId });

    boolean allowed = true;

    // Map a null userid to an empty string
    if (userId == null) {
      userId = ""; // Empty string means those users that have not
      // been authenticated, but who are still, of course,
      // members of EVERYONE. 
    }

    // We use the userid flavour of security context as we need
    // to drive the userid flavour of the sib.security checkDestinationAccess
    // which is attuned for access checks for messages from foreign buses.
    SecurityContext secContext = new SecurityContext(userId, discriminator);

    // Check authority to access the destination    
    if (!destination
      .checkDestinationAccess(secContext, OperationType.RECEIVE)) 
    {
      if (tc.isDebugEnabled())
        SibTr.debug(
          tc,
          "checkDestinationAccess",
          "not authorized to produce to this destination");

      // Build the message for the Exception and the Notification
      String nlsMessage = 
      nls_cwsik.getFormattedMessage("DELIVERY_ERROR_SIRC_18", // USER_NOT_AUTH_SEND_ERROR_CWSIP0306
                                    new Object[] { destination.getName(), 
                                                   userId },
                                    null); 
                                    
      // Fire a Notification if Eventing is enabled
      iProxyHandler.
        getMessageProcessor().
        getAccessChecker().
        fireDestinationAccessNotAuthorizedEvent(destination.getName(),
                                        userId,
                                        OperationType.RECEIVE,
                                        nlsMessage);

      // sib.security will audit this
      allowed = false;
    }

    // Check authority to produce to discriminator
    if (allowed) {
      // If the discriminator is non-wildcarded, we can check authority to consume on
      // the discriminator.
      if (discriminator != null
        && !iProxyHandler
          .getMessageProcessor()
          .getMessageProcessorMatching()
          .isWildCarded(discriminator)) 
      {
        if (!destination
          .checkDiscriminatorAccess(secContext, OperationType.RECEIVE)) 
        {
          if (tc.isDebugEnabled())
            SibTr.debug(
              tc,
              "checkDestinationAccess",
              "not authorized to produce to this discriminator");

          // Write an audit record if access is denied
          SibTr
            .audit(
              tc,
              nls_cwsik
                .getFormattedMessage("DELIVERY_ERROR_SIRC_20",
          // USER_NOT_AUTH_SEND_ERROR_CWSIP0308
          new Object[] {
            destination.getName(),
            discriminator,
            userId },
            null));

          allowed = false;
        }
      }
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "checkDestinationAccess", new Boolean(allowed));
    return allowed;
  }

}
