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

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.jwt.HeaderConstants;
import com.ibm.ws.security.fat.common.jwt.JwtMessageConstants;
import com.ibm.ws.security.fat.common.jwt.PayloadConstants;
import com.ibm.ws.security.fat.common.jwt.expectations.JwtApiExpectation;
import com.ibm.ws.security.fat.common.utils.CommonExpectations;
import com.ibm.ws.security.fat.common.utils.CommonWaitForAppChecks;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.builder.actions.JwtBuilderActions;
import com.ibm.ws.security.jwt.fat.builder.utils.BuilderHelpers;

import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.JavaInfo;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will run basic JWT Builder Config tests.
 *
 **/

@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JwtBuilderAPIConfigTests extends CommonSecurityFat {

    @Server("com.ibm.ws.security.jwt_fat.builder")
    public static LibertyServer builderServer;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    private static final JwtBuilderActions actions = new JwtBuilderActions();
    public static final TestValidationUtils validationUtils = new TestValidationUtils();

    @BeforeClass
    public static void setUp() throws Exception {

        serverTracker.addServer(builderServer);
        builderServer.addInstalledAppForValidation(JWTBuilderConstants.JWT_BUILDER_SERVLET);
        builderServer.startServerUsingExpandedConfiguration("server_configTests.xml", CommonWaitForAppChecks.getSecurityReadyMsgs());
        SecurityFatHttpUtils.saveServerPorts(builderServer, JWTBuilderConstants.BVT_SERVER_1_PORT_NAME_ROOT);

        // the server's default config contains an invalid value (on purpose),
        // tell the fat framework to ignore it!
        builderServer.addIgnoredErrors(Arrays.asList(JwtMessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE));

    }

    /**
     * <p>
     * Test Purpose:
     * </p>
     * <OL>
     * <LI>Invoke the JWT Builder using the default builder config.
     * <LI>What this means is that we're not specifying any JWT Builder id,
     * therefore, we'll use attributes from base config as well as embedded
     * defaults
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built using the default values for the JWT Token
     * </OL>
     */
    @Test
    public void JwtBuilderAPIConfigTests_defaultConfig() throws Exception {

        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderServer);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, null);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <P>
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that has "" as the id
     * <LI>Create a JWT build specifying "" as the config id and show that even
     * with a config with "" as the id, the builder throws and exception *
     * <LI>The builder does not pick up the config that has the id ""
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get another token built using the default values for the JWT
     * Token
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_noId() throws Exception {

        builderServer.reconfigureServerUsingExpandedConfiguration(_testName, "server_noId.xml");

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(SecurityFatHttpUtils.getServerUrlBase(builderServer) + JWTBuilderConstants.JWT_BUILDER_CREATE_ENDPOINT));
        expectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_MATCHES, JwtMessageConstants.CWWKS6008E_BUILD_ID_UNKNOWN + ".+\\[\\]", "Response did not show the expected failure."));

        Page response = actions.invokeJwtBuilder_create(_testName, builderServer, JWTBuilderConstants.EMPTY_STRING);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using an empty config (the config just has an
     * id, nothing more).
     * <LI>What this means is that we're not specifying any JWT Builder config,
     * therefore, we'll use attributes from base config as well as embedded
     * defaults
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built using the default values for the JWT Token
     * </OL>
     */
    @Test
    public void JwtBuilderAPIConfigTests_emptyConfig() throws Exception {

        String builderId = "emptyConfig";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <UL>
     * <LI>Invoke the JWT Builder using a config that has a specific "expiry"
     * defined..
     * <LI>What this means is that the token we create will use the "expiry" to
     * calculate the expiration time of the token instead of using 2 hours
     * </UL>
     * <P>
     * Expected Results:
     * <UL>
     * <LI>Should get a valid JWT Token with a lifetime of 1 hour instead of 2
     * hours
     * </UL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_specificExpiry() throws Exception {

        String builderId = "specificExpiry";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // override the default expiration time
        expectationSettings.put(PayloadConstants.EXPIRATION_TIME, BuilderHelpers.setNowLong() + (1 * 60 * 60));
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <UL>
     * <LI>Invoke the JWT Builder using a config that has a specific
     * "expiresInSeconds" defined..
     * <LI>What this means is that the token we create will use the
     * "expiresInSeconds" to calculate the expiration time.
     * <LI>This will override the 1h expiry defined in the config.
     *
     * </UL>
     * <P>
     * Expected Results:
     * <UL>
     * <LI>Should get a valid JWT Token with a lifetime of 1800 seconds (one
     * half hour) instead of one or two hours.
     * </UL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_specificExpiresInSeconds() throws Exception {

        String builderId = "specificExpirySeconds";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // override the default expiration time
        expectationSettings.put(PayloadConstants.EXPIRATION_TIME, BuilderHelpers.setNowLong() + (1800));
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <LI>Invoke the JWT Builder using a config that does define elapsedNBF.
     * <LI>What this means is that the token we create will use the "elapsedNBF" to calculate the time passed since token issued
     * and set that as the NBF claim.
     *
     * </UL>
     * <P>
     * Expected Results:
     * <UL>
     * <LI>Should get a valid JWT Token with a NBF set to 1800 seconds.
     * </UL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_specificElapsedNotBefore() throws Exception {

        String builderId = "specificElapsedNBF";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // override the default expiration time
        expectationSettings.put(PayloadConstants.NOT_BEFORE, BuilderHelpers.setNowLong() + (1800));
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <UL>
     * <LI>Invoke the JWT Builder using a config that has a specific "audience"
     * defined..
     * <LI>What this means is that the token we create will include "aud" set to
     * the value specified
     * </UL>
     * <P>
     * Expected Results:
     * <UL>
     * <LI>Should get a valid JWT Token with a "aud" set to a list containing
     * "Client02" and "Client03"
     * </UL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_specificAudiences() throws Exception {

        String builderId = "specificAudiences";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set an audience value
        JSONArray parmarray = new JSONArray();
        parmarray.add("Client02");
        parmarray.add("Client03");
        expectationSettings.put(PayloadConstants.AUDIENCE, parmarray);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <UL>
     * <LI>Invoke the JWT Builder using a config that has a specific "scopes"
     * defined..
     * <LI>What this means is that the token we create will include "scope" set
     * to the value specified
     * </UL>
     * <P>
     * Expected Results:
     * <UL>
     * <LI>Should get a valid JWT Token with a "scope" set to a string
     * containing "myScope yourScope"
     * </UL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_specificScopes() throws Exception {

        String builderId = "specificScopes";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set scope value
        JSONArray parmarray = new JSONArray();
        parmarray.add("myScope");
        parmarray.add("yourScope");
        expectationSettings.put(PayloadConstants.SCOPE, parmarray);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that has the "jti" attribute
     * set to "true".
     * <LI>What this means is that the token we create will include "jti" set to
     * some generated value
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built containing a jti value
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_specificJti() throws Exception {

        String builderId = "specificJti";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        expectationSettings.put(PayloadConstants.JWT_ID, true);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);
        expectations.addExpectation(new JwtApiExpectation(JWTBuilderConstants.STRING_DOES_NOT_MATCH, JWTBuilderConstants.JWT_CLAIM + PayloadConstants.JWT_ID + ".*null.*", "jti was found and should NOT have been"));
        expectations.addExpectation(new JwtApiExpectation(JWTBuilderConstants.STRING_MATCHES, JWTBuilderConstants.JWT_CLAIM + JWTBuilderConstants.JWT_JSON + "\\{" + ".*\"" + PayloadConstants.JWT_ID + "\".*\\}", "jti was NOT found in the list of claims"));
        expectations.addExpectation(new JwtApiExpectation(JWTBuilderConstants.STRING_MATCHES, JWTBuilderConstants.JWT_CLAIM + JWTBuilderConstants.JWT_JSON + JWTBuilderConstants.JWT_GETALLCLAIMS + JWTBuilderConstants.JWT_CLAIM_KEY + PayloadConstants.JWT_ID + ".*", "The jti claim was NOT found and should have been"));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that has the "issuer" set to a
     * specific value.
     * <LI>What this means is that the token we create will include "iss" set to
     * the specified value
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built containing iss set to the specified value
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_specificIssuer() throws Exception {

        String builderId = "specificIssuer";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set issuer value
        expectationSettings.put(PayloadConstants.ISSUER, "someSpecificIssuer");
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }
    
    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that has amrInclude set to "amrTest" which has a value "amrTestValue"
     * <LI>in the security subject. The builderAPI sets that value into the subject.
     * <LI>What this means is that the token we create will include "amr" set to ["amrValue"]
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built valid amr claim set to ["amrValue"]
     * </OL>
     * 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_amrValue_valid() throws Exception {

        String builderId = "AMRTestValid";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        expectationSettings.put(PayloadConstants.METHODS_REFERENCE, "amrValue");

        // have to set amr
        JSONObject testSettings = new JSONObject();
        testSettings.put(PayloadConstants.METHODS_REFERENCE, "amrTest");
        expectationSettings.put("overrideSettings", testSettings);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_PROTECTED_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeProtectedJwtBuilder(_testName, builderServer, builderId, testSettings, "testuser", "testuserpwd");
        validationUtils.validateResult(response, expectations);

    }
	
    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that has amrInclude set to "random" which has no value
     * <LI>in the security subject.
     * <LI>What this means is that the token we create should not include "amr"
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built without amr claim set
     * </OL>
     * 
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_amrValue_invalid() throws Exception {

        String builderId = "AMRTestInvalid";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        JSONObject testSettings = new JSONObject();
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_PROTECTED_SETAPIS_ENDPOINT, expectationSettings, builderServer);
        expectations = BuilderHelpers.buildBuilderClaimsNotFound(expectations, JWTBuilderConstants.JWT_CLAIM, PayloadConstants.METHODS_REFERENCE);
        
        Page response = actions.invokeProtectedJwtBuilder(_testName, builderServer, builderId, testSettings, "testuser", "testuserpwd");
        validationUtils.validateResult(response, expectations);

    }
	
    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that has amrInclude set to empty string
     * <LI>What this means is that the token we create should not include "amr"
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built without amr claim set
     * </OL>
     * 
     * @throws Exception
     */
	@Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_amrValue_empty() throws Exception {

        String builderId = "AMRTestEmpty";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        JSONObject testSettings = new JSONObject();
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_PROTECTED_SETAPIS_ENDPOINT, expectationSettings, builderServer);
        expectations = BuilderHelpers.buildBuilderClaimsNotFound(expectations, JWTBuilderConstants.JWT_CLAIM, PayloadConstants.METHODS_REFERENCE);
        
        Page response = actions.invokeProtectedJwtBuilder(_testName, builderServer, builderId, testSettings, "testuser", "testuserpwd");
        validationUtils.validateResult(response, expectations);

    }

    // **************************************************************************************************************************
    // Claim tests are run using an LDAP registry, refer to tests in the
    // JwtBuilderAPIWithLDAPConfigTests for coverage
    // **************************************************************************************************************************

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that has does not define a
     * builder specific "keyStoreRef" - we only have the global keystore
     * <LI>What this means is that the token we create will use the cert from
     * the global keystore
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built using the one cert from the global keystore
     * </OL>
     */
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_RS256_goodGlobalKeyStore() throws Exception {

        String builderId = "key_sigAlg_RS256_noKeyRef";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_RS256);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that defines a builder specific
     * "keyStoreRef", but does NOT specifiy the alias
     * <LI>What this means is that the token we create will use the cert from
     * the builder specific keystore
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built using the one cert from the builder specific
     * keystore
     * </OL>
     */
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_RS256_goodGlobalKeyStore_goodKeyStoreRef_noKeyAlias() throws Exception {

        String builderId = "key_sigAlg_RS256";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_RS256);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that defines a builder specific
     * "keyStoreRef", as well as the keyAlias
     * <LI>What this means is that the token we create will use the specified
     * cert from the builder specific keystore
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built using the specified cert from the builder
     * specific keystore
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_RS256_goodGlobalKeyStore_goodKeyStoreRef_goodKeyAlias() throws Exception {

        String builderId = "key_sigAlg_RS256_goodKeyAlias";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_RS256);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that defines a builder specific
     * "keyStoreRef", as well as a non-existant keyAlias
     * <LI>What this means is that we can not create the token because we can't
     * find the specified cert
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will fail to create a token
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_RS256_goodGlobalKeyStore_goodKeyStoreRef_badKeyAlias() throws Exception {

        String builderId = "key_sigAlg_RS256_badKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that does NOT define a builder
     * specific "keyStoreRef", but, does define the keyAlias
     * <LI>What this means is that the token we create will use the specified
     * cert from the global specific keystore
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built using the specified cert from the global
     * keystore
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_RS256_goodGlobalKeyStore_noKeyStoreRef_goodKeyAlias() throws Exception {

        String builderId = "key_sigAlg_RS256_goodKeyAlias_global";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_RS256);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /******************************************
     * start Additional Signature Algorithms
     ****************************************/
    /**********************************************************************************************************************
     * 1) Test with each signature algorithm that we support.
     * 2) Test setting the signature algorithm to something we support, but use a key that is of another type (1 each
     * RS, ES, PS)
     * 3) Test keystore has 2 keys of each algorithm type - there is no concept of use the wrong key (aside from testing
     * done for #2) since we're not using the token, just validating the content of the token.
     */
    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be RS256. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of a different RS alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>This will actually build the token - we have no way of differentiating and RS256 from an RS384 or RS512 key...
     * </OL>
     */
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_RS256_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchRS() throws Exception {

        String builderId = "key_sigAlg_RS256_badRSKeyAlias";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_RS256);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be RS256. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of an ES alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_RS256_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchES() throws Exception {

        String builderId = "key_sigAlg_RS256_badESKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be RS256. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of an PS alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_RS256_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchPS() throws Exception {

        String builderId = "key_sigAlg_RS256_badPSKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be RS384. The defines a builder
     * specific "keyStoreRef", as well as the keyAlias
     * <LI>What this means is that the token we create will use the specified RS384 cert from the builder specific keystore
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built using the specified RS384 cert from the builder specific keystore
     * </OL>
     */
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_RS384_goodGlobalKeyStore_goodKeyStoreRef_goodKeyAlias() throws Exception {

        String builderId = "key_sigAlg_RS384_goodKeyAlias";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_RS384);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be RS384. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of a different RS alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>This will actually build the token - we have no way of differentiating and RS256 from an RS384 or RS512 key...
     * </OL>
     */
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_RS384_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchRS() throws Exception {

        String builderId = "key_sigAlg_RS384_badRSKeyAlias";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_RS384);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be RS384. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of an ES alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_RS384_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchES() throws Exception {

        String builderId = "key_sigAlg_RS384_badESKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be RS384. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of an PS alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_RS384_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchPS() throws Exception {

        String builderId = "key_sigAlg_RS384_badPSKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be RS512. The defines a builder
     * specific "keyStoreRef", as well as the keyAlias
     * <LI>What this means is that the token we create will use the specified RS512 cert from the builder specific keystore
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built using the specified RS512 cert from the builder specific keystore
     * </OL>
     */
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_RS512_goodGlobalKeyStore_goodKeyStoreRef_goodKeyAlias() throws Exception {

        String builderId = "key_sigAlg_RS512_goodKeyAlias";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_RS512);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be RS512. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of a different RS alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>This will actually build the token - we have no way of differentiating and RS256 from an RS384 or RS512 key...
     * </OL>
     */
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_RS512_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchRS() throws Exception {

        String builderId = "key_sigAlg_RS512_badRSKeyAlias";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_RS512);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be RS512. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of an ES alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_RS512_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchES() throws Exception {

        String builderId = "key_sigAlg_RS512_badESKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be RS512. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of an PS alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_RS512_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchPS() throws Exception {

        String builderId = "key_sigAlg_RS512_badPSKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be ES256. The defines a builder
     * specific "keyStoreRef", as well as the keyAlias
     * <LI>What this means is that the token we create will use the specified ES256 cert from the builder specific keystore
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built using the specified ES256 cert from the builder specific keystore
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_ES256_goodGlobalKeyStore_goodKeyStoreRef_goodKeyAlias() throws Exception {

        String builderId = "key_sigAlg_ES256_goodKeyAlias";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_ES256);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be ES256. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of an RS alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_ES256_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchRS() throws Exception {

        String builderId = "key_sigAlg_ES256_badRSKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be ES256. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of a different ES alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_ES256_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchES() throws Exception {

        String builderId = "key_sigAlg_ES256_badESKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be ES256. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of an PS alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_ES256_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchPS() throws Exception {

        String builderId = "key_sigAlg_ES256_badPSKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be ES384. The defines a builder
     * specific "keyStoreRef", as well as the keyAlias
     * <LI>What this means is that the token we create will use the specified ES384 cert from the builder specific keystore
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built using the specified ES384 cert from the builder specific keystore
     * </OL>
     */
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_ES384_goodGlobalKeyStore_goodKeyStoreRef_goodKeyAlias() throws Exception {

        String builderId = "key_sigAlg_ES384_goodKeyAlias";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_ES384);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be ES384. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of an RS alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_ES384_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchRS() throws Exception {

        String builderId = "key_sigAlg_ES384_badRSKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be ES384. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of a different ES alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_ES384_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchES() throws Exception {

        String builderId = "key_sigAlg_ES384_badESKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be ES384. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of an PS alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_ES384_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchPS() throws Exception {

        String builderId = "key_sigAlg_ES384_badPSKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be ES512. The defines a builder
     * specific "keyStoreRef", as well as the keyAlias
     * <LI>What this means is that the token we create will use the specified ES512 cert from the builder specific keystore
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built using the specified ES512 cert from the builder specific keystore
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_ES512_goodGlobalKeyStore_goodKeyStoreRef_goodKeyAlias() throws Exception {

        String builderId = "key_sigAlg_ES512_goodKeyAlias";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_ES512);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be ES512. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of an RS alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_ES512_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchRS() throws Exception {

        String builderId = "key_sigAlg_ES512_badRSKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be ES512. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of a different ES alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_ES512_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchES() throws Exception {

        String builderId = "key_sigAlg_ES512_badESKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be ES512. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of an PS alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_ES512_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchPS() throws Exception {

        String builderId = "key_sigAlg_ES512_badPSKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be PS256. The defines a builder
     * specific "keyStoreRef", as well as the keyAlias
     * <LI>What this means is that the token we create will use the specified PS256 cert from the builder specific keystore
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built using the specified PS256 cert from the builder specific keystore
     * </OL>
     */
    // TODO enable/update when PS algs are supported @Test
    public void JwtBuilderAPIConfigTests_sigAlg_PS256_goodGlobalKeyStore_goodKeyStoreRef_goodKeyAlias() throws Exception {

        String builderId = "key_sigAlg_PS256_goodKeyAlias";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_PS256);
        Expectations expectations = null;
        if (JavaInfo.JAVA_VERSION >= 11) {
            expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);
        } else {
            // TODO fix checks for failures once PS algs are supported (the failure will be different from it not being supported)
            expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
            expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));
        }

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be PS256. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of an RS alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    // TODO enable/update when PS algs are supported@Test
    public void JwtBuilderAPIConfigTests_sigAlg_PS256_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchRS() throws Exception {

        String builderId = "key_sigAlg_PS256_badRSKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be PS256. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of an ES alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    // TODO enable/update when PS algs are supported@Test
    public void JwtBuilderAPIConfigTests_sigAlg_PS256_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchES() throws Exception {

        String builderId = "key_sigAlg_PS256_badESKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be PS256. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of a different PS alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    // TODO enable/update when PS algs are supported@Test
    public void JwtBuilderAPIConfigTests_sigAlg_PS256_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchPS() throws Exception {

        String builderId = "key_sigAlg_PS256_badPSKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be PS384. The defines a builder
     * specific "keyStoreRef", as well as the keyAlias
     * <LI>What this means is that the token we create will use the specified PS384 cert from the builder specific keystore
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built using the specified PS384 cert from the builder specific keystore
     * </OL>
     */
    // TODO enable/update when PS algs are supported @Test
    public void JwtBuilderAPIConfigTests_sigAlg_PS384_goodGlobalKeyStore_goodKeyStoreRef_goodKeyAlias() throws Exception {

        String builderId = "key_sigAlg_PS384_goodKeyAlias";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_PS384);
        Expectations expectations = null;
        if (JavaInfo.JAVA_VERSION >= 11) {
            expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);
        } else {
            // TODO fix checks for failures once PS algs are supported (the failure will be different from it not being supported)
            expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
            expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));
        }

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be PS384. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of an RS alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    // TODO enable/update when PS algs are supported @Test
    public void JwtBuilderAPIConfigTests_sigAlg_PS384_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchRS() throws Exception {

        String builderId = "key_sigAlg_PS384_badRSKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be PS384. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of an ES alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    // TODO enable/update when PS algs are supported @Test
    public void JwtBuilderAPIConfigTests_sigAlg_PS384_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchES() throws Exception {

        String builderId = "key_sigAlg_PS384_badESKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be PS384. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of a different PS alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    // TODO enable/update when PS algs are supported @Test
    public void JwtBuilderAPIConfigTests_sigAlg_PS384_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchPS() throws Exception {

        String builderId = "key_sigAlg_PS384_badPSKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be PS512. The defines a builder
     * specific "keyStoreRef", as well as the keyAlias
     * <LI>What this means is that the token we create will use the specified PS512 cert from the builder specific keystore
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built using the specified PS512 cert from the builder specific keystore
     * </OL>
     */
    // TODO enable/update when PS algs are supported @Test
    public void JwtBuilderAPIConfigTests_sigAlg_PS512_goodGlobalKeyStore_goodKeyStoreRef_goodKeyAlias() throws Exception {

        String builderId = "key_sigAlg_PS512_goodKeyAlias";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_PS512);
        Expectations expectations = null;
        if (JavaInfo.JAVA_VERSION >= 11) {
            expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);
        } else {
            // TODO fix checks for failures once PS algs are supported (the failure will be different from it not being supported)
            expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
            expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));
        }

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be PS512. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of an RS alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    // TODO enable/update when PS algs are supported @Test
    public void JwtBuilderAPIConfigTests_sigAlg_PS512_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchRS() throws Exception {

        String builderId = "key_sigAlg_PS512_badRSKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be PS512. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of an ES alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    // TODO enable/update when PS algs are supported @Test
    public void JwtBuilderAPIConfigTests_sigAlg_PS512_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchES() throws Exception {

        String builderId = "key_sigAlg_PS512_badESKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies the signature algorithm to be PS512. Then defines a builder
     * specific "keyStoreRef", as well as the keyAlias. The keyAlias references a key of a different PS alg.
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should fail to build the requested token as the algorithm and key type do not match
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    // TODO enable/update when PS algs are supported @Test
    public void JwtBuilderAPIConfigTests_sigAlg_PS512_goodGlobalKeyStore_goodKeyStoreRef_keyAliasMisMatchPS() throws Exception {

        String builderId = "key_sigAlg_PS512_badPSKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies that the signatureAlgorithm be HS384 - it also specifies a
     * sharedKey
     * <LI>What this means is that we can create a jwt token using the specified shared key
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will create a jwt token signing with a shared key
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_HS384_with_sharedKey() throws Exception {

        String builderId = "key_sigAlg_HS384_with_sharedKey";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_HS384);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies that the signatureAlgorithm be HS512 - it also specifies a
     * sharedKey
     * <LI>What this means is that we can create a jwt token using the specified shared key
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will create a jwt token signing with a shared key
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_HS512_with_sharedKey() throws Exception {

        String builderId = "key_sigAlg_HS512_with_sharedKey";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_HS512);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /******************************************
     * end Additional Signature Algorithms
     ****************************************/

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that does NOT define a builder
     * specific "keyStoreRef", but does specify a non-existant keyAlias
     * <LI>What this means is that we can not create the token because we can't
     * find the specified cert
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will fail to create a token
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_RS256_goodGlobalKeyStore_noKeyStoreRef_badKeyAlias() throws Exception {

        String builderId = "key_sigAlg_RS256_badKeyAlias_global";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that defines a bad builder
     * specific "keyStoreRef"
     * <LI>What this means is that we can not create the token because we can't
     * find the keystore
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will fail to create a token
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_RS256_goodGlobalKeyStore_badKeyStoreRef() throws Exception {

        String builderId = "key_sigAlg_RS256_badKeyStoreRef";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies that the
     * signatureAlgorithm be HS256 - it also specifies a sharedKey
     * <LI>What this means is that we can create a jwt token using the specified
     * shared key
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will create a jwt token signing with a shared key
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_HS256_with_sharedKey() throws Exception {

        String builderId = "key_sigAlg_HS256_with_sharedKey";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_HS256);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies that the
     * signatureAlgorithm be HS256 - it also specifies a sharedKey
     * <LI>What this means is that we can create a jwt token using the specified
     * shared key
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will create a jwt token signing with a shared key
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_HS256_with_xor_sharedKey() throws Exception {

        String builderId = "key_sigAlg_HS256_with_xor_sharedKey";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_HS256);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that specifies that the
     * signatureAlgorithm be HS256 - it omits the sharedKey
     * <LI>What this means is that we can NOT create a jwt token because we do
     * NOT have a shared key
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will NOT create a jwt token
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigTests_sigAlg_HS256_without_sharedKey() throws Exception {

        String builderId = "key_sigAlg_HS256_without_sharedKey";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that jwkEnabled is true
     * <LI>What this means is that we can create a jwt token with a valid "kid"
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will create a jwt token with a "kid" value
     * </OL>
     */
    @Test
    public void JwtBuilderAPIConfigTests_jwkEnabled_defaultValues() throws Exception {

        String builderId = "jwkEnabled";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // add a check just to make sure that the kid is in the header (can't do
        // anything about checking the value)
        expectationSettings.put(HeaderConstants.KEY_ID, "");

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that jwkEnabled is true and
     * signatureAlgorithm set to RS256
     * <LI>What this means is that we can create a jwt token with a valid "kid"
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will create a jwt token with a "kid" value
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_jwkEnabled_sigAlg_RS256() throws Exception {

        String builderId = "jwkEnabled_RS256";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // add a check just to make sure that the kid is in the header (can't do
        // anything about checking the value)
        expectationSettings.put(HeaderConstants.KEY_ID, "");
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_RS256);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that jwkEnabled is true and
     * signatureAlgorithm set to HS256
     * <LI>What this means is that we can NOT create a jwt token with a valid
     * "kid", we create a token with an X509 signature
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will create a jwt token without a "kid" and will use X509
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigTests_jwkEnabled_sigAlg_HS256() throws Exception {

        String builderId = "jwkEnabled_HS256";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // The signature alg of HS256 will override jwkEnabled
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_HS256);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    @Test
    public void JwtBuilderAPIConfigTests_jwkEnabled_sigAlg_HS256_xor() throws Exception {

        String builderId = "jwkEnabled_HS256_xor";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // The signature alg of HS256 will override jwkEnabled
        // set alg value
        expectationSettings.put(HeaderConstants.ALGORITHM, JWTBuilderConstants.SIGALG_HS256);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that jwkSigningKeySize set to
     * 1024
     * <LI>What this means is that we can create a jwt token with a signature of
     * length 1024
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will create a jwt token with a signature length of 1024
     * </OL>
     */
    @Test
    public void JwtBuilderAPIConfigTests_jwkEnabled_jwkSigningKeySize_1024() throws Exception {

        String builderId = "jwkEnabled_size_1024";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // add a check just to make sure that the kid is in the header (can't do
        // anything about checking the value)
        expectationSettings.put(HeaderConstants.KEY_ID, "");

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that jwkSigningKeySize set to
     * 2048
     * <LI>What this means is that we can create a jwt token with a signature of
     * length 2048
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will create a jwt token with a signature length of 2048
     * </OL>
     */
    @Test
    public void JwtBuilderAPIConfigTests_jwkEnabled_jwkSigningKeySize_2048() throws Exception {

        String builderId = "jwkEnabled_size_2048";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // add a check just to make sure that the kid is in the header (can't do
        // anything about checking the value)
        expectationSettings.put(HeaderConstants.KEY_ID, "");

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that jwkSigningKeySize set to
     * 4096
     * <LI>What this means is that we can create a jwt token with a signature of
     * length 4096
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will create a jwt token with a signature length of 4096
     * </OL>
     */
    @Test
    public void JwtBuilderAPIConfigTests_jwkEnabled_jwkSigningKeySize_4096() throws Exception {

        String builderId = "jwkEnabled_size_4096";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        // add a check just to make sure that the kid is in the header (can't do
        // anything about checking the value)
        expectationSettings.put(HeaderConstants.KEY_ID, "");

        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }
}
