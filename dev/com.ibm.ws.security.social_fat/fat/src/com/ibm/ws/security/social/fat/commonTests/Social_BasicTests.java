/*******************************************************************************
 * Copyright (c) 2017, 2021 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package com.ibm.ws.security.social.fat.commonTests;

import java.net.URL;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.expectations.Expectation;
import com.ibm.ws.security.fat.common.expectations.Expectations;
import com.ibm.ws.security.fat.common.expectations.ResponseFullExpectation;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.Constants;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.structures.ValidationDataToExpectationConverter;
import com.ibm.ws.security.social.fat.utils.SocialCommonTest;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialMessageConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.annotation.SkipForRepeat;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

/**
 * NOTE 1: There appears to be an issue with repeating EE9 tests for some of these tests. The EE9 tests run fine when
 * this class is run alone, but when running EE7/8 and then repeating with EE9 they fail. My suspicion given the
 * failures is that some static setting is being modified when run with the full suite, but since we have an abundance
 * of social tests, we will just skip in EE9 for now.
 */
@RunWith(FATRunner.class)
// @LibertyServerWrapper
// @Mode(TestMode.FULL)
public class Social_BasicTests extends SocialCommonTest {

    public static Class<?> thisClass = Social_BasicTests.class;
    public static boolean isTestingOidc = false; // subclass may override

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke Helloworld
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the login page from Social. After entering a valid id/pw, we should receive access to the
     * helloworld app
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    @SkipForRepeat(SkipForRepeat.EE9_FEATURES) // TODO See note 1 in class javadoc.
    public void Social_BasicTests_MainPath() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        String lastStep = invoke_social_login_actions[invoke_social_login_actions.length - 1];

        List<validationData> expectations = setGoodSocialExpectations(socialSettings, doNotAddJWTTokenValidation);
        // Ensure that the subject principals do NOT include a JWT
        String jwtUserPrincipal = "Principal: {";
        expectations = vData.addExpectation(expectations, lastStep, SocialConstants.RESPONSE_FULL,
                SocialConstants.STRING_DOES_NOT_CONTAIN, "Found an unexpected JWT principal in the app response.", null,
                jwtUserPrincipal);

        genericSocial(_testName, webClient, invoke_social_login_actions, socialSettings, expectations);

        if (isTestingOidc) {
            testUserInfo(webClient);
        }
    }

    /**
     * Test that userinfo is retrieved and available from an API call. If userinfo url is defined and enabled in
     * metadata, then upon authentication the userinfo JSON from the OP, if available, is to be stored in the subject as
     * a string and made accessible through the UserProfile API. Since we invoked the protected resource, we should
     * already be authenticated. This calls a jsp that invokes the UserProfile.getUserInfo() API to check the userinfo.
     */
    void testUserInfo(WebClient webClient) throws Exception {
        String endpoint = socialSettings.getProtectedResource();
        endpoint = endpoint.replace("rest/helloworld", "userProfileUserInfoApiTest.jsp");
        WebRequest req = new WebRequest(new URL(endpoint));
        HtmlPage wr = webClient.getPage(req);
        String response = wr.asText();
        Log.info(thisClass, _testName, "Got JSP response: " + response);

        String testAction = "testUserInfo";
        String expectedUser = socialSettings.getAdminUser();
        Expectations expectations = new Expectations();
        expectations
                .addExpectation(Expectation.createResponseExpectation(testAction, "\"sub\":\"" + expectedUser + "\"",
                        "Did not find expected \"sub\" claim and value in the JSP response."));
        expectations
                .addExpectation(Expectation.createResponseExpectation(testAction, "\"name\":\"" + expectedUser + "\"",
                        "Did not find expected \"name\" claim and value in the JSP response."));
        expectations.addExpectation(new ResponseFullExpectation(testAction, SocialConstants.STRING_MATCHES,
                "\"iss\":\"http[^\"]+/OidcConfigSample\"",
                "Did not find expected \"iss\" claim and value in the JSP response."));
        List<validationData> convertedExpectations = ValidationDataToExpectationConverter
                .convertExpectations(expectations);
        validationTools.validateResult(wr, testAction, convertedExpectations, socialSettings);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke Helloworld
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the login page from Social. After entering a valid id/pw, we should receive access to the
     * helloworld app
     * </OL>
     */
    @Mode(TestMode.LITE)
    @Test
    public void Social_BasicTests_MainPath_withJwtSsoFeature() throws Exception {

        genericTestServer.reconfigServer(providerConfigString + "_withJwtSsoFeature.xml", _testName, true, null);

        WebClient webClient = getAndSaveWebClient();

        String lastStep = invoke_social_login_actions[invoke_social_login_actions.length - 1];

        List<validationData> expectations = setGoodSocialExpectations(socialSettings, doNotAddJWTTokenValidation);
        // Ensure that the subject principals include a JWT
        String issClaim = "\"iss\":\"https?://[^/]+/jwt(sso)?/defaultJwtSso\"";
        String jwtUserPrincipal = "Principal: \\{.+" + issClaim;
        expectations = vData.addExpectation(expectations, lastStep, SocialConstants.RESPONSE_FULL,
                SocialConstants.STRING_MATCHES,
                "Did not find the expected JWT principal in the app response but should have.", null, jwtUserPrincipal);

        genericSocial(_testName, webClient, invoke_social_login_actions, socialSettings, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>Invoke Helloworld
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the login page from Social. After entering a valie id/pw, we should receive access to the
     * helloworld app
     * </OL>
     */
    @Test
    public void Social_BasicTests_InvalidUserPassword() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        updatedSocialTestSettings.setUserPassword("badPw");

        // Invoke the protected app, get the login page, put a bad pw in the login page and expect to get the login page
        // again
        List<validationData> expectations = vData.addSuccessStatusCodesForActions(invoke_social_login_actions);
        expectations = vData.addExpectation(expectations, SocialConstants.INVOKE_SOCIAL_RESOURCE,
                Constants.RESPONSE_FULL, Constants.STRING_MATCHES, "Did not get to the Login page", null,
                updatedSocialTestSettings.getLoginPage());
        if (provider.equals(SocialConstants.TWITTER_PROVIDER)) {
            // Twitter will redirect to a different login page
            expectations = vData.addExpectation(expectations, perform_social_login, Constants.RESPONSE_FULL,
                    Constants.STRING_CONTAINS, "Did NOT get to the Twitter Login only page", null,
                    SocialConstants.TWITTER_LOGIN_TITLE);
        } else {
            if (provider.equals(SocialConstants.LINKEDIN_PROVIDER)) {
                expectations = vData.addExpectation(expectations, perform_social_login, Constants.RESPONSE_FULL,
                        Constants.STRING_CONTAINS, "Did NOT get to the LinkedIn Sign-up page", null,
                        SocialConstants.LINKEDIN_FAILED_LOGIN_TITLE);
            } else {
                expectations = vData.addExpectation(expectations, perform_social_login, Constants.RESPONSE_FULL,
                        Constants.STRING_MATCHES, "Did not get to the Login page", null,
                        updatedSocialTestSettings.getLoginPage());
            }
        }
        if (provider.equals(SocialConstants.LIBERTYOP_PROVIDER)) {
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, perform_social_login,
                    Constants.MESSAGES_LOG, Constants.STRING_CONTAINS, "The OP should have received a login error",
                    SocialMessageConstants.CWWKS1100A_LOGIN_FAILED);
            expectations = validationTools.addMessageExpectation(testOPServer, expectations, perform_social_login,
                    Constants.MESSAGES_LOG, Constants.STRING_CONTAINS,
                    "The OP should have received a login error from WIM ",
                    SocialMessageConstants.CWIML4537E_LOGIN_FAILED);
        }
        HtmlPage response = (HtmlPage) genericSocial(_testName, webClient, invoke_social_login_actions,
                updatedSocialTestSettings, expectations);

        /***
         * OK, I give... we'll just make sure that we didn't get to the app - make sure we got some form of a login or
         * error page and stop at that LinkedIn, Twitter and now Facebook are giving one of various pages when we give a
         * bad password - it's hard to code what to expect, when they keep changing what they'll reply with.
         */
        // // login again, but this time use the correct password - show that subsequent good logins will work.
        // List<validationData> expectations2 = setGoodSocialExpectations(socialSettings, doNotAddJWTTokenValidation,
        // invoke_social_just_login_actions[invoke_social_just_login_actions.length - 1]);
        // // Twitter has 2 panels to process after entering the wrong password
        // // if (provider.equals(SocialConstants.TWITTER_PROVIDER)) {
        // // expectations2 = vData.addExpectation(expectations2, SocialConstants.TWITTER_LOGIN,
        // Constants.RESPONSE_FULL, Constants.STRING_CONTAINS, "Did NOT get to the Authorize only page", null,
        // SocialConstants.TWITTER_AUTHORIZE_TITLE);
        // // }
        //
        // if (provider.equalsIgnoreCase(SocialConstants.LINKEDIN_PROVIDER)) {
        // genericSocial(_testName, webClient, response, SocialConstants.LINKEDIN_SIGN_IN_AND_LOGIN_ACTIONS,
        // socialSettings, expectations2);
        // } else {
        // genericSocial(_testName, webClient, response, invoke_social_just_login_actions, socialSettings,
        // expectations2);
        // }
    }

}
