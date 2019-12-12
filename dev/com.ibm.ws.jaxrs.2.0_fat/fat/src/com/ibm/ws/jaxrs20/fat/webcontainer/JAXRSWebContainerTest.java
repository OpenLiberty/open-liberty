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
package com.ibm.ws.jaxrs20.fat.webcontainer;

import static com.ibm.ws.jaxrs20.fat.TestUtils.asString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.ConnectException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Simple tests for web container spec compliance.
 */
@RunWith(FATRunner.class)
public class JAXRSWebContainerTest {

    @Server("com.ibm.ws.jaxrs.fat.webcontainer")
    public static LibertyServer server;

    private static final String webwar = "webcontainer";

    private static HttpClient httpClient;

    @BeforeClass
    public static void setUp() throws Exception {
        ShrinkHelper.defaultDropinApp(server, webwar, "com.ibm.ws.jaxrs.fat.webcontainer");

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
            server.stopServer("CWWKW0100W", "SRVE8094W", "SRVE8115W");
        }
    }

    @Before
    public void getHttpClient() {
        httpClient = new DefaultHttpClient();
    }

    @After
    public void resetHttpClient() {
        httpClient.getConnectionManager().shutdown();
    }

    private static int getPort() {
        return server.getHttpDefaultPort();
    }

    private String getWebContainerTestURI() {
        return "http://localhost:" + getPort() + "/webcontainer/wctests/environment/webcontainer/context";
    }

    /**
     * Tests that a HTTPServletRequest can be injected.
     *
     * @throws Exception
     */
    @Test
    public void testHTTPServletRequestInjection() throws Exception {
        HttpGet get = new HttpGet(getWebContainerTestURI());
        HttpResponse resp = null;
        try {
            resp = httpClient.execute(get);
        } catch (ConnectException e) {
            // This test fails intermittently with a Connection refused exception, restarting the server to retry the request.
            server.stopServer();
            server.startServer(true);
            resp = httpClient.execute(get);
        }

        String content = asString(resp);
        System.out.println("testHTTPServletRequestInjection response = " + content);

        // TODO: tWAS test makes use of web filter, but not sure
        // if that would work here. Investigate later.

        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertTrue("HTTP response should be " + getWebContainerTestURI(), getWebContainerTestURI().endsWith(content));
    }

    /**
     * Tests that an injected HTTPServletResponse can take over the response
     * instead of further processing by the runtime engine.
     *
     * @throws Exception
     */
    @Mode(TestMode.FULL)
    @Test
    public void testHTTPServletResponseInjection() throws Exception {
        HttpPost post = new HttpPost(getWebContainerTestURI());
        HttpResponse resp = null;
        try {
            resp = httpClient.execute(post);
        } catch (ConnectException e) {
            // This test fails intermittently with a Connection refused exception, restarting the server to retry the request.
            server.stopServer();
            server.startServer(true);
            resp = httpClient.execute(post);
        }
        String content = asString(resp);
        System.out.println("testHTTPServletResponseInjection response = " + content);

        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("responseheadervalue", resp.getFirstHeader("responseheadername").getValue());
        assertEquals("HTTP response should be \"Hello World -- I was committted\"", "Hello World -- I was committted", content);

    }

    /**
     * Tests that a ServletContext can be injected.
     *
     * @throws Exception
     */
    @Test
    public void testServletContextInjection() throws Exception {
        HttpGet get = new HttpGet(getWebContainerTestURI() + "/servletcontext");
        HttpResponse resp = null;
        try {
            resp = httpClient.execute(get);
        } catch (ConnectException e) {
            // This test fails intermittently with a Connection refused exception, restarting the server to retry the request.
            server.stopServer();
            server.startServer(true);
            resp = httpClient.execute(get);
        }
        String content = asString(resp);
        System.out.println("testServletContextInjection response = " + content.intern());

        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertTrue("HTTP response should contain \"testing 1-2-3\"", content.contains("testing 1-2-3"));
    }

    /**
     * Tests that a ServletConfig can be injected.
     *
     * @throws Exception
     */
    @Test
    public void testServletConfigInjection() throws Exception {
        HttpGet get = new HttpGet(getWebContainerTestURI() + "/servletconfig");
        HttpResponse resp = null;
        try {
            resp = httpClient.execute(get);
        } catch (ConnectException e) {
            // This test fails intermittently with a Connection refused exception, restarting the server to retry the request.
            server.stopServer();
            server.startServer(true);
            resp = httpClient.execute(get);
        }
        String content = asString(resp);
        System.out.println("testServletConfigInjection response = " + content);

        assertEquals(200, resp.getStatusLine().getStatusCode());
        assertEquals("HTTP response should be \"WebContainerTests\"", "WebContainerTests", content);
    }

}
