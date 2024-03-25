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
package com.ibm.ws.wsat.service.impl;

import javax.xml.bind.JAXBElement;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.EndpointReferenceUtils;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.w3c.dom.Element;

import com.ibm.tx.remote.DistributableTransaction;
import com.ibm.tx.remote.Vote;
import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.ffdc.annotation.FFDCIgnore;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.wsat.common.impl.DebugUtils;
import com.ibm.ws.wsat.common.impl.WSATCoordinator;
import com.ibm.ws.wsat.common.impl.WSATParticipant;
import com.ibm.ws.wsat.common.impl.WSATParticipantState;
import com.ibm.ws.wsat.common.impl.WSATTransaction;
import com.ibm.ws.wsat.service.ProtocolServiceWrapper;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.service.WSATUtil;
import com.ibm.ws.wsat.service.WebClient;
import com.ibm.ws.wsat.tm.impl.ParticipantFactoryService;
import com.ibm.ws.wsat.tm.impl.TranManagerImpl;

/**
 * Implementation of the WS-AT Coordinator and Participant web services.
 */
public class ProtocolImpl {

    private static final TraceComponent TC = Tr.register(ProtocolImpl.class);

    private static final ProtocolImpl INSTANCE = new ProtocolImpl();

    private static final TranManagerImpl tranService = TranManagerImpl.getInstance();

    private EndpointReferenceType coordinatorEndpoint;
    private EndpointReferenceType participantEndpoint;

    private static String recoveryId;

    public static ProtocolImpl getInstance() {
        if (recoveryId == null) {
            recoveryId = tranService.getRecoveryId();
        }
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
        EndpointReferenceType eprCopy = EndpointReferenceUtils.duplicate(epr);
        ReferenceParametersType refs = new ReferenceParametersType();

        refs.getAny().add(new JAXBElement<String>(Constants.WS_WSAT_CTX_REF, String.class, ctxId));
        String recoveryId = tranService.getRecoveryId();
        if (recoveryId != null && !recoveryId.isEmpty()) {
            refs.getAny().add(new JAXBElement<String>(Constants.WS_WSAT_REC_REF, String.class, recoveryId));
        }

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
    public void prepare(ProtocolServiceWrapper wrapper) throws WSATException {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "prepare: recoveryId={0}, incoming={1}", recoveryId, wrapper.getRecoveryID());
        }

        if (recoveryId != null && wrapper.getRecoveryID() != null && !recoveryId.equals(wrapper.getRecoveryID())) {
            rerouteToCorrectParticipant(wrapper, WSATParticipantState.PREPARE);
            return;
        } else {
            final String globalId = wrapper.getTxID();
            final WSATTransaction tran = WSATTransaction.getTran(globalId);
            if (tran != null) {
                try {
                    Vote vote = tran.prepare();
                    WSATParticipantState resp = (vote == Vote.VoteCommit) ? WSATParticipantState.PREPARED : (vote == Vote.VoteReadOnly) ? WSATParticipantState.READONLY : WSATParticipantState.ABORTED;
                    participantResponse(tran, globalId, wrapper.getResponseEpr(), resp);
                } catch (WSATException e) {
                    participantResponse(tran, globalId, wrapper.getResponseEpr(), WSATParticipantState.ROLLBACK);
                }
            }
        }
    }

    private void rerouteToCorrectParticipant(ProtocolServiceWrapper wrapper, WSATParticipantState messageType) throws WSATException {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "REROUTE {0} originally sent to {1}", messageType, wrapper.getWsatProperties().get(Names.WSA_TO_QNAME.getLocalPart()));
        }

        String globalId = wrapper.getTxID();

        // Need to construct an EPR for the participant
        String newAddr = null;
        try {
            newAddr = tranService.getAddress(wrapper.getRecoveryID());
        } catch (Exception e) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Can't get address for {0} {1}", wrapper.getRecoveryID(), e);
            }
        }

        // Reroute address may be unavailable
        if (newAddr == null)
            return;

        String toAddr = WSATUtil.createRedirectAddr(wrapper.getWsatProperties().get(Names.WSA_TO_QNAME.getLocalPart()), newAddr);
        EndpointReferenceType toEpr = WSATUtil.createEpr(toAddr);

        // Copy across necessary reference parameters
        ReferenceParametersType refs = toEpr.getReferenceParameters();
        refs.getAny().add(new JAXBElement<String>(Constants.WS_WSAT_CTX_REF, String.class, globalId));
        refs.getAny().add(new JAXBElement<String>(Constants.WS_WSAT_REC_REF, String.class, wrapper.getRecoveryID()));
        toEpr.setReferenceParameters(refs);

        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "{0} needs to go to\n{1}", messageType, DebugUtils.printEPR(toEpr));
        }

        String partId = null;
        for (Object obj : wrapper.getResponseEpr().getReferenceParameters().getAny()) {
            try {
                Element name = (Element) obj;
                if (Constants.WS_WSAT_PART_REF.getLocalPart().equals(name.getLocalName()) && Constants.WS_WSAT_PART_REF.getNamespaceURI().equals(name.getNamespaceURI())) {
                    partId = name.getFirstChild().getNodeValue();
                }
            } catch (Throwable e) {
            }
        }

        WSATCoordinator coord = new WSATCoordinator(globalId, wrapper.getResponseEpr());
        WSATParticipant part = new WSATParticipant(globalId, partId, toEpr);

        WebClient webClient = WebClient.getWebClient(part, coord);
        webClient.setMisrouting(false);

        switch (messageType) {
            case PREPARE:
                webClient.prepare();
                break;
            case COMMIT:
                webClient.commit();
                break;
            case ROLLBACK:
                webClient.rollback();
                break;
        }
    }

    // COMMIT and ROLLBACK can occur during recovery when the tran manager might know about
    // the real transaction but our WSAT HashMaps might not be rebuilt. To handle this we
    // always go straight to the tran manager to process the request - if this fails we do
    // not try to send any response - we allow retry processing on the coordinator to eventually
    // sort things out.

    public void commit(ProtocolServiceWrapper wrapper) throws WSATException {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "commit: recoveryId={0}, incoming={1}", recoveryId, wrapper.getRecoveryID());
        }

        if (recoveryId != null && wrapper.getRecoveryID() != null && !recoveryId.equals(wrapper.getRecoveryID())) {
            rerouteToCorrectParticipant(wrapper, WSATParticipantState.COMMIT);
            return;
        }

        final String globalId = wrapper.getTxID();
        final WSATTransaction tran = WSATTransaction.getTran(globalId);

        DistributableTransaction t = null;

        if (tran != null) {
            tran.commit();
        } else {
            t = tranService.getRemoteTranMgr().getTransactionForID(globalId);
        }

        if (t != null) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Participant is probably still in replay. Coordinator can retry later: {0}", t);
            }
        } else {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "No sign of this subordinate. Assume it committed");
            }

            participantResponse(tran, globalId, wrapper.getResponseEpr(), WSATParticipantState.COMMITTED);
        }
    }

    @FFDCIgnore(WSATException.class)
    public void rollback(ProtocolServiceWrapper wrapper) throws WSATException {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "rollback: recoveryId={0}, incoming={1}", recoveryId, wrapper.getRecoveryID());
        }

        if (recoveryId != null && wrapper.getRecoveryID() != null && !recoveryId.equals(wrapper.getRecoveryID())) {
            rerouteToCorrectParticipant(wrapper, WSATParticipantState.ROLLBACK);
            return;
        }

        final String globalId = wrapper.getTxID();
        final WSATTransaction tran = WSATTransaction.getTran(globalId);

        if (tran != null) {
            try {
                tran.rollback();
            } catch (WSATException e) {
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "Transaction is probably gone already: {0}", e);
                }
            }
        }

        participantResponse(tran, globalId, wrapper.getResponseEpr(), WSATParticipantState.ABORTED);
    }

    private void coordinatorResponse(ProtocolServiceWrapper wrapper, WSATParticipantState response) throws WSATException {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "From EPR address: {0}", wrapper.getResponseEpr().getAddress().getValue());
            Tr.debug(TC, "Coordinator Endpoint: {0}", coordinatorEndpoint.getAddress().getValue());
            Tr.debug(TC, "From EPR address: {0}", participantEndpoint.getAddress().getValue());
        }

        WSATParticipant part = new WSATParticipant(wrapper.getTxID(), wrapper.getPartID(), wrapper.getResponseEpr());
        WSATCoordinator coord = new WSATCoordinator(wrapper.getTxID(), coordinatorEndpoint);
        coord.setParticipant(part);
        part.setCoordinator(coord);

        WebClient client = WebClient.getWebClient(part, coord);
        client.rollback();
    }

    private void participantResponse(WSATTransaction tran, String globalId, EndpointReferenceType fromEpr, WSATParticipantState response) throws WSATException {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "EPR:\n{0}", DebugUtils.printEPR(fromEpr));
        }
        // Send the response to our known coordinator, if we have one.  Otherwise fall back to
        // using the sender's EPR (see WS-AT specification section 8).
        WSATCoordinator coord = null;

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

    /*
     * Coordinator services. These services are invoked by the participant to
     * returns its response to a previous 2PC protocol request. The caller
     * thread will be blocked waiting for the response to occur.
     */

    public void prepared(ProtocolServiceWrapper wrapper) throws WSATException {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "prepared: recoveryId={0}, incoming={1}", recoveryId, wrapper.getRecoveryID());
        }

        if (recoveryId != null && wrapper.getRecoveryID() != null && !recoveryId.equals(wrapper.getRecoveryID())) {
            rerouteToCorrectCoordinator(wrapper, WSATParticipantState.PREPARED);
        } else {
            WSATParticipant participant = findParticipant(wrapper.getTxID(), wrapper.getPartID());
            if (participant != null) {
                participant.setResponse(WSATParticipantState.PREPARED);
            } else {
                // During participant recovery we might receive an unexpected 'prepared' if the participant
                // wants a re-send of the final commit/rollback state.
                if (TC.isDebugEnabled()) {
                    Tr.debug(TC, "Unsolicited PREPARED received: {0}/{1}/{2}. Replaying completion", wrapper.getTxID(), wrapper.getPartID(),
                             wrapper.getResponseEpr().getAddress().getValue());
                }
                ParticipantFactoryService.putRecoveryAddress(wrapper.getTxID(), wrapper.getPartID(), wrapper.getResponseEpr());
                if (!tranService.replayCompletion(wrapper.getTxID())) {
                    // Couldn't find the tran. Probably never got logged. Send a rollback
                    if (TC.isDebugEnabled()) {
                        Tr.debug(TC, "Couldn't find tran. Need to send rollback");
                        coordinatorResponse(wrapper, WSATParticipantState.ROLLBACK);
                    }
                }
            }
        }
    }

    public void readOnly(ProtocolServiceWrapper wrapper) throws WSATException {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "readOnly: recoveryId={0}, incoming={1}", recoveryId, wrapper.getRecoveryID());
        }

        if (recoveryId != null && wrapper.getRecoveryID() != null && !recoveryId.equals(wrapper.getRecoveryID())) {
            rerouteToCorrectCoordinator(wrapper, WSATParticipantState.READONLY);
        } else {
            WSATParticipant participant = findParticipant(wrapper.getTxID(), wrapper.getPartID());
            if (participant != null) {
                participant.setResponse(WSATParticipantState.READONLY);
            }
        }
    }

    /**
     * @param readonly
     * @throws WSATException
     */
    private void rerouteToCorrectCoordinator(ProtocolServiceWrapper wrapper, WSATParticipantState messageType) throws WSATException {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "REROUTE {0} originally sent to {1}", messageType, wrapper.getWsatProperties().get(Names.WSA_TO_QNAME.getLocalPart()));
        }

        String globalId = wrapper.getTxID();

        // Need to construct an EPR for the coordinator
        String newAddr = null;
        try {
            newAddr = tranService.getAddress(wrapper.getRecoveryID());
        } catch (Exception e) {
            if (TC.isDebugEnabled()) {
                Tr.debug(TC, "Can't get address for {0} {1}", wrapper.getRecoveryID(), e);
            }
        }

        // Reroute address may be unavailable
        if (newAddr == null)
            return;

        String toAddr = WSATUtil.createRedirectAddr(wrapper.getWsatProperties().get(Names.WSA_TO_QNAME.getLocalPart()), newAddr);
        EndpointReferenceType toEpr = WSATUtil.createEpr(toAddr);

        // Copy across necessary reference parameters
        ReferenceParametersType refs = toEpr.getReferenceParameters();
        refs.getAny().add(new JAXBElement<String>(Constants.WS_WSAT_CTX_REF, String.class, globalId));
        refs.getAny().add(new JAXBElement<String>(Constants.WS_WSAT_PART_REF, String.class, wrapper.getPartID()));
        refs.getAny().add(new JAXBElement<String>(Constants.WS_WSAT_REC_REF, String.class, wrapper.getRecoveryID()));

        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "REROUTE {0} needs to go to\n{1}", messageType, DebugUtils.printEPR(toEpr));
        }

        WSATCoordinator coord = new WSATCoordinator(globalId, toEpr);

        WebClient webClient = WebClient.getWebClient(coord, null);
        webClient.setMisrouting(false);

        switch (messageType) {
            case PREPARED:
                webClient.prepared();
                break;
            case COMMITTED:
                webClient.committed();
                break;
            case READONLY:
                webClient.readOnly();
                break;
            default:
                webClient.aborted();
        }
    }

    public void aborted(ProtocolServiceWrapper wrapper) throws WSATException {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "aborted: recoveryId={0}, incoming={1}", recoveryId, wrapper.getRecoveryID());
        }

        if (recoveryId != null && wrapper.getRecoveryID() != null && !recoveryId.equals(wrapper.getRecoveryID())) {
            rerouteToCorrectCoordinator(wrapper, WSATParticipantState.ABORTED);
        } else {
            WSATParticipant participant = findParticipant(wrapper.getTxID(), wrapper.getPartID());
            if (participant != null) {
                participant.setResponse(WSATParticipantState.ABORTED);
            }
        }
    }

    public void committed(ProtocolServiceWrapper wrapper) throws WSATException {
        if (TC.isDebugEnabled()) {
            Tr.debug(TC, "committed: recoveryId={0}, incoming={1}", recoveryId, wrapper.getRecoveryID());
        }

        if (recoveryId != null && wrapper.getRecoveryID() != null && !recoveryId.equals(wrapper.getRecoveryID())) {
            rerouteToCorrectCoordinator(wrapper, WSATParticipantState.COMMITTED);
        } else {
            WSATParticipant participant = findParticipant(wrapper.getTxID(), wrapper.getPartID());
            if (participant != null) {
                participant.setResponse(WSATParticipantState.COMMITTED);
            }
        }
    }

    // Find the WSATParticipant for the response
    private WSATParticipant findParticipant(String globalId, String partId) {
        WSATParticipant participant = null;
        WSATTransaction wsatTran = WSATTransaction.getCoordTran(globalId);
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
