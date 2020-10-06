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
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.Constants;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.utils.CommonWaitForAppChecks;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwtsso.fat.actions.JwtFatActions;
import com.ibm.ws.security.jwtsso.fat.actions.RunWithMpJwtVersion;
import com.ibm.ws.security.jwtsso.fat.utils.CommonExpectations;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatConstants;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class FeatureOnlyTest extends CommonSecurityFat {

    protected static Class<?> thisClass = FeatureOnlyTest.class;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new RunWithMpJwtVersion("mpJwt11")).andWith(new RunWithMpJwtVersion("mpJwt12"));

    @Server("com.ibm.ws.security.jwtsso.fat")
    public static LibertyServer server;

    private final JwtFatActions actions = new JwtFatActions();
    private final TestValidationUtils validationUtils = new TestValidationUtils();
    private WebClient webClient = new WebClient();
    private static JwtFatUtils fatUtils = new JwtFatUtils();

    String protectedUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + JwtFatConstants.SIMPLE_SERVLET_PATH;
    String defaultUser = JwtFatConstants.TESTUSER;
    String defaultPassword = JwtFatConstants.TESTUSERPWD;

    @BeforeClass
    public static void setUp() throws Exception {

        fatUtils.updateFeatureFile(server, "jwtSsoFeatures", RepeatTestFilter.CURRENT_REPEAT_ACTION);

        server.addInstalledAppForValidation(JwtFatConstants.APP_FORMLOGIN);
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration("server_withFeature.xml", CommonWaitForAppChecks.getSSLChannelReadyMsgs());

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
        expectations.addExpectation(Expectation.createResponseExpectation(currentAction, JwtFatConstants.JWT_COOKIE_NAME_MSG + JwtFatConstants.JWT_COOKIE_NAME,
                                                                          "Response from test step " + currentAction + " did not match expected value."));
        expectations.addExpectation(new ResponseFullExpectation(currentAction, Constants.STRING_MATCHES, JwtFatConstants.JWT_PRINCIPAL_MSG + ".*"
                                                                                                         + JwtFatConstants.RAW_TOKEN_KEY, "Response from test step " + currentAction
                                                                                                                                          + " did not match expected value."));

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
        expectations.addExpectation(Expectation.createResponseExpectation(currentAction, JwtFatConstants.JWT_COOKIE_NAME_MSG + "null",
                                                                          "Response from test step " + currentAction + " did not match expected value."));
        // syntax of the class name can vary ("." vs "/"), so make check a bit more generic
        expectations.addExpectation(new ResponseFullExpectation(currentAction, Constants.STRING_MATCHES, JwtFatConstants.JWT_PRINCIPAL_MSG
                                                                                                         + ".*org.*eclipse.*microprofile.*jwt.*JsonWebToken", "Response from test step "
                                                                                                                                                              + currentAction
                                                                                                                                                              + " did not match expected value."));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

}
