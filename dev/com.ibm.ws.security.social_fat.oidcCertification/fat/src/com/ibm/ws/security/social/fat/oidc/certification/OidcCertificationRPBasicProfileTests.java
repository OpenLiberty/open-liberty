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
import java.util.List;
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

    private final String protectedUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + "/formlogin/SimpleServlet";
    private final String codeCookiePatternString = Pattern.quote("cookie: " + Constants.CODE_COOKIE_NAME + " value: ") + "([^_]+)_";
    /** Required for the certification provider's client registration request. Must have a valid email format. */
    private final String clientRegistrationContact = "oidc_certification_contact@us.ibm.com";

    @BeforeClass
    public static void setUp() throws Exception {
        verifyCertificationEndpointIsResponding();

        serverTracker.addServer(server);

        List<String> waitForMessages = new ArrayList<String>();
        waitForMessages.add(MessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*" + Constants.DEFAULT_CONTEXT_ROOT);

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
    @Mode(TestMode.LITE)
    @Test
    public void test_responseType_code() throws Exception {
        String conformanceTestName = "rp-response_type-code";
        String oidcLoginId = "oidcLogin1";

        JsonObject opConfig = getOpConfigurationForConformanceTest(conformanceTestName);
        JsonObject clientConfig = registerClientAndUpdateSystemProperties(opConfig, oidcLoginId);

        Expectations expectations = new Expectations();
        expectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_OK));
        expectations.addExpectation(new ResponseUrlExpectation(Constants.STRING_EQUALS, protectedUrl, "Did not reach the expected protected URL."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_MATCHES, codeCookiePatternString, "Did not find the expected " + Constants.CODE_COOKIE_NAME + " cookie pattern in the servlet output."));
        expectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, "realmName=" + CERTIFICATION_HOST_AND_PORT, "Did not find the expected realm name in the servlet output."));

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, expectations);

        // TODO - user info endpoint check?
        verifyCodeCookieValues(response, conformanceTestName, clientConfig);
    }

    // TODO
    // (rp-id_token-issuer-mismatch) test_idTokenIssuerMismatch
    // (rp-id_token-sub) test_idTokenMissingSub
    // (rp-id_token-aud) test_idTokenInvalidAud
    // (rp-id_token-iat) test_idTokenMissingIat
    // (rp-id_token-kid-absent-single-jwks) test_idTokenMissingKid_oneJwkReturnedFromJwksUri
    // (rp-id_token-kid-absent-multiple-jwks) test_idTokenMissingKid_multipleJwksReturnedFromJwksUri
    // (rp-id_token-sig-rs256) test_idTokenValidSignature_rs256
    // (rp-id_token-sig-none) test_idTokenNoSignature
    // (rp-id_token-bad-sig-rs256) test_idTokenInvalidSignature_rs256
    // (rp-userinfo-bearer-header) test_userInfoEndpoint_includeBearerToken_header
    // (rp-userinfo-bearer-body) (Optional) test_userInfoEndpoint_includeBearerToken_body
    // (rp-userinfo-bad-sub-claim) test_userInfoEndpoint_invalidSub
    // (rp-nonce-invalid) test_idTokenInvalidNonce
    // (implicit) "openid" scope present in all requests
    // (rp-scope-userinfo-claims) test_userInfoEndpoint_useScopeValuesToRequestClaims
    // (rp-token_endpoint-client_secret_basic) test_tokenEndpoint_clientSecretBasic
    // (implicit) use https for all endpoints (http should be allowed for pure code flow)

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
        // client_secret_post is the default token endpoint auth method for our oidcLogin element, however the default per the OIDC spec is client_secret_basic.
        // We therefore must include this entry to ensure the right authentication method is used.
        bodyBuilder.add(Constants.CLIENT_REGISTRATION_KEY_TOKEN_ENDPOINT_AUTH_METHOD, "client_secret_post");
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
        ServerConfiguration config = server.getServerConfiguration();
        ConfigElementList<Variable> varList = config.getVariables();

        addOrUpdateConfigVariable(varList, Constants.CONFIG_VAR_CLIENT_ID, rpConfig.getString(Constants.RP_KEY_CLIENT_ID));
        addOrUpdateConfigVariable(varList, Constants.CONFIG_VAR_CLIENT_SECRET, rpConfig.getString(Constants.RP_KEY_CLIENT_SECRET));
        addOrUpdateConfigVariable(varList, Constants.CONFIG_VAR_AUTHORIZATION_ENDPOINT, opConfig.getString(Constants.OP_KEY_AUTHORIZATION_ENDPOINT));
        addOrUpdateConfigVariable(varList, Constants.CONFIG_VAR_TOKEN_ENDPOINT, opConfig.getString(Constants.OP_KEY_TOKEN_ENDPOINT));
        addOrUpdateConfigVariable(varList, Constants.CONFIG_VAR_JWKS_URI, opConfig.getString(Constants.OP_KEY_JWKS_URI));

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

    /**
     * Extracts the code cookie value from the response text, decodes it, reads it into a JSON object, and verifies some of the
     * values within the resulting object.
     */
    private void verifyCodeCookieValues(Page response, String conformanceTestName, JsonObject clientConfig) throws Exception, UnsupportedEncodingException {
        String codeCookieValue = extractCodeCookieValueFromResponse(response);
        Log.info(thisClass, testName.getMethodName(), "Found code cookie value: [" + codeCookieValue + "]");

        String decodedValue = new String(Base64Coder.base64DecodeString(codeCookieValue), "UTF-8");
        Log.info(thisClass, testName.getMethodName(), "Decoded cookie value: [" + decodedValue + "]");

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

}
