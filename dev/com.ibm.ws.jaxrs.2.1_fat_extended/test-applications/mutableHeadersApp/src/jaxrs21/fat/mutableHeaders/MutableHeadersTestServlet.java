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
package jaxrs21.fat.mutableHeaders;

import static org.junit.Assert.assertEquals;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/MutableHeadersTestServlet")
public class MutableHeadersTestServlet extends FATServlet {

    private Client client;

    
    @Override
    public void init() throws ServletException {
        client = ClientBuilder.newClient();
    }

    @Override
    public void destroy() {
        client.close();
    }

    @Test
    public void testHeadersCanBeMutated(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        int port = req.getServerPort();
        Response r = client.target("http://localhost:" + port + "/mutableHeadersApp/mutableHeaders")
                           .request()
                           .header(ContainerRequestFilter1.PREEXISTING, "original")
                           .get();
        String entity = r.readEntity(String.class);
        assertEquals("Failed to mutate headers: " + entity, "SUCCESS", entity);
        assertEquals("Unexpected response code", 200, r.getStatus());
        
    }
}