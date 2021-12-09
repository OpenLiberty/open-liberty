package com.ibm.ws.sib.msgstore.task;
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

import java.util.ArrayList;
import java.util.List;

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.NotInMessageStore;
import com.ibm.ws.sib.msgstore.PersistentDataEncodingException;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.msgstore.persistence.BatchingContext;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.persistence.impl.CachedPersistableImpl;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionState;
import com.ibm.ws.sib.utils.DataSlice;

import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;

public final class AddTask extends Task
{
    private final class CachedPersistable extends CachedPersistableImpl
    {
        private long _cachedLockId;
        private List<DataSlice> _cachedMemberData;
        private int _cachedInMemorySize;
        private boolean _isMemberDataCached;

        // Feature SIB0112b.ms.1
        public CachedPersistable(Persistable primaryPersistable, boolean copyMemberData) throws PersistentDataEncodingException, SevereMessageStoreException
        {
            super(primaryPersistable);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>$CachedPersistable");

            // The member data is copied conditionally depending on the item's storage
            // hints evaluated in the call to the constructor for this object
            if (copyMemberData)
            {
                List<DataSlice> memberData = primaryPersistable.getData();
                if ((null == memberData) || _isPersistentDataImmutable)
                {
                    _cachedMemberData = memberData;
                }
                else
                {
                    // Use the copy constructor of ArrayList to
                    // take a copy of the list.
                    _cachedMemberData = new ArrayList<DataSlice>(memberData);
                }

            }

            // The inMemorySize is cached, as it could change due to flattening/encoding of the
            // member data. We want the value returned here to be constant, otherwise, we're at risk
            // of another class getting its sums wrong. This length is an approximation of the size
            // of the member data in memory.
            _cachedInMemorySize = primaryPersistable.getInMemoryByteSize();

            _isMemberDataCached = copyMemberData;

            // The lockid is always copied since the storage hints only apply to the
            // member data. Even if we don't copy the member data at the time that the
            // AddTask is created, we must copy the lock id since the MS user can
            // immediately lock the item and we don't want to risk a timing oddity
            _cachedLockId = primaryPersistable.getLockID();

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>$CachedPersistable");
        }

        public List<DataSlice> getData() throws PersistentDataEncodingException, SevereMessageStoreException
        {
            if (!_isMemberDataCached)
            {
                return _primaryPersistable.getData();
            }
            return _cachedMemberData;
        }

        public int getInMemoryByteSize()
        {
            return _cachedInMemorySize;
        }

        public long getLockID()
        {
            return _cachedLockId;
        }

        public void setLockID(long l)
        {
            //not settable
            throw new UnsupportedOperationException();
        }

        // Feature SIB0112b.ms.1
        public void cacheMemberData() throws PersistentDataEncodingException, SevereMessageStoreException
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "cacheMemberData");

            if (!_isMemberDataCached)
            {
                List<DataSlice> memberData = _primaryPersistable.getData();

                if ((null == memberData) || _isPersistentDataImmutable)
                {
                    _cachedMemberData = memberData;
                }
                else
                {
                    // Use the copy constructor of ArrayList to
                    // take a copy of the list.
                    _cachedMemberData = new ArrayList<DataSlice>(memberData);
                }
                _isMemberDataCached = true;
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "cacheMemberData");
        }
    }

    private static TraceNLS nls = TraceNLS.getTraceNLS(MessageStoreConstants.MSG_BUNDLE);
    private static TraceComponent tc = SibTr.register(AddTask.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    private CachedPersistable _cachedPersistable = null;
    private Persistable _primaryPersistable;
    private final boolean _isPersistentDataImmutable;
    private final boolean _isPersistentDataNeverUpdated;

    public AddTask(AbstractItemLink link) throws SevereMessageStoreException
    {
        super(link);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", link);

        // 258179 - moved called to Task superclass - getItem();
        _primaryPersistable = super.getPersistable();
        _isPersistentDataImmutable = getItem().isPersistentDataImmutable();
        _isPersistentDataNeverUpdated = getItem().isPersistentDataNeverUpdated();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>", this);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.xalist.Task#abort(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void abort(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "abort", transaction);

        getLink().abortAdd(transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "abort");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.xalist.Task#commitStage2(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void commitExternal(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "commitExternal", transaction);

        // I did not expect that - the xaCommit for all Add task
        // must be called *after* all internalCommitAdd() calls
        // on the same transaction.  So we move the xaCommit call
        // to the 'external' commit phase.
        getLink().commitAdd(transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commitExternal");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.xalist.Task#commitStage1(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void commitInternal(final PersistentTransaction transaction)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "commitInternal", transaction);

        getLink().internalCommitAdd();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commitInternal");
    }

    /*
     *  (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.Operation#copyDataIfVulnerable()
     */
    public final void copyDataIfVulnerable() throws PersistentDataEncodingException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "copyDataIfVulnerable");

        // This is a conditional caching or copy of the data, only taken if the data might
        // be updated by the MS user or if the item cannot guarantee that the
        // item's data can be used by the MS at any time without needing a separate copy
        if (_cachedPersistable == null)
        {
            _cachedPersistable = new CachedPersistable(_primaryPersistable,
                                                       !(_isPersistentDataImmutable &&
                                                         _isPersistentDataNeverUpdated));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "copyDataIfVulnerable");
    }

    /*
     *  (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.Operation#ensureDataAvailable()
     */
    public void ensureDataAvailable() throws PersistentDataEncodingException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "ensureDataAvailable");

        // Check whether the item is still in the store
        AbstractItem item = getItem();
        if ((item == null) || !item.isInStore())
        {
            throw new NotInMessageStore();
        }

        // This is an unconditional caching or copy of the data, compared with a conditional
        // copy of the data which is taken by copyDataIfVulnerable
        if (_cachedPersistable == null)
        {
            _cachedPersistable = new CachedPersistable(_primaryPersistable, true);
        }
        else
        {
            _cachedPersistable.cacheMemberData();
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "ensureDataAvailable");
    }

    /*
     *  (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.Operation#getPersistable()
     */
    public final Persistable getPersistable()
    {
        if (_cachedPersistable != null)
        {
            return _cachedPersistable;
        }
        else
        {
            return _primaryPersistable;
        }
    }

    /*
     *  (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.task.Task#getTaskType()
     */
    public Task.Type getTaskType()
    {
        return Type.ADD;
    }

    /*
     *  (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.Operation#isCreateOfPersistentRepresentation()
     */
    public final boolean isCreateOfPersistentRepresentation()
    {
        return true;
    }

    /*
     *  (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.Operation#getPersistableInMemorySizeApproximation(int)
     */
    public final int getPersistableInMemorySizeApproximation(TransactionState tranState)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getPersistableInMemorySizeApproximation", tranState);

        int size;

        if ((tranState == TransactionState.STATE_COMMITTED)
            || (tranState == TransactionState.STATE_COMMITTING_1PC)
            || (tranState == TransactionState.STATE_PREPARING)
            || (tranState == TransactionState.STATE_PREPARED))
        {
            size = DEFAULT_TASK_PERSISTABLE_SIZE_APPROXIMATION + getPersistable().getInMemoryByteSize();
        }
        else if ((tranState == TransactionState.STATE_ROLLEDBACK)
                 || (tranState == TransactionState.STATE_ROLLINGBACK))
        {
            size = DEFAULT_TASK_PERSISTABLE_SIZE_APPROXIMATION;
        }
        else if (tranState == TransactionState.STATE_COMMITTING_2PC)
        {
            size = 0;
        }
        else
        {
            throw new IllegalStateException(nls.getFormattedMessage("INVALID_TASK_OPERATION_SIMS1520", new Object[] {tranState}, null));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getPersistableInMemorySizeApproximation", Integer.valueOf(size));
        return size;
    }

    /*
     *  (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.Operation#persist(com.ibm.ws.sib.msgstore.persistence.BatchingContext, int)
     */
    public final void persist(BatchingContext batchingContext, TransactionState tranState)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "persist", new Object[] { batchingContext, tranState});

        if ((tranState == TransactionState.STATE_COMMITTED)
            || (tranState == TransactionState.STATE_COMMITTING_1PC)
            || (tranState == TransactionState.STATE_PREPARING)
            || (tranState == TransactionState.STATE_PREPARED))
        {
            batchingContext.insert(getPersistable());
        }
        else if ((tranState == TransactionState.STATE_ROLLEDBACK)
                 || (tranState == TransactionState.STATE_ROLLINGBACK))
        {
            batchingContext.delete(getPersistable());
        }
        else if (tranState == TransactionState.STATE_COMMITTING_2PC)
        {
            //Do nothing.
        }
        else
        {
            throw new IllegalStateException(nls.getFormattedMessage("INVALID_TASK_OPERATION_SIMS1520", new Object[] {tranState}, null));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "persist");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.xalist.Task#postAbort(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void postAbort(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "postAbort", transaction);

        AbstractItem item = getItem();
        if (null == item)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "null item");
        }
        else
        {
            item.eventPostRollbackAdd(transaction);
        }
        getLink().postAbortAdd(transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "postAbort");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.xalist.Task#postCommit(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void postCommit(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "postCommit", transaction);

        AbstractItem item = getItem();
        if (null == item)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "null item");
        }
        else
        {
            item.eventPostCommitAdd(transaction);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "postCommit");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.xalist.Task#preCommit(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void preCommit(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "preCommit", transaction);

        getLink().preCommitAdd(transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "preCommit");
    }
}
