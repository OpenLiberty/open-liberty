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

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.RuntimeEvent;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.ExceptionDestinationHandler;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.UndeliverableReturnCode;
import com.ibm.ws.sib.processor.exceptions.SIMPControllableNotFoundException;
import com.ibm.ws.sib.processor.exceptions.SIMPException;
import com.ibm.ws.sib.processor.exceptions.SIMPRuntimeOperationFailedException;
import com.ibm.ws.sib.processor.gd.TickRange;
import com.ibm.ws.sib.processor.impl.AORequested;
import com.ibm.ws.sib.processor.impl.AOStream;
import com.ibm.ws.sib.processor.impl.ExceptionDestinationHandlerImpl;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.items.AOValue;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.impl.store.itemstreams.AOProtocolItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream;
import com.ibm.ws.sib.processor.runtime.SIMPReceivedMessageRequestInfo;
import com.ibm.ws.sib.processor.runtime.SIMPTransmitMessageRequestControllable;
import com.ibm.ws.sib.processor.runtime.anycast.ReceivedMessageRequestInfo;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * The object is created for each tick in the AOStream that represents the state of the
 * anycast protocol at the destination ME.
 */
public class TransmitMessageRequest extends AbstractControlAdapter implements SIMPTransmitMessageRequestControllable
{
  
  // The tick that this object represents
  private long tick;
    
  private SIMPItemStream itemStream;
  private AOStream aoStream;
  private MessageProcessor messageProcessor;
  private DestinationHandler destination;
  
  private static final TraceNLS nls = TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  
  private static TraceComponent tc =
    SibTr.register(
  TransmitMessageRequest.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);
  
  /**
   * This object is created for each tick on the AOStream when asked for it. i.e. iterator.next()
   * 
   * @param tick
   * @param itemStream
   * @param aoStream
   * @param messageProcessor
   * @param destination
   */
  public TransmitMessageRequest(long tick, AOProtocolItemStream itemStream, AOStream aoStream,
                                MessageProcessor messageProcessor, DestinationHandler destination)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "TransmitMessageRequest", new Object[]{new Long(tick), itemStream, aoStream, messageProcessor, destination} );
      
    this.tick = tick;
    this.itemStream = itemStream;
    this.aoStream = aoStream;
    this.messageProcessor = messageProcessor;
    this.destination = destination;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "TransmitMessageRequest");
  }
  
  private long getMessageID()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getMessageID");
      
    long msgId = -1;
    TickRange r = getTickRange();
    if (r.value instanceof AOValue)
    {
      msgId = ((AOValue)r.value).getMsgId();
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getMessageID", new Long(msgId));
    return msgId;
  }
  
  private TickRange getTickRange()
  {
    return aoStream.getTickRange(tick);
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getJsMessage()
   */
  public JsMessage getJsMessage() throws SIMPControllableNotFoundException, SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getJsMessage");

    assertValidControllable();

    SIMPMessage message = null;
    try
    {
      message = getSIMPMessage();
      if(message == null)
      {
        SIMPControllableNotFoundException finalE =
          new SIMPControllableNotFoundException(
            nls.getFormattedMessage(
              "INTERNAL_MESSAGING_ERROR_CWSIP0005",
              new Object[] {"TransmitMessageRequest.assertValidControllable",
                            "1:169:1.48",
                            aoStream.getID()},
              null));
              
        SibTr.exception(tc, finalE);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exception(tc, finalE);
        throw finalE; 
      }
    }
    catch (SIResourceException e)
    {
      // No FFDC code needed
      SIMPRuntimeOperationFailedException finalE =
        new SIMPRuntimeOperationFailedException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {"TransmitMessageRequest.getJsMessage",
                          "1:187:1.48",
                          e},
            null), e);

      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "removeMessage", finalE);
      throw finalE;
    }    
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getJsMessage", message.getMessage());
    return message.getMessage();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getState()
   */
  public String getState()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getState");
      
    String state = null;

    TickRange r = getTickRange();
    byte tickState = r.type;
    

    switch ( tickState )
    {
      case TickRange.Requested :
      {
        state = State.REQUEST.toString();
        break;
      }
      case TickRange.Accepted :
      {
        state = State.ACKNOWLEDGED.toString();
        break;
      }
      case TickRange.Completed :
      {
        state = State.ACKNOWLEDGED.toString();
        break;
      }
      case TickRange.Rejected :
      {
        state = State.REJECT.toString();
        break;
      }
      case TickRange.Value :
      {
        if (((AOValue) r.value).removing)
          state = State.REMOVING.toString();
        else   
          state = State.PENDING_ACKNOWLEDGEMENT.toString();
        
        break;
      }
    }
    
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getState", state);
    return state;
    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getStartTick()
   */
  public long getStartTick()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getStateTick");

    long startTick = getTickRange().startstamp;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getStartTick", new Long(startTick));
      
    return startTick;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getEndTick()
   */
  public long getEndTick()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getEndTick");
    
    long endTick = getTickRange().endstamp;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getEndTick", new Long(endTick));
    
    return endTick;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#removeMessage(boolean)
   */
  public void moveMessage(boolean discard) throws SIMPControllableNotFoundException, SIMPRuntimeOperationFailedException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "moveMessage");

    assertValidControllable();

    SIMPTransactionManager txManager = messageProcessor.getTXManager();

    LocalTransaction tran = txManager.createLocalTransaction(false);
    long msgID = getMessageID();
    try
    {
      SIMPMessage msg = getSIMPMessage();     
      
      if(!discard)
      {
        ExceptionDestinationHandler edh = new ExceptionDestinationHandlerImpl(destination);
        UndeliverableReturnCode code =
          edh.handleUndeliverableMessage(
            msg.getMessage(),
            null,
            tran,
            SIRCConstants.SIRC0036_MESSAGE_ADMINISTRATIVELY_REROUTED_TO_EXCEPTION_DESTINATION, //TODO get proper messages!
            new String[]{""+msgID,
                         destination.toString(),
                         messageProcessor.getMessagingEngineName()});          
        if(code == UndeliverableReturnCode.OK || code == UndeliverableReturnCode.DISCARD)
        {
          discard = true;
        }
        else
        {
          SIMPRuntimeOperationFailedException finalE =
            new SIMPRuntimeOperationFailedException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                new Object[] {"TransmitMessageRequest.moveMessage",
                              "1:328:1.48",
                              code},
                null));
  
          SibTr.exception(tc, finalE);
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
            SibTr.exit(tc, "moveMessage", finalE);
          throw finalE;          
        }
      }
      
      if(discard)
      {      
        Transaction msTran = messageProcessor.resolveAndEnlistMsgStoreTransaction(tran);
        aoStream.syncRemoveValueTick(tick, msTran, msg);  
      }
      
      // Commit the tran
      tran.commit();
    }
    catch (SIException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.TransmitMessageRequest.moveMessage",
        "1:353:1.48",
        this);

      SIMPRuntimeOperationFailedException finalE =
        new SIMPRuntimeOperationFailedException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {"TransmitMessageRequest.moveMessage",
                          "1:361:1.48",
                          e,
                          new Long(msgID)},
            null), e);

      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "moveMessage", finalE);
      throw finalE;
    }    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())  
      SibTr.exit(tc, "moveMessage");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getId()
   */
  public String getId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getId");
      SibTr.exit(tc, "getId", ""+aoStream.getID());
    }
    return ""+aoStream.getID() + ":" + tick;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPControllable#getName()
   */
  public String getName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getName");
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getName");
    return null;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#checkValidControllable()
   */
  public void assertValidControllable() throws SIMPControllableNotFoundException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "assertValidControllable");
      
    if(aoStream.itemStream == null || tick < 0 )
    {
      SIMPControllableNotFoundException finalE =
        new SIMPControllableNotFoundException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0005",
            new Object[] {"QueuedMessage.assertValidControllable",
                          "1:416:1.48",
                          new Long(aoStream.getID())},
            null));
  
      SibTr.exception(tc, finalE);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "assertValidControllable");
      throw finalE;
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "assertValidControllable");
    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.AbstractControllable#dereferenceControllable()
   */
  public void dereferenceControllable()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "dereferenceControllable");  
      SibTr.exit(tc, "dereferenceControllable");
    }
    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#registerControlAdapterAsMBean()
   */
  public void registerControlAdapterAsMBean()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "registerControlAdapterAsMBean");  
      SibTr.exit(tc, "registerControlAdapterAsMBean");
    }
    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#deregisterControlAdapterMBean()
   */
  public void deregisterControlAdapterMBean()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "deregisterControlAdapterMBean");  
      SibTr.exit(tc, "deregisterControlAdapterMBean");
    }
    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.ControlAdapter#runtimeEventOccurred(com.ibm.ws.sib.admin.RuntimeEvent)
   */
  public void runtimeEventOccurred(RuntimeEvent event)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "runtimeEventOccurred", event);  
      SibTr.exit(tc, "runtimeEventOccurred");
    }
  }
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getTransactionId()
   */
  public String getTransactionId()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTransactionId");
    
    String tranID = null;
    PersistentTranId pTranID = itemStream.getTransactionId();
    
    if (pTranID != null)
    {
      tranID = pTranID.toTMString(); 
    }
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTransactionId", tranID);
    return tranID;
  }
  
  private SIMPMessage getSIMPMessage() throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getSIMPMessage");

    SIMPMessage message = null;
    
    try
    {
      message = (SIMPMessage)itemStream.findById(getMessageID());
    }
    catch(MessageStoreException e)
    {
      // FFDC
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.TransmitMessageRequest.getSIMPMessage",
          "1:519:1.48",
          this);  
                  
      SibTr.exception(tc, e); 
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getSIMPMessage", e);
      
      throw new SIResourceException(e);
    }
  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getSIMPMessage", message);
    return message;
  }   
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPTransmitMessageRequestControllable#getTick()
   */
  public long getTick()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getTick");
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getTick", new Long(tick));
    return tick;
  } 
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPTransmitMessageRequestControllable#getRequestMessageInfo()
   */
  public SIMPReceivedMessageRequestInfo getRequestMessageInfo()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "getRequestMessageInfo");
      
    SIMPReceivedMessageRequestInfo requestInfo = null;
    Object tickValue = aoStream.getTickRange(tick).value;
    if(tickValue instanceof AORequested)
    {
      //this message has been requested but not answered - therefore
      //we provide request info
      AORequested aoReqValue = (AORequested)tickValue;
      
      requestInfo = new ReceivedMessageRequestInfo(
        aoReqValue.startTime,
        aoReqValue.expiryInterval,
        aoReqValue.aock.getSelectionCriterias(),
        aoStream.getDMEVersion(),
        aoReqValue.tick);  
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "getRequestMessageInfo", requestInfo);
    return requestInfo;    
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPTransmitMessageRequestControllable#cancelMessageRequest()
   */
  public synchronized void cancelMessageRequest(boolean discard)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "cancelMessageRequest", new Boolean(discard));
      
    SIMPTransactionManager txManager = messageProcessor.getTXManager();
    LocalTransaction tran = null;
    
    try
    {
      MessageItem msg = (MessageItem)getSIMPMessage();
      
      
      if(discard)
      {
        if(msg!=null)
        {
          tran = txManager.createLocalTransaction(false);
          Transaction msTran = messageProcessor.resolveAndEnlistMsgStoreTransaction(tran);

          // Cleanup the AOValue and the Msg
          aoStream.syncRemoveValueTick(tick, msTran, msg);
          
          tran.commit();
        }
        else
        {
          //if the request was not in the value state, then we simply expire
          //the request
          aoStream.expiredRequest(tick, true);          
        }
      }
      else
      {  
        if(msg!=null) 
        {          
          tran = txManager.createLocalTransaction(false);
          Transaction msTran = messageProcessor.resolveAndEnlistMsgStoreTransaction(tran);
          
          // Cleanup the AOValue
          aoStream.syncRemoveValueTick(tick, msTran, null);
          
          if(msg.isInStore())
          {
            long lockID = msg.getLockID();
            if(lockID!=AbstractItem.NO_LOCK_ID)
            {
              //unlock the message for others to consume it etc
              msg.unlockMsg(lockID, msTran, true);
            }  
          }
          tran.commit();
        }
        else
          //if the request was not in the value state, then we simply expire
          //the request
          aoStream.expiredRequest(tick, true); 
      }
    }
    catch(MessageStoreException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.runtime.TransmitMessageRequest.cancelMessageRequest",
        "1:644:1.48",
        this); 
        
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] { "com.ibm.ws.sib.processor.runtime.TransmitMessageRequest.cancelMessageRequest", 
                       "1:649:1.48", 
                       SIMPUtils.getStackTrace(e) });
      SibTr.exception(tc, e);
      
      try
      {
        if (tran!=null)
          tran.rollback();
      }
      catch (SIException ee)
      {
        // No FFDC code needed
        SibTr.exception(tc, e);
      }
    }
    catch (SIException e)
    {
      FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.runtime.TransmitMessageRequest.cancelMessageRequest",
          "1:669:1.48",
          this); 
          
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] { "com.ibm.ws.sib.processor.runtime.TransmitMessageRequest.cancelMessageRequest", 
                         "1:674:1.48", 
                         SIMPUtils.getStackTrace(e) });
        SibTr.exception(tc, e); 
        
        try
        {
          if (tran!=null)
            tran.rollback();
        }
        catch (SIException ee)
        {
          // No FFDC code needed
          SibTr.exception(tc, e);
        }

    }  

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "cancelMessageRequest");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.runtime.SIMPRemoteMessageControllable#getMEArrivalTimestamp()
   */
  public long getMEArrivalTimestamp() throws SIMPControllableNotFoundException, SIMPException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getMEArrivalTimestamp");

    long timestamp =  getJsMessage().getCurrentMEArrivalTimestamp();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getMEArrivalTimestamp", new Long(timestamp));
    return timestamp;
  }
}
