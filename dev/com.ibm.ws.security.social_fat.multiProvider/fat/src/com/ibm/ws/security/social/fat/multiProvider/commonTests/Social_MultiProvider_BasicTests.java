/*******************************************************************************
 * Copyright (c) 2017, 2022 IBM Corporation and others.
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
package com.ibm.ws.security.social.fat.multiProvider.commonTests;

import java.util.List;

import org.junit.Test;

import com.gargoylesoftware.htmlunit.WebClient;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.social.fat.utils.SocialCommonTest;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;

public class Social_MultiProvider_BasicTests extends SocialCommonTest {

    public static Class<?> thisClass = Social_MultiProvider_BasicTests.class;
    public static String runConfigType = null;
    public static String nameExtender = null;

    /***
     * These tests run with a config that contains multiple Social Login providers, we should be presented with the Social Login
     * selection page
     * when we request access to our app. Once we select the provider that we want, we should get the login page for that
     * provider.
     * After entering proper credentials, we should gain access to that page
     *
     * These tests are intended to run with several configurations. They'll need use different values in the selection panel based
     * on the type of config. If we're testing with a provider specific config, they'll always use the provider's name (ie:
     * facebookLogin, githubLogin, ...) If we're using the social login generic configs, they'll use the config id.
     * To simplify the tests, we'll use a base name of the providerName + an "extension". The extension will be the same
     * for all providers and will be set by the extending class.
     * The extending class that tests with one each of the provider specific configs, will set and extension of "null",
     * The extending class that tests with one each of the social login generic configs, will set and extension of 1
     * this will cause a redirect to be built that looks like
     * https://localhost:8020/ibm/api/social-login/redirect/facebookLogin1 or
     * https://localhost:8020/ibm/api/social-login/redirect/githubLogin/1
     */

    public String fixIdName(String id, String extension) throws Exception {
        return fixIdName(id, null, extension);
    }

    public String fixIdName(String id, String separator, String extension) throws Exception {

        if (extension != null) {
            if (separator == null) {
                return id + extension;
            } else {
                return id + separator + extension;
            }
        } else {
            return id;
        }
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this Test we are going to access Facebook (more details of how the test works can be found above)
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then Facebook login page. After entering a valid id/pw, we should
     * receive access to the helloworld app
     * </OL>
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRun.class)
    @AllowedFFDC({ "java.io.IOException" })
    @Mode(TestMode.LITE)
    @Test
    public void Social_MultiProvider_BasicTests_SelectFacebook() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.FACEBOOK_PROVIDER, SocialConstants.OAUTH_OP, true);
        //        setGenericVSSpeicificProviderFlags(runConfigType, null, SocialConstants.OAUTH_OP);
        updatedSocialTestSettings = updateFacebookSettings(updatedSocialTestSettings);
        updatedSocialTestSettings.setProviderButton(fixIdName(SocialConstants.FACEBOOK_LOGIN, nameExtender));
        if (configType.equals(ProviderConfig)) {
            updatedSocialTestSettings.setProviderButtonDisplay(SocialConstants.FACEBOOK_DISPLAY_NAME);
        } else {
            // for oauth2Login configs, the id should be the same as the display name
            updatedSocialTestSettings.setProviderButtonDisplay(updatedSocialTestSettings.getProviderButtonDisplay());
        }

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this Test we are going to access Twitter (more details of how the test works can be found above)
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then Twitter login page. After entering a valid id/pw, we should
     * receive access to the helloworld app
     * </OL>
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRumORGenericConfig.class)
    @AllowedFFDC({ "java.io.IOException" })
    @Mode(TestMode.LITE)
    @Test
    public void Social_MultiProvider_BasicTests_SelectTwitter() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.TWITTER_PROVIDER, SocialConstants.OAUTH_OP, true);
        updatedSocialTestSettings = updateTwitterSettings(updatedSocialTestSettings);
        if (configType.equals(ProviderConfig)) {
            updatedSocialTestSettings.setProviderButtonDisplay(SocialConstants.TWITTER_DISPLAY_NAME);
        } else {
            // for oauth2Login configs, the id should be the same as the display name
            updatedSocialTestSettings.setProviderButtonDisplay(updatedSocialTestSettings.getProviderButtonDisplay());
        }
        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this Test we are going to access Linkedin (more details of how the test works can be found above)
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then Linkedin login page. After entering a valid id/pw, we should
     * receive access to the helloworld app
     * </OL>
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRun.class)
    @AllowedFFDC({ "java.io.IOException" })
    @Mode(TestMode.LITE)
    @Test
    public void Social_MultiProvider_BasicTests_SelectLinkedin() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.LINKEDIN_PROVIDER, SocialConstants.OAUTH_OP, true);
        updatedSocialTestSettings = updateLinkedinSettings(updatedSocialTestSettings);
        updatedSocialTestSettings.setProviderButton(fixIdName(SocialConstants.LINKEDIN_LOGIN, nameExtender));
        if (configType.equals(ProviderConfig)) {
            updatedSocialTestSettings.setProviderButtonDisplay(SocialConstants.LINKEDIN_DISPLAY_NAME);
        } else {
            // for oauth2Login configs, the id should be the same as the display name
            updatedSocialTestSettings.setProviderButtonDisplay(updatedSocialTestSettings.getProviderButtonDisplay());
        }
        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this Test we are going to access GitHub (more details of how the test works can be found above)
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then GitHub login page. After entering a valid id/pw, we should
     * receive access to the helloworld app
     * </OL>
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRun.class)
    @AllowedFFDC({ "java.io.IOException" })
    /************************************************************************************
     *
     * Skip GitHub test until we can figure out how to keep it from either needing
     * the user to Reauthorizate, or we can figure out how to automate processing of that panel
     *
     */
    //    @Test
    public void Social_MultiProvider_BasicTests_SelectGitHub() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.GITHUB_PROVIDER, SocialConstants.OAUTH_OP, true);
        updatedSocialTestSettings = updateGitHubSettings(updatedSocialTestSettings);
        updatedSocialTestSettings.setProviderButton(fixIdName(SocialConstants.GITHUB_LOGIN, "/", nameExtender));
        if (configType.equals(ProviderConfig)) {
            updatedSocialTestSettings.setProviderButtonDisplay(SocialConstants.GITHUB_DISPLAY_NAME);
        } else {
            // for oauth2Login configs, the id should be the same as the display name
            updatedSocialTestSettings.setProviderButtonDisplay(updatedSocialTestSettings.getProviderButtonDisplay());
        }
        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this Test we are going to access Liberty OP (more details of how the test works can be found above)
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then Liberty OP login page. After entering a valid id/pw, we should
     * receive access to the helloworld app
     * </OL>
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfProviderConfig.class)
    @AllowedFFDC({ "java.io.IOException" })
    @Mode(TestMode.LITE)
    @Test
    public void Social_MultiProvider_BasicTests_SelectLibertyOP_oauth() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OAUTH_OP, true);
        //        setGenericVSSpeicificProviderFlags(runConfigType, null, SocialConstants.OAUTH_OP);
        updatedSocialTestSettings = updateLibertyOPSettings(updatedSocialTestSettings);
        updatedSocialTestSettings.setProviderButton(fixIdName(SocialConstants.OAUTH_2_LOGIN, nameExtender));
        updatedSocialTestSettings.setProviderButtonDisplay(updatedSocialTestSettings.getProviderButtonDisplay());

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this Test we are going to access Liberty OP (more details of how the test works can be found above)
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then Liberty OP login page. After entering a valid id/pw, we should
     * receive access to the helloworld app
     * </OL>
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfProviderConfig.class)
    @AllowedFFDC({ "java.io.IOException" })
    @Mode(TestMode.LITE)
    @Test
    public void Social_MultiProvider_BasicTests_SelectLibertyOP_oidc() throws Exception {

        WebClient webClient = getAndSaveWebClient();

        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OIDC_OP, true);
        //        setGenericVSSpeicificProviderFlags(runConfigType, null, SocialConstants.OAUTH_OP);
        updatedSocialTestSettings = updateLibertyOPSettings(updatedSocialTestSettings);
        updatedSocialTestSettings.setProviderButton(fixIdName(SocialConstants.OIDC_LOGIN, nameExtender));
        updatedSocialTestSettings.setProviderButtonDisplay(updatedSocialTestSettings.getProviderButtonDisplay());

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);

        genericSocial(_testName, webClient, invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }
}
