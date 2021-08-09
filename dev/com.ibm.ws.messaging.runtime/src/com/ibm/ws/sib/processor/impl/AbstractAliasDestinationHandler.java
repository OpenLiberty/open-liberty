/*******************************************************************************
 * Copyright (c) 2012, 2014 IBM Corporation and others.
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.mfp.control.ControlMessage;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.UndeliverableReturnCode;
import com.ibm.ws.sib.processor.exceptions.SIMPErrorException;
import com.ibm.ws.sib.processor.impl.exceptions.InvalidOperationException;
import com.ibm.ws.sib.processor.impl.indexes.SubscriptionIndex;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumableKey;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerKey;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.ControlHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DispatchableKey;
import com.ibm.ws.sib.processor.impl.interfaces.InputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.LocalConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.LocalizationPoint;
import com.ibm.ws.sib.processor.impl.interfaces.OutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.itemstreams.ProxyReferenceStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PubSubMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SourceProtocolItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.TargetProtocolItemStream;
import com.ibm.ws.sib.processor.proxyhandler.Neighbour;
import com.ibm.ws.sib.processor.runtime.impl.AnycastInputControl;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.utils.LockManager;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionAlreadyExistsException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SINonDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SINotAuthorizedException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;

/**
 * @author caseyj
 * 
 *         This class provides getters for destination definition properties.
 *         Any other destination handler functionality at all MUST go on child classes.
 */
abstract class AbstractAliasDestinationHandler
                extends AbstractDestinationHandler
{
    /** Trace for the component */
    private static final TraceComponent tc =
                    SibTr.register(
                                   AbstractAliasDestinationHandler.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    /** NLS for component */
    private static final TraceNLS nls =
                    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

    // The destinationHandler of the underlying target destination.
    protected DestinationHandler _targetDestinationHandler;

    /**
     * Warm start constructor invoked by the Message Store.
     */
    public AbstractAliasDestinationHandler()
    {
        super();

        // This space intentionally left blank.   
    }

    public AbstractAliasDestinationHandler(
                                           MessageProcessor messageProcessor,
                                           DestinationHandler resolvedDestinationHandler,
                                           String busName)
    {
        super(messageProcessor, busName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "AbstractAliasDestinationHandler",
                        new Object[] { messageProcessor, resolvedDestinationHandler, busName });

        //Alias destinations do not need to be stored in the message store
        setStorageStrategy(STORE_NEVER);

        //Remember the resolved destinationHandler for the alias destination       
        _targetDestinationHandler = resolvedDestinationHandler;

        if (isPubSub())
        {
            // Create the pub-sub input handler.
            inputHandler = new PubSubInputHandler(this,
                            resolvedDestinationHandler.getTargetProtocolItemStream(),
                            resolvedDestinationHandler.getPublishPoint(),
                            resolvedDestinationHandler.getProxyReferenceStream(),
                            resolvedDestinationHandler.getSourceProtocolItemStream());

        }
        else
        {
            inputHandler = new PtoPInputHandler(this, resolvedDestinationHandler.getTargetProtocolItemStream());
        }

        //Tell the target that this alias targets it.  If the target is deleted
        //(or if its an alias and its target changes or is deleted)
        //then this alias must be informed so it can handle connected
        //producers and consumers
        _targetDestinationHandler.addTargettingAlias(this);

        // Set the FB sendAllowed attribute based on the target, which could
        // be part of a chain
        _sendAllowedOnTargetForeignBus =
                        _targetDestinationHandler.getSendAllowedOnTargetForeignBus();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "AbstractAliasDestinationHandler", this);
    }

    public int checkPtoPOutputHandlers(boolean isMsgMediated,
                                       SIBUuid8 fixedMEUuid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkPtoPOutputHandlers", new Object[] { Boolean.valueOf(isMsgMediated),
                                                                     fixedMEUuid });

        int result = checkPtoPOutputHandlers(fixedMEUuid, null);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkPtoPOutputHandlers", Integer.valueOf(result));

        return result;
    }

    @Override
    public int checkPtoPOutputHandlers(SIBUuid8 fixedMEUuid,
                                       HashSet<SIBUuid8> scopedMEs)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkPtoPOutputHandlers", new Object[] { fixedMEUuid,
                                                                     scopedMEs });

        // Look to the resolved destination to check output handlers as the
        // alias destination doesnt store any messages
        int result = _targetDestinationHandler.checkPtoPOutputHandlers(fixedMEUuid, scopedMEs);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkPtoPOutputHandlers", Integer.valueOf(result));

        return result;
    }

    /**
     * @return the value provided by the call to the target destination.
     *         This is used when messages arrive over a link
     *         Does not check sendAllowed.
     *         See defect 283324
     */
    @Override
    public int checkCanAcceptMessage(SIBUuid8 MEUuid, HashSet<SIBUuid8> scopedMEs)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkCanAcceptMessage", new Object[] { MEUuid, scopedMEs });

        int returnValue;

        // Defer check to the target (queue, topic space or link)
        returnValue = _targetDestinationHandler.checkCanAcceptMessage(MEUuid, scopedMEs);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkCanAcceptMessage", Integer.valueOf(returnValue));

        return returnValue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#choosePtoPOutputHandler(com.ibm.ws.sib.mfp.JsDestinationAddress)
     */
    @Override
    public OutputHandler choosePtoPOutputHandler(SIBUuid8 fixedMEUuid,
                                                 SIBUuid8 preferredMEUuid,
                                                 boolean localMessage,
                                                 boolean forcePut,
                                                 HashSet<SIBUuid8> scopedMEs)
                    throws
                    SIRollbackException,
                    SIConnectionLostException,
                    SIResourceException,
                    SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "choosePtoPOutputHandler", new Object[] { fixedMEUuid,
                                                                     preferredMEUuid,
                                                                     localMessage,
                                                                     forcePut,
                                                                     scopedMEs });

        // We're an alias (or foreign destination) so we should never be called with a scoped ME set
        if (scopedMEs != null)
        {
            SIMPErrorException e = new SIMPErrorException("Alias called with scoped ME set");

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
            }

            e.setExceptionReason(SIRCConstants.SIRC0901_INTERNAL_MESSAGING_ERROR);
            e.setExceptionInserts(new String[] { "com.ibm.ws.sib.processor.impl.ProducerSessionImpl.handleMessage",
                                                "1:290:1.67.1.22",
                                                SIMPUtils.getStackTrace(e) });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "choosePtoPOutputHandlers", e);

            throw e;
        }

        // Look to the resolved destination to choose an output handler as the
        // alias destination doesnt store any messages
        OutputHandler result = _targetDestinationHandler.choosePtoPOutputHandler(fixedMEUuid, preferredMEUuid, localMessage, false, null);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "choosePtoPOutputHandler", result);

        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#createPubSubOutputHandler(com.ibm.ws.sib.processor.proxyhandler.Neighbour)
     */
    @Override
    public synchronized PubSubOutputHandler createPubSubOutputHandler(
                                                                      Neighbour neighbour) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "createPubSubOutputHandler",
                        new Object[] { neighbour });

        PubSubOutputHandler handler = _targetDestinationHandler.createPubSubOutputHandler(neighbour);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createPubSubOutputHandler", handler);

        return handler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getAllPubSubOutputHandlers()
     */
    @Override
    public HashMap getAllPubSubOutputHandlers()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getAllPubSubOutputHandlers");

        HashMap pubsubOutputHandlers = _targetDestinationHandler.getAllPubSubOutputHandlers();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getAllPubSubOutputHandlers", pubsubOutputHandlers);

        return pubsubOutputHandlers;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDurableSubscriptionConsumerDispatcher(com.ibm.ws.sib.processor.impl.ConsumerDispatcherState)
     */
    @Override
    public ConsumerDispatcher getDurableSubscriptionConsumerDispatcher(
                                                                       ConsumerDispatcherState subState)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDurableSubscriptionConsumerDispatcher", subState);

        ConsumerDispatcher consumerDispatcher = _targetDestinationHandler.getDurableSubscriptionConsumerDispatcher(subState);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(
                       tc,
                       "getDurableSubscriptionConsumerDispatcher",
                       consumerDispatcher);

        return consumerDispatcher;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getExceptionDestination()
     */
    @Override
    public String getExceptionDestination()
    {
        // Look to the resolved destination for this value
        return _targetDestinationHandler.getExceptionDestination();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getExceptionDiscardReliability()
     */
    @Override
    public Reliability getExceptionDiscardReliability()
    {
        // Look to the resolved destination for this value
        return _targetDestinationHandler.getExceptionDiscardReliability();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getTargetProtocolItemStream()
     */
    @Override
    public TargetProtocolItemStream getTargetProtocolItemStream()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getTargetProtocolItemStream");

        TargetProtocolItemStream targetProtocolItemStream = _targetDestinationHandler.getTargetProtocolItemStream();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getTargetProtocolItemStream", targetProtocolItemStream);

        return targetProtocolItemStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getSourceProtocolItemStream()
     */
    @Override
    public SourceProtocolItemStream getSourceProtocolItemStream()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getSourceProtocolItemStream");

        SourceProtocolItemStream sourceProtocolItemStream = _targetDestinationHandler.getSourceProtocolItemStream();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getSourceProtocolItemStream", sourceProtocolItemStream);

        return sourceProtocolItemStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getProxyReferenceStream()
     */
    @Override
    public ProxyReferenceStream getProxyReferenceStream()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getProxyReferenceStream");

        ProxyReferenceStream proxyReferenceStream = _targetDestinationHandler.getProxyReferenceStream();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getProxyReferenceStream", proxyReferenceStream);

        return proxyReferenceStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getResolvedDestinationHandler()
     */
    @Override
    public BaseDestinationHandler getResolvedDestinationHandler()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getResolvedDestinationHandler");

        BaseDestinationHandler resolvedDestinationHandler = _targetDestinationHandler.getResolvedDestinationHandler();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getResolvedDestinationHandler", resolvedDestinationHandler);

        return resolvedDestinationHandler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getLocalPtoPConsumerDispatcher()
     */
    @Override
    public ConsumerManager getLocalPtoPConsumerManager()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getLocalPtoPConsumerManager");

        ConsumerManager consumerManager = _targetDestinationHandler.getLocalPtoPConsumerManager();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getLocalPtoPConsumerManager", consumerManager);

        return consumerManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getPubSubOutputHandler(com.ibm.ws.sib.utils.SIBUuid8)
     */
    @Override
    public PubSubOutputHandler getPubSubOutputHandler(SIBUuid8 neighbourUUID)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getPubSubOutputHandler", neighbourUUID);

        PubSubOutputHandler handler = _targetDestinationHandler.getPubSubOutputHandler(neighbourUUID);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getPubSubOutputHandler", handler);

        return handler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getRemoteConsumerDispatcher(com.ibm.ws.sib.utils.SIBUuid8)
     */
    @Override
    public RemoteConsumerDispatcher getRemoteConsumerDispatcher(SIBUuid8 meId, SIBUuid12 gatheringTargetDestUuid, boolean createAIH)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getRemoteConsumerDispatcher", new Object[] { meId, gatheringTargetDestUuid, Boolean.valueOf(createAIH) });

        RemoteConsumerDispatcher remoteConsumerDispatcher = _targetDestinationHandler.getRemoteConsumerDispatcher(meId, gatheringTargetDestUuid, createAIH);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getRemoteConsumerDispatcher", remoteConsumerDispatcher);

        return remoteConsumerDispatcher;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#unlockPubsubOutputHandlers()
     */
    @Override
    public void unlockPubsubOutputHandlers()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "unlockPubsubOutputHandlers");

        _targetDestinationHandler.unlockPubsubOutputHandlers();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "unlockPubsubOutputHandlers");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#deletePubSubOutputHandler(com.ibm.ws.sib.utils.SIBUuid8)
     */
    @Override
    public synchronized void deletePubSubOutputHandler(SIBUuid8 neighbourUUID)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deletePubSubOutputHandler", neighbourUUID);

        _targetDestinationHandler.deletePubSubOutputHandler(neighbourUUID);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deletePubSubOutputHandler");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#deleteDurableSubscription(java.lang.String, com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    @Override
    public void deleteDurableSubscription(
                                          String subscriptionId,
                                          String durableHome)
                    throws SIDurableSubscriptionNotFoundException,
                    SIDestinationLockedException,
                    SIResourceException,
                    SIIncorrectCallException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "deleteDurableSubscription",
                        new Object[] { subscriptionId });

        _targetDestinationHandler.deleteDurableSubscription(subscriptionId
                                                            , durableHome);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteDurableSubscription", subscriptionId);

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#addConsumerPointMatchTarget(com.ibm.ws.sib.processor.impl.ConsumerKey, java.lang.String)
     */
    @Override
    public void addConsumerPointMatchTarget(
                                            DispatchableKey consumerPointData,
                                            SIBUuid8 cmUuid,
                                            SelectionCriteria criteria)
                    throws SIDiscriminatorSyntaxException, SISelectorSyntaxException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "addConsumerPointMatchTarget",
                        new Object[] { consumerPointData, criteria });

        _targetDestinationHandler.addConsumerPointMatchTarget(consumerPointData,
                                                              cmUuid,
                                                              criteria);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addConsumerPointMatchTarget");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#removeConsumerPointMatchTarget(com.ibm.ws.sib.processor.impl.ConsumerKey)
     */
    @Override
    public void removeConsumerPointMatchTarget(DispatchableKey consumerPointData)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeConsumerPointMatchTarget", consumerPointData);

        _targetDestinationHandler.removeConsumerPointMatchTarget(consumerPointData);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeConsumerPointMatchTarget");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#createSubscriptionConsumerDispatcher(com.ibm.ws.sib.processor.impl.ConsumerDispatcherState)
     */
    @Override
    public ConsumerDispatcher createSubscriptionConsumerDispatcher(ConsumerDispatcherState subState)
                    throws SIDiscriminatorSyntaxException,
                    SISelectorSyntaxException,
                    SIResourceException, SINonDurableSubscriptionMismatchException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createSubscriptionConsumerDispatcher", subState);

        ConsumerDispatcher consumerDispatcher = _targetDestinationHandler.createSubscriptionConsumerDispatcher(subState);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createSubscriptionConsumerDispatcher", consumerDispatcher);

        return consumerDispatcher;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#createSubscriptionConsumerDispatcherAndAttachCP(com.ibm.ws.sib.processor.impl.ConsumerDispatcherState)
     */
    @Override
    public ConsumerKey createSubscriptionConsumerDispatcherAndAttachCP(LocalConsumerPoint consumerPoint,
                                                                       ConsumerDispatcherState subState)
                    throws SIDiscriminatorSyntaxException,
                    SISelectorSyntaxException,
                    SIResourceException,
                    SINonDurableSubscriptionMismatchException,
                    SINotPossibleInCurrentConfigurationException,
                    SIDestinationLockedException,
                    SISessionDroppedException

    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createSubscriptionConsumerDispatcherAndAttachCP", subState);

        ConsumerKey consumerKey = _targetDestinationHandler.createSubscriptionConsumerDispatcherAndAttachCP(consumerPoint, subState);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createSubscriptionConsumerDispatcherAndAttachCP", consumerKey);

        return consumerKey;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isReconciled()
     */
    @Override
    public boolean isReconciled()
    {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getControlHandler(com.ibm.ws.sib.mfp.ProtocolType, com.ibm.ws.sib.trm.topology.Cellule)
     */
    @Override
    public ControlHandler getControlHandler(
                                            ProtocolType type, SIBUuid8 sourceMEUuid, ControlMessage msg)
    {
        throw new InvalidOperationException(
                        nls.getFormattedMessage(
                                                "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                                                new Object[] { "AbstractAliasDestinationHandler",
                                                              "1:681:1.67.1.22",
                                                              getBus() + " : " + getName() },
                                                null));
    }

    @Override
    public SubscriptionIndex getSubscriptionIndex()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getSubscriptionIndex");

        SubscriptionIndex sc = _targetDestinationHandler.getSubscriptionIndex();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getSubscriptionIndex", sc);

        return sc;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getSubscriptionList()
     */
    @Override
    public List getSubscriptionList()
    {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getReallocationLockManager()
     */
    @Override
    public LockManager getReallocationLockManager()
    {
        return _targetDestinationHandler.getReallocationLockManager();
    }

    // ------------------------------------------------------------------------------------

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getInputHandler(com.ibm.ws.sib.common.ProtocolType, com.ibm.ws.sib.trm.topology.Cellule)
     */
    @Override
    public InputHandler getInputHandler(ProtocolType type, SIBUuid8 sourceMEUuid, JsMessage msg)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getInputHandler", new Object[] { type, sourceMEUuid });

        InputHandler msgHandler = null;

        if (type == ProtocolType.UNICASTINPUT)
        {
            msgHandler = getInputHandler();
        }
        else if (type == ProtocolType.PUBSUBINPUT)
        {
            msgHandler = getInputHandler();
        }
        else if (type == ProtocolType.ANYCASTINPUT)
        {
            //Defer remote get inputHandler to the resolved destination
            msgHandler = _targetDestinationHandler.getInputHandler(type, sourceMEUuid, msg);
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "getInputHandler", "SIErrorException");
            //unsupported protocol type
            throw new SIErrorException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0005",
                                                    new Object[] { "AbstractAliasDestinationHandler",
                                                                  "1:751:1.67.1.22",
                                                                  getBus() + " : " + getName() },
                                                    null));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getInputHandler", msgHandler);

        return msgHandler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getInputHandler()
     */
    @Override
    public InputHandler getInputHandler()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getInputHandler");
            SibTr.exit(tc, "getInputHandler", inputHandler);
        }

        return inputHandler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isTemporary()
     */
    @Override
    public boolean isTemporary()
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isSystem()
     */
    @Override
    public boolean isSystem()
    {
        return false;
    }

    @Override
    public boolean isTargetedAtLink()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isTargetedAtLink");
            SibTr.exit(tc, "isTargetedAtLink", "delegated");
        }

        return _targetDestinationHandler.isTargetedAtLink();
    }

    @Override
    public PubSubMessageItemStream getPublishPoint()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getPublishPoint");

        PubSubMessageItemStream stream = _targetDestinationHandler.getPublishPoint();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getPublishPoint", stream);

        return stream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getBaseUuid()
     */
    @Override
    public SIBUuid12 getBaseUuid()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getBaseUuid");

        SIBUuid12 uuid = _targetDestinationHandler.getDefinition().getUUID();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getBaseUuid", uuid);

        return uuid;
    }

    /**
     * Was server is announcingthat it is now completely started, and open for
     * e-business.
     */
    @Override
    public void announceWasOpenForEBusiness()
    {
        //Alias destinations aren't mediated
    }

    /**
     * Was server is announcing that it is closing down now.
     */
    @Override
    public void announceWasClosedForEBusiness()
    {
        //Alias destinations aren't mediated
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#startMediating()
     */
    @Override
    public void announceMPStarted()
    {
        //Alias destinations aren't mediated
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#startMediating()
     */
    @Override
    public void announceMPStopping()
    {
        //Alias destinations aren't mediated
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#createDurableSubscription(com.ibm.ws.sib.processor.impl.ConsumerDispatcherState,
     * com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    @Override
    public void createDurableSubscription(ConsumerDispatcherState subState, TransactionCommon transaction) throws SIDurableSubscriptionAlreadyExistsException, SIDiscriminatorSyntaxException, SISelectorSyntaxException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createDurableSubscription", new Object[] { subState, transaction });

        _targetDestinationHandler.createDurableSubscription(subState,
                                                            transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createDurableSubscription");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#attachToDurableSubscription(com.ibm.ws.sib.processor.impl.LocalConsumerPoint,
     * com.ibm.ws.sib.processor.impl.ConsumerDispatcherState)
     */
    @Override
    public ConsumableKey attachToDurableSubscription(LocalConsumerPoint consumerPoint,
                                                     ConsumerDispatcherState subState)
                    throws SIDestinationLockedException, SIDurableSubscriptionMismatchException, SIDiscriminatorSyntaxException, SISelectorSyntaxException, SIDurableSubscriptionNotFoundException, SINotAuthorizedException, SIIncorrectCallException, SIResourceException, SINotPossibleInCurrentConfigurationException

    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "attachToDurableSubscription", new Object[] { consumerPoint, subState });

        ConsumableKey consumerKey =
                        _targetDestinationHandler.attachToDurableSubscription(consumerPoint, subState);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "attachToDurableSubscription");

        return consumerKey;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getAlterationTime()
     */
    @Override
    public long getAlterationTime()
    {
        return _targetDestinationHandler.getAlterationTime();
    }

    /**
     * The desination is being asked to start any dynamic things.
     */
    @Override
    public void start()
    {}

    /**
     * Stop anything that needs stopping.
     */
    @Override
    public void stop(int mode)
    {}

    /**
     * Is a real target destination corrupt?
     */
    @Override
    public boolean isCorruptOrIndoubt()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isCorruptOrIndoubt");

        boolean isCorruptOrIndoubt = _targetDestinationHandler.isCorruptOrIndoubt();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isCorruptOrIndoubt", Boolean.valueOf(isCorruptOrIndoubt));

        return isCorruptOrIndoubt;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#setCorrupt(boolean)
     */
    @Override
    public void setCorrupt(boolean isCorrupt)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setCorrupt", Boolean.valueOf(isCorrupt));

        _targetDestinationHandler.setCorrupt(isCorrupt);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setCorrupt");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#setIndoubt(boolean)
     */
    @Override
    public void setIndoubt(boolean isIndoubt)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setIndoubt", Boolean.valueOf(isIndoubt));

        _targetDestinationHandler.setIndoubt(isIndoubt);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setIndoubt");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#hasLocal()
     */
    @Override
    public boolean hasLocal()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "hasLocal");

        boolean hasLocal = _targetDestinationHandler.hasLocal();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "hasLocal", Boolean.valueOf(hasLocal));

        return hasLocal;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#hasRemote()
     */
    @Override
    public boolean hasRemote()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "hasRemote");

        boolean hasRemote = _targetDestinationHandler.hasRemote();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "hasRemote", Boolean.valueOf(hasRemote));

        return hasRemote;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isReceiveExclusive()
     */
    @Override
    public boolean isReceiveExclusive()
    {
        // Look to the resolved destination for this value
        return _targetDestinationHandler.isReceiveExclusive();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#handleUndeliverableMessage(com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage, int, java.lang.String[])
     */
    @Override
    public UndeliverableReturnCode handleUndeliverableMessage(
                                                              SIMPMessage msg,
                                                              int exceptionReason,
                                                              String[] exceptionInserts,
                                                              TransactionCommon tran)
                    throws SIIncorrectCallException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "handleUndeliverableMessage",
                        new Object[] { msg, new Integer(exceptionReason), exceptionInserts });

        UndeliverableReturnCode undeliverableReturnCode =
                        _targetDestinationHandler.handleUndeliverableMessage(msg, exceptionReason, exceptionInserts, tran);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "handleUndeliverableMessage", undeliverableReturnCode);

        return undeliverableReturnCode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#registerForMessageEvents(com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage)
     */
    @Override
    public void registerForMessageEvents(SIMPMessage msg)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "registerForMessageEvents", msg);

        _targetDestinationHandler.registerForMessageEvents(msg);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerForMessageEvents");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isToBeDeleted()
     */
    @Override
    public boolean isToBeDeleted()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isToBeDeleted", new Object[] { getName() });

        boolean toBeDeleted = _targetDestinationHandler.isToBeDeleted();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isToBeDeleted", Boolean.valueOf(toBeDeleted));

        return toBeDeleted;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getMaxFailedDeliveries()
     */
    @Override
    public int getMaxFailedDeliveries()
    {
        // Look to the resolved destination for this value
        return _targetDestinationHandler.getMaxFailedDeliveries();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getBlockedRetryInterval()
     */
    @Override
    public long getBlockedRetryInterval()
    {
        // Look to the resolved destination for this value
        return _targetDestinationHandler.getBlockedRetryInterval();
    }

    abstract public String getTargetName();

    abstract public String getTargetBus();

    /**
     * This destination handler is being deleted and should perform any
     * processing required.
     */
    public void delete()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "delete");
        //Tell the target of the alias to remove the backwards reference to it
        _targetDestinationHandler.removeTargettingAlias(this);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "delete");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getQueuePoint(com.ibm.ws.sib.utils.SIBUuid8)
     */
    @Override
    public LocalizationPoint getQueuePoint(SIBUuid8 meUuid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getQueuePoint", meUuid);
        LocalizationPoint stream = _targetDestinationHandler.getQueuePoint(meUuid);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getQueuePoint", stream);
        return stream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#registerControlAdapters()
     */
    @Override
    public void registerControlAdapters()
    {
        //We're not registering the Alias' control handlers at this time
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getAnycastInputHandler(com.ibm.ws.sib.utils.SIBUuid8)
     */
    @Override
    public AnycastInputHandler getAnycastInputHandler(SIBUuid8 dmeId, SIBUuid12 gatheringTargetDestUuid, boolean createAIH)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getAnycastInputHandler", new Object[] { dmeId, gatheringTargetDestUuid, Boolean.valueOf(createAIH) });

        AnycastInputHandler aih = _targetDestinationHandler.getAnycastInputHandler(dmeId, gatheringTargetDestUuid, createAIH);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getAnycastInputHandler", aih);

        return aih;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#notifyReceiveAllowedRCD(com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler)
     */
    @Override
    public void notifyReceiveAllowedRCD(DestinationHandler destinationHandler)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "notifyReceiveAllowedRCD", new Object[] { destinationHandler });

        _targetDestinationHandler.notifyReceiveAllowedRCD(destinationHandler);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "notifyReceiveAllowedRCD");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#notifyReceiveAllowed(com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler)
     */
    @Override
    public abstract void notifyReceiveAllowed(DestinationHandler destinationHandler);

    DestinationHandler getTarget()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getTarget");
            SibTr.exit(tc, "getTarget", _targetDestinationHandler);
        }
        return _targetDestinationHandler;
    }

    @Override
    public Iterator<AnycastInputControl> getAIControlAdapterIterator()
    {
        //no-op
        return null;
    }

    @Override
    public Iterator<ControlAdapter> getAOControlAdapterIterator()
    {
        //no-op
        return null;
    }
}
