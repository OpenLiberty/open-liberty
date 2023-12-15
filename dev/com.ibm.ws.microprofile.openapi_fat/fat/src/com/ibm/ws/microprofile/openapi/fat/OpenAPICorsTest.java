/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.microprofile.openapi.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.microprofile.openapi.fat.utils.OpenAPIConnection;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;

/**
 *
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OpenAPICorsTest {
    private static final String SERVER_NAME = "CorsServer";
    private static String REQUEST_HEADER_ORIGIN = "Origin";
    private static String REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS = "Access-Control-Request-Headers";
    private static String REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD = "Access-Control-Request-Method";
    private static String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";
    private static String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static String RESPONSE_HEADER_ACCESS_CONTROL_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    private static String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    @Server(SERVER_NAME)
    public static LibertyServer server;

    @ClassRule
    public static RepeatTests r = FATSuite.defaultRepeat(SERVER_NAME);

    @BeforeClass
    public static void setUp() throws Exception {
        server.startServer();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        server.stopServer();
    }

    @Test
    public void testEndpoint() throws Exception {
        // Check that the /openapi endpoint is available.
        final OpenAPIConnection conn = OpenAPIConnection.openAPIDocsConnection(server, false);
        checkConnectionIsOK(conn);
    }

    @Test
    public void testHeadersCorrectEndpoint() throws Exception {
        // test CORS request to "http://localhost:8010/openapi" which is in CORS domain
        OpenAPIConnection conn = OpenAPIConnection.openAPIDocsConnection(server, false);
        conn.header(REQUEST_HEADER_ORIGIN, "openliberty.io");
        HttpURLConnection response = conn.getConnection();

        assertEquals(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN + " response header is incorrect or missing",
            "openliberty.io", response.getHeaderField(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS + " response header is incorrect or missing",
            "true", response.getHeaderField(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS));
        assertEquals(RESPONSE_HEADER_ACCESS_CONTROL_EXPOSE_HEADERS + " response header is incorrect or missing",
            "TestHeader", response.getHeaderField(RESPONSE_HEADER_ACCESS_CONTROL_EXPOSE_HEADERS));
    }

    @Test
    public void testHeadersIncorrectEndpoint() throws Exception {
        // test CORS request to "http://localhost:8010/" which is not in CORS domain
        Map<String, String> header = new HashMap<>();
        header.put(REQUEST_HEADER_ORIGIN, "openliberty.io");
        URL url = HttpUtils.createURL(server, "/");

        HttpURLConnection response = HttpUtils.getHttpConnection(url, 200, null, 30, HTTPRequestMethod.GET, header,
            null);
        response.setReadTimeout(30 * 1000);

        checkAbsentResponseHeaders(response);
    }

    @Test
    public void testIncorrectOriginHeader() throws Exception {
        // test CORS request with origin header that is not allowed
        OpenAPIConnection conn = OpenAPIConnection.openAPIDocsConnection(server, false);
        conn.header(REQUEST_HEADER_ORIGIN, "incorrectorigin.com");
        HttpURLConnection response = conn.getConnection();

        checkAbsentResponseHeaders(response);
    }

    @Test
    public void testPreflightCorsRequest() throws Exception {
        OpenAPIConnection conn = OpenAPIConnection.openAPIDocsConnection(server, false);
        conn.method(HTTPRequestMethod.OPTIONS);
        conn.header(REQUEST_HEADER_ORIGIN, "openliberty.io");
        conn.header(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD, "GET");
        conn.header(REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS, "TestHeader2");
        HttpURLConnection response = conn.getConnection();

        assertEquals(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN + " response header is incorrect or missing",
            "openliberty.io", response.getHeaderField(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
        assertEquals(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS + " response header is incorrect or missing",
            "true", response.getHeaderField(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS));
        checkPreflightHeaders(response);
    }

    private void checkPreflightHeaders(HttpURLConnection response) throws Exception {
        assertEquals(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS + " response header is incorrect or missing",
            "GET, OPTIONS", response.getHeaderField(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS));
        assertEquals(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS + " response header is incorrect or missing",
            "TestHeader2", response.getHeaderField(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS));
    }

    private void checkAbsentResponseHeaders(HttpURLConnection response) throws Exception {
        // checks for the absence of CORS response headers from invalid requests
        assertFalse(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN + " should not be present in response headers",
            response.getHeaderFields().containsKey(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN));
        assertFalse(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS + " should not be present in response headers",
            response.getHeaderFields().containsKey(RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS));
        assertFalse(RESPONSE_HEADER_ACCESS_CONTROL_EXPOSE_HEADERS + " should not be present in response headers",
            response.getHeaderFields().containsKey(RESPONSE_HEADER_ACCESS_CONTROL_EXPOSE_HEADERS));
    }

    private void checkConnectionIsOK(OpenAPIConnection c)
        throws Exception {
        c.expectedResponseCode(HttpURLConnection.HTTP_OK).getConnection();
    }

}
