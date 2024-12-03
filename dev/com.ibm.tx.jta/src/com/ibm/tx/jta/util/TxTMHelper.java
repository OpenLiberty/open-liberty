/*******************************************************************************
 * Copyright (c) 2002, 2024 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.tx.jta.util;

import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;

import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.annotations.Reference;

import com.ibm.tx.TranConstants;
import com.ibm.tx.config.ConfigurationProvider;
import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.XAResourceFactory;
import com.ibm.tx.jta.impl.EventSemaphore;
import com.ibm.tx.jta.impl.LocalTIDTable;
import com.ibm.tx.jta.impl.RecoveryManager;
import com.ibm.tx.jta.impl.TranManagerSet;
import com.ibm.tx.jta.impl.TransactionImpl;
import com.ibm.tx.jta.impl.TxRecoveryAgentImpl;
import com.ibm.tx.jta.impl.UserTransactionImpl;
import com.ibm.tx.util.TMHelper;
import com.ibm.tx.util.TMService;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.recoverylog.spi.RecLogServiceImpl;
import com.ibm.ws.recoverylog.spi.RecoveryDirector;
import com.ibm.ws.recoverylog.spi.RecoveryDirectorFactory;
import com.ibm.ws.recoverylog.spi.RecoveryFailedException;
import com.ibm.ws.recoverylog.spi.RecoveryLogFactory;
import com.ibm.ws.uow.UOWScopeCallback;
import com.ibm.ws.uow.UOWScopeCallbackAgent;
import com.ibm.wsspi.tx.UOWEventListener;

import io.openliberty.checkpoint.spi.CheckpointPhase;

public class TxTMHelper implements TMService, UOWScopeCallbackAgent {
    private static final TraceComponent tc = Tr.register(TxTMHelper.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private static TMService.TMStates _state = TMService.TMStates.INACTIVE;

    private static TxRecoveryAgentImpl _recoveryAgent;

    protected final static EventSemaphore _asyncRecoverySemaphore = new EventSemaphore();

    protected static RuntimeException _resyncException;

    protected RecLogServiceImpl _recLogService;

    protected RecoveryDirector _recoveryDirector;

    protected boolean _recoverDBLogStarted;

    protected String _recoveryIdentity;
    protected String _recoveryGroup;

    private boolean _xaResourceFactoryReady;
    private boolean _waitForRecovery;
    private boolean _tmsReady;
    private boolean _recoveryLogFactoryReady;
    private RecoveryLogFactory _recoveryLogFactory;
    private boolean _recoveryLogServiceReady;
    private boolean _requireDataSourceActive;
    private boolean _localRecoveryFailed;

    protected static BundleContext _bc;

    static public TMService.TMStates getState() {
        return _state;
    }

    static public void setState(TMService.TMStates state) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Setting state from " + _state + " to " + state);
        _state = state;
    }

    public TxTMHelper() {
        TMHelper.setTMService(this);
    }

    //recoveryLogService=com.ibm.ws.recoverylog.spi.RecLogServiceImpl; \
    //recoveryLogFactory=com.ibm.ws.recoverylog.spi.RecoveryLogFactory; \
    //

    //<reference name="recoveryLogService" cardinality="1..1" interface="com.ibm.ws.recoverylog.spi.RecLogServiceImpl" bind="setRecoveryLogService" unbind="unsetRecoveryLogService"/>
    //<reference name="recoveryLogFactory" cardinality="1..1" interface="com.ibm.ws.recoverylog.spi.RecoveryLogFactory" bind="setRecoveryLogFactory" unbind="unsetRecoveryLogFactory"/>

    @Reference(unbind = "shutdown")
    protected void setConfigurationProvider(ConfigurationProvider p) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setConfigurationProvider", p);

        try {
            ConfigurationProviderManager.setConfigurationProvider(p);

            // in an osgi environment we may get unconfigured and then reconfigured as bundles are
            // started/stopped.  If we were previously unconfigured, then we would have shutdown and
            // our state will be 'stopped' (rather than inactive).  If so, then re-start now.
            // The alternative would perhaps be to modify the checkTMState method to re-start for this state?
            if (_state == TMService.TMStates.STOPPED) {
                start();
            }
        } catch (Exception e) {
            FFDCFilter.processException(e, "com.ibm.tx.jta.util.impl.TxTMHelper.setConfigurationProvider", "37", this);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "setConfigurationProvider");
    }

    @Reference(cardinality = OPTIONAL, policy = DYNAMIC)
    protected void setXaResourceFactory(ServiceReference<XAResourceFactory> ref) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setXaResourceFactory", "ref " + ref);

        _xaResourceFactoryReady = true;

        if (ableToStartRecoveryNow()) {
            // Can start recovery now
            try {
                startRecovery();
            } catch (Exception e) {
                FFDCFilter.processException(e, "com.ibm.tx.jta.util.impl.TxTMHelper.setXaResourceFactory", "148", this);
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "setXaResourceFactory");
    }

    protected void unsetXaResourceFactory(ServiceReference<XAResourceFactory> ref) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "unsetXaResourceFactory, ref " + ref);
    }

    @Reference
    public void setRecoveryLogFactory(RecoveryLogFactory fac) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setRecoveryLogFactory, factory: " + fac, this);
        _recoveryLogFactory = fac;
        _recoveryLogFactoryReady = true;

        if (ableToStartRecoveryNow()) {
            // Can start recovery now
            try {
                startRecovery();
            } catch (Exception e) {
                FFDCFilter.processException(e, "com.ibm.tx.jta.util.impl.TxTMHelper.setRecoveryLogFactory", "206", this);
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "setRecoveryLogFactory");
    }

    @Reference
    public void setRecoveryLogService(ServiceReference<RecLogServiceImpl> ref) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setRecoveryLogService", ref);

        _recLogService = ref.getBundle().getBundleContext().getService(ref);
        _recoveryLogServiceReady = true;

        if (ableToStartRecoveryNow()) {
            // Can start recovery now
            try {
                startRecovery();
            } catch (Exception e) {
                FFDCFilter.processException(e, "com.ibm.tx.jta.util.impl.TxTMHelper.setRecoveryLogService", "148", this);
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "setRecoveryLogService");
    }

    @Override
    public boolean isProviderInstalled(String providerId) {
        return true;
    }

    /*
     * (non-Javadoc)
     * In tWAS this method is driven when the transaction service is deemed to be ready for new
     * work. This is at the point that log REPLAY (not resync) is complete. Replay is complete once
     * recovery processing has created an in-memory view of the contents of its recovery logs and
     * does not to read from the logs again in order to complete its recovery processing.
     *
     * If replay is successful, the method is called from TranManagerSet.replayComplete() when
     * the failure scope is local - ie not in the peer recovery case.
     *
     * @see com.ibm.tx.util.TMService#asynchRecoveryProcessingComplete(java.lang.Throwable)
     */
    @Override
    public void asynchRecoveryProcessingComplete(Throwable t) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "asynchRecoveryProcessingComplete", t);
    }

    @Override
    public void start(boolean waitForRecovery) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "start", new Object[] { waitForRecovery });

        // Get bundle context, for use by recovery in DS Service lookup.
        retrieveBundleContext();

        _tmsReady = true;
        _waitForRecovery = waitForRecovery;

        if (ableToStartRecoveryNow()) {
            // Can start recovery now
            try {
                startRecovery();
            } catch (RecoveryFailedException exc) {
                if (CheckpointPhase.getPhase() != CheckpointPhase.INACTIVE && !CheckpointPhase.getPhase().restored()) {
                    // Local recovery failed during checkpoint, likely due inaccessible logging resource.
                    // Shutdown now to enable recovery logging to re-initialize (restart) during restore
                    // using the restored server's configuration and environment vars.
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Local recovery failed during checkpoint. Shutdown to enable local recovery during restore.");
                    shutdown();
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Local recovery failed.");
                    _localRecoveryFailed = true;
                }
            } catch (Exception e) {
                FFDCFilter.processException(e, "com.ibm.tx.jta.util.impl.TxTMHelper.start", "148", this);
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "start");
    }

    /**
     * Non-WAS version.
     * Initialize the configuration and the recovery service.
     *
     */
    public void startRecovery() throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "startRecovery");

        // Note that _recoverDBLogStarted is always false if we are logging to a filesystem.
        // If we are logging to an RDBMS, then we will start the TM which will spin off a thread
        // to perform recovery processing. Unfortunately, this latter thread, as part of
        // DataSource processing, will also attempt to start the TM as a result of registering
        // ResourceInfo. In this specific scenario we'd block on sync and hang recovery. The ThreadLocal
        // stored in the RecoveryManager allows us to determine which thread we are on and skip
        // the sync block if necessary. In other situations, where logs are stored in a database we
        // require that other threads DO block and therefore follow the existing filesystem model, where
        // recovery may be in progress but another thread wishes to start transactional work, determines
        // the the TM state is not "active" and attempts to start recovery itself.
        boolean skipRecovery = false;
        if (_recoverDBLogStarted) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Tran Logging to an RDBMS, recoveryAgent is: " + _recoveryAgent);
            if (_recoveryAgent != null) {
                if (_recoveryAgent.isReplayThread()) {
                    Tr.debug(tc, "Thread on which replay will be done - skip recovery processing");
                    skipRecovery = true;
                }
            }
        }

        if (!skipRecovery) {
            synchronized (this) {
                TMHelper.setTMService(this);

                // Test whether we are logging to an RDBMS
                final ConfigurationProvider cp = ConfigurationProviderManager.getConfigurationProvider();
                if (cp != null && cp.isSQLRecoveryLog()) {
                    _recoverDBLogStarted = true;
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Tran Logging to an RDBMS set recoverDBLogStarted flag");
                }

                if (getState() != TMService.TMStates.INACTIVE && getState() != TMService.TMStates.STOPPED) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "startRecovery", "Already started");
                    return;
                }

                setResyncException(null);

                if (_recLogService != null) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "We already have a recovery log service: " + _recLogService);
                } else {
                    _recLogService = new RecLogServiceImpl();
                }

                // Create the Recovery Director
                _recoveryDirector = RecoveryDirectorFactory.createRecoveryDirector();

                // For cloud support, retrieve recovery identity from the configuration if it is defined.
                if (cp != null) {
                    _recoveryIdentity = cp.getRecoveryIdentity();
                    _recoveryGroup = cp.getRecoveryGroup();
                }

                //Add this guard to ensure that we have sufficient config to drive recovery.
                boolean allowRecovery = true;
                if (cp == null) {
                    // A null ConfigurationProvider cannot be tolerated.
                    allowRecovery = false;
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Configuration Provider is null");
                } else {
                    String sName = cp.getServerName();
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Retrieved server name " + sName);
                    if (sName == null || sName.isEmpty()) {
                        // An empty string serverName suggests that the Location Service was unavailable
                        allowRecovery = false;
                    }
                }

                if (!allowRecovery) {
                    try {
                        shutdown();
                    } catch (RuntimeException e) {
                        FFDCFilter.processException(e, "com.ibm.tx.jta.util.TxTMHelper.start", "279", this);
                    }

                    final Throwable se = new SystemException();
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "startRecovery", se);
                    throw (SystemException) se;
                }

                if (_recoveryIdentity != null && !_recoveryIdentity.isEmpty()) {
                    _recLogService.initialize(_recoveryIdentity);
                } else {
                    String serverName = null;
                    if (cp != null) {
                        serverName = cp.getServerName();
                    }
                    _recLogService.initialize(serverName);
                }

                TxRecoveryAgentImpl txAgent = createRecoveryAgent(_recoveryDirector);

                if (_recoveryGroup != null && !_recoveryGroup.isEmpty()) {
                    txAgent.setRecoveryGroup(_recoveryGroup);

                    // We will do peer recovery if the recovery identity and group are set
                    if (_recoveryIdentity != null && !_recoveryIdentity.isEmpty()) {
                        _recLogService.setPeerRecoverySupported(true);
                        txAgent.setPeerRecoverySupported(true);
                        // Override the disable2PC property if it has been set
                        TransactionImpl.setDisable2PCDefault(false);
                        Tr.audit(tc, "WTRN0108I: Server with identity " + _recoveryIdentity + " is monitoring its peers for Transaction Peer Recovery");
                    } else {
                        Tr.audit(tc, "WTRN0108I: recoveryIdentity is not set. Transaction Peer Recovery is disabled");
                    }
                }

                setRecoveryAgent(txAgent);

                // Fake recovery only mode if we're to wait
                RecoveryManager._waitForRecovery = _waitForRecovery;

                // Kick off recovery
                try {
                    _recLogService.startRecovery(_recoveryLogFactory);
                } catch (Exception e) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Recovery failed with exception", e);
                    // mark recovery failed
                    _localRecoveryFailed = true;
                    // ensure other threads don't wait for recovery to complete
                    _asyncRecoverySemaphore.post();
                    throw e;
                }

                // Defect RTC 99071. Don't make the STATE transition until recovery has been fully
                // initialised, after replay completion but before resync completion.
                setState(TMService.TMStates.RECOVERING);

                if (_waitForRecovery) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Waiting for completion of asynchronous recovery");
                    _asyncRecoverySemaphore.waitEvent();
                    if (_localRecoveryFailed) {
                        // this thread awoke but recovery has already failed - bail out now
                        Tr.debug(tc, "Asynchronous recovery failed on another thread");
                        return;
                    }
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Asynchronous recovery is complete");

                    if (_resyncException != null) {
                        try {
                            shutdown();
                        } catch (RuntimeException e) {
                            FFDCFilter.processException(e, "com.ibm.tx.jta.util.TxTMHelper.start", "137", this);
                        }

                        final Throwable se = new SystemException().initCause(_resyncException);
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "startRecovery", se);
                        throw (SystemException) se;
                    }
                } // eof if waitForRecovery
            } // eof synchronized block
        } // eof if !skipRecovery
        else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Tran Logging to an RDBMS and we determined that we should skip recovery");
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "startRecovery");
    }

    private synchronized void shutdown(boolean withCleanup, int timeout) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "shutdown", new Object[] { withCleanup, timeout });

        if ((_state != TMService.TMStates.STOPPED) && (_state != TMService.TMStates.INACTIVE)
            || (_state == TMService.TMStates.INACTIVE &&
                CheckpointPhase.getPhase() != CheckpointPhase.INACTIVE && !CheckpointPhase.getPhase().restored())) {
            // Shutdown during checkpoint to enable recovery logging to reset during restore.
            // This is especially necessary for SQL recovery logging, where the datasource
            // will likely fail to connect during checkpoint, leaving TMState==INACTIVE and
            // the recovery director+agents initialized with potentially stale configuration.

            // Ensure no new transactions can start
            setState(TMService.TMStates.STOPPING);

            // If timeout 0, don't wait at all
            if (timeout != 0) {
                // Wait till running transactions have stopped
                // If timeout <= 0 wait forever if necessary
                int timeToWait = timeout;
                int timeSlept = 0;
                while (LocalTIDTable.getAllTransactions().length > 0) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "There are " + LocalTIDTable.getAllTransactions().length + " incomplete transactions");
                    if (timeout < 0 || timeToWait-- > 0) {
                        try {
                            // Sleep for a second at a time
                            Thread.sleep(1000);
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Waited " + ++timeSlept + " seconds for transactions to finish");
                        } catch (InterruptedException e) {}
                    } else {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Gave up waiting for transactions to finish after " + ++timeSlept + " seconds");
                        break;
                    }
                }

            }

            // Issue #24104 - When Transaction Service shuts down, mark laggard transactions as rollbackOnly
            TransactionImpl[] trans = LocalTIDTable.getAllTransactions();
            if (LocalTIDTable.getAllTransactions().length > 0) {

                if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                    Tr.event(tc, "Found " + trans.length + " active transactions.");

                for (int i = 0; i < trans.length; i++) {
                    final TransactionImpl tx = trans[i];

                    final int preStatus = tx.getStatus();

                    if (preStatus != Status.STATUS_ROLLEDBACK &&
                        preStatus != Status.STATUS_COMMITTED &&
                        preStatus != Status.STATUS_COMMITTING &&
                        preStatus != Status.STATUS_NO_TRANSACTION &&
                        preStatus != Status.STATUS_MARKED_ROLLBACK) {
                        rollbackTransaction(tx);

                        final int postStatus = tx.getStatus();

                        if (postStatus == Status.STATUS_ROLLEDBACK ||
                            postStatus == Status.STATUS_NO_TRANSACTION) {
                            Tr.warning(tc, "WTRN0034_DURING_SERVER_QUIESCE_TX_ROLLBACK_SUCCEEDED", Long.toString(tx.getLocalTID()));
                        } else if (postStatus == Status.STATUS_MARKED_ROLLBACK) {
                            Tr.warning(tc, "WTRN0036_DURING_SERVER_QUIESCE_TX_MARKED_ROLLBACK_ONLY", Long.toString(tx.getLocalTID()));
                        } else {
                            Tr.warning(tc, "WTRN0035_DURING_SERVER_QUIESCE_TX_ROLLBACK_FAILED", Long.toString(tx.getLocalTID()));
                        }
                    } else {
                        if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled())
                            Tr.event(tc, "Tx already committing or rolled back. Skipping.");
                    }

                }
            }

            // ConfigurationProviderManager.stop(true);

            _recoveryAgent.stop(false);

            _recLogService.stop();

            // Issue #23676. The JCA component needs TX services at shutdown. If TranManagerSet.cleanup() is called,
            // as it was previously at this point, then that removes the current (shutdown) thread's reference to the TM.
            // The task initiated by JCA may generate an IllegalStateException as a result of attempting to instantiate
            // a new ThreadLocal because Transaction Manager may have dereferenced its Configuration Provider.
            //
            // The withCleanup parameter allows the retention of the previous behaviour for use by the zos_tx unittests
            // who are the sole user of the TMHelper.shutdown(int) method
            if (withCleanup) {
                TransactionManager tm = TransactionManagerFactory.getTransactionManager();
                ((TranManagerSet) tm).cleanup();
            }

            setRecoveryAgent(null);

            RecoveryDirectorFactory.reset();

            LocalTIDTable.clear();

            if (CheckpointPhase.getPhase() != CheckpointPhase.INACTIVE && !CheckpointPhase.getPhase().restored()) {
                // Waiting on the semaphore will cause checkpoint to fail. Skip the wait
                // as resync never started and will never complete (interrupt).
                setResyncException(null);
            } else {
                _asyncRecoverySemaphore.waitEvent();
                setResyncException(null);
                _asyncRecoverySemaphore.clear();
            }

            ConfigurationProviderManager.stop(true);

            setState(TMService.TMStates.STOPPED);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "shutdown");
    }

    protected static void setRecoveryAgent(TxRecoveryAgentImpl recoveryAgent) {
        _recoveryAgent = recoveryAgent;
    }

    // Used by liberty
    public void shutdown(ConfigurationProvider cp) throws Exception {
        final int shutdownDelay;

        if (cp != null) {
            shutdownDelay = cp.getDefaultMaximumShutdownDelay();
        } else {
            shutdownDelay = 0;
        }

        shutdown(false, shutdownDelay);
    }

    @Override
    public void shutdown() throws Exception {
        shutdown(ConfigurationProviderManager.getConfigurationProvider());
    }

    @Override
    public void shutdown(int timeout) throws Exception {
        shutdown(true, timeout);
    }

    @Override
    public void start() throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "start");

        if (tc.isDebugEnabled()) {
            for (StackTraceElement ste : Thread.currentThread().getStackTrace()) {
                Tr.debug(tc, " " + ste);
            }
        }

        final ConfigurationProvider cp = ConfigurationProviderManager.getConfigurationProvider();
//        Code removed when DataSource changes were made for 93854
//        boolean deferStart = false;
//        if (cp != null)
//        {
//            // Used to test whether we are logging to an RDBMS - if so a ResourceFactory will have been
//            // configured
//            if (cp.getResourceFactory() != null)
//            {
//                // Test whether the user set the RecoverOnStartup flag to false and if they did,
//                // then store this info as next time through we'll want to allow recovery to
//                // proceed
//                if (tc.isDebugEnabled())
//                    Tr.debug(tc, "Check whether the user originally set recover on startup - flag is currently: " + _recoverOnStartFlag);
//                if (_recoverOnStartFlag)
//                {
//                    if (!cp.isRecoverOnStartup())
//                    {
//                        if (tc.isDebugEnabled())
//                            Tr.debug(tc, "recover on startup is FALSE in config, set flag FALSE");
//                        _recoverOnStartFlag = false;
//                    }
//
//                    if (tc.isDebugEnabled())
//                        Tr.debug(tc, "Resource Factory NOT NULL - temporary workaround DEFER RECOVERY START");
//                    deferStart = true;
//                }
//            }
//        }
//
//        if (!deferStart)

        start(cp != null && cp.isWaitForRecovery());

        if (tc.isEntryEnabled())
            Tr.exit(tc, "start");
    }

    public static synchronized void resyncComplete(RuntimeException r) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "resyncComplete", r);

        if (_state == TMService.TMStates.RECOVERING) {
            setState(TMService.TMStates.ACTIVE);
        }

        setResyncException(r);
        _asyncRecoverySemaphore.post();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "resyncComplete");
    }

    protected static void setResyncException(RuntimeException r) {
        _resyncException = r;
    }

    public static boolean ready() {
        return _state == TMStates.ACTIVE || _state == TMStates.RECOVERING;
    }

    @Override
    public void checkTMState() throws NotSupportedException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "checkTMState", _state);

        if (_state != TMService.TMStates.ACTIVE) {
            if (_state == TMService.TMStates.RECOVERING) {
                // A noop
            } else if (_state == TMService.TMStates.INACTIVE || _state == TMService.TMStates.STOPPED) {
                try {
                    TMHelper.start();
                } catch (Exception e) {
                    final NotSupportedException nse = new NotSupportedException();
                    nse.initCause(e);
                    throw nse;
                }
//            } else if (_state == TMService.TMStates.STOPPED &&
//                       CheckpointPhase.getPhase() != CheckpointPhase.INACTIVE && CheckpointPhase.getPhase().restored()) {
//                // A new state transition for InstantOn. Enable recovery logging to restart
//                // during restore whenever initial local recovery fails during checkpoint.
//                if (tc.isDebugEnabled())
//                    Tr.debug(tc, "checkTMState", "TMService was shutdown during checkpoint. Restarting the TMService during restore");
//                try {
//                    TMHelper.start();
//                } catch (Exception e) {
//                    final NotSupportedException nse = new NotSupportedException();
//                    nse.initCause(e);
//                    throw nse;
//                }
            } else if (_state == TMService.TMStates.STOPPING) {
                throw new NotSupportedException("JTM is stopping");
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "checkTMState");
    }

    // UOWScopeCallbackAgent interface
    @Override
    public void registerCallback(UOWScopeCallback callback) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "registerCallback", callback);

        UserTransactionImpl.instance().registerCallback(callback);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "registerCallback");
    }

    @Override
    public void unregisterCallback(UOWScopeCallback callback) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "unregisterCallback", callback);

        UserTransactionImpl.instance().unregisterCallback(callback);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "unregisterCallback");
    }

    public void setUOWEventListener(UOWEventListener el) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setUOWEventListener", el);
        ((UOWCurrent) TranManagerSet.instance()).setUOWEventListener(el);
    }

    public void unsetUOWEventListener(UOWEventListener el) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "unsetUOWEventListener", el);
        try {
            ((UOWCurrent) TranManagerSet.instance()).unsetUOWEventListener(el);
        } catch (IllegalStateException e) {
            // Server is on the way down
        }
    }

    protected TxRecoveryAgentImpl createRecoveryAgent(RecoveryDirector recoveryDirector) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createRecoveryAgent", recoveryDirector);

        TxRecoveryAgentImpl txAgent = new TxRecoveryAgentImpl(recoveryDirector);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createRecoveryAgent", txAgent);

        return txAgent;
    }

    /**
     * This method retrieves bundle context. There is a requirement to lookup the DS Services Registry during recovery.
     * Any bundle context will do for the lookup - this method is overridden in the ws.tx.embeddable bundle so that if that
     * bundle has started before the tx.jta bundle, then we are still able to access the Service Registry.
     *
     * @return
     */
    protected void retrieveBundleContext() {

        BundleContext bc = TxBundleTools.getBundleContext();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "retrieveBundleContext, bc " + bc);
        _bc = bc;
    }

    public static BundleContext getBundleContext() {

        if (tc.isDebugEnabled())
            Tr.debug(tc, "getBundleContext, bc " + _bc);
        return _bc;
    }

    private boolean ableToStartRecoveryNow() {
        if (tc.isEntryEnabled())
            Tr.debug(tc, "ableToStartRecoveryNow");

        boolean recoverNow = false;

        // If we are logging to a Database then additional services need to be in place before we
        // can recover.
        ConfigurationProviderManager.start();
        final ConfigurationProvider cp = ConfigurationProviderManager.getConfigurationProvider();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Working with config provider: " + cp);
        if (cp != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Need to coordinate: " + cp.needToCoordinateServices());
        }

        // If the ConfigurationProvider is a DefaultConfigurationProvider, then we are operating in a UT environment.
        if (cp != null && !cp.needToCoordinateServices()) {
            recoverNow = true;
        } else if (_localRecoveryFailed) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Local recovery has already been marked as failed, do not attempt again");
        } else {
            if (cp != null && cp.isSQLRecoveryLog())
                _requireDataSourceActive = true;
            boolean isDataSourceFactorySet = false;
            if (cp != null && cp.isDataSourceFactorySet())
                isDataSourceFactorySet = true;
            // Trace the set of flags that determine whether we can start recovery now.
            if (tc.isDebugEnabled())
                Tr.debug(tc, "_requireRecoveryLogFactory: " + _requireDataSourceActive +
                             ", _waitForRecovery: " + _waitForRecovery +
                             ", _tmsReady: " + _tmsReady +
                             ", _recoveryLogServiceReady: " + _recoveryLogServiceReady +
                             ", _dataSourceFactorySet: " + isDataSourceFactorySet +
                             ", _recoveryLogFactoryReady: " + _recoveryLogFactoryReady);

            if (_requireDataSourceActive)
                // If logging to a database then we need the full set of services in place before we can start recovery
                recoverNow = _tmsReady && _xaResourceFactoryReady && _recoveryLogServiceReady && _recoveryLogFactoryReady && isDataSourceFactorySet;
            else if (_waitForRecovery)
                // If the waitForRecovery flag has been specified then we need the full set of services in place before we can start recovery
                recoverNow = _tmsReady && _recoveryLogServiceReady && _xaResourceFactoryReady && _recoveryLogFactoryReady; // FOR NOW && _dataSourceFactoryReady;
            else
                recoverNow = _tmsReady && _recoveryLogServiceReady;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "ableToStartRecoveryNow", recoverNow);
        return recoverNow;
    }

    private void rollbackTransaction(Transaction tran) {
        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.entry(tc, "rollbackTransaction", tran);

        try {
            ((TransactionImpl) tran).timeoutTransaction(false);
        } catch (Throwable ex) {
            FFDCFilter.processException(ex, "com.ibm.tx.jta.util.impl.TxTMHelper.rollbackTransaction", "831", this);
            if (TraceComponent.isAnyTracingEnabled() && tc.isDebugEnabled())
                Tr.debug(tc, "Unexpected exception caught during transaction ROLLBACK!", ex);
        }

        if (TraceComponent.isAnyTracingEnabled() && tc.isEntryEnabled())
            Tr.exit(tc, "rollbackTransaction");
    }
}
