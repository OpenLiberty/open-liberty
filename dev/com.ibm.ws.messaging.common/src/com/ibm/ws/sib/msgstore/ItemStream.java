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

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * A FIFO stream of items.
 * <p>An item stream is an ordered list of {@link AbstractItem}s. {@link AbstractItem} are added to
 * the head of the list and normally removed from the tail. This means that {@link AbstractItem}s are always retrieved in the order that they were put.</p>
 * <p>{@link AbstractItem}s are stored in order of priority, with those of the same priority
 * being stored in sequence order.
 * </p>
 * <p>Instances of this class will only store instances of {@link AbstractItem}. </p>
 * <p> {@link AbstractItem}s only expire when they are placed in ItemStreams. If an {@link AbstractItem} in an ItemStream expires, it will be removed from the
 * ItemStream.
 * </p>
 * 
 */
public class ItemStream extends AbstractItem
{
    private static TraceComponent tc = SibTr.register(ItemStream.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    /**
     * We insist on a no-argument constructor for reinstatement from
     * the database.
     */
    public ItemStream()
    {
        super();
    }

    /**
     * Add an {@link Item} to an item stream under a transaction. An Item
     * can only be added onto one stream at a time. .
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the stream. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     * 
     * @param item
     * @param lockID id under which the item will be locked when the add is
     *            complete, or {@link AbstractItem#NO_LOCK_ID} if the added item is to
     *            be unlocked.
     * @param transaction
     * @throws SevereMessageStoreException
     * @throws {@link OutOfCacheSpace} if there is not enough space in the
     *         unstoredCache and the storage strategy is {@link AbstractItem#STORE_NEVER}.
     * 
     * @throws {@link StreamIsFull} if the size of the stream would exceed the
     *         maximum permissable size if an add were performed.
     * 
     * @throws {@ProtocolException} Thrown if an add is attempted when the
     *         transaction cannot allow any further work to be added i.e. after
     *         completion of the transaction.
     * 
     * @throws {@TransactionException} Thrown if an unexpected error occurs.
     * 
     * @throws {@PersistenceException} Thrown if a database error occurs.
     */
    public void addItem(final Item item, long lockID, final Transaction transaction)
                    throws ProtocolException, OutOfCacheSpace, StreamIsFull, TransactionException, PersistenceException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "addItem");

        ItemCollection ic = ((ItemCollection) _getMembership());
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "addItem");
            throw new InvalidAddOperation("STREAM_NOT_STORED_SIMS0004", new Object[] { item, this });
        }

        if (null != item._getMembership())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "addItem");
            throw new InvalidAddOperation("STREAM_ADD_CONFLICT_SIMS0005", new Object[] { item, this });
        }

        ic.addItem(item, lockID, transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "addItem");
    }

    /**
     * Add an {@link Item} to an item stream under a transaction. An Item
     * can only be added onto one stream at a time. .
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the stream. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     * 
     * @param item
     * @param transaction
     * @throws SevereMessageStoreException
     * @throws {@link OutOfCacheSpace} if there is not enough space in the
     *         unstoredCache and the storage strategy is {@link AbstractItem#STORE_NEVER}.
     * 
     * @throws {@link StreamIsFull} if the size of the stream would exceed the
     *         maximum permissable size if an add were performed.
     * 
     * @throws {@ProtocolException} Thrown if an add is attempted when the
     *         transaction cannot allow any further work to be added i.e. after
     *         completion of the transaction.
     * 
     * @throws {@TransactionException} Thrown if an unexpected error occurs.
     * 
     * @throws {@PersistenceException} Thrown if a database error occurs.
     */
    public void addItem(final Item item, final Transaction transaction)
                    throws ProtocolException, OutOfCacheSpace, StreamIsFull, TransactionException, PersistenceException, SevereMessageStoreException
    {

        // default lock id
        long lockId = NO_LOCK_ID;

        // delivery delay is set, hence DELIVERY_DELAY_LOCK_ID will be used to lock the Item
        if (item.getDeliveryDelay() > 0)
            lockId = DELIVERY_DELAY_LOCK_ID;

        addItem(item, lockId, transaction);
    }

    /**
     * Add an {@link ItemStream} to an item stream under a transaction. An ItemStream
     * can only be added onto one stream at a time. .
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the stream. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     * 
     * @param itemStream
     * @param lockID id under which the itemStream will be locked when the add is
     *            complete, or {@link AbstractItem#NO_LOCK_ID} if the added itemStream is to
     *            be unlocked.
     * @param transaction
     * @throws SevereMessageStoreException
     * @throws {@link OutOfCacheSpace} if there is not enough space in the
     *         unstoredCache and the storage strategy is {@link AbstractItem#STORE_NEVER}.
     * 
     * @throws {@link StreamIsFull} if the size of the stream would exceed the
     *         maximum permissable size if an add were performed.
     * 
     * @throws {@ProtocolException} Thrown if an add is attempted when the
     *         transaction cannot allow any further work to be added i.e. after
     *         completion of the transaction.
     * 
     * @throws {@TransactionException} Thrown if an unexpected error occurs.
     * 
     * @throws {@PersistenceException} Thrown if a database error occurs.
     */
    public void addItemStream(final ItemStream itemStream, long lockID, final Transaction transaction)
                    throws ProtocolException, OutOfCacheSpace, StreamIsFull, TransactionException, PersistenceException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "addItemStream");

        ItemCollection ic = ((ItemCollection) _getMembership());
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "addItemStream");
            throw new InvalidAddOperation("STREAM_NOT_STORED_SIMS0004", new Object[] { itemStream, this });
        }

        if (null != itemStream._getMembership())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "addItemStream");
            throw new InvalidAddOperation("STREAM_ADD_CONFLICT_SIMS0005", new Object[] { itemStream, this });
        }

        ic.addItemStream(itemStream, lockID, transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "addItemStream");
    }

    /**
     * Add an {@link ItemStream} to an item stream under a transaction. An ItemStream
     * can only be added onto one stream at a time. .
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the stream. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     * 
     * @param itemStream
     * @param transaction
     * @throws SevereMessageStoreException
     * @throws {@link OutOfCacheSpace} if there is not enough space in the
     *         unstoredCache and the storage strategy is {@link AbstractItem#STORE_NEVER}.
     * 
     * @throws {@link StreamIsFull} if the size of the stream would exceed the
     *         maximum permissable size if an add were performed.
     * 
     * @throws {@ProtocolException} Thrown if an add is attempted when the
     *         transaction cannot allow any further work to be added i.e. after
     *         completion of the transaction.
     * 
     * @throws {@TransactionException} Thrown if an unexpected error occurs.
     * 
     * @throws {@PersistenceException} Thrown if a database error occurs.
     */
    public void addItemStream(final ItemStream itemStream, final Transaction transaction)
                    throws ProtocolException, OutOfCacheSpace, StreamIsFull, TransactionException, PersistenceException, SevereMessageStoreException
    {
        addItemStream(itemStream, NO_LOCK_ID, transaction);
    }

    /**
     * Add an {@link ReferenceStream} to an item stream under a transaction. A ReferenceStream
     * can only be added onto one stream at a time. .
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the stream. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     * 
     * @param referenceStream
     * @param lockID id under which the referenceStream will be locked when the add is
     *            complete, or {@link AbstractItem#NO_LOCK_ID} if the added referenceStream is to
     *            be unlocked.
     * @param transaction
     * @throws SevereMessageStoreException
     * @throws {@link OutOfCacheSpace} if there is not enough space in the
     *         unstoredCache and the storage strategy is {@link AbstractItem#STORE_NEVER}.
     * 
     * @throws {@link StreamIsFull} if the size of the stream would exceed the
     *         maximum permissable size if an add were performed.
     * 
     * @throws {@ProtocolException} Thrown if an add is attempted when the
     *         transaction cannot allow any further work to be added i.e. after
     *         completion of the transaction.
     * 
     * @throws {@TransactionException} Thrown if an unexpected error occurs.
     * 
     * @throws {@PersistenceException} Thrown if a database error occurs.
     */
    public void addReferenceStream(final ReferenceStream referenceStream, long lockID, final Transaction transaction)
                    throws ProtocolException, OutOfCacheSpace, StreamIsFull, TransactionException, PersistenceException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "addReferenceStream");

        ItemCollection ic = ((ItemCollection) _getMembership());
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "addReferenceStream");
            throw new InvalidAddOperation("STREAM_NOT_STORED_SIMS0004", new Object[] { referenceStream, this });
        }

        if (null != referenceStream._getMembership())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "addReferenceStream");
            throw new InvalidAddOperation("STREAM_ADD_CONFLICT_SIMS0005", new Object[] { referenceStream, this });
        }

        ic.addReferenceStream(referenceStream, lockID, transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "addReferenceStream");
    }

    /**
     * Add an {@link ReferenceStream} to an item stream under a transaction. A ReferenceStream
     * can only be added onto one stream at a time. .
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the stream. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     * 
     * @param referenceStream
     * @param transaction
     * @throws SevereMessageStoreException
     * @throws {@link OutOfCacheSpace} if there is not enough space in the
     *         unstoredCache and the storage strategy is {@link AbstractItem#STORE_NEVER}.
     * 
     * @throws {@link StreamIsFull} if the size of the stream would exceed the
     *         maximum permissable size if an add were performed.
     * 
     * @throws {@ProtocolException} Thrown if an add is attempted when the
     *         transaction cannot allow any further work to be added i.e. after
     *         completion of the transaction.
     * 
     * @throws {@TransactionException} Thrown if an unexpected error occurs.
     * 
     * @throws {@PersistenceException} Thrown if a database error occurs.
     */
    public void addReferenceStream(final ReferenceStream referenceStream, final Transaction transaction)
                    throws ProtocolException, OutOfCacheSpace, StreamIsFull, TransactionException, PersistenceException, SevereMessageStoreException
    {
        addReferenceStream(referenceStream, NO_LOCK_ID, transaction);
    }

    /**
     * Callback invoked when one of the watermarks is breached.
     * The default action is to do nothing. Subclasses that set
     * watermarks are encouraged to override this method to
     * detect a breach.
     * The decision to perform this callback is made in a block that
     * is controlled under a monitor. The same monitor is used for the
     * queries {@link #getSizeInBytes()} and {@link #getSizeInItems()} (though the block is a way down the stack so thread switches may
     * occur on call stack creation or unwind).
     * The callback itself is made outside the control of the monitor,
     * so the values cannot be guaranteed still to be outside the
     * watermarks when the callback is made.
     */
    public void eventWatermarkBreached() {}

    /**
     * @param filter may be null
     * @return Item may be null.
     */
    public final Item findFirstMatchingItem(final Filter filter) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "findFirstMatchingItem", filter);

        ItemCollection ic = ((ItemCollection) _getMembership());
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "findFirstMatchingItem");
            throw new NotInMessageStore();
        }
        Item item = null;
        if (ic != null)
        {
            item = (Item) ic.findFirstMatchingItem(filter);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "findFirstMatchingItem", item);
        return item;
    }

    /**
     * @param filter may be null
     * @return ItemStream may be null.
     */
    public final ItemStream findFirstMatchingItemStream(final Filter filter) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "findFirstMatchingItemStream", filter);

        ItemCollection ic = ((ItemCollection) _getMembership());
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "findFirstMatchingItemStream");
            throw new NotInMessageStore();
        }
        ItemStream is = null;
        if (ic != null)
        {
            is = ic.findFirstMatchingItemStream(filter);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "findFirstMatchingItemStream", is);
        return is;
    }

    /**
     * @param filter may be null
     * @return Item may be null.
     */
    public final ReferenceStream findFirstMatchingReferenceStream(final Filter filter) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "findFirstMatchingReferenceStream", filter);

        ItemCollection ic = ((ItemCollection) _getMembership());
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "findFirstMatchingReferenceStream");
            throw new NotInMessageStore();
        }
        ReferenceStream rs = null;
        if (ic != null)
        {
            rs = ic.findFirstMatchingReferenceStream(filter);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "findFirstMatchingReferenceStream", rs);
        return rs;
    }

    /**
     * Reply the item in the receiver with a matching ID. The item returned
     * stream is neither removed from the message store nor locked for exclusive use
     * of the caller.
     * 
     * @param itemId
     * 
     * @return item found or null if none.
     * @throws SevereMessageStoreException
     * @throws MessageStoreException
     */
    public final AbstractItem findById(long itemId) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "findById", Long.valueOf(itemId));

        AbstractItem item = null;
        ItemCollection ic = ((ItemCollection) _getMembership());
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "findById");
            throw new NotInMessageStore(); // Defect 489210
        }
        item = ic.findById(itemId);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "findById", item);
        return item;
    }

    /**
     * Find the item that has been known to the stream for longest. The item returned
     * may be in any of the states defined in the state model. The caller should not
     * assume that the item can be used for any particular purpose.
     * 
     * @return Item may be null.
     * @throws {@link MessageStoreException} if the item was spilled and could not
     *         be unspilled. Or if item not found in backing store.
     */
    public final Item findOldestItem() throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "findOldestItem");

        ItemCollection ic = ((ItemCollection) _getMembership());
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "findOldestItem");
            throw new NotInMessageStore();
        }
        Item item = null;
        if (ic != null)
        {
            item = (Item) ic.findOldestItem();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "findOldestItem", item);
        return item;
    }

    /**
     * @return long The point at which an increase in size (in total number
     *         of bytes) of the stream will trigger a {@link #eventWatermarkBreached} callback.
     *         The watermark is considered breached when the size goes from below the watermark to
     *         above or equal to the watermark.
     *         The number of bytes totalled is that declared by the items in response
     *         to the {@link AbstractItem#getPersistentDataSize()}.
     *         The default value is -1, which will prevent the callback ever occuring
     *         (size can never be negative).
     *         Only items directly contained within the stream will contribute to the count,
     *         not contained reference streams, items streams, or items contained within
     *         contained streams.
     *         The size will be the maximal calculated for all counted items - ie those in
     *         all states which imply containment within the stream.
     *         This method will be called by the messageStore whenever the item is
     *         presented or recreated (that is, the returned value is not persisted).
     */
    public long getByteHighWaterMark()
    {
        return -1;
    }

    /**
     * @return long The point at which a decrease in size (in total number
     *         of bytes) of the stream will trigger a {@link #eventWatermarkBreached} callback.
     *         The watermark is considered breached when the size goes from above or equal
     *         to the watermark to below the watermark.
     *         The number of bytes totalled is that declared by the items in response
     *         to the {@link AbstractItem#getPersistentDataSize()}.
     *         The default value is -1, which will prevent the callback ever occuring
     *         (size can never be negative).
     *         Only items directly contained within the stream will contribute to the count,
     *         not contained reference streams, items streams, or items contained within
     *         contained streams.
     *         The size will be the maximal calculated for all counted items - ie those in
     *         all states which imply containment within the stream.
     *         This method will be called by the messageStore whenever the item is
     *         presented or recreated (that is, the returned value is not persisted).
     */
    public long getByteLowWaterMark()
    {
        return -1;
    }

    /**
     * @return long The point at which an increase in size (in number of items)
     *         of the stream will trigger a {@link #eventWatermarkBreached} callback.
     *         The watermark is considered breached when the size goes from below the watermark to
     *         above or equal to the watermark.
     *         The default value is -1, which will prevent the callback ever occuring
     *         (size can never be negative).
     *         Only items directly contained within the stream will contribute to the count,
     *         not contained reference streams, items streams, or items contained within
     *         contained streams.
     *         The size will be the maximal calculated for all counted items - ie those in
     *         all states which imply containment within the stream.
     *         This method will be called by the messageStore whenever the item is
     *         presented or recreated (that is, the returned value is not persisted).
     */
    public long getCountHighWaterMark()
    {
        return -1;
    }

    /**
     * @return long The point at which a decrease in size (in number of items)
     *         of the stream will trigger a {@link #eventWatermarkBreached} callback.
     *         The watermark is considered breached when the size goes from above or equal
     *         to the watermark to below the watermark.
     *         The default value is -1, which will prevent the callback ever occuring
     *         (size can never be negative).
     *         Only items directly contained within the stream will contribute to the count,
     *         not contained reference streams, items streams, or items contained within
     *         contained streams.
     *         The size will be the maximal calculated for all counted items - ie those in
     *         all states which imply containment within the stream.
     *         This method will be called by the messageStore whenever the item is
     *         presented or recreated (that is, the returned value is not persisted).
     */
    public long getCountLowWaterMark()
    {
        return -1;
    }

    /**
     * @return The {@link ItemStream} in which the receiver is stored, or null
     *         if none.
     * @throws SevereMessageStoreException
     */
    public final ItemStream getItemStream() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getItemStream");

        ItemCollection ic = ((ItemCollection) _getMembership());
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getItemStream");
            throw new NotInMessageStore();
        }
        ItemStream itemStream = null;
        if (null != ic)
        {
            itemStream = ic.getOwningItemStream();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getItemStream", itemStream);
        return itemStream;
    }

    /**
     * @return maximum time, in milliseconds, the item may exist in the store
     *         before it becomes eligible for expiry. The default implementation for
     *         ItemStream returns the value NEVER_EXPIRES.
     */
    @Override
    public long getMaximumTimeInStore()
    {
        return NEVER_EXPIRES;
    }

    /**
     * @return the object representing the statistics for this stream. The item returned
     *         is valid for the lifetime of the stream/message store cycle. Id Est: the statistics
     *         object will be useful until either the message store is shut or the stream is
     *         removed.
     * @throws SevereMessageStoreException
     * @throws MessageStoreException
     */
    public final Statistics getStatistics() throws SevereMessageStoreException
    {
        if (null == ((ItemCollection) _getMembership()))
        {
            throw new NotInMessageStore();
        }
        ItemCollection ic = ((ItemCollection) _getMembership());
        if (null == ic)
        {
            throw new NotInMessageStore();
        }
        return ic.getStatistics();
    }

    /**
     * @return true if the receiver is an aggregate type (eg {@link ItemStream} or {@link ReferenceStream}. Return
     *         false otherwise.
     *         The default behaviour is to return false.
     *         Overriden here to return true.
     */
    @Override
    public final boolean isCollection()
    {
        return true;
    }

    /**
     * @return true if the receiver is an instance of {@link ItemStream}, false otherwise. Default
     *         implementation returns false.
     *         Overridden here to return true.
     */
    @Override
    public final boolean isItemStream()
    {
        return true;
    }

    /**
     * @return true if the list is spilling.
     * @throws NotInMessageStore
     */
    public final boolean isSpilling() throws NotInMessageStore
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "isSpilling");

        boolean spilling = false;
        ItemCollection ic = ((ItemCollection) _getMembership());
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "isSpilling");
            throw new NotInMessageStore();
        }
        if (null != ic)
        {
            spilling = ic.isSpilling();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "isSpilling", Boolean.valueOf(spilling));
        return spilling;
    }

    /**
     * @param filter
     * @return a Locking Cursor on the stream. Jumpback is enabled.
     * @throws {@link MessageStoreException}
     */
    public final LockingCursor newLockingItemCursor(Filter filter) throws MessageStoreException
    {
        return newLockingItemCursor(filter, true);
    }

    /**
     * @param filter
     * @param jumpbackEnabled
     * @return a Locking Cursor on the stream
     * @throws {@link MessageStoreException}
     */
    public final LockingCursor newLockingItemCursor(Filter filter, boolean jumpbackEnabled) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "newLockingItemCursor", new Object[] { filter, Boolean.valueOf(jumpbackEnabled) });

        ItemCollection ic = (ItemCollection) _getMembership();
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "newLockingItemCursor");
            throw new NotInMessageStore();
        }
        LockingCursor cursor = null;
        if (ic != null)
        {
            cursor = ic.newLockingItemCursor(filter, jumpbackEnabled);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "newLockingItemCursor", cursor);
        return cursor;
    }

    /**
     * @param filter
     * @return a non-Locking Cursor on the stream.
     * @throws {@link MessageStoreException}
     */
    public final NonLockingCursor newNonLockingItemCursor(Filter filter) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "newNonLockingItemCursor");

        ItemCollection ic = (ItemCollection) _getMembership();
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "newNonLockingItemCursor");
            throw new NotInMessageStore();
        }
        NonLockingCursor cursor = null;
        if (ic != null)
        {
            cursor = ic.newNonLockingItemCursor(filter);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "newNonLockingItemCursor", cursor);
        return cursor;
    }

    /**
     * @param filter
     * @return a non-Locking Cursor on the stream.
     * @throws {@link MessageStoreException}
     */
    public final NonLockingCursor newNonLockingItemStreamCursor(Filter filter) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "newNonLockingItemStreamCursor");

        ItemCollection ic = (ItemCollection) _getMembership();
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "newNonLockingItemStreamCursor");
            throw new NotInMessageStore();
        }
        NonLockingCursor cursor = null;
        if (ic != null)
        {
            cursor = ic.newNonLockingItemStreamCursor(filter);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "newNonLockingItemStreamCursor", cursor);
        return cursor;
    }

    /**
     * @param filter
     * @return a non-Locking Cursor on the stream.
     * @throws {@link MessageStoreException}
     */
    public final NonLockingCursor newNonLockingReferenceStreamCursor(Filter filter) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "newNonLockingReferenceStreamCursor");

        ItemCollection ic = (ItemCollection) _getMembership();
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "newNonLockingReferenceStreamCursor");
            throw new NotInMessageStore();
        }
        NonLockingCursor cursor = null;
        if (ic != null)
        {
            cursor = ic.newNonLockingReferenceStreamCursor(filter);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "newNonLockingReferenceStreamCursor", cursor);
        return cursor;
    }

    /**
     * 
     * @param filter
     * @param transaction
     * 
     * @return Item, may be null.
     * @exception MessageStoreException
     */
    public final Item removeFirstMatchingItem(final Filter filter, final Transaction transaction)
                    throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeFirstMatchingItem", new Object[] { filter, transaction });

        ItemCollection ic = (ItemCollection) _getMembership();
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "removeFirstMatchingItem");
            throw new NotInMessageStore();
        }
        Item item = null;
        if (ic != null)
        {
            item = (Item) ic.removeFirstMatchingItem(filter, transaction);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeFirstMatchingItem", item);
        return item;
    }

    /**
     * 
     * @param filter may be null
     * @param transaction
     *            must not be null.
     * 
     * @return Item may be null.
     * @exception MessageStoreException
     */
    public final ItemStream removeFirstMatchingItemStream(final Filter filter, final Transaction transaction)
                    throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeFirstMatchingItemStream", new Object[] { filter, transaction });

        ItemCollection ic = (ItemCollection) _getMembership();
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "removeFirstMatchingItemStream");
            throw new NotInMessageStore();
        }
        ItemStream is = null;
        if (ic != null)
        {
            is = ic.removeFirstMatchingItemStream(filter, transaction);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeFirstMatchingItemStream", is);
        return is;
    }

    /**
     * 
     * @param filter may be null
     * @param transaction
     *            must not be null.
     * 
     * @return Item may be null.
     * @exception MessageStoreException
     */
    public final ReferenceStream removeFirstMatchingReferenceStream(final Filter filter, final Transaction transaction)
                    throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeFirstMatchingReferenceStream", new Object[] { filter, transaction });

        ItemCollection ic = (ItemCollection) _getMembership();
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "removeFirstMatchingReferenceStream");
            throw new NotInMessageStore();
        }
        ReferenceStream rs = null;
        if (ic != null)
        {
            rs = ic.removeFirstMatchingReferenceStream(filter, transaction);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeFirstMatchingReferenceStream", rs);
        return rs;
    }

    /**
     * @param countLow The point at which a decrease in size (in number of items)
     *            of the stream will trigger a {@link #eventWatermarkBreached} callback.
     *            (Items here is generic term, they will actually be references).
     *            The watermark is considered breached when the size goes from above or equal
     *            to the watermark to below the watermark.
     *            The default value is -1, which will prevent the callback ever occuring
     *            (size can never be negative).
     *            Only items directly contained within the stream will contribute to the count,
     *            not contained reference streams, items streams, or items contained within
     *            contained streams.
     *            The size will be the maximal calculated for all counted items - ie those in
     *            all states which imply containment within the stream.
     * 
     * @param countHigh The point at which an increase in size (in number of items)
     *            of the stream will trigger a {@link #eventWatermarkBreached} callback.
     *            The watermark is considered breached when the size goes from below the watermark to
     *            above or equal to the watermark.
     *            The default value is -1, which will prevent the callback ever occuring
     *            (size can never be negative).
     *            Only items directly contained within the stream will contribute to the count,
     *            not contained reference streams, items streams, or items contained within
     *            contained streams.
     *            The size will be the maximal calculated for all counted items - ie those in
     *            all states which imply containment within the stream.
     * 
     * @param bytesLow The point at which a decrease in size (in total number
     *            of bytes) of the stream will trigger a {@link #eventWatermarkBreached} callback.
     *            The watermark is considered breached when the size goes from above or equal
     *            to the watermark to below the watermark.
     *            The number of bytes totalled is that declared by the items in response
     *            to the {@link AbstractItem#getPersistentDataSize()}.
     *            The default value is -1, which will prevent the callback ever occuring
     *            (size can never be negative).
     *            Only items directly contained within the stream will contribute to the count,
     *            not contained reference streams, items streams, or items contained within
     *            contained streams.
     *            The size will be the maximal calculated for all counted items - ie those in
     *            all states which imply containment within the stream.
     * 
     * @param bytesHigh The point at which an increase in size (in total number
     *            of bytes) of the stream will trigger a {@link #eventWatermarkBreached} callback.
     *            The watermark is considered breached when the size goes from below the watermark to
     *            above or equal to the watermark.
     *            The number of bytes totalled is that declared by the items in response
     *            to the {@link AbstractItem#getPersistentDataSize()}.
     *            The default value is -1, which will prevent the callback ever occuring
     *            (size can never be negative).
     *            Only items directly contained within the stream will contribute to the count,
     *            not contained reference streams, items streams, or items contained within
     *            contained streams.
     *            The size will be the maximal calculated for all counted items - ie those in
     *            all states which imply containment within the stream.
     * @throws NotInMessageStore
     */
    public final void setWatermarks(long countLow, long countHigh, long bytesLow, long bytesHigh) throws NotInMessageStore
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setWatermarks", new Object[] { Long.valueOf(countLow), Long.valueOf(countHigh), Long.valueOf(bytesLow), Long.valueOf(bytesHigh) });

        ItemCollection ic = (ItemCollection) _getMembership();
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setWatermarks");
            throw new NotInMessageStore();
        }
        ic.setWatermarks(countLow, countHigh, bytesLow, bytesHigh);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setWatermarks");
    }

}
