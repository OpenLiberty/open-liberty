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
package com.ibm.ws.jaxrs.fat.ejbinjection.servlet;


import static org.junit.Assert.assertEquals;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
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

    @Test
    public void testNoInterfaceInjection() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("nointerface/greet")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello, World!", response.readEntity(String.class));
    }

    @Test
    public void testSingleInterfaceInjection() {
        String message = "Hello, World!";
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("singleinterface/echo")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .post(Entity.entity(message, MediaType.TEXT_PLAIN));
        assertEquals(200, response.getStatus());
        assertEquals(message, response.readEntity(String.class));
    }

    @Test
    public void testMultipleInterfacesInjection() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("multipleinterfaces/greet")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello, World!", response.readEntity(String.class));

        Response response2 = client.target(URI_CONTEXT_ROOT)
                        .path("multipleinterfaces/farewell")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response2.getStatus());
        assertEquals("Goodbye, World!", response2.readEntity(String.class));
    }

    @Test
    public void testSingleAnnotatedInterfaceInjection() {
        String message = "Hello, World!";
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("singleannotatedinterface/echo")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .post(Entity.entity(message, MediaType.TEXT_PLAIN));
        assertEquals(200, response.getStatus());
        assertEquals(message, response.readEntity(String.class));
    }

    @Test
    public void testMultipleAnnotatedInterfacesInjection() {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("multipleannotatedinterfaces/greet")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        assertEquals("Hello, World!", response.readEntity(String.class));

        Response response2 = client.target(URI_CONTEXT_ROOT)
                        .path("multipleannotatedinterfaces/farewell")
                        .request(MediaType.TEXT_PLAIN_TYPE)
                        .get();
        assertEquals(200, response2.getStatus());
        assertEquals("Goodbye, World!", response2.readEntity(String.class));
    }

}