/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs20.mediatype;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Test;

import componenttest.app.FATServlet;

@WebServlet(urlPatterns = "/MediaTypeTestServlet")
public class MediaTypeTestServlet extends FATServlet {
    private static final long serialVersionUID = 4563445834756294836L;

    final static String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/mediatype/";

    private Client client;

    @Override
    public void init() throws ServletException {
        client = ClientBuilder.newBuilder().register(LoggingFilter.class).build();
    }

    @After
    private void teardown() {
        client.close();
    }

    @Test
    public void testClientSendsNoHeaders() throws Exception {
        // must use lower level client APIs to avoid client sending a Content-type header
        URL url = new URL(URI_CONTEXT_ROOT + "app/resource/class");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write("foo".getBytes());
        conn.connect();
        assertEquals(415, conn.getResponseCode());
    }

    @Test
    public void testClientSendsAcceptHeaderMatchingClassProducesAnnotation() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("/app/resource/class")
                        .request(MediaType.TEXT_PLAIN)
                        .post(Entity.text("foo"));
        assertEquals(200, response.getStatus());
        assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()));
        String responseBody = response.readEntity(String.class);
        assertTrue(responseBody.contains("Accept: text/plain") && responseBody.contains("Content-Type: text/plain"));
    }

    @Test
    public void testClientSendsAcceptHeaderMatchingMethodProducesAnnotation() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("/app/resource/produceXml")
                        .request(MediaType.TEXT_XML)
                        .post(Entity.text("foo"));
        assertEquals(200, response.getStatus());
        assertTrue(MediaType.TEXT_XML_TYPE.isCompatible(response.getMediaType()));
        String responseBody = response.readEntity(String.class);
        assertTrue(responseBody.contains("Accept: text/xml") && responseBody.contains("Content-Type: text/plain"));
    }

    @Test
    public void testClientSendsContentTypeHeaderMatchingMethodConsumesAnnotation() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("/app/resource/consumeXml")
                        .request(MediaType.TEXT_PLAIN)
                        .post(Entity.xml("<foo/>"));
        assertEquals(200, response.getStatus());
        assertTrue(MediaType.TEXT_PLAIN_TYPE.isCompatible(response.getMediaType()));
        String responseBody = response.readEntity(String.class);
        assertTrue(responseBody.contains("Accept: text/plain") && responseBody.contains("Content-Type: application/xml"));
    }

    @Test
    public void testClientSendsContentTypeHeaderNonMatchingClassConsumesAnnotation() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("/app/resource/class")
                        .request(MediaType.TEXT_PLAIN)
                        .post(Entity.json("foo"));
        assertEquals(415, response.getStatus());
    }

    @Test
    public void testClientSendsContentTypeHeaderNonMatchingMethodConsumesAnnotation() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("/app/resource/consumeXml")
                        .request(MediaType.TEXT_PLAIN)
                        .post(Entity.json("foo"));
        assertEquals(415, response.getStatus());
    }

    @Test
    public void testClientSendsContentTypeHeaderNonMatchingClassConsumesAnnotation_noSlash() throws Exception {
        // must use lower level client APIs to avoid client blocking an invalid Content-type header
        URL url = new URL(URI_CONTEXT_ROOT + "app/resource/class");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "JUSTJSON");
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write("foo".getBytes());
        conn.connect();
        assertEquals(400, conn.getResponseCode());
    }

    @Test
    public void testClientSendsContentTypeHeaderNonMatchingMethodConsumesAnnotation_noSlash() throws Exception {
        // must use lower level client APIs to avoid client blocking an invalid Content-type header
        URL url = new URL(URI_CONTEXT_ROOT + "app/resource/consumeXml");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty(HttpHeaders.CONTENT_TYPE, "NotAMediaType");
        conn.setDoOutput(true);
        OutputStream os = conn.getOutputStream();
        os.write("foo".getBytes());
        conn.connect();
        assertEquals(400, conn.getResponseCode());
    }

    @Test
    public void testClientSendsAcceptHeaderNonMatchingClassProducesAnnotation() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("/app/resource/class")
                        .request(MediaType.APPLICATION_JSON)
                        .post(Entity.text("foo"));
        assertEquals(406, response.getStatus());
    }

    @Test
    public void testClientSendsAcceptHeaderNonMatchingMethodProducesAnnotation() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("/app/resource/produceXml")
                        .request(MediaType.APPLICATION_JSON)
                        .post(Entity.text("foo"));
        assertEquals(406, response.getStatus());
    }

    @Test
    public void testClientSendsAcceptHeaderNonMatchingClassProducesAnnotation_noSlash() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("/app/resource/produceXml")
                        .request("ANYTHING")
                        .post(Entity.text("foo"));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testClientSendsAcceptHeaderNonMatchingMethodProducesAnnotation_noSlash() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("/app/resource/produceXml")
                        .request("WHATEVER")
                        .post(Entity.text("foo"));
        assertEquals(400, response.getStatus());
    }

    @Test
    public void testClientSendsStarAcceptHeaderToClassProducesAnnotation() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("/app/resource/class")
                        .request("*")
                        .post(Entity.text("foo"));
        assertTrue(200 == response.getStatus());
        String responseBody = response.readEntity(String.class);
        assertTrue(responseBody.contains("Accept: *") && responseBody.contains("Content-Type: text/plain"));
    }

    @Test
    public void testClientSendsStarAcceptHeaderToMethodProducesAnnotation() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("/app/resource/produceXml")
                        .request("*")
                        .post(Entity.text("foo"));
        assertTrue(200 == response.getStatus());
        String responseBody = response.readEntity(String.class);
        assertTrue(responseBody.contains("Accept: *") && responseBody.contains("Content-Type: text/plain")
                   && responseBody.contains("<text>"));
    }
}