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
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
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
import com.ibm.ws.security.token.ltpa.servlet.LTPATestServlet;

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

    // refresh server: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m, dynamicExpirationValidation=true
    private static LibertyServer refreshServer;

    // non-refresh server: expiration=3m, no inactivityTimeout, no refreshThreshold
    private static LibertyServer nonRefreshServer;

    // Relative path (under publish/files/) of the pre-provisioned key set used
    // by both servers. Using a known key avoids the race between key generation
    // and the first test request.
    private static final String SHARED_KEYS_SRC = "alternate/validation1.keys";
    private static final String KEYS_DEST       = "resources/security/ltpa.keys";

    // Token age offsets in seconds used by authenticateAndBackdateToken().
    private static final int PAST_REFRESH_S   = 61;
    private static final int BEFORE_REFRESH_S = 20;
    private static final int PAST_INACTIVITY_S  = 121;
    private static final int PAST_EXPIRY_S      = 181;
    private static final int NEAR_INACTIVITY_S  = 115;

    private static final String CFG_TOKEN_REFRESH                      = "serverTokenRefresh.xml";
    private static final String CFG_TOKEN_NON_REFRESH                  = "serverTokenNonRefresh.xml";
    private static final String CFG_TOKEN_REFRESH_ONLY                 = "serverTokenRefreshOnly.xml";
    private static final String CFG_TOKEN_INACTIVITY_ONLY              = "serverTokenInactivityOnly.xml";
    private static final String CFG_TOKEN_INACTIVITY_EXCEEDS_EXPIRY    = "serverTokenInactivityExceedsExpiration.xml";
    private static final String CFG_TOKEN_INACTIVITY_EQUALS_EXPIRY     = "serverTokenInactivityEqualsExpiration.xml";
    private static final String CFG_TOKEN_REFRESH_EXCEEDS_INACTIVITY   = "serverTokenRefreshExceedsInactivity.xml";
    private static final String CFG_TOKEN_REFRESH_EQUALS_INACTIVITY    = "serverTokenRefreshEqualsInactivity.xml";
    private static final String CFG_TOKEN_REFRESH_DYN_EXP_VAL_FALSE    = "serverTokenRefreshDynExpValFalse.xml";

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
        nonRefreshServer.useSecondaryHTTPPort(); // Avoid port conflict with refreshServer

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
            // cause teardown to fail:
            // CWWKS4125W: inactivityTimeout >= expiration
            // CWWKS4124W: refreshThreshold >= inactivityTimeout AND refreshThreshold < expiration
            // CWWKS4123W: refreshThreshold >= inactivityTimeout AND refreshThreshold >= expiration
            // CWWKS4126W: inactivityTimeout set without refreshThreshold
            // CWWKS4127W: refreshThreshold set without inactivityTimeout
            refreshServer.stopServer("CWWKS4125W", "CWWKS4124W", "CWWKS4123W", "CWWKS4126W", "CWWKS4127W");
        }
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m, dynamicExpirationValidation=true.
     * <LI>Authenticate as user1 and backdate the token by {@code NEAR_INACTIVITY_S} (115s) — inside the
     *     refresh threshold window — then send an SSO request (cycle 1 refresh).
     * <LI>Sleep {@code PAST_REFRESH_S} (61s) to cross the refresh threshold on the refreshed cookie,
     *     then send a second SSO request (cycle 2 refresh).
     * <LI>Sleep 5s and send a final SSO request with the just-refreshed cookie.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Both cycle 1 and cycle 2 SSO requests trigger a refresh and issue a new LtpaToken2 cookie.
     * <LI>The final SSO request is rejected with HTTP 401 — the hard session cap
     *     ({@code sessionStart + expiration}) has been exceeded, ending the session.
     * <LI>This demonstrates that with dynamicExpirationValidation=true the absolute expiration enforced
     *     is based on the configured expiration regardless of the expiration set in the token.
     * </OL>
     */
    @Test
    public void testTokenFullLifeCycleWithDynamicExpirationValidationTrue() throws Exception {
        String url = getRefreshServletUrl();
        String method = "testTokenFullLifeCycleWithDynamicExpirationValidationTrue";
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m, dynamicExpirationValidation=true");

        String cookie = authenticateAndBackdateToken(url, "user1", "user1pwd", NEAR_INACTIVITY_S, method);
        cookie = ssoRequestExpectingRefresh(url, cookie, "cycle 1 (refresh expected, token age ~" + NEAR_INACTIVITY_S + "s)", method);

        Log.info(thisClass, method, "cycle 2: sleeping " + PAST_REFRESH_S + "s to cross refreshThreshold=1m");
        Thread.sleep(PAST_REFRESH_S * 1000L);
        cookie = ssoRequestExpectingRefresh(url, cookie, "cycle 2 (refresh expected, total time elapsed > inactivityTimeout)", method);

        Log.info(thisClass, method, "final check: sleeping 5s");
        Thread.sleep(5_000);
        HttpURLConnection conn = ssoRequest(url, cookie, "final SSO (absolute expiration exceeded, expect 401)", method);
        assertEquals("Session must be rejected once the absolute expiraiton is exceeded", 401, conn.getResponseCode());
        conn.disconnect();
    }

    // same as testTokenFullLifeCycleWithDynamicExpirationValidationTrue but for dynamicExpirationValidation=false
    @Test
    public void testTokenFullLifeCycleWithDynamicExpirationValidationFalse() throws Exception {
        setConfig(CFG_TOKEN_REFRESH_DYN_EXP_VAL_FALSE);
        String url = getRefreshServletUrl();
        String method = "testTokenFullLifeCycleWithDynamicExpirationValidationFalse";
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m, dynamicExpirationValidation=false");

        String cookie = authenticateAndBackdateToken(url, "user1", "user1pwd", NEAR_INACTIVITY_S, method);
        cookie = ssoRequestExpectingRefresh(url, cookie, "cycle 1 (refresh expected, token age ~" + NEAR_INACTIVITY_S + "s)", method);

        Log.info(thisClass, method, "cycle 2: sleeping " + PAST_REFRESH_S + "s to cross refreshThreshold=1m");
        Thread.sleep(PAST_REFRESH_S * 1000L);
        cookie = ssoRequestExpectingRefresh(url, cookie, "cycle 2 (refresh expected, total time elapsed > inactivityTimeout)", method);

        Log.info(thisClass, method, "final check: sleeping 5s then sending SSO, expect 401");
        Thread.sleep(5_000);
        HttpURLConnection conn = ssoRequest(url, cookie, "final SSO (session cap exceeded, expect 401)", method);
        assertEquals("Session must be rejected once the hard session cap is exceeded", 401, conn.getResponseCode());
        conn.disconnect();
    }

    // =========================================================================
    // Refresh Threshold Tests
    // =========================================================================

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m, dynamicExpirationValidation=true.
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
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m, dynamicExpirationValidation=true");

        String agedCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", BEFORE_REFRESH_S, method);

        HttpURLConnection conn = ssoRequest(url, agedCookie, "SSO before threshold", method);
        assertEquals("SSO request must succeed", 200, conn.getResponseCode());
        assertTokenNotRefreshed("Token should not be refreshed when inactivity remaining > refreshThreshold", conn);
        conn.disconnect();
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m, dynamicExpirationValidation=true.
     * <LI>Authenticate as user1 and backdate the token past the refresh threshold ({@code PAST_REFRESH_S}s = 61s).
     * <LI>Authenticate as user2 and backdate the token past the refresh threshold ({@code PAST_REFRESH_S}s = 61s).
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
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m, dynamicExpirationValidation=true");

        String user1AgedCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_REFRESH_S, method);
        String user2AgedCookie = authenticateAndBackdateToken(url, "user2", "user2pwd", PAST_REFRESH_S, method);

        assertFalse("Different users must have different backdated cookies", user1AgedCookie.equals(user2AgedCookie));

        String user1RefreshedCookie = ssoRequestExpectingRefresh(url, user1AgedCookie, "user1 SSO (refresh expected)", method);
        String user2RefreshedCookie = ssoRequestExpectingRefresh(url, user2AgedCookie, "user2 SSO (refresh expected)", method);

        assertFalse("Refreshed cookies must differ between users", user1RefreshedCookie.equals(user2RefreshedCookie));
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Start server with baseline config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m, dynamicExpirationValidation=true.
     * <LI>Switch server to non-refresh config: expiration=2m, no inactivityTimeout, no refreshThreshold.
     * <LI>Authenticate as user1 and backdate the token past the former refresh threshold ({@code PAST_REFRESH_S}s = 61s).
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

        // Feature is OFF: a past-threshold token must not be refreshed.
        String agedCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_REFRESH_S, method);

        HttpURLConnection conn = ssoRequest(url, agedCookie, "SSO past refreshThreshold (no refresh expected)", method);
        assertEquals("SSO must succeed, token only expires at absolute expiration without inactivityTimeout", 200, conn.getResponseCode());
        assertTokenNotRefreshed("No refresh expected — refreshThreshold and inactivityTimeout not configured", conn);
        conn.disconnect();
    }

    // =========================================================================
    // Inactivity Timeout Tests
    // =========================================================================

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m, dynamicExpirationValidation=true.
     * <LI>Authenticate as user1 and backdate the token past the inactivity timeout ({@code PAST_INACTIVITY_S}s = 121s).
     * <LI>Send an SSO request with the backdated cookie.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>The SSO request is rejected with HTTP 401.
     * <LI>The inactivity timeout is enforced before absolute expiration is reached.
     * </OL>
     */
    @Test
    public void testTokenRejectedAfterInactivityTimeoutDT() throws Exception {
        String url = getRefreshServletUrl();
        String method = "testTokenRejectedAfterInactivityTimeoutDT";

        Log.info(thisClass, method, "expiration=3m, inactivityTimeout=2m, refreshThreshold=1m, dynamicExpirationValidation=true");
        String expiredCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_INACTIVITY_S, method);
        HttpURLConnection conn = ssoRequest(url, expiredCookie, "SSO after inactivity timeout (rejection expected)", method);
        assertEquals("Idle token must be rejected with 401", 401, conn.getResponseCode());
        conn.disconnect();
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m, dynamicExpirationValidation=false.
     * <LI>Authenticate as user1 and backdate the token past the inactivity timeout ({@code PAST_INACTIVITY_S}s = 121s).
     * <LI>Send an SSO request with the backdated cookie.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>The SSO request is rejected with HTTP 401.
     * <LI>The inactivity timeout is enforced before absolute expiration is reached.
     * </OL>
     */
    @Test
    public void testTokenRejectedAfterInactivityTimeoutDF() throws Exception {
        setConfig(CFG_TOKEN_REFRESH_DYN_EXP_VAL_FALSE);
        String url = getRefreshServletUrl();
        String method = "testTokenRejectedAfterInactivityTimeoutDF";

        Log.info(thisClass, method, "expiration=3m, inactivityTimeout=2m, refreshThreshold=1m, dynamicExpirationValidation=false");
        String expiredCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_INACTIVITY_S, method);
        HttpURLConnection conn = ssoRequest(url, expiredCookie, "SSO after inactivity timeout (rejection expected)", method);
        assertEquals("Idle token must be rejected with 401", 401, conn.getResponseCode());
        conn.disconnect();
    }

    // =========================================================================
    // Misconfiguration / Warning Tests
    // =========================================================================

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=2m, refreshThreshold=1m, no inactivityTimeout.
     * <LI>Assert that warning CWWKS4127W is logged (refreshThreshold set without inactivityTimeout).
     * <LI>Authenticate as user1 and backdate the token past the refresh threshold ({@code PAST_REFRESH_S}s = 61s).
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

        String agedCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_REFRESH_S, method);

        HttpURLConnection conn = ssoRequest(url, agedCookie, "SSO past refreshThreshold (no refresh expected)", method);
        assertEquals("SSO must succeed, token only expires at absolute expiration without inactivityTimeout", 200, conn.getResponseCode());
        assertTokenNotRefreshed("No refresh expected — refreshThreshold has no effect without inactivityTimeout", conn);
        conn.disconnect();
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=2m, inactivityTimeout=1m, no refreshThreshold.
     * <LI>Assert that warning CWWKS4126W is logged (inactivityTimeout set without refreshThreshold).
     * <LI>Authenticate as user1 and backdate the token past the inactivity timeout ({@code PAST_REFRESH_S}s = 61s).
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

        String agedCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_REFRESH_S, method);

        HttpURLConnection conn = ssoRequest(url, agedCookie, "SSO past inactivity timeout (no refresh expected)", method);
        assertEquals("Inactivity timeout should be disabled, SSO must succeed.", 200, conn.getResponseCode());
        assertTokenNotRefreshed("Cookie should not be refreshed", conn);
        conn.disconnect();
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=3m, inactivityTimeout=4m, refreshThreshold=2m (inactivityTimeout exceeds expiration).
     * <LI>Assert that warning CWWKS4125W is logged (inactivityTimeout &gt;= expiration).
     * <LI>Authenticate as user1 and backdate the token past the absolute expiration ({@code PAST_EXPIRY_S}s = 181s).
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
        setConfig(CFG_TOKEN_INACTIVITY_EXCEEDS_EXPIRY);
        String url = getRefreshServletUrl();
        String method = "testTokenInactivityTimeoutExceedsExpiration";
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=4m, refreshThreshold=2m");

        assertWarningLogged("CWWKS4125W", "inactivityTimeout >= expiration", method);

        String expiredCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_EXPIRY_S, method);

        HttpURLConnection conn = ssoRequest(url, expiredCookie, "SSO after expiration (rejection expected)", method);
        assertEquals("Token must be rejected at expiration", 401, conn.getResponseCode());
        conn.disconnect();
    }
 
    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=3m, inactivityTimeout=3m, refreshThreshold=2m (inactivityTimeout equals expiration).
     * <LI>Assert that warning CWWKS4125W is logged (inactivityTimeout &gt;= expiration).
     * <LI>Authenticate as user1 and backdate the token past the absolute expiration ({@code PAST_EXPIRY_S}s = 181s).
     * <LI>Send an SSO request with the backdated cookie.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>Warning CWWKS4125W is emitted at startup.
     * <LI>The SSO request is rejected with HTTP 401 — the token has reached absolute expiration.
     * </OL>
     */
    @Test
    public void testTokenInactivityTimeoutEqualsExpiration() throws Exception {
        setConfig(CFG_TOKEN_INACTIVITY_EQUALS_EXPIRY);
        String url = getRefreshServletUrl();
        String method = "testTokenInactivityTimeoutEqualsExpiration";
        Log.info(thisClass, method, "Config: expiration=3m, inactivityTimeout=3m, refreshThreshold=2m");

        assertWarningLogged("CWWKS4125W", "inactivityTimeout >= expiration", method);

        String expiredCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_EXPIRY_S, method);

        HttpURLConnection conn = ssoRequest(url, expiredCookie, "SSO after expiration (rejection expected)", method);
        assertEquals("Token must be rejected at expiration", 401, conn.getResponseCode());
        conn.disconnect();
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=6m, inactivityTimeout=3m, refreshThreshold=4m (refreshThreshold exceeds inactivityTimeout).
     * <LI>Assert that warning CWWKS4124W is logged — refreshThreshold is auto-adjusted to inactivityTimeout/3 (1m).
     * <LI>Authenticate as user1 and backdate the token past the adjusted threshold ({@code PAST_INACTIVITY_S}s = 121s).
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
        setConfig(CFG_TOKEN_REFRESH_EXCEEDS_INACTIVITY);
        String url = getRefreshServletUrl();
        String method = "testRefreshThresholdExceedsInactivityTimeout";
        Log.info(thisClass, method, "Config: expiration=6m, inactivityTimeout=3m, refreshThreshold=4m; " +
                                    "expect refreshthreshold auto-adjust to 1m (inactivityTimeout/3)");

        assertWarningLogged("CWWKS4124W", "refreshThreshold(4m) >= inactivityTimeout(3m) and refreshThreshold < expiration", method);

        String agedCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_INACTIVITY_S, method);

        ssoRequestExpectingRefresh(url, agedCookie, "SSO after adjusted threshold (refresh expected)", method);
    }
    
    /**
     * Tests the following:
     * <OL>
     * <LI>Config: expiration=6m, inactivityTimeout=3m, refreshThreshold=3m (refreshThreshold equals inactivityTimeout).
     * <LI>Assert that warning CWWKS4124W is logged — refreshThreshold is auto-adjusted to inactivityTimeout/3 (1m).
     * <LI>Authenticate as user1 and backdate the token past the adjusted threshold ({@code PAST_INACTIVITY_S}s = 121s).
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
    public void testRefreshThresholdEqualsInactivityTimeout() throws Exception {
        setConfig(CFG_TOKEN_REFRESH_EQUALS_INACTIVITY);
        String url = getRefreshServletUrl();
        String method = "testRefreshThresholdEqualsInactivityTimeout";
        Log.info(thisClass, method, "Config: expiration=6m, inactivityTimeout=3m, refreshThreshold=3m; " +
                                    "expect refreshthreshold auto-adjust to 1m (inactivityTimeout/3)");

        assertWarningLogged("CWWKS4124W", "refreshThreshold(3m) >= inactivityTimeout(3m) and refreshThreshold < expiration", method);

        String agedCookie = authenticateAndBackdateToken(url, "user1", "user1pwd", PAST_INACTIVITY_S, method);

        ssoRequestExpectingRefresh(url, agedCookie, "SSO after adjusted threshold (refresh expected)", method);
    }

    // =========================================================================
    // Mixed Environment Tests (Refresh Server <-> Non-Refresh Server)
    // =========================================================================

    /**
     * Tests the following:
     * <OL>
     * <LI>Refresh server config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m, dynamicExpirationValidation=true.
     * <LI>Non-refresh server config: expiration=3m, no inactivityTimeout, no refreshThreshold.
     * <LI>Both servers share the same LTPA key set.
     * <LI>Authenticate as user1 on the refresh server and backdate the token past the inactivity timeout (121s).
     * <LI>Present the timed-out token to the non-refresh server.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>The non-refresh server rejects the token with HTTP 401 — the absolute expiration in the token
     *     is the inactivity timeout because of dynamicExpirationValidation, so the non-refresh server
     *     calculates the token as expired.
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
        HttpURLConnection conn = ssoRequest(nonRefreshUrl, agedCookie, "refresh server token on non-refresh server", method);
        assertEquals("Token from refresh server must be rejected by non-refresh server", 401, conn.getResponseCode());
        conn.disconnect();
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Refresh server config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m, dynamicExpirationValidation=false.
     * <LI>Non-refresh server config: expiration=3m, no inactivityTimeout, no refreshThreshold.
     * <LI>Both servers share the same LTPA key set.
     * <LI>Authenticate as user1 on the refresh server and backdate the token past the inactivity timeout ({@code PAST_INACTIVITY_S}s = 121s).
     * <LI>Present the timed-out token to the non-refresh server.
     * </OL>
     * <P>Expected Results:
     * <OL>
     * <LI>The non-refresh server accepts the token with HTTP 200 — because dynamicExpirationValidation=false,
     *     the token's expiry field encodes the configured expiration (3m) rather than the inactivity timeout (2m),
     *     so the non-refresh server does not consider the token expired at 121s.
     * </OL>
     */
    @Test
    public void testRefreshTokenAcceptedByNonRefreshServerAfterInactivityTimeout() throws Exception {
        setConfig(CFG_TOKEN_REFRESH_DYN_EXP_VAL_FALSE);
        String nonRefreshUrl = getNonRefreshServletUrl();
        String refreshUrl = getRefreshServletUrl();
        String method = "testRefreshTokenAcceptedByNonRefreshServerAfterInactivityTimeout";
        Log.info(thisClass, method, "Refresh server config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m, dynamicExpirationValidation=false");

        String agedCookie = authenticateAndBackdateToken(refreshUrl, "user1", "user1pwd", PAST_INACTIVITY_S, method);

        HttpURLConnection conn = ssoRequest(nonRefreshUrl, agedCookie, "refresh server token on non-refresh server", method);
        assertEquals("Token from refresh server must be accepted by non-refresh server when dynamicExpirationValidation=false", 200, conn.getResponseCode());
        conn.disconnect();
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Refresh server config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m, dynamicExpirationValidation=true.
     * <LI>Non-refresh server config: expiration=3m, no inactivityTimeout, no refreshThreshold.
     * <LI>Both servers share the same LTPA key set.
     * <LI>Authenticate as user1 on the non-refresh server and backdate the token past the refresh threshold ({@code PAST_REFRESH_S}s = 61s).
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

        String agedCookie = authenticateAndBackdateToken(nonRefreshUrl, "user1", "user1pwd", PAST_REFRESH_S, method);

        ssoRequestExpectingRefresh(refreshUrl, agedCookie, "non-refresh server token on refresh server (refresh expected)", method);
    }

    /**
     * Tests the following:
     * <OL>
     * <LI>Refresh server config: expiration=3m, inactivityTimeout=2m, refreshThreshold=1m, dynamicExpirationValidation=true.
     * <LI>Non-refresh server config: expiration=3m, no inactivityTimeout, no refreshThreshold.
     * <LI>Both servers share the same LTPA key set.
     * <LI>Authenticate as user1 on the refresh server and backdate the token past the refresh threshold ({@code PAST_REFRESH_S}s = 61s).
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

        // Authenticate on the refresh server and backdate the token past the refresh threshold (61s).
        String agedCookie = authenticateAndBackdateToken(refreshUrl, "user1", "user1pwd", PAST_REFRESH_S, method);

        // Present the backdated token to the non-refresh server — accepted but not refreshed.
        HttpURLConnection conn = ssoRequest(nonRefreshUrl, agedCookie, "refresh server token past threshold on non-refresh server (acceptance, no refresh expected)", method);
        assertEquals("Token from refresh server must be accepted by non-refresh server", 200, conn.getResponseCode());
        assertTokenNotRefreshed("Non-refresh server must not issue a new cookie — it has no refreshThreshold configured", conn);
        conn.disconnect();
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    /**
     * Authenticates via Basic Auth, requests the servlet to backdate the issued token by
     * {@code ageSeconds}, and returns the resulting {@code LtpaToken2} cookie value.
     */
    private String authenticateAndBackdateToken(String url, String username, String password, int ageSeconds, String method) throws IOException {
        String backdateUrl = url + "?action=backdate&offsetSeconds=" + ageSeconds;
        Log.info(thisClass, method, "authenticating as " + username + " with token age=" + ageSeconds + "s via " + backdateUrl);
        HttpURLConnection conn = makeAuthenticatedRequest(backdateUrl, username, password);
        assertEquals("Backdate request must succeed for " + username, 200, conn.getResponseCode());
        String cookie = readBackdatedTokenFromBody(conn);
        assertNotNull("Servlet must write a BACKDATED_TOKEN line for " + username, cookie);
        Log.info(thisClass, method, "cookie for " + username + ": " + maskCookie(cookie));
        conn.disconnect();
        return cookie;
    }

    /**
     * Sends a cookie-only SSO request and asserts that the server issues a new distinct
     * {@code LtpaToken2} cookie (HTTP 200). Returns the new cookie value.
     */
    private String ssoRequestExpectingRefresh(String url, String cookie, String label, String method) throws IOException {
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
    private HttpURLConnection ssoRequest(String url, String cookie, String label, String method) throws IOException {
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
        assertNull(message + " — but got new cookie", newCookie);
    }

    /**
     * Opens a GET connection to {@code urlString} and sets a Basic Authorization header
     * for the given credentials.
     */
    private HttpURLConnection makeAuthenticatedRequest(String urlString, String username, String password) throws IOException {
        HttpURLConnection conn = openConnection(urlString);
        conn.setRequestProperty("Authorization",
            "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes()));
        return conn;
    }

    /**
     * Reads the response body and returns the backdated token value from the line prefixed
     * with {@code LTPATestServlet.BACKDATED_TOKEN_PREFIX},
     * or {@code null} if no such line is present.
     */
    private String readBackdatedTokenFromBody(HttpURLConnection conn) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(LTPATestServlet.BACKDATED_TOKEN_PREFIX)) {
                    return line.substring(LTPATestServlet.BACKDATED_TOKEN_PREFIX.length()).trim();
                }
            }
        }
        return null;
    }

    /**
     * Extracts the raw {@code LtpaToken2} cookie value from the last matching
     * {@code Set-Cookie} response header, or {@code null} if no such header is present.
     * Assumes the header format is {@code LtpaToken2=<value>;...} (no spaces around {@code =}).
     */
    private String extractCookie(HttpURLConnection conn) {
        String header = getCookieHeader(conn.getHeaderFields());
        if (header == null) return null;
        int start = LTPA_COOKIE.length() + 1; // skip "LtpaToken2="
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
        // Place validation1.keys from publish/files/alternate/ into resources/security/
        // (same pattern as LTPAKeyPasswordTests).
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
     *   <li>{@code CWWKG0018I} — Configuration updated (may indicate no functional change,
     *       e.g. restoring the baseline config that the server is already running).
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
