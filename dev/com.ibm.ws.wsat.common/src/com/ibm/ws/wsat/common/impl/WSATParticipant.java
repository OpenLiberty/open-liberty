/*******************************************************************************
 * Copyright (c) 2019, 2025 IBM Corporation and others.
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

import java.io.Serializable;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import org.apache.cxf.ws.addressing.EndpointReferenceType;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.websphere.ras.annotation.Trivial;
import com.ibm.ws.jaxws.wsat.Constants;

/**
 * Represents a remote participant in a WSAT transaction.
 *
 * Note this class must be serializable as instances of it will be passed to the
 * transaction manager as keys to build XAResrources for completion and recovery.
 */
public class WSATParticipant extends WSATEndpoint implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final TraceComponent TC = Tr.register(WSATParticipant.class, Constants.TRACE_GROUP);

    /*
     * The transaction global id and the participant id are the only serializable
     * state that we maintain.
     */
    private final String globalId;
    private final String participantId;

    private transient WSATParticipantState state = WSATParticipantState.ACTIVE;
    private transient WSATCoordinator coordinator;

    @Trivial
    public WSATParticipant(String tranId, String partId, EndpointReferenceType epr) {
        super(epr);
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "WSATParticipant:\nglobalId:\n{0}\nparticipantId:\n{1}\nEPR:\n{2}", tranId, partId, DebugUtils.printEPR(epr));
        }
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

    @Trivial
    public synchronized void setResponse(WSATParticipantState newState) {

        // If we're aborted, we're aborted
        if (state != WSATParticipantState.ABORTED) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Set response from " + state + " to " + newState);
            }
            state = newState;
        } else {
            if (TC.isDebugEnabled()) {
                if (newState != WSATParticipantState.ABORTED) {
                    Tr.debug(TC, "Not overriding " + WSATParticipantState.ABORTED + " with " + newState);
                }
            }
        }

        notifyAll();
    }

    @Trivial
    public synchronized WSATParticipantState waitResponse(long timeoutMills, WSATParticipantState... responses) {
        if (TC.isEntryEnabled()) {
            Tr.entry(TC, "waitResponse", this, timeoutMills, responses);
        }

        final List<WSATParticipantState> responseList = Arrays.asList(responses);

        // Wait forever if timeout <= 0
        if (timeoutMills <= 0) {
            while (!responseList.contains(state)) {
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "Waiting 1 second for state [" + state + "] to be one of " + Arrays.toString(responses));
                }
                try {
                    wait(1000);
                } catch (InterruptedException e) {
                }
            }
        } else {
            final Instant expiry = Instant.now().plusMillis(timeoutMills);
            while (Instant.now().compareTo(expiry) < 0 && !responseList.contains(state)) {
                final long waitTime = expiry.minusMillis(Instant.now().toEpochMilli()).toEpochMilli();
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "Waiting " + waitTime + " milliseconds for state [" + state + "] to be one of " + Arrays.toString(responses));
                }
                try {
                    if (waitTime > 0) {
                        wait(waitTime);
                    } else {
                        break;
                    }
                } catch (InterruptedException e) {
                }
            }
        }

        final WSATParticipantState ret = responseList.contains(state) ? state : WSATParticipantState.TIMEOUT;
        if (TC.isEntryEnabled()) {
            Tr.exit(TC, "waitResponse", ret);
        }
        return ret;
    }

    /*
     * Remove this participant from the coordinator transaction when complete.
     */
    public void remove() {
        WSATTransaction tran = WSATTransaction.getCoordTran(globalId);
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
        if (other instanceof WSATParticipant) {
            final WSATParticipant otherPart = (WSATParticipant) other;
            return globalId.equals(otherPart.globalId) && participantId.equals(otherPart.participantId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (globalId.hashCode() * 31) + participantId.hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append(": ").append(globalId).append("/").append(participantId).append(" (").append(state).append(")");
        if (globalId != null) {
            sb.append(" ").append(Integer.toHexString(this.hashCode()));
        }
        return sb.toString();
    }
}
