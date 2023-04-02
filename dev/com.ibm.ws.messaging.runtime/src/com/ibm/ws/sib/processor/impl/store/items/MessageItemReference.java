/*******************************************************************************
 * Copyright (c) 2012, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.sib.processor.impl.store.items;

import java.io.IOException;
import java.util.List;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.Reliability;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.mfp.JsMessage;
import com.ibm.ws.sib.msgstore.ItemReference;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.ReferenceStream;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.BaseDestinationHandler;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.MessageEvents;
import com.ibm.ws.sib.processor.impl.store.itemstreams.DurableSubscriptionItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.MessageReferenceStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.ProxyReferenceStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.PubSubMessageItemStream;
import com.ibm.ws.sib.processor.impl.store.itemstreams.SIMPItemStream;
import com.ibm.ws.sib.processor.matching.MessageProcessorSearchResults;
import com.ibm.ws.sib.processor.runtime.impl.ControlAdapter;
import com.ibm.ws.sib.processor.runtime.impl.QueuedMessage;
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
public final class MessageItemReference extends ItemReference implements SIMPMessage, TransactionCallback
{
    // Indicates whether a new message Id is required
    private boolean requiresNewId = false;

    //trace
    private static TraceComponent tc =
                    SibTr.register(
                                   MessageItemReference.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    private static final TraceNLS nls =
                    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

    private MessageProcessorSearchResults searchResults;

    //The OutputHandler to which this message belongs
    //private OutputHandler outputHandler;

    private ControlAdapter controlAdapter = null;

    private MessageEventListener PRE_COMMIT_ADD;
    private MessageEventListener PRE_COMMIT_REMOVE;
    private MessageEventListener REFERENCES_DROPPED_TO_ZERO;
    
    private final MessageEventListeners postCommitAddListeners = new MessageEventListeners(MessageEvents.POST_COMMIT_ADD, 2);
    // The ConsumerDispatcher, PtoPMessageItemStream (or SubscriptionItemStream) and optionally a JSLocalConsumerPoint, 
    // and then the same again if the transaction rolls back.
    private final MessageEventListeners postCommitRemoveListeners = new MessageEventListeners(MessageEvents.POST_COMMIT_REMOVE, 5);
    private final MessageEventListeners postRollbackAddListeners = new MessageEventListeners(MessageEvents.POST_ROLLBACK_ADD, 2);
    // The ConsumerDispatcher, PtoPMessageItemStream (or SubscriptionItemStream) and optionally a JSLocalConsumerPoint, 
    // and then the same again if the transaction rolls back.    
    private final MessageEventListeners postRollbackRemoveListeners = new MessageEventListeners(MessageEvents.POST_ROLLBACK_REMOVE, 5);
    private final MessageEventListeners preUnlockedListeners = new MessageEventListeners(MessageEvents.PRE_UNLOCKED, 2);
    // The ConsumerDispatcher, and optionally a JSLocalConsumerPoint, 
    // and then the same again if the transaction rolls back.
    private final MessageEventListeners unlockedListeners = new MessageEventListeners(MessageEvents.UNLOCKED, 4);

    // Can this reference be downgraded if the message is assured?
    private boolean downgradePersistence = false;

    /**
     * A flag to indicate whether we guessed the
     * the stream to use for this message
     */
    private boolean streamIsGuess = false;

    /**
     * Indicates if the message is currently being re-driven through the
     * system after becoming reavailable (after an unlock)
     */
    private boolean reavailable = false;

    /**
     * Set if redelivery count reached and we therefore dont require
     * the message to be unlocked.
     */
    private boolean redeliveryCountReached = false;

    private SIBUuid8 localisingMEUuid;

    // We add this to the lock unlockCount to determine the actual redeliveryCount
    private long rmeUnlockCount = 0;

    /**
     * A local reference to the root JsMessage held by the MessageItem
     */
    private JsMessage jsMsg;

    /**
     * Indicates that restore has been called on this MessageItem but we failed
     * to fully initilise (registerForEvents) because the MP had not
     * been started yet. This is likely to occur when this Item is classed as
     * indoubt when the ME starts. If this property is true then we need to
     * recall the registerForEvents again when we get an event callback
     */
    private boolean failedInitInRestore = false;

    private MessageReferenceStream stream;

    /**
     * Empty constructor to allow recreation by the message store after it is
     * persisted. The message data is set via the itemRestore method.
     * public method as driven by MessageStore
     */
    public MessageItemReference()
    {
        super();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "MessageItemReference");

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "MessageItemReference");
    }

    /**
     * The normal constructor to create a new MessageItemReference.
     * public method as driven by MessageStore
     * 
     * @param msg The message to be referenced
     */
    public MessageItemReference(MessageItem msg)
    {
        this(msg, false);
    }

    /**
     * The normal constructor to create a new MessageItemReference.
     * public method as driven by MessageStore
     * 
     * @param msg The message to be referenced
     * @param downgradePersistence The reference is not to be persisted
     */
    public MessageItemReference(MessageItem msg, boolean downgradePersistence)
    {
        super(msg);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "MessageItemReference",
                        new Object[] { msg, Boolean.valueOf(downgradePersistence) });

        // Initialize this reference's handle on the JsMessage (before the
        // root item has released their handle to it).
        getMessage();

        // In some cases it is possible for references to persistent messages
        // to be down graded (i.e. for non-durable subscriptions).
        this.downgradePersistence = downgradePersistence;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "MessageItemReference", this);
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
     * Get the underlying message object
     * 
     * @return The underlying message object
     */
    @Override
    public JsMessage getMessage()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getMessage");

        // We hold a reference to the root JsMessage so that an individual subscription
        // can choose to release its reference to it withour wiping it out for all the
        // others.

        // Try to get hold of the reference without using a lock, if that works then great...
        JsMessage localMsg = jsMsg;

        // Otherwise we need to lock it down and retrieve the message from the root message
        // (who may need to restore it from the MsgStore)
        if (localMsg == null)
        {
            synchronized (this)
            {
                jsMsg = getMessageItem().getMessage();
                localMsg = jsMsg;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getMessage", localMsg);

        return localMsg;
    }

    /**
     * Get the underlying message object only if the message is available
     * 
     * @return The underlying message object
     */
    @Override
    public JsMessage getMessageIfAvailable()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getMessageIfAvailable");

        JsMessage localMsg = jsMsg;

        if (localMsg == null)
        {
            synchronized (this)
            {
                jsMsg = getMessageItem().getMessageIfAvailable();
                localMsg = jsMsg;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getMessageIfAvailable", localMsg);

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
            SibTr.entry(tc, "getStorageStrategy");

        int storageStrat = getMessageItem().getStorageStrategy();

        // It is possible that this reference has a lesser storage strategy
        // from the referenced message item.
        if (downgradePersistence)
        {
            // the maximum required 'persistence' is STORE_MAYBE
            if (storageStrat > STORE_MAYBE)
                storageStrat = STORE_MAYBE;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getStorageStrategy", Integer.valueOf(storageStrat));

        return storageStrat;
    }

    /**
     * Get the message's Reliability
     * 
     * @return the message's Reliability
     */
    @Override
    public Reliability getReliability()
    {
        return getMessageItem().getReliability();
    }

    /**
     * Return the maximum time this reference should spend in a ReferenceStream.
     * 
     * The expiry time of a message reference is always the same as its
     * referenced item. The referenced item's getMaximumTimeInStore() return
     * will still be the same as it was when it was originally stored - the store
     * time only updates when the message is removed.
     * 
     * This method first appeared in feature 166831.1
     */
    @Override
    public long getMaximumTimeInStore()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getMaximumTimeInStore");

        long maxTime = getMessageItem().getMaximumTimeInStore();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getMaximumTimeInStore", new Long(maxTime));

        return maxTime;
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
            SibTr.entry(
                        tc,
                        "eventPostCommitAdd",
                        new Object[] { transaction });

        if ((getMessageItem().getRegisterForPostEvents()) || failedInitInRestore)
        {
            stream = (MessageReferenceStream) getReferenceStream();
            //we can only register the listeners if we are fully initialized
            if ((stream instanceof DurableSubscriptionItemStream) || (stream instanceof ProxyReferenceStream))
            {
                stream.registerListeners(this);
            }
            failedInitInRestore = false;
            getMessageItem().setRegisterForPostEvents(failedInitInRestore);

        }

        if (!transaction.isAutoCommit())
        {
            try {
                postCommitAddListeners.messageEventOccurred(this, transaction);
            
            } catch (SIException e) {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.store.items.MessageItemReference.eventPostCommitAdd",
                                            "1:422:1.147",
                                            this);

                SibTr.exception(tc, e);

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "eventPostCommitAdd", e);

                throw new SIErrorException(e);
            }
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventPostCommitAdd");
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
            SibTr.entry(tc, "eventPostCommitRemove");

        if (failedInitInRestore)
        {
            //we can only register the listeners if we are fully initialized
            if (stream instanceof DurableSubscriptionItemStream)
            {
                stream.registerListeners(this);
            }
            failedInitInRestore = false;
        }

        dereferenceControlAdapter();

        try
        {
            postCommitRemoveListeners.messageEventOccurred(this, transaction);
        } catch (SIException e) {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.items.MessageItemReference.eventPostCommitRemove",
                                        "1:482:1.147",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "eventPostCommitRemove", e);

            throw new SIErrorException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventPostCommitRemove");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.AbstractItem#eventPostRollbackRemove(com.ibm.ws.sib.msgstore.Transaction)
     */
    @Override
    public void eventPostRollbackRemove(Transaction transaction) throws SevereMessageStoreException
    {
        super.eventPostRollbackRemove(transaction);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "eventPostRollbackRemove",
                        new Object[] { transaction });

        if (failedInitInRestore)
        {
            //we can only register the listeners if we are fully initialized
            if (stream instanceof DurableSubscriptionItemStream)
            {
                stream.registerListeners(this);
            }
            failedInitInRestore = false;
        }

        try {    
            postRollbackRemoveListeners.messageEventOccurred(this, transaction);

        } catch (SIException e) {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.items.MessageItemReference.eventPostRollbackRemove",
                                        "1:553:1.147",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "eventPostRollbackRemove", e);

            throw new SIErrorException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventPostRollbackRemove");
    }

    /**
     * Reset all callbacks to null.
     * 
     */
    private void resetEvents()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "resetEvents");

        // Reset all callbacks
        PRE_COMMIT_ADD = null;
        PRE_COMMIT_REMOVE = null;
        
        postCommitAddListeners.reset();
        postCommitRemoveListeners.reset();
        postRollbackAddListeners.reset();
        postRollbackRemoveListeners.reset();
        unlockedListeners.reset();
        preUnlockedListeners.reset();
       
        REFERENCES_DROPPED_TO_ZERO = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "resetEvents");
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
            SibTr.entry(tc, "eventPostRollbackAdd", transaction);

        if ((getMessageItem().getRegisterForPostEvents()) || failedInitInRestore)
        {
            stream = (MessageReferenceStream) getReferenceStream();
            //we can only register the listeners if we are fully initialized
            if ((stream instanceof DurableSubscriptionItemStream) || (stream instanceof ProxyReferenceStream))
            {
                stream.registerListeners(this);
            }
            failedInitInRestore = false;
            getMessageItem().setRegisterForPostEvents(failedInitInRestore);

        }

        try {
            postRollbackAddListeners.messageEventOccurred(this, transaction);
        
        } catch (SIException e) {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.items.MessageItemReference.eventPostRollbackAdd",
                                        "1:641:1.147",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "eventPostRollbackAdd", e);

            throw new SIErrorException(e);
        }

        resetEvents();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventPostRollbackAdd");
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
            SibTr.entry(tc, "eventUnlocked");

        if (failedInitInRestore)
        {
            //we can only register the listeners if we are fully initialized
            if (stream instanceof DurableSubscriptionItemStream)
            {
                stream.registerListeners(this);
            }
            failedInitInRestore = false;
        }

        reavailable = true;

        try {
            unlockedListeners.messageEventOccurred(this, null);
           
        } catch (SIException e) {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.items.MessageItemReference.eventUnlocked",
                                        "1:574:1.106",
                                        this);

            SibTr.exception(tc, e);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "eventUnlocked", e);

            throw new SIErrorException(e);
        }
        reavailable = false;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventUnlocked");
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

    /**
     * Returns the producerConnectionUuid.
     * 
     * @return SIBUuid12
     */
    @Override
    public SIBUuid12 getProducerConnectionUuid()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getProducerConnectionUuid");
            SibTr.exit(tc, "getProducerConnectionUuid");
        }

        return getMessageItem().getProducerConnectionUuid();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        MessageItem msg = null;

        try
        {
            msg = (MessageItem) getReferredItem();
        } catch (SevereMessageStoreException e)
        {
            // No FFDC code needed
            // It's quite possible that trace will try to perform a toString() on an ItemReference
            // when it (or it's Item) isn't actually in the MsgStore (e.g. at recovery time). For
            // that reason we simply swallow any exception from MsgStore here.
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "toString error when obtaining referred item", e);
        }

        String text = "MessageItemReference@" + Integer.toHexString(this.hashCode()) + "[";
        if (msg != null)
            text += msg.toString() + "]";
        else
            text += "NULL]";

        return text;
    }

    @Override
    public void xmlWriteOn(FormattedWriter writer) throws IOException
    {
        // If this reference is sitting on a ProxyReferenceStream then we're probably
        // going to be interested in the GD data in the message that it references.
        try
        {
            ReferenceStream rs = getReferenceStream();
            if (rs instanceof ProxyReferenceStream)
            {
                MessageItem msgItem = (MessageItem) getReferredItem();
                JsMessage msg = msgItem.getMessage();

                if (msg != null)
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

                    writer.outdent();
                    writer.newLine();
                    writer.endTag("xmitData");
                }
            }
        } catch (SevereMessageStoreException se)
        {
            // No FFDC code needed
        } catch (RuntimeException re)
        {
            // No FFDC code needed
        }
    }

    /**
     * Returns the referenced message item
     */
    private MessageItem getMessageItem()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getMessageItem");

        MessageItem msg = null;
        try
        {
            msg = (MessageItem) getReferredItem();
        } catch (MessageStoreException e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.items.MessageItemReference.getMessageItem",
                                        "1:857:1.147",
                                        this);

            SibTr.exception(tc, e);

            // TODO : For now we throw a runtime here but we need to look at the stack to see
            // what this means.

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "getMessageItem", e);

            throw new SIErrorException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getMessageItem", msg);

        return msg;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.SIMPMessage#isTransacted()
     */
    @Override
    public boolean isTransacted()
    {
        return getMessageItem().isTransacted();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.SIMPMessage#getProducerSeed()
     */
    @Override
    public int getProducerSeed()
    {
        return getMessageItem().getProducerSeed();
    }

    /**
     * @see com.ibm.ws.sib.msgstore.AbstractItem#restore(byte[])
     */
    // Feature SIB0112b.mp.1
    @Override
    public void restore(final List<DataSlice> dataSlices)
                    throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "restore", dataSlices);

        stream = (MessageReferenceStream) getReferenceStream();
        //we can only register the listeners if we are fully initialized
        if (stream instanceof DurableSubscriptionItemStream)
        {
            if (!stream.hasInDoubtItems())
            {
                stream.registerListeners(this);
                failedInitInRestore = false; //Set to false so we don't call it again
            }
            else
            {
                //we are not yet restored due to an indoubt tran.
                //We have to mark that we failed to initialise and do
                //it later when we get a callback
                failedInitInRestore = true;
            }
        }

        // If in-doubt remove, set current active transaction for ordered messaging
        if (isRemoving())
            stream.setCurrentTransaction(this, false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "restore");
    }

    /**
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#registerMessageEventListener(int, MessageEventListener)
     */
    @Override
    public void registerMessageEventListener(int event, MessageEventListener listener)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "registerMessageEventListener", new Object[] { Integer.valueOf(event), listener });

        boolean error = false;

        switch (event)
        {
            case MessageEvents.POST_COMMIT_ADD: {
                postCommitAddListeners.add(listener);
                break;
            }
            case MessageEvents.POST_COMMIT_REMOVE: {
                postCommitRemoveListeners.add(listener);               
                break;
            }
            case MessageEvents.POST_ROLLBACK_ADD: {
                postRollbackAddListeners.add(listener);
                break;
            }
            case MessageEvents.POST_ROLLBACK_REMOVE: {
                postRollbackRemoveListeners.add(listener); 
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
                unlockedListeners.add(listener);
                break;
            }
            case MessageEvents.PRE_UNLOCKED: {
                preUnlockedListeners.add(listener);
                break;
            }
        }

        if (error)
        {
            SIErrorException e = new SIErrorException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.store.items.MessageItemReference.registerMessageEventListener",
                                                                  "1:1013:1.147", Integer.valueOf(event) },
                                                    null));

            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.items.MessageItemReference.registerMessageEventListener",
                                        "1:1019:1.147",
                                        this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.store.items.MessageItemReference.registerMessageEventListener",
                                      "1:1026:1.147", Integer.valueOf(event) });

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "registerMessageEventListener", e);

            throw e;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerMessageEventListener");
    }

    /**
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#deregisterMessageEventListener(int, MessageEventListener)
     */
    @Override
    public void deregisterMessageEventListener(int event, MessageEventListener listener)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deregisterMessageEventListener", new Object[] { Integer.valueOf(event), listener });

        switch (event)
        {
            case MessageEvents.POST_COMMIT_ADD: {
                postCommitAddListeners.remove(listener);
                break;
            }
            case MessageEvents.POST_COMMIT_REMOVE: {
                postCommitRemoveListeners.remove(listener);
                break;
            }
            case MessageEvents.POST_ROLLBACK_ADD: {
                postRollbackAddListeners.remove(listener);
                break;
            }
            case MessageEvents.POST_ROLLBACK_REMOVE: {
                postRollbackRemoveListeners.remove(listener);
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
                unlockedListeners.remove(listener);
                break;
            }
            case MessageEvents.PRE_UNLOCKED: {
                preUnlockedListeners.remove(listener);
                break;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deregisterMessageEventListener");
    }

    /**
   */
    public MessageProcessorSearchResults getSearchResults()
    {
        return searchResults;
    }

    /**
     * @param list
     */
    public void setSearchResults(MessageProcessorSearchResults list)
    {
        searchResults = list;
    }

    /**
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage
     */
    @Override
    public long calculateWaitTimeUpdate(long timeNow)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "calculateWaitTimeUpdate");

        long calculatedWaitTime = getMessageItem().calculateWaitTimeUpdate(timeNow);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "calculateWaitTimeUpdate", Long.valueOf(calculatedWaitTime));

        return calculatedWaitTime;
    }

    /**
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage
     */
    @Override
    public long getAggregateWaitTime()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getAggregateWaitTime");

        long aggregateMessageWaitTime = getLatestWaitTimeUpdate() + getMessageItem().getAggregateWaitTime();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(
                       tc, "getAggregateWaitTime", Long.valueOf(aggregateMessageWaitTime));

        return aggregateMessageWaitTime;
    }

    /**
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage
     */
    @Override
    public long getLatestWaitTimeUpdate()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getLatestWaitTimeUpdate");

        long timeNow = java.lang.System.currentTimeMillis();

        long localWaitTime = timeNow - getMessageItem().getCurrentMEArrivalTimestamp();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getLatestWaitTimeUpdate", Long.valueOf(localWaitTime));

        return localWaitTime;
    }

    /**
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#isToBeStoredAtSendTime()
     *      Not implemented for references
     */
    @Override
    public boolean isToBeStoredAtSendTime()
    {
        return false;
    }

    /**
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#setStoreAtSendTime(boolean)
     *      Not implemented for references
     */
    @Override
    public void setStoreAtSendTime(boolean store)
    {
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

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#getControlAdapter()
     */
    @Override
    public ControlAdapter getControlAdapter()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getControlAdapter");

        if (controlAdapter == null)
        {
            createControlAdapter();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getControlAdapter", controlAdapter);

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
            SibTr.entry(tc, "createControlAdapter");

        DestinationHandler dh = null;
        try
        {
            ReferenceStream rs = getReferenceStream();
            if (rs instanceof MessageReferenceStream)
            {
                ItemStream is = rs.getItemStream();
                if (is instanceof PubSubMessageItemStream)
                {
                    dh = ((PubSubMessageItemStream) is).getDestinationHandler();
                    controlAdapter = new QueuedMessage(this, dh, rs);
                }
            }
        } catch (Exception e)
        {
            // FFDC
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.items.MessageItemReference.createControlAdapter",
                                        "1:1266:1.147",
                                        this);

            SibTr.exception(tc, e);
        }

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

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.ControllableResource#dereferenceControlAdapter()
     */
    @Override
    public void dereferenceControlAdapter()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "dereferenceControlAdapter");

        if (controlAdapter != null)
        {
            controlAdapter.dereferenceControllable();
            controlAdapter = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "dereferenceControlAdapter");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#getGuaranteedStreamUuid()
     */
    @Override
    public SIBUuid12 getGuaranteedStreamUuid()
    {
        return getMessageItem().getGuaranteedStreamUuid();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#setGuaranteedStreamUuid(com.ibm.ws.sib.utils.SIBUuid12)
     */
    @Override
    public void setGuaranteedStreamUuid(SIBUuid12 uuid)
    {
        getMessageItem().setGuaranteedStreamUuid(uuid);
        return;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#getReportCOD()
     */
    @Override
    public Byte getReportCOD()
    {
        return getMessageItem().getReportCOD();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#isFromRemoteME()
     */
    @Override
    public boolean isFromRemoteME()
    {
        return getMessageItem().isFromRemoteME();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#updateStatisticsMessageWaitTime(long)
     */
    @Override
    public long updateStatisticsMessageWaitTime()
    {
        //Not implemented for references
        return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#beforeCompletion(com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    @Override
    public void beforeCompletion(TransactionCommon arg0)
    {
        // Nothing to do, we just have this so that other layers don't need to know it they
        // have a message or a reference
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.transactions.TransactionCallback#afterCompletion(com.ibm.ws.sib.msgstore.transactions.Transaction, boolean)
     */
    @Override
    public void afterCompletion(TransactionCommon arg0, boolean arg1)
    {
        //PM38052 release the reference to the JsMessage, for assuredPersistent we
        // know that the persistent representation is now stable. For ReliablePersistent it might be
        releaseJsMessage();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage#isReDriven()
     */
    @Override
    public boolean isReavailable()
    {
        return reavailable;
    }

    @Override
    public int guessRedeliveredCount()
    {
        int count = 0;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "guessRedeliveredCount");

        count = super.guessUnlockCount() + (int) rmeUnlockCount;
        try {
            count += super.getPersistedRedeliveredCount();
        } catch (MessageStoreException e) {
            // Ignore the exception, and return the count.
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "guessRedeliveredCount", Integer.valueOf(count));

        return count;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.AbstractItem#eventRestored()
     */
    @Override
    public void eventRestored() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "eventRestored");
        super.eventRestored();
        // After a restart, if the referredItem is not obtained before a postCommit
        // then the item can not be obtained. Therefore we need to always obtain
        // the item (msgstore recreate their links in the following method).
        // Defect 302341
        getMessageItem();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "eventRestored");
    }

    @Override
    public void unlockMsg(long lockID, Transaction transaction, boolean incrementUnlock) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "unlockMsg");

        if (failedInitInRestore)
        {
            //we can only register the listeners if we are fully initialized
            if (stream instanceof DurableSubscriptionItemStream)
            {
                stream.registerListeners(this);
            }
            failedInitInRestore = false;
        }

        if (incrementUnlock)
        {
            redeliveryCountReached = false;

            try {
                preUnlockedListeners.messageEventOccurred(this, transaction);
          
            } catch (SIException e) {
                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.store.items.MessageItemReference.unlockMsg",
                                            "1:1450:1.147",
                                            this);

                SibTr.exception(tc, e);

                redeliveryCountReached = false;
            }

        }

        if (!redeliveryCountReached)
        {
            unlock(lockID, transaction, incrementUnlock);

            MessageItem item = getMessageItem();
            SIMPItemStream itemStream = (SIMPItemStream) item.getItemStream();
            BaseDestinationHandler bdh = item.getDestinationHandler(false, itemStream);

            // proceede if and only if the redeliverycount column exists
            if (bdh.isRedeliveryCountPersisted() && bdh.getMessageProcessor().getMessageStore().isRedeliveryCountColumnAvailable())
                persistRedeliveredCount(guessRedeliveredCount());
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
            SibTr.entry(tc, "setRedeliveryCountReached");
            SibTr.exit(tc, "setRedeliveryCountReached");
        }
        redeliveryCountReached = true;
    }

    @Override
    public boolean isReference()
    {
        return true;
    }

    @Override
    public void releaseJsMessage()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "releaseJsMessage");

        // We check to see if it is safe to remove a non persistent msg now. i.e. the asynchronous
        // gap between the commit add and the spilling to disk for store_maybe.
        // A best effort nonpersistent msg wil never get spilled to disk so we need to
        // check that we don't remove a reference to one.
        if (getReliability().compareTo(Reliability.BEST_EFFORT_NONPERSISTENT) > 0 && isPersistentRepresentationStable())
        {
            synchronized (this) // we don't want to take the msg away from underneath someone
            {
                jsMsg = null;
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "releaseJsMessage");
    }

    @Override
    public void markHiddenMessage(boolean hiddenMessage)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "markHiddenMessage", Boolean.valueOf(hiddenMessage));

        getMessageItem().markHiddenMessage(hiddenMessage);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "markHiddenMessage");
    }

    @Override
    public boolean isHidden()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "isHidden");

        boolean isHidden = getMessageItem().isHidden();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "isHidden", Boolean.valueOf(isHidden));

        return isHidden;
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

        getMessageItem().setHiddenExpiryTime(expiryTime);

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
        }

        long hiddenExpiryTime =
                        getMessageItem().getHiddenExpiryTime();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
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
        // Not currently used
        return 0L;
    }

    @Override
    public boolean isRemoteGet()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isRemoteGet");
            SibTr.exit(tc, "isRemoteGet", Boolean.FALSE);
        }

        // A remote getter always has a MessageItem rather than a reference out on the RME,
        // even when dealing with a subscription.
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
        // Noop for a message reference
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
        // Noop for a message reference
        return null;
    }

    @Override
    public void setRMEUnlockCount(long rmeUnlockCount)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "setRMEUnlockCount", Long.valueOf(rmeUnlockCount));
            SibTr.exit(this, tc, "setRMEUnlockCount");
        }
        this.rmeUnlockCount += rmeUnlockCount;
    }

    /**
     * @see com.ibm.ws.sib.msgstore.AbstractItem#getDeliveryDelay()
     * 
     *      Return the maximum time this reference should be in locked
     *      state before its unlocked for consumption,i.e message will
     *      be available for consumption
     *      after getDeliveryDelay() time
     * 
     *      The delivery Delay of a message reference is always the same as its
     *      referenced item.
     */
    @Override
    public long getDeliveryDelay() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getDeliveryDelay");

        long deliveryDelay = getMessageItem().getDeliveryDelay();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "getDeliveryDelay", new Long(deliveryDelay));

        return deliveryDelay;
    }
}
