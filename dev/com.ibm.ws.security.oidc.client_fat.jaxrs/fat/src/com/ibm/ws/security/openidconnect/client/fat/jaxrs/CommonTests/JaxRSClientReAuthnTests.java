/*******************************************************************************
 * Copyright (c) 2016, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.fat.jaxrs.CommonTests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonCookieTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that contains common code for all of the
 * OpenID Connect RP tests. There will be OP specific test classes that extend this class.
 **/

public class JaxRSClientReAuthnTests extends CommonTest {

    public static Class<?> thisClass = JaxRSClientReAuthnTests.class;
    public static HashMap<String, Integer> defRespStatusMap = null;
    protected static CommonCookieTools cookieTools = new CommonCookieTools();
    public static RSCommonTestTools rsTools = new RSCommonTestTools();

    private final static Boolean DOES_NOT_USE_CUSHION = false;
    private final static Boolean USES_CUSHION = true;
    private final static Boolean DOES_NOT_USE_REAUTHN = false;
    private final static Boolean USES_REAUTHN = true;
    private final static Boolean ID_TOKEN_SHORT_LIFETIME = true;
    private final static Boolean ID_TOKEN_LONG_LIFETIME = false;

    private TestSettings updateMap(TestSettings settings, String theKey, String theValue) throws Exception {
        //Map<String, String> map = new HashMap <String,String> ();

        Map<String, String> currentMap = settings.getRequestParms();
        if (currentMap == null) {
            currentMap = new HashMap<String, String>();
        }
        Log.info(thisClass, "updateMap", "Processing Key: " + theKey + " Value: " + theValue);
        currentMap.put(theKey, theValue);
        settings.setRequestParms(currentMap);

        return settings;
    }

    /**
     * Add additional checks for output from the other new API's
     *
     */
    private List<validationData> setEndToEndExpectations(String testCase, String finalAction, TestSettings settings) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        if (finalAction.equals(Constants.LOGIN_USER)) {
            expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        }

        expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the app info in the output", null,
                                            "Accessed Hello World");
        if (settings.getRequestParms() != null) {
            String testApp = settings.getRequestParms().get("targetApp"); // if we're trying to get the app name for verification, we should have a test app set...
            expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not go to the correct app", null,
                                                "Param: targetApp with value: " + testApp);
        }

        //disable until Ut's fix is deliverd		expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the UniqueSecurityName in the app output", null, "UniqueSecurityName=" + settings.getAdminUser()) ;

        return expectations;
    }

    /**
     * Add additional checks for output from the other new API's
     *
     */
    private List<validationData> setAccessDeniedExpectations(String finalAction) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes(null, finalAction);
        expectations = vData.addResponseStatusExpectation(expectations, finalAction, Constants.INTERNAL_SERVER_ERROR_STATUS);
        expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see response code 401 in the output", null,
                                            Constants.HTTP_UNAUTHORIZED_EXCEPTION);

        if (genericTestServer.getRSValidationType().equals(Constants.USERINFO_ENDPOINT)) {
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, finalAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES,
                                                                 "Did not find message in log saying userinfo request was made with an unrecognized token.",
                                                                 MessageConstants.CWWKS1617E_USERINFO_REQUEST_BAD_TOKEN);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES,
                                                                 "Did not find message in log saying userinfo request was made with an inactive token.",
                                                                 MessageConstants.CWWKS1720E_ACCESS_TOKEN_NOT_ACTIVE + ".*\\[" + genericTestServer.getRSValidationType() + "\\]");
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES,
                                                                 "Did not find message in log saying the resource server encountered an error because of the token.",
                                                                 MessageConstants.CWWKS1721E_OIDC_REQ_FAILED_ACCESS_TOKEN_NOT_VALID);
        } else if (genericTestServer.getRSValidationType().equals(Constants.INTROSPECTION_ENDPOINT)) {
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, finalAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES,
                                                                 "Did not find message in log saying the request failed because of an invalid token.",
                                                                 MessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES,
                                                                 "Did not find message in log saying userinfo request was made with an inactive token.",
                                                                 MessageConstants.CWWKS1720E_ACCESS_TOKEN_NOT_ACTIVE + ".*\\[" + genericTestServer.getRSValidationType() + "\\]");
        } else if (genericTestServer.getRSValidationType().equals(Constants.LOCAL_VALIDATION_METHOD)) {
            genericTestServer.addIgnoredServerException(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES,
                                                                 "Did not find message in log saying validation failed because of an expired token.",
                                                                 MessageConstants.CWWKS1773E_TOKEN_EXPIRED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, finalAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES,
                                                                 "Did not find message in log saying the OIDC client failed to validate the JWT.",
                                                                 MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE);
        }
        return expectations;
    }

    private void runTest(String testCase, Boolean usesReAuthn, Boolean cushionSet, Boolean idTokenHasShortLifetime, String rp_app, String rs_app) throws Exception {

        // remove the OP's cookie before 2nd/3rd invocations of the client servlet- even if the RP goes to the OP, it'll be hidden
        // from us because the OP will use it's cookie to authorize, ... (removeCookieFromConversation calls below)

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/simplejaxrsclient/Protected_SimpleJaxRSClient/" + rp_app);
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/" + rs_app);

        List<validationData> expectationsWithLogin = setEndToEndExpectations(_testName, Constants.LOGIN_USER, updatedTestSettings);
        List<validationData> expectationsNoLogin = setEndToEndExpectations(_testName, Constants.GET_LOGIN_PAGE, updatedTestSettings);
        List<validationData> expectationsAccessDenied = setAccessDeniedExpectations(Constants.GET_LOGIN_PAGE);

        // 1 - access app on RP to get credentials (need to log in to do this)
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectationsWithLogin);
        msgUtils.printAllCookies(wc);

        // chc - if reauthn is false, do we look at cushion?
        // 2 - access app again
        // 2a - if at least one attr is set to a short lifetime we need further checking to determine if we'll need
        //			to log in, or if we'll still have access
        //	2a1 - when reAuthn is true
        //		2a1a - we'll need to reauthenticate (test is setup to have a cushion larger than the lifetimes)
        //		2a1b - we'll still have access if no cushion
        //	2a2 - when reAuthn is false
        //		2a2a - if idToken is expired, reAuthn is internally set to true (config setting is ignored), so, if cushion
        //			is set, we'll need to log in
        //		2a2b - if idToken is not expired and/or cushion is NOT set we should still have access and not need to login
        // 2b - if both access_token and id_token are long lived, we should always have access without having to log in again
        //			this is our normal test behavior, but we don't have tests elsewhere that modify reAuthn and cushion, so
        //			add a few tests here just to make sure that we do the correct thing

        // 2a - if cushion is 0, we should still have access - NO login required
        // 2b - if cushion is specified (our configs are defined such that the cushion is larger than the lifetime, so, we'll need to login
        cookieTools.removeCookieFromConverstation(wc, Constants.OP_COOKIE);
        if (rp_app.contains("Short")) {
            if (usesReAuthn) {
                if (cushionSet) {
                    // 2a1a
                    Log.info(thisClass, "Step2", testCase + " Need Login");
                    genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectationsWithLogin);
                } else {
                    // 2a1b
                    Log.info(thisClass, "Step2", testCase + " No Login");
                    genericRP(_testName, wc, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectationsNoLogin);
                }
            } else {
                if (cushionSet && idTokenHasShortLifetime) {
                    // 2a2a
                    Log.info(thisClass, "Step2", testCase + " Need Login");
                    genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectationsWithLogin);
                } else {
                    // 2a2b
                    Log.info(thisClass, "Step2", testCase + " No Login");
                    genericRP(_testName, wc, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectationsNoLogin);
                }
            }
        } else {
            // 2b
            Log.info(thisClass, "Step2", testCase + " No Login");
            genericRP(_testName, wc, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectationsNoLogin);
        }
        // 3 - try to access the app after sleeping (giving time for short lived tokens to expire) - testing that we still need to log in
        // 3a - if at least one attr is set to a short lifetime we need further checking to determine if we'll need
        //			to log in, or if we'll still get access
        // 3a1 - if reAuthn is true, or the ID_Token is expired (which behaves like reAuthn is true), we'll be able to reauthenticate (login)
        // 3a2 - if neither reAuthn is true and the ID_Token is NOT expired, we'll just get a 401
        // 3b - Tokens are long lived - make sure even with reAuthn and the cushion configured, neither comes into play as we still have access
        //			should just get to the app
        helpers.testSleep(20);
        cookieTools.removeCookieFromConverstation(wc, Constants.OP_COOKIE);
        if (rp_app.contains("Short")) {
            if (usesReAuthn || idTokenHasShortLifetime) {
                Log.info(thisClass, "Step3", testCase + " Need Login");
                genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectationsWithLogin);
            } else {
                Log.info(thisClass, "Step3", testCase + " 401");
                genericRP(_testName, wc, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectationsAccessDenied);
            }
        } else {
            Log.info(thisClass, "Step3", testCase + " No Login");
            genericRP(_testName, wc, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectationsNoLogin);
        }
    }

    /**
     * sort lifetimes are used just so we don't have to sleep too long waiting for it to expire
     * Long lifetimes are the default lifetimes and won't expire for the duration of the test.
     * NOTE: when cushion is set, but reAuthnOnAccessTokenExpire=false, the cushion will be ignored for expired access_token, but
     * will
     * be honored for an expired ID_Token
     *
     * Client app is invoked 3 times
     * 1) get the access and ID Tokens
     * 2) access the app again - some configs will require the user to reauthenticate, others will still allow access
     * 3) sleep, then access the app again - some configs will reauthenticate, others will return 401 and long lifetime tests will
     * still have access.
     */

    /**
     * Set:
     * access_token that has a short lifetime
     * ID_Token with a long lifetime
     * set reAuthnOnAccessTokenExpire to true
     * set reAuthnCushion to 0
     * Expect:
     * 2nd call: access - no login
     * 3rd call: login needed
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientReAuthnTests_accessTokenShortLifetime_reAuthnTrue_noCushion() throws Exception {

        runTest(_testName, USES_REAUTHN, DOES_NOT_USE_CUSHION, ID_TOKEN_LONG_LIFETIME, "accessTokenShortLifetime_reAuthnTrue_noCushion", "helloworld_accessTokenShortLifetime");

    }

    /**
     * Set:
     * access_token that has a long lifetime
     * ID_Token with a short lifetime
     * set reAuthnOnAccessTokenExpire to true
     * set reAuthnCushion to 0
     * Expect:
     * 2nd call: no login
     * 3rd call: login needed
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientReAuthnTests_idTokenShortLifetime_reAuthnTrue_noCushion() throws Exception {

        runTest(_testName, USES_REAUTHN, DOES_NOT_USE_CUSHION, ID_TOKEN_SHORT_LIFETIME, "idTokenShortLifetime_reAuthnTrue_noCushion", "helloworld_idTokenShortLifetime");

    }

    /**
     * Set:
     * access_token that has a short lifetime
     * ID_Token with a short lifetime
     * set reAuthnOnAccessTokenExpire to true
     * set reAuthnCushion to 0
     * Expect:
     * 2nd call: No login
     * 3rd call: login needed
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientReAuthnTests_bothShortLifetime_reAuthnTrue_noCushion() throws Exception {

        runTest(_testName, USES_REAUTHN, DOES_NOT_USE_CUSHION, ID_TOKEN_SHORT_LIFETIME, "bothShortLifetime_reAuthnTrue_noCushion", "helloworld_bothShortLifetime");

    }

    /**
     * Set:
     * access_token that has a short lifetime
     * ID_Token with a long lifetime
     * set reAuthnOnAccessTokenExpire to false
     * set reAuthnCushion to 0
     * Expect:
     * 2nd call: No login
     * 3rd call: 401
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientReAuthnTests_accessTokenShortLifetime_reAuthnFalse_noCushion() throws Exception {

        runTest(_testName, DOES_NOT_USE_REAUTHN, DOES_NOT_USE_CUSHION, ID_TOKEN_LONG_LIFETIME, "accessTokenShortLifetime_reAuthnFalse_noCushion",
                "helloworld_accessTokenShortLifetime");

    }

    /**
     * Set:
     * access_token that has a long lifetime
     * ID_Token with a short lifetime
     * set reAuthnOnAccessTokenExpire to false
     * set reAuthnCushion to 0
     * Expect:
     * 2nd call: No Login
     * 3rd call: 401
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientReAuthnTests_idTokenShortLifetime_reAuthnFalse_noCushion() throws Exception {

        runTest(_testName, DOES_NOT_USE_REAUTHN, DOES_NOT_USE_CUSHION, ID_TOKEN_SHORT_LIFETIME, "idTokenShortLifetime_reAuthnFalse_noCushion", "helloworld_idTokenShortLifetime");

    }

    /**
     * Set:
     * access_token that has a short lifetime
     * ID_Token with a short lifetime
     * set reAuthnOnAccessTokenExpire to false
     * set reAuthnCushion to 0
     * Expect:
     * 2nd call: No login
     * 3rd call: login needed
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientReAuthnTests_bothShortLifetime_reAuthnFalse_noCushion() throws Exception {

        runTest(_testName, DOES_NOT_USE_REAUTHN, DOES_NOT_USE_CUSHION, ID_TOKEN_SHORT_LIFETIME, "bothShortLifetime_reAuthnFalse_noCushion", "helloworld_bothShortLifetime");

    }

    /**
     * Set:
     * access_token that has a short lifetime
     * ID_Token with a long lifetime
     * set reAuthnOnAccessTokenExpire to true
     * set reAuthnCushion to 10s
     * Expect:
     * 2nd call: need login
     * 3rd call: need login
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientReAuthnTests_accessTokenShortLifetime_reAuthnTrue_withCushion() throws Exception {

        runTest(_testName, USES_REAUTHN, USES_CUSHION, ID_TOKEN_LONG_LIFETIME, "accessTokenShortLifetime_reAuthnTrue_withCushion", "helloworld_accessTokenShortLifetime");

    }

    /**
     * Set:
     * access_token that has a long lifetime
     * ID_Token with a short lifetime
     * set reAuthnOnAccessTokenExpire to true
     * set reAuthnCushion to 10s
     * Expect:
     * 2nd call: need login
     * 3rd call: need login
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientReAuthnTests_idTokenShortLifetime_reAuthnTrue_withCushion() throws Exception {

        runTest(_testName, USES_REAUTHN, USES_CUSHION, ID_TOKEN_SHORT_LIFETIME, "idTokenShortLifetime_reAuthnTrue_withCushion", "helloworld_idTokenShortLifetime");

    }

    /**
     * Set:
     * access_token that has a short lifetime
     * ID_Token with a short lifetime
     * set reAuthnOnAccessTokenExpire to true
     * set reAuthnCushion to 10s
     * Expect:
     * 2nd call: need login
     * 3rd call: need login
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientReAuthnTests_bothShortLifetime_reAuthnTrue_withCushion() throws Exception {

        runTest(_testName, USES_REAUTHN, USES_CUSHION, ID_TOKEN_SHORT_LIFETIME, "bothShortLifetime_reAuthnTrue_withCushion", "helloworld_bothShortLifetime");

    }

    /**
     * Set:
     * access_token that has a short lifetime
     * ID_Token with a long lifetime
     * set reAuthnOnAccessTokenExpire to false
     * set reAuthnCushion to 10s
     * Expect:
     * 2nd call: no login
     * 3rd call: 401
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientReAuthnTests_accessTokenShortLifetime_reAuthnFalse_withCushion() throws Exception {

        runTest(_testName, DOES_NOT_USE_REAUTHN, USES_CUSHION, ID_TOKEN_LONG_LIFETIME, "accessTokenShortLifetime_reAuthnFalse_withCushion", "helloworld_accessTokenShortLifetime");

    }

    /**
     * Set:
     * access_token that has a long lifetime
     * ID_Token with a short lifetime
     * set reAuthnOnAccessTokenExpire to false
     * set reAuthnCushion to 10s
     * Expect:
     * 2nd call: login needed
     * 3rd call: login needed
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientReAuthnTests_idTokenShortLifetime_reAuthnFalse_withCushion() throws Exception {

        runTest(_testName, DOES_NOT_USE_REAUTHN, USES_CUSHION, ID_TOKEN_SHORT_LIFETIME, "idTokenShortLifetime_reAuthnFalse_withCushion", "helloworld_idTokenShortLifetime");

    }

    /**
     * Set:
     * access_token that has a short lifetime
     * ID_Token with a short lifetime
     * set reAuthnOnAccessTokenExpire to false
     * set reAuthnCushion to 10s
     * Expect:
     * 2nd call: login needed
     * 3rd call: login needed
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientReAuthnTests_bothShortLifetime_reAuthnFalse_withCushion() throws Exception {

        runTest(_testName, DOES_NOT_USE_REAUTHN, USES_CUSHION, ID_TOKEN_SHORT_LIFETIME, "bothShortLifetime_reAuthnFalse_withCushion", "helloworld_bothShortLifetime");

    }

    /**
     * Set:
     * access_token that has a long lifetime
     * ID_Token with a long lifetime
     * set reAuthnOnAccessTokenExpire to true
     * set reAuthnCushion to 0
     * Expect:
     * 2nd call: no login
     * 3rd call: no login
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientReAuthnTests_bothLongLifetime_reAuthnTrue_noCushion() throws Exception {

        runTest(_testName, USES_REAUTHN, DOES_NOT_USE_CUSHION, ID_TOKEN_SHORT_LIFETIME, "bothLongLifetime_reAuthnTrue_noCushion", "helloworld_bothLongLifetime");

    }

    /**
     * Set:
     * access_token that has a long lifetime
     * ID_Token with a long lifetime
     * set reAuthnOnAccessTokenExpire to false
     * set reAuthnCushion to 0
     * Expect:
     * 2nd call: no login
     * 3rd call: no login
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientReAuthnTests_bothLongLifetime_reAuthnFalse_noCushion() throws Exception {

        runTest(_testName, DOES_NOT_USE_REAUTHN, DOES_NOT_USE_CUSHION, ID_TOKEN_LONG_LIFETIME, "bothLongLifetime_reAuthnFalse_noCushion", "helloworld_bothLongLifetime");

    }

    /**
     * Set:
     * access_token that has a long lifetime
     * ID_Token with a long lifetime
     * set reAuthnOnAccessTokenExpire to true
     * set reAuthnCushion to 10s
     * Expect:
     * 2nd call: no login
     * 3rd call: no login
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientReAuthnTests_bothLongLifetime_reAuthnTrue_withCushion() throws Exception {

        runTest(_testName, USES_REAUTHN, USES_CUSHION, ID_TOKEN_SHORT_LIFETIME, "bothLongLifetime_reAuthnTrue_withCushion", "helloworld_bothLongLifetime");

    }

    /**
     * Set:
     * access_token that has a long lifetime
     * ID_Token with a long lifetime
     * set reAuthnOnAccessTokenExpire to false
     * set reAuthnCushion to 10s
     * Expect:
     * 2nd call: no login
     * 3rd call: no login
     *
     * @throws Exception
     */
    @Test
    public void JaxRSClientReAuthnTests_bothLongLifetime_reAuthnFalse_withCushion() throws Exception {

        runTest(_testName, DOES_NOT_USE_REAUTHN, USES_CUSHION, ID_TOKEN_SHORT_LIFETIME, "bothLongLifetime_reAuthnFalse_withCushion", "helloworld_bothLongLifetime");

    }

    /**
     * Set:
     * **disableLtpaCookie is false **
     * access_token that has a short lifetime
     * ID_Token with a short lifetime
     * set reAuthnOnAccessTokenExpire to true
     * set reAuthnCushion to 10s
     * Expect:
     * 2nd call: no login
     * 3rd call: no login
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void JaxRSClientReAuthnTests_disableLtpaCookieFalse_bothShortLifetime_reAuthnTrue_withCushion() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/simplejaxrsclient/Protected_SimpleJaxRSClient/"
                                       + "disableLtpaCookieFalse_bothShortLifetime_reAuthnTrue_withCushion");
        updateMap(updatedTestSettings, Constants.TARGET_APP, genericTestServer.getHttpString() + "/helloworld/rest/" + "helloworld_bothShortLifetime");

        List<validationData> expectationsWithLogin = setEndToEndExpectations(_testName, Constants.LOGIN_USER, updatedTestSettings);
        List<validationData> expectationsNoLogin = setEndToEndExpectations(_testName, Constants.GET_LOGIN_PAGE, updatedTestSettings);
        List<validationData> expectationsAccessDenied = setAccessDeniedExpectations(Constants.GET_LOGIN_PAGE);

        // access app on RP to get credentials (need to log in to do this)
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectationsWithLogin);
        msgUtils.printAllCookies(wc);

        // access app again - with cushion and reAuthn, we should need to authenticate, but, because we have the
        // RP LTPA token, we do NOT have to login again
        genericRP(_testName, wc, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectationsNoLogin);

        helpers.testSleep(20);
        cookieTools.removeCookieFromConverstation(wc, Constants.OP_COOKIE);

        // again, since we have the LTPA Token, the RP does NOT make us login, but the OP sees the expired tokens
        genericRP(_testName, wc, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectationsAccessDenied);
    }

}
