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
 * A FIFO stream of {@link ItemReference}s.
 * <p>A reference stream is an ordered list of {@link ItemReference}s. {@link ItemReference} are added to
 * the head of the list and normally removed from the tail. This means that {@link ItemReference}s are always retrieved in the order that they were put.</p>
 * <p>{@link ItemReference}s are stored in order of priority, with those of the same priority
 * being stored in sequence order.
 * </p>
 * <p>Instances of this class will only store instances of {@link ItemReference}. </p>
 * <p> {@link ItemReference}s only expire when they are placed in {@link ReferenceStream}s. If an {@link ItemReference} in an {@link ReferenceStream} expires, it will be removed
 * from the {@link ReferenceStream}.
 * </p>
 * <p>
 * A reference to an item can only be put to an {@link ReferenceStream} in the same
 * transaction that the item is put to an {@link ItemStream}. This is so that
 * we can perform reference counting on commits.
 * </p>
 * <p>
 * A reference can only be placed into a {@link ReferenceStream} that is contained
 * directly in the same {@link ItemStream} as the referred {@link Item}.
 * </p>
 * 
 */
public class ReferenceStream extends AbstractItem
{
    private static TraceComponent tc = SibTr.register(ReferenceStream.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    /**
     * We insist on a no-argument constructor for reinstatement from
     * the database.
     */
    public ReferenceStream()
    {
        super();
    }

    /**
     * Add an {@link ItemReference} to a reference stream under a transaction. An Item
     * can only be added onto one stream at a time. .
     * <p>
     * A reference to an item can only be put to an {@link ReferenceStream} in the same
     * transaction that the item is put to an {@link ItemStream}. This is so that
     * we can perform reference counting on commits.
     * </p>
     * <p>
     * A reference can only be placed into a {@link ReferenceStream} that is contained
     * directly in the same {@link ItemStream} as the referred {@link Item}.
     * </p>
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the stream. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     * 
     * @param ref
     * @param lockID id under which the reference will be locked when the add is
     *            complete, or {@link AbstractItem#NO_LOCK_ID} if the added reference is to
     *            be unlocked.
     * @param transaction
     * @throws SevereMessageStoreException
     * @throws {@link OutOfCacheSpace} if there is not enough space in the
     *         unstoredCache and the storage strategy is {@link AbstractItem#STORE_NEVER}.
     * @throws {@link StreamIsFull} if the size of the stream would exceed the
     *         maximum permissable size if an add were performed.
     * @throws {@ProtocolException} Thrown if an add is attempted when the
     *         transaction cannot allow any further work to be added i.e. after
     *         completion of the transaction.
     * @throws {@TransactionException} Thrown if an unexpected error occurs.
     */
    public void add(final ItemReference ref, long lockID, final Transaction transaction)
                    throws ProtocolException, OutOfCacheSpace, StreamIsFull, TransactionException, PersistenceException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "add", new Object[] { ref, Long.valueOf(lockID), transaction });

        if (null == _getMembership())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "add");
            throw new InvalidAddOperation("STREAM_NOT_STORED_SIMS0004", new Object[] { ref, this });
        }

        if (null != ref._getMembership())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "add");
            throw new InvalidAddOperation("STREAM_ADD_CONFLICT_SIMS0005", new Object[] { ref, this });
        }

        Item item = ref.getReferredItem();
        if (null == item)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "add");
            throw new ReferenceConsistencyViolation("REFERENCE_CONSISTENCY_SIMS0003", new Object[] { ref, this });
        }

        ReferenceCollection ic = ((ReferenceCollection) _getMembership());
        ic.addReference(ref, lockID, transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "add");
    }

    /**
     * Add an {@link ItemReference} to a reference stream under a transaction. An Item
     * can only be added onto one stream at a time. .
     * <p>
     * A reference to an item can only be put to an {@link ReferenceStream} in the same
     * transaction that the item is put to an {@link ItemStream}. This is so that
     * we can perform reference counting on commits.
     * </p>
     * <p>
     * A reference can only be placed into a {@link ReferenceStream} that is contained
     * directly in the same {@link ItemStream} as the referred {@link Item}.
     * </p>
     * <p>This method can be overridden by subclass implementors in order to
     * customize the behaviour of the stream. Any override should call the superclass
     * implementation to maintain correct behaviour.</p>
     * 
     * @param ref
     * @param transaction
     * @throws SevereMessageStoreException
     * @throws {@link OutOfCacheSpace} if there is not enough space in the
     *         unstoredCache and the storage strategy is {@link AbstractItem#STORE_NEVER}.
     * @throws {@link StreamIsFull} if the size of the stream would exceed the
     *         maximum permissable size if an add were performed.
     * @throws {@ProtocolException} Thrown if an add is attempted when the
     *         transaction cannot allow any further work to be added i.e. after
     *         completion of the transaction.
     * @throws {@TransactionException} Thrown if an unexpected error occurs.
     * @throws {@PersistenceException} Thrown if a database error occurs.
     */
    public void add(final ItemReference ref, final Transaction transaction)
                    throws ProtocolException, OutOfCacheSpace, StreamIsFull, TransactionException, PersistenceException, SevereMessageStoreException
    {

        // default lock id
        long lockId = NO_LOCK_ID;

        // delivery delay is set, hence DELIVERY_DELAY_LOCK_ID will be used to lock the Item
        if (ref.getDeliveryDelay() > 0)
            lockId = DELIVERY_DELAY_LOCK_ID;

        add(ref, lockId, transaction);

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
        ReferenceCollection ic = ((ReferenceCollection) _getMembership());
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
     * findFirstMatching (aka Non-Destructive get).
     * 
     * @param filter
     * 
     * @return Item may be null.
     * @throws {@link MessageStoreException} if the item was spilled and could not
     *         be unspilled. Or if item not found in backing store.
     * @throws {@link MessageStoreException} indicates that an unrecoverable exception has
     *         occurred in the underlying persistence mechanism.
     * @throws MessageStoreException
     */
    public final ItemReference findFirstMatching(final Filter filter) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "findFirstMatching", filter);

        ReferenceCollection ic = ((ReferenceCollection) _getMembership());
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "findFirstMatching");
            throw new NotInMessageStore();
        }
        ItemReference item = null;
        if (ic != null)
        {
            item = (ItemReference) ic.findFirstMatching(filter);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "findFirstMatching", item);
        return item;
    }

    /**
     * Find the reference that has been known to the stream for longest. The reference returned
     * may be in any of the states defined in the state model. The caller should not
     * assume that the reference can be used for any particular purpose.
     * 
     * @return Item, may be null.
     * @throws {@link MessageStoreException} if the item was spilled and could not
     *         be unspilled. Or if item not found in backing store.
     */
    public final ItemReference findOldestReference() throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "findOldestReference");

        ReferenceCollection ic = ((ReferenceCollection) _getMembership());
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "findOldestReference");
            throw new NotInMessageStore();
        }
        ItemReference item = null;
        if (ic != null)
        {
            item = (ItemReference) ic.findOldestItem();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "findOldestReference", item);
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
     *         (Items here is generic term, they will actually be references).
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

        ReferenceCollection ic = ((ReferenceCollection) _getMembership());
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
     *         ReferenceStream returns NEVER_EXPIRES.
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
     */
    public final Statistics getStatistics() throws SevereMessageStoreException
    {
        ReferenceCollection ic = ((ReferenceCollection) _getMembership());
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
     * @return true if the receiver is an instance of {@link ReferenceStream}, false otherwise. Default
     *         implementation returns false.
     *         Overridden here to return true.
     */
    @Override
    public final boolean isReferenceStream()
    {
        return true;
    }

    /**
     * @return true if the list is spilling
     * @throws NotInMessageStore
     */
    public boolean isSpilling() throws NotInMessageStore
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "isSpilling");

        boolean spilling = false;
        ReferenceCollection ic = ((ReferenceCollection) _getMembership());
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
    public final LockingCursor newLockingCursor(Filter filter) throws MessageStoreException
    {
        return newLockingCursor(filter, true);
    }

    /**
     * @param filter
     * @return a Locking Cursor on the stream.
     * @throws {@link MessageStoreException}
     */
    public final LockingCursor newLockingCursor(Filter filter, boolean jumpbackEnabled) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "newLockingCursor", new Object[] { filter, Boolean.valueOf(jumpbackEnabled) });

        ReferenceCollection rc = ((ReferenceCollection) _getMembership());
        LockingCursor cursor = null;
        if (null == rc)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "newLockingCursor");
            throw new NotInMessageStore();
        }
        if (rc != null)
        {
            cursor = rc.newLockingCursor(filter, jumpbackEnabled);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "newLockingCursor", cursor);
        return cursor;
    }

    /**
     * @param filter
     * @return a non-Locking Cursor on the stream.
     * @throws {@link MessageStoreException}
     */
    public final NonLockingCursor newNonLockingCursor(Filter filter) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "newNonLockingCursor");

        ReferenceCollection ic = ((ReferenceCollection) _getMembership());
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "newNonLockingCursor");
            throw new NotInMessageStore();
        }
        NonLockingCursor cursor = null;
        if (ic != null)
        {
            cursor = ic.newNonLockingCursor(filter);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "newNonLockingCursor", cursor);
        return cursor;
    }

    /**
     * removeFirstMatching (aka DestructiveGet).
     * 
     * @param filter
     * @param transaction
     *            must not be null.
     * 
     * @return Item may be null.
     * @throws {@link MessageStoreException} if the item was spilled and could not
     *         be unspilled. Or if item not found in backing store.
     * @throws {@link MessageStoreException} indicates that an unrecoverable exception has
     *         occurred in the underlying persistence mechanism.
     * @throws MessageStoreException
     */
    public final ItemReference removeFirstMatching(final Filter filter, final Transaction transaction)
                    throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeFirstMatching", new Object[] { filter, transaction });

        ReferenceCollection ic = ((ReferenceCollection) _getMembership());
        ItemReference item = null;
        if (ic != null)
        {
            item = (ItemReference) ic.removeFirstMatching(filter, transaction);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeFirstMatching", item);
        return item;
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

        ReferenceCollection ic = ((ReferenceCollection) _getMembership());
        if (null == ic)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "setWatermarks");
            throw new NotInMessageStore();
        }
        if (null != ic)
        {
            ic.setWatermarks(countLow, countHigh, bytesLow, bytesHigh);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setWatermarks");
    }

}
