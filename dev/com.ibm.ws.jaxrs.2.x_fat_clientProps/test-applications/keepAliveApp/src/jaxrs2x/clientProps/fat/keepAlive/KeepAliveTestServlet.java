/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs2x.clientProps.fat.keepAlive;

import static org.junit.Assert.assertEquals;

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

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/KeepAliveTestServlet")
public class KeepAliveTestServlet extends FATServlet {

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
    public void testDefaultKeepAlive(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Response r = target(req).request().header("Expect-Connection", "keep-alive").get();
        assertEquals("success", r.readEntity(String.class));
    }

    @Test
    public void testSetKeepAlive(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Response r = target(req).property("com.ibm.ws.jaxrs.client.keepalive.connection", "keep-alive").request().header("Expect-Connection", "keep-alive").get();
        assertEquals("success", r.readEntity(String.class));
    }

    @Test
    public void testSetClose(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Response r = target(req).property("com.ibm.ws.jaxrs.client.keepalive.connection", "close").request().header("Expect-Connection", "close").get();
        assertEquals("success", r.readEntity(String.class));
    }

    private WebTarget target(HttpServletRequest request) {
        String base = "http://" + request.getServerName() + ':' + request.getServerPort() + "/keepAliveApp/rest/test";
        return client.target(base);
    }

}