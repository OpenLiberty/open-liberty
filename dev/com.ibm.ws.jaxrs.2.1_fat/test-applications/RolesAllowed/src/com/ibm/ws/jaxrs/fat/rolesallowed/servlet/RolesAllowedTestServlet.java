/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package com.ibm.ws.jaxrs.fat.rolesallowed.servlet;

import static org.junit.Assert.assertEquals;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet(urlPatterns = "/RolesAllowedTestServlet")
public class RolesAllowedTestServlet extends FATServlet {

    private static final long serialVersionUID = 4563456788769868446L;

    private Client client;

    @Override
    public void before() throws ServletException {
        client = ClientBuilder.newClient();
    }

    @Override
    public void after() {
        client.close();
    }

    private final String endpoint = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/RolesAllowed/RolesAllowedResource/";

    @Test
    public void testEveryoneAllowed() throws Exception {
        Response response = null;
        WebTarget t = client.target(endpoint);

        response = t.request().get();
        assertEquals(200, response.getStatus());

        client.close();
    }

    @Test
    public void testNotAllowed() throws Exception {
        Response response = null;
        WebTarget t = client.target(endpoint + "admin");

        response = t.request().get();
        assertEquals(401, response.getStatus());

        client.close();
    }
}