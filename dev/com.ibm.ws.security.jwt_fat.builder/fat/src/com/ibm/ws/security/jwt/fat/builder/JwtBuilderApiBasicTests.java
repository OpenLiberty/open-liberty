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

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Pattern;

import org.jose4j.jwt.NumericDate;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.jwt.ClaimConstants;
import com.ibm.ws.security.fat.common.jwt.HeaderConstants;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.JwtMessageConstants;
import com.ibm.ws.security.fat.common.jwt.expectations.JwtClaimExpectation;
import com.ibm.ws.security.fat.common.jwt.utils.ClaimHelpers;
import com.ibm.ws.security.fat.common.servers.ServerInstanceUtils;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.buider.actions.JwtBuilderActions;
import com.ibm.ws.security.jwt.fat.buider.actions.JwtBuilderClaimRepeatActions;
import com.ibm.ws.security.jwt.fat.builder.utils.BuilderHelpers;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
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

@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JwtBuilderApiBasicTests extends CommonSecurityFat {

    @Server("com.ibm.ws.security.jwt_fat.builder")
    public static LibertyServer builderServer;
    @Server("com.ibm.ws.security.jwt_fat.builder.rs")
    public static LibertyServer rsServer;
    static String xx = null;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(JwtBuilderClaimRepeatActions.asCollection()).andWith(JwtBuilderClaimRepeatActions.asSingle());

    //    public static final ConsumerHelpers consumerHelpers = new ConsumerHelpers();
    private static final JwtBuilderActions actions = new JwtBuilderActions();
    public static final TestValidationUtils validationUtils = new TestValidationUtils();

    public long testExp = 2107268760L;
    public long oldExp = 1443551518L;
    public static String processClaimsAs = "null";
    public static String protectedApp;

    @BeforeClass
    public static void setUp() throws Exception {

        serverTracker.addServer(builderServer);
        builderServer.startServerUsingExpandedConfiguration("server_basicRegistry.xml");
        SecurityFatHttpUtils.saveServerPorts(builderServer, JWTBuilderConstants.BVT_SERVER_1_PORT_NAME_ROOT);

        serverTracker.addServer(rsServer);
        ServerInstanceUtils.addHostNameAndAddrToBootstrap(rsServer);
        rsServer.startServerUsingExpandedConfiguration("rs_server_orig.xml");
        SecurityFatHttpUtils.saveServerPorts(rsServer, JWTBuilderConstants.BVT_SERVER_2_PORT_NAME_ROOT);

        protectedApp = SecurityFatHttpUtils.getServerUrlBase(rsServer) + "helloworld/rest/helloworld";

        if (FATSuite.runAsCollection) {
            processClaimsAs = JwtConstants.AS_COLLECTION;
        } else {
            processClaimsAs = JwtConstants.AS_SINGLE;
        }

    }

    //    @Override
    //    @Before
    //    public void commonBeforeTest() {
    //        super.commonBeforeTest();
    //        try {
    //            builder = createBuilderWithDefaultClaims();
    //        } catch (Exception e) {
    //            Log.info(thisClass, "commonBeforeTest", e.toString());
    //            e.printStackTrace(System.out);
    //            // just set the builder to null - this will cause the test cases to blow up
    //            builder = null;
    //        }
    //
    //    }

    //    @Override
    //    public JWTTokenBuilder createBuilderWithDefaultClaims() throws Exception {
    //
    //        JWTTokenBuilder builder = consumerHelpers.createBuilderWithDefaultConsumerClaims();
    //
    //        builder.setAudience(SecurityFatHttpUtils.getServerSecureUrlBase(consumerServer) + JWTConsumerConstants.JWT_CONSUMER_ENDPOINT);
    //
    //        return builder;
    //    }
    //
    //    /**
    //     * Consume the built JWT Token - for these tests, that means passing the JWT Token to the JWTCLientConsumer app. This app will
    //     * invoke the JWT Consumer api's which will process/verify the token.
    //     *
    //     * @param token
    //     *            - the token to consume
    //     */
    //    @Override
    //    public Page consumeToken(String token) throws Exception {
    //
    //        return actions.invokeJwtConsumer(_testName, consumerServer, jwtConsumerId, token);
    //
    //    }
    //
    //    @Override
    //    public Expectations addGoodResponseAndClaimsExpectations(String currentAction, JWTTokenBuilder builder) throws Exception {
    //
    //        return consumerHelpers.addGoodConsumerClientResponseAndClaimsExpectations(currentAction, builder, consumerServer);
    //    }
    //
    //    @Override
    //    public Expectations updateExpectationsForJsonAttribute(Expectations expectations, String key, Object value) throws Exception {
    //
    //        return consumerHelpers.updateExpectationsForJsonAttribute(expectations, key, value);
    //
    //    }
    //
    //    @Override
    //    protected Expectations buildNegativeAttributeExpectations(String specificErrorId) throws Exception {
    //
    //        return consumerHelpers.buildNegativeAttributeExpectations(specificErrorId, currentAction, consumerServer, jwtConsumerId);
    //
    //    }
    //
    //    // get error messages
    //    @Override
    //    protected String getJtiReusedMsg() {
    //        return JwtMessageConstants.CWWKS6045E_JTI_REUSED;
    //    }
    //
    //    @Override
    //    protected String getIssuerNotTrustedMsg() {
    //        return JwtMessageConstants.CWWKS6022E_ISSUER_NOT_TRUSTED;
    //    }
    //
    //    @Override
    //    protected String getSignatureNotValidMsg() {
    //        return JwtMessageConstants.CWWKS6041E_JWT_SIGNATURE_INVALID;
    //    }
    //
    //    @Override
    //    protected String getTokenExpiredMsg() {
    //        return JwtMessageConstants.CWWKS6025E_TOKEN_EXPIRED;
    //    }
    //
    //    @Override
    //    protected String getMalformedClaimMsg() {
    //        return JwtMessageConstants.CWWKS6043E_MALFORMED_CLAIM;
    //    }
    //
    //    @Override
    //    protected String getIatAfterExpMsg() {
    //        return JwtMessageConstants.CWWKS6024E_IAT_AFTER_EXP;
    //    }
    //
    //    @Override
    //    protected String getIatAfterCurrentTimeMsg() {
    //        return JwtMessageConstants.CWWKS6044E_IAT_AFTER_CURRENT_TIME;
    //    }
    //
    //    @Override
    //    protected String getBadAudienceMsg() {
    //        return JwtMessageConstants.CWWKS6023E_BAD_AUDIENCE;
    //    }
    //
    //    @Override
    //    protected String getBadNotBeforeMsg() {
    //        return JwtMessageConstants.CWWKS6026E_FUTURE_NBF;
    //    }

    /**************************************************************
     * Test Builder create specific Tests
     **************************************************************/
    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified config (defaultJWT - no config, just use default values)
     * <LI>Do NOT run any of the api's to update the builder
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with the default values
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that no "set" api's were invoked
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.CollectionID)
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_create_id_defaultJWT() throws Exception {

        JSONObject settings = BuilderHelpers.setDefaultClaims(builderServer);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_CREATE_ENDPOINT, settings, builderServer);

        Page response = actions.invokeJwtBuilder_create(_testName, builderServer, null);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified config (defaultJWT - no config, just use default values)
     * <LI>Run the subject api to update the builder
     * <LI>generate a JWT token
     * <LI>Invoke a protected App using the generated token to show that it is valid
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with the default values
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that the subject "set" api was invoked
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * <LI>The output from the protected app
     * </UL>
     * </OL>
     */
    //    @SkipForRepeat(JwtBuilderClaimRepeatActions.SingleID)
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIBasicTests_create_id_defaultJWT_consumeToken() throws Exception {

        JSONObject settings = BuilderHelpers.setDefaultClaims(builderServer);
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, "user2");
        settings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, null, testSettings);
        //        Page response = actions.invokeJwtBuilder_create(_testName, builderServer, null);
        validationUtils.validateResult(response, expectations);

        // TODO consume
        // pull out the token and then invoke the protected app on the RS server
        String jwtToken = actions.extractJwtTokenFromResponse(response, JWTBuilderConstants.BUILT_JWT_TOKEN);

        List<NameValuePair> requestParms = new ArrayList<NameValuePair>();
        requestParms.add(new NameValuePair("access_token", jwtToken));
        Page appResponse = actions.invokeUrlWithParameters(_testName, new WebClient(), protectedApp, requestParms);

        //        Page appResponse = actions.invokeUrlWithBearerToken(_testName, new WebClient(), protectedApp, jwtToken);
        Expectations appExpectations = new Expectations();
        appExpectations.addExpectations(CommonExpectations.successfullyReachedUrl(null, protectedApp));
        //        Expectations appExpectations = goodTestExpectations(jwtTokenTools, protectedApp, className);
        validationUtils.validateResult(appResponse, appExpectations);
    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Attempt to create a builder using the specified config - the config does NOT exist
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should NOT be created
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that the configId could not be found
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.CollectionID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_create_id_notExist() throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(SecurityFatHttpUtils.getServerUrlBase(builderServer) + JWTBuilderConstants.JWT_BUILDER_CREATE_ENDPOINT));
        expectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, JwtMessageConstants.CWWKS6008E_BUILD_ID_UNKNOWN + ".+someBadBuilderId", "Response did not show the expected failure."));

        Page response = actions.invokeJwtBuilder_create(_testName, builderServer, "someBadBuilderId");
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Attempt to create a builder using a config Id of <null>
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should NOT be created
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that the configId was not valid
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.SingleID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_create_id_null() throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(SecurityFatHttpUtils.getServerUrlBase(builderServer) + JWTBuilderConstants.JWT_BUILDER_CREATE_ENDPOINT));
        expectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, JwtMessageConstants.CWWKS6008E_BUILD_ID_UNKNOWN + ".+null", "Response did not show the expected failure."));

        Page response = actions.invokeJwtBuilder_create(_testName, builderServer, JWTBuilderConstants.NULL_STRING);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Attempt to create a builder using a config Id of ""
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should NOT be created
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that the configId was not valid
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.CollectionID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_create_id_empty() throws Exception {

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(SecurityFatHttpUtils.getServerUrlBase(builderServer) + JWTBuilderConstants.JWT_BUILDER_CREATE_ENDPOINT));
        expectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, JwtMessageConstants.CWWKS6008E_BUILD_ID_UNKNOWN + ".+\\[\\]", "Response did not show the expected failure."));

        Page response = actions.invokeJwtBuilder_create(_testName, builderServer, JWTBuilderConstants.EMPTY_STRING);
        validationUtils.validateResult(response, expectations);

    }

    /***************************************************** Test audience ****************************************************/
    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the audience api to update the builder
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have any updates to audience
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "audience"
     * <LI>The failure messages from our attempt to invoke "audience"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.SingleID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_audience_nullList() throws Exception {

        // need separate test class
    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the audience api to update the builder
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updates to audience
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "audience"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.CollectionID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_audience_one() throws Exception {

        String builderId = "jwt1";
        JSONObject settings = BuilderHelpers.setDefaultClaims(builderServer);
        settings.put(ClaimConstants.ISSUER, builderId);
        //        settings.put(ClaimConstants.ISSUER, BuilderHelpers.createClaimContent(builderId, JwtConstants.JWT_BUILDER_ISSUER));

        JSONArray parmarray = new JSONArray();
        parmarray.add("Client02");
        JSONObject overrideSettings = new JSONObject();
        //        overrideSettings.put(ClaimConstants.AUDIENCE, BuilderHelpers.createClaimContent(parmarray, JwtConstants.JWT_BUILDER_AUDIENCE));
        overrideSettings.put(ClaimConstants.AUDIENCE, parmarray);
        settings.put("overrideSettings", overrideSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, builderServer);

        //        JwtClaims claims = BuilderHelpers.setDefaultClaims(builderServer);
        //        claims.setAudience("Client02");
        //        claims.setIssuer(builderId);
        //
        //        Expectations expectations = BuilderHelpers.addGoodBuilderSetApisClientResponseAndClaimsExpectations(null, claims, builderServer);

        //                JSONObject parms = new JSONObject();
        //                JSONArray parmarray = new JSONArray();
        //                parmarray.add("Client02");
        //                parms.put(ClaimConstants.AUDIENCE, parmarray);
        //                JSONObject claimsToSet = new JSONObject();
        //                updateJSONWithList(claimsToSet, ClaimConstants.AUDIENCE, claims.getAudience());
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, overrideSettings);
        validationUtils.validateResult(response, expectations);

    }

    public void updateJSONWithList(JSONObject jObject, String key, List<String> value) {

        if (value != null) {
            JSONArray parmarray = new JSONArray();
            for (String entry : value) {
                parmarray.add(entry);
            }
            jObject.put(key, parmarray);

        }
    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the audience api to update the builder
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updates to audience
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "audience"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.SingleID)
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_audience_multiple() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONArray parmarray = new JSONArray();
        parmarray.add("Client04");
        parmarray.add("Client05");
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.AUDIENCE, parmarray);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the audience api to update the builder
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updates to audience (with duplicates removed)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "audience"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.CollectionID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_audience_duplicates() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONArray parmarray = new JSONArray();
        parmarray.add("Client04");
        parmarray.add("Client05");
        parmarray.add("Client04");
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.AUDIENCE, parmarray);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);
        expectations.addExpectation(new JwtClaimExpectation(JwtConstants.STRING_DOES_NOT_CONTAIN, "Client04, Client05, Client04", "Found duplicate values in Audience"));
        expectations.addExpectation(new JwtClaimExpectation(JwtConstants.STRING_DOES_NOT_CONTAIN, "Client04, Client04, Client05", "Found duplicate values in Audience"));
        expectations.addExpectation(new JwtClaimExpectation(JwtConstants.STRING_DOES_NOT_CONTAIN, "Client05, Client04, Client04", "Found duplicate values in Audience"));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the audience api to update the builder
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updates to audience (should have mixed case versions of the same string as they really are
     * different)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "audience"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.SingleID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_audience_duplicates_caseSensitive() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONArray parmarray = new JSONArray();
        parmarray.add("Client04");
        parmarray.add("Client05");
        parmarray.add("client04");
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.AUDIENCE, parmarray);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the audience api to update the builder
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updates to audience (there should be NO null entry in the audience)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "audience"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.CollectionID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_audience_nullListEntry() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONArray parmarray = new JSONArray();
        parmarray.add("Client04");
        parmarray.add(null);
        parmarray.add("Client05");
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.AUDIENCE, parmarray);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the audience api to update the builder
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updates to audience (there should be NO empty entry in the audience)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "audience"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.SingleID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_audience_emptyListEntry() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONArray parmarray = new JSONArray();
        parmarray.add("Client04");
        parmarray.add("");
        parmarray.add("Client05");
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.AUDIENCE, parmarray);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /*****************************************************
     * Test expirationTime
     ****************************************************/

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the expirationTime api to update the builder
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updates to expiration (exp should be set to 2107268760)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "expirationTime"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.CollectionID)
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_expirationTime() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.EXPIRATION_TIME, testExp);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the expirationTime api to update the builder with a bad value (value in the past)
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have any updates to expiration (value should be currentTime + expiry time)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "expirationTime"
     * <LI>The failure messages from our attempt to invoke "expirationTime"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.SingleID)
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_expirationTime_inThePast() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.EXPIRATION_TIME, oldExp);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6042E_BAD_EXP_TIME, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the expirationTime api to update the builder with a bad value (value of zero)
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have any updates to expiration (value should be currentTime + expiry time)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "expirationTime"
     * <LI>The failure messages from our attempt to invoke "expirationTime"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.CollectionID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_expirationTime_zero() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.EXPIRATION_TIME, NumericDate.fromSeconds(0L).getValue());
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6042E_BAD_EXP_TIME, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the expirationTime api to update the builder with a bad value (value of -2)
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have any updates to expiration (value should be currentTime + expiry time)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "expirationTime"
     * <LI>The failure messages from our attempt to invoke "expirationTime"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.SingleID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_expirationTime_negative() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.EXPIRATION_TIME, NumericDate.fromSeconds(-2L).getValue());
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6042E_BAD_EXP_TIME, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /***************************************************** Test notBefore ****************************************************/
    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the notBefore api to update the builder with a good value
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated notBefore (value should be )
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "notBefore"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.CollectionID)
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_notBefore() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.NOT_BEFORE, NumericDate.fromSeconds(2106325918L).getValue());
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the notBefore api to update the builder with a bad value (value of zero)
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have any updates to notBefore (value should NOT be set)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "notBefore"
     * <LI>The failure messages from our attempt to invoke "notBefore"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.SingleID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_notBefore_zero() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.NOT_BEFORE, NumericDate.fromSeconds(0L).getValue());
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6018E_CLAIM_MUST_BE_GT_ZERO, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the notBefore api to update the builder with a bad value (value of -2L)
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have any updates to notBefore (value should NOT be set)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "notBefore"
     * <LI>The failure messages from our attempt to invoke "notBefore"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.CollectionID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_notBefore_negative() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.NOT_BEFORE, NumericDate.fromSeconds(-2L).getValue());
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6018E_CLAIM_MUST_BE_GT_ZERO, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /***************************************************** Test jwtId ****************************************************/
    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a config with jti set to false)
     * <LI>Run the jwtId api to update the builder with a value of false
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated jwtId (value should false/no jti set)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "jwtId"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.SingleID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_jwtId_cfgFalse_apiFalse() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.JWT_ID, false);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);
        expectations.addExpectation(new JwtClaimExpectation(JwtConstants.STRING_MATCHES, JwtConstants.JWT_BUILDER_CLAIM + JwtConstants.JWT_BUILDER_JWTID + ".*null.*", "jti was NOT found and should have been"));
        expectations.addExpectation(new JwtClaimExpectation(JwtConstants.STRING_DOES_NOT_MATCH, JwtConstants.JWT_BUILDER_CLAIM + JwtConstants.JWT_BUILDER_JSON + "\\{" + ".*\"" + ClaimConstants.JWT_ID + "\".*\\}", "jti was found in the list of claims and should NOT have been"));
        expectations.addExpectation(new JwtClaimExpectation(JwtConstants.STRING_DOES_NOT_MATCH, JwtConstants.JWT_BUILDER_CLAIM + JwtConstants.JWT_BUILDER_JSON + JwtConstants.JWT_BUILDER_GETALLCLAIMS + JwtConstants.JWT_BUILDER_KEY + ClaimConstants.JWT_ID + ".*", "The jti claim was found and should NOT have been"));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a config with jti set to false)
     * <LI>Run the jwtId api to update the builder with a value of true
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated jwtId (value should true/jti is set)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "jwtId"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.CollectionID)
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_jwtId_cfgFalse_apiTrue() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.JWT_ID, true);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);
        expectations.addExpectation(new JwtClaimExpectation(JwtConstants.STRING_DOES_NOT_MATCH, JwtConstants.JWT_BUILDER_CLAIM + JwtConstants.JWT_BUILDER_JWTID + ".*null.*", "jti was found and should NOT have been"));
        expectations.addExpectation(new JwtClaimExpectation(JwtConstants.STRING_MATCHES, JwtConstants.JWT_BUILDER_CLAIM + JwtConstants.JWT_BUILDER_JSON + "\\{" + ".*\"" + ClaimConstants.JWT_ID + "\".*\\}", "jti was NOT found in the list of claims"));
        expectations.addExpectation(new JwtClaimExpectation(JwtConstants.STRING_MATCHES, JwtConstants.JWT_BUILDER_CLAIM + JwtConstants.JWT_BUILDER_JSON + JwtConstants.JWT_BUILDER_GETALLCLAIMS + JwtConstants.JWT_BUILDER_KEY + ClaimConstants.JWT_ID + ".*", "The jti claim was NOT found and should have been"));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a config with jti set to true)
     * <LI>Run the jwtId api to update the builder with a value of true
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated jwtId (value should true/jti is set)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "jwtId"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.SingleID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_jwtId_cfgTrue_apiTrue() throws Exception {

        String builderId = "jwt_jtiTrue";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.JWT_ID, true);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);
        expectations.addExpectation(new JwtClaimExpectation(JwtConstants.STRING_DOES_NOT_MATCH, JwtConstants.JWT_BUILDER_CLAIM + JwtConstants.JWT_BUILDER_JWTID + ".*null.*", "jti was found and should NOT have been"));
        expectations.addExpectation(new JwtClaimExpectation(JwtConstants.STRING_MATCHES, JwtConstants.JWT_BUILDER_CLAIM + JwtConstants.JWT_BUILDER_JSON + "\\{" + ".*\"" + ClaimConstants.JWT_ID + "\".*\\}", "jti was NOT found in the list of claims"));
        expectations.addExpectation(new JwtClaimExpectation(JwtConstants.STRING_MATCHES, JwtConstants.JWT_BUILDER_CLAIM + JwtConstants.JWT_BUILDER_JSON + JwtConstants.JWT_BUILDER_GETALLCLAIMS + JwtConstants.JWT_BUILDER_KEY + ClaimConstants.JWT_ID + ".*", "The jti claim was NOT found and should have been"));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a config with jti set to true)
     * <LI>Run the jwtId api to update the builder with a value of false
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated jwtId (value should false/no jti set)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "jwtId"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.CollectionID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_jwtId_cfgTrue_apiFalse() throws Exception {

        String builderId = "jwt_jtiTrue";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.JWT_ID, false);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);
        expectations.addExpectation(new JwtClaimExpectation(JwtConstants.STRING_MATCHES, JwtConstants.JWT_BUILDER_CLAIM + JwtConstants.JWT_BUILDER_JWTID + ".*null.*", "jti was NOT found and should have been"));
        expectations.addExpectation(new JwtClaimExpectation(JwtConstants.STRING_DOES_NOT_MATCH, JwtConstants.JWT_BUILDER_CLAIM + JwtConstants.JWT_BUILDER_JSON + "\\{" + ".*\"" + ClaimConstants.JWT_ID + "\".*\\}", "jti was found in the list of claims and should NOT have been"));
        expectations.addExpectation(new JwtClaimExpectation(JwtConstants.STRING_DOES_NOT_MATCH, JwtConstants.JWT_BUILDER_CLAIM + JwtConstants.JWT_BUILDER_JSON + JwtConstants.JWT_BUILDER_GETALLCLAIMS + JwtConstants.JWT_BUILDER_KEY + ClaimConstants.JWT_ID + ".*", "The jti claim was found and should NOT have been"));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /***************************************************** Test subject ****************************************************/
    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the subject api to update the builder with a good value
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated subject (value should be user2)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "subject"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.SingleID)
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_subject_validUser() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, "user2");
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the subject api to update the builder with a bad value
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated subject (value should be someOtherUser)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "subject"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.CollectionID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_subject_invalidUser() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, "someOtherUser");
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the subject api to update the builder with a bad value (value is <null>)
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have any updates to subject (there should be no value set)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "subject"
     * <LI>The failure messages from our attempt to invoke "subject"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.SingleID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_subject_null() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, null);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6009E_INVALID_CLAIM, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the subject api to update the builder with a bad value (value is "")
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have any updates to subject (there should be no value set)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "subject"
     * <LI>The failure messages from our attempt to invoke "subject"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.CollectionID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_subject_emptyUser() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, "");
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6009E_INVALID_CLAIM, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /***************************************************** Test issuer ****************************************************/
    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the issuer api to update the builder with a good value
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated issuer (value should be "someIssuer")
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "issuer"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.SingleID)
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_issuer_validIssuer() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.ISSUER, "someIsser");
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the issuer api to update the builder with a bad value (value is <null>)
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have any updates to issuer (the value should be https://<hostname>:<secureport>/jwt/<configId>)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "issuer"
     * <LI>The failure messages from our attempt to invoke "issuer"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.CollectionID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_issuer_null() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.ISSUER, null);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6009E_INVALID_CLAIM, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the issuer api to update the builder with a bad value (value is "")
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have any updates to issuer (the value should be https://<hostname>:<secureport>/jwt/<configId>)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "issuer"
     * <LI>The failure messages from our attempt to invoke "issuer"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @SkipForRepeat(JwtBuilderClaimRepeatActions.SingleID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_issuer_empty() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.ISSUER, "");
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6009E_INVALID_CLAIM, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    //chc@Test
    public void yyy() throws Exception {

        JSONObject parmx = new JSONObject();
        parmx.put(ClaimConstants.ISSUER, "xx");
        parmx.put(ClaimConstants.AUDIENCE, "yy");
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(JwtConstants.JWT_BUILDER_CLAIM_API, parmx);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, "jwt1", claimsToSet);

    }

    /***************************************************** Test claim ****************************************************/
    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a good value
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated claim (value should be someClaim:someValue)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_one() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put("someClaim", "someValue");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a good value
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated claim (claims should contain someClaim:someValue anotherClaim:anotherValue
     * stillOneMoreClaim:stillOneMoreValue)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_multiple() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put("someClaim", "someValue");
        claimsToSet.put("anotherClaim", "anotherValue");
        claimsToSet.put("stillOneMoreClaim", "stillOneMoreValue");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a good value
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated claim (claims should contain azp:someParty)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_azp_causeItsSpecial() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(ClaimConstants.AUTHORIZED_PARTY, "someParty");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a bad value (value is <null>)
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have any updates to claim
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The failure messages from our attempt to invoke "claim"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    // test is validating that a null collection fails appropriately, so, skip if adding single claim (key,value) pairs
    //    @SkipForRepeat(JwtBuilderClaimRepeatActions.CollectionID)
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_null() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        String msgId = null;
        if (processClaimsAs.equals(JwtConstants.AS_COLLECTION)) {
            // message received when null map passed
            msgId = JwtMessageConstants.CWWKS6021E_CLAIMS_ARE_NOT_VALID;
        } else {
            // message received when key value is null
            msgId = JwtMessageConstants.CWWKS6015E_INVALID_CLAIM;
        }
        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, msgId, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a bad value (List of key:values, one value is <null>)
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have any updated claims with valid claims prior to the null (check for someClaim:someValue and no
     * more claims)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The failure messages from our attempt to invoke "claim"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_nullValueInList() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put("someClaim", "someValue");
        claimsToSet.put("anotherClaim", null);
        claimsToSet.put("stillOneMoreClaim", "stillOneMoreValue");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6009E_INVALID_CLAIM, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    //    /**
    //     * <p>
    //     * Test Purpose:
    //     * <OL>
    //     * <LI>Create a builder using the specified configId (a generic config used for most tests)
    //     * <LI>Run the claim api to update the builder with a bad value (List of key:values, one key is <null>)
    //     * <LI>generate a JWT token
    //     * </OL>
    //     * <P>
    //     * Expected Results:
    //     * <OL>
    //     * <LI>The builder should be created with default values as there is not much defined in the specified config
    //     * <LI>The builder should have any updated claims with valid claims prior to the null (check for someClaim:someValue and no
    //     * more claims)
    //     * <LI>The JWT Token should be created based on the builder
    //     * <LI>The JWT Token will be used to display the claim values
    //     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
    //     * <LI>The test case will validate the content of:
    //     * <UL>
    //     * <LI>The messages logged indicating that we were invoking "claim"
    //     * <LI>The failure messages from our attempt to invoke "claim"
    //     * <LI>The content of the returned token
    //     * <LI>The output from running the query apis
    //     * </UL>
    //     * </OL>
    //     */
    //    //chc@Test
    //    public void JwtBuilderAPIBasicTests_claim_nullKeyInList() throws Exception {
    //
    //        String builderId = "jwt1";
    //        JwtClaims claims = BuilderHelpers.setDefaultClaims(builderServer);
    //        claims.setIssuer(builderId);
    //        claims.setClaim("someClaim", "someValue");
    //        claims.setClaim(JWTBuilderConstants.NULL_STRING, "anotherValue");
    //        claims.setClaim("stillOneMoreClaim", "stillOneMoreValue");
    //
    //        Expectations expectations = new Expectations();
    //        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(SecurityFatHttpUtils.getServerUrlBase(builderServer) + JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT));
    //        expectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, JwtMessageConstants.CWWKS6015E_INVALID_CLAIM, "Response did not show the expected failure."));
    //
    //        JSONObject claimMap = new JSONObject();
    //        claimMap.put("someClaim", "someValue");
    //        claimMap.put(JWTBuilderConstants.NULL_STRING, "anotherValue");
    //        claimMap.put("stillOneMoreClaim", "stillOneMoreValue");
    //        JSONObject claimsToSet = new JSONObject();
    //        claimsToSet.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimMap);
    //
    //        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
    //        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
    //        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, claimsToSet);
    //        validationUtils.validateResult(response, expectations);
    //
    //    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with an empty value (List of key:values, one value is "")
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have any updated claims with valid claims (check for someClaim:someValue
     * anotherClaim: stillOneMoreClaim:stillOneMoreValue)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_emptyValueInList() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put("someClaim", "someValue");
        claimsToSet.put("anotherClaim", "");
        claimsToSet.put("stillOneMoreClaim", "stillOneMoreValue");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a bad value (List of key:values, one key is "")
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have any updated claims with valid claims prior to the null (check for someClaim:someValue and no
     * more claims)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The failure messages from our attempt to invoke "claim"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_emptyKeyInList() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put("someClaim", "someValue");
        claimsToSet.put("", "anotherValue");
        claimsToSet.put("stillOneMoreClaim", "stillOneMoreValue");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6015E_INVALID_CLAIM, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a good value of the correct type
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated claim (exp) (claim should contain exp:2107268760)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_exp_long() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(ClaimConstants.EXPIRATION_TIME, testExp);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a value of an invalid type
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should not update claim (exp) (claim should contain exp:<current time + expiry>)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The failure messages from our attempt to invoke "claim"
     * <LI>The content of the returned token (reflecting values showing that the failure did NOT mangle the builder contents)
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_exp_String() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(ClaimConstants.EXPIRATION_TIME, Long.toString(testExp));
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6019E_BAD_DATA_TYPE, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);
    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a good value of the correct type
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated claim (iat) (claim should contain iat:<approx current time)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_iat_long() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(ClaimConstants.ISSUED_AT, BuilderHelpers.setNowLong() + Long.valueOf(5 * 60));
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a value of an invalid type
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should not update claim (iat) (claim should contain iat:<current time>)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The failure messages from our attempt to invoke "claim"
     * <LI>The content of the returned token (reflecting values showing that the failure did NOT mangle the builder contents)
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_iat_String() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(ClaimConstants.ISSUED_AT, Long.toString(BuilderHelpers.setNowLong() + Long.valueOf(5 * 60)));
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6019E_BAD_DATA_TYPE, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a good value of the correct type
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated claim (nbf) (claim should contain nbf:<approx current time>)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_nbf_long() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(ClaimConstants.NOT_BEFORE, BuilderHelpers.setNowLong() + Long.valueOf(5 * 60));
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);
    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a value of an invalid type
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should not update claim (nbf) (claims should NOT contain nbf)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The failure messages from our attempt to invoke "claim"
     * <LI>The content of the returned token (reflecting values showing that the failure did NOT mangle the builder contents)
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_nbf_String() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(ClaimConstants.NOT_BEFORE, Long.toString(BuilderHelpers.setNowLong() + Long.valueOf(5 * 60)));
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6019E_BAD_DATA_TYPE, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a good value of the correct type
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated claim (iss) (claim should contain iss:JohnQIssuer)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_iss_String() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(ClaimConstants.ISSUER, "JohnQIssuer");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a value of an invalid type
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should not update claim (iss) (claim should contain iss:<https://<hostname>:<port>/jwt/<configId>>)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The failure messages from our attempt to invoke "claim"
     * <LI>The content of the returned token (reflecting values showing that the failure did NOT mangle the builder contents)
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_iss_Long() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(ClaimConstants.ISSUER, testExp);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6019E_BAD_DATA_TYPE, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a good value of the correct type
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated claim (token_type) (claim should contain token_type:myType)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_token_type_String() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(ClaimConstants.TOKEN_TYPE, "myType");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a value of an invalid type
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should not update claim (token_type) (claim should contain token_type:Bearer)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The failure messages from our attempt to invoke "claim"
     * <LI>The content of the returned token (reflecting values showing that the failure did NOT mangle the builder contents)
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_token_type_Long() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(ClaimConstants.TOKEN_TYPE, testExp);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6019E_BAD_DATA_TYPE, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a good value of the correct type
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated claim (sub) (claim should contain sub:buddy)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_sub_String() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(ClaimConstants.SUBJECT, "buddy");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a value of an invalid type
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should not update claim (sub) (claim should NOT contain sub)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The failure messages from our attempt to invoke "claim"
     * <LI>The content of the returned token (reflecting values showing that the failure did NOT mangle the builder contents)
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_sub_Long() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(ClaimConstants.SUBJECT, testExp);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6019E_BAD_DATA_TYPE, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a good value of the correct type
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated claim (jti) (claim should contain jti:lJ7GKhJCLrY5y5BL)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_jti_String() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(ClaimConstants.JWT_ID, "lJ7GKhJCLrY5y5BL");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a value of an invalid type
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should not update claim (jti) (claims should NOT contain jti)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The failure messages from our attempt to invoke "claim"
     * <LI>The content of the returned token (reflecting values showing that the failure did NOT mangle the builder contents)
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @ExpectedFFDC({ "com.ibm.ws.security.jwt.internal.JwtTokenException" })
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_jti_Long() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(ClaimConstants.JWT_ID, testExp);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6020E_CAN_NOT_CAST, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);
    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a good value of the correct type
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated claim (alg) (claim should contain alg:BS256)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    // TODO - this is creating an alg claim in the payload, not updating the alg in the header - with the tooling, it will be funky to add expectations
    // for this case - does it really show anything???
    //    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_alg_String() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(HeaderConstants.ALGORITHM, "BS256");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the claim api to update the builder with a good value of the correct type
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated claim (kid) (claim should contain kid:983457399)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "claim"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claim_kid_String() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);
        //        configSettings.put(HeaderConstants.KEY_ID, "");

        // key_id is really a header attribute, but, we're add a claim to the payload with a value, so, we can't use the
        // normal tooling to add an expectation for it.  We won't add it to the settings that we build the expectations from
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(HeaderConstants.KEY_ID, "983457399");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        //        configSettings.put("overrideSettings", testSettings);
        // now, create expecations
        expectations = ClaimHelpers.updateClaimExpectationsForJsonAttribute(expectations, JwtConstants.JWT_BUILDER_CLAIM, HeaderConstants.KEY_ID, BuilderHelpers.getVerboseName(HeaderConstants.KEY_ID), claimsToSet.get(HeaderConstants.KEY_ID));

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /***************************************************** Test remove ****************************************************/
    /***
     * Test Purpose:
     * <OL>
     * <LI>Test that the remove method actually removes the specified claim
     * <LI>Remove an "extra" claim that we've previously added
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The "extra" claim should NOT exist in the JWT Token
     * </OL>
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_remove_extraClaim() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        // add an extra claim
        claimsToSet.put("extraClaim", "myValue");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        JSONArray claimsToRemove = new JSONArray();
        claimsToRemove.add("extraClaim");
        testSettings.put(JwtConstants.JWT_BUILDER_REMOVE_API, claimsToRemove);

        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Test that the remove method actually removes the specified claim
     * <LI>Remove an "extra" claim that we have NOT added
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The "extra" claim should NOT exist in the JWT Token
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_remove_nonExistant_extraClaim() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims to remove into a json array.  Add that array into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONArray claimsToRemove = new JSONArray();
        claimsToRemove.add("extraClaim");
        testSettings.put(JwtConstants.JWT_BUILDER_REMOVE_API, claimsToRemove);

        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Test that the remove method actually removes the specified claim
     * <LI>Remove default claim "exp"
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The "exp" claim should NOT exist in the JWT Token
     * </OL>
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_remove_defaultClaim_exp() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims to remove into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        // add an extra claim
        claimsToSet.put(ClaimConstants.EXPIRATION_TIME, testExp);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        JSONArray claimsToRemove = new JSONArray();
        claimsToRemove.add(ClaimConstants.EXPIRATION_TIME);
        testSettings.put(JwtConstants.JWT_BUILDER_REMOVE_API, claimsToRemove);

        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Test that the remove method actually removes the specified claim
     * <LI>Remove default claim "iss"
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The "iss" claim should NOT exist in the JWT Token
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_remove_defaultClaim_iss() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims to remove into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONArray claimsToRemove = new JSONArray();
        claimsToRemove.add(ClaimConstants.ISSUER);
        testSettings.put(JwtConstants.JWT_BUILDER_REMOVE_API, claimsToRemove);

        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Test that the remove method actually removes the specified claim
     * <LI>Remove default claim "iat"
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The "iat" claim should NOT exist in the JWT Token
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_remove_defaultClaim_iat() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims to remove into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONArray claimsToRemove = new JSONArray();
        claimsToRemove.add(ClaimConstants.ISSUED_AT);
        testSettings.put(JwtConstants.JWT_BUILDER_REMOVE_API, claimsToRemove);

        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Test that the remove method actually removes the specified claim
     * <LI>Remove default claim "token_type"
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The "token_type" claim should NOT exist in the JWT Token
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_remove_defaultClaim_tokenType() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims to remove into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONArray claimsToRemove = new JSONArray();
        claimsToRemove.add(ClaimConstants.TOKEN_TYPE);
        testSettings.put(JwtConstants.JWT_BUILDER_REMOVE_API, claimsToRemove);

        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Test that the remove fails when we try to remove <null>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The claims in the token should NOT have been altered
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_remove_null() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims to remove into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONArray claimsToRemove = new JSONArray();
        claimsToRemove.add(null);
        testSettings.put(JwtConstants.JWT_BUILDER_REMOVE_API, claimsToRemove);

        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6015E_INVALID_CLAIM, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Test that the remove fails when we try to remove ""
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The claims in the token should NOT have been altered
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_remove_empty() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims to remove into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONArray claimsToRemove = new JSONArray();
        claimsToRemove.add("");
        testSettings.put(JwtConstants.JWT_BUILDER_REMOVE_API, claimsToRemove);

        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6015E_INVALID_CLAIM, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Test that nbf set via the notBefore api is removed when the remove api is called for nbf
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The "nbf" claim is added and is then removed and should NOT exist in the JWT Token
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_remove_apiClaim_nbf() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims to remove into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        // add an extra claim
        claimsToSet.put(ClaimConstants.NOT_BEFORE, 2106325918L);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        JSONArray claimsToRemove = new JSONArray();
        claimsToRemove.add(ClaimConstants.NOT_BEFORE);
        testSettings.put(JwtConstants.JWT_BUILDER_REMOVE_API, claimsToRemove);

        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Test that sub set via the subject api is removed when the remove api is called for sub
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The "sub" claim is added and is then removed and should NOT exist in the JWT Token
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_remove_apiClaim_sub() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims to remove into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONObject claimsToSet = new JSONObject();
        // add an extra claim
        claimsToSet.put(ClaimConstants.SUBJECT, "user2");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        JSONArray claimsToRemove = new JSONArray();
        claimsToRemove.add(ClaimConstants.SUBJECT);
        testSettings.put(JwtConstants.JWT_BUILDER_REMOVE_API, claimsToRemove);

        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Test that aud set via the audience api is removed when the remove api is called for aud
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The "aud" claim is added and is then removed and should NOT exist in the JWT Token
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_remove_apiClaim_aud() throws Exception {

        String builderId = "jwt1";
        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);

        JSONArray parmarray = new JSONArray();
        parmarray.add("Client02");
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.AUDIENCE, parmarray);
        JSONArray claimsToRemove = new JSONArray();
        claimsToRemove.add(ClaimConstants.AUDIENCE);
        testSettings.put(JwtConstants.JWT_BUILDER_REMOVE_API, claimsToRemove);

        settings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Test that jti set via the jwtId api is removed when the remove api is called for jti
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The "jti" claim is added and is then removed and should NOT exist in the JWT Token
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_remove_apiClaim_jti() throws Exception {

        String builderId = "jwt1";
        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.JWT_ID, true);
        settings.put("overrideSettings", testSettings);

        JSONArray claimsToRemove = new JSONArray();
        claimsToRemove.add(ClaimConstants.JWT_ID);
        testSettings.put(JwtConstants.JWT_BUILDER_REMOVE_API, claimsToRemove);

        settings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    // TODO - move this
    //    public String getBuiltToken(String builderId) throws Exception {
    //        return getBaseToken(builderId, JWTBuilderConstants.BUILT_JWT_TOKEN);
    //    }
    //
    //    public String getAltToken(String builderId) throws Exception {
    //        return getBaseToken(builderId, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM + ": ");
    //    }
    //
    //    public String getBaseToken(String builderId, String prefix) throws Exception {
    public String getBaseToken(String builderId) throws Exception {

        Page response = actions.invokeJwtBuilder_create(_testName, builderServer, builderId);
        String jwtToken = actions.extractJwtTokenFromResponse(response, JWTBuilderConstants.BUILT_JWT_TOKEN);
        Log.info(thisClass, _testName, "Token From Response: " + jwtToken);
        return jwtToken;
        //        JSONObject baseSettings = BuilderHelpers.setClaimsFromToken(jwtToken);
        //Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_CREATE_ENDPOINT, JwtConstants.JWT_BUILDER_CLAIM, baseSettings, builderServer);
        //validationUtils.validateResult(response, expectations);

    }

    public String getDecodedPayload(String jwtToken) throws Exception {

        String payload = getPayload(jwtToken);
        String decodedPayload = new String(Base64.getDecoder().decode(payload), "UTF-8");
        return decodedPayload;
    }

    public String getPayload(String jwtToken) throws Exception {
        String[] jwtParts = jwtToken.split(Pattern.quote("."));
        String payload = jwtParts[1];
        return payload;
    }

    /*****************************************************
     * Test various claimsFrom
     ****************************************************/
    /**
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and create a JWT Token (in the servlet)
     * <LI>In the same server instance, use <config2> to create another builder.
     * <LI>Use the claimsFrom api to load all claims from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that the second returned JWT Token contains all of the claims from the original token
     * </OL>
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtToken_allClaims() throws Exception {

        String baseBuilderId = "altJwt1";
        // The test code can't really convert the jwt string into the jwt token, so, tell the
        // test app to create a jwt token from another jwt builder config
        // use that as the source for the claimFrom call.
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_PARAM_BUILDER_ID, baseBuilderId);
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_TOKEN);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using one builder), then create a builder for another builder, load all claims from the token into the second builder
        String builderId = "jwt1";
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        // extract the first jwt token from the output and use that to create expectations.  We'll compare the content of the second token
        // to that of the first (since everything from the original token was obtained via claimFrom(<jwtToken>), they should be the same
        String jwtToken = actions.extractJwtTokenFromResponse(response, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM + ": ");
        JSONObject baseSettings = BuilderHelpers.setClaimsFromToken(jwtToken);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, baseSettings, builderServer);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and return the JWT Token string.
     * <LI>Invoke the builder client servlet again passing in the 3 part token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load all claims from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that the second returned JWT Token contains all of the claims from the original token
     * </OL>
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_allClaims() throws Exception {

        // build a token using the alternate builder config (we'll get claims from it)
        String baseBuilderId = "altJwt1";
        String jwtToken = getBaseToken(baseBuilderId);
        JSONObject baseSettings = BuilderHelpers.setClaimsFromToken(jwtToken);
        // since we getting all claims in the case, we can base our expectations upon the content of this first token)
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, baseSettings, builderServer);

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, jwtToken);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        String builderId = "jwt1";
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);

    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and return the JWT Token string.
     * <LI>Invoke the builder client servlet again passing in the encoded payload of the token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load all claims from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that the second returned JWT Token contains all of the claims from the original token
     * </OL>
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_encodedPayload_allClaims() throws Exception {

        // build a token using the alternate builder config (we'll get claims from it)
        String baseBuilderId = "altJwt1";
        String jwtToken = getBaseToken(baseBuilderId);
        JSONObject baseSettings = BuilderHelpers.setClaimsFromToken(jwtToken);
        // since we getting all claims in the case, we can base our expectations upon the content of this first token)
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, baseSettings, builderServer);

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        // just pass the encoded payload
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, getPayload(jwtToken));

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        String builderId = "jwt1";
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);

    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and return the JWT Token string.
     * <LI>Invoke the builder client servlet again passing in the decoded payload of the token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load all claims from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that the second returned JWT Token contains all of the claims from the original token
     * </OL>
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_decodedPayload_allClaims() throws Exception {

        // build a token using the alternate builder config (we'll get claims from it)
        String baseBuilderId = "altJwt1";
        String jwtToken = getBaseToken(baseBuilderId);
        JSONObject baseSettings = BuilderHelpers.setClaimsFromToken(jwtToken);
        // since we getting all claims in the case, we can base our expectations upon the content of this first token)
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, baseSettings, builderServer);

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        // just pass the decoded payload
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, getDecodedPayload(jwtToken));

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        String builderId = "jwt1";
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);

    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and create a JWT Token (in the servlet)
     * <LI>In the same server instance, use <config2> to create another builder.
     * <LI>Use the claimsFrom api to load specific claims from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that the second returned JWT Token contains the specific claims from the original token
     * </OL>
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtToken_specificClaims() throws Exception {

        String baseBuilderId = "altJwt1";
        // The test code can't really convert the jwt string into the jwt token, so, tell the
        // test app to create a jwt token from another jwt builder config
        // use that as the source for the claimFrom call.
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_PARAM_BUILDER_ID, baseBuilderId);
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_TOKEN);
        // build a list of claims that we want to use claimFrom with (claims that we want to copy from the original token to the new token)
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add(ClaimConstants.JWT_ID);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using one builder), then create a builder for another builder, load all claims from the token into the second builder
        String builderId = "jwt1";
        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);
        settings.put("overrideSettings", testSettings);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        // extract the first jwt token from the output and use that to create expectations.  We'll compare the content of the second token
        // to that of the first (since everything from the original token was obtained via claimFrom(<jwtToken>), they should be the same
        String jwtToken = actions.extractJwtTokenFromResponse(response, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM + ": ");
        JSONObject baseSettings = BuilderHelpers.setClaimsFromToken(jwtToken);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, baseSettings, builderServer);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and return the JWT Token string.
     * <LI>Invoke the builder client servlet again passing in the 3 part token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load specific claims from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that the second returned JWT Token contains the specific claims from the original token
     * </OL>
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_specificClaims() throws Exception {

        // build a token using the alternate builder config (we'll get claims from it)
        String baseBuilderId = "altJwt1";
        String jwtToken = getBaseToken(baseBuilderId);
        JSONObject baseSettings = BuilderHelpers.setClaimsFromToken(jwtToken);

        String builderId = "jwt1";
        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, jwtToken);
        // build a list of claims that we want to use claimFrom with (claims that we want to copy from the original token to the new token)
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add(ClaimConstants.JWT_ID);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        settings.put("overrideSettings", testSettings);
        // since we getting all claims in the case, we can base our expectations upon the content of this first token)
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, baseSettings, builderServer);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and return the JWT Token string.
     * <LI>Invoke the builder client servlet again passing in the encoded payload of the token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load specific claims from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that the second returned JWT Token contains the specific claims from the original token
     * </OL>
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_encodedPayload_specificClaims() throws Exception {

        // build a token using the alternate builder config (we'll get claims from it)
        String baseBuilderId = "altJwt1";
        String jwtToken = getBaseToken(baseBuilderId);
        JSONObject baseSettings = BuilderHelpers.setClaimsFromToken(jwtToken);

        String builderId = "jwt1";
        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, getPayload(jwtToken));
        // build a list of claims that we want to use claimFrom with (claims that we want to copy from the original token to the new token)
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add(ClaimConstants.JWT_ID);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        settings.put("overrideSettings", testSettings);
        // since we getting all claims in the case, we can base our expectations upon the content of this first token)
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, baseSettings, builderServer);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and return the JWT Token string.
     * <LI>Invoke the builder client servlet again passing in the decoded payload of the token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load specific claims from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that the second returned JWT Token contains the specific claims from the original token
     * </OL>
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_decodedPayload_specificClaims() throws Exception {

        // build a token using the alternate builder config (we'll get claims from it)
        String baseBuilderId = "altJwt1";
        String jwtToken = getBaseToken(baseBuilderId);
        JSONObject baseSettings = BuilderHelpers.setClaimsFromToken(jwtToken);

        String builderId = "jwt1";
        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, getDecodedPayload(jwtToken));
        // build a list of claims that we want to use claimFrom with (claims that we want to copy from the original token to the new token)
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add(ClaimConstants.JWT_ID);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        settings.put("overrideSettings", testSettings);
        // since we getting all claims in the case, we can base our expectations upon the content of this first token)
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, baseSettings, builderServer);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);

    }

    /**************************/
    /**
     * Test Purpose:
     * <OL>
     * <LI>Use <config2> to create a
     * <LI>Use the claimsFrom api to try to load all claims from a null token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that we get the correct failures trying to run claimFrom
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtToken_null_allClaims() throws Exception {

        String baseBuilderId = "altJwt1";
        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it the builder id and indicate the form that the jwt should be in when it's passed
        // to the api (it takes JwtToken, or String (the string can be the 3 part token, or the payload
        // only - decoded or encoded
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_PARAM_BUILDER_ID, baseBuilderId);
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_TOKEN_NULL);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using one builder), then create a builder for another builder, load all claims from the token into the second builder
        String builderId = "jwt1";
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6017E_BAD_EMPTY_TOKEN, builderServer);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Use <config2> to create a
     * <LI>Use the claimsFrom api to try to load specific claims from a null token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that we get the correct failures trying to run claimFrom
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtToken_null_specificClaims() throws Exception {

        String baseBuilderId = "altJwt1";
        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it the builder id and indicate the form that the jwt should be in when it's passed
        // to the api (it takes JwtToken, or String (the string can be the 3 part token, or the payload
        // only - decoded or encoded
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_PARAM_BUILDER_ID, baseBuilderId);
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_TOKEN_NULL);
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add(ClaimConstants.JWT_ID);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using one builder), then create a builder for another builder, load all claims from the token into the second builder
        String builderId = "jwt1";
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6017E_BAD_EMPTY_TOKEN, builderServer);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and create a JWT Token (in the servlet)
     * <LI>In the same server instance, use <config2> to create another builder.
     * <LI>Use the claimsFrom api to load a <null> claim from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that we get the correct failures trying to run claimFrom
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtToken_nullSpecificClaims() throws Exception {

        String baseBuilderId = "altJwt1";
        // The test code can't really convert the jwt string into the jwt token, so, tell the
        // test app to create a jwt token from another jwt builder config
        // use that as the source for the claimFrom call.
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_PARAM_BUILDER_ID, baseBuilderId);
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_TOKEN);
        // build a list of claims that we want to use claimFrom with (claims that we want to copy from the original token to the new token)
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add(null);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using one builder), then create a builder for another builder, load all claims from the token into the second builder
        String builderId = "jwt1";
        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);
        settings.put("overrideSettings", testSettings);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        // extract the first jwt token from the output and use that to create expectations.  We'll compare the content of the second token
        // to that of the first (since everything from the original token was obtained via claimFrom(<jwtToken>), they should be the same
        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6015E_INVALID_CLAIM, builderServer);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and create a JWT Token (in the servlet)
     * <LI>In the same server instance, use <config2> to create another builder.
     * <LI>Use the claimsFrom api to load a "" claim from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that we get the correct failures trying to run claimFrom
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtToken_emptySpecificClaims() throws Exception {

        String baseBuilderId = "altJwt1";
        // The test code can't really convert the jwt string into the jwt token, so, tell the
        // test app to create a jwt token from another jwt builder config
        // use that as the source for the claimFrom call.
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_PARAM_BUILDER_ID, baseBuilderId);
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_TOKEN);
        // build a list of claims that we want to use claimFrom with (claims that we want to copy from the original token to the new token)
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add("");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using one builder), then create a builder for another builder, load all claims from the token into the second builder
        String builderId = "jwt1";
        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);
        settings.put("overrideSettings", testSettings);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        // extract the first jwt token from the output and use that to create expectations.  We'll compare the content of the second token
        // to that of the first (since everything from the original token was obtained via claimFrom(<jwtToken>), they should be the same
        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6015E_INVALID_CLAIM, builderServer);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and create a JWT Token (in the servlet)
     * <LI>In the same server instance, use <config2> to create another builder.
     * <LI>Use the claimsFrom api to load a non-existant claim from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that we do NOT find a claim for the requested claimFrom
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtToken_nonExistantSpecificClaim() throws Exception {

        String baseBuilderId = "altJwt1";
        // The test code can't really convert the jwt string into the jwt token, so, tell the
        // test app to create a jwt token from another jwt builder config
        // use that as the source for the claimFrom call.
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_PARAM_BUILDER_ID, baseBuilderId);
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_TOKEN);
        // build a list of claims that we want to use claimFrom with (claims that we want to copy from the original token to the new token)
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add("someClaim");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using one builder), then create a builder for another builder, load all claims from the token into the second builder
        String builderId = "jwt1";
        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);
        settings.put("overrideSettings", testSettings);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        // extract the first jwt token from the output and use that to create expectations.  We'll compare the content of the second token
        // to that of the first (since everything from the original token was obtained via claimFrom(<jwtToken>), they should be the same
        String jwtToken = actions.extractJwtTokenFromResponse(response, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM + ": ");
        JSONObject baseSettings = BuilderHelpers.setClaimsFromToken(jwtToken);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, baseSettings, builderServer);

        validationUtils.validateResult(response, expectations);

    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Invoke the builder client servlet passing in the <null> as the token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load all claims from the null token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that we get the correct failures trying to run claimFrom
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_null_allClaims() throws Exception {

        // don't bother building a token from another build to get claims from - we're testing claimFrom(null)

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6017E_BAD_EMPTY_TOKEN, builderServer);

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        // just pass a null string
        String nullString = null;
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, nullString);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        String builderId = "jwt1";
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);

    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Invoke the builder client servlet passing in the <null> as the token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load specific claims from the null token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that we get the correct failures trying to run claimFrom
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_null_specificClaims() throws Exception {

        // don't bother building a token from another build to get claims from - we're testing claimFrom(null)

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6017E_BAD_EMPTY_TOKEN, builderServer);

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        // just pass a null string
        String nullString = null;
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, nullString);
        // build a list of claims that we want to use claimFrom with (claims that we want to copy from the original token to the new token)
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add(ClaimConstants.JWT_ID);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        String builderId = "jwt1";
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);

    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Invoke the builder client servlet passing in the "" as the token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load all claims from the empty token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that we get the correct failures trying to run claimFrom
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_empty_allClaims() throws Exception {

        // don't bother building a token from another build to get claims from - we're testing claimFrom("")

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6017E_BAD_EMPTY_TOKEN, builderServer);

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        // just pass a empty string
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, "");

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        String builderId = "jwt1";
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);
    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Invoke the builder client servlet passing in the "" as the token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load specific claims from the empty token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that we get the correct failures trying to run claimFrom
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_empty_specificClaims() throws Exception {

        // don't bother building a token from another build to get claims from - we're testing claimFrom("")

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6017E_BAD_EMPTY_TOKEN, builderServer);

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        // just pass a empty string
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, "");
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add(ClaimConstants.JWT_ID);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        String builderId = "jwt1";
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);

    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Invoke the builder client servlet passing in garbage as the token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load all claims from the garbae token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that we get the correct failures trying to run claimFrom
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    @ExpectedFFDC("org.jose4j.lang.JoseException")
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_garbage_allClaims() throws Exception {

        // don't bother building a token from another builder to get claims from - we're testing claimFrom("foo.foo.foo") (garbage)

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6017E_BAD_EMPTY_TOKEN, builderServer);

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        // just pass a empty string
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, "foo.foo.foo");

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        String builderId = "jwt1";
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);

    }

    /***
     * Test Purpose:
     * <OL>
     * <LI>Invoke the builder client servlet passing in garbage as the token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load all claims from the garbage token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that we get the correct failures trying to run claimFrom
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    @ExpectedFFDC("org.jose4j.lang.JoseException")
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_garbage_specificClaims() throws Exception {

        // don't bother building a token from another builder to get claims from - we're testing claimFrom("foo.foo.foo") (garbage)

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6017E_BAD_EMPTY_TOKEN, builderServer);

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        // just pass a empty string
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, "foo.foo.foo");
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add(ClaimConstants.JWT_ID);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        String builderId = "jwt1";
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and return the JWT Token string.
     * <LI>Invoke the builder client servlet again passing in the 3 part token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load <null> claims from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that we get the correct failures trying to run claimFrom
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_nullSpecificClaims() throws Exception {

        // build a token using the alternate builder config (we'll get claims from it)
        String baseBuilderId = "altJwt1";
        String jwtToken = getBaseToken(baseBuilderId);

        String builderId = "jwt1";

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, jwtToken);
        // build a list of claims that we want to use claimFrom with (claims that we want to copy from the original token to the new token)
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add(null);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        // since we getting all claims in the case, we can base our expectations upon the content of this first token)
        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6015E_INVALID_CLAIM, builderServer);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and return the JWT Token string.
     * <LI>Invoke the builder client servlet again passing in the 3 part token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load "" claims from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that we get the correct failures trying to run claimFrom
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_emptySpecificClaims() throws Exception {

        // build a token using the alternate builder config (we'll get claims from it)
        String baseBuilderId = "altJwt1";
        String jwtToken = getBaseToken(baseBuilderId);

        String builderId = "jwt1";

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, jwtToken);
        // build a list of claims that we want to use claimFrom with (claims that we want to copy from the original token to the new token)
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add("");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        // since we getting all claims in the case, we can base our expectations upon the content of this first token)
        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6015E_INVALID_CLAIM, builderServer);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and return the JWT Token string.
     * <LI>Invoke the builder client servlet again passing in the 3 part token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load non-existaint claims from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_nonExistantSpecificClaim() throws Exception {

        // build a token using the alternate builder config (we'll get claims from it)
        String baseBuilderId = "altJwt1";
        String jwtToken = getBaseToken(baseBuilderId);
        JSONObject baseSettings = BuilderHelpers.setClaimsFromToken(jwtToken);

        String builderId = "jwt1";
        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, jwtToken);
        // build a list of claims that we want to use claimFrom with (claims that we want to copy from the original token to the new token)
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add("someClaim");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        settings.put("overrideSettings", testSettings);
        // since we getting all claims in the case, we can base our expectations upon the content of this first token)
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, baseSettings, builderServer);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and return the JWT Token string.
     * <LI>Invoke the builder client servlet again passing in the encoded payload part token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load <null> claims from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that we get the correct failures trying to run claimFrom
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_encodedPayload_nullSpecificClaims() throws Exception {

        // build a token using the alternate builder config (we'll get claims from it)
        String baseBuilderId = "altJwt1";
        String jwtToken = getBaseToken(baseBuilderId);

        String builderId = "jwt1";

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, getPayload(jwtToken));
        // build a list of claims that we want to use claimFrom with (claims that we want to copy from the original token to the new token)
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add(null);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        // since we getting all claims in the case, we can base our expectations upon the content of this first token)
        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6015E_INVALID_CLAIM, builderServer);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and return the JWT Token string.
     * <LI>Invoke the builder client servlet again passing in the encoded payload part token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load "" claims from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that we get the correct failures trying to run claimFrom
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_encodedPayload_emptySpecificClaims() throws Exception {

        // build a token using the alternate builder config (we'll get claims from it)
        String baseBuilderId = "altJwt1";
        String jwtToken = getBaseToken(baseBuilderId);

        String builderId = "jwt1";

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, getPayload(jwtToken));
        // build a list of claims that we want to use claimFrom with (claims that we want to copy from the original token to the new token)
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add("");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        // since we getting all claims in the case, we can base our expectations upon the content of this first token)
        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6015E_INVALID_CLAIM, builderServer);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and return the JWT Token string.
     * <LI>Invoke the builder client servlet again passing in the encoded payload part token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load non-existant claims from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_encodedPayload_nonExistantSpecificClaim() throws Exception {

        // build a token using the alternate builder config (we'll get claims from it)
        String baseBuilderId = "altJwt1";
        String jwtToken = getBaseToken(baseBuilderId);
        JSONObject baseSettings = BuilderHelpers.setClaimsFromToken(jwtToken);

        String builderId = "jwt1";
        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, getPayload(jwtToken));
        // build a list of claims that we want to use claimFrom with (claims that we want to copy from the original token to the new token)
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add("someClaim");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        settings.put("overrideSettings", testSettings);
        // since we getting all claims in the case, we can base our expectations upon the content of this first token)
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, baseSettings, builderServer);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and return the JWT Token string.
     * <LI>Invoke the builder client servlet again passing in the decoded payload part token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load <null> claims from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that we get the correct failures trying to run claimFrom
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_decodedPayload_nullSpecificClaims() throws Exception {

        // build a token using the alternate builder config (we'll get claims from it)
        String baseBuilderId = "altJwt1";
        String jwtToken = getBaseToken(baseBuilderId);

        String builderId = "jwt1";

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, getDecodedPayload(jwtToken));
        // build a list of claims that we want to use claimFrom with (claims that we want to copy from the original token to the new token)
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add(null);
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        // since we getting all claims in the case, we can base our expectations upon the content of this first token)
        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6015E_INVALID_CLAIM, builderServer);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and return the JWT Token string.
     * <LI>Invoke the builder client servlet again passing in the decoded payload part token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load "" claims from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that we get the correct failures trying to run claimFrom
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_decodedPayload_emptySpecificClaims() throws Exception {

        // build a token using the alternate builder config (we'll get claims from it)
        String baseBuilderId = "altJwt1";
        String jwtToken = getBaseToken(baseBuilderId);

        String builderId = "jwt1";

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, getDecodedPayload(jwtToken));
        // build a list of claims that we want to use claimFrom with (claims that we want to copy from the original token to the new token)
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add("");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        // since we getting all claims in the case, we can base our expectations upon the content of this first token)
        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6015E_INVALID_CLAIM, builderServer);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Create a JWT builder using <config1> and return the JWT Token string.
     * <LI>Invoke the builder client servlet again passing in the decoded payload part token
     * <LI>Have the client use <config2> when it creates the builder.
     * <LI>Use the claimsFrom api to load non-existant claims from the original token
     * <LI>Build another token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Verify that the second token contains cliams appropriate for the config that the token is based on...
     * </OL>
     *
     * @throws Exception
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_claimsFrom_jwtString_decodedPayload_nonExistantSpecificClaim() throws Exception {

        // build a token using the alternate builder config (we'll get claims from it)
        String baseBuilderId = "altJwt1";
        String jwtToken = getBaseToken(baseBuilderId);
        JSONObject baseSettings = BuilderHelpers.setClaimsFromToken(jwtToken);

        String builderId = "jwt1";
        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);

        // build settings that will tell the test app how to run/what to pass to the "claimFrom" api
        // give it a flag that says jwt string, and then pass the 3 part jwt token string
        JSONObject testSettings = new JSONObject();
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM, JWTBuilderConstants.JWT_BUILDER_ACTION_CLAIM_FROM_JWT_STRING);
        testSettings.put(JWTBuilderConstants.JWT_TOKEN, getDecodedPayload(jwtToken));
        // build a list of claims that we want to use claimFrom with (claims that we want to copy from the original token to the new token)
        JSONArray claimsFrom = new JSONArray();
        claimsFrom.add("someClaim");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIMFROM_API, claimsFrom);

        settings.put("overrideSettings", testSettings);
        // since we getting all claims in the case, we can base our expectations upon the content of this first token)
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, baseSettings, builderServer);

        // Now, add any override values - for this test, there are none
        // Invoke the builder app to create a token (using (a second) builder, load all claims from the token into the second builder
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);

        validationUtils.validateResult(response, expectations);
    }

    /***************************************************** Test signWith ****************************************************/
    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the signWith api to update the builder with a good value
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated signWith (value should be created from sigALg HS256/signingKey "useThisToSign")
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "signWith"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_signWith_sigAlg_HS256_key_string() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(HeaderConstants.ALGORITHM, JwtConstants.SIGALG_HS256);
        testSettings.put(JwtConstants.SHARED_KEY, "useThisToSign");
        testSettings.put(JwtConstants.SHARED_KEY_TYPE, JwtConstants.SHARED_KEY_STRING_TYPE);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the signWith api to update the builder with a null signingKey value
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have updated signWith (value failed due to sigALg HS256/signingKey <null>)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "signWith"
     * <LI>The failure messages from our attempt to invoke "signWith"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_signWith_sigAlg_HS256_key_null() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(HeaderConstants.ALGORITHM, JwtConstants.SIGALG_HS256);
        testSettings.put(JwtConstants.SHARED_KEY, null);
        testSettings.put(JwtConstants.SHARED_KEY_TYPE, JwtConstants.SHARED_KEY_STRING_TYPE);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6036E_INVALID_KEY, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the signWith api to update the builder with an empty( "") signingKey value
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have updated signWith (value failed due to sigALg HS256/signingKey "")
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "signWith"
     * <LI>The failure messages from our attempt to invoke "signWith"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_signWith_sigAlg_HS256_key_empty() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(HeaderConstants.ALGORITHM, JwtConstants.SIGALG_HS256);
        testSettings.put(JwtConstants.SHARED_KEY, "");
        testSettings.put(JwtConstants.SHARED_KEY_TYPE, JwtConstants.SHARED_KEY_STRING_TYPE);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6036E_INVALID_KEY, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the signWith api to update the builder with a valid signingKey value, but RS256 as sigAlg
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have updated signWith (value failed due to sigALg RS256/signingKey "signWith")
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "signWith"
     * <LI>The failure messages from our attempt to invoke "signWith"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_signWith_sigAlg_RS256_key_string() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(HeaderConstants.ALGORITHM, JwtConstants.SIGALG_RS256);
        testSettings.put(JwtConstants.SHARED_KEY, "useThisToSign");
        testSettings.put(JwtConstants.SHARED_KEY_TYPE, JwtConstants.SHARED_KEY_STRING_TYPE);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6037E_INVALID_SIG_ALG, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the signWith api to update the builder with a valid signingKey value, but <null> as sigAlg
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have updated signWith (value failed due to sigALg <null>/signingKey "signWith")
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "signWith"
     * <LI>The failure messages from our attempt to invoke "signWith"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_signWith_sigAlg_null_key_string() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(HeaderConstants.ALGORITHM, null);
        testSettings.put(JwtConstants.SHARED_KEY, "useThisToSign");
        testSettings.put(JwtConstants.SHARED_KEY_TYPE, JwtConstants.SHARED_KEY_STRING_TYPE);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6037E_INVALID_SIG_ALG, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the signWith api to update the builder with a valid signingKey value, but "" as sigAlg
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have updated signWith (value failed due to sigALg ""/signingKey "signWith")
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "signWith"
     * <LI>The failure messages from our attempt to invoke "signWith"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_signWith_sigAlg_empty_key_string() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(HeaderConstants.ALGORITHM, "");
        testSettings.put(JwtConstants.SHARED_KEY, "useThisToSign");
        testSettings.put(JwtConstants.SHARED_KEY_TYPE, JwtConstants.SHARED_KEY_STRING_TYPE);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6037E_INVALID_SIG_ALG, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the signWith api to update the builder with a valid signingKey value, but "someNonAlg" as sigAlg
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have updated signWith (value failed due to sigALg someNonAlg/signingKey "signWith")
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "signWith"
     * <LI>The failure messages from our attempt to invoke "signWith"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_signWith_sigAlg_garbage_key_string() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(HeaderConstants.ALGORITHM, "someNonAlg");
        testSettings.put(JwtConstants.SHARED_KEY, "useThisToSign");
        testSettings.put(JwtConstants.SHARED_KEY_TYPE, JwtConstants.SHARED_KEY_STRING_TYPE);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6037E_INVALID_SIG_ALG, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the signWith api to update the builder with a good value
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have updated signWith (value should be created from sigALg RS256/signingKey <privateKey>)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "signWith"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_signWith_sigAlg_RS256_key_privKey() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(HeaderConstants.ALGORITHM, JwtConstants.SIGALG_RS256);
        //        testSettings.put(JwtConstants.SHARED_KEY, Base64.getEncoder().encode(generatePrivateKey().getEncoded()));
        testSettings.put(JwtConstants.SHARED_KEY_TYPE, JwtConstants.SHARED_KEY_PRIVATE_KEY_TYPE);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    //    private Key generatePrivateKey() throws Exception {
    //
    //        int DEFAULT_KEY_SIZE = 2048;
    //
    //        KeyPair keypair = generateKeyPair(DEFAULT_KEY_SIZE);
    //
    //        RSAPrivateKey priKey = (RSAPrivateKey) keypair.getPrivate();
    //
    //        return priKey;
    //    }
    //
    //    /**
    //     * <p>
    //     * Generates a public key
    //     *
    //     * @return - a public key
    //     * @throws Exception
    //     */
    //    private Key generatePublicKey() throws Exception {
    //
    //        int DEFAULT_KEY_SIZE = 2048;
    //
    //        KeyPair keypair = generateKeyPair(DEFAULT_KEY_SIZE);
    //
    //        RSAPublicKey pubKey = (RSAPublicKey) keypair.getPublic();
    //
    //        return pubKey;
    //    }
    //
    //    private KeyPair generateKeyPair(int size) throws Exception {
    //
    //        KeyPairGenerator keyGenerator = null;
    //        try {
    //            keyGenerator = KeyPairGenerator.getInstance("RSA");
    //        } catch (NoSuchAlgorithmException e) {
    //            // This should not happen, since we hardcoded as "RSA"
    //            return null;
    //        }
    //
    //        keyGenerator.initialize(size);
    //        KeyPair keypair = keyGenerator.generateKeyPair();
    //
    //        return keypair;
    //    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the signWith api to update the builder with a bad value
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have updated signWith (value should be created from sigALg RS256/signingKey <publicKey>)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "signWith"
     * <LI>The failure messages from our attempt to invoke "signWith"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_signWith_sigAlg_RS256_key_publicKey() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(HeaderConstants.ALGORITHM, JwtConstants.SIGALG_RS256);
        //        testSettings.put(JwtConstants.SHARED_KEY, Base64.getEncoder().encode(generatePrivateKey().getEncoded()));
        testSettings.put(JwtConstants.SHARED_KEY_TYPE, JwtConstants.SHARED_KEY_PUBLIC_KEY_TYPE);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6036E_INVALID_KEY, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the signWith api to update the builder with <null> signingKey value
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have updated signWith (value failed due to sigALg RS256/signingKey <null>)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "signWith"
     * <LI>The failure messages from our attempt to invoke "signWith"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_signWith_sigAlg_RS256_key_null() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(HeaderConstants.ALGORITHM, JwtConstants.SIGALG_RS256);
        testSettings.put(JwtConstants.SHARED_KEY, null);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6036E_INVALID_KEY, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the signWith api to update the builder with a valid signingKey value, but <null> as sigAlg
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have updated signWith (value failed due to sigALg <null>/signingKey privateKey)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "signWith"
     * <LI>The failure messages from our attempt to invoke "signWith"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_signWith_sigAlg_null_key_privKey() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(HeaderConstants.ALGORITHM, null);
        testSettings.put(JwtConstants.SHARED_KEY_TYPE, JwtConstants.SHARED_KEY_PRIVATE_KEY_TYPE);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6037E_INVALID_SIG_ALG, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the signWith api to update the builder with a valid signingKey value, but "" as sigAlg
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have updated signWith (value failed due to sigALg ""/signingKey privateKey)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "signWith"
     * <LI>The failure messages from our attempt to invoke "signWith"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_signWith_sigAlg_empty_key_privKey() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(HeaderConstants.ALGORITHM, "");
        testSettings.put(JwtConstants.SHARED_KEY_TYPE, JwtConstants.SHARED_KEY_PRIVATE_KEY_TYPE);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6037E_INVALID_SIG_ALG, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run the signWith api to update the builder with a valid signingKey value, but "someNonAlg" as sigAlg
     * <LI>generate a JWT token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should NOT have updated signWith (value failed due to sigALg "someNonAlg"/signingKey privateKey)
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking "signWith"
     * <LI>The failure messages from our attempt to invoke "signWith"
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * </UL>
     * </OL>
     */
    //chc@Test
    public void JwtBuilderAPIBasicTests_signWith_sigAlg_garbage_key_privKey() throws Exception {

        String builderId = "jwt1";
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        JSONObject testSettings = new JSONObject();
        testSettings.put(HeaderConstants.ALGORITHM, "someNonAlg");
        testSettings.put(JwtConstants.SHARED_KEY_TYPE, JwtConstants.SHARED_KEY_PRIVATE_KEY_TYPE);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6037E_INVALID_SIG_ALG, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /*************************** Test multiple settings and consume Token ***************************/
    /**
     * <p>
     * Test Purpose:
     * <OL>
     * <LI>Create a builder using the specified configId (a generic config used for most tests)
     * <LI>Run a variety of the api's to update the builder
     * <LI>generate a JWT token
     * <LI>Invoke a protected app using the generated JWT Token
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The builder should be created with default values as there is not much defined in the specified config
     * <LI>The builder should have been updated
     * <LI>The JWT Token should be created based on the builder
     * <LI>The JWT Token will be used to display the claim values
     * <LI>The JWT Token will be used to generate a JWT JSON String and this will be returned
     * <LI>The test case will validate the content of:
     * <UL>
     * <LI>The messages logged indicating that we were invoking various set apis
     * <LI>The content of the returned token
     * <LI>The output from running the query apis
     * <LI>The output from invoking the protected app
     * </UL>
     * </OL>
     */
    @Mode(TestMode.LITE)
    //chc@Test
    public void JwtBuilderAPIBasicTests_multiple_apis_and_consumeToken() throws Exception {

        String builderId = null;
        JSONObject configSettings = BuilderHelpers.setDefaultClaims(builderServer);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, "testuser");
        testSettings.put(ClaimConstants.EXPIRATION_TIME, testExp);
        testSettings.put(ClaimConstants.NOT_BEFORE, 1477691420L);
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put(ClaimConstants.AUTHORIZED_PARTY, "someParty");
        claimsToSet.put("someClaim", "someValue");
        claimsToSet.put("anotherClaim", "anotherValue");
        claimsToSet.put("stillOneMoreClaim", "stillOneMoreValue");
        testSettings.put(JwtConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        configSettings.put("overrideSettings", testSettings);

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, configSettings, builderServer);

        // Indicate how claims are to be handled (one at a time, or added as a hashmap)
        List<NameValuePair> extraParms = new ArrayList<NameValuePair>();
        extraParms.add(new NameValuePair(JwtConstants.ADD_CLAIMS_AS, processClaimsAs));
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParms, testSettings);
        validationUtils.validateResult(response, expectations);

        // TODO consume token
    }
}
