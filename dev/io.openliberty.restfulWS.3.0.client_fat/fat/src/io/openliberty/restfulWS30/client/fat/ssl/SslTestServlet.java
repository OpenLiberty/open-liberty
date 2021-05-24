/*******************************************************************************
 * Copyright (c) 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.restfulWS30.client.fat.ssl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


import jakarta.servlet.annotation.WebServlet;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet("/SslTestServlet")
public class SslTestServlet extends FATServlet {

    private static int PORT = Integer.getInteger("bvt.prop.HTTP_default.secure", 8020);


    @Test
    public void testCanSetAndUseConfiguredSSLContext() throws Exception {
        ClientBuilder builder = ClientBuilder.newBuilder()
                                             .property("com.ibm.ws.jaxrs.client.ssl.config", "mySSLConfig");
        Client client = builder.build();
        try {
            WebTarget target = client.target("https://localhost:" + PORT + "/ssl/hello/secure");
            Response response = target.request().get();
            assertEquals(200, response.getStatus());
            assertEquals("Hello secure world!", response.readEntity(String.class));
        } finally {
            client.close();
        }
    }

    @Test
    public void testUsesDefaultSSLConfigWhenNoContextSpecified() throws Exception {
        ClientBuilder builder = ClientBuilder.newBuilder();
        Client client = builder.build();
        WebTarget target = client.target("https://localhost:" + PORT + "/ssl/hello/secure");
        try {
            Response response = target.request().get();
            assertEquals(200, response.getStatus());
            assertEquals("Hello secure world!", response.readEntity(String.class));
        } finally {
            client.close();
        }
    }
}
