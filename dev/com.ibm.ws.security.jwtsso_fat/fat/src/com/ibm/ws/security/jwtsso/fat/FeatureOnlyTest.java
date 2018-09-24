/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.fat;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwtsso.fat.utils.CommonExpectations;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatActions;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatConstants;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServer;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class FeatureOnlyTest extends CommonSecurityFat {

    protected static Class<?> thisClass = FeatureOnlyTest.class;

    @Server("com.ibm.ws.security.jwtsso.fat")
    public static LibertyServer server;

    private final JwtFatActions actions = new JwtFatActions();
    private final TestValidationUtils validationUtils = new TestValidationUtils();
    private WebClient webClient = new WebClient();

    String protectedUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + JwtFatConstants.SIMPLE_SERVLET_PATH;
    String defaultUser = JwtFatConstants.TESTUSER;
    String defaultPassword = JwtFatConstants.TESTUSERPWD;

    @BeforeClass
    public static void setUp() throws Exception {
        server.addInstalledAppForValidation(JwtFatConstants.APP_FORMLOGIN);
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration("server_withFeature.xml");

    }

    @Before
    public void beforeTest() {
        webClient = new WebClient();
    }

    /**
     * Tests:
     * - Invoke the protected resource with the JWT SSO feature configured
     * - Log in with valid credentials
     * Expects:
     * - Should reach the protected resource
     * - JWT SSO cookie should be present
     * - LTPA cookie should NOT be present
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_simpleLogin_featureEnabled() throws Exception {

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));
        expectations.addExpectations(CommonExpectations.cookieDoesNotExist(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.cookieDoesNotExist(currentAction, webClient, JwtFatConstants.LTPA_COOKIE_NAME));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;

        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, defaultUser));
        expectations.addExpectations(CommonExpectations.jwtCookieExists(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.cookieDoesNotExist(currentAction, webClient, JwtFatConstants.LTPA_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Invoke the protected resource without the JWT SSO feature configured
     * - Log in with valid credentials
     * Expects:
     * - Should reach the protected resource
     * - JWT SSO cookie should NOT be present
     * - LTPA cookie should be present
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_simpleLogin_featureNotEnabled() throws Exception {

        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_noFeature.xml");

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));
        expectations.addExpectations(CommonExpectations.cookieDoesNotExist(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.cookieDoesNotExist(currentAction, webClient, JwtFatConstants.LTPA_COOKIE_NAME));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithLtpaCookie(currentAction, webClient, protectedUrl, defaultUser));
        expectations.addExpectations(CommonExpectations.ltpaCookieExists(currentAction, webClient));
        expectations.addExpectations(CommonExpectations.cookieDoesNotExist(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.JWT_COOKIE_NAME));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

}
