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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIApiConstants;
import com.ibm.websphere.sib.SIDestinationAddress;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.websphere.sib.management.SibNotificationConstants;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.admin.BaseDestinationDefinition;
import com.ibm.ws.sib.admin.BaseLocalizationDefinition;
import com.ibm.ws.sib.admin.DestinationDefinition;
import com.ibm.ws.sib.admin.LocalizationDefinition;
import com.ibm.ws.sib.admin.QualifiedDestinationName;
import com.ibm.ws.sib.mfp.JsDestinationAddress;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.mfp.control.ControlCreateStream;
import com.ibm.ws.sib.mfp.control.ControlMessage;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.Statistics;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.UndeliverableReturnCode;
import com.ibm.ws.sib.processor.impl.destination.AbstractProtoRealization;
import com.ibm.ws.sib.processor.impl.destination.JSPtoPRealization;
import com.ibm.ws.sib.processor.impl.destination.LocalisationManager;
import com.ibm.ws.sib.processor.impl.destination.PubSubRealization;
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
import com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener;
import com.ibm.ws.sib.processor.impl.interfaces.OutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.PtoPRealization;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.BatchHandler;
import com.ibm.ws.sib.processor.impl.store.MessageEvents;
import com.ibm.ws.sib.processor.impl.store.itemstreams.ProxyReferenceStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPXmitMsgsItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PubSubMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SourceProtocolItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.TargetProtocolItemStream;
import com.ibm.ws.sib.processor.proxyhandler.Neighbour;
import com.ibm.ws.sib.processor.runtime.SIMPIterator;
import com.ibm.ws.sib.processor.runtime.SIMPRemoteQueuePointControllable;
import com.ibm.ws.sib.processor.runtime.impl.AnycastInputControl;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.runtime.impl.MPRuntimeEvent;
import com.ibm.ws.sib.processor.runtime.impl.Queue;
import com.ibm.ws.sib.processor.runtime.impl.RemoteQueuePoint;
import com.ibm.ws.sib.processor.runtime.impl.Topicspace;
import com.ibm.ws.sib.processor.utils.LockManager;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.index.Index;
import com.ibm.ws.sib.security.auth.OperationType;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.TransactionCallback;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;
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
 * @author MP team
 * 
 *         <p>The destination class is the focal pointfor information about a destination.
 *         In WAS7 much of it has been restructured into the classes in the
 *         com.ibm.ws.sib.processor.impl.destination package
 *         <p> For cloned destinations, there is one destination object instance, with
 *         a number of queuing points associated with it. Destinations are
 *         managed by the DestinationManager class.
 */
public class BaseDestinationHandler
                extends AbstractBaseDestinationHandler
                implements MessageEventListener
{
    /**
     * Trace for the component
     */
    private static final TraceComponent tc =
                    SibTr.register(
                                   BaseDestinationHandler.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);
    private static final TraceComponent tc_cwsik =
                    SibTr.register((new Object() {}).getClass(), SIMPConstants.MP_TRACE_GROUP, SIMPConstants.CWSIK_RESOURCE_BUNDLE);

    /** NLS for component */
    static final TraceNLS nls =
                    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

    /** Persistent data version number. */
    private static final int PERSISTENT_VERSION = 1;

    /* The name of the destination */
    private String _name = null;

    /** indexes to keep track of the control adapters for remote queues */
    private Index _remoteQueuePoints = null;

    /**
     * Exception Destination object. When a message cannot be delivered to the consumer
     * because there is a problem, we may want to forward it to the exception destination
     * of the destination. This is done via this ExceptionDestinationHandlerImpl class instance. The
     * object encapsulates the exception destination itself and the routines for
     * forwarding the message onwards
     */
    private ExceptionDestinationHandlerImpl _exceptionDestination = null;

    /** Indicates that some of the reconciled streams should be deleted */
    private boolean _hasReconciledStreamsToBeDeleted = false;

    /**
     * Is this destination marked as to-be-deleted?
     * Marked volatile to provide visibility outside a locking hierarchy.
     */
    private volatile boolean _toBeDeleted = false;

    /** Is destination deletion logic active for this destination? */
    private boolean _deleteInProgress = false;

    /** Is this destination marked so that it will forever be ignored on subsequent restarts? */
    private boolean _toBeIgnored;

    /** Have we detected that this destination is corrupt or indoubt? */
    protected boolean _isCorruptOrIndoubt;

    /** Is this a temporary destination? */
    private boolean _isTemporary = false;

    /** Is reallocation of transmitQs required */
    boolean _isToBeReallocated = false;

    /**
     * Has the destination been reconciled? When reconciliation is taking place,
     * this is set to false until we know we are retaining the destination. By
     * default it is set to true since destinations created by admin while the
     * Message Processor is running will already by reconciled with WCCM.
     */
    private boolean _reconciled = true;

    /**
     * Report Handler object. All report message generation and sending is
     * performed by a reportHandler object. We have one here to avoid creating
     * a new one for each message that requires a report.
     */
    private ReportHandler _reportHandler = null;

    private boolean _isSystem;

    /** is total ordering required */
    private boolean _isOrderingRequired;

    /** is total ordering disabled due to state found during reconstitute */
    private boolean _isUnableToOrder;

    /** The administered fwd routing path */
    private List<SIDestinationAddress> _forwardRoutingPath = null;

    /** The administered reply destination */
    private JsDestinationAddress _replyDestination = null;
    private boolean _replySet = false;

    /**
     * The address of this destintion, used as a routing address when necessary
     * This is not used by LinkHandlers as any routing will come via a BusHandler
     * rather than the link handler.
     */
    private JsDestinationAddress _destinationAddr = null;

    /** Indicator as to whether the ME is running in an ND environment */
    private boolean _singleServer;

    /** PubSub dest handler */
    private PubSubRealization _pubSubRealization = null;

    /** PtoP dest handler */
    protected JSPtoPRealization _ptoPRealization = null;

    /** Reference to the protocol state handler */
    protected AbstractProtoRealization _protoRealization = null;

    /** The localisation manager handles sets of localisations and interfaces to WLM */
    protected LocalisationManager _localisationManager = null;

    /** Simple object to clarify where we lock the ready ConsumerPoint state */
    protected Object readyConsumerPointLock = new Object();

    /**
     * Warm start constructor invoked by the Message Store.
     */
    public BaseDestinationHandler()
    {
        super();
    }

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
    protected BaseDestinationHandler(
                                     DestinationDefinition myDestinationDefinition,
                                     MessageProcessor messageProcessor,
                                     SIMPItemStream parentItemStream,
                                     TransactionCommon transaction,
                                     HashMap<String, Object> durableSubscriptionsTable,
                                     String busName) throws SIResourceException
    {
        super(messageProcessor, myDestinationDefinition, busName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "BaseDestinationHandler",
                        new Object[] {
                                      myDestinationDefinition,
                                      messageProcessor,
                                      parentItemStream,
                                      transaction,
                                      durableSubscriptionsTable,
                                      busName });

        // 176658.3.1 - Register the destination handler for callback on transaction completion
        transaction.registerCallback(new DestinationAddTransactionCallback());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(
                        tc,
                        "Creating "
                                        + (isPubSub() ? "pubsub" : "ptp")
                                        + " BaseDestinationHandler "
                                        + getName()
                                        + "on bus "
                                        + busName);

        // set the value of orderingRequired
        setIsOrdered(myDestinationDefinition.isOrderingRequired());

        _name = myDestinationDefinition.getName();
        _destinationAddr = SIMPUtils.createJsDestinationAddress(_name, null, getBus());

        /*
         * If the destination is temporary, then set the storage strategy such
         * that it will not necessarily be persisted. With STORE_MAYBE, items
         * are currently persisted anyway, so we will have to do a check on startup
         * and discard any destinations which are temporary to protect against a
         * dirty shutdown. Normally we would expect these to be deleted when the
         * connection they were created on is closed.
         * 
         * 174199.2.9
         */
        if (_name.startsWith(SIMPConstants.TEMPORARY_QUEUE_DESTINATION_PREFIX) ||
            _name.startsWith(SIMPConstants.TEMPORARY_PUBSUB_DESTINATION_PREFIX))
        {
            _isTemporary = true;
            _isSystem = false;
            setStorageStrategy(STORE_MAYBE);
        }
        else
        {
            _isTemporary = false;
            _isSystem = _name.startsWith(SIMPConstants.SYSTEM_DESTINATION_PREFIX);
            setStorageStrategy(STORE_ALWAYS);
        }

        /*
         * Add the destinationHandler into the MessageStore. Needs to be called
         * before initializeNonPersistent so that nonPersistent Itemstreams can
         * subsequently be added.
         */
        try
        {
            Transaction msTran = messageProcessor.resolveAndEnlistMsgStoreTransaction(transaction);
            parentItemStream.addItemStream(this, msTran);

            /*
             * Create associated dest handler of appropriate type
             */
            createRealizationAndState(messageProcessor, transaction);

            if (isPubSub())
            {
                // Create the PubSub Control Adapter required by the psItemStream
                createControlAdapter();
            }

            /*
             * Add ItemStream for Guaranteed Delivery Protocol State. All destination types
             * may need to send pt-to-pt messages
             */
            _protoRealization.
                            getRemoteSupport().
                            createGDProtocolItemStreams(transaction);
        } catch (OutOfCacheSpace e)
        {
            // No FFDC code needed
            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "BaseDestinationHandler", "SIResourceException");
            throw new SIResourceException(e);
        } catch (MessageStoreException e)
        {
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.BaseDestinationHandler.BaseDestinationHandler",
                                        "1:485:1.700.3.45",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "BaseDestinationHandler", "SIResourceException");

            throw new SIResourceException(e);
        }

        // we can now initialize that which is common to cold and warm starts.
        initializeNonPersistent(
                                messageProcessor,
                                durableSubscriptionsTable,
                                transaction);

        _forwardRoutingPath = null;
        // Convert array of names to list of jsDestinationAddresses
        QualifiedDestinationName[] names = definition.getForwardRoutingPath();

        if (names != null)
        {
            _forwardRoutingPath = new ArrayList(names.length);
            for (int name = 0; name < names.length; name++)
            {
                _forwardRoutingPath.add(SIMPUtils.createJsDestinationAddress(
                                                                             names[name].getDestination(),
                                                                             null,
                                                                             names[name].getBus()));
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(
                        tc,
                        "Created "
                                        + (isPubSub() ? "pubsub" : "ptp")
                                        + " BaseDestinationHandler "
                                        + getName());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "BaseDestinationHandler", this);
    }

    /**
     * <p>Create a new instance of a destination. Used by extending classes that
     * dont have a destination definition. Assumes the extending class wants
     * a queue style destination</p>
     * 
     * @param messageProcessor
     * @param parentStream The Itemstream this DestinationHandler should be
     *            added into.
     * @param durableSubscriptionsTable Required only by topicspace
     *            destinations. Can be null if point to point (local or remote).
     * @param busName The name of the bus on which the destination resides
     */
    protected BaseDestinationHandler(
                                     MessageProcessor messageProcessor,
                                     SIMPItemStream parentItemStream,
                                     TransactionCommon transaction,
                                     HashMap<String, Object> durableSubscriptionsTable,
                                     String busName) throws SIResourceException
    {
        super(messageProcessor, null, busName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "BaseDestinationHandler",
                        new Object[] {
                                      messageProcessor,
                                      parentItemStream,
                                      transaction,
                                      durableSubscriptionsTable,
                                      busName });

        // 176658.3.1 - Register the destination handler for callback on transaction completion
        transaction.registerCallback(new DestinationAddTransactionCallback());

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(
                        tc,
                        "Creating "
                                        + (isPubSub() ? "pubsub" : "ptp")
                                        + " BaseDestinationHandler "
                                        + getName()
                                        + "on bus "
                                        + busName);

        _isTemporary = false;
        _isSystem = false;
        setStorageStrategy(STORE_ALWAYS);

        /*
         * Add the destinationHandler into the MessageStore. Needs to be called
         * before initializeNonPersistent so that nonPersistent Itemstreams can
         * subsequently be added.
         */
        try
        {
            Transaction msTran = messageProcessor.resolveAndEnlistMsgStoreTransaction(transaction);
            parentItemStream.addItemStream(this, msTran);

            /*
             * Create associated dest handler of appropriate type
             */
            createRealizationAndState(messageProcessor, transaction);

            // Add ItemStream for Guaranteed Delivery Protocol State
            _protoRealization.
                            getRemoteSupport().
                            createGDProtocolItemStreams(transaction);

        } catch (OutOfCacheSpace e)
        {
            // No FFDC code needed
            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "BaseDestinationHandler", "SIResourceException");
            throw new SIResourceException(e);
        } catch (MessageStoreException e)
        {
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.BaseDestinationHandler.BaseDestinationHandler",
                                        "1:620:1.700.3.45",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "BaseDestinationHandler", "SIResourceException");

            throw new SIResourceException(e);
        }

        // we can now initialize that which is common to cold and warm starts.
        initializeNonPersistent(
                                messageProcessor,
                                durableSubscriptionsTable,
                                transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(
                        tc,
                        "Created "
                                        + (isPubSub() ? "pubsub" : "ptp")
                                        + " BaseDestinationHandler "
                                        + getName());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "BaseDestinationHandler", this);
    }

    /**
     * Cold start version of method to create state associated with Destination.
     * 
     * @param messageProcessor
     * @param transaction
     * @throws SIResourceException
     */
    protected void createRealizationAndState(
                                             MessageProcessor messageProcessor,
                                             TransactionCommon transaction)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "createRealizationAndState",
                        new Object[] {
                                      messageProcessor,
                                      transaction });

        /*
         * Create associated protocol suppor of appropriate type
         */
        if (isPubSub())
        {
            _pubSubRealization = new PubSubRealization(this,
                            messageProcessor,
                            getLocalisationManager(),
                            transaction);
            _protoRealization = _pubSubRealization;
        }
        else
        {

            _ptoPRealization = new JSPtoPRealization(this,
                            messageProcessor,
                            getLocalisationManager());
            _protoRealization = _ptoPRealization;

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createRealizationAndState");
    }

    /**
     * Warm start version of method to create state associated with Destination.
     * 
     * @param messageProcessor
     * @throws SIResourceException
     */
    protected void createRealizationAndState(
                                             MessageProcessor messageProcessor)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "createRealizationAndState",
                        new Object[] {
                        messageProcessor });

        /*
         * Create associated protocol realization of appropriate type
         */
        if (isPubSub())
        {
            _pubSubRealization = new PubSubRealization(this,
                            messageProcessor,
                            getLocalisationManager());
            _protoRealization = _pubSubRealization;
        }
        else
        {

            _ptoPRealization = new JSPtoPRealization(this,
                            messageProcessor,
                            getLocalisationManager());
            _protoRealization = _ptoPRealization;

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createRealizationAndState");
    }

    /**
     * Initialize non-persistent fields. These fields are common to both MS
     * reconstitution of DestinationHandlers and initial creation.
     * <p>
     * Feature 174199.2.4
     * 
     * @param messageProcessor the message processor instance
     * @param durableSubscriptionsTable the topicspace durable subscriptions
     *            HashMap from the DestinationManager. Can be null if this
     *            BaseDestinationHandler is point to point (local or remote).
     * @param transaction the transaction to use for non persistent
     *            initialization. Can be null, in which case an auto transaction
     *            will be used.
     */
    void initializeNonPersistent(
                                 MessageProcessor messageProcessor,
                                 HashMap<String, Object> durableSubscriptionsTable,
                                 TransactionCommon transaction)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "initializeNonPersistent",
                        new Object[] {
                                      messageProcessor,
                                      durableSubscriptionsTable,
                                      transaction });

        //Check if we are running in an ND environment.  If not we can skip
        //some performance intensive WLM work
        _singleServer = messageProcessor.isSingleServer();

        if (isPubSub())
        {
            _pubSubRealization.
                            initialise(true,
                                       durableSubscriptionsTable);
        }
        else
        {
            _ptoPRealization.initialise();

            _remoteQueuePoints = new Index();
            createControlAdapter();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "initializeNonPersistent");
    }

    /**
     * Recover a BaseDestinationHandler retrieved from the MessageStore.
     * 
     * @param processor
     * @param durableSubscriptionsTable
     * 
     * @throws Exception
     */
    protected void reconstitute(
                                MessageProcessor processor,
                                HashMap<String, Object> durableSubscriptionsTable,
                                int startMode) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "reconstitute",
                        new Object[] { processor, durableSubscriptionsTable, Integer.valueOf(startMode) });

        super.reconstitute(processor);

        // Links are 'BaseDestinationHandlers' but have no destination-like addressibility.
        if (!isLink())
        {
            _name = getDefinition().getName();
            _destinationAddr = SIMPUtils.createJsDestinationAddress(_name, null, getBus());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(
                        tc,
                        "Reconstituting "
                                        + (isPubSub() ? "pubsub" : "ptp")
                                        + " BaseDestinationHandler "
                                        + getName());

        String name = getName();
        if ((name.startsWith(SIMPConstants.TEMPORARY_QUEUE_DESTINATION_PREFIX) || name.startsWith(SIMPConstants.TEMPORARY_PUBSUB_DESTINATION_PREFIX)))
        {
            _isTemporary = true;
            _isSystem = false;
        }
        else
        {
            _isTemporary = false;
            _isSystem = name.startsWith(SIMPConstants.SYSTEM_DESTINATION_PREFIX);
        }

        /*
         * Any kind of failure while retrieving data from the message store (whether
         * problems with the MS itself or with the data retrieved) means this
         * destination should be marked as corrupt. The only time an exception
         * should be thrown back up to the DM is when the corruption of the
         * destination is fatal to starting the ME (such as a corrupt system
         * destination, currently).
         */
        try
        {
            // Create appropriate state objects
            createRealizationAndState(messageProcessor);

            // Restore GD ProtocolItemStream before creating InputHandler
            _protoRealization.
                            getRemoteSupport().
                            reconstituteGD();

            // Reconstitute message ItemStreams
            if (isPubSub())
            {
                // Indicate that the destinationHandler has not yet been reconciled
                _reconciled = false;

                createControlAdapter();

                //Check if we are running in an ND environment.  If not we can skip
                //some performance intensive WLM work
                _singleServer = messageProcessor.isSingleServer();
                _pubSubRealization.reconstitute(startMode,
                                                durableSubscriptionsTable);

            }
            else
            {
                initializeNonPersistent(processor, durableSubscriptionsTable, null);

                // Indicate that the destinationHandler has not yet been reconciled
                _reconciled = false;

                _ptoPRealization.reconstitute(startMode,
                                              definition,
                                              isToBeDeleted(),
                                              isSystem());
            }

            // Reconstitute GD target streams
            _protoRealization.
                            reconstituteGDTargetStreams();
        } catch (Exception e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.BaseDestinationHandler.reconstitute",
                                        "1:945:1.700.3.45",
                                        this);

            SibTr.exception(tc, e);

            // At the moment, any exception we get while reconstituting means that we
            // want to mark the destination as corrupt.
            _isCorruptOrIndoubt = true;

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "reconstitute", e);

            throw new SIResourceException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(
                        tc,
                        "Reconstituted "
                                        + (isPubSub() ? "pubsub" : "ptp")
                                        + " BaseDestinationHandler "
                                        + getName());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconstitute");
    }

    /**
     * This method deletes the messages which are not having any references. Previously these messages
     * were deleted during ME startup in reconstitute method.This method is called from
     * DeletePubSubMsgsThread context
     */
    public void deleteMsgsWithNoReferences() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deleteMsgsWithNoReferences");

        if (null != _pubSubRealization) //doing a sanity check with checking for not null
            _pubSubRealization.deleteMsgsWithNoReferences();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteMsgsWithNoReferences");
    }

    /**
     * If a temporary BaseDestinationHandler has been recovered from the Message Store
     * we need to reconstitute enough of it such that none of the transactional
     * callbacks will suffer Null Pointer Exceptions when our items
     * (e.g. MessageItems) are deleted.
     * <p>
     * Most Message Store problems (MessageStoreException) will
     * be allowed to fall through to the DestinationManager for handling. Only a
     * failure to recover the durable subscriptions of a topicspace is dealt with
     * here.
     * 
     * @param processor
     * @param durableSubscriptionsTable
     * 
     * @throws MessageStoreException
     * @throws SIResourceException
     */
    private void reconstituteEnoughForDeletion(
                                               MessageProcessor processor,
                                               HashMap<String, Object> durableSubscriptionsTable)
                    throws
                    MessageStoreException,
                    SIRollbackException,
                    SIConnectionLostException,
                    SIIncorrectCallException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "reconstituteEnoughForDeletion",
                        new Object[] { processor, durableSubscriptionsTable });

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(
                        tc,
                        "Reconstituting for deletion "
                                        + (isPubSub() ? "pubsub" : "ptp")
                                        + " BaseDestinationHandler "
                                        + getName());

        initializeNonPersistent(processor, durableSubscriptionsTable, null);

        /*
         * For temporary destinations, we only need to set up the parent streams of
         * the message items and references. Partitioned point to point temporary
         * destinations are not valid, so we don't check for more than one
         * localisation.
         * 
         * Some of these ItemStreams may be recoverable (and hence need to be
         * deleted) and some may not. The exact situation is determined by
         * what the Message Store actually stored (before a non-clean shutdown).
         */
        _protoRealization.reconstituteEnoughForDeletion();

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(
                        tc,
                        "Reconstituted for deletion "
                                        + (isPubSub() ? "pubsub" : "ptp")
                                        + " BaseDestinationHandler "
                                        + getName());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reconstituteEnoughForDeletion");
    }

    /**
     * Call this method to delete a temporary destination which got left behind
     * on a non-clean shutdown of the Messaging Engine. It is not necessary to
     * call {@link #reconstitute} before calling this function.
     * 
     * @param MessageProcessor Message Processor this destination belongs to.
     * @param durableSubscriptionsTable Durable subscriptions table for
     *            topicspaces.
     */
    protected void deleteDirtyTemporary(
                                        MessageProcessor processor,
                                        HashMap<String, Object> durableSubscriptionsTable)
                    throws
                    SIRollbackException,
                    SIConnectionLostException,
                    MessageStoreException,
                    SIIncorrectCallException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "deleteDirtyTemporary",
                        new Object[] { processor, durableSubscriptionsTable });

        reconstituteEnoughForDeletion(processor, durableSubscriptionsTable);

        LocalTransaction siTran = txManager.createLocalTransaction(true);
        removeAll((Transaction) siTran);
        siTran.commit();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteDirtyTemporary");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getInputHandler(com.ibm.ws.sib.common.ProtocolType, com.ibm.ws.sib.trm.topology.Cellule)
     */
    @Override
    public InputHandler getInputHandler(ProtocolType type, SIBUuid8 sourceMEUuid, JsMessage msg)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getInputHandler",
                        new Object[] { type, sourceMEUuid, msg });

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

            // For durable, AIHs are referenced by pseudo destination ID so check for that first
            SIBUuid12 destID = msg.getGuaranteedTargetDestinationDefinitionUUID();
            SIBUuid12 gatheringTargetDestUuid = msg.getGuaranteedGatheringTargetUUID();

            msgHandler = _protoRealization.
                            getRemoteSupport().
                            getAnycastInputHandlerByPseudoDestId(destID);

            // Otherwise, use the uuid of the sourceCellule ME to find the RCD to deliver the message. We assume that this
            // uuid corresponds to that used to choose the RCD initially, and that was used by the associated
            // AIH as the uuid of the DME to which it sent the request for the message.
            if (msgHandler == null)
                msgHandler = getAnycastInputHandler(sourceMEUuid, gatheringTargetDestUuid, true);
        }
        //else
        //{
        //unsupported protocol type
        //return null
        //}

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getInputHandler", msgHandler);

        return msgHandler;
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
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "choosePtoPOutputHandler",
                        new Object[] { fixedMEUuid,
                                      preferredMEUuid,
                                      Boolean.valueOf(localMessage),
                                      Boolean.valueOf(forcePut),
                                      scopedMEs });

        OutputHandler result =
                        _protoRealization.
                                        choosePtoPOutputHandler(fixedMEUuid,
                                                                preferredMEUuid,
                                                                localMessage,
                                                                forcePut,
                                                                _singleServer, // Are we running in a single server environment?
                                                                scopedMEs);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "choosePtoPOutputHandler", result);

        return result;
    }

    /**
     * @return true if the message can be stored on this destination's local or transmit queues.
     *         This is used when messages arrive over a link
     *         See defect 283324
     */
    @Override
    public int checkCanAcceptMessage(SIBUuid8 MEUuid, HashSet<SIBUuid8> scopedMEs)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkCanAcceptMessage", new Object[] { MEUuid, scopedMEs });

        int returnValue = OUTPUT_HANDLER_FOUND;

        // A topic space has a single itemStream for all its messages, check it
        if (isPubSub())
        {
            if (getPublishPoint().isQHighLimit())
                returnValue = OUTPUT_HANDLER_ALL_HIGH_LIMIT;
        }
        // A queue (or link) has a pot per outputHandler (including a local consumerDispatcher)
        // so check them all
        else
        {
            returnValue = checkPtoPOutputHandlers(MEUuid, scopedMEs);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkCanAcceptMessage", Integer.valueOf(returnValue));
        return returnValue;
    }

    /**
     * Checks if there is space available on the outputHandlers (xmit steams)
     * for this message.
     * 
     * @param checkSendAllowed true if the "sendAllowed" flag on local
     *            messaging points should be taken into account. Otherwise they are ignored.
     * 
     * @return int result if an outputHandler can be found to take message, or reason for failure
     */
    @Override
    public int checkPtoPOutputHandlers(SIBUuid8 fixedMEUuid,
                                       HashSet<SIBUuid8> scopedMEs)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkPtoPOutputHandlers",
                        new Object[] { this,
                                      fixedMEUuid,
                                      scopedMEs });

        int result = DestinationHandler.OUTPUT_HANDLER_SEND_ALLOWED_FALSE;

        // First make sure the destination is sendAllowed
        if (isSendAllowed())
        {
            result = DestinationHandler.OUTPUT_HANDLER_NOT_FOUND;
            int localResult = DestinationHandler.NOT_SET;

            // If we have a suitable local message point 
            // then we can only check it if any ME restictions include this local ME.
            boolean checkLocal = false;
            if (hasLocal())
            {
                if ((fixedMEUuid == null) && (scopedMEs == null))
                    checkLocal = true;
                else if ((fixedMEUuid != null) && fixedMEUuid.equals(getMessageProcessor().getMessagingEngineUuid()))
                    checkLocal = true;
                else if ((scopedMEs != null) && scopedMEs.contains(getMessageProcessor().getMessagingEngineUuid()))
                    checkLocal = true;

                if (checkLocal)
                {
                    // Check that it is SendAllowed true and not at high limit
                    localResult = _ptoPRealization.checkAbleToSend();

                }
            }

            // If we don't have a suitable local QP then check all qualifying output handlers
            // (potentially fixed/scoped) for availability
            if ((!_singleServer || isLink()) &&
                (localResult != DestinationHandler.OUTPUT_HANDLER_FOUND) &&
                !(checkLocal && (fixedMEUuid != null))) // Don't bother checking for a remote one if we're fixed to the local one
                result = getLocalisationManager().checkRemoteMessagePointOutputHandlers(fixedMEUuid, scopedMEs);

            // If we still failed to find anything suitable remote from us then we use any
            // local result we have to try to better it
            if ((result == DestinationHandler.OUTPUT_HANDLER_NOT_FOUND) &&
                (localResult != DestinationHandler.NOT_SET))
                result = localResult;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkPtoPOutputHandlers", Integer.valueOf(result));
        return result;
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
            SibTr.entry(tc, "getRemoteConsumerDispatcher", meId);

        AnycastInputHandler aih = getAnycastInputHandler(meId, gatheringTargetDestUuid, createAIH);
        RemoteConsumerDispatcher result = null;
        if (aih != null)
            result = aih.getRCD();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getRemoteConsumerDispatcher", result);

        return result;
    }

    public void setHasReconciledStreamsToBeDeleted(boolean value)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setHasReconciledStreamsToBeDeleted", Boolean.valueOf(value));
        _hasReconciledStreamsToBeDeleted = value;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setHasReconciledStreamsToBeDeleted");
    }

    public boolean getHasReconciledStreamsToBeDeleted()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getHasReconciledStreamsToBeDeleted");
            SibTr.exit(tc, "getHasReconciledStreamsToBeDeleted", Boolean.valueOf(_hasReconciledStreamsToBeDeleted));
        }
        return _hasReconciledStreamsToBeDeleted;
    }

    /**
     * <p>Notify Remote Consumer Dispatchers consumers on the RME for this destination
     * of the change to the receive allowed attribute</p>
     * 
     * @param isReceiveAllowed
     * @param destinationHandler
     */
    @Override
    public void notifyReceiveAllowedRCD(DestinationHandler destinationHandler)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "notifyReceiveAllowedRCD", new Object[] { destinationHandler });

        _protoRealization.
                        getRemoteSupport().
                        notifyReceiveAllowedRCD(destinationHandler);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "notifyReceiveAllowedRCD");
    }

    /**
     * <p>Notify Remote Consumer Dispatchers consumers on the RME for this destination
     * of the change to the receive exclusive attribute</p>
     * 
     * @param isReceiveExclusive
     */
    @Override
    public void notifyRCDReceiveExclusiveChange(boolean isReceiveExclusive)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "notifyRCDReceiveExclusiveChange", new Object[] { Boolean.valueOf(isReceiveExclusive) });

        // Only do this for ptp as it does not seem to make sense for p/s
        if (!isPubSub())
        {
            //if this is a RMQ destination then there is no RCD to
            //worry about, so we only need be concerned with the case of a JS
            //q point.
            if (_ptoPRealization != null)
            {
                _ptoPRealization.getRemoteSupport().
                                notifyRCDReceiveExclusiveChange(isReceiveExclusive);
            }

        }
        else
        {
            // sanity check
            // log error
            SIErrorException e =
                            new SIErrorException(
                                            nls.getFormattedMessage(
                                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                                    new Object[] {
                                                                                  "com.ibm.ws.sib.processor.impl.BaseDestinationHandler",
                                                                                  "1:1458:1.700.3.45" },
                                                                    null));
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.BaseDestinationHandler.notifyRCDReceiveExclusiveChange",
                                        "1:1463:1.700.3.45",
                                        this);
            SibTr.exception(tc, e);

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.BaseDestinationHandler",
                                      "1:1470:1.700.3.45" });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "notifyRCDReceiveExclusiveChange", e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "notifyRCDReceiveExclusiveChange");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getControlHandler(com.ibm.ws.sib.common.ProtocolType, com.ibm.ws.sib.trm.topology.Cellule)
     */
    @Override
    public ControlHandler getControlHandler(
                                            ProtocolType type,
                                            SIBUuid8 sourceMEUuid,
                                            ControlMessage msg)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "getControlHandler",
                        new Object[] { type, sourceMEUuid, msg });

        ControlHandler msgHandler = null;

        if (type == ProtocolType.UNICASTINPUT)
        {
            // do nothing, check the tSIB code,
        }
        else if (type == ProtocolType.UNICASTOUTPUT)
        {
            // do nothing, check the tSIB code,
        }
        else if (type == ProtocolType.PUBSUBINPUT)
        {
            msgHandler = (ControlHandler) inputHandler;
        }
        else if (type == ProtocolType.PUBSUBOUTPUT)
        {
            msgHandler = _pubSubRealization.getControlHandler(sourceMEUuid);
        }
        else if (type == ProtocolType.ANYCASTINPUT)
        {
            // For durable, AIHs are referenced by pseudo destination ID so check for that first
            SIBUuid12 destID =
                            msg.getGuaranteedTargetDestinationDefinitionUUID();
            SIBUuid12 gatheringTargetDestUuid = msg.getGuaranteedGatheringTargetUUID();

            if (isPubSub())
            {
                msgHandler = _protoRealization.
                                getRemoteSupport().
                                getAnycastInputHandlerByPseudoDestId(destID);

            }

            // Otherwise, use the uuid of the sourceCellule ME to find the AIH to handle the message. We assume that this
            // uuid corresponds to that used to choose the AIH initially, and that was used by the AIH as the
            // uuid of the DME to which it sent the request for the message.

            /**
             * 466323
             * We now no longer create the AnycastInputHandler (+ its persistent resources) when
             * a message arrives and we cant find an existing one.
             * 
             * If the RME AIH resources do not exist, this means we *must* have previously completed
             * a successful flush. This means that the DME has sent us a flush. If they have done
             * this then they must be cleaned up their end.
             * 
             * We therefore do not create new resources when the AIH cannot be found. Instead we
             * generate warnings at the RemoteMessageReceiver level to indicate cleanup is required
             * on the DME. We should only be able to get into this situation if the msgStore for the
             * RME has been deleted (either db tables removed or ME was restarted based on a backup db).
             */
            if (msgHandler == null)
                msgHandler = getAnycastInputHandler(sourceMEUuid, gatheringTargetDestUuid, false);
        }
        else if (type == ProtocolType.ANYCASTOUTPUT)
        {
            // For durable, AOHs are referenced by pseudo destination ID so check for that first
            SIBUuid12 destID = msg.getGuaranteedTargetDestinationDefinitionUUID();

            msgHandler = _protoRealization.
                            getRemoteSupport().
                            getAnycastOutputHandlerByPseudoDestId(destID);

            // Otherwise, assume this is a queueing access and get the output handler as usual
            if (msgHandler == null)
                msgHandler = getAnycastOutputHandler();
        }
        //else
        // {
        //unsupported protocol type
        //return null
        // }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getControlHandler", msgHandler);

        return msgHandler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDurableSubscriptionConsumerDispatcher(com.ibm.ws.sib.processor.impl.ConsumerDispatcherState)
     */
    @Override
    public ConsumerDispatcher getDurableSubscriptionConsumerDispatcher(ConsumerDispatcherState subState)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDurableSubscriptionConsumerDispatcher", subState);

        ConsumerDispatcher consumerDispatcher = _pubSubRealization.
                        getDurableSubscriptionConsumerDispatcher(subState);

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
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getLocalPtoPConsumerDispatcher()
     */
    @Override
    public ConsumerManager getLocalPtoPConsumerManager()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getLocalPtoPConsumerManager");

        ConsumerManager consumerManager = null;

        consumerManager = _ptoPRealization.getLocalPtoPConsumerManager();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getLocalPtoPConsumerManager", consumerManager);

        return consumerManager;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#createSubscriptionConsumerDispatcher(com.ibm.ws.sib.processor.impl.ConsumerDispatcherState)
     */
    @Override
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

        final ConsumerDispatcher cd = _pubSubRealization.
                        createSubscriptionConsumerDispatcher(subState);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createSubscriptionConsumerDispatcher", cd);

        return cd;
    }
    @Override
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

        final ConsumerKey consumerKey = _pubSubRealization.
                        createSubscriptionConsumerDispatcherAndAttachCP(consumerPoint, subState);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createSubscriptionConsumerDispatcherAndAttachCP", consumerKey);

        return consumerKey;
    }

    /**
     * Called to get the AnycastOutputHandler for this Destination
     * 
     * @return
     */
    public final synchronized AnycastOutputHandler getAnycastOutputHandler()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getAnycastOutputHandler");

        AnycastOutputHandler aoh = null;
        if (_ptoPRealization != null)
            aoh = _ptoPRealization.getAnycastOutputHandler(definition, false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getAnycastOutputHandler", aoh);

        return aoh;
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

        PubSubOutputHandler handler =
                        _pubSubRealization.getPubSubOutputHandler(neighbourUUID);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getPubSubOutputHandler", handler);

        return handler;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#createPubSubOutputHandler(com.ibm.ws.sib.processor.proxyhandler.Neighbour)
     */
    @Override
    public synchronized PubSubOutputHandler createPubSubOutputHandler(Neighbour neighbour)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createPubSubOutputHandler", new Object[] { neighbour });

        PubSubOutputHandler handler =
                        _pubSubRealization.createPubSubOutputHandler(neighbour);

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

        HashMap handlers =
                        _pubSubRealization.getAllPubSubOutputHandlers();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getAllPubSubOutputHandlers", handlers);

        return handlers;
    }

    /**
     * Makes a copy of the PSOH map as it currently
     * stands and does not leave it locked.
     * 
     * @return
     * @author tpm
     */
    public HashMap cloneAllPubSubOutputHandlers()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "cloneAllPubSubOutputHandlers");

        HashMap clone =
                        _pubSubRealization.cloneAllPubSubOutputHandlers();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "cloneAllPubSubOutputHandlers", clone);
        return clone;
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

        _pubSubRealization.unlockPubsubOutputHandlers();

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

        _pubSubRealization.deletePubSubOutputHandler(neighbourUUID);

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

        _pubSubRealization.deleteAllPubSubOutputHandlers();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteAllPubSubOutputHandlers");
    }

    /**
     * This method checks to see if rollback is required
     * 
     * @param transaction The transaction to rollback.
     */
    public void handleRollback(LocalTransaction transaction)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "handleRollback", transaction);

        // Roll back the transaction if we created it.
        if (transaction != null)
        {
            try
            {
                transaction.rollback();
            } catch (SIException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.BaseDestinationHandler.handleRollback",
                                            "1:1828:1.700.3.45",
                                            this);

                SibTr.exception(tc, e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "handleRollback");
    }

    /**
     * Returns an array of all pseudoDestination UUIDs which should
     * be mapped to this BaseDestinationHandler. This method is
     * used by the DestinationManager to determine what pseudo
     * references need to be added after a destination is
     * reconstituted.
     * 
     * @return An array of all pseudoDestination UUIDs to be mapped
     *         to this BaseDestinationHandler.
     */
    public Object[] getPostReconstitutePseudoIds()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getPostReconstitutePseudoIds");

        Object[] result = _protoRealization.
                        getRemoteSupport().
                        getPostReconstitutePseudoIds();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getPostReconstitutePseudoIds", result);

        return result;
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
                                      Boolean.valueOf(isNonDurable),
                                      Boolean.valueOf(callProxyCode) });

        _pubSubRealization.dereferenceSubscriptionConsumerDispatcher(cd,
                                                                     isNonDurable,
                                                                     callProxyCode);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "dereferenceSubscriptionConsumerDispatcher");
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
                    throws
                    SIDiscriminatorSyntaxException,
                    SISelectorSyntaxException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "addConsumerPointMatchTarget",
                        new Object[] { consumerPointData, criteria });

        // Put the consumer point into the matchspace
        // This used to use the ptoPRealization but this is no longer the case.

        this.messageProcessor.
                        getMessageProcessorMatching().
                        addConsumerPointMatchTarget(
                                                    consumerPointData, // N.B we use the raw CP as key
                                                    getUuid(), // destination uuid
                                                    cmUuid, // consumer manager uuid
                                                    criteria); // discriminator & selector

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

        // Remove the consumer point from the matchspace
        // This used to use the ptoPRealization but this is no longer the case.

        messageProcessor
                        .getMessageProcessorMatching()
                        .removeConsumerPointMatchTarget(
                                                        consumerPointData);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeConsumerPointMatchTarget");
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
                    throws
                    SIResourceException
    {
        // F001333-14610
        // Delegate down onto the new method passing a null
        // subscription ID.
        return handleUndeliverableMessage(msg, exceptionReason, exceptionInserts, tran, null);
    }

    // F001333-14610
    // New method that accepts a subscription ID for the undeliverable
    // message for inclusion in the exception data.
    public UndeliverableReturnCode handleUndeliverableMessage(
                                                              SIMPMessage msg,
                                                              int exceptionReason,
                                                              String[] exceptionInserts,
                                                              TransactionCommon tran,
                                                              String subscriptionID)
                    throws
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "handleUndeliverableMessage",
                        new Object[] { msg, Integer.valueOf(exceptionReason), exceptionInserts, subscriptionID });

        //If an ExceptionDestinationHandlerImpl instance does not exist yet, then create it
        if (_exceptionDestination == null)
        {
            _exceptionDestination = new ExceptionDestinationHandlerImpl(this);
        }

        boolean tranCreated = false;
        if (tran == null)
        {
            /* Create a new transaction under which to perform the reroute */
            tran = txManager.createLocalTransaction(false);
            tranCreated = true;
        }
        UndeliverableReturnCode rc = null;

        try
        {

            /* Pass the undeliverable message to the exception destination */
            // F001333-14610
            // Pass the subscription ID to the exception handler
            // so that it can be set on the exceptioned message.
            rc = _exceptionDestination.handleUndeliverableMessage(msg,
                                                                  tran,
                                                                  exceptionReason,
                                                                  exceptionInserts,
                                                                  subscriptionID);

            if (rc == UndeliverableReturnCode.OK
                || rc == UndeliverableReturnCode.DISCARD)
            {
                // Message is no longer required. Remove from the itemstream
                try
                {
                    if (msg.isInStore())
                    {
                        Transaction msTran = messageProcessor.resolveAndEnlistMsgStoreTransaction(tran);
                        if (rc == UndeliverableReturnCode.DISCARD) {
                            SibTr.info(tc, "DISCARD_MESSAGE_INFO_CWSIP00216", new Object[] { msg.getID() });
                        }
                        msg.remove(msTran, msg.getLockID());
                    }
                } catch (MessageStoreException e)
                {
                    // MessageStoreException shouldn't occur so FFDC.
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.BaseDestinationHandler.handleUndeliverableMessage",
                                                "1:2051:1.700.3.45",
                                                this);

                    SibTr.exception(tc, e);
                    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                new Object[] {
                                              "com.ibm.ws.sib.processor.impl.BaseDestinationHandler",
                                              "1:2058:1.700.3.45",
                                              e,
                                              getName() });

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "handleUndeliverableMessage", e);

                    throw new SIResourceException(
                                    nls.getFormattedMessage(
                                                            "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                                            new Object[] {
                                                                          "com.ibm.ws.sib.processor.impl.BaseDestinationHandler",
                                                                          "1:2070:1.700.3.45",
                                                                          e,
                                                                          getName() },
                                                            null),
                                    e);
                }

                try
                {
                    if (tranCreated)
                        ((LocalTransaction) tran).commit();
                } catch (SIException e)
                {
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.BaseDestinationHandler.handleUndeliverableMessage",
                                                "1:2087:1.700.3.45",
                                                this);

                    SibTr.exception(tc, e);
                    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                new Object[] {
                                              "com.ibm.ws.sib.processor.impl.BaseDestinationHandler",
                                              "1:2094:1.700.3.45",
                                              e });

                    /* Persist operation FAILED therefore throw exception */
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "handleUndeliverableMessage", e);

                    if (e instanceof SIRollbackException)
                        tran = null;

                    throw new SIResourceException(
                                    nls.getFormattedMessage(
                                                            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                            new Object[] {
                                                                          "com.ibm.ws.sib.processor.impl.BaseDestinationHandler",
                                                                          "1:2109:1.700.3.45",
                                                                          e },
                                                            null),
                                    e);
                }
            }
            else if (rc == UndeliverableReturnCode.BLOCK
                     || rc == UndeliverableReturnCode.ERROR)
            {
                // Message could not be delivered. Unlock the message to make it available to other
                // consumers
                if (tranCreated)
                    handleRollback((LocalTransaction) tran);
            }
        } catch (SIResourceException e)
        {
            // No FFDC code needed
            if (tranCreated)
                handleRollback((LocalTransaction) tran);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "handleUndeliverableMessage", e);
            }

            throw e;
        } catch (RuntimeException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.BaseDestinationHandler.handleUndeliverableMessage",
                                        "1:2145:1.700.3.45",
                                        this);

            if (tranCreated)
                handleRollback((LocalTransaction) tran);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.exception(tc, e);
                SibTr.exit(tc, "handleUndeliverableMessage", e);
            }

            // 533027 Throwing a runtime exception out of this code is not appropriate,
            // as we do not want our caller to fail because of an unexpected condition
            // encountered while sending a message to the exception destination.
            // In particular, exceptions generated within processor and thrown across
            // message store boundaries (from inside callbacks) show up as runtime
            // exceptions at this level.
            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.BaseDestinationHandler",
                                                                  "1:2168:1.700.3.45",
                                                                  e },
                                                    null),
                            e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "handleUndeliverableMessage", rc);

        return rc;

    }

    private void notifyWaitingThread()
    {
        synchronized (this)
        {
            notifyAll();
        }
    }

    /**
     * Attach a Localisation to this Destination's Localisations.
     * <p>
     * This entails:
     * <p>
     * 1. Initializing the Localisation with this BaseDestinationHandler's details.
     * 2. Adding the Localisation to this BaseDestinationHandler's list.
     * 3. Creating new input/output handlers as appropriate.
     * <p>
     * Feature 174199.2.7
     * 
     * @param ptoPMessageItemStream is the PtoPMessageItemStream to add.
     * @param localisationIsRemote should be true if the PtoPMessageItemStream is remote.
     * @param transaction is the Transaction to add it under.
     */
    public void attachPtoPLocalisation(
                                       PtoPMessageItemStream ptoPMessageItemStream,
                                       boolean localisationIsRemote) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "attachPtoPLocalisation",
                        new Object[] {
                                      ptoPMessageItemStream,
                                      Boolean.valueOf(localisationIsRemote) });

        // Call through to the ptoPRealization
        _ptoPRealization.attachPtoPLocalisation(ptoPMessageItemStream,
                                                localisationIsRemote);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "attachPtoPLocalisation");
    }

    /**
     * Create a new PtoPMessageItemStream and add it to this Destination's Localisations.
     * <p>
     * In addition to creating and adding it, this function also performs all the
     * necessary updates to make it a recognized part of the Destination.
     * 
     * @param localisationIsRemote should be true if the localisation is remote.
     * @param transaction The Transaction to add under. Cannot be null.
     * @param messagingEngineUuid The uuid of the messaging engine that owns the localisation
     * @return PtoPMessageItemStream the new PtoPMessageItemStream added.
     * 
     * @throws SIResourceException if the add fails due to a Message Store problem.
     */
    protected LocalizationPoint addNewPtoPLocalization(
                                                       boolean localisationIsRemote,
                                                       TransactionCommon transaction,
                                                       SIBUuid8 messagingEngineUuid,
                                                       LocalizationDefinition destinationLocalizationDefinition,
                                                       boolean queuePoint) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "addNewPtoPLocalization",
                        new Object[] {
                                      Boolean.valueOf(localisationIsRemote),
                                      transaction,
                                      messagingEngineUuid,
                                      destinationLocalizationDefinition,
                                      Boolean.valueOf(queuePoint) });

        if (_ptoPRealization == null)
        {
            _ptoPRealization = new JSPtoPRealization(this, messageProcessor, _localisationManager);
            _ptoPRealization.initialise();
        }

        LocalizationPoint newMsgItemStream =
                        _ptoPRealization.
                                        addNewPtoPLocalization(localisationIsRemote,
                                                               transaction,
                                                               messagingEngineUuid,
                                                               destinationLocalizationDefinition,
                                                               queuePoint);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addNewPtoPLocalization", newMsgItemStream);

        return newMsgItemStream;
    }

    /**
     * Add PubSubLocalisation.
     * 
     */
    protected void addPubSubLocalisation(
                                         LocalizationDefinition destinationLocalizationDefinition)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "addPubSubLocalisation",
                        new Object[] {
                        destinationLocalizationDefinition });

        _pubSubRealization.addPubSubLocalisation(destinationLocalizationDefinition);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addPubSubLocalisation");

    }

    /**
     * Method assignQueuePointOutputHandler.
     * 
     * @param outputHandler
     *            <p>Add the outputHandler to the set of queuePointOutputHanders</p>
     */
    public void assignQueuePointOutputHandler(
                                              OutputHandler outputHandler,
                                              SIBUuid8 messagingEngineUuid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "assignQueuePointOutputHandler",
                        new Object[] { outputHandler, messagingEngineUuid });

        _ptoPRealization.assignQueuePointOutputHandler(outputHandler,
                                                       messagingEngineUuid);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "assignQueuePointOutputHandler");
    }

    @Override
    public boolean isCorruptOrIndoubt()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isCorruptOrIndoubt");
            SibTr.exit(tc, "isCorruptOrIndoubt", new Boolean(_isCorruptOrIndoubt));
        }

        return _isCorruptOrIndoubt;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#setCorrupt(boolean)
     */
    @Override
    public synchronized void setCorrupt(boolean newCorrupt)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setCorrupt", Boolean.valueOf(newCorrupt));

        boolean oldCorrupt = _isCorruptOrIndoubt;
        _isCorruptOrIndoubt = newCorrupt;

        // PK54812 We need to pass this through to the destination manager to
        // actually change the state of the destination.
        if (newCorrupt && !oldCorrupt)
        {
            destinationManager.corruptDestination(this);

            // Log a message with details of the destination that is corrupt
            SibTr.info(tc_cwsik, "DELIVERY_ERROR_SIRC_27",
                       new Object[] { getName() + " " + getUuid().toString() });
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setCorrupt");
    }

    @Override
    public synchronized void setIndoubt(boolean newIndoubt)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setIndoubt", new Boolean(newIndoubt));

        _isCorruptOrIndoubt = newIndoubt;

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

        boolean hasLocal = getLocalisationManager().hasLocal();

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

        boolean hasRemote = getLocalisationManager().hasRemote();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "hasRemote", Boolean.valueOf(hasRemote));

        return hasRemote;
    }

    /**
     * Do we have a remote localisation?
     */
    public void setRemote(boolean hasRemote)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setRemote", Boolean.valueOf(hasRemote));

        getLocalisationManager().setRemote(hasRemote);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setRemote");
    }

    /**
     * Do we have a local localisation?
     */
    public void setLocal()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setLocal");

        getLocalisationManager().setLocal();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setLocal");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isTemporary()
     */
    @Override
    public boolean isTemporary()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isTemporary");
            SibTr.exit(tc, "isTemporary", Boolean.valueOf(_isTemporary));
        }

        return _isTemporary;
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
        {
            SibTr.entry(
                        tc,
                        "isToBeDeleted",
                        new Object[] { getName(), getUuid().toString() });
            SibTr.exit(tc, "isToBeDeleted", Boolean.valueOf(_toBeDeleted));
        }

        return _toBeDeleted;
    }

    /**
     * Are we ignoring this destination handler due to corruption?
     * 
     * @return
     */
    public boolean isToBeIgnored()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isToBeIgnored");
            SibTr.exit(tc, "isToBeIgnored", Boolean.valueOf(_toBeIgnored));
        }

        return _toBeIgnored;
    }

    /**
     * This method is used where a destination has been marked for deletion but where we need
     * to ensure that all the preparatory work for deletion been completed.
     * 
     * @return
     */
    public synchronized void setDeleteInProgress(boolean deleteInProgress)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setDeleteInProgress");

        _deleteInProgress = deleteInProgress;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setDeleteInProgress", Boolean.valueOf(_deleteInProgress));
    }

    /**
     * This method is used where a destination has been marked for deletion but where we need
     * to ensure that all the preparatory work for deletion been completed.
     * 
     * @return
     */
    public synchronized boolean isDeleteInProgress()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isDeleteInProgress");
            SibTr.exit(tc, "isDeleteInProgress", Boolean.valueOf(_deleteInProgress));
        }

        return _deleteInProgress;
    }

    /**
     * Mark the DestinationHandler for deletion.
     */
    public void setToBeDeleted(boolean delete)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setToBeDeleted", Boolean.valueOf(delete));

        _toBeDeleted = delete;

        if (delete)
        {
            // Tell the associated statistics that they should disappear.
            if (_protoRealization != null)
                _protoRealization.setToBeDeleted();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setToBeDeleted");
    }

    /**
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#reset()
     * 
     *      For a base destination, a reset will only take affect on restart. Also,
     *      only corrupt destinations can be reset. A reset performed on a non-corrupt
     *      destination will have no effect.
     */
    @Override
    public void reset()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "reset");

        if (_isCorruptOrIndoubt)
        {
            _toBeIgnored = true;

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(
                            tc, "Destination " + getName() + " on bus " + getBus() +
                                " set to be ignored on restart");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reset");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream#restore(java.io.ObjectInputStream, int)
     */
    @Override
    public void restore(ObjectInputStream ois, int dataVersion)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "restore", new Object[] { ois, Integer.valueOf(dataVersion) });

        checkPersistentVersionId(dataVersion);

        try
        {
            HashMap hm = (HashMap) ois.readObject();

            restorePersistentDestinationData(hm);
        } catch (Exception e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.BaseDestinationHandler.restore",
                                        "1:2664:1.700.3.45",
                                        this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.BaseDestinationHandler",
                                      "1:2671:1.700.3.45",
                                      e });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "restore", "SIErrorException");
            throw new SIErrorException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.BaseDestinationHandler",
                                                                  "1:2681:1.700.3.45",
                                                                  e },
                                                    null),
                            e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "restore");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream#restorePersistentDestinationData(java.io.ObjectInputStream, int)
     */
    public void restorePersistentDestinationData(HashMap hm) throws Exception
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "restorePersistentDestinationData", new Object[] { hm });
        //, Integer.valueOf(dataVersion)

        super.restorePersistentData(hm);

        // Restore boolean flags
        //isLocal = ((Boolean) hm.get("isLocal")).booleanValue();
        _toBeDeleted = ((Boolean) hm.get("toBeDeleted")).booleanValue();
        _isTemporary = ((Boolean) hm.get("isTemporary")).booleanValue();
        _toBeIgnored = ((Boolean) hm.get("toBeIgnored")).booleanValue();
        _isSystem =
                        definition.getName().startsWith(SIMPConstants.SYSTEM_DESTINATION_PREFIX);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "restorePersistentDestinationData");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPReferenceStream#getVersion()
     */
    @Override
    public int getPersistentVersion()
    {
        return PERSISTENT_VERSION;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.AbstractItem#getPersistentData()
     */
    @Override
    public void getPersistentData(ObjectOutputStream oos)
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getPersistentData", oos);

        try
        {
            HashMap hm = new HashMap();

            addPersistentDestinationData(hm);

            oos.writeObject(hm);
        } catch (java.io.IOException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.BaseDestinationHandler.getPersistentData",
                                        "1:2759:1.700.3.45",
                                        this);

            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "getPersistentData", "SIErrorException");

            // Write the error to the console
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.BaseDestinationHandler",
                                      "1:2770:1.700.3.45",
                                      e,
                                      getName() });

            throw new SIErrorException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0003",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.BaseDestinationHandler",
                                                                  "1:2779:1.700.3.45",
                                                                  e,
                                                                  getName() },
                                                    null),
                            e);

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getPersistentData");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.AbstractItem#addPersistentDestinationData()
     */
    @SuppressWarnings("unchecked")
    public void addPersistentDestinationData(HashMap hm)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "addPersistentDestinationData", hm);

        super.addPersistentData(hm);

        // Is the destination marked as to be deleted
        hm.put("toBeDeleted", Boolean.valueOf(_toBeDeleted));

        // Is the destination temporary
        hm.put("isTemporary", Boolean.valueOf(_isTemporary));

        hm.put("toBeIgnored", Boolean.valueOf(_toBeIgnored));

        // Is the destination localised on the home ME
        //hm.put("isLocal", Boolean.valueOf(isLocal));

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addPersistentDestinationData");
    }

    /**
     * Retrieve the subscription index for this destination.
     * 
     * @return SubscriptionIndex.
     */
    @Override
    public SubscriptionIndex getSubscriptionIndex()
    {
        return _pubSubRealization.getSubscriptionIndex();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#updateDefinition(com.ibm.ws.sib.admin.DestinationDefinition)
     */
    @Override
    public void updateDefinition(BaseDestinationDefinition destinationDefinition)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "updateDefinition",
                        new Object[] { destinationDefinition });

        DestinationDefinition oldDefinition = definition;
        DestinationDefinition newDefinition = (DestinationDefinition) destinationDefinition;

        super.updateDefinition(destinationDefinition);

        _maxFailedDeliveries = definition.getMaxFailedDeliveries();

        // at runtime the blockedretryinterval was not getting updated
        // since this piece of code was missing
        _blockedRetryInterval = definition.getBlockedRetryTimeout();

        //update TRM if necessary
        registerDestination();

        // Convert array of names to list of jsDestinationAddresses
        QualifiedDestinationName[] names = definition.getForwardRoutingPath();

        if (names != null)
        {
            ArrayList newFRP = new ArrayList(names.length);
            for (int name = 0; name < names.length; name++)
            {
                newFRP.add(SIMPUtils.createJsDestinationAddress(
                                                                names[name].getDestination(),
                                                                null,
                                                                names[name].getBus()));
            }
            _forwardRoutingPath = newFRP;
        }
        else
        {
            _forwardRoutingPath = null;
        }

        _replyDestination = null;
        _replySet = false;

        // Check whether send allowed has changed
        if (oldDefinition != null &&
            (oldDefinition.isSendAllowed() != newDefinition.isSendAllowed()))
        {
            fireSendAllowedStateChangeEvent(newDefinition.isSendAllowed());
        }

        if (oldDefinition != null &&
            (oldDefinition.isReceiveAllowed() != newDefinition.isReceiveAllowed()))
        {
            fireReceiveAllowedStateChangeEvent(newDefinition.isReceiveAllowed());
        }

        if (!_reconciled)
            setIsOrdered(definition.isOrderingRequired());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updateDefinition");
    }

    @Override
    protected void notifyAOHReceiveExclusiveChange(boolean newValue)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "notifyAOHReceiveExclusiveChange",
                        new Object[] { Boolean.valueOf(newValue) });

        // only need to do this for queues, not for durable subscriptions
        synchronized (this)
        {
            _protoRealization.
                            getRemoteSupport().
                            notifyAOHReceiveExclusiveChange(newValue);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "notifyAOHReceiveExclusiveChange");

    }

    /**
     * <p>Register the destination vith WLM via TRM</p>
     * 
     * System destinations and Temporary destinations are not registered
     * with WLM. The destinations themselves have their own addressing
     * mechanisms.
     */
    void registerDestination()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "registerDestination");

        // Dont register temporary destinations or system destinations.
        if (!isTemporary() && !_isSystem)
        {
            getLocalisationManager().registerDestination();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerDestination");
    }

    /**
     * <p>Deregister the destination vith WLM via TRM</p>
     * 
     * System destinations and Temporary destinations are not registered
     * with WLM. The destinations themselves have their own addressing
     * mechanisms.
     */
    void deregisterDestination()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deregisterDestination");

        // Dont register temporary destinations or system destinations.
        if (!isTemporary() && !_isSystem)
        {
            getLocalisationManager().deregisterDestination();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deregisterDestination");
    }

    /**
     * Get the consumer lock object
     * 
     * @return
     */
    public Object getMediationConsumerLockObject()
    {
        return new Object();
    }

    /**
     * <p>For pt-to-pt destination, iterates over each itemstream holding messages
     * for a localisation of the destination and re-allocates those that are marked
     * as to-be-deleted. If at the end of the reallocation, the localisation
     * is eligible for deletion, then on return from the localisations rejig
     * call it will have been deleted from the destination handler.</p>
     * 
     * @return true if the destination has been cleaned up. This may include
     *         deleting the destinationHandler, if all the localisations are awaiting
     *         deletion and are cleaned up successfully.
     */
    public boolean cleanupBaseDestination()
                    throws
                    SIRollbackException,
                    SIConnectionLostException,
                    SIIncorrectCallException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "cleanupBaseDestination");

        // true if all localisations have been cleaned up successfully
        boolean allCleanedUp = true;

        // We must get the locks in the same order as
        // DestinationManager.deleteDestinationLocalization, as it's possible for us
        // to be in this method on the asynch deletion thread doing CLEANUP_PENDING
        // related work while a delete comes in on another thread
        synchronized (getMediationConsumerLockObject()) // Lock1
        {

            //TODO concern over "guest" producers, like forwrd routing paths, put1ers etc
            //they dont have a producersession, we need a shared lock they can take to
            //allow them to finish a send before clean up completes.

            //Check the destination has not already been deleted first
            // Don't try to do anything with corrupt destinations at this point.
            if (!isDeleted() && !isCorruptOrIndoubt())
            {
                if (!isPubSub())
                    _protoRealization.
                                    getReallocationLockManager().
                                    lockExclusive(); // Lock2
                try
                {
                    synchronized (this) // Lock3 Stop cleanup happening while the destination is being altered
                    { //synchronizes with updateLocalisationSet

                        // Now that we are synchronized, check another thread has not begun deletion
                        // logic for this destination. If it has, we shouldn't attempt to continue.
                        // Instead, we should return without performing any work because the other
                        // thread will restart this logic when the destination is ready for deletion.
                        if (isDeleteInProgress())
                        {
                            allCleanedUp = false;
                        }
                        else
                        {
                            //Clean-up the itemstreams for queue points, and
                            //transmission queues
                            allCleanedUp = cleanupLocalisations();

                            //Code moved from cleanupLocalisations so we only do it IF the destination
                            //is being deleted
                            if (isToBeDeleted())
                            {
                                // Last but not least, make sure the in doubt data has
                                // been handled before we try to remove anything.
                                if (!assureAllFlushed())
                                {
                                    allCleanedUp = false;
                                }
                            }
                            //End of code move

                            if (!isPubSub())
                            {
                                // If the destination is being completely removed and the delete of
                                // the localisations was succesful, then proceed with removing the
                                // destination handler.
                                if ((isToBeDeleted()) && (allCleanedUp))
                                {

                                    try
                                    {
                                        LocalTransaction siTran =
                                                        txManager.createLocalTransaction(false);

                                        //First ensure there are no batched deletes of protocol items
                                        BatchHandler sourceBatchHandler =
                                                        messageProcessor.getSourceBatchHandler();
                                        sourceBatchHandler.completeBatch(true);

                                        // Remove any protocol items
                                        _protoRealization.
                                                        getRemoteSupport().
                                                        removeProtocolItems(siTran);

                                        _protoRealization.
                                                        getRemoteSupport().
                                                        resetProtocolStreams();

                                        // 176558.3.1 - Register the transaction handler for callback on transaction completion
                                        siTran.registerCallback(
                                                        new DestinationRemoveTransactionCallback());

                                        // Useful debug
                                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                        {
                                            Statistics stats = getStatistics();
                                            SibTr.debug(tc, "Destination :" + getName() + " " + getUuid().toString() +
                                                            " Adding : " + stats.getAddingItemCount() +
                                                            " Available : " + stats.getAvailableItemCount() +
                                                            " Expiring : " + stats.getExpiringItemCount() +
                                                            " Locked : " + stats.getLockedItemCount() +
                                                            " Removing : " + stats.getRemovingItemCount() +
                                                            " Total : " + stats.getTotalItemCount() +
                                                            " Unavailable : " + stats.getUnavailableItemCount() +
                                                            " Updating : " + stats.getUpdatingItemCount());
                                        }

                                        // All the localisations are deleted now, so the destinationHandler itself can
                                        // also be deleted.
                                        Transaction msTran = messageProcessor.resolveAndEnlistMsgStoreTransaction(siTran);
                                        remove(msTran, NO_LOCK_ID);

                                        siTran.commit();

                                        //Remember that the destination has been deleted
                                        setDeleted();

                                        //Pt-to-pt destinations are remembered in destinationManager so that
                                        //inbound messages from other ME's can be sent to them.  Remove this
                                        //lookup now the destination has been deleted.

                                        //TODO Im concerned about a timing window here.  If another ME creates
                                        //a stream after we've cleaned up our destination, but before we remove
                                        //it from the uuid set, then an attempt could be made to use this
                                        //destinationHandler when its not fit for use.  Perhaps this removal
                                        //could be moved to the point where the inbound stream state is all
                                        //cleaned up? DM
                                    } catch (MessageStoreException e)
                                    {
                                        FFDCFilter.processException(
                                                                    e,
                                                                    "com.ibm.ws.sib.processor.impl.BaseDestinationHandler.cleanupBaseDestination",
                                                                    "1:3138:1.700.3.45",
                                                                    this);

                                        SibTr.exception(tc, e);

                                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                            SibTr.exit(tc, "cleanupBaseDestination", "SIResourceException");

                                        throw new SIResourceException(e);
                                    }
                                }
                            }
                            else //Destination is a pub/sub topicspace
                            {
                                if ((isToBeDeleted()) && (allCleanedUp))
                                {
                                    //signal to _pubSubRealization to gracefully exit from deleteMsgsWithNoReferences()
                                    //as cleaning up destination is started from cleanupDestination()
                                    _pubSubRealization.stopDeletingMsgsWihoutReferencesTask(true);
                                    _pubSubRealization.cleanupDestination();
                                }
                            }

                            if (getHasReconciledStreamsToBeDeleted())
                            {
                                //this is no longer the case
                                setHasReconciledStreamsToBeDeleted(false);
                            }
                        } // !deleteInProgress
                    } // Lock3
                } finally
                {
                    if (!isPubSub())
                        _protoRealization.
                                        getReallocationLockManager().
                                        unlockExclusive(); // Lock2
                }
            }//end !deleted !corrupt

        } // Lock1

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "cleanupBaseDestination", Boolean.valueOf(allCleanedUp));

        return allCleanedUp;
    }

    public boolean cleanupDestination()
                    throws
                    SIRollbackException,
                    SIConnectionLostException,
                    SIIncorrectCallException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "cleanupDestination");

        // will be set to true if all localisations are cleaned up successfully
        boolean allCleanedUp = cleanupBaseDestination();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "cleanupDestination", Boolean.valueOf(allCleanedUp));

        return allCleanedUp;
    }

    /**
     * <p>Cleanup any localisations of the destination that require it</p>
     */
    boolean cleanupLocalisations() throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "cleanupLocalisations");

        // true if all localisations have been cleaned up successfully
        boolean allCleanedUp = true;

        // True if we determine calling reallocation is appropriate
        boolean reallocationCanBeCalled = false;

        //Check the destination has not already been deleted first
        if (!isDeleted())
        {
            synchronized (this) //Stop cleanup happening while the destination is being altered
            {
                //synchronizes with updateLocalisationSet
                //If the entire destination is to be deleted
                //then all the localisations are to be deleted.
                // 448943 Remove a check for !isReconciled - We dont need this
                if (isToBeDeleted())
                {
                    addAllLocalisationsForCleanUp();
                }

                allCleanedUp = _protoRealization.cleanupPremediatedItemStreams();

                if (!isPubSub())
                {
                    //If the entire destination is being deleted, clean up the remote get
                    //infrastructure
                    if (isToBeDeleted())
                    {
                        boolean remoteGetCleanup = _ptoPRealization.cleanupLocalisations();
                        if (allCleanedUp)
                            allCleanedUp = remoteGetCleanup;
                    }
                    else
                    {
                        // PK57432 We are not deleting the destination. Just perform
                        // any other localisation cleanup
                        reallocationCanBeCalled = true;
                    }
                }
                else
                {
                    // For pub/sub, we'll want to remove any infrastructure associated with
                    // durable subscriptions since these are implemented as queue-like localizations.
                    if (isToBeDeleted())
                    {
                        boolean pubSubCleanedUp = _pubSubRealization.cleanupLocalisations();
                        if (allCleanedUp)
                            allCleanedUp = pubSubCleanedUp;
                    }
                    else
                    {
                        // We are not deleting the destination. Just perform
                        // any other localisation cleanup
                        reallocationCanBeCalled = true;
                    }
                }

            }
        }

        // PK57432 We must not hold any locks when we reallocate.
        if (reallocationCanBeCalled)
        {
            boolean syncReallocRequiredCheck;
            synchronized (this) {
                syncReallocRequiredCheck = _isToBeReallocated;
                _isToBeReallocated = false;
            }
            if (syncReallocRequiredCheck)
                reallocateTransmissionStreams(null);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "cleanupLocalisations", Boolean.valueOf(allCleanedUp));

        return allCleanedUp;
    }

    /**
     * <p>This method will compare the passed in set of localising ME uuids with the
     * set already known about. Adding and removing transmission queues to those
     * localisatoinsones as necessary.
     * <p>
     * If there are any new localising ME's the infrastructure
     * to be able to send messages to them is created.
     * <p>
     * If the ME knows about some that are not in WCCM, they are marked
     * for deletion.
     * <p>
     * If they are still being advertised in WLM, nothing more is done
     * until they are removed from WLM as WLM can still return them as places
     * to send messages too.
     * <p>
     * If the entries are not in WLM, an attempt is made to rejig the messages.
     * This will move them to another localisation if possible, or will put
     * them to the exception destination or discard them.
     * <p>
     * If after the rejig, there are no messages left awaiting
     * transmission to the deleted localisation, the infrastructure for the
     * localistion is removed, otherwise it is left until the last message
     * has been processed.
     * <p>
     * 
     * @param newQueuePointLocalisingMEUuids The set of MEs on which the
     *            queue point is localised, possibly including the local ME.
     * 
     * 
     * 
     * @throws SIResourceException
     */
    public void updateLocalizationSet(
                                      Set newQueuePointLocalisingMEUuids)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "updateLocalizationSet",
                        new Object[] {
                        newQueuePointLocalisingMEUuids });

        //TODO the transactionality of this method has been compromised by using autocommit
        //this needs some thought

        SIBUuid8 messagingEngineUuid = messageProcessor.getMessagingEngineUuid();

        synchronized (this) //Ensure any cleanup initiated by this method does not start
        {
            //until this method has finished - synchronises with cleanupDestination
            //If the local queue point has been removed, act on this here
            if ((!newQueuePointLocalisingMEUuids.contains(messagingEngineUuid.toString())) &&
                _ptoPRealization != null)
            {
                _ptoPRealization.localQueuePointRemoved(isDeleted(),
                                                        _isSystem,
                                                        isTemporary(),
                                                        messagingEngineUuid);
            }

            // If this isn't PubSub we'll have remote Queue points
            if (!isPubSub())
            {
                _ptoPRealization.updateRemoteQueuePointSet(newQueuePointLocalisingMEUuids);
            }

        }

        // If the Queue points are not null, then synchronize (Queue destination)
        _protoRealization.updateLocalisationSet(messagingEngineUuid,
                                                newQueuePointLocalisingMEUuids);

        //Close any remote consumers that are attached to deleted queue points
        _protoRealization.
                        getRemoteSupport().
                        closeRemoteConsumers(newQueuePointLocalisingMEUuids,
                                             getLocalisationManager());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updateLocalizationSet");

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getPublishPoint()
     */
    @Override
    public PubSubMessageItemStream getPublishPoint()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getPublishPoint");
        }

        PubSubMessageItemStream pubSubMessageItemStream =
                        _pubSubRealization.getPublishPoint();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.exit(tc, "getPublishPoint", pubSubMessageItemStream);
        }
        return pubSubMessageItemStream;
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

        LocalizationPoint stream = null;

        if (_ptoPRealization != null)
        {
            stream = _ptoPRealization.getQueuePoint(meUuid);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getQueuePoint", stream);

        return stream;
    }

    /**
     * Return the itemstream representing a transmit queue to a remote ME
     * 
     * @param meUuid
     * @return
     */
    PtoPXmitMsgsItemStream getXmitQueuePoint(SIBUuid8 meUuid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getXmitQueuePoint", meUuid);

        PtoPXmitMsgsItemStream stream = getLocalisationManager().getXmitQueuePoint(meUuid);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getXmitQueuePoint", stream);

        return stream;
    }

    /**
     * Returns the guess set for queue points.
     * Unit tests only
     * 
     * @return
     */
    HashSet getQueuePointGuessSet()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getQueuePointGuessSet");

        HashSet theQueuePoints = _ptoPRealization.getQueuePointGuessSet();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getQueuePointGuessSet", theQueuePoints);

        return theQueuePoints;
    }

    /**
     * registers/deregisters the PRE_MEDIATED_PUT capability to TRM
     * 
     * @param advertise True if PRE_MEDIATED_PUT capability should be advertised
     *            to TRM. If this is the case, of the capability has not already been advertised,
     *            it will be advertised, otherwise it will not be. Set to FALSE if the capability
     *            should be deadvertised
     */
    public void updatePreRegistration(boolean advertise)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "updatePreRegistration", Boolean.valueOf(advertise));

        getLocalisationManager().
                        getTRMFacade().
                        updatePreRegistration(advertise);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updatePreRegistration");
    }

    /**
     * registers/deregisters the POST_MEDIATED_PUT capability to TRM
     * 
     * @param advertise True if POST_MEDIATED_PUT capability should be advertised
     *            to TRM. If this is the case, of the capability has not already been advertised,
     *            it will be advertised, otherwise it will not be. Set to FALSE if the capability
     *            should be deadvertised
     */
    public void updatePostRegistration(boolean advertise)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "updatePostRegistration", Boolean.valueOf(advertise));

        getLocalisationManager().
                        getTRMFacade().
                        updatePostRegistration(advertise);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updatePostRegistration");
    }

    /**
     * registers/deregisters the GET capability to TRM
     * 
     * @param advertise True if GET capability should be advertised
     *            to TRM. If this is the case, of the capability has not already been advertised,
     *            it will be advertised, otherwise it will not be. Set to FALSE if the capability
     *            should be deadvertised
     */
    public void updateGetRegistration(boolean advertise)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "updateGetRegistration", Boolean.valueOf(advertise));

        getLocalisationManager().
                        getTRMFacade().
                        updateGetRegistration(advertise);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updateGetRegistration");
    }

    /**
     * <p>This method updates the destinationLocalizationDefinition associated with the
     * destinationHandler (if the destination is localised on this ME)
     * and performs any necessary modifications to the
     * message store and other components to reflect the new state of the
     * destinationHandler.</p>
     * 
     * @param destinationLocalizationDefinition
     *            <p>Updates the DestinationLocalizationDefinition associated with the
     *            destination.</p>
     */
    protected void updateLocalizationDefinition(BaseLocalizationDefinition destinationLocalizationDefinition,
                                                TransactionCommon transaction)
                    throws
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "updateLocalizationDefinition", new Object[] { destinationLocalizationDefinition, transaction });

        //TODO close all consumer sessions?

        if (isPubSub())
            _pubSubRealization.updateLocalisationDefinition((LocalizationDefinition) destinationLocalizationDefinition);
        else
        {
            if (destinationLocalizationDefinition instanceof LocalizationDefinition)
            {
                //this is an update of the existing PM localization
                _ptoPRealization.updateLocalisationDefinition(destinationLocalizationDefinition, transaction);
            }
            else
            {
                SIResourceException e = new SIResourceException(new UnsupportedOperationException());
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "updateLocalizationDefinition", e);
                throw e;
            }
        }

        //update TRM if necessary
        getLocalisationManager().updateTrmAdvertisements();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "updateLocalizationDefinition");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isReconciled()
     */
    @Override
    public boolean isReconciled()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isReconciled");
            SibTr.exit(tc, "isReconciled", Boolean.valueOf(_reconciled));
        }

        return _reconciled;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener#messageEventOccurred(int, com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage,
     * com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    @Override
    public void messageEventOccurred(
                                     int event,
                                     SIMPMessage msg,
                                     TransactionCommon tran)
                    throws SIRollbackException,
                    SIConnectionLostException,
                    SIIncorrectCallException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "messageEventOccurred",
                        new Object[] { Integer.valueOf(event), msg, tran });

        if (event == MessageEvents.REFERENCES_DROPPED_TO_ZERO)
        {
            _pubSubRealization.itemReferencesDroppedToZero(msg);
        }
        else if (event == MessageEvents.EXPIRY_NOTIFICATION)
        {
            eventMessageExpiryNotification(msg, tran);
        }
        else if (event == MessageEvents.COD_CALLBACK) //183715.1
        {
            sendCODMessage(msg, tran);
        }
        else
        {
            final SIErrorException e =
                            new SIErrorException(
                                            nls.getFormattedMessage(
                                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                                    new Object[] {
                                                                                  "com.ibm.ws.sib.processor.impl.BaseDestinationHandler",
                                                                                  "1:3717:1.700.3.45" },
                                                                    null));

            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.BaseDestinationHandler.messageEventOccurred",
                                        "1:3723:1.700.3.45",
                                        this);

            SibTr.exception(tc, e);

            SibTr.error(
                        tc,
                        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.BaseDestinationHandler",
                                      "1:3733:1.700.3.45" });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "messageEventOccurred", e);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "messageEventOccurred");
    }

    /**
     * This is a callback required for expiry notification. For example,
     * we generate a report message here if expiry reports are requested.
     * 
     * @param msg The expired message
     * @param tran The transaction under which the message will be deleted
     * 
     *            Feature 179365.6
     */
    private void eventMessageExpiryNotification(
                                                SIMPMessage msg,
                                                TransactionCommon tran) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "eventMessageExpiryNotification",
                        new Object[] { msg, tran });

        // If a ReportHandler object has not been created yet, then do so.
        if (_reportHandler == null)
            _reportHandler = new ReportHandler(messageProcessor);

        // Generate and send the report under the same transaction as the delete
        try
        {
            _reportHandler.handleMessage(msg, tran, SIApiConstants.REPORT_EXPIRY);
        } catch (SIException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.BaseDestinationHandler.eventMessageExpiryNotification",
                                        "1:3778:1.700.3.45",
                                        this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "eventMessageExpiryNotification", "SIResourceException");
            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "REPORT_MESSAGE_ERROR_CWSIP0422",
                                                    new Object[] { messageProcessor.getMessagingEngineName(), e },
                                                    null),
                            e);
        }

        /**
         * 463584
         * If this message has been restored from msgstore but this destination has been recreated
         * in admin thus a different destination uuid and diferent BDH instance the stats object will be null.
         * It only gets initialized during the create/updateLocalisations call. This call will never happen
         * on this BDH.
         * Therefore... If a destination has been recreated, the stats on expiry will no longer be valid.
         */
        if (_protoRealization != null)
            _protoRealization.onExpiryReport();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventMessageExpiryNotification");
    }

    /**
     * Creates the pseudo destination name string for remote durable
     * subscriptions.
     * The case when this local ME is the DME.
     * 
     * @param subName the durable sub name
     * @return a string of the form "localME##subName"
     */
    public String constructPseudoDurableDestName(String subName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "constructPseudoDurableDestName", subName);
        String psuedoDestName = constructPseudoDurableDestName(
                                                               messageProcessor.getMessagingEngineUuid().toString(),
                                                               subName);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "constructPseudoDurableDestName", psuedoDestName);
        return psuedoDestName;
    }

    /**
     * Creates the pseudo destination name string for remote durable
     * subscriptions.
     * The case when the the DME is remote to this ME.
     * 
     * @param meUUID the durable home ME uuid
     * @param subName the durable sub name
     * @return a string of the form "localME##subName"
     */
    public String constructPseudoDurableDestName(String meUUID, String durableName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "constructPseudoDurableDestName", new Object[] { meUUID, durableName });
        String returnString = meUUID + "##" + durableName;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "constructPseudoDurableDestName", returnString);
        return returnString;
    }

    /**
     * Takes a pseudo destination name of the form
     * 'meUuid##subscriptionName'
     * and returns a String of the form 'subscriptionName'
     * 
     * @param pseudoDestinationName
     */
    public String getSubNameFromPseudoDestination(String pseudoDestinationName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getSubNameFromPseudoDestination", pseudoDestinationName);
        String strippedSubName =
                        pseudoDestinationName.substring(
                                        pseudoDestinationName.indexOf("##") + 2);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getSubNameFromPseudoDestination", strippedSubName);
        return strippedSubName;
    }

    /**
     * Durable subscriptions homed on this ME but attached to from remote MEs
     * have AnycastOutputHandlers mapped by their pseudo destination names.
     * 
     * @return The AnycastOutput for this pseudo destination
     */
    public AnycastOutputHandler getAnycastOHForPseudoDest(String destName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getAnycastOHForPseudoDest", destName);
        AnycastOutputHandler returnAOH =
                        _pubSubRealization.
                                        getRemotePubSubSupport().
                                        getAnycastOHForPseudoDest(destName);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getAnycastOHForPseudoDest", returnAOH);
        return returnAOH;

    }

    /**
     * Method sendCODMessage.
     * Initializes the reportHandler and sends a COD message if appropriate
     * 
     * @param msg
     * @param transaction
     */
    void sendCODMessage(SIMPMessage msg, TransactionCommon tran) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "sendCODMessage", new Object[] { msg, tran });

        // If COD Report messages are required, this is when we need to create and
        // send them.

        if (msg.getReportCOD() != null)
        {
            // Create the ReportHandler object if not already created
            if (_reportHandler == null)
                _reportHandler = new ReportHandler(messageProcessor);

            try
            {
                _reportHandler.handleMessage(msg, tran, SIApiConstants.REPORT_COD);
            } catch (SIException e)
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.BaseDestinationHandler.sendCODMessage",
                                            "1:3909:1.700.3.45",
                                            this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "sendCODMessage", "SIResourceException");
                throw new SIResourceException(
                                nls.getFormattedMessage(
                                                        "REPORT_MESSAGE_ERROR_CWSIP0423",
                                                        new Object[] { messageProcessor.getMessagingEngineName(), e },
                                                        null),
                                e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "sendCODMessage");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener#registerForEvents(com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage)
     */
    @Override
    public void registerForEvents(SIMPMessage msg)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "registerForEvents", msg);

        msg.registerMessageEventListener(MessageEvents.EXPIRY_NOTIFICATION, this);
        msg.registerMessageEventListener(
                                         MessageEvents.REFERENCES_DROPPED_TO_ZERO,
                                         this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerForEvents");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#isSystem()
     */
    @Override
    public boolean isSystem()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isSystem");
            SibTr.exit(tc, "isSystem", Boolean.valueOf(_isSystem));
        }

        return _isSystem;
    }

    @Override
    public boolean isTargetedAtLink()
    {
        // As this is a real destination handler (e.g. a Queue) it can't be
        // targeted at a foreign destination
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isTargetedAtLink");
            SibTr.exit(tc, "isTargetedAtLink", Boolean.valueOf(false));
        }

        return false;
    }

    /**
     * Called on completion of the transaction that adds this ItemStream
     * Feature 176658.3.2
     */
    public class DestinationAddTransactionCallback implements TransactionCallback
    {

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#beforeCompletion(com.ibm.ws.sib.msgstore.transactions.Transaction)
         */
        @Override
        public void beforeCompletion(TransactionCommon transaction)
        {
            // Nothing to do
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#afterCompletion(com.ibm.ws.sib.msgstore.transactions.Transaction, boolean)
         */
        @Override
        public void afterCompletion(TransactionCommon transaction, boolean committed)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(
                            tc,
                            "afterCompletion",
                            new Object[] { transaction, Boolean.valueOf(committed) });

            if (committed)
            {
                // Register the control adapters after the destination has been committed.
                registerControlAdapters();

                //Move the destination into ACTIVE state
                destinationManager.activateDestination(BaseDestinationHandler.this);

                /*
                 * For creation of a local destination,
                 * if this is a topic space, need to inform that it has been
                 * created so any proxy subscriptions that may have been created
                 * can be attached to this destination.
                 */
                // sanjay liberty change
//        if (isPubSub())
//        {
//          try
//          {
//            messageProcessor.getProxyHandler().topicSpaceCreatedEvent(
//              BaseDestinationHandler.this);
//          }
//          catch (SIResourceException e)
//          {
//            // Shouldn't occur as the destination now exists.
//            FFDCFilter.processException(
//              e,
//              "com.ibm.ws.sib.processor.impl.BaseDestinationHandler.DestinationAddTransactionCallback.afterCompletion",
//              "1:4023:1.700.3.45",
//              this);
//
//            SibTr.exception(tc, e);
//          }
//        }

                // If this is a local Queue, advertise in WLM
                if (_ptoPRealization != null)
                    _ptoPRealization.registerDestination(hasLocal(),
                                                         isDeleted());
            }
            else
            { // !committed

                /*
                 * Tell destination manager to remove this destination
                 * as the create was backed out.
                 */
                destinationManager.removeDestination(BaseDestinationHandler.this);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "afterCompletion");
        }
    }

    /**
     * Called on completion of the transaction that removes this ItemStream
     * Feature 176658.3.2
     */
    public class DestinationRemoveTransactionCallback
                    implements TransactionCallback
    {

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#beforeCompletion(com.ibm.ws.sib.msgstore.transactions.Transaction)
         */
        @Override
        public void beforeCompletion(TransactionCommon transaction)
        {
            // Nothing to do
        }

        /*
         * (non-Javadoc)
         * 
         * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#afterCompletion(com.ibm.ws.sib.msgstore.transactions.Transaction, boolean)
         */
        @Override
        public void afterCompletion(TransactionCommon transaction, boolean committed)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(
                            tc,
                            "afterCompletion",
                            new Object[] { transaction, Boolean.valueOf(committed) });

            deregisterDestination();

            notifyWaitingThread();

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "afterCompletion");
        }
    }

    /**
     * Method clearLocalisingUuidsSet.
     * Clear the set of ME's that localise the destination
     */
    protected void clearLocalisingUuidsSet()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "clearLocalisingUuidsSet");

        getLocalisationManager().clearLocalisingUuidsSet();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "clearLocalisingUuidsSet");
        return;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getSubscriptionList()
     */
    @Override
    public List getSubscriptionList()
                    throws
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getSubscriptionList");

        List subscriptions = _pubSubRealization.getSubscriptionList();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getSubscriptionList", subscriptions);
        return subscriptions;
    }

    /**
     * <p>Add all the live localisations to the set requiring clean-up</p>
     */
    protected void addAllLocalisationsForCleanUp()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "addAllLocalisationsForCleanUp");

        _protoRealization.addAllLocalisationsForCleanUp(_singleServer);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addAllLocalisationsForCleanUp");
    }

    /**
     * <p>Add a localisations to the set requiring clean-up</p>
     */
    public void addLocalisationForCleanUp(SIBUuid8 meUuid,
                                          PtoPMessageItemStream ptoPMessageItemStream)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc,
                        "addLocalisationForCleanUp",
                        new Object[] {
                                      meUuid,
                                      ptoPMessageItemStream });

        _protoRealization.addLocalisationForCleanUp(meUuid, ptoPMessageItemStream);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "addLocalisationForCleanUp");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#announceWasOpenForEBusiness()
     */
    @Override
    public void announceWasOpenForEBusiness()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "announceWasOpenForEBusiness");

        // do Nothing

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "announceWasOpenForEBusiness");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#announceWasOpenForEBusiness()
     */
    @Override
    public void announceWasClosedForEBusiness()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "announceWasClosedForEBusiness");

        // do Nothing

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "announceWasClosedForEBusiness");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#announceWasOpenForEBusiness()
     */
    @Override
    public void announceMPStarted()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "announceMPStarted");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "announceMPStarted");
    }

    /**
     * MP is stopping. All mediation activity should stop also.
     */
    @Override
    public void announceMPStopping()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "announceMPStopping");

        if (isPubSub()) {
            if (null != _pubSubRealization) { //doing a sanity check with checking for not null
                //signal to _pubSubRealization to gracefully exit from deleteMsgsWithNoReferences()
                _pubSubRealization.stopDeletingMsgsWihoutReferencesTask(true);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "announceMPStopping");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getTargetProtocolItemStream()
     */
    @Override
    public TargetProtocolItemStream getTargetProtocolItemStream()
    {
        return _protoRealization.getRemoteSupport().getTargetProtocolItemStream();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getProxyReferenceStream()
     */
    @Override
    public ProxyReferenceStream getProxyReferenceStream()
    {
        return _pubSubRealization.getProxyReferenceStream();
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

        SourceProtocolItemStream sourceProtocolItemStream =
                        _protoRealization.
                                        getRemoteSupport().
                                        getSourceProtocolItemStream();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getSourceProtocolItemStream", sourceProtocolItemStream);

        return sourceProtocolItemStream;
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

        registerForEvents(msg);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerForMessageEvents");
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
        {
            SibTr.entry(tc, "getResolvedDestinationHandler");
            SibTr.exit(tc, "getResolvedDestinationHandler", this);
        }
        return this;
    }

    /**
     * @return reallocationLockManager
     */
    @Override
    public LockManager getReallocationLockManager()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getReallocationLockManager");

        LockManager reallocationLockManager =
                        _protoRealization.
                                        getReallocationLockManager();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getReallocationLockManager", reallocationLockManager);

        return reallocationLockManager;
    }

    /**
     * The desination is being asked to start any dynamic things.
     */
    @Override
    public void start()
    {}

    /**
     * Stop anything that needs stopping, like mediations..etc.
     */
    @Override
    public void stop(int mode)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "stop", Integer.valueOf(mode));

        // Deregister the destination
        deregisterDestination();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "stop");
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

        int status = _pubSubRealization.deleteDurableFromRemote(subName);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteDurableFromRemote", Integer.valueOf(status));

        return status;
    }

    /**
     * Handle a remote request to create a local durable subscription.
     * 
     * @param subName The name of the durable subscription to create.
     * @param criteria This wraps the topic (aka discriminator) for the durable
     *            subscription and its selector.
     * @param user The name of the user associated with the durable subscription.
     * @param isSIBServerSubject Flag whether the user is the privileged SIBServerSubject
     * @return DurableConstants.STATUS_OK if the create works,
     *         DurableConstants.STATUS_SUB_ALREADY_EXISTS if there is already a
     *         subscription with the given name, or DurableConstants.STATUS_SUB_GENERAL_ERROR
     *         if an exception occurs while creating the subscription.
     */
    public int createDurableFromRemote(
                                       String subName,
                                       SelectionCriteria criteria,
                                       String user,
                                       boolean isCloned,
                                       boolean isNoLocal,
                                       boolean isSIBServerSubject)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "createDurableFromRemote",
                        new Object[] { subName, criteria, user, Boolean.valueOf(isSIBServerSubject) });
        int status = DurableConstants.STATUS_OK;

        // Create a ConsumerDispatcherState for the local call
        ConsumerDispatcherState subState =
                        new ConsumerDispatcherState(
                                        subName,
                                        definition.getUUID(),
                                        criteria,
                                        isNoLocal,
                                        messageProcessor.getMessagingEngineName(),
                                        definition.getName(),
                                        getBus());

        // Set the security id into the CD state
        subState.setUser(user, isSIBServerSubject);

        //defect 259036 - we need to enable sharing of the durable subscription
        subState.setIsCloned(isCloned);

        try
        {
            _pubSubRealization.createLocalDurableSubscription(subState, null);
        } catch (SIDurableSubscriptionAlreadyExistsException e)
        {
            // No FFDC code needed
            status = DurableConstants.STATUS_SUB_ALREADY_EXISTS;
        } catch (Throwable e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.BaseDestinationHandler.createDurableFromRemote",
                                        "1:4436:1.700.3.45",
                                        this);

            // FFDC keeps the local broker happy, but we still need to return
            // a status to keep the remote ME live.
            status = DurableConstants.STATUS_SUB_GENERAL_ERROR;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createDurableFromRemote", Integer.valueOf(status));

        return status;
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

        // Call through to state handler
        _pubSubRealization.attachDurableFromRemote(request);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "attachDurableFromRemote");
    }

    @Override
    public void createControlAdapter()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createControlAdapter");

        if (isPubSub())
            controlAdapter = new Topicspace(messageProcessor, this);
        else
            controlAdapter = new Queue(messageProcessor, this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createControlAdapter");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#registerControlAdapterAsMBean()
     */
    @Override
    public void registerControlAdapterAsMBean()
    {}

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#deregisterControlAdapterMBean()
     */
    @Override
    public void deregisterControlAdapterMBean()
    {}

    /**
     * Attaches to a created DurableSubscription
     * 
     * Checks that there are no active subscriptions (unless it supports cloning)
     * 
     * Checks that the durable subscription exists :
     * 
     * @param consumerPoint
     */
    @Override
    public ConsumableKey attachToDurableSubscription(
                                                     LocalConsumerPoint consumerPoint,
                                                     ConsumerDispatcherState subState)
                    throws SIDurableSubscriptionMismatchException, SIDurableSubscriptionNotFoundException, SIDestinationLockedException, SISelectorSyntaxException, SIDiscriminatorSyntaxException, SINotPossibleInCurrentConfigurationException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "attachToDurableSubscription",
                        new Object[] { consumerPoint, subState });

        ConsumableKey result =
                        _pubSubRealization.attachToDurableSubscription(
                                                                       consumerPoint,
                                                                       subState);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "attachToDurableSubscription", result);

        return result;
    }

    /**
     * Creates a Durable subscription.
     * 
     * Creates a ConsumerDispatcher associated with this subscription and
     * creates the DurableSubscriptionItemStream which is used to receive the messages.
     * 
     * If no transaction is supplied, a transaction is created and the itemstreams are
     * created using this.
     * 
     * The ConsumerDispatcher isn't added to the MatchSpace until the eventPostCommit add
     * call.
     * 
     */
    @Override
    public void createDurableSubscription(
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
                        "createDurableSubscription",
                        new Object[] { subState, transaction });

        // ASSERT: subState.getDurableHome().equals(messageProcessor.getMessagingEngineName()
        _pubSubRealization.createLocalDurableSubscription(subState,
                                                          transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createDurableSubscription");
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
                    throws
                    SIDestinationLockedException,
                    SIDurableSubscriptionNotFoundException,
                    SIResourceException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "deleteDurableSubscription",
                        new Object[] { subscriptionId, durableHome });

        // ASSERT: subState.getDurableHome().equals(messageProcessor.getMessagingEngineName()
        _pubSubRealization.deleteLocalDurableSubscription(subscriptionId);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteDurableSubscription");
    }

    protected void setReconciled()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setReconciled");

        _reconciled = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setReconciled");
    }

    public LocalizationPoint getLocalLocalizationPoint()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getLocalLocalizationPoint");

        LocalizationPoint localizationPoint = null;

        localizationPoint = _ptoPRealization.getPtoPLocalLocalizationPoint();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getLocalLocalizationPoint", localizationPoint);

        return localizationPoint;
    }

    public Index getRemoteQueuePoints()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getRemoteQueuePoints");
            SibTr.exit(tc, "getRemoteQueuePoints", _remoteQueuePoints);
        }

        return _remoteQueuePoints;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getForwardRoutingPath()
     */
    @Override
    public List<SIDestinationAddress> getForwardRoutingPath()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getForwardRoutingPath");
            SibTr.exit(tc, "getForwardRoutingPath", _forwardRoutingPath);
        }
        return _forwardRoutingPath;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getDefaultForwardRoutingPath()
     */
    @Override
    public SIDestinationAddress[] getDefaultForwardRoutingPath()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDefaultForwardRoutingPath");
        // Convert array of names to list of jsDestinationAddresses
        QualifiedDestinationName[] names = definition.getForwardRoutingPath();
        SIDestinationAddress[] frp = null;

        if (names != null)
        {
            frp = new SIDestinationAddress[names.length];
            for (int name = 0; name < names.length; name++)
            {
                frp[name] = SIMPUtils.createJsDestinationAddress(
                                                                 names[name].getDestination(),
                                                                 null,
                                                                 names[name].getBus());
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDefaultForwardRoutingPath", frp);
        return frp;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getReplyDestination()
     */
    @Override
    public JsDestinationAddress getReplyDestination()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getReplyDestination");

        if (!_replySet)
        {
            // Create a JsDestinationAddress from the qualifiedName
            QualifiedDestinationName name = definition.getReplyDestination();

            if (name != null)
                _replyDestination = SIMPUtils.createJsDestinationAddress(
                                                                         name.getDestination(),
                                                                         messageProcessor.getMessagingEngineUuid(),
                                                                         name.getBus());

            _replySet = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getReplyDestination", _replyDestination);

        return _replyDestination;
    }

    /**
     * Being a 'real' destination, there is no implicit need to add a routing destination
     * address to any messages sent to this destination. However, if the sender is bound
     * to a single message point then we need to set a routing destination so that a particular
     * ME Uuid can be set into it.
     * Another reason for setting a routing address is if we're sending to a remote system or
     * temporary queue
     */
    @Override
    public JsDestinationAddress getRoutingDestinationAddr(JsDestinationAddress inAddress,
                                                          boolean fixedMessagePoint)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getRoutingDestinationAddr", new Object[] { this,
                                                                       inAddress,
                                                                       Boolean.valueOf(fixedMessagePoint) });

        JsDestinationAddress routingAddress = null;

        // Pull out any encoded ME from a system or temporary queue
        SIBUuid8 encodedME = null;
        if (_isSystem || _isTemporary)
            encodedME = SIMPUtils.parseME(inAddress.getDestinationName());

        // If this is a system or temporary queue that is located on a different ME to us
        // we need to set the actual address as the routing destination.
        if ((encodedME != null) && !(encodedME.equals(getMessageProcessor().getMessagingEngineUuid())))
        {
            routingAddress = inAddress;
        }
        // The only other reason for setting a routing destination is if the sender is bound
        // (or will be bound) to a single message point. In which case we use the routing
        // address to transmit the chosen ME (added later by the caller)
        else if (fixedMessagePoint)
        {
            //TODO what if inAddres is isLocalOnly() or already has an ME?

            routingAddress = _destinationAddr;
        }
        else if ((inAddress != null) && (inAddress.getME() != null))
        {
            routingAddress = _destinationAddr;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getRoutingDestinationAddr", routingAddress);

        return routingAddress;
    }

    /**
     * Check permission to access a Destination
     * 
     * @param secContext
     * @param operation
     * @return
     */
    @Override
    public boolean checkDestinationAccess(
                                          SecurityContext secContext,
                                          OperationType operation)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "checkDestinationAccess",
                        new Object[] { secContext, operation });

        boolean allow = false;

        if (accessChecker.checkDestinationAccess(secContext,
                                                 getBus(),
                                                 getDefinition().getName(),
                                                 operation))
        {
            allow = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkDestinationAccess", Boolean.valueOf(allow));

        return allow;
    }

    /**
     * Check permission to access a Discriminator
     * 
     * @param secContext
     * @param operation
     * @return
     */
    @Override
    public boolean checkDiscriminatorAccess(
                                            SecurityContext secContext,
                                            OperationType operation) throws SIDiscriminatorSyntaxException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "checkDiscriminatorAccess",
                        new Object[] { secContext, operation });

        boolean allow = true;

        if (isTopicAccessCheckRequired())
        {
            if (!accessChecker.checkDiscriminatorAccess(secContext,
                                                        this,
                                                        secContext.getDiscriminator(),
                                                        operation))
            {
                allow = false;
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkDiscriminatorAccess", Boolean.valueOf(allow));

        return allow;
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
            SibTr.entry(
                        tc,
                        "deleteRemoteDurableRME",
                        new Object[] { subState });

        _pubSubRealization.
                        deleteRemoteDurableRME(subState);

        // All done, return
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteRemoteDurableRME");
    }

    /**
     * Clean up the local AnycastOutputHandler that was created to handle
     * access to a locally homed durable subscription. This method should
     * only be invoked as part of ConsumerDispatcher.deleteConsumerDispatcher.
     * 
     * @param subName Name of the local durable subscription being removed.
     */
    public void deleteRemoteDurableDME(String subName)
                    throws SIRollbackException,
                    SIConnectionLostException,
                    SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "deleteRemoteDurableDME",
                        new Object[] { subName });

        _pubSubRealization.
                        getRemotePubSubSupport().
                        deleteRemoteDurableDME(subName);

        // All done, return
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteRemoteDurableDME");
    }

    /**
     * Request reallocation of transmitQs on the next
     * asynch deletion thread run
     */
    public void requestReallocation()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "requestReallocation");

        if (!isCorruptOrIndoubt()) { //PK73754

            // Reset reallocation flag under lock on the BDH.
            synchronized (this)
            {
                _isToBeReallocated = true;
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "requestReallocation", "Have set reallocation flag");
            }

            // Set cleanup_pending state
            destinationManager.getDestinationIndex().cleanup(this);

            destinationManager.startAsynchDeletion();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "requestReallocation");
    }

    /*
   *
   */
    private void reallocateTransmissionStreams(PtoPXmitMsgsItemStream ignoredStream)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "reallocateTransmissionStreams", ignoredStream);

        getLocalisationManager().reallocateTransmissionStreams(ignoredStream);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "reallocateTransmissionStreams");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#registerControlAdapters()
     */
    @Override
    public void registerControlAdapters()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "registerControlAdapters");

        //register the local queue point/publication point as appropriate
        _protoRealization.registerControlAdapters();

        // If _remoteQueuePoints not null indicates Queue destination
        SIMPIterator itr = null;
        if (_remoteQueuePoints != null)
        {
            //register the xmit queue points
            itr = _remoteQueuePoints.iterator();
            while (itr.hasNext())
            {
                ControlAdapter remote = (ControlAdapter) itr.next();
                remote.registerControlAdapterAsMBean();
            }
            itr.finished();
        }

        if (isLink())
        {
            //register the xmit queue points
            Iterator it = _localisationManager.getXmitQueueIterator();
            while (it.hasNext())
            {
                ControlAdapter remote = ((PtoPXmitMsgsItemStream) it.next()).getControlAdapter();
                remote.registerControlAdapterAsMBean();
            }
        }

        //We're not registering the BDH's control handler at this time
        // But we register the link as a SIBLink MBean
        if (isLink())
            getControlAdapter().registerControlAdapterAsMBean();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerControlAdapters");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#chooseConsumerDispatcher()
     */
    @Override
    public ConsumerManager chooseConsumerManager(SIBUuid12 gatheringTargetUuid,
                                                 SIBUuid8 fixedMEUuid,
                                                 HashSet<SIBUuid8> scopedMEs)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "chooseConsumerManager", new Object[] { gatheringTargetUuid, fixedMEUuid, scopedMEs });

        ConsumerManager consumerManager = null;

        consumerManager = _ptoPRealization.chooseConsumerManager(definition,
                                                                 isReceiveAllowed(),
                                                                 gatheringTargetUuid,
                                                                 fixedMEUuid,
                                                                 scopedMEs);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "chooseConsumerManager", consumerManager);

        return consumerManager;
    }

    public SIMPRemoteQueuePointControllable getRemoteQueuePointControl(SIBUuid8 remoteME, boolean create)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getRemoteQueuePointControl", new Object[] { remoteME, Boolean.valueOf(create) });

        SIMPRemoteQueuePointControllable rqp = null;

        synchronized (_remoteQueuePoints)
        {
            rqp = (SIMPRemoteQueuePointControllable) _remoteQueuePoints.get(remoteME);
            if (create && rqp == null)
            {
                rqp = new RemoteQueuePoint(remoteME, this, getMessageProcessor());
                _remoteQueuePoints.put(remoteME, rqp);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getRemoteQueuePointControl", rqp);

        return rqp;
    }

    @Override
    public AnycastInputHandler getAnycastInputHandler(SIBUuid8 dmeId, SIBUuid12 gatheringTargetDestUuid, boolean createAIH)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getAnycastInputHandler", new Object[] { dmeId, gatheringTargetDestUuid, Boolean.valueOf(createAIH) });

        AnycastInputHandler aih = _protoRealization.
                        getRemoteSupport().
                        getAnycastInputHandler(dmeId,
                                               gatheringTargetDestUuid,
                                               definition,
                                               createAIH);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getAnycastInputHandler", aih);

        return aih;
    }

    /** Override Method in AbstractBaseDestinationHandler */
    @Override
    public boolean isTopicAccessCheckRequired()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isTopicAccessCheckRequired");

        if (!isPubSub() || isTemporary())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "isTopicAccessCheckRequired", Boolean.FALSE);
            return false;
        }

        boolean check = super.isTopicAccessCheckRequired();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isTopicAccessCheckRequired", Boolean.valueOf(check));
        return check;
        // Look to the underlying definition
    }

    public Map getPseudoDurableAIHMap()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getPseudoDurableAIHMap");
        }
        Map pseudoDurableAIHMap = _pubSubRealization.
                        getRemotePubSubSupport().
                        getPseudoDurableAIHMap();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.exit(tc, "getPseudoDurableAIHMap", pseudoDurableAIHMap);
        }
        return pseudoDurableAIHMap;
    }

    /**
     * Removes the AIH and the RCD instances for a given dme ID. Also removes the itemStreams
     * from the messageStore for the aiContainerItemStream and the rcdItemStream
     * 
     * @param dmeID the uuid of the dme which the instances of the aih and rcd
     *            will be deleted.
     * @throws SIResourceException if there was a problem with removing the itemStreams
     *             from the messageStore
     * @return boolean as to whether the itemstreams were removed or not
     */
    public boolean removeAnycastInputHandlerAndRCD(String key) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeAnycastInputHandlerAndRCD", key);

        boolean removed = _protoRealization.
                        getRemoteSupport().
                        removeAnycastInputHandlerAndRCD(key);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeAnycastInputHandlerAndRCD", Boolean.valueOf(removed));

        return removed;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#closeConsumers()
     */
    @Override
    public void closeConsumers() throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "closeConsumers");

        // This method is called when the local queue point is being deleted
        // and this would close remote consumers as well, which we would only have
        // if this destination was being used for gathering.

        // The super class will miss local access to remote durable subscription
        // because there will be no corresponding local subscription entry.
        // So we find them and delete them here.
        _protoRealization.
                        getRemoteSupport().
                        closeConsumers();

        // Once we've handled remote durable, let the superclass have
        // a crack.
        super.closeConsumers();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "closeConsumers");

        return;
    }

    /**
     * Close the remote consumers for a given remote ME
     * 
     * @param remoteMEUuid
     * @throws SIResourceException
     */
    public void closeRemoteConsumer(SIBUuid8 dmeUuid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "closeRemoteConsumer", dmeUuid);

        _protoRealization.
                        getRemoteSupport().
                        closeRemoteConsumers(dmeUuid);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "closeRemoteConsumer");

        return;
    }

    /**
     * Assure that all non-anycast (i.e. PtoP and PubSub) streams
     * are in the flushed state.
     * 
     * For a source node, a stream is flushable if there are no in-doubt
     * messages. In this case, we'll flush the stream and return "true".
     * Otherwise, we'll mark the fact that we're waiting for the stream
     * to be flushed and we'll return false. When the stream finally
     * becomes flushable, the delete code is redriven, this method will
     * be called again and will return true, allowing the delete to complete.
     * 
     * For a target node, a stream is flushable only upon receiving a
     * "flushed" message from the source. If the stream doesn't exist,
     * then by default it is flushed and we return "true". Otherwise, we
     * send "are you flushed" and start an alarm to resend the message
     * until one of two events occur:
     * 
     * 1) We received flushed for the stream. If the stream set is now
     * flushable, then we remember this fact and redrive the delete
     * code. When the delete code calls assureAllFlushed we
     * return true allowing the delete to complete.
     * 
     * 2) We determine the source is no longer defined. In this case
     * we simply dump the streams and redrive the delete code. When the
     * delete code calls assureAllFlushed we return true allowing the
     * delete to complete.
     * 
     * Note that in the case where the target becomes undefined (and the
     * source is waiting to flush), the regular protocol code (i.e.
     * ackExpected resender) will detect this fact and quiesce the protocol.
     * 
     * @return true if all local streams have been flushed, and false
     *         otherwise.
     */
    protected boolean assureAllFlushed()
                    throws SIResourceException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "assureAllFlushed");

        boolean done = true;

        if (isPubSub())
        {
            // Tell pubsub to clean everything up.  Clean outgoing first.
            // Note that we attempt to drive both sides of the protocol
            // even though one side may already have told us the delete needs
            // to be deffered.
            PubSubInputHandler pHandler = (PubSubInputHandler) inputHandler;
            boolean resultOne = pHandler.flushAllForDeleteSource();
            boolean resultTwo = pHandler.flushAllForDeleteTarget();
            done = resultOne && resultTwo;
        }
        else
        {
            // If we've originated streams for PtoP, then make sure they're
            // all flushed before we continue with the delete.
            // We only perform this if the destination is not MQ
            if (getSourceProtocolItemStream() != null)
            {
                // The output handler drives the flush, so we need to track
                // it down first, then call the flush.  The assumption here
                // is that since we're in the middle of a delete nobody
                // can be doing nasty things like adding new queue points.
                done = _ptoPRealization.flushQueuePointOutputHandler();

            } //if (sourceProtocolItemStream != null)

            // Likewise, if we're a target then make sure we flushed all target
            // streams before allowing the delete to continue.
            // We only perform this if the destination is not MQ
            if (getTargetProtocolItemStream() != null)
            {
                done &= ((PtoPInputHandler) inputHandler).flushAllForDelete();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "assureAllFlushed", Boolean.valueOf(done));

        return done;
    }

    /**
     * Fire an event notification of type TYPE_SIB_MESSAGEPOINT_SEND_ALLOWED_STATE
     * 
     * @param newState
     */
    private void fireSendAllowedStateChangeEvent(boolean newState)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "fireSendAllowedStateChangeEvent", Boolean.valueOf(newState));

        MessageProcessor messageProcessor = getMessageProcessor();

        if (messageProcessor.getMessagingEngine().isEventNotificationEnabled())
        {
            // Build the message for the Notification

            String message =
                            nls.getFormattedMessage("NOTIFY_SEND_ALLOWED_STATE_CHANGE_CWSIP0551",
                                                    new Object[] { getName(),
                                                                  getUuid().toString(),
                                                                  Boolean.valueOf(newState) },
                                                    null);

            // Build the properties for the Notification
            Properties props = new Properties();

            props.put(SibNotificationConstants.KEY_DESTINATION_NAME, getName());
            props.put(SibNotificationConstants.KEY_DESTINATION_UUID, getUuid().toString());

            if (newState)
                props.put(SibNotificationConstants.KEY_SEND_ALLOWED_STATE,
                          SibNotificationConstants.SEND_ALLOWED_TRUE);
            else
                props.put(SibNotificationConstants.KEY_SEND_ALLOWED_STATE,
                          SibNotificationConstants.SEND_ALLOWED_FALSE);

            // Now create the Event object to pass to the control adapter
            MPRuntimeEvent MPevent =
                            new MPRuntimeEvent(SibNotificationConstants.TYPE_SIB_MESSAGEPOINT_SEND_ALLOWED_STATE,
                                            message,
                                            props);
            // Fire the event
            runtimeEventOccurred(MPevent);
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "fireSendAllowedStateChangeEvent", "Event Notification is disabled, cannot fire event");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "fireSendAllowedStateChangeEvent");
    }

    /**
     * Fire an event notification of type TYPE_SIB_MESSAGEPOINT_SEND_ALLOWED_STATE
     * 
     * @param newState
     */
    private void fireReceiveAllowedStateChangeEvent(boolean newState)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "fireReceiveAllowedStateChangeEvent", Boolean.valueOf(newState));

        MessageProcessor messageProcessor = getMessageProcessor();

        if (messageProcessor.getMessagingEngine().isEventNotificationEnabled())
        {
            // Build the message for the Notification

            String message =
                            nls.getFormattedMessage("NOTIFY_RECEIVE_ALLOWED_STATE_CHANGE_CWSIP0552",
                                                    new Object[] { getName(),
                                                                  getUuid().toString(),
                                                                  Boolean.valueOf(newState) },
                                                    null);

            // Build the properties for the Notification
            Properties props = new Properties();

            props.put(SibNotificationConstants.KEY_DESTINATION_NAME, getName());
            props.put(SibNotificationConstants.KEY_DESTINATION_UUID, getUuid().toString());

            if (newState)
                props.put(SibNotificationConstants.KEY_RECEIVE_ALLOWED_STATE,
                          SibNotificationConstants.RECEIVE_ALLOWED_TRUE);
            else
                props.put(SibNotificationConstants.KEY_RECEIVE_ALLOWED_STATE,
                          SibNotificationConstants.RECEIVE_ALLOWED_FALSE);

            // Now create the Event object to pass to the control adapter
            MPRuntimeEvent MPevent =
                            new MPRuntimeEvent(SibNotificationConstants.TYPE_SIB_MESSAGEPOINT_RECEIVE_ALLOWED_STATE,
                                            message,
                                            props);
            // Fire the event
            runtimeEventOccurred(MPevent);
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "fireReceiveAllowedStateChangeEvent", "Event Notification is disabled, cannot fire event");
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "fireReceiveAllowedStateChangeEvent");
    }

    /**
     * @param pevent
     */
    private void runtimeEventOccurred(MPRuntimeEvent event)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "runtimeEventOccurred", event);

        //Fire the event through the the local queue or publication point
        if (isPubSub())
        {
            _pubSubRealization.runtimeEventOccurred(event);
        }
        else
        {
            _ptoPRealization.runtimeEventOccurred(event);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "runtimeEventOccurred");
    }

    /**
     * Retrieve the PubSubRealization
     * 
     * @return
     */
    public PubSubRealization getPubSubRealization()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getPubSubRealization");
            SibTr.exit(tc, "getPubSubRealization", _pubSubRealization);
        }
        return _pubSubRealization;
    }

    /**
     * Retrieve the PtoPRealization
     * 
     * @return
     */
    public PtoPRealization getPtoPRealization()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getPtoPRealization");
            SibTr.exit(tc, "getPtoPRealization", _ptoPRealization);
        }
        return _ptoPRealization;
    }

    /**
     * Retrieve the ProtocolRealization
     * 
     * @return
     */
    public AbstractProtoRealization getProtocolRealization()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getProtocolRealization");
            SibTr.exit(tc, "getProtocolRealization", _protoRealization);
        }
        return _protoRealization;
    }

    /**
     * Retrieve the LocalisationManager
     * 
     * @return _localisationManager
     */
    public LocalisationManager getLocalisationManager()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getLocalisationManager", this);
        // Instantiate LocalisationManager to manage localisations and interface to WLM

        if (_localisationManager == null)
        {
            _localisationManager =
                            new LocalisationManager(this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getLocalisationManager", _localisationManager);

        return _localisationManager;
    }

    @Override
    public boolean isOrdered()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isOrdered");
            SibTr.exit(tc, "isOrdered", Boolean.valueOf(_isOrderingRequired));
        }

        return _isOrderingRequired;
    }

    /**
     * @param isOrderingRequired
     */
    public void setIsOrdered(boolean isOrderingRequired) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setIsOrdered", Boolean.valueOf(isOrderingRequired));

        // PK69943 It may be the case that we are trying to enable strict message ordering
        // on a destination, but the destination was found to have multiple in-doubt
        // transactions on it earlier during startup.
        // The best we can do here is to log an error message to inform the user that
        // strict message ordering will be broken for a destination.
        if (isOrderingRequired && _isUnableToOrder)
        {
            SibTr.error(tc, "ORDERING_INITIALISATION_ERROR_CWSIP0671",
                        new Object[] { getName(), messageProcessor.getMessagingEngineName() });
        }

        // PK69943 does not make a functional change - we still leave _isOrderingRequired
        // set to true when it's broken due to multiple indoubt txns.
        _isOrderingRequired = isOrderingRequired;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setIsOrdered");
    }

    /**
     * @param isUnableToOrder
     */
    public void setIsUnableToOrder(boolean isUnableToOrder) {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setIsUnableToOrder", new Boolean(isUnableToOrder));

        _isUnableToOrder = isUnableToOrder;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setIsUnableToOrder");
    }

    public final Object getReadyConsumerPointLock() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getReadyConsumerPointLock");
            SibTr.exit(tc, "getReadyConsumerPointLock", readyConsumerPointLock);
        }

        return readyConsumerPointLock;
    }

    @Override
    public Iterator<AnycastInputControl> getAIControlAdapterIterator()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getAIControlAdapterIterator");

        Iterator<AnycastInputControl> it =
                        _protoRealization.getRemoteSupport().getAIControlAdapterIterator();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getAIControlAdapterIterator", it);
        return it;
    }

    @Override
    public Iterator<ControlAdapter> getAOControlAdapterIterator()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getAOControlAdapterIterator");

        Iterator<ControlAdapter> it =
                        _protoRealization.getRemoteSupport().getAOControlAdapterIterator();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getAOControlAdapterIterator", it);
        return it;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.AbstractItem#xmlWriteOn(com.ibm.ws.sib.msgstore.FormattedWriter)
     */
    @Override
    public void xmlWriteOn(FormattedWriter writer) throws IOException
    {
        super.xmlWriteOn(writer);
        writer.newLine();
        writer.taggedValue("toBeDeleted", _toBeDeleted);
        writer.newLine();
        writer.taggedValue("toBeIgnored", _toBeIgnored);
    }

    /**
     * (non-Javadoc)
     * Method to get the high message threshold
     * 
     * @return the high message threshold set for the queue
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler#getQHighMsgDepth()
     */
    //117505
    @Override
    public long getQHighMsgDepth() {
        long qHighMsgLimit = _ptoPRealization.getQhighMsgLimit();
        return qHighMsgLimit;
    }

}
