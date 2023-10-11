/*******************************************************************************
 * Copyright (c) 2019, 2023 IBM Corporation and others.
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
package com.ibm.ws.wsat.common.impl;

import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.ibm.tx.remote.Vote;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.wsat.service.WSATContext;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.tm.impl.TranManagerImpl;

/**
 * Encapsulates the WS-AT nature of a transaction. This has its own ID which is
 * flowed as part of the CoordinationContext SOAP header and as part of the
 * EndpointReferences for WS-AT protocol services.
 */
public class WSATTransaction {

    private static final TraceComponent TC = Tr.register(WSATTransaction.class);

    private static final TranManagerImpl tranService = TranManagerImpl.getInstance();

    private final String globalId; // Our global wsat transaction id

    private WSATCoordinator registration;
    private WSATCoordinator coordinator;
    private final Boolean coordinatorLock = Boolean.valueOf(false);
    private final boolean recoveryTran; // true if this is being used for recovery

    private final long expiryTime; // time remaining before timeout

    /*
     * Static methods manage the global HashMap to locate existing
     * instances of the WSATTransaction classes based on the global
     * transaction ids.
     */

    public static void putTran(WSATTransaction tran) {
        tranService.getRemoteTranMgr().putResource(tran.getGlobalId(), tran);
    }

    public static WSATTransaction getTran(String globalId) {
        return (WSATTransaction) tranService.getRemoteTranMgr().getResource(globalId);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof WSATTransaction && globalId.equals(((WSATTransaction) o).getGlobalId());
    }

    public static WSATCoordinatorTran getCoordTran(String globalId) {
        WSATTransaction wsatTran = getTran(globalId);
        if (wsatTran != null && !(wsatTran instanceof WSATCoordinatorTran)) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Transaction is not a coordinator");
            }
            wsatTran = null;
        }
        return (WSATCoordinatorTran) wsatTran;
    }

    /*
     * Constructor provides the global id and the transaction expiry time.
     * Timeout is the time remaining (in milliseconds) before the transaction expires.
     */
    public WSATTransaction(String id, long timeout) {
        this(id, timeout, false);
    }

    public WSATTransaction(String id, long timeout, boolean recovery) {
        globalId = id;
        expiryTime = timeout;
        recoveryTran = recovery;
    }

    @Trivial
    public String getGlobalId() {
        return globalId;
    }

    @Trivial
    protected boolean isRecovery() {
        return recoveryTran;
    }

    /*
     * Returns a timeout (in milliseconds) of the time remaining
     * before this transaction expires.
     */
    @Trivial
    public long getTimeout() {
        return expiryTime;
    }

    /*
     * Registration endpoint is the EPR for the service to be used by a
     * remote system to register as a participant in the transaction.
     */
    @Trivial
    public WSATCoordinator setRegistration(EndpointReferenceType epr) {
        return registration = new WSATCoordinator(globalId, epr);
    }

    @Trivial
    public WSATCoordinator getRegistration() {
        return registration;
    }

    /*
     * Coordinator endpoint is the EPR for the service used by participants
     * to return responses to the 2PC protocol calls.
     */
    @Trivial
    public WSATCoordinator setCoordinator(EndpointReferenceType epr) {
        synchronized (coordinatorLock) {
            return coordinator = new WSATCoordinator(globalId, epr);
        }
    }

    /*
     * Reset coordinator during recovery processing
     */
    @Trivial
    public WSATCoordinator setCoordinator(WSATCoordinator coord) {
        synchronized (coordinatorLock) {
            return coordinator = coord;
        }
    }

    @Trivial
    public WSATCoordinator getCoordinator() {
        synchronized (coordinatorLock) {
            return coordinator;
        }
    }

    /*
     * The WSATContext is the internal representation of the information needed
     * to build a WS-Coord CoordinationContext for the distributed transaction.
     */

    public WSATContext getContext() {
        WSATContext ctx = null;
        if (!isRecovery()) {
            ctx = new WSATContext(getGlobalId(), getRegistration(), getTimeout());
        } else {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Transaction is in recovery mode, returning null CoordinationContext");
            }
        }
        return ctx;
    }

    // For debug
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + globalId;
    }

    // Protocol message handlers are here so they can be synchronized

    /**
     * @throws WSATException
     *
     */
    public synchronized void rollback() throws WSATException {
        tranService.rollbackTransaction(globalId);
    }

    /**
     * @return
     * @throws WSATException
     */
    public synchronized Vote prepare() throws WSATException {
        return tranService.prepareTransaction(globalId);
    }

    /**
     * @throws WSATException
     *
     */
    public synchronized void commit() throws WSATException {
        tranService.commitTransaction(globalId);
    }
}
