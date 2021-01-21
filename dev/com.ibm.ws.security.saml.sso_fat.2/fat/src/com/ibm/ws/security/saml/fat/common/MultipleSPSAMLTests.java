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
package com.ibm.ws.security.saml.fat.common;

import java.util.List;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.CookieManager;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.util.Cookie;
import com.ibm.ws.security.fat.common.ValidationData.validationData;
import com.ibm.ws.security.saml20.fat.commonTest.SAMLCommonTest;
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
public class MultipleSPSAMLTests extends SAMLCommonTest {

    private static final Class<?> thisClass = MultipleSPSAMLTests.class;
    public static String runType;

    public static String setRunType() throws Exception {
        if (flowType.equals(SAMLConstants.IDP_INITIATED)) {
            return SAMLConstants.BUILD_POST_IDP_INITIATED_REQUEST;
        }
        if (flowType.equals(SAMLConstants.SOLICITED_SP_INITIATED)) {
            return SAMLConstants.BUILD_POST_SP_INITIATED_REQUEST;
        }
        if (flowType.equals(SAMLConstants.UNSOLICITED_SP_INITIATED)) {
            return "TBD";
        }
        return "NOTSET";
    }

    @Mode(TestMode.LITE)
    @Test
    public void multipleSPSAMLTests_failToUseAccessFromAnotherSP() throws Exception {

        //	testSAMLServer.reconfigServer(buildSPServerName("server_2Providers.xml"), _testName, Constants.NO_EXTRA_MSGS, Constants.JUNIT_REPORTING);

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings1 = testSettings.copyTestSettings();
        updatedTestSettings1.updatePartnerInSettings("sp1", true);
        updatedTestSettings1.setSpDefaultApp(updatedTestSettings1.getSpDefaultApp().replace("sp1", "sp2"));

        List<validationData> expectations1 = helpers.setDefaultGoodSAMLExpectationsThroughACSOnly(runType, updatedTestSettings1, null);
        expectations1 = vData.addExpectation(expectations1, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on the login page", null, cttools.getLoginTitle(updatedTestSettings1.getIdpRoot()));

        genericSAML(_testName, webClient, updatedTestSettings1, standardFlowDefAppAgain, expectations1);

    }

    /**
     * Access first app and try to use SP token to access an app on another SP and it should fail.
     * Access a second app - conversation still has the token from the first access. Try to invoke
     * the first app - this should work because both tokens are in the conversation
     *
     * @throws Exception
     */
    //	@Mode(TestMode.LITE)
    @Test
    public void multipleSPSAMLTests_accessMultipleAppsInSameConversation() throws Exception {

        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings1 = testSettings.copyTestSettings();
        updatedTestSettings1.updatePartnerInSettings("sp1", true);
        updatedTestSettings1.setSpDefaultApp(updatedTestSettings1.getSpDefaultApp().replace("sp1", "sp2"));

        List<validationData> expectations1 = helpers.setDefaultGoodSAMLExpectationsThroughACSOnly(runType, updatedTestSettings1, null);
        expectations1 = vData.addExpectation(expectations1, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on default app", null, cttools.getLoginTitle(updatedTestSettings1.getIdpRoot()));

        genericSAML(_testName, webClient, updatedTestSettings1, standardFlowDefAppAgainKeepingCookies, expectations1);

        msgUtils.printMarker(_testName, "Before using webClient2");
        // now, remove the ltpaToken2 cookie from the conversation (if we leave it in the conversation, the IDP will just return our original SAML Token -
        //   which if used should result in a replay attack

        WebClient webClient2 = SAMLCommonTestHelpers.getWebClient();
        CookieManager cm1 = webClient.getCookieManager();
        Cookie cookie = cm1.getCookie("WASSamlSP_myTestName1");
        CookieManager cm2 = webClient2.getCookieManager();
        cm2.addCookie(cookie);

        msgUtils.printAllCookies(webClient2);

        SAMLTestSettings updatedTestSettings2 = testSettings.copyTestSettings();
        updatedTestSettings2.updatePartnerInSettings("sp2", true);
        updatedTestSettings2.setSpDefaultApp(updatedTestSettings1.getSpDefaultApp().replace("sp2", "sp1"));

        List<validationData> expectations2 = helpers.setDefaultGoodSAMLExpectations(flowType, updatedTestSettings2);

        genericSAML(_testName, webClient2, updatedTestSettings2, standardFlowDefAppAgainKeepingCookies, expectations2);

    }

    /**
     * Access first app and try to use SP token to access an app on another SP and it should fail.
     * Access an app using an ltpa token instead of an sp token, after that, the conversation has
     * both an ltpa and sp token for sp1. Try to access another app that would be using an ltpa
     * token and that should work. Finally, try to access yet another app using an sp token
     * for sp2 (never accessed yet) and that WILL not be accessable
     * This test should cover apps being protected by either sp or ltpa tokens - and attempts
     * made with and without the proper token in the conversation - the conversation will have
     * OTHER valid token (just not the correct ones for the app that's being called)
     *
     * @throws Exception
     */
    //	@Mode(TestMode.LITE)
    @Test
    public void multipleSPSAMLTests_accessMultipleApps_SPTokenAndLtpaToken() throws Exception {

        //	testSAMLServer.reconfigServer(buildSPServerName("server_2Providers.xml"), _testName, Constants.NO_EXTRA_MSGS, Constants.JUNIT_REPORTING);

        // Create the conversation object which will maintain state for us
        WebClient webClient = SAMLCommonTestHelpers.getWebClient();

        SAMLTestSettings updatedTestSettings1 = testSettings.copyTestSettings();
        updatedTestSettings1.updatePartnerInSettings("sp1", true);
        updatedTestSettings1.setSpDefaultApp(updatedTestSettings1.getSpDefaultApp().replace("sp1", "sp13"));

        List<validationData> expectations1 = helpers.setDefaultGoodSAMLExpectationsThroughACSOnly(runType, updatedTestSettings1, null);
        expectations1 = vData.addExpectation(expectations1, SAMLConstants.INVOKE_DEFAULT_APP, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on default app", null, cttools.getLoginTitle(updatedTestSettings1.getIdpRoot()));

        genericSAML(_testName, webClient, updatedTestSettings1, standardFlowDefAppAgainKeepingCookies, expectations1);

        // now, remove the ltpaToken2 cookie from the conversation (if we leave it in the conversation, the IDP will just return our original SAML Token -
        //   which if used should result in a replay attack
        WebClient webClient2 = SAMLCommonTestHelpers.getWebClient();
        CookieManager cm1 = webClient.getCookieManager();
        Cookie cookie = cm1.getCookie("WASSamlSP_myTestName1");
        Cookie cookie2 = cm1.getCookie("saml20_SP_sso");
        CookieManager cm2 = webClient2.getCookieManager();
        cm2.addCookie(cookie);
        if (cookie2 != null) {
            cm2.addCookie(cookie2);
        }

        msgUtils.printAllCookies(webClient2);

        SAMLTestSettings updatedTestSettings2 = testSettings.copyTestSettings();
        updatedTestSettings2.updatePartnerInSettings("sp13", true);
        updatedTestSettings2.setSpDefaultApp(updatedTestSettings2.getSpDefaultApp().replace("sp13", "sp5"));
        updatedTestSettings2.setSpAlternateApp(updatedTestSettings2.getSpAlternateApp().replace("sp13", "sp2"));
        updatedTestSettings2.setAccessTokenType(SAMLConstants.SP_AND_LTPA_ACCESS_TOKEN_TYPE);

        List<validationData> expectations2 = helpers.setDefaultGoodSAMLExpectationsThroughACSOnly(runType, updatedTestSettings2, null);
        expectations2 = vData.addExpectation(expectations2, SAMLConstants.INVOKE_ALTERNATE_APP, SAMLConstants.RESPONSE_TITLE, SAMLConstants.STRING_CONTAINS, "Did not land on TFMI Login page", null, cttools.getLoginTitle(updatedTestSettings2.getIdpRoot()));

        genericSAML(_testName, webClient2, updatedTestSettings2, standardFlowExtendedKeepingCookies, expectations2);

    }
}
