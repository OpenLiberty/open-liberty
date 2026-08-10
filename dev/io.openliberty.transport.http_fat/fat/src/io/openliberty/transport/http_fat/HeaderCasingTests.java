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

import componenttest.annotation.ExpectedFFDC;
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
     * Test edge cases for setHeader and addHeader methods.
     * This test validates that both Netty and CHFW implementations handle edge cases identically:
     * - null header names/values
     * - empty string header names/values
     * - whitespace-only header names/values
     * - special characters in header names
     * - newlines in header values
     * - very long header values
     */
    @Test
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    public void testHeaderEdgeCases() throws Exception {
        LOG.info("Testing header edge cases");
        
        HttpURLConnection con = getConnection(SERVLET_PATH + "?action=testEdgeCases");
        con.setRequestMethod("GET");
        
        int responseCode = con.getResponseCode();
        assertEquals("Unexpected response code", HttpURLConnection.HTTP_OK, responseCode);
        
        // Read the response to see the edge case test results
        BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line).append("\n");
        }
        reader.close();
        
        String responseText = response.toString();
        LOG.info("Edge case test results:\n" + responseText);
        
        // Validate expected behaviors based on both implementations
        
        // Test 1: setHeader with null header name - No exception (both impls)
        assertTrue("Test 1 should show no exception for null header name with setHeader",
                   responseText.contains("Test 1:") && responseText.contains("No exception thrown"));
        
        // Test 2: setHeader with null header value - No exception (both impls)
        assertTrue("Test 2 should show no exception for null header value with setHeader",
                   responseText.contains("Test 2:") && responseText.contains("No exception thrown"));
        
        // Test 3: addHeader with null header name - NullPointerException (both impls)
        if(componenttest.rules.repeater.JakartaEEAction.isEE11OrLaterActive()) {
            assertTrue("Test 3 should show NullPointerException for null header name with addHeader",
                    responseText.contains("Test 3:") && responseText.contains("No exception thrown"));
        } else {
            assertTrue("Test 3 should show NullPointerException for null header name with addHeader",
                    responseText.contains("Test 3:") && responseText.contains("NullPointerException"));
        }
        
        // Test 4: addHeader with null header value - IllegalArgumentException (both impls)
        assertTrue("Test 4 should show IllegalArgumentException for null header value with addHeader",
                   responseText.contains("Test 4:") && responseText.contains("IllegalArgumentException"));
        
        // Test 5: setHeader with empty string header name - StringIndexOutOfBoundsException (both impls)
        assertTrue("Test 5 should show StringIndexOutOfBoundsException for empty header name with setHeader",
                   responseText.contains("Test 5:") && responseText.contains("StringIndexOutOfBoundsException"));
        
        // Test 6: setHeader with empty string header value - No exception (both impls)
        assertTrue("Test 6 should show no exception for empty header value with setHeader",
                   responseText.contains("Test 6:") && responseText.contains("No exception thrown"));
        
        // Test 7: addHeader with empty string header name - StringIndexOutOfBoundsException (both impls)
        assertTrue("Test 7 should show StringIndexOutOfBoundsException for empty header name with addHeader",
                   responseText.contains("Test 7:") && responseText.contains("StringIndexOutOfBoundsException"));
        
        // Test 8: addHeader with empty string header value - No exception (both impls)
        assertTrue("Test 8 should show no exception for empty header value with addHeader",
                   responseText.contains("Test 8:") && responseText.contains("No exception thrown"));
        
        // Test 9: setHeader with whitespace-only header name - IllegalArgumentException (both impls)
        assertTrue("Test 9 should show IllegalArgumentException for whitespace header name",
                   responseText.contains("Test 9:") && responseText.contains("IllegalArgumentException"));
        
        // Test 10: setHeader with whitespace-only header value - No exception (both impls)
        assertTrue("Test 10 should show no exception for whitespace header value",
                   responseText.contains("Test 10:") && responseText.contains("No exception thrown"));
        
        // Test 11: setHeader with special characters in header name - IllegalArgumentException (both impls)
        assertTrue("Test 11 should show IllegalArgumentException for special chars in header name",
                   responseText.contains("Test 11:") && responseText.contains("IllegalArgumentException"));
        
        // Test 12: setHeader with newline in header value - IllegalArgumentException (both impls)
        assertTrue("Test 12 should show IllegalArgumentException for newline in header value",
                   responseText.contains("Test 12:") && responseText.contains("IllegalArgumentException"));
        
        // Test 13: setHeader with very long header value - No exception (both impls)
        assertTrue("Test 13 should show no exception for very long header value",
                   responseText.contains("Test 13:") && responseText.contains("No exception thrown"));
        
        // Test 14: setHeader with both null name and value - No exception (both impls)
        assertTrue("Test 14 should show no exception for both null name and value",
                   responseText.contains("Test 14:") && responseText.contains("No exception thrown"));
        
        // Test 15: addHeader with both empty string name and value - StringIndexOutOfBoundsException (both impls)
        assertTrue("Test 15 should show StringIndexOutOfBoundsException for both empty strings",
                   responseText.contains("Test 15:") && responseText.contains("StringIndexOutOfBoundsException"));
        
        LOG.info("All edge case validations passed - both implementations behave identically");
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

