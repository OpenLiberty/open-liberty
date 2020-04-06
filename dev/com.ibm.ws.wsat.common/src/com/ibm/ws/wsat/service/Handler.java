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
package com.ibm.ws.wsat.service;

import org.apache.cxf.ws.addressing.EndpointReferenceType;

/**
 * Service interface for the CXF interceptors
 */
public interface Handler {

    /**
     * Determine if a transaction is currently active on the caller's thread
     * 
     * @return true if local transaction active
     */
    public boolean isTranActive();

    /**
     * Set the EndpointRefererence for our coordination and protocol services
     * 
     * @param epr registration, coordinator and participant EPRs
     */
    public void setRegistrationEndpoint(EndpointReferenceType epr);

    public void setCoordinatorEndpoint(EndpointReferenceType epr);

    public void setParticipantEndpoint(EndpointReferenceType epr);

    /**
     * Called for client-side (ie out-going) user web service request.
     * Returns the WSAT coordination context id for any distributed transaction, or
     * null if no transaction is active.
     * 
     * Caller should add a CoordinationContext SOAP header containing this id and
     * the endpoint reference of the registration service to the out-going SOAP request.
     * 
     * @return Coordination context id
     * @throws WSATException
     */
    public WSATContext handleClientRequest() throws WSATException;

    /**
     * Called after the response for a client-side web service request
     * has been received.
     * 
     * @throws WSATException
     */
    public void handleClientResponse() throws WSATException;

    /**
     * Called if a fault occurs processing the client-side web service
     * request, or if a fault response is received.
     * 
     * @throws WSATException
     */
    public void handleClientFault() throws WSATException;

    /**
     * Called for the server-side (ie in-coming) user web service request.
     * It should be passed the id and registration endpoint from any CoordinationContext
     * header present on the request.
     * 
     * @param ctxId coordination context id
     * @param registration EPR for the registration service
     * @param expires transaction expiry time
     * @throws WSATException
     */
    public void handleServerRequest(String ctxId, EndpointReferenceType registration, long expires) throws WSATException;

    /**
     * Called after the response for a server-side web service request
     * is to be sent.
     * 
     * @throws WSATException
     */
    public void handleServerResponse() throws WSATException;

    /**
     * Called if a fault occurs processing the server-side web service
     * request, or if a fault response is generated.
     * 
     * @throws WSATException
     */
    public void handleServerFault() throws WSATException;
}
