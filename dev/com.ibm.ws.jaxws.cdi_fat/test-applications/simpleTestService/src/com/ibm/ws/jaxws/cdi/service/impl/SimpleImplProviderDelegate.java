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
package com.ibm.ws.jaxws.cdi.service.impl;

import javax.jws.WebService;
import javax.xml.soap.SOAPMessage;

@WebService(targetNamespace = "http://impl.service.cdi.jaxws.ws.ibm.com/", serviceName = "SimpleImplProviderService", portName = "SimpleImplProviderPort",
            wsdlLocation = "WEB-INF/wsdl/SimpleImplProviderService.wsdl")
public class SimpleImplProviderDelegate {

    com.ibm.ws.jaxws.cdi.service.impl.SimpleImplProvider _simpleImplProvider = null;

    public SOAPMessage invoke(SOAPMessage request) {
        return _simpleImplProvider.invoke(request);
    }

    public SimpleImplProviderDelegate() {
        _simpleImplProvider = new com.ibm.ws.jaxws.cdi.service.impl.SimpleImplProvider();
    }

}