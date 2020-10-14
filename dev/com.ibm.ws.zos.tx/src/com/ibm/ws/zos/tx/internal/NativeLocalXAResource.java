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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

import com.ibm.tx.jta.OnePhaseXAResource;
import com.ibm.tx.util.ByteArray;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.Transaction.JTA.Util;
import com.ibm.ws.Transaction.UOWCoordinator;
import com.ibm.ws.zos.tx.internal.rrs.RRSServices;
import com.ibm.ws.zos.tx.internal.rrs.RetrieveSideInformationFastReturnType;

/**
 * One phase XA resource implementation.
 */
public class NativeLocalXAResource implements OnePhaseXAResource {

    private final RRSServices rrsServices;

    private final NativeTransactionManager natvTxMgr;

    private final UOWCoordinator uowCoord;

    /**
     * Constructor.
     *
     * @param rrsServices
     */
    public NativeLocalXAResource(NativeTransactionManager natvTxMgr, UOWCoordinator uowCoord, RRSServices rrsServices) {
        this.natvTxMgr = natvTxMgr;
        this.uowCoord = uowCoord;
        this.rrsServices = rrsServices;
    }

    /**
     * {@inheritDoc}
     */
    public void start(Xid xid, int flags) throws XAException {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    public void end(Xid xid, int flags) throws XAException {
        // NO-OP
    }

    /**
     * {@inheritDoc}
     */
    public int prepare(Xid xid) throws XAException {
        throw new XAException(XAException.XAER_PROTO);
    }

    /**
     * {@inheritDoc}
     */
    public void commit(Xid xid, boolean onePhase) throws XAException {

        processOutcome(true);
    }

    /**
     * {@inheritDoc}
     */
    public void rollback(Xid xid) throws XAException {
        processOutcome(false);
    }

    /**
     * Processed resource resolution with the specified outcome.
     *
     * @param commit true to commit, false to rollback.
     * @throws XAException
     */
    private void processOutcome(boolean commit) throws XAException {
        String printableOutcome = (commit) ? "COMMIT" : "ROLLBACK";
        Map<UOWCoordinator, LocalTransactionData> localTxMap = natvTxMgr.getLocalTxMap();

        LocalTransactionData localTxData = localTxMap.get(uowCoord);

        try {
            // We have no knowledge of the unit of work.
            if (localTxData == null) {
                throw new IllegalStateException("Local transaction " + printableOutcome + " processing. No transaction data found for UOWCoordinator: " + uowCoord);
            }

            // Need to lock because base TM code says we may be called on the wrong thread.
            ReadWriteLock rwLock = localTxData.getLock();
            Lock wLock = rwLock.writeLock();
            wLock.lock();

            try {
                List<XAResource> list = localTxData.getXAResourceList();

                // If the list is empty, cleanup and notify this event.
                if (list.isEmpty()) {
                    localTxMap.remove(uowCoord);
                    throw new IllegalStateException("Local transaction outcome processing. No resources found for UOWCoordinator: " + uowCoord);
                }

                // If there is more than one resource. remove it from the list and return.
                if (list.size() > 1) {
                    if (!list.remove(this)) {
                        byte[] ctxToken = localTxData.getContextToken();
                        throw new IllegalStateException("Resource not found. Number or enlisted resources: " + list.size() +
                                                        ". Context token associated with local transaction: " +
                                                        ((ctxToken == null) ? "NULL" : Util.toHexString(ctxToken)));
                    }

                    return;
                }

                // There is a single native local resource left. Remove this transaction
                // from the localTxMap and proceed. At this point there is nothing we can
                // do if there is a failure past this point.
                localTxMap.remove(uowCoord);

                // OK first figure out if there's something to do.  This is all pointless
                // if the UR is clean.
                byte[] contextToken = localTxData.getContextToken();
                RetrieveSideInformationFastReturnType rusfRt = rrsServices.retrieveSideInformationFast(contextToken, 0);
                if ((rusfRt != null) && (rusfRt.getReturnCode() == RRSServices.ATR_OK)) {
                    if (rusfRt.isURStateInReset() == false) {
                        // Figure out if the context is current.
                        int rc = RRSServices.ATR_OK;
                        Context currCtx = natvTxMgr.getContextOnCurrentThread();
                        if ((currCtx != null) && (Arrays.equals(currCtx.getContextToken(), contextToken))) {
                            rc = rrsServices.endUR(commit ? RRSServices.ATR_COMMIT_ACTION : RRSServices.ATR_ROLLBACK_ACTION, null);
                        } else {
                            rc = natvTxMgr.resolveNonCurrentLocalModeUR(uowCoord, commit);
                        }
                        if (rc != RRSServices.ATR_OK) {
                            StringBuilder sb = new StringBuilder();
                            sb.append("Outcome: ");
                            sb.append(printableOutcome);
                            sb.append(". ");
                            sb.append(localTxData.getData());
                            NativeTransactionManager.processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "ATR4END", rc, sb.toString());
                        }
                    }
                } else {
                    int rc = (rusfRt != null) ? rusfRt.getReturnCode() : -1;
                    NativeTransactionManager.processInvalidServiceRCWithRuntimeExc("INVALID_RRS_SERVICE_RC", "ATR4RUSF", rc, new ByteArray(contextToken).toString());
                }
            } finally {
                wLock.unlock();
            }
        } catch (Throwable t) {
            XAException xae = new XAException(XAException.XAER_RMERR);
            xae.initCause(t);
            throw xae;
        }
    }

    /**
     * {@inheritDoc}
     */
    public void forget(Xid xid) throws XAException {
        throw new XAException(XAException.XAER_PROTO);
    }

    /**
     * {@inheritDoc}
     */
    public Xid[] recover(int flag) throws XAException {
        throw new XAException(XAException.XAER_PROTO);
    }

    /**
     * {@inheritDoc}
     */
    public int getTransactionTimeout() throws XAException {
        throw new XAException(XAException.XAER_PROTO);
    }

    /**
     * {@inheritDoc}
     */
    public boolean setTransactionTimeout(int seconds) throws XAException {
        throw new XAException(XAException.XAER_PROTO);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isSameRM(XAResource theXAResource) throws XAException {
        throw new XAException(XAException.XAER_PROTO);
    }

    /**
     * {@inheritDoc}
     */
    public String getResourceName() {
        return "NativeLocalXAResource";
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
        sb.append(", UOWCoordinator: ");
        sb.append(uowCoord);
        sb.append("]");
        return sb.toString();
    }
}