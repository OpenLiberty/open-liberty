/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.tests;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jwt.NumericDate;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.jwt.JWTTokenBuilder;
import com.ibm.ws.security.fat.common.jwt.utils.JwtTokenBuilderUtils;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.HttpUtils.HTTPRequestMethod;
import io.openliberty.security.jakartasec.fat.commonTests.CommonAnnotatedSecurityTests;
import io.openliberty.security.jakartasec.fat.configs.TestConfigMaps;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.MessageConstants;
import io.openliberty.security.jakartasec.fat.utils.PayloadConstants;
import io.openliberty.security.jakartasec.fat.utils.ServletMessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;

/**
 * Test that the runtime processing the response from the token endpoint with an app using @OpenIdAuthenticationMechanismDefinition
 * does so correctly.  THese tests will make sure that the token returned from the token endpoint will be validated properly.
 * We'll use a test token endpoint and have it return tokens created by the test cases - these tokens will include claims with
 * invalid values, or missing claims.
 *
 */

/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class TokenValidationTests extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = TokenValidationTests.class;

    protected static ShrinkWrapHelpers swh = null;

    @Server("jakartasec-3.0_fat.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.tokenValidation.jwt.rp")
    public static LibertyServer rpJwtServer;
    @Server("jakartasec-3.0_fat.tokenValidation.opaque.rp")
    public static LibertyServer rpOpaqueServer;

    public static LibertyServer rpServer;

    @ClassRule
    public static RepeatTests repeat = createTokenTypeRepeats();

    public static final SecurityFatHttpUtils secHttpUtils = new SecurityFatHttpUtils();
    public static final JwtTokenBuilderUtils tokenBuilderHelpers = new JwtTokenBuilderUtils();

    int Default_expires_in = 7199;
    int Omit_expires_in = 9999;
    String Default_token_type = "Bearer";
    String Default_scope = "openid profile";
    String Default_refresh_token = "21MhoIC95diaQo9tb5UpFBDFlHh45NixhcKkCwRipszH6WIzKz";

    Expectations tokenResponseExpectations = null;

    @BeforeClass
    public static void setUp() throws Exception {

        // write property that is used to configure the OP to generate JWT or Opaque tokens
        rpServer = setTokenTypeInBootstrap(opServer, rpJwtServer, rpOpaqueServer);

        // Add servers to server trackers that will be used to clean servers up and prevent servers
        // from being restored at the end of each test (so far, the tests are not reconfiguring the servers)
        updateTrackers(opServer, rpServer, false);

        List<String> waitForMsgs = null;
        opServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(opServer, Constants.BVT_SERVER_1_PORT_NAME_ROOT);
        opHttpBase = "http://localhost:" + opServer.getBvtPort();
        opHttpsBase = "https://localhost:" + opServer.getBvtSecurePort();

        rpServer.startServerUsingExpandedConfiguration("server_tokenValidation.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(rpServer, Constants.BVT_SERVER_2_PORT_NAME_ROOT);

        rpHttpBase = "http://localhost:" + rpServer.getBvtPort();
        rpHttpsBase = "https://localhost:" + rpServer.getBvtSecurePort();

        opServer.addIgnoredErrors(Arrays.asList(MessageConstants.CWWKS1617E_USERINFO_REQUEST_BAD_TOKEN));
        deployMyApps();

    }

    /**
     * Deploy the apps that this test class uses
     *
     * @throws Exception
     */
    public static void deployMyApps() throws Exception {

        swh = new ShrinkWrapHelpers(opHttpBase, opHttpsBase, rpHttpBase, rpHttpsBase);
        // deploy the apps that are defined 100% by the source code tree
        swh.deployConfigurableTestApps(rpServer, "TokenTestAppServlet.war", "TokenTestAppServlet.war",
                                       buildUpdatedConfigMap("TokenTestAppServlet", "OP1", false),
                                       "oidc.tokenTest.servlets",
                                       "oidc.client.base.*");

    }

    public static Map<String, Object> buildUpdatedConfigMap(String appName, String provider, boolean useNonce) throws Exception {

        // init the map with the provider info that the app should use
        Map<String, Object> testPropMap = TestConfigMaps.getProviderUri(opHttpsBase, provider);

        if (useNonce) {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getUseNonceExpressionTrue());
        } else {
            testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getUseNonceExpressionFalse());
        }
        testPropMap = TestConfigMaps.mergeMaps(testPropMap, TestConfigMaps.getTestTokenSaveEndpoint(rpHttpsBase));
        testPropMap = TestConfigMaps.mergeMaps(testPropMap, getIssuer());

        Map<String, Object> updatedMap = buildUpdatedConfigMap(opServer, rpServer, appName, "allValues.openIdConfig.properties", testPropMap);

        return updatedMap;

    }

    public static Map<String, Object> getIssuer() throws Exception {

        Map<String, Object> updatedMap = new HashMap<String, Object>();
        updatedMap.put(Constants.ISSUER, rpHttpBase + "/TokenEndpointServlet");
        return updatedMap;

    }

    /**
     * Initializes a security test JWTTokenBuilder object with the default values that will match the values
     * in the @OpenIdAuthenticationMechanismDefinition annotation in the test apps.
     * The test cases can override individual values to cause failures as needed
     *
     * @return
     * @throws Exception
     */
    public JWTTokenBuilder initTokenBuilder(String... skipSettingKeys) throws Exception {

        rspValues.setUseNonce(false);

        JWTTokenBuilder builder = new JWTTokenBuilder();
        if (!Arrays.asList(skipSettingKeys).contains(PayloadConstants.PAYLOAD_ISSUER)) {
            builder.setIssuer(rpHttpBase + "/TokenEndpointServlet");
        }
        if (!Arrays.asList(skipSettingKeys).contains(PayloadConstants.PAYLOAD_ISSUED_AT_TIME_IN_SECS)) {
            builder.setIssuedAtToNow();
        }
        if (!Arrays.asList(skipSettingKeys).contains(PayloadConstants.PAYLOAD_EXPIRATION_TIME_IN_SECS)) {
            builder.setExpirationTimeMinutesIntheFuture(120);
        }
        if (!Arrays.asList(skipSettingKeys).contains(PayloadConstants.PAYLOAD_SCOPE)) {
            builder.setScope("openid profile");
        }
        if (!Arrays.asList(skipSettingKeys).contains(PayloadConstants.PAYLOAD_SUBJECT)) {
            builder.setSubject("testuser");
        }
        if (!Arrays.asList(skipSettingKeys).contains(PayloadConstants.PAYLOAD_REALMNAME)) {
            builder.setRealmName("BasicRealm");
        }
        if (!Arrays.asList(skipSettingKeys).contains(PayloadConstants.PAYLOAD_TOKEN_TYPE)) {
            builder.setTokenType("Bearer");
        }
        builder.setAlorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
        builder.setHSAKey("mySharedKeyNowHasToBeLongerStrongerAndMoreSecureAndForHS512EvenLongerToBeStronger");
        builder.setKeyId("");
        builder.setClaim("token_src", "testcase builder");
        builder.setClaim("groupIds", Arrays.asList("all"));
        if (!Arrays.asList(skipSettingKeys).contains(PayloadConstants.PAYLOAD_AUDIENCE)) {
            builder.setAudience("client_1");
        }
        builder.setClaim(PayloadConstants.PAYLOAD_AT_HASH, "dummy_hash_value");

        return builder;
    }

    /**
     * Build and save a JWT token in the test token endpoint app.
     *
     * @param builder
     * @throws Exception
     */
    public void saveTokenEndpointToken(JWTTokenBuilder builder) throws Exception {
        saveTokenEndpointToken(builder, Default_expires_in, Default_token_type, Default_scope, Default_refresh_token);
    }

    public void saveTokenEndpointToken(JWTTokenBuilder builder, int expires_in, String token_type, String scope, String refresh_token) throws Exception {

        String jwtToken = builder.buildAsIs();

        saveTokenEndpointToken(jwtToken, jwtToken, expires_in, token_type, scope, refresh_token);
    }

    public void saveTokenEndpointToken(String access_token, String id_token, int expires_in, String token_type, String scope, String refresh_token) throws Exception {

        tokenResponseExpectations = new Expectations();
        String baseMsg = ServletMessageConstants.SERVLET + ServletMessageConstants.OPENID_CONTEXT;

        Map<String, List<String>> parms = new HashMap<String, List<String>>();
        if (access_token != null) {
            parms.put("access_token", Arrays.asList(access_token));
            tokenResponseExpectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, baseMsg + ServletMessageConstants.ACCESS_TOKEN
                                                                                                                  + access_token, "Did not find the proper access_token value in the the test app response."));
        }
        if (id_token != null) {
            parms.put("id_token", Arrays.asList(id_token));
            tokenResponseExpectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, baseMsg + ServletMessageConstants.ID_TOKEN
                                                                                                                  + id_token, "Did not find the proper id_token value in the the test app response."));
        }
        if (token_type != null) {
            parms.put("token_type", Arrays.asList(token_type));
            tokenResponseExpectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, baseMsg + ServletMessageConstants.TOKEN_TYPE
                                                                                                                  + token_type, "Did not find the proper token_type value in the the test app response."));
        }
        if (expires_in != 9999) { // 9999 is a flag telling the test token endpoint to omit sending expires_in
            parms.put("expires_in", Arrays.asList(Integer.toString(expires_in)));
            tokenResponseExpectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, baseMsg + ServletMessageConstants.EXPIRES_IN + "Optional["
                                                                                                                  + Integer.toString(expires_in)
                                                                                                                  + "]", "Did not find the proper expires_in value in the the test app response."));
        } else {
            tokenResponseExpectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, baseMsg + ServletMessageConstants.EXPIRES_IN
                                                                                                                  + "Optional[0]", "Did not find the proper expires_in value in the the test app response."));
        }
        if (scope != null) {
            parms.put("scope", Arrays.asList(scope));
        }
        if (refresh_token != null) {
            parms.put("refresh_token", Arrays.asList(refresh_token));
        }
        tokenResponseExpectations.addExpectation(new ResponseFullExpectation(null, Constants.STRING_CONTAINS, baseMsg + ServletMessageConstants.REFRESH_TOKEN
                                                                                                              + refresh_token, "Did not find the proper refresh_token value in the the test app response."));

        String tokenEndpt = rpHttpBase + "/TokenEndpointServlet/saveToken";
        // save a token endpoint response info to return from the test token endpoint
        secHttpUtils.getHttpConnectionWithAnyResponseCode(tokenEndpt, HTTPRequestMethod.PUT, parms);

    }

    /**
     * Run an end to end test and expect successful results.
     *
     * @param appRoot - the app to test with
     * @return - the final page
     * @throws Exception
     */
    public Page runEndToEndTest(String appRoot) throws Exception {
        return runEndToEndTest(appRoot, null);
    }

    /**
     * Run and end to end test. If expectations were passed in, use those to validate the response after processing the login page. If they were not passed in, create expectations
     * for a good end to end run.
     *
     * @param appRoot - the app to test with
     * @param expectations - not null for tests that expect problems with the tokens used
     * @return - the final page
     * @throws Exception
     */
    public Page runEndToEndTest(String appRoot, Expectations expectations) throws Exception {

        WebClient webClient = getAndSaveWebClient();

        Log.info(thisClass, _testName, "headers: " + rspValues.getHeaders());

        String app = "TokenTestApp";

        String url = rpHttpsBase + "/" + appRoot + "/" + app;

        Page response = invokeAppReturnLoginPage(webClient, url);

        if (expectations == null) {
            expectations = getGeneralAppExpecations(app);
            for (Expectation ex : tokenResponseExpectations.getExpectations()) {
                expectations.addExpectation(ex);
            }
        }

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);

        // confirm protected resource was accessed or we received the proper error messages from verifying the response from the token endpoint
        validationUtils.validateResult(response, expectations);

        return response;
    }

    /**
     * Build basic expectations for token validation failures - a 401 status code and generic token validation error messages - the callers will add more specific failures as
     * needed.
     *
     * @return - basic token validation failure expectations
     * @throws Exception
     */
    public Expectations getBaseErrorExpectations() throws Exception {

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2504E_CREDENTIAL_VALIDATION_ERROR, "Did not find a message in the server log stating that there was a failure validating the user credential."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2415E_TOKEN_VALIDATION_EXCEPTION, "Did not receive a message stating that there was an problem validating the token"));

        return expectations;
    }

    /**
     * Build Subject token validation failure expectations.
     *
     * @return - subject specific token validation failure expectations
     * @throws Exception
     */
    public Expectations getSubjectMismatchExpectations() throws Exception {

        Expectations expectations = getBaseErrorExpectations();
        if (_testName.contains("Missing")) {
            // check for jose4j embedded message
            expectations.addExpectation(new ServerMessageExpectation(rpServer, "No Subject", "Did not receive a message stating that the sub claim was missing."));
        }
        if (_testName.contains("Empty")) {
            expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2426E_TOKEN_HAS_EMPTY_CLAIM, "Did not find a message in the server log stating that sub claim was empty."));
        }

        return expectations;
    }

    /**
     * Build generic claim mismatch token validation failure expectations.
     *
     * @return - specific token validation failure expectations
     * @throws Exception
     */
    public Expectations getClaimMismatchExpectations(String claim, String expectedValue) throws Exception {

        Expectations expectations = getBaseErrorExpectations();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2424E_CLAIM_MISMATCH + ".*" + claim + ".*" + expectedValue
                                                                           + ".*", "Did not find a message in the server log stating that the " + claim
                                                                                   + " claim did not match the expected value of: " + expectedValue));

        return expectations;
    }

    /**
     * Build Expiration in the past token validation failure expectations.
     *
     * @return - exp specific token validation failure expectations
     * @throws Exception
     */
    public Expectations getExpInPastExpectations() throws Exception {

        Expectations expectations = getBaseErrorExpectations();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2427E_TOKEN_IS_EXPIRED, "Did not find a message in the server log stating that the token has expired."));

        return expectations;
    }

    /**
     * Build Claim incorrectly in the future token validation failure expectations.
     *
     * @return - specific time in future token validation failure expectations
     * @throws Exception
     */
    public Expectations getTimeValueInFutureExpectations(String claim) throws Exception {

        Expectations expectations = getBaseErrorExpectations();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2428E_TIME_IN_FUTURE + ".*"
                                                                           + claim, "Did not find a message in the server log stating that the "
                                                                                    + claim + " was in the future."));

        return expectations;
    }

    /**
     * Build missing required claims token validation failure expectations - check includes making sure that the claim is included in the message
     *
     * @param claim - the claim that should be recorded in the error message
     * @return - issuer specific token validation failure expectations
     * @throws Exception
     */
    public Expectations getMissingClaimExpectations(String claim) throws Exception {

        Expectations expectations = getBaseErrorExpectations();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2417E_MISSING_REQUIRED_CLAIM + ".*"
                                                                           + claim, "Did not find a message in the server log stating that the " + claim + " claim was missing."));

        return expectations;
    }

    public Expectations getJoseExceptionExpectations() throws Exception {

        Expectations expectations = getBaseErrorExpectations();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, "org.jose4j.lang.JoseException", "Did not find a jose4j error."));

        return expectations;

    }

    public Expectations getMissingRequiredParmsExpectations(String missingToken) throws Exception {

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2416E_TOKEN_REQUEST_ERROR, "Did not find a message in the server log stating that there was error sending a request to the token endpoint."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2429E_TOKEN_RESPONSE_MISSING_PARAMETER + ".*" + missingToken
                                                                           + ".*", "Did not find a message in the server log stating that the " + missingToken
                                                                                   + " claim was missing."));

        return expectations;
    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/
    /**
     * Test that a token returned from the token endpoint without an iss claim will fail validation.
     */
    @Test
    public void TokenValidationTests_tokens_issMissing() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_ISSUER);
        // omit the issuer
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getClaimMismatchExpectations(PayloadConstants.PAYLOAD_ISSUER, rpHttpBase + "/TokenEndpointServlet"));

    }

    /**
     * Test that a token returned from the token endpoint with an empty iss claim will fail validation.
     */
    @Test
    public void TokenValidationTests_tokens_issEmpty() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_ISSUER);
        // set issuer to an empty string
        builder.setIssuer("");

        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getClaimMismatchExpectations(PayloadConstants.PAYLOAD_ISSUER, rpHttpBase + "/TokenEndpointServlet"));

    }

    /**
     * Test that a token returned from the token endpoint with an iss claim that does not match the client specified issues will fail validation.
     */
    @Test
    public void TokenValidationTests_tokens_issMismatch() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_ISSUER);
        // override issuer value with what the OP would return in discovery, but our annotation overrides that value with a different value
        builder.setIssuer(opHttpsBase + "/someValue");
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getClaimMismatchExpectations(PayloadConstants.PAYLOAD_ISSUER, rpHttpBase + "/TokenEndpointServlet"));

    }

    /**
     * Test that a token returned from the token endpoint with an iss claim that matches the issuer value from discovery, but does NOT match the value in
     * the @OpenIdAuthenticationMechanismDefinition annotation (which takes precidence) will result in the token validation to fail.
     */
    // @Test - equivalent to a test in ConfigurationProviderMetadataTests
    public void TokenValidationTests_tokens_issMatchesDiscoveryMisMatchesProviderMetadata() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_ISSUER);
        // override issuer value with what the OP would return in discovery, but our annotation overrides that value with a different value
        builder.setIssuer(opHttpsBase + "/oidc/endpoint/OP1");
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getClaimMismatchExpectations(PayloadConstants.PAYLOAD_ISSUER, rpHttpBase + "/TokenEndpointServlet"));

    }

    /**
     * Test that a token returned from the token endpoint with an iss claim that does not match the issuer value from discovery, but does match the value in
     * the @OpenIdAuthenticationMechanismDefinition annotation (which takes precidence) will result in token validation to succeed.
     */
    // @Test - equivalent to a test in ConfigurationProviderMetadataTests
    public void TokenValidationTests_tokens_issMismatchesDiscoveryMatchesProviderMetadata() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet");

    }

    /**
     * Test that a token returned from the token endpoint without a sub claim will fail validation.
     */
    @Test
    public void TokenValidationTests_tokens_subMissing() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_SUBJECT);
        // omit the sub claim from the token
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getSubjectMismatchExpectations());

    }

    /**
     * Test that a token returned from the token endpoint with an empty sub claim will fail validation.
     */
    @Test
    public void TokenValidationTests_tokens_subEmpty() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_SUBJECT);
        // set the sub claim to an empty string
        builder.setSubject("");
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getSubjectMismatchExpectations());

    }

    /**
     * Test that a token returned from the token endpoint with any value in the sub claim will be allowed
     */
    @Test
    public void TokenValidationTests_tokens_sub() throws Exception {
        JWTTokenBuilder builder = initTokenBuilder();
        // override sub value
        builder.setSubject("bob");
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet");

    }

    /**
     * Test that a token returned from the token endpoint without an aud claim will fail validation.
     */
    @Test
    public void TokenValidationTests_tokens_audMissing() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_AUDIENCE);
        // omit the sub claim from the token
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getMissingClaimExpectations(PayloadConstants.PAYLOAD_AUDIENCE));

    }

    /**
     * Test that a token returned from the token endpoint with an empty aud claim will fail validation.
     */
    @Test
    public void TokenValidationTests_tokens_audEmpty() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_AUDIENCE);
        // set the sub claim to an empty string
        builder.setAudience("");
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getClaimMismatchExpectations(PayloadConstants.PAYLOAD_AUDIENCE, rspValues.getClientId()));

    }

    /**
     * Test that a token returned from the token endpoint with an invalid aud claim will fail validation.
     */
    @Test
    public void TokenValidationTests_tokens_audInvalid() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        // set aud to an invalid value
        builder.setAudience("badAudience");
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getClaimMismatchExpectations(PayloadConstants.PAYLOAD_AUDIENCE, rspValues.getClientId()));

    }

    /**
     * Test that a token returned from the token endpoint with an aud claim that contains a list of values that do NOT match the clientId and no azp claim will fail validation.
     */
    @Test
    public void TokenValidationTests_tokens_audList_correctAudNotInList_missingAzp() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        // set aud to a list of values where none are valid
        builder.setClaim(PayloadConstants.PAYLOAD_AUDIENCE, Arrays.asList("badAudience1", "badAudience2"));
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getClaimMismatchExpectations(PayloadConstants.PAYLOAD_AUDIENCE, rspValues.getClientId()));

    }

    /**
     * Test that a token returned from the token endpoint with an aud claim that contains a list of values that do NOT match the clientId and the azp claim that matches the
     * clientId will still fail validation.
     */
    @Test
    public void TokenValidationTests_tokens_audList_correctAudNotInList_azpValid() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        // set aud to a list of values where none are valid
        builder.setClaim(PayloadConstants.PAYLOAD_AUDIENCE, Arrays.asList("badAudience1", "badAudience2"));
        builder.setClaim(PayloadConstants.PAYLOAD_AUTHORIZED_PARTY, "client_1");
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getClaimMismatchExpectations(PayloadConstants.PAYLOAD_AUDIENCE, rspValues.getClientId()));

    }

    /**
     * Test that a token returned from the token endpoint with an aud claim that contains a list of values with one that matches the clientId and no azp claim fails validation.
     */
    @Test
    public void TokenValidationTests_tokens_audList_correctAudInList_azpMissing() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        // override issuer value with what the OP would return in discovery, but our annotation overrides that value with a different value
        builder.setClaim(PayloadConstants.PAYLOAD_AUDIENCE, Arrays.asList("badAudience", "client_1"));
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getMissingClaimExpectations(PayloadConstants.PAYLOAD_AUTHORIZED_PARTY));

    }

    /**
     * Test that a token returned from the token endpoint with an aud claim that contains a list of values with one that matches the clientId and an empty azp claim fails
     * validation.
     */
    @Test
    public void TokenValidationTests_tokens_audList_correctAudInList_azpEmpty() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        // override issuer value with what the OP would return in discovery, but our annotation overrides that value with a different value
        builder.setClaim(PayloadConstants.PAYLOAD_AUDIENCE, Arrays.asList("badAudience", "client_1"));
        builder.setClaim(PayloadConstants.PAYLOAD_AUTHORIZED_PARTY, "");
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getClaimMismatchExpectations(PayloadConstants.PAYLOAD_AUTHORIZED_PARTY, rspValues.getClientId()));

    }

    /**
     * Test that a token returned from the token endpoint with an aud claim that contains a list of values with one that matches the clientId and the azp claim that matches the
     * clientId will pass validation
     */
    @Test
    public void TokenValidationTests_tokens_audList_correctAudInList_azpValid() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        // override issuer value with what the OP would return in discovery, but our annotation overrides that value with a different value
        builder.setClaim(PayloadConstants.PAYLOAD_AUDIENCE, Arrays.asList("badAudience", "client_1"));
        builder.setClaim(PayloadConstants.PAYLOAD_AUTHORIZED_PARTY, "client_1");
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet");

    }

    /**
     * Test that a token returned from the token endpoint with an aud claim that contains a list of values with one that matches the clientId and an invalid azp claim fails
     * validation.
     */
    @Test
    public void TokenValidationTests_tokens_audList_correctAudInList_azpInvalid() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        // override issuer value with what the OP would return in discovery, but our annotation overrides that value with a different value
        builder.setClaim(PayloadConstants.PAYLOAD_AUDIENCE, Arrays.asList("badAudience", "client_1"));
        builder.setClaim(PayloadConstants.PAYLOAD_AUTHORIZED_PARTY, "badAudience");
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getClaimMismatchExpectations(PayloadConstants.PAYLOAD_AUTHORIZED_PARTY, rspValues.getClientId()));

    }

    /**
     * Test that a token returned from the token endpoint without an exp claim will fail validation.
     */
    @Test
    public void TokenValidationTests_tokens_expMissing() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_EXPIRATION_TIME_IN_SECS);
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getMissingClaimExpectations(PayloadConstants.PAYLOAD_EXPIRATION_TIME_IN_SECS));

    }

    /**
     * Test that a token returned from the token endpoint with an exp claim value that is in the past will fail validation.
     */
    @Test
    public void TokenValidationTests_tokens_expInThePast() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_EXPIRATION_TIME_IN_SECS);
        builder.setExpirationTime(NumericDate.fromSeconds(1672531140)); // 12/31/2022 11:59:00 PM
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getExpInPastExpectations());

    }

    /**
     * Test that a token returned from the token endpoint with an exp claim value that valid (in the future) and will pass validation
     */
    @Test
    public void TokenValidationTests_tokens_expValid() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet");

    }

    /**
     * Test that a token returned from the token endpoint without an iat claim will fail validation.
     */
    @Test
    public void TokenValidationTests_tokens_iatMissing() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_ISSUED_AT_TIME_IN_SECS);
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getMissingClaimExpectations(PayloadConstants.PAYLOAD_ISSUED_AT_TIME_IN_SECS));

    }

    /**
     * Test that a token returned from the token endpoint with an iat claim value that is in the future will fail validation.
     */
    @Test
    public void TokenValidationTests_tokens_iatInTheFuture() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_ISSUED_AT_TIME_IN_SECS);
        builder.setIssuedAt(NumericDate.fromSeconds(System.currentTimeMillis() + (24 * 60 * 60 * 1000))); // 1000 days in the future
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getTimeValueInFutureExpectations(PayloadConstants.PAYLOAD_ISSUED_AT_TIME_IN_SECS));

    }

    /**
     * Test that a token returned from the token endpoint with an iat claim value that valid (in the past) and will pass validation
     */
    @Test
    public void TokenValidationTests_tokens_iatValid() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet");

    }

    /**
     * Test that a token returned from the token endpoint without an nbf claim will pass validation and nbf is not required.
     */
    @Test
    public void TokenValidationTests_tokens_nbfMissing() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS);
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet");

    }

    /**
     * Test that a token returned from the token endpoint with an nbf claim that is in the future (but before the exp) will fail validation as the token is NOT valid yet.
     */
    @Test
    public void TokenValidationTests_tokens_nbfInTheFuture() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_EXPIRATION_TIME_IN_SECS);
        builder.setNotBefore(NumericDate.fromSeconds(System.currentTimeMillis() + (24 * 60 * 60 * 1000))); // 1000 days in the future
        builder.setExpirationTime(NumericDate.fromSeconds(System.currentTimeMillis() + (24 * 60 * 60 * 1001))); // 1001 days in the future
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getTimeValueInFutureExpectations(PayloadConstants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS));

    }

    /**
     * Test that a token returned from the token endpoint with an nbf claim that is in the future (but after the exp) will fail validation as the exp can not be before nbf.
     */
    @Test
    public void TokenValidationTests_tokens_nbfAfterExp() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        builder.setNotBefore(NumericDate.fromSeconds(System.currentTimeMillis() + (24 * 60 * 60 * 1000))); // 1000 days in the future
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet", getTimeValueInFutureExpectations(PayloadConstants.PAYLOAD_NOT_BEFORE_TIME_IN_SECS));

    }

    /**
     * Test that a token returned from the token endpoint with an nbf claim that is in the past and before exp will pass validation.
     */
    @Test
    public void TokenValidationTests_tokens_nbfValid() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        builder.setNotBeforeMinutesInThePast(5);
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet");

    }

    @Test
    public void TokenValidationTests_tokens_scopeMissing() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_SCOPE);
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet");

    }

    @Test
    public void TokenValidationTests_tokens_scopeEmpty() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_SCOPE);
        builder.setScope("");
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet");

    }

    @Test
    public void TokenValidationTests_tokens_scopeInvalid() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_SCOPE);
        builder.setScope("someScopeValue");
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet");

    }

    @Test
    public void TokenValidationTests_tokens_realmMissing() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_REALMNAME);
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet");

    }

    @Test
    public void TokenValidationTests_tokens_realmEmpty() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_REALMNAME);
        builder.setRealmName("");
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet");

    }

    @Test
    public void TokenValidationTests_tokens_realmInvalid() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_REALMNAME);
        builder.setRealmName("someRealmName");
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet");

    }

    @Test
    public void TokenValidationTests_tokens_tokenTypeMissing() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_TOKEN_TYPE);
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet");

    }

    @Test
    public void TokenValidationTests_tokens_tokenTypeEmpty() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_TOKEN_TYPE);
        builder.setTokenType("");
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet");

    }

    @Test
    public void TokenValidationTests_tokens_tokenTypeInvalid() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder(PayloadConstants.PAYLOAD_TOKEN_TYPE);
        builder.setTokenType("someTokenType");
        saveTokenEndpointToken(builder);

        runEndToEndTest("TokenTestAppServlet");

    }

    // now we'll alter the JSON token endpoint response content (not the content of the access or id tokens necessarily)

    /**
     * Omit the access_token from the json response returned by the token endpoint - check for the proper failure
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.TokenRequestException" })
    @Test
    public void TokenValidationTests_response_accessTokenMissing() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken(null, builder.buildAsIs(), Default_expires_in, Default_token_type, Default_scope, Default_refresh_token);

        runEndToEndTest("TokenTestAppServlet", getMissingRequiredParmsExpectations("access_token"));

    }

    /**
     * Token endpoint returns an empty access_token - valdidate that we still have access to the protected app since we can find what we need in the id_token
     *
     * @throws Exception
     */
    @Test
    public void TokenValidationTests_response_accessTokenEmpty() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken("", builder.buildAsIs(), Default_expires_in, Default_token_type, Default_scope, Default_refresh_token);
        opServer.addIgnoredErrors(Arrays.asList(MessageConstants.CWWKS1616E_USERINFO_REQUEST_MISSING_ACCESS_TOKEN));

        runEndToEndTest("TokenTestAppServlet");
    }

    /**
     * Token endpoint returns an invalid access_token (some text string) - valdidate that we still have access to the protected app since we can find what we need in the id_token
     *
     * @throws Exception
     */
    @Test
    public void TokenValidationTests_response_accessTokenInvalid() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken("ThisIsJustSomeString", builder.buildAsIs(), Default_expires_in, Default_token_type, Default_scope, Default_refresh_token);

        runEndToEndTest("TokenTestAppServlet");

    }

    /**
     * Omit the id_token from the json response returned by the token endpoint - check for the proper failure
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.TokenRequestException" })
    @Test
    public void TokenValidationTests_response_idTokenMissing() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken(builder.buildAsIs(), null, Default_expires_in, Default_token_type, Default_scope, Default_refresh_token);

        runEndToEndTest("TokenTestAppServlet", getMissingRequiredParmsExpectations("id_token"));

    }

    /**
     * Token endpoint returns an empty id_token - valdidate that we receive a 401 status code and the proper error(s) in the server log
     *
     * @throws Exception
     */
    @Test
    public void TokenValidationTests_response_idTokenEmpty() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken(builder.buildAsIs(), "", Default_expires_in, Default_token_type, Default_scope, Default_refresh_token);

        runEndToEndTest("TokenTestAppServlet", getJoseExceptionExpectations());

    }

    /**
     * Token endpoint returns an invalid id_token - valdidate that we receive a 401 status code and the proper error(s) in the server log
     *
     * @throws Exception
     */
    @Test
    public void TokenValidationTests_response_idTokenInvalid() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken(builder.buildAsIs(), "ThisIsJustSomeString", Default_expires_in, Default_token_type, Default_scope, Default_refresh_token);

        runEndToEndTest("TokenTestAppServlet", getJoseExceptionExpectations());

    }

    /**
     * Omit the expires_in from the json response returned by the token endpoint - ensure that we have access to the protected app and that the expires_in value is 0
     *
     * @throws Exception
     */
    @Test
    public void TokenValidationTests_response_expiresInMissing() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken(builder, Omit_expires_in, Default_token_type, Default_scope, Default_refresh_token);

        runEndToEndTest("TokenTestAppServlet");

    }

    /**
     * Token endpoint returns expires_in set to 0 - ensure that we have access to the protected app and that the expires_in value is 0
     *
     * @throws Exception
     */
    @Test
    public void TokenValidationTests_response_expiresInZero() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken(builder, 0, Default_token_type, Default_scope, Default_refresh_token);

        runEndToEndTest("TokenTestAppServlet");

    }

    /**
     * Omit the token_type from the json response returned by the token endpoint - check for the proper failure
     *
     * @throws Exception
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.exceptions.TokenRequestException" })
    @Test
    public void TokenValidationTests_response_tokenTypeMissing() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken(builder, Default_expires_in, null, Default_scope, Default_refresh_token);

        runEndToEndTest("TokenTestAppServlet", getMissingRequiredParmsExpectations("token_type"));

    }

    /**
     * Token endpoint returns an empty token_Type - valdidate that we still have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void TokenValidationTests_response_tokenTypeEmpty() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken(builder, Default_expires_in, "", Default_scope, Default_refresh_token);

        runEndToEndTest("TokenTestAppServlet");

    }

    /**
     * Token endpoint returns an invalid token_Type - valdidate that we still have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void TokenValidationTests_response_tokenTypeInvalid() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken(builder, Default_expires_in, "ThisIsJustSomeString", Default_scope, Default_refresh_token);

        runEndToEndTest("TokenTestAppServlet");

    }

    /**
     * Omit the scope from the json response returned by the token endpoint - ensure that we have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void TokenValidationTests_response_scopeMissing() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken(builder, Default_expires_in, Default_token_type, null, Default_refresh_token);

        runEndToEndTest("TokenTestAppServlet");

    }

    /**
     * Token endpoint returns an empty scope - ensure that we have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void TokenValidationTests_response_scopeEmpty() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken(builder, Default_expires_in, Default_token_type, "", Default_refresh_token);

        runEndToEndTest("TokenTestAppServlet");

    }

    /**
     * Token endpoint returns an invalid scope - ensure that we have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void TokenValidationTests_response_scopeInvalid() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken(builder, Default_expires_in, Default_token_type, "ThisIsJustSomeString", Default_refresh_token);

        runEndToEndTest("TokenTestAppServlet");

    }

    /**
     * Omit the refresh_token from the json response returned by the token endpoint - censure that we have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void TokenValidationTests_response_refreshTokenMissing() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken(builder, Default_expires_in, Default_token_type, Default_scope, null);

        runEndToEndTest("TokenTestAppServlet");

    }

    /**
     * Token endpoint returns an empty refresh_token - ensure that we have access to the protected app
     *
     * @throws Exception
     */
    @Test
    public void TokenValidationTests_response_refreshTokenEmpty() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken(builder, Default_expires_in, Default_token_type, Default_scope, "");

        runEndToEndTest("TokenTestAppServlet");

    }

    /**
     * Token endpoint returns an invalid refresh_token - ensure that we have access to the protected app - we'll only need the refresh_token if we try to refresh the tokens
     *
     * @throws Exception
     */
    @Test
    public void TokenValidationTests_response_refreshTokenInvalid() throws Exception {

        JWTTokenBuilder builder = initTokenBuilder();
        saveTokenEndpointToken(builder, Default_expires_in, Default_token_type, Default_scope, "ThisIsJustSomeString");

        runEndToEndTest("TokenTestAppServlet");

    }
}
