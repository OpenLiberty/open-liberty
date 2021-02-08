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
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTestHelpers;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLConstants;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLTestSettings;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * The testcases in this class were ported from tWAS' test SamlWebSSOTests.
 * If a tWAS test is not applicable, it will be noted in the comments below.
 * If a tWAS test fits better into another test class, it will be noted
 * which test project/class it now resides in.
 * In general, these tests perform a simple IdP initiated SAML Web SSO, using
 * httpunit to simulate browser requests. In this scenario, a Web client
 * accesses a static Web page on IdP and obtains a a SAML HTTP-POST link
 * to an application installed on a WebSphere SP. When the Web client
 * invokes the SP application, it is redirected to a TFIM IdP which issues
 * a login challenge to the Web client. The Web Client fills in the login
 * form and after a successful login, receives a SAML 2.0 token from the
 * TFIM IdP. The client invokes the SP application by sending the SAML
 * 2.0 token in the HTTP POST request.
 */
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class TwoServerLogoutTests extends SAMLLogoutCommonTest {

    private static final Class<?> thisClass = TwoServerLogoutTests.class;
    protected static final String EmptySessionKeyMsg = "SPSession key cannot be null or empty";
    protected static final String LoadingSessionInformation = "Loading Session Information";
    protected static final String NothingToLogOut = "The logout operation is complete, and no other services appear to have been accessed during this session";

    static String[] sp1_list = null;
    static String[] twoServer_allUniqueSPCookies_list = null;
    static String[] twoServer_allDuplicateSPCookies_list = null;

    protected static String server1MasterConfig = "";
    protected static String server2MasterConfig = "";
    protected static String server1OtherConfig = "";
    protected static String server2OtherConfig = "";

    // initialize some common cookie lists
    public static void setCookieSettings(CookieType cookieType) throws Exception {

        String thisMethod = "setCookieSettings";
        msgUtils.printMethodName(thisMethod);

        cookieInfo = new CookieInfo(cookieType);

        // These 2 have to be set for use in some common tooling - set as appropriate for your tests
        allSPCookies_list = new String[] { cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName(), cookieInfo.getSp13CookieName() };
        idp_session_list = new String[] { SAMLConstants.IDP_SESSION_COOKIE_NAME };

        // These are defined and set for as appropriate for this test class
        twoServer_allUniqueSPCookies_list = new String[] { cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName(), cookieInfo.getServer2Sp1CookieName(), cookieInfo.getServer2Sp2CookieName() };
        twoServer_allDuplicateSPCookies_list = new String[] { cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName(), cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName() };
        sp1_list = new String[] { cookieInfo.getSp1CookieName() };
        //        sp13_list = new String[] { cookieInfo.getSp13CookieName() };

    }

    /**
     * Just a refresher:
     * We're having the extending class dictate what the flows will be, setting the logout endpoints and deternining if the logout
     * is local only
     * standardFlow - steps/actions to obtain a saml token and gain access
     * justLogout - steps/actions to perform Just a logout (this may be just performLogout, or it may be a series of steps/actions
     * loginLogoutFlow - combines standFlow and justLogout (basically logs ya in, logs ya out of one sp - end to end flow)
     *
     */

    //    public void test_multipleLoginLogouts(String loginType, String logoutType, boolean localOnly) throws Exception {
    //        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
    //        // login is SolicitedSP initiated, logout starts on the SP, logout involves the IDP
    //        setLogoutFlowSettings(updatedTestSettings, loginType, logoutType, localOnly);
    //        test_logout_with_multipleSPs_on_2Servers(updatedTestSettings);
    //
    //    }
    //
    //    public void test_tryToUseSPServer2CookieAfterLogout(String loginType, String logoutType, boolean localOnly) throws Exception {
    //        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
    //        // login is SolicitedSP initiated, logout starts on the SP, logout involves the IDP
    //        setLogoutFlowSettings(updatedTestSettings, loginType, logoutType, localOnly);
    //        test_usingCookieAfterLogout(updatedTestSettings);
    //
    //    }

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
    public void test_logout_with_multipleSPs_on_2Servers(String loginType, String logoutType, boolean localOnly) throws Exception {

        SAMLTestSettings settings = testSettings.copyTestSettings();
        // Set test settings for requested flow type
        setLogoutFlowSettings(settings, loginType, logoutType, localOnly);

        allSPCookies_list = twoServer_allUniqueSPCookies_list;
        String[] server1Cookies = { cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName() };
        String[] server2Cookies = { cookieInfo.getServer2Sp1CookieName(), cookieInfo.getServer2Sp2CookieName() };

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        // call helper method to log in to 4 different SPs (2 sp's on SP server1, and 2 on SP server2)
        loginTo4SPs(webClient, settings);

        //Logout sp 1
        SAMLTestSettings updatedTestSettings1 = settings.copyTestSettings();
        updatedTestSettings1.updatePartnerInSettings("sp1", true);
        List<validationData> expectationsLogout = setGoodSAMLLogoutExpectations(logoutFlow, updatedTestSettings1, 2);
        expectationsLogout = setCookieExpectationsFor2ServerFlow(expectationsLogout, logoutFlow, server1Cookies, server2Cookies, cookieInfo.getSp1CookieName());
        genericSAML(_testName, webClient, updatedTestSettings1, logoutFlow, expectationsLogout);

        /****************************************/
        /* Sanity check that we can log back in */
        /****************************************/
        // if the logout stayed local, we won't need to log into the idp again as we have the idp session cookie, but, if
        // the logout went through the IDP, we'll need to log in to the IDP
        String[] steps = null;
        if (updatedTestSettings1.getLocalLogoutOnly()) {
            steps = noLoginKeepingCookies;
        } else {
            steps = standardFlowUsingIDPCookieKeepingCookies;
        }

        // set expectations for login - including checks for cookies afer invokeacs...
        List<validationData> expectationsLogin = setDefaultGoodSAMLLoginExpectations(steps, updatedTestSettings1, sp1_list);

        // Log in again to show that login will work
        genericSAML(_testName, webClient, updatedTestSettings1, steps, expectationsLogin);
    }

    public void test_usingCookieAfterLogout(String loginType, String logoutType, boolean localOnly) throws Exception {

        SAMLTestSettings settings = testSettings.copyTestSettings();
        // Set test settings for requested flow type
        setLogoutFlowSettings(settings, loginType, logoutType, localOnly);
        allSPCookies_list = twoServer_allUniqueSPCookies_list;
        String[] server1Cookies = { cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName() };
        String[] server2Cookies = { cookieInfo.getServer2Sp1CookieName(), cookieInfo.getServer2Sp2CookieName() };

        String cookieName = cookieInfo.getServer2Sp2CookieName();
        String cookiePrintName = "server2_sp2";
        String spName = "server2_sp2";

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        // call helper method to log in to 4 different SPs (2 sp's on SP server1, and 2 on SP server2)
        loginTo4SPs(webClient, settings);

        // save a cookie from SP server2 (we're going to log out of an sp on server1)
        Cookie cookie = helpers.extractSpecificCookie(webClient, cookieName);
        if (cookie == null) {
            fail(cookiePrintName + " cookie is null and should not be");
        }
        Log.info(thisClass, _testName, "Extracted Cookie: " + cookie.getName() + " Value: " + cookie.getValue());

        SAMLTestSettings updatedTestSettingsServer2sp2 = settings.copyTestSettings();
        updatedTestSettingsServer2sp2.setDefaultSAMLServerTestSettings("http://localhost:" + testSAMLServer2.getServerHttpPort().toString(), "https://localhost:" + testSAMLServer2.getServerHttpsPort().toString());
        updatedTestSettingsServer2sp2 = updateContextRoot(updatedTestSettingsServer2sp2);
        updatedTestSettingsServer2sp2.updatePartnerInSettings(spName, true);
        updatedTestSettingsServer2sp2.setLocalLogoutOnly(settings.getLocalLogoutOnly()); // setDefaultSAMLServerTestSettings overrides the value set by the copy

        List<validationData> expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettingsServer2sp2);
        //        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES, SAMLConstants.COOKIES, SAMLConstants.STRING_CONTAINS, "Conversation did NOT have an SP Cookie.", cookieName, null);

        // create a new client and use the saved cookie to access the app (just to show that with just the cookie, we can access the app)
        WebClient webClient2 = SAMLCommonTestHelpers.getWebClient();
        webClient2.getCookieManager().addCookie(cookie);
        Log.info(thisClass, _testName, "Trying to access app with new client with " + cookiePrintName + " cookie added");
        helpers.invokeAppSameConversation(_testName, webClient2, null, updatedTestSettingsServer2sp2, updatedTestSettingsServer2sp2.getSpDefaultApp(), expectations, SAMLConstants.INVOKE_DEFAULT_APP);

        //Logout sp 1
        SAMLTestSettings updatedTestSettings1 = settings.copyTestSettings();
        updatedTestSettings1.updatePartnerInSettings("sp1", true);
        List<validationData> expectationsLogout = setGoodSAMLLogoutExpectations(logoutFlow, updatedTestSettings1, 2);
        expectationsLogout = setCookieExpectationsFor2ServerFlow(expectationsLogout, logoutFlow, server1Cookies, server2Cookies, cookieInfo.getSp1CookieName());
        genericSAML(_testName, webClient, updatedTestSettings1, logoutFlow, expectationsLogout);

        // Now, try to access the app using the original server2_sp2 cookie (depending on the logout type, the IDP session cookie may or may not still exist
        // add the SP cookie back into the webClient and try to access the protected app - the cookie should be recognized as invalid and we should need to login
        webClient.getCookieManager().addCookie(cookie);

        List<validationData> accessAppExpectations = vData.addSuccessStatusCodes();
        // expect to be forced to log in again as the cookie is no longer valid
        // add checks to make sure that we hit a login request path

        // add checks for cookie existance
        if (updatedTestSettingsServer2sp2.getLocalLogoutOnly()) {
            accessAppExpectations = addCookieExpectations(accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP, new String[] { SAMLConstants.IDP_SESSION_COOKIE_NAME, cookieInfo.getServer2Sp1CookieName(), cookieInfo.getServer2Sp2CookieName() }, new String[] { cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName() });
            accessAppExpectations = vData.addExpectation(accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on default app", null, SAMLConstants.APP1_TITLE);
        } else {
            accessAppExpectations = addCookieExpectations(accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP, null, new String[] { cookieName, SAMLConstants.IDP_SESSION_COOKIE_NAME });
            if (flowType.equals(SAMLConstants.UNSOLICITED_SP_INITIATED)) {
                accessAppExpectations = vData.addExpectation(accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive request to process", null, SAMLConstants.IDP_CLIENT_JSP_TITLE);
            } else {
                accessAppExpectations = vData.addExpectation(accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_FULL, SAMLConstants.STRING_CONTAINS, "Did not receive request to process", null, SAMLConstants.SAML_REQUEST);
            }
        }
        // individual methods don't record the expectations that were set (the common method that controls the flow is what normally logs this, but, we're bypassing that)
        msgUtils.printExpectations(accessAppExpectations);
        helpers.invokeAppSameConversation(_testName, webClient, null, updatedTestSettingsServer2sp2, updatedTestSettingsServer2sp2.getSpDefaultApp(), accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP);

    }

    /**** Helper methods ****/
    /**
     * This is a routine that will log into sp1, sp2 and sp13. It will make sure that we get access to our protected apps and that
     * we have the correct cookies:
     * WASSamlSP_sp1, WASSamlSP_sp2 and saml20_SP_sso
     *
     * @param webClient
     * @throws Exception
     */
    private void loginTo4SPs(WebClient webClient, SAMLTestSettings settings) throws Exception {

        String[] oneSPCookie = { cookieInfo.getSp1CookieName() };
        String[] twoSPCookie = { cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName() };
        String[] threeSPCookie = { cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName(), cookieInfo.getServer2Sp1CookieName() };

        //Log in to SP1  - will have sp1 cookie after acs
        loginToSP(webClient, settings, standardFlowKeepingCookies, testUsers.getUser1(), testUsers.getPassword1(), "sp1", oneSPCookie);

        //Log in to SP2 using IDP session cookie - will have sp1 and sp2 cookies after acs
        String[] flow = setFlowForAnotherLogin();
        loginToSP(webClient, settings, flow, testUsers.getUser2(), testUsers.getPassword2(), "sp2", twoSPCookie);

        //Log in to server2_sp1 using IDP session cookie - will have sp1, sp2 and server2_sp1 cookies after acs
        loginToSP(webClient, settings, flow, testUsers.getUser3(), testUsers.getPassword3(), "server2_sp1", threeSPCookie, true);

        //Log in to server2_sp2 using IDP session cookie - will have sp1, sp2, server2_sp1 and server2_sp2 cookies after acs
        loginToSP(webClient, settings, flow, testUsers.getUser4(), testUsers.getPassword4(), "server2_sp2", twoServer_allUniqueSPCookies_list, true);

    }

    private void loginTo2SPsOn2Servers(WebClient webClient, SAMLTestSettings settings) throws Exception {

        String[] oneSPCookie = { cookieInfo.getSp1CookieName() };
        String[] twoSPCookie = { cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName() };
        String[] threeSPCookie = { cookieInfo.getSp1CookieName(), cookieInfo.getSp2CookieName(), cookieInfo.getSp1CookieName() };

        //Log in to SP1  - will have sp1 cookie after acs
        loginToSP(webClient, settings, standardFlowKeepingCookies, testUsers.getUser1(), testUsers.getPassword1(), "sp1", oneSPCookie);

        //Log in to SP2 using IDP session cookie - will have sp1 and sp2 cookies after acs
        String[] flow = setFlowForAnotherLogin();
        loginToSP(webClient, settings, flow, testUsers.getUser2(), testUsers.getPassword2(), "sp2", twoSPCookie);

        //Log in to server2_sp1 using IDP session cookie - will have sp1, sp2 and server2_sp1 cookies after acs
        loginToSP(webClient, settings, flow, testUsers.getUser3(), testUsers.getPassword3(), "sp1", threeSPCookie, true);

        //Log in to server2_sp2 using IDP session cookie - will have sp1, sp2, server2_sp1 and server2_sp2 cookies after acs
        loginToSP(webClient, settings, flow, testUsers.getUser4(), testUsers.getPassword4(), "sp2", twoServer_allDuplicateSPCookies_list, true);

    }

    public void reconfigServers(String server1Config, String server2Config) throws Exception {
        if (server1Config != null) {
            testSAMLServer.reconfigServer(server1Config, _testName, null, SAMLConstants.JUNIT_REPORTING);
        }
        if (server2Config != null) {
            testSAMLServer2.reconfigServer(server2Config, _testName, null, SAMLConstants.JUNIT_REPORTING);
        }

    }

}
