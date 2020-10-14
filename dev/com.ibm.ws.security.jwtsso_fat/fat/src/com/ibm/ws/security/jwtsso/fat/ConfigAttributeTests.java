/*******************************************************************************
 * Copyright (c) 2018, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.security.jwtsso.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.gargoylesoftware.htmlunit.util.NameValuePair;
import com.ibm.ws.security.fat.common.CommonSecurityFat;
import com.ibm.ws.security.fat.common.actions.TestActions;
import com.ibm.ws.security.fat.common.apps.jwtbuilder.JwtBuilderServlet;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.fat.common.expectations.ResponseTitleExpectation;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.CommonWaitForAppChecks;
import com.ibm.ws.security.fat.common.validation.TestValidationUtils;
import com.ibm.ws.security.jwtsso.fat.actions.JwtFatActions;
import com.ibm.ws.security.jwtsso.fat.actions.RunWithMpJwtVersion;
import com.ibm.ws.security.jwtsso.fat.utils.CommonExpectations;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatConstants;
import com.ibm.ws.security.jwtsso.fat.utils.JwtFatUtils;
import com.ibm.ws.security.jwtsso.fat.utils.MessageConstants;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.custom.junit.runner.RepeatTestFilter;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class ConfigAttributeTests extends CommonSecurityFat {

    protected static Class<?> thisClass = ConfigAttributeTests.class;

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
        webClient.getOptions().setThrowExceptionOnFailingStatusCode(false);
    }

    /**
     * Test the config attributes cookieName and includeLtpaCookie.
     * Invoke app on the happy path, check that jwt cookie with correct name came back,
     * and that ltpa cookie came back.
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_cookieName_includeLtpa() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_testcookiename.xml");

        String cookieName = "easyrider";

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(currentAction, protectedUrl));
        expectations.addExpectations(CommonExpectations.jwtCookieExists(currentAction, webClient, cookieName));
        expectations.addExpectations(CommonExpectations.ltpaCookieExists(currentAction, webClient));
        expectations.addExpectations(CommonExpectations.getResponseTextExpectationsForJwtCookie(currentAction, cookieName, defaultUser));
        expectations.addExpectations(CommonExpectations.getJwtPrincipalExpectations(currentAction, defaultUser, JwtFatConstants.DEFAULT_ISS_REGEX));
        expectations.addExpectations(CommonExpectations.responseTextIncludesCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests Config:
     * - <jwtSso disableJwtCookie="true" includeLtpaCookie="true" useLtpaIfJwtAbsent="true" setCookieSecureFlag="false"/>
     * Expects:
     * - JWT cookie is not found in the response, but the ltpa cookie is found.
     */
    @Test
    public void test_disableJwtCookie_true_includeLtpaCookie_true() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_disableJwtCookie_true_includeLtpa_true.xml");

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        Expectations expectations = new Expectations();

        currentAction = disableJwtCookie_test_base(currentAction, response, expectations);

        expectations.addExpectations(CommonExpectations.cookieDoesNotExist(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.ltpaCookieExists(currentAction, webClient));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * This helper method sets the action and expectations common across disableJwtCookie tests
     */
    private String disableJwtCookie_test_base(String currentAction, Page response, Expectations expectations) throws Exception {
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;

        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(currentAction, protectedUrl));
        expectations.addExpectations(CommonExpectations.getJwtPrincipalExpectations(currentAction, defaultUser, JwtFatConstants.DEFAULT_ISS_REGEX));

        return currentAction;
    }

    /**
     * Tests Config:
     * - <jwtSso disableJwtCookie="true" includeLtpaCookie="true" useLtpaIfJwtAbsent="true" cookieName="AdamsJwtCookie" setCookieSecureFlag="false"/>
     * Expects:
     * - JWT cookie is not found in the response, but the ltpa cookie is found.
     */
    @Test
    public void test_disableJwtCookie_true_includeLtpaCookie_true_differentJwtCookieName() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_disableJwtCookie_true_differentJwtCookieName_includeLtpa_true.xml");

        String jwtCookieName = "AdamsJwtCookie";

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        Expectations expectations = new Expectations();

        currentAction = disableJwtCookie_test_base(currentAction, response, expectations);

        expectations.addExpectations(CommonExpectations.cookieDoesNotExist(currentAction, webClient, jwtCookieName));
        expectations.addExpectations(CommonExpectations.cookieDoesNotExist(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.ltpaCookieExists(currentAction, webClient));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests Config:
     * - <jwtSso disableJwtCookie="true" includeLtpaCookie="true" useLtpaIfJwtAbsent="false" setCookieSecureFlag="false"/>
     * Expects:
     * - JWT cookie is not found in the response, but the ltpa cookie is found,
     * - the client is still on the login page because ltpa cookie is not used
     */
    @Test
    public void test_disableJwtCookie_true_includeLtpaCookie_true_useLtpaIfJwtAbsent_false() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_disableJwtCookie_true_includeLtpa_true_useLtpaIfJwtAbsent_false.xml");

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;

        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));
        expectations.addExpectations(CommonExpectations.cookieDoesNotExist(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.ltpaCookieExists(currentAction, webClient));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests Config:
     * - <jwtSso disableJwtCookie="true" includeLtpaCookie="false" setCookieSecureFlag="false"/>
     * Expects:
     * - neither jwt nor ltpa cookie is in the response, and the client is still on the login page
     */
    @Test
    public void test_disableJwtCookie_true_includeLtpaCookie_false() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_disableJwtCookie_true_includeLtpa_false.xml");

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;

        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));
        expectations.addExpectations(CommonExpectations.cookieDoesNotExist(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.cookieDoesNotExist(currentAction, webClient, JwtFatConstants.LTPA_COOKIE_NAME));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests Config:
     * - <jwtSso disableJwtCookie="false" includeLtpaCookie="true" useLtpaIfJwtAbsent="true" setCookieSecureFlag="false"/>
     * Expects:
     * - JWT cookie and ltpa cookie are both found in the response
     */
    @Test
    public void test_disableJwtCookie_false_includeLtpaCookie_true() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_disableJwtCookie_false_includeLtpa_true.xml");

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        Expectations expectations = new Expectations();

        currentAction = disableJwtCookie_test_base(currentAction, response, expectations);

        expectations.addExpectations(CommonExpectations.jwtCookieExists(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.ltpaCookieExists(currentAction, webClient));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests Config:
     * - <jwtSso disableJwtCookie="false" includeLtpaCookie="false" setCookieSecureFlag="false"/>
     * Expects:
     * - JWT cookie is found in the response and ltpa cookie is not
     */
    @Test
    public void test_disableJwtCookie_false_includeLtpaCookie_false() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_disableJwtCookie_false_includeLtpa_false.xml");

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        Expectations expectations = new Expectations();

        currentAction = disableJwtCookie_test_base(currentAction, response, expectations);

        expectations.addExpectations(CommonExpectations.jwtCookieExists(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.cookieDoesNotExist(currentAction, webClient, JwtFatConstants.LTPA_COOKIE_NAME));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests Config:
     * - <jwtSso disableJwtCookie="false" includeLtpaCookie="false" setCookieSecureFlag="false"/>
     * Expects:
     * - JWT cookie is found in the response and ltpa cookie is not
     */
    @Test
    public void test_disableJwtCookie_false_includeBadLtpaCookie() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_disableJwtCookie_false_includeLtpa_false.xml");

        Expectations expectations = new Expectations();

        WebClient webClient = new WebClient();
        Cookie jwtCookie = actions.logInAndObtainJwtCookie(_testName, webClient, protectedUrl, defaultUser, defaultPassword);
        Cookie badLtpaCookie = new Cookie("", JwtFatConstants.LTPA_COOKIE_NAME, "some bad value");

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Page response = actions.invokeUrlWithCookies(_testName, protectedUrl, jwtCookie, badLtpaCookie);

        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(currentAction, protectedUrl));
        expectations.addExpectations(CommonExpectations.jwtCookieExists(currentAction, webClient, JwtFatConstants.JWT_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.cookieDoesNotExist(currentAction, webClient, JwtFatConstants.LTPA_COOKIE_NAME));

        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - cookieName: Empty string
     * Expects:
     * - A CWWKS6302E message should be logged saying the specified cookie name cannot be null or empty
     * - The default JWT SSO cookie name should be used
     * - Should successfully reach the protected resource
     */
    @Test
    public void test_cookieName_empty() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_cookieNameEmpty.xml");

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, defaultUser));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS6302E_COOKIE_NAME_CANT_BE_EMPTY));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - cookieName: Includes whitespace
     * Expects:
     * - A CWWKS6303E message should be logged saying the specified cookie name is not valid
     * - The default JWT SSO cookie name should be used
     * - Should successfully reach the protected resource
     */
    @Test
    public void test_cookieName_includesWhitespace() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_cookieNameIncludesWhitespace.xml");

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, defaultUser));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS6303E_COOKIE_NAME_INVALID));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - cookieName: Includes invalid cookie characters (e.g. ";", "=")
     * Expects:
     * - A CWWKS6303E message should be logged saying the specified cookie name is not valid
     * - The default JWT SSO cookie name should be used
     * - Should successfully reach the protected resource
     */
    @Test
    public void test_cookieName_invalidCookieCharacters() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_cookieNameInvalidCharacters.xml");

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, defaultUser));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS6303E_COOKIE_NAME_INVALID));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - cookieName: Includes invalid unicode characters
     * Expects:
     * - A CWWKS6303E message should be logged saying the specified cookie name is not valid
     * - The default JWT SSO cookie name should be used
     * - Should successfully reach the protected resource
     */
    @Test
    public void test_cookieName_unicodeInvalid() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_cookieNameInvalidUnicodeCharacters.xml");

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, defaultUser));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS6303E_COOKIE_NAME_INVALID));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - cookieName: Includes only valid unicode characters
     * Expects:
     * - Should successfully reach the protected resource
     * - JWT cookie with the updated name should be present in the response
     */
    @Test
    public void test_cookieName_unicodeValid() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_cookieNameValidUnicodeCharacters.xml");

        String cookieName = "MyCookie";

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(currentAction, protectedUrl));
        expectations.addExpectations(CommonExpectations.getResponseTextExpectationsForJwtCookie(currentAction, cookieName, defaultUser));
        expectations.addExpectations(CommonExpectations.getJwtPrincipalExpectations(currentAction, defaultUser, JwtFatConstants.DEFAULT_ISS_REGEX));
        expectations.addExpectations(CommonExpectations.jwtCookieExists(currentAction, webClient, cookieName));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - cookieName: Exceptionally long string
     * Expects:
     * - Should successfully reach the protected resource
     * - JWT cookie with the updated name should be present in the response
     * - Cookie should NOT be broken into multiple cookies, despite its size
     */
    @Test
    public void test_cookieName_extremelyLong() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_cookieNameExtremelyLong.xml");

        String cookieName = "ExtremelyLongCookieNamexxxxxxxx10xxxxxxxx20";

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(currentAction, protectedUrl));
        expectations.addExpectations(CommonExpectations.getResponseTextExpectationsForJwtCookie(currentAction, cookieName, defaultUser));
        expectations.addExpectations(CommonExpectations.getJwtPrincipalExpectations(currentAction, defaultUser, JwtFatConstants.DEFAULT_ISS_REGEX));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);

        // Ensure that the cookie was NOT broken into multiple cookies due to size
        Set<Cookie> cookies = webClient.getCookieManager().getCookies();
        int relatedCookieCount = 0;
        for (Cookie cookie : cookies) {
            if (cookie.getName().startsWith(cookieName)) {
                relatedCookieCount++;
            }
        }
        assertEquals("Did not find exactly one cookie that started with expected string [" + cookieName + "]. Cookies were: " + cookies, 1, relatedCookieCount);
    }

    /**
     * Test the jwtBuilderRef attribute. Specify a nonexistent builderRef,
     * login should fail and we should get a meaningful error message in the log.
     * The ltpa cookie is included, but useLtpaIfJwtAbsent is false, so fallback should not occur.
     */
    @Mode(TestMode.LITE)
    @Test
    @ExpectedFFDC({ "com.ibm.websphere.security.jwt.InvalidBuilderException" })
    public void test_invalidBuilderRef_useLtpaIfJwtAbsentFalse() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_testbadbuilder.xml");

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, protectedUrl); // get back the login page
        validationUtils.validateResult(response, currentAction, expectations);

        // things should have bombed and we should be back at the login page
        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;
        expectations.addExpectation(new ResponseTitleExpectation(currentAction, JwtFatConstants.STRING_CONTAINS, "A Form login authentication failure occurred", "Did not find the expected title for a failed form login."));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS6008E_JWT_BUILDER_INVALID));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Test the jwtBuilderRef attribute. Specify an existing and valid jwtBuilderRef.
     * Authentication will fail because the issuer mismatches the consumer, but we should
     * see evidence in the logs that the customized issuer was presented.
     * That's all we care about.
     */
    @AllowedFFDC({ "com.ibm.websphere.security.jwt.InvalidClaimException",
                   "com.ibm.websphere.security.jwt.InvalidTokenException",
                   "com.ibm.ws.security.authentication.AuthenticationException" })
    @Mode(TestMode.LITE)
    @Test
    public void test_validBuilderRef() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_testgoodbuilder.xml");

        String issuer = "https://flintstone:19443/jwt/defaultJWT";

        Page response = actions.invokeUrl(_testName, protectedUrl); // get back the login page

        // now confirm we got the login page
        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS6022E_JWT_ISSUER_NOT_TRUSTED + ".+" + Pattern.quote(issuer)));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS6031E_JWT_ERROR_PROCESSING_JWT));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS5524E_ERROR_CREATING_JWT));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS5523E_ERROR_CREATING_JWT_USING_TOKEN_IN_REQ));

        // log in, which should drive building a token.
        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Test the jwtConsumerRef attribute. Specify an existing and valid jwtConsumerRef and jwtBuilderRef.
     * The issuer in the builder and consumer match, and are non-default.
     * A separate test checks that the non-default builder is used.
     * When both are used together, if we can authenticate to the app and then re-access the app,
     * the second access will cause the token to be checked by the consumer.
     * If the jwtConsumerRef is in use as it should be, then the second access will succeed.
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_validConsumerRef() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_testgoodconsumer.xml");

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(TestActions.ACTION_INVOKE_PROTECTED_RESOURCE));

        WebClient wc = new WebClient();
        Page response = actions.invokeUrl(_testName, wc, protectedUrl); // get back the login page
        validationUtils.validateResult(response, TestActions.ACTION_INVOKE_PROTECTED_RESOURCE, expectations);

        response = actions.doFormLogin(response, defaultUser, defaultPassword);

        String responseStr = response.getWebResponse().getContentAsString();
        boolean check = responseStr.contains("\"iss\":\"https://flintstone:19443/jwt/defaultJWT\"");
        assertTrue("Issuer in token did not match the one configured in the builder", check);

        // now access resource a second time, force token to be examined by consumer
        response = actions.invokeUrl(_testName, wc, protectedUrl);
        responseStr = response.getWebResponse().getContentAsString();
        boolean check2 = responseStr.contains("SimpleServlet");
        assertTrue("Did not access protected resource with custom consumer", check2);
    }

    /**
     * Test the detection of the mpJwt server config element. Specify an extra element and try to authenticate.
     * We should get an error message about the extra element.
     */
    @ExpectedFFDC({ "com.ibm.ws.security.mp.jwt.error.MpJwtProcessingException" })
    @AllowedFFDC({ "com.ibm.ws.security.authentication.AuthenticationException" })
    @Mode(TestMode.LITE)
    @Test
    public void test_invalidConsumerRef() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_testbadconsumer.xml");

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;
        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS5521E_MANY_JWT_CONSUMER_CONFIGS));

        Page response = actions.invokeUrl(_testName, protectedUrl); // get back the login page
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS6301E_MANY_JWT_CONSUMER_CONFIGS));

        response = actions.doFormLogin(response, defaultUser, defaultPassword); // should fail and we should get login page again
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - useLtpaIfJwtAbsent: true
     * - Obtain a JWT cookie for one user
     * - Obtain a separate LTPA cookie for a different user
     * - Wait until the JWT cookie has expired
     * - Invoke the protected resource with both cookies included
     * Expects:
     * - Should fail to validate the JWT cookie, but should successfully fall back to use the included LTPA token and access the protected resource
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_useLtpaIfJwtAbsent_true() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_noFeature.xml", MessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*formlogin");

        // Obtain a valid LTPA token
        Cookie ltpaCookie = actions.logInAndObtainLtpaCookie(_testName, protectedUrl, defaultUser, defaultPassword);

        // Enable useLtpaIfJwtAbsent
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_useLtpaIfJwtAbsent_true.xml", MessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*formlogin");

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedUrl(currentAction, protectedUrl));
        expectations.addExpectations(CommonExpectations.responseTextIncludesCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));
        expectations.addExpectations(CommonExpectations.responseTextIncludesExpectedRemoteUser(currentAction, defaultUser));
        // HttpServletRequest.getUserPrincipal() should return a JWT principal (not a WSPrincipal). This verifies that the JWT SSO feature was, in fact, utilized
        expectations.addExpectations(CommonExpectations.responseTextIncludesJwtPrincipal(currentAction));
        // Subject principals should contain JWT principal AND WSPrincipal
        expectations.addExpectation(new ResponseFullExpectation(currentAction, JwtFatConstants.STRING_MATCHES, "Principal: \\{.+\\}", "Should have found a JWT in the subject principals, but did not."));
        expectations.addExpectation(Expectation.createResponseExpectation(currentAction, "Principal: WSPrincipal:" + defaultUser,
                                                                          "Should have found a WSPrincipal in the subject principals, but did not."));
        expectations.addExpectations(CommonExpectations.responseTextIncludesExpectedAccessId(currentAction, JwtFatConstants.BASIC_REALM, defaultUser));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.JWT_COOKIE_NAME));

        Page response = actions.invokeUrlWithCookie(_testName, protectedUrl, ltpaCookie);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - useLtpaIfJwtAbsent: false
     * - Obtain a JWT cookie for one user
     * - Obtain a separate LTPA cookie for a different user
     * - Wait until the JWT cookie has expired
     * - Invoke the protected resource with both cookies included
     * Expects:
     * - Should fail to validate the JWT cookie and be redirected to the login page
     * - LTPA token should not be looked at by the runtime
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_useLtpaIfJwtAbsent_false() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_noFeature.xml", MessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*formlogin");

        // Obtain a valid LTPA token
        Cookie ltpaCookie = actions.logInAndObtainLtpaCookie(_testName, protectedUrl, defaultUser, defaultPassword);

        // Disable useLtpaIfJwtAbsent
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_useLtpaIfJwtAbsent_false.xml", MessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*formlogin");

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrlWithCookie(_testName, protectedUrl, ltpaCookie);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Test the setCookieSecureFlag attribute. Use the default setting (true),
     * and inspect cookie to see that it happened.
     * Expects:
     * - The JWT SSO cookie should be successfully created with the Secure flag
     * - Nonetheless, should be redirected to the login page because the request uses HTTP and therefore won't include the JWT SSO cookie
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_cookieSecureTrue_httpOnlyTrue() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_testcookiesecure.xml");

        WebClient wc = new WebClient();

        Page response = actions.invokeUrl(_testName, wc, protectedUrl); // get back the login page

        wc.getOptions().setThrowExceptionOnFailingStatusCode(false);

        String currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));
        // check for warning that secure cookie is being set on http
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS9127W_JWT_COOKIE_SECURITY_MISMATCH));
        expectations.addExpectations(CommonExpectations.jwtCookieExists(currentAction, wc, JwtFatConstants.JWT_COOKIE_NAME, JwtFatConstants.SECURE, JwtFatConstants.HTTPONLY));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Test that the jwtsso cookie respects the webAppSecurity httpOnlyCookies attribute setting.
     * Set webAppSecurity httpOnlyCookies="false" and inspect the cookie.
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_cookieSecureTrue_httpOnlyFalse() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_testcookiesecure_httponlyfalse.xml");

        WebClient wc = new WebClient();

        Page response = actions.invokeUrl(_testName, wc, protectedUrl); // get back the login page

        // now disable redirect so we can see the cookies in the 302
        wc.getOptions().setRedirectEnabled(false);
        wc.getOptions().setThrowExceptionOnFailingStatusCode(false);

        String currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;
        Expectations expectations = new Expectations();
        expectations.addExpectation(new ServerMessageExpectation(currentAction, server, MessageConstants.CWWKS9127W_JWT_COOKIE_SECURITY_MISMATCH));
        expectations.addExpectations(CommonExpectations.jwtCookieExists(currentAction, wc, JwtFatConstants.JWT_COOKIE_NAME, JwtFatConstants.SECURE, JwtFatConstants.NOT_HTTPONLY));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Tests:
     * - An SSL port is not opened by the server configuration
     * - Access the protected resource over HTTP
     * Expects:
     * - Issuer in the JWT SSO token should use the HTTP scheme, not HTTPS
     */
    @Test
    public void test_sslPortNotDefined() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "/server_noSslPort.xml");

        String currentAction = TestActions.ACTION_INVOKE_PROTECTED_RESOURCE;

        Expectations expectations = new Expectations();
        expectations.addExpectations(CommonExpectations.successfullyReachedLoginPage(currentAction));

        Page response = actions.invokeUrl(_testName, webClient, protectedUrl);
        validationUtils.validateResult(response, currentAction, expectations);

        currentAction = TestActions.ACTION_SUBMIT_LOGIN_CREDENTIALS;

        // Ensure that the issuer value does NOT use the HTTPS scheme
        String issuerRegex = "http://[^/]+/jwt/defaultJwtSso";
        expectations.addExpectations(CommonExpectations.successfullyReachedProtectedResourceWithJwtCookie(currentAction, protectedUrl, defaultUser, issuerRegex));
        expectations.addExpectations(CommonExpectations.responseTextMissingCookie(currentAction, JwtFatConstants.LTPA_COOKIE_NAME));

        response = actions.doFormLogin(response, defaultUser, defaultPassword);
        validationUtils.validateResult(response, currentAction, expectations);
    }

    /**
     * Test that the amr security attribute specified in the config is included in jwtToken. Uses the amrbuilder web app to
     * add the specified attribute to the subject and built a new token. Token is then decoded and sent back as response and
     * inspect the response to check the amrValue.
     */
    @Mode(TestMode.LITE)
    @Test
    public void test_amrValue() throws Exception {
        server.reconfigureServerUsingExpandedConfiguration(_testName, "server_amrValues.xml", MessageConstants.CWWKT0016I_WEB_APP_AVAILABLE + ".*amrbuilder");

        WebClient webClient = new WebClient();
        Cookie cookie = actions.logInAndObtainJwtCookie(_testName, webClient, protectedUrl, defaultUser, defaultPassword, "https?://" + "[^/]+/jwt/" + "amrBuilder");

        Page response = buildNewJwtAfterAddingSecurityAttribute(cookie);
        String responseStr = response.getWebResponse().getContentAsString();
        boolean check = responseStr.contains("\"amr\":[\"amrValue\"]");
        assertTrue("AMR in token did not match the one configured in the builder", check);

    }

    /**
     * Invokes the amrbuilder web application configured in the server with cookie to build JWT to check the amrValue is
     * included. A JWT is built using the amrBuilder configuration with the defaultJwtSso and the jwt is printed
     * in the response.
     */
    private Page buildNewJwtAfterAddingSecurityAttribute(Cookie cookie) throws Exception {
        String jwtBuilderUrl = "http://" + server.getHostname() + ":" + server.getHttpDefaultPort() + "/amrbuilder/AmrServlet";

        List<NameValuePair> requestParams = new ArrayList<NameValuePair>();
        requestParams.add(new NameValuePair(JwtBuilderServlet.PARAM_BUILDER_ID, "amrBuilder"));

        Map<String, String> requestHeaders = new HashMap<>();
        requestHeaders.put("Cookie", cookie.getName() + "=" + cookie.getValue());

        WebClient webClient = new WebClient();
        Page response = actions.invokeUrlWithParametersAndHeaders(_testName, webClient, jwtBuilderUrl, requestParams, requestHeaders);

        return response;
    }

}
