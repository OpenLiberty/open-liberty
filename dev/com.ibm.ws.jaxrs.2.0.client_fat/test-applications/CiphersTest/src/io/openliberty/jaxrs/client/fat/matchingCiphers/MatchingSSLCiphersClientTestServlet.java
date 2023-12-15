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
package io.openliberty.jaxrs.client.fat.matchingCiphers;


import static org.junit.Assert.assertEquals;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import com.ibm.ws.jaxrs20.client.JAXRSClientConstants;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/MatchingSSLCiphersClientTestServlet")
public class MatchingSSLCiphersClientTestServlet extends FATServlet {

    private static final String SERVER_CONTEXT_ROOT = "https://localhost:" + Integer.getInteger("bvt.prop.HTTP_default.secure") + "/simpleSSL/";

    private static Client client;

    @Override
    public void before() throws ServletException {
        client = ClientBuilder.newClient();
        client.property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig");
    }

    @Override
    public void after() {
        client.close();
    }

//    @Test
    public void testSimpleSSLRequestWithMatchingSSLCiphers() {
        Response response = client.target(SERVER_CONTEXT_ROOT)
                        .path("echo")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello World!", response.readEntity(String.class));
    }
}