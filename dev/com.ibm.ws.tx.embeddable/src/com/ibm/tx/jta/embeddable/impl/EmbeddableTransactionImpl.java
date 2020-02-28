/*******************************************************************************
 * Copyright (c) 2009, 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.tx.jta.embeddable.impl;

import java.io.Serializable;
import java.util.Stack;

import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.xa.Xid;

import com.ibm.ejs.ras.Tr;
import com.ibm.ejs.ras.TraceComponent;
import com.ibm.tx.TranConstants;
import com.ibm.tx.config.ConfigurationProvider;
import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.embeddable.EmbeddableTransactionManagerFactory;
import com.ibm.tx.jta.impl.JCARecoveryData;
import com.ibm.tx.jta.impl.TransactionImpl;
import com.ibm.tx.jta.impl.TransactionState;
import com.ibm.tx.jta.impl.TxPrimaryKey;
import com.ibm.tx.jta.impl.XidImpl;
import com.ibm.tx.remote.DistributableTransaction;
import com.ibm.tx.remote.TRANSACTION_ROLLEDBACK;
import com.ibm.tx.remote.TransactionWrapper;
import com.ibm.tx.util.TMHelper;
import com.ibm.websphere.uow.UOWSynchronizationRegistry;
import com.ibm.ws.Transaction.JTA.Util;
import com.ibm.ws.Transaction.JTS.Configuration;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.recoverylog.spi.RecoverableUnit;
import com.ibm.ws.recoverylog.spi.RecoverableUnitSection;
import com.ibm.ws.tx.embeddable.EmbeddableWebSphereTransactionManager;
import com.ibm.ws.uow.embeddable.SynchronizationRegistryUOWScope;

public class EmbeddableTransactionImpl extends com.ibm.tx.jta.impl.TransactionImpl implements SynchronizationRegistryUOWScope, DistributableTransaction {
    private static final TraceComponent tc = Tr.register(EmbeddableTransactionImpl.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    protected final int _inactivityTimeout = _configProvider.getClientInactivityTimeout();

    protected EmbeddableWebSphereTransactionManager.InactivityTimer _inactivityTimer;

    protected boolean _inactivityTimerActive;

    protected Stack<Thread> _mostRecentThread = new Stack<Thread>();

    // Counters to track when this context is 'on server' and 'downstream'
    private int _activeAssociations;
    private int _suspendedAssociations;

    private String _globalId;

    // Used for WSAT protocol
    private WSATRecoveryCoordinator _wsatRC;

    private int _retryWait = (_configProvider.getHeuristicRetryInterval() <= 0) ? defaultRetryTime : _configProvider.getHeuristicRetryInterval();

    private Thread _thread;

    public EmbeddableTransactionImpl() {
        super();
    }

    public EmbeddableTransactionImpl(int timeout, Xid xid, JCARecoveryData jcard) {
        super(timeout, xid, jcard);
    }

    public EmbeddableTransactionImpl(EmbeddableFailureScopeController fsc) {
        super(fsc);
    }

    public EmbeddableTransactionImpl(int txType, int timeout) {
        super(txType, timeout);
    }

    public EmbeddableTransactionImpl(int timeout) {
        super(timeout);

        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEventEnabled())
            Tr.event(tc, "EmbeddableTransaction BEGIN occurred for TX: " + _localTID);
        _activeAssociations++; // created locally and is 'on server'
        setThread(Thread.currentThread());
        updateMostRecentThread();
    }

    /**
     * Constructor when TransactionImpl is created
     * on importing a transaction from a remote server.
     * via WS-AT
     *
     * @param timeout   Transaction timeout in seconds
     * @param globalTID The imported identifier
     */
    public EmbeddableTransactionImpl(int timeout, String globalID) throws SystemException {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "EmbeddableTransactionImpl", new Object[] { timeout, globalID });

        _failureScopeController = Configuration.getFailureScopeController();

        _subordinate = true; /* @LI3187M */

        // Set this BEFORE initializeTran()
        _globalId = globalID;

        final TxPrimaryKey pk = initializeTran(timeout);
        _xid = new XidImpl(pk);

        if (traceOn) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "EmbeddableTransactionImpl", this);
        }
    }

    @Override
    protected void initialize(int timeout) {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEventEnabled())
            Tr.event(tc, "(SPI) Transaction BEGIN occurred for TX: " + _localTID);

        // Let the failure scope controller know about this transaction so that it can
        // examine all its associated transaction when its asked to shutdown (either at
        // server shutdown or peer recovery termination)
        if (_failureScopeController != null) {
            _failureScopeController.registerTransaction(this, false);

            //
            // Determine if this transaction was created prior to the recovery log becoming available.
            // If this is the case then this transaction will only ever be able to support 1PC
            // transactions and hense only allow a single resource to be enlisted. The reason for this
            // concerns the APPLID. The APPLID is used in the creation of XIDs in order that websphere
            // can distinguish (on xa_recover) between those it created and those other TMs created.
            // In the HA world, the recovery log is not necessararly immediatly avilable when the
            // server starts. As a result, we allow 1PC work without the logs and are forced to
            // to prevent 2PC work until they are made available by the HA framework. Before the
            // logs are available, XIDs created by the server use the new default (unique, generated
            // by the server run) APPLID. When the logs are processed, the APPLID is retrieved from
            // the log and used in future generation of XIDs. Since the log stores only a single
            // APPLID at present all recoverable work must use this XID as its the only one that
            // the transaction service will recognize during xa_recover processing. We can't allow
            // 2PC interaction with the resources under an XID with a temporary APPLID.
            //
            if (_failureScopeController.getRecoveryManager() == null) {
                _disableTwoPhase = true;
            }
        } else {
            _disableTwoPhase = true;
        }

        if (traceOn && tc.isDebugEnabled() && _disableTwoPhase)
            Tr.debug(tc, "No recovery log is currently available. Transaction can only support 1PC protocol");

        //
        // Create our transaction state object
        // which automatically starts in ACTIVE
        // state.
        //
        _status = new EmbeddableTransactionState(this);

        // LIDB1673.7.2: check for maximum timeout
        final int maximumTransactionTimeout = _configProvider.getMaximumTransactionTimeout();
        if (maximumTransactionTimeout > 0) {
            if (timeout > maximumTransactionTimeout || timeout == 0) {
                if (traceOn && tc.isEventEnabled())
                    Tr.event(tc, "Timeout limited by maximumTransactionTimeout");
                timeout = maximumTransactionTimeout;
            }
        }

        //
        // If we have a timeout we need to add ourselves to
        // the manager so the thread is started.
        //
        if (timeout > 0) {
            _timeout = timeout;
            EmbeddableTimeoutManager.setTimeout(this, EmbeddableTimeoutManager.ACTIVE_TIMEOUT, timeout);
        }
    }

    public boolean startInactivityTimer(EmbeddableWebSphereTransactionManager.InactivityTimer inactivityTimer) {
        _inactivityTimer = inactivityTimer;

        final boolean timerStarted = startInactivityTimer();

        if (!timerStarted) {
            _inactivityTimer = null;
        }

        return _inactivityTimerActive;
    }

    /**
     * Start an inactivity timer and call alarm method of parameter when
     * timeout expires.
     *
     * @param iat callback object to be notified when timer expires.
     *                This may be null.
     * @exception SystemException thrown if transaction is not active
     */
    public boolean startInactivityTimer() {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "startInactivityTimer");

        if (_inactivityTimeout > 0
            && _status.getState() == TransactionState.STATE_ACTIVE
            && !_inactivityTimerActive) {
            EmbeddableTimeoutManager.setTimeout(this,
                                                EmbeddableTimeoutManager.INACTIVITY_TIMEOUT, _inactivityTimeout);
            _inactivityTimerActive = true;

            _mostRecentThread.pop();
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "startInactivityTimer", _inactivityTimerActive);
        return _inactivityTimerActive;
    }

    /**
     * Rollback all resources, but do not drive state changes.
     * Used when transaction HAS TIMED OUT.
     */
    public void rollbackResources() {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "rollbackResources");

        try {
            final Transaction t = ((EmbeddableTranManagerSet) TransactionManagerFactory.getTransactionManager()).suspend();
            getResources().rollbackResources();
            if (t != null)
                ((EmbeddableTranManagerSet) TransactionManagerFactory.getTransactionManager()).resume(t);
        } catch (Exception ex) {
            FFDCFilter.processException(ex, "com.ibm.tx.jta.impl.EmbeddableTransactionImpl.rollbackResources", "104", this);
            if (traceOn && tc.isDebugEnabled())
                Tr.debug(tc, "Exception caught from rollbackResources()", ex);
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "rollbackResources");
    }

    @Override
    public EmbeddableRegisteredResources getResources() {
        if (_resources == null) {
            _resources = new EmbeddableRegisteredResources(this, _disableTwoPhase);
        }

        return (EmbeddableRegisteredResources) _resources;
    }

    /**
     * Stop inactivity timer associated with transaction.
     * This method needs to be synchronized to serialize with inactivity
     * timeout. If the timeout runs after this method, then there will
     * be no _inactivityTimer to call and the context will be on_server.
     * If the timeout runs before, then a subsequent resume will fail
     * as the transaction will be rolled back.
     *
     */
    public synchronized void stopInactivityTimer() {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "stopInactivityTimer");

        if (_inactivityTimerActive) {
            _inactivityTimerActive = false;
            EmbeddableTimeoutManager.setTimeout(this, EmbeddableTimeoutManager.INACTIVITY_TIMEOUT, 0);
        }

        // The inactivity timer's being stopped so the transaction is
        // back on-server. Push the thread that it's running on onto
        // the stack.
        _mostRecentThread.push(Thread.currentThread());

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "stopInactivityTimer");
    }

    //
    // Distribute the after completion callbacks.
    //
    // This method is called inline by commit/rollback at the end of the transaction.
    //
    @Override
    protected void distributeAfter(int status) throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "distributeAfter", status);

        // Take the transaction off the thread to allow container
        // mediated dispatches on components requiring a tx from
        // callbacks.
        EmbeddableTransactionManagerFactory.getTransactionManager().suspend(); //LIDB2775-103

        if (_syncs != null) {
            _syncs.distributeAfter(status);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "distributeAfter");
    }

    @Override
    public int getUOWType() {
        return UOWSynchronizationRegistry.UOW_TYPE_GLOBAL_TRANSACTION;
    }

    @Override
    public int getUOWStatus() {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "getUOWStatus", this);

        final int status = getStatus();
        final int uowStatus;

        switch (status) {
            case Status.STATUS_ACTIVE: {
                uowStatus = UOWSynchronizationRegistry.UOW_STATUS_ACTIVE;
                break;
            }
            case Status.STATUS_MARKED_ROLLBACK: {
                uowStatus = UOWSynchronizationRegistry.UOW_STATUS_ROLLBACKONLY;
                break;
            }
            case Status.STATUS_COMMITTING:
            case Status.STATUS_PREPARED:
            case Status.STATUS_PREPARING:
            case Status.STATUS_ROLLING_BACK: {
                uowStatus = UOWSynchronizationRegistry.UOW_STATUS_COMPLETING;
                break;
            }
            case Status.STATUS_COMMITTED: {
                uowStatus = UOWSynchronizationRegistry.UOW_STATUS_COMMITTED;
                break;
            }
            case Status.STATUS_ROLLEDBACK: {
                uowStatus = UOWSynchronizationRegistry.UOW_STATUS_ROLLEDBACK;
                break;
            }
            default: {
                if (traceOn && tc.isEntryEnabled())
                    Tr.exit(tc, "getUOWStatus", "IllegalStateException");
                throw new IllegalStateException();
            }
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "getUOWStatus", uowStatus);
        return uowStatus;
    }

    protected void updateMostRecentThread() {
        _mostRecentThread.push(Thread.currentThread());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.remote.DistributableTransaction#suspendAssociation()
     */

    /**
     * Called by interceptor when an outgoing request is sent.
     * This updates the server association counts for this context.
     */
    @Override
    public void suspendAssociation() {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "suspendAssociation");

        suspendAssociation(false);

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "suspendAssociation");
    }

    /**
     * Called by interceptor when an outgoing request is sent.
     * This updates the server association counts for this context.
     */
    public synchronized void suspendAssociation(boolean notify) {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "suspendAssociation",
                     new Object[] { notify, _activeAssociations, _suspendedAssociations, _thread != null ? String.format("%08X", _thread.getId()) : "Not on a thread" });

        _suspendedAssociations++;

        if (notify)
            notifyAll();

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "suspendAssociation",
                    new Object[] { _activeAssociations, _suspendedAssociations, _thread != null ? String.format("%08X", _thread.getId()) : "Not on a thread" });
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.remote.DistributableTransaction#resumeAssociation()
     */

    /**
     * Called by interceptor when incoming reply arrives.
     * This polices the single threaded operation of the transaction.
     */
    @Override
    public void resumeAssociation() {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "resumeAssociation",
                     new Object[] { _activeAssociations, _suspendedAssociations, _thread != null ? String.format("%08X", _thread.getId()) : "Not on a thread" });

        resumeAssociation(true);

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "resumeAssociation",
                    new Object[] { _activeAssociations, _suspendedAssociations, _thread != null ? String.format("%08X", _thread.getId()) : "Not on a thread" });
    }

    /**
     * This polices the single threaded operation of the transaction.
     * allowSetRollback indicates whether the condition where there is already an
     * active association should result in rolling back the transaction.
     *
     * In the standard case of a client interceptor attempting to resume the association between
     * a transaction and the thread as part of response processing allowSetRollback is set to true
     * - this means that if the transaction already has an active association with another thread
     * the transaction is marked for rollback and an exception is thrown, even though we still wait
     * to give the thread exclusive access to the transaction. This was the pre-existing behaviour
     * before APAR PI13992
     *
     * If another component is temporarily suspending+resuming while waiting for some other condition
     * we simply want to wait to allow the thread exclusive access to the transaction - in this case the
     * method should be called with allowSetRollback set to false - in this case the method waits to grant
     * the thread exclusive access to the transaction and does NOT set the transaction to rollback only
     * if the transaction is currently actively associated with another thread.
     */
    public synchronized void resumeAssociation(boolean allowSetRollback) throws TRANSACTION_ROLLEDBACK {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "resumeAssociation",
                     new Object[] { allowSetRollback, _activeAssociations, _suspendedAssociations, _thread != null ? String.format("%08X", _thread.getId()) : "Not on a thread" });

        // if another thread is active we have to wait
        // doSetRollback indicates if this method has marked the transaction for rollbackOnly
        // and if so TRANSACTION_ROLLEDBACK exception is thrown.
        boolean doSetRollback = false;
        while (_activeAssociations > _suspendedAssociations) {
            doSetRollback = allowSetRollback;
            try {
                if (doSetRollback && !_rollbackOnly)
                    setRollbackOnly();
            } catch (Exception ex) {
                FFDCFilter.processException(ex, "com.ibm.ws.Transaction.JTA.TransactionImpl.resumeAssociation", "1748", this);
                if (traceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "setRollbackOnly threw exception", ex);
                // swallow this exception
            }

            try {
                wait();
            } // woken up by removeAssociation
            catch (InterruptedException iex) { /* no ffdc */
            }

        } // end while

        _suspendedAssociations--;
        if (doSetRollback) {
            final TRANSACTION_ROLLEDBACK trb = new TRANSACTION_ROLLEDBACK("Context already active");
            if (traceOn && tc.isEntryEnabled())
                Tr.exit(tc, "resumeAssociation throwing rolledback",
                        new Object[] { _activeAssociations, _suspendedAssociations, _thread != null ? String.format("%08X", _thread.getId()) : "Not on a thread", trb });
            throw trb;
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "resumeAssociation",
                    new Object[] { _activeAssociations, _suspendedAssociations, _thread != null ? String.format("%08X", _thread.getId()) : "Not on a thread" });
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.remote.DistributableTransaction#getGlobalId()
     */
    @Override
    public String getGlobalId() {

        if (_globalId == null) {
            _globalId = Util.toHexString(getXid().getGlobalTransactionId());
        }

        return _globalId;
    }

    public boolean isResumable() {
        if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
            Tr.debug(tc, "isResumable", new Object[] { this, _thread == null || _suspendedAssociations >= _activeAssociations });
        return (_thread == null || _suspendedAssociations >= _activeAssociations);
    }

    /**
     * Called by interceptor when incoming request arrives.
     * This polices the single threaded operation of the transaction.
     */
    @Override
    public synchronized void addAssociation() {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "addAssociation",
                     new Object[] { _activeAssociations, _suspendedAssociations, _thread != null ? String.format("%08X", _thread.getId()) : "Not on a thread" });

        if (_activeAssociations > _suspendedAssociations) {
            if (traceOn && tc.isDebugEnabled())
                Tr.debug(tc, "addAssociation received incoming request for active context");
            try {
                setRollbackOnly();
            } catch (Exception ex) {
                FFDCFilter.processException(ex, "com.ibm.ws.Transaction.JTA.TransactionImpl.addAssociation", "1701", this);
                if (traceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "setRollbackOnly threw exception", ex);
                // swallow this exception
            }

            final TRANSACTION_ROLLEDBACK trb = new TRANSACTION_ROLLEDBACK("Context already active");
            if (traceOn && tc.isEntryEnabled())
                Tr.exit(tc, "addAssociation throwing rolledback",
                        new Object[] { _activeAssociations, _suspendedAssociations, _thread != null ? String.format("%08X", _thread.getId()) : "Not on a thread", trb });
            throw trb;
        }

        stopInactivityTimer();
        _activeAssociations++;

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "addAssociation",
                    new Object[] { _activeAssociations, _suspendedAssociations, _thread != null ? String.format("%08X", _thread.getId()) : "Not on a thread" });
    }

    /**
     * Called by interceptor when reply is sent.
     * This updates the server association count for this context.
     */
    @Override
    public synchronized void removeAssociation() {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "removeAssociation",
                     new Object[] { _activeAssociations, _suspendedAssociations, _thread != null ? String.format("%08X", _thread.getId()) : "Not on a thread" });

        _activeAssociations--;

        if (_activeAssociations <= 0) {
            startInactivityTimer();
        } else {
            _mostRecentThread.pop();
        }

        notifyAll(); //LIDB1673.23

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "removeAssociation",
                    new Object[] { _activeAssociations, _suspendedAssociations, _thread != null ? String.format("%08X", _thread.getId()) : "Not on a thread" });
    }

    /**
     * Enlist an asynchronous resource with the target TransactionImpl object.
     * A WSATParticipantWrapper is typically a representation of a downstream WSAT
     * subordinate server.
     *
     * @param asyncResource the remote WSATParticipantWrapper
     */
    @Override
    public void enlistAsyncResource(String xaResFactoryFilter, Serializable xaResInfo, Xid xid) throws SystemException // @LIDB1922-5C
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "enlistAsyncResource", new Object[] { "(SPI): args: ", xaResFactoryFilter, xaResInfo, xid });
        try {
            final WSATAsyncResource res = new WSATAsyncResource(xaResFactoryFilter, xaResInfo, xid);
            final WSATParticipantWrapper wrapper = new WSATParticipantWrapper(res);
            getResources().addAsyncResource(wrapper);
        } finally {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "enlistAsyncResource", "(SPI)");
        }
    }

    /**
     * Called on the timeout thread.
     * If the context is not on this server then rollback the transaction
     * else just mark it for rollback.
     */
    @Override
    public synchronized void timeoutTransaction(boolean initial) {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "timeoutTransaction", initial);

        if (traceOn && tc.isEventEnabled())
            Tr.event(tc, "(SPI) Transaction TIMEOUT occurred for TX: " + getLocalTID());

        _timedOut = true; // mark

        if (initial) // initial timeout
        {
            _rollbackOnly = true;

            abortTransactionParticipants();

            // inactivity timeout may have happened ... check status
            if (_status.getState() == TransactionState.STATE_ACTIVE) {
                // If there is no txn timeout, or we are into completion phase the timeout setting may be zero
                if (_timeout == 0)
                    _timeout = 10;
                EmbeddableTimeoutManager.setTimeout(this, EmbeddableTimeoutManager.REPEAT_TIMEOUT, _timeout);

                // d369039 only rollback if still in ACTIVE state
                if (_activeAssociations <= 0) {
                    rollbackResources();
                }
            }
        } else if (_activeAssociations <= 0) // off server ... do the rollback
        {
            final EmbeddableTranManagerSet tranManager = (EmbeddableTranManagerSet) TransactionManagerFactory.getTransactionManager();

            boolean resumed = false;
            try {
                tranManager.resume(this);
                resumed = true;
            } catch (Throwable t) {
                FFDCFilter.processException(t, "com.ibm.ws.tx.jta.TransactionImpl.timeoutTransaction", "1311", this);
                if (traceOn && tc.isDebugEnabled())
                    Tr.debug(tc, "timeoutTransaction resume threw exception", t);
                // swallow this exception
            }

            if (resumed) {
                boolean rolledback = false;
                try {
                    tranManager.rollback();
                    rolledback = true;
                } catch (Throwable t) {
                    FFDCFilter.processException(t, "com.ibm.ws.tx.jta.TransactionImpl.timeoutTransaction", "1326", this);
                    if (traceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "timeoutTransaction rollback threw exception", t);
                    // swallow this exception
                } finally {
                    if (!rolledback) {
                        tranManager.suspend();
                    }
                }
            }
        } else // on server ... just re-schedule timeout
        {
            _rollbackOnly = true; // for the case of server quiesce?
            // inactivity timeout may have happened ... check status
            if (_status.getState() == TransactionState.STATE_ACTIVE) {
                // If there is no txn timeout, or we are into completion phase the timeout setting may be zero
                if (_timeout == 0)
                    _timeout = 10;
                EmbeddableTimeoutManager.setTimeout(this, EmbeddableTimeoutManager.REPEAT_TIMEOUT, _timeout);
            }
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "timeoutTransaction");
    }

    /**
     * Directs the TransactionImpl to perform recovery actions based on its
     * reconstructed state after a failure, or after an in-doubt timeout has
     * occurred.
     * This method is called by the RecoveryManager during recovery, in which case
     * there is no terminator object, or during normal operation if the transaction
     * commit retry interval has been exceeded for the transaction.
     * If this method is called more times than the retry limit specified in
     * COMMITRETRY, then the global outcome of the transaction is taken from the
     * value of HEURISTICDIRECTION.
     *
     * This method is synchronized together with the Associations methods as it
     * needs to support concurrency between inbound requests and in-doubt timer
     * activity. On recovery, there will be no associations for non-subordinates.
     *
     * @return
     */
    @Override
    synchronized public void recover() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "recover", this);

        if (_activeAssociations <= 0) {
            final int state = _status.getState();
            if (_subordinate) {
                // For a subordinate, first check whether the global outcome is known locally.
                switch (state) {
                    // Due to the possibility of recovery being attempted asynchronously to
                    // an incoming superior request, we must cover the case where the
                    // transaction has now actually committed already.
                    case TransactionState.STATE_HEURISTIC_ON_COMMIT:
                    case TransactionState.STATE_COMMITTED:
                    case TransactionState.STATE_COMMITTING:
                        recoverCommit(true);
                        break;
                    // Due to the possibility of recovery being attempted asynchronously to
                    // an incoming superior request, we must cover the case where the
                    // transaction has now actually rolled back already.
                    case TransactionState.STATE_HEURISTIC_ON_ROLLBACK:
                    case TransactionState.STATE_ROLLED_BACK:
                    case TransactionState.STATE_ROLLING_BACK:
                        recoverRollback(true);
                        break;
                    // For a subordinate, the replay_completion method is invoked on the superior.
                    // If the number of times the replay_completion has been retried is greater
                    // than the value specified by COMMITRETRY, then HEURISTICDIRECTION is used
                    // to determine the transaction outcome.
                    default:
                        // If we were imported from a JCA provider, check whether it's still installed.
                        // If so, we need do nothing here since we expect the RA to complete the transaction.
                        // Otherwise, we will complete using the configured direction.
                        if (_JCARecoveryData != null) {
                            final String id = _JCARecoveryData.getWrapper().getProviderId();
                            if (TMHelper.isProviderInstalled(id)) {
                                if (tc.isDebugEnabled())
                                    Tr.debug(tc, "recover", "Do nothing. Expect provider " + id + " will complete.");
                                // Do nothing. RA is responsible for completing.
                            } else {
                                switch (_configProvider.getHeuristicCompletionDirection()) {
                                    case ConfigurationProvider.HEURISTIC_COMPLETION_DIRECTION_COMMIT:
                                        Tr.error(tc, "WTRN0098_COMMIT_RA_UNINSTALLED", new Object[] { getTranName(), id });
                                        recoverCommit(false);
                                        break;
                                    case ConfigurationProvider.HEURISTIC_COMPLETION_DIRECTION_MANUAL:
                                        // do nothing, administrative completion is required
                                        _needsManualCompletion = true;
                                        Tr.info(tc, "WTRN0101_MANUAL_RA_UNINSTALLED", new Object[] { getTranName(), id });
                                        break;
                                    default:
                                        Tr.error(tc, "WTRN0099_ROLLBACK_RA_UNINSTALLED", new Object[] { getTranName(), id });
                                        recoverRollback(false);
                                }
                            }
                        } else {
                            retryCompletion();
                        }
                        break;
                }
            } else {
                // For a top-level Transaction, we will only recover in the case
                // where we have successfully prepared.  If the state is not committing,
                // then assume it is rollback.
                if (state == TransactionState.STATE_LAST_PARTICIPANT) {
                    // LIDB1673-13 lps heuristic completion.
                    // The transaction was attempting to complete its
                    // 1PC resource when the server went down.
                    // Use the lpsHeuristicCompletion flag to determine
                    // how to complete the tx.
                    switch (ConfigurationProviderManager.getConfigurationProvider().getHeuristicCompletionDirection()) {
                        case ConfigurationProvider.HEURISTIC_COMPLETION_DIRECTION_COMMIT:
                            Tr.error(tc, "WTRN0096_HEURISTIC_MAY_HAVE_OCCURED", getTranName());
                            recoverCommit(false);
                            break;
                        case ConfigurationProvider.HEURISTIC_COMPLETION_DIRECTION_MANUAL:
                            // do nothing!?
                            _needsManualCompletion = true;
                            Tr.info(tc, "WTRN0097_HEURISTIC_MANUAL_COMPLETION", getTranName());
                            break;
                        default:
                            Tr.error(tc, "WTRN0102_HEURISTIC_MAY_HAVE_OCCURED", getTranName());
                            recoverRollback(false);
                    }
                } else if (state == TransactionState.STATE_COMMITTING)
                    recoverCommit(false);
                else
                    recoverRollback(false);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "recover");
    }

//    /**
//     * Directs the TransactionImpl to perform recovery actions based on its
//     * reconstructed state after a failure, or after an in-doubt timeout has
//     * occurred.
//     * This method is called by the RecoveryManager during recovery, in which case
//     * there is no terminator object, or during normal operation if the transaction
//     * commit retry interval has been exceeded for the transaction.
//     * If this method is called more times than the retry limit specified in
//     * COMMITRETRY, then the global outcome of the transaction is taken from the
//     * value of HEURISTICDIRECTION.
//     *
//     * This method is synchronized together with the Associations methods as it
//     * needs to support concurrency between inbound requests and in-doubt timer
//     * activity. On recovery, there will be no associations for non-subordinates.
//     *
//     * @return
//     */
//    @Override
//    synchronized public void recover()
//    {
//        final boolean traceOn = TraceComponent.isAnyTracingEnabled();
//
//        if (traceOn && tc.isEntryEnabled())
//            Tr.entry(tc, "recover", this);
//
//        if (_activeAssociations <= 0)
//        {
//            super.recover();
//        }
//
//        if (traceOn && tc.isEntryEnabled())
//            Tr.exit(tc, "recover");
//    }

    public boolean hasSuspendedAssociations() {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "hasSuspendedAssociations", new Object[] { "_activeAssociations=" + _activeAssociations, "_suspendedAssociations=" + _suspendedAssociations });
        final boolean result = _suspendedAssociations > 0;

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "hasSuspendedAssociations", result);
        return result;
    }

    /**
     * Called by the timeout manager when inactivity timer expires.
     * Needs to be synchronized as it may interfere with normal timeout.
     */
    public synchronized void inactivityTimeout() {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "inactivityTimeout", this);

        _inactivityTimerActive = false;

        if (_inactivityTimer != null) {
            try {
                // important that this runs as part of synchronized block
                // to prevent context being re-imported while processing.
                _inactivityTimer.alarm();
            } catch (Throwable exc) {
                FFDCFilter.processException(exc, "com.ibm.ws.tx.jta.TransactionImpl.inactivityTimeout", "2796", this);
                if (traceOn && tc.isEventEnabled())
                    Tr.event(tc, "exception caught in inactivityTimeout", exc);
                // swallow
            } finally {
                _inactivityTimer = null;
            }

        } else {
            if (_activeAssociations <= 0) // off server ... do the rollback
            {
                final EmbeddableTranManagerSet tranManager = (EmbeddableTranManagerSet) TransactionManagerFactory.getTransactionManager();

                try {
                    // resume this onto the current thread and roll it back
                    tranManager.resume(this);
                    // PK15024
                    // If there is a superior server involved in this transaction, make sure it is told about this inactivity timeout.
                    try {
                        tranManager.setRollbackOnly();
                    } catch (Exception e) {
                        FFDCFilter.processException(e, "com.ibm.ws.Transaction.JTA.TransactionImpl.inactivityTimeout", "4353", this);
                        if (traceOn && tc.isDebugEnabled())
                            Tr.debug(tc, "inactivityTimeout setRollbackOnly threw exception", e);
                    }
                    // Mark it so that a bean on the superior server can test its status on return.
                    // PK15024
                    tranManager.rollback();
                } catch (Exception ex) {
                    if (traceOn && tc.isDebugEnabled())
                        Tr.debug(tc, "inactivityTimeout resume/rollback threw exception", ex);
                    // swallow this exception
                    // main timeout may have already rolled back!
                }
            }
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "inactivityTimeout");
    }

    @Override
    public Thread getMostRecentThread() {
        // If active assocations is > 0 then transaction is on server or
        // downstream and the thread with which the transaction was last
        // associated is valid. Otherwise the request has returned to an
        // upstream server or client and the thread with which the
        // transaction was last associated will have been returned to the
        // thread pool and may be handling another request, i.e. it's
        // not relevant to this transaction.

        if (_activeAssociations > 0) {
            return _mostRecentThread.peek();
        }

        return null;
    }

    /**
     * Stop all active timers associated with this transaction.
     */
    @Override
    protected void cancelAlarms() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "cancelAlarms");

        if (_timeout > 0) {
            EmbeddableTimeoutManager.setTimeout(this, EmbeddableTimeoutManager.CANCEL_TIMEOUT, 0);
            _timeout = 0;
        }

        if (_inactivityTimerActive) {
            stopInactivityTimer();
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "cancelAlarms");
    }

    @Override
    protected void handleHeuristicOnCommit(boolean waitForHeuristic) {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "handleHeuristicOnCommit", new Object[] { waitForHeuristic, this });

        // If no resources to complete - check if we are a subordinate
        // with outstanding heuristic indication.  We may have missed a forget from
        // a superior as we do not log, it so on recovery may still be waiting for it.
        // Really when we get a forget we should remove our subordinate and heuristic
        // status from the log.  Check of superior has gone.
        if (_doNotRetryRecovery && waitForHeuristic &&
            TransactionState.STATE_HEURISTIC_ON_COMMIT == _status.getState()) {
            if (traceOn && tc.isEventEnabled())
                Tr.event(tc, "recoverCommit", "Checking if we can forget transaction");

            if (_wsatRC != null) {
                // WSAT currently will never send a forget...
                notifyCompletion();
            } else if (_JCARecoveryData == null) {
                replay();
            }
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "handleHeuristicOnCommit");
    }

    @Override
    protected void handleHeuristicOnRollback(boolean waitForHeuristic) {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "handleHeuristicOnRollback", new Object[] { waitForHeuristic, this });

        // If no resources to complete - check if we are a subordinate
        // with outstanding heuristic indication.  We may have missed a forget from
        // a superior as we do not log, it so on recovery may still be waiting for it.
        // Really when we get a forget we should remove our subordinate and heuristic
        // status from the log.  Check of superior has gone.
        if (_doNotRetryRecovery && waitForHeuristic &&
            TransactionState.STATE_HEURISTIC_ON_ROLLBACK == _status.getState()) {
            if (traceOn && tc.isEventEnabled())
                Tr.event(tc, "recoverRollback", "Checking if we can forget transaction");
            if (_wsatRC != null) {
                // WSAT currently will never send a forget...
                notifyCompletion();
            } else if (_JCARecoveryData == null) {
                replay();
            }
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "handleHeuristicOnRollback");
    }

    @Override
    protected void retryCompletion() {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "retryCompletion", new Object[] { this, _configProvider.getHeuristicRetryLimit(), _retryAttempts });

        if (_configProvider.getHeuristicRetryLimit() <= 0 || _retryAttempts < _configProvider.getHeuristicRetryLimit()) {
            _retryAttempts++;

            // Issue replay_completion for either IIOP or WSAT
            replay();

            // If replay_completion returns a state which does not specify a known outcome,
            // or throws NotPrepared, or the remote request fails, eg COMM_FAILURE, etc. then
            // set the timer to retry again later.
            // Note: this code is now the same as the old JTS1 and so should interwork ok.
            Tr.warning(tc, "WTRN0056_TRAN_RESYNC_FAILURE", getTranName());
            if (!_inRecovery) {
                // Use timeout manger if we are in-doubt and a normal transaction.
                // otherwise if in recovery, just return and let RecoveryManager poll us.
                if (_retryAttempts % 10 == 0 && _retryWait < Integer.MAX_VALUE / 2)
                    _retryWait *= 2;
                EmbeddableTimeoutManager.setTimeout(this, EmbeddableTimeoutManager.IN_DOUBT_TIMEOUT, _retryWait);
            }
        } else {
            // If we are not to attempt a retry of the replay_completion method, then
            // the HEURISTICCOMPLETION system variable is used to set the global
            // outcome.
            switch (_configProvider.getHeuristicCompletionDirection()) {
                case ConfigurationProvider.HEURISTIC_COMPLETION_DIRECTION_COMMIT:
                    Tr.error(tc, "WTRN0093_COMMIT_REPLAY_COMPLETION", getTranName());
                    recoverCommit(false);
                    break;
                case ConfigurationProvider.HEURISTIC_COMPLETION_DIRECTION_MANUAL:
                    // do nothing, administrative completion is required
                    _needsManualCompletion = true;
                    Tr.info(tc, "WTRN0095_MANUAL_REPLAY_COMPLETION", getTranName());
                    break;
                default:
                    Tr.error(tc, "WTRN0094_ROLLBACK_REPLAY_COMPLETION", getTranName());
                    recoverRollback(false);
            }
        }

        if (traceOn && tc.isEntryEnabled())
            Tr.exit(tc, "retryCompletion");
    }

    protected void replay() {
        final boolean traceOn = TraceComponent.isAnyTracingEnabled();

        if (traceOn && tc.isEntryEnabled())
            Tr.entry(tc, "replay", this);

        try {
            // Use the WSATRecoveryCoordinator to get the global outcome
            if (_wsatRC != null) {
                _wsatRC.replayCompletion(_globalId);
            } else {
                if (traceOn && tc.isEventEnabled())
                    Tr.event(tc, "No WSATRecoveryCoordinator to call replayCompletion on: " + _globalId);
            }
        } catch (Throwable exc) {
            FFDCFilter.processException(exc, "com.ibm.tx.jta.embeddable.impl.EmbeddableTransactionImpl.replay", "1018", this);
            if (traceOn && tc.isEventEnabled())
                Tr.event(tc, "exception caught in recover", exc);
        }

        if (traceOn && tc.isEntryEnabled()) {
            Tr.exit(tc, "replay");
        }
    }

    @Override
    protected void reconstructCoordinators(RecoverableUnit log) throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "reconstructCoordinators", new Object[] { log, this });

        // Create a WSATRecoveryCoordinator if we are a WSAT subordinate.
        final RecoverableUnitSection _wsatRCSection = log.lookupSection(RECCOORD_WSAT_SECTION);
        if (_wsatRCSection != null) {
            if (_subordinate) {
                // If we have already discovered we are a subordinate, then something is broken.
                Tr.error(tc, "WTRN0001_ERR_INT_ERROR", new Object[] { "reconstruct", this.getClass().getName() });
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "reconstructCoordinators", "SystemException");
                throw new SystemException();
            }

            try {
                _subordinate = true;
                final byte[] logData = _wsatRCSection.lastData();
                _wsatRC = WSATRecoveryCoordinator.fromLogData(logData);
//                WSATControlSet.reconstruct(this, _wsatRC, getFailureScopeController()); TODO
                _globalId = _wsatRC.getGlobalId();
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "We are WSAT subordinate: " + _globalId);

                new TransactionWrapper(this);
            } catch (Throwable e) {
                FFDCFilter.processException(e, "com.ibm.ws.tx.jta.TransactionImpl.reconstruct", "1670", this);
                Tr.fatal(tc, "WTRN0000_ERR_INT_ERROR", new Object[] { "reconstruct", this.getClass().getName(), e });
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "reconstructCoordinators", "SystemException");
                throw new SystemException(e.getLocalizedMessage());
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "reconstructCoordinators");
    }

    @Override
    protected TransactionState createState(TransactionImpl tx) {
        return new EmbeddableTransactionState((EmbeddableTransactionImpl) tx);
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.remote.DistributableTransaction#setWSATRecoveryCoordinator(com.ibm.tx.jta.embeddable.impl.WSATRecoveryCoordinator)
     */
    @Override
    public void setWSATRecoveryCoordinator(WSATRecoveryCoordinator rc) {
        _wsatRC = rc;
    }

    /**
     * @return
     */
    public WSATRecoveryCoordinator getWSATRecoveryCoordinator() {
        return _wsatRC;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.tx.remote.DistributableTransaction#replayCompletion()
     */
    @Override
    public void replayCompletion() {
        recover();
    }

    public void setThread(Thread t) {
        _thread = t;
    }

    public Thread getThread() {
        return _thread;
    }

    @Override
    public String toString() {
        return super.toString() + ",active=" + _activeAssociations + ",suspended=" + _suspendedAssociations + ","
               + (_thread != null ? "thread=" + String.format("%08X", _thread.getId()) : "Not on a thread, globalId=" + _globalId);
    }
}
