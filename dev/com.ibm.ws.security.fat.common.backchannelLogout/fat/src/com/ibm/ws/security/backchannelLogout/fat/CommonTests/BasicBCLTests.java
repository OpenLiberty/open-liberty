/*******************************************************************************
 * Copyright (c) 2022, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.backchannelLogout.fat.CommonTests;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.backchannelLogout.fat.utils.AfterLogoutStates;
import com.ibm.ws.security.backchannelLogout.fat.utils.BackChannelLogout_RegisterClients;
import com.ibm.ws.security.backchannelLogout.fat.utils.Constants;
import com.ibm.ws.security.backchannelLogout.fat.utils.SkipIfSocialClient;
import com.ibm.ws.security.backchannelLogout.fat.utils.SkipIfUsesMongoDB;
import com.ibm.ws.security.backchannelLogout.fat.utils.SkipIfUsesMongoDBOrSocialClient;
import com.ibm.ws.security.backchannelLogout.fat.utils.TokenKeeper;
import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.fat.common.social.SocialConstants;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.AppPasswordsAndTokensCommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.AppPasswordsAndTokensCommonTest.TokenValues;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings.StoreType;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.custom.junit.runner.TestModeFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * This test class contains tests that validate the proper behavior in end-to-end end_session requests.
 * These tests will focus on the proper logout/end_session behavior based on the OP and OAuth registered client
 * configs.
 **/

@SuppressWarnings("serial")
@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException", "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidTokenException" })
public class BasicBCLTests extends BackChannelLogoutCommonTests {

    protected static Class<?> thisClass = BasicBCLTests.class;

    protected static AppPasswordsAndTokensCommonTest appPTcommon = new AppPasswordsAndTokensCommonTest();

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    /**
     * Repeat tests using OIDC (with a Local or Custom Store) or OIDC with SAML OP's, OIDC and Social clients, end_session or http
     * session logout and when http session logout, have it invoke either the logout or end_session in the OP
     * While it would be nice to run all types of clients with all server configuration (and easy to do), it just takes too long
     * to run
     * Variations:
     * OIDC_end_session_MONGODB - OIDC Client - invoke end_session on the OP
     * OIDC_end_session_LOCALSTORE - OIDC Client - invoke end_session on the OP
     * OIDC_http_session_end_session - OIDC Client - invoke test app on the RP that does a HttpServletRequest.logout(), then
     * invokes the end_session endpoint of the OP Provider
     * OIDC_http_session_logout - OIDC Client - invoke test app on the RP that does a HttpServletRequest.logout(), then invokes
     * the logout endpoint of the OP Provider
     * Social_end_session - Social Client - invoke end_session on the OP
     * Social_http_session_end_session - Social Client - invoke test app on the Social Client that does a
     * HttpServletRequest.logout(), then invokes the end_session endpoint of the OP Provider
     * Social_http_session_logout - Social Client - invoke test app on the Social Client that does a HttpServletRequest.logout(),
     * then invokes the logout endpoint of the OP Provider
     * SAML_logout - OP with SAML to perform authorize - invoke logout on the IDP
     *
     * @return RepeatTests object for each variation of this class that will be run
     */
    public static RepeatTests createRepeats(String callingProject) {

        String localStore = "LOCALSTORE";

        String theOS = null;
        try {
            theOS = testOPServer.getServer().getMachine().getOperatingSystem().name();
        } catch (Exception e) {
            Log.info(thisClass, "createRepeats", e.getMessage());
            Log.info(thisClass, "createRepeats", "Received an exception trying to determine if the current OS is iSeries - Mongo DB tests would be skipped on iSeries - assume that it's not iSeries");
            theOS = "assumeNotISeries";
        }
        // note:  using the method addRepeat below instead of adding test repeats in line to simplify hacking up the tests locally to ony run one or 2 variations (all the calls are the same - dont' have to worry about using "with" vs "andWith")
        RepeatTests rTests = null;
        if (callingProject.equals(Constants.OIDC)) {
            if (TestModeFilter.shouldRun(TestMode.FULL)) {
                if (!theOS.equals("ISERIES")) {
                    rTests = addRepeat(rTests, new SecurityTestRepeatAction(Constants.OIDC + "_" + Constants.END_SESSION + "_" + Constants.MONGODB));
                }
                rTests = addRepeat(rTests, new SecurityTestRepeatAction(Constants.OIDC + "_" + Constants.END_SESSION + "_" + localStore));
                rTests = addRepeat(rTests, new SecurityTestRepeatAction(Constants.OIDC + "_" + Constants.LOGOUT_ENDPOINT + "_" + localStore));
                rTests = addRepeat(rTests, new SecurityTestRepeatAction(Constants.OIDC + "_" + Constants.HTTP_SESSION + "_" + Constants.LOGOUT_ENDPOINT));
                rTests = addRepeat(rTests, new SecurityTestRepeatAction(Constants.OIDC + "_" + Constants.HTTP_SESSION + "_" + Constants.END_SESSION));
            } else {
                // LITE mode only run one instance
                rTests = addRepeat(rTests, new SecurityTestRepeatAction(Constants.OIDC + "_" + Constants.END_SESSION + "_" + localStore));
            }
        } else {
            if (callingProject.equals(Constants.SOCIAL)) {
                rTests = addRepeat(rTests, new SecurityTestRepeatAction(SocialConstants.SOCIAL + "_" + Constants.END_SESSION));
                // LITE mode only run one instance
                if (TestModeFilter.shouldRun(TestMode.FULL)) {
                    rTests = addRepeat(rTests, new SecurityTestRepeatAction(SocialConstants.SOCIAL + "_" + Constants.LOGOUT_ENDPOINT));
                    rTests = addRepeat(rTests, new SecurityTestRepeatAction(SocialConstants.SOCIAL + "_" + Constants.HTTP_SESSION + "_" + Constants.LOGOUT_ENDPOINT));
                    rTests = addRepeat(rTests, new SecurityTestRepeatAction(SocialConstants.SOCIAL + "_" + Constants.HTTP_SESSION + "_" + Constants.END_SESSION));
                }
            } else {
                if (TestModeFilter.shouldRun(TestMode.FULL)) {
                    //                    rTests = addRepeat(rTests, new SecurityTestRepeatAction(Constants.SAML_IDP_INITIATED_LOGOUT));
                    //                    rTests = addRepeat(rTests, new SecurityTestRepeatAction(Constants.SAML + "_" + Constants.logout)); sp logout
                    rTests = addRepeat(rTests, new SecurityTestRepeatAction(Constants.SAML + "_" + Constants.LOGOUT_ENDPOINT));
                    //                    rTests = addRepeat(rTests, new SecurityTestRepeatAction(Constants.SAML + "_" + Constants.END_SESSION));
                    //                rTests = addRepeat(rTests, new SecurityTestRepeatAction(Constants.SAML + "_" + Constants.HTTP_SESSION + "_" + Constants.LOGOUT_ENDPOINT));
                    //                rTests = addRepeat(rTests, new SecurityTestRepeatAction(Constants.SAML + "_" + Constants.HTTP_SESSION + "_" + Constants.END_SESSION));
                } else {
                    // LITE mode only run one instance
                    rTests = addRepeat(rTests, new SecurityTestRepeatAction(Constants.SAML + "_" + Constants.END_SESSION));
                }

            }
        }
        // TODO - should we try to test with SP initiated and ibm_security_logout flows using saml?

        return rTests;

    }

    @BeforeClass
    public static void setUp() throws Exception {

        currentRepeatAction = RepeatTestFilter.getRepeatActionsAsString();
        initSkipFlags();

        makeRandomSettingSelections();

        // set some test flags based on the repeat action - these flags will be used by the test cases and test methods to determine the steps to run or what to expect
        setConfigBasedOnRepeat();

        // Start a normal OP, or an OP that uses SAML to authorize (in this case, we need to fire up a server running Shibboleth
        startProviderBasedOnRepeat(tokenType);

        // start an OIDC RP or a Social oidc client
        startClientBasedOnRepeat(tokenType);

        registerClientsIfNeeded();

    }

    /**
     * If the current repeat is a SAML variation, start a Shibboleth IDP and an OP with a samlWebSso20 client. That client will be
     * used to authorize using the SAML IDP.
     * Otherwise, start a standard OIDC OP.
     *
     * @param tokenType
     *            flag to be passed to common tooling to set config settings in the OP to have it create opaque or jwt
     *            access_tokens
     * @throws Exception
     */
    public static void startProviderBasedOnRepeat(String tokenType) throws Exception {

        List<String> serverApps = new ArrayList<String>() {
            {
                add(Constants.OAUTHCLIENT_APP); // need this app to get the refresh forms
            }
        };

        if (loginMethod.equals(Constants.SAML)) {
            Log.info(thisClass, "setUp", "pickAnIDP: " + pickAnIDP);
            testIDPServer = commonSetUp("com.ibm.ws.security.saml.sso-2.0_fat.shibboleth", "server_orig.xml", Constants.IDP_SERVER_TYPE, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.SKIP_CHECK_FOR_SECURITY_STARTED, true);
            pickAnIDP = true; // tells commonSetup to update the OP server files with current/this instance IDP server info
            testOPServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.op.saml", adjustServerConfig("op_server_basicTests.xml"), Constants.OIDC_OP, serverApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT);
            // now, we need to update the IDP files
            shibbolethHelpers.fixSPInfoInShibbolethServer(testOPServer, testIDPServer);
            shibbolethHelpers.fixVarsInShibbolethServerWithDefaultValues(testIDPServer);
            // now, start the shibboleth app with the updated config info
            shibbolethHelpers.startShibbolethApp(testIDPServer);
            testOPServer.addIgnoredServerException(MessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);
        } else {
            useLdap = false;
            Log.info(thisClass, "beforeClass", "Set useLdap to: " + useLdap);
            if (currentRepeatAction.contains(Constants.MONGODB)) {
                List<String> extraMsgs = new ArrayList<String>();
                extraMsgs.add("CWWKZ0001I.*" + Constants.OAUTHCONFIGMONGO_START_APP);
                testOPServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.op.mongo", adjustServerConfig("op_server_basicTests.xml"), Constants.OIDC_OP, serverApps, Constants.DO_NOT_USE_DERBY, Constants.USE_MONGODB, extraMsgs, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT, Constants.JUNIT_REPORTING);
                // register clients after all servers are started and we know everyone's ports
                testSettings.setStoreType(StoreType.CUSTOM);
                SkipIfUsesMongoDB.usesMongoDB = true;
                SkipIfUsesMongoDBOrSocialClient.usesMongoDB = true;
            } else {
                testOPServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.op", adjustServerConfig("op_server_basicTests.xml"), Constants.OIDC_OP, serverApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT);
            }
        }

        Map<String, String> vars = new HashMap<String, String>();

        if (currentRepeatAction.contains(SocialConstants.SOCIAL)) {
            // social client
            vars.put("bclRoot", "ibm/api/social-login/backchannel_logout");
        } else {
            // openidconnect client
            vars.put("bclRoot", "oidcclient/backchannel_logout");
        }

        updateServerSettings(testOPServer, vars);

        SecurityFatHttpUtils.saveServerPorts(testOPServer.getServer(), Constants.BVT_SERVER_1_PORT_NAME_ROOT);
        testOPServer.addIgnoredServerExceptions(MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI, MessageConstants.CWWKS5215E_NO_AVAILABLE_SP);

    }

    /**
     * If the current repeat is an OIDC or SAML variation, start an OIDC RP client, otherwise, start a Social oidc client
     *
     * @param tokenType
     *            flag to be passed to common tooling to set config settings in the OP to have it create opaque or jwt
     *            access_tokens
     * @throws Exception
     */
    public static void startClientBasedOnRepeat(String tokenType) throws Exception {

        List<String> clientApps = new ArrayList<String>() {
            {
                add(Constants.APP_FORMLOGIN);
                add(Constants.backchannelLogoutApp);
            }
        };
        // For tests using httpsessionlogout, we need an intermediate app to perform the logout (including making calls to individual bcl endpoints on the RPs)
        if (currentRepeatAction.contains(Constants.HTTP_SESSION)) {
            clientApps.add(Constants.simpleLogoutApp);
        }

        Map<String, String> vars = new HashMap<String, String>();
        if (loginMethod.equals(Constants.OIDC) || loginMethod.equals(Constants.SAML)) {
            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.rp", adjustServerConfig("rp_server_basicTests.xml"), Constants.OIDC_RP, clientApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT);
            vars = updateClientCookieNameAndPort(clientServer, "clientCookieName", Constants.clientCookieName);
            testSettings.setFlowType(Constants.RP_FLOW);
        } else {
            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.social", adjustServerConfig("social_server_basicTests.xml"), Constants.OIDC_RP, clientApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT);
            vars = updateClientCookieNameAndPort(clientServer, "clientCookieName", Constants.clientCookieName);
            testSettings.setFlowType(SocialConstants.SOCIAL);
            clientServer.addIgnoredServerExceptions(MessageConstants.CWWKG0058E_CONFIG_MISSING_REQUIRED_ATTRIBUTE, MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP, MessageConstants.CWWKS1725E_VALIDATION_ENDPOINT_URL_NOT_VALID_OR_FAILED); // the social client isn't happy with the public client not having a secret
            SkipIfSocialClient.socialClient = true;
            SkipIfUsesMongoDBOrSocialClient.socialClient = true;
        }

        updateServerSettings(clientServer, vars);

        SecurityFatHttpUtils.saveServerPorts(clientServer.getServer(), Constants.BVT_SERVER_2_PORT_NAME_ROOT);

        clientServer.addIgnoredServerExceptions(MessageConstants.CWWKS1541E_BACK_CHANNEL_LOGOUT_ERROR, MessageConstants.CWWKS1543E_BACK_CHANNEL_LOGOUT_REQUEST_VALIDATION_ERROR);

        if (currentRepeatAction.contains(Constants.HTTP_SESSION)) {
            logoutApp = clientServer.getHttpsString() + "/simpleLogoutTestApp/simpleLogout";
        }

    }

    public static void registerClientsIfNeeded() throws Exception {

        if (currentRepeatAction.contains(Constants.MONGODB)) {
            Log.info(thisClass, "setUP", "Setting up mongo clients");
            regClients = new BackChannelLogout_RegisterClients(testOPServer, clientServer, null);
            regClients.registerClientsForBasicBCLTests();

        }
    }

    /**
     * Update test settings with test case specific values - caller assumes user/password will be testuser/testuserpwd and that we
     * will be using a post logout
     *
     * @param provider
     *            the OP provider that the openidconnect client belongs to
     * @param client
     *            the openidconnect client that the test uses - we're using this as part of the test app names
     * @return updated test settings
     * @throws Exception
     */
    protected TestSettings updateTestSettingsProviderAndClient(String provider, String client) throws Exception {

        return updateTestSettingsProviderAndClient(provider, client, true);

    }

    /**
     * Update test settings with test case specific values - caller assumes user/password will be testuser/testuserpwd
     *
     * @param provider
     *            the OP provider that the openidconnect client belongs to
     * @param client
     *            the openidconnect client that the test uses - we're using this as part of the test app names
     * @param usePostLogout
     *            flag indicating if OP Provider config that the test uses specifies a post logout url this will (used to tell the
     *            logout test method to pass the post logout url in its request - it has to match whats in the config - the values
     *            set based on the flag also tells our validation code where we would finally land)
     * @return updated test settings
     * @throws Exception
     */
    protected TestSettings updateTestSettingsProviderAndClient(String provider, String client, boolean usePostLogout) throws Exception {

        return updateTestSettingsProviderAndClient(clientServer, provider, client, Constants.TESTUSER, Constants.TESTUSERPWD, usePostLogout);

    }

    /**
     * Update test settings with test case specific values
     *
     * @param server
     *            the clientServer instance that the test will use (there may be multiple RPs/Social clients
     * @param provider
     *            the OP provider that the openidconnect client belongs to
     * @param client
     *            the openidconnect client that the test uses - we're using this as part of the test app names
     * @param user
     *            the test user to use
     * @param passwd
     *            the password for the test user
     * @param usePostLogout
     *            flag indicating if OP Provider config that the test uses specifies a post logout url this will (used to tell the
     *            logout test method to pass the post logout url in its request - it has to match whats in the config - the values
     *            set based on the flag also tells our validation code where we would finally land)
     * @return updated test settings
     * @throws Exception
     */
    protected TestSettings updateTestSettingsProviderAndClient(TestServer server, String provider, String client, String user, String passwd, boolean usePostLogout) throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();

        updatedTestSettings.setProvider(provider);
        updatedTestSettings.setTestURL(server.getHttpsString() + "/formlogin/simple/" + client);
        updatedTestSettings.setProtectedResource(server.getHttpsString() + "/formlogin/simple/" + client);
        // set logout url - end_session
        if (logoutMethodTested.equals(Constants.SAML_IDP_INITIATED_LOGOUT)) { // update for idp/sp initiated
            updatedTestSettings.setEndSession(testIDPServer.getHttpsString() + "/idp/profile/Logout");
        } else {
            updatedTestSettings.setEndSession(updatedTestSettings.getEndSession().replace("OidcConfigSample", provider));
            if (logoutMethodTested.equals(Constants.LOGOUT_ENDPOINT)) {
                updatedTestSettings.setEndSession(updatedTestSettings.getEndSession().replace(Constants.END_SESSION_ENDPOINT, Constants.LOGOUT_ENDPOINT));
            }
        }
        updatedTestSettings.setTokenEndpt(updatedTestSettings.getTokenEndpt().replace("OidcConfigSample", provider));
        updatedTestSettings.setClientID(client);
        updatedTestSettings.setClientSecret("mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger"); // all of the clients are using the same secret
        updatedTestSettings.setUserName(user);
        updatedTestSettings.setUserPassword(passwd);
        updatedTestSettings.setAdminUser(user);
        updatedTestSettings.setAdminPswd(passwd);
        updatedTestSettings.setScope("openid profile");
        if (usePostLogout) {
            updatedTestSettings.setPostLogoutRedirect(server.getHttpString() + Constants.postLogoutJSessionIdApp);
        } else {
            updatedTestSettings.setPostLogoutRedirect(null);
        }

        return updatedTestSettings;

    }

    /**
     * Invoke the back channel logout app that issues a 400 status to reset its counter
     * Each time the app is called (during a logout), it returns a 400 status code, logs a message and increments a counter.
     * When we have tests that expect multiple bcl logouts to occur, we check the count created by this app to verify that the
     * correct number of logouts occurred.
     * This method causes that counter to be reset.
     *
     * @throws Exception
     */
    protected void resetBCL400AppCounter() throws Exception {
        genericInvokeEndpoint(_testName, getAndSaveWebClient(true), null, clientServer.getHttpsString() + "/backchannelLogoutTestApp/backChannelLogout400",
                Constants.GETMETHOD, "resetBCLCounter", null, null, vData.addSuccessStatusCodes(), testSettings);

    }

    /**
     * Invoke the back channel logout app that logs a message and counts the number of times it is invoked
     * Each time the app is called (during a logout), it logs a message and increments a counter.
     * When we have tests that expect multiple bcl logouts to occur, we check the count created by this app to verify that the
     * correct number of logouts occurred.
     * This method causes that counter to be reset.
     *
     * @throws Exception
     */
    protected void resetBCLAppCounter() throws Exception {
        genericInvokeEndpoint(_testName, getAndSaveWebClient(true), null, clientServer.getHttpsString() + "/backchannelLogoutTestApp/backChannelLogoutLogMsg",
                Constants.PUTMETHOD, "resetBCLCounter", null, null, vData.addSuccessStatusCodes(), testSettings);

    }

    /**
     * This method invokes the back channel logout msg app using the get method which logs the count of how many times it was
     * called (via post - which is how the OP will invoke the bcl endpoint). I then searches the reponse for the count and compare
     * that to the number the caller indicates should be found.
     *
     * @param count
     *            the number of times we expect the bcl endpoint to have been called for the calling test.
     * @throws Exception
     */
    protected void checkLogMsgCount(int count) throws Exception {

        String action = "printCallCount";
        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addResponseStatusExpectation(expectations, action, Constants.OK_STATUS);
        expectations = vData.addExpectation(expectations, action, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not find the proper count (" + count + ") in the response", null, "BackChannelLogout_logMsg_Servlet called number of times: " + Integer.toString(count));

        genericInvokeEndpoint(_testName, getAndSaveWebClient(true), null, clientServer.getHttpsString() + "/backchannelLogoutTestApp/backChannelLogoutLogMsg",
                Constants.GETMETHOD, action, null, null, expectations, testSettings);

    }

    /**
     * Validates that we have invoked the correct back channel logout endpoints.
     * The test bcl endpoint app will log the client id and the logout_token used in the request.
     * This method will retrieve that logout token and validate that it was built for the proper client login. We will compare
     * contents from the test settings and the sid from the original login for that client.
     * Assumes that id_token_hint was included in the logout request and that we'll skip the sid check.
     *
     * @param server
     *            the client server instance (needed to obtain the logout_token from the bcl endpoint app output)
     * @param settings
     *            the test settings used for the original client login
     * @return returns the sid from the logout_token
     * @throws Exception
     */
    protected String validateCorrectBCLUrisCalled(TestServer server, TestSettings settings) throws Exception {
        return validateCorrectBCLUrisCalled(server, settings, null);

    }

    /**
     * Validates that we have invoked the correct back channel logout endpoints.
     * The test bcl endpoint app will log the client id and the logout_token used in the request.
     * This method will retrieve that logout token and validate that it was built for the proper client login. We will compare
     * contents from the test settings and the sid from the original login for that client.
     * Assumes that the id_token_hink was included in the logout request.
     *
     * @param server
     *            the client server instance (needed to obtain the logout_token from the bcl endpoint app output)
     * @param settings
     *            the test settings used for the original client login
     * @param sidFromIdToken
     *            The original sid from the id_token
     * @return returns the sid from the logout_token
     * @throws Exception
     */
    protected String validateCorrectBCLUrisCalled(TestServer server, TestSettings settings, String sidFromIdToken) throws Exception {
        return validateCorrectBCLUrisCalled(server, settings, true, sidFromIdToken);
    }

    /**
     * Validates that we have invoked the correct back channel logout endpoints.
     * The test bcl endpoint app will log the client id and the logout_token used in the request.
     * This method will retrieve that logout token and validate that it was built for the proper client login. We will compare
     * contents from the test settings and the sid from the original login for that client.
     *
     * @param server
     *            the client server instance (needed to obtain the logout_token from the bcl endpoint app output)
     * @param settings
     *            the test settings used for the original client login
     * @param idTokenHintIncluded
     *            Flag indicating if the logout request passed the id_token_hint
     * @param sidFromIdToken
     *            The original sid from the id_token
     * @return returns the sid from the logout_token
     * @throws Exception
     */
    protected String validateCorrectBCLUrisCalled(TestServer server, TestSettings settings, boolean idTokenHintIncluded, String sidFromIdToken) throws Exception {

        String logout_token = getLogoutTokenFromMessagesLog(server, "BackChannelLogout_logMsg_Servlet: " + settings.getClientID() + ".* logout_token:");
        Log.info(thisClass, _testName, "logout_token: " + logout_token);

        if (idTokenHintIncluded) {
            TokenKeeper logoutToken = new TokenKeeper(logout_token);

            List<String> audience = logoutToken.getAudience();
            if (audience.contains(settings.getClientID())) {
                Log.info(thisClass, _testName, "Back Channel Logout was called for: " + settings.getClientID());
            } else {
                fail("Back Channel Logout was not called for audience: " + settings.getClientID());
            }

            if (sidFromIdToken == null) {
                Log.info(thisClass, _testName, "Skipping sid check");
            } else {
                if (logoutToken.getSessionId().contains(sidFromIdToken)) {
                    Log.info(thisClass, _testName, "Back Channel Logout was called for: " + sidFromIdToken);
                } else {
                    fail("Back Channel Logout was not called for sid: " + sidFromIdToken);
                }
            }
            return logoutToken.getSessionId();
        } else {
            if (logout_token == null) {
                addToAllowableTimeoutCount(1); // the search for the logout_token will result in a "timed out" message that we need to account for
                Log.info(thisClass, _testName, "Back Channel Logout was NOT called for: " + sidFromIdToken);
            } else {
                fail("Back Channel Logout was called for sid: " + sidFromIdToken + " and should NOT have been");
            }
            return null;
        }
    }

    protected List<String> validateCorrectBCLUrisCalled_noOrder(TestServer server, TestSettings settings, boolean idTokenHintIncluded, String sidFromIdToken) throws Exception {

        List<String> logout_tokens = getLogoutTokensFromMessagesLog(server, "BackChannelLogout_logMsg_Servlet: " + settings.getClientID() + ".* logout_token:");
        List<String> sids = new ArrayList<String>();

        if (idTokenHintIncluded) {
            for (String logout_token : logout_tokens) {
                Log.info(thisClass, _testName, "logout_token: " + logout_token);
                TokenKeeper logoutToken = new TokenKeeper(logout_token);

                List<String> audience = logoutToken.getAudience();
                if (audience.contains(settings.getClientID())) {
                    Log.info(thisClass, _testName, "Back Channel Logout was called for: " + settings.getClientID());
                } else {
                    fail("Back Channel Logout was not called for audience: " + settings.getClientID());
                }

                sids.add(logoutToken.getSessionId());
            }

            if (sidFromIdToken == null) {
                Log.info(thisClass, _testName, "Skipping sid check");
            } else {
                boolean found = false;
                for (String sid : sids) {
                    if (sid.contains(sidFromIdToken)) {
                        Log.info(thisClass, _testName, "Back Channel Logout was called for: " + sidFromIdToken);
                        found = true;
                    }
                }
                if (!found) {
                    fail("Back Channel Logout was not called for sid: " + sidFromIdToken);
                }
            }
            return sids;
            //            return logoutToken.getSessionId();

        } else {
            if (logout_tokens.size() == 0) {
                addToAllowableTimeoutCount(1); // the search for the logout_token will result in a "timed out" message that we need to account for
                Log.info(thisClass, _testName, "Back Channel Logout was NOT called for: " + sidFromIdToken);
            } else {
                fail("Back Channel Logout was called for sid: " + sidFromIdToken + " and should NOT have been");
            }
            return null;
        }
    }

    /**
     * Invoke either end_session or the test "simpleLogout" app - and if invoking "simpleLogout" have the app invoke
     * either the logout endpoint in the OP, or end_session
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

        String thisMethod = "invokeLogout";

        String opLogoutEndpoint = null;

        //        // Debug
        //        Log.info(thisClass, thisMethod, "Debug logoutMethodTested: " + logoutMethodTested);
        //        Log.info(thisClass, thisMethod, "Debug finalApp: " + finalAppWithPostRedirect);
        //        Log.info(thisClass, thisMethod, "Debug defaultApp: " + finalAppWithoutPostRedirect);
        //        Log.info(thisClass, thisMethod, "Debug logoutApp: " + logoutApp);
        //        Log.info(thisClass, thisMethod, "Debug sessionLogoutEndpoint: " + sessionLogoutEndpoint);

        switch (logoutMethodTested) {
        case Constants.SAML_IDP_INITIATED_LOGOUT: // update for idp/sp initiated
            return genericOP(_testName, webClient, settings, Constants.IDP_INITIATED_LOGOUT, logoutExpectations, previousResponse, null);
        case Constants.END_SESSION:
        case Constants.LOGOUT_ENDPOINT:
            // invoke end_session on the op - test controls if the id_token is passed as the id_token_hint by either passing or not passing the previous response
            return genericOP(_testName, webClient, settings, Constants.LOGOUT_ONLY_ACTIONS, logoutExpectations, previousResponse, null);
        case Constants.HTTP_SESSION:
            String id_token = null;
            if (sessionLogoutEndpoint.equals(Constants.LOGOUT_ENDPOINT)) {
                opLogoutEndpoint = testOPServer.getHttpsString() + "/oidc/endpoint/" + settings.getProvider() + "/" + Constants.LOGOUT_ENDPOINT;
            } else {
                if (previousResponse != null) {
                    id_token = validationTools.getIDToken(settings, previousResponse);
                }
                opLogoutEndpoint = settings.getEndSession();
            }
            List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "opLogoutUri", opLogoutEndpoint);
            if (id_token != null) {
                parms = eSettings.addEndpointSettingsIfNotNull(parms, "id_token_hint", id_token);
            }
            return genericInvokeEndpoint(_testName, webClient, null, logoutApp, Constants.POSTMETHOD, Constants.LOGOUT, parms, null, logoutExpectations, testSettings);
        default:
            fail("Logout method wasn't specified");
            return null;
        }

    }

    /**
     * Create expectations for the condition where we try to use a logout_token as an access token
     *
     * @param action
     *            the action/step where the failure will occur
     * @param settings
     *            the test case settings
     * @param validationMethod
     *            the method used to validate the token passed (part of the client config) - this will determine the failure that
     *            we'll receive
     * @return returns expectations used to verify proper behavior
     * @throws Exception
     */
    public List<validationData> setUseLogoutTokenAsAccessTokenExpectations(String action, TestSettings settings, String validationMethod) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addResponseStatusExpectation(expectations, action, Constants.OK_STATUS);
        expectations = vData.addExpectation(expectations, action, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not land on the login page", null, "Login");
        expectations = vData.addExpectation(expectations, action, Constants.RESPONSE_URL, Constants.STRING_DOES_NOT_CONTAIN, "Landed on the test app after a logout and should NOT have", null, settings.getTestURL());
        expectations = validationTools.addMessageExpectation(clientServer, expectations, action, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a there was a problem validating the access_token for inbound propagation.", MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP);

        if (validationMethod.equals(Constants.INTROSPECTION_ENDPOINT)) {
            expectations = validationTools.addMessageExpectation(clientServer, expectations, action, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a there was a problem validating the access_token.", MessageConstants.CWWKS1725E_VALIDATION_ENDPOINT_URL_NOT_VALID_OR_FAILED);
        } else {
            expectations = validationTools.addMessageExpectation(clientServer, expectations, action, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a there was a problem validating the access_token.", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE);
        }
        return expectations;
    }

    /**
     * Most of the tests use a localstore - this method checks for msg CWWKS2300E for a specific client - the message will be
     * logged during server startup.
     * We have to reset the mark to the top of the log and look for <client>*CWWKS2300E.
     *
     * @param client
     *            the client that msg CWWKS2300E should reference
     * @throws Exception
     */
    public void testForInvalidPublicClientUsingHttpAtStartup(String client) throws Exception {
        // make sure that there is an error message for this client from startup
        testOPServer.resetLogMarks();
        try {
            Log.info(thisClass, "startProviderBasedOnRepeat", "Searching for " + client + ".*" + MessageConstants.CWWKS2300E_HTTP_WITH_PUBLIC_CLIENT + " in the trace.log file");
            String searchMsg = testOPServer.getServer().waitForStringInLog(client + ".*" + MessageConstants.CWWKS2300E_HTTP_WITH_PUBLIC_CLIENT, testOPServer.getServer().getMatchingLogFile(Constants.TRACE_LOG));
            if (searchMsg == null) {
                fail("Did not find message " + MessageConstants.CWWKS2300E_HTTP_WITH_PUBLIC_CLIENT + " in the OP Trace log");
            } else {
                Log.info(thisClass, "startProviderBasedOnRepeat", "Found: " + searchMsg);
            }
        } catch (Exception e) {
            Log.info(thisClass, "startProviderBasedOnRepeat", e.toString());
            fail("Failed trying to detect " + MessageConstants.CWWKS2300E_HTTP_WITH_PUBLIC_CLIENT + " in the OP Trace");
        } finally {
            testOPServer.setMarkToEndOfLogs();
        }

    }

    public List<validationData> setTooManyLoginsExpectations() throws Exception {

        String loginStep = ((logoutMethodTested.equals(Constants.SAML) ? Constants.PERFORM_IDP_LOGIN : Constants.LOGIN_USER));

        List<validationData> tooManyLoginsExpectations = vData.addSuccessStatusCodes(null, loginStep);
        tooManyLoginsExpectations = vData.addResponseStatusExpectation(tooManyLoginsExpectations, loginStep, Constants.BAD_REQUEST_STATUS);
        tooManyLoginsExpectations = vData.addExpectation(tooManyLoginsExpectations, loginStep, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive a message stating that there are too many logins for the user.", null, MessageConstants.CWOAU0066E_EXCEEDED_MAX_USER_REQUESTS);
        tooManyLoginsExpectations = validationTools.addMessageExpectation(testOPServer, tooManyLoginsExpectations, loginStep, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that there are too many logins for the user and client id.", MessageConstants.CWOAU0054E_EXCEEDED_USER_CLIENT_TOKEN_LIMIT);
        tooManyLoginsExpectations = validationTools.addMessageExpectation(testOPServer, tooManyLoginsExpectations, loginStep, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that there are too many login requests for the user.", MessageConstants.CWOAU0066E_EXCEEDED_MAX_USER_REQUESTS);

        return tooManyLoginsExpectations;
    }

    /********************************************** Tests **********************************************/

    /**
     * Main path back channel logout. Uses the real bcl endpoint, not a test app
     * One login and then end_session/logout
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void BasicBCLTests_mainPath_confidentialClient() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_mainPath", "bcl_mainPath_confClient", false);

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        //        TokenKeeper tokens = new TokenKeeper(webClient, response, updatedTestSettings.getFlowType());
        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        // logout expectations - just make sure we landed on the logout page - always with a good status code
        invokeLogout(webClient, updatedTestSettings, initLogoutExpectations(finalAppWithoutPostRedirect), response);

        // Test uses the standard backchannelLogoutUri - so end_session with bcl steps should be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesValidBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());
        states.setIsUsingIntrospect(true);

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    /**
     * Main path back channel logout. Uses the real bcl endpoint, not a test app
     * One login and then end_session/logout
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void BasicBCLTests_mainPath_publicClient_withSecret() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_mainPath", "bcl_mainPath_publicClient_withSecret", false);

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        // logout expectations - just make sure we landed on the logout page - always with a good status code
        invokeLogout(webClient, updatedTestSettings, initLogoutExpectations(finalAppWithoutPostRedirect), response);

        // Test uses the standard backchannelLogoutUri - so end_session with bcl steps should be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesValidBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    /**
     * Main path back channel logout. Uses the real bcl endpoint, not a test app
     * One login and then end_session/logout
     *
     */
    /**
     * When using mongodb, we need to programmatically register the public client - the registration endpoint will not
     * create a client without a secret - it generates one for us, so we'll need to skip this test in that case.
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipIfUsesMongoDBOrSocialClient.class)
    @Mode(TestMode.LITE)
    @Test
    public void BasicBCLTests_mainPath_publicClient_withoutSecret() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_mainPath", "bcl_mainPath_publicClient_withoutSecret", false);
        updatedTestSettings.setClientSecret(null);

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        // logout expectations - just make sure we landed on the logout page - always with a good status code
        invokeLogout(webClient, updatedTestSettings, initLogoutExpectations(finalAppWithoutPostRedirect), response);

        // Test uses the standard backchannelLogoutUri - so end_session with bcl steps should be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesValidBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    /**
     * Test that we can use an http back channel logout uri with a confidential client - this test should behave the same as the
     * bcl main path test
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void BasicBCLTests_httpBackchannelLogoutUri_confidentialClient_httpsRequired_true() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_http_httpsRequired_true", "bcl_http_confClient_httpsRequired_true", false);

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        // logout expectations - just make sure we landed on the end_session/logout page - always with a good status code
        invokeLogout(webClient, updatedTestSettings, initLogoutExpectations(finalAppWithoutPostRedirect), response);

        // Test uses the standard backchannelLogoutUri - so end_session with bcl steps should be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesValidBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);
    }

    /**
     * Test that we can't use an http back channel uri with a public client - we should receive an exception when the bcl request
     * is made. The end_session/logout should return success, but errors should be logged in the server logs
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipIfUsesMongoDB.class)
    @Mode(TestMode.LITE)
    @AllowedFFDC({ "org.apache.http.NoHttpResponseException", "java.util.concurrent.ExecutionException", "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void BasicBCLTests_httpBackchannelLogoutUri_publicClient_httpsRequired_true_withSecret() throws Exception {

        String client = "bcl_http_publicClient_httpsRequired_true_withSecret";

        testForInvalidPublicClientUsingHttpAtStartup(client);

        // when testing using a localstore, we get a warning message during server start, but, we can attempt to use the client
        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_http_httpsRequired_true", client, false);

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        // logout expectations - just make sure we landed on the end_session/logout page - always with a good status code
        invokeLogout(webClient, updatedTestSettings, initLogoutWithHttpFailureExpectations(finalAppWithoutPostRedirect, client), response);

        // Access and refresh tokens should not be cleaned up since the BCL endpoint is not considered valid
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesInvalidBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());
        if (!currentRepeatAction.contains(Constants.END_SESSION)) {
            // The end_session flow, however, will still clean up the refresh token
            states.setIsRefreshTokenValid(true);
        }

        states.setIsAccessTokenValid(true);

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    /**
     * Test that we can't use an http back channel uri with a public client - we should receive an exception when the bcl request
     * is made. The end_session/logout should return success, but errors should be logged in the server logs
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipIfUsesMongoDBOrSocialClient.class)
    @AllowedFFDC({ "org.apache.http.NoHttpResponseException", "java.util.concurrent.ExecutionException", "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void BasicBCLTests_httpBackchannelLogoutUri_publicClient_httpsRequired_true_withoutSecret() throws Exception {

        String client = "bcl_http_publicClient_httpsRequired_true_withoutSecret";

        testForInvalidPublicClientUsingHttpAtStartup(client);

        // when testing using a localstore, we get a warning message during server start, but, we can attempt to use the client
        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_http_httpsRequired_true", client, false);
        updatedTestSettings.setClientSecret(null);

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        // logout expectations - just make sure we landed on the end_session/logout page - always with a good status code
        invokeLogout(webClient, updatedTestSettings, initLogoutWithHttpFailureExpectations(finalAppWithoutPostRedirect, client), response);

        // Access and refresh tokens should not be cleaned up since the BCL endpoint is not considered valid
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesInvalidBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());
        if (!currentRepeatAction.contains(Constants.END_SESSION)) {
            // The end_session flow, however, will still clean up the refresh token
            states.setIsRefreshTokenValid(true);
        }
        states.setIsAccessTokenValid(true);

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    /**
     * Test that we can use an http back channel logout uri with a confidential client - this test should behave the same as the
     * bcl main path test
     *
     * @throws Exception
     */
    @Test
    public void BasicBCLTests_httpBackchannelLogoutUri_confidentialClient_httpsRequired_false() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_http_httpsRequired_false", "bcl_http_confClient_httpsRequired_false", false);

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        // logout expectations - just make sure we landed on the end_session/logout page - always with a good status code
        invokeLogout(webClient, updatedTestSettings, initLogoutExpectations(finalAppWithoutPostRedirect), response);

        // Test uses the standard backchannelLogoutUri - so end_session with bcl steps should be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesValidBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);
    }

    /**
     * Test that we can't use an http back channel uri with a public client - we should receive an exception when the bcl request
     * is made. The end_session/logout should return success, but errors should be logged in the server logs
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipIfUsesMongoDB.class)
    @AllowedFFDC({ "org.apache.http.NoHttpResponseException", "java.util.concurrent.ExecutionException", "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void BasicBCLTests_httpBackchannelLogoutUri_publicClient_httpsRequired_false_withSecret() throws Exception {

        String client = "bcl_http_publicClient_httpsRequired_false_withSecret";

        testForInvalidPublicClientUsingHttpAtStartup(client);

        // when testing using a localstore, we get a warning message during server start, but, we can attempt to use the client
        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_http_httpsRequired_false", client, false);

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        // logout expectations - just make sure we landed on the end_session/logout page - always with a good status code
        invokeLogout(webClient, updatedTestSettings, initLogoutWithHttpFailureExpectations(finalAppWithoutPostRedirect, client), response);

        // Access and refresh tokens should not be cleaned up since the BCL endpoint is not considered valid
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesInvalidBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());
        if (!currentRepeatAction.contains(Constants.END_SESSION)) {
            // The end_session flow, however, will still clean up the refresh token
            states.setIsRefreshTokenValid(true);
        }

        states.setIsAccessTokenValid(true);

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    /**
     * Test that we can't use an http back channel uri with a public client - we should receive an exception when the bcl request
     * is made. The end_session/logout should return success, but errors should be logged in the server logs
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipIfUsesMongoDBOrSocialClient.class)
    @AllowedFFDC({ "org.apache.http.NoHttpResponseException", "java.util.concurrent.ExecutionException", "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void BasicBCLTests_httpBackchannelLogoutUri_publicClient_httpsRequired_false_withoutSecret() throws Exception {

        String client = "bcl_http_publicClient_httpsRequired_false_withoutSecret";

        testForInvalidPublicClientUsingHttpAtStartup(client);

        // when testing using a localstore, we get a warning message during server start, but, we can attempt to use the client
        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_http_httpsRequired_false", client, false);
        updatedTestSettings.setClientSecret(null);

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        // logout expectations - just make sure we landed on the end_session/logout page - always with a good status code
        invokeLogout(webClient, updatedTestSettings, initLogoutWithHttpFailureExpectations(finalAppWithoutPostRedirect, client), response);

        // Access and refresh tokens should not be cleaned up since the BCL endpoint is not considered valid
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesInvalidBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());
        if (!currentRepeatAction.contains(Constants.END_SESSION)) {
            // The end_session flow, however, will still clean up the refresh token
            states.setIsRefreshTokenValid(true);
        }

        states.setIsAccessTokenValid(true);

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    /**
     * The same user logs in using different clients within the same provider.
     * Invoke end_session passing the id_token as the id_token_hint - show that
     * we'll make a bcl request for each client logged in - the OP will search based on the sub only, but
     * it will build logout_tokens based on the sub and the sid of each login instance.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void BasicBCLTests_confirmBCLUriCalledForEachLogin_withIdTokenHint() throws Exception {

        // the app that this test uses will record a count of how many times its been called, reset the count at the beginning of the test
        resetBCLAppCounter();

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);
        WebClient webClient3 = getAndSaveWebClient(true);
        WebClient webClient4 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_logger1", "loggerClient1-1", false);
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_logger1", "loggerClient1-2", false);
        TestSettings updatedTestSettings3 = updateTestSettingsProviderAndClient("OidcConfigSample_logger1", "loggerClient1-3", false);
        TestSettings updatedTestSettings4 = updateTestSettingsProviderAndClient("OidcConfigSample_logger1", "loggerClient1-4", false);

        Object response1 = accessProtectedApp(webClient1, updatedTestSettings1);
        accessProtectedApp(webClient2, updatedTestSettings2);
        accessProtectedApp(webClient3, updatedTestSettings3);
        accessProtectedApp(webClient4, updatedTestSettings4);
        TokenKeeper keeper1 = setTokenKeeperFromUnprotectedApp(webClient1, updatedTestSettings1, 1);
        TokenKeeper keeper2 = setTokenKeeperFromUnprotectedApp(webClient2, updatedTestSettings2, 1);
        TokenKeeper keeper3 = setTokenKeeperFromUnprotectedApp(webClient3, updatedTestSettings3, 1);
        TokenKeeper keeper4 = setTokenKeeperFromUnprotectedApp(webClient4, updatedTestSettings4, 1);

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithoutPostRedirect);
        logoutExpectations = addDidInvokeBCLExpectation(logoutExpectations, updatedTestSettings1, null);
        logoutExpectations = addDidInvokeBCLExpectation(logoutExpectations, updatedTestSettings2, null);
        logoutExpectations = addDidInvokeBCLExpectation(logoutExpectations, updatedTestSettings3, null);
        logoutExpectations = addDidInvokeBCLExpectation(logoutExpectations, updatedTestSettings4, null);

        // Logout
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, response1);

        // make sure that the backChannelLogoutUri has been called for each client that we logged in (within the provider that we're logging out)
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings1, keeper1.getSessionId());
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings2, keeper2.getSessionId());
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings3, keeper3.getSessionId());
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings4, keeper4.getSessionId());
    }

    /**
     * The same user logs in using different clients within the same provider.
     * Invoke end_session passing the id_token as the id_token_hint - show that
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void BasicBCLTests_confirmBCLUriCalledForEachLogin_withoutIdTokenHint() throws Exception {

        // the app that this test uses will record a count of how many times its been called, reset the count at the beginning of the test
        resetBCLAppCounter();

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);
        WebClient webClient3 = getAndSaveWebClient(true);
        WebClient webClient4 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_logger2", "loggerClient2-1", false);
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_logger2", "loggerClient2-2", false);
        TestSettings updatedTestSettings3 = updateTestSettingsProviderAndClient("OidcConfigSample_logger2", "loggerClient2-3", false);
        TestSettings updatedTestSettings4 = updateTestSettingsProviderAndClient("OidcConfigSample_logger2", "loggerClient2-4", false);

        // login and access protected apps
        accessProtectedApp(webClient1, updatedTestSettings1);
        accessProtectedApp(webClient2, updatedTestSettings2);
        accessProtectedApp(webClient3, updatedTestSettings3);
        accessProtectedApp(webClient4, updatedTestSettings4);

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithoutPostRedirect);
        logoutExpectations = addDidInvokeBCLExpectation(logoutExpectations, updatedTestSettings1, null);
        logoutExpectations = addDidInvokeBCLExpectation(logoutExpectations, updatedTestSettings2, null);
        logoutExpectations = addDidInvokeBCLExpectation(logoutExpectations, updatedTestSettings3, null);
        logoutExpectations = addDidInvokeBCLExpectation(logoutExpectations, updatedTestSettings4, null);

        // Logout - omit the id_token_hint
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, null);

    }

    /**
     * Login using the same oidc/social client with different users.
     * Invoke end_session passing the id_token as the id_token_hint - show that just the instance that matches the user/sub in the
     * id_token passed is logged out.
     *
     * @throws Exception
     */
    @Test
    public void BasicBCLTests_confirmBCLUriCalledForEachUserLogin_withIdTokenHint() throws Exception {

        // the app that this test uses will record a count of how many times its been called, reset the count at the beginning of the test
        resetBCLAppCounter();

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);
        WebClient webClient3 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_logger3", "loggerClient3-1", false);
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient(clientServer, "OidcConfigSample_logger3", "loggerClient3-1", "user1", "security", false);
        TestSettings updatedTestSettings3 = updateTestSettingsProviderAndClient(clientServer, "OidcConfigSample_logger3", "loggerClient3-1", "user2", "security", false);

        Object response1 = accessProtectedApp(webClient1, updatedTestSettings1);
        accessProtectedApp(webClient2, updatedTestSettings2);
        accessProtectedApp(webClient3, updatedTestSettings3);

        TokenKeeper keeper1 = setTokenKeeperFromUnprotectedApp(webClient1, updatedTestSettings1, 1);
        TokenKeeper keeper2 = setTokenKeeperFromUnprotectedApp(webClient2, updatedTestSettings2, 1);
        TokenKeeper keeper3 = setTokenKeeperFromUnprotectedApp(webClient3, updatedTestSettings3, 1);

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithoutPostRedirect);
        logoutExpectations = addDidInvokeBCLExpectation(logoutExpectations, updatedTestSettings1, "1");
        logoutExpectations = addDidNotInvokeBCLExpectation(logoutExpectations, updatedTestSettings2, "2");
        logoutExpectations = addDidNotInvokeBCLExpectation(logoutExpectations, updatedTestSettings3, "3");

        // Logout
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, response1);

        // make sure that the backChannelLogoutUri has been called for each client that we logged in (within the provider that we're logging out)
        //        List<String> sids = new ArrayList<String>(3);
        //        sids.add(validateCorrectBCLUrisCalled(clientServer, updatedTestSettings1, keeper1.getSessionId()));
        //        sids.add(validateCorrectBCLUrisCalled(clientServer, updatedTestSettings2));
        //        sids.add(validateCorrectBCLUrisCalled(clientServer, updatedTestSettings3));
        List<String> sids = validateCorrectBCLUrisCalled_noOrder(clientServer, updatedTestSettings1, true, keeper1.getSessionId());

        Log.info(thisClass, _testName, "Logged out sids: " + Arrays.toString(sids.toArray()));
        if (!sids.contains(keeper1.getSessionId())) {
            fail("If appears that " + updatedTestSettings1.getClientID() + " was not logged out properly - sid: " + keeper1.getSessionId());
        }
        if (sids.contains(keeper2.getSessionId())) {
            fail("If appears that " + updatedTestSettings2.getClientID() + " was logged out and should not have been - sid: " + keeper2.getSessionId());
        }
        if (sids.contains(keeper3.getSessionId())) {
            fail("If appears that " + updatedTestSettings3.getClientID() + " was logged out and should not have been - sid: " + keeper3.getSessionId());
        }
    }

    /**
     * Login using the same oidc/social client with different users.
     * Invoke end_session and do NOT pas the id_token as the id_token_hint - use the webClient instance to log out.
     * Since the users are different, only the instance matching the sub will be logged out.
     *
     * @throws Exception
     */
    @Test
    public void BasicBCLTests_confirmBCLUriCalledForEachUserLogin_withoutIdTokenHint() throws Exception {

        // the app that this test uses will record a count of how many times its been called, reset the count at the beginning of the test
        resetBCLAppCounter();

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);
        WebClient webClient3 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_logger4", "loggerClient4-1", false);
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient(clientServer, "OidcConfigSample_logger4", "loggerClient4-1", "user1", "security", false);
        TestSettings updatedTestSettings3 = updateTestSettingsProviderAndClient(clientServer, "OidcConfigSample_logger4", "loggerClient4-1", "user2", "security", false);

        // login and access protected apps and save cookies/tokens
        accessProtectedApp(webClient1, updatedTestSettings1);
        accessProtectedApp(webClient2, updatedTestSettings2);
        accessProtectedApp(webClient3, updatedTestSettings3);

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithoutPostRedirect);
        logoutExpectations = addDidInvokeBCLExpectation(logoutExpectations, updatedTestSettings1, "1");
        logoutExpectations = addDidNotInvokeBCLExpectation(logoutExpectations, updatedTestSettings2, "2");
        logoutExpectations = addDidNotInvokeBCLExpectation(logoutExpectations, updatedTestSettings3, "3");

        // Logout - omit the id_token_hint
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, null);

    }

    /**
     * Test that the bcl for a client is only called once for each instance. We have a provider with 2 clients. Log in using each
     * one and then run end_session for that provider.
     * We should see 2 calls to the test app configured as the bcl endpoint. Reset the counter in the test app.
     * Log in using each client again and run end_session once more. We should only see bcl run once for each of the new accesses.
     * Validate that the count that the test has is 2 (not 4).
     *
     * @throws Exception
     */
    @Test
    public void BasicBCLTests_noDuplicateLogoutRequests_withIdTokenHint() throws Exception {

        // the app that this test uses will record a count of how many times its been called, reset the count at the beginning of the test
        resetBCLAppCounter();

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);
        WebClient webClient3 = getAndSaveWebClient(true);
        WebClient webClient4 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_checkDuplicateBCLCalls", "checkDupBcl_client1", false);
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_checkDuplicateBCLCalls", "checkDupBcl_client2", false);

        Object response1 = accessProtectedApp(webClient1, updatedTestSettings1);
        accessProtectedApp(webClient2, updatedTestSettings2);

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithoutPostRedirect);
        logoutExpectations = addDidInvokeBCLExpectation(logoutExpectations, updatedTestSettings1, null);
        logoutExpectations = addDidInvokeBCLExpectation(logoutExpectations, updatedTestSettings2, null);

        // Logout
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, response1);

        checkLogMsgCount(2);
        // reset the counter now that we've completed the end_session - we want the total count after the second end_session
        resetBCLAppCounter();

        Object response2 = accessProtectedApp(webClient3, updatedTestSettings1);
        accessProtectedApp(webClient4, updatedTestSettings2);

        // Logout
        invokeLogout(webClient3, updatedTestSettings1, logoutExpectations, response2);

        checkLogMsgCount(2); // ( should not be 4 (or anything above 2 which is how many logins we've done since the last end_session/logout)

    }

    /**
     * Test that bcl endpoints are not invoked even when the id_token is passed on the logout - the id_tokens are not known since
     * they're not cached
     *
     * @throws Exception
     */
    @Test
    public void BasicBCLTests_idTokenCacheEnabled_false_withIdTokenHint() throws Exception {

        // the app that this test uses will record a count of how many times its been called, reset the count at the beginning of the test
        resetBCLAppCounter();

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);
        WebClient webClient3 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_idTokenCacheEnabledFalse", "idTokenCacheEnabledFalseClient-1", false);
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_idTokenCacheEnabledFalse", "idTokenCacheEnabledFalseClient-2", false);
        TestSettings updatedTestSettings3 = updateTestSettingsProviderAndClient("OidcConfigSample_idTokenCacheEnabledFalse", "idTokenCacheEnabledFalseClient-3", false);

        Object response1 = accessProtectedApp(webClient1, updatedTestSettings1);
        accessProtectedApp(webClient2, updatedTestSettings2);
        accessProtectedApp(webClient3, updatedTestSettings3);

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithoutPostRedirect);
        logoutExpectations = addDidNotInvokeBCLExpectation(logoutExpectations, updatedTestSettings1, "1");
        logoutExpectations = addDidNotInvokeBCLExpectation(logoutExpectations, updatedTestSettings2, "2");
        logoutExpectations = addDidNotInvokeBCLExpectation(logoutExpectations, updatedTestSettings3, "3");

        // Logout
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, response1);

    }

    /**
     * Test that bcl endpoints are not invoked - the id_tokens are not known since they're not cached
     *
     * @throws Exception
     */
    @Test
    public void BasicBCLTests_idTokenCacheEnabled_false_withoutIdTokenHint() throws Exception {

        // the app that this test uses will record a count of how many times its been called, reset the count at the beginning of the test
        resetBCLAppCounter();

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);
        WebClient webClient3 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_idTokenCacheEnabledFalse", "idTokenCacheEnabledFalseClient-1", false);
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_idTokenCacheEnabledFalse", "idTokenCacheEnabledFalseClient-2", false);
        TestSettings updatedTestSettings3 = updateTestSettingsProviderAndClient("OidcConfigSample_idTokenCacheEnabledFalse", "idTokenCacheEnabledFalseClient-3", false);

        // login and access protected apps
        accessProtectedApp(webClient1, updatedTestSettings1);
        accessProtectedApp(webClient2, updatedTestSettings2);
        accessProtectedApp(webClient3, updatedTestSettings3);

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithoutPostRedirect);
        logoutExpectations = addDidNotInvokeBCLExpectation(logoutExpectations, updatedTestSettings1, "1");
        logoutExpectations = addDidNotInvokeBCLExpectation(logoutExpectations, updatedTestSettings2, "2");
        logoutExpectations = addDidNotInvokeBCLExpectation(logoutExpectations, updatedTestSettings3, "3");

        // Logout - omit the id_token_hint
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, null);

    }

    /**
     * Test that bcl endpoints are invoked - not caching the access_token should make no difference
     *
     * @throws Exception
     */
    @Test
    public void BasicBCLTests_accessTokenCacheEnabled_false_withIdTokenHint() throws Exception {

        // the app that this test uses will record a count of how many times its been called, reset the count at the beginning of the test
        resetBCLAppCounter();

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);
        WebClient webClient3 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_accessTokenCacheEnabledFalse", "accessTokenCacheEnabledFalseClient-1", false);
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_accessTokenCacheEnabledFalse", "accessTokenCacheEnabledFalseClient-2", false);
        TestSettings updatedTestSettings3 = updateTestSettingsProviderAndClient("OidcConfigSample_accessTokenCacheEnabledFalse", "accessTokenCacheEnabledFalseClient-3", false);

        Object response1 = accessProtectedApp(webClient1, updatedTestSettings1);
        accessProtectedApp(webClient2, updatedTestSettings2);
        accessProtectedApp(webClient3, updatedTestSettings3);

        TokenKeeper keeper1 = setTokenKeeperFromUnprotectedApp(webClient1, updatedTestSettings1, 1);
        TokenKeeper keeper2 = setTokenKeeperFromUnprotectedApp(webClient2, updatedTestSettings2, 1);
        TokenKeeper keeper3 = setTokenKeeperFromUnprotectedApp(webClient3, updatedTestSettings3, 1);

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithoutPostRedirect);
        logoutExpectations = addDidInvokeBCLExpectation(logoutExpectations, updatedTestSettings1, null);
        logoutExpectations = addDidInvokeBCLExpectation(logoutExpectations, updatedTestSettings2, null);
        logoutExpectations = addDidInvokeBCLExpectation(logoutExpectations, updatedTestSettings3, null);

        // Logout
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, response1);

        // make sure that the backChannelLogoutUri has been called for each client that we logged in (within the provider that we're logging out)
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings1, keeper1.getSessionId());
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings2, keeper2.getSessionId());
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings3, keeper3.getSessionId());
    }

    /**
     * Test that bcl endpoints are invoked - caching the access_token should make no difference
     *
     * @throws Exception
     */
    @Test
    public void BasicBCLTests_accessTokenCacheEnabled_false_withoutIdTokenHint() throws Exception {

        // the app that this test uses will record a count of how many times its been called, reset the count at the beginning of the test
        resetBCLAppCounter();

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);
        WebClient webClient3 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_accessTokenCacheEnabledFalse", "accessTokenCacheEnabledFalseClient-1", false);
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_accessTokenCacheEnabledFalse", "accessTokenCacheEnabledFalseClient-2", false);
        TestSettings updatedTestSettings3 = updateTestSettingsProviderAndClient("OidcConfigSample_accessTokenCacheEnabledFalse", "accessTokenCacheEnabledFalseClient-3", false);

        // login and access protected apps
        accessProtectedApp(webClient1, updatedTestSettings1);
        accessProtectedApp(webClient2, updatedTestSettings2);
        accessProtectedApp(webClient3, updatedTestSettings3);

        TokenKeeper keeper1 = setTokenKeeperFromUnprotectedApp(webClient1, updatedTestSettings1, 1);
        TokenKeeper keeper2 = setTokenKeeperFromUnprotectedApp(webClient2, updatedTestSettings2, 1);
        TokenKeeper keeper3 = setTokenKeeperFromUnprotectedApp(webClient3, updatedTestSettings3, 1);

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithoutPostRedirect);
        logoutExpectations = addDidInvokeBCLExpectation(logoutExpectations, updatedTestSettings1, null);
        logoutExpectations = addDidInvokeBCLExpectation(logoutExpectations, updatedTestSettings2, null);
        logoutExpectations = addDidInvokeBCLExpectation(logoutExpectations, updatedTestSettings3, null);

        // Logout
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, null);

        // make sure that the backChannelLogoutUri has been called for each client that we logged in (within the provider that we're logging out)
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings1, keeper1.getSessionId());
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings2, keeper2.getSessionId());
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings3, keeper3.getSessionId());

    }

    /**
     * Show that a back channel logout that takes a bit longer than usual will not timeout (as the delay is still less than the
     * back channel logout time allowed.
     *
     */
    @Test
    public void BasicBCLTests_defaultBCLTimeout() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_defaultBCLTimeout", "bcl_defaultBCLTimeout");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithPostRedirect);

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);

        // Test has a test app configured for the backchannelLogoutUri - so just he normal end_session steps will be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesFakeBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    /**
     * Show that multiple logins and then a back channel logout that takes a bit longer than usual will not timeout (as the delay
     * is still less than the back channel logout time allowed.
     */
    @Test
    public void BasicBCLTests_defaultBCLTimeout_multipleLoginsWithinOP() throws Exception {

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_defaultBCLTimeout", "bcl_defaultBCLTimeout");
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_defaultBCLTimeout", "bcl_otherDefaultBCLTimeout");

        Object response = accessProtectedApp(webClient1, updatedTestSettings1);
        TokenKeeper tokens1 = setTokenKeeperFromUnprotectedApp(webClient1, updatedTestSettings1, 1);

        response = accessProtectedApp(webClient2, updatedTestSettings2);
        TokenKeeper tokens2 = setTokenKeeperFromUnprotectedApp(webClient2, updatedTestSettings2, 1);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithPostRedirect);

        invokeLogout(webClient2, updatedTestSettings2, logoutExpectations, response);

        // Test has a test app configured for the backchannelLogoutUri - so just he normal end_session steps will be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesFakeBCLEndpoint, updatedTestSettings2.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings2.getRsTokenType(), isUsingSaml());

        validateLogoutResult(webClient2, updatedTestSettings2, tokens2, states);

        AfterLogoutStates otherStates = new AfterLogoutStates(Constants.usesFakeBCLEndpoint, updatedTestSettings1.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings1.getRsTokenType(), isUsingSaml());
        // now, since we didn't do the logout using webClient1, there are some things that will still exist (that had been cleaned up for webClient2)
        otherStates.setOPNoCookiesRemoved();
        otherStates.setClientNoCookiesRemoved();
        otherStates.setIsAppSessionAccess(true);

        validateLogoutResult(webClient1, updatedTestSettings1, tokens1, otherStates);

    }

    /**
     * Show that a back channel logout that takes a longer than the config allows will return with a 200 status code and that the
     * normal end_session OP cleanup will be done
     *
     */
    @AllowedFFDC({ "java.util.concurrent.CancellationException" })
    @Test
    public void BasicBCLTests_shortBCLTimeout() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_shortBCLTimeout", "bcl_shortBCLTimeout");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithPostRedirect);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1649E_BACK_CHANNEL_LOGOUT_TIMEOUT);

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        // Test has a test app configured for the backchannelLogoutUri - so just he normal end_session steps will be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesFakeBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());

        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    /**
     * Configure an invalid back channel url - show that we'll perform all of the other cleanup that would be done when bcl is
     * enabled - the failure that occurs when the invalid url is invoked will not result in a bad status code being returned to
     * the caller.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void BasicBCLTests_invalidBackchannelLogoutUri() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_invalidBCLUri");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithPostRedirect);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI);

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        // Test has a test app configured for the backchannelLogoutUri - so just he normal end_session steps will be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesInvalidBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());

        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    /**
     * Configure a client without a backchannelLogoutUri - show that with the id_token included in the request that the
     * access_token and refresh_token are still cleaned up when end_session is used, but that the refresh_token is NOT cleaned up
     * when logout is used
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void BasicBCLTests_omittedBackchannelLogoutUri_withIdTokenHint() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_omittedBCLUri");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithPostRedirect);

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        // Test has a test app configured for the backchannelLogoutUri - so just he normal end_session steps will be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesInvalidBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());
        states.setOPAllCookiesRemoved(); // after logout, this test expects all OP cookies to be removed

        if (logoutMethodTested.equals(Constants.LOGOUT_ENDPOINT) || (logoutMethodTested.equals(Constants.HTTP_SESSION) && sessionLogoutEndpoint.equals(Constants.LOGOUT_ENDPOINT)) || logoutMethodTested.equals(Constants.SAML)) {
            states.setIsRefreshTokenValid(true); // when we're using logout from from the OP or SAML and we do NOT have a bcl coded, we won't clean up the refresh_token
        }
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    /**
     * Configure a client without a backchannelLogoutUri - show that without the id_token included in the request that only
     * the access_token is cleaned up - the refresh_token will still be valid
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidTokenException" })
    @Test
    public void BasicBCLTests_omittedBackchannelLogoutUri_withoutIdTokenHint() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_omittedBCLUri");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        List<validationData> logoutExpectations = null;
        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        if (logoutMethodTested.equals(Constants.END_SESSION)) {
            logoutExpectations = initLogoutExpectations(finalAppWithoutPostRedirect);
        } else {
            logoutExpectations = initLogoutExpectations(finalAppWithPostRedirect);
        }

        // don't pass the response from login - that will prevent the id_token from being sent on the logout
        testOPServer.addIgnoredServerException(MessageConstants.CWWKS1636E_POST_LOGOUT_REDIRECT_MISMATCH);
        invokeLogout(webClient, updatedTestSettings, logoutExpectations, null);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        // Test has a test app configured for the backchannelLogoutUri - so just he normal end_session steps will be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesInvalidBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());
        states.setOPAllCookiesRemoved(); // after logout, this test expects all OP cookies to be removed

        states.setIsRefreshTokenValid(true);

        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    /**
     * Show that the caller will receive a 200 status code when the bcl uri returns a 400
     *
     * @throws Exception
     */
    @Test
    public void BasicBCLTests_backchannelLogoutUri_returns400() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns400");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithPostRedirect);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_400_Servlet.*");

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        // Test has a test app configured for the backchannelLogoutUri - so just he normal end_session steps will be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesFakeBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());
        states.setOPAllCookiesRemoved(); // after logout, this test expects all OP cookies to be removed

        states.setAllTokensCleanedUp(); // after logout, this test expects access and refresh tokens to be invalid
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    /**
     * Show that the caller will receive a 200 status code when the bcl uri returns a 501
     *
     * @throws Exception
     */
    @Test
    public void BasicBCLTests_backchannelLogoutUri_returns501() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns501");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithPostRedirect);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_501_Servlet.*");

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        // Test has a test app configured for the backchannelLogoutUri - so just he normal end_session steps will be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesFakeBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());
        states.setOPAllCookiesRemoved(); // after logout, this test expects all OP cookies to be removed
        //        states.setClientCookieRemovalBasedOnLogoutType();
        states.setAllTokensCleanedUp(); // after logout, this test expects access and refresh tokens to be invalid
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    /**
     * Show that the caller will receive a 200 status code when the bcl uris invoked for multiple clients return non-200 status
     * codes
     *
     * @throws Exception
     */
    @Test
    public void BasicBCLTests_backchannelLogoutUri_returnsMultipleDifferentFailures() throws Exception {

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns400");
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns501");

        Object response1 = accessProtectedApp(webClient1, updatedTestSettings1);
        Object response2 = accessProtectedApp(webClient2, updatedTestSettings2);

        TokenKeeper tokens1 = setTokenKeeperFromUnprotectedApp(webClient1, updatedTestSettings1, 1);

        TokenKeeper tokens2 = setTokenKeeperFromUnprotectedApp(webClient2, updatedTestSettings2, 1);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithPostRedirect);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_400_Servlet.*");
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_501_Servlet.*");

        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, response1);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps (doesn't actually log anything out)
        // Test has a test app configured for the backchannelLogoutUri - so just he normal end_session steps will be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesFakeBCLEndpoint, updatedTestSettings1.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings1.getRsTokenType(), isUsingSaml());
        states.setOPAllCookiesRemoved(); // after logout, this test expects all OP cookies to be removed

        states.setAllTokensCleanedUp(); // after logout, this test expects access and refresh tokens to be invalid
        validateLogoutResult(webClient1, updatedTestSettings1, tokens1, states);

        AfterLogoutStates otherStates = new AfterLogoutStates(Constants.usesFakeBCLEndpoint, updatedTestSettings1.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings1.getRsTokenType(), isUsingSaml());
        // now, since we didn't do the logout using webClient1, there are some things that will still exist (that had been cleaned up for webClient2)
        otherStates.setOPNoCookiesRemoved();
        otherStates.setClientNoCookiesRemoved();
        otherStates.setIsAppSessionAccess(true);

        validateLogoutResult(webClient2, updatedTestSettings2, tokens2, otherStates);

    }

    /**
     * Show that the caller will receive a 200 status code when the bcl uris invoked for multiple clients return 400 status
     * codes
     *
     * @throws Exception
     */
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
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithPostRedirect);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_400_Servlet - 1.*");
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_400_Servlet - 2.*");
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_400_Servlet - 3.*");

        // Logout
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, response);

    }

    /**
     * Make sure that back channel logout is not used for clients that do not have it configured
     *
     * @throws Exception
     */
    @Test
    public void BasicBCLTests_noBCLInvocationForClientWithoutBCLConfigured() throws Exception {

        // the app that this test uses will record a count of how many times its been called, reset the count at the beginning of the test
        resetBCLAppCounter();

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);
        WebClient webClient3 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_multiClientWithAndWithoutBCL", "bcl_client1", false);
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_multiClientWithAndWithoutBCL", "nobcl_client1", false);
        TestSettings updatedTestSettings3 = updateTestSettingsProviderAndClient("OidcConfigSample_multiClientWithAndWithoutBCL", "bcl_client2", false);

        Object response1 = accessProtectedApp(webClient1, updatedTestSettings1);
        accessProtectedApp(webClient2, updatedTestSettings2);
        accessProtectedApp(webClient3, updatedTestSettings3);
        TokenKeeper keeper1 = setTokenKeeperFromUnprotectedApp(webClient1, updatedTestSettings1, 1);
        TokenKeeper keeper2 = setTokenKeeperFromUnprotectedApp(webClient2, updatedTestSettings2, 1);
        TokenKeeper keeper3 = setTokenKeeperFromUnprotectedApp(webClient3, updatedTestSettings3, 1);

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithoutPostRedirect);
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a bcl request was made for client \"bcl_client1\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings1.getClientID() + "*");
        logoutExpectations = addDidNotInvokeBCLExpectation(logoutExpectations, updatedTestSettings2, null);
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a bcl request was made for client \"bcl_client2\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings3.getClientID() + "*");

        // Logout
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, response1);

        // make sure that the backChannelLogoutUri has been called for each client that we logged in (within the provider that we're logging out)
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings1, keeper1.getSessionId());
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings2, false, keeper2.getSessionId());
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings3, keeper3.getSessionId());
    }

    /**
     * Try to use a logout_token instead of an access_token for access to a protected app - make sure that access is denied and we
     * have to log in again.
     * NOTE: Social client oidcLogin does not allow token propagation, so, we'll skip this test in that case.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipIfSocialClient.class)
    @AllowedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtException" }) // expected in cases where the test runs, have to use AllowedFFDC for instances that we skip the test
    @Test
    public void BasicBCLTests_tryToAccessProtectedAppUsingLogoutTokenAsAccessToken() throws Exception {

        restoreAppMap("useLogoutTokenForAccess"); // reset test bcl app
        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_useLogoutTokenForAccess", "useLogoutTokenForAccess_introspect", false);

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithoutPostRedirect);

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);
        String logoutToken = getLogoutToken("useLogoutTokenForAccess"); // get the logout_token that the test bcl app will return for this test's client

        String action = "TRY_TO_USE_LOGOUT_TOKEN_AS_ACCESS_TOKEN";

        // error messages for when we try to use the logout_token with a client that validates with introspect
        List<validationData> expectations = setUseLogoutTokenAsAccessTokenExpectations(action, updatedTestSettings, Constants.INTROSPECTION_ENDPOINT);

        //        helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), logoutToken, Constants.HEADER, updatedTestSettings, expectations, action);
        accessProtectedAppViaPropagation(logoutToken, updatedTestSettings, expectations, action);

        // error messages are slightly different when we try to use the logout_token with a client that validates with userinfo
        updatedTestSettings.setProtectedResource(clientServer.getHttpsString() + "/formlogin/simple/" + "useLogoutTokenForAccess_userinfo");
        expectations = setUseLogoutTokenAsAccessTokenExpectations(action, updatedTestSettings, Constants.USERINFO_ENDPOINT);

        //        helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), logoutToken, Constants.HEADER, updatedTestSettings, expectations, action);
        accessProtectedAppViaPropagation(logoutToken, updatedTestSettings, expectations, action);

    }

    /**
     * Invoke logout with the client using introspection, but the introspect endpoint is invalid - make sure the appropriate
     * message is logged, but that
     * the runtime then uses userinfo to validate the token.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipIfSocialClient.class) // social client can't specify an introspect endpoint
    public void BasicBCLTests_invalidIntrospectEndpoint() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_mainPath", "variableIntrospectValidationEndpoint", false);

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        // reconfigure the introspect endpoint to something invalid
        Map<String, String> vars = new HashMap<String, String>();
        vars.put("variableValidationEndpoint", clientServer.getHttpsString() + "/oidc/endpoint/OidcConfigSample_mainPath/not_introspect");
        updateServerSettings(clientServer, vars);
        // save the "invalid" introspect endpoint for use in the check for the proper error message later
        updatedTestSettings.setIntrospectionEndpt(vars.get("variableValidationEndpoint"));

        // logout expectations - just make sure we landed on the logout page - always with a good status code
        invokeLogout(webClient, updatedTestSettings, initLogoutExpectations(finalAppWithoutPostRedirect), response);

        // Test uses the standard backchannelLogoutUri - so end_session with bcl steps should be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesValidBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());
        states.setIsUsingIntrospect(true);
        states.setIsUsingInvalidIntrospect(true);

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    /**
     * invoke end_session on a provider that didn't issue the id_token - make sure failure about the wrong issuer is issued
     **/
    // TODO - need fix for 25526 - will probably restore test to what I had originally
    @Mode(TestMode.LITE)
    //    @Test
    public void BasicBCLTests_invokeLogoutOfDifferentProvider() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_mainPath", "bcl_mainPath_confClient", false);

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        // update the end_session endpoint so we try to do the logout with another provider
        updatedTestSettings.setEndSession(updatedTestSettings.getEndSession().replace("OidcConfigSample_mainPath", "OidcConfigSample_http_httpsRequired_true"));

        List<validationData> expectations = vData.addSuccessStatusCodes();

        String logoutStep = ((loginMethod.equals(Constants.SAML) ? Constants.PROCESS_LOGOUT_PROPAGATE_YES : Constants.LOGOUT));
        expectations = vData.addResponseStatusExpectation(expectations, logoutStep, Constants.OK_STATUS);
        if (!(sessionLogoutEndpoint != null && sessionLogoutEndpoint.equals(Constants.LOGOUT_ENDPOINT) || logoutMethodTested.equals(Constants.LOGOUT_ENDPOINT))) {
            expectations = vData.addExpectation(expectations, logoutStep, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not successfully logout.", null, Constants.UNSUCCESSFUL_LOGOUT_MSG);
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, logoutStep, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the provider failed to validate the id_token.", MessageConstants.CWWKS1625E_FAILED_TO_VALIDATE_ID_TOKEN);
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, logoutStep, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that the issuer did not match.", MessageConstants.CWWKS1646E_BACK_CHANNEL_LOGOUT_ISSUER_MISMATCH);
        } else {
            expectations = initLogoutExpectations(finalAppWithoutPostRedirect);
        }
        // logout expectations - just make sure we landed on the logout page - always with a good status code
        invokeLogout(webClient, updatedTestSettings, expectations, response);

        // Test uses the standard backchannelLogoutUri - so end_session with bcl steps should be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesInvalidBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());
        states.setIsAccessTokenValid(true);
        states.setIsRefreshTokenValid(true);
        if (!(sessionLogoutEndpoint != null && sessionLogoutEndpoint.equals(Constants.LOGOUT_ENDPOINT) || logoutMethodTested.equals(Constants.LOGOUT_ENDPOINT))) {
            states.setOPNoCookiesRemoved();
        }
        states.setClientNoCookiesRemoved();
        states.setIsAppSessionAccess(true);

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20AccessDeniedException" })
    @Mode(TestMode.LITE)
    @Test
    public void BasicBCLTests_userClientTokenLimit() throws Exception {

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);
        WebClient webClient3 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_userClientTokenLimit", "bcl_userClientTokenLimit_Client", false);

        Object response = accessProtectedApp(webClient1, updatedTestSettings);
        accessProtectedApp(webClient2, null, updatedTestSettings, setTooManyLoginsExpectations());

        TokenKeeper tokens1 = setTokenKeeperFromUnprotectedApp(webClient1, updatedTestSettings, 1);

        // logout expectations - just make sure we landed on the logout page - always with a good status code
        invokeLogout(webClient1, updatedTestSettings, initLogoutExpectations(finalAppWithoutPostRedirect), response);

        // Test uses the standard backchannelLogoutUri - so end_session with bcl steps should be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesValidBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient1, updatedTestSettings, tokens1, states);

        // After logging out make sure that the cache has been cleaned out and we can log in again.
        accessProtectedApp(webClient3, updatedTestSettings);

    }

    /**
     * Create app_passwords and from those, create access_tokens
     * Use BCL Logout
     * After the logout, validate that all of the appropriate cookies and tokens were cleaned up
     * Also validate that the access_tokens created from the app_passwords are still valid (and can be used to access a protected
     * app)
     * Then validate that the app_passwords are still valid and can be used to create new access_token and that those tokens can
     * be used to access a protected app
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidTokenException" })
    @Mode(TestMode.LITE)
    @Test
    public void BasicBCLTests_appPasswords() throws Exception {

        testOPServer.addIgnoredServerExceptions(MessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID, MessageConstants.CWOAU0029E_TOKEN_NOT_IN_CACHE);
        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_appPasswords", "bcl_appPasswordsClient1", false);
        updatedTestSettings.setAppPasswordEndpt(updatedTestSettings.getAppPasswordsEndpt().replace("OidcConfigSample", "OidcConfigSample_appPasswords"));

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        List<TokenValues> tokenValues = appPTcommon.createAndValidateAppPasswordsFromSameAccessToken(updatedTestSettings, tokens.getAccessToken(), _testName, "bcl_appPasswordsClient1", AppPasswordsAndTokensCommonTest.unknownLifetime, 3);
        List<validationData> accessTokenExpectations = vData.addSuccessStatusCodes();

        List<String> createdAccessTokens = new ArrayList<>();
        // Make sure that the app passwords are good
        for (TokenValues tv : tokenValues) {
            String newAccessToken = appPTcommon.getAccessTokenFromAppPassword(updatedTestSettings, tv.getApp_password(), true, appPTcommon.getGoodTokenEndpointExpectations(updatedTestSettings));
            //            helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), newAccessToken, Constants.HEADER, updatedTestSettings, accessTokenExpectations, Constants.INVOKE_PROTECTED_RESOURCE);
            accessProtectedAppViaPropagation(newAccessToken, updatedTestSettings, accessTokenExpectations, Constants.INVOKE_PROTECTED_RESOURCE);
            createdAccessTokens.add(newAccessToken);

        }
        // logout expectations - just make sure we landed on the logout page - always with a good status code
        invokeLogout(webClient, updatedTestSettings, initLogoutExpectations(finalAppWithoutPostRedirect), response);

        // Test uses the standard backchannelLogoutUri - so end_session with bcl steps should be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesValidBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());

        states.setIsUsingIntrospect(true);

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

        // After logging out make sure that the cache has been cleaned out and we can log in again.
        accessProtectedApp(webClient, updatedTestSettings);

        Log.info(thisClass, _testName, "************************************************************ BEFORE reusing access_tokens from app_passwords ***************************************************************");
        // Now, let's see if the access_tokens created from the app_passwords are still valid after logout
        for (String accessToken : createdAccessTokens) {
            //            helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), accessToken, Constants.HEADER, updatedTestSettings, accessTokenExpectations, Constants.INVOKE_PROTECTED_RESOURCE);
            accessProtectedAppViaPropagation(accessToken, updatedTestSettings, accessTokenExpectations, Constants.INVOKE_PROTECTED_RESOURCE);

        }

        Log.info(thisClass, _testName, "************************************************************ BEFORE creating additional access_tokens from app_passwords ***************************************************************");
        // Finally, let's see if app_passwords can generate new access_tokens
        //        List<validationData> invalidAppPasswordExpectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_TOKEN_ENDPOINT);
        //        invalidAppPasswordExpectations = vData.addResponseStatusExpectation(invalidAppPasswordExpectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.UNAUTHORIZED_STATUS);
        //        invalidAppPasswordExpectations = vData.addExpectation(invalidAppPasswordExpectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "The response did not contain a  message indicating that an access_token could not be created from the app_password.", null, MessageConstants.CWOAU0074E_CANNOT_COMPLETE_PASSWORD_EXCHANGE);
        //        invalidAppPasswordExpectations = validationTools.addMessageExpectation(testOPServer, invalidAppPasswordExpectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain a message indicating that an access_token could not be created from the app_password.", MessageConstants.CWOAU0074E_CANNOT_COMPLETE_PASSWORD_EXCHANGE);

        for (TokenValues tv : tokenValues) {
            String newAccessToken = appPTcommon.getAccessTokenFromAppPassword(updatedTestSettings, tv.getApp_password(), true, appPTcommon.getGoodTokenEndpointExpectations(updatedTestSettings));
            //            String newAccessToken =
            //            appPTcommon.getAccessTokenFromAppPassword(updatedTestSettings, tv.getApp_password(), false, invalidAppPasswordExpectations);
            //            helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), newAccessToken, Constants.HEADER, updatedTestSettings, accessTokenExpectations, Constants.INVOKE_PROTECTED_RESOURCE);
            accessProtectedAppViaPropagation(newAccessToken, updatedTestSettings, accessTokenExpectations, Constants.INVOKE_PROTECTED_RESOURCE);

        }

    }

    @Mode(TestMode.LITE)
    @Test
    public void BasicBCLTests_appTokens() throws Exception {

        testOPServer.addIgnoredServerExceptions(MessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID, MessageConstants.CWOAU0029E_TOKEN_NOT_IN_CACHE);
        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_appTokens", "bcl_appTokensClient1", false);
        updatedTestSettings.setAppTokenEndpt(updatedTestSettings.getAppTokensEndpt().replace("OidcConfigSample", "OidcConfigSample_appTokens"));

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = setTokenKeeperFromUnprotectedApp(webClient, updatedTestSettings, 1);

        List<TokenValues> tokenValues = appPTcommon.createAndValidateAppTokensFromSameAccessToken(updatedTestSettings, tokens.getAccessToken(), _testName, "bcl_appTokensClient1", AppPasswordsAndTokensCommonTest.unknownLifetime, 3);

        List<validationData> appTokenExpectations = vData.addSuccessStatusCodes();
        //        appTokenExpectations = vData.addExpectation(appTokenExpectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, updatedTestSettings.getTestURL());

        // Make sure that the app tokens are good
        for (TokenValues tv : tokenValues) {
            //            helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), tv.getApp_token(), Constants.HEADER, updatedTestSettings, appTokenExpectations, Constants.INVOKE_PROTECTED_RESOURCE);
            accessProtectedAppViaPropagation(tv.getApp_token(), updatedTestSettings, appTokenExpectations, Constants.INVOKE_PROTECTED_RESOURCE);

        }
        // logout expectations - just make sure we landed on the logout page - always with a good status code
        invokeLogout(webClient, updatedTestSettings, initLogoutExpectations(finalAppWithoutPostRedirect), response);

        // Test uses the standard backchannelLogoutUri - so end_session with bcl steps should be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesValidBCLEndpoint, updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType(), isUsingSaml());
        states.setIsUsingIntrospect(true);

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

        // After logging out make sure that the cache has been cleaned out and we can log in again.
        accessProtectedApp(webClient, updatedTestSettings);

        Log.info(thisClass, _testName, "************************************************************ BEFORE validating the app_Tokens ***************************************************************");
        // Finally, let's see if app_Tokens are still valid
        for (TokenValues tv : tokenValues) {
            //            helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), tv.getApp_token(), Constants.HEADER, updatedTestSettings, appTokenExpectations, Constants.INVOKE_PROTECTED_RESOURCE);
            accessProtectedAppViaPropagation(tv.getApp_token(), updatedTestSettings, appTokenExpectations, Constants.INVOKE_PROTECTED_RESOURCE);

        }

    }

    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidTokenException" })
    @Mode(TestMode.LITE)
    @Test
    public void BasicBCLTests_appPasswords_additionalClient() throws Exception {

        testOPServer.addIgnoredServerExceptions(MessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID, MessageConstants.CWOAU0029E_TOKEN_NOT_IN_CACHE);
        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_appPasswords", "bcl_appPasswordsClient1", false);
        updatedTestSettings1.setAppPasswordEndpt(updatedTestSettings1.getAppPasswordsEndpt().replace("OidcConfigSample", "OidcConfigSample_appPasswords"));

        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_appPasswords", "bcl_appPasswordsClient2", false);
        updatedTestSettings2.setAppPasswordEndpt(updatedTestSettings2.getAppPasswordsEndpt().replace("OidcConfigSample", "OidcConfigSample_appPasswords"));

        Object response1 = accessProtectedApp(webClient1, updatedTestSettings1);
        Object response2 = accessProtectedApp(webClient2, updatedTestSettings2);

        TokenKeeper tokens1 = setTokenKeeperFromUnprotectedApp(webClient1, updatedTestSettings1, 1);
        TokenKeeper tokens2 = setTokenKeeperFromUnprotectedApp(webClient2, updatedTestSettings2, 1);

        // create app_passwords for both client1 and client2
        List<TokenValues> tokenValues1 = appPTcommon.createAndValidateAppPasswordsFromSameAccessToken(updatedTestSettings1, tokens1.getAccessToken(), _testName, "bcl_appPasswordsClient1", AppPasswordsAndTokensCommonTest.unknownLifetime, 3);
        List<TokenValues> tokenValues2a = appPTcommon.createAndValidateAppPasswordsFromSameAccessToken(updatedTestSettings2, tokens2.getAccessToken(), _testName, "bcl_appPasswordsClient2", AppPasswordsAndTokensCommonTest.unknownLifetime, 3);

        List<validationData> accessTokenExpectations1 = vData.addSuccessStatusCodes();
        //        accessTokenExpectations1 = vData.addExpectation(accessTokenExpectations1, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, updatedTestSettings1.getTestURL());
        List<validationData> accessTokenExpectations2 = vData.addSuccessStatusCodes();
        //        accessTokenExpectations2 = vData.addExpectation(accessTokenExpectations2, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, updatedTestSettings2.getTestURL());

        // create access_tokens from client1's app_passwords
        List<String> createdAccessTokens1 = new ArrayList<>();
        // Make sure that the app passwords are good
        for (TokenValues tv1 : tokenValues1) {
            String newAccessToken = appPTcommon.getAccessTokenFromAppPassword(updatedTestSettings1, tv1.getApp_password(), true, appPTcommon.getGoodTokenEndpointExpectations(updatedTestSettings1));
            //            helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), newAccessToken, Constants.HEADER, updatedTestSettings1, accessTokenExpectations1, Constants.INVOKE_PROTECTED_RESOURCE);
            accessProtectedAppViaPropagation(newAccessToken, updatedTestSettings1, accessTokenExpectations1, Constants.INVOKE_PROTECTED_RESOURCE);
            createdAccessTokens1.add(newAccessToken);

        }
        // create access_tokens from client2's app_passwords
        List<String> createdAccessTokens2a = new ArrayList<>();
        for (TokenValues tv2 : tokenValues2a) {
            String newAccessToken = appPTcommon.getAccessTokenFromAppPassword(updatedTestSettings2, tv2.getApp_password(), true, appPTcommon.getGoodTokenEndpointExpectations(updatedTestSettings2));
            //            helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), newAccessToken, Constants.HEADER, updatedTestSettings2, accessTokenExpectations2, Constants.INVOKE_PROTECTED_RESOURCE);
            accessProtectedAppViaPropagation(newAccessToken, updatedTestSettings2, accessTokenExpectations2, Constants.INVOKE_PROTECTED_RESOURCE);
            createdAccessTokens2a.add(newAccessToken);

        }

        // logout expectations - just make sure we landed on the logout page - always with a good status code
        invokeLogout(webClient1, updatedTestSettings1, initLogoutExpectations(finalAppWithoutPostRedirect), response1);

        // Test uses the standard backchannelLogoutUri - so end_session with bcl steps should be performed - set expected states accordingly
        AfterLogoutStates states1 = new AfterLogoutStates(Constants.usesValidBCLEndpoint, updatedTestSettings1.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings1.getRsTokenType(), isUsingSaml());
        states1.setIsUsingIntrospect(true);

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient1, updatedTestSettings1, tokens1, states1);

        // Test uses the standard backchannelLogoutUri - so end_session with bcl steps should be performed - set expected states accordingly
        AfterLogoutStates states2 = new AfterLogoutStates(Constants.usesValidBCLEndpoint, updatedTestSettings2.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings2.getRsTokenType(), isUsingSaml());
        states2.setIsUsingIntrospect(true);
        // TODO - still investigating if the OP cookie can be removed
        // logout doesn't know about another sessions cookies.
        states2.setOPNoCookiesRemoved();

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient2, updatedTestSettings2, tokens2, states2);

        //        // TODO - accessProtectedApp is part of validateLogoutResult - do we really need to call it again?
        //        // After logging out make sure that the cache has been cleaned out and we need to log in again.
        //        accessProtectedApp(webClient1, updatedTestSettings1);
        //
        //        // After logging out make sure that the cache has been cleaned out and we need to log in again.
        //        accessProtectedApp(webClient2, updatedTestSettings2);

        Log.info(thisClass, _testName, "************************************************************ BEFORE reusing access_tokens from app_passwords ***************************************************************");
        // Now, let's see if the access_tokens created from the app_passwords are still valid after logout
        for (String accessToken : createdAccessTokens1) {
            //            helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), accessToken, Constants.HEADER, updatedTestSettings1, accessTokenExpectations1, Constants.INVOKE_PROTECTED_RESOURCE);
            accessProtectedAppViaPropagation(accessToken, updatedTestSettings1, accessTokenExpectations1, Constants.INVOKE_PROTECTED_RESOURCE);

        }
        // make sure that access_tokens created for the second client are still valid
        for (String accessToken : createdAccessTokens2a) {
            //            helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), accessToken, Constants.HEADER, updatedTestSettings2, accessTokenExpectations2, Constants.INVOKE_PROTECTED_RESOURCE);
            accessProtectedAppViaPropagation(accessToken, updatedTestSettings2, accessTokenExpectations2, Constants.INVOKE_PROTECTED_RESOURCE);

        }

        Log.info(thisClass, _testName, "************************************************************ BEFORE creating additional access_tokens from app_passwords ***************************************************************");

        // make sure that the app_passwords created for the first client are still valid - the logout shouldn't have touched them
        for (TokenValues tv1 : tokenValues1) {
            String newAccessToken = appPTcommon.getAccessTokenFromAppPassword(updatedTestSettings1, tv1.getApp_password(), true, appPTcommon.getGoodTokenEndpointExpectations(updatedTestSettings1));
            //            helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), newAccessToken, Constants.HEADER, updatedTestSettings1, accessTokenExpectations1, Constants.INVOKE_PROTECTED_RESOURCE);
            accessProtectedAppViaPropagation(newAccessToken, updatedTestSettings1, accessTokenExpectations1, Constants.INVOKE_PROTECTED_RESOURCE);
        }

        // make sure that the app_passwords created for the second client are still valid - the logout shouldn't have touched them
        for (TokenValues tv2 : tokenValues2a) {
            String newAccessToken = appPTcommon.getAccessTokenFromAppPassword(updatedTestSettings2, tv2.getApp_password(), true, appPTcommon.getGoodTokenEndpointExpectations(updatedTestSettings2));
            //            helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), newAccessToken, Constants.HEADER, updatedTestSettings2, accessTokenExpectations2, Constants.INVOKE_PROTECTED_RESOURCE);
            accessProtectedAppViaPropagation(newAccessToken, updatedTestSettings2, accessTokenExpectations2, Constants.INVOKE_PROTECTED_RESOURCE);
        }

    }

    @Mode(TestMode.LITE)
    @Test
    public void BasicBCLTests_appTokens_additionalClient() throws Exception {

        testOPServer.addIgnoredServerExceptions(MessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID, MessageConstants.CWOAU0029E_TOKEN_NOT_IN_CACHE);
        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_appTokens", "bcl_appTokensClient1", false);
        updatedTestSettings1.setAppTokenEndpt(updatedTestSettings1.getAppTokensEndpt().replace("OidcConfigSample", "OidcConfigSample_appTokens"));
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_appTokens", "bcl_appTokensClient2", false);
        updatedTestSettings2.setAppTokenEndpt(updatedTestSettings2.getAppTokensEndpt().replace("OidcConfigSample", "OidcConfigSample_appTokens"));

        Object response1 = accessProtectedApp(webClient1, updatedTestSettings1);
        Object response2 = accessProtectedApp(webClient2, updatedTestSettings2);

        TokenKeeper tokens1 = setTokenKeeperFromUnprotectedApp(webClient1, updatedTestSettings1, 1);
        TokenKeeper tokens2 = setTokenKeeperFromUnprotectedApp(webClient2, updatedTestSettings2, 1);

        List<TokenValues> tokenValues1 = appPTcommon.createAndValidateAppTokensFromSameAccessToken(updatedTestSettings1, tokens1.getAccessToken(), _testName, "bcl_appTokensClient1", AppPasswordsAndTokensCommonTest.unknownLifetime, 3);
        List<TokenValues> tokenValues2a = appPTcommon.createAndValidateAppTokensFromSameAccessToken(updatedTestSettings2, tokens2.getAccessToken(), _testName, "bcl_appTokensClient2", AppPasswordsAndTokensCommonTest.unknownLifetime, 3);

        List<validationData> appTokenExpectations1 = vData.addSuccessStatusCodes();
        //        appTokenExpectations1 = vData.addExpectation(appTokenExpectations1, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, updatedTestSettings1.getTestURL());
        List<validationData> appTokenExpectations2 = vData.addSuccessStatusCodes();
        //        appTokenExpectations2 = vData.addExpectation(appTokenExpectations2, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, updatedTestSettings2.getTestURL());

        // Make sure that the app tokens are good
        for (TokenValues tv : tokenValues1) {
            //            helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), tv.getApp_token(), Constants.HEADER, updatedTestSettings1, appTokenExpectations1, Constants.INVOKE_PROTECTED_RESOURCE);
            accessProtectedAppViaPropagation(tv.getApp_token(), updatedTestSettings1, appTokenExpectations1, Constants.INVOKE_PROTECTED_RESOURCE);

        }
        for (TokenValues tv : tokenValues2a) {
            //            helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), tv.getApp_token(), Constants.HEADER, updatedTestSettings2, appTokenExpectations2, Constants.INVOKE_PROTECTED_RESOURCE);
            accessProtectedAppViaPropagation(tv.getApp_token(), updatedTestSettings2, appTokenExpectations2, Constants.INVOKE_PROTECTED_RESOURCE);

        }

        // logout expectations - just make sure we landed on the logout page - always with a good status code
        invokeLogout(webClient1, updatedTestSettings1, initLogoutExpectations(finalAppWithoutPostRedirect), response1);

        // Test uses the standard backchannelLogoutUri - so end_session with bcl steps should be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(Constants.usesValidBCLEndpoint, updatedTestSettings1.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings1.getRsTokenType(), isUsingSaml());
        states.setIsUsingIntrospect(true);

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient1, updatedTestSettings1, tokens1, states);

        // After logging out make sure that the cache has been cleaned out and we can log in again.
        accessProtectedApp(webClient1, updatedTestSettings1);
        // After logging out make sure that the cache has been cleaned out and we can log in again.
        accessProtectedApp(webClient2, updatedTestSettings2);

        Log.info(thisClass, _testName, "************************************************************ BEFORE validating the app_Tokens ***************************************************************");
        for (TokenValues tv : tokenValues1) {
            //            helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), tv.getApp_token(), Constants.HEADER, updatedTestSettings1, appTokenExpectations1, Constants.INVOKE_PROTECTED_RESOURCE);
            accessProtectedAppViaPropagation(tv.getApp_token(), updatedTestSettings1, appTokenExpectations1, Constants.INVOKE_PROTECTED_RESOURCE);

        }
        for (TokenValues tv : tokenValues2a) {
            //            helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), tv.getApp_token(), Constants.HEADER, updatedTestSettings2, appTokenExpectations2, Constants.INVOKE_PROTECTED_RESOURCE);
            accessProtectedAppViaPropagation(tv.getApp_token(), updatedTestSettings2, appTokenExpectations2, Constants.INVOKE_PROTECTED_RESOURCE);

        }

    }

    /**
     * Various config attributes that may be affected by, or affect bcl
     *
     * OAuth (OP)
     * userClientTokenLimit - done
     * revokeAccessTokensWithRefreshTokens
     * accessTokenCacheEnabled
     * Any app_password impact?
     *
     * OIDC (OP)
     *
     * oidc client (RP)
     * accessTokenInLtpaCookie
     * validationMethod - have tests using userinfo and introspect
     *
     *
     * oidcclient (Social)
     *
     *
     *
     *
     */
}
