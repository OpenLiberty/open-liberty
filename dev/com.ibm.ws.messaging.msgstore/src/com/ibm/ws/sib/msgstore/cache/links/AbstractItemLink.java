package com.ibm.ws.sib.msgstore.cache.links;

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
import java.io.StringWriter;
import java.lang.ref.SoftReference;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.sib.exception.SIException;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.Filter;
import com.ibm.ws.sib.msgstore.InvalidAddOperation;
import com.ibm.ws.sib.msgstore.Item;
import com.ibm.ws.sib.msgstore.Membership;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.MessageStoreException;
import com.ibm.ws.sib.msgstore.OutOfCacheSpace;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.PersistentDataEncodingException;
import com.ibm.ws.sib.msgstore.ProtocolException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.StreamIsFull;
import com.ibm.ws.sib.msgstore.TransactionException;
import com.ibm.ws.sib.msgstore.XmlConstants;
import com.ibm.ws.sib.msgstore.MessageStoreConstants.MaximumAllowedDeliveryDelayAction;
import com.ibm.ws.sib.msgstore.cache.ref.Indirection;
import com.ibm.ws.sib.msgstore.cache.ref.IndirectionCache;
import com.ibm.ws.sib.msgstore.cache.statemodel.ListStatistics;
import com.ibm.ws.sib.msgstore.cache.statemodel.LockIdMismatch;
import com.ibm.ws.sib.msgstore.cache.statemodel.StateException;
import com.ibm.ws.sib.msgstore.deliverydelay.DeliveryDelayManager;
import com.ibm.ws.sib.msgstore.deliverydelay.DeliveryDelayable;
import com.ibm.ws.sib.msgstore.expiry.Expirable;
import com.ibm.ws.sib.msgstore.expiry.Expirer;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.list.Link;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.persistence.PersistentMessageStore;
import com.ibm.ws.sib.msgstore.task.AddTask;
import com.ibm.ws.sib.msgstore.task.PersistLock;
import com.ibm.ws.sib.msgstore.task.PersistRedeliveredCount;
import com.ibm.ws.sib.msgstore.task.PersistUnlock;
import com.ibm.ws.sib.msgstore.task.RemoveLockedTask;
import com.ibm.ws.sib.msgstore.task.RemoveTask;
import com.ibm.ws.sib.msgstore.task.Task;
import com.ibm.ws.sib.msgstore.task.TaskList;
import com.ibm.ws.sib.msgstore.task.UpdateTask;
import com.ibm.ws.sib.msgstore.transactions.Transaction;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.transactions.LocalTransaction;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.utils.DataSlice;
import com.ibm.ws.sib.utils.ras.FormattedWriter;
import com.ibm.ws.sib.utils.ras.SibTr;

/**
 * Important Terms:
 * 
 * Stable:
 * the item held by the link has a stable persistent representation. This
 * could be either because there is a persistent representation that contains the
 * correct state, or because the item is 'store never'.
 * 
 * 
 * Discardable:
 * the link is in a state in which it would be safe to discard the
 * owned item from memory if it were stable. A link can currently only be discardable
 * when in the available state or the locked state.
 * 
 * 
 * Releasable:
 * the link is both discardable and stable. In this state the cache may
 * decide to request that the link discard the item.
 * 
 * 
 * NonReleasable:
 * the link is not stable, not discardable, or neither.
 */
public abstract class AbstractItemLink extends Link implements Membership, Priorities, XmlConstants, Expirable, Indirection, DeliveryDelayable
{

    static final long EXPIRY_LOCK_ID = 123456L;
    private static final int NOT_SET = -1;
    public static final int MEMORY_SIZE_MULTIPLIER = 4;

    private static final long NO_LOCK_ID = com.ibm.ws.sib.msgstore.AbstractItem.NO_LOCK_ID;

    private static TraceComponent tc = SibTr.register(AbstractItemLink.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);
    private int _backoutCount = 0;

    // Defect 601995
    // These named classes will help to determine which reference
    // to the Item is being used and which is null when looking 
    // at a heap dump. Previously the two member variables,
    // _strongReferenceToItem and _itemCacheManagedReference, 
    // that could possibly refer to the Item were indistinguishable
    // from each other. This change should allow us to see if an
    // Item is being cached or not from within a heap dump.
    // 
    // NOTE: To minimise the reach of these classes through the code 
    // care needs to be taken when checking the value of our 
    // references and returning the results to users of this class.
    // Externally to this class the lack of an Item is still
    // represented by a value of null.
    private static final class NullStrongReferenceToItem extends Item {};

    private static final NullStrongReferenceToItem NULL_STRONG_REF = new NullStrongReferenceToItem();

    private static final class NullCacheReferenceToItem extends Item {};

    private static final NullCacheReferenceToItem NULL_CACHE_REF = new NullCacheReferenceToItem();

    private AbstractItem _strongReferenceToItem = NULL_STRONG_REF;

    /*
     * Cache the size we use for calculations of total item stream size,
     * to ensure that it does not unexpectedly change over the lifetime of the item.
     * The value is marked volatile to ensure any change is written back to memory
     * before another thread accesses it.
     * If there is any possibility the value may still be NOT_SET, use
     * getInMemoryItemSize() rather than accessing the variable directly.
     */
    private volatile int _inMemoryItemSize = NOT_SET;

    private boolean _isStorageManaged = false;

    private IndirectionCache _itemCache;

    // Defect 270103 - These three instance variables are synchronized under the IndirectionList monitor
    private Indirection _itemCacheLinkNext = null;
    private Indirection _itemCacheLinkPrevious = null;
    private AbstractItem _itemCacheManagedReference = NULL_CACHE_REF;

    private boolean _itemIsDiscardableIfPersistentRepresentationStable = false;

    private ItemLinkState _itemLinkState = ItemLinkState.STATE_NOT_STORED;
    private long _lockID = NO_LOCK_ID;

    // forward pointer to next AbstractItemLink in the itemLinkMap
    private AbstractItemLink _nextMappedLink;

    private final LinkOwner _owningStreamLink;
    private boolean _persistentRepresentationIsStable = false;

    private PersistentTranId _transactionId;
    private final Persistable _tuple;
    private boolean _persistentDataEncodingFailed = false;
    private int _unlockCount = 0;
    private SoftReference _softReferenceToItem;

    // This value is read from the persistent store during ME startup.
    // This value will not change for the life of the ME.
    // It is added to the unlock count values inside guessRedeliveredCount().
    // The sum is stored back to the persistent store.
    private int _persistedRedeliveredCount = 0;

    /**
     * This constructor is only for use by unit tests.
     */
    protected AbstractItemLink()
    {
        _owningStreamLink = null;
        _tuple = null;
        _inMemoryItemSize = 0;
    }

    /**
     * @param item
     * @param owningStreamLink
     * @param persistable
     * 
     * @throws OutOfCacheSpace if there is not enough space in the
     *             unstoredCache and the storage strategy is AbstractItem.STORE_NEVER.
     * 
     */
    protected AbstractItemLink(AbstractItem item, LinkOwner owningStreamLink, Persistable persistable) throws OutOfCacheSpace
    {
        super();
        _itemLinkState = ItemLinkState.STATE_NOT_STORED;
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", new Object[] { owningStreamLink, persistable });

        _tuple = persistable;
        int strategy;
        if (null == item)
        {
            // root membership has no Item
            strategy = AbstractItem.STORE_ALWAYS;
            _tuple.setStorageStrategy(AbstractItem.STORE_ALWAYS);
            _tuple.setItemClassName(MessageStoreImpl.class.getName());
            _inMemoryItemSize = 0;

            // Root membership does not need to be storage managed
            _isStorageManaged = false;
        }
        else
        {
            strategy = item.getStorageStrategy();
            _tuple.setStorageStrategy(strategy);
            _tuple.setItemClassName(item.getClass().getName());
            // use a hard indirection until we know which kind of soft we want
            // - which we will know at pre-prepare time (lateInitialize)

            // We have a real Item, so we can get its size and set it into the Tuple.
            setInMemoryItemSize(item);
            _tuple.setInMemoryByteSize(_inMemoryItemSize);

            // suppress storage management for streams by not
            // registering with the storage manager if the item is a stream
            _isStorageManaged = (item.isItem() || item.isItemReference());
        }

        _owningStreamLink = owningStreamLink;
        persistable.setAbstractItemLink(this);

        MessageStoreImpl msi = getMessageStoreImpl();
        if (_isStorageManaged)
        {
            // Defect 601995
            _itemCache = msi.getManagedCache().register(strategy);
        }

        _createSoftReference(item);
        _strongReferenceToItem = item;

        if (AbstractItem.STORE_NEVER == _tuple.getStorageStrategy())
        {
            // if the storage strategy is store never we can (now) discard
            // the item when appropriate. To do this we declare that the
            // reference is 'stable' - one of the preconditions for discard.
            _persistentRepresentationIsStable = true;
        }

        // Get the persisted redelivery count from the store
        _persistedRedeliveredCount = _tuple.getRedeliveredCount();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * Recreate a link from a tuple. Note that we only create the
     * link, and do not necessarily create the item.
     * 
     * @param owningStreamLink
     * @param persistable
     */
    protected AbstractItemLink(final LinkOwner owningStreamLink, final Persistable persistable)
    {
        super();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", new Object[] { owningStreamLink, persistable });

        _itemLinkState = ItemLinkState.STATE_NOT_STORED;
        _tuple = persistable;
        setInMemoryItemSize(null);
        persistable.setAbstractItemLink(this);
        _owningStreamLink = owningStreamLink;

        // Get the persisted redelivery count from the store
        _persistedRedeliveredCount = _tuple.getRedeliveredCount();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * Create root link. Note that we only create the
     * link, and do not create the item.
     */
    protected AbstractItemLink(final Persistable persistable, boolean isRootLink)
    {
        super();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "<init>", persistable);

        _itemLinkState = ItemLinkState.STATE_NOT_STORED;
        _tuple = persistable;
        setInMemoryItemSize(null);
        persistable.setAbstractItemLink(this);
        _owningStreamLink = null;

        // Get the persisted redelivery count from the store
        _persistedRedeliveredCount = _tuple.getRedeliveredCount();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "<init>");
    }

    /**
     * @param transaction the Transaction under which the child is being added
     * @param stats the parent statistics
     *            Throw a runtime exception if stream is full or in wrong state.
     * @throws InvalidAddOperation
     */
    synchronized final void _assertCanAddChild(PersistentTransaction transaction, ListStatistics stats) throws StreamIsFull, InvalidAddOperation
    {
        boolean allowed = false;

        if ((_itemLinkState == ItemLinkState.STATE_ADDING_LOCKED)
            || (_itemLinkState == ItemLinkState.STATE_ADDING_UNLOCKED))
        {
            if (getTransactionId().equals(transaction.getPersistentTranId()))
            {
                allowed = true;
            }
        }
        else if ((_itemLinkState == ItemLinkState.STATE_UPDATING_DATA)
                 || (_itemLinkState == ItemLinkState.STATE_AVAILABLE)
                 || (_itemLinkState == ItemLinkState.STATE_LOCKED)
                 || (_itemLinkState == ItemLinkState.STATE_PERSISTENTLY_LOCKED)
                 || (_itemLinkState == ItemLinkState.STATE_PERSISTING_LOCK)
                 || (_itemLinkState == ItemLinkState.STATE_UNLOCKING_PERSISTENTLY_LOCKED))
        {
            // Can add whatever to the transaction
            allowed = true;
        }
        else
        {
            allowed = false;
        }

        if (!allowed)
        {
            throw new InvalidAddOperation("STREAM_WRONG_STATE_SIMS0006", new Object[] { this, _itemLinkState }); // Defect 497842
        }
    }

    /**
     * @param transaction
     * @return if an add can be made to the ownew of the state
     *         provided. Throw a runtime exception otherwise.
     * @throws InvalidAddOperation
     */
    synchronized final void _assertCanAddChildUnder(PersistentTransaction transaction) throws InvalidAddOperation
    {
        boolean allowed = false;

        if ((_itemLinkState == ItemLinkState.STATE_ADDING_LOCKED)
            || (_itemLinkState == ItemLinkState.STATE_ADDING_UNLOCKED))
        {
            if (getTransactionId().equals(transaction.getPersistentTranId()))
            {
                allowed = true;
            }
        }
        else if ((_itemLinkState == ItemLinkState.STATE_UPDATING_DATA)
                 || (_itemLinkState == ItemLinkState.STATE_AVAILABLE))
        {
            // Can add whatever to the transaction
            allowed = true;
        }
        else
        {
            allowed = false;
        }

        if (!allowed)
        {
            throw new InvalidAddOperation("STREAM_WRONG_STATE_SIMS0006", new Object[] { this, _itemLinkState }); // Defect 497842
        }
    }

    /**
     * @param transaction
     * @return if a remove can be made from the owner of the state
     *         under the transaction provided. Throw a runtime exception otherwise.
     * @throws SevereMessageStoreException
     */
    synchronized final void _assertCanRemoveChildUnder(PersistentTransaction transaction) throws SevereMessageStoreException
    {
        boolean allowed = false;

        if ((_itemLinkState == ItemLinkState.STATE_UPDATING_DATA)
            || (_itemLinkState == ItemLinkState.STATE_AVAILABLE))
        {
            allowed = true;
        }
        else if ((_itemLinkState == ItemLinkState.STATE_REMOVING_EXPIRING)
                 || (_itemLinkState == ItemLinkState.STATE_REMOVING_LOCKED)
                 || (_itemLinkState == ItemLinkState.STATE_REMOVING_PERSISTENTLY_LOCKED)
                 || (_itemLinkState == ItemLinkState.STATE_REMOVING_WITHOUT_LOCK))
        {
            final PersistentTranId id = transaction.getPersistentTranId();
            if (id.equals(getTransactionId()))
            {
                allowed = true;
            }
        }
        else
        {
            allowed = false;
        }

        if (!allowed)
        {
            throw new SevereMessageStoreException(_itemLinkState
                                                  + " - Wrong transaction("
                                                  + transaction.getPersistentTranId()
                                                  + ") for action (old id="
                                                  + getTransactionId()
                                                  + ")");
        }
    }

    private final void _assertCorrectTransaction(PersistentTransaction transaction) throws SevereMessageStoreException
    {
        final PersistentTranId id = transaction.getPersistentTranId();
        if (!id.equals(_transactionId))
        {
            throw new SevereMessageStoreException(_itemLinkState + " - Wrong transaction(" + id + ") for action (old id=" + _transactionId + ")");
        }
    }

    private final void _createSoftReference(AbstractItem item) throws OutOfCacheSpace
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "creating soft reference");

        _softReferenceToItem = new SoftReference(item);
    }

    /**
     * The state has changed so that the item could be discarded if it has
     * a stable persistent representation.
     * Change the flags that control the discard, and return true if this call
     * has resulted in a change to the releasable state.
     * 
     * @return
     */
    private synchronized final boolean _declareDiscardable()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_declareDiscardable");

        boolean linkHasBecomeReleasable = false;
        // Only discardable if storage managed (items and item references only).
        // Putting this check in here liberates the logic which changes the state of the
        // items from knowing about storage management
        if (_isStorageManaged)
        {
            if (!_itemIsDiscardableIfPersistentRepresentationStable)
            {
                // if we have become non-discardable, and we were
                // releasable (stable) then we must notify the cache
                linkHasBecomeReleasable = _persistentRepresentationIsStable;
                // 274012
                if (linkHasBecomeReleasable)
                {
                    // Defect 601995
                    _strongReferenceToItem = NULL_STRONG_REF;
                }

                // This flag must not be set if the item is not storage managed.
                // Otherwise, we could inadvertently start discarding and restoring
                // streams.
                _itemIsDiscardableIfPersistentRepresentationStable = true;
            }
        }
        else
        {
            // Just to make sure :-)
            _itemIsDiscardableIfPersistentRepresentationStable = false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_declareDiscardable", Boolean.valueOf(linkHasBecomeReleasable));
        return linkHasBecomeReleasable;
    }

    /**
     * The state has changed so that the item must not be discarded
     * regardless of whether it has a stable persistent representation.
     * Change the flags that control the discard, and return true if this call
     * has resulted in a change to the releasable state.
     * 
     * @return
     */
    private synchronized final boolean _declareNotDiscardable(AbstractItem item)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_declareNotDiscardable");

        boolean notifyRequired = false;
        // we are going to need the item in memory, so we make sure that
        // we pin it with a hard reference
        _strongReferenceToItem = item;

        if (_itemIsDiscardableIfPersistentRepresentationStable)
        {
            // if we have become discardable, and we were
            // releasable (stable) then we must notify the cache
            notifyRequired = _persistentRepresentationIsStable;
            _itemIsDiscardableIfPersistentRepresentationStable = false;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_declareNotDiscardable", Boolean.valueOf(notifyRequired));
        return notifyRequired;
    }

    /**
     * inform the cache (if there is one) that the item owned by this
     * link is not releasable.
     * 
     * @throws SevereMessageStoreException
     */
    private final void _declareNotReleasable() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_declareNotReleasable");

        if (_isStorageManaged)
        {
            _itemCache.unmanage(this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_declareNotReleasable");
    }

    /**
     * inform the cache (if there is one) that the item owned by this
     * link is releasable.
     * 
     * @throws SevereMessageStoreException
     */
    private final void _declareReleasable(AbstractItem item) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_declareReleasable");

        if (_isStorageManaged)
        {
            // 274012 - now set to null under the AIL's monitor in _declareDiscardable
            // and persistentRepresentationIsStable
            // _strongReferenceToItem = null;
            _itemCache.manage(this, item);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_declareReleasable");
    }

    /**
     * Gets a reference to the item, which is asserted to be obtained from the
     * strong reference. We use this when we know that the state of the item
     * tells us that there is a strong reference.
     * 
     * @throws SevereMessageStoreException
     */
    private final AbstractItem _getAndAssertItem() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "_getAndAssertItem");

        AbstractItem item = null;
        synchronized (this)
        {
            item = _strongReferenceToItem;
        }

        // Defect 601995
        if (item == NULL_STRONG_REF)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "_getAndAssertItem");
            throw new SevereMessageStoreException("_getAndAssertItem");
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "_getAndAssertItem", item);
        return item;
    }

    private final AbstractItem _getItemNoRestore()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "_getItemNoRestore");

        AbstractItem item = null;
        synchronized (this)
        {
            item = _strongReferenceToItem;
        }

        // Defect 602995
        // Don't let our named class get too far out into 
        // the public areas of the link where a lack of 
        // item is expected to be represented by a null.
        if (item == NULL_STRONG_REF)
        {
            item = null;
        }

        if ((null == item) && (null != _softReferenceToItem))
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(tc, "Retrieving Item from soft reference.");

            // Someone else may still have a reference to the item,
            // so we can save some trouble by recovering the item
            // from the weak reference. If the weak reference has been
            // cleared then the item will still be null, and the calling
            // code will just need to cope with it.
            item = (AbstractItem) _softReferenceToItem.get();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "_getItemNoRestore", item);
        return item;
    }

    private final AbstractItem _restoreItem() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_restoreItem");

        AbstractItem item = null;
        boolean linkHasBecomeNonReleasable = false;
        boolean linkHasBecomeReleasable = false;

        try
        {
            synchronized (this)
            {
                boolean unlink = false; // whether to unlink the item if it needs reading from persistence

                // ensure that we need to restore as the wait for
                // a lock may have allowed another thread to restore
                // the item
                item = _getItemNoRestore();
                if (null == item)
                {
                    MessageStoreImpl msi = getMessageStoreImpl();
                    if (_tuple.getStorageStrategy() == AbstractItem.STORE_NEVER)
                    {
                        // if we are a store never item we cannot restore from persistence
                        // so do nothing and let the null result unlink us.
                        if (ItemLinkState.STATE_NOT_STORED == _itemLinkState)
                        {
                            // always allowed, no change
                        }
                        else if (ItemLinkState.STATE_AVAILABLE == _itemLinkState)
                        {
                            unlink = true;
                        }
                        else
                        {
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(this, tc, "_restoreItem");
                            throw new StateException(_itemLinkState.toString());
                        }
                    }
                    else if (_persistentDataEncodingFailed)
                    {
                        // there is not going to be a persistent data representation that
                        // we can make sense of. We should only get here for STORE_MAYBE
                        // items in ItemLinkState.STATE_AVAILABLE.
                        if (_tuple.getStorageStrategy() == AbstractItem.STORE_MAYBE)
                        {
                            // if we have no persistent representation then we cannot restore
                            // from persistence so do nothing and let the null result unlink us.
                            if (ItemLinkState.STATE_NOT_STORED == _itemLinkState)
                            {
                                // always allowed, no change
                            }
                            else if (ItemLinkState.STATE_AVAILABLE == _itemLinkState)
                            {
                                unlink = true;
                            }
                            else
                            {
                                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                    SibTr.exit(this, tc, "_restoreItem");
                                throw new StateException(_itemLinkState.toString());
                            }
                        }
                        else
                        {
                            SevereMessageStoreException msre = new SevereMessageStoreException();
                            FFDCFilter.processException(msre, "com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink._restoreItem", "1:764:1.241", this, new Object[] { _tuple });
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, "Attempted to restore data not in store");
                            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                SibTr.exit(this, tc, "_restoreItem");
                            throw msre;
                        }
                    }
                    else if (isInStore())
                    {
                        // FSIB0112b.ms.1
                        long id = _tuple.getUniqueId();
                        List<DataSlice> dataSlices = readDataFromPersistence();
                        if (null != dataSlices)
                        {
                            try
                            {
                                String itemClassName = _tuple.getItemClassName();

                                // Load the item class
                                item = getMessageStoreImpl().getItemStreamInstance(itemClassName);

                                //item = (AbstractItem) Class.forName(itemClassName).newInstance();
                            } catch (Exception e)
                            {
                                FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink._restoreItem", "1:784:1.241", this);
                                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                                    SibTr.event(this, tc, "Exception caught while restoring item!", e);
                                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                                    SibTr.exit(this, tc, "_restoreItem");
                                throw new SevereMessageStoreException("RESTORING_ITEM_SIMS0502" + id, e);
                            }
                            // put the item into the message store map
                            // use the message store to route the setting of the membership into
                            // the item, by-passing the java visibility restrictions.
                            msi._setMembership(this, item);
                            // we need to do this last, so that the stream is completely plumbed
                            // in before we let the implementor loose on the restore method.
                            try
                            {
                                item.restore(dataSlices);

                                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                    SibTr.debug(this, tc, "item restored");
                            } catch (PersistentDataEncodingException pdee)
                            {
                                FFDCFilter.processException(pdee, "com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink._restoreItem", "1:803:1.241", this);
                                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                                    SibTr.event(this, tc, "Exception caught restoring Item!", pdee);
                            }
                            item.eventRestored();
                            itemHasBeenRestored(item);
                        }
                        else
                        {
                            // The row was in doubt when we restored the membership, but has since been
                            // rolled back.  Therefore we can treat this as a deleted membership
                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            {
                                SibTr.debug(this, tc, "Indoubt item disappeared: ", this);
                            }
                        }

                        if (null != item)
                        {
                            // We restored it so the persistent representation is stable
                            _persistentRepresentationIsStable = true;

                            // Re-initialise the primary flag for controlling strong and weak references
                            _itemIsDiscardableIfPersistentRepresentationStable = false;

                            _createSoftReference(item);
                            _isStorageManaged = (item.isItem() || item.isItemReference());
                            if (_isStorageManaged)
                            {
                                // Defect 601995
                                _itemCache = msi.getManagedCache().register(_tuple.getStorageStrategy());
                            }
                            else
                            {
                                _strongReferenceToItem = item;
                            }

                            if (isAvailable() || isPersistentlyLocked())
                            {
                                linkHasBecomeReleasable = _declareDiscardable();
                            }
                            else
                            {
                                linkHasBecomeNonReleasable = _declareNotDiscardable(item);
                            }

                            // Now we can get a better estimate of the in-memory size of
                            // the item, so fix it up.
                            // If the size changes, we must also update the list stats.
                            int oldSize = _inMemoryItemSize;
                            setInMemoryItemSize(item);
                            if (_inMemoryItemSize != oldSize)
                            {
                                ListStatistics stats = getParentStatistics();

                                // Defect 510343.1
                                stats.updateTotal(oldSize, _inMemoryItemSize);
                            }
                        }
                    }
                    else
                    {
                        // It is always possible to receive requests for the persistent data
                        // of an item not in the store because of requests by asynchronous
                        // threads and calls from methods which do not hold the AIL's monitor
                        // for the entirety of their execution.
                    }

                    if (unlink)
                    {
                        ListStatistics stats = getParentStatistics();

                        // Defect 510343.1
                        stats.decrementAvailable(_inMemoryItemSize);

                        _itemLinkState = ItemLinkState.STATE_NOT_STORED;
                        getMessageStoreImpl().unregister(this);
                        unlink();
                    } // end if (unlink)
                } // end if (null == item)
            } // end synchronized(this)

            if (linkHasBecomeReleasable)
            {
                _declareReleasable(item);
            }

            if (linkHasBecomeNonReleasable)
            {
                _declareNotReleasable();
            }
        } catch (OutOfCacheSpace e)
        {
            FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink._restoreItem", "1:897:1.241", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.event(tc, "Exception caught while restoring item!", e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(tc, "_restoreItem");
            throw new SevereMessageStoreException(e);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_restoreItem", item);
        return item;
    }

    private final void _restoreStateAdding(AbstractItemLink link, LinkOwner stream, final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_restoreStateAdding", new Object[] { link, stream, transaction });

        synchronized (this)
        {
            if (ItemLinkState.STATE_NOT_STORED == _itemLinkState)
            {
                // going 'not stored' to a 'stored' state requires
                // an increment in total count
                ListStatistics stats = getParentStatistics();

                // Defect 510343.1
                stats.incrementAdding(_inMemoryItemSize);

                _transactionId = transaction.getPersistentTranId();
                _itemLinkState = ItemLinkState.STATE_ADDING_UNLOCKED;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "State has already been set: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "_restoreStateAdding");
                throw new StateException(_itemLinkState.toString());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_restoreStateAdding");
    }

    final void _restoreStateAvailable(AbstractItemLink link, LinkOwner stream) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_restoreStateAvailable", new Object[] { link, stream });

        synchronized (this)
        {
            if (ItemLinkState.STATE_NOT_STORED == _itemLinkState)
            {
                // going 'not stored' to a 'stored' state requires
                // an increment in total count
                ListStatistics stats = getParentStatistics();

                // Defect 510343.1
                stats.incrementAvailable(_inMemoryItemSize);

                _itemLinkState = ItemLinkState.STATE_AVAILABLE;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "State has already been set: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "_restoreStateAvailable");
                throw new StateException(_itemLinkState.toString());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_restoreStateAvailable");
    }

    private final void _restoreStatePersistentlyLocked(AbstractItemLink link, LinkOwner stream, long lockID) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_restoreStatePersistentlyLocked", new Object[] { link, stream, Long.valueOf(lockID) });

        synchronized (this)
        {
            if (ItemLinkState.STATE_NOT_STORED == _itemLinkState)
            {
                // going 'not stored' to a 'stored' state requires
                // an increment in total count
                ListStatistics stats = getParentStatistics();

                // Defect 510343.1
                stats.incrementLocked(_inMemoryItemSize);

                _lockID = lockID;
                _itemLinkState = ItemLinkState.STATE_PERSISTENTLY_LOCKED;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "State has already been set: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "_restoreStatePersistentlyLocked");
                throw new StateException(_itemLinkState.toString());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_restoreStatePersistentlyLocked");
    }

    private final void _restoreStateLocked(AbstractItemLink link, LinkOwner stream, long lockID) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_restoreStateLocked", new Object[] { link, stream, Long.valueOf(lockID) });

        synchronized (this)
        {
            if (ItemLinkState.STATE_NOT_STORED == _itemLinkState)
            {
                // going 'not stored' to a 'stored' state requires
                // an increment in total count
                ListStatistics stats = getParentStatistics();

                stats.incrementLocked(_inMemoryItemSize);

                _lockID = lockID;
                // It has to set to STATE_LOCKED, because when deliverydelay manager unlocks the item using unlock(),
                // unlock() expects the state to be in either STATE_LOCKED or STATE_PERSISTENTLY_LOCKED
                _itemLinkState = ItemLinkState.STATE_LOCKED;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "State has already been set: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "_restoreStateLocked");
                throw new StateException(_itemLinkState.toString());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_restoreStateLocked");
    }

    private final void _restoreStateRemovingNoLock(AbstractItemLink link, LinkOwner stream, PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_restoreStateRemovingNoLock", new Object[] { link, stream, transaction });

        synchronized (this)
        {
            if (ItemLinkState.STATE_NOT_STORED == _itemLinkState)
            {
                // going 'not stored' to a 'stored' state requires
                // an increment in total count
                ListStatistics stats = getParentStatistics();

                // Defect 510343.1
                stats.incrementRemoving(_inMemoryItemSize);

                _transactionId = transaction.getPersistentTranId();
                _itemLinkState = ItemLinkState.STATE_REMOVING_WITHOUT_LOCK;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "State has already been set: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "_restoreStateRemovingNoLock");
                throw new StateException(_itemLinkState.toString());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_restoreStateRemovingNoLock");
    }

    private final void _restoreStateRemovingPersistentlyLocked(AbstractItemLink link, LinkOwner stream, long lockID, PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "_restoreStateRemovingPersistentlyLocked", new Object[] { link, stream, Long.valueOf(lockID), transaction });

        synchronized (this)
        {
            if (ItemLinkState.STATE_NOT_STORED == _itemLinkState)
            {
                // going 'not stored' to a 'stored' state requires
                // an increment in total count
                ListStatistics stats = getParentStatistics();

                // Defect 510343.1
                stats.incrementRemoving(_inMemoryItemSize);

                _lockID = lockID;
                _transactionId = transaction.getPersistentTranId();
                _itemLinkState = ItemLinkState.STATE_REMOVING_PERSISTENTLY_LOCKED;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "State has already been set: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "_restoreStateRemovingPersistentlyLocked");
                throw new StateException(_itemLinkState.toString());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "_restoreStateRemovingPersistentlyLocked");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.cache.xalist.Task#postAbort(com.ibm.ws.sib.msgstore.Transaction)
     */
    public void abortAdd(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "abortAdd", transaction);

        synchronized (this)
        {
            if ((_itemLinkState == ItemLinkState.STATE_ADDING_LOCKED)
                || (_itemLinkState == ItemLinkState.STATE_ADDING_UNLOCKED))
            {
                _assertCorrectTransaction(transaction);

                MessageStoreImpl msi = getMessageStoreImpl();
                Expirer expirer = msi._getExpirer();
                long expiryTime = getTuple().getExpiryTime();
                if (null != expirer && 0 != expiryTime)
                {
                    expirer.removeExpirable(this);
                }

                // Remove from the DeliveryDelayManager as well
                DeliveryDelayManager deliveryDelayManager = msi._getDeliveryDelayManager();
                long deliveryDelayTime = getTuple().getDeliveryDelayTime();
                if (null != deliveryDelayManager && 0 != deliveryDelayTime) {
                    deliveryDelayManager.removeDeliveryDelayable(this);
                }

                // going back to 'not stored' state requires a decrement in total count
                ListStatistics stats = getParentStatistics();

                // Defect 510343.1
                stats.decrementAdding(_inMemoryItemSize);

                _lockID = NO_LOCK_ID;
                _transactionId = null;
                _itemLinkState = ItemLinkState.STATE_NOT_STORED;

                msi.unregister(this);

                // Now, pay attention. This was added under defect 290610.
                // It looks it was omitted in changes for v6.0.2 and would result
                // in rolled back additions leaking into the linked lists.
                unlink();
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "abortAdd");
                throw new StateException(_itemLinkState.toString());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "abortAdd");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.task.TaskTarget#abortPersistLock(com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    public final void abortPersistLock(PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "abortPersistLock", transaction);

        synchronized (this)
        {
            if (ItemLinkState.STATE_PERSISTING_LOCK == _itemLinkState)
            {
                _assertCorrectTransaction(transaction);
                _itemLinkState = ItemLinkState.STATE_LOCKED;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "abortPersistLock");
                throw new StateException(_itemLinkState.toString());
            }
            _transactionId = null;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "abortPersistLock");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.task.TaskTarget#abortPersistUnlock(com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    public final void abortPersistUnlock(PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "abortPersistUnlock", transaction);

        boolean linkHasBecomeReleasable = false;
        AbstractItem item;
        synchronized (this)
        {
            if (ItemLinkState.STATE_UNLOCKING_PERSISTENTLY_LOCKED == _itemLinkState)
            {
                _assertCorrectTransaction(transaction);
                item = _getAndAssertItem();
                final int strategy = _tuple.getStorageStrategy();
                if ((AbstractItem.STORE_NEVER != strategy) &&
                    (AbstractItem.STORE_MAYBE != strategy))
                { // 272110 - added STORE_MAYBE to ease handling of corrupt persistent representation
                  // storable items can be discarded if in persistentLocked state
                    linkHasBecomeReleasable = _declareDiscardable();
                }
                _itemLinkState = ItemLinkState.STATE_PERSISTENTLY_LOCKED;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "abortPersistUnlock");
                throw new StateException(_itemLinkState.toString());
            }
            _transactionId = null;
        }
        // this stuff has to be done outside the synchronized block
        if (linkHasBecomeReleasable)
        {
            _declareReleasable(item);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "abortPersistUnlock");
    }

    /**
     * Use this to persist redelivered count. Invoke indirectly by the API.
     * This method uses auto-commit transaction to perform the task of persisting
     * the redelivered count. This does not involve any external participants.
     * Therefore, this operation does not require maintaining transaction states.
     * 
     * @param redeliveredCount
     * 
     * @throws SevereMessageStoreException
     */
    @Override
    public final void persistRedeliveredCount(int redeliveredCount) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "persistRedeliveredCount");

        PersistentTransaction msTran = (PersistentTransaction) getMessageStore().getTransactionFactory().createAutoCommitTransaction();

        // Set the value in the Persistable
        Persistable perTuple = getTuple();
        perTuple.setRedeliveredCount(redeliveredCount);

        // Create the persist task
        final Task persistTask = new PersistRedeliveredCount(this);

        // Add the task to worklist. Since this is an auto-commit transaction,
        // the task is run immediately and value is persisted.
        try {
            msTran.addWork(persistTask);
        } catch (Exception e) {
            FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink.persistRedeliveredCount", "1:1198:1.241", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.event(this, tc, "Exception caught persisting redelivery count!", e);
        }
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "persistRedeliveredCount");
    }

    public void abortRemove(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "abortRemove", transaction);

        boolean hasBecomeAvailable = false;
        boolean linkHasBecomeReleasable = false;
        ListStatistics stats = getParentStatistics();
        AbstractItem item;
        synchronized (this)
        {
            // Defect 451569
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "ItemLinkState=" + _itemLinkState);

            _assertCorrectTransaction(transaction);
            item = _getAndAssertItem();
            _backoutCount++;

            if ((_itemLinkState == ItemLinkState.STATE_REMOVING_EXPIRING)
                || (_itemLinkState == ItemLinkState.STATE_REMOVING_WITHOUT_LOCK))
            {
                synchronized (stats)
                {
                    stats.decrementRemoving();
                    stats.incrementAvailable();
                }

                linkHasBecomeReleasable = _declareDiscardable();
                _itemLinkState = ItemLinkState.STATE_AVAILABLE;
                hasBecomeAvailable = true;
            }
            else if (_itemLinkState == ItemLinkState.STATE_REMOVING_LOCKED)
            {

                synchronized (stats)
                {
                    stats.decrementRemoving();
                    stats.incrementLocked();
                }

                _itemLinkState = ItemLinkState.STATE_LOCKED;

            }
            else if (_itemLinkState == ItemLinkState.STATE_REMOVING_PERSISTENTLY_LOCKED)
            {
                synchronized (stats)
                {
                    stats.decrementRemoving();
                    stats.incrementLocked();
                }

                final int strategy = _tuple.getStorageStrategy();
                if ((AbstractItem.STORE_NEVER != strategy) &&
                    (AbstractItem.STORE_MAYBE != strategy))
                { // 272110 - added STORE_MAYBE to ease handling of corrupt persistent representation
                  // storable items can be discarded if in persistentLocked state
                    linkHasBecomeReleasable = _declareDiscardable();
                }
                _itemLinkState = ItemLinkState.STATE_PERSISTENTLY_LOCKED;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "abortRemove");
                throw new StateException(_itemLinkState.toString());
            }

            _transactionId = null;
        }

        // this stuff has to be done outside the synchronized block
        if (hasBecomeAvailable)
        { // all items are discardable in avaialable state.
            if (null != _owningStreamLink)
            {
                _owningStreamLink.linkAvailable(this);
            }
        }

        if (linkHasBecomeReleasable)
        {
            _declareReleasable(item);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "abortRemove");
    }

    /**
     * Stub function. No work is needed as the job of persisting redelivered count
     * is run as part of an auto-commit transaction.
     */
    public final void abortPersistRedeliveredCount(PersistentTransaction transaction)
    {}

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.task.TaskTarget#abortUpdate(com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    public final void abortUpdate(PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "abortUpdate", transaction);

        boolean linkHasBecomeReleasable = false;
        AbstractItem item;
        synchronized (this)
        {
            if (ItemLinkState.STATE_UPDATING_DATA == _itemLinkState)
            {
                _assertCorrectTransaction(transaction);
                item = _getAndAssertItem();

                ListStatistics stats = getParentStatistics();
                synchronized (stats)
                {
                    stats.decrementUpdating();
                    stats.incrementAvailable();
                }

                linkHasBecomeReleasable = _declareDiscardable();
                _itemLinkState = ItemLinkState.STATE_AVAILABLE;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "abortUpdate");
                throw new StateException(_itemLinkState.toString());
            }
            _transactionId = null;
        }
        // this stuff has to be done outside the synchronized block
        if (linkHasBecomeReleasable)
        {
            _declareReleasable(item);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "abortUpdate");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.cache.xalist.Link#assertCanDelete(com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    public SevereMessageStoreException assertCanDelete(PersistentTransaction transaction)
    {
        return null;
    }

    // For test purposes to drive restoration from the persistence layer
    public final void clearSoftReferenceToItem()
    {
        SoftReference ref = _softReferenceToItem;
        if (ref != null)
        {
            ref.clear();
        }
    }

    /**
     * Set the state to adding
     * 
     * @param stream stream to which add is being made
     * @param lockID
     * @param transaction
     * 
     * @throws StreamIsFull
     *             if the size of the stream would exceed the
     *             maximum permissable size if an add were performed.
     * @throws ProtocolException
     * @throws TransactionException
     * @throws SevereMessageStoreException
     */
    final void cmdAdd(final LinkOwner stream, long lockID, final PersistentTransaction transaction) throws StreamIsFull, ProtocolException, TransactionException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "cmdAdd", new Object[] { "Item Link:   " + this, "Stream Link: " + stream, formatLockId(lockID), "Transaction: " + transaction });

        synchronized (this)
        {
            if (ItemLinkState.STATE_NOT_STORED == _itemLinkState)
            {
                ListStatistics stats = getParentStatistics();
                stream._assertCanAddChild(transaction, stats);

                // Defect 510343.1
                stats.incrementAdding(_inMemoryItemSize);

                _lockID = lockID;
                _transactionId = transaction.getPersistentTranId();

                if (NO_LOCK_ID == lockID)
                {
                    _itemLinkState = ItemLinkState.STATE_ADDING_UNLOCKED;
                }
                else
                {
                    _itemLinkState = ItemLinkState.STATE_ADDING_LOCKED;
                }
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "cmdAdd");
                throw new StateException(_itemLinkState.toString());
            }
        }
        final Task task = new AddTask(this);
        transaction.addWork(task);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "cmdAdd");
    }

    final void cmdLock(long lockID) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "cmdLock", new Object[] { "Item Link:   " + this, "Stream Link: " + _owningStreamLink, formatLockId(lockID) });

        boolean hasBecomeNotReleasable = false;
        synchronized (this)
        {
            if (ItemLinkState.STATE_AVAILABLE == _itemLinkState)
            {
                ListStatistics stats = getParentStatistics();
                synchronized (stats)
                {
                    stats.incrementLocked();
                    stats.decrementAvailable();
                }

                hasBecomeNotReleasable = _declareNotDiscardable(_getItemNoRestore());
                _lockID = lockID;
                _itemLinkState = ItemLinkState.STATE_LOCKED;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "cmdLock");
                throw new StateException(_itemLinkState.toString());
            }
        }
        if (hasBecomeNotReleasable)
        {
            _declareNotReleasable();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "cmdLock");
    }

    /*
     * Use this to lock persistently. Invoke indirectly by the API.
     * 
     * @param transaction
     */
    final void cmdPersistLock(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "cmdPersistLock", new Object[] { "Item Link:   " + this, "Stream Link: " + _owningStreamLink, "Transaction: " + transaction });

        if (null != transaction)
        {
            boolean hasBecomeNonReleasable = false;
            synchronized (this)
            {
                if (ItemLinkState.STATE_LOCKED == _itemLinkState)
                {
                    _transactionId = transaction.getPersistentTranId();
                    hasBecomeNonReleasable = _declareNotDiscardable(_getAndAssertItem());
                    _itemLinkState = ItemLinkState.STATE_PERSISTING_LOCK;
                }
                else
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(this, tc, "cmdPersistLock");
                    throw new StateException(_itemLinkState.toString());
                }
            }
            if (hasBecomeNonReleasable)
            {
                _declareNotReleasable();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "cmdPersistLock");
    }

    @Override
    public final void cmdRemove(final long lockId, final Transaction transaction) throws ProtocolException, TransactionException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "cmdRemove", new Object[] { "Item Link:   " + this, "Stream Link: " + _owningStreamLink, formatLockId(lockId), "Transaction: " + transaction });

        boolean hasBecomeNotReleasable = false;
        // allow for throwing a state exception outside the sync block.....
        boolean requireStateException = false;
        Task task = null; // 237729 use task as a flag that we need to do work
        synchronized (this)
        {
            // Defect 451569
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "ItemLinkState=" + _itemLinkState);

            ListStatistics stats = getParentStatistics();

            if (_itemLinkState == ItemLinkState.STATE_AVAILABLE)
            {
                if (NO_LOCK_ID != lockId)
                {
                    throw new LockIdMismatch(NO_LOCK_ID, lockId);
                }
                hasBecomeNotReleasable = _declareNotDiscardable(_getItemNoRestore());

                synchronized (stats)
                {
                    stats.decrementAvailable();
                    stats.incrementRemoving();
                }

                _transactionId = transaction.getPersistentTranId();
                _itemLinkState = ItemLinkState.STATE_REMOVING_WITHOUT_LOCK;
                task = new RemoveTask(this); // 237729
            }
            else if (_itemLinkState == ItemLinkState.STATE_LOCKED)
            {
                if (_lockID != lockId)
                {
                    if (_lockID == Item.DELIVERY_DELAY_LOCK_ID) {
                        /*
                         * Do nothing!
                         * This scenario arises when we have deleted the
                         * destination and the delayed delivery messages are being removed from
                         * the destination from AsynchDeletionThread.cleanUpDestination()
                         * 
                         * The item will be locked with DELIVERY_DELAY_LOCK_ID but
                         * AsynchDeletionThread.cleanUpDestination()
                         * passes NO_LOCK_ID for removal.Hence we are absorbing the mismatch only for
                         * delayed delivery messages
                         */
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "Absorbing the LockIdMismatch exception"
                                                  + " since Item is locked using LockID=" + _lockID);
                    } else
                        throw new LockIdMismatch(_lockID, lockId);

                }

                synchronized (stats)
                {
                    stats.decrementLocked();
                    stats.incrementRemoving();
                }

                _transactionId = transaction.getPersistentTranId();
                _itemLinkState = ItemLinkState.STATE_REMOVING_LOCKED;
                task = new RemoveLockedTask(this); // 237729
            }
            else if (_itemLinkState == ItemLinkState.STATE_PERSISTENTLY_LOCKED)
            {
                if (_lockID != lockId)
                {
                    throw new LockIdMismatch(_lockID, lockId);
                }
                hasBecomeNotReleasable = _declareNotDiscardable(_getItemNoRestore());

                synchronized (stats)
                {
                    stats.decrementLocked();
                    stats.incrementRemoving();
                }

                _transactionId = transaction.getPersistentTranId();
                _itemLinkState = ItemLinkState.STATE_REMOVING_PERSISTENTLY_LOCKED;
                task = new RemoveLockedTask(this); // 237729
            }
            else if (_itemLinkState == ItemLinkState.STATE_NOT_STORED)
            {
                // A store never item may have been discarded without MP
                // knowing. So we can ignore the case where they request a remove
                // on an item that is not stored.
                if (AbstractItem.STORE_NEVER == _tuple.getStorageStrategy())
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "STORE_NEVER item is not in store - no exception thrown during remove");
                }
                else
                {
                    requireStateException = true;
                }
            }
            else
            {
                requireStateException = true;
            }

            if (requireStateException)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "cmdRemove");
                throw new StateException(_itemLinkState.toString());
            }
        }

        if (hasBecomeNotReleasable)
        {
            _declareNotReleasable();
        }

        if (null != task)
        { // 237729 only need to add work if a task was created
            ((PersistentTransaction) transaction).addWork(task);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "cmdRemove");
    }

    /**
     * Remove while locked for expiry
     * 
     * @param lockId
     * @param transaction
     * 
     * @throws ProtocolException
     * @throws TransactionException
     * @throws SevereMessageStoreException
     */
    final void cmdRemoveExpiring(final long lockId, final PersistentTransaction transaction) throws ProtocolException, TransactionException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "cmdRemoveExpiring", new Object[] { "Item Link:   " + this, "Stream Link: " + _owningStreamLink, formatLockId(lockId),
                                                                     "Transaction: " + transaction });

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "expiring item " + _tuple.getUniqueId());

        synchronized (this)
        {
            if (ItemLinkState.STATE_LOCKED_FOR_EXPIRY == _itemLinkState)
            {
                if (_lockID != lockId)
                {
                    throw new LockIdMismatch(_lockID, lockId);
                }

                ListStatistics stats = getParentStatistics();
                synchronized (stats)
                {
                    stats.decrementExpiring();
                    stats.incrementRemoving();
                }

                _transactionId = transaction.getPersistentTranId();
                _itemLinkState = ItemLinkState.STATE_REMOVING_EXPIRING;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "cmdRemoveExpiring");
                throw new StateException(_itemLinkState.toString());
            }
        }
        final Task task = new RemoveLockedTask(this);
        transaction.addWork(task);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "cmdRemoveExpiring");
    }

    final void cmdRemoveExpiryLock() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "cmdRemoveExpiryLock");

        boolean linkHasBecomeReleasable = false;
        AbstractItem item;
        synchronized (this)
        {
            if (ItemLinkState.STATE_LOCKED_FOR_EXPIRY == _itemLinkState)
            {
                item = _getAndAssertItem();

                ListStatistics stats = getParentStatistics();
                synchronized (stats)
                {
                    stats.decrementExpiring();
                    stats.incrementAvailable();
                }

                _lockID = NO_LOCK_ID;
                _transactionId = null;
                linkHasBecomeReleasable = _declareDiscardable();
                _itemLinkState = ItemLinkState.STATE_AVAILABLE;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "cmdRemoveExpiryLock");
                throw new StateException(_itemLinkState.toString());
            }
        }
        // we will only get here if we have become available
        // anything else is a runtime exception
        if (linkHasBecomeReleasable)
        {
            _declareReleasable(item);
        }
        if (null != _owningStreamLink)
        {
            _owningStreamLink.linkAvailable(this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "cmdRemoveExpiryLock");
    }

    /*
     * remove the item if it matches and is available
     * 
     * @param filter
     * 
     * @param lockID
     * 
     * @return item if locked
     * 
     * @throws MessageStoreException
     */
    final AbstractItem cmdRemoveIfMatches(final Filter filter, PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "cmdRemoveIfMatches", new Object[] { "Item Link:   " + this, "Stream Link: " + _owningStreamLink, "Filter:      " + filter,
                                                                      "Transaction: " + transaction });

        AbstractItem foundItem = null;

        /*
         * A new filter DeliveryDelayDeleteFilter is explicitly created
         * to fetch the items which are available or
         * are locked for delivery delay and off course which
         * are not expired
         */

        isExpired();
        /*
         * <p>
         * Prior to delivery delay feature when the destination is deleted,
         * the AsyncDeletionThread internally passes null as the filter and
         * used to delete only the available messages which are not expired.
         * But with the introduction of delivery delay feature items
         * locked for delivery delay that also must be deleted
         * or reallocated to exception destination.
         * <p>
         * Now when destination is deleted, if the item is either locked
         * for delivery delay or if its in available state and not expired
         * the item is eligible for deletion or reallocation to exception
         * destination
         */
        if (isAvailable() || (getLockID() == Item.DELIVERY_DELAY_LOCK_ID && isStateLocked()))
        {
            AbstractItem item = getItem();
            if (null == item)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "item does not exist");
            }
            else
            {
                try
                {
                    // DeliveryDelayDeleteFilter will always return true
                    if (null == filter || filter.filterMatches(item))
                    {
                        // only if we pass all the tests will take the monitor and lock.
                        boolean hasBecomeNotReleasable = false;
                        synchronized (this)
                        {
                            // matching may cause restore, which may cause expiry
                            if (isAvailable() || (_lockID == Item.DELIVERY_DELAY_LOCK_ID && isStateLocked()))
                            {
                                ListStatistics stats = getParentStatistics();
                                synchronized (stats)
                                {
                                    stats.decrementAvailable();
                                    stats.incrementRemoving();
                                }

                                foundItem = item;
                                hasBecomeNotReleasable = _declareNotDiscardable(item);

                                _transactionId = transaction.getPersistentTranId();

                                // to maintain consistency set the appropriate item link state
                                if (_lockID == Item.DELIVERY_DELAY_LOCK_ID)
                                    _itemLinkState = ItemLinkState.STATE_REMOVING_LOCKED;
                                else
                                    _itemLinkState = ItemLinkState.STATE_REMOVING_WITHOUT_LOCK;
                            }

                            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                                SibTr.debug(this, tc, "matched");
                        }

                        if (null != foundItem)
                        {
                            if (hasBecomeNotReleasable)
                            {
                                _declareNotReleasable();
                            }
                            Task task = new RemoveTask(this);
                            transaction.addWork(task);
                        }
                    }
                } catch (MessageStoreException e)
                {
                    //No FFDC Code Needed.
                    // assume exception is false return
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeIfMatches", foundItem);
        return foundItem;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.Membership#requestUpdate(com.ibm.ws.sib.msgstore.Transaction)
     */
    final void cmdRequestUpdate(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "cmdRequestUpdate", new Object[] { "Item Link:   " + this, "Stream Link: " + _owningStreamLink, "Transaction: " + transaction });

        boolean hasBecomeNotReleasable = false;
        synchronized (this)
        {
            if (ItemLinkState.STATE_AVAILABLE == _itemLinkState)
            {

                ListStatistics stats = getParentStatistics();
                synchronized (stats)
                {
                    stats.decrementAvailable();
                    stats.incrementUpdating();
                }

                _transactionId = transaction.getPersistentTranId();
                hasBecomeNotReleasable = _declareNotDiscardable(_getItemNoRestore());
                _itemLinkState = ItemLinkState.STATE_UPDATING_DATA;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "cmdRequestUpdate");
                throw new StateException(_itemLinkState.toString());
            }
        }
        if (hasBecomeNotReleasable)
        {
            _declareNotReleasable();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "cmdRequestUpdate");
    }

    public final void commitAdd(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "commitAdd", transaction);

        AbstractItem item;
        ListStatistics stats = getParentStatistics();
        boolean hasBecomeAvailable = false;
        boolean linkHasBecomeReleasable = false;
        synchronized (this)
        {
            // Defect 451569
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "ItemLinkState=" + _itemLinkState);

            if (_itemLinkState == ItemLinkState.STATE_ADDING_LOCKED)
            {
                _assertCorrectTransaction(transaction);
                item = _getAndAssertItem();

                synchronized (stats)
                {
                    stats.decrementAdding();
                    stats.incrementLocked();
                }

                registerWithExpirer();
                registerWithDeliveryDelayManager();
                // Defect 463642
                // Revert to using spill limits previously removed in SIB0112d.ms.2
                if (_owningStreamLink != null)
                {
                    _owningStreamLink.checkSpillLimits(); // Defect 484799
                }

                /*
                 * Implies we are adding delivery delayed message
                 * and its locked using DELIVERY_DELAY_LOCK_ID.
                 * We have to make item as releaseable other wise
                 * we will have always have strongreference to item
                 * which is undesirable
                 */
                if (_lockID == Item.DELIVERY_DELAY_LOCK_ID)
                    linkHasBecomeReleasable = _declareDiscardable();

                _itemLinkState = ItemLinkState.STATE_LOCKED;
            }
            else if (_itemLinkState == ItemLinkState.STATE_ADDING_UNLOCKED)
            {
                _assertCorrectTransaction(transaction);
                item = _getAndAssertItem();

                synchronized (stats)
                {
                    stats.decrementAdding();
                    stats.incrementAvailable();
                }

                registerWithExpirer();
                // Defect 463642
                // Revert to using spill limits previously removed in SIB0112d.ms.2
                if (_owningStreamLink != null)
                {
                    _owningStreamLink.checkSpillLimits(); // Defect 484799
                }
                hasBecomeAvailable = true;
                linkHasBecomeReleasable = _declareDiscardable();
                _itemLinkState = ItemLinkState.STATE_AVAILABLE;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "commitAdd");
                throw new StateException(_itemLinkState.toString());
            }

            _transactionId = null;
        }
        // this stuff has to be done outside the synchronized block
        if (hasBecomeAvailable)
        {
            // all items are discardable in avaialable state.
            // Drat.  Because we have gone for early insertion
            // of links they appear in the list before they become available.
            // This means that they can be passed over by a cursor during
            // addition.  As a result we need to notify them as available.
            if (_owningStreamLink != null)
            {
                _owningStreamLink.linkAvailable(this);
            }
        }

        if (linkHasBecomeReleasable)
        {
            _declareReleasable(item);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "commitAdd");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.task.TaskTarget#commitPersistLock(com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    public final void commitPersistLock(PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "commitPersistLock", transaction);

        AbstractItem item = null;
        boolean hasBecomePersistentlyLocked = false;
        boolean linkHasBecomeReleasable = false;
        synchronized (this)
        {
            if (ItemLinkState.STATE_PERSISTING_LOCK == _itemLinkState)
            {
                _assertCorrectTransaction(transaction);
                item = _getAndAssertItem();
                final int strategy = _tuple.getStorageStrategy();
                if ((AbstractItem.STORE_NEVER != strategy) &&
                    (AbstractItem.STORE_MAYBE != strategy))
                { // 272110 - added STORE_MAYBE to ease handling of corrupt persistent representation
                  // storable items can be discarded if in persistentLocked state
                    linkHasBecomeReleasable = _declareDiscardable();
                }
                _itemLinkState = ItemLinkState.STATE_PERSISTENTLY_LOCKED;
                hasBecomePersistentlyLocked = true;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "commitPersistLock");
                throw new StateException(_itemLinkState.toString());
            }
            _transactionId = null;
        }
        if (hasBecomePersistentlyLocked)
        {
            if (linkHasBecomeReleasable)
            {
                _declareReleasable(item);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "commitPersistLock");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.task.TaskTarget#commitPersistUnlock(com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    public final void commitPersistUnlock(PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "commitPersistUnlock", transaction);

        AbstractItem item = null;
        boolean hasBecomeAvailable = false;
        boolean linkHasBecomeReleasable = false;
        synchronized (this)
        {
            if (ItemLinkState.STATE_UNLOCKING_PERSISTENTLY_LOCKED == _itemLinkState)
            {
                _assertCorrectTransaction(transaction);

                item = getItem();

                ListStatistics stats = getParentStatistics();
                synchronized (stats)
                {
                    stats.decrementLocked();
                    stats.incrementAvailable();
                }

                // Save the item before we change the state
                _lockID = NO_LOCK_ID;
                _unlockCount++;
                linkHasBecomeReleasable = _declareDiscardable();
                _itemLinkState = ItemLinkState.STATE_AVAILABLE;
                hasBecomeAvailable = true;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "commitPersistUnlock");
                throw new StateException(_itemLinkState.toString());
            }
            _transactionId = null;
        }
        // this stuff has to be done outside the synchronized block
        if (linkHasBecomeReleasable)
        {
            _declareReleasable(item);
        }

        // PK37596
        // Now we have completed an unlock we have become available again
        // and so need to inform our containing list in case we need to
        // be added to a cursors jumpback list.
        if (hasBecomeAvailable)
        {
            // all items are discardable in avaialable state.
            if (_owningStreamLink != null)
            {
                _owningStreamLink.linkAvailable(this);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "commitPersistUnlock");
    }

    public final void commitRemove(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "commitRemove", transaction);

        synchronized (this)
        {
            if ((_itemLinkState == ItemLinkState.STATE_REMOVING_EXPIRING)
                || (_itemLinkState == ItemLinkState.STATE_REMOVING_LOCKED)
                || (_itemLinkState == ItemLinkState.STATE_REMOVING_PERSISTENTLY_LOCKED)
                || (_itemLinkState == ItemLinkState.STATE_REMOVING_WITHOUT_LOCK))
            {
                _assertCorrectTransaction(transaction);

                // Defect 463642
                // Revert to using spill limits previously removed in SIB0112d.ms.2
                if (_owningStreamLink != null)
                {
                    _owningStreamLink.checkSpillLimits(); // Defect 484799
                }

                // going 'not stored' to a 'stored' state requires
                // a decrement in total count
                ListStatistics stats = getParentStatistics();

                // Defect 510343.1
                stats.decrementRemoving(_inMemoryItemSize);

                MessageStoreImpl msi = getMessageStoreImpl();
                if (ItemLinkState.STATE_REMOVING_EXPIRING != _itemLinkState)
                {
                    Expirer expirer = msi._getExpirer();
                    long expiryTime = getTuple().getExpiryTime();
                    if (null != expirer && 0 != expiryTime)
                    {
                        expirer.removeExpirable(this);
                    }
                }

                /*
                 * Item is being removed hence, delete it from the DeliveryDelay Manager as well.
                 * Scenario where Destination is deleted
                 */
                DeliveryDelayManager deliveryDelayManager = msi._getDeliveryDelayManager();
                long deliveryDelayTime = getTuple().getDeliveryDelayTime();
                if (null != deliveryDelayManager && 0 != deliveryDelayTime) {
                    deliveryDelayManager.removeDeliveryDelayable(this);
                }

                _lockID = NO_LOCK_ID;
                _itemLinkState = ItemLinkState.STATE_NOT_STORED;

                msi.unregister(this);
                unlink();
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "commitRemove");
                throw new StateException(_itemLinkState.toString());
            }

            _transactionId = null;
        }

        internalCommitRemove(transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "commitRemove");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.task.TaskTarget#commitUpdate(com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    public final void commitUpdate(PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "commitUpdate", transaction);

        AbstractItem item = null;
        boolean linkHasBecomeReleasable = false;
        synchronized (this)
        {
            if (ItemLinkState.STATE_UPDATING_DATA == _itemLinkState)
            {
                _assertCorrectTransaction(transaction);
                item = _getAndAssertItem();

                ListStatistics stats = getParentStatistics();
                synchronized (stats)
                {
                    stats.decrementUpdating();
                    stats.incrementAvailable();
                }

                linkHasBecomeReleasable = _declareDiscardable();
                _itemLinkState = ItemLinkState.STATE_AVAILABLE;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "commitUpdate");
                throw new StateException(_itemLinkState.toString());
            }
            _transactionId = null;
        }
        // this stuff has to be done outside the synchronized block
        if (linkHasBecomeReleasable)
        {
            _declareReleasable(item);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "commitUpdate");
    }

    @Override
    public synchronized boolean expirableExpire(PersistentTransaction transaction) throws SevereMessageStoreException
    { // 179365.3
      // expire the receiver.  If any error happens, abort expiry and
      // leave the item in the list
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "expirableExpire", "tran=" + transaction);

        boolean removeFromExpiryList = false;
        boolean isExpired = isExpired();

        if (isExpired)
        {
            // if there is just impediment to expiry (ie references)
            //    remove the expiry lock
            //
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "item is expiring " + getID());

            try
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "expiring item " + getID());

                cmdRemoveExpiring(EXPIRY_LOCK_ID, transaction);
                /*
                 * At this state the item is removed.Hence we can delete from
                 * DeliveryDelayManager index as well.
                 * Rare scenario where Expirer will trigger first then the DeliveryDelayManager.In this case
                 * we will remove from the Unloker index as well
                 */
                MessageStoreImpl msi = getMessageStoreImpl();
                DeliveryDelayManager deliveryDelayManager = msi._getDeliveryDelayManager();
                long deliveryDelayTime = getTuple().getDeliveryDelayTime();
                if (null != deliveryDelayManager && 0 != deliveryDelayTime) {
                    deliveryDelayManager.removeDeliveryDelayable(this);
                }

                removeFromExpiryList = true;
                boolean canExpireSilently = getTuple().getCanExpireSilently();
                if (!canExpireSilently)
                {
                    // only bother looking for the item if we need to tell it about expiry.
                    // if the item is not in memory we need to get it in
                    AbstractItem item = getItem();
                    if (null != item)
                    {
                        item.eventExpiryNotification(transaction); // 179365.3
                    }
                }
            } catch (MessageStoreException e)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink.expirableExpire", "1:2130:1.241", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "failed to expire item " + getID() + " because: " + e);
                // No FFDC code needed. Note that the default behaviour on an exception
                // is to remove the expiry lock but leave the item in the expiry list.
                cmdRemoveExpiryLock();
            }
        }
        else if (!isInStore())
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "item already removed from store");
            // if the item has been removed for some other reason we can eliminate it from
            // the expiry list.
            removeFromExpiryList = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "expirableExpire", Boolean.valueOf(removeFromExpiryList));
        return removeFromExpiryList;
    }

    @Override
    public final long expirableGetExpiryTime()
    { // 183455
        long expiryTime = getTuple().getExpiryTime();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "expirableGetExpiryTime");
            SibTr.exit(this, tc, "expirableGetExpiryTime", Long.valueOf(expiryTime));
        }
        return expiryTime; // 183455
    }

    @Override
    public final long expirableGetID()
    {
        long l = getID();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "expirableGetID");
            SibTr.exit(this, tc, "expirableGetID", Long.valueOf(l));
        }
        return l;
    }

    @Override
    public final boolean expirableIsInStore()
    {
        return isInStore();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.deliverydelay.DeliveryDelayable#deliveryDelayableUnlock(com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction)
     * 
     * Note : synchronized block must not be used here!If intrensic lock is acquired and then unlock is invoked,
     * The ready consumer(different thread) will try to check if the item is available using
     * the intrensic lock which will not be available.
     * And the ready consumer will not be able to proceed with the consumption.
     * unlock() uses the intrensic lock so we are safe here
     */
    @Override
    public boolean deliveryDelayableUnlock(PersistentTransaction tran, long lockID) throws MessageStoreException {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "deliveryDelayableUnlock", "tran=" + tran + "lockID=" + lockID);

        boolean removeFromDeliveryDelayIndex = false;
        try {
            /*
             * Only if the item is not expiring and its in store and if its not being
             * removed(removal can happen when destination is being deleted)
             * we have to unlock.
             * Else it means the item is being expired and there is no point unlocking an expired item
             */
            if (!isExpired() && isInStore() && isStateLocked()) {
                //Unlock the item and dont increment the unlock count
                unlock(lockID, tran, false);
            } else {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Did not unlock item " + getID() + "whoes item link state is :" + _itemLinkState
                                          + " because: either"
                                          + "its expired or its not in store or the item link state is not "
                                          + "ItemLinkState.STATE_LOCKED");
            }
            // Remove from the unlock index
            removeFromDeliveryDelayIndex = true;
        } catch (MessageStoreException e) {
            //Something wrong has happened.We should not remove from the DeliveryDelayManager so that we can try next time
            removeFromDeliveryDelayIndex = false;
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink.deliveryDelayableUnlock", "1:231:1.145", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "failed to unlock item " + getID() + " because: " + e);

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "deliveryDelayableUnlock", removeFromDeliveryDelayIndex);
        return removeFromDeliveryDelayIndex;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.deliverydelay.DeliveryDelayable#handleInvalidDeliveryDelayable(com.ibm.ws.sib.msgstore.MessageStoreConstants.MAXIMUM_ALLOWED_DELIVERY_DELAY_ACTION)
     */
    @Override
    public boolean handleInvalidDeliveryDelayable(MaximumAllowedDeliveryDelayAction action) 
    	throws MessageStoreException, SIException {
    	final String methodName = "handleInvalidDeliveryDelayable";
    	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
             SibTr.entry(this, tc, methodName, "action=" + action);
    	
    	boolean unlocked = false;
    	if (_tuple.getDeliveryDelayTimeIsSuspect()) {
    		try {
    			// getItem() will pull the whole item into storage, so by now we should be sure we are actually going to do something.
    			AbstractItem abstractItem = getItem();
    			if (abstractItem != null) {
    				abstractItem.handleInvalidDeliveryDelayable(action);
    				switch(action) {
    				case unlock:// No break;
    				case exception:
    					LocalTransaction transaction = getMessageStore().getTransactionFactory().createLocalTransaction();
    					// Tell the DeliveryDelayable to unlock. If it returns true, then remove it from the DeliveryDelay index on return.
    					unlocked = deliveryDelayableUnlock((PersistentTransaction) transaction, AbstractItem.DELIVERY_DELAY_LOCK_ID);
    					transaction.commit();
    					break;

    				case warn:// No break;
    				default:
    					break;
    				} 	
    			}
    		} catch (MessageStoreException | SIException exception) {
    			SibTr.exit(this, tc, methodName, exception);
    			throw exception;
    		}
    	}

    	
    	if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
             SibTr.exit(this, tc, methodName, unlocked);
    	return unlocked;
    }
    
    /**
     * Simple function to check if the item link state is
     * in ItemLinkState.STATE_LOCKED.Seperate function is
     * created to acquire the lock
     * 
     * @return
     */
    private synchronized final boolean isStateLocked()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "isStateLocked");
            SibTr.exit(this, tc, "isStateLocked", _itemLinkState);
        }
        if (_itemLinkState == ItemLinkState.STATE_LOCKED)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.deliverydelay.DeliveryDelayable#deliveryDelayableGetDeliveryDelayTime()
     */
    @Override
    public long deliveryDelayableGetDeliveryDelayTime() {
        long deliveryDelayTime = getTuple().getDeliveryDelayTime();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "deliveryDelayableGetDeliveryDelayTime");
            SibTr.exit(this, tc, "deliveryDelayableGetDeliveryDelayTime", Long.valueOf(deliveryDelayTime));
        }
        return deliveryDelayTime; // 183455
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.deliverydelay.DeliveryDelayable#deliveryDelayableGetID()
     */
    @Override
    public long deliveryDelayableGetID() {
        long l = getID();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "deliveryDelayableGetID");
            SibTr.exit(this, tc, "deliveryDelayableGetID", Long.valueOf(l));
        }
        return l;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.deliverydelay.DeliveryDelayable#deliveryDelayableIsInStore()
     */
    @Override
    public boolean deliveryDelayableIsInStore() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "deliveryDelayableIsInStore");

        boolean isInStore = isInStore();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "deliveryDelayableIsInStore", isInStore);
        return isInStore;
    }

    // Defect 601995
    public final String formatLockId(Long lockID)
    {
        String retval;

        if (lockID == NO_LOCK_ID)
        {
            retval = "Lock ID:     NO_LOCK_ID";
        }
        else if (lockID == EXPIRY_LOCK_ID)
        {
            retval = "Lock ID:     EXPIRY_LOCK_ID";
        }
        else
        {
            retval = "Lock ID:     " + lockID;
        }

        return retval;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.Membership#getID()
     */
    @Override
    public final long getID()
    {
        return getTuple().getUniqueId();
    }

    /**
     * When the item is required, the hard and soft references are examined to
     * detect an existing item in memory. If there is one then this item is used.
     * If the item does not have a hard or soft reference, but does have a
     * persistent representation, then we attempt to restore the item from its
     * persistent representation.
     * If we have no persistent representation, or fail to recreate from the
     * persistent representation, then we unlink the link.
     * 
     * @return the item.
     * @throws SevereMessageStoreException
     * 
     */
    public final AbstractItem getItem() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getItem");

        AbstractItem item = _getItemNoRestore();
        synchronized (this)
        {
            if (isExpired())
            {
                item = null;
            }
            else
            {
                // if we did not have a reference to the item then we try to recreate
                if (null == item)
                {
                    try
                    {
                        item = _restoreItem();
                    } catch (SevereMessageStoreException e)
                    {
                        // No FFDC Code Needed.
                        // PK54812 Ensure we dump a suitable FFDC containing all of the message store
                        // information required in order to identify the rows etc. related to this entry.
                        try
                        {
                            StringWriter stringWriter = new StringWriter();
                            FormattedWriter writer = new FormattedWriter(stringWriter);
                            this.xmlWriteOn(writer);
                            FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink.getItem", "1:2252:1.241", this, new Object[] { stringWriter });
                            writer.close();
                        } catch (IOException ioe)
                        {
                            FFDCFilter.processException(ioe, "com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink.getItem", "1:2257:1.241", this);
                        }

                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            SibTr.event(this, tc, "RuntimeException caught restoring Item!", e);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(this, tc, "getItem");
                        throw e;
                    } catch (RuntimeException re)
                    {
                        // No FFDC Code Needed.
                        // PK54812 Ensure we dump a suitable FFDC containing all of the message store
                        // information required in order to identify the rows etc. related to this entry.
                        try
                        {
                            StringWriter stringWriter = new StringWriter();
                            FormattedWriter writer = new FormattedWriter(stringWriter);
                            this.xmlWriteOn(writer);
                            FFDCFilter.processException(re, "com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink.getItem", "1:2274:1.241", this, new Object[] { stringWriter });
                            writer.close();
                        } catch (IOException ioe)
                        {
                            FFDCFilter.processException(ioe, "com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink.getItem", "1:2279:1.241", this);
                        }

                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            SibTr.event(this, tc, "RuntimeException caught restoring Item!", re);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(this, tc, "getItem");
                        throw re;
                    }
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getItem", item);
        }
        return item;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.cache.statemodel.State#getLockID()
     */
    @Override
    public final synchronized long getLockID()
    {
        return _lockID;
    }

    /**
     * Called by the persistence layer to get the item data.
     * 
     * @return The item data.
     * @throws SevereMessageStoreException
     */
    // FSIB0112b.ms.1
    public final List<DataSlice> getMemberData() throws PersistentDataEncodingException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "getMemberData");

        List<DataSlice> dataSlices = null;
        try
        {
            AbstractItem item = getItem();
            if (null != item)
            {
                dataSlices = item.getPersistentData();
            }
        } catch (PersistentDataEncodingException pdee)
        {
            FFDCFilter.processException(pdee, "com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink.getMemberData", "1:2324:1.241", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                SibTr.event(this, tc, "Exception caught encoding member data!", pdee);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "getMemberData");
            synchronized (this)
            {
                _persistentDataEncodingFailed = true;
            }
            throw pdee;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "getMemberData", "return=" + dataSlices);
        return dataSlices;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.Membership#getMessageStore()
     */
    @Override
    public final MessageStore getMessageStore()
    {
        return getMessageStoreImpl();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.Membership#getMessageStore()
     */
    public MessageStoreImpl getMessageStoreImpl()
    {
        return _owningStreamLink.getMessageStoreImpl();
    }

    /**
     * @return Next pointer in the itemLinkMap or null if none
     */
    public final AbstractItemLink getNextMappedLink()
    {
        return _nextMappedLink;
    }

    public final LinkOwner getOwningStreamLink()
    {
        return _owningStreamLink;
    }

    ListStatistics getParentStatistics()
    {
        return _owningStreamLink.getListStatistics();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.MessageStore.Membership#getPriority()
     */
    @Override
    public final int getPriority()
    {
        int priority = getTuple().getPriority();
        return priority;
    }

    @Override
    public final int getPersistedRedeliveredCount()
    {
        return _persistedRedeliveredCount;
    }

    @Override
    public long getSequence()
    {
        long sequence = getTuple().getSequence();
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getSequence");
            SibTr.exit(this, tc, "getSequence", Long.valueOf(sequence));
        }
        return sequence;
    }

    /**
     * getInMemoryItemSize
     * Get the approximate maximum number of bytes the Item can take up in memory.
     * 
     * @return int The approx in-memory size of the Item
     */
    @Override
    public final int getInMemoryItemSize()
    {
        // If the _inMemoryItemSize is not set, set it now before returning it.
        // This is belt-and-braces, as we're pretty sure it always will be set
        // before we try to use it. This is certainly the case for all uses within
        // this class, but just in case we get called by someone we weren't expecting,
        // we'll make sure we have a relatively sensible value to return.
        if (_inMemoryItemSize == NOT_SET) {
            // If we can get hold of the Item without restoring it from disk, we can use
            // it to help set the estimated item size.
            setInMemoryItemSize(_getItemNoRestore());
        }
        return _inMemoryItemSize;
    }

    /**
     * setInMemoryItemSize
     * Set the _inMemoryItemSize from the Item itself, if we have one, or from what
     * we can find out from the Tuple. Also set the corresponding value in the Tuple.
     */
    private final void setInMemoryItemSize(AbstractItem item)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "setInMemoryItemSize", item);
        // If we have an Item, we can ask it for its estimated size.
        if (item != null) {
            _inMemoryItemSize = item.getInMemoryDataSize();
        }
        // Otherwise, we just use its persisted size & our standard multiplier.
        else {
            _inMemoryItemSize = _tuple.getPersistentSize() * MEMORY_SIZE_MULTIPLIER;
        }
        // Set the value on the Tuple.
        _tuple.setInMemoryByteSize(_inMemoryItemSize);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "setInMemoryItemSize", _inMemoryItemSize);
    }

    // Defect 522135
    // This method should not be synchronized as taking the lock
    // on the link when outputting the state in trace statements
    // in the SubCursor can cause a deadlock. This method is only
    // currently used for reading the AIL state for debug output
    // so a lack of synchronization is also less of a worry.
    public final ItemLinkState getState()
    {
        return _itemLinkState;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.cache.statemodel.State#getTransactionId()
     */
    @Override
    public final PersistentTranId getTransactionId()
    {
        return _transactionId;
    }

    public final Persistable getTuple()
    {
        return _tuple;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.store.Membership#guessBackoutCount()
     */
    @Override
    public final int guessBackoutCount()
    {
        return _backoutCount;
    }

    @Override
    public final int guessUnlockCount()
    {
        return _unlockCount;
    }

    protected final void initializeSequenceNumber()
    {
        getTuple().setSequence(_owningStreamLink.nextSequence());
    }

    /**
     * @return true if can expire the link now. This is a last
     *         chance to block
     */
    boolean internalCanExpire()
    {
        return getMessageStoreImpl().itemsCanExpire();
    }

    /**
     * Handle all internal changes (except externally visible state change) before
     * the externally visible commit call.
     */
    public void internalCommitAdd()
    {
        // defect 178563: create an internal commit phase, so all actions are finished
        // before commit changes the state
    }

    /**
     * Handle all internal changes (except externally visible state change) before
     * the externally visible commit call.
     * Currently only used for ItemReferenceLink
     * 
     * @param transaction
     */
    public void internalCommitRemove(PersistentTransaction transaction) throws SevereMessageStoreException
    {
        // defect 178563: create an internal commit phase, so all actions are finished
        // before commit changes the state
    }

    @Override
    public synchronized final boolean isAdding()
    {
        return ItemLinkState.STATE_ADDING_LOCKED == _itemLinkState || ItemLinkState.STATE_ADDING_UNLOCKED == _itemLinkState;
    }

    @Override
    public synchronized final boolean isAvailable()
    {
        return ItemLinkState.STATE_AVAILABLE == _itemLinkState;
    }

    public synchronized final boolean isCached()
    {
        return null != _itemCache;
    }

    // For testing purposes
    public final boolean isCacheManagedReferenceToItem()
    {
        // Defect 601995
        return (_itemCacheManagedReference != NULL_CACHE_REF);
    }

    // For testing purposes
    public synchronized final boolean isDiscardableIfPersistentRepresentationIsStable()
    {
        return _itemIsDiscardableIfPersistentRepresentationStable;
    }

    /**
     * Establish whether the item has reached its expiry time and if so, query
     * it to see if it is willing to expire. If so, set the state accordingly
     * and lock the item for expiry.
     * 
     * @return true if the item has already expired or if it has now been
     *         found to have reached its expiry time and is ready to be expired.
     *         Returns true if the receiver is in the 'lockedForExpiry' state at the
     *         end of the method.
     * @throws SevereMessageStoreException
     */
    final boolean isExpired() throws SevereMessageStoreException
    { // 182086
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "isExpired");

        if (isAvailable() && internalCanExpire())
        {
            // we can only expire if we are available
            long expiryTime = _tuple.getExpiryTime();
            if (expiryTime != 0 && expiryTime <= System.currentTimeMillis())
            {
                // we are due to expire
                boolean hasBecomeNonReleasable = false;
                synchronized (this)
                {
                    if (ItemLinkState.STATE_AVAILABLE == _itemLinkState)
                    {
                        // force item to be present
                        AbstractItem item = _getItemNoRestore();
                        if (null == item)
                        {
                            item = _restoreItem();
                        }

                        ListStatistics stats = getParentStatistics();
                        synchronized (stats)
                        {
                            stats.incrementExpiring();
                            stats.decrementAvailable();
                        }

                        _lockID = AbstractItemLink.EXPIRY_LOCK_ID;
                        hasBecomeNonReleasable = _declareNotDiscardable(item);
                        _itemLinkState = ItemLinkState.STATE_LOCKED_FOR_EXPIRY;
                    }
                }
                if (hasBecomeNonReleasable)
                {
                    _declareNotReleasable();
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "isExpired", Boolean.valueOf(isExpiring()));
        return isExpiring();
    }

    @Override
    public final boolean isExpiring()
    {
        return isLockedForExpiry();
    }

    @Override
    public synchronized final boolean isInStore()
    {
        return ItemLinkState.STATE_NOT_STORED != _itemLinkState;
    }

    public boolean isItemStreamLink()
    {
        return false;
    }

    @Override
    public synchronized final boolean isLocked()
    {
        if ((_itemLinkState == ItemLinkState.STATE_LOCKED)
            || (_itemLinkState == ItemLinkState.STATE_LOCKED_FOR_EXPIRY)
            || (_itemLinkState == ItemLinkState.STATE_PERSISTENTLY_LOCKED)
            || (_itemLinkState == ItemLinkState.STATE_PERSISTING_LOCK)
            || (_itemLinkState == ItemLinkState.STATE_UNLOCKING_PERSISTENTLY_LOCKED))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    final synchronized boolean isLockedForExpiry()
    {
        return ItemLinkState.STATE_LOCKED_FOR_EXPIRY == _itemLinkState;
    }

    public final boolean isPersistentDataImmutable() throws SevereMessageStoreException
    {
        return getItem().isPersistentDataImmutable();
    }

    public final boolean isPersistentDataNeverUpdated() throws SevereMessageStoreException
    {
        return getItem().isPersistentDataNeverUpdated();
    }

    @Override
    public synchronized final boolean isPersistentlyLocked()
    {
        return ItemLinkState.STATE_PERSISTENTLY_LOCKED == _itemLinkState;
    }

    // For testing purposes
    @Override
    public final boolean isPersistentRepresentationStable()
    {
        return _persistentRepresentationIsStable;
    }

    public boolean isReferenceStreamLink()
    {
        return false;
    }

    @Override
    public synchronized final boolean isRemoving()
    {
        if ((_itemLinkState == ItemLinkState.STATE_REMOVING_EXPIRING)
            || (_itemLinkState == ItemLinkState.STATE_REMOVING_LOCKED)
            || (_itemLinkState == ItemLinkState.STATE_REMOVING_PERSISTENTLY_LOCKED)
            || (_itemLinkState == ItemLinkState.STATE_REMOVING_WITHOUT_LOCK))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    final synchronized boolean isRemovingLocked()
    {
        return ItemLinkState.STATE_REMOVING_LOCKED == _itemLinkState;
    }

    // For testing purposes
    public final boolean isStorageManaged()
    {
        return _isStorageManaged;
    }

    // For testing purposes
    public synchronized final boolean isStrongReferenceToItem()
    {
        return (_strongReferenceToItem != NULL_STRONG_REF);
    }

    public boolean isStoreNever()
    {
        final int storageStratey = _tuple.getStorageStrategy();
        final boolean isStoreNever = (storageStratey == AbstractItem.STORE_NEVER);
        return isStoreNever;
    }

    @Override
    public synchronized final boolean isUpdating()
    {
        return ItemLinkState.STATE_UPDATING_DATA == _itemLinkState;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.cache.ref.Indirection#getNextMemoryCachedLink()
     * Warning: this will be called under the monitor of the owning cache
     */
    @Override
    public final Indirection itemCacheGetNextLink()
    {
        return _itemCacheLinkNext;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.cache.ref.Indirection#getPreviousMemoryCachedLink()
     * Warning: this will be called under the monitor of the owning cache
     */
    @Override
    public final Indirection itemCacheGetPrevioustLink()
    {
        return _itemCacheLinkPrevious;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.cache.ref.Indirection#setNextMemoryCachedLink(com.ibm.ws.sib.msgstore.cache.ref.Indirection)
     * Warning: this will be called under the monitor of the owning cache
     */
    @Override
    public final void itemCacheSetNextLink(Indirection _linkNext)
    {
        this._itemCacheLinkNext = _linkNext;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.cache.ref.Indirection#setPreviousMemoryCachedLink(com.ibm.ws.sib.msgstore.cache.ref.Indirection)
     * Warning: this will be called under the monitor of the owning cache
     */
    @Override
    public final void itemCacheSetPreviousLink(Indirection _linkPrevious)
    {
        this._itemCacheLinkPrevious = _linkPrevious;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.cache.ref.Indirection#itemCacheSetManagedReference(com.ibm.ws.sib.msgstore.AbstractItem)
     * Warning: this will be called under the monitor of the owning cache
     */
    @Override
    public void itemCacheSetManagedReference(AbstractItem item)
    {
        // Defect 601995
        if (item != null)
        {
            _itemCacheManagedReference = item;
        }
        else
        {
            // If our cache reference is being set to null then 
            // use the new named class instead.
            _itemCacheManagedReference = NULL_CACHE_REF;
        }
    }

    /**
     * Called by the stub when it restores an item. This is needed because we
     * have to reinitialize the watermarks when we reload from a warm start.
     * This will be overridden by the ItemStreamLink and ReferenceStreamLink.
     * 
     * Note that the method will be called before the instance variable has been set,
     * so some odd timing behaviour may be apparent if you try to do anything
     * too clever on this call.
     * 
     * @param item
     */
    protected void itemHasBeenRestored(AbstractItem item) {}

    /**
     * Perform late initialization (preCommit). Query the values which the item wishes
     * to change as late as he can, eg. storageStrategy, priority. Also calculate the
     * expiry time and delayed delivery time(the only place in the code where we should do so).
     * 
     * @throws SevereMessageStoreException
     */
    public final void lateInitialize() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "lateInitialize");

        AbstractItem item = getItem();
        final int priority = item.getPriority();
        if (priority > HIGHEST_PRIORITY || priority < LOWEST_PRIORITY)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "lateInitialize");
            throw new SevereMessageStoreException("invalid priority(" + priority + ") supplied");
        }
        getTuple().setPriority(priority);
        getTuple().setCanExpireSilently(item.canExpireSilently());

        long expiryTime = 0;
        long expiryDelay = item.getMaximumTimeInStore();
        if (expiryDelay != AbstractItem.NEVER_EXPIRES)
        {
            long expiryStartTime = item.getExpiryStartTime();
            if (expiryStartTime == 0)
            {
                expiryStartTime = System.currentTimeMillis();
            }
            expiryTime = expiryStartTime + expiryDelay;
        }

        getTuple().setExpiryTime(expiryTime);

        // set the delivery delay time
        long deliveryDelayTime = 0;
        long deliveryDelay = item.getDeliveryDelay();
        if (deliveryDelay != AbstractItem.DEFAULT_DELIVERY_DELAY) {

            /*
             * Imp thing to note here is that i am using getExpiryStartTime().
             * This is because getExpiryStartTime() internally calls getCurrentMEArrivalTimestamp(),
             * which is what is needed here.Delivery delay has nothing to do with expiry time, here we
             * are just reusing the existing functionality
             */
            long deliveryDelayStartTime = item.getExpiryStartTime();
            if (deliveryDelayStartTime == 0) {
                deliveryDelayStartTime = DeliveryDelayManager.timeNow();
            }
            deliveryDelayTime = deliveryDelayStartTime + deliveryDelay;
        }
        getTuple().setDeliveryDelayTime(deliveryDelayTime);

        int newStorageStrategy = item.getStorageStrategy();
        int oldStorageStrategy = getTuple().getStorageStrategy();
        if (oldStorageStrategy != newStorageStrategy)
        {
            // check that the storage strategy is not changing in an unnacceptable way
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "StorageStrategy changing from " + oldStorageStrategy + " to " + newStorageStrategy);
            if (AbstractItem.STORE_NEVER == oldStorageStrategy || AbstractItem.STORE_NEVER == newStorageStrategy)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "lateInitialize");
                throw new SevereMessageStoreException("Invalid change of storage strategy from " + oldStorageStrategy + " to " + newStorageStrategy);
            }
            getTuple().setStorageStrategy(newStorageStrategy);
        }

        if (null != _owningStreamLink)
        {
            // RootMembership (link) does not have an owning streamLink.  But it
            // does not need a sequence either.....
            int streamStorageStrategy = _owningStreamLink.getTuple().getStorageStrategy();
            if (newStorageStrategy > streamStorageStrategy)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "lateInitialize");
                throw new InvalidAddOperation("STREAM_STORAGE_MISMATCH_SIMS0500", new Object[] { Integer.valueOf(newStorageStrategy), Integer.valueOf(streamStorageStrategy) });
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "lateInitialize");
    }

    public final void lock(long lockID) throws SevereMessageStoreException
    {
        cmdLock(lockID);
    }

    /*
     * Used by LockingCursors
     * 
     * @param filter
     * 
     * @param lockID
     * 
     * @throws MessageStoreException
     */
    public final boolean lockIfMatches(final Filter filter, long lockID) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "lockIfMatches", new Object[] { filter, Long.valueOf(lockID) });

        boolean locked = false;

        boolean matches = false;
        AbstractItem item = getItem();
        if (null == item)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "item does not exist");
        }
        else
        {
            try
            {
                if (null == filter || filter.filterMatches(item))
                {
                    matches = true;
                }
            } catch (Exception e)
            {
                //No FFDC Code Needed.
                // assume exception is false return
            }
        }

        boolean hasBecomeNonReleasable = false;
        if (matches)
        {
            synchronized (this)
            {
                if (isAvailable() && !isExpired())
                {
                    if (ItemLinkState.STATE_AVAILABLE == _itemLinkState)
                    {
                        ListStatistics stats = getParentStatistics();
                        synchronized (stats)
                        {
                            stats.incrementLocked();
                            stats.decrementAvailable();
                        }
                    }
                    else
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                            SibTr.exit(this, tc, "lockIfMatches");
                        throw new StateException(_itemLinkState.toString());
                    }

                    _lockID = lockID;
                    hasBecomeNonReleasable = _declareNotDiscardable(item);
                    _itemLinkState = ItemLinkState.STATE_LOCKED;
                    locked = true;
                }
            }
            if (hasBecomeNonReleasable)
            {
                _declareNotReleasable();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "lockIfMatches", Boolean.valueOf(locked));
        return locked;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.Membership#lockIfAvailable(long)
     */
    @Override
    public final boolean lockItemIfAvailable(long lockID) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "lockItemIfAvailable", Long.valueOf(lockID));

        boolean locked = false;
        if (AbstractItem.NO_LOCK_ID == lockID)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                SibTr.exit(this, tc, "lockItemIfAvailable");
            throw new SevereMessageStoreException("invalid lockID - clashes with internal constant NO_LOCK_ID");
        }
        boolean hasBecomeNonReleasable = false;
        synchronized (this)
        {
            if (!isExpired() && isAvailable())
            {
                if (ItemLinkState.STATE_AVAILABLE == _itemLinkState)
                {
                    ListStatistics stats = getParentStatistics();
                    synchronized (stats)
                    {
                        stats.incrementLocked();
                        stats.decrementAvailable();
                    }
                }
                else
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(this, tc, "lockItemIfAvailable");
                    throw new StateException(_itemLinkState.toString());
                }
                locked = true;
                _lockID = lockID;
                hasBecomeNonReleasable = _declareNotDiscardable(_getItemNoRestore());
                _itemLinkState = ItemLinkState.STATE_LOCKED;
            }
        }
        if (hasBecomeNonReleasable)
        {
            _declareNotReleasable();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "lockItemIfAvailable", Boolean.valueOf(locked));
        return locked;
    }

    public final AbstractItem matches(final Filter filter) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "matches", filter);

        AbstractItem foundItem = null;

        /*
         * <p>
         * A new filter DeliveryDelayDeleteFilter is explicitly created
         * to fetch the items which are available or
         * are locked for delivery delay and off course which
         * are not expired
         * <p>
         * Prior to delivery delay feature when the destination is deleted,
         * the AsyncDeletionThread internally passes null as the filter and
         * used to delete only the available messages which are not expired.
         * But with the introduction of delivery delay feature items
         * locked for delivery delay that also must be deleted
         * or reallocated to exception destination.
         * <p>
         * Now when destination is deleted, if the item is either locked
         * for delivery delay or if its in available state and not expired
         * the item is eligible for deletion or reallocation to exception
         * destination
         */
        if ((getLockID() == Item.DELIVERY_DELAY_LOCK_ID && isStateLocked())
            || ((isAvailable()) && !isExpired())) {

            AbstractItem item = getItem();
            if (null == item)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "item does not exist");
            }
            else
            {
                try
                {
                    // DeliveryDelayDeleteFilter will always return true
                    if (null == filter || filter.filterMatches(item))
                    {
                        foundItem = item;
                    }
                } catch (Exception e)
                {
                    //No FFDC Code Needed.
                    // assume exception is false return
                }
            }

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "matches", foundItem);
        return foundItem;
    }

    /**
     * Version used by browse cursor as it can be set to allow matching
     * unavailable items to be seen.
     * 
     * @param filter
     * @param allowUnavailable
     * @throws SevereMessageStoreException
     */
    public final AbstractItem matches(final Filter filter, boolean allowUnavailable) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "matches", new Object[] { filter, Boolean.valueOf(allowUnavailable) });

        AbstractItem foundItem = null;
        if (allowUnavailable || isAvailable())
        {
            if (!isExpired())
            {
                AbstractItem item = getItem();
                if (null == item)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "item does not exist");
                }
                else
                {
                    try
                    {
                        if (null == filter || filter.filterMatches(item))
                        {
                            foundItem = item;
                        }
                    } catch (Exception e)
                    {
                        //No FFDC Code Needed.
                        // assume exception is false return
                    }
                }
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "matches", foundItem);
        return foundItem;
    }

    /**
     * Declare the receiver stable
     * This is used to drive the softening/disposal of the
     * indirect reference to the receivers item.
     * The item can be discarded if the link is stable and
     * available (always assuming the storage strategy allows).
     * If the item becomes unstable or unavailable the link
     * should not allow the item to be discarded.
     * 
     * @throws SevereMessageStoreException
     */
    public final void persistentRepresentationIsStable() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "persistentRepresentationIsStable");

        boolean notifyRequired = false;
        AbstractItem item = null;
        synchronized (this)
        {
            if (!_persistentRepresentationIsStable)
            {
                // if we have become stable and were releasable, then
                // notify cache.
                notifyRequired = _itemIsDiscardableIfPersistentRepresentationStable;
                // 274012
                if (notifyRequired)
                {
                    item = _getItemNoRestore();

                    // Defect 601995
                    _strongReferenceToItem = NULL_STRONG_REF;
                }
                _persistentRepresentationIsStable = true;
            }
        }
        if (notifyRequired)
        {
            _declareReleasable(item);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "persistentRepresentationIsStable");
    }

    /**
     * Declare the receiver stable
     * This is used to drive the softening/disposal of the
     * indirect reference tothe receivers item.
     * The item can be discarded if the link is stable and
     * available (always assuming the storage strategy allows).
     * If the item becomes unstable or unavailable the link
     * should not allow the item to be discarded.
     * 
     * @throws SevereMessageStoreException
     */
    public final void persistentRepresentationIsUnstable() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "persistentRepresentationIsUnstable");

        boolean notifyRequired = false;
        synchronized (this)
        {
            // we are going to need the item in memory, so we make sure that
            // we pin it with a hard reference
            if (_strongReferenceToItem == NULL_STRONG_REF && null != _softReferenceToItem)
            {
                // Someone else may still have a reference to the item,
                // so we can save some trouble by recovering the item
                // from the weak reference. If the weak reference has been
                // cleared then the item will still be null, and the calling
                // code will just need to cope with it.
                _strongReferenceToItem = (AbstractItem) _softReferenceToItem.get();

                // Defect 601995
                // Set the reference to the named class if still null
                // after the call to the soft reference.
                if (_strongReferenceToItem == null)
                {
                    _strongReferenceToItem = NULL_STRONG_REF;
                }
            }

            if (_persistentRepresentationIsStable)
            {
                // if we have become unstable, and we were releasable
                // then we need to notify cache
                notifyRequired = _itemIsDiscardableIfPersistentRepresentationStable;
                _persistentRepresentationIsStable = false;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "stable=false available=" + _itemIsDiscardableIfPersistentRepresentationStable);
        }

        if (notifyRequired)
        {
            _declareNotReleasable();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "persistentRepresentationIsUnstable");
    }

    /**
     * Use this to lock persistently. Invoke indirectly by the API.
     * 
     * @param transaction
     * 
     * @exception ProtocolException
     *                Thrown if an add is attempted when the
     *                transaction cannot allow any further work to be added i.e. after
     *                completion of the transaction.
     * @exception TransactionException
     *                Thrown if an unexpected error occurs.
     * @throws SevereMessageStoreException
     */
    @Override
    public final void persistLock(final Transaction transaction) throws ProtocolException, TransactionException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "persistLock", transaction);

        if (null != transaction)
        {
            PersistentTransaction mstran = (PersistentTransaction) transaction;
            cmdPersistLock(mstran);
            getTuple().setLockID(getLockID());
            final Task task = new PersistLock(this);
            mstran.addWork(task);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "persistLock");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.cache.xalist.Task#postAbort(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void postAbortAdd(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "postAbortAdd", transaction);

        releaseItem();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "postAbortAdd");
    }

    public final void postCommitRemove(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "postCommitRemove", transaction);

        releaseItem();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "postCommitRemove");
    }

    public final void preCommitAdd(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "preCommitAdd", transaction);

        synchronized (this)
        {
            if ((_itemLinkState == ItemLinkState.STATE_ADDING_LOCKED)
                || (_itemLinkState == ItemLinkState.STATE_ADDING_UNLOCKED))
            {
                getItem().eventPrecommitAdd(transaction);
                _assertCorrectTransaction(transaction);
                _owningStreamLink._assertCanAddChildUnder(transaction);
                lateInitialize(); // 183455
                initializeSequenceNumber();
                _owningStreamLink.append(this);
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "preCommitAdd");
                throw new StateException(_itemLinkState.toString());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "preCommitAdd");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.task.TaskTarget#preCommitRemove(com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    public final void preCommitRemove(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "preCommitRemove", transaction);

        synchronized (this)
        {
            SevereMessageStoreException ex = assertCanDelete(transaction);
            if (null != ex)
            {
                throw ex;
            }

            if ((_itemLinkState == ItemLinkState.STATE_REMOVING_EXPIRING)
                || (_itemLinkState == ItemLinkState.STATE_REMOVING_LOCKED)
                || (_itemLinkState == ItemLinkState.STATE_REMOVING_PERSISTENTLY_LOCKED)
                || (_itemLinkState == ItemLinkState.STATE_REMOVING_WITHOUT_LOCK))
            {
                _assertCorrectTransaction(transaction);
                _owningStreamLink._assertCanRemoveChildUnder(transaction);
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "preCommitRemove");
                throw new StateException(_itemLinkState.toString());
            }

            getItem().eventPrecommitRemove(transaction);
        } // end sync

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "preCommitRemove");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.task.TaskTarget#preCommitUpdate(com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    public final void preCommitUpdate(PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "preCommitUpdate", transaction);

        getItem().eventPrecommitUpdate(transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "preCommitUpdate");
    }

    // Feature SIB0112b.ms.1
    // Feature SIB0112le.ms.1
    @Override
    public final List<DataSlice> readDataFromPersistence() throws SevereMessageStoreException
    {
        List<DataSlice> dataSlices;
        // lazy restoration only needed if the item was stored.  There is
        // no point trying to restore if there is no data.
        PersistentMessageStore pm = getMessageStoreImpl().getPersistentMessageStore();
        try
        {
            dataSlices = pm.readDataOnly(getTuple());
        } catch (PersistenceException pe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink.readDataFromPersistence", "1:3271:1.241", this);
            throw new SevereMessageStoreException(pe);
        }

        return dataSlices;
    }

    public final void registerWithExpirer() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "registerWithExpirer");

        // Add this item link to expiry index. 175362 176228
        MessageStoreImpl msi = getMessageStoreImpl();
        Expirer expirer = msi._getExpirer();
        long expiryTime = getTuple().getExpiryTime();
        if (null != expirer && 0 != expiryTime)
        {
            expirer.addExpirable(this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "registerWithExpirer");
    }

    public final void registerWithDeliveryDelayManager() throws SevereMessageStoreException
    {

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "registerWithDeliveryDelayManager");

        MessageStoreImpl msi = getMessageStoreImpl();
        DeliveryDelayManager deliveryDelayManager = msi._getDeliveryDelayManager();
        long deliveryDelayTime = getTuple().getDeliveryDelayTime();
        if (null != deliveryDelayManager && AbstractItem.DEFAULT_DELIVERY_DELAY != deliveryDelayTime)
        {
            deliveryDelayManager.addDeliveryDelayable(this);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "registerWithDeliveryDelayManager");
    }

    /**
     * Releases all references to the item as a result of the final removal of the item from the MS
     * 
     * @throws SevereMessageStoreException
     */
    public final void releaseItem() throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(tc, "releaseItem");

        synchronized (this)
        {
            _strongReferenceToItem = NULL_STRONG_REF;
        }

        if (_softReferenceToItem != null)
        {
            _softReferenceToItem.clear();
        }

        // Defect 601995
        if (_isStorageManaged && (_itemCacheManagedReference != NULL_CACHE_REF))
        {
            _itemCache.unmanage(this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            {
                SibTr.debug(this, tc, "release item(" + getID() + ":" + _inMemoryItemSize + ") new cacheSize = " + _itemCache.getCurrentSize());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(tc, "releaseItem");
    }

    /**
     * This is the callback from the item cache to indicate that the
     * discardable item should be freed. It's only used to remove STORE_NEVER items
     * from the MS when the cache of these items is full.
     * 
     * @throws SevereMessageStoreException
     */
    @Override
    public final void releaseIfDiscardable() throws SevereMessageStoreException
    {
        boolean unlink = false; // flag so we can unlink outside sync block
        if (isStoreNever())
        {
            synchronized (this)
            {
                if (ItemLinkState.STATE_AVAILABLE == _itemLinkState)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "removing STORE_NEVER item as a result of discard");

                    ListStatistics stats = getParentStatistics();
                    synchronized (stats)
                    {
                        stats.decrementAvailable();
                        stats.decrementTotal(_inMemoryItemSize);
                    }

                    _itemLinkState = ItemLinkState.STATE_NOT_STORED;
                    unlink = true;
                }
            }
        }

        if (unlink)
        {
            getMessageStoreImpl().unregister(this);
            unlink();
        }
    }

    /**
     * remove the item if it matches and is available
     * 
     * @param filter
     * @param transaction
     * @return item if locked
     * 
     * @throws ProtocolException Thrown if an add is attempted when the
     *             transaction cannot allow any further work to be added i.e. after
     *             completion of the transaction.
     * 
     * @throws TransactionException Thrown if an unexpected error occurs.
     * @throws SevereMessageStoreException
     */
    public final AbstractItem removeIfMatches(final Filter filter, PersistentTransaction transaction) throws ProtocolException, TransactionException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "removeIfMatches", new Object[] { filter, transaction });

        AbstractItem foundItem = cmdRemoveIfMatches(filter, transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "removeIfMatches", foundItem);
        return foundItem;
    }

    /**
     * @param transaction
     * 
     * @throws ProtocolException Thrown if an add is attempted when the
     *             transaction cannot allow any further work to be added i.e. after
     *             completion of the transaction.
     * 
     * @throws TransactionException Thrown if an unexpected error occurs.
     * @throws SevereMessageStoreException
     * @see com.ibm.ws.sib.msgstore.Membership#requestUpdate(com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    @Override
    public final void requestUpdate(final Transaction transaction) throws ProtocolException, TransactionException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "requestUpdate", transaction);

        PersistentTransaction mstran = (PersistentTransaction) transaction;
        cmdRequestUpdate(mstran);
        Task task = new UpdateTask(this);
        mstran.addWork(task);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "requestUpdate");
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.sib.msgstore.Membership#requestXmlWriteOn(com.ibm.ws.sib.msgstore.FormattedWriter)
     */
    @Override
    public final void requestXmlWriteOn(FormattedWriter writer) throws IOException
    {
        xmlWriteOn(writer);
    }

    /**
     * Restores the state of the link from the persistable
     * 
     * @param tuple
     * @throws SevereMessageStoreException
     */
    final void restoreState(final Persistable tuple) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "restoreState", tuple);

        PersistentTranId xid = tuple.getPersistentTranId();
        boolean logicallyDeleted = tuple.isLogicallyDeleted();
        PersistentTransaction transaction = null;

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            SibTr.debug(this, tc, "xid = " + xid + "; logicallyDeleted = " + logicallyDeleted);

        if (xid != null)
        {
            transaction = getMessageStoreImpl().getXidManager().getTransactionFromTranId(xid);
        }
        if (null == transaction)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "no transaction so tuple is not in doubt ");

            // We get here if transaction is null - either XID is null (a rolled
            // back transaction has reset the row to its original state) or XID
            // is not null, but no transaction was found (a completed transaction)
            if (logicallyDeleted)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "tuple is logically deleted - removing link");
            }
            else
            {
                boolean canExpireSilently = false /* tuple.canExpireSilently() */;
                boolean expired = false /* tuple.isExpired() */;
                if (expired && canExpireSilently)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "tuple is expired and can expire silently - removing link");
                }
                else
                {
                    // The itemLink is AVAILABLE or LOCKED
                    long lockID = tuple.getLockID();
                    if (AbstractItem.NO_LOCK_ID != lockID)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "tuple is locked. LockID = " + lockID);

                        _restoreStatePersistentlyLocked(this, _owningStreamLink, lockID);
                        _owningStreamLink.append(this);
                    }
                    else
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                            SibTr.debug(this, tc, "tuple is not locked");

                        /*
                         * If the deliverydelaytime is greater than the current time during reconstitution then
                         * the item has to be added as locked.And it must be locked using DELIVERY_DELAY_LOCK_ID
                         * lock ID
                         * In the other case we just add the item as available so that it is available for
                         * consumption
                         */
                        if (tuple.getDeliveryDelayTime() > System.currentTimeMillis()) {
                            _restoreStateLocked(this, _owningStreamLink, Item.DELIVERY_DELAY_LOCK_ID);
                            // Register with the DeliveryDelayManager
                            registerWithDeliveryDelayManager();
                        }
                        else {
                            _restoreStateAvailable(this, _owningStreamLink);
                        }
                        _owningStreamLink.append(this);
                    }
                    //Only register with expirer when we are in available or locked state. The other
                    // states will register when either the commit call comes in, or don't require
                    // expiry as the item is already deleted.
                    registerWithExpirer();

                }
            }
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                SibTr.debug(this, tc, "Tuple is in doubt. transaction = " + transaction);

            Task task = null;
            if (logicallyDeleted)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "tuple is logically deleted - tuple is deleting");

                //Instantiate the itemLink as REMOVING
                long lockID = tuple.getLockID();
                if (AbstractItem.NO_LOCK_ID != lockID)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "tuple is 'removingLocked'. lockID = " + lockID);

                    _restoreStateRemovingPersistentlyLocked(this, _owningStreamLink, getLockID(), transaction);
                    _owningStreamLink.append(this);
                    task = new RemoveLockedTask(this);
                    //D184032
                }
                else
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                        SibTr.debug(this, tc, "tuple is not locked");

                    _restoreStateRemovingNoLock(this, _owningStreamLink, transaction);
                    _owningStreamLink.append(this);
                    task = new RemoveTask(this);
                    //D184032
                }
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "tuple is not logicallyDeleted, so is adding");

                /*
                 * If the deliverydelaytime is greater than the current time during reconstitution then
                 * the item has to be added as locked.And it must be locked using DELIVERY_DELAY_LOCK_ID
                 * lock ID
                 * In the other case we just add the item as available so that it is available for
                 * consumption
                 */
                if (tuple.getDeliveryDelayTime() > System.currentTimeMillis()) {
                    _restoreStateLocked(this, _owningStreamLink, Item.DELIVERY_DELAY_LOCK_ID);
                    //Not adding to deliverydelay manager here because it will be done in AddTask.commitExternal
                } else {
                    //Instantiate the itemLink as ADDING
                    _restoreStateAdding(this, _owningStreamLink, transaction);
                }
                _owningStreamLink.append(this);
                task = new AddTask(this);
                //D184032
            }
            try
            {
                transaction.addWork(task);
                ((TaskList) transaction.getWorkList()).declareAlreadyPrecommitted();
            } catch (MessageStoreException e)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink.restoreState", "1:3526:1.241", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                    SibTr.debug(this, tc, "Exception " + e + " while adding task to tasklist");
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "restoreState");
                throw new SevereMessageStoreException("SIMS_NNNN", e);
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "restoreState");
    }

    /**
     * @param link next pointer in the itemLinkMap or null if none
     */
    public final void setNextMappedLink(AbstractItemLink link)
    {
        _nextMappedLink = link;
    }

    // Defect 463642
    // Revert to using spill limits previously removed in SIB0112d.ms.2
    /**
     * @param b
     */
    public final void setParentWasSpillingAtAddTime(boolean b)
    {
        getTuple().setWasSpillingAtAddition(b);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("AIL(");
        buf.append(getID());
        buf.append(", ");
        buf.append(_itemLinkState);
        buf.append(")");
        return buf.toString();
    }

    /**
     * If transaction is null this is a non-persistent lock
     * 
     * @param lockID
     * @param transaction
     * @param incrementUnlockCountIfNonpersistent
     * 
     * @throws ProtocolException Thrown if an add is attempted when the
     *             transaction cannot allow any further work to be added i.e. after
     *             completion of the transaction.
     * 
     * @throws TransactionException Thrown if an unexpected error occurs.
     * @throws SevereMessageStoreException
     * @see com.ibm.ws.sib.msgstore.Membership#unlock(long, com.ibm.ws.sib.msgstore.transactions.Transaction)
     */
    @Override
    public final void unlock(final long lockID, final Transaction transaction, final boolean incrementUnlockCountIfNonpersistent) throws ProtocolException, TransactionException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.entry(this, tc, "unlock", new Object[] { Long.valueOf(lockID), transaction, Boolean.valueOf(incrementUnlockCountIfNonpersistent) });

        boolean invokeUnlockedCallback = false;
        boolean linkHasBecomeAvailable = false;
        boolean linkHasBecomeReleasable = false;
        boolean linkHasBecomeNotReleasable = false;
        boolean requiresPersistUnlockTask = false;
        AbstractItem item = null;
        synchronized (this)
        {
            if (_itemLinkState == ItemLinkState.STATE_LOCKED)
            {
                if (_lockID != lockID)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        SibTr.event(this, tc, "LockID mismatch! LockID(Item): " + _lockID + ", LockID(New): " + lockID);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(this, tc, "unlock");
                    throw new LockIdMismatch(_lockID, lockID);
                }

                if (null != transaction)
                {
                    _transactionId = transaction.getPersistentTranId();
                }

                if (_lockID == Item.DELIVERY_DELAY_LOCK_ID) { // For Delivery delay messages

                    // Fetch the item from persistance if not available
                    // This is needed because we need to call item.eventUnlocked()
                    // so that if there is any ready consumer we can deliver the 
                    // message immediately
                    item = getItem();
                    ListStatistics stats = getParentStatistics();
                    synchronized (stats)
                    {
                        stats.decrementLocked();
                        stats.incrementAvailable();
                    }

                    // we can make the item discardable 
                    // since we have fetched the item
                    linkHasBecomeReleasable = _declareDiscardable();
                    _lockID = NO_LOCK_ID;
                    _tuple.setLockID(AbstractItem.NO_LOCK_ID);
                    if (incrementUnlockCountIfNonpersistent)
                    {
                        _unlockCount++;
                    }
                    _itemLinkState = ItemLinkState.STATE_AVAILABLE;
                    invokeUnlockedCallback = true;
                    linkHasBecomeAvailable = true;
                } else
                {
                    item = _getAndAssertItem();

                    ListStatistics stats = getParentStatistics();
                    synchronized (stats)
                    {
                        stats.decrementLocked();
                        stats.incrementAvailable();
                    }

                    linkHasBecomeReleasable = _declareDiscardable();
                    _lockID = NO_LOCK_ID;
                    _tuple.setLockID(AbstractItem.NO_LOCK_ID);
                    if (incrementUnlockCountIfNonpersistent)
                    {
                        _unlockCount++;
                    }
                    _itemLinkState = ItemLinkState.STATE_AVAILABLE;
                    invokeUnlockedCallback = true;
                    linkHasBecomeAvailable = true;
                }
            }
            else if (_itemLinkState == ItemLinkState.STATE_PERSISTENTLY_LOCKED)
            {
                if (_lockID != lockID)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        SibTr.event(this, tc, "LockID mismatch! LockID(Item): " + _lockID + ", LockID(New): " + lockID);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(this, tc, "unlock");
                    throw new LockIdMismatch(_lockID, lockID);
                }

                if (null == transaction)
                {
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                        SibTr.event(this, tc, "No transaction supplied for persistent unlock!");
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                        SibTr.exit(this, tc, "unlock");
                    throw new StateException("Unlock of Persistently locked requires transaction");
                }

                _transactionId = transaction.getPersistentTranId();
                linkHasBecomeNotReleasable = _declareNotDiscardable(_getItemNoRestore());
                _tuple.setLockID(AbstractItem.NO_LOCK_ID);
                _itemLinkState = ItemLinkState.STATE_UNLOCKING_PERSISTENTLY_LOCKED;
                requiresPersistUnlockTask = true;
            }
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    SibTr.event(this, tc, "Invalid Item state: " + _itemLinkState);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
                    SibTr.exit(this, tc, "unlock");
                throw new StateException(_itemLinkState.toString());
            }
        }
        // this needs to be outside sync block
        if (linkHasBecomeReleasable)
        {
            _declareReleasable(_getItemNoRestore());
        }
        if (linkHasBecomeNotReleasable)
        {
            _declareNotReleasable();
        }
        if (requiresPersistUnlockTask)
        {
            Task task = new PersistUnlock(this);
            ((PersistentTransaction) transaction).addWork(task);
        }
        if (linkHasBecomeAvailable && isAvailable())
        {
            if (null != _owningStreamLink)
            {
                _owningStreamLink.linkAvailable(this);
            }
        }
        // this needs to be outside sync block
        if (invokeUnlockedCallback)
        {
            if (item != null)
            {
                item.eventUnlocked();
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            SibTr.exit(this, tc, "unlock");
    }

    @Override
    public void xmlShortWriteOn(FormattedWriter writer) throws IOException
    {
        String name = xmlTagName();
        writer.write('<');
        writer.write(writer.getNameSpace()); // Defect 358424
        writer.write(name);
        xmlWriteAttributesOn(writer);
        writer.write(" />");
    }

    abstract protected String xmlTagName();

    @Override
    protected synchronized void xmlWriteAttributesOn(FormattedWriter writer) throws IOException
    {
        writer.write(' ');
        writer.write(XML_ID);
        writer.write("=\"");
        writer.write(Long.toString(getTuple().getUniqueId()));
        writer.write("\" ");
        writer.write(XML_STATE);
        writer.write("=\"");
        writer.write(_itemLinkState.toString());
        writer.write("\" ");
        writer.write(XML_SIZE);
        writer.write("=\"");
        writer.write(Integer.toString(_inMemoryItemSize));
        writer.write("\" ");
        writer.write(XML_BACKOUT_COUNT);
        writer.write("=\"");
        writer.write(Integer.toString(_backoutCount));
        writer.write("\" ");
        writer.write(XML_UNLOCK_COUNT);
        writer.write("=\"");
        writer.write(Integer.toString(_unlockCount));
        writer.write('"');
        super.xmlWriteAttributesOn(writer);
    }

    protected void xmlWriteChildrenOn(FormattedWriter writer) throws IOException
    {
        AbstractItem item = _getItemNoRestore();
        if (null != item)
        {
            item.xmlWriteOn(writer);
        }
    }

    @Override
    public void xmlWriteOn(FormattedWriter writer) throws IOException
    {
        String name = xmlTagName();
        writer.write("<");
        writer.write(writer.getNameSpace()); // Defect 358424
        writer.write(name);
        xmlWriteAttributesOn(writer);
        writer.write(" >");
        writer.indent();
        if (null != getTuple())
        {
            getTuple().xmlWrite(writer);
        }
        xmlWriteChildrenOn(writer);
        writer.outdent();
        writer.newLine();
        writer.endTag(name);
    }
}
