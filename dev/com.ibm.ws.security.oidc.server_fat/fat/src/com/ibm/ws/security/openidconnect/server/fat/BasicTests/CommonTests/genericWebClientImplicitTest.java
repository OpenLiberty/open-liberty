/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
public class genericWebClientImplicitTest extends CommonTest {

    //private static final Class<?> thisClass = genericWebClientImplicitTest.class;

    protected TestSettings setImplicitClientDefaultSettings() throws Exception {
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientName("client03");
        updatedTestSettings.setClientID("client03");
        updatedTestSettings.setNonce(Constants.DEFAULT_NONCE);
        return updatedTestSettings;

    }

    /**
     * TestDescription:
     * 
     * This test case performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "implicit" . In this scenario, the client uses the authorization
     * server as an intermediary to obtain the access token, without invoking
     * the token endpoint. The authorization server authenticates the resource
     * owner before issuing the access token. In this scenario, the autoauthz
     * parameter is set to true, so the resource owner does not receive the
     * consent form from the authorization server. The test verifies that the
     * Oauth code flow, using the authorization grant type of "implicit" works
     * correctly.
     * 
     */
    @Test
    public void testImplicit() throws Exception {
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setImplicitClientDefaultSettings();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // now, we want to add expecations that actually validate responses received during the flow of the generic test.
        // Check if we got the redirect access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not receive redirect access token",
                null, Constants.REDIRECT_ACCESS_TOKEN);

        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive login page", null,
                Constants.LOGIN_PROMPT);

        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.INVOKE_AUTH_SERVER);

        genericOP(_testName, wc, updatedTestSettings, Constants.IMPLICIT_AUTHENTICATION_ACTIONS, expectations);
    }

    /***
     * Test Purpose: Override the default response_type with an invalid value of
     * "bob" Ensure that the value is detected and that an error is returned.
     * The response contains the error, but the status code returned is a 200
     * 
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidResponseTypeException" })
    public void testImplicitClientResponseTypeBad() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setImplicitClientDefaultSettings();
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
        genericOP(_testName, wc, updatedTestSettings, Constants.IMPLICIT_AUTHENTICATION_ACTION_WITH_ERR, expectations); //@AV999

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
    public void testImplicitClientResponseTypeBadWithGood() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setImplicitClientDefaultSettings();
        updatedTestSettings.setResponseType("bob token");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not fail on bad response type: ", null,
                "error=unsupported_response_type");
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not fail on bad response type: ", null,
                "CWOAU0027E%3A+The+response_type+parameter+was+invalid%3A+bob%2Btoken");
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not fail on bad response type: ", null,
                "Something went wrong redirecting to redirect.jsp");
        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.PERFORM_LOGIN);
        genericOP(_testName, wc, updatedTestSettings, Constants.IMPLICIT_AUTHENTICATION_ACTION_WITH_ERR, expectations);

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
    public void testImplicitClientResponseTypeBadWithGoodFirst() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setImplicitClientDefaultSettings();
        updatedTestSettings.setResponseType("token bob");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not fail on bad response type: ", null,
                "error=unsupported_response_type");
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not fail on bad response type: ", null,
                "CWOAU0027E%3A+The+response_type+parameter+was+invalid%3A+token%2Bbob");
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not fail on bad response type: ", null,
                "Something went wrong redirecting to redirect.jsp");
        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.PERFORM_LOGIN);
        genericOP(_testName, wc, updatedTestSettings, Constants.IMPLICIT_AUTHENTICATION_ACTION_WITH_ERR, expectations);

    }

    /***
     * Test Purpose: Override the default response_type with a value of "token".
     * We will NOT get an id_token in the response with eithr OAuth or OIDC.
     * Verify that it is NOT included The response contains the error, but the
     * status code returned is a 200
     * 
     * @throws Exception
     */
    @Test
    public void testImplicitClientResponseTypeTokenOnly() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setImplicitClientDefaultSettings();
        updatedTestSettings.setResponseType("token");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not receive redirect access token",
                null, Constants.REDIRECT_ACCESS_TOKEN);

        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive login page", null,
                Constants.LOGIN_PROMPT);
        // Make sure that we don't get an id-token in the response - as this test only specifies "token" in the response type - scope still includes openid (in the OIDC version of the test)
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS,
                "Token validate response found the id_token in the response and should not have", Constants.ID_TOKEN_KEY, Constants.NOT_FOUND);
        genericOP(_testName, wc, updatedTestSettings, Constants.IMPLICIT_AUTHENTICATION_ACTIONS, expectations);

    }

    /***
     * Test Purpose: Override the default response_type with a value of
     * "id_token". This is not valid for OAuth - it does not support id_token.
     * It is not valid for OIDC either as it does not currently support only
     * id_token - it supports "id_token token" The response contains the error,
     * but the status code returned is a 200
     * 
     * AJC Allowed because the OIDC doesn't need the Exception
     * 
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "com.ibm.ws.security.openidconnect.server.plugins.OIDCUnsupportedResponseTypeException", "com.ibm.ws.security.openidconnect.server.plugins.OIDCMissingScopeException", "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidResponseTypeException" })
    public void testImplicitClientResponseTypeIdTokenOnly() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setImplicitClientDefaultSettings();
        updatedTestSettings.setResponseType("id_token");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive login page", null,
        //Constants.LOGIN_PROMPT);

        if (eSettings.getProviderType() == Constants.OAUTH_OP) {
            // since scope only contains "scope1 scope2" and no openid, we'll get an exception
            expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS,
                    "Did not receive an error about missing openid in scope with response_type set to id_token", null, "error=invalid_scope");
            //expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS,
            // "Did not receive an error about missing openid in scope with response_type set to id_token", null,
            // "When+calling+response_type+id_token%2C+one+of+the+scopes+has+to+be+%27openid%27");
            expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS,
                    "Did not receive an error about missing openid in scope with response_type set to id_token", null,
                    "%27openid%27+should+be+specified+as+scope+if+the+response_type+is+id_token");
        } else {
            // currently getting that id_token not allowed without token...  
            expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS,
                    "OIDC - id_token without token in response_type is not currently supported", null, "error=unsupported_response_type");
            expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS,
                    "OIDC - id_token without token in response_type is not currently supported", null,
                    "response_type+id_token+without+response_type+token+is+not+supported+for+now");

            /*
             * replace previous expectations with these when/if id_token is supported on it's own
             * // Check if we got authorization code
             * expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL,
             * Constants.STRING_CONTAINS,
             * "Did not receive redirect access token", null, Constants.redirectAccessToken );
             * // validate general as well as specific information in the id_token
             * expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_ID_TOKEN,
             * Constants.STRING_CONTAINS,
             * "The general content of the id_token was incorrect", null, null );
             * expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_ID_TOKEN,
             * Constants.STRING_CONTAINS,
             * "iat was not found in the id_token", Constants.IDTOK_ISSUETIME_KEY, "" );
             * expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_ID_TOKEN,
             * Constants.STRING_MATCHES,
             * "client id (aud) was not correct in the id_token", Constants.IDTOK_AUDIENCE_KEY, updatedTestSettings.getClientID()
             * );
             * expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_ID_TOKEN,
             * Constants.STRING_MATCHES,
             * "userid id (sub) was not correct in the id_token", Constants.IDTOK_SUBJECT_KEY, updatedTestSettings.getAdminUser()
             * );
             */
        }
        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.PERFORM_LOGIN);
        genericOP(_testName, wc, updatedTestSettings, Constants.IMPLICIT_AUTHENTICATION_ACTION_WITH_ERR, expectations);

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
    @Mode(TestMode.LITE)
    @Test
    public void testImplicit_validateIDToken() throws Exception {
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setImplicitClientDefaultSettings();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not receive redirect access token",
                null, Constants.REDIRECT_ACCESS_TOKEN);
        // now, we want to add expecations that actually validate responses received during the flow of the generic test.
        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive login page", null,
                Constants.LOGIN_PROMPT);
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Could not invoke protected application", null, Constants.IMPLICIT_APP_TITLE);

        // add generic id_token expectations
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);
        // add generic response expectations
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);

        genericOP(_testName, wc, updatedTestSettings, Constants.IMPLICIT_AUTHENTICATION_ACTIONS, expectations);

    }

    /***
     * Test Purpose: Specify scope without "openid". This is allowed for both
     * OAuth and OIDC. For OAuth - it does not return an id_token anyway. OIDC
     * will allow the omission, but will not return the id_token. The Status
     * code returned should be 200.
     * 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void testImplicit_missingOpenidInScope() throws Exception {
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setImplicitClientDefaultSettings();
        updatedTestSettings.setScope("scope1 scope2");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // now, we want to add expecations that actually validate responses received during the flow of the generic test.
        // Check if we got the redirect access token
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not receive redirect access token",
                null, Constants.REDIRECT_ACCESS_TOKEN);

        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive login page", null,
                Constants.LOGIN_PROMPT);

        // make sure that id_token is NOT in the response
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS,
                "Token validate response found the id_token in the response and should not have", Constants.ID_TOKEN_KEY, Constants.NOT_FOUND);

        genericOP(_testName, wc, updatedTestSettings, Constants.IMPLICIT_AUTHENTICATION_ACTIONS, expectations);

    }

    /***
     * Test Purpose: Make an implicit flow request and omit the nonce. With
     * OAuth, the request is processed with no errors - no id_token is returned.
     * With OIDC, we should receive an error about an invalide request. The
     * Status code returned should be 200 in either case.
     * 
     * 
     * @throws Exception
     */
    @Test
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20MissingParameterException" })
    public void testImplicit_missingNonce() throws Exception {
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setImplicitClientDefaultSettings();
        updatedTestSettings.setNonce(null);

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // now, we want to add expecations that actually validate responses received during the flow of the generic test.
        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive login page", null,
                Constants.LOGIN_PROMPT);

        if (eSettings.getProviderType() == Constants.OAUTH_OP) {
            // Check if we got the redirect access token
            expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not receive redirect access token",
                    null, Constants.REDIRECT_ACCESS_TOKEN);
            expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_ID_TOKEN, Constants.STRING_CONTAINS,
                    "Token validate response found the id_token in the response and should not have", Constants.ID_TOKEN_KEY, Constants.NOT_FOUND);
            expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Could not invoke protected application", null, Constants.IMPLICIT_APP_TITLE);

        } else {
            // make sure that we failed because of the missing Nonce
            expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS,
                    "Should have failed because the nonce was missing", null, "error=invalid_request");
            expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS,
                    "Should have failed because the nonce was missing", null, "CWOAU0033E%3A+A+required+runtime+parameter+was+missing%3A+nonce");
            expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                    "OP Server messages.log should contain a message indicating that the nonce parameter is required.", null, "CWOAU0033E:.*nonce");

        }
        
        testOPServer.addIgnoredServerExceptions("CWWKS1610E");
        genericOP(_testName, wc, updatedTestSettings, Constants.IMPLICIT_AUTHENTICATION_ACTIONS, expectations);

    }

    /***
     * Test Purpose: Make an implicit flow request to an OP server with a
     * non-default value set for accessTokenLifetime. The expiration time
     * calcuated and put into the exp time in the id_token should reflect this
     * adjusted lifetime. There is no id_token when using OAuth, so only
     * validate when using OIDC Status code returned should be 200..
     * 
     * @throws Exception
     * 
     */
    @Test
    public void testImplicit_alterExpTimeout() throws Exception {

        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKS1403I.*" + Constants.OAUTHCONFIGSAMPLE_START_APP);
        // restart  the server with a new config with a shorter timeout
        //        testOPServer.restartServer("server_altTokenExpTime.xml", _testName, null, Constants.JUNIT_REPORTING, startMsgs);
        testOPServer.reconfigServer("server_altTokenExpTime.xml", _testName, Constants.JUNIT_REPORTING, startMsgs);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setImplicitClientDefaultSettings();
        updatedTestSettings.setAccessTimeout("10");

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Check if we got authorization code
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not receive redirect access token",
                null, Constants.REDIRECT_ACCESS_TOKEN);
        // now, we want to add expecations that actually validate responses received during the flow of the generic test.
        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive login page", null,
                Constants.LOGIN_PROMPT);

        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Could not invoke protected application", null, Constants.IMPLICIT_APP_TITLE);

        genericOP(_testName, wc, updatedTestSettings, Constants.IMPLICIT_AUTHENTICATION_ACTIONS, expectations);

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
    public void testImplicit_NotSigned() throws Exception {

        List<String> startMsgs = new ArrayList<String>();
        //startMsgs.add("CWWKS1403I.*" + Constants.OAUTHCONFIGSAMPLE_START_APP);
        //testOPServer.restartServer("server_notSigned.xml", _testName, Constants.NO_EXTRA_APPS, Constants.JUNIT_REPORTING, startMsgs);
        testOPServer.reconfigServer("server_notSigned.xml", _testName, Constants.JUNIT_REPORTING, startMsgs);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setImplicitClientDefaultSettings();
        updatedTestSettings.setSignatureAlg(Constants.SIGALG_NONE);

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Could not invoke protected application", null, Constants.IMPLICIT_APP_TITLE);

        // add generic id_token expectations
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);
        // add generic response expectations
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);

        genericOP(_testName, wc, updatedTestSettings, Constants.IMPLICIT_AUTHENTICATION_ACTIONS, expectations);

    }

    /***
     * Test Purpose: Specify none for signatureAlgorithm. Check for alg set to
     * RS256 and there to be a 3rd part of the id_token, status code returned
     * should be 200.
     * 
     * @throws Exception
     */
    //  RS256 not currently working
    //@Test
    public void testWebClientRS256() throws Exception {

        ArrayList<String> extraMsgs = new ArrayList<String>();
        extraMsgs.add("CWWKS1403I.*" + Constants.OAUTHCONFIGSAMPLE_START_APP);
        //        testOPServer.restartServer("server_RS256.xml", _testName, Constants.NO_EXTRA_APPS, Constants.JUNIT_REPORTING, extraMsgs);
        testOPServer.reconfigServer("server_RS256.xml", _testName, Constants.JUNIT_REPORTING, extraMsgs);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setImplicitClientDefaultSettings();
        updatedTestSettings.setSignatureAlg(Constants.SIGALG_RS256);

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Could not invoke protected application", null, Constants.IMPLICIT_APP_TITLE);

        // add generic id_token expectations
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);
        // add generic response expectations
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);

        genericOP(_testName, wc, updatedTestSettings, Constants.IMPLICIT_AUTHENTICATION_ACTIONS, expectations);

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

        ArrayList<String> extraMsgs = new ArrayList<String>();
        //extraMsgs.add("CWWKS1403I.*" + Constants.OAUTHCONFIGSAMPLE_START_APP);
        //testOPServer.restartServer("server_HS256.xml", _testName, Constants.NO_EXTRA_APPS, Constants.JUNIT_REPORTING, extraMsgs);
        testOPServer.reconfigServer("server_HS256.xml", _testName, Constants.JUNIT_REPORTING, extraMsgs);

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setImplicitClientDefaultSettings();
        updatedTestSettings.setSignatureAlg(Constants.SIGALG_HS256);

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.PERFORM_LOGIN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Could not invoke protected application", null, Constants.IMPLICIT_APP_TITLE);

        // add generic id_token expectations
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);
        // add generic response expectations
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);

        genericOP(_testName, wc, updatedTestSettings, Constants.IMPLICIT_AUTHENTICATION_ACTIONS, expectations);

    }

    /**
     * TestDescription:
     * 
     * This test case performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "implicit" . In this scenario, the client uses the authorization
     * server as an intermediary to obtain the access token, without invoking
     * the token endpoint. The authorization server authenticates the resource
     * owner before issuing the access token. In this scenario, the autoauthz
     * parameter is set to true, so the resource owner does not receive the
     * consent form from the authorization server. The test verifies that the
     * Oauth code flow, using the authorization grant type of "implicit" works
     * correctly.
     * 
     */
    @Test
    public void testImplicitUseBasicAuth() throws Exception {
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setImplicitClientDefaultSettings();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes(null);

        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Could not invoke protected application", null, Constants.IMPLICIT_APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_SERVER, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null, "false");

        genericOP(_testName, wc, updatedTestSettings, Constants.IMPLICIT_AUTHENTICATION_ACTIONS_WITH_BASIC_AUTH, expectations);
    }

    // need to add testing with nonce - but, at this time, nonce is not supported in the basic flow yet

    /*
     * expiration timeout - actually timeout - Have not figured out a way to actually break into the
     * production code flow to make it timeout - can't just sleep before trying to use the
     * returned access_token
     */

}
