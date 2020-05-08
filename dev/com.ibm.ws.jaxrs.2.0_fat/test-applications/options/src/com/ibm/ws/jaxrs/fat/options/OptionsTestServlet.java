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
package com.ibm.ws.jaxrs.fat.options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet(urlPatterns = "/OptionsTestServlet")
public class OptionsTestServlet extends FATServlet {

    private static final String clz = "OptionsTestServlet";
    private static final Logger LOG = Logger.getLogger(OptionsTestServlet.class.getName());

    private Client client;

    @Override
    public void init() throws ServletException {
        client = ClientBuilder.newBuilder().build();
    }

    @Override
    public void destroy() {
        client.close();
    }


    @Test
    public void testOptions(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        Response response = target(req, "/options/options/test").request().options();
        assertEquals(200, response.getStatus());
        System.out.println("testOptions Allowed headers are : " + response.getHeaderString("Allow"));
        assertTrue(response.getHeaderString("Allow").contains("GET"));
        assertTrue(response.getHeaderString("Allow").contains("PUT"));
        assertTrue(response.getHeaderString("Allow").contains("POST"));
        assertTrue(response.getHeaderString("Allow").contains("OPTIONS"));
        assertTrue(response.getHeaderString("Allow").contains("HEAD"));
        assertTrue(!(response.getHeaderString("Allow").contains("DELETE")));
        assertTrue(!(response.getHeaderString("Allow").contains("PATCH")));

    }

    @Test
    public void testOptions2(HttpServletRequest req, HttpServletResponse resp) throws Exception {

        Response response = target(req, "/options/options/test2/1").request().options();
        assertEquals(200, response.getStatus());
        System.out.println("testOptions2 Allowed headers are : " + response.getHeaderString("Allow"));
        assertTrue(!(response.getHeaderString("Allow").contains("GET")));
        assertTrue(!(response.getHeaderString("Allow").contains("PUT")));
        assertTrue(!(response.getHeaderString("Allow").contains("POST")));
        assertTrue(response.getHeaderString("Allow").contains("DELETE"));
        assertTrue(!(response.getHeaderString("Allow").contains("PATCH")));
        assertTrue(response.getHeaderString("Allow").contains("OPTIONS"));
        assertTrue(!(response.getHeaderString("Allow").contains("HEAD")));

    }






    private WebTarget target(HttpServletRequest request, String path) {
        String base = "http://" + request.getServerName() + ':' + request.getServerPort();
        System.out.println("target : " + base + path);
        return client.target(base + path);
    }


}