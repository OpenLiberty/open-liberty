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
package com.ibm.ws.jaxrs20.fat.servletcoexist;

import static com.ibm.ws.jaxrs20.fat.TestUtils.asString;
import static com.ibm.ws.jaxrs20.fat.TestUtils.getBaseTestUri;
import static org.junit.Assert.assertEquals;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

@RunWith(FATRunner.class)
@SkipForRepeat("EE9_FEATURES") // currently broken due to multiple issues
public class JAXRSServletCoexistTest {
    private static final String bvwar = "servletcoexist";
    private static final String bvwar1 = bvwar + "1";
    private static final String bvwar2 = bvwar + "2";
    private static final String bvwar3 = bvwar + "3";
    private static final String bvwar4 = bvwar + "4";

    private static HttpClient client;

    @Server("com.ibm.ws.jaxrs.fat." + bvwar)
    public static LibertyServer server;

    private final Class<?> c = JAXRSServletCoexistTest.class;

    private static String username = "jordan";
    private static String group = "wasdev";

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, bvwar1, "com.ibm.ws.jaxrs.fat.servletcoexist1");
        ShrinkHelper.defaultDropinApp(server, bvwar2, "com.ibm.ws.jaxrs.fat.servletcoexist2");
        ShrinkHelper.defaultDropinApp(server, bvwar3, "com.ibm.ws.jaxrs.fat.servletcoexist3",
                                      "com.ibm.ws.jaxrs.fat.servletcoexist4");

        // Make sure we don't fail because we try to start an
        // already started server
        try {
            server.startServer(true);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null) {
            server.stopServer();
        }
    }

    @Before
    public void getHttpClient() {
        client = new DefaultHttpClient();
    }

    @After
    public void resetHttpClient() {
        client.getConnectionManager().shutdown();
    }

    @Test
    public void testServletCoexist1Application_Rest() throws Exception {
        String uri = getBaseTestUri(bvwar1, bvwar1, "/users/" + username);
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        String responseBody = asString(resp);
        assertEquals(bvwar1 + " getUserById is called, id is " + username, responseBody);
    }

    @Test
    public void testServletCoexist1Application_WebServlet() throws Exception {
        String uri = getBaseTestUri(bvwar1, "webservlet");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        String responseBody = asString(resp);
        assertEquals("Hello this is webservlet1", responseBody.trim());
    }

    @Test
    public void testServletCoexist1Application_AnnServlet() throws Exception {
        String uri = getBaseTestUri(bvwar1, "annservlet");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        String responseBody = asString(resp);
        assertEquals("Hello this is annservlet1", responseBody.trim());
    }

    @Test
    public void testServletCoexist2IBMRestServlet_RestClass() throws Exception {
        String uri = getBaseTestUri(bvwar2, bvwar2, "/users/" + username);
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        String responseBody = asString(resp);
        assertEquals(bvwar2 + " getUserById is called, id is " + username, responseBody);
    }

    @Test
    public void testServletCoexist2IBMRestServlet_RestSingleton() throws Exception {
        String uri = getBaseTestUri(bvwar2, bvwar2, "/groups/" + group);
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        String responseBody = asString(resp);
        assertEquals(bvwar2 + " getGroupById is called, id is " + group, responseBody);
    }

    @Test
    public void testServletCoexist2IBMRestServlet_WebServlet() throws Exception {
        String uri = getBaseTestUri(bvwar2, "webservlet");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        String responseBody = asString(resp);
        assertEquals("Hello this is webservlet2", responseBody.trim());
    }

    @Test
    public void testServletCoexist2IBMRestServlet_AnnServlet() throws Exception {
        String uri = getBaseTestUri(bvwar2, "annservlet");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        String responseBody = asString(resp);
        assertEquals("Hello this is annservlet2", responseBody.trim());
    }

    @Test
    public void testServletCoexist3CustomApplication_RestClass() throws Exception {
        String uri = getBaseTestUri(bvwar3, bvwar3, "/users/" + username);
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        String responseBody = asString(resp);
        assertEquals(bvwar3 + " getUserById is called, id is " + username, responseBody);
    }

    @Test
    public void testServletCoexist3CustomApplication_RestSingleton() throws Exception {
        String uri = getBaseTestUri(bvwar3, bvwar3, "/groups/" + group);
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        String responseBody = asString(resp);
        assertEquals(bvwar3 + " getGroupById is called, id is " + group, responseBody);
    }

    @Test
    public void testServletCoexist3CustomApplication_WebServlet() throws Exception {
        String uri = getBaseTestUri(bvwar3, "webservlet");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        String responseBody = asString(resp);
        assertEquals("Hello this is webservlet3", responseBody.trim());
    }

    @Test
    public void testServletCoexist3CustomApplication_AnnServlet() throws Exception {
        String uri = getBaseTestUri(bvwar3, "annservlet");
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        String responseBody = asString(resp);
        assertEquals("Hello this is annservlet3", responseBody.trim());
    }

    @Test
    public void testServletCoexist4Annotation_RestClass() throws Exception {
        String uri = getBaseTestUri(bvwar3, bvwar4, "/users/" + username);
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        String responseBody = asString(resp);
        assertEquals(bvwar4 + " getUserById is called, id is " + username, responseBody);
    }

    @Test
    public void testServletCoexist4Annotation_RestSingleton() throws Exception {
        String uri = getBaseTestUri(bvwar3, bvwar4, "/groups/" + group);
        HttpGet getMethod = new HttpGet(uri);
        HttpResponse resp = client.execute(getMethod);
        String responseBody = asString(resp);
        assertEquals(bvwar4 + " getGroupById is called, id is " + group, responseBody);
    }
}