/*******************************************************************************
 * Copyright (c) 2019, 2021 IBM Corporation and others.
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

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.wsat.service.WSATContext;
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
        WSATTransaction t2 = (WSATTransaction) tranService.getRemoteTranMgr().getResource(globalId);

        return t2;
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

    public long getTimeout() {
        return expiryTime;
    }

    /*
     * Registration endpoint is the EPR for the service to be used by a
     * remote system to register as a participant in the transaction.
     */

    public WSATCoordinator setRegistration(EndpointReferenceType epr) {
        WSATCoordinator reg = new WSATCoordinator(globalId, epr);
        registration = reg;
        return reg;
    }

    public WSATCoordinator getRegistration() {
        return registration;
    }

    /*
     * Coordinator endpoint is the EPR for the service used by participants
     * to return responses to the 2PC protocol calls.
     */

    public synchronized WSATCoordinator setCoordinator(EndpointReferenceType epr) {
        WSATCoordinator coord = new WSATCoordinator(globalId, epr);
        coordinator = coord;
        return coord;
    }

    /*
     * Reset coordinator during recovery processing
     */
    public synchronized WSATCoordinator setCoordinator(WSATCoordinator coord) {
        coordinator = coord;
        return coord;
    }

    public synchronized WSATCoordinator getCoordinator() {
        return coordinator;
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
}
