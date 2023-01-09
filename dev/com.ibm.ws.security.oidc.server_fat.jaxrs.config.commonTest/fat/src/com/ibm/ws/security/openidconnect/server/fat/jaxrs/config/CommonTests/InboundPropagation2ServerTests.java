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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.CommonTests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.AuthorizationHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonCookieTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.JwtUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.structures.ValidationDataToExpectationConverter;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.Utils.CommonTools;
import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

public class InboundPropagation2ServerTests extends CommonTest {

    private static final Class<?> thisClass = InboundPropagation2ServerTests.class;
    protected static AuthorizationHelpers authHelpers = new AuthorizationHelpers();
    protected static CommonCookieTools cookieTools = new CommonCookieTools();
    public static CommonValidationTools validationTools = new CommonValidationTools();
    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    protected static String targetProvider = null;
    protected static String targetISSEndpoint = null;
    protected static String[] goodActions = null;
    protected static String flowType = Constants.WEB_CLIENT_FLOW;
    protected static String OPServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.OPserver";
    protected static String RSServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.RSserver";
    protected static final String oldAccessToken = "eNVMlACDRk7RKEi8AYp45Y2uogVACpERnHZfYDq6";
    /** The port number in the issuer claim of {@link oldJWTToken} */
    protected static final int oldJWTTokenIssuerPort = 8947;
    protected static CommonTools commonTools = new CommonTools();
    protected static String REALM_FROM_ISS = "http:"; //TODO: this will change once the 158219 is addressed

    private static boolean alreadyThrewFFDC = false;
    private static final String WWW_AUTHENTICATE_HEADER = "WWW-Authenticate";
    protected static int OIDC_ERROR_1 = 1;

    private JwtUtils jwtUtils = new JwtUtils();
    
    protected static boolean isTestingOIDC = false;

    @Before
    public void beforeTest() {
        createCommonPropagationToken();
    }

    /**
     * Expects:
     * - Uses the common propagation token to invoke the protected resource
     * - All successful status codes
     * - Did not get to the protected resource
     * - Did not find "error" parameter
     * - Received a response from authorization endpoint, not an invalid redirect URI
     * - Found CWOAU0062E message in the response
     * - Found CWOAU0056E message in OP messages log
     *
     * @param settings
     * @throws Exception
     */
    private void generalInboundNoneTest(TestSettings settings) throws Exception {
        List<validationData> expectations = getInboundNoneExpectations();
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, settings, expectations);
    }

    /**
     * Expects:
     * - All successful status codes
     * - Did not get to the protected resource
     * - Did not find "error" parameter
     * - Received a response from authorization endpoint, not an invalid redirect URI
     * - Found CWOAU0062E message in the response
     * - Found CWOAU0056E FFDC message in OP messages log (if that FFDC has not yet already been thrown)
     *
     * @return
     * @throws Exception
     */
    private List<validationData> getInboundNoneExpectations() throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Got to the protected servlet when we should not have.", null, Constants.HELLOWORLD_MSG);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found unexpected \"" + Constants.ERROR_RESPONSE_PARM + "\" in response.", null, "\"" + Constants.ERROR_RESPONSE_PARM + "\"");
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Should have received a response from the authorization endpoint and not been redirected to the invalid redirect URL.", null, "/" + genericTestServer.getProviderApp() + "/authorize");
        expectations = vData.addResponseExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, "Did not find CWOAU0062E message saying the redirect URI was invalid.", MessageConstants.CWOAU0062E_REDIRECT_URI_INVALID);
        if (!alreadyThrewFFDC) {
            // Only one unique FFDC will be generated for the first test calling this method, so this FFDC message should only be found in the first test
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find CWOAU0056E message in the OP logs saying redirect URI was not among those registered with the OP.", MessageConstants.CWOAU0056E_REDIRECT_URI_NOT_REGISTERED);
            alreadyThrewFFDC = true;
        }
        return expectations;
    }

    /***************************************************** Tests *****************************************************/

    /**
     * inboundPropagation: Not specified in config
     * Expected results:
     * - Accessing the RS-protected app should trigger the OAuth authorization flow since inboundPropagation="none" by default
     * - The runtime will construct its own guess as to what the appropriate redirect URI is based on the redirectToRPHostAndPort
     * config attribute
     * - The constructed redirect URI will not match the one registered with the OP
     * - End user will see CWOAU0062E message saying the redirect URI was not valid
     * - OP logs will have FFDC and CWOAU0056E message saying redirect URI was not registered
     * - Per section 4.1.2.1 of the OAuth spec (RFC6749), we should not redirect to the invalid URI and should not include an
     * "error" parameter in the response
     *
     * @throws Exception
     */
    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException")
    @Test
    public void InboundPropagation2ServerTests_inboundPropagation_notSpecified() throws Exception {
        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_ipNotSpecified");
        generalInboundNoneTest(updatedTestSettings);
    }

    /**
     * inboundPropagation: "none"
     * Expected results:
     * - Same results as if inboundPropagation were not specified
     *
     * @throws Exception
     */
    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException")
    @Test
    public void InboundPropagation2ServerTests_inboundPropagation_none() throws Exception {
        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_inboundPropNone");
        generalInboundNoneTest(updatedTestSettings);
    }

    /**
     * inboundPropagation: "none"
     * Propagation token is not included in request to protected resource
     * Expected results:
     * - Same results as if inboundPropagation were not specified
     *
     * @throws Exception
     */
    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException")
    @Test
    public void InboundPropagation2ServerTests_inboundPropagation_none_missingPropagationToken() throws Exception {
        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_inboundPropNone");
        updatedTestSettings.setWhere(null);
        generalInboundNoneTest(updatedTestSettings);
    }

    /**
     * inboundPropagation: "none"
     * RS-constructed redirect URI is registered with OP
     * Expected results:
     * - Accessing the RS-protected app should trigger the OAuth authorization flow since inboundPropagation="none"
     * - The runtime will construct its own guess as to what the appropriate redirect URI is based on the redirectToRPHostAndPort
     * config attribute
     * - The constructed redirect URI will match one registered with the OP
     * - Should ultimately reach the login page
     *
     * @throws Exception
     */
    @Test
    public void InboundPropagation2ServerTests_inboundPropagation_none_redirectUriRegistered() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_inboundPropNone_client02");
        updatedTestSettings.setClientID("client02");
        updatedTestSettings.setClientName("client02");

        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addResponseExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, "Should have reached the login prompt page.", Constants.LOGIN_PROMPT);

        // Obtain a propagation token
        Log.info(thisClass, testName.getMethodName(), "Obtaining propagation token for client02");
        WebResponse response = genericOP(_testName, new WebConversation(), updatedTestSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, expectations);
        String propagationToken = validationTools.getTokenForType(updatedTestSettings, response);

        String validationType = genericTestServer.getRSValidationType();

        // Use the token with selected validation method/endpoint
        Log.info(thisClass, testName.getMethodName(), "Invoking RS protected resource using " + validationType + " validation method");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), propagationToken, updatedTestSettings, expectations);
    }

    /**
     * inboundPropagation: "required"
     * Expected results:
     * - Successfully reached the RS-protected resource
     *
     * @throws Exception
     */    
    @Test
    public void InboundPropagation2ServerTests_inboundPropagation_required() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_inboundPropRequired");

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings);

        String validationType = genericTestServer.getRSValidationType();
        if (validationType.equals(Constants.USERINFO_ENDPOINT)) {
            // userinfo endpoint does not include a realm
            expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, REALM_FROM_ISS);
        }

        WebConversation wc = new WebConversation();
        helpers.invokeRsProtectedResource(_testName, wc, commonPropagationToken, updatedTestSettings, expectations);
        if (isTestingOIDC && ! commonPropagationToken.contains(".")) {  // test randomly chooses token types, this isn't valid for jwt's.
        	testUserInfo(wc, commonPropagationToken);   // add test for #8203, #8222
        }
    }
    
    /**
     * Test that userinfo is retrieved and available from an API call. If userinfo url is defined and enabled in metadata, then
     * upon authentication with an opaque token, 
     * the userinfo JSON from the OP, if available, is to be stored in the subject as a string and made
     * accessible through the PropagationHelper API. 
     * This calls a jsp that invokes the PropagationHelper.getUserInfo() API to check the userinfo.
     * The userinfo endpoint only works on oidc unless the internal attrib requireOpenidScopeForUserInfo is set to false.
     */
    void testUserInfo(WebConversation wc, String opaqueToken) throws Exception {
        String endpoint = "https://localhost:" + genericTestServer.getHttpDefaultSecurePort() + "/helloworld/propagationHelperUserInfoApiTest.jsp";

        GetMethodWebRequest request = new GetMethodWebRequest(endpoint);
        request.setHeaderField("Authorization", "Bearer " + opaqueToken);
        WebResponse resp = wc.getResponse(request);
        String response = resp.getText();
        Log.info(thisClass, _testName, "Got JSP response: " + response);

        String testAction = "testUserInfo";
        String expectedUser = testSettings.getAdminUser();
        Expectations expectations = new Expectations();
        expectations.addExpectation(Expectation.createResponseExpectation(testAction, "\"sub\":\"" + expectedUser + "\"", "Did not find expected \"sub\" claim and value in the JSP response."));  
        expectations.addExpectation(new ResponseFullExpectation(testAction, Constants.STRING_MATCHES, "\"iss\":\"http[^\"]+/OidcConfigSample\"", "Did not find expected \"iss\" claim and value in the JSP response."));
//        expectations.addExpectation(new Expectation(testAction, Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "\"iss\":\"http[^\"]+/OidcConfigSample\"", "Did not find expected \"iss\" claim and value in the JSP response."));
        List<validationData> convertedExpectations = ValidationDataToExpectationConverter.convertExpectations(expectations);
        validationTools.validateResult(resp, testAction, convertedExpectations, testSettings);
    }

    /**
     * inboundPropagation: "required"
     * RS-protected resource invocation does not include propagation token
     * Expected results:
     * - Per section 3.1 of the Bearer token spec (RFC6750), 401 status when invoking RS-protected resource
     * - Per section 3 (RFC6750), response contains WWW-Authenticate header with an auth-scheme of "Bearer"
     * - Per section 3 (RFC6750), WWW-Authenticate header contains at least one auth-param value
     * - Per section 3.1 (RFC6750), response should not contain an error code or other error information
     *
     * @throws Exception
     */
    @Test
    public void InboundPropagation2ServerTests_inboundPropagation_required_missingPropagationToken() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_inboundPropRequired");

        // Section 3.1, Bearer Token spec (RFC6750): Example without authentication information shows a 401 being returned
        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);

        // Section 3 (RFC6750): Protected resource request without authentication credentials must include WWW-Authenticate header with "Bearer" auth-scheme
        String properAuthenticateHeader = WWW_AUTHENTICATE_HEADER.toUpperCase() + ": " + Constants.BEARER;
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_HEADER, Constants.STRING_CONTAINS, "Response did not include a WWW-Authenticate header with Bearer auth-scheme.", null, properAuthenticateHeader);

        // iss 3710 // expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying propagation token was missing.", MessageConstants.CWWKS1726E_MISSING_PROPAGATION_TOKEN);

        helpers.invokeRSProtectedResource(_testName, Constants.POSTMETHOD, null, null, updatedTestSettings, expectations);

        // TODO (Addressed by work item 211722)
        //        // Make sure WWW-Authenticate header exists, has the right auth-scheme, and contains at least one auth-param value
        //        Map<String, String> authParams = validateWWWAuthenticateHeader(response);
        //
        //        for (String param : authParams.keySet()) {
        //            // Section 3.1 (RFC6750): Requests lacking authentication information "SHOULD NOT include an error code or other error information"
        //            assertTrue("Found illegal auth-param value in the response: \"" + param + "\"", !param.equals(Constants.ERROR_RESPONSE_PARM)
        //                                                                                            && !param.equals(Constants.ERROR_RESPONSE_DESCRIPTION)
        //                                                                                            && !param.equals(Constants.ERROR_RESPONSE_URI));
        //        }
    }

    /**
     * inboundPropagation: "supported"
     * Expected results:
     * - Successfully reached the RS-protected resource
     *
     * @throws Exception
     */
    @Test
    public void InboundPropagation2ServerTests_inboundPropagation_supported() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_inboundPropSupported");

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings);

        String validationType = genericTestServer.getRSValidationType();
        if (validationType.equals(Constants.USERINFO_ENDPOINT)) {
            // userinfo endpoint does not include a realm
            expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, REALM_FROM_ISS);
        }

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * inboundPropagation: "supported"
     * RS-protected resource invocation does not include propagation token
     * Expected results:
     * - Same results as if inboundPropagation="none" or wasn't specified
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException")
    @Test
    public void InboundPropagation2ServerTests_inboundPropagation_supported_missingPropagationToken() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_inboundPropSupported");
        updatedTestSettings.setWhere(null);

        List<validationData> expectations = getInboundNoneExpectations();
        // iss 3710 //expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying token validation failed.", MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * inboundPropagation: "supported"
     * Submit protected resource request with expired propagation token
     * Expected results:
     * - Accessing resource with expired token should fail
     * - Request should fall back to pure OAuth/OIDC flow
     * - Request should fail the same as if inboundPropagation="none" were specified
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException", "java.lang.IllegalStateException" })
    @Test
    public void InboundPropagation2ServerTests_inboundPropagation_supported_expiredPropagationToken() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_inboundPropSupported");

        // Choose which expired token to use based on the selected token type
        String expiredToken = oldAccessToken;
        String tokenType = updatedTestSettings.getRsTokenType();
        if (Constants.JWT_TOKEN.equals(tokenType)) {
            String validKid = jwtUtils.getValidJwksKid(testSettings);
            expiredToken = jwtUtils.buildOldJwtString(updatedTestSettings, validKid);
        }

        // Expired token should not allow access, so should fall back to pure OAuth/OIDC as if inboundPropagation was set to "none"
        List<validationData> expectations = getInboundNoneExpectations();
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE,
                Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                "Did not get message in RS logs saying token validation failed.", MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP);

        if (Constants.ACCESS_TOKEN_KEY.equals(tokenType)) {
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE,
                    Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                    "Did not get message in RS logs saying token validation failed.", MessageConstants.CWWKS1720E_ACCESS_TOKEN_NOT_ACTIVE);
        }

        String validationType = genericTestServer.getRSValidationType();
        // Errors when accessing protected resource because propagation token is expired
        if (Constants.USERINFO_ENDPOINT.equals(validationType)) {
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in OP logs saying access token was not recognized.", MessageConstants.CWWKS1617E_USERINFO_REQUEST_BAD_TOKEN);
        } else if (Constants.INTROSPECTION_ENDPOINT.equals(validationType)) {
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in OP logs saying access token was not valid.", MessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
        } else {
            // Local validation - will see errors in the RS logs
            String insertMsg = MessageConstants.CWWKS1739E_JWT_KEY_NOT_FOUND;
            if (Constants.X509_CERT.equals(updatedTestSettings.getRsCertType())) {
                if (testOPServer.getHttpDefaultSecurePort() != oldJWTTokenIssuerPort) {
                    // The hard-coded old JWT has an issuer with port 8947, which typically should match the port we're using. However sometimes we don't get that port,
                    // in which case the error message we'll see is one saying the issuer in the token doesn't match one of the trusted issuers.
                    insertMsg = MessageConstants.CWWKS1781E_TOKEN_ISSUER_NOT_TRUSTED;
                } else {
                    insertMsg = MessageConstants.CWWKS1774E_AUD_INVALID;
                }
            } else {
                insertMsg = MessageConstants.CWWKS1774E_AUD_INVALID;
            }
            //expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying the OIDC client failed to validate the token.", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE + ".*" + insertMsg);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying the OIDC client failed to validate the token.", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE + ".*" + MessageConstants.CWWKS1773E_TOKEN_EXPIRED);
        }

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), expiredToken, updatedTestSettings, expectations);
    }

    /**
     * inboundPropagation: "none", then "supported", then "required", then "none" again
     * Attempt to access the protected resource with each configuration, including a valid token in every request
     * Expected results:
     * - When "none", the protected resource is not reached and a CWOAU0062E message is found in the response
     * - When "supported" and "required", the protected resource is successfully reached
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException")
    @Test
    public void InboundPropagation2ServerTests_inboundPropagation_none_supported_required_none() throws Exception {

        // inboundPropagation="none"
        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_inboundPropNone");
        List<validationData> unsuccessfulExpectations = getInboundNoneExpectations();
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, unsuccessfulExpectations);

        // inboundPropagation="supported"
        updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_inboundPropSupported");

        String validationType = genericTestServer.getRSValidationType();
        List<validationData> validAccessExpectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings);
        if (validationType.equals(Constants.USERINFO_ENDPOINT)) {
            // userinfo endpoint does not include a realm
            validAccessExpectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, REALM_FROM_ISS);
        }
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, validAccessExpectations);

        // inboundPropagation="required"
        updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_inboundPropRequired");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, validAccessExpectations);

        // inboundPropagation="none"
        updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_inboundPropNone");
        unsuccessfulExpectations = getInboundNoneExpectations();
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, unsuccessfulExpectations);
    }

    /**
     * inboundPropagation: "none", then "supported", then "required", then "none" again
     * Separate config files are used for each attribute value to ensure dynamic config works
     * Dynamically reconfigure between a single client configuration and multiple client configurations
     * Attempt to access the protected resource with each configuration, including a valid token in every request
     * Expected results:
     * - When "none", the protected resource is not reached and a CWOAU0062E message is found in the response
     * - When "supported" and "required", the protected resource is successfully reached
     *
     * @throws Exception
     */
    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException")
    @Test
    public void InboundPropagation2ServerTests_inboundPropagation_none_supported_required_none_dynamic() throws Exception {

        String validationType = genericTestServer.getRSValidationType();

        // inboundPropagation="none"
        genericTestServer.reconfigServer("server_inboundProp_none.xml", _testName, true, null);
        invokeResource("helloworld_inboundPropNone", validationType, true);

        // Invoke the resource with all client configurations present
        genericTestServer.reconfigServer("server_inboundProp_tests.xml", _testName, true, null);
        invokeResource("helloworld_inboundPropNone", validationType, true);

        // inboundPropagation="supported"
        genericTestServer.reconfigServer("server_inboundProp_supported.xml", _testName, true, null);
        invokeResource("helloworld_inboundPropSupported", validationType, false);

        // Invoke the resource with all client configurations present
        genericTestServer.reconfigServer("server_inboundProp_tests.xml", _testName, true, null);
        invokeResource("helloworld_inboundPropSupported", validationType, false);

        // inboundPropagation="required"
        genericTestServer.reconfigServer("server_inboundProp_required.xml", _testName, true, null);
        invokeResource("helloworld_inboundPropRequired", validationType, false);

        // Invoke the resource with all client configurations present
        genericTestServer.reconfigServer("server_inboundProp_tests.xml", _testName, true, null);
        invokeResource("helloworld_inboundPropRequired", validationType, false);

        // inboundPropagation="none"
        genericTestServer.reconfigServer("server_inboundProp_none.xml", _testName, true, null);
        invokeResource("helloworld_inboundPropNone", validationType, true);

        // Invoke the resource with all client configurations present
        genericTestServer.reconfigServer("server_inboundProp_tests.xml", _testName, true, null);
        invokeResource("helloworld_inboundPropNone", validationType, true);

    }

    /**************************************************** Helpers ****************************************************/

    public static void setRealmForValidationType(TestSettings settings) throws Exception {
        if (settings.getRsTokenType().equals(Constants.JWT_TOKEN)) {
            REALM_FROM_ISS = Constants.BASIC_REALM;
        } else {
            REALM_FROM_ISS = "http://" + targetISSEndpoint;
        }
    }

    /**
     * Invokes the protected resource using test settings and expectations based on the values provided.
     *
     * @param protectedApp
     *            App to invoke for a specific auth filter/client config
     * @param validationMethod
     *            "introspect" or "userinfo"
     * @param shouldFallBackToOAuth
     *            Should the resource invocation fall back to an OAuth flow
     * @throws Exception
     */
    private void invokeResource(String protectedApp, String validationMethod, boolean shouldFallBackToOAuth) throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, protectedApp);

        List<validationData> expectations;
        if (shouldFallBackToOAuth) {
            expectations = getInboundNoneExpectations();
        } else {
            if (validationMethod.equals(Constants.USERINFO_ENDPOINT)) {
                expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, REALM_FROM_ISS);
            } else {
                expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings);
            }
        }

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * Validates that a WWW-Authenticate header is present in the response, that it uses the "Bearer" auth-scheme, and
     * that at least one auth-param value is present.
     *
     * @param response
     * @return
     */
    private Map<String, String> validateWWWAuthenticateHeader(WebResponse response) {
        String method = "validateWWWAuthenticateHeader";

        // Response should contain WWW-Authenticate header
        String[] authenticateHeaderVals = response.getHeaderFields(WWW_AUTHENTICATE_HEADER.toUpperCase());
        assertEquals("Found unexpected number of " + WWW_AUTHENTICATE_HEADER + " header values.", 1, authenticateHeaderVals.length);

        // Section 3 (RFC6750): "All challenges defined by this specification MUST use the auth-scheme value 'Bearer'."
        String headerValue = authenticateHeaderVals[0].trim();
        Log.info(thisClass, method, WWW_AUTHENTICATE_HEADER + " header value: " + headerValue);
        assertTrue(WWW_AUTHENTICATE_HEADER + " header must use auth-scheme value of " + Constants.BEARER, headerValue.startsWith(Constants.BEARER));

        // Section 3 (RFC6750): The Bearer scheme "MUST be followed by one or more auth-param values"
        Map<String, String> authParams = extractAuthParams(Constants.BEARER, headerValue);
        assertTrue(WWW_AUTHENTICATE_HEADER + " header must include at least one auth-param value.", authParams.size() > 0);

        return authParams;
    }

    /**
     * Extracts the comma-separated auth-param values from the given header value. If a duplicate auth-param name is found,
     * fail() is called to fail the test case. Param names and values are placed in a map and returned.
     *
     * @param authScheme
     * @param headerValue
     * @param allowDuplicateParams
     * @return
     */
    private Map<String, String> extractAuthParams(String authScheme, String headerValue) {
        String method = "extractAuthParams";
        Log.info(thisClass, method, "Extracting auth-param values from header: " + headerValue);

        Map<String, String> authParams = new HashMap<String, String>();

        // Remove the auth-scheme from the header value
        String valueWithoutScheme = headerValue.substring(headerValue.indexOf(authScheme) + authScheme.length());
        // auth-param values should be separated by commas
        String[] params = valueWithoutScheme.split(",");
        for (int i = 0; i < params.length; i++) {
            params[i] = params[i].trim();
        }
        for (String param : params) {
            String[] paramSplit = param.split("=");
            if (paramSplit.length < 2) {
                Log.warning(thisClass, "Did not find expected name=\"value\" pattern for auth-param: " + param);
                continue;
            }
            String paramName = paramSplit[0];
            String paramValue = paramSplit[1];
            // Remove quotes if they are the first AND last characters in the param value
            if ((paramValue.indexOf('"') == 0) && (paramValue.lastIndexOf('"') == (paramValue.length() - 1))) {
                paramValue = paramValue.substring(1, paramValue.length() - 1);
            }

            // Section 3 (RFC6750): Responses must not include duplicate auth-params
            if (authParams.containsKey(paramName)) {
                fail("Found duplicate auth-param in response: " + paramName);
            }
            Log.info(thisClass, method, "Recording auth-param: " + paramName + "=\"" + paramValue + "\"");
            authParams.put(paramName, paramValue);
        }

        return authParams;
    }

}
