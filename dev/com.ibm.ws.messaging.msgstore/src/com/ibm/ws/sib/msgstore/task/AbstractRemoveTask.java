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

import com.ibm.ws.sib.msgstore.AbstractItem;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.SevereMessageStoreException;
import com.ibm.ws.sib.msgstore.cache.links.AbstractItemLink;
import com.ibm.ws.sib.msgstore.cache.links.LinkOwner;
import com.ibm.ws.sib.msgstore.persistence.BatchingContext;
import com.ibm.ws.sib.msgstore.persistence.Persistable;
import com.ibm.ws.sib.msgstore.transactions.impl.PersistentTransaction;
import com.ibm.ws.sib.msgstore.transactions.impl.TransactionState;

import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

public abstract class AbstractRemoveTask extends Task
{
    private static TraceNLS nls = TraceNLS.getTraceNLS(MessageStoreConstants.MSG_BUNDLE);
    private static TraceComponent tc = SibTr.register(AbstractRemoveTask.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    public AbstractRemoveTask(final AbstractItemLink link) throws SevereMessageStoreException
    {
        super(link);
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.xalist.Task#abort(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void abort(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "abort", transaction);

        getLink().abortRemove(transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "abort");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.xalist.Task#commitStage2(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void commitExternal(final PersistentTransaction transaction)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "commitExternal", transaction);
            SibTr.exit(this, tc, "commitExternal");
        }
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.xalist.Task#commitStage1(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void commitInternal(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "commitInternal", transaction);

        getLink().commitRemove(transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commitInternal");
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
            || (tranState == TransactionState.STATE_COMMITTING_2PC)
            || (tranState == TransactionState.STATE_PREPARED)
            || (tranState == TransactionState.STATE_PREPARING)
            || (tranState == TransactionState.STATE_ROLLEDBACK)
            || (tranState == TransactionState.STATE_ROLLINGBACK))
        {
            size = DEFAULT_TASK_PERSISTABLE_SIZE_APPROXIMATION + getPersistable().getInMemoryByteSize();
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getPersistableInMemorySizeApproximation");
            throw new IllegalStateException(nls.getFormattedMessage("INVALID_TASK_OPERATION_SIMS1520", new Object[] {tranState}, null));
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getPersistableInMemorySizeApproximation", Integer.valueOf(size));
        return size;
    }

    /*
     *  (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.task.Task#getTaskType()
     */
    public Task.Type getTaskType()
    {
        return Type.REMOVE;
    }

    /*
     *  (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.persistence.Operation#isDeleteOfPersistentRepresentation()
     */
    public boolean isDeleteOfPersistentRepresentation()
    {
        return true;
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.xalist.Task#isRemoveFromList(com.ibm.ws.sib.msgstore.cache.xalist.TransactionalList)
     */
    public final boolean isRemoveFromList(final LinkOwner list)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isRemoveFromList");

        LinkOwner myList = getLink().getOwningStreamLink();
        boolean is = (myList == list);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isRemoveFromList", Boolean.valueOf(is));
        return is;
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
            || (tranState == TransactionState.STATE_COMMITTING_2PC))
        {
            batchingContext.delete(getPersistable());
        }
        else if ((tranState == TransactionState.STATE_PREPARED)
                 || (tranState == TransactionState.STATE_PREPARING))
        {
            Persistable tuple = getPersistable();
            tuple.setLogicallyDeleted(true);
            batchingContext.updateLogicalDeleteAndXID(tuple);
        }
        else if ((tranState == TransactionState.STATE_ROLLEDBACK)
                 || (tranState == TransactionState.STATE_ROLLINGBACK))
        {
            Persistable tuple = getPersistable();
            tuple.setLogicallyDeleted(false);
            batchingContext.updateLogicalDeleteAndXID(tuple);
        }
        else
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "persist");
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
            item.eventPostRollbackRemove(transaction);
        }

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
            item.eventPostCommitRemove(transaction);
        }
        getLink().postCommitRemove(transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "postCommit");
    }

    /* (non-Javadoc)
     * @see com.ibm.ws.sib.msgstore.cache.xalist.Task#preCommit(com.ibm.ws.sib.msgstore.Transaction)
     */
    public final void preCommit(final PersistentTransaction transaction) throws SevereMessageStoreException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "preCommit", transaction);

        getItem();  // try to cache item in case we havent got it yet....
        getLink().preCommitRemove(transaction);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "preCommit");
    }
}
