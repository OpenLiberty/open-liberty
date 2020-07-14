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
package jaxrs21.fat.jsonbcontextresolver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
@WebServlet(urlPatterns = "/JsonbContextResolverTestServlet")
public class JsonbContextResolverTestServlet extends FATServlet {

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
        String pattern = "{\"name\":\"Bob Smith\",\"age\":34}";
        Response response = target(req, "person").request().header("MyHeader", "CanReadHeaderFromContextInjection").get();
        assertEquals(200, response.getStatus());
        compareJSON(pattern, response.readEntity(String.class));
    }


    private void compareJSON(String expected, String actual) throws Exception {
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
            + "/jsonbContextResolverApp/rest/person/";
        return client.target(base + path);
    }
}
