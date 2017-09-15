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

// Import required classes.
import java.util.ArrayList;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.impl.ControlMessageFactory;
import com.ibm.ws.sib.mfp.control.SubscriptionMessage;
import com.ibm.ws.sib.mfp.control.SubscriptionMessageType;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 *  Wrapper class for handling the Proxy subscription
 *  messages that are forwarded onto Neighbouring ME's
 */
final class SubscriptionMessageHandler
{
  private static final TraceComponent tc =
    SibTr.register(
      SubscriptionMessageHandler.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  /** The SubscriptionMessage to be sent to the Neighbour */
  private SubscriptionMessage iSubscriptionMessage;

  /** List of Topics */
  private List iTopics;
  /** List of TopicSpaces */
  private List iTopicSpaces;
  /** List of foreign TopicSpaceMappings */
  private List iTopicSpaceMappings;
  /** The MEName */
  private String iMEName;
  /** The byte[] containing the meuuid */
  private SIBUuid8 iMEUUID;
  
  /** The Bus name for this message */
  private String iBusName;
  
  /** Cached reference to the proxy handler class */
  private MultiMEProxyHandler iProxyHandler;

  /** Whether this instance has been initialised */
  private boolean iInitialised = false;

  /** 
   * Constructor for a new Message Handler 
   * 
   * This will initialise the list above for setting into the subscription
   * message.
   * 
   * @param proxyHandler the proxy handler
   */
  protected SubscriptionMessageHandler(MultiMEProxyHandler proxyHandler)
  {
    if (tc.isEntryEnabled()) 
      SibTr.entry(tc, "SubscriptionMessageHandler", proxyHandler);

    iProxyHandler = proxyHandler;

    // Initialise the variables.  
    iTopics = new ArrayList();
    iTopicSpaces = new ArrayList();
    iTopicSpaceMappings = new ArrayList();
    iMEName = null;
    iMEUUID = null;
    iBusName = null;

    // Add the single element to The meName and me UUID
    iMEName = iProxyHandler.getMessageProcessor().getMessagingEngineName();
    iMEUUID = iProxyHandler.getMessageProcessor().
                getMessagingEngineUuid();
                
    // Get the Bus name for this Neighbour
    iBusName = iProxyHandler.getMessageProcessor().getMessagingEngineBus();
                

    // Generate the subscription message
    iSubscriptionMessage = createSubscriptionMessage();

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "SubscriptionMessageHandler", this);
  }

  /**
   * This method creates the subscription control message
   * 
   * @return The SubscriptionMessage created
   */
  private SubscriptionMessage createSubscriptionMessage()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "createSubscriptionMessage");

    ControlMessageFactory factory = null;
    SubscriptionMessage subscriptionMessage = null;

    try
    {
      factory = MessageProcessor.getControlMessageFactory();
      subscriptionMessage = factory.createNewSubscriptionMessage();
    }
    catch (Exception e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.proxyhandler.SubscriptionMessageHandler.createSubscriptionMessage",
        "1:162:1.34",
        this);
        
      SibTr.exception(tc, e);
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "createSubscriptionMessage", subscriptionMessage);

    return subscriptionMessage;
  }

  /** 
   * Resets the complete message state
   */
  private void reset()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "reset");

    if (iInitialised)
    {
      // Reset the ArrayLists associated with this message        
      iTopics.clear();
      iTopicSpaces.clear();
      iTopicSpaceMappings.clear();

      // Create a new message to send to this Neighbour.
      iSubscriptionMessage = createSubscriptionMessage();
    }
    else
    {
      iInitialised = true;
    }

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "reset");
  }

  /**
   * Method to reset the Subscription message object and
   * reinitialise it as a create proxy subscription message
   *    
   * @param subscription  The subscription to add to the message.
   * @param isLocalBus    The subscription is being sent to a the local bus
   *  
   */
  protected void resetCreateSubscriptionMessage(MESubscription subscription, boolean isLocalBus)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "resetCreateSubscriptionMessage", new Object[]{subscription, new Boolean(isLocalBus)});

    // Reset the state
    reset();

    // Indicate that this is a create message    
    iSubscriptionMessage.setSubscriptionMessageType(
      SubscriptionMessageType.CREATE);

    // Add the subscription related information.
    iTopics.add(subscription.getTopic());
    
    if(isLocalBus)
    {
      //see defect 267686:
      //local bus subscriptions expect the subscribing ME's
      //detination uuid to be set in the iTopicSpaces field
      iTopicSpaces.add(subscription.getTopicSpaceUuid().toString());
    }
    else
    {
      //see defect 267686:
      //foreign bus subscriptions need to set the subscribers's topic space name.
      //This is because the messages sent to this topic over the link
      //will need to have a routing destination set, which requires
      //this value.
      iTopicSpaces.add(subscription.getTopicSpaceName().toString());
    }
    
    iTopicSpaceMappings.add(subscription.getForeignTSName());

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "resetCreateSubscriptionMessage");
  }

  /**
   * Method to reset the Subscription message object and
   * reinitialise it as a delete proxy subscription message
   * 
   * @param subscription  The subscription object to reset on
   * @param isLocalBus    The subscription is being sent to the local bus
   * 
   */
  protected void resetDeleteSubscriptionMessage(MESubscription subscription, boolean isLocalBus)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "resetDeleteSubscriptionMessage", new Object[]{subscription, new Boolean(isLocalBus)});

    // Reset the state
    reset();

    // Indicate that this is a create message    
    iSubscriptionMessage.setSubscriptionMessageType(
      SubscriptionMessageType.DELETE);

    // Add the subscription related information.
    iTopics.add(subscription.getTopic());
    if(isLocalBus)
    {
      //see defect 267686:
      //local bus subscriptions expect the subscribing ME's
      //detination uuid to be set in the iTopicSpaces field
      iTopicSpaces.add(subscription.getTopicSpaceUuid().toString());
    }
    else
    {
      //see defect 267686:
      //foreign bus subscriptions need to set the subscribers's topic space name.
      //This is because the messages sent to this topic over the link
      //will need to have a routing destination set, which requires
      //this value.
      iTopicSpaces.add(subscription.getTopicSpaceName().toString());
    }
    iTopicSpaceMappings.add(subscription.getForeignTSName());

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "resetDeleteSubscriptionMessage");
  }

  /**
   * Method to reset the Subscription message object and
   * reinitialise it as a delete proxy subscription message.
   * 
   * This empty delete message that is created tells the Neighbouring
   * ME that this is the last message that it will receive from it.
   * 
   */
  protected void resetDeleteSubscriptionMessage()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "resetDeleteSubscriptionMessage");

    // Reset the state
    reset();

    // Indicate that this is a create message    
    iSubscriptionMessage.setSubscriptionMessageType(
      SubscriptionMessageType.DELETE);

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "resetDeleteSubscriptionMessage");
  }

  /**
   * Method to reset the Subscription message object and
   * reinitialise it as a Reset proxy subscription message
   */
  protected void resetResetSubscriptionMessage()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "resetResetSubscriptionMessage");

    // Reset the state
    reset();
    
    // Indicate that this is a create message    
    iSubscriptionMessage.setSubscriptionMessageType(
      SubscriptionMessageType.RESET);

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "resetResetSubscriptionMessage");
  }
  
  /**
   * Method to reset the Subscription message object and
   * reinitialise it as a Reply proxy subscription message
   */
  protected void resetReplySubscriptionMessage()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "resetReplySubscriptionMessage");

    // Reset the state
    reset();
    
    // Indicate that this is a create message    
    iSubscriptionMessage.setSubscriptionMessageType(
      SubscriptionMessageType.REPLY);

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "resetReplySubscriptionMessage");
  }

  /** 
   * Adds a subscription to the proxy message that is to be sent.
   * 
   * This will either be a delete or reset message.
   * Reset will add messages to the list to resync with the Neighbour
   * and the Delete will send it on due to receiving a Reset.
   * 
   * @param subscription The MESubscription to add to the message.
   * @param isLocalBus   The message is for the messaging engine's local bus
   */
  protected void addSubscriptionToMessage(MESubscription subscription, boolean isLocalBus)
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "addSubscriptionToMessage", new Object[]{subscription, new Boolean(isLocalBus)});

    // Add the subscription related information.
    iTopics.add(subscription.getTopic());
    if(isLocalBus)
    {
      //see defect 267686:
      //local bus subscriptions expect the subscribing ME's
      //detination uuid to be set in the iTopicSpaces field
      iTopicSpaces.add(subscription.getTopicSpaceUuid().toString());
    }
    else
    {
      //see defect 267686:
      //foreign bus subscriptions need to set the subscribers's topic space name.
      //This is because the messages sent to this topic over the link
      //will need to have a routing destination set, which requires
      //this value.
      iTopicSpaces.add(subscription.getTopicSpaceName().toString());
    }
    iTopicSpaceMappings.add(subscription.getForeignTSName());

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "addSubscriptionToMessage");
  }

  /** 
   * Method to return the Subscription Message to be sent
   * to the Neighbouring ME's
   * 
   * This also assigns all the Lists to the message.
   * 
   * @return SubscriptionMessage  The subscription message to be
   *                               sent to the Neighbouring ME.
   * @param isForeignBus  The subscription is being sent to a foreign bus
   */
  protected SubscriptionMessage getSubscriptionMessage()
  {
    if (tc.isEntryEnabled())
      SibTr.entry(tc, "getSubscriptionMessage");

    // Set the values in the message    
    iSubscriptionMessage.setTopics(iTopics);
    iSubscriptionMessage.setMEName(iMEName);
    iSubscriptionMessage.setMEUUID(iMEUUID.toByteArray());
    iSubscriptionMessage.setBusName(iBusName);
    iSubscriptionMessage.setReliability(Reliability.ASSURED_PERSISTENT);     //PK36530
    
    iSubscriptionMessage.setTopicSpaces(iTopicSpaces);
    iSubscriptionMessage.setTopicSpaceMappings(iTopicSpaceMappings);

    if (tc.isEntryEnabled())
      SibTr.exit(tc, "getSubscriptionMessage", new Object[] {
          iTopics, iMEName, iMEUUID, iBusName, iTopicSpaces, iTopicSpaceMappings});

    return iSubscriptionMessage;
  }

}
