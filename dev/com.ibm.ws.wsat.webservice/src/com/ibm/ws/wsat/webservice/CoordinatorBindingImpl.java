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

import com.ibm.ws.wsat.service.WSATException;
import com.ibm.ws.wsat.utils.WSATRequestHandler;
import com.ibm.ws.wsat.webservice.client.wsat.Notification;

@Addressing(enabled = true, required = true)
@javax.jws.WebService(endpointInterface = "com.ibm.ws.wsat.webservice.CoordinatorPortType", targetNamespace = "http://webservice.wsat.ws.ibm.com",
                      serviceName = "CoordinatorService", portName = "CoordinatorPort")
public class CoordinatorBindingImpl {
    @Resource
    private WebServiceContext ctx;

    public void preparedOperation(Notification parameters) throws WSATException {
        WSATRequestHandler.getInstance().handleCoordinatorPreparedRequest(parameters, ctx);
    }

    public void abortedOperation(Notification parameters) throws WSATException {
        WSATRequestHandler.getInstance().handleCoordinatorAbortedRequest(parameters, ctx);
    }

    public void readOnlyOperation(Notification parameters) throws WSATException {
        WSATRequestHandler.getInstance().handleCoordinatorReadonlyRequest(parameters, ctx);
    }

    public void committedOperation(Notification parameters) throws WSATException {
        WSATRequestHandler.getInstance().handleCoordinatorCommittedRequest(parameters, ctx);
    }

}
