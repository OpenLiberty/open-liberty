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

import javax.servlet.http.HttpServletResponse;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.AuthorizationHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonCookieTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.JaxRsCommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MultiProviderUtils;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSSkipIfAccessToken;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSSkipIfJWTToken;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.openidconnect.server.fat.jaxrs.config.Utils.CommonTools;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

public class ExistingAttributes2ServerTests extends JaxRsCommonTest {

    private static final Class<?> thisClass = ExistingAttributes2ServerTests.class;
    protected static AuthorizationHelpers authHelpers = new AuthorizationHelpers();
    protected static CommonCookieTools cookieTools = new CommonCookieTools();
    public static CommonValidationTools validationTools = new CommonValidationTools();
    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    protected static MultiProviderUtils mpUtils = new MultiProviderUtils();
    protected static String targetProvider = null;
    protected static String targetISSEndpoint = null;
    protected static String targetISSHttpsEndpoint = null;
    protected static String[] goodActions = null;
    protected static String flowType = Constants.WEB_CLIENT_FLOW;
    protected static String OPServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.OPserver";
    protected static String RSServerName = "com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs.config.RSserver";
    protected static final String oldAccessToken = "eNVMlACDRk7RKEi8AYp45Y2uogVACpERnHZfYDq6";
    protected static CommonTools commonTools = new CommonTools();
    protected static String defaultISSAccessId = "user:http://" + targetISSEndpoint + "/" + Constants.OIDC_USERNAME;
    protected static String defaultISSAccessIdWithHttps = null;
    protected static String accessIdWithHttpsProvider = null;

    protected static String http_realm = "";
    protected static String https_realm = "";

    private boolean alreadyThrewFFDC = false;
    private final String HTTPS_REQUIRED_CLIENT = "httpsRequiredClient";

    @Rule
    public static final TestRule conditIgnoreRule = new ConditionalIgnoreRule();

    @BeforeClass
    public static void beforeClass() {
        useLdap = false;
        Log.info(thisClass, "beforeClass", "Set useLdap to: " + useLdap);
    }

    @AfterClass
    public static void afterClass() {
        Log.info(thisClass, "afterClass", "Resetting useLdap to: " + defaultUseLdap);
        useLdap = defaultUseLdap;
    }

    @Before
    public void beforeTest() {
        createCommonPropagationToken();
    }

    /***************************************************** Tests *****************************************************/
    /**
     *
     *
     * N N OOOO TTTTT EEEE
     * NN N O O T E ** The clientID in the token (access_token) will be valid (introspect endpoint) as long as it was
     * N N N O O T EEEE issued by ANY of the providers configured in a server. It does NOT have to be issued by the
     * N NN O O T E ** provider that issued the token
     * N N OOOO T EEEE If/When that is ever "fixed", many of the following tests will fail...
     *
     *
     */

    /**
     *
     *
     * N N OOOO TTTTT EEEE
     * NN N O O T E ** Tests for most of the existing attributes are contained in this class
     * N N N O O T EEEE Tests for attributes added/supported with the addition of a JWT Token will be in
     * N NN O O T E ** in attribute specific test classes in this same project (we found that having multiple
     * N N OOOO T EEEE classes with multiple (yet manageable) configs works better than one class with
     * reconfigs for each test
     * Tests for disableIssChecking are in the NoOPMangleJWT1ServerTests class of the
     * com.ibm.ws.security.openidconnect.server-1.0_fat.jaxrs project because it was easier/better
     * to test with a test created JWT instead of trying to have the OP generate what we needed
     *
     */
    /**
     * Config contains only required attributes, plus attributes to make sure we get to the right client instance (e.g.
     * authFilterRef)
     * Only runs when using an access token as the propagation token
     * Expected results:
     * - Userinfo validation method:
     * - validationMethod="introspect" by default which won't match the validationEndpointUrl
     * - Did not reach protected resource
     * - OP log: CWWKS1633E message saying the userinfo request contained an unsupported "token" parameter
     * - RS log: CWWKS1721E message saying the token wasn't recognized
     * - Token introspection validation method:
     * - Should successfully reach the protected resource
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    @Test
    public void ExistingAttributes2ServerTests_minimumConfig() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_minimumConfig");

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings);

        if (genericTestServer.getRSValidationType().equals(Constants.USERINFO_ENDPOINT)) {
            // The default validation method is "introspect" so the userinfo endpoint as the validation endpoint will result in errors
            expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
            expectations = rsTools.setExpectationForAccessTokenOnly(testOPServer, expectations, updatedTestSettings, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in OP logs saying the userinfo request contained an unsupported \"token\" parameter.", MessageConstants.CWWKS1633E_USERINFO_UNSUPPORTED_PARAM);
            expectations = rsTools.setExpectationForAccessTokenOnly(genericTestServer, expectations, updatedTestSettings, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying the token couldn't be recognized.", MessageConstants.CWWKS1721E_OIDC_REQ_FAILED_ACCESS_TOKEN_NOT_VALID + ".+" + MessageConstants.CWWKS1633E_USERINFO_UNSUPPORTED_PARAM);
        }

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * Config contains only required attributes, plus attributes to make sure we get to the right client instance (e.g.
     * authFilterRef)
     * Only runs when using a JWT as the propagation token
     * Expected results:
     * - JWT using JWK validation:
     * - Not enough information in the config for a successful RS invocation
     * - 401 response with CWWKS1777E and CWWKS1737E messages in the RS logs for failures to validate the JWT
     * - All other scenarios should successfully reach the protected resource
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfAccessToken.class)
    @Test
    public void ExistingAttributes2ServerTests_minimumConfig_jwt() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_minimumConfig_jwt");

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings);

        if (Constants.JWK_CERT.equals(updatedTestSettings.getRsCertType())) {
            // If using JWK, more information is needed in the config than is needed when using x509 certificates for signing tokens, so this invocation will fail
            expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find message indicating signature algorithm mismatch.", MessageConstants.CWWKS1777E_SIG_ALG_MISMATCH);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find message indicating JWT validation failure.", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE);
        }

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * Config contains only required attributes, plus attributes to make sure we get to the right client instance (e.g.
     * authFilterRef)
     * Only runs when using a JWT as the propagation token
     * Runs the JWT + JWK combination
     * - More information is required in the config than the JWT + x509 combination (signature algorithm and JWK endpoint are
     * needed here)
     * Expected results:
     * - Should successfully reach the protected resource
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfAccessToken.class)
    @Test
    public void ExistingAttributes2ServerTests_minimumConfig_jwt_jwk() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_minimumConfig_jwt_jwk");
        updatedTestSettings.setRsCertType(Constants.JWK_CERT);

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /******************************************* clientId and client tests *******************************************/

    /**
     * clientId=""
     * Expected results:
     * - Userinfo/JWT validation method:
     * - Userinfo doesn't require client credentials, only the access token itself
     * - JWT isn't configured to return the clientId
     * - Should successfully reach the protected resource
     * - Token introspection validation method:
     * - Did not reach protected resource
     * - OP log: CWOAU0033E message saying a required parameter (client_id) was missing
     * - RS log: CWWKS1721E message saying an error occurred validating the access token
     *
     * @throws Exception
     */
    @Test
    public void ExistingAttributes2ServerTests_clientId_empty() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_clientId_empty");

        List<validationData> expectations = null;

        if (genericTestServer.getRSValidationType().equals(Constants.INTROSPECTION_ENDPOINT)) {
            expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
            expectations = rsTools.setExpectationForAccessTokenOnly(testOPServer, expectations, updatedTestSettings, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in OP logs saying a required parameter was missing.", MessageConstants.CWOAU0033E_REQ_RUNTIME_PARAM_MISSING + ".+" + "client_id");
            expectations = rsTools.setExpectationForAccessTokenOnly(genericTestServer, expectations, updatedTestSettings, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying client credentials were not valid.", MessageConstants.CWWKS1721E_OIDC_REQ_FAILED_ACCESS_TOKEN_NOT_VALID + ".+" + MessageConstants.CWWKS1406E_INTROSPECT_INVALID_CREDENTIAL);
        } else {
            // Userinfo doesn't require client authentication, jwt is not configured to return the clientId
            expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, http_realm);
        }

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * The client configured to protect the resource is disabled in the OP
     * inboundPropagation="required"
     * Expected results:
     * - Userinfo/JWT validation method:
     * - Should successfully reach the protected resource (userinfo: client authentication is not needed, JWT: we don't go to the
     * OP to verify the client)
     * - Token introspection validation method:
     * - Client authentication should fail for disabled client
     * - OP logs: CWOAU0038E message saying the client could not be verified
     * - RS log: CWWKS1723E message saying an invalid_client error occurred
     *
     * @throws Exception
     */
    @Test
    public void ExistingAttributes2ServerTests_clientDisabled_inboundPropRequired() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_clientDisabled_inboundPropRequired");

        List<validationData> expectations = vData.addSuccessStatusCodes();

        if (genericTestServer.getRSValidationType().equals(Constants.INTROSPECTION_ENDPOINT)) {
            expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
            expectations = rsTools.setExpectationForAccessTokenOnly(testOPServer, expectations, updatedTestSettings, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in OP logs saying the client could not be verified.", MessageConstants.CWOAU0038E_CLIENT_COULD_NOT_BE_VERIFIED);
            expectations = rsTools.setExpectationForAccessTokenOnly(genericTestServer, expectations, updatedTestSettings, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying an invalid_client error occurred.", MessageConstants.CWWKS1723E_INVALID_CLIENT_ERROR);
        } else {
            // Userinfo doesn't require client authentication, JWT won't know the client is disabled
            expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, http_realm);
        }

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * The client configured to protect the resource is disabled in the OP
     * inboundPropagation="supported"
     * Should fail to reach the protected resource and fall back to pure OAuth to access the resource
     * Expected results:
     * - Userinfo/JWT validation method:
     * - Should successfully reach the protected resource (userinfo: client authentication is not needed, JWT: we don't go to the
     * OP to verify the client)
     * - Token introspection validation method:
     * - Client authentication should fail for disabled client
     * - OP logs: CWOAU0038E message saying the client could not be verified, FFDC with CWOAU0023E message saying the client could
     * not be found
     * - RS log: CWWKS1740W message saying the inbound propagation request failed, so we're falling back to the regular OIDC
     * client behavior
     *
     * @throws Exception
     */
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidClientException" })
    @Test
    public void ExistingAttributes2ServerTests_clientDisabled_inboundPropSupported() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_clientDisabled_inboundPropSupported");
        updatedTestSettings.setInboundProp(Constants.SUPPORTED);

        List<validationData> expectations = vData.addSuccessStatusCodes();

        if (genericTestServer.getRSValidationType().equals(Constants.INTROSPECTION_ENDPOINT)) {
            expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Got to the protected servlet when we should not have.", null, Constants.HELLOWORLD_MSG);
            expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found unexpected \"" + Constants.ERROR_RESPONSE_PARM + "\" in response.", null, "\"" + Constants.ERROR_RESPONSE_PARM + "\"");
            expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Should have received a response from the authorization endpoint and not been redirected to the invalid redirect URL.", null, "/" + genericTestServer.getProviderApp() + "/authorize");
            expectations = vData.addResponseExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, "Did not find message saying the client could not be found.", MessageConstants.CWOAU0061E_COULD_NOT_FIND_CLIENT);
            expectations = rsTools.setExpectationForAccessTokenOnly(testOPServer, expectations, updatedTestSettings, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in OP logs saying the client could not be verified.", MessageConstants.CWOAU0038E_CLIENT_COULD_NOT_BE_VERIFIED);
            expectations = rsTools.setExpectationForAccessTokenOnly(testOPServer, expectations, updatedTestSettings, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in OP logs saying the client could not be found.", MessageConstants.CWOAU0023E_CLIENT_NOT_FOUND);
            expectations = rsTools.setExpectationForAccessTokenOnly(genericTestServer, expectations, updatedTestSettings, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying the inbound propagation request failed so the server's redirecting to the normal OIDC client.", MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP);

        } else {
            // Userinfo doesn't require client authentication, JWT won't know the client is disabled
            expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, http_realm);
        }

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * clientId="nonRegisteredClient"
     * Expected results:
     * - Userinfo/JWT validation method:
     * - Userinfo doesn't require client credentials, only the access token itself
     * - JWT won't go to the OP to verify the client
     * - Should successfully reach the protected resource
     * - Token introspection validation method:
     * - Did not reach protected resource
     * - OP log: CWOAU0038E message saying the client could not be verified
     * - RS log: CWWKS1723E message saying an invalid_client error occurred
     *
     * @throws Exception
     */
    @Test
    public void ExistingAttributes2ServerTests_clientId_notRegistered() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_clientId_notRegistered");

        List<validationData> expectations = null;

        if (genericTestServer.getRSValidationType().equals(Constants.INTROSPECTION_ENDPOINT)) {
            expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
            expectations = rsTools.setExpectationForAccessTokenOnly(testOPServer, expectations, updatedTestSettings, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in OP logs saying the client could not be verified.", MessageConstants.CWOAU0038E_CLIENT_COULD_NOT_BE_VERIFIED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying an invalid_client error occurred.", MessageConstants.CWWKS1723E_INVALID_CLIENT_ERROR);
        } else {
            // Userinfo doesn't require client authentication, JWT won't go to the OP to validate the clientId
            expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, http_realm);
        }

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * clientId="client03" (registered with OP but not the client associated with the token being sent)
     * Expected results:
     * - Userinfo/JWT validation method:
     * - Userinfo doesn't require client credentials, only the access token itself
     * - JWT not configured to return client_id
     * - Should successfully reach the protected resource
     * - Token introspection validation method:
     * - Client credentials map to a valid client registered with the OP
     * - Authenticated and authorized clients may perform token introspection
     * - Should successfully reach the protected resource
     *
     * @throws Exception
     */
    @Test
    public void ExistingAttributes2ServerTests_clientId_registered_doesNotMatchToken() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_clientId_registered_doesNotMatchToken");
        String useThisRealm = http_realm;
        if (genericTestServer.getRSValidationType().equals(Constants.INTROSPECTION_ENDPOINT)) {
            useThisRealm = Constants.BASIC_REALM;
        }

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, useThisRealm);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * clientId = A client registered with a different OP than the one used to obtain the access token
     * Expected results:
     * Opaque access token flows:
     * - Should get 401 attempting to access the protected resource
     * - OP logs CWWKS1454E message saying the access token wasn't valid because the client being used isn't a client in the OP
     * - RP logs CWWKS1720E message saying the access token isn't active.
     * JWT access token flows:
     * - Should successfully reach the protected resource
     * - JWT validation doesn't verify the "azp" claim yet, so the request will succeed
     */
    @AllowedFFDC(Constants.ID_TOKEN_VALIDATION_FFDC)
    @Mode(TestMode.LITE)
    @Test
    public void ExistingAttributes2ServerTests_clientRegisteredWithOtherProvider() throws Exception {
        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_clientInOtherProvider");

        List<validationData> expectations = null;
        if (Constants.JWT_TOKEN.equals(updatedTestSettings.getRsTokenType())) {
            // JWT validation is not implemented to verify the "azp" claim yet, so this flow will succeed. See Jose4jValidator.parseJwtWithValidation()
            String useThisRealm = http_realm.replace(targetProvider, "Other" + targetProvider);
            if (genericTestServer.getRSValidationType().equals(Constants.INTROSPECTION_ENDPOINT)) {
                useThisRealm = Constants.BASIC_REALM;
            }
            expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, useThisRealm);
        } else {
            // Opaque access tokens must be in the provider's cache. Since a different provider was used to create the propagation token,
            // this flow will fail.
            expectations = set401ExpectationsForUsingClientInOtherProvider();
        }

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    private List<validationData> set401ExpectationsForUsingClientInOtherProvider() throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes(null, Constants.INVOKE_RS_PROTECTED_RESOURCE);
        vData.addResponseStatusExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, HttpServletResponse.SC_UNAUTHORIZED);
        validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find CWWKS1720E message in RS log saying the access token is not active.", MessageConstants.CWWKS1720E_ACCESS_TOKEN_NOT_ACTIVE);
        if (genericTestServer.getRSValidationType().equals(Constants.INTROSPECTION_ENDPOINT)) {
            validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find CWWKS1454E message in OP log saying the access token was not valid or expired.", MessageConstants.CWWKS1454E_ACCESS_TOKEN_NOT_VALID);
        } else if (genericTestServer.getRSValidationType().equals(Constants.USERINFO_ENDPOINT)) {
            validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find CWWKS1617E message in OP log saying the access token was not recognized.", MessageConstants.CWWKS1617E_USERINFO_REQUEST_BAD_TOKEN);
            validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find CWWKS1721E message in RS log saying an error occurred.", MessageConstants.CWWKS1721E_OIDC_REQ_FAILED_ACCESS_TOKEN_NOT_VALID);
        }
        return expectations;
    }

    /********************************************** clientSecret tests **********************************************/

    /**
     * clientSecret=not specified in the config
     * Expected results:
     * - Userinfo/JWT validation method:
     * - Userinfo doesn't require client credentials, only the access token itself
     * - JWT won't use clientSecret
     * - Should successfully reach the protected resource
     * - Token introspection validation method:
     * - Did not reach protected resource
     * - OP log: CWOAU0038E message saying the client could not be verified
     * - RS log: CWWKS1723E message saying an invalid_client error occurred
     *
     * @throws Exception
     */
    @Test
    public void ExistingAttributes2ServerTests_clientSecret_notSpecified() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_clientSecret_notSpecified");

        List<validationData> expectations = vData.addSuccessStatusCodes();
        if (genericTestServer.getRSValidationType().equals(Constants.INTROSPECTION_ENDPOINT)) {
            expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in OP logs saying the client could not be verified.", MessageConstants.CWOAU0038E_CLIENT_COULD_NOT_BE_VERIFIED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying an invalid_client error occurred.", MessageConstants.CWWKS1723E_INVALID_CLIENT_ERROR);
        } else {
            // Userinfo doesn't require client authentication
            expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, http_realm);
        }

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * clientSecret=""
     * Expected results:
     * - Userinfo/JWT validation method:
     * - Userinfo doesn't require client credentials, only the access token itself
     * - JWT won't use clientSecret
     * - Should successfully reach the protected resource
     * - Token introspection validation method:
     * - Did not reach protected resource
     * - OP log: CWOAU0038E message saying the client could not be verified
     * - RS log: CWWKS1723E message saying an invalid_client error occurred
     *
     * @throws Exception
     */
    @Test
    public void ExistingAttributes2ServerTests_clientSecret_empty() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_clientSecret_empty");

        List<validationData> expectations = vData.addSuccessStatusCodes();

        if (genericTestServer.getRSValidationType().equals(Constants.INTROSPECTION_ENDPOINT)) {
            expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
            expectations = rsTools.setExpectationForAccessTokenOnly(testOPServer, expectations, updatedTestSettings, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in OP logs saying the client could not be verified.", MessageConstants.CWOAU0038E_CLIENT_COULD_NOT_BE_VERIFIED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying an invalid_client error occurred.", MessageConstants.CWWKS1723E_INVALID_CLIENT_ERROR);
        } else {
            // Userinfo doesn't require client authentication
            expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, http_realm);
        }

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * clientSecret="invalid_secret!@#$%^&*()`~ -_=+[{]};:'\",<.>/?"
     * Expected results:
     * - Userinfo/JWT validation method:
     * - Userinfo doesn't require client credentials, only the access token itself
     * - JWT won't use clientSecret
     * - Should successfully reach the protected resource
     * - Token introspection validation method:
     * - Did not reach protected resource
     * - OP log: CWOAU0038E message saying the client could not be verified
     * - RS log: CWWKS1723E message saying an invalid_client error occurred
     *
     * @throws Exception
     */
    @Test
    public void ExistingAttributes2ServerTests_clientSecret_invalid() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_clientSecret_invalid");

        List<validationData> expectations = vData.addSuccessStatusCodes();

        if (genericTestServer.getRSValidationType().equals(Constants.INTROSPECTION_ENDPOINT)) {
            expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
            expectations = rsTools.setExpectationForAccessTokenOnly(testOPServer, expectations, updatedTestSettings, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in OP logs saying the client could not be verified.", MessageConstants.CWOAU0038E_CLIENT_COULD_NOT_BE_VERIFIED);
            expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs saying an invalid_client error occurred.", MessageConstants.CWWKS1723E_INVALID_CLIENT_ERROR);
        } else {
            // Userinfo doesn't require client authentication
            expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, http_realm);
        }

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /************************************************* scopes tests *************************************************/

    /**
     * scope="openid myscope"
     * The client protecting the resource configures scopes that are not a subset of the scopes supported by the client used to
     * obtain the access token
     * Expected results:
     * TODO - Per section 3.1 of RFC 6750, a request that "requires higher privileges than provided by the access token" should
     * result in a 403 response
     * and insufficient_scope error
     * - Userinfo validation method:
     * -
     * - Token introspection validation method:
     * -
     *
     * @throws Exception
     */
    @Test
    public void ExistingAttributes2ServerTests_insufficientScopes() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_insufficientScopes");

        String useThisRealm = http_realm;
        if (genericTestServer.getRSValidationType().equals(Constants.INTROSPECTION_ENDPOINT)) {
            useThisRealm = Constants.BASIC_REALM;
        }
        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, useThisRealm);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /********************************************** httpsRequired tests **********************************************/

    /**
     * RS: httpsRequired="true"
     * OP: httpsRequired="true"
     * validationEndpointUrl uses HTTP, not HTTPS
     * Expected results:
     * - inboundPropagation="required":
     * - 401 response
     * - CWWKS1703E message in RS logs saying HTTPS is required
     * - inboundPropagation="supported":
     * - Should fall back to pure OAuth and end up on the OAuth authorization page
     * - CWWKS1740W message in RS log saying the token wasn't valid and the request is falling back to OpenID Connect
     * - CWOAU0062E message in the response saying the redirect URI was not valid
     * - CWOAU0056E message in the OP logs saying the redirect URI did not match one registered with the OP
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException" })
    @Test
    public void ExistingAttributes2ServerTests_RSHttpsRequiredTrue_OPHttpsRequiredTrue_httpValidationEndpointUrl() throws Exception {

        // Update test settings to use the HTTPS required client and provider
        TestSettings updatedTestSettings = getHttpsRequiredClientSettings();
        String testToken = getNewToken(updatedTestSettings);

        // inboundPropagation="required"
        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredTrue_OPHttpsRequiredTrue_httpValidationEndpointUrl_inboundPropRequired");
        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not get message in RS logs about HTTPS being required.", MessageConstants.CWWKS1703E_CLIENT_REQUIRES_HTTPS);
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), testToken, updatedTestSettings, expectations);

        // inboundPropagation="supported"
        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredTrue_OPHttpsRequiredTrue_httpValidationEndpointUrl_inboundPropSupported");
        expectations = getFallBackToOAuthExpectations(updatedTestSettings.getProvider());
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not find message saying the inbound propagation request failed so the server's redirecting to the normal OIDC client.", MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP);
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), testToken, updatedTestSettings, expectations);
    }

    /**
     * RS: httpsRequired="true"
     * OP: httpsRequired="true"
     * jwkEndpointUrl uses HTTP, not HTTPS
     * Expected results:
     * - inboundPropagation="required":
     * - 401 response
     * - CWOAU0037E message in OP logs saying HTTPS is required
     * - inboundPropagation="supported":
     * - Should fall back to pure OAuth and end up on the OAuth authorization page
     * - CWWKS1740W message in RS log saying the token wasn't valid and the request is falling back to OpenID Connect
     * - CWOAU0062E message in the response saying the redirect URI was not valid
     * - CWOAU0056E message in the OP logs saying the redirect URI did not match one registered with the OP
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfAccessToken.class)
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException" })
    @Test
    public void ExistingAttributes2ServerTests_RSHttpsRequiredTrue_OPHttpsRequiredTrue_httpJwkEndpointUrl() throws Exception {

        // Update test settings to use the HTTPS required client and provider
        TestSettings updatedTestSettings = getHttpsRequiredJwkClientSettings();
        String tokenWithJwk = getNewToken(updatedTestSettings);

        // Invoke the protected resource with inboundPropagation="required"
        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredTrue_OPHttpsRequiredTrue_httpJwkEndpointUrl_inboundPropRequired");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not get message in OP logs saying HTTPS is required.", MessageConstants.CWOAU0037E_HTTPS_REQUIRED);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not get message in RS logs saying JWK was not returned from endpoint due to HTTPS being required.", MessageConstants.CWWKS6049E_JWK_NOT_RETURNED + ".+" + MessageConstants.CWOAU0073E_FRONT_END_ERROR);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not get message in RS logs saying signing key for JWK was not found.", MessageConstants.CWWKS1739E_JWT_KEY_NOT_FOUND);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not get message in RS logs saying JWT validation failed.", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), tokenWithJwk, updatedTestSettings, expectations);

        // Perform same invocation with inboundPropagation="supported"
        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredTrue_OPHttpsRequiredTrue_httpJwkEndpointUrl_inboundPropSupported");

        expectations = getFallBackToOAuthExpectations("HttpsRequiredJwk" + genericTestServer.getProviderApp());
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find message in OP saying HTTPS is required.", MessageConstants.CWOAU0037E_HTTPS_REQUIRED);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not get message in RS logs saying JWK was not returned from endpoint due to HTTPS being required.", MessageConstants.CWWKS6049E_JWK_NOT_RETURNED + ".+" + MessageConstants.CWOAU0073E_FRONT_END_ERROR);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not get message in RS logs saying JWT validation failed.", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "Did not find message saying request was being redirected to authenticate with OpenID Connect.", MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), tokenWithJwk, updatedTestSettings, expectations);
    }

    /**
     * RS: httpsRequired="true"
     * OP: httpsRequired="true"
     * validationEndpointUrl uses HTTPS
     * Expected results:
     * - Successfully reached the protected resource
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    @Test
    public void ExistingAttributes2ServerTests_RSHttpsRequiredTrue_OPHttpsRequiredTrue_httpsValidationEndpointUrl() throws Exception {

        // Update test settings to use the HTTPS required client and provider
        TestSettings updatedTestSettings = getHttpsRequiredClientSettings();
        String testToken = getNewToken(updatedTestSettings);

        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredTrue_OPHttpsRequiredTrue_httpsValidationEndpointUrl_inboundPropRequired");
        successfulProtectedResourceInvocation(updatedTestSettings, https_realm.replace(targetProvider, updatedTestSettings.getProvider()), testToken);

        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredTrue_OPHttpsRequiredTrue_httpsValidationEndpointUrl_inboundPropSupported");
        successfulProtectedResourceInvocation(updatedTestSettings, https_realm.replace(targetProvider, updatedTestSettings.getProvider()), testToken);
    }

    /**
     * RS: httpsRequired="true"
     * OP: httpsRequired="true"
     * jwkEndpointUrl uses HTTPS
     * Expected results:
     * - Successfully reached the protected resource
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfAccessToken.class)
    @Test
    public void ExistingAttributes2ServerTests_RSHttpsRequiredTrue_OPHttpsRequiredTrue_httpsJwkEndpointUrl() throws Exception {

        // Update test settings to use the HTTPS required client and provider
        TestSettings updatedTestSettings = getHttpsRequiredJwkClientSettings();
        String tokenWithJwk = getNewToken(updatedTestSettings);

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings);

        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredTrue_OPHttpsRequiredTrue_httpsJwkEndpointUrl_inboundPropRequired");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), tokenWithJwk, updatedTestSettings, expectations);

        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredTrue_OPHttpsRequiredTrue_httpsJwkEndpointUrl_inboundPropSupported");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), tokenWithJwk, updatedTestSettings, expectations);
    }

    /**
     * RS: httpsRequired="true"
     * OP: httpsRequired="false"
     * validationEndpointUrl uses HTTP, not HTTPS
     * Expected results:
     * - inboundPropagation="required":
     * - 401 response
     * - CWWKS1703E message in RS logs saying HTTPS is required
     * - inboundPropagation="supported":
     * - Should fall back to pure OAuth and end up on the OAuth authorization page
     * - CWOAU0062E message in the response saying the redirect URI was not valid
     * - CWOAU0056E message in the OP logs saying the redirect URI did not match one registered with the OP
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException")
    @Test
    public void ExistingAttributes2ServerTests_RSHttpsRequiredTrue_OPHttpsRequiredFalse_httpValidationEndpointUrl() throws Exception {

        // inboundPropagation="required"
        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_RSHttpsRequiredTrue_OPHttpsRequiredFalse_httpValidationEndpointUrl_inboundPropRequired");
        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not get message in RS logs about HTTPS being required.", MessageConstants.CWWKS1703E_CLIENT_REQUIRES_HTTPS);
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);

        // inboundPropagation="supported"
        updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_RSHttpsRequiredTrue_OPHttpsRequiredFalse_httpValidationEndpointUrl_inboundPropSupported");
        expectations = getFallBackToOAuthExpectations(genericTestServer.getProviderApp());
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not find message saying the inbound propagation request failed so the server's redirecting to the normal OIDC client.", MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP);
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), commonPropagationToken, updatedTestSettings, expectations);
    }

    /**
     * RS: httpsRequired="true"
     * OP: httpsRequired="false"
     * jwkEndpointUrl uses HTTP
     * Expected results:
     * - Successfully reached the protected resource
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfAccessToken.class)
    @Test
    public void ExistingAttributes2ServerTests_RSHttpsRequiredTrue_OPHttpsRequiredFalse_httpJwkEndpointUrl() throws Exception {

        // Update test settings to use the provider configured to use JWK
        TestSettings updatedTestSettings = getJwkClientSettings();
        String tokenWithJwk = getNewToken(updatedTestSettings);

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings);

        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredTrue_OPHttpsRequiredFalse_httpJwkEndpointUrl_inboundPropRequired");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), tokenWithJwk, updatedTestSettings, expectations);

        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredTrue_OPHttpsRequiredFalse_httpJwkEndpointUrl_inboundPropSupported");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), tokenWithJwk, updatedTestSettings, expectations);
    }

    /**
     * RS: httpsRequired="true"
     * OP: httpsRequired="false"
     * validationEndpointUrl uses HTTPS
     * Expected results:
     * - Successfully reached the protected resource
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    @Test
    public void ExistingAttributes2ServerTests_RSHttpsRequiredTrue_OPHttpsRequiredFalse_httpsValidationEndpointUrl() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_RSHttpsRequiredTrue_OPHttpsRequiredFalse_httpsValidationEndpointUrl_inboundPropRequired");
        successfulProtectedResourceInvocation(updatedTestSettings, https_realm);

        updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_RSHttpsRequiredTrue_OPHttpsRequiredFalse_httpsValidationEndpointUrl_inboundPropSupported");
        successfulProtectedResourceInvocation(updatedTestSettings, https_realm);
    }

    /**
     * RS: httpsRequired="true"
     * OP: httpsRequired="false"
     * jwkEndpointUrl uses HTTPS
     * Expected results:
     * - Successfully reached the protected resource
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfAccessToken.class)
    @Test
    public void ExistingAttributes2ServerTests_RSHttpsRequiredTrue_OPHttpsRequiredFalse_httpsJwkEndpointUrl() throws Exception {

        // Update test settings to use the provider configured to use JWK
        TestSettings updatedTestSettings = getJwkClientSettings();
        String tokenWithJwk = getNewToken(updatedTestSettings);

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings);

        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredTrue_OPHttpsRequiredFalse_httpsJwkEndpointUrl_inboundPropRequired");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), tokenWithJwk, updatedTestSettings, expectations);

        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredTrue_OPHttpsRequiredFalse_httpsJwkEndpointUrl_inboundPropSupported");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), tokenWithJwk, updatedTestSettings, expectations);
    }

    /**
     * RS: httpsRequired="false"
     * OP: httpsRequired="true"
     * validationEndpointUrl uses HTTP, not HTTPS
     * Expected results:
     * - inboundPropagation="required":
     * - 401 response
     * - CWOAU0037E message in OP logs saying HTTPS is required
     * - CWWKS1721E message in RS logs saying HTTPS is required
     * - inboundPropagation="supported":
     * - Should fall back to pure OAuth and end up on the OAuth authorization page
     * - CWOAU0062E message in the response saying the redirect URI was not valid
     * - CWOAU0056E message in the OP logs saying the redirect URI did not match one registered with the OP
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    @AllowedFFDC("com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException")
    @Test
    public void ExistingAttributes2ServerTests_RSHttpsRequiredFalse_OPHttpsRequiredTrue_httpValidationEndpointUrl() throws Exception {

        // Update test settings to use the HTTPS required client and provider
        TestSettings updatedTestSettings = getHttpsRequiredClientSettings();
        String testToken = getNewToken(updatedTestSettings);

        // inboundPropagation="required"
        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredFalse_OPHttpsRequiredTrue_httpValidationEndpointUrl_inboundPropRequired");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not get message in OP logs saying HTTPS is required.", MessageConstants.CWOAU0037E_HTTPS_REQUIRED);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not get message in RS logs saying an error occurred.", MessageConstants.CWWKS1721E_OIDC_REQ_FAILED_ACCESS_TOKEN_NOT_VALID + ".+" + MessageConstants.CWOAU0073E_FRONT_END_ERROR);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), testToken, updatedTestSettings, expectations);

        // inboundPropagation="supported"
        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredFalse_OPHttpsRequiredTrue_httpValidationEndpointUrl_inboundPropSupported");
        expectations = getFallBackToOAuthExpectations("HttpsRequired" + genericTestServer.getProviderApp());

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), testToken, updatedTestSettings, expectations);
    }

    /**
     * RS: httpsRequired="false"
     * OP: httpsRequired="true"
     * jwkEndpointUrl uses HTTP, not HTTPS
     * Expected results:
     * - inboundPropagation="required":
     * - 401 response
     * - CWOAU0037E message in OP logs saying HTTPS is required
     * - ? message in RS logs saying ?
     * - inboundPropagation="supported":
     * - Should fall back to pure OAuth and end up on the OAuth authorization page
     * - CWWKS1740W message in RS log saying the inbound propagation request failed, so we're falling back to the regular OIDC
     * client behavior
     * - CWOAU0062E message in the response saying the redirect URI was not valid
     * - CWOAU0056E message in the OP logs saying the redirect URI did not match one registered with the OP
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfAccessToken.class)
    @AllowedFFDC({ "com.ibm.oauth.core.api.error.oauth20.OAuth20InvalidRedirectUriException" })
    @Test
    public void ExistingAttributes2ServerTests_RSHttpsRequiredFalse_OPHttpsRequiredTrue_httpJwkEndpointUrl() throws Exception {

        // Update test settings to use the HTTPS required + JWK client and provider
        TestSettings updatedTestSettings = getHttpsRequiredJwkClientSettings();
        String testToken = getNewToken(updatedTestSettings);

        // inboundPropagation="required"
        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredFalse_OPHttpsRequiredTrue_httpJwkEndpointUrl_inboundPropRequired");

        List<validationData> expectations = validationTools.add401Responses(Constants.INVOKE_RS_PROTECTED_RESOURCE);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not get message in OP logs saying HTTPS is required.", MessageConstants.CWOAU0037E_HTTPS_REQUIRED);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not get message in RS logs saying JWK was not returned from endpoint due to HTTPS being required.", MessageConstants.CWWKS6049E_JWK_NOT_RETURNED + ".+" + MessageConstants.CWOAU0073E_FRONT_END_ERROR);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not get message in RS logs saying signing key for JWK was not found.", MessageConstants.CWWKS1739E_JWT_KEY_NOT_FOUND);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not get message in RS logs saying JWT validation failed.", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), testToken, updatedTestSettings, expectations);

        // inboundPropagation="supported"
        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredFalse_OPHttpsRequiredTrue_httpJwkEndpointUrl_inboundPropSupported");
        expectations = getFallBackToOAuthExpectations("HttpsRequiredJwk" + genericTestServer.getProviderApp());
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not get message in OP logs saying HTTPS is required.", MessageConstants.CWOAU0037E_HTTPS_REQUIRED);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not get message in RS logs saying JWK was not returned from endpoint due to HTTPS being required.", MessageConstants.CWWKS6049E_JWK_NOT_RETURNED + ".+" + MessageConstants.CWOAU0073E_FRONT_END_ERROR);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not get message in RS logs saying JWT validation failed.", MessageConstants.CWWKS1737E_JWT_VALIDATION_FAILURE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not find message saying the inbound propagation request failed so the server's redirecting to the normal OIDC client.", MessageConstants.CWWKS1740W_RS_REDIRECT_TO_RP);

        helpers.invokeRsProtectedResource(_testName, new WebConversation(), testToken, updatedTestSettings, expectations);
    }

    /**
     * RS: httpsRequired="false"
     * OP: httpsRequired="true"
     * validationEndpointUrl uses HTTPS
     * Expected results:
     * - Successfully reached the protected resource
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    @Test
    public void ExistingAttributes2ServerTests_RSHttpsRequiredFalse_OPHttpsRequiredTrue_httpsValidationEndpointUrl() throws Exception {

        // Update test settings to use the HTTPS required client and provider
        TestSettings updatedTestSettings = getHttpsRequiredClientSettings();
        String testToken = getNewToken(updatedTestSettings);

        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredFalse_OPHttpsRequiredTrue_httpsValidationEndpointUrl_inboundPropRequired");
        successfulProtectedResourceInvocation(updatedTestSettings, https_realm.replace(targetProvider, updatedTestSettings.getProvider()), testToken);

        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredFalse_OPHttpsRequiredTrue_httpsValidationEndpointUrl_inboundPropSupported");
        successfulProtectedResourceInvocation(updatedTestSettings, https_realm.replace(targetProvider, updatedTestSettings.getProvider()), testToken);
    }

    /**
     * RS: httpsRequired="false"
     * OP: httpsRequired="true"
     * jwkEndpointUrl uses HTTPS
     * Expected results:
     * - Successfully reached the protected resource
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfAccessToken.class)
    @Test
    public void ExistingAttributes2ServerTests_RSHttpsRequiredFalse_OPHttpsRequiredTrue_httpsJwkEndpointUrl() throws Exception {

        // Update test settings to use the HTTPS required + JWK client and provider
        TestSettings updatedTestSettings = getHttpsRequiredJwkClientSettings();
        String testToken = getNewToken(updatedTestSettings);

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings);

        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredFalse_OPHttpsRequiredTrue_httpsJwkEndpointUrl_inboundPropRequired");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), testToken, updatedTestSettings, expectations);

        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredFalse_OPHttpsRequiredTrue_httpsJwkEndpointUrl_inboundPropSupported");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), testToken, updatedTestSettings, expectations);
    }

    /**
     * RS: httpsRequired="false"
     * OP: httpsRequired="false"
     * validationEndpointUrl uses HTTP, not HTTPS
     * Expected results:
     * - Successfully reached the protected resource
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    @Test
    public void ExistingAttributes2ServerTests_RSHttpsRequiredFalse_OPHttpsRequiredFalse_httpValidationEndpointUrl() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_RSHttpsRequiredFalse_OPHttpsRequiredFalse_httpValidationEndpointUrl_inboundPropRequired");
        successfulProtectedResourceInvocation(updatedTestSettings, http_realm);

        updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_RSHttpsRequiredFalse_OPHttpsRequiredFalse_httpValidationEndpointUrl_inboundPropSupported");
        successfulProtectedResourceInvocation(updatedTestSettings, http_realm);
    }

    /**
     * RS: httpsRequired="false"
     * OP: httpsRequired="false"
     * jwkEndpointUrl uses HTTP, not HTTPS
     * Expected results:
     * - Successfully reached the protected resource
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfAccessToken.class)
    @Test
    public void ExistingAttributes2ServerTests_RSHttpsRequiredFalse_OPHttpsRequiredFalse_httpJwkEndpointUrl() throws Exception {

        // Update test settings to use the JWK client and provider
        TestSettings updatedTestSettings = getJwkClientSettings();
        String testToken = getNewToken(updatedTestSettings);

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings);

        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredFalse_OPHttpsRequiredFalse_httpJwkEndpointUrl_inboundPropRequired");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), testToken, updatedTestSettings, expectations);

        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredFalse_OPHttpsRequiredFalse_httpJwkEndpointUrl_inboundPropSupported");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), testToken, updatedTestSettings, expectations);
    }

    /**
     * RS: httpsRequired="false"
     * OP: httpsRequired="false"
     * validationEndpointUrl uses HTTPS
     * Expected results:
     * - Successfully reached the protected resource
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfJWTToken.class)
    @Test
    public void ExistingAttributes2ServerTests_RSHttpsRequiredFalse_OPHttpsRequiredFalse_httpsValidationEndpointUrl() throws Exception {

        TestSettings updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_RSHttpsRequiredFalse_OPHttpsRequiredFalse_httpsValidationEndpointUrl_inboundPropRequired");
        successfulProtectedResourceInvocation(updatedTestSettings, https_realm);

        updatedTestSettings = rsTools.updateRSProtectedResource(testSettings, "helloworld_RSHttpsRequiredFalse_OPHttpsRequiredFalse_httpsValidationEndpointUrl_inboundPropSupported");
        successfulProtectedResourceInvocation(updatedTestSettings, https_realm);
    }

    /**
     * RS: httpsRequired="false"
     * OP: httpsRequired="false"
     * jwkEndpointUrl uses HTTPS
     * Expected results:
     * - Successfully reached the protected resource
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = RSSkipIfAccessToken.class)
    @Test
    public void ExistingAttributes2ServerTests_RSHttpsRequiredFalse_OPHttpsRequiredFalse_httpsJwkEndpointUrl() throws Exception {

        // Update test settings to use the JWK client and provider
        TestSettings updatedTestSettings = getJwkClientSettings();
        String testToken = getNewToken(updatedTestSettings);

        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings);

        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredFalse_OPHttpsRequiredFalse_httpsJwkEndpointUrl_inboundPropRequired");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), testToken, updatedTestSettings, expectations);

        updatedTestSettings = rsTools.updateRSProtectedResource(updatedTestSettings, "helloworld_RSHttpsRequiredFalse_OPHttpsRequiredFalse_httpsJwkEndpointUrl_inboundPropSupported");
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), testToken, updatedTestSettings, expectations);
    }

    /**************************************************** Helpers ****************************************************/

    /**
     * Expects 200 status codes, but does not expect to get to the protected resource (helloworld). Expects to end up on
     * the OAuth authorization page with a failure saying the redirect URI was not valid because it doesn't match one
     * registered with the OP.
     *
     * @param providerId
     * @return
     * @throws Exception
     */
    private List<validationData> getFallBackToOAuthExpectations(String providerId) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Got to the protected servlet when we should not have.", null, Constants.HELLOWORLD_MSG);
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_FULL, Constants.STRING_DOES_NOT_CONTAIN, "Found unexpected \"" + Constants.ERROR_RESPONSE_PARM + "\" in response.", null, "\"" + Constants.ERROR_RESPONSE_PARM + "\"");
        expectations = vData.addExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Should have received a response from the " + providerId + " provider's authorization endpoint and not been redirected to the invalid redirect URL.", null, "/" + providerId + "/authorize");
        expectations = vData.addResponseExpectation(expectations, Constants.INVOKE_RS_PROTECTED_RESOURCE, "Did not find CWOAU0062E message saying the redirect URI was invalid.", MessageConstants.CWOAU0062E_REDIRECT_URI_INVALID);
        if (!alreadyThrewFFDC) {
            // Only one unique FFDC will be generated for the first test calling this method, so this FFDC message should only be found in the first test
            expectations = rsTools.setExpectationForAccessTokenOnly(testOPServer, expectations, testSettings, Constants.INVOKE_RS_PROTECTED_RESOURCE, Constants.MESSAGES_LOG, null, "Did not find CWOAU0056E message in the OP logs saying redirect URI was not among those registered with the OP.", MessageConstants.CWOAU0056E_REDIRECT_URI_NOT_REGISTERED);
            alreadyThrewFFDC = true;
        }
        return expectations;
    }

    /**
     * Updates the existing test settings to use the client ID configured by the OP that requires HTTPS, and sets the target
     * provider to the OP
     * that requires HTTPS.
     *
     * @return
     * @throws Exception
     */
    private TestSettings getHttpsRequiredClientSettings() throws Exception {
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setClientID(HTTPS_REQUIRED_CLIENT);
        updatedTestSettings.setProvider("HttpsRequired" + targetProvider);
        updatedTestSettings = mpUtils.copyAndOverrideProviderSettings(updatedTestSettings, targetProvider, updatedTestSettings.getProvider());
        return updatedTestSettings;
    }

    /**
     * Updates the existing test settings to use the client ID configured by the OP that uses JWK, and sets the target provider
     * to that same OP.
     *
     * @return
     * @throws Exception
     */
    private TestSettings getJwkClientSettings() throws Exception {
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings = helpers.fixProviderInUrls(updatedTestSettings, "providers", "endpoint");
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);
        updatedTestSettings.setRsCertType(Constants.JWK_CERT);
        updatedTestSettings.setProvider("Jwk" + targetProvider);
        updatedTestSettings = mpUtils.copyAndOverrideProviderSettings(updatedTestSettings, targetProvider, updatedTestSettings.getProvider());
        return updatedTestSettings;
    }

    /**
     * Updates the existing test settings to use the client ID configured by the OP that requires HTTPS and uses JWK, and sets the
     * target provider
     * to that same OP.
     *
     * @return
     * @throws Exception
     */
    private TestSettings getHttpsRequiredJwkClientSettings() throws Exception {
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings = helpers.fixProviderInUrls(updatedTestSettings, "providers", "endpoint");
        updatedTestSettings.setClientID(HTTPS_REQUIRED_CLIENT);
        updatedTestSettings.setRsTokenType(Constants.JWT_TOKEN);
        updatedTestSettings.setRsCertType(Constants.JWK_CERT);
        updatedTestSettings.setProvider("HttpsRequiredJwk" + targetProvider);
        updatedTestSettings = mpUtils.copyAndOverrideProviderSettings(updatedTestSettings, targetProvider, updatedTestSettings.getProvider());
        return updatedTestSettings;
    }

    private String getNewToken(TestSettings testSettings) throws Exception {
        WebResponse response = genericOP(_testName, new WebConversation(), testSettings, Constants.BASIC_AUTHENTICATION_ACTIONS, vData.addSuccessStatusCodes());
        String testToken = validationTools.getTokenForType(testSettings, response);
        return testToken;
    }

    /**
     * Invokes the RS protected resource (helloworld) and expects to gain access.
     *
     * @param updatedTestSettings
     * @param realm
     * @throws Exception
     */
    private void successfulProtectedResourceInvocation(TestSettings updatedTestSettings, String realm) throws Exception {
        successfulProtectedResourceInvocation(updatedTestSettings, realm, commonPropagationToken);
    }

    /**
     * Invokes the RS protected resource (helloworld) and expects to gain access.
     *
     * @param updatedTestSettings
     * @param realm
     * @param token
     * @throws Exception
     */
    private void successfulProtectedResourceInvocation(TestSettings updatedTestSettings, String realm, String token) throws Exception {
        List<validationData> expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings);
        if (genericTestServer.getRSValidationType().equals(Constants.USERINFO_ENDPOINT)) {
            expectations = commonTools.getValidHelloWorldExpectations(updatedTestSettings, realm);
        }
        helpers.invokeRsProtectedResource(_testName, new WebConversation(), token, updatedTestSettings, expectations);
    }

    public static void setRealmForValidationType(TestSettings settings) throws Exception {

        if (settings.getRsTokenType().equals(Constants.JWT_TOKEN)) {
            http_realm = Constants.BASIC_REALM;
            https_realm = Constants.BASIC_REALM;
        } else {
            http_realm = "http://" + targetISSEndpoint;
            https_realm = "https://" + targetISSHttpsEndpoint;
        }
        defaultISSAccessId = "user:http://" + targetISSEndpoint + "/" + Constants.OIDC_USERNAME;
        defaultISSAccessIdWithHttps = "user:https://" + targetISSHttpsEndpoint + "/" + Constants.OIDC_USERNAME;
        accessIdWithHttpsProvider = "user:https://localhost:" + testOPServer.getHttpDefaultSecurePort().toString() + "/" + Constants.OIDC_ROOT +
                "/endpoint/" + "HttpsRequiredOidcConfigSample" + "/" + Constants.OIDC_USERNAME;

    }
}
