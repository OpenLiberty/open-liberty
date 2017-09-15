package com.ibm.tx.jta.impl;
/*******************************************************************************
 * Copyright (c) 2002, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import javax.transaction.HeuristicCommitException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.InvalidTransactionException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.tx.TranConstants;
import com.ibm.tx.jta.*;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.Transaction.JTA.HeuristicHazardException;
import com.ibm.ws.Transaction.JTA.ResourceWrapper;
import com.ibm.ws.Transaction.JTA.StatefulResource;

public class JCATranWrapperImpl implements JCATranWrapper // @LIDB2110C
{
    private static final TraceComponent tc = Tr.register(JCATranWrapperImpl.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE); // @LIDB2110C

    protected TransactionImpl _txn;

    protected TranManagerSet _tranManager; 

    protected boolean _prepared;
    protected boolean _associated;
    protected int _heuristic; // = StatefulResource.NONE; MUST be 0

    protected int _suspendedUOWType;
    protected Object _suspendedUOW;

    protected JCATranWrapperImpl(){}

    /**
     *
     * Create a new transaction wrapper for an existing transaction
     *
     * @param transactionImpl
     * @param prepared
     * @param associated
     */
    public JCATranWrapperImpl(TransactionImpl transactionImpl, boolean prepared, boolean associated) // @LIDB2110C
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "JCATranWrapper", new Object[] { transactionImpl, prepared, associated});

        _tranManager = (TranManagerSet)TransactionManagerFactory.getTransactionManager();

        _txn = transactionImpl;

        _prepared = prepared;

        _associated = associated;

        if (tc.isEntryEnabled()) Tr.exit(tc, "JCATranWrapper", this);
    }

    /**
     *
     * Create a new transaction wrapper and transaction
     *
     * @param timeout
     * @param xid
     * @param jcard
     */
    public JCATranWrapperImpl(int timeout, Xid xid, JCARecoveryData jcard) // @D240298A
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "JCATranWrapper", new Object[] { timeout, xid, jcard });

        _tranManager = (TranManagerSet)TransactionManagerFactory.getTransactionManager();

        suspend();   // suspend and save any LTC before we create the global txn

        _txn = new TransactionImpl(timeout, xid, jcard);

        _prepared = false;

        _associated = true;

        if (tc.isEntryEnabled()) Tr.exit(tc, "JCATranWrapper", this);
    }

    /**
     * Requests the prepare vote from the object
     *
     * @return javax.transaction.xa.XAResource.XA_OK or 
     * javax.transaction.xa.XAResource.XA_RDONLY
     * @exception XAException thrown with the following error codes:
     * <ul><li>XA_RBROLLBACK - transaction has rolled back</li>
     * <li>XAER_PROTO - routine was invoked in an inproper context</li>
     * <li>XA_RMERR - a resource manager error has occurred</li></ul>
     *
     * @see javax.transaction.xa.XAException
     */
    public int prepare(TxXATerminator xat) throws XAException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "prepare", _txn);

        // replay must have finished
        if (_tranManager.isReplayComplete())
        {
            // Suspend local transaction if present
            suspend();

            try
            {
                // Resume imported transaction
                _tranManager.resume(_txn);
            }
            catch (InvalidTransactionException e)
            {
                if (tc.isEntryEnabled()) Tr.exit(tc, "prepare", new Object[] { "resume threw InvalidTransactionException", e });
                resume();
                throw new XAException(XAException.XAER_RMERR);
            }
            catch (IllegalStateException e)
            {
                if (tc.isEntryEnabled()) Tr.exit(tc, "prepare", new Object[] { "resume threw IllegalStateException", e });
                resume();
                throw new XAException(XAException.XAER_PROTO);
            }

            // We're going to log the provider that prepares the transaction
            _txn.setJCARecoveryData(xat.getJCARecoveryData());
            
            try
            {
                int state = _txn.internalPrepare();
                switch (state)
                {
                case TransactionState.STATE_PREPARED :
                    // Transaction has prepared and is in doubt
                    _prepared = true;

                    if (tc.isEntryEnabled()) Tr.exit(tc, "prepare", "XAResource.XA_OK");
                    return XAResource.XA_OK;

                case TransactionState.STATE_COMMITTED :
                    // Transaction has committed
                    _txn.notifyCompletion();

                    if (tc.isEntryEnabled()) Tr.exit(tc, "prepare", "XAResource.XA_RDONLY");
                    return XAResource.XA_RDONLY;

                default :
                    _txn.internalRollback();
                    _txn.notifyCompletion();

                    if (tc.isEntryEnabled()) Tr.exit(tc, "prepare", "throwing XA_RBROLLBACK");
                    throw new XAException(XAException.XA_RBROLLBACK);
                }
            }
            catch (HeuristicMixedException e)
            {
                if (tc.isEntryEnabled()) Tr.exit(tc, "prepare", "internalPrepare threw HeuristicMixedException");
                // must rollback any resources now
                try {
                    _txn.internalRollback();
                }
                catch (Throwable t)
                {
                    // swallow any exceptions
                }

                throw new XAException(XAException.XA_HEURMIX);
            }
            catch (HeuristicHazardException e)
            {
                if (tc.isEntryEnabled()) Tr.exit(tc, "prepare", "internalPrepare threw HeuristicHazardException");
                // must rollback any resources now
                try {
                    _txn.internalRollback();
                }
                catch (Throwable t)
                {
                    // swallow any exceptions
                }

                throw new XAException(XAException.XA_HEURHAZ);
            }
            catch (HeuristicRollbackException e)
            {
                if (tc.isEntryEnabled()) Tr.exit(tc, "prepare", "internalPrepare threw HeuristicRollbackException");
                throw new XAException(XAException.XA_HEURRB);
            }
            catch (HeuristicCommitException e)
            {
                if (tc.isEntryEnabled()) Tr.exit(tc, "prepare", "internalPrepare threw HeuristicCommitException");
                throw new XAException(XAException.XA_HEURCOM);
            }
            catch (IllegalStateException e)
            {
                if (tc.isEntryEnabled()) Tr.exit(tc, "prepare", "internalPrepare threw IllegalStateException");
                throw new XAException(XAException.XAER_PROTO);
            }
            catch (SystemException e)
            {
                if (tc.isEntryEnabled()) Tr.exit(tc, "prepare", "internalPrepare threw SystemException");
                throw new XAException(XAException.XAER_RMERR);
            }
            finally
            {
                // suspend the imported tran
                _tranManager.suspend();

                // Resume local transaction if we suspended it earlier
                resume();
            }
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "prepare", "replay not finished");
        throw new XAException(XAException.XAER_RMFAIL);
    }

    /**
     * Informs the object that the transaction is to be committed
     *
     * @exception XAException thrown with the following error codes:
     * <ul><li>XA_HEURMIX - the transaction branch has been heuristically
     * committed and rolled back</li>
     * <li>XA_HEURRB - the transaction branch has been heuristically rolled back</li>
     * <li>XAER_PROTO - the routine was invoked in an inproper context</li>
     * <li>XA_RMERR - a resource manager error has occurred</li></ul>
     *
     * @see javax.transaction.xa.XAException
     */
    public void commit() throws XAException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "commit", _txn);

        // replay must have finished
        if (_tranManager.isReplayComplete())
        {
            final int state = _txn.getTransactionState().getState();

            switch (state)
            {
            case TransactionState.STATE_PREPARED :
                try
                {
                    // Suspend local transaction if present
                    suspend(); // @LIDB2110AA

                    try
                    {
                        // Resume imported transaction
                        _tranManager.resume(_txn);
                    }
                    catch (InvalidTransactionException e)
                    {
                        if (tc.isEntryEnabled()) Tr.exit(tc, "prepare", new Object[] { "resume threw InvalidTransactionException", e });
                        resume();
                        throw new XAException(XAException.XAER_RMERR);
                    }
                    catch (IllegalStateException e)
                    {
                        if (tc.isEntryEnabled()) Tr.exit(tc, "prepare", new Object[] { "resume threw IllegalStateException", e });
                        resume();
                        throw new XAException(XAException.XAER_PROTO);
                    } // @LIDB2110AA

                    _txn.getTransactionState().setState(TransactionState.STATE_COMMITTING);

                    _txn.internalCommit();
                    _txn.notifyCompletion();
                }
                catch (HeuristicMixedException e)
                {
                    _heuristic = StatefulResource.HEURISTIC_MIXED;
                }
                catch (HeuristicHazardException e)
                {
                    _heuristic = StatefulResource.HEURISTIC_HAZARD;
                }
                catch (HeuristicRollbackException e)
                {
                    _heuristic = StatefulResource.HEURISTIC_ROLLBACK;
                }
                catch (SystemException e)
                {
                    if (tc.isEntryEnabled()) Tr.exit(tc, "commit", "internalCommit threw SystemException");
                    throw new XAException(XAException.XAER_RMERR);
                }
                finally // @LIDB2110AA
                {
                    // Resume local transaction if we suspended it earlier
                    resume();
                }       // @LIDB2110AA
                break;

            case TransactionState.STATE_HEURISTIC_ON_ROLLBACK :
            case TransactionState.STATE_HEURISTIC_ON_COMMIT :
                // We had a heuristic on commit or rollback.
                // Let's use whatever that was
                _heuristic = _txn.getResources().getHeuristicOutcome();
                if (tc.isDebugEnabled()) Tr.debug(tc, "Heuristic was: " + ResourceWrapper.printResourceStatus(_heuristic));
                break;

            case TransactionState.STATE_COMMITTED :
            case TransactionState.STATE_COMMITTING :
                // could be a retry, so just accept
                break;

            case TransactionState.STATE_ROLLING_BACK :
            case TransactionState.STATE_ROLLED_BACK :
                _heuristic = StatefulResource.HEURISTIC_ROLLBACK;
                break;

            case TransactionState.STATE_NONE :
                // transaction has completed and has now finished
                break;

            default :
                if (tc.isEntryEnabled()) Tr.exit(tc, "commit", "transaction is not in a prepared state");
                throw new XAException(XAException.XAER_PROTO);
            }

            recordHeuristicOnCommit();
        }
        else
        {
            if (tc.isEntryEnabled()) Tr.exit(tc, "commit", "throwing XAER_RMFAIL");
            throw new XAException(XAException.XAER_RMFAIL);
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "commit");
    }

    protected void recordHeuristicOnCommit() throws XAException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "recordHeuristicOnCommit", this); 

        switch(_heuristic)
        {
        case StatefulResource.NONE:
            break;

        case StatefulResource.HEURISTIC_COMMIT:
            // But we are committing so this is fine.
            // We can discard the txn now.
            if (tc.isDebugEnabled()) Tr.debug(tc, "Swallowing HEURISTIC_COMMIT");
            _txn.notifyCompletion();
            break;
            
        case StatefulResource.HEURISTIC_ROLLBACK:
            if (tc.isEntryEnabled()) Tr.exit(tc, "recordHeuristicOnCommit", "throwing XA_HEURB");
            throw new XAException(XAException.XA_HEURRB);

        case StatefulResource.HEURISTIC_HAZARD:
            if (tc.isEntryEnabled()) Tr.exit(tc, "recordHeuristicOnCommit", "throwing XA_HEURHAZ");
            throw new XAException(XAException.XA_HEURHAZ);
            
        default:
            if (tc.isEntryEnabled()) Tr.exit(tc, "recordHeuristicOnCommit", "throwing XA_HEURMIX");
            throw new XAException(XAException.XA_HEURMIX);
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "recordHeuristicOnCommit");
    }

    /**
     * Informs the object that the transaction is to be committed in one-phase
     *
     * @exception XAException thrown with the following error codes:
     * <ul><li>XA_RBROLLBACK - the transaction has rolled back</li>
     * <li>XA_HEURHAZ - the transaction branch may have been heuristically committed</li>
     * <li>XA_HEURRB - the transaction branch has been heuristically rolled back</li>
     * <li>XAER_PROTO - the routine was invoked in an inproper context</li>
     * <li>XA_RMERR - a resource manager error has occurred</li></ul>
     *
     * @see javax.transaction.xa.XAException
     */
    public void commitOnePhase() throws XAException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "commitOnePhase", _txn);

        final int state = _txn.getTransactionState().getState();

        switch (state)
        {
            case TransactionState.STATE_ACTIVE :
                // Suspend local transaction if present
                suspend();

                try
                {
                    // Resume imported transaction
                    _tranManager.resume(_txn);
                    _txn.prolongFinish();
                    _txn.commit_one_phase();
                    _txn.notifyCompletion();
                }
                catch (RollbackException e)
                {
                    _txn.notifyCompletion();
                    if (tc.isEntryEnabled()) Tr.exit(tc, "commitOnePhase", "commit threw RollbackException");
                    throw new XAException(XAException.XA_RBROLLBACK);
                }
                catch (HeuristicMixedException e)
                {
                    _heuristic = StatefulResource.HEURISTIC_MIXED;
                }
                catch (HeuristicHazardException e)
                {
                    _heuristic = StatefulResource.HEURISTIC_HAZARD;
                }
                catch (HeuristicRollbackException e)
                {
                    _heuristic = StatefulResource.HEURISTIC_ROLLBACK;
                }
                catch (IllegalStateException e)
                {
                    _txn.notifyCompletion();
                    if (tc.isEntryEnabled()) Tr.exit(tc, "commitOnePhase", "commit threw IllegalStateException");
                    throw new XAException(XAException.XAER_PROTO);
                }
                catch (InvalidTransactionException e) // tm.resume
                {
                    if (tc.isEntryEnabled()) Tr.exit(tc, "commitOnePhase", "commit threw InvalidTransactionException");
                    throw new XAException(XAException.XAER_RMERR);
                }
                catch (SystemException e)
                {
                    _txn.notifyCompletion();
                    if (tc.isEntryEnabled()) Tr.exit(tc, "commitOnePhase", "commit threw SystemException");
                    throw new XAException(XAException.XAER_RMERR);
                }
                finally
                {
                    // Resume local transaction if we suspended it earlier
                    resume();
                }

                break;

            case TransactionState.STATE_COMMITTING :
            case TransactionState.STATE_COMMITTED :
                // probably a retry, just accept
                break;

            case TransactionState.STATE_ROLLING_BACK :
            case TransactionState.STATE_ROLLED_BACK :
            case TransactionState.STATE_HEURISTIC_ON_ROLLBACK :
                _heuristic = StatefulResource.HEURISTIC_ROLLBACK;
                break;

            case TransactionState.STATE_HEURISTIC_ON_COMMIT :
                _heuristic = StatefulResource.HEURISTIC_COMMIT;
                break;

            case TransactionState.STATE_NONE :
                // transaction has completed and is now finished
                break;

            default :
                if (tc.isEntryEnabled()) Tr.exit(tc, "commitOnePhase", "transaction is in an incorrect state");
                throw new XAException(XAException.XAER_PROTO);
        }
        
        recordHeuristicOnCommitOnePhase();


        if (tc.isEntryEnabled()) Tr.exit(tc, "commitOnePhase");
    }

    protected void recordHeuristicOnCommitOnePhase() throws XAException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "recordHeuristicOnCommitOnePhase", this);

        switch(_heuristic)
        {
        case StatefulResource.NONE:
            break;

        case StatefulResource.HEURISTIC_ROLLBACK:
            _txn.notifyCompletion();
            if (tc.isEntryEnabled()) Tr.exit(tc, "recordHeuristicOnCommitOnePhase", "heuristic rollback");
            throw new XAException(XAException.XA_RBROLLBACK);
            
        case StatefulResource.HEURISTIC_MIXED:
            if (tc.isEntryEnabled()) Tr.exit(tc, "recordHeuristicOnCommitOnePhase", "heuristic mixed");
            throw new XAException(XAException.XA_HEURMIX);
            
        default:
            if (tc.isEntryEnabled()) Tr.exit(tc, "recordHeuristicOnCommitOnePhase", "heuristic hazard");
            throw new XAException(XAException.XA_HEURHAZ);
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "recordHeuristicOnCommitOnePhase");
    }

    /**
     * Informs the object that the transaction is to be rolled back
     *
     * @exception XAException thrown with the following error codes:
     * <ul><li>XA_HEURMIX - the transaction branch has been committed
     * and heuristically rolled back</li>
     * <li>XAER_PROTO - the routine was invoked in an inproper context</li>
     * <li>XA_RMERR - a resource manager error has occurred</li></ul>
     *
     * @see javax.transaction.xa.XAException
     */
    public synchronized void rollback() throws XAException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "rollback", _txn);

        // If we've been prepared then replay must
        // have finished before we can rollback
        if (_prepared && !_tranManager.isReplayComplete())
        {
            if (tc.isEntryEnabled()) Tr.exit(tc, "rollback", "throwing XAER_RMFAIL(1)");
            throw new XAException(XAException.XAER_RMFAIL);
        }

        final int state = _txn.getTransactionState().getState();

        switch (state)
        {
            case TransactionState.STATE_ACTIVE :
            case TransactionState.STATE_PREPARED :
                try
                {
                    // Suspend local transaction if present
                    suspend(); // @LIDB2110AA

                    try
                    {
                        // Resume imported transaction
                        _tranManager.resume(_txn);
                    }
                    catch (InvalidTransactionException e)
                    {
                        if (tc.isEntryEnabled()) Tr.exit(tc, "prepare", new Object[] { "resume threw InvalidTransactionException", e });
                        resume();
                        throw new XAException(XAException.XAER_RMERR);
                    }
                    catch (IllegalStateException e)
                    {
                        if (tc.isEntryEnabled()) Tr.exit(tc, "prepare", new Object[] { "resume threw IllegalStateException", e });
                        resume();
                        throw new XAException(XAException.XAER_PROTO);
                    } // @LIDB2110AA

                    _txn.getTransactionState().setState(TransactionState.STATE_ROLLING_BACK);
                    _txn.cancelAlarms();
                    _txn.internalRollback();
                    _txn.notifyCompletion();
                }
                catch (IllegalStateException e)
                {
                    if (tc.isEntryEnabled()) Tr.exit(tc, "rollback", "throwing XAER_PROTO(1)");
                    throw new XAException(XAException.XAER_PROTO);
                }
                catch (SystemException e)
                {
                    if (tc.isEntryEnabled()) Tr.exit(tc, "rollback", "throwing XAER_RMERR");
                    throw new XAException(XAException.XAER_RMERR);
                }
                catch (HeuristicMixedException e)
                {
                    _heuristic = StatefulResource.HEURISTIC_MIXED;
                }
                catch (HeuristicHazardException e)
                {
                    _heuristic = StatefulResource.HEURISTIC_HAZARD;
                }
                catch (HeuristicCommitException e)
                {
                    _heuristic = StatefulResource.HEURISTIC_COMMIT;
                }
                finally // @LIDB2110AA
                {
                    // Resume local transaction if we suspended it earlier
                    resume();
                } // @LIDB2110AA

                break;

            case TransactionState.STATE_HEURISTIC_ON_COMMIT :
            case TransactionState.STATE_HEURISTIC_ON_ROLLBACK :
                _heuristic = _txn.getResources().getHeuristicOutcome();
                break;

            case TransactionState.STATE_ROLLING_BACK :
            case TransactionState.STATE_ROLLED_BACK :
                // probably a retry, just accept
                break;

            case TransactionState.STATE_NONE :
                // transaction has completed and is now finished
                break;

            default :
                _txn.notifyCompletion();

                if (tc.isEntryEnabled()) Tr.exit(tc, "rollback", "throwing XAER_PROTO(2)");
                throw new XAException(XAException.XAER_PROTO);
        }

        recordHeuristicOnRollback();

        if (tc.isEntryEnabled()) Tr.exit(tc, "rollback");
    }

    protected void recordHeuristicOnRollback() throws XAException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "recordHeuristicOnRollback", this);

        switch(_heuristic)
        {
        case StatefulResource.NONE:
            break;
            
        case StatefulResource.HEURISTIC_ROLLBACK:
            // But we are rolling back so this is fine.
            // We can discard the txn now.
            if (tc.isDebugEnabled()) Tr.debug(tc, "Swallowing HEURISTIC_ROLLBACK");
            _txn.notifyCompletion();
            break;
            
        case StatefulResource.HEURISTIC_HAZARD:
            if (tc.isEntryEnabled()) Tr.exit(tc, "recordHeuristicOnRollback", "throwing XA_HEURHAZ");
            throw new XAException(XAException.XA_HEURHAZ);

        case StatefulResource.HEURISTIC_COMMIT:
            if (tc.isEntryEnabled()) Tr.exit(tc, "recordHeuristicOnRollback", "throwing XA_HEURCOM");
            throw new XAException(XAException.XA_HEURCOM);

        default:
            if (tc.isEntryEnabled()) Tr.exit(tc, "recordHeuristicOnRollback", "throwing XA_HEURMIX");
            throw new XAException(XAException.XA_HEURMIX);
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "recordHeuristicOnRollback");
    }

    /**
     * Informs the object that the transaction is to be forgotten
     *
     * @exception XAException
     */
    public synchronized void forget() throws XAException
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "forget", _txn);

        // replay must have finished
        if (_tranManager.isReplayComplete())
        {
            _heuristic = StatefulResource.NONE;

            _txn.notifyCompletion();
        }
        else
        {
            if (tc.isEntryEnabled()) Tr.exit(tc, "forget", "throwing XAER_RMFAIL");
            throw new XAException(XAException.XAER_RMFAIL);
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "forget");
    }

    /**
     *  Suspend any transaction context off the thread - save the LTC in the wrapper
     */
    public void suspend()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "suspend");

        if (tc.isEventEnabled()) Tr.event(tc, "suspending (global)");
        _suspendedUOW = _tranManager.suspend();

        if (tc.isEntryEnabled()) Tr.exit(tc, "suspend", _suspendedUOWType);
    }

    /**
     * 
     */
    public void resume()
    {
        if (tc.isEntryEnabled()) Tr.entry(tc, "resume");

        try
        {
            _tranManager.resume((Transaction)_suspendedUOW);
        }
        catch(Exception e)
        {
            FFDCFilter.processException(e, "com.ibm.ws.Transaction.JTA.JCATranWrapperImpl.resume", "733", this);
            if (tc.isDebugEnabled()) Tr.debug(tc, "Failed to resume", new Object[]{_suspendedUOW, e});
        }

        if (tc.isEntryEnabled()) Tr.exit(tc, "resume");
    }

    /**
     * Returns the TransactionImpl object
     *
     * @return The TransactionImpl
     */
    public TransactionImpl getTransaction()
    {
        if (tc.isDebugEnabled()) Tr.debug(tc, "getTransaction", _txn);

        return _txn;
    }

    /**
     * @return
     */
    public boolean isPrepared()
    {
        if (tc.isDebugEnabled()) Tr.debug(tc, "isPrepared", _prepared);

        return _prepared;
    }

    /**
     * @return
     */
    public boolean hasAssociation()
    {
        if (tc.isDebugEnabled()) Tr.debug(tc, "hasAssociation", _associated);

        return _associated;
    }

    /**
     * 
     */
    public void addAssociation()
    {
        _associated = true;
    }

    /**
     * 
     */
    public void removeAssociation()
    {
        if (tc.isDebugEnabled()) Tr.debug(tc, "removeAssociation", _associated);

        _associated = false;
    }
    
    public String toString()
    {
        return _prepared + ":" +
               _associated + ":" +
               _heuristic + ":" +
               _txn.toString();
    }
}