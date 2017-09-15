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
package com.ibm.ws.sib.processor.impl.destination;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.admin.SIBExceptionBase;
import com.ibm.ws.sib.mfp.control.ControlCreateStream;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.MessageStoreRuntimeException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.Statistics;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPDestinationLockedException;
import com.ibm.ws.sib.processor.impl.AnycastInputHandler;
import com.ibm.ws.sib.processor.impl.AnycastOutputHandler;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcherState;
import com.ibm.ws.sib.processor.impl.DurableConstants;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.PubSubInputHandler;
import com.ibm.ws.sib.processor.impl.PubSubOutputHandler;
import com.ibm.ws.sib.processor.impl.RemoteConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.indexes.SubscriptionIndex;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumableKey;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerKey;
import com.ibm.ws.sib.processor.impl.interfaces.ControlHandler;
import com.ibm.ws.sib.processor.impl.interfaces.LocalConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.OutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.BatchHandler;
import com.ibm.ws.sib.processor.impl.store.MessageEvents;
import com.ibm.ws.sib.processor.impl.store.filters.ClassEqualsFilter;
import com.ibm.ws.sib.processor.impl.store.filters.SubscriptionFilter;
import com.ibm.ws.sib.processor.impl.store.filters.SubscriptionStateFilter;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.impl.store.items.MessageItemReference;
import com.ibm.ws.sib.processor.impl.store.itemstreams.AOContainerItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.DurableSubscriptionItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.ProxyReferenceStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PubSubMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream;
import com.ibm.ws.sib.processor.proxyhandler.Neighbour;
import com.ibm.ws.sib.processor.runtime.impl.MPRuntimeEvent;
import com.ibm.ws.sib.processor.utils.LockManager;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.DestinationType;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.SelectorDomain;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionAlreadyExistsException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SINonDurableSubscriptionMismatchException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;

/**
 * @author nyoung
 * 
 *         <p>The PubSubRealization the pubsub state specific to a BaseDestinationHandler
 *         that represents a TopicSpace.
 */
public class PubSubRealization
                extends AbstractProtoRealization
{
    /**
     * Trace for the component
     */
    private static final TraceComponent tc =
                    SibTr.register(
                                   PubSubRealization.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    /** NLS for component */
    static final TraceNLS nls =
                    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

    /**
     * For PubSub messages that are to be sent to Neighbouring ME's
     * then there is a list of ME Name to PubSubOutputHandlers.
     */
    private HashMap<SIBUuid8, PubSubOutputHandler> _pubsubOutputHandlers = null;

    /**
     * If the destination is representing a topicspace, it can have durable
     * subscriptions associated with it. The hashmap consumerDispatchersDurable
     * is the set of these durable subscriptions. An attaching consumer needs
     * to be able to check if a durable subscription already exists before
     * creating a new one and this is one of the reasons the hashmap is
     * maintained. Note, this variable only points at the hashmap stored on the
     * destination manager. Subscripton names are used as keys for this map
     * (i.e. clientID##subName or durHome##clientID##subName). The form
     * durHome##clientID##subName is used only when the durable home is not local.
     * Also, the durHome is actually the UUID8 of the durable home ME.
     */
    private HashMap<String, Object> _consumerDispatchersDurable = null;

    /**
     * A PubSubMessageItemStream is used by the BaseDestinationHandler for storing
     * publications for a topcispace.
     * Feature 174199.2.20
     */
    PubSubMessageItemStream _pubsubMessageItemStream = null;

    /**
     * Proxy persistent storage stream.
     * Feature 174199.2.20
     */
    private ProxyReferenceStream _proxyReferenceStream;

    private LockManager _pubsubOutputHandlerLockManager = null;

    private SubscriptionIndex _subscriptionIndex = null;

    protected RemotePubSubSupport _pubSubRemoteSupport = null;

    /**
     * <p>Cold start constructor.</p>
     * <p>Create a new instance of a destination, passing in the name of the
     * destination and its definition. A destination represents a topicspace in
     * pub/sub or a queue in point to point.</p>
     * 
     * @param destinationName
     * @param destinationDefinition
     * @param messageProcessor
     * @param parentStream The Itemstream this DestinationHandler should be
     *            added into.
     * @param durableSubscriptionsTable Required only by topicspace
     *            destinations. Can be null if point to point (local or remote).
     * @param busName The name of the bus on which the destination resides
     */
    public PubSubRealization(
                             BaseDestinationHandler myBaseDestinationHandler,
                             MessageProcessor messageProcessor,
                             LocalisationManager localisationManager,
                             TransactionCommon transaction)
        throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "PubSubRealization", new Object[] {
                                                               myBaseDestinationHandler, messageProcessor,
                                                               localisationManager, transaction });

        _baseDestinationHandler = myBaseDestinationHandler;
        _messageProcessor = messageProcessor;
        _destinationManager = messageProcessor.getDestinationManager();

        // Initialise DA manager to interface to WLM
        _localisationManager = localisationManager;
        _localisationManager.setLocal();
        _localisationManager.setRemote(false);

        _remoteSupport = new RemotePubSubSupport(myBaseDestinationHandler,
                        messageProcessor);

        _pubSubRemoteSupport = (RemotePubSubSupport) _remoteSupport;

        Transaction msTran = messageProcessor.resolveAndEnlistMsgStoreTransaction(transaction);

        try
        {
            _pubsubMessageItemStream =
                            new PubSubMessageItemStream(myBaseDestinationHandler, msTran);

            _proxyReferenceStream =
                            new ProxyReferenceStream(_pubsubMessageItemStream, msTran);
        } catch (OutOfCacheSpace e)
        {
            // No FFDC code needed
            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "PubSubRealization", "SIResourceException");
            throw new SIResourceException(e);
        } catch (MessageStoreException e)
        {
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.PubSubRealization",
                                        "1:291:1.35.2.4",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "PubSubRealization", "SIResourceException");

            throw new SIResourceException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "PubSubRealization", this);
    }

    public PubSubRealization(
                             BaseDestinationHandler myBaseDestinationHandler,
                             MessageProcessor messageProcessor,
                             LocalisationManager localisationManager)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "PubSubRealization", new Object[] {
                                                               myBaseDestinationHandler, messageProcessor,
                                                               localisationManager });

        _baseDestinationHandler = myBaseDestinationHandler;
        _messageProcessor = messageProcessor;
        _destinationManager = messageProcessor.getDestinationManager();

        _remoteSupport = new RemotePubSubSupport(myBaseDestinationHandler,
                        messageProcessor);

        // Initialise DA manager to interface to WLM
        _localisationManager = localisationManager;
        _localisationManager.setLocal();
        _localisationManager.setRemote(false);

        _pubSubRemoteSupport = (RemotePubSubSupport) _remoteSupport;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "PubSubRealization", this);
    }

    public void initialise(
                           boolean createPubSubInputHandler,
                           HashMap<String, Object> durableSubscriptionsTable)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "initialise",
                        new Object[] {
                                      new Boolean(createPubSubInputHandler),
                                      durableSubscriptionsTable });

        _consumerDispatchersDurable = durableSubscriptionsTable;
        _pubsubOutputHandlers = new HashMap<SIBUuid8, PubSubOutputHandler>();
        _pubsubOutputHandlerLockManager = new LockManager();
        _subscriptionIndex = new SubscriptionIndex();

        _pubSubRemoteSupport.initialisePseudoMaps();

        if (createPubSubInputHandler)
            createInputHandlersForPubSub();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "initialise");
    }

    // 176658.3.5
    // This is a pub/sub destination: reconstitute the appropriate ItemStreams
    // There will only be a proxy reference stream if the destination is pub/sub
    // There will only be one such proxy reference stream.
    public void reconstitute(
                             int startMode,
                             HashMap<String, Object> durableSubscriptionsTable)
                    throws
                    SIIncorrectCallException,
                    SIDiscriminatorSyntaxException,
                    SISelectorSyntaxException,
                    MessageStoreException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc,
                        "reconstitute",
                        new Object[] { new Integer(startMode), durableSubscriptionsTable });

        // Reconstitute the static state
        initialise(false,
                   durableSubscriptionsTable);

        // There should only be one messageItemStream in the BaseDestinationHandler.
        NonLockingCursor cursor =
                        _baseDestinationHandler.newNonLockingItemStreamCursor(
                                        new ClassEqualsFilter(PubSubMessageItemStream.class));
        _pubsubMessageItemStream = (PubSubMessageItemStream) cursor.next();

        // Sanity - A BaseDestinationHandler should not be in the DestinationManager
        // without a PubSubMessageItemStream!
        if (_pubsubMessageItemStream == null)
        {
            SIErrorException e =
                            new SIErrorException(
                                            nls.getFormattedMessage(
                                                                    "DESTINATION_HANDLER_RECOVERY_ERROR_CWSIP0048",
                                                                    new Object[] { _baseDestinationHandler.getName() },
                                                                    null));

            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.reconstitute",
                                        "1:458:1.35.2.4",
                                        this);

            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "reconstitute", e);
            throw e;
        }

        cursor.finished();

        _pubsubMessageItemStream.reconstitute(_baseDestinationHandler);
        _localisationManager.setLocal();

        cursor =
                        _pubsubMessageItemStream.newNonLockingReferenceStreamCursor(
                                        new ClassEqualsFilter(ProxyReferenceStream.class));
        _proxyReferenceStream = (ProxyReferenceStream) cursor.next();

        // Sanity - A BaseDestinationHandler should not be in the DestinationManager
        // without a ProxyReferenceStream in the pub/sub case!
        if (_proxyReferenceStream == null)
        {
            SIErrorException e =
                            new SIErrorException(
                                            nls.getFormattedMessage(
                                                                    "DESTINATION_HANDLER_RECOVERY_ERROR_CWSIP0048",
                                                                    new Object[] { _baseDestinationHandler.getName() },
                                                                    null));

            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.reconstitute",
                                        "1:491:1.35.2.4",
                                        this);

            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "reconstitute", e);
            throw e;
        }

        // PK62569 Increment the reference stream count on the pub/sub item stream
        // (the count is not persisted) to include the proxy reference stream.
        _pubsubMessageItemStream.incrementReferenceStreamCount();

        cursor.finished();

        createInputHandlersForPubSub();

        // recover the durable subscriptions
        reconstituteDurableSubscriptions();

        //Venu mock mock
        //comenting this out as there is no proxy handler for now. 
        /*
         * // Call the event created on the Proxy handler code.
         * _messageProcessor.getProxyHandler().topicSpaceCreatedEvent(
         * _baseDestinationHandler);
         */

        // Reconstitute source streams for PubSubInputHandler
        _pubSubRemoteSupport.reconstituteSourceStreams(startMode, null);

        // IMPORTANT: reconstitute remote durable state last so that
        // any local durable state has already been restored.
        _pubSubRemoteSupport.
                        reconstituteRemoteDurable(startMode, _consumerDispatchersDurable);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconstitute");
    }

    /**
     * Gives signal to PubSubMessageItemStream's deleteMsgsWithNoReferences() to gracefully exit. This happens in two cases
     * (i) When ME is stopped and (ii) When the destination is deleted
     * 
     * @param hasToStop
     */
    public void stopDeletingMsgsWihoutReferencesTask(boolean hasToStop) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "stopDeletingMsgsWihoutReferencesTask", hasToStop);

        if (null != _pubsubMessageItemStream) {
            //signal so that PubSubMessageItemStream's deleteMsgsWithNoReferences() greacefully exits
            _pubsubMessageItemStream.stopDeletingMsgsWihoutReferencesTask(hasToStop);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "stopDeletingMsgsWihoutReferencesTask", hasToStop);

    }

    /**
     * This method deletes the messages with no references. Previously these messages
     * were deleted during ME startup in reconstitute method.This method is called from
     * DeletePubSubMsgsThread context
     */
    public void deleteMsgsWithNoReferences() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deleteMsgsWithNoReferences");

        //No need to check for NULL as by this point of time _pubsubMessageItemStream would
        //hold a valid reference, but still doing sanity check
        if (null != _pubsubMessageItemStream)
            _pubsubMessageItemStream.deleteMsgsWithNoReferences();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteMsgsWithNoReferences");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.destination.AbstractProtoRealization#reconstituteEnoughForDeletion()
     */
    @Override
    public void reconstituteEnoughForDeletion()
                    throws
                    MessageStoreException,
                    SIRollbackException,
                    SIConnectionLostException,
                    SIIncorrectCallException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "reconstituteEnoughForDeletion");

        NonLockingCursor cursor =
                        _baseDestinationHandler.newNonLockingItemStreamCursor(
                                        new ClassEqualsFilter(PubSubMessageItemStream.class));
        _pubsubMessageItemStream = (PubSubMessageItemStream) cursor.next();

        if (null != _pubsubMessageItemStream)
            _pubsubMessageItemStream.reconstitute(_baseDestinationHandler);

        cursor.finished();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconstituteEnoughForDeletion");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getControlHandler(com.ibm.ws.sib.common.ProtocolType, com.ibm.ws.sib.trm.topology.Cellule)
     */
    //_preMediationInputHandler
    public ControlHandler getControlHandler(
                                            SIBUuid8 sourceMEUuid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "getControlHandler",
                        new Object[] { sourceMEUuid });

        ControlHandler msgHandler =
                        _pubsubOutputHandlers.get(sourceMEUuid);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getControlHandler", msgHandler);

        return msgHandler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDurableSubscriptionConsumerDispatcher(com.ibm.ws.sib.processor.impl.ConsumerDispatcherState)
     */
    public ConsumerDispatcher getDurableSubscriptionConsumerDispatcher(ConsumerDispatcherState subState)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDurableSubscriptionConsumerDispatcher", subState);

        ConsumerDispatcher consumerDispatcher = null;

        // NOTE: there are two unsynchronized calls to this method from SIMPSubscriptionAssert
        // The more common call from createDurableSubscription already holds the lock
        // here in which case the extra synchronized is minor overhead.

        synchronized (_consumerDispatchersDurable)
        {
            consumerDispatcher =
                            (ConsumerDispatcher) _consumerDispatchersDurable.get(
                                            subState.getSubscriberID());
        }

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
     * @see com.ibm.ws.sib.processor.impl.destination.AbstractProtoRealization#getLocalPostMedPtoPOH(boolean, boolean, boolean)
     */
    @Override
    public OutputHandler getLocalPostMedPtoPOH(boolean localMessage,
                                               boolean forcePut,
                                               boolean singleServer)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc,
                        "getLocalPostMedPtoPOH",
                        new Object[] { new Boolean(localMessage),
                                      new Boolean(forcePut),
                                      new Boolean(singleServer) });

        // Return null for pubsub
        OutputHandler result = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getLocalPostMedPtoPOH", result);

        return result;
    }

    /**
     * <p>This method is used to create a consumer dispatcher for subscriptions
     * A subscription can be either durable
     * or non-durable. If the subscription is durable, the reference stream
     * should be persistent, otherwise it can be a
     * temporary reference stream.</p>
     * 
     * @param state The state object
     * @param subscription The subscription referenceStream to pass to the
     *            Consumer Dispatcher.
     * @return ConsumerDispatcher The created Consumer dispatcher
     * 
     */
    private ConsumerDispatcher createConsumerDispatcher(
                                                        ConsumerDispatcherState state,
                                                        SubscriptionItemStream subscription)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "createConsumerDispatcher",
                        new Object[] { state, subscription });

        /*
         * Now create the consumer dispatcher,
         * passing it the subscription reference stream that has been created
         */
        final ConsumerDispatcher newConsumerDispatcher =
                        new ConsumerDispatcher(_baseDestinationHandler, subscription, state);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createConsumerDispatcher", newConsumerDispatcher);

        return newConsumerDispatcher;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#createSubscriptionConsumerDispatcherAndAttachCP
     */
    public ConsumerKey createSubscriptionConsumerDispatcherAndAttachCP(LocalConsumerPoint consumerPoint,
                                                                       ConsumerDispatcherState subState)
                    throws
                    SIDiscriminatorSyntaxException,
                    SISelectorSyntaxException,
                    SIResourceException,
                    SINonDurableSubscriptionMismatchException,
                    SINotPossibleInCurrentConfigurationException,
                    SIDestinationLockedException,
                    SISessionDroppedException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createSubscriptionConsumerDispatcherAndAttachCP", new Object[] { consumerPoint, subState });

        ConsumerKey consumerKey = null;

        ConsumerDispatcher cd = null;

        if (subState.getSubscriberID() == null) {
            //this is non-durable non shared scenario. i.e subscriber id is null. Then go ahead and create
            //consumer dispatcher and subscription item stream.
            cd = createSubscriptionItemStreamAndConsumerDispatcher(subState, false);

            // attach consumer to CD (with no selector as the CD is handling that)
            consumerKey = cd.attachConsumerPoint(consumerPoint,
                                                 null,
                                                 consumerPoint.getConsumerSession().getConnectionUuid(),
                                                 consumerPoint.getConsumerSession().getReadAhead(),
                                                 consumerPoint.getConsumerSession().getForwardScanning(),
                                                 null);
        } else {
            //this is non-durable shared scenario.
            //Check whether already subscriber present or not in hashamp
            //The lock has to be even for get .. as 
            //'retrieval/creation of CD and attaching consumer to CD' should be atomic. 
            synchronized (_destinationManager.getNondurableSharedSubscriptions()) { // this lock is too high level.. has to be further granularized.
                cd = (ConsumerDispatcher) _destinationManager.getNondurableSharedSubscriptions().get(subState.getSubscriberID());
                if (cd == null) {
                    //consumer dispatcher is null.. means this is first consumer trying to create subscriber. 
                    //Go ahead and create consumer dispatcher and Subscription item stream.
                    //we do not need to check for any flags like cloned because the call is from JMS2.0 explicitly
                    //asking for subscriber to be shared.

                    //_consumerDispatchersNonDurable is needed as the subscription creation has to be atomic
                    cd = createSubscriptionItemStreamAndConsumerDispatcher(subState, true);
                } else {
                    //already a prior consumer has created CD. Going to reuse it.
                    //Before check whether it is same topic and having topic selectors. 
                    //it not throw an exception.
                    //check whether it is same topic and having topic selectors.
                    if (!cd.getConsumerDispatcherState().equals(subState)) {
                        // Found consumer dispatcher but only the IDs match, therefore cannot connect
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "createSubscriptionConsumerDispatcher", subState);

                        throw new SINonDurableSubscriptionMismatchException(
                                        nls.getFormattedMessage(
                                                                "SUBSCRIPTION_ALREADY_EXISTS_ERROR_CWSIP0143",
                                                                new Object[] {
                                                                              subState.getSubscriberID(),
                                                                              _messageProcessor.getMessagingEngineName() },
                                                                null));
                    }

                }

                // CD has been either created freshly or obtained from prior consumer. 
                // Attach consumer point to CD in this atomic unit it self as other
                // close calls would not delete this CD in between. All non durable consumer close
                // also has to use the same lock i.e "_destinationManager.getNondurableSharedSubscriptions()"
                // for atomic unit of code 'which checks if CD has zero consumer points and then deletes the CD'

                // attach consumer to CD (with no selector as the CD is handling that)
                consumerKey = cd.attachConsumerPoint(consumerPoint,
                                                     null,
                                                     consumerPoint.getConsumerSession().getConnectionUuid(),
                                                     consumerPoint.getConsumerSession().getReadAhead(),
                                                     consumerPoint.getConsumerSession().getForwardScanning(),
                                                     null);
            } //lock _destinationManager.getNondurableSharedSubscriptions()
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createSubscriptionConsumerDispatcherAndAttachCP", consumerKey);

        return consumerKey;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#createSubscriptionConsumerDispatcher(com.ibm.ws.sib.processor.impl.ConsumerDispatcherState)
     */
    //Keeping this function as it is .. it is referenced by AbstractAliasDestinationHandler.
    //however nobody is calling this thru AbstractAliasDestinationHandler in Liberty.  
    public ConsumerDispatcher createSubscriptionConsumerDispatcher(ConsumerDispatcherState subState)
                    throws
                    SIDiscriminatorSyntaxException,
                    SISelectorSyntaxException,
                    SIResourceException,
                    SISelectorSyntaxException,
                    SIDiscriminatorSyntaxException,
                    SINonDurableSubscriptionMismatchException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createSubscriptionConsumerDispatcher", subState);

        ConsumerDispatcher cd = null;
        boolean isNewCDcreated = false;

        if (subState.getSubscriberID() == null) {
            //this is non-durable non shared scenario. i.e subscriber id is null. Then go ahead and create
            //consumer dispatcher and subscription item stream.
            cd = createSubscriptionItemStreamAndConsumerDispatcher(subState, false);
        } else {
            //this is non-durable shared scenario.
            //Check whether already subscriber present or not in hashamp
            cd = (ConsumerDispatcher) _destinationManager.getNondurableSharedSubscriptions().get(subState.getSubscriberID());
            if (cd == null) {
                //consumer dispatcher is null.. means this is first consumer trying to create subscriber. 
                //Go ahead and create consumer dispatcher and Subscription item stream.
                //we do not need to check for any flags like cloned because the call is from JMS2.0 explicitly
                //asking for subscriber to be shared.

                //_consumerDispatchersNonDurable is needed as the subscription creation has to be atomic
                synchronized (_destinationManager.getNondurableSharedSubscriptions()) { // this lock is too high level.. has to be further granularized.
                    //again try to get cd for a given consumer as another thread might have created it first i.e got lock first after cd=null
                    cd = (ConsumerDispatcher) _destinationManager.getNondurableSharedSubscriptions().get(subState.getSubscriberID());
                    if (cd == null) {
                        cd = createSubscriptionItemStreamAndConsumerDispatcher(subState, true);
                        isNewCDcreated = true;
                    }
                }
            }
        }

        //check whether this is non-durable shared  consumer and CD is prior created by
        //another consumer.. this consumer supposed to reuse it.
        if (!isNewCDcreated && (subState.getSubscriberID() != null)) {

            //check whether it is same topic and having topic selectors.
            if (!cd.getConsumerDispatcherState().equals(subState)) {
                // Found consumer dispatcher but only the IDs match, therefore cannot connect
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "createSubscriptionConsumerDispatcher", subState);

                throw new SINonDurableSubscriptionMismatchException(
                                nls.getFormattedMessage(
                                                        "SUBSCRIPTION_ALREADY_EXISTS_ERROR_CWSIP0143",
                                                        new Object[] {
                                                                      subState.getSubscriberID(),
                                                                      _messageProcessor.getMessagingEngineName() },
                                                        null));
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createSubscriptionConsumerDispatcher", cd);

        return cd;
    }

    /**
     * <p/>this method creates SubscriptionItemstream and CD and puts into relevant hashmaps.
     * 
     * @param state The consumer state object
     * @param isShared specifies whether the non durable subscriber can be shared or not.
     * @return ConsumerDispatcher The created Consumer dispatcher
     */
    private ConsumerDispatcher createSubscriptionItemStreamAndConsumerDispatcher(ConsumerDispatcherState subState, boolean isShared)
                    throws SIResourceException, SIDiscriminatorSyntaxException, SISelectorSyntaxException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createSubscriptionItemStreamAndConsumerDispatcher", new Object[] { subState, isShared });

        // Now attempt to create the item stream
        SubscriptionItemStream subscription = null;
        ConsumerDispatcher cd = null;

        try
        {
            // 174199.2.9
            // Temporary non-persistent topicspaces require non-persistent
            // consumer dispatchers.
            subscription =
                            new SubscriptionItemStream(
                                            _pubsubMessageItemStream,
                                            _baseDestinationHandler.getTransactionManager());
        } catch (OutOfCacheSpace e)
        {
            // No FFDC code needed
            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createSubscriptionItemStreamAndConsumerDispatcher", e);

            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "SUBSCRIPTION_CREATION_FAILED_CWSIP0034",
                                                    new Object[] { _baseDestinationHandler.getName(), e },
                                                    null),
                            e);
        } catch (MessageStoreException e)
        {
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.createSubscriptionItemStreamAndConsumerDispatcher",
                                        "1:763:1.35.2.4",
                                        this);

            SibTr.exception(tc, e);

            // Write the error to the console
            SibTr.error(
                        tc,
                        "SUBSCRIPTION_CREATION_FAILED_CWSIP0031",
                        new Object[] { _baseDestinationHandler.getName(), e });

            /*
             * The message store has thrown an exception, which leaves us stuck. Record an ffst,
             * write a trace record and throw a SIResourceException to the caller.
             */
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "createSubscriptionItemStreamAndConsumerDispatcher", e);

            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "SUBSCRIPTION_CREATION_FAILED_CWSIP0031",
                                                    new Object[] { _baseDestinationHandler.getName(), e },
                                                    null),
                            e);
        }

        cd = createConsumerDispatcher(subState, subscription);

        // Store the CD in the MatchSpace
        _messageProcessor
                        .getMessageProcessorMatching()
                        .addConsumerDispatcherMatchTarget(
                                                          cd,
                                                          _baseDestinationHandler.getUuid(),
                                                          subState.getSelectionCriteria());

        //store cd into hash map only in case if the consumer is shared. 
        // All non-durable shared consumer's subscriberID do not start with _NON_DURABLE_NON_SHARED
        if (!subState.getSubscriberID().contains("_NON_DURABLE_NON_SHARED"))
            _destinationManager.getNondurableSharedSubscriptions().put(subState.getSubscriberID(), cd);

        // 166833.2
        // Generate the subscribeEvent for this new subscription
        // to see if this needs to be forwarded to a Neighbouring ME.
        //venu liberty change
        //_messageProcessor.getProxyHandler().subscribeEvent(subState, null);

        _subscriptionIndex.put(cd);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createSubscriptionItemStreamAndConsumerDispatcher", cd);

        return cd;

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getPubSubOutputHandler(com.ibm.ws.sib.utils.SIBUuid8)
     */
    public PubSubOutputHandler getPubSubOutputHandler(SIBUuid8 neighbourUUID)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getPubSubOutputHandler", neighbourUUID);

        PubSubOutputHandler handler = null;

        //note that this does not obtain a read lock before attempting the get
        // Get the PubSub Output Handel
        if (_pubsubOutputHandlers != null)
            handler = _pubsubOutputHandlers.get(neighbourUUID);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getPubSubOutputHandler", handler);

        return handler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#createPubSubOutputHandler(com.ibm.ws.sib.processor.proxyhandler.Neighbour)
     */
    public synchronized PubSubOutputHandler createPubSubOutputHandler(Neighbour neighbour)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createPubSubOutputHandler", new Object[] { neighbour });

        // Check that this handler wasn't already created.
        PubSubOutputHandler handler = getPubSubOutputHandler(neighbour.getUUID());

        if (handler == null)
            handler =
                            new PubSubOutputHandler(
                                            _messageProcessor,
                                            neighbour,
                                            _baseDestinationHandler);

        _pubsubOutputHandlerLockManager.lockExclusive();
        // Put this outputhandler into the list of handlers.
        _pubsubOutputHandlers.put(neighbour.getUUID(), handler);
        _pubsubOutputHandlerLockManager.unlockExclusive();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createPubSubOutputHandler", handler);

        return handler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getAllPubSubOutputHandlers()
     */
    public HashMap<SIBUuid8, PubSubOutputHandler> getAllPubSubOutputHandlers()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getAllPubSubOutputHandlers");

        _pubsubOutputHandlerLockManager.lock();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getAllPubSubOutputHandlers", _pubsubOutputHandlers);

        return _pubsubOutputHandlers;
    }

    /**
     * Makes a copy of the PSOH map as it currently
     * stands and does not leave it locked.
     * 
     * @return
     * @author tpm
     */
    public HashMap<SIBUuid8, PubSubOutputHandler> cloneAllPubSubOutputHandlers()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "cloneAllPubSubOutputHandlers");

        HashMap<SIBUuid8, PubSubOutputHandler> clone = null;
        try
        {
            _pubsubOutputHandlerLockManager.lock();
            clone = (HashMap<SIBUuid8, PubSubOutputHandler>) _pubsubOutputHandlers.clone();
        } finally
        {
            _pubsubOutputHandlerLockManager.unlock();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "cloneAllPubSubOutputHandlers", clone);
        return clone;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#unlockPubsubOutputHandlers()
     */
    public void unlockPubsubOutputHandlers()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "unlockPubsubOutputHandlers");

        _pubsubOutputHandlerLockManager.unlock();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "unlockPubsubOutputHandlers");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#deletePubSubOutputHandler(com.ibm.ws.sib.utils.SIBUuid8)
     */
    public synchronized void deletePubSubOutputHandler(SIBUuid8 neighbourUUID)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deletePubSubOutputHandler", neighbourUUID);

        // If we're being deleted then the output handlers are removed
        // through a different path, otherwise we may make a stream
        // unflushable by removing the only link to the downstream node
        // capable of flushing the stream.
        if (_baseDestinationHandler.isToBeDeleted())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "deletePubSubOutputHandler");
            return;
        }

        _pubsubOutputHandlerLockManager.lockExclusive();
        // Remove the PubSubOutputHandler from the list of output handlers available.
        _pubsubOutputHandlers.remove(neighbourUUID);
        _pubsubOutputHandlerLockManager.unlockExclusive();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deletePubSubOutputHandler");
    }

    /**
     * Convenience method used by the flush after delete code to clean
     * up the output handlers once we know they've been flushed and
     * are safe to delete.
     */
    public synchronized void deleteAllPubSubOutputHandlers()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deleteAllPubSubOutputHandlers");

        _pubsubOutputHandlerLockManager.lockExclusive();
        _pubsubOutputHandlers.clear();
        _pubsubOutputHandlerLockManager.unlockExclusive();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteAllPubSubOutputHandlers");
    }

    /**
     * Attempt to create a durable subscription on the local ME.
     * 
     * @param subState State describing the subscription to create.
     * @param transactoin The context within which the subscription should be created.
     */
    public void createLocalDurableSubscription(
                                               ConsumerDispatcherState subState,
                                               TransactionCommon transaction)
                    throws
                    SIDurableSubscriptionAlreadyExistsException,
                    SIDiscriminatorSyntaxException,
                    SISelectorSyntaxException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "createLocalDurableSubscription",
                        new Object[] { subState, transaction });

        // Create new getCursor and consumer dispatchers to return to the calling method
        ConsumerDispatcher consumerDispatcher = null;

        /*
         * Lock the list of durable subscriptions to prevent others creating/using an
         * identical subscription
         */
        synchronized (_consumerDispatchersDurable)
        {
            // If there's already a consumer dispatcher with this client name, then the
            // create must fail according to the current core API semantics.
            consumerDispatcher = getDurableSubscriptionConsumerDispatcher(subState);

            if (consumerDispatcher != null)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(
                               tc,
                               "createLocalDurableSubscription",
                               "Sub Already Exists - " + subState);

                throw new SIDurableSubscriptionAlreadyExistsException(
                                nls.getFormattedMessage(
                                                        "SUBSCRIPTION_ALREADY_EXISTS_ERROR_CWSIP0143",
                                                        new Object[] {
                                                                      subState.getSubscriberID(),
                                                                      _messageProcessor.getMessagingEngineName() },
                                                        null));
            }
            /* We did not find the consumerDispatcher so we create it */

            /*
             * If a transaction was not provided...
             * Create a new LocalTransaction to perform the creation of a durable subscription.
             * Operations will include the creation of the itemstream,the persisting of
             * the durable state and the attaching of the consumerpoint.
             */
            boolean tranProvided = true;
            LocalTransaction siTran = null;

            if (transaction == null)
            {
                tranProvided = false;
                siTran =
                                _baseDestinationHandler
                                                .getTransactionManager()
                                                .createLocalTransaction(false);
                transaction = siTran;
            }

            /*
             * Create the consumerDispatcher, note we do not require this to be done under
             * the localTransaction. If something goes wrong after this operation, the
             * consumerDispatcher will be lost but thats ok.
             */

            Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(transaction);

            try
            {
                /*
                 * Create a reference stream to be used by the durable subscription's
                 * consumer dispatcher
                 */
                final DurableSubscriptionItemStream durableSubscriptionReferenceStream =
                                new DurableSubscriptionItemStream(
                                                subState,
                                                _baseDestinationHandler.getDestinationManager(),
                                                _pubsubMessageItemStream,
                                                msTran);

                /* Create the consumer dispatcher to represent the subscription */
                consumerDispatcher =
                                configureDurableSubscription(durableSubscriptionReferenceStream);
                consumerDispatcher.registerControlAdapterAsMBean();

                //Venu mock mock
                // No ProxyHandler .. so commenting our this 
                /*
                 * // 166833.2
                 * // Generate the subscribeEvent for this new subscription
                 * // to see if this needs to be forwarded to a Neighbouring ME.
                 * _messageProcessor.getProxyHandler().subscribeEvent(
                 * subState,
                 * msTran);
                 */
            } catch (OutOfCacheSpace e)
            {
                // No FFDC code needed
                SibTr.exception(tc, e);
                // Rollback the transaction
                if (!tranProvided)
                    _baseDestinationHandler.handleRollback(siTran);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(
                               tc,
                               "createLocalDurableSubscription",
                               "SIResourceException");
                throw new SIResourceException(
                                nls.getFormattedMessage(
                                                        "SUBSCRIPTION_CREATION_FAILED_CWSIP0033",
                                                        new Object[] {
                                                                      _baseDestinationHandler.getName(),
                                                                      subState.getSubscriberID(),
                                                                      e },
                                                        null),
                                e);

            } catch (MessageStoreException e)
            {
                // MessageStoreException shouldn't occur so FFDC.
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.createLocalDurableSubscription",
                                            "1:1093:1.35.2.4",
                                            this);
                SibTr.exception(tc, e);
                // Rollback the transaction
                if (!tranProvided)
                    _baseDestinationHandler.handleRollback(siTran);

                /*
                 * The message store has thrown an exception, which leaves us stuck. Record an ffst,
                 * write a trace record and throw a SIResourceException to the caller.
                 */
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "createLocalDurableSubscription", e);

                SibTr.error(
                            tc,
                            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.createLocalDurableSubscription",
                                          "1:1112:1.35.2.4",
                                          e,
                                          _baseDestinationHandler.getName() });

                throw new SIResourceException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.destination.PubSubRealization",
                                                                      "1:1121:1.35.2.4",
                                                                      e,
                                                                      _baseDestinationHandler.getName() },
                                                        null),
                                e);
            } catch (SIDiscriminatorSyntaxException e)
            {
                // No FFDC code needed
                _baseDestinationHandler.handleRollback(siTran);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "createLocalDurableSubscription", e);
                throw e;
            } catch (SISelectorSyntaxException e)
            {
                // No FFDC code needed
                _baseDestinationHandler.handleRollback(siTran);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "createLocalDurableSubscription", e);
                throw e;
            }
            //Venu mock mock
            //commenting out as this exception became unreachable
            /*
             * catch (SIResourceException e)
             * {
             * // No FFDC code needed
             * _baseDestinationHandler.handleRollback(siTran);
             * if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
             * SibTr.exit(tc, "createLocalDurableSubscription", e);
             * throw e;
             * }
             */
            catch (RuntimeException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.createLocalDurableSubscription",
                                            "1:1153:1.35.2.4",
                                            this);

                if (siTran != null)
                    _baseDestinationHandler.handleRollback(siTran);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "createLocalDurableSubscription", e);
                throw e;
            }

            if (!tranProvided)
            {
                /*
                 * If no transaction was provided, this call must have been made from
                 * the core api (i.e. JMS). We can not provide transaction at runtime.
                 * Since we are creating via JMS, we need to attach the consumer
                 * as part of the create operation.
                 */
                try
                {

                    /* We have our own transaction so commit it here */
                    siTran.commit();

                } catch (SIException e)
                {
                    // MessageStoreException shouldn't occur so FFDC.
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.createLocalDurableSubscription",
                                                "1:1182:1.35.2.4",
                                                this);

                    SibTr.exception(tc, e);
                    SibTr.error(
                                tc,
                                "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                new Object[] {
                                              "com.ibm.ws.sib.processor.impl.destination.PubSubRealization",
                                              "1:1191:1.35.2.4",
                                              e,
                                              _baseDestinationHandler.getName() });

                    // Failed to attach consumer, rollback the creation of the durable sub
                    // and throw exception.
                    _baseDestinationHandler.handleRollback(siTran);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "createLocalDurableSubscription", e);

                    throw new SIResourceException(
                                    nls.getFormattedMessage(
                                                            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                                            new Object[] {
                                                                          "com.ibm.ws.sib.processor.impl.destination.PubSubRealization",
                                                                          "1:1207:1.35.2.4",
                                                                          e,
                                                                          _baseDestinationHandler.getName() },
                                                            null),
                                    e);
                }
            }
            // Transaction was provided

        } // Release the durable subs lock

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createLocalDurableSubscription");
    }

    /**
     * Attaches to a created DurableSubscription which is homed on
     * the local ME.
     * 
     * @param consumerPoint The consumer point to attach.
     * @param subState The ConsumerDispatcherState describing the subscription.
     */
    public ConsumableKey attachToLocalDurableSubscription(
                                                          LocalConsumerPoint consumerPoint,
                                                          ConsumerDispatcherState subState)
                    throws
                    SIDurableSubscriptionMismatchException,
                    SIDurableSubscriptionNotFoundException,
                    SIDestinationLockedException,
                    SISelectorSyntaxException,
                    SIDiscriminatorSyntaxException,
                    SINotPossibleInCurrentConfigurationException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "attachToLocalDurableSubscription",
                        new Object[] { consumerPoint, subState });

        ConsumerDispatcher consumerDispatcher = null;
        ConsumableKey data = null;
        /*
         * Lock the list of durable subscriptions to prevent others creating/using an
         * identical subscription
         */
        synchronized (_consumerDispatchersDurable)
        {
            /*
             * Get the consumerDispatcher. If it does not exist then we create it. If it exists
             * already and is EXACTLY the same, then we connect to it. If it exists but only
             * the same in ID then throw exception.
             */
            consumerDispatcher = getDurableSubscriptionConsumerDispatcher(subState);

            if (consumerDispatcher != null)
            {
                // If subscription already has consumers attached then reject the create
                // unless we are setting up a cloned subscriber.
                if (consumerDispatcher.hasConsumersAttached() && !subState.isCloned())
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(
                                   tc,
                                   "attachToLocalDurableSubscription",
                                   "Consumers already attached");

                    throw new SIDestinationLockedException(
                                    nls.getFormattedMessage(
                                                            "SUBSCRIPTION_IN_USE_ERROR_CWSIP0152",
                                                            new Object[] {
                                                                          subState.getSubscriberID(),
                                                                          _messageProcessor.getMessagingEngineName() },
                                                            null));
                }

                // Attach to the consumerDispatcher if it is EXACTLY the same
                if (!consumerDispatcher.getConsumerDispatcherState().isReady()
                    || !consumerDispatcher.getConsumerDispatcherState().equals(subState))
                {
                    // Found consumer dispatcher but only the IDs match, therefore cannot connect
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "attachToLocalDurableSubscription", subState);

                    throw new SIDurableSubscriptionMismatchException(
                                    nls.getFormattedMessage(
                                                            "SUBSCRIPTION_ALREADY_EXISTS_ERROR_CWSIP0143",
                                                            new Object[] {
                                                                          subState.getSubscriberID(),
                                                                          _messageProcessor.getMessagingEngineName() },
                                                            null));
                }

                // If security is enabled, then check the user who is attempting
                // to attach matches the user who created the durable sub.
                if (_messageProcessor.isBusSecure())
                {
                    if (!consumerDispatcher
                                    .getConsumerDispatcherState()
                                    .equalUser(subState))
                    {
                        // Users don't match
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "attachToLocalDurableSubscription", subState);

                        throw new SIDurableSubscriptionMismatchException(
                                        nls.getFormattedMessage(
                                                                "USER_NOT_AUTH_ACTIVATE_ERROR_CWSIP0312",
                                                                new Object[] {
                                                                              subState.getUser(),
                                                                              subState.getSubscriberID(),
                                                                              _baseDestinationHandler.getName() },
                                                                null));
                    }
                }

                // Attach the consumerpoint to the subscription.
                data =
                                (ConsumableKey) consumerDispatcher.attachConsumerPoint(
                                                                                       consumerPoint,
                                                                                       null,
                                                                                       consumerPoint.getConsumerSession().getConnectionUuid(),
                                                                                       consumerPoint.getConsumerSession().getReadAhead(),
                                                                                       consumerPoint.getConsumerSession().getForwardScanning(),
                                                                                       null);
            } else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(
                               tc,
                               "attachToLocalDurableSubscription",
                               "SIDurableSubscriptionNotFoundException");
                throw new SIDurableSubscriptionNotFoundException(
                                nls.getFormattedMessage(
                                                        "SUBSCRIPTION_DOESNT_EXIST_ERROR_CWSIP0146",
                                                        new Object[] {
                                                                      subState.getSubscriberID(),
                                                                      _messageProcessor.getMessagingEngineName() },
                                                        null));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "attachToLocalDurableSubscription", data);

        return data;
    }

    /**
     * Delete a durable subscription homed on this ME.
     * 
     * @param subscriptionId The subscription name (of the form client##name) to remove.
     * @throws SIDestinationNotFoundException
     * @throws SIResourceException
     * @throws SIMPDestinationLockedException
     */
    public void deleteLocalDurableSubscription(
                                               String subscriptionId)
                    throws
                    SIResourceException,
                    SIMPDestinationLockedException,
                    SIDurableSubscriptionNotFoundException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "deleteLocalDurableSubscription",
                        new Object[] { subscriptionId });

        // Lock the table of durable subscriptions to prevent others accessing the subscription 
        // We must perform the following work inside the sync block, in the following
        // order to protect against any chance of a duplicate subscription:
        // - Find the ConsumerDispatcher from the map
        // - Find the matching subscription item stream
        // - Persist the toBeDeleted flag on the item stream, so that it can never pop
        //   back into existence in the future.
        // - Remove the ConsumerDispatcher from both the map and subscription index,
        //   allowing a new subscription to be created with the same ID
        // We can then release the lock before attempting to cleanup the messages on the
        // existing subscription.
        DurableSubscriptionItemStream rstream = null;
        synchronized (_consumerDispatchersDurable)
        {
            // Get the consumer dispatcher from the table of durable subscriptions
            ConsumerDispatcher consumerDispatcher =
                            (ConsumerDispatcher) _consumerDispatchersDurable.get(subscriptionId);

            if (consumerDispatcher == null
                || !consumerDispatcher.getConsumerDispatcherState().isReady())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(
                               tc,
                               "deleteLocalDurableSubscription",
                               "Sub not found " + subscriptionId);
                throw new SIDurableSubscriptionNotFoundException(
                                nls.getFormattedMessage(
                                                        "SUBSCRIPTION_DOESNT_EXIST_ERROR_CWSIP0146",
                                                        new Object[] {
                                                                      subscriptionId,
                                                                      _messageProcessor.getMessagingEngineName() },
                                                        null));
            }

            // If consumers are attached to this consumerDispatcher then reject the delete.
            if (consumerDispatcher.hasConsumersAttached())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(
                               tc,
                               "deleteLocalDurableSubscription",
                               " SIDestinationLockedException" + subscriptionId);

                throw new SIMPDestinationLockedException(// defect 177277
                nls.getFormattedMessage(
                                        "SUBSCRIPTION_IN_USE_ERROR_CWSIP0152",
                                        new Object[] {
                                                      subscriptionId,
                                                      _messageProcessor.getMessagingEngineName() },
                                        null),
                                SIMPDestinationLockedException.CONSUMERS_ATTACHED);
            }

            // Now find the matching item stream, using the consumer dispatcher
            final SubscriptionStateFilter subStateFilter =
                            new SubscriptionStateFilter();
            subStateFilter.setConsumerDispatcherStateFilter(consumerDispatcher.getConsumerDispatcherState());

            try
            {
                // Find the subscription in the topicspace itemstream
                rstream = (DurableSubscriptionItemStream) _pubsubMessageItemStream.
                                findFirstMatchingReferenceStream(subStateFilter);

                // Check there are no in-doubt receives on this stream.
                // Note this does not prevent us deleting a stream with in-doubt sends, which
                // is correct as producers sending to a subscription shouldn't prevent the
                // subscriber from deleting its subscription.
                if (rstream != null)
                {
                    if (rstream.getStatistics().getRemovingItemCount() != 0)
                    {
                        // There are uncommitted gets on this stream.           // 177786
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(
                                       tc,
                                       "deleteLocalDurableSubscription",
                                       "SIDestinationLockedException"
                                                       + rstream.getStatistics().getUnavailableItemCount()
                                                       + ":"
                                                       + subscriptionId);

                        throw new SIMPDestinationLockedException(
                                        nls.getFormattedMessage("SUBSCRIPTION_IN_USE_ERROR_CWSIP0153",
                                                                new Object[] { subscriptionId, _messageProcessor.getMessagingEngineName() },
                                                                null),
                                        SIMPDestinationLockedException.UNCOMMITTED_MESSAGES);
                    }
                }

            } catch (MessageStoreException e)
            {
                // MessageStoreException shouldn't occur so FFDC.
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.deleteLocalDurableSubscription",
                                            "1:1472:1.35.2.4",
                                            this);

                SibTr.exception(tc, e);
                SibTr.error(
                            tc,
                            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.destination.PubSubRealization",
                                          "1:1481:1.35.2.4",
                                          e,
                                          subscriptionId });

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "deleteLocalDurableSubscription", e);

                throw new SIResourceException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.destination.PubSubRealization",
                                                                      "1:1493:1.35.2.4",
                                                                      e,
                                                                      subscriptionId },
                                                        null),
                                e);
            }

            // Mark the subscription to be deleted, and persist the flag
            boolean persistingToBeDeletedFlag = false;
            try
            {
                if (rstream != null)
                {
                    rstream.markAsToBeDeleted();
                    if (rstream.isInStore())
                    {
                        // If we fail while performing the persistent update,
                        // we need to rollback the toBeDeleted mark.
                        persistingToBeDeletedFlag = true;
                        rstream.requestUpdate(
                                        _messageProcessor.getTXManager().createAutoCommitTransaction());
                        persistingToBeDeletedFlag = false;
                    }
                }
            } catch (MessageStoreException e)
            {
                // MessageStoreException shouldn't occur so FFDC.
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.deleteLocalDurableSubscription",
                                            "1:1523:1.35.2.4",
                                            this);

                SibTr.exception(tc, e);
                SibTr.error(
                            tc,
                            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.destination.PubSubRealization",
                                          "1:1532:1.35.2.4",
                                          e,
                                          subscriptionId });

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "deleteLocalDurableSubscription", e);

                throw new SIResourceException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.destination.PubSubRealization",
                                                                      "1:1544:1.35.2.4",
                                                                      e,
                                                                      subscriptionId },
                                                        null),
                                e);
            } finally
            {
                if (persistingToBeDeletedFlag)
                {
                    // We failed while persisting the toBeDeleted flag.
                    // We cannot remove the subscription from the in-memory tables, as otherwise
                    // we could have a duplicate subscription on restart if the user created a new one.
                    // As such we unmark the deletion before throwing the exception, allowing the user
                    // to continue to use this subscription (and attempt the deletion again later).
                    rstream.clearToBeDeleted();
                }
            }

            // Now we can safely remove this subscription from our map, allowing the
            // user to create a new subscription with the same ID.
            // Remove consumerDispatcher from durable subscriptions table
            _consumerDispatchersDurable.remove(subscriptionId);

            // Delete consumerDispatcher ( implicit remove from matchspace) 
            try
            {
                // Delete from matchspace and send proxy message
                consumerDispatcher.deleteConsumerDispatcher(
                                true);
            } catch (SIException e)
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.deleteLocalDurableSubscription",
                                            "1:1580:1.35.2.4",
                                            this);

                SibTr.exception(tc, e);

                // Could not delete consumer dispatcher
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "deleteLocalDurableSubscription", e);
            }

        } // Drop synchronization on durable subscription map

        // If all was well, attempt cleanup of the subscription.
        if (rstream != null)
        {
            // Synchronization and exception handling all dealt with in SubscriptionItemStream
            rstream.deleteIfPossible(true);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteLocalDurableSubscription", subscriptionId);

    }

    /**
     * Reconstitutes all durable subscriptions for this destination. Before this
     * method is called, the durable subscriptions itemstream must have been
     * recovered and setDurableSubscriptionResources() must have been called.
     * 
     * @throws MessageStoreException
     * @throws SIDiscriminatorSyntaxException
     * @throws SIResourceException
     * @throws SISelectorSyntaxException
     */
    private void reconstituteDurableSubscriptions()
                    throws
                    MessageStoreException,
                    SIDiscriminatorSyntaxException,
                    SISelectorSyntaxException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "reconstituteDurableSubscriptions");

        // First create a filter to locate subscriptions for this destination
        DurableSubscriptionItemStream durableSub = null;
        final SubscriptionStateFilter subStateFilter =
                        new SubscriptionStateFilter();

        /*
         * Get a browse cursor on the topicspace itemstream to allow us to search
         * for all the persisted durable sub items. Find the first one.
         */
        NonLockingCursor subCursor =
                        _pubsubMessageItemStream.newNonLockingReferenceStreamCursor(
                                        subStateFilter);

        /* Find the first subscription in the itemstream */
        durableSub = (DurableSubscriptionItemStream) subCursor.next();

        /* Loop through the persisted durable subscriptions */
        while (durableSub != null)
        {
            //we need to restore this item stream
            durableSub.initializeNonPersistent(_baseDestinationHandler);
            // Increment the count of refStreams on the parentMsgStream 
            _pubsubMessageItemStream.incrementReferenceStreamCount();

            boolean valid = checkDurableSubStillValid(durableSub);
            if (valid)
            {
                // Create a new ConsumerDispatcher for this durable subscription with the
                // recovered state item
                configureDurableSubscription(durableSub);
                //defect 257231
                //we need to ensure that unrestored msgRefs (due to in-doubt transactions)
                //are initialized with the CD and RefStream call backs
                Collection unrestoredMsgCollection =
                                durableSub.clearUnrestoredMessages();
                if (unrestoredMsgCollection != null)
                {
                    Iterator indoubtMessages = unrestoredMsgCollection.iterator();
                    while (indoubtMessages.hasNext())
                    {
                        MessageItemReference itemRef =
                                        (MessageItemReference) _baseDestinationHandler.findById(
                                                        ((Long) indoubtMessages.next()).longValue());
                        if (itemRef != null)
                        {
                            durableSub.registerListeners(itemRef);
                        }
                    }
                }
            }

            /* Get the next subscription in the itemstream */
            durableSub = (DurableSubscriptionItemStream) subCursor.next();

        } // Loop round if more subscriptions to recover

        subCursor.finished();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconstituteDurableSubscriptions");
    }

    private ConsumerDispatcher configureDurableSubscription(DurableSubscriptionItemStream durableSub)
                    throws SIDiscriminatorSyntaxException, SISelectorSyntaxException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "configureDurableSubscription", durableSub);

        // Create the consumer dispatcher to represent the subscription
        final ConsumerDispatcher consumerDispatcher =
                        createConsumerDispatcher(
                                                 durableSub.getConsumerDispatcherState(),
                                                 durableSub);

        // Put the consumerDispatcher into the table of durable subcriptions
        // Lock the table of durable subscriptions to prevent concurrent access
        synchronized (_consumerDispatchersDurable)
        {
            _consumerDispatchersDurable.put(
                                            durableSub.getConsumerDispatcherState().getSubscriberID(),
                                            consumerDispatcher);
        }
        // Put the consumer into the matchspace
        // We need an entry for each selectionCriteria on the DurableSubscription
        // The matchspace code will remove duplicates and only deliver a message
        // once to a ConsumerDispatcher (hopefully)
        SelectionCriteria[] selCriteria = durableSub.getConsumerDispatcherState().getSelectionCriteriaList();

        if (selCriteria != null)
        {
            for (int i = 0; i < selCriteria.length; i++)
            {
                _messageProcessor
                                .getMessageProcessorMatching()
                                .addConsumerDispatcherMatchTarget(
                                                                  consumerDispatcher,
                                                                  _baseDestinationHandler.getUuid(),
                                                                  selCriteria[i]);
            }
        }
        _subscriptionIndex.put(consumerDispatcher);
        consumerDispatcher.getControlAdapter().registerControlAdapterAsMBean();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "configureDurableSubscription", consumerDispatcher);

        return consumerDispatcher;
    }

    /**
     * Removes the subscription from the MatchSpace and removes the associated itemstream
     * if non durable.
     * 
     * @param cd The consumer dispatcher
     * @param isNonDurable If a nondurable subscription.
     * @param callProxyCode If we need to call the proxy code at all.
     * 
     */
    public void dereferenceSubscriptionConsumerDispatcher(
                                                          ConsumerDispatcher cd,
                                                          boolean isNonDurable,
                                                          boolean callProxyCode)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "dereferenceSubscriptionConsumerDispatcher",
                        new Object[] {
                                      cd,
                                      new Boolean(isNonDurable),
                                      new Boolean(callProxyCode) });

        try
        {
            _subscriptionIndex.remove(cd);

            // Remove all entries for the consumerDispatcher from the matchspace

            SelectionCriteria[] selCriteria = cd.getConsumerDispatcherState().getSelectionCriteriaList();

            if (selCriteria != null)
            {
                for (int i = 0; i < selCriteria.length; i++)
                {
                    // Remove the CD from the MatchSpace
                    _messageProcessor
                                    .getMessageProcessorMatching()
                                    .removeConsumerDispatcherMatchTarget(cd, selCriteria[i]);
                }
            }

            // 166833.2
            // Calls to the proxy code only need to occur on a non durable delete
            // subscription, or a createDurableSubscription rolled back.
            // It won't be called in the case that a durableSubscription.
            // eventCommittedRemove as the proxy message will have already been sent.

            //716917
            //isNonDurable was passed insted of callProxyCode
            //Due to which always false was being passed when we had durable subscriber, hence
            //the unsubscribe events was not being sent to neighbouring ME's when durable subscriber was unsubscribed
            //Its an Fix to 166833.2
//    sanjay liberty change
//      if (callProxyCode)
//        _messageProcessor.getProxyHandler().unsubscribeEvent(
//          cd.getConsumerDispatcherState(),
//          null,
//          callProxyCode);
//
//      // 175488
            if (isNonDurable)
            {
                // Need to attempt cleanup of the subscription.  If it cant be completed
                // now, it will be attempted by the asynch-deletion thread at a later
                // time.

                try
                {
                    //remove the CD from ME wide hash map maintained for non durable shared consumers in Destination Manager.
                    // All non-durable shared consumer's subscriberID do not start with _NON_DURABLE_NON_SHARED
                    if (!(cd.getConsumerDispatcherState().getSubscriberID().contains("_NON_DURABLE_NON_SHARED")))
                        _destinationManager.getNondurableSharedSubscriptions().remove(cd.getConsumerDispatcherState().getSubscriberID());
                    // Find the subscription in the topicspace itemstream
                    final SubscriptionItemStream subscription =
                                    (
                                    SubscriptionItemStream) _pubsubMessageItemStream
                                                    .findFirstMatchingReferenceStream(
                                                    new SubscriptionFilter(cd));

                    // Remove any items on the subscription reference stream, and remove the stream itself
                    if (subscription != null)
                    {
                        //Mark the subscription as requiring deletion
                        subscription.markAsToBeDeleted();

                        //Drive the deletion of the subscription now.  It may not be possible to
                        //complete it now if there are indoubt messages on the subscription, in
                        //which case the commit/backout of those messages will drive the delete.
                        subscription.deleteIfPossible(true);
                    }
                } catch (MessageStoreException e)
                {
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.dereferenceSubscriptionConsumerDispatcher",
                                                "1:1835:1.35.2.4",
                                                this);

                    SibTr.exception(tc, e);
                    SibTr.error(
                                tc,
                                "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                new Object[] {
                                              "com.ibm.ws.sib.processor.impl.destination.PubSubRealization",
                                              "1:1844:1.35.2.4",
                                              e,
                                              cd.getConsumerDispatcherState().getSubscriberID() });

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "dereferenceSubscriptionConsumerDispatcher");

                    throw new SIResourceException(
                                    nls.getFormattedMessage(
                                                            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                                            new Object[] {
                                                                          "com.ibm.ws.sib.processor.impl.destination.PubSubRealization",
                                                                          "1:1856:1.35.2.4",
                                                                          e,
                                                                          cd.getConsumerDispatcherState().getSubscriberID() },
                                                            null),
                                    e);
                }
            } // end of 175488
        } catch (SIException e)
        {
            // Exceptions should not be thrown by proxy handler code
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.dereferenceSubscriptionConsumerDispatcher",
                                        "1:1869:1.35.2.4",
                                        this);

            SibTr.exception(tc, e);
            SibTr.error(
                        tc,
                        "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.destination.PubSubRealization",
                                      "1:1878:1.35.2.4",
                                      e,
                                      cd.getConsumerDispatcherState().getSubscriberID() });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "dereferenceSubscriptionConsumerDispatcher");

            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.destination.PubSubRealization",
                                                                  "1:1890:1.35.2.4",
                                                                  e,
                                                                  cd.getConsumerDispatcherState().getSubscriberID() },
                                                    null),
                            e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "dereferenceSubscriptionConsumerDispatcher");
    }

    /**
     * Add PubSubLocalisation.
     * 
     */
    public void addPubSubLocalisation(LocalizationDefinition destinationLocalizationDefinition)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "addPubSubLocalisation",
                        new Object[] { destinationLocalizationDefinition });

        _pubsubMessageItemStream.updateLocalizationDefinition(
                        destinationLocalizationDefinition);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addPubSubLocalisation");

    }

    /**
     * Mark the DestinationHandler for deletion.
     */
    @Override
    public void setToBeDeleted()
    {
        super.setToBeDeleted();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setToBeDeleted");

        // If there are durable subscriptions associated with the TopicSpace that is to be deleted
        // then we dereference them at this stage. This will remove entries from the matchspace.
        synchronized (_consumerDispatchersDurable)
        {
            // Iterate over the table of durable subscriptions.
            Iterator<Object> iter = _consumerDispatchersDurable.values().iterator();
            while (iter.hasNext())
            {
                Object obj = iter.next();
                // May not be a ConsumerDispatcher instance. eg may be a PendingDurableDelete object.
                if (obj instanceof ConsumerDispatcher)
                {
                    ConsumerDispatcher cd = (ConsumerDispatcher) obj;

                    // Does the CD belong to this destination
                    if (cd.getDestination().equals(_baseDestinationHandler))
                    {
                        // If the ConsumerDispatcher is in the MatchSpace, then dereference it but only if it
                        // doesn't currently have consumers attached (defect 518845)
                        if (cd.isInMatchSpace()
                            && !cd.hasConsumersAttached())
                        {
                            try
                            {
                                dereferenceSubscriptionConsumerDispatcher(
                                                                          cd,
                                                                          !cd.isDurable(),
                                                                          false); // don't call proxy code at this point
                            } catch (SIResourceException e)
                            {
                                // No FFDC code needed

                                // Trace the exception, we'll have FFDC'ed already and we want to allow
                                // processing to continue.
                                SibTr.exception(tc, e);
                            }
                        } //eof isInMatchSpace
                    } // eof matching DestinationHandlers
                } // eof instanceof ConsumerDispatcher
            } // eof while
        } // eof sync

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setToBeDeleted");
    }

    /**
     * Retrieve the subscription index for this destination.
     * 
     * @return SubscriptionIndex.
     */
    public SubscriptionIndex getSubscriptionIndex()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getSubscriptionIndex");
            SibTr.exit(tc, "getSubscriptionIndex", _subscriptionIndex);
        }
        return _subscriptionIndex;
    }

    /**
     * Method createInputHandlersForPubSub.
     * <p>Create the PubSub and preMediated InputHandlers.</p>
     */
    private void createInputHandlersForPubSub()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createInputHandlersForPubSub");

        // Create the pub-sub input handler.
        _baseDestinationHandler.setInputHandler(
                        new PubSubInputHandler(
                                        _baseDestinationHandler,
                                        _pubSubRemoteSupport.getTargetProtocolItemStream(),
                                        _pubsubMessageItemStream,
                                        _proxyReferenceStream,
                                        _pubSubRemoteSupport.getSourceProtocolItemStream()));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createInputHandlersForPubSub");
    }

    public void cleanupDestination()
                    throws
                    SIRollbackException,
                    SIConnectionLostException,
                    SIIncorrectCallException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "cleanupDestination");

        ItemStream debugItemStream = null;
        try
        {
            //The destination can be deleted only when all the subscribers have
            //been cleaned up.  Attempt clean-up on each subscription
            NonLockingCursor cursor =
                            _pubsubMessageItemStream.newNonLockingReferenceStreamCursor(
                                            new ClassEqualsFilter(
                                                            SubscriptionItemStream.class,
                                                            DurableSubscriptionItemStream.class));
            SubscriptionItemStream subscription =
                            (SubscriptionItemStream) cursor.next();

            HashMap durableSubs = _destinationManager.getDurableSubscriptionsTable();
            synchronized (durableSubs)
            {
                while (subscription != null)
                {
                    try {
                        deleteLocalDurableSubscription(subscription.
                                        getConsumerDispatcher().
                                        getConsumerDispatcherState().
                                        getSubscriberID());
                    } catch (SIException e)
                    {
                        FFDCFilter.processException(
                                                    e,
                                                    "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.cleanupDestination",
                                                    "1:2074:1.35.2.4",
                                                    this);

                        SibTr.exception(tc, e);
                    }
                    subscription = (SubscriptionItemStream) cursor.next();
                }
            }

            cursor.finished();

            //Determine if all the subscriptions were cleaned up by checking if there
            //are now any left.
            cursor =
                            _pubsubMessageItemStream.newNonLockingReferenceStreamCursor(
                                            new ClassEqualsFilter(
                                                            SubscriptionItemStream.class,
                                                            DurableSubscriptionItemStream.class));
            subscription = (SubscriptionItemStream) cursor.next();

            if (subscription == null)
            {
                //There are no subscriptions left on the topicspace.

                LocalTransaction siTran =
                                _baseDestinationHandler
                                                .getTransactionManager()
                                                .createLocalTransaction(false);

                // Remove any protocol items
                _pubSubRemoteSupport.removeProtocolItems(siTran);

                siTran.commit();
                siTran =
                                _baseDestinationHandler
                                                .getTransactionManager()
                                                .createLocalTransaction(false);

                _pubSubRemoteSupport.resetProtocolStreams();

                //Remove the proxyReferenceStream if there is one
                if (_proxyReferenceStream != null)
                {
                    //First ensure there are no batched deletes of proxy references that
                    //could stop the delete from working.
                    BatchHandler sourceBatchHandler =
                                    _messageProcessor.getSourceBatchHandler();
                    sourceBatchHandler.completeBatch(true);

                    // Useful debug
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    {
                        if (_proxyReferenceStream.isInStore())
                        {
                            Statistics stats = _proxyReferenceStream.getStatistics();
                            SibTr.debug(
                                        tc,
                                        "Destination :"
                                                        + _baseDestinationHandler.getName()
                                                        + " "
                                                        + _baseDestinationHandler.getUuid().toString()
                                                        + " Adding : "
                                                        + stats.getAddingItemCount()
                                                        + " Available : "
                                                        + stats.getAvailableItemCount()
                                                        + " Expiring : "
                                                        + stats.getExpiringItemCount()
                                                        + " Locked : "
                                                        + stats.getLockedItemCount()
                                                        + " Removing : "
                                                        + stats.getRemovingItemCount()
                                                        + " Total : "
                                                        + stats.getTotalItemCount()
                                                        + " Unavailable : "
                                                        + stats.getUnavailableItemCount()
                                                        + " Updating : "
                                                        + stats.getUpdatingItemCount());
                        }
                        else
                            SibTr.debug(
                                        tc, "Destination :"
                                            + _baseDestinationHandler.getName()
                                            + " "
                                            + _baseDestinationHandler.getUuid().toString());

                    }

                    Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(siTran);
                    _proxyReferenceStream.removeAll(msTran);

                    siTran.commit();
                    siTran =
                                    _baseDestinationHandler
                                                    .getTransactionManager()
                                                    .createLocalTransaction(false);
                    // Decrease the number of referenceStreams on the parent
                    _pubsubMessageItemStream.decrementReferenceStreamCount();
                    _proxyReferenceStream = null;
                }

                //Remove the messages itemstream
                //First ensure there are no batched deletes of publications that
                //could stop the delete from working.
                BatchHandler publicationBatchHandler =
                                _messageProcessor.getPublicationBatchHandler();
                publicationBatchHandler.completeBatch(true);

                Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(siTran);

                // Remove all the items which have 0 reference count
                // Defect 273912
                _pubsubMessageItemStream.removeAllItemsWithNoRefCount(msTran);

                _pubsubMessageItemStream.removeItemStream(
                                                          msTran,
                                                          AbstractItem.NO_LOCK_ID);

                debugItemStream = _pubsubMessageItemStream;

                // Everything is now deleted now, so the destinationHandler itself can
                // also be deleted.

                siTran.commit();
                siTran =
                                _baseDestinationHandler
                                                .getTransactionManager()
                                                .createLocalTransaction(false);

                msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(siTran);
                _baseDestinationHandler.remove(
                                               msTran,
                                               AbstractItem.NO_LOCK_ID);

                debugItemStream = _baseDestinationHandler;

                siTran.commit();

                //Remember that the destination has been deleted
                _baseDestinationHandler.setDeleted();

            }

            cursor.finished();

        } catch (MessageStoreException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.cleanupDestination",
                                        "1:2223:1.35.2.4",
                                        this);

            SibTr.exception(tc, e);

            // Useful debug
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                if (debugItemStream != null && debugItemStream.isInStore())
                {
                    Statistics stats = null;

                    try {
                        debugItemStream.xmlRequestWriteOnSystemOut();
                        stats = debugItemStream.getStatistics();

                        SibTr.debug(
                                    tc,
                                    "Destination :"
                                                    + _baseDestinationHandler.getName()
                                                    + " "
                                                    + debugItemStream.toString()
                                                    + " Adding : "
                                                    + stats.getAddingItemCount()
                                                    + " Available : "
                                                    + stats.getAvailableItemCount()
                                                    + " Expiring : "
                                                    + stats.getExpiringItemCount()
                                                    + " Locked : "
                                                    + stats.getLockedItemCount()
                                                    + " Removing : "
                                                    + stats.getRemovingItemCount()
                                                    + " Total : "
                                                    + stats.getTotalItemCount()
                                                    + " Unavailable : "
                                                    + stats.getUnavailableItemCount()
                                                    + " Updating : "
                                                    + stats.getUpdatingItemCount());

                    } catch (IOException e1) {
                        // No FFDC code needed
                        SibTr.debug(tc, "Could not output destination xml : " + e1);
                    } catch (MessageStoreException e1) {
                        // No FFDC code needed
                        SibTr.debug(tc, "Could not output destination xml : " + e1);
                    }
                }
                else
                    SibTr.debug(
                                tc, "Destination :"
                                    + _baseDestinationHandler.getName());

            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "cleanupDestination", "SIResourceException");
            throw new SIResourceException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "cleanupDestination");
    }

    /**
     * <p>Cleanup any localisations of the destination that require it</p>
     */
    @Override
    public boolean cleanupLocalisations() throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "cleanupLocalisations");

        boolean allCleanedUp = _pubSubRemoteSupport.cleanupLocalisations(_consumerDispatchersDurable);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "cleanupLocalisations", new Boolean(allCleanedUp));

        return allCleanedUp;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getPublishPoint()
     */
    public PubSubMessageItemStream getPublishPoint()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getPublishPoint");
            SibTr.exit(tc, "getPublishPoint", _pubsubMessageItemStream);
        }

        return _pubsubMessageItemStream;
    }

    public void updateLocalisationDefinition(LocalizationDefinition destinationLocalizationDefinition)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "updateLocalisationDefinition",
                        new Object[] { destinationLocalizationDefinition });

        _pubsubMessageItemStream.updateLocalizationDefinition(
                        destinationLocalizationDefinition);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updateLocalisationDefinition");
    }

    /**
     * This is a callback required by publication MessageItems to delete
     * themselves when their last MessageItemReference referencer disappears.
     * 
     * @param msg The message that the reference count reached 0 on.
     * 
     *            Feature 174199.2.12
     * 
     *            The messageAddCall boolean needs to be set after a right before a call to
     *            the batch handler messagesAdded so that in the result of an exception being
     *            thrown, the handler is unlocked.
     */
    public void itemReferencesDroppedToZero(SIMPMessage msg)
                    throws
                    SIRollbackException,
                    SIConnectionLostException,
                    SIIncorrectCallException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "itemReferencesDroppedToZero", msg);

        // Get the current transaction from the batchHandler under which to perform the remove
        BatchHandler batchHandler = _messageProcessor.getPublicationBatchHandler();
        TransactionCommon transaction = null;

        // If a COD report message is required, register the callback to send
        // the report. We don`t do this in the batch because the callback would
        // deadlock with the messageStore (190035)
        boolean batched = true;
        // A marker to indicate how far through the method we get.
        boolean messageAddCall = false;

        try
        {
            boolean localTransactionCreated = false;
            LocalTransaction localTransaction = null;

            if (msg.getReportCOD() != null)
            {
                localTransaction = _baseDestinationHandler.
                                getTransactionManager().
                                createLocalTransaction(false);
                transaction = localTransaction;
                localTransactionCreated = true;
                transaction.registerCallback(msg);
                batched = false;

            }
            // TODO Until Message Store make their spilling logic sophisticated enough to realise
            // that spilling messages in 'remove' state is a little pointless we're better off removing
            // them immediately (don't batch them). This reduces the possibility of them being spilt.
            else if (((MessageItem) msg).getStorageStrategy() <= AbstractItem.STORE_MAYBE)
            {
                transaction = _baseDestinationHandler.
                                getTransactionManager().
                                createAutoCommitTransaction();
                batched = false;
            } else
            {
                transaction = batchHandler.registerInBatch();
            }

            try
            {
                // Remove the message from the topicspace
                Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(transaction);
                msg.remove(msTran, msg.getLockID());

                if (localTransactionCreated)
                {
                    localTransaction.commit();
                }

                // note that a batch listener could be added here for a callback when
                // when the transaction commits
                if (batched)
                {
                    // Indicate that we have called the messagesAdded
                    messageAddCall = true;
                    batchHandler.messagesAdded(1);
                }
            } catch (MessageStoreRuntimeException e)
            {
                // No FFDC code needed

                // This exception code should be removed when Message Store have updated to stop throwing exceptions
                // if we try and remove a Best Effort message that has already been removed.

                // This might be a StateException so we can ignore this if it is a best effort message
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.exception(tc, e);

                // If this is a Best Effort message and it isn't in the store then that isn't
                // a problem as it has already been removed.
                if (msg.getReliability() != Reliability.BEST_EFFORT_NONPERSISTENT)
                {
                    // FFDC
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.itemReferencesDroppedToZero",
                                                "1:2432:1.35.2.4",
                                                this);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(
                                   tc,
                                   "itemReferencesDroppedToZero",
                                   "MsgStoreRuntimeException " + e);

                    throw new SIErrorException(
                                    nls.getFormattedMessage(
                                                            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                                            new Object[] {
                                                                          "com.ibm.ws.sib.processor.impl.destination.PubSubRealization",
                                                                          "1:2446:1.35.2.4",
                                                                          e,
                                                                          _baseDestinationHandler.getName() },
                                                            null),
                                    e);
                }
            } catch (MessageStoreException e)
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.itemReferencesDroppedToZero",
                                            "1:2457:1.35.2.4",
                                            this);

                SibTr.exception(tc, e);
                SibTr.error(
                            tc,
                            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.destination.PubSubRealization",
                                          "1:2466:1.35.2.4",
                                          e,
                                          _baseDestinationHandler.getName() });

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(
                               tc,
                               "itemReferencesDroppedToZero",
                               "ResourceException " + e);

                throw new SIResourceException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.destination.PubSubRealization",
                                                                      "1:2481:1.35.2.4",
                                                                      e,
                                                                      _baseDestinationHandler.getName() },
                                                        null),
                                e);
            }
        } finally
        {
            // Before exiting this method, need to unlock the batch handler if it was locked.
            if (batched && !messageAddCall)
                try
                {
                    batchHandler.messagesAdded(0);
                } catch (SIResourceException e)
                {
                    // No FFDC code needed, This will allow for any exceptions that were thrown to
                    // be rethrown instead of overiding with a batch handler error.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        SibTr.exception(tc, e);
                }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(
                           tc,
                           "itemReferencesDroppedToZero",
                           new Boolean(messageAddCall));
        }
    }

    @Override
    public void onExpiryReport()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "onExpiryReport");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "onExpiryReport");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener#registerForEvents(com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage)
     */
    public void registerForEvents(SIMPMessage msg)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "registerForEvents", msg);

        msg.registerMessageEventListener(MessageEvents.EXPIRY_NOTIFICATION, _baseDestinationHandler);
        msg.registerMessageEventListener(
                                         MessageEvents.REFERENCES_DROPPED_TO_ZERO,
                                         _baseDestinationHandler);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerForEvents");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getSubscriptionList()
     */
    public List<String> getSubscriptionList() throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getSubscriptionList");
        List<String> subscriptions = null;

        // Build up a list of subscriptions on the destination
        try
        {
            NonLockingCursor cursor =
                            _pubsubMessageItemStream.newNonLockingReferenceStreamCursor(
                                            new ClassEqualsFilter(
                                                            SubscriptionItemStream.class,
                                                            DurableSubscriptionItemStream.class));
            SubscriptionItemStream subscription =
                            (SubscriptionItemStream) cursor.next();

            while (subscription != null)
            {
                if (subscriptions == null)
                {
                    subscriptions = new LinkedList<String>();
                }
                subscriptions.add(
                                subscription
                                                .getConsumerDispatcher()
                                                .getConsumerDispatcherState()
                                                .getSubscriberID());
                subscription = (SubscriptionItemStream) cursor.next();
            }

            cursor.finished();
        } catch (MessageStoreException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.getSubscriptionList",
                                        "1:2579:1.35.2.4",
                                        this);

            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "getSubscriptionList", "SIResourceException");
            throw new SIResourceException(e);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getSubscriptionList", subscriptions);
        return subscriptions;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getProxyReferenceStream()
     */
    public ProxyReferenceStream getProxyReferenceStream()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getProxyReferenceStream");
            SibTr.exit(tc, "getProxyReferenceStream", _proxyReferenceStream);
        }
        return _proxyReferenceStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#registerForMessageEvents(com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage)
     */
    public void registerForMessageEvents(SIMPMessage msg)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "registerForMessageEvents", msg);

        registerForEvents(msg);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerForMessageEvents");
    }

    ////////////////////////////////////////////////////
    // Methods which support durable pub/sub
    ////////////////////////////////////////////////////

    /**
     * Handle a remote request to delete a local durable subscription.
     * 
     * @param subName The name of the durable subscription to delete.
     * @return DurableConstants.STATUS_OK if the delete works,
     *         DurableConstants.STATUS_SUB_NOT_FOUND if no such subscription
     *         exists, DurableConstants.STATUS_SUB_CARDINALITY_ERROR if
     *         the subscription can't be deleted because there are attached
     *         consumers, or DurableConstants.STATUS_SUB_GENERAL_ERROR if an
     *         exception occurs while deleting the subscription.
     */
    public int deleteDurableFromRemote(String subName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deleteDurableFromRemote", subName);

        int status = DurableConstants.STATUS_OK;

        synchronized (_consumerDispatchersDurable)
        {
            // Do we have a consumer dispatcher for this subscription?
            ConsumerDispatcher current =
                            (ConsumerDispatcher) _consumerDispatchersDurable.get(subName);

            if (current == null)
                // Nope.  Either it never existed or we've already deleted it.
                status = DurableConstants.STATUS_SUB_NOT_FOUND;
            else
            {
                // Yup, see if we can delete it.  If yes, then do so.
                // Otherwise send back a cardinality error.
                int count = current.getConsumerCount();
                if (count == 0)
                {
                    // First remove the consumer dispatcher
                    try
                    {
                        // Firstly complete the batch on the pubsub inputhandler
                        ((PubSubInputHandler) _baseDestinationHandler.getInputHandler())
                                        .forceTargetBatchCompletion(
                                        null);
                        deleteLocalDurableSubscription(subName);
                    } catch (SIMPDestinationLockedException e)
                    {
                        // No FFDC code needed
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "Durable subscription locked");

                        if (e.getType()
                        == SIMPDestinationLockedException.CONSUMERS_ATTACHED)
                            status = DurableConstants.STATUS_SUB_CARDINALITY_ERROR;
                        else
                            status = DurableConstants.STATUS_SIB_LOCKED_ERROR;
                    } catch (SIDurableSubscriptionNotFoundException e)
                    {
                        // No FFDC code needed
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "Durable subscription not found");

                        status = DurableConstants.STATUS_SUB_NOT_FOUND;
                    } catch (SIResourceException e)
                    {
                        // No FFDC code needed

                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "Durable subscription resource exception");

                        status = DurableConstants.STATUS_SUB_GENERAL_ERROR;
                    }
                } else
                    // cardinality error
                    status = DurableConstants.STATUS_SUB_CARDINALITY_ERROR;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteDurableFromRemote", new Integer(status));

        return status;
    }

    /**
     * Attach to a durable subscription homed on a remote ME.
     * 
     * @param subState Subscription state.
     * @param consumerPoint The consumer point to attach to the ConsumerDispatcher
     *            we create if the subscription is created successfully.
     * @return A ConsumerKey which may be used to reference the appropriate ConsumerDispatcher.
     * @throws SIDurableSubscriptionMismatchException
     */
    public ConsumableKey attachToRemoteDurableSubscription(
                                                           LocalConsumerPoint consumerPoint,
                                                           ConsumerDispatcherState subState,
                                                           SIBUuid8 durableMEUuid)
                    throws
                    SIDurableSubscriptionNotFoundException,
                    SIDestinationLockedException,
                    SISelectorSyntaxException,
                    SIDiscriminatorSyntaxException,
                    SINotPossibleInCurrentConfigurationException,
                    SIResourceException,
                    SIDurableSubscriptionMismatchException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "attachToRemoteDurableSubscription",
                        new Object[] { consumerPoint, subState, durableMEUuid });

        // We'll send the request to the remote ME, from which one of four replies may result:
        // 1) success
        // 2) failure because someone else is already attached
        // 3) failure because the subscription does not exist
        // 4) some other failure (storage problems, bugs, etc).

        //get the durable ME uuid
        String remSubName =
                        _baseDestinationHandler.constructPseudoDurableDestName(
                                                                               durableMEUuid.toString(),
                                                                               subState.getSubscriberID());

        //defect 259036: we do not need to create a consumer dispatcher
        //if one already exists and we are a cloned subscription
        boolean createConsumerDispatcher = true;

        ConsumableKey data = null;

        synchronized (_consumerDispatchersDurable)
        {
            // An existing ConsumerDispatcher implies an existing connected client which
            // means the connect attempt fails...unless we're waiting on a durable
            // subscription which is in the process of being deleted.  In which case we'll
            // see if the flush is expected (DME is up) and if so kick it a bit to give it
            // another chance (or ten)
            Object current = null;
            boolean tryAgain = true;
            int remainingAttempts = 10;

            // Keep trying this until it works, fails, or we get bored
            while (tryAgain)
            {
                tryAgain = false;

                // See if we already have an RCD for this subscription
                current = _consumerDispatchersDurable.get(remSubName);

                // If there is a RCD for this subscription but it's pending a delete try to
                // get in and cancel the delete, if we succeed we can re-use the RCD, otherwise
                // we'll drop out and try again in a bit, to give it a chance to complete
                // the delete.
                if (current != null)
                {
                    // We don't just store RCDs in this map - we stick in Strings to represent RCDs
                    // currently being created (!)
                    if (current instanceof RemoteConsumerDispatcher)
                    {
                        RemoteConsumerDispatcher rcd = (RemoteConsumerDispatcher) current;
                        // If the RCD is in pending delete state there's a chance we can get in and
                        // cancel the delete to re-use it. Or, at the very least we can get it to
                        // resend the flush if the DME is up but wasn't before.
                        if (rcd.getPendingDelete())
                        {
                            current = rcd.getResolvedDurableCD(rcd);

                            // A null returned indicates the RCD is not in a usable state (cleaning up)
                            // Otherwise, we've rescued the old RCD for further use 
                            if (current != null)
                                ((RemoteConsumerDispatcher) current).setPendingDelete(false);
                        }
                        // If it's not in pending delete, we'll try to use it
                    }
                    // A String represents an RCD/AIH that's being created. If the subscription is
                    // clonable (supports multiple consumers) we'll wait for the completion as we'll
                    // be able to use it. Otherwise, we couldn't use it even if it existed (receive
                    // exclusive) so bomb out now.
                    else if ((current instanceof String) && subState.isCloned())
                    {
                        // Set to null so we go round the loop again (after a delay)
                        current = null;
                    }

                    // If we don't have an RCD anymore (we did when we came in) we wait for a bit and
                    // try again (unless we've bored of waiting)
                    if (current == null)
                    {
                        remainingAttempts--;

                        // If we've already tried a few times, give up - something else has
                        // maybe gone wrong
                        if (remainingAttempts == 0)
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(
                                           tc,
                                           "attachToRemoteDurableSubscription",
                                           "Consumers already attached");

                            throw new SIDestinationLockedException(
                                            nls.getFormattedMessage(
                                                                    "SUBSCRIPTION_IN_USE_ERROR_CWSIP0152",
                                                                    new Object[] {
                                                                                  subState.getSubscriberID(),
                                                                                  _messageProcessor.getMessagingEngineName() },
                                                                    null));
                        }

                        // Otherwise, sleep for a little but (releasing the lock) and try again later.
                        // If the cleanup of the RCD/AIH completes while we're waiting we'll be notified.
                        try
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(tc, "Waiting on " + _consumerDispatchersDurable);
                            _consumerDispatchersDurable.wait(100);
                        } catch (InterruptedException e)
                        {
                            // No FFDC code needed
                        }
                        tryAgain = true;
                    }
                }
                else if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "No existing consumer dispatcher found");
            }

            // By this point we should either have something in our hands (an RCD or a String) for
            // us to use. Or we need to create a new one

            if (current != null)
            {
                createConsumerDispatcher = false;

                // If we have an RCD we try to attach to it, if we're not clonable and someone is already
                // attached we'll fail.
                if (current instanceof RemoteConsumerDispatcher)
                {
                    try
                    {
                        data =
                                        (ConsumableKey) ((RemoteConsumerDispatcher) current).attachConsumerPoint(
                                                                                                                 consumerPoint,
                                                                                                                 null,
                                                                                                                 consumerPoint.getConsumerSession().getConnectionUuid(),
                                                                                                                 consumerPoint.getConsumerSession().getReadAhead(),
                                                                                                                 consumerPoint.getConsumerSession().getForwardScanning(),
                                                                                                                 null);
                    } catch (SIResourceException e)
                    {
                        // No FFDC code needed

                        // See if there is a consumer already attached locally, to give a better exception
                        // text
                        if (((RemoteConsumerDispatcher) current).hasConsumersAttached())
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(
                                           tc,
                                           "attachToRemoteDurableSubscription",
                                           new Object[] { "Consumers already attached locally", e });

                            throw new SIDestinationLockedException(
                                            nls.getFormattedMessage(
                                                                    "SUBSCRIPTION_IN_USE_ERROR_CWSIP0152",
                                                                    new Object[] {
                                                                                  subState.getSubscriberID(),
                                                                                  _messageProcessor.getMessagingEngineName() },
                                                                    null));
                        }
                        // Otherwise, check to see if the stream was created, again for a better exception text
                        else if (((RemoteConsumerDispatcher) current).getAnycastInputHandler().
                                        testStreamStatus(AnycastInputHandler.AIStreamStatus.STREAM_NON_EXISTENT))
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(
                                           tc,
                                           "attachToRemoteDurableSubscription",
                                           new Object[] { "Consumers already attached remotely", e });

                            throw new SIDestinationLockedException(
                                            nls.getFormattedMessage(
                                                                    "SUBSCRIPTION_IN_USE_ERROR_CWSIP0152",
                                                                    new Object[] {
                                                                                  subState.getSubscriberID(),
                                                                                  _messageProcessor.getMessagingEngineName() },
                                                                    null));
                        }
                        // Otherwise, just throw what we were given
                        else
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(
                                           tc,
                                           "attachToRemoteDurableSubscription",
                                           new Object[] { "Failed to attach remotely", e });
                            throw e;
                        }
                    }
                }
                else if (!subState.isCloned() && (current instanceof String))
                {
                    // This is a String so must be a placeholder for a creating one, which means
                    // someone else is about to attach so we would fail anyway (as we're not cloned)
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(
                                   tc,
                                   "attachToRemoteDurableSubscription",
                                   "Consumer in the process of attaching locally");

                    throw new SIDestinationLockedException(
                                    nls.getFormattedMessage(
                                                            "SUBSCRIPTION_IN_USE_ERROR_CWSIP0152",
                                                            new Object[] {
                                                                          subState.getSubscriberID(),
                                                                          _messageProcessor.getMessagingEngineName() },
                                                            null));
                }
                else
                {
                    //The durable home tells us that this subscription
                    //is remote, but now we find that there is a non-remote
                    //ConsumerDispatcher for the subscription.
                    //This is inconsistent so we throw an exception
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(
                                   tc,
                                   "attachToRemoteDurableSubscription",
                                   "A ConsumerDispatcher for this subscription exists locally.");

                    throw new SIDestinationLockedException(
                                    nls.getFormattedMessage(
                                                            "SUBSCRIPTION_ATTACH_FAILED_CWSIP0035",
                                                            new Object[] { subState.getSubscriberID(), durableMEUuid },
                                                            null));
                }
            }

            if (createConsumerDispatcher)
            {
                // Ok, no CD yet, which means we'll have to attempt to create a ConsumerDispatcher.
                // If we do all this while we're holding the lock then we may hold up all other durable
                // subs on this topic.  So create a bogus durSub entry and release the lock.  This will
                // only block out durSubs attempting to connect to exactly the same durable subscription.
                // If the create succeeds, then we'll fix the entry.  Otherwise we'll just remove it.
                _consumerDispatchersDurable.put(remSubName, remSubName);
            }
        } //end sync

        if (createConsumerDispatcher)
        {
            //we are now in a position to create the RCD
            // RCD we'll use to attach our ConsumerPoint below
            RemoteConsumerDispatcher rcd = null;

            try
            {
                rcd = _pubSubRemoteSupport.
                                createRemoteConsumerDispatcher(remSubName,
                                                               subState,
                                                               durableMEUuid);
            } catch (Exception e)
            {
                // No FFDC code needed
                // Cleanup before exiting
                synchronized (_consumerDispatchersDurable)
                {
                    _consumerDispatchersDurable.remove(remSubName);
                }

                // rethrow not founds
                if (e instanceof SIDurableSubscriptionNotFoundException)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(
                                   tc,
                                   "attachToRemoteDurableSubscription",
                                   "SIDurableSubscriptionNotFoundException");
                    throw (SIDurableSubscriptionNotFoundException) e;
                }

                // rethrow locked
                if (e instanceof SIDestinationLockedException)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(
                                   tc,
                                   "attachToRemoteDurableSubscription",
                                   "SIDestinationLockedException");
                    throw (SIDestinationLockedException) e;
                }

                // rethrow resource exceptions
                if (e instanceof SIResourceException)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(
                                   tc,
                                   "attachToRemoteDurableSubscription",
                                   "SIResourceException");
                    throw (SIResourceException) e;
                }

                // rethrow durable mismatch exception
                if (e instanceof SIDurableSubscriptionMismatchException)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(
                                   tc,
                                   "attachToRemoteDurableSubscription",
                                   "SIDurableSubscriptionMismatchException");
                    throw (SIDurableSubscriptionMismatchException) e;
                }

                // There shouldn't be anything else, but if so FFDC the
                // original and rethrow an SIErrorException

                // Otherwise, exception shouldn't occur so FFDC.
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.attachToRemoteDurableSubscription",
                                            "1:3044:1.35.2.4",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(
                               tc,
                               "attachToRemoteDurableSubscription",
                               "SIErrorException");
                throw new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.destination.PubSubRealization",
                                                                      "1:3059:1.35.2.4" },
                                                        null));
            }

            // Register the "real" consumer dispatcher and attach our consumer point
            synchronized (_consumerDispatchersDurable)
            {
                _consumerDispatchersDurable.put(remSubName, rcd);

                //we do the attach in the synschronized
                data =
                                (ConsumableKey) rcd.attachConsumerPoint(
                                                                        consumerPoint,
                                                                        null,
                                                                        consumerPoint.getConsumerSession().getConnectionUuid(),
                                                                        consumerPoint.getConsumerSession().getReadAhead(),
                                                                        consumerPoint.getConsumerSession().getForwardScanning(),
                                                                        null);

                // Wake up anyone who may be waiting for this
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Notifying all on " + _consumerDispatchersDurable);
                _consumerDispatchersDurable.notifyAll();
            }

        } //end create RCD

        // All done, return
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "attachToRemoteDurableSubscription", data);

        return data;
    }

    /**
     * Attempt to handle a remote request to attach to a local durable
     * subscription. The attach is successful if no exceptions are thrown.
     * 
     * @param request The ControlCreateStream request message.
     */
    public void attachDurableFromRemote(ControlCreateStream request)
                    throws
                    SIDestinationLockedException,
                    SIDurableSubscriptionMismatchException,
                    SIDurableSubscriptionNotFoundException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "attachDurableFromRemote", request);

        String subName = request.getDurableSubName();
        SIBUuid8 sender = request.getGuaranteedSourceMessagingEngineUUID();
        String destName =
                        _baseDestinationHandler.constructPseudoDurableDestName(
                                                                               request.getGuaranteedTargetMessagingEngineUUID().toString(),
                                                                               subName);

        // Instantiate selection criteria
        SelectionCriteria criteria =
                        _messageProcessor.getSelectionCriteriaFactory().createSelectionCriteria(
                                                                                                request.getDurableDiscriminator(),
                                                                                                request.getDurableSelector(),
                                                                                                SelectorDomain.getSelectorDomain(request.getDurableSelectorDomain()));

        //create a state object representing a subscription
        ConsumerDispatcherState subState =
                        new ConsumerDispatcherState(
                                        request.getDurableSubName(),
                                        request.getGuaranteedTargetDestinationDefinitionUUID(),
                                        criteria,
                                        request.isNoLocal(),
                                        _messageProcessor.getMessagingEngineName(),
                                        null,
                                        null);

        //defect 259036
        subState.setIsCloned(request.isCloned());
        subState.setTopicSpaceName(_baseDestinationHandler.getDefinition().getName());
        subState.setTopicSpaceBusName(_baseDestinationHandler.getBus());

        String user = request.getSecurityUserid();
        // Retrieve SIBServerSubject flag from the message.
        boolean isSIBServerSubject = request.isSecurityUseridSentBySystem();
        // Set the security id into the CD state
        subState.setUser(user, isSIBServerSubject);

        // If the AOH already exists (e.g. new connection request, or
        // resend of a previous request), then check if the request
        // matches the CD and, if so, delegate to the existing AOH.
        if (_pubSubRemoteSupport.locateExistingAOH(request,
                                                   sender,
                                                   destName,
                                                   subState) == null)
        {

            // Create a LocalConsumerPoint and a ConsumerDispatcherState and try to attach
            // locally.  Synchronize against the consumerDispatcherTable to resolve any
            // races with other concurrent attachers.
            synchronized (_consumerDispatchersDurable)
            {
                // Create the CDS based on the existing CDS, or throw an exception
                // if the subscription is unknown.
                ConsumerDispatcher cd =
                                (ConsumerDispatcher) _consumerDispatchersDurable.get(subName);
                if (cd == null)
                {
                    // No dice, throw the exception
                    SIDurableSubscriptionNotFoundException e =
                                    new SIDurableSubscriptionNotFoundException(
                                                    nls.getFormattedMessage(
                                                                            "SUBSCRIPTION_DOESNT_EXIST_ERROR_CWSIP0146",
                                                                            new Object[] {
                                                                                          subName,
                                                                                          _messageProcessor.getMessagingEngineName() },
                                                                            null));
                    SibTr.exception(tc, e);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(
                                   tc,
                                   "attachDurableFromRemote",
                                   "Subscription does not exist");
                    throw e;
                }
                else if (cd.getConsumerDispatcherState().getTargetDestination() != null)
                {
                    // Subscription was an internal one so reject the attach
                    SIDurableSubscriptionMismatchException e =
                                    new SIDurableSubscriptionMismatchException(
                                                    nls.getFormattedMessage(
                                                                            "INTERNAL_SUBSCRIPTION_ACCESS_DISALLOWED_CWSIP0147",
                                                                            new Object[] {
                                                                                          subName,
                                                                                          _messageProcessor.getMessagingEngineName() },
                                                                            null));
                    SibTr.exception(tc, e);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(
                                   tc,
                                   "attachDurableFromRemote",
                                   "Attaching to internal subscriptions is not allowed");
                    throw e;
                }

                // Copy noLocal and isCloned
                subState.setNoLocal(cd.getConsumerDispatcherState().isNoLocal());
                subState.setIsCloned(cd.getConsumerDispatcherState().isCloned());

                // Check cardinality next
                if (cd.hasConsumersAttached() && !subState.isCloned())
                {
                    Exception e =
                                    new SIDestinationLockedException(
                                                    nls.getFormattedMessage(
                                                                            "SUBSCRIPTION_IN_USE_ERROR_CWSIP0152",
                                                                            new Object[] {
                                                                                          subState.getSubscriberID(),
                                                                                          _messageProcessor.getMessagingEngineName() },
                                                                            null));
                    SibTr.exception(tc, e);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(
                                   tc,
                                   "attachDurableFromRemote",
                                   "Consumers already attached");
                    throw (SIDestinationLockedException) e;
                }

                // Attach to the consumerDispatcher if it is EXACTLY the same
                if (!cd.getConsumerDispatcherState().isReady()
                    || !cd.getConsumerDispatcherState().equals(subState))
                {
                    SIDurableSubscriptionMismatchException e =
                                    new SIDurableSubscriptionMismatchException(
                                                    nls.getFormattedMessage(
                                                                            "SUBSCRIPTION_ALREADY_EXISTS_ERROR_CWSIP0143",
                                                                            new Object[] {
                                                                                          subState.getSubscriberID(),
                                                                                          _messageProcessor.getMessagingEngineName() },
                                                                            null));
                    SibTr.exception(tc, e);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "attachDurableFromRemote", subState);
                    throw e;
                }

                // Ok, all that worked so create the AOH and forward the request.
                // This should cause attachment to occur.
                DestinationDefinition pseudoDest = null;
                AnycastOutputHandler pseudoHandler = null;

                try
                {
                    // Create the pseudo destination ID and hack up a definition for it
                    // NOTE: have to create the destination first since we need it's ID
                    // for the container stream below.
                    pseudoDest =
                                    _messageProcessor.createDestinationDefinition(
                                                                                  DestinationType.TOPICSPACE,
                                                                                  destName);

                    //defect 259036: if cloned then we are NOT receive
                    //exclusive
                    pseudoDest.setReceiveExclusive(!subState.isCloned());

                    // Create the stream next
                    AOContainerItemStream aostream =
                                    new AOContainerItemStream(pseudoDest.getUUID(), subName);
                    LocalTransaction siTran =
                                    _baseDestinationHandler.
                                                    getTransactionManager().
                                                    createLocalTransaction(true);
                    _baseDestinationHandler.addItemStream(aostream, (Transaction) siTran);
                    siTran.commit();

                    // TODO - should be using something better than System.currentTimeMillis() for the dmeVersion.
                    pseudoHandler =
                                    new AnycastOutputHandler(
                                                    pseudoDest.getName(),
                                                    pseudoDest.getUUID(),
                                                    pseudoDest.isReceiveExclusive(),
                                                    null,
                                                    cd,
                                                    aostream,
                                                    _messageProcessor,
                                                    _destinationManager.getAsyncUpdateThread(),
                                                    _destinationManager.getPersistLockThread(),
                                                    System.currentTimeMillis(),
                                                    false);

                    // Store the destination in our map and make sure the DestinationManager
                    // knows about it for future messages.
                    _pubSubRemoteSupport.
                                    storePseudoDestination(pseudoHandler,
                                                           destName,
                                                           pseudoDest);

                    // Finally, deliver the message to the aoh
                    pseudoHandler.handleControlMessage(
                                                       sender,
                                                       request);
                } catch (Exception e)
                {
                    // Exception shouldn't occur so FFDC.
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.attachDurableFromRemote",
                                                "1:3304:1.35.2.4",
                                                this);
                    SibTr.exception(tc, e);

                    // Then cleanup anything we managed to create
                    if (pseudoDest != null)
                    {
                        _pubSubRemoteSupport.
                                        cleanupPseudoDestination(destName, pseudoDest);
                    }

                    //TODO: does this get rid of the AOContainerStream as well?
                    if (pseudoHandler != null)
                        pseudoHandler.close();

                    // And finally throw an SIErrorException so that DurableOutputHandler sends something
                    // back to the caller.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "attachDurableFromRemote", "SIErrorException");
                    SIErrorException x =
                                    new SIErrorException(
                                                    nls.getFormattedMessage(
                                                                            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                                            new Object[] {
                                                                                          "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.attachDurableFromRemote",
                                                                                          "1:3329:1.35.2.4",
                                                                                          e },
                                                                            null));
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.attachDurableFromRemote",
                                                "1:3335:1.35.2.4",
                                                this);

                    SibTr.exception(tc, e);

                    SibTr.error(
                                tc,
                                "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                new Object[] {
                                              "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.attachDurableFromRemote",
                                              "1:3345:1.35.2.4",
                                              e });
                    throw x;
                }
            } // synchronized (consumerDispatchersDurable)
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "attachDurableFromRemote");
    }

    /**
     * Attaches to a created DurableSubscription
     * 
     * Checks that there are no active subscriptions (unless it supports cloning)
     * 
     * Checks that the durable subscription exists :
     * 
     * @param consumerPoint
     */
    public ConsumableKey attachToDurableSubscription(
                                                     LocalConsumerPoint consumerPoint,
                                                     ConsumerDispatcherState subState)
                    throws
                    SIDurableSubscriptionMismatchException,
                    SIDurableSubscriptionNotFoundException,
                    SIDestinationLockedException,
                    SISelectorSyntaxException,
                    SIDiscriminatorSyntaxException,
                    SINotPossibleInCurrentConfigurationException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "attachToDurableSubscription",
                        new Object[] { consumerPoint, subState });

        // Extract the UUID for the durable home and see whether this is a local
        // or remote attach
        ConsumableKey result = null;
        SIBUuid8 durableHomeID =
                        _messageProcessor.mapMeNameToUuid(subState.getDurableHome());
        if (durableHomeID == null)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "attachToDurableSubscription", "SIResourceException");
            // Lookup failed, throw an excepiton
            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "REMOTE_ME_MAPPING_ERROR_CWSIP0156",
                                                    new Object[] { subState.getDurableHome() },
                                                    null));
        }

        // Is durableHome local?
        if (durableHomeID.equals(_messageProcessor.getMessagingEngineUuid()))
        {
            // Directly attached
            result = attachToLocalDurableSubscription(consumerPoint, subState);
        } else
        {
            // Stash the durableHomeID in the ConsumerDispatcherState
            subState.setRemoteMEUuid(durableHomeID);
            // Remote attach
            result =
                            attachToRemoteDurableSubscription(
                                                              consumerPoint,
                                                              subState,
                                                              durableHomeID);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "attachToDurableSubscription", result);

        return result;
    }

    /**
     * Clean up after a deleted consumer dispatcher that represents the RME
     * endpoint for access to a remote durable subscription. This is necessary
     * to allow other local clients access to the same remote durable subscription
     * (or to allow access to a durable subscription with the same client and name,
     * but at a different remote location).
     * 
     * @param subState Subscription state.
     */
    public void deleteRemoteDurableRME(ConsumerDispatcherState subState)
                    throws SIDurableSubscriptionNotFoundException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc,
                        "deleteRemoteDurableRME",
                        new Object[] { subState });

        Object rcd = null;

        // Retreive the remote ME uuid from the ConsumerDispatcherState
        SIBUuid8 remoteMEUuid =
                        subState.getRemoteMEUuid();

        // Build the key for the consumerDispatchersDurable table
        String remSubName = null;
        if (remoteMEUuid != null)
        {
            remSubName =
                            _baseDestinationHandler.
                                            constructPseudoDurableDestName(remoteMEUuid.toString(),
                                                                           subState.getSubscriberID());
        }

        synchronized (_consumerDispatchersDurable)
        {
            if (remSubName != null)
            {
                rcd = _consumerDispatchersDurable.get(remSubName);
            }

            if ((rcd != null) && (rcd instanceof RemoteConsumerDispatcher))
            {
                // Mark the RCD to inicate that it is being deleted (and will have flush
                // work scheduled against it)
                ((RemoteConsumerDispatcher) rcd).setPendingDelete(true);
            }
            // Verify that the subscription exists.  If not, throw an error
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(
                               tc,
                               "deleteRemoteDurableRME",
                               "no such durable subcription");

                throw new SIDurableSubscriptionNotFoundException(
                                nls.getFormattedMessage(
                                                        "SUBSCRIPTION_DOESNT_EXIST_ERROR_CWSIP0072",
                                                        new Object[] {
                                                                      subState.getSubscriberID(),
                                                                      subState.getDurableHome() },
                                                        null));
            }
        }

        // Remove the RME side as if we were cleaning up the destination (which, in a sense, we are).
        // If necessary, delay the delete until the DME completes the flush.
        // Otherwise, we'll have problems if the DME missed the message and
        // we try to attach another consumer from the same RME.
        final String subName = remSubName;
        final AnycastInputHandler aih =
                        _pubSubRemoteSupport.getAIHByName(remSubName);
        final SIBUuid12 pseudoDestID = aih.getDestUuid();

        Runnable deleteAction = new Runnable()
        {
            @Override
            public void run()
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.entry(tc, "run");

                try
                {
                    // Actual delete happens here but we have to eat exceptions, yuck
                    LocalTransaction siTran =
                                    _baseDestinationHandler.
                                                    getTransactionManager().
                                                    createLocalTransaction(false);

                    boolean deleting =
                                    _pubSubRemoteSupport.
                                                    deleteRemoteDurableRME(subName, aih, siTran);

                    if (deleting)
                    {
                        _destinationManager.removePseudoDestination(pseudoDestID);

                        synchronized (_consumerDispatchersDurable)
                        {
                            // Remove the placeholder and signal anyone who happens to be
                            // waiting.
                            _consumerDispatchersDurable.remove(subName);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(tc, "Notifying all on " + _consumerDispatchersDurable);
                            _consumerDispatchersDurable.notifyAll();
                        }
                    }

                    siTran.commit();
                } catch (Exception e)
                {
                    // Have to FFDC, no other way to find out otherwise.
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.run",
                                                "1:3539:1.35.2.4",
                                                this);

                    SibTr.error(
                                tc,
                                "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                new Object[] {
                                              "com.ibm.ws.sib.processor.impl.destination.PubSubRealization",
                                              "1:3547:1.35.2.4",
                                              e });

                }

                // Test - see wait code below
                synchronized (pseudoDestID)
                {
                    pseudoDestID.notifyAll();
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "run");
            }
        };

        // Synchronize on a convenient object for delete
        // coordination below.
        synchronized (pseudoDestID)
        {
            synchronized (aih)
            {
                if (!aih
                                .testStreamStatus(
                                AnycastInputHandler.AIStreamStatus.STREAM_NON_EXISTENT))
                {
                    // Stream not yet flushed so have AIH call us when it's done
                    aih.addFlushedCallback(deleteAction);
                }
                else
                    // Stream must be flushed now so go ahead and run the delete action directly
                    deleteAction.run();
            }
        }

        // All done, return
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteRemoteDurableRME");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#registerControlAdapters()
     */
    @Override
    public void registerControlAdapters()
    {
        super.registerControlAdapters();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "registerControlAdapters");

        // register the publication point
        if (_pubsubMessageItemStream != null)
        {
            _pubsubMessageItemStream.registerControlAdapterAsMBean();
        }

        super.registerControlAdapters();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerControlAdapters");
    }

    /**
     * Ensure that if the durable sub was made through an alias, then the alias
     * still exists
     * 
     * @param durableSub
     * @return true if the durable sub is valid, false if it wasnt and has bee]
     *         removed
     */
    private boolean checkDurableSubStillValid(DurableSubscriptionItemStream durableSub)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkDurableSubStillValid", durableSub);

        boolean valid = false;

        ConsumerDispatcherState cdState = durableSub.getConsumerDispatcherState();

        if (durableSub.isToBeDeleted())
        {
            valid = false;
            _destinationManager.addSubscriptionToDelete(durableSub);
        } else if (cdState.getTopicSpaceUuid().equals(_baseDestinationHandler.getUuid()))
        {
            //The subscription was made through the topicspace directly
            //(most likely case)
            valid = true;
        } else
        {
            //The subscription must have been made through an alias.  Check its
            //still valid
            try
            {
                // Get the admin version since otherwise the target will not have been
                // resolved yet.
                BaseDestinationDefinition dh =
                                _destinationManager.getLocalME()
                                                .getMessagingEngine()
                                                .getSIBDestination(cdState.getTopicSpaceBusName(),
                                                                   cdState.getTopicSpaceName());

                if (dh.getUUID().equals(cdState.getTopicSpaceUuid()))
                {
                    valid = true;
                } else
                {
                    //The alias has a different uuid, so the old alias must have been deleted
                    // Remove the persistent state of the durable subscription
                    durableSub.markAsToBeDeleted();
                    try
                    {
                        durableSub.requestUpdate(
                                        _messageProcessor.getTXManager().createAutoCommitTransaction());
                    } catch (MessageStoreException e)
                    {
                        // FFDC
                        FFDCFilter.processException(
                                                    e,
                                                    "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.checkDurableSubStillValid",
                                                    "1:3667:1.35.2.4",
                                                    this);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            SibTr.exception(tc, e);

                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(
                                       tc,
                                       "checkDurableSubStillValid",
                                       "SIResourceException - Failed to update durable sub to delete.");

                        throw new SIResourceException(
                                        nls.getFormattedMessage(
                                                                "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                                new Object[] {
                                                                              "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.checkDurableSubStillValid",
                                                                              "1:3684:1.35.2.4",
                                                                              e },
                                                                null));
                    }
                    durableSub.deleteIfPossible(false);

                    // Send the subscription deleted event message to the Neighbours
//          sanjay liberty change
//          _messageProcessor.getProxyHandler().unsubscribeEvent(
//            cdState,
//            _baseDestinationHandler.getTransactionManager().createAutoCommitTransaction(),
//            true);
                }
            } catch (SIBExceptionBase e)
            {
                // No FFDC code needed
                // Remove the persistent state of the durable subscription
                durableSub.markAsToBeDeleted();
                try
                {
                    durableSub.requestUpdate(
                                    _messageProcessor.getTXManager().createAutoCommitTransaction());
                } catch (MessageStoreException e1)
                {
                    // FFDC
                    FFDCFilter.processException(
                                                e1,
                                                "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.checkDurableSubStillValid",
                                                "1:3712:1.35.2.4",
                                                this);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        SibTr.exception(tc, e);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(
                                   tc,
                                   "checkDurableSubStillValid",
                                   "SIResourceException - Failed to update durable sub to delete.");

                    throw new SIResourceException(
                                    nls.getFormattedMessage(
                                                            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                            new Object[] {
                                                                          "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.checkDurableSubStillValid",
                                                                          "1:3729:1.35.2.4",
                                                                          e },
                                                            null));
                }

                durableSub.deleteIfPossible(false);

                // Send the subscription deleted event message to the Neighbours
                _messageProcessor.getProxyHandler().unsubscribeEvent(
                                                                     cdState,
                                                                     _baseDestinationHandler.getTransactionManager().createAutoCommitTransaction(),
                                                                     true);
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkDurableSubStillValid", new Boolean(valid));

        return valid;
    }

    /**
     * @param pevent
     */
    public void runtimeEventOccurred(MPRuntimeEvent event)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "runtimeEventOccurred", event);

        // A publication point, fire against the control adapter belonging to the
        // pubsubMessageItemStream
        if (_pubsubMessageItemStream != null)
        {
            _pubsubMessageItemStream.getControlAdapter().runtimeEventOccurred(event);
        } else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(
                            tc,
                            "runtimeEventOccurred",
                            "publication point is null, cannot fire event");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "runtimeEventOccurred");
    }

    /**
     * Retrieve the message from the non-persistent ItemStream
     */
    public MessageItem retrieveMessageFromItemStream(long msgStoreID)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "retrieveMessageFromItemStream", new Long(msgStoreID));

        MessageItem msgItem = null;
        try
        {
            msgItem = (MessageItem) _pubsubMessageItemStream.findById(msgStoreID);
        } catch (MessageStoreException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.destination.PubSubRealization.retrieveMessageFromItemStream",
                                        "1:3797:1.35.2.4",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "retrieveMessageFromItemStream", e);

            throw new SIResourceException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "retrieveMessageFromItemStream", msgItem);

        return msgItem;
    }

    /**
     * @return
     */
    public RemotePubSubSupport getRemotePubSubSupport()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getRemotePubSubSupport");
            SibTr.exit(tc, "getRemotePubSubSupport", _pubSubRemoteSupport);
        }
        return _pubSubRemoteSupport;
    }

}
