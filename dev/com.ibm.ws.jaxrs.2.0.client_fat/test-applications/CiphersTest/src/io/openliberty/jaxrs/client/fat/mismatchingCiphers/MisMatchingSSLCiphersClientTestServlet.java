/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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
package io.openliberty.jaxrs.client.fat.mismatchingCiphers;


import static org.junit.Assert.fail;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.annotation.AllowedFFDC;
import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/MisMatchingSSLCiphersClientTestServlet")
public class MisMatchingSSLCiphersClientTestServlet extends FATServlet {

    private static final String SERVER_CONTEXT_ROOT = "https://localhost:" + Integer.getInteger("bvt.prop.HTTP_default.secure") + "/simpleSSL/";

    private static Client client;

    @Override
    public void before() throws ServletException {
        ClientBuilder cb = ClientBuilder.newBuilder();
        // Property must be set on the ClientBuilder for EE9+
        // Setting on the Client technically works for EE7 & EE8, but we don't document it.
        cb.property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig");
        client = cb.build();
    }

    @Override
    public void after() {
        client.close();
    }

    @Test
    @AllowedFFDC( { "javax.ws.rs.ProcessingException", "java.lang.IllegalArgumentException" })
    public void testSimpleSSLRequestWithMisMatchingSSLCiphers() {
        try {
            Response response = client.target(SERVER_CONTEXT_ROOT)
                            .path("echo")
                            .request(MediaType.TEXT_PLAIN_TYPE)
                            .get();

            // we should never reach this code, client should throw a javax.ws.rs.ProcessingException instead.
            fail("Request should throw a javax.ws.rs.ProcessingException trying to make a connection with mismatching cipher suites");
        } catch (ProcessingException ex) {} // we don't need to do anything here
    }
}