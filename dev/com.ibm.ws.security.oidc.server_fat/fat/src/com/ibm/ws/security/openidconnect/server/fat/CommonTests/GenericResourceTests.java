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
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

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
public class GenericResourceTests extends CommonTest {

    private static final Class<?> thisClass = GenericResourceTests.class;
    protected static AuthorizationHelpers authHelpers = new AuthorizationHelpers();
    public static CommonValidationTools validationTools = new CommonValidationTools();
    protected static String targetProvider = null;
    protected static String[] goodActions = null;
    protected static String flowType = Constants.WEB_CLIENT_FLOW;
    private static final String grantType = "ResourceIds";

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
     *
     */
    protected static TestSettings grantTypeSettings(String client, String provider) throws Exception {

        TestSettings updatedTestSettings = authHelpers.updateAuthTestSettings(eSettings, testSettings, client, null, targetProvider, targetProvider + provider, null);

        updatedTestSettings.setClientRedirect(testOPServer.getHttpString() + "/" + Constants.OAUTHCLIENT_APP + "/authorize_redirect.jsp");
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
     * @param expectations-
     *            the expectations to be used for validation
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
        String resource = settings.getResourceIds();
        if (resource != null && resource != "") {
            parms = eSettings.addEndpointSettings(parms, "resource", resource);
        }

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
     * @param expectations-
     *            the expectations to be used for validation
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
        String resource = settings.getResourceIds();
        if (resource != null && resource != "") {
            parms = eSettings.addEndpointSettings(parms, "resource", resource);
        }

        WebResponse response = genericInvokeEndpoint(_testName, wc, null, settings.getAuthorizeEndpt(), Constants.POSTMETHOD, Constants.INVOKE_AUTH_ENDPOINT, parms, headers, expectations, settings);

        return response;
    }

    /**
     * Utility to split value into a list
     *
     * @param value
     *            - the string to split
     * @param delim
     *            - what to use a the split delimiter
     * @return - list of individual strings
     * @throws Exception
     */
    protected String[] getSettingAsList(String value, String delim) throws Exception {

        if (value == null) {
            return new String[] {};
        }
        return value.split(delim);
    }

    /**
     * Add expectations to validate the JWT token
     *
     * @param expectations
     *            - existing expectations (if null we'll create a new set of expectations)
     * @param step
     *            - set in the process that we should validate the JWT
     * @param settings
     *            - settings to use to set expectations
     * @param expectedResourceIds
     *            - the resource id values that we should find in the token
     * @return - return updated/created expectations
     * @throws Exception
     */
    protected List<validationData> addJWTTokenExpectations(List<validationData> expectations, String step, TestSettings settings, String expectedResourceIds) throws Exception {

        Boolean openidFound = false;

        if (expectations == null) {
            expectations = vData.addSuccessStatusCodes();
        }

        // don't need to set any jwt values to be checked if we're NOT using jwt tokens...
        if (settings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            return expectations;
        }

        // for negative tests, we can't use the generic JWT validation - our test settings have one value and the server config has another - the test settings are used
        // to issue the request as well as validate the result and that won't allow us to invoke with one value and validate with another from the same call...
        if (settings.getResourceIds() != null) {
            // Generic validation of token (do claims exist that should, and not exist if they shouldn't)
            expectations = vData.addExpectation(expectations, step, Constants.RESPONSE_JWT_TOKEN, Constants.STRING_CONTAINS, "Token did NOT validate properly", null, null);
        }

        // now check the values of some of the claims
        expectations = vData.addExpectation(expectations, step, Constants.RESPONSE_JWT_TOKEN, Constants.STRING_CONTAINS, "Token did NOT contain a valid " + Constants.TOKEN_TYPE_KEY + " claim", Constants.TOKEN_TYPE_KEY, Constants.BEARER);

        for (String scope : getSettingAsList(settings.getScope(), " ")) {
            expectations = vData.addExpectation(expectations, step, Constants.RESPONSE_JWT_TOKEN, Constants.STRING_CONTAINS, "Token did NOT contain a valid " + Constants.JWT_SCOPE + " claim", Constants.JWT_SCOPE, scope);
            if (scope.equals("openid")) {
                openidFound = true;
            }
        }
        if (!openidFound) {
            expectations = vData.addExpectation(expectations, step, Constants.RESPONSE_JWT_TOKEN, Constants.STRING_DOES_NOT_CONTAIN, "Token should NOT contain openid claim, but did.", Constants.JWT_SCOPE, "openid");
        }

        for (String group : getSettingAsList(settings.getGroupIds(), " ")) {
            expectations = vData.addExpectation(expectations, step, Constants.RESPONSE_JWT_TOKEN, Constants.STRING_CONTAINS, "Token did NOT contain a valid " + Constants.PAYLOAD_GROUP + " claim", Constants.PAYLOAD_GROUP, group);
        }

        for (String issuer : getSettingAsList(settings.getIssuer(), " ")) {
            expectations = vData.addExpectation(expectations, step, Constants.RESPONSE_JWT_TOKEN, Constants.STRING_CONTAINS, "Token did NOT contain a valid " + Constants.PAYLOAD_ISSUER + " claim", Constants.PAYLOAD_ISSUER, issuer);
        }

        expectations = vData.addExpectation(expectations, step, Constants.RESPONSE_JWT_TOKEN, Constants.STRING_CONTAINS, "Token did NOT contain a valid " + Constants.PAYLOAD_SUBJECT + " claim", Constants.PAYLOAD_SUBJECT, settings.getAdminUser());
        expectations = vData.addExpectation(expectations, step, Constants.RESPONSE_JWT_TOKEN, Constants.STRING_CONTAINS, "Token did NOT contain a valid " + Constants.IDTOK_REALM_KEY + " claim", Constants.IDTOK_REALM_KEY, settings.getRealm());
        expectations = vData.addExpectation(expectations, step, Constants.RESPONSE_JWT_TOKEN, Constants.STRING_CONTAINS, "Token did NOT contain a valid " + Constants.PAYLOAD_AUTHORIZED_PARTY + " claim", Constants.PAYLOAD_AUTHORIZED_PARTY, settings.getClientID());
        expectations = addResourceChecks(expectations, step, settings, expectedResourceIds);

        return expectations;
    }

    /***
     * Add checks for extra resourceId values within aud. Assumes we already have some expectations and only adds checks if we're
     * generating a JWT token
     * This test class will create either an access_token, or a JWT as access_token
     *
     * @param expectations
     * @param settings
     * @param extraIds
     * @return
     * @throws Exception
     */
    public List<validationData> addResourceChecks(List<validationData> expectations, String step, TestSettings settings, String expectedResourceIds) throws Exception {

        // if we're NOT building a JWT as access_token, we do NOT need to check the aud value within the JWT (we won't have a JWT)
        if (settings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            return expectations;
        }
        if (expectedResourceIds == null) {
            expectations = vData.addExpectation(expectations, step, Constants.RESPONSE_JWT_TOKEN, Constants.STRING_CONTAINS, "Token did NOT contain a valid " + Constants.PAYLOAD_AUDIENCE + " claim", Constants.PAYLOAD_AUDIENCE, null);
            return expectations;
        }
        for (String id : expectedResourceIds.split("\\s+")) {
            expectations = vData.addExpectation(expectations, step, Constants.RESPONSE_JWT_TOKEN, Constants.STRING_CONTAINS, "Token did NOT contain a valid " + Constants.PAYLOAD_AUDIENCE + " claim", Constants.PAYLOAD_AUDIENCE, id);
        }
        return expectations;

    }

    /**
     * A positive authoriation_code flow test
     *
     * @param client
     *            - The OP client to use (value built into the request)
     * @param reqResourceIds
     *            - The resource value to pass on the request
     * @param expResourceIds
     *            - The resource values to find in the built token
     * @throws Exception
     */
    private void positiveAuthCode(String client, String reqResourceIds, String expResourceIds) throws Exception {
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = grantTypeSettings(client, grantType);
        updatedTestSettings.setResourceIds(reqResourceIds);

        List<validationData> expectations = vData.addSuccessStatusCodes();
        // add generic id_token expectations
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.INVOKE_TOKEN_ENDPOINT, updatedTestSettings);
        // add generic response expectations
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.INVOKE_TOKEN_ENDPOINT, updatedTestSettings);
        // verify that refresh_token is issued in the response
        expectations = vData.addTokenInResponseExpectation(expectations, Constants.INVOKE_TOKEN_ENDPOINT, Constants.REFRESH_TOKEN_GRANT_TYPE);
        expectations = addJWTTokenExpectations(expectations, Constants.INVOKE_TOKEN_ENDPOINT, updatedTestSettings, expResourceIds);

        WebResponse response = genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_NOJSP_ACTIONS, expectations);
        response = invokeTokenEndpoint_authCode(_testName, wc, response, updatedTestSettings, expectations);

    }

    /**
     * A negative authoriation_code flow test
     *
     * @param client
     *            - The OP client to use (value built into the request)
     * @param reqResourceIds
     *            - The resource value to pass on the request
     * @throws Exception
     */
    private void negativeAuthCode(String client, String reqResourceIds) throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = grantTypeSettings(client, grantType);
        updatedTestSettings.setResourceIds(reqResourceIds);

        List<validationData> expectations = negativeExpectations(Constants.INVOKE_TOKEN_ENDPOINT);

        WebResponse response = genericOP(_testName, wc, updatedTestSettings, Constants.BASIC_AUTHENTICATION_NOJSP_ACTIONS, expectations);
        response = invokeTokenEndpoint_authCode(_testName, wc, response, updatedTestSettings, expectations);
    }

    /**
     * A positive implicit flow test
     *
     * @param client
     *            - The OP client to use (value built into the request)
     * @param reqResourceIds
     *            - The resource value to pass on the request
     * @param expResourceIds
     *            - The resource values to find in the built token
     * @throws Exception
     */
    private void positiveImplicit(String client, String reqResourceIds, String expResourceIds) throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = grantTypeSettings(client, grantType);
        updatedTestSettings.setResourceIds(reqResourceIds);

        updatedTestSettings = setImplicitClientDefaultSettings(updatedTestSettings);

        List<validationData> expectations = vData.addSuccessStatusCodes();
        // add generic id_token expectations
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.INVOKE_AUTH_ENDPOINT, updatedTestSettings);
        // add generic response expectations
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.INVOKE_AUTH_ENDPOINT, updatedTestSettings);

        expectations = addJWTTokenExpectations(expectations, Constants.INVOKE_AUTH_ENDPOINT, updatedTestSettings, expResourceIds);

        invokeAuthorizationEndpoint_implicit(_testName, wc, updatedTestSettings, expectations);

    }

    /**
     * A negative implicit flow test
     *
     * @param client
     *            - The OP client to use (value built into the request)
     * @param reqResourceIds
     *            - The resource value to pass on the request
     * @param expResourceIds
     *            - The resource values to find in the built token
     * @throws Exception
     */
    private void negativeImplicit(String client, String reqResourceIds) throws Exception {

        // Create the conversation object which will maintain state for us
        WebConversation wc = new WebConversation();

        // update settings to use the provider with authAuth set to true
        TestSettings updatedTestSettings = grantTypeSettings(client, grantType);
        updatedTestSettings.setResourceIds(reqResourceIds);
        updatedTestSettings = setImplicitClientDefaultSettings(updatedTestSettings);

        List<validationData> expectations = negativeExpectations(Constants.INVOKE_AUTH_ENDPOINT);

        invokeAuthorizationEndpoint_implicit(_testName, wc, updatedTestSettings, expectations);
    }

    /**
     * Creates the negative expectations (bad status code, error information in the response and error messages that should be
     * found in the the OP message log)
     *
     * @param step
     *            - the step that should get the error
     * @return - newly created expectations containing checks for the expected error
     * @throws Exception
     */
    private List<validationData> negativeExpectations(String step) throws Exception {

        List<validationData> expectations = vData.addSuccessStatusCodes(null, step);
        expectations = vData.addResponseStatusExpectation(expectations, step, Constants.BAD_REQUEST_STATUS);
        expectations = vData.addJSONExpectation(expectations, step, "Did not receive internal server exception (" + Constants.ERROR_RESPONSE_DESCRIPTION + ")", Constants.ERROR_RESPONSE_PARM, Constants.ERROR_CODE_INVALID_REQUEST);

        expectations = vData.addJSONExpectation(expectations, step, "Did not receive internal server exception (" + Constants.ERROR_RESPONSE_DESCRIPTION + ": )", Constants.ERROR_RESPONSE_DESCRIPTION, MessageConstants.CWOAU0072E_ILLEGAL_PARAMETER);

        validationTools.addMessageExpectation(testOPServer, expectations, step, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did NOT find a message in the log indicating that the resource was invalid.", MessageConstants.CWOAU0072E_ILLEGAL_PARAMETER + ".+resource+.");

        return expectations;

    }

    /***********************************************************************************************************************************/

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists one value in the resourceIds attribute (using
     * client_oneResourceId_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request passes the configured resource value in the resource parm
     * The request should succeed and the aud value should be the value passed in the request
     *
     */
    @Test
    public void GenericResourceTests_ServerNoGrantWithResourceIds_RequestAuthCodeWithResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveAuthCode("client_oneResourceId_allGrantTypes", "xx", "xx");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists one value in the resourceIds attribute (using
     * client_oneResourceId_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request does NOT pass the resource parm
     * The request should succeed and the aud value should be the value configured in the config's client
     *
     */
    @Test
    public void GenericResourceTests_ServerNoGrantWithResourceIds_RequestAuthCodeWithNoResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveAuthCode("client_oneResourceId_allGrantTypes", null, "xx");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists one value in the resourceIds attribute (using
     * client_oneResourceId_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request passes multiple values in the resource parm
     * The request should fail as there are values in the resource parm that are NOT in the configuration
     *
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void GenericResourceTests_ServerNoGrantWithResourceIds_RequestAuthCodeWithMultipleResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource
        negativeAuthCode("client_oneResourceId_allGrantTypes", "xx zz");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists one value in the resourceIds attribute (using
     * client_oneResourceId_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request passes one value in the resource parm and it does NOT match what's in the client config
     * The request should fail as the value in the resource parm is NOT in the configuration
     *
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void GenericResourceTests_ServerNoGrantWithResourceIds_RequestAuthCodeWithMisMatchResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource
        negativeAuthCode("client_oneResourceId_allGrantTypes", "zz");

    }

    /***/

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists one value in the resourceIds attribute (using
     * client_oneResourceId_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request passes the configured resource value in the resource parm
     * The request should succeed and the aud value should be the value passed in the request
     *
     */
    @Test
    public void GenericResourceTests_ServerNoGrantWithResourceIds_RequestImplicitWithResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveImplicit("client_oneResourceId_allGrantTypes", "xx", "xx");
    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists one value in the resourceIds attribute (using
     * client_oneResourceId_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request does NOT pass the resource parm
     * The request should succeed and the aud value should be the value configured in the config's client
     *
     */
    @Test
    public void GenericResourceTests_ServerNoGrantWithResourceIds_RequestImplicitWithNoResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveImplicit("client_oneResourceId_allGrantTypes", null, "xx");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists one value in the resourceIds attribute (using
     * client_oneResourceId_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request passes multiple values in the resource parm
     * The request should fail as there are values in the resource parm that are NOT in the configuration
     *
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void GenericResourceTests_ServerNoGrantWithResourceIds_RequestImplicitWithMultipleResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource
        negativeImplicit("client_oneResourceId_allGrantTypes", "xx zz");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists one value in the resourceIds attribute (using
     * client_oneResourceId_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request passes one value in the resource parm and it does NOT match what's in the client config
     * The request should fail as the value in the resource parm is NOT in the configuration
     *
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void GenericResourceTests_ServerNoGrantWithResourceIds_RequestImplicitWithMisMatchResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource
        negativeImplicit("client_oneResourceId_allGrantTypes", "zz");

    }

    /***/

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that does NOT set the resourceIds attribute (using client_noResourceIds_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request passes a value in the resource parm
     * The request should fail as there are values in the resource part and there are NONE in the configuration
     *
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void GenericResourceTests_ServerNoGrantWithNoResourceIds_RequestAuthCodeWithResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource
        negativeAuthCode("client_noResourceIds_allGrantTypes", "xx");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that does NOT set the resourceIds attribute (using client_noResourceIds_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request does NOT pass the resource parm
     * The request should succeed and the aud value should be the value passed in the request
     *
     */
    @Test
    public void GenericResourceTests_ServerNoGrantWithNoResourceIds_RequestAuthCodeWithNoResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveAuthCode("client_noResourceIds_allGrantTypes", null, null);

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that does NOT set the resourceIds attribute (using client_noResourceIds_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request passes multiple values in the resource parm
     * The request should fail as the values in the resource parm are NOT in the configuration
     *
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void GenericResourceTests_ServerNoGrantWithNoResourceIds_RequestAuthCodeWithMultipleResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource
        negativeAuthCode("client_noResourceIds_allGrantTypes", "xx zz");

    }

    /***/

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that does NOT set the resourceIds attribute (using client_noResourceIds_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request passes a value in the resource parm
     * The request should fail as there are values in the resource part and there are NONE in the configuration
     *
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void GenericResourceTests_ServerNoGrantWithNoResourceIds_RequestImplicitWithResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource
        negativeImplicit("client_noResourceIds_allGrantTypes", "xx");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that does NOT set the resourceIds attribute (using client_noResourceIds_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request does NOT pass the resource parm
     * The request should succeed and aud should NOT be set
     *
     */
    @Test
    public void GenericResourceTests_ServerNoGrantWithNoResourceIds_RequestImplicitWithNoResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveImplicit("client_noResourceIds_allGrantTypes", null, null);

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that does NOT set the resourceIds attribute (using client_noResourceIds_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request passes multiple values in the resource parm
     * The request should fail as the values in the resource parm are NOT in the configuration
     *
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void GenericResourceTests_ServerNoGrantWithNoResourceIds_RequestImplicitWithMultipleResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource
        negativeImplicit("client_noResourceIds_allGrantTypes", "xx zz");

    }

    /***/
    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists multiple values in the resourceIds attribute (using
     * client_multipleResourceIds_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request passes a configured resource value in the resource parm
     * The request should succeed and the aud value should be the value passed in the request
     *
     */
    @Test
    public void GenericResourceTests_ServerNoGrantWithMultipleResourceIds_RequestAuthCodeWithResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveAuthCode("client_multipleResourceIds_allGrantTypes", "xx", "xx");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists multiple values in the resourceIds attribute (using
     * client_multipleResourceIds_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request does NOT pass the resource parm
     * The request should succeed and the aud value should be all of the values configured in the config's client
     *
     */
    @Test
    public void GenericResourceTests_ServerNoGrantWithMultipleResourceIds_RequestAuthCodeWithNoResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveAuthCode("client_multipleResourceIds_allGrantTypes", null, "xx yy zz");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists multiple values in the resourceIds attribute (using
     * client_multipleResourceIds_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request passes two of the configured resource values in the resource parm
     * The request should succeed and the aud value should be the values passed in the request
     *
     */
    @Test
    public void GenericResourceTests_ServerNoGrantWithMultipleResourceIds_RequestAuthCodeWithMultipleResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveAuthCode("client_multipleResourceIds_allGrantTypes", "xx zz", "xx zz");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists multiple values in the resourceIds attribute (using
     * client_multipleResourceIds_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request passes a configured resource value in the resource parm
     * The request should succeed and the aud value should be the value passed in the request
     *
     */
    @Test
    public void GenericResourceTests_ServerNoGrantWithMultipleResourceIds_RequestImplicitWithResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveImplicit("client_multipleResourceIds_allGrantTypes", "xx", "xx");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists multiple values in the resourceIds attribute (using
     * client_multipleResourceIds_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request does NOT pass the resource parm
     * The request should succeed and the aud value should be all of the values configured in the config's client
     *
     */
    @Test
    public void GenericResourceTests_ServerNoGrantWithMultipleResourceIds_RequestImplicitWithNoResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveImplicit("client_multipleResourceIds_allGrantTypes", null, "xx yy zz");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists multiple values in the resourceIds attribute (using
     * client_multipleResourceIds_allGrantTypes)
     * GrantType is NOT set in the config's client (all grant types are implied)
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request passes two of the configured resource values in the resource parm
     * The request should succeed and the aud value should be the values passed in the request
     *
     */
    @Test
    public void GenericResourceTests_ServerNoGrantWithMultipleResourceIds_RequestImplicitWithMultipleResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveImplicit("client_multipleResourceIds_allGrantTypes", "xx zz", "xx zz");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists one value in the resourceIds attribute (using client_oneResourceId_authCode)
     * GrantType is set to authorization_code in the config's client
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request passes the configured resource value in the resource parm
     * The request should succeed and the aud value should be the value passed in the request
     *
     */
    @Test
    public void GenericResourceTests_ServerAuthCodeGrantWithResourceIds_RequestAuthCodeWithResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveAuthCode("client_oneResourceId_authCode", "xx", "xx");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists one value in the resourceIds attribute (using client_oneResourceId_authCode)
     * GrantType is set to authorization_code in the config's client
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request does NOT pass the resource parm
     * The request should succeed and the aud value should be the value configured in the config's client
     *
     */
    @Test
    public void GenericResourceTests_ServerAuthCodeGrantWithResourceIds_RequestAuthCodeWithNoResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveAuthCode("client_oneResourceId_authCode", null, "xx");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists one value in the resourceIds attribute (using client_oneResourceId_authCode)
     * GrantType is set to authorization_code in the config's client
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request passes multiple values in the resource parm
     * The request should fail as there are values in the resource parm that are NOT in the configuration
     *
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void GenericResourceTests_ServerAuthCodeGrantWithResourceIds_RequestAuthCodeWithMultipleResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource
        negativeAuthCode("client_oneResourceId_authCode", "xx zz");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that does NOT set the resourceIds attribute (using client_noResourceIds_authCode)
     * GrantType is set to authorization_code in the config's client
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request passes a value in the resource parm
     * The request should fail as there are values in the resource part and there are NONE in the configuration
     *
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void GenericResourceTests_ServerAuthCodeGrantWithNoResourceIds_RequestAuthCodeWithResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource
        negativeAuthCode("client_noResourceIds_authCode", "xx");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that does NOT set the resourceIds attribute (using client_noResourceIds_authCode)
     * GrantType is set to authorization_code in the config's client
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request does NOT pass the resource parm
     * The request should succeed and the aud value should be the value passed in the request
     *
     */
    @Test
    public void GenericResourceTests_ServerAuthCodeGrantWithNoResourceIds_RequestAuthCodeWithNoResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveAuthCode("client_noResourceIds_authCode", null, null);

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that does NOT set the resourceIds attribute (using client_noResourceIds_authCode)
     * GrantType is set to authorization_code in the config's client
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request passes multiple values in the resource parm
     * The request should fail as the values in the resource parm are NOT in the configuration
     *
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void GenericResourceTests_ServerAuthCodeGrantWithNoResourceIds_RequestAuthCodeWithMultipleResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource
        negativeAuthCode("client_noResourceIds_authCode", "xx zz");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists multiple values in the resourceIds attribute (using
     * client_multipleResourceIds_authCode)
     * GrantType is set to authorization_code in the config's client
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request passes a configured resource value in the resource parm
     * The request should succeed and the aud value should be the value passed in the request
     *
     */
    @Test
    public void GenericResourceTests_ServerAuthCodeGrantWithMultipleResourceIds_RequestAuthCodeWithResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveAuthCode("client_multipleResourceIds_authCode", "xx", "xx");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists multiple values in the resourceIds attribute (using
     * client_multipleResourceIds_authCode)
     * GrantType is set to authorization_code in the config's client
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request does NOT pass the resource parm
     * The request should succeed and the aud value should be all of the values configured in the config's client
     *
     */
    @Test
    public void GenericResourceTests_ServerAuthCodeGrantWithMultipleResourceIds_RequestAuthCodeWithNoResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveAuthCode("client_multipleResourceIds_authCode", null, "xx yy zz");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists multiple values in the resourceIds attribute (using
     * client_multipleResourceIds_authCode)
     * GrantType is set to authorization_code in the config's client
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request passes two of the configured resource values in the resource parm
     * The request should succeed and the aud value should be the values passed in the request
     *
     */
    @Test
    public void GenericResourceTests_ServerAuthCodeGrantWithMultipleResourceIds_RequestAuthCodeWithMultipleResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveAuthCode("client_multipleResourceIds_authCode", "xx zz", "xx zz");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists multiple values in the resourceIds attribute (using
     * client_multipleResourceIds_authCode)
     * GrantType is set to authorization_code in the config's client
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request passes all three of the configured resource values in the resource parm, plus an extra value
     * The request should fail as there are values in the resource parm that are NOT in the configuration
     *
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void GenericResourceTests_ServerAuthCodeGrantWithMultipleResourceIds_RequestAuthCodeWithMultipleAdditionalResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        negativeAuthCode("client_multipleResourceIds_authCode", "xx yyy aaa zz");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists multiple values in the resourceIds attribute (using
     * client_multipleResourceIds_authCode)
     * GrantType is set to authorization_code in the config's client
     * Uses TokenEndpoint
     * The request passes a grant type of authoriation_code
     * The request passes multiple values in the resource, none of which are in the config
     * The request should fail as there are values in the resource parm that are NOT in the configuration
     *
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void GenericResourceTests_ServerAuthCodeGrantWithMultipleResourceIds_RequestAuthCodeWithMultipleMisMatchResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        negativeAuthCode("client_multipleResourceIds_authCode", "aa bb cc dd ee");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists one value in the resourceIds attribute (using client_oneResourceId_implicit)
     * GrantType is set to implicit in the config's client
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request passes the configured resource value in the resource parm
     * The request should succeed and the aud value should be the value passed in the request
     *
     */
    @Test
    public void GenericResourceTests_ServerImplicitGrantWithResourceIds_RequestImplicitWithResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveImplicit("client_oneResourceId_implicit", "xx", "xx");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists one value in the resourceIds attribute (using client_oneResourceId_implicit)
     * GrantType is set to implicit in the config's client
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request does NOT pass the resource parm
     * The request should succeed and the aud value should be the value configured in the config's client
     *
     */
    @Test
    public void GenericResourceTests_ServerImplicitGrantWithResourceIds_RequestImplicitWithNoResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveImplicit("client_oneResourceId_implicit", null, "xx");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists one value in the resourceIds attribute (using client_oneResourceId_implicit)
     * GrantType is set to implicit in the config's client
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request passes multiple values in the resource parm
     * The request should fail as there are values in the resource parm that are NOT in the configuration
     *
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void GenericResourceTests_ServerImplicitGrantWithResourceIds_RequestImplicitWithMultipleResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource
        negativeImplicit("client_oneResourceId_implicit", "xx zz");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that does NOT set the resourceIds attribute (using client_noResourceIds_implicit)
     * GrantType is set to implicit in the config's client
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request passes a value in the resource parm
     * The request should fail as there are values in the resource part and there are NONE in the configuration
     *
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void GenericResourceTests_ServerImplicitGrantWithNoResourceIds_RequestImplicitWithResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource
        negativeImplicit("client_noResourceIds_implicit", "xx");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that does NOT set the resourceIds attribute (using client_noResourceIds_implicit)
     * GrantType is set to implicit in the config's client
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request does NOT pass the resource parm
     * The request should succeed and aud should NOT be set
     *
     */
    @Test
    public void GenericResourceTests_ServerImplicitGrantWithNoResourceIds_RequestImplicitWithNoResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveImplicit("client_noResourceIds_implicit", null, null);

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that does NOT set the resourceIds attribute (using client_noResourceIds_implicit)
     * GrantType is set to implicit in the config's client
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request passes multiple values in the resource parm
     * The request should fail as the values in the resource parm are NOT in the configuration
     *
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void GenericResourceTests_ServerImplicitGrantWithNoResourceIds_RequestImplicitWithMultipleResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource
        negativeImplicit("client_noResourceIds_implicit", "xx zz");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists multiple values in the resourceIds attribute (using
     * client_multipleResourceIds_implicit)
     * GrantType is set to implicit in the config's client
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request passes a configured resource value in the resource parm
     * The request should succeed and the aud value should be the value passed in the request
     *
     */
    @Test
    public void GenericResourceTests_ServerImplicitGrantWithMultipleResourceIds_RequestImplicitWithResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveImplicit("client_multipleResourceIds_implicit", "xx", "xx");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists multiple values in the resourceIds attribute (using
     * client_multipleResourceIds_implicit)
     * GrantType is set to implicit in the config's client
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request does NOT pass the resource parm
     * The request should succeed and the aud value should be all of the values configured in the config's client
     *
     */
    @Test
    public void GenericResourceTests_ServerImplicitGrantWithMultipleResourceIds_RequestImplicitWithNoResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveImplicit("client_multipleResourceIds_implicit", null, "xx yy zz");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists multiple values in the resourceIds attribute (using
     * client_multipleResourceIds_implicit)
     * GrantType is set to implicit in the config's client
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request passes two of the configured resource values in the resource parm
     * The request should succeed and the aud value should be the values passed in the request
     *
     */
    @Test
    public void GenericResourceTests_ServerImplicitGrantWithMultipleResourceIds_RequestImplicitWithMultipleResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        positiveImplicit("client_multipleResourceIds_implicit", "xx zz", "xx zz");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists multiple values in the resourceIds attribute (using
     * client_multipleResourceIds_implicit)
     * GrantType is set to implicit in the config's client
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request passes all three of the configured resource values in the resource parm, plus an extra value
     * The request should fail as there are values in the resource parm that are NOT in the configuration
     *
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void GenericResourceTests_ServerImplicitGrantWithMultipleResourceIds_RequestImplicitWithMultipleAdditionalResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        negativeImplicit("client_multipleResourceIds_implicit", "xx yy aa zz");

    }

    /**
     * TestDescription:
     *
     * This test uses an OIDC/OAuth client that lists multiple values in the resourceIds attribute (using
     * client_multipleResourceIds_implicit)
     * GrantType is set to implicit in the config's client
     * Uses AuthorizationEndpoint
     * The request passes a grant type of implicit
     * The request passes multiple values in the resource, none of which are in the config
     * The request should fail as there are values in the resource parm that are NOT in the configuration
     *
     */
    @ExpectedFFDC({ "com.ibm.oauth.core.api.error.OidcServerException" })
    @Test
    public void GenericResourceTests_ServerImplicitGrantWithMultipleResourceIds_RequestImplicitWithMultipleMisMatchResourceId() throws Exception {

        // Parms:
        //      clientId,
        //      passed resource,
        //      expected resources
        negativeImplicit("client_multipleResourceIds_implicit", "aa bb cc dd ee");

    }

}
