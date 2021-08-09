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
import java.util.Iterator;
import java.util.List;

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.ItemReference;
import com.ibm.ws.sib.msgstore.ItemStream;
import com.ibm.ws.sib.msgstore.LockingCursor;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.NonLockingCursor;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.ProtocolException;
import com.ibm.ws.sib.msgstore.ReferenceCollection;
import com.ibm.ws.sib.msgstore.ReferenceStream;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.Statistics;
import com.ibm.ws.sib.msgstore.StreamIsFull;
import com.ibm.ws.sib.msgstore.TransactionException;
import com.ibm.ws.sib.msgstore.cache.statemodel.ListStatistics;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.list.PrioritizedCursor;
import com.ibm.ws.sib.msgstore.list.PrioritizedList;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.persistence.PersistentMessageStore;
import com.ibm.ws.sib.msgstore.persistence.TupleTypeEnum;
import com.ibm.ws.sib.msgstore.task.TaskList;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.utils.ras.FormattedWriter;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

public final class ReferenceStreamLink extends LinkOwner implements ReferenceCollection
{
    private static TraceComponent tc = SibTr.register(ReferenceStreamLink.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    private long _nextSequenceToIssue = 0;

    private PrioritizedList _references = null;
    private final ListStatistics _statistics;

    /**
     * @param tuple
     * @param itemCollection
     * @throws MessageStoreException
     */
    public ReferenceStreamLink(final LinkOwner owningStreamLink, final Persistable tuple)
    {
        super(owningStreamLink, tuple);
        // set tuple max size
        // int max = tuple.getStreamMaxSize();
        // getListStatistics().setMaximumSize(max);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "<init>", new Object[] { owningStreamLink, tuple});
            SibTr.exit(this, tc, "<init>", this);
        }
        _statistics = new ListStatistics(this);
    }

    /**
     * @param item
     * @param itemID
     * @param itemCollection
     * @throws MessageStoreException
     */
    public ReferenceStreamLink(final ReferenceStream item, final LinkOwner owningStreamLink, final Persistable persistable) throws OutOfCacheSpace
    {
        super(item, owningStreamLink, persistable);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[] { owningStreamLink, item, persistable});

        _statistics = new ListStatistics(this);
        _setWatermarks(item);
        // since we are creating the ReferenceStream link, we can avoid initializing from the
        // disk - this will be faster.....
        _references = new PrioritizedList();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>", this);
    }

    private final void _initializeReferences() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "_initializeReferences");

        MessageStoreImpl _messageStore = getMessageStoreImpl();
        PersistentMessageStore pm = _messageStore.getPersistentMessageStore();
        try
        {
            List list = pm.readNonStreamItems(getTuple());
            Iterator it = list.iterator();
            while (it.hasNext())
            {
                Persistable tuple = (Persistable) it.next();
                AbstractItemLink link = null;
                if (tuple.getTupleType().equals(TupleTypeEnum.ITEM_REFERENCE))
                {
                    link = new ItemReferenceLink(this, tuple);
                    link.restoreState(tuple);
                    if (link.isInStore())
                    {
                        _messageStore.register(link);
                    }
                }
                else
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_initializeReferences");
                    throw new SevereMessageStoreException("Wrong tuple type in ReferenceStream:" + tuple.getTupleType());
                }
                // notch up the sequence number.  Note this is based on the tuple rather
                // than the link, as links may be removed by expiry.
                long seq = tuple.getSequence();
                synchronized(this)
                {
                    if (seq >= _nextSequenceToIssue)
                    {
                        _nextSequenceToIssue = seq + 1;
                    }
                }
            }
        }
        catch (PersistenceException e)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.cache.links.ReferenceStreamLink._initializeReferences", "147", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_initializeReferences");
            throw new SevereMessageStoreException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_initializeReferences");
    }

    private final PrioritizedList _references() throws SevereMessageStoreException
    {
        if (null == _references)
        {
            _references = new PrioritizedList();
            ((ItemStreamLink) getOwningStreamLink()).assertReadyForReferenceInitialisation();
            _initializeReferences();
        }
        return _references;
    }

    private final void _setWatermarks(ReferenceStream referenceStream)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "_setWatermarks", referenceStream);

        long countLow = referenceStream.getCountLowWaterMark();
        long countHigh = referenceStream.getCountHighWaterMark();
        long byteLow = referenceStream.getByteLowWaterMark();
        long byteHigh = referenceStream.getByteHighWaterMark();
        _statistics.setWatermarks(countLow, countHigh, byteLow, byteHigh);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "_setWatermarks", _statistics);
    }

    /**
     * @see com.ibm.ws.sib.msgstore.ItemCollection#addReference(com.ibm.ws.sib.msgstore.ItemReference, com.ibm.ws.sib.msgstore.transactions.Transaction)
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
    public final void addReference(ItemReference reference, long lockID, Transaction transaction) throws OutOfCacheSpace, ProtocolException, StreamIsFull, TransactionException, PersistenceException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "addReference", new Object[] { reference, transaction});

        _references();
        final MessageStoreImpl messageStore = getMessageStoreImpl();
        int strategy = reference.getStorageStrategy();
        final long itemID = messageStore.getUniqueValue(strategy);
        TupleTypeEnum type = TupleTypeEnum.ITEM_REFERENCE;
        reference.setSizeRefsByMsgSize(messageStore.getJdbcSpillSizeMsgRefsByMsgSize()); // PK57207
        Persistable childPersistable = getTuple().createPersistable(itemID, type);
        final AbstractItemLink link = new ItemReferenceLink(reference, this, childPersistable);
        // Defect 463642
        // Revert to using spill limits previously removed in SIB0112d.ms.2
        link.setParentWasSpillingAtAddTime(getListStatistics().isSpilling());
        messageStore.registerLink(link, reference);
        link.cmdAdd(this, lockID, (PersistentTransaction)transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addReference");
    }

    /**
     * @param _link
     * @throws SevereMessageStoreException
     */
    public final void append(final AbstractItemLink link) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "append", link);

        _references().append(link);

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
                ex = new SevereMessageStoreException("STREAM_NOT_EMPTY_SIMS0501");
            }
        }
        return ex;
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
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "canSoftenReference");
            SibTr.exit(this, tc, "canSoftenReference", Boolean.valueOf(false));
        }
        return false;
    }

    /* lazy initialization of sublists
     */
    public synchronized final void ensureReferencesLoaded() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "ensureReferencesLoaded");

        _references();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "ensureReferencesLoaded");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.links.LinkOwner#eventWatermarkBreached()
     */
    public final void eventWatermarkBreached() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "eventWatermarkBreached");

        ReferenceStream referenceStream = (ReferenceStream)getItem();
        if (null != referenceStream)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "notifying eventWatermarkBreached: "+referenceStream);

            referenceStream.eventWatermarkBreached();

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "notified eventWatermarkBreached: "+referenceStream);
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "no referenceStream to notify");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "eventWatermarkBreached");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.impl.AbstractItemLink#expirableExpire()
     */
    public final boolean expirableExpire(PersistentTransaction transaction) throws SevereMessageStoreException // 179365.3
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "expirableExpire", "tran=" + transaction); // 179365.3

        _references();
        boolean allowed = true;
        if (_statistics.getTotalItemCount() > 0)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "cannot expire a non-empty stream");
            allowed = false;
        }
        else
        {
            allowed = super.expirableExpire(transaction); // 179365.3
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
        // force a load of the links to ensure that all are in the
        // hashtable
        getStatistics();
        return getMessageStoreImpl()._findById(itemId);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#findFirstMatching(com.ibm.ws.sib.msgstore.Filter)
     */
    public final AbstractItem findFirstMatching(Filter filter) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "findFirstMatching", filter);

        AbstractItem item = _references().findFirstMatching(filter);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "findFirstMatching", item);
        return item;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#findOldestItem()
     */
    public final AbstractItem findOldestItem() throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "findOldestItem");

        AbstractItem item = _references().findOldestItem();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "findOldestItem", item);
        return item;
    }

    /**
     * @return the owned item stream if this is an item stream link.
     */
    final ItemStream getItemStream()
    {
        return null;
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

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.links.LinkOwner#getReferenceStream()
     */
    final ReferenceStream getReferenceStream() throws SevereMessageStoreException
    {
        return(ReferenceStream) getItem();
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#getStatistics()
     */
    public final Statistics getStatistics() throws SevereMessageStoreException
    {
        _references();
        return getListStatistics();
    }

    public final boolean isReferenceStreamLink()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ReferenceCollection#isSpilling()
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
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "itemHasBeenRestored",item);

        super.itemHasBeenRestored(item);
        _setWatermarks((ReferenceStream)item);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "itemHasBeenRestored");
    }

    public final void linkAvailable(AbstractItemLink link) throws SevereMessageStoreException
    {
        _references().linkAvailable(link);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.links.LinkOwner#loadOwnedLinks()
     */
    public final boolean loadOwnedLinks() throws SevereMessageStoreException
    {
        boolean loaded = false;
        if (null == _references)
        {
            _references();
            loaded = true;
        }
        return loaded;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#newLockingCursor(com.ibm.ws.sib.msgstore.Filter)
     */
    public final LockingCursor newLockingCursor(Filter filter, boolean jumpbackEnabled) throws PersistenceException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "newLockingCursor", new Object[] {filter, Boolean.valueOf(jumpbackEnabled)});

        AbstractItem item = getItem(); //225627
        int storageStrategy = item.getStorageStrategy(); // 225627
        long lockID = getMessageStoreImpl().getUniqueLockID(storageStrategy); // 225627
        PrioritizedCursor cursor = _references().newCursor(filter, lockID, jumpbackEnabled);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "newLockingCursor", cursor);
        return cursor;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#newNonLockingCursor(com.ibm.ws.sib.msgstore.Filter)
     */
    public final NonLockingCursor newNonLockingCursor(Filter filter) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "newNonLockingCursor", filter);

        PrioritizedCursor cursor = _references().newCursor(filter, AbstractItem.NO_LOCK_ID, false);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "newNonLockingCursor", cursor);
        return cursor;
    }

    public final synchronized long nextSequence()
    {
        return _nextSequenceToIssue++;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.ItemCollection#removeFirstMatching(com.ibm.ws.sib.msgstore.Filter, com.ibm.ws.sib.msgstore.Transaction)
     */
    public final AbstractItem removeFirstMatching(Filter filter, Transaction transaction) throws MessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "removeFirstMatching", new Object[] { filter, transaction});

        PrioritizedList refs = _references();
        AbstractItem item = refs.removeFirstMatching(filter, (PersistentTransaction)transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "removeFirstMatching", item);
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
        return "ReferenceStreamLink("
        + getID()
        + ")"
        + super.toString()
        + " state="
        + getState();
        //+ " size="
        //+ getStatistics().countTotalItems();
    }

    protected final boolean xmlHasChildren()
    {
        if (null != _references)
        {
            return _references.xmlHasChildren();
        }
        return false;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink#xmlTagName()
     */
    protected final String xmlTagName()
    {
        return XML_REFERENCE_STREAM;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.impl.AbstractItemLink#xmlWriteChildrenOn(com.ibm.ws.sib.msgstore.FormatBuffer)
     */
    protected final void xmlWriteChildrenOn(FormattedWriter writer) throws IOException
    {
        super.xmlWriteChildrenOn(writer);
        if (null != _references)
        {
            _references.xmlWriteChildrenOn(writer, XML_REFERENCES);
        }
    }
}
