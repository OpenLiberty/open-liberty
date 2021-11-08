/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.fat;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.utils.CommonWaitForAppChecks;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwtsso.fat.actions.JwtFatActions;
import com.ibm.ws.security.jwtsso.fat.utils.CommonExpectations;
import com.ibm.ws.security.jwtsso.fat.utils.CommonJwtssoFat;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatConstants;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class BuilderTests extends CommonJwtssoFat {

    protected static Class<?> thisClass = BuilderTests.class;

    @Server("com.ibm.ws.security.jwtsso.fat")
    public static LibertyServer server;

    private final JwtFatActions actions = new JwtFatActions();
    private final TestValidationUtils validationUtils = new TestValidationUtils();
    private static JwtFatUtils fatUtils = new JwtFatUtils();

    String protectedUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + JwtFatConstants.SIMPLE_SERVLET_PATH;
    String defaultUser = JwtFatConstants.TESTUSER;
    String defaultPassword = JwtFatConstants.TESTUSERPWD;

    @BeforeClass
    public static void setUp() throws Exception {

        fatUtils.updateFeatureFileForEE9(server);
        transformApps(server);

        server.addInstalledAppForValidation(JwtFatConstants.APP_FORMLOGIN);
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration("server_withFeature.xml", CommonWaitForAppChecks.getLTPAReadyMsgs(CommonWaitForAppChecks.getSSLChannelReadyMsgs()));
    }

    /**
     * Tests:
     * - Log into a protected resource with the jwtSso element pointing to a jwtBuilder that has JWK enabled
     * Expects:
     * - Should successfully reach protected resource
     * - JWT cookie header should include a "kid" entry for the JWK ID
     */
    @Test
    public void test_jwkEnabled() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_builder_jwkEnabled.xml");

        WebClient webClient = actions.createWebClient();

        String builderId = "builder_jwkEnabled";

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;

        String expectedIssuer = "https://[^/]+/jwt/" + builderId;
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, defaultUser, expectedIssuer));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.jwtCookieExists(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME, JwtFatConstants.SECURE,
                                                                        JwtFatConstants.HTTPONLY));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);

        Cookie jwtCookie = webClient.getCookieManager().getCookie(JwtFatConstants.JWT_COOKIE_NAME);
        verifyJwtHeaderContainsKey(jwtCookie.getValue(), "kid");
        actions.destroyWebClient(webClient);

    }

    /**
     * Tests:
     * - Log into a protected resource with the jwtSso element not pointing to a jwtBuilder
     * - MP JWT consumer configuration specifies the jwksUri attribute
     * Expects:
     * - Should successfully reach protected resource
     * - JWT cookie should be signed by a certificate from the keystore, and its header should NOT include a "kid" entry
     */
    @Test
    public void test_noBuilderRef_mpJwtJwksUriConfigured() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_noBuilder_jwksUriConfigured.xml");

        WebClient webClient = actions.createWebClient();

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;

        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, defaultUser));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);

        //Cookie jwtCookie = webClient.getCookieManager().getCookie(JwtFatConstants.JWT_COOKIE_NAME);
        //verifyJwtHeaderDoesNotContainKey(jwtCookie.getValue(), "kid");

        actions.destroyWebClient(webClient);
    }

}
