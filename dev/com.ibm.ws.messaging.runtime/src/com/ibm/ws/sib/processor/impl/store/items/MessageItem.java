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
package com.ibm.ws.sib.processor.impl.store.items;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.SIRCConstants;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsApiMessage;
import com.ibm.ws.sib.mfp.JsJmsTextMessage;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.mfp.MessageEncodeFailedException;
import com.ibm.ws.sib.mfp.MessageRestoreFailedException;
import com.ibm.ws.sib.mfp.impl.JsMessageFactory;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.MessageStoreRuntimeException;
import com.ibm.ws.sib.msgstore.NotInMessageStore;
import com.ibm.ws.sib.msgstore.PersistentDataEncodingException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.MessageStoreConstants.MaximumAllowedDeliveryDelayAction;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.exceptions.SIMPErrorException;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.MessageProcessor;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.MessageEvents;
import com.ibm.ws.sib.processor.impl.store.itemstreams.MQLinkMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PtoPXmitMsgsItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PubSubMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream;
import com.ibm.ws.sib.processor.matching.MessageProcessorSearchResults;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.runtime.impl.MQLinkQueuedMessage;
import com.ibm.ws.sib.processor.runtime.impl.QueuedMessage;
import com.ibm.ws.sib.processor.utils.SIMPUtils;
import com.ibm.ws.sib.processor.utils.UserTrace;
import com.ibm.ws.sib.transactions.TransactionCallback;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.SIBUuid12;
import com.ibm.ws.sib.utils.SIBUuid8;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A wrapper to SIBusMessage to allow it to be stored in the message store.
 * 
 * @author tevans
 */
public class MessageItem extends Item implements SIMPMessage, TransactionCallback
{

    // NLS for component
    private static final TraceNLS nls_mt =
                    TraceNLS.getTraceNLS(SIMPConstants.TRACE_MESSAGE_RESOURCE_BUNDLE);

    // NLS for component
    private static final TraceNLS nls =
                    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

    //trace
    private static final TraceComponent tc =
                    SibTr.register(
                                   MessageItem.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    private ControlAdapter controlAdapter;

    private int producerSeed = 0;

    // If this is an Item that represents a pubsub message there is only a
    // need to persist (STORE_ALWAYS) the message if there are any references
    // to it from durable subscriptions.
    // -1 : No references (pt-to-pt or no subscriptions)
    //  1 : Durable references
    //  0 : only non-durable references
    private int maintainPersistence = -1;

    /**
     * A flag to indicate whether we guessed the
     * the stream to use for this message
     */
    private boolean streamIsGuess = false;

    /**
     * Record the pubsub fan out of this message so that stats can use the figure
     * when the publish commits. Not used for point to point.
     */
    private int fanOut;

    /**
     * The following message attributes are cached for performance reasons
     */
    private Reliability msgReliability;
    private int msgPriority = -2;
    private SIBUuid12 guaranteedStreamUuid;
    private long currentMEArrivalTimeStamp;
    private long messageWaitTime = -1;
    /**
     * The time that the message has spent in the bus prior to arrival at this ME.
     */
    private long preMEArrivalWaitTime = 0;
    private boolean arrivalTimeStored = true;
    private String debugName;

    private String _busName;

    /** Used for message classification by XD */
    private String messageControlClassification = null;

    /**
     * Used to show whether a value for messageControlClassification has been
     * retrieved from the JsMessage or not. We cannot simply test whether
     * messageControlClassification is null, or not, as the value might be null where XD is
     * registered but is not classifying messages. In this latter case we'd repeatedly
     * attempt to retrieve the value from the JsMessage
     */
    private boolean retrievedMessageControlClassification = false;

    /**
     * Indicates that restore has been called on this MessageItem but we failed
     * to fully initilise (vai initiliaseNonPersitent) because the MP had not
     * been started yet. This is likely to occur when this Item is classed as
     * indoubt when the ME starts. If this property is true then we need to
     * recall the initiliseNonPersistent again when we get an event callback
     */
    private boolean failedInitInRestore = false;

    /**
     * Status to indicate whether event listeners have to be registered for
     * the POST COMMIT , ROLLBACK events in MessageItemReference
     */
    private boolean registerEvents = false;

    private SIMPItemStream itemstream;

    //The OutputHandler to which this message belongs
    //private OutputHandler outputHandler;
    //The InputHandler to which this message belongs
    //private InputHandler inputHandler;

    private MessageEventListener PRE_COMMIT_ADD;
    private MessageEventListener PRE_COMMIT_REMOVE;
    private MessageEventListener POST_COMMIT_ADD_1;
    private MessageEventListener POST_COMMIT_ADD_2;
    private MessageEventListener POST_COMMIT_REMOVE_1;
    private MessageEventListener POST_COMMIT_REMOVE_2;
    private MessageEventListener POST_COMMIT_REMOVE_3;
    private MessageEventListener POST_COMMIT_REMOVE_4;
    private MessageEventListener POST_COMMIT_REMOVE_5;
    private MessageEventListener POST_ROLLBACK_ADD_1;
    private MessageEventListener POST_ROLLBACK_ADD_2;
    private MessageEventListener POST_ROLLBACK_REMOVE_1;
    private MessageEventListener POST_ROLLBACK_REMOVE_2;
    private MessageEventListener POST_ROLLBACK_REMOVE_3;
    private MessageEventListener POST_ROLLBACK_REMOVE_4;
    private MessageEventListener POST_ROLLBACK_REMOVE_5;
    private MessageEventListener UNLOCKED_1;
    private MessageEventListener UNLOCKED_2;
    private MessageEventListener UNLOCKED_3;
    private MessageEventListener PRE_UNLOCKED_1;
    private MessageEventListener PRE_UNLOCKED_2;
    private MessageEventListener REFERENCES_DROPPED_TO_ZERO;
    private MessageEventListener PRE_PREPARE_TRANSACTION;
    private MessageEventListener POST_COMMITTED_TRANSACTION;
    private MessageEventListener EXPIRY_NOTIFICATION;
    private MessageEventListener COD_CALLBACK;

    //The actual message
    private JsMessage msg;

    //The soft reference to the msg. This will be present if we have restored the message
    // after we sucesfully released the message.
    private SoftReference<JsMessage> softReferenceMsg;

    //Is this message persistent?
    //private boolean persistent;
    //Was this message 'put' transactionally?
    private boolean transacted = false;
    //The maximum storage strategy possible
    private int maxStorageStrategy = STORE_ALWAYS;

    // Indicates whether a new message Id is required
    private boolean requiresNewId = false;

    //Store the message at send time rather than pre-prepare?
    //Only implemented for pt-to-pt
    private boolean storeAtSendTime = false;

    //Indicates whether the message came in from a remoteME
    //Used for p-to-p so that when a message is re-routed to
    //a different InputHandler on the localME we can still tell
    //that it orginally came from a remote ME.
    //In the pub sub case we use it to ensure that the message is not
    //sent to upstream MEs.
    private boolean fromRemoteME = false;

    //Indicates whether the message came in from a remote Bus
    //Used for p-to-p so that when a message is re-routed to
    //a different InputHandler on the localME we can still tell
    //that it orginally came from a remote Bus. This is necessary
    //for security checking
    //Is used by pub sub to make sure that messages published from
    //a remote bus are properly distributed on the local bus.
    private boolean fromRemoteBus = false;

    /**
     * Used for durable subs noLocal matching.
     * Doesn't need to be spilled as no local matching is done early.
     */
    private SIBUuid12 producerConnectionUuid = null;

    // List of all matchingConsumers for this message
    private MessageProcessorSearchResults searchResults;

    /**
     * The latest measured period of time this message spent in the Messaging
     * Engine. This is used by the expiry and statistics implementations.
     */
    private long latestWaitTimeUpdate;

    /**
     * The cached value of the report COD from the original message
     */
    private Byte _reportCOD;

    /**
     * Indicates that the reportCOD local copy has been set.
     */
    private boolean _reportCODSet = false;

    /**
     * Indicates that the guaranteedStreamUuidSet local copy has been set.
     */
    private boolean guaranteedStreamUuidSet = false;

    /**
     * Indicates if the message is currently being re-driven through the
     * system after becoming reavailable (after an unlock)
     */
    private boolean reavailable = false;

    /**
     * Indicates this message should be delivered irrespective of queue high
     * limits or if the destination localization is send allowed.
     * See defects 244425 & 244425.1
     */
    private boolean forcePut = false;

    /**
     * Number of times this message has been redirected to another
     * destination in a forward routing path. If this value goes beyond
     * the maximum frp depth then it will sent to the exception
     * destination.
     */
    private int redirectCount = 0;

    /**
     * Set if redelivery count reached and we therefore dont require
     * the message to be unlocked.
     */
    private boolean redeliveryCountReached = false;

    /**
     * Does the sender of this message prefer the local message point
     * (if it exists)?
     */
    private boolean preferLocal = true;

    /**
     * Indicate that we only want to restore the jsmessage
     */
    private boolean restoringJsMessageOnly = false;

    private boolean hiddenMessage;

    /**
     * If the message is hidden, this variable determines when should it be revealed.
     */
    private long hiddenExpiryTime = 0;

    private SIBUuid8 localisingMEUuid;

    // We add this to the lock unlockCount to determine the actual redeliveryCount
    private long rmeUnlockCount = 0;

    // This is the value we will be adding to the rmeUnlockCount if the unlock completes
    private long uncommittedRMEUnlockCount;

    // Certain fields in a JsMessage form part of a 'set', and one can't be set without
    // the other at the time of a serialisation of the message. Therefore, if there's
    // a risk of a serialization occurring while setting such field we need to hold a lock
    // (this only happens if the message is already in the MessageStore at the time that we're
    // updating JsMEssage fields, which is only when we add properties for a link message - I
    // think!)
    private final Object messageSyncUpdateLock = new Object();

    /**
     * Empty constructor to allow recreation by the message store after it is
     * persisted. The message data is set via the itemRestore method.
     * public as driven by MessageStore
     */
    public MessageItem()
    {
        super();
    }

    /**
     * The normal constructor to create a new MessageItem to wrapper a new message.
     * public method as driven by MessageStore
     * 
     * @param msg The message to be wrappered
     */
    public MessageItem(JsMessage msg)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "MessageItem", msg);
        //store the msg data
        this.msg = msg;

        //defect 260440: we set the originating bus in the msg so we can tell
        //whether it came in over a link and, if so, which link.
        // If we published, then this field will be our bus and the message will go
        // down to other links.
        // If another link published, we will publish it to all buses other than
        // that which originated the message.
        setOriginatingBus(msg.getBus());

        //record if it should be persistent
        //this.persistent = persistent;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "MessageItem", this);
    }

    /**
     * The normal constructor to create a new MessageItem to wrapper a new message.
     * 
     * @param msg The message to be wrappered
     * @param waitTime The message wait time to use as a starting point
     * */
    public MessageItem(JsMessage msg, long waitTime)
    {
        this(msg);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "MessageItem", new Object[] { msg, Long.valueOf(waitTime) });

        //Set the starting messageWaitTime.  The thinking here is that its slow
        //to get this value out of the message, so in the cases where we know it
        //must be 0, we can avoid getting the value.
        messageWaitTime = waitTime;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "MessageItem", this);
    }

    /**
     * Get the underlying message object
     * 
     * @return The underlying message object
     */
    @Override
    public JsMessage getMessage()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMessage");

        JsMessage localMsg = getJSMessage(true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMessage", localMsg);

        return localMsg;
    }

    /**
     * Get the underlying message object,only if the message is
     * available in the message store
     * 
     * @return The underlying message object
     */
    @Override
    public JsMessage getMessageIfAvailable()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMessageIfAvailable");

        //If message is not available in the message store do not throw exception
        boolean throwExceptionIfNotAvailable = false;
        JsMessage localMsg = getJSMessage(throwExceptionIfNotAvailable);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMessageIfAvailable", localMsg);

        return localMsg;
    }

    /**
     * Get the underlying message object
     * 
     * @param throwExceptionIfNotAvailable Boolean to indicate whether exception
     *            has to be thrown if message not available
     * @return The underlying message object
     */
    private JsMessage getJSMessage(boolean throwExceptionIfNotAvailable)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getJSMessage", Boolean.valueOf(throwExceptionIfNotAvailable));

        JsMessage localMsg = msg;
        if (localMsg == null)
        {
            if (softReferenceMsg != null)
            {
                localMsg = softReferenceMsg.get(); //Get the referred object
            }

            if (localMsg == null)
            {
                synchronized (this)
                {
                    restoreJsMessageFromMsgstore(throwExceptionIfNotAvailable);
                    localMsg = msg;
                    // It is possible that restoreJsMessageFromMsgstore restored no message and didn't
                    // throw an exception. In this case don't create the SoftReference.
                    if (localMsg != null)
                    {
                        //Assign the msg to the soft reference, this is to ensure that we
                        // don't keep a hard reference to the msg in memory after we have restored 
                        softReferenceMsg = new SoftReference<JsMessage>(msg);
                        //Remove the hard reference.
                        msg = null;
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getJSMessage", localMsg);
        return localMsg;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.AbstractItem#getStorageStrategy()
     */
    @Override
    public int getStorageStrategy()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getStorageStrategy");

        int storageStrategy;

        Reliability rel = getReliability();
        if (rel == Reliability.BEST_EFFORT_NONPERSISTENT)
        {
            storageStrategy = STORE_NEVER;
        }
        else
        {
            // If the message has a reliability of Assured the storage strategy is
            // STORE_ALWAYS except when this message is for pubsub and only has
            // non-durable references. In this case the references will be
            // STORE_MAYBE as there is no need to make the message more robust.
            if ((maintainPersistence != 0)
                && (rel == Reliability.RELIABLE_PERSISTENT))
            {
                storageStrategy = STORE_EVENTUALLY;
            }
            else if ((maintainPersistence != 0)
                     && (rel == Reliability.ASSURED_PERSISTENT))
            {
                storageStrategy = STORE_ALWAYS;
            }
            else
            {
                storageStrategy = STORE_MAYBE;
            }
        }

        // ensure value does not exceed maximum permitted, eg not
        // possible to store assured on a temporary destination
        storageStrategy =
                        storageStrategy < maxStorageStrategy
                                        ? storageStrategy
                                        : maxStorageStrategy;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getStorageStrategy", Integer.valueOf(storageStrategy));

        return storageStrategy;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.AbstractItem#restore(byte[])
     */
    // Feature SIB0112b.mp.1
    @Override
    public void restore(final List<DataSlice> dataSlices)
                    throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "restore", dataSlices);

        restoreInternal(dataSlices, true);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "restore");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.AbstractItem#restoreIfMsgAvailable(byte[])
     */
    // 668676.1
    @Override
    protected void restoreIfMsgAvailable(final List<DataSlice> dataSlices)
                    throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "restoreIfMsgAvailable", dataSlices);

        restoreInternal(dataSlices, false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "restoreIfMsgAvailable");
    }

    /*
     * Restore the state of the receiver from the data that was persisted
     * 
     * @param dataSlices - the data that was persisted on behalf of the item.
     * 
     * @param throwExceptionIfNotAvailable Boolean to indicate whether exception
     * has to be thrown if message not available
     * Throws SevereMessageStoreException if the restore failed
     */
    public void restoreInternal(final List<DataSlice> dataSlices, boolean throwExceptionIfMsgNotAvailable)
                    throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "restoreInternal", new Object[] { dataSlices, Boolean.valueOf(throwExceptionIfMsgNotAvailable) });

        try
        {
            //get a new message instance
            JsMessageFactory jmf = JsMessageFactory.getInstance();

            // Defect 430302
            // We need to pass a clone of the list as MFP may modify
            // the list before passing it to Comms. We won't assume that the
            // List is implemented as an ArrayList so we create our own now
            ArrayList arraylistDataSlices = new ArrayList(dataSlices);
            msg = jmf.restoreJsMessage(arraylistDataSlices, getOwningMessageStore());
            //Updating Redelivery count to MFP as dataSlices do not have it.
            if (msg != null)
                msg.setRedeliveredCount(guessRedeliveredCount());

            itemstream = (SIMPItemStream) getItemStream();

            if (!restoringJsMessageOnly)
                initialiseNonPersistent(true);

        } catch (Exception e)
        {
            // No FFDC code needed
            // set msg as null when specified not to throw Exception when message not available
            if ((e instanceof NotInMessageStore) && !throwExceptionIfMsgNotAvailable)
            {
                msg = null;
            }
            else
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.store.items.MessageItem.restoreInternal",
                                            "1:677:1.244.1.40",
                                            this);

                SibTr.exception(tc, e);

                // PK54812 If the exception represents a damaged message item, mark the
                // destination as invalid
                if (e instanceof MessageRestoreFailedException) {
                    SIMPItemStream stream = (SIMPItemStream) getItemStream();
                    DestinationHandler destHandler = getDestinationHandler(false, stream);
                    destHandler.setCorrupt(true);
                }

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "restoreInternal", e);

                // The error message is already NLS'd
                throw new MessageStoreRuntimeException(e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "restoreInternal");
    }

    BaseDestinationHandler getDestinationHandler(boolean registerForEvents,
                                                 SIMPItemStream itemstream)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDestinationHandler",
                        new Object[] { Boolean.valueOf(registerForEvents), itemstream });

        BaseDestinationHandler destinationHandler = null;

        if (itemstream instanceof PubSubMessageItemStream)
        {
            PubSubMessageItemStream mis = (PubSubMessageItemStream) itemstream;
            destinationHandler = mis.getDestinationHandler();
        }
        else
        {
            PtoPMessageItemStream localisation = (PtoPMessageItemStream) itemstream;
            destinationHandler = localisation.getDestinationHandler();

            if (registerForEvents)
            {
                MessageEventListener listener = (MessageEventListener) localisation.getOutputHandler();
                if (listener != null)
                {
                    listener.registerForEvents(this);
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getDestinationHandler", destinationHandler);

        return destinationHandler;
    }

    public void initialiseNonPersistent(boolean registerForEvents) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "initialiseNonPersistent");

        // Get the destination handler and register for events.
        BaseDestinationHandler destinationHandler =
                        getDestinationHandler(registerForEvents, itemstream);

        if (destinationHandler != null)
        {
            // Although this is always likelt to be true in this method. it is
            //  still safe to have the check for the future.
            JsMessage localMsg = getJSMessage(true);

            // Register for the post add items.
            destinationHandler.registerForEvents(this);

            if (localMsg.getReportCOD() != null)
                registerMessageEventListener(MessageEvents.COD_CALLBACK, destinationHandler);

            //Store the destination name used for debug trace
            debugName = destinationHandler.getName();

            //defect 260440
            //we also need to re-initialise the originating bus to avoid sending
            //this to busses that have already seen it.
            //This value should not have been changed between it arriving on
            //this ME and it being stored i.e. should only equal localBus if the message
            //was actually produced on this bus in the first place.
            String originalBus = localMsg.getBus();
            setOriginatingBus(originalBus);
            MessageProcessor mp = destinationHandler.getMessageProcessor();
            if (originalBus != null && !originalBus.equals(mp.getMessagingEngineBus()))
            {
                //we are from a remote bus
                fromRemoteBus = true;
            }
            //we need to know if this message came from a remote ME
            //so that we can properly distribute it to other MEs.
            SIBUuid8 srcME = localMsg.getGuaranteedSourceMessagingEngineUUID();
            if (srcME != null && !mp.getMessagingEngineUuid().equals(srcME))
            {
                //we are from a remote ME
                fromRemoteME = true;
            }
            failedInitInRestore = false; //Set to false so we don't call it again
        }
        else
        {
            //we are not yet restored due to an indoubt tran.
            //We have to mark that we failed to initialise and do
            //it later when we get a callback
            failedInitInRestore = true;
        }

        // For ordered messaging, if the message is in doubt at startup due to a remove,
        // set the current transaction id to be the one associated with the message
        if (isRemoving()) // TODO : && isOrdered()
            itemstream.setCurrentTransaction(this, false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "initialiseNonPersistent");

    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.AbstractItem#getPersistentData()
     * 
     * We should always be assured that we have the msg at this point as
     * we shouldn't have released the JsMessage until it has been persisted
     * to the msgstore, at which time msgstore shouldn't call getPersistentData on us
     */
    // Feature SIB0112b.mp.1
    @Override
    public List<DataSlice> getPersistentData() throws PersistentDataEncodingException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getPersistentData");

        //Late store of the currentMEArrivalTimestamp.  If the message is never stored
        //in the message store, then there's no point taking the hit of storing the
        //timestamp in the message
        if (!arrivalTimeStored)
        {
            msg.setCurrentMEArrivalTimestamp(currentMEArrivalTimeStamp);
            arrivalTimeStored = true;
        }
        if (producerConnectionUuid != null)
        {
            //see defect 278038
            //Late store of the currentMEArrivalTimestamp.  If the message is never stored
            //in the message store, then there's no point taking the hit
            msg.setConnectionUuid(producerConnectionUuid);
        }

        List<DataSlice> data = null;

        try
        {
            // Prevent any concurrent JsMessage property changes that may cause
            // a serialization error
            synchronized (messageSyncUpdateLock)
            {
                //get the message's data in a list of DataSlice objects
                data = msg.flatten(getOwningMessageStore());
            }
        } catch (MessageEncodeFailedException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.items.MessageItem.getPersistentData",
                                        "1:861:1.244.1.40",
                                        this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "MESSAGE_CORRUPT_ERROR_CWSIP0262",
                        new Object[] {
                        SIMPUtils.getStackTrace(e)
                        });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getPersistentData", e);

            throw new PersistentDataEncodingException(
                            nls.getFormattedMessage(
                                                    "MESSAGE_CORRUPT_ERROR_CWSIP0262",
                                                    new Object[] {
                                                    e },
                                                    null),
                            e);
        } catch (MessageStoreException e)
        {
            // No FFDC code needed
            // We dont generate an FFDC here as this is sometimes acceptable behaviour - d542075

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getPersistentData", e);

            throw new PersistentDataEncodingException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getPersistentData");

        return data;
    }

    /**
     * Returns an estimated in memory size for the Message that we have.
     */
    @Override
    public int getInMemoryDataSize()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getInMemoryDataSize");

        JsMessage localMsg = getJSMessage(true);
        int msgSize = localMsg.getInMemorySize();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getInMemoryDataSize", Integer.valueOf(msgSize));

        return msgSize;
    }

    /**
     * Get the message's Reliability
     * 
     * @return the message's Reliability
     */
    @Override
    public Reliability getReliability()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getReliability");

        if (msgReliability == null)
        {
            JsMessage localMsg = getJSMessage(true);
            msgReliability = localMsg.getReliability();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getReliability", msgReliability);

        return msgReliability;
    }

    /**
     * Sets both the cached version of the reliability and the
     * reliability in the underlying message.
     * 
     * Called by the AbstractInputHandler to update the message
     * reliability to that of the destination.
     * 
     * @param reliability
     */
    public void setReliability(Reliability reliability)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setReliability", reliability);

        JsMessage localMsg = getJSMessage(true);
        msgReliability = reliability;
        localMsg.setReliability(msgReliability);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setReliability");
    }

    // Javadoc inherited
    @Override
    public long getExpiryStartTime()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getExpiryStartTime");

        // Set the expiry to start from the last time we updated the wait time.
        long startTime = getCurrentMEArrivalTimestamp();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getExpiryStartTime", Long.valueOf(startTime));

        return startTime;
    }

    /**
     * Return the maximum time this message should spend in the ItemStream.
     * 
     * If the message has no time to live then this should be the message store's
     * decision. Otherwise we use the time to live less the time the message
     * has already spend in the ME.
     * 
     * This method first appeared in feature 166831.1
     */
    @Override
    public long getMaximumTimeInStore()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMaximumTimeInStore");

        JsMessage localMsg = getJSMessage(true);
        long timeToLive = localMsg.getTimeToLive().longValue();
        long maxTime = NEVER_EXPIRES; // 180921

        if (timeToLive > 0)
        {
            maxTime = timeToLive - getAggregateWaitTime();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMaximumTimeInStore", Long.valueOf(maxTime));

        return maxTime;
    }

    /**
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage
     */
    @Override
    public long calculateWaitTimeUpdate(long timeNow)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "calculateWaitTimeUpdate");

        long calculatedWaitTime = timeNow - getCurrentMEArrivalTimestamp();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "calculateWaitTimeUpdate", Long.valueOf(calculatedWaitTime));

        return calculatedWaitTime;
    }

    /**
     * @see com.ibm.ws.sib.msgstore.AbstractItem#canExpireSilently()
     */
    @Override
    public boolean canExpireSilently()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "canExpireSilently");

        JsMessage localMsg = getJSMessage(true);

        if (localMsg.getReportExpiry() != null || TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "canExpireSilently", Boolean.FALSE);
            return false;
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "canExpireSilently");

        return super.canExpireSilently();
    }

    /**
     * @see com.ibm.ws.sib.msgstore.AbstractItem#eventExpiryNotification(Transaction)
     */
    @Override
    public void eventExpiryNotification(Transaction transaction) throws SevereMessageStoreException
    {
        super.eventExpiryNotification(transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "eventExpiryNotification", transaction);

        if (failedInitInRestore)
        {
            initialiseNonPersistent(true);
        }

        if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
        {
            if (getJSMessage(true).isApiMessage())
            {

                String apiMsgId = null;

                if (getJSMessage(true) instanceof JsApiMessage)
                    apiMsgId = ((JsApiMessage) getJSMessage(true)).getApiMessageId();
                else
                {
                    if (getJSMessage(true).getApiMessageIdAsBytes() != null)
                        apiMsgId = getJSMessage(true).getApiMessageIdAsBytes().toString();
                }

                SibTr.debug(this, UserTrace.tc_mt,
                            nls_mt.getFormattedMessage(
                                                       "MESSAGE_EXPIRED_CWSJU0011",
                                                       new Object[] {
                                                                     apiMsgId,
                                                                     getJSMessage(true).getSystemMessageId(),
                                                                     debugName },
                                                       null));
            }
        }

        if (EXPIRY_NOTIFICATION != null)
        {
            try
            {
                //Check that the report expiry is not null as we might be here because of trace been on
                if (getJSMessage(true).getReportExpiry() != null)
                    EXPIRY_NOTIFICATION.messageEventOccurred(MessageEvents.EXPIRY_NOTIFICATION, this, transaction);
            } catch (SIException e)
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.store.items.MessageItem.eventExpiryNotification",
                                            "1:1096:1.244.1.40",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "eventExpiryNotification", e);

                throw new SIErrorException(e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "eventExpiryNotification");

    }

    @Override
    public void handleInvalidDeliveryDelayable(MaximumAllowedDeliveryDelayAction action) 
    	throws MessageStoreException, SIException {
    	final String methodName = "handleInvalidDeliveryDelayable";
    	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    		SibTr.entry(this, tc, methodName, action);

    	try {    
    		JsMessage jsMessage = getJSMessage(true);  		
    		String apiMsgId = null;
    		if (jsMessage instanceof JsApiMessage) {
    			apiMsgId = ((JsApiMessage) jsMessage).getApiMessageId();
    		} else {
    			if (jsMessage.getApiMessageIdAsBytes() != null)
    				apiMsgId = getJSMessage(true).getApiMessageIdAsBytes().toString();
    		}
    		SIMPItemStream itemstream = (SIMPItemStream) getItemStream();
			BaseDestinationHandler baseDestinationHandler = getDestinationHandler(false, itemstream);
    		
    		switch(action) {
    		case exception:  			
    			baseDestinationHandler.handleUndeliverableMessage(this
    					                                         ,SIRCConstants.SIRC0906_SUSPECT_DELIVERY_DELAY_TIME
    					                                         ,new String[]{apiMsgId,
    					                                        		       jsMessage.getSystemMessageId(),
    					                 	                                   baseDestinationHandler.getName(),
    					                 	                                   baseDestinationHandler.getMessageProcessor().getMessagingEngineName()}
    					                                         ,null);  
    			// No break; always fall through and issue a warning message.
    		default:   				
    			SibTr.warning(tc,"DELIVERY_DELAY_TIME_WARNING_CWSIP0580",
                              new Object[] {apiMsgId,
    									    jsMessage.getSystemMessageId(),
    										baseDestinationHandler.getName(),
    								        action.toString()}
    	                     );
    			break;
    		}
    		
    	} catch (MessageStoreException | SIException exception) {
    		SibTr.exit(this, tc, methodName, exception);
    		throw exception;
    	}

    	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
    		SibTr.exit(this, tc, methodName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.AbstractItem#eventPostCommitRemove(com.ibm.ws.sib.msgstore.Transaction)
     */
    @Override
    public void eventPostCommitRemove(Transaction transaction) throws SevereMessageStoreException
    {
        super.eventPostCommitRemove(transaction);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "eventPostCommitRemove", transaction);

        if (failedInitInRestore)
        {
            initialiseNonPersistent(true);
        }

        dereferenceControlAdapter();

        try
        {
            // TODO I'm curious why we have five slots here as there should only ever be three things registered
            // (the ConsumerDispatcher, PtoPMessageItemStream (or SubscriptionItemStream) and optionally a
            // JSLocalConsumerPoint).
            if (POST_COMMIT_REMOVE_5 != null)
            {
                POST_COMMIT_REMOVE_5.messageEventOccurred(MessageEvents.POST_COMMIT_REMOVE, this, transaction);
            }
            if (POST_COMMIT_REMOVE_4 != null)
            {
                POST_COMMIT_REMOVE_4.messageEventOccurred(MessageEvents.POST_COMMIT_REMOVE, this, transaction);
            }
            if (POST_COMMIT_REMOVE_3 != null)
            {
                POST_COMMIT_REMOVE_3.messageEventOccurred(MessageEvents.POST_COMMIT_REMOVE, this, transaction);
            }
            if (POST_COMMIT_REMOVE_2 != null)
            {
                POST_COMMIT_REMOVE_2.messageEventOccurred(MessageEvents.POST_COMMIT_REMOVE, this, transaction);
            }
            if (POST_COMMIT_REMOVE_1 != null)
            {
                POST_COMMIT_REMOVE_1.messageEventOccurred(MessageEvents.POST_COMMIT_REMOVE, this, transaction);
            }
        } catch (SIException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.items.MessageItem.eventPostCommitRemove",
                                        "1:1160:1.244.1.40",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "eventPostCommitRemove", e);

            throw new SIErrorException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "eventPostCommitRemove");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.AbstractItem#eventPostCommitAdd(com.ibm.ws.sib.msgstore.Transaction)
     */
    @Override
    public void eventPostCommitAdd(Transaction transaction) throws SevereMessageStoreException
    {
        super.eventPostCommitAdd(transaction);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "eventPostCommitAdd", transaction);

        if (failedInitInRestore)
        {
            initialiseNonPersistent(true);
            setRegisterForPostEvents(true);
        }

        if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
        {
            try
            {
                if (getJSMessage(true).isApiMessage())
                {
                    boolean foreignBus = false;
                    boolean link = false;
                    boolean mqlink = false;

                    String targetME = null;
                    if (debugName != null)
                    {
                        SIMPItemStream itemstream = (SIMPItemStream) getItemStream();

                        BaseDestinationHandler destination = getDestinationHandler(false, itemstream);

                        if (itemstream instanceof PubSubMessageItemStream)
                        {
                            targetME = destination.getMessageProcessor().getMessagingEngineName();
                        }
                        else
                        {
                            SIBUuid8 meUUID = ((PtoPMessageItemStream) itemstream).getLocalizingMEUuid();
                            if (meUUID.equals(destination.getMessageProcessor().getMessagingEngineUuid()))
                                targetME = destination.getMessageProcessor().getMessagingEngineName();
                            else
                            {
                                // If it's not the local messaging engine, then don't attempt the very
                                // expensive operation of trawling through admin for a messaging engine
                                // we may (intra-bus) or may not (inter-bus) know about - just for UserTrace.
                                // While the V7 and later interface is better than V6.1, looking up the
                                // name would be an excessive overhead in the critical path.
                                targetME = meUUID.toString();
                            }

                        }

                        foreignBus = destination.isForeignBus();
                        link = destination.isLink();
                        mqlink = destination.isMQLink();
                    }

                    String apiMsgId = null;
                    String correlationId = null;

                    if (getJSMessage(true) instanceof JsApiMessage)
                    {
                        apiMsgId = ((JsApiMessage) getJSMessage(true)).getApiMessageId();
                        correlationId = ((JsApiMessage) getJSMessage(true)).getCorrelationId();
                    }
                    else
                    {
                        if (getJSMessage(true).getApiMessageIdAsBytes() != null)
                            apiMsgId = new String(getJSMessage(true).getApiMessageIdAsBytes());

                        if (getJSMessage(true).getCorrelationIdAsBytes() != null)
                            correlationId = new String(getJSMessage(true).getCorrelationIdAsBytes());
                    }

                    String msg = "PRODUCER_SEND_COMMIT_CWSJU0003";

                    if (foreignBus)
                        msg = "PRODUCER_SEND_COMMIT_BUS_CWSJU0064";
                    else if (mqlink)
                        msg = "PRODUCER_SEND_COMMIT_MQLINK_CWSJU0066";
                    else if (link)
                        msg = "PRODUCER_SEND_COMMIT_LINK_CWSJU0065";

                    SibTr.debug(this, UserTrace.tc_mt,
                                nls_mt.getFormattedMessage(
                                                           msg,
                                                           new Object[] {
                                                                         apiMsgId,
                                                                         getJSMessage(true).getSystemMessageId(),
                                                                         correlationId,
                                                                         debugName,
                                                                         targetME },
                                                           null));
                }
            } catch (MessageStoreException e)
            {
                // No FFDC code needed

                // Log the exception
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.exception(tc, e);

                // As the item is no longer in the store it has likely been committed already.
                // there doesn't seem any point to log the Message Trace at this point.
            }
        }

        // Remove temporary hack to delete publication with no subscribers 183715.1
        if (!transaction.isAutoCommit())
        {
            try
            {
                if (POST_COMMIT_ADD_1 != null)
                {
                    POST_COMMIT_ADD_1.messageEventOccurred(MessageEvents.POST_COMMIT_ADD, this, transaction);
                }
                if (POST_COMMIT_ADD_2 != null)
                {
                    POST_COMMIT_ADD_2.messageEventOccurred(MessageEvents.POST_COMMIT_ADD, this, transaction);
                }
            } catch (SIException e)
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.store.items.MessageItem.eventPostCommitAdd",
                                            "1:1313:1.244.1.40",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "eventPostCommitAdd", e);

                throw new SIErrorException(e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "eventPostCommitAdd");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.AbstractItem#eventPostRollbackAdd(com.ibm.ws.sib.msgstore.Transaction)
     */
    @Override
    public void eventPostRollbackAdd(Transaction transaction) throws SevereMessageStoreException
    {
        super.eventPostRollbackAdd(transaction);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "eventPostRollbackAdd", transaction);

        if (failedInitInRestore)
        {
            initialiseNonPersistent(true);
            setRegisterForPostEvents(true);
        }

        try
        {
            if (POST_ROLLBACK_ADD_1 != null)
            {
                POST_ROLLBACK_ADD_1.messageEventOccurred(MessageEvents.POST_ROLLBACK_ADD, this, transaction);
            }
            if (POST_ROLLBACK_ADD_2 != null)
            {
                POST_ROLLBACK_ADD_2.messageEventOccurred(MessageEvents.POST_ROLLBACK_ADD, this, transaction);
            }
        } catch (SIException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.items.MessageItem.eventPostRollbackAdd",
                                        "1:1360:1.244.1.40",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "eventPostRollbackAdd", e);

            throw new SIErrorException(e);
        }

        resetEvents();

        if (TraceComponent.isAnyTracingEnabled() && UserTrace.tc_mt.isDebugEnabled())
        {
            String apiMsgId = null;

            if (getJSMessage(true) instanceof JsApiMessage)
                apiMsgId = ((JsApiMessage) getJSMessage(true)).getApiMessageId();
            else
            {
                if (getJSMessage(true).getApiMessageIdAsBytes() != null)
                    apiMsgId = getJSMessage(true).getApiMessageIdAsBytes().toString();
            }

            // 524133 Get the transaction for the insert
            Object persistentTranId = (transaction != null) ? transaction.getPersistentTranId() : null;

            SibTr.debug(this, UserTrace.tc_mt,
                        nls_mt.getFormattedMessage(
                                                   "MESSAGE_ROLLBACK_CWSJU0010",
                                                   new Object[] {
                                                                 apiMsgId,
                                                                 getJSMessage(true).getSystemMessageId(),
                                                                 debugName,
                                                                 persistentTranId },
                                                   null));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "eventPostRollbackAdd");
    }

    /*
     * (non-Javadoc)
     * The logic in this method has been reversed so that the register vs callbacks are handled
     * like a stack rather than a queue. This sequence is now vital to SIB0115 and shouldn't be changed.
     * 
     * @see com.ibm.ws.sib.store.AbstractItem#eventPostRollbackRemove(com.ibm.ws.sib.msgstore.Transaction)
     */
    @Override
    public void eventPostRollbackRemove(Transaction transaction) throws SevereMessageStoreException
    {
        super.eventPostRollbackRemove(transaction);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "eventPostRollbackRemove", transaction);

        if (failedInitInRestore)
        {
            initialiseNonPersistent(true);
        }

        try
        {
            //Don't change the order of this callback sequence
            // TODO We should possibly change this logic to something similar to 541867 (on the unlock)
            // as I guess the aim here is not to drive the ConsumerDispatcher until after we've driven any
            // consumer. Basing this on the order of registration is a bit iffy.
            // I'm also curious why we have five slots here as there should only ever be three things registered
            // (the ConsumerDispatcher, PtoPMessageItemStream (or SubscriptionItemStream) and optionally a
            // JSLocalConsumerPoint).
            if (POST_ROLLBACK_REMOVE_5 != null)
            {
                POST_ROLLBACK_REMOVE_5.messageEventOccurred(MessageEvents.POST_ROLLBACK_REMOVE, this, transaction);
            }
            if (POST_ROLLBACK_REMOVE_4 != null)
            {
                POST_ROLLBACK_REMOVE_4.messageEventOccurred(MessageEvents.POST_ROLLBACK_REMOVE, this, transaction);
            }
            if (POST_ROLLBACK_REMOVE_3 != null)
            {
                POST_ROLLBACK_REMOVE_3.messageEventOccurred(MessageEvents.POST_ROLLBACK_REMOVE, this, transaction);
            }
            if (POST_ROLLBACK_REMOVE_2 != null)
            {
                POST_ROLLBACK_REMOVE_2.messageEventOccurred(MessageEvents.POST_ROLLBACK_REMOVE, this, transaction);
            }
            if (POST_ROLLBACK_REMOVE_1 != null)
            {
                POST_ROLLBACK_REMOVE_1.messageEventOccurred(MessageEvents.POST_ROLLBACK_REMOVE, this, transaction);
            }
        } catch (SIException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.items.MessageItem.eventPostRollbackRemove",
                                        "1:1455:1.244.1.40",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "eventPostRollbackRemove", e);

            throw new SIErrorException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "eventPostRollbackRemove");
    }

    /**
     * Reset all callbacks to null.
     * 
     */
    private void resetEvents()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "resetEvents");

        // Reset all callbacks
        PRE_COMMIT_ADD = null;
        PRE_COMMIT_REMOVE = null;
        POST_COMMIT_ADD_1 = null;
        POST_COMMIT_ADD_2 = null;
        POST_COMMIT_REMOVE_1 = null;
        POST_COMMIT_REMOVE_2 = null;
        POST_COMMIT_REMOVE_3 = null;
        POST_COMMIT_REMOVE_4 = null;
        POST_ROLLBACK_ADD_1 = null;
        POST_ROLLBACK_ADD_2 = null;
        POST_ROLLBACK_REMOVE_1 = null;
        POST_ROLLBACK_REMOVE_2 = null;
        POST_ROLLBACK_REMOVE_3 = null;
        POST_ROLLBACK_REMOVE_4 = null;
        UNLOCKED_1 = null;
        UNLOCKED_2 = null;
        UNLOCKED_3 = null;
        PRE_UNLOCKED_1 = null;
        PRE_UNLOCKED_2 = null;
        REFERENCES_DROPPED_TO_ZERO = null;
        PRE_PREPARE_TRANSACTION = null;
        POST_COMMITTED_TRANSACTION = null;
        EXPIRY_NOTIFICATION = null;
        COD_CALLBACK = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "resetEvents");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.AbstractItem#eventUnlocked()
     */
    @Override
    public void eventUnlocked() throws SevereMessageStoreException
    {
        super.eventUnlocked();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "eventUnlocked");

        if (failedInitInRestore)
        {
            initialiseNonPersistent(true);
        }

        reavailable = true;

        try
        {
            // 541867
            // When a message is unlocked, a registered ConsumerDispatcher will try to deliver
            // it to a ready consumer. When this happens the consumer will register for the UNLOCK
            // event. If that is added to a later slot (e.g. UNLOCKED_3) than the one that's currently
            // being processed (the ConsumerDispatcher) it is possible for us to drive the new consumer's
            // unlock event straight away (even though the message has just been locked).

            // This can also result in two (or more) consumers (or multiple instances of the same consumer)
            // being registered on the same message as we may not have deregistered the initial consumer
            // at the time that the ConsumerDispatcher delivers the to the second (or the same) consumer
            // after the unlock.

            // The solution is to proccess all the non-ConsumerDispatchers registered before going back
            // and driving the ConsumerDispatcher.
            for (int pass = 0; pass < 2; pass++)
            {
                if ((UNLOCKED_1 != null) &&
                    (((pass == 0) && !(UNLOCKED_1 instanceof ConsumerDispatcher)) ||
                    ((pass == 1) && (UNLOCKED_1 instanceof ConsumerDispatcher))))
                {
                    UNLOCKED_1.messageEventOccurred(MessageEvents.UNLOCKED, this, null);
                }
                if ((UNLOCKED_2 != null) &&
                    (((pass == 0) && !(UNLOCKED_2 instanceof ConsumerDispatcher)) ||
                    ((pass == 1) && (UNLOCKED_2 instanceof ConsumerDispatcher))))
                {
                    UNLOCKED_2.messageEventOccurred(MessageEvents.UNLOCKED, this, null);
                }
                if ((UNLOCKED_3 != null) &&
                    (((pass == 0) && !(UNLOCKED_3 instanceof ConsumerDispatcher)) ||
                    ((pass == 1) && (UNLOCKED_3 instanceof ConsumerDispatcher))))
                {
                    UNLOCKED_3.messageEventOccurred(MessageEvents.UNLOCKED, this, null);
                }
            }
        } catch (SIException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.items.MessageItem.eventUnlocked",
                                        "1:1568:1.244.1.40",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "eventUnlocked", e);

            throw new SIErrorException(e);
        }

        reavailable = false;

        rmeUnlockCount += uncommittedRMEUnlockCount;
        uncommittedRMEUnlockCount = 0;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "eventUnlocked");
    }

    /**
     * NOTE: This is not part of the AbstractItem interface. This is called directly by classes that
     * lock a message.
     */
    @Override
    public void eventLocked()
    {
        // do nothing
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.Item#itemReferencesDroppedToZero()
     */
    @Override
    public void itemReferencesDroppedToZero()
    {
        super.itemReferencesDroppedToZero();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "itemReferencesDroppedToZero");

        if (failedInitInRestore)
        {
            try
            {
                initialiseNonPersistent(true);
            } catch (SevereMessageStoreException e)
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.store.items.MessageItem.itemReferencesDroppedToZero",
                                            "1:1617:1.244.1.40",
                                            this);

                //There was a problem getting hold of the itemstream when trying to initilise
                //We wouldn't have registered any callbacks yet so we shouldn't carry on

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "itemReferencesDroppedToZero", e);

                throw new SIErrorException(e);
            }
        }

        if (REFERENCES_DROPPED_TO_ZERO != null)
        {
            try
            {
                REFERENCES_DROPPED_TO_ZERO.messageEventOccurred(MessageEvents.REFERENCES_DROPPED_TO_ZERO, this, null);
            } catch (SIException e)
            {
                // Exceptions after a commit are bad !
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.store.items.MessageItem.itemReferencesDroppedToZero",
                                            "1:1644:1.244.1.40",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "itemReferencesDroppedToZero", e);

                throw new SIErrorException(e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "itemReferencesDroppedToZero");
    }

    /**
     * Returns the producerConnectionUuid.
     * Please note, this value may be null in the case of a p2p
     * message.
     * 
     * @return SIBUuid12
     */
    @Override
    public SIBUuid12 getProducerConnectionUuid()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getProducerConnectionUuid");
        }

        if (producerConnectionUuid == null)
        {
            JsMessage localMsg = getJSMessage(true);
            producerConnectionUuid = localMsg.getConnectionUuid();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.exit(this, tc, "getProducerConnectionUuid", producerConnectionUuid);
        }

        return producerConnectionUuid;
    }

    /**
     * Sets the producerConnectionUuid.
     * 
     * @param producerConnectionUuid The producerConnectionUuid to set
     */
    public void setProducerConnectionUuid(SIBUuid12 producerConnectionUuid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setProducerConnectionUuid", producerConnectionUuid);

        this.producerConnectionUuid = producerConnectionUuid;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setProducerConnectionUuid");
    }

    public void setProducerSeed(int producerSeed)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setProducerSeed", Integer.valueOf(producerSeed));
        this.producerSeed = producerSeed;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setProducerSeed");
    }

    public void setForcePut(boolean _forcePut)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setForcePut", Boolean.valueOf(_forcePut));
        this.forcePut = _forcePut;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setForcePut");
    }

    public boolean isForcePut()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "isForcePut");
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "isForcePut", Boolean.valueOf(forcePut));
        return forcePut;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        try
        {
            String sysId = "null";
            String apiId = "null";
            if (msg != null)
            {
                sysId = msg.getSystemMessageId();

                if (msg instanceof JsApiMessage)
                    apiId = ((JsApiMessage) msg).getApiMessageId();
                else if (msg.getApiMessageIdAsBytes() != null)
                    apiId = new String(msg.getApiMessageIdAsBytes());
            }

            long msId = -1;
            try
            {
                if (isInStore())
                    msId = getID();
            } catch (MessageStoreException e)
            {
                // No FFDC code needed
                SibTr.exception(tc, e);
            }

            String text = null;
            //if we can easily get the text of the message then use that
            if (msg instanceof JsJmsTextMessage)
            {
                try
                {
                    text = "'" + ((JsJmsTextMessage) msg).getText() + "'";
                    if (text != null && text.length() > 30)
                        text = text.substring(0, 30) + "..'";
                } catch (UnsupportedEncodingException e)
                {
                    // No FFDC code needed
                    text = "Unsupported message encoding";
                }
            }
            else
                text = String.valueOf(msg);

            return "MessageItem@" + Integer.toHexString(this.hashCode()) + "[" + sysId + "," + apiId + "," + msId + "," + text + "]";
        } catch (RuntimeException e)
        {
            // No FFDC code needed

            // As we're not locking the state down there's very slim chance of hitting
            // something like a NPE while we do this, we'll just catch it and ignore it
            return super.toString();
        }
    }

    /**
     * Mark the message as transacted
     * 
     * @param transacted true if the message was put transactionally
     */
    public void setTransacted(boolean transacted)
    {
        this.transacted = transacted;
    }

    /**
     * Was this message put transactionally
     * 
     * @return true if this message was put transactionally
     */
    @Override
    public boolean isTransacted()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "isTransacted");
            SibTr.exit(this, tc, "isTransacted", Boolean.valueOf(transacted));
        }
        return transacted;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.SIMPMessage#getProducerSeed()
     */
    @Override
    public int getProducerSeed()
    {
        return producerSeed;
    }

    /**
     * Set the maximum storage strategy permitted
     */
    public void setMaxStorageStrategy(int requestedMaxStrategy)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this,
                        tc,
                        "setMaxStorageStrategy",
                        Integer.valueOf(requestedMaxStrategy));
        maxStorageStrategy = requestedMaxStrategy;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setMaxStorageStrategy");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.AbstractItem#getPriority()
     */
    @Override
    public int getPriority()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getPriority");

        if (msgPriority == -2)
        {
            JsMessage localMsg = getJSMessage(true);
            msgPriority = localMsg.getPriority().intValue();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getPriority", Integer.valueOf(msgPriority));

        return msgPriority;
    }

    /**
     * @see com.ibm.ws.sib.msgstore.AbstractItem#getDeliveryDelay()
     * 
     *      Return the maximum time this reference should be in locked state before
     *      its unlocked for consumption,i.e message will be available for consumption
     *      after getDeliveryDelay() time
     */
    @Override
    public long getDeliveryDelay()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getDeliveryDelay");
        long deliveryDelay = 0;

        try {
            deliveryDelay = getJSMessage(true).getDeliveryDelay();
            return deliveryDelay;
        } finally {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getDeliveryDelay", deliveryDelay);

        }

    }

    /**
     * Set the message priority and cache it
     */
    public void setPriority(int newPriority)
    {
        JsMessage localMsg = getJSMessage(true);
        msgPriority = newPriority;
        localMsg.setPriority(newPriority);
    }

    /**
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#registerMessageEventListener(int, MessageEventListener)
     */
    @Override
    public void registerMessageEventListener(int event, MessageEventListener listener)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "registerMessageEventListener", new Object[] { Integer.valueOf(event), listener });

        boolean error = false;

        switch (event)
        {
            case MessageEvents.POST_COMMIT_ADD: {
                if (POST_COMMIT_ADD_1 == null)
                    POST_COMMIT_ADD_1 = listener;
                else if (POST_COMMIT_ADD_2 == null)
                    POST_COMMIT_ADD_2 = listener;
                else
                    error = true;
                break;
            }
            case MessageEvents.POST_COMMIT_REMOVE: {
                if (POST_COMMIT_REMOVE_1 == null)
                    POST_COMMIT_REMOVE_1 = listener;
                else if (POST_COMMIT_REMOVE_2 == null)
                    POST_COMMIT_REMOVE_2 = listener;
                else if (POST_COMMIT_REMOVE_3 == null)
                    POST_COMMIT_REMOVE_3 = listener;
                else if (POST_COMMIT_REMOVE_4 == null)
                    POST_COMMIT_REMOVE_4 = listener;
                else if (POST_COMMIT_REMOVE_5 == null)
                    POST_COMMIT_REMOVE_5 = listener;
                else
                    error = true;
                break;
            }
            case MessageEvents.POST_ROLLBACK_ADD: {
                if (POST_ROLLBACK_ADD_1 == null)
                    POST_ROLLBACK_ADD_1 = listener;
                else if (POST_ROLLBACK_ADD_2 == null)
                    POST_ROLLBACK_ADD_2 = listener;
                else
                    error = true;
                break;
            }
            case MessageEvents.POST_ROLLBACK_REMOVE: {
                if (POST_ROLLBACK_REMOVE_1 == null)
                    POST_ROLLBACK_REMOVE_1 = listener;
                else if (POST_ROLLBACK_REMOVE_2 == null)
                    POST_ROLLBACK_REMOVE_2 = listener;
                else if (POST_ROLLBACK_REMOVE_3 == null)
                    POST_ROLLBACK_REMOVE_3 = listener;
                else if (POST_ROLLBACK_REMOVE_4 == null)
                    POST_ROLLBACK_REMOVE_4 = listener;
                else if (POST_ROLLBACK_REMOVE_5 == null)
                    POST_ROLLBACK_REMOVE_5 = listener;
                else
                    error = true;
                break;
            }
            case MessageEvents.PRE_COMMIT_ADD: {
                if (PRE_COMMIT_ADD == null)
                    PRE_COMMIT_ADD = listener;
                else
                    error = true;
                break;
            }
            case MessageEvents.PRE_COMMIT_REMOVE: {
                if (PRE_COMMIT_REMOVE == null)
                    PRE_COMMIT_REMOVE = listener;
                else
                    error = true;
                break;
            }
            case MessageEvents.REFERENCES_DROPPED_TO_ZERO: {
                if (REFERENCES_DROPPED_TO_ZERO == null)
                    REFERENCES_DROPPED_TO_ZERO = listener;
                else
                    error = true;
                break;
            }
            case MessageEvents.UNLOCKED: {
                if (UNLOCKED_1 == null)
                    UNLOCKED_1 = listener;
                else if (UNLOCKED_2 == null)
                    UNLOCKED_2 = listener;
                else if (UNLOCKED_3 == null)
                    UNLOCKED_3 = listener;
                else
                    error = true;
                break;
            }
            case MessageEvents.PRE_UNLOCKED: {
                if (PRE_UNLOCKED_1 == null)
                    PRE_UNLOCKED_1 = listener;
                else if (PRE_UNLOCKED_2 == null)
                    PRE_UNLOCKED_2 = listener;
                else
                    error = true;
                break;
            }
            case MessageEvents.PRE_PREPARE_TRANSACTION: //183715.1
            {
                if (PRE_PREPARE_TRANSACTION == null)
                    PRE_PREPARE_TRANSACTION = listener;
                else
                    error = true;
                break;
            }
            case MessageEvents.POST_COMMITTED_TRANSACTION: {
                if (POST_COMMITTED_TRANSACTION == null)
                    POST_COMMITTED_TRANSACTION = listener;
                else
                    error = true;
                break;
            }
            case MessageEvents.EXPIRY_NOTIFICATION: {
                if (EXPIRY_NOTIFICATION == null)
                    EXPIRY_NOTIFICATION = listener;
                else
                    error = true;
                break;
            }
            case MessageEvents.COD_CALLBACK: {
                if (COD_CALLBACK == null)
                    COD_CALLBACK = listener;
                else
                    error = true;
                break;
            }
        }

        if (error)
        {
            SIErrorException e = new SIErrorException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.store.items.MessageItem.registerMessageEventListener",
                                                                  "1:1982:1.244.1.40", Integer.valueOf(event) },
                                                    null));

            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.items.MessageItem.registerMessageEventListener",
                                        "1:1988:1.244.1.40",
                                        this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.store.items.MessageItem.registerMessageEventListener",
                                      "1:1995:1.244.1.40", Integer.valueOf(event) });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "registerMessageEventListener", e);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "registerMessageEventListener");
    }

    /**
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#deregisterMessageEventListener(int, MessageEventListener)
     */
    @Override
    public void deregisterMessageEventListener(int event, MessageEventListener listener)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "deregisterMessageEventListener", new Object[] { Integer.valueOf(event), listener });

        switch (event)
        {
            case MessageEvents.POST_COMMIT_ADD: {
                if (POST_COMMIT_ADD_1 == listener)
                    POST_COMMIT_ADD_1 = null;
                else if (POST_COMMIT_ADD_2 == listener)
                    POST_COMMIT_ADD_2 = null;
                break;
            }
            case MessageEvents.POST_COMMIT_REMOVE: {
                if (POST_COMMIT_REMOVE_1 == listener)
                    POST_COMMIT_REMOVE_1 = null;
                else if (POST_COMMIT_REMOVE_2 == listener)
                    POST_COMMIT_REMOVE_2 = null;
                else if (POST_COMMIT_REMOVE_3 == listener)
                    POST_COMMIT_REMOVE_3 = null;
                else if (POST_COMMIT_REMOVE_4 == listener)
                    POST_COMMIT_REMOVE_4 = null;
                break;
            }
            case MessageEvents.POST_ROLLBACK_ADD: {
                if (POST_ROLLBACK_ADD_1 == listener)
                    POST_ROLLBACK_ADD_1 = null;
                else if (POST_ROLLBACK_ADD_2 == listener)
                    POST_ROLLBACK_ADD_2 = null;
                break;
            }
            case MessageEvents.POST_ROLLBACK_REMOVE: {
                if (POST_ROLLBACK_REMOVE_1 == listener)
                    POST_ROLLBACK_REMOVE_1 = null;
                else if (POST_ROLLBACK_REMOVE_2 == listener)
                    POST_ROLLBACK_REMOVE_2 = null;
                else if (POST_ROLLBACK_REMOVE_3 == listener)
                    POST_ROLLBACK_REMOVE_3 = null;
                else if (POST_ROLLBACK_REMOVE_4 == listener)
                    POST_ROLLBACK_REMOVE_4 = null;
                break;
            }
            case MessageEvents.PRE_COMMIT_ADD: {
                if (PRE_COMMIT_ADD == listener)
                    PRE_COMMIT_ADD = null;
                break;
            }
            case MessageEvents.PRE_COMMIT_REMOVE: {
                if (PRE_COMMIT_REMOVE == listener)
                    PRE_COMMIT_REMOVE = null;
                break;
            }
            case MessageEvents.REFERENCES_DROPPED_TO_ZERO: {
                if (REFERENCES_DROPPED_TO_ZERO == listener)
                    REFERENCES_DROPPED_TO_ZERO = null;
                break;
            }
            case MessageEvents.UNLOCKED: {
                if (UNLOCKED_1 == listener)
                    UNLOCKED_1 = null;
                else if (UNLOCKED_2 == listener)
                    UNLOCKED_2 = null;
                else if (UNLOCKED_3 == listener)
                    UNLOCKED_3 = null;
                break;
            }
            case MessageEvents.PRE_UNLOCKED: {
                if (PRE_UNLOCKED_1 == listener)
                    PRE_UNLOCKED_1 = null;
                else if (PRE_UNLOCKED_2 == listener)
                    PRE_UNLOCKED_2 = null;
                break;
            }
            case MessageEvents.PRE_PREPARE_TRANSACTION: //183715.1
            {
                if (PRE_PREPARE_TRANSACTION == listener)
                    PRE_PREPARE_TRANSACTION = null;
                break;
            }
            case MessageEvents.POST_COMMITTED_TRANSACTION: {
                if (POST_COMMITTED_TRANSACTION == listener)
                    POST_COMMITTED_TRANSACTION = null;
                break;
            }
            case MessageEvents.EXPIRY_NOTIFICATION: {
                if (EXPIRY_NOTIFICATION == listener)
                    EXPIRY_NOTIFICATION = null;
                break;
            }
            case MessageEvents.COD_CALLBACK: {
                if (COD_CALLBACK == listener)
                    COD_CALLBACK = null;
                break;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "deregisterMessageEventListener");
    }

    //183715.1 setMarkedForDeletion() and deleteMsg() removed

    /**
     * Note the existence of a persistent reference
     */
    public void addPersistentRef()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "addPersistentRef");

        // We have at least one durable reference so the message
        // must maintain its level of persistence
        maintainPersistence = 1;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "addPersistentRef");
    }

    /**
     * Note the existence of a nonpersistent reference
     */
    public void addNonPersistentRef()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "addNonPersistentRef");

        // We only downgrade the persistence of a message if all it has is
        // non-durable references. If it has any durable references the persistence
        // must be maintained.
        if (maintainPersistence == -1)
            maintainPersistence = 0;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "addNonPersistentRef");
    }

    /**
   */
    public MessageProcessorSearchResults getSearchResults()
    {
        return searchResults;
    }

    /**
     * @param searchResults The MP Search results object
     */
    public void setSearchResults(MessageProcessorSearchResults searchResults)
    {
        this.searchResults = searchResults;
    }

    /**
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage
     */
    @Override
    public long getAggregateWaitTime()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getAggregateWaitTime");

        if (messageWaitTime == -1)
        {
            JsMessage localMsg = getJSMessage(true);
            messageWaitTime = localMsg.getMessageWaitTime().longValue();

            // Store this value in the preMEArrivalWaitTime, as it will represent the time that the
            // message has spent bouncing around the bus prior to reaching this ME.
            preMEArrivalWaitTime = messageWaitTime;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this,
                       tc, "getAggregateWaitTime", Long.valueOf(messageWaitTime));

        return messageWaitTime;
    }

    /**
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage
     */
    @Override
    public long getLatestWaitTimeUpdate()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getLatestWaitTimeUpdate");
            SibTr.exit(this, tc, "getLatestWaitTimeUpdate", Long.valueOf(latestWaitTimeUpdate));
        }

        return latestWaitTimeUpdate;
    }

    /**
     * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#beforeCompletion(Transaction)
     */
    @Override
    public void beforeCompletion(TransactionCommon transaction) //183715.1
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "beforeCompletion", transaction);

        if (failedInitInRestore)
        {
            try
            {
                initialiseNonPersistent(true);
            } catch (SevereMessageStoreException e)
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.store.items.MessageItem.beforeCompletion",
                                            "1:2209:1.244.1.40",
                                            this);

                //There was a problem getting hold of the itemstream when trying to initilise
                //We wouldn't have registered any callbacks yet so we shouldn't carry on

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "beforeCompletion", e);

                throw new SIErrorException(e);
            }

        }

        try
        {
            if (PRE_PREPARE_TRANSACTION != null)
            {
                PRE_PREPARE_TRANSACTION.messageEventOccurred(MessageEvents.PRE_PREPARE_TRANSACTION, this, transaction);
                PRE_PREPARE_TRANSACTION = null; // Once used we reset the callback 190376
            }
            // Need the else if because we never want both callbacks to happen - 19
            else if (COD_CALLBACK != null)
            {
                COD_CALLBACK.messageEventOccurred(MessageEvents.COD_CALLBACK, this, transaction);
            }
        } catch (SIException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.items.MessageItem.beforeCompletion",
                                        "1:2243:1.244.1.40",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "beforeCompletion", e);

            throw new SIErrorException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "beforeCompletion");

        return;
    }

    /**
     * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#afterCompletion(Transaction, boolean)
     */
    @Override
    public void afterCompletion(TransactionCommon transaction, boolean committed)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "afterCompletion", transaction);

        if (failedInitInRestore)
        {
            try
            {
                initialiseNonPersistent(true);
            } catch (SevereMessageStoreException e)
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.store.items.MessageItem.afterCompletion",
                                            "1:2279:1.244.1.40",
                                            this);

                //There was a problem getting hold of the itemstream when trying to initilise
                //We wouldn't have registered any callbacks yet so we shouldn't carry on

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "afterCompletion", e);

                throw new SIErrorException(e);
            }
        }

        if (committed && POST_COMMITTED_TRANSACTION != null)
        {
            try
            {
                POST_COMMITTED_TRANSACTION.messageEventOccurred(
                                                                MessageEvents.POST_COMMITTED_TRANSACTION, this, transaction);
                POST_COMMITTED_TRANSACTION = null;
            } catch (SIException e)
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.store.items.MessageItem.afterCompletion",
                                            "1:2307:1.244.1.40",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "afterCompletion", e);

                throw new SIErrorException(e);
            }
        }

        //PM38052 release the reference to the JsMessage, for assuredPersistent we
        // know that the persistent representation is now stable. For ReliablePersistent it might be
        releaseJsMessage();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "afterCompletion");

        return;
    }

    /**
     * Sets a flag to indicate whether we guessed the
     * the stream to use for this message
     * 
     * @param streamIsGuess flag
     */
    @Override
    public void setStreamIsGuess(boolean streamIsGuess)
    {
        this.streamIsGuess = streamIsGuess;
    }

    /**
     * Gets the flag which indicates whether we guessed the
     * the stream to use for this message
     * 
     * @return boolean streamIsGuess flag
     */
    @Override
    public boolean getStreamIsGuess()
    {
        return streamIsGuess;
    }

    /**
     * Set the number of subscriptions in pubsub that this message will fan out to
     * once the publication commits.
     * 
     * @param newFanOut Number of subscriptions that will receive the fan out.
     */
    public void setFanOut(int newFanOut)
    {
        fanOut = newFanOut;
    }

    /**
     * Return the number of subscriptions that received this fanned out
     * publication. Intended to be called post commit.
     */
    public int getFanOut()
    {
        return fanOut;
    }

    /**
     * Tells us if this message requires an new Id
     */
    @Override
    public boolean getRequiresNewId()
    {
        return requiresNewId;
    }

    /**
     * Call this when the message requires a new Id
     */
    @Override
    public void setRequiresNewId(boolean value)
    {
        requiresNewId = value;
    }

    /**
     * Prints the Message Details to the xml output.
     */
    @Override
    public void xmlWriteOn(FormattedWriter writer) throws IOException
    {
        // Restore an un-cached message to we can dump its state
        if (msg == null)
            getJSMessage(true);

        try
        {
            if (msg != null)
            {
                writer.newLine();
                writer.taggedValue("JsMessage", msg.getClass().getName());
                writer.newLine();
                writer.taggedValue("systemMessageId", msg.getSystemMessageId());

                if (msg instanceof JsApiMessage)
                {
                    writer.newLine();
                    writer.taggedValue("APIMessageId", ((JsApiMessage) msg).getApiMessageId());
                    writer.newLine();
                    writer.taggedValue("APICorrelId", ((JsApiMessage) msg).getCorrelationId());
                }
                else
                {
                    if (msg.getApiMessageIdAsBytes() != null)
                    {
                        writer.newLine();
                        writer.taggedValue("APIMessageId", new String(msg.getApiMessageIdAsBytes()));
                    }
                    if (msg.getCorrelationIdAsBytes() != null)
                    {
                        writer.newLine();
                        writer.taggedValue("APICorrelId", new String(msg.getCorrelationIdAsBytes()));
                    }
                }

                writer.newLine();
                writer.taggedValue("putTime", new Date(msg.getTimestamp()));

                // If this MessageItem is sitting on an xmit ItemStream then we're probably
                // going to be interested in the GD data in the message.
                try
                {
                    ItemStream is = getItemStream();
                    if (is instanceof PtoPXmitMsgsItemStream)
                    {
                        writer.newLine();
                        writer.startTag("xmitData");
                        writer.indent();

                        writer.newLine();
                        writer.taggedValue("startTick", msg.getGuaranteedValueStartTick());
                        writer.newLine();
                        writer.taggedValue("endTick", msg.getGuaranteedValueEndTick());
                        writer.newLine();
                        writer.taggedValue("priority", msg.getPriority());
                        writer.newLine();
                        writer.taggedValue("reliability", msg.getReliability());
                        writer.newLine();
                        writer.taggedValue("streamId", msg.getGuaranteedStreamUUID());
                        if (msg.getRoutingDestination() != null)
                        {
                            writer.newLine();
                            writer.taggedValue("routingDestination", msg.getRoutingDestination());
                        }

                        writer.outdent();
                        writer.newLine();
                        writer.endTag("xmitData");
                    }
                } catch (SevereMessageStoreException se)
                {
                    // No FFDC code needed
                } catch (RuntimeException re)
                {
                    // No FFDC code needed
                }

                String text = null;
                //if we can easily get the text of the message then use that
                if (msg instanceof JsJmsTextMessage)
                {
                    try
                    {
                        text = ((JsJmsTextMessage) msg).getText();
                        if (text != null && text.length() > 40)
                            text = text.substring(0, 37) + "...";
                    } catch (UnsupportedEncodingException e)
                    {
                        // No FFDC code needed
                        text = "Unsupported message encoding";
                    }
                }
                else
                    text = String.valueOf(msg);

                writer.newLine();
                writer.taggedValue("data", text);
            }
        } catch (RuntimeException e)
        {
            // No FFDC code needed

            // As we're not locking the state down there's very slim chance of hitting
            // something like a NPE while we do this, we'll just catch it and ignore it
        }
    }

    /**
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#setStoreAtSendTime(boolean)
     */
    @Override
    public void setStoreAtSendTime(boolean store)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "setStoreAtSendTime");
            SibTr.exit(this, tc, "setStoreAtSendTime");
        }
        storeAtSendTime = store;
    }

    /**
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#isToBeStoredAtSendTime()
     */
    @Override
    public boolean isToBeStoredAtSendTime()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "isToBeStoredAtSendTime");
            SibTr.exit(this, tc, "isToBeStoredAtSendTime");
        }
        return storeAtSendTime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#getControlAdapter()
     */
    @Override
    public ControlAdapter getControlAdapter()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getControlAdapter");

        if (controlAdapter == null)
        {
            createControlAdapter();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getControlAdapter", controlAdapter);

        return controlAdapter;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#createControlAdapter()
     */
    @Override
    public void createControlAdapter()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "createControlAdapter");

        DestinationHandler dh = null;
        try
        {
            ItemStream is = getItemStream();
            if (is instanceof PubSubMessageItemStream)
            {
                dh = ((PubSubMessageItemStream) is).getDestinationHandler();
                controlAdapter = new QueuedMessage(this, dh, is);
            }
            else if (is instanceof MQLinkMessageItemStream)
            {
                dh = ((MQLinkMessageItemStream) is).getDestinationHandler();
                controlAdapter = new MQLinkQueuedMessage(this, dh, is);
            }
            else if (is instanceof PtoPMessageItemStream)
            {
                dh = ((PtoPMessageItemStream) is).getDestinationHandler();
                controlAdapter = new QueuedMessage(this, dh, is);
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Unknown item stream type " + is);
            }
        } catch (Exception e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.items.MessageItem.createControlAdapter",
                                        "1:2585:1.244.1.40",
                                        this);

            SibTr.exception(tc, e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "createControlAdapter");
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

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#isFromRemoteME()
     */
    @Override
    public boolean isFromRemoteME()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "isFromRemoteME");
            SibTr.exit(this, tc, "isFromRemoteME", Boolean.valueOf(fromRemoteME));
        }
        return fromRemoteME;
    }

    /**
     * 
     * @param boolean true if the message came from another ME
     * 
     */
    public void setFromRemoteME(boolean fromRemoteME)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "setFromRemoteME", Boolean.valueOf(fromRemoteME));
            SibTr.exit(this, tc, "setFromRemoteME");
        }
        this.fromRemoteME = fromRemoteME;
    }

    /**
     * 
     * @return true if the message came from another Bus
     * 
     */
    public boolean isFromRemoteBus()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "isFromRemoteBus");
            SibTr.exit(this, tc, "isFromRemoteBus", Boolean.valueOf(fromRemoteBus));
        }
        return fromRemoteBus;
    }

    /**
     * 
     * @param boolean true if the message came from another Bus
     * 
     */
    public void setFromRemoteBus(boolean fromRemoteBus)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "setFromRemoteBus", Boolean.valueOf(fromRemoteBus));
            SibTr.exit(this, tc, "setFromRemoteBus");
        }
        this.fromRemoteBus = fromRemoteBus;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#dereferenceControlAdapter()
     */
    @Override
    public void dereferenceControlAdapter()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "dereferenceControlAdapter");

        //dereference is only called from the post remove callback
        //if it is being removed then it must have been added and the
        //controlAdapter should have been created
        if (controlAdapter != null)
        {
            controlAdapter.dereferenceControllable();
            controlAdapter = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "dereferenceControlAdapter");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#getGuaranteedStreamUuid()
     */
    @Override
    public SIBUuid12 getGuaranteedStreamUuid()
    {
        if (!guaranteedStreamUuidSet)
        {
            JsMessage localMsg = getJSMessage(true);
            guaranteedStreamUuidSet = true;
            guaranteedStreamUuid = localMsg.getGuaranteedStreamUUID();
        }

        return guaranteedStreamUuid;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#setGuaranteedStreamUuid()
     */
    @Override
    public void setGuaranteedStreamUuid(SIBUuid12 uuid)
    {
        JsMessage localMsg = getJSMessage(true);
        guaranteedStreamUuid = uuid;
        guaranteedStreamUuidSet = true;

        localMsg.setGuaranteedStreamUUID(guaranteedStreamUuid);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#getReportCOD()
     */
    @Override
    public Byte getReportCOD()
    {
        // If the cached reference isn't set up, then get one now.
        if (!_reportCODSet)
        {
            JsMessage localMsg = getJSMessage(true);
            _reportCODSet = true;
            _reportCOD = localMsg.getReportCOD();
        }
        return _reportCOD;
    }

    public long getCurrentMEArrivalTimestamp()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getCurrentMEArrivalTimestamp");

        if (currentMEArrivalTimeStamp == 0)
        {
            JsMessage localMsg = getJSMessage(true);
            currentMEArrivalTimeStamp = localMsg.getCurrentMEArrivalTimestamp().longValue();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getCurrentMEArrivalTimestamp", currentMEArrivalTimeStamp);

        return currentMEArrivalTimeStamp;
    }

    public void setCurrentMEArrivalTimestamp(long timeStamp)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setCurrentMEArrivalTimestamp", timeStamp);

        currentMEArrivalTimeStamp = timeStamp;
        arrivalTimeStored = false;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setCurrentMEArrivalTimestamp");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#updateStatisticsMessageWaitTime(long)
     */
    @Override
    public long updateStatisticsMessageWaitTime()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "updateStatisticsMessageWaitTime");

        //Store in the message the amount of time it was on the queue
        long timeNow = java.lang.System.currentTimeMillis();
        latestWaitTimeUpdate = calculateWaitTimeUpdate(timeNow);
        messageWaitTime = latestWaitTimeUpdate + getPreMEArrivalWaitTime();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "updateStatisticsMessageWaitTime", messageWaitTime);

        return messageWaitTime;
    }

    /**
     * Set the name of the destination that the message is on
     * 
     * @param string
     */
    public void setDebugName(String string)
    {
        debugName = string;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#isReDriven()
     */
    @Override
    public boolean isReavailable()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isReavailable");
            SibTr.exit(tc, "isReavailable", Boolean.valueOf(reavailable));
        }
        return reavailable;
    }

    @Override
    public int guessRedeliveredCount()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "guessRedeliveredCount");

        int count = 0;

        count = super.guessUnlockCount() + (int) rmeUnlockCount + (int) uncommittedRMEUnlockCount;
        try {
            count += super.getPersistedRedeliveredCount();
        } catch (MessageStoreException e) {
            // Ignore the exception, and return the count.
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "guessRedeliveredCount", Integer.valueOf(count));

        return count;
    }

    /**
     * Sets the previous hop Bus name
     * 
     * @param busName
     */
    private void setOriginatingBus(String busName)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setOriginatingBus", busName);
        _busName = busName;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setOriginatingBus");
    }

    /**
     * Gets the bus that the message was previously at.
     * 
     * @return
     */
    public String getOriginatingBus()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getOriginatingBus");
            SibTr.exit(this, tc, "getOriginatingBus", _busName);
        }
        return _busName;
    }

    /**
     * These methods give the MS permission to delay requesting the message's
     * persistent data until the last possible instant, and never to have to
     * copy the persistent data array because it will not change under our feet.
     * 
     * This gives a big performance boost for nonpersistent messages.
     * 
     * @return
     */
    @Override
    public boolean isPersistentDataImmutable()
    {
        return true;
    }

    @Override
    public boolean isPersistentDataNeverUpdated()
    {
        return true;
    }

    /**
     * Forces the local messageItem currentMEArrivalTime value to be
     * set to the JsMessage rather than wait till the data has been
     * spilled to the db.
     * 
     * This method will only work if messageItem.setCurrentMEArrivalTimestamp
     * has been called previously.
     */
    public void forceCurrentMEArrivalTimeToJsMessage()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "forceCurrentMEArrivalTimeToJsMessage");

        if (!arrivalTimeStored)
        {
            JsMessage localMsg = getJSMessage(true);
            localMsg.setCurrentMEArrivalTimestamp(currentMEArrivalTimeStamp);
            arrivalTimeStored = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "forceCurrentMEArrivalTimeToJsMessage");
    }

    /**
     * @return
     */
    public int getRedirectCount() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getRedirectCount");
            SibTr.exit(this, tc, "getRedirectCount", Integer.valueOf(redirectCount));
        }
        return redirectCount;
    }

    /**
     * @param i
     */
    public void incrementRedirectCount() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "incrementRedirectCount");
            SibTr.exit(this, tc, "incrementRedirectCount", Integer.valueOf(redirectCount));
        }
        redirectCount++;
    }

    /**
     * unlockMsg - call any registered callbacks before we unlock the
     * message
     * N.B All MP unlocking should go through this method
     */

    @Override
    public void unlockMsg(long lockID, Transaction transaction, boolean incrementUnlock) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "unlockMsg", new Object[] { Long.valueOf(lockID), transaction, Boolean.valueOf(incrementUnlock) });

        if (failedInitInRestore)
        {
            initialiseNonPersistent(true);
        }

        if (incrementUnlock)
        {
            // If we do not wish to increment the count, there is no point checking
            // against redelivery threshold
            redeliveryCountReached = false;

            try
            {
                if (PRE_UNLOCKED_1 != null)
                {
                    PRE_UNLOCKED_1.messageEventOccurred(MessageEvents.PRE_UNLOCKED, this, transaction);
                }
                if (PRE_UNLOCKED_2 != null)
                {
                    PRE_UNLOCKED_2.messageEventOccurred(MessageEvents.PRE_UNLOCKED, this, transaction);
                }
            } catch (SIException e)
            {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.store.items.MessageItem.unlockMsg",
                                            "1:2955:1.244.1.40",
                                            this);

                SibTr.exception(tc, e);

                // Something went wrong so make sure we unlock the message
                redeliveryCountReached = false;
            }

        }

        // If the pre_unlocked callback redirected the message to the exc dest,
        // then unlock the message.
        if (!redeliveryCountReached)
        {
            unlock(lockID, transaction, incrementUnlock);

            SIMPItemStream itemStream = (SIMPItemStream) getItemStream();
            BaseDestinationHandler bdh = getDestinationHandler(false, itemStream);

            // proceede if and only if the redeliverycount column exists
            if (bdh.isRedeliveryCountPersisted() && bdh.getMessageProcessor().getMessageStore().isRedeliveryCountColumnAvailable()) {

                int rdl_count = guessRedeliveredCount();

                persistRedeliveredCount(rdl_count);

                //updating the value in MFP.
                if (msg != null)
                    msg.setRedeliveredCount(rdl_count);

            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "unlockMsg");
    }

    @Override
    public void persistRedeliveredCount(int redeliveredCount) throws SevereMessageStoreException
    {
        super.persistRedeliveredCount(redeliveredCount);
    }

    @Override
    public void setRedeliveryCountReached() {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "setRedeliveryCountReached");
            SibTr.exit(this, tc, "setRedeliveryCountReached");
        }
        redeliveryCountReached = true;
    }

    @Override
    public boolean isReference()
    {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#releaseJsMessage()
     */
    @Override
    public void releaseJsMessage()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "releaseJsMessage");
        }

        // We check to see if it is safe to remove a non persistent msg now. i.e. the asynchronous
        // gap between the commit add and the spilling to disk for store_maybe.
        // A best effort nonpersistent msg wil never get spilled to disk so we need to
        // check that we don't remove a reference to one.
        if (getReliability().compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) > 0 && isPersistentRepresentationStable())
        {
            synchronized (this) // we don't want to take the msg away from underneath someone
            {
                softReferenceMsg = new SoftReference<JsMessage>(msg);
                msg = null;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.exit(this, tc, "releaseJsMessage");
        }
    }

    /*
     * Restores the JsMessage from the msgstore.
     * 
     * @param throwExceptionIfNotAvailable Boolean to indicate whether exception
     * has to be thrown if message not available
     * Throws SIMPErrorException if the restore failed
     */
    private void restoreJsMessageFromMsgstore(boolean throwExceptionIfNotAvailable)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "restoreJsMessageFromMsgstore", Boolean.valueOf(throwExceptionIfNotAvailable));
        }
        restoringJsMessageOnly = true;

        // Call a new msgstore method on the Item (?) that will tell msgstore
        // to restore the item
        try
        {
            restoreData(throwExceptionIfNotAvailable);
        } catch (MessageStoreException e)
        {
            // No FFDC code needed
            // set msg as null when specified not to throw Exception when message not available
            if ((e instanceof NotInMessageStore) && !throwExceptionIfNotAvailable)
            {
                msg = null;
            }
            else
            {
                // This is bad, if we are here then we don't have our cache and the msgstore can't restore
                // the message for some reason.

                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.store.items.MessageItem.restoreJsMessageFromMsgstore",
                                            "1:3079:1.244.1.40",
                                            this);

                SibTr.exception(tc, e);

                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                            new Object[] {
                                          "1:3086:1.244.1.40",
                                          SIMPUtils.getStackTrace(e)
                            });

                SIMPErrorException errorException =
                                new SIMPErrorException(
                                                nls.getFormattedMessage(
                                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                                        new Object[] { "com.ibm.ws.sib.processor.impl.store.items.MessageItem", "1:3094:1.244.1.40" },
                                                                        null));

                // TODO : For now we throw a runtime here but we need to look at the stack to see
                // what this means.

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "restoreJsMessageFromMsgstore", e);

                throw errorException;
            }
        } finally
        {
            restoringJsMessageOnly = false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.exit(this, tc, "restoreJsMessageFromMsgstore", msg);
        }
    }

    // Remember if the producer prefers local queue points over others
    // (no persistence required - not in JsMessage)
    public void setPreferLocal(boolean preferLocal)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "setPreferLocal", Boolean.valueOf(preferLocal));
            SibTr.exit(tc, "setPreferLocal");
        }
        this.preferLocal = preferLocal;
    }

    // Indicate if the producer prefers local queue points over others
    public boolean preferLocal()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "preferLocal");
            SibTr.exit(tc, "preferLocal", Boolean.valueOf(preferLocal));
        }
        return preferLocal;
    }

    @Override
    public void markHiddenMessage(boolean hiddenMessage)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "markHiddenMessage", Boolean.valueOf(hiddenMessage));
            SibTr.exit(tc, "markHiddenMessage");
        }
        this.hiddenMessage = hiddenMessage;
    }

    @Override
    public boolean isHidden()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isHidden");
            SibTr.exit(tc, "isHidden", Boolean.valueOf(hiddenMessage));
        }
        return hiddenMessage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#setHiddenExpiryTime(long)
     */
    @Override
    public void setHiddenExpiryTime(long expiryTime)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "setHiddenExpiryTime", Long.valueOf(expiryTime));
        }
        hiddenExpiryTime = expiryTime;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.exit(tc, "setHiddenExpiryTime");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#getHiddenExpiryTime()
     */
    @Override
    public long getHiddenExpiryTime()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getHiddenExpiryTime");
            SibTr.exit(tc, "getHiddenExpiryTime", Long.valueOf(hiddenExpiryTime));
        }
        return hiddenExpiryTime;
    }

    @Override
    public SIBUuid8 getLocalisingMEUuid() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getLocalisingMEUuid");
            SibTr.exit(tc, "getLocalisingMEUuid", localisingMEUuid);
        }
        return localisingMEUuid;
    }

    @Override
    public void setLocalisingME(SIBUuid8 messagingEngineUuid)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setLocalisingME", messagingEngineUuid);

        this.localisingMEUuid = messagingEngineUuid;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setLocalisingME");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.JsMessageWrapper#getMessageWaitTime()
     */
    @Override
    public long getMessageWaitTime()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getMessageWaitTime");
            SibTr.exit(this,
                       tc, "getMessageWaitTime", Long.valueOf(messageWaitTime));
        }

        return messageWaitTime;
    }

    /**
     * Retrieve the time that the message has spent in the bus prior to reaching this ME
     * 
     * @return preMEArrivalWaitTime
     */
    private long getPreMEArrivalWaitTime()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getPreMEArrivalWaitTime");

        if (messageWaitTime == -1)
        {
            JsMessage localMsg = getJSMessage(true);
            messageWaitTime = localMsg.getMessageWaitTime().longValue();

            // Store this value in the preMEArrivalWaitTime, as it will represent the time that the
            // message has spent bouncing around the bus prior to reaching this ME.
            preMEArrivalWaitTime = messageWaitTime;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this,
                       tc, "getPreMEArrivalWaitTime", Long.valueOf(preMEArrivalWaitTime));

        return preMEArrivalWaitTime;
    }

    @Override
    public boolean isRemoteGet()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isRemote");
            SibTr.exit(tc, "isRemote", Boolean.FALSE);
        }
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#setMessageControlClassification(java.lang.String)
     */
    @Override
    public void setMessageControlClassification(String classification)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setMessageControlClassification", classification);
        JsMessage localMsg = getJSMessage(true);

        // Cannot set this parameter on a Control message
        if (localMsg.isApiMessage())
        {
            if (localMsg instanceof JsApiMessage)
            {
                ((JsApiMessage) localMsg).setMessageControlClassification(classification);
                messageControlClassification = classification;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setMessageControlClassification");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#getMessageControlClassification()
     * 
     * @param throwExceptionIfMessageNotAvailable Boolean to indicate whether exception
     * has to be thrown if message not available
     */
    @Override
    public String getMessageControlClassification(boolean throwExceptionIfMessageNotAvailable)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMessageControlClassification", Boolean.valueOf(throwExceptionIfMessageNotAvailable));

        // Have we retrieved the messageControlClassification from the message already?
        // Note that the value of messageControlClassification might be null where XD is
        // registered but is not classifying messages.
        if (!retrievedMessageControlClassification)
        {
            JsMessage localMsg = getJSMessage(throwExceptionIfMessageNotAvailable);
            if (localMsg == null)
            {
                messageControlClassification = null;
            }
            else
            {
                // Cannot set this parameter on a Control message
                if (localMsg.isApiMessage())
                {
                    if (localMsg instanceof JsApiMessage)
                    {
                        messageControlClassification = ((JsApiMessage) localMsg).getMessageControlClassification();
                    }
                }
                retrievedMessageControlClassification = true;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMessageControlClassification", messageControlClassification);

        return messageControlClassification;
    }

    @Override
    public void setRMEUnlockCount(long rmeUnlockCount)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "setRMEUnlockCount", Long.valueOf(rmeUnlockCount));
            SibTr.exit(this, tc, "setRMEUnlockCount");
        }
        this.uncommittedRMEUnlockCount = rmeUnlockCount;
    }

    public Object getSynchUpdateLock()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getSynchUpdateLock");
            SibTr.exit(this, tc, "getSynchUpdateLock", messageSyncUpdateLock);
        }

        return messageSyncUpdateLock;
    }

    //sets whether event listeners has to be registered for post events
    public void setRegisterForPostEvents(boolean registerEvents)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "setRegisterForPostEvents", Boolean.valueOf(registerEvents));
            SibTr.exit(this, tc, "setRegisterForPostEvents");
        }
        this.registerEvents = registerEvents;
    }

    //Indicates whether event listeners has to be registered for post events
    public boolean getRegisterForPostEvents()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getRegisterForPostEvents");
            SibTr.exit(this, tc, "getRegisterForPostEvents", Boolean.valueOf(registerEvents));
        }
        return registerEvents;
    }

}
