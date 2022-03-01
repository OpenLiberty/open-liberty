/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxws.test.wsr.server.impl;

import static org.junit.Assert.assertEquals;

import javax.jws.WebService;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.ibm.ws.jaxws.test.wsr.server.People;

@WebService(serviceName = "PeopleService", portName = "BillPort", endpointInterface = "com.ibm.ws.jaxws.test.wsr.server.People",
            targetNamespace = "http://server.wsr.test.jaxws.ws.ibm.com")
public class Bill implements People {
    private static final String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/prototype/ep2";

    @Override
    public String hello() {
        // Implement JAX-RS call here
        Client client = ClientBuilder.newClient();
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("jaxwsEP2")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        return response.readEntity(String.class);
    }
}
