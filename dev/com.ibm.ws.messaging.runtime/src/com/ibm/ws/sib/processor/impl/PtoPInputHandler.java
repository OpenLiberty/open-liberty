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
import java.util.List;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.exception.WsRuntimeException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsApiMessage;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsDestinationAddressFactory;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageCreateFailedException;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.mfp.control.ControlAck;
import com.ibm.ws.sib.mfp.control.ControlAreYouFlushed;
import com.ibm.ws.sib.mfp.control.ControlMessage;
import com.ibm.ws.sib.mfp.control.ControlNack;
import com.ibm.ws.sib.mfp.control.ControlRequestFlush;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.UndeliverableReturnCode;
import com.ibm.ws.sib.processor.exceptions.SIMPConnectionLostException;
import com.ibm.ws.sib.processor.exceptions.SIMPErrorException;
import com.ibm.ws.sib.processor.exceptions.SIMPLimitExceededException;
import com.ibm.ws.sib.processor.exceptions.SIMPNoLocalisationsException;
import com.ibm.ws.sib.processor.exceptions.SIMPNotAuthorizedException;
import com.ibm.ws.sib.processor.exceptions.SIMPNotPossibleInCurrentConfigurationException;
import com.ibm.ws.sib.processor.exceptions.SIMPResourceException;
import com.ibm.ws.sib.processor.exceptions.SIMPRollbackException;
import com.ibm.ws.sib.processor.exceptions.SIMPSendAllowedException;
import com.ibm.ws.sib.processor.gd.ExpressTargetStream;
import com.ibm.ws.sib.processor.gd.GDConfig;
import com.ibm.ws.sib.processor.gd.GuaranteedTargetStream;
import com.ibm.ws.sib.processor.impl.exceptions.RMQResourceException;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.InputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.MessageProducer;
import com.ibm.ws.sib.processor.impl.interfaces.OutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.ProducerInputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.MessageEvents;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.impl.store.itemstreams.TargetProtocolItemStream;
import com.ibm.ws.sib.processor.utils.LockManager;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.UserTrace;
import com.ibm.ws.sib.security.auth.OperationType;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;
import com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException;

/**
 * The Point to Point version of the InputHandler.
 * 
 * The InputHandler attaches itself to a Destination which is
 * a Queue.
 */
public class PtoPInputHandler
                extends AbstractInputHandler
{
    // NLS for component
    private static final TraceNLS nls =
                    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);
    private static final TraceNLS nls_cwsik =
                    TraceNLS.getTraceNLS(SIMPConstants.CWSIK_RESOURCE_BUNDLE);

    private static final TraceComponent tc =
                    SibTr.register(
                                   PtoPInputHandler.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    // NLS for component
    private static final TraceNLS nls_mt =
                    TraceNLS.getTraceNLS(SIMPConstants.TRACE_MESSAGE_RESOURCE_BUNDLE);

    private final DestinationManager _destinationManager;

    // Flag to indicate whether the destination is a LinkHandler
    // and name of the Link and Uuid of sending Bus if it is
    // Defect 238709: Note that an MQlinkHandler is much closer to a BDH than a LinkHandler
    // and for the purposes of this class will be classified as NOT being a link
    private boolean _isLink = false;
    private String _linkName = null;

    // If true, then a pending flush for delete from the target
    // node has completed successfully.
    private boolean _flushedForDeleteTarget = false;

    // If non-null, then we're attempting to flush the target
    // stream so we can delete the destination.
    private AlarmListener _deleteFlushTarget = null;

    // If a link is blocked because a target destination cannot accept messages, this variable
    // allows us to keep track of which destination is the blocker.
    private JsDestinationAddress _linkBlockingDestination = null;

    public PtoPInputHandler(DestinationHandler destination,
                            TargetProtocolItemStream targetProtocolItemStream)
    {
        super(destination, targetProtocolItemStream);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "PtoPInputHandler",
                        new Object[] { destination, targetProtocolItemStream });

        _destinationManager = _messageProcessor.getDestinationManager();

        // Defect 238709: An MQLinkHandler is deemed not to be a link
        if (destination.isLink() && !destination.isMQLink())
        {
            _isLink = true;
            _linkName = destination.getName();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "PtoPInputHandler", this);

    }

    /**
     * Processes the message to either a local or remote destination
     * 
     * @param msg The message to put
     * @param transaction The transaction object
     * @param sourceCellule The source for the message
     * 
     */
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
            SibTr.entry(this, tc, "internalHandleMessage", new Object[] { msg,
                                                                         transaction,
                                                                         sourceMEUuid,
                                                                         inAddress,
                                                                         sender,
                                                                         msgFRP });

        if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
            traceSend(msg);

        boolean lockedDestination = false;
        boolean forcePut = msg.isForcePut();

        // This checks the message Reliability against the destination
        // and sets the max storage strategy for temporary destinations
        super.handleMessage(msg);

        SIBUuid8 localMEUuid = _messageProcessor.getMessagingEngineUuid();

        // Remember if the original user transaction was a real one or not
        msg.setTransacted(!transaction.isAutoCommit());

        if (localMEUuid.equals(sourceMEUuid))
        {
            // The message may actually have been produced locally or it may have come
            // from a remote ME and been re-routed to this inputHandler using the
            // routingDestination.

            // First, sort out any changes to the message's reverse routing path
            updateReverseRoutingPath(msg);

            // First, sort out any changes to the message's reverse routing path
            List<SIDestinationAddress> frp = updateForwardRoutingPath(msg);

            //defect 269072: we cannot skip FRP destinations if the next
            //destination is on a foreign bus as we do not know anything
            //about it.
            boolean isPuttingToForeignBus = _destination.isForeignBus();

            if (frp != null && !isPuttingToForeignBus) // Correct foreign bus check (aliases?)??
            {
                handleFRPMessage(msg, transaction, sender, msgFRP);
            }
            else if ((frp == null) &&
                     (_destination.getDestinationType() == DestinationType.SERVICE))
            {
                //Put the message to the exception destination
                handleUndeliverableMessage(_destination
                                           , null
                                           , msg
                                           , SIRCConstants.SIRC0034_FORWARD_ROUTING_PATH_ERROR
                                           , new String[] { _destination.getName(),
                                                           _messageProcessor.getMessagingEngineName() },
                                           transaction);
            }
            // Otherwise, we process the message on this destination (or the destination
            // that this destination references - if an alias).
            else
            {
                // We have not sideways punted the message so need to perform
                // additional checks now

                //For debug purposes, tell the message which destinations its on
                if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
                {
                    msg.setDebugName(_destination.getName());
                }

                // If the message came in from a remote bus then
                // check whether the userId is authorised to this destination
                if (msg.isFromRemoteBus() && _messageProcessor.isBusSecure())
                    checkInboundAuthorisation(msg.getMessage());

                // If possible use the sender's cached routing destination (which would
                // also include any fixed message point).
                // We can't use it if this message had an FRP already in it as the message
                // may go elsewhere. This means messages with an embedded FRP cannot be bound to a
                // single mesage point if the sender requests it

                // Will we need to set a routing destination back into the sender?
                boolean newSenderRoutingAddr = false;
                JsDestinationAddress routingAddr = null;

                // Work out if we're already bound to a single ME, or we need to
                SIBUuid8 fixedME = null;
                boolean fixToME = false;
                // If we're in the context of a sender that has specified bindToMessagePoint
                // and the message doesn't have an FRP embedded into it then we can fix to a single ME
                if ((sender != null) && sender.fixedMessagePoint() && !msgFRP)
                {
                    // Fixing on an ME doesn't make sense when sending down a link as we only have the one.
                    // In fact, if you fix to an ME you end up fixing to the target ME (the one at the
                    // other end of the link) which then cancels any attept to workload balance messages
                    // as they come of the link.
                    // TODO: If we ever support multiple links then we need to fix this so that you can
                    //       fix to a single link, rather than the target ME.
                    if (!_destination.isTargetedAtLink())
                        fixToME = true;
                }

                if (inAddress != null)
                {
                    // If the address is marked localOnly but we don't already have an
                    // ME in it we'll only use the local message point
                    // if it exists (even if it's currently unavailable).
                    // If we don't have a local one we simply ignore the option
                    // If the destination is foreign it can't possibly have a local
                    // message point.
                    if ((inAddress.getME() == null) && inAddress.isLocalOnly())
                    {
                        if (!_destination.isTargetedAtLink() && _destination.hasLocal())
                        {
                            // Burn this ME's UUID into the address
                            inAddress.setME(_messageProcessor.getMessagingEngineUuid());
                        }
                    }

                    // If the address that the sender used has an ME in it, we'll use it
                    if (inAddress.getME() != null)
                    {
                        fixedME = inAddress.getME();
                        fixToME = true;
                    }
                }

                if (msg.getMessage().isMediated())
                {

                    // The originator of the message (the app)
                    // may have bound the producer to a single message point in the hope of
                    // getting affinity across multiple messages.

                    // If this message has only been transformed rather than re-routed 
                    // then we'll have a go at maintaining the affinity on the outbound
                    // queue point. As we don't have the producer's session context we use the existence
                    // of a fixed ME in the routingAddress in the message to indicate that the sender
                    // bound the messages to a single message point.

                    // The only routing restriction we can apply reliably (i.e. across ME restarts) is to
                    // send all such messages to the local queue point if it exists (not just available).
                    JsDestinationAddress inRoutingAddress = msg.getMessage().getRoutingDestination();
                    if ((inRoutingAddress != null) && (inRoutingAddress.getME() != null))
                    {
                        if (_destination.hasLocal())
                        {
                            fixedME = _messageProcessor.getMessagingEngineUuid();
                            fixToME = true;
                        }
                    }
                }

                // If a local sender doesn't already have a routing destination for us or we
                // can't use it because some *** of an application set an FRP into the message
                // (which means we can't bind to a single ME if they ask) or the message came in
                // remotely we need to resolve a routing address.
                if (msgFRP || (sender == null) || !sender.isRoutingDestinationSet())
                {
                    // Resolve any required routing destination. This is quite likely to be null
                    // as we only need one for certain reasons (e.g. foreign destination, fixed ME
                    // scoped alias).
                    routingAddr = _destination.getRoutingDestinationAddr(inAddress,
                                                                         fixToME);

                    // If we have a fixed ME then set it into the routing address for
                    // anyone else looking at this message.
                    if (fixedME != null)
                    {
                        if ((inAddress == null) || !routingAddr.equals(inAddress))
                        {
                            // If we got this address from the destination handler we need to make a copy of it
                            // before we modify it as this is actually a reference to the cached address on the
                            // destination itself
                            routingAddr = SIMPUtils.createJsDestinationAddress(routingAddr.getDestinationName(),
                                                                               fixedME,
                                                                               routingAddr.getBusName());
                        }
                        else
                        {
                            routingAddr = inAddress; // Use the inAddress (which may be 'equal' the destination's but be a different object)
                            routingAddr.setME(fixedME);
                        }
                    }

                    // We'll set it back into the sender, if we're allowed.
                    if ((sender != null) && !msgFRP)
                        newSenderRoutingAddr = true;
                }
                // If the sender already has a routing destination set (which may be null)
                // then use it
                else if ((sender != null) && sender.isRoutingDestinationSet())
                {
                    routingAddr = sender.getRoutingDestination();

                    // Pull out any fixed ME that it has
                    if (routingAddr != null)
                    {
                        fixedME = routingAddr.getME();
                        if (fixedME != null)
                            fixToME = true;
                    }
                }

                // Now we know if we're fixed to a particular ME or not we can check for
                // a 'good' OutputHandler. We do this check now, in case we're transacted
                // and don't try the OutputHandlers until the transaction commits
                try
                {
                    checkHandlerAvailable(fixedME);
                }
                // This indicates that the queue high limit has been reached
                catch (SIMPLimitExceededException e)
                {
                    // No FFDC code needed

                    //If we are forcing put then we ignore queue high limits.
                    if (forcePut)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "Forcing msg put despite exception :" + e);
                    }
                    // We don't accept messages produced locally when we're full,
                    // we throw an exception back to the app.
                    else if (!msg.isFromRemoteME())
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        {
                            SibTr.exception(tc, e);
                            SibTr.exit(tc, "internalHandleMessage", "SIMPLimitExceededException");
                        }
                        throw e;
                    }
                    //If it came from another bus then we put the message to the
                    //exception destination.
                    else if (msg.isFromRemoteBus())
                    {
                        if (msg.getReliability().compareTo(Reliability.EXPRESS_NONPERSISTENT) > 0)
                        {
                            // We have already checked at the target stream to see if there is
                            // space for this message either on its destination or on the link's
                            // exception destination.
                            // However, there is a window when either (or both) could have filled
                            // up since the check, perhaps due to local puts.
                            // We rethrow the exception, which will allow the message to be put to
                            // the link's exception destination if possible.
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(tc, "Message from remote bus rethrow exception: " + e);
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            {
                                SibTr.exception(tc, e);
                                SibTr.exit(tc, "internalHandleMessage", "SIMPLimitExceededException");
                            }
                            throw e;
                        }
                        else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "Message dropped - it was only best effort");

                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "internalHandleMessage");

                        return;
                    }
                    // Otherwise, this is a message from a remote ME in this bus. We let
                    // these onto the destination as they've already performed a WLM check
                    // to get here, which mean't we weren't full at the time.
                    else
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "Allowing remote message on to destination :" + e);
                    }
                } catch (SIMPNotPossibleInCurrentConfigurationException e)
                {
                    // No FFDC code needed
                    // This indicates that the queue was put disabled
                    // It doesn't matter whether the message was produced remotely
                    // or locally this is a problem
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    {
                        SibTr.exception(tc, e);
                        SibTr.exit(tc, "internalHandleMessage", "SIMPNotPossibleInCurrentConfigurationException");
                    }
                    throw e;
                }

                SIBUuid8 preferredME = null;
                // If the sender prefers any local ME over others then we'll
                // give it a go for them
                if (msg.preferLocal())
                {
                    if (_destination.hasLocal())
                        preferredME = _messageProcessor.getMessagingEngineUuid();
                }

                // If message was produced locally and transacted and we have not
                // decided to force it to be stored now,  we delay the store until
                // we get a callback at pre-prepare time.
                if (!(msg.isFromRemoteME())
                    && (msg.isTransacted())
                    && !(msg.isToBeStoredAtSendTime()))
                {
                    // We need to set any required routing destination into the message
                    // before we register it with the transaction. However, because we're
                    // about to loose the context of the sender (we've no guarantee that
                    // the sender will exist by the time the transaction commits) we need
                    // to make sure that if a fixed message point is still required we
                    // resolve it now.

                    // If we've decided we need to fix to a single ME but haven't yet chosen
                    // one, choose it now.
                    if (fixToME && (fixedME == null))
                    {
                        // Choose one of the available output handlers
                        OutputHandler outputHandler =
                                        _destination.choosePtoPOutputHandler(null,
                                                                             preferredME,
                                                                             !msg.isFromRemoteME(),
                                                                             forcePut,
                                                                             null);

                        //We can't find a suitable localisation.
                        //Although a queue must have at least one localisation this is
                        //possible if the sender restricted the potential localisations
                        //using a fixed ME or a scoping alias (to an out-of-date set of localisation)
                        //In some cases all we can do is put the message to the exception destination.  In others, we
                        //can throw an exception to the putting application.  From here, we throw an exception
                        //and its up to the calling class to determine how best to handle it
                        if (outputHandler == null)
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(
                                           tc,
                                           "internalHandleMessage",
                                           "SIMPNoLocalisationsException");
                            SIMPNoLocalisationsException e = new SIMPNoLocalisationsException(
                                            nls_cwsik.getFormattedMessage(
                                                                          "DELIVERY_ERROR_SIRC_26", // NO_LOCALISATIONS_FOUND_ERROR_CWSIP0032
                                                                          new Object[] { _destination.getName() }, null));

                            e.setExceptionReason(SIRCConstants.SIRC0026_NO_LOCALISATIONS_FOUND_ERROR);
                            e.setExceptionInserts(new String[] { _destination.getName() });
                            throw e;
                        }

                        // Set the chosen one into the routing destination
                        if ((inAddress == null) || !routingAddr.equals(inAddress))
                        {
                            // If we got this address from the destination handler we need to make a copy of it
                            // before we modify it as this is actually a reference to the cached address on the
                            // destination itself
                            routingAddr = SIMPUtils.createJsDestinationAddress(routingAddr.getDestinationName(),
                                                                               outputHandler.getTargetMEUuid(),
                                                                               routingAddr.getBusName());
                        }
                        else
                        {
                            routingAddr = inAddress; // Use the inAddress (which may be 'equal' the destination's but be a different object)
                            routingAddr.setME(outputHandler.getTargetMEUuid());
                        }
                    }

                    // Set the routing destination into the message if it's changed
                    if ((routingAddr != null) || (msg.getMessage().getRoutingDestination() != null))
                        msg.getMessage().setRoutingDestination(routingAddr);

                    // If necessary, set a new routing destination into the sender.
                    if (newSenderRoutingAddr)
                        sender.setRoutingAddress(routingAddr);

                    // Register the message for storing at pre-prepare of the transaction.
                    registerMessage(msg, transaction); //183715.1
                }
                else
                {
                    // If COD reports required, register for the prePrepare callback
                    if (msg.getReportCOD() != null && _destination instanceof BaseDestinationHandler)
                        msg.registerMessageEventListener(MessageEvents.COD_CALLBACK, (BaseDestinationHandler) _destination);

                    // Read Lock the destination to prevent the reallocator running on the chosen localisation
                    LockManager reallocationLock = _destination.getReallocationLockManager();
                    reallocationLock.lock();
                    lockedDestination = true;

                    try
                    {
                        // 176658.3.5
                        // Now look for the best OutputHander at this time.
                        OutputHandler outputHandler =
                                        _destination.choosePtoPOutputHandler(fixedME,
                                                                             preferredME,
                                                                             (!msg.isFromRemoteME()),
                                                                             forcePut,
                                                                             null);

                        //We can't find a suitable localisation.
                        //Although a queue must have at least one localisation this is
                        //possible if the sender restricted the potential localisations
                        //using a fixed ME or a scoping alias (to an out-of-date set of localisation)
                        //In some cases all we can do is put the message to the exception destination.  In others, we
                        //can throw an exception to the putting application.  From here, we throw an exception
                        //and its up to the calling class to determine how best to handle it
                        if (outputHandler == null)
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(
                                           tc,
                                           "internalHandleMessage",
                                           "SIMPNoLocalisationsException");
                            SIMPNoLocalisationsException e = new SIMPNoLocalisationsException(
                                            nls_cwsik.getFormattedMessage(
                                                                          "DELIVERY_ERROR_SIRC_26", // NO_LOCALISATIONS_FOUND_ERROR_CWSIP0032
                                                                          new Object[] { _destination.getName() }, null));

                            e.setExceptionReason(SIRCConstants.SIRC0026_NO_LOCALISATIONS_FOUND_ERROR);
                            e.setExceptionInserts(new String[] { _destination.getName() });
                            throw e;
                        }

                        // If the sender wants all future message to go to the same message
                        // point we fix the message point to be this one for all future requests
                        // (unless it already had it set or this message had an FRP from the message)
                        if (fixToME && (fixedME == null))
                        {
                            // We need to set this chosen ME into the routing destination
                            // as we don't already have it.
                            if ((inAddress == null) || !routingAddr.equals(inAddress))
                            {
                                // If we got this address from the destination handler we need to make a copy of it
                                // before we modify it as this is actually a reference to the cached address on the
                                // destination itself
                                routingAddr = SIMPUtils.createJsDestinationAddress(routingAddr.getDestinationName(),
                                                                                   outputHandler.getTargetMEUuid(),
                                                                                   routingAddr.getBusName());
                            }
                            else
                            {
                                routingAddr = inAddress; // Use the inAddress (which may be 'equal' the destination's but be a different object)
                                routingAddr.setME(outputHandler.getTargetMEUuid());
                            }
                        }

                        // Now we set the routing destination into the sender for future use
                        // (with the fixed ME set into it if applicable)
                        if (newSenderRoutingAddr)
                            sender.setRoutingAddress(routingAddr);

                        // Now we've resolved any possible fixed ME in the routing destination
                        // we set it into the message (remember, the routing destination is still
                        // most likely to be null).
                        if ((routingAddr != null) || (msg.getMessage().getRoutingDestination() != null))
                            msg.getMessage().setRoutingDestination(routingAddr);

                        //Indicate in the message if this was a guess
                        msg.setStreamIsGuess(outputHandler.isWLMGuess());

                        boolean stored = false;

                        outputHandler.put(msg,
                                          transaction,
                                          null,
                                          stored);
                    } catch (SIMPNoLocalisationsException e)
                    {
                        //No FFDC code needed
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "internalHandleMessage", e);
                        throw e;
                    } catch (SIRollbackException e)
                    {
                        // No FFDC code needed

                        SIMPRollbackException ee = new SIMPRollbackException(e.getMessage());
                        ee.setStackTrace(e.getStackTrace());
                        ee.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
                        ee.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.ProducerSessionImpl.handleMessage",
                                                             "1:804:1.323",
                                                             SIMPUtils.getStackTrace(e) });

                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "internalHandleMessage", ee);

                        throw ee;
                    } catch (SIConnectionLostException e)
                    {
                        // No FFDC code needed

                        SIMPConnectionLostException ee = new SIMPConnectionLostException(e.getMessage());
                        ee.setStackTrace(e.getStackTrace());
                        ee.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
                        ee.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.ProducerSessionImpl.handleMessage",
                                                             "1:820:1.323",
                                                             SIMPUtils.getStackTrace(e) });

                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "internalHandleMessage", ee);

                        throw ee;
                    } catch (SIResourceException e)
                    {
                        // No FFDC code needed

                        SIMPResourceException ee = new SIMPResourceException(e);

                        ee.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
                        ee.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.ProducerSessionImpl.handleMessage",
                                                             "1:836:1.323",
                                                             SIMPUtils.getStackTrace(e) });
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "internalHandleMessage", ee);
                        throw ee;
                    } catch (RuntimeException e)
                    {
                        // FFDC
                        FFDCFilter.processException(
                                                    e,
                                                    "com.ibm.ws.sib.processor.impl.PtoPInputHandler.internalHandleMessage",
                                                    "1:848:1.323",
                                                    this);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        {
                            SibTr.exception(tc, e);
                        }

                        SIMPErrorException ee = new SIMPErrorException(e);

                        ee.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
                        ee.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.ProducerSessionImpl.handleMessage",
                                                             "1:860:1.323",
                                                             SIMPUtils.getStackTrace(e) });
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "internalHandleMessage", ee);
                        throw ee;
                    } finally
                    {
                        if (lockedDestination == true)
                        {
                            // unlock the reallocation lock
                            reallocationLock.unlock();
                            lockedDestination = false;
                        }
                    }
                }
            }
        }
        else
        {
            // This is a message that has been received from a
            // remote ME. Call remoteToLocal put to add it to the
            // in this case and instead use the one attached to
            // target stream
            // The message will be processed in deliverOrderedMessages when
            // the stream is in order

            // We actually ignore the source cellule in this case and
            // instead use the one attached to the message itself.
            try
            {
                remoteToLocalPut(msg, sourceMEUuid);
            } catch (SIResourceException e)
            {
                // No FFDC code needed
                SIMPResourceException ee = new SIMPResourceException(e);

                ee.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
                ee.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.ProducerSessionImpl.handleMessage",
                                                     "1:900:1.323",
                                                     SIMPUtils.getStackTrace(e) });
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "internalHandleMessage", ee);
                throw ee;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "internalHandleMessage");

        return;
    }

    private void updateReverseRoutingPath(MessageItem msg)
                    throws SIResourceException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "updateReverseRoutingPath", new Object[] { this, msg });

        boolean updatedRRP = false;

        // Set any administered reply destination

        JsDestinationAddress replyDest = _destination.getReplyDestination();
        if (replyDest != null &&
            (!msg.isFromRemoteME() || msg.isFromRemoteBus()) && // Defect 266979 & 276902
            (!msg.getMessage().isReverseRoutingPathEmpty()))
        {
            List<SIDestinationAddress> replyPath = msg.getMessage().getReverseRoutingPath();
            replyPath.add(0, replyDest);
            msg.getMessage().setReverseRoutingPath(replyPath);
        }

        /*
         * Resolve destination addresses in the reverse routing path if
         * one exists. For every destination in the path, if the destination
         * is local, set its uuid to this MEs uuid.
         */
        List<SIDestinationAddress> rrp = null;

        if (!msg.getMessage().isReverseRoutingPathEmpty())
            rrp = msg.getMessage().getReverseRoutingPath(); //Can not return null in the current implementation
        if (rrp != null)
        {
            JsDestinationAddress dest = null;
            for (int i = 0; i < rrp.size(); i++)
            {
                dest = (JsDestinationAddress) rrp.get(i);

                // If no busName was set, the local bus is implied. set it here in case the
                // message gets sent out of this bus and doesn't know how to get back.
                if (dest.getBusName() == null)
                {
                    dest.setBusName(_messageProcessor.getMessagingEngineBus());
                    updatedRRP = true;
                    rrp.set(i, dest); // Update the list if we have set the busName
                }

                if (dest.getME() == null && dest.isLocalOnly())
                {
                    try
                    {
                        SIBUuid8 meUuid = null;

                        // The creator of this message has requested that this reply destination is
                        // scoped to the local message point. First we must check to see if there is a
                        // local message point for the destination (taking into account aliases).
                        // If there isn't then we just ignore the request.
                        DestinationHandler reverseDestination =
                                        _destinationManager.getDestination(dest, true);

                        if (reverseDestination != null)
                        {
                            boolean localMessagePoint = false;

                            //If the destination is foreign it can't have a local message point
                            if (!reverseDestination.isTargetedAtLink())
                            {
                                if (reverseDestination.hasLocal())
                                    localMessagePoint = true;
                            }

                            if (localMessagePoint)
                            {
                                // Burn this ME's UUID into the address
                                meUuid = _messageProcessor.getMessagingEngineUuid();
                            }
                        }

                        // We need to remove the localOnly flag from the address before we
                        // send the message on to (possibly) a different ME as the localOnly
                        // is meant to indicate "local to the sending application, not the replier"

                        // Get hold of the JsDestinationAddress factory as we need to create a
                        // new address.
                        JsDestinationAddressFactory addressFactory =
                                        (JsDestinationAddressFactory) MessageProcessor.getSingletonInstance(
                                                        SIMPConstants.JS_DESTINATION_ADDRESS_FACTORY);

                        // Create a new address with no localOnly flag but possibly with an ME
                        // uuid in it.
                        JsDestinationAddress newAddr = addressFactory.createJsDestinationAddress(dest.getDestinationName(),
                                                                                                 false,
                                                                                                 meUuid,
                                                                                                 dest.getBusName());

                        // Replace the original address with the new one
                        rrp.set(i, newAddr);
                        updatedRRP = true;
                    } catch (SITemporaryDestinationNotFoundException e)
                    {
                        // No FFDC code needed

                        // We don't really care about the temporary/system queue case as they can only
                        // have one queue point anyway - so the localOnly won't make any difference
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        {
                            SibTr.exception(tc, e);
                            SibTr.debug(
                                        tc,
                                        "Destination not found - ignoring localOnly flag");
                        }
                    }
                }
            }

            if (updatedRRP) //If the frp has changed then we need to set the field in the msg
            {
                msg.getMessage().setReverseRoutingPath(rrp);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updateReverseRoutingPath", new Object[] { Boolean.valueOf(updatedRRP), rrp });
    }

    private List<SIDestinationAddress> updateForwardRoutingPath(MessageItem msg)
                    throws SIResourceException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "updateForwardRoutingPath", new Object[] { this, msg });

        // Resolve destination addresses in the forward routing path if
        // one exists. For every destination in the path, if the destination
        // is local, set its uuid to this MEs uuid. (179339.3.1)
        List<SIDestinationAddress> frp = null;
        boolean updatedFRP = false;

        if (!msg.getMessage().isForwardRoutingPathEmpty())
        {
            frp = msg.getMessage().getForwardRoutingPath(); //Can not return null in the current implementation
        }
        // Set any administered forward routing path, unless we already have one in the
        // message - which may have come from the application (msgFRP = true) or from
        // a destination which references this destination via its own FRP. In either
        // case the first one takes precedence and this one is ignored.

        if (frp != null)
        {
            JsDestinationAddress dest = null;
            for (int i = 0; i < frp.size(); i++)
            {
                dest = (JsDestinationAddress) frp.get(i);
                if (dest.getME() == null && dest.isLocalOnly())
                {
                    // The creator of this message has requested that this destination is
                    // scoped to the local message point. First we must check to see if there is a
                    // local message point for the destination (taking into account aliases).
                    // If there isn't then we just ignore the request.
                    try
                    {
                        SIBUuid8 meUuid = null;

                        DestinationHandler frpDestination =
                                        _destinationManager.getDestination(dest, true);

                        boolean localMessagePoint = false;

                        //If the destination is foreign it can't have a local message point
                        if (!frpDestination.isTargetedAtLink())
                        {
                            if (frpDestination.hasLocal())
                                localMessagePoint = true;
                        }

                        if (localMessagePoint)
                        {
                            // Burn this ME's UUID into the address
                            meUuid = _messageProcessor.getMessagingEngineUuid();
                        }
                        // We can remove the localOnly flag from the address before we
                        // send the message on to (possibly) a message point as we'll already have
                        // resolved any local message points.

                        // Get hold of the JsDestinationAddress factory as we need to create a
                        // new address.
                        JsDestinationAddressFactory addressFactory =
                                        (JsDestinationAddressFactory) MessageProcessor.getSingletonInstance(
                                                        SIMPConstants.JS_DESTINATION_ADDRESS_FACTORY);

                        // Create a new address with no localOnly flag but possibly with an ME
                        // uuid in it.
                        JsDestinationAddress newAddr = addressFactory.createJsDestinationAddress(dest.getDestinationName(),
                                                                                                 false,
                                                                                                 meUuid,
                                                                                                 dest.getBusName());

                        // Replace the original address with the new one
                        frp.set(i, newAddr);
                        updatedFRP = true;
                    } catch (SITemporaryDestinationNotFoundException e)
                    {
                        // No FFDC code needed

                        // We don't really care about the temporary/system queue case as they can only
                        // have one queue point anyway - so the localOnly won't make any difference
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        {
                            SibTr.exception(tc, e);
                            SibTr.debug(
                                        tc,
                                        "Destination not found - ignoring localOnly flag");
                        }
                    }
                }
            }

            if (updatedFRP) //If the frp has changed then we need to set the field in the msg
            {
                msg.getMessage().setForwardRoutingPath(frp);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updateForwardRoutingPath", new Object[] { Boolean.valueOf(updatedFRP), frp });

        return frp;
    }

    private void checkInboundAuthorisation(JsMessage jsMsg) throws SIDiscriminatorSyntaxException, SINotAuthorizedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkInboundAuthorisation", jsMsg);

        // Before we test for anything else, see whether the destination is
        // a system destination, in which case we'll bypass it.
        // Prepare to do security checks
        if (!_destination.isSystem())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Bus is secure prepare for access checks");

            String discriminator = jsMsg.getDiscriminator();
            SecurityContext secContext = null;

            // Will drive the form of sib.security checkDestinationAccess() that
            // takes a JsMessage
            secContext = new SecurityContext(jsMsg,
                            null, // alt user
                            discriminator,
                            _messageProcessor.getAuthorisationUtils());

            // Check authority to produce to destination
            checkDestinationAccess(_destination, secContext);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkInboundAuthorisation");
    }

    /**
     * An override of AbstractInputHandler.remoteToLocalPut. We
     * override the inherited version since PtP uses stream IDs rather
     * than source cellule.
     * 
     * @param msgItem The message to be put.
     * @param producerSession The producer putting the message. In the
     *            remote case, this is always implemented by MPIO.
     * @param sourceCellule The ME which originated the message.
     */
    protected void remoteToLocalPut(
                                    MessageItem msgItem,
                                    SIBUuid8 sourceMEUuid)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "remoteToLocalPut",
                        new Object[] { msgItem, sourceMEUuid });

        _targetStreamManager.handleMessage(msgItem);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "remoteToLocalPut");

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
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "messageEventOccurred", new Object[] { new Integer(event), msg, tran });

        if (event == MessageEvents.PRE_PREPARE_TRANSACTION) //183715.1
        {
            try
            {
                eventPrecommitAdd((MessageItem) msg, tran);
            } catch (SIDiscriminatorSyntaxException e)
            {
                // No FFDC code needed

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "messageEventOccurred", "SIResourceException " + e);
                throw new SIResourceException(e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "messageEventOccurred");
    }

    /**
     * Method eventPrecommitAdd.
     * 
     * @param msg
     * @param transaction
     * @throws SIStoreException
     * @throws SIResourceException
     */
    final protected void eventPrecommitAdd(MessageItem msg, final TransactionCommon transaction) throws SIDiscriminatorSyntaxException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "eventPrecommitAdd", new Object[] { msg, transaction });

        if (!(_destination.isToBeDeleted()))
        {
            if (msg.isTransacted() && (!(msg.isToBeStoredAtSendTime())))
            {
                // LockR the destination to prevent reallocation from occurring on the chosen localisation
                LockManager reallocationLock = _destination.getReallocationLockManager();
                reallocationLock.lock();
                try
                {
                    // If we fixed the ME it'll be in the routing address in the message
                    SIBUuid8 fixedME = null;
                    JsDestinationAddress routingAddr = msg.getMessage().getRoutingDestination();
                    if (routingAddr != null)
                        fixedME = routingAddr.getME();

                    // If the sender prefers any local ME over others then we'll give it a go
                    SIBUuid8 preferredME = null;
                    if (msg.preferLocal())
                    {
                        if (_destination.hasLocal())
                            preferredME = _messageProcessor.getMessagingEngineUuid();
                    }
                    // 176658.3.5
                    OutputHandler handler = _destination.choosePtoPOutputHandler(fixedME,
                                                                                 preferredME,
                                                                                 !msg.isFromRemoteME(),
                                                                                 msg.isForcePut(),
                                                                                 null);

                    if (handler == null)
                    {
                        // We can't find a suitable localisation.
                        // Although a queue must have at least one localisation this is
                        // possible if the sender restricted the potential localisations
                        // using a fixed ME or a scoping alias (to an out-of-date set of
                        // localisation)

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        {
                            SibTr.debug(tc, "No suitable OutputHandler found for " + _destination.getName() + " (" + fixedME + ")");
                        }

                        //Put the message to the exception destination.
                        handleUndeliverableMessage(_destination
                                                   , null // null LinkHandler
                                                   , msg
                                                   , SIRCConstants.SIRC0026_NO_LOCALISATIONS_FOUND_ERROR
                                                   , new String[] { _destination.getName() }
                                                   , transaction);
                    }
                    else
                    {
                        //Indicate in the message if this was a guess
                        msg.setStreamIsGuess(handler.isWLMGuess());

                        // put the message to the output handler
                        handler.put(msg, transaction, null, true);
                    }
                } finally
                {
                    // unlock the reallocation lock
                    reallocationLock.unlock();
                }
            }
        }
        else
        {
            //The destination has been deleted.  Put the message to the exception destination#
            ExceptionDestinationHandlerImpl exceptionDestinationHandlerImpl =
                            (ExceptionDestinationHandlerImpl) _messageProcessor.createExceptionDestinationHandler(null);

            //Set indicator to send the message to the exception destination immediately,
            //rather than registering it for pre-prepare of the transaction, as this is
            //pre-prepare of the transaction!
            msg.setStoreAtSendTime(true);

            String destName = _destination.getName();
            if (_destination.isLink())
                destName = ((LinkHandler) _destination).getBusName();

            final UndeliverableReturnCode rc =
                            exceptionDestinationHandlerImpl.handleUndeliverableMessage(msg
                                                                                       , transaction
                                                                                       , SIRCConstants.SIRC0032_DESTINATION_DELETED_ERROR
                                                                                       , new String[] { destName,
                                                                                                       _messageProcessor.getMessagingEngineName() });
            if (rc != UndeliverableReturnCode.OK)
            {
                if (rc == UndeliverableReturnCode.DISCARD)
                {
                    //The message is to be discarded.  Do nothing and it will disappear.
                }
                else
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "eventPrecommitAdd", "WsRuntimeException");
                    //We cannot put the message to the exception destination.  All we can
                    //do in this case is rollback the users transaction.  This is done by
                    //throwing an exception that is caught by the transaction manager.
                    throw new WsRuntimeException(
                                    nls.getFormattedMessage(
                                                            "DESTINATION_DELETED_ERROR_CWSIP0247",
                                                            new Object[] { _destination.getName(),
                                                                          rc },
                                                            null));
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventPrecommitAdd");
    }

    public void sendToME(ControlMessage cMsg,
                         SIBUuid8 sourceMEUuid,
                         SIBUuid8 busUuid,
                         int priority) throws SIResourceException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendToME");

        SIBUuid8 routingMEUuid = null;

        // If the destination in a Link add Link specific properties to message
        if (_isLink)
        {
            // Add Link specific properties to message
            cMsg = addLinkProps(cMsg, busUuid);
            SIBUuid8 linkRoutingMEUuid = ((LinkHandler) _destination).getRemoteMEUuid();
            if (linkRoutingMEUuid == null)
            {
                // TODO: Log the fact that we can't get a link to reply on

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "sendToME", "Can't get a link to reply on");
                return;
            }
            routingMEUuid = linkRoutingMEUuid;
        }
        else
        {
            // If this isn't a link we can send responses back to the ME
            // which sent the orginal JsMessage
            routingMEUuid = sourceMEUuid;
        }

        // Send this to the RemoteMessageTransmitter
        _mpio.sendToMe(routingMEUuid, priority, cMsg);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendToME");

    }

    /**
     * sendAckMessage is called from preCommitCallback after
     * the message has been delivered to the final destination
     */
    @Override
    public void sendAckMessage(SIBUuid8 sourceMEUuid,
                               SIBUuid12 destUuid,
                               SIBUuid8 busUuid,
                               long ackPrefix,
                               int priority,
                               Reliability reliability,
                               SIBUuid12 stream,
                               boolean consolidate) throws SIResourceException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendAckMessage", new Long(ackPrefix));

        ControlAck ackMsg = createControlAckMessage();

        // As we are using the Guaranteed Header - set all the attributes as
        // well as the ones we want.
        SIMPUtils.setGuaranteedDeliveryProperties(ackMsg,
                                                  _messageProcessor.getMessagingEngineUuid(),
                                                  sourceMEUuid,
                                                  stream,
                                                  null,
                                                  destUuid,
                                                  ProtocolType.UNICASTOUTPUT,
                                                  GDConfig.PROTOCOL_VERSION);

        ackMsg.setPriority(priority);
        ackMsg.setReliability(reliability);

        ackMsg.setAckPrefix(ackPrefix);

        // Send Ack messages at the priority of the original message +1
        sendToME(ackMsg, sourceMEUuid, busUuid, priority + 1);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendAckMessage");
    }

    /**
     * Sends a Nack message back to the originating ME
     */
    @Override
    public void sendNackMessage(SIBUuid8 sourceMEUuid,
                                SIBUuid12 destUuid,
                                SIBUuid8 busUuid,
                                long startTick,
                                long endTick,
                                int priority,
                                Reliability reliability,
                                SIBUuid12 stream)
                    throws SIResourceException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "sendNackMessage",
                        new Object[] { new Long(startTick), new Long(endTick) });

        ControlNack nackMsg = createControlNackMessage();

        // As we are using the Guaranteed Header - set all the attributes as
        // well as the ones we want.
        SIMPUtils.setGuaranteedDeliveryProperties(nackMsg,
                                                  _messageProcessor.getMessagingEngineUuid(),
                                                  sourceMEUuid,
                                                  stream,
                                                  null,
                                                  destUuid,
                                                  ProtocolType.UNICASTOUTPUT,
                                                  GDConfig.PROTOCOL_VERSION);

        nackMsg.setPriority(priority);
        nackMsg.setReliability(reliability);

        nackMsg.setStartTick(startTick);
        nackMsg.setEndTick(endTick);

        //  Send Nack messages at original message priority +2
        sendToME(nackMsg, sourceMEUuid, busUuid, priority + 2);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendNackMessage ");
    }

    /**
     * Creates an ACK message for sending
     * 
     * @return the new ACK message
     * 
     * @throws SIResourceException if the message can't be created.
     */
    private ControlAck createControlAckMessage() throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createControlAckMessage");

        ControlAck ackMsg = null;

        // Create new AckMessage
        try
        {
            ackMsg = _cmf.createNewControlAck();
        } catch (MessageCreateFailedException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.PtoPInputHandler.createControlAckMessage",
                                        "1:1546:1.323",
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "createControlAckMessage", e);
            }

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.PtoPInputHandler",
                                      "1:1558:1.323",
                                      e });

            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.PtoPInputHandler",
                                                                  "1:1566:1.323",
                                                                  e },
                                                    null),
                            e);
        }

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
    private ControlNack createControlNackMessage()
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createControlNackMessage");

        ControlNack nackMsg = null;

        // Create new NackMessage
        try
        {
            nackMsg = _cmf.createNewControlNack();
        } catch (MessageCreateFailedException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.PtoPInputHandler.createControlNackMessage",
                                        "1:1604:1.323",
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "createControlNackMessage", e);
            }

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.PtoPInputHandler",
                                      "1:1616:1.323",
                                      e });

            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.PtoPInputHandler",
                                                                  "1:1624:1.323",
                                                                  e },
                                                    null),
                            e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createControlNackMessage");

        return nackMsg;
    }

    /**
     * Creates an AREYOUFLUSHED message for sending
     * 
     * @return the new AREYOUFLUSHED message
     * 
     * @throws SIResourceException if the message can't be created.
     */
    protected ControlAreYouFlushed createControlAreYouFlushed() throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createControlAreYouFlushed");

        ControlAreYouFlushed flushedqMsg = null;

        // Create new message
        try
        {
            flushedqMsg = _cmf.createNewControlAreYouFlushed();
        } catch (MessageCreateFailedException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.PtoPInputHandler.createControlAreYouFlushed",
                                        "1:1661:1.323",
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "createControlAreYouFlushed", e);
            }

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002", new Object[] {
                                                                                "com.ibm.ws.sib.processor.impl.PtoPInputHandler",
                                                                                "1:1672:1.323",
                                                                                e });

            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.PtoPInputHandler",
                                                                  "1:1680:1.323",
                                                                  e },
                                                    null),
                            e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createControlAreYouFlushed");

        return flushedqMsg;
    }

    /**
     * Creates a REQUESTFLUSH message for sending
     * 
     * @return the new REQUESTFLUSH message
     * 
     * @throws SIResourceException if the message can't be created.
     */
    protected ControlRequestFlush createControlRequestFlush() throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createControlRequestFlush");

        ControlRequestFlush rflushMsg = null;

        // Create new message
        try
        {
            rflushMsg = _cmf.createNewControlRequestFlush();
        } catch (MessageCreateFailedException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.PtoPInputHandler.createControlRequestFlush",
                                        "1:1717:1.323",
                                        this);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "createControlRequestFlush", e);
            }

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.PtoPInputHandler",
                                      "1:1729:1.323",
                                      e });

            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.PtoPInputHandler",
                                                                  "1:1737:1.323",
                                                                  e },
                                                    null),
                            e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createControlRequestFlush", rflushMsg);

        return rflushMsg;
    }

    /**
     * @param jsMsg
     * @return jsMsg with link properties added
     */
    private ControlMessage addLinkProps(ControlMessage msg, SIBUuid8 busUuid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "addLinkProps");

        // Add Link specific properties to message
        msg.setRoutingDestination(null);
        msg.setGuaranteedCrossBusLinkName(_linkName);
        msg.setGuaranteedCrossBusSourceBusUUID(busUuid);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addLinkProps");

        return msg;
    }

    /**
     * Puts a message to the forward routing path destination of a message.
     * Will throw an SINotAuthorizedException if the user is not allowed
     * to put a message to the destination along the forward routing path
     * 
     * @param msg
     * @param tran
     * @param sourceCellule
     * @return
     * @throws SINotAuthorizedException
     * @throws SIMPNotPossibleInCurrentConfigurationException
     */
    private DestinationHandler handleFRPMessage(MessageItem msg,
                                                TransactionCommon tran,
                                                MessageProducer sender,
                                                boolean msgFRP)

                    throws SINotAuthorizedException,
                    SIMPNotPossibleInCurrentConfigurationException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "handleFRPMessage", new Object[] { msg, tran, sender, Boolean.valueOf(msgFRP) });

        List<SIDestinationAddress> frp = msg.getMessage().getForwardRoutingPath();

        // Walk down the FRP until we come to a destination that the message needs
        // to go to.
        // e.g. it's the end of the list, it's a topicspace or it's in a foreign bus
        JsDestinationAddress head = null;
        DestinationHandler frpDestination = null;
        DestinationHandler previousDestination = _destination;
        do
        {
            // Pop off first element of FRP
            head = (JsDestinationAddress) frp.get(0);

            String name = head.getDestinationName();
            String busName = head.getBusName();

            //Get the named destination from the destination manager
            try
            {
                frpDestination =
                                _messageProcessor.getDestinationManager().
                                                getDestination(name, busName, false);

                // If security is enabled, then we need to check authority to access
                // the next destination
                if (_messageProcessor.isBusSecure())
                {
                    // Will drive the form of sib.security checkDestinationAccess() that
                    // takes a JsMessage
                    JsMessage jsMsg = msg.getMessage();
                    String discriminator = jsMsg.getDiscriminator();
                    SecurityContext secContext = new SecurityContext(jsMsg,
                                    null, // alt user
                                    discriminator,
                                    _messageProcessor.getAuthorisationUtils());
                    checkDestinationAccess(frpDestination,
                                           secContext);
                }

                previousDestination = frpDestination;

                frp.remove(0);
            } catch (SIException e)
            {
                // No FFDC code Needed

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.exception(tc, e);

                // Need to throw NotAuthorized exceptions back to the caller.
                if (e instanceof SINotAuthorizedException)
                {
                    // Write an audit record if access is denied
                    SibTr.audit(tc, nls_cwsik.getFormattedMessage(
                                                                  "DELIVERY_ERROR_SIRC_18", // USER_NOT_AUTH_SEND_ERROR_CWSIP0308
                                                                  new Object[] { name,
                                                                                msg.getMessage().getSecurityUserid() },
                                                                  null));

                    // Thrown if user denied access to destination
                    SIMPNotAuthorizedException ex =
                                    new SIMPNotAuthorizedException(e.getMessage());

                    ex.setExceptionReason(SIRCConstants.SIRC0020_USER_NOT_AUTH_SEND_ERROR);
                    ex.setExceptionInserts(new String[] { name,
                                                         msg.getMessage().getSecurityUserid() });
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "handleFRPMessage", ex);
                    throw ex;
                }

                ExceptionDestinationHandlerImpl excDest = new ExceptionDestinationHandlerImpl(previousDestination);

                final UndeliverableReturnCode rc =
                                excDest.handleUndeliverableMessage(msg,
                                                                   tran,
                                                                   SIRCConstants.SIRC0037_INVALID_ROUTING_PATH_ERROR,
                                                                   new String[] { name,
                                                                                 _messageProcessor.getMessagingEngineName(),
                                                                                 previousDestination.getName(),
                                                                                 SIMPUtils.getStackTrace(e)
                                                                   });

                if (rc == UndeliverableReturnCode.ERROR || rc == UndeliverableReturnCode.BLOCK)
                {
                    SIMPNotPossibleInCurrentConfigurationException ee = new SIMPNotPossibleInCurrentConfigurationException(
                                    nls_cwsik.getFormattedMessage(
                                                                  "DELIVERY_ERROR_SIRC_23", // EXCEPTION_DESTINATION_ERROR_CWSIP0296
                                                                  new Object[] { name, rc, SIMPUtils.getStackTrace(e), _destination.getName() },
                                                                  null),
                                    e);

                    ee.setExceptionReason(SIRCConstants.SIRC0023_EXCEPTION_DESTINATION_ERROR);
                    ee.setExceptionInserts(new String[] { name, rc.toString(), SIMPUtils.getStackTrace(e), _destination.getName() });

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "handleFRPMessage", ee);
                    throw ee;
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "handleFRPMessage");
                // Indicate message was delivered to exception destination
                return null;
            }

        } // bypass the destination if its a queue (If the destination
          // is foreign we cannot tell if there is a medition or not, so we err on the side of
          // caution and send it there anyway)
        while ((!previousDestination.isPubSub() && // and it is not PubSub
                !frp.isEmpty() && // and there is more FRP to process
        !(previousDestination.isForeign() || previousDestination.isForeignBus()))); // and it is not foreign

        // Set the remainder of the FRP back into the message
        msg.getMessage().setForwardRoutingPath(frp);

        // Get the InputHandler for the chosen destination 
        ProducerInputHandler handler = null;
        if (frpDestination != null)
        {
            ProtocolType type = ProtocolType.UNICASTINPUT;
            if (frpDestination.isPubSub())
                type = ProtocolType.PUBSUBINPUT;
            handler = (ProducerInputHandler) frpDestination.getInputHandler(type,
                                                                            _messageProcessor.getMessagingEngineUuid(),
                                                                            null);
        }

        // If handler is null, the message went to the exception destination.
        // Otherwise we have a new InputHandler for the next interesting destination
        // in the FRP. Let it process the message.
        if (handler != null)
        {
            try
            {
                // Do some cleanup of the message to make it look like new 
                JsMessage jsMsg = msg.getMessage();

                // Defect 496906: Set the outAddress to "head" - the JsDestinationAddress of the
                // frpDestination we are working with. This variable is used as a base from which
                // to generate the routing address which will be set into the message.
                JsDestinationAddress outAddress = head;
                // Reset the redelivery count
                jsMsg.setRedeliveredCount(0);

                // If the FRP destination is either an alias or is foreign, then update
                // the RFH2 flag.
                if (frpDestination.isAlias() ||
                    frpDestination.isForeign() ||
                    frpDestination.isForeignBus())
                {
                    ProducerSessionImpl.setRFH2Allowed(jsMsg, frpDestination);
                }

                msg.incrementRedirectCount();

                // Generate error if max frp depth is exceeded
                if (msg.getRedirectCount() > _messageProcessor.getCustomProperties().get_max_frp_depth())
                {
                    SibTr.register(PtoPInputHandler.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.CWSIK_RESOURCE_BUNDLE);

                    SibTr.error(tc, "DELIVERY_ERROR_SIRC_43",
                                new Integer(msg.getRedirectCount()));

                    SibTr.register(PtoPInputHandler.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

                    //Put the message to the exception destination
                    handleUndeliverableMessage(frpDestination
                                               , null // LinkHandler
                                               , msg
                                               , SIRCConstants.SIRC0043_MAX_FRP_DEPTH_EXCEEDED
                                               , new String[] { frpDestination.getName() },
                                               tran);
                }
                // We haven't hit the loop limit so lets pass the message onto the
                // next InputHandler
                else
                {
                    handler.handleProducerMessage(msg,
                                                  tran,
                                                  outAddress,
                                                  null,
                                                  msgFRP);
                }
            } catch (SIMPNoLocalisationsException e)
            {
                // No FFDC code needed

                // We can't find a suitable localisation.
                // Although a queue must have at least one localisation this is
                // possible if the sender restricted the potential localisations
                // using a fixed ME or a scoping alias (to an out-of-date set of
                // localisation)

                //Put the message to the exception destination
                handleUndeliverableMessage(frpDestination
                                           , null // LinkHandler
                                           , msg
                                           , SIRCConstants.SIRC0026_NO_LOCALISATIONS_FOUND_ERROR
                                           , new String[] { frpDestination.getName() },
                                           tran);
            } catch (Exception e)
            {
                //defect 259323
                //due to an unexpected error we cannot send this message along the
                //FRP. The best we can do is send the message to the exception destination
                //and ffdc/trace this event
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.PtoPInputHandler.handleFRPMessage",
                                            "1:2072:1.323",
                                            this);

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                {
                    SibTr.debug(tc, "FRP message being put to exception destination",
                                new Object[] { msg, e });
                }
                handleUndeliverableMessage(frpDestination
                                           , null // LinkHandler
                                           , msg
                                           , SIRCConstants.SIRC0001_DELIVERY_ERROR
                                           , new String[] { frpDestination.getName() },
                                           tran);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "handleFRPMessage");

        return frpDestination;
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

                    throws SIIncorrectCallException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this,
                        tc,
                        "deliverOrderedMessages",
                        new Object[] {
                                      msgList,
                                      targetStream,
                                      new Integer(priority),
                                      reliability });

        Exception failingException = null;
        JsDestinationAddress failingDestination = null;

        //Create transaction
        TransactionCommon tran = _targetBatchHandler.registerInBatch();
        // A marker to indicate how far through the method we get.
        boolean messageAddCall = false;
        try
        {
            MessageItem msgItem = null;

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

                // Deliver all messages in list
                for (int i = 0; i < msgList.size(); i++)
                {
                    boolean resetRequired = true;

                    try
                    {
                        msgItem = (MessageItem) msgList.get(i);
                        currentEndTick = msgItem.getMessage().getGuaranteedValueEndTick();
                        JsDestinationAddress routingDestinationAddr =
                                        msgItem.getMessage().getRoutingDestination();

                        // If we find we have a deleted destination, we may need to perform
                        // a move to the system exception destination.
                        String deletedTargetDestination = null;
                        boolean sendToSystemExceptionDestination = false;

                        // If this message has a routingDestination we try to deliver the message
                        // there
                        if (routingDestinationAddr != null)
                        {
                            DestinationHandler routingDestination = null;
                            //
                            LinkHandler linkHandler = _isLink ? (LinkHandler) _destination : null;

                            try
                            {
                                // If this has come across a Link then override the
                                // userId if necessary and reset the link properties as
                                // we are now pretending this was produced on this ME
                                if (_isLink)
                                {
                                    String linkInboundUserid = ((LinkHandler) _destination).getInboundUserid();
                                    if (linkInboundUserid != null)
                                    {
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                            SibTr.debug(tc, "Set link inbound userid: " + linkInboundUserid + ", into message");
                                        // Call SIB.security (ultimately) to set inbounduserid into msg
                                        _messageProcessor.getAccessChecker().setSecurityIDInMessage(
                                                                                                    linkInboundUserid,
                                                                                                    msgItem.getMessage());
                                    }

                                    msgItem.getMessage().setGuaranteedCrossBusLinkName(null);
                                    msgItem.getMessage().setGuaranteedCrossBusSourceBusUUID(null);

                                }

                                // Find the routing destination.
                                // For links, we exclude invisible (deleted) destinations, so that
                                // standard link exception destination handling occurs if we find
                                // the requested destination doesn't exist, or has been deleted.
                                // For intra-bus comms, we include invisible (deleted) destinations,
                                // so that we do not attempt to call the exception destination
                                // handler with a deleted destination, and we honor the
                                // discard messages setting on the bus (see below).
                                routingDestination =
                                                _destinationManager.getDestination(
                                                                                   routingDestinationAddr,
                                                                                   !_isLink,
                                                                                   true); // create remote temp queue if required

                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    SibTr.debug(tc, "Routing dest to " + routingDestination);

                                // If this is coming in off a link then use the link's configuration
                                // to determine if any local message points should be preferred
                                if (_isLink)
                                {
                                    msgItem.setPreferLocal(linkHandler.preferLocalTargetQueuePoint());
                                }
                                // If it's come from another ME in the bus then they're sending it
                                // to this message point because they want to so we prefer the local one.
                                // This allows the mesasge to be re-routed if absolutely necessary.
                                else
                                    msgItem.setPreferLocal(true);

                                if (_isLink || !routingDestination.isToBeDeleted())
                                {
                                    // Call the handler to deal with the message
                                    // (If the routing address in the message was used for binding to
                                    // a particular ME then this inputHandler will probably be us)
                                    // (If the routing address in the messages was used for identifying
                                    // a scoped alias then we'll use the alias's input handler to restrict
                                    // the message points available to us, rather than all of them (which is
                                    // what this input handler will have access to - as we're the real destination)
                                    ProducerInputHandler inputHandler;
                                    inputHandler = (ProducerInputHandler) routingDestination.getInputHandler(ProtocolType.UNICASTINPUT, //always unicast
                                                                                                             null, //only needed for anycast, which is not the case here
                                                                                                             msgItem.getMessage());

                                    inputHandler.handleMessage(msgItem,
                                                               tran,
                                                               _messageProcessor.getMessagingEngineUuid());
                                }
                                else
                                {
                                    // For messages arriving within the bus, we might feasibly find
                                    // the routing destination has been deleted.
                                    // In that case we should honor the discard messages setting on
                                    // the bus and either route to the system exception destination
                                    // or discard the message.
                                    deletedTargetDestination = routingDestination.getName();
                                    sendToSystemExceptionDestination = !_messageProcessor.discardMsgsAfterQueueDeletion();

                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                        SibTr.debug(this, tc, "Message received from within bus for a deleted routing destination " + routingDestination +
                                                              (sendToSystemExceptionDestination ? ". Exceptioning message" : ". Discarding message"));
                                }
                            } catch (SITemporaryDestinationNotFoundException e)
                            {
                                // No FFDC code needed

                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                {
                                    SibTr.exception(tc, e);
                                    SibTr.debug(
                                                tc,
                                                "Discarding the message as Temporary destination not found");
                                }
                            } catch (SIMPNoLocalisationsException e)
                            {
                                // No FFDC code needed

                                // We can't find a suitable localisation.
                                // Although a queue must have at least one localisation this is
                                // possible if the sender restricted the potential localisations
                                // using a fixed ME or a scoping alias (to an out-of-date set of
                                // localisation)

                                failingException = e;
                                failingDestination = routingDestinationAddr;

                                //Put the message to the exception destination
                                handleUndeliverableMessage(
                                                           routingDestination,
                                                           linkHandler,
                                                           msgItem,
                                                           SIRCConstants.SIRC0026_NO_LOCALISATIONS_FOUND_ERROR,
                                                           new String[] { routingDestination.getName() },
                                                           tran);
                            } catch (SIMPLimitExceededException e)
                            {
                                // No FFDC code needed

                                // If this is a link and the target destination is unable to accept messages, then we'll attempt
                                // to put the message to the exception destination that belongs to the link.

                                failingException = e;
                                failingDestination = routingDestinationAddr;

                                // If the link exception destination is unable to accept the message, or if a null or empty
                                // exception destination has been configured for the link, then handleUndeliverableMessage() will
                                // throw an SIResourceException which will result in the message being discarded.
                                handleUndeliverableMessage(
                                                           null, // set the DH parameter to null, so that we use the LH exception destination
                                                           linkHandler,
                                                           msgItem,
                                                           e.getExceptionReason(), // Extract the exception reason and inserts from the
                                                           e.getExceptionInserts(), // SIMPLimitExceededException exception
                                                           tran);
                            } catch (SIMPSendAllowedException e)
                            {
                                // No FFDC code needed

                                // If the message came off an interbus link we should exception it (if
                                // possible) to get it out  of the way to allow other messages to be processed.
                                if (_isLink)
                                {
                                    // Putting message to exception destination as the destination is currently unavailable
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    {
                                        SibTr.debug(
                                                    tc,
                                                    "Putting message to exception destination as destination was send-disallowed");
                                        SibTr.exception(tc, e);
                                    }

                                    failingException = e;
                                    failingDestination = routingDestinationAddr;

                                    // There was nowhere to send the message, send it to the exception
                                    // destination instead
                                    handleUndeliverableMessage(
                                                               routingDestination,
                                                               linkHandler,
                                                               msgItem,
                                                               e.getExceptionReason(),
                                                               e.getExceptionInserts(),
                                                               tran);
                                }
                                // If this isn't a link then we have a routing destination because some
                                // other level of routing restriction has been applied (e.g. scoped Alias or
                                // fixed ME). If this case we should just rethrow the exception so that the
                                // stays on the target stream and is retried until it succeeds.
                                else
                                {
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                        SibTr.debug(
                                                    tc,
                                                    "Failing the message to keep retrying until the queue point becomes send-allowed");

                                    SibTr.exception(tc, e);
                                    throw e;
                                }
                            } catch (SINotPossibleInCurrentConfigurationException e)
                            {
                                // No FFDC code needed

                                // Putting message to exception destination as the destination is currently unavailable
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                {
                                    SibTr.debug(
                                                tc,
                                                "Putting message to exception destination as destination not found");
                                    SibTr.exception(tc, e);
                                }

                                failingException = e;
                                failingDestination = routingDestinationAddr;

                                // There was nowhere to send the message, send it to the exception
                                // destination instead
                                handleUndeliverableMessage(
                                                           routingDestination,
                                                           linkHandler,
                                                           msgItem,
                                                           e.getExceptionReason(),
                                                           e.getExceptionInserts(),
                                                           tran);
                            } catch (RMQResourceException e)
                            {
                                // No FFDC code needed

                                // Putting message to exception destination as the destination is currently unavailable
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                {
                                    SibTr.debug(
                                                tc,
                                                "Putting message to exception destination as RMQ destination was not available");
                                    SibTr.exception(tc, e);
                                }

                                failingException = e;
                                failingDestination = routingDestinationAddr;

                                // There was nowhere to send the message, send it to the exception
                                // destination instead
                                handleUndeliverableMessage(
                                                           routingDestination,
                                                           linkHandler,
                                                           msgItem,
                                                           e.getExceptionReason(),
                                                           e.getExceptionInserts(),
                                                           tran);
                            } catch (SIResourceException e)
                            {
                                // No FFDC code needed

                                // If this is a link and we got a Resource Exception when trying to put the message to the target
                                // destination, then we'll attempt to put the message to the exception destination that belongs
                                // to the link.
                                if (_isLink)
                                {
                                    // Putting message to link exception destination
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    {
                                        SibTr.debug(tc, "Putting message to link exception destination");
                                        SibTr.exception(tc, e);
                                    }

                                    failingException = e;
                                    failingDestination = routingDestinationAddr;

                                    handleUndeliverableMessage(
                                                               null, // set the DH parameter to null, so that we use the LH exc dest
                                                               linkHandler,
                                                               msgItem,
                                                               SIRCConstants.SIRC0026_NO_LOCALISATIONS_FOUND_ERROR,
                                                               new String[] { routingDestination.getName() },
                                                               tran);
                                } // eof _isLink
                                else
                                {
                                    // Not a link so re-throw the exception
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                        SibTr.debug(
                                                    tc,
                                                    "Failing the message after Resource exception");

                                    SibTr.exception(tc, e);
                                    throw e;
                                }
                            }

                        }
                        else
                        {
                            if (!_isLink)
                            {
                                //Put the message to the inputHandler so that if the local localisation
                                //is not available, the message can be "sideways-punted" to a different
                                //localisation
                                try
                                {
                                    if (!_destination.isToBeDeleted())
                                    {
                                        InputHandler inputHandler = null;

                                        //If the destination is a topicspace and we've been sent a pt-to-pt
                                        //message, then having accepted
                                        //the message, we should publish it.  To do this we must get the
                                        //pub-sub inputhandler for the destination
                                        if (_destination.isPubSub())
                                        {
                                            inputHandler = _destination.getInputHandler();
                                        }
                                        else
                                        {
                                            //Use this inputHandler.
                                            inputHandler = this;
                                        }

                                        inputHandler.handleMessage(
                                                                   msgItem,
                                                                   tran,
                                                                   _messageProcessor.getMessagingEngineUuid());
                                    }
                                    else
                                    {
                                        // The destination has been deleted, then we should honor the
                                        // discard messages setting on the bus and either route to the
                                        // system exception destination or discard the message.
                                        deletedTargetDestination = _destination.getName();
                                        sendToSystemExceptionDestination = !_messageProcessor.discardMsgsAfterQueueDeletion();

                                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                            SibTr.debug(this, tc, "Message received from within bus for a deleted destination. " +
                                                                  (sendToSystemExceptionDestination ? "Exceptioning message" : "Discarding message"));
                                    }

                                } catch (SIMPNoLocalisationsException e)
                                {
                                    // No FFDC code needed

                                    // We can't find a suitable localisation.
                                    // Although a queue must have at least one localisation this is
                                    // possible if the sender restricted the potential localisations
                                    // using a fixed ME or a scoping alias (to an out-of-date set of
                                    // localisation)

                                    //Put the message to the exception destination
                                    handleUndeliverableMessage(
                                                               _destination,
                                                               null, // not a link
                                                               msgItem,
                                                               SIRCConstants.SIRC0026_NO_LOCALISATIONS_FOUND_ERROR,
                                                               new String[] { _destination.getName() },
                                                               tran);
                                } catch (SIMPSendAllowedException e)
                                {
                                    // No FFDC code needed

                                    // The quee point is current send disallowed, we don't want to
                                    // exception it, instead we leave it 'failing' on the target stream
                                    // so it gets retried periodically (on ackExpected/Nack processing)
                                    // in case the QP is re-enabled.

                                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                                        SibTr.exception(tc, e);

                                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                        SibTr.exit(tc, "deliverOrderedMessages", e);

                                    throw e;
                                } catch (SIMPNotPossibleInCurrentConfigurationException e)
                                {
                                    // No FFDC code needed

                                    // There's a problem with the configuration so we can't process this
                                    // message, we better exception it instead.

                                    //Put the message to the exception destination
                                    handleUndeliverableMessage(
                                                               _destination,
                                                               null, // not a link
                                                               msgItem,
                                                               SIRCConstants.SIRC0026_NO_LOCALISATIONS_FOUND_ERROR,
                                                               new String[] { _destination.getName() },
                                                               tran);
                                }
                            }
                            // This is a link but there was no routing destination!!
                            else
                            {
                                SIErrorException e = new SIErrorException(
                                                nls.getFormattedMessage(
                                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                                        new Object[] {
                                                                                      "com.ibm.ws.sib.processor.impl.PtoPInputHandler",
                                                                                      "1:2561:1.323" },
                                                                        null));

                                failingException = e;
                                failingDestination = null;

                                FFDCFilter.processException(
                                                            e,
                                                            "com.ibm.ws.sib.processor.impl.PtoPInputHandler.deliverOrderedMessages",
                                                            "1:2570:1.323",
                                                            this);

                                SibTr.exception(tc, e);

                                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                    SibTr.exit(tc, "deliverOrderedMessages", e);

                                throw e;
                            }
                        }

                        // If we need to send the message to the system exception destination,
                        // do so here.
                        if (sendToSystemExceptionDestination)
                        {
                            ExceptionDestinationHandlerImpl exceptionDestinationHandlerImpl =
                                            (ExceptionDestinationHandlerImpl) _messageProcessor.createExceptionDestinationHandler(null);
                            final UndeliverableReturnCode rc =
                                            exceptionDestinationHandlerImpl.handleUndeliverableMessage(msgItem,
                                                                                                       tran,
                                                                                                       SIRCConstants.SIRC0032_DESTINATION_DELETED_ERROR,
                                                                                                       new String[] { deletedTargetDestination,
                                                                                                                     _messageProcessor.getMessagingEngineName() });
                            if (rc != UndeliverableReturnCode.OK && rc != UndeliverableReturnCode.DISCARD)
                            {
                                // We cannot put the message to the exception destination.
                                // Throw a suitable exception and trace+FFST.
                                if (rc == UndeliverableReturnCode.BLOCK)
                                {
                                    SIResourceException e =
                                                    new SIResourceException(nls.getFormattedMessage("INTERNAL_MESSAGING_ERROR_CWSIP0005",
                                                                                                    new Object[] {
                                                                                                                  "com.ibm.ws.sib.processor.impl.PtoPInputHandler",
                                                                                                                  "1:2605:1.323", rc }, null));

                                    FFDCFilter.processException(e, "com.ibm.ws.sib.processor.impl.PtoPInputHandler.deliverOrderedMessages",
                                                                "1:2608:1.323", this);

                                    SibTr.exception(tc, e);

                                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                        SibTr.exit(tc, "deliverOrderedMessages", e);

                                    throw e;
                                }
                                else
                                {
                                    SIErrorException e =
                                                    new SIErrorException(nls.getFormattedMessage("INTERNAL_MESSAGING_ERROR_CWSIP0005",
                                                                                                 new Object[] {
                                                                                                               "com.ibm.ws.sib.processor.impl.PtoPInputHandler",
                                                                                                               "1:2623:1.323", rc }, null));

                                    FFDCFilter.processException(e, "com.ibm.ws.sib.processor.impl.PtoPInputHandler.deliverOrderedMessages",
                                                                "1:2626:1.323", this);

                                    SibTr.exception(tc, e);

                                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                        SibTr.exit(tc, "deliverOrderedMessages", e);

                                    throw e;
                                }
                            }
                        }

                        lastSuccessfulMsg = i;
                        lastSuccessfulEndTick = currentEndTick;
                        resetRequired = false;
                    } catch (SIResourceException e)
                    {
                        // No FFDC code needed

                        // We've failed to process a message, we can't carry on with this
                        // list of messages, instead we move the doubt horizon to point to
                        // the failed message so that we don't accidently ack it if someone asks
                        targetStream.resetDoubtHorizon(currentEndTick, failingException, failingDestination);
                        resetRequired = false;

                        // then break out of the for loop to allow any successful work to be added
                        // to the batch.
                        break;
                    } finally
                    {
                        // This finally block is to intercept all unexpected errors and clean up.

                        // We've failed to process a message, we can't carry on with this
                        // list of messages, instead we move the doubt horizon to point to
                        // the failed message so that we don't accidently ack it if someone asks
                        if (resetRequired)
                            targetStream.resetDoubtHorizon(currentEndTick, failingException, failingDestination);
                    }
                }

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
                                      ExpressTargetStream expressTargetStream)

                    throws SIIncorrectCallException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deliverExpressMessage", new Object[] { msgItem });

        //Create transaction
        LocalTransaction siTran = _txManager.createLocalTransaction(false);

        try
        {
            long endTick = msgItem.getMessage().getGuaranteedValueValueTick();

            JsDestinationAddress routingDestinationAddr =
                            msgItem.getMessage().getRoutingDestination();
            if (routingDestinationAddr != null)
            {
                DestinationHandler routingDestination = null;
                try
                {
                    routingDestination =
                                    _destinationManager.getDestination(routingDestinationAddr, false, true);

                    if (_isLink)
                    {
                        LinkHandler link = (LinkHandler) _destination;

                        // If this is coming in off a link then use the link's configuration
                        // to determine if any local message points should be preferred
                        msgItem.setPreferLocal(link.preferLocalTargetQueuePoint());

                        String linkInboundUserid = ((LinkHandler) _destination).getInboundUserid();
                        if (linkInboundUserid != null)
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(tc, "Set link inbound userid: " + linkInboundUserid + ", into message");
                            // Call SIB.security (ultimately) to set inbounduserid into msg
                            _messageProcessor.getAccessChecker().setSecurityIDInMessage(
                                                                                        linkInboundUserid,
                                                                                        msgItem.getMessage());
                        }
                    }
                    // If it's come from another ME in the bus then they're sending it
                    // to this message point because they want to so we prefer the local one.
                    // This allows the mesasge to be re-routed if absolutely necessary.
                    else
                        msgItem.setPreferLocal(true);

                    // Call the handler to deal with the message - passing null as the producer
                    // session id.
                    ProducerInputHandler inputHandler =
                                    (ProducerInputHandler) routingDestination.getInputHandler();

                    inputHandler.handleMessage(msgItem,
                                               siTran,
                                               _messageProcessor.getMessagingEngineUuid());

                } catch (SIException e)
                {
                    // No FFDC code needed

                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    {
                        SibTr.exception(tc, e);
                        SibTr.debug(tc, "Discarding the express message as it could not be from the targetStreamManager");
                    }
                }
            }
            else
            {
                ConsumerDispatcher consumerDispatcher;

                consumerDispatcher = (ConsumerDispatcher) _destination.getLocalPtoPConsumerManager();
                consumerDispatcher.put(msgItem, siTran, null, false);
            }

            // Update the completedPrefix on the stream
            // This will put all ticks before this point into Completed state
            expressTargetStream.setCompletedPrefix(endTick);

            siTran.commit();
        } catch (SIIncorrectCallException e)
        {
            // No FFDC code needed
            handleRollback(siTran);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "deliverExpressMessage", e);

            throw e;
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
                                        "com.ibm.ws.sib.processor.impl.PtoPInputHandler.deliverExpressMessage",
                                        "1:2840:1.323",
                                        this);

            handleRollback(siTran);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "deliverExpressMessage", e);
            }

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deliverExpressMessage");
    }

    @Override
    public void sendAreYouFlushedMessage(SIBUuid8 sourceMEUuid,
                                         SIBUuid12 destUuid,
                                         SIBUuid8 busUuid,
                                         long queryID,
                                         SIBUuid12 streamID)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendAreYouFlushedMessage",
                        new Object[] { new Long(queryID), streamID });
        ControlAreYouFlushed flushQuery = createControlAreYouFlushed();

        // As we are using the Guaranteed Header - set all the attributes as
        // well as the ones we want.
        SIMPUtils.setGuaranteedDeliveryProperties(flushQuery,
                                                  _messageProcessor.getMessagingEngineUuid(),
                                                  sourceMEUuid,
                                                  streamID,
                                                  null,
                                                  destUuid,
                                                  ProtocolType.UNICASTOUTPUT,
                                                  GDConfig.PROTOCOL_VERSION);

        flushQuery.setPriority(SIMPConstants.CTRL_MSG_PRIORITY);
        flushQuery.setReliability(Reliability.ASSURED_PERSISTENT);

        flushQuery.setRequestID(queryID);

        sendToME(flushQuery, sourceMEUuid, busUuid, SIMPConstants.MSG_HIGH_PRIORITY);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendAreYouFlushedMessage");
    }

    @Override
    public void sendRequestFlushMessage(SIBUuid8 sourceMEUuid,
                                        SIBUuid12 destUuid,
                                        SIBUuid8 busUuid,
                                        long queryID,
                                        SIBUuid12 streamID,
                                        boolean indoubtDiscard)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendRequestFlushMessage",
                        new Object[] { new Long(queryID), streamID });
        ControlRequestFlush flushRequest = createControlRequestFlush();

        // As we are using the Guaranteed Header - set all the attributes as
        // well as the ones we want.
        SIMPUtils.setGuaranteedDeliveryProperties(flushRequest,
                                                  _messageProcessor.getMessagingEngineUuid(),
                                                  sourceMEUuid,
                                                  streamID,
                                                  null,
                                                  destUuid,
                                                  ProtocolType.UNICASTOUTPUT,
                                                  GDConfig.PROTOCOL_VERSION);

        flushRequest.setPriority(SIMPConstants.CTRL_MSG_PRIORITY);
        flushRequest.setReliability(Reliability.ASSURED_PERSISTENT);

        flushRequest.setRequestID(queryID);
        flushRequest.setIndoubtDiscard(indoubtDiscard);

        sendToME(flushRequest, sourceMEUuid, busUuid, SIMPConstants.MSG_HIGH_PRIORITY);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendRequestFlushMessage");
    }

    private void checkHandlerAvailable(SIBUuid8 fixedMEUuid)
                    throws SIResourceException,
                    SINotPossibleInCurrentConfigurationException,
                    SIMPLimitExceededException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkHandlerAvailable", new Object[] { this, fixedMEUuid });

        int reason = _destination.checkPtoPOutputHandlers(fixedMEUuid, null);

        switch (reason)
        {
            case (DestinationHandler.OUTPUT_HANDLER_FOUND):
                break;
            case (DestinationHandler.OUTPUT_HANDLER_SEND_ALLOWED_FALSE): {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(
                               tc,
                               "checkHandlerAvailable",
                               "Destination send disallowed");

                SIMPSendAllowedException e =
                                new SIMPSendAllowedException(
                                                nls_cwsik.getFormattedMessage(
                                                                              "DELIVERY_ERROR_SIRC_24", // DESTINATION_SEND_DISALLOWED_CWSIP0252
                                                                              new Object[] { _destination.getName(),
                                                                                            _messageProcessor.getMessagingEngineName() },
                                                                              null));

                e.setExceptionReason(SIRCConstants.SIRC0024_DESTINATION_SEND_DISALLOWED);
                e.setExceptionInserts(new String[] { _destination.getName(),
                                                    _messageProcessor.getMessagingEngineName() });
                throw e;
            }
            case (DestinationHandler.OUTPUT_HANDLER_ALL_HIGH_LIMIT): {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(
                               tc,
                               "checkHandlerAvailable",
                               "Destination reached high limit");
                //117505
                long destHighMsg = _destination.getQHighMsgDepth();

                SibTr.info(tc, "NOTIFY_DEPTH_THRESHOLD_REACHED_CWSIP0553",
                           new Object[] { _destination.getName(), _messageProcessor.getMessagingEngineName(), destHighMsg });
                //117505

                SIMPLimitExceededException e = new SIMPLimitExceededException(
                                nls_cwsik.getFormattedMessage(
                                                              "DELIVERY_ERROR_SIRC_25", // DESTINATION_HIGH_MESSAGES_ERROR_CWSIP0250
                                                              new Object[] { _destination.getName(),
                                                                            _messageProcessor.getMessagingEngineName() }, null));

                e.setExceptionReason(SIRCConstants.SIRC0025_DESTINATION_HIGH_MESSAGES_ERROR);
                e.setExceptionInserts(new String[] { _destination.getName(),
                                                    _messageProcessor.getMessagingEngineName() });
                throw e;
            }
            case (DestinationHandler.OUTPUT_HANDLER_NOT_FOUND): {
                // We can't find a suitable localisation.
                // Although a queue must have at least one localisation this is
                // possible if the sender restricted the potential localisations
                // using a fixed ME or a scoping alias (to an out-of-date set of
                // localisation)

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(
                               tc,
                               "checkHandlerAvailable",
                               "SIMPNoLocalisationsException");

                SIMPNoLocalisationsException e = new SIMPNoLocalisationsException(
                                nls_cwsik.getFormattedMessage(
                                                              "DELIVERY_ERROR_SIRC_26", // NO_LOCALISATIONS_FOUND_ERROR_CWSIP0032
                                                              new Object[] { _destination.getName() }, null));

                e.setExceptionReason(SIRCConstants.SIRC0026_NO_LOCALISATIONS_FOUND_ERROR);
                e.setExceptionInserts(new String[] { _destination.getName() });
                throw e;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkHandlerAvailable");
        return;
    }

    /**
     * <p>Put the undeliverable message to the exception destination</p>
     * If this is a link, then the linkHandler parameter offers the ability to work
     * with the exception destination associated with it.
     * 
     * @param destinationHandler
     * @param linkHandler
     * @param msg
     * @param exceptionReason
     * @param exceptionInserts
     * @return
     * @throws SIStoreException
     */
    private void handleUndeliverableMessage(
                                            DestinationHandler destinationHandler,
                                            LinkHandler linkHandler,
                                            SIMPMessage msg,
                                            int exceptionReason,
                                            String[] exceptionInserts,
                                            TransactionCommon tran) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "handleUndeliverableMessage",
                        new Object[] { destinationHandler, linkHandler, msg, new Integer(exceptionReason), exceptionInserts,
                                      tran });

        // Destination exception destination handler
        ExceptionDestinationHandlerImpl destExceptionDestination = null;
        // Link exception destination handler
        ExceptionDestinationHandlerImpl linkExceptionDestination = null;

        // Create handlers associated with the destination and link handlers
        if (destinationHandler == null)
        {
            if (linkHandler == null)
            {
                // Where both handlers are null retain the behaviour that uses the default exception destination.
                destExceptionDestination = new ExceptionDestinationHandlerImpl(null, _messageProcessor);
            }
            // if the linkHandler is non null, then we'll establish a linkExceptionDestination below
        }
        else
        {
            // destinationHandler is non null
            destExceptionDestination = new ExceptionDestinationHandlerImpl(destinationHandler);
        } // eof destinationHandler != null

        UndeliverableReturnCode rc = UndeliverableReturnCode.OK;
        // Pass the undeliverable message to the destination exception destination if it is not null
        if (destExceptionDestination != null)
        {
            rc =
                            destExceptionDestination.handleUndeliverableMessage(
                                                                                msg,
                                                                                tran,
                                                                                exceptionReason,
                                                                                exceptionInserts);
        }

        // If no destination exception destination was established or if we got a BLOCK return from the use
        // of the destination exception destination, then we drive the link exception destination
        if (destExceptionDestination == null || rc == UndeliverableReturnCode.BLOCK)
        {
            // Get the Link Exception Destination
            if (linkHandler != null)
            {
                linkExceptionDestination = new ExceptionDestinationHandlerImpl(linkHandler);

                rc =
                                linkExceptionDestination.handleUndeliverableMessage(
                                                                                    msg,
                                                                                    tran,
                                                                                    exceptionReason,
                                                                                    exceptionInserts);
            }
        }

        // If the ExceptionDestinationHandlerImpl.handleUndeliverableMessage() call returned either a
        // "BLOCK" or an "ERROR" return then we throw an exception. BLOCK will be returned if the exception
        // destination is full or if a null or empty exception destination was configured.
        //
        // In the error case we generate an FFDC but in both cases we throw an exception to get the caller to
        // fail the attempt to store the message, forcing it to either return the error or retry at it's discretion.
        if (rc == UndeliverableReturnCode.BLOCK)
        {
            // Throw an exception
            SIResourceException e = new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.PtoPInputHandler",
                                                                  "1:3103:1.323",
                                                                  rc },
                                                    null));

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "handleUndeliverableMessage", e);

            throw e;
        }
        // An ERROR just isn't acceptable
        else if (rc == UndeliverableReturnCode.ERROR)
        {
            //We cannot put the message to the exception destination.  Throw an
            //exception and trace FFST.
            SIErrorException e = new SIErrorException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.PtoPInputHandler",
                                                                  "1:3124:1.323",
                                                                  rc },
                                                    null));

            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.PtoPInputHandler.handleUndeliverableMessage",
                                        "1:3131:1.323",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "handleUndeliverableMessage", e);

            throw e;
        }
        // otherwise a DISCARD is equivalent to an OK so let it go

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "handleUndeliverableMessage");

        return;

    }

    /**
     * Check whether it will be possible to place a message on the exception destination belonging
     * to a destination.
     * 
     * @param destinationHandler
     * @return an integer reason code
     */
    private int checkCanExceptionMessage(DestinationHandler destinationHandler)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "checkCanExceptionMessage",
                        new Object[] { destinationHandler });

        // Destination exception destination handler
        ExceptionDestinationHandlerImpl exceptionDestinationHandler = null;

        // Instantiate an ExceptionDestinationHandlerImpl for the destination
        exceptionDestinationHandler = new ExceptionDestinationHandlerImpl(destinationHandler);

        int returnValue = exceptionDestinationHandler.checkCanExceptionMessage();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkCanExceptionMessage", Integer.valueOf(returnValue));

        return returnValue;
    }

    /**
     * <p>Check authority to produce to a destination</p>
     * 
     * @param destinationHandler
     * @param msg
     * @param exceptionReason
     * @param exceptionInserts
     * @return
     * @throws SIStoreException
     */
    private void checkDestinationAccess(DestinationHandler destination,
                                        SecurityContext secContext) throws SIDiscriminatorSyntaxException, SINotAuthorizedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "checkDestinationAccess",
                        new Object[] { destination, secContext });

        // Check authority to access the destination
        if (!destination.
                        checkDestinationAccess(secContext,
                                               OperationType.SEND))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkDestinationAccess", "not authorized to produce to this destination");

            // Get the username
            String userName = secContext.getUserName(true);

            // Build the message for the Exception and the Notification
            String nlsMessage =
                            nls_cwsik.getFormattedMessage("DELIVERY_ERROR_SIRC_18", // USER_NOT_AUTH_SEND_ERROR_CWSIP0306
                                                          new Object[] { destination.getName(),
                                                                        userName },
                                                          null);

            // Fire a Notification if Eventing is enabled
            _messageProcessor.
                            getAccessChecker().
                            fireDestinationAccessNotAuthorizedEvent(destination.getName(),
                                                                    userName,
                                                                    OperationType.SEND,
                                                                    nlsMessage);

            // Thrown if user denied access to destination
            SIMPNotAuthorizedException e = new SIMPNotAuthorizedException(nlsMessage);

            e.setExceptionReason(SIRCConstants.SIRC0018_USER_NOT_AUTH_SEND_ERROR);
            e.setExceptionInserts(new String[] { destination.getName(),
                                                secContext.getUserName(true) });
            throw e;
        }

        // Check authority to produce to discriminator
        if (!destination.
                        checkDiscriminatorAccess(secContext,
                                                 OperationType.SEND))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "checkDestinationAccess", "not authorized to produce to this discriminator");

            // Write an audit record if access is denied
            SibTr.audit(tc, nls_cwsik.getFormattedMessage(
                                                          "DELIVERY_ERROR_SIRC_20", // USER_NOT_AUTH_SEND_ERROR_CWSIP0308
                                                          new Object[] { destination.getName(),
                                                                        secContext.getDiscriminator(),
                                                                        secContext.getUserName(true) },
                                                          null));

            // Thrown if user denied access to discriminator
            SIMPNotAuthorizedException e = new SIMPNotAuthorizedException(
                            nls_cwsik.getFormattedMessage(
                                                          "DELIVERY_ERROR_SIRC_20", // USER_NOT_AUTH_SEND_ERROR_CWSIP0308
                                                          new Object[] { destination.getName(),
                                                                        secContext.getDiscriminator(),
                                                                        secContext.getUserName(true) },
                                                          null));

            e.setExceptionReason(SIRCConstants.SIRC0020_USER_NOT_AUTH_SEND_ERROR);
            e.setExceptionInserts(new String[] { destination.getName(),
                                                secContext.getDiscriminator(),
                                                secContext.getUserName(true) });
            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkDestinationAccess");
    }

    private void traceSend(MessageItem message)
    {
        if (message.getMessage().isApiMessage())
        {
            String text = null;
            text = "PRODUCER_SEND_CWSJU0054";

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

            if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
            {
                SibTr.debug(UserTrace.tc_mt,
                            nls_mt.getFormattedMessage(
                                                       text,
                                                       new Object[] {
                                                                     apiMsgId,
                                                                     message.getMessage().getSystemMessageId(),
                                                                     correlationId,
                                                                     _destination.getName() },
                                                       null));
            }
        }
    }

    @Override
    public String toString()
    {
        return "PtoPInputHandler to Destination " + _destination.getName();
    }

    /**
     * Ensure all target streams are flushed. This is done
     * in preparation for deleting the destination. If the
     * delete is deferred, then this code will autormatically
     * redrive delete when possible.
     * 
     * @return true if the stream is flushed and the delete
     *         can continue, otherwise the delete is deferred until
     *         the flush completes.
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
            if (_flushedForDeleteTarget)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "flushAllForDelete", new Boolean(true));
                return true;
            }

            // Short circuit if flush already in progress
            if (_deleteFlushTarget != null)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "flushAllForDelete", new Boolean(false));
                return false;
            }

            // If we're flushable now then return, otherwise send the query
            // and start an alarm
            if (_targetStreamManager.isEmpty())
            {
                _flushedForDeleteTarget = true;
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "flushAllForDelete", new Boolean(true));
                return true;
            }

            // Otherwise, send out the initial query, and set a retry alarm
            _deleteFlushTarget = new AlarmListener()
            {
                @Override
                public void alarm(Object al)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.entry(tc, "alarm", al);
                    PtoPInputHandler ptIH = (PtoPInputHandler) al;
                    if (ptIH._targetStreamManager.isEmpty())
                    {
                        // Flush finished
                        synchronized (ptIH)
                        {
                            ptIH._flushedForDeleteTarget = true;
                            ptIH._deleteFlushTarget = null;
                        }

                        // Now redrive the actual deletion
                        ptIH._messageProcessor.getDestinationManager().startAsynchDeletion();
                    }
                    else
                    {
                        // Query the flush again
                        try
                        {
                            ptIH._targetStreamManager.queryUnflushedStreams();
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                                SibTr.event(tc, "Querying target for PtoP flush on destination: " + _destination.getName());
                            ptIH._messageProcessor.getAlarmManager().create(SIMPConstants.LOG_DELETED_FLUSH_WAIT, this, ptIH);
                        }
                        catch (SIResourceException e)
                        {
                            // This shouldn't actually be possible so log it
                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.PtoPInputHandler.alarm",
                                                        "1:3423:1.323",
                                                        this);

                            SibTr.exception(tc, e);

                            // There's no one to catch this exception so eat it.  Note that this also
                            // kills the flush retry.
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                                SibTr.event(tc, "Target flushed cancelled by SIResourceException");
                        }
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "alarm");
                    }
                }
            };

            _targetStreamManager.queryUnflushedStreams();
            _messageProcessor.getAlarmManager().create(SIMPConstants.LOG_DELETED_FLUSH_WAIT, _deleteFlushTarget, this);

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "flushAllForDelete", new Boolean(false));

        return false;
    }

    /**
     * @see com.ibm.ws.sib.processor.impl.interfaces.MessageDeliverer#checkAbleToAcceptMessage
     * 
     *      See if the destination or, if a link, the link's exception destination, can
     *      handle any more messages. If it cannot then we report that back to the caller.
     * 
     *      WARNING
     *      =======
     *      Once a stream is full it should not be considered not_full until
     *      the stream has reduced the backlog a bit. Therefore there is some
     *      hysteresis in the switching of destinations from full to not_full
     * @param routingDestinationAddr the destination address if this is a link.
     *            If this is not a link then this parameter will be null.
     * 
     * @return reason code signifying whether the destination can accept messages or the reason for
     *         not being able to do so.
     */
    @Override
    public int checkAbleToAcceptMessage(JsDestinationAddress routingDestinationAddr) throws SIException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkAbleToAcceptMessage", routingDestinationAddr);

        // Anything that isn't a link is let through as we would expect WLM to prevent
        // incoming messages once we're blocked (give or take a little latency)

        // Set the reason to OUTPUT_HANDLER_FOUND
        int blockingReason = DestinationHandler.OUTPUT_HANDLER_FOUND;

        //first we try to check if there is room on the actual destination
        //at the end of the link
        if (_isLink)
        {
            blockingReason = DestinationHandler.OUTPUT_HANDLER_NOT_FOUND;

            if (routingDestinationAddr != null)
            {
                String destName = routingDestinationAddr.getDestinationName();
                //always allow system or temporary destinations through
                if (destName.startsWith(SIMPConstants.TEMPORARY_QUEUE_DESTINATION_PREFIX) ||
                    destName.startsWith(SIMPConstants.TEMPORARY_PUBSUB_DESTINATION_PREFIX) ||
                    destName.startsWith(SIMPConstants.SYSTEM_DESTINATION_PREFIX))
                {
                    blockingReason = DestinationHandler.OUTPUT_HANDLER_FOUND;
                }

                //we need to look at the routing (target) destination first, to see if it's blocked
                else
                {
                    boolean checkedTarget = false;
                    try
                    {
                        // Can the routing destination or its exception destination accept a message
                        blockingReason =
                                        checkTargetAbleToAcceptOrExceptionMessage(routingDestinationAddr);

                        // We've checked the target and have a return code
                        checkedTarget = true;
                    } catch (SIMPNotPossibleInCurrentConfigurationException e)
                    {
                        // No FFDC code needed

                        // There's a problem with the configuration of the target destination - so set checkedTarget to
                        // true, retain the OUTPUT_HANDLER_NOT_FOUND return code and continue processing. If we find that
                        // the link exception destination is also unable to accept the message then we want to report
                        // the target blocking reason rather than the link blocking reason, which may be different.
                        checkedTarget = true;

                        // Log the exception
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.exception(tc, e);
                    } catch (SIException e)
                    {
                        FFDCFilter.processException(
                                                    e,
                                                    "com.ibm.ws.sib.processor.impl.PtoPInputHandler.checkAbleToAcceptMessage",
                                                    "1:3527:1.323",
                                                    this);

                        //There are many reasons why this destination lookup might have failed
                        //e.g. destination is not local. Therefore we log this exception and
                        //continue processing.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        {
                            SibTr.exception(tc, e);
                        }
                    }

                    // If we are not able to put messages to the target or its exception destination then
                    // we want to go on & see if we can put to the link's exception destination. Once more,
                    // the intent is to keep the link open for as long as possible
                    if (blockingReason != DestinationHandler.OUTPUT_HANDLER_FOUND)
                    {
                        int linkBlockingReason = checkLinkAbleToExceptionMessage();

                        // If we can exception the message then reset the blockingReason return code
                        if (linkBlockingReason == DestinationHandler.OUTPUT_HANDLER_FOUND)
                            blockingReason = DestinationHandler.OUTPUT_HANDLER_FOUND;
                        // If we didn't get a reason code from checking the target or its exception destination
                        // then use the link's.
                        else if (!checkedTarget)
                            blockingReason = linkBlockingReason;
                    }

                    // If the link is blocked we keep track of the target destination because it is
                    // responsible for blocking the link. We'll use this to support the freeing up of the
                    // link at a later stage.
                    if (blockingReason != DestinationHandler.OUTPUT_HANDLER_FOUND)
                    {
                        _linkBlockingDestination = routingDestinationAddr;
                    }

                } // eof else if (! system | temp)
            } // end null routing addr
        } // eof _isLink

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkAbleToAcceptMessage", Integer.valueOf(blockingReason));

        return blockingReason;
    }

    /**
     * See if a target destination or, if necessary, its exception destination, can
     * handle any more messages.
     * 
     * @return reason code signifying whether the destination can accept messages or the reason for
     *         not being able to do so.
     * @throws SINotPossibleInCurrentConfigurationException
     * @throws SIResourceException
     * @throws SITemporaryDestinationNotFoundException
     */
    private int checkTargetAbleToAcceptOrExceptionMessage(JsDestinationAddress targetDestinationAddr)
                    throws SITemporaryDestinationNotFoundException, SIResourceException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkTargetAbleToAcceptOrExceptionMessage", targetDestinationAddr);

        int blockingReason = DestinationHandler.OUTPUT_HANDLER_NOT_FOUND;

        // If the original routingDestination address in the message was blank we simply return
        // 'not found'.
        if (targetDestinationAddr != null)
        {
            // Lookup the routing (target) destination. This may throw a SIMPNotPossibleInCurrentConfigurationException.
            DestinationHandler targetDestination =
                            _messageProcessor.getDestinationManager().getDestination(targetDestinationAddr, false);
            SIBUuid8 targetDestinationMEUuid = targetDestinationAddr.getME();

            // Can the routing destination accept a message
            blockingReason = targetDestination.checkCanAcceptMessage(targetDestinationMEUuid, null);

            // If the target is full (or put-disabled, etc) then we want to go on & see if we can
            // put to the exception destination of the target destination
            if (blockingReason != DestinationHandler.OUTPUT_HANDLER_FOUND)
            {
                int linkBlockingReason = checkCanExceptionMessage(targetDestination);

                // If we can exception the message then reset the blockingReason return code
                if (linkBlockingReason == DestinationHandler.OUTPUT_HANDLER_FOUND)
                    blockingReason = DestinationHandler.OUTPUT_HANDLER_FOUND;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkTargetAbleToAcceptOrExceptionMessage", Integer.valueOf(blockingReason));

        return blockingReason;
    }

    /**
     * See if a link's exception destination can handle any more messages.
     * 
     * @return reason code signifying whether the destination can accept messages or the reason for
     *         not being able to do so.
     */
    private int checkLinkAbleToExceptionMessage()
                    throws SIException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkLinkAbleToExceptionMessage");

        int blockingReason = DestinationHandler.OUTPUT_HANDLER_NOT_FOUND;

        blockingReason = checkCanExceptionMessage(_destination);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkLinkAbleToExceptionMessage", Integer.valueOf(blockingReason));

        return blockingReason;
    }

    /**
     * See if the condition that led to the link being blocked has been resolved. The block could have been
     * caused by a number of factors, such as the routing destination being full or put-disabled or the
     * link exception destination being full, etc, etc.
     * 
     * This code is called by the processAckExpected code in the target stream
     * in order to determine whether NACKs can be sent.
     * 
     * WARNING - specific to stream full case
     * =======
     * Once a stream is full it should not be considered not_full until the stream has reduced the
     * backlog a bit. Therefore there is some hysteresis in the switching of destinations from full
     * to not_full.
     * 
     * @return true if the destination or link is unable to accept messages.
     */
    @Override
    public int checkStillBlocked()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkStillBlocked");

        // For safety, we assume that the destination is blocked (def 244425 and 464463)
        int blockingReason = DestinationHandler.OUTPUT_HANDLER_NOT_FOUND;

        if (!_isLink)
        {
            // For non-links we want to process ackExpecteds as normal
            blockingReason = DestinationHandler.OUTPUT_HANDLER_FOUND;
        }
        else
        {
            // If this is a link then we are potentially interested in a number of destinations: the destination
            // currently blocking the link, its exception destination, the link's exception destination and maybe
            // the system exception destination. For example, if no link exception destination is defined and a
            // message could not be delivered on either the target destination or the target's exception destination,
            // then the entire link will be blocked until that message can be delivered or has been deleted at the
            // source end.
            //
            // If a link does have an exception destination defined then there are situations where that exception
            // destination itself may be unable to accept messages and will therefore lead to the link being blocked.
            // If the condition that led to the blocking of the link no longer applies and if no other blocking
            // condition has arisen then we can start sending NACKs to the source as there will be space for what
            // might be returned. If it still cannot accept messages then we are still not able to send NACKs.

            // If the link was blocked because the routing destination or the configured exception destinations
            // were full, then check that they now have room
            boolean checkedTarget = false;
            try
            {
                // Do a general message acceptance test on the link blocking destination and any associated
                // exception destination if the link is still marked as blocked.
                blockingReason =
                                checkTargetAbleToAcceptOrExceptionMessage(_linkBlockingDestination);

                // We've checked the target and have a return code
                checkedTarget = true;
            } catch (SIMPNotPossibleInCurrentConfigurationException e)
            {
                // No FFDC code needed

                // There's a problem with the configuration of the target destination - so set checkedTarget to
                // true, retain the OUTPUT_HANDLER_NOT_FOUND return code and continue processing. If we find that
                // the link exception destination is also unable to accept the message then we want to report
                // the target blocking reason rather than the link blocking reason, which may be different.
                checkedTarget = true;

                // Log the exception
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.exception(tc, e);
            } catch (SIException e)
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.PtoPInputHandler.checkStillBlocked",
                                            "1:3720:1.323",
                                            this);
            }

            // If still blocked, then are we able to exploit the link exception destination
            try
            {
                if (blockingReason != DestinationHandler.OUTPUT_HANDLER_FOUND)
                {
                    int linkBlockingReason = checkLinkAbleToExceptionMessage();

                    // If we can exception the message then reset the blockingReason return code
                    if (linkBlockingReason == DestinationHandler.OUTPUT_HANDLER_FOUND)
                        blockingReason = DestinationHandler.OUTPUT_HANDLER_FOUND;
                    // If we didn't get a reason code from checking the target or its exception destination
                    // then use the link's.
                    else if (!checkedTarget)
                        blockingReason = linkBlockingReason;
                }
            } catch (SIException e)
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.PtoPInputHandler.checkStillBlocked",
                                            "1:3745:1.323",
                                            this);
            }

            // If the link is no longer blocked, clear out the blocking destination
            if (blockingReason == DestinationHandler.OUTPUT_HANDLER_FOUND)
                _linkBlockingDestination = null;
        } // eof processing specific to link

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkStillBlocked", new Integer(blockingReason));
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

        if (_isLink)
        {
            //A gap starting at sequence id {0} in the message stream from bus {1} on link {2} has been detected on messaging engine {3}.
            SibTr.info(tc, "UNRESOLVED_GAP_IN_LINK_TRANSMITTER_CWSIP0790",
                       new Object[] { (new Long(gap)).toString(),
                                     ((LinkHandler) _destination).getBusName(),
                                     _destination.getName(),
                                     _messageProcessor.getMessagingEngineName() });
        }
        else
        {
            //A gap starting at sequence id {0} in the message stream for destination {1} from messaging engine {2} has been detected on messaging engine {3}.
            SibTr.info(tc, "UNRESOLVED_GAP_IN_DESTINATION_TRANSMITTER_CWSIP0792",
                       new Object[] { (new Long(gap)).toString(),
                                     _destination.getName(),
                                     SIMPUtils.getMENameFromUuid(sourceMEUuid),
                                     _messageProcessor.getMessagingEngineName() });
        }

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

        if (_isLink)
        {
            //The gap starting at sequence id {0} in the message stream from bus {1} on link {2} has been resolved on message engine {3}.
            SibTr.info(tc, "RESOLVED_GAP_IN_LINK_TRANSMITTER_CWSIP0791",
                       new Object[] { (new Long(filledGap)).toString(),
                                     ((LinkHandler) _destination).getBusName(),
                                     _destination.getName(),
                                     _messageProcessor.getMessagingEngineName() });
        }
        else
        {
            //The gap starting at sequence id {0} in the message stream for destination {1} from messaging engine {2} has been resolved on message engine {3}.
            SibTr.info(tc, "RESOLVED_GAP_IN_DESTINATION_TRANSMITTER_CWSIP0793",
                       new Object[] { (new Long(filledGap)).toString(),
                                     _destination.getName(),
                                     SIMPUtils.getMENameFromUuid(sourceMEUuid),
                                     _messageProcessor.getMessagingEngineName() });
        }

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

        if (_isLink)
        {
            // "{0} percent repeated messages received from bus {1} on link {2} on messaging engine {3}"
            SibTr.info(tc, "REPEATED_MESSAGE_THRESHOLD_REACHED_ON_LINK_CWSIP0794",
                       new Object[] { new Integer(percent),
                                     ((LinkHandler) _destination).getBusName(),
                                     _destination.getName(),
                                     _messageProcessor.getMessagingEngineName() });
        }
        else
        {
            // "{0} percent repeated messages received from messaging engine {1} on messaging engine {2} for destination {3}"
            SibTr.info(tc, "REPEATED_MESSAGE_THRESHOLD_REACHED_ON_DESTINATION_CWSIP0795",
                       new Object[] { new Integer(percent),
                                     SIMPUtils.getMENameFromUuid(sourceMEUuid),
                                     _messageProcessor.getMessagingEngineName(),
                                     _destination.getName() });
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reportRepeatedMessages");
    }

    @Override
    public long sendNackMessageWithReturnValue(SIBUuid8 source, SIBUuid12 destUuid,
                                               SIBUuid8 busUuid, long startTick, long endTick, int priority,
                                               Reliability reliability, SIBUuid12 streamID) throws SIResourceException {
        return 0;
    }

    @Override
    public long handleControlMessageWithReturnValue(SIBUuid8 sourceMEUuid,
                                                    ControlMessage cMsg) throws SIIncorrectCallException,
                    SIResourceException, SIConnectionLostException, SIRollbackException {
        return 0;
    }
}
