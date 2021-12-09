/*******************************************************************************
 * Copyright (c) 2019,2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.wsat.common.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;

/**
 * A subclass of WSATTransaction that represents the coordinator. In addition
 * to the superclass WSATransaction details this also maintains a set of
 * known participants in the global transaction.
 */
public class WSATCoordinatorTran extends WSATTransaction {

    private static final String CLASS_NAME = WSATCoordinatorTran.class.getName();
    private static final TraceComponent TC = Tr.register(WSATCoordinatorTran.class);

    private final Map<String, WSATParticipant> participants = new HashMap<String, WSATParticipant>();
    private final AtomicInteger participantIds = new AtomicInteger();

    public WSATCoordinatorTran(String id, long timeout) {
        super(id, timeout);
    }

    public WSATCoordinatorTran(String id, long timeout, boolean recovery) {
        super(id, timeout, recovery);
    }

    /*
     * Each participant added to the transaction is given its own unique id. This
     * is unique within the context of this global transaction.
     */
    public synchronized WSATParticipant addParticipant(EndpointReferenceType epr) {
        String partId = Integer.toString(participantIds.incrementAndGet());
        WSATParticipant participant = new WSATParticipant(getGlobalId(), partId, epr);
        participants.put(partId, participant);

        // Build the coordinator EPR required by this participant.  This must include the
        // participant ID, so when the participant returns a response to use we can easily
        // identify the sender.
        EndpointReferenceType coordEpr = getCoordinator().getEndpointReference(partId);
        participant.setCoordinator(new WSATCoordinator(getGlobalId(), coordEpr));

        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Added new participant: {0}", participant);
        }
        return participant;
    }

    /*
     * Add back a participant during recovery processing.
     */
    public synchronized WSATParticipant addParticipant(WSATParticipant participant) {
        EndpointReferenceType coordEpr = getCoordinator().getEndpointReference(participant.getId());
        participant.setCoordinator(new WSATCoordinator(getGlobalId(), coordEpr));

        participants.put(participant.getId(), participant);
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "Restored recovered participant: {0}", participant);
        }
        return participant;
    }

    public synchronized void removeParticipant(String partId) {
        participants.remove(partId);

        // If this is a recovery transaction we will not be able to use the TranSyncRegistry
        // to detect transaction end, so we need to use the fact that we have no participants
        // left to trigger clean-up.  This is valid for recovery state only.
        if (isRecovery() && participants.size() == 0) {
            afterCompletion(0);
        }
    }

    public synchronized WSATParticipant getParticipant(String partId) {
        return participants.get(partId);
    }
}
