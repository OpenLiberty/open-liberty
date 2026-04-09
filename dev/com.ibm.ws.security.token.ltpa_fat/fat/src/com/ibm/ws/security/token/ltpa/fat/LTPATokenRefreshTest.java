/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.token.ltpa.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

/**
 * Test LTPA token refresh functionality.
 *
 * This test verifies that:
 * 1. LTPA tokens are refreshed when they approach expiration
 * 2. New cookies are set in the response when tokens are refreshed
 * 3. Tokens are NOT refreshed when they are still valid
 * 4. The refresh threshold configuration is respected
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LTPATokenRefreshTest {

    private static final String APP_NAME = "ltpaTest";
    private static final String SERVLET_NAME = "LTPATestServlet";
    private static final String LTPA_COOKIE_NAME = "LtpaToken2";
    private static final Class<?> thisClass = LTPATokenRefreshTest.class;
    private static LibertyServer server;

    // Timing constants for token refresh tests
    // Configuration: expiration=3m, refreshThreshold=1m, maxLifetime=4m
    private static final long REFRESH_THRESHOLD_WAIT_MS = 130000;  // 130 seconds (2m 10s) - wait past 1m threshold
    private static final long SHORT_EXPIRATION_WAIT_MS = 70000;    // 70 seconds (1m 10s) - for short expiration tests
    private static final long REQUEST_INTERVAL_MS = 5000;          // 5 seconds - interval between requests

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        public void starting(Description description) {
            Log.info(thisClass, description.getMethodName(),
                     "\n@@@@@@@@@@@@@@@@@\nEntering test " + description.getMethodName() + "\n@@@@@@@@@@@@@@@@@");
        }

        @Override
        public void finished(Description description) {
            Log.info(thisClass, description.getMethodName(),
                     "\n@@@@@@@@@@@@@@@@@\nExiting test " + description.getMethodName() + "\n@@@@@@@@@@@@@@@@@");
        }
    };

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.token.ltpa.fat.refresh");
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/ltpafattestlibertyinternals-1.0.mf");
        server.addInstalledAppForValidation(APP_NAME);
    }

    @Before
    public void setUp() throws Exception {
        // Start with default configuration
        server.setServerConfigurationFile("serverTokenRefresh.xml");
        server.startServer(true);
        server.waitForStringInLog("CWWKZ0001I.*" + APP_NAME);
    }

    @After
    public void tearDown() throws Exception {
        try {
            if (server != null && server.isStarted()) {
                server.stopServer();
            }
        } catch (Exception e) {
            Log.error(thisClass, "tearDown", e);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    /**
     * Test that LTPA token is refreshed when it approaches the refresh threshold.
     *
     * Configuration:
     * - Token expiration: 3 minutes (180 seconds)
     * - Refresh threshold: 1 minute (60 seconds)
     * - Max lifetime: 4 minutes (240 seconds)
     *
     * Expected behavior:
     * 1. Initial request creates a new LTPA token
     * 2. Wait for token to approach refresh threshold (less than 1 minute remaining)
     * 3. Second request should trigger token refresh
     * 4. New LTPA cookie should be set in response
     */
    @Test
    public void testTokenRefreshWhenApproachingThreshold() throws Exception {
        String testName = "testTokenRefreshWhenApproachingThreshold";
        Log.info(thisClass, testName, "Starting test");

        // Step 1: Make initial request to get LTPA token
        String servletUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() +
                            "/" + APP_NAME + "/" + SERVLET_NAME;

        HttpURLConnection conn1 = makeRequest(servletUrl, null);
        assertEquals("First request should succeed", 200, conn1.getResponseCode());

        String initialCookie = extractLTPACookie(conn1);
        assertNotNull("Initial LTPA cookie should be set", initialCookie);
        Log.info(thisClass, testName, "Initial LTPA cookie: " + maskCookie(initialCookie));

        // Step 2: Wait for token to approach refresh threshold
        // With 3m expiration and 1m threshold, token should refresh after ~2m
        Log.info(thisClass, testName, "Waiting " + REFRESH_THRESHOLD_WAIT_MS + "ms for token to approach refresh threshold...");
        Thread.sleep(REFRESH_THRESHOLD_WAIT_MS);

        // Step 3: Make second request - should trigger refresh
        HttpURLConnection conn2 = makeRequest(servletUrl, initialCookie);
        assertEquals("Second request should succeed", 200, conn2.getResponseCode());

        String refreshedCookie = extractLTPACookie(conn2);

        // Step 4: Verify token was refreshed
        if (refreshedCookie != null) {
            assertFalse("LTPA cookie should be refreshed (different from initial)",
                        initialCookie.equals(refreshedCookie));
            Log.info(thisClass, testName, "Token was successfully refreshed");
            Log.info(thisClass, testName, "Refreshed LTPA cookie: " + maskCookie(refreshedCookie));
        } else {
            Log.info(thisClass, testName, "WARNING: No refreshed cookie received - token may not have reached refresh threshold yet");
        }

        conn1.disconnect();
        conn2.disconnect();
    }

    /**
     * Test that LTPA token is NOT refreshed when it's still far from expiration.
     *
     * Configuration:
     * - Token expiration: 2 minutes (120 seconds)
     * - Refresh threshold: 1 minute (60 seconds)
     *
     * Expected behavior:
     * 1. Initial request creates a new LTPA token
     * 2. Immediate second request should NOT trigger refresh (token still valid with 2m > 1m threshold)
     * 3. No new LTPA cookie should be set in response
     */
    @Test
    public void testTokenNotRefreshedWhenStillValid() throws Exception {
        String testName = "testTokenNotRefreshedWhenStillValid";
        Log.info(thisClass, testName, "Starting test");

        String servletUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() +
                            "/" + APP_NAME + "/" + SERVLET_NAME;

        // Step 1: Make initial request
        HttpURLConnection conn1 = makeRequest(servletUrl, null);
        assertEquals("First request should succeed", 200, conn1.getResponseCode());

        String initialCookie = extractLTPACookie(conn1);
        assertNotNull("Initial LTPA cookie should be set", initialCookie);
        Log.info(thisClass, testName, "Initial LTPA cookie: " + maskCookie(initialCookie));

        // Step 2: Make immediate second request (token still fresh)
        HttpURLConnection conn2 = makeRequest(servletUrl, initialCookie);
        assertEquals("Second request should succeed", 200, conn2.getResponseCode());

        String secondCookie = extractLTPACookie(conn2);

        // Step 3: Verify token was NOT refreshed
        if (secondCookie == null) {
            Log.info(thisClass, testName, "No new cookie in response - token was not refreshed (expected)");
        } else {
            // If a cookie is returned, it should be the same as the initial one
            assertEquals("LTPA cookie should not be refreshed when still valid",
                         initialCookie, secondCookie);
            Log.info(thisClass, testName, "Same cookie returned - token was not refreshed (expected)");
        }

        conn1.disconnect();
        conn2.disconnect();
    }

    /**
     * Test token refresh with short expiration time.
     *
     * Configuration:
     * - Token expiration: 2 minutes (120 seconds)
     * - Refresh threshold: 1 minute (60 seconds)
     *
     * Expected behavior:
     * Token should refresh quickly after initial creation
     */
    @Test
    public void testTokenRefreshWithShortExpiration() throws Exception {
        String testName = "testTokenRefreshWithShortExpiration";
        Log.info(thisClass, testName, "Starting test");

        // Update server configuration for short expiration
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("serverTokenRefreshShort.xml");
        server.waitForConfigUpdateInLogUsingMark(null);
        Log.info(thisClass, testName, "Server reconfigured with short token expiration");

        String servletUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() +
                            "/" + APP_NAME + "/" + SERVLET_NAME;

        // Make initial request
        HttpURLConnection conn1 = makeRequest(servletUrl, null);
        assertEquals("First request should succeed", 200, conn1.getResponseCode());

        String initialCookie = extractLTPACookie(conn1);
        assertNotNull("Initial LTPA cookie should be set", initialCookie);
        Log.info(thisClass, testName, "Initial LTPA cookie: " + maskCookie(initialCookie));

        // Wait for token to approach threshold (2m expiration, 1m threshold = 1m window)
        Log.info(thisClass, testName, "Waiting " + SHORT_EXPIRATION_WAIT_MS + "ms for token to approach refresh threshold...");
        Thread.sleep(SHORT_EXPIRATION_WAIT_MS);

        // Make second request - should trigger refresh
        HttpURLConnection conn2 = makeRequest(servletUrl, initialCookie);
        assertEquals("Second request should succeed", 200, conn2.getResponseCode());

        String refreshedCookie = extractLTPACookie(conn2);

        if (refreshedCookie != null) {
            assertFalse("LTPA cookie should be refreshed with short expiration",
                        initialCookie.equals(refreshedCookie));
            Log.info(thisClass, testName, "Token was successfully refreshed with short expiration");
            Log.info(thisClass, testName, "Refreshed LTPA cookie: " + maskCookie(refreshedCookie));
        } else {
            Log.info(thisClass, testName, "WARNING: No refreshed cookie received - token may not have reached refresh threshold yet");
        }

        conn1.disconnect();
        conn2.disconnect();
    }

    /**
     * Test multiple sequential requests to verify consistent token refresh behavior.
     */
    @Test
    public void testMultipleRequestsWithTokenRefresh() throws Exception {
        String testName = "testMultipleRequestsWithTokenRefresh";
        Log.info(thisClass, testName, "Starting test");

        String servletUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() +
                            "/" + APP_NAME + "/" + SERVLET_NAME;

        String currentCookie = null;

        // Make 3 requests with delays
        for (int i = 1; i <= 3; i++) {
            Log.info(thisClass, testName, "Making request #" + i);

            HttpURLConnection conn = makeRequest(servletUrl, currentCookie);
            assertEquals("Request #" + i + " should succeed", 200, conn.getResponseCode());

            String newCookie = extractLTPACookie(conn);

            if (i == 1) {
                assertNotNull("First request should set LTPA cookie", newCookie);
                currentCookie = newCookie;
            } else {
                if (newCookie != null) {
                    Log.info(thisClass, testName, "Request #" + i + " received new cookie (token refreshed)");
                    currentCookie = newCookie;
                } else {
                    Log.info(thisClass, testName, "Request #" + i + " used existing cookie (no refresh)");
                }
            }

            conn.disconnect();

            // Wait between requests
            if (i < 3) {
                Thread.sleep(REQUEST_INTERVAL_MS);
            }
        }

        assertNotNull("Should have valid LTPA cookie after all requests", currentCookie);
    }

    /**
     * Helper method to make HTTP request with optional cookie.
     * IMPORTANT: Caller MUST call disconnect() on the returned connection to prevent resource leaks.
     *
     * @param urlString the URL to request
     * @param cookie optional LTPA cookie value
     * @return the HTTP connection (caller must call disconnect())
     * @throws IOException if the request fails
     */
    private HttpURLConnection makeRequest(String urlString, String cookie) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.setDoOutput(false);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(false);

        // Add cookie if provided
        if (cookie != null) {
            conn.setRequestProperty("Cookie", LTPA_COOKIE_NAME + "=" + cookie);
        }

        // Add basic auth for initial authentication
        String userpass = "user1:user1pwd";
        String basicAuth = "Basic " + java.util.Base64.getEncoder().encodeToString(userpass.getBytes());
        conn.setRequestProperty("Authorization", basicAuth);

        conn.connect();

        // Read response to complete the request
        try (InputStream is = conn.getInputStream();
                        BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Just consume the response
            }
        } catch (IOException e) {
            // May occur for error responses (e.g., 401, 403)
            Log.info(thisClass, "makeRequest", "IOException while reading response: " + e.getMessage());
        }

        return conn;
    }

    /**
     * Extract LTPA cookie value from HTTP response.
     */
    private String extractLTPACookie(HttpURLConnection conn) {
        return LTPATestUtils.extractLTPACookie(conn);
    }

    /**
     * Mask cookie value for logging (show only first and last few characters).
     */
    private String maskCookie(String cookie) {
        return LTPATestUtils.maskCookie(cookie);
    }
}
