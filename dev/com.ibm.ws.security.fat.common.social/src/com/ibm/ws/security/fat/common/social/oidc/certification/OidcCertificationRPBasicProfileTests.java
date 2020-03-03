/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.fat.common.social.oidc.certification;

import static org.junit.Assert.fail;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue.ValueType;
import javax.servlet.http.HttpServletResponse;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.ibm.websphere.simplicity.config.ConfigElementList;
import com.ibm.websphere.simplicity.config.ServerConfiguration;
import com.ibm.websphere.simplicity.config.Variable;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.Constants.JsonCheckType;
import com.ibm.ws.security.fat.common.Constants.StringCheckType;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.social.MessageConstants;
import com.ibm.ws.security.fat.common.social.apps.formlogin.BaseServlet;
import com.ibm.ws.security.fat.common.social.expectations.UserInfoJsonExpectation;
import com.ibm.ws.security.fat.common.utils.FatStringUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * See: https://openid.net/certification/rp_testing/
 * 
 * This class should encompass all tests required for the minimal certification for the Basic RP profile.
 */
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public abstract class OidcCertificationRPBasicProfileTests extends CommonSecurityFat {

    public static Class<?> thisClass = OidcCertificationRPBasicProfileTests.class;

    public static final String CERTIFICATION_HOST_AND_PORT = "https://rp.certification.openid.net:8080";

    protected static TestActions actions = new TestActions();
    protected static TestValidationUtils validationUtils = new TestValidationUtils();

    /** Required for the certification provider's client registration request. Must have a valid email format. */
    protected final String clientRegistrationContact = "oidc_certification_contact@us.ibm.com";
    protected final String defaultScope = "openid";
    protected final String defaultSignatureAlgorithm = "RS256";

    // NOTE: These values must be set by extending classes in order for the tests to work
    protected static LibertyServer server;
    protected static String protectedUrl = null;
    protected static String certificationBaseUrl = null;
    /** Identifies the RP so the certification host can keep track of server-visible results for us */
    protected static String rpId = null;
    protected static String clientId = null;
    protected static String defaultTokenEndpointAuthMethod = "client_secret_post";

    protected enum UserInfo {
        DISABLED, ENABLED
    };

    @BeforeClass
    public static void commonBeforeClass() {
        verifyCertificationEndpointIsResponding();
    }

    protected static void verifyCertificationEndpointIsResponding() {
        String method = "verifyCertificationEndpointIsResponding";
        String endpoint = CERTIFICATION_HOST_AND_PORT;
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(method, HttpServletResponse.SC_OK));
        try {
            Page response = actions.invokeUrl(method, endpoint);
            validationUtils.validateResult(response, method, expectations);
        } catch (Exception e) {
            fail("Failed to properly access the RP certification endpoint [" + endpoint + "]. No tests will run in this class. The exception was: " + e + ". The underlying cause was: " + e.getCause());
        }
    }

    /**
     * Tests:
     * - Make an authentication request using the Authorization Code Flow
     * Expected Results:
     * - Should successfully make authentication request and access the protected resource
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_responseType_code() throws Exception {
        String conformanceTestName = "rp-response_type-code";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        registerClientAndUpdateSystemProperties(opConfig, clientId);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);

        String assignedUserName = extractAssignedUserNameFromResponse(response);
        Log.info(thisClass, _testName, "Extracted remote user: [" + assignedUserName + "]");

        Expectations expectations = getTestExpectations_rp_response_type_code(conformanceTestName, assignedUserName);

        validationUtils.validateResult(response, expectations);
    }

    protected Expectations getTestExpectations_rp_response_type_code(String conformanceTestName, String assignedUserName) {
        return getSuccessfulConformanceTestExpectations(conformanceTestName, assignedUserName, UserInfo.DISABLED);
    }

    /**
     * Tests:
     * - Request an ID token and verify its "iss" claim
     * - The "iss" claim in the ID token returned from the OP does not match the expected issuer of the token
     * Expected Results:
     * - 401 when accessing the protected resource
     * - Error message should be logged saying the issuer in the ID token does not match the expected issuer value
     */
    @Test
    public void test_idTokenIssuerMismatch() throws Exception {
        String conformanceTestName = "rp-id_token-issuer-mismatch";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        JsonObject rpConfig = registerClientAndUpdateSystemProperties(opConfig, clientId);

        Expectations expectations = getTestExpectations_rp_id_token_issuer_mismatch(rpConfig);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);
    }

    protected Expectations getTestExpectations_rp_id_token_issuer_mismatch(JsonObject rpConfig) {
        String clientId = rpConfig.getString(Constants.RP_KEY_CLIENT_ID);
        Expectations expectations = getUnauthorizedResponseExpectations();
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1751E_OIDC_IDTOKEN_VERIFY_ISSUER_ERR + ".+" + clientId));
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1706E_OIDC_CLIENT_IDTOKEN_VERIFY_ERR + ".+" + clientId));
        return expectations;
    }

    /**
     * Tests:
     * - Request an ID token and verify its "sub" claim
     * - The ID token returned from the OP does not contain a "sub" claim
     * Expected Results:
     * - 401 when accessing the protected resource
     * - Error message should be logged saying the OIDC client failed to validate the ID token because the "sub" claim was missing
     */
    @AllowedFFDC("org.jose4j.jwt.consumer.InvalidJwtException")
    @Test
    public void test_idTokenMissingSub() throws Exception {
        String conformanceTestName = "rp-id_token-sub";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        JsonObject rpConfig = registerClientAndUpdateSystemProperties(opConfig, clientId);

        Expectations expectations = getTestExpectations_rp_id_token_sub(rpConfig);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);
    }

    protected Expectations getTestExpectations_rp_id_token_sub(JsonObject rpConfig) {
        String clientId = rpConfig.getString(Constants.RP_KEY_CLIENT_ID);
        Expectations expectations = getUnauthorizedResponseExpectations();
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1706E_OIDC_CLIENT_IDTOKEN_VERIFY_ERR + ".+" + clientId + ".+" + "No Subject.+claim"));
        return expectations;
    }

    /**
     * Tests:
     * - Request an ID token and verify its "aud" claim
     * - The ID token returned from the OP does not contain an "aud" claim
     * Expected Results:
     * - 401 when accessing the protected resource
     * - Error message should be logged saying ID token validation failed because the "aud" claim doesn't match the client ID
     */
    @Test
    public void test_idTokenInvalidAud() throws Exception {
        String conformanceTestName = "rp-id_token-aud";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        JsonObject rpConfig = registerClientAndUpdateSystemProperties(opConfig, clientId);

        Expectations expectations = getTestExpectations_rp_id_token_aud(rpConfig);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);
    }

    protected Expectations getTestExpectations_rp_id_token_aud(JsonObject rpConfig) {
        String clientId = rpConfig.getString(Constants.RP_KEY_CLIENT_ID);
        Expectations expectations = getUnauthorizedResponseExpectations();
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1706E_OIDC_CLIENT_IDTOKEN_VERIFY_ERR + ".+" + clientId + ".+" + MessageConstants.CWWKS1754E_OIDC_IDTOKEN_VERIFY_AUD_ERR));
        return expectations;
    }

    /**
     * Tests:
     * - Request an ID token and verify its "iat" claim
     * - The ID token returned from the OP does not contain an "iat" claim
     * Expected Results:
     * - 401 when accessing the protected resource
     * - Error message should be logged saying ID token validation failed because the "iat" claim is missing
     */
    @Test
    public void test_idTokenMissingIat() throws Exception {
        String conformanceTestName = "rp-id_token-iat";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        JsonObject rpConfig = registerClientAndUpdateSystemProperties(opConfig, clientId);

        Expectations expectations = getTestExpectations_rp_id_token_iat(rpConfig);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);
    }

    protected Expectations getTestExpectations_rp_id_token_iat(JsonObject rpConfig) {
        String clientId = rpConfig.getString(Constants.RP_KEY_CLIENT_ID);
        Expectations expectations = getUnauthorizedResponseExpectations();
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1775E_OIDC_ID_VERIFY_IAT_ERR + ".+" + clientId));
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1706E_OIDC_CLIENT_IDTOKEN_VERIFY_ERR + ".+" + clientId));
        return expectations;
    }

    /**
     * Tests:
     * - Request an ID token and verify its signature
     * - ID token is missing "kid" entry
     * - The JWKS endpoint returns a single key; use it to verify the ID token signature
     * Expected Results:
     * - Should successfully access the protected resource
     */
    @Test
    public void test_idTokenMissingKid_oneJwkReturnedFromJwksUri() throws Exception {
        String conformanceTestName = "rp-id_token-kid-absent-single-jwks";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        registerClientAndUpdateSystemProperties(opConfig, clientId);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);

        String assignedUserName = extractAssignedUserNameFromResponse(response);
        Log.info(thisClass, _testName, "Extracted remote user: [" + assignedUserName + "]");

        Expectations expectations = getTestExpectations_rp_id_token_kid_absent_single_jwks(conformanceTestName, assignedUserName);

        validationUtils.validateResult(response, expectations);
    }

    protected Expectations getTestExpectations_rp_id_token_kid_absent_single_jwks(String conformanceTestName, String assignedUserName) {
        return getSuccessfulConformanceTestExpectations(conformanceTestName, assignedUserName, UserInfo.DISABLED);
    }

    /**
     * Tests:
     * - Request an ID token and verify its signature
     * - ID token is missing "kid" entry
     * - The JWKS endpoint returns multiple keys
     * Expected Results:
     * - 401 when accessing the protected resource
     * - Error message should be logged saying a signing key was not available
     */
    @Test
    public void test_idTokenMissingKid_multipleJwksReturnedFromJwksUri() throws Exception {
        String conformanceTestName = "rp-id_token-kid-absent-multiple-jwks";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        JsonObject rpConfig = registerClientAndUpdateSystemProperties(opConfig, clientId);

        Expectations expectations = getTestExpectations_rp_id_token_kid_absent_multiple_jwks(rpConfig);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);
    }

    protected Expectations getTestExpectations_rp_id_token_kid_absent_multiple_jwks(JsonObject rpConfig) {
        String clientId = rpConfig.getString(Constants.RP_KEY_CLIENT_ID);
        Expectations expectations = getUnauthorizedResponseExpectations();
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1739E_OIDC_CLIENT_NO_VERIFYING_KEY));
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1706E_OIDC_CLIENT_IDTOKEN_VERIFY_ERR + ".+" + clientId));
        return expectations;
    }

    /**
     * Tests:
     * - Accept an ID token signed with RS-256 algorithm
     * Expected Results:
     * - Should successfully access the protected resource
     */
    @Test
    public void test_idTokenValidSignature_rs256() throws Exception {
        String conformanceTestName = "rp-id_token-sig-rs256";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        registerClientAndUpdateSystemProperties(opConfig, clientId);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);

        String assignedUserName = extractAssignedUserNameFromResponse(response);
        Log.info(thisClass, _testName, "Extracted remote user: [" + assignedUserName + "]");

        Expectations expectations = getTestExpectations_rp_id_token_sig_rs256(conformanceTestName, assignedUserName);

        validationUtils.validateResult(response, expectations);
    }

    protected Expectations getTestExpectations_rp_id_token_sig_rs256(String conformanceTestName, String assignedUserName) {
        return getSuccessfulConformanceTestExpectations(conformanceTestName, assignedUserName, UserInfo.DISABLED);
    }

    /**
     * Tests:
     * - Accept an unsigned ID token
     * Expected Results:
     * - Should successfully access the protected resource
     */
    @Test
    public void test_idTokenNoSignature() throws Exception {
        String conformanceTestName = "rp-id_token-sig-none";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        registerClientAndUpdateSystemProperties(opConfig, clientId);

        Map<String, String> varsToSet = new HashMap<String, String>();
        varsToSet.put(Constants.CONFIG_VAR_SIGNATURE_ALGORITHM, "none");
        setServerConfigurationVariables(varsToSet);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);

        String assignedUserName = extractAssignedUserNameFromResponse(response);
        Log.info(thisClass, _testName, "Extracted remote user: [" + assignedUserName + "]");

        Expectations expectations = getTestExpectations_rp_id_token_sig_none(conformanceTestName, assignedUserName);

        validationUtils.validateResult(response, expectations);
    }

    protected Expectations getTestExpectations_rp_id_token_sig_none(String conformanceTestName, String assignedUserName) {
        return getSuccessfulConformanceTestExpectations(conformanceTestName, assignedUserName, UserInfo.DISABLED);
    }

    /**
     * Tests:
     * - ID token is signed with an invalid signature
     * Expected Results:
     * - 401 when accessing the protected resource
     * - Error message should be logged saying ID token validation failed because of a signature verification failure
     */
    @Test
    public void test_idTokenInvalidSignature_rs256() throws Exception {
        String conformanceTestName = "rp-id_token-bad-sig-rs256";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        JsonObject rpConfig = registerClientAndUpdateSystemProperties(opConfig, clientId);

        Expectations expectations = getTestExpectations_rp_id_token_bad_sig_rs256(rpConfig);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);
    }

    protected Expectations getTestExpectations_rp_id_token_bad_sig_rs256(JsonObject rpConfig) {
        String clientId = rpConfig.getString(Constants.RP_KEY_CLIENT_ID);
        Expectations expectations = getUnauthorizedResponseExpectations();
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1756E_OIDC_IDTOKEN_SIGNATURE_VERIFY_ERR + ".+" + clientId));
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1706E_OIDC_CLIENT_IDTOKEN_VERIFY_ERR + ".+" + clientId));
        return expectations;
    }

    /**
     * Tests:
     * - ID token includes a "nonce" value that does not match the original nonce value provided in the authentication request
     * Expected Results:
     * - 401 when accessing the protected resource
     * - Error message should be logged saying ID token validation failed because the nonce in the token didn't match the original
     * value
     */
    @Test
    public void test_idTokenInvalidNonce() throws Exception {
        String conformanceTestName = "rp-nonce-invalid";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        JsonObject rpConfig = registerClientAndUpdateSystemProperties(opConfig, clientId);

        Expectations expectations = getTestExpectations_rp_nonce_invalid(rpConfig);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);
    }

    protected Expectations getTestExpectations_rp_nonce_invalid(JsonObject rpConfig) {
        String clientId = rpConfig.getString(Constants.RP_KEY_CLIENT_ID);
        Expectations expectations = getUnauthorizedResponseExpectations();
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1714E_OIDC_CLIENT_REQUEST_NONCE_FAILED + ".+" + clientId));
        return expectations;
    }

    /**
     * Tests:
     * - Use the client_secret_basic method to authenticate when invoking the token endpoint
     * Expected Results:
     * - Should successfully access the protected resource
     */
    @Test
    public void test_tokenEndpoint_clientSecretBasic() throws Exception {
        String conformanceTestName = "rp-token_endpoint-client_secret_basic";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        registerClientAndUpdateSystemProperties(opConfig, clientId);

        Map<String, String> varsToSet = getUpdatedConfigVariables_rp_token_endpoint_client_secret_basic();
        setServerConfigurationVariables(varsToSet);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);

        String assignedUserName = extractAssignedUserNameFromResponse(response);
        Log.info(thisClass, _testName, "Extracted remote user: [" + assignedUserName + "]");

        Expectations expectations = getTestExpectations_rp_token_endpoint_client_secret_basic(conformanceTestName, assignedUserName);

        validationUtils.validateResult(response, expectations);
    }

    protected Map<String, String> getUpdatedConfigVariables_rp_token_endpoint_client_secret_basic() {
        Map<String, String> varsToSet = new HashMap<String, String>();
        varsToSet.put(Constants.CONFIG_VAR_TOKEN_ENDPOINT_AUTH_METHOD, "client_secret_basic");
        return varsToSet;
    }

    protected Expectations getTestExpectations_rp_token_endpoint_client_secret_basic(String conformanceTestName, String assignedUserName) {
        return getSuccessfulConformanceTestExpectations(conformanceTestName, assignedUserName, UserInfo.DISABLED);
    }

    /**
     * Tests:
     * - Go through authentication flow and access the UserInfo endpoint with the access token in the Authorization header
     * Expected Results:
     * - Should successfully access the protected resource and obtain data from the UserInfo endpoint
     */
    @Test
    public void test_userInfoEndpoint_includeBearerToken_header() throws Exception {
        String conformanceTestName = "rp-userinfo-bearer-header";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        registerClientAndUpdateSystemProperties(opConfig, clientId);

        Map<String, String> varsToSet = getUpdatedConfigVariables_rp_userinfo_bearer_header();
        setServerConfigurationVariables(varsToSet);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);

        String assignedUserName = extractAssignedUserNameFromResponse(response);
        Log.info(thisClass, _testName, "Extracted remote user: [" + assignedUserName + "]");

        Expectations expectations = getTestExpectations_rp_userinfo_bearer_header(conformanceTestName, assignedUserName);

        validationUtils.validateResult(response, expectations);
    }

    protected Map<String, String> getUpdatedConfigVariables_rp_userinfo_bearer_header() {
        Map<String, String> varsToSet = new HashMap<String, String>();
        varsToSet.put(Constants.CONFIG_VAR_USER_INFO_ENDPOINT_ENABLED, "true");
        return varsToSet;
    }

    protected Expectations getTestExpectations_rp_userinfo_bearer_header(String conformanceTestName, String assignedUserName) {
        Expectations expectations = getSuccessfulConformanceTestExpectations(conformanceTestName, assignedUserName, UserInfo.ENABLED);
        expectations.addExpectation(new UserInfoJsonExpectation("sub", StringCheckType.EQUALS, assignedUserName));
        expectations.addExpectation(new UserInfoJsonExpectation("name", JsonCheckType.KEY_DOES_NOT_EXIST, null));
        expectations.addExpectation(new UserInfoJsonExpectation("address", JsonCheckType.KEY_DOES_NOT_EXIST, null));
        expectations.addExpectation(new UserInfoJsonExpectation("email", JsonCheckType.KEY_DOES_NOT_EXIST, null));
        expectations.addExpectation(new UserInfoJsonExpectation("phone_number", JsonCheckType.KEY_DOES_NOT_EXIST, null));
        return expectations;
    }

    /**
     * Tests:
     * - UserInfo response returns a "sub" value that doesn't match the "sub" claim in the ID token
     * Expected Results:
     * - Should successfully access the protected resource, but the UserProfile credential should be missing the UserInfo string
     * - Error message should be logged saying the UserInfo data is not valid because the "sub" claims do not match
     */
    @Test
    public void test_userInfoEndpoint_invalidSub() throws Exception {
        String conformanceTestName = "rp-userinfo-bad-sub-claim";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        registerClientAndUpdateSystemProperties(opConfig, clientId);

        Map<String, String> varsToSet = getUpdatedConfigVariables_rp_userinfo_bad_sub_claim();
        setServerConfigurationVariables(varsToSet);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);

        String assignedUserName = extractAssignedUserNameFromResponse(response);
        Log.info(thisClass, _testName, "Extracted remote user: [" + assignedUserName + "]");

        Expectations expectations = getTestExpectations_rp_userinfo_bad_sub_claim(conformanceTestName, assignedUserName);

        validationUtils.validateResult(response, expectations);
    }

    protected Map<String, String> getUpdatedConfigVariables_rp_userinfo_bad_sub_claim() {
        Map<String, String> varsToSet = new HashMap<String, String>();
        varsToSet.put(Constants.CONFIG_VAR_USER_INFO_ENDPOINT_ENABLED, "true");
        return varsToSet;
    }

    protected Expectations getTestExpectations_rp_userinfo_bad_sub_claim(String conformanceTestName, String assignedUserName) {
        Expectations expectations = getSuccessfulConformanceTestExpectations(conformanceTestName, assignedUserName, UserInfo.DISABLED);
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1749E_USERINFO_INVALID));
        return expectations;
    }

    /**
     * Tests:
     * - Request that certain claims be included in the UserInfo response by including certain scopes in the authentication
     * request
     * Expected Results:
     * - Should successfully access the protected resource
     * - The UserInfo string included in the subject should include information reflecting the scopes included in the
     * authentication request (e.g. name, address, phone number)
     */
    @Test
    public void test_userInfoEndpoint_useScopeValuesToRequestClaims() throws Exception {
        String conformanceTestName = "rp-scope-userinfo-claims";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        registerClientAndUpdateSystemProperties(opConfig, clientId);

        String scopeString = createScopeStringBasedOnOpSupportedScopes(opConfig);
        Map<String, String> varsToSet = getUpdatedConfigVariables_rp_scope_userinfo_claims(scopeString);
        setServerConfigurationVariables(varsToSet);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);

        String assignedUserName = extractAssignedUserNameFromResponse(response);
        Log.info(thisClass, _testName, "Extracted remote user: [" + assignedUserName + "]");

        Expectations expectations = getTestExpectations_rp_scope_userinfo_claims(conformanceTestName, assignedUserName);

        validationUtils.validateResult(response, expectations);
    }

    protected Map<String, String> getUpdatedConfigVariables_rp_scope_userinfo_claims(String scopeString) {
        Map<String, String> varsToSet = new HashMap<String, String>();
        varsToSet.put(Constants.CONFIG_VAR_USER_INFO_ENDPOINT_ENABLED, "true");
        varsToSet.put(Constants.CONFIG_VAR_SCOPE, scopeString.trim());
        return varsToSet;
    }

    protected Expectations getTestExpectations_rp_scope_userinfo_claims(String conformanceTestName, String assignedUserName) {
        Expectations expectations = getSuccessfulConformanceTestExpectations(conformanceTestName, assignedUserName, UserInfo.ENABLED);
        expectations.addExpectation(new UserInfoJsonExpectation("sub", StringCheckType.EQUALS, assignedUserName));
        expectations.addExpectation(new UserInfoJsonExpectation("name"));
        expectations.addExpectation(new UserInfoJsonExpectation("address", ValueType.OBJECT));
        expectations.addExpectation(new UserInfoJsonExpectation("email"));
        expectations.addExpectation(new UserInfoJsonExpectation("phone_number"));
        return expectations;
    }

    /************************************************ Helper methods ************************************************/

    /**
     * Extending classes must implement this method to return the appropriate redirect URI for the specified client for the
     * feature under test.
     */
    protected abstract String getRedirectUriForClient(String clientId);

    protected JsonObject getOpConfigurationForConformanceTest(String conformanceTestName) throws Exception {
        String method = "getOpConfigurationForConformanceTest";
        String configUrl = certificationBaseUrl + "/" + conformanceTestName + "/.well-known/openid-configuration";
        try {
            Object response = actions.invokeUrl(method, configUrl);
            String responseText = WebResponseUtils.getResponseText(response);
            JsonObject opConfig = Json.createReader(new StringReader(responseText)).readObject();
            Log.info(thisClass, method, "Received OP config for test [" + conformanceTestName + "]: " + opConfig);
            return opConfig;
        } catch (Exception e) {
            throw new Exception("Failed to obtain OP configuration for test [" + conformanceTestName + "]. Exception was: " + e);
        }
    }

    /**
     * Performs dynamic registration to register the client for the conformance test and updates system properties to use the OP
     * and RP values returned from the certification host (e.g. OP's authorization/token/jwks endpoints, RP's client ID/secret).
     */
    protected JsonObject registerClientAndUpdateSystemProperties(JsonObject opConfig, String clientId) throws Exception {
        try {
            String registrationUrl = opConfig.getString(Constants.OP_KEY_REGISTRATION_ENDPOINT);

            Page response = submitAndValidateRegistrationRequest(clientId, registrationUrl);
            JsonObject clientConfig = parseClientConfigFromResponse(response);
            setServerConfigurationVariables(clientConfig, opConfig);

            return clientConfig;
        } catch (Exception e) {
            throw new Exception("An error occurred attempting to register the client. Exception was: " + e);
        }
    }

    protected Page submitAndValidateRegistrationRequest(String clientId, String registrationUrl) throws Exception {
        WebRequest request = createClientRegistrationRequest(clientId, registrationUrl);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_CREATED));

        Page response = actions.submitRequest("submitAndValidateRegistrationRequest", request);
        validationUtils.validateResult(response, expectations);
        return response;
    }

    protected WebRequest createClientRegistrationRequest(String clientId, String registrationUrl) throws MalformedURLException {
        JsonObject requestBody = buildClientRegistrationRequestBody(clientId);
        WebRequest request = actions.createPostRequest(registrationUrl, requestBody.toString());
        request.setAdditionalHeader("Content-Type", "application/json");
        return request;
    }

    protected JsonObject buildClientRegistrationRequestBody(String clientId) {
        JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();

        JsonArrayBuilder redirectUris = Json.createArrayBuilder();
        redirectUris.add(getRedirectUriForClient(clientId));

        bodyBuilder.add(Constants.CLIENT_REGISTRATION_KEY_REDIRECT_URIS, redirectUris.build());
        bodyBuilder.add(Constants.CLIENT_REGISTRATION_KEY_CONTACTS, clientRegistrationContact);
        if (!_testName.contains("clientSecretBasic")) {
            // client_secret_post is the default token endpoint auth method for our OIDC client, however the default per the OIDC spec is client_secret_basic.
            // We therefore must include this entry to ensure the right authentication method is used (except for the conformance test that's supposed to use
            // client_secret_basic).
            bodyBuilder.add(Constants.CLIENT_REGISTRATION_KEY_TOKEN_ENDPOINT_AUTH_METHOD, "client_secret_post");
        }
        return bodyBuilder.build();
    }

    protected JsonObject parseClientConfigFromResponse(Page response) throws Exception {
        String responseText = WebResponseUtils.getResponseText(response);
        return Json.createReader(new StringReader(responseText)).readObject();
    }

    /**
     * Sets the variables used in the server configuration for various RP- and OP-related values (e.g. endpoint URLs, client ID,
     * client secret). This allows us to dynamically change the variable values in the server configuration without having to
     * reboot the server.
     */
    protected void setServerConfigurationVariables(JsonObject rpConfig, JsonObject opConfig) throws Exception {
        Map<String, String> variablesToSet = getDefaultServerVariables(rpConfig, opConfig);
        setServerConfigurationVariables(variablesToSet);
    }

    protected Map<String, String> getDefaultServerVariables(JsonObject rpConfig, JsonObject opConfig) {
        Map<String, String> variablesToSet = new HashMap<String, String>();
        variablesToSet.put(Constants.CONFIG_VAR_CLIENT_ID, rpConfig.getString(Constants.RP_KEY_CLIENT_ID));
        variablesToSet.put(Constants.CONFIG_VAR_CLIENT_SECRET, rpConfig.getString(Constants.RP_KEY_CLIENT_SECRET));
        variablesToSet.put(Constants.CONFIG_VAR_SCOPE, defaultScope);
        variablesToSet.put(Constants.CONFIG_VAR_AUTHORIZATION_ENDPOINT, opConfig.getString(Constants.OP_KEY_AUTHORIZATION_ENDPOINT));
        variablesToSet.put(Constants.CONFIG_VAR_TOKEN_ENDPOINT, opConfig.getString(Constants.OP_KEY_TOKEN_ENDPOINT));
        variablesToSet.put(Constants.CONFIG_VAR_USER_INFO_ENDPOINT, opConfig.getString(Constants.OP_KEY_USER_INFO_ENDPOINT));
        variablesToSet.put(Constants.CONFIG_VAR_JWKS_URI, opConfig.getString(Constants.OP_KEY_JWKS_URI));
        variablesToSet.put(Constants.CONFIG_VAR_SIGNATURE_ALGORITHM, defaultSignatureAlgorithm);
        variablesToSet.put(Constants.CONFIG_VAR_TOKEN_ENDPOINT_AUTH_METHOD, defaultTokenEndpointAuthMethod);
        variablesToSet.put(Constants.CONFIG_VAR_USER_INFO_ENDPOINT_ENABLED, "false");
        return variablesToSet;
    }

    protected void setServerConfigurationVariables(Map<String, String> variablesToSet) throws Exception {
        server.setMarkToEndOfLog();
        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Variable> varList = config.getVariables();

        for (Entry<String, String> variableEntry : variablesToSet.entrySet()) {
            addOrUpdateConfigVariable(varList, variableEntry.getKey(), variableEntry.getValue());
        }

        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(server.listAllInstalledAppsForValidation());
    }

    protected void addOrUpdateConfigVariable(ConfigElementList<Variable> vars, String name, String value) {
        Variable var = vars.getBy("name", name);
        if (var == null) {
            vars.add(new Variable(name, value));
        } else {
            var.setValue(value);
        }
    }

    protected Expectations getUnauthorizedResponseExpectations() {
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_EQUALS, protectedUrl, "Did not reach the expected protected URL."));
        return expectations;
    }

    protected Expectations getSuccessfulConformanceTestExpectations(String conformanceTestName, String assignedUserName, UserInfo userInfo) {
        Expectations expectations = getSuccessfulAccessExpectations(protectedUrl);
        expectations.addExpectations(getResponseServletContentExpectations(conformanceTestName, assignedUserName, userInfo));
        return expectations;
    }

    protected Expectations getSuccessfulAccessExpectations(String protectedResourceUrl) {
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_OK));
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_EQUALS, protectedResourceUrl, "Did not reach the expected protected URL."));
        return expectations;
    }

    protected Expectations getResponseServletContentExpectations(String conformanceTestName, String assignedUserName, UserInfo userInfo) {
        Expectations expectations = new Expectations();
        expectations.addExpectations(getServletOutputPublicCredentialExpectations(conformanceTestName, assignedUserName));
        expectations.addExpectations(getServletOutputUserInfoPresenceExpectations(userInfo));
        return expectations;
    }

    protected Expectations getServletOutputPublicCredentialExpectations(String conformanceTestName, String assignedUserName) {
        String realm = getExpectedRealm(conformanceTestName);
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, "uniqueSecurityName=" + assignedUserName, "Did not find the expected unique security name in the servlet output."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, "accessId=user:" + realm + "/" + assignedUserName, "Did not find the expected access ID in the servlet output."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, "realmName=" + realm, "Did not find the expected realm name in the servlet output."));
        return expectations;
    }

    protected String getExpectedRealm(String conformanceTestName) {
        return CERTIFICATION_HOST_AND_PORT + "/" + rpId + "/" + conformanceTestName;
    }

    protected Expectations getServletOutputUserInfoPresenceExpectations(UserInfo userInfo) {
        Expectations expectations = new Expectations();
        if (userInfo == UserInfo.ENABLED) {
            // If UserInfo is enabled, the UserInfo information must, at at minimum, include the "sub" claim
            expectations.addExpectation(new UserInfoJsonExpectation("sub"));
        } else {
            expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, BaseServlet.OUTPUT_PREFIX + "string: null", "UserInfo string in the subject's private credentials should have been null because the UserInfo endpoint is not enabled."));
        }
        return expectations;
    }

    /**
     * The OIDC certification OP creates a random user name as the authenticated user in each test. This method extracts that
     * username from the test servlet response text.
     */
    protected String extractAssignedUserNameFromResponse(Page response) throws Exception {
        String responseText = WebResponseUtils.getResponseText(response);
        return FatStringUtils.extractRegexGroup(responseText, "getRemoteUser: (.+)");
    }

    protected String createScopeStringBasedOnOpSupportedScopes(JsonObject opConfig) {
        String scopeString = "";
        JsonArray scopesSupported = opConfig.getJsonArray(Constants.OP_KEY_SCOPES_SUPPORTED);
        for (int i = 0; i < scopesSupported.size(); i++) {
            String scope = scopesSupported.getString(i);
            // "offline_access" scope requires some additional RP registration settings that we aren't bothering with, so don't add it if it's supported
            if (!scope.equals("offline_access")) {
                scopeString += scope + " ";
            }
        }
        return scopeString;
    }

}
