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

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;

/**
 * Represents a remote participant in a WSAT transaction.
 * 
 * Note this class must be serializable as instances of it will be passed to the
 * transaction manager as keys to build XAResrources for completion and recovery.
 */
public class WSATParticipant extends WSATEndpoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final String CLASS_NAME = WSATParticipant.class.getName();
    private static final TraceComponent TC = Tr.register(WSATParticipant.class);

    /*
     * The transaction global id and the participant id are the only serializable
     * state that we maintain.
     */
    private final String globalId;
    private final String participantId;

    private transient WSATParticipantState state = WSATParticipantState.ACTIVE;
    private transient WSATCoordinator coordinator;

    public WSATParticipant(String tranId, String partId, EndpointReferenceType epr) {
        super(epr);
        globalId = tranId;
        participantId = partId;
    }

    @Trivial
    public String getId() {
        return participantId;
    }

    @Trivial
    public String getGlobalId() {
        return globalId;
    }

    @Trivial
    public WSATCoordinator getCoordinator() {
        return coordinator;
    }

    @Trivial
    public void setCoordinator(WSATCoordinator coord) {
        coordinator = coord;
    }

    /*
     * State variable is used to handle blocking waiting for the async response
     * to protocol services, nothing more.
     */

    public synchronized void setState(WSATParticipantState newState) {
        state = newState;
    }

    public synchronized void setResponse(WSATParticipantState newState) {
        state = newState;
        notifyAll();
    }

    public synchronized WSATParticipantState waitResponse(long timeoutMills, WSATParticipantState... responses) {
        List<WSATParticipantState> responseList = Arrays.asList(responses);
        long now = System.nanoTime() / 1000000; // Use time in miiliseconds
        long expiry = now + timeoutMills;
        while (now < expiry && !responseList.contains(state)) {
            try {
                wait(expiry - now);
            } catch (InterruptedException e) {
            }
            now = System.nanoTime() / 1000000;
        }
        return responseList.contains(state) ? state : WSATParticipantState.TIMEOUT;
    }

    /*
     * Remove this participant from the coordinator transaction when complete.
     */
    public void remove() {
        WSATCoordinatorTran tran = WSATTransaction.getCoordTran(globalId);
        if (tran != null) {
            tran.removeParticipant(participantId);
        }
    }

    /*
     * equals method for the transaction manager. Participants are equal if they
     * have the same globalId and participantId
     */
    @Override
    public boolean equals(Object other) {
        boolean result = false;
        if (other != null && other instanceof WSATParticipant) {
            WSATParticipant otherPart = (WSATParticipant) other;
            result = globalId.equals(otherPart.globalId) && participantId.equals(otherPart.participantId);
        }
        return result;
    }

    @Override
    public int hashCode() {
        return (globalId.hashCode() * 31) + participantId.hashCode();
    }

    // For debug
    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + globalId + "/" + participantId + " (" + state + ")";
    }
}
