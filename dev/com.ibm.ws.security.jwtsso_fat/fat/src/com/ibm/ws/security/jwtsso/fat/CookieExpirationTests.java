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

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.utils.CommonWaitForAppChecks;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwtsso.fat.actions.JwtFatActions;
import com.ibm.ws.security.jwtsso.fat.actions.RunWithMpJwtVersion;
import com.ibm.ws.security.jwtsso.fat.utils.CommonExpectations;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatConstants;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatUtils;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class CookieExpirationTests extends CommonSecurityFat {

    protected static Class<?> thisClass = CookieExpirationTests.class;

    @ClassRule
    public static RepeatTests r = RepeatTests.with(new RunWithMpJwtVersion("mpJwt11")).andWith(new RunWithMpJwtVersion("mpJwt12"));

    @Server("com.ibm.ws.security.jwtsso.fat")
    public static LibertyServer server;

    private final JwtFatActions actions = new JwtFatActions();
    private final TestValidationUtils validationUtils = new TestValidationUtils();
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

    /**
     * Tests:
     * - Log into the protected resource with the JWT SSO feature configured
     * - JWT builder being used has its expiry attribute set to 0
     * - JWT consumer with the default clock skew will be used
     * - Sleep a few seconds to go beyond the JWT SSO cookie's lifetime
     * - Re-invoke the protected resource with the JWT SSO cookie
     * Expects:
     * - Should reach the protected resource because we're still within the JWT consumer's clock skew
     */
    @Test
    public void test_shortJwtCookieLifetime_reuseCookieWithinClockSkew() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_shortJwtLifetime.xml");

        WebClient webClient = new WebClient();
        String expectedIssuer = "https://" + "[^/]+" + "/jwt/builder_shortLifetime";

        Cookie jwtCookie = actions.logInAndObtainJwtCookie(_testName, webClient, protectedUrl, defaultUser, defaultPassword, expectedIssuer);

        Log.info(thisClass, _testName, "Sleeping beyond JWT SSO cookie's lifetime...");
        Thread.sleep(5000);

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, defaultUser, expectedIssuer));
        expectations.addExpectations(CommonExpectations.jwtCookieExists(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.cookieDoesNotExist(currentAction, webClient, JwtFatConstants.LTPA_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));

        Page response = actions.invokeUrlWithCookie(_testName, protectedUrl, jwtCookie);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Log into the protected resource with the JWT SSO feature configured
     * - JWT builder being used has its expiry attribute set to 0
     * - JWT consumer being used has its clockSkew attribute set to a very short time (a few seconds)
     * - Sleep a few seconds to go beyond the JWT SSO cookie's lifetime and the consumer's clock skew
     * - Re-invoke the protected resource with the JWT SSO cookie
     * Expects:
     * - Should be prompted with the login page because the JWT SSO cookie is no longer valid
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException", "com.ibm.websphere.security.jwt.InvalidTokenException",
                   "com.ibm.ws.security.authentication.AuthenticationException" })
    @Test
    public void test_shortJwtCookieLifetime_reuseCookieOutsideClockSkew() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_shortJwtLifetime_shortClockSkew.xml");

        WebClient webClient = new WebClient();
        String expectedIssuer = "https://" + "[^/]+" + "/jwt/builder_shortLifetime";

        Cookie jwtCookie = actions.logInAndObtainJwtCookie(_testName, webClient, protectedUrl, defaultUser, defaultPassword, expectedIssuer);

        Log.info(thisClass, _testName, "Sleeping beyond JWT SSO cookie's lifetime and JWT consumer's clock skew...");
        Thread.sleep(8000);

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));
        // as of 6248 we now run quietly when a cookie routinely expires.
        /*
         * expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS6025E_JWT_TOKEN_EXPIRED));
         * expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS6031E_JWT_ERROR_PROCESSING_JWT));
         * expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS5524E_ERROR_CREATING_JWT));
         * expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ));
         */

        Page response = actions.invokeUrlWithCookie(_testName, protectedUrl, jwtCookie);
        validationUtils.validateResult(response, currentAction, expectations);
    }

}
