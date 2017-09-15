package com.ibm.ws.sib.msgstore.transactions.impl;
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
import java.util.Collections;
import java.util.List;

import javax.transaction.xa.*;

import com.ibm.websphere.sib.exception.SIResourceException;
import com.ibm.ws.sib.transactions.PersistentTranId;
import com.ibm.ws.sib.transactions.TransactionCallback;

import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.sib.utils.ras.SibTr;

import com.ibm.ws.sib.msgstore.*;
import com.ibm.ws.sib.msgstore.impl.MessageStoreImpl;
import com.ibm.ws.sib.msgstore.persistence.BatchingContext;
import com.ibm.ws.sib.msgstore.task.TaskList;

/**
 * This is the two-phase implementation of the Transaction interface. When passed as
 * part of a call to the ItemStream interfaces this object will allow the execution
 * of work in multiple item batches that can then be committed or rolled back as a group
 * using the XA two-phase-commit transaction protocol. This type of Transaction is
 * commonly used to participate in globally coordinated transactions involving more than
 * one resource manager. Essentially this class maps to a single Xid branch in the XA model.
 */
public class XidParticipant implements TransactionParticipant
{
    private static TraceComponent tc = SibTr.register(XidParticipant.class,
                                                      MessageStoreConstants.MSG_GROUP,
                                                      MessageStoreConstants.MSG_BUNDLE);

    // PK81848: Transaction state is updated/checked within a lock.
    // This ensures data visibility between threads, and allows us to defer rollbacks if
    // they are requested while an action is currently being performed on another thread.
    // No methods should be called with the lock held, it should only be used for checking/setting state.    
    private static class TransactionStateLock {}
    private final TransactionStateLock _stateLock = new TransactionStateLock();    

    private TransactionState   _state           = TransactionState.STATE_ACTIVE;
    private boolean            _rollbackChanges = false;
    private boolean            _indoubt         = false;

    // A separate substate is introduced (in PK81848) for the pre-commit phase of the transaction.
    // This is the period between entering the prepare, or one-phase commit, method and returning
    // from that method.
    // State transitions for the pre-commit substate are as follows:
    //                     / -> ROLLBACK_DEFERRED -> ROLLINGBACK -> ROLLEDBACK
    // NONE -> PREPARING -<
    //                     \ -> PREPARED
    // Note that the entire lifecycle is within the execution of the prepare/commit method.
    private TransactionState   _preCommitSubstate = TransactionState.STATE_NONE;
    // End of state variables associated with the state lock.

    private PersistentTranId   _ptid;
    private WorkList           _workList;

    private final List<TransactionCallback>  _callbacks = Collections.synchronizedList(new ArrayList<TransactionCallback>(5));

    private MessageStoreImpl   _ms;
    private PersistenceManager _persistence;

    private int                _maxSize;
    private int                _size = 0;

    private BatchingContext    _bc;


    /**
     * <B>Standard Constructor</B>
     * <BR>
     * Used during normal processing to instantiate a new XidParticipant.
     * 
     * @param ms      The MessageStore that this participant is doing work in.
     * @param ptid    The Global transaction id
     * @param persistence
     *                The PersistenceManager to use for storing the transactions work
     * @param maxSize The maximum number of actions allowed in this transaction
     */
    public XidParticipant(MessageStoreImpl ms, PersistentTranId ptid, PersistenceManager persistence, int maxSize)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[] {"MessageStore="+ms, "PersistentTranId="+ptid, "Persistence="+persistence, "MaxSize="+maxSize});

        _ms          = ms;
        _ptid        = ptid;
        _persistence = persistence;
        _maxSize     = maxSize;

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
    }


    /**
     * <B>Recovery Constructor</B>
     * <BR>
     * Used at recovery time to instantiate an XidParticipant that has
     * already been prepared and so is waiting for completion.
     * 
     * @param ms      The MessageStore that this participant is doing work in.
     * @param ptid    The Global transaction id
     * @param persistence
     *                The PersistenceManager to use for storing the transactions work
     * @param maxSize The maximum number of actions allowed in this transaction
     * @param state   The state of this recovered transaction
     */
    public XidParticipant(MessageStoreImpl ms, PersistentTranId ptid, PersistenceManager persistence, int maxSize, TransactionState state)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "<init>", new Object[] {"MessageStore="+ms, "PersistentTranId="+ptid, "Persistence="+persistence, "State="+_state});

        _ms          = ms;
        _ptid        = ptid;
        _persistence = persistence;
        _state       = state;
        _maxSize     = maxSize;

        if (_state == TransactionState.STATE_PREPARED)
        {
            _rollbackChanges = true;
            _indoubt         = true;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "<init>");
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

    /*************************************************************************/
    /*                       Transaction Implementation                      */
    /*************************************************************************/


    public synchronized void addWork(WorkItem item) throws ProtocolException, TransactionException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "addWork", "WorkItem="+item);

        // Check the current transaction state (use state lock to ensure we see the latest value)
        synchronized (_stateLock)
        {
            if ((_state != TransactionState.STATE_ACTIVE) && !_indoubt)
            {
                ProtocolException pe = new ProtocolException("TRAN_PROTOCOL_ERROR_SIMS1001", new Object[]{});
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot add work to transaction. Transaction is complete or completing!", pe);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "addWork");
                throw pe;
            }
        }

        if (_workList == null)
        {
            _workList = new TaskList();
        }

        if (item != null)
        {
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


    // Defect 178563
    public WorkList getWorkList()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getWorkList");
            SibTr.exit(this, tc, "getWorkList", "return="+_workList);
        }
        return _workList;
    }


    // Defect 178563
    public void registerCallback(TransactionCallback callback)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "registerCallback", "Callback="+callback);

        _callbacks.add(callback);

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "registerCallback");
    }


    public boolean isAutoCommit() 
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "isAutoCommit");
            SibTr.exit(this, tc, "isAutoCommit", "return=false");
        }
        return false;
    }


    // Feature 184806.3.2
    public PersistentTranId getPersistentTranId()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getPersistentTranId");
            SibTr.exit(this, tc, "getPersistentTranId", "return="+_ptid);
        }
        return _ptid;
    }


    // Feature 199334.1
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


    // Defect 186657.4
    public boolean isAlive()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "isAlive");

        boolean retval;
        // Check the current transaction state (use state lock to ensure we see the latest value)
        synchronized (_stateLock)
        {
            retval = (_state == TransactionState.STATE_ACTIVE || _indoubt);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "isAlive", "return="+retval);
        return retval;
    }


    /*************************************************************************/
    /*                 TransactionParticipant Implementation                 */
    /*************************************************************************/


    public int prepare() throws ProtocolException, RollbackException, SeverePersistenceException, TransactionException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "prepare");

        // Check the current transaction state (use state lock to ensure we see the latest value)
        synchronized (_stateLock)
        {
            if (_state != TransactionState.STATE_ACTIVE)
            {
                ProtocolException pe = new ProtocolException("TRAN_PROTOCOL_ERROR_SIMS1001", new Object[]{});
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot prepare Transaction. Transaction is complete or completing!", pe);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare");
                throw pe;
            }

            // Transition our prepare sub-state immediately - before we call the callbacks
            _preCommitSubstate = TransactionState.STATE_PREPARING;
        }

        // PK81848
        // From this point on, we must perform a rollback if we fail to complete the prepare,
        // or another thread requests a deferred rollback
        try
        {
            // Call beforeCompletion on MP callback
            for (int i = 0; i < _callbacks.size(); i++)
            {
                TransactionCallback callback = (TransactionCallback) _callbacks.get(i);
                callback.beforeCompletion(this);
            }

            // Do we have work to do?
            if (_workList != null)
            {
                _workList.preCommit(this);

                synchronized (_stateLock)
                {
                    // Need to change the overalll state after we have 
                    // called the callbacks and preCommitted 
                    // the workList as they may trigger
                    // an addWork call to add work to the
                    // transaction.
                    _state = TransactionState.STATE_PREPARING;
                }

                // The transaction is about to be in a state where any rollback must
                // update the database.  We cannot narrow the window any more than this
                _rollbackChanges = true;
                
                // Perform the prepare
                _persistence.prepare(this);

                // State check/change must be performed holding the state lock
                synchronized (_stateLock)
                {
                    // Another thread may have have received a rollback, while we were
                    // performing our prepare - if the database was extremely slow to respond
                    if (_preCommitSubstate == TransactionState.STATE_ROLLBACK_DEFERRED)
                    {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.event(this, tc, "Deferred rollback requested during prepare for TranId: "+_ptid);

                        // Throw an exception to trigger rollback processing.
                        throw new DeferredRollbackException();
                    }
                    else
                    {
                        _state = _preCommitSubstate = TransactionState.STATE_PREPARED;
                    }
                }                
            }
            else
            {
                // State change must be performed holding the state lock
                synchronized (_stateLock)
                {
                    // Defect 301480
                    // We don't have any work in our workList so in theory we should be
                    // able to vote READ_ONLY at this point. However in a particular case
                    // (Best Effort message being sent off server) a call back in an empty
                    // XAResource is used to trigger work on a remote server. In this case 
                    // we need beforeCompletion AND afterCompletion to be called at the 
                    // CORRECT time during transaction completion to ensure that the off 
                    // server work stays part of the XA transaction and gets informed of 
                    // the correct completion direction.
                    _state = _preCommitSubstate = TransactionState.STATE_PREPARED;
                }
            }
        }
        catch (SeverePersistenceException spe)
        {
            // SPE exceptions are handled with an RMFAIL return code by the caller,
            // so unlike all other types of exception no attempt is made here to
            // rollback the transaction. However, we report a local error - so
            // the whole JVM will be coming down with a few minutes.
            com.ibm.ws.ffdc.FFDCFilter.processException(spe, "com.ibm.ws.sib.msgstore.transactions.XidParticipant.prepare", "1:420:1.73", this);

            // As we have thrown a severe exception we need to inform
            // the ME that this instance is no longer able to continue.
            if (_ms != null)
            {
                _ms.reportLocalError();
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unrecoverable persistence exception caught during prepare phase of transaction!", spe);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare", spe);
            throw spe;
        }
        // Catch and handle all exceptions but allow 
        // Errors to filter up without interference
        catch (Exception e)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.transactions.XidParticipant.prepare", "1:436:1.73", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Transaction will roll-back due to Exception in prepare!", e);

            // Update our prepare state before calling rollback. If we are responding
            // to a deferred rollback request this will have no affect but if this is 
            // due to some other failure it will lock out any other attempts to rollback
            synchronized (_stateLock)
            {
                _preCommitSubstate = TransactionState.STATE_ROLLINGBACK;
            }

            // An error has occurred but we are able to keep the ME
            // going so we just need to clean up this transaction.
            try
            {
                // When we throw our RollbackException the TM is
                // going to assume we are complete so we need to 
                // ensure we are rolled-back.
                // 
                // PK81848.2
                // We need to pass "true" in order to turn off the
                // state checking which should block other threads 
                // from attempting a rollback when we are in the 
                // middle of internal rollback processing.
                rollback(true);
            }
            // Catch and handle all exceptions but allow 
            // Errors to filter up without interference
            catch (Exception e2)
            {
                com.ibm.ws.ffdc.FFDCFilter.processException(e2, "com.ibm.ws.sib.msgstore.transactions.XIDParticipant.prepare", "1:466:1.73", this);
                
                // This is a problem because we now must RMFAIL this prepare call as we do not know the outcome.
                // Transition to rollback expected state now even if we are in deferred rollback.  This is the
                // only thing we can do (problem is we said XA_OK to roll back in deferred case and now we
                // cannot honour it).  This case may still leave indoubts.
                synchronized(_stateLock)
                {
                    _state = _preCommitSubstate = TransactionState.STATE_ROLLBACK_EXPECTED;
                }
                
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare", "Exception caught rolling-back transaction after preCommit failure! " + e2);
                throw new TransactionException(e2);
            }

            // Mark ourselves as complete, regardless of the outcome of the attempt to rollback.
            synchronized (_stateLock)
            {
                _state = _preCommitSubstate = TransactionState.STATE_ROLLEDBACK;
            }

            // Wrap any persistence exception, or an unexpected exception, in a rollback exception.
            // This informs the TM that they can forget about this transaction.
            RollbackException re = new RollbackException("COMPLETION_EXCEPTION_SIMS1002", new Object[]{e}, e);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare", re);
            throw re;
        }

        // Non-exception return path always returns XA_OK
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "prepare", "return="+XAResource.XA_OK);
        return XAResource.XA_OK;
    }


    public void commit(final boolean onePhase) throws ProtocolException, RollbackException, SeverePersistenceException, 
                                                      TransactionException, PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "commit", "onePhase="+onePhase);

        // State check must be performed holding the state lock
        synchronized (_stateLock)
        {
            if ((_state == TransactionState.STATE_COMMITTED) || (_state == TransactionState.STATE_ROLLEDBACK))
            {
                ProtocolException pe = new ProtocolException("TRAN_PROTOCOL_ERROR_SIMS1001", new Object[]{});
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot commit Transaction. Transaction is already complete!", pe);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
                throw pe;
            }

            if (!onePhase && (_state != TransactionState.STATE_PREPARED))
            {
                ProtocolException pe = new ProtocolException("XA_PROTOCOL_ERROR_SIMS1000", new Object[]{});
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot commit Transaction. Transaction has not been prepared!", pe);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
                throw pe;
            }

            // If this is a one-phase commit, then use the pre-commit substate to protect
            // against a rollback arriving asynchronously while we are processing.
            // This transition occurs immediately, before we invoke any callbacks.
            if (onePhase)
            {
                _preCommitSubstate = TransactionState.STATE_PREPARING;
            }
            else
            {
                // Not one-phase so change our state early to stop any rollbacks from
                // being requested on a different thread.
                _state = TransactionState.STATE_COMMITTING_2PC;
            }
        }

        try
        {
            // if we are completing using one-phase-commit then we need 
            // to call our beforeCompletion callbacks as they won't have
            // been called in prepare.
            if (onePhase)
            {
                for (int i = 0; i < _callbacks.size(); i++)
                {
                    TransactionCallback callback = (TransactionCallback) _callbacks.get(i);
                    callback.beforeCompletion(this);
                }
    
                // Do we have work to do?
                if (_workList != null)
                {
                    _workList.preCommit(this);
                }
    
                // State change must be performed holding the state lock
                synchronized (_stateLock)
                {
                    // Need to change the state after we have 
                    // called the callbacks as they may trigger
                    // a setWorkList call to add work to the
                    // transaction.
                    // This will be visible to all threads, and we do not allow rollbacks
                    // on any other thread once we have reached this state change.
                    _state = TransactionState.STATE_COMMITTING_1PC;
    
                    // PK81848.2
                    // If another thread has requested a rollback while we were
                    // calling our callbacks this is the last point at which
                    // we can honour it.
                    if (_preCommitSubstate == TransactionState.STATE_ROLLBACK_DEFERRED)
                    {
                        // Throw exception to trigger rollback processing.
                        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.event(this, tc, "Deferred rollback requested during preCommit for TranId: "+_ptid);

                        throw new DeferredRollbackException();
                    }
                }
            } // end if (onePhase)

            // Peek the current indoubt state, then drop the state lock before calling any methods.
            boolean indoubt;
            synchronized (_stateLock)
            {
                indoubt = _indoubt;
            }

            // Do we have work to do?
            if (_workList != null)
            {
                // Call the persistence layer to write our 
                // committed changes to the database.
                _persistence.commit(this, onePhase);

                _workList.commit(this);
            }
            else if (_workList == null && indoubt)
            {
                // Defect 572430
                // If this transaction is a recovered in-doubt and its work
                // list is null then it will have contained aysnchronous
                // persistent work at prepare time. As a result of an ME
                // failure the asynchronous work did not have time to be 
                // written to the database. However as the write of the 
                // row to the tran table is synchronous we will have a 
                // transaction to clean up.
                _persistence.commit(this, onePhase);
            }

            if (indoubt)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Indoubt transaction completed successfully. TranId="+_ptid);
            }

            // State change must be performed holding the state lock
            synchronized (_stateLock)
            {
                // The transaction moves into committed state
                _state = TransactionState.STATE_COMMITTED;

                // The pre-commit substate moves into the final state (PREPARED)
                if (onePhase) 
                {
                    _preCommitSubstate = TransactionState.STATE_PREPARED;
                }
            }
        }
        catch (SeverePersistenceException spe)
        {
            // SPE exceptions are handled with an RMFAIL return code by the caller,
            // so unlike all other types of exception no attempt is made here to
            // rollback the transaction. However, we report a local error - so
            // the whole JVM will be coming down with a few minutes.
            com.ibm.ws.ffdc.FFDCFilter.processException(spe, "com.ibm.ws.sib.msgstore.transactions.XidParticipant.commit", "1:636:1.73", this);

            // As we have thrown a severe exception we need to inform
            // the ME that this instance is no longer able to continue.
            if (_ms != null)
            {
                _ms.reportLocalError();
            }
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unrecoverable persistence exception caught during commit phase of transaction!", spe);
            throw spe;
        }
        // Catch and handle all exceptions but allow 
        // Errors to filter up without interference
        catch (Exception e)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.transactions.XidParticipant.commit", "1:651:1.73", this);

            if (onePhase)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Transaction will rollback due to exception in commit!", e);

                // Update our preCommit state before calling rollback. If we are responding
                // to a deferred rollback request this will have no affect but if this is 
                // due to some other failure it will lock out any other attempts to rollback
                synchronized (_stateLock)
                {
                    _preCommitSubstate = TransactionState.STATE_ROLLINGBACK;
                }
    
                // An error has occurred but we are able to keep the ME
                // going so we just need to clean up this transaction.
                try
                {
                    // When we throw our RollbackException the TM is
                    // going to assume we are complete so we need to 
                    // ensure we are rolled-back.
                    if (_workList != null)
                    {
                        _workList.rollback(this);
                    }
                }
                // Catch and handle all exceptions but allow 
                // Errors to filter up without interference
                catch (Exception e2)
                {
                    // We are going to throw either a rollback exception, or a SPE, at this point anyway.
                    // FFDC and continue.
                    com.ibm.ws.ffdc.FFDCFilter.processException(e2, "com.ibm.ws.sib.msgstore.transactions.XIDParticipant.commit", "1:683:1.73", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught rolling-back transaction after commit failure!", e2);
                }
    
                // Mark ourselves as complete, regardless of the outcome of the attempt to rollback.
                synchronized (_stateLock)
                {
                    _state = _preCommitSubstate = TransactionState.STATE_ROLLEDBACK;
                }
    
                // Wrap any persistence exception, or an unexpected exception, in a rollback exception.
                // This informs the TM that they can forget about this transaction.
                RollbackException re = new RollbackException("COMPLETION_EXCEPTION_SIMS1002", new Object[]{e}, e);
                throw re;
            }
            // For a two-phase transaction, we need to return to prepared state, ready
            // for a retry attempt by the TM
            else
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Transaction will be reset for retry due to exception in commit!", e);

                // State change must be performed holding the state lock
                synchronized (_stateLock)
                {
                    _state = TransactionState.STATE_PREPARED;
                }

                // If we had a failed commit in a two-phase case, we need to throw a non-rollback exception.
                // If we have a PE then we can throw it directly, otherwise we need to wrap our exception
                // in a generic transaction exception.
                if (e instanceof PersistenceException)
                {
                    throw (PersistenceException)e;          
                }
                else
                {
                    throw new TransactionException("COMPLETION_EXCEPTION_SIMS1002", new Object[]{e}, e);
                }
            }
        }
        finally
        {
            // State check must be performed holding the state lock
            TransactionState state;
            synchronized(_stateLock)
            {
                state = _state;
            }

            // Defect 398385
            // Are we still prepared? If so then we are expecting
            // to retry our commit processing and don't need to
            // call postComplete yet.
            if (state != TransactionState.STATE_PREPARED)
            {
                // We always ensure that all afterCompletion
                // callbacks are called even in the case of 
                // rollback.
                boolean committed = (state == TransactionState.STATE_COMMITTED);
    
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
                // Catch and handle all exceptions but allow 
                // Errors to filter up without interference
                catch (Exception e)
                {
                    com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.transactions.XidParticipant.commit", "1:761:1.73", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught during post commit phase of transaction!", e);
                    // We aren't going to rethrow this exception as if
                    // a previous exception has been thrown outside the 
                    // finally block it is likely to have more 
                    // information about the root problem.
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "commit");
        }
    }


    public void rollback() throws ProtocolException, SeverePersistenceException, TransactionException, PersistenceException
    {
        // PK81848.2
        // This will call our internal rollback with 
        // all state checks carried out.
        rollback(false);
    }

    // PK81848.2
    // We now have this internal  method to allow us to bypass the state 
    // checking when calling rollback from within another completion 
    // method. Without this changing the state in prepare or commit  would
    // also cause the rollback called from within prepare/commit to fail as
    // it would not pass the state check.
    private void rollback(boolean internal) throws ProtocolException, SeverePersistenceException, TransactionException, PersistenceException
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "rollback", "internal="+internal);

        // State check/change must be performed holding the state lock
        synchronized (_stateLock)
        {
            // For XA tranasctions, we accept a rollback on any thread, at any time until the point
            // we have begun commit processing. The XA spec says we must be able to rollback until we
            // have successfully committed. Hence it could be argued that we should still be able
            // to accept a rollback while in committing states. However, it is beyond the scope of
            // PK81848 (which adds the committing checks) to address that.

            if (!internal && _state != TransactionState.STATE_ROLLBACK_EXPECTED)
            {
                // It is worth noting in these state checks that once the transition into ROLLBACK_DEFERRED
                // has taken place a succesful return from rollback will follow and therefore no further 
                // attempts should be made. In case we get really unlucky however and have multiple threads
                // calling rollback at once the preCommit states should be checked for also.
                if ((_state == TransactionState.STATE_COMMITTED) ||                     // Tran has committed
                    (_state == TransactionState.STATE_ROLLEDBACK) ||                    // Tran has rolled back
                    (_state == TransactionState.STATE_COMMITTING_1PC) ||                // Tran has started one-phase commit
                    (_state == TransactionState.STATE_COMMITTING_2PC) ||                // Tran has started two-phase commit
                    (_preCommitSubstate == TransactionState.STATE_ROLLBACK_DEFERRED) || // Rollback has been deferred and is awaiting prepare thread
                    (_preCommitSubstate == TransactionState.STATE_ROLLINGBACK))         // Prepare thread has is processing the deferred rollback
                {
                    ProtocolException pe = new ProtocolException("TRAN_PROTOCOL_ERROR_SIMS1001", new Object[]{});
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Cannot rollback Transaction. Transaction is not in a valid state: _state="+_state+", _preCommitSubstate="+_preCommitSubstate, pe);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
                    throw pe;
                }
            }

            // If another thread is currently preparing this transaction, then we cannot safely
            // perform the rollback - even though it's a valid XA transition.
            // As such, we change the state to deferred rollback, and leave the preparing
            // thread to perform the rollback and throw XA_RBROLLBACK to the caller of prepare.
            // Holding the _stateLock allows us to ensure that the rollback will occur, as the
            // preparing thread must take this lock and check for the state before it returns.
            // This does mean we will be returning from the rollback before the work of the rollback
            // has completed. This is an interpretation of the spec, based on our inabillty
            // to honor "xa_rollback() must not itself be susceptible to indefinite blocking" with
            // an external database that is outside of our control.
            if (_preCommitSubstate == TransactionState.STATE_PREPARING)
            {
                _preCommitSubstate = TransactionState.STATE_ROLLBACK_DEFERRED;

                // Return immediately, with good completion
                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Deferred rollback request accepted for TranId: "+_ptid);
                if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback", "STATE_ROLLBACK_DEFERRED");
                return;
            }
            else
            {
                _state = TransactionState.STATE_ROLLINGBACK;
            }
        }

        try
        {
            // Peek the current values of our state variables, and drop the lock before making any calls
            boolean rollbackChanges;
            boolean indoubt;
            synchronized (_stateLock)
            {
                rollbackChanges = _rollbackChanges;
                indoubt         = _indoubt;
            }
            // Do we have work to do?
            if (_workList != null)
            {
                if (rollbackChanges)
                {
                    // We need to rollback the persistence layer as
                    // work will have been committed to the database
                    // in the prepare stage.
                    _persistence.rollback(this);
                }

                _workList.rollback(this);
            }
            else if (_workList == null && indoubt)
            {
                // Defect 572430
                // If this transaction is a recovered in-doubt and its work
                // list is null then it will have contained aysnchronous
                // persistent work at prepare time. As a result of an ME
                // failure the asynchronous work did not have time to be 
                // written to the database. However as the write of the 
                // row to the tran table is synchronous we will have a 
                // transaction to clean up.
                _persistence.rollback(this);
            }

            if (indoubt)
            {
                if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled()) SibTr.debug(this, tc, "Indoubt transaction completed successfully. TranId="+_ptid);
            }

            // State change must be performed holding the state lock
            synchronized (_stateLock)
            {
                _state = TransactionState.STATE_ROLLEDBACK;
            }
        }
        catch (SeverePersistenceException spe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(spe, "com.ibm.ws.sib.msgstore.transactions.XidParticipant.rollback", "1:896:1.73", this);

            // As we have thrown a severe exception we need to inform
            // the ME that this instance is no longer able to continue.
            if (_ms != null)
            {
                _ms.reportLocalError();
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unrecoverable persistence exception caught during rollback phase of transaction!", spe);
            throw spe;
        }
        catch (PersistenceException pe)
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(pe, "com.ibm.ws.sib.msgstore.transactions.XidParticipant.rollback", "1:910:1.73", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Persistence exception caught during rollback phase of transaction!", pe);
            throw pe;
        }
        // Catch and handle all exceptions but allow 
        // Errors to filter up without interference
        catch (Exception e) 
        {
            com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.transactions.XidParticipant.rollback", "1:918:1.73", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Unexpected exception caught during rollback phase of transaction!", e);
            throw new TransactionException("COMPLETION_EXCEPTION_SIMS1002", new Object[]{e}, e);
        }
        finally
        {
            // Peek the current transaction state and drop the lock before making any calls
            TransactionState state;
            synchronized (_stateLock)
            {
                state = _state;
            }

            if (state == TransactionState.STATE_ROLLEDBACK)
            {
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
                // Catch and handle all exceptions but allow 
                // Errors to filter up without interference
                catch (Exception e)
                {
                    com.ibm.ws.ffdc.FFDCFilter.processException(e, "com.ibm.ws.sib.msgstore.transactions.XidParticipant.commit", "1:951:1.73", this);
                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) SibTr.event(this, tc, "Exception caught during post commit phase of transaction!", e);
                    // We aren't going to rethrow this exception as if
                    // a previous exception has been thrown outside the 
                    // finally block it is likely to have more 
                    // information about the root problem.
                }
            }

            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "rollback");
        }
    }

    public String toXmlString()
    {
        StringBuffer retval = new StringBuffer();

        retval.append("<transaction>\n");
        retval.append("<type>GLOBAL</type>\n");
        retval.append("<xid>");
        retval.append(_ptid);
        retval.append("</xid>\n");

        retval.append("<state>");
        retval.append(_state);
        retval.append("</state>\n");

        retval.append("<size>");
        retval.append(_size);
        retval.append("</size>\n");

        retval.append("<max-size>");
        retval.append(_maxSize);
        retval.append("</max-size>\n");

        if (_workList != null)
        {
            retval.append(_workList.toXmlString());
        }

        retval.append("</transaction>\n");

        return retval.toString();
    }


    /*************************************************************************/
    /*                  PersistentTransaction Implementation                 */
    /*************************************************************************/


    public final int getTransactionType()
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
        {
            SibTr.entry(this, tc, "getTransactionType");
            SibTr.exit(this, tc, "getTransactionType", "return=TX_GLOBAL");
        }
        return TX_GLOBAL;
    }

    public void setTransactionState(TransactionState state)
    {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.entry(this, tc, "setTransactionState", "State="+state);

        // State change must be performed holding the state lock
        synchronized (_stateLock)
        {
            _state = state;
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled()) SibTr.exit(this, tc, "setTransactionState");
    }

    public TransactionState getTransactionState()
    {
        // State check must be performed holding the state lock
        synchronized (_stateLock)
        {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            {
                SibTr.entry(this, tc, "getTransactionState");
                SibTr.exit(this, tc, "getTransactionState", "return="+_state);
            }
            return _state;
        }
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
