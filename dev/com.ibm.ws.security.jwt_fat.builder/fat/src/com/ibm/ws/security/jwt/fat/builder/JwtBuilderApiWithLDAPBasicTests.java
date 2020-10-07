/*******************************************************************************
 * Copyright (c) 2019, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwt.fat.builder;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.jwt.JwtMessageConstants;
import com.ibm.ws.security.fat.common.jwt.PayloadConstants;
import com.ibm.ws.security.fat.common.servers.ServerBootstrapUtils;
import com.ibm.ws.security.fat.common.utils.CommonWaitForAppChecks;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.builder.actions.JwtBuilderActions;
import com.ibm.ws.security.jwt.fat.builder.utils.BuilderHelpers;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will run basic JWT Builder tests with LDAP.
 *
 **/

@SuppressWarnings("restriction")
@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JwtBuilderApiWithLDAPBasicTests extends JwtBuilderCommonLDAPFat {

    @Server("com.ibm.ws.security.jwt_fat.builder")
    public static LibertyServer builderServer;

    private static final JwtBuilderActions actions = new JwtBuilderActions();
    public static final TestValidationUtils validationUtils = new TestValidationUtils();
    protected static ServerBootstrapUtils bootstrapUtils = new ServerBootstrapUtils();

    @ClassRule
    public static RepeatTests r = RepeatTests.withoutModification();

    @BeforeClass
    public static void setUp() throws Exception {

        setupLdapServer(builderServer);

        serverTracker.addServer(builderServer);
        builderServer.addInstalledAppForValidation(JWTBuilderConstants.JWT_BUILDER_SERVLET);
        builderServer.startServerUsingExpandedConfiguration("server_LDAPRegistry.xml", CommonWaitForAppChecks.getSecurityReadyMsgs());
        SecurityFatHttpUtils.saveServerPorts(builderServer, JWTBuilderConstants.BVT_SERVER_1_PORT_NAME_ROOT);

    }

    /**************************************************************
     * Test Builder create specific Tests
     **************************************************************/

    /**
     * <p>
     * Invoke the JWT Builder and run fetch for 4 different claims, "uid, sn, cn, homeStreet". Config is using LDAP, so we should
     * be able to
     * retrieve all of these claims defined for the user specified. (homeStreet is something unique to our LDAP)
     * <p>
     * Make sure that we create a valid JWT Token and that this token contains these 3 claims.
     *
     * @throws Exception
     */
    @Test
    public void JwtBuilderAPIWithLDAPBasicTests_runFetch_subjectSet_existingClaims() throws Exception {

        String builderId = "jwt1";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        testSettings.put(PayloadConstants.SUBJECT, USER);
        JSONArray claimsToFetch = new JSONArray();
        claimsToFetch.add("uid");
        claimsToFetch.add("sn");
        claimsToFetch.add("cn");
        claimsToFetch.add("homeStreet");
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_FETCH_API, claimsToFetch);

        expectationSettings.put("overrideSettings", testSettings);

        // for validation purposes only, create a map of what we expect to find for the values returned from the registry
        JSONObject fetchSettings = new JSONObject();
        fetchSettings.put("uid", USER);
        fetchSettings.put("sn", USER);
        fetchSettings.put("cn", USER);
        fetchSettings.put("homeStreet", "Burnet");
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, fetchSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Invoke the JWT Builder and run fetch for with a null value.
     * <p>
     * Make sure that we get an invalid claim exception. Allow test client to continue to generate a JWT Token. Make sure that the
     * token has not been corrupted by the failed fetch request.
     *
     * @throws Exception
     */
    @Test
    public void JwtBuilderAPIWithLDAPBasicTests_runFetch_subjectSet_nullClaims() throws Exception {

        String builderId = "jwt1";

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        testSettings.put(PayloadConstants.SUBJECT, USER);
        JSONArray claimsToFetch = new JSONArray();
        claimsToFetch.add(null);
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_FETCH_API, claimsToFetch);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6015E_INVALID_CLAIM, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Invoke the JWT Builder and run fetch for with an empty ("") value.
     * <p>
     * Make sure that we get an invalid claim exception. Allow test client to continue to generate a JWT Token. Make sure that the
     * token has not been corrupted by the failed fetch request.
     *
     * @throws Exception
     */
    @Test
    public void JwtBuilderAPIWithLDAPBasicTests_runFetch_subjectSet_emptyClaim() throws Exception {

        String builderId = "jwt1";

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        testSettings.put(PayloadConstants.SUBJECT, USER);
        JSONArray claimsToFetch = new JSONArray();
        claimsToFetch.add("");
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_FETCH_API, claimsToFetch);

        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, JwtMessageConstants.CWWKS6015E_INVALID_CLAIM, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Invoke the JWT Builder and run fetch for a claim that doesn't exist. Config is using LDAP, we should NOT find the claim
     * specified.
     * <p>
     * Make sure that we create a valid JWT Token and that this token does not contain the undefined claim. Also, make sure that
     * no exceptions are raised because a claim can not be found.
     *
     * @throws Exception
     */
    @Test
    public void JwtBuilderAPIWithLDAPBasicTests_runFetch_subjectSet_nonExistingClaim() throws Exception {

        String builderId = "jwt1";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        testSettings.put(PayloadConstants.SUBJECT, USER);
        JSONArray claimsToFetch = new JSONArray();
        claimsToFetch.add("doesntExistClaim");
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_FETCH_API, claimsToFetch);

        expectationSettings.put("overrideSettings", testSettings);

        JSONObject fetchSettings = new JSONObject();
        fetchSettings.put("doesntExistClaim", null);
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, fetchSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }

    /**
     * <p>
     * Invoke the JWT Builder, do NOT set the subject and run fetch for claims that do exist. Config is using LDAP, we should NOT
     * find the claim specified.
     * <p>
     * Make sure that we create a valid JWT Token and that this token does not contain the requested claims. Also, make sure that
     * no exceptions are raised because a claim can not be found.
     *
     * @throws Exception
     */
    @Test
    public void JwtBuilderAPIWithLDAPBasicTests_runFetch_subjectNotSet_existingClaims() throws Exception {

        String builderId = "jwt1";
        JSONObject expectationSettings = BuilderHelpers.setDefaultClaims(builderId);

        // create settings that will be passed to the test app as well as used to create what to expect in the results
        // set freeform claims into a json object.  Add that object into the json object of things to set
        JSONObject testSettings = new JSONObject();
        JSONArray claimsToFetch = new JSONArray();
        claimsToFetch.add("uid");
        claimsToFetch.add("sn");
        claimsToFetch.add("cn");
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_FETCH_API, claimsToFetch);

        expectationSettings.put("overrideSettings", testSettings);

        // for validation purposes only, create a map of what we expect to find for the values returned from the registry
        JSONObject fetchSettings = new JSONObject();
        fetchSettings.put("uid", null); // shouldn't have uid
        fetchSettings.put("sn", null); // shouldn't have sn
        fetchSettings.put("cn", null); // shouldn't have cn
        Expectations expectations = BuilderHelpers.createGoodBuilderExpectations(JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT, expectationSettings, fetchSettings, builderServer);

        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);

    }
}
