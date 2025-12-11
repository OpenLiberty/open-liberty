/*******************************************************************************
 * Copyright (c) 2013, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.server.fat.BasicTests.CommonTests;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * Client credentials grant type tests:
 * 
 * This test verifies that the Oauth/OIDC code flow using the authorization grant type of
 * client_credentials works correctly.
 * 
 * These test cases perform a simple client_credentials grant type flow, using httpunit to
 * simulate browser requests for the positive test cases. For the negative tests,
 * httpUrlRequest is used to send the requests to the token endpoint to avoid
 * and httpunit problem with an exception where we cannot obtain the error response.
 * 
 * The test contains an end-to-end positive scenario, where a Web client invokes a front
 * end client application that obtains access token for the client using
 * WebSphere authorization server. The test uses the authorization grant
 * type "client_credentials". In this scenario, the client uses the
 * authorization server as an intermediary to obtain the access token from
 * the token endpoint by sending the resource owner's credentials. No authorization is
 * required when the client_credentials grant type is used, but in order to access the
 * protected resource, the client must be present in the user registry.
 * 
 * This test also contains positive scenarios that go directly to the token endpoint with the
 * client_credentials grant type in order to obtain the access token. In these tests,
 * the client is not required to be in the user registry and the access token is obtained.
 * 
 */
@RunWith(FATRunner.class)
public class genericWebClientCredentialTest extends ServerCommonTest {

    private static final Class<?> thisClass = genericWebClientCredentialTest.class;

    public static final String NULL_CLIENT_SECRET = (String) null;

    // subclasses randomly choose a tokentype and oidc subclass will set this
    public static String randomlyChosenTokenType = null;

    /**
     * Test Purpose: Verify good basic client_credentials end-to-end flow with valid userid and password to get access token and
     * access a protected resource (Snoop).
     * <OL>
     * <LI>Access token endpoint with client_credentials grant type to get an access token
     * <LI>The client, client01 is a registered client so the client is authenticated and the token is issued.
     * <LI>The client, client01 is found in the user registry so the protected resource (snoop) is accessed.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Access token obtained successfully and the protected resource Snoop is accessed.
     * </OL>
     **/
    @Test
    @Mode(TestMode.LITE)
    public void testClientCredentialBasicFlowToAccessProtectedResource_Succeeds() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // expect good (200) status codes for all steps
        List<validationData> expectations = expectGoodStatusCodeForAllSteps();

        // Check if we got the access token
        expectations = vData.addExpectation(expectations, Constants.SUBMIT_TO_AUTH_SERVER_FOR_TOKEN, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not receive access token", null, Constants.RECV_FROM_TOKEN_ENDPOINT);

        // Make sure we get to the app
        expectations = vData.addExpectation(expectations, Constants.INVOKE_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Could not invoke protected application", null, Constants.APP_TITLE);

        genericOP(_testName, wc, testSettings, Constants.BASIC_PROTECTED_RESOURCE_ACTIONS_WITH_CLIENT_CRED, expectations);
    }

    /**
     * Test Purpose: Verify client_credentials grant type with a confidential client and POST is successful
     * when authentication data is passed in the Basic Authentication Header
     * <OL>
     * <LI>Access token endpoint with client credentials grant type to get an access token
     * <LI>The client specified is a public client which should not be allowed to use this grant type.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Successful 200 return code and access_token returned by endpoint and scope:""
     * <LI>
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void testClientCredentials_ValidClient_BasicAuthHeader_Scope_Post_Success() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientID("client01");
        updatedTestSettings.setClientSecret("secret1234");
        updatedTestSettings.setScope("openid");

        List<validationData> expectations = expectGoodStatusCodeForAllSteps();
        expectations = setPositiveJsonExpectations(expectations, updatedTestSettings);

        //check functional id's in jwt tokens if jwt tokens are in use
        if (randomlyChosenTokenType != null) {
            if (randomlyChosenTokenType.equals(Constants.JWT_TOKEN) || randomlyChosenTokenType.equals(Constants.MP_JWT_TOKEN)) {
                Log.info(this.getClass(), "info:", "setting expectations for functionalId");
                expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_JWT_TOKEN, Constants.STRING_CONTAINS,
                        "jwt did not contain expected functional id claim", "functional_user_id", "funcid");
                expectations = vData.addExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.RESPONSE_JWT_TOKEN, Constants.STRING_CONTAINS,
                        "jwt did not contain expected functional id claim", "functional_user_groupIds", "funcgroup");
            }
        }

        List<endpointSettings> headers = createTokenEndpointBasicAuthenticationHeader(updatedTestSettings.getClientID(), updatedTestSettings.getClientSecret());
        List<endpointSettings> parms = eSettings.addEndpointSettings(null, Constants.GRANT_TYPE, Constants.CLIENT_CRED_GRANT_TYPE);
        parms = eSettings.addEndpointSettings(parms, "scope", updatedTestSettings.getScope());
        genericInvokeEndpoint(_testName, wc, null, updatedTestSettings.getTokenEndpt(), Constants.POSTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, headers,
                expectations);
    }

    /**
     * Test Purpose: Verify client_credentials grant type with a confidential client and POST is successful
     * when authentication data is passed in the request parameters. An unrecognized parm is ignored.
     * <OL>
     * <LI>Access token endpoint with client credentials grant type to get an access token
     * <LI>The client specified is a public client which should not be allowed to use this grant type.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Successful 200 return code and access_token returned by endpoint and scope:""
     * <LI>
     * </OL>
     **/
    @Test
    public void testClientCredentials_ValidClient_RequestParms_Scope_UnrecognizedParmIgnored_Post_Success() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientID("client01");
        updatedTestSettings.setClientSecret("secret1234");
        updatedTestSettings.setScope("openid");

        List<validationData> expectations = expectGoodStatusCodeForAllSteps();
        expectations = setPositiveJsonExpectations(expectations, updatedTestSettings);

        List<endpointSettings> parms = eSettings.addEndpointSettings(null, Constants.GRANT_TYPE, Constants.CLIENT_CRED_GRANT_TYPE);
        parms = eSettings.addEndpointSettings(parms, "client_id", updatedTestSettings.getClientID());
        parms = eSettings.addEndpointSettings(parms, "client_secret", updatedTestSettings.getClientSecret());
        parms = eSettings.addEndpointSettings(parms, "unrecognized_parm", "");
        parms = eSettings.addEndpointSettings(parms, "scope", updatedTestSettings.getScope());

        genericInvokeEndpoint(_testName, wc, null, updatedTestSettings.getTokenEndpt(), Constants.POSTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, null,
                expectations);
    }

    /**
     * Test Purpose: Verify client_credentials grant type with a confidential client and POST is successful
     * when authentication data is passed in the request parameters. An unrecognized parm is ignored.
     * <OL>
     * <LI>Access token endpoint with client credentials grant type to get an access token
     * <LI>The client specified is a public client which should not be allowed to use this grant type.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Successful 200 return code and access_token returned by endpoint.
     * <LI>
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void testClientCredentials_ValidClient_RequestParmsWithScope_Post_Success() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientID("client01");
        updatedTestSettings.setClientSecret("secret1234");
        updatedTestSettings.setScope("openid");

        List<validationData> expectations = expectGoodStatusCodeForAllSteps();
        expectations = setPositiveJsonExpectations(expectations, updatedTestSettings);

        List<endpointSettings> parms = eSettings.addEndpointSettings(null, Constants.GRANT_TYPE, Constants.CLIENT_CRED_GRANT_TYPE);
        parms = eSettings.addEndpointSettings(parms, "client_id", updatedTestSettings.getClientID());
        parms = eSettings.addEndpointSettings(parms, "client_secret", updatedTestSettings.getClientSecret());
        parms = eSettings.addEndpointSettings(parms, "scope", updatedTestSettings.getScope());

        genericInvokeEndpoint(_testName, wc, null, updatedTestSettings.getTokenEndpt(), Constants.POSTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, null,
                expectations);
    }

    // -------------------------------------------------------------------------------------------------------------------------------------------------------
    /**
     * Test Purpose: Verify negative client_credentials flow missing the clientID and secret
     * <OL>
     * <LI>Access token endpoint with client_credentials grant type but missing clientID and secret
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Error 400 Bad Request is returned from token endpoint
     * <LI>Response contains - error: invalid_request
     * <LI>error_description: CWWKS1406E: The token request had an invalid client credential. The request URI was...
     * <LI>OP message.log contains a message CWOAU0033E: A required runtime parameter was missing: client_id
     * <LI>
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void testClientCredential_MissingClientIDPassword_Error400_InvalidRequest() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientID("");
        updatedTestSettings.setClientSecret("");

        List<validationData> expectations = setNegativeExpectationsForBadRequest();
        expectations = setNegativeResponseExpectationForInvalidRequest(expectations, Constants.MSG_INVALID_CLIENT_CRED, Constants.MSG_REQUIRED_PARAM_MISSING + ":.*client_id");

        List<endpointSettings> parms = eSettings.addEndpointSettings(null, Constants.GRANT_TYPE, Constants.CLIENT_CRED_GRANT_TYPE);
        
        testOPServer.addIgnoredServerExceptions("CWOAU0033E");
        
        genericInvokeEndpointWithHttpUrlConn(_testName, null, testSettings.getTokenEndpt(), Constants.POSTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, null, expectations);

    }

    /**
     * Test Purpose: Verify negative basic client credential flow with valid
     * clientID but a bad client secret.
     * <OL>
     * <LI>Access token endpoint with client_credentials grant type to get an access token
     * <LI>The client01 is a regsitered client, but the password sent to token endpoint is invalid.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Error 401 Unauthorized is returned from token endpoint
     * <LI>Response contains - error: invalid_client
     * <LI>error_description: CWWKS1406E: The token request had an invalid client credential. The request URI was ...
     * <LI>OP message.log contains a message CWOAU0038E: The client could not be verified. Either the client ID: client01 or
     * client secret is incorrect.
     * <LI>
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void testClientCredentials_BadClientSecret_BasicAuthHeader_Error401_InvalidClient() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientSecret("badSecret");

        List<validationData> expectations = expectGoodStatusCodesExceptForTokenEndpoint();
        expectations = setNegativeExpectationsForUnauthorized();
        expectations = setNegativeResponseExpectationForInvalidClient(expectations, Constants.MSG_INVALID_CLIENT_CRED,
                Constants.MSG_INVALID_CLIENTID_OR_SECRET + ":.*" + updatedTestSettings.getClientID(), true);

        List<endpointSettings> headers = createTokenEndpointBasicAuthenticationHeader(updatedTestSettings.getClientID(), updatedTestSettings.getClientSecret());
        List<endpointSettings> parms = eSettings.addEndpointSettings(null, Constants.GRANT_TYPE, Constants.CLIENT_CRED_GRANT_TYPE);

        testOPServer.addIgnoredServerExceptions("CWOAU0038E");

        genericInvokeEndpointWithHttpUrlConn(_testName, null, testSettings.getTokenEndpt(), Constants.POSTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, headers, expectations);
    }

    /**
     * Test Purpose: Verify negative client credential flow and an unregistered
     * client
     * <OL>
     * <LI>Access token endpoint with client credentials grant type to get an access token
     * <LI>The client specified is not a registered client.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Error 401 Unauthorized is returned from token endpoint
     * <LI>Response contains - error: invalid_client
     * <LI>error_description: CWWKS1406E: The token request had an invalid client credential. The request URI was ...
     * <LI>OP message.log contains a message CWOAU0038E: The client could not be verified. Either the client ID:
     * unregisteredClient or client secret is incorrect.
     * <LI>
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void testClientCredentials_UnregisteredClient_BasicAuthHeader_Error401_InvalidClient() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientID("unregisteredClient");
        updatedTestSettings.setClientSecret("secret1234");

        List<validationData> expectations = expectGoodStatusCodesExceptForTokenEndpoint();
        expectations = setNegativeExpectationsForUnauthorized();
        expectations = setNegativeResponseExpectationForInvalidClient(expectations, Constants.MSG_INVALID_CLIENT_CRED,
                Constants.MSG_INVALID_CLIENTID_OR_SECRET + ":.*unregisteredClient", true);

        List<endpointSettings> headers = createTokenEndpointBasicAuthenticationHeader(updatedTestSettings.getClientID(), updatedTestSettings.getClientSecret());
        List<endpointSettings> parms = eSettings.addEndpointSettings(null, Constants.GRANT_TYPE, Constants.CLIENT_CRED_GRANT_TYPE);

        testOPServer.addIgnoredServerExceptions("CWOAU0038E");

        genericInvokeEndpointWithHttpUrlConn(_testName, null, testSettings.getTokenEndpt(), Constants.POSTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, headers, expectations);

    }

    /**
     * Test Purpose: Verify negative client credential flow missing the client secret in request parms
     * <OL>
     * <LI>Access token endpoint with client credentials grant type to get an access token.
     * 
     * <LI>The client is specified in request parameters without a client secret parameter.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Error 400 Bad Request is returned from token endpoint
     * <LI>Response contains - error: invalid_client
     * <LI>error_description: CWWKS1406E: The token request had an invalid client credential. The request URI was ...
     * <LI>OP message.log contains a message CWOAU0038E: The client could not be verified. Either the client ID: pclient01 or
     * client secret is incorrect.
     * </OL>
     **/
    @Test
    public void testClientCredentials_MissingClientSecret_RequestParms_Error400_InvalidClient() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientID("client01");
        updatedTestSettings.setClientSecret("");

        List<validationData> expectations = expectGoodStatusCodesExceptForTokenEndpoint();
        expectations = setNegativeExpectationsForBadRequest();
        expectations = setNegativeResponseExpectationForInvalidClient(expectations, Constants.MSG_INVALID_CLIENT_CRED,
                Constants.MSG_INVALID_CLIENTID_OR_SECRET + ":.*client01", false);

        List<endpointSettings> parms = eSettings.addEndpointSettings(null, Constants.GRANT_TYPE, Constants.CLIENT_CRED_GRANT_TYPE);
        parms = eSettings.addEndpointSettings(parms, "client_id", updatedTestSettings.getClientID());

        testOPServer.addIgnoredServerExceptions("CWOAU0038E");

        genericInvokeEndpointWithHttpUrlConn(_testName, null, testSettings.getTokenEndpt(), Constants.POSTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, null, expectations);

    }

    /**
     * Try to access the /token endpoint with an HTTP PUT request.
     * 
     * Expected results:
     * - Should receive 405 response code saying the HTTP method is not supported
     */
    //@Test
    public void testClientCredentials_ValidClient_BasicAuthHeader_Put_Error405() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientID("client01");
        updatedTestSettings.setClientSecret("secret1234");

        List<validationData> expectations = expectGoodStatusCodesExceptForTokenEndpoint();
        expectations = vData.addResponseStatusExpectation(null, Constants.INVOKE_TOKEN_ENDPOINT, Constants.NOT_ALLOWED_STATUS);

        List<endpointSettings> headers = createTokenEndpointBasicAuthenticationHeader(updatedTestSettings.getClientID(), updatedTestSettings.getClientSecret());
        List<endpointSettings> parms = eSettings.addEndpointSettings(null, Constants.GRANT_TYPE, Constants.CLIENT_CRED_GRANT_TYPE);
        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getTokenEndpt(), Constants.PUTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, headers, expectations);

    }

    /**
     * Test Purpose: Verify client credential grant type with a public client is not allowed when using Basic Authentication
     * Header.
     * 
     * <OL>
     * <LI>Access token endpoint with client credentials grant type to get an access token when public client clientID is passed
     * in BasicAuthentication header without a client secret.
     * <LI>The client specified is a public client (clientID but no secret) which should not be allowed to use this grant type.
     * 
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Error 401 Unauthorized is returned from token endpoint
     * <LI>Response contains - error: invalid_client
     * <LI>error_description: CWWKS1406E: The token request had an invalid client credential. The request URI was ...
     * <LI>OP message.log contains CWOAU0038E: The client could not be verified. Either the client ID: pclient01 or client secret
     * is incorrect.
     * <LI>
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @Test
    public void testClientCredentials_PublicClient_BasicAuthHeader_Error401_InvalidClient() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientID("pclient01");

        List<validationData> expectations = expectGoodStatusCodesExceptForTokenEndpoint();
        expectations = setNegativeExpectationsForUnauthorized();
        expectations = setNegativeResponseExpectationForInvalidClient(expectations, Constants.MSG_INVALID_CLIENT_CRED,
                Constants.MSG_INVALID_CLIENTID_OR_SECRET + ":.*pclient01", true);

        List<endpointSettings> headers = createTokenEndpointBasicAuthenticationHeader(updatedTestSettings.getClientID(), NULL_CLIENT_SECRET);
        List<endpointSettings> parms = eSettings.addEndpointSettings(null, Constants.GRANT_TYPE, Constants.CLIENT_CRED_GRANT_TYPE);

        testOPServer.addIgnoredServerExceptions("CWOAU0038E");

        genericInvokeEndpointWithHttpUrlConn(_testName, null, testSettings.getTokenEndpt(), Constants.POSTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, headers, expectations);

    }

    /**
     * Test Purpose: Verify client credential grant type with a public client is not allowed when using request parms.
     * 
     * <OL>
     * <LI>Access token endpoint with client credentials grant type to get an access token when public client, clientID is passed
     * in request parms without a client secret.
     * <LI>The client specified is a public client (clientID but no secret) which should not be allowed to use this grant type.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Error 400 Bad Request is returned from token endpoint
     * <LI>Response contains - error: invalid_client
     * <LI>error_description: CWWKS1406E: The token request had an invalid client credential. The request URI was ...
     * <LI>OP message.log contains a message CWOAU0038E: The client could not be verified. Either the client ID:
     * unregisteredClient or client secret is incorrect.
     * </OL>
     **/
    @Test
    public void testClientCredentials_PublicClient_RequestParms_Error400_InvalidClient() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientID("pclient01");

        List<validationData> expectations = expectGoodStatusCodesExceptForTokenEndpoint();
        expectations = setNegativeExpectationsForBadRequest();
        expectations = setNegativeResponseExpectationForInvalidClient(expectations, Constants.MSG_INVALID_CLIENT_CRED,
                Constants.MSG_INVALID_CLIENTID_OR_SECRET + ":.*pclient01", false);

        List<endpointSettings> parms = eSettings.addEndpointSettings(null, Constants.GRANT_TYPE, Constants.CLIENT_CRED_GRANT_TYPE);
        parms = eSettings.addEndpointSettings(parms, "client_id", updatedTestSettings.getClientID());

        testOPServer.addIgnoredServerExceptions("CWOAU0038E");

        genericInvokeEndpointWithHttpUrlConn(_testName, null, testSettings.getTokenEndpt(), Constants.POSTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, null, expectations);

    }

    /**
     * Test Purpose: Verify client credential grant type with a confidential client and GET is results in error
     * 401 invalid_client because mutliple client credentials are specified for different clients in basic auth header
     * and in request parameters.
     * <OL>
     * <LI>Access token endpoint with client credentials grant type with client01 in header and client02 in parms.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Error 401 Unauthorized is returned from token endpoint
     * <LI>Response contains - error: invalid_client
     * <LI>error_description: CWOAU0031E: The client_id passed in the request to the token endpoint: client02 did not match the
     * authenticated client provided in the API call: client01"
     * <LI>OP message.log contains CWOAU0031E:
     * </OL>
     **/
    @Mode(TestMode.LITE)
    @ExpectedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20MismatchedClientAuthenticationException")
    @Test
    public void testClientCredentials_MultipleCreds_BasicAuthHeader_Error400_InvalidRequest() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientID("client01");
        updatedTestSettings.setClientSecret("secret1234");
        updatedTestSettings.setScope("openid");

        List<validationData> expectations = expectGoodStatusCodesExceptForTokenEndpoint();
        expectations = setNegativeExpectationsForBadRequest();
        expectations = setNegativeResponseExpectationForInvalidRequest(expectations, Constants.MSG_CLIENTID_NOT_MATCH_AUTHENTICATED, Constants.MSG_CLIENTID_NOT_MATCH_AUTHENTICATED_LOG);

        List<endpointSettings> headers = createTokenEndpointBasicAuthenticationHeader(updatedTestSettings.getClientID(), updatedTestSettings.getClientSecret());
        List<endpointSettings> parms = eSettings.addEndpointSettings(null, Constants.GRANT_TYPE, Constants.CLIENT_CRED_GRANT_TYPE);
        parms = eSettings.addEndpointSettings(parms, "client_id", "client02");
        parms = eSettings.addEndpointSettings(parms, "client_secret", updatedTestSettings.getClientSecret());
        parms = eSettings.addEndpointSettings(parms, "scope", updatedTestSettings.getScope());

        testOPServer.addIgnoredServerExceptions("CWOAU0063E");

        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getTokenEndpt(), Constants.POSTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, headers, expectations);

    }

    /**
     * Test Purpose: Verify client credential grant type with a confidential client and GET is results in error
     * 400 invalid_request because the clientID is not specified in the request parms.
     * <OL>
     * <LI>Access token endpoint with client credentials grant type and missing clientID in request parms.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Error 400 Bad Request is returned from token endpoint
     * <LI>Response contains - error: invalid_request
     * <LI>error_description: CWWKS1406E: The token request had an invalid client credential. The request URI was...
     * <LI>OP message.log contains CWOAU0033E: A required runtime parameter was missing: client_id
     * </OL>
     **/
    @Test
    public void testClientCredentials_MissingClientID_RequestParms_Error400_InvalidRequest() throws Exception {

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientID("client01");
        updatedTestSettings.setClientSecret("secret1234");

        List<validationData> expectations = expectGoodStatusCodesExceptForTokenEndpoint();
        expectations = setNegativeExpectationsForBadRequest();
        expectations = setNegativeResponseExpectationForInvalidRequest(expectations, Constants.MSG_INVALID_CLIENT_CRED, Constants.MSG_REQUIRED_PARAM_MISSING + ":.*client_id");

        List<endpointSettings> parms = eSettings.addEndpointSettings(null, Constants.GRANT_TYPE, Constants.CLIENT_CRED_GRANT_TYPE);
        parms = eSettings.addEndpointSettings(parms, "client_secret", updatedTestSettings.getClientSecret());

        testOPServer.addIgnoredServerExceptions("CWOAU0033E");

        genericInvokeEndpointWithHttpUrlConn(_testName, null, updatedTestSettings.getTokenEndpt(), Constants.POSTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, null, expectations);

    }

}
