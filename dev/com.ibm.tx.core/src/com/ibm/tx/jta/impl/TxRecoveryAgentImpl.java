package com.ibm.tx.jta.impl;

/*******************************************************************************
 * Copyright (c) 2002, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

import java.io.File;
import java.net.URISyntaxException;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

import javax.transaction.SystemException;

import com.ibm.tx.TranConstants;
import com.ibm.tx.config.ConfigurationProvider;
import com.ibm.tx.config.ConfigurationProviderManager;
import com.ibm.tx.jta.util.TranLogConfiguration;
import com.ibm.tx.util.logging.FFDCFilter;
import com.ibm.tx.util.logging.Tr;
import com.ibm.tx.util.logging.TraceComponent;
import com.ibm.ws.Transaction.JTA.Util;
import com.ibm.ws.Transaction.JTS.Configuration;
import com.ibm.ws.recoverylog.spi.ClientId;
import com.ibm.ws.recoverylog.spi.CustomLogProperties;
import com.ibm.ws.recoverylog.spi.FailureScope;
import com.ibm.ws.recoverylog.spi.FileFailureScope;
import com.ibm.ws.recoverylog.spi.FileLogProperties;
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
import com.ibm.wsspi.resource.ResourceFactory;

public class TxRecoveryAgentImpl implements RecoveryAgent {
    private static final TraceComponent tc = Tr.register(TxRecoveryAgentImpl.class, TranConstants.TRACE_GROUP, TranConstants.NLS_FILE);

    private static final int transactionLogRLI = 1; // 169107
    private static final int partnerLogRLI = 2; // 169107

    private static final int TRANSACTION_RECOVERYLOG_FORMAT_VERSION = 1;

    protected RecoveryDirector _recoveryDirector;

    protected final HashMap<String, FailureScopeController> failureScopeControllerTable = new HashMap<String, FailureScopeController>();
    // In the special case where we are operating in the cloud, we'll also work with a "lease" log
    SharedServerLeaseLog _leaseLog;
    private String _recoveryGroup;
    private boolean _isPeerRecoverySupported;

    protected String localRecoveryIdentity;

    protected TxRecoveryAgentImpl() {}

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

        // In the normal Liberty runtime the Applid will have been set into the JTMConfigurationProvider by the
        // TransactionManagerService. We additionally can set the applid here for the benefit of the unittest framework.
        if (cp.getApplId() == null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "TXAGENT, cp applid null - " + cp + " set applid - " + Util.toHexString(newApplId));
            cp.setApplId(newApplId);
            Configuration.setApplId(newApplId);
        } else {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "TXAGENT, do not reset cp - " + cp + " set applid - " + Util.toHexString(cp.getApplId()));
            Configuration.setApplId(cp.getApplId());
        }
    }

    @Override
    public void agentReportedFailure(int clientId, FailureScope failureScope) {
        // TODO Auto-generated method stub

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
            Tr.entry(tc, "initiateRecovery", fs);
        String recoveredServerIdentity = null;
        try {
            recoveredServerIdentity = fs.serverName();

            // Big message if Peer recovery is supported, just debug otherwise
            if (_isPeerRecoverySupported) {
                Tr.audit(tc, "WTRN0108I: Recovery initiated for server " + recoveredServerIdentity);
            } else {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Recovery initiated for server -  ", recoveredServerIdentity);
            }
            // Determine whether we are dealing with a custom log configuration (e.g. WXS or JDBC)
            boolean isCustom = false;
            String logDir = ConfigurationProviderManager.getConfigurationProvider().getTransactionLogDirectory();
            int logSize = ConfigurationProviderManager.getConfigurationProvider().getTransactionLogSize();

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
            FailureScopeController fsc = failureScopeControllerTable.get(recoveredServerIdentity);

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
                        throw new RecoveryFailedException();
                    }
                }
            } else {
                try {
                    fsc = new FailureScopeController(fs);
                } catch (SystemException exc) {
                    FFDCFilter.processException(exc, "com.ibm.ws.runtime.component.TxServiceImpl.initiateRecovery", "1177", this);
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
            byte[] applId = ConfigurationProviderManager.getConfigurationProvider().getApplId();
            int epoch = Configuration.getCurrentEpoch();

            RecoveryLog transactionLog = null;
            RecoveryLog partnerLog = null;

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
                    ResourceFactory nontranDSResourceFactory = ConfigurationProviderManager.getConfigurationProvider().getResourceFactory();
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Retrieved non tran DS Resource Factory, ", nontranDSResourceFactory);
                    ((CustomLogProperties) transactionLogProps).setResourceFactory(nontranDSResourceFactory);
                    ((CustomLogProperties) partnerLogProps).setResourceFactory(nontranDSResourceFactory);
                } else {
                    // Set up FileLogProperties
                    String tranLogDirStem = tlc.expandedLogDirectory();
                    tranLogDirStem = tranLogDirStem.trim();
                    String tranLogDirToUse = tranLogDirStem + File.separator + TransactionImpl.TRANSACTION_LOG_NAME;

                    transactionLogProps = new FileLogProperties(transactionLogRLI, TransactionImpl.TRANSACTION_LOG_NAME, tranLogDirToUse, tlc.logFileSize(), tranLogDirStem);

                    String partnerLogDirToUse = tlc.expandedLogDirectory();
                    partnerLogDirToUse = partnerLogDirToUse.trim() + File.separator + TransactionImpl.PARTNER_LOG_NAME;

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

                // In the special case where we support tx recovery (eg for operating in the cloud), we'll also work with a "lease" log
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Test to see if peer recovery is supported -  ", _isPeerRecoverySupported);
                if (_isPeerRecoverySupported) {
                    _leaseLog = rlm.getLeaseLog(localRecoveryIdentity, _recoveryGroup, transactionLogProps);

                    // Set the Lease Timeout into the lease log
                    _leaseLog.setPeerRecoveryLeaseTimeout(getPeerRecoveryLeaseTimeout());
                }
            }

            //
            // Create the RecoveryManager and associate it with the logs
            //
            fsc.createRecoveryManager(this, transactionLog, partnerLog, null, applId, epoch);

            // Initiate recovery on a separate thread.
            // Cannot use default threadpool threads as these are subject to hang detection and if we
            // fail to recover, we leave the group, suspend the thread awhile and the rejoin the group.
            // The hang detection logic will scream if we suspend too long.  Also do not want to drain
            // default thread pool, nor do we want to create recovery pools that may never get used and
            // just absorb resource.

            final RecoveryManager rm = fsc.getRecoveryManager();
            final boolean localRecovery = recoveredServerIdentity.equals(localRecoveryIdentity);

            // If we have a lease log then we need to set it into the recovery manager, so that it too will be processed.
            if (_leaseLog != null) {
                // If this is the local server and we're operating with lightweight peer recovery, we need to
                // acquire a lock against the lease log.
                if (localRecovery) {
                    if (!_leaseLog.lockLocalLease(localRecoveryIdentity)) {
                        if (tc.isDebugEnabled())
                            Tr.debug(tc, "Cannot lock server's own logs");
                        Object[] errorObject = new Object[] { localRecoveryIdentity };
                        RecoveryFailedException rex = new RecoveryFailedException();
                        Tr.audit(tc, "CWRLS0008_RECOVERY_LOG_FAILED",
                                 errorObject);
                        Tr.info(tc, "CWRLS0009_RECOVERY_LOG_FAILED_DETAIL", rex);

                        // Drive recovery failure processing
                        rm.recoveryFailed(rex);

                        // Check the system property but by default we want the server to be shutdown if we, the server
                        // that owns the logs is not able to recover them. The System Property supports the tWAS style
                        // of processing.
                        if (!doNotShutdownOnRecoveryFailure()) {
                            ConfigurationProvider cp = ConfigurationProviderManager.getConfigurationProvider();
                            cp.shutDownFramework();
                        }

                        if (tc.isEntryEnabled())
                            Tr.exit(tc, "initiateRecovery", rex);

                        // Output a message as to why we are terminating the server as in
                        Tr.error(tc, "CWRLS0024_EXC_DURING_RECOVERY", rex);
                        throw rex;
                    }
                }

                rm.setLeaseLog(_leaseLog);
                rm.setRecoveryGroup(_recoveryGroup);
                rm.setLocalRecoveryIdentity(localRecoveryIdentity);
            }

            Thread t = (Thread) AccessController.doPrivileged(new PrivilegedAction<Object>() {
                @Override
                public Object run() {
                    Thread temp = new Thread(rm, "Recovery Thread");
                    return temp;
                }
            });
            t.start();

            // Once we have got things going on another thread, tell the recovery directory that recovery is "complete". This
            // essentially means that other components can have a got at recovery now.
            _recoveryDirector.serialRecoveryComplete(this, fs);

            //RTC170534 - wait for Replay Completion before spawning the timout manager to monitor leases.
            fsc.getRecoveryManager().waitForReplayCompletion();

            if (!localRecovery) {
                fsc.getRecoveryManager().waitForRecoveryCompletion();
            }

            // If we have a lease log then we need to set it into the recovery manager, so that it too will be processed.
            if (_leaseLog != null) {
                // Release the lock on the lease log. This could be the local server or a peer.
                try {
                    if (localRecovery) {
                        if (_leaseLog.releaseLocalLease(recoveredServerIdentity)) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Have released locallease lock");
                        }
                    } else {
                        if (_leaseLog.releasePeerLease(recoveredServerIdentity)) {
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Have released peer lease lock");
                        }
                    }
                } catch (Exception e) {
                    // Note the error but continue
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Caught exception on lock release - " + e);
                }

                // If Recovery Failed, then by default we shall bring down the Liberty Server
                if (fsc.getRecoveryManager().recoveryFailed()) {
                    RecoveryFailedException rex = new RecoveryFailedException();
                    // Check the system property but by default we want the server to be shutdown if we, the server
                    // that owns the logs is not able to recover them. The System Property supports the tWAS style
                    // of processing.
                    if (localRecovery && !doNotShutdownOnRecoveryFailure()) {
                        ConfigurationProvider cp = ConfigurationProviderManager.getConfigurationProvider();
                        cp.shutDownFramework();
                    }

                    if (tc.isEntryEnabled())
                        Tr.exit(tc, "initiateRecovery", rex);

                    // Output a message as to why we are terminating the server as in
                    Tr.error(tc, "CWRLS0024_EXC_DURING_RECOVERY", rex);
                    throw rex;
                }

                // Only spawn timeout manager if this is the local server and recovery succeeded
                if (localRecovery) {
                    if (tc.isDebugEnabled())
                        Tr.debug(tc, "Local server recovery identity so spawn lease timeout manager");

                    LeaseTimeoutManager.setTimeout(_leaseLog,
                                                   recoveredServerIdentity,
                                                   _recoveryGroup,
                                                   this,
                                                   _recoveryDirector,
                                                   getPeerLeaseCheckInterval());
                }
            }

        } catch (InvalidFailureScopeException e) {
            FFDCFilter.processException(e, "com.ibm.ws.runtime.component.TxServiceImpl.initiateRecovery", "1599", this);
            Tr.error(tc, "WTRN0016_EXC_DURING_RECOVERY", e);

            if (tc.isEntryEnabled())
                Tr.exit(tc, "initiateRecovery", e);
            throw new RecoveryFailedException(); // 171598
        } catch (InvalidLogPropertiesException e) {
            FFDCFilter.processException(e, "com.ibm.ws.runtime.component.TxServiceImpl.initiateRecovery", "1599", this);
            Tr.error(tc, "WTRN0016_EXC_DURING_RECOVERY", e);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "initiateRecovery", e);
            throw new RecoveryFailedException(); // 171598
        } catch (URISyntaxException e) {
            FFDCFilter.processException(e, "com.ibm.ws.runtime.component.TxServiceImpl.initiateRecovery", "1599", this);
            Tr.error(tc, "WTRN0016_EXC_DURING_RECOVERY", e);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "initiateRecovery", e);
            throw new RecoveryFailedException(); // 171598
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "initiateRecovery");
    }

    @Override
    public boolean isSnapshotSafe() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String[] logDirectories(FailureScope failureScope) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void prepareForRecovery(FailureScope failureScope) {
        // TODO Auto-generated method stub

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
            FFDCFilter.processException(exc, "com.ibm.ws.runtime.component.TxServiceImpl.terminateRecovery", "1274", this);
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
            FFDCFilter.processException(exc, "com.ibm.ws.runtime.component.TxServiceImpl.terminateRecovery", "1308", this);
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Unable to indicate termination completion to recovery director: " + exc);
            if (tc.isEntryEnabled())
                Tr.exit(tc, "terminateRecovery");
            throw new TerminationFailedException(exc);
        }

        if (tc.isEntryEnabled())
            Tr.exit(tc, "terminateRecovery");
    }

    public void stop(boolean immediate) {
        // Stop lease timeout alarm popping when server is on its way down
        LeaseTimeoutManager.stopTimeout();

        // The entire server is shutting down. All recovery/peer recovery processing must be stopped. Sping
        // through all known failure scope controllers (which includes the local failure scope if we started
        // processing recovery for it) and tell them to shutdown.
        final Collection<FailureScopeController> failureScopeControllerTableValues = failureScopeControllerTable.values();
        final Iterator<FailureScopeController> failureScopeControllerTableValuesIterator = failureScopeControllerTableValues.iterator();

        while (failureScopeControllerTableValuesIterator.hasNext()) {
            final FailureScopeController fsc = failureScopeControllerTableValuesIterator.next();
            fsc.shutdown(immediate);
        }
    }

    @Override
    public void logFileWarning(String logname, int bytesInUse, int bytesTotal) {
        // TODO Auto-generated method stub
        if (tc.isEntryEnabled())
            Tr.entry(tc, "logFileWarning", new Object[] { logname, Integer.valueOf(bytesInUse), Integer.valueOf(bytesTotal) });
        if (tc.isEntryEnabled())
            Tr.exit(tc, "logFileWarning");
    }

    public void setRecoveryGroup(String recoveryGroup) {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "setRecoveryGroup", new Object[] { recoveryGroup });
        _recoveryGroup = recoveryGroup;
        if (tc.isEntryEnabled())
            Tr.exit(tc, "setRecoveryGroup");
    }

    @Override
    public String getRecoveryGroup() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getRecoveryGroup");

        if (tc.isEntryEnabled())
            Tr.exit(tc, "getRecoveryGroup", _recoveryGroup);
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
            Tr.entry(tc, "processLeasesForPeers", new Object[] { recoveryIdentity, recoveryGroup });
        ArrayList<String> peersToRecover = null;

        if (_leaseLog != null) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "work with leaseLog " + _leaseLog);

            try {
                // Retrieve peers in the recovery group from lease table
                PeerLeaseTable peerLeaseTable = new PeerLeaseTable();
                _leaseLog.getLeasesForPeers(peerLeaseTable, recoveryGroup);

                //Now extract a list of the peers that need to be recovered
//TODO: These **should** be just those whose leases have expired
                peersToRecover = peerLeaseTable.getExpiredPeers();

                // Discard the local server from the list
                peersToRecover.remove(recoveryIdentity);

                int numPeers = peersToRecover.size();
                Tr.audit(tc, "WTRN0108I: Have checked leases for peers in recovery group " + recoveryGroup + " - need to Recover " + numPeers + " peer servers");

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
            Tr.entry(tc, "claimPeerLeaseForRecovery", new java.lang.Object[] { recoveryIdentityToRecover, myRecoveryIdentity, leaseInfo, this });

        boolean peerClaimed = _leaseLog.claimPeerLeaseForRecovery(recoveryIdentityToRecover, myRecoveryIdentity, leaseInfo);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "claimPeerLeaseForRecovery", peerClaimed);
        return peerClaimed;
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
            Tr.entry(tc, "createCustomTranLogConfiguration", new java.lang.Object[] { recoveredServerIdentity, logDir, this });

        TranLogConfiguration tlc = null;
        final java.util.Properties props = new java.util.Properties();
        java.net.URI logSettingURI = new java.net.URI(logDir);
        String scheme = logSettingURI.getScheme();
        String logSetting = logSettingURI.getAuthority();
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Scheme read from URI " + scheme + ", log setting" + logSetting);
        // For the cloud and peer recovery scenarios, we'll automatically add a suffix that matches the recoveryIdentity
        if (tc.isDebugEnabled())
            Tr.debug(tc, "Test to see if peer recovery is supported -  ", isPeerRecoverySupported);
        if (isPeerRecoverySupported) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Work with server recovery identity -  " + recoveredServerIdentity + ", reset current logdir");

            if (recoveredServerIdentity != null) {
                logDir = "custom://com.ibm.rls.jdbc.SQLRecoveryLogFactory?datasource=Liberty" +
                         ",tablesuffix=" + recoveredServerIdentity;
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "log dir is now -  ", logDir);
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
     */
    private TranLogConfiguration createFileTranLogConfiguration(String recoveredServerIdentity,
                                                                FailureScope fs,
                                                                String logDir,
                                                                int logSize,
                                                                boolean isPeerRecoverySupported) throws URISyntaxException {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "createFileTranLogConfiguration", new java.lang.Object[] { recoveredServerIdentity, fs, logDir, logSize, this });

        TranLogConfiguration tlc = null;

        if (_isPeerRecoverySupported) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Work with server recovery identity -  ", recoveredServerIdentity);
            // Do we need to reset the logdir?
            if (recoveredServerIdentity.equals(localRecoveryIdentity)) {
                if (tc.isDebugEnabled())
                    Tr.debug(tc, "Local server recovery identity so no need to reset the logDir");

            } else {
                // Reset the logdir
                if (fs != null && fs instanceof FileFailureScope) {
                    FileFailureScope ffs = (FileFailureScope) fs;
                    if (ffs != null) {
                        LeaseInfo li = ffs.getLeaseInfo();
                        if (li != null) {
                            logDir = li.getLeaseDetail();
                            if (tc.isDebugEnabled())
                                Tr.debug(tc, "Have reset the logDir to ", logDir);
                        }
                    }
                }
            }
        }

        tlc = new TranLogConfiguration(logDir, logDir, logSize);

        if (tc.isEntryEnabled())
            Tr.exit(tc, "createFileTranLogConfiguration", tlc);
        return tlc;
    }

    /**
     * This method retrieves a system property named com.ibm.tx.jta.impl.PeerLeaseCheckInterval
     * which allows a value to be specified for the time we should wait between peer server status checks.
     *
     * @return
     */
    private int getPeerLeaseCheckInterval() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getPeerLeaseCheckInterval");

        int intToReturn;
        Integer peerLeaseCheckInterval = null;

        try {
            peerLeaseCheckInterval = AccessController.doPrivileged(
                                                                   new PrivilegedExceptionAction<Integer>() {
                                                                       @Override
                                                                       public Integer run() {
                                                                           return Integer.getInteger("com.ibm.tx.jta.impl.PeerLeaseCheckInterval", 20); // Default is 20 seconds
                                                                       }
                                                                   });
        } catch (PrivilegedActionException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception setting Peer Lease-Check Interval", e);
            peerLeaseCheckInterval = null;
        }

        if (peerLeaseCheckInterval == null)
            peerLeaseCheckInterval = new Integer(20);
        intToReturn = peerLeaseCheckInterval.intValue();
        if (tc.isEntryEnabled())
            Tr.exit(tc, "getPeerLeaseCheckInterval", intToReturn);
        return intToReturn;
    }

    /**
     * This method retrieves a system property named com.ibm.tx.jta.impl.PeerRecoveryLeaseTimeout
     * which allows a value to be specified for the expiry time of a lease.
     *
     * @return
     */
    private int getPeerRecoveryLeaseTimeout() {
        if (tc.isEntryEnabled())
            Tr.entry(tc, "getPeerRecoveryLeaseTimeout");

        int intToReturn;
        Integer PeerRecoveryLeaseTimeout = null;

        try {
            PeerRecoveryLeaseTimeout = AccessController.doPrivileged(
                                                                     new PrivilegedExceptionAction<Integer>() {
                                                                         @Override
                                                                         public Integer run() {
                                                                             return Integer.getInteger("com.ibm.tx.jta.impl.PeerRecoveryLeaseTimeout", 30); // Default is 30 seconds
                                                                         }
                                                                     });
        } catch (PrivilegedActionException e) {
            if (tc.isDebugEnabled())
                Tr.debug(tc, "Exception setting Peer Recovery Lease-Timeout", e);
            PeerRecoveryLeaseTimeout = null;
        }

        if (PeerRecoveryLeaseTimeout == null)
            PeerRecoveryLeaseTimeout = new Integer(20);
        intToReturn = PeerRecoveryLeaseTimeout.intValue();
        if (tc.isEntryEnabled())
            Tr.exit(tc, "getPeerRecoveryLeaseTimeout", intToReturn);
        return intToReturn;
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
                                                                                       Tr.debug(tc, "Have retrieved jvm property with result, " + theResult.booleanValue());
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
}
