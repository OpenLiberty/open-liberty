/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.managedbeans;

import static org.junit.Assert.assertEquals;

import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet(urlPatterns = "/ManagedBeansTestServlet")
public class ManagedBeansTestServlet extends FATServlet {

    private static final long serialVersionUID = 698782229822756594L;
    private static final String baseUri = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/testManagedBeans/managedbean/";

    @Test
    public void testApplicationSubclassPostConstruct() throws Exception {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target(baseUri + "applicationvalue");
        Response response = t.request().accept("text/plain").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "100,true");
        c.close();
    }

    @Test
    public void testResourcePostConstruct() throws Exception {
        ClientBuilder cb = ClientBuilder.newBuilder();
        Client c = cb.build();
        WebTarget t = c.target(baseUri + "resourcevalue");
        Response response = t.request().accept("text/plain").get();
        assertEquals(200, response.getStatus());
        assertEquals(response.readEntity(String.class), "1000,true");
        c.close();
    }
}
