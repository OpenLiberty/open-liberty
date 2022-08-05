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
package com.ibm.ws.wsat.webservice;

import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.soap.Addressing;

import com.ibm.websphere.ras.Tr;
import com.ibm.websphere.ras.TraceComponent;
import com.ibm.ws.jaxws.wsat.Constants;
import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.utils.WSATRequestHandler;
import com.ibm.ws.wsat.webservice.client.wsat.Notification;

@javax.jws.WebService(endpointInterface = "com.ibm.ws.wsat.webservice.ParticipantPortType", targetNamespace = "http://webservice.wsat.ws.ibm.com",
                      serviceName = "ParticipantService", portName = "ParticipantPort")
@Addressing(enabled = true, required = true)
public class ParticipantBindingImpl {

    private static final TraceComponent tc = Tr.register(ParticipantBindingImpl.class, Constants.TRACE_GROUP, null);

    @Resource
    private WebServiceContext ctx;

    public void prepareOperation(Notification parameters) throws WSATException {
        WSATRequestHandler.getInstance().handleParticipantPrepareRequest(parameters, ctx);
    }

    public void commitOperation(Notification parameters) throws WSATException {
        WSATRequestHandler.getInstance().handleParticipantCommitRequest(parameters, ctx);
    }

    public void rollbackOperation(Notification parameters) throws WSATException {
        WSATRequestHandler.getInstance().handleParticipantRollbackRequest(parameters, ctx);
    }

}
