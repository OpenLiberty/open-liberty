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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

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
 * FAT tests for the LTPA inactivity timeout feature.
 *
 * Default config ({@code serverTokenInactivity.xml}):
 *   expiration=10m, inactivityTimeout=1m, refreshThreshold=not set
 *
 * Refresh config ({@code serverTokenRefresh.xml}, used by testInactivityWindowResetsOnTokenRefresh):
 *   expiration=4m, inactivityTimeout=2m, refreshThreshold=1m
 *
 * The inactivity-only config isolates the hard-expiry-on-idle path:
 * - A token used within 1 minute remains valid.
 * - A token idle for more than 1 minute returns 401 even though absolute expiry
 *   (10 minutes) has not been reached.
 * - No proactive refresh occurs because refreshThreshold is not configured.
 * - The feature is completely inert when beta edition is disabled.
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LTPAInactivityTimeoutFATTest {

    private static final String APP_NAME = "ltpaTest";
    private static final String SERVLET_NAME = "LTPATestServlet";
    private static final String LTPA_COOKIE_NAME = "LtpaToken2";
    private static final Class<?> thisClass = LTPAInactivityTimeoutFATTest.class;
    private static LibertyServer server;

    // serverTokenInactivity.xml: inactivityTimeout=1m (60s idle deadline)
    // Wait 70s to go 10s past the 60s idle deadline — gives a comfortable margin.
    private static final long INACTIVITY_WAIT_MS = 70_000;

    // serverTokenRefresh.xml: inactivityTimeout=2m, refreshThreshold=1m (threshold crossed at ~60s idle)
    // Wait 70s so that inactivity remaining = 120s − 70s = 50s ≤ 60s threshold → clone is returned.
    // Reuse the same value as INACTIVITY_WAIT_MS; a separate constant avoids confusion.
    private static final long REFRESH_THRESHOLD_WAIT_MS = 70_000;

    // Small delay — well within any 1-minute or 2-minute inactivity window
    private static final long FRESH_REQUEST_DELAY_MS = 2_000;

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        public void starting(Description description) {
            Log.info(thisClass, description.getMethodName(),
                     "\n=====================================\nStarting test: " +
                     description.getMethodName() + "\n=====================================");
        }

        @Override
        public void finished(Description description) {
            Log.info(thisClass, description.getMethodName(),
                     "\n=====================================\nFinished test: " +
                     description.getMethodName() + "\n=====================================");
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
        server.setServerConfigurationFile("serverTokenInactivity.xml");
        server.startServer(true);
        server.waitForStringInLog("CWWKZ0001I.*" + APP_NAME);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1 — token used within inactivity window stays valid, no new cookie
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * A token that is used again well within the inactivity window must remain
     * valid and must NOT produce a new Set-Cookie header (no proactive refresh
     * because refreshThreshold is not configured).
     */
    @Test
    public void testTokenRemainsValidWithinInactivityWindow() throws Exception {
        String testName = "testTokenRemainsValidWithinInactivityWindow";
        String url = getServletUrl();

        // Authenticate and get initial cookie
        HttpURLConnection conn1 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        assertEquals("Initial authentication must succeed", 200, conn1.getResponseCode());
        String cookie = extractLTPACookie(conn1);
        assertNotNull("LTPA cookie must be set after authentication", cookie);
        conn1.disconnect();
        Log.info(thisClass, testName, "Initial cookie: " + LTPATestUtils.maskCookie(cookie));

        // Small delay — well within inactivity window
        Thread.sleep(FRESH_REQUEST_DELAY_MS);

        // SSO request — must succeed and must NOT issue a new cookie
        HttpURLConnection conn2 = makeRequestWithCookie(url, cookie);
        assertEquals("SSO request within inactivity window must succeed", 200, conn2.getResponseCode());
        String newCookie = extractLTPACookie(conn2);
        assertNull("No new cookie expected — refreshThreshold is not configured", newCookie);
        conn2.disconnect();
        Log.info(thisClass, testName, "Token still valid within inactivity window — no new cookie (expected)");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2 — no Set-Cookie when refreshThreshold is not configured, even after partial idle
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * When {@code refreshThreshold} is not set, the server must never return a new
     * Set-Cookie header during a valid session — regardless of how much of the
     * inactivity window has been consumed.
     *
     * Configuration (serverTokenInactivity.xml): expiration=10m, inactivityTimeout=1m, refreshThreshold=not set
     *
     * Wait 30s — half the 60s inactivity window — then make an SSO request.
     * The token is still valid (30s < 60s deadline), but because there is no
     * {@code refreshThreshold} configured, proactive refresh must NOT fire and
     * the response must contain no Set-Cookie header.
     */
    @Test
    public void testNoNewCookieWhenRefreshThresholdNotConfiguredAfterPartialIdle() throws Exception {
        String testName = "testNoNewCookieWhenRefreshThresholdNotConfiguredAfterPartialIdle";
        String url = getServletUrl();

        // Authenticate and get initial cookie
        HttpURLConnection conn1 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        assertEquals("Initial authentication must succeed", 200, conn1.getResponseCode());
        String cookie = extractLTPACookie(conn1);
        assertNotNull("LTPA cookie must be set after authentication", cookie);
        conn1.disconnect();
        Log.info(thisClass, testName, "Initial cookie: " + LTPATestUtils.maskCookie(cookie));

        // Wait 30s — halfway through the 60s inactivity window, token is still valid
        long halfWindowMs = 30_000;
        Log.info(thisClass, testName, "Waiting " + halfWindowMs + "ms (half of 60s inactivity window)...");
        Thread.sleep(halfWindowMs);

        // SSO request — token valid, but refreshThreshold not configured → no new cookie
        HttpURLConnection conn2 = makeRequestWithCookie(url, cookie);
        assertEquals("SSO request at half-idle must succeed (token still valid)", 200, conn2.getResponseCode());
        String newCookie = extractLTPACookie(conn2);
        assertNull("No Set-Cookie expected — refreshThreshold is not configured, so proactive refresh must not fire",
                   newCookie);
        conn2.disconnect();
        Log.info(thisClass, testName, "Correct: no new cookie returned after partial idle with no refreshThreshold");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3 — token idle past inactivityTimeout returns 401
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * A token that has been idle for longer than {@code inactivityTimeout} must
     * be rejected (401 or 302 redirect) even though the absolute expiry (10 min)
     * has not been reached.
     */
    @Test
    public void testTokenExpiredAfterInactivityTimeout() throws Exception {
        String testName = "testTokenExpiredAfterInactivityTimeout";
        String url = getServletUrl();

        // Authenticate and get initial cookie
        HttpURLConnection conn1 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        assertEquals("Initial authentication must succeed", 200, conn1.getResponseCode());
        String cookie = extractLTPACookie(conn1);
        assertNotNull("LTPA cookie must be set", cookie);
        conn1.disconnect();
        Log.info(thisClass, testName, "Received cookie: " + LTPATestUtils.maskCookie(cookie));

        // Wait past the 1-minute inactivity window (absolute expiry is 10 min away)
        Log.info(thisClass, testName, "Waiting " + INACTIVITY_WAIT_MS + "ms past inactivity timeout...");
        Thread.sleep(INACTIVITY_WAIT_MS);

        // Request with idle token — must be rejected
        HttpURLConnection conn2 = makeRequestWithCookie(url, cookie);
        int responseCode = conn2.getResponseCode();
        Log.info(thisClass, testName, "Response code with idle token: " + responseCode);
        conn2.disconnect();

        assertTrue("Idle token must be rejected (401 or 302), got: " + responseCode,
                   responseCode == 401 || responseCode == 302 || responseCode == 403);
        Log.info(thisClass, testName, "Idle token correctly rejected with status " + responseCode);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4 — re-authentication after inactivity expiry issues a new token
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * After a token expires due to inactivity, the user can re-authenticate with
     * credentials and receive a fresh token that is distinct from the expired one.
     */
    @Test
    public void testReauthenticationAfterInactivityExpiry() throws Exception {
        String testName = "testReauthenticationAfterInactivityExpiry";
        String url = getServletUrl();

        // Initial authentication
        HttpURLConnection conn1 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        String expiredCookie = extractLTPACookie(conn1);
        assertNotNull("Must receive initial cookie", expiredCookie);
        conn1.disconnect();

        // Let the token go idle
        Log.info(thisClass, testName, "Waiting " + INACTIVITY_WAIT_MS + "ms for inactivity expiry...");
        Thread.sleep(INACTIVITY_WAIT_MS);

        // Verify idle token is rejected
        HttpURLConnection conn2 = makeRequestWithCookie(url, expiredCookie);
        int rejectedCode = conn2.getResponseCode();
        conn2.disconnect();
        assertTrue("Idle token must be rejected before re-auth", rejectedCode == 401 || rejectedCode == 302 || rejectedCode == 403);

        // Re-authenticate with credentials — must succeed and issue a new cookie
        HttpURLConnection conn3 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        assertEquals("Re-authentication must succeed", 200, conn3.getResponseCode());
        String newCookie = extractLTPACookie(conn3);
        assertNotNull("Must receive new cookie after re-authentication", newCookie);
        assertTrue("New cookie must differ from the expired one", !expiredCookie.equals(newCookie));
        conn3.disconnect();
        Log.info(thisClass, testName, "Re-authentication succeeded with new cookie: " + LTPATestUtils.maskCookie(newCookie));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 5 — inactivity window resets on each successful request
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The inactivity deadline resets on each token use because each clone stamps a
     * fresh {@code creationTime}.  By making two requests that are each within the
     * inactivity window, the total elapsed time can exceed {@code inactivityTimeout}
     * while the token stays valid — because every successful clone resets the clock.
     *
     * Config (serverTokenRefresh.xml): expiration=4m, inactivityTimeout=2m, refreshThreshold=1m
     *   - Refresh fires when inactivity remaining ≤ 60s, i.e. after ~60s of idle.
     *   - After 70s idle: remaining = 120s − 70s = 50s ≤ 60s → clone returned, window reset.
     *   - After another 70s idle from the reset point: remaining = 50s ≤ 60s → another clone.
     *   - Total wall time ≈ 140s > 120s inactivityTimeout, yet token stays valid because
     *     each clone resets the inactivity clock.
     */
    @Test
    public void testInactivityWindowResetsOnTokenRefresh() throws Exception {
        String testName = "testInactivityWindowResetsOnTokenRefresh";

        // Switch to a config that has both inactivityTimeout AND refreshThreshold
        // so the token gets proactively cloned (resetting creationTime) on each request.
        // serverTokenRefresh.xml: expiration=4m, inactivityTimeout=2m, refreshThreshold=1m
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("serverTokenRefresh.xml");
        server.waitForConfigUpdateInLogUsingMark(null);
        Log.info(thisClass, testName, "Switched to serverTokenRefresh.xml (expiration=4m, inactivityTimeout=2m, refreshThreshold=1m)");

        String url = getServletUrl();

        // Authenticate
        HttpURLConnection conn1 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        assertEquals("Initial authentication must succeed", 200, conn1.getResponseCode());
        String cookie = extractLTPACookie(conn1);
        assertNotNull("Must receive initial cookie", cookie);
        conn1.disconnect();
        Log.info(thisClass, testName, "Initial cookie: " + LTPATestUtils.maskCookie(cookie));

        // Wait 70s so inactivity remaining = 120s − 70s = 50s ≤ 60s threshold → clone returned.
        Log.info(thisClass, testName, "Waiting " + REFRESH_THRESHOLD_WAIT_MS + "ms to cross refresh threshold...");
        Thread.sleep(REFRESH_THRESHOLD_WAIT_MS);

        // First SSO request — should trigger clone, resetting creationTime
        HttpURLConnection conn2 = makeRequestWithCookie(url, cookie);
        assertEquals("SSO request must succeed after waiting past threshold", 200, conn2.getResponseCode());
        String refreshedCookie = extractLTPACookie(conn2);
        conn2.disconnect();

        if (refreshedCookie != null) {
            Log.info(thisClass, testName, "Token was cloned — inactivity window reset");
            cookie = refreshedCookie;
        } else {
            Log.info(thisClass, testName, "No clone yet — token still within window");
        }

        // Wait another 70s from the reset point.
        // If the clone occurred: new inactivity deadline = now + 120s → remaining after 70s = 50s → still valid (or another clone).
        // If no clone occurred: total idle = 140s > 120s inactivityTimeout → would be rejected (401).
        Log.info(thisClass, testName, "Waiting another " + REFRESH_THRESHOLD_WAIT_MS + "ms from the reset point...");
        Thread.sleep(REFRESH_THRESHOLD_WAIT_MS);

        HttpURLConnection conn3 = makeRequestWithCookie(url, cookie);
        int responseCode = conn3.getResponseCode();
        conn3.disconnect();

        // Expected: 200 (window was reset by clone) or another clone triggered.
        // If the window was NOT reset: total idle ≈ 140s > 120s inactivityTimeout → 401.
        Log.info(thisClass, testName, "Response after second wait: " + responseCode);
        assertEquals("Token should still be valid after two waits if window was reset", 200, responseCode);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String getServletUrl() {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() +
               "/" + APP_NAME + "/" + SERVLET_NAME;
    }

    private HttpURLConnection makeAuthenticatedRequest(String urlString, String cookie,
                                                       String username, String password) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(false);
        if (cookie != null) {
            conn.setRequestProperty("Cookie", LTPA_COOKIE_NAME + "=" + cookie);
        }
        String basicAuth = "Basic " + java.util.Base64.getEncoder()
                               .encodeToString((username + ":" + password).getBytes());
        conn.setRequestProperty("Authorization", basicAuth);
        consumeResponse(conn);
        return conn;
    }

    private HttpURLConnection makeRequestWithCookie(String urlString, String cookie) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(false);
        conn.setRequestProperty("Cookie", LTPA_COOKIE_NAME + "=" + cookie);
        consumeResponse(conn);
        return conn;
    }

    private void consumeResponse(HttpURLConnection conn) {
        try {
            int code = conn.getResponseCode();
            InputStream is = (code >= 400) ? conn.getErrorStream() : conn.getInputStream();
            if (is != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                    while (br.readLine() != null) { /* consume */ }
                }
            }
        } catch (IOException e) {
            Log.info(thisClass, "consumeResponse", "IOException: " + e.getMessage());
        }
    }

    private String extractLTPACookie(HttpURLConnection conn) {
        return LTPATestUtils.extractLTPACookie(conn);
    }
}
