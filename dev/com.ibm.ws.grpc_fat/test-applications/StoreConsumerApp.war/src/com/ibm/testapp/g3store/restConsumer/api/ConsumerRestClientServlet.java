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
package com.ibm.testapp.g3store.restConsumer.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.eclipse.microprofile.rest.client.RestClientBuilder;
import org.junit.Test;

import componenttest.app.FATServlet;

/**
 *
 */
@WebServlet(urlPatterns = "/ConsumerRestClientServlet")
public class ConsumerRestClientServlet extends FATServlet {

    /**  */
    private static final long serialVersionUID = 1L;
    Logger _log = Logger.getLogger(ConsumerRestClientServlet.class.getName());
    private RestClientBuilder builder;

    @Context
    HttpHeaders httpHeaders;

    @Test
    public void getAllAppNames(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        final String m = "getAllAppNames";
        int port = req.getServerPort();
        ConsumerRestEndpoint client = RestClientBuilder.newBuilder()
                        .baseUrl(new URL("http://localhost:" + port + "/consumer/appNames"))
                        .property("com.ibm.ws.jaxrs.client.receive.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.connection.timeout", "120000")
                        .property("com.ibm.ws.jaxrs.client.keepalive.connection", "close")
                        .build(ConsumerRestEndpoint.class);

        _log.info(m + " invoking rest client");

        Response r = client.getAllAppNames(httpHeaders);
        assertEquals(200, r.getStatus());
        String entity = r.readEntity(String.class);
        _log.info(m + " entity: " + entity);
        assertNotNull(entity);
        assertEquals("close", entity.toLowerCase());
    }

}
