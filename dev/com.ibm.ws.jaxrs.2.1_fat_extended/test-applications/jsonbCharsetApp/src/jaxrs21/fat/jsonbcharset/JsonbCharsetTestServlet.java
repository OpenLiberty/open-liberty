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
package jaxrs21.fat.jsonbcharset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;
import java.util.regex.Pattern;

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
@WebServlet(urlPatterns = "/JsonbCharsetTestServlet")
public class JsonbCharsetTestServlet extends FATServlet {

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
    public void testUS_ASCII(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String pattern = "{\"name\":\"Bob Smith\",\"age\":34}";
        Response response = target(req, "person").request().header("Accept-Charset", "US-ASCII").get();
        assertEquals(200, response.getStatus());
        compareJSON(pattern, response.readEntity(String.class));
    }

    @Test
    public void testISO_8859_1(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String pattern = "{\"name\":\"Bob Smith\",\"age\":34}";
        Response response = target(req, "person").request().header("Accept-Charset", "ISO-8859-1").get();
        assertEquals(200, response.getStatus());
        compareJSON(pattern, response.readEntity(String.class));
    }

    @Test
    public void testUTF8(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String pattern = "{\"name\":\"Bŏb Smitй\",\"age\":34}";
        Response response = target(req, "person").request().header("Accept-Charset", "UTF-8").get();
        assertEquals(200, response.getStatus());
        compareJSON(pattern, response.readEntity(String.class));
    }

    @Test
    public void testUTF16(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String pattern = "{\"name\":\"Bŏb Smitй\",\"age\":34}";
        Response response = target(req, "person").request().header("Accept-Charset", "UTF-16").get();
        assertEquals(200, response.getStatus());
        compareJSON(pattern, response.readEntity(String.class));
    }

    @Test
    public void testUTF32(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String pattern = "{\"name\":\"Bŏb Smitй\",\"age\":34}";
        Response response = target(req, "person").request().header("Accept-Charset", "UTF-32").get();
        assertEquals(200, response.getStatus());
        compareJSON(pattern, response.readEntity(String.class));
    }

    @Test
    public void testUS_ASCII_ServerReturnsUniqueChar(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Response response = target(req, "personUniqueChar").request().header("Accept-Charset", "US-ASCII").get();
        assertEquals(200, response.getStatus());
        Person p = response.readEntity(Person.class);
        assertTrue("Bŏb Smitй".equals(p.getName()));
    }

    @Test
    public void testISO_8859_1_ServerReturnsUniqueChar(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        Response response = target(req, "personUniqueChar").request().header("Accept-Charset", "ISO-8859-1").get();
        assertEquals(200, response.getStatus());
        Person p = response.readEntity(Person.class);
        assertTrue("Bŏb Smitй".equals(p.getName()));
    }

    @Test
    public void testUnspecified(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String pattern = "{\"name\":\"Bob Smith\",\"age\":34}";
        Response response = target(req, "person").request().get();
        assertEquals(200, response.getStatus());
        compareJSON(pattern, response.readEntity(String.class));
    }

    @Test
    public void testUnspecified_ServerReturnsUniqueChar(HttpServletRequest req, HttpServletResponse resp) throws Exception {
        String pattern = "{\"name\":\"Bŏb Smitй\",\"age\":34}";
        Response response = target(req, "personUniqueChar").request().get();
        assertEquals(200, response.getStatus());
        compareJSON(pattern, response.readEntity(String.class));
    }

    private void compareJSON(String expected, String actual) throws Exception {
        System.out.println("compareJSON\n\texpected = " + expected + "\n\tactual = " + actual);
        try {
            JsonReader jsonReader = Json.createReader(new StringReader(expected));
            JsonObject exp = jsonReader.readObject();
            jsonReader = Json.createReader(new StringReader(actual));
            JsonObject act = jsonReader.readObject();
            assertTrue("expected=" + expected + ", actual=" + actual, exp.equals(act));
        } catch (JsonParsingException e) {
            // Json failed to parse as an object, try array
            JsonReader jsonReader = Json.createReader(new StringReader(expected));
            JsonArray exp = jsonReader.readArray();
            jsonReader = Json.createReader(new StringReader(actual));
            JsonArray act = jsonReader.readArray();
            assertTrue("expected=" + expected + ", actual=" + actual, exp.equals(act));
        }
    }

    private WebTarget target(HttpServletRequest request, String path) {
        String base = "http://" + request.getServerName() + ':' + request.getServerPort()
            + "/jsonbCharsetApp/rest/person/";
        return client.target(base + path);
    }
}
