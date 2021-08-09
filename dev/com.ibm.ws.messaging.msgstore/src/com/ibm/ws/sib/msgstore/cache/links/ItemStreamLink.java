package com.ibm.ws.sib.msgstore.cache.links;
/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.ItemCollection;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.LockingCursor;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.MismatchedMessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.ProtocolException;
import com.ibm.ws.sib.msgstore.ReferenceStream;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.Statistics;
import com.ibm.ws.sib.msgstore.StreamIsFull;
import com.ibm.ws.sib.msgstore.TransactionException;
import com.ibm.ws.sib.msgstore.cache.statemodel.ListStatistics;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.list.LinkedList;
import com.ibm.ws.sib.msgstore.list.PrioritizedCursor;
import com.ibm.ws.sib.msgstore.list.PrioritizedList;
import com.ibm.ws.sib.msgstore.list.UnprioritizedNonlockingCursor;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.persistence.PersistentMessageStore;
import com.ibm.ws.sib.msgstore.persistence.TupleTypeEnum;
import com.ibm.ws.sib.msgstore.task.TaskList;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/* Item streams store item streams and reference streams.
 * This class uses the superclass to store items, a (non-prioritized) list
 * to store the itemStreams and a (non-prioritized) list
 * to store the referenceStreams.
 * All Item manipulation can be delegated to superclass.  
 */
public class ItemStreamLink extends LinkOwner implements ItemCollection
{
    private static TraceComponent tc = Tr.register(ItemStreamLink.class, 
                                                   MessageStoreConstants.MSG_GROUP, 
                                                   MessageStoreConstants.MSG_BUNDLE);

    private PrioritizedList _items;
    private LinkedList _itemStreams;
    private LinkedList _referenceStreams;

    private long _nextSequenceToIssue = 0;
    private final ListStatistics _statistics;

    /**
     * @param item
     * @param itemID
     * @param itemCollection
     * @throws MessageStoreException
     */
    public ItemStreamLink(final ItemStream item, final LinkOwner owningStreamLink, final Persistable persistable) throws OutOfCacheSpace 
    {
        super(item, owningStreamLink, persistable);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[] { owningStreamLink, item, persistable});

        // since we are creating the ItemStream link, we can avoid initializing from the 
        // disk - this will be faster.....
        _items = new PrioritizedList();
        _itemStreams = new LinkedList();
        _referenceStreams = new LinkedList();
        _statistics = new ListStatistics(this);
        _setWatermarks(item);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>", this);
    }

    /**
     * @param tuple
     * @param itemCollection
     * @throws MessageStoreException
     */
    public ItemStreamLink(final LinkOwner owningStreamLink, final Persistable tuple)
    {
        super(owningStreamLink, tuple);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "<init>", new Object[] { owningStreamLink, tuple});
            SibTr.exit(this, tc, "<init>", this);
        }
        _statistics = new ListStatistics(this);
    }

    private final void _ensureReferencesAreLoaded() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "_ensureReferencesAreLoaded");

        // now ensure that the references are loaded
        ReferenceStreamLink rs = (ReferenceStreamLink) _referenceStreams.getHead();
        while (null != rs)
        {
            rs.ensureReferencesLoaded();
            rs = (ReferenceStreamLink) _referenceStreams.getNextLink(rs);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_ensureReferencesAreLoaded");
    }

    /**
     * @param map map of id --> child tuples
     * @throws SevereMessageStoreException 
     */
    final void _initializeChildren(HashMap map) throws SevereMessageStoreException
    {
        _itemStreams = new LinkedList();
        _referenceStreams = new LinkedList();
        // my children's tuples have key MEMBER_KEY

        MessageStoreImpl messageStore = getMessageStoreImpl();
        ArrayList children = (ArrayList) map.remove(Long.valueOf(this.getID()));
        for (int i = 0; null != children && i < children.size(); i++)
        {
            Persistable tuple = (Persistable) children.get(i);
            AbstractItemLink childLink = null;
            tuple.setContainingStream(getTuple());
            if (tuple.getTupleType().equals(TupleTypeEnum.ITEM_STREAM))
            {
                childLink = new ItemStreamLink(this, tuple);
                childLink.restoreState(tuple);
                if (childLink.isInStore())
                {
                    messageStore.register(childLink);
                }
                if (childLink.isItemStreamLink())
                {
                    ((ItemStreamLink) childLink)._initializeChildren(map);
                }
            }
            else if (tuple.getTupleType().equals(TupleTypeEnum.REFERENCE_STREAM))
            {
                childLink = new ReferenceStreamLink(this, tuple);
                childLink.restoreState(tuple);
                if (childLink.isInStore())
                {
                    messageStore.register(childLink);
                }
            }
            else
            {
                throw new SevereMessageStoreException("Wrong tuple type in ItemStream:" + tuple.getTupleType());
            }
            // notch up the sequence number.  Note this is based on the tuple rather
            // than the link, as links may be removed by expiry.
            long seq = tuple.getSequence();
            if (seq >= _nextSequenceToIssue)
            {
                _nextSequenceToIssue = seq + 1;
            }
        }
    }

    private final void _initializeItems() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "_initializeItems");

        _items = new PrioritizedList();
        MessageStoreImpl messageStore = getMessageStoreImpl();
        PersistentMessageStore pm = messageStore.getPersistentMessageStore();
        try
        {
            List list = pm.readNonStreamItems(getTuple());
            Iterator it = list.iterator();
            while (it.hasNext())
            {
                Persistable tuple = (Persistable) it.next();
                AbstractItemLink link = null;
                if (tuple.getTupleType().equals(TupleTypeEnum.ITEM))
                {
                    link = new ItemLink(this, tuple);
                    link.restoreState(tuple);
                    if (link.isInStore())
                    {
                        messageStore.register(link);
                    }
                }
                else
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_initializeItems");
                    throw new SevereMessageStoreException("Wrong tuple type in ItemStream:" + tuple.getTupleType());
                }
                // notch up the sequence number.  Note this is based on the tuple rather
                // than the link, as links may be removed by expiry.
                long seq = tuple.getSequence();
                if (seq >= _nextSequenceToIssue)
                {
                    _nextSequenceToIssue = seq + 1;
                }
            }
        }
        catch (PersistenceException e)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.cache.links.ItemStreamLink._initializeItems", "217", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_initializeItems");
            throw new SevereMessageStoreException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_initializeItems");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.impl.LinkOwner#initializePrioritySublists()
     */
    private final PrioritizedList _items() throws SevereMessageStoreException
    {
        if (null == _items)
        {
            _initializeItems();
            _ensureReferencesAreLoaded();
        }
        return _items;
    }

    private final void _setWatermarks(ItemStream itemStream)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "_setWatermarks", itemStream);

        long countLow = itemStream.getCountLowWaterMark();
        long countHigh = itemStream.getCountHighWaterMark();
        long byteLow = itemStream.getByteLowWaterMark();
        long byteHigh = itemStream.getByteHighWaterMark();
        _statistics.setWatermarks(countLow, countHigh, byteLow, byteHigh);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_setWatermarks",_statistics);
    }

    /**
     * @see com.ibm.ws.sib.msgstore.ItemCollection#addItem(com.ibm.ws.sib.msgstore.Item, com.ibm.ws.sib.msgstore.Transaction)
     * 
     * @throws OutOfCacheSpace if there is not enough space in the 
     * unstoredCache and the storage strategy is AbstractItem.STORE_NEVER.
     *  
     * @throws StreamIsFull if the size of the stream would exceed the 
     * maximum permissable size if an add were performed.
     * @throws SevereMessageStoreException 
     *
     * @throws {@ProtocolException} Thrown if an add is attempted when the 
     * transaction cannot allow any further work to be added i.e. after 
     * completion of the transaction.
     * 
     * @throws {@TransactionException} Thrown if an unexpected error occurs.
     */
    public final void addItem(Item item, long lockID, Transaction transaction) throws OutOfCacheSpace, ProtocolException, StreamIsFull, TransactionException, PersistenceException, SevereMessageStoreException 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "addItem", new Object[] { item, Long.valueOf(lockID), transaction});

        _items();

        // Defect 410652
        // Check the transaction being used is from the same MessageStore as
        // ours so that we don't get a mismatch and run the possibility of 
        // hitting a DuplicateKeyException at persistence time. If the MS's
        // don't match then the unique key generator being used for this
        // add could be using a range that has already been used in the MS
        // that will be used to persist the transaction.
        final MessageStoreImpl messageStore = getMessageStoreImpl();
        final MessageStore     tranStore    = ((PersistentTransaction)transaction).getOwningMessageStore();
        // We only need to do a simple equality check as all that we really 
        // care about is that the same MS instance in memory is being used.
        if (messageStore != tranStore)
        {
            MismatchedMessageStoreException mmse = new MismatchedMessageStoreException("Transaction supplied on add does not originate from this MessageStore! MS: "+messageStore+", Tran.MS: "+tranStore);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.event(this, tc, "Transaction supplied on add does not originate from this MessageStore! MS: "+messageStore+", Tran.MS: "+tranStore, mmse);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addItem");
            throw mmse;
        }

        int strategy = item.getStorageStrategy();
        final long itemID = messageStore.getUniqueValue(strategy);
        TupleTypeEnum type = TupleTypeEnum.ITEM;
        Persistable childPersistable = getTuple().createPersistable(itemID, type);
        final AbstractItemLink link = new ItemLink(item, this, childPersistable);
        // Defect 463642 
        // Revert to using spill limits previously removed in SIB0112d.ms.2
        link.setParentWasSpillingAtAddTime(getListStatistics().isSpilling());
        messageStore.registerLink(link, item);
        link.cmdAdd(this, lockID, (PersistentTransaction)transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addItem", new Object[]{"Item:      "+item, "Item Link: "+link});
    }

    /**
     * @see com.ibm.ws.sib.msgstore.ItemCollection#addItemStream(com.ibm.ws.sib.msgstore.ItemStream, com.ibm.ws.sib.msgstore.Transaction)
     * 
     * @throws OutOfCacheSpace if there is not enough space in the 
     * unstoredCache and the storage strategy is AbstractItem.STORE_NEVER.
     *  
     * @throws StreamIsFull if the size of the stream would exceed the 
     * maximum permissable size if an add were performed.
     * @throws SevereMessageStoreException 
     * @throws {@ProtocolException} Thrown if an add is attempted when the 
     * transaction cannot allow any further work to be added i.e. after 
     * completion of the transaction.
     * 
     * @throws {@TransactionException} Thrown if an unexpected error occurs.
     */
    public final void addItemStream(ItemStream itemStream, long lockID, Transaction transaction) throws OutOfCacheSpace, ProtocolException, StreamIsFull, TransactionException, PersistenceException, SevereMessageStoreException 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "addItemStream", new Object[] { itemStream, Long.valueOf(lockID), transaction});

        // Defect 410652
        // Check the transaction being used is from the same MessageStore as
        // ours so that we don't get a mismatch and run the possibility of 
        // hitting a DuplicateKeyException at persistence time. If the MS's
        // don't match then the unique key generator being used for this
        // add could be using a range that has already been used in the MS
        // that will be used to persist the transaction.
        final MessageStoreImpl messageStore = getMessageStoreImpl();
        final MessageStore     tranStore    = ((PersistentTransaction)transaction).getOwningMessageStore();
        // We only need to do a simple equality check as all that we really 
        // care about is that the same MS instance in memory is being used.
        if (messageStore != tranStore)
        {
            MismatchedMessageStoreException mmse = new MismatchedMessageStoreException("Transaction supplied on add does not originate from this MessageStore! MS: "+messageStore+", Tran.MS: "+tranStore);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.event(this, tc, "Transaction supplied on add does not originate from this MessageStore! MS: "+messageStore+", Tran.MS: "+tranStore, mmse);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addItemStream");
            throw mmse;
        }

        int strategy = itemStream.getStorageStrategy();
        final long itemID = messageStore.getUniqueValue(strategy);

        TupleTypeEnum type = TupleTypeEnum.ITEM_STREAM;
        Persistable childPersistable = getTuple().createPersistable(itemID, type);
        final AbstractItemLink link = new ItemStreamLink(itemStream, this, childPersistable);
        // Defect 463642 
        // Revert to using spill limits previously removed in SIB0112d.ms.2
        link.setParentWasSpillingAtAddTime(getListStatistics().isSpilling());
        messageStore.registerLink(link, itemStream);
        link.cmdAdd(this, lockID, (PersistentTransaction)transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addItemStream", new Object[]{"Stream:      "+itemStream, "Stream Link: "+link});
    }

    /**
     * @see com.ibm.ws.sib.msgstore.ItemCollection#addReferenceStream(com.ibm.ws.sib.msgstore.ReferenceStream, com.ibm.ws.sib.msgstore.Transaction)
     * 
     * @throws OutOfCacheSpace if there is not enough space in the 
     * unstoredCache and the storage strategy is AbstractItem.STORE_NEVER.
     *  
     * @throws StreamIsFull if the size of the stream would exceed the 
     * maximum permissable size if an add were performed.
     * @throws SevereMessageStoreException 
     * @throws {@ProtocolException} Thrown if an add is attempted when the 
     * transaction cannot allow any further work to be added i.e. after 
     * completion of the transaction.
     * 
     * @throws {@TransactionException} Thrown if an unexpected error occurs.
     */
    public final void addReferenceStream(ReferenceStream referenceStream, long lockID, Transaction transaction) throws OutOfCacheSpace, ProtocolException, StreamIsFull, TransactionException, PersistenceException, SevereMessageStoreException 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "addReferenceStream", new Object[] { referenceStream, Long.valueOf(lockID), transaction});

        // Defect 410652
        // Check the transaction being used is from the same MessageStore as
        // ours so that we don't get a mismatch and run the possibility of 
        // hitting a DuplicateKeyException at persistence time. If the MS's
        // don't match then the unique key generator being used for this
        // add could be using a range that has already been used in the MS
        // that will be used to persist the transaction.
        final MessageStoreImpl messageStore = getMessageStoreImpl();
        final MessageStore     tranStore    = ((PersistentTransaction)transaction).getOwningMessageStore();
        // We only need to do a simple equality check as all that we really 
        // care about is that the same MS instance in memory is being used.
        if (messageStore != tranStore)
        {
            MismatchedMessageStoreException mmse = new MismatchedMessageStoreException("Transaction supplied on add does not originate from this MessageStore! MS: "+messageStore+", Tran.MS: "+tranStore);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.event(this, tc, "Transaction supplied on add does not originate from this MessageStore! MS: "+messageStore+", Tran.MS: "+tranStore, mmse);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addReferenceStream");
            throw mmse;
        }

        int strategy = referenceStream.getStorageStrategy();
        final long itemID = messageStore.getUniqueValue(strategy);
        TupleTypeEnum type = TupleTypeEnum.REFERENCE_STREAM;
        Persistable childPersistable = getTuple().createPersistable(itemID, type);
        final AbstractItemLink link = new ReferenceStreamLink(referenceStream, this, childPersistable);
        // Defect 463642 
        // Revert to using spill limits previously removed in SIB0112d.ms.2
        link.setParentWasSpillingAtAddTime(getListStatistics().isSpilling());
        messageStore.registerLink(link, referenceStream);
        link.cmdAdd(this, lockID, (PersistentTransaction)transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addReferenceStream", new Object[]{"Stream:      "+referenceStream, "Stream Link: "+link});
    } 

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.impl.LinkOwner#append(com.ibm.ws.sib.msgstore.impl.AbstractItemLink)
     * Used by AddTask to really attach the link.  The addTask does not know what it is adding so
     * needs to use a general type.
     */
    public final void append(AbstractItemLink link) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "append", link);

        if (link.isItemStreamLink())
        {
            _itemStreams.append(link);
        }
        else if (link.isReferenceStreamLink())
        {
            _referenceStreams.append(link);
        }
        else
        {
            _items().append(link);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "append");
    }

    public final SevereMessageStoreException assertCanDelete(PersistentTransaction transaction)
    {
    	SevereMessageStoreException ex = super.assertCanDelete(transaction);
        if (null != ex)
        {
            return ex;
        }
        int removesFromMeUnderThisTransaction = 0;
        final TaskList taskList = (TaskList) transaction.getWorkList();
        if (null != taskList)
        {
            // the number being removed from this list must be the same as the number being
            // removed from this list under the current transaction (tasklist).
            removesFromMeUnderThisTransaction = taskList.countRemovingItems(this);
            boolean canDelete = getListStatistics().canDelete(removesFromMeUnderThisTransaction);
            if (!canDelete)
            {
                // Defect 474728
                // We can provide more information in the exception message here to help
                // the user (most likely MP) determine what went wrong.
                long   itemCount = getListStatistics().getTotalItemCount();
                String className = null;
                try
                {
	                AbstractItem item = getItem();
	                if (item != null) 
	                {
	                    className = item.getClass().getName();
	                }
	                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot delete non-empty stream! Items on Stream="+itemCount+", Removes in this transaction="+removesFromMeUnderThisTransaction+", Stream class="+className); 
	                ex = new SevereMessageStoreException("STREAM_NOT_EMPTY_SIMS0507", new Object[] {Long.valueOf(itemCount), className});
                }
                catch (SevereMessageStoreException smse)
                {
                	com.ibm.ws.ffdc.FFDCFilter.processException(smse, "com.ibm.ws.sib.msgstore.cache.links.ItemStreamLink.assertCanDelete", "1:506:1.112", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Severe exception caught during assertCanDelete!", smse);
                    ex = smse;
                }
            }
        }
        return ex;
    }

    /* This method is called by a dependent reference stream to ensure that the 
     * stream is ready for references to be created to its items. Essentially
     * this requires that all items have been loaded. This means that we can delay
     * the loading of the items to the last possible momment.
     */
    final void assertReadyForReferenceInitialisation() throws SevereMessageStoreException
    {
        _items();
    }

    // Defect 484799
    public final void checkSpillLimits() 
    {
        _statistics.checkSpillLimits();
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.impl.AbstractItemLink#canSoftenReference()
     */
    protected final boolean canSoftenReference()
    {
        return false;
    }

    public final void ensureItemsLoaded() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "ensureItemsLoaded");

        _items();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "ensureItemsLoaded");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.links.LinkOwner#eventWatermarkBreached()
     */
    public final void eventWatermarkBreached() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "eventWatermarkBreached");

        ItemStream itemStream = (ItemStream)getItem();
        if (null != itemStream)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "notifying eventWatermarkBreached: "+itemStream);

            itemStream.eventWatermarkBreached();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "notified eventWatermarkBreached: "+itemStream);
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "no itemStream to notify");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "eventWatermarkBreached");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.impl.AbstractItemLink#expirableExpire()
     */
    public final boolean expirableExpire(PersistentTransaction transaction) throws SevereMessageStoreException // 179365.3
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "expirableExpire", "tran=" + transaction); // 179365.3

        boolean allowed = true;
        if (_statistics.getTotalItemCount() > 0)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "cannot expire a non-empty stream");
            allowed = false;
        }
        else
        {
            allowed = super.expirableExpire(transaction);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "expirableExpire", Boolean.valueOf(allowed));
        return allowed;
    }

    /**
     * @throws SevereMessageStoreException 
     * @see com.ibm.ws.sib.msgstore.ItemCollection#findById(long)
     * TODO we can eventually replace this implementation with a local
     * hashtable, rather than use the one within messageStoreImpl.
     */
    public final AbstractItem findById(long itemId) throws SevereMessageStoreException
    {
        // force a load of the links to ensure that all are in the hashtable
        getStatistics();  
        return getMessageStoreImpl()._findById(itemId);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#findFirstMatching(com.ibm.ws.sib.msgstore.Filter)
     */
    public AbstractItem findFirstMatchingItem(Filter filter) throws MessageStoreException 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "findFirstMatching", filter);

        PrioritizedList items = _items();
        AbstractItem item = items.findFirstMatching(filter);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "findFirstMatching", item);
        return item;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#findFirstMatchingItemStream(com.ibm.ws.sib.msgstore.Filter)
     */
    public final ItemStream findFirstMatchingItemStream(Filter filter) throws MessageStoreException 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "findFirstMatchingItemStream", filter);

        ItemStream item = (ItemStream) _itemStreams.findFirstMatching(filter);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "findFirstMatchingItemStream", item);
        return item;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#findFirstMatchingReferenceStream(com.ibm.ws.sib.msgstore.Filter)
     */
    public final ReferenceStream findFirstMatchingReferenceStream(Filter filter) throws MessageStoreException 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "findFirstMatchingReferenceStream", filter);

        ReferenceStream item = (ReferenceStream) _referenceStreams.findFirstMatching(filter);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "findFirstMatchingReferenceStream", item);
        return item;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#findOldestItem()
     */
    public AbstractItem findOldestItem() throws MessageStoreException 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "findOldestItem");

        _items();
        AbstractItem item = _items().findOldestItem();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "findOldestItem", item);
        return item;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.links.LinkOwner#getItemStream()
     */
    final ItemStream getItemStream() throws SevereMessageStoreException
    {
        return(ItemStream) getItem();
    }

    public final ListStatistics getListStatistics()
    {
        return _statistics;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemMembership#getOwningItemStream()
     */
    public final ItemStream getOwningItemStream() throws SevereMessageStoreException
    {
        return((ItemStreamLink) getOwningStreamLink()).getItemStream();
    }

    /**
     * @return the owned reference stream if this is a reference
     * stream link.
     */
    final ReferenceStream getReferenceStream()
    {
        return null;
    }
    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#getStatistics()
     */
    public final Statistics getStatistics() throws SevereMessageStoreException
    {
        _items();
        return getListStatistics();
    }

    public final boolean isItemStreamLink()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#isSpilling()
     */
    public final boolean isSpilling()
    {
        boolean spilling = false;
        // defect 208011 - all streams except STORE_NEVER can spill, not just store
        if (AbstractItem.STORE_NEVER != getTuple().getStorageStrategy())
        {
            // Defect 463642 
            // Revert to using spill limits previously removed in SIB0112d.ms.2
            spilling = getListStatistics().isSpilling();
        }
        return spilling;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink#itemHasBeenRestored(com.ibm.ws.sib.msgstore.AbstractItem)
     */
    protected final void itemHasBeenRestored(AbstractItem item)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "itemHasBeenRestored", item);

        super.itemHasBeenRestored(item);
        _setWatermarks((ItemStream)item);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "itemHasBeenRestored");
    }

    public final void linkAvailable(AbstractItemLink link) throws SevereMessageStoreException
    {
        _items().linkAvailable(link);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.links.LinkOwner#loadOwnedLinks()
     */
    public final boolean loadOwnedLinks() throws SevereMessageStoreException
    {
        boolean loaded = false;
        if (null == _items)
        {
            _items();
            loaded = true;
        }
        return loaded;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#newLockingCursor(com.ibm.ws.sib.msgstore.Filter, boolean)
     */
    public LockingCursor newLockingItemCursor(Filter filter, boolean jumpbackEnabled) throws PersistenceException, SevereMessageStoreException 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "newLockingCursor", new Object[] {filter, Boolean.valueOf(jumpbackEnabled)});

        AbstractItem item = getItem(); //225627
        int storageStrategy = item.getStorageStrategy(); // 225627
        long lockID = getMessageStoreImpl().getUniqueLockID(storageStrategy); // 225627
        PrioritizedCursor cursor = _items().newCursor(filter, lockID, jumpbackEnabled);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "newLockingCursor", cursor);
        return cursor;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#newNonLockingCursor(com.ibm.ws.sib.msgstore.Filter)
     */
    public NonLockingCursor newNonLockingItemCursor(Filter filter) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "newNonLockingCursor", filter);

        PrioritizedCursor cursor = _items().newCursor(filter, AbstractItem.NO_LOCK_ID, false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "newNonLockingCursor", cursor);
        return cursor;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#newNonLockingItemStreamCursor(com.ibm.ws.sib.msgstore.Filter)
     */
    public final NonLockingCursor newNonLockingItemStreamCursor(Filter filter)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "newNonLockingItemStreamCursor", filter);
        
        NonLockingCursor cursor = new UnprioritizedNonlockingCursor(_itemStreams, filter);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "newNonLockingItemStreamCursor", cursor);
        return cursor;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#newNonLockingReferenceStreamCursor(com.ibm.ws.sib.msgstore.Filter)
     */
    public final NonLockingCursor newNonLockingReferenceStreamCursor(Filter filter)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "newNonLockingReferenceStreamCursor", filter);
        
        NonLockingCursor cursor = new UnprioritizedNonlockingCursor(_referenceStreams, filter);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "newNonLockingReferenceStreamCursor", cursor);
        return cursor;
    }

    public final synchronized long nextSequence()
    {
        return _nextSequenceToIssue++;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#removeFirstMatching(com.ibm.ws.sib.msgstore.Filter, com.ibm.ws.sib.msgstore.Transaction)
     */
    public AbstractItem removeFirstMatchingItem(Filter filter, Transaction transaction) throws MessageStoreException 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "removeFirstMatching", new Object[] { filter, transaction});
        
        PrioritizedList items = _items();
        AbstractItem item = items.removeFirstMatching(filter, (PersistentTransaction)transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "removeFirstMatching", item);
        return item;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#removeFirstMatchingItemStream(com.ibm.ws.sib.msgstore.Filter, com.ibm.ws.sib.msgstore.Transaction)
     */
    public final ItemStream removeFirstMatchingItemStream(Filter filter, Transaction transaction) throws MessageStoreException 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "removeFirstMatchingItemStream");
        
        ItemStream item = (ItemStream) _itemStreams.removeFirstMatching(filter, (PersistentTransaction)transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "removeFirstMatchingItemStream", item);
        return item;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#removeFirstMatchingReferenceStream(com.ibm.ws.sib.msgstore.Filter, com.ibm.ws.sib.msgstore.Transaction)
     */
    public final ReferenceStream removeFirstMatchingReferenceStream(Filter filter, Transaction transaction) throws MessageStoreException 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "removeFirstMatchingReferenceStream");

        ReferenceStream item = (ReferenceStream) _referenceStreams.removeFirstMatching(filter, (PersistentTransaction)transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "removeFirstMatchingReferenceStream", item);
        return item;
    }

    public final void setWatermarks(long countLow, long countHigh, long bytesLow, long bytesHigh)
    {
        _statistics.setWatermarks(countLow, countHigh, bytesLow, bytesHigh);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("ItemStreamLink(");
        buf.append(getID());
        buf.append(")");
        buf.append(super.toString());
        buf.append(" state=");
        buf.append(getState());
        return buf.toString();
    }

    protected boolean xmlHasChildren()
    {
        if (null != _items)
        {
            return _items.xmlHasChildren();
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink#xmlTagName()
     */
    protected final String xmlTagName()
    {
        return XML_ITEM_STREAM;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.impl.AbstractItemLink#xmlWriteChildrenOn(com.ibm.ws.sib.msgstore.FormatBuffer)
     */
    protected void xmlWriteChildrenOn(FormattedWriter writer) throws IOException 
    {
        super.xmlWriteChildrenOn(writer);
        if (null != _items)
        {
            _items.xmlWriteChildrenOn(writer, XML_ITEMS);
        }
        if (null != _itemStreams)
        {
            _itemStreams.xmlWriteOn(writer, XML_ITEM_STREAMS);
        }
        if (null != _referenceStreams)
        {
            _referenceStreams.xmlWriteOn(writer, XML_REFERENCE_STREAMS);
        }
    }
}
