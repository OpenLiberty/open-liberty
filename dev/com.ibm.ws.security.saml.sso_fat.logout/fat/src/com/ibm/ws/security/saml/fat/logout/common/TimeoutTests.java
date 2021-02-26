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

import java.util.List;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLMessageConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/*
 * These tests are used to validate the correct behavior between the Liberty SAML SP and a
 * Shibboleth IDP when the IDP is configured with a very short idp.session.timeout value
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class TimeoutTests extends SAMLLogoutCommonTest {

    private static final Class<?> thisClass = TimeoutTests.class;
    private static final String IDP_TIMEOUT_MSG = "ProcessLogout: No active session found matching current request";
    //    private static final String NO_SESSION_FOUND_MSG = "No SPSession found for service https";
    private static final String NO_SESSION_FOUND_MSG = "No.*ession found.*"; // Windows gets a more generic error

    private static final String SESSION_NOT_FOUND = "SessionNotFound";

    protected static String serverMasterConfig = "";
    protected static String serverOtherConfig = "";

    static String shortLifetimeSP = "spShortLifetime";
    static String shortLifetimeCookie = SAMLConstants.SP_COOKIE_PREFIX + shortLifetimeSP;
    String[] spShortLifetime_list = { shortLifetimeCookie };

    /**
     * Just a refresher:
     * We're having the extending class dictate what the flows will be, setting the logout endpoints and deternining if the logout
     * is local only
     * standardFlow - steps/actions to obtain a saml token and gain access
     * justLogout - steps/actions to perform Just a logout (this may be just performLogout, or it may be a series of steps/actions
     * loginLogoutFlow - combines standFlow and justLogout (basically logs ya in, logs ya out of one sp - end to end flow)
     *
     */

    static String[] twoServer_allSPCookies_list = null;
    static String[] sp1_list = null;
    static String[] sp13_list = null;

    // initialize some common cookie lists
    public static void setCookieSettings(CookieType cookieType) throws Exception {

        String thisMethod = "setCookieSettings";
        msgUtils.printMethodName(thisMethod);

        cookieInfo = new CookieInfo(cookieType);

        // These 2 have to be set for use in some common tooling - set as appropriate for your tests
        allSPCookies_list = new String[] { cookieInfo.getSp1CookieName() };
        idp_session_list = new String[] { SAMLConstants.IDP_SESSION_COOKIE_NAME };

        sp1_list = new String[] { cookieInfo.getSp1CookieName() };

    }

    /**
     * Login via one SP and then sleep until after the IDP session expires
     * Try to logout.
     * Types of Logouts and their behavior:
     * IDP Initiated - since the session is expired, we won't get very far - Shibboleth won't find our SP session - it'll thrash
     * around, but our SP will not be called to log out
     * all of the cookies will exist at the end of logout and we'll be able to use the SP cookie after the logout attempt. We have
     * no way to disable the cookie
     * ibm_security_logout (local) - since we never go the IDP, the IDP session lifetime does NOT matter - see same behavior as a
     * normal logout flow
     * ibm_security_logout (remote) - local clean up of the sp cookie will succeed, but the IDP will (again) thrash around -
     * leaving the idp session cookie
     * httpServletRequest (local) - since we never go the IDP, the IDP session lifetime does NOT matter - see same behavior as a
     * normal logout flow
     * httpServletRequest (remote) -local clean up of the sp cookie will succeed, but the IDP will (again) thrash around - leaving
     * the idp session cookie
     *
     * @throws Exception
     */
    public void test_logoutAfterIDPSessionExpires(String loginType, String logoutType, boolean localOnly, String[] flowOverride) throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();
        boolean spCookieStillGood;

        SAMLTestSettings settings = testSettings.copyTestSettings();
        // Set test settings for requested flow type
        setLogoutFlowSettings(settings, loginType, logoutType, localOnly);
        if (flowOverride != null) {
            logoutFlow = flowOverride;
        }

        loginToSP(webClient, settings, standardFlowKeepingCookies, testUsers.getUser1(), testUsers.getPassword1(), "sp1", sp1_list);

        helpers.testSleep(35);

        //Logout sp 1
        SAMLTestSettings updatedTestSettings = settings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp1", true);

        String lastAction = logoutFlow[logoutFlow.length - 1];
        List<validationData> expectationsLogout = vData.addSuccessStatusCodes();

        if (updatedTestSettings.getLocalLogoutOnly()) {
            expectationsLogout = setGoodSAMLLogoutExpectations(logoutFlow, updatedTestSettings, 1);
            expectationsLogout = addCookieExpectations(expectationsLogout, lastAction, idp_session_list, sp1_list);
            spCookieStillGood = false;
        } else {
            // idp init - will still have the sp cookie, all others should should only have idp session
            if (logoutFlowType.equals(LogoutFlowType.IDPINITIATED)) {
                expectationsLogout = setGoodSAMLLogoutExpectations(logoutFlow, updatedTestSettings, 1);
                expectationsLogout = vData.addExpectation(expectationsLogout, lastAction, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Logout skipped page (Title)", null, SAMLConstants.SAML_SHIBBOLETH_LOGIN_HEADER);
                expectationsLogout = vData.addExpectation(expectationsLogout, lastAction, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the Logout skipped page (Body)", null, "You elected not to log out of all the services accessed during your session.");
                expectationsLogout = vData.addExpectation(expectationsLogout, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_CONTAINS, "Did not get msg indicating that the idp session had timed out.", null, IDP_TIMEOUT_MSG);
                expectationsLogout = addCookieExpectations(expectationsLogout, lastAction, new String[] { cookieInfo.getSp1CookieName(), SAMLConstants.IDP_SESSION_COOKIE_NAME }, null);
                spCookieStillGood = true;
            } else {
                expectationsLogout = vData.addExpectation(expectationsLogout, lastAction, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Logout skipped page (Title)", null, SAMLConstants.SUCCESSFUL_DEFAULT_SP_LOGOUT_TITLE);
                expectationsLogout = vData.addExpectation(expectationsLogout, lastAction, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not get to the Logout skipped page (MSG)", null, SAMLConstants.OK_MESSAGE);
                expectationsLogout = vData.addExpectation(expectationsLogout, lastAction, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the Logout skipped page (Body)", null, SAMLConstants.FAILED_LOGOUT_MSG);
                expectationsLogout = vData.addExpectation(expectationsLogout, lastAction, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_CONTAINS, "Did not get msg indicating that the idp session had timed out.", null, SESSION_NOT_FOUND);
                // The IDP doesn't delete it's cookie, but, that's their problem
                expectationsLogout = addCookieExpectations(expectationsLogout, lastAction, idp_session_list, sp1_list);
                spCookieStillGood = false;
            }
        }

        genericSAML(_testName, webClient, updatedTestSettings, logoutFlow, expectationsLogout);

        // make sure that we really do NOT have access
        testAccessToAppAfterLogout(webClient, updatedTestSettings, spCookieStillGood);
    }

    public void test_logoutAfterSAMLTokenExpires(String loginType, String logoutType, boolean localOnly, String[] flowOverride) throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings settings = testSettings.copyTestSettings();
        // Set test settings for requested flow type
        setLogoutFlowSettings(settings, loginType, logoutType, localOnly);

        if (flowOverride != null) {
            logoutFlow = flowOverride;
        }
        loginToSP(webClient, settings, standardFlowKeepingCookies, testUsers.getUser1(), testUsers.getPassword1(), shortLifetimeSP, spShortLifetime_list);

        helpers.testSleep(25);

        //Logout sp spShortLifetime
        SAMLTestSettings updatedTestSettings = settings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings(shortLifetimeSP, true);

        String lastAction = logoutFlow[logoutFlow.length - 1];
        List<validationData> expectationsLogout = vData.addSuccessStatusCodes();

        switch (logoutFlowType) {
        case IDPINITIATED:
            expectationsLogout = vData.addExpectation(expectationsLogout, lastAction, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Logout skipped page (Title)", null, SAMLConstants.SAML_SHIBBOLETH_LOGIN_HEADER);
            expectationsLogout = vData.addExpectation(expectationsLogout, lastAction, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the Logout skipped page (Body)", null, "You elected not to log out of all the services accessed during your session.");
            expectationsLogout = vData.addExpectation(expectationsLogout, SAMLConstants.PROCESS_LOGOUT_PROPAGATE_YES, SAMLConstants.IDP_PROCESS_LOG, SAMLConstants.STRING_MATCHES, "Did not get msg indicating that no session was found.", null, NO_SESSION_FOUND_MSG);
            // The idp session is destroyed because the SAML token is no longer valid - the local cookie still exists because the IDP deletes it  (that's an IDP issue, not a Liberty SP issue)
            expectationsLogout = addCookieExpectations(expectationsLogout, lastAction, spShortLifetime_list, idp_session_list);
            break;
        case HTTPSERVLETREMOTE:
            expectationsLogout = helpers.addMessageExpectation(testSAMLServer, expectationsLogout, lastAction, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Server did not log an error message", SAMLMessageConstants.CWWKS5218E_USER_SESSION_NOT_FOUND);
        case HTTPSERVLETLOCAL: // // remote has a failure message, and displays the same response as local
            expectationsLogout = vData.addExpectation(expectationsLogout, lastAction, SAMLConstants.RESPONSE_MESSAGE, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Message", null, SAMLConstants.OK_MESSAGE);
            expectationsLogout = vData.addExpectation(expectationsLogout, lastAction, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Message", null, HttpServletRequestAppMsg);
            break;
        case IBMSECURITYREMOTE: // remote has a failure message, and displays the same response as local
            expectationsLogout = helpers.addMessageExpectation(testSAMLServer, expectationsLogout, lastAction, SAMLConstants.SAML_MESSAGES_LOG, SAMLConstants.STRING_CONTAINS, "Server did not log an error message", SAMLMessageConstants.CWWKS5218E_USER_SESSION_NOT_FOUND);
        case IBMSECURITYLOCAL:
            expectationsLogout = vData.addExpectation(expectationsLogout, lastAction, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout page", null, SAMLConstants.SUCCESSFUL_DEFAULT_LOGOUT_TITLE);
            expectationsLogout = vData.addExpectation(expectationsLogout, lastAction, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not get to the Successful Logout Message", null, SAMLConstants.SUCCESSFUL_DEFAULT_LOGOUT_MSG);
            break;
        default:
            fail("Should NOT get to this case");
        }

        if (!logoutFlowType.equals(LogoutFlowType.IDPINITIATED)) {
            // for all but the IDP initiated flow, all local cookies are gone, but the idp session should still exist
            expectationsLogout = addCookieExpectations(expectationsLogout, lastAction, idp_session_list, spShortLifetime_list);
        }

        genericSAML(_testName, webClient, updatedTestSettings, logoutFlow, expectationsLogout);

        // make sure that we really do NOT have access
        testAccessToAppAfterLogout(webClient, updatedTestSettings, false);

    }

    public void testAccessToAppAfterLogout(WebClient webClient, SAMLTestSettings settings, boolean spCookieStillGood) throws Exception {
        List<validationData> accessAppExpectations = vData.addSuccessStatusCodes();
        if (spCookieStillGood) {
            accessAppExpectations = vData.addExpectation(accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not get to the Snoop Servlet", null, SAMLConstants.APP1_TITLE);
        } else {
            if (flowType.equals(SAMLConstants.UNSOLICITED_SP_INITIATED)) {
                accessAppExpectations = vData.addExpectation(accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive request to process", null, SAMLConstants.IDP_CLIENT_JSP_TITLE);
            } else {
                accessAppExpectations = vData.addExpectation(accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive request to process", null, SAMLConstants.SAML_REQUEST);
            }
        }
        helpers.invokeAppSameConversation(_testName, webClient, null, settings, settings.getSpDefaultApp(), accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP);

    }
}
