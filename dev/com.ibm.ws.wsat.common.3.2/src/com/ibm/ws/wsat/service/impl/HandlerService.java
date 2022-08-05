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

import org.apache.cxf.ws.addressing.EndpointReferenceType;
import org.osgi.service.component.annotations.Component;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.wsat.service.Handler;
import com.ibm.ws.wsat.service.WSATContext;
import com.ibm.ws.wsat.service.WSATException;

/**
 * OSGI service implementation of the WSAT interceptor Handler interface
 */
@Component(property = { "service.vendor=IBM" })
public class HandlerService implements Handler {

    private static final String CLASS_NAME = HandlerService.class.getName();
    private static final TraceComponent TC = Tr.register(HandlerService.class);

    private final HandlerImpl handlerService = HandlerImpl.getInstance();
    private final RegistrationImpl registrationService = RegistrationImpl.getInstance();
    private final ProtocolImpl protocolService = ProtocolImpl.getInstance();

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsat.service.Handler#setRegistrationEndpoint(org.apache.cxf.ws.addressing.EndpointReferenceType)
     */
    @Override
    public void setRegistrationEndpoint(EndpointReferenceType epr) {
        registrationService.setRegistrationEndpoint(epr);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsat.service.Handler#setCoordinatorEndpoint(org.apache.cxf.ws.addressing.EndpointReferenceType)
     */
    @Override
    public void setCoordinatorEndpoint(EndpointReferenceType epr) {
        protocolService.setCoordinatorEndpoint(epr);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsat.service.Handler#setParticipantEndpoint(org.apache.cxf.ws.addressing.EndpointReferenceType)
     */
    @Override
    public void setParticipantEndpoint(EndpointReferenceType epr) {
        protocolService.setParticipantEndpoint(epr);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsat.service.Handler#isTranActive()
     */
    @Override
    public boolean isTranActive() {
        return handlerService.isTranActive();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsat.service.Handler#handleClientRequest()
     */
    @Override
    public WSATContext handleClientRequest() throws WSATException {
        return handlerService.clientRequest();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsat.service.Handler#handleClientResponse(java.lang.String)
     */
    @Override
    public void handleClientResponse() throws WSATException {
        handlerService.clientResponse();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsat.service.Handler#handleClientFault(java.lang.String)
     */
    @Override
    public void handleClientFault() throws WSATException {
        handlerService.clientFault();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsat.service.Handler#handleServerRequest(java.lang.String, java.lang.String)
     */
    @Override
    public void handleServerRequest(String ctxId, EndpointReferenceType coordinator, long expires) throws WSATException {
        handlerService.serverRequest(ctxId, coordinator, expires);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsat.service.Handler#handleServerResponse(java.lang.String)
     */
    @Override
    public void handleServerResponse() throws WSATException {
        handlerService.serverResponse();
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.ibm.ws.wsat.service.Handler#handleServerFault(java.lang.String)
     */
    @Override
    public void handleServerFault() throws WSATException {
        handlerService.serverFault();
    }
}
