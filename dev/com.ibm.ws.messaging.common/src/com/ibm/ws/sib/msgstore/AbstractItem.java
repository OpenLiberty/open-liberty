package com.ibm.ws.sib.msgstore;

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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.sib.msgstore.MessageStoreConstants.MaximumAllowedDeliveryDelayAction;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Defines the obligations of objects stored within the {@link MessageStore}. Only
 * instances of this class (or its subclasses) may be stored in the message store.
 * 
 * <p><b><u>Item Responsibilities </u></b><br>
 * Item responsibilities include:
 * <ul>
 * <li>Being dynamically creatable: so they must have public, no-argument
 * constructors. {@link #AbstractItem()}.</li>
 * <li>Replying their storage strategy {@link #getStorageStrategy()}.
 * </li>
 * <li>Yielding their state as opaque binary objects (e.g. byte array).
 * </li>
 * <li>Restoring their state from opaque binary objects.</li>
 * </ul>
 * </p>
 * <p> Store items appear to the object store as a byte array and a classname.</p>
 * 
 * <p>
 * The state model for put/get indicates when events are sent.
 * <IMG src="doc-files/AbstractItemState.gif">
 * </p>
 * <p>
 * Note that an item is considered to be 'in the store' from the time it is
 * presented to an {@link ItemStream} until the point at which the 'add' is
 * rolled back, or a 'remove' is committed.
 * </p>
 * <p>
 * <b><u>Abstract Item Identity</u></b><br>
 * Each Abstract Item which is in the store has an associated ID. Once the item
 * has been removed from the store, the ID is no longer valid ({@link #getID()} will
 * return {@link #NO_ID}). If an Item is removed from the store, and then re-added, it will be given an new
 * ID.
 * </p>
 */
public abstract class AbstractItem
{

    /*
     * create an inner class so we can identify the synchronization
     * lock when we do lock analysis. Instances of this class are only
     * used to synchronize the state of the membership variable.
     */
    private static final class MembershipLock {}

    private final MembershipLock _membershipLock = new MembershipLock();

    /**
     * Value used for priority if none is specified by subclass implementors.
     */
    public static final int DEFAULT_PRIORITY = 5;

    /**
     * Value to be set in 'maximum time in store' when item should never expire. This
     * value will be used if no value is specified by the subclass implementor.
     */
    public static final long NEVER_EXPIRES = -2;

    public static final long NO_ID = -1;

    /**
     * Value used to indicate that an item is not locked.
     */
    public static final long NO_LOCK_ID = -2;

    /**
     * Value to indicate that an item is locked for Delivery Delay scenario
     */
    public static final long DELIVERY_DELAY_LOCK_ID = -56789L;

    /**
     * Value used for priority if none is specified by subclass implementors.
     */
    public static final int DEFAULT_DELIVERY_DELAY = 0;

    /**
     * value used to indicate that the item must be persistent
     */
    public static final int STORE_ALWAYS = 4;

    /**
     * value used to indicate that the item is stored eventually
     */
    public static final int STORE_EVENTUALLY = 3;

    /**
     * value used to indicate that the item may be stored
     */
    public static final int STORE_MAYBE = 2;

    /**
     * value used to indicate that the item is not stored in any way
     */
    public static final int STORE_NEVER = 1;

    private static TraceComponent tc = SibTr.register(AbstractItem.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    // store reference to membership
    private Membership _membership = null;

    /**
     * A public no-argument constructor so that items may be dynamically
     * created. All subclasses should provide such a subclass.
     */
    public AbstractItem() {}

    /*
     * @return {@link MessageStore.Membership}
     */
    final Membership _getMembership()
    {
        synchronized (_membershipLock)
        {
            return _membership;
        }
    }

    /*
     * Internal use only
     * 
     * @param membership {@link MessageStore.Membership}
     */
    final void _setMembership(final Membership membership)
    {
        synchronized (_membershipLock)
        {
            _membership = membership;
        }
    }

    /**
     * Reply true if the receiver can be expired without informing the user
     * of the message store. Silent expiry allows efficiency gains to be
     * made, and so is the default option. Subclasses can override the method
     * to ensure notification via {@link #eventExpiryNotification(Transaction)}.
     * <p>Please note that the return value may be cached so that items can be
     * silently expired without restoring them from persistent storage.</p>
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the item.</p>
     * 
     * @return true if silent expiry is allowed.
     */
    public boolean canExpireSilently()
    {
        return true;
    }

    /**
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the item.</p>
     * 
     * @return true if the data in the receiver could change between the
     *         item being presented to an {@link ItemStream} and the commit of the
     *         transaction under which it is put. Return false if this is
     *         <b>definitely</b> not the case.
     *         <p>The default implementation returns false.</p>
     *         <p> Returning false will allow the message store to optimise the
     *         performance of persistence under some circumstances.</p>
     */
    public boolean deferDataPersistence()
    {
        return false;
    }

    /**
     * Notification that the item has expired.
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the item. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     * <p>This method may not be called if silent expiry has allowed by
     * returning true to {@link #canExpireSilently()}. It may not be called
     * if there is a system crash after removal of the item from persistent
     * store and before this method is called.</p>
     * 
     * @param transaction the {@link Transaction} under which the event has occurred
     */
    public void eventExpiryNotification(Transaction transaction) throws SevereMessageStoreException { /* 179365.3 */}

    /**
     * Notification that all actions enlisted in the transaction under which an
     * add has been performed have been commited.
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the item. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     * 
     * @param transaction the {@link Transaction} under which the
     *            event has occurred
     *            <p>Thismethod is not called in a synchronized block.
     *            The state of the item cannot be relied upon since thread
     *            switching may occur before the callback is invoked.</p>
     */
    public void eventPostCommitAdd(Transaction transaction) throws SevereMessageStoreException {}

    /**
     * Notification that all actions enlisted in the transaction under which a
     * remove has been performed have been commited.
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the item. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     * 
     * @param transaction the {@link Transaction} under which the
     *            event has occurred
     *            <p>Thismethod is not called in a synchronized block.
     *            The state of the item cannot be relied upon since thread
     *            switching may occur before the callback is invoked.</p>
     */
    public void eventPostCommitRemove(Transaction transaction) throws SevereMessageStoreException
    {
        _setMembership(null);
    }

    /**
     * Notification that all actions enlisted in the transaction under which an
     * update has been performed have been commited.
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the item. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     * 
     * @param transaction the {@link Transaction} under which the
     *            event has occurred
     *            <p>Thismethod is not called in a synchronized block.
     *            The state of the item cannot be relied upon since thread
     *            switching may occur before the callback is invoked.</p>
     */
    public void eventPostCommitUpdate(Transaction transaction) throws SevereMessageStoreException {}

    /**
     * Notification that all actions enlisted in the transaction under which an
     * add has been performed have been rolled back.
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the item. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     * 
     * @param transaction the {@link Transaction} under which the
     *            event has occurred
     *            <p>Thismethod is not called in a synchronized block.
     *            The state of the item cannot be relied upon since thread
     *            switching may occur before the callback is invoked.</p>
     */
    public void eventPostRollbackAdd(Transaction transaction) throws SevereMessageStoreException
    {
        _setMembership(null);
    }

    /**
     * Notification that all actions enlisted in the transaction under which a
     * remove has been performed have been rolled back.
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the item. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     * 
     * @param transaction the {@link Transaction} under which the
     *            event has occurred
     *            <p>Thismethod is not called in a synchronized block.
     *            The state of the item cannot be relied upon since thread
     *            switching may occur before the callback is invoked.</p>
     */
    public void eventPostRollbackRemove(Transaction transaction) throws SevereMessageStoreException {}

    /**
     * Notification that all actions enlisted in the transaction under which an
     * update has been performed have been rolled back.
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the item. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     * 
     * @param transaction the {@link Transaction} under which the
     *            event has occurred
     *            <p>Thismethod is not called in a synchronized block.
     *            The state of the item cannot be relied upon since thread
     *            switching may occur before the callback is invoked.</p>
     */
    public void eventPostRollbackUpdate(Transaction transaction) throws SevereMessageStoreException {}

    /**
     * Called to notify the item that a transactional add is
     * about to be committed.
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the item. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     * <p>This method will be called before either {@link #eventPostCommitAdd(Transaction)} or {@link #eventPostRollbackAdd(Transaction)} for the same transaction.
     * Between these two calls there will be no other event calls.
     * Ordering between this event
     * notification and any other event notification is not specified, and should neither
     * be assumed nor relied upon.
     * </p>
     * 
     * @param transaction the {@link Transaction} under which the
     *            event has occurred
     *            <p>This is called within a synchronized block and so may
     *            deadlock if any code invoked in an overriden method is synchronized.</p>
     */
    public void eventPrecommitAdd(final Transaction transaction) throws SevereMessageStoreException {}

    /**
     * Called to notify the item that a transactional remove is
     * about to be committed.
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the item. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     * <p>This method will be called before either {@link #eventPostCommitAdd(Transaction)} or {@link #eventPostRollbackRemove(Transaction)} for the same transaction.
     * Between these two calls there will be no other event calls.
     * Ordering between this event
     * notification and any other event notification is not specified, and should neither
     * be assumed nor relied upon.
     * </p>
     * <p>This is called within a synchronized block and so may
     * deadlock if any code invoked in an overriden method is synchronized.</p>
     * 
     * @param transaction the {@link Transaction} under which the
     *            event has occurred
     */
    public void eventPrecommitRemove(final Transaction transaction) throws SevereMessageStoreException {}

    /**
     * Called to notify the item that a transactional update is
     * about to be committed.
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the item. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     * <p>This method will be called before either {@link #eventPostCommitUpdate(Transaction)} or {@link #eventPostRollbackUpdate(Transaction)} for the same transaction.
     * Between these two calls there will be no other event calls.
     * Ordering between this event
     * notification and any other event notification is not specified, and should neither
     * be assumed nor relied upon.
     * </p>
     * <p>This is called within a synchronized block and so may
     * deadlock if any code invoked in an overriden method is synchronized.</p>
     * 
     * @param transaction the {@link Transaction} under which the
     *            event has occurred
     */
    public void eventPrecommitUpdate(final Transaction transaction) throws SevereMessageStoreException {}

    /**
     * Called to notify the item that its restoration has been completed. Note that
     * for Streams this does not neccessarily mean that the stream contents have been
     * reloaded (though these will be reloaded at first access).
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the item. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     */
    public void eventRestored() throws SevereMessageStoreException {}

    /**
     * Called to notify the item that a lock has
     * been removed.
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the item. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     */
    public void eventUnlocked() throws SevereMessageStoreException {}

    /**
     * Return the expiry start time or zero if the expiry delay should run from the
     * time at which the item is added to the expiry index.
     * 
     * @return the expiry start time.
     */
    public long getExpiryStartTime()
    {
        return 0; // Default expiry start is the time at which the expirable is added to the index
    }

    /**
     * @return (long) the unique ID for the item, {@link #NO_ID} if the receiver
     *         is not part of an {@link ItemStream}.
     *         <p>
     *         Note that an item is considered to be 'in the store' from the time it is
     *         presented to an {@link ItemStream} until the point at which the 'add' is
     *         rolled back, or a 'remove' is committed.
     *         </p>
     *         <p>
     *         If an Item is removed from the store, and then re-added, it will be given an new
     *         ID.
     *         </p>
     * @throws NotInMessageStore
     */
    public final long getID() throws NotInMessageStore
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getID");

        long id = NO_ID;
        Membership membership = _getMembership();
        if (null == membership)
        {
            throw new NotInMessageStore();
        }
        if (null != membership)
        {
            id = membership.getID();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getID", Long.valueOf(id));
        return id;
    }

    /**
     * Estimate the maximum in-memory size of the message in its most expensive
     * form (i.e. both fully-fluffed and encoded for persistence/transmission).
     * 
     * This allows the Message Store to keep better track of the amount of
     * heap space being consumed, than by using getPersistentDataSize.
     * This is the value which is used for the figuring out whether to spill (as well
     * as queue depth) and whether an item will fit into a batch for spilling or
     * persisting.
     * 
     * The method will be overridden by subclasses which can provide a useful
     * value, in particular MessageItem.
     * 
     * @return the approximate maximum in-memory size of the data in bytes.
     *         Defaults to 400 as that matches a standard messages ration of 4(ish)x the
     *         persistent size.
     */
    public int getInMemoryDataSize()
    {
        return 400;
    }

    /**
     * @return the lock ID if the item is currently locked, or {@link #NO_ID} if not locked.
     * @throws MessageStoreException
     */
    public final long getLockID() throws MessageStoreException
    {
        Membership membership = _getMembership();
        if (null == membership)
        {
            throw new NotInMessageStore();
        }
        return membership.getLockID();
    }

    /**
     * @return the persisted redelivered count value.
     * @throws MessageStoreException
     */
    public final int getPersistedRedeliveredCount() throws MessageStoreException
    {
        int count = 0;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "getPersistedRedeliveredCount");

        Membership thisItemLink = _getMembership();

        if (thisItemLink == null) {
            throw new NotInMessageStore();
        }

        count = thisItemLink.getPersistedRedeliveredCount();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) {
            SibTr.exit(tc, "getPersistedRedeliveredCount");
        }

        return count;
    }

    /**
     * @return maximum time, in milliseconds, the item may exist in the store
     *         before it becomes eligible for expiry. The default implementation
     *         returns the value NEVER_EXPIRES.
     */
    public long getMaximumTimeInStore()
    {
        return NEVER_EXPIRES;
    }

    protected final MessageStore getOwningMessageStore() throws NotInMessageStore
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getOwningMessageStore");

        Membership membership = _getMembership();
        if (null == membership)
        {
            throw new NotInMessageStore(); // 247513
        }
        MessageStore messageStore = membership.getMessageStore();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getOwningMessageStore", messageStore);
        return messageStore;
    }

    /**
     * <p>Reply the persistent state of the receiver as a byte array.
     * This will be called when the item is ready to persist itself to backing
     * store, and delays the cost of serialization until the last possible point
     * in time (usually pre-prepare of a transaction). If a transaction is aborted
     * then serialization may be avoided, and this method may not be called.</p>
     * The default implementation returns null to indicate that no data persistent
     * data is required. This may not be useful. Note that restore from both spill
     * and store may use the same restore method.</p>
     * <p>This method should be overridden by subclass implementors in order to
     * customize the behaviour of the item.</p>
     * <p>If this method throws a RuntimeException, the messaging engine may
     * shutdown. Encoding problems should be indicated using a checked exception.
     * 
     * @return byte array of data to store
     * @throws PersistentDataEncodingException indicating that a problem was
     *             encountered encoding the persistent data.
     */
    //FSIB0112b.ms.1
    public List<DataSlice> getPersistentData() throws PersistentDataEncodingException
    {
        return null;
    }

    /**
     * Reply the priority to use for the item.
     * <p>This method should be overridden by subclass implementors in order to
     * customize the behaviour of the item.</p>
     * 
     * @return the (int) priority of the item, which should be a number between
     *         zero and nine inclusive. The default implementation returns {@link #DEFAULT_PRIORITY}.
     */
    public int getPriority()
    {
        return DEFAULT_PRIORITY;
    }

    /**
     * Reply the Delivery Delay time set on the item,i.e message will be
     * available for consumption after getDeliveryDelay() time until then
     * the item will be in locked state
     * 
     * <p>This method should be overridden by subclass implementors in order to
     * customize the behaviour of the item.</p>
     * 
     * @return The delivery delay time set on the item
     */
    public long getDeliveryDelay() {
        return DEFAULT_DELIVERY_DELAY;
    }

    /**
     * Reply an integer representing the storage strategy to be used for the receiver.
     * This must be one of the values defined by the constants:
     * <ul>
     * <li>{@link #STORE_ALWAYS}</li>
     * <li>{@link #STORE_EVENTUALLY}</li>
     * <li>{@link #STORE_MAYBE}</li>
     * <li>{@link #STORE_NEVER}</li>
     * </ul>
     * 
     * <p>This method should be overridden by subclass implementors in order to
     * customize the behaviour of the item.</p>
     * 
     * @return the persistence strategy. The default implementation
     *         returns the value {@link #STORE_NEVER} indicating
     *         a non-persistent item.
     */
    public int getStorageStrategy()
    {
        return STORE_NEVER;
    }

    /**
     * @return transaction id or null
     */
    public final PersistentTranId getTransactionId()
    {
        PersistentTranId id = null;
        Membership membership = _getMembership();
        if (null != membership)
        {
            id = membership.getTransactionId();
        }
        return id;
    }

    /**
     * @return an approximation to the number of attempted get operations
     *         on the item that have been rolled back in the current session. The
     *         number is neither persistent, nor accurate, but can be used to
     *         indicate a dangerous cycle in processing a 'bad' item. Such occurrences
     *         may be indicated by an ever-increasing backout count.
     */
    public final int guessBackoutCount()
    {
        int count = 0;
        Membership membership = _getMembership();
        if (null != membership)
        {
            count = membership.guessBackoutCount();
        }
        return count;
    }

    /**
     * @return an approximation to the number of attempted lock operations
     *         on the item that have been rescinded (unlocked) in the current session. The
     *         number is neither persistent, nor accurate, but can be used to
     *         indicate a dangerous cycle in processing a 'bad' item. Such occurrences
     *         may be indicated by an ever-increasing unlock count.
     */
    public final int guessUnlockCount()
    {
        int count = 0;
        Membership membership = _getMembership();
        if (null != membership)
        {
            count = membership.guessUnlockCount();
        }
        return count;
    }

    public final boolean isAdding()
    {
        boolean is = false;
        Membership membership = _getMembership();
        if (null != membership)
        {
            is = membership.isAdding();
        }
        return is;
    }

    public final boolean isAvailable()
    {
        boolean is = false;
        Membership membership = _getMembership();
        if (null != membership)
        {
            is = membership.isAvailable();
        }
        return is;
    }

    /**
     * @return true if the receiver is an aggregate type (eg {@link ItemStream} or {@link ReferenceStream}. Return
     *         false otherwise.
     *         The default behaviour is to return false.
     */
    public boolean isCollection()
    {
        return false;
    }

    public final boolean isExpiring()
    {
        boolean is = false;
        Membership membership = _getMembership();
        if (null != membership)
        {
            is = membership.isExpiring();
        }
        return is;
    }

    public final boolean isInStore()
    {
        boolean is = false;
        Membership membership = _getMembership();
        if (null != membership)
        {
            is = membership.isInStore();
        }
        return is;
    }

    /**
     * @return true if the receiver is an instance of {@link Item}, false otherwise. Default
     *         implementation returns false.
     */
    public boolean isItem()
    {
        return false;
    }

    /**
     * @return true if the receiver is an instance of {@link ItemReference}, false otherwise. Default
     *         implementation returns false.
     */
    public boolean isItemReference()
    {
        return false;
    }

    /**
     * @return true if the receiver is an instance of {@link ItemStream}, false otherwise. Default
     *         implementation returns false.
     */
    public boolean isItemStream()
    {
        return false;
    }

    /**
     * @return true if the receiver is locked. False otherwise.
     *         Note that this will return true if the item is locked
     *         persistently.
     */
    public final boolean isLocked()
    {
        boolean is = false;
        Membership membership = _getMembership();
        if (null != membership)
        {
            is = membership.isLocked();
        }
        return is;
    }

    /**
     * Replies a flag used as a hint to permit the Message Store to optimise its
     * handling of the persistent data. If the persistent data is immutable, once
     * the persistent data has been queried, the Message Store can hang on to the
     * reference to the persistent data's byte array without having to take a copy,
     * regardless of the time at which the persistent data is actually accessed
     * subsequently.
     * 
     * @return true if the item's persistent data is immutable, false otherwise.
     *         The default implementation returns false.
     */
    public boolean isPersistentDataImmutable()
    {
        return false;
    }

    /**
     * Replies a flag used as a hint to permit the Message Store to optimise its
     * handling of the persistent data. If the persistent data is guaranteed never
     * to be updated and the item can cope with the persistent data being requested
     * at any time after the item has been added to the Message Store, the Message
     * Store may be able to delay requesting the item's persistent data until just
     * before it is required to be stored persistently.
     * 
     * @return true if the item's persistent data is never updated, false otherwise.
     *         The default implementation returns false.
     */
    public boolean isPersistentDataNeverUpdated()
    {
        return false;
    }

    // Feature SIB0112le.ms.1
    /**
     * This method allows an Item to detect if it has a stable representation
     * of it's data persisted to disk. If the Items data is in the process of
     * being written this method will return false until the writing has completed
     * and we have a consistent representation of the Item on disk. e.g. a
     * STORE_MAYBE Item will return false for this method until the Item has been
     * spilled to disk.
     * 
     * 
     * NOTE: For STORE_NEVER Items this method doesn't hold much meaning as
     * if no representation is ever going to be created it will always be
     * stable (even though it doesn't exist).
     * 
     * @return <UL>
     *         <LI>true - If the persistent representation is stable.</LI>
     *         <LI>false - If the persistent representation is in the process of being changed.</LI>
     *         </UL>
     */
    public final boolean isPersistentRepresentationStable()
    {
        boolean is = false;
        Membership membership = _getMembership();
        if (null != membership)
        {
            is = membership.isPersistentRepresentationStable();
        }
        return is;
    }

    /**
     * @return true if the receiver is locked persistently.
     */
    public final boolean isPersistentlyLocked()
    {
        boolean is = false;
        Membership membership = _getMembership();
        if (null != membership)
        {
            is = membership.isPersistentlyLocked();
        }
        return is;
    }

    /**
     * @return true if the receiver is an instance of {@link ReferenceStream}, false otherwise. Default
     *         implementation returns false.
     */
    public boolean isReferenceStream()
    {
        return false;
    }

    public final boolean isRemoving()
    {
        boolean is = false;
        Membership membership = _getMembership();
        if (null != membership)
        {
            is = membership.isRemoving();
        }
        return is;
    }

    public final synchronized boolean isUpdating()
    {
        boolean is = false;
        Membership membership = _getMembership();
        if (null != membership)
        {
            is = membership.isUpdating();
        }
        return is;
    }

    /**
     * Attempt to lock the item using the given lock ID. If the lock is
     * succesful then the item behaves as if it were succesfully locked
     * by a {@link LockingCursor} with the same lock ID. The lock is not
     * persistent.
     * NOTE: if the item is not in store then this method should return false
     * rather than throwing an exception. The MP guys are expecting that the
     * item should sometime be not-stored.
     * 
     * @param lockID
     * @return boolean true if successful
     * @throws SevereMessageStoreException
     * @throws MessageStoreException
     */
    public final boolean lockItemIfAvailable(long lockID) throws SevereMessageStoreException
    {
        boolean locked = false;
        Membership membership = _getMembership();
        if (null != membership)
        {
            locked = membership.lockItemIfAvailable(lockID);
        }
        return locked;
    }

    /**
     * Use this method to persist the lock currently active on the item.
     * Item MUST be locked.
     * 
     * @param transaction
     * @throws SevereMessageStoreException
     * @throws {@ProtocolException} Thrown if an add is attempted when the
     *         transaction cannot allow any further work to be added i.e. after
     *         completion of the transaction.
     * 
     * @throws {@TransactionException} Thrown if an unexpected error occurs.
     */
    public final void persistLock(final Transaction transaction) throws ProtocolException, TransactionException, SevereMessageStoreException
    {
        Membership membership = _getMembership();
        if (null == membership)
        {
            throw new NotInMessageStore();
        }
        membership.persistLock(transaction);
    }

    /**
     * Use this method to persist the redelivered count for the item.
     * 
     * @param redeliveredCount
     * @throws SevereMessageStoreException
     */
    public void persistRedeliveredCount(int redeliveredCount) throws SevereMessageStoreException
    {
        Membership thisItemLink = _getMembership();
        if (null == thisItemLink)
        {
            throw new NotInMessageStore();
        }
        thisItemLink.persistRedeliveredCount(redeliveredCount);
    }

    /**
     * If the receiver is currently 'in' an {@link ItemStream} then remove
     * it from the {@link ItemStream}. The removal is performed under the
     * aegis of a transaction, and is subject to the item being removable.
     * An item is removable if either:
     * <ul>
     * <li>The item is unlocked, and not part of a transaction.</li>
     * <li>The item is locked by the lock ID is
     * supplied as an argument to the remove. </li>
     * </ul>
     * If the item is not 'in' an {@link ItemStream} then no action is
     * taken.
     * <p>An Item is not removable if there are still {@link ItemReference}s
     * to it. An exception will be thrown.
     * </p>
     * 
     * If the item is locked then the lockID must be provided in order to
     * unlock the item.
     * If the item is locked, and the lockID is provided, then the
     * item will be removed without becoming visible to any other getter.
     * 
     * @param transaction under which the remove is to be performed.
     * @param lockID lockID (if any) under which the item was locked.
     * 
     * @throws MessageStoreException if the item could not be removed. For
     *             example, if the item is part of another transaction.
     */
    public final void remove(final Transaction transaction, final long lockID) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "remove");

        Membership membership = _getMembership();
        if (null == membership)
        {
            throw new NotInMessageStore();
        }
        else
        {
            membership.cmdRemove(lockID, transaction);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "remove");
    }

    /**
     * Request an update
     * 
     * @param transaction
     * @throws MessageStoreException
     */
    public final void requestUpdate(Transaction transaction) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "requestUpdate", transaction);

        Membership membership = _getMembership();
        if (null == membership)
        {
            throw new NotInMessageStore();
        }
        else
        {
            membership.requestUpdate(transaction);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "requestUpdate");
    }

    /**
     * Restore the state of the receiver from the data that was persisted.
     * The default implementation does nothing - to match the default
     * implementation of {@link #getPersistentData()}.
     * <p>This method should be overridden by subclass implementors in order to
     * customize the behaviour of the item.</p>
     * <p>If this method throws a RuntimeException, the messaging engine may
     * shutdown. Encoding problems should be indicated using a checked exception.
     * 
     * @param data - the data that was persisted on behalf of the item.
     * @throws PersistentDataEncodingException indicating that a problem was
     *             encountered restoring the persistent data. The item will continue to be used
     *             and the item's subclass implementation must handle the error.
     */
    //FSIB0112b.ms.1
    public void restore(final List<DataSlice> dataSlices) throws PersistentDataEncodingException, SevereMessageStoreException {}

    /**
     * Restore the state of the receiver from the data that was persisted,
     * only if the message is available in the message store
     * The default implementation throws UnsupportedOperationException.
     * <p>This method should be overridden by subclass implementors in order to
     * customize the behaviour of the item.</p>
     * <p>If this method throws a RuntimeException, the messaging engine may
     * shutdown. Encoding problems should be indicated using a checked exception.
     * 
     * @param data - the data that was persisted on behalf of the item.
     * @throws PersistentDataEncodingException indicating that a problem was
     *             encountered restoring the persistent data. The item will continue to be used
     *             and the item's subclass implementation must handle the error.
     */
    //668676.1
    protected void restoreIfMsgAvailable(final List<DataSlice> dataSlices) throws PersistentDataEncodingException, SevereMessageStoreException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Read the binary data for the Item from the pesistent store
     * and trigger a call to the restore() method. This allows the
     * implementing Item to release it's reference to it's binary
     * data at runtime to save on heap use. If the Item needs to
     * access the binary data again then this method is used to
     * trigger a reload from disk.
     * 
     * @param throwExceptionIfMsgNotAvailable Boolean to indicate whether exception
     *            has to be thrown if message not available that is used to decide whether
     *            to call restore or restoreIfMsgAvailable
     * @throws SevereMessageStoreException
     */
    // Feature SIB0112le.ms.1
    // 668676.1
    public void restoreData(boolean throwExceptionIfMsgNotAvailable) throws PersistentDataEncodingException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "restoreData", Boolean.valueOf(throwExceptionIfMsgNotAvailable));

        Membership membership = _getMembership();
        if (null == membership)
        {
            throw new NotInMessageStore();
        }
        else
        {
            List<DataSlice> dataSlices = membership.readDataFromPersistence();

            // Call the implementation back with the persistent data
            if (throwExceptionIfMsgNotAvailable)
                restore(dataSlices);
            else
                restoreIfMsgAvailable(dataSlices);

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "restoreData");
    }

    /**
     * Unlock an item locked under a non-persistent lock.
     * 
     * @param lockID
     * @throws MessageStoreException
     */
    public final void unlock(long lockID) throws MessageStoreException
    {
        unlock(lockID, null, true);
    }

    /**
     * @param lockID
     * @param transaction transaction for unlocking if lock is persistent,
     *            null if lock is not persistent. If non-null and lock is non-persistent
     *            then this argument is ignored.
     * @throws MessageStoreException
     */
    public final void unlock(long lockID, Transaction transaction) throws MessageStoreException
    {
        unlock(lockID, transaction, true);
    }

    /**
     * @param lockID
     * @param transaction transaction for unlocking if lock is persistent,
     *            null if lock is not persistent. If non-null and lock is non-persistent
     *            then this argument is ignored.
     * @param incrementUnlockCountIfNonpersistent whether to increment the unlock count for a nonpersistent lock
     * @throws MessageStoreException
     */
    public final void unlock(long lockID, Transaction transaction, boolean incrementUnlockCountIfNonpersistent) throws MessageStoreException
    {
        Membership membership = _getMembership();
        if (null == membership)
        {
            throw new NotInMessageStore();
        }
        membership.unlock(lockID, transaction, incrementUnlockCountIfNonpersistent);
    }

    /**
     * Request that the receiver prints its xml representation
     * (recursively) onto the given writer).
     * 
     * @param writer
     * @throws IOException
     * @throws NotInMessageStore
     */
    public final void xmlRequestWriteOn(FormattedWriter writer) throws IOException, NotInMessageStore
    {
        Membership membership = _getMembership();
        if (null == membership)
        {
            throw new NotInMessageStore();
        }
        if (null != membership)
        {
            membership.requestXmlWriteOn(writer);
            writer.flush();
        }
    }

    /**
     * Request that the receiver prints its xml representation
     * (recursively) onto standard out.
     * 
     * @throws IOException
     * @throws NotInMessageStore
     */
    public final void xmlRequestWriteOnSystemOut() throws IOException, NotInMessageStore
    {
        Membership membership = _getMembership();
        if (null == membership)
        {
            throw new NotInMessageStore();
        }
        if (null != membership)
        {
            FormattedWriter writer = new FormattedWriter(new OutputStreamWriter(System.out));
            membership.requestXmlWriteOn(writer);
            writer.flush();
        }
    }

    /**
     * An opportunity for the item to describe state during the
     * generation of XML output.
     * This method is for the item only. It should not call
     * xmlRequestWriteOn().
     * <p>Implementations of this method should call writer.newLine() before
     * writing any data. The data written will be indented relative to the
     * parent element in the XML.
     * 
     * @param writer
     * @throws IOException
     */
    public void xmlWriteOn(FormattedWriter writer) throws IOException {}

	/**
	 * Handle migration to Was V9.0.0 where a suspect delivery delay is discovered.
	 * @param action
	 */
	public void handleInvalidDeliveryDelayable(MaximumAllowedDeliveryDelayAction action) throws MessageStoreException, SIException {
		// Can be overriden, otherwise no action is taken.
	}
}
