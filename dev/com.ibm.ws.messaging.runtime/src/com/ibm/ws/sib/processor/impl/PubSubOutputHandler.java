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
import java.util.Map;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.VirtualLinkDefinition;
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
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.gd.GDConfig;
import com.ibm.ws.sib.processor.gd.InternalOutputStream;
import com.ibm.ws.sib.processor.gd.InternalOutputStreamManager;
import com.ibm.ws.sib.processor.gd.TickRange;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidOperationException;
import com.ibm.ws.sib.processor.impl.interfaces.ControlHandler;
import com.ibm.ws.sib.processor.impl.interfaces.ControllableResource;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl;
import com.ibm.ws.sib.processor.impl.interfaces.HealthStateListener;
import com.ibm.ws.sib.processor.impl.interfaces.InputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.InputHandlerStore;
import com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener;
import com.ibm.ws.sib.processor.impl.interfaces.OutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.interfaces.UpstreamControl;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.io.MPIO;
import com.ibm.ws.sib.processor.proxyhandler.Neighbour;
import com.ibm.ws.sib.processor.runtime.HealthState;
import com.ibm.ws.sib.processor.runtime.impl.AttachedRemoteSubscriberControl;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.runtime.impl.LinkRemoteTopicSpaceControl;
import com.ibm.ws.sib.processor.runtime.impl.RemoteTopicSpaceControl;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.UserTrace;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.SelectorDomain;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 */
public final class PubSubOutputHandler
  implements OutputHandler,
             ControlHandler,
             DownstreamControl,
             UpstreamControl,
             MessageEventListener,
             ControllableResource
{
  private static final TraceComponent tc =
    SibTr.register(
      PubSubOutputHandler.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  // NLS for component
  private static final TraceNLS nls =
    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

  
  /** The subscription state object for this PubSubOutputHandler.
   */
  private ConsumerDispatcherState _subscriptionState;

  // A multikey map which maps stream IDs to appropriate stream 
  // data structures.  We don't bother creating an entry for a
  // stream until we actually see data for it (the creation
  // is actually performed in PubSubInputHandler).
  private InternalOutputStreamManager _internalOutputStreamManager;

  private ControlHandler _parentInputHandler;

  // The cellule which is logically on the other end (i.e. one hop away)
  // of this OutputHandler.
  private SIBUuid8 _targetMEUuid;

  private MPIO _mpio;
  
  /** The Neighbour instance for this OutputHandler */
  private Neighbour _neighbour;
  
  private ControlMessageFactory _cmf;

  /**
   * The destination uuid for this topic space.
   */
  private String    _destName;
  private String    _busName;
  
  private BaseDestinationHandler _destinationHandler;
  
  /**
   * The foreign bus topic space name that this topicspace maps to
   */
  private String _foreignTSName = null;
  
  /**
   * The link name used for transport to a foreign bus
   */
  private boolean   _isLink = false;
  private String    _linkName = null;
  private boolean   _linkSetOutboundUserId = false;
  private String    _linkOutboundUserid = null;
  private VirtualLinkDefinition link = null;
  
  /**
   * The routingAddress used for inter-bus publications
   */
  private JsDestinationAddress _routingDestination = null;
  
  private boolean _isGuess = false;
  
  private MessageProcessor _messageProcessor;
  
  /** The remote topicspace control adapter */
  private ControlAdapter _controlAdapter;

  private boolean isRegistered;

  /**
   * Constructor for the remote PubSub handler
   *
   * @param messageProcessor  The MP Instance
   * @param parentInputHandler  The InputHandler.
   * @param destUuid    The topicsSpace destination uuid.
   * @param destName    The topicSpace destination name
   * @param busName     The topicspace bus name
   * @param neighbour     The Neighbour object that created this Output Handler.
   */
  public PubSubOutputHandler(
    MessageProcessor messageProcessor,
    Neighbour neighbour,
    BaseDestinationHandler baseDest) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "PubSubOutputHandler", 
        new Object[]{messageProcessor, 
                     neighbour,
                     baseDest});
                     
    _messageProcessor = messageProcessor;
    _destName = baseDest.getName();
    _busName = baseDest.getBus();
    _neighbour = neighbour;
   
    _parentInputHandler = (ControlHandler)baseDest.getInputHandler();

    _mpio = messageProcessor.getMPIO();    

    _targetMEUuid= neighbour.getUUID();
    _cmf = MessageProcessor.getControlMessageFactory();
    _destinationHandler = baseDest;
    
    // Get the linkName for this bus
    
    if( !neighbour.getBusId().equals(messageProcessor.getMessagingEngineBus()))
    {
      link = messageProcessor.getDestinationManager().getLinkDefinition(neighbour.getBusId());
      
      // Defect 238709: An MQLinkHandler is deemed not to be a link  
      if (link != null && link.getType().equals("SIBVirtualGatewayLink"))
      {
        _linkName = link.getName();
        _linkOutboundUserid = link.getOutboundUserid();
        _isLink = true;
    
        if(_linkOutboundUserid != null) 
        {
          _linkSetOutboundUserId = true;       
        }
      }
    }
      
    // Create the internalOutputStream array for this output handler.
    _internalOutputStreamManager = new InternalOutputStreamManager(this,this,_messageProcessor,_targetMEUuid, _isLink);
    
    createControlAdapter();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "PubSubOutputHandler", this);
  }

  /**
   * Adds a topic to the subscription state for this OutputHandler.
   *
   * @param topic  The name of the topic
   */
  public void addTopic(String topic)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "addTopic", topic);

    SelectionCriteria criteria = _messageProcessor.
                                   getSelectionCriteriaFactory().
                                   createSelectionCriteria(topic,
                                                           null,
                                                           SelectorDomain.SIMESSAGE);

    if (_subscriptionState == null)
    {
       _subscriptionState =
        new ConsumerDispatcherState(_destinationHandler.getUuid(), criteria, _destName, _busName);
    }
    else
      _subscriptionState.addSelectionCriteria(criteria);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "addTopic");
  }

  /**
   * Removes a topic from the set of topics with this OutputHandler
   */
  public void removeTopic(String topic)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "removeTopic", topic);

    if (_subscriptionState == null)
    {
      if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        SibTr.debug(tc, "removeTopic", "Topic not found");
    }
    else
      _subscriptionState.removeTopic(topic);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "removeTopic");
  }

  /**
   * Get the set of topics associated with this OutputHandler
   */
  public String[] getTopics()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTopics");
      
    String[] topics = null;
    if (_subscriptionState != null)      
      topics = _subscriptionState.getTopics();
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())  
      SibTr.exit(tc, "getTopics", topics);
    return topics;
  }

  /**
   * Get the topic space name associated with this OutputHandler
   */
  public SIBUuid12 getTopicSpaceUuid()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getTopicSpaceUuid");
    SIBUuid12 retval = _destinationHandler.getUuid();
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getTopicSpaceUuid", retval);
    return retval;
  }

  /**
   * @see com.ibm.ws.sib.processor.impl.OutputHandler#put(SIMPMessage, Transaction, InputHandler, boolean)
   */
  public boolean put(
    SIMPMessage msg,
    TransactionCommon transaction,
    InputHandlerStore inputHandlerStore,
    boolean storedByIH) throws SIResourceException
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

    // add to stream with Uncommitted tick
    _internalOutputStreamManager.addMessage(msg,false);
    
    if (link!=null) // Will only register if not already registered
      registerControlAdapterAsMBean();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "put", Boolean.valueOf(storedByIH));

    return storedByIH;
  }
  
  public void putInsert(
    SIMPMessage msg,
    boolean commitInsert) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "putInsert",
        new Object[] {
          msg,
          Boolean.valueOf(commitInsert)});

    _internalOutputStreamManager.addMessage(msg, commitInsert);
    
    if (link!=null) // Will only register if not already registered
      registerControlAdapterAsMBean();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "putInsert");

    return;
  }
  
  public void putSilence(SIMPMessage msg) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "putSilence",
        new Object[] {
          msg});

    _internalOutputStreamManager.addSilence(msg);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "putSilence");
  }
  
  /**
   * Returns boolean if this message can be sent to this Neighbour
   * We need to check this in order to avoid the message being continually bounced
   * between two MEs.
   * The message can be sent to a neighbour if:
   * 1) The neighbour is on a different bus that did not orignate the
   *    message
   * 2) This neighbour is on the same bus but the received message
   *    was published on a different bus so, as far as this neighbour
   *    is concerned it should be effectively the same as us having 
   *    published it.
   * 3) The neighbour is on the same bus and this ME is the original publisher
   *    of the message
   */
  public boolean okToForward(MessageItem item)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "okToForward", item);
    boolean returnValue = false;    
    if(isLink())
    {
      //we can only send if this bus was not the originating bus
      returnValue = neighbourOnDifferentBus(item.getOriginatingBus());
      if (returnValue)
      {
        // Check foreign bus is sendAllowed
        BusHandler bus = null;
        try
        {
          bus = _destinationHandler.getDestinationManager().findBus(_neighbour.getBusId());
        }
        catch (Exception e)
        {
          // FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.PubSubOutputHandler.okToForward",
            "1:446:1.164.1.5",
            this);
            
          SibTr.error(tc, "PUBSUB_LINK_FORWARD_CWSIP0855",
            new Object[] {
              _destinationHandler.getName(),
              _neighbour.getBusId(),
              e});            
        }
        
        if (bus!=null)
          returnValue = bus.isSendAllowed();               
      }
    }
    else
    {
      if(item.isFromRemoteBus())
      {
        //the msg has come in on a remote bus and is being sent to
        //a PSOH on the local bus
        returnValue = true;
      }
      else
      {
        //we can only send if this ME was the originator
        //which means the msg was not from a remote ME
        returnValue = !item.isFromRemoteME();
      }
      
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.exit(tc, "okToForward", new Boolean(returnValue));    
    return returnValue;
  }
  
  /**
   * Returns true if this neighbour is on a different bus
   * to busId
   */
  public boolean neighbourOnDifferentBus(String busId)
  {
    return _neighbour.okToForward(busId);
  }

  public void registerForEvents(SIMPMessage msg) 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "registerForEvents", msg);
    
    InvalidOperationException e = 
      new InvalidOperationException(nls.getFormattedMessage(
        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
          "1:499:1.164.1.5" },
        null));
    
    // FFDC
    FFDCFilter.processException(
      e,
      "com.ibm.ws.sib.processor.impl.PubSubOutputHandler.registerForEvents",
      "1:506:1.164.1.5",
      this);
      
    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
      new Object[] {
        "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
        "1:512:1.164.1.5" });
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "registerForEvents", e);
    throw e;  
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControlHandler#handleControlMessage(com.ibm.ws.sib.mfp.control.ControlMessage)
   */
  public void handleControlMessage(SIBUuid8 sourceMEUuid, ControlMessage cMsg) 
  
  throws SIConnectionLostException, SIRollbackException, SIIncorrectCallException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "handleControlMessage", new Object[]{sourceMEUuid,cMsg});
    
    // Work out type of ControlMessage and process it
    ControlMessageType type = cMsg.getControlMessageType();

    // Forward flush queries to our parent.  The cellule
    // we pass here doesn't actually matter since our PubSubInputHandler
    // will ignore it and just use the one in the message.
    if (type == ControlMessageType.REQUESTFLUSH) 
    {
      _parentInputHandler.handleControlMessage(_targetMEUuid, cMsg);      
    }
  
    // Forward flush queries to our parent.  The cellule
    // we pass here doesn't actually matter since our PubSubInputHandler
    // will ignore it and just use the one in the message.
    else if ( type == ControlMessageType.AREYOUFLUSHED)
    {
      _internalOutputStreamManager.processFlushQuery((ControlAreYouFlushed)cMsg);      
    }
    
    // Now we can process the rest of the control messages...
    
    else if (type == ControlMessageType.ACK)
    {
      _internalOutputStreamManager.processAck((ControlAck)cMsg);
    }
    else if (type == ControlMessageType.NACK)
    {
      _internalOutputStreamManager.processNack((ControlNack)cMsg);
    }
    else
    {
      // Not a recognised type
      // throw exception
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "handleControlMessage");

  }

  // Our PubSubInputHandler calls this ONLY (currently) when ackExpected
  // needs to be forwarded downstream.
  void processAckExpected(
    long ackExpStamp,
    int priority,
    Reliability reliability,
    SIBUuid12 stream)
   throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processAckExpected", new Long(ackExpStamp));

    _internalOutputStreamManager.processAckExpected(ackExpStamp, priority, reliability, stream);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processAckExpected");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener#messageEventOccurred(int, com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage, com.ibm.ws.sib.msgstore.Transaction)
   */
  public void messageEventOccurred(
    int event,
    SIMPMessage msg,
    TransactionCommon tran)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "messageEventOccurred", 
        new Object[]{new Integer(event), msg, tran});
        
    InvalidOperationException e = 
      new InvalidOperationException(nls.getFormattedMessage(
        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
          "1:603:1.164.1.5" },
        null));
    
    // FFDC
    FFDCFilter.processException(
      e,
      "com.ibm.ws.sib.processor.impl.PubSubOutputHandler.messageEventOccurred",
      "1:610:1.164.1.5",
      this);
    
    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
    new Object[] {
      "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
      "1:616:1.164.1.5" } );
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "messageEventOccurred", e);
    throw e;  
  }

  /** toString
   */
  public String toString()
  {
    return "PubSubOutputHandler: "
           + _busName
           + ":"
           + _destName
           + ":"
           + _destinationHandler.getUuid().toString()
           + " on "
           + _neighbour.toString();
  }

  /**
   * @see com.ibm.ws.sib.processor.impl.OutputHandler#getUuid()
   */
  public SIBUuid8 getTargetMEUuid()
  {
    return _neighbour.getUUID();
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl#sendAckExpectedMessage(long, com.ibm.ws.sib.trm.topology.Cellule, int, com.ibm.ws.sib.common.Reliability)
   */
  public void sendAckExpectedMessage(
    long ackExpStamp,
    int priority,
    Reliability reliability,
    SIBUuid12 stream)
    throws SIResourceException
  {

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(
        tc,
        "sendAckExpectedMessage",
        new Object[] {
          new Long(ackExpStamp),
          _targetMEUuid,
          new Integer(priority),
          reliability });

    ControlAckExpected ackexpMsg;
    try
    {
      ackexpMsg = _cmf.createNewControlAckExpected();
    }
    catch (Exception e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.PubSubOutputHandler.sendAckExpectedMessage",
        "1:676:1.164.1.5",
        this);

      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
          "1:683:1.164.1.5",
          e });

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendAckExpectedMessage", e);

      throw new SIResourceException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
            "1:694:1.164.1.5",
            e },
          null),
        e);
    }

    // As we are using the Guaranteed Header - set all the attributes as 
    // well as the ones we want.
    SIMPUtils.setGuaranteedDeliveryProperties(ackexpMsg,
        _messageProcessor.getMessagingEngineUuid(), 
        null,
        stream,
        null,
        _destinationHandler.getUuid(),
        ProtocolType.PUBSUBINPUT,
        GDConfig.PROTOCOL_VERSION);
   
    ackexpMsg.setTick(ackExpStamp);
    ackexpMsg.setPriority(priority);
    ackexpMsg.setReliability(reliability);
    
    // SIB0105
    // Update the health state of this stream 
    InternalOutputStream sourceStream = (InternalOutputStream)_internalOutputStreamManager
                                              .getStreamSet(stream, false)
                                              .getStream(priority, reliability);
    if (sourceStream != null)
    {
      sourceStream.setLatestAckExpected(ackExpStamp);
      sourceStream.getControlAdapter()
        .getHealthState().updateHealth(HealthStateListener.ACK_EXPECTED_STATE, 
                                       HealthState.AMBER);
    }

    // If the destination in a Link add Link specific properties to message
    if( _isLink )
    {
      ackexpMsg = (ControlAckExpected)addLinkProps(ackexpMsg);
    }
    // Send ackExpected message to destination
    // Using MPIO

    //add a target cellule to the array for sending
    SIBUuid8[] fromTo = new SIBUuid8[1];
    fromTo[0] = _targetMEUuid;

    // Send the message to the MessageTransmitter
    _mpio.sendDownTree(fromTo,
                         priority,
                         ackexpMsg);


    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendAckExpectedMessage");

  }

  /**
    * sendSilenceMessage may be called from InternalOutputStream
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
       SibTr.entry(tc, "sendSilenceMessage",
         new Object[] {
           new Long(startStamp),
           new Long(endStamp),
           new Long(completedPrefix),
           new Integer(priority),
           reliability });

     ControlSilence sMsg;
     try
     {
       // Create new Silence message
       sMsg = _cmf.createNewControlSilence();
     }
     catch (Exception e)
     {
       // FFDC
       FFDCFilter.processException(
         e,
         "com.ibm.ws.sib.processor.impl.PubSubOutputHandler.sendSilenceMessage",
         "1:787:1.164.1.5",
         this);

       SibTr.exception(tc, e);
       SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
         new Object[] {
           "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
           "1:794:1.164.1.5",
           e });

       if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
         SibTr.exit(tc, "sendSilenceMessage", e);

       throw new SIResourceException(
         nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
            "1:805:1.164.1.5",
            e },
          null),
         e);
     }

     // As we are using the Guaranteed Header - set all the attributes as 
     // well as the ones we want.
     SIMPUtils.setGuaranteedDeliveryProperties(sMsg,
         _messageProcessor.getMessagingEngineUuid(), 
         null,
         stream,
         null,
         _destinationHandler.getUuid(),
         ProtocolType.PUBSUBINPUT,
         GDConfig.PROTOCOL_VERSION);
    
     sMsg.setStartTick(startStamp);
     sMsg.setEndTick(endStamp);
     sMsg.setPriority(priority);
     sMsg.setReliability(reliability);
     sMsg.setCompletedPrefix(completedPrefix);
     sMsg.setRequestedOnly(requestedOnly);
     
     // If the destination in a Link add Link specific properties to message
     if( _isLink )
     {
       sMsg = (ControlSilence)addLinkProps(sMsg);
     }
    
     // Send message to destination
     // Using MPIO

     // Send the message to the MessageTransmitter
     //add a target cellule to the array for sending
     SIBUuid8[] fromTo = new SIBUuid8[1];
     fromTo[0] = _targetMEUuid;
      
     // Send at priority+1 if this is a response to a Nack
     if( requestedOnly )
       _mpio.sendDownTree(fromTo, priority+1, sMsg);
     else
       _mpio.sendDownTree(fromTo, priority, sMsg);

     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "sendSilenceMessage");

   }
   
  public MessageItem getValueMessage(long msgStoreID)
   throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "getValueMessage", new Long(msgStoreID));
   
    // Retrieve the message from the non-persistent ItemStream
    MessageItem msgItem =
      _destinationHandler.
        getPubSubRealization().
          retrieveMessageFromItemStream(msgStoreID);
        
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "getValueMessage", msgItem);
    return msgItem;
  }

  // This method is only called by the InternalOutputStream in response to
  // a Nack message.
  // msgList is a list of tickRanges which hold MsgStore Ids of SIMPMessages 
  // to be sent downstream
  public List sendValueMessages(
    List msgList,
    long completedPrefix,
    boolean requestedOnly,
    int priority,
    Reliability reliability,
    SIBUuid12 stream) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendValueMessages",
        new Object[] { msgList,
                       new Long(completedPrefix),
                       new Boolean(requestedOnly)});

    // We will send the messages using MPIO
    // Work out the cellule pair needed by the
    // messageTransmitter
    SIBUuid8[] fromTo = new SIBUuid8[1];
    fromTo[0] = _targetMEUuid;

    JsMessage jsMsg = null;

    TickRange tickRange = null;
    MessageItem msgItem = null;
    long msgId = -1;
    List<TickRange> expiredMsgs = null;
    
    for (int i = 0; i < msgList.size(); i++)
    {
      tickRange = (TickRange)msgList.get(i);
     
      // Get the messageStore Id from the stream
      msgId = tickRange.itemStreamIndex;

      // Retrieve the message from the non-persistent ItemStream
      msgItem =
        _destinationHandler.
          getPubSubRealization().
            retrieveMessageFromItemStream(msgId);
                  
      // If the item wasn't found it has expired  
      if ( msgItem == null )
      {
        if ( expiredMsgs == null )
          expiredMsgs = new ArrayList<TickRange>();
        
        expiredMsgs.add(tickRange);   

        // In this case send Silence instead      
        ControlMessage cMsg = createSilenceMessage(tickRange.valuestamp,
                                                   completedPrefix,
                                                   priority,
                                                   reliability,
                                                   stream);

        ((ControlSilence)cMsg).setRequestedOnly(requestedOnly);
             
        // If the destination in a Link add Link specific properties to message
        if( _isLink )
        {
          cMsg = addLinkProps(cMsg);
          cMsg.setRoutingDestination( _routingDestination );  
        }
                  
        // Send the message to the MessageTransmitter
        // Send at priority+1 if this is a response to a Nack
        if( requestedOnly )
          _mpio.sendDownTree(fromTo, priority+1, cMsg);
        else
          _mpio.sendDownTree(fromTo, priority, cMsg);
  
      }
      else
      {
        try
        {
          // PM34074
          // Retrieve a copy of the message from MessageItem
          jsMsg = msgItem.getMessage().getReceived();
        }
        catch(MessageCopyFailedException e)
        {
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.impl.PubSubOutputHandler.sendValueMessages",
            "1:960:1.164.1.5",
            this);
            
          if (tc.isEntryEnabled())
            SibTr.exit(tc, "sendValueMessages", "SIErrorException: "+e);  
          throw new SIErrorException(e);
        }
      
        // Modify the streamId if necessary     
        if( jsMsg.getGuaranteedStreamUUID() !=  stream)
        {
          jsMsg.setGuaranteedStreamUUID(stream);
        }

        // Are there Completed ticks after this Value
        // If so we need to adjust the message to reflect this
        if (tickRange.endstamp > tickRange.valuestamp)
        {
          jsMsg.setGuaranteedValueEndTick(tickRange.endstamp);
        }
      
        // Update the completedPrefix to current value
        jsMsg.setGuaranteedValueCompletedPrefix(completedPrefix);

        // Set the requestedOnly flag
        jsMsg.setGuaranteedValueRequestedOnly(requestedOnly);
      
        // If the destination in a Link add Link specific properties to message
        if( _isLink )
        {
          jsMsg = addLinkProps(jsMsg);
          jsMsg.setRoutingDestination( _routingDestination );  
        }
      
        if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
        {
          DestinationHandler dest = 
            _messageProcessor.getDestinationManager().getDestinationInternal(_destinationHandler.getUuid(), false);
          
          if (dest != null)
            UserTrace.traceOutboundSend(jsMsg, 
                                        _neighbour.getUUID(), 
                                        dest.getName(),
                                        dest.isForeignBus() || dest.isLink(),
                                        dest.isMQLink(),
                                        dest.isTemporary());
          else
            UserTrace.traceOutboundSend(jsMsg, 
                                        _neighbour.getUUID(), 
                                        _destinationHandler.getUuid().toString().toString(),
                                        false,
                                        false,
                                        false);

        }
        
        // Send the message to the MessageTransmitter
        // Send at priority+1 if this is a response to a Nack
        if( requestedOnly )
          _mpio.sendDownTree(fromTo, priority+1, jsMsg);
        else
          _mpio.sendDownTree(fromTo, priority, jsMsg);
                        
      }
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendValueMessages", expiredMsgs);
      
    return expiredMsgs;  
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl#sendFlushedMessage(com.ibm.ws.sib.utils.SIBUuid12)
   */
  public void sendFlushedMessage(SIBUuid8 ignore, SIBUuid12 streamID) throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.entry(tc, "sendFlushedMessage", new Object[]{ignore, streamID});
       
    ControlFlushed flushMsg = createControlFlushed(_targetMEUuid, streamID);
    
    // If the destination in a Link add Link specific properties to message
    if( _isLink )
    {
      flushMsg = (ControlFlushed)addLinkProps(flushMsg);
    }
    
    _mpio.sendToMe(_targetMEUuid, SIMPConstants.MSG_HIGH_PRIORITY, flushMsg);   
   
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "sendFlushedMessage");
  }
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl#sendNotFlushedMessage(com.ibm.ws.sib.utils.SIBUuid12, long)
   */
  public void sendNotFlushedMessage(SIBUuid8 ignore, SIBUuid12 streamID, long requestID) 
  throws SIResourceException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "sendNotFlushedMessage", 
        new Object[]{ignore, streamID, new Long(requestID)});
        
    ControlNotFlushed notFlushed = createControlNotFlushed(_targetMEUuid, streamID, requestID);
    notFlushed = _internalOutputStreamManager.stampNotFlushed(notFlushed, streamID);
    
    // If the destination in a Link add Link specific properties to message
    if( _isLink )
    {
      notFlushed = (ControlNotFlushed)addLinkProps(notFlushed);
      notFlushed.setRoutingDestination(_routingDestination);
    }
    _mpio.sendToMe(_targetMEUuid, SIMPConstants.MSG_HIGH_PRIORITY, notFlushed);    
        
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "sendNotFlushedMessage");  
  }

  // Called when messaeg has to be sent from OutputHandler rather than
  // InputHandler because it is for a link and additional link specific 
  // properties need to be set on the mssage
  public void sendLinkMessage( MessageItem msgItem, boolean rollback) 
   throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendLinkMessage", msgItem);
        
    JsMessage jsMsg=null;
    try
    {
      //defect 245624
      jsMsg = msgItem.getMessage().getReceived();
    }
    catch(MessageCopyFailedException e)
    {
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.PubSubOutputHandler.sendLinkMessage",
        "1:1097:1.164.1.5",
        this);
        
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        SibTr.exit(tc, "sendLinkMessage", "SIErrorException");  
      throw new SIErrorException(e);
    }
     
    SIBUuid8[] fromTo = new SIBUuid8[1];
    fromTo[0] = _targetMEUuid;
  
    if( rollback )
    {
      ControlMessage cMsg = createSilenceMessage(jsMsg.getGuaranteedValueValueTick(),
                                                 jsMsg.getGuaranteedValueCompletedPrefix(),
                                                 msgItem.getPriority(),
                                                 msgItem.getReliability(),
                                                 jsMsg.getGuaranteedStreamUUID());   
      cMsg = addLinkProps(cMsg);
      cMsg.setRoutingDestination( _routingDestination );  
      
      //call MPIO to finally send the message to the remote MEs
      _mpio.sendDownTree(fromTo,                //the list of source target pairs
                        msgItem.getPriority(), //priority
                        cMsg);                 //the Silence Message  
 
    }
    else
    { 
      // Add Link specific properties to message
      jsMsg = addLinkProps(jsMsg);
      jsMsg.setRoutingDestination( _routingDestination );  
      jsMsg.setGuaranteedSourceMessagingEngineUUID( _messageProcessor.getMessagingEngineUuid() );
                     
      //call MPIO to finally send the message to the remote MEs
      _mpio.sendDownTree(fromTo,                //the list of source target pairs
                        msgItem.getPriority(), //priority
                        jsMsg);                //the JsMessage  
    }
            
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendLinkMessage");
     
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.OutputHandler#commitInsert(com.ibm.ws.sib.processor.impl.store.MessageItem)
   */
  public boolean commitInsert(MessageItem msgItem) throws SIResourceException 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "commitInsert", msgItem);

    _internalOutputStreamManager.commitInsert(msgItem);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "commitInsert");
    
    return true;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.OutputHandler#rollbackInsert(com.ibm.ws.sib.processor.impl.store.MessageItem)
   */
  public boolean rollbackInsert(MessageItem msgItem) 
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "rollbackInsert", msgItem);

    _internalOutputStreamManager.rollbackInsert(msgItem);
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "rollbackInsert");
      
    return true;  
  }

  /**
  * Creates an NACK message for sending
  *
  * @return the new NACK message
  *
  * @throws SIResourceException if the message can't be created.
  */
  private ControlNack createControlNackMessage(
    int priority,
    Reliability reliability,
    SIBUuid12 stream)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createControlNackMessage");

    ControlNack nackMsg;

    // Create new AckMessage and send it
    try
    {
      nackMsg = _cmf.createNewControlNack();
    }
    catch (MessageCreateFailedException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.PubSubOutputHandler.createControlNackMessage",
        "1:1204:1.164.1.5",
        this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.exception(tc, e);
        SibTr.exit(tc, "createControlNackMessage", e);
      }
      
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
          "1:1216:1.164.1.5",
          e });

      throw new SIResourceException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
            "1:1224:1.164.1.5",
            e },
          null),
        e);
    }

    // As we are using the Guaranteed Header - set all the attributes as 
    // well as the ones we want.
    SIMPUtils.setGuaranteedDeliveryProperties(nackMsg,
        _messageProcessor.getMessagingEngineUuid(), 
        null,
        stream,
        null,
        _destinationHandler.getUuid(),
        ProtocolType.PUBSUBINPUT,
        GDConfig.PROTOCOL_VERSION);

    nackMsg.setPriority(priority);
    nackMsg.setReliability(reliability);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createControlNackMessage");

    return nackMsg;
  }

  /**
    * Creates an ACK message for sending
    *
    * @return the new ACK message
    *
    * @throws SIResourceException if the message can't be created.
    */
  private ControlAck createControlAckMessage(
    int priority,
    Reliability reliability,
    SIBUuid12 stream)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createControlAckMessage");

    ControlAck ackMsg;

    // Create new AckMessage and send it
    try
    {
      ackMsg = _cmf.createNewControlAck();
    }
    catch (MessageCreateFailedException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.PubSubOutputHandler.createControlAckMessage",
        "1:1279:1.164.1.5",
        this);

      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.exception(tc, e);
        SibTr.exit(tc, "createControlAckMessage", e);
      }
      
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
          "1:1291:1.164.1.5",
          e });

      throw new SIResourceException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
            "1:1299:1.164.1.5",
            e },
          null),
        e);
    }

    // As we are using the Guaranteed Header - set all the attributes as 
    // well as the ones we want.
    SIMPUtils.setGuaranteedDeliveryProperties(ackMsg,
        _messageProcessor.getMessagingEngineUuid(), 
        null,
        stream,
        null,
        _destinationHandler.getUuid(),
        ProtocolType.PUBSUBINPUT,
        GDConfig.PROTOCOL_VERSION);
  
    ackMsg.setPriority(priority);
    ackMsg.setReliability(reliability);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createControlAckMessage");

    return ackMsg;
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
      sMsg = _cmf.createNewControlSilence();
    }
    catch (Exception e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.PubSubOutputHandler.createSilenceMessage",
        "1:1353:1.164.1.5",
         this);
      SibTr.exception(tc, e);
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
          "1:1359:1.164.1.5",
          e });
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
           SibTr.exit(tc, "createSilenceMessage", e);

      throw new SIResourceException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
            "1:1369:1.164.1.5",
            e },
          null),
           e);
    }

    // As we are using the Guaranteed Header - set all the attributes as 
    // well as the ones we want.
    SIMPUtils.setGuaranteedDeliveryProperties(sMsg,
        _messageProcessor.getMessagingEngineUuid(), 
        null,
        stream,
        null,
        _destinationHandler.getUuid(),
        ProtocolType.PUBSUBINPUT,
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
   * Creates a FLUSHED message for sending
   *
   * @param target The target cellule (er ME) for the message.
   * @param stream The UUID of the stream the message should be sent on
   * @return the new FLUSHED message
   * @throws SIResourceException if the message can't be created.
   */
  private ControlFlushed createControlFlushed(SIBUuid8 target, SIBUuid12 stream)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createControlFlushed", stream);

    ControlFlushed flushedMsg;

    // Create new message
    try
    {
      flushedMsg = _cmf.createNewControlFlushed();
    }
    catch (MessageCreateFailedException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.PubSubOutputHandler.createControlFlushed",
        "1:1424:1.164.1.5",
        this);
        
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.exception(tc, e);
        SibTr.exit(tc, "createControlFlushed", e);
      }
      
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
          "1:1436:1.164.1.5",
          e });

      throw new SIResourceException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
            "1:1444:1.164.1.5",
            e },
          null),
        e);
    }

    // As we are using the Guaranteed Header - set all the attributes as
    // well as the ones we want.
    SIMPUtils.setGuaranteedDeliveryProperties(flushedMsg,
        _messageProcessor.getMessagingEngineUuid(), 
        null,
        stream,
        null,
        _destinationHandler.getUuid(),
        ProtocolType.PUBSUBINPUT,
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
   * @param target The target cellule (er ME) for the message.
   * @param stream The UUID of the stream the message should be sent on.
   * @param reqID The request ID that the message answers.
   * @return the new NOTFLUSHED message.
   * @throws SIResourceException if the message can't be created.
   */
  private ControlNotFlushed createControlNotFlushed(SIBUuid8 target, SIBUuid12 stream, long reqID)
    throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "createControlNotFlushed", new Object[] {target, stream, new Long(reqID)});

    ControlNotFlushed notFlushedMsg;

    // Create new message
    try
    {
      notFlushedMsg = _cmf.createNewControlNotFlushed();
    }
    catch (MessageCreateFailedException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.PubSubOutputHandler.createControlNotFlushed",
        "1:1498:1.164.1.5",
        this);
        
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.exception(tc, e);
        SibTr.exit(tc, "createControlNotFlushed", e);
      }
      
      SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
          "1:1510:1.164.1.5",
          e });

      throw new SIResourceException(
        nls.getFormattedMessage(
          "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
            "1:1518:1.164.1.5",
            e },
          null),
        e);
    }

    // As we are using the Guaranteed Header - set all the attributes as
    // well as the ones we want.
    SIMPUtils.setGuaranteedDeliveryProperties(notFlushedMsg,
        _messageProcessor.getMessagingEngineUuid(), 
        null,
        stream,
        null,
        _destinationHandler.getUuid(),
        ProtocolType.PUBSUBINPUT,
        GDConfig.PROTOCOL_VERSION);

    notFlushedMsg.setPriority(SIMPConstants.CTRL_MSG_PRIORITY);
    notFlushedMsg.setReliability(Reliability.ASSURED_PERSISTENT);
    
    notFlushedMsg.setRequestID(reqID);

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "createControlNotFlushed");

    return notFlushedMsg;
  }

 /**
   * @param jsMsg
   * @return jsMsg with link properties added
   */
  private JsMessage addLinkProps(JsMessage msg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "addLinkProps", msg);
    // Add Link specific properties to message
    msg.setGuaranteedCrossBusLinkName( _linkName );
    msg.setGuaranteedCrossBusSourceBusUUID( _messageProcessor.getMessagingEngineBusUuid() );
    
    // Check whether we need to override the outbound Userid
    if( _linkSetOutboundUserId )
    {
      // Check whether this message was sent by the privileged 
      // Jetstream SIBServerSubject.If it was then we don't reset
      // the userid in the message
      if(!_messageProcessor.getAuthorisationUtils().sentBySIBServer(msg))
      {            
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
          SibTr.debug(tc, "Set outbound userid: " + _linkOutboundUserid + ", in message");                   
        // Call SIB.security (ultimately) to set outbounduserid into msg
        _messageProcessor.
          getAccessChecker().
          setSecurityIDInMessage(_linkOutboundUserid, msg);    
        // Set the application userid (JMSXuserid)
        msg.setApiUserId(_linkOutboundUserid);
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
   private ControlMessage addLinkProps(ControlMessage msg)
   {
     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "addLinkProps", msg);
     // Add Link specific properties to message
     msg.setGuaranteedCrossBusLinkName( _linkName );
     msg.setGuaranteedCrossBusSourceBusUUID( _messageProcessor.getMessagingEngineBusUuid() );

     if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "addLinkProps");

     return msg;
   } 
  
  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.UpstreamControl#sendNackMessage(com.ibm.ws.sib.trm.topology.Cellule, long, long, boolean, int, com.ibm.websphere.sib.Reliability, com.ibm.ws.sib.utils.SIBUuid12)
   */
  public void sendNackMessage(SIBUuid8 meUuid, 
                              SIBUuid12 destUuid,
                              SIBUuid8  busUuid,  
                              long startTick, 
                              long endTick, 
                              int priority, 
                              Reliability reliability, 
                              SIBUuid12 streamID)
  throws SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendNackMessage", 
        new Object[] { meUuid,
                       new Long(startTick),
                       new Long(endTick),
                       new Integer(priority),
                       reliability,
                       streamID });

    ControlNack newNackMsg =
      createControlNackMessage(
        priority,
        reliability,
        streamID);
    
    newNackMsg.setStartTick(startTick);
    newNackMsg.setEndTick(endTick);
        
    try
    {
      _parentInputHandler.handleControlMessage(null, newNackMsg);
    }
    catch (SIException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.PubSubOutputHandler.sendNackMessage",
        "1:1639:1.164.1.5",
        this);
        
      SibTr.exception(tc, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.exception(tc, e);
        SibTr.exit(tc, "sendNackMessage", e);
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendNackMessage");
  }

  public long sendNackMessageWithReturnValue(SIBUuid8 meUuid, SIBUuid12 destUuid,
    SIBUuid8  busUuid, long startTick, long endTick, int priority, Reliability reliability, 
    SIBUuid12 streamID) throws SIResourceException {
	  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendNackMessageWithReturnValue", 
        new Object[] { meUuid,new Long(startTick), new Long(endTick),
                       new Integer(priority), reliability, streamID });
	  
    long returnValue = -1;

    ControlNack newNackMsg = createControlNackMessage(priority, reliability, streamID);	    
    newNackMsg.setStartTick(startTick);
    newNackMsg.setEndTick(endTick);
	        
    try {
      returnValue = _parentInputHandler.handleControlMessageWithReturnValue(null, newNackMsg);
    }
    catch (SIException e) {
	      // FFDC
      FFDCFilter.processException(e,
	        "com.ibm.ws.sib.processor.impl.PubSubOutputHandler.sendNackMessageWithReturnValue",
	        "1:1621:1.165", this);
	        
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
        SibTr.exception(tc, e);
        SibTr.exit(tc, "sendNackMessageWithReturnValue", e);
      }
    }
	    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendNackMessageWithReturnValue", new Long(returnValue));
	    
    return returnValue;  	
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.UpstreamControl#sendAckMessage(com.ibm.ws.sib.trm.topology.Cellule, long, int, com.ibm.websphere.sib.Reliability, com.ibm.ws.sib.utils.SIBUuid12)
   */
  public void sendAckMessage(
    SIBUuid8 meUuid,
    SIBUuid12 destUuid,
    SIBUuid8  busUuid,  
    long ackPrefix,
    int priority,
    Reliability reliability,
    SIBUuid12 streamID,
    boolean consolidate)
    throws SIResourceException
  {  
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "sendAckMessage",
        new Object[] { meUuid, 
                       new Long(ackPrefix),
                       new Integer(priority),
                       reliability,
                       streamID });

    ControlAck newAckMsg =
      createControlAckMessage(
        priority,
        reliability,
        streamID);
    
    newAckMsg.setAckPrefix(ackPrefix);        
    
    try
    {
      _parentInputHandler.handleControlMessage(null, newAckMsg);
    }
    catch (SIException e)
    {
      // FFDC
      FFDCFilter.processException(
        e,
        "com.ibm.ws.sib.processor.impl.PubSubOutputHandler.sendAckMessage",
        "1:1731:1.164.1.5",
        this);
        
      SibTr.exception(tc, e);
      
      if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      {
        SibTr.exception(tc, e);
        SibTr.exit(tc, "sendAckMessage", e);
      }
    }
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "sendAckMessage");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.UpstreamControl#sendAreYouFlushedMessage(com.ibm.ws.sib.trm.topology.Cellule, long, com.ibm.ws.sib.utils.SIBUuid12)
   */
  public void sendAreYouFlushedMessage(
    SIBUuid8 meUuid, 
    SIBUuid12 destUuid,
    SIBUuid8  busUuid,  
    long queryID, 
    SIBUuid12 streamID)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "sendAreYouFlushedMessage", 
        new Object[]{meUuid, new Long(queryID), streamID});
        
    InvalidOperationException e = 
      new InvalidOperationException(nls.getFormattedMessage(
        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
          "1:1766:1.164.1.5" },
        null));
    
    // FFDC
    FFDCFilter.processException(
      e,
      "com.ibm.ws.sib.processor.impl.PubSubOutputHandler.sendAreYouFlushedMessage",
      "1:1773:1.164.1.5",
      this);
      
    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
      new Object[] {
        "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
        "1:1779:1.164.1.5" });
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "sendAreYouFlushedMessage", e);
    throw e;  
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.UpstreamControl#sendRequestFlushMessage(com.ibm.ws.sib.trm.topology.Cellule, long, com.ibm.ws.sib.utils.SIBUuid12)
   */
  public void sendRequestFlushMessage(
    SIBUuid8 meUuid,
    SIBUuid12 destUuid,
    SIBUuid8  busUuid,  
    long queryID, 
    SIBUuid12 streamID,
    boolean indoubtDiscard)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "sendRequestFlushMessage", 
        new Object[]{meUuid, new Long(queryID), streamID});
    InvalidOperationException e = 
      new InvalidOperationException(nls.getFormattedMessage(
        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
        new Object[] {
          "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
          "1:1804:1.164.1.5" },
        null));
    
    // FFDC
    FFDCFilter.processException(
      e,
      "com.ibm.ws.sib.processor.impl.PubSubOutputHandler.sendRequestFlushMessage",
      "1:1811:1.164.1.5",
      this);
      
    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
    new Object[] {
      "com.ibm.ws.sib.processor.impl.PubSubOutputHandler",
      "1:1817:1.164.1.5" });
      
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "sendRequestFlushMessage", e);
    throw e;  
  }

  /**
   * @param ack
   * @param min
   * @return
   * @throws SIResourceException
   */
  public long checkAck(ControlAck ack, long min) throws SIResourceException
  {
    return _internalOutputStreamManager.checkAck(ack,min);
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener#registerForEventsPostAddItem(com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage)
   */
  public void registerForEventsPostAddItem(SIMPMessage msg)
  {
  }
  
  /**
   * This method is called by the input handler when it flushes a local stream.
   * This is our cue to remove any data structures we may be maintaining for
   * this stream.
   * 
   * @param stream The flushed stream
   */
  public void removeStream(SIBUuid12 stream)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(tc, "removeStream", stream);
    // nuke the stream out of our internal store
    _internalOutputStreamManager.remove(stream);
    
    deregisterControlAdapterMBean();
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "removeStream");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.OutputHandler#isWLMGuess()
   */
  public boolean isWLMGuess()
  {
    return _isGuess;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.OutputHandler#setWLMGuess(boolean)
   */
  public void setWLMGuess(boolean guess)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "setWLMGuess", new Boolean(guess));

    this._isGuess = guess;
    
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
       SibTr.exit(tc, "setWLMGuess");
  }
  
  /* 
   * @return flag indicating whether this handler is owned by a Link
   */
  public boolean isLink()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "isLink");
      SibTr.exit(tc, "isLink", new Boolean(_isLink));
    }
    return _isLink;
  }

  /**
   * @return the foreign bus topicspace name that this sub maps to
   */
  public String getTopicSpaceMapping()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
    {
      SibTr.entry(tc, "getTopicSpaceMapping"); 
      SibTr.exit(tc, "getTopicSpaceMapping", _foreignTSName);
    }
    return _foreignTSName;
  }

  /**
   * Creates the JsDestinationAddress object passed over in publications
   * to foreign buses.
   * 
   * @param String The foreign topicspace mapping
   */
  public void setTopicSpaceMapping(String foreignTSName)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) 
      SibTr.entry(tc, "setTopicSpaceMapping", foreignTSName);
    // Create routingDestination object
    _routingDestination =
      SIMPUtils.createJsDestinationAddress(foreignTSName, 
                                            null, 
                                            _neighbour.getBusId());
    
    this._foreignTSName = foreignTSName;
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(tc, "setTopicSpaceMapping");
  }

  /**
   * This method returns true if this outputhandler's itemstream has reached QHighMessages
   * For PubSubOutputHandler, this check is not applicable since TopicSpace itemstream limit
   * is only checked on publish in PubSubInputHandler. 
   * For PubSubOutputHandler, this method is never called. 
   *   
   * @return boolean false
   */
  public boolean isQHighLimit()
  {
    return false;
  }

  public ConsumerDispatcherState getConsumerDispatcherState()
  {
    return _subscriptionState;
  }
  
  public String getBusName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getBusName");
      SibTr.exit(tc, "getBusName", _busName);
    } 
    return _busName;
  }
  

  /**
   * @return The internal output stream manager
   */
  public InternalOutputStreamManager getInternalOutputStreamManager()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getInternalOutputStreamManager");
      SibTr.exit(tc, "getInternalOutputStreamManager", _internalOutputStreamManager);
    }      
    return _internalOutputStreamManager;
  }
  
  public String getDestinationName()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getDestinationName");
      SibTr.exit(tc, "getDestinationName", _destName);
    }    
    return _destName;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#getControlAdapter()
   */
  public ControlAdapter getControlAdapter()
  {
    return _controlAdapter;
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#createControlAdapter()
   */
  public void createControlAdapter()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
     SibTr.entry(tc, "createControlAdapter");
    
    //we have no remote topic space control yet, but the fact
    //is that one might exist due to a remote get being performed
    //from this remote ME
    //We must go through the existing AIHs and see.
    Map aihMap = _destinationHandler.getPseudoDurableAIHMap();
    Iterator aihIterator = aihMap.values().iterator();
    SIBUuid8 psohRemoteMEUuid = getTargetMEUuid();
    while(aihIterator.hasNext())
    {
      AnycastInputHandler aih = (AnycastInputHandler)aihIterator.next();
      //get the remote ME uuid for this AIH
      SIBUuid8 aihRemoteMEUuid = aih.getLocalisationUuid();
      //see if it is for the same me as this PSOH
      if(aihRemoteMEUuid.equals(psohRemoteMEUuid))
      {
        //the control adapter for the remote TS alaready exists
        //so we just update it
        AttachedRemoteSubscriberControl attachedRSControl = aih.getControlAdapter();
        // We want to work on the RemoteTopicSpaceControl rather than the AttachedRemoteSubcriberControl
        _controlAdapter = attachedRSControl.getRemoteTopicSpaceControl();
        
        _controlAdapter.registerControlAdapterAsMBean();
        break;
      }
    }
    if(_controlAdapter==null)
    {
      //there are no AIHs for getting messages from this remote ME
      //We create a new one
      if (link != null)
      {
        _controlAdapter = new LinkRemoteTopicSpaceControl(this, _messageProcessor, link.getName());
        // Dont register MBean until a message is sent
      }
      else
      {
        _controlAdapter = new RemoteTopicSpaceControl(this, null, _messageProcessor);
        _controlAdapter.registerControlAdapterAsMBean();
      }
    } 
         
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
     SibTr.exit(tc, "createControlAdapter");
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#dereferenceControlAdapter()
   */
  public void dereferenceControlAdapter()
  {
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#registerControlAdapterAsMBean()
   */
  public void registerControlAdapterAsMBean()
  {
    if (!isRegistered)
    {
      _controlAdapter.registerControlAdapterAsMBean();
      isRegistered=true;
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#deregisterControlAdapterMBean()
   */
  public void deregisterControlAdapterMBean()
  {
    _controlAdapter.deregisterControlAdapterMBean();
    isRegistered=false;
  }

  public DestinationHandler getDestinationHandler() 
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    {
      SibTr.entry(tc, "getDestinationHandler");
      SibTr.exit(tc, "getDestinationHandler", _destinationHandler);
    } 
    return _destinationHandler;
  }

  public long handleControlMessageWithReturnValue(SIBUuid8 sourceMEUuid,
    ControlMessage cMsg) throws SIIncorrectCallException,
    SIResourceException, SIConnectionLostException, SIRollbackException {
    return 0;
  }  
}
