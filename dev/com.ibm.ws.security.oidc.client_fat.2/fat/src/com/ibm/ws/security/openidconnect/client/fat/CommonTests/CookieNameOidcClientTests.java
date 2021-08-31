/*******************************************************************************
 * Copyright (c) 2014, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.client.fat.CommonTests;

import java.net.HttpURLConnection;
import java.net.URL;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.ibm.ws.security.fat.common.utils.AutomationTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ClientTestHelpers;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonCookieTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.meterware.httpunit.WebConversation;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * This is the test class that contains common code for all of the
 * OpenID Connect end session logout tests.
 **/

public class CookieNameOidcClientTests extends CommonTest {

    public static Class<?> thisClass = CookieNameOidcClientTests.class;
    protected static CommonCookieTools cookieTools = new CommonCookieTools();
    public static final String opServerName = "com.ibm.ws.security.openidconnect.client-1.0_fat.op";
    public static final String testSpecificOPCookieName = "MyOPCookieName";
    public static final String rpServerName = "com.ibm.ws.security.openidconnect.client-1.0_fat.rp";
    public static final String testSpecificRPCookieName = "MyRPCookieName";
    public static final String testSpecificCookieName = "MyCookieName";

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this scenario, the OIDC client logs into OP using the RP-OP basic flow
     * <LI>and gets ID token/access token after a successful login.
     * <LI>Check for the default OP and RP cookie names in the header.
     * <LI>
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The OP and RP cookies in the header should have a value
     * </OL>
     */
    @Test
    public void OidcClientCheckDefaultCookieNames() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setPostLogoutRedirect(null);

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not successfully logout.", null, Constants.SUCCESSFUL_LOGOUT_MSG);
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

        // validate the cookie name and that it has value
        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testOPServer, opServerName));
        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testRPServer, rpServerName));
        cookieTools.validateCookie(wc, Constants.LTPA_TOKEN, false);

    }

    /**
     * PI84359
     * checks that when authenticationTimeLimit has a non-default value in the configuration,
     * the rp-issued cookie has an appropriate expiration time. In this case we'll set the
     * timeLimit to zero so the cookie should show up as expired.
     * 
     * @throws Exception
     */
    @Test
    public void OidcClientCheckCookieTimeouts() throws Exception {

        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_orig.xml", "rp_server_cookie_expired.xml");

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setPostLogoutRedirect(null);

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        genericRP(_testName, wc, updatedTestSettings, Constants.GET_LOGIN_PAGE_ONLY, expectations);

        // httpUnit does a terrible job with reporting cookies and their states.
        // we must use low level connection to assess if the cookie is expired.
        System.out.println("****** checking cookies");

        URL rpApp = AutomationTools.getNewUrl("http://localhost:" + testRPServer.getServerHttpPort() + "/formlogin/SimpleServlet");
        HttpURLConnection conn = (HttpURLConnection) rpApp.openConnection();
        conn.setInstanceFollowRedirects(false);
        int i = 1;
        String cookieLine = null;
        while (true) {
            String key = conn.getHeaderFieldKey(i);
            String value = conn.getHeaderField(i);
            System.out.println("*** header key: " + key + " value: " + value);
            if (value == null || i > 100) {
                break;
            } // end of the headers
            if (value.contains("WASOidcState")) {
                cookieLine = value;
            }
            i++;
        }
        boolean foundExpiredCookie = false;
        if (cookieLine != null && (cookieLine.contains("Expires=") && cookieLine.contains("16:00"))) {
            foundExpiredCookie = true;
        }
        Assert.assertTrue("did not find expected expired cookie", foundExpiredCookie);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this scenario, the OIDC client logs into OP using the RP-OP basic flow
     * <LI>and gets ID token/access token after a successful login.
     * <LI>Then use the end_session endpoint to log out.
     * <LI>Check for the default OP and RP cookie names in the header.
     * <LI>
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The RP cookie in the header should have a value
     * <LI>The OP cookie in the header should NOT have a value
     * </OL>
     */
    @Test
    public void OidcClientCheckDefaultCookieNameAfterLogout() throws Exception {

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setPostLogoutRedirect(null);

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not successfully logout.", null, Constants.SUCCESSFUL_LOGOUT_MSG);
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_LOGOUT_ACTIONS, expectations);

        // validate the cookie name and that it has value
        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testOPServer, opServerName), false);
        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testRPServer, rpServerName));
        cookieTools.validateCookie(wc, Constants.LTPA_TOKEN, false);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this scenario, the OIDC client logs into OP using the RP-OP basic flow
     * <LI>and gets ID token/access token after a successful login.
     * <LI>Check for the test specified OP and RP cookie names in the header.
     * <LI>
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The OP and RP cookies in the header should have a value
     * </OL>
     */
    @Test
    public void OidcClientCheckSpecificCookieNames() throws Exception {

        // Reconfigure OP server with cookie name defined
        // Reconfigure RP server with cookie name defined
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_cookieName.xml", "rp_server_cookieName.xml");

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setPostLogoutRedirect(null);

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not successfully logout.", null, Constants.SUCCESSFUL_LOGOUT_MSG);
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

        // validate the cookie name and that it has value
        cookieTools.validateCookie(wc, testSpecificOPCookieName);
        cookieTools.validateCookie(wc, testSpecificRPCookieName);
        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testOPServer, opServerName), false);
        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testRPServer, rpServerName), false);
        cookieTools.validateCookie(wc, Constants.LTPA_TOKEN, false);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this scenario, the OIDC client logs into OP using the RP-OP basic flow
     * <LI>and gets ID token/access token after a successful login.
     * <LI>Then use the end_session endpoint to log out.
     * <LI>Check for the test specified OP and RP cookie names in the header.
     * <LI>
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The RP cookie in the header should have a value
     * <LI>The OP cookie in the header should NOT have a value
     * </OL>
     */
    @Test
    public void OidcClientCheckSpecificCookieNameAfterLogout() throws Exception {

        // Reconfigure OP server with cookie name defined
        // Reconfigure RP server with cookie name defined
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_cookieName.xml", "rp_server_cookieName.xml");

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setPostLogoutRedirect(null);

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not successfully logout.", null, Constants.SUCCESSFUL_LOGOUT_MSG);
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_LOGOUT_ACTIONS, expectations);

        // validate the cookie name and that it has value
        cookieTools.validateCookie(wc, testSpecificOPCookieName, false);
        cookieTools.validateCookie(wc, testSpecificRPCookieName);
        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testRPServer, rpServerName), false);
        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testOPServer, opServerName), false);
        cookieTools.validateCookie(wc, Constants.LTPA_TOKEN, false);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this scenario, the OIDC client logs into OP using the RP-OP basic flow
     * <LI>and gets ID token/access token after a successful login.
     * <LI>Check for the test specified duplicate OP and RP cookie names in the header.
     * <LI>
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The common cookie in the header should have a value
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientOPRPDuplicateSpecificCookieNames() throws Exception {

        // Reconfigure OP server with cookie name defined
        // Reconfigure RP server with cookie name defined
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_duplicateCookieName.xml", "rp_server_duplicateCookieName.xml");

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setPostLogoutRedirect(null);

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not successfully logout.", null, Constants.SUCCESSFUL_LOGOUT_MSG);
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_POST_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

        // validate the cookie name and that it has value
        cookieTools.validateCookie(wc, testSpecificCookieName);
        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testRPServer, rpServerName), false);
        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testOPServer, opServerName), false);
        cookieTools.validateCookie(wc, Constants.LTPA_TOKEN, false);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this scenario, the OIDC client logs into OP using the RP-OP basic flow
     * <LI>and gets ID token/access token after a successful login.
     * <LI>Then use the end_session endpoint to log out.
     * <LI>Check for the test specified duplicate OP and RP cookie names in the header.
     * <LI>
     * </OL>
     * <P>
     * Expected Results:
     * <OL>
     * <LI>The common cookie in the header should NOT have a value
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void OidcClientOPRPDuplicateSpecificCookieNamesAfterLogout() throws Exception {

        // Reconfigure OP server with cookie name defined
        // Reconfigure RP server with cookie name defined
        ClientTestHelpers.reconfigServers(_testName, Constants.JUNIT_REPORTING, "op_server_duplicateCookieName.xml", "rp_server_duplicateCookieName.xml");

        WebConversation wc = new WebConversation();

        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setScope("openid profile");
        updatedTestSettings.setPostLogoutRedirect(null);

        List<validationData> expectations = vData.addSuccessStatusCodes(null);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not get the OpenID Connect login page.", null, Constants.LOGIN_PROMPT);
        expectations = validationTools.addDefaultIDTokenExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
        expectations = validationTools.addDefaultGeneralResponseExpectations(expectations, _testName, eSettings.getProviderType(), Constants.LOGIN_USER, updatedTestSettings);
        expectations = vData.addExpectation(expectations, Constants.LOGOUT, Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did Not successfully logout.", null, Constants.SUCCESSFUL_LOGOUT_MSG);
        genericRP(_testName, wc, updatedTestSettings, Constants.GOOD_OIDC_LOGIN_LOGOUT_ACTIONS, expectations);

        // validate the cookie name and that it does not have value
        cookieTools.validateCookie(wc, testSpecificCookieName, false);
        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testRPServer, rpServerName), false);
        cookieTools.validateCookie(wc, cookieTools.buildDefaultCookieName(testOPServer, opServerName), false);
        cookieTools.validateCookie(wc, Constants.LTPA_TOKEN, false);

    }

}
