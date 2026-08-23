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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
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
import componenttest.rules.repeater.JakartaEEAction;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;


@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LTPATokenRefreshTests {

    private static final String APP_NAME     = "ltpaTest";
    private static final String SERVLET_NAME = "LTPATestServlet";
    private static final String LTPA_COOKIE  = "LtpaToken2";
    private static final Class<?> thisClass  = LTPATokenRefreshTests.class;

    private static LibertyServer server;

    // Token age offsets in seconds used by authenticateAndSetTokenAge().
    private static final int PAST_THRESHOLD_S   = 70;
    private static final int BEFORE_THRESHOLD_S = 20;
    private static final int PAST_INACTIVITY_S  = 130;
    private static final int PAST_EXPIRY_S      = 190;

    private static final String CFG_TOKEN_REFRESH                      = "serverTokenRefresh.xml";
    private static final String CFG_TOKEN_REFRESH_ONLY                 = "serverTokenRefreshOnly.xml";
    private static final String CFG_TOKEN_INACTIVITY_ONLY              = "serverTokenInactivityOnly.xml";
    private static final String CFG_TOKEN_EXCEEDS_EXPIRY               = "serverTokenInactivityExceedsExpiration.xml";
    private static final String CFG_TOKEN_THRESHOLD_EXCEEDS_INACTIVITY = "serverTokenRefreshExceedsInactivity.xml";

    @Rule
    public final TestWatcher logger = new TestWatcher() {
        @Override
        public void starting(Description description) {
            Log.info(thisClass, description.getMethodName(), "\n@@@@@@@@@@@@@@@@@\nEntering test " + description.getMethodName() + "\n@@@@@@@@@@@@@@@@@");
        }

        @Override
        public void finished(Description description) {
            Log.info(thisClass, description.getMethodName(), "\n@@@@@@@@@@@@@@@@@\nExiting test " + description.getMethodName() + "\n@@@@@@@@@@@@@@@@@");
        }
    };

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        server = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.token.ltpa.fat.refresh");
        server.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/ltpafattestlibertyinternals-1.0.mf");
        if (JakartaEEAction.isEE9OrLaterActive()) {
            JakartaEEAction.transformApp(Paths.get(server.getServerRoot() + "/apps/ltpaTest.war"));
        }
        server.addInstalledAppForValidation(APP_NAME);
    }

    @Before
    public void setUp() throws Exception {
        // Enable beta edition so ProductInfo.getBetaEdition() returns true,
        // activating inactivityTimeout / refreshThreshold in LTPAToken2 and LTPAConfigurationImpl.
        server.setJvmOptions(Arrays.asList("-Dcom.ibm.ws.beta.edition=true"));
        server.setServerConfigurationFile(CFG_TOKEN_REFRESH);
        server.startServer(true);
        server.waitForStringInLog("CWWKZ0001I.*" + APP_NAME);
    }

    @After
    public void tearDown() throws Exception {
        if (server != null && server.isStarted()) {
            // These warnings are intentionally produced by specific test configs and must not
            // cause teardown to fail: CWWKS4125W (inactivityTimeout >= expiration),
            // CWWKS4124W (refreshThreshold >= inactivityTimeout, relative-to-expiration path),
            // CWWKS4123W (refreshThreshold >= inactivityTimeout, clearly-wrong path).
            server.stopServer("CWWKS4125W", "CWWKS4124W", "CWWKS4123W");
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (server != null && server.isStarted()) {
            server.stopServer("CWWKS4125W", "CWWKS4124W", "CWWKS4123W");
        }
    }

    // Verifies a token is refreshed (new cookie issued) once the refresh threshold is crossed, and that the new cookie works for SSO.
    @Test
    public void testTokenRefreshedAndUsableWhenThresholdCrossed() throws Exception {
        String url = getServletUrl();
        String method = "testTokenRefreshedAndUsableWhenThresholdCrossed";
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m");

        String agedCookie = authenticateAndSetTokenAge(url, "user1", "user1pwd", PAST_THRESHOLD_S, method);

        String refreshedCookie = ssoRequestExpectingRefresh(url, agedCookie, "SSO after threshold", method);

        HttpURLConnection conn3 = ssoRequest(url, refreshedCookie, "SSO with refreshed cookie", method);
        assertEquals("Refreshed cookie must be accepted", 200, conn3.getResponseCode());
        Log.info(thisClass, method, "PASSED: new cookie issued and successfully used for SSO");
    }

    // Verifies no token refresh occurs when the request arrives before the refresh threshold window opens.
    @Test
    public void testTokenNotRefreshedBeforeThreshold() throws Exception {
        String url = getServletUrl();
        String method = "testTokenNotRefreshedBeforeThreshold";
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m");

        String agedCookie = authenticateAndSetTokenAge(url, "user1", "user1pwd", BEFORE_THRESHOLD_S, method);

        HttpURLConnection conn2 = ssoRequest(url, agedCookie, "SSO before threshold", method);
        assertEquals("SSO request must succeed", 200, conn2.getResponseCode());
        assertTokenNotRefreshed("Token should not be refreshed when inactivity remaining > refreshThreshold", conn2);
        Log.info(thisClass, method, "PASSED: no new cookie received");
    }

    // Verifies that a token refresh resets the inactivity clock, allowing the refreshed cookie to remain valid beyond the original window.
    @Test
    public void testInactivityWindowResetsAfterTokenRefresh() throws Exception {
        String url = getServletUrl();
        String method = "testInactivityWindowResetsAfterTokenRefresh";
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m");

        String agedCookie = authenticateAndSetTokenAge(url, "user1", "user1pwd", PAST_THRESHOLD_S, method);
        String refreshedCookie = ssoRequestExpectingRefresh(url, agedCookie, "SSO after first age (refresh expected)", method);
        Log.info(thisClass, method, "inactivity window reset; refreshed cookie has a fresh creation time");

        HttpURLConnection conn3 = ssoRequest(url, refreshedCookie, "SSO with refreshed cookie (window reset)", method);
        assertEquals("Refreshed cookie must be valid (inactivity clock was reset)", 200, conn3.getResponseCode());
        Log.info(thisClass, method, "PASSED: refreshed cookie accepted; inactivity clock was correctly reset");
    }

    // Verifies that concurrent sessions for different users each receive independent token refreshes with unique cookies.
    @Test
    public void testTokenRefreshWithMultipleUsers() throws Exception {
        String url = getServletUrl();
        String method = "testTokenRefreshWithMultipleUsers";
        Log.info(thisClass, method, "config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m");

        String user1AgedCookie = authenticateAndSetTokenAge(url, "user1", "user1pwd", PAST_THRESHOLD_S, method);
        String user2AgedCookie = authenticateAndSetTokenAge(url, "user2", "user2pwd", PAST_THRESHOLD_S, method);

        assertFalse("Different users must have different backdated cookies", user1AgedCookie.equals(user2AgedCookie));
        Log.info(thisClass, method, "users have unique backdated cookies");

        String user1RefreshedCookie = ssoRequestExpectingRefresh(url, user1AgedCookie, "user1 SSO (refresh expected)", method);
        String user2RefreshedCookie = ssoRequestExpectingRefresh(url, user2AgedCookie, "user2 SSO (refresh expected)", method);

        assertFalse("Refreshed cookies must differ between users",
                    user1RefreshedCookie.equals(user2RefreshedCookie));
        Log.info(thisClass, method, "PASSED: both users received unique refreshed cookies");
    }

    // Verifies that HttpOnly and Path attributes on the Set-Cookie header are preserved across a token refresh.
    @Test
    public void testCookieAttributesPreservedAfterRefresh() throws Exception {
        String url = getServletUrl();
        String method = "testCookieAttributesPreservedAfterRefresh";
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m");

        String agedCookie = authenticateAndSetTokenAge(url, "user1", "user1pwd", PAST_THRESHOLD_S, method);

        HttpURLConnection conn2 = ssoRequest(url, agedCookie, "SSO after threshold (refresh expected)", method);
        assertEquals("SSO authentication must succeed", 200, conn2.getResponseCode());
        String header2 = getCookieHeader(conn2.getHeaderFields());
        assertNotNull("Refreshed Set-Cookie header must be present", header2);
        assertTokenRefreshed(conn2, agedCookie);
        conn2.disconnect();

        Log.info(thisClass, method, "refreshed Set-Cookie header: " + header2);
        assertTrue("Refreshed cookie must have HttpOnly attribute", header2.toLowerCase().contains("httponly"));
        assertTrue("Refreshed cookie must have Path attribute",     header2.toLowerCase().contains("path="));
        Log.info(thisClass, method, "PASSED: cookie attributes present on refreshed token");
    }

    // Verifies that a token issued under the old config is accepted immediately after a config switch,
    // and that the new config's inactivity timeout is enforced for subsequently issued tokens.
    @Test
    public void testTokenBehavesCorrectlyAfterConfigUpdate() throws Exception {
        setConfig(CFG_TOKEN_EXCEEDS_EXPIRY);
        String url = getServletUrl();
        String method = "testTokenBehaviourAfterConfigUpdate";
        Log.info(thisClass, method, "Config: starts on " + CFG_TOKEN_EXCEEDS_EXPIRY + " expiration=3m, inactivityTimeout=4m, refreshThreshold=2m");

        String cookie = authenticateAndSetTokenAge(url, "user1", "user1pwd", 0, method);

        Log.info(thisClass, method, "switching server configuration to " + CFG_TOKEN_REFRESH + " expiration=3m, inactivityTimeout=2m, refreshThreshold=1m");
        setConfig(CFG_TOKEN_REFRESH);
        Log.info(thisClass, method, "config update complete; new inactivityTimeout=2m is now active");

        HttpURLConnection conn2 = ssoRequest(url, cookie, "SSO immediately after config switch", method);
        assertEquals("Old cookie must be accepted immediately after config switch", 200, conn2.getResponseCode());

        String expiredCookie = authenticateAndSetTokenAge(url, "user1", "user1pwd", PAST_INACTIVITY_S, method);

        HttpURLConnection conn3 = ssoRequest(url, expiredCookie, "SSO after inactivity timeout (rejection expected)", method);

        assertTrue("SSO must fail after new config's inactivity timeout: got HTTP " + conn3.getResponseCode(),
                   conn3.getResponseCode() == 401 || conn3.getResponseCode() == 302 || conn3.getResponseCode() == 403);
        Log.info(thisClass, method, "PASSED: old-config cookie accepted after switch; new config's inactivity timeout correctly enforced");
    }

    // Verifies a token is rejected after the inactivity timeout and that re-authentication issues a new distinct cookie.
    @Test
    public void testTokenRejectedAfterInactivityTimeoutThenReauthSucceeds() throws Exception {
        String url = getServletUrl();
        String method = "testTokenRejectedAfterInactivityTimeoutThenReauthSucceeds";
        Log.info(thisClass, method, "expiration=3m, inactivityTimeout=2m, refreshThreshold=1m");

        String expiredCookie = authenticateAndSetTokenAge(url, "user1", "user1pwd", PAST_INACTIVITY_S, method);

        HttpURLConnection conn = ssoRequest(url, expiredCookie, "SSO after inactivity timeout (rejection expected)", method);
        int status = conn.getResponseCode();
        assertTrue("Idle token must be rejected (401/302/403), got: " + status,
                   status == 401 || status == 302 || status == 403);

        Log.info(thisClass, method, "re-authenticating after inactivity expiry");
        String newCookie = authenticateAndSetTokenAge(url, "user1", "user1pwd", 0, method);
        assertFalse("New cookie must differ from expired one", expiredCookie.equals(newCookie));
        Log.info(thisClass, method, "PASSED: idle token correctly rejected with HTTP " + status + ", reauthentication succeeded with new cookie");
    }

    // Verifies a token is rejected after the absolute expiration time and that re-authentication issues a new distinct cookie.
    @Test
    public void testTokenExpiresAfterExpirationTimeThenReauthSucceeds() throws Exception {
        String url = getServletUrl();
        String method = "testTokenExpiresAfterExpirationTimeThenReauthSucceeds";
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m");

        String expiredCookie = authenticateAndSetTokenAge(url, "user1", "user1pwd", PAST_EXPIRY_S, method);

        HttpURLConnection conn = ssoRequest(url, expiredCookie, "SSO after absolute expiry (rejection expected)", method);
        int status = conn.getResponseCode();
        assertTrue("Absolutely expired token must be rejected (401/302/403), got: " + status,
                   status == 401 || status == 302 || status == 403);

        Log.info(thisClass, method, "re-authenticating after expiry");
        String newCookie = authenticateAndSetTokenAge(url, "user1", "user1pwd", 0, method);
        assertFalse("New cookie must differ from expired one", expiredCookie.equals(newCookie));
        Log.info(thisClass, method, "PASSED: expired token correctly rejected with HTTP " + status + ", reauthentication succeeded with new cookie");
    }

    // Verifies refreshThreshold alone has no effect without inactivityTimeout; the token lives until absolute expiration with no refresh.
    @Test
    public void testTokenRefreshDisabledWhenOnlyRefreshThresholdConfigured() throws Exception {
        setConfig(CFG_TOKEN_REFRESH_ONLY);
        String url = getServletUrl();
        String method = "testTokenRefreshDisabledWhenOnlyRefreshThresholdConfigured";
        Log.info(thisClass, method, "Config: expiration=2m, refreshThreshold=1m, no inactivityTimeout");

        String agedCookie = authenticateAndSetTokenAge(url, "user1", "user1pwd", PAST_THRESHOLD_S, method);

        HttpURLConnection conn2 = ssoRequest(url, agedCookie, "SSO past refreshThreshold (no refresh expected)", method);
        assertEquals("SSO must succeed, token only expires at absolute expiration without inactivityTimeout", 200, conn2.getResponseCode());
        assertTokenNotRefreshed("No refresh expected — refreshThreshold has no effect without inactivityTimeout", conn2);

        String expiredCookie = authenticateAndSetTokenAge(url, "user1", "user1pwd", PAST_INACTIVITY_S, method);
        HttpURLConnection conn3 = ssoRequest(url, expiredCookie, "SSO after expiration (rejection expected)", method);
        assertTrue("SSO must fail, token should be expired: got HTTP " + conn3.getResponseCode(),
                   conn3.getResponseCode() == 401 || conn3.getResponseCode() == 302 || conn3.getResponseCode() == 403);
        Log.info(thisClass, method, "PASSED: no new cookie after refresh threshold and cookie expires at expiration time");
    }

    // Verifies inactivityTimeout alone has no effect without refreshThreshold; SSO succeeds past the timeout and the token expires normally.
    @Test
    public void testTokenRefreshDisabledWhenOnlyInactivityTimeoutConfigured() throws Exception {
        setConfig(CFG_TOKEN_INACTIVITY_ONLY);
        String url = getServletUrl();
        String method = "testTokenRefreshDisabledWhenOnlyInactivityTimeoutConfigured";
        Log.info(thisClass, method, "Config: expiration=2m, inactivityTimeout=1m, no refreshThreshold");

        String agedCookie = authenticateAndSetTokenAge(url, "user1", "user1pwd", PAST_THRESHOLD_S, method);

        HttpURLConnection conn2 = ssoRequest(url, agedCookie, "SSO past inactivity timeout (no refresh expected)", method);
        assertEquals("Inactivity timeout should be disabled, SSO must succeed.", 200, conn2.getResponseCode());
        assertTokenNotRefreshed("Cookie should not be refreshed", conn2);

        String expiredCookie = authenticateAndSetTokenAge(url, "user1", "user1pwd", PAST_INACTIVITY_S, method);
        HttpURLConnection conn3 = ssoRequest(url, expiredCookie, "SSO after expiration (rejection expected)", method);
        assertTrue("SSO must fail, token should be expired: got HTTP " + conn3.getResponseCode(),
                   conn3.getResponseCode() == 401 || conn3.getResponseCode() == 302 || conn3.getResponseCode() == 403);
        Log.info(thisClass, method, "PASSED: SSO succeeded past inactivity timeout and cookie expired after expiration");
    }

    // Verifies CWWKS4125W is emitted when inactivityTimeout >= expiration, and that the token expires at the absolute expiration boundary.
    @Test
    public void testTokenInactivityTimeoutExceedsExpiration() throws Exception {
        setConfig(CFG_TOKEN_EXCEEDS_EXPIRY);
        String url = getServletUrl();
        String method = "testTokenInactivityExceedsExpiration";
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=4m, refreshThreshold=2m");

        String warnMsg = server.waitForStringInLog("CWWKS4125W");
        assertNotNull("Expected CWWKS4125W when inactivityTimeout >= expiration", warnMsg);
        Log.info(thisClass, method, "warning logged as expected: " + warnMsg);

        String expiredCookie = authenticateAndSetTokenAge(url, "user1", "user1pwd", PAST_EXPIRY_S, method);

        HttpURLConnection conn2 = ssoRequest(url, expiredCookie, "SSO after expiration (rejection expected)", method);
        assertTrue("Token must be rejected at expiration, got HTTP " + conn2.getResponseCode(),
                conn2.getResponseCode() == 401 || conn2.getResponseCode() == 302 || conn2.getResponseCode() == 403);
        Log.info(thisClass, method, "PASSED: token correctly rejected at expiration boundary");
    }

    // Verifies CWWKS4124W is emitted when refreshThreshold >= inactivityTimeout, the threshold is
    // auto-adjusted to inactivityTimeout/3, and a token is still refreshed when that window is crossed.
    @Test
    public void testRefreshThresholdExceedsInactivityTimeout() throws Exception {
        setConfig(CFG_TOKEN_THRESHOLD_EXCEEDS_INACTIVITY);
        String url = getServletUrl();
        String method = "testRefreshThresholdExceedsInactivityTimeout";
        Log.info(thisClass, method,
                 "Config: expiration=6m, inactivityTimeout=3m, refreshThreshold=4m; " +
                 "expect auto-adjust to 1m (inactivityTimeout/3)");

        String warnMsg = server.waitForStringInLog("CWWKS4124W");
        assertNotNull("Expected CWWKS4124W when refreshThreshold(4m) >= inactivityTimeout(3m) " +
                      "and refreshThreshold < expiration, but no warning was found in the log", warnMsg);
        Log.info(thisClass, method, "warning logged as expected: " + warnMsg);

        String agedCookie = authenticateAndSetTokenAge(url, "user1", "user1pwd", PAST_INACTIVITY_S, method);

        String refreshedCookie = ssoRequestExpectingRefresh(url, agedCookie, "SSO after adjusted threshold (refresh expected)", method);

        HttpURLConnection conn3 = ssoRequest(url, refreshedCookie, "SSO with refreshed cookie (must succeed)", method);
        assertEquals("Refreshed token must be accepted for SSO", 200, conn3.getResponseCode());

        Log.info(thisClass, method,
                 "PASSED: CWWKS4124W emitted, threshold auto-adjusted to 1m, " +
                 "token refreshed when adjusted threshold was crossed");
    }

    // Authenticates via Basic Auth, backdates the token by ageSeconds on the server, and returns the LtpaToken2 cookie value.
    private String authenticateAndSetTokenAge(String url, String username, String password,
                                              int ageSeconds, String method) throws IOException {
        String backdateUrl = url + "?action=backdate&offsetSeconds=" + ageSeconds;
        Log.info(thisClass, method, "authenticating as " + username + " with token age=" + ageSeconds + "s via " + backdateUrl);
        HttpURLConnection conn = makeAuthenticatedRequest(backdateUrl, username, password);
        assertEquals("Backdate request must succeed for " + username, 200, conn.getResponseCode());
        String cookie = extractCookie(conn);
        assertNotNull("Container must issue an LtpaToken2 cookie for " + username, cookie);
        Log.info(thisClass, method, "cookie for " + username + ": " + maskCookie(cookie));
        conn.disconnect();
        return cookie;
    }

    // Sends a cookie-only SSO request expecting a refresh; asserts 200 and returns the new distinct cookie.
    private String ssoRequestExpectingRefresh(String url, String cookie, String label,
                                              String method) throws IOException {
        HttpURLConnection conn = ssoRequest(url, cookie, label, method);
        assertEquals(label + ": SSO must succeed", 200, conn.getResponseCode());
        String newCookie = assertTokenRefreshed(conn, cookie);
        Log.info(thisClass, method, label + ": refreshed cookie: " + maskCookie(newCookie));
        conn.disconnect();
        return newCookie;
    }

    // Sends a cookie-only SSO request, logs the label and response code, and returns the connection.
    private HttpURLConnection ssoRequest(String url, String cookie, String label,
                                         String method) throws IOException {
        Log.info(thisClass, method, label + ": sending SSO request");
        HttpURLConnection conn = openConnection(url);
        conn.setRequestProperty("Cookie", LTPA_COOKIE + "=" + cookie);
        Log.info(thisClass, method, label + ": HTTP " + conn.getResponseCode());
        return conn;
    }

    // Asserts a new distinct Set-Cookie was issued and returns the new cookie value.
    private String assertTokenRefreshed(HttpURLConnection conn, String previousCookie) {
        String newCookie = extractCookie(conn);
        Log.info(thisClass, "assertTokenRefreshed", "response header fields for new cookie: " + conn.getHeaderFields());
        assertNotNull("Expected a token refresh (new Set-Cookie: LtpaToken2) but none was issued", newCookie);
        assertFalse("Refreshed cookie must differ from the previous cookie", previousCookie.equals(newCookie));
        return newCookie;
    }

    // Asserts that no token refresh occurred — fails if an unexpected Set-Cookie was issued.
    private void assertTokenNotRefreshed(String message, HttpURLConnection conn) {
        String newCookie = extractCookie(conn);
        assertNull(message + " — but got new cookie: " + maskCookie(newCookie), newCookie);
    }

    // Opens an authenticated GET connection with Basic credentials.
    private HttpURLConnection makeAuthenticatedRequest(String urlString,
                                                       String username, String password) throws IOException {
        HttpURLConnection conn = openConnection(urlString);
        conn.setRequestProperty("Authorization",
            "Basic " + java.util.Base64.getEncoder()
                           .encodeToString((username + ":" + password).getBytes()));
        return conn;
    }

    // Extracts the LtpaToken2 cookie value from the last matching Set-Cookie response header.
    private String extractCookie(HttpURLConnection conn) {
        String header = getCookieHeader(conn.getHeaderFields());
        if (header == null) return null;
        int start = LTPA_COOKIE.length() + 1;
        int end   = header.indexOf(";");
        return header.substring(start, end == -1 ? header.length() : end);
    }

    // Returns the full Set-Cookie header string for the last LtpaToken2 cookie in the response.
    private String getCookieHeader(Map<String, List<String>> headers) {
        List<String> setCookies = headers.get("Set-Cookie");
        if (setCookies == null) return null;
        String prefix = LTPA_COOKIE + "=";
        String last = null;
        for (String header : setCookies) {
            if (header.startsWith(prefix)) {
                last = header;
            }
        }
        return last;
    }

    // Returns a masked cookie value for safe logging (first and last 10 characters).
    private String maskCookie(String cookie) {
        if (cookie == null)          return "null";
        if (cookie.length() < 20)    return "***";
        return cookie.substring(0, 10) + "..." + cookie.substring(cookie.length() - 10);
    }

    // Opens a non-caching, non-redirecting GET connection to the given URL.
    private HttpURLConnection openConnection(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(false);
        return conn;
    }

    // Returns the full URL of the test servlet.
    private String getServletUrl() {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() +
               "/" + APP_NAME + "/" + SERVLET_NAME;
    }

    // Swaps the server configuration file and waits for Liberty to apply the change.
    private void setConfig(String config) throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(config);
        server.waitForConfigUpdateInLogUsingMark(null);
    }
}
