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

public final class UpdateTask extends Task
{

    // Feature SIB0112b.ms.1
    private static final class CachedPersistable extends CachedPersistableImpl
    {
        private List<DataSlice> _cachedMemberData;
        private int _cachedInMemorySize;

        public CachedPersistable(Persistable primaryPersistable) throws PersistentDataEncodingException, SevereMessageStoreException
        {
            super(primaryPersistable);

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>$CachedPersistable");

            List<DataSlice> memberData = primaryPersistable.getData();

            // Use the copy constructor of ArrayList to
            // take a copy of the list.
            _cachedMemberData = new ArrayList<DataSlice>(memberData);

            // The inMemorySize is cached, as it could change due to flattening/encoding of the
            // member data. We want the value returned here to be constant, otherwise, we're at risk
            // of another class getting its sums wrong. This length is an approximation of the size
            // of the member data in memory.
            _cachedInMemorySize = primaryPersistable.getInMemoryByteSize();

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>$CachedPersistable");
        }

        public List<DataSlice> getData()
        {
            return _cachedMemberData;
        }

        public int getInMemoryByteSize()
        {
            return _cachedInMemorySize;
        }
    }

    private static TraceNLS nls = TraceNLS.getTraceNLS(MessageStoreConstants.MSG_BUNDLE);
    private static TraceComponent tc = SibTr.register(UpdateTask.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    private Persistable _cachedPersistable = null;
    private Persistable _primaryPersistable;

    public UpdateTask(AbstractItemLink link) throws SevereMessageStoreException
    {
        super(link);
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", link);

        _primaryPersistable = super.getPersistable();

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>", this);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.task.Task#abort(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void abort(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "abort", transaction);

        getLink().abortUpdate(transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "abort");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.task.Task#commitStage2(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void commitExternal(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "commitExternal", transaction);

        getLink().commitUpdate(transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commitExternal");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.task.Task#commitStage1(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void commitInternal(final PersistentTransaction transaction) {}

    /*
     *  (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.Operation#copyDataIfVulnerable()
     */
    public final void copyDataIfVulnerable() throws PersistentDataEncodingException, SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "copyDataIfVulnerable");

        if (_cachedPersistable == null)
        {
            _cachedPersistable = new CachedPersistable(_primaryPersistable);
        }; // end if

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "copyDataIfVulnerable");
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
     * @see com.ibm.ws.sib.msgstore.persistence.Operation#getPersistableInMemorySizeApproximation(int)
     */
    public final int getPersistableInMemorySizeApproximation(TransactionState tranState)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getPersistableInMemorySizeApproximation", tranState);

        int size;

        if ((tranState == TransactionState.STATE_COMMITTED) || (tranState == TransactionState.STATE_COMMITTING_1PC))
        {
            size = DEFAULT_TASK_PERSISTABLE_SIZE_APPROXIMATION + getPersistable().getInMemoryByteSize();
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
     * @see com.ibm.ws.sib.msgstore.task.Task#getTaskType()
     */
    public final Task.Type getTaskType()
    {
        return Type.UPDATE;
    }

    /*
     *  (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.Operation#persist(com.ibm.ws.sib.msgstore.persistence.BatchingContext, int)
     */
    public final void persist(BatchingContext batchingContext, TransactionState tranState)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "persist", new Object[] { batchingContext, tranState});

        if ((tranState == TransactionState.STATE_COMMITTED) || (tranState == TransactionState.STATE_COMMITTING_1PC))
        {
            batchingContext.updateDataAndSize(getPersistable());
        }
        else
        {
            throw new IllegalStateException(nls.getFormattedMessage("INVALID_TASK_OPERATION_SIMS1520", new Object[] {tranState}, null));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "persist");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.task.Task#postAbort(com.ibm.ws.sib.msgstore.Transaction)
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
            item.eventPostRollbackUpdate(transaction);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "postAbort");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.task.Task#postCommit(com.ibm.ws.sib.msgstore.Transaction)
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
            item.eventPostCommitUpdate(transaction);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "postCommit");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.task.Task#preCommit(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void preCommit(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "preCommit", transaction);

        getItem();  // cache item
        getLink().preCommitUpdate(transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "preCommit");
    }
}
