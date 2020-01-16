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
package com.ibm.ws.jaxrs21.fat.uriInfo;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet(urlPatterns = "/ClientTestServlet")
public class ClientTestServlet extends FATServlet {

    private static final long serialVersionUID = -8965492570925619992L;

    final static String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/uriInfo/";

    private Client client;

    @Override
    public void init() throws ServletException {
        client = ClientBuilder.newClient();
    }

    @After
    private void teardown() {
        client.close();
    }

    @Test
    public void newClientNoModifyUriInfoTest() throws Exception {
        Response response = null;
        CompletableFuture<Response> completableFuture = client.target(URI_CONTEXT_ROOT).path("resources/test").request(MediaType.TEXT_PLAIN_TYPE).rx().get().toCompletableFuture();
        try {
            response = completableFuture.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        assertEquals(200, response.getStatus());
        assertEquals("uriInfo.baseUri: http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/uriInfo/resources/, "
                     + "client pre=http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/uriInfo/resources/test/client, "
                     + "client preUri=http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/uriInfo/resources/test/client, "
                     + "client post=http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/uriInfo/resources/test/client, "
                     + "client postUri=http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/uriInfo/resources/test/client, "
                     + "uriInfo.baseUri 2nd time: http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/uriInfo/resources/",
                     response.readEntity(String.class));

    }
}