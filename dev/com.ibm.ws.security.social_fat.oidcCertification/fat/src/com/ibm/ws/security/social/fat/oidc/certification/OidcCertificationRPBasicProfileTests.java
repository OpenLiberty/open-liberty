/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.social.fat.oidc.certification;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
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
import com.ibm.ws.common.internal.encoder.Base64Coder;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.social.MessageConstants;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * See: https://openid.net/certification/rp_testing/
 * 
 * This class should encompass all tests required for the minimal certification for the Basic RP profile.
 */
@Mode(TestMode.LITE)
@RunWith(FATRunner.class)
public class OidcCertificationRPBasicProfileTests extends CommonSecurityFat {

    public static Class<?> thisClass = OidcCertificationRPBasicProfileTests.class;

    @Server("com.ibm.ws.security.social_fat.oidcCertification")
    public static LibertyServer server;

    static TestActions actions = new TestActions();
    static TestValidationUtils validationUtils = new TestValidationUtils();

    /** Identifies the RP so the certification host can keep track of server-visible results for us */
    private static final String RP_ID = Constants.CERTIFICATION_RP_ID + ".code";
    private static final String CERTIFICATION_HOST_AND_PORT = "https://rp.certification.openid.net:8080";
    private static final String CERTIFICATION_BASE_URL = CERTIFICATION_HOST_AND_PORT + "/" + RP_ID;

    private final String defaultOidcLogin = "oidcLogin1";
    private final String protectedUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/formlogin/SimpleServlet";
    private final String codeCookiePatternString = Pattern.quote("cookie: " + Constants.CODE_COOKIE_NAME + " value: ") + "([^_]+)_";
    /** Required for the certification provider's client registration request. Must have a valid email format. */
    private final String clientRegistrationContact = "oidc_certification_contact@us.ibm.com";
    private final String defaultSignatureAlgorithm = "RS256";
    private final String defaultTokenEndpointAuthMethod = "client_secret_post";

    @BeforeClass
    public static void setUp() throws Exception {
        verifyCertificationEndpointIsResponding();

        serverTracker.addServer(server);

        List<String> waitForMessages = new ArrayList<String>();
        waitForMessages.add(MessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + Constants.DEFAULT_CONTEXT_ROOT);

        List<String> ignoreStartupMessages = new ArrayList<String>();
        ignoreStartupMessages.add(MessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE + ".*" + "tokenEndpointAuthMethod");
        server.addIgnoredErrors(ignoreStartupMessages);

        server.startServerUsingConfiguration(Constants.CONFIGS_DIR + "server_oidcCertification.xml", waitForMessages);
    }

    private static void verifyCertificationEndpointIsResponding() {
        String method = "verifyCertificationEndpointIsResponding";
        String endpoint = CERTIFICATION_BASE_URL;
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(method, HttpServletResponse.SC_OK));
        try {
            Page response = actions.invokeUrl(method, endpoint);
            validationUtils.validateResult(response, method, expectations);
        } catch (Exception e) {
            fail("Failed to properly access the RP certification endpoint [" + endpoint + "]. No tests will run in this class. The exception was: " + e);
        }
    }

    /**
     * Tests:
     * - Make an authentication request using the Authorization Code Flow
     * Expected Results:
     * - Should successfully make authentication request and access the protected resource
     */
    @Test
    public void test_responseType_code() throws Exception {
        String conformanceTestName = "rp-response_type-code";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        JsonObject clientConfig = registerClientAndUpdateSystemProperties(opConfig, defaultOidcLogin);

        Expectations expectations = getSuccessfulAccessExpectations(protectedUrl);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);

        verifySuccessfulConformanceTestResponse(response, conformanceTestName, clientConfig);
    }

    /**
     * Tests:
     * - Request an ID token and verify its "iss" claim
     * - The "iss" claim in the ID token returned from the OP does not match the expected issuer of the token
     * Expected Results:
     * - 401 when accessing the protected resource
     * - CWWKS1751E message should be logged saying the issuer in the ID token does not match the expected issuer value
     */
    @Test
    public void test_idTokenIssuerMismatch() throws Exception {
        String conformanceTestName = "rp-id_token-issuer-mismatch";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        JsonObject clientConfig = registerClientAndUpdateSystemProperties(opConfig, defaultOidcLogin);

        String clientId = clientConfig.getString(Constants.RP_KEY_CLIENT_ID);

        Expectations expectations = getUnauthorizedResponseExpectations();
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1751E_OIDC_IDTOKEN_VERIFY_ISSUER_ERR + ".+" + clientId));
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1706E_OIDC_CLIENT_IDTOKEN_VERIFY_ERR + ".+" + clientId));

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);
    }

    /**
     * Tests:
     * - Request an ID token and verify its "sub" claim
     * - The ID token returned from the OP does not contain a "sub" claim
     * Expected Results:
     * - 401 when accessing the protected resource
     * - CWWKS1706E message should be logged saying the OIDC client failed to validate the ID token because the "sub" claim was
     * missing
     */
    @Test
    public void test_idTokenMissingSub() throws Exception {
        String conformanceTestName = "rp-id_token-sub";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        JsonObject clientConfig = registerClientAndUpdateSystemProperties(opConfig, defaultOidcLogin);

        String clientId = clientConfig.getString(Constants.RP_KEY_CLIENT_ID);

        Expectations expectations = getUnauthorizedResponseExpectations();
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1706E_OIDC_CLIENT_IDTOKEN_VERIFY_ERR + ".+" + clientId + ".+" + "No Subject.+claim"));

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);
    }

    /**
     * Tests:
     * - Request an ID token and verify its "aud" claim
     * - The ID token returned from the OP does not contain an "aud" claim
     * Expected Results:
     * - 401 when accessing the protected resource
     * - CWWKS1754E message should be logged saying the OIDC client failed to validate the ID token because the "aud" claim
     * doesn't match the client ID
     */
    @Test
    public void test_idTokenInvalidAud() throws Exception {
        String conformanceTestName = "rp-id_token-aud";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        JsonObject clientConfig = registerClientAndUpdateSystemProperties(opConfig, defaultOidcLogin);

        String clientId = clientConfig.getString(Constants.RP_KEY_CLIENT_ID);

        Expectations expectations = getUnauthorizedResponseExpectations();
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1706E_OIDC_CLIENT_IDTOKEN_VERIFY_ERR + ".+" + clientId + ".+" + MessageConstants.CWWKS1754E_OIDC_IDTOKEN_VERIFY_AUD_ERR));

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);
    }

    /**
     * Tests:
     * - Request an ID token and verify its "iat" claim
     * - The ID token returned from the OP does not contain an "iat" claim
     * Expected Results:
     * - 401 when accessing the protected resource
     * - CWWKS1775E message should be logged saying the OIDC client failed to validate the ID token because the "iat" claim is
     * missing
     */
    @Test
    public void test_idTokenMissingIat() throws Exception {
        String conformanceTestName = "rp-id_token-iat";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        JsonObject clientConfig = registerClientAndUpdateSystemProperties(opConfig, defaultOidcLogin);

        String clientId = clientConfig.getString(Constants.RP_KEY_CLIENT_ID);

        Expectations expectations = getUnauthorizedResponseExpectations();
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1775E_OIDC_ID_VERIFY_IAT_ERR + ".+" + clientId));
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1706E_OIDC_CLIENT_IDTOKEN_VERIFY_ERR + ".+" + clientId));

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);
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
        JsonObject clientConfig = registerClientAndUpdateSystemProperties(opConfig, defaultOidcLogin);

        Expectations expectations = getSuccessfulAccessExpectations(protectedUrl);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);

        verifySuccessfulConformanceTestResponse(response, conformanceTestName, clientConfig);
    }

    /**
     * Tests:
     * - Request an ID token and verify its signature
     * - ID token is missing "kid" entry
     * - The JWKS endpoint returns multiple keys
     * Expected Results:
     * - 401 when accessing the protected resource
     * - CWWKS1739E message should be logged saying a signing key was not available
     */
    @Test
    public void test_idTokenMissingKid_multipleJwksReturnedFromJwksUri() throws Exception {
        String conformanceTestName = "rp-id_token-kid-absent-multiple-jwks";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        JsonObject clientConfig = registerClientAndUpdateSystemProperties(opConfig, defaultOidcLogin);

        String clientId = clientConfig.getString(Constants.RP_KEY_CLIENT_ID);

        Expectations expectations = getUnauthorizedResponseExpectations();
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1739E_OIDC_CLIENT_NO_VERIFYING_KEY));
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1706E_OIDC_CLIENT_IDTOKEN_VERIFY_ERR + ".+" + clientId));

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);
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
        JsonObject clientConfig = registerClientAndUpdateSystemProperties(opConfig, defaultOidcLogin);

        Expectations expectations = getSuccessfulAccessExpectations(protectedUrl);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);

        verifySuccessfulConformanceTestResponse(response, conformanceTestName, clientConfig);
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
        JsonObject clientConfig = registerClientAndUpdateSystemProperties(opConfig, defaultOidcLogin);

        Map<String, String> varsToSet = new HashMap<String, String>();
        varsToSet.put(Constants.CONFIG_VAR_SIGNATURE_ALGORITHM, "none");
        setServerConfigurationVariables(varsToSet);

        Expectations expectations = getSuccessfulAccessExpectations(protectedUrl);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);

        verifySuccessfulConformanceTestResponse(response, conformanceTestName, clientConfig);
    }

    /**
     * Tests:
     * - ID token is signed with an invalid signature
     * Expected Results:
     * - 401 when accessing the protected resource
     * - CWWKS1756E message should be logged saying ID token validation failed because of a signature verification failure
     */
    @Test
    public void test_idTokenInvalidSignature_rs256() throws Exception {
        String conformanceTestName = "rp-id_token-bad-sig-rs256";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        JsonObject clientConfig = registerClientAndUpdateSystemProperties(opConfig, defaultOidcLogin);

        String clientId = clientConfig.getString(Constants.RP_KEY_CLIENT_ID);

        Expectations expectations = getUnauthorizedResponseExpectations();
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1756E_OIDC_IDTOKEN_SIGNATURE_VERIFY_ERR + ".+" + clientId));
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1706E_OIDC_CLIENT_IDTOKEN_VERIFY_ERR + ".+" + clientId));

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);
    }

    /**
     * Tests:
     * - ID token includes a "nonce" value that does not match the original nonce value provided in the authentication request
     * Expected Results:
     * - 401 when accessing the protected resource
     * - CWWKS1714E message should be logged saying ID token validation failed because the nonce in the token didn't match the
     * original value
     */
    @Test
    public void test_idTokenInvalidNonce() throws Exception {
        String conformanceTestName = "rp-nonce-invalid";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        JsonObject clientConfig = registerClientAndUpdateSystemProperties(opConfig, defaultOidcLogin);

        String clientId = clientConfig.getString(Constants.RP_KEY_CLIENT_ID);

        Expectations expectations = getUnauthorizedResponseExpectations();
        expectations.addExpectation(new ServerMessageExpectation(server, MessageConstants.CWWKS1714E_OIDC_CLIENT_REQUEST_NONCE_FAILED + ".+" + clientId));

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);
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
        JsonObject clientConfig = registerClientAndUpdateSystemProperties(opConfig, defaultOidcLogin);

        Map<String, String> varsToSet = new HashMap<String, String>();
        varsToSet.put(Constants.CONFIG_VAR_TOKEN_ENDPOINT_AUTH_METHOD, "client_secret_basic");
        setServerConfigurationVariables(varsToSet);

        Expectations expectations = getSuccessfulAccessExpectations(protectedUrl);

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);

        verifySuccessfulConformanceTestResponse(response, conformanceTestName, clientConfig);
    }

    // TODO
    // (rp-userinfo-bearer-header) test_userInfoEndpoint_includeBearerToken_header
    // (rp-userinfo-bad-sub-claim) test_userInfoEndpoint_invalidSub
    // (rp-scope-userinfo-claims) test_userInfoEndpoint_useScopeValuesToRequestClaims

    /************************************************ Helper methods ************************************************/

    private JsonObject getOpConfigurationForConformanceTest(String conformanceTestName) throws Exception {
        String method = "getOpConfigurationForConformanceTest";
        String configUrl = CERTIFICATION_BASE_URL + "/" + conformanceTestName + "/.well-known/openid-configuration";
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
    private JsonObject registerClientAndUpdateSystemProperties(JsonObject opConfig, String oidcLoginId) throws Exception {
        try {
            String registrationUrl = opConfig.getString(Constants.OP_KEY_REGISTRATION_ENDPOINT);

            Page response = submitAndValidateRegistrationRequest(oidcLoginId, registrationUrl);
            JsonObject clientConfig = parseClientConfigFromResponse(response);
            setServerConfigurationVariables(clientConfig, opConfig);

            return clientConfig;
        } catch (Exception e) {
            throw new Exception("An error occurred attempting to register the client. Exception was: " + e);
        }
    }

    private Page submitAndValidateRegistrationRequest(String oidcLoginId, String registrationUrl) throws Exception {
        WebRequest request = createClientRegistrationRequest(oidcLoginId, registrationUrl);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_CREATED));

        Page response = actions.submitRequest("submitAndValidateRegistrationRequest", request);
        validationUtils.validateResult(response, expectations);
        return response;
    }

    private WebRequest createClientRegistrationRequest(String oidcLoginId, String registrationUrl) throws MalformedURLException {
        JsonObject requestBody = buildClientRegistrationRequestBody(oidcLoginId);
        WebRequest request = actions.createPostRequest(registrationUrl, requestBody.toString());
        request.setAdditionalHeader("Content-Type", "application/json");
        return request;
    }

    private JsonObject buildClientRegistrationRequestBody(String oidcLoginId) {
        JsonObjectBuilder bodyBuilder = Json.createObjectBuilder();

        JsonArrayBuilder redirectUris = Json.createArrayBuilder();
        redirectUris.add("https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + Constants.DEFAULT_CONTEXT_ROOT + "/redirect/" + oidcLoginId);

        bodyBuilder.add(Constants.CLIENT_REGISTRATION_KEY_REDIRECT_URIS, redirectUris.build());
        bodyBuilder.add(Constants.CLIENT_REGISTRATION_KEY_CONTACTS, clientRegistrationContact);
        if (!testName.getMethodName().contains("clientSecretBasic")) {
            // client_secret_post is the default token endpoint auth method for our oidcLogin element, however the default per the OIDC spec is client_secret_basic.
            // We therefore must include this entry to ensure the right authentication method is used (except for the conformance test that's supposed to use
            // client_secret_basic).
            bodyBuilder.add(Constants.CLIENT_REGISTRATION_KEY_TOKEN_ENDPOINT_AUTH_METHOD, "client_secret_post");
        }
        return bodyBuilder.build();
    }

    private JsonObject parseClientConfigFromResponse(Page response) throws Exception {
        String responseText = WebResponseUtils.getResponseText(response);
        return Json.createReader(new StringReader(responseText)).readObject();
    }

    /**
     * Sets the variables used in the server configuration for various RP- and OP-related values (e.g. endpoint URLs, client ID,
     * client secret). This allows us to dynamically change the variable values in the server configuration without having to
     * reboot the server.
     */
    private void setServerConfigurationVariables(JsonObject rpConfig, JsonObject opConfig) throws Exception {
        Map<String, String> variablesToSet = new HashMap<String, String>();
        variablesToSet.put(Constants.CONFIG_VAR_CLIENT_ID, rpConfig.getString(Constants.RP_KEY_CLIENT_ID));
        variablesToSet.put(Constants.CONFIG_VAR_CLIENT_SECRET, rpConfig.getString(Constants.RP_KEY_CLIENT_SECRET));
        variablesToSet.put(Constants.CONFIG_VAR_AUTHORIZATION_ENDPOINT, opConfig.getString(Constants.OP_KEY_AUTHORIZATION_ENDPOINT));
        variablesToSet.put(Constants.CONFIG_VAR_TOKEN_ENDPOINT, opConfig.getString(Constants.OP_KEY_TOKEN_ENDPOINT));
        variablesToSet.put(Constants.CONFIG_VAR_JWKS_URI, opConfig.getString(Constants.OP_KEY_JWKS_URI));
        variablesToSet.put(Constants.CONFIG_VAR_SIGNATURE_ALGORITHM, defaultSignatureAlgorithm);
        variablesToSet.put(Constants.CONFIG_VAR_TOKEN_ENDPOINT_AUTH_METHOD, defaultTokenEndpointAuthMethod);
        setServerConfigurationVariables(variablesToSet);
    }

    private void setServerConfigurationVariables(Map<String, String> variablesToSet) throws Exception {
        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Variable> varList = config.getVariables();

        for (Entry<String, String> variableEntry : variablesToSet.entrySet()) {
            addOrUpdateConfigVariable(varList, variableEntry.getKey(), variableEntry.getValue());
        }

        server.updateServerConfiguration(config);
        server.waitForConfigUpdateInLogUsingMark(server.listAllInstalledAppsForValidation());
    }

    void addOrUpdateConfigVariable(ConfigElementList<Variable> vars, String name, String value) {
        Variable var = vars.getBy("name", name);
        if (var == null) {
            vars.add(new Variable(name, value));
        } else {
            var.setValue(value);
        }
    }

    private void verifySuccessfulConformanceTestResponse(Page response, String conformanceTestName, JsonObject clientConfig) throws Exception, UnsupportedEncodingException {
        // TODO - user info endpoint check?
        verifyCodeCookieValues(response, conformanceTestName, clientConfig);
    }

    /**
     * Extracts the code cookie value from the response text, decodes it, reads it into a JSON object, and verifies some of the
     * values within the resulting object.
     */
    private void verifyCodeCookieValues(Page response, String conformanceTestName, JsonObject clientConfig) throws Exception, UnsupportedEncodingException {
        String codeCookieValue = extractCodeCookieValueFromResponse(response);
        Log.info(thisClass, testName.getMethodName(), "Found code cookie value: [" + codeCookieValue + "]");

        String decodedValue = new String(Base64Coder.base64DecodeString(codeCookieValue), "UTF-8");
        Log.info(thisClass, testName.getMethodName(), "Decoded cookie value: [" + decodedValue + "]");

        Log.info(thisClass, "verifyCodeCookieValues", "Verifying the issuer and client ID values in the code cookie");
        JsonObject codeObject = Json.createReader(new StringReader(decodedValue)).readObject();
        assertEquals("The issuer value found does not match the expected value for this conformance test.", CERTIFICATION_BASE_URL + "/" + conformanceTestName, codeObject.getString("iss"));
        assertEquals("The client_id value found does not match the expected value for this conformance test.", clientConfig.getString(Constants.RP_KEY_CLIENT_ID), codeObject.getString("client_id"));
    }

    private String extractCodeCookieValueFromResponse(Page response) throws Exception {
        String responseText = WebResponseUtils.getResponseText(response);
        Pattern codeCookieValuePattern = Pattern.compile(codeCookiePatternString);
        Matcher cookieValuleMatcher = codeCookieValuePattern.matcher(responseText);
        if (!cookieValuleMatcher.find()) {
            fail("Failed to find the code cookie pattern (" + codeCookiePatternString + ") in the response text. The response text was: " + responseText);
        }
        return cookieValuleMatcher.group(1);
    }

    private Expectations getSuccessfulAccessExpectations(String protectedResourceUrl) {
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_OK));
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_EQUALS, protectedResourceUrl, "Did not reach the expected protected URL."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_MATCHES, codeCookiePatternString, "Did not find the expected " + Constants.CODE_COOKIE_NAME + " cookie pattern in the servlet output."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, "realmName=" + CERTIFICATION_HOST_AND_PORT, "Did not find the expected realm name in the servlet output."));
        return expectations;
    }

    private Expectations getUnauthorizedResponseExpectations() {
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_EQUALS, protectedUrl, "Did not reach the expected protected URL."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, MessageConstants.CWWKS5489E_PUBLIC_FACING_ERROR, "Should have found the public-facing error message in the protected resource invocation response but did not."));
        return expectations;
    }

}
