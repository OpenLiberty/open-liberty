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
import java.util.List;
import java.util.Set;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SINotSupportedException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.admin.DestinationAliasDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.admin.MQLinkDefinition;
import com.ibm.ws.sib.admin.SIBExceptionBase;
import com.ibm.ws.sib.admin.SIBExceptionDestinationNotFound;
import com.ibm.ws.sib.admin.VirtualLinkDefinition;
import com.ibm.ws.sib.processor.Administrator;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.SubscriptionDefinition;
import com.ibm.ws.sib.processor.exceptions.SIMPDestinationAlreadyExistsException;
import com.ibm.ws.sib.processor.exceptions.SIMPDestinationCorruptException;
import com.ibm.ws.sib.processor.exceptions.SIMPNullParameterException;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPLocalMsgsItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PubSubMessageItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPMessageProcessorControllable;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SITransaction;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.SelectorDomain;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionAlreadyExistsException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException;

/**
 * @author prmf
 *
 * <p>The AdministratorImpl class is responsible for the administration
 * interface to the Message Processor.</p>
 */
public final class AdministratorImpl implements Administrator
{
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceComponent tc =
    SibTr.register(
      AdministratorImpl.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
      
  private MessageProcessor messageProcessor;
  private DestinationManager destinationManager;
  
  // Indicates whether the ME is started or not
  private boolean started;
     
  /**
   * Constructor AdministratorImpl
   *
   * <p>Creates an administrator.</p>
   *
   * @param messageprocessor
   */
  public AdministratorImpl(MessageProcessor messageProcessor)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AdministratorImpl", messageProcessor);

    this.messageProcessor = messageProcessor;
    destinationManager = messageProcessor.getDestinationManager();
    started = true;

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AdministratorImpl", this);

  }

  /**
   * <p>Creates a durable subscription.</p>
   *
   * @param id   The subscription id
   * @param def  The subscription definition
   * @param tran The transaction context
   * @return
   * @throws SIDestinationAlreadyExistsException
   * @throws SIDestinationNotFoundException
   * @throws SIIncompatibleDestinationException
   * @throws SIResourceException
   * @throws SIMPNullParameterException
   * @throws SINotAuthorizedException
   */
  public void createSubscription(
    String id,
    SubscriptionDefinition subDef,
    SITransaction tran) 
    throws SINotPossibleInCurrentConfigurationException, SIIncorrectCallException, SIMPDestinationCorruptException, SIDurableSubscriptionAlreadyExistsException, SIDiscriminatorSyntaxException, SISelectorSyntaxException, SIResourceException
   {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createSubscription", new Object[] { id, subDef, tran });

    checkStarted();
     
    DestinationHandler dest;
    try
    {
      dest = destinationManager.getDestination(subDef.getDestination(), false);
    }
    catch (SITemporaryDestinationNotFoundException e)
    {
      // No FFDC code needed 
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createSubscription", "SINotPossibleInCurrentConfigurationException");
      throw new SINotPossibleInCurrentConfigurationException(e.getMessage());
    }

    // set up a SelectionCriteria
    SelectorDomain sDomain = SelectorDomain.getSelectorDomain(subDef.getSelectorDomain());
    
    SelectionCriteria criteria = messageProcessor.
                                   getSelectionCriteriaFactory().
                                   createSelectionCriteria(subDef.getTopic(),
                                                           subDef.getSelector(),
                                                           sDomain);    
    ConsumerDispatcherState subState =
      new ConsumerDispatcherState(
        id,
        dest.getUuid(),
        criteria,
        subDef.isNoLocal(),
        subDef.getDurableHome(),
        dest.getName(),
        dest.getBus());

    subState.setIsCloned(subDef.isSupportsMultipleConsumers());
    subState.setUser(subDef.getUser(),false);
	subState.setTargetDestination(subDef.getTargetDestination());
    
    if (!dest.isPubSub())
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "createSubscription", "SIIncorrectCallException");
      throw new SINotPossibleInCurrentConfigurationException(
        nls.getFormattedMessage(
          "DESTINATION_USEAGE_ERROR_CWSIP0141",
          new Object[]{dest.getName(),
                       messageProcessor.getMessagingEngineName(),
                       id},
          null));            
    }
   
    dest.createDurableSubscription(subState, (TransactionCommon) tran); 
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createSubscription");

  }

  /**
   * <p>Delete a subscription.</p>
   *
   * @param  name  The destination name
   * @param  force The force deletion switch
   * @return
   */
  public void deleteSubscription(
    String subId,
    boolean force) 
    
    throws SINotAuthorizedException, 
           SIDurableSubscriptionNotFoundException, 
           SIDestinationLockedException, 
           SIResourceException, 
           SIIncorrectCallException,
           SINotPossibleInCurrentConfigurationException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "deleteSubscription",
        new Object[] { subId, new Boolean(force) });

    checkStarted();
    
    if (subId == null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "deleteSubscription", "SubId is null");
      throw new SIDurableSubscriptionNotFoundException(
        nls.getFormattedMessage(
          "SUBSCRIPTION_DOESNT_EXIST_ERROR_CWSIP0072",
          new Object[] {null,
                        messageProcessor.getMessagingEngineName()},
          null));
    }

    HashMap durableSubs = destinationManager.getDurableSubscriptionsTable();    

    synchronized (durableSubs)
    {
      // Look up the consumer dispatcher for this subId in the system durable subs list
      ConsumerDispatcher cd = (ConsumerDispatcher) durableSubs.get(subId);
      
      // Does the subscription exist, if it doesn't, throw a 
      // SIDestinationNotFoundException
      if (cd == null)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
          SibTr.exit(tc, "deleteSubscription", "SIDurableSubscriptionNotFoundException");
          
        throw new SIDurableSubscriptionNotFoundException(          
          nls.getFormattedMessage(
            "SUBSCRIPTION_DOESNT_EXIST_ERROR_CWSIP0146",
            new Object[] { subId,
                           messageProcessor.getMessagingEngineName() },
          null));
      }        

      // Obtain the destination from the queueing points
      DestinationHandler destination = cd.getDestination();

      // Call the deleteDurableSubscription method on the destination
      // NOTE: this assumes the durable subscription is always local
      destination.deleteDurableSubscription(subId, messageProcessor.getMessagingEngineName());
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteSubscription");
  }

  /**
   * <p>Query a subscription.</p>
   *
   * @param  id   The subscription id
   * @return def  The subscription definition
   * @throws SIDestinationNotFoundException
   * @throws SIResourceException
   * @throws SIMPNullParameterException
   */
  public SubscriptionDefinition querySubscription(String subId) 
  throws SIDurableSubscriptionNotFoundException, SINotPossibleInCurrentConfigurationException, SIIncorrectCallException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "querySubscription", subId);

    checkStarted();
    
    if (subId == null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "querySubscription", "Exception SubId null");

      throw new SIIncorrectCallException(
        nls.getFormattedMessage(
          "SUBSCRIPTION_DOESNT_EXIST_ERROR_CWSIP0072",
          new Object[] { null,
                         messageProcessor.getMessagingEngineName() },
          null));
    }

    ConsumerDispatcherState subState = null;
    HashMap durableSubs = destinationManager.getDurableSubscriptionsTable();

    synchronized (durableSubs)
    {
      //Look up the consumer dispatcher for this subId in the system durable subs list
      ConsumerDispatcher cd = (ConsumerDispatcher) durableSubs.get(subId);

      if (cd == null)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "querySubscription", "Subscription not found");

        throw new SIDurableSubscriptionNotFoundException(
          nls.getFormattedMessage(
            "SUBSCRIPTION_DOESNT_EXIST_ERROR_CWSIP0072",
            new Object[] { subId,
                           messageProcessor.getMessagingEngineName() },
            null));
      }

      // Obtain the subscription state from the consumer dispatcher
      subState = cd.getConsumerDispatcherState();
    }
    
    DestinationHandler dest = null;
    try
    {    
      dest = 
        destinationManager.getDestination(subState.getTopicSpaceUuid(), true);
    }
    catch (SITemporaryDestinationNotFoundException e)
    {
      // No FFDC code needed 
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "querySubscription", "SINotPossibleInCurrentConfigurationException");
      throw new SINotPossibleInCurrentConfigurationException(e.getMessage());
    }

    // Create subscription definition
    SubscriptionDefinition subDef =
      new SubscriptionDefinitionImpl(
        dest.getName(),
        subState.getSelectionCriteria().getDiscriminator(),
        subState.getSelectionCriteria().getSelectorString(),
        subState.getSelectionCriteria().getSelectorDomain().toInt(),
        subState.getUser(),
        subState.isNoLocal(),
        subState.getDurableHome());

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "querySubscription", subDef);

    return subDef;
  }

  /**
   * <p>Gets list of all durable subscriptions.</p>
   *
   * @return list  The list of subscriptions
   * @throws SIResourceException
   *
   */
  public List getSubscriptionList()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getSubscriptionList");

    List list;
    HashMap durableSubs = destinationManager.getDurableSubscriptionsTable();

    synchronized (durableSubs)
    {
      list = new ArrayList(durableSubs.keySet());
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getSubscriptionList", list);

    return list;
  }

  /**
   * <p>Creates a subscription definition.</p>
   *
   * @return The subscription definition
   */
  public SubscriptionDefinition createSubscriptionDefinition()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createSubscriptionDefinition");

    SubscriptionDefinition subDef = new SubscriptionDefinitionImpl();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createSubscriptionDefinition", subDef);

    return subDef;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.Administrator#createDestinationLocalization(com.ibm.ws.sib.admin.DestinationDefinition, com.ibm.ws.sib.admin.LocalizationDefinition,  com.ibm.ws.sib.admin.MQLocalizationProxyDefinition)
   */
  public void createDestinationLocalization(
    DestinationDefinition destinationDefinition,
    LocalizationDefinition destinationLocalizationDefinition) 
    
    throws SIBExceptionDestinationNotFound, SIBExceptionBase, SIResourceException, SIMPDestinationAlreadyExistsException, SINotPossibleInCurrentConfigurationException                                         
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createDestinationLocalization", 
        new Object[] {destinationDefinition, 
                      destinationLocalizationDefinition});
  
    checkStarted();
    
    // Get locality set from admin of all MEs which host the queue point.
    // Venu mock mock
    // messageProcessor.getSIBDestinationLocalitySet now returns messageprocessor LocalitySet as
       /*
    Set destinationLocalizingSet = 
      messageProcessor.getSIBDestinationLocalitySet(
        null, destinationDefinition.getUUID().toString(), true); */

    Set destinationLocalizingSet = 
        messageProcessor.getSIBDestinationLocalitySet(
          null,null, true);
      
    
  
      
    destinationManager.createDestinationLocalization(destinationDefinition,
                                                     destinationLocalizationDefinition,                                                     
                                                     destinationLocalizingSet,                                                     
                                                     false);
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createDestinationLocalization");
                                                          
    return;                                                     
  }
  
  /**
   * Method only called by unit tests to create destinations
   */
  public void createDestinationLocalization(
      DestinationDefinition destinationDefinition,
      LocalizationDefinition destinationLocalizationDefinition,
      Set destinationLocalizingMEs,      
      boolean isTemporary) throws SIResourceException, SIMPDestinationAlreadyExistsException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createDestinationLocalization", new Object[] {
          destinationDefinition, destinationLocalizationDefinition,
          destinationLocalizingMEs,
          new Boolean(isTemporary) });
    
    destinationManager.createDestinationLocalization(destinationDefinition,
        destinationLocalizationDefinition,        
        destinationLocalizingMEs,
        false);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createDestinationLocalization");
  }
  
  public void alterDestinationLocalization(
      DestinationDefinition destinationDefinition,
      LocalizationDefinition destinationLocalizationDefinition)       
    throws SIBExceptionDestinationNotFound, SIBExceptionBase, SINotPossibleInCurrentConfigurationException, SIIncorrectCallException, SIResourceException
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.entry(tc, "alterDestinationLocalization", new Object[] {destinationLocalizationDefinition, destinationDefinition});
      
      checkStarted();
        
      //Get locality set from admin
      Set queuePointLocalizingSet = 
        messageProcessor.getSIBDestinationLocalitySet(
          null, destinationDefinition.getUUID().toString(), true );
        
        
      destinationManager.alterDestinationLocalization(destinationDefinition,destinationLocalizationDefinition,queuePointLocalizingSet);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "alterDestinationLocalization");
                                                            
      return;                                                     
    }
  
  public void deleteDestinationLocalization(String destinationUuid
                                            ,DestinationDefinition destinationDefinition
                                            ) 
  throws SIBExceptionDestinationNotFound, SIBExceptionBase, SITemporaryDestinationNotFoundException, SINotPossibleInCurrentConfigurationException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "deleteDestinationLocalization", new Object[] { destinationUuid });
     
    checkStarted();
      
    Set queuePointLocalizingSet = null;    
    
    if (destinationDefinition != null)
    {
      //Get locality set from admin
      queuePointLocalizingSet = messageProcessor.getSIBDestinationLocalitySet(null, destinationDefinition.getUUID().toString(), true);      
    }
      
    destinationManager.deleteDestinationLocalization(destinationUuid
                                                    ,destinationDefinition
                                                    ,queuePointLocalizingSet);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteDestinationLocalization");
                                                          
    return;                                                     
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.Administrator#getDestinationLocalizationDefinition(java.lang.String)
   */
  public LocalizationDefinition getDestinationLocalizationDefinition(String destinationUuid) throws SINotPossibleInCurrentConfigurationException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getDestinationLocalizationDefinition", destinationUuid);

    checkStarted();
    
    LocalizationDefinition localizationDefinition = null;
    DestinationHandler destinationHandler = null;

    try
    {
      destinationHandler = destinationManager.getDestination(new SIBUuid12(destinationUuid), false);
    }
    catch (SITemporaryDestinationNotFoundException e)
    {
      // No FFDC code needed 
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "getDestinationLocalizationDefinition", "SINotPossibleInCurrentConfigurationException");
      throw new SINotPossibleInCurrentConfigurationException(e.getMessage());
    }
    
    if(destinationHandler.isPubSub())
    {
      PubSubMessageItemStream pubSubMessageItemStream = destinationHandler.getPublishPoint();
      if (pubSubMessageItemStream != null)
      {
        localizationDefinition = pubSubMessageItemStream.getLocalizationDefinition();
      }
    }    
    else
    {
      PtoPLocalMsgsItemStream ptoPLocalMsgsItemStream = null;
      
      if ((!destinationHandler.isAlias()) && (!destinationHandler.isForeign()))
      {
        ptoPLocalMsgsItemStream = (PtoPLocalMsgsItemStream) destinationHandler.getQueuePoint(messageProcessor.getMessagingEngineUuid());
        if (ptoPLocalMsgsItemStream != null)
        {
          localizationDefinition = ptoPLocalMsgsItemStream.getLocalizationDefinition();
        }
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getDestinationLocalizationDefinition", localizationDefinition);
     
    //TODO - return clone.  Cant do till admin implement clone() method (168499.21)
      
    return localizationDefinition;
  }
        
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.Administrator#initiateCleanUp(java.lang.String)
   */
  public void initiateCleanUp(String destinationUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "initiateCleanUp", destinationUuid);

    DestinationHandler destinationHandler = null;

    try
    {
      destinationHandler = destinationManager.getDestination(new SIBUuid12(destinationUuid), true);
    }
    catch (SINotPossibleInCurrentConfigurationException e)
    {
      // No FFDC code needed
      //do nothing            
    } 
    catch (SITemporaryDestinationNotFoundException e)
    {
      // No FFDC code needed 
      //do nothing            
    }   
    
    if (destinationHandler != null)
    {
      destinationManager.markDestinationAsCleanUpPending(destinationHandler);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "initiateCleanUp");
      
    return;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.Administrator#createMQLink(com.ibm.ws.sib.admin.DestinationDefinition, java.lang.String, com.ibm.ws.sib.admin.LocalizationDefinition)
   */
  public void createMQLink(
    VirtualLinkDefinition vld, 
    MQLinkDefinition mqld,
    LocalizationDefinition ld) 
    throws SIIncorrectCallException, SIMPDestinationAlreadyExistsException, SIResourceException, SIException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createMQLink", new Object[] {vld, mqld, ld});

    checkStarted();
    
    Set localizingMEs = vld.getLinkLocalitySet();
    destinationManager.createMQLinkLocalization(mqld, ld, vld, localizingMEs);
 
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createMQLink");

    return;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.Administrator#createGatewayLink(com.ibm.ws.sib.admin.DestinationDefinition, java.lang.String)
   */
  public void createGatewayLink(
    VirtualLinkDefinition vld,
    String uuid) throws SIResourceException, SIMPDestinationAlreadyExistsException, SIIncorrectCallException, SIException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createGatewayLink", new Object[] {vld, uuid});
    
    checkStarted();
      
    destinationManager.createLinkLocalization(vld);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createGatewayLink");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.Administrator#localDestinationExists(java.lang.String)
   */
  public boolean destinationExists(String destinationName)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "destinationExists", destinationName);

    boolean returnVal = false;
    
    try
    {
      returnVal = 
        destinationManager.destinationExists(destinationName, 
                                             messageProcessor.getMessagingEngineBus());
    }
    catch (SIMPNullParameterException e)
    {
      // No FFDC code needed
    }
     
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "destinationExists", new Boolean(returnVal));
      
    return returnVal;
  }  
  
  public SIMPMessageProcessorControllable getMPRuntimeControl()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getMPRuntimeControl");

    SIMPMessageProcessorControllable control =
      (SIMPMessageProcessorControllable)messageProcessor.getControlAdapter();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMPRuntimeControl", control);

    return control;
  }
  
  public boolean isStarted()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isStarted");
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isStarted", Boolean.valueOf(started));
    return started;
  }
  
  public void stop()
  { 
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "stop");
    
    started = false;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "stop");
  }
  
  /** 
   * Checks that the ME is started
   * @throws SINotSupportedException if the ME isn't started
   * 
   */
  private void checkStarted() throws SINotPossibleInCurrentConfigurationException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "checkStarted");

    synchronized (this)
    {
      if (!isStarted())
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "checkStarted", "ME not started");

        throw new SINotPossibleInCurrentConfigurationException(
          nls.getFormattedMessage("MESSAGE_PROCESSOR_NOT_STARTED_ERROR_CWSIP0211", 
          new Object[] { messageProcessor.getMessagingEngineName(), messageProcessor.getMessagingEngineBus()}, 
          null));
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "checkStarted");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.Administrator#deleteGatewayLink(java.lang.String)
   */
  public void deleteGatewayLink(String linkUuid) 
    throws SINotPossibleInCurrentConfigurationException, 
           SIResourceException, 
           SIConnectionLostException, 
           SIException, 
           SIBExceptionBase 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "deleteGatewayLink", linkUuid);    
    destinationManager.deleteLinkLocalization(new SIBUuid12(linkUuid));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteGatewayLink");    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.Administrator#deleteMQLink(java.lang.String)
   */
  public void deleteMQLink(String mqLinkUuid) 
    throws SINotPossibleInCurrentConfigurationException, 
           SIResourceException, 
           SIConnectionLostException, 
           SIException, 
           SIBExceptionBase 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "deleteMQLink", mqLinkUuid);    
    destinationManager.deleteMQLinkLocalization(new SIBUuid8(mqLinkUuid));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "deleteMQLink");  
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.Administrator#alterMQLink(com.ibm.ws.sib.admin.VirtualLinkDefinition, java.lang.String, com.ibm.ws.sib.admin.LocalizationDefinition)
   */
  public void alterMQLink(VirtualLinkDefinition vld,
                          MQLinkDefinition mqld, 
                          LocalizationDefinition ld) 
  throws SINotPossibleInCurrentConfigurationException, 
         SIResourceException, 
         SIConnectionLostException, 
         SIException, 
         SIBExceptionBase 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "alterMQLink", new Object[] {vld, mqld, ld});    
    destinationManager.alterMQLinkLocalization(mqld, ld, vld);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "alterMQLink");  
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.Administrator#alterGatewayLink(com.ibm.ws.sib.admin.VirtualLinkDefinition, java.lang.String)
   */
  public void alterGatewayLink(VirtualLinkDefinition vld, 
                               String linkUuid) 
  throws SINotPossibleInCurrentConfigurationException, 
         SIResourceException, 
         SIConnectionLostException, 
         SIException, 
         SIBExceptionBase 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "alterGatewayLink", new Object[] {vld, linkUuid});    
    destinationManager.alterLinkLocalization(vld, new SIBUuid12(linkUuid));
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "alterGatewayLink");   
  }

@Override
public void alterDestinationAlias(
		DestinationAliasDefinition destinationAliasDefinition)
		throws SINotPossibleInCurrentConfigurationException,
		SIIncorrectCallException, SIResourceException,
		SIConnectionLostException, SIException, SIBExceptionBase		
		{
	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
	      SibTr.entry(tc, "alterDestinationAlias", new Object[] {destinationAliasDefinition.getName(), destinationAliasDefinition.getUUID()});   
	destinationManager.alterDestinationAlias(destinationAliasDefinition);	
	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
	      SibTr.exit(tc, "alterDestinationAlias= "+destinationAliasDefinition.getName());
	
		} 

}
