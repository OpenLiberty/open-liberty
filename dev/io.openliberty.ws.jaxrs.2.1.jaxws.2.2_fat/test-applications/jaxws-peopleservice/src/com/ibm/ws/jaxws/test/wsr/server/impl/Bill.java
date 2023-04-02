/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
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
package com.ibm.ws.jaxws.test.wsr.server.impl;

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
        // Increasing the timeouts for the rest client to prevent failures in slow builds
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("com.ibm.ws.jaxrs.client.connection.timeout", "50000");
        cb.property("com.ibm.ws.jaxrs.client.receive.timeout", "50000");

        Client client = cb.build();
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("jaxwsEP2")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        return response.readEntity(String.class);
    }
}
