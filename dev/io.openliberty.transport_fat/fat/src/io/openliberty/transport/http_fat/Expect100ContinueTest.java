/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http_fat;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Test to ensure the Expect 100 continue behavior does not regress.
 */
@RunWith(FATRunner.class)
public class Expect100ContinueTest {

    private static final String CLASS_NAME = Expect100ContinueTest.class.getName();
    static final Logger LOG = Logger.getLogger(CLASS_NAME);

    private static final int LOG_SEARCH_TIMEOUT = 10000;

    @Server("Expect100ContinueResponse")
    public static LibertyServer server;

    // Reuse the ContentTypeResponseHeaderTests application for getting data
    private static final String TEST_APP = "contentTypeApp";
    private static final String TEST_APP_CONTEXT_ROOT = TEST_APP;

    @BeforeClass
    public static void setup() throws Exception {
        // Create a simple web application with test resources
        ShrinkHelper.defaultApp(server, TEST_APP, "io.openliberty.transport.http_fat.contentypetest.servlets");

        // Make sure the apps are in the server before starting it
        server.addInstalledAppForValidation(TEST_APP);

        // Start the server and wait for it to be ready
        server.startServer();
        // ensure app has started.
        server.waitForStringInLog("CWWKT0016I:.*" + TEST_APP + ".*");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        // Stop the server
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Before the tests, we need to ensure the end of mark is at the end of the logs
     * so that there are no repeated log messages while verifying test logic.
     *
     * @throws Exception
     */
    @Before
    public void beforeTest() throws Exception {
        server.setMarkToEndOfLog();
        server.setTraceMarkToEndOfDefaultTrace();
    }

    /**
     * This tests verifies that by sending an expect 100 continue headers that
     * the server will properly respond with a 100 continue and will also send
     * the response as well.
     *
     * @throws Exception
     */
    @Test
    public void test100Continue() throws Exception {
        byte[] data = "Hello World!".getBytes();
        HttpURLConnection con = buildExpectContinueRequest();
        con.getOutputStream().write(data);
        con.getOutputStream().flush();

        // Ensure we get the final response code before verifying logs to ensure 100 continue response was properly logged.
        int responseCode = con.getResponseCode();
        LOG.info("test100Continue -> Response code received: " + responseCode);
        assertTrue("Unexpected response code: " + responseCode, responseCode == HttpURLConnection.HTTP_OK);
        // Ensure we find the message "Request contains [Expect: 100-continue]"
        assertNotNull("We were expecting to find a request with Expect: 100-continue but did not!", server.waitForStringInTraceUsingMark("Request contains [Expect: 100-continue]"));
    }


    /**
     * This tests verifies that by sending an expect 100 continue headers with
     * an invalid header, the server will not respond with a 100 continue and
     * will treat it as a bad request.
     * 
     * Allowed FFDC is added because during Netty parsing, an IllegalArgumentException
     * is generated from the Netty code even though the behavior between Netty and
     * Legacy is the same.
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC("java.lang.IllegalArgumentException")
    public void test100ContinueInvalidRequest() throws Exception {
        byte[] data = "Hello World!".getBytes();
        HttpURLConnection con = buildExpectContinueRequest();
        con.setRequestProperty("Invalid@Name", "ShouldFail");
        con.getOutputStream().write(data);
        con.getOutputStream().flush();

        int responseCode = con.getResponseCode();
        LOG.info("test100Continue -> Response code received: " + responseCode);
        assertTrue("Unexpected response code: " + responseCode, responseCode == HttpURLConnection.HTTP_BAD_REQUEST);
        // Ensure we do NOT find the message "Request contains [Expect: 100-continue]"
        assertNull("Found a Expect: 100-continue request but should not have been processed!", server.waitForStringInTraceUsingMark("Request contains [Expect: 100-continue]", LOG_SEARCH_TIMEOUT));
    }

    private HttpURLConnection buildExpectContinueRequest() throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + TEST_APP_CONTEXT_ROOT + "/test.html");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Expect", "100-Continue");
        con.setDoOutput(true);
        return con;
    }

}
