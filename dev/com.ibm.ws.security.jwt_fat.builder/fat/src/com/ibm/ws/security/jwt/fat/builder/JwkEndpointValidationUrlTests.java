/*******************************************************************************
 * Copyright (c) 2018, 2109 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.builder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseMessageExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.jwt.ClaimConstants;
import com.ibm.ws.security.fat.common.jwt.JwtMessageConstants;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.jwt.fat.buider.actions.JwtBuilderActions;
import com.ibm.ws.security.jwt.fat.builder.utils.BuilderHelpers;
import com.ibm.ws.security.jwt.fat.builder.validation.BuilderTestValidationUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * Tests that use the Consumer API when extending the ConsumeMangledJWTTests.
 * The server will be configured with the appropriate jwtConsumer's
 * We will validate that we can <use> (and the output is correct):
 * 1) create a JWTConsumer
 * 2) create a JwtToken object
 * 3) create a claims object
 * 4) use all of the get methods on the claims object
 * 5) use toJsonString method got get all attributes in the payload
 *
 */

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JwkEndpointValidationUrlTests extends CommonSecurityFat {

    @Server("com.ibm.ws.security.jwt_fat.builder")
    public static LibertyServer builderServer;

    private static final JwtBuilderActions actions = new JwtBuilderActions();
    public static final BuilderTestValidationUtils validationUtils = new BuilderTestValidationUtils();

    String urlPart1 = "jwt/ibm/api/";
    String urlJwkPart = "/jwk";
    String urlTokenPart = "/token";
    int defaultKeySize = 2048;

    @BeforeClass
    public static void setUp() throws Exception {

        serverTracker.addServer(builderServer);
        builderServer.startServerUsingExpandedConfiguration("server_configTests.xml");
        SecurityFatHttpUtils.saveServerPorts(builderServer, JWTBuilderConstants.BVT_SERVER_1_PORT_NAME_ROOT);

        // the server's default config contains an invalid value (on purpose), tell the fat framework to ignore it!
        builderServer.addIgnoredErrors(Arrays.asList(JwtMessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE));

    }

    /**
     * <p>
     * Invoke the genericBuilder to:
     * <OL>
     * <LI>Create a JWT token with JWK.
     * <LI>Validate the token contents using values saved in jwtBuilderSettings
     * <LI>Invoke the protected app HelloWorld using the token (under the covers, the RS will invoke the jwkEndpointUrl)
     * <LI>Invoke the JwkEnpointUrl (using http) and validate the contents of the response.
     * </OL>
     * <p>
     * All calls should succeed
     *
     * @throws Exception
     */
    //TODO - hit protected app
    @Mode(TestMode.LITE)
    @Test
    public void JwkEndpointValidationUrlTests_http() throws Exception {

        String builderId = "jwkEnabled";

        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, "testuser");
        settings.put("overrideSettings", testSettings);

        Expectations builderExpectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, builderServer);

        Page builderResponse = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(builderResponse, builderExpectations);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(actions.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, defaultKeySize);

    }

    /**
     * <p>
     * Invoke the JwkEnpointUrl (using http) and an invalid configId (it doesn't exist)
     * <p>
     * The call should receive a not found http status response.
     *
     * @throws Exception
     */
    @Test
    public void JwkEndpointValidationUrlTests_badConfigId() throws Exception {

        String builderId = "badConfig";
        Expectations validateExpectations = new Expectations();
        validateExpectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_NOT_FOUND));
        validateExpectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, JwtMessageConstants.CWWKS6005E_CONFIG_NOT_AVAILABLE + ".*badConfig.*", "Response did not show the expected failure."));
        validateExpectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6005E_CONFIG_NOT_AVAILABLE + ".*badConfig.*", "Message log did not contain an error indicating a problem with the signing key."));

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);

    }

    /**
     * <p>
     * Invoke the genericBuilder to:
     * <OL>
     * <LI>Create a JWT token with JWK created from the server default X509 certificate (jwkEnabled is false on the jwt builder
     * server).
     * <LI>Validate the token contents using values saved in jwtBuilderSettings
     * <LI>Invoke the protected app HelloWorld using the token (under the covers, the RS will invoke the jwkEndpointUrl)
     * <LI>Invoke the JwkEnpointUrl (using http).
     * </OL>
     * <p>
     * All calls should succeed
     *
     * @throws Exception
     */
    //TODO - hit protected app
    @Test
    public void JwkFromServerX509_http() throws Exception {

        String builderId = "jwkFromServerX509";

        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, "testuser");
        settings.put("overrideSettings", testSettings);

        Expectations builderExpectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, builderServer);

        Page builderResponse = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(builderResponse, builderExpectations);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(actions.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, defaultKeySize);
    }

    /**
     * <p>
     * Invoke the genericBuilder to:
     * <OL>
     * <LI>Create a JWT token with JWK created from the X509 certificate from the specified keystore (jwkEnabled is false on the
     * jwt builder server).
     * <LI>Validate the token contents using values saved in jwtBuilderSettings
     * <LI>Invoke the protected app HelloWorld using the token (under the covers, the RS will invoke the jwkEndpointUrl)
     * <LI>Invoke the JwkEnpointUrl (using http).
     * </OL>
     * <p>
     * All calls should succeed
     *
     * @throws Exception
     */
    //TODO - hit protected app
    @Test
    public void JwkFromKeyStoreX509_http() throws Exception {

        String builderId = "jwkFromKeyStoreX509";

        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, "testuser");
        settings.put("overrideSettings", testSettings);

        Expectations builderExpectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, builderServer);

        Page builderResponse = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(builderResponse, builderExpectations);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(actions.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, defaultKeySize);

    }

    /**
     * <p>
     * Invoke the genericBuilder to:
     * <OL>
     * <LI>Create a JWT token with JWK.
     * <LI>Validate the token contents using values saved in jwtBuilderSettings
     * <LI>Invoke the protected app HelloWorld using the token (under the covers, the RS will invoke the jwkEndpointUrl)
     * <LI>Invoke the JwkEnpointUrl (using https) and validate the contents of the response.
     * </OL>
     * <p>
     * All calls should succeed
     *
     * @throws Exception
     */
    //TODO - hit protected app
    @Test
    public void JwkEndpointValidationUrlTests_https() throws Exception {

        String builderId = "jwkEnabled";

        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, "testuser");
        settings.put("overrideSettings", testSettings);

        Expectations builderExpectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, builderServer);

        Page builderResponse = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(builderResponse, builderExpectations);

        String url = buildEndpointUrl_https(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(actions.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, defaultKeySize);

    }

    /**
     * Invoke the jwt feature's token endpoint at h:p/jwt/ibm/api/<configid>/token
     * A token should be generated with the creds of the user supplied during the call.
     * Then use that token to access a protected resource and verify that works.
     *
     * To see the details of how this endpoint gets invoked see
     * JwtBuilderTestUtils.genericBuilder and
     * JwtBuilderTestUtils.invokeJWTTokenEndpoint
     *
     * @throws Exception
     */
    //TODO - hit protected app
    @Mode(TestMode.LITE)
    @Test
    public void TokenEndpointValidationTest_https() throws Exception {

        String builderId = "jwkEnabled";

        String url = buildEndpointUrl_https(builderId, urlTokenPart);

        Expectations tokenExpectations = new Expectations();
        tokenExpectations.addExpectations(CommonExpectations.successfullyReachedUrl(null, url));
        tokenExpectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, "\"token\": \"", "The token was not found in the response"));

        Page tokenResponse = actions.invokeUrlWithBasicAuth(_testName, url, "testuser", "testuserpwd");
        validationUtils.validateResult(tokenResponse, tokenExpectations);

    }

    /**
     * Invoke the jwt feature's token endpoint at h:p/jwt/ibm/api/<configid>/token over http
     * Expect a 404 because https is required.
     */
    @Mode(TestMode.LITE)
    @Test
    public void TokenEndpointTestHttpsEnforced() throws Exception {

        String builderId = "jwkEnabled";

        String url = buildEndpointUrl_http(builderId, urlTokenPart);

        Expectations tokenExpectations = new Expectations();
        tokenExpectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_NOT_FOUND));
        tokenExpectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, JwtMessageConstants.CWWKS6052E_JWT_TRUSTED_ISSUERS_NULL + ".*" + url + ".*", "Response did not show the expected failure."));
        tokenExpectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6052E_JWT_TRUSTED_ISSUERS_NULL + ".*" + url + ".*", "Message log did not contain an error indicating a problem with the signing key."));

        Page tokenResponse = actions.invokeUrlWithBasicAuth(_testName, url, "testuser", "testuserpwd");
        validationUtils.validateResult(tokenResponse, tokenExpectations);

    }

    /**
     * Invoke the jwt feature's token endpoint at h:p/jwt/ibm/api/<configid>/token over http
     * Http should be disallowed, unless an x-forwarded-proto header is present
     * (which indicates it has been forward from a proxy which initially received it as https)
     *
     * Expect a 401 because we have added the x-forward header, so should move past
     * the http check and fail with 401 because there is no auth header in the request.
     *
     */
    @Mode(TestMode.LITE)
    @Test
    public void TokenEndpointTestHttpConfiguration() throws Exception {

        String builderId = "jwkEnabled";

        String url = buildEndpointUrl_http(builderId, urlTokenPart);

        Map<String, String> extraHeaders = new HashMap<String, String>();
        extraHeaders.put("X-Forwarded-Proto", "https");

        Expectations tokenExpectations = new Expectations();
        tokenExpectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_UNAUTHORIZED));
        tokenExpectations.addExpectation(new ResponseUrlExpectation(null, Constants.STRING_EQUALS, url, "Did not reach the expected URL."));
        tokenExpectations.addExpectation(new ResponseMessageExpectation(Constants.STRING_CONTAINS, Constants.UNAUTHORIZED_MESSAGE, "Did not find \"Unauthorized\" in the response message field"));

        Page tokenResponse = actions.invokeUrlWithParametersAndHeaders(_testName, actions.createWebClient(), url, null, extraHeaders);
        validationUtils.validateResult(tokenResponse, tokenExpectations);

    }

    /**
     * <p>
     * Invoke the genericBuilder to:
     * <OL>
     * <LI>Create a JWT token with JWK (signing size is 1024)
     * <LI>Validate the token contents using values saved in jwtBuilderSettings
     * <LI>Invoke the protected app HelloWorld using the token (under the covers, the RS will invoke the jwkEndpointUrl)
     * <LI>Invoke the JwkEnpointUrl (using http) and validate the contents of the response.
     * </OL>
     * <p>
     * All calls should succeed
     *
     * @throws Exception
     */
    //TODO - hit protected app
    @Test
    public void JwkEndpointValidationUrlTests_jwkSigningKeySize_1024() throws Exception {

        String builderId = "jwkEnabled_size_1024";

        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, "testuser");
        settings.put("overrideSettings", testSettings);

        Expectations builderExpectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, builderServer);

        Page builderResponse = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(builderResponse, builderExpectations);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(actions.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, 1024);

    }

    /**
     * <p>
     * Invoke the genericBuilder to:
     * <OL>
     * <LI>Create a JWT token with JWK (signing size is 2048).
     * <LI>Validate the token contents using values saved in jwtBuilderSettings
     * <LI>Invoke the protected app HelloWorld using the token (under the covers, the RS will invoke the jwkEndpointUrl)
     * <LI>Invoke the JwkEnpointUrl (using http) and validate the contents of the response.
     * </OL>
     * <p>
     * All calls should succeed
     *
     * @throws Exception
     */
    //TODO - hit protected app
    @Test
    public void JwkEndpointValidationUrlTests_jwkSigningKeySize_2048() throws Exception {

        String builderId = "jwkEnabled_size_2048";

        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, "testuser");
        settings.put("overrideSettings", testSettings);

        Expectations builderExpectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, builderServer);

        Page builderResponse = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(builderResponse, builderExpectations);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(actions.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, defaultKeySize);

    }

    /**
     * <p>
     * Invoke the genericBuilder to:
     * <OL>
     * <LI>Create a JWT token with JWK (signing size is 4096).
     * <LI>Validate the token contents using values saved in jwtBuilderSettings
     * <LI>Invoke the protected app HelloWorld using the token (under the covers, the RS will invoke the jwkEndpointUrl)
     * <LI>Invoke the JwkEnpointUrl (using http) and validate the contents of the response.
     * </OL>
     * <p>
     * All calls should succeed
     *
     * @throws Exception
     */
    //TODO - hit protected app
    @Test
    public void JwkEndpointValidationUrlTests_jwkSigningKeySize_4096() throws Exception {

        String builderId = "jwkEnabled_size_4096";

        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, "testuser");
        settings.put("overrideSettings", testSettings);

        Expectations builderExpectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, builderServer);

        Page builderResponse = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(builderResponse, builderExpectations);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(actions.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, 4096);

    }

    /**
     * <p>
     * Invoke the genericBuilder to:
     * <OL>
     * <LI>Create a JWT token with JWK.
     * <LI>Validate the token contents using values saved in jwtBuilderSettings
     * <LI>Invoke the protected app HelloWorld using the token (under the covers, the RS will invoke the jwkEndpointUrl)
     * <LI>Invoke the JwkEnpointUrl (using http) and validate the contents of the response.
     * </OL>
     * <p>
     * All calls should succeed - default size will be used
     *
     * @throws Exception
     */
    //TODO - hit protected app
    @Test
    public void JwkEndpointValidationUrlTests_jwkSigningKeySize_invalid() throws Exception {

        String builderId = "jwkEnabled_size_invalid";

        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, "testuser");
        settings.put("overrideSettings", testSettings);

        Expectations builderExpectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, builderServer);

        Page builderResponse = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(builderResponse, builderExpectations);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(actions.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, defaultKeySize);

    }

    /**
     * <p>
     * Invoke the JwkEnpointUrl (using http) and specifying a config that uses HS256.
     * <p>
     * The request should fail because of the incorrect signature algorithm
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwkEndpointValidationUrlTests_sigAlg_HS256() throws Exception {

        String builderId = "jwkEnabled_HS256";

        Expectations validateExpectations = new Expectations();
        validateExpectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_BAD_REQUEST));
        validateExpectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, JwtMessageConstants.CWWKS6039E_JWK_BAD_SIG_ALG, "Output did NOT state that the builder can not be used because the signature algorithm is HS256"));
        validateExpectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6039E_JWK_BAD_SIG_ALG, "Message log did NOT contain an exception indicating that the builder can not be used because the signature algorithm is HS256"));

        String url = buildEndpointUrl_http(builderId, urlJwkPart);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);

    }

    /**
     * <p>
     * Invoke the JwkEnpointUrl (using http) and specifying a config that does NOT have jwkEnabled set to true
     * <p>
     * The request should not fail because jwk now will be created from x509 certificate.
     *
     * @throws Exception
     */
    @Test
    public void JwkEndpointValidationUrlTests_jwkEnabled_false() throws Exception {
        String builderId = "emptyConfig";

        String url = buildEndpointUrl_http(builderId, urlJwkPart);

        Expectations validateExpectations = new Expectations();
        validateExpectations.addExpectations(CommonExpectations.successfullyReachedUrl(null, url));

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        validationUtils.validateSignatureSize(validateResponse, defaultKeySize);
    }

    /**************************************************************************/
    /**
     * <p>
     * Build the requested http url - use the http port, the build id and the endpoint passed in
     *
     * @param builderId
     *            - the config id to use to build the request
     * @param endpoint
     *            - the endpoint to use to build the request
     * @throws Exception
     */
    public String buildEndpointUrl_http(String builderId, String endpoint) throws Exception {

        return SecurityFatHttpUtils.getServerIpUrlBase(builderServer) + urlPart1 + builderId + endpoint;
    }

    /**
     * <p>
     * Build the requested http url - use the http port, the build id and the endpoint passed in
     *
     * @param builderId
     *            - the config id to use to build the request
     * @param endpoint
     *            - the endpoint to use to build the request
     * @throws Exception
     */
    public String buildEndpointUrl_https(String builderId, String endpoint) throws Exception {

        return SecurityFatHttpUtils.getServerIpSecureUrlBase(builderServer) + urlPart1 + builderId + endpoint;
    }

    //    /**
    //     * <p>
    //     * Invoke and validate the rsponse from the specified jwkEndpoint url
    //     * <OL>
    //     * <LI>Invoke the passed in endpoint (all expectations for this step will be checked by genericInvokeEndpoint)
    //     * <LI>If validateResponse is true, we're expecting good results, so check the returned values:
    //     * <UL>
    //     * <LI>compare the kid from the built token against the kid returned by the JWK Validation Endpoint
    //     * <LI>compare the alg from the built token against the alg returned by the JWK Validation Endpoint
    //     * </UL>
    //     * </OL>
    //     *
    //     * @param validateResponse
    //     *            - flag to indocate if we should validate the content of the response
    //     * @param configId
    //     *            - the config id to use to build the request
    //     * @param expectations
    //     *            - settings to validate against
    //     * @param builderToken
    //     *            - the token created by the JWT Builder (will be used to compare against what the JWK Validation Endpoint
    //     *            returned
    //     * @throws Exception
    //     */
    //    public Page invokeJwkEndpointUrl(Expectations expectations, String url, int keySize, String token) throws Exception {
    //
    //        String thisMethod = "invokeJwkEndpointUrl";
    //
    //        return actions.invokeUrl(_testName, url);
    //
    //        //        String jwtToken = response.getText();
    //        //
    //        //        Log.info(thisClass, _testName, "response value: " + jwtToken);
    //        //        JSONObject val = JSONObject.parse(jwtToken);
    //        //
    //        //        String keys = val.get("keys").toString();
    //        //        Log.info(thisClass, _testName, "keys: " + keys);
    //        //
    //        //        JSONArray keyArrays = (JSONArray) val.get("keys");
    //        //        // We are now generating two jwks again by default like before and should get the latest element
    //        //        int cnt = 0;
    //        //        for (Object o : keyArrays) {
    //        //            JSONObject jsonLineItem = (JSONObject) o;
    //        //            if (keyArrays.size() > 1 && cnt < 1) {
    //        //                cnt++;
    //        //                continue;
    //        //            }
    //        //            String kid = (String) jsonLineItem.get(Constants.HEADER_KEY_ID);
    //        //            Log.info(thisClass, _testName, "kid: " + kid);
    //        //            String alg = (String) jsonLineItem.get(Constants.HEADER_ALGORITHM);
    //        //            Log.info(thisClass, _testName, "alg: " + alg);
    //        //
    //        //            String n = (String) jsonLineItem.get("n");
    //        //            String decoded_n = ApacheJsonUtils.fromBase64StringToJsonString(n);
    //        //            Log.info(thisClass, _testName, "n: " + decoded_n);
    //        //            Log.info(thisClass, _testName, "raw size of n: " + decoded_n.length());
    //        //            int calculatedSize = decoded_n.length() * 8;
    //        //            Log.info(thisClass, _testName, "Comparing expected size of signature (" + keySize + ") against size (" + calculatedSize + ") calculated from the token");
    //        //            msgUtils.assertTrueAndLog(thisMethod, "The size of the signature in the token (" + calculatedSize + ")did NOT match the expected size (" + keySize + ")", calculatedSize == keySize);
    //        //
    //        //            if (builderToken != null) {
    //        //                JSONObject jwtHeaderObject = getHeaderJSON(builderToken);
    //        //                String builderKid = (String) jwtHeaderObject.get(Constants.HEADER_KEY_ID);
    //        //                String builderAlg = (String) jwtHeaderObject.get(Constants.HEADER_ALGORITHM);
    //        //                Log.info(thisClass, _testName, "Comparing kid from token and JWK Validation Endpoint");
    //        //                msgUtils.assertTrueAndLog(thisMethod, "The kid (" + builderKid + ") found in the built token does not match the kid (" + kid + ") returned by the JwkValidationEndpoint", builderKid.equals(kid));
    //        //                Log.info(thisClass, _testName, "Comparing alg from token and JWK Validation Endpoint");
    //        //                msgUtils.assertTrueAndLog(thisMethod, "The alg (" + builderAlg + ") found in the built token does not match the alg (" + alg + ") returned by the JwkValidationEndpoint", builderAlg.equals(alg));
    //        //
    //        //            }
    //        //
    //        //        }
    //    }
}