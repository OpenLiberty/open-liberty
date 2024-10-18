/*******************************************************************************
 * Copyright (c) 2022, 2024 IBM Corporation and others.
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

package com.ibm.ws.security.openidconnect.client.fat.IBM;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.HttpMethod;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonValidationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.rules.repeater.JakartaEEAction;

@Mode(TestMode.FULL)
@RunWith(FATRunner.class)
public class OidcClientCookieVerificationTests extends CommonTest {

    public static Class<?> thisClass = OidcClientCookieVerificationTests.class;

    public static final String OP_SSO_COOKIE_NAME = "WAS_OP_SSO_cookie";
    public static final String RP_SSO_COOKIE_NAME = "WAS_RP_SSO_cookie";

    @SuppressWarnings("serial")
    @BeforeClass
    public static void setUp() throws Exception {

        List<String> apps = new ArrayList<String>() {
            {
                add(Constants.OPENID_APP);
            }
        };

        testSettings = new TestSettings();

        // Start the OIDC OP server
        testOPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.op", "op_server_cookieVerification.xml", Constants.OIDC_OP, Constants.NO_EXTRA_APPS, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS);

        //Start the OIDC RP server and setup default values
        testRPServer = commonSetUp("com.ibm.ws.security.openidconnect.client-1.0_fat.rp", "rp_server_cookieVerification.xml", Constants.OIDC_RP, apps, Constants.DO_NOT_USE_DERBY, Constants.NO_EXTRA_MSGS, Constants.OPENID_APP, Constants.IBMOIDC_TYPE);

        testSettings.setFlowType(Constants.RP_FLOW);

        testRPServer.addIgnoredServerExceptions("CWWKS1859E", "CWWKS1741W");
    }

    /**
     * Go through the golden path OIDC login flow with nonce enabled. Verify that the nonce cookie is ultimately deleted
     * correctly.
     */
    @Test
    public void test_verifyNonceCookieDeleted() throws Exception {
        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        String protectedUrl = updatedTestSettings.getTestURL().replace(Constants.DEFAULT_SERVLET, "simple/nonceEnabled");
        updatedTestSettings.setTestURL(protectedUrl);
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setNonce(Constants.EXIST_WITH_ANY_VALUE);

        List<validationData> expectations = vData.addSuccessStatusCodesForActions(Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_HEADER, Constants.STRING_DOES_NOT_CONTAIN, "Should not have seen any Set-Cookie headers in the response but did.", null, "Set-Cookie");
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_COOKIE, Constants.STRING_MATCHES, "Should have found a WASOidcState cookie but didn't.", null, "WASOidcState[pn][0-9]+=[^" + CommonValidationTools.COOKIE_DELIMITER + "]+");
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_COOKIE, Constants.STRING_MATCHES, "Should have found a WASOidcNonce cookie but didn't.", null, "WASOidcNonce[pn][0-9]+=[^" + CommonValidationTools.COOKIE_DELIMITER + "]+");
        expectations = getSuccessfulAccessExpectations(updatedTestSettings, expectations);
        String expirationCookieFormat = JakartaEEAction.isEE10OrLaterActive() ? ";(?i) max-age=0" : ";(?i) Expires=Thu, 01 Dec 1994";
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_HEADER, Constants.STRING_MATCHES, "Should have found a Set-Cookie header to clear the WASOidcState cookie but didn't.", null, "WASOidcState[pn][0-9]+=\"\"" + expirationCookieFormat);
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_HEADER, Constants.STRING_MATCHES, "Should have found a Set-Cookie header to clear the WASOidcNonce cookie but didn't.", null, "WASOidcNonce[pn][0-9]+=\"\"" + expirationCookieFormat);

        genericRP(_testName, webClient, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

        List<String> allowedCookies = Arrays.asList("JSESSIONID", "LtpaToken2", OP_SSO_COOKIE_NAME, RP_SSO_COOKIE_NAME, "WASOidcSession");

        // Verify all cookies that should have been deleted do not appear in the web client anymore
        validationTools.verifyOnlyAllowedCookiesStillPresent(webClient, allowedCookies);
    }

    /**
     * Go through the golden path OIDC login flow and then invoke the RP's /logout endpoint. That endpoint should invoke
     * request.logout() and then redirect to the OP's end_session endpoint to finish logging out at the OP. We then re-invoke the
     * protected resource using the same WebClient, and therefore with any of the cookies leftover. We should not be able to
     * access the resource again since the user logged out.
     */
    @Test
    public void test_reuseCookiesAfterLogout_rpLogoutEndpointRedirectsToOpEndSessionEndpoint() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        String endSession = updatedTestSettings.getEndSession();
        updatedTestSettings.setPostLogoutRedirect(endSession);
        updatedTestSettings.setEndSession(updatedTestSettings.getTestURL().replaceAll("/SimpleServlet", "/logout"));
        updatedTestSettings.setLogoutHttpMethod(HttpMethod.GET);

        List<validationData> expectations = vData.addExpectation(null, Constants.LOGOUT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not successfully logout.", null, Constants.SUCCESSFUL_LOGOUT_MSG);
        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not end up at expected post-logout URL.", null, "/oidc/end_session");

        logInAndLogOut(webClient, updatedTestSettings, expectations);

        // Both the OP's and RP's SSO cookies should have been removed
        validationTools.verifyOnlyAllowedCookiesStillPresent(webClient, Arrays.asList("JSESSIONID"));

        // Invoke the protected resource again with the leftover cookies
        expectations = vData.addSuccessStatusCodesForActions(Constants.GET_LOGIN_PAGE_ONLY);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the authorization endpoint redirect page.", null, "Redirect To OP");
        genericRP(_testName, webClient, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectations);
    }

    /**
     * Go through the golden path OIDC login flow and then invoke the RP's /logout endpoint. That endpoint should invoke
     * request.logout() and then redirect to the OP's /logout endpoint to finish logging out at the OP. We then re-invoke the
     * protected resource using the same WebClient, and therefore with any of the cookies leftover. We should not be able to
     * access the resource again since the user logged out.
     */
    @Test
    public void test_reuseCookiesAfterLogout_rpLogoutEndpointRedirectsToOpLogoutEndpoint() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setPostLogoutRedirect(updatedTestSettings.getAuthorizeEndpt().replaceAll("/authorize", "/logout"));
        updatedTestSettings.setEndSession(updatedTestSettings.getTestURL().replaceAll("/SimpleServlet", "/logout"));
        updatedTestSettings.setLogoutHttpMethod(HttpMethod.GET);

        List<validationData> expectations = vData.addExpectation(null, Constants.LOGOUT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not successfully logout.", null, "Logout successful");
        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_URL, Constants.STRING_MATCHES, "Did not end up at expected post-logout URL.", null, "/oidc/[^/]+/[^/]+/logout");

        logInAndLogOut(webClient, updatedTestSettings, expectations);

        // Both the OP's and RP's SSO cookies should have been removed
        validationTools.verifyOnlyAllowedCookiesStillPresent(webClient, Arrays.asList("JSESSIONID"));

        // Invoke the protected resource again with the leftover cookies
        expectations = vData.addSuccessStatusCodesForActions(Constants.GET_LOGIN_PAGE_ONLY);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the authorization endpoint redirect page.", null, "Redirect To OP");
        genericRP(_testName, webClient, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectations);
    }

    /**
     * Go through the golden path OIDC login flow and then invoke the OP's /logout endpoint. We then re-invoke the protected
     * resource using the same WebClient, and therefore with any of the cookies leftover. We should be able to access the
     * resource again since the RP's SSO cookie hasn't been removed.
     */
    @Test
    public void test_reuseCookiesAfterLogout_logoutEndpointOP() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setEndSession(updatedTestSettings.getEndSession().replaceAll("/end_session", "/logout"));

        List<validationData> expectations = vData.addExpectation(null, Constants.LOGOUT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not successfully logout.", null, "Logout successful");

        logInAndLogOut(webClient, updatedTestSettings, expectations);

        List<String> allowedCookies = Arrays.asList("JSESSIONID", RP_SSO_COOKIE_NAME, "WASOidcSession");

        // Only the OP's SSO cookie should have been removed; the RP's should still be present
        validationTools.verifyOnlyAllowedCookiesStillPresent(webClient, allowedCookies);

        // Invoke the protected resource again with the leftover cookies (including a still-valid RP SSO cookie)
        expectations = vData.addSuccessStatusCodesForActions(Constants.GET_LOGIN_PAGE_ONLY);
        expectations = getSuccessfulAccessExpectations(updatedTestSettings, expectations);
        genericRP(_testName, webClient, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectations);
    }

    /**
     * Go through the golden path OIDC login flow and then invoke the OP's /end_session endpoint. We then re-invoke the protected
     * resource using the same WebClient, and therefore with any of the cookies leftover. We should be able to access the
     * resource again since the RP's SSO cookie hasn't been removed.
     */
    @Test
    public void test_reuseCookiesAfterLogout_endSessionEndpoint() throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setPostLogoutRedirect(null);

        List<validationData> expectations = vData.addExpectation(null, Constants.LOGOUT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not successfully logout.", null, Constants.SUCCESSFUL_LOGOUT_MSG);
        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_URL, Constants.STRING_CONTAINS, "Did not end up at expected post-logout URL.", null, "/oidc/end_session");

        logInAndLogOut(webClient, updatedTestSettings, expectations);

        List<String> allowedCookies = Arrays.asList("JSESSIONID", RP_SSO_COOKIE_NAME, "WASOidcSession");

        // Only the OP's SSO cookie should have been removed; the RP's should still be present
        validationTools.verifyOnlyAllowedCookiesStillPresent(webClient, allowedCookies);

        // Invoke the protected resource again with the leftover cookies (including a still-valid RP SSO cookie)
        expectations = vData.addSuccessStatusCodesForActions(Constants.GET_LOGIN_PAGE_ONLY);
        expectations = getSuccessfulAccessExpectations(updatedTestSettings, expectations);
        genericRP(_testName, webClient, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectations);
    }

    /**
     * - Logs in
     * - Copies resulting cookies over to a new WebClient object
     * - Invokes the protected resource with the leftover cookies, expecting to be granted access
     * - Logs out using the original WebClient session
     * - Invokes the protected resource with the second WebClient object again, this time expecting to be denied access
     */
    @Test
    public void test_reuseCookiesAfterLogout_differentSession_rpLogoutEndpoint() throws Exception {

        Object currentPage = null;
        WebClient webClient = getAndSaveWebClient(true);

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        String endSession = updatedTestSettings.getEndSession();
        updatedTestSettings.setPostLogoutRedirect(endSession.replaceAll("/end_session", "/logout"));
        updatedTestSettings.setEndSession(updatedTestSettings.getTestURL().replaceAll("/SimpleServlet", "/logout"));
        updatedTestSettings.setLogoutHttpMethod(HttpMethod.GET);

        logIn(webClient, updatedTestSettings);

        List<String> allowedCookies = Arrays.asList("JSESSIONID", "LtpaToken2", OP_SSO_COOKIE_NAME, RP_SSO_COOKIE_NAME, "WASOidcSession");

        validationTools.verifyOnlyAllowedCookiesStillPresent(webClient, allowedCookies);

        // Copy all cookies over to a new WebClient
        Set<Cookie> leftoverCookies = webClient.getCookieManager().getCookies();
        WebClient webClient2 = getAndSaveWebClient(true);
        for (Cookie cookie : leftoverCookies) {
            webClient2.getCookieManager().addCookie(cookie);
        }

        // Make sure that a new client with copied cookies can access the protected app without having to log in
        Log.info(thisClass, _testName, "Make sure that a new client with copied cookies can access the protected app without having to log in");
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(Constants.GET_LOGIN_PAGE_ONLY);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.GET_LOGIN_PAGE, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.GET_LOGIN_PAGE, updatedTestSettings);

        currentPage = genericRP(_testName, webClient2, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectations);

        // Log out using the original WebClient
        Log.info(thisClass, _testName, "Log out using the original WebClient");
        expectations = vData.addExpectation(null, Constants.LOGOUT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not successfully logout.", null, "Logout successful");
        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_URL, Constants.STRING_MATCHES, "Did not end up at expected post-logout URL.", null, "/oidc/[^/]+/[^/]+/logout");

        genericRP(_testName, webClient, updatedTestSettings, currentPage, Constants.GOOD_OIDC_LOGOUT_ONLY_ACTIONS, expectations);

        Log.info(thisClass, _testName, "Verify cookies have been deleted from the original web client");
        validationTools.verifyOnlyAllowedCookiesStillPresent(webClient, Arrays.asList("JSESSIONID"));

        allowedCookies = Arrays.asList("JSESSIONID", "LtpaToken2", OP_SSO_COOKIE_NAME, RP_SSO_COOKIE_NAME, "WASOidcSession"); 

        Log.info(thisClass, _testName, "Verify cookies are still present in the second web client");
        validationTools.verifyOnlyAllowedCookiesStillPresent(webClient2, allowedCookies);

        // Invoke the protected resource again using the other WebClient with the copied-over cookies still intact
        Log.info(thisClass, _testName, "Invoke the protected resource again using the other WebClient with the copied-over cookies still intact");
        expectations = vData.addSuccessStatusCodesForActions(Constants.GET_LOGIN_PAGE_ONLY);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);

        genericRP(_testName, webClient2, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectations);
    }

    // TODO - OAuthClientTracker.TRACK_OAUTH_CLIENT_COOKIE_NAME

    private Object logIn(WebClient webClient, TestSettings settings) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = getSuccessfulAccessExpectations(settings, expectations);

        return genericRP(_testName, webClient, settings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);
    }

    private void logInAndLogOut(WebClient webClient, TestSettings settings, List<validationData> logoutExpectations) throws Exception {
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(logoutExpectations, Constants.GOOD_OIDC_LOGIN_LOGOUT_ACTIONS);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did not get to the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = getSuccessfulAccessExpectations(settings, expectations);

        genericRP(_testName, webClient, settings, Constants.GOOD_OIDC_LOGIN_LOGOUT_ACTIONS, expectations);
    }

    private List<validationData> getSuccessfulAccessExpectations(TestSettings settings, List<validationData> expectations) throws Exception {
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, settings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, settings);
        return expectations;
    }

}