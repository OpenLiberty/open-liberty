/*******************************************************************************
 * Copyright (c) 2021 IBM Corporation and others.
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
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.utils.CommonWaitForAppChecks;
import com.ibm.ws.security.fat.common.utils.ServerFileUtils;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwtsso.fat.actions.JwtFatActions;
import com.ibm.ws.security.jwtsso.fat.actions.RunWithMpJwtVersion;
import com.ibm.ws.security.jwtsso.fat.utils.CommonExpectations;
import com.ibm.ws.security.jwtsso.fat.utils.CommonJwtssoFat;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatConstants;
import com.ibm.ws.security.jwtsso.fat.utils.MessageConstants;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

/**
 * Test jwtSso with builders that create tokens using a variety of signature algorithms. Also use
 * mpJwt configs to consume tokens created with different signature algorithms.
 * We test all possible signature algorithms in the jwtBuilder and mpJwt FATS. Will not
 * test all possible signature algorithms in this FAT since that's already done.
 */

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class SigAlgTests extends CommonJwtssoFat {

    protected static Class<?> thisClass = SigAlgTests.class;

    // can't run without mpJwt configured for these tests, but we should be testing with
    // all available mpJwt versions
    @ClassRule
    public static RepeatTests r = RepeatTests.with(new RunWithMpJwtVersion(JwtFatConstants.MPJWT_VERSION_11))
                    .andWith(new RunWithMpJwtVersion(JwtFatConstants.MPJWT_VERSION_12));

    @Server("com.ibm.ws.security.jwtsso.fat")
    public static LibertyServer server;

    private final JwtFatActions actions = new JwtFatActions();
    private final TestValidationUtils validationUtils = new TestValidationUtils();
    private static ServerFileUtils fatUtils = new ServerFileUtils();

    String protectedUrl = "https://" + server.getHostname() + ":" + server.getHttpDefaultSecurePort() + JwtFatConstants.SIMPLE_SERVLET_PATH;
    String defaultUser = JwtFatConstants.TESTUSER;
    String defaultPassword = JwtFatConstants.TESTUSERPWD;

    @BeforeClass
    public static void setUp() throws Exception {

        fatUtils.updateFeatureFile(server, "jwtSsoFeatures", RepeatTestFilter.getMostRecentRepeatAction());

        server.addInstalledAppForValidation(JwtFatConstants.APP_FORMLOGIN);
        serverTracker.addServer(server);
        server.startServerUsingExpandedConfiguration("server_withFeature.xml", CommonWaitForAppChecks.getLTPAReadyMsgs(CommonWaitForAppChecks.getSSLChannelReadyMsgs()));

    }

    /**
     * Tests:
     * - Log into a protected resource with the jwtSso element pointing to a jwtBuilder that RS384 configured as the signature algorithm
     * Expects:
     * - Should successfully reach protected resource
     * - JWT cookie header should include a "kid" entry and alg set to RS384
     */
    @Test
    public void SigAlgTests_signWithRS384() throws Exception {
        // reconfig server to add RS384 (make sure that SSL channel has restarted)
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_signRS384.xml", MessageConstants.CWWKO0219I_SSL_CHANNEL_READY);

        WebClient webClient = actions.createWebClient();

        String builderId = "sigAlgRS384Builder";

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;

        String expectedIssuer = "https://[^/]+/jwt/" + builderId;
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, defaultUser, expectedIssuer));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.jwtCookieExists(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME, JwtFatConstants.NOT_SECURE,
                                                                        JwtFatConstants.HTTPONLY));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);

        Cookie jwtCookie = webClient.getCookieManager().getCookie(JwtFatConstants.JWT_COOKIE_NAME);
        verifyJwtHeaderContainsKey(jwtCookie.getValue(), "kid");
        verifyJwtHeaderContainsKeyAndValue(jwtCookie.getValue(), "alg", JwtFatConstants.SIGALG_RS384);
        actions.destroyWebClient(webClient);

    }

    /**
     * Tests:
     * - Log into a protected resource with the jwtSso element pointing to a jwtBuilder that ES512 configured as the signature algorithm
     * Expects:
     * - Should successfully reach protected resource
     * - JWT cookie header should include a "kid" entry and alg set to ES512
     */
    @Mode(TestMode.LITE)
    @Test
    public void SigAlgTests_signWithES512() throws Exception {
        // reconfig server to add RS384 (make sure that SSL channel has restarted)
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_signES512.xml", MessageConstants.CWWKO0219I_SSL_CHANNEL_READY);

        WebClient webClient = actions.createWebClient();

        String builderId = "sigAlgES512Builder";

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;

        String expectedIssuer = "https://[^/]+/jwt/" + builderId;
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, defaultUser, expectedIssuer));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.jwtCookieExists(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME, JwtFatConstants.NOT_SECURE,
                                                                        JwtFatConstants.HTTPONLY));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);

        Cookie jwtCookie = webClient.getCookieManager().getCookie(JwtFatConstants.JWT_COOKIE_NAME);
        verifyJwtHeaderContainsKey(jwtCookie.getValue(), "kid");
        verifyJwtHeaderContainsKeyAndValue(jwtCookie.getValue(), "alg", JwtFatConstants.SIGALG_ES512);
        actions.destroyWebClient(webClient);

    }
}
