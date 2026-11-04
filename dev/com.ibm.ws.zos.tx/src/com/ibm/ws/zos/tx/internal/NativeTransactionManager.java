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
package com.ibm.ws.zos.tx.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import org.osgi.framework.BundleContext;

import com.ibm.tx.jta.DestroyXAResourceException;
import com.ibm.tx.jta.XAResourceFactory;
import com.ibm.tx.jta.XAResourceNotAvailableException;
import com.ibm.tx.jta.impl.XidImpl;
import com.ibm.tx.util.ByteArray;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.Transaction.JTA.Util;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.tx.rrs.RRSXAResourceFactory;
import com.ibm.ws.zos.tx.internal.rrs.ExpressInterestReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RRSServices;
import com.ibm.ws.zos.tx.internal.rrs.RegisterResMgrReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RegistryException;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveLogNameReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveRMMetadataReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveSideInformationFastReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveSideInformationReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveURInterestReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveWorkIdentifierReturnType;
import com.ibm.ws.zos.tx.internal.rrs.SetExitInformationReturnType;
import com.ibm.wsspi.kernel.service.location.WsLocationAdmin;
import com.ibm.wsspi.kernel.service.location.WsLocationConstants;
import com.ibm.wsspi.kernel.service.location.WsResource;
import com.ibm.wsspi.logging.IntrospectableService;
import com.ibm.wsspi.tx.UOWEventEmitter;
import com.ibm.wsspi.tx.UOWEventListener;

/**
 * Transaction manager extension that provides the capability of
 * interacting with RRS within a transaction.
 */
public class NativeTransactionManager implements UOWEventListener, RRSXAResourceFactory, IntrospectableService {
    private static final TraceComponent tc = Tr.register(NativeTransactionManager.class);

    /**
     * The resource manager name log file path within the workarea.
     */
    private final String RMDATA_LOG_PATH = "rrs/tx/rmData.log";

    /**
     * Configuration property for the shutdown timeout. This is the amount of
     * time that we'll wait for unresolved transactions to finished before
     * forcing the shutdown of the context manager and transaction manager.
     */
    protected static final String SHUTDOWN_TIMEOUT_PROPERTY = "shutdownTimeout";

    /**
     * Configuration property that allows the user to specify a 1 to 8
     * alphanumeric (A-Z,a-z,0-9) and national (@,#,$)character prefix to be used as
     * part of the server generated resource manager name that is registered with
     * Resource Recovery Services (RRS). The prefix is intended to allow only authorized
     * users to register resource manager names.
     */
    protected static final String RMNAME_PREFIX_PROPERTY = "resourceManagerNamePrefix";

    /**
     * Object reference that provides the ability to call RRS native
     * services.
     */
    private RRSServices rrsServices;

    /**
     * Resource manager name representing the server's registration with the
     * system (registration services).
     */
    private String resMgrName;

    /**
     * Resource manager name registry token representing the server's registration with the
     * system (registration services).
     */
    private byte[] resMgrNameRegistryToken;

    /**
     * Resource manager token representing the server's registration with the
     * system (registration services).
     */
    private byte[] resMgrToken;

    /**
     * Registry token used to retrieve the
     * resource manager token representing the server's registration with the
     * system (registration services).
     */
    private byte[] resMgrRegistryToken;

    /**
     * Resource manager log name.
     */
    private byte[] resMgrLogName;

    /**
     * The context manager
     */
    private ContextManager contextManager = null;

    /**
     * The reference to the WSLocationAdmin service.
     */
    private WsLocationAdmin locationAdmin = null;

    /**
     * A map that stores global transaction ID (gtrid) and global transaction data object pairs.
     */
    protected ConcurrentHashMap<ByteArray, GlobalTransactionData> globalTxMap;

    /**
     * A map that stores resource manager name and HashMap pairs. The latter is the restart map
     * containing URID and UR Data pairs for restarted URs.
     */
    protected ConcurrentHashMap<String, HashMap<ByteArray, RestartURData>> restartedRMMap;

    /**
     * A map that stores uowCoordinator and local transaction data pairs.
     */
    protected ConcurrentHashMap<UOWCoordinator, LocalTransactionData> localTxMap;

    /**
     * The number of milliseconds that we'll wait before stopping the transaction
     * manager if there are unresolved transactions and/or contexts.
     */
    private long shutdownTimeoutMillis = 15000L; // 15 Seconds default

    /**
     * Boolean flag indicates whether we are active or not. This is set to false
     * during a deactivate and is a clue to XAResource instances that this TM
     * instance cannot be used to resolve transactions.
     */
    private boolean active = false;

    /**
     * Ebcdic representation of the server's data.
     */
    private byte[] ebcdicServerDataBytes;

    /**
     * Ascii representation of the server's data.
     */
    private byte[] asciiServerDataBytes;

    /**
     * Persistent interest data object wrapper.
     * In practice every transaction (that need it) should have its own PersistentInterestData
     * object instance. However, it is being cached here because of performance reasons due to
     * the fact that what needs to be hardened is generic server information that is not
     * particular to a UR and is not intended for main line or recovery use.
     * If and when it becomes necessary to store UR specific information, the use of this
     * variable must be to be re-evaluated.
     */
    private PersistentInterestData pdataWrapper;

    /**
     * The 1 to 8 alphanumeric (A-Z and 0 to 9) and national ($#@) character
     * prefix to be used as part of the server generated resource manager name.
     * The default value is: DEFAULT.
     */
    private String resMgrNamePrefix = "DEFAULT";

    /**
     * Indicates if we are allowed to log resource manager data in RRS' metadata
     * logstream. This is false if user authorization is not in place, if the metadata logstream
     * has not been defined or has been incorrectly defined, or the RM we are processing is
     * being recovered.
     */
    private boolean isMetadataLoggingAllowed = false;

    /**
     * Indicates whether or not the the current resource manager name has been seen before.
     */
    private boolean isCurrentRMNameNew = false;

    /**
     * Constructor.
     */
    public NativeTransactionManager() {
        globalTxMap = new ConcurrentHashMap<ByteArray, GlobalTransactionData>();
        restartedRMMap = new ConcurrentHashMap<String, HashMap<ByteArray, RestartURData>>();
        localTxMap = new ConcurrentHashMap<UOWCoordinator, LocalTransactionData>();
    }

    /**
     * Takes a string and converts it to a byte array of the
     * specified length in the EBCDIC code page.
     * If the string length is less than the specified length, it is
     * padded on the right with EBCDIC blanks.
     *
     * @param str       The string to convert to EBCDIC.
     * @param length    The desired length of the resulting byte array.
     * @param reportExc The flag that determines whether or not exceptions are reported.
     *
     * @return The EBCDIC byte array representation of the input string.
     */
    @Trivial
    public static byte[] convertStringToEbcdicBytes(String str, int length, boolean reportExc) {
        byte[] retBytes = null;

        if (str != null && length >= str.length()) {
            try {
                byte[] ebcdicBytes = str.getBytes("Cp1047");
                retBytes = new byte[length];
                if (length > str.length()) {
                    Arrays.fill(retBytes, (byte) 0x40);
                }
                System.arraycopy(ebcdicBytes, 0, retBytes, 0, str.length());
            } catch (Throwable t) {
                if (reportExc) {
                    throw new RuntimeException("String conversion error. EBCDIC codepage", t);
                }
            }
        }

        return retBytes;
    }

    /**
     * Updates the configuration for this service.
     *
     * @param properties The configuration properties.
     */
    protected void updateConfig(Map<String, Object> props) {
        shutdownTimeoutMillis = (Long) props.get(SHUTDOWN_TIMEOUT_PROPERTY);

        // Read the resource manager prefix.
        String rmNamePrefix = (String) props.get(RMNAME_PREFIX_PROPERTY);
        if (!resMgrNamePrefix.equalsIgnoreCase(rmNamePrefix)) {
            if (active == false) {
                if (rmNamePrefix.matches("[a-zA-Z0-9$#@]{1,8}")) {
                    resMgrNamePrefix = rmNamePrefix.toUpperCase();
                } else {
                    Tr.warning(tc, "INVALID_CONFIGURED_RMNAME_PREFIX", rmNamePrefix);
                }
            } else {
                Tr.warning(tc, "RMNAME_PREFIX_RUNTIME_CONFIG_CHANGE_NOT_ALLOWED", rmNamePrefix);
            }
        }
    }

    /**
     * Component activation call.
     */
    protected void activate(BundleContext bundleContext, Map<String, Object> properties) {

        updateConfig(properties);

        // Register this service with the data below for recovery purposes.
        Dictionary<String, Object> d = new Hashtable<String, Object>();
        String[] svcRegClassNames = new String[] { this.getClass().getCanonicalName(), RRSXAResourceFactory.class.getCanonicalName(), XAResourceFactory.class.getCanonicalName() };
        d.put("native.xa.factory", this.getClass().getCanonicalName());
        bundleContext.registerService(svcRegClassNames, this, d);

        // Process initial setup with RRS.
        SetupWithRRSReturnData setupData = processInitialSetupWithRRS(getRMNameFromLog(), false);
        resMgrToken = setupData.getResMgrToken();
        resMgrRegistryToken = setupData.getResMgrRegistryToken();
        Throwable throwable = setupData.getThrowable();
        try {
            if (throwable != null) {
                throw throwable;
            }

            // Perform restart processing. The RM state is changed to RUN upon successful execution.
            restartWithRRS();

            // Log metadata information if needed and allowed.
            setRMMetadata();
        } catch (Throwable t) {
            // If we have enough data try to deregister the RM.
            if (resMgrNameRegistryToken != null && resMgrRegistryToken != null) {
                unregisterResourceManager(resMgrName, resMgrNameRegistryToken, resMgrToken, resMgrRegistryToken);
            }

            throw new RuntimeException(t);
        }

        active = true;
    }

    /**
     * Called when the config for this class is modified.
     */
    public void modify(Map<String, Object> newProperties) {
        updateConfig(newProperties);
    }

    /**
     * Component deactivation call.
     */
    protected void deactivate() {

        StringBuffer debugData = new StringBuffer();

        try {
            // Before we attempt to do anything if there are pending transactions,
            // sleep for a few seconds to give the TM time to complete processing them.
            // The TM will attempt to resolve pending transactions during shutdown
            // processing or even after the feature set is pulled out of the server.xml.
            long millisLeftToWait = shutdownTimeoutMillis;
            int sleepIntervalMillis = 500;
            int numTxLeft = 0;

            while ((millisLeftToWait > 0) && (globalTxMap.isEmpty() == false)) {
                if (globalTxMap.size() != numTxLeft) {
                    numTxLeft = globalTxMap.size();
                    Tr.info(tc, "TM_DEACTIVATE_WAITING_FOR_TRANS", new Object[] { resMgrName.trim(), numTxLeft, millisLeftToWait });
                }

                try {
                    Thread.sleep(sleepIntervalMillis);
                    millisLeftToWait -= sleepIntervalMillis;
                } catch (Throwable t) {
                    // Something happened. No need to keep sleeping anymore.
                    // Proceed to the next step.
                    if (tc.isDebugEnabled()) {
                        Tr.debug(tc, "Sleep interrupted during deactivation.", t);
                    }
                    millisLeftToWait = 0;
                }
            }
            millisLeftToWait = Math.max(millisLeftToWait, 0); // End up positive.
            active = false;

            // Handle in-flight transactions. Transactions in any other state will be
            // handled by the recovery process when the server is restarted or when
            // the transaction manager completes outcome processing while we are iterating
            // over the list of pending transactions, or even after we deregister the RM.
            if (!globalTxMap.isEmpty()) {
                Enumeration<GlobalTransactionData> txs = globalTxMap.elements();

                while (txs.hasMoreElements()) {
                    GlobalTransactionData txData = txs.nextElement();

                    // Disregard any restarted transactions.
                    if (txData.isRestarted()) {
                        break;
                    }

                    // Obtain the data lock.
                    ReadWriteLock rwLock = txData.getLock();
                    Lock wLock = rwLock.writeLock();
                    wLock.lock();

                    try {
                        XidImpl xid = txData.getXid();

                        // We can only handle in-flight cases. We presume abort.
                        if (txData.getURState() == RRSServices.ATR_IN_FLIGHT) {
                            try {
                                if (txData.getURIToken() == null) {
                                    expressInterest(txData);
                                }

                                // An in-flight will not have a context interest.
                                byte[] uriRegistryToken = txData.getURIRegistryToken();
                                int rc = rrsServices.backoutAgentUR(uriRegistryToken, null, RRSServices.ATR_DEFER_IMPLICIT);
                                if (rc == RRSServices.ATR_OK) {
                                    // Set the UR as forgotten. Map cleanup will take place when the
                                    // transaction manager processes syncpoint.
                                    txData.setUrForgotten(true);
                                } else {
                                    processInvalidServiceRCWithXAExc("INVALID_RRS_SERVICE_RC", "ATR4ABAK", rc, XAException.XAER_RMERR, txData.getData());
                                }
                            } catch (Throwable t) {
                                // Absorb any exceptions and continue to next entry.
                                debugData.append((xid == null) ? "NULL-XID" : xid.toString());
                                debugData.append(". ");
                            }
                        } else {
                            debugData.append((xid == null) ? "NULL-XID" : xid.toString());
                            debugData.append(". ");
                        }
                    } finally {
                        wLock.unlock();
                    }
                }
            } else {
                // Handle deactivation with no work done.
                if (resMgrRegistryToken == null) {
                    return;
                }
            }

            // Clean up the context pool and all active contexts;
            contextManager.destroyContextManager(millisLeftToWait);
        } finally {
            // Deregister from RRS and context services.
            unregisterResourceManager(resMgrName, resMgrNameRegistryToken, resMgrToken, resMgrRegistryToken);

            if (debugData.length() != 0) {
                Tr.warning(tc, "OUTSTANDING_TRANS_RM_UNREGISTERED", new Object[] { resMgrName.trim(), resMgrToken, globalTxMap.size(), debugData });
            }
        }
    }

    /**
     * Sets the RRSServices object reference.
     */
    protected void setRRSServices(RRSServices rrsServices) {
        this.rrsServices = rrsServices;
    }

    /**
     * Clears the RRSServices object reference.
     */
    protected void unsetRRSServices(RRSServices rrsServices) {
        this.rrsServices = null;
    }

    /**
     * Sets the context manager reference.
     */
    protected void setContextManager(ContextManager ctxMgr) {
        this.contextManager = ctxMgr;
    }

    /**
     * Clears the context manager reference.
     */
    protected void unsetContextManager(ContextManager ctxMgr) {
        this.contextManager = null;
    }

    /**
     * Sets the WsLocationAdmin reference.
     */
    protected void setLocationAdmin(WsLocationAdmin locationAdmin) {
        this.locationAdmin = locationAdmin;
    }

    /**
     * Clears the WsLocationAdmin reference.
     */
    protected void unsetLocationAdmin(WsLocationAdmin locationAdmin) {
        this.locationAdmin = null;
    }

    /**
     * Retrieves the map of global transaction IDs and corresponding global transaction
     * data.
     *
     * @return The map containing global transaction IDs and GlobaTransactionData pairs.
     */
    protected Map<ByteArray, GlobalTransactionData> getGlobalTxMap() {
        return globalTxMap;
    }

    /**
     * Retrieves the map of restarted RMs.
     *
     * @return The map of restarted RMs.
     */
    protected Map<String, HashMap<ByteArray, RestartURData>> getRestartedRMMap() {
        return restartedRMMap;
    }

    /**
     * Retrieves the map of local transaction coordinators and local transaction data pairs.
     *
     * @return The map containing local transaction coordinator and local transaction data pairs.
     */
    protected Map<UOWCoordinator, LocalTransactionData> getLocalTxMap() {
        return localTxMap;
    }

    /**
     * Retrieves the resource manager token.
     *
     * @return The 16 byte resource manager token.
     */
    protected byte[] getResourceManagerToken() {
        return resMgrToken;
    }

    /**
     * Retrieves the registry token used to get the resource manager token.
     *
     * @return The registry token used to get the 16 byte resource manager token.
     */
    protected byte[] getResourceManagerRegistryToken() {
        return resMgrRegistryToken;
    }

    /**
     * Tells the caller if this NativeTransactionManager instance is active and
     * can resolve transactions. An inactive NativeTransactionManager instance
     * has no active connection to RRS. Its RM token(s) is/are not valid.
     *
     * @return true if active, false if not.
     */
    protected boolean isActive() {
        return active;
    }

    /**
     * Processes the initial setup with RRS.
     *
     * @param rmName   The resource manager name to setup with.
     * @param recovery The indicator of whether or not this call is being
     *                     made on behalf of recovery processing.
     * @return Resource manager tokens.
     */
    protected SetupWithRRSReturnData processInitialSetupWithRRS(String rmName, boolean recovery) {

        String stck = null;
        String prefix = resMgrNamePrefix;
        byte[] localRMToken = null;
        byte[] localRMRegistryToken = null;

        // If the RM is not null, we are processing recovery or the rmName has already been logged
        // in the workarea.
        if (rmName != null) {
            String loggedPrefix = null;
            String[] tkns = rmName.split("\\.");

            switch (tkns.length) {
                case 3:
                    String tmp = tkns[1];
                    loggedPrefix = tmp.substring(0, 8);
                    stck = tmp.substring(8);
                    break;
                case 4:
                    loggedPrefix = tkns[1];
                    stck = tkns[2];
                    break;
                default:
                    throw new RuntimeException("Invalid resource manager name: " + rmName);
            }

            // If we are processing recovery, use the prefix found in the rmName input. Otherwise,
            // we use whatever prefix is current.
            if (recovery) {
                prefix = loggedPrefix;
            }
        }

        RegisterResMgrReturnType rrmrt = rrsServices.registerResourceManager(RRSServices.CRG_UNREG_EOM,
                                                                             new byte[16],
                                                                             convertStringToEbcdicBytes(prefix, prefix.length(), true),
                                                                             (stck != null) ? convertStringToEbcdicBytes(stck, stck.length(), true) : null);
        if (rrmrt == null) {
            processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "CRG4GRM", -1, "");
        }

        // Check for prefix authentication failures for non-default prefixes.
        if (!prefix.equals("DEFAULT") && rrmrt.isRMPrefixAuthCheckFailed()) {
            Tr.error(tc, "RMNAME_PREFIX_AUTH_CHECK_FAILED", new Object[] { prefix, rrmrt.getInternalAuthCheckRc(), rrmrt.getSAFRc(), rrmrt.getRACFRc(), rrmrt.getRACFRsn() });
            String msg = "The authorization check to validate the user privileges against the configured " +
                         "resource manager prefix name of " + prefix + " has failed. Look for message CWLIB0113E for more details.";
            throw new RuntimeException(msg);
        }

        int rc = rrmrt.getReturnCode();
        if (rc != RRSServices.CRG_OK) {
            StringBuilder sb = new StringBuilder();
            sb.append("ResourManagerNamePrefix: ");
            sb.append(prefix);
            sb.append(", ResourManagerNameSTCK: ");
            sb.append(stck);
            processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "CRG4GRM", rc, sb.toString());
        }

        // The resource manager was registered successfully.
        SetupWithRRSReturnData returnData = new SetupWithRRSReturnData();
        try {
            // Get the resource manager token registry token.
            localRMToken = rrmrt.getResMgrToken();
            localRMRegistryToken = rrmrt.getResMgrRegistryToken();
            byte[] localResMgrNameRegistryToken = rrmrt.getResMgrNameRegistryToken();

            // set the RM data to be returned to the caller.
            returnData.setRMNameRegistryToken(localResMgrNameRegistryToken);
            returnData.setResMgrToken(localRMToken);
            returnData.setResMgrRegistryToken(localRMRegistryToken);

            // Get the resource manager name and log it if it has changed
            // since the last activation.
            if (!recovery) {
                try {
                    resMgrName = new String(rrmrt.getResMgrName(), "IBM-1047");
                } catch (Throwable t) {
                    throw new RuntimeException("String conversion error. ASCII codepage", t);
                }

                resMgrNameRegistryToken = localResMgrNameRegistryToken;

                if (!resMgrName.equals(rmName)) {
                    isCurrentRMNameNew = true;
                    logRMName(resMgrName);
                }

                contextManager.initialize(localRMRegistryToken);
            }

            // Set exit information the system with both RRS and registration services.
            SetExitInformationReturnType sert = rrsServices.setExitInformation(localResMgrNameRegistryToken, localRMRegistryToken, recovery);

            if (sert.getReturnCode() != RRSServices.CRG_OK) {
                processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "CRG4SEIF", -1, "");
            }

            if (rc != RRSServices.CRG_OK) {
                StringBuilder sb = new StringBuilder();
                sb.append("ResourManagerName: ");
                sb.append(rmName);
                sb.append("ResourManagerNameRegistryToken: ");
                sb.append((localResMgrNameRegistryToken == null) ? "NULL" : Util.toHexString(localResMgrNameRegistryToken));
                sb.append(", ResourManagerToken: ");
                sb.append((localRMToken == null) ? "NULL" : Util.toHexString(localRMToken));
                sb.append("ResourManagerRegistryToken: ");
                sb.append((localRMRegistryToken == null) ? "NULL" : Util.toHexString(localRMRegistryToken));
                processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "CRG4SEIF", rc, sb.toString());
            }

            if (!recovery) {
                // Save the metadata logging allowed indicator.
                isMetadataLoggingAllowed = sert.isMetadataLoggingAllowed();

                // Set environment information to establish the default transaction mode (Local) for an in-reset UR.
                int[] envIds = { RRSServices.ATR_TRAN_MODE_SETTING };
                int[] envIdVals = { RRSServices.ATR_LOCAL_MODE };
                int[] protVals = { RRSServices.ATR_PROTECTED };
                rc = rrsServices.setEnvironment(null,
                                                envIds,
                                                envIdVals,
                                                protVals);

                if (rc != RRSServices.ATR_OK) {
                    processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "ATR4SENV", rc, "");
                }
            }
        } catch (Throwable t) {
            returnData.setThrowable(t);
        }

        return returnData;
    }

    /**
     * Performs restart processing for the current server instance.
     *
     * @param rmRegistryToken The resource manager token registry token.
     */
    protected void restartWithRRS() {
        restartWithRRS(resMgrName, resMgrNameRegistryToken, resMgrToken, resMgrRegistryToken);
    }

    /**
     * Performs restart processing for the specified resource manager.
     *
     * @param rmName              The resource manager name.
     * @param rmNameRegistryToken The resource manager name registry token.
     * @param rmToken             The resource manager token.
     * @param rmRegistryToken     The resource manager registry token.
     */
    protected void restartWithRRS(String rmName, byte[] rmNameRegistryToken, byte[] rmToken, byte[] rmRegistryToken) {

        // Restart only happens once per resource manager.
        if (restartedRMMap.get(rmName) != null) {
            throw new IllegalStateException("Restart has already been processed for resource manager: " +
                                            rmName.trim() + ". Resource manager token: " + Util.toHexString(rmToken));
        }

        HashMap<ByteArray, RestartURData> restartedURMap = new HashMap<ByteArray, RestartURData>();
        // Setup for recovery processing. Retrieve the logName.
        RetrieveLogNameReturnType rlnRt = rrsServices.retrieveLogName(rmRegistryToken);

        if (rlnRt == null) {
            processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "ATR4IRLN", -1, "");
        }

        int rc = rlnRt.getReturnCode();

        switch (rc) {
            case RRSServices.ATR_OK:
                resMgrLogName = rlnRt.getRmLogName();
                break;
            case RRSServices.ATR_RM_LOGNAME_NOT_SET:
                break;
            default:
                StringBuilder sb = new StringBuilder();
                sb.append("ResourManagerName: ");
                sb.append(rmName);
                sb.append(", ResourManagerToken: ");
                sb.append((rmToken == null) ? "NULL" : Util.toHexString(rmToken));
                sb.append(", ResourManagerRegistryToken: ");
                sb.append((rmRegistryToken == null) ? "NULL" : Util.toHexString(rmRegistryToken));
                processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "ATR4IRLN", rc, sb.toString());
                throw new RuntimeException("RRS service returned the invalid return code from ATR4IRLN service.");
        }

        // Set the log name.
        if (resMgrLogName != null) {
            byte[] logName = getLogName();
            rc = rrsServices.setLogName(rmRegistryToken, logName);

            if (rc != RRSServices.ATR_OK) {
                StringBuilder sb = new StringBuilder();
                sb.append("ResourManagerName: ");
                sb.append(rmName);
                sb.append(", ResourManagerToken: ");
                sb.append((rmToken == null) ? "NULL" : Util.toHexString(rmToken));
                sb.append(", ResourManagerRegistryToken: ");
                sb.append((rmRegistryToken == null) ? "NULL" : Util.toHexString(rmRegistryToken));
                sb.append(", LogNameBytes: ");
                sb.append((logName == null) ? "NULL" : Util.toHexString(logName));
                processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "ATR4ISLN", rc, sb.toString());
            }
        }

        // Begin actual restart processes.
        rc = rrsServices.beginRestart(rmRegistryToken);

        if (rc != RRSServices.ATR_OK && rc != RRSServices.ATR_NO_MORE_INCOMPLETE_INTERESTS) {
            StringBuilder sb = new StringBuilder();
            sb.append("ResourManagerName: ");
            sb.append(rmName);
            sb.append(", ResourManagerToken: ");
            sb.append((rmToken == null) ? "NULL" : Util.toHexString(rmToken));
            sb.append(", ResourManagerRegistryToken: ");
            sb.append((rmRegistryToken == null) ? "NULL" : Util.toHexString(rmRegistryToken));
            processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "ATR4IBRS", rc, sb.toString());
        }

        // Retrieve all UR interests and store them for later use.
        do {
            try {
                RetrieveURInterestReturnType ruirt = rrsServices.retrieveURInterest(rmRegistryToken);

                if (ruirt == null) {
                    processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "ATR4IRNI", -1, "");
                }

                rc = ruirt.getReturnCode();
                switch (rc) {
                    case RRSServices.ATR_NO_MORE_INCOMPLETE_INTERESTS:
                        break;
                    case RRSServices.ATR_OK:
                        ByteArray urid = new ByteArray(ruirt.getURID());

                        if (!restartedURMap.containsKey(urid)) {
                            byte[] uriToken = ruirt.getUriToken();
                            byte[] uriRegistryToken = ruirt.getUriRegistryToken();
                            byte[] pdata = ruirt.getPdata();
                            int urState = ruirt.getState();
                            XidImpl xid = getURWorkID(uriRegistryToken, uriToken);
                            boolean isHeuristic = getURHeuristicIndicator(uriRegistryToken, uriToken);
                            boolean needsRecovery = isHeuristic;
                            int response = RRSServices.ATR_RESPOND_CONTINUE;

                            switch (urState) {
                                case RRSServices.ATR_IN_DOUBT:
                                    needsRecovery = true;
                                case RRSServices.ATR_IN_COMMIT:
                                case RRSServices.ATR_IN_BACKOUT:
                                    if (needsRecovery) {
                                        RestartURData urData = new RestartURData(xid, rmName, rmNameRegistryToken, rmToken, rmRegistryToken, urid.getBytes(), uriToken, uriRegistryToken, isHeuristic, urState, pdata);
                                        restartedURMap.put(urid, urData);
                                    } else {
                                        // Follow TWAS' behavior
                                        response = RRSServices.ATR_RESPOND_COMPLETE;
                                    }

                                    byte[] nonPData = new byte[16];
                                    try {
                                        rrsServices.respondToRetrievedInterest(uriRegistryToken, response, nonPData);
                                    } catch (RegistryException rex) {
                                        // UR disappeared -- remove it from the registry.
                                        restartedURMap.remove(urid);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                        break;
                    default:
                        StringBuilder sb = new StringBuilder();
                        sb.append("ResourManagerName: ");
                        sb.append(rmName);
                        sb.append(", ResourManagerToken: ");
                        sb.append((rmToken == null) ? "NULL" : Util.toHexString(rmToken));
                        sb.append(", ResourManagerRegistryToken: ");
                        sb.append((rmRegistryToken == null) ? "NULL" : Util.toHexString(rmRegistryToken));
                        processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "ATR4IRNI", rc, sb.toString());
                }
            } catch (RegistryException rex) {
                throw new RuntimeException("An error was encountered while restarting with RRS", rex);
            }
        } while (rc != RRSServices.ATR_NO_MORE_INCOMPLETE_INTERESTS);

        // end restart.
        rc = rrsServices.endRestart(rmRegistryToken);

        if (rc != RRSServices.ATR_OK) {
            StringBuilder sb = new StringBuilder();
            sb.append("ResourManagerName: ");
            sb.append(rmName);
            sb.append(", ResourManagerToken: ");
            sb.append((rmToken == null) ? "NULL" : Util.toHexString(rmToken));
            sb.append(", ResourManagerRegistryToken: ");
            sb.append((rmRegistryToken == null) ? "NULL" : Util.toHexString(rmRegistryToken));
            processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "ATR4IERS", rc, sb.toString());
        }

        // Done with restart.
        Tr.info(tc, "RM_RESTART_COMPLETE", new Object[] { rmName.trim(), Util.toHexString(rmToken), restartedURMap.size() });

        // Keep track of the restarted UR map only if there is something to recover.
        // If there is nothing to recover, native recovery is complete.
        if (restartedURMap.size() > 0) {
            restartedRMMap.put(rmName, restartedURMap);
        } else {
            recoveryComplete(rmName, rmNameRegistryToken, rmToken, rmRegistryToken);
        }
    }

    /**
     * Expresses interest in the current UR.
     *
     * @param natvTm The native transaction manager object reference.
     * @param txData The transaction Data object reference.
     * @param xid    The global transaction ID.
     *
     * @throws XAException       Thrown if an RRS service returns an unexpected return code.
     * @throws RegistryException Thrown if a problem occurred while adding or retrieving a URI token
     *                               from the native registry. This could mean that the URI token could not be added during
     *                               the call to express interest, or could not be retrieved during the call to set syncpoint
     *                               controls. In either case, no further operations on the URI will be possible. In the
     *                               express interest case, the interest was deleted before throwing the exception.
     */
    protected void expressInterest(GlobalTransactionData txData) throws XAException, RegistryException {

        // Currently all transactions use the same pdata wrapper and cached pdata. This is done
        // purely for performance due to the fact that what needs to be hardened is generic
        // server information that is not particular to a UR and is not intended for
        // main line or recovery use.
        PersistentInterestData pdataWrapper = getPdataWrapper();
        byte[] pdata = pdataWrapper.getPdata();
        if (pdata == null) {
            pdata = pdataWrapper.generatePdata(getServerDataBytes(true));
        }

        byte[] nonPData = new byte[16];
        byte[] uriToken = null;
        byte[] uriRegistryToken = null;
        Context ctxTokenWrapper = txData.getContext();
        byte[] ctxToken = ctxTokenWrapper.getContextToken();

        ExpressInterestReturnType eirt = rrsServices.expressURInterest(getResourceManagerRegistryToken(),
                                                                       ctxToken,
                                                                       RRSServices.ATR_PRESUMED_ABORT,
                                                                       nonPData,
                                                                       pdata,
                                                                       null,
                                                                       null);

        if (eirt == null) {
            processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "ATR4EINT", -1, "");
        }

        int rc = eirt.getReturnCode();

        if (rc == RRSServices.ATR_OK) {
            uriToken = eirt.getUriToken();
            uriRegistryToken = eirt.getUriRegistryToken();
            txData.setURIToken(uriToken, uriRegistryToken);
        } else {
            processInvalidServiceRCWithXAExc("INVALID_RRS_SERVICE_RC", "ATR4EINT", rc, XAException.XAER_RMERR, txData.getData());
        }

        // Set syncpoint controls to establish the role that the resource manager will play
        // when processing the global transaction.
        rc = rrsServices.setSyncpointControls(uriRegistryToken, RRSServices.ATR_PREPARE_OK, RRSServices.ATR_COMMIT_OK, RRSServices.ATR_BACKOUT_OK, RRSServices.ATR_SDSRM);

        if (rc != RRSServices.ATR_OK) {
            processInvalidServiceRCWithXAExc("INVALID_RRS_SERVICE_RC", "ATR4SSPC", rc, XAException.XAER_RMERR, txData.getData());
        }
    }

    /**
     * Retrieves the work id (Xid) for the given Unit of recovery interest token.
     *
     * @param uriRegistryToken The token to look up the unit of recovery interest
     *                             token from the native registry.
     * @param the              URI token. This is only used to print the token in error
     *                             messages.
     *
     * @return The work Id (Xid).
     *
     * @throws RegistryException thrown if the URI could not be found in the native
     *                               registry.
     */
    private XidImpl getURWorkID(byte[] uriRegistryToken, byte[] uriToken) throws RegistryException {

        XidImpl xid = null;
        RetrieveWorkIdentifierReturnType rwirt = rrsServices.retrieveWorkIdentifier(uriRegistryToken);

        if (rwirt == null) {
            processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "ATR4RWID", -1, "");
        }

        int rc = rwirt.getRRSReturnCode();

        if (rc == RRSServices.ATR_OK) {
            xid = new XidImpl(rwirt.getXid(), 0);
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("URIToken: ");
            sb.append((uriToken == null) ? "NULL" : Util.toHexString(uriToken));
            processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "ATR4RWID", rc, sb.toString());
        }

        return xid;
    }

    /**
     * Retrieves the heuristic flag for the given unit of recovery interest.
     *
     * @param uriRegistryToken The token to look up the unit of recovery interest
     *                             token from the native registry.
     * @param the              URI token. This is only used to print the token in error
     *                             messages.
     * @return True if heuristic indicator is set. False otherwise.
     *
     * @throws RegistryException Thrown if the URI could not be found in the native
     *                               registry.
     */
    private boolean getURHeuristicIndicator(byte[] uriRegistryToken, byte[] uriToken) throws RegistryException {

        boolean isHeuristic = false;
        int[] infoList = new int[] { RRSServices.ATR_HEURISTIC_MIX };
        RetrieveSideInformationReturnType rsirt = rrsServices.retrieveSideInformation(uriRegistryToken, infoList);

        if (rsirt == null) {
            processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "ATR4RUSI", -1, "");
        }

        int rc = rsirt.getReturnCode();

        if (rc == RRSServices.ATR_OK) {
            isHeuristic = (rsirt.getSideInfo(0) == RRSServices.ATR_SIDE_VALUE_NOT_SET) ? false : true;
        } else {
            StringBuilder sb = new StringBuilder();
            sb.append("URI Token: ");
            sb.append((uriToken == null) ? "NULL" : Util.toHexString(uriToken));
            processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "ATR4RUSI", rc, sb.toString());
        }

        return isHeuristic;
    }

    /**
     * Provides RRS with RM related data to be persisted. The data is persisted only if:
     * - The user has UPDATE/ALTER authority to RRS FACILITY class (ATRSRV).
     * - The RM's state is RUN.
     * - The RM is not a recovering RM.
     * - The RM has not been seen before or has been seen but no data was logged previously.
     * - The METADATA logstream has been defined (properly).
     *
     * Note that RM METADATA is deleted ONLY when the RM is deleted. This makes ATRSRV
     * authorization a pre-requisite because only then we know that RM entries will be
     * removed from the METADATA logstream (clean shutdown or when the server using the same
     * RM name is restarted after a failure - no -clean restart).
     *
     * Currently, it is not imperative that we log RM data the METADATA logstream or that we halt
     * or fail execution due to failures as result of processing this method.
     * Consequently, we are be very forgiving about bad return codes and exceptions.
     */
    protected void setRMMetadata() {
        try {
            // If we are not allowed to log. Return.
            if (!isMetadataLoggingAllowed) {
                return;
            }

            // Lets pause for a minute and reflect on this two facts:
            // 1. If data is logged multiple times during the lifetime of the RM, RRS accumulates
            //    that data if the RM is not deleted (i.e. repeated server failures/restarts).
            // 2. RRS deletes all of the data accumulated for the RM if/when the RM is deleted (
            //    clean shutdown, clean deactivation, etc).
            // If the user is authorized, and we need to log for an already seen RM name, we have 2 options:
            // We can log every time we are activated or we can check if there is data already logged before
            // we log anything.
            // For option 2, we could check if data has already been logged. If so,
            // do nothing to prevent accumulating entries for this RM. If not, log the data because
            // logging conditions were just met and we can tell that is the case. This also helps prevent
            // excessive data leakage (i.e. failures/restarts followed by a restart with
            // a RM name change (prefix change) or server restart with the clean option after
            // several server failures) if it takes place.
            //
            // Checking for logged entries for already seen RMs maybe overkill under normal conditions
            // because RM entries are deleted as long as one ATRSRV is invoked to delete the RM at
            // some point during the life of the RM. Last even if the user were to zip the code and
            // move it somewhere else or change sever names, i would assume that the user would have
            // to start with the -clean option, which will trigger a new entry to be logged because
            // there would be a new RM name.
            //
            // On the other hand option 1, logging on each activation, could catch future cases or cases currently
            // not know to me where we may need to log because things could change without requiring a clean
            // server start. Also, we know that in production systems, servers are stopped cleanly at least
            // most of the time, which would cause RM cleanup.
            //
            // We could really go either way on this one, but since we have seen in the past total system
            // failures if a RRS logstream becomes "full", we will err on the side of what is a bit better.
            // After all, the check will only happen once during activation and if we have seen the RM name
            // previously.
            if (!isCurrentRMNameNew) {
                RetrieveRMMetadataReturnType rmrt = rrsServices.retrieveRMMetadata(resMgrRegistryToken);
                if (rmrt == null || rmrt.getReturnCode() != 0 || rmrt.getMetaData().length > 0) {
                    return;
                }
            }

            // Log the data.
            int rc = rrsServices.setRMMetadata(resMgrRegistryToken, getServerDataBytes(true));
            switch (rc) {
                case RRSServices.ATR_OK:
                    break;
                case RRSServices.ATR_RM_8K_METADATA_NOT_ALLOWED:
                case RRSServices.ATR_RM_METADATA_LOG_UNAVAILABLE:
                default:
                    StringBuilder sb = new StringBuilder();
                    sb.append("ReturnCode: ");
                    sb.append(rc);
                    sb.append(", ResourManagerToken: ");
                    sb.append((resMgrRegistryToken == null) ? "NULL" : Util.toHexString(resMgrRegistryToken));

                    if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                        Tr.event(tc, "Data not logged. An invalid return code was detected from a call to process service ATR4SDTA. " + sb.toString());
                    }

                    break;
            }
        } catch (Throwable t) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Internal native failure detected while attempting to process service: CRG4SDTA. ", t);
            }
        }
    }

    /**
     * Gets log name to be used by the resource manager.
     *
     * @return
     */
    private byte[] getLogName() {
        String logName = "BBOT.LOGNAME.IBM";
        return convertStringToEbcdicBytes(logName, logName.length(), true);
    }

    /**
     * Sets the resource manager name.
     *
     * @param name The resource manager name.
     */
    public void setRMName(String name) {
        resMgrName = name;
    }

    /**
     * Retrieves the resource manager name.
     *
     * @return The String representing the resource manager name.
     */
    public String getResourceManagerName() {
        return resMgrName;
    }

    /**
     * Retrieves the resource manager name registry token.
     *
     * @return The resource manager name registry token.
     */
    protected byte[] getResourceManagerNameRegistryToken() {
        return resMgrNameRegistryToken;
    }

    /**
     * Retrieves the resource manager name from the resource manager log if one exists.
     * Note that starting the server with the clean option will purge the work area file.
     *
     * @return The resource manager name.
     */
    public String getRMNameFromLog() {

        WsResource resMgrNameLog = locationAdmin.getServerWorkareaResource(RMDATA_LOG_PATH);
        String rmName = null;

        if (resMgrNameLog.exists()) {
            // The resource manager data log exists, reuse the logged resource manager name.
            InputStream is = null;
            try {
                is = resMgrNameLog.get();
                byte[] rmNameBytes = new byte[32];
                int bytesRead = is.read(rmNameBytes);

                if (bytesRead != rmNameBytes.length) {
                    throw new RuntimeException("Invalid number of bytes (" + bytesRead + ") read from file: " +
                                               resMgrNameLog.toRepositoryPath());
                }

                rmName = new String(rmNameBytes);

            } catch (IOException ioe) {
                throw new RuntimeException("Unable to read from file: " + resMgrNameLog.toRepositoryPath(), ioe);
            } finally {
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException ioee) {
                        throw new RuntimeException("A failure was encountering while closing stream for file: " + resMgrNameLog.toRepositoryPath(), ioee);
                    }
                }
            }
        }

        return rmName;
    }

    /**
     * Logs the resource manager name in the resource manager log. If the log does not
     * exists, once is created.
     *
     * @param rmName The resource manager log to be hardened.
     */
    public void logRMName(String rmName) {
        WsResource resMgrNameLog = locationAdmin.getServerWorkareaResource(RMDATA_LOG_PATH);

        // The resource manager log does not exist, create it.
        if (!resMgrNameLog.exists()) {
            if (!resMgrNameLog.create()) {
                throw new RuntimeException("Unable to create resource manager data file: <workarea>/" + RMDATA_LOG_PATH);
            }
        }

        // Log the resoruce manager name.
        OutputStream os = null;
        try {
            os = resMgrNameLog.putStream();
            byte[] rmNameBytes = rmName.getBytes();
            os.write(rmNameBytes);
            os.flush();
        } catch (IOException ioe) {
            throw new RuntimeException("Unable to write to file: " + resMgrNameLog.toRepositoryPath(), ioe);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException ioee) {
                    throw new RuntimeException("A failure was encountering while closing stream for file: " + resMgrNameLog.toRepositoryPath(), ioee);
                }
            }
        }

        if (tc.isDebugEnabled()) {
            Tr.debug(tc, "Current resource manager name: ", resMgrName);
        }
    }

    /**
     * Retrieves an object encapsulating the current thread's context token.
     *
     * @return An object encapsulating the current thread's context token.
     */
    public Context getContextOnCurrentThread() {
        return contextManager.getCurrentContext();
    }

    /**
     * Tries to resolve a local transaction which is not on the current thread.
     *
     * @param uow    The local mode UOW to be resolved. The UOW should be suspended.
     * @param commit Set to true if the UR should be committed, false if backed out.
     *
     * @return The return code from ATR4END.
     */
    int resolveNonCurrentLocalModeUR(UOWCoordinator uow, boolean commit) {
        // Resume the suspended UOW.  If it is not suspended then it's not safe
        // for us to try to resolve it.
        int rc = RRSServices.ATR_OK;
        contextManager.resume(uow);
        try {
            int action = (commit) ? RRSServices.ATR_COMMIT_ACTION : RRSServices.ATR_ROLLBACK_ACTION;
            rc = rrsServices.endUR(action, null);
        } finally {
            contextManager.suspend(uow);
        }

        return rc;
    }

    /**
     * Removes a UR entry from the restarted UR map. It is called when
     * recovery has completed for a UR identified by the given URID.
     *
     * @param txData The data for an individual global transaction.
     */
    protected void removeEntryFromRestartedURMap(GlobalTransactionData txData) {
        String rmName = txData.getRMName();
        Map<ByteArray, RestartURData> restartedURMap = restartedRMMap.get(rmName);

        if (restartedURMap == null) {
            throw new IllegalStateException("An invalid restarted UR map was found while attempting to remove " +
                                            "a recoved UR entry. " + txData.toString());
        }

        byte[] urid = txData.getURId();
        restartedURMap.remove(new ByteArray(urid));

        if (restartedURMap.size() == 0) {
            byte[] rmNameRegistryToken = txData.getRMNameRegistryToken();
            byte[] rmToken = txData.getRMToken();
            byte[] rmRegistryToken = txData.getRMRegistryToken();
            recoveryComplete(rmName, rmNameRegistryToken, rmToken, rmRegistryToken);
        }
    }

    /**
     * Processes final cleanup for a resource manager who does not or no longer has
     * recovery processing pending.
     *
     * @param rmName              The resource manager name.
     * @param rmNameRegistryToken The resource manager name registry token.
     * @param rmToken             The resource manager token.
     * @param rmRegistryToken     The resource manager registry token.
     */
    protected void recoveryComplete(String rmName, byte[] rmNameRegistryToken, byte[] rmToken, byte[] rmRegistryToken) {
        // If recovery completed for a resource manager other than the current one,
        // unregister the RM.
        if (!rmName.equalsIgnoreCase(resMgrName)) {
            unregisterResourceManager(rmName, rmNameRegistryToken, rmToken, rmRegistryToken);
        }

        // Remove the RM entry from the restarted RM map.
        // Check if the RM is in the map first just for clarity.
        if (restartedRMMap.containsKey(rmName)) {
            restartedRMMap.remove(rmName);
        }

        // Issue recovery complete message.
        Tr.info(tc, "RM_RECOVERY_COMPLETE", rmName.trim(), Util.toHexString(rmToken));
    }

    /**
     * Deregisters the resource manager from RRS.
     */
    public void unregisterResourceManager(String rmName, byte[] rmNameRegToken, byte[] rmToken, byte[] rmRegToken) {
        int rc = rrsServices.unregisterResourceManager(rmNameRegToken, rmRegToken);

        if (rc != RRSServices.CRG_OK) {
            StringBuilder sb = new StringBuilder();
            sb.append("ResourManagerName: ");
            sb.append((rmName == null) ? "NULL" : rmName);
            sb.append("ResourManagerNameRegistryToken: ");
            sb.append((rmNameRegToken == null) ? "NULL" : rmNameRegToken);
            sb.append(", ResourManagerToken: ");
            sb.append((rmToken == null) ? "NULL" : Util.toHexString(rmToken));
            sb.append(", ResourManagerRegistryToken: ");
            sb.append((rmRegToken == null) ? "NULL" : Util.toHexString(rmRegToken));
            processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "CRG4DRM", rc, sb.toString());
        }
    }

    /**
     * Performs post end cleanup for the specified local transaction.
     *
     * @param uwoc The local transaction coordinator.
     */
    private void localTxPostEndCleanup(UOWCoordinator uowc) {
        // There are failure paths where we might end up with entries in
        // our tables that may never be cleaned up. If we are called for post_end
        // remove the specified UOW's entry from the local tx map. There is
        // really nothing we can do at this point except cleanup.
        // Note that the context manager will perform further cleanup with RRS during end.
        LocalTransactionData ltcData = localTxMap.get(uowc);
        if (ltcData != null) {
            if (TraceComponent.isAnyTracingEnabled() && tc.isEventEnabled()) {
                Tr.event(tc, "Local transaction ending. Dangling native resource entry found. ", ltcData.getData());
            }

            localTxMap.remove(uowc);
        }
    }

    /**
     * Processes bad service return code or internal return codes while attempting to process a native service call.
     *
     * @param messageId   The message ID to use.
     * @param serviceName The target service name.
     * @param returnCode  The return code.
     * @param data        The extra relevant information to be displayed as part of the message.
     */
    public static void processInvalidServiceRCWithRuntimeExc(String messageId, String serviceName, int returnCode, String data) {
        if (returnCode > 0) {
            Tr.error(tc, messageId, new Object[] { serviceName, Integer.toHexString(returnCode), data });
            throw new RuntimeException("Service " + serviceName + " failed with return code: " + returnCode);
        } else {
            throw new RuntimeException("Internal native failure detected while attempting to process service: " + serviceName);
        }
    }

    /**
     * Processes bad service return code or internal return codes while attempting to process a native service call.
     *
     * @param messageId   The message ID to use.
     * @param serviceName The target service name.
     * @param returnCode  The return code.
     * @param data        The extra relevant information to be displayed as part of the message.
     *
     * @throws XAException If there is a failure processing a native RRS service call.
     */
    public void processInvalidServiceRCWithXAExc(String messageId, String serviceName, int returnCode, int xaExceptionCode, String data) throws XAException {
        if (returnCode > 0) {
            Tr.error(tc, messageId, new Object[] { serviceName, Integer.toHexString(returnCode), data });
            throw new XAException(xaExceptionCode);
        } else {
            RuntimeException cause = new RuntimeException("Internal native failure detected while attempting to process service: " + serviceName);
            XAException xae = new XAException(XAException.XAER_RMERR);
            xae.initCause(cause);
            throw xae;
        }
    }

    /**
     * Collects global transaction data from the globalTxMap.
     *
     * @return A string representation of the data.
     */
    @Trivial
    @FFDCIgnore(Throwable.class)
    private String getGlobalTransactionMapData() {
        StringBuilder sb = new StringBuilder();

        try {
            if (!globalTxMap.isEmpty()) {
                for (Map.Entry<ByteArray, GlobalTransactionData> entry : globalTxMap.entrySet()) {
                    ByteArray gtrid = entry.getKey();
                    byte[] gtridBytes = gtrid.getBytes();
                    sb.append("GLOBAL_TRANSACTION: [GTRID: ");
                    sb.append((gtridBytes == null) ? "NULL" : Util.toHexString(gtridBytes) + "]\n");
                    sb.append("------------------\n\n");

                    GlobalTransactionData txData = entry.getValue();
                    ReadWriteLock rwLock = txData.getLock();
                    Lock wLock = rwLock.writeLock();
                    wLock.lock();
                    try {
                        sb.append(txData.getData());

                        byte[] uriRegistryToken = txData.getURIRegistryToken();
                        byte[] uriToken = txData.getURIToken();
                        byte[] contextToken = txData.getContext().getContextToken();
                        // Call ATR4RUSI
                        if (uriRegistryToken != null && uriToken != null) {
                            AsyncURSyncpointData asyncData = new AsyncURSyncpointData(this, uriToken, uriRegistryToken, rrsServices);
                            sb.append("\n" + asyncData.toString());
                        }
                        // Call ATR4RUSF
                        if (contextToken != null) {
                            RetrieveSideInformationFastReturnType rusfRt = rrsServices.retrieveSideInformationFast(contextToken, RRSServices.ATR_INTEREST_COUNT_MASK);
                            if (rusfRt.getReturnCode() == 0) {
                                sb.append("\n" + rusfRt.toString());
                            }
                        }
                    } catch (Throwable t) {
                        // Absorb exception and move on.
                    } finally {
                        wLock.unlock();
                    }
                    sb.append("\n\n");
                }
            } else {
                sb.append("No active global transactions found.");
            }
        } catch (Throwable t) {
            sb.append("NativeTransactionManager.getGlobalTransactionMapData. Error: " + t.toString());
        }

        return sb.toString();
    }

    /**
     * Collects local transaction data from the localTxMap.
     *
     * @return A string representation of the data.
     */
    @Trivial
    @FFDCIgnore(Throwable.class)
    private String getLocalTransactionMapData() {
        StringBuilder sb = new StringBuilder();

        try {
            if (!localTxMap.isEmpty()) {
                for (Map.Entry<UOWCoordinator, LocalTransactionData> entry : localTxMap.entrySet()) {
                    UOWCoordinator uowc = entry.getKey();
                    sb.append("LOCAL_TRANSACTION: [" + uowc + "]\n");
                    sb.append("-----------------\n\n");

                    LocalTransactionData txData = entry.getValue();
                    ReadWriteLock rwLock = txData.getLock();
                    Lock wLock = rwLock.writeLock();
                    wLock.lock();
                    try {
                        sb.append(txData.getData());
                        byte[] contextToken = txData.getContextToken();

                        if (contextToken != null) {
                            RetrieveSideInformationFastReturnType rusfRt = rrsServices.retrieveSideInformationFast(contextToken, RRSServices.ATR_INTEREST_COUNT_MASK);
                            if (rusfRt.getReturnCode() == 0) {
                                sb.append("\n" + rusfRt.toString());
                            }
                        }
                    } catch (Throwable t) {
                        // Absorb exception and move on.
                    } finally {
                        wLock.unlock();
                    }

                    sb.append("\n\n");
                }
            } else {
                sb.append("No active local transactions found.");
            }
        } catch (Throwable t) {
            sb.append("NativeTransactionManager.getLocalTransactionMapData. Error: " + t.toString());
        }

        return sb.toString();
    }

    /**
     * Collects restart transaction data from the restartedRMMap.
     *
     * @return A string representation of the data.
     */
    @Trivial
    @FFDCIgnore(Throwable.class)
    private String getRestartTransactionMapData() {
        StringBuilder sb = new StringBuilder();

        try {
            if (!restartedRMMap.isEmpty()) {
                for (Map.Entry<String, HashMap<ByteArray, RestartURData>> rmEntry : restartedRMMap.entrySet()) {
                    String resMgrName = rmEntry.getKey();
                    sb.append("RESOURCE_MANAGER: [" + resMgrName.trim() + "]");
                    HashMap<ByteArray, RestartURData> urDataMap = rmEntry.getValue();
                    if (urDataMap != null && urDataMap.size() > 0) {
                        sb.append("\n{");
                        for (Map.Entry<ByteArray, RestartURData> urMapEntry : urDataMap.entrySet()) {
                            RestartURData urData = urMapEntry.getValue();
                            sb.append("\n" + urData.printData());
                        }
                        sb.append("\n}");
                    } else {
                        sb.append("\nNo active recovery data found for resource manager: " + resMgrName.trim());
                    }
                    sb.append("\n\n");
                }
            } else {
                sb.append("No active recovery data found.");
            }
        } catch (Throwable t) {
            sb.append("NativeTransactionManager.getRestartTransactionMapData. Error: " + t.toString());
        }

        return sb.toString();
    }

    /**
     * Retrieves a byte representation of the server's data:
     * [ServerName]:[ServerUUID]:[ServerConfigDir]
     *
     * @param inEbcdic True if the string data is to be converted to ebcdic.
     *                     False otherwise.
     *
     * @return The server's data in byte form.
     */
    private byte[] getServerDataBytes(boolean inEbcdic) {

        // Check for cached data first. Note that we are assuming
        // that the server data (name, uuid, and configDir)
        // cannot change at runtime.
        if (inEbcdic == true && ebcdicServerDataBytes != null) {
            return ebcdicServerDataBytes;
        }
        if (inEbcdic == false && asciiServerDataBytes != null) {
            return asciiServerDataBytes;
        }

        byte[] serverData = null;

        if (locationAdmin != null) {
            StringBuilder sb = new StringBuilder();
            sb.append(locationAdmin.getServerName());
            sb.append(":");
            sb.append(locationAdmin.getServerId().toString());
            sb.append(":ServerConfigDir=");
            sb.append(locationAdmin.resolveString(WsLocationConstants.SYMBOL_SERVER_CONFIG_DIR));
            String data = sb.toString();

            if (inEbcdic) {
                serverData = ebcdicServerDataBytes = convertStringToEbcdicBytes(data, data.length(), false);
            } else {
                serverData = asciiServerDataBytes = data.getBytes();
            }
        }

        return serverData;
    }

    /**
     * Retrieves an instance of the persistent interest data wrapper.
     * This method's existence is purely for performance due to the fact that
     * what needs to be hardened is generic server information that is not
     * particular to a UR and is not intended for main line or recovery use.
     *
     * @return The persistent interest data wrapper.
     */
    private PersistentInterestData getPdataWrapper() {
        if (pdataWrapper == null) {
            pdataWrapper = new PersistentInterestData(PersistentInterestData.RESOURCE_INTEREST_TYPE, GlobalTransactionData.CURRENT_PDATA_VERSION);
        }

        return pdataWrapper;
    }

    //-------------------------------------------------------------------------
    // XAResourceFactory interface implementation.
    //-------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public XAResource getXAResource(Serializable xaResourceInfo) throws XAResourceNotAvailableException {

        if (xaResourceInfo == null) {
            throw new XAResourceNotAvailableException(new Exception("NULL resource info input."));
        }

        return new NativeGlobalXAResource(this, rrsServices, xaResourceInfo);
    }

    /**
     * Obtains a one phase capable native XA resource
     *
     * @param pmiName The resource's factory name (jndi name)
     *
     * @return A native one phase XA resource.
     *
     * @throws XAResourceNotAvailableException
     */
    @Override
    public XAResource getOnePhaseXAResource(UOWCoordinator uowCoord) {
        return new NativeLocalXAResource(this, uowCoord, rrsServices);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public XAResource getTwoPhaseXAResource(Xid xid) {
        return new NativeGlobalXAResource(this, rrsServices);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Serializable getXAResourceInfo(Xid xid) {

        // This method will return a string until the transaction manager
        // starts supporting the registration of proprietary resource info objects.
        // This string represents that resource manager name, which is all we care
        // (for now) for recovery.
        return getResourceManagerName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void delist(UOWCoordinator uowc, XAResource xaResource) {

        // This method only handle delistments for resources enlisted for
        // cleanup (LTC resolution setting = Application ).

        if (uowc == null) {
            throw new IllegalArgumentException("Invalid transaction coordinator.");
        }

        if (xaResource == null) {
            throw new IllegalArgumentException("Invalid XA resource.");
        }

        LocalTransactionData localTxData = localTxMap.get(uowc);

        if (localTxData == null) {
            throw new IllegalStateException("Delist processing. No transaction data found for UOWCoordinator: " + uowc);
        }

        List<XAResource> xaList = localTxData.getXAResourceList();

        // The XA list should never be empty.
        if (xaList.size() == 0) {
            throw new IllegalStateException("No resources found for this UOWCoordinator: " + uowc);
        }

        // If there is only one entry left in the list, it means that all of the native resources
        // enlisted for cleanup were committed or rolled back and there are no danglers.
        if (xaList.size() == 1) {
            localTxMap.remove(uowc);
        } else {
            xaList.remove(xaResource);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void enlist(UOWCoordinator uowc, XAResource xaResource) {

        if (uowc == null) {
            throw new IllegalArgumentException("Invalid transaction coordinator.");
        }

        if (xaResource == null) {
            throw new IllegalArgumentException("Invalid XA resource.");
        }

        if (uowc.getRollbackOnly()) {
            throw new IllegalStateException("Transaction marked rollbackOnly. Resource cannot be enlisted.");
        }

        LocalTransactionData localTxData = localTxMap.get(uowc);

        // If this is the first time we are seeing this transaction.
        if (localTxData == null) {
            Context currContext = getContextOnCurrentThread();
            localTxData = new LocalTransactionData(currContext);
            localTxMap.put(uowc, localTxData);
        }

        List<XAResource> xaList = localTxData.getXAResourceList();
        xaList.add(xaResource);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroyXAResource(XAResource xaResource) throws DestroyXAResourceException {
        // NO-OP.
    }

    //-------------------------------------------------------------------------
    // UOWEventListener interface implementation
    //-------------------------------------------------------------------------

    /**
     * Unit of work event listener interface implementation.
     * This method is called by the core transaction service
     * component when events such as tx.suspend and tx.resume
     * take place.
     *
     * @param uowc  The unit of work reference for which the current event
     *                  is taking place.
     * @param event The event type.
     * @param data  The data associated with a particular event.
     */
    @Override
    public void UOWEvent(UOWEventEmitter uow, int event, Object data) {

        // uow is a UOWCoordinator really
        final UOWCoordinator uowc = (UOWCoordinator) uow;

        switch (event) {
            case UOWEventListener.POST_BEGIN:
                if (contextManager.isInitialized() == false) {
                    return;
                }
                contextManager.begin(uowc);
                break;
            case UOWEventListener.POST_END:
                if (contextManager.isInitialized() == false) {
                    throw new IllegalStateException("Invalid context manager");
                }

                if (uowc.isGlobal() == false) {
                    localTxPostEndCleanup(uowc);
                }

                contextManager.end(uowc);
                break;
            case UOWEventListener.SUSPEND:
                if (contextManager.isInitialized() == false) {
                    return;
                }
                contextManager.suspend(uowc);
                break;
            case UOWEventListener.RESUME:
                if (contextManager.isInitialized() == false) {
                    return;
                }
                contextManager.resume(uowc);
                break;
            default:
                break;
        }
    }

    //-------------------------------------------------------------------------
    // IntrospectableService interface implementation
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return this.getClass().getCanonicalName() + " data introspection.";
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    /** {@inheritDoc} */
    @Override
    public void introspect(OutputStream out) throws IOException {
        StringBuilder sb = new StringBuilder();
        byte[] serverData = getServerDataBytes(false);

        // Add a quick summary.
        sb.append("\nNativeTransactioManager Data.");
        sb.append("\n\n1. Summary.");
        sb.append("\n\nServerData (NAME:UUID:CONFIGD) : ");
        sb.append((serverData == null) ? "NULL" : new String(serverData));
        sb.append("\nResourceManagerName            : ");
        sb.append(resMgrName);
        sb.append("\nResourceManagerNameRegistryToken   : ");
        sb.append((resMgrNameRegistryToken == null) ? "NULL" : Util.toHexString(resMgrNameRegistryToken));
        sb.append("\nResourceManagerToken           : ");
        sb.append((resMgrToken == null) ? "NULL" : Util.toHexString(resMgrToken));
        sb.append("\nResourceManagerRegistryToken   : ");
        sb.append((resMgrRegistryToken == null) ? "NULL" : Util.toHexString(resMgrRegistryToken));
        sb.append("\nResourceManagerLogName         : ");
        sb.append((resMgrLogName == null) ? "NULL" : Util.toHexString(resMgrLogName));
        sb.append("\nIsBundleActive                 : ");
        sb.append(active);
        sb.append("\nShutdownTimeoutMillis          : ");
        sb.append(shutdownTimeoutMillis);
        sb.append("\nIsCtxManagerInitialized        : ");
        sb.append(contextManager.isInitialized());
        sb.append("\nUserDefinedRMNamePrefix        : ");
        sb.append(resMgrNamePrefix);

        // Collect Object references.
        sb.append("\n\n2. Object References. ");
        sb.append("\n\nRRSServices                    : ");
        sb.append(rrsServices);
        sb.append("\nContextManager                 : ");
        sb.append(contextManager);
        sb.append("\nWsLocationAdmin                : ");
        sb.append(locationAdmin);

        // Display data for all active global transactions.
        sb.append("\n\n3. Active Global Transaction Information.\n\n");
        sb.append(getGlobalTransactionMapData());

        // Display data for all active local transactions.
        sb.append("\n\n4. Active Local Transaction Information.\n\n");
        sb.append(getLocalTransactionMapData());

        // Display data for all active recovery work.
        sb.append("\n\n5. Transaction Recovery Information.\n\n");
        sb.append(getRestartTransactionMapData());

        // Write to the stream and flush the data. The stream is closed by the caller.
        String introspectionData = sb.toString();
        out.write(introspectionData.getBytes());
        out.flush();
    }
}