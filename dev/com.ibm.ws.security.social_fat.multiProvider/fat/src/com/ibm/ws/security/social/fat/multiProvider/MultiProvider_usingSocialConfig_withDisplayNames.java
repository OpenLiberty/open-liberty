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
import com.ibm.ws.security.social.fat.utils.SocialMessageConstants;
import com.ibm.ws.security.social.fat.utils.SocialTestSettings;

import componenttest.annotation.AllowedFFDC;
import componenttest.annotation.ExpectedFFDC;
import componenttest.custom.junit.runner.FATRunner;
import componenttest.custom.junit.runner.Mode;
import componenttest.custom.junit.runner.Mode.TestMode;
import componenttest.topology.impl.LibertyServerWrapper;

/**
 * These tests are used to show that we can list list/display the provider display names. They also show that
 * we use the correct selection even when there are multiple choices and even if there are duplicate display
 * names.
 *
 * These tests are a bit different than the other selection panel tests.
 * They use a config that has multiple configs for each provider. Each provider has four(4) configurations. They are:
 * 1) config has a unique display name
 * 2) config has a unique display name
 * 3) config has a duplicate display name
 * 4) config has a duplicate display name (also has a bad clientSecret to show that we really are using this config)
 * All of the configs have unique config id's (OSGI doesn't let us see multiple configs with the same id, so, no point in trying)
 *
 * When we specifiy an app that does NOT have a filter, we should get the selection panel. When we specify an app that does
 * match a filter,
 * we should use that provider directly - there should be no need for the selection panel.
 * Each test will invoke an un-filtered app and expects the selection panel, it'll then choose a specific provider and make
 * sure that we can get to the app. It will then invoke the filtered version of the app and expect to get the login page
 * immediately.
 */

@RunWith(FATRunner.class)
@LibertyServerWrapper
@Mode(TestMode.FULL)
public class MultiProvider_usingSocialConfig_withDisplayNames extends SocialCommonTest {

    public static Class<?> thisClass = MultiProvider_usingSocialConfig_withDisplayNames.class;

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
        genericTestServer = commonSetUp(SocialConstants.SERVER_NAME + ".multiProvider.social", "server_multiProvider_usingSocialConfig_withDisplayNames.xml", SocialConstants.GENERIC_SERVER, extraApps, SocialConstants.DO_NOT_USE_DERBY, startMsgs);

        setGenericVSSpeicificProviderFlags(GenericConfig);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this Test we are going to access Facebook using each of the available provider configs.
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then Facebook login page. After entering a valid id/pw, we should
     * receive access to the helloworld app for the first 3 requests, and error messages indicating that
     * the client secret is bad in the forth as the secret in that config is bad.
     * </OL>
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRun.class)
    @AllowedFFDC({ "java.io.IOException", "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void MultiProvider_usingSocialConfig_withDisplayNames_SelectFacebook() throws Exception {

        // let genericSocial create a WebClient each time we call it - we want a unique client each time
        setGenericVSSpeicificProviderFlags(GenericConfig);

        /* Invoke one of the unique providers */
        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.FACEBOOK_PROVIDER, UseSelectionPanel);
        updatedSocialTestSettings = updateFacebookSettings(updatedSocialTestSettings);
        updatedSocialTestSettings.setProviderButtonDisplay("Facebook_1");
        updatedSocialTestSettings.setProviderButton("facebookLogin_displayName1");
        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        /* Invoke the other unique provider */
        updatedSocialTestSettings.setProviderButtonDisplay("Facebook_2");
        updatedSocialTestSettings.setProviderButton("facebookLogin_displayName2");
        expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        /* Invoke one of the duplidate provider entries (this one contains valid values) */
        updatedSocialTestSettings.setProviderButtonDisplay("Facebook_3");
        updatedSocialTestSettings.setProviderButton("facebookLogin_displayName3");
        expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        /* Invoke the other duplicate provider entry (which happens to be bad) */
        updatedSocialTestSettings.setProviderButtonDisplay("Facebook_3");
        updatedSocialTestSettings.setProviderButton("facebookLogin_displayName4");
        expectations = vData.addSuccessStatusCodesForActions(SocialConstants.FACEBOOK_PERFORM_SOCIAL_LOGIN, invoke_social_login_actions);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.FACEBOOK_PERFORM_SOCIAL_LOGIN, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.FACEBOOK_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an unauthorized exception", "Error validating client secret");
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.FACEBOOK_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error attempting to access the endpoint", SocialMessageConstants.CWWKS5478E_BAD_ENDPOINT_REQUEST);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.FACEBOOK_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received general social login error", SocialMessageConstants.CWWKS5451E_BADTOKEN_INFO);
        expectations = vData.addExpectation(expectations, SocialConstants.FACEBOOK_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.UNAUTHORIZED_MESSAGE, null, SocialConstants.UNAUTHORIZED_MESSAGE);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        /*
         * Invoke using the facebookLogin config button (uses specific config, others use generic - make sure that it behaves
         * properly)
         */
        setGenericVSSpeicificProviderFlags(ProviderConfig);
        updatedSocialTestSettings.setProviderButtonDisplay("Facebook_5");
        updatedSocialTestSettings.setProviderButton(SocialConstants.FACEBOOK_LOGIN);
        expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /** */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRun.class)
    @AllowedFFDC({ "java.io.IOException" })
    @Test
    public void MultiProvider_usingSocialConfig_withDisplayNames_SelectTwitter() throws Exception {

        // let genericSocial create a WebClient each time we call it - we want a unique client each time

        setGenericVSSpeicificProviderFlags(ProviderConfig);

        /* Invoke one of the unique providers */
        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.TWITTER_PROVIDER, UseSelectionPanel);
        updatedSocialTestSettings = updateTwitterSettings(updatedSocialTestSettings);
        updatedSocialTestSettings.setProviderButtonDisplay("Twitter_5");
        updatedSocialTestSettings.setProviderButton(SocialConstants.TWITTER_LOGIN);
        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this Test we are going to access Linkedin using each of the available provider configs.
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then Linkedin login page. After entering a valid id/pw, we should
     * receive access to the helloworld app for the first 3 requests, and error messages indicating that
     * the client secret is bad in the forth as the secret in that config is bad.
     * </OL>
     */
    @ConditionalIgnoreRule.ConditionalIgnore(condition = skipIfExternalProvidersShouldntRun.class)
    @AllowedFFDC({ "java.io.IOException", "com.ibm.ws.security.social.error.SocialLoginException" })
    @Test
    public void MultiProvider_usingSocialConfig_withDisplayNames_SelectLinkedin() throws Exception {

        // let genericSocial create a WebClient each time we call it - we want a unique client each time

        setGenericVSSpeicificProviderFlags(GenericConfig);

        /* Invoke one of the unique providers */
        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.LINKEDIN_PROVIDER, SocialConstants.OAUTH_OP, UseSelectionPanel);
        updatedSocialTestSettings = updateLinkedinSettings(updatedSocialTestSettings);
        updatedSocialTestSettings.setProviderButtonDisplay("Linkedin_1");
        updatedSocialTestSettings.setProviderButton("linkedinLogin_displayName1");
        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        updatedSocialTestSettings.setProviderButtonDisplay("Linkedin_2");
        updatedSocialTestSettings.setProviderButton("linkedinLogin_displayName2");
        expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        updatedSocialTestSettings.setProviderButtonDisplay("Linkedin_3");
        updatedSocialTestSettings.setProviderButton("linkedinLogin_displayName3");
        expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        updatedSocialTestSettings.setProviderButtonDisplay("Linkedin_3");
        updatedSocialTestSettings.setProviderButton("linkedinLogin_displayName4");
        expectations = vData.addSuccessStatusCodesForActions(SocialConstants.LINKEDIN_PERFORM_SOCIAL_LOGIN, invoke_social_login_actions);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.LINKEDIN_PERFORM_SOCIAL_LOGIN, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.LINKEDIN_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an unauthorized exception", "Client authentication failed");
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.LINKEDIN_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error attempting to access the endpoint", SocialMessageConstants.CWWKS5478E_BAD_ENDPOINT_REQUEST);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.LINKEDIN_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received general social login error", SocialMessageConstants.CWWKS5451E_BADTOKEN_INFO);
        expectations = vData.addExpectation(expectations, SocialConstants.LINKEDIN_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.UNAUTHORIZED_MESSAGE, null, SocialConstants.UNAUTHORIZED_MESSAGE);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        /*
         * Invoke using the linkedinLogin config button (uses specific config, others use generic - make sure that it behaves
         * properly)
         */
        setGenericVSSpeicificProviderFlags(ProviderConfig);
        updatedSocialTestSettings.setProviderButtonDisplay("Linkedin_5");
        updatedSocialTestSettings.setProviderButton(SocialConstants.LINKEDIN_LOGIN);
        expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this Test we are going to access GitHub using each of the available provider configs.
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then GitHub login page. After entering a valid id/pw, we should
     * receive access to the helloworld app for the first 3 requests, and error messages indicating that
     * the client secret is bad in the forth as the secret in that config is bad.
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
    public void MultiProvider_usingSocialConfig_withDisplayNames_SelectGitHub() throws Exception {

        // let genericSocial create a WebClient each time we call it - we want a unique client each time

        setGenericVSSpeicificProviderFlags(GenericConfig);

        /* Invoke one of the unique providers */
        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.GITHUB_PROVIDER, SocialConstants.OAUTH_OP, UseSelectionPanel);
        updatedSocialTestSettings = updateGitHubSettings(updatedSocialTestSettings);
        updatedSocialTestSettings.setProviderButtonDisplay("github_1");
        updatedSocialTestSettings.setProviderButton("githubLogin/displayName1");
        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        updatedSocialTestSettings.setProviderButtonDisplay("github_2");
        updatedSocialTestSettings.setProviderButton("githubLogin/displayName2");
        expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        updatedSocialTestSettings.setProviderButtonDisplay("github_3");
        updatedSocialTestSettings.setProviderButton("githubLogin/displayName3");
        expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        updatedSocialTestSettings.setProviderButtonDisplay("github_3");
        updatedSocialTestSettings.setProviderButton("githubLogin/displayName4");
        expectations = vData.addSuccessStatusCodesForActions(SocialConstants.GITHUB_PERFORM_SOCIAL_LOGIN, invoke_social_login_actions);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.GITHUB_PERFORM_SOCIAL_LOGIN, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.GITHUB_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error due to missing claims", SocialMessageConstants.CWWKS5452E_NOTAUTH_DUE_TO_MISSING_CLAIMS);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.GITHUB_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error processing the response", SocialMessageConstants.CWWKS5490E_CANNOT_PROCESS_RESPONSE);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.GITHUB_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error getting user info", SocialMessageConstants.CWWKS5461E_ERROR_GETTING_USERINFO);
        expectations = vData.addExpectation(expectations, SocialConstants.GITHUB_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.UNAUTHORIZED_MESSAGE, null, SocialConstants.UNAUTHORIZED_MESSAGE);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        /*
         * Invoke using the githubLogin config button (uses specific config, others use generic - make sure that it behaves
         * properly)
         */
        setGenericVSSpeicificProviderFlags(ProviderConfig);
        updatedSocialTestSettings = updateGitHubSettings(updatedSocialTestSettings);
        updatedSocialTestSettings.setProviderButtonDisplay("GitHub_5");
        updatedSocialTestSettings.setProviderButton(SocialConstants.GITHUB_LOGIN);
        expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this Test we are going to access Liberty OP using each of the available provider configs.
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then Liberty OP login page. After entering a valid id/pw, we should
     * receive access to the helloworld app for the first 3 requests, and error messages indicating that
     * the client secret is bad in the forth as the secret in that config is bad.
     * </OL>
     */
    @ExpectedFFDC({ "com.ibm.ws.security.social.error.SocialLoginException" })
    @AllowedFFDC({ "java.io.IOException" })
    @Mode(TestMode.LITE)
    @Test
    public void MultiProvider_usingSocialConfig_withDisplayNames_SelectLibertyOP_oauth() throws Exception {

        // let genericSocial create a WebClient each time we call it - we want a unique client each time

        setGenericVSSpeicificProviderFlags(GenericConfig);

        /* Invoke one of the unique providers */
        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OAUTH_OP, UseSelectionPanel);
        updatedSocialTestSettings = updateLibertyOPSettings(updatedSocialTestSettings);
        updatedSocialTestSettings.setProviderButtonDisplay("oauth_1");
        updatedSocialTestSettings.setProviderButton("oauth2Login_displayName1");
        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        updatedSocialTestSettings.setProviderButtonDisplay("oauth_2");
        updatedSocialTestSettings.setProviderButton("oauth2Login_displayName2");
        expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        updatedSocialTestSettings.setProviderButtonDisplay("oauth_3");
        updatedSocialTestSettings.setProviderButton("oauth2Login_displayName3");
        expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        updatedSocialTestSettings.setProviderButtonDisplay("oauth_3");
        updatedSocialTestSettings.setProviderButton("oauth2Login_displayName4");
        expectations = vData.addSuccessStatusCodesForActions(SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, invoke_social_login_actions);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.UNAUTHORIZED_STATUS);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "OP logs should have shown error validating client.", SocialMessageConstants.CWOAU0038E_CLIENT_COULD_NOT_BE_VERIFIED);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an unauthorized exception", SocialMessageConstants.CWWKS1406E_INTROSPECT_INVALID_CREDENTIAL);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received an error attempting to access the endpoint", SocialMessageConstants.CWWKS5478E_BAD_ENDPOINT_REQUEST);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Should have received general social login error", SocialMessageConstants.CWWKS5451E_BADTOKEN_INFO);
        expectations = vData.addExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.RESPONSE_MESSAGE, SocialConstants.STRING_CONTAINS, "Was expecting the response message to contain: " + SocialConstants.UNAUTHORIZED_MESSAGE, null, SocialConstants.UNAUTHORIZED_MESSAGE);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        /* skip provider specific config as it does not exist */
    }

    /**
     * Test Purpose:
     * <OL>
     * <LI>In this Test we are going to access Liberty OP using each of the available provider configs.
     * </OL>
     * Expected Results:
     * <OL>
     * <LI>Should get the selection page, then Liberty OP login page. After entering a valid id/pw, we should
     * receive access to the helloworld app for the first 3 requests, and error messages indicating that
     * the client secret is bad in the forth as the secret in that config is bad.
     * </OL>
     */
    @ExpectedFFDC({ "io.openliberty.security.oidcclientcore.http.BadPostRequestException" })
    @Mode(TestMode.LITE)
    @Test
    public void MultiProvider_usingSocialConfig_withDisplayNames_SelectLibertyOP_oidc() throws Exception {

        // let genericSocial create a WebClient each time we call it - we want a unique client each time

        setGenericVSSpeicificProviderFlags(GenericConfig);

        /* Invoke one of the unique providers */
        SocialTestSettings updatedSocialTestSettings = socialSettings.copyTestSettings();
        setActionsForProvider(SocialConstants.LIBERTYOP_PROVIDER, SocialConstants.OIDC_OP, UseSelectionPanel);
        updatedSocialTestSettings = updateLibertyOPSettings(updatedSocialTestSettings);
        updatedSocialTestSettings.setProviderButtonDisplay("oidc_1");
        updatedSocialTestSettings.setProviderButton("oidcLogin_displayName1");
        List<validationData> expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        updatedSocialTestSettings.setProviderButtonDisplay("oidc_2");
        updatedSocialTestSettings.setProviderButton("oidcLogin_displayName2");
        expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        updatedSocialTestSettings.setProviderButtonDisplay("oidc_3");
        updatedSocialTestSettings.setProviderButton("oidcLogin_displayName3");
        expectations = setGoodSocialExpectations(updatedSocialTestSettings, doNotAddJWTTokenValidation);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        updatedSocialTestSettings.setProviderButtonDisplay("oidc_3");
        updatedSocialTestSettings.setProviderButton("oidcLogin_displayName4");
        expectations = vData.addSuccessStatusCodesForActions(SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, invoke_social_login_actions);
        expectations = vData.addResponseStatusExpectation(expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.BAD_REQUEST_STATUS);
        expectations = validationTools.addMessageExpectation(testOPServer, expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "OP logs should have shown error validating client.", SocialMessageConstants.CWOAU0038E_CLIENT_COULD_NOT_BE_VERIFIED);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Did not find a CWWKS1406E error message in the logs saying the token request had an invalid credential.", SocialMessageConstants.CWWKS1406E_INTROSPECT_INVALID_CREDENTIAL);
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Did not find a CWWKS1708E error message in the logs saying the OIDC client couldn't contact the provider.", SocialMessageConstants.CWWKS1708E_CLIENT_FAILED_TO_CONTACT_PROVIDER);
        // I believe the SRVE8094W error is a bug in AuthorizationCodeHandler.sendErrorJSON() where the response is being flushed too early
        expectations = validationTools.addMessageExpectation(genericTestServer, expectations, SocialConstants.LIBERTYOP_PERFORM_SOCIAL_LOGIN, SocialConstants.MESSAGES_LOG, SocialConstants.STRING_CONTAINS, "Did not find a error message for setting a header in a committed response.", SocialMessageConstants.SRVE8094W_CANNOT_SET_HEADER_RESPONSE_COMMITTED);
        genericSocial(_testName, null, invoke_social_login_actions, updatedSocialTestSettings, expectations);

        /* skip provider specific config as it does not exist */

    }

}
