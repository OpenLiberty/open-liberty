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
package io.openliberty.interop;

import static org.junit.Assert.assertEquals;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JaxrsPrototypeClientTestServlet")
public class InteropStartJaxrsClientTestServlet extends FATServlet {

    private static final String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/interop/ep2";

    private Client client;

    @Override
    public void before() throws ServletException {
        // Increasing the timeouts for the rest client to prevent failures in slow builds
        ClientBuilder cb = ClientBuilder.newBuilder();
        cb.property("com.ibm.ws.jaxrs.client.connection.timeout", "50000");
        cb.property("com.ibm.ws.jaxrs.client.receive.timeout", "50000");
        client = cb.build();
    }

    @Override
    public void after() {
        client.close();
    }

    @Test
    public void testHelloWorld() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("echo")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();

        assertEquals(200, response.getStatus());
        assertEquals("Echo from JAXRS Endpoint 2", response.readEntity(String.class));
    }
}