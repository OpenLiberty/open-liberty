/*******************************************************************************
 * Copyright (c) 2026 IBM Corporation and others.
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
package com.ibm.ws.security.jwt.fat.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.json.JsonObject;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.json.java.JSONArray;
import com.ibm.json.java.JSONObject;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.utils.CommonWaitForAppChecks;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.jwt.fat.builder.actions.JwtBuilderActions;
import com.ibm.ws.security.jwt.fat.builder.utils.BuilderHelpers;
import com.ibm.ws.security.jwt.fat.builder.utils.JwtBuilderMessageConstants;
import com.ibm.ws.security.jwt.fat.builder.validation.BuilderTestValidationUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

/**
 * This is the test class that will run JWT Builder API workload identity tests.
 *
 **/

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class JwtBuilderAPIWorkloadIdentityTests extends CommonSecurityFat {
    @Server("com.ibm.ws.security.jwt_fat.builder")
    public static LibertyServer builderServer;

    private static final JwtBuilderActions actions = new JwtBuilderActions();
    public static final BuilderTestValidationUtils validationUtils = new BuilderTestValidationUtils();

    @BeforeClass
    public static void setUp() throws Exception {
        transformApps(builderServer);

        serverTracker.addServer(builderServer);
        skipRestoreServerTracker.addServer(builderServer);
        builderServer.addInstalledAppForValidation(JWTBuilderConstants.JWT_BUILDER_SERVLET);
        builderServer.startServerUsingExpandedConfiguration("server_workloadIdentity.xml", CommonWaitForAppChecks.getSecurityReadyMsgs());
        SecurityFatHttpUtils.saveServerPorts(builderServer, JWTBuilderConstants.BVT_SERVER_1_PORT_NAME_ROOT);

        builderServer.addIgnoredErrors(Arrays.asList(
                JwtBuilderMessageConstants.CWWKG0032W_CONFIG_INVALID_VALUE,
                JwtBuilderMessageConstants.CWWKS6055W_BETA_SIGNATURE_ALGORITHM_USED,
                JwtBuilderMessageConstants.CWWKS6074W_JWK_KEY_COVERAGE_WINDOW_TOO_SHORT));
    }

    @Mode(TestMode.LITE)
    @Test
    public void testWorkloadIdentityClaimIsPresent() throws Exception {
        // Make an HTTP request to the jwtbuilderclient servlet "create" endpoint on builderServer
        // This returns an HTTP response in which the created JWT is contained in the body
        Page response = actions.invokeJwtBuilder_create(_testName, builderServer, "workloadIdentityBuilder");
        // Extract the raw JWT string from the HTTP response body and parse it into a javax.json.JsonObject
        JsonObject payload = BuilderHelpers.extractJwtPayload(response);
        // Asserts the workload identity claim exists
        assertTrue("workload-identity claim was missing from JWT payload", payload.containsKey("workload-identity"));
        // Extracts the claim value as a plain string
        String workloadIdentity = payload.getString("workload-identity");
        // Splits the plain string on the comma delimiter
        String[] parts = workloadIdentity.split(",");
        // Validates the format contract: exactly 4 parts
        assertEquals("workload-identity claim should have 4 comma-separated parts", 4, parts.length);
        // parts[0] = hostname
        // parts[1] = usr dir
        // parts[2] = server name
        // parts[3] = app name
        assertFalse("hostname part should not be empty", parts[0].isEmpty());
        assertFalse("usr dir part should not be empty", parts[1].isEmpty());
        assertEquals("server name part should match", "com.ibm.ws.security.jwt_fat.builder", parts[2].trim());
        assertEquals("app name part should match", "jwtbuilderclient", parts[3].trim());
    }

    @Mode(TestMode.LITE)
    @Test
    public void testWorkloadIdentityClaimCannotBeOverwritten() throws Exception {
        // Use the workload identity builder config so the claim name is reserved
        String builderId = "workloadIdentityBuilder";
        // Attempt to overwrite the workload identity claim via the .claim() API
        // The JSON object representing the API calls the test servlet will make
        JSONObject testSettings = new JSONObject();
        // The JSON object that holds the parameters to be passed to .claim()
        JSONObject claimsToSet = new JSONObject();
        claimsToSet.put("workload-identity", "xyz");
        // Packages the claim attempt under the claim_api key
        // The servlet knows any entry under claim_api should be passed to .claim()
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_CLAIM_API, claimsToSet);
        // Expect an error message in the response indicating the claim is protected
        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(
            JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT,
            JwtBuilderMessageConstants.CWWKS6071E_WORKLOAD_IDENTITY_CLAIM_CANNOT_BE_MODIFIED,
            builderServer
        );
        // Create an additional HTTP request parameter that tells the servlet to call .claim() once per entry
        List<NameValuePair> extraParams = new ArrayList<NameValuePair>();
        extraParams.add(new NameValuePair(JWTBuilderConstants.ADD_CLAIMS_AS, JWTBuilderConstants.AS_SINGLE));
        // Make the HTTP request to the jwtbuilderclient servlet, which creates the builder from workloadIdentityBuilder
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, extraParams, testSettings);
        validationUtils.validateResult(response, expectations);
    }

    @Mode(TestMode.LITE)
    @Test
    public void testWorkloadIdentityClaimCannotBeRemoved() throws Exception {
        // Use the workload identity builder config so the claim name is reserved
        String builderId = "workloadIdentityBuilder";
        // Attempt to remove the workload identity claim via the .remove() API
        // The JSON object representing the API calls the test servlet will make
        JSONObject testSettings = new JSONObject();
        // The JSON object that holds the parameters to be passed to .claim()
        JSONArray claimsToRemove = new JSONArray();
        claimsToRemove.add("workload-identity");
        // Packages the remove attempt under the remove_api key
        // The servlet knows any entry under remove_api should be passed to .remove()
        testSettings.put(JWTBuilderConstants.JWT_BUILDER_REMOVE_API, claimsToRemove);
        // Expect an error message in the response indicating the claim is protected
        Expectations expectations = BuilderHelpers.createBadBuilderExpectations(
            JWTBuilderConstants.JWT_BUILDER_SETAPIS_ENDPOINT,
            JwtBuilderMessageConstants.CWWKS6071E_WORKLOAD_IDENTITY_CLAIM_CANNOT_BE_MODIFIED,
            builderServer
        );
        // Make the HTTP request to the jwtbuilderclient servlet, which creates the builder from workloadIdentityBuilder
        Page response = actions.invokeJwtBuilder_setApis(_testName, builderServer, builderId, testSettings);
        validationUtils.validateResult(response, expectations);
    }
}