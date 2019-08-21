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
import com.ibm.ws.security.fat.common.jwt.ClaimConstants;
import com.ibm.ws.security.fat.common.jwt.JwtConstants;
import com.ibm.ws.security.fat.common.jwt.JwtMessageConstants;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.buider.actions.JwtBuilderActions;
import com.ibm.ws.security.jwt.fat.builder.utils.BuilderHelpers;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;
import componenttest.topology.utils.LDAPUtils;

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
public class JwtBuilderAPIWithLDAPConfigTests extends CommonSecurityFat {

    @Server("com.ibm.ws.security.jwt_fat.builder")
    public static LibertyServer builderServer;

    private static final JwtBuilderActions actions = new JwtBuilderActions();
    public static final TestValidationUtils validationUtils = new TestValidationUtils();

    @BeforeClass
    public static void setUp() throws Exception {

        LDAPUtils.addLDAPVariables(builderServer);
        serverTracker.addServer(builderServer);
        builderServer.startServerUsingExpandedConfiguration("server_LDAPRegistry_configTests.xml");
        SecurityFatHttpUtils.saveServerPorts(builderServer, JWTBuilderConstants.BVT_SERVER_1_PORT_NAME_ROOT);

        // the server's default config contains an invalid value (on purpose), tell the fat framework to ignore it!
        builderServer.addIgnoredErrors(Arrays.asList(JwtMessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE));

    }

    /**
     * <p>
     * Invoke the JWT Builder using a config that has a claims attribute that is empty. Make sure that we create a valid JWT Token
     * with NO extra claims.
     *
     * @throws Exception
     */
    @Test
    public void JwtBuilderAPIWithLDAPConfigTests_emptyClaims() throws Exception {

        String builderId = "emptyClaims";
        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);

        // have to set subject
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, "testuser");
        settings.put("overrideSettings", testSettings);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, builderServer);
        expectations = BuilderHelpers.buildBuilderClaimsNotFound(expectations, JwtConstants.JWT_BUILDER_CLAIM, "uid");
        expectations = BuilderHelpers.buildBuilderClaimsNotFound(expectations, JwtConstants.JWT_BUILDER_CLAIM, "sn");
        expectations = BuilderHelpers.buildBuilderClaimsNotFound(expectations, JwtConstants.JWT_BUILDER_CLAIM, "cn");

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Invoke the JWT Builder using a config that has a claims attribute set to "uid, sn, cn". Config is using LDAP, so we should
     * find all of these claims defined for the user specified.
     * <p>
     * Make sure that we create a valid JWT Token and that this token contains these 3 claims.
     *
     * @throws Exception
     */
    @Test
    public void JwtBuilderAPIWithLDAPConfigTests_specificClaims_allSet() throws Exception {

        String builderId = "specificClaims_allSet";
        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);
        settings.put("uid", "testuser");
        settings.put("sn", "testuser");
        settings.put("cn", "testuser");

        // have to set subject
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, "testuser");
        settings.put("overrideSettings", testSettings);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Invoke the JWT Builder using a config that has a claims attribute set to "uid, sn, cn, yourClaim". Config is using LDAP, so
     * we should find some of these claims defined for the user specified.
     * <p>
     * Make sure that we create a valid JWT Token and that this token contains the 3 valid claims and not the undefined 4th claim.
     * Also, make sure that no exceptions are raised because a claim can not be found.
     *
     * @throws Exception
     */
    @Test
    public void JwtBuilderAPIWithLDAPConfigTests_specificClaims_someNotSet() throws Exception {

        String builderId = "specificClaims_someNotSet";
        JSONObject settings = BuilderHelpers.setDefaultClaims(builderId);
        settings.put("uid", "testuser");
        settings.put("sn", "testuser");
        settings.put("cn", "testuser");
        // have to set subject
        JSONObject testSettings = new JSONObject();
        testSettings.put(ClaimConstants.SUBJECT, "testuser");
        settings.put("overrideSettings", testSettings);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, settings, builderServer);
        expectations = BuilderHelpers.buildBuilderClaimsNotFound(expectations, JwtConstants.JWT_BUILDER_CLAIM, "yourClaim");
        //        expectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_DOES_NOT_MATCH, JwtConstants.JWT_BUILDER_CLAIM + JwtConstants.JWT_BUILDER_JSON + JwtConstants.JWT_BUILDER_GETALLCLAIMS + ".*yourClaim.*", "Found unknown claim \"yourClaim\" in the listed claims and it should not be there."));
        //        expectations.addExpectation(new ResponseFullExpectation(JWTBuilderConstants.STRING_DOES_NOT_MATCH, JwtConstants.JWT_BUILDER_CLAIM + JwtConstants.JWT_BUILDER_JSON + "\\{" + ".*yourClaim.*\\}", "Found unknown claim \"yourClaim\" in the list of claims and it should not be there."));

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

}