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
package com.ibm.ws.wsat.service.impl;

import javax.xml.bind.JAXBElement;

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.apache.cxf.ws.addressing.ReferenceParametersType;
import org.apache.cxf.wsdl.EndpointReferenceUtils;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.wsat.common.impl.WSATCoordinator;
import com.ibm.ws.wsat.common.impl.WSATCoordinatorTran;
import com.ibm.ws.wsat.common.impl.WSATParticipant;
import com.ibm.ws.wsat.common.impl.WSATTransaction;
import com.ibm.ws.wsat.service.WSATContext;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.service.WSATFault;
import com.ibm.ws.wsat.service.WSATFaultException;
import com.ibm.ws.wsat.service.WebClient;
import com.ibm.ws.wsat.tm.impl.TranManagerImpl;

/**
 * Implementation of the WS-Coor protocol Activation and Register web services
 */
public class RegistrationImpl {

    private static final String CLASS_NAME = RegistrationImpl.class.getName();
    private static final TraceComponent TC = Tr.register(RegistrationImpl.class);

    private static final RegistrationImpl INSTANCE = new RegistrationImpl();

    private final TranManagerImpl tranService = TranManagerImpl.getInstance();
    private final ProtocolImpl protocolService = ProtocolImpl.getInstance();

    public static RegistrationImpl getInstance() {
        return INSTANCE;
    }

    private EndpointReferenceType registrationEndpoint;

    /*
     * Store our registration EPR
     */
    public synchronized void setRegistrationEndpoint(EndpointReferenceType epr) {
        registrationEndpoint = epr;
        notifyAll();
    }

    /*
     * Return a copy of the EPR with a ReferenceParameter for a WSAT tran
     * global id added.
     */
    public synchronized EndpointReferenceType getRegistrationEndpoint(String global) throws WSATException {
        while (registrationEndpoint == null) {
            try {
                wait(30000);
                if (registrationEndpoint == null) {
                    throw new WSATException(Tr.formatMessage(TC, "NO_SERVICE_ENDPOINT_CWLIB0209"));
                }
            } catch (InterruptedException e) {
            }
        }

        EndpointReferenceType epr = EndpointReferenceUtils.duplicate(registrationEndpoint);
        ReferenceParametersType refs = new ReferenceParametersType();

        refs.getAny().add(new JAXBElement<String>(Constants.WS_WSAT_CTX_REF, String.class, global));
        epr.setReferenceParameters(refs);

        return epr;
    }

    /*
     * activate - called to create a new global transaction in the coordinator role
     * 
     * Note: this function is used internally by the implementation - we do not expose
     * the WS-Coor Activation service.
     */
    public WSATContext activate(String globalId, long timeout, boolean recovery) throws WSATException {
        if (timeout < 0) {
            throw new WSATException(Tr.formatMessage(TC, "WSAT_TRAN_EXPIRED_CWLIB0203"));
        }

        // Generate a new WSATTran control with the global transaction id
        WSATCoordinatorTran wsatTran = new WSATCoordinatorTran(globalId, timeout, recovery);

        // Set ourselves as the registration service and note the protocol
        // coordinator to return when others register with us.
        wsatTran.setRegistration(getRegistrationEndpoint(wsatTran.getGlobalId()));
        wsatTran.setCoordinator(protocolService.getCoordinatorEndpoint(wsatTran.getGlobalId()));

        // Once activated we can add the reference to the control map
        WSATTransaction.putTran(wsatTran);

        // Build the context object to return
        return wsatTran.getContext();
    }

    /*
     * activate - called to create a global transaction in the participant role
     * 
     * Note: this function is used internally by the implementation - we do not expose
     * the WS-Coor Activation service.
     */
    public WSATContext activate(String globalId, EndpointReferenceType registration, long timeout, boolean recovery) throws WSATException {
        if (timeout < 0) {
            throw new WSATException(Tr.formatMessage(TC, "WSAT_TRAN_EXPIRED_CWLIB0203"));
        }

        // Generate the global tran based on supplied information
        WSATTransaction wsatTran = new WSATTransaction(globalId, timeout, recovery);

        // Registration service is supplied by the context
        wsatTran.setRegistration(registration);

        // Once activated we can add the reference to the control map
        WSATTransaction.putTran(wsatTran);

        // Build the context object to return
        return wsatTran.getContext();
    }

    /*
     * register - called to register a new participant as part of an existing
     * global transaction.
     */
    // TODO: Should we return WSATFaultExceptions from here and make the web-service 
    //       layer return the fault response, or should we return the fault from here?
    public EndpointReferenceType register(String globalId, EndpointReferenceType partEpr) throws WSATException {
        // Get the transaction - this should exist.  We should always be registering
        // into an existing active transaction.
        WSATCoordinatorTran wsatTran = WSATTransaction.getCoordTran(globalId);
        if (wsatTran == null) {
            // Not a known transaction.  This should result in the WS-Coor spec 
            // CannotRegisterParticipant fault being returned.
            throw new WSATFaultException(WSATFault.getCannotRegisterParticipant(Tr.formatMessage(TC, "NO_WSAT_TRAN_CWLIB0201", globalId)));
        }

        // Add the new participant and enlist with the transaction manager so it 
        // will take part in 2PC transaction completion.
        WSATParticipant participant = wsatTran.addParticipant(partEpr);
        tranService.registerParticipant(globalId, participant);

        return participant.getCoordinator().getEndpointReference();
    }

    /*
     * registerParticipant - called to invoke registration of a participant back with a
     * coordinator.
     * 
     * Note: this function is used internally by the implementation, it is
     * not part of the registration web service
     */
    public void registerParticipant(String globalId, WSATTransaction wsatTran) throws WSATException {
        // Generate an EPR for our participant protocol service.  This needs to contain
        // the transaction global id (in a ReferenceParameter) so we can identify the 
        // controlling transaction when we later get invoked for prepare/commit/rollback
        EndpointReferenceType participant = protocolService.getParticipantEndpoint(globalId);

        // Register as a participant back with the coordinator passing back our participant
        // EPR as a parameter.  This returns the coordinator that is used to return the 
        // prepared/committed/readonly/aborted protocol flows.  Note we cannot assume the 
        // registration and coordinator EPRs are the same.
        WSATCoordinator wsatCoord = wsatTran.getRegistration();
        WebClient webClient = WebClient.getWebClient(wsatCoord, null);
        EndpointReferenceType coordinator = webClient.register(participant);
        WSATCoordinator remoteCoord = wsatTran.setCoordinator(coordinator);

        // Register the remote coordinator with the transaction manager so it can be used
        // for potential recovery replay. 
        tranService.registerCoordinator(globalId, remoteCoord);
    }
}
