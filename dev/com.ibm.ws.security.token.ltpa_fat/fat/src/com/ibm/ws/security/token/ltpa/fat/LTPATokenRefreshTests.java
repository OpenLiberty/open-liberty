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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
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
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;
import componenttest.annotation.SkipForRepeat;


@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
public class LTPATokenRefreshTests {

    private static final String APP_NAME     = "ltpaTest";
    private static final String SERVLET_NAME = "LTPATestServlet";
    private static final String LTPA_COOKIE  = "LtpaToken2";
    private static final Class<?> thisClass  = LTPATokenRefreshTests.class;

    private static LibertyServer server;

    private static final long REFRESH_WAIT_MS       = 61_000;
    private static final long INACTIVITY_WAIT_MS    = 121_000;
    private static final long HALF_WINDOW_MS        = 30_000;
    private static final long ABSOLUTE_EXPIRY_WAIT_MS = 241_000;

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
        server.addInstalledAppForValidation(APP_NAME);
    }

    @Before
    public void setUp() throws Exception {
        // Enable beta edition so ProductInfo.getBetaEdition() returns true,
        // activating inactivityTimeout / refreshThreshold in LTPAToken2 and LTPAConfigurationImpl.
        server.setJvmOptions(Arrays.asList("-Dcom.ibm.ws.beta.edition=true"));
        server.setServerConfigurationFile("serverTokenRefresh.xml");
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

    @Test
    public void testTokenRefreshedAndUsableWhenThresholdCrossed() throws Exception {
        String url = getServletUrl();
        String method = "testTokenRefreshedAndUsableWhenThresholdCrossed";
        Log.info(thisClass, method, "Config: expiration=4m, inactivityTimeout=2m, refreshThreshold=1m");

        HttpURLConnection conn1 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        int status1 = conn1.getResponseCode();
        Log.info(thisClass, method, "authentication response: HTTP " + status1);
        assertEquals("Initial authentication must succeed", 200, status1);
        String cookie = extractCookie(conn1);
        Log.info(thisClass, method, "initial LTPA cookie: " + maskCookie(cookie));
        assertNotNull("LTPA cookie must be set after authentication", cookie);
        conn1.disconnect();

        Log.info(thisClass, method, "waiting " + (REFRESH_WAIT_MS/1000) + "s to cross the refresh threshold");
        Thread.sleep(REFRESH_WAIT_MS);

        Log.info(thisClass, method, "sending SSO request: expecting server to return new cookie");
        HttpURLConnection conn2 = makeRequestWithCookie(url, cookie);
        int status2 = conn2.getResponseCode();
        Log.info(thisClass, method, "SSO response: HTTP " + status2);
        assertEquals("SSO request must succeed", 200, status2);
        String refreshedCookie = assertTokenRefreshed(conn2);
        Log.info(thisClass, method, "refreshed LTPA cookie: " + maskCookie(refreshedCookie));
        assertFalse("Refreshed cookie must differ from original", cookie.equals(refreshedCookie));
        conn2.disconnect();

        Log.info(thisClass, method, "sending SSO with the refreshed cookie");
        HttpURLConnection conn3 = makeRequestWithCookie(url, refreshedCookie);
        int status3 = conn3.getResponseCode();
        Log.info(thisClass, method, "SSO response: HTTP " + status3);
        assertEquals("Refreshed clone must be accepted", 200, status3);
        Log.info(thisClass, method, "PASSED: new cookie issued and successfully used for SSO");
        conn3.disconnect();
    }

    @Test
    public void testTokenNotRefreshedBeforeThreshold() throws Exception {
        String url = getServletUrl();
        String method = "testTokenNotRefreshedBeforeThreshold";
        Log.info(thisClass, method, "Config: expiration=4m, inactivityTimeout=2m, refreshThreshold=1m");

        HttpURLConnection conn1 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        int status1 = conn1.getResponseCode();
        Log.info(thisClass, method, "authentication response: HTTP " + status1);
        assertEquals("Initial authentication must succeed", 200, status1);
        String cookie = extractCookie(conn1);
        Log.info(thisClass, method, "LTPA cookie received: " + maskCookie(cookie));
        assertNotNull("LTPA cookie must be set after authentication", cookie);
        conn1.disconnect();

        Log.info(thisClass, method, "Waiting " + HALF_WINDOW_MS + ": letting token age 30s (before refresh window)");
        Thread.sleep(HALF_WINDOW_MS);

        Log.info(thisClass, method, "sending SSO request with LTPA cookie");
        HttpURLConnection conn2 = makeRequestWithCookie(url, cookie);
        int status2 = conn2.getResponseCode();
        Log.info(thisClass, method, "SSO response: HTTP " + status2);
        assertEquals("SSO request must succeed", 200, status2);
        assertTokenNotRefreshed("Token should not be refreshed when inactivity remaining > refreshThreshold", conn2);
        Log.info(thisClass, method, "PASSED: no new cookie received");
        conn2.disconnect();
    }
    
    @Test
    public void testInactivityWindowResetsAfterTokenRefresh() throws Exception {
        String url = getServletUrl();
        String method = "testInactivityWindowResetsOnTokenRefresh";
        Log.info(thisClass, method, "Config: expiration=4m, inactivityTimeout=2m, refreshThreshold=1m");
        
        HttpURLConnection conn1 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        int status1 = conn1.getResponseCode();
        Log.info(thisClass, method, "initial authentication response: HTTP " + status1);
        assertEquals("Initial authentication must succeed", 200, status1);
        String cookie = extractCookie(conn1);
        Log.info(thisClass, method, "initial LTPA cookie: " + maskCookie(cookie));
        assertNotNull("Must receive initial cookie", cookie);
        conn1.disconnect();

        // First wait — crosses threshold, clone resets the inactivity clock
        Log.info(thisClass, method, "first wait: " + (REFRESH_WAIT_MS/1000) + "s to cross the 1-minute refresh threshold");
        Thread.sleep(REFRESH_WAIT_MS);

        Log.info(thisClass, method, "sending SSO request after first wait; new cookie EXPECTED");
        HttpURLConnection conn2 = makeRequestWithCookie(url, cookie);
        int status2 = conn2.getResponseCode();
        Log.info(thisClass, method, "SSO response: HTTP " + status2);
        assertEquals("Request after first wait must succeed", 200, status2);
        String refreshedCookie = assertTokenRefreshed(conn2);
        Log.info(thisClass, method, "new cookie received: " + maskCookie(refreshedCookie) + " ; inactivity window should reset");
        conn2.disconnect();

        // Second wait — same 61s from the reset point; window was reset so token still valid
        Log.info(thisClass, method, "second wait: " + (REFRESH_WAIT_MS/1000) + "s from the reset point " +
                 "(cumulative idle from original auth is about 122s, which exceeds 2 minutes, but the window was reset by the refreshed cookie)");
        Thread.sleep(REFRESH_WAIT_MS);

        Log.info(thisClass, method, "sending SSO request after second wait using refreshed cookie");
        HttpURLConnection conn3 = makeRequestWithCookie(url, refreshedCookie);
        int status3 = conn3.getResponseCode();
        Log.info(thisClass, method, "SSO response: HTTP " + status3);
        assertEquals("Token must still be valid after second wait (window was reset)", 200, status3);
        Log.info(thisClass, method, "PASSED: token accepted after about 122s cumulative idle because the refresh reset the window");
        conn3.disconnect();
    }

    @Test
    public void testTokenRefreshWithMultipleUsers() throws Exception {
        String url = getServletUrl();
        String method = "testTokenRefreshWithMultipleUsers";
        Log.info(thisClass, method, "config: expiration=4m, inactivityTimeout=2m, refreshThreshold=1m");

        Log.info(thisClass, method, "authenticating as user1");
        HttpURLConnection conn1 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        int status1 = conn1.getResponseCode();
        Log.info(thisClass, method, "user1 authentication response: HTTP " + status1);
        assertEquals("User1 authentication must succeed", 200, status1);
        String user1Cookie = extractCookie(conn1);
        Log.info(thisClass, method, "user1 LTPA cookie: " + maskCookie(user1Cookie));
        assertNotNull("user1 must receive LTPA cookie", user1Cookie);
        conn1.disconnect();

        Log.info(thisClass, method, "authenticating as user2");
        HttpURLConnection conn2 = makeAuthenticatedRequest(url, null, "user2", "user2pwd");
        int status2 = conn2.getResponseCode();
        Log.info(thisClass, method, "user2 authentication response: HTTP " + status2);
        assertEquals("User2 authentication must succeed", 200, status2);
        String user2Cookie = extractCookie(conn2);
        Log.info(thisClass, method, "user2 LTPA cookie: " + maskCookie(user2Cookie));
        assertNotNull("user2 must receive LTPA cookie", user2Cookie);
        conn2.disconnect();

        assertFalse("Different users must have different cookies", user1Cookie.equals(user2Cookie));
        Log.info(thisClass, method, "users have unique cookies");

        Log.info(thisClass, method, "waiting " + (REFRESH_WAIT_MS/1000) + "s to cross the 1-minute refreshthreshold for both users");
        Thread.sleep(REFRESH_WAIT_MS);

        Log.info(thisClass, method, "sending SSO request for user1 after wait; new cookie EXPECTED");
        HttpURLConnection conn3 = makeRequestWithCookie(url, user1Cookie);
        int status3 = conn3.getResponseCode();
        Log.info(thisClass, method, "user1 SSO response: HTTP " + status3);
        assertEquals("user1 SSO must succeed", 200, status3);
        String user1RefreshedCookie = assertTokenRefreshed(conn3);
        Log.info(thisClass, method, "user1 refreshed cookie: " + maskCookie(user1RefreshedCookie));
        conn3.disconnect();

        Log.info(thisClass, method, "sending SSO request for user2 after wait; new cookie EXPECTED");
        HttpURLConnection conn4 = makeRequestWithCookie(url, user2Cookie);
        int status4 = conn4.getResponseCode();
        Log.info(thisClass, method, "user2 SSO response: HTTP " + status4);
        assertEquals("user2 SSO after wait must succeed", 200, status4);
        String user2RefreshedCookie = assertTokenRefreshed(conn4);
        Log.info(thisClass, method, "user2 refreshed cookie: " + maskCookie(user2RefreshedCookie));
        conn4.disconnect();

        assertFalse("Refreshed cookies must differ between users",
                    user1RefreshedCookie.equals(user2RefreshedCookie));
        Log.info(thisClass, method, "PASSED: both users received unique refreshed cookies");
    }

    @Test
    public void testCookieAttributesPreservedAfterRefresh() throws Exception {
        String url = getServletUrl();
        String method = "testCookieAttributesPreservedDuringRefresh";
        Log.info(thisClass, method, "Config: expiration=4m, inactivityTimeout=2m, refreshThreshold=1m");

        HttpURLConnection conn1 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        int status1 = conn1.getResponseCode();
        Log.info(thisClass, method, "authentication response: HTTP " + status1);
        assertEquals("Authentication must succeed", 200, status1);

        String header1 = getCookieHeader(conn1.getHeaderFields());
        assertNotNull("Must have Set-Cookie header after authentication", header1);
        boolean initHttpOnly = header1.toLowerCase().contains("httponly");
        boolean initSecure   = header1.toLowerCase().contains("secure");
        boolean initHasPath  = header1.toLowerCase().contains("path=");

        String cookie = extractCookie(conn1);
        assertNotNull("Must have LTPA cookie", cookie);
        conn1.disconnect();

        Log.info(thisClass, method, "initial Set-Cookie header: " + header1);
        Log.info(thisClass, method, "initial attributes: HttpOnly=" + initHttpOnly +
                 ", Secure=" + initSecure + ", Path=" + initHasPath);
        Log.info(thisClass, method, "waiting " + (REFRESH_WAIT_MS/1000) + "s to cross the 1-minute refresh threshold");
        Thread.sleep(REFRESH_WAIT_MS);

        Log.info(thisClass, method, "sending SSO request after wait; expecting clone with preserved attributes");
        HttpURLConnection conn2 = makeRequestWithCookie(url, cookie);
        int status2 = conn2.getResponseCode();
        Log.info(thisClass, method, "SSO response: HTTP " + status2);
        assertEquals("SSO authentication must succeed", 200, status2);
        
        String header2 = getCookieHeader(conn2.getHeaderFields());
        assertNotNull("Refreshed Set-Cookie header must be present", header2);
        assertTokenRefreshed(conn2);
        conn2.disconnect();

        Log.info(thisClass, method, "refreshed Set-Cookie header: " + header2);
        boolean refreshHttpOnly = header2.toLowerCase().contains("httponly");
        boolean refreshSecure   = header2.toLowerCase().contains("secure");
        boolean refreshHasPath  = header2.toLowerCase().contains("path=");
        Log.info(thisClass, method, "refreshed attributes: HttpOnly=" + refreshHttpOnly +
                ", Secure=" + refreshSecure + ", Path=" + refreshHasPath);

        assertEquals("HttpOnly must be preserved", initHttpOnly, refreshHttpOnly);
        assertEquals("Secure must be preserved",   initSecure,   refreshSecure);
        assertEquals("Path must be preserved",     initHasPath,  refreshHasPath);
        Log.info(thisClass, method, "PASSED: all cookie attributes preserved across refresh");
    }

    @Test
    public void testTokenBehavesCorrectlyAfterConfigUpdate() throws Exception {
        setConfig("serverTokenTimeoutExceedsExpiration.xml");
        String url = getServletUrl();
        String method = "testTokenBehaviourAfterConfigUpdate";
        Log.info(thisClass, method, "Config: starts on serverTokenTimeoutExceedsExpiration.xml expiration=2m, inactivityTimeout=3m, refreshThreshold=1m");

        HttpURLConnection conn1 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        int status1 = conn1.getResponseCode();
        Log.info(thisClass, method, "initial authentication response: HTTP " + status1);
        String cookie = extractCookie(conn1);
        Log.info(thisClass, method, "initial LTPA cookie: " + maskCookie(cookie));
        assertNotNull("Must receive initial cookie", cookie);
        conn1.disconnect();

        Log.info(thisClass, method, "switching server configuration to serverTokenRefresh.xml expiration=4m, inactivityTimeout=2m, refreshThreshold=1m");
        setConfig("serverTokenRefresh.xml");
        Thread.sleep(500);
        Log.info(thisClass, method, "config update complete; new inactivityTimeout=2m is now active");

        Log.info(thisClass, method, "sending SSO request; old cookie must still be valid immediately after configuration switch");
        HttpURLConnection conn2 = makeRequestWithCookie(url, cookie);
        int status2 = conn2.getResponseCode();
        Log.info(thisClass, method, "SSO response: HTTP " + status2);
        assertEquals("Old cookie must be accepted immediately after config switch", 200, status2);
        conn2.disconnect();

        Log.info(thisClass, method, "waiting " + (REFRESH_WAIT_MS/1000) + "s to cross the refresh threshold under new config");
        Thread.sleep(REFRESH_WAIT_MS);

        HttpURLConnection conn3 = makeRequestWithCookie(url, cookie);
        int status3 = conn3.getResponseCode();
        Log.info(thisClass, method, "SSO authentication response: HTTP " + status3);
        assertEquals("SSO must succeed after config update", 200, status3);
        String refreshedCookie = extractCookie(conn3);
        Log.info(thisClass, method, "new LTPA cookie: " + maskCookie(refreshedCookie));
        assertTokenRefreshed(conn3);
        conn3.disconnect();

        Log.info(thisClass, method, "waiting another " + (INACTIVITY_WAIT_MS/1000) + "s to pass inactivity timeout");
        Thread.sleep(INACTIVITY_WAIT_MS);
        Log.info(thisClass, method, "sending SSO request; token must be expired");
        HttpURLConnection conn4 = makeRequestWithCookie(url, cookie);
        int status4 = conn4.getResponseCode();
        Log.info(thisClass, method, "SSO response: HTTP " + status4);
        assertTrue("SSO must fail, token should be expired: got HTTP " + status4,
                   status4 == 401 || status4 == 302 || status4 == 403);
        Log.info(thisClass, method, "PASSED: token issued under old config was cloned under new config's refreshThreshold and expired after new config's inactivity timeout");
        conn4.disconnect();
    }

    @Test
    public void testTokenRejectedAfterIdleTimeoutThenReauthSucceeds() throws Exception {
        String url = getServletUrl();
        String method = "testTokenRejectedAfterIdleTimeoutThenReauthSucceeds";
        Log.info(thisClass, method, "expiration=4m, inactivityTimeout=2m, refreshThreshold=1m");

        HttpURLConnection conn1 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        int status1 = conn1.getResponseCode();
        Log.info(thisClass, method, "authentication response: HTTP " + status1);
        assertEquals("Initial authentication must succeed", 200, status1);
        String cookie = extractCookie(conn1);
        Log.info(thisClass, method, "initial LTPA cookie: " + maskCookie(cookie));
        assertNotNull("LTPA cookie must be set", cookie);
        conn1.disconnect();

        Log.info(thisClass, method, "sleeping " + (INACTIVITY_WAIT_MS/1000) + "s to exceed the 2-minute inactivity timeout ");
        Thread.sleep(INACTIVITY_WAIT_MS);
        Log.info(thisClass, method, "sending SSO request");
        HttpURLConnection conn2 = makeRequestWithCookie(url, cookie);
        int status2 = conn2.getResponseCode();
        Log.info(thisClass, method, "SSO response: HTTP " + status2);
        conn2.disconnect();
        assertTrue("Idle token must be rejected (401/302/403), got: " + status2,
                   status2 == 401 || status2 == 302 || status2 == 403);

        Log.info(thisClass, method, "re-authenticating after inactivity expiry");
        HttpURLConnection conn3 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        int status3 = conn3.getResponseCode();
        Log.info(thisClass, method, "reauthentication response: HTTP " + status3);
        assertEquals("Reauthentication after inactivty timeout must succeed", 200, status3);

        String newCookie = extractCookie(conn3);
        Log.info(thisClass, method, "new LTPA cookie after re-authentication: " + maskCookie(newCookie));
        assertNotNull("Must receive a new cookie after re-authentication", newCookie);
        assertFalse("New cookie must differ from expired one", cookie.equals(newCookie));
        Log.info(thisClass, method, "PASSED: idle token correctly rejected with HTTP " + status2 + ", reauthentication succeeded with new cookie");
    }

    @Test
    public void testTokenExpiresAfterExpirationTimeThenReauthSucceeds() throws Exception {
        String url = getServletUrl();
        String method = "testTokenExpiresAfterExpirationTimeThenReauthSucceeds";
        Log.info(thisClass, method, "Config: expiration=4m, inactivityTimeout=2m, refreshThreshold=1m");
        
        HttpURLConnection conn1 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        int status1 = conn1.getResponseCode();
        Log.info(thisClass, method, "authentication response: HTTP " + status1);
        assertEquals("Authentication must succeed", 200, status1);
        String cookie = extractCookie(conn1);
        Log.info(thisClass, method, "initial LTPA cookie: " + maskCookie(cookie));
        assertNotNull("Must receive cookie", cookie);
        conn1.disconnect();

        Log.info(thisClass, method, "waiting " + (ABSOLUTE_EXPIRY_WAIT_MS/1000) + "s to exceed the 4-minute absolute expiry");
        Thread.sleep(ABSOLUTE_EXPIRY_WAIT_MS);

        Log.info(thisClass, method, "sending SSO request; token must be REJECTED");
        HttpURLConnection conn2 = makeRequestWithCookie(url, cookie);
        int status2 = conn2.getResponseCode();
        Log.info(thisClass, method, "SSO response: HTTP " + status2);
        conn2.disconnect();
        assertTrue("Absolutely expired token must be rejected (401/302/403), got: " + status2,
                   status2 == 401 || status2 == 302 || status2 == 403);

        Log.info(thisClass, method, "re-authenticating after expiry");
        HttpURLConnection conn3 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        int status3 = conn3.getResponseCode();
        Log.info(thisClass, method, "re-authentication response: HTTP " + status3);
        assertEquals("Re-authentication must succeed", 200, status3);

        String newCookie = extractCookie(conn3);
        Log.info(thisClass, method, "new LTPA cookie after re-authentication: " + maskCookie(newCookie));
        assertNotNull("Must receive a new cookie after re-authentication", newCookie);
        assertFalse("New cookie must differ from expired one", cookie.equals(newCookie));
        Log.info(thisClass, method, "PASSED: expired token correctly rejected with HTTP " + status2 + ", reauthentication succeeded with new cookie");
        conn3.disconnect();
    }

    @Test
    public void testTokenRefreshDisabledWhenOnlyRefreshThresholdConfigured() throws Exception {
        setConfig("serverTokenRefreshOnly.xml");
        String url = getServletUrl();
        String method = "testTokenRefreshDisabledWhenOnlyRefreshThresholdConfigured";
        Log.info(thisClass, method, "Config: expiration=2m, refreshThreshold=1m, no inactivityTimeout");

        HttpURLConnection conn1 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        int status1 = conn1.getResponseCode();
        Log.info(thisClass, method, "authentication response: HTTP " + status1);
        assertEquals("Initial authentication must succeed", 200, status1);
        String cookie = extractCookie(conn1);
        assertNotNull("Must receive cookie", cookie);
        Log.info(thisClass, method, "initial LTPA cookie: " + maskCookie(cookie));
        conn1.disconnect();

        Log.info(thisClass, method, "waiting " + (REFRESH_WAIT_MS/1000) + "s past refreshThreshold");
        Thread.sleep(REFRESH_WAIT_MS);

        Log.info(thisClass, method, "sending SSO request; token must still be valid (no inactivity death) and produce no clone");
        HttpURLConnection conn2 = makeRequestWithCookie(url, cookie);
        int status2 = conn2.getResponseCode();
        Log.info(thisClass, method, "SSO response: HTTP " + status2);
        assertEquals("SSO must succeed, token only expires at absolute expiration without inactivityTimeout", 200, status2);
        assertTokenNotRefreshed("No clone expected — refreshThreshold has no effect without inactivityTimeout", conn2);
        
        Log.info(thisClass, method, "waiting another " + (REFRESH_WAIT_MS/1000) + "s");
        Thread.sleep(REFRESH_WAIT_MS);
        Log.info(thisClass, method, "sending SSO request; token must be expired");
        HttpURLConnection conn3 = makeRequestWithCookie(url, cookie);
        int status3 = conn3.getResponseCode();
        Log.info(thisClass, method, "SSO response: HHTP " + status3);
        assertTrue("SSO must fail, token should be expired: got HTTP " + status3,
                   status3 == 401 || status3 == 302 || status3 == 403);

        Log.info(thisClass, method, "PASSED: no new cookie after refresh threshold and cookie expires at expiration time");
        conn2.disconnect();
    }

    @Test
    public void testTokenRefreshDisabledWhenOnlyInactivityTimeoutConfigured() throws Exception {
        setConfig("serverTokenInactivityOnly.xml");
        String url = getServletUrl();
        String method = "testTokenRefreshDisabledWhenOnlyInactivityTimeoutConfigured";
        Log.info(thisClass, method, "Config: expiration=2m, inactivityTimeout=1m, no refreshThreshold");

        HttpURLConnection conn1 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        int status1 = conn1.getResponseCode();
        Log.info(thisClass, method, "authentication response: HTTP " + status1);
        assertEquals("Initial authentication must succeed", 200, status1);
        String cookie = extractCookie(conn1);
        assertNotNull("Must receive cookie", cookie);
        Log.info(thisClass, method, "initial LTPA cookie: " + maskCookie(cookie));
        conn1.disconnect();

        Log.info(thisClass, method, "waiting 61s (past inactivity timeout)");
        Thread.sleep(61_000);

        Log.info(thisClass, method, "sending SSO request; token must be valid");
        HttpURLConnection conn2 = makeRequestWithCookie(url, cookie);
        int status2 = conn2.getResponseCode();
        Log.info(thisClass, method, "SSO response: HTTP " + status2);
        assertEquals("Inactivity timeout should be disabled, SSO must succeed.", 200, status2);
        assertTokenNotRefreshed("Cookie should not be refreshed", conn2);

        Log.info(thisClass, method, "waiting another 61s to expire cookie");
        Thread.sleep(61_000);
        Log.info(thisClass, method, "sending SSO request; token must be expired");
        HttpURLConnection conn3 = makeRequestWithCookie(url, cookie);
        int status3 = conn3.getResponseCode();
        Log.info(thisClass, method, "SSO response: HTTP " + status3);
        assertTrue("SSO must fail, token should be expired: got HTTP " + status3,
                   status3 == 401 || status3 == 302 || status3 == 403);

        Log.info(thisClass, method, "PASSED: SSO succeeded past inactivity timeout and cookie expired after expiration");
        conn2.disconnect();
    }

    @Test
    public void testTokenExpiresAtExpirationWhenTimeoutExceedsExpiration() throws Exception {
        setConfig("serverTokenTimeoutExceedsExpiration.xml");
        String url = getServletUrl();
        String method = "testTokenExpiresAtExpirationWhenTimeoutExceedsExpiration";
        Log.info(thisClass, method, "Config: expiration=2m, inactivityTimeout=3m, refreshThreshold=1m");

        HttpURLConnection conn1 = makeAuthenticatedRequest(url, null, "user1", "user1pwd");
        int status1 = conn1.getResponseCode();
        Log.info(thisClass, method, "authentication response: HTTP " + status1);
        assertEquals("Initial authentication must succeed", 200, status1);
        String cookie = extractCookie(conn1);
        assertNotNull("Must receive cookie", cookie);
        Log.info(thisClass, method, "initial LTPA cookie: " + maskCookie(cookie));
        conn1.disconnect();

        Log.info(thisClass, method, "waiting 121s to pass the 2-minute expiration");
        Thread.sleep(121_000);

        Log.info(thisClass, method, "sending SSO request after expiration; token must be REJECTED");
        HttpURLConnection conn2 = makeRequestWithCookie(url, cookie);
        int status2 = conn2.getResponseCode();
        Log.info(thisClass, method, "SSO response: HTTP " + status2);
        conn2.disconnect();
        assertTrue("Token must be rejected at expiration, got HTTP " + status2,
                status2 == 401 || status2 == 302 || status2 == 403);
        Log.info(thisClass, method, "PASSED: token correctly rejected at expiration boundary");
    }


    
    // Make an authenticated HTTP GET using Basic credentials.
    private HttpURLConnection makeAuthenticatedRequest(String urlString, String cookie,
                                                       String username, String password) throws IOException {
        HttpURLConnection conn = openConnection(urlString);
        if (cookie != null) {
            conn.setRequestProperty("Cookie", LTPA_COOKIE + "=" + cookie);
        }
        conn.setRequestProperty("Authorization",
            "Basic " + java.util.Base64.getEncoder()
                           .encodeToString((username + ":" + password).getBytes()));
        consumeResponse(conn);
        return conn;
    }

    // Make an HTTP GET carrying an LTPA cookie (SSO — no credentials).
    private HttpURLConnection makeRequestWithCookie(String urlString, String cookie) throws IOException {
        HttpURLConnection conn = openConnection(urlString);
        conn.setRequestProperty("Cookie", LTPA_COOKIE + "=" + cookie);
        consumeResponse(conn);
        return conn;
    }

    private HttpURLConnection openConnection(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.setDoOutput(false);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(false);
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
            Log.info(thisClass, "consumeResponse", "IOException consuming response: " + e.getMessage());
        }
    }

    // Extract the LtpaToken2 cookie value from a response's Set-Cookie headers.
    private String extractCookie(HttpURLConnection conn) {
        String header = getCookieHeader(conn.getHeaderFields());
        if (header == null) return null;
        int start = LTPA_COOKIE.length() + 1;
        int end   = header.indexOf(";");
        return header.substring(start, end == -1 ? header.length() : end);
    }

    // Return the full Set-Cookie header string for LtpaToken2
    private String getCookieHeader(Map<String, List<String>> headers) {
        List<String> setCookies = headers.get("Set-Cookie");
        if (setCookies != null) {
            for (String header : setCookies) {
                if (header.startsWith(LTPA_COOKIE + "=")) {
                    return header;
                }
            }
        }
        return null;
    }

    // Assert that a token refresh occurred — fails if no Set-Cookie was issued.    
    private String assertTokenRefreshed(HttpURLConnection conn) {
        String newCookie = extractCookie(conn);
        assertNotNull("Expected a token refresh (new Set-Cookie: LtpaToken2) but none was issued", newCookie);
        return newCookie;
    }

    // Assert that NO token refresh occurred — fails if an unexpected Set-Cookie was issued.
    private void assertTokenNotRefreshed(String message, HttpURLConnection conn) {
        String newCookie = extractCookie(conn);
        assertNull(message + " — but got new cookie: " + maskCookie(newCookie), newCookie);
    }

    // Mask a cookie value for safe logging (show only first and last 10 characters).
    private String maskCookie(String cookie) {
        if (cookie == null)          return "null";
        if (cookie.length() < 20)    return "***";
        return cookie.substring(0, 10) + "..." + cookie.substring(cookie.length() - 10);
    }

    private String getServletUrl() {
        return "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() +
               "/" + APP_NAME + "/" + SERVLET_NAME;
    }

    private void setConfig(String config) throws Exception {
        server.setMarkToEndOfLog();
        server.setServerConfigurationFile(config);
        server.waitForConfigUpdateInLogUsingMark(null);
    }
}