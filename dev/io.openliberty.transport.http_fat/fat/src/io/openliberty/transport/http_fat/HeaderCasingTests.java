/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.transport.http_fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.ShrinkHelper;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests to verify HTTP header casing behavior.
 * 
 * This test verifies that:
 * 1. Standard HTTP headers use proper casing (e.g., "Content-Type" not "content-type")
 * 2. Custom headers maintain the casing from their first occurrence
 * 3. Subsequent uses of the same header (with different casing) reuse the original casing
 * 4. This behavior is consistent with CHFW (Channel Framework) behavior
 */
@RunWith(FATRunner.class)
public class HeaderCasingTests {
    private static final Class<?> c = HeaderCasingTests.class;
    private static final Logger LOG = Logger.getLogger(c.getName());

    @Server("HeaderCasing")
    public static LibertyServer server;

    private static final String TEST_APP = "HeaderCasing";
    private static final String SERVLET_PATH = "/HeaderCasing/HeaderCasingServlet";

    @BeforeClass
    public static void setup() throws Exception {
        // Create the test application
        ShrinkHelper.defaultApp(server, TEST_APP, "io.openliberty.transport.http.headercasing.servlet");

        // Make sure the app is in the server before starting it
        server.addInstalledAppForValidation(TEST_APP);

        // Start the server and wait for it to be ready
        server.startServer();
        
        // Ensure app has started
        server.waitForStringInLog("CWWKT0016I:.*" + TEST_APP + ".*");
    }

    @AfterClass
    public static void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test that standard HTTP headers use proper casing in responses.
     * Standard headers like "Content-Type" should always use the canonical casing
     * defined in HttpHeaderKeys, regardless of how they were set.
     */
    @Test
    public void testStandardHeaderCasing() throws Exception {
        LOG.info("Testing standard header casing normalization");
        
        HttpURLConnection con = getConnection(SERVLET_PATH + "?action=setStandard");
        con.setRequestMethod("GET");
        
        int responseCode = con.getResponseCode();
        assertEquals("Unexpected response code", HttpURLConnection.HTTP_OK, responseCode);
        
        // Check that Content-Type uses proper casing
        Map<String, List<String>> headers = con.getHeaderFields();
        
        // Content-Type should be present with proper casing
        boolean foundContentType = false;
        String contentTypeKey = null;
        for (String key : headers.keySet()) {
            if (key != null && key.equalsIgnoreCase("content-type")) {
                foundContentType = true;
                contentTypeKey = key;
                break;
            }
        }
        
        assertTrue("Content-Type header not found", foundContentType);
        assertEquals("Content-Type should use proper casing", "Content-Type", contentTypeKey);
        
        // Cache-Control should be present with proper casing
        boolean foundCacheControl = false;
        String cacheControlKey = null;
        for (String key : headers.keySet()) {
            if (key != null && key.equalsIgnoreCase("cache-control")) {
                foundCacheControl = true;
                cacheControlKey = key;
                break;
            }
        }
        
        assertTrue("Cache-Control header not found", foundCacheControl);
        assertEquals("Cache-Control should use proper casing", "Cache-Control", cacheControlKey);
        
        LOG.info("Standard headers verified: Content-Type=" + contentTypeKey + ", Cache-Control=" + cacheControlKey);
    }

    /**
     * Test that custom headers maintain the casing from their first occurrence.
     * When a custom header is set multiple times with different casing,
     * all occurrences should use the casing from the first time it was set.
     */
    @Test
    public void testCustomHeaderCasingConsistency() throws Exception {
        LOG.info("Testing custom header casing consistency");
        
        HttpURLConnection con = getConnection(SERVLET_PATH + "?action=setCustom");
        con.setRequestMethod("GET");
        
        int responseCode = con.getResponseCode();
        assertEquals("Unexpected response code", HttpURLConnection.HTTP_OK, responseCode);
        
        Map<String, List<String>> headers = con.getHeaderFields();
        
        // Find X-Custom-Header (should maintain first occurrence casing)
        String customHeaderKey = null;
        List<String> customHeaderValues = null;
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            String key = entry.getKey();
            if (key != null && key.equalsIgnoreCase("x-custom-header")) {
                customHeaderKey = key;
                customHeaderValues = entry.getValue();
                break;
            }
        }
        
        assertNotNull("X-Custom-Header not found", customHeaderKey);
        assertNotNull("X-Custom-Header values not found", customHeaderValues);
        
        // The header key should use the casing from the first setHeader call
        assertEquals("Custom header should use first occurrence casing", "X-CUSTOM-Header", customHeaderKey);
        
        // Should have multiple values
        assertTrue("Should have multiple header values", customHeaderValues.size() > 1);
        
        LOG.info("Custom header verified: " + customHeaderKey + " with " + customHeaderValues.size() + " values");
        for (String value : customHeaderValues) {
            LOG.info("  Value: " + value);
        }
    }

    /**
     * Test mixed standard and custom headers to ensure both follow proper casing rules.
     */
    @Test
    public void testMixedHeaderCasing() throws Exception {
        LOG.info("Testing mixed standard and custom header casing");
        
        HttpURLConnection con = getConnection(SERVLET_PATH + "?action=setMixed");
        con.setRequestMethod("GET");
        
        int responseCode = con.getResponseCode();
        assertEquals("Unexpected response code", HttpURLConnection.HTTP_OK, responseCode);
        
        Map<String, List<String>> headers = con.getHeaderFields();
        
        // Check Content-Type (standard header)
        String contentTypeKey = findHeaderKey(headers, "content-type");
        assertNotNull("Content-Type header not found", contentTypeKey);
        assertEquals("Content-Type should use standard casing", "Content-Type", contentTypeKey);
        
        // Check X-First-Custom (custom header)
        String firstCustomKey = findHeaderKey(headers, "x-first-custom");
        assertNotNull("X-First-Custom header not found", firstCustomKey);
        assertEquals("X-First-Custom should use first occurrence casing", "X-First-Custom", firstCustomKey);
        
        // Check X-Second-Custom (custom header)
        String secondCustomKey = findHeaderKey(headers, "x-second-custom");
        assertNotNull("X-Second-Custom header not found", secondCustomKey);
        assertEquals("X-Second-Custom should use first occurrence casing", "X-Second-Custom", secondCustomKey);
        
        LOG.info("Mixed headers verified:");
        LOG.info("  Standard: " + contentTypeKey);
        LOG.info("  Custom 1: " + firstCustomKey);
        LOG.info("  Custom 2: " + secondCustomKey);
    }

    /**
     * Test that request headers sent with various casings are handled correctly.
     */
    @Test
    public void testRequestHeaderCasing() throws Exception {
        LOG.info("Testing request header casing");
        
        HttpURLConnection con = getConnection(SERVLET_PATH + "?action=echo");
        con.setRequestMethod("GET");
        
        // Set custom request headers with specific casing
        con.setRequestProperty("X-Test-Header", "TestValue");
        con.setRequestProperty("X-Another-Header", "AnotherValue");
        
        int responseCode = con.getResponseCode();
        assertEquals("Unexpected response code", HttpURLConnection.HTTP_OK, responseCode);
        
        // Read the response to see what headers were received
        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
        }
        reader.close();
        
        String responseText = response.toString();
        LOG.info("Request headers echoed:\n" + responseText);
        
        // Verify our custom headers are present in the echo
        assertTrue("X-Test-Header should be in response",
                   responseText.contains("X-Test-Header") || responseText.contains("x-test-header"));
        assertTrue("X-Another-Header should be in response",
                   responseText.contains("X-Another-Header") || responseText.contains("x-another-header"));
    }

    /**
     * Test that popular standard HTTP headers maintain proper casing when set with various casings.
     * This test verifies the most commonly used headers normalize to their canonical form.
     */
    @Test
    public void testPopularHeadersCasing() throws Exception {
        LOG.info("Testing popular HTTP headers casing normalization");
        
        HttpURLConnection con = getConnection(SERVLET_PATH + "?action=setPopularHeaders");
        con.setRequestMethod("GET");
        
        int responseCode = con.getResponseCode();
        assertEquals("Unexpected response code", HttpURLConnection.HTTP_OK, responseCode);
        
        Map<String, List<String>> headers = con.getHeaderFields();
        
        // Test the most popular headers with their expected canonical casing
        String[] popularHeaders = {
            "Content-Type",
            "Content-Length",
            "Cache-Control",
            "Authorization",
            "Accept",
            "Accept-Encoding",
            "User-Agent",
            "Host"
        };
        
        for (String expectedCasing : popularHeaders) {
            String actualKey = findHeaderKey(headers, expectedCasing);
            if (actualKey != null) {
                assertEquals("Header " + expectedCasing + " should use proper casing",
                           expectedCasing, actualKey);
                LOG.info("Verified header casing: " + actualKey);
            } else {
                LOG.info("Header " + expectedCasing + " not present in response (optional)");
            }
        }
    }

    /**
     * Helper method to find a header key by case-insensitive name.
     */
    private String findHeaderKey(Map<String, List<String>> headers, String headerName) {
        for (String key : headers.keySet()) {
            if (key != null && key.equalsIgnoreCase(headerName)) {
                return key;
            }
        }
        return null;
    }

    /**
     * Creates an HttpURLConnection to the specified path.
     */
    private HttpURLConnection getConnection(String path) throws Exception {
        URL url = new URL("http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        return con;
    }
}

