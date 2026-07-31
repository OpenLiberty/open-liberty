/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.webcontainer.servlet31.fat.tests;

import static org.junit.Assert.assertEquals;

import java.util.logging.Logger;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests for the WebContainer maxParamPerRequest configuration attribute.
 *
 * The server.xml configures {@code <webContainer maxParamPerRequest="3"/>}.
 * Requests with a parameter count at or below the limit must return HTTP 200.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class WCMaxParamPerRequestTest {

    private static final Logger LOG = Logger.getLogger(WCMaxParamPerRequestTest.class.getName());

    private static final String APP_NAME = "MaxParamPerRequest";
    private static final String CONTEXT_ROOT = "/" + APP_NAME;
    private static final String SERVLET_PATH = "/MaxParamServlet";

    @Server("servlet31_maxParamPerRequestServer")
    public static LibertyServer server;

    @BeforeClass
    public static void setup() throws Exception {
        ShrinkHelper.defaultDropinApp(server, APP_NAME + ".war",
                                      "com.ibm.ws.webcontainer.servlet_31_fat.maxparam");

        server.startServer(WCMaxParamPerRequestTest.class.getSimpleName() + ".log");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            // SRVE0325E: Exceeding maximum parameters allowed per request 3 ,current 4 , cannot add more. 
            // SRVE0777E: Exception thrown by application class 'com.ibm.wsspi.webcontainer.util.RequestUtils.parseQueryString:670' 
            server.stopServer("SRVE0777E", "SRVE0325E");
        }
    }

    /**
     * Sends a request with one parameter (below the limit of 3).
     * Expects HTTP 200.
     */
    @Test
    public void testBelowParamLimit() throws Exception {
        LOG.info("testBelowParamLimit: sending 1 query parameter (limit=3)");
        assertResponseCode(200, "name1=value1");
    }

    /**
     * Sends a request with four parameters (above the limit of 3).
     * Expects HTTP 500.
     */
    @Test
    @ExpectedFFDC("java.lang.IllegalStateException")
    public void testAboveParamLimit() throws Exception {
        LOG.info("testAboveParamLimit: sending 2 query parameters (limit=3)");
        assertResponseCode(500, "name1=value1&name2=value2&name3=value3&name4=value4");
    }

    /**
     * Sends a request with exactly three parameters (equal to the limit of 3).
     * Expects HTTP 200.
     */
    @Test
    public void testAtParamLimit() throws Exception {
        LOG.info("testAtParamLimit: sending 3 query parameters (limit=3)");
        assertResponseCode(200, "name1=value1&name2=value2&name3=value3");
    }

    // -------------------------------------------------------------------------

    private void assertResponseCode(int expectedCode, String queryString) throws Exception {
        String url = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort()
                     + CONTEXT_ROOT + SERVLET_PATH + "?" + queryString;

        LOG.info("GET " + url);

        HttpClient client = new HttpClient();
        GetMethod get = new GetMethod(url);
        try {
            int responseCode = client.executeMethod(get);
            String responseBody = get.getResponseBodyAsString();
            LOG.info("Response code : " + responseCode);
            LOG.info("Response body : " + responseBody);

            assertEquals("Unexpected HTTP response code for query string [" + queryString + "]",
                         expectedCode, responseCode);
        } finally {
            get.releaseConnection();
        }
    }
}
