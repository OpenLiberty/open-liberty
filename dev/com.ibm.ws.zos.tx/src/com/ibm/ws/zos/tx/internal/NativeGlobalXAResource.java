/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.zos.tx.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.tx.jta.impl.XidImpl;
import com.ibm.tx.util.ByteArray;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.Transaction.JTA.Util;
import com.ibm.ws.zos.tx.internal.rrs.BeginTransactionReturnType;
import com.ibm.ws.zos.tx.internal.rrs.PrepareAgentURReturnType;
import com.ibm.ws.zos.tx.internal.rrs.RRSServices;
import com.ibm.ws.zos.tx.internal.rrs.RegistryException;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveURDataReturnType;

public class NativeGlobalXAResource implements XAResource {
    private static final TraceComponent tc = Tr.register(NativeGlobalXAResource.class);

    /**
     * The string which is added to exceptions when the native TM instance has deregistered.
     */
    private static final String TM_INACTIVE_RESOLUTION_EXCEPTION_STRING = "The transaction branch cannot be resolved because the NativeTransactionManager instance which created the branch has been deactivated.  The server must be restarted to allow this transaction to complete.";

    /**
     * RRS services object reference.
     */
    private final RRSServices rrsServices;

    /**
     * Native transaction manager reference.
     */
    private final NativeTransactionManager natvTxMgr;

    /**
     * Resource info reference.
     */
    private Serializable resourceInfo;

    /**
     * Mainline constructor.
     *
     * @param natvTxMgr   The NativeTransactionManager object reference.
     * @param rrsServices The RRSServices object reference.
     */
    public NativeGlobalXAResource(NativeTransactionManager natvTxMgr, RRSServices rrsServices) {
        this.rrsServices = rrsServices;
        this.natvTxMgr = natvTxMgr;
    }

    /**
     * Recovery constructor.
     *
     * @param natvTxMgr    The NativeTransactionManager object reference.
     * @param rrsServices  The RRSServices object reference.
     * @param resourceInfo The serializable resource information object reference.
     */
    public NativeGlobalXAResource(NativeTransactionManager natvTxMgr, RRSServices rrsServices, Serializable resourceInfo) {
        this.rrsServices = rrsServices;
        this.resourceInfo = resourceInfo;
        this.natvTxMgr = natvTxMgr;
    }

    /**
     * Associates the resource with the transaction.
     */
    @Override
    public void start(Xid xid, int flags) throws XAException {

        Map<ByteArray, GlobalTransactionData> globalTxMap = natvTxMgr.getGlobalTxMap();
        ByteArray gtrid = new ByteArray(xid.getGlobalTransactionId());
        GlobalTransactionData txData = null;

        // Start can be called multiple times. If we have already gone through start,
        // make sure not to do that again. Just add this resource XA resource list and return.
        if (globalTxMap.containsKey(gtrid)) {
            txData = globalTxMap.get(gtrid);
            if (txData.getXAStarted()) {
                txData.getXAResourceList().add(this);
                return;
            } else {
                throw new IllegalStateException("Start processing for transaction should have already taken place. Prior start processing failure is possible.");
            }
        }

        // Begin a global transaction with RRS.
        BeginTransactionReturnType btrt = rrsServices.beginTransaction(RRSServices.ATR_GLOBAL_MODE);

        if (btrt == null) {
            natvTxMgr.processInvalidServiceRCWithXAExc("INVALID_RRS_SERVICE_RC", "ATR4BEG", -1, XAException.XAER_RMERR, "");
        }

        XidImpl xidImpl = new XidImpl(xid);
        int rc = btrt.getReturnCode();
        byte[] urToken = null;
        byte[] urid = null;

        if (rc == RRSServices.ATR_OK) {
            urid = btrt.getURId();
            urToken = btrt.getURToken();
        } else {
            natvTxMgr.processInvalidServiceRCWithXAExc("INVALID_RRS_SERVICE_RC", "ATR4BEG", rc, XAException.XAER_RMERR, xidImpl.toString());
        }

        // Set the work ID.
        rc = rrsServices.setWorkIdentifier(urToken, xidImpl.toBytes());

        if (rc != RRSServices.ATR_OK) {
            StringBuilder sb = new StringBuilder();
            sb.append("URToken: ");
            sb.append((urToken == null) ? "NULL" : Util.toHexString(urToken));
            sb.append(", Xid: ");
            sb.append(xidImpl.toString());
            natvTxMgr.processInvalidServiceRCWithXAExc("INVALID_RRS_SERVICE_RC", "ATR4SWID", rc, XAException.XAER_RMERR, sb.toString());
        }

        // Add an entry for the current transaction to the map tracking transactions.
        txData = new GlobalTransactionData(xidImpl, natvTxMgr.getResourceManagerName(), natvTxMgr.getResourceManagerNameRegistryToken(), natvTxMgr.getResourceManagerToken(), natvTxMgr.getResourceManagerRegistryToken(), urid, urToken, natvTxMgr.getContextOnCurrentThread(), RRSServices.ATR_IN_FLIGHT, true);

        txData.getXAResourceList().add(this);
        globalTxMap.put(gtrid, txData);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int prepare(Xid xid) throws XAException {
        int vote = XA_OK;

        // Iterate over the list of resources for the current transaction. We
        // vote read only on N-1 resources. The last resource should be called for
        // commit one phase or for prepare.
        Map<ByteArray, GlobalTransactionData> globalTxMap = natvTxMgr.getGlobalTxMap();
        GlobalTransactionData txData = globalTxMap.get(new ByteArray(xid.getGlobalTransactionId()));

        // Make sure we have the given Xid on record.
        if (txData == null) {
            throw new XAException(XAException.XAER_NOTA);
        }

        // Obtain the data lock.
        ReadWriteLock rwLock = txData.getLock();
        Lock wLock = rwLock.writeLock();
        wLock.lock();

        try {
            List<XAResource> xaList = txData.getXAResourceList();

            // The XA resource list should not be empty.
            if (xaList.isEmpty()) {
                throw new IllegalStateException("Prepare processing. No resources found for xid: " + xid);
            }

            // If there are more than one entry in the xa resource list, vote read-only and return.
            if (xaList.size() > 1) {
                xaList.remove(this);
                return XAResource.XA_RDONLY;
            }

            // If the UR was asynchronously rolled back-forgotten (i.e. deactivation),
            // and the TM has called us for prepare, return with the appropriate
            // exception.
            if (txData.isUrForgotten()) {
                globalTxMap.remove(new ByteArray(xid.getGlobalTransactionId()));
                throw new XAException(XAException.XAER_NOTA);
            }

            // There is a single entry in the XA resource list and there are most likely other resources
            // involved with the transaction such that their presence prevented the transaction
            // manager from optimizing (commit one phase).
            // If there is no URIToken, this means that we have not expressed interest in the UR. Do it now.
            if (txData.getURIToken() == null) {
                try {
                    natvTxMgr.expressInterest(txData);
                } catch (RegistryException rex) {
                    globalTxMap.remove(new ByteArray(xid.getGlobalTransactionId()));
                    throw (XAException) new XAException(XAException.XAER_NOTA).initCause(rex);
                }
            }

            // ---------------------------------------------------------------
            // Call agent prepare.  The authorized code will also express an
            // interest in the context containing the UR.
            // ---------------------------------------------------------------
            try {
                boolean processInvalidRC = true;
                int xaErrorCode = 0;
                Context contextTokenWrapper = txData.getContext();
                byte[] uriRegistryToken = txData.getURIRegistryToken();
                byte[] contextRegistryToken = contextTokenWrapper.getContextRegistryToken();
                byte[] rmToken = txData.getRMRegistryToken();

                PrepareAgentURReturnType part = rrsServices.prepareAgentUR(uriRegistryToken, contextRegistryToken, rmToken, RRSServices.ATR_DEFER_IMPLICIT);
                int agentPrepareReturnCode = part.getAgentPrepareReturnCode();
                int expressContextInterestReturnCode = part.getExpressContextInterestReturnCode();
                String failingService = null;
                int failingRc = 0;

                if (expressContextInterestReturnCode == RRSServices.CTX_OK) {
                    failingService = "ATR4APRP";
                    failingRc = agentPrepareReturnCode;
                    switch (agentPrepareReturnCode) {
                        case RRSServices.ATR_OK:
                            // -----------------------------------------------
                            // If agent prepare succeeded, we are in-doubt.
                            // Save the context interest token in the tx data
                            // so that we can delete the interest when we
                            // resolve the UR.
                            // -----------------------------------------------
                            processInvalidRC = false;
                            txData.setURState(RRSServices.ATR_IN_DOUBT);
                            contextTokenWrapper.setContextInterestRegistryToken(part.getContextInterestRegistryToken());
                            break;
                        case RRSServices.ATR_FORGET:
                            processInvalidRC = false;
                            txData.setUrForgotten(true);
                            vote = XAResource.XA_RDONLY;
                            break;
                        case RRSServices.ATR_BACKED_OUT:
                        case RRSServices.ATR_BACKED_OUT_OUTCOME_PENDING:
                            xaErrorCode = XAException.XA_RBROLLBACK;
                            txData.setUrForgotten(true);
                            // This is it - we're not getting called again.
                            globalTxMap.remove(new ByteArray(xid.getGlobalTransactionId()));
                            break;
                        case RRSServices.ATR_NOT_AVAILABLE:
                            xaErrorCode = XAException.XAER_RMFAIL;
                            break;
                        case RRSServices.ATR_BACKED_OUT_OUTCOME_MIXED:
                            txData.setUrForgotten(true);
                            xaErrorCode = XAException.XA_HEURMIX;
                            break;
                        case RRSServices.ATR_URI_TOKEN_INV:
                            xaErrorCode = XAException.XAER_NOTA;
                            break;
                        case RRSServices.ATR_UR_STATE_ERROR:
                            // Check for asynchronous processing.
                            byte[] uriToken = txData.getURIToken();
                            AsyncURSyncpointData asyncData = new AsyncURSyncpointData(natvTxMgr, uriToken, uriRegistryToken, rrsServices);

                            if (asyncData.getTerminatingSyncpoint() || asyncData.getApplicationBackout()) {
                                processInvalidRC = false;
                                forgetUR(uriRegistryToken, txData);
                            }

                            if (asyncData.getHeuristicMixed()) {
                                throw new XAException(XAException.XA_HEURMIX);
                            } else {
                                throw new XAException(XAException.XA_RBROLLBACK);
                            }
                        default:
                            xaErrorCode = XAException.XAER_RMERR;
                            break;
                    }
                } else {
                    // Express context interest failed.  Can't prepare.
                    failingService = "CTX4EINT";
                    failingRc = expressContextInterestReturnCode;
                    xaErrorCode = XAException.XAER_RMERR;
                }

                if (processInvalidRC) {
                    natvTxMgr.processInvalidServiceRCWithXAExc("INVALID_RRS_SERVICE_RC", failingService, failingRc, xaErrorCode, txData.getData());
                }
            } catch (RegistryException rex) {
                // The URI token could not be found in the registry.
                throw (XAException) new XAException(XAException.XAER_NOTA).initCause(rex);
            }

            // If we were read-only, we're done.  We're only called for prepare on
            // non-restart resources.
            if (vote == XAResource.XA_RDONLY) {
                xaList.remove(this);
                globalTxMap.remove(new ByteArray(xid.getGlobalTransactionId()));
            }
        } finally {
            wLock.unlock();
        }

        return vote;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void commit(Xid xid, boolean onePhase) throws XAException {

        Map<ByteArray, GlobalTransactionData> globalTxMap = natvTxMgr.getGlobalTxMap();
        GlobalTransactionData txData = globalTxMap.get(new ByteArray(xid.getGlobalTransactionId()));

        // Make sure we have the given Xid on record.
        if (txData == null) {
            throw new XAException(XAException.XAER_NOTA);
        }

        // Obtain the data lock.
        ReadWriteLock rwLock = txData.getLock();
        Lock wLock = rwLock.writeLock();
        wLock.lock();

        try {
            List<XAResource> xaList = txData.getXAResourceList();

            // Restarted transactions do not have an XA resource list. However for normal transactions, make
            // sure that there is only 1 entry in the XA resource list.
            // That is because N-1 resource instances should have voted READ_ONLY on prepare
            // and should have removed themselves from the list.
            if (!txData.isRestarted() && xaList.size() != 1) {
                // TODO: What to do here ... remove N-1 and rollback the last one and throw a HEURISTIC ?
                throw new IllegalStateException("Commit processing. Invalid number of resources.");
            }

            // If prepare encountered some issues, the UR is most likely now forgotten because of
            // the ATR_DEFER_IMPLICIT log option.
            // If this is the case, remove the last entry form the map and return with the appropriate exception.
            if (txData.isUrForgotten()) {
                globalTxMap.remove(new ByteArray(xid.getGlobalTransactionId()));
                throw new XAException(XAException.XAER_NOTA);
            }

            // If the native transaction manager has been deactivated, we cannot tell RRS about the outcome
            // of the transaction.  Right now, all we can do is tell the transaction manager that we can't
            // resolve the transaction right now.  In the future, we'd like to try to locate a new
            // native transaction manager instance which is connected to RRS.  Currently, classloader issues
            // prevent us from doing that.
            if (natvTxMgr.isActive() == false) {
                byte[] urid = txData.getURId();
                String uridString = (urid == null) ? "*URID NOT SET*" : new ByteArray(urid).toString();
                Tr.error(tc, "TM_INACTIVE_CANT_RESOLVE_XARESOURCE", new Object[] { uridString, xid.toString() });
                throw (XAException) new XAException(XAException.XA_RETRY).initCause(new IllegalStateException(TM_INACTIVE_RESOLUTION_EXCEPTION_STRING));
            }

            // If the transaction manager is optimizing: end the UR if the right context is on the
            // current thread; otherwise, express interest and delegate commit to RRS.
            if (onePhase) {
                Context currContext = txData.getContext();
                byte[] currTxCtxtoken = currContext.getContextToken();
                Context currThreadCtx = natvTxMgr.getContextOnCurrentThread();
                byte[] currThreadCtxtoken = currThreadCtx.getContextToken();
                try {
                    if (Arrays.equals(currTxCtxtoken, currThreadCtxtoken)) {
                        endUR(txData, true);
                    } else {
                        if (txData.getURIToken() == null) {
                            try {
                                natvTxMgr.expressInterest(txData);
                            } catch (RegistryException rex) {
                                globalTxMap.remove(new ByteArray(xid.getGlobalTransactionId()));
                                throw (XAException) new XAException(XAException.XAER_NOTA).initCause(rex);
                            }
                        }
                        // Use the EXPLICIT log option to be able to remember heuristic outcomes.
                        delegateCommitAgentUR(txData, RRSServices.ATR_DEFER_EXPLICIT, RRSServices.ATR_STANDARD_COMMIT_MASK);
                    }
                } catch (XAException xae) {
                    // If we backed out, we are finished with the transaction, so
                    // we need to remove the transaction from our maps.
                    if ((xae.errorCode >= XAException.XA_RBBASE) && (xae.errorCode <= XAException.XA_RBEND)) {
                        globalTxMap.remove(new ByteArray(xid.getGlobalTransactionId()));
                        if (txData.isRestarted()) {
                            natvTxMgr.removeEntryFromRestartedURMap(txData);
                        }
                    }
                    throw xae;
                }
            } else {
                // Use the EXPLICIT log option to be able to remember heuristic outcomes.
                commitAgentUR(txData, RRSServices.ATR_DEFER_EXPLICIT);
            }

            // We are done. Remove this transaction from our maps.
            globalTxMap.remove(new ByteArray(xid.getGlobalTransactionId()));

            // If we are processing commit for a restarted transaction undergoing recovery,
            // tidy up the restarted transaction map.
            if (txData.isRestarted()) {
                natvTxMgr.removeEntryFromRestartedURMap(txData);
            }
        } finally {
            wLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rollback(Xid xid) throws XAException {

        Map<ByteArray, GlobalTransactionData> globalTxMap = natvTxMgr.getGlobalTxMap();
        GlobalTransactionData txData = globalTxMap.get(new ByteArray(xid.getGlobalTransactionId()));

        // Make sure we have the given Xid on record.
        if (txData == null) {
            throw new XAException(XAException.XAER_NOTA);
        }

        // Obtain the data lock.
        ReadWriteLock rwLock = txData.getLock();
        Lock wLock = rwLock.writeLock();
        wLock.lock();

        try {
            List<XAResource> xaList = txData.getXAResourceList();

            // Restarted transactions do not have an XA resource list.
            if (!txData.isRestarted()) {
                if (xaList.isEmpty()) {
                    throw new XAException("Rollback processing. No resources found.");
                }

                // If there is more than one entry in the XA resource list, remove
                // the entry from the map and return.
                if (xaList.size() > 1) {
                    xaList.remove(this);
                    return;
                }
            }

            // If prepare encountered some issues, the UR is most likely now forgotten because of
            // the ATR_DEFER_IMPLICIT log option.
            // If this is the case, remove the last entry form the map and return with the appropriate exception.
            if (txData.isUrForgotten()) {
                globalTxMap.remove(new ByteArray(xid.getGlobalTransactionId()));
                throw new XAException(XAException.XAER_NOTA);
            }

            // If the native transaction manager has been deactivated, we cannot tell RRS about the outcome
            // of the transaction.  Right now, all we can do is tell the transaction manager that we can't
            // resolve the transaction right now.  In the future, we'd like to try to locate a new
            // native transaction manager instance which is connected to RRS.  Currently, classloader issues
            // prevent us from doing that.
            if (natvTxMgr.isActive() == false) {
                byte[] urid = txData.getURId();
                String uridString = (urid == null) ? "*URID NOT SET*" : new ByteArray(urid).toString();
                Tr.error(tc, "TM_INACTIVE_CANT_RESOLVE_XARESOURCE", new Object[] { uridString, xid.toString() });
                throw (XAException) new XAException(XAException.XA_RETRY).initCause(new IllegalStateException(TM_INACTIVE_RESOLUTION_EXCEPTION_STRING));
            }

            // If the URIToken for this method has not been set, we most likely have not
            // gone through expression of interest yet or it failed during prepare. Try it again.
            byte[] uriToken = txData.getURIToken();
            Context currContext = natvTxMgr.getContextOnCurrentThread();
            Context txContext = txData.getContext();
            boolean canCallEnd = (uriToken != null) ? false : Arrays.equals(txContext.getContextToken(), currContext.getContextToken());

            if ((uriToken == null) && (canCallEnd)) {
                endUR(txData, false);
            } else {
                if (uriToken == null) {
                    try {
                        natvTxMgr.expressInterest(txData);
                    } catch (RegistryException rex) {
                        globalTxMap.remove(new ByteArray(xid.getGlobalTransactionId()));
                        throw (XAException) new XAException(XAException.XAER_NOTA).initCause(rex);
                    }
                }

                // Use the EXPLICIT log option to be able to remember heuristic outcomes.
                backoutAgentUR(txData, RRSServices.ATR_DEFER_EXPLICIT);
            }

            // We are done. Remove this transaction from our maps.
            globalTxMap.remove(new ByteArray(xid.getGlobalTransactionId()));

            // If we are processing rollback for a restarted transaction undergoing recovery,
            // tidy up the restarted transaction map.
            if (txData.isRestarted()) {
                natvTxMgr.removeEntryFromRestartedURMap(txData);
            }
        } finally {
            wLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forget(Xid xid) throws XAException {

        Map<ByteArray, GlobalTransactionData> globalTxMap = natvTxMgr.getGlobalTxMap();
        GlobalTransactionData txData = globalTxMap.get(new ByteArray(xid.getGlobalTransactionId()));

        // Make sure we have the given Xid on record.
        if (txData == null) {
            throw new XAException(XAException.XAER_NOTA);
        }

        // Obtain the data lock.
        ReadWriteLock rwLock = txData.getLock();
        Lock wLock = rwLock.writeLock();
        wLock.lock();

        try {
            List<XAResource> xaList = txData.getXAResourceList();

            // When forget gets called, we should be the last entry in the resource list. All other
            // entries should have been removed during prepare or rollback.
            // Restarted transactions do not have an XA resource list.
            if (!txData.isRestarted() && xaList.size() != 1) {
                throw new XAException("Forget processing. Invalid number of resources.");
            }

            // If the UR was already forgotten for some reason, there is nothing to do, but
            // to remove the last entry form the map and return with the appropriate exception.
            if (txData.isUrForgotten()) {
                globalTxMap.remove(new ByteArray(xid.getGlobalTransactionId()));
                throw new XAException(XAException.XAER_NOTA);
            }

            // If the native transaction manager has been deactivated, we cannot tell RRS about the outcome
            // of the transaction.  Right now, all we can do is tell the transaction manager that we can't
            // resolve the transaction right now.  In the future, we'd like to try to locate a new
            // native transaction manager instance which is connected to RRS.  Currently, classloader issues
            // prevent us from doing that.
            if (natvTxMgr.isActive() == false) {
                byte[] urid = txData.getURId();
                String uridString = (urid == null) ? "*URID NOT SET*" : new ByteArray(urid).toString();
                Tr.error(tc, "TM_INACTIVE_CANT_RESOLVE_XARESOURCE", new Object[] { uridString, xid.toString() });
                throw (XAException) new XAException(XAException.XA_RETRY).initCause(new IllegalStateException(TM_INACTIVE_RESOLUTION_EXCEPTION_STRING));
            }

            // Forget the UR.
            try {
                boolean processInvalidRC = true;
                int xaErrorCode = 0;
                byte[] uriRegistryToken = txData.getURIRegistryToken();
                int rc = rrsServices.forgetAgentURInterest(uriRegistryToken, RRSServices.ATR_DEFER);

                switch (rc) {
                    case RRSServices.ATR_OK:
                    case RRSServices.ATR_OK_NO_CONTEXT:
                    case RRSServices.ATR_FORGET_NOT_REQUIRED:
                        txData.setUrForgotten(true);
                        processInvalidRC = false;
                        globalTxMap.remove(new ByteArray(xid.getGlobalTransactionId()));
                        break;
                    case RRSServices.ATR_NOT_AVAILABLE:
                        xaErrorCode = XAException.XAER_RMFAIL;
                        break;
                    case RRSServices.ATR_URI_TOKEN_INV:
                        xaErrorCode = XAException.XAER_NOTA;
                        break;
                    default:
                        xaErrorCode = XAException.XAER_RMERR;
                        break;
                }

                if (processInvalidRC) {
                    natvTxMgr.processInvalidServiceRCWithXAExc("INVALID_RRS_SERVICE_RC", "ATR4AFGT", rc, xaErrorCode, txData.getData());
                }
            } catch (RegistryException rex) {
                throw (XAException) new XAException(XAException.XAER_NOTA).initCause(rex);
            }
            // We are done. Remove this transaction from our maps.
            globalTxMap.remove(new ByteArray(xid.getGlobalTransactionId()));

            // If we are processing forget for a restarted transaction undergoing recovery,
            // tidy up the restarted transaction map.
            if (txData.isRestarted()) {
                natvTxMgr.removeEntryFromRestartedURMap(txData);
            }
        } finally {
            wLock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Xid[] recover(int flag) throws XAException {

        // The resource information cannot be null during recovery. Currently, it represents
        // the resource manager name for which we are processing recovery.
        if (resourceInfo == null) {
            throw new RuntimeException("Invalid Resource Information Object. Recovery cannot proceed without this information.");
        }

        String loggedRMName = (String) resourceInfo;
        String currentRMName = natvTxMgr.getResourceManagerName();

        // By now, we must have resource manager name for the current server instance.
        if (currentRMName == null) {
            throw new IllegalStateException("No resource manager name associated with current server instance.");
        }

        Map<String, HashMap<ByteArray, RestartURData>> restartedRMMap = natvTxMgr.getRestartedRMMap();
        Map<ByteArray, RestartURData> restartedURMap = restartedRMMap.get(loggedRMName);

        // We are trying to process recovery for a resource that is not the current.
        if (!currentRMName.equalsIgnoreCase(loggedRMName)) {
            if (restartedURMap == null) {
                SetupWithRRSReturnData setupData = natvTxMgr.processInitialSetupWithRRS(loggedRMName, true);
                byte[] rmToken = setupData.getResMgrToken();
                byte[] rmRegistryToken = setupData.getResMgrRegistryToken();
                byte[] rmNameRegistryToken = setupData.getResMgrNameRegistryToken();
                Throwable throwable = setupData.getThrowable();

                try {
                    // If processInitialSetupWithRRS saved an exception process it now.
                    if (throwable != null) {
                        throw throwable;
                    }
                    natvTxMgr.restartWithRRS(loggedRMName, rmNameRegistryToken, rmToken, rmRegistryToken);
                    restartedURMap = restartedRMMap.get(loggedRMName);
                } catch (Throwable t) {
                    // Deregister the RM if successfully registered.
                    if (rmNameRegistryToken != null && rmRegistryToken != null) {
                        natvTxMgr.unregisterResourceManager(loggedRMName, rmNameRegistryToken, rmToken, rmRegistryToken);
                    }
                    XAException xae = new XAException("Recover call falied with exception: " + t.toString());
                    xae.errorCode = XAException.XAER_RMFAIL;
                    throw xae;
                }
            }
        }

        final ArrayList<Xid> xidsToRecover = new ArrayList<Xid>();
        int numOfURsToRecover = (restartedURMap == null) ? 0 : restartedURMap.size();
        if (numOfURsToRecover > 0) {
            Map<ByteArray, GlobalTransactionData> globalTxMap = natvTxMgr.getGlobalTxMap();
            for (RestartURData urData : restartedURMap.values()) {
                XidImpl xid = urData.getXid();
                if (!globalTxMap.containsKey(new ByteArray(xid.getGlobalTransactionId()))) {
                    GlobalTransactionData restartTxData = new GlobalTransactionData(urData, null); // No context.  We could pass the context we get back from RRS during restart.
                    globalTxMap.put(new ByteArray(xid.getGlobalTransactionId()), restartTxData);
                }
                xidsToRecover.add(xid);
            }
        }

        return xidsToRecover.toArray(new Xid[numOfURsToRecover]);
    }

    /**
     * Obtains the state for the specified UR interest.
     *
     * @param txData The object holding data for the transaction being processed.
     *
     * @return The UR state.
     *
     * @throws XAException
     */
    public int getURState(GlobalTransactionData txData) throws XAException {
        int urState = RRSServices.ATR_IN_RESET;
        byte[] uriToken = txData.getURIToken();
        RetrieveURDataReturnType rurdrt = rrsServices.retrieveURData(uriToken, RRSServices.ATR_EXTENDED_STATES);

        if (rurdrt == null) {
            natvTxMgr.processInvalidServiceRCWithXAExc("INVALID_RRS_SERVICE_RC", "ATR4RURD", -1, XAException.XAER_RMERR, "");
        }

        int rc = rurdrt.getReturnCode();

        if (rc == RRSServices.ATR_OK) {
            urState = rurdrt.getState();
        } else {
            natvTxMgr.processInvalidServiceRCWithXAExc("INVALID_RRS_SERVICE_RC", "ATR4RURD", rc, XAException.XAER_RMERR, txData.getData());
        }

        return urState;
    }

    /**
     * Process a commit agent UR service request.
     *
     * @param txData    The Global transaction data.
     * @param logOption The log option.
     *
     * @throws XAException
     */
    public void commitAgentUR(GlobalTransactionData txData, int logOption) throws XAException {

        try {
            boolean processInvalidRC = true;
            int xaErrorCode = 0;
            byte[] uriRegistryToken = txData.getURIRegistryToken();
            Context ctx = txData.getContext();
            byte[] ciRegistryToken = (ctx != null) ? ctx.getContextInterestRegistryToken() : null;

            // Commit the UR.
            int rc = rrsServices.commitAgentUR(uriRegistryToken, ciRegistryToken, logOption);

            switch (rc) {
                case RRSServices.ATR_OK:
                case RRSServices.ATR_COMMITTED_OUTCOME_PENDING:
                    processInvalidRC = false;
                    if (ctx != null) {
                        ctx.setContextInterestRegistryToken(null);
                    }
                    if (logOption == RRSServices.ATR_DEFER_IMPLICIT) {
                        txData.setUrForgotten(true);
                    }
                    break;
                case RRSServices.ATR_COMMITTED_OUTCOME_MIXED:
                    xaErrorCode = XAException.XA_HEURMIX;
                    if (ctx != null) {
                        ctx.setContextInterestRegistryToken(null);
                    }
                    if (logOption == RRSServices.ATR_DEFER_IMPLICIT) {
                        txData.setUrForgotten(true);
                    }
                    break;
                case RRSServices.ATR_NOT_AVAILABLE:
                    xaErrorCode = XAException.XAER_RMFAIL;
                    break;
                case RRSServices.ATR_URI_TOKEN_INV:
                    if (ctx != null) {
                        ctx.setContextInterestRegistryToken(null);
                    }
                    xaErrorCode = XAException.XAER_NOTA;
                    break;
                case RRSServices.ATR_UR_STATE_ERROR:
                    // Check for expected UR states.
                    byte[] uriToken = txData.getURIToken();
                    int urState = getURState(txData);
                    if (urState == RRSServices.ATR_IN_COMMIT ||
                        urState == RRSServices.ATR_IN_BACKOUT ||
                        urState == RRSServices.ATR_IN_END ||
                        urState == RRSServices.ATR_IN_COMPLETION ||
                        urState == RRSServices.ATR_IN_FORGET) {

                        AsyncURSyncpointData asyncData = new AsyncURSyncpointData(natvTxMgr, uriToken, uriRegistryToken, rrsServices);

                        if (asyncData.getTerminatingSyncpoint() ||
                            asyncData.getResolvedInDoubt() ||
                            asyncData.getApplicationBackout()) {
                            processInvalidRC = false;
                            if (logOption == RRSServices.ATR_DEFER_IMPLICIT) {
                                forgetUR(uriRegistryToken, txData);
                            }
                        }

                        if (!asyncData.getCommitted() || asyncData.getHeuristicMixed()) {
                            throw new XAException(XAException.XA_HEURMIX);
                        }
                    }
                    break;
                default:
                    xaErrorCode = XAException.XAER_RMERR;
                    break;
            }

            if (processInvalidRC) {
                natvTxMgr.processInvalidServiceRCWithXAExc("INVALID_RRS_SERVICE_RC", "ATR4ACMT", rc, xaErrorCode, txData.getData());
            } else {
                if (logOption == RRSServices.ATR_DEFER_EXPLICIT && !txData.isUrForgotten()) {
                    forgetUR(uriRegistryToken, txData);
                }
            }
        } catch (RegistryException rex) {
            throw (XAException) new XAException(XAException.XAER_NOTA).initCause(rex);
        }
    }

    /**
     * Processes a delegate commit agent UR service request.
     *
     * @param txData       The global transaction data.
     * @param logOption    The log option.
     * @param commitOption the commit option.
     *
     * @throws XAException
     */
    public void delegateCommitAgentUR(GlobalTransactionData txData, int logOption, int commitOption) throws XAException {
        try {
            boolean processInvalidRC = true;
            int xaErrorCode = 0;
            byte[] uriRegistryToken = txData.getURIRegistryToken();

            int rc = rrsServices.delegateCommitAgentUR(uriRegistryToken,
                                                       logOption,
                                                       commitOption);
            switch (rc) {
                case RRSServices.ATR_OK:
                case RRSServices.ATR_COMMITTED_OUTCOME_PENDING:
                    processInvalidRC = false;
                    if (logOption == RRSServices.ATR_DEFER_IMPLICIT) {
                        txData.setUrForgotten(true);
                    }
                    break;
                case RRSServices.ATR_FORGET:
                    processInvalidRC = false;
                    txData.setUrForgotten(true);
                    break;
                case RRSServices.ATR_BACKED_OUT:
                case RRSServices.ATR_BACKED_OUT_OUTCOME_PENDING:
                    xaErrorCode = XAException.XA_RBROLLBACK;
                    txData.setUrForgotten(true);
                    break;
                case RRSServices.ATR_COMMITTED_OUTCOME_MIXED:
                    xaErrorCode = XAException.XA_HEURMIX;
                    if (logOption == RRSServices.ATR_DEFER_IMPLICIT) {
                        txData.setUrForgotten(true);
                    }
                    break;
                case RRSServices.ATR_BACKED_OUT_OUTCOME_MIXED:
                    xaErrorCode = XAException.XA_HEURMIX;
                    txData.setUrForgotten(true);
                    break;
                case RRSServices.ATR_URI_TOKEN_INV:
                    xaErrorCode = XAException.XAER_NOTA;
                    break;
                case RRSServices.ATR_UR_STATE_ERROR:
                    // Check for expected UR states.
                    byte[] uriToken = txData.getURIToken();
                    int urState = getURState(txData);
                    if (urState == RRSServices.ATR_IN_COMMIT ||
                        urState == RRSServices.ATR_IN_BACKOUT ||
                        urState == RRSServices.ATR_IN_END ||
                        urState == RRSServices.ATR_IN_COMPLETION ||
                        urState == RRSServices.ATR_IN_FORGET) {

                        AsyncURSyncpointData asyncData = new AsyncURSyncpointData(natvTxMgr, uriToken, uriRegistryToken, rrsServices);

                        if (asyncData.getTerminatingSyncpoint() ||
                            asyncData.getResolvedInDoubt() ||
                            asyncData.getApplicationBackout()) {
                            processInvalidRC = false;
                            if (logOption == RRSServices.ATR_DEFER_IMPLICIT) {
                                forgetUR(uriRegistryToken, txData);
                            }
                        }

                        if (!asyncData.getCommitted() || asyncData.getHeuristicMixed()) {
                            throw new XAException(XAException.XA_HEURMIX);
                        }
                    }
                    break;
                default:
                    xaErrorCode = XAException.XAER_RMERR;
                    break;
            }

            if (processInvalidRC) {
                natvTxMgr.processInvalidServiceRCWithXAExc("INVALID_RRS_SERVICE_RC", "ATR4ADCT", rc, xaErrorCode, txData.getData());
            } else {
                if (logOption == RRSServices.ATR_DEFER_EXPLICIT && !txData.isUrForgotten()) {
                    forgetUR(uriRegistryToken, txData);
                }
            }
        } catch (RegistryException rex) {
            throw (XAException) new XAException(XAException.XAER_NOTA).initCause(rex);
        }
    }

    /**
     * Process a backout agent UR service request.
     *
     * @param txData    The Global transaction data.
     * @param logOption The log option.
     *
     * @throws XAException
     */
    public void backoutAgentUR(GlobalTransactionData txData, int logOption) throws XAException {
        try {
            boolean processInvalidRC = true;
            int xaErrorCode = 0;
            byte[] uriRegistryToken = txData.getURIRegistryToken();
            Context txContext = txData.getContext();
            byte[] ciRegistryToken = (txContext != null) ? txContext.getContextInterestRegistryToken() : null;

            // Backout the UR.
            int rc = rrsServices.backoutAgentUR(uriRegistryToken, ciRegistryToken, logOption);

            switch (rc) {
                case RRSServices.ATR_OK:
                case RRSServices.ATR_BACKED_OUT_OUTCOME_PENDING:
                    processInvalidRC = false;
                    if (txContext != null) {
                        txContext.setContextInterestRegistryToken(null);
                    }
                    if (logOption == RRSServices.ATR_DEFER_IMPLICIT) {
                        txData.setUrForgotten(true);
                    }
                    break;
                case RRSServices.ATR_BACKED_OUT_OUTCOME_MIXED:
                    xaErrorCode = XAException.XA_HEURMIX;
                    if (txContext != null) {
                        txContext.setContextInterestRegistryToken(null);
                    }
                    if (logOption == RRSServices.ATR_DEFER_IMPLICIT) {
                        txData.setUrForgotten(true);
                    }
                    break;
                case RRSServices.ATR_NOT_AVAILABLE:
                    xaErrorCode = XAException.XAER_RMFAIL;
                    break;
                case RRSServices.ATR_URI_TOKEN_INV:
                    xaErrorCode = XAException.XAER_NOTA;
                    if (txContext != null) {
                        txContext.setContextInterestRegistryToken(null);
                    }
                    break;
                case RRSServices.ATR_UR_STATE_ERROR:

                    // Check for expected UR states.
                    byte[] uriToken = txData.getURIToken();
                    int urState = getURState(txData);
                    if (urState == RRSServices.ATR_IN_COMMIT ||
                        urState == RRSServices.ATR_IN_BACKOUT ||
                        urState == RRSServices.ATR_IN_END ||
                        urState == RRSServices.ATR_IN_COMPLETION ||
                        urState == RRSServices.ATR_IN_FORGET) {

                        AsyncURSyncpointData asyncData = new AsyncURSyncpointData(natvTxMgr, uriToken, uriRegistryToken, rrsServices);

                        if (asyncData.getTerminatingSyncpoint() ||
                            asyncData.getResolvedInDoubt() ||
                            asyncData.getApplicationBackout()) {
                            processInvalidRC = false;
                            if (logOption == RRSServices.ATR_DEFER_IMPLICIT) {
                                forgetUR(uriRegistryToken, txData);
                            }
                        }

                        if (asyncData.getCommitted() || asyncData.getHeuristicMixed()) {
                            throw new XAException(XAException.XA_HEURMIX);
                        }
                    }
                    break;
                default:
                    xaErrorCode = XAException.XAER_RMERR;
                    break;
            }

            if (processInvalidRC) {
                natvTxMgr.processInvalidServiceRCWithXAExc("INVALID_RRS_SERVICE_RC", "ATR4ABAK", rc, xaErrorCode, txData.getData());
            } else {
                if (logOption == RRSServices.ATR_DEFER_EXPLICIT && !txData.isUrForgotten()) {
                    forgetUR(uriRegistryToken, txData);
                }
            }
        } catch (RegistryException rex) {
            throw (XAException) new XAException(XAException.XAER_NOTA).initCause(rex);
        }
    }

    /**
     * Calls RRS' end service to end the UR identified given urToken. In most
     * cases, the txData is marked as forgotten because the UR is gone.
     *
     * @param txData     Data for the transaction to be ended, including the UR token.
     * @param commitWork True if end is to be issued with the commit option, false
     *                       for the backout option.
     *
     * @throws XAException If ATREND returns a bad return code. The error code
     *                         inside the XAException will explain what the problem
     *                         was. XA_RBROLLBACK indicates the UR rolled back when
     *                         it was supposed to commit. XA_HEURMIX indicates a
     *                         heuristic condition. XAER_RMERR or XAER_NOTA
     *                         indicates an unknown error.
     */
    private void endUR(GlobalTransactionData txData, boolean commitWork) throws XAException {

        boolean processInvalidRC = true;
        int xaErrorCode = 0;
        byte[] urToken = txData.getURToken();
        int action = (commitWork) ? RRSServices.ATR_COMMIT_ACTION : RRSServices.ATR_ROLLBACK_ACTION;
        int rc = rrsServices.endUR(action, urToken);

        switch (rc) {
            case RRSServices.ATR_OK:
                processInvalidRC = false;
                txData.setUrForgotten(true);
                break;
            case RRSServices.ATR_COMMITTED_OUTCOME_PENDING:
                txData.setUrForgotten(true);
                if (!commitWork) {
                    xaErrorCode = XAException.XA_HEURMIX;
                }
                break;
            case RRSServices.ATR_BACKED_OUT:
            case RRSServices.ATR_BACKED_OUT_OUTCOME_PENDING:
                txData.setUrForgotten(true);
                if (commitWork) {
                    xaErrorCode = XAException.XA_RBROLLBACK;
                }
                break;
            case RRSServices.ATR_COMMITTED_OUTCOME_MIXED:
            case RRSServices.ATR_BACKED_OUT_OUTCOME_MIXED:
                txData.setUrForgotten(true);
                xaErrorCode = XAException.XA_HEURMIX;
                break;
            case RRSServices.ATR_UR_TOKEN_INV:
                xaErrorCode = XAException.XAER_NOTA;
                break;
            default:
                xaErrorCode = XAException.XAER_RMERR;
                break;
        }

        if (processInvalidRC) {
            natvTxMgr.processInvalidServiceRCWithXAExc("INVALID_RRS_SERVICE_RC", "ATR4END", rc, xaErrorCode, txData.getData());
        }
    }

    /**
     * Calls RRS' forget agent UR service.
     *
     * @param uriRegistryToken The token that is used to look up the URI token in the native registry. The URI token identifies the UR interest to be forgotten.
     * @param txData           The Global transaction data for the current transaction.
     *
     * @throws XAException
     */
    private void forgetUR(byte[] uriRegistryToken, GlobalTransactionData txData) throws XAException {
        try {
            int rc = rrsServices.forgetAgentURInterest(uriRegistryToken, RRSServices.ATR_DEFER);

            switch (rc) {
                case RRSServices.ATR_OK:
                case RRSServices.ATR_OK_NO_CONTEXT:
                    txData.setUrForgotten(true);
                    break;
                default:
                    natvTxMgr.processInvalidServiceRCWithXAExc("INVALID_RRS_SERVICE_RC", "ATR4AFGT", rc, XAException.XAER_RMERR, txData.getData());
                    break;
            }
        } catch (RegistryException rex) {
            throw (XAException) new XAException(XAException.XAER_NOTA).initCause(rex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void end(Xid xid, int flags) throws XAException {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getTransactionTimeout() throws XAException {
        throw new XAException(XAException.XAER_PROTO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean setTransactionTimeout(int seconds) throws XAException {
        throw new XAException(XAException.XAER_PROTO);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isSameRM(XAResource theXAResource) throws XAException {
        return false;
    }

    /**
     * Collects the data associated with this XA resource.
     *
     * @return A string representation of the data associated with this XA transaction.
     */
    @Trivial
    public String getData() {
        StringBuilder sb = new StringBuilder();
        sb.append("NativeGlobalXAResource [");
        sb.append("NativeTxManager: ");
        sb.append(natvTxMgr);
        sb.append(", ResourceInfo: ");
        sb.append(resourceInfo);
        sb.append("]");
        return sb.toString();
    }
}