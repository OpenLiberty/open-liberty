package com.ibm.tx.jta.util;

/*******************************************************************************
 * Copyright (c) 2002, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;

import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;

import com.ibm.tx.TranConstants;
import com.ibm.tx.config.ConfigurationProvider;
import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.tx.jta.TransactionManagerFactory;
import com.ibm.tx.jta.impl.EventSemaphore;
import com.ibm.tx.jta.impl.LocalTIDTable;
import com.ibm.tx.jta.impl.RecoveryManager;
import com.ibm.tx.jta.impl.TranManagerSet;
import com.ibm.tx.jta.impl.TransactionImpl;
import com.ibm.tx.jta.impl.TxRecoveryAgentImpl;
import com.ibm.tx.jta.impl.UserTransactionImpl;
import com.ibm.tx.util.TMHelper;
import com.ibm.tx.util.TMService;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.Transaction.UOWCurrent;
import com.ibm.ws.recoverylog.spi.RecLogServiceImpl;
import com.ibm.ws.recoverylog.spi.RecoveryDirector;
import com.ibm.ws.recoverylog.spi.RecoveryDirectorFactory;
import com.ibm.ws.uow.UOWScopeCallback;
import com.ibm.ws.uow.UOWScopeCallbackAgent;
import com.ibm.wsspi.tx.UOWEventListener;

public class TxTMHelper implements TMService, UOWScopeCallbackAgent {
    private static final TraceComponent tc = Tr.register(TxTMHelper.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private static TMService.TMStates _state = TMService.TMStates.INACTIVE;

    private static TxRecoveryAgentImpl _recoveryAgent;

    protected final static EventSemaphore _asyncRecoverySemaphore = new EventSemaphore();

    protected static RuntimeException _resyncException;

    protected RecLogServiceImpl _recLogService;

    protected RecoveryDirector _recoveryDirector;

    private UOWEventListener _uowEventListener;

    protected boolean _recoverDBLogStarted = false;

    protected String _recoveryIdentity = null;
    protected String _recoveryGroup = null;

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

    protected TxTMHelper(boolean dummy) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "NOT Adding shutdown hook");
        TMHelper.setTMService(this);
    }

    // methods to handle dependency injection in osgi environment
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

    protected void unsetConfigurationProvider(ConfigurationProvider p) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "unsetConfigurationProvider", p);
        if (p != null) {
            // Used to test whether we are logging to an RDBMS - if so a ResourceFactory will have been
            // configured
            if (p.getResourceFactory() == null) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Logging to a filesytem, shutdown now");
                // Where transactions are logged to an RDBMS, shutdown is driven at the point where
                // the DataSource Service is being unset.
                try {
                    shutdown();
                } catch (Exception e) {
                    FFDCFilter.processException(e, "com.ibm.tx.jta.util.TxTMHelper.unsetConfigurationProvider", "138", this);
                }
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "unsetConfigurationProvider");
    }

    @Override
    public Object runAsSystem(PrivilegedExceptionAction a) throws PrivilegedActionException {
        return AccessController.doPrivileged(a);
    }

    @Override
    public Object runAsSystemOrSpecified(PrivilegedExceptionAction a) throws PrivilegedActionException {
        return runAsSystem(a);
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

    /**
     * Non-WAS version.
     * Initialize the configuration and the recovery service.
     *
     */
    @Override
    public void start(boolean waitForRecovery) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "start", waitForRecovery);

        // Note that _recoverDBLogStarted is always false if we are logging to a filesystem.
        // If we are logging to an RDBMS, then we will start the TM which will spin off a thread
        // to perform recovery processing. Unfortunately, this latter thread, as part of
        // DataSource processing, will also attempt to start the TM as a result of registering
        // ResourceInfo. This flag guards against that eventuality.
        if (!_recoverDBLogStarted) {
            synchronized (this) {
                TMHelper.setTMService(this);
                ConfigurationProviderManager.start();

                // Now that the config is loaded, initalize trace
                Tr.reinitializeTracer();

                // Test whether we are logging to an RDBMS - if so a ResourceFactory will have been configured
                final ConfigurationProvider cp = ConfigurationProviderManager.getConfigurationProvider();
                if (cp != null && cp.getResourceFactory() != null) {
                    _recoverDBLogStarted = true;
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Tran Logging to an RDBMS set recoverDBLogStarted flag");
                }

                if (getState() != TMService.TMStates.INACTIVE && getState() != TMService.TMStates.STOPPED) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "start", "Already started");
                    return;
                }

                setResyncException(null);
                _recLogService = new RecLogServiceImpl();

                // Create the Recovery Director
                _recoveryDirector = RecoveryDirectorFactory.createRecoveryDirector();

                // For cloud support, retrieve recovery identity from the configuration if it is defined.
                if (cp != null) {
                    _recoveryIdentity = cp.getRecoveryIdentity();
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "RecoveryIdentity is ", _recoveryIdentity);
                    _recoveryGroup = cp.getRecoveryGroup();
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "recoveryGroup is ", _recoveryGroup);
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
                        Tr.exit(tc, "start", se);
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

                // For now I'll code such that the presence of a RecoveryIdentity attribute says that we are operating in the Cloud
                if (_recoveryIdentity != null && !_recoveryIdentity.isEmpty()) {
                    _recLogService.setPeerRecoverySupported(true);
                    txAgent.setPeerRecoverySupported(true);
                    // Override the disable2PC property if it has been set
                    TransactionImpl.setDisable2PCDefault(false);
                    Tr.audit(tc, "WTRN0108I: Server with identity " + _recoveryIdentity + " is monitoring its peers for Transaction Peer Recovery");
                }

                //TODO: We don't currently use the recoveryGroup....but in due course we will, so retain this
                // code snippet
                if (_recoveryGroup != null && !_recoveryGroup.isEmpty()) {
                    txAgent.setRecoveryGroup(_recoveryGroup);

                    _recLogService.setRecoveryGroup(_recoveryGroup);
                }

                setRecoveryAgent(txAgent);

                // Fake recovery only mode if we're to wait
                RecoveryManager._waitForRecovery = waitForRecovery;

                // Kick off recovery
                _recLogService.start();

                // Defect RTC 99071. Don't make the STATE transition until recovery has been fully
                // initialised, after replay completion but before resync completion.
                setState(TMService.TMStates.RECOVERING);

                if (waitForRecovery) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Waiting for completion of asynchronous recovery");
                    _asyncRecoverySemaphore.waitEvent();
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
                            Tr.exit(tc, "start", se);
                        throw (SystemException) se;
                    }
                } // eof if waitForRecovery
            } // eof synchronized block
        } // eof if !_recoverDBLogStarted
        else if (tc.isDebugEnabled())
            Tr.debug(tc, "Tran Logging to an RDBMS and START processing is in progress");

        if (tc.isEntryEnabled())
            Tr.exit(tc, "start");
    }

    private synchronized void shutdown(boolean explicit, int timeout) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "shutdown", new Object[] { explicit, timeout });

        if ((_state != TMService.TMStates.STOPPED) && (_state != TMService.TMStates.INACTIVE)) {
            // Ensure no new transactions can start
            setState(TMService.TMStates.STOPPING);

            // If timeout 0, don't wait at all
            if (timeout != 0) {
                // Wait till running transactions have stopped
                // If timeout <= 0 wait forever if necessary
                int timeToWait = timeout;
                int timeSlept = 0;
                while (LocalTIDTable.getAllTransactions().length > 0) {
                    if (timeout < 0 || timeToWait-- > 0) {
                        try {
                            // Sleep for a second at a time
                            Thread.sleep(1000);
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Waited " + ++timeSlept + " seconds for transactions to finish");
                        } catch (InterruptedException e) {
                        }
                    } else {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Gave up waiting for transactions to finish after " + ++timeSlept + " seconds");
                        break;
                    }
                }
            }

            // ConfigurationProviderManager.stop(true);

            _recoveryAgent.stop(false);

            _recLogService.stop();

            TransactionManager tm = TransactionManagerFactory.getTransactionManager();
            ((TranManagerSet) tm).cleanup();

            setRecoveryAgent(null);

            RecoveryDirectorFactory.reset();

            LocalTIDTable.clear();

            try {
                _asyncRecoverySemaphore.waitEvent();
            } catch (InterruptedException e) {
            }

            setResyncException(null);
            _asyncRecoverySemaphore.clear();

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

    public synchronized static void resyncComplete(RuntimeException r) {
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
            Tr.entry(tc, "checkTMState");
        if (_state != TMService.TMStates.ACTIVE) {
            if (_state == TMService.TMStates.RECOVERING) {
                // Check that the initial phase of recovery is complete
            } else if (_state == TMService.TMStates.INACTIVE) {
                try {
                    TMHelper.start();
                } catch (Exception e) {
                    final NotSupportedException nse = new NotSupportedException();
                    nse.initCause(e);
                    throw nse;
                }
            } else if (_state == TMService.TMStates.STOPPING) {
                throw new NotSupportedException("JTM is stopping");
            } else if (_state == TMService.TMStates.STOPPED) {
                throw new NotSupportedException("JTM is stopped");
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
        ((UOWCurrent) TranManagerSet.instance()).unsetUOWEventListener(el);
    }

    protected TxRecoveryAgentImpl createRecoveryAgent(RecoveryDirector recoveryDirector) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createRecoveryAgent", recoveryDirector);

        TxRecoveryAgentImpl txAgent = new TxRecoveryAgentImpl(recoveryDirector);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createRecoveryAgent", txAgent);

        return txAgent;
    }
}