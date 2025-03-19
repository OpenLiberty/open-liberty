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
package com.ibm.ws.security.social.fat.multiProvider;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.ibm.websphere.simplicity.log.Log;
import com.ibm.ws.security.fat.common.utils.ConditionalIgnoreRule;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.RSCommonTestTools;
import com.ibm.ws.security.oauth_oidc.fat.commonTest.ValidationData.validationData;
import com.ibm.ws.security.social.fat.utils.SocialCommonTest;
import com.ibm.ws.security.social.fat.utils.SocialConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * These tests are a bit different than the other selection panel tests.
 * They use a config that has multiple configs for each provider. Each provider has a config that does NOT have a filter and
 * one that does.
 * When we specifiy an app that does NOT have a filter, we should get the selection panel. When we specify an app that does
 * match a filter, we should use that provider directly - there should be no need for the selection panel.
 * Each test will invoke an un-filtered app and expects the selection panel, it'll then choose a specific provider and make
 * sure that we can get to the app. It will then invoke the filtered version of the app and expect to get the login page
 * immediately.
 */

@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class MultiProvider_usingSocialConfig_withFilters extends SocialCommonTest {

    public static Class<?> thisClass = MultiProvider_usingSocialConfig_withFilters.class;

    public static RSCommonTestTools rsTools = new RSCommonTestTools();

    @BeforeClass
    public static void setUp() throws Exception {
        classOverrideValidationEndpointValue = SocialConstants.USERINFO_ENDPOINT;

        List<String> startMsgs = new ArrayList<String>();
        startMsgs.add("CWWKT0016I.*" + SocialConstants.SOCIAL_DEFAULT_CONTEXT_ROOT);

        List<String> extraApps = new ArrayList<String>();
        extraApps.add(SocialConstants.HELLOWORLD_SERVLET);

        // TODO fix
        List<String> opStartMsgs = new ArrayList<String>();
        //        opStartMsgs.add("CWWKS1600I.*" + SocialConstants.OIDCCONFIGMEDIATOR_APP);
        opStartMsgs.add("CWWKS1631I.*");

        // TODO fix
        List<String> opExtraApps = new ArrayList<String>();
        opExtraApps.add(SocialConstants.OP_SAMPLE_APP);

        String[] propagationTokenTypes = rsTools.chooseTokenSettings(SocialConstants.OIDC_OP);
        String tokenType = propagationTokenTypes[0];
        String certType = propagationTokenTypes[1];
        Log.info(thisClass, "setupBeforeTest", "inited tokenType to: " + tokenType);

        socialSettings = new SocialTestSettings();
        testSettings = socialSettings;

        testOPServer = commonSetUp(SocialConstants.SERVER_NAME + ".multiProvider.op", "op_server_orig.xml", SocialConstants.OIDC_OP, null, SocialConstants.DO_NOT_USE_DERBY, opStartMsgs, null, SocialConstants.OIDC_OP, true, true, tokenType, certType);
        genericTestServer = commonSetUp(SocialConstants.SERVER_NAME + ".multiProvider.social", "server_multiProvider_usingSocialConfig_withFilters.xml", SocialConstants.GENERIC_SERVER, extraApps, SocialConstants.DO_NOT_USE_DERBY, startMsgs);

        setGenericVSSpeicificProviderFlags(GenericConfig);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this Test we are going to access Facebook using first an un-filtered app, then a filtered app (more details of how
     * the test works can be found above)
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then Facebook login page. After entering a valid id/pw, we should
     * receive access to the helloworld app. Then when we invoke the app again, we should immediately get the
     * Facebook login page - entering a valid id/pw should give us access to our app.
     * </OL>
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRun.class)
    @AllowedFFDC({ "java.io.IOException" })
    @Test
    public void MultiProvider_usingSocialConfig_withFilters_SelectFacebook() throws Exception {

        // let genericSocial create a WebClient each time we call it - we want a unique client each time

        // first use the config that does NOT match a filter - expect a selection panel
        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.FACEBOOK_PROVIDER, SocialConstants.OAUTH_OP, UseSelectionPanel);
        updatedSocialTestSettings = updateFacebookSettings(updatedSocialTestSettings);
        updatedSocialTestSettings.setProviderButton(SocialConstants.FACEBOOK_LOGIN + "1");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        // Then use the config that does match a filter
        // expectations will look for the login panel immediately after we try to invoke the filter protected app
        //      instead of getting the provider selection panel
        SocialTestSettings updatedSocialTestSettings2 = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.FACEBOOK_PROVIDER, SocialConstants.OAUTH_OP, DoesNotUseSelectionPanel);
        updatedSocialTestSettings2 = updateFacebookSettings(updatedSocialTestSettings2);
        updatedSocialTestSettings2.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_facebook");

        List<validationData> expectations2 = setGoodSocialExpectations(updatedSocialTestSettings2, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings2, expectations2);

    }

    /** SKIP testing with twitter - tests are only valid with generic configs and we don't support an oauth1Login */

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this Test we are going to access Linkedin using first an un-filtered app, then a filtered app (more details of how
     * the test works can be found above)
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then Linkedin login page. After entering a valid id/pw, we should
     * receive access to the helloworld app. Then when we invoke the app again, we should immediately get the
     * Linkedin login page - entering a valid id/pw should give us access to our app.
     * </OL>
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRun.class)
    @AllowedFFDC({ "java.io.IOException" })
    @Test
    public void MultiProvider_usingSocialConfig_withFilters_SelectLinkedin() throws Exception {

        // let genericSocial create a WebClient each time we call it - we want a unique client each time

        // first use the config that does NOT match a filter - expect a selection panel
        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.LINKEDIN_PROVIDER, SocialConstants.OAUTH_OP, UseSelectionPanel);
        updatedSocialTestSettings = updateLinkedinSettings(updatedSocialTestSettings);
        updatedSocialTestSettings.setProviderButton(SocialConstants.LINKEDIN_LOGIN + "1");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        // Then use the config that does match a filter
        // expectations will look for the login panel immediately after we try to invoke the filter protected app
        //      instead of getting the provider selection panel
        SocialTestSettings updatedSocialTestSettings2 = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.LINKEDIN_PROVIDER, SocialConstants.OAUTH_OP, DoesNotUseSelectionPanel);
        updatedSocialTestSettings2 = updateLinkedinSettings(updatedSocialTestSettings2);
        updatedSocialTestSettings2.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_linkedin");

        List<validationData> expectations2 = setGoodSocialExpectations(updatedSocialTestSettings2, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings2, expectations2);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this Test we are going to access GitHub using first an un-filtered app, then a filtered app (more details of how
     * the test works can be found above)
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then GitHub login page. After entering a valid id/pw, we should
     * receive access to the helloworld app. Then when we invoke the app again, we should immediately get the
     * GitHub login page - entering a valid id/pw should give us access to our app.
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
    public void MultiProvider_usingSocialConfig_withFilters_SelectGitHub() throws Exception {

        // let genericSocial create a WebClient each time we call it - we want a unique client each time

        // first use the config that does NOT match a filter - expect a selection panel
        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.GITHUB_PROVIDER, SocialConstants.OAUTH_OP, UseSelectionPanel);
        updatedSocialTestSettings = updateGitHubSettings(updatedSocialTestSettings);
        updatedSocialTestSettings.setProviderButton(SocialConstants.GITHUB_LOGIN + "/1");

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        // Then use the config that does match a filter
        // expectations will look for the login panel immediately after we try to invoke the filter protected app
        //      instead of getting the provider selection panel
        SocialTestSettings updatedSocialTestSettings2 = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.GITHUB_PROVIDER, SocialConstants.OAUTH_OP, DoesNotUseSelectionPanel);
        updatedSocialTestSettings2 = updateGitHubSettings(updatedSocialTestSettings2);
        updatedSocialTestSettings2.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_github");

        List<validationData> expectations2 = setGoodSocialExpectations(updatedSocialTestSettings2, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings2, expectations2);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this Test we are going to access Liberty OP using first an un-filtered app, then a filtered app (more details of how
     * the test works can be found above)
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then Liberty OP login page. After entering a valid id/pw, we should
     * receive access to the helloworld app. Then when we invoke the app again, we should immediately get the
     * Liberty OP login page - entering a valid id/pw should give us access to our app.
     * </OL>
     */
    @AllowedFFDC({ "java.io.IOException" })
    @Mode(TestMode.LITE)
    @Test
    public void MultiProvider_usingSocialConfig_withFilters_SelectLibertyOP_oauth() throws Exception {

        // let genericSocial create a WebClient each time we call it - we want a unique client each time

        // first use the config that does NOT match a filter - expect a selection panel
        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OAUTH_OP, UseSelectionPanel);
        updatedSocialTestSettings = updateLibertyOPSettings(updatedSocialTestSettings);
        updatedSocialTestSettings.setProviderButton(SocialConstants.OAUTH_2_LOGIN + 1);

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        // Then use the config that does match a filter
        // expectations will look for the login panel immediately after we try to invoke the filter protected app
        //      instead of getting the provider selection panel
        SocialTestSettings updatedSocialTestSettings2 = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OAUTH_OP, DoesNotUseSelectionPanel);
        updatedSocialTestSettings2 = updateLibertyOPSettings(updatedSocialTestSettings2);
        updatedSocialTestSettings2.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_oauth2");

        List<validationData> expectations2 = setGoodSocialExpectations(updatedSocialTestSettings2, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings2, expectations2);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this Test we are going to access Liberty OP using first an un-filtered app, then a filtered app (more details of how
     * the test works can be found above)
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then Liberty OP login page. After entering a valid id/pw, we should
     * receive access to the helloworld app. Then when we invoke the app again, we should immediately get the
     * Liberty OP login page - entering a valid id/pw should give us access to our app.
     * </OL>
     */
    @AllowedFFDC({ "java.io.IOException" })
    @Mode(TestMode.LITE)
    @Test
    public void MultiProvider_usingSocialConfig_withFilters_SelectLibertyOP_oidc() throws Exception {

        // let genericSocial create a WebClient each time we call it - we want a unique client each time

        // first use the config that does NOT match a filter - expect a selection panel
        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OIDC_OP, UseSelectionPanel);
        updatedSocialTestSettings = updateLibertyOPSettings(updatedSocialTestSettings);
        updatedSocialTestSettings.setProviderButton(SocialConstants.OIDC_LOGIN + 1);

        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        // Then use the config that does match a filter
        // expectations will look for the login panel immediately after we try to invoke the filter protected app
        //      instead of getting the provider selection panel
        SocialTestSettings updatedSocialTestSettings2 = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OIDC_OP, DoesNotUseSelectionPanel);
        updatedSocialTestSettings2 = updateLibertyOPSettings(updatedSocialTestSettings2);
        updatedSocialTestSettings2.setProtectedResource(genericTestServer.getServerHttpsString() + "/helloworld/rest/helloworld_oidc");

        List<validationData> expectations2 = setGoodSocialExpectations(updatedSocialTestSettings2, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings2, expectations2);

    }

}
