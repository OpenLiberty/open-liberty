/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package jaxrs21.fat.subresource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/SubResourceTestServlet")
public class SubResourceTestServlet extends FATServlet {

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
    public void testSubResourceMsgBodyWriter(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Response response = target(req, "id/1").request().get();
        String result = response.readEntity(String.class);
        assertEquals(200, response.getStatus());
        System.out.println("result=" + result);
        assertEquals("{\"root\":\"1\"}", result);

        response = target(req, "sub/id/1").request().get();
        result = response.readEntity(String.class);
        assertEquals(200, response.getStatus());
        System.out.println("result=" + result);
        assertEquals("{\"subId\":\"1\"}", result);
    }

    private WebTarget target(HttpServletRequest request, String path) {
        String base = "http://" + request.getServerName() + ':' + request.getServerPort() + "/subResourceApp/root/";
        return client.target(base + path);
    }

}