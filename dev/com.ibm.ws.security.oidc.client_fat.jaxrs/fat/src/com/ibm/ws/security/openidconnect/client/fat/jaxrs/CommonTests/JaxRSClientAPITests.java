/*******************************************************************************
 * Copyright (c) 2016, 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.client.fat.jaxrs.CommonTests;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.openidconnect.client.fat.jaxrs.FATSuite;
import com.meterware.httpunit.WebConversation;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that contains common code for all of the
 * OpenID Connect RP tests. There will be OP specific test classes that extend this class.
 **/

public class JaxRSClientAPITests extends CommonTest {

    public static Class<?> thisClass = JaxRSClientAPITests.class;
    public static HashMap<String, Integer> defRespStatusMap = null;

    public static String[] test_GOOD_LOGIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_POST_LOGIN_ACTIONS = Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT;
    public static String[] test_GOOD_LOGIN_AGAIN_ACTIONS = Constants.GOOD_OIDC_LOGIN_AGAIN_ACTIONS;
    public static String[] test_LOGIN_PAGE_ONLY = Constants.GET_LOGIN_PAGE_ONLY;
    public static String test_FinalAction = Constants.LOGIN_USER;
    protected static final Boolean contextWillNotBeSet = false;
    protected static final Boolean contextWillBeSet = true;

    /**
     * Add additional checks for output from the other new API's
     *
     */
    protected List<validationData> setRSOauthExpectationsWithAPIChecks(String testCase, String finalAction, TestSettings settings, Boolean contextWillBeSet) throws Exception {

        String bearer, scopes = null;
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        if (!Constants.JWE_TOKEN_FORMAT.equals(FATSuite.repeatFlag)) {
            if (contextWillBeSet) {
                expectations = validationTools.addDefaultRSOAuthExpectations(expectations, testCase, finalAction, settings);
                bearer = "Bearer";
                scopes = settings.getScope();
                if (scopes.contains("openid")) {
                    expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Did not see a valid ID Token in the ouptut", null, "JaxRSClient-getIdToken: null");
                    expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see a valid raw ID token string in the ouptut", null, "JaxRSClient-getIdToken-rawIdTokenString: eyJ");
                } else {
                    expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Received an ID Token and should NOT have", null, "JaxRSClient-getIdToken: null");
                    expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Found a non-null raw ID token string and should NOT have.", null, "JaxRSClient-getIdToken-rawIdTokenString: null");
                }
            } else {
                bearer = "null";
                scopes = "null";
                expectations = vData.addExpectation(expectations, finalAction, testRPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not see a message that all of the subject values were null", null, "All values in subject are null as they should be");
                expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Got to the target App on the RS server and should not have...", null, "formlogin/SimpleServlet");
            }
            expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the Access Token Type set to Bearer printed in the app output", null, "JaxRSClient-getAccessTokenType: " + bearer);
            //		expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the Access Token Expiration Time printed in the app output", null, "JaxRSClient-getAccessTokenExpirationTime: 0") ;
            expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the Access Token printed in the app output", null, "JaxRSClient-getAccessToken: ");
            expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the Access Token Scopes printed in the app output", null, "JaxRSClient-getScopes: " + scopes);
            expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the Access Token IDToken printed in the app output", null, "JaxRSClient-getIdToken: ");
            expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see the raw ID token string printed in the app output.", null, "JaxRSClient-getIdToken-rawIdTokenString: ");
        } else {
            if (contextWillBeSet) {
                expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not see the Access Token Type set to Bearer printed in the app output", null, "token_type.*Bearer.*");
                expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not see the correct issuer in the output", null, "iss.*localhost.*TokenEndpointServlet.*");
            }
        }
        return expectations;
    }

    /**
     * Add additional checks for output from the other new API's
     *
     */
    protected List<validationData> setRSOauth401Expectations(String finalAction) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes(null, finalAction);
        expectations = vData.addResponseStatusExpectation(expectations, finalAction, Constants.INTERNAL_SERVER_ERROR_STATUS);
        expectations = vData.addExpectation(expectations, finalAction, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not see response code 401 in the output", null, Constants.HTTP_UNAUTHORIZED_EXCEPTION);
        expectations = vData.addExpectation(expectations, finalAction, testRPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not see response code 401 in the output", null, Constants.HTTP_UNAUTHORIZED_EXCEPTION);
        return expectations;
    }

    protected TestSettings updateMap(TestSettings settings, String theKey, String theValue) throws Exception {
        //Map<String, String> map = new HashMap <String,String> ();

        Map<String, String> currentMap = settings.getRequestParms();
        if (currentMap == null) {
            currentMap = new HashMap<String, String>();
        }
        Log.info(thisClass, "updateMap", "Processing Key: " + theKey + " Value: " + theValue);
        currentMap.put(theKey, theValue);
        if (Constants.JWE_TOKEN_FORMAT.equals(FATSuite.repeatFlag)) {
            currentMap.put("skipApiChecks", "true");
        }
        settings.setRequestParms(currentMap);

        return settings;
    }

    /**
     * Create and Save a JWS token that the test userinfo endpoint can return - this will
     * allow us to test that the RP and RS can process a userinfo reponse that contains
     * a JWS instead of a simple JSON reponse
     */
    public void saveJWSUserInfoResponse() throws Exception {

        String thisMethod = "saveJWSUserInfoResponse";
        msgUtils.printMethodName(thisMethod);

        JWTTokenBuilder builder = new JWTTokenBuilder();

        builder.setIssuer(testOPServer.getHttpString() + "/TokenEndpointServlet");
        builder.setAlorithmHeaderValue(Constants.SIGALG_RS256);
        builder.setRSAKey(testOPServer.getServer().getServerRoot() + "/RS256private-key.pem");
        builder.setIssuer("testuser");
        // calling buildJWE will override the header contents
        String jwtToken = builder.build();
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "userinfoToken", jwtToken);
        genericInvokeEndpointWithHttpUrlConn(_testName, null, testSettings.getUserinfoEndpt(), Constants.PUTMETHOD, "misc", parms, null, null);

    }

    /**
     * Create and Save a JWE token that the test userinfo endpoint can return - this will
     * allow us to test that the RP and RS can process a userinfo reponse that contains
     * a JWE instead of a simple JSON reponse
     */
    public void saveJWEUserInfoResponse() throws Exception {

        String thisMethod = "saveJWEUserInfoResponse";
        msgUtils.printMethodName(thisMethod);

        JWTTokenBuilder builder = new JWTTokenBuilder();

        builder.setIssuer(testOPServer.getHttpString() + "/TokenEndpointServlet");
        builder.setAlorithmHeaderValue(Constants.SIGALG_RS256);
        builder.setRSAKey(testOPServer.getServer().getServerRoot() + "/RS256private-key.pem");
        builder.setIssuer("testuser");
        builder.setKeyManagementKey(JwtKeyTools.getPublicKeyFromPem(JwtKeyTools.getComplexPublicKeyForSigAlg(testOPServer.getServer(), Constants.SIGALG_RS256)));
        // calling buildJWE will override the header contents
        String jwtToken = builder.buildJWE("JOSE", "jwt");
        List<endpointSettings> parms = eSettings.addEndpointSettingsIfNotNull(null, "userinfoToken", jwtToken);
        genericInvokeEndpointWithHttpUrlConn(_testName, null, testSettings.getUserinfoEndpt(), Constants.PUTMETHOD, "misc", parms, null, null);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid RP url - the test servlet then invokes an app on the RS server -
     * passing in the access_token. The Access Token is obtained using the PropagationHelper.getAccessToken() api. Access should
     * be granted to the second app...
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true"
     * <LI>Test is showing a good main path flow using HTTP between RP, OP and RS. In this scenario,
     * <LI>the httpsRequired attribute is set to "false".
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void APIOidcJaxRSClientTests_protectedApp() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid RP url - the test servlet is NOT protected, so, we have no security
     * context when we are in the test server. The app tries to retrieve the access_token using PropagationHelper.getAccessToken()
     * api. This should return a null as there is no Subject. We will run all of the api's and make sure that all of them return
     * null (and do not throw any exceptions). We do NOT appempt to invoke the app on the RS server as there is no access_token
     * <LI>There is nothing special about the configuration - it just needs to be valid.
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true"
     * <LI>Test is showing a good main path flow using HTTP between RP, OP and RS. In this scenario,
     * <LI>the httpsRequired attribute is set to "false".
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>All api's should return null - we should not apptempt to invoke the app on the RS server
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void APIOidcJaxRSClientTests_unProtectedApp() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setTestURL(testRPServer.getHttpsString() + "/jaxrsclient/JaxRSClient");
        updateMap(updatedTestSettings, Constants.CONTEXT_SET, "false");
        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, Constants.GET_LOGIN_PAGE, updatedTestSettings, contextWillNotBeSet);

        genericRP(_testName, wc, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Attempt to access a test servlet specifying valid OP url - the test servlet then invokes an app on the RS server -
     * passing in the access_token. The Access Token is obtained using the PropagationHelper.getAccessToken() api. Access should
     * be granted to the second app...
     * <LI>There is nothing special about the configuration - just that the grantType is implicit
     * <LI>In this scenario, the consent form is disabled in OP by setting the oauthProvider attribute
     * <LI>in server.xml as: autoAuthorize="true"
     * <LI>Test is showing a good main path flow using HTTP between RP and OP. In this scenario,
     * <LI>the httpsRequired attribute is set to "false".
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should authenticate and access the test servlet without issue
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void APIOidcJaxRSClientTests_implicitFlow() throws Exception {

        testRPServer.reconfigServer("rp_server_implicit.xml", _testName, false, null);
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet);

        String[] actionFlag = test_GOOD_LOGIN_ACTIONS;
        if (Constants.JWE_TOKEN_FORMAT.equals(FATSuite.repeatFlag)) {
            actionFlag = Constants.GET_LOGIN_PAGE_ONLY;
        }
        genericRP(_testName, wc, updatedTestSettings, actionFlag, expectations);

    }

    @Mode(TestMode.LITE)
    @Test
    public void APIOidcJaxRSClientTests_jaxrsOAuthClientProperty_string_true() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updateMap(updatedTestSettings, Constants.WHERE, Constants.PROPAGATE_TOKEN_STRING_TRUE);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    @Mode(TestMode.LITE)
    @Test
    public void APIOidcJaxRSClientTests_jaxrsOAuthClientProperty_boolean_true() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updateMap(updatedTestSettings, Constants.WHERE, Constants.PROPAGATE_TOKEN_BOOLEAN_TRUE);

        List<validationData> expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    @Mode(TestMode.LITE)
    @Test
    public void APIOidcJaxRSClientTests_jaxrsOAuthClientProperty_string_false() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updateMap(updatedTestSettings, Constants.WHERE, Constants.PROPAGATE_TOKEN_STRING_FALSE);

        List<validationData> expectations = setRSOauth401Expectations(test_FinalAction);
        // issue 3710 // expectations = validationTools.addMessageExpectation(genericTestServer, expectations, test_FinalAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Did not find message in logs saying a propagation token was missing.", MessageConstants.CWWKS1726E_MISSING_PROPAGATION_TOKEN);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    //        @Mode(TestMode.LITE)
    @Test
    public void APIOidcJaxRSClientTests_jaxrsOAuthClientProperty_boolean_false() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updateMap(updatedTestSettings, Constants.WHERE, Constants.PROPAGATE_TOKEN_BOOLEAN_FALSE);

        List<validationData> expectations = setRSOauth401Expectations(test_FinalAction);
        // issue 3710 // expectations = validationTools.addMessageExpectation(genericTestServer, expectations, test_FinalAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Did not find message in logs saying a propagation token was missing.", MessageConstants.CWWKS1726E_MISSING_PROPAGATION_TOKEN);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    @Mode(TestMode.LITE)
    @Test
    public void APIOidcJaxRSClientTests_jaxrsJWTClientProperty_string_true() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updateMap(updatedTestSettings, Constants.WHERE, Constants.PROPAGATE_JWT_TOKEN_STRING_TRUE);

        List<validationData> expectations = null;
        // we should not have access if we pass an opaque token
        if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            expectations = setRSOauth401Expectations(test_FinalAction);
        } else {
            expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet);
        }

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    @Mode(TestMode.LITE)
    @Test
    public void APIOidcJaxRSClientTests_jaxrsJWTClientProperty_boolean_true() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updateMap(updatedTestSettings, Constants.WHERE, Constants.PROPAGATE_JWT_TOKEN_BOOLEAN_TRUE);

        List<validationData> expectations = null;
        // we should not have access if we pass an opaque token
        if (updatedTestSettings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            expectations = setRSOauth401Expectations(test_FinalAction);
        } else {
            expectations = setRSOauthExpectationsWithAPIChecks(_testName, test_FinalAction, updatedTestSettings, contextWillBeSet);
        }

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    @Mode(TestMode.LITE)
    @Test
    public void APIOidcJaxRSClientTests_jaxrsJWTClientProperty_string_false() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updateMap(updatedTestSettings, Constants.WHERE, Constants.PROPAGATE_JWT_TOKEN_STRING_FALSE);

        List<validationData> expectations = setRSOauth401Expectations(test_FinalAction);
        // issue 3710 // expectations = validationTools.addMessageExpectation(genericTestServer, expectations, test_FinalAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Did not find message in logs saying a propagation token was missing.", MessageConstants.CWWKS1726E_MISSING_PROPAGATION_TOKEN);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }

    @Mode(TestMode.LITE)
    @Test
    public void APIOidcJaxRSClientTests_jaxrsJWTClientProperty_boolean_false() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updateMap(updatedTestSettings, Constants.WHERE, Constants.PROPAGATE_JWT_TOKEN_BOOLEAN_FALSE);

        List<validationData> expectations = setRSOauth401Expectations(test_FinalAction);
        // issue 3710 // expectations = validationTools.addMessageExpectation(genericTestServer, expectations, test_FinalAction, Constants.MESSAGES_LOG, Constants.STRING_MATCHES, "Did not find message in logs saying a propagation token was missing.", MessageConstants.CWWKS1726E_MISSING_PROPAGATION_TOKEN);

        genericRP(_testName, wc, updatedTestSettings, test_GOOD_LOGIN_ACTIONS, expectations);

    }
}
