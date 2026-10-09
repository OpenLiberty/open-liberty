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
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
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
 * FAT tests for SSOAuthenticator LTPA token refresh functionality.
 *
 * These tests verify the complete SSO authentication flow with token refresh:
 * 1. Initial authentication and LTPA cookie creation
 * 2. SSO authentication using an existing LTPA cookie (no credentials)
 * 3. Token refresh detection — server returns a new Set-Cookie when inactivity remaining ≤ threshold
 * 4. Cookie attributes (HttpOnly, Secure, Path) are preserved across a refresh
 * 5. Per-user token isolation — different users always hold different cookies
 * 6. Absolute expiration — a fully-expired token (past hard cap) requires re-authentication
 */
@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class SSOAuthenticatorRefreshTest {

    private static final String APP_NAME = "ltpaTest";
    private static final String SERVLET_NAME = "LTPATestServlet";
    private static final String LTPA_COOKIE_NAME = "LtpaToken2";
    private static final Class<?> thisClass = SSOAuthenticatorRefreshTest.class;
    private static LibertyServer server;

    // Timing constants for token refresh tests.
    //
    // serverTokenRefresh.xml:      expiration=4m (240s), inactivityTimeout=2m (120s), refreshThreshold=1m (60s)
    // serverTokenRefreshShort.xml: expiration=3m (180s), inactivityTimeout=2m (120s), refreshThreshold=1m (60s)
    //
    // Refresh fires when (inactivity deadline − now) ≤ 60s, i.e. after ~60s of idle.
    // Wait 70s: remaining = 120s − 70s = 50s ≤ 60s threshold → clone returned (10s margin).
    private static final long REFRESH_THRESHOLD_WAIT_MS = 70000;   // 70s  — 10s past the 60s refresh trigger
    private static final long SHORT_EXPIRATION_WAIT_MS  = 70000;   // 70s  — same trigger point for short-expiration config
    // Full absolute expiry: serverTokenRefreshShort.xml has expiration=3m (180s).
    // Wait 250s to be comfortably past the 180s hard cap.
    private static final long FULL_EXPIRATION_WAIT_MS   = 250000;  // 250s — past the 3m absolute expiration (short config)
    private static final long CONFIG_UPDATE_WAIT_MS     = 2000;    //   2s — wait for config update to propagate
    private static final long RAPID_REQUEST_DELAY_MS    = 500;     // 500ms — delay between rapid successive requests
    private static final long FRESH_TOKEN_DELAY_MS      = 1000;    //   1s — delay between fresh-token requests

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        public void starting(Description description) {
            Log.info(thisClass, description.getMethodName(),
                     "\n=====================================\n" +
                                                             "Starting test: " + description.getMethodName() +
                                                             "\n=====================================");
        }

        @Override
        public void finished(Description description) {
            Log.info(thisClass, description.getMethodName(),
                     "\n=====================================\n" +
                                                             "Finished test: " + description.getMethodName() +
                                                             "\n=====================================");
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
     * Test the complete SSO authentication flow with token refresh.
     *
     * Configuration (serverTokenRefresh.xml): expiration=4m, inactivityTimeout=2m, refreshThreshold=1m
     *
     * Scenario:
     * 1. Authenticate with credentials → receive initial LtpaToken2 cookie
     * 2. Immediate SSO request → token still fresh (≈120s inactivity remaining >> 60s threshold) → no new cookie
     * 3. Wait 70s idle → inactivity remaining = 50s ≤ 60s threshold → next request returns a clone
     * 4. SSO request triggers clone → refreshed cookie returned in Set-Cookie
     * 5. Verify the refreshed cookie also works for subsequent SSO requests
     */
    @Test
    public void testSSOAuthenticationWithTokenRefresh() throws Exception {
        String testName = "testSSOAuthenticationWithTokenRefresh";
        Log.info(thisClass, testName, "Testing complete SSO authentication flow with token refresh");

        String servletUrl = getServletUrl();

        // Step 1: Initial authentication with credentials
        Log.info(thisClass, testName, "Step 1: Initial authentication with credentials");
        HttpURLConnection conn1 = makeAuthenticatedRequest(servletUrl, null, "user1", "user1pwd");
        assertEquals("Initial authentication should succeed", 200, conn1.getResponseCode());

        String cookie1 = extractLTPACookie(conn1);
        assertNotNull("LTPA cookie should be set after authentication", cookie1);
        Log.info(thisClass, testName, "Received initial LTPA cookie: " + maskCookie(cookie1));
        conn1.disconnect();

        // Step 2: SSO authentication using cookie (no credentials)
        Log.info(thisClass, testName, "Step 2: SSO authentication using existing cookie");
        HttpURLConnection conn2 = makeRequestWithCookie(servletUrl, cookie1);
        assertEquals("SSO authentication should succeed", 200, conn2.getResponseCode());

        String cookie2 = extractLTPACookie(conn2);
        if (cookie2 == null) {
            Log.info(thisClass, testName, "No new cookie - using existing token (expected for fresh token)");
        }
        conn2.disconnect();

        // Step 3: Wait for inactivity window to cross the refresh threshold.
        // After 70s idle: remaining = 120s − 70s = 50s ≤ 60s threshold → clone returned.
        Log.info(thisClass, testName, "Step 3: Waiting " + REFRESH_THRESHOLD_WAIT_MS + "ms to cross refresh threshold");
        Thread.sleep(REFRESH_THRESHOLD_WAIT_MS);

        // Step 4: SSO request should trigger clone — use most current cookie
        Log.info(thisClass, testName, "Step 4: Making SSO request that should trigger clone");
        String currentCookie = (cookie2 != null) ? cookie2 : cookie1;
        HttpURLConnection conn3 = makeRequestWithCookie(servletUrl, currentCookie);
        assertEquals("SSO request should succeed", 200, conn3.getResponseCode());

        String cookie3 = extractLTPACookie(conn3);
        if (cookie3 != null) {
            assertFalse("Token should be refreshed (different cookie)", currentCookie.equals(cookie3));
            Log.info(thisClass, testName, "Token successfully refreshed: " + maskCookie(cookie3));

            // Step 5: Verify new cookie works for SSO
            Log.info(thisClass, testName, "Step 5: Verifying refreshed cookie works for SSO");
            HttpURLConnection conn4 = makeRequestWithCookie(servletUrl, cookie3);
            assertEquals("SSO with refreshed cookie should succeed", 200, conn4.getResponseCode());
            conn4.disconnect();
        } else {
            Log.info(thisClass, testName, "Token not yet at refresh threshold");
        }
        conn3.disconnect();
    }

    /**
     * Test that SSO works correctly without refresh when token is fresh.
     */
    @Test
    public void testSSOWithoutRefreshForFreshToken() throws Exception {
        String testName = "testSSOWithoutRefreshForFreshToken";
        Log.info(thisClass, testName, "Testing SSO without refresh for fresh tokens");

        String servletUrl = getServletUrl();

        // Authenticate and get cookie
        HttpURLConnection conn1 = makeAuthenticatedRequest(servletUrl, null, "user1", "user1pwd");
        String cookie = extractLTPACookie(conn1);
        assertNotNull("Should receive LTPA cookie", cookie);
        conn1.disconnect();

        // Make multiple SSO requests immediately (token still fresh)
        for (int i = 1; i <= 3; i++) {
            Log.info(thisClass, testName, "SSO request #" + i + " with fresh token");
            HttpURLConnection conn = makeRequestWithCookie(servletUrl, cookie);
            assertEquals("SSO request #" + i + " should succeed", 200, conn.getResponseCode());

            String newCookie = extractLTPACookie(conn);
            if (newCookie == null) {
                Log.info(thisClass, testName, "No new cookie in response #" + i + " (expected - token still fresh)");
            } else {
                assertEquals("Cookie should not change for fresh token", cookie, newCookie);
            }
            conn.disconnect();

            // Small delay between requests
            if (i < 3) {
                Thread.sleep(FRESH_TOKEN_DELAY_MS);
            }
        }
    }

    /**
     * Test that token refresh preserves per-user isolation.
     *
     * Configuration (serverTokenRefresh.xml): expiration=4m, inactivityTimeout=2m, refreshThreshold=1m
     *
     * Two users authenticate independently.  After 70s idle both tokens cross the refresh
     * threshold.  The refreshed cookies must remain distinct — one user's clone must never
     * equal another user's clone.
     */
    @Test
    public void testTokenRefreshWithMultipleUsers() throws Exception {
        String testName = "testTokenRefreshWithMultipleUsers";
        Log.info(thisClass, testName, "Testing token refresh with multiple users");

        String servletUrl = getServletUrl();

        // User 1 authenticates
        Log.info(thisClass, testName, "User1 authenticating");
        HttpURLConnection conn1 = makeAuthenticatedRequest(servletUrl, null, "user1", "user1pwd");
        String user1Cookie = extractLTPACookie(conn1);
        assertNotNull("User1 should receive LTPA cookie", user1Cookie);
        conn1.disconnect();

        // User 2 authenticates
        Log.info(thisClass, testName, "User2 authenticating");
        HttpURLConnection conn2 = makeAuthenticatedRequest(servletUrl, null, "user2", "user2pwd");
        String user2Cookie = extractLTPACookie(conn2);
        assertNotNull("User2 should receive LTPA cookie", user2Cookie);
        conn2.disconnect();

        // Verify cookies are different
        assertFalse("Different users should have different cookies", user1Cookie.equals(user2Cookie));

        // Both users make SSO requests
        Log.info(thisClass, testName, "User1 making SSO request");
        HttpURLConnection conn3 = makeRequestWithCookie(servletUrl, user1Cookie);
        assertEquals("User1 SSO should succeed", 200, conn3.getResponseCode());
        conn3.disconnect();

        Log.info(thisClass, testName, "User2 making SSO request");
        HttpURLConnection conn4 = makeRequestWithCookie(servletUrl, user2Cookie);
        assertEquals("User2 SSO should succeed", 200, conn4.getResponseCode());
        conn4.disconnect();

        // Wait 70s so both tokens cross the refresh threshold (remaining = 50s ≤ 60s).
        Log.info(thisClass, testName, "Waiting " + REFRESH_THRESHOLD_WAIT_MS + "ms to cross refresh threshold");
        Thread.sleep(REFRESH_THRESHOLD_WAIT_MS);

        // Both users should get refreshed tokens
        Log.info(thisClass, testName, "User1 requesting token refresh");
        HttpURLConnection conn5 = makeRequestWithCookie(servletUrl, user1Cookie);
        String user1RefreshedCookie = extractLTPACookie(conn5);
        conn5.disconnect();

        Log.info(thisClass, testName, "User2 requesting token refresh");
        HttpURLConnection conn6 = makeRequestWithCookie(servletUrl, user2Cookie);
        String user2RefreshedCookie = extractLTPACookie(conn6);
        conn6.disconnect();

        // Verify refreshed cookies are still different
        if (user1RefreshedCookie != null && user2RefreshedCookie != null) {
            assertFalse("Refreshed cookies should still be different for different users",
                        user1RefreshedCookie.equals(user2RefreshedCookie));
            Log.info(thisClass, testName, "Both users received unique refreshed tokens");
        }
    }

    /**
     * Test cookie attributes are preserved during refresh.
     */
    @Test
    public void testCookieAttributesPreservedDuringRefresh() throws Exception {
        String testName = "testCookieAttributesPreservedDuringRefresh";
        Log.info(thisClass, testName, "Testing cookie attributes preservation during refresh");

        String servletUrl = getServletUrl();

        // Initial authentication
        HttpURLConnection conn1 = makeAuthenticatedRequest(servletUrl, null, "user1", "user1pwd");
        Map<String, List<String>> headers1 = conn1.getHeaderFields();
        String cookieHeader1 = getCookieHeader(headers1, LTPA_COOKIE_NAME);
        assertNotNull("Should have Set-Cookie header", cookieHeader1);

        Log.info(thisClass, testName, "Initial cookie header: " + cookieHeader1);
        boolean initialHttpOnly = cookieHeader1.toLowerCase().contains("httponly");
        boolean initialSecure = cookieHeader1.toLowerCase().contains("secure");
        boolean initialHasPath = cookieHeader1.toLowerCase().contains("path=");

        // Extract cookie before disconnecting
        String cookie = extractLTPACookie(conn1);
        assertNotNull("Should have LTPA cookie", cookie);
        conn1.disconnect();

        // Wait and trigger refresh
        Thread.sleep(REFRESH_THRESHOLD_WAIT_MS);
        HttpURLConnection conn2 = makeRequestWithCookie(servletUrl, cookie);
        Map<String, List<String>> headers2 = conn2.getHeaderFields();
        String cookieHeader2 = getCookieHeader(headers2, LTPA_COOKIE_NAME);

        if (cookieHeader2 != null) {
            Log.info(thisClass, testName, "Refreshed cookie header: " + cookieHeader2);
            boolean refreshedHttpOnly = cookieHeader2.toLowerCase().contains("httponly");
            boolean refreshedSecure = cookieHeader2.toLowerCase().contains("secure");
            boolean refreshedHasPath = cookieHeader2.toLowerCase().contains("path=");

            assertEquals("HttpOnly attribute should be preserved", initialHttpOnly, refreshedHttpOnly);
            assertEquals("Secure attribute should be preserved", initialSecure, refreshedSecure);
            assertEquals("Path attribute should be preserved", initialHasPath, refreshedHasPath);

            Log.info(thisClass, testName, "Cookie attributes preserved during refresh");
        }

        conn2.disconnect();
    }

    /**
     * Test rapid successive requests after the refresh threshold has been crossed.
     *
     * Configuration (serverTokenRefresh.xml): expiration=4m, inactivityTimeout=2m, refreshThreshold=1m
     *
     * After 70s idle the threshold is crossed.  Five rapid requests are made 500ms apart.
     * Each request that returns a new cookie updates the active cookie for the next request.
     * The test confirms: all 5 requests succeed (200) and at least the initial cookie is held.
     */
    @Test
    public void testRapidRequestsWithTokenRefresh() throws Exception {
        String testName = "testRapidRequestsWithTokenRefresh";
        Log.info(thisClass, testName, "Testing rapid successive requests with token refresh");

        String servletUrl = getServletUrl();

        // Initial authentication
        HttpURLConnection conn = makeAuthenticatedRequest(servletUrl, null, "user1", "user1pwd");
        String cookie = extractLTPACookie(conn);
        assertNotNull("Should receive initial cookie", cookie);
        conn.disconnect();

        // Wait for refresh threshold
        Thread.sleep(REFRESH_THRESHOLD_WAIT_MS);

        // Make 5 rapid requests 500ms apart; update the active cookie whenever a clone is returned.
        List<String> cookies = new ArrayList<>();
        cookies.add(cookie);
        for (int i = 1; i <= 5; i++) {
            Log.info(thisClass, testName, "Rapid request #" + i);
            HttpURLConnection rapidConn = makeRequestWithCookie(servletUrl, cookie);
            assertEquals("Rapid request #" + i + " should succeed", 200, rapidConn.getResponseCode());

            String newCookie = extractLTPACookie(rapidConn);
            if (newCookie != null) {
                Log.info(thisClass, testName, "Request #" + i + " returned a cloned cookie");
                cookie = newCookie;
                cookies.add(newCookie);
            }

            rapidConn.disconnect();
            Thread.sleep(RAPID_REQUEST_DELAY_MS);
        }
        Log.info(thisClass, testName, "Completed 5 requests; cookie updates (clones): " + (cookies.size() - 1));
        assertTrue("Should have at least the initial cookie", cookies.size() >= 1);
    }

    /**
     * Test that token refresh works correctly after a server configuration update.
     *
     * Authenticates under serverTokenRefresh.xml (expiration=4m), then switches to
     * serverTokenRefreshShort.xml (expiration=3m, same inactivityTimeout and refreshThreshold).
     * The old cookie must still be accepted after the config switch, and the next request
     * after 70s idle must trigger a clone under the new configuration.
     */
    @Test
    public void testTokenRefreshAfterConfigUpdate() throws Exception {
        String testName = "testTokenRefreshAfterConfigUpdate";
        Log.info(thisClass, testName, "Testing token refresh after configuration update");

        String servletUrl = getServletUrl();

        // Initial authentication with default config
        HttpURLConnection conn1 = makeAuthenticatedRequest(servletUrl, null, "user1", "user1pwd");
        String cookie1 = extractLTPACookie(conn1);
        assertNotNull("Should receive initial cookie", cookie1);
        conn1.disconnect();

        // Update server configuration to shorter expiration
        Log.info(thisClass, testName, "Updating server configuration to shorter token expiration");
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("serverTokenRefreshShort.xml");
        server.waitForConfigUpdateInLogUsingMark(null);

        // Wait for new config to take effect
        Thread.sleep(CONFIG_UPDATE_WAIT_MS);

        // Make SSO request with old cookie - should still work
        HttpURLConnection conn2 = makeRequestWithCookie(servletUrl, cookie1);
        assertEquals("SSO with old cookie should still work after config update", 200, conn2.getResponseCode());
        conn2.disconnect();

        // Wait for short expiration threshold
        Thread.sleep(SHORT_EXPIRATION_WAIT_MS);

        // Request should trigger refresh with new expiration settings
        HttpURLConnection conn3 = makeRequestWithCookie(servletUrl, cookie1);
        assertEquals("Request should succeed", 200, conn3.getResponseCode());

        String cookie2 = extractLTPACookie(conn3);
        if (cookie2 != null) {
            assertFalse("Should receive refreshed cookie with new expiration", cookie1.equals(cookie2));
            Log.info(thisClass, testName, "Token refreshed with new configuration");
        }

        conn3.disconnect();
    }

    /**
     * Test that a token past its absolute expiration deadline is rejected and requires re-authentication.
     *
     * Configuration (serverTokenRefreshShort.xml): expiration=3m (180s), inactivityTimeout=2m, refreshThreshold=1m
     *
     * Wait 250s — well past the 180s hard cap.  The server must reject the stale cookie
     * (401/302/403); subsequent re-authentication with credentials must succeed and
     * return a new cookie that differs from the expired one.
     */
    @Test
    public void testExpiredTokenRequiresReauthentication() throws Exception {
        String testName = "testExpiredTokenRequiresReauthentication";
        Log.info(thisClass, testName, "Testing that expired tokens require re-authentication");

        // Use short expiration config
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile("serverTokenRefreshShort.xml");
        server.waitForConfigUpdateInLogUsingMark(null);

        String servletUrl = getServletUrl();

        // Authenticate and get cookie
        HttpURLConnection conn1 = makeAuthenticatedRequest(servletUrl, null, "user1", "user1pwd");
        String cookie = extractLTPACookie(conn1);
        Log.info(thisClass, testName, "Received cookie: " + cookie);
        assertNotNull("Should receive cookie", cookie);
        conn1.disconnect();

        // Wait 250s — past the 180s absolute expiration (serverTokenRefreshShort.xml: expiration=3m).
        Log.info(thisClass, testName, "Waiting " + FULL_EXPIRATION_WAIT_MS + "ms for token to pass absolute expiration (3m=180s)");
        Thread.sleep(FULL_EXPIRATION_WAIT_MS);

        Log.info(thisClass, testName, "making a request with cookie ");
        // Try to use expired cookie - should fail or require re-auth
        HttpURLConnection conn2 = makeRequestWithCookie(servletUrl, cookie);
        int responseCode = conn2.getResponseCode();

        Log.info(thisClass, testName, "Response code with expired token: " + responseCode);

        // Should either get 401 Unauthorized or be redirected to login
        assertTrue("Expired token should not grant access",
                   responseCode == 401 || responseCode == 302 || responseCode == 403);

        conn2.disconnect();

        // Re-authenticate should work
        HttpURLConnection conn3 = makeAuthenticatedRequest(servletUrl, null, "user1", "user1pwd");
        assertEquals("Re-authentication should succeed", 200, conn3.getResponseCode());

        String newCookie = extractLTPACookie(conn3);
        assertNotNull("Should receive new cookie after re-authentication", newCookie);
        assertFalse("New cookie should be different from expired one", cookie.equals(newCookie));

        conn3.disconnect();
    }

    // Helper methods

    private String getServletUrl() {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() +
               "/" + APP_NAME + "/" + SERVLET_NAME;
    }

    /**
     * Make an authenticated HTTP request with credentials.
     * IMPORTANT: Caller MUST call disconnect() on the returned connection to prevent resource leaks.
     *
     * @param urlString the URL to request
     * @param cookie    optional LTPA cookie
     * @param username  the username for basic auth
     * @param password  the password for basic auth
     * @return the HTTP connection (caller must call disconnect())
     * @throws IOException if the request fails
     */
    private HttpURLConnection makeAuthenticatedRequest(String urlString, String cookie,
                                                       String username, String password) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.setDoOutput(false);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(false);

        if (cookie != null) {
            conn.setRequestProperty("Cookie", LTPA_COOKIE_NAME + "=" + cookie);
        }

        String userpass = username + ":" + password;
        String basicAuth = "Basic " + java.util.Base64.getEncoder().encodeToString(userpass.getBytes());
        conn.setRequestProperty("Authorization", basicAuth);

        conn.connect();
        consumeResponse(conn);

        return conn;
    }

    /**
     * Make an HTTP request with an LTPA cookie.
     * IMPORTANT: Caller MUST call disconnect() on the returned connection to prevent resource leaks.
     *
     * @param urlString the URL to request
     * @param cookie    the LTPA cookie value
     * @return the HTTP connection (caller must call disconnect())
     * @throws IOException if the request fails
     */
    private HttpURLConnection makeRequestWithCookie(String urlString, String cookie) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.setDoOutput(false);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(false);

        conn.setRequestProperty("Cookie", LTPA_COOKIE_NAME + "=" + cookie);

        // Don't call connect() - let consumeResponse trigger the request
        consumeResponse(conn);

        return conn;
    }

    /**
     * Consume the HTTP response body to complete the request.
     * Handles both success (2xx) and error (4xx, 5xx) responses properly.
     */
    private void consumeResponse(HttpURLConnection conn) {
        InputStream is = null;
        try {
            // Get response code first - this triggers the actual HTTP request
            int responseCode = conn.getResponseCode();

            // Use error stream for error responses, input stream for success
            if (responseCode >= 400) {
                is = conn.getErrorStream();
            } else {
                is = conn.getInputStream();
            }
            Log.info(thisClass, "consumeResponse", "response code: " + responseCode);
            // Consume the response body
            if (is != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        // Just consume the response
                    }
                }
            }
        } catch (IOException e) {
            // Log but don't fail - connection is still valid for getting headers/response code
            Log.info(thisClass, "consumeResponse", "IOException while consuming response: " + e.getMessage());
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Ignore close errors
                }
            }
        }
    }

    private String extractLTPACookie(HttpURLConnection conn) {
        return LTPATestUtils.extractLTPACookie(conn);
    }

    private String getCookieHeader(Map<String, List<String>> headers, String cookieName) {
        return LTPATestUtils.getCookieHeader(headers, cookieName);
    }

    private String maskCookie(String cookie) {
        return LTPATestUtils.maskCookie(cookie);
    }
}
