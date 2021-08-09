/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.common.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.transaction.Synchronization;

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
public class WSATTransaction implements Synchronization {

    private static final String CLASS_NAME = WSATTransaction.class.getName();
    private static final TraceComponent TC = Tr.register(WSATTransaction.class);

    private static Map<String, WSATTransaction> tranMap =
                    Collections.synchronizedMap(new HashMap<String, WSATTransaction>());

    private final TranManagerImpl tranService = TranManagerImpl.getInstance();

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
        tranMap.put(tran.getGlobalId(), tran);
    }

    public static WSATTransaction getTran(String globalId) {
        return tranMap.get(globalId);
    }

    public static void removeTran(String globalId) {
        tranMap.remove(globalId);
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

        // We cannot registry with TranSync when doing recovery
        if (!recoveryTran) {
            tranService.registerTranSync(this);
        }
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
     * Remove coordinator when done
     */
    public synchronized void removeCoordinator() {
        // If this is a recovery transaction we will not be able to use the TranSyncRegistry
        // to detect transaction end, so we need to use the fact that we have removed the 
        // coordinator to trigger clean-up.  This is valid for recovery state only.
        if (isRecovery()) {
            afterCompletion(0);
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

    /*
     * Transaction synchronization is used to detect the end of the transaction
     * so we can tidy up the internal tables.
     */

    @Override
    public void beforeCompletion() {
        // Nothing to do
    }

    @Override
    public void afterCompletion(int status) {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Removing global transaction: {0}", this);
        }
        WSATTransaction.removeTran(getGlobalId());
    }

    // For debug
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + globalId;
    }
}
