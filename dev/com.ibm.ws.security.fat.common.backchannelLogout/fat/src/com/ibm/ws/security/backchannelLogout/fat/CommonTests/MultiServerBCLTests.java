/*******************************************************************************
 * Copyright (c) 2023, 2024 IBM Corporation and others.
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
import com.ibm.ws.security.backchannelLogout.fat.utils.AfterLogoutStates.BCL_FORM;
import com.ibm.ws.security.backchannelLogout.fat.utils.BackChannelLogout_RegisterClients;
import com.ibm.ws.security.backchannelLogout.fat.utils.Constants;
import com.ibm.ws.security.backchannelLogout.fat.utils.TokenKeeper;
import com.ibm.ws.security.backchannelLogout.fat.utils.VariationSettings;
import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;
import com.ibm.ws.security.fat.common.social.SocialConstants;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.AppPasswordsAndTokensCommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
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
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * This test class contains tests that validate the proper behavior in end-to-end end_session requests when multiple client
 * servers are used.
 * These tests will focus on the proper logout/end_session behavior based on the OP and OAuth registered client configs.
 **/

@SuppressWarnings("serial")
@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
@AllowedFFDC({ "org.apache.http.NoHttpResponseException", "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidTokenException" })
public class MultiServerBCLTests extends BackChannelLogoutCommonTests {

    protected static Class<?> thisClass = MultiServerBCLTests.class;

    public static CommonValidationTools validationTools = new CommonValidationTools();

    protected static AppPasswordsAndTokensCommonTest appPTcommon = new AppPasswordsAndTokensCommonTest();

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    /**
     * Repeat tests using OIDC (with a Local Store) or OIDC with SAML OP's, OIDC and Social clients, end_session
     * While it would be nice to run all types of clients with all server configuration (and easy to do), it just takes too long
     * to run
     * Variations:
     * OIDC_end_session_LOCALSTORE - OIDC Client - invoke end_session on the OP
     * Social_end_session - Social Client - invoke end_session on the OP
     * SAML_end_session - OP with SAML to perform authorize - invoke end_session on the SP
     *
     * @return RepeatTests object for each variation of this class that will be run
     */
    public static RepeatTests createRepeats(String callingProject) {

        String localStore = "LOCALSTORE";
        Log.info(thisClass, "createRepeats", "Starting createRepeats");

        // Using limited repeats since these tests search for messages not to appear in the logs and those timeouts cause the tests to take too long to run
        RepeatTests rTests = null;
        if (callingProject.equals(Constants.OIDC)) {
            rTests = addRepeat(rTests, new SecurityTestRepeatAction(Constants.OIDC + "_" + Constants.END_SESSION + "_" + localStore));
        } else {
            if (callingProject.equals(Constants.SOCIAL)) {
                rTests = addRepeat(rTests, new SecurityTestRepeatAction(SocialConstants.SOCIAL + "_" + Constants.END_SESSION));
            } else {
                rTests = addRepeat(rTests, new SecurityTestRepeatAction(Constants.SAML + "_" + Constants.END_SESSION));
            }
        }

        return rTests;

    }

    @BeforeClass
    public static void setUp() throws Exception {

        currentRepeatAction = RepeatTestFilter.getRepeatActionsAsString();

        makeRandomSettingSelections();

        // set some test flags based on the repeat action - these flags will be used by the test cases and test methods to determine the steps to run or what to expect
        vSettings = new VariationSettings(currentRepeatAction);

        // Start a normal OP, or an OP that uses SAML to authorize (in this case, we need to fire up a server running Shibboleth
        startProviderBasedOnRepeat(tokenType);

        // start an OIDC RP or a Social oidc client
        startClientsBasedOnRepeat(tokenType);

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

        // we can re-use the basic OP config
        if (vSettings.loginMethod.equals(Constants.SAML)) {
            Log.info(thisClass, "setUp", "pickAnIDP: " + pickAnIDP);
            // IDP server uses ports bvt.prop.security_3_HTTP_default*
            testIDPServer = commonSetUp("com.ibm.ws.security.saml.sso-2.0_fat.shibboleth", "server_orig.xml", Constants.IDP_SERVER_TYPE, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.SKIP_CHECK_FOR_SECURITY_STARTED, true);
            pickAnIDP = true; // tells commonSetup to update the OP server files with current/this instance IDP server info
            testIDPServer.setRestoreServerBetweenTests(false);
            testOPServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.op.saml", adjustServerConfig("op_server_multiServerTests.xml"), Constants.OIDC_OP, serverApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT);
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
                testOPServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.op.mongo", adjustServerConfig("op_server_multiServerTests.xml"), Constants.OIDC_OP, serverApps, Constants.DO_NOT_USE_DERBY, Constants.USE_MONGODB, extraMsgs, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT, Constants.JUNIT_REPORTING);
                // register clients after all servers are started and we know everyone's ports
                testSettings.setStoreType(StoreType.CUSTOM);
            } else {
                testOPServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.op", adjustServerConfig("op_server_multiServerTests.xml"), Constants.OIDC_OP, serverApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT);
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

        testOPServer.setRestoreServerBetweenTests(false);
        SecurityFatHttpUtils.saveServerPorts(testOPServer.getServer(), Constants.BVT_SERVER_1_PORT_NAME_ROOT);
        testOPServer.addIgnoredServerException(MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI);

    }

    /**
     * If the current repeat is an OIDC or SAML variation, start multiple OIDC RP clients, otherwise, start multiple Social oidc
     * clients
     *
     * @param tokenType
     *            flag to be passed to common tooling to set config settings in the OP to have it create opaque or jwt
     *            access_tokens
     * @throws Exception
     */
    public static void startClientsBasedOnRepeat(String tokenType) throws Exception {

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

        if (vSettings.loginMethod.equals(Constants.OIDC) || vSettings.loginMethod.equals(Constants.SAML)) {
            skipServerStart = true;
            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.rp", adjustServerConfig("rp_server_multiServerTests.xml"), Constants.OIDC_RP, clientApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT);
            updateClientCookieNameAndPort(clientServer, "clientCookieName", Constants.clientCookieName, false);
            clientServer.getServer().startServer("RP_1_setup.log");

            skipServerStart = true;
            clientServer2 = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.rp2", adjustServerConfig("rp_server_multiServerTests.xml"), Constants.OIDC_RP, clientApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);
            updateClientCookieNameAndPort(clientServer2, "clientCookieName", Constants.client2CookieName, false);
            clientServer2.getServer().startServer("RP_2_setup.log");

            skipServerStart = true;
            genericTestServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.rs", adjustServerConfig("rs_server_multiServerTests.xml"), Constants.GENERIC_SERVER, clientApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);
            updateClientCookieNameAndPort(genericTestServer, "clientCookieName", Constants.genericServerCookieName, false);
            genericTestServer.getServer().startServer("GenericServer_setup.log");

            skipServerStart = false;
            testSettings.setFlowType(Constants.RP_FLOW);
        } else {
            skipServerStart = true;
            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.social", adjustServerConfig("social_server_multiServerTests.xml"), Constants.OIDC_RP, clientApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT);
            Log.info(thisClass, "startClientsBasedOnRepeat", "Just before updating the cookie info");
            updateClientCookieNameAndPort(clientServer, "clientCookieName", Constants.clientCookieName, false);
            Log.info(thisClass, "startClientsBasedOnRepeat", "Just before starting the server");
            clientServer.getServer().startServer("Social_1_setup.log");

            skipServerStart = true;
            clientServer2 = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.social2", adjustServerConfig("social_server_multiServerTests.xml"), Constants.OIDC_RP, clientApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);
            updateClientCookieNameAndPort(clientServer2, "clientCookieName", Constants.client2CookieName, false);
            clientServer2.getServer().startServer("Social_2_setup.log");

            skipServerStart = true;
            genericTestServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.social.prop", adjustServerConfig("social_server_multiServerTests.xml"), Constants.GENERIC_SERVER, clientApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);
            updateClientCookieNameAndPort(genericTestServer, "clientCookieName", Constants.genericServerCookieName, false);
            genericTestServer.getServer().startServer("GenericServer_setup.log");

            skipServerStart = false;

            testSettings.setFlowType(SocialConstants.SOCIAL);
            //            clientServer.addIgnoredServerException(MessageConstants.CWWKG0058E_CONFIG_MISSING_REQUIRED_ATTRIBUTE); // the social client isn't happy with the public client not having a secret
        }

        clientServer.setRestoreServerBetweenTests(false);
        clientServer2.setRestoreServerBetweenTests(false);
        genericTestServer.setRestoreServerBetweenTests(false);
        SecurityFatHttpUtils.saveServerPorts(clientServer.getServer(), Constants.BVT_SERVER_2_PORT_NAME_ROOT);
        SecurityFatHttpUtils.saveServerPorts(clientServer2.getServer(), Constants.BVT_SERVER_5_PORT_NAME_ROOT);
        SecurityFatHttpUtils.saveServerPorts(genericTestServer.getServer(), Constants.BVT_SERVER_4_PORT_NAME_ROOT);

        clientServer.addIgnoredServerExceptions(MessageConstants.CWWKS1541E_BACK_CHANNEL_LOGOUT_ERROR, MessageConstants.CWWKS1543E_BACK_CHANNEL_LOGOUT_REQUEST_VALIDATION_ERROR);

        if (currentRepeatAction.contains(Constants.HTTP_SESSION)) {
            //            if (currentRepeatAction.contains(Constants.OIDC_RP) || currentRepeatAction.contains(SocialConstants.SOCIAL)) {
            logoutApp = clientServer.getHttpsString() + "/simpleLogoutTestApp/simpleLogout";
            //            } else {
            //                logoutApp = testOPServer.getHttpsString() + "/simpleLogoutTestApp/simpleLogout";
            //            }
        }

    }

    public static void registerClientsIfNeeded() throws Exception {

        if (currentRepeatAction.contains(Constants.MONGODB)) {
            Log.info(thisClass, "registerClientsIfNeeded", "Setting up mongo clients");
            regClients = new BackChannelLogout_RegisterClients(testOPServer, clientServer, clientServer2, genericTestServer);
            regClients.registerClientsForMultiServerBCLTests();
            testOPServer.addIgnoredServerException(MessageConstants.CWWKS1420E_CLIENT_NOT_AUTHORIZED_TO_INTROSPECT);
        }
    }

    protected void resetMultiServerLogout() throws Exception {

        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "clear_login_sids", "true");
        genericInvokeEndpointWithHttpUrlConn(_testName, null, clientServer.getHttpsString() + "/backchannelLogoutTestApp/backChannelLogoutMultiServer",
                Constants.PUTMETHOD, "resetMultiServerLogout", parms, null, vData.addSuccessStatusCodes());

    }

    protected void saveInstanceForMultiServerLogout(String sid, String bclInstance) throws Exception {

        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "sid", sid);
        parms = eSettings.addEndpointSettingsIfNotNull(parms, "bcl_endpoint", bclInstance);
        genericInvokeEndpointWithHttpUrlConn(_testName, null, clientServer.getHttpsString() + "/backchannelLogoutTestApp/backChannelLogoutMultiServer",
                Constants.PUTMETHOD, "saveInstanceForMultiServerLogout", parms, null, vData.addSuccessStatusCodes());

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
     * @param bclEndpoint
     *            The bcl endpoint that should have been invoked
     * @return returns the sid from the logout_token
     * @throws Exception
     */
    protected String validateCorrectBCLUrisCalled(TestServer server, TestSettings settings, boolean idTokenHintIncluded, String sidFromIdToken, String bclEndpoint) throws Exception {

        String this_method = "validateCorrectBCLUrisCalled";
        String logout_token = getLogoutTokenFromMessagesLog(server, "For sid: " + sidFromIdToken + " - Will invoke " + bclEndpoint + ".* with the passed in logout_token:");
        Log.info(thisClass, _testName, "logout_token: " + logout_token);

        if (idTokenHintIncluded) {
            TokenKeeper logoutToken = new TokenKeeper(logout_token);

            List<String> audience = logoutToken.getAudience();
            if (audience.contains(settings.getClientID())) {
                Log.info(thisClass, this_method, "Back Channel Logout was called for: " + settings.getClientID());
            } else {
                fail("Back Channel Logout was not called for audience: " + settings.getClientID());
            }

            if (sidFromIdToken == null) {
                Log.info(thisClass, this_method, "Skipping sid check");
            } else {
                if (logoutToken.getSessionId().contains(sidFromIdToken)) {
                    Log.info(thisClass, this_method, "Back Channel Logout was called for: " + sidFromIdToken);
                } else {
                    fail("Back Channel Logout was not called for sid: " + sidFromIdToken);
                }
            }
            return logoutToken.getSessionId();
        } else {
            if (logout_token == null) {
                addToAllowableTimeoutCount(1); // the search for the logout_token will result in a "timed out" message that we need to account for
                Log.info(thisClass, this_method, "Back Channel Logout was NOT called for: " + sidFromIdToken);
            } else {
                fail("Back Channel Logout was not called for sid: " + sidFromIdToken + " and should have been");
            }
            return null;
        }
    }

    protected String validateCorrectBCLUrisNotCalled(TestServer server, String sidFromIdToken, boolean diffUser) throws Exception {

        List<validationData> skippedSidExpectations = null;
        if (diffUser) {
            skippedSidExpectations = validationTools.addMessageExpectation(server, null, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.MSG_NOT_LOGGED, "Message log contained a message indicating that we logged out a sid that was for another user.  sid: " + sidFromIdToken, "For sid: " + sidFromIdToken);
        } else {
            skippedSidExpectations = validationTools.addMessageExpectation(server, null, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.MSG_NOT_LOGGED, "Message log contained a message indicating that we logged out a sid that was for another provider.  sid: " + sidFromIdToken, "For sid: " + sidFromIdToken);
        }
        addToAllowableTimeoutCount(1);
        for (validationData expected : skippedSidExpectations) {
            validationTools.validateWithServerLog(expected);
        }
        return null;
    }

    public TokenKeeper invokeAndSave(TestSettings settings, String bclEndpoint) throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TokenKeeper keeper = new TokenKeeper(webClient, accessProtectedApp(webClient, settings), settings.getFlowType());
        saveInstanceForMultiServerLogout(getStringValue(new JwtTokenForTest(keeper.getIdToken()).getMapPayload(), Constants.IDTOK_SESSION_ID), bclEndpoint);

        return keeper;

    }

    public void validateTokensStillGood(WebClient webClient, TestSettings settings) throws Exception {

        String action = "validateTokens";
        String status = Integer.toString(Constants.OK_STATUS);
        List<validationData> retryExpectations = vData.addExpectation(null, action, Constants.RESPONSE_STATUS, Constants.STRING_CONTAINS, "Did not receive status code " + status + ".", null, status);
        retryExpectations = vData.addExpectation(retryExpectations, action, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app", null, settings.getTestURL());
        retryExpectations = vData.addExpectation(retryExpectations, action, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not land on the back channel logout test app using provider: " + settings.getProvider(), null, settings.getProvider());

        genericInvokeEndpoint(_testName, webClient, null, settings.getTestURL(), Constants.GETMETHOD, action, null, null, retryExpectations, settings, false);
    }

    /********************************************** Tests **********************************************/

    /**
     * This test logs in using clients from one provider across multiple server instances.
     * The client configs within the OP provider can each only specify ONE bcl endpoint. If you log in using that one client on
     * multiple servers, there is no way for the OP's runtime to log out all of those instances. This test uses an application
     * that tracks session ids with the actual bcl endpoint that would be used to log out. The bcl endpoint coded in the provider
     * client config also points to that test app. When logout is invoked, the OP's bcl runtime calls the test app which will then
     * logout the sid instance for which it was called.
     *
     * @throws Exception
     */

    public void complexTest(boolean reuseWebClientForLogout, boolean passHint) throws Exception {
        // clean up any stored sids/bcl endpoints from prevous tests
        resetMultiServerLogout();

        clientServer.getServer().initializeAnyExistingMarks();

        //        boolean usingLogoutEndpoint = currentRepeatAction.contains(Constants.LOGOUT_ENDPOINT);
        //        boolean shouldWeReallyLogout = reuseWebClientForLogout || (passHint && !usingLogoutEndpoint);
        //        boolean shouldWeReallyLogout = reuseWebClientForLogout || passHint;
        Log.info(thisClass, "complexTest", "reuseWebClientForLogout: " + Boolean.toString(reuseWebClientForLogout));
        Log.info(thisClass, "complexTest", "passHint: " + Boolean.toString(passHint));
        Log.info(thisClass, "complexTest", "http_session with no end_session/logout: " + Boolean.toString(currentRepeatAction.contains(Constants.HTTP_SESSION) && vSettings.sessionLogoutEndpoint == null));
        boolean shouldWeReallyLogout = (reuseWebClientForLogout || (passHint)) && (!(currentRepeatAction.contains(Constants.HTTP_SESSION) && vSettings.sessionLogoutEndpoint == null));

        String provider1 = "OidcConfigSample_multiServer1";
        String provider1_client1 = "bcl_multiServer_client1-1";
        String provider1_client2 = "bcl_multiServer_client1-2";
        String provider1_client3 = "bcl_multiServer_client1-3";

        String provider2 = "OidcConfigSample_multiServer2";
        String provider2_client1 = "bcl_multiServer_client2-1";
        String provider2_client2 = "bcl_multiServer_client2-2";
        String provider2_client3 = "bcl_multiServer_client2-3";

        // create strings for each bcl endpoint
        // Provider 1
        String bclEndpoint1_1 = buildBackchannelLogoutUri(clientServer, provider1_client1);
        String bclEndpoint1_2 = buildBackchannelLogoutUri(clientServer2, provider1_client1);
        String bclEndpoint1_3 = buildBackchannelLogoutUri(clientServer, provider1_client2);
        String bclEndpoint1_4 = buildBackchannelLogoutUri(clientServer, provider1_client3);
        String bclEndpoint1_5 = buildBackchannelLogoutUri(genericTestServer, provider1_client1);

        // create strings for each bcl endpoint
        // Provider 2
        String bclEndpoint2_1 = buildBackchannelLogoutUri(clientServer, provider2_client1);
        String bclEndpoint2_2 = buildBackchannelLogoutUri(clientServer2, provider2_client1);
        String bclEndpoint2_3 = buildBackchannelLogoutUri(clientServer, provider2_client2);
        String bclEndpoint2_4 = buildBackchannelLogoutUri(clientServer, provider2_client3);
        String bclEndpoint2_5 = buildBackchannelLogoutUri(genericTestServer, provider2_client1);

        // create and update testSettings specific to each invocation
        // Provider 1
        TestSettings updatedTestSettings1_1_testuser = updateTestSettingsProviderAndClient(clientServer, provider1, provider1_client1, Constants.TESTUSER, Constants.TESTUSERPWD, false);
        TestSettings updatedTestSettings1_2_testuser = updateTestSettingsProviderAndClient(clientServer2, provider1, provider1_client1, Constants.TESTUSER, Constants.TESTUSERPWD, false);
        TestSettings updatedTestSettings1_2_user1 = updateTestSettingsProviderAndClient(clientServer2, provider1, provider1_client1, "user1", "security", false);
        TestSettings updatedTestSettings1_3_testuser = updateTestSettingsProviderAndClient(clientServer, provider1, provider1_client2, Constants.TESTUSER, Constants.TESTUSERPWD, false);
        TestSettings updatedTestSettings1_3_user1 = updateTestSettingsProviderAndClient(clientServer, provider1, provider1_client2, "user1", "security", false);
        TestSettings updatedTestSettings1_4 = updateTestSettingsProviderAndClient(clientServer, provider1, provider1_client3, Constants.TESTUSER, Constants.TESTUSERPWD, false);
        TestSettings updatedTestSettings1_5 = updateTestSettingsProviderAndClient(genericTestServer, provider1, provider1_client1, Constants.TESTUSER, Constants.TESTUSERPWD, false);

        // Provider 2
        TestSettings updatedTestSettings2_1 = updateTestSettingsProviderAndClient(clientServer, provider2, provider2_client1, Constants.TESTUSER, Constants.TESTUSERPWD, false);
        TestSettings updatedTestSettings2_2_testuser = updateTestSettingsProviderAndClient(clientServer2, provider2, provider2_client1, Constants.TESTUSER, Constants.TESTUSERPWD, false);
        TestSettings updatedTestSettings2_2_user1 = updateTestSettingsProviderAndClient(clientServer2, provider2, provider2_client1, "user1", "security", false);
        TestSettings updatedTestSettings2_3_testuser = updateTestSettingsProviderAndClient(clientServer, provider2, provider2_client2, Constants.TESTUSER, Constants.TESTUSERPWD, false);
        TestSettings updatedTestSettings2_3_user1 = updateTestSettingsProviderAndClient(clientServer, provider2, provider2_client2, "user1", "security", false);
        TestSettings updatedTestSettings2_4 = updateTestSettingsProviderAndClient(clientServer, provider2, provider2_client3, Constants.TESTUSER, Constants.TESTUSERPWD, false);
        TestSettings updatedTestSettings2_5 = updateTestSettingsProviderAndClient(genericTestServer, provider2, provider2_client1, Constants.TESTUSER, Constants.TESTUSERPWD, false);

        // access each protected app, save all of the tokens and cookies, store the session id and real bcl endpoint to use for that session in the logout test application request and
        // keep a copy of the full response so we can get the
        // Provider 1
        Log.info(thisClass, "CHC - Debug", "testUrl: " + updatedTestSettings1_1_testuser.getTestURL());
        TokenKeeper keeper1_1_testuser = invokeAndSave(updatedTestSettings1_1_testuser, bclEndpoint1_1);
        Log.info(thisClass, "CHC - Debug", "testUrl: " + updatedTestSettings1_2_testuser.getTestURL());
        TokenKeeper keeper1_2_testuser = invokeAndSave(updatedTestSettings1_2_testuser, bclEndpoint1_2);
        Log.info(thisClass, "CHC - Debug", "testUrl: " + updatedTestSettings1_2_user1.getTestURL() + " User: " + updatedTestSettings1_2_user1.getAdminUser());
        TokenKeeper keeper1_2_user1 = invokeAndSave(updatedTestSettings1_2_user1, bclEndpoint1_2);
        TokenKeeper keeper1_3_testuser = invokeAndSave(updatedTestSettings1_3_testuser, bclEndpoint1_3);
        TokenKeeper keeper1_3_user1 = invokeAndSave(updatedTestSettings1_3_user1, bclEndpoint1_3);
        TokenKeeper keeper1_4 = invokeAndSave(updatedTestSettings1_4, bclEndpoint1_4);

        List<endpointSettings> headers1 = eSettings.addEndpointSettingsIfNotNull(null, updatedTestSettings1_5.getHeaderName(), Constants.BEARER + " " + keeper1_1_testuser.getAccessToken());
        genericInvokeEndpoint(_testName, getAndSaveWebClient(true), null, updatedTestSettings1_5.getTestURL(), Constants.GETMETHOD, _testName, null, headers1, vData.addSuccessStatusCodes(), updatedTestSettings1_5, false);
        saveInstanceForMultiServerLogout(getStringValue(new JwtTokenForTest(keeper1_1_testuser.getIdToken()).getMapPayload(), Constants.IDTOK_SESSION_ID), bclEndpoint1_5);

        // Provider 2
        TokenKeeper keeper2_1 = invokeAndSave(updatedTestSettings2_1, bclEndpoint2_1);
        TokenKeeper keeper2_2_testuser = invokeAndSave(updatedTestSettings2_2_testuser, bclEndpoint2_2);
        TokenKeeper keeper2_2_user1 = invokeAndSave(updatedTestSettings2_2_user1, bclEndpoint2_2);
        TokenKeeper keeper2_3_testuser = invokeAndSave(updatedTestSettings2_3_testuser, bclEndpoint2_3);
        TokenKeeper keeper2_3_user1 = invokeAndSave(updatedTestSettings2_3_user1, bclEndpoint2_3);
        TokenKeeper keeper2_4 = invokeAndSave(updatedTestSettings2_4, bclEndpoint2_4);

        List<endpointSettings> headers2 = eSettings.addEndpointSettingsIfNotNull(null, updatedTestSettings2_5.getHeaderName(), Constants.BEARER + " " + keeper2_1.getAccessToken());
        genericInvokeEndpoint(_testName, getAndSaveWebClient(true), null, updatedTestSettings2_5.getTestURL(), Constants.GETMETHOD, _testName, null, headers2, vData.addSuccessStatusCodes(), updatedTestSettings2_5, false);
        saveInstanceForMultiServerLogout(getStringValue(new JwtTokenForTest(keeper2_1.getIdToken()).getMapPayload(), Constants.IDTOK_SESSION_ID), bclEndpoint2_5);

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(BCL_FORM.TEST_BCL, vSettings.finalAppWithoutPostRedirect, reuseWebClientForLogout);
        // check for error messages when the logout test app tries to invoke bcl for the instance on the RS server - there should be nothing to clean up for a propagated access
        if (shouldWeReallyLogout) {
            logoutExpectations = validationTools.addMessageExpectation(genericTestServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1541E_BACK_CHANNEL_LOGOUT_ERROR + ".*bcl_multiServer_client1-1.*");
            logoutExpectations = validationTools.addMessageExpectation(genericTestServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1543E_BACK_CHANNEL_LOGOUT_REQUEST_VALIDATION_ERROR);
            logoutExpectations = validationTools.addMessageExpectation(genericTestServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1552E_NO_RECENT_SESSIONS_WITH_CLAIMS + ".*" + getStringValue(new JwtTokenForTest(keeper1_1_testuser.getIdToken()).getMapPayload(), Constants.IDTOK_SESSION_ID) + ".*");
            // there should be no request made to the RS server for the logout of provider2's propagated token
        } else {
            // no real reference to what to logout passed - the RS should not be called at all
            logoutExpectations = validationTools.addMessageExpectation(genericTestServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.MSG_NOT_LOGGED, "Message log did contain a message (" + MessageConstants.CWWKS1541E_BACK_CHANNEL_LOGOUT_ERROR + ") indicating that a there was a problem invoking the back channel logout - logout should not have been called.", MessageConstants.CWWKS1541E_BACK_CHANNEL_LOGOUT_ERROR + ".*bcl_multiServer_client1-1.*");
            logoutExpectations = validationTools.addMessageExpectation(genericTestServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.MSG_NOT_LOGGED, "Message log did contain a message (" + MessageConstants.CWWKS1543E_BACK_CHANNEL_LOGOUT_REQUEST_VALIDATION_ERROR + ") indicating that a there was a problem invoking the back channel logout - logout should not have been called.", MessageConstants.CWWKS1543E_BACK_CHANNEL_LOGOUT_REQUEST_VALIDATION_ERROR);
            logoutExpectations = validationTools.addMessageExpectation(genericTestServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.MSG_NOT_LOGGED, "Message log did contain a message (" + MessageConstants.CWWKS1552E_NO_RECENT_SESSIONS_WITH_CLAIMS + ") indicating that a there was a problem invoking the back channel logout - logout should not have been called.", MessageConstants.CWWKS1552E_NO_RECENT_SESSIONS_WITH_CLAIMS + ".*" + getStringValue(new JwtTokenForTest(keeper1_1_testuser.getIdToken()).getMapPayload(), Constants.IDTOK_SESSION_ID) + ".*");
            // there should be no request made to the RS server for the logout of provider2's propagated token
        }
        logoutExpectations = validationTools.addMessageExpectation(genericTestServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.MSG_NOT_LOGGED, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1552E_NO_RECENT_SESSIONS_WITH_CLAIMS + ".*" + getStringValue(new JwtTokenForTest(keeper2_1.getIdToken()).getMapPayload(), Constants.IDTOK_SESSION_ID) + ".*");
        addToAllowableTimeoutCount(1);

        WebClient webClientForLogout = keeper1_1_testuser.getWebClient();
        if (!reuseWebClientForLogout) {
            webClientForLogout = getAndSaveWebClient(true);
        }
        // Logout
        if (passHint) {
            invokeLogout(webClientForLogout, updatedTestSettings1_1_testuser, logoutExpectations, keeper1_1_testuser.getIdToken(), reuseWebClientForLogout);
        } else {
            invokeLogout(webClientForLogout, updatedTestSettings1_1_testuser, logoutExpectations, (String) null, reuseWebClientForLogout);

        }

        clientServer.getServer().resetLogMarks();

        // make sure that the backChannelLogoutUri has been called for each client that we logged in (using the user testuser) (within the provider that we're logging out)
        // make sure that the backChannelLogoutUri has NOT been called for each client that we logged in (using a user other than testuser)
        // Provider 1
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings1_1_testuser, shouldWeReallyLogout, keeper1_1_testuser.getSessionId(), bclEndpoint1_1);
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings1_2_testuser, shouldWeReallyLogout, keeper1_2_testuser.getSessionId(), bclEndpoint1_2);
        validateCorrectBCLUrisNotCalled(clientServer, keeper1_2_user1.getSessionId(), true);
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings1_3_testuser, shouldWeReallyLogout, keeper1_3_testuser.getSessionId(), bclEndpoint1_3);
        validateCorrectBCLUrisNotCalled(clientServer, keeper1_3_user1.getSessionId(), true);
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings1_4, shouldWeReallyLogout, keeper1_4.getSessionId(), bclEndpoint1_4);

        // Provider 2
        validateCorrectBCLUrisNotCalled(clientServer, keeper2_1.getSessionId(), false);
        validateCorrectBCLUrisNotCalled(clientServer, keeper2_2_testuser.getSessionId(), false);
        validateCorrectBCLUrisNotCalled(clientServer, keeper2_2_user1.getSessionId(), true);
        validateCorrectBCLUrisNotCalled(clientServer, keeper2_3_testuser.getSessionId(), false);
        validateCorrectBCLUrisNotCalled(clientServer, keeper2_3_user1.getSessionId(), true);
        validateCorrectBCLUrisNotCalled(clientServer, keeper2_4.getSessionId(), false);

        // Please NOTE - the validateCorrectBCLUris(Called|NotCalled) methods need to be run before the validateTokensStillGood methods since the mark in the log will be reset by a call in validateTokensStillGood
        // now make sure that the other users cookies/tokens are still valid by using them (we checked that the sids were not used to invoke bcl endpoints already)
        // Provider 1
        validateTokensStillGood(keeper1_2_user1.getWebClient(), updatedTestSettings1_2_user1);
        validateTokensStillGood(keeper1_3_user1.getWebClient(), updatedTestSettings1_3_user1);

        // Provider 2
        validateTokensStillGood(keeper2_1.getWebClient(), updatedTestSettings2_1);
        validateTokensStillGood(keeper2_2_testuser.getWebClient(), updatedTestSettings2_2_testuser);
        validateTokensStillGood(keeper2_2_user1.getWebClient(), updatedTestSettings2_2_user1);
        validateTokensStillGood(keeper2_3_testuser.getWebClient(), updatedTestSettings2_3_testuser);
        validateTokensStillGood(keeper2_3_user1.getWebClient(), updatedTestSettings2_3_user1);
        validateTokensStillGood(keeper2_4.getWebClient(), updatedTestSettings2_4);

    }

    @Mode(TestMode.LITE)
    @Test
    public void MultiServerBCLTests_withIdTokenHint() throws Exception {
        complexTest(true, true);
    }

    @Test
    public void MultiServerBCLTests_withoutIdTokenHint() throws Exception {

        complexTest(true, false);
    }

    @Test
    public void MultiServerBCLTests_withIdTokenHint_newWebClientForLogout() throws Exception {

        complexTest(false, true);
    }

    @Test
    public void MultiServerBCLTests_withoutIdTokenHint_newWebClientForLogout() throws Exception {

        complexTest(false, false);
    }

}
