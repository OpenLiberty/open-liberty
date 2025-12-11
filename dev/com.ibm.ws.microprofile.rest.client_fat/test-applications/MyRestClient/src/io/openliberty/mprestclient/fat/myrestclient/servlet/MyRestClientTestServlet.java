/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.mprestclient.fat.myrestclient.servlet;

import static org.junit.Assert.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;
import io.openliberty.mprestclient.fat.myrestclient.bundle.MyRESTClient;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/MyRestClientTestServlet")
public class MyRestClientTestServlet extends FATServlet {

    private static final String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/MyRestClient";

    private Client client;

    @Override
    public void before() throws ServletException {
        client = ClientBuilder.newClient();
    }

    @Override
    public void after() {
        client.close();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testHelloWorld() throws MalformedURLException, InterruptedException {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("rest/greet")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get();

        assertEquals(200, response.getStatus());
        assertEquals("Hello, World!", response.readEntity(String.class));
        
        String result = MyRESTClient.getAPI(new URL(URI_CONTEXT_ROOT)).greet();
        assertEquals("Hello, World!", result);
    }
}
