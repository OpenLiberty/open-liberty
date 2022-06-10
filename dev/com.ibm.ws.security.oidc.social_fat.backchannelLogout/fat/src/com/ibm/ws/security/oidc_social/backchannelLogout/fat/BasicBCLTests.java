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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.actions.SecurityTestRepeatAction;
import com.ibm.ws.security.fat.common.social.SocialConstants;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.oidc_social.backchannelLogout.fat.CommonTests.BackChannelLogoutCommonTests;
import com.ibm.ws.security.oidc_social.backchannelLogout.fat.utils.AfterLogoutStates;
import com.ibm.ws.security.oidc_social.backchannelLogout.fat.utils.Constants;
import com.ibm.ws.security.oidc_social.backchannelLogout.fat.utils.SkipIfHttpLogout;
import com.ibm.ws.security.oidc_social.backchannelLogout.fat.utils.TokenKeeper;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServerWrapper;

// TODO - review test output once 20799 is resolved - right now, I'm seeing multiple calls the 400/500 test apps that should
// probably only be one call
// once end_session is called for a provider, it shouldn't call the bcl for those clients until they're logged in again
// Keep in mind that many of these bcl test apps don't actually logout - they invoke test apps to capture the logout_token or
// count how many calls are made, ...

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

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    // Repeat tests using the OIDC and Social clients, end_session or http session logout and when http session logout, have it invoke either the logout or end_session in the OP
    // Variations:
    //     OIDC_end_session - OIDC Client - invoke end_session on the OP
    //     OIDC_http_session_end_session - OIDC Client - invoke test app on the RP that does a  HttpServletRequest.logout(), then invokes the end_session endpoint of the OP Provider
    //     OIDC_http_session_logout - OIDC Client - invoke test app on the RP that does a  HttpServletRequest.logout(), then invokes the logout endpoint of the OP Provider
    //     Social_end_session - Social Client - invoke end_session on the OP
    //     Social_http_session_end_session - Social Client - invoke test app on the Social Client that does a  HttpServletRequest.logout(), then invokes the end_session endpoint of the OP Provider
    //     Social_http_session_logout - Social Client - invoke test app on the Social Client that does a  HttpServletRequest.logout(), then invokes the logout endpoint of the OP Provider
    //     SAML_logout - OP with SAML to perform authorize - invoke logout on the IDP

    // TODO - do we need to have a logout test app on the OP to see what it would do? --- yes!!!
    @ClassRule
    //    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(Constants.OIDC + "_" + Constants.END_SESSION))
    //            .andWith(new SecurityTestRepeatAction(Constants.OIDC + "_" + Constants.HTTP_SESSION + "_" + Constants.LOGOUT_ENDPOINT))
    //            .andWith(new SecurityTestRepeatAction(Constants.OIDC + "_" + Constants.HTTP_SESSION + "_" + Constants.END_SESSION))
    //            .andWith(new SecurityTestRepeatAction(SocialConstants.SOCIAL + "_" + Constants.END_SESSION))
    //            .andWith(new SecurityTestRepeatAction(SocialConstants.SOCIAL + "_" + Constants.HTTP_SESSION + "_" + Constants.LOGOUT_ENDPOINT))
    //            .andWith(new SecurityTestRepeatAction(SocialConstants.SOCIAL + "_" + Constants.HTTP_SESSION + "_" + Constants.END_SESSION))
    //            .andWith(new SecurityTestRepeatAction(Constants.SAML + "_" + Constants.LOGOUT_ENDPOINT));

    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(Constants.OIDC + "_" + Constants.END_SESSION));
    //    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(Constants.OIDC + "_" + Constants.HTTP_SESSION + "_" + Constants.LOGOUT_ENDPOINT));
    //    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(Constants.OIDC + "_" + Constants.HTTP_SESSION + "_" + Constants.END_SESSION));
    //    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(SocialConstants.SOCIAL + "_" + Constants.END_SESSION));
    //    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(SocialConstants.SOCIAL + "_" + Constants.HTTP_SESSION + "_" + Constants.LOGOUT_ENDPOINT));
    //    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(SocialConstants.SOCIAL + "_" + Constants.HTTP_SESSION + "_" + Constants.END_SESSION));
    // saml runtime isn't implemented yet - the tests can get an access_token, but bcl function is not called, so we won't hit our bcl test apps, or clear the proper cookies, ...
    //    public static RepeatTests repeat = RepeatTests.with(new SecurityTestRepeatAction(Constants.SAML + "_" + Constants.LOGOUT_ENDPOINT));
    // TODO - should we try to test with SP initiated and ibm_security_logout flows using saml?

    /*****************************************************************************************************************/
    /* TODO - add logic to include logout type in setting of successful<server>Logout and unsuccessful<server>Logout */
    /*****************************************************************************************************************/

    private static String finalAppWithPostRedirect = null;
    private static String finalAppWithoutPostRedirect = null;
    protected static String logoutApp = null;

    @BeforeClass
    public static void setUp() throws Exception {

        currentRepeatAction = RepeatTestFilter.getRepeatActionsAsString();

        String[] propagationTokenTypes = rsTools.chooseTokenSettings(Constants.OIDC_OP);
        String tokenType = propagationTokenTypes[0];
        // TODO - remove hard coded token selection
        tokenType = Constants.ACCESS_TOKEN_KEY;
        //        tokenType = Constants.JWT_TOKEN;
        Log.info(thisClass, "setupBeforeTest", "inited tokenType to: " + tokenType);

        // Start a normal OP, or an OP that uses SAML to authorize (in this case, we need to fire up a server running Shibboleth
        startProviderBasedOnRepeat(tokenType);

        // start an OIDC RP or a Social oidc client
        startClientBasedOnRepeat(tokenType);

        // set some test flags based on the repeat action - these flags will be used by the test cases and test methods to determine the steps to run or what to expect
        setConfigBasedOnRepeat();

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
                add(Constants.simpleLogoutApp);
            }
        };

        if (currentRepeatAction.contains(Constants.SAML)) {
            Log.info(thisClass, "setUp", "pickAnIDP: " + pickAnIDP);
            testIDPServer = commonSetUp("com.ibm.ws.security.saml.sso-2.0_fat.shibboleth", "server_orig.xml", Constants.IDP_SERVER_TYPE, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.SKIP_CHECK_FOR_SECURITY_STARTED, true);
            pickAnIDP = true; // tells commonSetup to update the OP server files with current/this instance IDP server info
            testOPServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.op.saml", "op_server_basicTests.xml", Constants.OIDC_OP, serverApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT);
            // now, we need to update the IDP files
            shibbolethHelpers.fixSPInfoInShibbolethServer(testOPServer, testIDPServer);
            shibbolethHelpers.fixVarsInShibbolethServerWithDefaultValues(testIDPServer);
            // now, start the shibboleth app with the updated config info
            shibbolethHelpers.startShibbolethApp(testIDPServer);
            testOPServer.addIgnoredServerException(MessageConstants.CWWKS5207W_SAML_CONFIG_IGNORE_ATTRIBUTES);
        } else {
            useLdap = false;
            Log.info(thisClass, "beforeClass", "Set useLdap to: " + useLdap);
            testOPServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.op", "op_server_basicTests.xml", Constants.OIDC_OP, serverApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT);
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
        if (currentRepeatAction.contains(Constants.LOGOUT_ENDPOINT)) {
            clientApps.add(Constants.simpleLogoutApp);
        }

        if (currentRepeatAction.contains(Constants.OIDC) || currentRepeatAction.contains(Constants.SAML)) {
            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.rp", "rp_server_basicTests.xml", Constants.OIDC_RP, clientApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT);
            updateClientCookieNameAndPort(clientServer, "clientCookieName", Constants.clientCookieName);
            //            clientServer2 = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.rp.2", "rp_server_basicTests.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);
            //            client2CookieName = "testRP2Cookie";
            //            updateClientCookieNameAndPort(clientServer2, "clientCookieName", client2CookieName);
            testSettings.setFlowType(Constants.RP_FLOW);
        } else {
            clientServer = commonSetUp("com.ibm.ws.security.backchannelLogout_fat.social", "social_server_basicTests.xml", Constants.OIDC_RP, clientApps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE, true, true, tokenType, Constants.X509_CERT);
            updateClientCookieNameAndPort(clientServer, "clientCookieName", Constants.clientCookieName);
            // TODO - add second client
            testSettings.setFlowType(SocialConstants.SOCIAL);

        }

    }

    /**
     * There are 3 main setups and each has 1-3 different ways that we can log out - this method will set some flags to tell the
     * test cases and supporting methods what type of config we have and the logout that we're expecting
     * 1) OIDC Provider with an openidconnect client
     * --- invoke end_session on the OP to log out
     * --- invoke a test app on the rp to invoke http logout then invoke end_session on the OP
     * --- invoke a test app on the rp to invoke http logout then invoke logout on the OP
     * 2) OIDC Provider with social client
     * --- invoke end_session on the OP to log out
     * --- invoke a test app on the social client to invoke http logout then invoke end_session on the OP
     * --- invoke a test app on the social client to invoke http logout then invoke logout on the OP
     * 3) OIDC Provider with SAML with an openidconnect client
     * --- invoke IDP logout on the IDP (Shibboleth server) to log out
     **/
    public static void setConfigBasedOnRepeat() throws Exception {

        finalAppWithPostRedirect = Constants.postLogoutJSessionIdApp;

        if (currentRepeatAction.contains(Constants.HTTP_SESSION)) {
            logoutMethodTested = Constants.LOGOUT_ENDPOINT;
            logoutApp = clientServer.getHttpsString() + "/simpleLogoutTestApp/simpleLogout";
            SkipIfHttpLogout.usingHttpLogout = true;
            if (currentRepeatAction.contains(Constants.END_SESSION)) {
                sessionLogoutEndpoint = Constants.END_SESSION;
                finalAppWithoutPostRedirect = Constants.defaultLogoutPage;
            } else {
                sessionLogoutEndpoint = Constants.LOGOUT_ENDPOINT;
                finalAppWithoutPostRedirect = Constants.LOGOUT_ENDPOINT;
                // override for configs that use post redrects - using the logout endpoint on the OP will NOT result in a call to them
                finalAppWithPostRedirect = Constants.LOGOUT_ENDPOINT;
            }
        } else {
            // TODO will probably need some more specific code from SAML once the runtime code is written
            if (currentRepeatAction.contains(Constants.SAML)) {
                logoutMethodTested = Constants.SAML;
            } else {
                logoutMethodTested = Constants.END_SESSION;
            }
            finalAppWithoutPostRedirect = Constants.defaultLogoutPage;
            SkipIfHttpLogout.usingHttpLogout = false;
            sessionLogoutEndpoint = null;
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
        if (logoutMethodTested.equals(Constants.SAML)) {
            updatedTestSettings.setEndSession(testIDPServer.getHttpsString() + "/idp/profile/Logout");
        } else {
            updatedTestSettings.setEndSession(updatedTestSettings.getEndSession().replace("OidcConfigSample", provider));
        }
        updatedTestSettings.setTokenEndpt(updatedTestSettings.getTokenEndpt().replace("OidcConfigSample", provider));
        updatedTestSettings.setClientID(client);
        updatedTestSettings.setClientSecret("mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger"); // all of the clients are using the same secret
        updatedTestSettings.setUserName(user);
        updatedTestSettings.setUserPassword(passwd);
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

        if (debug) {
            return null;

        }
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
                fail("Back Channel Logout was not called for sid: " + sidFromIdToken + " and should NOT have been");
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
        case Constants.SAML:
            return genericOP(_testName, webClient, settings, Constants.IDP_INITIATED_LOGOUT, logoutExpectations, previousResponse, null);
        case Constants.END_SESSION:
            // invoke end_session on the op - test controls if the id_token is passed as the id_token_hint by either passing or not passing the previous response
            return genericOP(_testName, webClient, settings, Constants.LOGOUT_ONLY_ACTIONS, logoutExpectations, previousResponse, null);
        case Constants.LOGOUT_ENDPOINT:
            if (sessionLogoutEndpoint.equals(Constants.LOGOUT_ENDPOINT)) {
                opLogoutEndpoint = testOPServer.getHttpsString() + "/oidc/endpoint/" + settings.getProvider() + "/" + Constants.LOGOUT_ENDPOINT;
            } else {
                opLogoutEndpoint = settings.getEndSession();
            }
            List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "opLogoutUri", opLogoutEndpoint);
            Log.info(thisClass, thisMethod, "chc - should be here");
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

    /********************************************** Tests **********************************************/

    /**
     * Main path back channel logout. Uses the real bcl endpoint, not a test app
     * One login and then end_session/logout
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void BasicBCLTests_mainPath() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_mainPath", "bcl_mainPath", false);

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = new TokenKeeper(webClient, response, updatedTestSettings.getFlowType());

        // logout expectations - just make sure we landed on the logout page - always with a good status code
        invokeLogout(webClient, updatedTestSettings, initLogoutExpectations(finalAppWithoutPostRedirect), response);

        // Test uses the standard backchannelLogoutUri - so end_session with bcl steps should be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType());

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

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

        TokenKeeper tokens = new TokenKeeper(webClient, response, updatedTestSettings.getFlowType());

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithPostRedirect);

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);

        // Test has a test app configured for the backchannelLogoutUri - so just he normal end_session steps will be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType()); // init with all cookies and tokens existing and valid
        states.setOPAllCookiesRemoved(); // after logout, this test expects all OP cookies to be removed
        states.setClientCookieRemovalBasedOnLogoutType();
        states.setAllTokensCleanedUp(); // after logout, this test expects access and refresh tokens to be invalid

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    /**
     * Show that multiple logins and then a back channel logout that takes a bit longer than usual will not timeout (as the delay
     * is still less than the back channel logout time allowed.
     */
    //    @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipIfHttpLogout.class)
    @Test
    public void BasicBCLTests_defaultBCLTimeout_multipleLoginsWithinOP() throws Exception {

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_defaultBCLTimeout", "bcl_defaultBCLTimeout");
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_defaultBCLTimeout", "bcl_otherDefaultBCLTimeout");

        Object response = accessProtectedApp(webClient1, updatedTestSettings1);
        TokenKeeper tokens1 = new TokenKeeper(webClient1, response, updatedTestSettings1.getFlowType());

        response = accessProtectedApp(webClient2, updatedTestSettings2);
        TokenKeeper tokens2 = new TokenKeeper(webClient2, response, updatedTestSettings2.getFlowType());

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithPostRedirect);

        invokeLogout(webClient2, updatedTestSettings2, logoutExpectations, response);

        // Test has a test app configured for the backchannelLogoutUri - so just he normal end_session steps will be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(updatedTestSettings2.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings2.getRsTokenType()); // init with all cookies and tokens existing and valid
        states.setOPAllCookiesRemoved(); // after logout, this test expects all OP cookies to be removed
        states.setClientCookieRemovalBasedOnLogoutType();
        states.setAllTokensCleanedUp(); // after logout, this test expects access and refresh tokens to be invalid
        // TODO - only the refresh_token for the passed id_token will be cleaned up
        validateLogoutResult(webClient2, updatedTestSettings2, tokens2, states);

        AfterLogoutStates otherStates = new AfterLogoutStates(updatedTestSettings1.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings1.getRsTokenType()); // init with all cookies and tokens existing and valid
        otherStates.setAccessTokenValid(false); // after logout, this test expects access token only to be invalid
        // TODO end_session only revoking the refresh_token for the id_token that was passed
        validateLogoutResult(webClient1, updatedTestSettings1, tokens1, otherStates);

    }

    /**
    *
    */
    @AllowedFFDC({ "java.util.concurrent.CancellationException" })
    @Test
    public void BasicBCLTests_shortBCLTimeout() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_shortBCLTimeout", "bcl_shortBCLTimeout");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = new TokenKeeper(webClient, response, updatedTestSettings.getFlowType());

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithPostRedirect);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1649E_BACK_CHANNEL_LOGOUT_TIMEOUT);

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        // Test has a test app configured for the backchannelLogoutUri - so just he normal end_session steps will be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType()); // init with all cookies and tokens existing and valid
        states.setOPAllCookiesRemoved(); // after logout, this test expects all OP cookies to be removed
        states.setClientCookieRemovalBasedOnLogoutType();
        states.setAllTokensCleanedUp(); // after logout, this test expects access and refresh tokens to be invalid
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    //    /**
    //    *
    //    */
    //    @ExpectedFFDC({ "java.util.concurrent.CancellationException" })
    //    @Test
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

    @Test
    public void BasicBCLTests_invalidBackchannelLogoutUri() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_invalidBCLUri");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = new TokenKeeper(webClient, response, updatedTestSettings.getFlowType());

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithPostRedirect);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI);

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        // Test has a test app configured for the backchannelLogoutUri - so just he normal end_session steps will be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType()); // init with all cookies and tokens existing and valid
        states.setOPAllCookiesRemoved(); // after logout, this test expects all OP cookies to be removed
        states.setClientCookieRemovalBasedOnLogoutType();
        states.setAllTokensCleanedUp(); // after logout, this test expects access and refresh tokens to be invalid
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    /**
     * Test that we can use an http back channel logout uri with a confidential client - this test should behave the same as the
     * bcl main path test
     *
     * @throws Exception
     */
    @Test
    public void BasicBCLTests_httpBackchannelLogoutUri_confidentialClient() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_http", "bcl_http_confClient", false);

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = new TokenKeeper(webClient, response, updatedTestSettings.getFlowType());

        // logout expectations - just make sure we landed on the end_session/logout page - always with a good status code
        invokeLogout(webClient, updatedTestSettings, initLogoutExpectations(finalAppWithoutPostRedirect), response);

        // Test uses the standard backchannelLogoutUri - so end_session with bcl steps should be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType());

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);
    }

    /**
     * Test that we can't use an http back channel uri with a public client - we should receive an exception when the bcl request
     * is made. The end_session/logout should return success, but errors should be logged in the server logs
     *
     * @throws Exception
     */
    @AllowedFFDC({ "org.apache.http.NoHttpResponseException", "java.util.concurrent.ExecutionException" })
    @Test
    public void BasicBCLTests_httpBackchannelLogoutUri_publicClient() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_http", "bcl_http_publicClient", false);

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = new TokenKeeper(webClient, response, updatedTestSettings.getFlowType());

        // logout expectations - just make sure we landed on the end_session/logout page - always with a good status code
        invokeLogout(webClient, updatedTestSettings, initLogoutExpectations(finalAppWithoutPostRedirect), response);

        // Test uses the standard backchannelLogoutUri - so end_session with bcl steps should be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType());

        // Make sure that all cookies and tokens have been cleaned up
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);
        fail("Public client with http - issue 21372");
    }

    @Test
    public void BasicBCLTests_omittedBackchannelLogoutUri() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_omittedBCLUri");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = new TokenKeeper(webClient, response, updatedTestSettings.getFlowType());

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithPostRedirect);

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        // Test has a test app configured for the backchannelLogoutUri - so just he normal end_session steps will be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType()); // init with all cookies and tokens existing and valid
        states.setOPAllCookiesRemoved(); // after logout, this test expects all OP cookies to be removed
        states.setClientCookieRemovalBasedOnLogoutType();
        states.setAllTokensCleanedUp(); // after logout, this test expects access and refresh tokens to be invalid
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    @Test
    public void BasicBCLTests_backchannelLogoutUri_returns400() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns400");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = new TokenKeeper(webClient, response, updatedTestSettings.getFlowType());

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithPostRedirect);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_400_Servlet.*");

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        // Test has a test app configured for the backchannelLogoutUri - so just he normal end_session steps will be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType()); // init with all cookies and tokens existing and valid
        states.setOPAllCookiesRemoved(); // after logout, this test expects all OP cookies to be removed
        states.setClientCookieRemovalBasedOnLogoutType();
        states.setAllTokensCleanedUp(); // after logout, this test expects access and refresh tokens to be invalid
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    @Test
    public void BasicBCLTests_backchannelLogoutUri_returns501() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns501");

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        TokenKeeper tokens = new TokenKeeper(webClient, response, updatedTestSettings.getFlowType());

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithPostRedirect);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_501_Servlet.*");

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps
        // Test has a test app configured for the backchannelLogoutUri - so just he normal end_session steps will be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(updatedTestSettings.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings.getRsTokenType()); // init with all cookies and tokens existing and valid
        states.setOPAllCookiesRemoved(); // after logout, this test expects all OP cookies to be removed
        states.setClientCookieRemovalBasedOnLogoutType();
        states.setAllTokensCleanedUp(); // after logout, this test expects access and refresh tokens to be invalid
        validateLogoutResult(webClient, updatedTestSettings, tokens, states);

    }

    @Test
    public void BasicBCLTests_backchannelLogoutUri_returnsMultipleDifferentFailures() throws Exception {

        WebClient webClient1 = getAndSaveWebClient(true);
        WebClient webClient2 = getAndSaveWebClient(true);

        TestSettings updatedTestSettings1 = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns400");
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient("OidcConfigSample_invalidBCL", "bcl_returns501");

        Object response1 = accessProtectedApp(webClient1, updatedTestSettings1);
        Object response2 = accessProtectedApp(webClient2, updatedTestSettings2);

        TokenKeeper tokens1 = new TokenKeeper(webClient1, response1, updatedTestSettings1.getFlowType());

        TokenKeeper tokens2 = new TokenKeeper(webClient2, response2, updatedTestSettings2.getFlowType());

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithPostRedirect);
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_400_Servlet.*");
        logoutExpectations = validationTools.addMessageExpectation(testOPServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a there was a problem invoking the back channel logout.", MessageConstants.CWWKS1648E_BACK_CHANNEL_LOGOUT_INVALID_URI + ".*BackChannelLogout_501_Servlet.*");

        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, response1);

        // Make sure that we do NOT need to log in to gain access to the app after we've only logged out on the OP (still have client cookie)
        // test uses a bcl uri that just sleeps (doesn't actually log anything out)
        // Test has a test app configured for the backchannelLogoutUri - so just he normal end_session steps will be performed - set expected states accordingly
        AfterLogoutStates states = new AfterLogoutStates(updatedTestSettings1.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings1.getRsTokenType()); // init with all cookies and tokens existing and valid
        states.setOPAllCookiesRemoved(); // after logout, this test expects all OP cookies to be removed
        states.setClientCookieRemovalBasedOnLogoutType();
        states.setAllTokensCleanedUp(); // after logout, this test expects access and refresh tokens to be invalid
        validateLogoutResult(webClient1, updatedTestSettings1, tokens1, states);

        // check that the other refresh token was disabled
        AfterLogoutStates otherStates = new AfterLogoutStates(updatedTestSettings2.getFlowType(), logoutMethodTested, sessionLogoutEndpoint, updatedTestSettings2.getRsTokenType()); // init with all cookies and tokens existing and valid
        otherStates.setAccessTokenValid(false); // after logout, this test expects access token only to be invalid
        // TODO end_session only revoking the refresh_token for the id_token that was passed
        validateLogoutResult(webClient2, updatedTestSettings2, tokens2, otherStates);

    }

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

        // TODO - I think these checks can be removed - they shouldn't really prove anything
        // TODO - enabled the next two lines when runtime is updated to make multiple calls.
        //        logoutExpectations = addOPLogoutCookieExpectations(logoutExpectations, successfulOPLogout); // OP cookie should be gone
        //        logoutExpectations = addRPLogoutCookieExpectations(logoutExpectations, clientCookieName, fakeJSessionId, unsuccessfulOPLogout); // client cookie should still exist since bcl uri's just return different status and don't log anything out

        // Logout
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, response);

    }

    /**
     * The same user logs in using different clients within the same provider.
     * Invoke end_session passing the id_token as the id_token_hint - show that
     * we'll make a bcl request for each client logged in - the OP will search based on the sub only, but
     * it will build logout_tokens based on the sub and the sid of each login instance.
     *
     * @throws Exception
     */
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
        TokenKeeper keeper1 = new TokenKeeper(webClient1, response1, updatedTestSettings1.getFlowType());
        TokenKeeper keeper2 = new TokenKeeper(webClient2, accessProtectedApp(webClient2, updatedTestSettings2), updatedTestSettings2.getFlowType());
        TokenKeeper keeper3 = new TokenKeeper(webClient3, accessProtectedApp(webClient3, updatedTestSettings3), updatedTestSettings3.getFlowType());
        TokenKeeper keeper4 = new TokenKeeper(webClient4, accessProtectedApp(webClient4, updatedTestSettings4), updatedTestSettings4.getFlowType());

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithoutPostRedirect);
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings1.getClientID() + "*");
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings2.getClientID() + "*");
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings3.getClientID() + "*");
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings4.getClientID() + "*");

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

        Object response1 = accessProtectedApp(webClient1, updatedTestSettings1);
        TokenKeeper keeper1 = new TokenKeeper(webClient1, response1, updatedTestSettings1.getFlowType());
        TokenKeeper keeper2 = new TokenKeeper(webClient2, accessProtectedApp(webClient2, updatedTestSettings2), updatedTestSettings2.getFlowType());
        TokenKeeper keeper3 = new TokenKeeper(webClient3, accessProtectedApp(webClient3, updatedTestSettings3), updatedTestSettings3.getFlowType());
        TokenKeeper keeper4 = new TokenKeeper(webClient4, accessProtectedApp(webClient4, updatedTestSettings4), updatedTestSettings4.getFlowType());

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithoutPostRedirect);
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings1.getClientID() + "*");
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings2.getClientID() + "*");
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings3.getClientID() + "*");
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings4.getClientID() + "*");

        // Logout
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, response1);

        // make sure that the backChannelLogoutUri has been called for each client that we logged in (within the provider that we're logging out)
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings1, keeper1.getSessionId());
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings2, keeper2.getSessionId());
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings3, keeper3.getSessionId());
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings4, keeper4.getSessionId());
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
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient(clientServer, "OidcConfigSample_logger3", "loggerClient3-1", "user1", "user1pwd", false);
        TestSettings updatedTestSettings3 = updateTestSettingsProviderAndClient(clientServer, "OidcConfigSample_logger3", "loggerClient3-1", "user2", "user2pwd", false);

        Object response1 = accessProtectedApp(webClient1, updatedTestSettings1);
        TokenKeeper keeper1 = new TokenKeeper(webClient1, response1, updatedTestSettings1.getFlowType());

        TokenKeeper keeper2 = new TokenKeeper(webClient2, accessProtectedApp(webClient2, updatedTestSettings2), updatedTestSettings2.getFlowType());
        TokenKeeper keeper3 = new TokenKeeper(webClient3, accessProtectedApp(webClient3, updatedTestSettings3), updatedTestSettings3.getFlowType());

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithoutPostRedirect);
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings1.getClientID() + "*");
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings2.getClientID() + "*");
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings3.getClientID() + "*");

        // Logout
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, response1);

        // make sure that the backChannelLogoutUri has been called for each client that we logged in (within the provider that we're logging out)
        List<String> sids = new ArrayList<String>(3);
        sids.add(validateCorrectBCLUrisCalled(clientServer, updatedTestSettings1, keeper1.getSessionId()));
        sids.add(validateCorrectBCLUrisCalled(clientServer, updatedTestSettings2));
        sids.add(validateCorrectBCLUrisCalled(clientServer, updatedTestSettings3));

        Log.info(thisClass, _testName, "Logged out sids: " + Arrays.toString(sids.toArray()));
        if (!sids.contains(keeper1.getSessionId())) {
            fail("If appears that " + updatedTestSettings1.getClientID() + " was not logged out properly - sid: " + keeper1.getSessionId());
        }
        if (sids.contains(keeper2.getSessionId())) {
            fail("If appears that " + updatedTestSettings2.getClientID() + " was not logged out and should not have been - sid: " + keeper2.getSessionId());
        }
        if (sids.contains(keeper3.getSessionId())) {
            fail("If appears that " + updatedTestSettings3.getClientID() + " was not logged out and should not have been - sid: " + keeper3.getSessionId());
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
        TestSettings updatedTestSettings2 = updateTestSettingsProviderAndClient(clientServer, "OidcConfigSample_logger4", "loggerClient4-1", "user1", "user1pwd", false);
        TestSettings updatedTestSettings3 = updateTestSettingsProviderAndClient(clientServer, "OidcConfigSample_logger4", "loggerClient4-1", "user2", "user2pwd", false);

        //        Object response1 = accessProtectedApp(webClient1, updatedTestSettings1);
        TokenKeeper keeper1 = new TokenKeeper(webClient1, accessProtectedApp(webClient1, updatedTestSettings1), updatedTestSettings1.getFlowType());

        TokenKeeper keeper2 = new TokenKeeper(webClient2, accessProtectedApp(webClient2, updatedTestSettings2), updatedTestSettings2.getFlowType());
        TokenKeeper keeper3 = new TokenKeeper(webClient3, accessProtectedApp(webClient3, updatedTestSettings3), updatedTestSettings3.getFlowType());

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithoutPostRedirect);
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings1.getClientID() + "*");
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings2.getClientID() + "*");
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings3.getClientID() + "*");

        // Logout
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, null);

        // make sure that the backChannelLogoutUri has been called for each client that we logged in (within the provider that we're logging out)
        List<String> sids = new ArrayList<String>(3);
        sids.add(validateCorrectBCLUrisCalled(clientServer, updatedTestSettings1, keeper1.getSessionId()));
        sids.add(validateCorrectBCLUrisCalled(clientServer, updatedTestSettings2));
        sids.add(validateCorrectBCLUrisCalled(clientServer, updatedTestSettings3));

        Log.info(thisClass, _testName, "Logged out sids: " + Arrays.toString(sids.toArray()));
        if (!sids.contains(keeper1.getSessionId())) {
            fail("If appears that " + updatedTestSettings1.getClientID() + " was not logged out properly - sid: " + keeper1.getSessionId());
        }
        if (sids.contains(keeper2.getSessionId())) {
            fail("If appears that " + updatedTestSettings2.getClientID() + " was not logged out and should not have been - sid: " + keeper2.getSessionId());
        }
        if (sids.contains(keeper3.getSessionId())) {
            fail("If appears that " + updatedTestSettings3.getClientID() + " was not logged out and should not have been - sid: " + keeper3.getSessionId());
        }

    }

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
        TokenKeeper keeper1 = new TokenKeeper(webClient1, response1, updatedTestSettings1.getFlowType());
        TokenKeeper keeper2 = new TokenKeeper(webClient2, accessProtectedApp(webClient2, updatedTestSettings2), updatedTestSettings2.getFlowType());
        TokenKeeper keeper3 = new TokenKeeper(webClient3, accessProtectedApp(webClient3, updatedTestSettings3), updatedTestSettings3.getFlowType());

        // logout expectations
        List<validationData> logoutExpectations = initLogoutExpectations(finalAppWithoutPostRedirect);
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_client1\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings1.getClientID() + "*");
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_DOES_NOT_MATCH, "Message log contained message indicating that a bcl request was made for client \"nobcl_client1\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings2.getClientID() + "*");
        addToAllowableTimeoutCount(1);
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_client2\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings3.getClientID() + "*");

        // Logout
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, response1);

        // make sure that the backChannelLogoutUri has been called for each client that we logged in (within the provider that we're logging out)
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings1, keeper1.getSessionId());
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings2, false, keeper2.getSessionId());
        validateCorrectBCLUrisCalled(clientServer, updatedTestSettings3, keeper3.getSessionId());
    }

    @ExpectedFFDC({ "org.jose4j.jwt.consumer.InvalidJwtException" })
    @Test
    public void BasicBCLTests_tryToAccessProtectedAppUsingLogoutTokenAsAccessToken() throws Exception {

        restoreAppMap("useLogoutTokenForAccess"); // reset test bcl app
        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = updateTestSettingsProviderAndClient("OidcConfigSample_useLogoutTokenForAccess", "useLogoutTokenForAccess_introspect", false);

        Object response = accessProtectedApp(webClient, updatedTestSettings);

        // logout expectations - just make sure we landed on the post logout redirect page - always with a good status code
        List<validationData> logoutExpectations = initLogoutExpectations(Constants.defaultLogoutPage);

        invokeLogout(webClient, updatedTestSettings, logoutExpectations, response);
        String logoutToken = getLogoutToken("useLogoutTokenForAccess"); // get the logout_token that the test bcl app will return for this test's client

        String action = "TRY_TO_USE_LOGOUT_TOKEN_AS_ACCESS_TOKEN";

        // error messages for when we try to use the logout_token with a client that validates with introspect
        List<validationData> expectations = setUseLogoutTokenAsAccessTokenExpectations(action, updatedTestSettings, Constants.INTROSPECTION_ENDPOINT);

        helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), logoutToken, Constants.HEADER, updatedTestSettings, expectations, action);

        // error messages are slightly different when we try to use the logout_token with a client that validates with userinfo
        updatedTestSettings.setProtectedResource(clientServer.getHttpsString() + "/formlogin/simple/" + "useLogoutTokenForAccess_userinfo");
        expectations = setUseLogoutTokenAsAccessTokenExpectations(action, updatedTestSettings, Constants.USERINFO_ENDPOINT);

        helpers.invokeProtectedResource(_testName, getAndSaveWebClient(true), logoutToken, Constants.HEADER, updatedTestSettings, expectations, action);

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
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings1.getClientID() + "*");
        logoutExpectations = validationTools.addMessageExpectation(clientServer, logoutExpectations, Constants.LOGOUT, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Message log did not contain message indicating that a first bcl request was made for client \"bcl_logsMsg\".", ".*BackChannelLogout_logMsg_Servlet: " + updatedTestSettings2.getClientID() + "*");

        // Logout
        invokeLogout(webClient1, updatedTestSettings1, logoutExpectations, response1);

        checkLogMsgCount(2);
        // reset the counter now that we've completed the end_session - we want the total count after the second end_session
        resetBCLAppCounter();

        Object response2 = accessProtectedApp(webClient3, updatedTestSettings1);
        accessProtectedApp(webClient4, updatedTestSettings2);

        // Logout
        invokeLogout(webClient3, updatedTestSettings1, logoutExpectations, response2);

        checkLogMsgCount(2); // ( should not 4)

    }

    /**
     * invoke end_session on a provider that didn't issue the id_token - make sure failure is clean - right now, it's getting a
     * signature failure.
     **/

    /**
     * Various config attributes that may be affected by, or affect bcl
     *
     * OAuth (OP)
     * userClientTokenLimit
     * revokeAccessTokensWithRefreshTokens
     * accessTokenCacheEnabled
     * Any app_password impact?
     *
     * OIDC (OP)
     *
     * oidc client (RP)
     * accessTokenInLtpaCookie
     * validationMethod
     *
     *
     * oidcclient (Social)
     *
     *
     *
     *
     */
    //    @Test
    //    public void BasicBCLTests_xx() throws Exception {
    //
    //        WebClient webClient = getAndSaveWebClient(true);
    //
    //        invokeGenericForm_refreshToken(_testName,  webClient,  testSettings, refresh_token, expectations) ;
    //    }
    // do and don't invoke back channel logout
}