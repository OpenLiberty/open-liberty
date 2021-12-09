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
package com.ibm.ws.wsat.service.impl;

import javax.xml.bind.JAXBElement;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.ReferenceParametersType;

import com.ibm.tx.remote.Vote;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.wsat.common.impl.WSATCoordinator;
import com.ibm.ws.wsat.common.impl.WSATCoordinatorTran;
import com.ibm.ws.wsat.common.impl.WSATParticipant;
import com.ibm.ws.wsat.common.impl.WSATParticipantState;
import com.ibm.ws.wsat.common.impl.WSATTransaction;
import com.ibm.ws.wsat.cxf.utils.WSATCXFUtils;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.service.WebClient;
import com.ibm.ws.wsat.tm.impl.TranManagerImpl;

/**
 * Implementation of the WS-AT Coordinator and Participant web services.
 */
public class ProtocolImpl {

    private static final String CLASS_NAME = ProtocolImpl.class.getName();
    private static final TraceComponent TC = Tr.register(ProtocolImpl.class);

    private static final ProtocolImpl INSTANCE = new ProtocolImpl();

    private final TranManagerImpl tranService = TranManagerImpl.getInstance();

    private EndpointReferenceType coordinatorEndpoint;
    private EndpointReferenceType participantEndpoint;

    public static ProtocolImpl getInstance() {
        return INSTANCE;
    }

    /*
     * Store our protocol EPRs. These are the basic host/port/context
     * details for the coordinator and participant endpoints this Liberty
     * instance is hosting.
     */
    public synchronized void setCoordinatorEndpoint(EndpointReferenceType epr) {
        coordinatorEndpoint = epr;
        notifyAll();
    }

    public synchronized void setParticipantEndpoint(EndpointReferenceType epr) {
        participantEndpoint = epr;
        notifyAll();
    }

    /*
     * Return a copy of the EPRs with a ReferenceParameter added. These are
     * the versions of the EPRs that we send to partner systems during the
     * registration flows. They consist of the basic EPRs (as set above) with
     * the active transaction global id added as a ReferenceParameter. This
     * means when we receive calls back on these EPRs we can easily identify
     * the associated global transaction.
     */
    public synchronized EndpointReferenceType getCoordinatorEndpoint(String globalId) throws WSATException {
        while (coordinatorEndpoint == null) {
            try {
                wait(30000);
                if (coordinatorEndpoint == null) {
                    throw new WSATException(Tr.formatMessage(TC, "NO_SERVICE_ENDPOINT_CWLIB0209"));
                }
            } catch (InterruptedException e) {
            }
        }
        return getEndpoint(coordinatorEndpoint, globalId);
    }

    public synchronized EndpointReferenceType getParticipantEndpoint(String globalId) throws WSATException {
        while (participantEndpoint == null) {
            try {
                wait(30000);
                if (participantEndpoint == null) {
                    throw new WSATException(Tr.formatMessage(TC, "NO_SERVICE_ENDPOINT_CWLIB0209"));
                }
            } catch (InterruptedException e) {
            }
        }
        return getEndpoint(participantEndpoint, globalId);
    }

    private EndpointReferenceType getEndpoint(EndpointReferenceType epr, String ctxId) {
        EndpointReferenceType eprCopy = WSATCXFUtils.duplicate(epr);
        ReferenceParametersType refs = new ReferenceParametersType();

        refs.getAny().add(new JAXBElement<String>(Constants.WS_WSAT_CTX_REF, String.class, ctxId));
        eprCopy.setReferenceParameters(refs);

        return eprCopy;
    }

    /*
     * Participant services. These services are invoked by the coordinator
     * during 2PC. We need to invoke the appropriate method on our local
     * transaction (via the transaction manager) and return a response to
     * the coordinator. Note that these are one-way web-services, so we
     * return a response by making a separate call back to the coordinator.
     */

    // We never get a PREPARE in recovery mode, therefore we should either know about the
    // WSAT transaction, or it must have already ended (this can happen if a rollback gets
    // sent before we've finished our own prepare processing).  In all cases we must send
    // a response - we send ABORTED if we no longer know about the tran, or get other
    // unexpected errors.

    @FFDCIgnore(WSATException.class)
    public void prepare(String globalId, EndpointReferenceType fromEpr) throws WSATException {
        try {
            Vote vote = tranService.prepareTransaction(globalId);
            WSATParticipantState resp = (vote == Vote.VoteCommit) ? WSATParticipantState.PREPARED : (vote == Vote.VoteReadOnly) ? WSATParticipantState.READONLY : WSATParticipantState.ABORTED;
            participantResponse(globalId, fromEpr, resp);
        } catch (WSATException e) {
            participantResponse(globalId, fromEpr, WSATParticipantState.ROLLBACK);
        }
    }

    // COMMIT and ROLLBACK can occur during recovery when the tran manager might know about
    // the real transaction but our WSAT HashMaps might not be rebuilt. To handle this we
    // always go straight to the tran manager to process the request - if this fails we do
    // not try to send any response - we allow retry processing on the coordinator to eventually
    // sort things out.

    @FFDCIgnore(WSATException.class)
    public void commit(String globalId, EndpointReferenceType fromEpr) {
        try {
            tranService.commitTransaction(globalId);
            participantResponse(globalId, fromEpr, WSATParticipantState.COMMITTED);
        } catch (WSATException e) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Unable to complete commit: {0}", e);
            }
        }
    }

    @FFDCIgnore(WSATException.class)
    public void rollback(String globalId, EndpointReferenceType fromEpr) {
        try {
            tranService.rollbackTransaction(globalId);
        } catch (WSATException e) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Unable to complete rollback: {0}", e);
            }
        }
        try {
            participantResponse(globalId, fromEpr, WSATParticipantState.ABORTED);
        } catch (WSATException e) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Unable to send rollback response: {0}", e);
            }
        }
    }

    private void participantResponse(String globalId, EndpointReferenceType fromEpr, WSATParticipantState response) throws WSATException {
        // Send the response to our known coordinator, if we have one.  Otherwise fall back to
        // using the sender's EPR (see WS-AT spec section 8).
        WSATCoordinator coord = null;
        WSATTransaction tran = findTransaction(globalId);
        if (tran != null) {
            coord = tran.getCoordinator();
        } else if (fromEpr != null) {
            coord = new WSATCoordinator(globalId, fromEpr);
        }
        if (coord != null) {
            WebClient client = WebClient.getWebClient(coord, coord.getParticipant());
            if (response == WSATParticipantState.PREPARED) {
                client.prepared();
            } else if (response == WSATParticipantState.COMMITTED) {
                client.committed();
            } else if (response == WSATParticipantState.READONLY) {
                client.readOnly();
            } else {
                client.aborted();
            }
        } else {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Unable to find response coordinator");
            }
        }
    }

    private WSATTransaction findTransaction(String globalId) {
        WSATTransaction tran = WSATTransaction.getTran(globalId);
        if (tran == null) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Unable to find transaction: {0}", globalId);
            }
        }
        return tran;
    }

    /*
     * Coordinator services. These services are invoked by the participant to
     * returns its response to a previous 2PC protocol request. The caller
     * thread will be blocked waiting for the response to occur.
     */

    public void prepared(String globalId, String partId, EndpointReferenceType fromEpr) throws WSATException {
        WSATParticipant participant = findParticipant(globalId, partId);
        if (participant != null) {
            participant.setResponse(WSATParticipantState.PREPARED);
        } else {
            // During participant recovery we might receive an unexpected 'prepared' if the participant
            // wants a re-send of the final commit/rollback state.
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Unsolicited PREPARED received: {0}/{1}. Replaying completion", globalId, partId);
            }
            tranService.replayCompletion(globalId);
        }
    }

    public void readOnly(String globalId, String partId) throws WSATException {
        WSATParticipant participant = findParticipant(globalId, partId);
        if (participant != null) {
            participant.setResponse(WSATParticipantState.READONLY);
        }
    }

    public void aborted(String globalId, String partId) throws WSATException {
        WSATParticipant participant = findParticipant(globalId, partId);
        if (participant != null) {
            participant.setResponse(WSATParticipantState.ABORTED);
        }
    }

    public void committed(String globalId, String partId) throws WSATException {
        WSATParticipant participant = findParticipant(globalId, partId);
        if (participant != null) {
            participant.setResponse(WSATParticipantState.COMMITTED);
        }
    }

    // Find the WSATParticipant for the response
    private WSATParticipant findParticipant(String globalId, String partId) {
        WSATParticipant participant = null;
        WSATCoordinatorTran wsatTran = WSATTransaction.getCoordTran(globalId);
        if (wsatTran != null) {
            participant = wsatTran.getParticipant(partId);
            if (participant == null) {
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "Unable to find participant for transaction: {0}/{1}", globalId, partId);
                }
            }
        } else {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Unable to find transaction: {0}", globalId);
            }
        }
        return participant;
    }
}
