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
package com.ibm.ws.jaxrs.fat.simpleJson;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.ResponseProcessingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet(urlPatterns = "/JaxrsJsonClientTestServlet")
public class JaxrsJsonClientTestServlet extends FATServlet {

    private static final long serialVersionUID = 4563445389586844836L;

    final static String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/simpleJson/";

    private static Client client;

    // needed to make requests with invalid json to the server
    private static HttpClient httpClient;

    @Override
    public void init() throws ServletException {
        client = ClientBuilder.newClient();
//      httpClient = new HttpClientBuilder.create().build();
        httpClient = new DefaultHttpClient();
    }

    @After
    private void teardown() {
        client.close();
        httpClient.getConnectionManager().shutdown();
    }

    @Test
    public void simpleTest() throws Exception {
        Car corvette = new Car();
        corvette.color = "red";
        corvette.make = "Chevrolet";
        corvette.model = "Corvette";
        corvette.year = 2014;

        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("simpleresource/post")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .post(Entity.json(corvette));
        assertEquals(200, response.getStatus());
        String actual = response.readEntity(String.class);

        assertEquals("2014 Chevrolet Corvette", actual);
    }

    @Test
    public void sendInvalidJson() throws Exception {

        HttpPost post = new HttpPost(URI_CONTEXT_ROOT + "simpleresource/post");
        StringEntity entity = new StringEntity("invalid");
        post.setEntity(entity);
        post.addHeader("Content-Type", "application/json");

        HttpResponse response = httpClient.execute(post);
        assertEquals(400, response.getStatusLine().getStatusCode());
    }

    @Test
    public void recieveInvalidJson() throws Exception {

        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("badsimpleresource/badresponse")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get();

        boolean exception = false;
        try {
            response.readEntity(Foo.class);
            fail();
        } catch (ResponseProcessingException e) {
            exception = true;
// Adding this catch block as a temporary fix for EE9 until
// https://issues.redhat.com/projects/RESTEASY/issues/RESTEASY-2727 is addressed.
        } catch (ProcessingException e) {

            exception = true;
        }
    }

    private class Foo {
        public String foo;
    }

}