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
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
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
public class ReplayCookieTests extends CommonJwtFat {

    protected static Class<?> thisClass = ReplayCookieTests.class;

    @Server("com.ibm.ws.security.jwtsso.fat")
    public static LibertyServer server;

    private JwtFatActions actions = new JwtFatActions();
    private TestValidationUtils validationUtils = new TestValidationUtils();

    String httpUrlBase = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort();
    String protectedUrl = httpUrlBase + JwtFatConstants.SIMPLE_SERVLET_PATH;
    String defaultUser = JwtFatConstants.TESTUSER;
    String defaultPassword = JwtFatConstants.TESTUSERPWD;

    @BeforeClass
    public static void setUp() throws Exception {
        setUpAndStartServer(server, JwtFatConstants.COMMON_CONFIG_DIR + "/server_withFeature.xml");
    }

    /**
     * Tests:
     * - Logs into the protected resource with the JWT SSO feature configured
     * - Re-access the protected resource using the same web conversation (which will include the JWT SSO cookie)
     * Expects:
     * - Upon re-access, should reach the resource without having to log in
     */
    @Test
    public void test_reaccessResource_useSameWebConversation_includeJwtCookie() throws Exception {
        server.reconfigureServer(JwtFatConstants.COMMON_CONFIG_DIR + "/server_withFeature.xml");

        String user = JwtFatConstants.USER_1;
        String password = JwtFatConstants.USER_1_PWD;

        WebClient webClient = new WebClient();
        actions.logInAndObtainJwtCookie(testName.getMethodName(), webClient, protectedUrl, user, password);

        // Access the protected resource again using the same web conversation
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, user));
        expectations.addExpectations(CommonExpectations.jwtCookieExists(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.cookieDoesNotExist(currentAction, webClient, JwtFatConstants.LTPA_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));

        Page response = actions.invokeUrl(testName.getMethodName(), webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Logs into the protected resource with the JWT SSO feature configured
     * - Delete the JWT SSO cookie from the web conversation
     * - Re-access the protected resource using the same web conversation
     * Expects:
     * - Upon re-access, should receive the login page because a cookie is not present
     */
    @Test
    public void test_reaccessResource_useSameWebConversation_deleteJwtCookie() throws Exception {
        server.reconfigureServer(JwtFatConstants.COMMON_CONFIG_DIR + "/server_withFeature.xml");

        WebClient webClient = new WebClient();
        actions.logInAndObtainJwtCookie(testName.getMethodName(), webClient, protectedUrl, defaultUser, defaultPassword);

        webClient.getCookieManager().clearCookies();

        // Access the protected resource again using the same web conversation
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(testName.getMethodName(), webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Logs into the protected resource with the JWT SSO feature configured
     * - Re-access the protected resource in a new web conversation without the JWT SSO cookie
     * Expects:
     * - Upon re-access, should receive the login page because a cookie is not present
     */
    @Test
    public void test_reaccessResource_newConversationWithoutJwtCookie() throws Exception {
        server.reconfigureServer(JwtFatConstants.COMMON_CONFIG_DIR + "/server_withFeature.xml");

        actions.logInAndObtainJwtCookie(testName.getMethodName(), protectedUrl, defaultUser, defaultPassword);

        // Access the protected resource again, but without the JWT cookie
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(testName.getMethodName(), protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Logs into the protected resource with the JWT SSO feature configured
     * - Re-access the protected resource in a new web conversation, including the JWT SSO cookie that was just obtained
     * Expects:
     * - Upon re-access, should reach the resource without having to log in
     */
    @Test
    public void test_reaccessResource_newConversationWithValidJwtCookie() throws Exception {
        server.reconfigureServer(JwtFatConstants.COMMON_CONFIG_DIR + "/server_withFeature.xml");

        Cookie jwtCookie = actions.logInAndObtainJwtCookie(testName.getMethodName(), protectedUrl, defaultUser, defaultPassword);

        // Access the protected again using a new conversation with the JWT SSO cookie included
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, defaultUser));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));

        Page response = actions.invokeUrlWithCookie(testName.getMethodName(), protectedUrl, jwtCookie);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Log into the protected resource WITHOUT the JWT SSO feature, obtaining an LTPA token/cookie
     * - Reconfigure the server to enable the JWT SSO feature
     * - fallbackToLtpa is not set, meaning we should NOT fall back to use the LTPA token
     * - Re-access the protected resource with the LTPA cookie that was just obtained
     * Expects:
     * - Upon re-access, should receive the login page because the LTPA cookie should not be used for authentication
     */
    @Test
    public void test_obtainLtpa_reconfigureToUseJwtSso_reaccessResourceWithLtpaCookie() throws Exception {
        server.reconfigureServer(JwtFatConstants.COMMON_CONFIG_DIR + "/server_noFeature.xml");

        Cookie ltpaCookie = actions.logInAndObtainLtpaCookie(testName.getMethodName(), new WebClient(), protectedUrl, defaultUser, defaultPassword);

        server.reconfigureServer(JwtFatConstants.COMMON_CONFIG_DIR + "/server_withFeature.xml");

        // Access the protected again using a new conversation with the LTPA cookie included
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrlWithCookie(testName.getMethodName(), protectedUrl, ltpaCookie);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Log into the protected resource WITHOUT the JWT SSO feature, obtaining an LTPA token/cookie
     * - Reconfigure the server to enable the JWT SSO feature
     * - useLtpaIfJwtAbsent is set to true
     * - Re-access the protected resource with the LTPA cookie that was just obtained
     * Expects:
     * - Upon re-access, should reach the resource without having to log in
     * - User principal (as obtained from HttpServletRequest.getUserPrincipal()) should now be a JWT, not a WSPrincipal
     * - Subject should contain both the original WSPrincipal and a new JWT principal
     */
    @Test
    public void test_obtainLtpa_reconfigureToUseJwtSso_reaccessResourceWithLtpaCookie_fallbackToLtpa() throws Exception {
        server.reconfigureServer(JwtFatConstants.COMMON_CONFIG_DIR + "/server_noFeature.xml");

        Cookie ltpaCookie = actions.logInAndObtainLtpaCookie(testName.getMethodName(), new WebClient(), protectedUrl, defaultUser, defaultPassword);

        server.reconfigureServer(JwtFatConstants.COMMON_CONFIG_DIR + "/server_useLtpaIfJwtAbsent.xml");

        // Access the protected again using a new conversation with the LTPA cookie included
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(currentAction, protectedUrl));
        expectations.addExpectations(CommonExpectations.responseTextIncludesCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.JWT_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.responseTextIncludesExpectedRemoteUser(currentAction, defaultUser));
        expectations.addExpectations(CommonExpectations.responseTextIncludesJwtPrincipal(currentAction));
        expectations.addExpectations(CommonExpectations.responseTextIncludesExpectedAccessId(currentAction, JwtFatConstants.BASIC_REALM, defaultUser));
        expectations.addExpectations(CommonExpectations.getJwtPrincipalExpectations(currentAction, defaultUser, JwtFatConstants.DEFAULT_ISS_REGEX));

        Page response = actions.invokeUrlWithCookie(testName.getMethodName(), protectedUrl, ltpaCookie);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - Log into the protected resource with the JWT SSO feature, obtaining an JWT SSO cookie
     * - Reconfigure the server to disable the JWT SSO feature
     * - Re-access the protected resource with the JWT cookie that was just obtained
     * Expects:
     * - Upon re-access, should receive the login page
     */
    @Test
    public void test_obtainJwt_reconfigureToDisableJwtSso_reaccessResourceWithJwtCookie() throws Exception {
        server.reconfigureServer(JwtFatConstants.COMMON_CONFIG_DIR + "/server_withFeature.xml");

        Cookie jwtCookie = actions.logInAndObtainJwtCookie(testName.getMethodName(), protectedUrl, defaultUser, defaultPassword);

        server.reconfigureServer(JwtFatConstants.COMMON_CONFIG_DIR + "/server_noFeature.xml");

        // Access the protected again using a new conversation with the JWT SSO cookie included
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrlWithCookie(testName.getMethodName(), protectedUrl, jwtCookie);
        validationUtils.validateResult(response, currentAction, expectations);
    }

}
