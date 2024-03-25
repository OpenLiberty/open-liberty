/*******************************************************************************
 * Copyright (c) 2019, 2024 IBM Corporation and others.
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

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.bind.JAXBElement;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.w3c.dom.Node;

import com.ibm.tx.remote.Vote;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.wsat.service.WSATContext;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.service.impl.ProtocolImpl;
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

    private final Map<String, WSATParticipant> participants = new ConcurrentHashMap<String, WSATParticipant>();

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

    public static WSATTransaction getCoordTran(String globalId) {
        return getTran(globalId);
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
    public WSATCoordinator setRegistration(EndpointReferenceType epr) {
        return registration = new WSATCoordinator(globalId, epr);
    }

    public WSATCoordinator getRegistration() {
        return registration;
    }

    /*
     * Coordinator endpoint is the EPR for the service used by participants
     * to return responses to the 2PC protocol calls.
     */
    public WSATCoordinator setCoordinator(EndpointReferenceType epr) {
        synchronized (coordinatorLock) {
            return coordinator = new WSATCoordinator(globalId, epr);
        }
    }

    /*
     * Reset coordinator during recovery processing
     */
    public WSATCoordinator setCoordinator(WSATCoordinator coord) {
        synchronized (coordinatorLock) {
            return coordinator = coord;
        }
    }

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

    /*
     * Return a coordinator EPR for a specific participant. This is the same as the
     * basic coordinator EPR but it has an additional ReferenceParameter containing
     * the participant identifier. Later, when the participant calls us back using
     * this EPR we will easily be able to identify who the caller was.
     */
    public EndpointReferenceType getCoordinatorEPR(EndpointReferenceType bareEpr, String partId) {
        EndpointReferenceType epr = EndpointReferenceUtils.duplicate(bareEpr);
        // duplicate doesn't seem to copy the ReferenceParams?, so add
        // back the originals plus our new participant id.
        ReferenceParametersType refs = new ReferenceParametersType();
        for (Object ref : bareEpr.getReferenceParameters().getAny()) {
            if (TC.isDebugEnabled()) {

                Tr.debug(TC, "Adding this reference parameter: {0}, {1}", ref.getClass().getCanonicalName(), ref);
                if (ref instanceof Node) {
                    Tr.debug(TC, "Local name: {0}", ((Node) ref).getLocalName());
                    if (Constants.WS_WSAT_PART_ID.equals(((Node) ref).getLocalName())) {
                        Tr.debug(TC, "Skipping");
                        continue;
                    }
                }
            }
            refs.getAny().add(ref);
        }
        JAXBElement<String> part = new JAXBElement<String>(Constants.WS_WSAT_PART_REF, String.class, partId);
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Now adding this additional reference parameter: {0}", part);
        }
        refs.getAny().add(part);
        epr.setReferenceParameters(refs);
        return epr;
    }

    /*
     * Each participant added to the transaction is given its own unique id. This
     * is unique within the context of this global transaction.
     */
    public WSATParticipant addParticipant(EndpointReferenceType epr) throws WSATException {
        String partId = UUID.randomUUID().toString();
        WSATParticipant participant = new WSATParticipant(getGlobalId(), partId, epr);
        participants.put(partId, participant);

        // Build the coordinator EPR required by this participant.  This must include the
        // participant ID, so when the participant returns a response to use we can easily
        // identify the sender.
        EndpointReferenceType bareCoordEpr = ProtocolImpl.getInstance().getCoordinatorEndpoint(getGlobalId());
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Bare coordinator EPR:\n{0}", DebugUtils.printEPR(bareCoordEpr));
        }
        EndpointReferenceType coordEpr = getCoordinatorEPR(bareCoordEpr, partId);
        participant.setCoordinator(new WSATCoordinator(getGlobalId(), coordEpr));

        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Added new participant: {0}", participant);
        }
        return participant;
    }

    /*
     * Add back a participant during recovery processing.
     */
    public WSATParticipant addParticipant(WSATParticipant participant) {
        EndpointReferenceType coordEpr = getCoordinator().getEndpointReference(participant.getId());
        participant.setCoordinator(new WSATCoordinator(getGlobalId(), coordEpr));

        participants.put(participant.getId(), participant);
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Restored recovered participant: {0}", participant);
        }
        return participant;
    }

    public void removeParticipant(String partId) {
        participants.remove(partId);
    }

    public WSATParticipant getParticipant(String partId) {
        return participants.get(partId);
    }
}
