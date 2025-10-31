/*******************************************************************************
 * Copyright (c) 2023 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.security.jakartasec.fat.tests;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.Page;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ServerMessageExpectation;
import com.ibm.ws.security.fat.common.utils.SecurityFatHttpUtils;
import com.ibm.ws.security.fat.common.web.WebResponseUtils;

import componenttest.annotation.Server;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.RepeatTests;
import componenttest.topology.impl.LibertyServer;
import io.openliberty.security.jakartasec.fat.commonTests.CommonAnnotatedSecurityTests;
import io.openliberty.security.jakartasec.fat.configs.TestConfigMaps;
import io.openliberty.security.jakartasec.fat.utils.Constants;
import io.openliberty.security.jakartasec.fat.utils.MessageConstants;
import io.openliberty.security.jakartasec.fat.utils.ShrinkWrapHelpers;

/**
 * This class adds tests to validate that the correct errors are logged and that the proper
 * responses are returned when the callback from the authentication endpoint fails the
 * checks described in step 3 of the Jakarta Security 3.0 spec.
 *
 * https://jakarta.ee/specifications/security/3.0/jakarta-security-spec-3.0.html#authentication-dialog
 */
/**
 * Tests appSecurity-5.0
 */
@SuppressWarnings("restriction")
@RunWith(FATRunner.class)
public class AuthenticationEndpointValidationTests extends CommonAnnotatedSecurityTests {

    protected static Class<?> thisClass = AuthenticationEndpointValidationTests.class;

    protected static ShrinkWrapHelpers swh = null;

    @Server("jakartasec-3.0_fat.op")
    public static LibertyServer opServer;
    @Server("jakartasec-3.0_fat.authenticationEndpointValidation.jwt.rp")
    public static LibertyServer rpJwtServer;
    @Server("jakartasec-3.0_fat.authenticationEndpointValidation.opaque.rp")
    public static LibertyServer rpOpaqueServer;

    public static LibertyServer rpServer;

    // create repeats for opaque and jwt tokens - in lite mode, only run with opaque tokens
    @ClassRule
    public static RepeatTests repeat = createTokenTypeRepeats(TestMode.LITE, Constants.OPAQUE_TOKEN_FORMAT);

    private static final String STATE_COOKIE_PREFIX = "WASOidcState";

    @BeforeClass
    public static void setUp() throws Exception {

        // write property that is used to configure the OP to generate JWT or Opaque tokens
        rpServer = setTokenTypeInBootstrap(opServer, rpJwtServer, rpOpaqueServer);

        // Add servers to server trackers that will be used to clean servers up and prevent servers
        // from being restored at the end of each test (so far, the tests are not reconfiguring the servers)
        updateTrackers(opServer, rpServer, false);

        List<String> waitForMsgs = null;
        opServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(opServer, Constants.BVT_SERVER_1_PORT_NAME_ROOT);
        opHttpBase = "http://localhost:" + opServer.getBvtPort();
        opHttpsBase = "https://localhost:" + opServer.getBvtSecurePort();

        rpServer.startServerUsingExpandedConfiguration("server_orig.xml", waitForMsgs);
        SecurityFatHttpUtils.saveServerPorts(rpServer, Constants.BVT_SERVER_2_PORT_NAME_ROOT);

        rpHttpBase = "http://localhost:" + rpServer.getBvtPort();
        rpHttpsBase = "https://localhost:" + rpServer.getBvtSecurePort();

        deployMyApps(); // run this after starting the RP so we have the rp port to update the openIdConfig.properties file within the apps

        // Wait to ensure LTPA configuration has completed before starting tests.
        opServer.waitForStringInLog("CWWKS4105I");
        rpServer.waitForStringInLog("CWWKS4105I");

    }

    /**
     * Deploy the apps that this test class uses
     *
     * @throws Exception
     */
    public static void deployMyApps() throws Exception {

        swh = new ShrinkWrapHelpers(opHttpBase, opHttpsBase, rpHttpBase, rpHttpsBase);

        swh.defaultDropinApp(rpServer, "SimplestAnnotatedWithEL.war", "oidc.client.withEL.servlets", "oidc.client.base.*");

        swh.deployConfigurableTestApps(rpServer, "SimplestAnnotatedWithCookies.war", "SimplestAnnotatedWithEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "SimplestAnnotatedWithCookies", null,
                                                             TestConfigMaps.getUseSessionExpressionFalse()),
                                       "oidc.client.withEL.servlets", "oidc.client.base.*");
        swh.deployConfigurableTestApps(rpServer, "SimplestAnnotatedWithRedirectToOriginalResource.war", "SimplestAnnotatedWithEL.war",
                                       buildUpdatedConfigMap(opServer, rpServer, "SimplestAnnotatedWithRedirectToOriginalResource", null,
                                                             TestConfigMaps.getRedirectToOriginalResourceExpressionTrue()),
                                       "oidc.client.withEL.servlets", "oidc.client.base.*");

    }

    /**
     * Invokes the target url, logs in, and stops before invoking the callback endpoint.
     * The callback endpoint url to be invoked, including its query params, are returned.
     *
     * @param webClient
     * @param url
     * @return
     * @throws Exception
     */
    private String invokeUrlAndGetToCallback(WebClient webClient, String url) throws Exception {

        Page response = actions.invokeUrl(_testName, webClient, url);

        WebClientOptions webClientOptions = webClient.getOptions();
        boolean wasRedirectEnabledOriginally = webClientOptions.isRedirectEnabled();

        webClientOptions.setRedirectEnabled(false);

        response = actions.doFormLogin(response, Constants.TESTUSER, Constants.TESTUSERPWD);
        response = actions.invokeUrl(_testName, webClient, WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION));

        webClientOptions.setRedirectEnabled(wasRedirectEnabledOriginally);

        return WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION);
    }

    /****************************************************************************************************************/
    /* Tests */
    /****************************************************************************************************************/

    /**
     * Tests with redirectToOriginalResource set to false and redirects to the unsecured servlet instead of the callback.
     * The callback check should fail, since the request url doesn't match the redirectURI.
     * The callback check should fail with a CredentialValidationResult.NOT_VALIDATED_RESULT which gets converted to
     * AuthenticationStatus.NOT_DONE and finally to AuthStatus.SUCCESS (200).
     *
     * @throws Exception
     */
    @Test
    public void AuthenticationEndpointValidationTests_request_doesNotMatchRedirectUri() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "OidcAnnotatedServletWithEL";
        String url = rpHttpsBase + "/SimplestAnnotatedWithEL/" + app;

        String callbackUrl = invokeUrlAndGetToCallback(webClient, url);
        String newCallbackUrl = callbackUrl.replaceFirst("/Callback", "/UnsecuredServlet");

        Page response = actions.invokeUrl(_testName, webClient, newCallbackUrl);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Tests with redirectToOriginalResource set to true and redirects to the unsecured servlet instead of the original resource.
     * The callback check should fail, since the request url doesn't match the redirectURI nor the stored request url.
     * The callback check should fail with a CredentialValidationResult.NOT_VALIDATED_RESULT which gets converted to
     * AuthenticationStatus.NOT_DONE and finally to AuthStatus.SUCCESS (200).
     *
     * @throws Exception
     */
    @Test
    public void AuthenticationEndpointValidationTests_request_doesNotMatchStoredRequest() throws Exception {

        WebClient webClient = getAndSaveWebClient();
        WebClientOptions webClientOptions = webClient.getOptions();

        String app = "OidcAnnotatedServletWithEL";
        String url = rpHttpsBase + "/SimplestAnnotatedWithRedirectToOriginalResource/" + app;

        String callbackUrl = invokeUrlAndGetToCallback(webClient, url);

        webClientOptions.setRedirectEnabled(false);

        Page response = actions.invokeUrl(_testName, webClient, callbackUrl);

        String redirectUrl = WebResponseUtils.getResponseHeaderField(response, Constants.RESPONSE_HEADER_LOCATION);
        String newRedirectUrl = redirectUrl.replaceFirst("/" + app, "/UnsecuredServlet");

        webClientOptions.setRedirectEnabled(true);

        response = actions.invokeUrl(_testName, webClient, newRedirectUrl);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Removes the stored state cookie.
     * The callback check should fail, since there is no stored state cookie.
     * The callback check should fail with a CredentialValidationResult.NOT_VALIDATED_RESULT which gets converted to
     * AuthenticationStatus.NOT_DONE and finally to AuthStatus.SUCCESS (200).
     *
     * @throws Exception
     */
    @Test
    public void AuthenticationEndpointValidationTests_state_notStored() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "OidcAnnotatedServletWithEL";
        String url = rpHttpsBase + "/SimplestAnnotatedWithCookies/" + app;

        String callbackUrl = invokeUrlAndGetToCallback(webClient, url);

        CookieManager cookieManager = webClient.getCookieManager();
        for (Cookie cookie : cookieManager.getCookies()) {
            if (cookie.getName().startsWith(STATE_COOKIE_PREFIX)) {
                cookieManager.removeCookie(cookie);
                break;
            }
        }

        Page response = actions.invokeUrl(_testName, webClient, callbackUrl);

        Expectations expectations = new Expectations();
        expectations.addSuccessCodeForCurrentAction();
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Modifies the stored state cookie value to something else.
     * The callback check should fail, since the stored state value will not match the query param state value.
     * The callback check should fail with a CredentialValidationResult.INVALID_RESULT which gets converted to
     * AuthenticationStatus.SEND_CONTINUE and finally to AuthStatus.SC_UNAUTHORIZED (401).
     *
     * @throws Exception
     */
    @Test
    public void AuthenticationEndpointValidationTests_state_doesNotMatch() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "OidcAnnotatedServletWithEL";
        String url = rpHttpsBase + "/SimplestAnnotatedWithCookies/" + app;

        String callbackUrl = invokeUrlAndGetToCallback(webClient, url);

        CookieManager cookieManager = webClient.getCookieManager();
        for (Cookie cookie : cookieManager.getCookies()) {
            if (cookie.getName().startsWith(STATE_COOKIE_PREFIX)) {
                Cookie modifiedCookie = new Cookie(cookie.getDomain(), cookie.getName(), "modifiedCookieValue");
                cookieManager.addCookie(modifiedCookie);
                break;
            }
        }

        Page response = actions.invokeUrl(_testName, webClient, callbackUrl);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2407E_ERROR_VERIFYING_RESPONSE, "Did not receive an error message stating that the client encountered an error verifying the authentication response."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2411E_STATE_VALUE_IN_CALLBACK_DOES_NOT_MATCH_STORED_VALUE, "Did not receive an error message stating that the state value in the callback does not match the stored state value."));
        validationUtils.validateResult(response, expectations);

    }

    /**
     * Replaces the code query param in the callback with an error query param.
     * The callback check should fail, since there is an error in the callback.
     * The callback check should fail with a CredentialValidationResult.INVALID_RESULT which gets converted to
     * AuthenticationStatus.SEND_CONTINUE and finally to AuthStatus.SC_UNAUTHORIZED (401).
     *
     * @throws Exception
     */
    @Test
    public void AuthenticationEndpointValidationTests_error() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String app = "OidcAnnotatedServletWithEL";
        String url = rpHttpsBase + "/SimplestAnnotatedWithEL/" + app;

        String callbackUrl = invokeUrlAndGetToCallback(webClient, url);
        String callbackUrlWithError = callbackUrl.replaceFirst("code=[^&]+", "error=login_required");

        Page response = actions.invokeUrl(_testName, webClient, callbackUrlWithError);

        Expectations expectations = new Expectations();
        expectations.addUnauthorizedStatusCodeAndMessageForCurrentAction();
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2407E_ERROR_VERIFYING_RESPONSE, "Did not receive an error message stating that the client encountered an error verifying the authentication response."));
        expectations.addExpectation(new ServerMessageExpectation(rpServer, MessageConstants.CWWKS2414E_CALLBACK_URL_INCLUDES_ERROR_PARAMETER, "Did not receive an error message stating that the callback url includes an error param."));
        validationUtils.validateResult(response, expectations);

    }

}
