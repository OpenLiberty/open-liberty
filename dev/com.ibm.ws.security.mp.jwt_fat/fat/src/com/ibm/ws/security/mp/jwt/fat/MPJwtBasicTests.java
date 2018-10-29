/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.mp.jwt.fat;

import java.util.Arrays;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.jwt.JwtTokenForTest;
import com.ibm.ws.security.fat.common.servers.ServerBootstrapUtils;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwt.fat.mpjwt.MpJwtFatConstants;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class MPJwtBasicTests extends CommonMpJwtFat {

    protected static Class<?> thisClass = MPJwtBasicTests.class;
    protected static ServerBootstrapUtils bootstrapUtils = new ServerBootstrapUtils();

    @Server("com.ibm.ws.security.mp.jwt.fat")
    public static LibertyServer resourceServer;

    @Server("com.ibm.ws.security.mp.jwt.fat.builder")
    public static LibertyServer jwtBuilderServer;

    private final TestValidationUtils validationUtils = new TestValidationUtils();

    String defaultUser = MpJwtFatConstants.TESTUSER;
    String defaultPassword = MpJwtFatConstants.TESTUSERPWD;

    @BeforeClass
    public static void setUp() throws Exception {

        setUpAndStartRSServerForTests(resourceServer, "rs_server_orig_withAudience.xml");

        setUpAndStartBuilderServer(jwtBuilderServer, "server_using_buildApp.xml");

    }

    protected static void setUpAndStartRSServerForTests(LibertyServer server, String configFile) throws Exception {
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTNAME, SecurityFatHttpUtils.getServerHostName());
        bootstrapUtils.writeBootstrapProperty(server, MpJwtFatConstants.BOOTSTRAP_PROP_FAT_SERVER_HOSTIP, SecurityFatHttpUtils.getServerHostIp());
        bootstrapUtils.writeBootstrapProperty(server, "mpJwt_keyName", "rsacert");
        bootstrapUtils.writeBootstrapProperty(server, "mpJwt_jwksUri", "");
        deployRSServerApiTestApps(server);
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration(configFile);
        SecurityFatHttpUtils.saveServerPorts(server, MpJwtFatConstants.BVT_SERVER_1_PORT_NAME_ROOT);
        server.addIgnoredErrors(Arrays.asList(MpJwtMessageConstants.CWWKW1001W_CDI_RESOURCE_SCOPE_MISMATCH));
    }

    /***************************************************** Tests ****************************************************/

    /******************** SecurityContext *********************/
    @Mode(TestMode.LITE)
    @Test
    public void MPJwtBasicTests_mainPath_usingSecurityContext_RequestScoped() throws Exception {

        testMainPath(MpJwtFatConstants.MPJWT_APP_SEC_CONTEXT_REQUEST_SCOPE, MpJwtFatConstants.MPJWT_APP_CLASS_SEC_CONTEXT_REQUEST_SCOPE);

    }

    @Mode(TestMode.LITE)
    @Test
    public void MPJwtBasicTests_mainPath_usingSecurityContext_ApplicationScoped() throws Exception {

        testMainPath(MpJwtFatConstants.MPJWT_APP_SEC_CONTEXT_APP_SCOPE, MpJwtFatConstants.MPJWT_APP_CLASS_SEC_CONTEXT_APP_SCOPE);

    }

    @Mode(TestMode.LITE)
    @Test
    public void MPJwtBasicTests_mainPath_usingSecurityContext_SessionScoped() throws Exception {

        testMainPath(MpJwtFatConstants.MPJWT_APP_SEC_CONTEXT_SESSION_SCOPE, MpJwtFatConstants.MPJWT_APP_CLASS_SEC_CONTEXT_SESSION_SCOPE);

    }

    @Mode(TestMode.LITE)
    @Test
    public void MPJwtBasicTests_mainPath_usingSecurityContext_NotScoped() throws Exception {

        testMainPath(MpJwtFatConstants.MPJWT_APP_SEC_CONTEXT_NO_SCOPE, MpJwtFatConstants.MPJWT_APP_CLASS_SEC_CONTEXT_NO_SCOPE);

    }

    /******************** JsonWebToken Injection *********************/
    @Mode(TestMode.LITE)
    @Test
    public void MPJwtBasicTests_mainPath_usingInjection_RequestScoped() throws Exception {

        testMainPath(MpJwtFatConstants.MPJWT_APP_TOKEN_INJECT_REQUEST_SCOPE, MpJwtFatConstants.MPJWT_APP_CLASS_TOKEN_INJECT_REQUEST_SCOPE);

    }

    /**
     * Test that injection of JsonWebToken in an application scoped app will work properly.
     * Claim Injection in an application scoped app does NOT allow the app to start - that is
     * tested by a separate test class using a separage server, ...
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwtBasicTests_mainPath_usingInjection_ApplicationScoped() throws Exception {

        testMainPath(MpJwtFatConstants.MPJWT_APP_TOKEN_INJECT_APP_SCOPE, MpJwtFatConstants.MPJWT_APP_CLASS_TOKEN_INJECT_APP_SCOPE);

    }

    @Mode(TestMode.LITE)
    @Test
    public void MPJwtBasicTests_mainPath_usingInjection_SessionScoped() throws Exception {

        testMainPath(MpJwtFatConstants.MPJWT_APP_TOKEN_INJECT_SESSION_SCOPE, MpJwtFatConstants.MPJWT_APP_CLASS_TOKEN_INJECT_SESSION_SCOPE);

    }

    @Mode(TestMode.LITE)
    @Test
    public void MPJwtBasicTests_mainPath_usingInjection_NotScoped() throws Exception {

        testMainPath(MpJwtFatConstants.MPJWT_APP_TOKEN_INJECT_NO_SCOPE, MpJwtFatConstants.MPJWT_APP_CLASS_TOKEN_INJECT_NO_SCOPE);

    }

    /******************** Claim Injection *********************/
    /**
     * Invoke app that uses claim injection of various types (raw, jsonvalue, instance, ...)
     * Expect that we will be able to invoke the app and that the values obtained via instance
     * will be correct. The other values may or may not be correct as the scoping will decide when
     * they are updated.
     * The tests that will run where the non-instance values should all be correct will
     * make sure that NO messages indicating a mis-match were found
     */
    /**
     * With request scope - all values should be correct
     *
     * @throws Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void MPJwtBasicTests_mainPath_usingClaimInjection_RequestScoped() throws Exception {

        testMainPath(MpJwtFatConstants.MPJWT_APP_CLAIM_INJECT_REQUEST_SCOPE, MpJwtFatConstants.MPJWT_APP_CLASS_CLAIM_INJECT_REQUEST_SCOPE);

    }

    @Mode(TestMode.LITE)
    @Test
    public void MPJwtBasicTests_mainPath_usingClaimInjection_NotScoped() throws Exception {

        testMainPath(MpJwtFatConstants.MPJWT_APP_CLAIM_INJECT_NO_SCOPE, MpJwtFatConstants.MPJWT_APP_CLASS_CLAIM_INJECT_NO_SCOPE);

    }

    @Mode(TestMode.LITE)
    @Test
    public void MPJwtBasicTests_mainPath_usingClaimInjection_ApplicationScope_Instance() throws Exception {

        testMainPath(MpJwtFatConstants.MPJWT_APP_CLAIM_INJECT_APP_SCOPE, MpJwtFatConstants.MPJWT_APP_CLASS_CLAIM_INJECT_APP_SCOPE);

    }

    @Mode(TestMode.LITE)
    @Test
    public void MPJwtBasicTests_mainPath_usingClaimInjection_SessionScope_Instance() throws Exception {

        testMainPath(MpJwtFatConstants.MPJWT_APP_CLAIM_INJECT_SESSION_SCOPE, MpJwtFatConstants.MPJWT_APP_CLASS_CLAIM_INJECT_SESSION_SCOPE);

    }

    /**
     * This method obtains a (mp)jwt in 2 ways (to validate that we're able to generate and consume the tokens created via various
     * means -
     * this is also causing us to hit our app multiple times to ensure that injected values are not not cached (they are correct
     * for the current token)
     * 1) invoke the jwt endpoint (which will issue an mp-jwt token)
     * 2) use an app that will invoke the jwt builder, This will generate a jwt token that we've added the extra upn claim to.
     * This method will then invoke the "test" app that will pass the token as we invoke the specified test app. All tests in this
     * class expect successful results.
     * We'll check the values that the test app logs - these values are obtained via various types of injection in the app.
     *
     * @param app
     *            - app to invoke
     * @param className
     *            - the className that we expect to see logged (the class that we expect to have invoked)
     * @throws Exception
     */
    public void testMainPath(String app, String className) throws Exception {
        testMainPath(actions.getJwtFromTokenEndpoint(_testName, "defaultJWT1", SecurityFatHttpUtils.getServerSecureUrlBase(jwtBuilderServer), defaultUser, defaultPassword), app,
                     className);

        testMainPath(actions.getJwtTokenUsingBuilder(_testName, jwtBuilderServer), app, className);
    }

    /**
     * This method is passed an (MP-Jwt) token. It can be built in different ways (using: the jwt endpoint, the jwt builder,
     * ...(OP server generated))
     * We then invoke the specified test app (the test apps use different types of injection)
     * The test apps log various values and we'll check those values against what we expect to get for the injection type and
     * scope.
     *
     * @param builtToken
     *            - the token that was just built
     * @param app
     *            - the app to invoke
     * @param className
     *            - the class name that we expect to see logged (tells us if we invoked the correct app)
     * @throws Exception
     */
    public void testMainPath(String builtToken, String app, String className) throws Exception {

        JwtTokenForTest jwtTokenTools = new JwtTokenForTest(builtToken);

        String testUrl = buildAppUrl(resourceServer, MpJwtFatConstants.MICROPROFILE_SERVLET, app);

        WebClient webClient = actions.createWebClient();

        Page response = actions.invokeUrlWithBearerToken(_testName, webClient, testUrl, builtToken);
        Expectations expectations = goodTestExpectations(jwtTokenTools, testUrl, className);
        validationUtils.validateResult(response, expectations);
    }

}
