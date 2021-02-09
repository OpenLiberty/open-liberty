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
package jaxrs21.fat.jsonp;

import static org.junit.Assert.assertEquals;

import javax.json.Json;
import javax.json.JsonNumber;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import componenttest.app.FATServlet;

import org.junit.Test;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JsonpTestServlet")
public class JsonpTestServlet extends FATServlet {
    private Client client;

    @Override
    public void init() throws ServletException {
        client = ClientBuilder.newBuilder().build();
    }

    @Override
    public void destroy() {
        client.close();
    }

    private WebTarget target(HttpServletRequest request, String path) {
        String base = "http://" + request.getServerName() + ':' + request.getServerPort() + "/jsonpApp";
        System.out.println("target: " + base + path);
        return client.target(base + path);
    }

    @Test
    public void testSingleDigitJsonNumber(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        JsonNumber jsonNum =  Json.createValue(3);
        WebTarget t = target(req, "/resource/number/incr");
        int res = t.request().accept(MediaType.APPLICATION_JSON).post(Entity.json(jsonNum), Integer.class);
        assertEquals(4, res);
    }
}
