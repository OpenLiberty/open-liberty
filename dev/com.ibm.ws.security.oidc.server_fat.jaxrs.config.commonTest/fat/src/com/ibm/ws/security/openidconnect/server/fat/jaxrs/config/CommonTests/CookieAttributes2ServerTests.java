/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
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
package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.CommonTests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ApacheJsonUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.AuthorizationHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonCookieTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MultiProviderUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.Utils.CommonTools;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

public class CookieAttributes2ServerTests extends CommonTest {

    //	private static final Class<?> thisClass = CookieAttributes2ServerTests.class;
    private static final Class<?> thisClass = CookieAttributes2ServerTests.class;
    protected static AuthorizationHelpers authHelpers = new AuthorizationHelpers();
    protected static CommonCookieTools cookieTools = new CommonCookieTools();
    protected static CommonValidationTools validationTools = new CommonValidationTools();
    protected static RSCommonTestTools rsTools = new RSCommonTestTools();
    protected static MultiProviderUtils mpUtils = new MultiProviderUtils();
    protected static String targetProvider = null;
    //	protected static String[] goodActions = GET_TOKEN_ACTIONS ;
    protected static String flowType = Constants.WEB_CLIENT_FLOW;
    protected static String OPServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.OPserver";
    protected static String RSServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.RSserver";

    protected static CommonTools commonConfigTools = new CommonTools();
    protected static final String nullString = null;
    protected static final Boolean found = true;
    protected static final Boolean notFound = false;
    protected static final Boolean passToken = true;
    protected static final Boolean doNotPassToken = false;
    protected static final Boolean positiveTest = false;
    protected static final Boolean negativeTest = true;

    private static String inboundPropType = Constants.REQUIRED;
    private static Boolean testWithOPLTPAToken = false;
    private static Boolean ltpaFound, clientFound;
    private static final Boolean expiredTest = true;
    private static final Boolean notExpiredTest = false;
    private static Boolean cfgDisableLTPA, cfgSessionDisabled;
    private static Boolean cookieCheck = true;
    private static int callCount = 0;
    private static String goodTokenToUse = null;
    private static String expiredTokenToUse = null;
    protected static int OIDC_ERROR_1 = 1;

    /**
     * The tests in this class validate the behavior of the RS server as we vary disableLtpaCookie and authnSessionDisabled
     *
     * This test class is run for each of the inboundPropagation settings "none", "supported", "required". Those settings
     * dictate whether the disableLtpaCookie or authnSessionDisabled attribute is used. This test class will also be
     * run with and without use of the OP LTPA cookie to ensure that it is used correctly if it exists.
     * This test class will be used with both an OAuth and OIDC OP - the test will make sure that we succeed/fail
     * as appropriate (as it is different using the 2 different types of OP servers)
     *
     * These tests invoke our protected app - passing or not passing a valid or expired access_token.
     * They will check for success, failure, the correct panel flow as appropriate. They will aslo check for the
     * appropriate cookies (RS LTPA cookie, or client cookie) in the conversation after the requests are made.
     *
     */

    /************************************* General Tooling ****************************************/
    /**
     * Set global flag indicating if this instance tests using the OP's LTPA cookie
     *
     * @param setting
     * @throws Exception
     */
    public static void setTestWithOPLTPAToken(Boolean setting) throws Exception {
        testWithOPLTPAToken = setting;
    }

    /**
     * Sets global flag indicating what type of inboundPropagation we're using - behavior is based on the setting of this config
     * property
     *
     * @param type
     * @throws Exception
     */
    public static void setInboundPropType(String type) throws Exception {
        inboundPropType = type;
    }

    /************************************* Test Case tooling ****************************************/

    /**
     * This is the method that all of the extending classes call to start the servers, initialize test settings and set
     * any other global, up front settings. We have 8 extending classes and they are for the most part the same. The
     * values passed in will support the minor differences
     *
     * @param rs_server_config
     *            - the server.xml file to use for the RS server (op uses the same file for all runs)
     * @param bucketInboundPropType
     *            - how is the inboundPropagation attribute set (none, supported or required)
     * @param certType
     *            TODO
     * @param useOPLTPAToken
     *            - should this instance use the OP LTPA token, or should we remove it from the conversation each time.
     * @param OPPRoviderType
     *            - OP Provider type (OAuth/OIDC)
     * @throws Exception
     */

    // what to pass into this method
    // For testing OIDC, using access_token - cert type can/should be null
    //             OIDC, using id_token - cert type should be x509
    //             OIDC, using jwt token - cert type can be x509 or jwk
    //             ISAM, using jwk token - cert type can be x509 or jwk
    public static void tokenTestingSetup(String rs_server_config, String bucketInboundPropType, String providerType, Boolean useOPLTPAToken) throws Exception {

        String thisMethod = "tokenTestSetup";
        msgUtils.printClassName(thisClass.toString());
        msgUtils.printMethodName(thisMethod);
        Log.info(thisClass, "setupBeforeTest", "Prep for test");
        Log.info(thisClass, "OPProviderType: ", providerType);

        String OPApp;
        String[] propagationTokenTypes = rsTools.chooseTokenSettings(providerType);
        String tokenType = propagationTokenTypes[0];
        String certType = propagationTokenTypes[1];

        if (providerType.contains(Constants.OIDC_OP)) {
            OPApp = Constants.OIDCCONFIGSAMPLE_APP;
        } else {
            OPApp = Constants.OAUTHCONFIGSAMPLE_APP;
        }
        // add any additional messages that you want the "start" to wait for
        // we should wait for any providers that this test requires
        List<String> extraMsgs = new ArrayList<String>();
        extraMsgs.add("CWWKS1631I.*");

        List<String> extraApps = new ArrayList<String>();

        TestServer.addTestApp(null, extraMsgs, Constants.OP_SAMPLE_APP, providerType);
        TestServer.addTestApp(extraApps, null, Constants.OP_CLIENT_APP, providerType);
        TestServer.addTestApp(extraApps, extraMsgs, Constants.OP_TAI_APP, providerType);

        List<String> extraMsgs2 = new ArrayList<String>();

        List<String> extraApps2 = new ArrayList<String>();
        extraApps2.add(Constants.HELLOWORLD_SERVLET);

        //        TestServer.addTestApp(extraApps2, null, Constants.OP_TAI_APP, Constants.OIDC_OP);

        testSettings = new TestSettings();
        // if provider is ISAM or self issue, don't start the OP (TODO - support neiter is currently supported.
        if (!providerType.equals(Constants.ISAM_OP)) {
            testOPServer = commonSetUp(OPServerName, "server_disableLtpaCookie.xml", providerType, extraApps, Constants.DO_NOT_USE_DERBY, extraMsgs, null, providerType, true, true, tokenType, certType);
        }
        genericTestServer = commonSetUp(RSServerName, rs_server_config, Constants.GENERIC_SERVER, extraApps2, Constants.DO_NOT_USE_DERBY, extraMsgs2, null, providerType, true, true, tokenType, certType);

        // We use a variable insert for the validationMethod config attribute which the config evaluator will think is invalid
        genericTestServer.addIgnoredServerException(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".*" + "validationMethod");

        targetProvider = OPApp;
        flowType = Constants.WEB_CLIENT_FLOW;
        //		goodActions = RS_END_TO_END_PROTECTED_RESOURCE_ACTIONS ;

        setInboundPropType(bucketInboundPropType);

        setTestWithOPLTPAToken(useOPLTPAToken);

        // set RS protected resource to point to second server.
        testSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/helloworld");

        createTokens();
    }

    /**
     * Request creation of a good and an expired access_token that will be used throughout this instance of the test class
     *
     * @throws Exception
     */
    protected static void createTokens() throws Exception {

        CookieAttributes2ServerTests theClass = new CookieAttributes2ServerTests();
        theClass.createGoodToken();
        theClass.createExpiredToken();
    }

    /**
     * Create an access_token for use throughout this instance of the test class
     *
     * @throws Exception
     */
    private void createGoodToken() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();

        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/" + "helloworld_disableLTPACookie_Default_authnSessionDisabled_Default");

        List<validationData> getATexpectations = getTokenExpectations(updatedTestSettings);

        WebResponse response = genericOP("createGoodToken", wc, updatedTestSettings, GET_TOKEN_ACTIONS, getATexpectations);
        //		 goodTokenToUse = validationTools.getTokenFromResponse(response, Constants.ACCESS_TOKEN_KEY) ;
        goodTokenToUse = validationTools.getTokenForType(testSettings, response);
        printPropagationToken(testSettings, goodTokenToUse);

    }

    /**
     * Create an expired access_token for use throughout this instance of the test class
     *
     * @throws Exception
     */
    private void createExpiredToken() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();

        if (eSettings.getProviderType().equals(Constants.OAUTH_OP)) {
            updatedTestSettings = mpUtils.copyAndOverrideProviderSettings(updatedTestSettings, Constants.OAUTHCONFIGSAMPLE_APP, "OAuthConfigSample2", Constants.SNIFFING);
            updatedTestSettings.setProtectedResource(eSettings.assembleProtectedResource(testOPServer.getHttpsString(), updatedTestSettings.getConfigTAI(), Constants.SNIFFING));
        } else {
            updatedTestSettings = mpUtils.copyAndOverrideProviderSettings(updatedTestSettings, Constants.OIDCCONFIGSAMPLE_APP, "OidcConfigSample2", Constants.SNIFFING);
            updatedTestSettings.setProtectedResource(eSettings.assembleProtectedResource(testOPServer.getHttpsString(), updatedTestSettings.getConfigTAI(), Constants.SNIFFING));
        }
        updatedTestSettings.setClientName("client06");
        updatedTestSettings.setClientID("client06");

        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/" + "helloworld_expired_disableLTPACookie_Default_authnSessionDisabled_Default");

        List<validationData> getATexpectations = getTokenExpectations(updatedTestSettings);

        WebResponse response = genericOP("createExpiredToken", wc, updatedTestSettings, GET_TOKEN_ACTIONS, getATexpectations);
        //		 expiredAccessToken = validationTools.getTokenFromResponse(response, Constants.ACCESS_TOKEN_KEY) ;
        expiredTokenToUse = validationTools.getTokenForType(testSettings, response);
        printPropagationToken(testSettings, expiredTokenToUse);

        // expire the token
        helpers.testSleep(10);

    }

    private void printPropagationToken(TestSettings settings, String propagationToken) throws Exception {
        String thisMethod = "printPropagationToken";
        if (settings.getRsTokenType().equals(Constants.JWT_TOKEN)) {
            Log.info(thisClass, thisMethod, "Full id_token : " + propagationToken);
            String[] id_token_parts = propagationToken.split("\\.");
            for (String s : id_token_parts) {
                Log.info(thisClass, thisMethod, "id_token part : " + s);

                Log.info(thisClass, thisMethod, "Decoded tokenPart : " + ApacheJsonUtils.fromBase64StringToJsonString(s));
            }

        } else {
            Log.info(thisClass, thisMethod, "Full access_token : " + propagationToken);
        }
    }

    /**
     * helper method to set a global flag to true to indicate that we should be checking cookies
     */
    protected void setCookieCheckOn() {
        cookieCheck = true;
    }

    /**
     * helper method to set a global flag to false to indicate that we should not be checking cookies
     */
    private void setCookieCheckOff() {
        cookieCheck = false;
    }

    /**
     * Validates the existance of the correct cookies. Parameters are passed that indicate if the LTPA and/or client cookies
     * should be found
     *
     * @param wc
     *            - the current converstation
     * @param shouldLTPABeFound
     *            - true/false indicating if the RS LTAP token should be found in the conversation
     * @param shouldClientBeFound
     *            - true/false indicating if the client cookie should be found in the conversation
     * @throws Exception
     */
    protected void cookieValidation(WebConversation wc, Boolean shouldLTPABeFound, Boolean shouldClientBeFound) throws Exception {

        String theMethod = "cookieValidation";
        msgUtils.printMethodName(theMethod);

        msgUtils.printAllCookies(wc);
        if (!cookieCheck) {
            Log.info(thisClass, theMethod, "cookie check is turned off - skipping cookie check");
            return;
        }

        String foundValue = cookieTools.getCookieValue(wc, Constants.RS_COOKIE);
        if (shouldLTPABeFound) {
            msgUtils.assertTrueAndLog(theMethod, "RS LTPA Cookie was NOT found in conversation and should have been", (foundValue != null));
        } else {
            msgUtils.assertTrueAndLog(theMethod, "RS LTPA Cookie found in conversation and should NOT have been", (foundValue == null));
        }

        foundValue = cookieTools.getCookieName(wc, Constants.CLIENT_COOKIE);
        if (shouldClientBeFound) {
            msgUtils.assertTrueAndLog(theMethod, "Client Cookie was NOT found in conversation and should have been", (foundValue != null));
        } else {
            msgUtils.assertTrueAndLog(theMethod, "Client Cookie found in conversation and should NOT have been", (foundValue == null));
        }
    }

    /**
     * Sets flags indicating if the RS LTPA and/or client cookie should be found in the current conversation
     *
     * @param disableLTPA
     *            - true/false indicating if current config has disableLtpaCookie set to true/false
     * @param sessionDisabled
     *            - true/false indicating if current config has authnSessionDisabled set to true/false
     * @throws Exception
     */
    public static void setCookieExpectations(Boolean disableLTPA, Boolean sessionDisabled) throws Exception {
        setCookieExpectations(disableLTPA, sessionDisabled, false);
    }

    /**
     * Sets flags indicating if the RS LTPA and/or client cookie should be found in the current conversation
     * The config settings and existance of a good access_token (in some cases) dictate what cookies to expect
     *
     * @param disableLTPA
     *            - true/false indicating if current config has disableLtpaCookie set to true/false
     * @param sessionDisabled
     *            - true/false indicating if current config has authnSessionDisabled set to true/false
     * @param goodToken
     *            - true/false indicating if the access_token is good (valid/passed)/bad (expired/not passed)
     * @throws Exception
     */
    public static void setCookieExpectations(Boolean disableLTPA, Boolean sessionDisabled, Boolean goodToken) throws Exception {

        cfgDisableLTPA = disableLTPA;
        cfgSessionDisabled = sessionDisabled;

        if (inboundPropType.equals(Constants.NONE)) {
            if (disableLTPA) {
                ltpaFound = notFound;
                clientFound = found;
            } else {
                ltpaFound = found;
                clientFound = notFound;
            }
        }
        if (inboundPropType.equals(Constants.SUPPORTED)) {
            if (disableLTPA) {
                if (!goodToken) {
                    ltpaFound = notFound;
                    clientFound = found;
                } else {
                    ltpaFound = notFound;
                    clientFound = notFound;
                }
            } else {
                if (!goodToken) {
                    ltpaFound = found;
                } else {
                    ltpaFound = notFound;
                }
                clientFound = notFound;
            }
        }
        if (inboundPropType.equals(Constants.REQUIRED)) {
            if (sessionDisabled || !goodToken) {
                ltpaFound = notFound;
            } else {
                ltpaFound = found;
            }
            clientFound = notFound;
        }

    }

    /**
     * Sets expectations for the current test
     * Routine sets what to expect for this test case based on
     * 1) with a good result: which url's will need to be invoked (will we need to use the login page?)
     * 2) with a bad result: did we test with an expired token (we'll get to the OP in this case and need to look for messages
     * there (then which endpoint are we using as the OP messages will be different)
     * 3) if we're using an OAuth server instead of an OIDC, we can't login - any tests that need a login will fail at this point
     * (unless we've already failed, then we'll fail as soon as we
     * try to invoke the app cue to an expired state cookie in the conversation.
     *
     * @param wc
     *            - the conversation
     * @param settings
     *            - the test settings for this test case
     * @param passToken
     *            - will we be passing the token
     * @param negativeTest
     *            - is this a test that expects a bad result
     * @param expiredToken
     *            - is the token used expired
     * @return expectations for use with the main tooking (url's are invoked and the expectations list what should be checked)
     * @throws Exception
     */
    private List<validationData> setMyExpectations(WebConversation wc, TestSettings settings, Boolean passToken, Boolean negativeTest, Boolean expiredToken) throws Exception {
        String thisMethod = "setMyExpectations";

        String finalAction = Constants.INVOKE_RS_PROTECTED_RESOURCE;

        /******************************************/
        /*
         * create good expectations and decide what the final action will be 1) INVOKE_RS_PROTECTED_RESOURCE or 2) LOGIN_USER
         * We'll need to determine if the login page is needed - if so, set finalAction to that otherwise we'll just need to
         * hit the protected app
         */
        List<validationData> expectations;
        // start by setting good expectations - may be replaced if this is a negative test
        expectations = vData.addSuccessStatusCodes();
        // TODO - with new flow for ISAM - this will have to be updated
        // the steps/actions will be different
        if (willWeNeedToLogin(wc, passToken, expiredToken)) {
            finalAction = LOGIN_USER;
            //			 expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
            expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        }
        expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the HelloWorld App", null, "Accessed Hello World!");
        expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found UnAuthenticated in the App output", null, Constants.HELLOWORLD_UNAUTHENTICATED);

        /******************************************/
        /*
         * if this is a negative test, replace expectations with negative ones...
         * The finalAction is still the same as what we determined above
         */
        if (negativeTest) {
            expectations = validationTools.add401Responses(finalAction);
            // expired tokens get the OP server involved - so look for messages there as well as in the RS
            if (!expiredToken) {
                if (!Constants.SUPPORTED.equals(settings.getInboundProp())) {
                    // iss 3710 // expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating that token was expired.", MessageConstants.CWWKS1726E_MISSING_PROPAGATION_TOKEN);
                }
            } else {
                if (!Constants.SUPPORTED.equals(settings.getInboundProp())) { // 212457
                    if (settings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
                        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "RS message log did not contain message indicating that token was expired.", MessageConstants.CWWKS1720E_ACCESS_TOKEN_NOT_ACTIVE);
                        if (genericTestServer.getRSValidationType().equals(Constants.USERINFO_ENDPOINT)) {
                            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "RS message log did not contain message indicating that token was expired.", MessageConstants.CWWKS1721E_OIDC_REQ_FAILED_ACCESS_TOKEN_NOT_VALID + ".*" + MessageConstants.CWWKS1617E_USERINFO_REQUEST_BAD_TOKEN);
                        }
                    } else {
                        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Message log did not contain message indicating the client failed to validate the ID token.", MessageConstants.CWWKS1773E_TOKEN_EXPIRED);
                    }
                }
                if (genericTestServer.getRSValidationType().equals(Constants.INTROSPECTION_ENDPOINT)) {
                    expectations = rsTools.setExpectationForAccessTokenOnly(testOPServer, expectations, settings, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "OP message log did not contain message indicating that token was expired", MessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
                } else {
                    expectations = rsTools.setExpectationForAccessTokenOnly(testOPServer, expectations, settings, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "OP message log did not contain message indicating that token was expired", MessageConstants.CWWKS1617E_USERINFO_REQUEST_BAD_TOKEN);
                }
            }
        }

        /******************************************/
        /*
         * if we're using ouath and don't have a good access_token, we can't use the login page from
         * the OP. (OAuth doesn't support using an RP/RS server) We need override expectations again as appropriate
         */
        if (inValidOAuthRequest(passToken && !expiredToken)) {
            Log.info(thisClass, thisMethod, "callCount: " + callCount);
            expectations = validationTools.add401Responses(finalAction);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not fail when trying to use an OAuth provider.", MessageConstants.CWWKS1712E_ID_TOKEN_MISSING);
            if (expiredToken && !Constants.NONE.equals(settings.getInboundProp())) {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find message in RS log saying the token was not valid.", MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP);
                expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find message in OP log saying the access token is not valid or expired.", MessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
            }
            setCookieCheckOff();
        }
        if (passToken && !negativeTest && expiredToken && callCount == 0) {
            if (expiredToken && !Constants.NONE.equals(settings.getInboundProp())) {
                expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find message in RS log saying the token was not valid.", MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP);
                if (settings.getRsTokenType().equals(Constants.JWT_TOKEN)) {
                    expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find message in RS log saying the OIDC client failed to validate the JWT.", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE + ".*" + MessageConstants.CWWKS1773E_TOKEN_EXPIRED);
                } else {
                    if (genericTestServer.getRSValidationType().equals(Constants.INTROSPECTION_ENDPOINT)) {
                        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find message in OP log saying the access token is not valid or expired.", MessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
                    } else {
                        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find message in OP log saying the access token is not valid or expired.", MessageConstants.CWWKS1617E_USERINFO_REQUEST_BAD_TOKEN);
                    }
                }
            }
        }

        msgUtils.printOAuthOidcExpectations(expectations, settings);
        return expectations;

    }

    //TODO when we have an ISAM server, the login step name may change
    /**
     * Set the expectations for obtaining a valid token (nothing fancy here - just include checks to make sure we get an
     * authorization code and then the token and refresh_token
     *
     * @param settings
     *            - current test settings
     * @return - expectations for obtaining a valid token
     * @throws Exception
     */
    private static List<validationData> getTokenExpectations(TestSettings settings) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes();

        expectations = vData.addExpectation(expectations, PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null, Constants.RECV_AUTH_CODE);
        expectations = vData.addExpectation(expectations, PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access_token and refresh_token", null, Constants.RECV_FROM_TOKEN_ENDPOINT);

        return expectations;
    }

    /**
     * Determine if test will need to go to the login page for this run.
     * We can skip the page if we have any of the LTPA or client cookies, and the inboundPropagation type allows the runtime to
     * look at those.
     * OR, if we have a valid token and the inboundPropagation type allows the runtime to use that token
     *
     * @param wc
     *            - the converstation (to determine which cookies are available)
     * @param passToken
     *            - are we passing the token
     * @param useExpiredToken
     *            - is the token expired
     * @return - returns true - we need to use the login page, false - we do NOT need to use the login page
     * @throws Exception
     */
    private Boolean willWeNeedToLogin(WebConversation wc, Boolean passToken, Boolean useExpiredToken) throws Exception {
        Boolean startedWithOPLTPA = cookieTools.getCookieValue(wc, Constants.OP_COOKIE) != null;
        Boolean startedWithRSLTPA = cookieTools.getCookieValue(wc, Constants.RS_COOKIE) != null;
        Boolean startedWithCLCookie = cookieTools.getCookieName(wc, Constants.CLIENT_COOKIE) != null;
        Boolean willNeedToLogin = true;

        Log.info(thisClass, "willWeNeedToLogin", "propType: " + inboundPropType + " cfgDisableLTPA: " + cfgDisableLTPA.toString() + " cfgSessionDisabled: " + cfgSessionDisabled.toString());
        Log.info(thisClass, "willWeNeedToLogin", "startedWithOPLTPA: " + startedWithOPLTPA.toString() + " startedWithRSLTPA: " + startedWithRSLTPA.toString() + " startedWithCLCookie: " + startedWithCLCookie.toString());

        if (inboundPropType.equals(Constants.NONE)) {
            if (cfgDisableLTPA) {
                if (startedWithOPLTPA || (startedWithCLCookie)) {
                    willNeedToLogin = false;
                }
            } else {
                if (startedWithOPLTPA || startedWithRSLTPA) {
                    willNeedToLogin = false;
                }
            }
        }
        if (inboundPropType.equals(Constants.SUPPORTED)) {
            if (startedWithOPLTPA || startedWithRSLTPA || startedWithCLCookie) {
                willNeedToLogin = false;
            } else {
                if (passToken && !useExpiredToken) {
                    willNeedToLogin = false;
                    //					if (useExpiredToken) {
                    //						willNeedToLogin = true ;
                    //					}
                }
            }

        }
        if (inboundPropType.equals(Constants.REQUIRED)) {
            // for required, we must always have an token, so never need to login
            willNeedToLogin = false;
        }
        Log.info(thisClass, "willWeNeedToLogin", willNeedToLogin.toString());
        return willNeedToLogin;
    }

    /**
     * If we're testing with an OAuth OP instead an OIDC OP, some things won't work (others will)
     * Check if we're using an OAuth OP and if we're using None or Supported inboundProp - we can't have the OP grant access in
     * these cases
     *
     * @param goodToken
     *            - is the token good (is it passed and if it is was it expired)
     * @return - true (this is an invalid OAuth request), false (this is a valid request)
     * @throws Exception
     */
    private Boolean inValidOAuthRequest(Boolean goodToken) throws Exception {
        Log.info(thisClass, "inValidOAuthRequest", "providerType: " + eSettings.getProviderType() + " inboundPropType: " + inboundPropType + " goodToken: " + goodToken);
        if (eSettings.getProviderType().equals(Constants.OAUTH_OP)) {
            // when the RS is actually acting like and RP, we can't use an OAuth OP - we'll get a 401 and a CWWKS1712E message
            if (inboundPropType.equals(Constants.NONE) || ((inboundPropType.equals(Constants.SUPPORTED) && !goodToken))) {
                Log.info(thisClass, "inValidOAuthRequest", "Setting invalid OAuth to true");
                return true;
            }
        }
        return false;
    }

    /**
     * Invoke the test app with or without the good or expired token. This method will also process the login page if/when needed
     *
     * @param wc
     *            - the test conversation for this test instance
     * @param settings
     *            - test case settings
     * @param passToken
     *            - should the token be passed
     * @param expiredToken
     *            - should we use the expired token
     * @param expectations
     *            - what the test should expect
     * @return - the response from the last call (shouldn't be needed, but pass it back in case caller needs it)
     * @throws Exception
     */
    protected WebResponse invokeAppAsAppropriate(WebConversation wc, TestSettings settings, Boolean passToken, Boolean expiredToken, List<validationData> expectations) throws Exception {

        WebResponse response = null;
        String theMethod = "invokeAppAsAppropriate";
        Log.info(thisClass, theMethod, "passToken: " + passToken.toString() + " expiredToken: " + expiredToken.toString());

        Boolean willNeedToLogin = willWeNeedToLogin(wc, passToken, expiredToken);

        // add the correct token to the request if appropriate
        HashMap<String, String[]> headers = null;
        String useThisToken = null;
        if (passToken) {
            if (expiredToken) {
                useThisToken = expiredTokenToUse;
            } else {
                useThisToken = goodTokenToUse;
            }
        }
        if (useThisToken != null) {
            headers = helpers.setupHeadersMap(settings, useThisToken);
        }

        // invoke the protected app - check for success/failure/login panel as appropriate
        response = helpers.invokeRSProtectedResource(_testName, wc, Constants.GETMETHOD, headers, null, settings, expectations);

        // if this flow requires a login, login and then validate expectations
        if (willNeedToLogin) {
            settings.setUserParm(Constants.OIDC_USERPARM);
            settings.setPassParm(Constants.OIDC_PASSPARM);
            settings.setUserName(Constants.OIDC_USERNAME);
            settings.setUserPassword(Constants.OIDC_USERPASSWORD);
            helpers.processProviderLoginForm(_testName, wc, response, settings, expectations);
        }

        return response;
    }

    /**
     * Runs a positive, not expired test - calls tokenTest passing passed parms and hard coding positiveTest and notExpiredTest
     *
     * @param appToRun
     *            - the app to run
     * @param passToken
     *            - test will pass the token
     * @param isLTPACookie
     *            - is ltpa cookie enabled/disabled
     * @param isClientCookie
     *            - is client cookie enabled/disabled
     * @throws Exception
     */
    private void positiveTokenTest(String appToRun, Boolean passToken, Boolean isLTPACookie, Boolean isClientCookie) throws Exception {
        tokenTest(appToRun, passToken, isLTPACookie, isClientCookie, positiveTest, notExpiredTest);
    }

    /**
     * Runs a negative, not expired test - calls tokenTest passing passed parms and hard coding negativeTest and notExpiredTest
     *
     * @param appToRun
     *            - the app to run
     * @param passToken
     *            - test will pass the token
     * @param isLTPACookie
     *            - is ltpa cookie enabled/disabled
     * @param isClientCookie
     *            - is client cookie enabled/disabled
     * @throws Exception
     */
    private void negativeTokenTest(String appToRun, Boolean passToken, Boolean isLTPACookie, Boolean isClientCookie) throws Exception {
        tokenTest(appToRun, passToken, isLTPACookie, isClientCookie, negativeTest, notExpiredTest);
    }

    /**
     * Runs a positive, expired test - calls tokenTest passing passed parms and hard coding positiveTest and expiredTest
     *
     * @param appToRun
     *            - the app to run
     * @param passToken
     *            - test will pass the token
     * @param isLTPACookie
     *            - is ltpa cookie enabled/disabled
     * @param isClientCookie
     *            - is client cookie enabled/disabled
     * @throws Exception
     */
    private void positiveTokenTestExpiredToken(String appToRun, Boolean isLTPACookie, Boolean isClientCookie) throws Exception {
        tokenTest(appToRun, passToken, isLTPACookie, isClientCookie, positiveTest, expiredTest);
    }

    /**
     * Runs a negative, expired test - calls tokenTest passing passed parms and hard coding negativeTest and expiredTest
     *
     * @param appToRun
     *            - the app to run
     * @param passToken
     *            - test will pass the token
     * @param isLTPACookie
     *            - is ltpa cookie enabled/disabled
     * @param isClientCookie
     *            - is client cookie enabled/disabled
     * @throws Exception
     */
    private void negativeTokenTestExpiredToken(String appToRun, Boolean isLTPACookie, Boolean isClientCookie) throws Exception {
        tokenTest(appToRun, passToken, isLTPACookie, isClientCookie, negativeTest, expiredTest);
    }

    /**
     * Runs the steps of the tests based on the parms passed
     *
     * @param appToRun
     *            - the app this testcase will invoke
     * @param passToken
     *            - should we pass the token
     * @param isLTPACookie
     *            - should we get an RS LTPA token after invoking
     * @param isClientCookie
     *            - should we get a Client cookie after invoking
     * @param negativeResult
     *            - should we expect a failure
     * @param expiredToken
     *            - if passing an token, should it be expired
     * @throws Exception
     */
    private void tokenTest(String appToRun, Boolean passToken, Boolean isLTPACookie, Boolean isClientCookie, Boolean negativeResult, Boolean expiredToken) throws Exception {

        Log.info(thisClass, "tokenTest", "Good Access Token: " + goodTokenToUse);
        Log.info(thisClass, "tokenTest", "Expired Access Token: " + expiredTokenToUse);
        WebConversation wc = new WebConversation();
        setCookieCheckOn();
        callCount = 0;

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        if (!passToken) {
            updatedTestSettings.setWhere("notSet");
        }
        updatedTestSettings.setRSProtectedResource(genericTestServer.getHttpsString() + "/helloworld/rest/" + appToRun);

        List<validationData> expectations = setMyExpectations(wc, updatedTestSettings, passToken, negativeResult, expiredToken);
        invokeAppAsAppropriate(wc, updatedTestSettings, passToken, expiredToken, expectations);

        // make sure we find the appropriate cookies in the conversation
        cookieValidation(wc, isLTPACookie, isClientCookie);

        callCount = 1;

        // some flows result in a state cookie that is removed from the server and marked expired in the conversation
        // httpunit does not clear that expired cookie - subsequent calls made to the RS/RP server with that
        // expired cookie result in a state mismatch failure  (a browser and newer httpunit code should delete that
        // expirec cookie - so, for now, hack around it and remove it ourselves.
        cookieTools.clearExpiredCookies(wc);

        // Having the OP LTPA cookie affects behavior of some inboundProp flows - we'll test with/without
        // it - if testing without it, we need to remove it from the conversation
        if (!testWithOPLTPAToken) {
            cookieTools.removeCookieFromConverstation(wc, Constants.OP_COOKIE);
            msgUtils.printAllCookies(wc);
        }
        String oidcCodeValue = wc.getCookieValue("WASOidcCode");
        if (oidcCodeValue != null && !oidcCodeValue.isEmpty()) {
            Log.info(thisClass, "tokenTest", "WASOidcCode:" + oidcCodeValue);
            // Due to a bug in httpUnit. (WHen it failed,, such as: 401, the new cookies are not updated)
            wc.putCookie("WASOidcCode", ""); // since the WASOidcCode is used no matter it failed or succeed
        }

        // will invoke again - testing to make sure that we use the cookies/tokens if they were generated
        // on the first request.
        expectations = setMyExpectations(wc, updatedTestSettings, passToken, negativeResult, expiredToken);
        invokeAppAsAppropriate(wc, updatedTestSettings, passToken, expiredToken, expectations);

        // make sure we find the appropriate cookies in the conversation
        cookieValidation(wc, isLTPACookie, isClientCookie);

    }

    /************************************* Test Cases ****************************************/

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = default (true)
     * pass the Valid token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_default_authnSessionDisabled_default_includeToken() throws Exception {

        setCookieExpectations(false, true, true);
        positiveTokenTest("helloworld_disableLTPACookie_Default_authnSessionDisabled_Default", passToken, ltpaFound, clientFound);
    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = true
     * authnSessionDisabled = default (true)
     * pass the Valid token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_true_authnSessionDisabled_default_includeToken() throws Exception {

        setCookieExpectations(true, true, true);
        positiveTokenTest("helloworld_disableLTPACookie_True_authnSessionDisabled_Default", passToken, ltpaFound, clientFound);

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = false
     * authnSessionDisabled = default (true)
     * pass the Valid token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_false_authnSessionDisabled_default_includeToken() throws Exception {

        setCookieExpectations(false, true, true);
        positiveTokenTest("helloworld_disableLTPACookie_False_authnSessionDisabled_Default", passToken, ltpaFound, clientFound);

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = default (true)
     * omit the token
     * Expected behaviour:
     * We should NOT have access to the protected app
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_default_authnSessionDisabled_default_omitToken() throws Exception {

        setCookieExpectations(false, true, false);
        if (inboundPropType.equals(Constants.REQUIRED)) {
            negativeTokenTest("helloworld_disableLTPACookie_Default_authnSessionDisabled_Default", doNotPassToken, ltpaFound, clientFound);
        } else {
            positiveTokenTest("helloworld_disableLTPACookie_Default_authnSessionDisabled_Default", doNotPassToken, ltpaFound, clientFound);
        }

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = true
     * authnSessionDisabled = default (true)
     * omit the token
     * Expected behaviour:
     * We should NOT have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_true_authnSessionDisabled_default_omitToken() throws Exception {

        setCookieExpectations(true, true, false);
        if (inboundPropType.equals(Constants.REQUIRED)) {
            negativeTokenTest("helloworld_disableLTPACookie_True_authnSessionDisabled_Default", doNotPassToken, ltpaFound, clientFound);
        } else {
            positiveTokenTest("helloworld_disableLTPACookie_True_authnSessionDisabled_Default", doNotPassToken, ltpaFound, clientFound);
        }

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = default (true)
     * omit the token
     * Expected behaviour:
     * We should NOT have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_false_authnSessionDisabled_default_omitToken() throws Exception {

        setCookieExpectations(false, true, false);
        if (inboundPropType.equals(Constants.REQUIRED)) {
            negativeTokenTest("helloworld_disableLTPACookie_False_authnSessionDisabled_Default", doNotPassToken, ltpaFound, clientFound);
        } else {
            positiveTokenTest("helloworld_disableLTPACookie_False_authnSessionDisabled_Default", doNotPassToken, ltpaFound, clientFound);
        }

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = default (true)
     * pass an expired access_token
     * Expected behaviour:
     * We should NOT have access to the protected app
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_default_authnSessionDisabled_default_expiredToken() throws Exception {

        setCookieExpectations(false, true, false);
        if (inboundPropType.equals(Constants.REQUIRED)) {
            negativeTokenTestExpiredToken("helloworld_expired_disableLTPACookie_Default_authnSessionDisabled_Default", ltpaFound, clientFound);
        } else {
            positiveTokenTestExpiredToken("helloworld_expired_disableLTPACookie_Default_authnSessionDisabled_Default", ltpaFound, clientFound);
        }

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = default (true)
     * pass an expired access_token
     * Expected behaviour:
     * We should NOT have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_true_authnSessionDisabled_default_expiredToken() throws Exception {

        setCookieExpectations(true, true, false);
        if (inboundPropType.equals(Constants.REQUIRED)) {
            negativeTokenTestExpiredToken("helloworld_expired_disableLTPACookie_True_authnSessionDisabled_Default", ltpaFound, clientFound);
        } else {
            positiveTokenTestExpiredToken("helloworld_expired_disableLTPACookie_True_authnSessionDisabled_Default", ltpaFound, clientFound);
        }

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = default (true)
     * pass an expired access_token
     * Expected behaviour:
     * We should NOT have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_false_authnSessionDisabled_default_expiredToken() throws Exception {

        setCookieExpectations(false, true, false);
        if (inboundPropType.equals(Constants.REQUIRED)) {
            negativeTokenTestExpiredToken("helloworld_expired_disableLTPACookie_False_authnSessionDisabled_Default", ltpaFound, clientFound);
        } else {
            positiveTokenTestExpiredToken("helloworld_expired_disableLTPACookie_False_authnSessionDisabled_Default", ltpaFound, clientFound);
        }

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = true
     * pass the Valid access_token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_default_authnSessionDisabled_true_includeToken() throws Exception {

        setCookieExpectations(false, true, true);
        positiveTokenTest("helloworld_disableLTPACookie_Default_authnSessionDisabled_True", passToken, ltpaFound, clientFound);
    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = true
     * authnSessionDisabled = true
     * pass the Valid access_token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_true_authnSessionDisabled_true_includeToken() throws Exception {

        setCookieExpectations(true, true, true);
        positiveTokenTest("helloworld_disableLTPACookie_True_authnSessionDisabled_True", passToken, ltpaFound, clientFound);

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = false
     * authnSessionDisabled = true
     * pass the Valid access_token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_false_authnSessionDisabled_true_includeToken() throws Exception {

        setCookieExpectations(false, true, true);
        positiveTokenTest("helloworld_disableLTPACookie_False_authnSessionDisabled_True", passToken, ltpaFound, clientFound);

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = true
     * omit the access_token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_default_authnSessionDisabled_true_omitToken() throws Exception {

        setCookieExpectations(false, true, false);
        if (inboundPropType.equals(Constants.REQUIRED)) {
            negativeTokenTest("helloworld_disableLTPACookie_Default_authnSessionDisabled_True", doNotPassToken, ltpaFound, clientFound);
        } else {
            positiveTokenTest("helloworld_disableLTPACookie_Default_authnSessionDisabled_True", doNotPassToken, ltpaFound, clientFound);
        }

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = true
     * omit the access_token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    // will change to a positive case when authnSessionDisabled behavior is fixed
    public void CookieAttributes2ServerTests_disableLTPACookie_true_authnSessionDisabled_true_omitToken() throws Exception {

        setCookieExpectations(true, true, false);
        if (inboundPropType.equals(Constants.REQUIRED)) {
            negativeTokenTest("helloworld_disableLTPACookie_True_authnSessionDisabled_True", doNotPassToken, ltpaFound, clientFound);
        } else {
            positiveTokenTest("helloworld_disableLTPACookie_True_authnSessionDisabled_True", doNotPassToken, ltpaFound, clientFound);
        }

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = true
     * omit the access_token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_false_authnSessionDisabled_true_omitToken() throws Exception {

        setCookieExpectations(false, true, false);
        if (inboundPropType.equals(Constants.REQUIRED)) {
            negativeTokenTest("helloworld_disableLTPACookie_False_authnSessionDisabled_True", doNotPassToken, ltpaFound, clientFound);
        } else {
            positiveTokenTest("helloworld_disableLTPACookie_False_authnSessionDisabled_True", doNotPassToken, ltpaFound, clientFound);
        }

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = true
     * pass an expired access_token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_default_authnSessionDisabled_true_expiredToken() throws Exception {

        setCookieExpectations(false, true, false);
        if (inboundPropType.equals(Constants.REQUIRED)) {
            negativeTokenTestExpiredToken("helloworld_expired_disableLTPACookie_Default_authnSessionDisabled_True", ltpaFound, clientFound);
        } else {
            positiveTokenTestExpiredToken("helloworld_expired_disableLTPACookie_Default_authnSessionDisabled_True", ltpaFound, clientFound);
        }

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = true
     * pass an expired access_token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_true_authnSessionDisabled_true_expiredToken() throws Exception {

        setCookieExpectations(true, true, false);
        if (inboundPropType.equals(Constants.REQUIRED)) {
            negativeTokenTestExpiredToken("helloworld_expired_disableLTPACookie_True_authnSessionDisabled_True", ltpaFound, clientFound);
        } else {
            positiveTokenTestExpiredToken("helloworld_expired_disableLTPACookie_True_authnSessionDisabled_True", ltpaFound, clientFound);
        }

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = true
     * pass an expired access_token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_false_authnSessionDisabled_true_expiredToken() throws Exception {

        setCookieExpectations(false, true, false);
        if (inboundPropType.equals(Constants.REQUIRED)) {
            negativeTokenTestExpiredToken("helloworld_expired_disableLTPACookie_False_authnSessionDisabled_True", ltpaFound, clientFound);
        } else {
            positiveTokenTestExpiredToken("helloworld_expired_disableLTPACookie_False_authnSessionDisabled_True", ltpaFound, clientFound);
        }

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = false
     * pass the Valid access_token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_default_authnSessionDisabled_false_includeToken() throws Exception {

        setCookieExpectations(false, false, true);
        positiveTokenTest("helloworld_disableLTPACookie_Default_authnSessionDisabled_False", passToken, ltpaFound, clientFound);
    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = true
     * authnSessionDisabled = false
     * pass the Valid access_token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_true_authnSessionDisabled_false_includeToken() throws Exception {

        setCookieExpectations(true, false, true);
        positiveTokenTest("helloworld_disableLTPACookie_True_authnSessionDisabled_False", passToken, ltpaFound, clientFound);

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = false
     * authnSessionDisabled = false
     * pass the Valid access_token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_false_authnSessionDisabled_false_includeToken() throws Exception {

        setCookieExpectations(false, false, true);
        positiveTokenTest("helloworld_disableLTPACookie_False_authnSessionDisabled_False", passToken, ltpaFound, clientFound);

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = false
     * omit the access_token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_default_authnSessionDisabled_false_omitToken() throws Exception {

        setCookieExpectations(false, false, false);
        if (inboundPropType.equals(Constants.REQUIRED)) {
            negativeTokenTest("helloworld_disableLTPACookie_Default_authnSessionDisabled_False", doNotPassToken, ltpaFound, clientFound);
        } else {
            positiveTokenTest("helloworld_disableLTPACookie_Default_authnSessionDisabled_False", doNotPassToken, ltpaFound, clientFound);
        }
    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = false
     * omit the access_token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_true_authnSessionDisabled_false_omitToken() throws Exception {

        setCookieExpectations(true, false, false);
        if (inboundPropType.equals(Constants.REQUIRED)) {
            negativeTokenTest("helloworld_disableLTPACookie_True_authnSessionDisabled_False", doNotPassToken, ltpaFound, clientFound);
        } else {
            positiveTokenTest("helloworld_disableLTPACookie_True_authnSessionDisabled_False", doNotPassToken, ltpaFound, clientFound);
        }
    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = false
     * omit the access_token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_false_authnSessionDisabled_false_omitToken() throws Exception {

        setCookieExpectations(false, false, false);
        if (inboundPropType.equals(Constants.REQUIRED)) {
            negativeTokenTest("helloworld_disableLTPACookie_False_authnSessionDisabled_False", doNotPassToken, ltpaFound, clientFound);
        } else {
            positiveTokenTest("helloworld_disableLTPACookie_False_authnSessionDisabled_False", doNotPassToken, ltpaFound, clientFound);
        }
    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = false
     * pass an expired access_token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_default_authnSessionDisabled_false_expiredToken() throws Exception {

        // suspect supported will behave more like required
        setCookieExpectations(false, false, false);
        if (inboundPropType.equals(Constants.REQUIRED)) {
            negativeTokenTestExpiredToken("helloworld_expired_disableLTPACookie_Default_authnSessionDisabled_False", ltpaFound, clientFound);
        } else {
            positiveTokenTestExpiredToken("helloworld_expired_disableLTPACookie_Default_authnSessionDisabled_False", ltpaFound, clientFound);
        }

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = false
     * pass an expired access_token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_true_authnSessionDisabled_false_expiredToken() throws Exception {

        setCookieExpectations(true, false, false);
        if (inboundPropType.equals(Constants.REQUIRED)) {
            negativeTokenTestExpiredToken("helloworld_expired_disableLTPACookie_True_authnSessionDisabled_False", ltpaFound, clientFound);
        } else {
            positiveTokenTestExpiredToken("helloworld_expired_disableLTPACookie_True_authnSessionDisabled_False", ltpaFound, clientFound);
        }

    }

    /**
     * Settings/values passed/notpassed
     * disableLtpaCookie = default (false)
     * authnSessionDisabled = false
     * pass an expired access_token
     * Expected behaviour:
     * We should have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void CookieAttributes2ServerTests_disableLTPACookie_false_authnSessionDisabled_false_expiredToken() throws Exception {

        setCookieExpectations(false, false, false);
        if (inboundPropType.equals(Constants.REQUIRED)) {
            negativeTokenTestExpiredToken("helloworld_expired_disableLTPACookie_False_authnSessionDisabled_False", ltpaFound, clientFound);
        } else {
            positiveTokenTestExpiredToken("helloworld_expired_disableLTPACookie_False_authnSessionDisabled_False", ltpaFound, clientFound);
        }

    }

}
