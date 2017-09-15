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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.mfp.control.ControlCardinalityInfo;
import com.ibm.ws.sib.mfp.control.ControlCreateStream;
import com.ibm.ws.sib.mfp.control.ControlMessage;
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.control.ControlDurableConfirm;
import com.ibm.ws.sib.mfp.control.ControlCreateDurable;
import com.ibm.ws.sib.mfp.control.ControlDeleteDurable;
import com.ibm.ws.sib.processor.MPSelectionCriteriaFactory;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.gd.GDConfig;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidOperationException;
import com.ibm.ws.sib.processor.impl.interfaces.ControlHandler;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.SelectorDomain;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

import java.util.HashMap;
import java.util.Map;

/**
 * This output handler is used to implement the DME side of durable
 * subscription creation, deletion and initial connection establishment.
 * The anycast protocol is used retrieve messages from a remote durable
 * subscription.  As a result, the DurableOutputHandler ONLY processes
 * control messages.
 */
public class DurableOutputHandler 
  implements ControlHandler, DurableConstants {

  // Standard debug/trace
  private static final TraceComponent tc =
        SibTr.register(
          DurableOutputHandler.class,
          SIMPConstants.MP_TRACE_GROUP,
          SIMPConstants.RESOURCE_BUNDLE);
     
  // NLS for component
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
 
  // No instantiation please
  private DurableOutputHandler() 
  {
  }
  
  /////////////////////////////////////////////////////////////
  // ControlHandler methods
  /////////////////////////////////////////////////////////////

  /**
   * NOP for durable handlers
   */
  public void handleControlMessage(SIBUuid8 sourceMEUuid, ControlMessage cMsg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "handleControlMessage", new Object[]{sourceMEUuid, cMsg});
      
    InvalidOperationException e = new InvalidOperationException(
      nls.getFormattedMessage(
        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.DurableOutputHandler",
          "1:112:1.45.1.1" },
        null));
    
    // FFDC
    FFDCFilter.processException(
      e,
      "com.ibm.ws.sib.processor.impl.DurableOutputHandler.handleControlMessage",
      "1:119:1.45.1.1",
      this);
      
     SibTr.exception(tc, e);
     
     SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.DurableOutputHandler",
          "1:127:1.45.1.1" });
     
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "handleControlMessage", e);
    throw e;
  }

  /**
   * This is the only message handling method which should be invoked
   * on the DurableOutputHandler.  This method will receive control messages
   * giving the status of durable subcription creation or deletion, as well
   * as stream creation requests.
   * 
   * @param sourceCellue The origin of the message.
   * @param cMsg The ControlMessage to process.
   */
  public static void staticHandleControlMessage(
    ControlMessage cMsg, 
    DestinationManager DM,
    MessageProcessor MP)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "staticHandleControlMessage", 
        new Object[]{cMsg, DM, MP});

    // This should be one of three requests:
    // 1) Request to create a durable subscription
    // 2) Request to delete a durable subscription
    // 3) Request to attach to an existing durable subscription
    // anything else causes an exception
    
    ControlMessageType type = cMsg.getControlMessageType();

    if (type == ControlMessageType.CREATEDURABLE)
      handleCreateDurable(DM, (ControlCreateDurable) cMsg, MP);
    else if (type == ControlMessageType.DELETEDURABLE)
      handleDeleteDurable(DM, (ControlDeleteDurable) cMsg, MP);
    else if (type == ControlMessageType.CREATESTREAM)
      handleCreateStream(DM, (ControlCreateStream) cMsg, MP);
    else
    {
      // unknown type, log error
      SIErrorException e = new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.DurableOutputHandler",
            "1:173:1.45.1.1" },
          null));
      
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.DurableOutputHandler.staticHandleControlMessage",
        "1:180:1.45.1.1",
        DurableOutputHandler.class);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.DurableOutputHandler",
          "1:187:1.45.1.1" });
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "staticHandleControlMessage", e);
      
      throw e;
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "staticHandleControlMessage");
  }

  /**
   * Attempt to create a new durable subscription and send back either a
   * ControlDurableConfirm giving the result.
   * 
   * @param msg The ControlCreateDurable request message.
   */  
  protected static void handleCreateDurable(
    DestinationManager DM, 
    ControlCreateDurable msg,
    MessageProcessor MP)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "handleCreateDurable", new Object[] {DM, msg, MP});

    int       status        = STATUS_SUB_GENERAL_ERROR;
    SIBUuid12 handlerID     = msg.getGuaranteedTargetDestinationDefinitionUUID(); // create requests require a target dest ID
    String    subName       = msg.getDurableSubName();
    String    discriminator = msg.getDurableDiscriminator();
    String    selectorString      = msg.getDurableSelector();
    SelectorDomain selectorDomain = SelectorDomain.
                                      getSelectorDomain(msg.getDurableSelectorDomain());
    
    // Create a selectorProperties Map to convey any additional properties associated
    // with the selector. At present (26/03/08) there is only one additional property
    // which is itself a map (of name spaces). The name space map is used in the XPath10 selector domain
    // to map URLs to prefixes. The use of a selectorProperties map keeps the Core SPI generic but
    // when conveying information over JMF we need a simpler structure and so will need to
    // break out individual properties for transportation.
    Map<String, Object> selectorProperties = null;
    Map<String, String> selectorNamespaceMap = msg.getDurableSelectorNamespaceMap();
    if(selectorNamespaceMap != null)
    {
      selectorProperties = new HashMap<String, Object>();
      selectorProperties.put("namespacePrefixMappings", selectorNamespaceMap);
    }

    String    user          = msg.getSecurityUserid();
    //TODO this flag needs to be set from the message.
    boolean   isSIBServerSubject = msg.isSecurityUseridSentBySystem();
    
    try {
      // Resolve the target BaseDestinationHandler
      BaseDestinationHandler handler = (BaseDestinationHandler) DM.getDestination(handlerID, false);
      
      // We'll create SelectionCriteria based on the properties we've been passed.
      SelectionCriteria criteria = null;
      
      // Use the appropriate SelectionCriteria factory dependent on whether we've been passed
      // selectorProperties
      if(selectorProperties == null)
      {
        criteria = MP.getSelectionCriteriaFactory().
                     createSelectionCriteria(discriminator,
                                             selectorString, 
                                             selectorDomain);
      }
      else
      {
        // Non-null selectorProperties, so we create MPSelectionCriteria
        criteria = 
          MPSelectionCriteriaFactory.
            getInstance().
              createSelectionCriteria(discriminator,
                                      selectorString, 
                                      selectorDomain,
                                      selectorProperties);
      }
      
      // Ask the BaseDestinationHandler to attempt the create
      // then we send back the result.
      status = handler.createDurableFromRemote(subName, 
                                               criteria,
                                               user, 
                                               msg.isCloned(),
                                               msg.isNoLocal(),
                                               isSIBServerSubject);
    }
    catch (Exception e)
    {
      // Log the exception
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.DurableOutputHandler.handleCreateDurable",
        "1:281:1.45.1.1",
        DurableOutputHandler.class);

      SibTr.exception(tc, e);
    }
    
    // Forward the status to the caller and we're done
    SIBUuid8              sender = msg.getGuaranteedSourceMessagingEngineUUID(); 
    ControlDurableConfirm reply  = createDurableConfirm(MP, sender, msg.getRequestID(), status);
    MP.getMPIO().sendToMe(sender, SIMPConstants.CONTROL_MESSAGE_PRIORITY, reply);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleCreateDurable");
  }
  
  /**
   * Attempt to delete the durable subscription specified by the sender
   * and send back a ControlDurableConfirm giving the result.
   * 
   * @param msg The ControlDeleteDurable request message.
   */
  protected static void handleDeleteDurable(
    DestinationManager DM,
    ControlDeleteDurable msg,
    MessageProcessor MP)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "handleDeleteDurable", new Object[] {DM, msg, MP});

    int status = STATUS_SUB_GENERAL_ERROR;
    
    try 
    {
      // Track down a handler for this durable sub, error if we can't find one
      String  subName     = msg.getDurableSubName();
      HashMap durableSubs = DM.getDurableSubscriptionsTable();

      synchronized (durableSubs)
      {
        //Look up the consumer dispatcher for this subId in the system durable subs list
        ConsumerDispatcher cd =
          (ConsumerDispatcher) durableSubs.get(subName);
          
        // Check that the durable subscription existed
        if (cd == null) 
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
            SibTr.exit(tc, "handleDeleteDurable", "SIDurableSubscriptionNotFoundException");
          
          status = DurableConstants.STATUS_SUB_NOT_FOUND;
          
          throw new SIDurableSubscriptionNotFoundException(
            nls.getFormattedMessage(
              "SUBSCRIPTION_DOESNT_EXIST_ERROR_CWSIP0072",
              new Object[] { subName,                
                             MP.getMessagingEngineName() },
              null));
        }
          
        // Obtain the destination from the consumer dispatcher
        // A cast error here will result in an error sent back to the caller
        BaseDestinationHandler handler = cd.getDestination();

        boolean userMatch = true;
        // If security is disabled then we'll bypass the check
        if(MP.isBusSecure())
        {                
          // Check that the user who is attempting to delete this durable subscription
          // matches that set in the CD state when the subscription was created
        
          ConsumerDispatcherState subState = cd.getConsumerDispatcherState();
          String theUser = msg.getSecurityUserid(); 
          if(theUser == null)
          {
            if(subState.getUser() != null)
              userMatch = false;
          }
          else
          {
            if (!theUser.equals(subState.getUser()))
              userMatch = false;  
          }
        }  
        // Handle the case where the users do not match
        if (!userMatch)
        {  
          status = DurableConstants.STATUS_NOT_AUTH_ERROR;
        }        
        else
        {
          status = handler.deleteDurableFromRemote(subName);
        }
      }
    }
    catch (SIDurableSubscriptionNotFoundException e) 
    {
       // No FFDC code needed
       SibTr.exception(tc, e);
    }
    catch (Exception e)
    {
      // FFDC
      // Any other exception is unexpected
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.DurableOutputHandler.handleDeleteDurable",
        "1:387:1.45.1.1",
        DurableOutputHandler.class);
    }
    
    // Forward the status to the caller and we're done
    SIBUuid8              sender = msg.getGuaranteedSourceMessagingEngineUUID(); 
    ControlDurableConfirm reply  = createDurableConfirm(MP, sender, msg.getRequestID(), status);
    MP.getMPIO().sendToMe(sender, SIMPConstants.CONTROL_MESSAGE_PRIORITY, reply);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleDeleteDurable");
  }

  /**
   * Attempt to attach to an existing durable subscription.  The reply will be
   * one of: 1) ControlNotFlushed (successful attachment), 2) ControlCardinalityInfo
   * (durable sub is locked), or 3) ControlDurableConfirm (either a general
   * error or some specific error such as the durable subscription does not
   * exist).
   * 
   * @param msg The ControlCreateStream request message.
   */
  protected static void handleCreateStream(
    DestinationManager DM,
    ControlCreateStream msg,
    MessageProcessor MP)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "handleCreateStream", new Object[] {DM, msg, MP});

    ControlMessage reply = null;

    try
    {
      // Resolve the handler and attempt to attach to an 
      // existing local durable susbcription.
      SIBUuid12 handlerID     = msg.getGuaranteedTargetDestinationDefinitionUUID(); // attach requests require a target dest ID
      
      // Resolve the target BaseDestinationHandler
      BaseDestinationHandler handler = (BaseDestinationHandler) DM.getDestination(handlerID, false);

      handler.attachDurableFromRemote(msg);
    } 
    catch(SIDestinationLockedException e)
    {
      // No FFDC code needed
      // Cardinality error
      reply = createCardinalityInfo(MP, msg.getGuaranteedSourceMessagingEngineUUID(),msg.getRequestID(),1);
    }
    catch(Exception e)
    {
      // No FFDC code needed
      int      status = DurableConstants.STATUS_SUB_GENERAL_ERROR;
      SIBUuid8 sender = msg.getGuaranteedSourceMessagingEngineUUID(); 
      
      if (e instanceof SIDurableSubscriptionNotFoundException)
        status = DurableConstants.STATUS_SUB_NOT_FOUND;
      else if (e instanceof SIDurableSubscriptionMismatchException)
        status = DurableConstants.STATUS_SUB_MISMATCH_ERROR;       
      else if (e instanceof SINotAuthorizedException)
        status = DurableConstants.STATUS_NOT_AUTH_ERROR;
      else
      {
        // Anything else is logged and becomes a general error
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.DurableOutputHandler.handleCreateStream",
          "1:454:1.45.1.1",
          DurableOutputHandler.class);
        SibTr.exception(tc, e);
      }
            
      reply = createDurableConfirm(MP, sender,msg.getRequestID(),status);
    }

    // Only send a reply on error conditions
    if (reply != null)
    {
      SIBUuid8 sender = msg.getGuaranteedSourceMessagingEngineUUID(); 
      MP.getMPIO().sendToMe(sender, SIMPConstants.CONTROL_MESSAGE_PRIORITY, reply);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleCreateStream");
  }
  
  ////////////////////////////////////////////////
  // Methods to build messages
  ////////////////////////////////////////////////
  
  /**
   * Create a DurableConfirm reply.
   * 
   * @param target The target ME for the message.
   * @param reqID The request ID of the original request message.
   * @param status The status to record in this reply.
   */
  protected static ControlDurableConfirm createDurableConfirm(
    MessageProcessor MP,
    SIBUuid8 target,
    long reqID,
    int status)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createDurableConfirm", new Object[] {MP, target, new Long(reqID), new Integer(status)});
    
    ControlDurableConfirm msg = null;
         
    try
    {
      // Create and initialize the message
      msg = MessageProcessor.getControlMessageFactory().createNewControlDurableConfirm();
      initializeControlMessage(MP.getMessagingEngineUuid(), msg, target);
      
      // Parameterize for CreateStream
      msg.setRequestID(reqID);
      msg.setStatus(status);
    }
    catch (Exception e)
    {
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.DurableOutputHandler.createDurableConfirm",
        "1:509:1.45.1.1",
        DurableOutputHandler.class);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createDurableConfirm", msg);
      
    return msg;
  }

  /**
   * Create a CardinalityInfo reply.
   * 
   * @param target The target ME for this reply.
   * @param reqID The request ID of the original request message.
   * @param card The cardinality to record on this message.
   */
  protected static ControlCardinalityInfo createCardinalityInfo(
    MessageProcessor MP,
    SIBUuid8 target,
    long reqID,
    int card)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createCardinalityInfo", new Object[] {MP, target, new Long(reqID), new Integer(card)});
    
    ControlCardinalityInfo msg = null;
         
    try
    {
      // Create and initialize the message
      msg = MessageProcessor.getControlMessageFactory().createNewControlCardinalityInfo();
      initializeControlMessage(MP.getMessagingEngineUuid(), msg, target);
      
      // Parameterize for CreateStream
      msg.setRequestID(reqID);
      msg.setCardinality(card);
    }
    catch (Exception e)
    {
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.DurableOutputHandler.createCardinalityInfo",
        "1:552:1.45.1.1",
        DurableOutputHandler.class);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createCardinalityInfo", msg);
      
    return msg;
  }
  
  /**
   * Common initialization for all messages sent by the DurableOutputHandler.
   * 
   * @param msg Message to initialize.
   * @param remoteMEId The ME the message will be sent to.
   */
  protected static void initializeControlMessage(SIBUuid8 sourceME, ControlMessage msg, SIBUuid8 remoteMEId)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "initializeControlMessage", new Object[] {msg, remoteMEId});

    SIMPUtils.setGuaranteedDeliveryProperties(msg,
        sourceME, 
        remoteMEId,
        null,
        null,
        null,
        ProtocolType.DURABLEINPUT,
        GDConfig.PROTOCOL_VERSION); 
           
    msg.setPriority(SIMPConstants.CONTROL_MESSAGE_PRIORITY);
    msg.setReliability(SIMPConstants.CONTROL_MESSAGE_RELIABILITY);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "initializeControlMessage");
  }

  public long handleControlMessageWithReturnValue(SIBUuid8 sourceMEUuid,
		ControlMessage cMsg) throws SIIncorrectCallException,
		SIResourceException, SIConnectionLostException, SIRollbackException {
    return 0;
  }
  
}
