package com.ibm.tx.remote;

/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.util.Hashtable;

import javax.transaction.HeuristicCommitException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.RollbackException;
import javax.transaction.Status;
import javax.transaction.SystemException;

import com.ibm.tx.TranConstants;
import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.embeddable.impl.EmbeddableTimeoutManager;
import com.ibm.tx.jta.embeddable.impl.EmbeddableTranManagerSet;
import com.ibm.tx.jta.embeddable.impl.EmbeddableTransactionImpl;
import com.ibm.tx.jta.impl.TransactionState;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.JTA.HeuristicHazardException;
import com.ibm.ws.Transaction.JTA.StatefulResource;
import com.ibm.ws.Transaction.JTS.ResourceCallback;
import com.ibm.ws.ffdc.FFDCFilter;

/**
 * The TransactionWrapper class provides operations that are defined
 * by the CoordinatorResource and RecoveryCoordinator interfaces.
 * They are implemented in this wrapper class so they can be invoked
 * either from the Corba object implementations or the private JTA2
 * WSCoordinator flows.
 * 
 * A TransactionWrapper is created for each RecoveryCoordinator,
 * CoordinatorResource and WSCoordinatorImpl.TidHolder. So, in the
 * case of Corba interop, if we are in intermediate node, we may have
 * 2 independent wrappers per TransactionImpl. If we are an intermediate
 * when using JTA2 mode, there will be only one wrapper per TransactionImpl
 * and the creation logic in the interceptors check for these cases. If we
 * are in an intermediate node and one side is Corba and the other JTA2, then
 * we will have also have two independent wrappers, one for the Corba object
 * and one for the WSCoordinator object. Cleanup of the owning objects (eg
 * CoordinatorResource or TidHolder) is done by the objects registering a
 * ResourceCallback on the TransactionWrapper. There is only a single
 * callback point and that is destroy. The TransactionWrapper will call
 * this resource callback as part of its own destroy oeration when the
 * TransactionImpl is being forgotten at end of transaction. Note: a
 * transaction may become prolonged in the case of a subordinate returning
 * heuristic, in that either we may have to wait for the superior to complete
 * or the subordinate to retry completion to a resource. The wrappers are not
 * destroyed until the transaction is completely forgotten, even though a
 * RecoveryCoordinator Ris not required after the local subordinate transaction
 * has completed.
 * 
 * The completion methods in this class make use of the TransactionImpl associations
 * support to prevent possible concurrency issues with transaction timeouts and
 * incoming requests and also in-doubt timeouts (ie replay_completion) and
 * the superior polling the subordinate. This is because for on-server we
 * should never be running the same transaction on two or more threads even for
 * loopbacks, server timeouts or client retries. The completion methods are
 * also synchronized in case of retries, where the original request received
 * a comms timeout and it is retried while the original request is still active.
 * Synchronizations occur at this level of implementation as this is the common
 * code for both WSCoordinatorImpl and CoordinatorResourceImpl, etc.
 * 
 */

public final class TransactionWrapper implements ResourceCallback
{
    private static final TraceComponent tc = Tr.register(TransactionWrapper.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private final EmbeddableTransactionImpl _transaction;
    private int _heuristic = StatefulResource.NONE;
    private ResourceCallback _resourceCallback;
    private final int _retryWait = (ConfigurationProviderManager.getConfigurationProvider().getHeuristicRetryInterval() <= 0) ? EmbeddableTransactionImpl.defaultRetryTime : ConfigurationProviderManager.getConfigurationProvider().getHeuristicRetryInterval();

    private static final Hashtable<String, TransactionWrapper> _wrappers = new Hashtable<String, TransactionWrapper>();

    public TransactionWrapper(EmbeddableTransactionImpl transaction)
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "TransactionWrapper", new Object[] { transaction, transaction.getGlobalId() });

        _transaction = transaction;
        _transaction.addDestroyCallback(this);

        _wrappers.put(transaction.getGlobalId(), this);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "TransactionWrapper");
    }

    public static TransactionWrapper getTransactionWrapper(String globalId) {
        return _wrappers.get(globalId);
    }

    /**
     * Requests the prepare phase vote from the object.
     * <p>
     * This uses a private interface to pass the superior Coordinator's prepare request.
     * on to the TransactionImpl that registered the CoordinatorResourceImpl.
     * <p>
     * The result from the TransactionImpl is returned to the caller.
     * 
     * @param
     * 
     * @return The vote.
     * 
     * @exception SystemException The operation failed.
     * @throws RollbackException
     * @exception HeuristicMixed Indicates that a participant voted to roll the
     *                transaction back, but one or more others have already heuristically committed.
     * @exception HeuristicHazard Indicates that a participant voted to roll the
     *                transaction back, but one or more others may have already heuristically
     *                committed.
     * 
     * @see Resource
     */
    //----------------------------------------------------------------------------

    public synchronized Vote prepare() throws SystemException, HeuristicMixedException, HeuristicHazardException, RollbackException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "prepare", this);

        Vote result = Vote.VoteRollback;

        // If there is no associated transaction, raise an execption which should
        // cause the superior transaction to be rolled back.  We should only lose
        // the transaction if we do a destroy, eg on a rollback, or previous prepare
        // vote, etc. in which case we should never be calling prepare again.
        if (_transaction == null)
        {
            final SystemException se = new SystemException(MinorCode.NO_COORDINATOR);

            FFDCFilter.processException(se, "com.ibm.tx.remote.TransactionWrapper.prepare", "157", this);
            Tr.error(tc, "WTRN0001_ERR_INT_ERROR", new Object[] { "prepare", "com.ibm.ws.Transaction.JTS.TransactionWrapper" });
            destroy();
            if (tc.isEntryEnabled())
                Tr.exit(tc, "prepare", se);
            throw se;
        }

        // Ensure timeout cannot rollback the underlying transaction
        ((DistributableTransaction) _transaction).addAssociation();

        if (_transaction.getTransactionState().getState() == TransactionState.STATE_ACTIVE) {
            try {
                // Resume the transaction created from the incoming
                // request so that it is installed on the thread.
                ((EmbeddableTranManagerSet) TransactionManagerFactory.getTransactionManager()).resume(_transaction);

                // Get the TransactionImpl's vote.
                // If the TransactionImpl throws HeuristicMixed, then rethrow.
                // internalPrepare checks the tx state
                final int state = _transaction.internalPrepare();
                switch (state) {
                    case TransactionState.STATE_PREPARED:
                        // Start the in-doubt timer in case we never get a commit/rollback
                        EmbeddableTimeoutManager.setTimeout(_transaction, EmbeddableTimeoutManager.IN_DOUBT_TIMEOUT, _retryWait);
                        result = Vote.VoteCommit;
                        break;
                    case TransactionState.STATE_ROLLED_BACK: // one phase opt
                        result = Vote.VoteRollback;
                        _transaction.notifyCompletion();
                        break;
                    case TransactionState.STATE_COMMITTED:
                        result = Vote.VoteReadOnly;
                        _transaction.notifyCompletion();
                        break;
                    // If the TransactionImpl has voted to roll the transaction back, then this
                    // CoordinatorResourceImpl will not be called again.  Ensure that the
                    // TransactionImpl has rolled back.
                    case TransactionState.STATE_ROLLING_BACK:
                    default:
                        _transaction.internalRollback();
                        result = Vote.VoteRollback;
                        _transaction.notifyCompletion();
                        break;
                }
            } catch (HeuristicMixedException hme) {
                // No FFDC code needed.
                // This exception should not be raised by internalPrepare for a subordinate
                // but may be raised by internalRollback 

                // must rollback any prepared resources ...
                try {
                    _transaction.internalRollback();
                } catch (Throwable t) {
                    FFDCFilter.processException(t, "com.ibm.tx.remote.TransactionWrapper.prepare", "228", this);
                    // swallow any exceptions ...
                }

                ((DistributableTransaction) _transaction).removeAssociation();

                final HeuristicMixedException hm = new HeuristicMixedException();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "prepare", hm);
                throw hm;
            } catch (HeuristicHazardException hhe) {
                // No FFDC code needed.
                // This exception should not be raised by internalPrepare for a subordinate
                // but may be raised by internalRollback 

                // must rollback any prepared resources ...
                try {
                    _transaction.internalRollback();
                } catch (Throwable t) {
                    FFDCFilter.processException(t, "com.ibm.tx.remote.TransactionWrapper.prepare", "250", this);
                    // swallow any exceptions ...
                }

                ((DistributableTransaction) _transaction).removeAssociation();

                final HeuristicHazardException hh = new HeuristicHazardException();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "prepare", hh);
                throw hh;
            } catch (Throwable t) { // IllegalState or javax.transaction.SystemException
                FFDCFilter.processException(t, "com.ibm.tx.remote.TransactionWrapper.prepare", "241", this);
                result = Vote.VoteRollback;
                _transaction.notifyCompletion();
            } finally {
                // suspend any context that is still on the thread
                // NB we may have called notifyCompletion at this point,
                // and suspend calls notifyUOWCallbacks, so if the tx is
                // still on the thread, there could be a problem.
                ((EmbeddableTranManagerSet) TransactionManagerFactory.getTransactionManager()).suspend();
            }
        } else { // probably timed out
            result = Vote.VoteRollback;
        }

        ((DistributableTransaction) _transaction).removeAssociation();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "prepare", result);
        return result;
    }

    /**
     * Informs the object that the transaction is to be committed.
     * <p>
     * Passes the superior Coordinator's commit request on to the TransactionImpl that
     * registered the CoordinatorResourceImpl, using a private interface.
     * <p>
     * If the TransactionImpl does not raise any heuristic exception, the
     * CoordinatorResourceImpl destroys itself.
     * 
     * @param
     * 
     * @return
     * 
     * @exception HeuristicRollback The transaction has already been rolled back.
     * @exception HeuristicMixed At least one participant in the transaction has
     *                rolled back its changes.
     * @exception HeuristicHazard At least one participant in the transaction may
     *                not report its outcome.
     * @exception SystemException An error occurred. The minor code indicates
     *                the reason for the exception.
     * 
     * @see Resource
     */
    //----------------------------------------------------------------------------

    public synchronized void commit() throws HeuristicRollbackException, HeuristicMixedException, HeuristicHazardException, SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "commit", this);

        // Ensure in-doubt timeout cannot complete the underlying transaction
        try {
            ((DistributableTransaction) _transaction).addAssociation();
        } catch (TRANSACTION_ROLLEDBACK ex) {
            // convert this to TRANSIENT as we want caller to retry
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Server busy: throwing TRANSIENT to force retry", ex);
            final TRANSIENT te = new TRANSIENT(MinorCode.SERVER_BUSY, Boolean.FALSE);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "commit", te);
            throw te;
        }

        boolean sysException = false;

        // Cancel any outstanding in-doubt timer
        EmbeddableTimeoutManager.setTimeout(_transaction, EmbeddableTimeoutManager.CANCEL_TIMEOUT, 0);

        final int state = _transaction.getTransactionState().getState();
        switch (state) {
            case TransactionState.STATE_PREPARED:
                try {
                    _transaction.getTransactionState().setState(TransactionState.STATE_COMMITTING);
                } catch (javax.transaction.SystemException se) {
                    FFDCFilter.processException(se, "com.ibm.tx.remote.TransactionWrapper.commit", "354", this);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Exception caught setting state to COMMITTING!", se);
                    sysException = true;
                }

                try {
                    // Resume the transaction created from the incoming
                    // request so that it is installed on the thread.
                    ((EmbeddableTranManagerSet) TransactionManagerFactory.getTransactionManager()).resume(_transaction);

                    _transaction.internalCommit();
                    _transaction.notifyCompletion();
                } catch (HeuristicMixedException hme) {
                    // No FFDC code needed.
                    _heuristic = StatefulResource.HEURISTIC_MIXED;
                } catch (HeuristicHazardException hme) {
                    // No FFDC code needed.
                    _heuristic = StatefulResource.HEURISTIC_HAZARD;
                } catch (HeuristicRollbackException hre) {
                    // No FFDC code needed.
                    _heuristic = StatefulResource.HEURISTIC_ROLLBACK;
                } catch (Throwable exc) { // javax.transaction.SystemException
                    FFDCFilter.processException(exc, "com.ibm.tx.remote.TransactionWrapper.commit", "380", this);
                    Tr.error(tc, "WTRN0068_COMMIT_FAILED", exc);
                    _transaction.notifyCompletion();
                    sysException = true;
                }
                break;

            // If the transaction that this object represents has already been completed,
            // raise a heuristic exception if necessary.  This object must wait for a
            // forget before destroying itself if it returns a heuristic exception.

            case TransactionState.STATE_HEURISTIC_ON_COMMIT:
            case TransactionState.STATE_COMMITTED:
                // Return last heuristic value and allow for recovery
                _heuristic = _transaction.getResources().getHeuristicOutcome();
                break;

            case TransactionState.STATE_COMMITTING:
                // We should only get in this state if we are in recovery and this
                // inbound commit arrives.  In other cases, if we are committing
                // we will hold out using the association counts and if we are
                // locally retrying we will be in a heuristic state as we returned
                // heuristic hazard to the superior.

                ((DistributableTransaction) _transaction).removeAssociation();
                final TRANSIENT tre = new TRANSIENT();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "commit", tre);
                throw tre;

            case TransactionState.STATE_ROLLING_BACK:
            case TransactionState.STATE_HEURISTIC_ON_ROLLBACK:
            case TransactionState.STATE_ROLLED_BACK:
                // Admin heuristic rollback ...
                // again retry ... respond with heurrb
                _heuristic = StatefulResource.HEURISTIC_ROLLBACK;
                break;

            case TransactionState.STATE_NONE:
                // Transaction has completed and is now finished
                // Normally the remoteable object would be disconnected from the orb,
                // but ... timing may mean get got here while it was happenning
                // We could just return ok, but it is more true to return exception
                ((DistributableTransaction) _transaction).removeAssociation();
                final OBJECT_NOT_EXIST one = new OBJECT_NOT_EXIST();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "commit", one);
                throw one;

            default:
                Tr.error(tc, "WTRN0069_COMMIT_BAD_STATE", TransactionState.stateToString(state));
                sysException = true;
                break;
        } // end switch

        ((DistributableTransaction) _transaction).removeAssociation();

        switch (_heuristic) {
            case StatefulResource.NONE:
                break;

            case StatefulResource.HEURISTIC_ROLLBACK:
//                _transaction.addHeuristic();
                final HeuristicRollbackException hr = new HeuristicRollbackException();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "commit", hr);
                throw hr;

            case StatefulResource.HEURISTIC_HAZARD:
//                _transaction.addHeuristic();
                final HeuristicHazardException hh = new HeuristicHazardException();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "commit", hh);
                throw hh;

            default:
//                _transaction.addHeuristic();
                final HeuristicMixedException hm = new HeuristicMixedException();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "commit", hm);
                throw hm;
        }

        if (sysException) {
            final SystemException se = new SystemException();
            if (tc.isEntryEnabled())
                Tr.exit(tc, "commit", se);
            throw se;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "commit");
    }

    /**
     * Informs the object that the transaction is to be committed in one phase.
     * <p>
     * Passes the superior Coordinator's single-phase commit request on to the
     * TransactionImpl that registered the CoordinatorResourceImpl, using a private
     * interface.
     * <p>
     * The result from the TransactionImpl is returned to the caller. If the
     * TransactionImpl did not raise any heuristic exception, the CoordinatorResourceImpl
     * destroys itself.
     * 
     * @param
     * 
     * @return
     * 
     * @exception TRANSACTION_ROLLEDBACK The transaction could not be committed and
     *                has been rolled back.
     * @exception HeuristicHazard One or more resources in the transaction may have
     *                not report its outcome.
     * @exception SystemException An error occurred. The minor code indicates
     *                the reason for the exception.
     * 
     * @see Resource
     */
    //----------------------------------------------------------------------------

    public synchronized void commit_one_phase() throws RollbackException, HeuristicHazardException, SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "commit_one_phase", this);

        // Ensure timeout cannot rollback the underlying transaction
        ((DistributableTransaction) _transaction).addAssociation();

        // If the transaction that this object represents has already been completed,
        // raise an exception if necessary.
        boolean rolledBack = false; // Use locals in case of async completion
        boolean sysException = false;

        final int state = _transaction.getTransactionState().getState();
        switch (state) {
            case TransactionState.STATE_ACTIVE:
                // We have received a commit one phase request
                // from our superior. This transaction should
                // no longer act as a subordinate and should
                // assume superior status prior to beginning
                // commit processing - this is enabled by calling
                // the top-level commit operation.

                // The commit operation may throw the HeuristicHazard exception.  In this case
                // allow it to go back to the caller.  The commit call performs all state checks.
                try {
                    // Resume the transaction created from the incoming
                    // request so that it is installed on the thread.
                    ((EmbeddableTranManagerSet) TransactionManagerFactory.getTransactionManager()).resume(_transaction);

                    // need to prolongFinish to deal with heuristics
                    // as commit_one_phase always calls notifyCompletion
                    _transaction.prolongFinish();
                    _transaction.commit_one_phase();
                    _transaction.notifyCompletion();
                } catch (HeuristicMixedException exc) {
                    // No FFDC code needed.
                    _heuristic = StatefulResource.HEURISTIC_MIXED;
                } catch (HeuristicHazardException exc) {
                    // No FFDC code needed.
                    _heuristic = StatefulResource.HEURISTIC_HAZARD;
                } catch (HeuristicRollbackException exc) {
                    // No FFDC code needed.
                    rolledBack = true;
                    _transaction.notifyCompletion();
                } catch (RollbackException exc) {
                    // No FFDC as rollback is a valid response
                    rolledBack = true;
                    _transaction.notifyCompletion();
                } catch (Throwable exc) { // SecurityException/IllegalStateException/SystemException
                    FFDCFilter.processException(exc, "com.ibm.tx.remote.TransactionWrapper.commit_one_phase", "456", this);
                    Tr.error(tc, "WTRN0070_ONE_PHASE_COMMIT_FAILED", exc);
                    sysException = true;
                    _transaction.notifyCompletion();
                    // destroy();
                }
                break;

            case TransactionState.STATE_COMMITTING:
                // We should only get in this state if the superior failed to get a
                // response on the original commit calls.  We can be in committing
                // state either if we are retrying local resources or in recovery
                // Check the heuristic state and return that.
                _heuristic = _transaction.getResources().getHeuristicOutcome();
                if (_heuristic != StatefulResource.NONE)
                    break;

                // If we are not in any heuristic state then we are not retrying and
                // must be in recovery about to perform the commit. 
                // Continue to return transient until we have a real outcome to return.
                // The superior will consider this as heuristic hazard.  Also the
                // same for LPS state.
            case TransactionState.STATE_LAST_PARTICIPANT:
                ((DistributableTransaction) _transaction).removeAssociation();
                final TRANSIENT tre = new TRANSIENT();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "commit_one_phase", tre);
                throw tre;

            case TransactionState.STATE_COMMITTED:
                // this is probably a retry ... check heuristic state 
                _heuristic = _transaction.getResources().getHeuristicOutcome();
                break;

            case TransactionState.STATE_HEURISTIC_ON_COMMIT:
            case TransactionState.STATE_HEURISTIC_ON_ROLLBACK:
                // Should never get in this state as C1P never gets
                // in this state as it presumes superior status even
                // for admin heuristic commit
                break;

            case TransactionState.STATE_ROLLING_BACK:
            case TransactionState.STATE_ROLLED_BACK:
                // Transaction timed out or admin heuristic rollback or LPS rollback
                // again, probably a retry ... throw exception
                rolledBack = true;
                break;

            case TransactionState.STATE_NONE:
                // Transaction has completed and is now finished
                // Normally the remoteable object would be disconnected from the orb,
                // but ... timing may mean get got here while it was happenning
                ((DistributableTransaction) _transaction).removeAssociation();
                final OBJECT_NOT_EXIST one = new OBJECT_NOT_EXIST();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "commit_one_phase", one);
                throw one;

            default:
                Tr.error(tc, "WTRN0069_COMMIT_BAD_STATE", TransactionState.stateToString(state));
                sysException = true;
                break;
        } // end switch

        ((DistributableTransaction) _transaction).removeAssociation();

        switch (_heuristic)
        {
            case StatefulResource.NONE:
                break;

            default:
//                _transaction.addHeuristic();
                final HeuristicHazardException hh = new HeuristicHazardException();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "commit_one_phase", hh);
                throw hh;
        }

        if (rolledBack)
        {
            final TRANSACTION_ROLLEDBACK tre = new TRANSACTION_ROLLEDBACK(0, Boolean.TRUE);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "commit_one_phase", tre);
            throw tre;
        }
        else if (sysException)
        {
            final INTERNAL ie = new INTERNAL(MinorCode.LOGIC_ERROR, null);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "commit_one_phase", ie);
            throw ie;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "commit_one_phase");
    }

    /**
     * Informs the object that the transaction is to be rolled back.
     * <p>
     * Passes the superior Coordinator's rollback request on to the TransactionImpl
     * that registered the CoordinatorResourceImpl, using a private interface.
     * <p>
     * If the TransactionImpl does not raise any heuristic exception, the
     * CoordinatorResourceImpl destroys itself.
     * 
     * @param
     * 
     * @return
     * 
     * @exception HeuristicCommit The transaction has already been committed.
     * @exception HeuristicMixed At least one participant in the transaction has
     *                committed its changes.
     * @exception HeuristicHazard At least one participant in the transaction may
     *                not report its outcome.
     * @exception SystemException An error occurred. The minor code indicates
     *                the reason for the exception.
     * 
     * @see Resource
     */
    //----------------------------------------------------------------------------

    public synchronized void rollback() throws HeuristicCommitException, HeuristicMixedException, HeuristicHazardException, SystemException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "rollback", this);

        // Ensure timeout cannot rollback the underlying transaction
        ((DistributableTransaction) _transaction).addAssociation();

        boolean sysException = false;

        // Cancel any outstanding (in-doubt or transaction) timer
        EmbeddableTimeoutManager.setTimeout(_transaction, EmbeddableTimeoutManager.CANCEL_TIMEOUT, 0);

        final int state = _transaction.getTransactionState().getState();
        switch (state)
        {
            case TransactionState.STATE_ACTIVE:
            case TransactionState.STATE_PREPARED:
                try
                {
                    _transaction.getTransactionState().setState(TransactionState.STATE_ROLLING_BACK);
                } catch (javax.transaction.SystemException se)
                {
                    FFDCFilter.processException(se, "com.ibm.tx.remote.TransactionWrapper.rollback", "586", this);
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Exception caught setting state to ROLLING_BACK!", se);
                    sysException = true;
                }

                try
                {
                    // Resume the transaction created from the incoming
                    // request so that it is installed on the thread.
                    ((EmbeddableTranManagerSet) TransactionManagerFactory.getTransactionManager()).resume(_transaction);

                    _transaction.internalRollback();
                    _transaction.notifyCompletion();
                } catch (HeuristicMixedException hme)
                {
                    // No FFDC code needed.
                    _heuristic = StatefulResource.HEURISTIC_MIXED;
                } catch (HeuristicHazardException hhe)
                {
                    // No FFDC code needed.
                    _heuristic = StatefulResource.HEURISTIC_HAZARD;
                } catch (HeuristicCommitException hce)
                {
                    // No FFDC code needed.
                    _heuristic = StatefulResource.HEURISTIC_COMMIT;
                } catch (Throwable exc) // javax.transaction.SystemException
                {
                    FFDCFilter.processException(exc, "com.ibm.tx.remote.TransactionWrapper.rollback", "610", this);
                    Tr.error(tc, "WTRN0071_ROLLBACK_FAILED", exc);
                    _transaction.notifyCompletion();
                    sysException = true;
                }
                break;

            // If the transaction that this object represents has already been completed,
            // raise a heuristic exception if necessary.  This object must wait for a
            // forget before destroying itself if it returns a heuristic exception.

            case TransactionState.STATE_HEURISTIC_ON_ROLLBACK:
            case TransactionState.STATE_ROLLED_BACK:
                // Return last heuristic value and allow for recovery
                _heuristic = _transaction.getResources().getHeuristicOutcome();
                break;

            case TransactionState.STATE_ROLLING_BACK:
                // We should only get in this state if we are in recovery and this
                // inbound rollback arrives.  In other cases, if we are rolling back
                // we will hold out using the association counts and if we are
                // locally retrying we will be in a heuristic state as we returned
                // heuristic hazard to the superior.
                ((DistributableTransaction) _transaction).removeAssociation();
                final TRANSIENT tre = new TRANSIENT();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "rollback", tre);
                throw tre;

            case TransactionState.STATE_COMMITTING:
            case TransactionState.STATE_HEURISTIC_ON_COMMIT:
            case TransactionState.STATE_COMMITTED:
                // Admin heuristic commit ...
                // again retry ... respond with heurcom
                _heuristic = StatefulResource.HEURISTIC_COMMIT;
                break;

            case TransactionState.STATE_NONE:
                // Transaction has completed and is now finished
                // Normally the remoteable object would be disconnected from the orb,
                // but ... timing may mean get got here while it was happenning
                // We could just return ok, but it is more true to return exception
                ((DistributableTransaction) _transaction).removeAssociation();
                final OBJECT_NOT_EXIST one = new OBJECT_NOT_EXIST();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "rollback", one);
                throw one;

            default:
                Tr.error(tc, "WTRN0072_ROLLBACK_BAD_STATE", TransactionState.stateToString(state));
                sysException = true;
                _transaction.notifyCompletion();
                break;
        } // end switch

        ((DistributableTransaction) _transaction).removeAssociation();

        switch (_heuristic)
        {
            case StatefulResource.NONE:
                break;

            case StatefulResource.HEURISTIC_HAZARD:
//                _transaction.addHeuristic();
                final HeuristicHazardException hh = new HeuristicHazardException();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "rollback", hh);
                throw hh;

            case StatefulResource.HEURISTIC_COMMIT:
//                _transaction.addHeuristic();
                final HeuristicCommitException hc = new HeuristicCommitException();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "rollback", hc);
                throw hc;

            default:
//                _transaction.addHeuristic();
                final HeuristicMixedException hm = new HeuristicMixedException();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "rollback", hm);
                throw hm;
        }

        if (sysException)
        {
            // destroy();
            final INTERNAL ie = new INTERNAL(MinorCode.LOGIC_ERROR, null);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "rollback", ie);
            throw ie;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "rollback");
    }

    /**
     * Informs the object that the transaction is to be forgotten.
     * <p>
     * Informs the CoordinatorResourceImpl that it does not need to retain heuristic
     * information any longer. Note: this code requires the superior to call forget.
     * If we return a heuristic, we could start an indoubt timer to ensure we clean
     * up in case the superior dies prior to a forget. Note: we do poll on recovery
     * but not in normal running.
     * 
     * 
     * @param
     * 
     * @return
     * 
     * @exception SystemException An error occurred. The minor code indicates
     *                the reason for the exception.
     * 
     * @see Resource
     */
    //----------------------------------------------------------------------------

    public synchronized void forget() throws SystemException
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "forget", this);

        _transaction.notifyCompletion();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "forget");
    }

    /**
     * Requests replay completion from the object.
     * <p>
     * This uses a private interface to pass the subordinate Coordinator's request
     * to replay completion on the superior TransactionImpl that the subordinate
     * CoordinatorResource registered with. We do not pass the Resource object
     * through as it is not required.
     * <p>
     * The status of the TransactionImpl is returned to the caller after converting
     * it to a Corba status. In the case where we can determine that the superior
     * has only a single resource and will have issued commit_one_phase, we return
     * OBJECT_NOT_EXIST as the effect of commit_one_phase is to delegate all
     * completion responsiblity to the resource (ie subordinate). Note: this
     * test is limited in that it does not check for read-only votes.
     * 
     * @param
     * 
     * @return The status.
     * 
     * @exception NotPrepared The transaction is not yet prepared.
     * 
     * @see RecoveryCoordinator
     */
    //----------------------------------------------------------------------------

    public int replay_completion() throws NotPrepared
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "replay_completion");

        int result = Status.STATUS_ROLLEDBACK;

        final int status = _transaction.getStatus();

        final int numberOfResources = _transaction.getResources().numRegistered();

        if (tc.isDebugEnabled())
            Tr.debug(tc, "replay_completion status:" + status);

        switch (status)
        {
        // If the transaction is still active, raise the NotPrepared exception.
        // The transaction must be marked rollback-only at this point because we
        // cannot allow the transaction to complete if a participant has failed.
            case javax.transaction.Status.STATUS_ACTIVE:
                try
                {
                    _transaction.setRollbackOnly();
                } catch (Throwable exc)
                {
                    FFDCFilter.processException(exc, "com.ibm.tx.remote.TransactionWrapper.replay_completion", "171", this);
                    if (tc.isEventEnabled())
                        Tr.event(tc, "replay_completion caught exception setting coordinator rollback_only", exc);
                }
            case javax.transaction.Status.STATUS_MARKED_ROLLBACK:
                final NotPrepared npe = new NotPrepared();
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "replay_completion", npe);
                throw npe;

                // If the transaction is prepared, the caller must wait for the
                // Coordinator to tell it what to do, so return an unknown status, and
                // do nothing.  Note that if this Coordinator is sitting waiting for
                // its superior, this could take a long time.
            case javax.transaction.Status.STATUS_PREPARED:
                result = Status.STATUS_UNKNOWN;
                break;

            case javax.transaction.Status.STATUS_PREPARING:
                if (numberOfResources == 1)
                {
                    // There is only 1 resource registered and it is the caller so
                    // we will be issuing commit_one_phase to it, ie will have delegated
                    // responsibility to it to complete the txn, so we "no longer exist"
                    // as part of the transaction, so raise object_not_exist.
                    final OBJECT_NOT_EXIST one = new OBJECT_NOT_EXIST();
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "replay_completion", one);
                    throw one;
                }
                result = Status.STATUS_UNKNOWN;
                break;

            // If the transaction has been committed, the caller will receive a commit.
            case javax.transaction.Status.STATUS_COMMITTING:
                result = Status.STATUS_COMMITTING;
                break;

            case javax.transaction.Status.STATUS_COMMITTED:
                if (numberOfResources == 1)
                {
                    // There is only 1 resource registered and it is the caller so
                    // we have issued commit_one_phase to it, ie will have delegated
                    // responsibility to it to complete the txn, so we "no longer exist"
                    // as part of the transaction, so raise object_not_exist.
                    final OBJECT_NOT_EXIST one = new OBJECT_NOT_EXIST();
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "replay_completion", one);
                    throw one;
                }
                result = Status.STATUS_COMMITTED;
                break;

            case javax.transaction.Status.STATUS_NO_TRANSACTION:
                result = Status.STATUS_NO_TRANSACTION;
                break;

            case javax.transaction.Status.STATUS_ROLLEDBACK:
                if (numberOfResources == 1)
                {
                    // There is only 1 resource registered and it is the caller so
                    // we have issued commit_one_phase to it, ie will have delegated
                    // responsibility to it to complete the txn, so we "no longer exist"
                    // as part of the transaction, so raise object_not_exist.
                    final OBJECT_NOT_EXIST one = new OBJECT_NOT_EXIST();
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "replay_completion", one);
                    throw one;
                }

                // In any other situation, assume that the transaction has been rolled
                // back. As there is a Coordinator, it will direct the Resource to roll
                // back.
            default:
                result = Status.STATUS_ROLLEDBACK;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "replay_completion", result);
        return result;
    }

    /**
     * Set the transaction to be rollback_only.
     * <p>
     * This uses a private interface to pass the subordinate Coordinator's request
     * to rollback_only on the superior TransactionImpl that the subordinate
     * CoordinatorResource registered with.
     */
    //----------------------------------------------------------------------------

    public void rollback_only()
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "rollback_only");

        try
        {
            _transaction.setRollbackOnly();
            _transaction.subRollback();
        } catch (Throwable exc)
        {
            FFDCFilter.processException(exc, "com.ibm.tx.remote.TransactionWrapper.rollback_only", "813", this);
            if (tc.isEventEnabled())
                Tr.event(tc, "rollback_only caught exception setting coordinator rollback_only", exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "rollback_only");
    }

    public EmbeddableTransactionImpl getTransaction()
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getTransaction");

        return _transaction;
    }

    public void setResourceCallback(ResourceCallback callback)
    {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setResourceCallback");

        _resourceCallback = callback;
    }

    // This is the afterFinished/destroy callback.  
    // This method used to be synchronized, but was changed for APAR PK20881 as
    // it was found to cause deadlocks if two threads on the same server tried
    // to rollback the same transaction at the same time. This situation could
    // arise if a client inactivity timeout occured on a server at the same time
    // as another server tried to rollback the transaction.
    @Override
    public void destroy() // PK20881
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "destroy");

        // Dummy transactionWrappers may not be in any table and so
        // will not have a resourceCallback registered to remove them.
        if (_resourceCallback != null)
            _resourceCallback.destroy();

        _wrappers.remove(_transaction.getGlobalId());

        //      Do not remove connection with the TransactionImpl.  This will delay garbage
//      collection until the TransactionWrapper is garbage collected.
//      There is a window when an incoming request can access the remoteable object
//      (ie WSCoordinator or CoordinatorResource) and get access to the TransactionWrapper
//      while destroy() is called by another thread as the synchronization is on the
//      TransactionWrapper.  When the incoming request gets control, it will find that
//      _transaction is null.  Rather than check for this case, we leave the connection
//      to the TransactionImpl and its associated TransactionState.  The code above will
//      then check the transaction state and respond appropriately.  These checks are
//      already required as the superior may retry requests, etc.
//        _transaction = null;

        if (tc.isEntryEnabled())
            Tr.exit(tc, "destroy");
    }
}
