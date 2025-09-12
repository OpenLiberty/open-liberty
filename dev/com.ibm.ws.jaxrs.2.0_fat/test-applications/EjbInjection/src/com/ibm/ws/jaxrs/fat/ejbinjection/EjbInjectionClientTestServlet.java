/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
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
package com.ibm.ws.jaxrs.fat.ejbinjection;


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
@WebServlet(urlPatterns = "/EjbInjectionClientTestServlet")
public class EjbInjectionClientTestServlet extends FATServlet {

    private static final String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/ejbinjection/";

    private Client client;

    @Override
    public void before() throws ServletException {
        client = ClientBuilder.newClient();
    }

    @Override
    public void after() {
        client.close();
    }

//    @Test
    public void testNoInterfaceInjection() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("nointerface/greet")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello, World!", response.readEntity(String.class));
    }

//    @Test
    public void testOneInterfaceInjection() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("interface/greet")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello, World!", response.readEntity(String.class));
    }

    // TODO: There is currently a bug in RESTEasy that prevents this from working.
    // The first interface is always selected when the method is invoked.
    // Meaning, calling EjbInjectionMultipleInterfacesResource.goodbye() fails because it's
    // not a method on EjbInjectionBeanInterface.
    @Test
    public void testMultiInterfaceInjection() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("multiinterface/greet")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello, World!", response.readEntity(String.class));

        Response response2 = client.target(URI_CONTEXT_ROOT)
                        .path("multiinterface/farewell")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response2.getStatus());
        assertEquals("Goodbye, World!", response2.readEntity(String.class));
    }

}