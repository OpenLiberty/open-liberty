/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.oidc_social.backchannelLogout.fat;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.oidc_social.backchannelLogout.fat.CommonTests.BackChannelLogoutCommonTests;
import com.ibm.ws.security.oidc_social.backchannelLogout.fat.utils.SkipIfHttpLogout;
import com.ibm.ws.security.oidc_social.backchannelLogout.fat.utils.TokenKeeper;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServerWrapper;

// TODO - review test output once 20799 is resolved - right now, I'm seeing multiple calls the 400/500 test apps that should
// probably only be one call
// once end_session is called for a provider, it shouldn't call the bcl for those clients until they're logged in again

/**
 * This test class contains tests that validate the proper behavior in end-to-end end_session requests.
 * These tests will focus on the proper logout/end_session behavior based on the OP and OAuth registered client
 * configs.
 **/

@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException", "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidTokenException" })
public class BasicBCLTests extends BackChannelLogoutCommonTests {

    protected static Class<?> thisClass = BasicBCLTests.class;

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    // Repeat tests using the OIDC and Social endpoints
    // also repeat using different client back ends (local store, Derby, Mongo)
    @ClassRule
    //    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(Constants.OIDC))
    //            .andWith(new SecurityTestRepeatAction(SocialConstants.SOCIAL));
    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(Constants.OIDC + "_" + Constants.END_SESSION))
            .andWith(new SecurityTestRepeatAction(Constants.OIDC + "_" + Constants.LOGOUT));
    //            .andWith(new SecurityTestRepeatAction(SocialConstants.SOCIAL + "_" + Constants.END_SESSION))
    //            .andWith(new SecurityTestRepeatAction(SocialConstants.SOCIAL + "_" + Constants.LOGOUT));

    //    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(SocialConstants.SOCIAL));

    private static final String fakeJSessionId = "thisIsAPlaceholderJSessionId";
    private static String finalApp = null;
    private static String defaultApp = null;
    protected static String logoutApp = null;

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        // bypass LDAP setup which isn't needed and wastes a good bit of time.
        useLdap = false;
        Log.info(thisClass, "beforeClass", "Set useLdap to: " + useLdap);

        List<String> clientApps = new ArrayList<String>() {
            {
                add(Constants.APP_FORMLOGIN);
            }
        };
        List<String> serverApps = new ArrayList<String>() {
            {
                add(Constants.OAUTHCLIENT_APP);
            }
        };

        testOPServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.op", "op_server_basicTests.xml", Constants.OIDC_OP, serverApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);

        if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.OIDC)) {
            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.rp", "rp_server_basicTests.xml", Constants.OIDC_RP, clientApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);
            clientCookieName = "testRPCookie";
            updateClientCookieNameAndPort(clientServer, "clientCookieName", clientCookieName);
            //            clientServer2 = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.rp.2", "rp_server_basicTests.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);
            //            client2CookieName = "testRP2Cookie";
            //            updateClientCookieNameAndPort(clientServer2, "clientCookieName", client2CookieName);
            testSettings.setFlowType(Constants.RP_FLOW);
        } else {
            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.social", "social_server_basicTests.xml", Constants.OIDC_RP, clientApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);
            clientCookieName = "testSocialCookie";
            updateClientCookieNameAndPort(clientServer, "clientCookieName", clientCookieName);
            // TODO - add second client
            testSettings.setFlowType("Social_Flow");

        }

        // Setup some common values that vary on the type of logout we'll be doing.
        if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.END_SESSION)) {
            logoutMethodTested = Constants.END_SESSION;
            finalApp = postLogoutJSessionIdApp;
            defaultApp = defaultLogoutPage;
            SkipIfHttpLogout.usingHttpLogout = false;
        } else {
            logoutMethodTested = Constants.LOGOUT;
            finalApp = simpleLogoutApp;
            defaultApp = simpleLogoutApp;
            logoutApp = testOPServer.getHttpsString() + "/simpleLogoutTestApp/simpleLogout";
            SkipIfHttpLogout.usingHttpLogout = true;
        }
    }

    protected TestSettings updateTestSettingsProviderAndClient(String provider, String client) throws Exception {

        return updateTestSettingsProviderAndClient(clientServer, provider, client, true);

    }

    protected TestSettings updateTestSettingsProviderAndClient(String provider, String client, boolean usePostLogout) throws Exception {

        return updateTestSettingsProviderAndClient(clientServer, provider, client, usePostLogout);

    }

    protected TestSettings updateTestSettingsProviderAndClient(TestServer server, String provider, String client, boolean usePostLogout) throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();

        updatedTestSettings.setTestURL(server.getHttpsString() + "/formlogin/simple/" + client);
        // set logout url - end_session
        updatedTestSettings.setEndSession(updatedTestSettings.getEndSession().replace("OidcConfigSample", provider));
        updatedTestSettings.setTokenEndpt(updatedTestSettings.getTokenEndpt().replace("OidcConfigSample", provider));
        updatedTestSettings.setClientID(client);
        updatedTestSettings.setClientSecret("mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger"); // all of the clients are using the same secret
        updatedTestSettings.setScope("openid profile");
        if (usePostLogout) {
            updatedTestSettings.setPostLogoutRedirect(server.getHttpString() + postLogoutJSessionIdApp);
        } else {
            updatedTestSettings.setPostLogoutRedirect(null);
        }

        return updatedTestSettings;

    }

    protected void resetBCL400AppCounter() throws Exception {
        genericInvokeEndpoint(_testName, getAndSaveWebClient(true), null, clientServer.getHttpsString() + "/backchannelLogoutTestApp/backChannelLogout400",
                Constants.GETMETHOD, "resetBCLCounter", null, null, vData.addSuccessStatusCodes(), testSettings);

    }

    protected void resetBCLAppCounter() throws Exception {
        genericInvokeEndpoint(_testName, getAndSaveWebClient(true), null, clientServer.getHttpsString() + "/backchannelLogoutTestApp/backChannelLogoutLogMsg",
                Constants.GETMETHOD, "resetBCLCounter", null, null, vData.addSuccessStatusCodes(), testSettings);

    }

    protected void validateCorrectBCLUrisCalled(TestServer server, TestSettings settings) throws Exception {

        String logout_token = getLogoutTokenFromMessagesLog(server, "BackChannelLogout_logMsg_Servlet: " + settings.getClientID() + ".* logout_token:");
        Log.info(thisClass, _testName, "logout_token: " + logout_token);
        JwtTokenForTest logoutTokenData = gatherDataFromToken(logout_token, settings);
        String audience = logoutTokenData.getMapPayload().get(Constants.PAYLOAD_AUDIENCE).toString();
        if (audience.contains(settings.getClientID())) {
            Log.info(thisClass, _testName, "Back Channel Logout was called for: " + settings.getClientID());
        } else {
            fail("Back Channel Logout was not called for: " + settings.getClientID());
        }
    }

    /**
     * Invoke either end_session or the test "simpleLogout" app
     *
     * @param webClient
     *            the client to get the context from
     * @param settings
     *            the current test settings (used to get the end_session endpoint)
     * @param logoutExpectations
     *            the expectations to validate
     * @param previousResponse
     *            in the case of end_session, the prevous response to get the id_token from (to use as the hint)
     * @return the http reponse for further validation
     * @throws Exception
     */
    public Object invokeLogout(WebClient webClient, TestSettings settings, List<validationData> logoutExpectations, Object previousResponse) throws Exception {

        if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.END_SESSION)) {
            return genericOP(_testName, webClient, settings, Constants.LOGOUT_ONLY_ACTIONS, logoutExpectations, previousResponse, null);
        } else {
            return genericInvokeEndpoint(_testName, webClient, null, logoutApp,
                    Constants.POSTMETHOD, Constants.LOGOUT, null, null, logoutExpectations, testSettings);
        }

    }

    /**
     * Show that a back channel logout that takes a bit longer than usual will not timeout (as the delay is still less than the
     * time).
     *
     */
    @Test
    public void BasicBCLTests_defaultBCLTimeout() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_defaultBCLTimeout", "bcl_defaultBCLTimeout");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = new TokenKeeper(response, logoutMethodTested);
        String jSessionId = getCookieValue(webClient, Constants.JSESSION_ID_COOKIE);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalApp);

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        validateLogoutResult(webClient, updatedTestSettings, clientCookieName, tokens.getRefreshToken(), jSessionId, successfulOPLogout, unsuccessfulRPLogout);

    }

    /**
    *
    */
    @Test
    public void BasicBCLTests_defaultBCLTimeout_multipleLoginsWithinOP() throws Exception {

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_defaultBCLTimeout", "bcl_defaultBCLTimeout");
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_defaultBCLTimeout", "bcl_otherDefaultBCLTimeout");

        Object response = accessProtectedApp(webClient1, updatedTestSettings1);
        TokenKeeper tokens1 = new TokenKeeper(response, logoutMethodTested);

        response = accessProtectedApp(webClient2, updatedTestSettings2);
        TokenKeeper tokens2 = new TokenKeeper(response, logoutMethodTested);

        String jSessionId = getCookieValue(webClient1, Constants.JSESSION_ID_COOKIE);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalApp);

        invokeLogout(webClient2, updatedTestSettings2, logoutExpectations, response);

        // TODO - only the refresh_token for the passed id_token will be cleaned up
        validateLogoutResult(webClient2, updatedTestSettings2, clientCookieName, tokens2.getRefreshToken(), jSessionId, successfulOPLogout, unsuccessfulRPLogout);

        // TODO end_session only revoking the refresh_token for the id_token
        refreshTokens(webClient1, updatedTestSettings1, tokens1.getRefreshToken(), refreshTokenValid);

    }

    /**
    *
    */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipIfHttpLogout.class)
    //    @ExpectedFFDC({ "java.util.concurrent.CancellationException" })  TODO - replace allowed with expected once req.logout() call bcl
    @AllowedFFDC({ "java.util.concurrent.CancellationException" })
    @Test
    public void BasicBCLTests_shortBCLTimeout() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_shortBCLTimeout", "bcl_shortBCLTimeout");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = new TokenKeeper(response, logoutMethodTested);
        
        String jSessionId = getCookieValue(webClient, Constants.JSESSION_ID_COOKIE);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalApp);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1649E_BACK_CHANNEL_LOGOUT_TIMEOUT);

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        validateLogoutResult(webClient, updatedTestSettings, clientCookieName, tokens.getRefreshToken(), jSessionId, successfulOPLogout, unsuccessfulRPLogout);

    }

    //    /**
    //    *
    //    */
    //    @ExpectedFFDC({ "java.util.concurrent.CancellationException" })
    //    //chc@Test
    //    public void BasicBCLTests_shortBCLTimeout_multipleLogins() throws Exception {
    //
    //        WebClient webClient1 = getAndSaveWebClient(true);
    //        WebClient webClient2 = getAndSaveWebClient(true);
    //
    //        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_shortBCLTimeout", "bcl_shortBCLTimeout");
    //
    //        Object response1 = accessProtectedApp(webClient1, updatedTestSettings);
    //        Object response2 = accessProtectedApp(webClient2, updatedTestSettings);
    //
    //        List<validationData> expectations = initLogoutExpectations();
    //        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_TIMEOUT);
    //
    //       invokeLogout(webClient1, updatedTestSettings, expectations, response1);
    //
    //    }

    @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipIfHttpLogout.class)
    @Test
    public void BasicBCLTests_invalidBackchannelLogoutUri() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_invalidBCLUri");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = new TokenKeeper(response, logoutMethodTested);

        String jSessionId = getCookieValue(webClient, Constants.JSESSION_ID_COOKIE);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalApp);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI);

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        validateLogoutResult(webClient, updatedTestSettings, clientCookieName, tokens.getRefreshToken(), jSessionId, successfulOPLogout, unsuccessfulRPLogout);

    }

    @Test
    public void BasicBCLTests_omittedBackchannelLogoutUri() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_omittedBCLUri");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = new TokenKeeper(response, logoutMethodTested);

        String jSessionId = getCookieValue(webClient, Constants.JSESSION_ID_COOKIE);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalApp);

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        validateLogoutResult(webClient, updatedTestSettings, clientCookieName, tokens.getRefreshToken(), jSessionId, successfulOPLogout, unsuccessfulRPLogout);

    }

    @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipIfHttpLogout.class)
    @Test
    public void BasicBCLTests_backchannelLogoutUri_returns400() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns400");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = new TokenKeeper(response, logoutMethodTested);
        
        String jSessionId = getCookieValue(webClient, Constants.JSESSION_ID_COOKIE);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalApp);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_400_Servlet.*");

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        validateLogoutResult(webClient, updatedTestSettings, clientCookieName, tokens.getRefreshToken(), jSessionId, successfulOPLogout, unsuccessfulRPLogout);

    }

    @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipIfHttpLogout.class)
    @Test
    public void BasicBCLTests_backchannelLogoutUri_returns501() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns501");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = new TokenKeeper(response, logoutMethodTested);
        String jSessionId = getCookieValue(webClient, Constants.JSESSION_ID_COOKIE);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalApp);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_501_Servlet.*");

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        validateLogoutResult(webClient, updatedTestSettings, clientCookieName, tokens.getRefreshToken(), jSessionId, successfulOPLogout, unsuccessfulRPLogout);

    }

    @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipIfHttpLogout.class)
    @Test
    public void BasicBCLTests_backchannelLogoutUri_returnsMultipleDifferentFailures() throws Exception {

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns400");
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns501");

        Object response1 = accessProtectedApp(webClient1, updatedTestSettings1);
        Object response2 = accessProtectedApp(webClient2, updatedTestSettings2);

        TokenKeeper tokens1 = new TokenKeeper(response1, logoutMethodTested);
        String jSessionId1 = getCookieValue(webClient1, Constants.JSESSION_ID_COOKIE);

        TokenKeeper tokens2 = new TokenKeeper(response2, logoutMethodTested);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalApp);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_400_Servlet.*");
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_501_Servlet.*");

        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, response1);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        validateLogoutResult(webClient1, updatedTestSettings1, clientCookieName, tokens1.getRefreshToken(), jSessionId1, successfulOPLogout, unsuccessfulRPLogout);
        // check that the other refresh token was disabled
        refreshTokens(webClient2, updatedTestSettings2, tokens2.getRefreshToken(), refreshTokenValid); // TODO once we handle multiple clients, switch false to true

    }

    @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipIfHttpLogout.class)
    @Test
    public void BasicBCLTests_backchannelLogoutUri_returnsMultipleSameFailures() throws Exception {

        // the app that this test uses will record a count of how many times its been called, reset the count at the beginning of the test
        resetBCL400AppCounter();

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);
        WebClient webClient3 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns400");
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns400");
        TestSettings updatedTestSettings3 = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns400");

        Object response = accessProtectedApp(webClient1, updatedTestSettings1);
        response = accessProtectedApp(webClient2, updatedTestSettings2);
        response = accessProtectedApp(webClient3, updatedTestSettings3);

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(finalApp);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_400_Servlet - 1.*");
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_400_Servlet - 2.*");
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_400_Servlet - 3.*");

        // TODO - I think these checks can be removed - they shouldn't really prove anything
        // TODO - enabled the next two lines when runtime is updated to make multiple calls.
        //        logoutExpectations = addOPLogoutCookieExpectations(logoutExpectations, successfulOPLogout); // OP cookie should be gone
        //        logoutExpectations = addRPLogoutCookieExpectations(logoutExpectations, clientCookieName, fakeJSessionId, unsuccessfulOPLogout); // client cookie should still exist since bcl uri's just return different status and don't log anything out

        // Logout
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, response);

    }

    @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipIfHttpLogout.class)
    @Test
    public void BasicBCLTests_confirmBCLUriCalledForEachLogin() throws Exception {

        // the app that this test uses will record a count of how many times its been called, reset the count at the beginning of the test
        resetBCLAppCounter();

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);
        WebClient webClient3 = getAndSaveWebClient(true);
        WebClient webClient4 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_logger", "loggerClient1", false);
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_logger", "loggerClient2", false);
        TestSettings updatedTestSettings3 = updateTestSettingsProviderAndClient("OidcConfigSample_logger", "loggerClient3", false);
        TestSettings updatedTestSettings4 = updateTestSettingsProviderAndClient("OidcConfigSample_logger", "loggerClient4", false);

        Object response1 = accessProtectedApp(webClient1, updatedTestSettings1);
        accessProtectedApp(webClient2, updatedTestSettings2);
        accessProtectedApp(webClient3, updatedTestSettings3);
        accessProtectedApp(webClient4, updatedTestSettings4);

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(defaultApp);
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings1.getClientID() + "*");
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings2.getClientID() + "*");
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings3.getClientID() + "*");
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings4.getClientID() + "*");

        // Logout
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, response1);

        // make sure that the backChannelLogoutUri has been called for each client that we logged in (within the provider that we're logging out)
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings1);
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings2);
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings3);
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings4);
    }
    //    @Test
    //    public void BasicBCLTests_xx() throws Exception {
    //
    //        WebClient webClient = getAndSaveWebClient(true);
    //
    //        invokeGenericForm_refreshToken(_testName,  webClient,  testSettings, refresh_token, expectations) ;
    //    }
    // do and don't invoke back channel logout
}