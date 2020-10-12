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
package com.ibm.ws.security.jwt.fat.builder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseMessageExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseStatusExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseUrlExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.jwt.PayloadConstants;
import com.ibm.ws.security.fat.common.jwt.servers.JwtServerInstanceUtils;
import com.ibm.ws.security.fat.common.jwt.utils.JwtKeyTools;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.utils.CommonWaitForAppChecks;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;
import com.ibm.ws.security.jwt.fat.builder.actions.JwtBuilderActions;
import com.ibm.ws.security.jwt.fat.builder.utils.BuilderHelpers;
import com.ibm.ws.security.jwt.fat.builder.utils.JwtBuilderMessageConstants;
import com.ibm.ws.security.jwt.fat.builder.validation.BuilderTestValidationUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will run JWT Endpoint Validation tests.
 *
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JwkEndpointValidationUrlTests extends CommonSecurityFat {

    @Server("com.ibm.ws.security.jwt_fat.builder")
    public static LibertyServer builderServer;
    @Server("com.ibm.ws.security.jwt_fat.builder.rs")
    public static LibertyServer rsServer;
    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    private static final JwtBuilderActions actions = new JwtBuilderActions();
    public static final BuilderTestValidationUtils validationUtils = new BuilderTestValidationUtils();

    String urlPart1 = "jwt/ibm/api/";
    String urlJwkPart = "/jwk";
    String urlTokenPart = "/token";
    int defaultKeySize = 2048;
    boolean UseTokenInHeader = true;
    boolean UseTokenAsParm = false;

    @BeforeClass
    public static void setUp() throws Exception {

        serverTracker.addServer(builderServer);
        builderServer.addInstalledAppForValidation(JWTBuilderConstants.JWT_BUILDER_SERVLET);
        builderServer.startServerUsingExpandedConfiguration("server_configTests.xml", CommonWaitForAppChecks.getSecurityReadyMsgs());
        SecurityFatHttpUtils.saveServerPorts(builderServer, JWTBuilderConstants.BVT_SERVER_1_PORT_NAME_ROOT);

        // the server's default config contains an invalid value (on purpose),
        // tell the fat framework to ignore it!
        builderServer.addIgnoredErrors(Arrays.asList(JwtBuilderMessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE, JwtBuilderMessageConstants.CWWKS6055W_BETA_SIGNATURE_ALGORITHM_USED, JwtBuilderMessageConstants.CWWKS6059W_KEY_MANAGEMENT_KEY_ALIAS_MISSING));

        // start server to run protected app - make sure we can use the JWT
        // Token that we produce
        serverTracker.addServer(rsServer);
        JwtServerInstanceUtils.addHostNameAndAddrToBootstrap(rsServer);
        rsServer.addInstalledAppForValidation(JWTBuilderConstants.HELLOWORLD_APP);
        rsServer.startServerUsingExpandedConfiguration("rs_server_orig.xml", CommonWaitForAppChecks.getSecurityReadyMsgs());
        SecurityFatHttpUtils.saveServerPorts(rsServer, JWTBuilderConstants.BVT_SERVER_2_PORT_NAME_ROOT);

    }

    /**
     * <p>
     * Invoke the genericBuilder to:
     * <OL>
     * <LI>Create a JWT token with JWK.
     * <LI>Validate the token contents
     * <LI>Invoke the protected app HelloWorld using the token (under the
     * covers, the RS will invoke the jwkEndpointUrl)
     * <LI>Invoke the JwkEnpointUrl (using http) and validate the contents of
     * the response.
     * </OL>
     * <p>
     * All calls should succeed
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwkEndpointValidationUrlTests_http() throws Exception {

        String builderId = "jwkEnabled";

        // build a jwt token with the "default" test claims
        Page builderResponse = buildJwtForEndpointValidationTests(builderId);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, defaultKeySize);

        // use the token to access the protected app - one last check to ensure
        // that the token is valid
        invokeAndValidateProtectedApp(UseTokenInHeader, builderResponse, builderId);

    }

    /**
     * <p>
     * Invoke the JwkEnpointUrl (using http) and an invalid configId (it doesn't
     * exist)
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
        validateExpectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, JwtBuilderMessageConstants.CWWKS6005E_CONFIG_NOT_AVAILABLE + ".*badConfig.*", "Response did not show the expected failure."));
        validateExpectations.addExpectation(new ServerMessageExpectation(builderServer, JwtBuilderMessageConstants.CWWKS6005E_CONFIG_NOT_AVAILABLE + ".*badConfig.*", "Message log did not contain an error indicating a problem with the signing key."));

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);

    }

    /**
     * <p>
     * Invoke the genericBuilder to:
     * <OL>
     * <LI>Create a JWT token with JWK created from the server default X509
     * certificate (jwkEnabled is false on the jwt builder server).
     * <LI>Validate the token contents
     * <LI>Invoke the protected app HelloWorld using the token (under the
     * covers, the RS will invoke the jwkEndpointUrl)
     * <LI>Invoke the JwkEnpointUrl (using http).
     * </OL>
     * <p>
     * All calls should succeed
     *
     * @throws Exception
     */
    @Test
    public void JwkFromServerX509_http() throws Exception {

        String builderId = "jwkFromServerX509";

        // build a jwt token with the "default" test claims
        Page builderResponse = buildJwtForEndpointValidationTests(builderId);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, defaultKeySize);
        // use the token to access the protected app - one last check to ensure
        // that the token is valid
        invokeAndValidateProtectedApp(UseTokenAsParm, builderResponse, builderId);

    }

    /**
     * <p>
     * Invoke the genericBuilder to:
     * <OL>
     * <LI>Create a JWT token with JWK created from the X509 certificate from
     * the specified keystore (jwkEnabled is false on the jwt builder server).
     * <LI>Validate the token contents
     * <LI>Invoke the protected app HelloWorld using the token (under the
     * covers, the RS will invoke the jwkEndpointUrl)
     * <LI>Invoke the JwkEnpointUrl (using http).
     * </OL>
     * <p>
     * All calls should succeed
     *
     * @throws Exception
     */
    @Test
    public void JwkFromKeyStoreX509_http() throws Exception {

        String builderId = "jwkFromKeyStoreX509";

        // build a jwt token with the "default" test claims
        Page builderResponse = buildJwtForEndpointValidationTests(builderId);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, defaultKeySize);

        // use the token to access the protected app - one last check to ensure
        // that the token is valid
        invokeAndValidateProtectedApp(UseTokenInHeader, builderResponse, builderId);

    }

    /**
     * <p>
     * Invoke the genericBuilder to:
     * <OL>
     * <LI>Create a JWT token with JWK.
     * <LI>Validate the token contents
     * <LI>Invoke the protected app HelloWorld using the token (under the
     * covers, the RS will invoke the jwkEndpointUrl)
     * <LI>Invoke the JwkEnpointUrl (using https) and validate the contents of
     * the response.
     * </OL>
     * <p>
     * All calls should succeed
     *
     * @throws Exception
     */
    @Test
    public void JwkEndpointValidationUrlTests_https() throws Exception {

        String builderId = "jwkEnabled";

        // build a jwt token with the "default" test claims
        Page builderResponse = buildJwtForEndpointValidationTests(builderId);

        String url = buildEndpointUrl_https(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, defaultKeySize);

        // use the token to access the protected app - one last check to ensure
        // that the token is valid
        invokeAndValidateProtectedApp(UseTokenAsParm, builderResponse, builderId);

    }

    /**
     * Invoke the jwt feature's token endpoint at
     * h:p/jwt/ibm/api/<configid>/token A token should be generated with the
     * creds of the user supplied during the call. Then use that token to access
     * a protected resource and verify that works.
     *
     * To see the details of how this endpoint gets invoked see
     * JwtBuilderTestUtils.genericBuilder and
     * JwtBuilderTestUtils.invokeJWTTokenEndpoint
     *
     * @throws Exception
     */
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

        // use the token to access the protected app - one last check to ensure
        // that the token is valid
        String protectedApp = SecurityFatHttpUtils.getServerUrlBase(rsServer) + "helloworld/rest/helloworld_jwkEnabled";

        String tokenEndpointOutput = WebResponseUtils.getResponseText(tokenResponse); // {"token":
                                                                                      // "mess"}
        Log.info(thisClass, _testName, "*** RESPONSE TEXT: " + tokenEndpointOutput);
        String jwtToken = tokenEndpointOutput.replace("{", "").replace("}", "").replace("token", "").replace("\"", "").replace(" ", "").replace(":", "");
        Log.info(thisClass, _testName, "*** RESPONSE TOKEN: " + jwtToken);

        Page appResponse = actions.invokeUrlWithBearerTokenUsingPost(_testName, protectedApp, jwtToken);

        Expectations appExpectations = new Expectations();
        appExpectations.addExpectations(CommonExpectations.successfullyReachedUrl(protectedApp));
        appExpectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, JWTBuilderConstants.ACCESS_TOKEN + "=" + jwtToken, "Protected app output did NOT contain access_token=<generated JWT Token>."));
        appExpectations.addExpectation(new ResponseFullExpectation(Constants.STRING_CONTAINS, JWTBuilderConstants.WSPRINCIPAL + "testuser", "Protected app output did NOT contain WSPrincipal:testuser."));
        validationUtils.validateResult(appResponse, appExpectations);
    }

    /**
     * Invoke the jwt feature's token endpoint at
     * h:p/jwt/ibm/api/<configid>/token over http Expect a 404 because https is
     * required.
     */
    @Mode(TestMode.LITE)
    @Test
    public void TokenEndpointTestHttpsEnforced() throws Exception {

        String builderId = "jwkEnabled";

        String url = buildEndpointUrl_http(builderId, urlTokenPart);

        Expectations tokenExpectations = new Expectations();
        tokenExpectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_NOT_FOUND));
        tokenExpectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, JwtBuilderMessageConstants.CWWKS6052E_JWT_TRUSTED_ISSUERS_NULL + ".*" + url + ".*", "Response did not show the expected failure."));
        tokenExpectations.addExpectation(new ServerMessageExpectation(builderServer, JwtBuilderMessageConstants.CWWKS6052E_JWT_TRUSTED_ISSUERS_NULL + ".*" + url + ".*", "Message log did not contain an error indicating a problem with the signing key."));

        Page tokenResponse = actions.invokeUrlWithBasicAuth(_testName, url, "testuser", "testuserpwd");
        validationUtils.validateResult(tokenResponse, tokenExpectations);

    }

    /**
     * Invoke the jwt feature's token endpoint at
     * h:p/jwt/ibm/api/<configid>/token over http Http should be disallowed,
     * unless an x-forwarded-proto header is present (which indicates it has
     * been forward from a proxy which initially received it as https)
     *
     * Expect a 401 because we have added the x-forward header, so should move
     * past the http check and fail with 401 because there is no auth header in
     * the request.
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
     * <LI>Validate the token contents
     * <LI>Invoke the protected app HelloWorld using the token (under the
     * covers, the RS will invoke the jwkEndpointUrl)
     * <LI>Invoke the JwkEnpointUrl (using http) and validate the contents of
     * the response.
     * </OL>
     * <p>
     * All calls should succeed
     *
     * @throws Exception
     */
    @Test
    public void JwkEndpointValidationUrlTests_jwkSigningKeySize_1024() throws Exception {

        String builderId = "jwkEnabled_size_1024";

        // build a jwt token with the "default" test claims
        Page builderResponse = buildJwtForEndpointValidationTests(builderId);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, 1024);

        // use the token to access the protected app - one last check to ensure
        // that the token is valid
        invokeAndValidateProtectedApp(UseTokenAsParm, builderResponse, builderId.replace("jwkEnabled_", ""));

    }

    /**
     * <p>
     * Invoke the genericBuilder to:
     * <OL>
     * <LI>Create a JWT token with JWK (signing size is 2048).
     * <LI>Validate the token contents
     * <LI>Invoke the protected app HelloWorld using the token (under the
     * covers, the RS will invoke the jwkEndpointUrl)
     * <LI>Invoke the JwkEnpointUrl (using http) and validate the contents of
     * the response.
     * </OL>
     * <p>
     * All calls should succeed
     *
     * @throws Exception
     */
    @Test
    public void JwkEndpointValidationUrlTests_jwkSigningKeySize_2048() throws Exception {

        String builderId = "jwkEnabled_size_2048";

        // build a jwt token with the "default" test claims
        Page builderResponse = buildJwtForEndpointValidationTests(builderId);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, defaultKeySize);

        // use the token to access the protected app - one last check to ensure
        // that the token is valid
        invokeAndValidateProtectedApp(UseTokenInHeader, builderResponse, builderId.replace("jwkEnabled_", ""));

    }

    /**
     * <p>
     * Invoke the genericBuilder to:
     * <OL>
     * <LI>Create a JWT token with JWK (signing size is 4096).
     * <LI>Validate the token contents
     * <LI>Invoke the protected app HelloWorld using the token (under the
     * covers, the RS will invoke the jwkEndpointUrl)
     * <LI>Invoke the JwkEnpointUrl (using http) and validate the contents of
     * the response.
     * </OL>
     * <p>
     * All calls should succeed
     *
     * @throws Exception
     */
    @Test
    public void JwkEndpointValidationUrlTests_jwkSigningKeySize_4096() throws Exception {

        String builderId = "jwkEnabled_size_4096";

        // build a jwt token with the "default" test claims
        Page builderResponse = buildJwtForEndpointValidationTests(builderId);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, 4096);

        // use the token to access the protected app - one last check to ensure
        // that the token is valid
        invokeAndValidateProtectedApp(UseTokenInHeader, builderResponse, builderId.replace("jwkEnabled_", ""));

    }

    /**
     * <p>
     * Invoke the genericBuilder to:
     * <OL>
     * <LI>Create a JWT token with JWK.
     * <LI>Validate the token contents
     * <LI>Invoke the protected app HelloWorld using the token (under the
     * covers, the RS will invoke the jwkEndpointUrl)
     * <LI>Invoke the JwkEnpointUrl (using http) and validate the contents of
     * the response.
     * </OL>
     * <p>
     * All calls should succeed - default size will be used
     *
     * @throws Exception
     */
    @Test
    public void JwkEndpointValidationUrlTests_jwkSigningKeySize_invalid() throws Exception {

        String builderId = "jwkEnabled_size_invalid";

        // build a jwt token with the "default" test claims
        Page builderResponse = buildJwtForEndpointValidationTests(builderId);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, defaultKeySize);

        // use the token to access the protected app - one last check to ensure
        // that the token is valid
        invokeAndValidateProtectedApp(UseTokenAsParm, builderResponse, builderId.replace("jwkEnabled_", ""));

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
        validateExpectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, JwtBuilderMessageConstants.CWWKS6039E_JWK_BAD_SIG_ALG, "Output did NOT state that the builder can not be used because the signature algorithm is HS256"));
        validateExpectations.addExpectation(new ServerMessageExpectation(builderServer, JwtBuilderMessageConstants.CWWKS6039E_JWK_BAD_SIG_ALG, "Message log did NOT contain an exception indicating that the builder can not be used because the signature algorithm is HS256"));

        String url = buildEndpointUrl_http(builderId, urlJwkPart);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);

    }

    /**
     * <p>
     * Invoke the JwkEnpointUrl (using http) and specifying a config that uses HS384
     * <p>
     * The request should fail because of the incorrect signature algorithm
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwkEndpointValidationUrlTests_sigAlg_HS384() throws Exception {

        String builderId = "jwkEnabled_HS384";

        Expectations validateExpectations = new Expectations();
        validateExpectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_BAD_REQUEST));
        validateExpectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, JwtBuilderMessageConstants.CWWKS6039E_JWK_BAD_SIG_ALG, "Output did NOT state that the builder can not be used because the signature algorithm is HS384"));
        validateExpectations.addExpectation(new ServerMessageExpectation(builderServer, JwtBuilderMessageConstants.CWWKS6039E_JWK_BAD_SIG_ALG, "Message log did NOT contain an exception indicating that the builder can not be used because the signature algorithm is HS384"));

        String url = buildEndpointUrl_http(builderId, urlJwkPart);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);

    }

    /**
     * <p>
     * Invoke the JwkEnpointUrl (using http) and specifying a config that uses HS512
     * <p>
     * The request should fail because of the incorrect signature algorithm
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwkEndpointValidationUrlTests_sigAlg_HS512() throws Exception {

        String builderId = "jwkEnabled_HS512";

        Expectations validateExpectations = new Expectations();
        validateExpectations.addExpectation(new ResponseStatusExpectation(HttpServletResponse.SC_BAD_REQUEST));
        validateExpectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, JwtBuilderMessageConstants.CWWKS6039E_JWK_BAD_SIG_ALG, "Output did NOT state that the builder can not be used because the signature algorithm is HS512"));
        validateExpectations.addExpectation(new ServerMessageExpectation(builderServer, JwtBuilderMessageConstants.CWWKS6039E_JWK_BAD_SIG_ALG, "Message log did NOT contain an exception indicating that the builder can not be used because the signature algorithm is HS512"));

        String url = buildEndpointUrl_http(builderId, urlJwkPart);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);

    }

    /**
     * <p>
     * Invoke the JwkEnpointUrl (using http) and specifying a config that does
     * NOT have jwkEnabled set to true
     * <p>
     * The request should not fail because jwk now will be created from x509
     * certificate.
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

    /**
     * <p>
     * Invoke the JwkEnpointUrl (using http) and specifying a config has the signature algorithm set to RS384 and jwkEnabled set
     * to true
     * <p>
     * The request should succeed - the RS384 alg should be set and the size should be 2048
     *
     * @throws Exception
     */
    @Test
    public void JwkEndpointValidationUrlTests_sigAlg_RS384() throws Exception {

        String builderId = "jwkEnabled_RS384";

        // build a jwt token with the "default" test claims
        Page builderResponse = buildJwtForEndpointValidationTests(builderId, JWTBuilderConstants.SIGALG_RS384);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, defaultKeySize);

        // TODO
        //        // use the token to access the protected app - one last check to ensure that the token is valid
        //        invokeAndValidateProtectedApp(UseTokenInHeader, builderResponse, builderId.replace("jwkEnabled_", ""));

    }

    /**
     * <p>
     * Invoke the JwkEnpointUrl (using http) and specifying a config has the signature algorithm set to RS512 and jwkEnabled set
     * to true
     * <p>
     * The request should succeed - the RS512 alg should be set and the size should be 2048
     *
     * @throws Exception
     */
    @Test
    public void JwkEndpointValidationUrlTests_sigAlg_RS512() throws Exception {

        String builderId = "jwkEnabled_RS512";

        // build a jwt token with the "default" test claims
        Page builderResponse = buildJwtForEndpointValidationTests(builderId, JWTBuilderConstants.SIGALG_RS512);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, defaultKeySize);

        // TODO
        //        // use the token to access the protected app - one last check to ensure that the token is valid
        //        invokeAndValidateProtectedApp(UseTokenInHeader, builderResponse, builderId.replace("jwkEnabled_", ""));

    }

    /**
     * <p>
     * Invoke the JwkEnpointUrl (using http) and specifying a config has the signature algorithm set to ES256 and jwkEnabled set
     * to true
     * <p>
     * The request should succeed - the ES256 alg should be set and the crv should be P-256
     *
     * @throws Exception
     */
    @Test
    public void JwkEndpointValidationUrlTests_sigAlg_ES256() throws Exception {

        String builderId = "jwkEnabled_ES256";

        // build a jwt token with the "default" test claims
        Page builderResponse = buildJwtForEndpointValidationTests(builderId, JWTBuilderConstants.SIGALG_ES256);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the crv is correct
        validationUtils.validateCurve(validateResponse, "P-256");
        // extra validation - make sure that the signature algorithm is correct
        validationUtils.validateSigAlg(validateResponse, JWTBuilderConstants.SIGALG_ES256);

        // TODO
        //        // use the token to access the protected app - one last check to ensure that the token is valid
        //        invokeAndValidateProtectedApp(UseTokenInHeader, builderResponse, builderId.replace("jwkEnabled_", ""));

    }

    /**
     * <p>
     * Invoke the JwkEnpointUrl (using http) and specifying a config has the signature algorithm set to ES384 and jwkEnabled set
     * to true
     * <p>
     * The request should succeed - the ES384 alg should be set and the crv should be P-384
     *
     * @throws Exception
     */
    @Test
    public void JwkEndpointValidationUrlTests_sigAlg_ES384() throws Exception {

        String builderId = "jwkEnabled_ES384";

        // build a jwt token with the "default" test claims
        Page builderResponse = buildJwtForEndpointValidationTests(builderId, JWTBuilderConstants.SIGALG_ES384);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the crv is correct
        validationUtils.validateCurve(validateResponse, "P-384");
        // extra validation - make sure that the signature algorithm is correct
        validationUtils.validateSigAlg(validateResponse, JWTBuilderConstants.SIGALG_ES384);

        // TODO
        //        // use the token to access the protected app - one last check to ensure that the token is valid
        //        invokeAndValidateProtectedApp(UseTokenInHeader, builderResponse, builderId.replace("jwkEnabled_", ""));

    }

    /**
     * <p>
     * Invoke the JwkEnpointUrl (using http) and specifying a config has the signature algorithm set to ES512 and jwkEnabled set
     * to true
     * <p>
     * The request should succeed - the ES512 alg should be set and the crv should be P-521
     *
     * @throws Exception
     */
    @Test
    public void JwkEndpointValidationUrlTests_sigAlg_ES512() throws Exception {

        String builderId = "jwkEnabled_ES512";

        // build a jwt token with the "default" test claims
        Page builderResponse = buildJwtForEndpointValidationTests(builderId, JWTBuilderConstants.SIGALG_ES512);

        String url = buildEndpointUrl_http(builderId, urlJwkPart);
        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the crv is correct
        validationUtils.validateCurve(validateResponse, "P-521");
        // extra validation - make sure that the signature algorithm is correct
        validationUtils.validateSigAlg(validateResponse, JWTBuilderConstants.SIGALG_ES512);

        // TODO
        //        // use the token to access the protected app - one last check to ensure that the token is valid
        //        invokeAndValidateProtectedApp(UseTokenInHeader, builderResponse, builderId.replace("jwkEnabled_", ""));

    }

    //    @Test
    public void JwkEndpointValidationUrlTests_sigAlg_PS256() throws Exception {

        String builderId = "jwkEnabled_PS256";
        String url = buildEndpointUrl_http(builderId, urlJwkPart);

        // PS sig alg only valid with java 11 or above
        if (JavaInfo.JAVA_VERSION >= 11) {

            // build a jwt token with the "default" test claims
            Page builderResponse = buildJwtForEndpointValidationTests(builderId, JWTBuilderConstants.SIGALG_PS256);

            // create validation endpoint expectations from the built token
            Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

            Page validateResponse = actions.invokeUrl(_testName, url);
            validationUtils.validateResult(validateResponse, validateExpectations);
            // TODO - what specifically needs to be validated?
            // extra validation - make sure that the signature size is correct
            validationUtils.validateSignatureSize(validateResponse, defaultKeySize);

            // TODO
            //        // use the token to access the protected app - one last check to ensure that the token is valid
            //        invokeAndValidateProtectedApp(UseTokenInHeader, builderResponse, builderId.replace("jwkEnabled_", ""));

        } else {
            // TODO check for the proper error message about PS sig alg and less than java 11
            Page validateResponse = actions.invokeUrl(_testName, url);
            // check for messages
        }
    }

    //    @Test
    public void JwkEndpointValidationUrlTests_sigAlg_PS384() throws Exception {

        String builderId = "jwkEnabled_PS384";
        String url = buildEndpointUrl_http(builderId, urlJwkPart);

        // PS sig alg only valid with java 11 or above
        if (JavaInfo.JAVA_VERSION >= 11) {

            // build a jwt token with the "default" test claims
            Page builderResponse = buildJwtForEndpointValidationTests(builderId, JWTBuilderConstants.SIGALG_PS384);

            // create validation endpoint expectations from the built token
            Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

            Page validateResponse = actions.invokeUrl(_testName, url);
            validationUtils.validateResult(validateResponse, validateExpectations);
            // extra validation - make sure that the signature size is correct
            validationUtils.validateSignatureSize(validateResponse, defaultKeySize);

            // TODO
            //        // use the token to access the protected app - one last check to ensure that the token is valid
            //        invokeAndValidateProtectedApp(UseTokenInHeader, builderResponse, builderId.replace("jwkEnabled_", ""));

        } else {
            // TODO check for the proper error message about PS sig alg and less than java 11
            Page validateResponse = actions.invokeUrl(_testName, url);
            // check for messages
        }

    }

    //    @Test
    public void JwkEndpointValidationUrlTests_sigAlg_PS512() throws Exception {

        String builderId = "jwkEnabled_PS512";
        String url = buildEndpointUrl_http(builderId, urlJwkPart);

        // PS sig alg only valid with java 11 or above
        if (JavaInfo.JAVA_VERSION >= 11) {

            // build a jwt token with the "default" test claims
            Page builderResponse = buildJwtForEndpointValidationTests(builderId, JWTBuilderConstants.SIGALG_PS512);

            // create validation endpoint expectations from the built token
            Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url);

            Page validateResponse = actions.invokeUrl(_testName, url);
            validationUtils.validateResult(validateResponse, validateExpectations);
            // extra validation - make sure that the signature size is correct
            validationUtils.validateSignatureSize(validateResponse, defaultKeySize);

            // TODO
            //        // use the token to access the protected app - one last check to ensure that the token is valid
            //        invokeAndValidateProtectedApp(UseTokenInHeader, builderResponse, builderId.replace("jwkEnabled_", ""));

        } else {
            // TODO check for the proper error message about PS sig alg and less than java 11
            Page validateResponse = actions.invokeUrl(_testName, url);
            // check for messages
        }

    }

    /**
     * <p>
     * Invoke the JwkEnpointUrl (using http) and specifying a config that has encryption enabled using an RS256 key
     *
     * <p>
     * The request should succeed -
     *
     * @throws Exception
     */
    @Test
    public void JwkEndpointValidationUrlTests_encrypt_RS256() throws Exception {

        String builderId = "key_encrypt_good_RS256";
        String url = buildEndpointUrl_http(builderId, urlJwkPart);

        // build a jwt token with the "default" test claims (need to validate different info for an encrypted token)
        Page builderResponse = buildEncryptedJwtForEndpointValidationTests(builderId, JWTBuilderConstants.DEFAULT_KEY_MGMT_KEY_ALG, JWTBuilderConstants.DEFAULT_CONTENT_ENCRYPT_ALG);

        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url, JwtKeyTools.getComplexPrivateKeyForSigAlg(builderServer, JWTBuilderConstants.SIGALG_RS256));

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, 4096);

        // TODO
        //        // use the token to access the protected app - one last check to ensure that the token is valid
        //        invokeAndValidateProtectedApp(UseTokenInHeader, builderResponse, builderId.replace("jwkEnabled_", ""));

    }

    @Test
    public void JwkEndpointValidationUrlTests_encrypt_RS384() throws Exception {

        String builderId = "key_encrypt_good_RS384";
        String url = buildEndpointUrl_http(builderId, urlJwkPart);

        // build a jwt token with the "default" test claims (need to validate different info for an encrypted token)
        Page builderResponse = buildEncryptedJwtForEndpointValidationTests(builderId, JWTBuilderConstants.DEFAULT_KEY_MGMT_KEY_ALG, JWTBuilderConstants.DEFAULT_CONTENT_ENCRYPT_ALG);

        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url, JwtKeyTools.getComplexPrivateKeyForSigAlg(builderServer, JWTBuilderConstants.SIGALG_RS384));

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, 4096);

        // TODO
        //        // use the token to access the protected app - one last check to ensure that the token is valid
        //        invokeAndValidateProtectedApp(UseTokenInHeader, builderResponse, builderId.replace("jwkEnabled_", ""));

    }

    @Test
    public void JwkEndpointValidationUrlTests_encrypt_RS512() throws Exception {

        String builderId = "key_encrypt_good_RS512";
        String url = buildEndpointUrl_http(builderId, urlJwkPart);

        // build a jwt token with the "default" test claims (need to validate different info for an encrypted token)
        Page builderResponse = buildEncryptedJwtForEndpointValidationTests(builderId, JWTBuilderConstants.DEFAULT_KEY_MGMT_KEY_ALG, JWTBuilderConstants.DEFAULT_CONTENT_ENCRYPT_ALG);

        // create validation endpoint expectations from the built token
        Expectations validateExpectations = BuilderHelpers.createGoodValidationEndpointExpectations(BuilderHelpers.extractJwtTokenFromResponse(builderResponse, JWTBuilderConstants.BUILT_JWT_TOKEN), url, JwtKeyTools.getComplexPrivateKeyForSigAlg(builderServer, JWTBuilderConstants.SIGALG_RS512));

        Page validateResponse = actions.invokeUrl(_testName, url);
        validationUtils.validateResult(validateResponse, validateExpectations);
        // extra validation - make sure that the signature size is correct
        validationUtils.validateSignatureSize(validateResponse, 4096);

        // TODO
        //        // use the token to access the protected app - one last check to ensure that the token is valid
        //        invokeAndValidateProtectedApp(UseTokenInHeader, builderResponse, builderId.replace("jwkEnabled_", ""));

    }

    /**************************************************************************/
    /**
     * <p>
     * Build the requested http url - use the http port, the build id and the
     * endpoint passed in
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
     * Build the requested http url - use the http port, the build id and the
     * endpoint passed in
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

    public void invokeAndValidateProtectedApp(Boolean tokenInHeader, Page builderResponse, String appNameExtension) throws Exception {

        String protectedApp = SecurityFatHttpUtils.getServerUrlBase(rsServer) + "helloworld/rest/helloworld_" + appNameExtension;

        Page appResponse = null;
        if (tokenInHeader) {
            appResponse = actions.invokeProtectedAppWithJwtTokenInHeader(_testName, builderResponse, protectedApp);
        } else {
            appResponse = actions.invokeProtectedAppWithJwtTokenAsParm(_testName, builderResponse, protectedApp);
        }
        Expectations appExpectations = new Expectations();
        appExpectations.addExpectations(CommonExpectations.successfullyReachedUrl(protectedApp));
        validationUtils.validateResult(appResponse, appExpectations);
    }

    /**
     * Many of the tests build a token with all of the same data - this is a
     * helper method to do that for them
     *
     * @param builderId
     *            - the id/config to use to build a token
     * @return - return a response with the token
     * @throws Exception
     */
    public Page buildJwtForEndpointValidationTests(String builderId) throws Exception {
        return buildJwtForEndpointValidationTests(builderId, JWTBuilderConstants.SIGALG_RS256);
    }

    public Page buildJwtForEndpointValidationTests(String builderId, String alg) throws Exception {

        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        JSONObject testSettings = new JSONObject();
        testSettings.put(PayloadConstants.SUBJECT, "testuser");
        expectationSettings.put("overrideSettings", testSettings);
        if (alg != null) {
            expectationSettings.put("alg", alg);
        }

        Expectations builderExpectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page builderResponse = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(builderResponse, builderExpectations);

        return builderResponse;
    }

    public Page buildEncryptedJwtForEndpointValidationTests(String builderId, String keyMgmtKeyAlg, String contentEncryptAlg) throws Exception {

        JSONObject expectationSettings = BuilderHelpers.setDefaultClaimsWithEncryption(builderId, keyMgmtKeyAlg, contentEncryptAlg);
        JSONObject testSettings = new JSONObject();
        testSettings.put(PayloadConstants.SUBJECT, "testuser");
        expectationSettings.put("overrideSettings", testSettings);

        Expectations builderExpectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page builderResponse = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(builderResponse, builderExpectations);

        return builderResponse;
    }

}
