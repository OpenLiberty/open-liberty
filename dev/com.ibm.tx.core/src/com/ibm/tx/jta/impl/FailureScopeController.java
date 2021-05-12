package com.ibm.tx.jta.impl;

/*******************************************************************************
 * Copyright (c) 2002, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;

import javax.transaction.SystemException;

import com.ibm.tx.TranConstants;
import com.ibm.tx.util.ConcurrentHashSet;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.Transaction.JTA.FailureScopeLifeCycle;
import com.ibm.ws.Transaction.JTA.FailureScopeLifeCycleHelper;
import com.ibm.ws.Transaction.JTS.Configuration;
import com.ibm.ws.kernel.service.util.CpuInfo;
import com.ibm.ws.recoverylog.spi.FailureScope;
import com.ibm.ws.recoverylog.spi.RecoveryAgent;
import com.ibm.ws.recoverylog.spi.RecoveryLog;

public class FailureScopeController {
    private static final TraceComponent tc = Tr.register(FailureScopeController.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    protected FailureScope _failureScope;
    protected String _serverName;

    protected RecoveryLog _tranLog;
    protected RecoveryLog _xaLog;
    protected RecoveryLog _recoverXaLog;
    protected PartnerLogTable _partnerLogTable;

    /**
     * Boolean flag used to record if the managed failure scope is the local
     * failure scope (true) or a peer failure scope (false)
     */
    protected boolean _localFailureScope;

    protected RecoveryManager _recoveryManager;

    protected FailureScopeLifeCycle _fslc;

    /**
     * This set contains a list of all active transactions.
     */
    protected Set<TransactionImpl> _transactions;

    private static final int SMP_THRESH = AccessController.doPrivileged(new PrivilegedAction<Integer>() {
        @Override
        public Integer run() {
            return Integer.getInteger("com.ibm.tx.jta.FailureScopeController.SMP_THRESH", 4);
        }
    });

    protected static final boolean isConcurrent = CpuInfo.getAvailableProcessors() > SMP_THRESH;

    protected FailureScopeController() {}

    @SuppressWarnings("unused")
    public FailureScopeController(FailureScope fs) throws SystemException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "FailureScopeController", fs);

        _failureScope = fs;

        _serverName = fs.serverName();

        // If small SMP system, use synchronized hashset. If a large system,
        // use a concurrent data structure
        if (isConcurrent) {
            _transactions = new ConcurrentHashSet<TransactionImpl>();
        } else {
            _transactions = new java.util.HashSet<TransactionImpl>();
        }

        _localFailureScope = (_serverName.equals(Configuration.getServerName()));

        _partnerLogTable = new PartnerLogTable(this);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "FailureScopeController", this);
    }

    public RecoveryLog getTransactionLog() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getTransactionLog", new Object[] { this, _tranLog });
        return _tranLog;
    }

    public RecoveryLog getPartnerLog() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getPartnerLog", new Object[] { this, _xaLog });
        return _xaLog;
    }

    public PartnerLogTable getPartnerLogTable() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getPartnerLogTable", new Object[] { this, _partnerLogTable });
        return _partnerLogTable;
    }

    /**
     * Returns a boolean flag to indicate if the managed failure scope is the local
     * failure scope (true) or a peer failure scope (false)
     *
     * @return boolean local failure scope indicator flag
     */
    public boolean localFailureScope() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "localFailureScope", new Object[] { this, _localFailureScope });
        return _localFailureScope;
    }

    /**
     * Returns the name of the server represented by the managed failure scope.
     *
     * @return String The server name
     */
    public String serverName() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "serverName", new Object[] { this, _serverName });
        return _serverName;
    }

    /**
     * Creates a RecoveryManager object instance and associates it with this FailureScopeController
     * The recovery manager handles recovery processing on behalf of the managed failure scope.
     *
     * @return String The new RecoveryManager instance.
     */
    public void createRecoveryManager(RecoveryAgent agent, RecoveryLog tranLog, RecoveryLog xaLog, RecoveryLog recoverXaLog, byte[] defaultApplId, int defaultEpoch)/*
                                                                                                                                                                     * throws
                                                                                                                                                                     * Exception
                                                                                                                                                                     */
    {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createRecoveryManager", new Object[] { this, agent, tranLog, xaLog, recoverXaLog, defaultApplId, defaultEpoch });

        _tranLog = tranLog;
        _xaLog = xaLog;
        _recoverXaLog = recoverXaLog;
        _recoveryManager = new RecoveryManager(this, agent, tranLog, xaLog, recoverXaLog, defaultApplId, defaultEpoch);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createRecoveryManager", _recoveryManager);
    }

    public RecoveryManager getRecoveryManager() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getRecoveryManager", new Object[] { this, _recoveryManager });
        return _recoveryManager;
    }

    public void shutdown(boolean immediate) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "shutdown", new Object[] { this, Boolean.valueOf(immediate) });

        // As long as we are not being requsted to perform an immediate shutdown and recovery
        // processing was not prevented in the first place, perform the shutdown logic.
        // I'll preserve the logic here for 232512 but it might be OK to skip this
        // in the absence of a recovery manager

        if (immediate && (!_localFailureScope)) {
            IllegalArgumentException iae = new IllegalArgumentException();
            if (tc.isDebugEnabled())
                Tr.debug(tc, "shutdown", iae);
            FFDCFilter.processException(iae, "com.ibm.tx.jta.impl.FailureScopeController.shutdown", "419", this);
            throw iae;
        }

        if (!immediate && (_recoveryManager == null || !_recoveryManager.recoveryPrevented())) {
            // If there is a recovery manager then direct it to stop recovery processing.
            if (_recoveryManager != null) {
                _recoveryManager.prepareToShutdown();
            }

            // The call to removeFromActiveList must be made AFTER prepareToShutdown as this synchronizes with the recovery thread
            // doing the corresponding addToActiveList
            FailureScopeLifeCycleHelper.removeFromActiveList(_fslc);

            // Process local log clean up here - merge the partner log tables, etc
            // and update and close the logs.  For peer recovery, the logs are closed
            // in recoveryManager.resync() once recovery is suspended/complete.
            if (_localFailureScope) {
                // Tell the partner log data objects within the partner log table that we
                // are trying to shutdown and it should not attempt to log data.
                _partnerLogTable.terminate();

                final TransactionImpl[] runningTransactions = getAllTransactions();

                final boolean transactionsLeft = runningTransactions != null && runningTransactions.length > 0;

                boolean partnersLeft = true; /* @PK31789A */
                try /* @PK31789A */
                { /* @PK31789A */
                    // Drive shutdown processing for the recovery                 @PK31789A
                    // manager to clean up.                                       @PK31789A
                    PartnerLogTable plt = null;
                    if (_recoveryManager != null) /* @PK31789A */
                    { /* @PK31789A */

                        // preShutdown will ensure that the tranlog is forced.    @PK31789A
                        // If there are any exceptions, then it is not safe to    @PK31789A
                        // tidy up the partnerlog,                                @PK31789A
                        // so bypass 'this.shutdown(runningTransactions)'         @PK31789A

                        _recoveryManager.preShutdown(transactionsLeft); /* @PK31789A */

                        plt = _recoveryManager.getPartnerLogTable();
                    }

                    partnersLeft = shutdown(runningTransactions, plt); /* @PK31789A */
                } /* @PK31789A */
                catch (Exception e) /* @PK31789A */
                { /* @PK31789A */
                    // exception from preShutdown already ffdc'd and logged       @PK31789A
                    // nothing more required here                                 @PK31789A
                } /* @PK31789A */

                if (_recoveryManager != null) {
                    _recoveryManager.postShutdown(partnersLeft);
                }

                if (!partnersLeft) {
                    Tr.audit(tc, "WTRN0105_CLEAN_SHUTDOWN");
                    // Shutdown is clean, we may delete the home server lease if peer recovery is enabled.
                    // This is a noop if peer recovery is not enabled.
                    if (_recoveryManager != null) {
                        _recoveryManager.deleteServerLease(serverName());
                    }
                } else if (tc.isDebugEnabled()) {
                    if (partnersLeft) {
                        Tr.debug(tc, "Not a clean shutdown", new Object[] { immediate, _localFailureScope });
                    }
                }
            } else {
                // Cleanup remaining transactions and close the logs
                if (_recoveryManager != null)
                    _recoveryManager.cleanupRemoteFailureScope();
            }

            // Now that recovery processing has stopped, clear out all the fields to guarentee what
            // we can no longer drive recovery processing for this failure scope.
            _tranLog = null;
            _xaLog = null;
            _recoverXaLog = null;
            _recoveryManager = null;
            _failureScope = null;
            _serverName = null;
            _partnerLogTable = null;
            _fslc = null;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "shutdown");
    }

    protected boolean shutdown(TransactionImpl[] runningTransactions, PartnerLogTable plt) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "shutdown", new Object[] { runningTransactions, plt });

        // Check for any active transactions referenceing the PLTs
        // Note: we merge after this - old 6.0/6.1 code used to merge before this
        // and we could lookup the wrong entry and delete a good one.
        if (runningTransactions != null) {
            // Go through list of txns and
            // determine if any could have generated in-doubt resources??
            // and match the ids with our XARecoveryData records and update appropriately.
            for (TransactionImpl tx : runningTransactions) {
                if (tc.isEventEnabled())
                    Tr.event(tc, "Transaction " + tx + " is still active");

                tx.shutdown();
            }
        }

        // Now we have finished with transactions...
        // Merge the RecoveryManager's partnerLogTable            @PK31789A
        // into our runtime one                                   @PK31789A
        if (plt != null)
            _partnerLogTable.merge(plt); /* @PK31789A */

        final boolean result = _partnerLogTable.shutdown();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "shutdown", result);
        return result;
    }

    /**
     * This method is called to register the creation of a new transaction associated
     * with the managed failure scope.
     *
     * @param tran The transaction identity object
     * @param recovered Flag to indicate if the new transaction was created as part
     *            of a recovery process for this failure scope (true) or
     *            normal running (false)
     */
    public void registerTransaction(TransactionImpl tran, boolean recovered) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "registerTransaction", new Object[] { this, tran, recovered });

        //AJQ - use concurrent set on large SMP
        if (isConcurrent)
            _transactions.add(tran);

        //AJQ - new gate for synchronized block
        if (!isConcurrent || recovered) {
            synchronized (this) {
                //AJQ - only if using standard hashset
                if (!isConcurrent)
                    _transactions.add(tran);

                if (recovered) {
                    _recoveryManager.registerTransaction(tran);
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "registerTransaction");
    }

    /**
     * This method is called to register the completion of a transaction associated
     * with the managed failure scope.
     *
     * @param tran The transaction identity object
     */
    public void deregisterTransaction(TransactionImpl tran, boolean recovered) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "deregisterTransaction", new Object[] { this, tran, recovered });

        //AJQ - use concurrent set on large SMP
        if (isConcurrent)
            _transactions.remove(tran);

        //AJQ - new gate for synchronized block
        if (!isConcurrent || recovered) {
            synchronized (this) {
                //AJQ - only if using standard hashset
                if (!isConcurrent)
                    _transactions.remove(tran);

                if (recovered) {
                    _recoveryManager.deregisterTransaction(tran);
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "deregisterTransaction");
    }

    public TransactionImpl[] getAllTransactions() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getAllTransactions", this);

        TransactionImpl[] transactionArray = null;

        synchronized (this) {
            final int numTransactions = _transactions.size();

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Found " + numTransactions + " active transaction(s)");

            transactionArray = new TransactionImpl[numTransactions];
            _transactions.toArray(transactionArray);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getAllTransactions", transactionArray);

        return transactionArray;
    }

    public FailureScope failureScope() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "failureScope", _failureScope);
        return _failureScope;
    }

    public void setFailureScopeLifeCycle(FailureScopeLifeCycle fslc) {
        _fslc = fslc;
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setFailureScopeLifeCycle", _fslc);
    }
}