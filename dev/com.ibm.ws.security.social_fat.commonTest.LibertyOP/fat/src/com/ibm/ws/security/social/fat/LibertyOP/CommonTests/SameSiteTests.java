/*******************************************************************************
 * Copyright (c) 2020, 2024 IBM Corporation and others.
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

package com.ibm.ws.security.social.fat.LibertyOP.CommonTests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.AutomationTools;
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
import componenttest.custom.junit.runner.RepeatTestFilter;
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

        expectations = vData.addSuccessStatusCodesForActions(SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, invoke_social_login_actions);
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

    public void validateCookies(Object response, SameSiteTestExpectations.TestServerExpectations testExpectation, String samesiteSetting, boolean partitionedCookie) throws Exception {

        String[] cookies = {};
        String[] clearedCookies = {};
        List<validationData> expectations = null;

        if (!testExpectation.equals(SameSiteTestExpectations.TestServerExpectations.CLIENT_REDIRECT_FAILURE)) { // no cookies to check on redirect failure
            if (testExpectation.equals(SameSiteTestExpectations.TestServerExpectations.CLIENT_GENERIC_FAILURE) && samesiteSetting.equals(SocialConstants.SAMESITE_NONE)) {
                if (RepeatTestFilter.getRepeatActionsAsString().contains(OAuthOIDCRepeatActions.oidc_type)) {
                    cookies = new String[] { "WASOidcCode", "WASOidcState", "WASOidcNonce" };
                    clearedCookies = new String[] { "WASReqURLOidc", "WASOidcSession" };
                } else {
                    cookies = new String[] { "WASSocialState" };
                    clearedCookies = new String[] { "WASReqURL", "WASPostParam" };
                }
            } else {
                if (RepeatTestFilter.getRepeatActionsAsString().contains(OAuthOIDCRepeatActions.oidc_type)) {
                    cookies = new String[] { "WASOidcCode", "WASOidcState", "WASOidcSession", "WASOidcNonce", "WASReqURLOidc", "LtpaToken2" };
                } else {
                    cookies = new String[] { "LtpaToken2" };
                    clearedCookies = new String[] { "WASReqURL", "WASPostParam", "WASSocialState" };
                }
            }
        }
        for (String cookie : cookies) {
            if (_testName.toLowerCase().contains(SocialConstants.SAMESITE_DISABLED.toLowerCase()) && samesiteSetting.equals(SocialConstants.SAMESITE_DISABLED)) {
                //                if (samesiteSetting.equals(SocialConstants.SAMESITE_DISABLED)) {
                expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_HEADER, SocialConstants.STRING_DOES_NOT_MATCH, "Was expecting SameSite to NOT be included in the cookie setting for: " + cookie, null, cookie + ".*" + SocialConstants.SAMESITE_KEY);
                expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_HEADER, SocialConstants.STRING_DOES_NOT_MATCH, "Was expecting that Partitioned is NOT included in the cookie setting for: " + cookie + " and it was there.", null, cookie + ".*" + SocialConstants.PARTITIONEDCOOKIE_KEY);
            } else {
                expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_HEADER, SocialConstants.STRING_MATCHES, "Was expecting SameSite=" + samesiteSetting + " be included in the cookie setting for: " + cookie + " and it was NOT there.", null, cookie + ".*" + SocialConstants.SAMESITE_KEY + "=" + samesiteSetting);
                if (samesiteSetting.equals(SocialConstants.SAMESITE_NONE)) {
                    if (partitionedCookie) {
                        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_HEADER, SocialConstants.STRING_MATCHES, "Was expecting that Partitioned be included in the cookie setting for: " + cookie + " and it was NOT there.", null, cookie + ".*" + SocialConstants.PARTITIONEDCOOKIE_KEY);
                    } else {
                        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_HEADER, SocialConstants.STRING_DOES_NOT_MATCH, "Was expecting that Partitioned is NOT included in the cookie setting for: " + cookie + " and it was there.", null, cookie + ".*" + SocialConstants.PARTITIONEDCOOKIE_KEY);
                    }
                } else {
                    expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_HEADER, SocialConstants.STRING_DOES_NOT_MATCH, "Was expecting that Partitioned is NOT included in the cookie setting for: " + cookie + " and it was there.", null, cookie + ".*" + SocialConstants.PARTITIONEDCOOKIE_KEY);
                }
            }
        }
        for (String cookie : clearedCookies) {
            expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_HEADER, SocialConstants.STRING_DOES_NOT_MATCH, "Was expecting SameSite to NOT be included in the cookie setting for: " + cookie, null, cookie + ".*" + SocialConstants.SAMESITE_KEY);
            expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_HEADER, SocialConstants.STRING_DOES_NOT_MATCH, "Was expecting that Partitioned is NOT included in the cookie setting for: " + cookie + " and it was there.", null, cookie + ".*" + SocialConstants.PARTITIONEDCOOKIE_KEY);
        }

        headerCheck(response, expectations);
    }

    public void headerCheck(Object response, List<validationData> expectations) throws Exception {

        String thisMethod = "headerCheck";

        if (expectations == null) {
            return;
        }
        Map<String, String[]> headers = AutomationTools.getResponseHeaders(response);
        if (headers == null) {
            return;
        }

        String fullResponseContent = validationTools.getResponseHeadersString(response);
        for (validationData expected : expectations) {
            boolean found = false;
            for (Entry<String, String[]> header : headers.entrySet()) {
                for (String value : header.getValue()) {

                    Pattern pattern = Pattern.compile(expected.getValidationValue());
                    Matcher m = pattern.matcher(value);
                    found = m.find();
                    if (found) {
                        Log.info(thisClass, "headerCheck", "Breaking (one value)");
                        break;
                    }
                }
                if (found) {
                    Log.info(thisClass, "headerCheck", "Breaking (one header)");
                    break;
                }
            }
            Log.info(thisClass, "headerCheck", "string was found: " + Boolean.toString(found));

            if (expected.getCheckType().equals(SocialConstants.STRING_MATCHES) || expected.getCheckType().equals(SocialConstants.STRING_EQUALS)) {
                //                        Pattern pattern = Pattern.compile(expected.getValidationValue());
                //                        Matcher m = pattern.matcher(value);
                //                        found = m.find();

                msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg()
                        + " Was expecting [" + expected.getValidationValue() + "]"
                        + " but received " + fullResponseContent,
                        found);
            } else {
                //                        Pattern pattern = Pattern.compile(expected.getValidationValue());
                //                        Matcher m = pattern.matcher(value);

                msgUtils.assertTrueAndLog(thisMethod, expected.getPrintMsg()
                        + " Was not expecting [" + expected.getValidationValue() + "]"
                        + " but received " + fullResponseContent,
                        !found);

            }
        }
    }

    public void mainPathTest(SameSiteTestExpectations.TestServerExpectations testExpectation, String samesiteSetting, boolean partitionedCookie, SocialTestSettings settings, String subTestPrefix) throws Exception {

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
        case CLIENT_GENERIC_FAILURE:
            expectations = badExpectations(genericTestServer, subTestPrefix);
            break;
        case CLIENT_REDIRECT_FAILURE:
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

        Object response = genericSocial(_testName, webClient, invoke_social_login_actions, settings, expectations);

        validateCookies(response, testExpectation, samesiteSetting, partitionedCookie);

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

        mainPathTest(testExpectations.getBaseTestResult(), testExpectations.getSamesiteSetting(), testExpectations.getPartitionedCookie(), socialSettings, "Base Tests");

        /*********************************************************/
        /* test using http with the request to the app on the RP */
        /*********************************************************/
        // NOTE:  we are updating the actual "TestSettings" with a different url
        // only this test is using the updated settings (not updatedTestSettings passed in call to mainPathTest)
        updatedTestSettings.setProtectedResource(genericTestServer.getServerHttpString() + "/helloworld/rest/helloworld");
        mainPathTest(testExpectations.getHttpClientAppUrlTestResult(), testExpectations.getSamesiteSetting(), testExpectations.getPartitionedCookie(), updatedTestSettings, "Http Social App request");
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
            mainPathTest(testExpectations.getHttpRedirectUrlTestResult(), testExpectations.getSamesiteSetting(), testExpectations.getPartitionedCookie(), socialSettings, "Http Social Redirect");

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
            mainPathTest(testExpectations.getHttpAuthEndpointUrlTestResult(), testExpectations.getSamesiteSetting(), testExpectations.getPartitionedCookie(), socialSettings, "Http Social Authorization Endpoint");

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
            mainPathTest(testExpectations.getHttpTokenEndpointUrlTestResult(), testExpectations.getSamesiteSetting(), testExpectations.getPartitionedCookie(), socialSettings, "Http Social Token Endpoint");
        }
    }

    @Mode(TestMode.LITE)
    @Test
    public void SameSiteTests_OPdisabledSOCIALdisabled() throws Exception {

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(); // use defaults
        Map<String, String> socialRPSettings = samesiteTestTools.setClientConfigSettings(); // use defaults

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
        Map<String, String> socialRPSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations();
        testExpectations.setHttpClientAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_GENERIC_FAILURE);
        testExpectations.setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, socialRPSettings);

    }

    @Test
    public void SameSiteTests_OPdisabledSOCIALlax() throws Exception {

        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings();
        Map<String, String> socialRPSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        runVariations(new SameSiteTestExpectations(SocialConstants.SAMESITE_LAX, false), opSettings, socialRPSettings);
    }

    @Test
    public void SameSiteTests_OPdisabledSOCIALnone() throws Exception {

        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings();
        Map<String, String> socialRPSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations(SocialConstants.SAMESITE_NONE, false);
        testExpectations.setHttpClientAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_GENERIC_FAILURE);
        testExpectations.setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, socialRPSettings);
    }

    @Test
    public void SameSiteTests_OPdisabledSOCIALstrict() throws Exception {

        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings();
        Map<String, String> socialRPSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        runVariations(new SameSiteTestExpectations(SocialConstants.SAMESITE_STRICT, false), opSettings, socialRPSettings);
    }

    /*****/
    /*****/

    @Test
    public void SameSiteTests_OPlaxSOCIALdisabled() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setClientConfigSettings();

        runVariations(opSettings, socialRPSettings);
    }

    @Test
    public void SameSiteTests_OPlaxSOCIALlax() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        runVariations(new SameSiteTestExpectations(SocialConstants.SAMESITE_LAX, false), opSettings, socialRPSettings);
    }

    //    @Mode(TestMode.LITE)
    @Test
    public void SameSiteTests_OPlaxSOCIALnone() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations(SocialConstants.SAMESITE_NONE, false);
        testExpectations.setHttpClientAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_GENERIC_FAILURE);
        testExpectations.setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, socialRPSettings);
    }

    @Test
    public void SameSiteTests_OPlaxSOCIALstrict() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        runVariations(new SameSiteTestExpectations(SocialConstants.SAMESITE_STRICT, false), opSettings, socialRPSettings);
    }

    /*****/
    /*****/

    @Test
    public void SameSiteTests_OPnoneSOCIALdisabled() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setClientConfigSettings();

        runVariations(opSettings, socialRPSettings);

    }

    @Test
    public void SameSiteTests_OPnoneSOCIALlax() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        runVariations(new SameSiteTestExpectations(SocialConstants.SAMESITE_LAX, false), opSettings, socialRPSettings);
    }

    //    @Mode(TestMode.LITE)
    @Test
    public void SameSiteTests_OPnoneSOCIALnone() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations(SocialConstants.SAMESITE_NONE, false);
        testExpectations.setHttpClientAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_GENERIC_FAILURE);
        testExpectations.setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_REDIRECT_FAILURE);
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
        Map<String, String> socialRPSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations(SocialConstants.SAMESITE_NONE, false);
        testExpectations.setHttpClientAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_GENERIC_FAILURE);
        testExpectations.setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, socialRPSettings);

    }

    @Test
    public void SameSiteTests_OPnoneSOCIALstrict() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        runVariations(new SameSiteTestExpectations(SocialConstants.SAMESITE_STRICT, false), opSettings, socialRPSettings);
    }

    /*****/
    /*****/

    @Test
    public void SameSiteTests_OPstrictSOCIALdisabled() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setClientConfigSettings();

        runVariations(opSettings, socialRPSettings);
    }

    @Test
    public void SameSiteTests_OPstrictSOCIALlax() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        runVariations(new SameSiteTestExpectations(SocialConstants.SAMESITE_LAX, false), opSettings, socialRPSettings);
    }

    @Test
    public void SameSiteTests_OPstrictSOCIALnone() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations(SocialConstants.SAMESITE_NONE, false);
        testExpectations.setHttpClientAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_GENERIC_FAILURE);
        testExpectations.setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, socialRPSettings);
    }

    @Test
    public void SameSiteTests_OPstrictSOCIALstrict() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> socialRPSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        runVariations(new SameSiteTestExpectations(SocialConstants.SAMESITE_STRICT, false), opSettings, socialRPSettings);
    }

    @Test
    public void SameSiteTests_OPdisabledSOCIALdisabledPartitionedTrue() throws Exception {

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(); // use defaults
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.PartitionedCookieKey, "True");
        Map<String, String> rpSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates); // use defaults

        runVariations(opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPdisabledSOCIALnonePartitionedFalse() throws Exception {

        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        rpUpdates.put(SameSiteTestTools.PartitionedCookieKey, "False");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings();
        Map<String, String> rpSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations(SocialConstants.SAMESITE_NONE, false);
        testExpectations.setHttpClientAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_GENERIC_FAILURE);
        testExpectations
                .setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPdisabledSOCIALnonePartitionedTrue() throws Exception {

        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        rpUpdates.put(SameSiteTestTools.PartitionedCookieKey, "True");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings();
        Map<String, String> rpSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations(SocialConstants.SAMESITE_NONE, true);
        testExpectations.setHttpClientAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_GENERIC_FAILURE);
        testExpectations
                .setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPlaxSOCIALdisabledPartitionedTrue() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.PartitionedCookieKey, "True");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        runVariations(opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPlaxSOCIALnonePartitionedFalse() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        rpUpdates.put(SameSiteTestTools.PartitionedCookieKey, "False");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations(SocialConstants.SAMESITE_NONE, false);
        testExpectations.setHttpClientAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_GENERIC_FAILURE);
        testExpectations
                .setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPlaxSOCIALnonePartitionedTrue() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        rpUpdates.put(SameSiteTestTools.PartitionedCookieKey, "True");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations(SocialConstants.SAMESITE_NONE, true);
        testExpectations.setHttpClientAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_GENERIC_FAILURE);
        testExpectations
                .setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, rpSettings);
    }

    /*****/
    /*****/

    @Test
    public void SameSiteTests_OPnonePartitionedFalseSOCIALdisabled() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        opUpdates.put(SameSiteTestTools.PartitionedCookieKey, "False");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setClientConfigSettings();

        runVariations(opSettings, rpSettings);

    }

    @Test
    public void SameSiteTests_OPnonePartitionedTruSOCIALdisabled() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        opUpdates.put(SameSiteTestTools.PartitionedCookieKey, "True");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setClientConfigSettings();

        runVariations(opSettings, rpSettings);

    }

    @Test
    public void SameSiteTests_OPnonePartitionedFalseSOCIALlax() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        opUpdates.put(SameSiteTestTools.PartitionedCookieKey, "False");
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        runVariations(new SameSiteTestExpectations(SocialConstants.SAMESITE_LAX, false), opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPnonePartitionedTrueSOCIALlax() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        opUpdates.put(SameSiteTestTools.PartitionedCookieKey, "True");
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_LAX);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        runVariations(new SameSiteTestExpectations(SocialConstants.SAMESITE_LAX, false), opSettings, rpSettings);
    }

    @Mode(TestMode.LITE)
    @Test
    public void SameSiteTests_OPnonePartitionedFalseSOCIALnonePartitionedFalse() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        opUpdates.put(SameSiteTestTools.PartitionedCookieKey, "False");
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        rpUpdates.put(SameSiteTestTools.PartitionedCookieKey, "False");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations(SocialConstants.SAMESITE_NONE, false);
        testExpectations.setHttpClientAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_GENERIC_FAILURE);
        testExpectations
                .setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPnonePartitionedFalseSOCIALnonePartitionedTrue() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        opUpdates.put(SameSiteTestTools.PartitionedCookieKey, "False");
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        rpUpdates.put(SameSiteTestTools.PartitionedCookieKey, "True");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations(SocialConstants.SAMESITE_NONE, true);
        testExpectations.setHttpClientAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_GENERIC_FAILURE);
        testExpectations
                .setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPnonePartitionedTrueSOCIALnonePartitionedFalse() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        opUpdates.put(SameSiteTestTools.PartitionedCookieKey, "True");
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        rpUpdates.put(SameSiteTestTools.PartitionedCookieKey, "False");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations(SocialConstants.SAMESITE_NONE, false);
        testExpectations.setHttpClientAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_GENERIC_FAILURE);
        testExpectations
                .setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPnonePartitionedTrueSOCIALnonePartitionedTrue() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        opUpdates.put(SameSiteTestTools.PartitionedCookieKey, "True");
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        rpUpdates.put(SameSiteTestTools.PartitionedCookieKey, "True");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations(SocialConstants.SAMESITE_NONE, true);
        testExpectations.setHttpClientAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_GENERIC_FAILURE);
        testExpectations
                .setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPnonePartitionedFalseSOCIALstrict() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        opUpdates.put(SameSiteTestTools.PartitionedCookieKey, "False");
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        runVariations(new SameSiteTestExpectations(SocialConstants.SAMESITE_STRICT, false), opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPnonePartitionedTrueSOCIALstrict() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        opUpdates.put(SameSiteTestTools.PartitionedCookieKey, "True");
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        runVariations(new SameSiteTestExpectations(SocialConstants.SAMESITE_STRICT, true), opSettings, rpSettings);
    }

    /*****/
    /*****/

    @Test
    public void SameSiteTests_OPstrictSOCIALnonePartitionedFalse() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        rpUpdates.put(SameSiteTestTools.PartitionedCookieKey, "False");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations(SocialConstants.SAMESITE_NONE, false);
        testExpectations.setHttpClientAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_GENERIC_FAILURE);
        testExpectations
                .setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPstrictSOCIALnonePartitionedTrue() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        rpUpdates.put(SameSiteTestTools.PartitionedCookieKey, "True");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations(SocialConstants.SAMESITE_NONE, true);
        testExpectations.setHttpClientAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_GENERIC_FAILURE);
        testExpectations
                .setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, rpSettings);
    }

    @Test
    public void SameSiteTests_OPstrictSOCIALstrictPartitionedTrue() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_STRICT);
        rpUpdates.put(SameSiteTestTools.PartitionedCookieKey, "True");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        runVariations(new SameSiteTestExpectations(SocialConstants.SAMESITE_STRICT, false), opSettings, rpSettings);
    }

    @Mode(TestMode.LITE)
    @Test
    public void SameSiteTests_OPnonePartitionedDeferSOCIALnonePartitionedDefer() throws Exception {

        Map<String, String> opUpdates = new HashMap<String, String>();
        opUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        opUpdates.put(SameSiteTestTools.PartitionedCookieKey, "Defer");
        Map<String, String> rpUpdates = new HashMap<String, String>();
        rpUpdates.put(SameSiteTestTools.SameSiteCookieKey, SocialConstants.SAMESITE_NONE);
        rpUpdates.put(SameSiteTestTools.PartitionedCookieKey, "Defer");

        Map<String, String> opSettings = samesiteTestTools.setOPConfigSettings(opUpdates);
        Map<String, String> rpSettings = samesiteTestTools.setConfigConfigSettings(rpUpdates);

        SameSiteTestExpectations testExpectations = new SameSiteTestExpectations(SocialConstants.SAMESITE_NONE, false);
        testExpectations.setHttpClientAppUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_GENERIC_FAILURE);
        testExpectations
                .setHttpRedirectUrlTestResult(SameSiteTestExpectations.TestServerExpectations.CLIENT_REDIRECT_FAILURE);
        runVariations(testExpectations, opSettings, rpSettings);
    }

}
