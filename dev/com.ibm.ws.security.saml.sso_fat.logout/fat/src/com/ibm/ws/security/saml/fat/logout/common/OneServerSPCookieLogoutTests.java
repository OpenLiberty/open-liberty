/*******************************************************************************
 * Copyright (c) 2018, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.saml.fat.logout.common;

import static org.junit.Assert.fail;

import org.apache.commons.lang3.StringUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * The testcases in this class test SAML Logout. These tests run in multiple configs and with various combinations of login and
 * logouts.
 * Most of the tests use different users for each SP. A subset of tests will use the same user for all SP's
 * These tests use SP Cookies - we will have another test class that will run a subset of tests using LTPA (LTPA will NOT be
 * recommended for use with SAML)
 * The logout behavior will vary depending on the logout as well as the login used.
 * The cookies that are created/removed/left will also depend on the combination of login/logout. These tests will validate that
 * the cookies are handled properly.
 * The following combinations are handled :
 * - IDP Login / IDP Logout
 * - Solicited SP Login / IDP Logout
 * - Unsolicited SP Login / IDP Logout
 * - IDP Login / ibm_security_logout Logout - spLogout = false (logout request does not leave the SP)
 * - Solicited SP Login / ibm_security_logout Logout - spLogout = false (logout request does not leave the SP)
 * - Unsolicited SP Login / ibm_security_logout Logout - spLogout = false (logout request does not leave the SP)
 * - IDP Login / ibm_security_logout Logout - spLogout = true (logout request includes the IDP)
 * - Solicited SP Login / ibm_security_logout Logout - spLogout = true (logout request includes the IDP)
 * - Unsolicited SP Login / ibm_security_logout Logout - spLogout = true (logout request includes the IDP)
 * - IDP Login / httpServletRequest Logout - spLogout = false (logout request does not leave the SP)
 * - Solicited SP Login / httpServletRequest Logout - spLogout = false (logout request does not leave the SP)
 * - Unsolicited SP Login / httpServletRequest Logout - spLogout = false (logout request does not leave the SP)
 * - IDP Login / httpServletRequest Logout - spLogout = true (logout request includes the IDP)
 * - Solicited SP Login / httpServletRequest Logout - spLogout = true (logout request includes the IDP)
 * - Unsolicited SP Login / httpServletRequest Logout - spLogout = true (logout request includes the IDP)
 *
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class OneServerSPCookieLogoutTests extends SAMLLogoutCommonTest {

    private static final Class<?> thisClass = OneServerSPCookieLogoutTests.class;
    protected static final String EmptySessionKeyMsg = "SPSession key cannot be null or empty";
    protected static final String LoadingSessionInformation = "Loading Session Information";
    protected static final String NothingToLogOut = "The logout operation is complete, and no other services appear to have been accessed during this session";

    static String[] twoServer_allSPCookies_list = null;
    static String[] sp1_list = null;
    static String[] sp13_list = null;

    // initialize some common cookie lists
    public static void setCookieSettings(CookieType cookieType) throws Exception {

        String thisMethod = "setCookieSettings";
        msgUtils.printMethodName(thisMethod);

        cookieInfo = new CookieInfo(cookieType);

        // These 2 have to be set for use in some common tooling - set as appropriate for your tests
        allSPCookies_list = new String[] { cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName(), cookieInfo.getSp13CookieName() };
        idp_session_list = new String[] { SAMLConstants.IDP_SESSION_COOKIE_NAME };

        // These are defined and set for as appropriate for this test class
        twoServer_allSPCookies_list = new String[] { cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName(), cookieInfo.getServer2Sp1CookieName(), cookieInfo.getServer2Sp2CookieName() };
        sp1_list = new String[] { cookieInfo.getSp1CookieName() };
        sp13_list = new String[] { cookieInfo.getSp13CookieName() };

    }

    /**
     * Just a refresher:
     * We're having the extending class dictate what the flows will be, setting the logout endpoints and determining if the logout
     * is local only
     * standardFlow - steps/actions to obtain a saml token and gain access
     * justLogout - first step/action in the logout process (it is different depending on the logout type)
     * logoutStep - the step in the flow that actually completes the logout
     * logoutFlow - steps/actions to perform logout (this may be just performLogout, or it may be a series of steps/actions)
     * loginLogoutFlow - combines standFlow and logoutFLow (basically logs ya in, logs ya out of one sp - end to end flow)
     *
     */

    /**
     *
     * The test verifies the correct behavior for logging out
     * Simply log in to 1 sp and then log out
     *
     * @thows Exception
     */
    @Mode(TestMode.LITE)
    @Test
    public void OneServerSPCookieLogoutTests_mainPath() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        List<validationData> expectations = setDefaultGoodSAMLLoginExpectations(loginLogoutFlow, updatedTestSettings, sp1_list);
        expectations = setGoodSAMLLogoutExpectations(expectations, loginLogoutFlow, updatedTestSettings, 1);
        expectations = setCookieExpectationsForFlow(expectations, loginLogoutFlow, LoggedInToOneSP, sp1_list, cookieInfo.getSp1CookieName());

        genericSAML(_testName, webClient, updatedTestSettings, loginLogoutFlow, expectations);
    }

    /**
     * // * Log in to multiple SPs
     * // * Log out of one of the SPs
     * // * DO NOT delete the IDP session cookie before trying to log out
     * // * Expect to loose access to all of the SPs (all sp/ltps cookies should be removed and the idp session cookie should be
     * // * removed in some cases)
     * // * 1) using httpServletRequest - do NOT invoke SSO/IDP logout
     * // * only 1 logout step - the sp1 cookie should be deleted, but the idp session cookie should still exist
     * // * 2) using httpServletRequest - DO invoke IDP logout
     * // * multiple logout steps - the sp1 and IDP Session cookies should be deleted
     * // * 3) using new SP logout endpoint - invokes IDP Logout
     * // * multiple logout steps - the sp1 and IDP Session cookies should be deleted
     * // *
     * // * @throws Exception
     * //
     */
    @Test
    @AllowedFFDC(value = { "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    public void OneServerSPCookieLogoutTests_multipleSPs_leaveIDPSessionCookie_allUseSameUser() throws Exception {

        testUsers = new Testusers(UserType.SAME);
        multipleSPs_leaveIDPSessionCookie();
    }

    @Test
    @AllowedFFDC(value = { "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    public void OneServerSPCookieLogoutTests_multipleSPs_leaveIDPSessionCookie_useDifferentUsers() throws Exception {

        testUsers = new Testusers(UserType.DIFFERENT);
        multipleSPs_leaveIDPSessionCookie();
    }

    public void multipleSPs_leaveIDPSessionCookie() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        // call helper method to log in to 3 different SPs
        loginTo3SPs(webClient);

        //Logout sp 13
        SAMLTestSettings updatedTestSettings13 = testSettings.copyTestSettings();
        updatedTestSettings13.updatePartnerInSettings("sp13", true);

        String[] logoutSteps = logoutFlow;
        List<validationData> expectationsLogout = setGoodSAMLLogoutExpectations(logoutFlow, updatedTestSettings13, 3);

        Log.info(thisClass, _testName, "logoutFlowType: " + logoutFlowType.toString());
        Log.info(thisClass, _testName, "loginFlowType: " + flowType);

        expectationsLogout = setCookieExpectationsForFlow(expectationsLogout, logoutSteps, LoggedInToMultipleSPs, allSPCookies_list, cookieInfo.getSp13CookieName());
        genericSAML(_testName, webClient, updatedTestSettings13, logoutSteps, expectationsLogout);

        /****************************************/
        /* Sanity check that we can log back in */
        /****************************************/
        // if the logout stayed local, we won't need to log into the idp again as we have the idp session cookie, but, if
        // the logout went through the IDP, we'll need to log in to the IDP
        String[] steps = null;
        if (updatedTestSettings13.getLocalLogoutOnly()) {
            steps = noLoginKeepingCookies;
        } else {
            steps = standardFlowUsingIDPCookieKeepingCookies;
        }

        // set expectations for login - including checks for cookies afer invokeacs...
        List<validationData> expectationsLTPA = setDefaultGoodSAMLLoginExpectations(steps, updatedTestSettings13, sp13_list);

        // Log in again to show that login will work
        genericSAML(_testName, webClient, updatedTestSettings13, steps, expectationsLTPA);
    }

    /**
     * Log in to multiple SPs
     * Log out of one of the SPs
     * DO NOT delete the IDP session cookie before trying to log out
     * Expect to loose access to all of the SPs (all sp/ltps cookies should be removed and the idp session cookie should be
     * removed in some cases)
     * 1) using httpServletRequest - do NOT invoke SSO/IDP logout
     * only 1 logout step - the sp1 cookie should be deleted, but the idp session cookie should still exist
     * 2) using httpServletRequest - DO invoke IDP logout
     * multiple logout steps - the sp1 and IDP Session cookies should be deleted
     * 3) using new SP logout endpoint - invokes IDP Logout
     * multiple logout steps - the sp1 and IDP Session cookies should be deleted
     *
     * @throws Exception
     */
    @Test
    @AllowedFFDC(value = { "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    public void OneServerSPCookieLogoutTests_multipleSPs_removeIDPSessionCookie_allUseSameUser() throws Exception {

        testUsers = new Testusers(UserType.SAME);
        multipleSPs_removeIDPSessionCookie();
    }

    @Test
    @AllowedFFDC(value = { "com.ibm.websphere.servlet.session.UnauthorizedSessionRequestException" })
    public void OneServerSPCookieLogoutTests_multipleSPs_removeIDPSessionCookie_useDifferentUsers() throws Exception {

        testUsers = new Testusers(UserType.DIFFERENT);
        multipleSPs_removeIDPSessionCookie();
    }

    public void multipleSPs_removeIDPSessionCookie() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        // call helper method to log in to 3 different SPs
        loginTo3SPs(webClient);

        //Logout
        //expect IDP token to not exist
        SAMLTestSettings updatedTestSettings13 = testSettings.copyTestSettings();
        updatedTestSettings13.updatePartnerInSettings("sp13", true);

        List<validationData> expectationsLogout = setGoodSAMLLogoutExpectations(logoutFlow, updatedTestSettings13, 3);
        expectationsLogout = setCookieExpectationsForFlow(expectationsLogout, logoutFlow, LoggedInToMultipleSPs, allSPCookies_list, cookieInfo.getSp13CookieName());

        // different logout expectations for local/idp logout and different steps will remove the idp session...
        genericSAML(_testName, webClient, updatedTestSettings13, logoutFlow, expectationsLogout);

        WebClient webClient2 = SAMLCommonTestHelpers.getWebClient();
        // Remove IDP session cookie (if it exists)
        Log.info(thisClass, null, "Removing IDP Session cookie from conversation before calling logout");
        webClient2 = helpers.addAllCookiesExcept(webClient2, helpers.extractAllCookiesExcept(webClient, SAMLConstants.IDP_SESSION_COOKIE_NAME), SAMLConstants.IDP_SESSION_COOKIE_NAME);

        //Login again using standard flow, expect to login to IDP because the IDP session cookie was removed
        List<validationData> expectationsLoginAgain = setDefaultGoodSAMLLoginExpectations(standardFlow, updatedTestSettings13, new String[] { SAMLConstants.IDP_SESSION_COOKIE_NAME, cookieInfo.getSp13CookieName() });

        genericSAML(_testName, webClient2, updatedTestSettings13, standardFlow, expectationsLoginAgain);
    }

    /**
     * Log in to an SP that uses an SP cookie
     * Save the SP cookie
     * Log out
     * Put the cookie back into the "conversation"
     * Try to access the app
     * Make sure that we do not have access, we should be prompted to log in again (login "flow" will be different if logout
     * was only done locally, or if calls were made to the IDP)
     *
     * @throws Exception
     */
    @Test
    public void OneServerSPCookieLogoutTests_useCookie_afterLogout() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        List<validationData> expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES, SAMLConstants.COOKIES, SAMLConstants.STRING_CONTAINS, "Conversation did NOT have an SP Cookie.", cookieInfo.getSp1CookieName(), null);
        genericSAML(_testName, webClient, updatedTestSettings, standardFlowKeepingCookies, expectations);

        // Save the SP cookie
        Cookie cookie = helpers.extractSpecificCookie(webClient, cookieInfo.getSp1CookieName());
        if (cookie == null) {
            fail("SP1" + " cookie is null and should not be");
        }
        Log.info(thisClass, _testName, "Extracted Cookie: " + cookie.getName() + " Value: " + cookie.getValue());

        // create a new client and use the sp1 cookie to access the app (just to show that with just the cookie, we can access the app)
        WebClient webClient2 = SAMLCommonTestHelpers.getWebClient();
        webClient2.getCookieManager().addCookie(cookie);
        Log.info(thisClass, _testName, "Trying to access app with new client with " + "SP1" + " cookie added");
        helpers.invokeAppSameConversation(_testName, webClient2, null, updatedTestSettings, updatedTestSettings.getSpDefaultApp(), expectations, SAMLConstants.INVOKE_DEFAULT_APP);

        // now, logout
        List<validationData> expectationsLogout = setGoodSAMLLogoutExpectations(logoutFlow, updatedTestSettings, 1);
        // if we're logging out locally only, the idp session cookie will exist, if we're going to the IDP to logout, it will NOT
        if (updatedTestSettings.getLocalLogoutOnly()) {
            expectationsLogout = addCookieExpectations(expectationsLogout, SAMLConstants.INVOKE_DEFAULT_APP, new String[] { SAMLConstants.IDP_SESSION_COOKIE_NAME }, new String[] { cookieInfo.getSp1CookieName() });
        } else {
            expectationsLogout = addCookieExpectations(expectationsLogout, SAMLConstants.INVOKE_DEFAULT_APP, null, new String[] { cookieInfo.getSp1CookieName(), SAMLConstants.IDP_SESSION_COOKIE_NAME });
        }
        // log out - logout will make sure that we don't have any sp cookies
        genericSAML(_testName, webClient, updatedTestSettings, logoutFlow, expectationsLogout);

        // add the SP1 cookie back into the webClient and try to access the protected app - the cookie should be recognized as invalid and we should need to login
        webClient.getCookieManager().addCookie(cookie);

        List<validationData> accessAppExpectations = vData.addSuccessStatusCodes();
        // expect to be forced to log in again as the cookie is no longer valid
        // add checks to make sure that we hit a login request path
        // the page we go to will be different based on the login flow being used (we'll just make sure that we get to a login page)
        if (flowType.equals(SAMLConstants.UNSOLICITED_SP_INITIATED)) {
            accessAppExpectations = vData.addExpectation(accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive request to process", null, SAMLConstants.IDP_CLIENT_JSP_TITLE);
        } else {
            accessAppExpectations = vData.addExpectation(accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive request to process", null, SAMLConstants.SAML_REQUEST);
        }

        // add checks for cookie existance
        // The SP1 cookie will be removed from the conversation when it is determined that it is invalid
        if (updatedTestSettings.getLocalLogoutOnly()) {
            accessAppExpectations = addCookieExpectations(accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP, new String[] { SAMLConstants.IDP_SESSION_COOKIE_NAME }, new String[] { cookieInfo.getSp1CookieName() });
        } else {
            accessAppExpectations = addCookieExpectations(accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP, null, new String[] { cookieInfo.getSp1CookieName(), SAMLConstants.IDP_SESSION_COOKIE_NAME });
        }

        helpers.invokeAppSameConversation(_testName, webClient, null, updatedTestSettings, updatedTestSettings.getSpDefaultApp(), accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP);

    }

    /**
     * Tests what happens when you try to log out of an SP that you did not log in to - after you have NOT logged into any SPs
     * Local only logouts (servletRequest.logout that stays in the SP succeeds), all others report some sort of failure - but
     * nothing too terrible happens
     *
     * @throws Exception
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void OneServerSPCookieLogoutTests_logoutSPNotLoggedInTo_noOtherSPLoggedIn() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);
        List<validationData> expectationsLogout = null;
        String[] steps = logoutFlow;
        if (logoutFlowType == LogoutFlowType.IDPINITIATED) {
            steps = justLogout;
            expectationsLogout = vData.addExpectation(expectationsLogout, SAMLConstants.PERFORM_SP_LOGOUT, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive get frame indicating that there is nothing to log out", null, NothingToLogOut);
        } else {
            if (logoutFlowType == LogoutFlowType.IBMSECURITYREMOTE) {
                steps = justLogout;
                expectationsLogout = vData.addExpectation(expectationsLogout, logoutStep, SAMLConstants.RESPONSE_URL, SAMLConstants.STRING_CONTAINS, "Did not get to the Logout submit page", null, updatedTestSettings.getSpLogoutURL());
                expectationsLogout = vData.addExpectation(expectationsLogout, logoutStep, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Page (Title check)", null, SAMLConstants.SUCCESSFUL_DEFAULT_LOGOUT_TITLE);
                expectationsLogout = vData.addExpectation(expectationsLogout, logoutStep, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Page (Status check)", null, SAMLConstants.SUCCESSFUL_DEFAULT_LOGOUT_MSG);
                expectationsLogout = helpers.addMessageExpectation(testSAMLServer, expectationsLogout, logoutStep, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Server did not log an error message", SAMLMessageConstants.CWWKS5218E_USER_SESSION_NOT_FOUND);
            } else {
                if (logoutFlowType == LogoutFlowType.HTTPSERVLETREMOTE) {
                    steps = justLogout;
                    expectationsLogout = vData.addExpectation(expectationsLogout, logoutStep, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Message", null, SAMLConstants.OK_MESSAGE);
                    expectationsLogout = vData.addExpectation(expectationsLogout, logoutStep, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Message", null, HttpServletRequestAppMsg);
                    //                expectationsLogout = addCookieExpectations(expectationsLogout, logoutStep, allSPCookies_list, new String[] { cookieInfo.getDefaultSPCookieName() });
                    expectationsLogout = helpers.addMessageExpectation(testSAMLServer, expectationsLogout, logoutStep, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Server did not log an error message", SAMLMessageConstants.CWWKS5218E_USER_SESSION_NOT_FOUND);
                } else {
                    expectationsLogout = setGoodSAMLLogoutExpectations(expectationsLogout, steps, updatedTestSettings, 3);
                }
            }
        }

        // none of the cookies should exist - flow doesn't matter
        expectationsLogout = addCookieExpectations(expectationsLogout, logoutStep, null, new String[] { SAMLConstants.IDP_SESSION_COOKIE_NAME, cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName(), cookieInfo.getSp13CookieName() });

        genericSAML(_testName, webClient, updatedTestSettings, steps, expectationsLogout);

    }

    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void OneServerSPCookieLogoutTests_logoutSPNotLoggedInTo_otherSPLoggedIn_allUseSameUser() throws Exception {
        testUsers = new Testusers(UserType.SAME);
        logoutSPNotLoggedInTo_otherSPLoggedIn();
    }

    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void OneServerSPCookieLogoutTests_logoutSPNotLoggedInTo_otherSPLoggedIn_useDifferentUsers() throws Exception {
        testUsers = new Testusers(UserType.DIFFERENT);
        logoutSPNotLoggedInTo_otherSPLoggedIn();
    }

    public void logoutSPNotLoggedInTo_otherSPLoggedIn() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        loginTo3SPs(webClient);

        //Logout defaultSP
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("defaultSP", true);

        List<validationData> expectationsLogout = null;

        String[] steps = logoutFlow;
        if (logoutFlowType == LogoutFlowType.IBMSECURITYREMOTE) {
            steps = justLogout;
            expectationsLogout = vData.addExpectation(expectationsLogout, logoutStep, SAMLConstants.RESPONSE_URL, SAMLConstants.STRING_CONTAINS, "Did not get to the Logout submit page", null, updatedTestSettings.getSpLogoutURL());
            expectationsLogout = vData.addExpectation(expectationsLogout, logoutStep, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Page (Title check)", null, SAMLConstants.SUCCESSFUL_DEFAULT_LOGOUT_TITLE);
            expectationsLogout = vData.addExpectation(expectationsLogout, logoutStep, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Page (Status check)", null, SAMLConstants.SUCCESSFUL_DEFAULT_LOGOUT_MSG);
            expectationsLogout = helpers.addMessageExpectation(testSAMLServer, expectationsLogout, logoutStep, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Server did not log an error message", SAMLMessageConstants.CWWKS5218E_USER_SESSION_NOT_FOUND);
            expectationsLogout = addCookieExpectations(expectationsLogout, logoutStep, allSPCookies_list, new String[] { cookieInfo.getDefaultSPCookieName() });
        } else {
            if (logoutFlowType == LogoutFlowType.HTTPSERVLETREMOTE) {
                steps = justLogout;
                expectationsLogout = vData.addExpectation(expectationsLogout, logoutStep, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Message", null, SAMLConstants.OK_MESSAGE);
                expectationsLogout = vData.addExpectation(expectationsLogout, logoutStep, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Message", null, HttpServletRequestAppMsg);
                expectationsLogout = addCookieExpectations(expectationsLogout, logoutStep, allSPCookies_list, new String[] { cookieInfo.getDefaultSPCookieName() });
                expectationsLogout = helpers.addMessageExpectation(testSAMLServer, expectationsLogout, logoutStep, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Server did not log an error message", SAMLMessageConstants.CWWKS5218E_USER_SESSION_NOT_FOUND);
            } else {
                if (updatedTestSettings.getLocalLogoutOnly()) {
                    expectationsLogout = addCookieExpectations(expectationsLogout, logoutStep, new String[] { SAMLConstants.IDP_SESSION_COOKIE_NAME }, new String[] { cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName(), cookieInfo.getSp13CookieName() });
                } else {
                    expectationsLogout = addCookieExpectations(expectationsLogout, logoutStep, allSPCookies_list, new String[] { cookieInfo.getDefaultSPCookieName() });
                }
                expectationsLogout = setGoodSAMLLogoutExpectations(expectationsLogout, steps, updatedTestSettings, 3);
            }
        }

        // logout of an sp that we haven't logged in to
        genericSAML(_testName, webClient, updatedTestSettings, steps, expectationsLogout);

        /****/

        // now, after we attempted to logout of something that we had not logged into, let's try one that we have logged into
        SAMLTestSettings updatedTestSettingsLogout2 = testSettings.copyTestSettings();
        updatedTestSettingsLogout2.updatePartnerInSettings("sp1", true);
        //use SP token by default

        List<validationData> expectations = null;
        //        PostLogoutPage postLogoutPage = PostLogoutPage.DEFAULTPOSTLOGOUTPAGE;
        steps = logoutFlow;
        if (logoutFlowType == LogoutFlowType.IDPINITIATED) {
            steps = justLogout;
            expectations = vData.addSuccessStatusCodes();
            expectations = vData.addExpectation(expectations, SAMLConstants.PERFORM_SP_LOGOUT, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive get frame indicating that there is nothing to log out", null, NothingToLogOut);
        } else {
            expectations = setGoodSAMLLogoutExpectations(expectations, steps, updatedTestSettingsLogout2, 3);
            expectations = setCookieExpectationsForFlow(expectations, steps, LoggedInToMultipleSPs, allSPCookies_list, cookieInfo.getSp1CookieName());
        }

        genericSAML(_testName, webClient, updatedTestSettingsLogout2, steps, expectations);

    }

    //    //    @Test
    //    //    public void OneServerSPCookieLogoutTests_logoutWithoutLoggingIn() throws Exception {
    //    // same as        OneServerSPCookieLogoutTests_logoutSPNotLoggedInTo_noOtherSPLoggedIn and OneServerSPCookieLogoutTests_logoutSPNotLoggedInTo_otherSPLoggedIn
    //    //    }
    //
    //    @Test
    //    public void OneServerSPCookieLogoutTests_logoutAfterLogout() throws Exception {
    //
    //    }
    //
    /**
     * Test case that will specify an SP that is NOT configured
     *
     * @throws Exception
     */

    // marked as allowed instead of expected because when run in a local only flow, we don't get the failure
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void OneServerSPCookieLogoutTests_logoutSPDoesNotExist_otherSPLoggedIn() throws Exception {

        testUsers = new Testusers(UserType.SAME); // users don't matter - just need to be set
        sp_missing_or_disabled("sp99");

    }

    /**
     * Test case that will specify an SP that is configured to be disabled
     *
     * @throws Exception
     */
    // marked as allowed instead of expected because when run in a local only flow, we don't get a failure
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    @Test
    public void OneServerSPCookieLogoutTests_logoutSPDisabled_otherSPLoggedIn() throws Exception {

        testUsers = new Testusers(UserType.SAME); // users don't matter - just need to be set
        sp_missing_or_disabled("sp1s2");

    }

    /**
     * The SP MetaData is used by the IDP - the SP Metadata is missing the Logout information
     *
     * @throws Exception
     */
    // marked as allowed instead of expected because when run in a local only flow, we don't get the failure
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    //    @Mode(TestMode.LITE)
    @Test
    public void OneServerSPCookieLogoutTests_logoutUrlMissingFromSPMetaData() throws Exception {

        String[] spCookie = { cookieInfo.getSpUnderscoreCookieName() };
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp_underscore", true);
        //use SP Cookie by default

        int locOfPropYes = Arrays.asList(loginLogoutFlow).lastIndexOf(SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES);
        int locOfSPLogout = Arrays.asList(loginLogoutFlow).lastIndexOf(SAMLConstants.PERFORM_SP_LOGOUT);
        String[] thisFlow = loginLogoutFlow;
        String lastStep = null;

        List<validationData> expectations = null;
        if (updatedTestSettings.getLocalLogoutOnly()) {
            expectations = vData.addSuccessStatusCodes();
            expectations = setGoodSAMLLogoutExpectations(expectations, loginLogoutFlow, updatedTestSettings, 1);
            expectations = setCookieExpectationsForFlow(expectations, thisFlow, LoggedInToOneSP, spCookie, cookieInfo.getSpUnderscoreCookieName());
        } else {
            if (logoutFlowType == LogoutFlowType.IDPINITIATED) {
                // note:  copyOfRange (array, inclusive start index, exclusive end index)
                lastStep = SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES;
                expectations = vData.addSuccessStatusCodes();
                thisFlow = Arrays.copyOfRange(loginLogoutFlow, 0, locOfPropYes + 1);
                expectations = vData.addExpectation(expectations, lastStep, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_CONTAINS, "Did not get a failure trying to find the logout url in the SP metadata that that IDP uses", null, "Unable to resolve outbound message endpoint for relying party");
            } else {
                lastStep = loginLogoutFlow[locOfSPLogout + 1];
                expectations = vData.addSuccessStatusCodes(null, lastStep);
                expectations = vData.addResponseStatusExpectation(expectations, lastStep, SAMLConstants.BAD_REQUEST_STATUS);
                thisFlow = Arrays.copyOfRange(loginLogoutFlow, 0, locOfSPLogout + 2);
                expectations = vData.addExpectation(expectations, lastStep, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_CONTAINS, "Did not get a failure trying to find the logout url in the SP metadata that that IDP uses (idp-process.log)", null, "Unable to resolve outbound message endpoint for relying party");
                expectations = vData.addExpectation(expectations, lastStep, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get a failure trying to find the logout url in the SP metadata that that IDP uses (reponse title)", null, "Unable to Respond");
            }
        }
        expectations = setDefaultGoodSAMLLoginExpectations(expectations, loginLogoutFlow, updatedTestSettings, spCookie);

        Log.info(thisClass, _testName, "Original steps: " + StringUtils.join(loginLogoutFlow, ", "));
        Log.info(thisClass, _testName, "Corrected steps: " + StringUtils.join(thisFlow, ", "));
        genericSAML(_testName, webClient, updatedTestSettings, thisFlow, expectations);
    }

    /**
     * The IDP MetaData is used by the SP - the IDP Metadata is missing the Logout information
     *
     * @throws Exception
     */
    @AllowedFFDC(value = { "com.ibm.ws.security.saml.error.SamlException" })
    //    @Mode(TestMode.LITE)
    @Test
    public void OneServerSPCookieLogoutTests_logoutUrlMissingFromIDPMetaData() throws Exception {

        String[] spCookie = { cookieInfo.getSpDashCookieName() };

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp-dash", true);
        //use SP Cookie by default

        int locOfPropYes = Arrays.asList(loginLogoutFlow).lastIndexOf(SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES);
        int locOfSPLogout = Arrays.asList(loginLogoutFlow).lastIndexOf(SAMLConstants.PERFORM_SP_LOGOUT);
        String[] thisFlow = loginLogoutFlow;

        List<validationData> expectations = setDefaultGoodSAMLLoginExpectations(loginLogoutFlow, updatedTestSettings, spCookie);
        if (updatedTestSettings.getLocalLogoutOnly()) { //HTTPSERVLETLOCAL, IBMSECURITYLOCAL
            expectations = setGoodSAMLLogoutExpectations(expectations, loginLogoutFlow, updatedTestSettings, 1);
            expectations = setCookieExpectationsForFlow(expectations, thisFlow, LoggedInToOneSP, spCookie, cookieInfo.getSpDashCookieName());
        } else {
            switch (logoutFlowType) { //HTTPSERVLETREMOTE, IBMSECURITYREMOTE, IDPINITIATED
            case HTTPSERVLETREMOTE:
                thisFlow = Arrays.copyOfRange(loginLogoutFlow, 0, locOfSPLogout + 1);
                expectations = helpers.addMessageExpectation(testSAMLServer, expectations, logoutStep, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Server did not log an error message", SAMLMessageConstants.CWWKS5214E_LOGOUT_ENDPOINT_MISSING);
                expectations = vData.addExpectation(expectations, logoutStep, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not get an ok in response", null, SAMLConstants.OK_MESSAGE);
                expectations = vData.addExpectation(expectations, logoutStep, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the httpServletRequest Logout app", null, SAMLConstants.HTTP_SERVLET_REQUEST_RESPONSE);
                expectations = addCookieExpectations(expectations, logoutStep, idp_session_list, new String[] { cookieInfo.getSpDashCookieName() });
                break;
            case IBMSECURITYREMOTE:
                thisFlow = Arrays.copyOfRange(loginLogoutFlow, 0, locOfSPLogout + 1);
                expectations = helpers.addMessageExpectation(testSAMLServer, expectations, logoutStep, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Server did not log an error message", SAMLMessageConstants.CWWKS5214E_LOGOUT_ENDPOINT_MISSING);
                expectations = vData.addExpectation(expectations, logoutStep, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not get an ok in response", null, SAMLConstants.OK_MESSAGE);
                expectations = addCookieExpectations(expectations, logoutStep, idp_session_list, new String[] { cookieInfo.getSpDashCookieName() });
                break;
            case IDPINITIATED:
                thisFlow = Arrays.copyOfRange(loginLogoutFlow, 0, locOfPropYes + 1);
                expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Server did not log an error message", SAMLMessageConstants.CWWKS5214E_LOGOUT_ENDPOINT_MISSING);
                expectations = vData.addExpectation(expectations, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not get an ok in response", null, SAMLConstants.OK_MESSAGE);
                expectations = addCookieExpectations(expectations, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES, null, concatStringArrays(allSPCookies_list, idp_session_list));
                break;
            default:
                fail("should never get here");
            }
        }

        Log.info(thisClass, _testName, "Original steps: " + StringUtils.join(loginLogoutFlow, ", "));
        Log.info(thisClass, _testName, "Corrected steps: " + StringUtils.join(thisFlow, ", "));
        genericSAML(_testName, webClient, updatedTestSettings, thisFlow, expectations);

    }

    /**
     * Test that an empty string is allowed in postLogoutRedirectUrl and that we land on the default Post Logout Page
     *
     * @throws Exception
     */
    @Test
    public void OneServerSPCookieLogoutTests_postLogoutRedirectUrl_emptyString() throws Exception {

        common_postLogoutRedirectUrl_test(cookieInfo.getEmptyStringCookieName(), "customLogout_emptyString", PostLogoutPage.DEFAULTPOSTLOGOUTPAGE);

    }

    /**
     * Test that an invalid url in postLogoutRedirectUrl will result in a warning/info msg in the server log and that we land on
     * the default Post Logout Page
     *
     * @throws Exception
     */
    @AllowedFFDC(value = { "java.security.PrivilegedActionException" })
    @Test
    public void OneServerSPCookieLogoutTests_postLogoutRedirectUrl_invalidURL() throws Exception {

        common_postLogoutRedirectUrl_test(cookieInfo.getInvalidURLCookieName(), "customLogout_invalidURL", PostLogoutPage.DEFAULTPOSTLOGOUTPAGE, SAMLMessageConstants.CWWKS5014W_INVALID_URL);

    }

    /**
     * Test that a url with a relative path in postLogoutRedirectUrl will result in landing on the custom Post Logout Page
     *
     * @throws Exception
     */
    @Test
    public void OneServerSPCookieLogoutTests_postLogoutRedirectUrl_relativepath() throws Exception {

        common_postLogoutRedirectUrl_test(cookieInfo.getRelativePathCookieName(), "customLogout_relativePath", PostLogoutPage.CUSTOMPOSTLOGOUTPAGE);

    }

    /**
     * Test that an invalid relative path in postLogoutRedirectUrl will result in a warning/info msg in the server log and that we
     * land on the default Post Logout Page
     *
     * @throws Exception
     */
    @AllowedFFDC(value = { "java.security.PrivilegedActionException" })
    @Test
    public void OneServerSPCookieLogoutTests_postLogoutRedirectUrl_invalidRelativepath() throws Exception {

        common_postLogoutRedirectUrl_test(cookieInfo.getInvalidRelativePathCookieName(), "customLogout_invalidRelativePath", PostLogoutPage.DEFAULTPOSTLOGOUTPAGE, SAMLMessageConstants.CWWKS5014W_INVALID_URL);

    }

    /**
     * Test that a valid absolute path in postLogoutRedirectUrl will result in landing on the custom Post Logout Page
     *
     * @throws Exception
     */
    @Test
    public void OneServerSPCookieLogoutTests_postLogoutRedirectUrl_absoluteLocalURL() throws Exception {

        common_postLogoutRedirectUrl_test(cookieInfo.getAbsLocalURLCookieName(), "customLogout_absLocalURL", PostLogoutPage.CUSTOMPOSTLOGOUTPAGE);

    }

    /**
     * Test that a external absolute path in postLogoutRedirectUrl will result in landing on the that external page
     *
     * @throws Exception
     */
    @Test
    public void OneServerSPCookieLogoutTests_postLogoutRedirectUrl_absoluteExternalURL() throws Exception {

        common_postLogoutRedirectUrl_test(cookieInfo.getAbsExternalURLCookieName(), "customLogout_absExternalURL", PostLogoutPage.EXTERNALPOSTLOGOUTPAGE);

    }

    /**
     * Test that a external absolute path in postLogoutRedirectUrl will result in landing on the that external page
     *
     * @throws Exception
     */
    @Test
    public void OneServerSPCookieLogoutTests_postLogoutRedirectUrl_doesNotExistURL() throws Exception {

        String[] spCookie = { cookieInfo.getSp5CookieName() };

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp5", true);

        List<validationData> expectations = null;
        if ((logoutFlowType == LogoutFlowType.HTTPSERVLETREMOTE) || (logoutFlowType == LogoutFlowType.IBMSECURITYREMOTE)) {
            expectations = vData.addSuccessStatusCodes(null, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES);
            expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES, SAMLConstants.NOT_FOUND_STATUS);
            expectations = setDefaultGoodSAMLLoginExpectations(expectations, loginLogoutFlow, updatedTestSettings, spCookie);
            expectations = vData.addExpectation(expectations, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not get a failure in response (Message)", null, SAMLConstants.NOT_FOUND_MSG);
            expectations = vData.addExpectation(expectations, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get a failure in response (Full text)", null, SAMLConstants.NOT_FOUND_ERROR);
            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Server did not log an error message", SAMLMessageConstants.CWWKS5003E_ENDPOINT_NOT_SUPPORTED);
            expectations = setDefaultGoodSAMLLoginExpectations(expectations, loginLogoutFlow, updatedTestSettings, spCookie);
            genericSAML(_testName, webClient, updatedTestSettings, loginLogoutFlow, expectations);
        } else {
            common_postLogoutRedirectUrl_test(cookieInfo.getSp5CookieName(), "sp5", PostLogoutPage.DEFAULTPOSTLOGOUTPAGE);

        }

    }

    /**
     * Login to multiple sp with one session, then login in to other sp's using another idp session.
     * Log out of an sp in session 1 - make sure that we're only logged out of the sessions that
     * we should be.
     */
    @Mode(TestMode.LITE)
    @Test
    public void OneServerSPCookieLogoutTests_logoutWithMultipleIDPSessions_differentConversations_allUseSameUser() throws Exception {

        testUsers = new Testusers(UserType.SAME);
        logoutWithMultipleIDPSessions_differentConversations();
    }

    @Mode(TestMode.LITE)
    @Test
    public void OneServerSPCookieLogoutTests_logoutWithMultipleIDPSessions_differentConversations_useDifferentUsers() throws Exception {

        testUsers = new Testusers(UserType.DIFFERENT);
        logoutWithMultipleIDPSessions_differentConversations();
    }

    public void logoutWithMultipleIDPSessions_differentConversations() throws Exception {

        String[] client1_oneSPCookie = { cookieInfo.getSp1CookieName() };
        String[] client1_twoSPCookies = { cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName() };

        String[] client2_oneSPCookie = { cookieInfo.getSp13CookieName() };
        String[] client2_twoSPCookies = { cookieInfo.getSp13CookieName(), cookieInfo.getSp2CookieName() };
        String[] client2_threeSPCookies = { cookieInfo.getSp13CookieName(), cookieInfo.getSp2CookieName(), cookieInfo.getSp5CookieName() };
        String[] client2_missingAfterCookies = { cookieInfo.getSp1CookieName(), cookieInfo.getSp5CookieName() };

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();
        WebClient webClient2 = SAMLCommonTestHelpers.getWebClient();

        // log into 2 sps in 2 different webClients
        loginToSP(webClient, standardFlowKeepingCookies, testUsers.getUser1(), testUsers.getPassword1(), "sp1", client1_oneSPCookie);
        loginToSP(webClient, setFlowForAnotherLogin(), testUsers.getUser2(), testUsers.getPassword2(), "sp2", client1_twoSPCookies);

        loginToSP(webClient2, standardFlowKeepingCookies, testUsers.getUser3(), testUsers.getPassword3(), "sp13", client2_oneSPCookie);
        loginToSP(webClient2, setFlowForAnotherLogin(), testUsers.getUser2(), testUsers.getPassword2(), "sp2", client2_twoSPCookies);

        //Logout sp1 in webclient 1
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        // Now, log out of an sp in webClient 1
        // The cookies are associated with the web client, so, when logging out of sp1, all cookies associated with that specific client
        // should be gone - and cookies associated with other web clients should still exist

        PostLogoutPage postLogoutPage = PostLogoutPage.DEFAULTPOSTLOGOUTPAGE;
        List<validationData> expectationsLogout = null;
        expectationsLogout = setCookieExpectationsForFlow(expectationsLogout, logoutFlow, LoggedInToMultipleSPs, client1_twoSPCookies, cookieInfo.getSp1CookieName());
        expectationsLogout = setGoodSAMLLogoutExpectations(expectationsLogout, logoutFlow, updatedTestSettings, 2, postLogoutPage);
        genericSAML(_testName, webClient, updatedTestSettings, logoutFlow, expectationsLogout);

        // make sure that we can still use sp2 with webClient2
        // When we're using LTPA cookies, AND using the same user to log into each sp,
        // we'll still see the cookie, but, the underlying cache entry will no longer exist and we'll have to log in
        SAMLTestSettings accessAppTestSettings = testSettings.copyTestSettings();
        accessAppTestSettings.updatePartnerInSettings("sp2", true);
        accessAppTestSettings.setIdpUserName(testUsers.getUser2());
        accessAppTestSettings.setIdpUserPwd(testUsers.getPassword2());

        List<validationData> accessAppExpectations = vData.addSuccessStatusCodes();
        // Each SP cookie should be unique and therefore exist and be usable - we should get to the app without having to log in
        accessAppExpectations = vData.addExpectation(accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on default app", null, SAMLConstants.APP1_TITLE);

        helpers.invokeAppSameConversation(_testName, webClient2, null, accessAppTestSettings, accessAppTestSettings.getSpDefaultApp(), accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP);

        // after the logout, we should make sure that all cookies exist and are usable - then try to use a different sp in the
        // second client - show that the idp session is still good
        // don't need to set the step as validateConversationCookies doesn't check it (the methods that call it normally only call it if the method is correct)
        List<validationData> client2Expectations = addCookieExpectations(null, client2_twoSPCookies, client2_missingAfterCookies);
        Log.info(thisClass, _testName, "Validate cookies in webClient2");
        for (validationData expected : client2Expectations) {
            if (expected.getWhere().equals(SAMLConstants.COOKIES)) {
                validationTools.validateConversationCookies(webClient2, expected);
            }
        }

        loginToSP(webClient2, setFlowForAnotherLogin(), testUsers.getUser5(), testUsers.getPassword5(), "sp5", client2_threeSPCookies);

    }

    /**** Helper methods ****/
    /**
     * The flow/behavior of the tests for a missing SP, or a disabled SP are the same. This method performs the test for both
     * First we log into 3 existing/enabled SPs, then we try to log out from one that either doesn't exist or is disabled.
     *
     * @param sp
     *            - the sp to use for the test - this is the only difference between the 2 tests.
     * @throws Exception
     */
    public void sp_missing_or_disabled(String sp) throws Exception {
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        // call helper method to log in to 3 different SPs
        loginTo3SPs(webClient);

        //Logout sp
        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings(sp, true);
        List<validationData> expectations = new ArrayList<validationData>();
        String[] steps = logoutFlow;
        switch (logoutFlowType) {
        case IBMSECURITYLOCAL:
        case IBMSECURITYREMOTE:
            steps = justLogout;
            if (sp.equals("sp99")) { // app doesn't exist, so not found is issued
                expectations = vData.addSuccessStatusCodes(expectations, SAMLConstants.PERFORM_SP_LOGOUT);
                expectations = vData.addResponseStatusExpectation(expectations, SAMLConstants.PERFORM_SP_LOGOUT, SAMLConstants.NOT_FOUND_STATUS);
                expectations = vData.addExpectation(expectations, logoutStep, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_MATCHES, "Did not find appropriate context root not found title.", null, SAMLConstants.CONTEXT_ROOT_NOT_FOUND);
                expectations = addCookieExpectations(expectations, logoutStep, new String[] { SAMLConstants.IDP_SESSION_COOKIE_NAME, cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName(), cookieInfo.getSp13CookieName() }, null);
            } else { // app is disabled
                expectations = vData.addSuccessStatusCodes(expectations);
                expectations = vData.addExpectation(expectations, logoutStep, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Page (Title check)", null, SAMLConstants.SUCCESSFUL_DEFAULT_LOGOUT_TITLE);
                expectations = vData.addExpectation(expectations, logoutStep, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Page (Status check)", null, SAMLConstants.SUCCESSFUL_DEFAULT_LOGOUT_MSG);
                expectations = addCookieExpectations(expectations, logoutStep, new String[] { SAMLConstants.IDP_SESSION_COOKIE_NAME }, new String[] { cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName(), cookieInfo.getSp13CookieName() });
                if (logoutFlowType == LogoutFlowType.IBMSECURITYREMOTE) {
                    expectations = helpers.addMessageExpectation(testSAMLServer, expectations, logoutStep, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive warning about not finding sp", SAMLMessageConstants.CWWKS5215E_NO_AVAILABLE_SP);
                }
            }
            break;
        case HTTPSERVLETREMOTE:
            steps = justLogout;
            expectations = vData.addSuccessStatusCodes(expectations);
            expectations = vData.addExpectation(expectations, logoutStep, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to httpServletRequest App", null, HttpServletRequestAppMsg);
            // make sure the cookies have not been disabled
            expectations = addCookieExpectations(expectations, logoutStep, new String[] { SAMLConstants.IDP_SESSION_COOKIE_NAME }, new String[] { cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName(), cookieInfo.getSp13CookieName() });
            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, logoutStep, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Server did not log an error message", SAMLMessageConstants.CWWKS5215E_NO_AVAILABLE_SP);
            break;
        default: // (IDP Initiated and HTTPSERVLETLOCAL)
            expectations = setGoodSAMLLogoutExpectations(expectations, steps, updatedTestSettings, 3);
            setCookieExpectationsForFlow(expectations, steps, LoggedInToMultipleSPs, allSPCookies_list, null);
            break;
        }

        genericSAML(_testName, webClient, updatedTestSettings, steps, expectations);

    }

    /**
     * Performs a login/logout and verifies that we have the correct cookies, but, most importantly, verifies that we land on the
     * correct
     * postLogoutRedirect page
     *
     * @param spCookieName
     *            - cookie name that the sp uses (validate that it exists when it should and gets removed when it should)
     * @param spName
     *            - the SP that we're testing with (its config determines which postLogoutPageUrl is used)
     * @param postLogoutUrl
     *            - flag indicating which page to validate (default, custom or external)
     * @param errorMsg
     *            - what error message if any to look for in the server side message log
     * @throws Exception
     */
    public void common_postLogoutRedirectUrl_test(String spCookieName, String spName, PostLogoutPage postLogoutUrl) throws Exception {
        common_postLogoutRedirectUrl_test(spCookieName, spName, postLogoutUrl, null);
    }

    public void common_postLogoutRedirectUrl_test(String spCookieName, String spName, PostLogoutPage postLogoutUrl, String errorMsg) throws Exception {

        String[] cookieList = new String[] { spCookieName };

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings(spName, true);

        List<validationData> expectations = setDefaultGoodSAMLLoginExpectations(loginLogoutFlow, updatedTestSettings, cookieList);
        expectations = setGoodSAMLLogoutExpectations(expectations, loginLogoutFlow, updatedTestSettings, 1, postLogoutUrl);
        expectations = setCookieExpectationsForFlow(expectations, loginLogoutFlow, LoggedInToOneSP, cookieList, spCookieName);
        if (errorMsg != null && !updatedTestSettings.getLocalLogoutOnly() && logoutFlowType != LogoutFlowType.IDPINITIATED) {
            String lastStep = loginLogoutFlow[loginLogoutFlow.length - 1];
            expectations = helpers.addMessageExpectation(testSAMLServer, expectations, lastStep, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Did not receive warning that the postLogoutRedirectUrl was invalid - runtime will use the default page", errorMsg);
        }

        genericSAML(_testName, webClient, updatedTestSettings, loginLogoutFlow, expectations);
    }

}
