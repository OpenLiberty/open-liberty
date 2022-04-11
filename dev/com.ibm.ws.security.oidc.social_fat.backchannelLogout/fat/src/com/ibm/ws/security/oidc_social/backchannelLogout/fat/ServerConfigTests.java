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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.oidc_social.backchannelLogout.fat.CommonTests.BackChannelLogoutCommonTests;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * This test class contains tests that validate the proper behavior in end-to-end logouts and end_session requests.
 * These tests will focus on the proper logout/end_session behavior based on the OP and OAuth registered client
 * configs.
 **/

@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException", "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidTokenException" })
public class ServerConfigTests extends BackChannelLogoutCommonTests {

    // Repeat tests using the OIDC and Social endpoints
    // also repeat using different client back ends (local store, Derby, Mongo)
    @ClassRule
    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(Constants.OIDC));
    //            .andWith(new SecurityTestRepeatAction(SocialConstants.SOCIAL));

    private static final String fakeJSessionId = "thisIsAPlaceholderJSessionId";

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

        testOPServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.op", "op_server_configTests.xml", Constants.OIDC_OP, serverApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);

        if (RepeatTestFilter.getRepeatActionsAsString().contains(Constants.OIDC)) {
            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.rp", "rp_server_configTests.xml", Constants.OIDC_RP, clientApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);
            clientCookieName = "testRPCookie";
            updateClientCookieNameAndPort(clientServer, "clientCookieName", clientCookieName);
            //            clientServer2 = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.rp.2", "rp_server_configTests.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);
            //            client2CookieName = "testRP2Cookie";
            //            updateClientCookieNameAndPort(clientServer2, "clientCookieName", client2CookieName);
            testSettings.setFlowType(Constants.RP_FLOW);
        } else {
            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.social", "social_server_configTests.xml", Constants.OIDC_RP, clientApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);
            clientCookieName = "testSocialCookie";
            updateClientCookieNameAndPort(clientServer, "clientCookieName", clientCookieName);
            // TODO - add second client
            testSettings.setFlowType("Social_Flow");

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

    protected void resetBCLAppCounter() throws Exception {
        genericInvokeEndpoint(_testName, getAndSaveWebClient(true), null, clientServer.getHttpsString() + "/backchannelLogoutTestApp/backChannelLogoutLogMsg",
                Constants.GETMETHOD, "resetBCLCounter", null, null, vData.addSuccessStatusCodes(), testSettings);

    }

    protected void validateCorrectBCLUrisCalled(TestServer server, TestSettings settings) throws Exception {

        String logout_token = getLogoutTokenFromMessagesLog(server, "BackChannelLogout_logMsg_Servlet - 1 logout_token:");
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
     * Show that a back channel logout that takes a bit longer than usual will not timeout (as the delay is still less than the
     * time).
     *
     */
    @Test
    public void ServerConfigTests_defaultBCLTimeout() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_defaultBCLTimeout", "bcl_defaultBCLTimeout");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        String refreshToken = getRefreshToken(response);
        String jSessionId = getCookieValue(webClient, Constants.JSESSION_ID_COOKIE);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(postLogoutJSessionIdApp);

        genericOP(_testName, webClient, updatedTestSettings, Constants.LOGOUT_ONLY_ACTIONS, logoutExpectations, response, null);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        validateLogoutResult(webClient, updatedTestSettings, clientCookieName, refreshToken, jSessionId, successfulOPLogout, unsuccessfulRPLogout);

    }

    /**
    *
    */
    @Test
    public void ServerConfigTests_defaultBCLTimeout_multipleLoginsWithinOP() throws Exception {

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_defaultBCLTimeout", "bcl_defaultBCLTimeout");
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_defaultBCLTimeout", "bcl_otherDefaultBCLTimeout");

        Object response = accessProtectedApp(webClient1, updatedTestSettings1);
        response = accessProtectedApp(webClient2, updatedTestSettings2);

        String refreshToken = getRefreshToken(response);
        String jSessionId = getCookieValue(webClient1, Constants.JSESSION_ID_COOKIE);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(postLogoutJSessionIdApp);

        genericOP(_testName, webClient1, updatedTestSettings1, Constants.LOGOUT_ONLY_ACTIONS, logoutExpectations, response, null);

        validateLogoutResult(webClient1, updatedTestSettings1, clientCookieName, refreshToken, jSessionId, successfulOPLogout, unsuccessfulRPLogout);

    }

    /**
    *
    */
    @ExpectedFFDC({ "java.util.concurrent.CancellationException" })
    @Test
    public void ServerConfigTests_shortBCLTimeout() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_shortBCLTimeout", "bcl_shortBCLTimeout");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        String refreshToken = getRefreshToken(response);
        String jSessionId = getCookieValue(webClient, Constants.JSESSION_ID_COOKIE);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(postLogoutJSessionIdApp);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a there was a problem involing the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_TIMEOUT);

        genericOP(_testName, webClient, updatedTestSettings, Constants.LOGOUT_ONLY_ACTIONS, logoutExpectations, response, null);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        validateLogoutResult(webClient, updatedTestSettings, clientCookieName, refreshToken, jSessionId, successfulOPLogout, unsuccessfulRPLogout);

    }

    //    /**
    //    *
    //    */
    //    @ExpectedFFDC({ "java.util.concurrent.CancellationException" })
    //    //chc@Test
    //    public void ServerConfigTests_shortBCLTimeout_multipleLogins() throws Exception {
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
    //        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a there was a problem involing the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_TIMEOUT);
    //
    //        genericOP(_testName, webClient1, updatedTestSettings, Constants.LOGOUT_ONLY_ACTIONS, expectations, response1, null);
    //
    //    }

    @Test
    public void ServerConfigTests_invalidBackchannelLogoutUri() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_invalidBCLUri");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        String refreshToken = getRefreshToken(response);
        String jSessionId = getCookieValue(webClient, Constants.JSESSION_ID_COOKIE);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(postLogoutJSessionIdApp);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a there was a problem involing the back channel logout.", MessageConstants.CWWKS1647E_BACK_CHANNEL_LOGOUT_INVALID_URI);

        genericOP(_testName, webClient, updatedTestSettings, Constants.LOGOUT_ONLY_ACTIONS, logoutExpectations, response, null);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        validateLogoutResult(webClient, updatedTestSettings, clientCookieName, refreshToken, jSessionId, successfulOPLogout, unsuccessfulRPLogout);

    }

    @Test
    public void ServerConfigTests_omittedBackchannelLogoutUri() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_omittedBCLUri");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        String refreshToken = getRefreshToken(response);
        String jSessionId = getCookieValue(webClient, Constants.JSESSION_ID_COOKIE);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(postLogoutJSessionIdApp);

        genericOP(_testName, webClient, updatedTestSettings, Constants.LOGOUT_ONLY_ACTIONS, logoutExpectations, response, null);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        validateLogoutResult(webClient, updatedTestSettings, clientCookieName, refreshToken, jSessionId, successfulOPLogout, unsuccessfulRPLogout);

    }

    @Test
    public void ServerConfigTests_backchannelLogoutUri_returns400() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns400");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        String refreshToken = getRefreshToken(response);
        String jSessionId = getCookieValue(webClient, Constants.JSESSION_ID_COOKIE);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(postLogoutJSessionIdApp);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem involing the back channel logout.", MessageConstants.CWWKS1647E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_400_Servlet.*");

        genericOP(_testName, webClient, updatedTestSettings, Constants.LOGOUT_ONLY_ACTIONS, logoutExpectations, response, null);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        validateLogoutResult(webClient, updatedTestSettings, clientCookieName, refreshToken, jSessionId, successfulOPLogout, unsuccessfulRPLogout);

    }

    @Test
    public void ServerConfigTests_backchannelLogoutUri_returns501() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns501");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        String refreshToken = getRefreshToken(response);
        String jSessionId = getCookieValue(webClient, Constants.JSESSION_ID_COOKIE);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(postLogoutJSessionIdApp);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem involing the back channel logout.", MessageConstants.CWWKS1647E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_501_Servlet.*");

        genericOP(_testName, webClient, updatedTestSettings, Constants.LOGOUT_ONLY_ACTIONS, logoutExpectations, response, null);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        validateLogoutResult(webClient, updatedTestSettings, clientCookieName, refreshToken, jSessionId, successfulOPLogout, unsuccessfulRPLogout);

    }

    @Test
    public void ServerConfigTests_backchannelLogoutUri_returnsMultipleDifferentFailures() throws Exception {

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns400");
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns501");

        Object response1 = accessProtectedApp(webClient1, updatedTestSettings1);
        Object response2 = accessProtectedApp(webClient2, updatedTestSettings2);

        String refreshToken1 = getRefreshToken(response1);
        String jSessionId1 = getCookieValue(webClient1, Constants.JSESSION_ID_COOKIE);

        String refreshToken2 = getRefreshToken(response2);
        String jSessionId2 = getCookieValue(webClient2, Constants.JSESSION_ID_COOKIE);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(postLogoutJSessionIdApp);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem involing the back channel logout.", MessageConstants.CWWKS1647E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_400_Servlet.*");
        // TODO enabled next line when runtime is updated to make multiple calls
        //        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem involing the back channel logout.", MessageConstants.CWWKS1647E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_501_Servlet.*");

        genericOP(_testName, webClient1, updatedTestSettings1, Constants.LOGOUT_ONLY_ACTIONS, logoutExpectations, response1, null);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        validateLogoutResult(webClient1, updatedTestSettings1, clientCookieName, refreshToken1, jSessionId1, successfulOPLogout, unsuccessfulRPLogout);
        // check that the other refresh token was disabled
        refreshTokens(webClient2, updatedTestSettings2, refreshToken2, false); // TODO once we handle multiple clients, switch false to true

    }

    //chc@Test
    public void ServerConfigTests_backchannelLogoutUri_returnsMultipleSameFailures() throws Exception {

        // the app that this test uses will record a count of how many times its been called, reset the count at the beginning of the test
        resetBCLAppCounter();

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);
        WebClient webClient3 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_logsMsg");
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_logsMsg");
        TestSettings updatedTestSettings3 = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_logsMsg");

        Object response = accessProtectedApp(webClient1, updatedTestSettings1);
        response = accessProtectedApp(webClient2, updatedTestSettings2);
        response = accessProtectedApp(webClient3, updatedTestSettings3);

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(postLogoutJSessionIdApp);
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet - 1.*");
        // TODO - enabled the next two lines when runtime is updated to make multiple calls.
        //        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet - 2.*");
        //        expectations = validationTools.addMessageExpectation(clientServer, expectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet - 3.*");
        logoutExpectations = addOPLogoutCookieExpectations(logoutExpectations, successfulOPLogout); // OP cookie should be gone
        logoutExpectations = addRPLogoutCookieExpectations(logoutExpectations, clientCookieName, fakeJSessionId, unsuccessfulOPLogout); // client cookie should still exist since bcl uri's just return different status and don't log anything out

        // Logout
        genericOP(_testName, webClient1, updatedTestSettings1, Constants.LOGOUT_ONLY_ACTIONS, logoutExpectations, response, null);

    }

    @Test
    public void ServerConfigTests_confirmBCLUriCalledForEachLogin() throws Exception {

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
        List<validationData> logoutExpectations = initLogoutExpectations(defaultLogoutPage);
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet - 1.*");
        //        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet - 2.*");
        //        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet - 3.*");
        //        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet - 4.*");

        // Logout
        genericOP(_testName, webClient1, updatedTestSettings1, Constants.LOGOUT_ONLY_ACTIONS, logoutExpectations, response1, null);

        // make sure that the backChannelLogoutUri has been called for each client that we logged in (within the provider that we're logging out)
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings1);
        // TODO enable the next 3 lines after multiple clients can be logged out
        //        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings2);
        //        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings3);
        //        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings4);
    }
    //    @Test
    //    public void ServerConfigTests_xx() throws Exception {
    //
    //        WebClient webClient = getAndSaveWebClient(true);
    //
    //        invokeGenericForm_refreshToken(_testName,  webClient,  testSettings, refresh_token, expectations) ;
    //    }
    // do and don't invoke back channel logout
}