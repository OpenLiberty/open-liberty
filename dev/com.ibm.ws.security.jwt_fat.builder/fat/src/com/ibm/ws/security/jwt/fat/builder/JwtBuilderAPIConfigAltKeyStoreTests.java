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

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.jwt.HeaderConstants;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.JwtMessageConstants;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.buider.actions.JwtBuilderActions;
import com.ibm.ws.security.jwt.fat.builder.utils.BuilderHelpers;

import componenttest.annotation.ExpectedFFDC;
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

@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JwtBuilderAPIConfigAltKeyStoreTests extends CommonSecurityFat {

    @Server("com.ibm.ws.security.jwt_fat.builder")
    public static LibertyServer builderServer;

    private static final JwtBuilderActions actions = new JwtBuilderActions();
    public static final TestValidationUtils validationUtils = new TestValidationUtils();

    @BeforeClass
    public static void setUp() throws Exception {

        serverTracker.addServer(builderServer);
        builderServer.startServerUsingExpandedConfiguration("server_configTests2.xml");
        SecurityFatHttpUtils.saveServerPorts(builderServer, JWTBuilderConstants.BVT_SERVER_1_PORT_NAME_ROOT);

        // the server's default config contains an invalid value (on purpose), tell the fat framework to ignore it!
        builderServer.addIgnoredErrors(Arrays.asList(JwtMessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE));

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that does NOT define a builder specific "keyStoreRef" or keyAlias. The global
     * keystore does NOT have an RSA cert
     * <LI>What this means is that we can not create the token because we can't find a good cert
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will fail to create a token
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigAltKeyStoreTests_sigAlg_RS256_badGlobalKeyStore() throws Exception {

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, null);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that has a global keystore that does not have an RSA cert and defines a builder
     * specific "keyStoreRef", it does NOT specify a keyAlias
     * <LI>What this means is that we can create the token using the one cert in the config specific keystore
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built using the one cert from the builder specific keystore
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigAltKeyStoreTests_sigAlg_RS256_badGlobalKeyStore_goodKeyStoreRef_noKeyAlias() throws Exception {

        String builderId = "key_sigAlg_RS256";
        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);
        settings.put(HeaderConstants.ALGORITHM, JwtConstants.SIGALG_RS256);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that has a global keystore that does not have an RSA cert and defines a builder
     * specific "keyStoreRef", it does specify a keyAlias
     * <LI>What this means is that we can create the token using the specified cert in the config specific keystore
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>Should get a token built using the specified cert from the builder specific keystore
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void JwtBuilderAPIConfigAltKeyStoreTests_sigAlg_RS256_badGlobalKeyStore_goodKeyStoreRef_goodKeyAlias() throws Exception {

        String builderId = "key_sigAlg_RS256_goodKeyAlias";
        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);
        settings.put(HeaderConstants.ALGORITHM, JwtConstants.SIGALG_RS256);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that has a global keystore that does not have an RSA cert and defines a builder
     * specific "keyStoreRef", it specifies a non-existant keyAlias
     * <LI>What this means is that we can not create the token because we can't find the specified cert
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will fail to create a token
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigAltKeyStoreTests_sigAlg_RS256_badGlobalKeyStore_goodKeyStoreRef_badKeyAlias() throws Exception {

        String builderId = "key_sigAlg_RS256_badKeyAlias";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that has a global keystore that does not have an RSA cert and does NOT define a
     * builder specific "keyStoreRef", it specifies a keyAlias
     * <LI>What this means is that we can not create the token because we can't find the specified cert
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will fail to create a token
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigAltKeyStoreTests_sigAlg_RS256_badGlobalKeyStore_noKeyStoreRef_goodKeyAlias() throws Exception {

        String builderId = "key_sigAlg_RS256_goodKeyAlias_global";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that has a global keystore that does not have an RSA cert and does NOT define a
     * builder specific "keyStoreRef", it specifies a keyAlias
     * <LI>What this means is that we can not create the token because we can't find the specified cert
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will fail to create a token
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigAltKeyStoreTests_sigAlg_RS256_badGlobalKeyStore_noKeyStoreRef_badKeyAlias() throws Exception {

        String builderId = "key_sigAlg_RS256_badKeyAlias_global";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke the JWT Builder using a config that has a global keystore that does not have an RSA cert and does define a
     * builder specific "keyStoreRef" that does NOT exist
     * <LI>What this means is that we can not create the token because we can't find the specified keystore
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>We will fail to create a token
     * </OL>
     */
    @ExpectedFFDC("com.ibm.ws.security.jwt.internal.JwtTokenException")
    @Test
    public void JwtBuilderAPIConfigAltKeyStoreTests_sigAlg_RS256_badGlobalKeyStore_badKeyStoreRef() throws Exception {

        String builderId = "key_sigAlg_RS256_badKeyStoreRef";

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, builderServer);
        expectations.addExpectation(new ServerMessageExpectation(builderServer, JwtMessageConstants.CWWKS6016E_BAD_KEY_ALIAS, "Message log did not contain an error indicating a problem with the signing key."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId);
        validationUtils.validateResult(response, expectations);

    }

}