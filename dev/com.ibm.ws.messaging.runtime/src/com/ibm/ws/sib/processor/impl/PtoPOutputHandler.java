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

// Import required classes.
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.mfp.MessageCreateFailedException;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.mfp.control.ControlAck;
import com.ibm.ws.sib.mfp.control.ControlAckExpected;
import com.ibm.ws.sib.mfp.control.ControlAreYouFlushed;
import com.ibm.ws.sib.mfp.control.ControlFlushed;
import com.ibm.ws.sib.mfp.control.ControlMessage;
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.control.ControlNack;
import com.ibm.ws.sib.mfp.control.ControlNotFlushed;
import com.ibm.ws.sib.mfp.control.ControlSilence;
import com.ibm.ws.sib.mfp.impl.ControlMessageFactory;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.UndeliverableReturnCode;
import com.ibm.ws.sib.processor.gd.GDConfig;
import com.ibm.ws.sib.processor.gd.SourceStream;
import com.ibm.ws.sib.processor.gd.SourceStreamManager;
import com.ibm.ws.sib.processor.gd.StreamSet;
import com.ibm.ws.sib.processor.gd.TickRange;
import com.ibm.ws.sib.processor.impl.exceptions.FlushAlreadyInProgressException;
import com.ibm.ws.sib.processor.impl.interfaces.ControlHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl;
import com.ibm.ws.sib.processor.impl.interfaces.FlushComplete;
import com.ibm.ws.sib.processor.impl.interfaces.HealthStateListener;
import com.ibm.ws.sib.processor.impl.interfaces.InputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.InputHandlerStore;
import com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener;
import com.ibm.ws.sib.processor.impl.interfaces.OutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.Reallocator;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.BatchHandler;
import com.ibm.ws.sib.processor.impl.store.MessageEvents;
import com.ibm.ws.sib.processor.impl.store.filters.ClassEqualsFilter;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.impl.store.itemstreams.ProtocolItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream;
import com.ibm.ws.sib.processor.io.MPIO;
import com.ibm.ws.sib.processor.runtime.HealthState;
import com.ibm.ws.sib.processor.utils.LockManager;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.UserTrace;
import com.ibm.ws.sib.processor.utils.am.MPAlarmManager;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * The PtoPOutputHandler is created to represent distributing
 * messages to destinations on Remote ME's
 */
public final class PtoPOutputHandler
  implements OutputHandler,
             ControlHandler,
             DownstreamControl,
             MessageEventListener,
             Reallocator
{
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  private static final TraceComponent tc =
    SibTr.register(
      PtoPOutputHandler.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

   
  private BatchHandler sourceBatchHandler;

  private ControlMessageFactory cmf;
  

  /**
   * These will be the same except when this is a link
   * The targetCellule is the targetME for messages
   * The routingCellule is passed into MPIO 
   */
  private SIBUuid8 targetMEUuid;
  private SIBUuid8 routingMEUuid;
  
  private SIBUuid8 unknownUuid = new SIBUuid8( SIMPConstants.UNKNOWN_UUID );
 
  /**
   * The sourceStreamManager handles all stream operations such as flush
   */
  private SourceStreamManager sourceStreamManager;

  /**
   * The itemstream used to store the messages for the OutputHandler
   * 183715.1
   */
  private PtoPMessageItemStream transmissionItemStream;
  
  /**
   * The routingAddress used for publications to system/temp destinations
   */
  JsDestinationAddress routingDestination = null;
  boolean isSystemOrTemp = false;   

  //private MessagingEngine trmME;


  private DestinationHandler destinationHandler;

  // Flag to indicate whether the destination is a LinkHandler 
  // and name of the Link and Uuid of sending Bus if it is
  private boolean   isLink = false;
  private String    linkName = null;

  private MessageProcessor messageProcessor;
  private boolean isGuess = false;


  private MPIO mpio;
  
  private long lockID = 0;

  // If true, then a pending flush for delete for a source
  // node has completed successfully.
  protected boolean flushedForDeleteSource = false;
  
  // If non-null then we're attempting to flush a source
  // stream so we can delete the destination.
  protected FlushComplete deleteFlushSource = null;

  // Is the transmision stream high/low
  private volatile boolean _qHigh;
  
  private MPAlarmManager am;
  

  /**
   * Standard constructor for the PtoPOutputHandler
   */
  public PtoPOutputHandler(
    DestinationHandler destinationHandler,
    MessageProcessor messageProcessor,
    ProtocolItemStream protocolItemStream,
    SIBUuid8 targetMEUuid,
    PtoPMessageItemStream itemStream) throws SIResourceException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "PtoPOutputHandler", 
        new Object[]{destinationHandler, 
                     messageProcessor, 
                     protocolItemStream, 
                     targetMEUuid, 
                     itemStream});
       
    try
    {    
      // Cache a lock ID to lock the items with          
      lockID = messageProcessor.getMessageStore().getUniqueLockID(AbstractItem.STORE_NEVER);
    }
    catch (PersistenceException e)
    {
      // No FFDC code needed
          
      if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
        SibTr.exception(tc, e);
            
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
        SibTr.exit(tc, "PtoPOutputHandler", "SIResourceException");
      throw new SIResourceException(e);
    }

    // Assign the ME name to the Handler
    this.destinationHandler = destinationHandler;
    transmissionItemStream = itemStream;                  //183715.1

    // Create the sourceStream for this output handler.
    sourceStreamManager = new SourceStreamManager(messageProcessor,
                                                  this,
                                                  destinationHandler,
                                                  protocolItemStream,
                                                  targetMEUuid,
                                                  this);



    // Defect 238709: An MQLinkHandler is deemed not to be a link    
    if( destinationHandler.isLink() && !destinationHandler.isMQLink() )
    {
      isLink = true;
      linkName = destinationHandler.getName();
                
      String linkType = ((LinkHandler)destinationHandler).getType();        
           
      if( linkType.equals("SIBVirtualGatewayLink"))
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "SIBLink type, set null routing cell"); 
        
        this.routingMEUuid = null;         
      }
      else
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
           SibTr.debug(tc, "MQLink type, set routingCell: " + targetMEUuid);
        // Create Cellule for routing to targetME
        this.routingMEUuid = targetMEUuid;                     
      }
    }
    else
    {
      // Create Cellule for routing to targetME
      this.routingMEUuid = targetMEUuid;
    }
    
    this.messageProcessor = messageProcessor;
    this.targetMEUuid = targetMEUuid;
    
    mpio = messageProcessor.getMPIO();                                             
                                                      
    sourceBatchHandler = messageProcessor.getSourceBatchHandler();
    
    cmf = MessageProcessor.getControlMessageFactory();
    
    // If we don`t know uuid of remote destination we need to put routing destination in the acks
    if (destinationHandler.isSystem() || destinationHandler.isTemporary())
    {
      routingDestination = SIMPUtils.createJsDestinationAddress(destinationHandler.getName(), null);
      isSystemOrTemp = true;   
    }     
    
    am = messageProcessor.getAlarmManager();
                                                                                                                                            
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "PtoPOutputHandler", this);
  }

  /**
   * Wait for the current stream to quiesce, remove it
   * from storage, then create a replacement stream ID.
   * We assume that all production has already been
   * stopped and that no new production will occur until
   * after the flush has been completed.  The reference
   * passed to this method contains the callback for
   * signalling when the flush has completed.
   *
   * @param complete An instance of the FlushComplete interface
   * which we'll invoke when the flush of the current stream
   * has completed.
   * @throws FlushAlreadyInProgressException if someone calls
   * this method but a flush is already in progress.
   */
  public void startFlush(FlushComplete complete) 
  
  throws SIRollbackException, 
         SIConnectionLostException, 
         SIResourceException, 
         SIErrorException, 
         FlushAlreadyInProgressException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc,"startFlush");

    sourceStreamManager.startFlush(complete);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "startFlush");
  }
  
  /**
   * Put a message on this PtoPOutputHandler for delivery to the remote ME.
   * This method is called during the preCommitCallback from the
   * messageStore
   *
   * @param msg The message to be delivered
   * @param transaction The transaction to be used (must at least have an autocommit transaction)
   * @param inputHandlerStore The input handler putting this message
   * @param storedByIH true if the message has already been stored in the IH
   * @return true if the message was stored in the IH (either before or during this call)
   * @throws SIStoreException thrown if there is a problem in the message store
   */
  public boolean put(
    SIMPMessage msg,
    TransactionCommon transaction,
    InputHandlerStore inputHandlerStore,
    boolean storedByIH)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "put",
        new Object[] {
          msg,
          transaction,
          inputHandlerStore,
          new Boolean(storedByIH)});
    
    // Get the JsMessage as we need to update the Guaranteed fields
    JsMessage jsMsg = msg.getMessage();
    SIMPUtils.setGuaranteedDeliveryProperties(jsMsg,
        messageProcessor.getMessagingEngineUuid(), 
        transmissionItemStream.getLocalizingMEUuid(),
        null,
        null,
        destinationHandler.getUuid(),
        ProtocolType.UNICASTINPUT,
        GDConfig.PROTOCOL_VERSION);
    
    // Remember if the original user transaction was a real one or not
    ((MessageItem)msg).setTransacted(!transaction.isAutoCommit());

    try
    {
      // Optimitically add the message to the sourceStreamManager before adding
      // the item to the itemstream, as this ensures the required GD fields are
      // initialised without having to initialise them to dummy values.
      boolean addedToStream = sourceStreamManager.addMessage(msg);

      // If this message was not added to the stream (the message is best effort)
      // there is no need to add it to the itemStream as there will be no possibility
      // for recovery at a later date.
      if(!addedToStream)
      {
        // If the stream was guessed and we are best effort then discard
        // (Also avoids NPE for PK36530)
        if (!msg.getStreamIsGuess())
        {
          // If the destination in a Link add Link specific properties to message
          if( isLink )
          {
            // Prevent any concurrent serialization of the JsMessage that could fail
            // due to having one property set but not the other
            synchronized(((MessageItem)msg).getSynchUpdateLock())
            {
              jsMsg = addLinkProps(jsMsg);
            }
          }

          // If the message was not transacted we can send it now
          if(!msg.isTransacted())
          {
            if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
              UserTrace.traceOutboundSend(jsMsg, 
                                       routingMEUuid, 
                                       destinationHandler.getName(),
                                       destinationHandler.isForeignBus() || destinationHandler.isLink(),
                                       destinationHandler.isMQLink(),
                                       destinationHandler.isTemporary());
          
            //  Send message using MPIO
            mpio.sendToMe(routingMEUuid,
                          msg.getPriority(),
                          jsMsg);
          }
          // Otherwise we wait for the transaction to commit
          else
            msg.registerMessageEventListener(MessageEvents.POST_COMMITTED_TRANSACTION, this);
        }
      }
      else
      {
        LocalTransaction siTran = null;
      
        if(!msg.isTransacted())
        { 
          //Use a local transaction as code is driven from the commit callback
          siTran = messageProcessor.getTXManager().createLocalTransaction(false);
          transaction = siTran;
        }
      
        Transaction msTran = messageProcessor.resolveAndEnlistMsgStoreTransaction(transaction);
        
        // Add the item, but don't lock it until we are ready to send it 
        try 
        {
          transmissionItemStream.addItem((MessageItem) msg, msTran);
        }
        catch (OutOfCacheSpace e)
        {
          // No FFDC code needed
          //If the add failed, remove the message from the sourceStreamManager
          sourceStreamManager.removeMessage(msg);
          throw e;       
        }
        catch (MessageStoreException e)
        {
          // FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.put",
            "1:505:1.241",
            this);
          //If the add failed, remove the message from the sourceStreamManager
          sourceStreamManager.removeMessage(msg);
          throw e;
        }                    
    
        registerForEvents(msg);

        if (siTran != null)
        {
          try
          {
            siTran.commit();
          }
          catch (SIConnectionLostException e)
          {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "put", "SIResourceException");
            throw new SIResourceException(e);
          }
          catch (SIIncorrectCallException e)
          {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "put", "SIResourceException");
            throw new SIResourceException(e);
          }
          catch (SIErrorException e)
          {
            // No FFDC code needed
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "put", "SIResourceException");
            throw new SIResourceException(e);
          }      
        }
      }
    }
    catch (OutOfCacheSpace e)
    {
      // No FFDC code needed
      SibTr.exception(tc, e);
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "put", e);
      
      throw new SIResourceException(e);       
    }
    catch (MessageStoreException e)
    {
      // MessageStoreException shouldn't occur so FFDC.
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.put",
        "1:555:1.241",
        this);

      SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "put", e);
      
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] { 
          "com.ibm.ws.sib.processor.impl.PtoPOutputHandler", "1:565:1.241", e });

      throw new SIResourceException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] { 
            "com.ibm.ws.sib.processor.impl.PtoPOutputHandler", "1:571:1.241", e },
          null),
        e);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "put", new Boolean(storedByIH));

    return storedByIH;
  }

  public void registerForEvents(SIMPMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "registerForEvents", msg);
    // We want to know about post commits so we can turn Uncommitted
    // into Value.  We want to know about post rollbacks so we can
    // decrement the use count.
    msg.registerMessageEventListener(MessageEvents.POST_COMMIT_ADD, this);
    msg.registerMessageEventListener(MessageEvents.POST_ROLLBACK_ADD, this);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "registerForEvents");
  }

  /**
   * Processes a control message
   *
   * @param cMsg The control message
   * @param msgSource The cellule which was the source of the original message
   * @param controlSource The cellule which was the source of this control message
   * @throws SIResourceException
   */
  public void handleControlMessage(SIBUuid8 sourceMEUuid, ControlMessage cMsg) throws SIRollbackException, SIConnectionLostException, SIResourceException, SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "handleControlMessage", new Object[]{sourceMEUuid, cMsg});
    // TODO: it's entirely possible for this next assert to be violated.
    // We need to add code to check this and throw an exception.
    // ASSERT: sourceCellule.equals(this.targetCellule)!

    // Grab the message type
    ControlMessageType type = cMsg.getControlMessageType();

    // IMPORTANT: do the flush related messages first so that we answer 
    // consistently with respect to any in progress flush.
    if(type == ControlMessageType.REQUESTFLUSH)
    {
      // TODO: this should invoke some higher-level interface indicating that
      // our target wants to be flushed.      
    }
    else if(type == ControlMessageType.AREYOUFLUSHED)
    {
      sourceStreamManager.processFlushQuery((ControlAreYouFlushed)cMsg);
    }
    // Now we can process the rest of the control messages...
    else if (type == ControlMessageType.ACK)
    {
      // Handle the ack as well as any flush which might
      // be waiting to progress.
      processAck((ControlAck)cMsg);
    }
    else if (type == ControlMessageType.NACK)
    {
      // Process nack.  Note that nacks don't change
      // our flushable status.
      sourceStreamManager.processNack((ControlNack)cMsg);
    }
    else
    {
      // Not a recognised type
      // throw exception
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "handleControlMessage");
  }


  /**
   * @param jsMsg
   * @return jsMsg with link properties added
   */
  private JsMessage addLinkProps(JsMessage msg )
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addLinkProps" );
      
    // Add Link specific properties to message
    msg.setGuaranteedCrossBusLinkName( linkName );
    msg.setGuaranteedCrossBusSourceBusUUID( messageProcessor.getMessagingEngineBusUuid() );
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
       SibTr.debug(tc, "Test whether outbound userid should be set in message");                       
    // Check whether we need to override the outbound Userid
    String linkOutboundUserid = ((LinkHandler)destinationHandler).getOutboundUserid(); 
    if( linkOutboundUserid != null )
    {
      // Check whether this message was sent by the privileged 
      // Jetstream SIBServerSubject.If it was then we don't reset
      // the userid in the message
      if(!messageProcessor.getAuthorisationUtils().sentBySIBServer(msg))
      {            
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "Set outbound userid: " + linkOutboundUserid + ", in message");           
        // Call SIB.security (ultimately) to set outbounduserid into msg
        messageProcessor.
          getAccessChecker().
          setSecurityIDInMessage(linkOutboundUserid, msg);    
        // Set the application userid (JMSXuserid)
        msg.setApiUserId(linkOutboundUserid);  
      }                      
    }           
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addLinkProps");
     
    return msg;   
  }
  /**
   * @param jsMsg
   * @return jsMsg with link properties added
   */
  private ControlMessage addLinkProps(ControlMessage msg )
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addLinkProps" );
      
    // Add Link specific properties to message
    msg.setGuaranteedCrossBusLinkName( linkName );
    msg.setGuaranteedCrossBusSourceBusUUID( messageProcessor.getMessagingEngineBusUuid() );
       
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addLinkProps");
     
    return msg;   
  }


  /**
   * sendAckExpectedMessage is called from SourceStream timer alarm
   */
  public void sendAckExpectedMessage(long ackExpStamp,
                                     int priority,
                                     Reliability reliability,
                                     SIBUuid12 stream) // not used for ptp
  throws SIResourceException
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendAckExpectedMessage",
    new Object[] { new Long(ackExpStamp),
                   new Integer(priority),
                   reliability });

    if( routingMEUuid != null )
    {
      ControlAckExpected ackexpMsg;
      try
      {
        ackexpMsg = cmf.createNewControlAckExpected();
      }
      catch (Exception e)
      {
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.sendAckExpectedMessage",
          "1:733:1.241",
          this);

        SibTr.exception(tc, e);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "sendAckExpectedMessage", e);
      
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
            "1:744:1.241",
            e });
      
        throw new SIResourceException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
              "1:752:1.241",
              e },
            null),
            e);
      }

      // As we are using the Guaranteed Header - set all the attributes as
      // well as the ones we want.
      SIMPUtils.setGuaranteedDeliveryProperties(ackexpMsg,
          messageProcessor.getMessagingEngineUuid(), 
          targetMEUuid,
          stream,
          null,
          destinationHandler.getUuid(),
          ProtocolType.UNICASTINPUT,
          GDConfig.PROTOCOL_VERSION);
   
      ackexpMsg.setTick(ackExpStamp);
      ackexpMsg.setPriority(priority);
      ackexpMsg.setReliability(reliability);
      
      // SIB0105
      // Update the health state of this stream 
      SourceStream sourceStream = (SourceStream)sourceStreamManager
                                                .getStreamSet()
                                                .getStream(priority, reliability);
      if (sourceStream != null)
      {
        sourceStream.setLatestAckExpected(ackExpStamp);
        sourceStream.getControlAdapter()
          .getHealthState().updateHealth(HealthStateListener.ACK_EXPECTED_STATE, 
                                         HealthState.AMBER);
      }

      // If the destination in a Link add Link specific properties to message
      if( isLink )
      {
        ackexpMsg = (ControlAckExpected)addLinkProps(ackexpMsg);
      }
        
      // If the destination is system or temporary then  the 
      // routingDestination into th message
      if( this.isSystemOrTemp )
      {
        ackexpMsg.setRoutingDestination(routingDestination); 
      }
    
      // Send ackExpected message to destination
      // Using MPIO
      mpio.sendToMe(routingMEUuid, priority, ackexpMsg );
 
    }
    else
    {
      if(TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc,"Unable to send AckExpected as Link not started");
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendAckExpectedMessage");

  }

  /**
   * sendSilenceMessage may be called from SourceStream
   * when a Nack is recevied
   */
  public void sendSilenceMessage(
    long startStamp,
    long endStamp,
    long completedPrefix,
    boolean requestedOnly,      
    int priority,
    Reliability reliability,
    SIBUuid12 stream)
    throws SIResourceException
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendSilenceMessage");

    ControlSilence sMsg;
    try
    {
      // Create new Silence message
      sMsg = cmf.createNewControlSilence();
    }
    catch (Exception e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.sendSilenceMessage",
        "1:849:1.241",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
          "1:856:1.241",
          e });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendSilenceMessage", e);

      throw new SIResourceException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
            "1:867:1.241",
            e },
          null),
        e);
    }

    // As we are using the Guaranteed Header - set all the attributes as
    // well as the ones we want.
    SIMPUtils.setGuaranteedDeliveryProperties(sMsg,
        messageProcessor.getMessagingEngineUuid(), 
        targetMEUuid,
        stream,
        null,
        destinationHandler.getUuid(),
        ProtocolType.UNICASTINPUT,
        GDConfig.PROTOCOL_VERSION);
  
    sMsg.setStartTick(startStamp);
    sMsg.setEndTick(endStamp);
    sMsg.setPriority(priority);
    sMsg.setReliability(reliability);
    sMsg.setCompletedPrefix(completedPrefix);

    // If the destination in a Link add Link specific properties to message
    if( isLink )
    {
      sMsg = (ControlSilence)addLinkProps(sMsg);
    }
    
    // If the destination is system or temporary then  the 
    // routingDestination into th message
    if( this.isSystemOrTemp )
    {
      sMsg.setRoutingDestination(routingDestination); 
    }
 
    // Send message to destination
    // Using MPIO
    // If requestedOnly then this is a response to a Nack so resend at priority+1 
    if( requestedOnly )
      mpio.sendToMe(routingMEUuid, priority+1, sMsg);
    else
      mpio.sendToMe(routingMEUuid, priority, sMsg);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendSilenceMessage");

  }
  
  public MessageItem getValueMessage(long msgStoreID) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getValueMessage", new Long(msgStoreID));
   
    MessageItem messageItem = null;
    try
    {
      messageItem =
        (MessageItem)transmissionItemStream.findById(msgStoreID);
    }
    catch (MessageStoreException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.getValueMessage",
        "1:937:1.241",
        this);
        
      SibTr.exception(tc, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "getValueMessage", e);
      
      throw new SIResourceException(e);
      
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getValueMessage", messageItem);
    return messageItem;
  }

  // This method is only called by the SourceStream in response to
  // a Nack message.
  // msgList is a list of TickRanges representing Value messages.
  // Note that the message has already been assigned a stream ID.
  public List<TickRange> sendValueMessages(
    List msgList,
    long completedPrefix,
    boolean requestedOnly,
    int priority,
    Reliability reliability,
    SIBUuid12 streamId)   // not used for ptp
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendValueMessages",
        new Object[] { msgList,
                       new Long(completedPrefix),
                       targetMEUuid });

    JsMessage jsMsg = null;
        
    TickRange tickRange = null;
    MessageItem msgItem = null;
    long msgId = -1;
    List<TickRange> expiredMsgs = null;

    for (int i = 0; i < msgList.size(); i++)
    {
      tickRange = (TickRange)msgList.get(i);
      msgId = tickRange.itemStreamIndex;

      msgItem = getValueMessage(msgId);
 
      // If the message could not be found on the ItemStream then
      // we asume that it has expired  
      boolean removeMessage = false;
      if ( msgItem == null )
      {
        removeMessage = true;   
      }
      else
      {
        // Attempt to lock the message
        // This will fail if the message has already expired
        // or if it is already locked
        try
        {
          if ( !(msgItem.lockItemIfAvailable(lockID)) )
          {
            // Check whether message is already locked, as if this is a resend
            // after a Nack then it may be
            if ( !msgItem.isLocked() )
            {
              removeMessage = true;
            }
          }
        }
        catch (MessageStoreException e)
        {
          // FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.sendValueMessages",
            "1:1016:1.241",
            this);
            
          SibTr.exception(tc, e);
          
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendValueMessages", e);
          
          throw new SIResourceException(e);          
        }
        
      }
            
      if( removeMessage )
      {      
        if ( expiredMsgs == null )
          expiredMsgs = new ArrayList<TickRange>();
        
        expiredMsgs.add(tickRange);   

        // In this case send Silence instead      
        ControlMessage cMsg = createSilenceMessage(tickRange.valuestamp,
                                                   completedPrefix,
                                                   priority,
                                                   reliability,
                                                   streamId);
                          
        if( isLink )
          cMsg = addLinkProps(cMsg);                             
        
        if( this.isSystemOrTemp )
          cMsg.setRoutingDestination(routingDestination); 
            
        // Send at priority+1 if this is a response to a Nack
        if( requestedOnly )
          mpio.sendToMe(routingMEUuid, priority+1, cMsg);
        else
          mpio.sendToMe(routingMEUuid, priority, cMsg);
                  
      }
      else
      {
        // Retrieve message from MessageItem
        jsMsg = msgItem.getMessage();

        // Modify the streamId if necessary     
        if( jsMsg.getGuaranteedStreamUUID() !=  streamId)
        {
          jsMsg.setGuaranteedStreamUUID(streamId);
        }

        // Modify the targetMEUuid if necessary     
        if( jsMsg.getGuaranteedTargetMessagingEngineUUID().equals(unknownUuid) )
        {
          jsMsg.setGuaranteedTargetMessagingEngineUUID(targetMEUuid);
        }

        // Are there Completed ticks after this Value
        // If so we need to adjust the message to reflect this
        if (tickRange.endstamp > tickRange.valuestamp)
        {
          jsMsg.setGuaranteedValueEndTick(tickRange.endstamp);
        }

        // Update the completedPrefix to current value
        jsMsg.setGuaranteedValueCompletedPrefix(completedPrefix);

        //Store in the message the amount of time it was on the queue
        long waitTime = msgItem.updateStatisticsMessageWaitTime();      
        jsMsg.setMessageWaitTime(waitTime);
                
        // If the destination in a Link add Link specific properties to message
        if( isLink )
        {
          // Prevent any concurrent serialization of the JsMessage that could fail
          // due to having one property set but not the other
          synchronized(msgItem.getSynchUpdateLock())
          {
            jsMsg = addLinkProps(jsMsg);
          }
        }
        
        // As we're re-sending an existing message it'll probably already have a
        // routing destination in it if this is a system or temporary
        // destination so we shouldn't re-set it (in fact we can't
        // set one for tempQs as we don't know what the original one was
        // called as this PtoPOutputHandler will be the catch-all TDReceiver).
        // Defect 349843.
        if( this.isSystemOrTemp && (jsMsg.getRoutingDestination() == null))
        {
          jsMsg.setRoutingDestination(routingDestination);
        }
           
        if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
          UserTrace.traceOutboundSend(jsMsg, 
                                     routingMEUuid, 
                                     destinationHandler.getName(),
                                     destinationHandler.isForeignBus() || destinationHandler.isLink(),
                                     destinationHandler.isMQLink(),
                                     destinationHandler.isTemporary());
           
        // Send message to destination
        // Using MPIO
        // Send at priority+1 because this is a resend after a Nack
        if( requestedOnly )
          mpio.sendToMe(routingMEUuid, priority+1, jsMsg);
        else
          mpio.sendToMe(routingMEUuid, priority, jsMsg);         
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendValueMessages", expiredMsgs);
      
    return expiredMsgs;  
  }

  /* The messageAddCall boolean needs to be set after a right before a call to 
   * the batch handler messagesAdded so that in the result of an exception being 
   * thrown, the handler is unlocked.
   */
  void processAck(ControlAck ackMsg) 
  throws SIRollbackException, 
         SIConnectionLostException, 
         SIResourceException, 
         SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processAck",
        new Object[] { ackMsg });

    List indexList = null;
    SourceStream batchListener = null;
    synchronized(sourceStreamManager)
    {
      indexList = sourceStreamManager.processAck(ackMsg);
      batchListener = sourceStreamManager.getBatchListener(ackMsg);
    }
    long index = 0;
    MessageItem msgItem = null;

    if(indexList != null)
    {         
      TransactionCommon tran = sourceBatchHandler.registerInBatch();
      // A marker to indicate how far through the method we get. 
      boolean messageAddCall = false;      
      
      try
      {      
        TickRange tr = null;
        for (int i = 0; i < indexList.size(); i++)
        {
          tr = (TickRange)indexList.get(i);
          batchListener.addToBatchList(tr);
          
          index = tr.itemStreamIndex;
          try
          {
            msgItem =
              (MessageItem)transmissionItemStream.findById(index);
            
            //it is possible that this is null in the case of a message
            //deleted via the Admin panels 
            if( msgItem != null)
            {  
              Transaction msTran = messageProcessor.resolveAndEnlistMsgStoreTransaction(tran);
              msgItem.remove(msTran, msgItem.getLockID());
            }  
          }
          catch (MessageStoreException e)
          {
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.processAck",
              "1:1191:1.241",
              this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
              new Object[] {
                "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
                "1:1198:1.241",
                e });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(tc, "processAck", e);

            throw new SIResourceException(
              nls.getFormattedMessage(
                "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                new Object[] {
                  "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
                  "1:1209:1.241",
                  e },
                null),
              e);
          }
        }
        
        // Indicate that we are about to unlock using messagesAdded.
        messageAddCall = true;
        sourceBatchHandler.messagesAdded(indexList.size(), batchListener);      
      }
      finally
      {
        // Before exiting this method, need to unlock the batch handler if it was locked.
        if (!messageAddCall)
        try
        {      
          sourceBatchHandler.messagesAdded(0);
        }
        catch (SIResourceException e)
        {
          // No FFDC code needed, This will allow for any exceptions that were thrown to
          // be rethrown instead of overiding with a batch handler error.
          if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
            SibTr.exception(tc, e);               
        }
        
      }
    }
    
    // Before we exit, let the source stream manager take care of any pending flushes
    sourceStreamManager.attemptFlushIfNecessary();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processAck");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener#messageEventOccurred(int, com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage, com.ibm.ws.sib.msgstore.Transaction)
   */
  public void messageEventOccurred(int event,
                                   SIMPMessage msg,
                                   TransactionCommon tran) throws SIResourceException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "messageEventOccurred", new Object[]{new Integer(event),msg,tran});

    if(event == MessageEvents.POST_COMMITTED_TRANSACTION)
    {
      eventPostCommit(msg);
    }
    else if(event == MessageEvents.POST_COMMIT_ADD)
    {
      eventPostAdd(msg, tran, false);
    }
    else if(event == MessageEvents.POST_ROLLBACK_ADD)
    {
      eventPostAdd(msg, tran, true);
    }
    else
    {
      SIErrorException e = new SIErrorException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0001",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
            "1:1275:1.241" },
          null));

      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.messageEventOccurred",
        "1:1281:1.241",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
          "1:1288:1.241" });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "messageEventOccurred", e);

      throw e;
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "messageEventOccurred");
  }

  private ControlSilence createSilenceMessage(
      long tick,
      long completedPrefix,
      int priority,
      Reliability reliability,
      SIBUuid12 stream)
      throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "createSilenceMessage", 
        new Object[]{new Long(tick), 
                     new Long(completedPrefix), 
                     new Integer(priority), 
                     reliability, 
                     stream});
                     
    ControlSilence sMsg = null;
    try
    {
      // Create new Silence message
      sMsg = cmf.createNewControlSilence();
    }
    catch (Exception e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.createSilenceMessage",
        "1:1327:1.241",
         this);
      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
      new Object[] {
        "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
        "1:1333:1.241",
        e } );
        
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
           SibTr.exit(tc, "createSilenceMessage", e);

      throw new SIResourceException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
            "1:1344:1.241",
            e },
          null),
           e);
    }

    // As we are using the Guaranteed Header - set all the attributes as 
    // well as the ones we want.
    SIMPUtils.setGuaranteedDeliveryProperties(sMsg,
        messageProcessor.getMessagingEngineUuid(), 
        targetMEUuid,
        stream,
        null,
        destinationHandler.getUuid(),
        ProtocolType.UNICASTINPUT,
        GDConfig.PROTOCOL_VERSION);
  
    sMsg.setStartTick(tick);
    sMsg.setEndTick(tick);
    sMsg.setPriority(priority);
    sMsg.setReliability(reliability);
    sMsg.setCompletedPrefix(completedPrefix);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "createSilenceMessage", sMsg);
    return sMsg;
  }

  /**
  * Called when a message receives an eventCommittedAdd from the messageStore. i.e.
  * a message has been transactionally committed after being put in the messageStore.
  *
  * @param msg The message which has been committed
  * @param tran The transaction used to commit the message
  * @throws SIStoreException Thrown if there is ANY problem
  * @see com.ibm.ws.sib.store.AbstractItem#eventCommittedAdd(com.ibm.ws.sib.msgstore.Transaction)
  */
  private void eventPostAdd(SIMPMessage msg,
                            TransactionCommon transaction,
                            boolean rollback) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventPostAdd", new Object[]{msg,transaction,new Boolean(rollback)});
     
    JsMessage jsMsg = msg.getMessage();
    boolean sendMessage = false;
    
    if(rollback)
    {
      sendMessage = rollbackInsert((MessageItem)msg);
    }
    else
    {
      sendMessage = commitInsert((MessageItem)msg);
    }
    
    // A message is now committed to sourceStream. Run reallocation if 
    // reallocator was running while the message was committing. Also
    // don`t send the message.
    if (isReallocationRequired(msg))
    {
      ((BaseDestinationHandler)destinationHandler).requestReallocation();
      sendMessage = false;      
    }
    
    // sendMessage will be true unless the message is beyond the inDoubt window
    // or we have guessed that this is the correct stream to use and 
    // had to move the message because WLM told us to.
    if( sendMessage && !rollback )
    {
      // If this is a message not silence then
      // attempt to lock the message to prevent expiry
      // as we are about to send it now
      boolean msgLocked = false;
      try
      {
        msgLocked = msg.lockItemIfAvailable(lockID);

        // It's just possible that a Nack has come in for this message before we've
        // even sent it (because there are load's of producers concurrently sending in
        // messages, which really srews around the order of the messages!). In that
        // case the message would already be locked, but by the nack processing, not us.
        // Either way, we need to keep the message.
        if(!msgLocked && msg.isLocked() )
        {
          msgLocked = true;
        }
        	
      }
      catch(MessageStoreException e)
      {
        // FFDC
        FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.eventPostAdd",
            "1:1438:1.241",
            this);  
                    
        SibTr.exception(tc, e); 
        
        // Leave the msg where it is and throw exception 
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "eventPostAdd", e);
        
        throw new SIResourceException(e);
      }
      
      if (!msgLocked)
      {
        // We failed to lock the message so it must have already expired or been
        // deleted already, call removeMessage, which will try to remove the message
        // or tell us that the message has already been removed
        boolean msgRemoved = sourceStreamManager.removeMessage(msg);
        
        // If we've just removed the message then we better send a silence in its place
        if(msgRemoved)
          rollback = true;
        // Otherwise, someone else has removed it, probably because they've already processed
        // it, so we should do nothing with it.
        else
          sendMessage = false;
      }
    }
          
    // In this case send Silence instead      
    if( sendMessage )  
    {
      if( rollback )
      {  
        ControlMessage cMsg = createSilenceMessage(jsMsg.getGuaranteedValueValueTick(),
                                                   jsMsg.getGuaranteedValueCompletedPrefix(),
                                                   msg.getPriority(),
                                                   msg.getReliability(),
                                                   msg.getMessage().getGuaranteedStreamUUID());    
      
        // If the destination in a Link add Link specific properties to message
        if( isLink )
        {
          cMsg = addLinkProps(cMsg);
        }
        
        mpio.sendToMe(
            routingMEUuid,
               cMsg.getPriority().intValue(),
               cMsg);
      }  
      else
      {
        // If the destination in a Link add Link specific properties to message
        if( isLink )
        {
          // Prevent any concurrent serialization of the JsMessage that could fail
          // due to having one property set but not the other
          synchronized(((MessageItem)msg).getSynchUpdateLock())
          {
            jsMsg = addLinkProps(jsMsg);
          }
        }
          
        //Store in the message the amount of time it was on the queue
        long waitTime = msg.updateStatisticsMessageWaitTime();      
        jsMsg.setMessageWaitTime(waitTime);
      
        if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
          UserTrace.traceOutboundSend(jsMsg, 
                                     routingMEUuid,
                                     destinationHandler.getName(),
                                     destinationHandler.isForeignBus() || destinationHandler.isLink(),
                                     destinationHandler.isMQLink(),
                                     destinationHandler.isTemporary());

        //  Send message using MPIO
        mpio.sendToMe(
            routingMEUuid,
            jsMsg.getPriority().intValue(),
            jsMsg);
        
        // We have sent the msg over the wire so we can now drop the jsmessage for
        // now so that we don't keep it is memory for to long
        msg.releaseJsMessage();
        
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "eventPostAdd");
  }

  /**
  * Called when the transaction is committed (not the add of a message). This only
  * occurs if the message was best_effort and never put to the queue.
  *
  * @param msg The message which has been committed
  * @param tran The transaction used to commit the message
  * @throws SIStoreException Thrown if there is ANY problem
  * @see com.ibm.ws.sib.store.AbstractItem#eventCommittedAdd(com.ibm.ws.sib.msgstore.Transaction)
  */
  private void eventPostCommit(SIMPMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "eventPostCommit", msg);

    JsMessage jsMsg = msg.getMessage();
    
    if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
      UserTrace.traceOutboundSend(jsMsg, 
                                 routingMEUuid,
                                 destinationHandler.getName(),
                                 destinationHandler.isForeignBus() || destinationHandler.isLink(),
                                 destinationHandler.isMQLink(),
                                 destinationHandler.isTemporary());

    //  Send message using MPIO
    mpio.sendToMe(routingMEUuid,
                        msg.getPriority(),
                        jsMsg);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "eventPostCommit");
  }

  /** toString
   */
  public String toString()
  {
    return "PtoPOutputHandler: "
           + destinationHandler.getName()
           + " on "
           + transmissionItemStream.getLocalizingMEUuid().toString();
  }

  /**
   * @see com.ibm.ws.sib.processor.impl.OutputHandler#getTargetMEUuid()
   */
  public SIBUuid8 getTargetMEUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getTargetMEUuid");
      SibTr.exit(tc, "getTargetMEUuid", transmissionItemStream.getLocalizingMEUuid());
    }

    return transmissionItemStream.getLocalizingMEUuid();
  }

  public SIBUuid8 getRoutingMEUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getRoutingMEUuid");
      SibTr.exit(tc, "getRoutingMEUuid", routingMEUuid);
    }
    
    return routingMEUuid;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.OutputHandler#commitInsert(com.ibm.ws.sib.processor.impl.store.MessageItem)
   */
  public boolean commitInsert(MessageItem msg) throws SIResourceException
  {   
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "commitInsert", msg); 
    boolean updated = updateSourceStream(msg, false);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "commitInsert", new Boolean(updated));
    return updated;
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.OutputHandler#rollbackInsert(com.ibm.ws.sib.processor.impl.store.MessageItem)
   */
  public boolean rollbackInsert(MessageItem msg) throws SIResourceException
  {  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "rollbackInsert", msg);
    boolean updated = updateSourceStream(msg, true);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "rollbackInsert", new Boolean(updated));
    return updated;
  }

  /**
   * This method checks to see if rollback is required
   *
   * @param transaction The transaction to rollback.
   */
  protected void handleRollback(LocalTransaction transaction)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "handleRollback", transaction);

    // Roll back the transaction if we created it.
    if (transaction != null)
    {
      try
      {
        transaction.rollback();
      }
      catch (SIException e)
      {
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.handleRollback",
          "1:1644:1.241",
          this);

        SibTr.exception(tc, e);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleRollback");
  }
  
  /**
   * Returns true if the given message was uncommitted at the time of the
   * last reallocation of this stream. If so we need to reallocate the
   * stream again.
   * @param msg
   * @return boolean
   * @throws SIResourceException
   */
  public boolean isReallocationRequired(SIMPMessage msg) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isReallocationRequired", msg);
      
    
    // return flag indicating whether the message should be reallocated
    boolean reallocate = false;    

    // Ask the ssm if the stream needs reallocation
    reallocate = sourceStreamManager.isReallocationRequired(msg);
        
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isReallocationRequired");
      
    return reallocate;    
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.OutputHandler#rollbackInsert(com.ibm.ws.sib.processor.impl.store.MessageItem)
   */
  public boolean updateSourceStream(MessageItem msg, boolean rollback) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "updateSourceStream", new Object[] { msg, new Boolean(rollback)});
      
    
    // return flag indicating whether the message can be sent downstream
    boolean sendMessage = true;    

    // Write the value message to the stream
    // This will also write a range of Completed ticks between
    // the previous Value tick and the new one
    sendMessage = sourceStreamManager.updateSourceStream(msg, rollback);
        
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateSourceStream");
      
    return sendMessage;    
  }

  /**
   * Creates a FLUSHED message for sending
   *
   * @param stream The UUID of the stream the message should be sent on
   * @return the new FLUSHED message
   * @throws SIResourceException if the message can't be created.
   */
  private ControlFlushed createControlFlushed(SIBUuid12 stream)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createControlFlushed", new Object[] {stream});

    ControlFlushed flushedMsg;

    // Create new message
    try
    {
      flushedMsg = cmf.createNewControlFlushed();
    }
    catch (MessageCreateFailedException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.createControlFlushed",
        "1:1730:1.241",
        this);
        
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.exception(tc, e);
        SibTr.exit(tc, "createControlFlushed", e);
      }
      
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
          "1:1742:1.241",
          e });

      throw new SIResourceException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
            "1:1750:1.241",
            e },
          null),
        e);
    }

    // As we are using the Guaranteed Header - set all the attributes as
    // well as the ones we want.
    SIMPUtils.setGuaranteedDeliveryProperties(flushedMsg,
        messageProcessor.getMessagingEngineUuid(), 
        targetMEUuid,
        stream,
        null,
        destinationHandler.getUuid(),
        ProtocolType.UNICASTINPUT,
        GDConfig.PROTOCOL_VERSION); 
  
    flushedMsg.setPriority(SIMPConstants.CTRL_MSG_PRIORITY);
    flushedMsg.setReliability(Reliability.ASSURED_PERSISTENT);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createControlFlushed");

    return flushedMsg;
  }

  /**
   * Creates a NOTFLUSHED message for sending
   *
   * @param stream The UUID of the stream the message should be sent on.
   * @param reqID The request ID that the message answers.
   * @return the new NOTFLUSHED message.
   * @throws SIResourceException if the message can't be created.
   */
  private ControlNotFlushed createControlNotFlushed(SIBUuid12 stream, long reqID)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createControlNotFlushed", new Object[] {stream, new Long(reqID)});

    ControlNotFlushed notFlushedMsg;

    // Create new message
    try
    {
      notFlushedMsg = cmf.createNewControlNotFlushed();
    }
    catch (MessageCreateFailedException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.createControlNotFlushed",
        "1:1803:1.241",
        this);
        
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.exception(tc, e);
        SibTr.exit(tc, "createControlNotFlushed", e);
      }
      
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
      new Object[] {
        "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
        "1:1815:1.241",
        e } );

      throw new SIResourceException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
            "1:1823:1.241",
            e },
          null),
        e);
    }

    // As we are using the Guaranteed Header - set all the attributes as
    // well as the ones we want.
    SIMPUtils.setGuaranteedDeliveryProperties(notFlushedMsg,
        messageProcessor.getMessagingEngineUuid(), 
        targetMEUuid,
        stream,
        null,
        destinationHandler.getUuid(),
        ProtocolType.UNICASTINPUT,
        GDConfig.PROTOCOL_VERSION);
    
    notFlushedMsg.setPriority(SIMPConstants.CTRL_MSG_PRIORITY);
    notFlushedMsg.setReliability(Reliability.ASSURED_PERSISTENT);
    
    notFlushedMsg.setRequestID(reqID);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createControlNotFlushed");

    return notFlushedMsg;
  }

  /**
   * Restores the GD source streams
   *
   */
  public void reconstitutePtoPSourceStreams(StreamSet streamSet, int startMode ) 
  throws SIRollbackException, SIConnectionLostException, SIResourceException, SIErrorException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reconstitutePtoPSourceStreams", new Object[] {streamSet, new Integer(startMode)});

    sourceStreamManager.reconstituteStreamSet(streamSet);
    
    // Don't do flush if we are asked to start in recovery mode 
    if(   ((startMode & JsConstants.ME_START_FLUSH ) == JsConstants.ME_START_FLUSH )
       && ((startMode & JsConstants.ME_START_RECOVERY ) == 0 ) )
   {
      this.sendFlushedMessage(null, streamSet.getStreamID());
      // Now change streamID of streamSet
      streamSet.setStreamID(new SIBUuid12());
      // This calls requestUpdate on the StreamSet Item which will
      // cause a callback to the streamSet.getPersistentData() by msgstore
      Transaction tran = messageProcessor.getTXManager().createAutoCommitTransaction();
      try
      {
        streamSet.requestUpdate(tran);
      }
      catch (MessageStoreException e)
      {
         // MessageStoreException shouldn't occur so FFDC.
         FFDCFilter.processException(
         e,
         "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.reconstitutePtoPSourceStreams",
         "1:1883:1.241",
         this);
  
         SibTr.exception(tc, e);
         if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "reconstitutePtoPSourceStreams", "SIStoreException");
         throw new SIResourceException(e);
      }      
    }
        
    NonLockingCursor cursor = null;
    try
    {
      cursor = transmissionItemStream.newNonLockingItemCursor(
        new ClassEqualsFilter(MessageItem.class));
      cursor.allowUnavailableItems();  

      MessageItem msg = (MessageItem)cursor.next();
      while(msg!=null)
      {
        // Change streamID in message to streamID of StreamSet
        // If we are restoring from a stale backup this will
        // and the restoreMessage method puts it on a new SourceStream
        if( msg.getGuaranteedStreamUuid() !=  streamSet.getStreamID())
        {
          msg.setGuaranteedStreamUuid(streamSet.getStreamID());
        }
              
        //add all messages back in to the streams
        if(!(msg.isAdding() || msg.isRemoving()))
        {
          // commit those which are not in doubt
          sourceStreamManager.restoreMessage(msg, true);
        }
        else
        {
          // add to stream in uncommitted state
          sourceStreamManager.restoreMessage(msg, false);
        }
        
        msg = (MessageItem)cursor.next();
      }
      
      // Consolidate all streams which may have been reconstituted
      // This is necessary as messages may have been added out of order
      // This will return a list of the messageStoreIds of all the
      // messages which need to be locked because they are inside the
      // sendWindows of the streams which they were added to and so may
      // already have been sent 
      List msgsToLock = sourceStreamManager.consolidateStreams(startMode);
      
      // Run through locking all messages in list so they won't expire.      
      // watch out as list is Longs msgstore ids
      long msgId = 0;
      MessageItem msgItem = null;
      for (int i = 0; i < msgsToLock.size(); i++)
      {
        msgId = ((Long)msgsToLock.get(i)).longValue();
        msgItem = (MessageItem)transmissionItemStream.findById(msgId); 
        if( msgItem != null )
          msgItem.lockItemIfAvailable(lockID);
        else
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "message wasn't there when we tried to lock it");
        }
      }    
    }
    catch (MessageStoreException e)
    {
      // MessageStoreException shouldn't occur so FFDC.
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.reconstitutePtoPSourceStreams",
        "1:1956:1.241",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
          "1:1963:1.241",
          e });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "reconstitutePtoPSourceStreams", e);

      throw new SIResourceException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
            "1:1974:1.241",
            e },
          null),
        e);
    }
    finally
    {
      if (cursor != null)
        cursor.finished();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())  
      SibTr.exit(tc, "reconstitutePtoPSourceStreams");      
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl#sendFlushedMessage(com.ibm.ws.sib.utils.SIBUuid12)
   * 
   * Sends an 'I am flushed' message in response to a query from a target
   */
  public void sendFlushedMessage(SIBUuid8 ignoreUuid, SIBUuid12 streamID) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "sendFlushedMessage", new Object[]{ignoreUuid, streamID});

    ControlFlushed flushMsg = createControlFlushed(streamID);
    // If the destination in a Link add Link specific properties to message
    if( isLink )
    {
      flushMsg = (ControlFlushed)addLinkProps(flushMsg);
    }
    
    // If the destination is system or temporary then  the 
    // routingDestination into th message
    if( this.isSystemOrTemp )
    {
      flushMsg.setRoutingDestination(routingDestination); 
    }
 
    mpio.sendToMe(routingMEUuid, SIMPConstants.MSG_HIGH_PRIORITY, flushMsg);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "sendFlushedMessage");
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl#sendNotFlushedMessage(com.ibm.ws.sib.utils.SIBUuid12)
   * 
   * Sends an 'I am not flushed' message in response to a query from a target
   */
  public void sendNotFlushedMessage(SIBUuid8 ignore, SIBUuid12 streamID, long requestID) 
  throws SIResourceException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "sendNotFlushedMessage", 
        new Object[]{ignore, streamID, new Long(requestID)});

    ControlNotFlushed notFlushed = createControlNotFlushed(streamID, requestID);
    notFlushed = sourceStreamManager.stampNotFlushed(notFlushed);
    
    // If the destination in a Link add Link specific properties to message
    if( isLink )
    {
      notFlushed = (ControlNotFlushed)addLinkProps(notFlushed);
      // The following is set so we can create the linkReceiver correctly on the 
      // other side of the link - 499581
      notFlushed.setRoutingDestination(routingDestination);
    }
    else if( this.isSystemOrTemp )
    {
      // If the destination is system or temporary then  the 
      // routingDestination into th message
      notFlushed.setRoutingDestination(routingDestination); 
    }
    
 
    mpio.sendToMe(routingMEUuid, SIMPConstants.MSG_HIGH_PRIORITY, notFlushed);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "sendNotFlushedMessage");
  }

  /*
   * Reallocates messages from the sourcestream to alternative localisations
   * 
   * Called when TRM informs us that this localistaion has become unreachable.
   * Called when TRM informs us that this localisation has become reachable.
   * Called when TRM informs us that another localisation for the same destination has become reachable.
   * Called when an Admin action has forced a flush of the streams 
   * 
   * Warning: Callers MUST NOT hold any locks as the ReallocationLock is at the top of
   *          the lock hierarchy.
   *          
   * @param destination The destination handler associated with this outputhandler
   * @param txManager Under which to create a transaction for our work
   */
   
  public void reallocateMsgs(DestinationHandler destination, boolean allMsgs, boolean forceRemove) throws SIResourceException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reallocateMsgs", 
      new Object[]{this.toString(), destination, new Boolean(allMsgs), new Boolean(forceRemove)});  
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
      SibTr.debug(tc, "Reallocating messages queued for " + destination.getName() + " from ME " + messageProcessor.getMessagingEngineUuid() + " to ME " + targetMEUuid);
    
    //Attempt to reallocate the messages to another localisation of the destination,
    //or to the exception destination, or discard the messages.

    ExceptionDestinationHandlerImpl exceptionDestinationHandlerImpl = null;
    LocalTransaction transaction = null;
    int transactionSize = -1;
    
    // PK57432 We are able to send messages, unless we encounter a guess on any stream
    // (a guess on one stream will mean all other streams would also get guesses).
    boolean streamCanSendMsgs = true;
    
    // Obtain and release an exclusive lock on the destination to ensure a
    // send is not taking place
    LockManager reallocationLock = 
      ((BaseDestinationHandler)destination).getReallocationLockManager();
    reallocationLock.lockExclusive();
    // Release Lock
    reallocationLock.unlockExclusive();    
   
    synchronized(this) 
    {
      StreamSet streamSet = sourceStreamManager.getStreamSet();
  
      if (streamSet == null) 
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "reallocateMsgs");
        return;
      } 
      
      Iterator stream_it = streamSet.iterator();
      
      //Iterate over all streams in the stream set
      while(stream_it.hasNext())
      {
        transactionSize = 0;
        transaction = destination.getTxManager().createLocalTransaction(false);
        
        SourceStream stream = (SourceStream) stream_it.next(); 
    
        // We need to leave the stream in the same guess state as it started
        // in, otherwise, if we incorrectly change it from 'guesses' to
        // 'no guesses' any other thread currently halfway through processing
        // a message may incorrectly think that it can send a message - this
        // leaves the message locked to this OutputHandler and cannot be
        // reallocated later.
        boolean streamStateChecked = false;
        boolean oldGuessState = stream.containsGuesses();
        
        // prevent the stream from sending any more messages while
        // we are reallocating. This avoids the need to lock the 
        // stream      
        stream.guessesInStream();  
  
        
        // Obtain a list of all messages which are in the stream
        // but have not yet been sent, or all the messages in the 
        // stream if we have been asked to reallocate everything
        List indexList = null;
        if( allMsgs == true)
          indexList = stream.getAllMessagesOnStream();
        else
          indexList = stream.getMessagesAfterSendWindow();
  
        ArrayList<SIMPMessage> markedForSilence = new ArrayList<SIMPMessage>();
        Iterator indexList_it = indexList.iterator();
  
        // Populate msgList with messages indexed in indexList
        long index = -1;
        while (indexList_it.hasNext())
        {
          index = ((Long)indexList_it.next()).longValue();
          MessageItem msg = null;
          
          try
          {
            msg = (MessageItem) transmissionItemStream.findById(index);
          }
          catch (MessageStoreException e)
          {
            // FFDC
            FFDCFilter.processException(
              e,
              "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.reallocateMsgs",
              "1:2168:1.241",
              this);
              
            SibTr.exception(tc, e);
            
            try
            {
              // Attempt to rollback any outstanding tran
              transaction.rollback();
            }
            catch(SIException ee)
            {
              // No FFDC code needed
              SibTr.exception(tc, ee);
            }          
            
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
              SibTr.exit(tc, "reallocateMsgs", e);
            
            return;
          }
          // Check for null (message expired)
          if (msg!=null)
          {
            streamStateChecked = true;
            
            // It's possible that the message was sent to an alias that mapped
            // to this destination, in which case we need to take into account any
            // scoped ME sets
            DestinationHandler scopingDestination = destination;
            
            // For each message we need to reallocate
            OutputHandler outputHandler = null;
            
            streamCanSendMsgs = false;  //assume that the existing stream cannot
            
            boolean removeMsg = true; //we assume that we will remove the message from
                              //the item stream and write silence into the src stream
            
            boolean sendToInputHandler = false; 
            boolean newStreamIsGuess = false;       
            if (!destination.isToBeDeleted())
            {
              
                // If this message has a routingDestination set in it it may be
                // because the message was fixed on a specific ME or set of MEs.
                // Pull out the ME to use in the reallocation decision.
                SIBUuid8 fixedME = null;
                JsDestinationAddress routingDestinationAddr = msg.getMessage().getRoutingDestination();
                if(routingDestinationAddr != null)
                {
                  // Pull out any fixed ME
                  fixedME = routingDestinationAddr.getME();
                  
                  // If we're not fixed then check to see if we were sent to an alias, and if
                  // so use that for routing from now on.
                  if(fixedME == null)
                  {
                    DestinationHandler routingDestHandler;
                    try
                    {
                      routingDestHandler = 
                        messageProcessor.getDestinationManager().getDestination(routingDestinationAddr, false);
                      if(routingDestHandler.isAlias())
                      {
                        scopingDestination = routingDestHandler;
                      }
                    }
                    catch (SIException e)
                    {
                      // No FFDC code needed        
                      if (TraceComponent.isAnyTracingEnabled())
                        SibTr.exception(tc, e);
  
                      // If we can't find the alias destination then we've missed our chance
                      // to scope the message. This is acceptable - there is no guarantee on
                      // message scoping if the config changes after sending the message so
                      // ignore this and use the original destination.
                    }
                  }
                }
                
                //Obtain an output handler for this destination  
                outputHandler = scopingDestination.
                  choosePtoPOutputHandler(fixedME,
                                          transmissionItemStream.getLocalizingMEUuid(), // preferred ME                                           
                                         (!msg.isFromRemoteME()), false, null);
  
                if(outputHandler!=null)
                {
                  newStreamIsGuess = outputHandler.isWLMGuess();
                  //determine if this is a new output handler or not
                  if(outputHandler==this)
                  {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                      SibTr.debug(tc, "Leaving message " + msg.getMessage().getSystemMessageId() +
                                        " on this stream (" + newStreamIsGuess + ")");
                    
                    // PK57432 WLM may have guessed to come to this conclusion,
                    // because all candidates are unavailable.
                    // We should not attempt to transmit the messages in this case.
                    streamCanSendMsgs = ! newStreamIsGuess; 
  
                    removeMsg = false;  //msg is in the correct place so we
                                        //do not need to remove it
  
                    // If WLM has re-chosen this OutputHandler it means either:
                    //  a) The target is now available
                    //  b) The target is unavailable but there are no other available targets
                    //     (otherwise it would have chosen one of those instead)
                    // In either case WLM with always return the same choice for every message
                    // on this stream so there is no benefit in continuing to iterate through
                    // the msgs as they will all stay on this stream.
                    
                    if(fixedME == null && scopingDestination.equals(destination)) 
                    {
                      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Exiting reallocate due to no better options");
  
                      //If WLM guessed the same handler without any restriction being
                      //forced onto it but we're not able to re-send the messages
                      // then exit the msg loop as there's no point reallocating more
                      // messages on this stream after a guess.
                      break;
                    }
                  }
                  else
                  {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                      SibTr.debug(tc, "Reallocating message " + msg.getMessage().getSystemMessageId() + " to alternative ME");
                    
                    // WLM chose an alternative outputhandler so get the inputhandler
                    // and resend the message to it (allowing for the preferredME)
                    sendToInputHandler = true;
                  }
                }//end if outputHandler!=null
                else
                {
                  if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Leaving message " + msg.getMessage().getSystemMessageId() +
                                      " on this stream due to no OutputHandler!");
  
                  removeMsg = false; // No output handler but that doesnt mean we remove the msg
                }
              
            } 
            else
            {
              if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Discarding message " + msg.getMessage().getSystemMessageId() + " due to deleted queue");
  
              if (!messageProcessor.discardMsgsAfterQueueDeletion())
              {
                /*
                 * The destination is being removed so messages should be moved to the exception destination
                 */
                 
                if (exceptionDestinationHandlerImpl == null)
                {
                  //Create an exception destination handler
                  exceptionDestinationHandlerImpl = 
                     (ExceptionDestinationHandlerImpl) messageProcessor.createExceptionDestinationHandler(null);
     
                }
                
                String destName = destinationHandler.getName();
                if(destinationHandler.isLink())
                  destName = ((LinkHandler)destinationHandler).getBusName();
                
                final UndeliverableReturnCode rc = 
                   exceptionDestinationHandlerImpl.handleUndeliverableMessage(msg
                                                                          ,transaction
                                                                          ,SIRCConstants.SIRC0032_DESTINATION_DELETED_ERROR
                                                                          ,new String[] {destName,
                                                                                         messageProcessor.getMessagingEngineName()}
                                                                         );
               
                if (rc == UndeliverableReturnCode.ERROR || rc == UndeliverableReturnCode.BLOCK)
                {
                  // Messages could not be moved so dont bother trying the rest. Just flag an error.
                  
                  SIErrorException e = new SIErrorException(nls.getFormattedMessage(
                      "DESTINATION_DELETED_ERROR_CWSIP0550",
                      new Object[] { destinationHandler.getName(), destinationHandler.getUuid().toString() },
                      null));
  
                  FFDCFilter.processException(
                    e,
                    "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.reallocateMsgs",
                    "1:2372:1.241",
                    this);
                  
                  SibTr.exception(tc, e);
                  
                  removeMsg = false;
                  break;
                }
                 
                transactionSize++;
              }
              // else remove will be true 
            } 
            
            if(sendToInputHandler)
            {
              // send this message to a new input handler 
              InputHandler inputHandler = scopingDestination.getInputHandler(ProtocolType.UNICASTINPUT,
                                                                             null,
                                                                             msg.getMessage());
              try
              {
                JsMessage message = msg.getMessage().getReceived();
                
                MessageItem messageItem = new MessageItem(message);
                
                // Copy across the original message's prefer local setting
                messageItem.setPreferLocal(msg.preferLocal());
  
                inputHandler.handleMessage(messageItem, 
                                           transaction, 
                                           messageProcessor.getMessagingEngineUuid());
                transactionSize++;
              }
              catch (SIException e)
              {
                // No FFDC code needed        
                handleReallocationFailure(e, destination, transaction);   
              } 
              catch (MessageCopyFailedException e)
              {
                // No FFDC code needed
                handleReallocationFailure(e, destination, transaction);                
              }
            }          
            
            // Remove message from itemstream
            if (removeMsg || forceRemove )
            {
              
              Transaction msTran = messageProcessor.resolveAndEnlistMsgStoreTransaction(transaction);
              try
              {
                // remove it from this itemstream
                msg.remove(msTran, msg.getLockID());
                transactionSize++;  
                markedForSilence.add(msg);             
              }
              catch(MessageStoreException e)
              {
                // No FFDC code needed
                handleReallocationFailure(e, destination, transaction);
              }          
            }
            
            // Batch up the transactional operations.
            if (transactionSize > SIMPConstants.REALLOCATION_BATCH_SIZE)
            {
              //Commit the transaction and start another one
              try
              {
                transaction.commit();
                transaction = destination.getTxManager().createLocalTransaction(false);
                transactionSize = 0;
                
                // Turn markedForSilence to silence
                Iterator<SIMPMessage> markedForSilence_it = markedForSilence.iterator();
                while(markedForSilence_it.hasNext())
                {
                  SIMPMessage silenceMsg = markedForSilence_it.next();
                  stream.writeSilenceForced(silenceMsg);
                }
                // reset markedForSilence
                markedForSilence = new ArrayList<SIMPMessage>();
                
              } catch (SIException e)
              {
                // No FFDC code needed
                handleReallocationFailure(e, destination, transaction);                                                                        
              }
            }
          }
        }
        
        // Commit transaction
        try
        {
          transaction.commit();    
                  
          // Turn markedForSilence to silence
          Iterator<SIMPMessage> markedForSilence_it = markedForSilence.iterator();
          while(markedForSilence_it.hasNext())
          {
            SIMPMessage silenceMsg = markedForSilence_it.next();
            stream.writeSilenceForced(silenceMsg);
          }
              
        } 
        catch (SIException e)
        {
          // No FFDC code needed
          handleReallocationFailure(e, destination, transaction);
        }
        
        // If there weren't any messages driven to re-calculate the guess state of the stream,
        // make sure the stream is set back to how it was before we started.
        if(!streamStateChecked)
      	  streamCanSendMsgs = !oldGuessState;
                
        // Drive the transmit of the remaining messages on the stream
        if (streamCanSendMsgs)
          stream.noGuessesInStream();      
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reallocateMsgs");
  }

  private void handleReallocationFailure(Exception e, DestinationHandler destination, LocalTransaction transaction)
   throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "handleReallocationFailure", 
        new Object[]{e, destination, transaction});
    // Cannot put message to exception destination, so FFDC
    FFDCFilter.processException(
      e,
      "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.handleReallocationFailure",
      "1:2512:1.241",
      this);   
      
    SibTr.exception(tc, e);
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc,"handleReallocationFailure",e);
                                        
    // Roll back the transaction if we created it.
    if (transaction != null)
    { 
      try
      {
        transaction.rollback();
      }
      catch (SIException ee)
      {
        // FFDC
        FFDCFilter.processException(
          ee,
          "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.handleReallocationFailure",
          "1:2533:1.241",
          this);
                
        SibTr.exception(tc, ee);
        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
            "1:2540:1.241",
            ee,
            destination.getName()});
        
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
          SibTr.exit(tc, "handleReallocationFailure", "SIStoreException");               
        
        throw new SIErrorException(
          nls.getFormattedMessage(
            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
            new Object[] {
              "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
              "1:2552:1.241",
              ee,
              destination.getName()},
          null),
          ee);
      }
    }
    
    throw new SIResourceException(
      nls.getFormattedMessage(
        "INTERNAL_MESSAGING_ERROR_CWSIP0003",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.PtoPOutputHandler",
          "1:2565:1.241",
          e,
          destination.getName()},
          null),
      e);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.OutputHandler#isWLMGuess()
   */
  public boolean isWLMGuess()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isWLMGuess");
      SibTr.exit(tc, "isWLMGuess", Boolean.valueOf(isGuess));
    }
    
    return isGuess;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.OutputHandler#setWLMGuess(boolean)
   */
  public void setWLMGuess(boolean guess)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setWLMGuess", new Boolean(guess));

    this.isGuess = guess;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "setWLMGuess");
  }

  /**  
   * @return boolean true if this outputhandler's itemstream has reached QHighMessages
   */
  public boolean isQHighLimit()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "isQHighLimit");

    boolean limited = transmissionItemStream.isQHighLimit();
    
    // Update health state if necessary
    if (limited && !_qHigh)
    {
      _qHigh = true;
      sourceStreamManager
        .getStreamSetRuntimeControl()
        .getHealthState().updateHealth(HealthStateListener.STREAM_FULL_STATE, 
                                       HealthState.AMBER);
      am.create(SIMPConstants.HEALTH_QHIGH_TIMEOUT, new AlarmListener(){
        public void alarm(Object arg0) 
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "alarm");
          if (!isQHighLimit())
          {
            _qHigh = false;
            sourceStreamManager
            .getStreamSetRuntimeControl()
            .getHealthState().updateHealth(HealthStateListener.STREAM_FULL_STATE, 
                                           HealthState.GREEN);
          }
          else
            am.create(SIMPConstants.HEALTH_QHIGH_TIMEOUT, this);   
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "alarm");
        }        
      });
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "isQHighLimit", new Boolean(limited));

    return limited;
  }
  
 /**
  * This method should only be called when the PtoPOutputHandler was created
  * for a Link with an unknown targetCellule and WLM has now told us correct
  * targetCellule.
  */
  public void updateTargetCellule(SIBUuid8 targetMEUuid) throws SIResourceException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "updateTargetCellule", targetMEUuid);
     
    this.targetMEUuid = targetMEUuid;
    
    sourceStreamManager.updateTargetCellule(targetMEUuid);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "updateTargetCellule");
   
   }

  /**
   * This method should only be called when the PtoPOutputHandler was created
   * for a Link. It is called every time a message is sent 
   */
   public void updateRoutingCellule( SIBUuid8 routingME ) 
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.entry(tc, "updateRoutingCellule", routingME);
     
     this.routingMEUuid = routingME;
    
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "updateRoutingCellule");
  }

  public SourceStreamManager getSourceStreamManager()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getSourceStreamManager");
      SibTr.exit(tc, "getSourceStreamManager", sourceStreamManager);
    }

    return sourceStreamManager;
  }
  
  /**
   * Ensure all source streams are flushed (and therefore in-doubt
   * messages are resolved).  This is done in preparation for
   * deleting the destination.  If the delete is deferred then
   * this code automatically redrives the delete when possible.
   * 
   * @return true if the source stream is flushed and the delete
   * can continue, otherwise the delete must be deferred.
   */
  public boolean flushAllForDelete()
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "flushAllForDelete");

    synchronized (this)
    {
      // Flush may have completed, if so then return
      // without starting another one.
      if (flushedForDeleteSource)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "flushAllForDelete", Boolean.TRUE);
        return true;
      }

      // Short circuit if flush already in progress
      if (deleteFlushSource != null)
      {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "flushAllForDelete", Boolean.FALSE);
        return false;
      }
         
      // Otherwise, we need to start a new flush
      final PtoPOutputHandler psOH = this; 
      deleteFlushSource = new FlushComplete() {
        public void flushComplete(DestinationHandler destinationHandler)
          {
            // Remember that the flush completed for when we redrive
            // the delete code.
            synchronized (psOH) {
              psOH.flushedForDeleteSource = true;
              psOH.deleteFlushSource      = null;
            }
            
            // Now redrive the actual deletion
            psOH.messageProcessor.getDestinationManager().startAsynchDeletion();

          }
        };       
    }
    
    // Start the flush and return false
    try 
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
        SibTr.event(tc, "Started PtoP source flush for destination: " + destinationHandler.getName());
      startFlush(deleteFlushSource);
    } 
    catch (FlushAlreadyInProgressException e) 
    {
      // This shouldn't actually be possible so log it
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.PtoPOutputHandler.flushAllForDelete",
        "1:2756:1.241",
        this);
        
      SibTr.exception(tc, e);
  
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "flushAllForDelete", "FlushAlreadyInProgressException");

      throw new SIResourceException(e);
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "flushAllForDelete", Boolean.FALSE);
    return false;
  }

  public long handleControlMessageWithReturnValue(SIBUuid8 sourceMEUuid,
                ControlMessage cMsg) throws SIIncorrectCallException,
                SIResourceException, SIConnectionLostException, SIRollbackException {
	return 0;
  }  
}
