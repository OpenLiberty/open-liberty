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

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.ejs.util.am.AlarmListener;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.SIApiConstants;
import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SINotPossibleInCurrentConfigurationException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageCopyFailedException;
import com.ibm.ws.sib.mfp.ProtocolType;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.InvalidAddOperation;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.NotInMessageStore;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.ReferenceStream;
import com.ibm.ws.sib.msgstore.RollbackException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.MPSubscription;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.UndeliverableReturnCode;
import com.ibm.ws.sib.processor.exceptions.SIMPMessageNotLockedException;
import com.ibm.ws.sib.processor.impl.interfaces.BrowseCursor;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerKey;
import com.ibm.ws.sib.processor.impl.interfaces.ConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.ControllableSubscription;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.DispatchableConsumerPoint;
import com.ibm.ws.sib.processor.impl.interfaces.DispatchableKey;
import com.ibm.ws.sib.processor.impl.interfaces.InputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.InputHandlerStore;
import com.ibm.ws.sib.processor.impl.interfaces.JSConsumerKey;
import com.ibm.ws.sib.processor.impl.interfaces.JSConsumerManager;
import com.ibm.ws.sib.processor.impl.interfaces.JSKeyGroup;
import com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener;
import com.ibm.ws.sib.processor.impl.interfaces.OutputHandler;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.MessageEvents;
import com.ibm.ws.sib.processor.impl.store.filters.MessageSelectorFilter;
import com.ibm.ws.sib.processor.impl.store.items.AOValue;
import com.ibm.ws.sib.processor.impl.store.items.MessageItem;
import com.ibm.ws.sib.processor.impl.store.items.MessageItemReference;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream;
import com.ibm.ws.sib.processor.matching.MatchingConsumerPoint;
import com.ibm.ws.sib.processor.matching.MessageProcessorMatchTarget;
import com.ibm.ws.sib.processor.matching.MessageProcessorSearchResults;
import com.ibm.ws.sib.processor.runtime.SIMPTopicSpaceControllable;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.runtime.impl.LocalSubscriptionControl;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.linkedlist.SimpleEntry;
import com.ibm.ws.sib.processor.utils.linkedlist.SimpleLinkedList;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.transactions.TransactionCallback;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.SelectionCriteria;
import com.ibm.wsspi.sib.core.exception.SIConnectionLostException;
import com.ibm.wsspi.sib.core.exception.SIDestinationLockedException;
import com.ibm.wsspi.sib.core.exception.SIDiscriminatorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SIDurableSubscriptionNotFoundException;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;
import com.ibm.wsspi.sib.core.exception.SISelectorSyntaxException;
import com.ibm.wsspi.sib.core.exception.SISessionDroppedException;
import com.ibm.wsspi.sib.core.exception.SITemporaryDestinationNotFoundException;

/**
 * A ConsumerDispatcher
 * 
 * TODO see what other methods can percolate up to AbstractConsumerManager
 * 
 * @author tevans
 */
public class ConsumerDispatcher
                extends AbstractConsumerManager
                implements OutputHandler, MessageEventListener, ControllableSubscription, JSConsumerManager, TransactionCallback
{

    //Trace
    private static final TraceComponent tc =
                    SibTr.register(
                                   ConsumerDispatcher.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    // NLS for component
    private static final TraceNLS nls =
                    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

    /**
     * If the destination is pub-sub then this ConsumerDispatcher represents a subscription.
     * The original messages are held in the topicspace. The subscription passed
     * in is a reference stream and holds references to those messages.
     */
    private SubscriptionItemStream subscriptionItemStream = null;
    /**
     * If the destination is pt-pt then this ConsumerDispatcher represents the localization
     * of a queue. A simple, non-partitioned queue, has just one item stream, shared with
     * the InputHandler.
     */
    protected PtoPMessageItemStream itemStream = null;
    /** The complete list of attached consumerPoints */
    protected LinkedList<DispatchableKey> consumerPoints;
    /** Used as a LILO stack of the ready consumerPoints which do not have selectors */
    protected SimpleLinkedList nonSpecificReadyCPs;
    /** Indicates if this ConsumerDispatcher is being deleted */
    /** Used as a LILO stack of the ready forward scanning consumerPoints which do not have selectors */
    private final SimpleLinkedList readyFwdScanningCPs;

    /** Current Receive Allowed */
    private boolean currentReceiveAllowed;

    /**
     * A count of the number of ready consumers which have non-null/non-empty
     * selectors (specific ready consumers)
     */
    protected long specificReadyConsumerCount = 0;
    /**
     * A version number for the set of specific consumers, incremented
     * whenever a specific consumer is attached
     */
    protected long specificConsumerVersion = 0;
    /**
     * A version number for the set of all ready consumers, incremented
     * whenever any consumer is added or removed from the ready consumer stack
     */
    protected long readyConsumerVersion = 0;

    /**
     * dispatcherState - Object representing the state of this consumer dispatcher. This
     * may not neccesarily be a subscription, but may contain the selector for a queue destination.
     */
    protected ConsumerDispatcherState dispatcherState;

    /**
     * mpSubscription - Object allowing easy access to the state of this consumer dispatcher.
     * This will always be a durable subscription.
     */
    protected MPSubscription mpSubscription = null;

    /**
     * currentTransaction - For ordered messaging. The current working transaction.
     * no other transactions can be used until this one completes.
     */
    protected PersistentTranId currentTran;

    /**
     * orderedLME - For ordered messaging. The locked message enumeration
     * associated with the currently transacted consumer.
     */
    protected JSLockedMessageEnumeration currentLME;

    /**
     * Lock object for ordering
     */
    protected Object orderLock = new Object();

    /**
     * transactionMap - for "ignoreInitialIndoubts" consumers. We keep
     * a reference to every tran that the consumer makes use of to
     * receive.
     */
    protected Set transactionSet = null;

    /**
     * For messaging ordering. Set to true until any active tran is complete at startup
     */
    protected boolean streamHasInDoubtRemoves = true;

    /** The state of the ConsumerDispatcher :- READY_FOR_USE, LOCKED or DELETED */
    protected SIMPState state;

    /**
     * A seed to use in selection of a specific consumer when messages are
     * being produced on short lived sessions (protected by readyConsumerPointLock of DH)
     */
    private int shortLivedProducerSeed = 0;

    /** The Report Handler object to generate and send report COA messages */
    private ReportHandler reportHandler = null;

    private boolean isGuess = false;

    /** If the destination is pub sub */
    private boolean _isPubSub;

    private final SIBUuid12 subscriptionUuid = new SIBUuid12();

    /** Indicates that a subscription has been added to the Matchspace */
    private boolean isInMatchSpace = false;

    private ControlAdapter subscriptionControlAdaptor;

    /** The thread that notifies this consumer dispatcher that the receive allowed state has changed */
    private ReceiveAllowedThread _receiveAllowedThread = null;

    /**
     * Creates a new pub-sub ConsumerDispatcher
     * 
     * @param destination The owning Destination
     * @param subscription The Reference Stream to be used
     * @param dispatcherState The initial state of this subscription
     */
    public ConsumerDispatcher(
                              BaseDestinationHandler destination,
                              SubscriptionItemStream subscriptionItemStream,
                              ConsumerDispatcherState dispatcherState)
    {
        //do the main work of creating ConsumerDispatcher
        this(destination, dispatcherState);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "ConsumerDispatcher",
                        new Object[] { destination, subscriptionItemStream, dispatcherState });

        subscriptionItemStream.setConsumerDispatcher(this);

        //store the reference stream
        this.subscriptionItemStream = subscriptionItemStream;

        _isPubSub = destination.isPubSub();

        if (dispatcherState != null)
        {
            // F001333-14610
            // Generate a subscriber id if we are a non-durable subscription
            if (_isPubSub && !dispatcherState.isDurable())
            {
                try
                {
                    String subscriptionID = null;
                    //JMS2 allows non-durable shared subscriptions. Provide subscriptionID only for
                    //non-durable non-shared which would come with subscriptionID as null
                    if (dispatcherState.getSubscriberID() == null)
                        subscriptionID = "_NON_DURABLE_NON_SHARED" + subscriptionItemStream.getID();
                    else
                        subscriptionID = dispatcherState.getSubscriberID();

                    dispatcherState.setSubscriberID(subscriptionID);
                } catch (NotInMessageStore nims)
                {
                    // No FFDC Code Needed.
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        SibTr.event(tc, "NotInMessageStore exception caught retrieving subscription id!", nims);

                    // Nothing we can do at this point so use a dummy
                    // subscriber id for now.
                    dispatcherState.setSubscriberID("_NON_DURABLE_XXXX");
                }
            }
        }

        if (subscriptionItemStream.isUnableToOrder())
        {
            // PK69943 We are still at a point where we don't know if ordering is enabled.
            // So we callback to the destination handler so it can display a message if we
            // later find ordering is requested.
            destination.setIsUnableToOrder(true);
        }

        createControlAdapter();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "ConsumerDispatcher", this);
    }

    /**
     * Create a pt-pt ConsumerDispatcher
     * 
     * @param destination the owning Destination
     * @param itemStream The item stream to be used
     * @param dispatcherState A state object is also created for pt-to-pt to store the
     *            selector and discriminator information, if any
     */
    public ConsumerDispatcher(
                              BaseDestinationHandler destination,
                              PtoPMessageItemStream itemStream,
                              ConsumerDispatcherState dispatcherState)
    {
        //do the main work of creating ConsumerDispatcher
        this(destination, dispatcherState);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "ConsumerDispatcher",
                        new Object[] { destination, itemStream, dispatcherState });

        //store the itemStream
        this.itemStream = itemStream;

        _isPubSub = destination.isPubSub();

        // An itemstream that is unable to work with ordered messaging should only
        // crop up at reconciliation time. Therefore we can set the ordering value of
        // a destination before anyone uses it.
        if (itemStream.isUnableToOrder())
        {
            // PK69943 We are still at a point where we don't know if ordering is enabled.
            // So we callback to the destination handler so it can display a message if we
            // later find ordering is requested.
            destination.setIsUnableToOrder(true);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "ConsumerDispatcher", this);
    }

    /**
     * Do the main work of creating ConsumerDispatcher
     * 
     * @param destination The owning Destination
     * @param dispatcherState Some state information
     */
    private ConsumerDispatcher(
                               BaseDestinationHandler destination,
                               ConsumerDispatcherState dispatcherState)
    {
        super(destination);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "ConsumerDispatcher",
                        new Object[] { destination, dispatcherState });

        //record the sub state
        this.dispatcherState = dispatcherState;
        //create a new list to hold references to the attached consumers
        consumerPoints = new LinkedList<DispatchableKey>();
        //create new stacks to hold references to the ready consumers
        nonSpecificReadyCPs = new SimpleLinkedList();
        readyFwdScanningCPs = new SimpleLinkedList();

        //itemStream.setListener(new QPMessageItemListener(this));

        // Set the state of the ConsumerDispatcher to be locked until the
        // creation of the destination it is associated with is
        // committed.
        state = SIMPState.LOCKED;

        // Set current Receive Allowed state
        currentReceiveAllowed = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "ConsumerDispatcher", this);
    }

    /**
     * Store message on the ConsumerDispatchers itemStream, storing on the inputHandler
     * first if required.
     * 
     * @param msg The message to be stored
     * @param transaction The transaction to be used
     * @param inputHandlerStore The inputHandler from which the message was received
     * @param storedByIH true if the message has already been stored in the IH's itemstream
     * @return true if the the message was stored in the IH, either before or during this call
     * @throws SIResourceException if there was a resource problem in the message store.
     * @throws SIStoreException thrown if there was a problem in the message store
     */
    boolean storeMessage(
                         MessageItem msg,
                         TransactionCommon transaction,
                         InputHandlerStore inputHandlerStore,
                         boolean storedByIH)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "storeMessage",
                        new Object[] {
                                      msg,
                                      transaction,
                                      inputHandlerStore,
                                      Boolean.valueOf(storedByIH) });

        //Store the msg to the item stream
        //When pubsub and we are attached to the output side of the destination (feature 176658.3.7)
        //we have to have stored this message under the InputHandler
        //item stream first, then reference it here
        if (_isPubSub)
        {
            try
            {
                // If this subscription is not an internal subscription then
                // create references on the relevant durable streams
                if (dispatcherState.getTargetDestination() == null)
                {
                    //If the message has not yet been stored in the IH
                    if (!storedByIH)
                    {
                        //store it in the IH
                        inputHandlerStore.storeMessage(msg, transaction);
                        storedByIH = true;

                    }

                    boolean downgradePersistence = false;

                    // If this is a non-durable subscription there is no need to persist a
                    // reference to a persistent message, it can be downgraded to a volatile
                    // reference.
                    if (dispatcherState.isDurable())
                    {
                        msg.addPersistentRef();
                    }
                    else
                    {
                        msg.addNonPersistentRef();
                        downgradePersistence = true;
                    }

                    //store a reference in the reference stream
                    final MessageItemReference msgRef =
                                    new MessageItemReference(msg, downgradePersistence);

                    subscriptionItemStream.getSubscriptionLockManager().lock();

                    try
                    {
                        if (!subscriptionItemStream.isToBeDeleted())
                        {
                            registerForEvents(msgRef);
                            Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(transaction);
                            subscriptionItemStream.add(msgRef, msTran);
                            msTran.registerCallback(msgRef); //PM38052 registering this callback allows afterCompletion 
                                                             // to be called on the MessageItemReference
                        }
                    } finally
                    {
                        subscriptionItemStream.getSubscriptionLockManager().unlock();
                    }

                }
                else
                {
                    // SM0010.mp.1 This is an internal subscription. We do not
                    // want to create references. Instead we want to bypass
                    // the reference stream and forward on to the target dest

                    // Lookup the target destination
                    DestinationHandler target;
                    try
                    {
                        target = _baseDestHandler.
                                        getDestinationManager().
                                        getDestination(dispatcherState.getTargetDestination(),
                                                       false);

                        if (target == null)
                        {
                            SIResourceException e = new SIResourceException(
                                            nls.getFormattedMessage(
                                                                    "INTERNAL_SUBSCRIPTION_TARGET_NOT_FOUND_CWSIP0115",
                                                                    new Object[] {
                                                                                  dispatcherState.getSubscriberID(),
                                                                                  dispatcherState.getDurableHome(),
                                                                                  dispatcherState.getTargetDestination() },
                                                                    null));

                            // Target destination for internal subscription does not exist
                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.storeMessage",
                                                        "1:583:1.280.5.25",
                                                        this);

                            SibTr.exception(tc, e);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(tc, "storeMessage", e);

                            throw e;
                        }

                        // Take a copy of the message
                        JsMessage msgCopy = null;
                        try
                        {
                            msgCopy = msg.getMessage().getReceived();
                        } catch (MessageCopyFailedException e)
                        {
                            // FFDC
                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.storeMessage",
                                                        "1:606:1.280.5.25",
                                                        this);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(tc, "storeMessage", "MessageCopyFailedException");

                            throw new SIResourceException(e);
                        }

                        // Get the inputhandler associated with it
                        InputHandler inputHandler =
                                        target.
                                                        getInputHandler(target.isPubSub() ? ProtocolType.PUBSUBINPUT : ProtocolType.UNICASTINPUT,
                                                                        _messageProcessor.getMessagingEngineUuid(),
                                                                        null);

                        inputHandler.handleMessage(new MessageItem(msgCopy),
                                                   transaction, _messageProcessor.getMessagingEngineUuid());

                    } catch (SITemporaryDestinationNotFoundException e)
                    {
                        // No FFDC code needed
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "storeMessage", "SITemporaryDestinationNotFoundException");
                        throw new SIResourceException(e);
                    } catch (SINotPossibleInCurrentConfigurationException e)
                    {
                        // No FFDC code needed
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "storeMessage", "SINotPossibleInCurrentConfigurationException");
                        throw new SIResourceException(e);
                    } catch (SIIncorrectCallException e)
                    {
                        // No FFDC code needed
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "storeMessage", "SIIncorrectCallException");
                        throw new SIResourceException(e);
                    }
                }
            } catch (RollbackException e)
            {
                // No FFDC code needed

                // We catch the RollbackException explicitly because we do not want to generate
                // and FFDC and error in the log. Rollbacks are not internal errors.
                // Any real errors will be caught by the MessageStoreException catch block below.
                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "storeMessage", e);

                throw new SIResourceException(e);
            } catch (InvalidAddOperation e)
            {
                // No FFDC code needed
                if (state != SIMPState.DELETED)
                {
                    // MessageStoreException shouldn't occur so FFDC.
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.storeMessage",
                                                "1:668:1.280.5.25",
                                                this);

                    SibTr.exception(tc, e);
                    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                new Object[] {
                                              "com.ibm.ws.sib.processor.impl.ConsumerDispatcher",
                                              "1:675:1.280.5.25",
                                              e });

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "storeMessage", e);

                    throw new SIResourceException(
                                    nls.getFormattedMessage(
                                                            "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                            new Object[] {
                                                                          "com.ibm.ws.sib.processor.impl.ConsumerDispatcher",
                                                                          "1:686:1.280.5.25",
                                                                          e },
                                                            null),
                                    e);
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "ConsumerDeispatcher deleted " + this);

            } catch (MessageStoreException e)
            {
                // MessageStoreException shouldn't occur so FFDC.
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.storeMessage",
                                            "1:702:1.280.5.25",
                                            this);

                SibTr.exception(tc, e);
                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.ConsumerDispatcher",
                                          "1:709:1.280.5.25",
                                          e });

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "storeMessage", e);

                throw new SIResourceException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.ConsumerDispatcher",
                                                                      "1:720:1.280.5.25",
                                                                      e },
                                                        null),
                                e);
            }

        }
        else //pt-pt
        {
            // Add the item to the itemstream.   183715.1
            // The itemstream should never be null.  If it is a null pointer exception
            // will be thrown.

            // The transaction should also never be null.  An auto-commit transaction
            // should be passed in if the message is not transacted.

            try
            {
                // Register for the event on the listener.
                _baseDestHandler.registerForEvents(msg);

                // 516307: Register for events before we add the message. If we're using an auto commit tran
                // then there's a possibility that a consumer can come in on another thread and see this message
                // even before the addItem method returns. This means they can remove the message from the
                // ItemStream and no events will be driven, so no post processing occurs, including PMI statistics
                // counting and re-driving the message if the consumer chooses to unlock or roll back the consumption
                // of the message.
                // (this isn't a problem for pubsub as we never use an auto commit tran)
                registerForEvents(msg);

                Transaction msTran = _messageProcessor.resolveAndEnlistMsgStoreTransaction(transaction);
                itemStream.addItem(msg, msTran);
            } catch (OutOfCacheSpace e)
            {
                // No FFDC code needed
                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "storeMessage", e);

                throw new SIResourceException(e);
            } catch (MessageStoreException e)
            {
                // MessageStoreException shouldn't occur so FFDC.
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.storeMessage",
                                            "1:769:1.280.5.25",
                                            this);

                SibTr.exception(tc, e);
                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.ConsumerDispatcher",
                                          "1:776:1.280.5.25",
                                          e });

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "storeMessage", e);

                throw new SIResourceException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.ConsumerDispatcher",
                                                                      "1:787:1.280.5.25",
                                                                      e },
                                                        null),
                                e);
            }

            storedByIH = false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "storeMessage", Boolean.valueOf(storedByIH));

        return storedByIH;
    }

    @Override
    public void registerForEvents(SIMPMessage msg)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "registerForEvents", msg);
        msg.registerMessageEventListener(MessageEvents.POST_COMMIT_ADD, this);
        msg.registerMessageEventListener(MessageEvents.POST_COMMIT_REMOVE, this);
        msg.registerMessageEventListener(MessageEvents.POST_ROLLBACK_REMOVE, this);
        msg.registerMessageEventListener(MessageEvents.UNLOCKED, this);
        msg.registerMessageEventListener(MessageEvents.PRE_UNLOCKED, this);

        /*
         * The messages itemstream is interested in events in case the destination
         * is deleted and the messages need to be processed as they appear on
         * the queue.
         */
        if (itemStream != null)
        {
            itemStream.registerForEvents(msg);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerForEvents");
    }

    /**
     * Actually hand the message on to a consumerPoint. This is where no-local and
     * exception destination processing is done.
     * 
     * @param msg The message being delivered
     * @param tran The transaction being used
     * @param consumerPoint The consumer point to give the message to
     * @param storedByCD true if the message is stored on the CD's itemstream
     * @return true if the message was taken by the consumer point
     * @throws SIStoreException thrown if there was a problem in the message store
     */
    private boolean giveMessageToConsumer(
                                          SIMPMessage msg,
                                          TransactionCommon tran,
                                          DispatchableConsumerPoint consumerPoint,
                                          boolean storedByCD)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "giveMessageToConsumer",
                        new Object[] { msg, tran, consumerPoint, Boolean.valueOf(storedByCD) });

        boolean messageTaken = false;

        // Identify the fact that the message originated on a local queue point
        // in order for consumer key groups to find the correct getCursor later on
        if (msg.getLocalisingMEUuid() == null)
            msg.setLocalisingME(_messageProcessor.getMessagingEngineUuid());

        //Put the message to the consumer
        messageTaken = consumerPoint.put(msg, storedByCD);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "giveMessageToConsumer", Boolean.valueOf(messageTaken));

        return messageTaken;
    }

    /**
     * Put a message on this ConsumerDispatcher for delivery to consumers.
     * 
     * @param msg The message to be delivered
     * @param tran The transaction to be used (must at least have an autocommit transaction)
     * @param inputHandlerStore The input handler putting this message
     * @param storedByIH true if the message has already been stored in the IH
     * @return true if the message was stored in the IH (either before or during this call)
     * @throws SIStoreException thrown if there is a problem in the message store
     */
    @Override
    public boolean put(
                       SIMPMessage msg,
                       TransactionCommon tran,
                       InputHandlerStore inputHandlerStore,
                       boolean storedByIH) throws SIDiscriminatorSyntaxException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "put",
                        new Object[] { msg, tran, inputHandlerStore, Boolean.valueOf(storedByIH) });

        //Set a unique id in the message if explicitly told to or
        //if one has not already been set
        JsMessage jsMsg = msg.getMessage();
        if (msg.getRequiresNewId() || jsMsg.getSystemMessageId() == null)
        {
            jsMsg.setSystemMessageSourceUuid(_messageProcessor.getMessagingEngineUuid());
            jsMsg.setSystemMessageValue(_messageProcessor.nextTick());
            msg.setRequiresNewId(false);
        }

        // If COA Report messages are required, this is when we need to create and
        // send them.

        if (msg.getMessage().getReportCOA() != null)
        {
            // PM81457.dev: If the target destination for the message is WMQ, WMQ would produce the report
            // message and put it in to the reply queue (set in reverse routing path for the message).
            // So we should avoid SIB to produce report message for the below reasons.
            //  (1) There will be a duplicate report message and hence applications are mandated to consume
            //      both report messages, otherwise second report message would stay in the reply queue forever.
            //  (2) When application consumes the SIBus report, it might think that the report message is
            //      from WMQand the message flow ends. But it possible that there could be a failure from
            //      WMQ side and it would never send a report message.
            if (!_baseDestHandler.isMQLink()) {
                // Create the ReportHandler object if not already created
                if (reportHandler == null)
                    reportHandler = new ReportHandler(_messageProcessor);

                try
                {
                    reportHandler.handleMessage(msg, tran, SIApiConstants.REPORT_COA);
                } catch (Exception e)
                {
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.put",
                                                "1:912:1.280.5.25",
                                                this);

                    SibTr.exception(tc, e);

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(tc, "put", "SIResourceException");

                    throw new SIResourceException(e);
                }
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "Ignored report message generation for the request message targeted for MQ destination (via MQLink)", _baseDestHandler.getName());
            }
        }

        // TODO : Can we do a seperate early check for noLocal on non-durable subs here?

        // 174000
        // If pt-to-pt and the put was transacted then we know we need to store the message
        // The message will then be dispatched as part of the postCommit callback.
        // If pubsub, we delay the store until we know whether noLocal is set and any
        // consumers actually need the message.

        if (msg.isTransacted() && !_isPubSub)
        {
            final boolean retVal =
                            storeMessage((MessageItem) msg, tran, inputHandlerStore, storedByIH);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "put", Boolean.valueOf(retVal));

            return retVal;
        }

        final boolean retVal =
                        internalPut(msg, tran, inputHandlerStore, storedByIH, false, true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "put", Boolean.valueOf(retVal));

        return retVal;
    }

    /**
     * Store the message if required, Find a ready consumer, and attempt to deliver the message
     * to that ready consumer if required. This may involve a call to the matchspace.
     * 
     * @param msg The message to be dispatched
     * @param tran The transaction to be used
     * @param inputHandlerStore The InputHandlerStore which put this message
     * @param storedByIH true if the message has already been stored by the IH
     * @param storedByCD true if the message has already been stored by the CD
     * @param firstPut true if this is the first time this message is being dispatched
     * @return true if the message has been stored by the IH by the end of this call
     * @throws SIStoreException throw if there is a problem in the message store
     */
    private boolean internalPut(
                                SIMPMessage msg,
                                TransactionCommon tran,
                                InputHandlerStore inputHandlerStore,
                                boolean storedByIH,
                                boolean storedByCD,
                                boolean firstPut) throws SIResourceException, SIDiscriminatorSyntaxException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "internalPut",
                        new Object[] {
                                      msg,
                                      tran,
                                      inputHandlerStore,
                                      Boolean.valueOf(storedByIH),
                                      Boolean.valueOf(storedByCD),
                                      Boolean.valueOf(firstPut),
                                      msg.getMessage().getSystemMessageId() });

        long lastSpecificVersion = 0;
        // Used to determine if the consumer set has changed
        boolean finalMatch = false;
        // Once this is true it doesn't matter if the consumers change
        boolean continueSearch = true;
        DispatchableKey readyConsumer = null;
        boolean newMatchRequired = false;
        boolean grabCurrentReadyVersion = false;
        boolean statsNeedUpdating = firstPut;
        // Stats are only updated if firstPut
        long newestReadyVersion = Long.MAX_VALUE;
        // The newest ready consumer we're interested in
        MatchingConsumerPoint[] matchResults = null;
        // The search results object is used to retrieve the list of matching consumer keys
        // if needed and the result is stored in the matchResults object.
        MessageProcessorSearchResults searchResults = null;

        boolean specificConsumer = false;
        // Used to determine what kind of consumer we have
        boolean eligibleForDelivery = true;
        // Set to false if our noLocal check means we don`t dispatch the msg
        boolean shortLivedProducerSeedIncremented = false;
        // Set to true to ensure we do not double-increment the shortLivedProducerSeed

        // Private list of ready forward scanning consumers
        java.util.ArrayList<DispatchableKey> forwardScanningReadyConsumers = null;

        // Indicates that a messageItemReference was created early before the message
        // was stored.
        boolean referenceCreatedEarly = false;

        if (storedByCD)
        {
            // As we've already stored the message we only need to check the consumers that
            // were ready at the time (actually just after) we stored it. Any consumers
            // becoming ready since then will have to have checked the message store on
            // the way in and will see the message if its still there.
            grabCurrentReadyVersion = true;
        }

        while (continueSearch)
        {
            synchronized (_baseDestHandler.getReadyConsumerPointLock())
            {

                //Once the message has been stored there is no need in trying to give a message
                //to a newly ready consumer (in fact it could cause us to loop indefinitely). So
                //once we've stored the message we grab the ready consumer set version (updated
                //everytime a consumer goes from notready to ready)
                if (grabCurrentReadyVersion)
                {
                    newestReadyVersion = readyConsumerVersion;
                    grabCurrentReadyVersion = false;
                }
                //Search the non-specific ready list (performed under the lock to ensure we
                //see the current ready consumers). popFirstReadyConsumer() will only return
                //a consumer that was made ready as or before readyConsumerVersion exceeded
                //the newestReadyVersion value.
                //readyConsumer = nonSpecificReadyConsumerPoints.pop(newestReadyVersion);
                readyConsumer = (DispatchableKey) nonSpecificReadyCPs.getFirst();
                if (readyConsumer != null)
                {
                    if (readyConsumer.getVersion() > newestReadyVersion)
                    {
                        readyConsumer = null;
                    }
                }

                if (readyConsumer != null)
                {
                    // We have a nonSpecificReadyConsumer
                    specificConsumer = false;

                    // We may have a key group in our hands. This is a good point to resolve
                    // down to an individual member of the group
                    readyConsumer = readyConsumer.resolvedKey();

                    // We have a ready consumer, we have to release the readyList lock before
                    // we can try to deliver the message as this will attempt to get the
                    // consumer's lock and a consumer can hold this while trying to add
                    // themselves to the QP's ready list. This would cause deadlock.

                    // popFirstReadyConsumer() will have removed this consumer from the ready
                    // list as there are only two possible outcomes, either they take the
                    // message or they're no longer ready. In either case they can be removed
                    // from the list.

                    // As we've set readyConsumer we need do nothing here, just drop through
                }
                else
                {
                    // We try for a specificReadyConsumer
                    specificConsumer = true;

                    // Otherwise, look to see if there are any ready specific consumers (performed
                    // under the lock to ensure we see the correct value)
                    if (specificReadyConsumerCount > 0)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(tc, "sepcificReadyConsumerCount:" + specificReadyConsumerCount);

                        // If the specific consumer set has changed since the last time we were
                        // here (or if it's our first time) then we have to get matchspace to
                        // re-parse the message. Otherwise we can use the results from the
                        // previous time round the loop. Again, this is under the lock to make sure
                        // we see the right version).
                        if ((specificConsumerVersion != lastSpecificVersion)
                            && (!finalMatch))
                        {
                            if ((lastSpecificVersion > 0) // i.e. we've matched before
                                && (!storedByCD))
                            {
                                // If we already have a set of match results and the consumer set has changed
                                // since then but we haven't put the message into the Message Store yet we may as
                                // well skip the match here and store the message otherwise, if the consumer
                                // set keeps changing, we'll go round this match loop forever.
                            }
                            else
                            {
                                newMatchRequired = true;
                                lastSpecificVersion = specificConsumerVersion;

                                // If the message is already in the Message Store it is visible to new consumers
                                // therefore, once we've performed this match it doesn't matter if new consumers
                                // come along later as they'll have the chance to see the message without our
                                // help. So this match can be our last.
                                if (storedByCD)
                                    finalMatch = true;
                            }
                        }
                        else
                        {
                            // If we are here and matchResults are null, then we'll have
                            // driven the MatchSpace to analyseMessages for XD but have yet
                            // to extract ConsumerPoint results from the SearchResults
                            if (matchResults == null)
                            {
                                // Extract the ConsumerPoint results
                                Object allResults[] =
                                                searchResults.getResults(_baseDestHandler.getName());
                                Set matchSet =
                                                (Set) allResults[MessageProcessorMatchTarget.JS_CONSUMER_TYPE];
                                matchResults =
                                                (MatchingConsumerPoint[]) matchSet.toArray(new MatchingConsumerPoint[0]);
                            }
                            // Now process the matchResults
                            if (matchResults.length > 0)
                            {
                                // We probably have ready specific consumers and we have a valid set
                                // of matchspace results so we need to pick one to deliver the
                                // message to.

                                // We need a seed to allow us to attempt a round-robin selection
                                // from the set of ready consumers. For a long lived producer,
                                // this is based on the number of messages produced in the session.
                                int seed = msg.getProducerSeed();

                                // When the producer is short lived (common in J2EE) we cannot use
                                // the count of messages from that producer to provide even distribution.
                                // In this case we increment our own dispatcher-wide seed count.
                                // Negative counts mean an producer which has wrapped passed maxint.
                                if (seed < SIMPConstants.LONG_LIVED_PRODUCER_THRESHOLD && seed >= 0) {
                                    // PK74905 This producer has sent few messages - use local seed count.
                                    seed = shortLivedProducerSeed;
                                    // Ensure we only increment the seed once, as we might go round
                                    // the loop again and re-drive this logic (for a non-transacted
                                    // send where we find we need to call store before we deliver).
                                    // 630988.1 defect information
                                    // Let us increment the seed value only if the match results return more than 1.
                                    // When there are two MDB instances (two WPS app servers in a cluster), each
                                    // matching request message would have two waiting consumers, but each reply message
                                    // would only have one matching consumer, And as they arrive interleaved, incrementing
                                    // the seed for every message would always result in the same request message consumer being
                                    // chosen first. As there is no need to use the seed when there is only one matching consumer
                                    // (the reply messages), then avoiding the seed increment would prevent this anomaly
                                    if (!shortLivedProducerSeedIncremented && matchResults.length > 1) { // 630988.1
                                        shortLivedProducerSeed++;
                                        shortLivedProducerSeedIncremented = true;
                                    }
                                }

                                // Use the seed to determine the startpoint in the matchspace results array
                                int startPoint = seed % matchResults.length;

                                // Protect against a negative index. Mod can give negative so we need
                                // this here, but we don't have to worry about Integer.MIN_VALUE problems.
                                if (startPoint < 0)
                                    startPoint = (0 - startPoint);

                                int index = startPoint;

                                // Check for a ready consumer
                                while (readyConsumer == null)
                                {
                                    // Retrieve the DispatchableKey from the match result.
                                    final DispatchableKey match = matchResults[index].
                                                    getConsumerPointData();

                                    // Find a ready consumer that is old enough for us
                                    if ((match.isKeyReady()) && (match.getVersion() <= newestReadyVersion))
                                    {
                                        // Remember this consumer but we won't try to deliver the message
                                        // until the lock is released.
                                        readyConsumer = match;
                                    }
                                    else
                                    {
                                        // Move on to the next consumer but check we haven't looped round
                                        // to the start.
                                        index = (index + 1) % matchResults.length;
                                        if (index == startPoint)
                                            break;
                                    }
                                }
                            }
                        }
                    }
                }

                // If we have a consumer, and while we still have the lock, we need to check if
                // the consumer has specified noLocal and is still eligible for the message. (i.e.
                // it is not on the same connection as the producer). If ineligible then we
                // stop looking for consumers, do not store, and drop through to the end of the
                // method.

                if (readyConsumer != null)
                {
                    // If noLocal is set to true and...
                    // If the ProducerConnectionID is the same as the ConsumerConnection ID then we
                    // do not deliver or store this message.
                    if (dispatcherState.isNoLocal()
                        && readyConsumer.getConnectionUuid().equals(msg.getProducerConnectionUuid()))
                    {
                        // We don`t want to deliver to the consumer since the message was
                        // produced on the same connection. Therefore flag that we want to
                        // "drop" the message from this consumer
                        eligibleForDelivery = false;
                        continueSearch = false;

                    }
                    else
                    {
                        // If we haven`t stored the message yet, check whether we need to.
                        if ((!storedByCD)
                            && (readyConsumer.requiresRecovery(msg) || msg.isTransacted()))
                        {
                            // If we have a transacted consumer or producer and we haven`t stored the message yet,
                            // then do not deliver yet. Wait until the message is stored.
                            readyConsumer = null;

                            if (msg.isTransacted())
                            {
                                // If producer transacted then drop out. We don`t want to deliver yet,
                                // we leave this until the postCommit call. We know that there is an eligible
                                // consumer (after the noLocal checks) and we therefore need to store the message.
                                continueSearch = false;
                            }
                        }
                        else
                        {
                            // Make the consumer unready because we are going to use it.
                            readyConsumer.markNotReady();

                            // and forget about them for now
                            removeReadyConsumer(readyConsumer.getParent(), specificConsumer);

                            // PM31067 start
                            // If we are pub/sub and we have an unrecoverable message (BENP) or not transacted
                            // then we will be trying to give the message straight to the consumers rather than storing
                            // the message first. This means we will be bypassing the storeMessage method which
                            // would have created the MessageItemReference, this also means we don't reply on any 
                            // transactional callbacks, such as in the BENP transacted case.
                            // We need to create a MsgItemReference for each interested consumer as when we deliver
                            // the message to the consumer the LCP will be registering for message events. If each
                            // LCP (consumer) does this on the same MessageItem then we throw an InternalException.
                            // We need each LCP to register for message events on there own MessageItemReference.
                            //
                            // If we do the below but then the consumer does not take the message (giveMessagetoConsumer
                            // returns false) then we will store the message. In this case we need to revert back to the
                            // original messageItem to store rather than this new messageItemReference.
                            if (isPubSub())
                            {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    SibTr.debug(tc, "An non-recoverable or non-transacted pubsub message, create MsgReference");

                                // there is a chance that we might be passed in a MessageItemReference to start with if
                                // we have gone down this path before but the consumer became unready to take the message 
                                if (msg instanceof MessageItem)
                                {
                                    referenceCreatedEarly = true;
                                    MessageItemReference msgRef = new MessageItemReference((MessageItem) msg, true);
                                    msg = msgRef;
                                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                        SibTr.debug(tc, "MsgReference created: " + msg);
                                }
                                else
                                {
                                    // the msg is probably already a msgItemReference so no need to do anything
                                }
                            }
                        }
                    }
                }
                // If we have no normal consumer ready we can see if there are any forward scanning
                // consumers ready. If there are and the message is already in the message store we
                // can just wake them all up and let them fight for the message.
                // We have to wake them all up as we don't know which if any of them has a cursor
                // behind this message, if we picked one and their cursor was already ahead of the message
                // the consumer would accept the put but not see the message and this method would
                // assume it had sccessfully given the message to someone and not try another
                // forward scanning consumer. So instead by waking them all up and they
                // all check for the message and at most one locks it. If this consumer then chooses
                // not to keep it they will unlock it and this method will be redriven, giving the
                // other consumers a chance.
                //TODO Currently the MS cursor will actually move all cursors over the new message
                //     so when we wake them up the second time they won't see the message. MS promise
                //     to fix this soon.
                else
                {
                    if (storedByCD)
                    {
                        // We don't hold the list lock while we wake up the consumers so we have to
                        // make a copy of the list of ready forward scanning consumers while we still hold
                        // the lock.
                        DispatchableKey readyFSConsumer = (DispatchableKey) readyFwdScanningCPs.getFirst();
                        if (readyFSConsumer != null)
                        {
                            DispatchableKey nextReadyFSConsumer;
                            // Make the copy list
                            forwardScanningReadyConsumers = new java.util.ArrayList<DispatchableKey>();
                            while (readyFSConsumer != null)
                            {
                                // Remember the next entry in the list
                                nextReadyFSConsumer = (DispatchableKey) ((SimpleEntry) readyFSConsumer).next();
                                // Add the current entry to the copy list
                                forwardScanningReadyConsumers.add(readyFSConsumer);
                                // Mark this consumer as no longer ready
                                readyFSConsumer.markNotReady();
                                //Remove them from the ready list
                                ((SimpleEntry) readyFSConsumer).remove();
                                // Move the cursor on to the next entry
                                readyFSConsumer = nextReadyFSConsumer;
                            }
                        }
                    }
                }

            } // synchronized

            // Now we are no longer synchronized we can do the actual work...

            // If our noLocal check showed that we don`t want to deliver to this
            // consumer, then drop out of the method.
            if (eligibleForDelivery)
            {

                // If we managed to find a ready consumer we can try to deliver it to them.
                // It is possible that they are no longer interested so we'll have to do all
                // the above again.
                if (readyConsumer != null)
                {
                    // We must NOT hold the readyList lock as this method will attempt to lock
                    // the consumer. The consumer can attempt to lock the readyList while it
                    // holds this lock. This prevents deadlock
                    if (!giveMessageToConsumer(msg,
                                               tran,
                                               readyConsumer.getConsumerPoint(),
                                               storedByCD))
                    {
                        continueSearch = true;
                    }
                    else
                    {
                        continueSearch = false;
                    }

                    // If the consumer rejects the message (they are no longer ready) we need
                    // to do it all over again.
                    if (continueSearch)
                    {
                        readyConsumer = null;
                    }

                }
                // If we have a set of forward scanning consumers we can wake them all up now
                // and let them fight for the message.
                else if (forwardScanningReadyConsumers != null)
                {
                    Iterator<DispatchableKey> consumerIterator =
                                    forwardScanningReadyConsumers.iterator();
                    while (consumerIterator.hasNext())
                    {
                        giveMessageToConsumer(
                                              msg,
                                              tran,
                                              (consumerIterator.next()).getConsumerPoint(),
                                              storedByCD);
                    }
                    continueSearch = false;
                }
                // If we need matchspace to parse the message to get a set of matching
                // consumers we should do it now, then we can go back round and see if any
                // of the matching consumers are ready.
                else if (newMatchRequired)
                {
                    // Get a search results object to use
                    searchResults =
                                    (MessageProcessorSearchResults) _messageProcessor
                                                    .getSearchResultsObjectPool()
                                                    .remove();

                    // Search the MatchSpace to find the set of matching consumers
                    searchMatchSpace(msg, searchResults);

                    // Extract the ConsumerPoint results
                    Object allResults[] =
                                    searchResults.getResults(_baseDestHandler.getName());
                    Set matchSet =
                                    (Set) allResults[MessageProcessorMatchTarget.JS_CONSUMER_TYPE];
                    matchResults =
                                    (MatchingConsumerPoint[]) matchSet.toArray(new MatchingConsumerPoint[0]);
                    newMatchRequired = false;
                }
                // If neither of the above were true we have checked all consumers and none
                // of them were ready. If we haven't yet written the message out to the
                // message store we should do so now, then we need to go back round to see
                // if anyone became ready while we were doing it.
                else if (!storedByCD)
                {
                    if (referenceCreatedEarly)
                    {
                        try
                        {
                            msg = (MessageItem) ((MessageItemReference) msg).getReferredItem();
                        } catch (SevereMessageStoreException e)
                        {
                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.internalPut",
                                                        "1:1553:1.280.5.25",
                                                        this);

                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(tc, "internalPut", "SevereMessageStoreException");

                            throw new SIResourceException(e);
                        }
                    }

                    storedByIH =
                                    storeMessage(
                                                 (MessageItem) msg,
                                                 tran,
                                                 inputHandlerStore,
                                                 storedByIH);
                    storedByCD = true;

                    // As we've now stored the message we only need to check the consumers that
                    // were ready at the time (actually just after) we stored it. Any consumers
                    // becoming ready since then will have to have checked the message store on
                    // the way in and will see the message if its still there.
                    grabCurrentReadyVersion = true;

                    // If we have stored the message as part of a transaction we have to drop
                    // out of the search and wait until the message has been committed before
                    // we can choose a consumer for it.
                    // A transaction = null means that it is Auto Commit
                    if (tran != null && !tran.isAutoCommit())
                        continueSearch = false;
                }
                // Otherwise we've checked everyone was ready after we'd written the
                // message to the message store so we can stop trying. If a consumer comes
                // along now ooking for a message it will find it in the message store with
                // no help from us.
                else
                {
                    continueSearch = false;
                }
            }
        }

        // If a MessageProcessorSearchResults object was created, add it back into the pool
        // at this point as it is now safe to do so.
        if (searchResults != null)
            _messageProcessor.getSearchResultsObjectPool().add(searchResults);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "internalPut", Boolean.valueOf(storedByIH));

        return storedByIH;
    }

    /**
     * Returns whether this consumerDispatcher is being used for a durable subscription.
     * 
     * @return false if not pubsub or not durable.
     */
    @Override
    public boolean isDurable()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isDurable");

        final boolean dur = (_isPubSub && dispatcherState.isDurable());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isDurable", Boolean.valueOf(dur));

        return dur;
    }

    /**
     * Add a ready consumer into our ready 'lists'
     * 
     * @param consumerKey - handle to the ready consumer
     * @param bSelector - consumer has a selector or not
     * @return readyConsumerVersion
     */
    @Override
    public long newReadyConsumer(JSConsumerKey consumerKey, boolean bSelector)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "newReadyConsumer",
                        new Object[] { consumerKey, Boolean.valueOf(bSelector) });
        // If this is a forward scanning consumer just remember we have one
        if (consumerKey.getForwardScanning())
        {
            //move the consumerPoint in to the ready list
            readyFwdScanningCPs.put((SimpleEntry) consumerKey);
        }
        else
        {
            // If the consumer has a selector...
            if (bSelector)
            {
                //increment the specific consumer count and the version number
                specificReadyConsumerCount++;

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "sepcificReadyConsumerCount:" + specificReadyConsumerCount);
            }
            else
            {
                //move the consumerPoint in to the ready list
                nonSpecificReadyCPs.put((SimpleEntry) consumerKey);
            }
        }

        // Increment the ready version
        ++readyConsumerVersion;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "newReadyConsumer", Long.valueOf(readyConsumerVersion));
        return readyConsumerVersion;
    }

    /**
     * Remove the ready consumer from our ready 'lists'
     * 
     * @param consumerKey - handle to the ready consumer
     * @param bSelector - consumer has a selector or not
     */
    @Override
    public void removeReadyConsumer(JSConsumerKey consumerKey, boolean bSelector)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeReadyConsumer",
                        new Object[] { consumerKey, Boolean.valueOf(bSelector) });

        if (consumerKey == null)
        {
            SIErrorException e =
                            new SIErrorException(
                                            nls.getFormattedMessage(
                                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                                    new Object[] {
                                                                                  "com.ibm.ws.sib.processor.impl.ConsumerDispatcher",
                                                                                  "1:1684:1.280.5.25" },
                                                                    null));

            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.removeReadyConsumer",
                                        "1:1690:1.280.5.25",
                                        this);

            SibTr.exception(tc, e);

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.ConsumerDispatcher",
                                      "1:1698:1.280.5.25" });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "removeReadyConsumer", e);

            throw e;
        }

        if (consumerKey.getForwardScanning())
        {
            //remove the consumer point from the FS ready list
            ((SimpleEntry) consumerKey).remove();
        }
        else
        {
            if (bSelector)
            {
                //decrement the specific ready consumer counter
                specificReadyConsumerCount--;

                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(tc, "sepcificReadyConsumerCount:" + specificReadyConsumerCount);
            }
            else
            {
                //remove the consumer point from the ready list
                ((SimpleEntry) consumerKey).remove();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeReadyConsumer");
    }

    /**
     * Attach a new ConsumerPoint to this ConsumerDispatcher. A ConsumerKey
     * object is created for this ConsumerPoint which contains various pieces of
     * state information including the consumer point's ready state, which is
     * initially set to not ready. Also included is a getCursor to be used by
     * the consumer point to access this ConsumerDispatcher's item or reference
     * stream.
     * 
     * @param consumerPoint The consumer point being attached
     * @param selector The Filter that the consumer has specified
     * @param discriminator The discriminator that the consumer has specified
     * @param connectionUuid The connections UUID
     * @param readAhead If the consumer can read ahead
     * @return The ConsumerKey object which was created for this consumer point.
     *         being deleted
     */
    @Override
    public ConsumerKey attachConsumerPoint(
                                           ConsumerPoint consumerPoint,
                                           SelectionCriteria criteria,
                                           SIBUuid12 connectionUuid,
                                           boolean readAhead,
                                           boolean forwardScanning,
                                           JSConsumerSet consumerSet) throws SINotPossibleInCurrentConfigurationException, SIDestinationLockedException, SISelectorSyntaxException, SIDiscriminatorSyntaxException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "attachConsumerPoint",
                        new Object[] {
                                      consumerPoint,
                                      criteria,
                                      connectionUuid,
                                      Boolean.valueOf(readAhead),
                                      consumerSet });

        DispatchableConsumerPoint dispatchableConsumerPoint = (DispatchableConsumerPoint) consumerPoint;
        ConsumerKey consumerKey = null;
        synchronized (consumerPoints)
        {
            // Check if the destination has been deleted.
            if ((dispatchableConsumerPoint.getNamedDestination(this).isToBeDeleted()) ||
                (dispatchableConsumerPoint.getNamedDestination(this).isDeleted()))
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "attachConsumerPoint", "destination deleted");

                throw new SINotPossibleInCurrentConfigurationException(
                                nls.getFormattedMessage(
                                                        "DESTINATION_DELETED_ERROR_CWSIP0111",
                                                        new Object[] { _baseDestHandler.getName(),
                                                                      _messageProcessor.getMessagingEngineName() },
                                                        null));
            }

            // Two types of exclusive check here:
            // 1) ReceiveExclusive when another consumer is already on Queue
            // 2) Durable when we're pubsub
            // 3) Ordered messaging enabled
            if (!_isPubSub)
            {
                if (consumerPoints.size() > 0)
                {
                    if (dispatchableConsumerPoint.getNamedDestination(this).isReceiveExclusive())
                    {
                        SIDestinationLockedException e = new SIDestinationLockedException(
                                        nls.getFormattedMessage(
                                                                "DESTINATION_RECEIVE_EXCLUSIVE_CWSIP0114",
                                                                new Object[] { _baseDestHandler.getName(),
                                                                              _messageProcessor.getMessagingEngineName() },
                                                                null));

                        SibTr.exception(tc, e);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "attachConsumerPoint",
                                       "Destination receive exclusive");
                        throw e;
                    }

                    // If this is an ordered consumer and we already have other consumer, reject it
                    if (!dispatchableConsumerPoint.ignoreInitialIndoubts())
                    {
                        SIDestinationLockedException e = new SIDestinationLockedException(
                                        nls.getFormattedMessage(
                                                                "TEMPORARY_CWSIP9999",
                                                                new Object[] { _baseDestHandler.getName(),
                                                                              _messageProcessor.getMessagingEngineName() },
                                                                null));

                        SibTr.exception(tc, e);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "attachConsumerPoint",
                                       "Destination locked due to indoubt messages");
                        throw e;
                    }
                    else // If this ia a normal consumer but we have an ordered consumer attached, reject the consumer
                    if (!consumerPoints.get(0).getConsumerPoint().ignoreInitialIndoubts())
                    {
                        SIDestinationLockedException e = new SIDestinationLockedException(
                                        nls.getFormattedMessage(
                                                                "TEMPORARY_CWSIP9999", // Should be CWSIP0999??
                                                                new Object[] { _baseDestHandler.getName(),
                                                                              _messageProcessor.getMessagingEngineName() },
                                                                null));

                        SibTr.exception(tc, e);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "attachConsumerPoint",
                                       "Destination locked for ordering");
                        throw e;
                    }
                }
            }
            else if (isDurable() && !dispatcherState.isCloned() && consumerPoints.size() > 0)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "attachConsumerPoint", "SIDestinationLockedException");
                // NOTE: this is a bit of an abuse of the locked exception since
                // we'd rather throw SIDurableSubscriptionLockedException.
                // We actually only see this case for remote durable when an
                // existing AOH handles an attempt to attach to an in-use
                // durable sub. In this case, the AOH sends a cardinality error
                // which gets wrapped appropriately at the remote requester.
                throw new SIDestinationLockedException(
                                nls.getFormattedMessage(
                                                        "SUBSCRIPTION_IN_USE_ERROR_CWSIP0152",
                                                        new Object[] { dispatcherState.getSubscriberID(),
                                                                      _messageProcessor.getMessagingEngineName() },
                                                        null));

            }

            if (_baseDestHandler.isOrdered() && consumerPoints.size() > 0)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "attachConsumerPoint", "SIDestinationLockedException");
                // NOTE: this is a bit of an abuse of the locked exception since
                // we'd rather throw an ordered message specific exception.
                // We only allow one consumer to attach when ordered messaging is
                // specified.b
                throw new SIDestinationLockedException(
                                nls.getFormattedMessage(
                                                        "ORDERED_DESTINATION_IN_USE_CWSIP0116",
                                                        new Object[] { _baseDestHandler.getName(),
                                                                      _messageProcessor.getMessagingEngineName() },
                                                        null));

            }

            //create a new ConsumerKey object and add it in to the list of
            //atatched consumer points
            consumerKey =
                            createConsumerKey(
                                              dispatchableConsumerPoint,
                                              criteria,
                                              connectionUuid,
                                              readAhead,
                                              forwardScanning,
                                              consumerSet);

            // Add the CP to the list irrespective of whether p2p or pubsub, we use the array
            // to keep track of whether any CPs are attached.
            consumerPoints.add((DispatchableKey) consumerKey);

        }

        //if we're a pt-pt destination
        if (!_isPubSub)
        {
            // Store the CP in the MatchSpace
            _baseDestHandler.addConsumerPointMatchTarget(
                                                         (DispatchableKey) consumerKey,
                                                         getUuid(), // uuid of the ConsumerDispatcher
                                                         criteria);

            // 594730: Now that we've added the conusmer's selector to the matchspace
            // we know that any search performed by internalPut will pick this consumer
            // up, so we update the specificConsumerVersion now to ensure that we re-run the
            // search. Otherwise there's a chance that a previous search (when this consumer
            // wasn't quite registered) will appear to still be valid, resulting in the
            // consumer not being found and the message not being delivered to it.
            if (((DispatchableKey) consumerKey).isSpecific())
            {
                synchronized (_baseDestHandler.getReadyConsumerPointLock())
                {
                    specificConsumerVersion++;
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "attachConsumerPoint", consumerKey);
        return consumerKey;
    }

    /**
     * Initialise the transaction map for semi-ordered consumers.
     * This occurs when a consumer with ignoreInitialIndoubts set
     * to false is attaching.
     * 
     * Briefly, we track all transactions that this consumer receives
     * under.
     */
    private boolean initialiseTransactionSet() throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "initialiseTransactionSet");

        boolean suspend = false;

        // Always start with an empty set.
        transactionSet = new HashSet();

        try {

            if (itemStream != null)
                itemStream.getActiveTransactions(transactionSet);

        } catch (MessageStoreException e) {
            // FFDC

            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.initialiseTransactionSet",
                                        "1:1957:1.280.5.25",
                                        this);

            SIResourceException e2 = new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.ConsumerDispatcher",
                                                                  "1:1965:1.280.5.25",
                                                                  e }, null), e);

            SibTr.exception(tc, e2);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.ConsumerDispatcher",
                                      "1:1972:1.280.5.25",
                                      e2 });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "initialiseTransactionSet", e2);

            throw e2;
        }

        if (transactionSet.size() > 0)
            suspend = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "initialiseTransactionSet", new Object[] { transactionSet, Boolean.valueOf(suspend) });

        return suspend;
    }

    /**
     * Detach a consumer point from this CD.
     * 
     * @param consumerKey The ConsumerKey object of the consumer point
     *            being detached
     */
    @Override
    public void detachConsumerPoint(ConsumerKey consumerKey) throws SIResourceException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "detachConsumerPoint", consumerKey);

        //  if p2p then we must remove the CP from the matchspace also
        if (!_isPubSub)
        {
            // Remove the CP from the MatchSpace
            _baseDestHandler.removeConsumerPointMatchTarget((DispatchableKey) consumerKey);
        }
        else
        {
            state = SIMPState.DELETED;
        }

        /*
         * 455255
         * A deadlock occurs between the close of consumers on asynch deletion of a destination
         * and the manual close of the consumer by the appliation. The two locks involved are t
         * the consumerPoints lock (below) and a lock on the _pseudoDurableAIHMap object in the
         * RemotePubSubSupport class.
         * To avoid this, we force a lock to be taken on the RemotePubSubSupport object before
         * either of the above locks is obtained. (In the ptop case we just take the lock on consumerPoints
         * twice)
         * The corresponding lock is found on RemotePubSubSupport.cleanupLocalisations()
         */
        final Object deletionLock;
        if (isPubSub())
            deletionLock = _baseDestHandler.getPubSubRealization().getRemotePubSubSupport();
        else
            deletionLock = consumerPoints;

        synchronized (deletionLock)
        {
            synchronized (consumerPoints)
            {
                //remove this consumer point from the list of attached consumer points
                //Note it may not be in the list if the detach is being forced by delete
                //of the destination
                if (consumerPoints.contains(consumerKey))
                {
                    consumerPoints.remove(consumerKey);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Consumer key removed - new size of consumerPoints is " + consumerPoints.size());
                }
                else
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(tc, "Consumer key not in map", consumerKey);
                }

                //166829.1
                //If We are a subscription and non-durable, and we have no consumer attached,
                //then delete this consumer dispatcher.  Note: the one case where we DO delete,
                //even if we're durable, is when we're the RME for a remote durable subscription.
                //However, when we're the RME, we don't have a matchspace entry.
                if (consumerPoints.isEmpty()
                    && _isPubSub &&
                    dispatcherState.getSubscriberID().contains("_NON_DURABLE_NON_SHARED") //only non durable non shared consumers 
                    && (!isDurable() || (this instanceof RemoteConsumerDispatcher)))
                {
                    // The proxy event message needs to be send and the call needs
                    // to be made to the proxy code.
                    // We know that it was also added to the MatchSpace.
                    deleteConsumerDispatcher(!isDurable());
                }
            }
        }

        // Dealing pubsub non-durable shared consumers separately to avoid dead lock
        if (_isPubSub && (!isDurable()) && !dispatcherState.getSubscriberID().contains("_NON_DURABLE_NON_SHARED")) {
            // We have to deal pubsub non-durable shared consumers as separate as there is
            // chance of dead lock. The creatiing/obtaining CD for a cosnumer and attaching involves
            // getting locks on hashmap _destinationManager.getNondurableSharedSubscriptions() and
            // 'ConsumerPoints'
            //The above code locks ConsumerPoints first.. then after tries to lock 
            //_destinationManager.getNondurableSharedSubscriptions(), chance of deadlock

            // no deletionLock is required as no remote support is there.

            synchronized (_baseDestHandler.getDestinationManager().getNondurableSharedSubscriptions()) { // the lock is too harsh as it is entire ME wide
                synchronized (consumerPoints) {
                    if (consumerPoints.isEmpty()) {
                        //no consumers attached then delete CD. It can be assured that no other consumer 
                        // would be trying obtain the CD
                        deleteConsumerDispatcher(!isDurable());
                    }
                }

            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "detachConsumerPoint");
    }

    /**
     * Helper methods to create a ConsumerKey. Can be overridden by subclasses
     * 
     * @param consumerPoint
     * @param getCursor
     * @param selector
     * @param connectionUuid
     * @return
     */
    protected ConsumerKey createConsumerKey(
                                            DispatchableConsumerPoint consumerPoint,
                                            SelectionCriteria criteria,
                                            SIBUuid12 connectionUuid,
                                            boolean readAhead,
                                            boolean forwardScanning,
                                            JSConsumerSet consumerSet)
                    throws SISelectorSyntaxException, SIDiscriminatorSyntaxException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc,
                        "createConsumerKey",
                        new Object[] { consumerPoint,
                                      criteria,
                                      connectionUuid,
                                      Boolean.valueOf(readAhead),
                                      Boolean.valueOf(forwardScanning),
                                      consumerSet });

        ConsumerKey key = new LocalQPConsumerKey(
                        consumerPoint,
                        this,
                        criteria,
                        connectionUuid,
                        forwardScanning,
                        consumerSet);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createConsumerKey", key);

        return key;
    }

    /**
     * Helper method to create a ConsumerKeyGroup. Can be overridden by subclasses
     * 
     * @return
     */
    @Override
    protected JSKeyGroup createConsumerKeyGroup(JSConsumerSet consumerSet)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "createConsumerKeyGroup", consumerSet);

        JSKeyGroup ckg = new LocalQPConsumerKeyGroup(this, consumerSet);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "createConsumerKeyGroup", ckg);

        return ckg;
    }

    /**
     * Returns true if this consumerDispatcher currently has consumers attached to
     * it.
     * 
     * @return boolean
     */
    public boolean hasConsumersAttached()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "hasConsumersAttached");

        //Does the consumer list have any elements? If so return true, else false
        boolean hasConsumersAttached = (getConsumerCount() > 0);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "hasConsumersAttached", Boolean.valueOf(hasConsumersAttached));

        return hasConsumersAttached;
    }

    /**
     * Get the number of consumers on this ConsumerDispatcher
     * <p>
     * Feature 166832.23
     * 
     * @return number of consumers.
     */
    @Override
    public int getConsumerCount()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getConsumerCount");

        int consumerCount;

        synchronized (consumerPoints)
        {
            consumerCount = consumerPoints.size();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getConsumerCount", Integer.valueOf(consumerCount));

        return consumerCount;
    }

    /**
     * This list is cloned to stop illegal access to the ConsumerPoints
     * controlled by this ConsumerDispatcher
     * 
     * @return
     */
    @Override
    public List<DispatchableKey> getConsumerPoints()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getConsumerPoints");
            SibTr.exit(tc, "getConsumerPoints", consumerPoints);
        }
        return (List<DispatchableKey>) consumerPoints.clone();
    }

    /**
     * Attempts to dereference this consumerDispatcher. Will throw exception if consumers are
     * attached. If succesful, the consumerDispatcher reference will be removed from the destination
     * and the consumerDispatcher will be removed from the matchspace.
     * 
     * @param callProxyCode Whether the proxy code should be called at all!
     */
    public void deleteConsumerDispatcher(
                                         boolean callProxyCode)

                    throws SIRollbackException, SIConnectionLostException, SIResourceException,
                    SIErrorException, SINotPossibleInCurrentConfigurationException

    //throws SIResourceException, SINotPossibleInCurrentConfigurationException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "deleteConsumerDispatcher",
                        new Object[] {
                                      Boolean.valueOf(callProxyCode),
                                      Boolean.valueOf(isInMatchSpace) });

        //Lock the list of consumers to stop anyone connecting to this consumerDispatcher
        synchronized (consumerPoints)
        {
            //Check for attached consumers. If so, throw exception and exit
            if (hasConsumersAttached())
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "deleteConsumerDispatcher", "Consumers attached ");
                throw new SINotPossibleInCurrentConfigurationException(
                                nls.getFormattedMessage(
                                                        "DESTINATION_DELETION_ERROR_CWSIP0112",
                                                        new Object[] { _baseDestHandler.getName() },
                                                        null));
            }
        }

        state = SIMPState.DELETED;

        //Call the derference routine on the destination. This will remove from the matchspace also
        // Only required if it really was added to the MatchSpace.
        if (isInMatchSpace)
            _baseDestHandler.dereferenceSubscriptionConsumerDispatcher(
                                                                       this,
                                                                       !isDurable(),
                                                                       callProxyCode);

        try
        {
            if (isDurable())
            {
                if (this instanceof RemoteConsumerDispatcher)
                    // We're an RME for remote durable.  Tell the destination to clean
                    // up any protocol state for us.
                    _baseDestHandler.deleteRemoteDurableRME(dispatcherState);
                else
                    // We may have allowed remote access, in which case we need to clean
                    // up any existing AOHs
                    _baseDestHandler.deleteRemoteDurableDME(dispatcherState.getSubscriberID());
            }
        } catch (SIDurableSubscriptionNotFoundException e)
        {
            // This would should be FFDC'd
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.deleteConsumerDispatcher",
                                        "1:2265:1.280.5.25",
                                        this);

            SibTr.exception(tc, e);

            // No point throwing the exception on, the FFDC is the best we can do
        }
        getControlAdapter().dereferenceControllable();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteConsumerDispatcher");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.Browsable#getBrowseCursor(com.ibm.ws.sib.store.Filter)
     */
    @Override
    public BrowseCursor getBrowseCursor(SelectionCriteria criteria) throws SIResourceException, SISelectorSyntaxException
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

            //try to get a new browseCursor from the message store
            if (itemStream != null)
                msgStoreCur = itemStream.newNonLockingItemCursor(filter);
            else if (subscriptionItemStream != null)
                msgStoreCur = subscriptionItemStream.newNonLockingCursor(filter);

            cursor = new JSBrowseCursor(msgStoreCur);

        } catch (SISelectorSyntaxException e)
        {
            // shouldn't occur so FFDC.
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.getBrowseCursor",
                                        "1:2314:1.280.5.25",
                                        this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.ConsumerDispatcher",
                                      "1:2321:1.280.5.25",
                                      e });

            if (cursor != null)
            {
                //try and finish off this bad cursor
                try
                {
                    cursor.finished();
                } catch (SISessionDroppedException e1)
                {
                    //an error trying to close the bad cursor. We swallow this error
                    //and instead throw the exception that caused us to try and close
                    FFDCFilter.processException(
                                                e1,
                                                "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.getBrowseCursor",
                                                "1:2338:1.280.5.25",
                                                this);

                    SibTr.exception(tc, e1);
                    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                new Object[] {
                                              "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.getBrowseCursor",
                                              "1:2345:1.280.5.25",
                                              SIMPUtils.getStackTrace(e1) });
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "getBrowseCursor", e);
            throw e;
        } catch (Exception e)
        {
            // shouldn't occur so FFDC.
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.getBrowseCursor",
                                        "1:2361:1.280.5.25",
                                        this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.ConsumerDispatcher",
                                      "1:2368:1.280.5.25",
                                      e });

            if (cursor != null)
            {
                try
                {
                    cursor.finished();
                } catch (SISessionDroppedException e1)
                {
                    FFDCFilter.processException(
                                                e1,
                                                "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.getBrowseCursor",
                                                "1:2382:1.280.5.25",
                                                this);

                    SibTr.exception(tc, e1);
                    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                new Object[] {
                                              "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.getBrowseCursor",
                                              "1:2389:1.280.5.25",
                                              SIMPUtils.getStackTrace(e1) });
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "getBrowseCursor", e);

            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.ConsumerDispatcher",
                                                                  "1:2402:1.280.5.25",
                                                                  e },
                                                    null),
                            e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getBrowseCursor", cursor);
        //return the cursor or null
        return cursor;
    }

    /**
     * Method getSubscriptionState. Returns an object containing the
     * current attributes of this subscription.
     * 
     * @return ConsumerDispatcherState
     */
    @Override
    public ConsumerDispatcherState getConsumerDispatcherState()
    {
        return dispatcherState;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        String output = "@" + Integer.toHexString(this.hashCode()) + "[" + _baseDestHandler.getName() + ",";

        if (itemStream != null)
            output += "IS@" + Integer.toHexString(itemStream.hashCode());
        else if (subscriptionItemStream != null)
            output += "SIS@" + Integer.toHexString(subscriptionItemStream.hashCode());
        else
            output += "null";

        return output + "]";
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener#messageEventOccurred(int, com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage,
     * com.ibm.ws.sib.msgstore.Transaction)
     */
    @Override
    public void messageEventOccurred(
                                     int event,
                                     SIMPMessage msg,
                                     TransactionCommon tran)

                    throws SIDiscriminatorSyntaxException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "messageEventOccurred",
                        new Object[] { Integer.valueOf(event), msg, tran });

        if (event == MessageEvents.POST_COMMIT_ADD)
            eventPostCommitAdd(msg, tran);
        else if (MessageEvents.POST_COMMIT_REMOVE == event)
            eventPostCommitRemove(msg, tran);
        else if (event == MessageEvents.POST_ROLLBACK_REMOVE)
            eventPostRollbackRemove(msg, tran);
        else if (event == MessageEvents.UNLOCKED)
            eventUnlocked(msg);
        else if (event == MessageEvents.PRE_UNLOCKED)
            eventPreUnlocked(msg, tran);
        else
        {
            SIErrorException e =
                            new SIErrorException(
                                            nls.getFormattedMessage(
                                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                                    new Object[] {
                                                                                  "com.ibm.ws.sib.processor.impl.ConsumerDispatcher",
                                                                                  "1:2476:1.280.5.25" },
                                                                    null));

            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.messageEventOccurred",
                                        "1:2482:1.280.5.25",
                                        this);

            SibTr.exception(tc, e);

            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.ConsumerDispatcher",
                                      "1:2490:1.280.5.25" });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "messageEventOccurred", e);

            throw e;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "messageEventOccurred");
    }

    /**
     * Called when a message receives an eventCommittedAdd from the messageStore. i.e.
     * a message has been transactionally committed after being put in the messageStore.
     * 
     * @param msg The message which has been committed
     * @param transaction The transaction used to commit the message
     * @throws SIStoreException Thrown if there is ANY problem
     * @see com.ibm.ws.sib.store.AbstractItem#eventCommittedAdd(com.ibm.ws.sib.msgstore.Transaction)
     */
    protected void eventPostCommitAdd(SIMPMessage msg, TransactionCommon transaction)
                    throws SIDiscriminatorSyntaxException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "eventPostCommitAdd", new Object[] { msg, transaction });

        //if receive allowed, try to hand the message on to any ready consumer points
        //at this point it must be in both the IH and CD item/reference streams
        internalPut(msg, transaction, null, true, true, true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventPostCommitAdd");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener#eventPostCommitRemove(com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage,
     * com.ibm.ws.sib.msgstore.Transaction)
     */
    protected void eventPostCommitRemove(SIMPMessage msg, TransactionCommon transaction)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "eventPostCommitRemove", new Object[] { msg, transaction });

        // NB: ideally this would go in afterCompletion but we
        // dont have the transaction reference to register for the callback
        synchronized (orderLock)
        {

            // Remove the transaction from the transactionSet if applicable
            // Use the orderLock to synch with attachConsumer
            if (transactionSet != null)
            {
                transactionSet.remove(transaction.getPersistentTranId());
                if (transactionSet.isEmpty())
                {
                    transactionSet = null;
                    // Get the consumer and resume it
                    consumerPoints.get(0).
                                    getConsumerPoint().
                                    resumeConsumer(DispatchableConsumerPoint.SUSPEND_FLAG_INITIAL_INDOUBTS);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventPostCommitRemove");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener#eventPostRollbackRemove(com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage,
     * com.ibm.ws.sib.msgstore.Transaction)
     */
    protected void eventPostRollbackRemove(
                                           SIMPMessage msg,
                                           TransactionCommon transaction) throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "eventPostRollbackRemove",
                        new Object[] { msg, transaction });

        // NB: ideally this would go in afterCompletion but we
        // dont have the transaction reference to register for the callback
        synchronized (orderLock)
        {

            // Remove the transaction from the transactionSet if applicable
            // Use the orderLock to synch with attachConsumer
            if (transactionSet != null)
            {
                transactionSet.remove(transaction.getPersistentTranId());
                if (transactionSet.isEmpty())
                {
                    transactionSet = null;
                    // Get the consumer and resume it
                    consumerPoints.get(0).
                                    getConsumerPoint().
                                    resumeConsumer(DispatchableConsumerPoint.SUSPEND_FLAG_INITIAL_INDOUBTS);
                }
            }
        }

        // 174107.1
        // Msgstore state model shows that rollback moves to locked state (IF the message
        // was removed under a lock).
        // We must explicitly unlock the message before finding a new consumer.
        try
        {
            if (!msg.isPersistentlyLocked())
            {
                if (!msg.isHidden())
                {
                    if (msg.isLocked())
                        msg.unlockMsg(msg.getLockID(), null, true);
                    else
                        msg.eventUnlocked();
                }
            }
        } catch (MessageStoreException e)
        {
            // MessageStoreException shouldn't occur so FFDC.
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.eventPostRollbackRemove",
                                        "1:2618:1.280.5.25",
                                        this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.ConsumerDispatcher",
                                      "1:2625:1.280.5.25",
                                      e });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "eventPostRollbackRemove", e);

            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.ConsumerDispatcher",
                                                                  "1:2636:1.280.5.25",
                                                                  e },
                                                    null),
                            e);
        }
        //do not want to call internalPut again yet - internalPut will be called from the
        //eventUnlocked callback

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventPostRollbackRemove");
    }

    /*
     * Called by our overridden unlock method in SIMPItem
     */
    protected void eventPreUnlocked(SIMPMessage msg, TransactionCommon tran)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "eventPreUnlocked", new Object[] { msg, msg.guessRedeliveredCount() });

        /*
         * 166955.4
         * Check whether the retryCount has been exceeded. If it has then this is a poison msg
         * and needs to be sent to the exception destination rather than to a valid consumer
         * again
         */
        // But only check for messages that are not persistently locked (messages involved in RemoteGet)
        // Otherwise the remote get protocols dont work

        if ((msg.guessRedeliveredCount() + 1) >= _baseDestHandler.getMaxFailedDeliveries())
        {
            /*
             * Message has been delivered a greater number of times than allowed.
             * Route message on to the exception destination associated with this
             * destination.
             */

            // Deregister rollback callbacks to avoid recursive event callbacks
            msg.deregisterMessageEventListener(MessageEvents.POST_ROLLBACK_REMOVE, this);
            msg.deregisterMessageEventListener(MessageEvents.POST_ROLLBACK_ADD, this);

            UndeliverableReturnCode rc = null;
            try
            {
                if (_isPubSub)
                {
                    // F001333-14610
                    // Pass the subscription ID to the destination handler
                    // so that it can be set on the exceptioned message.
                    rc = _baseDestHandler.handleUndeliverableMessage(msg,
                                                                     SIRCConstants.SIRC0035_BACKOUT_THRESHOLD_ERROR,
                                                                     new String[] { _baseDestHandler.getName(),
                                                                                   _messageProcessor.getMessagingEngineName() },
                                                                     tran,
                                                                     dispatcherState.getSubscriberID());
                }
                else
                {
                    // Not pub sub so no subscription ID to pass
                    rc = _baseDestHandler.handleUndeliverableMessage(msg,
                                                                     SIRCConstants.SIRC0035_BACKOUT_THRESHOLD_ERROR,
                                                                     new String[] { _baseDestHandler.getName(),
                                                                                   _messageProcessor.getMessagingEngineName() },
                                                                     tran);
                }
            } catch (SIResourceException e)
            {
                // No FFDC code needed
                // Exception had already been handled
            }

            // re-register the callbacks
            msg.registerMessageEventListener(MessageEvents.POST_ROLLBACK_REMOVE, this);
            msg.registerMessageEventListener(MessageEvents.POST_ROLLBACK_ADD, this);

            if (rc == null || rc == UndeliverableReturnCode.ERROR)
            {
                // Spin of a new alarm
                _messageProcessor.getAlarmManager().
                                create(SIMPConstants.EXCEPTION_RETRY_TIMEOUT,
                                       new ExceptionDestinationRetryHandler(msg, this));
            }

            if (rc == UndeliverableReturnCode.BLOCK)
            {
                // PK03344 - Handle retry count when no exception destination available
                // When the retry count is reached and we have nowhere to send the message
                // we suspend the consumer for a period of time to avoid 100% cpu usage
                // due to continuous message retry.

                // MDBs will only have one consumer (PK03344 handles the MDB case).
                // For multiple consumers we suspend them all.

                // Pause the consumers for the specified time
                pauseConsumers(msg, _baseDestHandler.getBlockedRetryInterval());
            }

            if (rc != UndeliverableReturnCode.BLOCK)
                msg.setRedeliveryCountReached();

        } // End of 165955.4

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventPreUnlocked");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener#eventUnlocked(com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage)
     */
    protected void eventUnlocked(SIMPMessage msg) throws SIDiscriminatorSyntaxException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "eventUnlocked", msg);

        //  try to hand the message on to any ready consumer points
        //at this point it must be in both the IH and CD item/reference streams
        internalPut(msg, null, null, true, true, false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventUnlocked");
    }

    /**
     * Pause (suspend) all potential consumers of a message that has been
     * rolledback to delay their re-processing of said message.
     * 
     * @param msg
     * @param retryInterval
     */
    private void pauseConsumers(SIMPMessage msg, long retryInterval)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "pauseConsumers", new Object[] { msg, Long.valueOf(retryInterval) });

        // We need to lock the consumerPoints to check for consumers to pause but
        // we can't actually suspend them under that lock or we'll deadlock (465125)
        // so we build a new consumer list and process them outside the lock
        LinkedList<DispatchableKey> cachedConsumerPoints = new LinkedList<DispatchableKey>();
        synchronized (consumerPoints)
        {
            Iterator<DispatchableKey> itr = consumerPoints.iterator();
            while (itr.hasNext())
            {
                DispatchableKey cKey = itr.next();

                // If consumer is interested in this message, suspend it.
                if (((LocalQPConsumerKey) cKey).filterMatches((AbstractItem) msg))
                {
                    cachedConsumerPoints.add(cKey);
                }
            }
        } // sync

        // Now pause the consumers outside the lock
        Iterator<DispatchableKey> itr2 = cachedConsumerPoints.iterator();
        while (itr2.hasNext())
        {
            DispatchableKey cKey = itr2.next();

            DispatchableConsumerPoint consumerPoint =
                            cKey.getConsumerPoint();
            // If this consumer is already suspended then don't sent up a new alarm and
            // don't resuspend the consumer
            BlockedConsumerRetryHandler blockedConsumerRetryHandler = new BlockedConsumerRetryHandler(consumerPoint);
            if (blockedConsumerRetryHandler.startSuspend())
            {
                // Spin of a new alarm that will suspend the consumer immediately
                //  then wake up and resume the consumer
                _messageProcessor.getAlarmManager().
                                create(retryInterval, blockedConsumerRetryHandler);
            }
        }

        // Spin of a new alarm that will mark the itemStream as blocked
        // and unblock it in time with the consumers. This allows us to
        // display the state via the MBeans/panels using the QueueMessage
        // objects
        _messageProcessor.getAlarmManager().create(retryInterval,
                                                   new BlockedRetryHandler());

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "pauseConsumers");
    }

    /**
     * Method setReadyForUse.
     * <p>Sets the state of the ConsumerDispatcher to "READY_FOR_USE"</p>
     */
    @Override
    public void setReadyForUse()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setReadyForUse");

        state = SIMPState.READY_FOR_USE;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setReadyForUse");
    }

    /** Updates to state that the subscription is in the MatchSpace. */
    public void setIsInMatchSpace(boolean inMatchSpace)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setIsInMatchSpace", Boolean.valueOf(inMatchSpace));
        isInMatchSpace = inMatchSpace;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setIsInMatchSpace");
    }

    /**
     * Indicates whether a subscription associated with the ConsumerDispatcher has
     * been stored in the MatchSpace.
     * 
     * @return isInMatchSpace
     */
    public boolean isInMatchSpace()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isInMatchSpace");
            SibTr.exit(tc, "isInMatchSpace", Boolean.valueOf(isInMatchSpace));
        }

        return isInMatchSpace;
    }

    /**
     * Method isLocked.
     * 
     * @return boolean
     *         <p>Returns true if the ConsumerDispatchers state is "LOCKED"</p>
     */
    @Override
    public boolean isLocked()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isLocked");

        final boolean isLocked = (state == SIMPState.LOCKED);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isLocked", Boolean.valueOf(isLocked));

        return isLocked;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.OutputHandler#getTargetMEUuid()
     */
    @Override
    public SIBUuid8 getTargetMEUuid()
    {
        // We're a ConsumerDispatcher - return the local ME
        return _messageProcessor.getMessagingEngineUuid();
    }

    /**
     * Returns the subscription.
     * 
     * @return ReferenceStream
     */
    public ReferenceStream getReferenceStream()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getReferenceStream");
            SibTr.exit(tc, "getReferenceStream", subscriptionItemStream);
        }
        return subscriptionItemStream;
    }

    /**
     * Returns the p-to-p ItemStream.
     * 
     * @return ReferenceStream
     */
    public ItemStream getItemStream()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getItemStream");
            SibTr.exit(tc, "getItemStream", itemStream);
        }
        return itemStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.OutputHandler#commitInsert(com.ibm.ws.sib.processor.impl.store.MessageItem)
     */
    @Override
    public boolean commitInsert(MessageItem msg)
    {
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.OutputHandler#rollbackInsert(com.ibm.ws.sib.processor.impl.store.MessageItem)
     */
    @Override
    public boolean rollbackInsert(MessageItem msg)
    {
        return true;
    }

    /**
     * Method closeAllConsumersForDelete.
     * <p>Called when the local destination is being deleted, to close all attached
     * consumers.</p>
     */
    public void closeAllConsumersForDelete(DestinationHandler destinationBeingDeleted)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "closeAllConsumersForDelete", destinationBeingDeleted);

        Iterator<DispatchableKey> itr = null;
        Iterator<DispatchableKey> clonedItr = null;

        // The consumerPoints list is cloned for the notifyException
        // call.
        synchronized (consumerPoints)
        {
            //since the close of the underlying session causes the consumer point to
            //be removed from the list we have to take a clone of the list
            clonedItr = ((LinkedList<DispatchableKey>) consumerPoints.clone()).iterator();
            itr = ((LinkedList<DispatchableKey>) consumerPoints.clone()).iterator();
        }

        // Defect 360452
        // Iterate twice to avoid deadlock. First iteration to mark
        // as not ready. This ensure no messages on the destination
        // will arrive at the consumers.
        synchronized (_baseDestHandler.getReadyConsumerPointLock())
        {
            while (itr.hasNext())
            {
                DispatchableKey consumerKey = itr.next();

                // If we're making the consumer notReady then we must also remove
                // them from the list of ready consumers that we hold
                if (consumerKey.isKeyReady())
                    removeReadyConsumer(consumerKey.getParent(), consumerKey.isSpecific());

                consumerKey.markNotReady();
            }
        }

        // Second iteration to close sessions outside of readyConsumerPointLock
        // to avoid deadlock with receive.
        while (clonedItr.hasNext())
        {
            DispatchableKey consumerKey = clonedItr.next();
            consumerKey.getConsumerPoint().implicitClose(destinationBeingDeleted.getUuid(), null, _messageProcessor.getMessagingEngineUuid());
        }

        // Close any browsers
        closeBrowsersDestinationDeleted(destinationBeingDeleted);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "closeAllConsumersForDelete");
    }

    /**
     * Method closeAllConsumersForReceiveExclusive.
     * <p>Called when a destination is dynamically altered to be receive exclusive.
     * All consumers are thrown off the destination. One will be able to reconnect.</p>
     */
    public void closeAllConsumersForReceiveExclusive()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "closeAllConsumersForReceiveExclusive");

        Iterator<DispatchableKey> itr = null;
        Iterator<DispatchableKey> clonedItr = null;

        // The consumerPoints list is cloned for the notifyException
        // call.
        synchronized (consumerPoints)
        {
            //since the close of the underlying session causes the consumer point to
            //be removed from the list we have to take a clone of the list
            clonedItr = ((LinkedList<DispatchableKey>) consumerPoints.clone()).iterator();
            itr = ((LinkedList<DispatchableKey>) consumerPoints.clone()).iterator();
        }

        // Defect 360452
        // Iterate twice to avoid deadlock. First iteration to mark
        // as not ready. This ensure no messages on the destination
        // will arrive at the consumers.
        synchronized (_baseDestHandler.getReadyConsumerPointLock())
        {
            while (itr.hasNext())
            {
                DispatchableKey consumerKey = itr.next();

                // If we're making the consumer notReady then we must also remove
                // them from the list of ready consumers that we hold
                if (consumerKey.isKeyReady())
                    removeReadyConsumer(consumerKey.getParent(), consumerKey.isSpecific());

                consumerKey.markNotReady();
            }
        }

        // Second iteration to close sessions outside of readyConsumerPointLock
        // to avoid deadlock with receive.
        while (clonedItr.hasNext())
        {
            DispatchableKey consumerKey = clonedItr.next();
            consumerKey.getConsumerPoint().implicitClose(null, null, _messageProcessor.getMessagingEngineUuid());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "closeAllConsumersForReceiveExclusive");
    }

    /**
     * <p>Called when the localization is set to ReceiveAllowed-false, to notify all all attached & waiting
     * consumers.</p>
     * 
     * Should be only a single instance of the receive allowed thread at any one time.
     * 
     * @param isAllowed The requested Receive Allowed state
     * @param destinationHandler The destination which is being changed.
     */
    public void notifyReceiveAllowed(DestinationHandler destinationHandler)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "notifyReceiveAllowed",
                        new Object[] { destinationHandler });

        try
        {
            ReceiveAllowedThread receiveAllowedThread = getReceiveAllowedThread(destinationHandler);

            // If the returned value is null - it indicates that the receive allowed thread
            // was already running so we have just marked it to rerun again.
            // otherwise we need to start a new thread with the returned receiveAllowedThread
            // instance.
            if (receiveAllowedThread != null)
                _messageProcessor.startNewThread(receiveAllowedThread);
        } catch (InterruptedException e)
        {
            // No FFDC code needed
            SibTr.exception(tc, e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "notifyReceiveAllowed");
    }

    /**
     * Checks if there is a ReceiveAllowedThread already running. If there is it will mark the
     * thread to indicate that a rerun has been requested.
     * If not, a new ReceiveAllowedThread is created and returned.
     * 
     * @param destinationHandler
     */
    private synchronized ReceiveAllowedThread getReceiveAllowedThread(DestinationHandler destinationHandler)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getReceiveAllowedThread", destinationHandler);

        if (_receiveAllowedThread == null)
        {
            _receiveAllowedThread = new ReceiveAllowedThread(destinationHandler);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "getReceiveAllowedThread", _receiveAllowedThread);

            return _receiveAllowedThread;
        }

        _receiveAllowedThread.markForUpdate();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getReceiveAllowedThread", null);
        return null;
    }

    /**
     * Checks to see if another update has occured to the ReceiveAllowedState.
     * If it has it returns false to indicate that another run is required of the
     * thread, otherwise it deletes this instance of the receiveAllowedThread by setting
     * the reference to null
     */
    private synchronized boolean deleteReceiveAllowedThread()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deleteReceiveAllowedThread");

        if (_receiveAllowedThread.isMarkedForUpdate())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "deleteReceiveAllowedThread", Boolean.FALSE);
            return false;
        }

        _receiveAllowedThread = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteReceiveAllowedThread", Boolean.TRUE);
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener#registerForEventsPostAddItem(com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage)
     */
    public void registerForEventsPostAddItem(SIMPMessage msg)
    {}

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.OutputHandler#isWLMGuess()
     */
    @Override
    public boolean isWLMGuess()
    {
        return isGuess;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.OutputHandler#setWLMGuess(boolean)
     */
    @Override
    public void setWLMGuess(boolean guess)
    {
        this.isGuess = guess;
    }

    /**
     * <p>Determine if the subscription state object passed in is the same
     * object as the subscription state known in the consumer dispatcher.</p>
     * 
     * @param comparisionState
     * @return true if state is the same object, false otherwise
     */
    public boolean dispatcherStateEquals(ConsumerDispatcherState comparisionState)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "dispatcherStateEquals", comparisionState);

        boolean same = (dispatcherState == comparisionState);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "dispatcherStateEquals", Boolean.valueOf(same));

        return same;
    }

    /**
     * @return boolean true if this outputhandler's itemstream has reached QHighMessages
     */
    @Override
    public boolean isQHighLimit()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isQHighLimit");

        boolean limited = itemStream.isQHighLimit();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isQHighLimit", Boolean.valueOf(limited));

        return limited;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableSubscription#getSubscriptionUuid()
     */
    @Override
    public SIBUuid12 getSubscriptionUuid()
    {
        return subscriptionUuid;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableSubscription#getOutputhandler()
     */
    @Override
    public OutputHandler getOutputHandler()
    {
        return this;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#getControlAdapter()
     */
    @Override
    public ControlAdapter getControlAdapter()
    {
        return subscriptionControlAdaptor;
    }

    @Override
    public void dereferenceControlAdapter()
    {
        subscriptionControlAdaptor.dereferenceControllable();
        subscriptionControlAdaptor = null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#createControlAdapter()
     */
    @Override
    public void createControlAdapter()
    {
        if (_isPubSub)
        {
            subscriptionControlAdaptor = new LocalSubscriptionControl(this,
                            (SIMPTopicSpaceControllable) _baseDestHandler.getControlAdapter(),
                            _messageProcessor);
        }
        //else do nothing for pt-pt
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#registerControlAdapterAsMBean()
     */
    @Override
    public void registerControlAdapterAsMBean()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "registerControlAdapterAsMBean");
        LocalSubscriptionControl subscriptionControlAdaptor = (LocalSubscriptionControl) getControlAdapter();
        subscriptionControlAdaptor.registerControlAdapterAsMBean();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerControlAdapterAsMBean");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#deregisterControlAdapterMBean()
     */
    @Override
    public void deregisterControlAdapterMBean()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deregisterControlAdapterAsMBean");
        subscriptionControlAdaptor.dereferenceControllable();
        subscriptionControlAdaptor = null;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deregisterControlAdapterAsMBean");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableSubscription#isLocal()
     */
    @Override
    public boolean isLocal()
    {
        return true;
    }

    @Override
    public boolean isPubSub()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isPubSub");
            SibTr.exit(tc, "isPubSub", Boolean.valueOf(_isPubSub));
        }

        return _isPubSub;
    }

    /**
     * <p>This method checks that Receive Allowed state of the destination & localization.
     * 
     * @return true if destination is receive allowed
     */
    protected boolean isReceiveAllowed(DestinationHandler destinationHandler)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isReceiveAllowed", destinationHandler);

        boolean allowed = true;

        // Allow cleanup to receive messages
        if (!destinationHandler.isToBeDeleted())
        {
            if ((!destinationHandler.isReceiveAllowed()))
                allowed = false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isReceiveAllowed", Boolean.valueOf(allowed));
        return allowed;
    }

    /**
     * This class implements Runnable so that we can easily call it in a new Thread.
     * It's sole purpose is to notify consumers that a Receive Allowed state of
     * the localization has changed
     * 
     */
    private class ReceiveAllowedThread implements Runnable
    {
        private final DestinationHandler _destinationHandler;
        private boolean _rerunRequested = false;

        /**
         * Create a new AsynchThread for the given LCP against the given destination.
         */
        ReceiveAllowedThread(DestinationHandler dh)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(
                            tc,
                            "ReceiveAllowedThread",
                            new Object[] { dh });

            //store the variables
            _destinationHandler = dh;

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "ReceiveAllowedThread", this);
        }

        /**
         * Check to see if the receive allowed state has changed since the last run.
         * 
         * @return
         */
        public boolean isMarkedForUpdate()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "isMarkedForUpdate");

            boolean marked = _rerunRequested;
            _rerunRequested = false;

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "isMarkedForUpdate", Boolean.valueOf(marked));
            return marked;
        }

        /**
         * Used to indicate if the receiveAllowed state has changed since
         * the last time it was run.
         */
        public void markForUpdate()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "markForUpdate");

            _rerunRequested = true;

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "markForUpdate");
        }

        /**
         * The ReceiveAllowedThread cycles round the list of Consumers and notifies each of
         * a receive allowed change.
         * 
         * The change happens asynchronously so checks at the end of a loop to see if the receive
         * allowed value has changed again.
         */
        @Override
        public void run()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "run", this);

            boolean run = true;

            // While we can still run and the receiveAllowed state has changed
            while (run)
            {
                boolean newReceiveAllowed = isReceiveAllowed(_destinationHandler);

                synchronized (consumerPoints)
                {
                    // Ensure every transition produces at least one notification for new consumers
                    if (newReceiveAllowed == currentReceiveAllowed)
                        currentReceiveAllowed = !newReceiveAllowed;
                }

                while (newReceiveAllowed != currentReceiveAllowed)
                {
                    Iterator<DispatchableKey> consumerPointIterator = null;

                    // Small synchronization to stop deadlocks on the notifyReceiveAllowed call.
                    // All connection applications will pick up the receive allowed changes
                    synchronized (consumerPoints)
                    {
                        consumerPointIterator = ((List<DispatchableKey>) consumerPoints.clone()).iterator();
                    }

                    while (consumerPointIterator.hasNext())
                    {
                        DispatchableKey consumerKey = consumerPointIterator.next();
                        consumerKey.notifyReceiveAllowed(newReceiveAllowed, _destinationHandler);
                    }

                    synchronized (consumerPoints)
                    {
                        // All done so update current state, then pick up any subsequent request
                        currentReceiveAllowed = newReceiveAllowed;
                        newReceiveAllowed = isReceiveAllowed(_destinationHandler);
                    }
                }

                // Update the run flag - true indicates that the thread has been deleted
                // so we need to ! the result
                run = !deleteReceiveAllowedThread();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "run");
        }
    } // End class ReceiveAllowedThread

    /**
     * Class to handle the retry of messages to the exception destination after
     * the exception destination cannot be delivered to.
     */
    private class ExceptionDestinationRetryHandler implements AlarmListener
    {
        private final SIMPMessage msg;
        private final ConsumerDispatcher cd;
        private int wait_time = SIMPConstants.EXCEPTION_RETRY_TIMEOUT;

        public ExceptionDestinationRetryHandler(SIMPMessage msg, ConsumerDispatcher cd)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "ExceptionDestinationRetryHandler", new Object[] { msg, cd });

            this.msg = msg;
            this.cd = cd;

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "ExceptionDestinationRetryHandler", this);
        }

        @Override
        public void alarm(Object thandle)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "alarm", thandle);
            /*
             * Message has been delivered a greater number of times than allowed.
             * Route message on to the exception destination associated with this
             * destination.
             */

            // Deregister rollback callbacks to avoid recursive event callbacks
            msg.deregisterMessageEventListener(MessageEvents.POST_ROLLBACK_REMOVE, cd);
            msg.deregisterMessageEventListener(MessageEvents.POST_ROLLBACK_ADD, cd);

            UndeliverableReturnCode rc = null;
            try
            {
                rc = _baseDestHandler.handleUndeliverableMessage(
                                                                 msg,
                                                                 SIRCConstants.SIRC0035_BACKOUT_THRESHOLD_ERROR,
                                                                 new String[] {
                                                                               _baseDestHandler.getName(),
                                                                               _messageProcessor.getMessagingEngineName() },
                                                                 null);
            } catch (SIResourceException e)
            {
                // No FFDC code needed
                // Exception had already been handled
            }

            // re-register the callbacks
            msg.registerMessageEventListener(MessageEvents.POST_ROLLBACK_REMOVE, cd);
            msg.registerMessageEventListener(MessageEvents.POST_ROLLBACK_ADD, cd);

            // If an exception was thrown or ERROR returned, then reset the alarm
            if (rc == null || rc == UndeliverableReturnCode.ERROR)
            {
                // Spin of a new alarm
                _messageProcessor.getAlarmManager().
                                create(wait_time, this);

                wait_time *= 2;
                if (wait_time > SIMPConstants.MAX_EXCEPTION_RETRY_TIMEOUT)
                    wait_time = SIMPConstants.MAX_EXCEPTION_RETRY_TIMEOUT;
            }

            //If BLOCK then unlock the message to  make it available on the queue again
            if (rc == UndeliverableReturnCode.BLOCK)
            {
                try
                {
                    if (!msg.isPersistentlyLocked())
                        msg.unlockMsg(msg.getLockID(), null, true);
                } catch (MessageStoreException e)
                {
                    // MessageStoreException shouldn't occur so FFDC.
                    FFDCFilter.processException(
                                                e,
                                                "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.ExceptionDestinationRetryHandler.alarm",
                                                "1:3523:1.280.5.25",
                                                this);

                    SibTr.exception(tc, e);
                    SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                new Object[] {
                                              "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.ExceptionDestinationRetryHandler",
                                              "1:3530:1.280.5.25",
                                              e });
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "alarm", rc);
        }

    }

    /**
     * Class to handle the suspension and resuming of a consumer when the retry count
     * has been reached but no exception destination is available.
     */
    private static class BlockedConsumerRetryHandler implements AlarmListener
    {
        private final DispatchableConsumerPoint cp;

        public BlockedConsumerRetryHandler(DispatchableConsumerPoint cp)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "BlockedConsumerRetryHandler", cp);

            this.cp = cp;

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "BlockedConsumerRetryHandler", this);
        }

        /*
         * Will atempt to suspend the consumer. If the suspend call returns false
         * then this means the consumer wasn't suspended as it is already suspended so
         * the alarm should NOT be created for this handler.
         */
        public boolean startSuspend()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "startSuspend");
            // Suspend the consumer
            boolean suspended = cp.suspendConsumer(DispatchableConsumerPoint.SUSPEND_FLAG_RETRY_TIMER);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "startSuspend", suspended);
            return suspended;
        }

        @Override
        public void alarm(Object thandle)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "alarm", this);

            // Resume the consumer
            cp.resumeConsumer(DispatchableConsumerPoint.SUSPEND_FLAG_RETRY_TIMER);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "alarm");
        }

    }

    /**
     * Class to handle the suspension and resuming of a consumer when the retry count
     * has been reached but no exception destination is available.
     */
    private class BlockedRetryHandler implements AlarmListener
    {
        public BlockedRetryHandler()
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "BlockedRetryHandler");

            // Mark the itemStream as blocked
            if (itemStream != null)
                itemStream.setBlocked(true);
            else if (subscriptionItemStream != null)
                subscriptionItemStream.setBlocked(true);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "BlockedRetryHandler", this);
        }

        @Override
        public void alarm(Object thandle)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.entry(tc, "BlockedRetryHandler.alarm", thandle);

            // Mark the itemStream as not blocked
            if (itemStream != null)
                itemStream.setBlocked(false);
            else if (subscriptionItemStream != null)
                subscriptionItemStream.setBlocked(false);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "BlockedRetryHandler.alarm");
        }
    }

    /**
     * @return Returns the mpSubscription.
     */

    public MPSubscription getMPSubscription()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getMPSubscription");
        if (mpSubscription == null)
        {
            // Only create this if this consumer dispatcher is
            // for a durable subscription
            if (dispatcherState.isDurable())
            {
                mpSubscription = new MPSubscriptionImpl(this, _messageProcessor);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getMPSubscription", mpSubscription);
        return mpSubscription;
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
            SibTr.entry(tc, "afterCompletion", new Object[] { transaction, Boolean.valueOf(committed) });

        // Reset the current transaction now that this one has completed
        synchronized (orderLock)
        {
            // If asynch and a rollback occurred, unlock all messages
            // in the LME
            try
            {
                if (!committed && currentLME != null)
                    currentLME.unlockAll();
            } catch (SIResourceException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.afterCompletion",
                                            "1:3673:1.280.5.25",
                                            this);

                SibTr.exception(tc, e);
                SibTr.error(tc, "ORDERED_MESSAGING_ERROR_CWSIP0117",
                            new Object[] {
                                          _baseDestHandler.getName(),
                                          _messageProcessor.getMessagingEngineName() });

            } catch (SISessionDroppedException e)
            {
                // FFDC
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.afterCompletion",
                                            "1:3689:1.280.5.25",
                                            this);

                SibTr.exception(tc, e);
                SibTr.error(tc, "ORDERED_MESSAGING_ERROR_CWSIP0117",
                            new Object[] {
                                          _baseDestHandler.getName(),
                                          _messageProcessor.getMessagingEngineName() });

            } catch (SIMPMessageNotLockedException e)
            {
                // No FFDC code needed
                // This exception has occurred beause someone has deleted the
                // message(s). Ignore this exception as it is unlocked anyway
            }

            currentTran = null;
            currentLME = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "afterCompletion");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#beforeCompletion(com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    @Override
    public void beforeCompletion(TransactionCommon transaction) {
        // no-op
    }

    /**
     * @param transaction
     * @return
     */
    @Override
    public boolean isNewTransactionAllowed(TransactionCommon transaction)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isNewTransactionAllowed", transaction);

        boolean allowed = true;

        // Always allow non-durable subscribers to receive
        if (isPubSub() && !isDurable())
            allowed = true;
        else
        {
            synchronized (orderLock)
            {
                if (streamHasInDoubtRemoves)
                {
                    if (itemStream != null)
                        currentTran = itemStream.getOrderedActiveTran();
                    else if (subscriptionItemStream != null)
                        currentTran = subscriptionItemStream.getOrderedActiveTran();

                    if (currentTran == null)
                        streamHasInDoubtRemoves = false;
                }

                // If the current tran is null or the same as the tran provided - disallow new trans
                if (currentTran != null &&
                    (transaction == null || !currentTran.equals(transaction.getPersistentTranId())))
                    allowed = false;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isNewTransactionAllowed", Boolean.valueOf(allowed));

        return allowed;
    }

    /**
     * @param transaction
     */
    @Override
    public void setCurrentTransaction(TransactionCommon transaction, JSLockedMessageEnumeration lme) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setCurrentTransaction", transaction);

        // Set the current active transaction to this one and register for callbacks
        if (currentTran == null)
        {
            currentTran = transaction.getPersistentTranId();
            currentLME = lme;
            transaction.registerCallback(this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setCurrentTransaction");
    }

    /**
   *
   */
    @Override
    public void checkInitialIndoubts(DispatchableConsumerPoint consumer) throws SIResourceException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "checkInitialIndoubts");

        // Build the activeTransactionSet if possible
        if (!_isPubSub && !consumer.ignoreInitialIndoubts())
        {
            synchronized (orderLock)
            {
                // Intitialise the transaction set for semi-ordered consumers
                if (initialiseTransactionSet())
                {
                    // transactions were found (i.e. msgs were on the itemstream in removing state)
                    // Therefore we suspend the consumer until they all complete
                    consumer.suspendConsumer(DispatchableConsumerPoint.SUSPEND_FLAG_INITIAL_INDOUBTS);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "checkInitialIndoubts");
    }

    /**
     * Search the MatchSpace for a specific consumer
     * 
     * @param msg
     * @param searchResults
     * @return
     * @throws SIDiscriminatorSyntaxException
     */
    private void searchMatchSpace(
                                  SIMPMessage msg,
                                  MessageProcessorSearchResults searchResults)
                    throws SIDiscriminatorSyntaxException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "searchMatchSpace", new Object[] { msg, searchResults });

        // Defect 382250, set the unlockCount from MsgStore into the message
        // in the case where the message is being redelivered.
        JsMessage searchMsg = msg.getMessage();
        int redelCount = msg.guessRedeliveredCount();

        if (redelCount > 0)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Set deliverycount into message: " + redelCount);
            searchMsg.setDeliveryCount(redelCount);
        }

        // Search the match space.
        _messageProcessor
                        .getMessageProcessorMatching()
                        .retrieveMatchingConsumerPoints(
                                                        _baseDestHandler.getUuid(), // the uuid of the destination
                                                        getUuid(), // the uuid of the ConsumerDispatcher
                                                        searchMsg,
                                                        searchResults);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "searchMatchSpace", searchResults);
    }

    @Override
    public SIMPMessage getMessageByValue(AOValue value)
                    throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getMessageByValue", Long.valueOf(value.getMsgId()));

        SIMPMessage msgItem = null;

        try
        {
            if (value.isRestored())
                msgItem = (SIMPMessage) (itemStream == null ? subscriptionItemStream.findById(value.getMsgId()) :
                                itemStream.findById(value.getMsgId()));
        } catch (MessageStoreException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.ConsumerDispatcher.getMessageByValue",
                                        "1:4101:1.280.5.25",
                                        this);

            SibTr.exception(tc, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "getMessageByValue", e);
            throw new SIResourceException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getMessageByValue", msgItem);
        return msgItem;
    }

    @Override
    public void setCurrentTransaction(SIMPMessage msg, boolean isInDoubtOnRemoteConsumer)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setCurrentTransaction", new Object[] { msg, Boolean.valueOf(isInDoubtOnRemoteConsumer) });

        if (itemStream == null)
            subscriptionItemStream.setCurrentTransaction(msg, isInDoubtOnRemoteConsumer);
        else
            itemStream.setCurrentTransaction(msg, isInDoubtOnRemoteConsumer);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setCurrentTransaction");
    }
}
