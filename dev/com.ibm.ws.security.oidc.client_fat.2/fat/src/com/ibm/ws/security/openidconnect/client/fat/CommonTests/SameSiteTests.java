/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.openidconnect.client.fat.CommonTests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.CommonTest;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.MessageConstants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.SameSiteTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestSettings;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.structures.SameSiteTestExpectations;

import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@LibertyServerWrapper
@Mode(TestMode.FULL)
public class SameSiteTests extends CommonTest {

    public static Class<?> thisClass = SameSiteTests.class;

    public static SameSiteTestTools samesiteTestTools = null;

    public List<validationData> setGoodExpectations(TestSettings settings, String inSubTestPrefix) throws Exception {

        String subTestPrefix = "(" + inSubTestPrefix + "): ";
        List<validationData> expectations = new ArrayList<validationData>();

        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_TITLE,
                Constants.STRING_CONTAINS, subTestPrefix + "Did Not get the OpenID Connect login page.", null,
                Constants.LOGIN_TITLE);
        expectations = vData.addExpectation(expectations, Constants.GET_LOGIN_PAGE, Constants.RESPONSE_FULL,
                Constants.STRING_CONTAINS, subTestPrefix + "Did Not get the OpenID Connect login page.", null,
                Constants.LOGIN_PROMPT);
        expectations = validationTools.addRequestParmsExpectations(expectations, _testName, Constants.LOGIN_USER,
                settings);
        if (settings.getRsTokenType().equals(Constants.ACCESS_TOKEN_KEY)) {
            expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL,
                    Constants.STRING_CONTAINS, subTestPrefix + "Did not see the access_token printed in the app output",
                    null, "access_token=");
        } else {
            expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL,
                    Constants.STRING_MATCHES, subTestPrefix + "Did not see the JWT Token printed in the app output",
                    null, Constants.JWT_STR_START + ".*\"iss\":");
        }
        if (settings.getRequestParms() != null && settings.getWhere().contains(Constants.PARM)) {
            String testApp = settings.getRequestParms().get("targetApp"); // if we're trying to get the app name for
                                                                          // verification, we should have a test app
                                                                          // set...
            expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL,
                    Constants.STRING_CONTAINS, subTestPrefix + "Did not go to the correct app", null,
                    "Param: targetApp with value: " + testApp);
        }
        expectations = validationTools.addIdTokenStringValidation(vData, expectations, Constants.LOGIN_USER,
                Constants.RESPONSE_FULL, Constants.IDToken_STR);
        return expectations;

    }

    public List<validationData> badExpectations(TestServer server, String inSubTestPrefix) throws Exception {

        String subTestPrefix = "(" + inSubTestPrefix + "): ";
        List<validationData> expectations = null;

        expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_TITLE,
                Constants.STRING_CONTAINS,
                subTestPrefix + "Did not receive another redirect to login again as the cookie is dropped - Title",
                null, "Redirect To OP");
        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL,
                Constants.STRING_CONTAINS,
                subTestPrefix + "Did not receive another redirect to login again as the cookie is dropped", null,
                "client01");

        return expectations;
    }

    public List<validationData> badRedirectExpectations(TestServer server, String inSubTestPrefix) throws Exception {

        String subTestPrefix = "(" + inSubTestPrefix + "): ";
        List<validationData> expectations = null;

        expectations = vData.addExpectation(expectations, Constants.LOGIN_USER, Constants.RESPONSE_FULL,
                Constants.STRING_CONTAINS, subTestPrefix + "Did not receive error message "
                        + MessageConstants.CWOAU0073E_FRONT_END_ERROR + " in the response",
                null, MessageConstants.CWOAU0073E_FRONT_END_ERROR);
        expectations = validationTools.addMessageExpectation(server, expectations, Constants.LOGIN_USER,
                Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                subTestPrefix + "Server log did not contain an error message about the missing WASReqURLOidc cookie.",
                MessageConstants.CWWKS1520E_MISSING_SAMESITE_COOKIE);

        return expectations;

    }

    public void mainPathTest(SameSiteTestExpectations.TestServerExpectations testExpectation, TestSettings settings,
            String subTestPrefix) throws Exception {

        WebClient webClient = getAndSaveWebClient(true);

        Log.info(thisClass, "main", "redirect enabled: " + webClient.getOptions().isRedirectEnabled());

        msgUtils.printMethodName(_testName + " Step/Sub-Test " + subTestPrefix, "Starting TEST ");
        samesiteTestTools.logStepInServerSideLogs("STARTING", _testName + " Step/Sub-Test " + subTestPrefix);

        samesiteTestTools.backupConfig(_testName, "SameSiteTests_", testOPServer, subTestPrefix);
        samesiteTestTools.backupConfig(_testName, "SameSiteTests_", testRPServer, subTestPrefix);

        List<validationData> expectations = null;
        switch (testExpectation) {
        case ALL_SERVERS_SUCCEED:
            expectations = setGoodExpectations(settings, subTestPrefix);
            break;
        case OP_GENERIC_FAILURE:
            expectations = badExpectations(testOPServer, subTestPrefix);
            break;
        case RP_GENERIC_FAILURE:
            expectations = badExpectations(testRPServer, subTestPrefix);
            break;
        case RP_REDIRECT_FAILURE:
            expectations = badRedirectExpectations(testRPServer, subTestPrefix);
            break;
        default:
            expectations = setGoodExpectations(settings, subTestPrefix);
            break;
        }

        genericRP(_testName, webClient, settings, Constants.GOOD_OIDC_LOGIN_ACTIONS_SKIP_CONSENT, expectations);

        msgUtils.printMethodName(_testName + " Step/Sub-Test " + subTestPrefix, "Ending TEST ");
        samesiteTestTools.logStepInServerSideLogs("ENDING", _testName + " Step/Sub-Test " + subTestPrefix);

    }

    public void runVariations(Map<String, String> opSettings, Map<String, String> rpSettings) throws Exception {
        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations();
        runVariations(testExpectations, opSettings, rpSettings);
    }

    // run variations (caller will specify which server may have problems on a specific variation)
    public void runVariations(SameSiteTestExpectations testExpectations, Map<String, String> inOPSettings,
            Map<String, String> inRPSettings) throws Exception {

        Map<String, String> opSettings = samesiteTestTools.createOrRestoreConfigSettings(inOPSettings);
        Map<String, String> rpSettings = samesiteTestTools.createOrRestoreConfigSettings(inRPSettings);

        mainPathTest(testExpectations.getBaseTestResult(), testSettings, "Base Tests");

        /*********************************************************/
        /* test using http with the request to the app on the RP */
        /*********************************************************/
        // NOTE: we are updating the actual "TestSettings" with a different url
        // only this test is using the updated settings (not updatedTestSettings passed in call to mainPathTest)
        TestSettings updatedTestSettings = testSettings.copyTestSettings();
        updatedTestSettings.setTestURL(
                testRPServer.getServerHttpString() + "/" + Constants.OPENID_APP + "/" + Constants.DEFAULT_SERVLET);
        mainPathTest(testExpectations.getHttpRPAppUrlTestResult(), updatedTestSettings, "Http RP App request");
        // No server config's were updated in the previous test, so, nothing has to be restored

        /*********************************************************/
        /*
         * test with the RP using http redirect redirect always requires https
         */
        /*********************************************************/
        rpSettings.put(SameSiteTestTools.RedirectHostKey, testRPServer.getServerHttpString());
        samesiteTestTools.updateServerSettings(testRPServer, rpSettings);
        mainPathTest(testExpectations.getHttpRedirectUrlTestResult(), testSettings, "Http RP Redirect");

        /*********************************************************/
        /*
         * test with RP using http authorization endpoint authorization endpoint always requires https
         */
        /*********************************************************/
        opSettings = samesiteTestTools.createOrRestoreConfigSettings(inOPSettings);
        rpSettings = samesiteTestTools.createOrRestoreConfigSettings(inRPSettings);
        rpSettings.put(SameSiteTestTools.AuthorizationHostKey, testOPServer.getServerHttpString());
        samesiteTestTools.updateServerSettings(testOPServer, opSettings);
        samesiteTestTools.updateServerSettings(testRPServer, rpSettings);
        mainPathTest(testExpectations.getHttpAuthEndpointUrlTestResult(), testSettings,
                "Http RP Authorization Endpoint");

        /*********************************************************/
        /*
         * test with RP using http token endpoint token endpoint always requires https
         */
        /*********************************************************/
        opSettings = samesiteTestTools.createOrRestoreConfigSettings(inOPSettings);
        rpSettings = samesiteTestTools.createOrRestoreConfigSettings(inRPSettings);
        rpSettings.put(SameSiteTestTools.TokenHostKey, testOPServer.getServerHttpString());
        samesiteTestTools.updateServerSettings(testOPServer, opSettings);
        samesiteTestTools.updateServerSettings(testRPServer, rpSettings);
        mainPathTest(testExpectations.getHttpTokenEndpointUrlTestResult(), testSettings, "Http RP Token Endpoint");

    }

    @Mode(TestMode.LITE)
    @Test
    public void SameSiteTests_OPdisabledRPdisabled() throws Exception {

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(); // use defaults
        Map<String, String> rpSettings = samesiteTestTools.setRPConfigSettings(); // use defaults

        runVariations(opSettings, rpSettings);
    }

    @Mode(TestMode.LITE)
    @Test
    public void SameSiteTests_OPdisabledRPdisabled_ssoRequiresSSLTrue() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SsoRequiresSSL, "true");
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SsoRequiresSSL, "true");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations();
        testExpectations.setHttpRPAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_GENERIC_FAILURE);
        testExpectations
                .setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, rpSettings);

    }

    @Test
    public void SameSiteTests_OPdisabledRPlax() throws Exception {

        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_LAX);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings();
        Map<String, String> rpSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        runVariations(opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPdisabledRPnone() throws Exception {

        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_NONE);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings();
        Map<String, String> rpSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations();
        testExpectations.setHttpRPAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_GENERIC_FAILURE);
        testExpectations
                .setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPdisabledRPstrict() throws Exception {

        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_STRICT);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings();
        Map<String, String> rpSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        runVariations(opSettings, rpSettings);
    }

    /*****/
    /*****/

    @Test
    public void SameSiteTests_OPlaxRPdisabled() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_LAX);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setRPConfigSettings();

        runVariations(opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPlaxRPlax() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_LAX);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_LAX);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        runVariations(opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPlaxRPnone() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_LAX);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_NONE);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations();
        testExpectations.setHttpRPAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_GENERIC_FAILURE);
        testExpectations
                .setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPlaxRPstrict() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_LAX);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_STRICT);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        runVariations(opSettings, rpSettings);
    }

    /*****/
    /*****/

    @Test
    public void SameSiteTests_OPnoneRPdisabled() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_NONE);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setRPConfigSettings();

        runVariations(opSettings, rpSettings);

    }

    @Test
    public void SameSiteTests_OPnoneRPlax() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_NONE);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_LAX);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        runVariations(opSettings, rpSettings);
    }

    @Mode(TestMode.LITE)
    @Test
    public void SameSiteTests_OPnoneRPnone() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_NONE);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_NONE);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations();
        testExpectations.setHttpRPAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_GENERIC_FAILURE);
        testExpectations
                .setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, rpSettings);
    }

    @Mode(TestMode.LITE)
    @Test
    public void SameSiteTests_OPnoneRPnone_ssoRequiresSSLTrue() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_NONE);
        opUpdates.put(SameSiteTestTools.SsoRequiresSSL, "true");
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_NONE);
        rpUpdates.put(SameSiteTestTools.SsoRequiresSSL, "true");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations();
        testExpectations.setHttpRPAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_GENERIC_FAILURE);
        testExpectations
                .setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, rpSettings);

    }

    @Test
    public void SameSiteTests_OPnoneRPstrict() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_NONE);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_STRICT);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        runVariations(opSettings, rpSettings);
    }

    /*****/
    /*****/

    @Test
    public void SameSiteTests_OPstrictRPdisabled() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_STRICT);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setRPConfigSettings();

        runVariations(opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPstrictRPlax() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_STRICT);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_LAX);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        runVariations(opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPstrictRPnone() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_STRICT);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_NONE);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations();
        testExpectations.setHttpRPAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_GENERIC_FAILURE);
        testExpectations
                .setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPstrictRPstrict() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_STRICT);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, Constants.SAMESITE_STRICT);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        runVariations(opSettings, rpSettings);
    }

}
