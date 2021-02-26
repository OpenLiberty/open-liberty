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

import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
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
public class OneServerLTPALogoutTests extends SAMLLogoutCommonTest {

    private static final Class<?> thisClass = OneServerLTPALogoutTests.class;
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
     * We're having the extending class dictate what the flows will be, setting the logout endpoints and deternining if the logout
     * is local only
     * standardFlow - steps/actions to obtain a saml token and gain access
     * justLogout - steps/actions to perform Just a logout (this may be just performLogout, or it may be a series of steps/actions
     * loginLogoutFlow - combines standFlow and justLogout (basically logs ya in, logs ya out of one sp - end to end flow)
     *
     */

    /**
     *
     * The test verifies the correct behavior for logging out
     * Simply log in to 1 sp and then log out
     * 1) using httpServletRequest - do NOT invoke SSO/IDP logout
     * only 1 logout step - the LTPA cookie should be deleted, but the idp session cookie should still exist
     * 2) using httpServletRequest - DO invoke IDP logout
     * multiple logout steps - the LTPA and IDP Session cookies should be deleted
     * 3) using new SP logout endpoint - invokes IDP Logout
     * multiple logout steps - the LTPA and IDP Session cookies should be deleted
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfUsingSPCookie.class)
    @Test
    public void OneServerLTPALogoutTests_mainPath() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp13", true);

        List<validationData> expectations = setDefaultGoodSAMLLoginExpectations(loginLogoutFlow, updatedTestSettings, sp13_list);
        expectations = setGoodSAMLLogoutExpectations(expectations, loginLogoutFlow, updatedTestSettings, 1);
        expectations = setCookieExpectationsForFlow(expectations, loginLogoutFlow, LoggedInToOneSP, sp13_list, cookieInfo.getSp13CookieName());

        genericSAML(_testName, webClient, updatedTestSettings, loginLogoutFlow, expectations);
    }

    /**
     * Log in to an SP that uses the LTPA cookie
     * Save the LTPA cookie
     * Log out
     * Put the cookie back into the "conversation"
     * Try to access the app
     * Make sure that we do not have access, we should be prompted to log in again (login "flow" will be different if instance did
     * a local vs IDP logout)
     *
     * @throws Exception
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfUsingSPCookie.class)
    @Test
    public void OneServerLTPALogoutTests_useCookie_afterLogout() throws Exception {

        //        useCookieAfterLogout(cookieInfo.getSp13CookieName(), "LTPA", "sp13");
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.updatePartnerInSettings("sp13", true);
        List<validationData> expectations = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings);
        expectations = vData.addExpectation(expectations, SAMLConstants.INVOKE_ACS_WITH_SAML_RESPONSE_KEEPING_COOKIES, SAMLConstants.COOKIES, SAMLConstants.STRING_CONTAINS, "Conversation did NOT have an SP Cookie.", cookieInfo.getSp13CookieName(), null);
        genericSAML(_testName, webClient, updatedTestSettings, standardFlowKeepingCookies, expectations);

        // Save the SP cookie
        Cookie cookie = helpers.extractSpecificCookie(webClient, cookieInfo.getSp13CookieName());
        if (cookie == null) {
            fail("SP13" + " cookie is null and should not be");
        }
        Log.info(thisClass, _testName, "Extracted Cookie: " + cookie.getName() + " Value: " + cookie.getValue());

        // create a new client and use the sp1 cookie to access the app (just to show that with just the cookie, we can access the app)
        WebClient webClient2 = SAMLCommonTestHelpers.getWebClient();
        webClient2.getCookieManager().addCookie(cookie);
        Log.info(thisClass, _testName, "Trying to access app with new client with " + "SP13" + " cookie added");
        helpers.invokeAppSameConversation(_testName, webClient2, null, updatedTestSettings, updatedTestSettings.getSpDefaultApp(), expectations, SAMLConstants.INVOKE_DEFAULT_APP);

        // now, logout
        List<validationData> expectationsLogout = setGoodSAMLLogoutExpectations(logoutFlow, updatedTestSettings, 1);
        // if we're logging out locally only, the idp session cookie will exist, if we're going to the IDP to logout, it will NOT
        if (updatedTestSettings.getLocalLogoutOnly()) {
            expectationsLogout = addCookieExpectations(expectationsLogout, SAMLConstants.INVOKE_DEFAULT_APP, new String[] { SAMLConstants.IDP_SESSION_COOKIE_NAME }, new String[] { cookieInfo.getSp13CookieName() });
        } else {
            expectationsLogout = addCookieExpectations(expectationsLogout, SAMLConstants.INVOKE_DEFAULT_APP, null, new String[] { cookieInfo.getSp13CookieName(), SAMLConstants.IDP_SESSION_COOKIE_NAME });
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
            accessAppExpectations = addCookieExpectations(accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP, new String[] { SAMLConstants.IDP_SESSION_COOKIE_NAME }, new String[] { cookieInfo.getSp13CookieName() });
        } else {
            accessAppExpectations = addCookieExpectations(accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP, null, new String[] { cookieInfo.getSp13CookieName(), SAMLConstants.IDP_SESSION_COOKIE_NAME });
        }

        helpers.invokeAppSameConversation(_testName, webClient, null, updatedTestSettings, updatedTestSettings.getSpDefaultApp(), accessAppExpectations, SAMLConstants.INVOKE_DEFAULT_APP);

    }

}
