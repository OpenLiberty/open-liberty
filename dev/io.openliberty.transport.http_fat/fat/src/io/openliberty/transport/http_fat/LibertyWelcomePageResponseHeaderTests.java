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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;
import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Test to verify Content-Security-Protocol header is added in the response
 * headers on the liberty default welcome page
 */
@RunWith(FATRunner.class)
public class LibertyWelcomePageResponseHeaderTests {
    private static final Class<?> c = LibertyWelcomePageResponseHeaderTests.class;
    private static final Logger LOG = Logger.getLogger(c.getName());

    @Server("ContentSecurityProtocolHeader")
    public static LibertyServer server;

    /**
     *
     * 
     * @throws Exception
     */
    @BeforeClass
    public static void setup() throws Exception {
        // Start the server and wait for it to be ready
        server.startServer();
        // ensure server has started.
        server.waitForStringInLog("CWWKF0011I:.*");
    }

    /**
     * shut down the test server
     * 
     * @throws Exception
     */
    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test that checks if the liberty default welcome page returns the
     * Content-Security-Protocol header in the
     * response
     */
    @Test
    public void testCSPHeadersOnDefaultWelcomePage() throws Exception {
        String contentSecurityProtocolHeader = getCSPResposeHeaders();
        assertNotNull("Content-Security-Protocol header was not set", contentSecurityProtocolHeader);
        String csp = "default-src 'self'; script-src 'self' 'unsafe-inline' https://openliberty.io https://public.dhe.ibm.com; style-src 'self' 'unsafe-inline';";
        assertEquals("Correct Content-Security-Protocol header was not found", csp,
                contentSecurityProtocolHeader.trim());

        Log.info(c, "testCSPHeadersOnDefaultWelcomePage",
                "Successfully verified Content-Security-Policy header on the default welcome page: "
                        + contentSecurityProtocolHeader);
    }

    /**
     * Helper method to get the Content-Security-Protocol header from a response
     * 
     * @return The Content-Security-Policy header value
     * @throws Exception If an error occurs
     */
    private String getCSPResposeHeaders() throws Exception {
        HttpURLConnection con = getConnection();
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();

        assertEquals("Did not get the expected 200 OK response code from the Welcome Page", HttpURLConnection.HTTP_OK,
                responseCode);

        String contentSecurityProtocolHeader = con.getHeaderField("Content-Security-Policy");

        Log.info(c, "getCSPResposeHeaders", "CSP Header: " + contentSecurityProtocolHeader);

        return contentSecurityProtocolHeader;
    }

    /**
     * Creates an HttpURLConnection to the specified path
     * 
     * @return An HttpURLConnection
     * @throws IOException If an error occurs
     */
    private HttpURLConnection getConnection() throws IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        return con;
    }

}
