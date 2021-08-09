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
package jaxrs21.fat.jsonb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.StringReader;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.stream.JsonParsingException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Test;

import componenttest.app.FATServlet;

@SuppressWarnings("serial")
@WebServlet(urlPatterns = "/JsonBTestServlet")
public class JsonBTestServlet extends FATServlet {

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
    public void testGETPerson(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String pattern = "{\"first\":\"first\",\"last\":\"last\"}";
        Response response = target(req, "person").request().get();
        assertEquals(200, response.getStatus());
        compareJSON(pattern, response.readEntity(String.class));
    }

    @Test
    public void testPOSTPerson(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String pattern = "{\"first\":\"first\",\"last\":\"last\"}";
        Response response = target(req, "person").request().post(Entity.entity(pattern, MediaType.APPLICATION_JSON));
        assertEquals(200, response.getStatus());
        compareJSON(pattern, response.readEntity(String.class));
    }

    @Test
    public void testGETCollection(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String pattern = "[\"string1\",\"\",\"string3\"]";
        Response response = target(req, "string").request().get();
        assertEquals(200, resp.getStatus());
        compareJSON(pattern, response.readEntity(String.class));
    }

    @Test
    public void testPOSTCollection(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String pattern = "[\"string1\", \"\", \"string3\"]";
        Response response = target(req, "string").request().post(Entity.entity(pattern, MediaType.APPLICATION_JSON));
        assertEquals(200, resp.getStatus());
        compareJSON(pattern, response.readEntity(String.class));
    }

    @Test
    public void testGETCollectionWithObject(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String pattern = "[{\"first\":\"first1\",\"last\":\"last1\"},"
                         + "{\"first\":\"first2\",\"last\":\"last2\"},"
                         + "{\"first\":\"first3\",\"last\":\"last3\"}]";
        Response response = target(req, "personcollect").request().get();
        assertEquals(200, resp.getStatus());
        compareJSON(pattern, response.readEntity(String.class));
    }

    @Test
    public void testPOSTCollectionWithObject(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String pattern = "[{\"first\":\"first1\",\"last\":\"last1\"},"
                         + "{\"first\":\"first2\",\"last\":\"last2\"},"
                         + "{\"first\":\"first3\",\"last\":\"last3\"}]";
        Response response = target(req, "personcollect").request().post(Entity.entity(pattern, MediaType.APPLICATION_JSON));
        assertEquals(200, resp.getStatus());
        compareJSON(pattern, response.readEntity(String.class));
    }

    @Test
    public void testGETCollectionWithCollection(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String pattern = "[[{\"first\":\"first1\",\"last\":\"last1\"},"
                         + "{\"first\":\"first2\",\"last\":\"last2\"},"
                         + "{\"first\":\"first3\",\"last\":\"last3\"}],"
                         + "[{\"first\":\"first4\",\"last\":\"last4\"},"
                         + "null,"
                         + "{\"first\":\"first6\",\"last\":\"last6\"}]]";
        Response response = target(req, "collectionofcollection").request().get();
        assertEquals(200, resp.getStatus());
        compareJSON(pattern, response.readEntity(String.class));
    }

    @Test
    public void testPOSTCollectionWithCollection(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String pattern = "[[{\"first\":\"first1\",\"last\":\"last1\"},"
                         + "{\"first\":\"first2\",\"last\":\"last2\"},"
                         + "{\"first\":\"first3\",\"last\":\"last3\"}],"
                         + "[{\"first\":\"first4\",\"last\":\"last4\"},"
                         + "null,"
                         + "{\"first\":\"first6\",\"last\":\"last6\"}]]";
        Response response = target(req, "collectionofcollection").request().post(Entity.entity(pattern, MediaType.APPLICATION_JSON));
        assertEquals(200, resp.getStatus());
        compareJSON(pattern, response.readEntity(String.class));
    }

    @Test
    public void testGETCollectionWithArray(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String pattern = "[[{\"first\":\"first1\",\"last\":\"last1\"},"
                         + "{\"first\":\"first2\",\"last\":\"last2\"},"
                         + "{\"first\":\"first3\",\"last\":\"last3\"}],"
                         + "[{\"first\":\"first4\",\"last\":\"last4\"},"
                         + "null,"
                         + "{\"first\":\"first6\",\"last\":\"last6\"}]]";
        Response response = target(req, "collectionofarray").request().get();
        assertEquals(200, resp.getStatus());
        compareJSON(pattern, response.readEntity(String.class));
    }

    @Test
    public void testPOSTCollectionWithArray(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String pattern = "[[{\"first\":\"first1\",\"last\":\"last1\"},"
                         + "{\"first\":\"first2\",\"last\":\"last2\"},"
                         + "{\"first\":\"first3\",\"last\":\"last3\"}],"
                         + "[{\"first\":\"first4\",\"last\":\"last4\"},"
                         + "null,"
                         + "{\"first\":\"first6\",\"last\":\"last6\"}]]";
        Response response = target(req, "collectionofarray").request().post(Entity.entity(pattern, MediaType.APPLICATION_JSON));
        assertEquals(200, resp.getStatus());
        compareJSON(pattern, response.readEntity(String.class));
    }

    private void compareJSON(String expected, String actual) throws Exception {
        System.out.println("compareJSON: \n expected: " + expected + "\n actual: " + actual);
        try {
            JsonReader jsonReader = Json.createReader(new StringReader(expected));
            JsonObject exp = jsonReader.readObject();
            jsonReader = Json.createReader(new StringReader(actual));
            JsonObject act = jsonReader.readObject();
            assertTrue("expected=" + expected + ", actual=" + actual, exp.equals(act));
        } catch (JsonParsingException e) {
            // Json failed to parse as an object, try array
            try {
                JsonReader jsonReader = Json.createReader(new StringReader(expected));
                JsonArray exp = jsonReader.readArray();
                jsonReader = Json.createReader(new StringReader(actual));
                JsonArray act = jsonReader.readArray();
                assertTrue("expected=" + expected + ", actual=" + actual, exp.equals(act));
            } catch (JsonParsingException e2) {
                e2.printStackTrace();
                fail("Could not parse JSON expected=" + expected + ", actual=" + actual);
            }
        }
    }

    private WebTarget target(HttpServletRequest request, String path) {
        String base = "http://" + request.getServerName() + ':' + request.getServerPort() + "/jsonbapp/pojo/person/";
        return client.target(base + path);
    }
}
