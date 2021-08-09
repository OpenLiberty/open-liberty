/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.social.fat.LibertyOP.CommonTests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.SameSiteTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.TestServer;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.structures.SameSiteTestExpectations;
import com.ibm.ws.security.social.fat.utils.SocialCommonTest;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialMessageConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class SameSiteTests extends SocialCommonTest {

    public static Class<?> thisClass = SameSiteTests.class;

    public static RSCommonTestTools rsTools = new RSCommonTestTools();
    public static SameSiteTestTools samesiteTestTools = null;
    public static String socialProviderName = null;

    public List<validationData> badExpectations(TestServer server, String inSubTestPrefix) throws Exception {

        String subTestPrefix = "(" + inSubTestPrefix + "): ";
        List<validationData> expectations = null;

        expectations = vData.addSuccessStatusCodes();
        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_TITLE, SocialConstants.STRING_CONTAINS, subTestPrefix + "Did not receive another redirect to login again as the cookie is dropped - Title", null, "Redirect To OP");
        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, subTestPrefix + "Did not receive another redirect to login again as the cookie is dropped", null, socialProviderName);

        return expectations;
    }

    public List<validationData> badRedirectExpectations(TestServer server, String inSubTestPrefix) throws Exception {

        String subTestPrefix = "(" + inSubTestPrefix + "): ";
        List<validationData> expectations = null;

        expectations = vData.addSuccessStatusCodesForActions(SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, inovke_social_login_actions);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, subTestPrefix + "Did not receive Unauthorized in the message.", null, SocialConstants.UNAUTHORIZED_MESSAGE);
        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, subTestPrefix + "Did not receive the " + SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED + " message in the response", null, SocialMessageConstants.CWWKS5489E_SOCIAL_LOGIN_FAILED);
        expectations = validationTools.addMessageExpectation(server, expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, subTestPrefix + "Server log did not contain an error message about the missing state.", SocialMessageConstants.CWWKS5480E_MISSING_STATE);

        return expectations;
    }

    public List<validationData> badBrowserOnlyExpectations(String inSubTestPrefix) throws Exception {

        String subTestPrefix = "(" + inSubTestPrefix + "): ";
        List<validationData> expectations = new ArrayList<validationData>();
        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_FULL, SocialConstants.STRING_CONTAINS, subTestPrefix + "Did not receive error message " + SocialMessageConstants.CWOAU0073E_FRONT_END_ERROR + " in the response", null, SocialMessageConstants.CWOAU0073E_FRONT_END_ERROR);

        return expectations;
    }

    public void mainPathTest(SameSiteTestExpectations.TestServerExpectations testExpectation, SocialTestSettings settings, String subTestPrefix) throws Exception {

        WebClient webClient = getAndSaveWebClient();

        Log.info(thisClass, "main", "redirect enabled: " + webClient.getOptions().isRedirectEnabled());

        msgUtils.printMethodName(_testName + " Step/Sub-Test " + subTestPrefix, "Starting TEST ");
        samesiteTestTools.logStepInServerSideLogs("STARTING", _testName + " Step/Sub-Test " + subTestPrefix);

        samesiteTestTools.backupConfig(_testName, "SameSiteTests_", testOPServer, subTestPrefix);
        samesiteTestTools.backupConfig(_testName, "SameSiteTests_", genericTestServer, subTestPrefix);

        List<validationData> expectations = null;
        switch (testExpectation) {
        case ALL_SERVERS_SUCCEED:
            expectations = setGoodSocialExpectations(settings, doNotAddJWTTokenValidation);
            break;
        case OP_GENERIC_FAILURE:
            expectations = badExpectations(testOPServer, subTestPrefix);
            break;
        case RP_GENERIC_FAILURE:
            expectations = badExpectations(genericTestServer, subTestPrefix);
            break;
        case RP_REDIRECT_FAILURE:
            expectations = badRedirectExpectations(genericTestServer, subTestPrefix);
            break;
        // some failures result in only messages on the browser
        // this case covers those
        case ONLY_BROWSER_SHOWS_FAILURE:
            expectations = badBrowserOnlyExpectations(subTestPrefix);
            break;
        default:
            expectations = setGoodSocialExpectations(settings, doNotAddJWTTokenValidation);
            break;
        }

        genericSocial(_testName, webClient, inovke_social_login_actions, settings, expectations);

        msgUtils.printMethodName(_testName + " Step/Sub-Test " + subTestPrefix, "Ending TEST ");
        samesiteTestTools.logStepInServerSideLogs("ENDING", _testName + " Step/Sub-Test " + subTestPrefix);

    }

    public void runVariations(Map<String, String> opSettings, Map<String, String> socialRPSettings) throws Exception {
        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations();
        runVariations(testExpectations, opSettings, socialRPSettings);
    }

    // run variations (caller will specify which server may have problems on a specific variation)
    public void runVariations(SameSiteTestExpectations testExpectations, Map<String, String> inOPSettings, Map<String, String> inSocialRPSettings) throws Exception {

        Map<String, String> opSettings = samesiteTestTools.createOrRestoreConfigSettings(inOPSettings);
        Map<String, String> socialRPSettings = samesiteTestTools.createOrRestoreConfigSettings(inSocialRPSettings);

        SocialTestSettings updatedTestSettings = socialSettings.copyTestSettings();

        mainPathTest(testExpectations.getBaseTestResult(), socialSettings, "Base Tests");

        /*********************************************************/
        /* test using http with the request to the app on the RP */
        /*********************************************************/
        // NOTE:  we are updating the actual "TestSettings" with a different url
        // only this test is using the updated settings (not updatedTestSettings passed in call to mainPathTest)
        updatedTestSettings.setProtectedResource(genericTestServer.getServerHttpString() + "/helloworld/rest/helloworld");
        mainPathTest(testExpectations.getHttpRPAppUrlTestResult(), updatedTestSettings, "Http Social App request");
        // No server config's were updated in the previous test, so, nothing has to be restored

        // oidc enforces https use with all of the endpoints and that check will occur before we would encounter
        // different behavior for samesite, so, we'll only test with oauth
        if (socialProviderName.contains("oauth2Login1")) {

            /*********************************************************/
            /*
             * test with the RP using http redirect
             * redirect always requires https
             */
            /*********************************************************/
            socialRPSettings.put(SameSiteTestTools.RedirectHostKey, genericTestServer.getServerHttpString());
            samesiteTestTools.updateServerSettings(genericTestServer, socialRPSettings);
            mainPathTest(testExpectations.getHttpRedirectUrlTestResult(), socialSettings, "Http Social Redirect");

            /*********************************************************/
            /*
             * test with social using http authorization endpoint
             * authorization endpoint always requires https
             */
            /*********************************************************/
            opSettings = samesiteTestTools.createOrRestoreConfigSettings(inOPSettings);
            socialRPSettings = samesiteTestTools.createOrRestoreConfigSettings(inSocialRPSettings);
            socialRPSettings.put(SameSiteTestTools.AuthorizationHostKey, testOPServer.getServerHttpString());
            samesiteTestTools.updateServerSettings(testOPServer, opSettings);
            samesiteTestTools.updateServerSettings(genericTestServer, socialRPSettings);
            mainPathTest(testExpectations.getHttpAuthEndpointUrlTestResult(), socialSettings, "Http Social Authorization Endpoint");

            /*********************************************************/
            /*
             * test with social using http token endpoint
             * token endpoint always requires https
             */
            /*********************************************************/
            opSettings = samesiteTestTools.createOrRestoreConfigSettings(inOPSettings);
            socialRPSettings = samesiteTestTools.createOrRestoreConfigSettings(inSocialRPSettings);
            socialRPSettings.put(SameSiteTestTools.TokenHostKey, testOPServer.getServerHttpString());
            samesiteTestTools.updateServerSettings(testOPServer, opSettings);
            samesiteTestTools.updateServerSettings(genericTestServer, socialRPSettings);
            mainPathTest(testExpectations.getHttpTokenEndpointUrlTestResult(), socialSettings, "Http Social Token Endpoint");
        }
    }

    @Mode(TestMode.LITE)
    @Test
    public void SameSiteTests_OPdisabledSOCIALdisabled() throws Exception {

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(); // use defaults
        Map<String, String> socialRPSettings = samesiteTestTools.setRPConfigSettings(); // use defaults

        runVariations(opSettings, socialRPSettings);
    }

    @Mode(TestMode.LITE)
    @Test
    public void SameSiteTests_OPdisabledSOCIALdisabled_ssoRequiresSSLTrue() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SsoRequiresSSL, "true");
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SsoRequiresSSL, "true");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations();
        testExpectations.setHttpRPAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_GENERIC_FAILURE);
        testExpectations.setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, socialRPSettings);

    }

    @Test
    public void SameSiteTests_OPdisabledSOCIALlax() throws Exception {

        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings();
        Map<String, String> socialRPSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        runVariations(opSettings, socialRPSettings);
    }

    @Test
    public void SameSiteTests_OPdisabledSOCIALnone() throws Exception {

        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings();
        Map<String, String> socialRPSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations();
        testExpectations.setHttpRPAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_GENERIC_FAILURE);
        testExpectations.setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, socialRPSettings);
    }

    @Test
    public void SameSiteTests_OPdisabledSOCIALstrict() throws Exception {

        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings();
        Map<String, String> socialRPSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        runVariations(opSettings, socialRPSettings);
    }

    /*****/
    /*****/

    @Test
    public void SameSiteTests_OPlaxSOCIALdisabled() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setRPConfigSettings();

        runVariations(opSettings, socialRPSettings);
    }

    @Test
    public void SameSiteTests_OPlaxSOCIALlax() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        runVariations(opSettings, socialRPSettings);
    }

    //    @Mode(TestMode.LITE)
    @Test
    public void SameSiteTests_OPlaxSOCIALnone() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations();
        testExpectations.setHttpRPAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_GENERIC_FAILURE);
        testExpectations.setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, socialRPSettings);
    }

    @Test
    public void SameSiteTests_OPlaxSOCIALstrict() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        runVariations(opSettings, socialRPSettings);
    }

    /*****/
    /*****/

    @Test
    public void SameSiteTests_OPnoneSOCIALdisabled() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setRPConfigSettings();

        runVariations(opSettings, socialRPSettings);

    }

    @Test
    public void SameSiteTests_OPnoneSOCIALlax() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        runVariations(opSettings, socialRPSettings);
    }

    //    @Mode(TestMode.LITE)
    @Test
    public void SameSiteTests_OPnoneSOCIALnone() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations();
        testExpectations.setHttpRPAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_GENERIC_FAILURE);
        testExpectations.setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, socialRPSettings);
    }

    //    @Mode(TestMode.LITE)
    @Test
    public void SameSiteTests_OPnoneSOCIALnone_ssoRequiresSSLTrue() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        opUpdates.put(SameSiteTestTools.SsoRequiresSSL, "true");
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        rpUpdates.put(SameSiteTestTools.SsoRequiresSSL, "true");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations();
        testExpectations.setHttpRPAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_GENERIC_FAILURE);
        testExpectations.setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, socialRPSettings);

    }

    @Test
    public void SameSiteTests_OPnoneSOCIALstrict() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        runVariations(opSettings, socialRPSettings);
    }

    /*****/
    /*****/

    @Test
    public void SameSiteTests_OPstrictSOCIALdisabled() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setRPConfigSettings();

        runVariations(opSettings, socialRPSettings);
    }

    @Test
    public void SameSiteTests_OPstrictSOCIALlax() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        runVariations(opSettings, socialRPSettings);
    }

    @Test
    public void SameSiteTests_OPstrictSOCIALnone() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations();
        testExpectations.setHttpRPAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_GENERIC_FAILURE);
        testExpectations.setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.RP_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, socialRPSettings);
    }

    @Test
    public void SameSiteTests_OPstrictSOCIALstrict() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setRPConfigSettings(rpUpdates);

        runVariations(opSettings, socialRPSettings);
    }

}
