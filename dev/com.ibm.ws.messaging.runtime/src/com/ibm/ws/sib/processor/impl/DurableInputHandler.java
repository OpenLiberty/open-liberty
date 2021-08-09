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

import java.util.HashMap;
import java.util.Map;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.mfp.control.ControlCardinalityInfo;
import com.ibm.ws.sib.mfp.control.ControlCreateDurable;
import com.ibm.ws.sib.mfp.control.ControlCreateStream;
import com.ibm.ws.sib.mfp.control.ControlDeleteDurable;
import com.ibm.ws.sib.mfp.control.ControlDurableConfirm;
import com.ibm.ws.sib.mfp.control.ControlMessage;
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.control.ControlNotFlushed;
import com.ibm.ws.sib.processor.MPSelectionCriteria;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.gd.GDConfig;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidOperationException;
import com.ibm.ws.sib.processor.impl.interfaces.ControlHandler;
import com.ibm.ws.sib.processor.impl.interfaces.InputHandler;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.SelectorDomain;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionAlreadyExistsException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * This input handler is used to implement the RME side of durable
 * subscription creation, deletion, and initial connection establishment.
 * The anycast protocol is used retrieve messages from a remote durable
 * subscription.  As a result, the DurableInputHandler ONLY processes
 * control messages.
 */
public class DurableInputHandler 
  implements InputHandler, 
             ControlHandler,
             DurableConstants
{
  // Standard debug/trace
  private static final TraceComponent tc =
        SibTr.register(
          DurableInputHandler.class,
          SIMPConstants.MP_TRACE_GROUP,
          SIMPConstants.RESOURCE_BUNDLE);

  // Request IDs are stored here for coordination with replies
  private static Map<Long, Object[]> _requestMap = new HashMap<Long, Object[]>();
  
  // Inner class instance for handling alarms
  private static AlarmListener _alarmHandler = new AlarmListener() 
  {
    public void alarm(Object arg)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "alarm", arg);
      internalAlarmHandler(arg);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "alarm");
    }
  };
  
  // NLS for component
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  // No instantiation please
  private DurableInputHandler() 
  {
  }
  
  ////////////////////////////////////////////////////////////////////////
  // InputHandler methods
  ////////////////////////////////////////////////////////////////////////

  /**
   * This method is a NOP for durable handlers.
   *
   * @param msg ignored
   * @param transaction ignored
   * @param producerSession ignored
   * @param sourceCellule ignored
   * @param targetCellule ignored
   */
  public void handleMessage(
    MessageItem msg,
    TransactionCommon transaction,
    SIBUuid8 sourceMEUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "handleMessage", new Object[]{msg, transaction, sourceMEUuid});
      
    SIErrorException e = new SIErrorException(
      nls.getFormattedMessage(
        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.DurableInputHandler",
          "1:143:1.52.1.1" },
        null));
    
    // FFDC
    FFDCFilter.processException(
      e,
      "com.ibm.ws.sib.processor.impl.DurableInputHandler.handleMessage",
      "1:150:1.52.1.1",
      this);    
   
    SibTr.exception(tc, e);
    
    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
      new Object[] {
        "com.ibm.ws.sib.processor.impl.DurableInputHandler",
        "1:158:1.52.1.1" });

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "handleMessage", e); 
       
    throw e;
  }

  /////////////////////////////////////////////////////////////
  // ControlHandler methods
  /////////////////////////////////////////////////////////////

  /**
   * NOP for the durable handlers.
   */
  public void handleControlMessage(SIBUuid8 sourceMEUuid, ControlMessage cMsg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "handleControlMessage", new Object[]{sourceMEUuid, cMsg});
      
    InvalidOperationException e = new InvalidOperationException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.DurableInputHandler",
            "1:183:1.52.1.1" },
          null));
    
    // FFDC
    FFDCFilter.processException(
      e,
      "com.ibm.ws.sib.processor.impl.DurableInputHandler.handleControlMessage",
      "1:190:1.52.1.1",
      this);
      
    SibTr.exception(tc, e);
    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
      new Object[] {
        "com.ibm.ws.sib.processor.impl.DurableInputHandler",
        "1:197:1.52.1.1" });
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "handleControlMessage", e);
    throw e;
  }
  
  /**
   * This is the only message handling method which should be invoked
   * on the DurableInputHandler.  This method will receive control messages
   * giving the status of durable subcription creation or deletion, as well
   * as stream creation requests.
   * 
   * @param sourceCellue The origin of the message.
   * @param cMsg The ControlMessage to process.
   */
  public static void staticHandleControlMessage(ControlMessage cMsg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "staticHandleControlMessage",
                new Object[] {cMsg});

    ControlMessageType type = cMsg.getControlMessageType();

    if ((type == ControlMessageType.NOTFLUSHED) ||
        (type == ControlMessageType.CARDINALITYINFO) ||
        (type == ControlMessageType.DURABLECONFIRM))
    {
      // See if the request ID is pending and wakeup the waiter
      long reqID = 0;
      if (cMsg instanceof ControlNotFlushed)
        reqID = ((ControlNotFlushed) cMsg).getRequestID();
      else if (cMsg instanceof ControlCardinalityInfo)
        reqID = ((ControlCardinalityInfo) cMsg).getRequestID();
      else if (cMsg instanceof ControlDurableConfirm)
        reqID = ((ControlDurableConfirm) cMsg).getRequestID();
      
      // Now wakeup any waiters.  If this is a stale reply, then it's ignored.  
      wakeupWaiter(reqID, cMsg);
    }
    else
    {
      // unknown type, log error
      SIErrorException e = new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.DurableInputHandler",
            "1:245:1.52.1.1" },
          null));
      
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.DurableInputHandler.staticHandleControlMessage",
        "1:252:1.52.1.1",
        DurableInputHandler.class);
        
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.DurableInputHandler",
          "1:258:1.52.1.1" });

      SibTr.exception(tc, e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "staticHandleControlMessage");
  }

  /**
   * Attempt to wake up a blocked thread waiting for a request reply.
   * 
   * @param reqID The ID of the request for which a reply was received.
   * @param result The reply message.
   */
  protected static void wakeupWaiter(long reqID, Object result)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "wakeupWaiter", new Object[] {new Long(reqID), result});
      
    synchronized (_requestMap)
    {
      Long     key    = new Long(reqID);
      Object[] waiter = _requestMap.get(key);
      
      if (waiter != null)
      {
        // Waiting request, wake up
        waiter[0] = result;
        _requestMap.remove(key);
        synchronized (waiter)
        {
          waiter.notify();
        }
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "wakeupWaiter");
  }
  
  /**
   * Create a CreateStream request for an existing durable connection.
   * 
   * @param subState The state describing the subscription we are attaching to.
   * @param reqID The request ID to be used for the request.
   * @param dme The dme to which the request should be sent
   */
  protected static ControlCreateStream createDurableCreateStream(
    MessageProcessor MP,
    ConsumerDispatcherState subState,
    long reqID,
    SIBUuid8 dme) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createDurableCreateStream", new Object[] {MP, subState, new Long(reqID), dme});
    
    ControlCreateStream msg = null;
         
    try
    {

      // Create and initialize the message
      msg = MessageProcessor.getControlMessageFactory().createNewControlCreateStream();
      initializeControlMessage(MP.getMessagingEngineUuid(), msg, dme);
      
      // Parameterize for CreateStream
      msg.setRequestID(reqID);
      msg.setDurableSubName(subState.getSubscriberID());
      msg.setGuaranteedTargetDestinationDefinitionUUID(subState.getTopicSpaceUuid());
      
      SelectionCriteria criteria = subState.getSelectionCriteria();
      //check for null values for MFP - defect 251989
      //the discriminator
      if(criteria==null || criteria.getDiscriminator()==null)
      {
        msg.setDurableDiscriminator(null); 
      } 
      else
      {
        msg.setDurableDiscriminator(criteria.getDiscriminator()); 
      } 
      //the selector
      if(criteria==null || criteria.getSelectorString()==null)
      {
        msg.setDurableSelector(null); 
      } 
      else
      {
        msg.setDurableSelector(subState.getSelectionCriteria().getSelectorString());
      } 
      //the selector domain
      if(criteria==null || criteria.getSelectorDomain()==null)
      {
        msg.setDurableSelectorDomain(SelectorDomain.SIMESSAGE.toInt());
      }  
      else
      {
        msg.setDurableSelectorDomain(criteria.getSelectorDomain().toInt());
      }
      
      //defect 259036
      msg.setCloned(subState.isCloned());
      msg.setNoLocal(subState.isNoLocal());
            
      msg.setSecurityUserid(subState.getUser());
      // Set the flag that signals whether this is
      // the privileged SIBServerSubject.
      msg.setSecurityUseridSentBySystem(subState.isSIBServerSubject());
    }
    catch (Exception e)
    {
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.DurableInputHandler.createDurableCreateStream",
        "1:372:1.52.1.1",
        DurableInputHandler.class);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createDurableCreateStream", msg);
      
    return msg;
  }

  /**
   * Create a CreateDurable request for a new durable connection.
   * 
   * @param subState The state describing the subscription to be created.
   * @param reqID The request ID to be used for the request.
   * @param dme The dme to which the request should be sent
   */
  protected static ControlCreateDurable createDurableCreateDurable(
    MessageProcessor MP,
    ConsumerDispatcherState subState,
    long reqID,
    SIBUuid8 dme) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createDurableCreateDurable", new Object[] {MP, subState, new Long(reqID), dme});

    ControlCreateDurable msg = null;
      
    try
    {
      // Create and initialize the message
      msg = MessageProcessor.getControlMessageFactory().createNewControlCreateDurable();
      initializeControlMessage(MP.getMessagingEngineUuid(), msg, dme);
      
      // Parameterize for CreateStream
      msg.setRequestID(reqID);
      msg.setDurableSubName(subState.getSubscriberID());
      
      SelectionCriteria criteria = subState.getSelectionCriteria();
      //check for null values for MFP - discriminator can be null
      //the discriminator
      
      if(criteria==null || criteria.getDiscriminator()==null)
      {
        msg.setDurableDiscriminator(null); 
      } 
      else
      {
        msg.setDurableDiscriminator(criteria.getDiscriminator()); 
      } 
      //the selector
      if(criteria==null || criteria.getSelectorString()==null)
      {
        msg.setDurableSelector(null); 
      } 
      else
      {
        msg.setDurableSelector(subState.getSelectionCriteria().getSelectorString());
      } 
      //the selector domain
      if(criteria==null || criteria.getSelectorDomain()==null)
      {
        msg.setDurableSelectorDomain(SelectorDomain.SIMESSAGE.toInt());
      }  
      else
      {
        msg.setDurableSelectorDomain(criteria.getSelectorDomain().toInt());
      }
      
      // Check the selectorProperties Map to see if we need to convey any additional properties associated
      // with the selector. At present (26/03/08) there is only one additional property
      // which is itself a map (of name spaces). The name space map is used in the XPath10 selector domain
      // to map URLs to prefixes. The use of a selectorProperties map keeps the Core SPI generic but
      // when conveying information over JMF we need a simpler structure and so will need to
      // break out individual properties for transportation.
      if(criteria==null)
      {
        msg.setDurableSelectorNamespaceMap(null);
      }  
      else
      {
        // See if these criteria have any selector properties. They might if they are MPSelectionCriteria
        if(criteria instanceof MPSelectionCriteria)
        {
          MPSelectionCriteria mpCriteria = (MPSelectionCriteria)criteria;
          Map<String, Object> selectorProperties = mpCriteria.getSelectorProperties();
          
          if(selectorProperties != null)
          {
            Map<String, String> selectorNamespaceMap = 
              (Map<String, String>)selectorProperties.get("namespacePrefixMappings");

            if(selectorNamespaceMap != null)
              msg.setDurableSelectorNamespaceMap(selectorNamespaceMap);
            else
              msg.setDurableSelectorNamespaceMap(null);
          }
          else
          {
            msg.setDurableSelectorNamespaceMap(null);
          }
        } 
        else
        {
          msg.setDurableSelectorNamespaceMap(null);
        } // eof instanceof MPSelectionCriteria
      } // eof null criteria
      
      //defect 259036
      msg.setCloned(subState.isCloned());
      msg.setNoLocal(subState.isNoLocal());

      msg.setSecurityUserid(subState.getUser());

      // Set the flag that signals whether this is
      // the privileged SIBServerSubject.
      msg.setSecurityUseridSentBySystem(subState.isSIBServerSubject());          
    }
    catch (Exception e)
    {
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.DurableInputHandler.createDurableCreateDurable",
        "1:495:1.52.1.1",
        DurableInputHandler.class);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createDurableCreateDurable", msg);
      
    return msg;
  }

  /**
   * Create a DeleteDurable request for an existing durable connection.
   * 
   * @param subName the name of the subscription to delete.
   * @param reqID The request ID to be used for the request.
   * @param dme The dme to which the request should be sent
   */
  protected static ControlDeleteDurable createDurableDeleteDurable(
    MessageProcessor MP,
    String subName,
    String userName,
    long reqID,
    SIBUuid8 dme) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createDurableDeleteDurable", new Object[] {MP, subName, userName, new Long(reqID), dme});

    ControlDeleteDurable msg = null;
      
    try
    {
      // Create and initialize the message
      msg = MessageProcessor.getControlMessageFactory().createNewControlDeleteDurable();
      initializeControlMessage(MP.getMessagingEngineUuid(), msg, dme);
      
      // Parameterize for CreateStream
      msg.setRequestID(reqID);
      msg.setDurableSubName(subName);
      msg.setSecurityUserid(userName);
    }
    catch (Exception e)
    {
      FFDCFilter.processException(e,
        "com.ibm.ws.sib.processor.impl.DurableInputHandler.createDurableDeleteDurable",
        "1:540:1.52.1.1",
        DurableInputHandler.class);
      SibTr.exception(tc, e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createDurableDeleteDurable", msg);
      
    return msg;
  }

  /**
   * Common initialization for all messages sent by the DurableInputHandler.
   * 
   * @param msg Message to initialize.
   * @param remoteMEId The ME the message will be sent to.
   */
  protected static void initializeControlMessage(SIBUuid8 sourceME, ControlMessage msg, SIBUuid8 remoteMEId)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "initializeControlMessage", new Object[] {sourceME, msg, remoteMEId});

    SIMPUtils.setGuaranteedDeliveryProperties(msg,
        sourceME, 
        remoteMEId,
        null,
        null,
        null,
        ProtocolType.DURABLEOUTPUT,
        GDConfig.PROTOCOL_VERSION); 
       
    msg.setPriority(SIMPConstants.CONTROL_MESSAGE_PRIORITY);
    msg.setReliability(SIMPConstants.CONTROL_MESSAGE_RELIABILITY);
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "initializeControlMessage");
  }

  /**
   * Issue a general request, wait for the reply, then return it.
   *
   * @param msg The ControlMessage to send.
   * @param me The ME to send the request to.
   * @param retry The retry interval (in milliseconds) for the request.
   * @param tries The number of times to attempt to send the request.
   * @param requestID The unique ID of the request
   * @return The result of sending the request, either null for a timeout, or
   * some valid return code for the actual request.
   */
  public static Object issueRequest(
    MessageProcessor MP,
    ControlMessage msg, 
    SIBUuid8 remoteUuid, 
    long retry, 
    int tries, 
    long requestID)
  {       
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "issueRequest", new Object[] {MP, msg, remoteUuid, new Long(retry), new Integer(tries), new Long(requestID)});
     
    // Short circuit ME rechability test
    if (!MP.getMPIO().isMEReachable(remoteUuid))
    { 
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "issueRequest", null); 
      return null; 
    } 
      
    // Prepare the request map
    Object[] awaitResult = new Object[1];
    synchronized (_requestMap)
    {
      _requestMap.put(new Long(requestID), awaitResult);
    }
    
    synchronized (awaitResult)
    {
      // Now send the request, setup the retry alarm, and wait for a result
      MP.getMPIO().sendToMe(remoteUuid, SIMPConstants.CONTROL_MESSAGE_PRIORITY, msg);
      ResendRecord retryRecord = new ResendRecord(MP, msg, remoteUuid, retry, tries, requestID);
      MP.getAlarmManager().create(retry, _alarmHandler, retryRecord);
      
      while (true)
        try
        {
          awaitResult.wait();
          break;
        }
        catch (InterruptedException e)
        {
          // No FFDC code needed
          // We shouldn't be interrupted, but if we are loop around and try again
        }
    }
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "issueRequest", awaitResult[0]);
      
    return awaitResult[0];
  }


  /**
   * Issue a CreateStream request for an existing remote durable subscription.
   * The caller is blocked until we receive a reply for this request.
   * 
   * @param req The state describing the request
   * @return If successful, a ControlNotFlushed, otherwise an exception is thrown.
   * @throws SIDurableSubscriptionMismatchException
   */
  public static ControlNotFlushed issueCreateStreamRequest(
    MessageProcessor MP,
    ConsumerDispatcherState subState,
    SIBUuid12 destID,
    SIBUuid8 destME)
    throws SIResourceException,
           SIDestinationLockedException,
           SIDurableSubscriptionNotFoundException,
           SINotAuthorizedException, 
           SIDurableSubscriptionMismatchException
  {   
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "issueCreateStreamRequest", new Object[] {MP, subState, destID});
          
    long           requestID = MP.nextTick();
    SIBUuid8       durHomeID = destME;
    ControlMessage msg       = createDurableCreateStream(MP, subState, requestID, durHomeID);
    
    // attach requires a destination ID
    msg.setGuaranteedTargetDestinationDefinitionUUID(destID);
    
    Object result = 
      issueRequest(MP, msg, durHomeID,
                   CREATESTREAM_RETRY_TIMEOUT, -1, // 219870: retry forever, otherwise use CREATESTREAM_NUMTRIES,
                   requestID);
      
    // If we get a ControlNotFlushed, return a new destination
    // otherwise throw an exception.
    if (result == null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "issueCreateStreamRequest", "SIResourceException");
      // Gave up because ME unreachable
      throw new SIResourceException(
        nls.getFormattedMessage(
          "REMOTE_DURABLE_TIMEOUT_ERROR_CWSIP0631",
          new Object[] {
            "attach",
            subState.getSubscriberID(), 
            subState.getDurableHome()},
          null));
    }
    
    if (result instanceof ControlCardinalityInfo)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "issueCreateStreamRequest", "SIDurableSubscriptionLockedException");
      // Subscription already has an attachment
      throw new SIDestinationLockedException(
        nls.getFormattedMessage(
          "SUBSCRIPTION_IN_USE_ERROR_CWSIP0152", 
          new Object[] { subState.getSubscriberID(),
                         subState.getDurableHome()}, 
          null));
    } 
    else if (result instanceof ControlDurableConfirm)
    {
      // Throw exception based on status info
      int status = ((ControlDurableConfirm) result).getStatus();
      
      if (status == DurableConstants.STATUS_SUB_NOT_FOUND)
      { 
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
          SibTr.exit(tc, "issueCreateStreamRequest", "SIDurableSubscriptionNotFoundException"); 
        // not found error      
        throw new SIDurableSubscriptionNotFoundException(
          nls.getFormattedMessage(
            "SUBSCRIPTION_DOESNT_EXIST_ERROR_CWSIP0146",
            new Object[] { subState.getSubscriberID(),
                           subState.getDurableHome() },
            null));
      }
      else if(status == DurableConstants.STATUS_NOT_AUTH_ERROR)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
          SibTr.exit(tc, "issueCreateStreamRequest", "SINotAuthorizedException"); 
        // not auth error      
        throw new SINotAuthorizedException(
          nls.getFormattedMessage(
            "USER_NOT_AUTH_ACTIVATE_ERROR_CWSIP0312",
            new Object[] { subState.getUser(), subState.getSubscriberID(), subState.getTopicSpaceUuid() },
            null));          
      }
      else if(status == DurableConstants.STATUS_SUB_MISMATCH_ERROR)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
          SibTr.exit(tc, "issueCreateStreamRequest", "SIDurableSubscriptionMismatchException"); 
        // durable mismatch error  
        throw new SIDurableSubscriptionMismatchException(
            nls.getFormattedMessage(
              "SUBSCRIPTION_ALREADY_EXISTS_ERROR_CWSIP0143",
              new Object[] { subState.getSubscriberID(),
                             subState.getDurableHome()},
              null));
      }
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "issueCreateStreamRequest", "SIErrorException");
        
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.DurableInputHandler",
          "1:751:1.52.1.1" });
      // Anything else is a general error
      throw new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.DurableInputHandler",
            "1:758:1.52.1.1" },
          null));
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "issueCreateStreamRequest", result);
      
    return (ControlNotFlushed) result;
  }

  /**
   * Issue a CreateDurable request for a new remote durable subscription.
   * The caller is blocked until we receive a reply for this request.
   * 
   * @param req The state describing the request
   * @return One of STATUS_OK, STATUS_SUB_ALREADY_EXISTS, or STATUS_SUB_GENERAL_ERROR
   */
  public static int issueCreateDurableRequest(
    MessageProcessor MP,
    ConsumerDispatcherState subState, 
    SIBUuid8 remoteMEUuid,
    SIBUuid12 destinationID)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "issueCreateDurableRequest", new Object[] { MP, subState, remoteMEUuid, destinationID});
      
    long           requestID = MP.nextTick();
    ControlMessage msg       = createDurableCreateDurable(MP, subState, requestID, remoteMEUuid);
    
    // Create requires a destination ID
    msg.setGuaranteedTargetDestinationDefinitionUUID(destinationID);
      
    Object result = 
      issueRequest(MP, msg, remoteMEUuid,
                   CREATEDURABLE_RETRY_TIMEOUT, -1, // 219870: retry forever, otherwise use CREATEDURABLE_NUMTRIES,
                   requestID);

    if (result == null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "issueCreateDurableRequest", "SIResourceException");
      // Timeout, throw a general error
      throw new SIResourceException(
        nls.getFormattedMessage(
          "REMOTE_DURABLE_TIMEOUT_ERROR_CWSIP0631",
          new Object[] {
            "create",
            subState.getSubscriberID(), 
            subState.getDurableHome()},
          null));
    }
      
    // Otherwise, reply should always be a ControlDurableConfirm with a status code
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "issueCreateDurableRequest", new Integer(((ControlDurableConfirm) result).getStatus()));
    return ((ControlDurableConfirm) result).getStatus();
  }

  /**
   * Issue a DeleteDurable request for an existing remote durable subscription.
   * The caller is blocked until we receive a reply for this request.
   * 
   * @param req The state describing the request
   * @return One of STATUS_OK, STATUS_SUB_NOT_FOUND, or STATUS_SUB_GENERAL_ERROR
   */
  public static int issueDeleteDurableRequest(
    MessageProcessor MP, 
    String subName, 
    String userName,
    SIBUuid8 remoteMEUuid)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "issueDeleteDurableRequest", new Object[] {MP, subName, userName, remoteMEUuid});
      
    long     requestID = MP.nextTick();
    ControlMessage msg = createDurableDeleteDurable(MP, subName, userName, requestID, remoteMEUuid);
    
    Object result = 
      issueRequest(MP, msg, remoteMEUuid,
                   DELETEDURABLE_RETRY_TIMEOUT, -1, // 219870: retry forever, otherwise use DELETEDURABLE_NUMTRIES,
                   requestID);

    if (result == null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "issueDeleteDurableRequest", "SIResourceException");
      // Timeout, throw a general error
      throw new SIResourceException(
        nls.getFormattedMessage(
          "REMOTE_DURABLE_TIMEOUT_ERROR_CWSIP0631",
          new Object[] {
            "delete",
            subName, 
            remoteMEUuid},
          null));
    }
      
    // Otherwise, reply should always be a ControlDurableConfirm with a status code
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "issueDeleteDurableRequest", new Integer(((ControlDurableConfirm) result).getStatus()));
    return ((ControlDurableConfirm) result).getStatus();
  }

  /////////////////////////////////////////////////////////////
  // Private methods
  /////////////////////////////////////////////////////////////

  /**
   * Process a retry alarm.
   * 
   * @param arg This should be an instance of ResendRecord indicating
   * how we should retry the request.
   */
  protected static void internalAlarmHandler(Object arg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "internalAlarmHandler", arg);
    
    ResendRecord record = (ResendRecord) arg;
    
    synchronized (_requestMap)
    {
      Long key = new Long(record.requestID);
      
      if (_requestMap.containsKey(key))
      {
        // Someone still waiting for the request, figure out what to do about it
        if (record.triesRemaining != 0)
        {
          // We have tries remaining so resend
          
          // Short circuit if ME unreachable
          if (!record.MP.getMPIO().isMEReachable(record.targetUuid))
            wakeupWaiter(record.requestID, null);
            
          record.MP.getMPIO().sendToMe(record.targetUuid, SIMPConstants.CONTROL_MESSAGE_PRIORITY, record.msg);
          // 219870: use triesRemaining < 0 to try forever
          if (record.triesRemaining > 0)
            record.triesRemaining--;
          record.MP.getAlarmManager().create(record.resendInterval, _alarmHandler, record);
        }
        else
        {
          // Wakeup the waiter with a timeout error
          wakeupWaiter(record.requestID, null);
        }
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "internalAlarmHandler");
  }

  /**
   * Attempt to create a durable subscription on a remote ME.
   *
   * @param MP The MessageProcessor.
   * @param subState State describing the subscription to create.
   * @param remoteME The ME where the subscription should be created.
   */
  public static void createRemoteDurableSubscription(
    MessageProcessor MP,
    ConsumerDispatcherState subState,
    SIBUuid8 remoteMEUuid,
    SIBUuid12 destinationID) 
    throws SIDurableSubscriptionAlreadyExistsException,
           SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createRemoteDurableSubscription", new Object[] { MP, subState, remoteMEUuid, destinationID });

    // Issue the request via the DurableInputHandler
    int status = issueCreateDurableRequest(MP, subState, remoteMEUuid, destinationID);

    switch (status)
    {
      case DurableConstants.STATUS_SUB_ALREADY_EXISTS:
      {      
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "createRemoteDurableSubscription", "SIDurableSubscriptionAlreadyExistsException");
        throw new SIDurableSubscriptionAlreadyExistsException(
          nls.getFormattedMessage(
            "SUBSCRIPTION_ALREADY_EXISTS_ERROR_CWSIP0143",
            new Object[] {subState.getSubscriberID(),
                          subState.getDurableHome()},
            null));
      }
      case DurableConstants.STATUS_SUB_GENERAL_ERROR:
      {  
        // Problem on other side which should be logged, best we      
        // can do is throw an exception here.
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "createRemoteDurableSubscription", "SIErrorException");
          
        SibTr.error(tc,"INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.DurableInputHandler",
            "1:955:1.52.1.1" });  
            
        throw new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.DurableInputHandler",
              "1:962:1.52.1.1" },
            null));
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createRemoteDurableSubscription");
  }

  /**
   * Attempt to delete a durable subscription on a remote ME.
   *
   */
  public static void deleteRemoteDurableSub(
    MessageProcessor MP,
    String subName,
    String userName,
    SIBUuid8 remoteMEUuid)
    throws SIResourceException,
           SIDurableSubscriptionNotFoundException,
           SIDestinationLockedException,
           SINotAuthorizedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "deleteRemoteDurableSub", 
        new Object[]{MP, subName, userName, remoteMEUuid});

    // Issue the request
    int status = issueDeleteDurableRequest(MP, subName, userName, remoteMEUuid);

    switch (status)
    {
      case DurableConstants.STATUS_SUB_NOT_FOUND:
      {      
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
          SibTr.exit(tc, "deleteRemoteDurableSub", "SIDurableSubscriptionNotFoundException");

        throw new SIDurableSubscriptionNotFoundException(
          nls.getFormattedMessage(
            "SUBSCRIPTION_DOESNT_EXIST_ERROR_CWSIP0072",
            new Object[] { subName,
                remoteMEUuid },
            null));
      }
        
      case DurableConstants.STATUS_SUB_CARDINALITY_ERROR:
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())       
          SibTr.exit(tc, "deleteRemoteDurableSub", "SIDurableSubscriptionLockedException");

        throw new SIDestinationLockedException(
          nls.getFormattedMessage(
            "SUBSCRIPTION_IN_USE_ERROR_CWSIP0152", 
            new Object[] { subName,
                remoteMEUuid }, 
            null));
      }

      case DurableConstants.STATUS_NOT_AUTH_ERROR:
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())       
          SibTr.exit(tc, "deleteRemoteDurableSub", "SINotAuthorizedException");

        throw new SINotAuthorizedException(
          nls.getFormattedMessage(
            "USER_NOT_AUTH_DELETE_ERROR_CWSIP0311",
            new Object[] { userName, subName, null},
            null));
      }
      
      case DurableConstants.STATUS_SIB_LOCKED_ERROR:
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())       
          SibTr.exit(tc, "deleteRemoteDurableSub", "SIDestinationLockedException");

        throw new SIDestinationLockedException(
            nls.getFormattedMessage(
              "SUBSCRIPTION_IN_USE_ERROR_CWSIP0153",
              new Object[] { subName,
                  remoteMEUuid },
              null));
      }
     
      case DurableConstants.STATUS_SUB_GENERAL_ERROR:
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
          SibTr.exit(tc, "deleteRemoteDurableSub", "SIResourceException");
          
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.DurableInputHandler",
          "1:1053:1.52.1.1" });
          
        throw new SIResourceException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.DurableInputHandler",
              "1:1060:1.52.1.1" },
            null));
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "deleteRemoteDurableSub");
    
  }


  /////////////////////////////////////////////////////////////
  // Inner classes
  /////////////////////////////////////////////////////////////
  
  // Instances of this class are used to maintain liveness for
  // the various control message requests
  static class ResendRecord
  {
    MessageProcessor MP;
    ControlMessage   msg;
    SIBUuid8         targetUuid;
    long             resendInterval;
    int              triesRemaining;
    long             requestID;
    
    public ResendRecord(MessageProcessor MP, 
                        ControlMessage M, 
                        SIBUuid8 T, 
                        long resend, 
                        int tries, 
                        long req)
    {
      this.MP        = MP;
      msg            = M;
      targetUuid         = T;
      resendInterval = resend;
      triesRemaining = tries;
      requestID      = req;
    }
  }
  
  public long handleControlMessageWithReturnValue(SIBUuid8 sourceMEUuid,
		ControlMessage cMsg) throws SIIncorrectCallException,
		SIResourceException, SIConnectionLostException, SIRollbackException {
    return 0;
  }
  
}
