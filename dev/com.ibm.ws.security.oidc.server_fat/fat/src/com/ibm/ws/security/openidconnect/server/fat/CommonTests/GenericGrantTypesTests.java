/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.server.fat.CommonTests;

import java.util.List;

import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.AuthorizationHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.EndpointSettings.endpointSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;

/**
 * Test class for grantType. Tests grantType handling within the OP.
 * These tests are run for both web and implicit clients.
 * They run with both oauth and oidc. The extending class will set the test
 * setting appropriatly for the flow/provider being tested.
 * The extending class specifies the action/flow.
 * 
 * @author chrisc
 * 
 */
public class GenericGrantTypesTests extends CommonTest {

    private static final Class<?> thisClass = GenericGrantTypesTests.class;
    protected static AuthorizationHelpers authHelpers = new AuthorizationHelpers();
    public static CommonValidationTools validationTools = new CommonValidationTools();
    protected static String targetProvider = null;
    protected static String[] goodActions = null;
    protected static String flowType = Constants.WEB_CLIENT_FLOW;
    private static final String grantType = "GrantTypes";
    private static final String grantType2 = "GrantTypes2";

    /**
     * Updates the test setting with values needed for an implicit flow (called
     * by the extending classes)
     * 
     * @return - returns updated test settings
     * @throws Exception
     */
    protected static TestSettings setImplicitClientDefaultSettings(TestSettings updatedTestSettings) throws Exception {

        String thisMethod = "setImplicitClientDefaultSettings";
        msgUtils.printMethodName(thisMethod);

        updatedTestSettings.setNonce(Constants.DEFAULT_NONCE);
        updatedTestSettings.setFlowType(Constants.IMPLICIT_FLOW);
        if (updatedTestSettings.getScope().contains("openid")) {
            Log.info(thisClass, thisMethod, "Setting response_type to: id_token token");
            updatedTestSettings.setResponseType("id_token token");
        } else {
            Log.info(thisClass, thisMethod, "Setting response_type to: token");
            updatedTestSettings.setResponseType("token");
        }
        return updatedTestSettings;

    }

    /**
     * @param provider
     *            TODO
     * 
     */
    protected static TestSettings grantTypeSettings(String client, String provider) throws Exception {

        TestSettings updatedTestSettings = authHelpers.updateAuthTestSettings(eSettings, testSettings, client, null, targetProvider, targetProvider + provider, null);

        updatedTestSettings.setClientRedirect(testOPServer.getHttpString() + "/" + Constants.OAUTHCLIENT_APP + "/authorize_redirect.jsp");
        return updatedTestSettings;

    }

    protected static TestSettings resourceOwnerSettings(String client, String resowner_uname, String resowner_pswd, String provider) throws Exception {

        TestSettings updatedTestSettings = authHelpers.updateAuthTestSettings(eSettings, testSettings, client, null, targetProvider, targetProvider + provider, null);
        updatedTestSettings.setClientRedirect(testOPServer.getHttpString() + "/" + Constants.OAUTHCLIENT_APP + "/authorize_redirect.jsp");
        updatedTestSettings.setAdminUser(resowner_uname);
        updatedTestSettings.setAdminPswd(resowner_pswd);
        return updatedTestSettings;

    }

    /**
     * Invoke the token endpoint directly intead of using client.jsp
     * First, we need to build up the list of parm needed for the endpoint - those come from our settings
     * 
     * @param testCase
     *            - the testcase name for logging purposes
     * @param wc
     *            - the web converation
     * @param inResponse
     *            - the response from the previous frame
     * @param settings
     *            - the current test settings - used to set the parms
     * @param expectations
     *            - the expectations to be used for validation
     * @return - the response from the endpoint invocation
     * @throws Exception
     */
    public WebResponse invokeTokenEndpoint_authCode(String testCase, WebConversation wc, WebResponse inResponse, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeTokenEndpoint_authCode";
        msgUtils.printMethodName(thisMethod);

        List<endpointSettings> headers = null;
        List<endpointSettings> parms = eSettings.addEndpointSettings(null, "client_id", settings.getClientID());
        parms = eSettings.addEndpointSettings(parms, "client_secret", settings.getClientSecret());
        parms = eSettings.addEndpointSettings(parms, "token_endpoint", settings.getTokenEndpt());
        parms = eSettings.addEndpointSettings(parms, "redirect_uri", settings.getClientRedirect());
        parms = eSettings.addEndpointSettings(parms, "grant_type", Constants.AUTH_CODE_GRANT_TYPE);
        parms = eSettings.addEndpointSettings(parms, "code", validationTools.getValueFromResponseFull(inResponse, "Received authorization code: "));
        WebResponse response = genericInvokeEndpoint(_testName, wc, inResponse, settings.getTokenEndpt(), Constants.POSTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, headers, expectations, settings);

        return response;
    }

    /**
     * Invoke the token endpoint directly intead of using client.jsp
     * First, we need to build up the list of parm needed for the endpoint - those come from our settings
     * 
     * @param testCase
     *            - the testcase name for logging purposes
     * @param wc
     *            - the web converation
     * @param inResponse
     *            - the response from the previous frame
     * @param settings
     *            - the current test settings - used to set the parms
     * @param expectations
     *            - the expectations to be used for validation
     * @return - the response from the endpoint invocation
     * @throws Exception
     */
    public WebResponse invokeAuthorizationEndpoint_implicit(String testCase, WebConversation wc, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeAuthorizationEndpoint_implicit";
        msgUtils.printMethodName(thisMethod);

        List<endpointSettings> headers = null;
        Log.info(thisClass, thisMethod, "Building basic auth with user: " + settings.getAdminUser() + " and password: " + settings.getAdminPswd());
        headers = eSettings.addEndpointSettings(null, "Authorization", cttools.buildBasicAuthCred(settings.getAdminUser(), settings.getAdminPswd()));

        List<endpointSettings> parms = eSettings.addEndpointSettings(null, "client_id", settings.getClientID());
        parms = eSettings.addEndpointSettings(parms, "response_type", settings.getResponseType());
        parms = eSettings.addEndpointSettings(parms, "redirect_uri", settings.getClientRedirect());
        parms = eSettings.addEndpointSettings(parms, "scope", settings.getScope());
        parms = eSettings.addEndpointSettings(parms, "nonce", settings.getNonce());
        parms = eSettings.addEndpointSettings(parms, "testCase", testCase);

        WebResponse response = genericInvokeEndpoint(_testName, wc, null, settings.getAuthorizeEndpt(), Constants.POSTMETHOD, Constants.INVOKE_AUTH_ENDPOINT, parms, headers, expectations, settings);

        return response;
    }

    // Removed invokeTokenEndpoint_password() from here as it already exists in commonTest project

    /**
     * Invoke the token endpoint directly intead of using client.jsp
     * First, we need to build up the list of parm needed for the endpoint - those come from our settings
     * 
     * @param testCase
     *            - the testcase name for logging purposes
     * @param wc
     *            - the web converation
     * @param inResponse
     *            - the response from the previous frame
     * @param settings
     *            - the current test settings - used to set the parms
     * @param expectations
     *            - the expectations to be used for validation
     * @return - the response from the endpoint invocation
     * @throws Exception
     */
    public WebResponse invokeTokenEndpoint_refreshToken(String testCase, WebConversation wc, WebResponse inResponse, TestSettings settings, List<validationData> expectations) throws Exception {

        String thisMethod = "invokeTokenEndpoint_refreshToken";
        msgUtils.printMethodName(thisMethod);

        String refreshToken = validationTools.getTokenFromResponse(inResponse, Constants.REFRESH_TOKEN_KEY);

        List<endpointSettings> headers = null;
        Log.info(thisClass, thisMethod, "Building basic auth with user: " + settings.getClientID() + " and password: " + settings.getClientSecret());
        headers = eSettings.addEndpointSettings(null, "Authorization", cttools.buildBasicAuthCred(settings.getClientID(), settings.getClientSecret()));

        List<endpointSettings> parms = eSettings.addEndpointSettings(null, "client_id", settings.getClientID());
        parms = eSettings.addEndpointSettings(parms, "client_secret", settings.getClientSecret());
        parms = eSettings.addEndpointSettings(parms, "scope", settings.getScope());
        parms = eSettings.addEndpointSettings(parms, "grant_type", "refresh_token");
        parms = eSettings.addEndpointSettings(parms, "refresh_token", refreshToken);
        WebResponse response = genericInvokeEndpoint(_testName, wc, inResponse, settings.getTokenEndpt(), Constants.POSTMETHOD, Constants.INVOKE_TOKEN_ENDPOINT, parms, headers, expectations, settings);

        return response;
    }

    // Removed invokeTokenEndpoint_clientCredentials() from here as it exists in the commonTest project.

    /**
     * Build the list of expectations for a mismatch test. We should
     * get a 400 status code as well as an unsupported_grant_type error
     * 
     * @param reqGrantType
     *            - the grant_type that the test is requesting - it should show up in the error message
     * @param endpoint
     *            - the endpoint that we're invoking
     * @return
     * @throws Exception
     */
    public List<validationData> buildGrantTypesMismatchExpectations(String reqGrantType, String endpoint, int errorCode) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes(null, endpoint);
        expectations = vData.addResponseStatusExpectation(expectations, endpoint, errorCode);
        expectations = vData.addExpectation(expectations, endpoint, Constants.JSON_OBJECT, Constants.STRING_CONTAINS, "Did not receive invalid grantTypes exception (" + Constants.ERROR_RESPONSE_PARM + ")", Constants.ERROR_RESPONSE_PARM, Constants.ERROR_CODE_UNSUPPORTED_GRANT_TYPE);
        expectations = vData.addExpectation(expectations, endpoint, Constants.JSON_OBJECT, Constants.STRING_MATCHES, "Did not receive invalid grantTypes exception (" + Constants.ERROR_RESPONSE_DESCRIPTION + ")", Constants.ERROR_RESPONSE_DESCRIPTION, "CWOAU0025E:.*" + reqGrantType);
        return expectations;
    }

    /**
     * Build the list of expectations for a mismatch test. We should
     * get a 400 status code as well as an unsupported_grant_type error
     * 
     * @param reqGrantType
     *            - the grant_type that the test is requesting - it should show up in the error message
     * @param endpoint
     *            - the endpoint that we're invoking
     * @return
     * @throws Exception
     */
    public List<validationData> buildGrantTypesMismatchExpectations(String reqGrantType, String endpoint) throws Exception {
        return buildGrantTypesMismatchExpectations(reqGrantType, endpoint, Constants.BAD_REQUEST_STATUS);
    }

    /**
     * Build the list of expectations for a mismatch test. We should
     * get a 400 status code as well as an unsupported_grant_type error
     * 
     * @param reqGrantType
     *            - the grant_type that the test is requesting - it should show up in the error message
     * @param endpoint
     *            - the endpoint that we're invoking
     * @return
     * @throws Exception
     */
    public List<validationData> buildGrantTypesServerMismatchExpectations(String reqGrantType, String endpoint) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes(null, endpoint);
        expectations = vData.addResponseStatusExpectation(expectations, endpoint, Constants.INTERNAL_SERVER_ERROR_STATUS);
        expectations = vData.addExpectation(expectations, endpoint, Constants.JSON_OBJECT, Constants.STRING_CONTAINS, "Did not receive internal server exception (" + Constants.ERROR_RESPONSE_PARM + ")", Constants.ERROR_RESPONSE_PARM, Constants.ERROR_SERVER_CONFIG);
        expectations = vData.addExpectation(expectations, endpoint, Constants.JSON_OBJECT, Constants.STRING_CONTAINS, "Did not receive internal server exception (" + Constants.ERROR_RESPONSE_DESCRIPTION + ")", Constants.ERROR_RESPONSE_DESCRIPTION, Constants.SERVER_CONFIG_ERROR);
        return expectations;
    }

    /**
     * Build the list of expectations for a "resource owner bad creds" test.
     * We should get a 400 status code as well as an unsupported_grant_type error
     * 
     * @param reqGrantType
     *            - the grant_type that the test is requesting - it should show up in the error message
     * @param endpoint
     *            - the endpoint that we're invoking
     * @return
     * @throws Exception
     */
    public List<validationData> buildResourceOwnerBadCredsExpectations(String endpoint) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes(null, endpoint);
        expectations = vData.addResponseStatusExpectation(expectations, endpoint, Constants.BAD_REQUEST_STATUS);
        expectations = vData.addExpectation(expectations, endpoint, Constants.JSON_OBJECT, Constants.STRING_CONTAINS, "Did not receive internal server exception (" + Constants.ERROR_RESPONSE_PARM + ")", Constants.ERROR_RESPONSE_PARM, Constants.ERROR_SERVER_ERROR);
        expectations = vData.addExpectation(expectations, endpoint, Constants.JSON_OBJECT, Constants.STRING_CONTAINS, "Did not receive internal server exception (" + Constants.ERROR_RESPONSE_DESCRIPTION + ")", Constants.ERROR_RESPONSE_DESCRIPTION, Constants.ERROR_RESOURCE_OWNER_BAD_CREDS);
        return expectations;
    }

    /**
     * Build the list of expectations for a match test. We should
     * get a 200 status code and a valid access token, and possibly an id_token
     * 
     * @param endpoint
     *            - the endpoint that we're invoking
     * @param endpointType
     *            - end point type oauth or oidc
     * @param settings
     *            - the test settings for this particular test
     * @param testcase
     *            - the testcase name for logging
     * @return
     * @throws Exception
     */
    public List<validationData> buildGrantTypesMatchExpectations(String endpoint, String endpointType, TestSettings settings, String testcase) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes();
        // add generic id_token expectations
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, testcase, endpointType, endpoint, settings);
        // add generic response expectations
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, testcase, endpointType, endpoint, settings);
        return expectations;
    }

    /***********************************************************************************************************************************/

    /**
     * TestDescription:
     * 
     * This test uses a client that does not have any grantTypes defined.
     * All grantTypes are therefore supported - request contains authorization_code.
     * Request should succeed
     * 
     */
    @Test
    public void testGrantType_NoGrantTypesInConfig() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = grantTypeSettings("client00", grantType);

        List<validationData> expectations = vData.addSuccessStatusCodes();
        // add generic id_token expectations
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.INVOKE_TOKEN_ENDPOINT, updatedTestSettings);
        // add generic response expectations
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.INVOKE_TOKEN_ENDPOINT, updatedTestSettings);
        // verify that refresh_token is issued in the response
        expectations = vData.addTokenInResponseExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.REFRESH_TOKEN_GRANT_TYPE);

        WebResponse response = genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_NOJSP_ACTIONS, expectations);
        response = invokeTokenEndpoint_authCode(_testName, wc, response, updatedTestSettings, expectations);
    }

    /**
     * TestDescription:
     * 
     * This test uses a client that has authorization_code set in its grantTypes
     * Request contains authorization_code.
     * Request should succeed
     * 
     */
    @Test
    public void testGrantType_authCode_match() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = grantTypeSettings("client01", grantType);

        List<validationData> expectations = buildGrantTypesMatchExpectations(Constants.INVOKE_TOKEN_ENDPOINT, eSettings.getProviderType(), updatedTestSettings, _testName);

        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.INVOKE_TOKEN_ENDPOINT, updatedTestSettings);

        WebResponse response = genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_NOJSP_ACTIONS, expectations);
        response = invokeTokenEndpoint_authCode(_testName, wc, response, updatedTestSettings, expectations);
    }

    /**
     * TestDescription:
     * 
     * This test uses a client that has something other than authorization_code set in its grantTypes
     * Request contains authorization_code.
     * Request should fail as the grant_type in the request does not match
     * what is configured in the client
     * 
     */
    @ExpectedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidGrantTypeException")
    @Test
    public void testGrantType_authCode_misMatch() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = grantTypeSettings("client05", grantType);

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_AUTH_ENDPOINT);
        expectations = vData.addResponseStatusExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, Constants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, Constants.JSON_OBJECT, Constants.STRING_CONTAINS, "Did not receive invalid grantTypes exception (" + Constants.ERROR_RESPONSE_PARM + ")", Constants.ERROR_RESPONSE_PARM, Constants.ERROR_CODE_UNSUPPORTED_GRANT_TYPE);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_AUTH_ENDPOINT, Constants.JSON_OBJECT, Constants.STRING_MATCHES, "Did not receive invalid grantTypes exception (" + Constants.ERROR_RESPONSE_DESCRIPTION + ")", Constants.ERROR_RESPONSE_DESCRIPTION, "CWOAU0025E:.*" + Constants.AUTH_CODE_GRANT_TYPE);

        genericOP(_testName, wc, updatedTestSettings, Constants.AUTH_ENDPOINT_NOJSP_ACTIONS, expectations);
    }

    /**
     * TestDescription:
     * 
     * This test uses a client that has implicit set in its grantTypes
     * Request contains implicit.
     * Request should succeed
     * 
     */
    @Test
    public void testGrantType_implicit_match() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = grantTypeSettings("client02", grantType);
        updatedTestSettings = setImplicitClientDefaultSettings(updatedTestSettings);

        List<validationData> expectations = buildGrantTypesMatchExpectations(Constants.INVOKE_AUTH_ENDPOINT, eSettings.getProviderType(), updatedTestSettings, _testName);

        invokeAuthorizationEndpoint_implicit(_testName, wc, updatedTestSettings, expectations);
    }

    /**
     * TestDescription:
     * 
     * This test uses a client that has something other than implicit set in its grantTypes
     * Request contains implicit.
     * Request should fail as the grant_type in the request does not match
     * what is configured in the client
     * 
     */
    @ExpectedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidGrantTypeException")
    @Test
    public void testGrantType_implicit_misMatch() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = grantTypeSettings("client05", grantType);
        updatedTestSettings = setImplicitClientDefaultSettings(updatedTestSettings);

        List<validationData> expectations = buildGrantTypesMismatchExpectations(Constants.IMPLICIT_GRANT_TYPE, Constants.INVOKE_AUTH_ENDPOINT, Constants.UNAUTHORIZED_STATUS);

        invokeAuthorizationEndpoint_implicit(_testName, wc, updatedTestSettings, expectations);
    }

    /**
     * TestDescription:
     * 
     * This test uses a client that has implicit set in its grantTypes
     * This test uses a provider that has urn:ietf:params:oauth:grant-type:jwt-bearer
     * set in its grantType
     * Request contains implicit.
     * Request should fail as there are no common grantType set between the provider
     * and the client, therefore there is nothing to validate the client request against
     * 
     */
    @ExpectedFFDC("com.ibm.oauth.core.api.error.OAuthConfigurationException")
    @Test
    public void testGrantType_implicit_match_provider_misMatch() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = grantTypeSettings("client02", grantType2);
        updatedTestSettings = setImplicitClientDefaultSettings(updatedTestSettings);

        List<validationData> expectations = buildGrantTypesServerMismatchExpectations(Constants.IMPLICIT_GRANT_TYPE, Constants.INVOKE_AUTH_ENDPOINT);

        testOPServer.addIgnoredServerExceptions("CWWKS1606E");
        
        invokeAuthorizationEndpoint_implicit(_testName, wc, updatedTestSettings, expectations);
    }

    /**
     * TestDescription:
     * 
     * This test uses a client that has password set in its grantTypes
     * Request contains password.
     * Request should succeed
     * 
     */
    @Test
    public void testGrantType_password_match() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = grantTypeSettings("client05", grantType);

        List<validationData> expectations = buildGrantTypesMatchExpectations(Constants.INVOKE_TOKEN_ENDPOINT, Constants.OAUTH_OP, updatedTestSettings, _testName);

        invokeTokenEndpoint_password(_testName, wc, updatedTestSettings, expectations);
    }

    /**
     * TestDescription:
     * 
     * This test uses a client that has password set in its grantTypes and default OAuth mediator configured
     * in the oauthProvider. This test is added for testing the apar fix delivered by defect 160516.
     * This scenario needs two things: 1) grant_type of "password" and 2) default mediator configured in oauthProvider.
     * Request contains password.
     * Request should succeed
     * 
     */
    @Test
    public void testGrantType_password_with_mediator() throws Exception {

        String newProvider = "DefaultMediator";

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with default mediator
        TestSettings updatedTestSettings = resourceOwnerSettings("client05", "testuser", "testuserpwd", newProvider);

        List<validationData> expectations = buildGrantTypesMatchExpectations(Constants.INVOKE_TOKEN_ENDPOINT, Constants.OAUTH_OP, updatedTestSettings, _testName);

        invokeTokenEndpoint_password(_testName, wc, updatedTestSettings, expectations);

    }

    /**
     * TestDescription:
     * 
     * This test uses a client that has password set in its grantTypes and default OAuth mediator configured
     * in the oauthProvider. This test is added for testing the apar fix delivered by defect 160516.
     * This scenario needs two things: 1) grant_type of "password" and 2) default mediator configured in oauthProvider.
     * In this scenario, invalid resource owner's credentials are provided to invoke the token endpoint.
     * The call to token endpoint is expected to fail.
     * Request contains invalid userid and password.
     * Request should fail.
     * 
     */

    @Test
    @AllowedFFDC(value = { "com.ibm.websphere.security.PasswordCheckFailedException", "com.ibm.oauth.core.api.error.OidcServerException", "com.ibm.oauth.core.api.error.oauth20.OAuth20MediatorException" })
    public void testGrantType_password_with_bad_creds() throws Exception {

        String newProvider = "DefaultMediator";

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with default mediator
        TestSettings updatedTestSettings = resourceOwnerSettings("client05", "bad_user", "bad_passswd", newProvider);

        List<validationData> expectations = buildResourceOwnerBadCredsExpectations(Constants.INVOKE_TOKEN_ENDPOINT);

        testOPServer.addIgnoredServerExceptions("CWIML4537E", "CWOAU0069E");
        
        invokeTokenEndpoint_password(_testName, wc, updatedTestSettings, expectations);

    }

    /**
     * TestDescription:
     * 
     * This test uses a client that has password set in its grantTypes and the new oauthProvider attribute
     * "skipResourceOwnerValidation" is not defined. The test verifies that the default value of "false" for
     * skipResourceOwner validation attribute is working correctly. In this scenario, the token endpoint
     * is invoked with bad resource owner credentials and since resource owner validation is not supposed
     * to be skipped by default, the test expects to receive the appropriate exception.
     * This test is added for testing the apar fix delivered by defect 159780.
     * 
     */
    @AllowedFFDC(value = { "com.ibm.websphere.security.PasswordCheckFailedException", "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void testGrantType_password_with_default_skipValidation() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the oauthProvider which does not have skipResourceOwnervalidtion attribute defined.   		
        TestSettings updatedTestSettings = resourceOwnerSettings("client05", "bad_user", "bad_userpwd", grantType);

        List<validationData> expectations = buildResourceOwnerBadCredsExpectations(Constants.INVOKE_TOKEN_ENDPOINT);

        testOPServer.addIgnoredServerExceptions("CWIML4537E", "CWOAU0069E");
        
        invokeTokenEndpoint_password(_testName, wc, updatedTestSettings, expectations);

    }

    /**
     * TestDescription:
     * 
     * This test uses a client that has password set in its grantTypes and the new oauthProvider attribute
     * "skipResourceOwnerValidation" is set to "true". The test verifies that the resource owner validation
     * is not done in this case. In this scenario, the token endpoint is invoked with bad resource
     * owner credentials and since resource owner validation is supposed to be skipped, the test expects
     * to receive the access token without any exceptions.
     * This test is added for testing the apar fix delivered by defect 159780.
     * 
     */

    @Test
    public void testGrantType_password_with_skipValidation() throws Exception {

        String newProvider = "SkipUserValidation";

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // Update settings to use the provider with skipResourceOwnerValidation attribute set to true
        TestSettings updatedTestSettings = resourceOwnerSettings("client05", "bad_user", "bad_userpwd", newProvider);

        List<validationData> expectations = buildGrantTypesMatchExpectations(Constants.INVOKE_TOKEN_ENDPOINT, Constants.OAUTH_OP, updatedTestSettings, _testName);

        invokeTokenEndpoint_password(_testName, wc, updatedTestSettings, expectations);

    }

    /**
     * TestDescription:
     * 
     * This test uses a client that has something other than password set in its grantTypes
     * Request contains password.
     * Request should fail as the grant_type in the request does not match
     * what is configured in the client
     * 
     */
    @ExpectedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidGrantTypeException")
    @Test
    public void testGrantType_password_misMatch() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = grantTypeSettings("client01", grantType);

        List<validationData> expectations = buildGrantTypesMismatchExpectations(Constants.PASSWORD_GRANT_TYPE, Constants.INVOKE_TOKEN_ENDPOINT);

        invokeTokenEndpoint_password(_testName, wc, updatedTestSettings, expectations);
    }

    /**
     * TestDescription:
     * 
     * This test uses a client that has password set in its grantTypes
     * This test uses a provider that has urn:ietf:params:oauth:grant-type:jwt-bearer
     * set in its grantType
     * Request contains password.
     * Request should fail as there are no common grantType set between the provider
     * and the client, therefore there is nothing to validate the client request against
     * 
     */
    @ExpectedFFDC("com.ibm.oauth.core.api.error.OAuthConfigurationException")
    @Test
    public void testGrantType_password_match_provider_misMatch() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = grantTypeSettings("client05", grantType2);

        List<validationData> expectations = buildGrantTypesServerMismatchExpectations(Constants.PASSWORD_GRANT_TYPE, Constants.INVOKE_TOKEN_ENDPOINT);

        testOPServer.addIgnoredServerExceptions("CWWKS1606E");
        
        invokeTokenEndpoint_password(_testName, wc, updatedTestSettings, expectations);
    }

    /**
     * TestDescription:
     * 
     * This test uses a client that has refresh_token set in its grantTypes
     * Request contains refresh_token.
     * Request should succeed
     * 
     */
    @Test
    public void testGrantType_refreshToken_match() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = grantTypeSettings("client03", grantType);

        List<validationData> expectations = buildGrantTypesMatchExpectations(Constants.INVOKE_TOKEN_ENDPOINT, eSettings.getProviderType(), updatedTestSettings, _testName);

        WebResponse response = genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_NOJSP_ACTIONS, expectations);
        response = invokeTokenEndpoint_authCode(_testName, wc, response, updatedTestSettings, expectations);

        List<validationData> expectations2 = buildGrantTypesMatchExpectations(Constants.INVOKE_TOKEN_ENDPOINT, Constants.OAUTH_OP, updatedTestSettings, _testName);

        response = invokeTokenEndpoint_refreshToken(_testName, wc, response, updatedTestSettings, expectations2);

    }

    /**
     * TestDescription:
     * 
     * This test uses a client that has something other than refresh_token set in its grantTypes
     * Request contains refresh_token.
     * Request should fail as the grant_type in the request does not match
     * what is configured in the client
     * 
     */
    @Test
    public void testGrantType_refreshToken_misMatch() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = grantTypeSettings("client01", grantType);

        List<validationData> expectations = vData.addSuccessStatusCodes();
        // add generic id_token expectations
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.INVOKE_TOKEN_ENDPOINT, updatedTestSettings);
        // add generic response expectations
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.INVOKE_TOKEN_ENDPOINT, updatedTestSettings);

        // verify that refresh_token is not issued since client01 is not configured for it
        expectations = vData.addNoTokenInResponseExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.REFRESH_TOKEN_GRANT_TYPE);

        WebResponse response = genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_NOJSP_ACTIONS, expectations);
        response = invokeTokenEndpoint_authCode(_testName, wc, response, updatedTestSettings, expectations);

    }

    /**
     * TestDescription:
     * 
     * This test uses a client that has client_credential set in its grantTypes
     * Request contains client_credential.
     * Request should succeed
     * 
     */
    @Test
    public void testGrantType_clientCred_match() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = grantTypeSettings("client04", grantType);

        List<validationData> expectations = buildGrantTypesMatchExpectations(Constants.INVOKE_TOKEN_ENDPOINT, Constants.OAUTH_OP, updatedTestSettings, _testName);

        invokeTokenEndpoint_clientCredentials(_testName, wc, updatedTestSettings, expectations);
    }

    /**
     * TestDescription:
     * 
     * This test uses a client that has something other than client_credential set in its grantTypes
     * Request contains client_credential.
     * Request should fail as the grant_type in the request does not match
     * what is configured in the client
     * 
     */
    @ExpectedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidGrantTypeException")
    @Test
    public void testGrantType_clientCred_misMatch() throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = grantTypeSettings("client01", grantType);

        List<validationData> expectations = buildGrantTypesMismatchExpectations(Constants.CLIENT_CRED_GRANT_TYPE, Constants.INVOKE_TOKEN_ENDPOINT);

        invokeTokenEndpoint_clientCredentials(_testName, wc, updatedTestSettings, expectations);
    }

    /*****************************************************************************
     * jwt (urn:ietf:params:oauth:grant-type:jwt-bearer) grant type has a setup
     * that is so different than the rest of the tests, that I've added those tests
     * to the JWT FAT project in :
     * com.ibm.ws.security.openidconnect.server-
     * 1.0_fat.granttype.jwt/fat/src/com/ibm/ws/security/openidconnect/server/fat/granttype/jwt/CommonTests/genericWebClientGrantJwtTest.ja
     * v
     * a
     * testJwtToken_grantType_match
     * testJwtToken_grantType_misMatch
     *****************************************************************************/
}