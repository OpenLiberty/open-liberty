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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.JsConstants;
import com.ibm.ws.sib.matchspace.MatchSpaceKey;
import com.ibm.ws.sib.mfp.JsApiMessage;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.mfp.MessageCreateFailedException;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.mfp.control.ControlAck;
import com.ibm.ws.sib.mfp.control.ControlAckExpected;
import com.ibm.ws.sib.mfp.control.ControlAreYouFlushed;
import com.ibm.ws.sib.mfp.control.ControlMessage;
import com.ibm.ws.sib.mfp.control.ControlMessageType;
import com.ibm.ws.sib.mfp.control.ControlNack;
import com.ibm.ws.sib.mfp.control.ControlRequestFlush;
import com.ibm.ws.sib.mfp.control.ControlSilence;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.LockingCursor;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPConnectionLostException;
import com.ibm.ws.sib.processor.exceptions.SIMPErrorException;
import com.ibm.ws.sib.processor.exceptions.SIMPIncorrectCallException;
import com.ibm.ws.sib.processor.exceptions.SIMPLimitExceededException;
import com.ibm.ws.sib.processor.exceptions.SIMPNotAuthorizedException;
import com.ibm.ws.sib.processor.exceptions.SIMPNotPossibleInCurrentConfigurationException;
import com.ibm.ws.sib.processor.exceptions.SIMPResourceException;
import com.ibm.ws.sib.processor.exceptions.SIMPRollbackException;
import com.ibm.ws.sib.processor.gd.ExpressTargetStream;
import com.ibm.ws.sib.processor.gd.GDConfig;
import com.ibm.ws.sib.processor.gd.GuaranteedTargetStream;
import com.ibm.ws.sib.processor.gd.InternalInputStreamManager;
import com.ibm.ws.sib.processor.gd.SourceStream;
import com.ibm.ws.sib.processor.gd.SourceStreamManager;
import com.ibm.ws.sib.processor.gd.StreamSet;
import com.ibm.ws.sib.processor.gd.TickRange;
import com.ibm.ws.sib.processor.impl.exceptions.FlushAlreadyInProgressException;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidOperationException;
import com.ibm.ws.sib.processor.impl.interfaces.Browsable;
import com.ibm.ws.sib.processor.impl.interfaces.BrowseCursor;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl;
import com.ibm.ws.sib.processor.impl.interfaces.FlushComplete;
import com.ibm.ws.sib.processor.impl.interfaces.InputHandlerStore;
import com.ibm.ws.sib.processor.impl.interfaces.MessageProducer;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.BatchHandler;
import com.ibm.ws.sib.processor.impl.store.MessageEvents;
import com.ibm.ws.sib.processor.impl.store.filters.ClassEqualsFilter;
import com.ibm.ws.sib.processor.impl.store.filters.MessageSelectorFilter;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.impl.store.items.MessageItemReference;
import com.ibm.ws.sib.processor.impl.store.itemstreams.ProtocolItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.ProxyReferenceStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PubSubMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPReferenceStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.TargetProtocolItemStream;
import com.ibm.ws.sib.processor.matching.MatchingConsumerDispatcher;
import com.ibm.ws.sib.processor.matching.MessageProcessorMatching;
import com.ibm.ws.sib.processor.matching.MessageProcessorSearchResults;
import com.ibm.ws.sib.processor.matching.TopicAuthorization;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.UserTrace;
import com.ibm.ws.sib.security.auth.OperationType;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SILimitExceededException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * The PubSub version of the InputHandler.
 * 
 * The InputHandler attaches itself to a Destination which is
 * a TopicSpace.
 */
public final class PubSubInputHandler
                extends AbstractInputHandler
                implements DownstreamControl,
                Browsable,
                InputHandlerStore
{
    // NLS for component
    private static final TraceNLS nls =
                    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

    private static final TraceComponent tc =
                    SibTr.register(
                                   PubSubInputHandler.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    // NLS for component
    private static final TraceNLS nls_mt =
                    TraceNLS.getTraceNLS(SIMPConstants.TRACE_MESSAGE_RESOURCE_BUNDLE);
    private static final TraceNLS nls_cwsik =
                    TraceNLS.getTraceNLS(SIMPConstants.CWSIK_RESOURCE_BUNDLE);

    // The source stream manager maintains stream state for
    // the stream originated by the handler.  There can be
    // at most one of these streams at a time.  A flush replaces
    // the current stream with a new stream.
    private final SourceStreamManager _sourceStreamManager;

    private final BatchHandler _sourceBatchHandler;

    // This is a multi-keyed map which aggregates stream state
    // for one or more streams stored in output handlers.
    // The stream stored in sourceStreamManager is NOT stored
    // in this map.
    private final InternalInputStreamManager _internalInputStreamManager;

    // A cache for the name of the cellule (er ME) implemented
    // by the local message processor.
    private final SIBUuid8 _localMEUuid;

    private final SIMPReferenceStream _proxyReferenceStream;

    private final MessageProcessorMatching _matchspace;

    // The itemstream used to store publications.
    private final PubSubMessageItemStream _itemStream;

    // This maps stream IDs to the neighboring cellule which
    // originated the stream.  I.e. this map keeps track of
    // upstream paths for streams.  We don't really need
    // this until we have true multihop but it DOES eliminate
    // having to pass both cellules and streams to the various
    // output handlers.
    private final HashMap<SIBUuid12, SIBUuid8> _originStreamMap;

    // If true, then a pending flush for delete from all source
    // nodes has completed successfully.
    private boolean _flushedForDeleteSource = false;

    // If non-null then we're attempting to flush all source
    // streams so we can delete the destination.
    private FlushComplete _deleteFlushSource = null;

    // If true, then a pending flush for delete from all target
    // nodes has completed successfully.
    private boolean _flushedForDeleteTarget = false;

    // If non-null, then we're attempting to flush all target
    // streams so we can delete the destination.
    private AlarmListener _deleteFlushTarget = null;

    public PubSubInputHandler(DestinationHandler destination,
                              TargetProtocolItemStream targetProtocolItemStream,
                              PubSubMessageItemStream itemStream,
                              ProxyReferenceStream proxyReferenceStream,
                              ProtocolItemStream protocolItemStream)
    {
        super(destination, targetProtocolItemStream);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "PubSubInputHandler",
                        new Object[] {
                                      destination,
                                      targetProtocolItemStream,
                                      itemStream,
                                      proxyReferenceStream,
                                      protocolItemStream });

        this._proxyReferenceStream = proxyReferenceStream;
        proxyReferenceStream.setPubSubInputHandler(this);

        // Create the sourceStream for this input handler.
        // This stream is only used for messages from local Publishers
        // Note that 'this' is a DownstreamControl.  Stream ID is stored
        // with the map to simplify synchronization during a flush.
        _sourceStreamManager = new SourceStreamManager(_messageProcessor,
                        this,
                        destination,
                        protocolItemStream,
                        destination.getMessageProcessor().getMessagingEngineUuid(),
                        null);

        // Create the internalInputStreams for this input handler.
        // These stream are only used for messages from remote Publishers
        // and sent to remote destinations.
        _internalInputStreamManager = new InternalInputStreamManager(_messageProcessor,
                        this);

        // Work out the sourceCellule for this ME
        _localMEUuid = _messageProcessor.getMessagingEngineUuid();

        this._itemStream = itemStream;
        _matchspace = _messageProcessor.getMessageProcessorMatching();
        _sourceBatchHandler = _messageProcessor.getSourceBatchHandler();

        // Create the map for tracking upstream paths.
        _originStreamMap = new HashMap<SIBUuid12, SIBUuid8>();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "PubSubInputHandler", this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener#messageEventOccurred(int, com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage,
     * com.ibm.ws.sib.msgstore.Transaction)
     */
    @Override
    public void messageEventOccurred(int event,
                                     SIMPMessage msg,
                                     TransactionCommon tran)
                    throws
                    SIDiscriminatorSyntaxException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "messageEventOccurred", new Object[] { new Integer(event), msg, tran });

        if (event == MessageEvents.PRE_PREPARE_TRANSACTION) //183715.1
        {
            eventPrecommitAdd(msg, tran);
        }
        else if (event == MessageEvents.POST_COMMIT_ADD)
        {
            eventPostAdd(msg, tran, false);
        }
        else if (event == MessageEvents.POST_ROLLBACK_ADD)
        {
            eventPostAdd(msg, tran, true);
        }
        else if (event == MessageEvents.POST_COMMITTED_TRANSACTION)
        {
            eventPostCommit(msg);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "messageEventOccurred");
    }

    final protected void eventPrecommitAdd(SIMPMessage msg, TransactionCommon tran)
                    throws
                    SIDiscriminatorSyntaxException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "eventPrecommitAdd", new Object[] { msg, tran });

        if (msg.isTransacted())
        {
            // Perform the PubSub match and delivery
            // Message should never already stored at this point in-case there
            // are no subscribers                                183715.1
            localFanOut((MessageItem) msg, tran, false); //183715.1

            // Release the JsMessage from the parent MessageItem (Any MessageItemReferences will
            // have their own references to the JsMessage (unless they've released their's too)
            msg.releaseJsMessage();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventPrecommitAdd");
    }

    private void eventPostAdd(SIMPMessage msg, TransactionCommon tran, boolean rollback) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "eventPostAdd", new Object[] { msg, tran, new Boolean(rollback) });

        //this should only ever be a reference
        MessageItemReference ref = (MessageItemReference) msg;
        //get the original message
        MessageItem msgItem = null;
        try
        {
            msgItem = (MessageItem) ref.getReferredItem();
        } catch (MessageStoreException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.PubSubInputHandler.eventPostAdd",
                                        "1:385:1.329.1.1",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "eventPostAdd", e);

            throw new SIResourceException(e);
        }

        JsMessage jsMsg = msgItem.getMessage();

        if (rollback)
        {
            _sourceStreamManager.updateSourceStream(msgItem, rollback);
        }
        else
        {
            //Change the message from Uncommited to Value in sourceStream 
            _sourceStreamManager.updateSourceStream(ref, rollback);
        }

        //defect 260440
        //at this point we can set the bus field in the jsMsg as it has already
        //been stored to disk
        jsMsg.setBus(_messageProcessor.getMessagingEngineBus());

        //get the list of matching output handlers for this message
        MessageProcessorSearchResults searchResults = ref.getSearchResults();
        //don't give a monkeys what the topic was, just want the results back!
        List matchingPubsubOutputHandlers = searchResults.getPubSubOutputHandlers(null);
        ArrayList<SIBUuid8> fromTo = new ArrayList<SIBUuid8>(matchingPubsubOutputHandlers.size()); //see defect 285784: 
        //we should not use array as we might end up with null elements (if there
        //are upstream MEs). The NPE that results causes localPut to bomb-out 
        //before msg is delivered to local ConsumerDispatchers. 
        //We use the match list size as an initial capacity as it is 
        //probably quite a good guess.

        //call commitInsert on each of them and build up a list of source/target cellule pairs
        Iterator itr = matchingPubsubOutputHandlers.iterator();
        int i = 0;
        while (itr.hasNext())
        {
            //get the next output handler
            PubSubOutputHandler outputHandler = (PubSubOutputHandler) itr.next();
            if (outputHandler.okToForward(msgItem))
            {
                if (rollback)
                {
                    outputHandler.rollbackInsert(msgItem);
                }
                else
                {
                    outputHandler.commitInsert(msgItem);

                    if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
                        UserTrace.traceOutboundSend(jsMsg,
                                                    outputHandler.getTargetMEUuid(),
                                                    _destination.getName(),
                                                    _destination.isForeignBus() || _destination.isLink(),
                                                    _destination.isMQLink(),
                                                    _destination.isTemporary());
                }

                //add a target cellule to the array for sending
                //or send the message directly from the outputHandler
                //if this is a Link 
                if (outputHandler.isLink())
                {
                    outputHandler.sendLinkMessage(msgItem, rollback);
                }
                else
                {
                    SIBUuid8 targetMEUuid = outputHandler.getTargetMEUuid();
                    fromTo.add(targetMEUuid); //see defect 285784
                    i++;
                }
            }//end ok to forward     
        }

        // call MPIO to finally send the message to the matching remote MEs
        // if there are messages which have not been sent by the OutputHandlers
        if (i > 0)
        {
            //Now that we have a list of fromTo pairs we can obtain an array representation 
            //see defect 285784
            SIBUuid8[] fromToArray =
                            fromTo.toArray(new SIBUuid8[fromTo.size()]);

            if (rollback)
            {
                // Create a silence message on the same stream to take the 
                // place of the rolled back value message.
                ControlMessage cMsg = createSilenceMessage(jsMsg.getGuaranteedValueValueTick(),
                                                           jsMsg.getGuaranteedValueCompletedPrefix(),
                                                           msgItem.getPriority(),
                                                           msgItem.getReliability(),
                                                           jsMsg.getGuaranteedStreamUUID());

                _mpio.sendDownTree(fromToArray, //the list of source target pairs
                                   msgItem.getPriority(), //priority
                                   cMsg); //the Silence message
            }
            else
            {
                _mpio.sendDownTree(fromToArray, //the list of source target pairs
                                   msgItem.getPriority(), //priority
                                   jsMsg); //the JsMessage
            }
        }

        //just to be safe, nullify the reference's reference to the results object...
        ref.setSearchResults(null);

        // Release the JsMessage from the outbound reference now that we've sent it, if we
        // ever need it again (e.g. due to a nack) we can get the root item to restore it for
        // us.
        msg.releaseJsMessage();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventPostAdd");
    }

    /**
     * Called by one of our input stream data structures when we don't have
     * any info for a tick and need to nack upstream.
     * 
     * @param startTick
     * @param endTick
     */
    @Override
    public void sendNackMessage(
                                SIBUuid8 upstream,
                                SIBUuid12 destUuid,
                                SIBUuid8 busUuid,
                                long startTick,
                                long endTick,
                                int priority,
                                Reliability reliability,
                                SIBUuid12 stream) throws SIResourceException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "sendNackMessage",
                        new Object[] { new Long(startTick), new Long(endTick) });

        ControlNack nackMsg = createControlNackMessage(priority, reliability, stream);

        nackMsg.setStartTick(startTick);
        nackMsg.setEndTick(endTick);

        if (upstream == null)
        {
            upstream = _originStreamMap.get(stream);
        }

        // Send this to MPIO
        // Send Nack messages at message priority +2
        _mpio.sendToMe(
                       upstream,
                       priority + 2,
                       nackMsg);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendNackMessage ");
    }

    /**
     * All of our matching local subscribers and/or output handlers have acked the
     * message so tell the upstream sender.
     * 
     * @param ackPrefix
     * @param msgSource
     * @param priority
     * @param reliability
     * @throws SIResourceException
     */
    @Override
    public void sendAckMessage(
                               SIBUuid8 upstream,
                               SIBUuid12 destUuid,
                               SIBUuid8 busUuid,
                               long ackPrefix,
                               int priority,
                               Reliability reliability,
                               SIBUuid12 stream,
                               boolean consolidate) throws SIResourceException, SIErrorException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendAckMessage", new Long(ackPrefix));

        if (upstream == null)
        {
            upstream = _originStreamMap.get(stream);
        }

        ControlAck ackMsg = createControlAckMessage(priority, reliability, stream);

        ackMsg.setAckPrefix(ackPrefix);

        if ((consolidate) &&
            (_internalInputStreamManager.hasStream(stream, priority, reliability)))
        {
            try
            {
                processAck(ackMsg);
            } catch (SIResourceException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.PubSubInputHandler.sendAckMessage",
                                            "1:600:1.329.1.1",
                                            this);
            }
        }
        else
        {
            // Send this to MPIO
            // Send Ack messages at the priority of the original message +1
            _mpio.sendToMe(
                           upstream,
                           priority + 1,
                           ackMsg);

        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendAckMessage");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControlHandler#handleControlMessage(com.ibm.ws.sib.trm.topology.Cellule, com.ibm.ws.sib.mfp.control.ControlMessage)
     * 
     * TODO maybe we should have separate upStream and downStream versions of this method
     */
    @Override
    public void handleControlMessage(SIBUuid8 sourceMEUuid, ControlMessage cMsg)
                    throws
                    SIIncorrectCallException,
                    SIErrorException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "handleControlMessage", new Object[] { cMsg });

        // Note that this method is called by the PubSubOutputHandler
        // and not by the RemoteMessageReceiver for Ack, Nack, RequestFlush
        // and AreYouFlushed messages

        // Work out type of ControlMessage and process it
        ControlMessageType type = cMsg.getControlMessageType();

        //  IMPORTANT: do the flush related messages first so that we answer 
        // consistently with respect to any in progress flush.
        if (type == ControlMessageType.REQUESTFLUSH)
        {
            // TODO: this should invoke some higher-level interface indicating that
            // our target wants to be flushed.      
        }

        // Now we can process the rest of the control messages...

        // Someone downstream has completed an ack prefix
        // or can't handle a nack.
        else if (type == ControlMessageType.ACK)
        {
            processAck((ControlAck) cMsg);
        }
        else if (type == ControlMessageType.NACK)
        {
            processNack((ControlNack) cMsg);
        }
        else if (type == ControlMessageType.ACKEXPECTED)
        {
            processAckExpected((ControlAckExpected) cMsg);
        }
        else
        {
            //call the super class to handle downstream (target) control messages
            super.handleControlMessage(sourceMEUuid, cMsg);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "handleControlMessage");
    }

    @Override
    public long handleControlMessageWithReturnValue(SIBUuid8 sourceMEUuid, ControlMessage cMsg)
                    throws
                    SIIncorrectCallException,
                    SIErrorException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "handleControlMessageWithReturnValue", new Object[] { cMsg });

        // no need to trace the return value as this is just pass-by function.
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "handleControlMessageWithReturnValue");

        return processNackWithReturnValue((ControlNack) cMsg);

    }

    /**
     * Process an AckExpected message.
     * 
     * @param ackExpMsg The AckExpected message
     */
    private void processAckExpected(ControlAckExpected ackExpMsg)
                    throws SIResourceException
    {

        // This is called by a PubSubOutputHandler when it finds Unknown
        // ticks in it's own stream and need the InputStream to resend them
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "processAckExpected", ackExpMsg);

        // The upstream cellule for this stream is stored in the originStreamMap
        // unless we were the originator.
        SIBUuid12 streamID = ackExpMsg.getGuaranteedStreamUUID();
        int priority = ackExpMsg.getPriority().intValue();
        Reliability reliability = ackExpMsg.getReliability();

        if (_internalInputStreamManager.hasStream(streamID, priority, reliability))
        {
            _internalInputStreamManager.processAckExpected(ackExpMsg);
        }
        else
        {
            _targetStreamManager.handleAckExpectedMessage(ackExpMsg);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processAckExpected");

    }

    /**
     * Process a nack from a PubSubOutputHandler.
     * 
     * @param nack The nack message from one of our PubSubOutputHandlers.
     */
    private long processNackWithReturnValue(ControlNack nackMsg)
                    throws SIResourceException
    {
        // This is called by a PubSubOutputHandler when it finds Unknown
        // ticks in it's own stream and need the InputStream to resend them
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "processNackWithReturnValue", nackMsg);

        long returnValue = -1;

        // The upstream cellule for this stream is stored in the originStreamMap
        // unless we were the originator.
        SIBUuid12 stream = nackMsg.getGuaranteedStreamUUID();

        if (_sourceStreamManager.hasStream(stream))
        {
            // 242139: Currently we get back to here for every processNack
            // that the InternalOutputStream handles. This is an error but
            // a harmles one. For now the fix is to avoid throwing an exception 
            // as the InternalOutputstream has in fact satisfied the Nack correctly.  
            // The longer term fix is to writeCombined range into the IOS not just the
            // value

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Ignoring processNack on sourceStream at PubSubInputHandler");

            returnValue = _sourceStreamManager.getStreamSet().getStream(nackMsg.getPriority(), nackMsg.getReliability()).getCompletedPrefix();
        }
        else
        {
            // Else we are an IME
            //  send nacks if necessary
            _internalInputStreamManager.processNack(nackMsg);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processNackWithReturnValue", new Long(returnValue));

        return returnValue;

    }

    private void processNack(ControlNack nackMsg)
                    throws SIResourceException
    {

        // This is called by a PubSubOutputHandler when it finds Unknown
        // ticks in it's own stream and need the InputStream to resend them
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "processNack", nackMsg);

        // The upstream cellule for this stream is stored in the originStreamMap
        // unless we were the originator.
        SIBUuid12 stream = nackMsg.getGuaranteedStreamUUID();

        if (_sourceStreamManager.hasStream(stream))
        {
            // 242139: Currently we get back to here for every processNack
            // that the InternalOutputStream handles. This is an error but
            // a harmles one. For now the fix is to avoid throwing an exception 
            // as the InternalOutputstream has in fact satisfied the Nack correctly.  
            // The longer term fix is to writeCombined range into the IOS not just the
            // value

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Ignoring processNack on sourceStream at PubSubInputHandler");

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "processNack");

            // Throw exception as at SME the InternalOutputStreams should
            // always have all the information that is required to satisfy
            // Nacks and should never call back to the InputHandler

//       SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
//          new Object[] {
//            "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
//            PROBE_ID_160 });
//
//       throw new SIResourceException(
//          nls.getFormattedMessage(
//            "INTERNAL_MESSAGING_ERROR_CWSIP0001",
//            new Object[] {
//              "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
//              PROBE_ID_160 },
//            null));

        }
        else
        {
            // Else we are an IME
            //  send nacks if necessary
            _internalInputStreamManager.processNack(nackMsg);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "processNack");

    }

    /**
     * This method processes Acks from PubSubOutputHandlers.
     * It uses the stream ID to determine the correct InputStream
     * to aggregate the Ack into. This will be a sourceStream if this
     * is the publishing ME, or an internalInputStream if not.
     * If the aggregated Ack causes the InputStream AckPrefix to increase
     * then one of the following will happen:-
     * For a sourceStream the proxyreferences for all newly Acked messages
     * will be removed from the refItemStream
     * For an internalInputStream the Ack will be sent upstream to the source ME
     * 
     * @param ack The control message which one of our OutputHandlers received
     *            which requires procesing.
     * 
     *            The messageAddCall boolean needs to be set after a right before a call to
     *            the batch handler messagesAdded so that in the result of an exception being
     *            thrown, the handler is unlocked.
     */
    private void processAck(ControlAck ack)
                    throws SIRollbackException, SIConnectionLostException, SIResourceException, SIErrorException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "processAck");

        SIBUuid12 stream = ack.getGuaranteedStreamUUID();

        // This code iterates through all of the OutputHandlers
        // associated with this InputHandler, checking their ack prefixes
        // The ack prefix of the InputStream is updated if the minimum of the
        // ack prefixes of the OutputStreams is greater than the current
        // InputStream ack prefix.
        List indexList = null;
        long index = 0;
        MessageItemReference msgItemRef = null;

        // This is the maximum tick in a stream
        long min = Long.MAX_VALUE;

        final Iterator outputHandlers =
                        _destination.getAllPubSubOutputHandlers().values().iterator();

        PubSubOutputHandler outputHandler = null;

        // TODO: In IME case we have to check the Ack on the TargetStream too
        // if we have one
        // But watch out for race where this has not been created yet because
        // writing to TargetStream happens after sending msg to remoteMEs
        // so it's just possible we get Acks back before we've done it.
        while (outputHandlers.hasNext())
        {
            outputHandler = (PubSubOutputHandler) outputHandlers.next();

            min = outputHandler.checkAck(ack, min);
        } // end while

        // Determine whether we are an IME 
        if (!_sourceStreamManager.hasStream(stream))
        {
            min = _targetStreamManager.checkAck(ack, min);
        }

        try
        {
            // We need to ensure that we read the ackPrefix, check it 
            // against the outputStream ackPrefixes and update it in one
            // operation  
            synchronized (this)
            {
                // First determine whether this Ack is for the sourceStream
                // or one of the internalInputStreams
                if (_sourceStreamManager.hasStream(stream))
                {
                    // Update the SourceStream ackPrefix
                    indexList = _sourceStreamManager.processAck(ack, min);
                }
                else // internal input stream
                {
                    // Update the SourceStream ackPrefix
                    indexList = _internalInputStreamManager.processAck(ack, min);
                }
            } // End sync

            // If this is the sourceME and we updated the ackPrefix
            // then this list will contain messages to be deleted
            // Otherwise it will always be null 
            if (indexList != null)
            {
                TransactionCommon tran = _sourceBatchHandler.registerInBatch();
                // A marker to indicate how far through the method we get. 
                boolean messageAddCall = false;

                try
                {
                    SourceStream batchListener = _sourceStreamManager.getBatchListener(ack);
                    TickRange tr = null;
                    for (int i = 0; i < indexList.size(); i++)
                    {
                        tr = (TickRange) indexList.get(i);
                        batchListener.addToBatchList(tr);

                        index = tr.itemStreamIndex;
                        try
                        {
                            msgItemRef =
                                            (MessageItemReference) _proxyReferenceStream
                                                            .findById(
                                                            index);
                            //it is possible that this is null in the case of an expired msg writing
                            //silence into the stream
                            if (msgItemRef != null)
                            {
                                Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(tran);
                                msgItemRef.remove(msTran, msgItemRef.getLockID());
                            }
                        } catch (MessageStoreException e)
                        {
                            // MessageStoreException shouldn't occur so FFDC.
                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.PubSubInputHandler.processAck",
                                                        "1:951:1.329.1.1",
                                                        this);

                            SibTr.exception(tc, e);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(tc, "processAck", e);

                            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                        new Object[] {
                                                      "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
                                                      "1:962:1.329.1.1",
                                                      e });

                            throw new SIResourceException(
                                            nls.getFormattedMessage(
                                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                                    new Object[] {
                                                                                  "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
                                                                                  "1:970:1.329.1.1",
                                                                                  e },
                                                                    null),
                                            e);
                        }
                    }
                    // Indicate that the messagesAdded call has been made
                    messageAddCall = true;
                    _sourceBatchHandler.messagesAdded(indexList.size(), batchListener);
                } finally
                {
                    if (!messageAddCall)
                        try
                        {
                            _sourceBatchHandler.messagesAdded(0);
                        } catch (SIResourceException e)
                        {
                            // No FFDC code needed, This will allow for any exceptions that were thrown to
                            // be rethrown instead of overiding with a batch handler error.
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                                SibTr.exception(tc, e);
                        }
                }
            } // End of deleting messages at source

        } finally
        {
            // unlock as the getAllPubSubOutputHandlers locks it.
            _destination.unlockPubsubOutputHandlers();

            // Before we exit, let the source stream manager take care of any pending flushes
            _sourceStreamManager.attemptFlushIfNecessary();

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "processAck");
        }
    }

    @Override
    public void handleMessage(MessageItem msg,
                              TransactionCommon transaction,
                              SIBUuid8 sourceMEUuid)
                    throws SIConnectionLostException, SIRollbackException, SINotPossibleInCurrentConfigurationException,
                    SIIncorrectCallException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "handleMessage", new Object[] { msg,
                                                           transaction,
                                                           sourceMEUuid });

        // If the message came in with a routing destination in it, pass it on
        JsDestinationAddress routingAddr = msg.getMessage().getRoutingDestination();

        // This mesasge has arrived from somewhere else (not directly from an
        // app) so if it has an FRP it MUST have come from the message.
        boolean msgFRP = !(msg.getMessage().isForwardRoutingPathEmpty());

        internalHandleMessage(msg, transaction, sourceMEUuid, routingAddr, null, msgFRP);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "handleMessage");
    }

    @Override
    public void handleProducerMessage(MessageItem msg,
                                      TransactionCommon transaction,
                                      JsDestinationAddress inAddress,
                                      MessageProducer sender,
                                      boolean msgFRP)
                    throws SIConnectionLostException, SIRollbackException, SINotPossibleInCurrentConfigurationException,
                    SIIncorrectCallException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "handleProducerMessage", new Object[] { msg,
                                                                   transaction,
                                                                   inAddress,
                                                                   sender,
                                                                   msgFRP });

        internalHandleMessage(msg, transaction, _messageProcessor.getMessagingEngineUuid(), inAddress, sender, msgFRP);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "handleProducerMessage");
    }

    /**
     * Processes the message to either a local or remote destination
     * 
     * @param msg The message to put
     * @param transaction The transaction object
     * @param sourceCellule The source for the message
     * 
     */
    private void internalHandleMessage(MessageItem msg,
                                       TransactionCommon transaction,
                                       SIBUuid8 sourceMEUuid,
                                       JsDestinationAddress inAddress,
                                       MessageProducer sender,
                                       boolean msgFRP)
                    throws SIConnectionLostException, SIRollbackException, SINotPossibleInCurrentConfigurationException,
                    SIIncorrectCallException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "internalHandleMessage", new Object[] { msg,
                                                                   transaction,
                                                                   sourceMEUuid,
                                                                   inAddress,
                                                                   sender,
                                                                   msgFRP });

        if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
            traceSend(msg);

        super.handleMessage(msg);

        // Remember if the original user transaction was a real one or not
        msg.setTransacted(!transaction.isAutoCommit());

        //Inbound pub/sub messages are discarded once the destination is marked as 
        //deleted.
        if (!(_destination.isToBeDeleted()))
        {
            //For debug purposes, tell the message which destinations its on
            if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
            {
                msg.setDebugName(_destination.getName());
            }

            if (sourceMEUuid.equals(_localMEUuid))
            {
                localPut(msg, transaction);
            }
            else
            {
                // Note: important to pass the originating cellule here so we
                // can update the origin map.
                try
                {
                    remotePut(msg, sourceMEUuid);
                } catch (SIResourceException e)
                {
                    // No FFDC code needed

                    SIMPResourceException ee = new SIMPResourceException(e);
                    ee.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
                    ee.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.PubSubInputHandler.handleMessage",
                                                         "1:1134:1.329.1.1",
                                                         SIMPUtils.getStackTrace(e)
                    });
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "internalHandleMessage", ee);
                    throw ee;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "internalHandleMessage");

        return;
    }

    /**
     * The local put method is driven when the producer is attached locally
     * attached to this PubSub Input handler
     */
    private void localPut(
                          MessageItem msg,
                          TransactionCommon transaction)
                    throws
                    SIIncorrectCallException,
                    SIResourceException,
                    SINotPossibleInCurrentConfigurationException,
                    SINotAuthorizedException,
                    SILimitExceededException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "localPut",
                        new Object[] { msg, transaction });

        boolean stored = false;

        /*
         * If a non-empty frp exists at this point then send the message to the exception
         * destination
         */
        if (!msg.getMessage().isForwardRoutingPathEmpty())
        {
            // If we have a non-empty forward routing path then throw an exception. 
            // A topicspace can only be the final element in the path.

            SIMPIncorrectCallException e = new SIMPIncorrectCallException(
                            nls.getFormattedMessage(
                                                    "FORWARD_ROUTING_PATH_ERROR_CWSIP0249",
                                                    new Object[] { _destination.getName(),
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));

            e.setExceptionReason(SIRCConstants.SIRC0037_INVALID_ROUTING_PATH_ERROR);
            e.setExceptionInserts(new String[] { _destination.getName(),
                                                _messageProcessor.getMessagingEngineName(),
                                                "unknown",
                                                SIMPUtils.getStackTrace(e) });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "localPut", e);
            }

            throw e;

        }

        // If the message came in from a remote bus then
        // check whether the userId is authorised to access this destination
        if (msg.isFromRemoteBus())
        {
            // Check whether bus security is enabled 
            if (_messageProcessor.isBusSecure())
            {
                JsMessage jsMsg = msg.getMessage();

                // Before we test for anything else, see whether this message 
                // was sent by the privileged Jetstream SIBServerSubject. If it was
                // then we bypass the security checks
                if (!_messageProcessor.getAuthorisationUtils().sentBySIBServer(jsMsg))
                {
                    // Check authority to produce to destination
                    String userid = null;
                    if (_destination.isLink() && !_destination.isMQLink())
                    {
                        userid = ((LinkHandler) _destination).getInboundUserid();
                    }

                    // If the InboundUserid is null or we're not working with a link
                    // Set the userid from the message
                    if (userid == null)
                    {
                        // Use the id extracted from the message for access checks
                        userid = jsMsg.getSecurityUserid();
                    }

                    // Defect 240261: Map a null userid to an empty string
                    if (userid == null)
                    {
                        userid = ""; // Empty string means those users that have not
                                     // been authenticated, but who are still, of course,
                                     // members of EVERYONE. 
                    }

                    String discriminator = msg.getMessage().getDiscriminator();

                    // Create secContext from userid and discriminator
                    SecurityContext secContext = new SecurityContext(userid, discriminator);

                    if (!_destination.checkDestinationAccess(secContext,
                                                             OperationType.SEND))
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "localPut", "not authorized to produce to this destination");

                        // Build the message for the Exception and the Notification
                        String nlsMessage =
                                        nls_cwsik.getFormattedMessage("DELIVERY_ERROR_SIRC_18", // USER_NOT_AUTH_SEND_ERROR_CWSIP0306
                                                                      new Object[] { _destination.getName(),
                                                                                    userid },
                                                                      null);

                        // Fire a Notification if Eventing is enabled
                        _messageProcessor.
                                        getAccessChecker().
                                        fireDestinationAccessNotAuthorizedEvent(_destination.getName(),
                                                                                userid,
                                                                                OperationType.SEND,
                                                                                nlsMessage);

                        // Thrown if user denied access to destination
                        SIMPNotAuthorizedException e = new SIMPNotAuthorizedException(nlsMessage);

                        e.setExceptionReason(SIRCConstants.SIRC0018_USER_NOT_AUTH_SEND_ERROR);
                        e.setExceptionInserts(new String[] { _destination.getName(),
                                                            userid });
                        throw e;
                    }

                    if (!_destination.
                                    checkDiscriminatorAccess(secContext,
                                                             OperationType.SEND))
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "localPut", "not authorized to produce to this destination's discriminator");

                        // Write an audit record if access is denied
                        SibTr.audit(tc, nls_cwsik.getFormattedMessage(
                                                                      "DELIVERY_ERROR_SIRC_20", // USER_NOT_AUTH_SEND_ERROR_CWSIP0308
                                                                      new Object[] { _destination.getName(),
                                                                                    secContext.getDiscriminator(),
                                                                                    userid },
                                                                      null));

                        // Thrown if user denied access to destination 
                        SIMPNotAuthorizedException e = new SIMPNotAuthorizedException(
                                        nls_cwsik.getFormattedMessage(
                                                                      "DELIVERY_ERROR_SIRC_20", // USER_NOT_AUTH_SEND_ERROR_CWSIP0308
                                                                      new Object[] { _destination.getName(),
                                                                                    secContext.getDiscriminator(),
                                                                                    userid },
                                                                      null));

                        e.setExceptionReason(SIRCConstants.SIRC0020_USER_NOT_AUTH_SEND_ERROR);
                        e.setExceptionInserts(new String[] { _destination.getName(),
                                                            secContext.getDiscriminator(),
                                                            userid });
                        throw e;
                    }
                }
            }
        }

        boolean forcePut = msg.isForcePut();
        // Check SendAllowed for local case.
        //send allowed can be false ONLY if this msg is not force put
        boolean isSendAllowed = forcePut ||
                                (_itemStream.isSendAllowed() && _destination.isSendAllowed());

        if (!isSendAllowed)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(
                           tc,
                           "localPut",
                           "Destination send disallowed");

            SIMPNotPossibleInCurrentConfigurationException e = new SIMPNotPossibleInCurrentConfigurationException(
                            nls.getFormattedMessage("DESTINATION_SEND_DISALLOWED_CWSIP0253",
                                                    new Object[] { _destination.getName(),
                                                                  _messageProcessor.getMessagingEngineName() }, null));

            e.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
            e.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.PubSubInputHandler.localPut",
                                                "1:1334:1.329.1.1",
                                                SIMPUtils.getStackTrace(e) });
            throw e;
        }

        // check total items on itemstream not exceeded (allow extra 1 for ref stream)
        //but only if forcePut==false
        long topicSpaceHighLimit = _destination.getPublishPoint().getDestHighMsgs();
        if (!forcePut &&
            ((topicSpaceHighLimit != -1)
            && (_itemStream.getTotalMsgCount() >= topicSpaceHighLimit)
            ))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(
                           tc,
                           "localPut",
                           "Destination reached high limit");

            SIMPLimitExceededException e = new SIMPLimitExceededException(
                            nls.getFormattedMessage("DESTINATION_HIGH_MESSAGES_ERROR_CWSIP0251",
                                                    new Object[] { _destination.getName(),
                                                                  new Long(topicSpaceHighLimit),
                                                                  _messageProcessor.getMessagingEngineName() },
                                                    null));
            e.setExceptionReason(SIRCConstants.SIRC0025_DESTINATION_HIGH_MESSAGES_ERROR);
            e.setExceptionInserts(new String[] { _destination.getName(),
                                                new Long(topicSpaceHighLimit).toString() });
            throw e;
        }

        if (msg.isTransacted())
        {
            // Dont store the msg till pre-prepare when we know if there are any
            // subscribers.  For now, just register for the pre-prepare callback on
            // the transaction.  //183715.1
            registerMessage(msg, transaction); //183715.1      
        }
        else
        {
            LocalTransaction siTran = _txManager.createLocalTransaction(false);

            // If COD reports are required, register for the precommit callback
            if (msg.getReportCOD() != null && _destination instanceof BaseDestinationHandler)
                msg.registerMessageEventListener(MessageEvents.COD_CALLBACK, (BaseDestinationHandler) _destination);

            try
            {
                // Perform the PubSub match.
                stored = localFanOut(msg, siTran, false);

                if (stored)
                    siTran.commit();
                else
                    siTran.rollback();
            } catch (RuntimeException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.PubSubInputHandler.localPut",
                                            "1:1397:1.329.1.1",
                                            this);

                SIMPErrorException ee = new SIMPErrorException(e);

                ee.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
                ee.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.ProducerSessionImpl.handleMessage",
                                                     "1:1404:1.329.1.1",
                                                     SIMPUtils.getStackTrace(e) });
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                {
                    SibTr.exception(tc, ee);
                    SibTr.exit(tc, "localPut", ee);
                }

                throw ee;

            } catch (SIRollbackException e)
            {
                // No FFDC code needed
                handleRollback(siTran);

                SIMPRollbackException ee = new SIMPRollbackException(e.getMessage());
                ee.setStackTrace(e.getStackTrace());
                ee.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
                ee.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.ProducerSessionImpl.handleMessage",
                                                     "1:1424:1.329.1.1",
                                                     SIMPUtils.getStackTrace(e) });

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "localPut", ee);

                throw ee;
            } catch (SIConnectionLostException e)
            {
                // No FFDC code needed
                handleRollback(siTran);

                SIMPConnectionLostException ee = new SIMPConnectionLostException(e.getMessage());
                ee.setStackTrace(e.getStackTrace());
                ee.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
                ee.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.ProducerSessionImpl.handleMessage",
                                                     "1:1441:1.329.1.1",
                                                     SIMPUtils.getStackTrace(e) });

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "localPut", ee);

                throw ee;
            } catch (SIIncorrectCallException e)
            {
                // No FFDC code needed
                handleRollback(siTran);

                SIMPIncorrectCallException ee = new SIMPIncorrectCallException(e.getMessage());
                ee.setStackTrace(e.getStackTrace());
                ee.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
                ee.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.ProducerSessionImpl.handleMessage",
                                                     "1:1458:1.329.1.1",
                                                     SIMPUtils.getStackTrace(e) });
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "localPut", ee);
                throw ee;
            } catch (SIResourceException e)
            {
                // No FFDC code needed
                handleRollback(siTran);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "localPut", e);

                SIMPResourceException ee = new SIMPResourceException(e);
                ee.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
                ee.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.ProducerSessionImpl.handleMessage",
                                                     "1:1475:1.329.1.1",
                                                     SIMPUtils.getStackTrace(e) });
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "localPut", ee);
                throw ee;
            }

            // Release the JsMessage from the parent MessageItem (Any MessageItemReferences will
            // have their own references to the JsMessage (unless they've released their's too)
            msg.releaseJsMessage();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "localPut");
    }

    /**
     * The remote put method is driven when the producer is remote
     * to this PubSub Input handler.
     * 
     * @param msg The inbound data message.
     * @param sourceCellule The cellule from which the message was received. We need
     *            this to update the originMap for the inbound stream.
     */
    private void remotePut(
                           MessageItem msg,
                           SIBUuid8 sourceMEUuid)
                    throws
                    SIResourceException,
                    SIDiscriminatorSyntaxException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "remotePut",
                        new Object[] { msg, sourceMEUuid });

        // Update the origin map if this is a new stream.
        // This unsynchronized update is ok until multiple paths
        // are possible (in which case there could be a race
        // for the same stream on this method).
        SIBUuid12 stream = msg.getMessage().getGuaranteedStreamUUID();
        if (!_originStreamMap.containsKey(stream))
            _originStreamMap.put(stream, sourceMEUuid);

        // First get matches
        // Match Consumers is only called for PubSub targets.
        MessageProcessorSearchResults searchResults = matchMessage(msg);

        String topic = msg.getMessage().getDiscriminator();

        //First the remote to remote case (forwarding)
        HashMap allPubSubOutputHandlers = _destination.getAllPubSubOutputHandlers();
        List matchingPubsubOutputHandlers = searchResults.getPubSubOutputHandlers(topic);

        // Check to see if we have any Neighbours
        if (allPubSubOutputHandlers != null &&
            allPubSubOutputHandlers.size() > 0)
        {
            remoteToRemotePut(msg, allPubSubOutputHandlers, matchingPubsubOutputHandlers);
        }

        // Calling getAllPubSubOutputHandlers() locks the handlers
        // so now we have finished we need to unlock
        _destination.unlockPubsubOutputHandlers();

        // Now handle remote to local case

        // If there are any ConsumerDispatchers we will need to save the
        // list in the Targetstream with the message
        Set consumerDispatchers = searchResults.getConsumerDispatchers(topic);

        // Check to see if we have any matching Neighbours
        if (consumerDispatchers != null &&
            consumerDispatchers.size() > 0)
        {
            // Add the list of consumerDispatchers to the MessageItem
            // as we will need it in DeliverOrdered messages
            msg.setSearchResults(searchResults);

            // This will add the message to the TargetStream
            remoteToLocalPut(msg);

        }
        else
        {
            // This will add Silence to the TargetStream
            remoteToLocalPutSilence(msg);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "remotePut");
    }

    /**
     * 
     * @param msg
     * @param matchingPubsubOutputHandlers
     * @throws GDException
     * @throws SIResourceException
     */
    private void remoteToRemotePut(MessageItem msg,
                                   HashMap allPubSubOutputHandlers,
                                   List matchingPubsubOutputHandlers)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "remoteToRemotePut",
                        new Object[] { msg, matchingPubsubOutputHandlers });

        // True if there are any matching outputHandlers    
        boolean checkMatches = false;
        // Array of Cellule pairs for MPIO
        ArrayList<SIBUuid8> routes = null;

        Iterator itr = allPubSubOutputHandlers.values().iterator();

        //defect 260440: we do not want to set the bus field in the message until
        //after it is stored. The reason is that we require the message to be stored
        //with its original bus value so we can still determine, on restoring,
        //whether the message should be sent to various links or not.  
        //msg.getMessage().setBus(messageProcessor.getMessagingEngineBus());

        JsMessage jsMessage = null;

        if (matchingPubsubOutputHandlers != null &&
            matchingPubsubOutputHandlers.size() > 0)
        {
            routes = new ArrayList<SIBUuid8>(matchingPubsubOutputHandlers.size());
            checkMatches = true;
        }

        while (itr.hasNext())
        {
            PubSubOutputHandler handler = (PubSubOutputHandler) itr.next();
            if (checkMatches && matchingPubsubOutputHandlers.contains(handler))
            {
                if (handler.okToForward(msg))
                {
                    //there is at least one put, so now is the time to
                    //set the bus in a copy of the message

                    // Create the internalInputStream here, once we know
                    // we have a match - see defect 282761
                    // This does not save the message in the stream but creates
                    // a stream if one does not yet exist    
                    _internalInputStreamManager.processMessage(msg.getMessage());

                    if (jsMessage == null)
                    {
                        try
                        {
                            //defect 260440
                            //We are now sending the message. This means we will have to modify
                            //the msg's 'bus' field. 
                            //However, we must ensure that a stored message
                            //has the bus field set to its originating bus so,
                            //unless this is a msg that will be stored and rebuilt
                            //at restart, we can avoid making a copy
                            boolean persistedAfterRestart =
                                            (msg.getReliability().compareTo(Reliability.RELIABLE_PERSISTENT)) > 0;
                            if (persistedAfterRestart)
                            {
                                jsMessage = msg.getMessage().getReceived();
                                jsMessage.setBus(_messageProcessor.getMessagingEngineBus());
                                //but now we need a new message item
                                MessageItem msg2 = new MessageItem(jsMessage);

                                // Carry over the original MessageItem's settings
                                msg2.setFromRemoteME(msg.isFromRemoteME());
                                msg2.setCurrentMEArrivalTimestamp(msg.getCurrentMEArrivalTimestamp());
                                msg = msg2;
                            }
                            else
                            {
                                //there is no danger of using the same message
                                jsMessage = msg.getMessage();
                            }

                        } catch (Exception e)
                        {
                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.PubSubInputHandler.remoteToRemotePut",
                                                        "1:1660:1.329.1.1",
                                                        this);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            {
                                SibTr.exit(tc, "remoteToRemotePut", e);
                            }
                            throw new SIResourceException(e);
                        }
                    }
                    // Put the message to the Neighbour.
                    // No transaction required as no ItemStream
                    handler.put(msg, null, this, true);

                    //add a target cellule to the array for sending
                    //or send the message directly from the outputHandler
                    //if this is a Link 
                    if (handler.isLink())
                    {
                        handler.sendLinkMessage(msg, false);
                    }
                    else
                    {
                        // add a target cellule to the array for sending
                        SIBUuid8 targetMEUuid = handler.getTargetMEUuid();
                        routes.add(targetMEUuid);
                    }
                } //end ok to forward
                  //else
                  //{ 
                  // defect 260440: 
                  // 601 used to now remove this OH from 
                  // the set of matching outputhandlers, but it is cheaper
                  // instead to merely repeat the 'okToForward' check later on      
                  //}

            }
            else
            {
                // for those output handlers which do not match
                // Put Completed into the stream.  Set "create" to
                // true in case this is the first time we've sent something
                // to a particular OutputHandler.
                //
                // See defect 282761:
                // We should only send a silence if the PSOH is topologically
                // visible
                if (handler.okToForward(msg))
                {
                    handler.putSilence(msg);
                }
            }
        }

        if ((routes != null) && (routes.size() != 0))
        {

            SIBUuid8[] fromTo = new SIBUuid8[routes.size()];
            fromTo = routes.toArray(fromTo);

            //call MPIO to finally send the message to the matching remote MEs
            _mpio.sendDownTree(fromTo, //the list of source target pairs
                               msg.getPriority(), //priority
                               msg.getMessage()); //the JsMessage
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "remoteToRemotePut");
    }

    /**
     * This is a put message that has oringinated from another ME
     * When there are no matching local consumers we need to write
     * Silence into the stream instead
     * 
     * @param msgItem The message to be put.
     * @throws SIResourceException
     */
    protected void remoteToLocalPutSilence(MessageItem msgItem) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "remoteToLocalPutSilence",
                        new Object[] { msgItem });

        // Write Silence to the targetStream instead
        // If the targetStream does not exist this will just return     
        _targetStreamManager.handleSilence(msgItem);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "remoteToLocalPutSilence");
    }

    /**
     * Returns a list of matching OutputHandlers for a particular message.
     * Note that this method takes a MessageProcessorSearchResults object
     * from a pool. This object must be returned by the caller when it is
     * finished with.
     * 
     * @param msg
     * @return
     */
    private MessageProcessorSearchResults matchMessage(MessageItem msg)
                    throws SIDiscriminatorSyntaxException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "matchMessage",
                        new Object[] { msg });

        //Extract the message from the MessageItem
        JsMessage jsMsg = msg.getMessage();

        //  Match Consumers is only called for PubSub targets.
        TopicAuthorization topicAuth = _messageProcessor.getDiscriminatorAccessChecker();
        MessageProcessorSearchResults searchResults = new MessageProcessorSearchResults(topicAuth);

        // Defect 382250, set the unlockCount from MsgStore into the message
        // in the case where the message is being redelivered.
        int redelCount = msg.guessRedeliveredCount();

        if (redelCount > 0)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Set deliverycount into message: " + redelCount);
            jsMsg.setDeliveryCount(redelCount);
        }

        // Get the matching consumers from the matchspace
        _matchspace.retrieveMatchingOutputHandlers(
                                                   _destination,
                                                   jsMsg.getDiscriminator(),
                                                   (MatchSpaceKey) jsMsg,
                                                   searchResults);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(
                       tc,
                       "matchMessage",
                       new Object[] { searchResults });

        return searchResults;
    }

    /**
     * Fan out the given message to subscribers.
     * 
     * Perform match asks the match space for the matching set of output handlers
     * based on the topic of the message.
     * 
     * It will send messages to both the ConsumerDispatchers and the PubSubOutput
     * handlers for delivery to remote ME's
     * 
     * @param msg The message object
     * @param tran The transaction
     * @param stored If the message has been stored to an item/reference stream
     * @return boolean if the message has been stored to an item/reference stream
     */
    private boolean localFanOut(
                                MessageItem msg,
                                TransactionCommon tran,
                                boolean stored)
                    throws
                    SIDiscriminatorSyntaxException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "localFanOut",
                        new Object[] { msg, tran, new Boolean(stored) });

        boolean notBestEffort = (msg.getReliability().compareTo(Reliability.BEST_EFFORT_NONPERSISTENT)) > 0;
        boolean isTransacted = msg.isTransacted();

        //Find matching output handlers    
        MessageProcessorSearchResults searchResults = matchMessage(msg);

        String topic = msg.getMessage().getDiscriminator();

        //168373.1
        JsMessage jsMsg = msg.getMessage();

        // cycle through the Neighbouring ME's
        List matchingPubsubOutputHandlers = searchResults.getPubSubOutputHandlers(topic);

        MessageItemReference ref = null;

        //Although there may be matching neighbours, some of them
        //may be upstream, and so shouldn't count as matches.
        //Therefore we can only tell if we actually have to initialize msg fields
        //once we start enumerating the output handlers.
        boolean sourceStreamUpdated = false;
        boolean proxyRefStreamUpdated = false;

        // Check to see if we have any matching Neighbours
        if (matchingPubsubOutputHandlers != null &&
            matchingPubsubOutputHandlers.size() > 0)
        {
            ArrayList<SIBUuid8> fromTo = null; //see defect 285784: 
            //we should not use array as we might end up with null elements (if there
            //are upstream MEs). The NPE that results causes localPut to bomb-out 
            //before msg is delivered to local ConsumerDispatchers. 

            // If we've just registered for the post_commit callback (see above comment)
            // we have nothing else to do here. Otherwise, traverse the list of output handlers. 
            if (notBestEffort || !isTransacted)
            {
                HashMap allPubSubOutputHandlers = _destination.getAllPubSubOutputHandlers();
                try
                {
                    Iterator itr = allPubSubOutputHandlers.values().iterator();

                    int i = 0;
                    while (itr.hasNext())
                    {
                        PubSubOutputHandler handler = (PubSubOutputHandler) itr.next();

                        //see if this is a topological match 
                        if (handler.okToForward(msg))
                        {
                            //It is only now that we are certain that we have to send the message
                            //to a PSOH - either a msg or a silence. 
                            //Therefore we should now set some message properties
                            //(if we have not already done so)
                            if (!sourceStreamUpdated)
                            {
                                //set the necessary msg properties EXCEPT the 'bus' field,
                                //which should not be set before the message is stored.
                                //The reason is that if the msg is to be stored then we need
                                //the 'bus' field to be set to the originating bus.
                                //Stored & best_effort transacted messages have their 
                                //'bus' field updated after the add
                                setPropertiesInMessage(jsMsg,
                                                       _destination.getUuid(),
                                                       msg.getProducerConnectionUuid());

                                // This allocates the tick and adds to stream atomically
                                _sourceStreamManager.addMessage(msg);

                                sourceStreamUpdated = true;
                            }
                            //see if this is a criteria match
                            if (matchingPubsubOutputHandlers.contains(handler))
                            {
                                if (!proxyRefStreamUpdated)
                                {
                                    // If this message has a stream (i.e. the message isn't best_effort)
                                    // we create a reference to the message.
                                    // We will then be driven by the commit of this to send the messages
                                    // to the neighbours.
                                    if (notBestEffort)
                                    {
                                        if (!stored)
                                            storeMessage(msg, tran);
                                        stored = true;
                                        ref = addProxyReference(msg, searchResults, tran);
                                    }
                                    // If we are transacted and best_effort we must wait for the transaction to
                                    // be committed before we send the message so we register for an event
                                    // on the transaction before we send the message.
                                    else if (isTransacted)
                                    {
                                        msg.setSearchResults(searchResults);
                                        msg.registerMessageEventListener(MessageEvents.POST_COMMITTED_TRANSACTION, this);
                                    }
                                    // Otherwise, we're non-transacted best_effort and we must send the
                                    // message direct from here, so we build a list of targets from the
                                    // search results.
                                    else
                                    {
                                        fromTo = new ArrayList<SIBUuid8>(matchingPubsubOutputHandlers.size()); //We use the 
                                        //match list size as an initial capacity as it is probably 
                                        //quite a good guess.
                                    }
                                    proxyRefStreamUpdated = true;
                                }

                                // Put the message to the Neighbour.
                                stored = handler.put(msg, tran, this, stored);

                                if (!notBestEffort)
                                {
                                    //i.e. we are best effort non transacted
                                    //Update the bus field - defect 260440: 
                                    //we should set the 'bus' in the jsMsg here because
                                    //the message is not going to be stored. 
                                    jsMsg.setBus(_messageProcessor.getMessagingEngineBus());

                                    //add a target cellule to the array for sending
                                    //or send the message directly from the outputHandler
                                    //if this is a Link 
                                    if (handler.isLink())
                                    {
                                        // If the output handler is a link, only forward it if it isn't to
                                        // the bus it may have just come from.
                                        handler.sendLinkMessage(msg, false);
                                    }
                                    else
                                    {
                                        // Add this handler to th target list
                                        SIBUuid8 targetCellule = handler.getTargetMEUuid();
                                        fromTo.add(targetCellule); //see defect 285784
                                        i++;

                                        // trace the fact we're about to transmit the message
                                        if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
                                            UserTrace.traceOutboundSend(jsMsg,
                                                                        handler.getTargetMEUuid(),
                                                                        _destination.getName(),
                                                                        _destination.isForeignBus() || _destination.isLink(),
                                                                        _destination.isMQLink(),
                                                                        _destination.isTemporary());
                                    }
                                }
                            }
                            else if (notBestEffort)
                            {
                                // There is no criteria match, so we put completed into the stream.
                                // Set "create" to true in case this is the first time
                                // this stream has seen a tick.
                                handler.putSilence(msg);
                            }
                        }//end topological match
                         // defect 260440: 
                         // 601 used to now remove this OH from 
                         // the set of matching outputhandlers, but it is cheaper
                         // instead to merely repeat the 'okToForward' check later on
                         //  else
                         //  {
                         //    if(matchingPubsubOutputHandlers.contains(handler))
                         //    {
                         //      matchingPubsubOutputHandlers.remove(handler);
                         //    }              
                         //  }
                    }//end while

                    // If we've built a list of targets we must want to send them
                    // directly from here (we're non-transacted best effort)
                    if (i > 0)
                    {
                        //Now that we have a list of fromTo pairs we can obtain an array representation 
                        //see defect 285784
                        SIBUuid8[] fromToArray =
                                        fromTo.toArray(new SIBUuid8[fromTo.size()]);
                        _mpio.sendDownTree(fromToArray, //the list of source target pairs
                                           msg.getPriority(), //priority
                                           jsMsg); //the JsMessage
                    }
                } finally
                {
                    // unlock as the getAllPubSubOutputHandlers locks it.
                    _destination.unlockPubsubOutputHandlers();
                }
            }
            else
            {
                //we are best effort and transacted
                //We need to scan the loop of PSOHs and make sure at least one is okToForward
                Iterator itr = _destination.getAllPubSubOutputHandlers().values().iterator();
                try
                {
                    while (itr.hasNext())
                    {
                        PubSubOutputHandler handler = (PubSubOutputHandler) itr.next();
                        //see if this is a topological match 
                        if (handler.okToForward(msg) && matchingPubsubOutputHandlers.contains(handler))
                        {
                            msg.setSearchResults(searchResults);
                            msg.registerMessageEventListener(MessageEvents.POST_COMMITTED_TRANSACTION, this);
                            break;
                        }
                    }//end while
                } finally
                {
                    // unlock as the getAllPubSubOutputHandlers locks it.
                    _destination.unlockPubsubOutputHandlers();
                }
            }
        }
        else
        {
            //Set a unique id in the message if explicitly told to or
            //if one has not already been set
            if (msg.getRequiresNewId() || jsMsg.getSystemMessageId() == null)
            {
                jsMsg.setSystemMessageSourceUuid(_messageProcessor.getMessagingEngineUuid());
                jsMsg.setSystemMessageValue(_messageProcessor.nextTick());
                msg.setRequiresNewId(false);
            }
        }

        stored = deliverToConsumerDispatchers(searchResults, msg, tran, stored);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "localFanOut", new Boolean(stored));

        return stored;
    }

    /**
     * Restore fan out of the given message to subscribers.
     * 
     * Perform match asks the match space for the matching set of output handlers
     * based on the topic of the message.
     * 
     * It will send messages to the PubSubOutput handlers for delivery
     * to remote ME's
     * 
     * @param msg Reference to the message object
     * @param commitInsert If the message has been committed
     * @return boolean If the message reference is required
     */
    private boolean restoreFanOut(
                                  MessageItemReference ref,
                                  boolean commitInsert)
                    throws
                    SIDiscriminatorSyntaxException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "restoreFanOut",
                        new Object[] { ref, new Boolean(commitInsert) });

        boolean keepReference = true;
        MessageItem msg = null;
        try
        {
            msg = (MessageItem) ref.getReferredItem();
        } catch (MessageStoreException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.PubSubInputHandler.restoreFanOut",
                                        "1:2102:1.329.1.1",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "restoreFanOut", e);

            throw new SIResourceException(e);
        }

        //Find matching output handlers    
        MessageProcessorSearchResults searchResults = matchMessage(msg);

        String topic = msg.getMessage().getDiscriminator();

        // cycle through the Neighbouring ME's
        List matchingPubsubOutputHandlers = searchResults.getPubSubOutputHandlers(topic);

        // Check to see if we have any matching Neighbours
        if (matchingPubsubOutputHandlers != null &&
            matchingPubsubOutputHandlers.size() > 0)
        {
            HashMap allPubSubOutputHandlers = _destination.getAllPubSubOutputHandlers();
            try
            {
                Iterator itr = allPubSubOutputHandlers.values().iterator();

                while (itr.hasNext())
                {
                    PubSubOutputHandler handler = (PubSubOutputHandler) itr.next();
                    if (handler.okToForward(msg))
                    {
                        if (matchingPubsubOutputHandlers.contains(handler))
                        {
                            // Put the message to the Neighbour.
                            handler.putInsert(msg, commitInsert);
                        }
                        else
                        {
                            // Put Completed into stream
                            // Set "create" to true in case this is the first time
                            // this stream has seen a tick.
                            handler.putSilence(msg);
                        }
                    }
                    //else
                    //{
                    // defect 260440: 
                    // 601 used to now remove this OH from 
                    // the set of matching outputhandlers, but it is cheaper
                    // instead to merely repeat the 'okToForward' check later on
                    //}
                }
            } finally
            {
                // unlock as the getAllPubSubOutputHandlers locks it.
                _destination.unlockPubsubOutputHandlers();
            }
        }
        else
        // no matching Neighbours, mark itemReference to be removed from referenceStream
        {
            keepReference = false;
        }

        // If we have OutputHandlers to deliver this to, and ref is indoubt,
        // then we keep searchResults for use in the eventPostAdd() callback
        if (keepReference && !commitInsert)
        {
            ref.setSearchResults(searchResults);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "restoreFanOut");

        return keepReference;
    }

    /*
     * This method delivers a message to the ConsumerDispatchers
     * listed in searchResults
     * 
     * @param searchResults The list of matching output handlers
     * 
     * @param msg The message object
     * 
     * @param tran The transaction
     * 
     * @param stored If the message has been stored to an item/reference stream
     * 
     * @return boolean If the message has been stored to an item/reference stream
     */
    private boolean deliverToConsumerDispatchers(
                                                 MessageProcessorSearchResults searchResults,
                                                 MessageItem msg,
                                                 TransactionCommon tran,
                                                 boolean stored) throws SIDiscriminatorSyntaxException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "deliverToConsumerDispatchers",
                        new Object[] { searchResults, msg, tran, new Boolean(stored) });

        String topic = msg.getMessage().getDiscriminator();
        boolean hasInternalSubscription = false;

        Set consumerDispatchers = searchResults.getConsumerDispatchers(topic);

        if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
        {
            if (msg.getMessage().isApiMessage())
            {
                String apiMsgId = null;

                if (msg.getMessage() instanceof JsApiMessage)
                    apiMsgId = ((JsApiMessage) msg.getMessage()).getApiMessageId();
                else
                {
                    if (msg.getMessage().getApiMessageIdAsBytes() != null)
                    {
                        apiMsgId = msg.getMessage().getApiMessageIdAsBytes().toString();
                    }
                }

                SibTr.debug(UserTrace.tc_mt,
                            nls_mt.getFormattedMessage(
                                                       "PUBLICATION_COUNT_CWSJU0008",
                                                       new Object[] {
                                                                     apiMsgId,
                                                                     _destination.getName(),
                                                                     new Integer(consumerDispatchers.size()) },
                                                       null));
            }
        }

        if (consumerDispatchers != null)
        {
            //  As we have a tran CD put will only add the msg to its item stream.
            Iterator i = consumerDispatchers.iterator();
            while (i.hasNext())
            {
                ConsumerDispatcher consumerDispatcher = ((MatchingConsumerDispatcher) (i.next())).getConsumerDispatcher();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Found consumerDispatcher: " + consumerDispatcher.getConsumerDispatcherState());

                // Check that the consumer is ready to receive the message
                if (consumerDispatcher.getConsumerDispatcherState().isReady())
                {
                    stored = consumerDispatcher.put(msg, tran, this, stored);
                }

                // Are any consumer dispatchers an internal subscription
                hasInternalSubscription |= consumerDispatcher.
                                getConsumerDispatcherState().
                                getTargetDestination() != null;
            }

            // Set the number of local subscriptions this message will fan out to for
            // later consumption by the statistics instrumentation.
            msg.setFanOut(consumerDispatchers.size());
        }

        // update stored
        stored |= hasInternalSubscription;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deliverToConsumerDispatchers", new Boolean(stored));

        return stored;
    }

    /**
     * Add a msg reference to the proxy subscription reference stream.
     * 
     * @param msg
     * @param tran
     * @throws SIResourceException if there is a message store resource problem
     * @throws SIStoreException if there is a general problem with the message store.
     */
    private MessageItemReference addProxyReference(MessageItem msg,
                                                   MessageProcessorSearchResults matchingPubsubOutputHandlers,
                                                   TransactionCommon tran) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "addProxyReference", new Object[] { msg, tran });
        MessageItemReference ref = new MessageItemReference(msg);

        // This ensures that the persitence of the MessageItem cannot be downgaded
        // by the consumerDispatcher because we have remote subscribers.
        msg.addPersistentRef();

        ref.setSearchResults(matchingPubsubOutputHandlers);
        try
        {
            ref.registerMessageEventListener(MessageEvents.POST_COMMIT_ADD, this);
            ref.registerMessageEventListener(MessageEvents.POST_ROLLBACK_ADD, this);
            Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(tran);
            _proxyReferenceStream.add(ref, msTran);
        } catch (OutOfCacheSpace e)
        {
            // No FFDC code needed
            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "addProxyReference", "SIResourceException");
            throw new SIResourceException(e);
        } catch (MessageStoreException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.PubSubInputHandler.addProxyReference",
                                        "1:2315:1.329.1.1",
                                        this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
                                      "1:2322:1.329.1.1",
                                      e });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "addProxyReference", e);

            throw new SIResourceException(nls.getFormattedMessage(
                                                                  "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                                  new Object[] {
                                                                                "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
                                                                                "1:2332:1.329.1.1",
                                                                                e },
                                                                  null));
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addProxyReference");

        return ref;
    }

    void referenceCountZeroCallback(MessageItem msg) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "referenceCountZeroCallback", msg);

        LockingCursor getCursor = null;
        try
        {
            getCursor = _itemStream.newLockingItemCursor(null);
            msg.lockItemIfAvailable(getCursor.getLockID());
            msg.remove(_txManager.createAutoCommitTransaction(), msg.getLockID());
        } catch (MessageStoreException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.PubSubInputHandler.referenceCountZeroCallback",
                                        "1:2359:1.329.1.1",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "referenceCountZeroCallback", e);

            throw new SIResourceException(e);
        } finally
        {
            getCursor.finished();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "referenceCountZeroCallback");
    }

    /**
     * @param msg
     * @param durableSubId
     * @param transaction
     */
    void recordDurableSub(
                          MessageItem msg,
                          String durableSubId,
                          TransactionCommon transaction)
    {

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl#sendAckExpectedMessage(long, com.ibm.ws.sib.trm.topology.Cellule, int, com.ibm.ws.sib.common.Reliability)
     */
    @Override
    public void sendAckExpectedMessage(
                                       long ackExpStamp,
                                       int priority,
                                       Reliability reliability,
                                       SIBUuid12 stream)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendAckExpectedMessage",
                        new Object[] { new Long(ackExpStamp), new Integer(priority), reliability, stream });
        HashMap allPubSubOutputHandlers = _destination.getAllPubSubOutputHandlers();
        try
        {
            Iterator itr = allPubSubOutputHandlers.values().iterator();

            while (itr.hasNext())
            {
                PubSubOutputHandler handler = (PubSubOutputHandler) itr.next();

                // Send AckExpected to all OutputHandlers
                handler.processAckExpected(ackExpStamp, priority, reliability, stream);
            }
        } finally
        {
            // By calling the getAllPubSubOutputHandlers it will lock the 
            // handlers
            _destination.unlockPubsubOutputHandlers();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendAckExpectedMessage");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl#sendSilenceMessage(long, long, long, com.ibm.ws.sib.trm.topology.Cellule, int,
     * com.ibm.ws.sib.common.Reliability)
     */
    @Override
    public void sendSilenceMessage(
                                   long startStamp,
                                   long endStamp,
                                   long completedPrefix,
                                   boolean requestedOnly,
                                   int priority,
                                   Reliability reliability,
                                   SIBUuid12 stream)
    {
        // NOP, the PubSubInputHandler doesn't originate
        // silence.  The associated output handlers do it
        // for us.
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
                        new Object[] { new Long(tick),
                                      new Long(completedPrefix),
                                      new Integer(priority),
                                      reliability,
                                      stream });

        ControlSilence sMsg = null;
        try
        {
            // Create new Silence message
            sMsg = _cmf.createNewControlSilence();
        } catch (Exception e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.PubSubInputHandler.createSilenceMessage",
                                        "1:2471:1.329.1.1",
                                        this);
            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
                                      "1:2477:1.329.1.1",
                                      e });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createSilenceMessage", e);

            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
                                                                  "1:2487:1.329.1.1",
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
                                                  _destination.getUuid(),
                                                  ProtocolType.PUBSUBOUTPUT,
                                                  GDConfig.PROTOCOL_VERSION);

        sMsg.setStartTick(tick);
        sMsg.setEndTick(tick);
        sMsg.setPriority(priority);
        sMsg.setReliability(reliability);
        sMsg.setCompletedPrefix(completedPrefix);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createSilenceMessage", sMsg);
        return sMsg;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl#sendValueMessages(java.util.ArrayList, long, com.ibm.ws.sib.trm.topology.Cellule)
     */
    @Override
    public List sendValueMessages(
                                  List msgList,
                                  long completedPrefix,
                                  boolean requestedOnly,
                                  int priority,
                                  Reliability reliability,
                                  SIBUuid12 stream)
    {
        // NOP, PubSubOutputHandlers are responsible for pushing value
        // messages downstream.
        return null;
    }

    @Override
    public MessageItem getValueMessage(long msgStoreID)
                    throws SIResourceException
    {
        // NOP, PubSubOutputHandlers are responsible for pushing value
        // messages downstream.
        return null;
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
        } catch (MessageCreateFailedException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.PubSubInputHandler.createControlAckMessage",
                                        "1:2567:1.329.1.1",
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "createControlAckMessage", e);
            }

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
                                      "1:2579:1.329.1.1",
                                      e });

            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
                                                                  "1:2587:1.329.1.1",
                                                                  e },
                                                    null),
                            e);
        }

        // As we are using the Guaranteed Header - set all the attributes as 
        // well as the ones we want
        SIMPUtils.setGuaranteedDeliveryProperties(ackMsg,
                                                  _messageProcessor.getMessagingEngineUuid(),
                                                  null,
                                                  stream,
                                                  null,
                                                  _destination.getUuid(),
                                                  ProtocolType.PUBSUBOUTPUT,
                                                  GDConfig.PROTOCOL_VERSION);

        ackMsg.setPriority(priority);
        ackMsg.setReliability(reliability);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createControlAckMessage");

        return ackMsg;
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
            SibTr.entry(tc, "createControlNackMessage",
                        new Object[] { new Integer(priority), reliability, stream });

        ControlNack nackMsg = null;

        // Create new AckMessage and send it
        try
        {
            nackMsg = _cmf.createNewControlNack();
        } catch (MessageCreateFailedException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.PubSubInputHandler.createControlNackMessage",
                                        "1:2643:1.329.1.1",
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "createControlNackMessage", e);
            }

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
                                      "1:2655:1.329.1.1",
                                      e });

            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
                                                                  "1:2663:1.329.1.1",
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
                                                  _destination.getUuid(),
                                                  ProtocolType.PUBSUBOUTPUT,
                                                  GDConfig.PROTOCOL_VERSION);

        nackMsg.setPriority(priority);
        nackMsg.setReliability(reliability);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createControlNackMessage");

        return nackMsg;
    }

    /**
     * This method is only called by the TargetStream when
     * is has filled a gap in the stream
     * msgList is a list of MessageItems to deliver
     * WARNING: This method must never be called while the caller
     * holds the GuaranteedTargetStream lock
     * 
     * The messageAddCall boolean needs to be set after a right before a call to
     * the batch handler messagesAdded so that in the result of an exception being
     * thrown, the handler is unlocked.
     */
    @Override
    public void deliverOrderedMessages(List msgList,
                                       GuaranteedTargetStream targetStream,
                                       int priority,
                                       Reliability reliability)

                    throws SIDiscriminatorSyntaxException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deliverOrderedMessages",
                        new Object[] { msgList,
                                      targetStream,
                                      new Integer(priority),
                                      reliability });

        //Register interest in the current batch
        TransactionCommon tran = _targetBatchHandler.registerInBatch();

        // A marker to indicate how far through the method we get. 
        boolean messageAddCall = false;

        try
        {
            MessageItem msgItem = null;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Get the tick from list of length: " + msgList.size());

            // Get the end tick of the last message in the list    
            msgItem = (MessageItem) msgList.get(msgList.size() - 1);
            long endTick = msgItem.getMessage().getGuaranteedValueEndTick();

            // This check is done in case the previous batch rolledback 
            // while we were waiting for the batch lock.
            // If this happened then we give up trying to deliver these
            // messages as the previous batch must be redelivered first
            // ( The rollback resets the doubtHorizon to before the 
            // failing batch )
            if (targetStream.getDoubtHorizon() > endTick)
            {
                int lastSuccessfulMsg = -1;
                long lastSuccessfulEndTick = -1;
                long currentEndTick = -1;
                final int length = msgList.size();

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Process message list of length: " + length);

                // Deliver all messages in list
                for (int i = 0; i < length; i++)
                {
                    boolean resetRequired = true;
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Deliver message: " + i);
                    try
                    {
                        msgItem = (MessageItem) msgList.get(i);
                        currentEndTick = msgItem.getMessage().getGuaranteedValueEndTick();

                        msgItem.setTransacted(true);

                        //get the matching results back from the message
                        MessageProcessorSearchResults searchResults = msgItem.getSearchResults();

                        deliverToConsumerDispatchers(searchResults, msgItem, tran, false);

                        // Remember the last message we successfully delivered (if we break out
                        // of this loop early due to a failure we won't accidently move the
                        // completed prefix past the failed message when the batch is committed)
                        lastSuccessfulMsg = i;
                        lastSuccessfulEndTick = currentEndTick;

                        // Remove reset and pooling of search results (we should put this back in at
                        // some point)

                        resetRequired = false;

                        // Release the JsMessage from the parent MessageItem (Any MessageItemReferences will
                        // have their own references to the JsMessage (unless they've released their's too)
                        msgItem.releaseJsMessage();
                    } catch (SIResourceException e)
                    {
                        // No FFDC code needed

                        // There is no need to let SIResourceExceptions go any further than this
                        // method, instead we break out of the for loop to allow any successful
                        // work to be added to the batch. The finally will move the doubtHorizon
                        // back to the correct position.
                        // (The finally will catch any other exception and move the doubt horizon
                        // back but still throw the exception back to the caller)

                        // then break out of the for loop to allow any successful work to be added
                        // to the batch.
                        break;
                    } finally
                    {
                        if (resetRequired)
                        {
                            // We've failed to process a message, we can't carry on with this
                            // list of messages instead we move the doubt horizon to point to
                            // the failed message so that we don't accidently ack it if someone
                            // asks. 
                            targetStream.resetDoubtHorizon(currentEndTick);
                        }
                    }
                } // end of for loop

                if (lastSuccessfulMsg != -1)
                {
                    // We are about to call the messages Added !
                    messageAddCall = true;

                    // Now that we have the Batch lock we are free to get the 
                    // stream lock and update the pending completedPrefix to the
                    // last successful message delivered
                    targetStream.setNextCompletedPrefix(lastSuccessfulEndTick);

                    // Add any messages to the batch.
                    // If this fails the batch will be rolled back and the targetStream
                    // rollback listener will be called which will reset the doubtHorizon
                    // to the start of this list of messages, so we don't want an 
                    // SIResourceException to be flowed up the stack.
                    try
                    {
                        _targetBatchHandler.messagesAdded((lastSuccessfulMsg + 1), targetStream);
                    } catch (SIResourceException e)
                    {
                        // No FFDC code needed

                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            SibTr.exception(tc, e);
                    }
                }
            }
        } finally
        {
            // Before exiting this method, need to unlock the batch handler if it was locked.
            if (!messageAddCall)
                try
                {
                    _targetBatchHandler.messagesAdded(0);
                } catch (SIResourceException e)
                {
                    // No FFDC code needed, This will allow for any exceptions that were thrown to
                    // be rethrown instead of overiding with a batch handler error.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        SibTr.exception(tc, e);
                }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "deliverOrderedMessages", new Boolean(messageAddCall));
        }
    }

    // This method is only called by the TargetStream when
    // is has filled a gap in the stream
    // msgList is a list of MessageItems
    @Override
    public void deliverExpressMessage(MessageItem msgItem,
                                      ExpressTargetStream expressTargetStream) throws SIResourceException, SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deliverExpressMessage", new Object[] { msgItem });

        //Create transaction
        LocalTransaction siTran = _txManager.createLocalTransaction(false);

        try
        {
            // msg is currently the last message in the list
            // get it's tick value for use in sending Acks and updating stream
            long endTick = msgItem.getMessage().getGuaranteedValueValueTick();

            msgItem.setTransacted(true);

            //get the matching results back from the message
            MessageProcessorSearchResults searchResults = msgItem.getSearchResults();

            // Store it
            boolean stored = deliverToConsumerDispatchers(searchResults, msgItem, siTran, false);

            // Update the completedPrefix on the stream
            // This will put all ticks before this point into Completed state
            expressTargetStream.setCompletedPrefix(endTick);

            if (stored)
                siTran.commit();
            else
                siTran.rollback();

        } catch (SIResourceException e)
        {
            // No FFDC code needed
            handleRollback(siTran);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "deliverExpressMessage", e);

            throw e;
        } catch (RuntimeException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.PubSubInputHandler.deliverExpressMessage",
                                        "1:2908:1.329.1.1",
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "deliverExpressMessage", e);
            }

            throw e;
        } catch (SIIncorrectCallException e)
        {
            // No FFDC code needed
            handleRollback(siTran);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "deliverExpressMessage", e);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deliverExpressMessage");
    }

    @Override
    public BrowseCursor getBrowseCursor(SelectionCriteria criteria) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getBrowseCursor", criteria);

        NonLockingCursor msgStoreCur = null;
        MessageSelectorFilter filter = null;
        BrowseCursor cursor = null;
        try
        {
            //if there is a selection criteria then we have to
            //create a MessageStore filter
            if (criteria != null &&
                ((criteria.getSelectorString() != null && !criteria.getSelectorString().equals("")) ||
                (criteria.getDiscriminator() != null && !criteria.getDiscriminator().equals(""))))
            {
                filter = new MessageSelectorFilter(_messageProcessor, criteria);
            }

            msgStoreCur = _itemStream.newNonLockingItemCursor(filter);
            cursor = new JSBrowseCursor(msgStoreCur);
        } catch (Exception e)
        {
            // Exception shouldn't occur so FFDC.
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.PubSubInputHandler.getBrowseCursor",
                                        "1:2962:1.329.1.1",
                                        this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
                                      "1:2969:1.329.1.1",
                                      e });
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "getBrowseCursor", "SIResourceException");
            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
                                                                  "1:2977:1.329.1.1",
                                                                  e },
                                                    null),
                            e);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getBrowseCursor", cursor);
        return cursor;
    }

    @Override
    public void storeMessage(MessageItem msg, TransactionCommon tran) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "storeMessage", new Object[] { msg, tran });

        try
        {
            // If the transaction is null, then this means it should be an
            // Auto commit transaction
            if (tran == null)
                tran = _txManager.createAutoCommitTransaction();

            _destination.registerForMessageEvents(msg);

            Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(tran);
            _itemStream.addItem(msg, msTran);
        } catch (OutOfCacheSpace e)
        {
            // No FFDC code needed
            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "storeMessage", "SIResourceException");
            throw new SIResourceException(e);
        } catch (MessageStoreException e)
        {
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.PubSubInputHandler.storeMessage",
                                        "1:3018:1.329.1.1",
                                        this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] { "com.ibm.ws.sib.processor.impl.PubSubInputHandler", "1:3023:1.329.1.1", e });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "storeMessage", e);

            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] { "com.ibm.ws.sib.processor.impl.PubSubInputHandler", "1:3031:1.329.1.1", e },
                                                    null),
                            e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "storeMessage");
    }

    @Override
    public ItemStream getItemStream()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getItemStream");
            SibTr.exit(tc, "getItemStream", _itemStream);
        }

        return _itemStream;
    }

    /**
     * Sets properties in the message that are common to both
     * links and non-links.
     * 
     * @param jsMsg
     * @param destinationUuid
     * @param producerConnectionUuid
     * @author tpm
     */
    public void setPropertiesInMessage(JsMessage jsMsg,
                                       SIBUuid12 destinationUuid,
                                       SIBUuid12 producerConnectionUuid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setPropertiesInMessage",
                        new Object[] { jsMsg, destinationUuid, producerConnectionUuid });

        SIMPUtils.setGuaranteedDeliveryProperties(jsMsg,
                                                  _messageProcessor.getMessagingEngineUuid(),
                                                  null,
                                                  null,
                                                  null,
                                                  destinationUuid,
                                                  ProtocolType.PUBSUBINPUT,
                                                  GDConfig.PROTOCOL_VERSION);

        if (jsMsg.getConnectionUuid() == null)
        {
            //defect 278038:
            //The producerSession will have set this in the msgItem for pub sub 
            //destinations. However, the field may not have been set in the
            //jsMsg if the msg was not persisted before this point.
            //Since the message is going off the box, we have no choice but to set it now.
            jsMsg.setConnectionUuid(producerConnectionUuid);
        }
        //NOTE: the 'bus' field is not set in this method as it should
        //only be set once the message has been stored
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setPropertiesInMessage");
    }

    /**
     * Restores the GD source streams
     * 
     */
    public void reconstitutePubSubSourceStreams(StreamSet streamSet, int startMode)
                    throws
                    SIErrorException, SIDiscriminatorSyntaxException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "reconstitutePubSubSourceStreams", new Object[] { streamSet, new Integer(startMode) });

        _sourceStreamManager.reconstituteStreamSet(streamSet);

        // Don't do flush if we are asked to start in recovery mode 
        if (((startMode & JsConstants.ME_START_FLUSH) == JsConstants.ME_START_FLUSH)
            && ((startMode & JsConstants.ME_START_RECOVERY) == 0))
        {
            this.sendFlushedMessage(null, streamSet.getStreamID());
            // Now change streamID of streamSet
            streamSet.setStreamID(new SIBUuid12());
            // This calls requestUpdate on the StreamSet Item which will
            // cause a callback to the streamSet.getPersistentData() by msgstore
            Transaction tran = _txManager.createAutoCommitTransaction();
            try
            {
                streamSet.requestUpdate(tran);
            } catch (MessageStoreException e)
            {
                // MessageStoreException shouldn't occur so FFDC.
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.PubSubInputHandler.reconstitutePubSubSourceStreams",
                                            "1:3145:1.329.1.1",
                                            this);

                SibTr.exception(tc, e);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "reconstitutePubSubSourceStreams", "SIStoreException");
                throw new SIResourceException(e);
            }
        }

        try
        {
            NonLockingCursor cursor = _proxyReferenceStream.newNonLockingCursor(
                            new ClassEqualsFilter(MessageItemReference.class));
            cursor.allowUnavailableItems();
            MessageItemReference ref = (MessageItemReference) cursor.next();

            // Rebuild internalOutputStreams by running refs through matchspace 
            LocalTransaction siTran = null;
            Transaction msTran = null;
            int batchCount = 0;
            while (ref != null)
            {
                // If the count of reconstituted messages has reached 50, commit the transaction
                if (batchCount >= 50) {
                    try
                    {
                        siTran.commit();
                        // Null out the transaction object so that a new one is created next
                        siTran = null;
                    } catch (SIIncorrectCallException e)
                    {
                        // No FFDC code needed
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "reconstitutePubSubSourceStreams", "SIResourceException");
                        throw new SIResourceException(e);
                    }
                }

                // If the transaction object is null then we've either never created it or have just committed it.
                // Create a new transaction 
                if (siTran == null) {
                    siTran = _txManager.createLocalTransaction(true);
                    msTran = (Transaction) siTran;
                    batchCount = 0;
                }

                boolean commitInsert = false;

                // Change streamID in message to streamID of StreamSet
                // If we are restoring from a stale backup this will
                // ensure that the restoreFanOut method puts it on a new InteralOutputStream
                if (ref.getGuaranteedStreamUuid() != streamSet.getStreamID())
                {
                    ref.setGuaranteedStreamUuid(streamSet.getStreamID());
                }

                if (!(ref.isAdding() || ref.isRemoving()))
                    commitInsert = true;
                // If there are no matching neighbours, remove reference
                if (!restoreFanOut(ref, commitInsert))
                {
                    if (!(ref.isAdding() || ref.isRemoving()))
                    {
                        ref.remove(msTran, ref.getLockID());
                        batchCount++;
                    }
                    else
                    // TODO: ref is indoubt, need to mark it for later deletion on commit
                    {
                    }
                }
                ref = (MessageItemReference) cursor.next();
            }
            try
            {
                if (siTran != null) {
                    siTran.commit();
                }
            } catch (SIIncorrectCallException e)
            {
                // No FFDC code needed
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "reconstitutePubSubSourceStreams", "SIResourceException");
                throw new SIResourceException(e);
            }

            // Ditch the cursor & create again to get back to head of list
            cursor.finished();
            cursor = _proxyReferenceStream.newNonLockingCursor(
                            new ClassEqualsFilter(MessageItemReference.class));
            cursor.allowUnavailableItems();
            ref = (MessageItemReference) cursor.next();

            // Add all messages back in to the SourceStreams
            while (ref != null)
            {
                // Change streamID in message to streamID of StreamSet
                // If we are restoring from a stale backup this will
                // and the restoreMessage method puts it on a new SourceStream
                if (ref.getGuaranteedStreamUuid() != streamSet.getStreamID())
                {
                    ref.setGuaranteedStreamUuid(streamSet.getStreamID());
                }

                //add all messages back in to the streams
                if (!(ref.isAdding() || ref.isRemoving()))
                {
                    // commit those which are not in doubt
                    _sourceStreamManager.restoreMessage(ref, true);
                }
                else
                {
                    // add to stream in uncommitted state
                    _sourceStreamManager.restoreMessage(ref, false);
                }

                ref = (MessageItemReference) cursor.next();
            }
            // Consolidate all streams which may have been reconstituted
            _sourceStreamManager.consolidateStreams(startMode);

            cursor.finished();

        } catch (MessageStoreException e)
        {
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.PubSubInputHandler.reconstitutePubSubSourceStreams",
                                        "1:3277:1.329.1.1",
                                        this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
                                      "1:3284:1.329.1.1",
                                      e });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "reconstitutePubSubSourceStreams", e);

            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
                                                                  "1:3295:1.329.1.1",
                                                                  e },
                                                    null),
                            e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconstitutePubSubSourceStreams");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl#sendFlushedMessage(com.ibm.ws.sib.utils.SIBUuid12)
     * 
     * This is only called from attemptFlush() as flushQuery's are processed
     * by the PubSubOuputHandler
     */
    @Override
    public void sendFlushedMessage(SIBUuid8 ignore, SIBUuid12 streamID) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendFlushedMessage", new Object[] { streamID });

        // This flush should be broadcast to all downstream neighbors 
        // for this cell as it is the result of a startFlush()

        // This is a bit of a kludge since we may be sending flushes to cells
        // which have no information about a stream.
        HashMap allPubSubOutputHandlers = _destination.getAllPubSubOutputHandlers();
        try
        {
            Iterator itr = allPubSubOutputHandlers.values().iterator();

            while (itr.hasNext())
            {
                // Get the appropriate target cellule and forward the message
                PubSubOutputHandler handler = (PubSubOutputHandler) itr.next();

                // Note that the null Cellule we pass in here is ignored
                // as each OutputHandler knows its targetCellule 
                handler.sendFlushedMessage(null, streamID);

                // Also, tell the handler to remove any information for this
                // stream since it's just been flushed.
                handler.removeStream(streamID);
            }
        } finally
        {
            // By calling the getAllPubSubOutputHandlers it will lock the 
            // handlers
            _destination.unlockPubsubOutputHandlers();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendFlushedMessage");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DownstreamControl#sendNotFlushedMessage(com.ibm.ws.sib.utils.SIBUuid12, long)
     * 
     * Not called as flushQuery is processed by PubSubOuptutHandler
     */
    @Override
    public void sendNotFlushedMessage(SIBUuid8 target, SIBUuid12 streamID, long requestID)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendNotFlushedMessage",
                        new Object[] { target, streamID, new Long(requestID) });

        InvalidOperationException e =
                        new InvalidOperationException(nls.getFormattedMessage(
                                                                              "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                                              new Object[] {
                                                                                            "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
                                                                                            "1:3366:1.329.1.1" },
                                                                              null));

        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                    new Object[] {
                                  "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
                                  "1:3372:1.329.1.1" });

        // FFDC
        FFDCFilter.processException(
                                    e,
                                    "com.ibm.ws.sib.processor.impl.PubSubInputHandler.sendNotFlushedMessage",
                                    "1:3378:1.329.1.1",
                                    this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendNotFlushedMessage");
    }

    @Override
    public void sendAreYouFlushedMessage(
                                         SIBUuid8 upstream,
                                         SIBUuid12 destUuid,
                                         SIBUuid8 busUuid,
                                         long queryID,
                                         SIBUuid12 streamID)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendAreYouFlushedMessage",
                        new Object[] { upstream, new Long(queryID), streamID });

        ControlAreYouFlushed flushQuery = createControlAreYouFlushed(upstream, queryID, streamID);
        _mpio.sendToMe(upstream,
                       SIMPConstants.MSG_HIGH_PRIORITY,
                       flushQuery);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendAreYouFlushedMessage");
    }

    @Override
    public void sendRequestFlushMessage(
                                        SIBUuid8 upstream,
                                        SIBUuid12 destUuid,
                                        SIBUuid8 busUuid,
                                        long queryID,
                                        SIBUuid12 streamID,
                                        boolean indoubtDiscard)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendRequestFlushMessage",
                        new Object[] { upstream, new Long(queryID), streamID });
        ControlRequestFlush flushRequest = createControlRequestFlush(upstream, queryID, streamID);
        flushRequest.setIndoubtDiscard(indoubtDiscard);
        _mpio.sendToMe(upstream,
                       SIMPConstants.MSG_HIGH_PRIORITY,
                       flushRequest);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendRequestFlushMessage");
    }

    /**
     * Wait for the current stream to quiesce, remove it
     * from storage, then create a replacement stream ID.
     * We assume that all production has already been
     * stopped and that no new production will occur until
     * after the flush has been completed. The reference
     * passed to this method contains the callback for
     * signalling when the flush has completed.
     * 
     * @param complete An instance of the FlushComplete interface
     *            which we'll invoke when the flush of the current stream
     *            has completed.
     * @throws FlushAlreadyInProgressException if someone calls
     *             this method but a flush is already in progress.
     */
    public void startFlush(FlushComplete complete)
                    throws SIRollbackException, SIConnectionLostException, SIResourceException, SIErrorException, FlushAlreadyInProgressException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "startFlush", complete);

        _sourceStreamManager.startFlush(complete);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "startFlush");
    }

    private void traceSend(MessageItem message)
    {
        if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
        {
            if (message.getMessage().isApiMessage())
            {
                String text = "PRODUCER_SEND_TOPICSPACE_CWSJU0005";
                if (_destination.isTemporary())
                    text = "PRODUCER_SEND_TEMPORARY_TOPICSPACE_CWSJU0102";

                String apiMsgId = null;
                String correlationId = null;

                if (message.getMessage() instanceof JsApiMessage)
                {
                    apiMsgId = ((JsApiMessage) message.getMessage()).getApiMessageId();
                    correlationId = ((JsApiMessage) message.getMessage()).getCorrelationId();
                }
                else
                {
                    if (message.getMessage().getApiMessageIdAsBytes() != null)
                        apiMsgId = new String(message.getMessage().getApiMessageIdAsBytes());

                    if (message.getMessage().getCorrelationIdAsBytes() != null)
                        correlationId = new String(message.getMessage().getCorrelationIdAsBytes());
                }

                SibTr.debug(UserTrace.tc_mt,
                            nls_mt.getFormattedMessage(
                                                       text,
                                                       new Object[] {
                                                                     apiMsgId,
                                                                     correlationId,
                                                                     _destination.getName() },
                                                       null));
            }
        }
    }

    /**
     * Called when the transaction is committed (not the add of a message). This only
     * occurs if the message was best_effort and transacted.
     * 
     * @param msg The message which has been committed
     * @throws SIStoreException Thrown if there is ANY problem
     * @see com.ibm.ws.sib.store.AbstractItem#eventCommittedAdd(com.ibm.ws.sib.msgstore.Transaction)
     */
    private void eventPostCommit(SIMPMessage msg) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "eventPostCommit", msg);

        MessageItem msgItem = (MessageItem) msg;
        JsMessage jsMsg = msgItem.getMessage();

        // If we need to send the message out we will create copies,
        // as we cannot initialize the message post-commit - consumer threads
        // might already be taking copies of the message to delivery to consumers.
        MessageItem msgItemCopy = null;
        JsMessage jsMsgCopy = null;

        //get the list of matching output handlers for this message
        MessageProcessorSearchResults searchResults = msgItem.getSearchResults();

        //Although there may be matching neighbours, some of them
        //may be upstream, and so shouldn't count as matches.
        //Therefore we can only tell if we actually have to initialize msg fields
        //once we start enumerating the output handlers.
        boolean sourceStreamUpdated = false;

        //don't give a monkeys what the topic was, just want the results back!
        List matchingPubsubOutputHandlers = searchResults.getPubSubOutputHandlers(null);

        ArrayList<SIBUuid8> fromTo = new ArrayList<SIBUuid8>(matchingPubsubOutputHandlers.size()); //see defect 285784: 
        //we should not use array as we might end up with null elements (if there
        //are upstream MEs). The NPE that results causes localPut to bomb-out 
        //before msg is delivered to local ConsumerDispatchers. 
        //We use the match list size as an initial capacity as it is 
        //probably quite a good guess.

        Iterator itr = matchingPubsubOutputHandlers.iterator();
        int i = 0;
        while (itr.hasNext())
        {
            //get the next output handler
            PubSubOutputHandler outputHandler = (PubSubOutputHandler) itr.next();

            if (outputHandler.okToForward(msgItem))
            {
                if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
                    UserTrace.traceOutboundSend(jsMsg,
                                                outputHandler.getTargetMEUuid(),
                                                _destination.getName(),
                                                _destination.isForeignBus() || _destination.isLink(),
                                                _destination.isMQLink(),
                                                _destination.isTemporary());

                if (!sourceStreamUpdated)
                {
                    // Create a copy of the message
                    try
                    {
                        jsMsgCopy = jsMsg.getReceived();
                        msgItemCopy = new MessageItem(jsMsgCopy);
                    } catch (MessageCopyFailedException e)
                    {
                        // FFDC
                        FFDCFilter.processException(
                                                    e,
                                                    "com.ibm.ws.sib.processor.impl.PubSubInputHandler.eventPostCommit",
                                                    "1:3562:1.329.1.1",
                                                    this);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        {
                            SibTr.exception(tc, e);
                            SibTr.exit(tc, "eventPostCommit", e);
                        }

                        SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                    new Object[] {
                                                  "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
                                                  "1:3574:1.329.1.1",
                                                  e });

                        throw new SIResourceException(
                                        nls.getFormattedMessage(
                                                                "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                                new Object[] {
                                                                              "com.ibm.ws.sib.processor.impl.PubSubInputHandler",
                                                                              "1:3582:1.329.1.1",
                                                                              e },
                                                                null),
                                        e);
                    }

                    //set the necessary msg properties 
                    setPropertiesInMessage(jsMsgCopy,
                                           _destination.getUuid(),
                                           msgItemCopy.getProducerConnectionUuid());
                    //and the 'bus' field
                    jsMsgCopy.setBus(_messageProcessor.getMessagingEngineBus());

                    // This allocates the tick and adds to stream atomically
                    _sourceStreamManager.addMessage(msgItemCopy);

                    sourceStreamUpdated = true;
                }
                // Unfortunately each individual InternalOutputStream StreamSet must be initialised.
                // Otherwise the areYouFlushed query sent from each target will fail to realise the
                // 'stream' (not that there really is one in this case) is not flushed. In which case
                // a flushed message will be sent to the target and the message ignored.
                // The cheapest (from a code point of view, not performance) and safest mechanism to
                // ensure the StreamSet is there is to drive PSOH.put() which drives
                // InternalOutputStreamManager.addMessage() which lazily instantiates the StreamSet.
                // We could improve performance later by reducing the amount of work performed in
                // the IOSM.addMessage() method for best_effort messages (e.g. just call
                // IOSM.getStreamSet()).
                outputHandler.put(msgItemCopy, null, null, false);

                //add a target cellule to the array for sending
                //or send the message directly from the outputHandler
                //if this is a Link 
                if (outputHandler.isLink())
                {
                    outputHandler.sendLinkMessage(msgItemCopy, false);
                }
                else
                {
                    SIBUuid8 targetMEUuid = outputHandler.getTargetMEUuid();
                    fromTo.add(targetMEUuid); //see defect 285784
                    i++;
                }
            }//end ok to forward

        }

        // call MPIO to finally send the message to the matching remote MEs
        if (i > 0)
        {
            //Now that we have a list of fromTo pairs we can obtain an array representation 
            //see defect 285784
            SIBUuid8[] fromToArray =
                            fromTo.toArray(new SIBUuid8[fromTo.size()]);

            _mpio.sendDownTree(fromToArray, //the list of source target pairs
                               msgItemCopy.getPriority(), //priority
                               jsMsgCopy); //the copy the JsMessage we created
        }

        //just to be safe, nullify the reference's reference to the results object...
        msgItem.setSearchResults(null);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventPostCommit");
    }

    /**
     * Ensure all source streams are flushed (and therefore in-doubt
     * messages are resolved). This is done in preparation for
     * deleting the destination. If the delete must be deferred,
     * this code will automatically redrive delete when possible.
     * 
     * @return true The source was successfully flushed and
     *         deletion may continue, otherwise the delete must be deferred
     *         until the source can flush.
     */
    public boolean flushAllForDeleteSource()
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "flushAllForDeleteSource");

        synchronized (this)
        {
            // Flush may have completed, if so then return
            // without starting another one.
            if (_flushedForDeleteSource)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "flushAllForDeleteSource", Boolean.TRUE);
                return true;
            }

            // Short circuit if flush already in progress
            if (_deleteFlushSource != null)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "flushAllForDeleteSource", Boolean.FALSE);
                return false;
            }

            // Otherwise, we need to start a new flush
            final PubSubInputHandler psIH = this;
            _deleteFlushSource = new FlushComplete() {
                @Override
                public void flushComplete(DestinationHandler destinationHandler)
                {
                    // Remember that the flush completed for when we redrive
                    // the delete code.
                    synchronized (psIH) {
                        psIH._flushedForDeleteSource = true;
                        psIH._deleteFlushSource = null;
                    }

                    // PubSubOutputHandlers now safe to cleanup
                    ((BaseDestinationHandler) psIH._destination).deleteAllPubSubOutputHandlers();

                    // Now redrive the actual deletion
                    psIH._messageProcessor.getDestinationManager().startAsynchDeletion();
                }
            };
        }

        // Start the flush and return false
        try
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.event(tc, "Started source flush for destination: " + _destination.getName());
            startFlush(_deleteFlushSource);
        } catch (FlushAlreadyInProgressException e)
        {
            // This shouldn't actually be possible so log it
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.PubSubInputHandler.flushAllForDeleteSource",
                                        "1:3718:1.329.1.1",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "flushAllForDeleteSource", "FlushAlreadyInProgressException");

            throw new SIResourceException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "flushAllForDeleteSource", Boolean.FALSE);
        return false;
    }

    /**
     * Ensure all target streams are flushed (and therefore in-doubt
     * messages are resolved). This is done in preparation for
     * deleting the destination. If the delete must be deferred,
     * this code will automatically redrive delete when possible.
     * 
     * @return true if the target streams have been successfully flushed
     *         and deletion can continue, otherwise the delete must be
     *         deferred until the streams are successfully flushed.
     */
    public boolean flushAllForDeleteTarget()
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "flushAllForDeleteTarget");

        synchronized (this)
        {
            // Flush may have completed, if so then return
            // without starting another one.
            if (_flushedForDeleteTarget)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "flushAllForDeleteTarget", Boolean.TRUE);
                return true;
            }

            // Short circuit if flush already in progress
            if (_deleteFlushTarget != null)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "flushAllForDeleteTarget", Boolean.FALSE);
                return false;
            }

            // If we're flushable now then return, otherwise send the query
            // and start an alarm
            if (_targetStreamManager.isEmpty())
            {
                _flushedForDeleteTarget = true;
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "flushAllForDeleteTarget", Boolean.TRUE);
                return true;
            }

            // Otherwise, send out the initial query, and set a retry alarm 
            _deleteFlushTarget = new AlarmListener() {
                @Override
                public void alarm(Object al)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.entry(tc, "alarm", al);
                    PubSubInputHandler psIH = (PubSubInputHandler) al;
                    if (psIH._targetStreamManager.isEmpty())
                    {
                        // Flush finished
                        synchronized (psIH)
                        {
                            psIH._flushedForDeleteTarget = true;
                            psIH._deleteFlushTarget = null;
                        }

                        // Now redrive the actual deletion
                        psIH._messageProcessor.getDestinationManager().startAsynchDeletion();

                    }
                    else
                    {
                        // Query the flush again
                        try
                        {
                            psIH._targetStreamManager.queryUnflushedStreams();
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                                SibTr.event(tc, "Querying target for flush on destination: " + _destination.getName());
                            psIH._messageProcessor.getAlarmManager().create(SIMPConstants.LOG_DELETED_FLUSH_WAIT, this, psIH);
                        }
                        catch (SIResourceException e)
                        {
                            // This shouldn't actually be possible so log it
                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.PubSubInputHandler.alarm",
                                                        "1:3815:1.329.1.1",
                                                        this);

                            SibTr.exception(tc, e);

                            // There's no one to catch this exception so eat it.  Note that this also
                            // kills the flush retry.
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                                SibTr.event(tc, "Target flushed cancelled by SIResourceException");

                        }
                    }
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "alarm");
                }
            };

            _targetStreamManager.queryUnflushedStreams();
            _messageProcessor.getAlarmManager().create(SIMPConstants.LOG_DELETED_FLUSH_WAIT, _deleteFlushTarget, this);

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "flushAllForDeleteTarget", Boolean.FALSE);
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.MessageDeliverer#checkAbleToAcceptMessage
     */
    @Override
    public int checkAbleToAcceptMessage(JsDestinationAddress addr)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkAbleToAcceptMessage", addr);

        int blockingReason = DestinationHandler.OUTPUT_HANDLER_FOUND;
        //we check the remoteQueueHighLimit in this case. 
        //See defect 281311
        boolean canAccept = !_itemStream.isRemoteQueueHighLimit();

        if (!canAccept)
        {
            blockingReason = DestinationHandler.OUTPUT_HANDLER_ALL_HIGH_LIMIT;
            //117505
            long destHighMsg = _itemStream.getDestHighMsgs();
            SibTr.info(tc, "NOTIFY_DEPTH_THRESHOLD_REACHED_CWSIP0553",
                       new Object[] { _destination.getName(), _messageProcessor.getMessagingEngineName(), destHighMsg });
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkAbleToAcceptMessage", Integer.valueOf(blockingReason));
        return blockingReason;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.MessageDeliverer#checkStillBlocked
     */
    @Override
    public int checkStillBlocked()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkStillBlocked");

        int blockingReason = DestinationHandler.OUTPUT_HANDLER_FOUND;

        boolean isBlocked = !_itemStream.isQLowRemoteLimit();
        if (isBlocked)
            blockingReason = DestinationHandler.OUTPUT_HANDLER_ALL_HIGH_LIMIT;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkStillBlocked", Integer.valueOf(blockingReason));
        return blockingReason;

    }

    /**
     * Report a long lived gap in a GD stream (510343)
     * 
     * @param sourceMEUuid
     * @param gap
     */
    @Override
    public void reportUnresolvedGap(String sourceMEUuid, long gap)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "reportUnresolvedGap", new Object[] { sourceMEUuid, new Long(gap) });

        //A gap starting at sequence id {0} in the message stream for destination {1} from messaging engine {2} has been detected on messaging engine {3}.
        SibTr.info(tc, "UNRESOLVED_GAP_IN_DESTINATION_TRANSMITTER_CWSIP0792",
                   new Object[] { (new Long(gap)).toString(),
                                 _destination.getName(),
                                 SIMPUtils.getMENameFromUuid(sourceMEUuid),
                                 _messageProcessor.getMessagingEngineName() });

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reportUnresolvedGap");
    }

    /**
     * Issue an all clear on a previously reported gap in a GD stream (510343)
     * 
     * @param sourceMEUuid
     * @param filledGap
     */
    @Override
    public void reportResolvedGap(String sourceMEUuid, long filledGap)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "reportResolvedGap", new Object[] { sourceMEUuid, new Long(filledGap) });

        //The gap starting at sequence id {0} in the message stream for destination {1} from messaging engine {2} has been resolved on message engine {3}.
        SibTr.info(tc, "RESOLVED_GAP_IN_DESTINATION_TRANSMITTER_CWSIP0793",
                   new Object[] { (new Long(filledGap)).toString(),
                                 _destination.getName(),
                                 SIMPUtils.getMENameFromUuid(sourceMEUuid),
                                 _messageProcessor.getMessagingEngineName() });

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reportResolvedGap");
    }

    /**
     * Report a high proportion of repeated messages being sent from a remote ME (510343)
     * 
     * @param sourceMEUUID
     * @param percent
     */
    @Override
    public void reportRepeatedMessages(String sourceMEUuid, int percent)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "reportRepeatedMessages", new Object[] { sourceMEUuid, new Integer(percent) });

        // "{0} percent repeated messages received from messaging engine {1} on messaging engine {2} for destination {3}"
        SibTr.info(tc, "REPEATED_MESSAGE_THRESHOLD_REACHED_ON_DESTINATION_CWSIP0795",
                   new Object[] { new Integer(percent),
                                 SIMPUtils.getMENameFromUuid(sourceMEUuid),
                                 _messageProcessor.getMessagingEngineName(),
                                 _destination.getName() });

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reportRepeatedMessages");
    }

    @Override
    public long sendNackMessageWithReturnValue(SIBUuid8 source, SIBUuid12 destUuid,
                                               SIBUuid8 busUuid, long startTick, long endTick, int priority,
                                               Reliability reliability, SIBUuid12 streamID) throws SIResourceException {
        return 0;
    }
}
