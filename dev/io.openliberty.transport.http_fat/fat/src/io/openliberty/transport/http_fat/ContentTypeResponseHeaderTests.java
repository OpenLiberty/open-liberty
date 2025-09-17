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
 * Test to verify Content-type mapping for various file extensions
 */
@RunWith(FATRunner.class)
public class ContentTypeResponseHeaderTests {
    private static final Class<?> c = ContentTypeResponseHeaderTests.class;
    private static final Logger LOG = Logger.getLogger(c.getName());

    @Server("ContentTypeResponseHeader")
    public static LibertyServer server;

    private static final String TEST_APP = "contentTypeApp";
    private static final String TEST_APP_CONTEXT_ROOT = TEST_APP;

    /**
     *
     * 
     * @throws Exception
     */
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
     * Test that checks if favicon.ico returns the proper Content-type header in the
     * response
     */
    @Test
    public void testFaviconMimeType() throws Exception {
        // Test favicon.ico MIME type
        String faviconContentType = getContentType(TEST_APP_CONTEXT_ROOT + "/favicon.ico");
        assertEquals("Incorrect MIME type for favicon.ico", "image/x-icon", faviconContentType);

        Log.info(c, "testFaviconMimeType", "Successfully verified favicon.ico MIME type: " + faviconContentType);
    }

    /**
     * Test to verify MIME types for various common file extensions
     */
    @Test
    public void testCommonMimeTypes() throws Exception {
        // Define expected MIME types
        Map<String, String> expectedMimeTypes = new HashMap<>();
        expectedMimeTypes.put("/test.html", "text/html");
        expectedMimeTypes.put("/test.css", "text/css");
        expectedMimeTypes.put("/test.js", "application/x-javascript");
        expectedMimeTypes.put("/test.ico", "image/x-icon");
        expectedMimeTypes.put("/test.xml", "text/xml");
        expectedMimeTypes.put("/test.json", "application/json");

        // Test each file extension
        for (Map.Entry<String, String> entry : expectedMimeTypes.entrySet()) {
            String path = entry.getKey();
            String expectedMimeType = entry.getValue();

            String actualMimeType = getContentType(TEST_APP_CONTEXT_ROOT + path);
            assertEquals("Incorrect MIME type for " + path, expectedMimeType, actualMimeType);

            Log.info(c, "testCommonMimeTypes", "Successfully verified MIME type for " + path + ": " + actualMimeType);
        }
    }

    /**
     * Helper method to get the Content-Type from a response
     * 
     * @param path The path to request
     * @return The Content-Type header value
     * @throws Exception If an error occurs
     */
    private String getContentType(String path) throws Exception {
        HttpURLConnection con = getConnection(path);
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();
        // Accept 404 responses since we're only checking headers, not content
        assertTrue("Unexpected response code: " + responseCode,
                responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_NOT_FOUND);

        String contentType = con.getHeaderField("Content-Type");

        // Some MIME types include charset, strip it for comparison
        if (contentType != null && contentType.contains(";")) {
            contentType = contentType.substring(0, contentType.indexOf(";")).trim();
        }

        return contentType;
    }

    /**
     * Creates an HttpURLConnection to the specified path
     * 
     * @param path The path to connect to
     * @return An HttpURLConnection
     * @throws IOException If an error occurs
     */
    private HttpURLConnection getConnection(String path) throws IOException {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/" + path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        return con;
    }

}
