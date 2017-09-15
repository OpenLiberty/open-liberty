/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.MessageCreateFailedException;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.mfp.control.ControlAckExpected;
import com.ibm.ws.sib.mfp.control.ControlAreYouFlushed;
import com.ibm.ws.sib.mfp.control.ControlFlushed;
import com.ibm.ws.sib.mfp.control.ControlMessage;
import com.ibm.ws.sib.mfp.impl.ControlMessageFactory;
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.control.ControlNotFlushed;
import com.ibm.ws.sib.mfp.control.ControlRequestFlush;
import com.ibm.ws.sib.mfp.control.ControlSilence;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPIncorrectCallException;
import com.ibm.ws.sib.processor.exceptions.SIMPNotPossibleInCurrentConfigurationException;
import com.ibm.ws.sib.processor.gd.GDConfig;
import com.ibm.ws.sib.processor.gd.StreamSet;
import com.ibm.ws.sib.processor.gd.TargetStreamManager;
import com.ibm.ws.sib.processor.impl.interfaces.BatchListener;
import com.ibm.ws.sib.processor.impl.interfaces.ControlHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.MessageDeliverer;
import com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener;
import com.ibm.ws.sib.processor.impl.interfaces.MessageProducer;
import com.ibm.ws.sib.processor.impl.interfaces.OutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.ProducerInputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.interfaces.UpstreamControl;
import com.ibm.ws.sib.processor.impl.store.BatchHandler;
import com.ibm.ws.sib.processor.impl.store.MessageEvents;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.impl.store.itemstreams.TargetProtocolItemStream;
import com.ibm.ws.sib.processor.io.MPIO;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * @author tevans
 */
public abstract class AbstractInputHandler implements ProducerInputHandler,
                                                      MessageDeliverer,
                                                      UpstreamControl,
                                                      ControlHandler,
                                                      MessageEventListener
{
  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
  private static final TraceNLS nls_cwsik =
    TraceNLS.getTraceNLS(SIMPConstants.CWSIK_RESOURCE_BUNDLE);

  private static final TraceComponent tc =
    SibTr.register(
      AbstractInputHandler.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  

  protected SIMPTransactionManager _txManager;
  protected MessageProcessor _messageProcessor;
  protected DestinationHandler _destination;

  /** The LockManager is used to stop remote Message Receivers
   * from processing messages when a delete destination is occuring.
   */
  protected BatchHandler _targetBatchHandler;

  protected MPIO _mpio;

  protected TargetStreamManager _targetStreamManager;
  protected ControlMessageFactory _cmf;

  private List<MessageProducer> _producers;
  
  private boolean _destinationDeleted = false;

  AbstractInputHandler(DestinationHandler destination,
                       TargetProtocolItemStream targetProtocolItemStream)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "AbstractInputHandler",
        new Object[]{destination, targetProtocolItemStream});

    _destination = destination;
    _messageProcessor = destination.getMessageProcessor();
    _txManager = _messageProcessor.getTXManager();
    _mpio = _messageProcessor.getMPIO();
    _targetBatchHandler = _messageProcessor.getTargetBatchHandler();
    _producers = new LinkedList<MessageProducer>();

    _targetStreamManager = new TargetStreamManager(_messageProcessor,
                                                  _destination,
                                                  this,
                                                  this,
                                                  targetProtocolItemStream,
                                                  _txManager);

    _cmf = MessageProcessor.getControlMessageFactory();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "AbstractInputHandler", this);
  }


  /**
   * Register for the pre prepare callback.
   */
  public void registerForEvents(SIMPMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "registerForEvents", msg);
    msg.registerMessageEventListener(MessageEvents.PRE_PREPARE_TRANSACTION, this);  //183715.1
    // If COD reports required, register for the prePrepare callback
    if(msg.getReportCOD() != null && _destination instanceof BaseDestinationHandler)
      msg.registerMessageEventListener(MessageEvents.COD_CALLBACK, (BaseDestinationHandler) _destination);
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "registerForEvents");
  }

  /**
   * @param producerSession  The producer session to attach
   */
  public void attachProducer(MessageProducer producerSession)
    throws SINotPossibleInCurrentConfigurationException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "attachProducer", producerSession);

    synchronized (_producers)
    {
      if (_destinationDeleted)
      {
        String destName = _destination.getName();
        if(_destination.isLink())
          destName = ((LinkHandler)_destination).getBusName();
        
        SIMPNotPossibleInCurrentConfigurationException e = new SIMPNotPossibleInCurrentConfigurationException(
            nls_cwsik.getFormattedMessage(
              "DELIVERY_ERROR_SIRC_32",  // DESTINATION_DELETED_ERROR_CWSIP0421
              new Object[] { destName,
                             _messageProcessor.getMessagingEngineName()},
              null));

        e.setExceptionReason(SIRCConstants.SIRC0032_DESTINATION_DELETED_ERROR);
        e.setExceptionInserts(new String[] { _destination.getName(),
                                      _messageProcessor.getMessagingEngineName()});

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "attachProducer", e);
        throw e;
      }

      _producers.add(producerSession);

    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "attachProducer");
  }

  /**
   * This is a put message that has oringinated from another ME
   *
   * @param msgItem  The message to be put.
   */
  protected void remoteToLocalPut(MessageItem msgItem) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "remoteToLocalPut",
        new Object[] { msgItem });

    _targetStreamManager.handleMessage(msgItem);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "remoteToLocalPut");

  }


  /**
   * Used to restore the GD target stream
   *
   */
  public void reconstituteTargetStreams(StreamSet streamSet)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "reconstituteTargetStreams", streamSet);

    _targetStreamManager.reconstituteStreamSet(streamSet);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "reconstituteTargetStreams");
  }

  /**
   * @param impl
   */
  public void detachProducer(MessageProducer producerSession)

  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "detachProducer", producerSession);

    synchronized (_producers)
    {
      _producers.remove(producerSession);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "detachProducer");
  }

  public String toString()
  {
    return "Dest InputHandler: "
      + _destination.getName();
  }

  /**
   * Get the number of producers to this InputHandler.
   * <p>
   * Feature 166832.21
   */
  public int getProducerCount()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getProducerCount");

    int producerCount;

    synchronized (_producers)
    {
      producerCount = _producers.size();
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getProducerCount", new Integer(producerCount));

    return producerCount;
  }

  /**
   * This method checks to see if rollback is required and
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
          "com.ibm.ws.sib.processor.impl.AbstractInputHandler.handleRollback",
          "1:345:1.170",
          this);

        SibTr.exception(tc, e);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleRollback");
  }
  
  /**
   * Puts a message to the InputHandlers destination
   *
   * @param msg  The message to put
   * @param transaction  The transaction object
   * @param producerSession  The producer that create the message
   * @param address  This is the DestinationAddress if one has been specified, may be null
   * @param sourceCellule The source for the message
   * @throws SIMPNotPossibleInCurrentConfigurationException 
   * @throws SIResourceException
   * @throws SIIncorrectCallException
   * @throws SIRollbackException
   * @throws SIConnectionLostException
   * @throws SIResourceException
   * @throws SIIncorrectCallException
   * @throws SIRollbackException
   * @throws
   */
  public OutputHandler handleMessage(MessageItem msg)
  throws SIMPNotPossibleInCurrentConfigurationException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "handleMessage", msg);

    //Set the current arrival time for the msg
    msg.setCurrentMEArrivalTimestamp(System.currentTimeMillis());

    // 169892
    // If message's Reliability has not been defined,
    // default it to that of the Destination
    Reliability msgReliability = msg.getReliability();
    if (msgReliability == Reliability.NONE)
    {
      msgReliability = _destination.getDefaultReliability();
      msg.setReliability(msgReliability);
    }
    else
    {
      // 169892
      // If message's Reliability is more than destinations, for a temporary
      // destination this is allowed (since max storage strategy will be set),
      // otherwise throw an exception
      if (msgReliability.compareTo(_destination.getMaxReliability()) > 0)
      {
        if ((_destination.isTemporary() ||
             _destination.getName().equals(_messageProcessor.getTDReceiverAddr().getDestinationName()))
          && (msg.getReliability().compareTo(Reliability.RELIABLE_PERSISTENT) >= 0 ))
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(
              tc,
              "handleMessage",
              "Sending ASSURED message to temporary destination");
        }
        else
        {
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "handleMessage", "Reliablity greater than dest");

          SIMPNotPossibleInCurrentConfigurationException e = new SIMPNotPossibleInCurrentConfigurationException(
            nls_cwsik.getFormattedMessage(
              "DELIVERY_ERROR_SIRC_33",  // MESSAGE_RELIABILITY_ERROR_CWSIP0231
              new Object[] {
                msg.getReliability().toString(),
                _destination.getMaxReliability().toString(),
                _destination.getName(),
                _messageProcessor.getMessagingEngineName()},
              null));
          e.setExceptionReason(SIRCConstants.SIRC0033_MESSAGE_RELIABILITY_ERROR);
          e.setExceptionInserts(new String[] {
                              msg.getReliability().toString(),
                              _destination.getMaxReliability().toString(),
                              _destination.getName(),
                              _messageProcessor.getMessagingEngineName()});
          throw e;
        }
      }
    }

    // For a temporary destination, maximum storage strategy is STORE_MAYBE (RELIABLE_NON_PERSISTENT)
    if(_destination.isTemporary() ||
       _destination.getName().equals(_messageProcessor.getTDReceiverAddr().getDestinationName()))
    {
      msg.setMaxStorageStrategy(AbstractItem.STORE_MAYBE);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleMessage");
    
    return null;
  }

  /**
   * Method registerMessage.
   * @param msg
   * @param tran
   * <p>Register the message with the pre-prepare callback on the transaction.
   * When called back, the choice of OutputHandler for the message is made
   * and the message is put to the itemstream of the chosen OutputHandler.
   * </p>
   */
  public void registerMessage(MessageItem msg, TransactionCommon tran) throws SIIncorrectCallException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "registerMessage", new Object[] { msg, tran });

    if (tran != null && !tran.isAlive())
    {
      SIMPIncorrectCallException e = new SIMPIncorrectCallException(
        nls_cwsik.getFormattedMessage(
           "DELIVERY_ERROR_SIRC_16",  // TRANSACTION_SEND_USAGE_ERROR_CWSIP0093
           new Object[] { _destination },
           null) );

      e.setExceptionReason(SIRCConstants.SIRC0016_TRANSACTION_SEND_USAGE_ERROR);
      e.setExceptionInserts(new String[] { _destination.getName() });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
        SibTr.exception(tc, e);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "registerMessage", e);

      throw e;
    }

    registerForEvents(msg);

    /* Register the message with the pre-prepare callback on the transaction.
     * When called back, the choice of OutputHandler for the message is made
     * and the message is put to the itemstream of the chosen OutputHandler.
     */
    tran.registerCallback(msg);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "registerMessage");
  }

  /**
   * Method closeProducersDestinationDeleted.
   * <p>Close and detach all producer sessions.</p>
   */
  public void closeProducersDestinationDeleted()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "closeProducersDestinationDeleted");

    synchronized (_producers)
    {
      _destinationDeleted = true;

      Iterator<MessageProducer> i = _producers.iterator();

      while(i.hasNext())
      {
        ProducerSessionImpl producerSessionImpl = (ProducerSessionImpl) i.next();

          // Close the producer session, indicating that it has been closed due to delete
          producerSessionImpl._closeProducerDestinationDeleted();

          // Remove the producer session from the Producers ArrayList
          i.remove();        
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "closeProducersDestinationDeleted");
  }

  /*
   *  (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ProducerInputHandler#detachAllProducersForNewInputHandler(com.ibm.ws.sib.processor.impl.interfaces.ProducerInputHandler)
   */
  public void detachAllProducersForNewInputHandler(ProducerInputHandler newHandler)throws SIException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "detachAllProducersForNewInputHandler", newHandler);

    //detach all producers and update
    synchronized (_producers)
    {
      Iterator<MessageProducer> it = _producers.iterator();
      while(it.hasNext())
      {
        ProducerSessionImpl prod = (ProducerSessionImpl)it.next();
        it.remove();
        //attach this to the new input handler
        newHandler.attachProducer(prod);
        prod.updateInputHandler(newHandler);
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "detachAllProducersForNewInputHandler");
  }

  /**
   * Creates an AREYOUFLUSHED message for sending
   *
   * @param ID The request ID to stamp on the message.
   * @return the new AREYOUFLUSHED message
   *
   * @throws SIResourceException if the message can't be created.
   */
  protected ControlAreYouFlushed createControlAreYouFlushed(SIBUuid8 target, long ID, SIBUuid12 stream)

  throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createControlAreYouFlushed");

    ControlAreYouFlushed flushedqMsg;

    // Create new message and send it
    try
    {
      flushedqMsg = _cmf.createNewControlAreYouFlushed();
    }
    catch (MessageCreateFailedException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AbstractInputHandler.createControlAreYouFlushed",
        "1:588:1.170",
        this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.exception(tc, e);
        SibTr.exit(tc, "createControlAreYouFlushed", e);
      }

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.AbstractInputHandler",
          "1:600:1.170",
          e });

      throw new SIResourceException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.AbstractInputHandler",
            "1:608:1.170",
            e },
          null),
        e);
    }

    // As we are using the Guaranteed Header - set all the attributes as
    // well as the ones we want.
    SIMPUtils.setGuaranteedDeliveryProperties(flushedqMsg,
        _messageProcessor.getMessagingEngineUuid(), 
        target,
        stream,
        null,
        _destination.getUuid(),
        ProtocolType.UNICASTOUTPUT,
        GDConfig.PROTOCOL_VERSION);
    
    if(_destination.isPubSub())
    {
      flushedqMsg.setGuaranteedProtocolType(ProtocolType.PUBSUBOUTPUT);
    }

    flushedqMsg.setRequestID(ID);
    flushedqMsg.setPriority(SIMPConstants.CTRL_MSG_PRIORITY);
    flushedqMsg.setReliability(SIMPConstants.CONTROL_MESSAGE_RELIABILITY);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createControlAreYouFlushed", flushedqMsg);

    return flushedqMsg;
  }

  /**
   * Creates a REQUESTFLUSH message for sending
   *
   * @param ID The request ID to stamp on the message.
   * @return the new REQUESTFLUSH message
   *
   * @throws SIResourceException if the message can't be created.
   */
  protected ControlRequestFlush createControlRequestFlush(SIBUuid8 target, long ID, SIBUuid12 stream)
   throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createControlRequestFlush");

    ControlRequestFlush rflushMsg;

    // Create new message and send it
    try
    {
      rflushMsg = _cmf.createNewControlRequestFlush();
    }
    catch (MessageCreateFailedException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.AbstractInputHandler.createControlRequestFlush",
        "1:667:1.170",
        this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.exception(tc, e);
        SibTr.exit(tc, "createControlRequestFlush", e);
      }

      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.AbstractInputHandler",
          "1:679:1.170",
          e });

      throw new SIResourceException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.AbstractInputHandler",
            "1:687:1.170",
            e },
          null),
        e);
    }

    // As we are using the Guaranteed Header - set all the attributes as
    // well as the ones we want.
    SIMPUtils.setGuaranteedDeliveryProperties(rflushMsg,
        _messageProcessor.getMessagingEngineUuid(), 
        target,
        stream,
        null,
        _destination.getUuid(),
        null,
        GDConfig.PROTOCOL_VERSION);
    
    if(_destination.isPubSub())
    {
      rflushMsg.setGuaranteedProtocolType(ProtocolType.PUBSUBOUTPUT);
    }
    else
    {
      rflushMsg.setGuaranteedProtocolType(ProtocolType.UNICASTOUTPUT);
    }

    rflushMsg.setRequestID(ID);
    rflushMsg.setPriority(SIMPConstants.CTRL_MSG_PRIORITY);
    rflushMsg.setReliability(SIMPConstants.CONTROL_MESSAGE_RELIABILITY);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createControlRequestFlush", rflushMsg);

    return rflushMsg;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControlHandler#handleControlMessage(com.ibm.ws.sib.trm.topology.Cellule, com.ibm.ws.sib.mfp.control.ControlMessage)
   *
   * Handle all downstream control messages i.e. target control messages
   */
  public void handleControlMessage(SIBUuid8 sourceMEUuid, ControlMessage cMsg)
    throws SIIncorrectCallException, SIErrorException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "handleControlMessage", new Object[] { sourceMEUuid, cMsg });

    // Next work out type of ControlMessage and process it
    ControlMessageType type = cMsg.getControlMessageType();

    // First check to see whether this is an "are you flushed" reply.
    // Such messages will not be mappable to a stream ID since we
    // don't yet have a stream data structure (that's why we sent the
    // query in the first place).  Or...these could be stale messages
    // for streams we don't care about.  Either way, handle them
    // elsewhere.
    if(type == ControlMessageType.FLUSHED)
    {
      _targetStreamManager.handleFlushedMessage((ControlFlushed)cMsg);
    }
    else if(type == ControlMessageType.NOTFLUSHED)
    {
      _targetStreamManager.handleNotFlushedMessage((ControlNotFlushed)cMsg);
    }
    else if (type == ControlMessageType.SILENCE)
    {
      _targetStreamManager.handleSilenceMessage((ControlSilence) cMsg);
    }
    else if (type == ControlMessageType.ACKEXPECTED)
    {
      _targetStreamManager.handleAckExpectedMessage((ControlAckExpected) cMsg);
    }
    else
    {
      // Not a recognised type
      // throw exception
    }
  }

  /**
   * <p>Returns the targetStreamManager.</p>
   * @return TargetStreamManager
   */
  public TargetStreamManager getTargetStreamManager()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getTargetStreamManager");
      SibTr.exit(tc, "getTargetStreamManager", _targetStreamManager);
    }

    return _targetStreamManager;
  }

  public void forceTargetBatchCompletion(BatchListener listener) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "forceTargetBatchCompletion", new Object[] { listener });

    _targetBatchHandler.completeBatch(true, listener);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "forceTargetBatchCompletion");
  }

  /*
   *  (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ProducerInputHandler#getInboundStreamsEmpty()
   */
  public boolean getInboundStreamsEmpty()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getInboundStreamsEmpty");

    boolean returnValue = false;
    if(_targetStreamManager!=null)
    {
      returnValue = _targetStreamManager.isEmpty();
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getInboundStreamsEmpty", new Boolean(returnValue));
    return returnValue;
  }
  
}
