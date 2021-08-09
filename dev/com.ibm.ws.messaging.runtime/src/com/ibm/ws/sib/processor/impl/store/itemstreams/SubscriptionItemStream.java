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
package com.ibm.ws.sib.processor.impl.store.itemstreams;

import java.io.IOException;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.DeliveryDelayDeleteFilter;
import com.ibm.ws.sib.msgstore.ItemReference;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NotInMessageStore;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.Statistics;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.processor.SIMPConstants;
import com.ibm.ws.sib.processor.impl.ConsumerDispatcher;
import com.ibm.ws.sib.processor.impl.interfaces.DestinationHandler;
import com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener;
import com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage;
import com.ibm.ws.sib.processor.impl.store.MessageEvents;
import com.ibm.ws.sib.processor.impl.store.SIMPTransactionManager;
import com.ibm.ws.sib.processor.utils.LockManager;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.transactions.TransactionCommon;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * @author caseyj
 * 
 *         Non-durable subscription class.
 */
public class SubscriptionItemStream extends MessageReferenceStream implements MessageEventListener
{
    private static final TraceNLS nls =
                    TraceNLS.getTraceNLS(SIMPConstants.RESOURCE_BUNDLE);

    private static final int BATCH_DELETE_SIZE = 10;

    /**
     * Trace for the component
     */
    private static final TraceComponent tc =
                    SibTr.register(
                                   SubscriptionItemStream.class,
                                   SIMPConstants.MP_TRACE_GROUP,
                                   SIMPConstants.RESOURCE_BUNDLE);

    private ConsumerDispatcher consumerDispatcher = null;

    private boolean unableToOrder = false;
    private PersistentTranId currentTranId = null;

    /**
     * Indicates whether the itemstream is awaiting deletion once all state
     * associated with it such as indoubt messages etc has been cleared up.
     */
    protected boolean toBeDeleted = false;
    private DestinationHandler destinationHandler = null;
    private boolean removingAvailableReferences = false;
    private final LockManager _subscriptionLockManager = new LockManager();

    /**
     * Flag to indicate that the consumers from this itemStream are currently blocked
     * by the ConsumerDispatcher. We use this flag in the controllables to show that
     * a message isn't actually available to consumers at this point
     */
    private volatile boolean _blocked = false;

    public SubscriptionItemStream()
    {
        super();
    }

    /**
     * Create a non-durable subscription. Adds itself to the given parent
     * ItemStream.
     * 
     * @param parentItemStream The ItemStream to add the consructed object to.
     * @param txManager The transaction manager to use to obtain transactions
     * @param subscriberId
     */
    public SubscriptionItemStream(PubSubMessageItemStream parentItemStream,
                                  SIMPTransactionManager txManager)
        throws OutOfCacheSpace, MessageStoreException
    {
        super();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(
                        tc,
                        "SubscriptionItemStream",
                        new Object[] { parentItemStream, txManager });

        //We never want non-durable subscriptions to survive a restart 172021.3    
        setStorageStrategy(STORE_MAYBE);

        Transaction transaction = txManager.createAutoCommitTransaction();

        parentItemStream.addReferenceStream(this, transaction);
        parentItemStream.incrementReferenceStreamCount();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "SubscriptionItemStream", this);
    }

    /**
     * Set the consumerDispatcher object to which this itemstream belongs
     * 
     * @param the consumerDispatcher object
     */
    public void setConsumerDispatcher(ConsumerDispatcher consumerDispatcher)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setConsumerDispatcher", consumerDispatcher);

        this.consumerDispatcher = consumerDispatcher;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setConsumerDispatcher");
    }

    /**
     * Get the consumerDispatcher object to which this itemstream belongs
     * 
     * @return the consumerDispatcher object
     */
    public ConsumerDispatcher getConsumerDispatcher()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getConsumerDispatcher");
            SibTr.exit(tc, "getConsumerDispatcher", consumerDispatcher);
        }
        return consumerDispatcher;
    }

    /** Prints debug information to the XML writer */
    @Override
    public void xmlWriteOn(FormattedWriter writer) throws IOException
    {
        if (consumerDispatcher != null)
        {
            writer.newLine();
            writer.taggedValue("consumerDispatcher", consumerDispatcher.toString());
        }
    }

    /**
     * @see com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener#registerForEvents(SIMPMessage)
     */
    @Override
    public void registerForEvents(SIMPMessage msg)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "registerForEvents");

        msg.registerMessageEventListener(MessageEvents.POST_COMMIT_REMOVE, this);
        msg.registerMessageEventListener(MessageEvents.POST_ROLLBACK_REMOVE, this);
        msg.registerMessageEventListener(MessageEvents.POST_COMMIT_ADD, this);
        msg.registerMessageEventListener(MessageEvents.POST_ROLLBACK_ADD, this);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerForEvents");
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
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "messageEventOccurred", new Object[] { new Integer(event), msg, tran });

        switch (event)
        {
            case MessageEvents.POST_COMMIT_REMOVE:
            case MessageEvents.POST_ROLLBACK_REMOVE:
            case MessageEvents.UNLOCKED:
                // Reset ordered tran id
                if (currentTranId != null)
                {
                    currentTranId = null;
                    break;
                }
            case MessageEvents.POST_COMMIT_ADD:
            case MessageEvents.POST_ROLLBACK_ADD:

                deleteIfPossible(true);
                break;

            default: {
                final SIErrorException e = new SIErrorException(
                                nls.getFormattedMessage(
                                                        "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                                                        new Object[] {
                                                                      "com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream",
                                                                      "1:241:1.54" },
                                                        null));

                FFDCFilter.processException(
                                            e,
                                            "com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream.messageEventOccurred",
                                            "1:247:1.54",
                                            this);

                SibTr.exception(tc, e);
                SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0001",
                            new Object[] {
                                          "com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream",
                                          "1:254:1.54" });

                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(tc, "messageEventOccurred", e);

                throw e;

            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "messageEventOccurred");
    }

    /**
     * Method deleteIfPossible.
     * 
     * <p>Test whether the item stream is marked as awaiting deletion
     * and if so, check if there are still items on the itemstream that
     * will block its removal.</p>
     * 
     * Any failures to delete the subscription will queue the request to the
     * Asynch deletion thread.
     */
    public void deleteIfPossible(boolean startAsynchThread)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "deleteIfPossible", new Boolean(startAsynchThread));

        // Lock exclusively to stop any further messages from being added.
        _subscriptionLockManager.lockExclusive();

        try
        {
            if (destinationHandler == null)
            {
                // Get the transaction manager for the message processor
                PubSubMessageItemStream mis = (PubSubMessageItemStream) getItemStream();
                destinationHandler = (DestinationHandler) mis.getItemStream();
            }

            if (toBeDeleted ||
                destinationHandler.isToBeDeleted())
            {
                SIMPTransactionManager txManager = destinationHandler.getTxManager();
                boolean complete = false;

                try
                {
                    Statistics statistics = null;
                    try
                    {
                        statistics = getStatistics();
                    } catch (NotInMessageStore e)
                    {
                        // No FFDC Code needed

                        // This exception can be thrown if a Durable subscription was on a destination that 
                        // was deleted and it was a durable subscription which was already marked for deletion
                        complete = true;
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(tc, "deleteIfPossible", "Subscription already deleted");
                        return;
                    }

                    long countOfAvailableItems = statistics.getAvailableItemCount();

                    /*
                     * There may be itemreference which is locked for
                     * delivery delay.Those messages must also be deleted.
                     * Hence get the locked message count.Its imp to note that
                     * all the messages will not be deleted only the messages which
                     * are available or messages which are locked for delivery delay
                     * will be deleted.That check is done in removeAllAvailableReferences()
                     * where a DeliveryDelay filter is used.
                     */
                    long countOfLockedMessages = statistics.getLockedItemCount();

                    if (countOfAvailableItems > 0 || countOfLockedMessages > 0)
                    {
                        //Remove any available references from the subscription.
                        try
                        {
                            removeAllAvailableReferences();
                        } catch (SIResourceException e1)
                        {
                            // No FFDC code needed
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(tc, "deleteIfPossible", e1);
                            // Exit so finally block run
                            return;
                        }
                    }

                    statistics = getStatistics();

                    long countOfTotalItems = statistics.getTotalItemCount();
                    if (countOfTotalItems == 0)
                    {
                        //All references are gone.  The subscription can be removed.

                        if (destinationHandler.isToBeDeleted())
                        {
                            //It may now be possible to clean up the destination.  Have a go!
                            destinationHandler.getDestinationManager().markDestinationAsCleanUpPending(destinationHandler);
                        }

                        try
                        {
                            PubSubMessageItemStream parentItemStream = (PubSubMessageItemStream) getItemStream();
                            remove(txManager.createAutoCommitTransaction(), NO_LOCK_ID);
                            parentItemStream.decrementReferenceStreamCount();
                            complete = true;
                        } catch (MessageStoreException e)
                        {
                            FFDCFilter.processException(
                                                        e,
                                                        "com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream.deleteIfPossible",
                                                        "1:364:1.54",
                                                        this);

                            SibTr.exception(tc, e);
                            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                        new Object[] {
                                                      "com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream",
                                                      "1:371:1.54",
                                                      e });

                        }

                    }
                } catch (Exception e)
                {
                    // No FFDC code needed
                    // Log that this exception occured
                    SibTr.exception(tc, e);
                } finally
                {
                    if (!complete)
                    {
                        destinationHandler.getDestinationManager().addSubscriptionToDelete(this);
                        if (startAsynchThread)
                            destinationHandler.getDestinationManager().startAsynchDeletion();
                    }
                }
            }
        } catch (MessageStoreException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream.deleteIfPossible",
                                        "1:400:1.54",
                                        this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream",
                                      "1:407:1.54",
                                      e });

        } finally
        {
            // Finally unlock the lock manager
            _subscriptionLockManager.unlockExclusive();
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "deleteIfPossible");
        return;
    }

    /**
     * Method isToBeDeleted.
     * 
     * @return boolean
     */
    public boolean isToBeDeleted()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isToBeDeleted");
            SibTr.exit(tc, "isToBeDeleted", Boolean.valueOf(toBeDeleted));
        }

        return toBeDeleted;
    }

    /**
     * Mark this itemstream as awaiting deletion
     */
    public void markAsToBeDeleted()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "markAsToBeDeleted");

        toBeDeleted = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "markAsToBeDeleted");
    }

    /**
     * Removes all available reference items from the reference stream in batches
     * 
     * @throws MessageStoreException
     */

    public void removeAllAvailableReferences() throws SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "removeAllAvailableReferences");

        LocalTransaction transaction = null;
        ItemReference itemReference = null;

        try
        {
            // Get the transaction manager for the message processor
            PubSubMessageItemStream mis = (PubSubMessageItemStream) getItemStream();
            DestinationHandler dh = (DestinationHandler) mis.getItemStream();
            SIMPTransactionManager txManager = dh.getTxManager();

            removingAvailableReferences = true;

            if (txManager != null)
            {
                /* Create new transaction */
                transaction = txManager.createLocalTransaction(true);
                int countOfItems = 0;

                //Remove all the references in batches to avoid large UOWs in the msgstore
                do
                {
                    do
                    {
                        /*
                         * Introduce a new DeliveryDelayDeleteFilter which basically retrieves
                         * the messages which are either available or which are locked
                         * for delivery delay It is just a marker class.Earlier to
                         * delivery delay feature null was being passed
                         */
                        itemReference = this.removeFirstMatching(new DeliveryDelayDeleteFilter(), (Transaction) transaction);
                        if (itemReference != null)
                            countOfItems++;

                    } while ((itemReference != null) && (countOfItems < BATCH_DELETE_SIZE));

                    //Commit the batch
                    if (countOfItems > 0)
                    {
                        transaction.commit();

                        /* Create new transaction */
                        if (itemReference != null)
                        {
                            transaction = txManager.createLocalTransaction(true);
                        }

                        countOfItems = 0;
                    }

                } while (itemReference != null);
            }
        } catch (MessageStoreException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream.removeAllAvailableReferences",
                                        "1:509:1.54",
                                        this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream",
                                      "1:516:1.54",
                                      e });

            if (transaction != null)
                handleRollback(transaction);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "removeAllAvailableReferences");

            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream",
                                                                  "1:530:1.54",
                                                                  e },
                                                    null),
                            e);
        } catch (SIException e)
        {
            FFDCFilter.processException(
                                        e,
                                        "com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream.removeAllAvailableReferences",
                                        "1:540:1.54",
                                        this);

            SibTr.exception(tc, e);
            SibTr.error(tc, "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                        new Object[] {
                                      "com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream",
                                      "1:547:1.54",
                                      e });

            if (transaction != null)
                handleRollback(transaction);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "removeAllAvailableReferences");

            throw new SIResourceException(
                            nls.getFormattedMessage(
                                                    "INTERNAL_MESSAGING_ERROR_CWSIP0002",
                                                    new Object[] {
                                                                  "com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream",
                                                                  "1:561:1.54",
                                                                  e },
                                                    null),
                            e);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "removeAllAvailableReferences");
    }

    /**
     * This method checks to see if rollback is required and
     * throws an SIErrorException if rollback failed.
     * 
     * @param transaction The transaction to rollback.
     */
    private void handleRollback(LocalTransaction transaction)
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
                                            "com.ibm.ws.sib.processor.impl.store.itemstreams.SubscriptionItemStream.handleRollback",
                                            "1:594:1.54",
                                            this);

                SibTr.exception(tc, e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "handleRollback");
    }

    /**
     * @see com.ibm.ws.sib.processor.impl.store.itemstreams.MessageReferenceStream#registerListeners(SIMPMessage)
     */
    @Override
    public void registerListeners(SIMPMessage msg)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "registerListeners", msg);

        if (!removingAvailableReferences)
        {
            // Register for events on the consumer dispatcher?   
            consumerDispatcher.registerForEvents(msg);

            // Register for events on this itemstream
            this.registerForEvents(msg);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "registerListeners");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.processor.impl.interfaces.MessageEventListener#registerForEventsPostAddItem(com.ibm.ws.sib.processor.impl.interfaces.SIMPMessage)
     */
    public void registerForEventsPostAddItem(SIMPMessage msg)
    {}

    /**
     * Returns the lock manager associated with this subscription
     */
    public LockManager getSubscriptionLockManager()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getSubscriptionLockManager");
            SibTr.exit(tc, "getSubscriptionLockManager", _subscriptionLockManager);
        }
        return _subscriptionLockManager;
    }

    /**
     * @param id
     */
    @Override
    public void setCurrentTransaction(SIMPMessage msg, boolean isInDoubtOnRemoteConsumer)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setCurrentTransaction", new Object[] { msg, Boolean.valueOf(isInDoubtOnRemoteConsumer) });

        if (currentTranId != null && !msg.getTransactionId().equals(currentTranId))
        {
            unableToOrder = true;

            // PK69943 Do not output a CWSIP0671 message here, as we do not know if
            // the destination is ordered or not. Leave that to the code that later checks unableToOrder
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) {
                SibTr.debug(tc, "Unable to order. Transaction: " + msg.getTransactionId() + " Current:" + currentTranId);
            }

        }
        else
            currentTranId = msg.getTransactionId();

        if (isInDoubtOnRemoteConsumer)
        {
            // Register callbacks to notify us when a remote consumer transaction completes
            msg.registerMessageEventListener(MessageEvents.UNLOCKED, this);
            msg.registerMessageEventListener(MessageEvents.POST_COMMIT_REMOVE, this);
        }
        else
        {
            // Register callbacks to notify us when a local consumer transaction completes
            msg.registerMessageEventListener(MessageEvents.POST_COMMIT_REMOVE, this);
            msg.registerMessageEventListener(MessageEvents.POST_ROLLBACK_REMOVE, this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setCurrentTransaction");
    }

    /**
     * @param id
     */
    public PersistentTranId getOrderedActiveTran()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "getOrderedActiveTran");
            SibTr.exit(tc, "getOrderedActiveTran", currentTranId);
        }
        return currentTranId;
    }

    /**
     * @param id
     */
    public boolean isUnableToOrder()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isUnableToOrder");
            SibTr.exit(tc, "isUnableToOrder", Boolean.valueOf(unableToOrder));
        }
        return unableToOrder;
    }

    // Mark the whole itemStream as blocked (so that the MBeans show the 'real' state of 
    // the items
    public void setBlocked(boolean blocked)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "setBlocked", new Object[] { Boolean.valueOf(blocked), Boolean.valueOf(_blocked) });

        _blocked = true;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "setBlocked");
    }

    public boolean isBlocked()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(tc, "isBlocked");
            SibTr.exit(tc, "isBlocked", Boolean.valueOf(_blocked));
        }

        return _blocked;
    }
}
