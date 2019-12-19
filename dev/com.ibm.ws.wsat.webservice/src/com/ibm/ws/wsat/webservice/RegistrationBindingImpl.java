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

import com.ibm.ws.wsat.utils.WSATRequestHandler;
import com.ibm.ws.wsat.webservice.client.wscoor.RegisterResponseType;
import com.ibm.ws.wsat.webservice.client.wscoor.RegisterType;

@javax.jws.WebService(endpointInterface = "com.ibm.ws.wsat.webservice.RegistrationPortType", targetNamespace = "http://webservice.wsat.ws.ibm.com",
                      serviceName = "RegistrationService", portName = "RegistrationPort", wsdlLocation = "META-INF/wsdl/wscoor-liberty.wsdl")
@Addressing(enabled = true, required = true)
public class RegistrationBindingImpl {

    @Resource
    private WebServiceContext ctx;

    public RegisterResponseType registerOperation(RegisterType parameters) throws Throwable {
        return WSATRequestHandler.getInstance().handleRegistrationRequest(parameters, ctx);
    }
}
