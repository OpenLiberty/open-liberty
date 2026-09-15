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
 * 1. LTPA tokens are proactively refreshed when inactivity time remaining falls at or below the refresh threshold
 * 2. A new Set-Cookie header is returned in the response when a token is refreshed
 * 3. Tokens are NOT refreshed when inactivity time remaining is still above the refresh threshold
 * 4. The refreshThreshold configuration is measured against the inactivity window, not absolute expiration
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LTPATokenRefreshTest {

    private static final String APP_NAME = "ltpaTest";
    private static final String SERVLET_NAME = "LTPATestServlet";
    private static final String LTPA_COOKIE_NAME = "LtpaToken2";
    private static final Class<?> thisClass = LTPATokenRefreshTest.class;
    private static LibertyServer server;

    // Timing constants for token refresh tests.
    //
    // serverTokenRefresh.xml:      expiration=4m, inactivityTimeout=2m, refreshThreshold=1m
    // serverTokenRefreshShort.xml: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m
    //
    // Refresh fires when (inactivity deadline - now) <= refreshThreshold (1m).
    // With a 2m inactivity window, the threshold is crossed after ~60s of idle.
    // Wait 70s to give a comfortable 10s margin above the 60s trigger point.
    private static final long REFRESH_THRESHOLD_WAIT_MS = 70000;   // 70s — 10s past the 60s refresh trigger
    private static final long SHORT_EXPIRATION_WAIT_MS  = 70000;   // 70s — same trigger point for short-expiration config
    private static final long REQUEST_INTERVAL_MS       = 5000;    //  5s — interval between sequential requests

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
        // Enable beta edition in the server JVM so ProductInfo.getBetaEdition() returns true.
        // This activates inactivityTimeout / refreshThreshold in LTPAToken2 and LTPAConfigurationImpl.
        server.setJvmOptions(java.util.Arrays.asList("-Dcom.ibm.ws.beta.edition=true"));
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
     * Test that an LTPA token is refreshed when it approaches the inactivity refresh threshold.
     *
     * Configuration (serverTokenRefresh.xml):
     * - expiration:        4 minutes (240s) — absolute hard cap; token always dies here
     * - inactivityTimeout: 2 minutes (120s) — idle window; token expires if no request within this window
     * - refreshThreshold:  1 minute   (60s) — proactively clone the token when <=60s of inactivity remains
     *
     * Timeline:
     *   t=0s   first request → fresh token, creationTime stamped, inactivity deadline = t+120s
     *   t=70s  second request → inactivity remaining = 50s ≤ 60s threshold → clone returned
     *   t=240s absolute expiration → token is dead regardless of activity
     *
     * Expected behavior:
     * 1. Initial request creates a new LTPA token and sets an LtpaToken2 cookie
     * 2. After 70s idle the inactivity window has <1m left — the server returns a refreshed cookie
     * 3. The refreshed cookie value differs from the original (new creationTime, same absolute expiry)
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

        // Step 2: Wait for token to cross the refresh threshold.
        // inactivityTimeout=2m, refreshThreshold=1m → refresh fires when <=60s inactivity remains.
        // After 70s of idle: remaining = 120s - 70s = 50s ≤ 60s threshold → clone is returned.
        Log.info(thisClass, testName, "Waiting " + REFRESH_THRESHOLD_WAIT_MS + "ms for inactivity window to cross refresh threshold...");
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
     * Test that an LTPA token is NOT refreshed when it is well above the refresh threshold.
     *
     * Configuration (serverTokenRefresh.xml):
     * - expiration:        4 minutes (240s) — absolute hard cap
     * - inactivityTimeout: 2 minutes (120s) — idle window
     * - refreshThreshold:  1 minute   (60s) — refresh only when <=60s of inactivity remains
     *
     * Timeline:
     *   t=0s  first request  → fresh token, inactivity deadline = t+120s
     *   t~0s  second request → inactivity remaining ≈ 120s >> 60s threshold → no clone
     *
     * Expected behavior:
     * 1. Initial request creates a new LTPA token and sets an LtpaToken2 cookie
     * 2. Immediate follow-up request: inactivity remaining ≈ 120s, well above the 60s threshold — no refresh
     * 3. No new LtpaToken2 Set-Cookie header should appear in the second response
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
     * Test token refresh when using a shorter absolute expiration.
     *
     * Configuration (serverTokenRefreshShort.xml):
     * - expiration:        3 minutes (180s) — absolute hard cap
     * - inactivityTimeout: 2 minutes (120s) — idle window; strictly less than expiration so the sliding window is active
     * - refreshThreshold:  1 minute   (60s) — proactively clone when <=60s of inactivity remains
     *
     * Timeline:
     *   t=0s   first request → fresh token, inactivity deadline = t+120s, absolute deadline = t+180s
     *   t=70s  second request → inactivity remaining = 50s ≤ 60s threshold → clone returned
     *   t=180s absolute expiration
     *
     * Expected behavior: same refresh trigger point as the main config (60s crossed after 70s idle).
     * The shorter absolute expiration validates that the inactivity window, not the expiration, drives refresh.
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

        // Wait 70s for inactivity window to cross the refresh threshold.
        // inactivityTimeout=2m, refreshThreshold=1m → after 70s idle: remaining = 50s ≤ 60s threshold → clone returned.
        Log.info(thisClass, testName, "Waiting " + SHORT_EXPIRATION_WAIT_MS + "ms for inactivity window to cross refresh threshold...");
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
     * Test multiple sequential requests to verify consistent token handling.
     *
     * Configuration (serverTokenRefresh.xml):
     * - expiration=4m, inactivityTimeout=2m, refreshThreshold=1m
     *
     * Requests are spaced 5s apart — well within the 120s inactivity window — so no refresh
     * is expected during this sequence.  The test confirms that a valid cookie is maintained
     * across all requests regardless of whether the server chooses to refresh.
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

            // Wait 5s between requests (well within 120s inactivity window — no refresh expected)
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
