/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.server.fat.BasicTests.CommonTests;

import java.util.ArrayList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MultiProviderUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.utils.LDAPUtils;

@RunWith(FATRunner.class)
public class genericWebClientAuthCodeTest extends CommonTest {

    private static final Class<?> thisClass = genericWebClientAuthCodeTest.class;

    /**
     * TestDescription:
     *
     * This testcase performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code". In this scenario, the client uses the
     * authorization server as an intermediary to obtain the authorization code
     * and then uses this authorization code to request access token from the
     * token endpoint. The authorization server authenticates the resource owner
     * before issuing the authorization code which is redirected to the client.
     * In this scenario, the autoauthz parameter is set to true, so the resource
     * owner does not receive the consent form from the authorizarion server.
     * The test verifies that the Oauth code flow, using the authorization grant
     * type of "authorization code" works correctly.
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuthCodeBasicFlow() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes();

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                            "Found the JWT SSO cookie name in the output but should not have.", null, "<td>" + Constants.JWT_SSO_COOKIE_NAME + "</td>");

        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /**
     * Tests:
     * - Authorization code flow with the jwtSso-1.0 feature enabled
     * Expects:
     * - Should successfully reach the protected resource
     * - Should find the JWT SSO cookie name in the final response
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuthCodeBasicFlow_withJwtSsoFeature() throws Exception {
        testOPServer.reconfigServer("server_withJwtSsoFeature.xml", _testName, Constants.JUNIT_REPORTING, null);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes();

        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Did not find the expected JWT SSO cookie name in the output but should have.", null, "<td>" + Constants.JWT_SSO_COOKIE_NAME + "</td>");

        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    //@Mode(TestMode.LITE)
    @Test
    public void testAuthCodeBasicFlowwithDashandUnderscore() throws Exception {
        testOPServer.reconfigServer("server_id_contains_dash_underscore.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        WebConversation wc2 = new WebConversation();

        TestSettings updatedTestSettings;
        MultiProviderUtils mpUtils = new MultiProviderUtils();

        if (eSettings.getProviderType() == Constants.OAUTH_OP) {
            updatedTestSettings = mpUtils.copyAndOverrideProviderSettings(testSettings, Constants.OAUTHCONFIGSAMPLE_APP, "OAuthConfigSample-1", null);
            updatedTestSettings.setProtectedResource(eSettings.assembleProtectedResource(testOPServer.getHttpsString(), testSettings.getConfigTAI(), Constants.SNOOPING));
        } else {
            updatedTestSettings = mpUtils.copyAndOverrideProviderSettings(testSettings, Constants.OIDCCONFIGSAMPLE_APP, "OidcConfigSample-1", null);
            updatedTestSettings.setProtectedResource(eSettings.assembleProtectedResource(testOPServer.getHttpsString(), testSettings.getConfigTAI(), Constants.SNOOPING));
        }

        // expect good (200) status codes for all steps for provider names including dashes
        List<validationData> expectations = vData.addSuccessStatusCodes();

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");

        TestSettings updatedTestSettings2;
        MultiProviderUtils mpUtils2 = new MultiProviderUtils();
        if (eSettings.getProviderType() == Constants.OAUTH_OP) {
            updatedTestSettings2 = mpUtils2.copyAndOverrideProviderSettings(testSettings, Constants.OAUTHCONFIGSAMPLE_APP, "OAuthConfigSample_1", null);
        } else {
            updatedTestSettings2 = mpUtils2.copyAndOverrideProviderSettings(testSettings, Constants.OIDCCONFIGSAMPLE_APP, "OidcConfigSample_1", null);
        }

        // expect good (200) status codes for all steps for updatedTestSettings2 testing for provider names including underscores
        List<validationData> expectations2 = vData.addSuccessStatusCodes();

        // Check if we got authorization code
        expectations2 = vData.addExpectation(expectations2, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                             Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations2 = vData.addExpectation(expectations2, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                             Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations2 = vData.addExpectation(expectations2, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                             "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations2 = vData.addExpectation(expectations2, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                             "false");

        //changed testSettings to updated
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);
        genericOP(_testName, wc2, updatedTestSettings2, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations2);
    }

    /**
     * TestDescription:
     *
     * This testcase performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code". In this scenario, the client uses the
     * authorization server as an intermediary to obtain the authorization code
     * and then uses this authorization code to request access token from the
     * token endpoint. The authorization server authenticates the resource owner
     * before issuing the authorization code which is redirected to the client.
     * oauth-roles uses the special subject of ALL_AUTHENTICATED_USERS.
     * In this scenario, the autoauthz parameter is set to true, so the resource
     * owner does not receive the consent form from the authorizarion server.
     * The test verifies that the Oauth code flow, using the authorization grant
     * type of "authorization code" works correctly.
     *
     */
    @Test
    public void testAuthCodeBasicFlowAllAuthOAuthRoles() throws Exception {

        //		testOPServer.restartServer("server_oauth_roles_all_auth.xml", _testName, Constants.NO_EXTRA_APPS, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
        testOPServer.reconfigServer("server_oauth_roles_all_auth.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.PERFORM_LOGIN);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");

        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * This testcase performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code". In this scenario, the client uses the
     * authorization server as an intermediary to obtain the authorization code
     * and then uses this authorization code to request access token from the
     * token endpoint. The authorization server authenticates the resource owner
     * before issuing the authorization code which is redirected to the client.
     * oauth-roles uses the special subject of ALL_AUTHENTICATED_USERS.
     * In this scenario, the autoauthz parameter is set to true, so the resource
     * owner does not receive the consent form from the authorizarion server.
     * The test verifies that the Oauth code flow, using the authorization grant
     * type of "authorization code" works correctly.
     *
     */
    @Test
    public void testAuthCodeBasicFlowAllAuthOAuthAndAuthRoles() throws Exception {

        //testOPServer.restartServer("server_oauth_and_auth_roles_all_auth.xml", _testName, Constants.NO_EXTRA_APPS, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
        testOPServer.reconfigServer("server_oauth_and_auth_roles_all_auth.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.PERFORM_LOGIN);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");

        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * This testcase performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code". In this scenario, the client uses the
     * authorization server as an intermediary to obtain the authorization code
     * and then uses this authorization code to request access token from the
     * token endpoint. The authorization server authenticates the resource owner
     * before issuing the authorization code which is redirected to the client.
     * oauth-roles uses the special subject of ALL_AUTHENTICATED_USERS.
     * In this scenario, the autoauthz parameter is set to true, so the resource
     * owner does not receive the consent form from the authorizarion server.
     * The test verifies that the Oauth code flow, using the authorization grant
     * type of "authorization code" works correctly.
     *
     */
    @Test
    public void testAuthCodeBasicFlowAllAuthAuthRoles() throws Exception {

        //testOPServer.restartServer("server_auth_roles_all_auth.xml", _testName, Constants.NO_EXTRA_APPS, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
        testOPServer.reconfigServer("server_auth_roles_all_auth.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.PERFORM_LOGIN);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");

        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * This testcase performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code" . In this scenario, the client uses the
     * authorization server as an intermediary to obtain the authorization code
     * and then uses this authorization code to request access token from the
     * token endpoint. The authorization server authenticates the resource owner
     * before issuing the authorization code which is redirected to the client.
     * In this scenario, the autoauthz parameter is set to false, so the
     * resource owner receives the consent form from the authorizarion server.
     * The test verifies that for "authorization code" grant type, when the
     * autoauthz parameter is set to false, the resource owner receives the
     * consent form.
     *
     */
    @Test
    public void testAuthCodeApprovalForm() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientName("client02");
        updatedTestSettings.setClientID("client02");
        updatedTestSettings.setAutoAuthz("false");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got the approval form
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get the approval form", null,
                                            Constants.APPROVAL_FORM);
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not get the approval form", null,
                                            Constants.APPROVAL_HEADER);

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * This test case tests the authorization grant type of
     * "authorization code". In this scenario, the client obtains authorization
     * code by invoking the authorization endpoint and sends the authorization
     * code to the token endpoint to get the access token. The client then
     * invokes a protected resource by sending the access token. In this
     * sceanrio, the "autoauthz" paramter is set to true which will bypass the
     * consent or approval form.
     *
     */
    @Test
    public void testAuthCodeBadCreds() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminPswd("badPswd");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Did not get the login page again (due to the bad password)", null, Constants.LOGIN_PROMPT);
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Did not get the login page again (due to the bad password)", null, Constants.LOGIN_ERROR);
        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.PERFORM_LOGIN);

        testOPServer.addIgnoredServerExceptions("CWIML4537E");

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * This test case tests the authorization grant type of
     * "authorization code". In this scenario, the client obtains authorization
     * code by invoking the authorization endpoint and sends the authorization
     * code to the token endpoint to get the access token. The client then
     * invokes a protected resource by sending the access token. In this
     * sceanrio, the "autoauthz" paramter is set to true which will bypass the
     * consent or approval form.
     *
     */
    @Test
    public void testAuthCodeBadToken() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        WebResponse response = null;

        // add good expected status codes for everything except validation endpoint
        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_PROTECTED_RESOURCE);

        response = genericOP(_testName, wc, testSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);

        // setup the empty token value
        List<endpointSettings> parms = eSettings.addEndpointSettings(null, "access_token", validationTools.getTokenFromResponse(response, Constants.ACCESS_TOKEN_KEY) + "extra");

        // Second request should be unauthorized
        // origin expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.EXCEPTION_MESSAGE, Constants.STRING_CONTAINS, "Did not get expected 401 exception", null, "HTTP response code: 401");
        expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.JSON_OBJECT, Constants.STRING_CONTAINS,
                                            "Did not receive 'OAuth service failed the request'", Constants.ERROR_RESPONSE_DESCRIPTION, "OAuth service failed the request");

        response = genericInvokeEndpoint(_testName, wc, response, testSettings.getProtectedResource(), Constants.GETMETHOD, Constants.INVOKE_PROTECTED_RESOURCE, parms, null,
                                         expectations);

    }

    /**
     * TestDescription:
     *
     * This scenario tests the authorization grant type of "auth code" when the TAI properties
     * are defined in the provider configuration file, instead of in the WAS security.xml.
     * In this scenario, the OAuth provider file contains the following TAI properties:
     * <parameter name="filter" type="tai" customizable="true">
     * <value>request-url%=snooping</value>
     * </parameter>
     * <parameter name="oauthOnly" type="tai" customizable="true">
     * <value>true</value>
     * </parameter>
     * The test verifies that the OAuth provider TAI properties can be processed successfully
     * from the provider configuration file.
     *
     */
    @Test
    public void testAuthCodeProviderConfig() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAuthorizeEndpt(eSettings.assembleEndpoint(testOPServer.getHttpsString(), Constants.ENDPOINT_TYPE, testSettings.getConfigTAIProvider(),
                                                                         Constants.AUTHORIZE_ENDPOINT));
        updatedTestSettings.setTokenEndpt(eSettings.assembleEndpoint(testOPServer.getHttpsString(), Constants.ENDPOINT_TYPE, testSettings.getConfigTAIProvider(),
                                                                     Constants.TOKEN_ENDPOINT));
        updatedTestSettings.setProtectedResource(eSettings.assembleProtectedResource(testOPServer.getHttpsString(), testSettings.getConfigTAI(), Constants.SNOOPING));

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * This testcase performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code" . In this scenario, the client uses the
     * authorization server as an intermediary to obtain the authorization code
     * and then uses this authorization code to request access token from the
     * token endpoint. The authorization server authenticates the resource owner
     * before issuing the authorization code which is redirected to the client.
     * In this scenario, the autoauthz parameter is set to false, so the
     * resource owner receives the consent form from the authorizarion server.
     * The test verifies that for "authorization code" grant type, when the
     * autoauthz parameter is set to false, the resource owner receives the
     * consent custom consent form.
     *
     */
    @Test
    public void testAuthCodeCustomApproval() throws Exception {

        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKS1403I.*" + Constants.OAUTHCONFIGSAMPLE_START_APP);
        // reconfigure the server to use a config with a custom consent form
        testOPServer.reconfigServer("server_consentform.xml", _testName, Constants.JUNIT_REPORTING, startMsgs);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientName("client02");
        updatedTestSettings.setClientID("client02");
        updatedTestSettings.setAutoAuthz("false");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got the approval form
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get the approval form", null,
                                            Constants.APPROVAL_FORM);
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not get the approval form", null,
                                            Constants.CUSTOM_APPROVAL_HEADER);

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * This testcase performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code". In this scenario, the client uses the
     * authorization server as an intermediary to obtain the authorization code
     * and then uses this authorization code to request access token from the
     * token endpoint. The authorization server authenticates the resource owner
     * before issuing the authorization code which is redirected to the client.
     * In this scenario, the autoauthz parameter is set to true, so the resource
     * owner does not receive the consent form from the authorizarion server.
     * The test verifies that the Oauth code flow, using the authorization grant
     * type of "authorization code" works correctly.
     *
     */
    @Test
    public void testAuthCodeCustomLogin() throws Exception {

        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKS1403I.*" + Constants.OAUTHCONFIGSAMPLE_START_APP);
        // reconfigure the server to use a config with a custom consent form
        testOPServer.reconfigServer("server_loginform.xml", _testName, Constants.JUNIT_REPORTING, startMsgs);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Make sure we got the custom login page
        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not land on custom login page",
                                            null, Constants.CUSTOM_LOGIN_TITLE);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");

        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * This testcase performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code". In this scenario, the client uses the
     * authorization server as an intermediary to obtain the authorization code
     * and then uses this authorization code to request access token from the
     * token endpoint. The authorization server authenticates the resource owner
     * before issuing the authorization code which is redirected to the client.
     * In this scenario, the autoauthz parameter is set to true, so the resource
     * owner does not receive the consent form from the authorizarion server.
     * The test verifies that the Oauth code flow, using the authorization grant
     * type of "authorization code" works correctly.
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuthCodeCustomError() throws Exception {

        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKS1403I.*" + Constants.OAUTHCONFIGSAMPLE_START_APP);
        // reconfigure the server to use a config with a custom consent form
        testOPServer.reconfigServer("server_loginform.xml", _testName, Constants.JUNIT_REPORTING, startMsgs);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminPswd("badPswd");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Make sure we got the custom login page
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS,
                                            "Did not land on custom error page", null, Constants.CUSTOM_ERROR_TITLE);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                            "Received authorization code and should not have", null, Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN,
                                            "Received access token and should not have", null, Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.PERFORM_LOGIN);

        testOPServer.addIgnoredServerExceptions("CWIML4537E");

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * The test case call login with customLoginNE.jsp
     * When it login error with badPswd, it does not expect to get the customError.jsp
     * Since there are none defined in customLoginNE.jsp
     * It ought to get the login.jsp?error=error
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuthCodeCustomNEError() throws Exception {
        if (!LDAPUtils.USE_LOCAL_LDAP_SERVER) {
            List<String> startMsgs = new ArrayList<String>();
            startMsgs.add("CWWKS1403I.*" + Constants.OAUTHCONFIGSAMPLE_START_APP);
            // reconfigure the server to use a config with a custom consent form
            testOPServer.reconfigServer("server_loginform_ne.xml", _testName, Constants.JUNIT_REPORTING, startMsgs);

            // Create the conversation object which will maintain state for us
            WebConversation wc = new WebConversation();

            TestSettings updatedTestSettings = testSettings.copyTestSettings();
            updatedTestSettings.setAdminPswd("badPswd");

            // expect good (200) status codes for all steps
            List<validationData> expectations = vData.addSuccessStatusCodes(null);

            // Check if we got authorization code
            expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                "Did not get the login page again (due to the bad password)", null, Constants.LOGIN_PROMPT);
            expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                "Did not get the login page again (due to the bad password)", null, Constants.LOGIN_ERROR);
            expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.PERFORM_LOGIN);

            testOPServer.addIgnoredServerExceptions("CWIML4537E");

            genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);

        }
    }

    /**
     * TestDescription:
     *
     * This testcase performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code". In this scenario, the client uses the
     * authorization server as an intermediary to obtain the authorization code
     * and then uses this authorization code to request access token from the
     * token endpoint. The authorization server authenticates the resource owner
     * before issuing the authorization code which is redirected to the client.
     * In this scenario, the autoauthz parameter is set to true, so the resource
     * owner does not receive the consent form from the authorizarion server.
     * The test verifies that the Oauth code flow, using the authorization grant
     * type of "authorization code" works correctly.
     *
     */
    //chc@Mode(TestMode.LITE)
    @Test
    public void testAuthCodeDeclarativeAuthz() throws Exception {

        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKS1403I.*" + Constants.OAUTHCONFIGSAMPLE_START_APP);
        // reconfigure the server to use a config with a custom consent form
        testOPServer.reconfigServer("server_loginform.xml", _testName, Constants.JUNIT_REPORTING, startMsgs);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAuthorizeEndpt(eSettings.assembleEndpoint(testOPServer.getHttpsString(), Constants.DECLARATIVE_TYPE, testSettings.getConfigSample(),
                                                                         Constants.AUTHORIZE_ENDPOINT));

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Make sure we did NOT get the custom login page
        //expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_TITLE, Constants.STRING_MATCHES, "Did not land on custom login page", null, Constants.CUSTOM_LOGIN_TITLE);
        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_TITLE, Constants.STRING_MATCHES,
                                            "Did not land on the default login page", null, Constants.LOGIN_TITLE);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * This testcase performs a simple end-end OAuth flow with a provider that
     * is correctly defined - OAuthConfigSample. Then the provider is modified to
     * delete the necessary client, client01, and the same flow is attempted again.
     * The request should fail with a message that the client was not found. Then
     * the client is added back and the flow is attempted again, this one should
     * succeed.
     *
     */
    @Test
    @ExpectedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidClientException")
    public void testAuthCodeDynamicFlow() throws Exception {

        // -------------------------------------------------------------------
        // expectations for when the config conatins client01
        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");

        // --------------------------

        // expectations for when the config does NOT contain client01
        // expect good (200) status codes for all steps
        List<validationData> missingExpectations = vData.addSuccessStatusCodes(null);

        // Check if we got authorization code
        missingExpectations = vData.addExpectation(missingExpectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                                   "Did not receive client01 not found response", null, Constants.CLIENT_COULD_NOT_BE_FOUND);
        missingExpectations = vData.addNoTokensInResponseExpectations(missingExpectations, Constants.PERFORM_LOGIN);

        // -------------------------------------------------------------------

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // invoke - expect to get to protected resource
        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

        // ---------------------------------------------------------------
        Log.info(thisClass, _testName, "Reconfigure the server - removing client01");

        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKS1403I.*" + Constants.OAUTHCONFIGSAMPLE_START_APP);
        // reconfigure the server to use a config with client01 missing
        testOPServer.reconfigServer("server_noclient.xml", _testName, Constants.JUNIT_REPORTING, startMsgs);

        // start a new converstation
        wc = new WebConversation();
        // run the same tests again and expect good results - get to protected resource
        genericOP(_testName, wc, testSettings, Constants.SUBMIT_ACTIONS, missingExpectations);

        // ---------------------------------------------------------------
        Log.info(thisClass, _testName, "Restore  the original server");

        // restore the original server - that contains client01
        testOPServer.restoreServer();

        // start a new converstation
        wc = new WebConversation();
        // run the same tests again and expect good results - get to protected resource
        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * This testcase performs a simple end-end OAuth flow with a provider that
     * has a custom mediator plugin. The custom mediator will fail all requests,
     * The test is designed to verify that the mediator is loaded and invoked.
     *
     */
    @Test
    @ExpectedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20MediatorException")
    public void testOAuthMediatorAuthCodeFlow() throws Exception {

        // make sure that the server is fully operational - previous tests will have reconfigured and the mediator shared library can't not be reloaded during just a reconfig
        //testOPServer.restartServer(, testName, checkApps, reportViaJunit, startMessages)
        ArrayList<String> extraMsgs = new ArrayList<String>();
        extraMsgs.add("CWWKS1403I.*" + Constants.OAUTHCONFIGSAMPLE_START_APP);
        extraMsgs.add("CWWKS1403I.*" + Constants.OAUTHCONFIGMEDIATOR_START_APP);
        //		testOPServer.restartServer("server_orig.xml", _testName, Constants.NO_EXTRA_APPS, Constants.JUNIT_REPORTING, extraMsgs);
        testOPServer.reconfigServer("server_orig.xml", _testName, Constants.JUNIT_REPORTING, extraMsgs);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientName("mediatorclient");
        updatedTestSettings.setClientID("mediatorclient");
        updatedTestSettings.setAuthorizeEndpt(eSettings.assembleEndpoint(testOPServer.getHttpsString(), Constants.ENDPOINT_TYPE, testSettings.getConfigMediator(),
                                                                         Constants.AUTHORIZE_ENDPOINT));
        updatedTestSettings.setTokenEndpt(eSettings.assembleEndpoint(testOPServer.getHttpsString(), Constants.ENDPOINT_TYPE, testSettings.getConfigMediator(),
                                                                     Constants.TOKEN_ENDPOINT));
        updatedTestSettings.setProtectedResource(eSettings.assembleProtectedResource(testOPServer.getHttpsString(), testSettings.getConfigTAI(), Constants.SNORKING));

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.PERFORM_LOGIN);
        expectations = vData.addResponseStatusExpectation(expectations, Constants.PERFORM_LOGIN, Constants.BAD_REQUEST_STATUS);

        // now, we want to add expecations that actually validate responses received during the flow of the generic test.
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code ", null,
                                            Constants.RECV_AUTH_CODE);
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Mediator did not fail the request ", null,
                                            "test deliberate fail");

        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive login page", null,
                                            Constants.LOGIN_PROMPT);

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * This is a negative test case for the authorization grant type of
     * "authorization code", using OAuth provider that uses XML file for storing
     * registered clients. There is no TAI filter property defined for the OAuth
     * provider that is used in this scenario. Since the filter property is not
     * defined for this OAuth request, the request is expected to be rejected
     * with an internal server exception. This test verifies that the OAuth
     * filter property (provider_<id>.filter) is required for processing the
     * OAuth request.
     *
     */
    @Test
    public void testAuthCodeBasicFlowNoFilter() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAuthorizeEndpt(eSettings.assembleEndpoint(testOPServer.getHttpsString(), Constants.ENDPOINT_TYPE, testSettings.getConfigNoFilter(),
                                                                         Constants.AUTHORIZE_ENDPOINT));
        updatedTestSettings.setTokenEndpt(eSettings.assembleEndpoint(testOPServer.getHttpsString(), Constants.ENDPOINT_TYPE, testSettings.getConfigNoFilter(),
                                                                     Constants.TOKEN_ENDPOINT));
        updatedTestSettings.setProtectedResource(eSettings.assembleProtectedResource(testOPServer.getHttpsString(), testSettings.getConfigTAI(), Constants.SNOOPING));
        updatedTestSettings.setClientName("nclient01");
        updatedTestSettings.setClientID("nclient01");

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_PROTECTED_RESOURCE);

        // Should get a 401
        //expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.EXCEPTION_MESSAGE, Constants.STRING_CONTAINS, "Did not get expected 401 exception", null, "HTTP response code: 401");
        expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.JSON_OBJECT, Constants.STRING_CONTAINS,
                                            "Did not receive 'OAuth service failed the request'", Constants.ERROR_RESPONSE_DESCRIPTION, "OAuth service failed the request");

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * This scenario tests the oauthOnly TAI property of OAuth TAI. The test
     * invokes a protected resource which is protected by an OAuth provider that
     * has oauthOnly property set to true and verifies that the request is
     * rejected without prompting for the login form.
     *
     */
    @Test
    public void testAuthCodeBasicFlowOAuthOnlyTrue() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientName("dclient01");
        updatedTestSettings.setClientID("dclient01");

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_PROTECTED_RESOURCE);

        // Should get a 401
        //expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.EXCEPTION_MESSAGE, Constants.STRING_CONTAINS, "Did not get expected 401 exception", null, "HTTP response code: 401");
        expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.JSON_OBJECT, Constants.STRING_CONTAINS,
                                            "Did not receive 'OAuth service failed the request'", Constants.ERROR_RESPONSE_DESCRIPTION, "OAuth service failed the request");

        genericOP(_testName, wc, updatedTestSettings, Constants.ONLY_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * This scenario tests the oauthOnly TAI property of OAuth TAI. The test invokes a
     * protected resource that has the oauthOnly property set to false, it
     * verifies that the application can be invoked after a successful login.
     *
     */
    @Test
    public void testAuthCodeBasicFlowOAuthOnlyFalse() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientName("dclient01");
        updatedTestSettings.setClientID("dclient01");
        updatedTestSettings.setProtectedResource(eSettings.assembleProtectedResource(testOPServer.getHttpsString(), testSettings.getConfigTAI(), Constants.SSODEMO));

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_PROTECTED_RESOURCE);
        //expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.UNAUTHORIZED_STATUS)  ;

        // Check if we got authorization code
        // The failure caused by the continue result and actually failed at:
        //   com.ibm.ws.webcontainer.security.internal.BasicAuthAuthenticator
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.EXCEPTION_MESSAGE, Constants.STRING_CONTAINS,
                                            "Did NOT get expected exception", null, Integer.toString(Constants.UNAUTHORIZED_STATUS));

        genericOP(_testName, wc, updatedTestSettings, Constants.ONLY_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * This is a negative test case for the authorization grant type of
     * "authorization code", using OAuth provider that uses XML file for storing
     * registered clients. In this scenario, a non-registered client tries to
     * obtain authorization code from the authorization server. Since the client
     * is not registered, the request is expected to be rejected with an
     * appropriate exception. This test verifies that the OAuth authorization
     * server will reject any unregistered OAuth client if it tries to obtain
     * access token.
     *
     */
    @Test
    @ExpectedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidClientException")
    public void testAuthCodeUnRegClient() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientName("unreg01");
        updatedTestSettings.setClientID("unreg01");

        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Did not receive " + updatedTestSettings.getClientID() + " not found response", null, Constants.CLIENT_COULD_NOT_BE_FOUND);
        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.SUBMIT_TO_AUTH_SERVER);
        genericOP(_testName, wc, updatedTestSettings, Constants.SUBMIT_ACTIONS, expectations);

    }

    /***
     * Test Purpose: Override the default response_type with an invalid value of
     * "bob" Ensure that the value is detected and that an error is returned.
     * The response contains the error, but the status code returned is a 200
     *
     * @throws Exception
     */
    @Test
    @ExpectedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidResponseTypeException")
    public void testWebClientResponseTypeBad() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setResponseType("bob");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not fail on bad response type: ", null,
                                            "unsupported_response_type");
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not fail on bad response type: ", null,
                                            "CWOAU0027E%3A+The+response_type+parameter+was+invalid%3A+bob");
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not fail on bad response type: ", null,
                                            "Something went wrong redirecting to redirect.jsp");
        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.PERFORM_LOGIN);
        genericOP(_testName, wc, updatedTestSettings, Constants.SUBMIT_ACTIONS, expectations);
        //genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);

    }

    /***
     * Test Purpose: Override the default response_type with a value that
     * contains first an invalid value and then a valid type. Ensure that the
     * invalid value is detected and that an error is returned. The response
     * contains the error, but the status code returned is a 200
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidResponseTypeException" })
    public void testWebClientResponseTypeBadWithGood() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setResponseType("bob code");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not fail on bad response type: ", null,
                                            "error=unsupported_response_type");
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not fail on bad response type: ", null,
                                            "Something went wrong redirecting to redirect.jsp");
        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.PERFORM_LOGIN);
        genericOP(_testName, wc, updatedTestSettings, Constants.SUBMIT_ACTIONS, expectations);
        //genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);

    }

    /***
     * Test Purpose: Override the default response_type with a value that
     * contains first a valid value, then an invalid type. Ensure that the
     * invalid value is detected and that an error is returned. The response
     * contains the error, but the status code returned is a 200
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidResponseTypeException" })
    public void testWebClientResponseTypeBadWithGoodFirst() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setResponseType("code bob");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not fail on bad response type: ", null,
                                            "error=unsupported_response_type");
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not fail on bad response type: ", null,
                                            "Something went wrong redirecting to redirect.jsp");
        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.PERFORM_LOGIN);
        genericOP(_testName, wc, updatedTestSettings, Constants.SUBMIT_ACTIONS, expectations);
        //genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);

    }

    /***
     * Test Purpose: Good end to end flow - With an OAuth provider, ensure that
     * there is no id_token in the response. With an OIDC provider, ensure that
     * the id_token is in the response. Also verify the content of the id_token
     * - all required parms are included and make sure that the values in those
     * required parms are correct. Status code returned should be 200.
     *
     * @throws Exception
     */
    //chc @Mode(TestMode.LITE)
    @Test
    public void testWebClient_validateIDToken() throws Exception {
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");

        // add generic id_token expectations
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, testSettings);
        // add generic response expectations
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, testSettings);

        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /***
     * Test Purpose: Specify scope without "openid". This is allowed for both
     * OAuth and OIDC. For OAuth - it does not return an id_token anyway. OIDC
     * will allow the omission, but will not return the id_token. The Status
     * code returned should be 200.
     *
     * @throws Exception
     */
    //chc@Mode(TestMode.LITE)
    @Test
    public void testWebClient_missingOpenidInScope() throws Exception {
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("scope1 scope2");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);

        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive login page", null,
                                            Constants.LOGIN_PROMPT);

        // make sure that id_token is NOT in the response
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS,
                                            "Token validate response found the id_token in the response and should not have", Constants.ID_TOKEN_KEY, Constants.NOT_FOUND);

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);

    }

    /***
     * Test Purpose: Specify none for signatureAlgorithm. Check for alg set to
     * none and there to be nothing in the 3rd part of the id_token, there
     * should be no nonce in the payload of the id_token and status code
     * returned should be 200.
     *
     * @throws Exception
     */
    @Test
    public void testWebClientNotSigned() throws Exception {

        //testOPServer.restartServer("server_notSigned.xml", _testName, Constants.NO_EXTRA_APPS, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
        testOPServer.reconfigServer("server_notSigned.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setSignatureAlg(Constants.SIGALG_NONE);

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        // add generic id_token expectations
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);
        // add generic r esponse expectations
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);
    }

    /***
     * Test Purpose: Specify none for signatureAlgorithm. Check for alg set to
     * RS256 and there to be a 3rd part of the id_token, status code returned
     * should be 200.
     *
     * @throws Exception
     */
    @Test
    //Until 132178 is complete
    @Ignore("See: https://github.com/OpenLiberty/open-liberty/issues/16014")
    public void testWebClientRS256() throws Exception {

        //testOPServer.restartServer("server_RS256.xml", _testName, Constants.NO_EXTRA_APPS, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
        testOPServer.reconfigServer("server_RS256.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setSignatureAlg(Constants.SIGALG_RS256);

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        // add generic id_token expectations
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);
        // add generic response expectations
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /***
     * Test Purpose: Specify none for signatureAlgorithm. Check for alg set to
     * HS256 and there to be a 3rd part of the id_token, status code returned
     * should be 200.
     *
     * @throws Exception
     */
    @Test
    public void testWebClientHS256() throws Exception {

        //testOPServer.restartServer("server_HS256.xml", _testName, Constants.NO_EXTRA_APPS, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);
        testOPServer.reconfigServer("server_HS256.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setSignatureAlg(Constants.SIGALG_HS256);

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        // add generic id_token expectations
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);
        // add generic response expectations
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /***
     * Test Purpose: Set the authorizeeEndpoint url using the http port instead of the https port
     * Response should contain a 404 status code and a message that https must be used.
     *
     * @throws Exception
     */
    @Test
    public void testWebHttpAuthorizeEndpt() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        if (eSettings.getProviderType() == Constants.OAUTH_OP) {
            updatedTestSettings.setAuthorizeEndpt(testOPServer.getHttpString() + updatedTestSettings.getDetailOAuthOPAuthorizeEndpt());
        } else {
            updatedTestSettings.setAuthorizeEndpt(testOPServer.getHttpString() + updatedTestSettings.getDetailOIDCOPAuthorizeEndpt());
        }

        // expect good (200) status codes for all steps
        String expected404Step = Constants.SUBMIT_TO_AUTH_SERVER;
        List<validationData> expectations = validationTools.getDefault404VDataExpectationsWithOtherwiseSuccessfulStatusCodes(expected404Step);
        // we expect a 404 as https is required
        expectations = vData.addNoTokensInResponseExpectations(expectations, expected404Step);

        testOPServer.addIgnoredServerExceptions("CWOAU0037E");

        genericOP(_testName, wc, updatedTestSettings, Constants.SUBMIT_ACTIONS, expectations);

    }

    /***
     * Test Purpose: Set the authorizeeEndpoint url using the http port instead of the https port
     * Response should contain a 404 status code and a message that https must be used.
     *
     * @throws Exception
     */
    @Test
    public void testWebHttpTokenEndpt() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        if (eSettings.getProviderType() == Constants.OAUTH_OP) {
            updatedTestSettings.setTokenEndpt(testOPServer.getHttpString() + updatedTestSettings.getDetailOAuthOPTokenEndpt());
        } else {
            updatedTestSettings.setTokenEndpt(testOPServer.getHttpString() + updatedTestSettings.getDetailOIDCOPTokenEndpt());
        }

        // expect good (200) status codes for all steps except login
        String expected404Step = Constants.PERFORM_LOGIN;
        List<validationData> expectations = validationTools.getDefault404VDataExpectationsWithOtherwiseSuccessfulStatusCodes(expected404Step);
        // we expect a 404 as https is required
        expectations = vData.addNoTokensInResponseExpectations(expectations, expected404Step);

        testOPServer.addIgnoredServerExceptions("CWOAU0037E");

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);
    }

    /***
     * Test Purpose: Set the authorizeeEndpoint url using the http port instead of the https port
     * Response should contain a 404 status code and a message that https must be used.
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testWebRefreshToken() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // expect good (200) status codes for all steps
        //List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.SUBMIT_TO_AUTH_SERVER);
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // add generic id_token expectations
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, testSettings);
        // add generic response expectations
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, testSettings);
        WebResponse response = genericOP(_testName, wc, testSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);

        String originalRefreshToken = validationTools.getTokenFromResponse(response, Constants.REFRESH_TOKEN_KEY);
        String originalAccessToken = validationTools.getTokenFromResponse(response, Constants.ACCESS_TOKEN_KEY);

        // with JWT tokens, we call refresh too fast - it ends up creating the same exp in the token and generates a new but identical token
        // sleep so that the iat and exp will be different in the new token...
        helpers.testSleep(2);

        // Set up parameters for refresh.jsp to get new refresh and access token
        List<endpointSettings> parms = eSettings.addEndpointSettings(null, "refresh_token", originalRefreshToken);
        parms = eSettings.addEndpointSettings(parms, "client_id", testSettings.getClientID());
        parms = eSettings.addEndpointSettings(parms, "client_secret", testSettings.getClientSecret());
        parms = eSettings.addEndpointSettings(parms, "token_endpoint", testSettings.getTokenEndpt());
        parms = eSettings.addEndpointSettings(parms, "scope", testSettings.getScope());

        expectations = vData.addExpectation(expectations, Constants.INVOKE_REFRESH_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token",
                                            null, Constants.RECV_FROM_TOKEN_ENDPOINT);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_REFRESH_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not update refresh token",
                                            null, Constants.REFRESH_TOKEN_UPDATED);

        // add generic id_token expectations - for now, the id_token IS NOT returned in the refresh response
        // WHEN IF ID_TOKEN is added, this test will break and act as a reminder that we need to validate the id_token, but not only
        // validate it, validate it against the original and contents of the access token
        //expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.INVOKE_REFRESH_ENDPOINT, updatedTestSettings);
        // add generic response expectations
        // for now since the id_token will not be in the response, let's pretend this is an oAuth flow which omits id_token
        //expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.INVOKE_REFRESH_ENDPOINT, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, Constants.OAUTH_OP, Constants.INVOKE_REFRESH_ENDPOINT, testSettings);
        response = genericInvokeForm(_testName, wc, response, testSettings, testSettings.getRefreshTokUrl(), Constants.GETMETHOD, Constants.INVOKE_REFRESH_ENDPOINT, parms,
                                     expectations);

        String updatedRefreshToken = validationTools.getTokenFromResponse(response, Constants.REFRESH_TOKEN_KEY);
        String updatedAccessToken = validationTools.getTokenFromResponse(response, Constants.ACCESS_TOKEN_KEY);

        msgUtils.assertTrueAndLog(_testName, "The access token was NOT updated", !originalAccessToken.equals(updatedAccessToken));
        msgUtils.assertTrueAndLog(_testName, "The refresh token was NOT updated", !originalRefreshToken.equals(updatedRefreshToken));

        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);

        helpers.invokeProtectedResource(_testName, wc, originalAccessToken, testSettings, expectations);
        helpers.invokeProtectedResource(_testName, wc, updatedAccessToken, testSettings, expectations);
    }

    /***
     * Test Purpose: Set the authorizeeEndpoint url using the http port instead of the https port
     * Response should contain a 404 status code and a message that https must be used.
     *
     * @throws Exception
     */
    // commented out until 124815 is fixed - http is NOT disallowed and should be
    //@Test
    public void testWebHttpRefreshToken() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        if (eSettings.getProviderType() == Constants.OAUTH_OP) {
            updatedTestSettings.setRefreshTokUrl(testOPServer.getHttpString() + updatedTestSettings.getDetailOAuthOPRefreshTokUrl());
        } else {
            updatedTestSettings.setRefreshTokUrl(testOPServer.getHttpString() + updatedTestSettings.getDetailOIDCOPRefreshTokUrl());
        }

        // expect good (200) status codes for all steps
        //List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.SUBMIT_TO_AUTH_SERVER);
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // add generic id_token expectations for login step
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);
        // add generic response expectations
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);
        WebResponse response = genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);

        String originalRefreshToken = validationTools.getTokenFromResponse(response, Constants.REFRESH_TOKEN_KEY);

        // Set up parameters for refresh.jsp to get new refresh and access token
        List<endpointSettings> parms = eSettings.addEndpointSettings(null, "refresh_token", originalRefreshToken);
        parms = eSettings.addEndpointSettings(parms, "client_id", updatedTestSettings.getClientID());
        parms = eSettings.addEndpointSettings(parms, "client_secret", updatedTestSettings.getClientSecret());
        parms = eSettings.addEndpointSettings(parms, "token_endpoint", updatedTestSettings.getTokenEndpt());
        parms = eSettings.addEndpointSettings(parms, "scope", updatedTestSettings.getScope());

        // we expect a 404 as https is required
        expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_REFRESH_ENDPOINT, Constants.NOT_FOUND_STATUS);
        expectations = vData
                        .addExpectation(expectations, Constants.INVOKE_REFRESH_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                        "Did not fail because http was used instead of https", null,
                                        "Error 404: CWOAU0037E: HTTP scheme is used at the specified endpoint: " + updatedTestSettings.getTokenEndpt() + ", HTTPS is required.");

        response = genericInvokeForm(_testName, wc, response, updatedTestSettings, updatedTestSettings.getRefreshTokUrl(), Constants.GETMETHOD, Constants.INVOKE_REFRESH_ENDPOINT,
                                     parms, expectations);

    }

    /***
     * Test Purpose: Set the authorizeeEndpoint url using the http port instead of the https port
     * Response should contain a 404 status code and a message that https must be used.
     *
     * @throws Exception
     */
    @Test
    public void testWebHttpProtectedResource() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        if (eSettings.getProviderType() == Constants.OAUTH_OP) {
            updatedTestSettings.setProtectedResource(testOPServer.getHttpString() + updatedTestSettings.getDetailOAuthOPProtectedResource());
        } else {
            updatedTestSettings.setProtectedResource(testOPServer.getHttpString() + updatedTestSettings.getDetailOIDCOPProtectedResource());
        }

        // expect good (200) status codes for all steps
        //List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.SUBMIT_TO_AUTH_SERVER);
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        // we expect a 404 as https is required
        //expectations = vData.addResponseStatusExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.NOT_FOUND_STATUS)  ;
        //expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not fail because http was used instead of https", null, "Error 404: CWOAU0037E: HTTP scheme is used at the specified endpoint: " + updatedTestSettings.getAuthorizeEndpt() + ", HTTPS is required.");

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     *
     * This testcase performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code". In this scenario, the client uses the
     * authorization server as an intermediary to obtain the authorization code
     * and then uses this authorization code to request access token from the
     * token endpoint. The authorization server authenticates the resource owner
     * before issuing the authorization code which is redirected to the client.
     * In this scenario, the autoauthz parameter is set to true, so the resource
     * owner does not receive the consent form from the authorizarion server.
     * The test verifies that the Oauth code flow, using the authorization grant
     * type of "authorization code" works correctly.
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuthCodeBasicFlowUseBasicAuth() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes();

        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");

        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS_WITH_BASIC_AUTH, expectations);

    }

    /**
     * TestDescription:
     *
     * This testcase performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code". In this scenario, the client uses the
     * authorization server as an intermediary to obtain the authorization code
     * and then uses this authorization code to request access token from the
     * token endpoint. The authorization server authenticates the resource owner
     * before issuing the authorization code which is redirected to the client.
     * In this scenario, the autoauthz parameter is set to true, so the resource
     * owner does not receive the consent form from the authorizarion server.
     * The test verifies that the Oauth code flow, using the authorization grant
     * type of "authorization code" works correctly.
     *
     *
     */
    @Mode(TestMode.LITE)
    @Test
    @ExpectedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20AccessDeniedException")
    public void testAuthCodeBasicFlowUseBasicAuthBadPw() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminPswd("badPswd");

        // expect bad (401) status codes for authorization token endpoint
        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.SUBMIT_TO_AUTH_SERVER);

        expectations = vData.addResponseStatusExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_MATCH,
                                            "Found ID token string but should not have.", null, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                                            "Do not get the error of the invalid user of Basic Authorization header", null,
                                            "CWWKS1440E:.*failed because the Authorization header.*failed to be verified");
        // CWWKS1440E: The login of the request failed because the Authorization header in the request failed to be verified as a valid user.

        testOPServer.addIgnoredServerExceptions("CWIML4537E", "CWWKS1440E", "SRVE8115W");

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATE_ACTIONS_WITH_BASIC_AUTH, expectations);

    }

    /**
     * TestDescription:
     *
     * This testcase performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code". In this scenario, the client uses the
     * authorization server as an intermediary to obtain the authorization code
     * and then uses this authorization code to request access token from the
     * token endpoint. The authorization server authenticates the resource owner
     * before issuing the authorization code which is redirected to the client.
     * In this scenario, the autoauthz parameter is set to true, so the resource
     * owner does not receive the consent form from the authorizarion server.
     * The test verifies that the Oauth code flow, using the authorization grant
     * type of "authorization code" works correctly.
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void testAuthCodeBasicFlowUseBasicAuthAutoAuthzFalse() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAutoAuthz("false");

        // expect good (200) status codes for all steps
        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got the approval form
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get the approval form", null,
                                            Constants.APPROVAL_FORM);
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_TITLE, Constants.STRING_CONTAINS, "Did not get the approval form", null,
                                            Constants.APPROVAL_HEADER);
        //List<validationData> expectations = vData.addSuccessStatusCodes();

        // Make sure we get to the app
        //expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        //expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null, "false");
        //genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS_WITH_BASIC_AUTH, expectations);
        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATE_ACTIONS_WITH_BASIC_AUTH, expectations);

    }

    /**
     * TestDescription:
     *
     * This testcase performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code". In this scenario, the client uses the
     * authorization server as an intermediary to obtain the authorization code
     * and then uses this authorization code to request access token from the
     * token endpoint. The authorization server authenticates the resource owner
     * before issuing the authorization code which is redirected to the client.
     * In this scenario, the autoauthz parameter is set to true, so the resource
     * owner does not receive the consent form from the authorizarion server.
     * The test verifies that the Oauth code flow, using the authorization grant
     * type of "authorization code" works correctly.
     *
     */
    @Mode(TestMode.LITE)
    @Test
    @ExpectedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20AccessDeniedException")
    public void testAuthCodeBasicFlowUseBasicAuthBadPswdAutoAuthzFalse() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAutoAuthz("false");

        updatedTestSettings.setAdminPswd("badPswd");

        // expect bad (401) status codes for authorization token endpoint
        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.SUBMIT_TO_AUTH_SERVER);

        expectations = vData.addResponseStatusExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_MATCH,
                                            "Found ID token string but should not have.", null, Constants.IDToken_STR);
        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                                            "Do not get the error of the invalid user of Basic Authorization header", null,
                                            "CWWKS1440E:.*failed because the Authorization header.*failed to be verified");
        // CWWKS1440E: The login of the request failed because the Authorization header in the request failed to be verified as a valid user.

        testOPServer.addIgnoredServerExceptions("CWIML4537E", "CWWKS1440E", "SRVE8094W", "SRVE8115W");

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATE_ACTIONS_WITH_BASIC_AUTH, expectations);

    }

    /**
     * TestDescription:
     *
     * This testcase attempts an end-to-end auth code flow.
     * It specifies a redirect-url that does not match what is in the server config.
     * This should fail and we should find error msgs in the server logs
     *
     */
    @ExpectedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException")
    @Mode(TestMode.LITE)
    @Test
    public void testAuthCodeBasicFlow_invalidRedirectURL() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientRedirect(updatedTestSettings.getClientRedirect().replace(".jsp", "2.jsp"));;

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes();

        // Make sure we get a failure about a bad redirect url
        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Did not fail because of a bad redirect URL", null, "CWOAU0062E");

        genericOP(_testName, wc, updatedTestSettings, Constants.SUBMIT_ACTIONS, expectations);

    }

    @Test
    public void testAuthCodeBasicFlowDynamicallyAddUser() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes();

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive authorization code", null,
                                            Constants.RECV_AUTH_CODE);
        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null,
                                            Constants.RECV_FROM_TOKEN_ENDPOINT);
        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                                            "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null,
                                            "false");

        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

        // Reconfigure to use new server config with new user added to the registry
        testOPServer.reconfigServer("server_withNewUser.xml", _testName, Constants.JUNIT_REPORTING, Constants.NO_EXTRA_MSGS);

        // Update test settings so that the new user credentials are used when logging in
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setAdminUser("newuser");
        updatedTestSettings.setAdminPswd("newuserpwd");

        genericOP(_testName, new WebConversation(), updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);
    }

}
