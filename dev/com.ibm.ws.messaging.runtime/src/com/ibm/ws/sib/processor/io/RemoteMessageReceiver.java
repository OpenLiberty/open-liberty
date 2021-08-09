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
package com.ibm.ws.sib.processor.io;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.JsAdminUtils;
import com.ibm.ws.sib.mfp.AbstractMessage;
import com.ibm.ws.sib.mfp.JsApiMessage;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageCreateFailedException;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.mfp.control.ControlDecisionExpected;
import com.ibm.ws.sib.mfp.control.ControlMessage;
import com.ibm.ws.sib.mfp.control.ControlAreYouFlushed;
import com.ibm.ws.sib.mfp.control.ControlFlushed;
import com.ibm.ws.sib.mfp.control.ControlAccept;
import com.ibm.ws.sib.mfp.control.ControlReject;
import com.ibm.ws.sib.mfp.control.ControlRequest;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.gd.GDConfig;
import com.ibm.ws.sib.processor.impl.DestinationManager;
import com.ibm.ws.sib.processor.impl.DurableInputHandler;
import com.ibm.ws.sib.processor.impl.DurableOutputHandler;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.indexes.statemodel.State;
import com.ibm.ws.sib.processor.impl.interfaces.ControlHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.InputHandler;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.UserTrace;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * @author tevans
 */
public final class RemoteMessageReceiver
{
  //trace
  private static final TraceComponent tc =
    SibTr.register(
      RemoteMessageReceiver.class,
      SIMPConstants.MP_TRACE_GROUP,
      SIMPConstants.RESOURCE_BUNDLE);

  // NLS for component
  private static final TraceNLS nls_mt =
    TraceNLS.getTraceNLS(SIMPConstants.TRACE_MESSAGE_RESOURCE_BUNDLE);

  
  private MessageProcessor _messageProcessor;
  private DestinationManager _destinationManager;
  private SIMPTransactionManager _txManager;
  private SIBUuid8 _localMEUuid;
  private SIBUuid8 _localBus;
  private String _localBusName;
  private MPIO _mpio;

  RemoteMessageReceiver(MessageProcessor messageProcessor, MPIO mpio)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "RemoteMessageReceiver", new Object[]{messageProcessor, mpio});

    _messageProcessor = messageProcessor;
    _mpio = mpio;
    init();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "RemoteMessageReceiver", this);
  }

  protected void init()
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "init");

    _destinationManager = _messageProcessor.getDestinationManager();
    _txManager = _messageProcessor.getTXManager();
    _localMEUuid = _messageProcessor.getMessagingEngineUuid();
    _localBus = _messageProcessor.getMessagingEngineBusUuid();
    _localBusName = _messageProcessor.getMessagingEngineBus();

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "init");
  }

  void receiveMessage(AbstractMessage aMessage)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "receiveMessage", new Object[]{aMessage});

    SIBUuid8 targetMEUuid = aMessage.getGuaranteedTargetMessagingEngineUUID();

      // If the targetUuid is not set then use our already created messagingEngineCellule
    if (targetMEUuid == null)
      targetMEUuid = _localMEUuid;

    // Get source me uuid
    SIBUuid8 sourceMEUuid = aMessage.getGuaranteedSourceMessagingEngineUUID();

    // Trace out the interesting parts of the message
    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
    {
      SibTr.debug(tc, "localMEUuid     : " + _localMEUuid);
      SibTr.debug(tc, "localBus         : " + _localBus);
      MPIOMsgDebug.debug(tc, aMessage, -1);
    }

    if( targetMEUuid.equals( _localMEUuid ) )
    {
      DestinationHandler dest = null;
      String destId = null;
      SIBUuid8 sourceBusUUID = aMessage.getGuaranteedCrossBusSourceBusUUID();
      // If this message was sent from another bus get the LinkHandler
      if(  ( sourceBusUUID != null )
         &&( !sourceBusUUID.equals(_localBus) ))
      {
        dest = _destinationManager.getLink(aMessage.getGuaranteedCrossBusLinkName());
        if( dest == null )
        {
          // We don't have a link defined at this end
          // Log error and discard this message
          SibTr.error(tc,"LINK_NOT_FOUND_ERROR_CWSIP0041",
                      new Object[]{aMessage.getGuaranteedCrossBusLinkName(),
                                   _messageProcessor.getMessagingEngineName()});

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "receiveMessage");

          return;
        }
      }
      else
      {
        // The message came from another ME in our bus
        try
        {
          JsDestinationAddress routingDestination = aMessage.getRoutingDestination();

          // If the message has a routing destination that is in the local bus then
          // we may need to parse the destination name to decide who will process it.
          // If we have a routing destination but it is for a destination in a foreign
          // bus then we must already have the UUID of the link that it is to be transmitted
          // on in the message (as we already know that it originated in this bus),
          // so we'll use that instead.
          if(routingDestination != null &&
             (_localBusName.equals(routingDestination.getBusName()) ||
              routingDestination.getBusName() == null))
          {
            destId = routingDestination.getDestinationName();
            // If the message is for a temporary destination, send it to the TDReceiver
            // system queue.
            if(destId.startsWith(SIMPConstants.TEMPORARY_PUBSUB_DESTINATION_PREFIX) ||
               destId.startsWith(SIMPConstants.TEMPORARY_QUEUE_DESTINATION_PREFIX) )
            {
              //Get the Temporary Destination Receiver
              dest = _destinationManager.getDestination(_messageProcessor.getTDReceiverAddr(), false);
            }
            // If the message is for a system queue then look it up by its name
            // (the sending ME doesn't have its UUID)
            else if(destId.startsWith(SIMPConstants.SYSTEM_DESTINATION_PREFIX))
            {
              dest = _destinationManager.getDestination(destId, false);
            }
            else
            {
              SIBUuid12 destID = aMessage.getGuaranteedTargetDestinationDefinitionUUID();

              // For logging purposes
              if (destID != null) destId = destID.toString();

              dest =  _destinationManager.getDestinationInternal(destID, true);
            }

          }
          else
          {
            SIBUuid12 destID = aMessage.getGuaranteedTargetDestinationDefinitionUUID();
            // For logging purposes
            if (destID != null) destId = destID.toString();

            if(aMessage.isControlMessage())
            {
              // Some of the durable messages are "out of band" (i.e. not associated with
              // a particular destination), so we short circuit those here.
              ProtocolType msgType = aMessage.getGuaranteedProtocolType();

              if (msgType == ProtocolType.DURABLEINPUT)
              {
                DurableInputHandler.staticHandleControlMessage((ControlMessage) aMessage);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                      SibTr.exit(tc, "receiveMessage");
                return;
              }
              else if (msgType == ProtocolType.DURABLEOUTPUT)
              {
                // Trap DURABLEOUTPUT messages since these may be out of band (i.e. not associated
                // with a particular destination).
                DurableOutputHandler.staticHandleControlMessage(
                  (ControlMessage) aMessage,
                  _destinationManager,
                  _messageProcessor);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                      SibTr.exit(tc, "receiveMessage");
                return;
              }
            }
            //We DO want to include invisible destination handlers in our search.
            //The reason is, though invisible destinations should not be able to
            //produce/consume new messages, they SHOULD be able to receive
            //messages that are already in transit, because otherwise the sending
            //ME does not know for certain if the message has been received and
            //so risks duplicating messages
            dest =  _destinationManager.getDestinationInternal(destID, true);
          }
        }
        catch (SINotPossibleInCurrentConfigurationException e)
        {
          // No FFDC code needed
          // The destination was not found, this is handled below
          dest = null;
        }
        catch (SIException e)
        {

          // FFDC
          FFDCFilter.processException(
            e,
            "com.ibm.ws.sib.processor.io.RemoteMessageReceiver.receiveMessage",
            "1:296:1.120",
            this);

          // We should not have received this message, throw the message away
          // Use the generic internal messaging error
          SibTr.error(
            tc,
            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
            new Object[]{"com.ibm.ws.sib.processor.io.RemoteMessageReceiver.receiveMessage",
                "1:305:1.120"} );

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exception(tc, e);

          // We weren't expecting that, exit
          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "receiveMessage");

          return;
        }

        if(!aMessage.isControlMessage())
        {
          traceApiMessage((JsMessage) aMessage, dest);
        }

        if (dest == null)
        {
          // Ignore messages sent to a topicspace or queue that is not found.
          // Assumption is that it has been deleted on this ME and will
          // be deleted on the sending ME.

          // Trace the fact that this happened, all the info we need is
          // already traced out
          if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(tc, "Message received for unknown destination");

          if(aMessage.isControlMessage())
          {
            processUnknownControlMessage((ControlMessage) aMessage);
          }

          if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "receiveMessage");
          return;
        }
      }

      try
      {
        // It's possible for a remote message to arrive for a destinationt that hasn't
        // completed its creation (as it's advertised before the commit of the create).
        // If this happens we can't process this message as we don't know what condition
        // the various parts of the destination are in. For this reason we just ignore
        // messages for destinations in this state. If the sender is that worried, they'll
        // resend it again soon (using their own retry logic)
        State state;
        if(dest.isLink())
          state = _destinationManager.getLinkIndex().getState(dest);
        else
          state = _destinationManager.getDestinationIndex().getState(dest);

        if((state == null) || !state.isCreateInProgress())
        {
          if(aMessage.isControlMessage())
          {
            processControlMessage( (ControlMessage) aMessage, sourceMEUuid, targetMEUuid, dest, state);
          }
          else
          {
            processJsMessage((JsMessage) aMessage, sourceMEUuid, targetMEUuid, dest, state);
          }
        }
        else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
        {
          SibTr.debug(tc, "Message received for destination or link currently being created", dest);
        }
      }
      catch (SIException e)
      {
        // No FFDC code needed
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exception(tc, e);
      }
    }
    else
    {
      // Message is targeted at a different ME
      forwardMessage(aMessage, targetMEUuid);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "receiveMessage");
  }

  private void traceApiMessage(JsMessage msg, DestinationHandler dest)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "traceApiMessage", new Object[]{msg,dest});

    if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
    {
      if (msg.isApiMessage())
      {
        String destName = null;

        String text = "INBOUND_MESSAGE_RECEIVED_CWSJU0020";

        if (dest!=null)
        {
          destName = dest.getName();
          if (destName.startsWith(SIMPConstants.TEMPORARY_QUEUE_DESTINATION_PREFIX) ||
              destName.startsWith(SIMPConstants.TEMPORARY_PUBSUB_DESTINATION_PREFIX))
            text = "INBOUND_MESSAGE_RECEIVED_TEMP_CWSJU0120";
        }

        String apiMsgId = null;
        String correlationId = null;

        if (msg instanceof JsApiMessage)
        {
          apiMsgId = ((JsApiMessage)msg).getApiMessageId();
          correlationId = ((JsApiMessage)msg).getCorrelationId();
        }
        else
        {
          if (msg.getApiMessageIdAsBytes() != null)
            apiMsgId = new String(msg.getApiMessageIdAsBytes());

          if (msg.getCorrelationIdAsBytes() != null)
            correlationId = new String(msg.getCorrelationIdAsBytes());
        }

        SibTr.debug(UserTrace.tc_mt,
          nls_mt.getFormattedMessage(
            text,
            new Object[] {
              apiMsgId,
              msg.getSystemMessageId(),
              correlationId,
              msg.getSystemMessageSourceUuid(),
              destName},
            null));
      }
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "traceApiMessage");
  }

  private void processUnknownControlMessage(ControlMessage ctlMsg)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processUnknownControlMessage", new Object[]{ctlMsg});

    if (ctlMsg.getGuaranteedProtocolType().equals(ProtocolType.ANYCASTINPUT))
    {
      if (ctlMsg instanceof ControlDecisionExpected)
      {
        ControlDecisionExpected cde = (ControlDecisionExpected) ctlMsg;
        /**
         * 466323
         * For incoming anycast messages from a DME which we do not have any corresponding
         * RME resources defined, we ignore the msg. We output a warning containg enough
         * information for the administrator to be able to clean up the DME streams for
         * this RME
         */

        // Lookup the remote me name if possible
        SIBUuid8 remoteUuid = cde.getGuaranteedSourceMessagingEngineUUID();
        String meName =
          JsAdminUtils.getMENameByUuid(remoteUuid.toString());
        if (meName==null)
          meName = remoteUuid.toString();

        // We use the suppressor to avoid multiple messages for the same destination.
        SibTr.warning(tc,
                SibTr.Suppressor.ALL_FOR_A_WHILE_SIMILAR_INSERTS,
                "UNEXPECTED_MESSAGE_RECEIVED_CWSIP0784",
                new Object[] { cde.getGuaranteedGatheringTargetUUID(),
                               meName,
                               cde.getTick(),
                               _messageProcessor.getMessagingEngineName()});
      }
      // else we ignore the message, if the DME is not cleaned up it should eventually start
      // sending us ControlDecisionExpecteds repeatedly
    }

//  There are two cases where we need to send something back:
    //
    // 1) For AreYouFlushed on PtoP or PubSub.  This is required to
    //    make destination deletion live.
    //
    // 2) For Accept, Reject or Get on Anycast.  This is also required
    //    to make destination deletion live.
    //
    // In either case we reply with Flushed using the appropriate protocol,
    // reliability and stream ID.
    if ((ctlMsg instanceof ControlAreYouFlushed) ||
        (ctlMsg instanceof ControlAccept) ||
        (ctlMsg instanceof ControlReject) ||
        (ctlMsg instanceof ControlRequest))
    {
      ProtocolType   pt         = ctlMsg.getGuaranteedProtocolType();
      ControlFlushed flushedMsg = null;

      // Flip the protocol type
      if (pt.equals(ProtocolType.ANYCASTOUTPUT))
        pt = ProtocolType.ANYCASTINPUT;
      else if (pt.equals(ProtocolType.UNICASTOUTPUT))
        pt = ProtocolType.UNICASTINPUT;
      else if (pt.equals(ProtocolType.PUBSUBOUTPUT))
        pt = ProtocolType.PUBSUBINPUT;

      // Create new message
      try
      {
        flushedMsg = MessageProcessor.getControlMessageFactory().createNewControlFlushed();
      }
      catch (MessageCreateFailedException e)
      {
        // FFDC
        FFDCFilter.processException(
          e,
          "com.ibm.ws.sib.processor.io.RemoteMessageReceiver.processUnknownControlMessage",
          "1:520:1.120",
          this);

        // Unfortunately, we have to eat the exception here, but at
        // least we logged it
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
          SibTr.exception(tc, e);
          SibTr.exit(tc, "processUnknownControlMessage", e);
        }

        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
          new Object[] {
            "com.ibm.ws.sib.processor.io.RemoteMessageReceiver",
            "1:534:1.120",
            e });

      }

      // Set the header.
      SIMPUtils.setGuaranteedDeliveryProperties(flushedMsg,
          _messageProcessor.getMessagingEngineUuid(),
          ctlMsg.getGuaranteedSourceMessagingEngineUUID(),
          ctlMsg.getGuaranteedStreamUUID(),
          null,
          ctlMsg.getGuaranteedTargetDestinationDefinitionUUID(),
          pt,
          GDConfig.PROTOCOL_VERSION);

      flushedMsg.setPriority(SIMPConstants.CTRL_MSG_PRIORITY);
      flushedMsg.setReliability(ctlMsg.getReliability());
      flushedMsg.setMediated(ctlMsg.isMediated());

      // Last but not least, send the message out
      SIBUuid8 targetUuid = ctlMsg.getGuaranteedSourceMessagingEngineUUID();
      _mpio.sendToMe(targetUuid,SIMPConstants.CTRL_MSG_PRIORITY,flushedMsg);
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processUnknownControlMessage");
  }


  private void processControlMessage(ControlMessage ctlMsg,
                                     SIBUuid8 sourceMEUuid,
                                     SIBUuid8 targetMEUuid,
                                     DestinationHandler dest,
                                     State state)
  throws SIConnectionLostException, SIRollbackException, SIIncorrectCallException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processControlMessage",
                  new Object[]{ctlMsg, sourceMEUuid, targetMEUuid, dest, state});

    ControlHandler controlHandler =
      dest.getControlHandler(ctlMsg.getGuaranteedProtocolType(), sourceMEUuid, ctlMsg);

    if( controlHandler != null)
    {
      controlHandler.handleControlMessage( sourceMEUuid, ctlMsg );
    }
    else if (ctlMsg.getGuaranteedProtocolType() == ProtocolType.ANYCASTINPUT)
    {
      if (ctlMsg instanceof ControlDecisionExpected)
      {
        ControlDecisionExpected cde = (ControlDecisionExpected) ctlMsg;
        /**
         * 466323
         * For incoming anycast messages from a DME which we do not have any corresponding
         * RME resources defined, we ignore the msg. We output a warning containg enough
         * information for the administrator to be able to clean up the DME streams for
         * this RME
         * Note, remote durable messages would not have got this far as the pseudoDest
         * would not have existed. Rogue remoteDurable msgs are handled in processUnknownControlMessage
         */

        // Lookup the remote me name if possible
        String meName =
          JsAdminUtils.getMENameByUuid(sourceMEUuid.toString());
        if (meName==null)
          meName = sourceMEUuid.toString();

        // We use the suppressor to avoid multiple messages for the same destination.
        SibTr.warning(tc,
                SibTr.Suppressor.ALL_FOR_A_WHILE_SIMILAR_INSERTS,
                "UNEXPECTED_MESSAGE_RECEIVED_CWSIP0784",
                new Object[] { dest.getName(), meName, cde.getTick(), _messageProcessor.getMessagingEngineName() });
      }
      // else we ignore the message, if the DME is not cleaned up it should eventually start
      // sending us ControlDecisionExpecteds repeatedly
    }
    // 541867
    // If we found the destination, but not a control handler for this message it may be because we've
    // cleaned up our end of the stream. Currently this only happens when we delete some of the destination
    // (either the whole destination or a single localisation) and the source end of the stream is flushable.
    // Therefore, if we're being deleted we handle this message as if we've already deleted everything already.
    // Normally this wouldn't occur, but can if a flush got lost (or never sent) and the target doesn't know
    // what to do with a stream it's used in the past.
    // Note: We don't do this if the destination is in unreconciled or corrupt as there may well be some
    //       hidden stream state that isn't flushable yet.
    // TODO: If we ever get round to cleaning up GD streams at any point other than at delete time then we'll
    //       need to work out how to handle the case where one end of the stream has been cleaned up without
    //       the other (i.e. the target end) knowing.
    else if(dest.isToBeDeleted())
    {
      processUnknownControlMessage(ctlMsg);
    }
    else
    {
      // We do not have the correct control handler, this shouldn't happen unless this ME is out of step
      // with the other ME (DataStore deleted??) or we have an incorrectly configured link.
      SibTr.error(
        tc,
        SibTr.Suppressor.ALL_FOR_A_WHILE_SIMILAR_INSERTS,
        "INTERNAL_MESSAGING_ERROR_CWSIP0008",
        new Object[]{"com.ibm.ws.sib.processor.impl.RemoteMessageReceiver.processControlMessage",
                     "1:636:1.120",
                     dest.getName(),
                     sourceMEUuid + ", " + ctlMsg.getControlMessageType() + ", " + state} ); // Hack to get three additional properties
    }
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
          SibTr.exit(tc, "processControlMessage");
  }

  private void processJsMessage(JsMessage jsMsg,
                                SIBUuid8 sourceMEUuid,
                                SIBUuid8 targetMEUuid,
                                DestinationHandler dest,
                                State state)
  throws SIConnectionLostException, SIRollbackException, SINotPossibleInCurrentConfigurationException, SIIncorrectCallException, SIResourceException
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "processJsMessage",
                  new Object[]{jsMsg, sourceMEUuid, targetMEUuid, dest, state});

    InputHandler handler = dest.getInputHandler(jsMsg.getGuaranteedProtocolType(), sourceMEUuid, jsMsg);

    if( handler != null)
    {
    /*
      MessageItem msgItem = new MessageItem(jsMsg);

      msgItem.setFromRemoteME(true);

      // If we have a linkHanlder then this message came from another Bus
      // Defect 238709: An MQLinkHandler is deemed not to be a link
      if( dest.isLink() && !dest.isMQLink())
        msgItem.setFromRemoteBus(true);

      // Call the handler to deal with the message - any exceptions are caught
      // higher up
      handler.handleMessage(msgItem
                           ,_txManager.createAutoCommitTransaction()
                           ,sourceMEUuid
                           );*/
      }
    else
    {
      // We do not have the correct control handler, this shouldn't happen unless this ME is out of step
      // with the other ME (DataStore deleted??) or we have an incorrectly configured link.
      SibTr.error(
        tc,
        SibTr.Suppressor.ALL_FOR_A_WHILE_SIMILAR_INSERTS,
        "INTERNAL_MESSAGING_ERROR_CWSIP0008",
        new Object[]{"com.ibm.ws.sib.processor.impl.RemoteMessageReceiver.processJsMessage",
            "1:712:1.120",
            dest.getName(),
            sourceMEUuid + ", " + state} );
    }

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.exit(tc, "processJsMessage");
  }

  /**
   * Forwards a message onto a foreign bus
   * @param aMessage
   * @param sourceCellule
   * @param targetCellule
   */
  private void forwardMessage(AbstractMessage aMessage,
                                SIBUuid8 targetMEUuid)
  {
    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
      SibTr.entry(tc, "forwardMessage",
                  new Object[]{aMessage, targetMEUuid});

    if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled() && !aMessage.isControlMessage())
    {
      JsMessage jsMsg = (JsMessage) aMessage;
      JsDestinationAddress routingDestination = jsMsg.getRoutingDestination();
      if(routingDestination != null)
      {
        String destId = routingDestination.getDestinationName();
        boolean temporary = false;
        if(destId.startsWith(SIMPConstants.TEMPORARY_PUBSUB_DESTINATION_PREFIX))
          temporary = true;

        UserTrace.forwardJSMessage(jsMsg,
                                   targetMEUuid,
                                   destId,
                                   temporary);
      }
      else
      {
        DestinationHandler dest =
          _destinationManager.getDestinationInternal(aMessage.getGuaranteedTargetDestinationDefinitionUUID(), false);

        if (dest != null)
          UserTrace.forwardJSMessage(jsMsg,
                                     targetMEUuid,
                                     dest.getName(),
                                     dest.isTemporary());
        else
          UserTrace.forwardJSMessage(jsMsg,
                                     targetMEUuid,
                                     jsMsg.getGuaranteedTargetDestinationDefinitionUUID().toString(),
                                     false);
      }

    }

    // Send this to MPIO
    _mpio.sendToMe( targetMEUuid,
                    aMessage.getPriority().intValue(),
                    aMessage );

    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
         SibTr.exit(tc, "forwardMessage");
  }

}
