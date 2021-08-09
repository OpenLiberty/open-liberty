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
package com.ibm.ws.jaxws.test.jmx.impl;

import javax.jws.WebService;

import com.ibm.ws.jaxws.test.jmx.service.WSTestEndpointService;

/**
 * the test endpoint
 */
@WebService(serviceName = "WSTestEndpointService", portName = "WSTestEndpoint", targetNamespace = "http://jaxws.samples.ibm.com.jmx/")
public class WSTestEndpoint implements WSTestEndpointService {

    @Override
    public String say() {
        return "I am the first endpoint";
    }
}
