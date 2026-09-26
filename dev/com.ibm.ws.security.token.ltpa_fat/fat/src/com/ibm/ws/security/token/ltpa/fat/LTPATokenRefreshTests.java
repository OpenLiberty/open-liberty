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

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.impl.LibertyServerFactory;

@RunWith(FATRunner.class)
@Mode(TestMode.FULL)
@SkipForRepeat(SkipForRepeat.EE9_OR_LATER_FEATURES)
public class LTPATokenRefreshTests {

    private static final String APP_NAME     = "ltpaTest";
    private static final String SERVLET_NAME = "LTPATestServlet";
    private static final String LTPA_COOKIE  = "LtpaToken2";
    private static final Class<?> thisClass  = LTPATokenRefreshTests.class;

    // refresh server: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m
    private static LibertyServer refreshServer;

    // non-refresh server: expiration=3m, no inactivityTimeout, no refreshThreshold
    private static LibertyServer nonRefreshServer;

    // Relative path (under publish/files/) of the pre-provisioned key set used
    // by both servers. Using a known key avoids the race between key generation
    // and the first test request.
    private static final String SHARED_KEYS_SRC = "alternate/validation1.keys";
    private static final String KEYS_DEST       = "resources/security/ltpa.keys";

    // Token age offsets in seconds used by authenticateAndBackdateToken().
    private static final int PAST_THRESHOLD_S   = 70;
    private static final int BEFORE_THRESHOLD_S = 20;
    private static final int PAST_INACTIVITY_S  = 130;
    private static final int PAST_EXPIRY_S      = 190;

    private static final String CFG_TOKEN_REFRESH                      = "serverTokenRefresh.xml";
    private static final String CFG_TOKEN_NON_REFRESH                  = "serverTokenNonRefresh.xml";
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
        refreshServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.token.ltpa.fat.refresh");
        refreshServer.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/ltpafattestlibertyinternals-1.0.mf");
        refreshServer.addInstalledAppForValidation(APP_NAME);

        nonRefreshServer = LibertyServerFactory.getLibertyServer("com.ibm.ws.security.token.ltpa.fat.refreshDisabled");
        nonRefreshServer.copyFileToLibertyInstallRoot("lib/features", "internalFeatureForFat/ltpafattestlibertyinternals-1.0.mf");
        nonRefreshServer.addInstalledAppForValidation(APP_NAME);
        nonRefreshServer.useSecondaryHTTPPort();

        // Pre-provision both servers with identical LTPA keys so tokens minted by the
        // non-refresh server are decryptable by the refresh server and vice versa.
        copySharedKeysToServer(refreshServer);
        copySharedKeysToServer(nonRefreshServer);

        // Start refreshServer once for the entire class with the baseline configuration.
        refreshServer.setJvmOptions(Arrays.asList("-Dcom.ibm.ws.beta.edition=true"));
        refreshServer.setServerConfigurationFile(CFG_TOKEN_REFRESH);
        refreshServer.startServer(true);
        refreshServer.waitForStringInLog("CWWKZ0001I.*" + APP_NAME);

        // Start nonRefreshServer once for the entire class.
        nonRefreshServer.setJvmOptions(Arrays.asList("-Dcom.ibm.ws.beta.edition=true"));
        nonRefreshServer.startServer(true);
        nonRefreshServer.waitForStringInLog("CWWKZ0001I.*" + APP_NAME);
    }

    @Before
    public void setUp() throws Exception {
        // Restore refreshServer to the baseline configuration before each test
        setConfig(CFG_TOKEN_REFRESH);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (nonRefreshServer != null && nonRefreshServer.isStarted()) {
            nonRefreshServer.stopServer();
        }
        if (refreshServer != null && refreshServer.isStarted()) {
            // These warnings are intentionally produced by specific test configs and must not
            // cause teardown to fail: CWWKS4125W (inactivityTimeout >= expiration),
            // CWWKS4124W (refreshThreshold >= inactivityTimeout, relative-to-expiration path),
            // CWWKS4123W (refreshThreshold >= inactivityTimeout, clearly-wrong path),
            // CWWKS4126W (inactivityTimeout set without refreshThreshold),
            // CWWKS4127W (refreshThreshold set without inactivityTimeout).
            refreshServer.stopServer("CWWKS4125W", "CWWKS4124W", "CWWKS4123W", "CWWKS4126W", "CWWKS4127W");
        }
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m.
     * <LI>Authenticate as user1 and backdate the token past the refresh threshold (70s).
     * <LI>Send an SSO request with the backdated cookie.
     * <LI>Send a second SSO request using the newly refreshed cookie.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>The server issues a new LtpaToken2 cookie on the first SSO request (token refresh triggered).
     * <LI>The refreshed cookie is accepted and returns HTTP 200 on the second SSO request.
     * </OL>
     */
    @Test
    public void testTokenRefreshedAndUsableWhenThresholdCrossed() throws Exception {
        String url = getRefreshServletUrl();
        String method = "testTokenRefreshedAndUsableWhenThresholdCrossed";
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m");

        String agedCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_THRESHOLD_S, method);

        String refreshedCookie = ssoRequestExpectingRefresh(url, agedCookie, "SSO after threshold", method);

        HttpURLConnection conn = ssoRequest(url, refreshedCookie, "SSO with refreshed cookie", method);
        assertEquals("Refreshed cookie must be accepted", 200, conn.getResponseCode());
        conn.disconnect();
        Log.info(thisClass, method, "PASSED: new cookie issued and successfully used for SSO");
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m.
     * <LI>Authenticate as user1 and backdate the token to 20s — below the refresh threshold.
     * <LI>Send an SSO request with the backdated cookie.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>The SSO request succeeds with HTTP 200.
     * <LI>No new LtpaToken2 cookie is issued — refresh is not triggered before the threshold is crossed.
     * </OL>
     */
    @Test
    public void testTokenNotRefreshedBeforeThreshold() throws Exception {
        String url = getRefreshServletUrl();
        String method = "testTokenNotRefreshedBeforeThreshold";
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m");

        String agedCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", BEFORE_THRESHOLD_S, method);

        HttpURLConnection conn = ssoRequest(url, agedCookie, "SSO before threshold", method);
        assertEquals("SSO request must succeed", 200, conn.getResponseCode());
        assertTokenNotRefreshed("Token should not be refreshed when inactivity remaining > refreshThreshold", conn);
        conn.disconnect();
        Log.info(thisClass, method, "PASSED: no new cookie received");
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m.
     * <LI>Authenticate as user1 and backdate the token past the refresh threshold (110s).
     * <LI>Send an SSO request with the backdated cookie to trigger a refresh.
     * <LI>Wait 10 seconds to age the token past original inactivityTimeout of 120s.
     * <LI>Send a second SSO request using the newly refreshed cookie.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>A new LtpaToken2 cookie is issued on the first SSO request.
     * <LI>The refreshed cookie is accepted with HTTP 200, confirming the inactivity clock was
     *     reset to the point of refresh rather than the original token creation time.
     * </OL>
     */
    @Test
    public void testInactivityWindowResetsAfterTokenRefresh() throws Exception {
        String url = getRefreshServletUrl();
        String method = "testInactivityWindowResetsAfterTokenRefresh";
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m");

        // Backdate token within refresh window and 10 seconds before inactivity timeout (age = 110s)
        String agedCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", 110, method);
        String refreshedCookie = ssoRequestExpectingRefresh(url, agedCookie, "SSO after first age (refresh expected)", method);

        // Sleep for 11s to age past original inactivityTimeout of 120s
        Log.info(thisClass, method, "waiting 11s to pass the original inactivityTimeout of 120s");
        Thread.sleep(11_000);

        HttpURLConnection conn = ssoRequest(url, refreshedCookie, "SSO with refreshed cookie (inactivity timeout reset)", method);
        assertEquals("Refreshed cookie must be valid (inactivity clock was reset)", 200, conn.getResponseCode());
        conn.disconnect();

        Log.info(thisClass, method, "PASSED: refreshed cookie accepted; inactivity clock was correctly reset");
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m.
     * <LI>Authenticate as user1 and backdate the token past the refresh threshold (70s).
     * <LI>Authenticate as user2 and backdate the token past the refresh threshold (70s).
     * <LI>Send an SSO request for each user.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Both users receive a new LtpaToken2 cookie (token refresh triggered for each).
     * <LI>The two refreshed cookies are distinct from each other.
     * </OL>
     */
    @Test
    public void testTokenRefreshWithMultipleUsers() throws Exception {
        String url = getRefreshServletUrl();
        String method = "testTokenRefreshWithMultipleUsers";
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m");

        String user1AgedCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_THRESHOLD_S, method);
        String user2AgedCookie = authenticateAndBackdateToken(url, "user2", "user2pwd", PAST_THRESHOLD_S, method);

        assertFalse("Different users must have different backdated cookies", user1AgedCookie.equals(user2AgedCookie));
        Log.info(thisClass, method, "users have unique backdated cookies");

        String user1RefreshedCookie = ssoRequestExpectingRefresh(url, user1AgedCookie, "user1 SSO (refresh expected)", method);
        String user2RefreshedCookie = ssoRequestExpectingRefresh(url, user2AgedCookie, "user2 SSO (refresh expected)", method);

        assertFalse("Refreshed cookies must differ between users", user1RefreshedCookie.equals(user2RefreshedCookie));
        Log.info(thisClass, method, "PASSED: both users received unique refreshed cookies");
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m.
     * <LI>Authenticate as user1 and backdate the token past the refresh threshold (70s).
     * <LI>Send an SSO request with the backdated cookie.
     * <LI>Inspect the Set-Cookie header on the response.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>A new LtpaToken2 cookie is issued (token refresh triggered).
     * <LI>The Set-Cookie header on the refreshed cookie contains the HttpOnly attribute.
     * <LI>The Set-Cookie header on the refreshed cookie contains the Path attribute.
     * </OL>
     */
    @Test
    public void testCookieAttributesPreservedAfterRefresh() throws Exception {
        String url = getRefreshServletUrl();
        String method = "testCookieAttributesPreservedAfterRefresh";
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m");

        String agedCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_THRESHOLD_S, method);

        HttpURLConnection conn = ssoRequest(url, agedCookie, "SSO after threshold (refresh expected)", method);
        assertEquals("SSO authentication must succeed", 200, conn.getResponseCode());
        String header = getCookieHeader(conn.getHeaderFields());
        assertNotNull("Refreshed Set-Cookie header must be present", header);
        assertTokenRefreshed(conn, agedCookie);
        conn.disconnect();

        Log.info(thisClass, method, "refreshed Set-Cookie header: " + header);
        assertTrue("Refreshed cookie must have HttpOnly attribute", header.toLowerCase().contains("httponly"));
        assertTrue("Refreshed cookie must have Path attribute",     header.toLowerCase().contains("path="));

        Log.info(thisClass, method, "PASSED: cookie attributes present on refreshed token");
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Start server with baseline config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m.
     * <LI>Switch server to non-refresh config: expiration=2m, no inactivityTimeout, no refreshThreshold.
     * <LI>Authenticate as user1 and backdate the token past the former refresh threshold (70s).
     * <LI>Send an SSO request with the backdated cookie.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>After the config switch the SSO request succeeds with HTTP 200 — with no inactivityTimeout
     *     configured, the token is only bounded by absolute expiration and no refresh is triggered.
     * </OL>
     */
    @Test
    public void testTokenBehavesCorrectlyAfterConfigUpdate() throws Exception {
        String url = getRefreshServletUrl();
        String method = "testTokenBehavesCorrectlyAfterConfigUpdate";

        Log.info(thisClass, method, "switching from " + CFG_TOKEN_REFRESH + " to " + CFG_TOKEN_NON_REFRESH +
                 " (expiration=2m, no inactivityTimeout, no refreshThreshold)");
        setConfig(CFG_TOKEN_NON_REFRESH);
        Log.info(thisClass, method, "config update complete; refresh and inactivity are now disabled");

        // Feature is OFF: a past-threshold token must not be refreshed.
        String agedCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_THRESHOLD_S, method);

        HttpURLConnection conn = ssoRequest(url, agedCookie, "SSO past refreshThreshold (no refresh expected)", method);
        assertEquals("SSO must succeed, token only expires at absolute expiration without inactivityTimeout", 200, conn.getResponseCode());
        assertTokenNotRefreshed("No refresh expected — refreshThreshold and inactivityTimeout not configured", conn);

        Log.info(thisClass, method, "PASSED: refresh not enforced after switch");
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m.
     * <LI>Authenticate as user1 and backdate the token past the inactivity timeout (130s).
     * <LI>Send an SSO request with the backdated cookie.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>The SSO request is rejected with HTTP 401.
     * <LI>The inactivity timeout is enforced before absolute expiration is reached.
     * </OL>
     */
    @Test
    public void testTokenRejectedAfterInactivityTimeout() throws Exception {
        String url = getRefreshServletUrl();
        String method = "testTokenRejectedAfterInactivityTimeout";
        Log.info(thisClass, method, "expiration=3m, inactivityTimeout=2m, refreshThreshold=1m");

        String expiredCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_INACTIVITY_S, method);

        HttpURLConnection conn = ssoRequest(url, expiredCookie, "SSO after inactivity timeout (rejection expected)", method);
        int status = conn.getResponseCode();
        assertEquals("Idle token must be rejected with 401", 401, status);
        conn.disconnect();
        Log.info(thisClass, method, "PASSED: idle token correctly rejected with HTTP " + status);
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m.
     * <LI>Authenticate as user1 and backdate the token past the absolute expiration (190s).
     * <LI>Send an SSO request with the backdated cookie.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>The SSO request is rejected with HTTP 401.
     * <LI>The absolute expiration boundary is enforced regardless of inactivity timeout or refresh threshold.
     * </OL>
     */
    @Test
    public void testTokenExpiresAfterExpirationTime() throws Exception {
        String url = getRefreshServletUrl();
        String method = "testTokenExpiresAfterExpirationTime";
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m");

        String expiredCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_EXPIRY_S, method);

        HttpURLConnection conn = ssoRequest(url, expiredCookie, "SSO after absolute expiry (rejection expected)", method);
        int status = conn.getResponseCode();
        assertEquals("Absolutely expired token must be rejected with 401", 401, status);
        conn.disconnect();
        Log.info(thisClass, method, "PASSED: expired token correctly rejected with HTTP " + status);
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=2m, refreshThreshold=1m, no inactivityTimeout.
     * <LI>Assert that warning CWWKS4127W is logged (refreshThreshold set without inactivityTimeout).
     * <LI>Authenticate as user1 and backdate the token past the refresh threshold (70s).
     * <LI>Send an SSO request with the backdated cookie.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Warning CWWKS4127W is emitted at startup.
     * <LI>The SSO request succeeds with HTTP 200 — refresh is not triggered because
     *     refreshThreshold has no effect without inactivityTimeout.
     * </OL>
     */
    @Test
    public void testTokenRefreshDisabledWhenOnlyRefreshThresholdConfigured() throws Exception {
        setConfig(CFG_TOKEN_REFRESH_ONLY);
        String url = getRefreshServletUrl();
        String method = "testTokenRefreshDisabledWhenOnlyRefreshThresholdConfigured";
        Log.info(thisClass, method, "Config: expiration=2m, refreshThreshold=1m, no inactivityTimeout");

        assertWarningLogged("CWWKS4127W", "refreshThreshold is set without inactivityTimeout", method);

        String agedCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_THRESHOLD_S, method);

        HttpURLConnection conn = ssoRequest(url, agedCookie, "SSO past refreshThreshold (no refresh expected)", method);
        assertEquals("SSO must succeed, token only expires at absolute expiration without inactivityTimeout", 200, conn.getResponseCode());
        assertTokenNotRefreshed("No refresh expected — refreshThreshold has no effect without inactivityTimeout", conn);
        conn.disconnect();

        Log.info(thisClass, method, "PASSED: no refresh performed");
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=2m, inactivityTimeout=1m, no refreshThreshold.
     * <LI>Assert that warning CWWKS4126W is logged (inactivityTimeout set without refreshThreshold).
     * <LI>Authenticate as user1 and backdate the token past the inactivity timeout (70s).
     * <LI>Send an SSO request with the backdated cookie.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Warning CWWKS4126W is emitted at startup.
     * <LI>The SSO request succeeds with HTTP 200 — inactivity is not enforced because
     *     inactivityTimeout has no effect without refreshThreshold.
     * </OL>
     */
    @Test
    public void testTokenRefreshDisabledWhenOnlyInactivityTimeoutConfigured() throws Exception {
        setConfig(CFG_TOKEN_INACTIVITY_ONLY);
        String url = getRefreshServletUrl();
        String method = "testTokenRefreshDisabledWhenOnlyInactivityTimeoutConfigured";
        Log.info(thisClass, method, "Config: expiration=2m, inactivityTimeout=1m, no refreshThreshold");

        assertWarningLogged("CWWKS4126W", "inactivityTimeout is set without refreshThreshold", method);

        String agedCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_THRESHOLD_S, method);

        HttpURLConnection conn = ssoRequest(url, agedCookie, "SSO past inactivity timeout (no refresh expected)", method);
        assertEquals("Inactivity timeout should be disabled, SSO must succeed.", 200, conn.getResponseCode());
        assertTokenNotRefreshed("Cookie should not be refreshed", conn);
        conn.disconnect();

        Log.info(thisClass, method, "PASSED: SSO succeeded past inactivity timeout");
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=3m, inactivityTimeout=4m, refreshThreshold=2m (inactivityTimeout exceeds expiration).
     * <LI>Assert that warning CWWKS4125W is logged (inactivityTimeout &gt;= expiration).
     * <LI>Authenticate as user1 and backdate the token past the absolute expiration (190s).
     * <LI>Send an SSO request with the backdated cookie.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Warning CWWKS4125W is emitted at startup.
     * <LI>The SSO request is rejected with HTTP 401 — the token has reached absolute expiration.
     * </OL>
     */
    @Test
    public void testTokenInactivityTimeoutExceedsExpiration() throws Exception {
        setConfig(CFG_TOKEN_EXCEEDS_EXPIRY);
        String url = getRefreshServletUrl();
        String method = "testTokenInactivityTimeoutExceedsExpiration";
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=4m, refreshThreshold=2m");

        assertWarningLogged("CWWKS4125W", "inactivityTimeout >= expiration", method);

        String expiredCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_EXPIRY_S, method);

        HttpURLConnection conn = ssoRequest(url, expiredCookie, "SSO after expiration (rejection expected)", method);
        int status = conn.getResponseCode();
        assertEquals("Token must be rejected at expiration, got HTTP " + status, 401, status);
        conn.disconnect();
        Log.info(thisClass, method, "PASSED: token correctly rejected at expiration boundary");
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=6m, inactivityTimeout=3m, refreshThreshold=4m (refreshThreshold exceeds inactivityTimeout).
     * <LI>Assert that warning CWWKS4124W is logged — refreshThreshold is auto-adjusted to inactivityTimeout/3 (1m).
     * <LI>Authenticate as user1 and backdate the token past the adjusted threshold (130s).
     * <LI>Send an SSO request with the backdated cookie.
     * <LI>Send a second SSO request using the refreshed cookie.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Warning CWWKS4124W is emitted at startup.
     * <LI>The first SSO request triggers a token refresh — the adjusted threshold (1m) was crossed.
     * <LI>The refreshed cookie is accepted with HTTP 200 on the second SSO request.
     * </OL>
     */
    @Test
    public void testRefreshThresholdExceedsInactivityTimeout() throws Exception {
        setConfig(CFG_TOKEN_THRESHOLD_EXCEEDS_INACTIVITY);
        String url = getRefreshServletUrl();
        String method = "testRefreshThresholdExceedsInactivityTimeout";
        Log.info(thisClass, method,
                 "Config: expiration=6m, inactivityTimeout=3m, refreshThreshold=4m; " +
                 "expect refreshthreshold auto-adjust to 1m (inactivityTimeout/3)");

        assertWarningLogged("CWWKS4124W", "refreshThreshold(4m) >= inactivityTimeout(3m) and refreshThreshold < expiration", method);

        String agedCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_INACTIVITY_S, method);

        String refreshedCookie = ssoRequestExpectingRefresh(url, agedCookie, "SSO after adjusted threshold (refresh expected)", method);

        HttpURLConnection conn = ssoRequest(url, refreshedCookie, "SSO with refreshed cookie (must succeed)", method);
        assertEquals("Refreshed token must be accepted for SSO", 200, conn.getResponseCode());
        conn.disconnect();

        Log.info(thisClass, method, "PASSED: CWWKS4124W emitted, threshold auto-adjusted to 1m, " + "token refreshed when adjusted threshold was crossed");
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Refresh server config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m.
     * <LI>Non-refresh server config: expiration=3m, no inactivityTimeout, no refreshThreshold.
     * <LI>Both servers share the same LTPA key set.
     * <LI>Authenticate as user1 on the refresh server with a fresh token (no backdating).
     * <LI>Present the refresh server token to the non-refresh server.
     * <LI>Authenticate as user1 on the non-refresh server with a fresh token (no backdating).
     * <LI>Present the non-refresh server token to the refresh server.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>The refresh server token is accepted by the non-refresh server with HTTP 200.
     * <LI>The non-refresh server token is accepted by the refresh server with HTTP 200.
     * </OL>
     */
    @Test
    public void testFreshTokensAcceptedAcrossMixedEnvironments() throws Exception {
        String nonRefreshUrl = getNonRefreshServletUrl();
        String refreshUrl = getRefreshServletUrl();
        String method = "testFreshTokensAcceptedAcrossMixedEnvironments";

        // Mint a fresh token on the refresh server and present it to the non-refresh server.
        String refreshCookie = authenticateAndBackdateToken(refreshUrl, "user1", "user1pwd", 0, method);
        HttpURLConnection conn = ssoRequest(nonRefreshUrl, refreshCookie, "refresh server token on non-refresh server (acceptance expected)", method);
        assertEquals("Token from refresh server must be accepted by non-refresh server", 200, conn.getResponseCode());
        conn.disconnect();

        // Mint a fresh token on the non-refresh server and present it to the refresh server.
        String nonRefreshCookie = authenticateAndBackdateToken(nonRefreshUrl, "user1", "user1pwd", 0, method);
        conn = ssoRequest(refreshUrl, nonRefreshCookie, "non-refresh server token on refresh server (acceptance expected)", method);
        assertEquals("Token from non-refresh server must be accepted by refresh server", 200, conn.getResponseCode());
        conn.disconnect();

        Log.info(thisClass, method, "PASSED: fresh tokens accepted in both directions between refresh server and non-refresh server");
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Refresh server config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m.
     * <LI>Non-refresh server config: expiration=3m, no inactivityTimeout, no refreshThreshold.
     * <LI>Both servers share the same LTPA key set.
     * <LI>Authenticate as user1 on the refresh server and backdate the token past the inactivity timeout (130s).
     * <LI>Present the timed-out token to the non-refresh server.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>The non-refresh server accepts the token with HTTP 200 — it does not enforce
     *     the inactivity timeout from refresh server's configuration.
     * </OL>
     */
    @Test
    public void testRefreshTokenRejectedByNonRefreshServerAfterInactivityTimeout() throws Exception {
        String nonRefreshUrl = getNonRefreshServletUrl();
        String refreshUrl = getRefreshServletUrl();
        String method = "testRefreshTokenRejectedByNonRefreshServerAfterInactivityTimeout";

        // Authenticate on the refresh server and backdate the token past inactivity (2m).
        String agedCookie = authenticateAndBackdateToken(refreshUrl, "user1", "user1pwd", PAST_INACTIVITY_S, method);

        // Present the timed-out token to the non-refresh server.
        HttpURLConnection conn = ssoRequest(nonRefreshUrl, agedCookie, "refresh server token on non-refresh server (past inactivity, rejection expected)", method);
        int status = conn.getResponseCode();
        assertEquals("Token from refresh server must be rejected by non-refresh server after inactivityTimeout elapses", 401, status);
        conn.disconnect();

        Log.info(thisClass, method, "PASSED: refresh server token past inactivity timeout rejected by non-refresh server");
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Refresh server config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m.
     * <LI>Non-refresh server config: expiration=3m, no inactivityTimeout, no refreshThreshold.
     * <LI>Both servers share the same LTPA key set.
     * <LI>Authenticate as user1 on the non-refresh server and backdate the token past the refresh threshold (70s).
     * <LI>Present the backdated token to the refresh server.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>The refresh server accepts the token and issues a new LtpaToken2 cookie (token refresh triggered).
     * </OL>
     */
    @Test
    public void testNonRefreshTokenRefreshedByRefreshServer() throws Exception {
        String nonRefreshUrl = getNonRefreshServletUrl();
        String refreshUrl = getRefreshServletUrl();
        String method = "testNonRefreshTokenRefreshedByRefreshServer";

        // Authenticate on the non-refresh server and backdate the token past the refresh server's threshold (1m).
        String agedCookie = authenticateAndBackdateToken(nonRefreshUrl, "user1", "user1pwd", PAST_THRESHOLD_S, method);

        // Present the backdated token to the refresh server — must be accepted and a refresh must be issued.
        ssoRequestExpectingRefresh(refreshUrl, agedCookie, "non-refresh server token on refresh server (refresh expected)", method);
        Log.info(thisClass, method, "PASSED: token from non-refresh server accepted by refresh server and refreshed");
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Refresh server config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m.
     * <LI>Non-refresh server config: expiration=3m, no inactivityTimeout, no refreshThreshold.
     * <LI>Both servers share the same LTPA key set.
     * <LI>Authenticate as user1 on the non-refresh server and backdate the token past absolute expiration (190s).
     * <LI>Present the expired token to the refresh server.
     * <LI>Authenticate as user1 on the refresh server and backdate the token past absolute expiration (190s).
     * <LI>Present the expired token to the non-refresh server.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>The refresh server rejects the non-refresh server token with HTTP 401.
     * <LI>The non-refresh server rejects the refresh server token with HTTP 401.
     * </OL>
     */
    @Test
    public void testExpiredTokensRejectedAcrossMixedEnvironments() throws Exception {
        String nonRefreshUrl = getNonRefreshServletUrl();
        String refreshUrl = getRefreshServletUrl();
        String method = "testExpiredTokensRejectedAcrossMixedEnvironments";

        // token minted on non-refresh server, presented to refresh server.
        String expiredFromNonRefresh = authenticateAndBackdateToken(nonRefreshUrl, "user1", "user1pwd", PAST_EXPIRY_S, method);
        HttpURLConnection conn1 = ssoRequest(refreshUrl, expiredFromNonRefresh, "expired non-refresh server token on refresh server (rejection expected)", method);
        int status1 = conn1.getResponseCode();
        assertEquals("Absolutely expired token from non-refresh server must be rejected by refresh server — got HTTP " + status1, 401, status1);
        conn1.disconnect();

        // token minted on refresh server, presented to non-refresh server.
        String expiredFromRefresh = authenticateAndBackdateToken(refreshUrl, "user1", "user1pwd", PAST_EXPIRY_S, method);
        HttpURLConnection conn2 = ssoRequest(nonRefreshUrl, expiredFromRefresh, "expired refresh server token on non-refresh server (rejection expected)", method);
        int status2 = conn2.getResponseCode();
        assertEquals("Absolutely expired token from refresh server must be rejected by non-refresh server — got HTTP " + status2, 401, status2);
        conn2.disconnect();

        Log.info(thisClass, method, "PASSED: expired tokens correctly rejected in both directions across mixed environments");
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Refresh server config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m.
     * <LI>Non-refresh server config: expiration=3m, no inactivityTimeout, no refreshThreshold.
     * <LI>Both servers share the same LTPA key set.
     * <LI>Authenticate as user1 on the refresh server and backdate the token past the refresh threshold (70s).
     * <LI>Present the backdated token to the refresh server to trigger a refresh.
     * <LI>Present the newly refreshed token to the non-refresh server.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>The refresh server issues a new LtpaToken2 cookie (token refresh triggered).
     * <LI>The non-refresh server accepts the refreshed token with HTTP 200.
     * </OL>
     */
    @Test
    public void testRefreshedTokenAcceptedByNonRefreshServer() throws Exception {
        String nonRefreshUrl = getNonRefreshServletUrl();
        String refreshUrl = getRefreshServletUrl();
        String method = "testRefreshedTokenAcceptedByNonRefreshServer";

        // Authenticate on the refresh server and backdate the token past the refresh threshold (1m).
        String backdatedCookie = authenticateAndBackdateToken(refreshUrl, "user1", "user1pwd", PAST_THRESHOLD_S, method);

        // Trigger refresh on the refresh server.
        String refreshedCookie = ssoRequestExpectingRefresh(refreshUrl, backdatedCookie, "trigger refresh on refresh server", method);

        // Present the refreshed token to the non-refresh server.
        HttpURLConnection conn = ssoRequest(nonRefreshUrl, refreshedCookie, "refreshed token on non-refresh server (acceptance expected)", method);
        assertEquals("Refreshed token from refresh server must be accepted by non-refresh server before inactivity elapses", 200, conn.getResponseCode());
        conn.disconnect();

        Log.info(thisClass, method, "PASSED: refreshed token accepted by non-refresh server — inactivity window has not yet elapsed");
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Refresh server config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m.
     * <LI>Non-refresh server config: expiration=3m, no inactivityTimeout, no refreshThreshold.
     * <LI>Both servers share the same LTPA key set.
     * <LI>Authenticate as user1 on the refresh server and backdate the token past the refresh threshold (70s).
     * <LI>Present the backdated token to the non-refresh server.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>The non-refresh server accepts the token with HTTP 200.
     * <LI>No new LtpaToken2 cookie is issued — the non-refresh server has no refreshThreshold
     *     and does not trigger a refresh.
     * </OL>
     */
    @Test
    public void testRefreshTokenPastThresholdNotRefreshedByNonRefreshServer() throws Exception {
        String nonRefreshUrl = getNonRefreshServletUrl();
        String refreshUrl = getRefreshServletUrl();
        String method = "testRefreshTokenPastThresholdNotRefreshedByNonRefreshServer";

        // Authenticate on the refresh server and backdate the token past the refresh threshold (70s).
        String agedCookie = authenticateAndBackdateToken(refreshUrl, "user1", "user1pwd", PAST_THRESHOLD_S, method);

        // Present the backdated token to the non-refresh server — accepted but not refreshed.
        HttpURLConnection conn = ssoRequest(nonRefreshUrl, agedCookie, "refresh server token past threshold on non-refresh server (acceptance, no refresh expected)", method);
        assertEquals("Token from refresh server must be accepted by non-refresh server", 200, conn.getResponseCode());
        assertTokenNotRefreshed("Non-refresh server must not issue a new cookie — it has no refreshThreshold configured", conn);
        conn.disconnect();

        Log.info(thisClass, method, "PASSED: refresh server token past threshold accepted by non-refresh server without triggering a refresh");
    }


    // =========================================================================
    // Helper methods
    // =========================================================================

    /**
     * Authenticates via Basic Auth, requests the servlet to backdate the issued token by
     * {@code ageSeconds}, and returns the resulting {@code LtpaToken2} cookie value.
     */
    private String authenticateAndBackdateToken(String url, String username, String password,
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

    /**
     * Sends a cookie-only SSO request and asserts that the server issues a new distinct
     * {@code LtpaToken2} cookie (HTTP 200). Returns the new cookie value.
     */
    private String ssoRequestExpectingRefresh(String url, String cookie, String label,
                                              String method) throws IOException {
        HttpURLConnection conn = ssoRequest(url, cookie, label, method);
        assertEquals(label + ": SSO must succeed", 200, conn.getResponseCode());
        String newCookie = assertTokenRefreshed(conn, cookie);
        Log.info(thisClass, method, label + ": refreshed cookie: " + maskCookie(newCookie));
        conn.disconnect();
        return newCookie;
    }

    /**
     * Sends a cookie-only SSO GET request, logs the label and HTTP response code,
     * and returns the open connection for further inspection by the caller.
     */
    private HttpURLConnection ssoRequest(String url, String cookie, String label,
                                         String method) throws IOException {
        Log.info(thisClass, method, label + ": sending SSO request");
        HttpURLConnection conn = openConnection(url);
        conn.setRequestProperty("Cookie", LTPA_COOKIE + "=" + cookie);
        Log.info(thisClass, method, label + ": HTTP " + conn.getResponseCode());
        return conn;
    }

    /**
     * Asserts that the response contains a {@code Set-Cookie: LtpaToken2} header whose
     * value differs from {@code previousCookie}, indicating a token refresh occurred.
     * Returns the new cookie value.
     */
    private String assertTokenRefreshed(HttpURLConnection conn, String previousCookie) {
        String newCookie = extractCookie(conn);
        assertNotNull("Expected a token refresh (new Set-Cookie: LtpaToken2) but none was issued", newCookie);
        assertFalse("Refreshed cookie must differ from the previous cookie", previousCookie.equals(newCookie));
        return newCookie;
    }

    /**
     * Asserts that the response does not contain a {@code Set-Cookie: LtpaToken2} header,
     * confirming that no token refresh occurred. Fails with {@code message} if one is found.
     */
    private void assertTokenNotRefreshed(String message, HttpURLConnection conn) {
        String newCookie = extractCookie(conn);
        assertNull(message + " — but got new cookie: " + maskCookie(newCookie), newCookie);
    }

    /**
     * Opens a GET connection to {@code urlString} and sets a Basic Authorization header
     * for the given credentials.
     */
    private HttpURLConnection makeAuthenticatedRequest(String urlString,
                                                       String username, String password) throws IOException {
        HttpURLConnection conn = openConnection(urlString);
        conn.setRequestProperty("Authorization",
            "Basic " + java.util.Base64.getEncoder()
                           .encodeToString((username + ":" + password).getBytes()));
        return conn;
    }

    /**
     * Extracts the raw {@code LtpaToken2} cookie value from the last matching
     * {@code Set-Cookie} response header, or {@code null} if no such header is present.
     */
    private String extractCookie(HttpURLConnection conn) {
        String header = getCookieHeader(conn.getHeaderFields());
        if (header == null) return null;
        int start = LTPA_COOKIE.length() + 1;
        int end   = header.indexOf(";");
        return header.substring(start, end == -1 ? header.length() : end);
    }

    /**
     * Returns the full {@code Set-Cookie} header string for the last {@code LtpaToken2}
     * cookie in the response headers map, or {@code null} if none is present.
     */
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

    /**
     * Returns a masked representation of {@code cookie} for safe log output,
     * showing only the first and last 10 characters separated by {@code ...}.
     */
    private String maskCookie(String cookie) {
        if (cookie == null)          return "null";
        if (cookie.length() < 20)    return "***";
        return cookie.substring(0, 10) + "..." + cookie.substring(cookie.length() - 10);
    }

    /**
     * Opens a non-caching, non-redirecting GET connection to the given URL.
     */
    private HttpURLConnection openConnection(String urlString) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlString).openConnection();
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.setUseCaches(false);
        conn.setInstanceFollowRedirects(false);
        return conn;
    }

    /**
     * Returns the base servlet URL for the refresh server.
     */
    private String getRefreshServletUrl() {
        return "http://" + refreshServer.getHostname() + ":" + refreshServer.getHttpDefaultPort() +
               "/" + APP_NAME + "/" + SERVLET_NAME;
    }

    /**
     * Returns the base servlet URL for the non-refresh server.
     */
    private String getNonRefreshServletUrl() {
        return "http://" + nonRefreshServer.getHostname() + ":" + nonRefreshServer.getHttpDefaultPort() +
               "/" + APP_NAME + "/" + SERVLET_NAME;
    }

    /**
     * Copies the pre-provisioned shared LTPA keys file into the given server's
     * {@code resources/security/} directory and renames it to {@code ltpa.keys},
     * ensuring both servers start with identical key material so each can decrypt
     * tokens issued by the other.
     */
    private static void copySharedKeysToServer(LibertyServer srv) throws Exception {
        // Place validation1.keys directly into resources/security/ (same pattern as LTPAKeyPasswordTests).
        srv.copyFileToLibertyServerRoot("resources/security", SHARED_KEYS_SRC);
        // Rename validation1.keys → ltpa.keys within that directory.
        File placed  = new File(srv.getServerRoot(), "resources/security/validation1.keys");
        File dest    = new File(srv.getServerRoot(), KEYS_DEST);
        if (!placed.renameTo(dest)) {
            throw new Exception("Failed to rename " + placed + " to " + dest);
        }
    }

    /**
     * Swaps the refresh-enabled server's configuration file to {@code config} and waits
     * for Liberty to finish processing the change.
     *
     * <p>Two outcomes are both valid:
     * <ul>
     *   <li>{@code CWWKS4105I} — LTPA reloaded (config meaningfully changed LTPA parameters).
     *   <li>{@code CWWKG0018I} — Liberty detected no functional change (e.g. restoring the
     *       baseline config that the server is already running).
     * </ul>
     * Either message confirms the server has finished processing the file write and is in
     * the expected state. Waiting only for {@code CWWKS4105I} causes a 30-second timeout
     * whenever the config being applied is functionally identical to what is already running.
     */
    private void setConfig(String config) throws Exception {
        refreshServer.setMarkToEndOfLog();
        refreshServer.setServerConfigurationFile(config);
        // Wait for whichever confirmation Liberty emits first.
        String done = refreshServer.waitForStringInLog("CWWKS4105I|CWWKG0018I", 30000);
        if (done == null) {
            throw new Exception("Timeout waiting for LTPA configuration (neither CWWKS4105I nor CWWKG0018I appeared)");
        }
    }

    /**
     * Waits for the given warning message ID to appear in the server log and asserts that
     * it was found. Fails the test with a descriptive message (including {@code context})
     * if the warning does not appear within the default timeout.
     */
    private void assertWarningLogged(String messageId, String context, String method) {
        String warnMsg = refreshServer.waitForStringInLog(messageId);
        assertNotNull("Expected " + messageId + " when " + context + ", but no warning was found in the log", warnMsg);
        Log.info(thisClass, method, "warning logged as expected: " + warnMsg);
    }
}
