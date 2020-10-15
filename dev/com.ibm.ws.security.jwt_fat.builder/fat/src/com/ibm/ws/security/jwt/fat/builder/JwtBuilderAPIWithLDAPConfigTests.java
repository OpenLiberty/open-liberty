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
import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.jwt.PayloadConstants;
import com.ibm.ws.security.fat.common.utils.CommonWaitForAppChecks;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.builder.actions.JwtBuilderActions;
import com.ibm.ws.security.jwt.fat.builder.utils.BuilderHelpers;
import com.ibm.ws.security.jwt.fat.builder.utils.JwtBuilderMessageConstants;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will run basic JWT Builder Config tests with LDAP.
 *
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JwtBuilderAPIWithLDAPConfigTests extends JwtBuilderCommonLDAPFat {

    @Server("com.ibm.ws.security.jwt_fat.builder")
    public static LibertyServer builderServer;

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    private static final JwtBuilderActions actions = new JwtBuilderActions();
    public static final TestValidationUtils validationUtils = new TestValidationUtils();

    @BeforeClass
    public static void setUp() throws Exception {

        setupLdapServer(builderServer);

        serverTracker.addServer(builderServer);
        builderServer.addInstalledAppForValidation(JWTBuilderConstants.JWT_BUILDER_SERVLET);
        builderServer.startServerUsingExpandedConfiguration("server_LDAPRegistry_configTests.xml", CommonWaitForAppChecks.getSecurityReadyMsgs());
        SecurityFatHttpUtils.saveServerPorts(builderServer, JWTBuilderConstants.BVT_SERVER_1_PORT_NAME_ROOT);

        // the server's default config contains an invalid value (on purpose), tell the fat framework to ignore it!
        builderServer.addIgnoredErrors(Arrays.asList(JwtBuilderMessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE, JwtBuilderMessageConstants.CWWKS6059W_KEY_MANAGEMENT_KEY_ALIAS_MISSING));

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
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);

        // have to set subject
        JSONObject testSettings = new JSONObject();
        testSettings.put(PayloadConstants.SUBJECT, USER);
        expectationSettings.put("overrideSettings", testSettings);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);
        expectations = BuilderHelpers.buildBuilderClaimsNotFound(expectations, JWTBuilderConstants.JWT_CLAIM, "uid");
        expectations = BuilderHelpers.buildBuilderClaimsNotFound(expectations, JWTBuilderConstants.JWT_CLAIM, "sn");
        expectations = BuilderHelpers.buildBuilderClaimsNotFound(expectations, JWTBuilderConstants.JWT_CLAIM, "cn");

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
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        expectationSettings.put("uid", USER);
        expectationSettings.put("sn", USER);
        expectationSettings.put("cn", USER);

        // have to set subject
        JSONObject testSettings = new JSONObject();
        testSettings.put(PayloadConstants.SUBJECT, USER);
        expectationSettings.put("overrideSettings", testSettings);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);

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
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);
        expectationSettings.put("uid", USER);
        expectationSettings.put("sn", USER);
        expectationSettings.put("cn", USER);
        // have to set subject
        JSONObject testSettings = new JSONObject();
        testSettings.put(PayloadConstants.SUBJECT, USER);
        expectationSettings.put("overrideSettings", testSettings);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, builderServer);
        expectations = BuilderHelpers.buildBuilderClaimsNotFound(expectations, JWTBuilderConstants.JWT_CLAIM, "yourClaim");

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

}
