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
package com.ibm.ws.jaxrs.fat.paramconverter;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.After;
import org.junit.Test;

import com.ibm.ws.jaxrs.fat.paramconverter.objects.TestListObject;
import com.ibm.ws.jaxrs.fat.paramconverter.objects.TestObject;

import componenttest.app.FATServlet;

@WebServlet(urlPatterns = "/ClientTestServlet")
public class ClientTestServlet extends FATServlet {

    private static final long serialVersionUID = 4563445389586844836L;

    final static String URI_CONTEXT_ROOT = "http://localhost:" + Integer.getInteger("bvt.prop.HTTP_default") + "/paramconverter/";

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
    public void testStringArrayParamConverter() throws Exception {

        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("application/resource/stringarray")
                        .queryParam("ids", "a,b,c")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        String[] actual = response.readEntity(String[].class);

        Object expected = Array.newInstance(String.class, 3);
        ((String[]) expected)[0] = "a";
        ((String[]) expected)[1] = "b";
        ((String[]) expected)[2] = "c";

        for (int i = 0; i < 3; i++) {
            assertEquals("expected: " + ((String[]) expected)[i] + " actual: " + actual[i], ((String[]) expected)[i], actual[i]);
        }
    }

    @Test
    public void testStringListParamConverter() throws Exception {

        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("application/resource/stringlist")
                        .queryParam("ids", "a,b,c")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        List<String> actual = response.readEntity(new GenericType<List<String>>() {});

        List<String> expected = new ArrayList<String>();
        expected.add("a");
        expected.add("b");
        expected.add("c");

        assertEquals("expected: " + expected + " actual: " + actual, expected, actual);
    }

    @Test
    public void testStringSetParamConverter() throws Exception {

        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("application/resource/stringset")
                        .queryParam("ids", "a,b,c")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        Set<String> actual = response.readEntity(new GenericType<Set<String>>() {});

        Set<String> expected = new HashSet<String>();
        expected.add("a");
        expected.add("b");
        expected.add("c");

        assertEquals("expected: " + expected + " actual: " + actual, expected, actual);
    }

    @Test
    public void testStringSortedSetParamConverter() throws Exception {

        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("application/resource/stringset")
                        .queryParam("ids", "a,b,c")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        Set<String> actual = response.readEntity(new GenericType<SortedSet<String>>() {});

        Set<String> expected = new TreeSet<String>();
        expected.add("a");
        expected.add("b");
        expected.add("c");

        assertEquals("expected: " + expected + " actual: " + actual, expected, actual);
    }

    @Test
    public void testStringMapParamConverter() throws Exception {

        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("application/resource/stringmap")
                        .queryParam("ids", "overwrittenbyconverter")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        Map<String, String> actual = response.readEntity(new GenericType<Map<String, String>>() {});

        Map<String, String> expected = new HashMap<String, String>();
        expected.put("a", "3");
        expected.put("b", "1");
        expected.put("c", "2");

        assertEquals("expected: " + expected + " actual: " + actual, expected, actual);
    }

    @Test
    public void testMultiParameters() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("application/resource/multiparam")
                        .queryParam("list", "a,b,c")
                        .queryParam("set", "1,2,3")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        String expected = "[a, b, c],[1, 2, 3]";
        String actual = response.readEntity(String.class);
        assertEquals("expected: " + expected + " actual: " + actual, expected, actual);
    }

    // negative test when user mis specifies the @QueryParam(id), object should come back null, shouldn't blow up code
    @Test
    public void testBadQueryParamId() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("application/resource/stringset")
                        .queryParam("bad", "a,b,c")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        String expected = "";
        String actual = response.readEntity(String.class);
        assertEquals("expected: " + expected + " actual: " + actual, expected, actual);
    }

    @Test
    public void testObject() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("application/resource/testobject")
                        .queryParam("object", "testobject")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        TestObject actual = response.readEntity(TestObject.class);
        String expected = "testobject";
        assertEquals("expected: " + expected + " actual: " + actual.content, expected, actual.content);
    }

    @Test
    public void testListObject() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("application/resource/testlistobject")
                        .queryParam("object", "a,b,c")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get();
        assertEquals(200, response.getStatus());
        TestListObject actual = response.readEntity(TestListObject.class);

        TestListObject expected = new TestListObject();
        expected.add("a");
        expected.add("b");
        expected.add("c");

        assertEquals("expected: " + expected + " actual: " + actual, expected, actual);
    }

    @Test
    public void testNoPublicConstructorObject() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("application/resource/nopublicconstructorobject")
                        .queryParam("object", "nopublicconstructor")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get();
        assertEquals(200, response.getStatus());

        // return a string because jaxrs-2.1 blows up trying to read the json object with a private constructor
        String actual = response.readEntity(String.class);
        String expected = "nopublicconstructor";
        assertEquals("expected: " + expected + " actual: " + actual, expected, actual);
    }

    @Test
    public void testNoPublicConstructorListObject() throws Exception {
        Response response = client.target(URI_CONTEXT_ROOT)
                        .path("application/resource/nopublicconstructorlistobject")
                        .queryParam("object", "a,b,c")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get();
        assertEquals(200, response.getStatus());

        // return a string because jaxrs-2.1 blows up trying to read the json object with a private constructor
        String actual = response.readEntity(String.class);
        String expected = "abc";
        assertEquals("expected: " + expected + " actual: " + actual, expected, actual);
    }

}