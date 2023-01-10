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

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.AuthorizationHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonCookieTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.Utils.CommonTools;
import com.meterware.httpunit.WebConversation;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

public class ValidationMethod2ServerTests extends CommonTest {

    private static final Class<?> thisClass = ValidationMethod2ServerTests.class;
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
    protected static CommonTools commonTools = new CommonTools();
    protected String defaultISSAccessId = "user:http://" + targetISSEndpoint + Constants.OIDC_USERNAME;
    protected static final String REALM_FROM_ISS = "http:"; //TODO: this will change once the 158219 is addressed

    protected final String OAUTH_OP_USERINFO_ERROR = "Will not run this test since pure OAuth providers cannot use the userinfo endpoint";

    private static boolean alreadyThrewFFDC = false;

    protected static int OIDC_ERROR_1 = 1;

    @Before
    public void beforeTest() {
        createCommonPropagationToken();
    }

    /***************************************************** Tests *****************************************************/

    /**
     * validationMethod: introspect
     * validationEndpointUrl: /introspect
     * Expected results:
     * - Successfully reached the protected resource
     *
     * @throws Exception
     */
    @Test
    public void ValidationMethod2ServerTests_validation_match_introspect() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_validation_match_introspect");

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * validationMethod: userinfo
     * validationEndpointUrl: /userinfo
     * Expected results:
     * - Will only run in the OIDC test bucket
     * - Successfully reached the protected resource
     * - Realm name is null in the credentials
     *
     * @throws Exception
     */
    @Test
    public void ValidationMethod2ServerTests_validation_match_userinfo() throws Exception {
        if (eSettings.getProviderType().equals(Constants.OAUTH_OP)) {
            Log.info(thisClass, _testName, OAUTH_OP_USERINFO_ERROR);
            return;
        }
        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_validation_match_userinfo");

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, REALM_FROM_ISS, defaultISSAccessId);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * validationMethod: introspect
     * validationEndpointUrl: /introspect
     * OP client used sets introspectTokens="false"
     * Expected results:
     * - Per section 2.2 of the Token Introspection spec (RFC7662), in an authorized introspection call where "the protected
     * resource is not allowed to
     * introspect [the] particular token," the "authorization server MUST return an introspection response with the 'active' field
     * set to 'false'."
     * - Per section 2.3 (RFC7662), this is not considered an error response and should therefore return a 200 status code.
     * For now:
     * - OP logs: CWWKS1420E message saying the client is not authorized to introspect tokens
     * - RS logs: CWWKS1723E message saying an invalid_client error occurred attempting to validate the access token
     *
     * @throws Exception
     */
    @Test
    public void ValidationMethod2ServerTests_validation_introspect_opIntrospectTokensFalse() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_validation_introspect_opIntrospectTokensFalse");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in OP logs saying the client is not authorized to introspect tokens.", MessageConstants.CWWKS1420E_CLIENT_NOT_AUTHORIZED_TO_INTROSPECT);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying an invalid_client error occurred.", MessageConstants.CWWKS1723E_INVALID_CLIENT_ERROR);
        // TODO
        // For now the base OAuth/OIDC runtime isn't spec compliant; an HTTP 200 response required by sections 2.2 and 2.3 of RFC 7662 is not being returned,
        // nor is a response with the "active" field set to "false" being returned

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * validationMethod: Not specified in config
     * validationEndpointUrl: ""
     * Expected results:
     * - Default validationMethod is "introspect"
     * - Request returns 401
     * - CWWKS1725E message in RS logs saying the validation endpoint was invalid
     *
     * @throws Exception
     */
    @Test
    public void ValidationMethod2ServerTests_validationMethod_notSpecified_validationEndpointUrl_empty() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_validMethod_notSpecified_validEndptUrl_empty");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying the validation endpoint was invalid.", MessageConstants.CWWKS1725E_VALIDATION_ENDPOINT_URL_NOT_VALID_OR_FAILED);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * validationMethod: Not specified in config
     * validationEndpointUrl: /introspect
     * Expected results:
     * - Default validationMethod is "introspect"
     * - Successfully reached the protected resource
     *
     * @throws Exception
     */
    @Test
    public void ValidationMethod2ServerTests_validationMethod_notSpecified_validationEndpointUrl_introspect() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_validMethod_notSpecified_validEndptUrl_introspect");

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * validationMethod: Not specified in config
     * validationEndpointUrl: Not specified in config
     * Expected results:
     * - Default validationMethod is "introspect"
     * - Request returns 401
     * - CWWKS1725E message in RS logs saying the validation endpoint was invalid
     *
     * @throws Exception
     */
    @Test
    public void ValidationMethod2ServerTests_validationMethod_notSpecified_validationEndpointUrl_notSpecified() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_validMethod_notSpecified_validEndptUrl_notSpecified");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying the validation endpoint was invalid.", MessageConstants.CWWKS1725E_VALIDATION_ENDPOINT_URL_NOT_VALID_OR_FAILED);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * validationMethod: Not specified in config
     * validationEndpointUrl: /userinfo
     * Expected results:
     * - Will only run in the OIDC test bucket
     * - Default validationMethod is "introspect"
     * - Request returns 401
     * - OP logs: CWWKS1633E message saying userinfo request contained unsupported "token" parameter
     * - RS logs: CWWKS1721E message saying could not validate the access token because it was not recognized by validation
     * endpoint
     *
     * @throws Exception
     */
    @Test
    public void ValidationMethod2ServerTests_validationMethod_notSpecified_validationEndpointUrl_userinfo() throws Exception {
        if (eSettings.getProviderType().equals(Constants.OAUTH_OP)) {
            Log.info(thisClass, _testName, OAUTH_OP_USERINFO_ERROR);
            return;
        }

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_validMethod_notSpecified_validEndptUrl_userinfo");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in OP logs saying the userinfo request contained an unsupported \"token\" parameter.", MessageConstants.CWWKS1633E_USERINFO_UNSUPPORTED_PARAM);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying the token couldn't be recognized.", MessageConstants.CWWKS1721E_OIDC_REQ_FAILED_ACCESS_TOKEN_NOT_VALID + ".+" + MessageConstants.CWWKS1633E_USERINFO_UNSUPPORTED_PARAM);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * validationMethod: /introspect
     * validationEndpointUrl: ""
     * Expected results:
     * - Request returns 401
     * - CWWKS1725E message in RS logs saying the validation endpoint was invalid
     *
     * @throws Exception
     */
    @Test
    public void ValidationMethod2ServerTests_validationMethod_introspect_validationEndpointUrl_empty() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_validMethod_introspect_validEndptUrl_empty");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying the validation endpoint was invalid.", MessageConstants.CWWKS1725E_VALIDATION_ENDPOINT_URL_NOT_VALID_OR_FAILED);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * validationMethod: /introspect
     * validationEndpointUrl: Not specified in config
     * Expected results:
     * - Request returns 401
     * - CWWKS1725E message in RS logs saying the validation endpoint was invalid
     *
     * @throws Exception
     */
    @Test
    public void ValidationMethod2ServerTests_validationMethod_introspect_validationEndpointUrl_notSpecified() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_validMethod_introspect_validEndptUrl_notSpecified");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying the validation endpoint was invalid.", MessageConstants.CWWKS1725E_VALIDATION_ENDPOINT_URL_NOT_VALID_OR_FAILED);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * validationMethod: introspect
     * validationEndpointUrl: /userinfo
     * Expected results:
     * - Will only run in the OIDC test bucket
     * - Request returns 401
     * - OP logs: CWWKS1633E message saying userinfo request contained unsupported "token" parameter
     * - RS logs: CWWKS1721E message saying could not validate the access token because it was not recognized by validation
     * endpoint
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ValidationMethod2ServerTests_validationMethod_introspect_validationEndpointUrl_userinfo() throws Exception {
        if (eSettings.getProviderType().equals(Constants.OAUTH_OP)) {
            Log.info(thisClass, _testName, OAUTH_OP_USERINFO_ERROR);
            return;
        }

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_validMethod_introspect_validEndptUrl_userinfo");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in OP logs saying the userinfo request contained an unsupported \"token\" parameter.", MessageConstants.CWWKS1633E_USERINFO_UNSUPPORTED_PARAM);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying the token couldn't be recognized.", MessageConstants.CWWKS1721E_OIDC_REQ_FAILED_ACCESS_TOKEN_NOT_VALID + ".+" + MessageConstants.CWWKS1633E_USERINFO_UNSUPPORTED_PARAM);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * validationMethod: /userinfo
     * validationEndpointUrl: ""
     * Expected results:
     * - Request returns 401
     * - CWWKS1725E message in RS logs saying the validation endpoint was invalid
     *
     * @throws Exception
     */
    @Test
    public void ValidationMethod2ServerTests_validationMethod_userinfo_validationEndpointUrl_empty() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_validMethod_userinfo_validEndptUrl_empty");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying the validation endpoint was invalid.", MessageConstants.CWWKS1725E_VALIDATION_ENDPOINT_URL_NOT_VALID_OR_FAILED);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * validationMethod: userinfo
     * validationEndpointUrl: /introspect
     * Expected results:
     * - Request returns 401
     * - OP logs: CWOAU0033E message saying a required runtime parameter was missing (client_id)
     * - RS logs: CWWKS1721E message saying could not validate the access token because it was not recognized by validation
     * endpoint
     *
     * @throws Exception
     */
    @Test
    public void ValidationMethod2ServerTests_validationMethod_userinfo_validationEndpointUrl_introspect() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_validMethod_userinfo_validEndptUrl_introspect");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in OP logs saying a required runtime parameter was missing.", MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING + ".+client_id");
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying the token couldn't be recognized.", MessageConstants.CWWKS1721E_OIDC_REQ_FAILED_ACCESS_TOKEN_NOT_VALID + ".+" + MessageConstants.CWWKS1406E_INTROSPECT_INVALID_CREDENTIAL);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * validationMethod: /userinfo
     * validationEndpointUrl: Not specified in config
     * Expected results:
     * - Request returns 401
     * - CWWKS1725E message in RS logs saying the validation endpoint was invalid
     *
     * @throws Exception
     */
    @Test
    public void ValidationMethod2ServerTests_validationMethod_userinfo_validationEndpointUrl_notSpecified() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_validMethod_userinfo_validEndptUrl_notSpecified");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying the validation endpoint was invalid.", MessageConstants.CWWKS1725E_VALIDATION_ENDPOINT_URL_NOT_VALID_OR_FAILED);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * validationMethod: Chosen at random
     * validationEndpointUrl: Some invalid URL format
     * Expected results:
     * - Request returns 401
     * - Introspection validation method: CWWKS1727E message in RS logs saying there was an error validating the access token
     * - Userinfo validation method: CWWKS1725E message in RS logs saying the validation endpoint was invalid
     *
     * @throws Exception
     */
    @ExpectedFFDC("java.lang.IllegalArgumentException")
    @Test
    public void ValidationMethod2ServerTests_validationEndpointUrl_invalidUrlFormat() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_validEndptUrl_invalidUrlFormat");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        if (genericTestServer.getRSValidationType().equals(Constants.INTROSPECTION_ENDPOINT)) {
            validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying there was an error validating the access token.", MessageConstants.CWWKS1727E_ERROR_VALIDATING_ACCESS_TOKEN);
        } else {
            validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying the validation endpoint was not valid.", MessageConstants.CWWKS1725E_VALIDATION_ENDPOINT_URL_NOT_VALID_OR_FAILED);
        }

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * validationMethod: Chosen at random
     * validationEndpointUrl: Points to a valid URL other than a usable introspect or userinfo endpoint
     * Expected results:
     * -
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void ValidationMethod2ServerTests_validationEndpointUrl_invalidEndpoint() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_validEndptUrl_invalidEndpoint");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying the token could not be validated by the endpoint.", MessageConstants.CWWKS1725E_VALIDATION_ENDPOINT_URL_NOT_VALID_OR_FAILED);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * validationEndpointUrl: Not specified
     * inboundPropagation: required
     * Access token not provided in protected resource request
     * Expected results:
     * - Request returns 401
     * - CWWKS1726E message in RS logs saying the server failed the request because the access token was missing
     *
     * @throws Exception
     */
    @Test
    public void ValidationMethod2ServerTests_validationEndpointUrl_notSpecified_inboundPropRequired_noAccessToken() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_validEndptUrl_notSpecified_inboundPropRequired");
        updatedTestSettings.setWhere(null);

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        // iss 3710 // validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying the request failed because of a missing access token.", MessageConstants.CWWKS1726E_MISSING_PROPAGATION_TOKEN);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * validationEndpointUrl: Not specified
     * inboundPropagation: supported
     * Include access token in protected resource request
     * Expected results:
     * - Should not gain access to the protected resource
     * - Should fall back to pure OAuth flow
     * - Should not find "error" parameter
     * - Received a response from authorization endpoint, not an invalid redirect URI
     * - Found CWOAU0062E message in the response
     * - OP logs: CWOAU0056E message saying redirect URI was not registered
     * - RS logs: CWWKS1725E message saying the validation endpoint was invalid
     *
     * @throws Exception
     */
    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException")
    @Test
    public void ValidationMethod2ServerTests_validationEndpointUrl_notSpecified_inboundPropSupported() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_validEndptUrl_notSpecified_inboundPropSupported");
        updatedTestSettings.setInboundProp(Constants.SUPPORTED);

        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Got to the protected servlet when we should not have.", null, Constants.HELLOWORLD_MSG);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found unexpected \"" + Constants.ERROR_RESPONSE_PARM + "\" in response.", null, "\"" + Constants.ERROR_RESPONSE_PARM + "\"");
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Should have received a response from the authorization endpoint and not been redirected to the invalid redirect URL.", null, "/" + genericTestServer.getProviderApp() + "/authorize");
        expectations = vData.addResponseExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, "Did not find CWOAU0062E message saying the redirect URI was invalid.", MessageConstants.CWOAU0062E_REDIRECT_URI_INVALID);
        // 212457
        // expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying validationEndpointUrl was not valid.", MessageConstants.CWWKS1725E_VALIDATION_ENDPOINT_URL_NOT_VALID_OR_FAILED);
        if (!alreadyThrewFFDC) {
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find CWOAU0056E message in the OP logs saying redirect URI was not among those registered with the OP.", MessageConstants.CWOAU0056E_REDIRECT_URI_NOT_REGISTERED);
            alreadyThrewFFDC = true;
        }

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * validationEndpointUrl: Not specified
     * inboundPropagation: supported
     * Access token not provided in protected resource request
     * Expected results:
     * - Should not gain access to the protected resource
     * - Should fall back to pure OAuth flow
     * - Should not find "error" parameter
     * - Received a response from authorization endpoint, not an invalid redirect URI
     * - Found CWOAU0062E message in the response
     * - OP logs: CWOAU0056E message saying redirect URI was not registered
     * - RP logs: CWWKS1726E message saying the server failed the request because the access token was missing
     *
     * @throws Exception
     */
    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException")
    @Test
    public void ValidationMethod2ServerTests_validationEndpointUrl_notSpecified_inboundPropSupported_noAccessToken() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_validEndptUrl_notSpecified_inboundPropSupported");
        updatedTestSettings.setWhere(null);
        updatedTestSettings.setInboundProp(Constants.SUPPORTED);

        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Got to the protected servlet when we should not have.", null, Constants.HELLOWORLD_MSG);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found unexpected \"" + Constants.ERROR_RESPONSE_PARM + "\" in response.", null, "\"" + Constants.ERROR_RESPONSE_PARM + "\"");
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Should have received a response from the authorization endpoint and not been redirected to the invalid redirect URL.", null, "/" + genericTestServer.getProviderApp() + "/authorize");
        expectations = vData.addResponseExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, "Did not find CWOAU0062E message saying the redirect URI was invalid.", MessageConstants.CWOAU0062E_REDIRECT_URI_INVALID);
        // 212457
        // validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying the request failed because of a missing access token.", MessageConstants.CWWKS1726E_MISSING_ACCESS_TOKEN);
        if (!alreadyThrewFFDC) {
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find CWOAU0056E message in the OP logs saying redirect URI was not among those registered with the OP.", MessageConstants.CWOAU0056E_REDIRECT_URI_NOT_REGISTERED);
            alreadyThrewFFDC = true;
        }

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

}
