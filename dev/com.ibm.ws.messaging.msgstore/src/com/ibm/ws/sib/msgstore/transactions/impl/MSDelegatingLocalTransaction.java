package com.ibm.ws.sib.msgstore.transactions.impl;
/*******************************************************************************
 * Copyright (c) 2012, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ejs.ras.TraceNLS;
import com.ibm.websphere.sib.exception.SIErrorException;
import com.ibm.websphere.sib.exception.SIIncorrectCallException;
import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.msgstore.MessageStore;
import com.ibm.ws.sib.msgstore.MessageStoreConstants;
import com.ibm.ws.sib.msgstore.PersistenceException;
import com.ibm.ws.sib.msgstore.ProtocolException;
import com.ibm.ws.sib.msgstore.SeverePersistenceException;
import com.ibm.ws.sib.msgstore.TransactionException;
import com.ibm.ws.sib.msgstore.TransactionMaxSizeExceededException;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.persistence.BatchingContext;
import com.ibm.ws.sib.msgstore.task.TaskList;
import com.ibm.ws.sib.msgstore.transactions.ExternalLocalTransaction;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.transactions.TransactionCallback;
import com.ibm.ws.sib.utils.ras.SibTr;
import com.ibm.wsspi.sib.core.exception.SIRollbackException;

/**
 * This is the one-phase implementation of the Transaction interface. When passed as
 * part of a call to the ItemStream interfaces this object will allow the execution
 * of work in multiple item batches that can then be committed or rolled back as a group
 * using the one-phase-commit transaction protocol. This type of Transaction is
 * commonly used to coordinate transactions that are local to the resource manager (ME).
 */
public class MSDelegatingLocalTransaction implements ExternalLocalTransaction, PersistentTransaction
{
    private static TraceNLS      nls = TraceNLS.getTraceNLS(MessageStoreConstants.MSG_BUNDLE);
    private static TraceComponent tc = SibTr.register(MSDelegatingLocalTransaction.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    protected MessageStoreImpl   _ms;
    protected PersistenceManager _persistence;
    protected XidManager         _xidManager;

    protected TransactionState   _state = TransactionState.STATE_ACTIVE;

    protected PersistentTranId  _ptid;
    protected WorkList          _workList;

    protected List<TransactionCallback>  _callbacks = Collections.synchronizedList(new ArrayList<TransactionCallback>(5));

    protected int                _maxSize;
    protected int                _size = 0;

    protected BatchingContext    _bc;


    /**
     * The Default constructor for MSDelegatingLocalTransaction.
     *
     * @param ms      The {@link MessageStoreImpl MessageStore} that this transaction is a part of.
     * @param persistence The {@link PersistenceManager} implementation to be used this transaction.
     * @param maxSize The number of operations allowed in this transaction.
     */
    public MSDelegatingLocalTransaction(MessageStoreImpl ms, PersistenceManager persistence, int maxSize)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "MSDelegatingLocalTransaction", "MessageStore="+ms+", Persistence="+persistence+", MaxSize="+maxSize);

        _ms          = ms;
        _persistence = persistence;
        _maxSize     = maxSize;

        if (_ms != null)
        {
          
            _xidManager = _ms.getXidManager();

        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "MSDelegatingLocalTransaction");
    }


    /*************************************************************************/
    /*                        Transaction Implementation                     */
    /*************************************************************************/


    public synchronized void addWork(WorkItem item) throws ProtocolException, TransactionException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "addWork", "WorkItem="+item);

        if ((_state != TransactionState.STATE_ACTIVE))
        {
            ProtocolException pe = new ProtocolException("TRAN_PROTOCOL_ERROR_SIMS1001", new Object[]{});
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot add work to transaction. Transaction is complete or completing!", pe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addWork");
            throw pe;
        }

        if (item != null)
        {
            if (_workList == null)
            {
                _workList = new TaskList();
            }

            _workList.addWork(item);
        }
        else
        {
            ProtocolException pe = new ProtocolException("TRAN_PROTOCOL_ERROR_SIMS1001", new Object[]{});
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot add null work item to transaction!", pe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addWork");
            throw pe;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addWork");
    }


    public WorkList getWorkList()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getWorkList");
            SibTr.exit(this, tc, "getWorkList", "return="+_workList);
        }
        return _workList;
    }


    public void registerCallback(TransactionCallback callback)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "registerCallback", "Callback="+callback);

        _callbacks.add(callback);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "registerCallback");
    }


    /**
     * @return True if (and only if) the transaction has (or can have)
     * subordinates.
     */
    public boolean hasSubordinates()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "hasSubordinates");

        boolean retval = false;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "hasSubordinates", "return="+retval);
        return retval;
    }


    public final boolean isAutoCommit()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "isAutoCommit");
            SibTr.exit(this, tc, "isAutoCommit", "return=false");
        }
        return false;
    }


    public synchronized PersistentTranId getPersistentTranId()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "getPersistentTranId");

        if (_ptid == null)
        {
            if (_xidManager != null)
            {
                _ptid = new PersistentTranId(_xidManager.generateLocalTranId());
            }
            else
            {
                _ptid = new PersistentTranId(this.hashCode());
            }
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "getPersistentTranId", "return="+_ptid);
        return _ptid;
    }


    public void incrementCurrentSize() throws SIResourceException
    {
        if (++_size > _maxSize)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Transaction size incremented: "+_size);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Maximum transaction size reached, throwing exception!");
            throw new SIResourceException(new TransactionMaxSizeExceededException());
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Transaction size incremented: "+_size);
    }


    public boolean isAlive()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isAlive");

        boolean retval = (_state == TransactionState.STATE_ACTIVE);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isAlive", "return="+retval);
        return retval;
    }


    /*************************************************************************/
    /*                    ExternalTransaction Implementation                 */
    /*************************************************************************/


    /**
     * Begin a new local transaction by readying this object for re-use by the
     * ItemStream interfaces.
     *
     * @exception SIIncorrectCallException
     *                   Thrown if existing work has not been completed.
     */
    public void begin() throws SIIncorrectCallException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "begin");

        // Are we currently in use? do we have work
        // we still need to commit?
        if (_state == TransactionState.STATE_ACTIVE && _workList != null)
        {
          SIIncorrectCallException ice = new SIIncorrectCallException(nls.getFormattedMessage("TRAN_PROTOCOL_ERROR_SIMS1001", null, "TRAN_PROTOCOL_ERROR_SIMS1001"));
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot begin new LocalTran. Existing work needs completing first!", ice);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "begin");
            throw ice;
        }

        synchronized(this)
        {
          _ptid     = null;
        }
        _workList = null;
        _size     = 0;

        _callbacks.clear();

        _state = TransactionState.STATE_ACTIVE;


        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "begin");
    }


    /*************************************************************************/
    /*                SIUncoordinatedTransaction Implementation              */
    /*************************************************************************/


    /**
     * Commit the work associated with the current local transaction.
     *
     * @exception SIIncorrectCallException
     *                   Thrown if this transaction has already been completed.
     * @exception SIRollbackException
     *                   The work associated with this LocalTransaction encountered problems and was
     *                   rolled-back as a result of them.
     * @exception SIResourceException
     *                   Thrown if an unknown MessageStore error occurs.
     * @exception SIErrorException
     *                   Thrown if an unknown error occurs.
     */
    public void commit() throws SIIncorrectCallException, SIRollbackException, SIResourceException, SIErrorException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "commit");

        if (_state != TransactionState.STATE_ACTIVE)
        {
            SIIncorrectCallException sie = new SIIncorrectCallException(nls.getFormattedMessage("CANNOT_COMMIT_COMPLETE_SIMS1004", new Object[]{}, null));
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot commit Transaction. Transaction is complete or completing!", sie);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
            throw sie;
        }

        for (int i = 0; i < _callbacks.size(); i++)
        {
            TransactionCallback callback = (TransactionCallback) _callbacks.get(i);
            callback.beforeCompletion(this);
        }

        // Do we have work to do?
        if (_workList != null)
        {
            try
            {
                _workList.preCommit(this);
            }
            catch (Throwable t)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingLocalTransaction.commit", "1:363:1.51.1.14", this);

                // An error has occurred during preCommit
                // so we need to rollback and throw a
                // RollbackException
                try
                {
                    // When we throw our RollbackException the TM is
                    // going to assume we are complete so we need to
                    // ensure we are rolled-back.
                    rollback();
                }
                catch (Exception e)
                {
                    com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingLocalTransaction.commit", "1:377:1.51.1.14", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught rolling-back transaction after preCommit failure!", e);
                }

                // Mark ourselves as complete and inform
                // the TM that we have rolled-back.
                _state = TransactionState.STATE_ROLLEDBACK;
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Transaction rolled-back due to Exception in preCommit!", t);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
                throw new SIRollbackException(nls.getFormattedMessage("COMPLETION_EXCEPTION_SIMS1002", new Object[] {t}, null), t);
            }
        }

        // Need to change the state after we have
        // called the callbacks and preCommitted
        // the workList as they may trigger
        // an addWork call to add work to the
        // transaction.
        _state = TransactionState.STATE_COMMITTING_1PC;

        try
        {
            // Do we have work to do?
            if (_workList != null)
            {
                _persistence.commit(this, true);

                _workList.commit(this);
            }

            _state = TransactionState.STATE_COMMITTED;
        }
        catch (SeverePersistenceException spe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(spe, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingLocalTransaction.commit", "1:411:1.51.1.14", this);

            // As we have thrown a severe exception we need to inform
            // the ME that this instance is no longer able to continue.
            if (_ms != null)
            {
                _ms.reportLocalError();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unrecoverable persistence exception caught during commit phase of transaction!", spe);
            throw new SIResourceException(nls.getFormattedMessage("COMPLETION_EXCEPTION_SIMS1002", new Object[] {spe}, null), spe);
        }
        catch (PersistenceException pe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingLocalTransaction.commit", "1:425:1.51.1.14", this);

            // The persistence layer has had a problem
            // so we need to rollback our in memory model
            // as we can't ensure our data's integrity.
            try
            {
                // When we throw our RollbackException the TM is
                // going to assume we are complete so we need to
                // ensure we are rolled-back.
                _workList.rollback(this);

                _state = TransactionState.STATE_ROLLEDBACK;
            }
            catch (Throwable t)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingLocalTransaction.commit", "1:441:1.51.1.14", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught rolling-back WorkList after Commit failure!", t);
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Transaction rolled-back due to Exception in Commit!", pe);
            throw new SIRollbackException(nls.getFormattedMessage("COMPLETION_EXCEPTION_SIMS1002", new Object[] {pe}, null), pe);
        }
        catch (Throwable t)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingLocalTransaction.commit", "1:450:1.51.1.14", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught during commit phase of transaction!", t);
            throw new SIErrorException(nls.getFormattedMessage("COMPLETION_EXCEPTION_SIMS1002", new Object[] {t}, null), t);
        }
        finally
        {
            // We always ensure that all afterCompletion
            // callbacks are called even in the case of
            // rollback.
            boolean committed = (_state == TransactionState.STATE_COMMITTED);

            // Defect 316887
            try
            {
                if (_workList != null)
                {
                    _workList.postComplete(this, committed);
                }

                for (int i = 0; i < _callbacks.size(); i++)
                {
                    TransactionCallback callback = (TransactionCallback) _callbacks.get(i);
                    callback.afterCompletion(this, committed);
                }
            }
            catch (Throwable t)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingLocalTransaction.commit", "1:477:1.51.1.14", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught during post commit phase of transaction!", t);
                // We aren't going to rethrow this exception as if
                // a previous exception has been thrown outside the
                // finally block it is likely to have more
                // information about the root problem.
            }


            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
        }
    }


    /**
     * Rollback all work associated with this local transaction.
     *
     * @exception SIIncorrectCallException
     *                   Thrown if this transaction has already been completed.
     * @exception SIResourceException
     *                   Thrown if an unknown MessageStore error occurs.
     */
    public void rollback() throws SIIncorrectCallException, SIResourceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "rollback");

        if (_state != TransactionState.STATE_ACTIVE)
        {
            SIIncorrectCallException sie = new SIIncorrectCallException(nls.getFormattedMessage("CANNOT_ROLLBACK_COMPLETE_SIMS1005", new Object[]{}, null));
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot rollback Transaction. Transaction is complete or completing!", sie);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
            throw sie;
        }

        _state = TransactionState.STATE_ROLLINGBACK;

        try
        {
            // We are rolling back all changes so we don't need
            // to tell the persistence layer to do anything we
            // just need to get the in memory model to back out
            // its changes.
            if (_workList != null)
            {
                _workList.rollback(this);
            }

            _state = TransactionState.STATE_ROLLEDBACK;
        }
        catch (Throwable t)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingLocalTransaction.rollback", "1:539:1.51.1.14", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught during rollback phase of transaction!", t);
            throw new SIResourceException(nls.getFormattedMessage("COMPLETION_EXCEPTION_SIMS1002", new Object[] {t}, null), t);
        }
        finally
        {
            // We always ensure that all afterCompletion
            // callbacks are called even in the case of
            // rollback.

            // Defect 316887
            try
            {
                if (_workList != null)
                {
                    _workList.postComplete(this, false);
                }

                for (int i = 0; i < _callbacks.size(); i++)
                {
                    TransactionCallback callback = (TransactionCallback) _callbacks.get(i);
                    callback.afterCompletion(this, false);
                }
            }
            catch (Throwable t)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(t, "com.ibm.ws.sib.msgstore.transactions.MSDelegatingLocalTransaction.rollback", "1:565:1.51.1.14", this);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught during post rollback phase of transaction!", t);
                // We aren't going to rethrow this exception as if
                // a previous exception has been thrown outside the
                // finally block it is likely to have more
                // information about the root problem.
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
        }
    }


    /*************************************************************************/
    /*                  PersistentTransaction Implementation                 */
    /*************************************************************************/


    public final int getTransactionType()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getTransactionType");
            SibTr.exit(this, tc, "getTransactionType", "return=TX_LOCAL");
        }
        return TX_LOCAL;
    }


    public void setTransactionState(TransactionState state)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setTransactionState", "State="+state);

        _state = state;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setTransactionState");
    }

    public TransactionState getTransactionState()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getTransactionState");
            SibTr.exit(this, tc, "getTransactionState", "return="+_state);
        }
        return _state;
    }


    public BatchingContext getBatchingContext()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getBatchingContext");
            SibTr.exit(this, tc, "getBatchingContext", "return="+_bc);
        }
        return _bc;
    }


    public void setBatchingContext(BatchingContext bc)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setBatchingContext", "BatchingContext="+bc);

        _bc = bc;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setBatchingContext");
    }

    // Defect 410652
    /**
     * This method is used to check the MessageStore instance that an implementing
     * transaction object originated from. This is used to check that a transaction
     * is being used to add Items to the same MessageStore as that it came from.
     *
     * @return The MessageStore instance where this transaction originated from.
     */
    public MessageStore getOwningMessageStore()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getOwningMessageStore");
            SibTr.exit(this, tc, "getOwningMessageStore", "return="+_ms);
        }
        return _ms;
    }
}
