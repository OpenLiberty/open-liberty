/*******************************************************************************
 * Copyright (c) 2002, 2025 IBM Corporation and others.
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
package com.ibm.tx.jta.impl;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import com.ibm.tx.TranConstants;
import com.ibm.tx.config.ConfigurationProvider;
import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.tx.jta.config.DefaultConfigurationProvider;
import com.ibm.tx.jta.util.TranLogConfiguration;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.Transaction.JTA.Util;
import com.ibm.ws.Transaction.JTS.Configuration;
import com.ibm.ws.ffdc.FFDCFilter;
import com.ibm.ws.recoverylog.spi.ClientId;
import com.ibm.ws.recoverylog.spi.CustomLogProperties;
import com.ibm.ws.recoverylog.spi.FailureScope;
import com.ibm.ws.recoverylog.spi.FileFailureScope;
import com.ibm.ws.recoverylog.spi.FileLogProperties;
import com.ibm.ws.recoverylog.spi.HeartbeatLog;
import com.ibm.ws.recoverylog.spi.InternalLogException;
import com.ibm.ws.recoverylog.spi.InvalidFailureScopeException;
import com.ibm.ws.recoverylog.spi.InvalidLogPropertiesException;
import com.ibm.ws.recoverylog.spi.LeaseInfo;
import com.ibm.ws.recoverylog.spi.LogProperties;
import com.ibm.ws.recoverylog.spi.PeerLeaseTable;
import com.ibm.ws.recoverylog.spi.RecoveryAgent;
import com.ibm.ws.recoverylog.spi.RecoveryDirector;
import com.ibm.ws.recoverylog.spi.RecoveryDirectorFactory;
import com.ibm.ws.recoverylog.spi.RecoveryFailedException;
import com.ibm.ws.recoverylog.spi.RecoveryLog;
import com.ibm.ws.recoverylog.spi.RecoveryLogManager;
import com.ibm.ws.recoverylog.spi.SharedServerLeaseLog;
import com.ibm.ws.recoverylog.spi.TerminationFailedException;
import com.ibm.wsspi.classloading.ClassLoadingService;
import com.ibm.wsspi.resource.ResourceConfig;
import com.ibm.wsspi.resource.ResourceFactory;

public class TxRecoveryAgentImpl implements RecoveryAgent {
    private static final TraceComponent tc = Tr.register(TxRecoveryAgentImpl.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private static final int transactionLogRLI = 1; // 169107
    private static final int partnerLogRLI = 2; // 169107

    private static final int TRANSACTION_RECOVERYLOG_FORMAT_VERSION = 1;

    protected RecoveryDirector _recoveryDirector;
    private RecoveryManager _recoveryManager;

    protected final HashMap<String, FailureScopeController> failureScopeControllerTable = new HashMap<String, FailureScopeController>();

    private RecoveryLog _homePartnerLog;
    // In the special case where we are operating in the cloud, we'll also work with a "lease" log
    SharedServerLeaseLog _leaseLog;

    private String _recoveryGroup;
    private boolean _isPeerRecoverySupported;

    protected String localRecoveryIdentity;

    private ClassLoadingService clService;

    /**
     * Flag to indicate whether the server is stopping.
     */
    volatile private boolean _serverStopping;

    protected TxRecoveryAgentImpl() {
    }

    private static ThreadLocal<Boolean> _replayThread = new ThreadLocal<Boolean>();

    public TxRecoveryAgentImpl(RecoveryDirector rd) throws Exception {
        _recoveryDirector = rd;

        final RecoveryLogManager rlm = rd.registerService(this, ClientId.RASEQ_TRANSACTIONSERVICE);

        // Save the log manager for use by callbacks to access logs
        Configuration.setLogManager(rlm);

        final FailureScope currentFailureScope = rd.currentFailureScope();
        Configuration.setServerName(currentFailureScope.serverName());
        localRecoveryIdentity = currentFailureScope.serverName();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "TxRecoveryAgentImpl constructor - localRecoveryIdentity set to ", localRecoveryIdentity);
        final FailureScopeController fsc = createFailureScopeController(currentFailureScope);
        failureScopeControllerTable.put(currentFailureScope.serverName(), fsc); // @372790C
        Configuration.setFailureScopeController(fsc);

        byte[] newApplId = new byte[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };

        // RTC 179941
        ConfigurationProvider cp = ConfigurationProviderManager.getConfigurationProvider();

        // Set the default value of the ThreadLocal to false. It will be set to true in the thread driving replay.
        _replayThread.set(false);

        // In the normal Liberty runtime the Applid will have been set into the JTMConfigurationProvider by the
        // TransactionManagerService. We additionally can set the applid here for the benefit of the unittest framework.
        byte[] applId = cp.getApplId();
        if (applId == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "TXAGENT, cp applid null - {0} set applid - {1}", cp, Util.toHexString(newApplId));
            cp.setApplId(newApplId);
            Configuration.setApplId(newApplId);
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "TXAGENT, do not reset cp - {0} set applid - {1}", cp, Util.toHexString(applId));
            Configuration.setApplId(applId);
        }
    }

    @Override
    public void agentReportedFailure(int clientId, FailureScope failureScope) {
    }

    @Override
    public int clientIdentifier() {
        return ClientId.RLCI_TRANSACTIONSERVICE;
    }

    @Override
    public String clientName() {
        return ClientId.RLCN_TRANSACTIONSERVICE;
    }

    @Override
    public int clientVersion() {
        return TRANSACTION_RECOVERYLOG_FORMAT_VERSION;
    }

    /**
     * Returns a flag to indicate if the client wants file locking to be DISABLED for
     * file based recovery logs. This method is essentially temporary until the RLS
     * has the WCCM basis to make this chocie for itself. If any client returns TRUE
     * then file locking will be DISABLED, however currently only the transaction service
     * recovery agent is actually checked.
     *
     * by default, file locking is ENABLED.
     *
     * @return boolean
     */
    @Override
    public boolean disableFileLocking() {
        return false;
    }

    @Override
    public void initiateRecovery(FailureScope fs) throws RecoveryFailedException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "initiateRecovery", this, fs);
        String recoveredServerIdentity = fs.serverName();
        FailureScopeController fsc = null;
        ConfigurationProvider cp = null;
        RecoveryLog transactionLog = null;
        RecoveryLog partnerLog = null;

        final boolean localRecovery = recoveredServerIdentity.equals(localRecoveryIdentity);

        try {
            synchronized (this) {

                // Big message if Peer recovering, just debug otherwise
                if (!localRecovery) {
                    // If we are attempting to recover a peer and the home server is stopping, then do not continue
                    if (_serverStopping) {
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "initiateRecovery", "server stopping");
                        throw new RecoveryFailedException("server stopping");
                    }
                    Tr.audit(tc, "WTRN0108I: Recovery initiated for server {0}", recoveredServerIdentity);
                } else {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Recovery initiated for server {0}", recoveredServerIdentity);
                }

                // Determine whether we are dealing with a custom log configuration (e.g. WXS or JDBC)
                boolean isCustom = false;

                cp = ConfigurationProviderManager.getConfigurationProvider();
                if (cp == null) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "initiateRecovery", "ConfigurationProvider is null");
                    throw new RecoveryFailedException("ConfigurationProvider is null");
                }

                String logDir = cp.getTransactionLogDirectory();
                int logSize = cp.getTransactionLogSize();

                if (logDir.startsWith("custom")) {
                    isCustom = true;
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Found a custom tran log directory");
                }

                // Retrieve the recovery log configuration information for this failure scope. This will
                // be retrieved from WCCM if its not been encountered before on this run.
                TranLogConfiguration tlc = null;

                // We now need to determine the properties and instantiate an appropriate TranLogConfiguration object
                if (isCustom) {
                    // Create "custom" tlc.
                    tlc = createCustomTranLogConfiguration(recoveredServerIdentity, logDir, _isPeerRecoverySupported);
                } else {
                    // Create File tlc
                    tlc = createFileTranLogConfiguration(recoveredServerIdentity, fs, logDir, logSize, _isPeerRecoverySupported);
                }

                // Retrieve any existing failureScopeController for this FailureScope. The only occasion where we expect to find
                // one is local recovery where its pre-created at server startup. If it does exist in this case it should
                // not have been populated with a RecoveryManager.
                fsc = failureScopeControllerTable.get(recoveredServerIdentity);

                if (fsc != null) {
                    if (fsc.getRecoveryManager() != null) {
                        if (_isPeerRecoverySupported) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Where peer recovery is supported, a pre-existing RM is ok");

                            //TODO: Just how safe is this? suppose some asynch recovery is still going on?
                            // If I try to terminate current recovery by calling terminateRecovery() a can of worms opens up
                            // but do I need some kind of better policing of this?
                        } else {
                            if (tc.isEntryEnabled())
                                Tr.exit(tc, "initiateRecovery", "already recovering failure scope " + fs);
                            throw new RecoveryFailedException("Already recovering failure scope " + fs);
                        }
                    }
                } else {
                    try {
                        fsc = createFailureScopeController(fs);
                    } catch (Exception exc) {
                        FFDCFilter.processException(exc, "com.ibm.tx.jta.impl.TxRecoveryAgentImpl.initiateRecovery", "259", this);
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Exception caught whist creating FailureScopeController", exc);
                        throw new RecoveryFailedException(exc);
                    }
                    failureScopeControllerTable.put(recoveredServerIdentity, fsc);
                }

                // If we are going to recover the home server then further transactions can be started. For this we need to
                // generate XIDs which contain the servers APPLID. As a result we must pass to the recovery manager the
                // defaults that have been stored in the Configuration class. These may be overwritten later if this is
                // a real recovery rather than a cold start - in which case the recovery log would contain the APPLID
                // used on the last run of the server. For peer recovery, we ignore these fields and just leave them null.
                cp = ConfigurationProviderManager.getConfigurationProvider();
                if (cp == null) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "initiateRecovery", "ConfigurationProvider is null");
                    throw new RecoveryFailedException("ConfigurationProvider is null");
                }
                byte[] applId = cp.getApplId();
                int epoch = Configuration.getCurrentEpoch();

                // As long as a physical location for the recovery logs is found, and logging is enabled (ie user
                // has not specified ";0" as the log location string for a file based log) then create the
                if ((tlc != null) && (tlc.enabled())) {
                    final LogProperties transactionLogProps;
                    final LogProperties partnerLogProps;

                    if (tlc.type() == TranLogConfiguration.TYPE_CUSTOM) {
                        // Set up CustomLogProperties
                        transactionLogProps = new CustomLogProperties(transactionLogRLI, TransactionImpl.TRANSACTION_LOG_NAME, tlc.customId(), tlc.customProperties());
                        partnerLogProps = new CustomLogProperties(partnerLogRLI, TransactionImpl.PARTNER_LOG_NAME, tlc.customId(), tlc.customProperties());
                        // For Liberty we need to retrieve the resource factory associated with the non transactional datasource
                        // and set it into the CustomLogProperties. This specific property is currently only referenced in the Liberty
                        // specific SQLNonTransactionalDataSource class, which overrides the tWAS equivalent.
                        cp = ConfigurationProviderManager.getConfigurationProvider();
                        if (cp == null) {
                            if (tc.isEntryEnabled())
                                Tr.exit(tc, "initiateRecovery", "ConfigurationProvider is null");
                            throw new RecoveryFailedException("ConfigurationProvider is null");
                        }
                        final ResourceFactory nontranDSResourceFactory = cp.getResourceFactory();
                        ((CustomLogProperties) transactionLogProps).setResourceFactory(nontranDSResourceFactory);
                        ((CustomLogProperties) partnerLogProps).setResourceFactory(nontranDSResourceFactory);
                        final ResourceConfig resConfig = cp.getResourceConfig();
                        ((CustomLogProperties) transactionLogProps).setResourceConfig(resConfig);
                        ((CustomLogProperties) partnerLogProps).setResourceConfig(resConfig);
                    } else {
                        // Set up FileLogProperties
                        String recLogDirStem = tlc.expandedLogDirectory().trim();
                        Path tranLogDirToUse = Paths.get(recLogDirStem, TransactionImpl.TRANSACTION_LOG_NAME);
                        transactionLogProps = new FileLogProperties(transactionLogRLI, TransactionImpl.TRANSACTION_LOG_NAME, tranLogDirToUse, tlc.logFileSize(), recLogDirStem);

                        Path partnerLogDirToUse = Paths.get(recLogDirStem, TransactionImpl.PARTNER_LOG_NAME);
                        partnerLogProps = new FileLogProperties(partnerLogRLI, TransactionImpl.PARTNER_LOG_NAME, partnerLogDirToUse, tlc.logFileSize());
                    }

                    final RecoveryLogManager rlm = Configuration.getLogManager();

                    //
                    // Create the Transaction log
                    //
                    transactionLog = rlm.getRecoveryLog(fs, transactionLogProps);

                    //
                    // Create the Partner (XAResources) log
                    //
                    partnerLog = rlm.getRecoveryLog(fs, partnerLogProps);
                    if (localRecovery) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Set the home partnerLog to " + partnerLog);
                        _homePartnerLog = partnerLog;
                    }

                    // In the special case where we support tx peer recovery (eg for operating in the cloud), we'll also work with a "lease" log
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Test to see if peer recovery is supported - {0}", _isPeerRecoverySupported);
                    cp = ConfigurationProviderManager.getConfigurationProvider();
                    if (cp == null) {
                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "initiateRecovery", "ConfigurationProvider is null");
                        throw new RecoveryFailedException("ConfigurationProvider is null");
                    }

                    if (_isPeerRecoverySupported) {
                        _leaseLog = rlm.getLeaseLog(localRecoveryIdentity,
                                                    _recoveryGroup,
                                                    cp.getLeaseCheckInterval(),
                                                    cp.getLeaseCheckStrategy(),
                                                    cp.getLeaseLength(),
                                                    transactionLogProps);
                    }
                }

                //
                // Create the RecoveryManager and associate it with the logs
                //
                if (fsc != null) {
                    fsc.createRecoveryManager(this, transactionLog, partnerLog, null, applId, epoch);

                    // Initiate recovery on a separate thread.
                    // Cannot use default threadpool threads as these are subject to hang detection and if we
                    // fail to recover, we leave the group, suspend the thread awhile and the rejoin the group.
                    // The hang detection logic will scream if we suspend too long.  Also do not want to drain
                    // default thread pool, nor do we want to create recovery pools that may never get used and
                    // just absorb resource.

                    _recoveryManager = fsc.getRecoveryManager();
                }

                if (localRecovery && _isPeerRecoverySupported) {
                    if (_leaseLog == null) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Cannot lock server's own logs");
                        Object[] errorObject = new Object[] { localRecoveryIdentity };
                        RecoveryFailedException rex = new RecoveryFailedException("Cannot lock server's own logs");
                        Tr.audit(tc, "CWRLS0008_RECOVERY_LOG_FAILED",
                                 errorObject);
                        Tr.info(tc, "CWRLS0009_RECOVERY_LOG_FAILED_DETAIL", rex);

                        // Drive recovery failure processing
                        _recoveryManager.recoveryFailed(rex);

                        // Check the system property but by default we want the server to be shutdown if we, the server
                        // that owns the logs is not able to recover them. The System Property supports the tWAS style
                        // of processing.
                        if (!doNotShutdownOnRecoveryFailure()) {
                            cp = ConfigurationProviderManager.getConfigurationProvider();
                            if (cp == null) {
                                if (tc.isEntryEnabled())
                                    Tr.exit(tc, "initiateRecovery", "ConfigurationProvider is null");
                                throw new RecoveryFailedException("ConfigurationProvider is null");
                            }
                            cp.shutDownFramework();
                        }

                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "initiateRecovery", rex);

                        // Output a message as to why we are terminating the server as in
                        Tr.error(tc, "CWRLS0024_EXC_DURING_RECOVERY", rex);
                        throw rex;
                    }
                }

                _recoveryManager.configurePeerRecovery(_leaseLog, _recoveryGroup, localRecoveryIdentity);
            }

            synchronized (this) {
                // Check that peer recovery is continuing as it may be interrupted by shutdown of the home server in which case we stop recovery processing.
                if (!localRecovery && _serverStopping) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "initiateRecovery", "server stopping");
                    throw new RecoveryFailedException("server stopping");
                }

                final Thread t = AccessController.doPrivileged(new PrivilegedAction<Thread>() {
                    @Override
                    public Thread run() {
                        return new Thread(_recoveryManager, "Recovery Thread");
                    }
                });

                AccessController.doPrivileged(new PrivilegedExceptionAction<Void>() {

                    @Override
                    public Void run() throws RecoveryFailedException {
                        // If we're not unit testing, set a ThreadContextClassLoader on the recovery thread so SSL classes can be loaded
                        ConfigurationProvider cp = ConfigurationProviderManager.getConfigurationProvider();
                        if (cp == null) {
                            if (tc.isEntryEnabled())
                                Tr.exit(tc, "initiateRecovery.run", "ConfigurationProvider is null");
                            throw new RecoveryFailedException("ConfigurationProvider is null");
                        }

                        if (!(cp.getClass().getCanonicalName().equals(DefaultConfigurationProvider.class.getCanonicalName()))) {
                            final ClassLoader cl = getThreadContextClassLoader(TxRecoveryAgentImpl.class);
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Setting Context ClassLoader on " + t.getName() + " (" + String.format("%08X", t.getId()) + ")", cl);

                            t.setContextClassLoader(cl);
                        } else {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "unit testing so not setting Context ClassLoader on " + t.getName() + " (" + String.format("%08X", t.getId()) + ")");
                        }

                        return null;
                    }
                });

                t.start();

                // Once we have got things going on another thread, tell the recovery directory that recovery is "complete". This
                // essentially means that other components can have a go at recovery now.
                _recoveryDirector.serialRecoveryComplete(this, fs);
            }

            //RTC170534 - wait for Replay Completion before spawning the timeout manager to monitor leases.
            // Peer recovery may be interrupted by shutdown of the home server in which case we stop replay processing.
            if (!localRecovery && _serverStopping) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "initiateRecovery", "server stopping");
                throw new RecoveryFailedException("server stopping");
            }

            if (fsc != null)
                fsc.getRecoveryManager().waitForReplayCompletion(localRecovery);

            // Replay has completed but recovery processing may have failed
            boolean recoveryFailed = false;
            if (fsc != null)
                recoveryFailed = fsc.getRecoveryManager().recoveryFailed();

            if (tc.isDebugEnabled())
                Tr.debug(tc, "Replay completed but did recovery fail -  " + recoveryFailed);

            // Peer recovery may be interrupted by shutdown of the home server in which case we stop recovery processing.
            if (!localRecovery) {
                if (!_serverStopping) {
                    if (fsc != null) {
                        if (fsc.getRecoveryManager() != null)
                            fsc.getRecoveryManager().waitForRecoveryCompletion(localRecovery);
                    }
                } else {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "initiateRecovery", "server stopping");
                    throw new RecoveryFailedException("server stopping");
                }
            }

            // If we have a lease log then we need to set it into the recovery manager, so that it too will be processed.
            synchronized (this) {
                // Again, peer recovery may be interrupted by shutdown of the home server in which case we stop recovery processing.
                if (!localRecovery && _serverStopping) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "initiateRecovery", "server stopping");
                    throw new RecoveryFailedException("server stopping");
                }

                if (_leaseLog != null) {
                    // If Recovery Failed, then by default, if this is the home server, we shall bring down the Liberty Server
                    RecoveryManager rm; // RecoverManager might be removed on another thread
                    if (fsc != null && (rm = fsc.getRecoveryManager()) != null) {
                        if (rm.recoveryFailed()) {
                            // Check the system property but by default we want the server to be shutdown if we, the server
                            // that owns the logs is not able to recover them. The System Property supports the tWAS style
                            // of processing.
                            if (localRecovery) {
                                if (!doNotShutdownOnRecoveryFailure()) {
                                    cp = ConfigurationProviderManager.getConfigurationProvider();
                                    if (cp == null) {
                                        if (tc.isEntryEnabled())
                                            Tr.exit(tc, "initiateRecovery", "ConfigurationProvider is null");
                                        throw new RecoveryFailedException("ConfigurationProvider is null");
                                    }
                                    cp.shutDownFramework();
                                }

                                RecoveryFailedException rex = new RecoveryFailedException("Home server recovery failed in peer environment");
                                if (tc.isEntryEnabled())
                                    Tr.exit(tc, "initiateRecovery", rex);

                                // Output a message as to why we are terminating the server as in
                                Tr.error(tc, "CWRLS0024_EXC_DURING_RECOVERY", rex.toString());
                                throw rex;
                            }
                        }
                    }

                    // Only spawn timeout manager if this is the local server and recovery succeeded
                    if (localRecovery) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Local server recovery identity so spawn lease timeout manager");

                        cp = ConfigurationProviderManager.getConfigurationProvider();
                        if (cp == null) {
                            if (tc.isEntryEnabled())
                                Tr.exit(tc, "initiateRecovery", "ConfigurationProvider is null");
                            throw new RecoveryFailedException("ConfigurationProvider is null");
                        }
                        LeaseTimeoutManager.setTimeouts(_leaseLog,
                                                        recoveredServerIdentity,
                                                        _recoveryGroup,
                                                        this,
                                                        _recoveryDirector,
                                                        cp.getLeaseLength() * cp.getLeaseRenewalThreshold() / 100,
                                                        cp.getLeaseCheckInterval());
                    }
                } else {
                    // Not a peer recovery environment
                    if (fsc != null && fsc.getRecoveryManager().recoveryFailed()) {
                        RecoveryFailedException rex = new RecoveryFailedException("Server recovery failed");

                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "initiateRecovery", rex);

                        // Output a message as to why we are terminating the server as in
                        Tr.error(tc, "CWRLS0024_EXC_DURING_RECOVERY", rex.toString());
                        throw rex;
                    }
                }
            }
        } catch (InvalidFailureScopeException | InvalidLogPropertiesException | URISyntaxException | PrivilegedActionException e) {
            if (!_serverStopping)
                FFDCFilter.processException(e, "com.ibm.tx.jta.impl.TxRecoveryAgentImpl.initiateRecovery", "586", this);
            Tr.error(tc, "WTRN0016_EXC_DURING_RECOVERY", e);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "initiateRecovery", e);
            throw new RecoveryFailedException(e);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "initiateRecovery");
    }

    /**
     * @param cl
     * @return
     * @throws RecoveryFailedException
     */
    private ClassLoader getThreadContextClassLoader(Class<? extends TxRecoveryAgentImpl> cl) throws RecoveryFailedException {
        return getClassLoadingService().createThreadContextClassLoader(cl.getClassLoader());
    }

    public synchronized ClassLoadingService getClassLoadingService() throws RecoveryFailedException {
        if (clService == null) {
            clService = getService(ClassLoadingService.class);
        }
        return clService;
    }

    /**
     * @param <T>
     * @param service
     * @return
     * @throws RecoveryFailedException
     */
    private <T> T getService(final Class<T> service) throws RecoveryFailedException {
        T impl = null;

        BundleContext context = FrameworkUtil.getBundle(service).getBundleContext();

        ServiceReference<T> ref = context.getServiceReference(service);
        if (ref != null) {
            impl = context.getService(ref);
        } else {
            throw new RecoveryFailedException("Unable to locate service: " + service);
        }

        return impl;
    }

    /**
     * @param fs
     */
    @Override
    public void terminateRecovery(FailureScope fs) throws TerminationFailedException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "terminateRecovery", fs);

        final String terminateServerName = fs.serverName();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "The transaction service has received a request to terminate recovery processing for server " + terminateServerName);

        RecoveryDirector recoveryDirector = null;
        try {
            recoveryDirector = RecoveryDirectorFactory.recoveryDirector();
        } catch (InternalLogException exc) {
            FFDCFilter.processException(exc, "com.ibm.tx.jta.impl.TxRecoveryAgentImpl.terminateRecovery", "650", this);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "terminateRecovery");
            throw new TerminationFailedException(exc);
        }

        // Check to see if this is a request to stop processing the local failure scope. The RLS
        // should never ask us to "drop" our own recovery logs.
        final boolean terminatingThisServer = (terminateServerName.equals(Configuration.getServerName()));
        if (terminatingThisServer) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Received unexpected request to terminate recovery processing for local failure scope");
            if (tc.isEntryEnabled())
                Tr.exit(tc, "terminateRecovery");
            throw new TerminationFailedException();
        }

        // Find the relevant failure scope controller instance
        final FailureScopeController fsc = failureScopeControllerTable.remove(terminateServerName);

        // Direct it to terminate recovery processing.
        if (fsc != null) {
            fsc.shutdown(false);
        }

        // Recovery terminate is complete. All that remains is to let the recovery director know.
        try {
            recoveryDirector.terminationComplete(this, fs);
        } catch (InvalidFailureScopeException exc) {
            // There is nothing much that can be done here. This is the very last stage of recovery termination
            // and if this occurs then this indicates that there is a defect in the code. This exception is
            // raised by the RLS in the event that ot does not recognize this failure scope and recovery agent
            // conbindation.
            FFDCFilter.processException(exc, "com.ibm.tx.jta.impl.TxRecoveryAgentImpl.terminateRecovery", "683", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Unable to indicate termination completion to recovery director: " + exc);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "terminateRecovery");
            throw new TerminationFailedException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "terminateRecovery");
    }

    public synchronized void stop(boolean immediate) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "stop", new Object[] { this, Boolean.valueOf(immediate) });

        // Set the flag to signify that the server is stopping
        _serverStopping = true;

        // Stop lease timeout alarm popping when server is on its way down
        LeaseTimeoutManager.stopTimeout();

        // Drive the serverStopping() method on the SQLMultiScopeRecoveryLog if appropriate. This will manage
        // the cancelling of the HADB Log Availability alarm
        if (_homePartnerLog != null && _homePartnerLog instanceof HeartbeatLog) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "The log is a Heartbeatlog");
            HeartbeatLog heartbeatLog = (HeartbeatLog) _homePartnerLog;
            heartbeatLog.serverStopping();
        }

        // The entire server is shutting down. All recovery/peer recovery processing must be stopped. Sping
        // through all known failure scope controllers (which includes the local failure scope if we started
        // processing recovery for it) and tell them to shutdown.
        final Collection<FailureScopeController> failureScopeControllerTableValues = failureScopeControllerTable.values();
        final Iterator<FailureScopeController> failureScopeControllerTableValuesIterator = failureScopeControllerTableValues.iterator();

        while (failureScopeControllerTableValuesIterator.hasNext()) {
            final FailureScopeController fsc = failureScopeControllerTableValuesIterator.next();
            fsc.shutdown(immediate);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "stop");
    }

    @Override
    public void logFileWarning(String logname, int bytesInUse, int bytesTotal) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "logFileWarning", new Object[] { logname, bytesInUse, bytesTotal });
    }

    public void setRecoveryGroup(String recoveryGroup) {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setRecoveryGroup", new Object[] { recoveryGroup });
        _recoveryGroup = recoveryGroup;
    }

    @Override
    public String getRecoveryGroup() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getRecoveryGroup", _recoveryGroup);
        return _recoveryGroup;
    }

    /**
     * @param isPeerRecoverySupported the _isPeerRecoverySupported to set
     */
    public void setPeerRecoverySupported(boolean isPeerRecoverySupported) {
        // Rename this variable. Its interpretation in this class is that peer recovery is supported.
        this._isPeerRecoverySupported = isPeerRecoverySupported;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.RecoveryAgent#processLeasesForPeers(com.ibm.ws.recoverylog.spi.FailureScope)
     */
    @Override
    public ArrayList<String> processLeasesForPeers(String recoveryIdentity, String recoveryGroup) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "processLeasesForPeers", new Object[] { this, recoveryIdentity, recoveryGroup });
        ArrayList<String> peersToRecover = null;

        if (_leaseLog != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "work with leaseLog {0}", _leaseLog);

            try {
                // Retrieve peers in the recovery group from lease table
                PeerLeaseTable peerLeaseTable = new PeerLeaseTable();
                _leaseLog.getLeasesForPeers(peerLeaseTable, recoveryGroup);

                //Now extract a list of the peers that need to be recovered
                peersToRecover = peerLeaseTable.getExpiredPeers();

                if (tc.isDebugEnabled()) {
                    if (peersToRecover != null && !peersToRecover.isEmpty()) {
                        Tr.debug(tc, "Have checked leases for " + peerLeaseTable.size() + " peer" + (peerLeaseTable.size() != 1 ? "s" : "") + " in recovery group "
                                     + recoveryGroup);
                        if (peersToRecover.size() > 0) {
                            for (String peer : peersToRecover) {
                                Tr.debug(tc, "Need to recover: " + peer);
                            }
                        }
                    } else {
                        Tr.debug(tc, "No peer servers will be recovered");
                    }
                }
            } catch (Exception e) {
                System.out.println("Caught exception when trying to get leases for peers: " + e);
                e.printStackTrace();
            }
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "processLeasesForPeers", peersToRecover);
        return peersToRecover;
    }

    @Override
    public boolean claimPeerLeaseForRecovery(String recoveryIdentityToRecover, String myRecoveryIdentity, LeaseInfo leaseInfo) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "claimPeerLeaseForRecovery", recoveryIdentityToRecover, myRecoveryIdentity, leaseInfo, this);

        final boolean peerClaimed = _leaseLog.claimPeerLeaseForRecovery(recoveryIdentityToRecover, myRecoveryIdentity, leaseInfo);

        // Release lock if the claim failed
        if (!peerClaimed) {
            try {
                _leaseLog.releasePeerLease(recoveryIdentityToRecover);
            } catch (Exception e) {
                RecoveryFailedException rfex = new RecoveryFailedException(e);
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "claimPeerLeaseForRecovery", rfex);
                throw rfex;
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "claimPeerLeaseForRecovery", peerClaimed);
        return peerClaimed;
    }

    @Override
    public void releasePeerLeaseForRecovery(String recoveryIdentityToRecover) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "releasePeerLeaseForRecovery", recoveryIdentityToRecover, this);

        try {
            _leaseLog.releasePeerLease(recoveryIdentityToRecover);
        } catch (Exception e) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "releasePeerLeaseForRecovery", e);
            throw e;
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "releasePeerLeaseForRecovery");
    }

    /**
     * Creates a custom TranLogConfiguration object appropriate for storing transaction logs in an RDBMS or other custom repository.
     *
     * @param recoveredServerIdentity
     * @param logDir
     * @param isPeerRecoverySupported
     * @return
     * @throws URISyntaxException
     */
    private TranLogConfiguration createCustomTranLogConfiguration(String recoveredServerIdentity, String logDir, boolean isPeerRecoverySupported) throws URISyntaxException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createCustomTranLogConfiguration", recoveredServerIdentity, logDir, this);

        TranLogConfiguration tlc = null;
        final Properties props = new Properties();
        URI logSettingURI = new URI(logDir);
        String scheme = logSettingURI.getScheme();
        String logSetting = logSettingURI.getAuthority();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Scheme read from URI {0}, log setting {1}", scheme, logSetting);
        // For the cloud and peer recovery scenarios, we'll automatically add a suffix that matches the recoveryIdentity
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Test to see if peer recovery is supported - {0}", isPeerRecoverySupported);
        if (isPeerRecoverySupported) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Work with server recovery identity - {0}, reset current logdir", recoveredServerIdentity);

            if (recoveredServerIdentity != null) {
                logDir = "custom://com.ibm.rls.jdbc.SQLRecoveryLogFactory?datasource=Liberty" +
                         ",tablesuffix=" + recoveredServerIdentity;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "log dir is now - {0}", logDir);
            }
        }
        props.setProperty("LOG_DIRECTORY", logDir);
        tlc = new TranLogConfiguration(logSetting, props);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createCustomTranLogConfiguration", tlc);
        return tlc;
    }

    /**
     * Creates a Filesystem TranLogConfiguration object appropriate for storing transaction logs in a filesystem.
     *
     * @param recoveredServerIdentity
     * @param fs
     * @param logDir
     * @param logSize
     * @param isPeerRecoverySupported
     * @return
     * @throws URISyntaxException
     * @throws RecoveryFailedException
     */
    private TranLogConfiguration createFileTranLogConfiguration(String recoveredServerIdentity,
                                                                FailureScope fs,
                                                                String logDir,
                                                                int logSize,
                                                                boolean isPeerRecoverySupported) throws URISyntaxException, RecoveryFailedException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createFileTranLogConfiguration", recoveredServerIdentity, fs, logDir, logSize, this);

        TranLogConfiguration tlc = null;

        if (tc.isDebugEnabled())
            Tr.debug(tc, "Work with server recovery identity - {0}", recoveredServerIdentity);
        // Do we need to reset the logdir?
        if (recoveredServerIdentity.equals(localRecoveryIdentity)) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Local server recovery identity so no need to reset the logDir");
            tlc = new TranLogConfiguration(logDir, logDir, logSize);
        } else {
            // Reset the logdir
            if (fs instanceof FileFailureScope) {
                FileFailureScope ffs = (FileFailureScope) fs;
                LeaseInfo li = ffs.getLeaseInfo();
                if (li != null) {
                    String s = li.getLeaseDetail().getPath();
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Using log directory", s);
                    tlc = new TranLogConfiguration(s, s, logSize);
                }
            }
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createFileTranLogConfiguration", tlc);
        return tlc;
    }

    /**
     * This method retrieves a system property named com.ibm.ws.recoverylog.spi.DoNotShutdownOnRecoveryFailure
     * which allows the server to start with failed recovery logs - non 2PC work may still be performed by the server.
     *
     * @return
     */
    private boolean doNotShutdownOnRecoveryFailure() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "doNotShutdownOnRecoveryFailure");

        boolean doCheck = true;
        Boolean doNotShutdownOnRecoveryFailure = null;

        try {
            doNotShutdownOnRecoveryFailure = AccessController.doPrivileged(
                                                                           new PrivilegedExceptionAction<Boolean>() {
                                                                               @Override
                                                                               public Boolean run() {
                                                                                   Boolean theResult = Boolean.getBoolean("com.ibm.ws.recoverylog.spi.DoNotShutdownOnRecoveryFailure");
                                                                                   if (tc.isDebugEnabled())
                                                                                       Tr.debug(tc, "Have retrieved jvm property with result, {0}", theResult.booleanValue());
                                                                                   return theResult;
                                                                               }
                                                                           });
        } catch (PrivilegedActionException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception getting DoNotShutdownOnRecoveryFailure property", e);
            doNotShutdownOnRecoveryFailure = null;
        }

        if (doNotShutdownOnRecoveryFailure == null)
            doNotShutdownOnRecoveryFailure = Boolean.TRUE;

        doCheck = doNotShutdownOnRecoveryFailure.booleanValue();
        if (tc.isEntryEnabled())
            Tr.exit(tc, "doNotShutdownOnRecoveryFailure", doCheck);
        return doCheck;
    }

    protected FailureScopeController createFailureScopeController(FailureScope currentFailureScope) throws Exception {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createFailureScopeController", currentFailureScope);

        FailureScopeController fsc = new FailureScopeController(currentFailureScope);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createFailureScopeController", fsc);

        return fsc;
    }

    /**
     * Given a FailureScope, return a reference to the corresponding custom partner recovery log.
     *
     * @param fs
     * @return
     */
    @Override
    public HeartbeatLog getHeartbeatLog(FailureScope fs) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getHeartbeatLog", this, fs);
        RecoveryLog partnerLog = null;
        HeartbeatLog heartbeatLog = null;
        String recoveredServerIdentity = null;
        ConfigurationProvider cp = null;
        recoveredServerIdentity = fs.serverName();
        final boolean localRecovery = recoveredServerIdentity.equals(localRecoveryIdentity);
        try {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "getHeartbeatLog for server - {0}", recoveredServerIdentity);

            cp = ConfigurationProviderManager.getConfigurationProvider();
            // Big message if Peer recovering, just debug otherwise
            if (!localRecovery) {
                // If we are attempting to recover a peer and the home server is stopping, then do not continue
                if (_serverStopping) {
                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "getHeartbeatLog", "server stopping");
                    throw new RecoveryFailedException("server stopping");
                }
            }

            if (cp == null) {
                if (tc.isEntryEnabled())
                    Tr.exit(tc, "getHeartbeatLog", "ConfigurationProvider is null");
                throw new RecoveryFailedException("ConfigurationProvider is null");
            }

            // Determine whether we are dealing with a custom log configuration (e.g. WXS or JDBC)
            boolean isCustom = false;
            String logDir = cp.getTransactionLogDirectory();

            if (logDir.startsWith("custom")) {
                isCustom = true;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Found a custom tran log directory");
            }

            // Retrieve the recovery log configuration information for this failure scope. This will
            // be retrieved from WCCM if its not been encountered before on this run.
            TranLogConfiguration tlc = null;

            // We now need to determine the properties and instantiate an appropriate TranLogConfiguration object
            if (isCustom) {
                // Create "custom" tlc.
                tlc = createCustomTranLogConfiguration(recoveredServerIdentity, logDir, _isPeerRecoverySupported);

                // As long as a physical location for the recovery logs is found, and logging is enabled (ie user
                // has not specified ";0" as the log location string for a file based log) then create the
                if ((tlc != null) && (tlc.enabled())) {

                    final LogProperties partnerLogProps;

                    if (tlc.type() == TranLogConfiguration.TYPE_CUSTOM) {
                        // Set up CustomLogProperties

                        partnerLogProps = new CustomLogProperties(partnerLogRLI, TransactionImpl.PARTNER_LOG_NAME, tlc.customId(), tlc.customProperties());
                        // For Liberty we need to retrieve the resource factory associated with the non transactional datasource
                        // and set it into the CustomLogProperties. This specific property is currently only referenced in the Liberty
                        // specific SQLNonTransactionalDataSource class, which overrides the tWAS equivalent.
                        ResourceFactory nontranDSResourceFactory = cp.getResourceFactory();
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Retrieved non tran DS Resource Factory {0}", nontranDSResourceFactory);

                        ((CustomLogProperties) partnerLogProps).setResourceFactory(nontranDSResourceFactory);
                        ((CustomLogProperties) partnerLogProps).setResourceConfig(cp.getResourceConfig());

                        //
                        // Get the Partner (XAResources) log
                        //
                        final RecoveryLogManager rlm = Configuration.getLogManager();
                        partnerLog = rlm.getRecoveryLog(fs, partnerLogProps);

                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Custom PartnerLog is set - {0}", partnerLog);

                        if (partnerLog instanceof HeartbeatLog) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "The log is a Heartbeatlog");
                            heartbeatLog = (HeartbeatLog) partnerLog;
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (tc.isEntryEnabled())
                Tr.exit(tc, "getHeartbeatLog", e);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "getHeartbeatLog", heartbeatLog);
        return heartbeatLog;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.ibm.ws.recoverylog.spi.RecoveryAgent#isLogLockingEnabled()
     */
    @Override
    @Trivial
    public boolean isLogLockingEnabled() {
        ConfigurationProvider cp = ConfigurationProviderManager.getConfigurationProvider();
        boolean enableLocking = cp.enableLogLocking();
        return enableLocking;
    }

    @Override
    public boolean isSQLRecoveryLog() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isSQLRecoveryLog");
        ConfigurationProvider cp = ConfigurationProviderManager.getConfigurationProvider();
        boolean isSQLLog = cp.isSQLRecoveryLog();
        if (tc.isEntryEnabled())
            Tr.exit(tc, "isSQLRecoveryLog", isSQLLog);
        return isSQLLog;
    }

    /**
     * @return the _recoveryManager
     */
    public RecoveryManager getRecoveryManager() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "getRecoveryManager", _recoveryManager);
        return _recoveryManager;
    }

    @Override
    public boolean isReplayThread() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "isReplayThread");

        boolean isReplayThread = false;
        if (_replayThread.get() != null)
            isReplayThread = _replayThread.get();

        if (tc.isEntryEnabled())
            Tr.exit(tc, "isReplayThread", isReplayThread);
        return isReplayThread;
    }

    @Override
    public void setReplayThread() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "setReplayThread");
        _replayThread.set(Boolean.TRUE);
    }

    @Override
    public void terminateServer() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "terminateServer");
        ConfigurationProviderManager.getConfigurationProvider().shutDownFramework();
    }

    @Override
    public boolean isServerStopping() {
        if (tc.isDebugEnabled())
            Tr.debug(tc, "isServerStopping", _serverStopping);
        return _serverStopping;
    }

    @Override
    public void deleteServerLease(String recoveryIdentity, boolean isPeerServer) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "deleteServerLease", this, recoveryIdentity, isPeerServer);
        if (_recoveryManager != null) {
            _recoveryManager.deleteServerLease(recoveryIdentity, isPeerServer);
        }
        if (tc.isEntryEnabled())
            Tr.exit(tc, "deleteServerLease");
    }
}
