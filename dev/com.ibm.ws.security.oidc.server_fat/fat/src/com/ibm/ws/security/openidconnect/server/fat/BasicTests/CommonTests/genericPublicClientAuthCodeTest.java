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

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

@RunWith(FATRunner.class)
public class genericPublicClientAuthCodeTest extends CommonTest {

    //private static final Class<?> thisClass = genericPublicClientAuthCodeTest.class;

    protected TestSettings setPublicClientDefaultSettings() throws Exception {
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientName("pclient01");
        updatedTestSettings.setClientID("pclient01");
        updatedTestSettings.setClientSecret("");
        updatedTestSettings.setAuthorizeEndpt(eSettings.assembleEndpoint(testOPServer.getHttpsString(), Constants.ENDPOINT_TYPE, testSettings.getConfigPublic(), Constants.AUTHORIZE_ENDPOINT));
        updatedTestSettings.setTokenEndpt(eSettings.assembleEndpoint(testOPServer.getHttpsString(), Constants.ENDPOINT_TYPE, testSettings.getConfigPublic(), Constants.TOKEN_ENDPOINT));
        updatedTestSettings.setProtectedResource(eSettings.assembleProtectedResource(testOPServer.getHttpsString(), testSettings.getConfigTAI(), Constants.SNIFFING));

        return updatedTestSettings;

    }

    /**
     * TestDescription:
     * 
     * This test case performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code" for a public client. For a public client, the
     * client secret is not sent to the authorization server. In this scenario,
     * the client uses the authorization server as an intermediary to obtain the
     * authorization code and then uses this authorization code to request
     * access token from the token endpoint. The authorization server
     * authenticates the resource owner before issuing the authorization code
     * which is redirected to the client. In this scenario, the autoauthz
     * parameter is set to true, so the resource owner does not receive the
     * consent form from the authorizarion server. The test verifies that the
     * Oauth code flow, using the authorization grant type of "authorization
     * code" works correctly for a public client.
     * 
     */
    //    @Mode(TestMode.LITE)
    @Test
    public void testPublicClientAuthCodeBasicFlow() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setPublicClientDefaultSettings();

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

    /***
     * Test Purpose: Override the default response_type with an invalid value of
     * "bob" Ensure that the value is detected and that an error is returned.
     * The response contains the error, but the status code returned is a 200
     * 
     * @throws Exception
     */
    @Test
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidResponseTypeException" })
    public void testPublicClientResponseTypeBad() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setPublicClientDefaultSettings();
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
        //genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);
        genericOP(_testName, wc, updatedTestSettings, Constants.SUBMIT_ACTIONS, expectations);//@AV999

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
    //    @Mode(TestMode.LITE)
    @Test
    public void testPublicClient_validateIDToken() throws Exception {

        TestSettings updatedTestSettings = setPublicClientDefaultSettings();

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
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.PERFORM_LOGIN, updatedTestSettings);

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS, expectations);

    }

    /***
     * Test Purpose: Specify scope without "openid". This is allowed for both
     * OAuth and OIDC. For OAuth - it does not return an id_token anyway. OIDC
     * will allow the omission, but will not return the id_token. The Status
     * code returned should be 200.
     * 
     * @throws Exception
     */
    @Test
    public void testPublicClient_missingOpenidInScope() throws Exception {
        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setPublicClientDefaultSettings();
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

    /**
     * TestDescription:
     * 
     * This test case performs a simple end-end OAuth flow, using httpunit to
     * simulate browser requests. In this scenario, a Web client invokes a front
     * end client application that obtains access token for the client using
     * WebSphere authorization server. The test uses the authorization grant
     * type "authorization code" for a public client. For a public client, the
     * client secret is not sent to the authorization server. In this scenario,
     * the client uses the authorization server as an intermediary to obtain the
     * authorization code and then uses this authorization code to request
     * access token from the token endpoint. The authorization server
     * authenticates the resource owner before issuing the authorization code
     * which is redirected to the client. In this scenario, the autoauthz
     * parameter is set to true, so the resource owner does not receive the
     * consent form from the authorizarion server. The test verifies that the
     * Oauth code flow, using the authorization grant type of "authorization
     * code" works correctly for a public client.
     * 
     */
    @Mode(TestMode.LITE)
    @Test
    public void testPublicClientAuthCodeBasicFlowUseBasicAuth() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = setPublicClientDefaultSettings();

        // expect good (200) status codes for all steps
        List<validationData> expectations = vData.addSuccessStatusCodes();

        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Could not invoke protected application", null, Constants.APP_TITLE);
        // Response should not have an ltpa token
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_TOKEN, null, "Response has an ltpa token, but should not", null, "false");

        genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS_WITH_BASIC_AUTH, expectations);

    }

    /**
     * Test Purpose: Verify negative client credential flow using public client with grant type that requires confidential
     * clients.
     * - Access token endpoint with client credentials grant type to get an access token.
     * - The client is specified in request parameters without a client secret parameter.
     * - Public clients are allowed.
     * 
     * Expected Results:
     * - Error 400 Bad Request is returned from token endpoint
     * - Response contains - error: invalid_client
     * - error_description: CWOAU0071E: A public client attempted to access the token endpoint using the client_credentials grant
     * type. This grant type can only be used by confidential clients. The client_id is: pclient01
     * - OP message.log contains a message CWOAU0071E: A public client attempted to access the token endpoint using the
     * client_credentials grant type. This grant type can only be used by confidential clients. The client_id is: pclient01
     */
    @Test
    public void testClientCredentials_AllowPublicClients_PublicClient_Error400_InvalidClient() throws Exception {

        TestSettings updatedTestSettings = setPublicClientDefaultSettings();

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_TOKEN_ENDPOINT);

        // Expect 400 for token endpoint
        expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_TOKEN_ENDPOINT, Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_MESSAGE, null, "Did not receive the expected "
                + Constants.BAD_REQUEST_STATUS + " status code", null, "Bad Request");

        // Expect invalid_client
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Should have failed for error code " + Constants.ERROR_CODE_INVALID_CLIENT, null, Constants.ERROR_CODE_INVALID_CLIENT);

        // Expect CWOAU0071E message in the error description and the server log
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS,
                "Token endpoint response did not contain the correct error description.", Constants.ERROR_RESPONSE_DESCRIPTION, Constants.MSG_PUBLIC_CLIENT_ENDPT_REQUIRES_CONF_CLIENT);
        expectations = vData.addNoTokensInResponseExpectations(expectations, Constants.INVOKE_TOKEN_ENDPOINT);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, testOPServer, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "Server messages.log should contain a message indicating that the request was not valid.", null, Constants.MSG_PUBLIC_CLIENT_ENDPT_REQUIRES_CONF_CLIENT + ":.*pclient01");

        List<endpointSettings> parms = eSettings.addEndpointSettings(null, Constants.GRANT_TYPE, Constants.CLIENT_CRED_GRANT_TYPE);
        parms = eSettings.addEndpointSettings(parms, "client_id", updatedTestSettings.getClientID());

        testOPServer.addIgnoredServerExceptions("CWOAU0071E");
        
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getTokenEndpt(), Constants.POSTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, null, expectations);

    }

}
